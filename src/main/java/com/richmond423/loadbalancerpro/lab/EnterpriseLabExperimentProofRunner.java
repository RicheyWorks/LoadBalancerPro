package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.ServerDegradationState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluation.Disposition;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluation.Trigger;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.ArmRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.OperatorReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.OperatorStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.RequestBatchReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.RequestBatchRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentProofReport.ScenarioEvidence;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Foreground, synchronous end-to-end proof that composes the shipped PR1-PR5 experiment stack.
 */
public final class EnterpriseLabExperimentProofRunner {
    public static final String SCENARIO_ID = "tail-latency-pressure";
    public static final int BACKEND_COUNT = 3;
    private static final Instant PROOF_EPOCH = Instant.parse("2026-07-16T22:00:00Z");
    private static final Duration NORMAL_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration TIMEOUT_PROOF_LIMIT = Duration.ofMillis(25);
    private final Clock reportClock;

    public EnterpriseLabExperimentProofRunner() {
        this(Clock.systemUTC());
    }

    EnterpriseLabExperimentProofRunner(Clock reportClock) {
        this.reportClock = Objects.requireNonNull(reportClock, "reportClock cannot be null");
    }

    public EnterpriseLabExperimentProofReport run(String requestedSuite) throws IOException {
        Suite suite = Suite.from(requestedSuite);
        List<ScenarioEvidence> evidence = new ArrayList<>();
        if (suite.includesCompletion()) {
            evidence.add(stableCompletion());
            evidence.add(transientRecoveryCompletion());
            evidence.add(durationCompletion());
        }
        if (suite.includesRollback()) {
            evidence.add(latencyRollback());
            evidence.add(failureRollback());
            evidence.add(timeoutRollback());
            evidence.add(partialDegradationRollback());
            evidence.add(holdDegradationRollback());
            evidence.add(insufficientEvidenceRollback());
            evidence.add(staleEvidenceRollback());
            evidence.add(operatorCancellationRollback());
            evidence.add(requestLimitRollback());
            evidence.add(shutdownRollback());
        }
        return EnterpriseLabExperimentProofReport.create(
                reportClock.instant(),
                suite.wireValue,
                BACKEND_COUNT,
                evidence);
    }

    private ScenarioEvidence stableCompletion() throws IOException {
        try (ProofSession proof = ProofSession.open("stable-completion", 60,
                Duration.ofSeconds(30), 15, 2)) {
            proof.baseline(60);
            OperatorReceipt started = proof.start();
            OperatorReceipt replayedStart = proof.service.start(proof.experimentId, proof.startRequestId, true);
            require(replayedStart.status() == OperatorStatus.IDEMPOTENT,
                    "stable proof start replay was not idempotent");
            require(started.experimentRecord().orElseThrow().configuration().candidateDecision().decision()
                            .guardrailDecision().influenceAllowed(),
                    "stable proof did not receive bounded guardrail approval");
            proof.candidate(60, NORMAL_TIMEOUT);
            OperatorReceipt firstEvaluation = proof.evaluate("evaluate-stable-1");
            require(firstEvaluation.experimentRecord().orElseThrow().lifecycle().state()
                            == EnterpriseLabExperimentState.HOLDING,
                    "stable proof did not enter bounded hold-down");
            require(lastDisposition(firstEvaluation) == Disposition.CONTINUE_HOLDING,
                    "stable proof did not retain healthy hold evidence");
            OperatorReceipt replayedEvaluation = proof.evaluate("evaluate-stable-1");
            require(replayedEvaluation.status() == OperatorStatus.IDEMPOTENT,
                    "stable proof evaluation replay was not idempotent");
            OperatorReceipt completed = proof.evaluate("evaluate-stable-2");
            EnterpriseLabExperimentOperatorRecord record = terminal(completed, EnterpriseLabExperimentState.COMPLETED);
            require(record.evaluations().stream().map(EnterpriseLabExperimentEvaluation::disposition)
                            .toList().equals(List.of(Disposition.CONTINUE_HOLDING, Disposition.COMPLETED)),
                    "stable proof did not retain hold and completion evaluations");
            return proof.evidence(
                    "healthy request boundary enters hold-down and completes with baseline restoration",
                    true,
                    true,
                    List.of(
                            "real baseline and candidate loopback requests were observed",
                            "adaptive allocation received bounded guardrail approval before explicit application",
                            "hold-down evaluation completed before baseline restoration"),
                    record);
        }
    }

