package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.DriftClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.ReconciliationAction;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.ReconciliationTrigger;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationTransactionCoordinator.Checkpoint;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationTransactionCoordinator.TransactionStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceMutationAuthority.MutationAuthorization;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Foreground bounded packaged proof for allocation transactions and reconciliation. */
public final class EnterpriseLabAllocationProofRunner {
    private static final String SCENARIO = "tail-latency-pressure";
    private static final Instant PROOF_TIME = Instant.parse("2026-07-18T18:00:00Z");
    private static final SecureRandom RANDOM = new SecureRandom();

    public EnterpriseLabAllocationProofReport run(Path output) throws IOException {
        Path safeOutput = output.toAbsolutePath().normalize();
        Files.createDirectories(safeOutput);

        NormalEvidence normal = normalTransaction(caseRoot(safeOutput, "normal"));
        boolean beforeApply = crashBeforeApply(caseRoot(safeOutput, "crash-before-apply"));
        ExternalEvidence afterApply = crashAfterApply(safeOutput);
        boolean afterCommit = crashAfterCommit(safeOutput);
        TakeoverEvidence takeover = staleOwnerTakeover(safeOutput);
        Path ownershipOutput = safeOutput.resolve("os-ownership-component")
                .toAbsolutePath().normalize();
        if (!ownershipOutput.startsWith(safeOutput)) {
            throw new IllegalArgumentException(
                    "ownership component escaped the allocation proof output boundary");
        }
        EnterpriseLabEvidenceOwnershipProofReport ownershipProof =
                new EnterpriseLabEvidenceOwnershipProofRunner().run(ownershipOutput);
        Map<String, String> drift = driftMatrix(caseRoot(safeOutput, "drift"));
        boolean restorationFailedClosed = restorationFailure(
                caseRoot(safeOutput, "restoration-failure"));
        boolean repeatedStable = repeatedReconciliation(
                caseRoot(safeOutput, "repeated"));

        return EnterpriseLabAllocationProofReport.create(
                Instant.now(),
                normal.passed(),
                beforeApply,
                afterApply.passed(),
                afterCommit,
                takeover.takeoverPassed()
                        && ownershipProof.allPassed()
                        && ownershipProof.abruptStaleOwnerClassified()
                        && ownershipProof.baselineRestorationVerified(),
                takeover.staleMutationDenied()
                        && ownershipProof.nonOwnerAllocationChangeDenied()
                        && ownershipProof.restartedPriorOwnerDenied(),
                restorationFailedClosed,
                repeatedStable,
                afterApply.separateProcessObserved() && takeover.separateProcessObserved(),
                drift,
                takeover.recordCount(),
                takeover.ownerGeneration(),
                takeover.routerGeneration(),
                takeover.baselineFingerprint(),
                takeover.installedFingerprint(),
                List.of(
                        "proof-only foreground execution; production routing does not depend on the holder",
                        "literal 127.0.0.1 targets and authenticated loopback holder control only",
                        "bounded holder lifetime, request count, output bytes, transaction records, and retries",
                        "fixed repository-approved backend identities; no addresses or allocations from callers",
                        "takeover result composes the OS-lock separate-process proof with external-holder allocation reconciliation",
                        "no cloud, tenant, private-network, production, native executable, or arbitrary command action",
                        "generated evidence remains under the validated target output directory"));
    }

    private static NormalEvidence normalTransaction(Path root) {
        try (Context context = Context.create(root, 1L)) {
            var baseline = context.coordinator(NO_FAILURE).establishSafeBaseline(
                    "normal-baseline");
            context.clock.advanceSeconds(1);
            var committed = context.coordinator(NO_FAILURE).applyCandidate(
                    "normal-candidate", "normal-experiment", context.decision, true);
            boolean exact = baseline.status() == TransactionStatus.BASELINE_COMMITTED
                    && committed.status() == TransactionStatus.COMMITTED
                    && committed.durablePhase().orElse(null) == TransactionPhase.COMMITTED
                    && committed.intendedFingerprint().equals(committed.installedFingerprint())
                    && context.router.installedSnapshot().routingSnapshot().kind() == Kind.CANDIDATE;
            var reconciled = context.reconciler().reconcile(
                    ReconciliationTrigger.OPERATOR_VERIFICATION, List.of());
            boolean restored = reconciled.ready()
                    && reconciled.action() == ReconciliationAction.BASELINE_RESTORED
                    && context.router.installedSnapshot().routingSnapshot().kind()
                    != Kind.CANDIDATE;
            return new NormalEvidence(
                    exact && restored,
                    reconciled.baselineFingerprint(),
                    context.router.installedSnapshot().allocationFingerprint());
        }
    }

