package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandLedger.ApplicationEventDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorCommandLedger.SupervisorEventDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.RequestDraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Separate-JVM writer used only by the cross-process ledger acceptance tests.
 */
final class EnterpriseLabCommandLedgerProcessProbe {
    private static final Instant START = Instant.parse("2026-07-30T00:00:00Z");
    private static final String OWNERSHIP_FINGERPRINT = "a".repeat(64);
    private static final String INSTALLED_FINGERPRINT = "b".repeat(64);

    private EnterpriseLabCommandLedgerProcessProbe() {
    }

    public static void main(String[] arguments) {
        if (arguments.length != 5) {
            throw new IllegalArgumentException(
                    "expected mode, trusted root, ready marker, release marker, and duration");
        }
        String mode = arguments[0];
        Path root = Path.of(arguments[1]).toAbsolutePath().normalize();
        Path ready = Path.of(arguments[2]).toAbsolutePath().normalize();
        Path release = Path.of(arguments[3]).toAbsolutePath().normalize();
        long durationMillis = Long.parseLong(arguments[4]);
        try {
            switch (mode) {
                case "supervisor-paused" -> writePausedSupervisorFrame(root, ready, release);
                case "supervisor-continuous" ->
                        writeSupervisorFrames(root, ready, durationMillis);
                case "application-continuous" ->
                        writeApplicationFrames(root, ready, durationMillis);
                default -> throw new IllegalArgumentException("unsupported probe mode: " + mode);
            }
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
            System.exit(2);
        }
    }

    private static void writePausedSupervisorFrame(
            Path root, Path ready, Path release) throws Exception {
        AtomicBoolean paused = new AtomicBoolean();
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = supervisorState(ownership);
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.createForTesting(
                            ownership,
                            EnterpriseLabSupervisorCommandLedger.HARD_MAX_LEDGER_BYTES,
                            EnterpriseLabSupervisorCommandLedger.HARD_MAX_EVENTS,
                            (checkpoint, bytesWritten) -> {
                                if (checkpoint
                                        == EnterpriseLabSupervisorCommandLedger.WriteCheckpoint
                                        .AFTER_WRITE_ATTEMPT
                                        && paused.compareAndSet(false, true)) {
                                    writeMarker(ready, Integer.toString(bytesWritten));
                                    awaitMarker(release);
                                }
                            });
            Request request = supervisorRequest("paused-frame", state, START);
            ledger.append(request, SupervisorEventDraft.receipt(state, START));
        }
    }

    private static void writeSupervisorFrames(
            Path root, Path ready, long durationMillis) throws Exception {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = supervisorState(ownership);
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            writeMarker(ready, "ready");
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(durationMillis);
            int sequence = 0;
            while (System.nanoTime() < deadline
                    && sequence < EnterpriseLabSupervisorCommandLedger.HARD_MAX_EVENTS) {
                Instant occurredAt = START.plusMillis(sequence);
                Request request = supervisorRequest(
                        "supervisor-" + sequence, state, occurredAt);
                ledger.append(request, SupervisorEventDraft.receipt(state, occurredAt));
                sequence++;
            }
        }
    }

    private static void writeApplicationFrames(
            Path root, Path ready, long durationMillis) {
        EnterpriseLabMutationTestAuthority authority =
                new EnterpriseLabMutationTestAuthority(root);
        EnterpriseLabSupervisorProtocolCodec codec =
                new EnterpriseLabSupervisorProtocolCodec(
                        EnterpriseLabSupervisorConfiguration.approvedTargets());
        try (EnterpriseLabApplicationCommandLedger ledger =
                     EnterpriseLabApplicationCommandLedger.createOwned(root, authority)) {
            writeMarker(ready, "ready");
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(durationMillis);
            int sequence = 0;
            while (System.nanoTime() < deadline
                    && sequence < EnterpriseLabApplicationCommandLedger.HARD_MAX_EVENTS) {
                Instant occurredAt = START.plusMillis(sequence);
                Request request = codec.issue(new RequestDraft(
                        "application-" + sequence,
                        CommandType.HEALTH,
                        "application-instance-1",
                        OWNERSHIP_FINGERPRINT,
                        1L,
                        "supervisor-instance-1",
                        1L,
                        EnterpriseLabSupervisorProtocol.NONE,
                        Optional.empty(),
                        AllocationPurpose.RECONCILIATION_NO_OP,
                        Optional.empty(),
                        EnterpriseLabSupervisorProtocol.NONE,
                        EnterpriseLabSupervisorProtocol.NONE,
                        occurredAt,
                        Map.of()));
                ledger.append(request, ApplicationEventDraft.intent(
                        INSTALLED_FINGERPRINT,
                        7L,
                        occurredAt,
                        Map.of("scope", "cross-process-ledger-proof")));
                sequence++;
            }
        }
    }

    private static EnterpriseLabSupervisorState supervisorState(
            EnterpriseLabSupervisorOwnership ownership) {
        EnterpriseLabSupervisorService service =
                EnterpriseLabSupervisorService.startForTesting(
                        ownership,
                        EnterpriseLabSupervisorConfiguration.approvedTargets(),
                        Clock.fixed(START, ZoneOffset.UTC),
                        request -> EnterpriseLabSupervisorService
                                .OwnershipVerification.allow(),
                        checkpoint -> { });
        return service.state();
    }

    private static Request supervisorRequest(
            String requestId,
            EnterpriseLabSupervisorState state,
            Instant occurredAt) {
        EnterpriseLabSupervisorProtocolCodec codec =
                new EnterpriseLabSupervisorProtocolCodec(
                        EnterpriseLabSupervisorConfiguration.approvedTargets());
        return codec.issue(new RequestDraft(
                requestId,
                CommandType.HEALTH,
                "cross-process-observer",
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                state.supervisorInstanceId(),
                state.supervisorGeneration(),
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                occurredAt,
                Map.of("scope", "cross-process-ledger-proof")));
    }

    private static void writeMarker(Path marker, String value) {
        try {
            Files.writeString(
                    marker,
                    value,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new IllegalStateException("could not publish process marker", exception);
        }
    }

    private static void awaitMarker(Path marker) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            if (Files.isRegularFile(marker)) {
                return;
            }
            try {
                Thread.sleep(5L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "interrupted while waiting for release marker", exception);
            }
        }
        throw new IllegalStateException("release marker was not published within bounds");
    }
}