    private ScenarioEvidence transientRecoveryCompletion() throws IOException {
        try (ProofSession proof = ProofSession.open("transient-recovery-completion", 60,
                Duration.ofSeconds(30), 15, 1)) {
            proof.baseline(60);
            proof.start();
            String highestShareBackend = proof.highestCandidateShareBackend();
            proof.cluster.setMode(highestShareBackend, BackendMode.FIRST_FAILURE_THEN_SUCCESS);
            proof.candidate(60, NORMAL_TIMEOUT);
            EnterpriseLabExperimentOperatorRecord record = terminal(
                    proof.evaluate("evaluate-recovery"), EnterpriseLabExperimentState.COMPLETED);
            EnterpriseLabExperimentEvaluation evaluation = record.evaluations().get(0);
            var recoveredBackend = evaluation.backendEvidence().get(highestShareBackend);
            require(recoveredBackend.failureCount() == 1,
                    "recovery proof did not retain the scripted real HTTP failure");
            require(recoveredBackend.degradationState() == ServerDegradationState.HEALTHY,
                    "recovery proof did not return the transiently failed backend to healthy evidence");
            return proof.evidence(
                    "one bounded real failure recovers inside policy and completes",
                    false,
                    true,
                    List.of(
                            "candidate evidence retained one actual HTTP failure",
                            "subsequent successes returned the backend to healthy evidence",
                            "aggregate evidence stayed inside rollback policy and completed"),
                    record);
        }
    }

    private ScenarioEvidence durationCompletion() throws IOException {
        try (ProofSession proof = ProofSession.open("duration-completion", 64,
                Duration.ofSeconds(1), 15, 1)) {
            proof.baseline(60);
            proof.start();
            proof.candidate(60, NORMAL_TIMEOUT);
            proof.clock.advance(Duration.ofMillis(800));
            EnterpriseLabExperimentOperatorRecord record = terminal(
                    proof.evaluate("evaluate-duration"), EnterpriseLabExperimentState.COMPLETED);
            require(record.lifecycle().requestCount() == 60
                            && record.lifecycle().requestCount() < record.configuration().maximumRequestCount(),
                    "duration proof reached the request boundary instead of the duration boundary");
            return proof.evidence(
                    "fresh healthy evidence completes at the configured duration boundary",
                    false,
                    true,
                    List.of(
                            "duration boundary was reached before request capacity",
                            "evidence remained fresh at evaluation",
                            "completion restored the recorded baseline"),
                    record);
        }
    }

    private ScenarioEvidence latencyRollback() throws IOException {
        try (ProofSession proof = ProofSession.open("tail-latency-rollback", 60,
                Duration.ofSeconds(30), 15, 1)) {
            proof.nanos.setMeasuredLatency(Duration.ofMillis(5));
            proof.baseline(60);
            proof.start();
            proof.nanos.setMeasuredLatency(Duration.ofMillis(20));
            proof.candidate(60, NORMAL_TIMEOUT);
            EnterpriseLabExperimentOperatorRecord record = terminal(
                    proof.evaluate("evaluate-latency"), EnterpriseLabExperimentState.ROLLED_BACK);
            require(hasTrigger(record, Trigger.LATENCY_REGRESSION),
                    "latency proof did not record the latency-regression rollback trigger");
            return proof.evidence(
                    "elevated p95 and p99 loopback latency automatically rolls back",
                    false,
                    true,
                    List.of(
                            "actual loopback responses supplied candidate outcomes",
                            "injected monotonic measurement recorded a four-times tail regression",
                            "policy evaluation restored the baseline"),
                    record);
        }
    }