    private static boolean crashBeforeApply(Path root) {
        try (Context context = Context.create(root, 1L)) {
            context.coordinator(NO_FAILURE).establishSafeBaseline("before-apply-baseline");
            boolean crashed = false;
            try {
                context.coordinator(checkpoint -> {
                    if (checkpoint == Checkpoint.AFTER_INTENT_PERSIST) {
                        throw new SimulatedCrash(checkpoint);
                    }
                }).applyCandidate(
                        "before-apply-candidate",
                        "before-apply-experiment",
                        context.decision,
                        true);
            } catch (SimulatedCrash expected) {
                crashed = expected.checkpoint == Checkpoint.AFTER_INTENT_PERSIST;
            }
            var report = context.reconciler().reconcile(
                    ReconciliationTrigger.STARTUP, List.of());
            EnterpriseLabAllocationState head = context.store.replay().chainHead().orElseThrow();
            return crashed
                    && report.ready()
                    && report.classification() == DriftClassification.UNAPPLIED_INTENT
                    && report.action() == ReconciliationAction.INCOMPLETE_INTENT_REJECTED
                    && head.transactionPhase() == TransactionPhase.REJECTED
                    && context.router.installedSnapshot().routingSnapshot().kind()
                    != Kind.CANDIDATE;
        }
    }

    private static ExternalEvidence crashAfterApply(Path output) throws IOException {
        String token = randomToken();
        Path root = caseRoot(output, "crash-after-apply");
        EnterpriseLabExperimentTargetCatalog catalog =
                EnterpriseLabAllocationProofStateHolder.fixedTargets();
        ProofAuthority authority = new ProofAuthority(root, "proof-owner-a", 1L);
        MutableClock clock = new MutableClock(PROOF_TIME);
        EnterpriseLabAdaptiveDecision decision = decision();
        EnterpriseLabLoopbackAllocationRouter initial = router(
                catalog, authority, clock, Optional.empty());
        try (var holder = EnterpriseLabAllocationProofStateHolder.start(
                output, token, initial.installedSnapshot(), catalog);
                EnterpriseLabAllocationStateStore store =
                        EnterpriseLabAllocationStateStore.createOwned(root, catalog, authority)) {
            EnterpriseLabLoopbackAllocationRouter firstRouter = router(
                    catalog, authority, clock, Optional.of(holder.store()));
            EnterpriseLabAllocationTransactionCoordinator crashing = coordinator(
                    store,
                    firstRouter,
                    catalog,
                    authority,
                    clock,
                    checkpoint -> {
                        if (checkpoint == Checkpoint.AFTER_ROUTER_APPLY) {
                            throw new SimulatedCrash(checkpoint);
                        }
                    });
            crashing.establishSafeBaseline("after-apply-baseline");
            boolean crashed = false;
            try {
                crashing.applyCandidate(
                        "after-apply-candidate",
                        "after-apply-experiment",
                        decision,
                        true);
            } catch (SimulatedCrash expected) {
                crashed = expected.checkpoint == Checkpoint.AFTER_ROUTER_APPLY;
            }
            boolean candidateSurvived = holder.store().read().routingSnapshot().kind()
                    == Kind.CANDIDATE;

            EnterpriseLabLoopbackAllocationRouter restartedRouter = router(
                    catalog, authority, clock, Optional.of(holder.store()));
            EnterpriseLabAllocationTransactionCoordinator restarted = coordinator(
                    store, restartedRouter, catalog, authority, clock, NO_FAILURE);
            EnterpriseLabAllocationReconciliationGate gate =
                    EnterpriseLabAllocationReconciliationGate.pending();
            EnterpriseLabAllocationReconciler reconciler = new EnterpriseLabAllocationReconciler(
                    store,
                    restarted,
                    restartedRouter,
                    authority,
                    gate,
                    clock,
                    restartedRouter::installedSnapshot,
                    checkpoint -> { });
            var report = reconciler.reconcile(
                    ReconciliationTrigger.STARTUP, List.of());
            boolean restored = holder.store().read().routingSnapshot().kind()
                    != Kind.CANDIDATE;
            return new ExternalEvidence(
                    crashed
                            && candidateSurvived
                            && report.ready()
                            && report.classification()
                            == DriftClassification.PARTIAL_TRANSACTION
                            && report.action() == ReconciliationAction.BASELINE_RESTORED
                            && restored,
                    holder.alive());
        }
    }

