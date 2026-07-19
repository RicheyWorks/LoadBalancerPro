package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One-request-per-connection binary IPC server bound to literal 127.0.0.1.
 * It uses a fixed worker count, bounded queue, socket idle timeout, strict frame
 * sizes, and transport authentication separate from business evidence.
 */
public final class EnterpriseLabSupervisorServer implements AutoCloseable {
    public static final String READINESS_SCHEMA_VERSION =
            EnterpriseLabSupervisorConnectionMetadata.SCHEMA_VERSION;
    public static final int FRAME_MAGIC = 0x4c425053;
    public static final byte FRAME_VERSION = 1;
    public static final int CREDENTIAL_BYTES = 64;

    static final String CREDENTIAL_FILE_NAME = "supervisor-credential-v1.txt";
    static final String CREDENTIAL_TEMP_FILE_NAME = "supervisor-credential-v1.tmp";
    static final String READINESS_FILE_NAME = "supervisor-ready-v1.json";
    static final String READINESS_TEMP_FILE_NAME = "supervisor-ready-v1.tmp";

    static final byte TRANSPORT_OK = 0;
    static final byte TRANSPORT_UNAUTHORIZED = 1;
    static final byte TRANSPORT_MALFORMED = 2;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EnterpriseLabSupervisorOwnership ownership;
    private final EnterpriseLabSupervisorService service;
    private final EnterpriseLabSupervisorProtocolCodec codec;
    private final EnterpriseLabSupervisorConnectionMetadataCodec metadataCodec;
    private final Clock clock;
    private final int configuredPort;
    private final Duration connectionIdleTimeout;
    private final Duration maximumConnectionLifetime;
    private final ThreadPoolExecutor executor;
    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Path credentialFile;
    private final Path credentialTemporary;
    private final Path readinessFile;
    private final Path readinessTemporary;

    private ServerSocket serverSocket;
    private byte[] credential;

    public EnterpriseLabSupervisorServer(
            EnterpriseLabSupervisorOwnership ownership,
            EnterpriseLabSupervisorService service,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            Clock clock,
            int configuredPort) {
        this(
                ownership,
                service,
                targetCatalog,
                clock,
                configuredPort,
                EnterpriseLabSupervisorConfiguration.CONNECTION_IDLE_TIMEOUT,
                EnterpriseLabSupervisorConfiguration.MAX_CONNECTION_LIFETIME);
    }