    private ScenarioEvidence failureRollback() throws IOException {
        try (ProofSession proof = ProofSession.open("failure-rate-rollback", 12,
                Duration.ofSeconds(30), 6, 1)) {
            proof.baseline(12);
            proof.start();
            proof.cluster.setAllModes(BackendMode.FAILURE);
            proof.candidate(12, NORMAL_TIMEOUT);
            OperatorReceipt rolledBack = proof.evaluate("evaluate-failure");
            EnterpriseLabExperimentOperatorRecord firstRecord = terminal(
                    rolledBack, EnterpriseLabExperimentState.ROLLED_BACK);
            require(hasTrigger(firstRecord, Trigger.FAILURE_RATE),
                    "failure proof did not record the failure-rate rollback trigger");
            OperatorReceipt replay = proof.evaluate("evaluate-failure");
            require(replay.status() == OperatorStatus.IDEMPOTENT,
                    "failure rollback replay was not idempotent");
            OperatorReceipt terminalEvaluation = proof.evaluate("evaluate-after-rollback");
            EnterpriseLabExperimentOperatorRecord record = terminal(
                    terminalEvaluation, EnterpriseLabExperimentState.ROLLED_BACK);
            require(lastDisposition(terminalEvaluation) == Disposition.TERMINAL_NO_CHANGE
                            && !terminalEvaluation.trafficActionPerformed(),
                    "repeated rollback evaluation altered terminal routing state");
            return proof.evidence(
                    "real HTTP failures automatically roll back exactly once",
                    true,
                    true,
                    List.of(
                            "all candidate requests returned real HTTP failures",
                            "failure-rate policy restored the baseline",
                            "idempotent and new terminal evaluations performed no second traffic action"),
                    record);
        }
    }

    private ScenarioEvidence timeoutRollback() throws IOException {
        try (ProofSession proof = ProofSession.open("timeout-rate-rollback", 12,
                Duration.ofSeconds(30), 6, 1)) {
            proof.baseline(12);
            proof.start();
            proof.cluster.setAllModes(BackendMode.TIMEOUT);
            proof.candidate(12, TIMEOUT_PROOF_LIMIT);
            EnterpriseLabExperimentOperatorRecord record = terminal(
                    proof.evaluate("evaluate-timeout"), EnterpriseLabExperimentState.ROLLED_BACK);
            require(hasTrigger(record, Trigger.TIMEOUT_RATE),
                    "timeout proof did not record the timeout-rate rollback trigger");
            require(proof.outcomeCounts.getOrDefault("TIMEOUT", 0) == 12,
                    "timeout proof did not retain all bounded timeout outcomes");
            return proof.evidence(
                    "bounded real request timeouts automatically roll back",
                    false,
                    true,
                    List.of(
                            "loopback handlers were held by a bounded latch beyond the client limit",
                            "all candidate requests were recorded as timeouts",
                            "timeout-rate policy restored the baseline"),
                    record);
        }
    }

    private ScenarioEvidence partialDegradationRollback() throws IOException {
        try (ProofSession proof = ProofSession.open("partial-degradation-rollback", 60,
                Duration.ofSeconds(30), 15, 1)) {
            proof.baseline(60);
            proof.start();
            proof.cluster.setMode(proof.highestCandidateShareBackend(), BackendMode.PERIODIC_FAILURE);
            proof.candidate(60, NORMAL_TIMEOUT);
            EnterpriseLabExperimentOperatorRecord record = terminal(
                    proof.evaluate("evaluate-partial"), EnterpriseLabExperimentState.ROLLED_BACK);
            require(hasTrigger(record, Trigger.PARTIAL_DEGRADATION),
                    "partial-degradation proof did not record its structured rollback trigger");
            return proof.evidence(
                    "one intermittently failing backend triggers partial-degradation rollback",
                    false,
                    true,
                    List.of(
                            "one high-share backend returned periodic real HTTP failures",
                            "failures remained non-consecutive and below aggregate failure policy",
                            "partial backend degradation independently restored the baseline"),
                    record);
        }
    }

