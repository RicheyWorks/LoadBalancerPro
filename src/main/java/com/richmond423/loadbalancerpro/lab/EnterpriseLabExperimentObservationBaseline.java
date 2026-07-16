package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.ServerRollingSignalState;
import com.richmond423.loadbalancerpro.core.ServerSignalEvidence;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Immutable pre-candidate latency/evidence baseline captured from the real loopback ingress.
 */
public record EnterpriseLabExperimentObservationBaseline(
        String schemaVersion,
        String experimentId,
        String scenarioId,
        String configurationFingerprint,
        Instant capturedAt,
        Map<String, BackendBaseline> backends) {
    public static final String SCHEMA_VERSION = "enterprise-lab-experiment-observation-baseline/v1";

    public EnterpriseLabExperimentObservationBaseline {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported observation baseline schemaVersion");
        }
        experimentId = requireNonBlank(experimentId, "experimentId");
        scenarioId = requireNonBlank(scenarioId, "scenarioId");
        configurationFingerprint = requireSha256(configurationFingerprint, "configurationFingerprint");
        capturedAt = Objects.requireNonNull(capturedAt, "capturedAt cannot be null");
        Instant baselineCapturedAt = capturedAt;
        Objects.requireNonNull(backends, "backends cannot be null");
        if (backends.isEmpty() || backends.size() > EnterpriseLabLoopbackAllocationSnapshot.HARD_MAX_BACKENDS) {
            throw new IllegalArgumentException("observation baseline backend count must be between 1 and 64");
        }
        Map<String, BackendBaseline> sorted = new TreeMap<>();
        backends.forEach((backendId, baseline) -> {
            String safeBackendId = requireNonBlank(backendId, "baseline backendId");
            BackendBaseline safeBaseline = Objects.requireNonNull(baseline, "backend baseline cannot be null");
            if (!safeBackendId.equals(safeBaseline.backendId())) {
                throw new IllegalArgumentException("baseline map key must match backendId");
            }
            if (safeBaseline.latestObservationAt().filter(latest -> latest.isAfter(baselineCapturedAt)).isPresent()) {
                throw new IllegalArgumentException("baseline observation cannot be after capturedAt");
            }
            if (sorted.putIfAbsent(safeBackendId, safeBaseline) != null) {
                throw new IllegalArgumentException("baseline backendIds must be unique");
            }
        });
        backends = Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    public static EnterpriseLabExperimentObservationBaseline capture(
            EnterpriseLabExperimentConfiguration configuration,
            EnterpriseLabLoopbackObservationIngress ingress,
            Instant capturedAt) {
        EnterpriseLabExperimentConfiguration safeConfiguration = Objects.requireNonNull(
                configuration, "configuration cannot be null");
        EnterpriseLabLoopbackObservationIngress safeIngress = Objects.requireNonNull(
                ingress, "ingress cannot be null");
        Instant safeTime = Objects.requireNonNull(capturedAt, "capturedAt cannot be null");
        TreeSet<String> expected = new TreeSet<>(safeConfiguration.baselineSnapshot().allocations().keySet());
        if (!expected.equals(new TreeSet<>(safeIngress.approvedBackendIds()))) {
            throw new IllegalArgumentException(
                    "observation ingress backends must exactly match the configured loopback allocation");
        }
        Map<String, BackendBaseline> captured = new TreeMap<>();
        for (String backendId : expected) {
            ServerRollingSignalState signal = safeIngress.snapshot(backendId, safeTime);
            captured.put(backendId, BackendBaseline.from(signal));
        }
        return new EnterpriseLabExperimentObservationBaseline(
                SCHEMA_VERSION,
                safeConfiguration.experimentId(),
                safeConfiguration.scenarioId(),
                safeConfiguration.contentFingerprint(),
                safeTime,
                captured);
    }

    public String contentFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, schemaVersion);
            update(digest, experimentId);
            update(digest, scenarioId);
            update(digest, configurationFingerprint);
            update(digest, capturedAt.toString());
            backends.forEach((backendId, baseline) -> {
                update(digest, backendId);
                update(digest, baseline.toString());
            });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record BackendBaseline(
            String backendId,
            int sampleCount,
            ServerSignalEvidence evidence,
            OptionalDouble p95LatencyMillis,
            OptionalDouble p99LatencyMillis,
            Optional<Instant> latestObservationAt) {

        public BackendBaseline {
            backendId = requireNonBlank(backendId, "backendId");
            if (sampleCount < 0) {
                throw new IllegalArgumentException("sampleCount cannot be negative");
            }
            evidence = Objects.requireNonNull(evidence, "evidence cannot be null");
            p95LatencyMillis = requireLatency(p95LatencyMillis, "p95LatencyMillis");
            p99LatencyMillis = requireLatency(p99LatencyMillis, "p99LatencyMillis");
            latestObservationAt = Objects.requireNonNull(latestObservationAt, "latestObservationAt cannot be null");
            if (p95LatencyMillis.isPresent() != p99LatencyMillis.isPresent()) {
                throw new IllegalArgumentException("baseline p95 and p99 latency must be present together");
            }
            if (p95LatencyMillis.isPresent()
                    && p99LatencyMillis.getAsDouble() < p95LatencyMillis.getAsDouble()) {
                throw new IllegalArgumentException("baseline p99 latency cannot be below p95 latency");
            }
        }

        private static BackendBaseline from(ServerRollingSignalState signal) {
            return new BackendBaseline(
                    signal.serverId(),
                    signal.sampleCount(),
                    signal.evidence(),
                    signal.latencyWindowSignal().rollingP95LatencyMillis(),
                    signal.latencyWindowSignal().rollingP99LatencyMillis(),
                    signal.latestObservationAt());
        }
    }

    private static OptionalDouble requireLatency(OptionalDouble value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.isPresent() && (!Double.isFinite(value.getAsDouble()) || value.getAsDouble() < 0.0)) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
        return value;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static String requireSha256(String value, String fieldName) {
        String fingerprint = requireNonBlank(value, fieldName);
        if (!fingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(fieldName + " must be lowercase SHA-256");
        }
        return fingerprint;
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