    EnterpriseLabSupervisorServer(
            EnterpriseLabSupervisorOwnership ownership,
            EnterpriseLabSupervisorService service,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            Clock clock,
            int configuredPort,
            Duration connectionIdleTimeout,
            Duration maximumConnectionLifetime) {
        this.ownership = Objects.requireNonNull(ownership, "ownership cannot be null");
        this.service = Objects.requireNonNull(service, "service cannot be null");
        this.codec = new EnterpriseLabSupervisorProtocolCodec(
                Objects.requireNonNull(targetCatalog, "targetCatalog cannot be null"));
        this.metadataCodec = new EnterpriseLabSupervisorConnectionMetadataCodec();
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.configuredPort = EnterpriseLabSupervisorConfiguration.requireConfiguredPort(
                configuredPort);
        this.connectionIdleTimeout = requirePositiveDuration(
                connectionIdleTimeout, "connectionIdleTimeout");
        this.maximumConnectionLifetime = requirePositiveDuration(
                maximumConnectionLifetime, "maximumConnectionLifetime");
        this.executor = new ThreadPoolExecutor(
                EnterpriseLabSupervisorConfiguration.MAX_CONCURRENT_CONNECTIONS,
                EnterpriseLabSupervisorConfiguration.MAX_CONCURRENT_CONNECTIONS,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(
                        EnterpriseLabSupervisorConfiguration.MAX_QUEUED_CONNECTIONS),
                runnable -> {
                    Thread thread = new Thread(runnable, "enterprise-lab-supervisor-worker");
                    thread.setDaemon(false);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
        Path directory = ownership.supervisorDirectory();
        this.credentialFile = EnterpriseLabSupervisorOwnership.controlledPath(
                directory, CREDENTIAL_FILE_NAME);
        this.credentialTemporary = EnterpriseLabSupervisorOwnership.controlledPath(
                directory, CREDENTIAL_TEMP_FILE_NAME);
        this.readinessFile = EnterpriseLabSupervisorOwnership.controlledPath(
                directory, READINESS_FILE_NAME);
        this.readinessTemporary = EnterpriseLabSupervisorOwnership.controlledPath(
                directory, READINESS_TEMP_FILE_NAME);
    }

    /** Runs until authenticated clean shutdown or the bounded request count is exhausted. */
    public RunResult run() {
        ownership.requireHeld();
        try (ServerSocket listener = new ServerSocket()) {
            prepareCredential();
            this.serverSocket = listener;
            listener.setReuseAddress(false);
            listener.bind(
                    new InetSocketAddress(
                            EnterpriseLabSupervisorConfiguration.literalLoopbackAddress(),
                            configuredPort),
                    EnterpriseLabSupervisorConfiguration.MAX_QUEUED_CONNECTIONS);
            listener.setSoTimeout(500);
            if (!listener.getInetAddress().equals(
                    EnterpriseLabSupervisorConfiguration.literalLoopbackAddress())) {
                throw new IllegalStateException(
                        "supervisor listener did not bind literal IPv4 loopback");
            }
            publishReadiness(listener.getLocalPort());
            while (!closed.get()
                    && !service.shutdownRequested()
                    && requestCount.get()
                    < EnterpriseLabSupervisorConfiguration.MAX_REQUESTS_PER_PROCESS) {
                try {
                    Socket socket = listener.accept();
                    if (!socket.getInetAddress().isLoopbackAddress()
                            || !socket.getLocalAddress().equals(
                            EnterpriseLabSupervisorConfiguration.literalLoopbackAddress())) {
                        socket.close();
                        continue;
                    }
                    if (requestCount.incrementAndGet()
                            > EnterpriseLabSupervisorConfiguration.MAX_REQUESTS_PER_PROCESS) {
                        socket.close();
                        break;
                    }
                    try {
                        executor.execute(() -> handle(socket));
                    } catch (RejectedExecutionException exception) {
                        socket.close();
                    }
                } catch (java.net.SocketTimeoutException ignored) {
                    // Bounded poll permits shutdown without an unbounded accept wait.
                }
            }
            boolean clean = service.shutdownRequested();
            return new RunResult(
                    clean ? ExitReason.CLEAN_SHUTDOWN : ExitReason.REQUEST_LIMIT_REACHED,
                    listener.getLocalPort(),
                    requestCount.get(),
                    service.state().supervisorInstanceId(),
                    service.state().supervisorGeneration());
        } catch (IOException exception) {
            throw new ServerException(
                    Failure.LISTENER_FAILURE,
                    "supervisor literal-loopback listener failed safely", exception);
        } finally {
            close();
        }
    }

    private void handle(Socket socket) {
        try (socket) {
            long connectionStartedNanos = System.nanoTime();
            socket.setTcpNoDelay(true);
            DataInputStream input = new DataInputStream(new DeadlineInputStream(
                    socket.getInputStream(),
                    socket,
                    connectionStartedNanos,
                    connectionIdleTimeout,
                    maximumConnectionLifetime));
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            int magic = input.readInt();
            int version = input.readUnsignedByte();
            int credentialLength = input.readUnsignedShort();
            int requestLength = input.readInt();
            if (magic != FRAME_MAGIC
                    || version != FRAME_VERSION
                    || credentialLength != CREDENTIAL_BYTES
                    || requestLength < 1
                    || requestLength > EnterpriseLabSupervisorProtocol.HARD_MAX_REQUEST_BYTES) {
                writeTransport(output, TRANSPORT_MALFORMED, new byte[0]);
                return;
            }
            byte[] suppliedCredential = readExactly(input, credentialLength);
            if (!MessageDigest.isEqual(credential, suppliedCredential)) {
                writeTransport(output, TRANSPORT_UNAUTHORIZED, new byte[0]);
                return;
            }
            byte[] requestBytes = readExactly(input, requestLength);
            Request request;
            try {
                request = codec.decodeRequest(requestBytes);
            } catch (RuntimeException exception) {
                writeTransport(output, TRANSPORT_MALFORMED, new byte[0]);
                return;
            }
            long commandStartedNanos = System.nanoTime();
            Response response = service.dispatch(request);
            if (elapsedNanos(commandStartedNanos, System.nanoTime())
                    > EnterpriseLabSupervisorConfiguration.COMMAND_TIMEOUT.toNanos()) {
                writeTransport(output, TRANSPORT_MALFORMED, new byte[0]);
                return;
            }
            requireConnectionWithinLifetime(
                    connectionStartedNanos,
                    System.nanoTime(),
                    maximumConnectionLifetime);
            byte[] responseBytes = codec.encodeResponse(response);
            if (responseBytes.length > EnterpriseLabSupervisorProtocol.HARD_MAX_RESPONSE_BYTES) {
                writeTransport(output, TRANSPORT_MALFORMED, new byte[0]);
                return;
            }
            writeTransport(output, TRANSPORT_OK, responseBytes);
        } catch (IOException | RuntimeException ignored) {
            // The connection is one-shot and fails closed without logging secrets or raw input.
        }
    }

    private void prepareCredential() {
        ownership.requireHeld();
        byte[] random = new byte[32];
        byte[] encoded = new byte[CREDENTIAL_BYTES];
        try {
            SECURE_RANDOM.nextBytes(random);
            for (int index = 0; index < random.length; index++) {
                int value = random[index] & 0xff;
                encoded[index * 2] = lowercaseHex(value >>> 4);
                encoded[(index * 2) + 1] = lowercaseHex(value & 0x0f);
            }
        } finally {
            Arrays.fill(random, (byte) 0);
        }
        this.credential = encoded;
        byte[] published = Arrays.copyOf(encoded, encoded.length + 1);
        published[published.length - 1] = (byte) '\n';
        try {
            publishControlled(credentialTemporary, credentialFile, published);
        } finally {
            Arrays.fill(published, (byte) 0);
        }
    }

    private static byte lowercaseHex(int nibble) {
        return (byte) (nibble < 10 ? '0' + nibble : 'a' + nibble - 10);
    }

    private void publishReadiness(int port) {
        EnterpriseLabSupervisorState state = service.state();
        EnterpriseLabSupervisorConnectionMetadata metadata =
                new EnterpriseLabSupervisorConnectionMetadata(
                        READINESS_SCHEMA_VERSION,
                        EnterpriseLabSupervisorConnectionMetadata.LITERAL_ADDRESS,
                        port,
                        state.supervisorInstanceId(),
                        state.supervisorGeneration(),
                        state.durableStateGeneration(),
                        state.currentRecordFingerprint(),
                        clock.instant());
        publishControlled(
                readinessTemporary,
                readinessFile,
                metadataCodec.encode(metadata));
    }

    private void publishControlled(Path temporary, Path destination, byte[] bytes) {
        ownership.requireHeld();
        try {
            Files.deleteIfExists(temporary);
            try (var channel = java.nio.channels.FileChannel.open(
                    temporary,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE)) {
                EnterpriseLabSupervisorOwnership.restrictFilePermissions(temporary);
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    if (channel.write(buffer) == 0) {
                        throw new IOException("supervisor publication made no progress");
                    }
                }
                channel.force(true);
            }
            atomicPublish(temporary, destination);
            EnterpriseLabSupervisorOwnership.restrictFilePermissions(destination);
        } catch (AtomicMoveNotSupportedException exception) {
            throw new ServerException(
                    Failure.ATOMIC_PUBLICATION_UNSUPPORTED,
                    "supervisor metadata requires atomic publication", exception);
        } catch (IOException exception) {
            throw new ServerException(
                    Failure.PUBLICATION_FAILURE,
                    "supervisor metadata publication failed safely", exception);
        }
    }

    private static void atomicPublish(Path temporary, Path destination)
            throws IOException {
        try {
            Files.move(
                    temporary,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            throw exception;
        } catch (IOException firstFailure) {
            if (!Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                throw firstFailure;
            }
            EnterpriseLabSupervisorOwnership.validateControlledFile(
                    destination, destination.getParent());
            Files.delete(destination);
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
        }
    }

    private static byte[] readExactly(InputStream input, int count) throws IOException {
        byte[] bytes = input.readNBytes(count);
        if (bytes.length != count) {
            throw new EOFException("bounded supervisor frame ended early");
        }
        return bytes;
    }

    static int boundedReadTimeoutMillis(
            long startedNanos,
            long currentNanos,
            Duration idleTimeout,
            Duration maximumLifetime) throws java.net.SocketTimeoutException {
        Duration safeIdle = requirePositiveDuration(idleTimeout, "idleTimeout");
        Duration safeLifetime = requirePositiveDuration(maximumLifetime, "maximumLifetime");
        long remainingNanos = requireConnectionWithinLifetime(
                startedNanos, currentNanos, safeLifetime);
        long roundedRemainingMillis = Math.max(
                1L,
                (remainingNanos + TimeUnit.MILLISECONDS.toNanos(1L) - 1L)
                        / TimeUnit.MILLISECONDS.toNanos(1L));
        return Math.toIntExact(Math.min(safeIdle.toMillis(), roundedRemainingMillis));
    }

    private static long requireConnectionWithinLifetime(
            long startedNanos,
            long currentNanos,
            Duration maximumLifetime) throws java.net.SocketTimeoutException {
        long remainingNanos = maximumLifetime.toNanos()
                - elapsedNanos(startedNanos, currentNanos);
        if (remainingNanos <= 0L) {
            throw new java.net.SocketTimeoutException(
                    "bounded supervisor connection lifetime expired");
        }
        return remainingNanos;
    }

    private static long elapsedNanos(long startedNanos, long currentNanos) {
        long elapsed = currentNanos - startedNanos;
        return Math.max(0L, elapsed);
    }

    private static Duration requirePositiveDuration(Duration value, String field) {
        Duration safe = Objects.requireNonNull(value, field + " cannot be null");
        if (safe.isZero()
                || safe.isNegative()
                || safe.toMillis() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(field + " is outside hard bounds");
        }
        return safe;
    }

    private static void writeTransport(
            DataOutputStream output, byte status, byte[] body) throws IOException {
        output.writeInt(FRAME_MAGIC);
        output.writeByte(FRAME_VERSION);
        output.writeByte(status);
        output.writeInt(body.length);
        if (body.length > 0) {
            output.write(body);
        }
        output.flush();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        ServerSocket listener = serverSocket;
        if (listener != null && !listener.isClosed()) {
            try {
                listener.close();
            } catch (IOException ignored) {
                // Shutdown continues through the bounded executor path.
            }
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(
                    EnterpriseLabSupervisorConfiguration.SHUTDOWN_TIMEOUT.toMillis(),
                    TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
                executor.awaitTermination(
                        EnterpriseLabSupervisorConfiguration.SHUTDOWN_TIMEOUT.toMillis(),
                        TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
        if (service.shutdownRequested()) {
            deleteCleanMetadata(readinessFile);
            deleteCleanMetadata(credentialFile);
        }
        if (credential != null) {
            Arrays.fill(credential, (byte) 0);
            credential = null;
        }
    }

    private void deleteCleanMetadata(Path path) {
        try {
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                EnterpriseLabSupervisorOwnership.validateControlledFile(
                        path, path.getParent());
                Files.delete(path);
            }
        } catch (IOException | RuntimeException ignored) {
            // Stale metadata is generation-fenced and rotated on the next startup.
        }
    }

    static byte[] readCredentialForTesting(Path trustedRoot) throws IOException {
        Path directory = trustedRoot.toAbsolutePath().normalize()
                .resolve(EnterpriseLabSupervisorOwnership.DIRECTORY_NAME);
        Path file = EnterpriseLabSupervisorOwnership.controlledPath(
                directory, CREDENTIAL_FILE_NAME);
        byte[] published = Files.readAllBytes(file);
        try {
            if (published.length != CREDENTIAL_BYTES + 1
                    || published[published.length - 1] != (byte) '\n') {
                throw new IOException("supervisor credential file is malformed");
            }
            for (int index = 0; index < CREDENTIAL_BYTES; index++) {
                byte value = published[index];
                if (!((value >= (byte) '0' && value <= (byte) '9')
                        || (value >= (byte) 'a' && value <= (byte) 'f'))) {
                    throw new IOException("supervisor credential file is malformed");
                }
            }
            return Arrays.copyOf(published, CREDENTIAL_BYTES);
        } finally {
            Arrays.fill(published, (byte) 0);
        }
    }

    static Optional<Integer> readReadyPortForTesting(Path trustedRoot) throws IOException {
        Path directory = trustedRoot.toAbsolutePath().normalize()
                .resolve(EnterpriseLabSupervisorOwnership.DIRECTORY_NAME);
        Path file = EnterpriseLabSupervisorOwnership.controlledPath(
                directory, READINESS_FILE_NAME);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\\"port\\\":([0-9]{1,5})")
                .matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int port = Integer.parseInt(matcher.group(1));
        return port >= 1 && port <= 65_535 ? Optional.of(port) : Optional.empty();
    }

    boolean credentialClearedForTesting() {
        return credential == null;
    }

    public enum ExitReason {
        CLEAN_SHUTDOWN,
        REQUEST_LIMIT_REACHED
    }

    public record RunResult(
            ExitReason exitReason,
            int port,
            int requestCount,
            String supervisorInstanceId,
            long supervisorGeneration) {
        public RunResult {
            exitReason = Objects.requireNonNull(exitReason, "exitReason cannot be null");
            if (port < 1 || port > 65_535
                    || requestCount < 0
                    || requestCount > EnterpriseLabSupervisorConfiguration.MAX_REQUESTS_PER_PROCESS) {
                throw new IllegalArgumentException("supervisor run result is outside hard bounds");
            }
            supervisorInstanceId = Objects.requireNonNull(
                    supervisorInstanceId, "supervisorInstanceId cannot be null");
        }
    }

    public enum Failure {
        LISTENER_FAILURE,
        PUBLICATION_FAILURE,
        ATOMIC_PUBLICATION_UNSUPPORTED
    }

    public static final class ServerException extends IllegalStateException {
        private final Failure failure;

        private ServerException(Failure failure, String message, Throwable cause) {
            super(message, cause);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        public Failure failure() {
            return failure;
        }
    }

    private static final class DeadlineInputStream extends FilterInputStream {
        private final Socket socket;
        private final long startedNanos;
        private final Duration idleTimeout;
        private final Duration maximumLifetime;

        private DeadlineInputStream(
                InputStream input,
                Socket socket,
                long startedNanos,
                Duration idleTimeout,
                Duration maximumLifetime) {
            super(input);
            this.socket = Objects.requireNonNull(socket, "socket cannot be null");
            this.startedNanos = startedNanos;
            this.idleTimeout = idleTimeout;
            this.maximumLifetime = maximumLifetime;
        }

        @Override
        public int read() throws IOException {
            applyDeadline();
            return super.read();
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (length == 0) {
                return 0;
            }
            applyDeadline();
            return super.read(bytes, offset, length);
        }

        private void applyDeadline() throws IOException {
            socket.setSoTimeout(boundedReadTimeoutMillis(
                    startedNanos,
                    System.nanoTime(),
                    idleTimeout,
                    maximumLifetime));
        }
    }
}