    private ScenarioEvidence holdDegradationRollback() throws IOException {
        try (ProofSession proof = ProofSession.open("hold-degradation-rollback", 64,
                Duration.ofSeconds(1), 15, 2)) {
            proof.baseline(60);
            proof.start();
            proof.candidate(45, NORMAL_TIMEOUT);
            proof.clock.advance(Duration.ofMillis(800));
            OperatorReceipt holding = proof.evaluate("evaluate-hold-healthy");
            require(holding.experimentRecord().orElseThrow().lifecycle().state()
                            == EnterpriseLabExperimentState.HOLDING
                            && lastDisposition(holding) == Disposition.CONTINUE_HOLDING,
                    "hold-degradation proof did not enter healthy hold-down");
            proof.cluster.setAllModes(BackendMode.FAILURE);
            proof.candidate(19, NORMAL_TIMEOUT);
            EnterpriseLabExperimentOperatorRecord record = terminal(
                    proof.evaluate("evaluate-hold-degraded"), EnterpriseLabExperimentState.ROLLED_BACK);
            require(hasTrigger(record, Trigger.FAILURE_RATE),
                    "hold degradation did not trigger harmful rollback");
            return proof.evidence(
                    "healthy hold-down degrades and automatically rolls back",
                    false,
                    true,
                    List.of(
                            "fresh healthy evidence first entered hold-down",
                            "additional bounded hold traffic returned real HTTP failures",
                            "the next evaluation restored baseline instead of completing"),
                    record);
        }
    }

    private ScenarioEvidence insufficientEvidenceRollback() throws IOException {
        try (ProofSession proof = ProofSession.open("insufficient-evidence-rollback", 4,
                Duration.ofSeconds(30), 4, 1)) {
            proof.baseline(4);
            proof.start();
            proof.candidate(4, NORMAL_TIMEOUT);
            EnterpriseLabExperimentOperatorRecord record = terminal(
                    proof.evaluate("evaluate-insufficient"), EnterpriseLabExperimentState.ROLLED_BACK);
            require(hasTrigger(record, Trigger.SPARSE_EVIDENCE_AT_BOUNDARY),
                    "insufficient proof did not record sparse boundary evidence");
            return proof.evidence(
                    "sparse per-backend evidence at request boundary rolls back conservatively",
                    false,
                    true,
                    List.of(
                            "all configured requests used real loopback targets",
                            "bounded allocation left per-backend evidence below tail minimums",
                            "insufficient evidence restored the baseline"),
                    record);
        }
    }

    private ScenarioEvidence staleEvidenceRollback() throws IOException {
        try (ProofSession proof = ProofSession.open("stale-evidence-rollback", 15,
                Duration.ofMinutes(5), 15, 1)) {
            proof.baseline(15);
            proof.start();
            proof.candidate(15, NORMAL_TIMEOUT);
            proof.clock.advance(Duration.ofSeconds(31));
            EnterpriseLabExperimentOperatorRecord record = terminal(
                    proof.evaluate("evaluate-stale"), EnterpriseLabExperimentState.ROLLED_BACK);
            require(hasTrigger(record, Trigger.STALE_EVIDENCE),
                    "stale proof did not record the stale-evidence rollback trigger");
            return proof.evidence(
                    "candidate evidence older than the local window rolls back conservatively",
                    false,
                    true,
                    List.of(
                            "candidate outcomes were initially captured from real loopback requests",
                            "deterministic clock advancement exceeded the thirty-second evidence age",
                            "stale evidence restored the baseline"),
                    record);
        }
    }

    private ScenarioEvidence operatorCancellationRollback() throws IOException {
        try (ProofSession proof = ProofSession.open("operator-cancel-rollback", 12,
                Duration.ofSeconds(30), 6, 1)) {
            proof.baseline(12);
            proof.start();
            OperatorReceipt cancelled = proof.service.cancel(
                    proof.experimentId, "cancel-operator", "bounded proof cancellation");
            EnterpriseLabExperimentOperatorRecord record = terminal(
                    cancelled, EnterpriseLabExperimentState.ROLLED_BACK);
            OperatorReceipt replay = proof.service.cancel(
                    proof.experimentId, "cancel-operator", "bounded proof cancellation");
            require(replay.status() == OperatorStatus.IDEMPOTENT
                            && replay.experimentRecord().orElseThrow().contentFingerprint()
                                    .equals(record.contentFingerprint()),
                    "operator cancellation replay was not immutable and idempotent");
            return proof.evidence(
                    "explicit operator cancellation restores baseline and is idempotent",
                    true,
                    true,
                    List.of(
                            "real baseline observations preceded explicit candidate application",
                            "cancellation used the evaluator rollback path",
                            "repeated cancellation retained the same immutable final fingerprint"),
                    record);
        }
    }