    private static boolean crashAfterCommit(Path output) throws IOException {
        String token = randomToken();
        Path root = caseRoot(output, "crash-after-commit");
        EnterpriseLabExperimentTargetCatalog catalog =
                EnterpriseLabAllocationProofStateHolder.fixedTargets();
        ProofAuthority authority = new ProofAuthority(root, "proof-owner-a", 1L);
        MutableClock clock = new MutableClock(PROOF_TIME);
        EnterpriseLabAdaptiveDecision decision = decision();
        EnterpriseLabLoopbackAllocationRouter initial = router(
                catalog, authority, clock, Optional.empty());
        try (var holder = EnterpriseLabAllocationProofStateHolder.start(
                output, token, initial.installedSnapshot(), catalog);
                EnterpriseLabAllocationStateStore store =
                        EnterpriseLabAllocationStateStore.createOwned(root, catalog, authority)) {
            EnterpriseLabLoopbackAllocationRouter firstRouter = router(
                    catalog, authority, clock, Optional.of(holder.store()));
            EnterpriseLabAllocationTransactionCoordinator crashing = coordinator(
                    store,
                    firstRouter,
                    catalog,
                    authority,
                    clock,
                    checkpoint -> {
                        if (checkpoint == Checkpoint.AFTER_COMMIT_PERSIST) {
                            throw new SimulatedCrash(checkpoint);
                        }
                    });
            crashing.establishSafeBaseline("after-commit-baseline");
            boolean crashed = false;
            try {
                crashing.applyCandidate(
                        "after-commit-candidate",
                        "after-commit-experiment",
                        decision,
                        true);
            } catch (SimulatedCrash expected) {
                crashed = expected.checkpoint == Checkpoint.AFTER_COMMIT_PERSIST;
            }
            int records = store.replay().records().size();
            EnterpriseLabLoopbackAllocationRouter restartedRouter = router(
                    catalog, authority, clock, Optional.of(holder.store()));
            EnterpriseLabAllocationTransactionCoordinator restarted = coordinator(
                    store, restartedRouter, catalog, authority, clock, NO_FAILURE);
            var replay = restarted.applyCandidate(
                    "after-commit-candidate",
                    "after-commit-experiment",
                    decision,
                    true);
            boolean noDuplicate = replay.status() == TransactionStatus.IDEMPOTENT
                    && records == store.replay().records().size();
            EnterpriseLabAllocationReconciliationGate gate =
                    EnterpriseLabAllocationReconciliationGate.pending();
            var recovery = new EnterpriseLabAllocationReconciler(
                    store,
                    restarted,
                    restartedRouter,
                    authority,
                    gate,
                    clock,
                    restartedRouter::installedSnapshot,
                    checkpoint -> { })
                    .reconcile(ReconciliationTrigger.STARTUP, List.of());
            return crashed
                    && noDuplicate
                    && recovery.ready()
                    && recovery.action() == ReconciliationAction.BASELINE_RESTORED
                    && holder.store().read().routingSnapshot().kind() != Kind.CANDIDATE;
        }
    }

