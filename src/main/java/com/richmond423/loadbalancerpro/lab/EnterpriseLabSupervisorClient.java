package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.RequestDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseStatus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Synchronous application-side client for one pinned local supervisor epoch.
 * It never accepts a host, URL, proxy, redirect, or externally supplied port.
 */
public final class EnterpriseLabSupervisorClient implements AutoCloseable {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int HARD_MAX_CREDENTIAL_FILE_BYTES = 65;
    private static final Set<PosixFilePermission> FORBIDDEN_CREDENTIAL_PERMISSIONS = Set.of(
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_WRITE,
            PosixFilePermission.OTHERS_EXECUTE);

    private final Path directory;
    private final Path readinessFile;
    private final Path credentialFile;
    private final EnterpriseLabSupervisorProtocolCodec protocolCodec;
    private final EnterpriseLabSupervisorConnectionMetadataCodec metadataCodec;
    private final Clock clock;
    private final SocketConnector socketConnector;
    private final EnterpriseLabSupervisorConnectionMetadata metadata;
    private byte[] credential;
    private boolean closed;

    public static EnterpriseLabSupervisorClient connect(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            Clock clock) {
        return connect(trustedRoot, targetCatalog, clock, DefaultSocketConnector.INSTANCE);
    }

    static EnterpriseLabSupervisorClient connect(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            Clock clock,
            SocketConnector socketConnector) {
        Objects.requireNonNull(targetCatalog, "targetCatalog cannot be null");
        Objects.requireNonNull(clock, "clock cannot be null");
        Objects.requireNonNull(socketConnector, "socketConnector cannot be null");
        ClientException last = null;
        for (int attempt = 1;
             attempt <= EnterpriseLabSupervisorConfiguration.MAX_CLIENT_TRANSPORT_ATTEMPTS;
             attempt++) {
            EnterpriseLabSupervisorClient client = null;
            try {
                client = create(trustedRoot, targetCatalog, clock, socketConnector);
                Response health = client.executeConnectHealth(client.healthRequest());
                if (health.status() != ResponseStatus.ACCEPTED) {
                    throw failure(Failure.AUTHENTICATION_REJECTED,
                            "supervisor health authentication was not accepted");
                }
                return client;
            } catch (ClientException exception) {
                if (client != null) {
                    client.close();
                }
                last = exception;
                if (!retryableStartup(exception.failure())) {
                    throw exception;
                }
            }
        }
        throw Objects.requireNonNull(last, "bounded supervisor connect failed without evidence");
    }

    private static EnterpriseLabSupervisorClient create(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            Clock clock,
            SocketConnector socketConnector) {
        Clock safeClock = Objects.requireNonNull(clock, "clock cannot be null");
        EnterpriseLabSupervisorProtocolCodec protocolCodec =
                new EnterpriseLabSupervisorProtocolCodec(
                        Objects.requireNonNull(targetCatalog,
                                "targetCatalog cannot be null"));
        SocketConnector safeConnector = Objects.requireNonNull(
                socketConnector, "socketConnector cannot be null");
        Path root = validateTrustedRoot(trustedRoot);
        Path directory;
        Path readiness;
        Path credential;
        try {
            directory = EnterpriseLabSupervisorOwnership.controlledPath(
                    root, EnterpriseLabSupervisorOwnership.DIRECTORY_NAME);
            EnterpriseLabSupervisorOwnership.validateControlledDirectory(directory, root);
            readiness = EnterpriseLabSupervisorOwnership.controlledPath(
                    directory, EnterpriseLabSupervisorServer.READINESS_FILE_NAME);
            credential = EnterpriseLabSupervisorOwnership.controlledPath(
                    directory, EnterpriseLabSupervisorServer.CREDENTIAL_FILE_NAME);
        } catch (RuntimeException exception) {
            throw failure(Failure.UNSAFE_PATH,
                    "supervisor local control directory is unavailable or unsafe");
        }
        EnterpriseLabSupervisorConnectionMetadataCodec metadataCodec =
                new EnterpriseLabSupervisorConnectionMetadataCodec();
        EnterpriseLabSupervisorConnectionMetadata first = readMetadata(
                metadataCodec, readiness);
        requireFresh(first, safeClock.instant());
        byte[] credentialBytes = readCredential(credential);
        boolean transferred = false;
        try {
            EnterpriseLabSupervisorConnectionMetadata second = readMetadata(
                    metadataCodec, readiness);
            if (!first.equals(second)) {
                throw failure(Failure.METADATA_CHANGED,
                        "supervisor connection metadata changed during bounded discovery");
            }
            EnterpriseLabSupervisorClient client = new EnterpriseLabSupervisorClient(
                    directory,
                    readiness,
                    credential,
                    protocolCodec,
                    metadataCodec,
                    safeClock,
                    safeConnector,
                    first,
                    credentialBytes);
            transferred = true;
            return client;
        } finally {
            if (!transferred) {
                Arrays.fill(credentialBytes, (byte) 0);
            }
        }
    }