    private ScenarioEvidence requestLimitRollback() throws IOException {
        try (ProofSession proof = ProofSession.open("request-limit-rollback", 3,
                Duration.ofSeconds(30), 1, 1)) {
            proof.baseline(3);
            proof.start();
            RequestBatchReceipt denied = proof.service.executeRequests(
                    proof.experimentId,
                    new RequestBatchRequest("candidate-over-limit", 4, NORMAL_TIMEOUT),
                    true);
            require(denied.status() == OperatorStatus.DENIED
                            && "REQUEST_LIMIT_EXCEEDED".equals(denied.reasonCode())
                            && denied.sentCount() == 0
                            && !denied.trafficActionPerformed(),
                    "request limit proof did not deny traffic before transport");
            EnterpriseLabExperimentOperatorRecord record = terminal(
                    proof.service.cancel(proof.experimentId, "cancel-after-limit", "request bound reached"),
                    EnterpriseLabExperimentState.ROLLED_BACK);
            return proof.evidence(
                    "over-limit request batch is denied before transport and candidate is rolled back",
                    false,
                    true,
                    List.of(
                            "bounded baseline requests reached literal loopback targets",
                            "over-limit candidate batch sent zero requests",
                            "explicit cleanup restored the baseline"),
                    record);
        }
    }

    private ScenarioEvidence shutdownRollback() throws IOException {
        try (ProofSession proof = ProofSession.open("shutdown-rollback", 12,
                Duration.ofSeconds(30), 6, 1)) {
            proof.baseline(12);
            proof.start();
            proof.service.close();
            EnterpriseLabExperimentOperatorRecord record = proof.service.findFinalRecord(proof.experimentId)
                    .orElseThrow(() -> new IllegalStateException("shutdown did not retain a final record"));
            require(record.lifecycle().state() == EnterpriseLabExperimentState.ROLLED_BACK
                            && record.operatorActions().stream().anyMatch(action -> action.operation().equals("shutdown")),
                    "shutdown did not restore baseline through its bounded terminal action");
            proof.service.close();
            return proof.evidence(
                    "incomplete active experiment restores baseline on service shutdown",
                    true,
                    true,
                    List.of(
                            "real baseline observations preceded candidate installation",
                            "service close recorded a shutdown rollback action",
                            "repeated close performed no additional traffic action"),
                    record);
        }
    }

