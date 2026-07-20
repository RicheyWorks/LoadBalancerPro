package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.RequestDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabSupervisorServerTest {
    private static final Instant NOW = Instant.parse("2026-07-19T09:00:00Z");
    private static final String APP = "application-a";
    private static final String OWNER = "a".repeat(64);

    @TempDir
    Path root;

    private EnterpriseLabExperimentTargetCatalog targets;
    private EnterpriseLabSupervisorProtocolCodec codec;
    private Clock clock;

    @BeforeEach
    void setUp() {
        targets = EnterpriseLabSupervisorConfiguration.approvedTargets();
        codec = new EnterpriseLabSupervisorProtocolCodec(targets);
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Test
    void literalLoopbackServerAuthenticatesBoundsDispatchesAndShutsDownCleanly()
            throws Exception {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            EnterpriseLabSupervisorServer server = new EnterpriseLabSupervisorServer(
                    ownership, service, targets, clock, 0);
            ExecutorService runner = Executors.newSingleThreadExecutor();
            try {
                Future<EnterpriseLabSupervisorServer.RunResult> future =
                        runner.submit(server::run);
                int port = awaitPort();
                byte[] credential = EnterpriseLabSupervisorServer
                        .readCredentialForTesting(root);
                assertEquals(64, credential.length);

                Request health = observation("health-1", CommandType.HEALTH);
                TransportResponse healthTransport = exchange(
                        port, credential, codec.encodeRequest(health));
                assertEquals(0, healthTransport.status());
                var healthResponse = codec.decodeResponse(healthTransport.body(), health);
                assertEquals(ResponseStatus.ACCEPTED, healthResponse.status());
                assertEquals("HEALTHY", healthResponse.reasonCode());
                assertEquals("127.0.0.1", connectedAddress(port));
                assertEquals(1, supervisorLedgerEvents());

                byte[] wrongCredential = "f".repeat(64)
                        .getBytes(StandardCharsets.US_ASCII);
                TransportResponse unauthorized = exchange(
                        port, wrongCredential, codec.encodeRequest(health));
                assertEquals(1, unauthorized.status());
                assertEquals(0, unauthorized.body().length);
                assertEquals(1, supervisorLedgerEvents());

                TransportResponse malformed = malformedOversized(port, credential);
                assertEquals(2, malformed.status());
                assertEquals(0, malformed.body().length);
                assertEquals(1, supervisorLedgerEvents());

                Request advance = advance(service.state());
                TransportResponse advanceTransport = exchange(
                        port, credential, codec.encodeRequest(advance));
                assertEquals(ResponseStatus.ACCEPTED,
                        codec.decodeResponse(advanceTransport.body(), advance).status());
                assertEquals(2, supervisorLedgerEvents());

                Request shutdown = shutdown(service.state());
                TransportResponse shutdownTransport = exchange(
                        port, credential, codec.encodeRequest(shutdown));
                assertEquals(ResponseStatus.ACCEPTED,
                        codec.decodeResponse(shutdownTransport.body(), shutdown).status());
                assertEquals(3, supervisorLedgerEvents());

                EnterpriseLabSupervisorServer.RunResult result =
                        future.get(8, TimeUnit.SECONDS);
                assertEquals(
                        EnterpriseLabSupervisorServer.ExitReason.CLEAN_SHUTDOWN,
                        result.exitReason());
                assertTrue(server.credentialClearedForTesting());
                assertTrue(result.requestCount() >= 5);
                assertFalse(Files.exists(supervisorPath(
                        EnterpriseLabSupervisorServer.READINESS_FILE_NAME)));
                assertFalse(Files.exists(supervisorPath(
                        EnterpriseLabSupervisorServer.CREDENTIAL_FILE_NAME)));
                assertTrue(Files.isRegularFile(supervisorPath(
                        EnterpriseLabSupervisorStateStore.STATE_FILE_NAME)));
            } finally {
                server.close();
                runner.shutdownNow();
            }
        }
    }

    @Test
    void restartRotatesCredentialAndAdvancesSupervisorGeneration() throws Exception {
        byte[] firstCredential;
        String firstInstance;
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            firstInstance = service.state().supervisorInstanceId();
            try (RunningServer running = start(ownership, service)) {
                firstCredential = running.credential();
                Request advance = advance(service.state());
                exchange(running.port(), firstCredential, codec.encodeRequest(advance));
                Request shutdown = shutdown(service.state());
                exchange(running.port(), firstCredential, codec.encodeRequest(shutdown));
                assertEquals(
                        EnterpriseLabSupervisorServer.ExitReason.CLEAN_SHUTDOWN,
                        running.await().exitReason());
            }
        }

        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            assertEquals(2L, service.state().supervisorGeneration());
            assertNotEquals(firstInstance, service.state().supervisorInstanceId());
            try (RunningServer running = start(ownership, service)) {
                assertFalse(Arrays.equals(firstCredential, running.credential()));
                Request shutdown = shutdown(service.state());
                exchange(running.port(), running.credential(), codec.encodeRequest(shutdown));
                assertEquals(
                        EnterpriseLabSupervisorServer.ExitReason.CLEAN_SHUTDOWN,
                        running.await().exitReason());
            }
        }
    }

    @Test
    void absoluteConnectionLifetimeClosesPeerBeforeLongerIdleTimeout()
            throws Exception {
        assertEquals(
                3_000,
                EnterpriseLabSupervisorServer.boundedReadTimeoutMillis(
                        1_000L,
                        1_000L,
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(10)));
        assertEquals(
                250,
                EnterpriseLabSupervisorServer.boundedReadTimeoutMillis(
                        1_000L,
                        10_750_001_000L,
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(11)));
        assertThrows(
                java.net.SocketTimeoutException.class,
                () -> EnterpriseLabSupervisorServer.boundedReadTimeoutMillis(
                        1_000L,
                        10_000_001_000L,
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(10)));

        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            EnterpriseLabSupervisorServer server = new EnterpriseLabSupervisorServer(
                    ownership,
                    service,
                    targets,
                    clock,
                    0,
                    Duration.ofSeconds(2),
                    Duration.ofMillis(200));
            ExecutorService runner = Executors.newSingleThreadExecutor();
            try {
                Future<EnterpriseLabSupervisorServer.RunResult> future =
                        runner.submit(server::run);
                int port = awaitPort();
                try (Socket stalled = new Socket(
                        EnterpriseLabSupervisorConfiguration.literalLoopbackAddress(), port)) {
                    stalled.setSoTimeout(1_500);
                    assertEquals(-1, stalled.getInputStream().read());
                }

                byte[] credential = EnterpriseLabSupervisorServer
                        .readCredentialForTesting(root);
                Request advance = advance(service.state());
                exchange(port, credential, codec.encodeRequest(advance));
                Request shutdown = shutdown(service.state());
                exchange(port, credential, codec.encodeRequest(shutdown));
                assertEquals(
                        EnterpriseLabSupervisorServer.ExitReason.CLEAN_SHUTDOWN,
                        future.get(8, TimeUnit.SECONDS).exitReason());
            } finally {
                server.close();
                runner.shutdownNow();
            }
        }
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
        int port = awaitPort();
        byte[] credential = EnterpriseLabSupervisorServer.readCredentialForTesting(root);
        return new RunningServer(server, executor, future, port, credential);
    }

    private int awaitPort() throws Exception {
        long deadline = System.nanoTime()
                + EnterpriseLabSupervisorConfiguration.STARTUP_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            Optional<Integer> port =
                    EnterpriseLabSupervisorServer.readReadyPortForTesting(root);
            if (port.isPresent()) {
                return port.orElseThrow();
            }
            Thread.sleep(10L);
        }
        throw new IllegalStateException("supervisor readiness timed out");
    }

    private Request observation(String requestId, CommandType command) {
        return codec.issue(new RequestDraft(
                requestId,
                command,
                "observer-app",
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
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
        return codec.issue(new RequestDraft(
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
        return codec.issue(new RequestDraft(
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

    private static TransportResponse exchange(
            int port, byte[] credential, byte[] request) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(
                            EnterpriseLabSupervisorConfiguration.literalLoopbackAddress(), port),
                    Math.toIntExact(
                            EnterpriseLabSupervisorConfiguration.CONNECTION_IDLE_TIMEOUT.toMillis()));
            socket.setSoTimeout(Math.toIntExact(
                    EnterpriseLabSupervisorConfiguration.CONNECTION_IDLE_TIMEOUT.toMillis()));
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            output.writeInt(EnterpriseLabSupervisorServer.FRAME_MAGIC);
            output.writeByte(EnterpriseLabSupervisorServer.FRAME_VERSION);
            output.writeShort(credential.length);
            output.writeInt(request.length);
            output.write(credential);
            output.write(request);
            output.flush();
            DataInputStream input = new DataInputStream(socket.getInputStream());
            assertEquals(EnterpriseLabSupervisorServer.FRAME_MAGIC, input.readInt());
            assertEquals(EnterpriseLabSupervisorServer.FRAME_VERSION, input.readByte());
            int status = input.readUnsignedByte();
            int length = input.readInt();
            assertTrue(length >= 0
                    && length <= EnterpriseLabSupervisorProtocol.HARD_MAX_RESPONSE_BYTES);
            byte[] body = input.readNBytes(length);
            assertEquals(length, body.length);
            return new TransportResponse(status, body);
        }
    }

    private static TransportResponse malformedOversized(
            int port, byte[] credential) throws Exception {
        try (Socket socket = new Socket(
                EnterpriseLabSupervisorConfiguration.literalLoopbackAddress(), port)) {
            socket.setSoTimeout(Math.toIntExact(
                    EnterpriseLabSupervisorConfiguration.CONNECTION_IDLE_TIMEOUT.toMillis()));
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            output.writeInt(EnterpriseLabSupervisorServer.FRAME_MAGIC);
            output.writeByte(EnterpriseLabSupervisorServer.FRAME_VERSION);
            output.writeShort(credential.length);
            output.writeInt(EnterpriseLabSupervisorProtocol.HARD_MAX_REQUEST_BYTES + 1);
            output.flush();
            DataInputStream input = new DataInputStream(socket.getInputStream());
            assertEquals(EnterpriseLabSupervisorServer.FRAME_MAGIC, input.readInt());
            assertEquals(EnterpriseLabSupervisorServer.FRAME_VERSION, input.readByte());
            int status = input.readUnsignedByte();
            int length = input.readInt();
            return new TransportResponse(status, input.readNBytes(length));
        }
    }

    private static String connectedAddress(int port) throws Exception {
        try (Socket socket = new Socket(
                EnterpriseLabSupervisorConfiguration.literalLoopbackAddress(), port)) {
            return socket.getInetAddress().getHostAddress();
        }
    }

    private Path supervisorPath(String name) {
        return root.resolve(EnterpriseLabSupervisorOwnership.DIRECTORY_NAME).resolve(name);
    }

    private int supervisorLedgerEvents() {
        return EnterpriseLabSupervisorCommandLedger.inspect(root).replay().events().size();
    }

    private record TransportResponse(int status, byte[] body) {
        private TransportResponse {
            body = Arrays.copyOf(body, body.length);
        }
    }

    private record RunningServer(
            EnterpriseLabSupervisorServer server,
            ExecutorService executor,
            Future<EnterpriseLabSupervisorServer.RunResult> future,
            int port,
            byte[] credential) implements AutoCloseable {
        private RunningServer {
            credential = Arrays.copyOf(credential, credential.length);
        }

        @Override
        public byte[] credential() {
            return Arrays.copyOf(credential, credential.length);
        }

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