    private static TakeoverEvidence staleOwnerTakeover(Path output) throws IOException {
        String token = randomToken();
        Path root = caseRoot(output, "stale-owner-takeover");
        EnterpriseLabExperimentTargetCatalog catalog =
                EnterpriseLabAllocationProofStateHolder.fixedTargets();
        MutableClock clock = new MutableClock(PROOF_TIME);
        ProofAuthority ownerA = new ProofAuthority(root, "proof-owner-a", 1L);
        EnterpriseLabLoopbackAllocationRouter initial = router(
                catalog, ownerA, clock, Optional.empty());
        try (var holder = EnterpriseLabAllocationProofStateHolder.start(
                output, token, initial.installedSnapshot(), catalog);
                EnterpriseLabAllocationStateStore storeA =
                        EnterpriseLabAllocationStateStore.createOwned(root, catalog, ownerA)) {
            EnterpriseLabLoopbackAllocationRouter routerA = router(
                    catalog, ownerA, clock, Optional.of(holder.store()));
            EnterpriseLabAllocationTransactionCoordinator coordinatorA = coordinator(
                    storeA, routerA, catalog, ownerA, clock, NO_FAILURE);
            coordinatorA.establishSafeBaseline("takeover-baseline");
            coordinatorA.applyCandidate(
                    "takeover-candidate", "takeover-experiment", decision(), true);
            ownerA.fail(FailureClassification.RECORD_REPLACED);

            ProofAuthority ownerB = new ProofAuthority(root, "proof-owner-b", 2L);
            try (EnterpriseLabAllocationStateStore storeB =
                    EnterpriseLabAllocationStateStore.createOwned(root, catalog, ownerB)) {
                EnterpriseLabLoopbackAllocationRouter routerB = router(
                        catalog, ownerB, clock, Optional.of(holder.store()));
                EnterpriseLabAllocationTransactionCoordinator coordinatorB = coordinator(
                        storeB, routerB, catalog, ownerB, clock, NO_FAILURE);
                EnterpriseLabAllocationReconciliationGate gate =
                        EnterpriseLabAllocationReconciliationGate.pending();
                var report = new EnterpriseLabAllocationReconciler(
                        storeB,
                        coordinatorB,
                        routerB,
                        ownerB,
                        gate,
                        clock,
                        routerB::installedSnapshot,
                        checkpoint -> { })
                        .reconcile(ReconciliationTrigger.TAKEOVER, List.of());
                int beforeStaleAttempt = storeB.replay().records().size();
                boolean staleDenied;
                try {
                    coordinatorA.applyCandidate(
                            "stale-process-candidate",
                            "stale-process-experiment",
                            decision(),
                            true);
                    staleDenied = false;
                } catch (EnterpriseLabEvidenceOwnershipException expected) {
                    staleDenied = expected.classification()
                            == FailureClassification.RECORD_REPLACED;
                }
                var replay = storeB.replay();
                EnterpriseLabAllocationState baseline = replay.baseline().orElseThrow();
                EnterpriseLabInstalledAllocationSnapshot installed = holder.store().read();
                boolean takeoverPassed = report.ready()
                        && report.classification()
                        == DriftClassification.STALE_OWNER_GENERATION
                        && report.action() == ReconciliationAction.BASELINE_RESTORED
                        && installed.ownerGeneration() == 2L
                        && installed.routingSnapshot().kind() != Kind.CANDIDATE
                        && replay.chainHead().orElseThrow().ownerGeneration() == 2L;
                return new TakeoverEvidence(
                        takeoverPassed,
                        staleDenied && beforeStaleAttempt == replay.records().size(),
                        holder.alive(),
                        replay.records().size(),
                        installed.ownerGeneration(),
                        installed.routerGeneration(),
                        EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                                baseline.scenarioId(), baseline.baselineAllocation()),
                        installed.allocationFingerprint());
            }
        }
    }

    private static Map<String, String> driftMatrix(Path root) {
        Map<String, String> results = new LinkedHashMap<>();
        results.put("changed-backend-share", drift(root.resolve("share"),
                DriftKind.CHANGED_SHARE));
        results.put("missing-backend", drift(root.resolve("missing"),
                DriftKind.MALFORMED));
        results.put("unexpected-backend", drift(root.resolve("unexpected"),
                DriftKind.MALFORMED));
        results.put("stale-router-generation", drift(root.resolve("router-generation"),
                DriftKind.STALE_ROUTER_GENERATION));
        results.put("stale-owner-generation", drift(root.resolve("owner-generation"),
                DriftKind.STALE_OWNER_GENERATION));
        results.put("invalid-normalization", drift(root.resolve("normalization"),
                DriftKind.MALFORMED));
        results.put("zero-total-allocation", drift(root.resolve("zero-total"),
                DriftKind.MALFORMED));
        results.put("candidate-while-baseline-expected", drift(root.resolve("candidate"),
                DriftKind.CHANGED_SHARE));
        results.put("baseline-while-candidate-committed", drift(root.resolve("reset"),
                DriftKind.BASELINE_AFTER_COMMIT));
        results.put("router-fingerprint-mismatch", drift(root.resolve("fingerprint"),
                DriftKind.MALFORMED));
        return Map.copyOf(results);
    }

    private static String drift(Path root, DriftKind kind) {
        try (Context context = Context.create(root, 1L)) {
            context.coordinator(NO_FAILURE).establishSafeBaseline("drift-baseline");
            EnterpriseLabAllocationTransactionCoordinator.InstalledStateReader reader;
            if (kind == DriftKind.BASELINE_AFTER_COMMIT
                    || kind == DriftKind.STALE_ROUTER_GENERATION) {
                context.coordinator(NO_FAILURE).applyCandidate(
                        "drift-candidate", "drift-experiment", context.decision, true);
                EnterpriseLabInstalledAllocationSnapshot baseline =
                        context.router.baselineInstalledSnapshot();
                reader = () -> baseline;
            } else if (kind == DriftKind.STALE_OWNER_GENERATION) {
                EnterpriseLabLoopbackAllocationSnapshot baseline =
                        context.router.baselineSnapshot();
                EnterpriseLabInstalledAllocationSnapshot stale =
                        EnterpriseLabInstalledAllocationSnapshot.installed(
                                baseline,
                                context.clock,
                                "CONTROLLED_STALE_OWNER_DRIFT",
                                EnterpriseLabInstalledAllocationSnapshot.UNOWNED_GENERATION);
                reader = () -> stale;
            } else if (kind == DriftKind.CHANGED_SHARE) {
                EnterpriseLabLoopbackAllocationSnapshot changed =
                        EnterpriseLabLoopbackAllocationSnapshot.normalized(
                                SCENARIO,
                                1L,
                                "controlled-drift",
                                Kind.CANDIDATE,
                                Set.of("blue", "green", "orange"),
                                Map.of("blue", 0.50d, "green", 0.25d, "orange", 0.25d));
                EnterpriseLabInstalledAllocationSnapshot installed =
                        EnterpriseLabInstalledAllocationSnapshot.installed(
                                changed,
                                context.clock,
                                "CONTROLLED_SHARE_DRIFT",
                                1L);
                reader = () -> installed;
            } else {
                reader = () -> {
                    throw new IllegalArgumentException(
                            "controlled invalid installed allocation evidence");
                };
            }
            EnterpriseLabAllocationTransactionCoordinator coordinator =
                    new EnterpriseLabAllocationTransactionCoordinator(
                            context.store,
                            context.router,
                            context.catalog,
                            context.authority,
                            context.clock,
                            NO_FAILURE,
                            reader);
            EnterpriseLabAllocationReconciliationGate gate =
                    EnterpriseLabAllocationReconciliationGate.pending();
            var report = new EnterpriseLabAllocationReconciler(
                    context.store,
                    coordinator,
                    context.router,
                    context.authority,
                    gate,
                    context.clock,
                    reader::read,
                    checkpoint -> { })
                    .reconcile(ReconciliationTrigger.RUNTIME_CHECKPOINT, List.of());
            if (report.classification() == DriftClassification.SAFE_BASELINE_INSTALLED) {
                throw new IllegalStateException("controlled drift was silently accepted");
            }
            return report.classification().name();
        }
    }

    private static boolean restorationFailure(Path root) {
        try (Context context = Context.create(root, 1L)) {
            context.coordinator(NO_FAILURE).establishSafeBaseline("failure-baseline");
            EnterpriseLabLoopbackAllocationSnapshot changed =
                    EnterpriseLabLoopbackAllocationSnapshot.normalized(
                            SCENARIO,
                            1L,
                            "restoration-failure-drift",
                            Kind.CANDIDATE,
                            Set.of("blue", "green", "orange"),
                            Map.of("blue", 0.50d, "green", 0.25d, "orange", 0.25d));
            EnterpriseLabInstalledAllocationSnapshot candidate =
                    EnterpriseLabInstalledAllocationSnapshot.installed(
                            changed, context.clock, "CONTROLLED_RESTORATION_FAILURE", 1L);
            RejectingStore rejectingStore = new RejectingStore(candidate);
            EnterpriseLabLoopbackAllocationRouter failingRouter = router(
                    context.catalog,
                    context.authority,
                    context.clock,
                    Optional.of(rejectingStore));
            EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator(
                    context.store,
                    failingRouter,
                    context.catalog,
                    context.authority,
                    context.clock,
                    NO_FAILURE);
            EnterpriseLabAllocationReconciliationGate gate =
                    EnterpriseLabAllocationReconciliationGate.pending();
            int evidenceBefore = context.store.replay().records().size();
            var report = new EnterpriseLabAllocationReconciler(
                    context.store,
                    coordinator,
                    failingRouter,
                    context.authority,
                    gate,
                    context.clock,
                    failingRouter::installedSnapshot,
                    checkpoint -> { })
                    .reconcile(ReconciliationTrigger.OPERATOR_VERIFICATION, List.of());
            return !report.ready()
                    && !gate.admissionAllowed()
                    && rejectingStore.read().equals(candidate)
                    && context.store.replay().records().size() >= evidenceBefore
                    && report.action() != ReconciliationAction.BASELINE_RESTORED;
        }
    }

    private static boolean repeatedReconciliation(Path root) {
        try (Context context = Context.create(root, 1L)) {
            EnterpriseLabAllocationReconciler reconciler = context.reconciler();
            var first = reconciler.reconcile(ReconciliationTrigger.STARTUP, List.of());
            int records = context.store.replay().records().size();
            EnterpriseLabInstalledAllocationSnapshot installed =
                    context.router.installedSnapshot();
            for (int index = 0; index < 5; index++) {
                var repeated = reconciler.reconcile(
                        ReconciliationTrigger.RUNTIME_CHECKPOINT, List.of());
                if (!repeated.ready()
                        || repeated.action() != ReconciliationAction.VERIFIED_NO_OP
                        || context.store.replay().records().size() != records
                        || !context.router.installedSnapshot().equals(installed)) {
                    return false;
                }
            }
            return first.ready();
        }
    }

    private static EnterpriseLabAllocationTransactionCoordinator coordinator(
            EnterpriseLabAllocationStateStore store,
            EnterpriseLabLoopbackAllocationRouter router,
            EnterpriseLabExperimentTargetCatalog catalog,
            EnterpriseLabEvidenceMutationAuthority authority,
            Clock clock,
            EnterpriseLabAllocationTransactionCoordinator.FailureInjector failure) {
        return new EnterpriseLabAllocationTransactionCoordinator(
                store,
                router,
                catalog,
                authority,
                clock,
                failure,
                router::installedSnapshot);
    }

    private static EnterpriseLabLoopbackAllocationRouter router(
            EnterpriseLabExperimentTargetCatalog catalog,
            EnterpriseLabEvidenceMutationAuthority authority,
            Clock clock,
            Optional<EnterpriseLabLoopbackAllocationRouter.InstalledStateStore> store) {
        var targets = catalog.findTargets(SCENARIO).orElseThrow();
        EnterpriseLabLoopbackObservationIngress ingress =
                new EnterpriseLabLoopbackObservationIngress(
                        Set.of("blue", "green", "orange"));
        Map<String, Double> baseline = decision().decision()
                .guardrailDecision().baselineAllocations();
        return store
                .map(value -> new EnterpriseLabLoopbackAllocationRouter(
                        targets,
                        ingress,
                        baseline,
                        Optional.of(authority),
                        clock,
                        value))
                .orElseGet(() -> new EnterpriseLabLoopbackAllocationRouter(
                        targets,
                        ingress,
                        baseline,
                        Optional.of(authority),
                        clock));
    }

    private static EnterpriseLabAdaptiveDecision decision() {
        return new EnterpriseLabAdaptiveDecisionService().decide(
                SCENARIO, "active-experiment", true, false, false);
    }

    private static Path caseRoot(Path output, String name) {
        Path root = output.resolve("allocation-proof-evidence")
                .resolve(name).toAbsolutePath().normalize();
        if (!root.startsWith(output.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException(
                    "allocation proof case root escaped the output boundary");
        }
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "allocation proof case root could not be created", exception);
        }
        return root;
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return java.util.HexFormat.of().formatHex(bytes);
    }

    private static final EnterpriseLabAllocationTransactionCoordinator.FailureInjector
            NO_FAILURE = checkpoint -> { };

    private enum DriftKind {
        CHANGED_SHARE,
        STALE_ROUTER_GENERATION,
        STALE_OWNER_GENERATION,
        BASELINE_AFTER_COMMIT,
        MALFORMED
    }

    private record NormalEvidence(
            boolean passed,
            String baselineFingerprint,
            String installedFingerprint) {
    }

    private record ExternalEvidence(boolean passed, boolean separateProcessObserved) {
    }

    private record TakeoverEvidence(
            boolean takeoverPassed,
            boolean staleMutationDenied,
            boolean separateProcessObserved,
            int recordCount,
            long ownerGeneration,
            long routerGeneration,
            String baselineFingerprint,
            String installedFingerprint) {
    }

    private static final class SimulatedCrash extends RuntimeException {
        private final Checkpoint checkpoint;

        private SimulatedCrash(Checkpoint checkpoint) {
            super("controlled proof crash at " + checkpoint);
            this.checkpoint = checkpoint;
        }
    }

    private static final class RejectingStore
            implements EnterpriseLabLoopbackAllocationRouter.InstalledStateStore {
        private final EnterpriseLabInstalledAllocationSnapshot installed;

        private RejectingStore(EnterpriseLabInstalledAllocationSnapshot installed) {
            this.installed = installed;
        }

        @Override
        public EnterpriseLabInstalledAllocationSnapshot read() {
            return installed;
        }

        @Override
        public boolean compareAndSet(
                EnterpriseLabInstalledAllocationSnapshot expected,
                EnterpriseLabInstalledAllocationSnapshot update) {
            return false;
        }
    }

    private static final class ProofAuthority
            implements EnterpriseLabEvidenceMutationAuthority {
        private final Path trustedRoot;
        private final String ownerId;
        private final long generation;
        private FailureClassification failure;

        private ProofAuthority(
                Path trustedRoot,
                String ownerId,
                long generation) {
            this.trustedRoot = trustedRoot.toAbsolutePath().normalize();
            this.ownerId = ownerId;
            this.generation = generation;
        }

        private void fail(FailureClassification classification) {
            this.failure = classification;
        }

        @Override
        public MutationAuthorization requireMutationAuthorization() {
            if (failure != null) {
                throw new EnterpriseLabEvidenceOwnershipException(
                        failure, "controlled proof ownership loss");
            }
            return new MutationAuthorization(trustedRoot, ownerId, generation);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone)) {
                throw new IllegalArgumentException("proof clock supports UTC only");
            }
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private static final class Context implements AutoCloseable {
        private final EnterpriseLabExperimentTargetCatalog catalog;
        private final ProofAuthority authority;
        private final MutableClock clock;
        private final EnterpriseLabAdaptiveDecision decision;
        private final EnterpriseLabLoopbackAllocationRouter router;
        private final EnterpriseLabAllocationStateStore store;

        private Context(
                EnterpriseLabExperimentTargetCatalog catalog,
                ProofAuthority authority,
                MutableClock clock,
                EnterpriseLabAdaptiveDecision decision,
                EnterpriseLabLoopbackAllocationRouter router,
                EnterpriseLabAllocationStateStore store) {
            this.catalog = catalog;
            this.authority = authority;
            this.clock = clock;
            this.decision = decision;
            this.router = router;
            this.store = store;
        }

        private static Context create(Path root, long generation) {
            try {
                Files.createDirectories(root);
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "allocation proof context root could not be created", exception);
            }
            EnterpriseLabExperimentTargetCatalog catalog =
                    EnterpriseLabAllocationProofStateHolder.fixedTargets();
            ProofAuthority authority = new ProofAuthority(
                    root, "proof-owner-" + generation, generation);
            MutableClock clock = new MutableClock(PROOF_TIME);
            EnterpriseLabLoopbackAllocationRouter router =
                    EnterpriseLabAllocationProofRunner.router(
                            catalog, authority, clock, Optional.empty());
            EnterpriseLabAllocationStateStore store =
                    EnterpriseLabAllocationStateStore.createOwned(
                            root, catalog, authority);
            return new Context(catalog, authority, clock, decision(), router, store);
        }

        private EnterpriseLabAllocationTransactionCoordinator coordinator(
                EnterpriseLabAllocationTransactionCoordinator.FailureInjector failure) {
            return EnterpriseLabAllocationProofRunner.coordinator(
                    store, router, catalog, authority, clock, failure);
        }

        private EnterpriseLabAllocationReconciler reconciler() {
            EnterpriseLabAllocationReconciliationGate gate =
                    EnterpriseLabAllocationReconciliationGate.pending();
            return new EnterpriseLabAllocationReconciler(
                    store,
                    coordinator(NO_FAILURE),
                    router,
                    authority,
                    gate,
                    clock,
                    router::installedSnapshot,
                    checkpoint -> { });
        }

        @Override
        public void close() {
            store.close();
        }
    }
}
