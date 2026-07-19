package com.richmond423.loadbalancerpro.lab;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/** Sanitized fixed-file discovery metadata for one supervisor process epoch. */
public record EnterpriseLabSupervisorConnectionMetadata(
        String schemaVersion,
        String address,
        int port,
        String supervisorInstanceId,
        long supervisorGeneration,
        long durableStateGeneration,
        String stateFingerprint,
        Instant publishedAt) {
    public static final String SCHEMA_VERSION =
            "enterprise-lab-supervisor-readiness/v1";
    public static final String LITERAL_ADDRESS = "127.0.0.1";

    private static final Pattern CANONICAL_ID =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public EnterpriseLabSupervisorConnectionMetadata {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException(
                    "unsupported supervisor connection metadata schemaVersion");
        }
        if (!LITERAL_ADDRESS.equals(address)) {
            throw new IllegalArgumentException(
                    "supervisor connection metadata requires literal IPv4 loopback");
        }
        if (port < EnterpriseLabSupervisorConfiguration.MIN_CONFIGURED_PORT
                || port > EnterpriseLabSupervisorConfiguration.MAX_CONFIGURED_PORT) {
            throw new IllegalArgumentException(
                    "supervisor connection metadata port is outside hard bounds");
        }
        if (supervisorInstanceId == null
                || !CANONICAL_ID.matcher(supervisorInstanceId).matches()
                || EnterpriseLabSupervisorProtocol.NONE.equals(supervisorInstanceId)) {
            throw new IllegalArgumentException(
                    "supervisor connection metadata instance is invalid");
        }
        if (supervisorGeneration < EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                || supervisorGeneration > EnterpriseLabEvidenceOwnership.MAX_GENERATION) {
            throw new IllegalArgumentException(
                    "supervisor connection metadata generation is outside hard bounds");
        }
        if (durableStateGeneration < 1L
                || durableStateGeneration
                > EnterpriseLabSupervisorState.HARD_MAX_DURABLE_STATE_GENERATION) {
            throw new IllegalArgumentException(
                    "supervisor durable state generation is outside hard bounds");
        }
        if (stateFingerprint == null || !SHA_256.matcher(stateFingerprint).matches()) {
            throw new IllegalArgumentException(
                    "supervisor connection state fingerprint must be lowercase SHA-256");
        }
        publishedAt = Objects.requireNonNull(publishedAt, "publishedAt cannot be null");
    }

    public boolean sameProcessEpoch(EnterpriseLabSupervisorConnectionMetadata other) {
        return other != null
                && supervisorGeneration == other.supervisorGeneration
                && supervisorInstanceId.equals(other.supervisorInstanceId)
                && address.equals(other.address)
                && port == other.port;
    }
}
