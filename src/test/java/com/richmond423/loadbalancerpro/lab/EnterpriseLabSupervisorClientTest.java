package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorClient.ClientException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorClient.Failure;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.RequestDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabSupervisorClientTest {
    private static final Instant NOW = Instant.parse("2026-07-19T10:00:00Z");
    private static final String APP = "application-a";
    private static final String OWNER = "a".repeat(64);

    @TempDir
    Path root;

    private EnterpriseLabExperimentTargetCatalog targets;
    private EnterpriseLabSupervisorProtocolCodec protocolCodec;
    private EnterpriseLabSupervisorConnectionMetadataCodec metadataCodec;
    private Clock clock;

    @BeforeEach
    void setUp() {
        targets = EnterpriseLabSupervisorConfiguration.approvedTargets();
        protocolCodec = new EnterpriseLabSupervisorProtocolCodec(targets);
        metadataCodec = new EnterpriseLabSupervisorConnectionMetadataCodec();
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Test
    void authenticatesThroughExplicitNoProxyLoopbackAndCleansCredential() throws Exception {
        String oldSocksHost = System.getProperty("socksProxyHost");
        String oldSocksPort = System.getProperty("socksProxyPort");
        System.setProperty("socksProxyHost", "203.0.113.7");
        System.setProperty("socksProxyPort", "9");
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            try (RunningServer running = start(ownership, service)) {
                EnterpriseLabSupervisorClient client =
                        EnterpriseLabSupervisorClient.connect(root, targets, clock);
                Request read = observation(
                        "read-installed", CommandType.READ_INSTALLED_ALLOCATION,
                        client.connectionMetadata());
                assertEquals(ResponseStatus.ACCEPTED, client.execute(read).status());
                assertEquals(
                        EnterpriseLabSupervisorConnectionMetadata.LITERAL_ADDRESS,
                        client.connectionMetadata().address());

                assertEquals(ResponseStatus.ACCEPTED,
                        client.execute(advance(service.state())).status());
                assertEquals(ResponseStatus.ACCEPTED,
                        client.execute(shutdown(service.state())).status());
                assertEquals(
                        EnterpriseLabSupervisorServer.ExitReason.CLEAN_SHUTDOWN,
                        running.await().exitReason());

                client.close();
                assertTrue(client.credentialClearedForTesting());
                assertEquals(Failure.CLIENT_CLOSED,
                        assertThrows(ClientException.class,
                                () -> client.execute(read)).failure());
            }
        } finally {
            restoreProperty("socksProxyHost", oldSocksHost);
            restoreProperty("socksProxyPort", oldSocksPort);
        }
    }

    @Test
    void detectsCredentialChangeAndSupervisorRestartOrRegression() throws Exception {
        EnterpriseLabSupervisorClient firstClient = null;
        String firstInstance;
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            try (RunningServer running = start(ownership, service)) {
                EnterpriseLabSupervisorClient connected =
                        EnterpriseLabSupervisorClient.connect(root, targets, clock);
                firstClient = connected;
                firstInstance = connected.connectionMetadata().supervisorInstanceId();
                byte[] original = EnterpriseLabSupervisorServer.readCredentialForTesting(root);
                Files.writeString(
                        credentialFile(),
                        "f".repeat(64) + "\n",
                        StandardCharsets.US_ASCII,
                        StandardOpenOption.TRUNCATE_EXISTING);
                assertEquals(
                        Failure.CREDENTIAL_CHANGED,
                        assertThrows(
                                ClientException.class,
                                () -> connected.execute(observation(
                                        "credential-changed",
                                        CommandType.HEALTH,
                                        connected.connectionMetadata())))
                                .failure());
                Files.writeString(
                        credentialFile(),
                        new String(original, StandardCharsets.US_ASCII) + "\n",
                        StandardCharsets.US_ASCII,
                        StandardOpenOption.TRUNCATE_EXISTING);
                Arrays.fill(original, (byte) 0);
                connected.execute(advance(service.state()));
                connected.execute(shutdown(service.state()));
                assertEquals(
                        EnterpriseLabSupervisorServer.ExitReason.CLEAN_SHUTDOWN,
                        running.await().exitReason());
            }
        }

        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            try (RunningServer running = start(ownership, service)) {
                EnterpriseLabSupervisorClient staleClient = firstClient;
                assertEquals(
                        Failure.SUPERVISOR_RESTARTED,
                        assertThrows(
                                ClientException.class,
                                () -> staleClient.execute(observation(
                                        "old-client",
                                        CommandType.HEALTH,
                                        staleClient.connectionMetadata())))
                                .failure());

                EnterpriseLabSupervisorClient secondClient =
                        EnterpriseLabSupervisorClient.connect(root, targets, clock);
                EnterpriseLabSupervisorConnectionMetadata current =
                        secondClient.connectionMetadata();
                assertEquals(2L, current.supervisorGeneration());
                assertNotEquals(firstInstance, current.supervisorInstanceId());
                EnterpriseLabSupervisorConnectionMetadata regressed =
                        new EnterpriseLabSupervisorConnectionMetadata(
                                current.schemaVersion(),
                                current.address(),
                                current.port(),
                                "supervisor-regressed",
                                1L,
                                current.durableStateGeneration(),
                                current.stateFingerprint(),
                                current.publishedAt());
                Files.write(readinessFile(), metadataCodec.encode(regressed));
                assertEquals(
                        Failure.GENERATION_REGRESSION,
                        assertThrows(
                                ClientException.class,
                                () -> secondClient.execute(observation(
                                        "regressed",
                                        CommandType.HEALTH,
                                        current)))
                                .failure());
                Files.write(readinessFile(), metadataCodec.encode(current));
                secondClient.execute(shutdown(service.state()));
                assertEquals(
                        EnterpriseLabSupervisorServer.ExitReason.CLEAN_SHUTDOWN,
                        running.await().exitReason());
                secondClient.close();
            }
        } finally {
            if (firstClient != null) {
                firstClient.close();
            }
        }
    }

    @Test
    void boundsConnectionAttemptsAndNeverLetsConnectorSelectAnAddress() throws Exception {
        prepareControlFiles(18081);
        AtomicInteger attempts = new AtomicInteger();
        EnterpriseLabSupervisorClient.SocketConnector connector =
                (address, port, timeoutMillis) -> {
                    attempts.incrementAndGet();
                    assertEquals("127.0.0.1", address.getHostAddress());
                    assertEquals(18081, port);
                    assertEquals(2_000, timeoutMillis);
                    throw new IOException("synthetic local refusal");
                };

        ClientException failure = assertThrows(
                ClientException.class,
                () -> EnterpriseLabSupervisorClient.connect(
                        root, targets, clock, connector));
        assertEquals(Failure.CONNECTION_FAILED, failure.failure());
        assertEquals(
                EnterpriseLabSupervisorConfiguration.MAX_CLIENT_TRANSPORT_ATTEMPTS,
                attempts.get());
    }

    @Test
    void rejectsOversizedBinaryResponseBeforeAllocationOrRetry() throws Exception {
        InetAddress loopback = EnterpriseLabSupervisorConfiguration.literalLoopbackAddress();
        try (ServerSocket listener = new ServerSocket(0, 8, loopback)) {
            prepareControlFiles(listener.getLocalPort());
            AtomicInteger accepted = new AtomicInteger();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> responder = executor.submit(() -> {
                try (Socket socket = listener.accept()) {
                    accepted.incrementAndGet();
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    input.readInt();
                    input.readUnsignedByte();
                    int credentialLength = input.readUnsignedShort();
                    int requestLength = input.readInt();
                    input.readNBytes(credentialLength + requestLength);
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    output.writeInt(EnterpriseLabSupervisorServer.FRAME_MAGIC);
                    output.writeByte(EnterpriseLabSupervisorServer.FRAME_VERSION);
                    output.writeByte(EnterpriseLabSupervisorServer.TRANSPORT_OK);
                    output.writeInt(
                            EnterpriseLabSupervisorProtocol.HARD_MAX_RESPONSE_BYTES + 1);
                    output.flush();
                } catch (IOException exception) {
                    throw new IllegalStateException("bounded fake server failed", exception);
                }
            });
            try {
                ClientException failure = assertThrows(
                        ClientException.class,
                        () -> EnterpriseLabSupervisorClient.connect(root, targets, clock));
                assertEquals(Failure.RESPONSE_EXCEEDED_BOUNDS, failure.failure());
                responder.get(5, TimeUnit.SECONDS);
                assertEquals(1, accepted.get());
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void rejectsUnexpectedSupervisorIdentityWithoutRetry() throws Exception {
        InetAddress loopback = EnterpriseLabSupervisorConfiguration.literalLoopbackAddress();
        try (ServerSocket listener = new ServerSocket(0, 8, loopback)) {
            prepareControlFiles(listener.getLocalPort());
            AtomicInteger accepted = new AtomicInteger();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> responder = executor.submit(() -> {
                try (Socket socket = listener.accept()) {
                    accepted.incrementAndGet();
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    assertEquals(EnterpriseLabSupervisorServer.FRAME_MAGIC, input.readInt());
                    assertEquals(EnterpriseLabSupervisorServer.FRAME_VERSION,
                            input.readUnsignedByte());
                    int credentialLength = input.readUnsignedShort();
                    int requestLength = input.readInt();
                    input.readNBytes(credentialLength);
                    Request request = protocolCodec.decodeRequest(
                            input.readNBytes(requestLength));
                    Response response = protocolCodec.issue(request, new ResponseDraft(
                            request.requestId(),
                            request.requestFingerprint(),
                            request.commandType(),
                            "supervisor-impostor",
                            request.expectedSupervisorGeneration(),
                            0L,
                            request.commandType().classification(),
                            ResponseStatus.REJECTED,
                            false,
                            Optional.empty(),
                            EnterpriseLabSupervisorProtocol.NONE,
                            0L,
                            1L,
                            VerificationResult.NOT_ATTEMPTED,
                            "IDENTITY_REJECTED",
                            "the bounded local supervisor identity was rejected",
                            NOW));
                    byte[] body = protocolCodec.encodeResponse(response);
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    output.writeInt(EnterpriseLabSupervisorServer.FRAME_MAGIC);
                    output.writeByte(EnterpriseLabSupervisorServer.FRAME_VERSION);
                    output.writeByte(EnterpriseLabSupervisorServer.TRANSPORT_OK);
                    output.writeInt(body.length);
                    output.write(body);
                    output.writeByte(
                            EnterpriseLabSupervisorServer.TRANSPORT_DELIVERY_RECORDED);
                    output.flush();
                } catch (IOException exception) {
                    throw new IllegalStateException("bounded fake server failed", exception);
                }
            });
            try {
                ClientException failure = assertThrows(
                        ClientException.class,
                        () -> EnterpriseLabSupervisorClient.connect(root, targets, clock));
                assertEquals(Failure.SUPERVISOR_IDENTITY_MISMATCH, failure.failure());
                responder.get(5, TimeUnit.SECONDS);
                assertEquals(1, accepted.get());
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void rejectsExpiredFutureAndNonCanonicalCredentialBeforeConnecting() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        EnterpriseLabSupervisorClient.SocketConnector connector =
                (address, port, timeoutMillis) -> {
                    attempts.incrementAndGet();
                    throw new IOException("connector must not run for invalid local controls");
                };

        prepareControlFiles(
                18081,
                NOW.minus(EnterpriseLabSupervisorConfiguration.MAX_CREDENTIAL_AGE)
                        .minusSeconds(1L));
        assertEquals(
                Failure.CREDENTIAL_EXPIRED,
                assertThrows(ClientException.class,
                        () -> EnterpriseLabSupervisorClient.connect(
                                root, targets, clock, connector)).failure());

        prepareControlFiles(
                18081,
                NOW.plus(EnterpriseLabSupervisorConfiguration.MAX_REQUEST_CLOCK_SKEW)
                        .plusSeconds(1L));
        assertEquals(
                Failure.METADATA_FROM_FUTURE,
                assertThrows(ClientException.class,
                        () -> EnterpriseLabSupervisorClient.connect(
                                root, targets, clock, connector)).failure());

        prepareControlFiles(18081);
        Files.writeString(
                credentialFile(),
                "A".repeat(64) + "\n",
                StandardCharsets.US_ASCII,
                StandardOpenOption.TRUNCATE_EXISTING);
        assertEquals(
                Failure.CREDENTIAL_INVALID,
                assertThrows(ClientException.class,
                        () -> EnterpriseLabSupervisorClient.connect(
                                root, targets, clock, connector)).failure());

        prepareControlFiles(18081);
        Files.writeString(
                readinessFile(),
                "{}\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING);
        assertEquals(
                Failure.METADATA_UNAVAILABLE,
                assertThrows(ClientException.class,
                        () -> EnterpriseLabSupervisorClient.connect(
                                root, targets, clock, connector)).failure());
        assertEquals(0, attempts.get());
    }

    private EnterpriseLabSupervisorService service(
            EnterpriseLabSupervisorOwnership ownership) {
        return EnterpriseLabSupervisorService.startForTesting(
                ownership,
                targets,
                clock,
                request -> EnterpriseLabSupervisorService.OwnershipVerification.allow(),
                point -> { });
    }

    private RunningServer start(
            EnterpriseLabSupervisorOwnership ownership,
            EnterpriseLabSupervisorService service) throws Exception {
        EnterpriseLabSupervisorServer server = new EnterpriseLabSupervisorServer(
                ownership, service, targets, clock, 0);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<EnterpriseLabSupervisorServer.RunResult> future = executor.submit(server::run);
        long deadline = System.nanoTime()
                + EnterpriseLabSupervisorConfiguration.STARTUP_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            Optional<Integer> port = EnterpriseLabSupervisorServer
                    .readReadyPortForTesting(root);
            if (port.isPresent()) {
                return new RunningServer(server, executor, future);
            }
            Thread.sleep(10L);
        }
        server.close();
        executor.shutdownNow();
        throw new IllegalStateException("supervisor readiness timed out");
    }

    private Request observation(
            String requestId,
            CommandType command,
            EnterpriseLabSupervisorConnectionMetadata connection) {
        return protocolCodec.issue(new RequestDraft(
                requestId,
                command,
                "observer-app",
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                connection.supervisorInstanceId(),
                connection.supervisorGeneration(),
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                NOW,
                Map.of()));
    }

    private Request advance(EnterpriseLabSupervisorState state) {
        return protocolCodec.issue(new RequestDraft(
                "advance-1",
                CommandType.ADVANCE_APPLICATION_OWNERSHIP,
                APP,
                OWNER,
                1L,
                state.supervisorInstanceId(),
                state.supervisorGeneration(),
                "handoff-1",
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                NOW,
                Map.of()));
    }

    private Request shutdown(EnterpriseLabSupervisorState state) {
        return protocolCodec.issue(new RequestDraft(
                "shutdown-" + state.supervisorGeneration(),
                CommandType.CLEAN_SHUTDOWN,
                APP,
                OWNER,
                1L,
                state.supervisorInstanceId(),
                state.supervisorGeneration(),
                "shutdown-transaction-" + state.supervisorGeneration(),
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                NOW,
                Map.of()));
    }

    private void prepareControlFiles(int port) throws Exception {
        prepareControlFiles(port, NOW);
    }

    private void prepareControlFiles(int port, Instant publishedAt) throws Exception {
        Files.createDirectories(supervisorDirectory());
        EnterpriseLabSupervisorConnectionMetadata metadata =
                new EnterpriseLabSupervisorConnectionMetadata(
                        EnterpriseLabSupervisorConnectionMetadata.SCHEMA_VERSION,
                        EnterpriseLabSupervisorConnectionMetadata.LITERAL_ADDRESS,
                        port,
                        "supervisor-test",
                        1L,
                        1L,
                        "b".repeat(64),
                        publishedAt);
        Files.write(readinessFile(), metadataCodec.encode(metadata));
        Files.writeString(
                credentialFile(),
                "a".repeat(64) + "\n",
                StandardCharsets.US_ASCII);
        EnterpriseLabSupervisorOwnership.restrictFilePermissions(credentialFile());
    }

    private Path supervisorDirectory() {
        return root.resolve(EnterpriseLabSupervisorOwnership.DIRECTORY_NAME);
    }

    private Path readinessFile() {
        return supervisorDirectory().resolve(
                EnterpriseLabSupervisorServer.READINESS_FILE_NAME);
    }

    private Path credentialFile() {
        return supervisorDirectory().resolve(
                EnterpriseLabSupervisorServer.CREDENTIAL_FILE_NAME);
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private record RunningServer(
            EnterpriseLabSupervisorServer server,
            ExecutorService executor,
            Future<EnterpriseLabSupervisorServer.RunResult> future)
            implements AutoCloseable {
        private EnterpriseLabSupervisorServer.RunResult await() throws Exception {
            return future.get(8, TimeUnit.SECONDS);
        }

        @Override
        public void close() {
            server.close();
            executor.shutdownNow();
        }
    }
}
