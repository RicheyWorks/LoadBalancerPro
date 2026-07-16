package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluation.Disposition;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluation.Trigger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable summary and full operator records from one packaged real-loopback proof suite.
 */
public record EnterpriseLabExperimentProofReport(
        String schemaVersion,
        Instant generatedAt,
        String requestedSuite,
        int backendCount,
        int totalActualRequests,
        boolean allPassed,
        List<ScenarioEvidence> scenarios,
        List<String> scopeBoundaries,
        String contentFingerprint) {
    public static final String SCHEMA_VERSION = "enterprise-lab-experiment-proof/v1";
    public static final int MAX_SCENARIOS = 16;

    public EnterpriseLabExperimentProofReport {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported experiment proof schemaVersion");
        }
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt cannot be null");
        requestedSuite = requireText(requestedSuite, "requestedSuite");
        if (backendCount < 1 || backendCount > EnterpriseLabLoopbackAllocationSnapshot.HARD_MAX_BACKENDS) {
            throw new IllegalArgumentException("backendCount must be between 1 and 64");
        }
        if (totalActualRequests < 0) {
            throw new IllegalArgumentException("totalActualRequests cannot be negative");
        }
        scenarios = List.copyOf(Objects.requireNonNull(scenarios, "scenarios cannot be null"));
        if (scenarios.isEmpty() || scenarios.size() > MAX_SCENARIOS) {
            throw new IllegalArgumentException("proof report must contain between 1 and 16 scenarios");
        }
        Set<String> ids = new LinkedHashSet<>();
        int countedRequests = 0;
        boolean countedPassed = true;
        for (ScenarioEvidence scenario : scenarios) {
            if (!ids.add(scenario.proofId())) {
                throw new IllegalArgumentException("proof scenario IDs must be unique");
            }
            countedRequests = Math.addExact(countedRequests, scenario.actualRequestCount());
            countedPassed = countedPassed && scenario.passed();
        }
        if (countedRequests != totalActualRequests || countedPassed != allPassed) {
            throw new IllegalArgumentException("proof aggregate counters must match scenario evidence");
        }
        scopeBoundaries = List.copyOf(Objects.requireNonNull(scopeBoundaries, "scopeBoundaries cannot be null"));
        if (scopeBoundaries.isEmpty()) {
            throw new IllegalArgumentException("scopeBoundaries cannot be empty");
        }
        String expected = fingerprint(generatedAt, requestedSuite, backendCount, totalActualRequests,
                allPassed, scenarios, scopeBoundaries);
        if (!expected.equals(contentFingerprint)) {
            throw new IllegalArgumentException("proof report contentFingerprint does not match report content");
        }
    }

    public static EnterpriseLabExperimentProofReport create(
            Instant generatedAt,
            String requestedSuite,
            int backendCount,
            List<ScenarioEvidence> scenarios) {
        List<ScenarioEvidence> safeScenarios = List.copyOf(scenarios);
        int totalRequests = safeScenarios.stream().mapToInt(ScenarioEvidence::actualRequestCount).sum();
        boolean passed = safeScenarios.stream().allMatch(ScenarioEvidence::passed);
        List<String> boundaries = List.of(
                "literal 127.0.0.1 ephemeral backends created by this foreground proof only",
                "repository-approved Enterprise Lab scenario and PR1-PR5 operator stack only",
                "bounded synchronous requests, durations, timeouts, histories, and target-only evidence",
                "not production routing, durable audit storage, live cloud, tenant, load, stress, or readiness proof");
        return new EnterpriseLabExperimentProofReport(
                SCHEMA_VERSION,
                generatedAt,
                requestedSuite,
                backendCount,
                totalRequests,
                passed,
                safeScenarios,
                boundaries,
                fingerprint(generatedAt, requestedSuite, backendCount, totalRequests,
                        passed, safeScenarios, boundaries));
    }

    public record ScenarioEvidence(
            String proofId,
            String expectedOutcome,
            int actualRequestCount,
            Map<String, Integer> observedOutcomes,
            String guardrailAction,
            boolean guardrailClamped,
            List<Disposition> evaluationDispositions,
            List<Trigger> rollbackTriggers,
            boolean trafficActionObserved,
            boolean baselineRestored,
            boolean idempotencyVerified,
            boolean boundVerified,
            List<String> checks,
            boolean passed,
            EnterpriseLabExperimentOperatorRecord finalRecord,
            String contentFingerprint) {

        public ScenarioEvidence {
            proofId = requireCanonicalId(proofId, "proofId");
            expectedOutcome = requireText(expectedOutcome, "expectedOutcome");
            if (actualRequestCount < 0) {
                throw new IllegalArgumentException("actualRequestCount cannot be negative");
            }
            observedOutcomes = immutableCounts(observedOutcomes);
            int countedOutcomes = observedOutcomes.values().stream().mapToInt(Integer::intValue).sum();
            if (countedOutcomes != actualRequestCount) {
                throw new IllegalArgumentException("observed outcome counts must equal actualRequestCount");
            }
            guardrailAction = requireText(guardrailAction, "guardrailAction");
            evaluationDispositions = List.copyOf(Objects.requireNonNull(
                    evaluationDispositions, "evaluationDispositions cannot be null"));
            rollbackTriggers = List.copyOf(Objects.requireNonNull(rollbackTriggers, "rollbackTriggers cannot be null"));
            checks = List.copyOf(Objects.requireNonNull(checks, "checks cannot be null"));
            if (checks.isEmpty()) {
                throw new IllegalArgumentException("checks cannot be empty");
            }
            finalRecord = Objects.requireNonNull(finalRecord, "finalRecord cannot be null");
            if (!finalRecord.lifecycle().terminal() || finalRecord.currentAllocation().kind()
                    == EnterpriseLabLoopbackAllocationSnapshot.Kind.CANDIDATE) {
                throw new IllegalArgumentException("proof scenario must retain a terminal non-candidate record");
            }
            String expected = scenarioFingerprint(
                    proofId, expectedOutcome, actualRequestCount, observedOutcomes, guardrailAction,
                    guardrailClamped, evaluationDispositions, rollbackTriggers, trafficActionObserved,
                    baselineRestored, idempotencyVerified, boundVerified, checks, passed, finalRecord);
            if (!expected.equals(contentFingerprint)) {
                throw new IllegalArgumentException("scenario contentFingerprint does not match scenario evidence");
            }
        }

        public static ScenarioEvidence create(
                String proofId,
                String expectedOutcome,
                int actualRequestCount,
                Map<String, Integer> observedOutcomes,
                boolean idempotencyVerified,
                boolean boundVerified,
                List<String> checks,
                EnterpriseLabExperimentOperatorRecord finalRecord) {
            List<Disposition> dispositions = finalRecord.evaluations().stream()
                    .map(EnterpriseLabExperimentEvaluation::disposition)
                    .toList();
            List<Trigger> triggers = finalRecord.evaluations().stream()
                    .flatMap(evaluation -> evaluation.triggers().stream())
                    .distinct()
                    .toList();
            String guardrailAction = finalRecord.configuration().candidateDecision().decision()
                    .guardrailDecision().action().name();
            boolean clamped = "CLAMP".equals(guardrailAction);
            boolean trafficAction = finalRecord.operatorActions().stream()
                    .anyMatch(EnterpriseLabExperimentOperatorRecord.OperatorActionEvidence::trafficActionPerformed);
            boolean restored = EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                    finalRecord.currentAllocation().allocations(),
                    finalRecord.configuration().baselineSnapshot().allocations());
            boolean passed = finalRecord.lifecycle().terminal()
                    && finalRecord.currentAllocation().kind() != EnterpriseLabLoopbackAllocationSnapshot.Kind.CANDIDATE
                    && restored
                    && boundVerified;
            Map<String, Integer> safeCounts = immutableCounts(observedOutcomes);
            List<String> safeChecks = List.copyOf(checks);
            return new ScenarioEvidence(
                    proofId,
                    expectedOutcome,
                    actualRequestCount,
                    safeCounts,
                    guardrailAction,
                    clamped,
                    dispositions,
                    triggers,
                    trafficAction,
                    restored,
                    idempotencyVerified,
                    boundVerified,
                    safeChecks,
                    passed,
                    finalRecord,
                    scenarioFingerprint(
                            proofId, expectedOutcome, actualRequestCount, safeCounts, guardrailAction,
                            clamped, dispositions, triggers, trafficAction, restored, idempotencyVerified,
                            boundVerified, safeChecks, passed, finalRecord));
        }
    }

    private static String fingerprint(
            Instant generatedAt,
            String suite,
            int backendCount,
            int totalRequests,
            boolean passed,
            List<ScenarioEvidence> scenarios,
            List<String> boundaries) {
        MessageDigest digest = sha256();
        update(digest, SCHEMA_VERSION);
        update(digest, generatedAt.toString());
        update(digest, suite);
        update(digest, Integer.toString(backendCount));
        update(digest, Integer.toString(totalRequests));
        update(digest, Boolean.toString(passed));
        scenarios.forEach(scenario -> update(digest, scenario.contentFingerprint()));
        boundaries.forEach(boundary -> update(digest, boundary));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String scenarioFingerprint(
            String proofId,
            String expectedOutcome,
            int actualRequestCount,
            Map<String, Integer> observedOutcomes,
            String guardrailAction,
            boolean guardrailClamped,
            List<Disposition> dispositions,
            List<Trigger> triggers,
            boolean trafficAction,
            boolean restored,
            boolean idempotency,
            boolean bounded,
            List<String> checks,
            boolean passed,
            EnterpriseLabExperimentOperatorRecord record) {
        MessageDigest digest = sha256();
        update(digest, proofId);
        update(digest, expectedOutcome);
        update(digest, Integer.toString(actualRequestCount));
        observedOutcomes.forEach((outcome, count) -> {
            update(digest, outcome);
            update(digest, Integer.toString(count));
        });
        update(digest, guardrailAction);
        update(digest, Boolean.toString(guardrailClamped));
        dispositions.forEach(value -> update(digest, value.name()));
        triggers.forEach(value -> update(digest, value.name()));
        update(digest, Boolean.toString(trafficAction));
        update(digest, Boolean.toString(restored));
        update(digest, Boolean.toString(idempotency));
        update(digest, Boolean.toString(bounded));
        checks.forEach(check -> update(digest, check));
        update(digest, Boolean.toString(passed));
        update(digest, record.contentFingerprint());
        return HexFormat.of().formatHex(digest.digest());
    }

    private static Map<String, Integer> immutableCounts(Map<String, Integer> values) {
        Objects.requireNonNull(values, "observedOutcomes cannot be null");
        Map<String, Integer> safe = new LinkedHashMap<>();
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String key = requireText(entry.getKey(), "outcome");
            Integer count = Objects.requireNonNull(entry.getValue(), "outcome count cannot be null");
            if (count < 0) {
                throw new IllegalArgumentException("outcome counts cannot be negative");
            }
            safe.put(key, count);
        });
        return Collections.unmodifiableMap(new LinkedHashMap<>(safe));
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

    private static String requireCanonicalId(String value, String fieldName) {
        String safe = requireText(value, fieldName);
        if (safe.length() > 128 || !safe.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(fieldName + " must be a bounded canonical identifier");
        }
        return safe;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
