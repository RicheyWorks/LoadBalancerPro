package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.RequestDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabSupervisorProcessIntegrationTest {
    @TempDir
    Path root;

    @Test
    void separateJvmOwnsLockAndReconstructsDurableInstalledStateAfterAbruptExit()
            throws Exception {
        EnterpriseLabExperimentTargetCatalog targets =
                EnterpriseLabSupervisorConfiguration.approvedTargets();
        EnterpriseLabSupervisorProtocolCodec codec =
                new EnterpriseLabSupervisorProtocolCodec(targets);

        Process first = startChild();
        Published firstPublished;
        try {
            firstPublished = awaitPublished(first, "");
            ResponseStatus firstHealth = health(
                    targets, codec, firstPublished, "health-first");
            assertEquals(ResponseStatus.ACCEPTED, firstHealth);
            var competing = assertThrows(
                    EnterpriseLabSupervisorOwnership.OwnershipException.class,
                    () -> EnterpriseLabSupervisorOwnership.acquire(root));
            assertEquals(
                    EnterpriseLabSupervisorOwnership.Failure.LIVE_COMPETING_SUPERVISOR,
                    competing.failure());
        } finally {
            stopAbruptly(first);
        }

        assertTrue(Files.isRegularFile(supervisorPath(
                EnterpriseLabSupervisorStateStore.STATE_FILE_NAME)));
        EnterpriseLabSupervisorState firstState;
        try (EnterpriseLabSupervisorOwnership ownership = awaitOwnership()) {
            firstState = new EnterpriseLabSupervisorStateStore(ownership, targets)
                    .readIfPresent().orElseThrow();
        }
        assertEquals(1L, firstState.supervisorGeneration());
        assertTrue(firstState.installedAllocation().safeDefault());

        Process second = startChild();
        try {
            Published secondPublished = awaitPublished(
                    second, firstPublished.readinessText());
            assertNotEquals(firstPublished.credential(), secondPublished.credential());
            assertNotEquals(firstPublished.readinessText(), secondPublished.readinessText());
            assertTrue(secondPublished.readinessText()
                    .contains("\"supervisorGeneration\":2"));
            assertEquals(
                    ResponseStatus.ACCEPTED,
                    health(targets, codec, secondPublished,
                            "health-second"));
        } finally {
            stopAbruptly(second);
        }

        try (EnterpriseLabSupervisorOwnership ownership = awaitOwnership()) {
            EnterpriseLabSupervisorState restarted =
                    new EnterpriseLabSupervisorStateStore(ownership, targets)
                            .readIfPresent().orElseThrow();
            assertEquals(2L, restarted.supervisorGeneration());
            assertEquals(firstState.installedAllocation(), restarted.installedAllocation());
            assertEquals(firstState.durableStateGeneration() + 1L,
                    restarted.durableStateGeneration());
            assertNotEquals(firstState.supervisorInstanceId(),
                    restarted.supervisorInstanceId());
            assertFalse(restarted.transactionIncomplete());
        }
    }

    private Process startChild() throws IOException {
        List<String> command = new ArrayList<>();
        Path javaExecutable = Path.of(
                System.getProperty("java.home"),
                "bin",
                System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT)
                        .contains("win") ? "java.exe" : "java");
        command.add(javaExecutable.toString());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication");
        command.add("--enterprise-lab-supervisor");
        command.add("--enterprise-lab-supervisor-data-directory="
                + root.toAbsolutePath().normalize());
        command.add("--enterprise-lab-supervisor-port=0");
        return new ProcessBuilder(command)
                .directory(Path.of("").toAbsolutePath().normalize().toFile())
                .redirectErrorStream(true)
                .start();
    }

    private EnterpriseLabSupervisorOwnership awaitOwnership() throws Exception {
        long deadline = System.nanoTime()
                + EnterpriseLabSupervisorConfiguration.STARTUP_TIMEOUT.toNanos();
        EnterpriseLabSupervisorOwnership.OwnershipException last = null;
        while (System.nanoTime() < deadline) {
            try {
                return EnterpriseLabSupervisorOwnership.acquire(root);
            } catch (EnterpriseLabSupervisorOwnership.OwnershipException exception) {
                if (exception.failure()
                        != EnterpriseLabSupervisorOwnership.Failure.LIVE_COMPETING_SUPERVISOR) {
                    throw exception;
                }
                last = exception;
                Thread.sleep(20L);
            }
        }
        throw new IllegalStateException(
                "terminated supervisor lock was not released within bounds", last);
    }

    private Published awaitPublished(Process process, String priorReadiness)
            throws Exception {
        long deadline = System.nanoTime()
                + EnterpriseLabSupervisorConfiguration.STARTUP_TIMEOUT.toNanos();
        Path readiness = supervisorPath(EnterpriseLabSupervisorServer.READINESS_FILE_NAME);
        Path credential = supervisorPath(EnterpriseLabSupervisorServer.CREDENTIAL_FILE_NAME);
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                throw new IllegalStateException(
                        "supervisor child exited before readiness: " + boundedOutput(process));
            }
            if (Files.isRegularFile(readiness) && Files.isRegularFile(credential)) {
                String readinessText = Files.readString(readiness, StandardCharsets.UTF_8);
                String credentialText = Files.readString(
                        credential, StandardCharsets.US_ASCII).trim();
                Optional<Integer> port =
                        EnterpriseLabSupervisorServer.readReadyPortForTesting(root);
                if (!readinessText.equals(priorReadiness)
                        && credentialText.matches("[0-9a-f]{64}")
                        && port.isPresent()) {
                    return new Published(
                            root, port.orElseThrow(), credentialText, readinessText);
                }
            }
            Thread.sleep(20L);
        }
        throw new IllegalStateException("supervisor child readiness timed out");
    }

    private static ResponseStatus health(
            EnterpriseLabExperimentTargetCatalog targets,
            EnterpriseLabSupervisorProtocolCodec codec,
            Published published,
            String requestId) throws Exception {
        try (EnterpriseLabSupervisorClient client =
                     EnterpriseLabSupervisorClient.connect(
                             published.root(), targets, Clock.systemUTC())) {
            EnterpriseLabSupervisorConnectionMetadata connection =
                    client.connectionMetadata();
            assertEquals(published.port(), connection.port());
            Request request = codec.issue(new RequestDraft(
                    requestId,
                    CommandType.HEALTH,
                    "process-proof-observer",
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
                    Instant.now(),
                    Map.of("scope", "separate-process-proof")));
            return client.execute(request).status();
        }
    }

    private static void stopAbruptly(Process process) throws Exception {
        if (!process.isAlive()) {
            return;
        }
        process.destroyForcibly();
        if (!process.waitFor(
                EnterpriseLabSupervisorConfiguration.SHUTDOWN_TIMEOUT.toMillis(),
                TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("supervisor child did not terminate within bounds");
        }
    }

    private static String boundedOutput(Process process) {
        try {
            String value = new String(
                    process.getInputStream().readNBytes(1_024), StandardCharsets.UTF_8)
                    .replace('\r', ' ').replace('\n', ' ').trim();
            return value.length() <= 256 ? value : value.substring(0, 256);
        } catch (IOException exception) {
            return "output unavailable";
        }
    }

    private Path supervisorPath(String fileName) {
        return root.resolve(EnterpriseLabSupervisorOwnership.DIRECTORY_NAME)
                .resolve(fileName);
    }

    private record Published(
            Path root, int port, String credential, String readinessText) {
    }
}