    private EnterpriseLabSupervisorClient(
            Path directory,
            Path readinessFile,
            Path credentialFile,
            EnterpriseLabSupervisorProtocolCodec protocolCodec,
            EnterpriseLabSupervisorConnectionMetadataCodec metadataCodec,
            Clock clock,
            SocketConnector socketConnector,
            EnterpriseLabSupervisorConnectionMetadata metadata,
            byte[] credential) {
        this.directory = directory;
        this.readinessFile = readinessFile;
        this.credentialFile = credentialFile;
        this.protocolCodec = protocolCodec;
        this.metadataCodec = metadataCodec;
        this.clock = clock;
        this.socketConnector = socketConnector;
        this.metadata = metadata;
        this.credential = credential;
    }

    public synchronized Response execute(Request request) {
        requireOpen();
        Request safe = Objects.requireNonNull(request, "request cannot be null");
        if (!metadata.supervisorInstanceId().equals(
                safe.expectedSupervisorInstanceId())
                || metadata.supervisorGeneration()
                != safe.expectedSupervisorGeneration()) {
            throw failure(Failure.REQUEST_FENCE_MISMATCH,
                    "supervisor request does not match the pinned process epoch");
        }
        return executeInternal(safe);
    }

    public synchronized EnterpriseLabSupervisorConnectionMetadata connectionMetadata() {
        requireOpen();
        return metadata;
    }

    private Response executeInternal(Request request) {
        ClientException last = null;
        for (int attempt = 1;
             attempt <= EnterpriseLabSupervisorConfiguration.MAX_CLIENT_TRANSPORT_ATTEMPTS;
             attempt++) {
            validatePinnedFiles();
            try {
                return exchange(request);
            } catch (SocketTimeoutException exception) {
                last = failure(Failure.RESPONSE_TIMEOUT,
                        "supervisor response exceeded a transport deadline");
            } catch (IOException exception) {
                last = failure(Failure.CONNECTION_FAILED,
                        "supervisor literal-loopback transport failed safely");
            }
        }
        throw Objects.requireNonNull(last, "bounded supervisor request failed without evidence");
    }

    private Response executeConnectHealth(Request request) {
        validatePinnedFiles();
        try {
            return exchange(request);
        } catch (SocketTimeoutException exception) {
            throw failure(Failure.RESPONSE_TIMEOUT,
                    "supervisor response exceeded a transport deadline");
        } catch (IOException exception) {
            throw failure(Failure.CONNECTION_FAILED,
                    "supervisor literal-loopback transport failed safely");
        }
    }

