package com.richmond423.loadbalancerpro.lab;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable installed state consulted by the router for new loopback routing
 * decisions and independently exposed for bounded read-back evidence.
 */
public record EnterpriseLabInstalledAllocationSnapshot(
        String schemaVersion,
        EnterpriseLabLoopbackAllocationSnapshot routingSnapshot,
        long routerGeneration,
        String allocationFingerprint,
        List<String> eligibleBackendIds,
        List<String> excludedBackendIds,
        Instant installedAt,
        String installationReason,
        long ownerGeneration) {
    public static final String SCHEMA_VERSION = "enterprise-lab-installed-loopback-allocation/v1";
    public static final long UNOWNED_GENERATION = 0L;
    private static final int MAX_REASON_LENGTH = 256;
    private static final Pattern UNSAFE_EVIDENCE = Pattern.compile(
            "(?i)(password|secret|token|api[_-]?key|credential)\\s*[:=]");

    public EnterpriseLabInstalledAllocationSnapshot {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported installed allocation schemaVersion");
        }
        routingSnapshot = Objects.requireNonNull(
                routingSnapshot, "routingSnapshot cannot be null");
        if (routerGeneration < 0
                || routerGeneration > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION
                || routingSnapshot.revision() != routerGeneration) {
            throw new IllegalArgumentException(
                    "routerGeneration must match the bounded routing snapshot revision");
        }
        String expectedFingerprint = EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                routingSnapshot.scenarioId(), routingSnapshot.allocations());
        allocationFingerprint = Objects.requireNonNull(
                allocationFingerprint, "allocationFingerprint cannot be null");
        if (!allocationFingerprint.equals(expectedFingerprint)) {
            throw new IllegalArgumentException(
                    "allocationFingerprint does not match the installed allocation");
        }
        eligibleBackendIds = List.copyOf(Objects.requireNonNull(
                eligibleBackendIds, "eligibleBackendIds cannot be null"));
        excludedBackendIds = List.copyOf(Objects.requireNonNull(
                excludedBackendIds, "excludedBackendIds cannot be null"));
        if (!eligibleBackendIds.equals(routingSnapshot.eligibleBackendIds())
                || !excludedBackendIds.equals(routingSnapshot.excludedBackendIds())) {
            throw new IllegalArgumentException(
                    "backend eligibility must be derived exactly from the installed allocation");
        }
        installedAt = Objects.requireNonNull(installedAt, "installedAt cannot be null");
        installationReason = requireReason(installationReason);
        if (ownerGeneration != UNOWNED_GENERATION
                && (ownerGeneration < EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                || ownerGeneration > EnterpriseLabEvidenceOwnership.MAX_GENERATION)) {
            throw new IllegalArgumentException("ownerGeneration is outside hard bounds");
        }
    }

    static EnterpriseLabInstalledAllocationSnapshot installed(
            EnterpriseLabLoopbackAllocationSnapshot routingSnapshot,
            Clock clock,
            String installationReason,
            long ownerGeneration) {
        EnterpriseLabLoopbackAllocationSnapshot safe = Objects.requireNonNull(
                routingSnapshot, "routingSnapshot cannot be null");
        return new EnterpriseLabInstalledAllocationSnapshot(
                SCHEMA_VERSION,
                safe,
                safe.revision(),
                EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                        safe.scenarioId(), safe.allocations()),
                safe.eligibleBackendIds(),
                safe.excludedBackendIds(),
                Objects.requireNonNull(clock, "clock cannot be null").instant(),
                installationReason,
                ownerGeneration);
    }

    public boolean safeDefault() {
        return routerGeneration == 0L
                && routingSnapshot.kind() == EnterpriseLabLoopbackAllocationSnapshot.Kind.BASELINE;
    }

    private static String requireReason(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("installationReason cannot be null or blank");
        }
        String safe = value.trim();
        if (!safe.equals(value)
                || safe.length() > MAX_REASON_LENGTH
                || safe.chars().anyMatch(character -> Character.isISOControl(character))
                || UNSAFE_EVIDENCE.matcher(safe).find()) {
            throw new IllegalArgumentException(
                    "installationReason must be bounded sanitized plain text");
        }
        return safe;
    }
}
