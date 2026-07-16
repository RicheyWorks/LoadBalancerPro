package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.LifecycleSnapshot;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable API-facing lifecycle record for one bounded loopback experiment.
 */
public record EnterpriseLabExperimentOperatorRecord(
        String schemaVersion,
        String experimentId,
        String scenarioId,
        EnterpriseLabExperimentConfiguration configuration,
        LifecycleSnapshot lifecycle,
        Optional<EnterpriseLabExperimentObservationBaseline> observationBaseline,
        EnterpriseLabLoopbackAllocationSnapshot currentAllocation,
        List<EnterpriseLabExperimentEvaluation> evaluations,
        List<OperatorActionEvidence> operatorActions,
        Optional<Instant> completedAt,
        String storageMode,
        String evidenceBoundary,
        String contentFingerprint) {
    public static final String SCHEMA_VERSION = "enterprise-lab-experiment-operator-record/v1";
    public static final int MAX_OPERATOR_ACTIONS = 128;

    public EnterpriseLabExperimentOperatorRecord {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported operator record schemaVersion");
        }
        experimentId = requireText(experimentId, "experimentId");
        scenarioId = requireText(scenarioId, "scenarioId");
        configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
        lifecycle = Objects.requireNonNull(lifecycle, "lifecycle cannot be null");
        observationBaseline = Objects.requireNonNull(
                observationBaseline, "observationBaseline cannot be null");
        currentAllocation = Objects.requireNonNull(currentAllocation, "currentAllocation cannot be null");
        evaluations = List.copyOf(Objects.requireNonNull(evaluations, "evaluations cannot be null"));
        operatorActions = List.copyOf(Objects.requireNonNull(operatorActions, "operatorActions cannot be null"));
        completedAt = Objects.requireNonNull(completedAt, "completedAt cannot be null");
        storageMode = requireText(storageMode, "storageMode");
        evidenceBoundary = requireText(evidenceBoundary, "evidenceBoundary");
        if (!experimentId.equals(configuration.experimentId())
                || !experimentId.equals(lifecycle.experimentId())
                || !scenarioId.equals(configuration.scenarioId())
                || !scenarioId.equals(currentAllocation.scenarioId())) {
            throw new IllegalArgumentException("operator record components must describe one experiment and scenario");
        }
        if (evaluations.size() > EnterpriseLabExperimentEvaluator.MAX_RETAINED_EVALUATIONS) {
            throw new IllegalArgumentException("operator record evaluation history exceeds bounded capacity");
        }
        if (operatorActions.size() > MAX_OPERATOR_ACTIONS) {
            throw new IllegalArgumentException("operator record action history exceeds bounded capacity");
        }
        validateActionChain(experimentId, configuration.createdAt(), operatorActions);
        if (lifecycle.terminal() != completedAt.isPresent()) {
            throw new IllegalArgumentException("completedAt presence must match terminal lifecycle state");
        }
        String expectedFingerprint = fingerprint(
                experimentId,
                scenarioId,
                configuration,
                lifecycle,
                observationBaseline,
                currentAllocation,
                evaluations,
                operatorActions,
                completedAt,
                storageMode,
                evidenceBoundary);
        if (!expectedFingerprint.equals(contentFingerprint)) {
            throw new IllegalArgumentException("operator record contentFingerprint does not match record content");
        }
    }

    public static EnterpriseLabExperimentOperatorRecord create(
            EnterpriseLabExperimentConfiguration configuration,
            LifecycleSnapshot lifecycle,
            Optional<EnterpriseLabExperimentObservationBaseline> observationBaseline,
            EnterpriseLabLoopbackAllocationSnapshot currentAllocation,
            List<EnterpriseLabExperimentEvaluation> evaluations,
            List<OperatorActionEvidence> operatorActions) {
        Optional<Instant> completedAt = lifecycle.terminal()
                ? lifecycle.transitions().stream()
                        .reduce((first, second) -> second)
                        .map(EnterpriseLabExperimentTransition::occurredAt)
                : Optional.empty();
        String storageMode = "process-local bounded in-memory record";
        String evidenceBoundary = "authenticated Enterprise Lab literal-loopback workflow only; not production routing";
        String fingerprint = fingerprint(
                configuration.experimentId(),
                configuration.scenarioId(),
                configuration,
                lifecycle,
                observationBaseline,
                currentAllocation,
                evaluations,
                operatorActions,
                completedAt,
                storageMode,
                evidenceBoundary);
        return new EnterpriseLabExperimentOperatorRecord(
                SCHEMA_VERSION,
                configuration.experimentId(),
                configuration.scenarioId(),
                configuration,
                lifecycle,
                observationBaseline,
                currentAllocation,
                evaluations,
                operatorActions,
                completedAt,
                storageMode,
                evidenceBoundary,
                fingerprint);
    }

    public record OperatorActionEvidence(
            String schemaVersion,
            long sequence,
            String experimentId,
            String operatorRequestId,
            String operation,
            Instant occurredAt,
            EnterpriseLabExperimentState stateBefore,
            EnterpriseLabExperimentState stateAfter,
            int requestCount,
            int observationCount,
            boolean trafficActionPerformed,
            String reason,
            String previousFingerprint,
            String contentFingerprint) {
        public static final String SCHEMA_VERSION = "enterprise-lab-experiment-operator-action/v1";
        public static final String GENESIS_FINGERPRINT = "GENESIS";

        public OperatorActionEvidence {
            if (!SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IllegalArgumentException("unsupported operator action schemaVersion");
            }
            if (sequence < 1) {
                throw new IllegalArgumentException("operator action sequence must be positive");
            }
            experimentId = requireText(experimentId, "experimentId");
            operatorRequestId = requireText(operatorRequestId, "operatorRequestId");
            operation = requireText(operation, "operation");
            occurredAt = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
            stateBefore = Objects.requireNonNull(stateBefore, "stateBefore cannot be null");
            stateAfter = Objects.requireNonNull(stateAfter, "stateAfter cannot be null");
            if (requestCount < 0 || observationCount < 0 || observationCount > requestCount) {
                throw new IllegalArgumentException("operator action request/observation counters are inconsistent");
            }
            reason = boundedReason(reason);
            previousFingerprint = requireFingerprintOrGenesis(previousFingerprint);
            String expected = actionFingerprint(
                    sequence,
                    experimentId,
                    operatorRequestId,
                    operation,
                    occurredAt,
                    stateBefore,
                    stateAfter,
                    requestCount,
                    observationCount,
                    trafficActionPerformed,
                    reason,
                    previousFingerprint);
            if (!expected.equals(contentFingerprint)) {
                throw new IllegalArgumentException("operator action fingerprint does not match action content");
            }
        }

        public static OperatorActionEvidence create(
                long sequence,
                String experimentId,
                String operatorRequestId,
                String operation,
                Instant occurredAt,
                EnterpriseLabExperimentState stateBefore,
                EnterpriseLabExperimentState stateAfter,
                int requestCount,
                int observationCount,
                boolean trafficActionPerformed,
                String reason,
                String previousFingerprint) {
            String safeReason = boundedReason(reason);
            String fingerprint = actionFingerprint(
                    sequence,
                    experimentId,
                    operatorRequestId,
                    operation,
                    occurredAt,
                    stateBefore,
                    stateAfter,
                    requestCount,
                    observationCount,
                    trafficActionPerformed,
                    safeReason,
                    previousFingerprint);
            return new OperatorActionEvidence(
                    SCHEMA_VERSION,
                    sequence,
                    experimentId,
                    operatorRequestId,
                    operation,
                    occurredAt,
                    stateBefore,
                    stateAfter,
                    requestCount,
                    observationCount,
                    trafficActionPerformed,
                    safeReason,
                    previousFingerprint,
                    fingerprint);
        }
    }

    private static String fingerprint(
            String experimentId,
            String scenarioId,
            EnterpriseLabExperimentConfiguration configuration,
            LifecycleSnapshot lifecycle,
            Optional<EnterpriseLabExperimentObservationBaseline> observationBaseline,
            EnterpriseLabLoopbackAllocationSnapshot currentAllocation,
            List<EnterpriseLabExperimentEvaluation> evaluations,
            List<OperatorActionEvidence> operatorActions,
            Optional<Instant> completedAt,
            String storageMode,
            String evidenceBoundary) {
        MessageDigest digest = sha256();
        update(digest, SCHEMA_VERSION);
        update(digest, experimentId);
        update(digest, scenarioId);
        update(digest, configuration.contentFingerprint());
        update(digest, lifecycle.lastTransitionFingerprint());
        update(digest, observationBaseline.map(EnterpriseLabExperimentObservationBaseline::contentFingerprint)
                .orElse("NOT_CAPTURED"));
        update(digest, currentAllocation.toString());
        evaluations.forEach(evaluation -> update(digest, evaluation.contentFingerprint()));
        operatorActions.forEach(action -> update(digest, action.contentFingerprint()));
        update(digest, completedAt.map(Instant::toString).orElse("ACTIVE"));
        update(digest, storageMode);
        update(digest, evidenceBoundary);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String actionFingerprint(
            long sequence,
            String experimentId,
            String operatorRequestId,
            String operation,
            Instant occurredAt,
            EnterpriseLabExperimentState stateBefore,
            EnterpriseLabExperimentState stateAfter,
            int requestCount,
            int observationCount,
            boolean trafficActionPerformed,
            String reason,
            String previousFingerprint) {
        MessageDigest digest = sha256();
        update(digest, OperatorActionEvidence.SCHEMA_VERSION);
        update(digest, Long.toString(sequence));
        update(digest, experimentId);
        update(digest, operatorRequestId);
        update(digest, operation);
        update(digest, occurredAt.toString());
        update(digest, stateBefore.name());
        update(digest, stateAfter.name());
        update(digest, Integer.toString(requestCount));
        update(digest, Integer.toString(observationCount));
        update(digest, Boolean.toString(trafficActionPerformed));
        update(digest, reason);
        update(digest, previousFingerprint);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static String boundedReason(String value) {
        String reason = requireText(value, "reason").replace('\r', ' ').replace('\n', ' ');
        return reason.length() <= 256 ? reason : reason.substring(0, 256);
    }

    private static String requireFingerprintOrGenesis(String value) {
        String fingerprint = requireText(value, "previousFingerprint");
        if (!OperatorActionEvidence.GENESIS_FINGERPRINT.equals(fingerprint)
                && !fingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("previousFingerprint must be GENESIS or lowercase SHA-256");
        }
        return fingerprint;
    }

    private static void validateActionChain(
            String experimentId,
            Instant createdAt,
            List<OperatorActionEvidence> actions) {
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("operator record must retain its arm action");
        }
        String expectedPrevious = OperatorActionEvidence.GENESIS_FINGERPRINT;
        Instant previousTime = createdAt;
        for (int index = 0; index < actions.size(); index++) {
            OperatorActionEvidence action = actions.get(index);
            if (action.sequence() != index + 1L
                    || !experimentId.equals(action.experimentId())
                    || !expectedPrevious.equals(action.previousFingerprint())) {
                throw new IllegalArgumentException(
                        "operator actions must form one ordered fingerprint chain for the experiment");
            }
            if (action.occurredAt().isBefore(previousTime)) {
                throw new IllegalArgumentException("operator action times cannot move backwards");
            }
            expectedPrevious = action.contentFingerprint();
            previousTime = action.occurredAt();
        }
    }
}