    private Response exchange(Request request) throws IOException {
        byte[] requestBytes;
        try {
            requestBytes = protocolCodec.encodeRequest(request);
        } catch (RuntimeException exception) {
            throw failure(Failure.REQUEST_INVALID,
                    "supervisor request failed strict local validation");
        }
        long startedNanos = System.nanoTime();
        try (Socket socket = socketConnector.connect(
                EnterpriseLabSupervisorConfiguration.literalLoopbackAddress(),
                metadata.port(),
                Math.toIntExact(EnterpriseLabSupervisorConfiguration
                        .CLIENT_CONNECT_TIMEOUT.toMillis()))) {
            socket.setTcpNoDelay(true);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            output.writeInt(EnterpriseLabSupervisorServer.FRAME_MAGIC);
            output.writeByte(EnterpriseLabSupervisorServer.FRAME_VERSION);
            output.writeShort(credential.length);
            output.writeInt(requestBytes.length);
            output.write(credential);
            output.write(requestBytes);
            output.flush();

            DataInputStream input = new DataInputStream(new DeadlineInputStream(
                    socket.getInputStream(), socket, startedNanos));
            if (input.readInt() != EnterpriseLabSupervisorServer.FRAME_MAGIC
                    || input.readUnsignedByte()
                    != EnterpriseLabSupervisorServer.FRAME_VERSION) {
                throw failure(Failure.RESPONSE_INVALID,
                        "supervisor transport response magic or version is invalid");
            }
            int transportStatus = input.readUnsignedByte();
            int responseLength = input.readInt();
            if (responseLength < 0
                    || responseLength
                    > EnterpriseLabSupervisorProtocol.HARD_MAX_RESPONSE_BYTES) {
                throw failure(Failure.RESPONSE_EXCEEDED_BOUNDS,
                        "supervisor transport response exceeded its byte bound");
            }
            if (transportStatus != EnterpriseLabSupervisorServer.TRANSPORT_OK) {
                if (responseLength != 0) {
                    throw failure(Failure.RESPONSE_INVALID,
                            "rejected supervisor transport response carried a body");
                }
                if (transportStatus
                        == EnterpriseLabSupervisorServer.TRANSPORT_UNAUTHORIZED) {
                    throw failure(Failure.AUTHENTICATION_REJECTED,
                            "supervisor transport authentication was rejected");
                }
                throw failure(Failure.TRANSPORT_REJECTED,
                        "supervisor transport rejected the request");
            }
            if (responseLength < 1) {
                throw failure(Failure.RESPONSE_INVALID,
                        "accepted supervisor transport response requires a body");
            }
            byte[] responseBytes = readExactly(input, responseLength);
            if (input.readUnsignedByte()
                    != EnterpriseLabSupervisorServer.TRANSPORT_DELIVERY_RECORDED) {
                throw failure(Failure.RESPONSE_INVALID,
                        "supervisor response lacked durable delivery evidence");
            }
            Response response;
            try {
                response = protocolCodec.decodeResponse(responseBytes, request);
            } catch (RuntimeException exception) {
                throw failure(Failure.RESPONSE_INVALID,
                        "supervisor response failed strict request-bound decoding");
            }
            if (!metadata.supervisorInstanceId().equals(
                    response.supervisorInstanceId())) {
                throw failure(Failure.SUPERVISOR_IDENTITY_MISMATCH,
                        "supervisor response identity did not match the pinned epoch");
            }
            if (response.supervisorGeneration() < metadata.supervisorGeneration()) {
                throw failure(Failure.GENERATION_REGRESSION,
                        "supervisor response generation regressed");
            }
            if (response.supervisorGeneration() > metadata.supervisorGeneration()) {
                throw failure(Failure.SUPERVISOR_RESTARTED,
                        "supervisor generation changed during the request");
            }
            return response;
        }
    }

    private void validatePinnedFiles() {
        requireOpen();
        EnterpriseLabSupervisorConnectionMetadata observed = readMetadata(
                metadataCodec, readinessFile);
        requireFresh(observed, clock.instant());
        if (observed.supervisorGeneration() < metadata.supervisorGeneration()) {
            throw failure(Failure.GENERATION_REGRESSION,
                    "supervisor readiness generation regressed");
        }
        if (observed.supervisorGeneration() > metadata.supervisorGeneration()) {
            throw failure(Failure.SUPERVISOR_RESTARTED,
                    "supervisor restarted and requires explicit client reconnection");
        }
        if (!metadata.sameProcessEpoch(observed)
                || !metadata.equals(observed)) {
            throw failure(Failure.METADATA_CHANGED,
                    "supervisor connection metadata changed inside one generation");
        }
        byte[] observedCredential = readCredential(credentialFile);
        try {
            if (!MessageDigest.isEqual(credential, observedCredential)) {
                throw failure(Failure.CREDENTIAL_CHANGED,
                        "supervisor credential changed without a generation transition");
            }
        } finally {
            Arrays.fill(observedCredential, (byte) 0);
        }
    }

    private Request healthRequest() {
        byte[] random = new byte[12];
        SECURE_RANDOM.nextBytes(random);
        return protocolCodec.issue(new RequestDraft(
                "client-health-" + HexFormat.of().formatHex(random),
                CommandType.HEALTH,
                "supervisor-client-bootstrap",
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                metadata.supervisorInstanceId(),
                metadata.supervisorGeneration(),
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                clock.instant(),
                java.util.Map.of()));
    }