    private static EnterpriseLabExperimentOperatorRecord terminal(
            OperatorReceipt receipt,
            EnterpriseLabExperimentState expectedState) {
        require(receipt.status() == OperatorStatus.RECORDED || receipt.status() == OperatorStatus.APPLIED,
                "terminal operation was not recorded");
        EnterpriseLabExperimentOperatorRecord record = receipt.experimentRecord().orElseThrow();
        require(record.lifecycle().state() == expectedState,
                "unexpected terminal state: " + record.lifecycle().state() + "; evaluations=" + record.evaluations());
        require(record.lifecycle().terminal() && record.completedAt().isPresent(),
                "terminal record is incomplete");
        require(record.currentAllocation().kind() == Kind.RESTORED_BASELINE
                        || record.currentAllocation().kind() == Kind.BASELINE,
                "terminal record retained candidate allocation");
        require(EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                        record.currentAllocation().allocations(),
                        record.configuration().baselineSnapshot().allocations()),
                "terminal record did not restore baseline allocations");
        return record;
    }

    private static Disposition lastDisposition(OperatorReceipt receipt) {
        List<EnterpriseLabExperimentEvaluation> evaluations = receipt.experimentRecord().orElseThrow().evaluations();
        if (evaluations.isEmpty()) {
            throw new IllegalStateException("operator record did not retain evaluation evidence");
        }
        return evaluations.get(evaluations.size() - 1).disposition();
    }

    private static boolean hasTrigger(EnterpriseLabExperimentOperatorRecord record, Trigger trigger) {
        return record.evaluations().stream().anyMatch(evaluation -> evaluation.triggers().contains(trigger));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private enum Suite {
        COMPLETION("completion"),
        ROLLBACK("rollback"),
        ALL("all");

        private final String wireValue;

        Suite(String wireValue) {
            this.wireValue = wireValue;
        }

        private static Suite from(String value) {
            if (value == null || value.isBlank()) {
                return ALL;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (Suite suite : values()) {
                if (suite.wireValue.equals(normalized)) {
                    return suite;
                }
            }
            throw new IllegalArgumentException("experiment proof suite must be completion, rollback, or all");
        }

        private boolean includesCompletion() {
            return this == COMPLETION || this == ALL;
        }

        private boolean includesRollback() {
            return this == ROLLBACK || this == ALL;
        }
    }

    private enum BackendMode {
        SUCCESS,
        FAILURE,
        TIMEOUT,
        FIRST_FAILURE_THEN_SUCCESS,
        PERIODIC_FAILURE
    }

    private static final class ProofSession implements AutoCloseable {
        private final String proofId;
        private final String experimentId;
        private final String startRequestId;
        private final LoopbackCluster cluster;
        private final MutableClock clock;
        private final AdjustableMonotonicClock nanos;
        private final EnterpriseLabExperimentOperatorService service;
        private final Map<String, Integer> outcomeCounts = new LinkedHashMap<>();
        private int actualRequestCount;
        private int batchSequence;

        private ProofSession(
                String proofId,
                LoopbackCluster cluster,
                MutableClock clock,
                AdjustableMonotonicClock nanos,
                EnterpriseLabExperimentOperatorService service) {
            this.proofId = proofId;
            this.experimentId = "proof-" + proofId;
            this.startRequestId = "start-" + proofId;
            this.cluster = cluster;
            this.clock = clock;
            this.nanos = nanos;
            this.service = service;
        }

        private static ProofSession open(
                String proofId,
                int maximumRequests,
                Duration maximumDuration,
                int minimumEvidence,
                int holdCycles) throws IOException {
            LoopbackCluster cluster = LoopbackCluster.start();
            try {
                EnterpriseLabExperimentTargetCatalog targets = new EnterpriseLabExperimentTargetCatalog(
                        cluster.backends.stream()
                                .map(backend -> new EnterpriseLabLoopbackTarget(
                                        SCENARIO_ID, backend.backendId, backend.uri()))
                                .toList());
                MutableClock clock = new MutableClock(PROOF_EPOCH);
                AdjustableMonotonicClock nanos = new AdjustableMonotonicClock(Duration.ofMillis(5));
                EnterpriseLabExperimentOperatorService service = new EnterpriseLabExperimentOperatorService(
                        targets,
                        new EnterpriseLabScenarioCatalogService(),
                        new EnterpriseLabAdaptiveDecisionService(),
                        clock,
                        nanos::next,
                        1);
                ProofSession proof = new ProofSession(proofId, cluster, clock, nanos, service);
                Duration expiration = maximumDuration.plus(Duration.ofMinutes(1));
                OperatorReceipt armed = service.arm(new ArmRequest(
                        "arm-" + proofId,
                        proof.experimentId,
                        SCENARIO_ID,
                        maximumRequests,
                        maximumDuration,
                        minimumEvidence,
                        holdCycles,
                        expiration), true);
                require(armed.status() == OperatorStatus.APPLIED
                                && armed.experimentRecord().orElseThrow().lifecycle().state()
                                        == EnterpriseLabExperimentState.ARMED,
                        "proof experiment did not arm");
                return proof;
            } catch (RuntimeException exception) {
                cluster.close();
                throw exception;
            }
        }

        private void baseline(int count) {
            sendBatches(count, false, NORMAL_TIMEOUT);
        }

        private OperatorReceipt start() {
            clock.advance(Duration.ofMillis(100));
            cluster.resetCounters();
            OperatorReceipt receipt = service.start(experimentId, startRequestId, true);
            require(receipt.status() == OperatorStatus.APPLIED
                            && receipt.experimentRecord().orElseThrow().lifecycle().state()
                                    == EnterpriseLabExperimentState.RUNNING
                            && receipt.experimentRecord().orElseThrow().currentAllocation().kind() == Kind.CANDIDATE,
                    "proof candidate allocation did not start");
            return receipt;
        }

        private void candidate(int count, Duration timeout) {
            sendBatches(count, true, timeout);
        }

        private void sendBatches(int count, boolean candidate, Duration timeout) {
            int remaining = count;
            while (remaining > 0) {
                int batchCount = Math.min(EnterpriseLabExperimentOperatorService.MAX_REQUESTS_PER_BATCH, remaining);
                String phase = candidate ? "candidate" : "baseline";
                RequestBatchReceipt receipt = service.executeRequests(
                        experimentId,
                        new RequestBatchRequest(
                                phase + "-" + proofId + "-" + (++batchSequence),
                                batchCount,
                                timeout),
                        true);
                require(receipt.status() == OperatorStatus.RECORDED
                                && receipt.sentCount() == batchCount
                                && receipt.observationsRecorded() == batchCount,
                        "proof request batch was not fully observed");
                if (candidate) {
                    require(receipt.candidateRequestsRecorded() == batchCount,
                            "candidate request batch was not correlated with lifecycle evidence");
                } else {
                    require(receipt.candidateRequestsRecorded() == 0,
                            "baseline request batch was incorrectly counted as candidate evidence");
                }
                recordOutcomes(receipt);
                clock.advance(Duration.ofMillis(100));
                remaining -= batchCount;
            }
        }

        private void recordOutcomes(RequestBatchReceipt receipt) {
            receipt.outcomes().stream().filter(value -> value.requestSent()).forEach(value -> {
                actualRequestCount++;
                outcomeCounts.merge(value.outcome(), 1, Integer::sum);
            });
        }

        private OperatorReceipt evaluate(String requestId) {
            return service.evaluate(experimentId, requestId, true);
        }

        private String highestCandidateShareBackend() {
            return service.findRecord(experimentId).orElseThrow().configuration().candidateDecision().decision()
                    .guardrailDecision().effectiveAllocations().entrySet().stream()
                    .max(Map.Entry.<String, Double>comparingByValue().thenComparing(Map.Entry::getKey))
                    .orElseThrow()
                    .getKey();
        }

        private ScenarioEvidence evidence(
                String expectedOutcome,
                boolean idempotencyVerified,
                boolean boundVerified,
                List<String> checks,
                EnterpriseLabExperimentOperatorRecord record) {
            EnterpriseLabExperimentOperatorRecord retained = service.findFinalRecord(experimentId).orElseThrow();
            require(retained.contentFingerprint().equals(record.contentFingerprint()),
                    "final record lookup did not retain the immutable terminal fingerprint");
            boolean derivedBoundsVerified = boundVerified
                    && retained.lifecycle().requestCount() <= retained.configuration().maximumRequestCount()
                    && actualRequestCount <= retained.configuration().maximumRequestCount() * 2
                    && retained.operatorActions().size() <= EnterpriseLabExperimentOperatorRecord.MAX_OPERATOR_ACTIONS
                    && retained.evaluations().size() <= EnterpriseLabExperimentEvaluator.MAX_RETAINED_EVALUATIONS;
            return ScenarioEvidence.create(
                    proofId,
                    expectedOutcome,
                    actualRequestCount,
                    outcomeCounts,
                    idempotencyVerified,
                    derivedBoundsVerified,
                    checks,
                    retained);
        }

        @Override
        public void close() {
            service.close();
            cluster.close();
        }
    }

    private static final class LoopbackCluster implements AutoCloseable {
        private final List<LoopbackBackend> backends;
        private final CountDownLatch timeoutRelease;

        private LoopbackCluster(List<LoopbackBackend> backends, CountDownLatch timeoutRelease) {
            this.backends = List.copyOf(backends);
            this.timeoutRelease = timeoutRelease;
        }

        private static LoopbackCluster start() throws IOException {
            CountDownLatch timeoutRelease = new CountDownLatch(1);
            List<LoopbackBackend> started = new ArrayList<>();
            try {
                for (String backendId : List.of("blue", "green", "orange")) {
                    started.add(LoopbackBackend.start(backendId, timeoutRelease));
                }
                return new LoopbackCluster(started, timeoutRelease);
            } catch (IOException | RuntimeException exception) {
                started.forEach(LoopbackBackend::close);
                timeoutRelease.countDown();
                throw exception;
            }
        }

        private void setAllModes(BackendMode mode) {
            backends.forEach(backend -> backend.setMode(mode));
        }

        private void setMode(String backendId, BackendMode mode) {
            LoopbackBackend backend = backends.stream()
                    .filter(value -> value.backendId.equals(backendId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown proof backend"));
            backend.setMode(mode);
        }

        private void resetCounters() {
            backends.forEach(LoopbackBackend::resetCounter);
        }

        @Override
        public void close() {
            timeoutRelease.countDown();
            backends.forEach(LoopbackBackend::close);
        }
    }

    private static final class LoopbackBackend implements AutoCloseable {
        private final String backendId;
        private final HttpServer server;
        private final CountDownLatch timeoutRelease;
        private BackendMode mode = BackendMode.SUCCESS;
        private int requestCount;

        private LoopbackBackend(String backendId, HttpServer server, CountDownLatch timeoutRelease) {
            this.backendId = backendId;
            this.server = server;
            this.timeoutRelease = timeoutRelease;
        }

        private static LoopbackBackend start(String backendId, CountDownLatch timeoutRelease) throws IOException {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            LoopbackBackend backend = new LoopbackBackend(backendId, server, timeoutRelease);
            server.createContext("/enterprise-lab/proof", backend::handle);
            server.start();
            return backend;
        }

        private synchronized void setMode(BackendMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode cannot be null");
            this.requestCount = 0;
        }

        private synchronized void resetCounter() {
            requestCount = 0;
        }

        private void handle(HttpExchange exchange) throws IOException {
            int status;
            BackendMode selected;
            int ordinal;
            synchronized (this) {
                selected = mode;
                ordinal = ++requestCount;
            }
            if (!"GET".equals(exchange.getRequestMethod())) {
                status = 405;
            } else {
                status = switch (selected) {
                    case SUCCESS -> 204;
                    case FAILURE -> 500;
                    case FIRST_FAILURE_THEN_SUCCESS -> ordinal == 1 ? 500 : 204;
                    case PERIODIC_FAILURE -> ordinal % 7 == 0 ? 500 : 204;
                    case TIMEOUT -> awaitTimeoutRelease();
                };
            }
            try {
                exchange.sendResponseHeaders(status, -1);
            } finally {
                exchange.close();
            }
        }

        private int awaitTimeoutRelease() {
            try {
                timeoutRelease.await(1, TimeUnit.SECONDS);
                return 204;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return 503;
            }
        }

        private URI uri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                    + "/enterprise-lab/proof");
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private synchronized void advance(Duration duration) {
            instant = instant.plus(Objects.requireNonNull(duration, "duration cannot be null"));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public synchronized Instant instant() {
            return instant;
        }
    }

    private static final class AdjustableMonotonicClock {
        private long value;
        private long measuredLatencyNanos;

        private AdjustableMonotonicClock(Duration measuredLatency) {
            setMeasuredLatency(measuredLatency);
        }

        private synchronized void setMeasuredLatency(Duration measuredLatency) {
            Objects.requireNonNull(measuredLatency, "measuredLatency cannot be null");
            if (measuredLatency.isZero() || measuredLatency.isNegative()
                    || measuredLatency.compareTo(Duration.ofSeconds(10)) > 0) {
                throw new IllegalArgumentException("measuredLatency must be positive and no greater than ten seconds");
            }
            measuredLatencyNanos = measuredLatency.toNanos();
        }

        private synchronized long next() {
            value = Math.addExact(value, measuredLatencyNanos);
            return value;
        }
    }
}