    private static byte[] readCredential(Path path) {
        byte[] encoded = readControlled(path, HARD_MAX_CREDENTIAL_FILE_BYTES, true);
        if (encoded.length != HARD_MAX_CREDENTIAL_FILE_BYTES
                || encoded[encoded.length - 1] != (byte) '\n') {
            Arrays.fill(encoded, (byte) 0);
            throw failure(Failure.CREDENTIAL_INVALID,
                    "supervisor credential file violates its exact byte contract");
        }
        byte[] credential = Arrays.copyOf(encoded,
                EnterpriseLabSupervisorServer.CREDENTIAL_BYTES);
        Arrays.fill(encoded, (byte) 0);
        for (byte value : credential) {
            if (!((value >= (byte) '0' && value <= (byte) '9')
                    || (value >= (byte) 'a' && value <= (byte) 'f'))) {
                Arrays.fill(credential, (byte) 0);
                throw failure(Failure.CREDENTIAL_INVALID,
                        "supervisor credential file is not canonical hexadecimal");
            }
        }
        return credential;
    }

    private static EnterpriseLabSupervisorConnectionMetadata readMetadata(
            EnterpriseLabSupervisorConnectionMetadataCodec codec, Path path) {
        try {
            return codec.decode(readControlled(
                    path,
                    EnterpriseLabSupervisorConnectionMetadataCodec.HARD_MAX_METADATA_BYTES,
                    false));
        } catch (ClientException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw failure(Failure.METADATA_UNAVAILABLE,
                    "supervisor connection metadata is unavailable or invalid");
        }
    }

    private static byte[] readControlled(Path path, int maximum, boolean credential) {
        try {
            EnterpriseLabSupervisorOwnership.validateControlledFile(
                    path, path.getParent());
            if (credential) {
                requireCredentialPermissions(path);
            }
            BasicFileAttributes before = Files.readAttributes(
                    path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            long size = before.size();
            if (size < 1L || size > maximum) {
                throw failure(Failure.METADATA_UNAVAILABLE,
                        "supervisor local control file exceeds its byte bound");
            }
            ByteBuffer buffer = ByteBuffer.allocate(Math.toIntExact(size));
            try (FileChannel channel = FileChannel.open(
                    path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                int zeroProgress = 0;
                while (buffer.hasRemaining()) {
                    int read = channel.read(buffer);
                    if (read < 0) {
                        throw failure(Failure.METADATA_UNAVAILABLE,
                                "supervisor local control file ended early");
                    }
                    if (read == 0 && ++zeroProgress >= 3) {
                        throw failure(Failure.METADATA_UNAVAILABLE,
                                "supervisor local control file read made no progress");
                    }
                    if (read > 0) {
                        zeroProgress = 0;
                    }
                }
                if (channel.read(ByteBuffer.allocate(1)) != -1) {
                    throw failure(Failure.METADATA_CHANGED,
                            "supervisor local control file grew during bounded read");
                }
            }
            BasicFileAttributes after = Files.readAttributes(
                    path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (after.size() != size
                    || !Objects.equals(before.fileKey(), after.fileKey())) {
                throw failure(Failure.METADATA_CHANGED,
                        "supervisor local control file changed during bounded read");
            }
            return buffer.array();
        } catch (ClientException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw failure(Failure.METADATA_UNAVAILABLE,
                    "supervisor local control file is unavailable");
        }
    }

    private static void requireCredentialPermissions(Path path) throws IOException {
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(
                    path, LinkOption.NOFOLLOW_LINKS);
            for (PosixFilePermission forbidden : FORBIDDEN_CREDENTIAL_PERMISSIONS) {
                if (permissions.contains(forbidden)) {
                    throw failure(Failure.CREDENTIAL_PERMISSION_UNSAFE,
                            "supervisor credential permissions are too broad");
                }
            }
        } catch (UnsupportedOperationException ignored) {
            // Windows ACL inheritance remains the same bounded local-host boundary as PR2.
        }
    }

    private static Path validateTrustedRoot(Path value) {
        Path root = Objects.requireNonNull(value, "trustedRoot cannot be null")
                .toAbsolutePath().normalize();
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    root, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isDirectory() || attributes.isSymbolicLink()) {
                throw failure(Failure.UNSAFE_PATH,
                        "supervisor client root must be a real local directory");
            }
            return root;
        } catch (ClientException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.UNSAFE_PATH,
                    "supervisor client root is unavailable");
        }
    }

    private static void requireFresh(
            EnterpriseLabSupervisorConnectionMetadata metadata, Instant now) {
        Duration age = Duration.between(metadata.publishedAt(), now);
        if (age.compareTo(EnterpriseLabSupervisorConfiguration.MAX_CREDENTIAL_AGE) > 0) {
            throw failure(Failure.CREDENTIAL_EXPIRED,
                    "supervisor credential epoch exceeded its maximum age");
        }
        if (age.compareTo(
                EnterpriseLabSupervisorConfiguration.MAX_REQUEST_CLOCK_SKEW.negated()) < 0) {
            throw failure(Failure.METADATA_FROM_FUTURE,
                    "supervisor connection metadata exceeded bounded clock skew");
        }
    }

    private static byte[] readExactly(InputStream input, int count) throws IOException {
        byte[] bytes = input.readNBytes(count);
        if (bytes.length != count) {
            throw new EOFException("bounded supervisor response ended early");
        }
        return bytes;
    }

    private void requireOpen() {
        if (closed || credential == null) {
            throw failure(Failure.CLIENT_CLOSED,
                    "supervisor client is closed");
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (credential != null) {
            Arrays.fill(credential, (byte) 0);
            credential = null;
        }
    }

    boolean credentialClearedForTesting() {
        return closed && credential == null;
    }

    private static boolean retryableStartup(Failure failure) {
        return failure == Failure.METADATA_UNAVAILABLE
                || failure == Failure.CONNECTION_FAILED
                || failure == Failure.RESPONSE_TIMEOUT
                || failure == Failure.AUTHENTICATION_REJECTED;
    }

    private static ClientException failure(Failure failure, String message) {
        return new ClientException(failure, message);
    }

    public enum Failure {
        UNSAFE_PATH,
        METADATA_UNAVAILABLE,
        METADATA_CHANGED,
        METADATA_FROM_FUTURE,
        CREDENTIAL_INVALID,
        CREDENTIAL_PERMISSION_UNSAFE,
        CREDENTIAL_EXPIRED,
        CREDENTIAL_CHANGED,
        CONNECTION_FAILED,
        RESPONSE_TIMEOUT,
        RESPONSE_EXCEEDED_BOUNDS,
        RESPONSE_INVALID,
        TRANSPORT_REJECTED,
        AUTHENTICATION_REJECTED,
        REQUEST_INVALID,
        REQUEST_FENCE_MISMATCH,
        SUPERVISOR_IDENTITY_MISMATCH,
        GENERATION_REGRESSION,
        SUPERVISOR_RESTARTED,
        CLIENT_CLOSED
    }

    public static final class ClientException extends IllegalStateException {
        private final Failure failure;

        private ClientException(Failure failure, String message) {
            super(message);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        public Failure failure() {
            return failure;
        }
    }

    @FunctionalInterface
    interface SocketConnector {
        Socket connect(java.net.InetAddress address, int port, int timeoutMillis)
                throws IOException;
    }

    private enum DefaultSocketConnector implements SocketConnector {
        INSTANCE;

        @Override
        public Socket connect(java.net.InetAddress address, int port, int timeoutMillis)
                throws IOException {
            Socket socket = new Socket(Proxy.NO_PROXY);
            boolean connected = false;
            try {
                socket.connect(new InetSocketAddress(address, port), timeoutMillis);
                connected = true;
                return socket;
            } finally {
                if (!connected) {
                    socket.close();
                }
            }
        }
    }

    private static final class DeadlineInputStream extends FilterInputStream {
        private final Socket socket;
        private final long startedNanos;

        private DeadlineInputStream(
                InputStream input, Socket socket, long startedNanos) {
            super(input);
            this.socket = socket;
            this.startedNanos = startedNanos;
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
            socket.setSoTimeout(EnterpriseLabSupervisorServer.boundedReadTimeoutMillis(
                    startedNanos,
                    System.nanoTime(),
                    EnterpriseLabSupervisorConfiguration.CLIENT_RESPONSE_IDLE_TIMEOUT,
                    EnterpriseLabSupervisorConfiguration.MAX_CLIENT_RESPONSE_LIFETIME));
        }
    }
}
