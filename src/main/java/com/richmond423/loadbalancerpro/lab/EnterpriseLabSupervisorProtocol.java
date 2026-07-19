package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransitionReason;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.VerificationResult;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Versioned business-message boundary shared by the Enterprise Lab application
 * and its bounded local allocation supervisor. Transport credentials are
 * deliberately absent from these evidence-safe values.
 */
public final class EnterpriseLabSupervisorProtocol {
    public static final String SCHEMA_VERSION =
            "enterprise-lab-allocation-supervisor-ipc/v1";
    public static final String NONE = "NONE";
    public static final int HARD_MAX_REQUEST_BYTES = 65_536;
    public static final int HARD_MAX_RESPONSE_BYTES = 65_536;
    public static final int HARD_MAX_METADATA_ENTRIES = 16;
    public static final int MAX_ID_LENGTH = 128;
    public static final int MAX_METADATA_KEY_LENGTH = 64;
    public static final int MAX_METADATA_VALUE_LENGTH = 256;

    private static final Pattern CANONICAL_ID =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern UNSAFE_EVIDENCE = Pattern.compile(
            "(?i)(?:\\bbearer\\s+[A-Za-z0-9._~+/-]{12,}={0,2}"
                    + "|-----BEGIN [^-\\r\\n]{1,40}PRIVATE KEY-----"
                    + "|\\b(?:api[_-]?key|password|access[_-]?token|refresh[_-]?token|client[_-]?secret)"
                    + "\\s*[:=]\\s*\\S+|\\bcookie\\s*:\\s*\\S+"
                    + "|(?:^|\\R)\\s*at\\s+[A-Za-z0-9_.$]+\\([^\\r\\n]*\\.java:\\d+\\))");
    private static final Pattern UNSAFE_LOCATION = Pattern.compile(
            "(?i)(?:[a-z][a-z0-9+.-]*://|file:|^[A-Za-z]:[\\\\/]"
                    + "|(?:^|\\s)/|\\\\"
                    + "|(?:^|/)\\.\\.(?:/|$)"
                    + "|(?:^|[\\s(])(?:localhost|(?:\\d{1,3}\\.){3}\\d{1,3}"
                    + "|\\[[0-9a-f:]+\\]|(?:[a-z0-9-]+\\.)+[a-z]{2,})"
                    + "(?::\\d{1,5})?(?:$|[\\s,)]))");
    private static final java.util.Set<String> SENSITIVE_KEYS = java.util.Set.of(
            "authorization", "password", "passwd", "apikey", "accesstoken",
            "refreshtoken", "cookie", "setcookie", "privatekey", "clientsecret",
            "credential", "credentials", "secret", "token", "stacktrace", "throwable");
    private static final java.util.Set<String> FORBIDDEN_CONTROL_KEYS = java.util.Set.of(
            "path", "filepath", "directory", "url", "uri", "host", "hostname", "address",
            "port", "command", "shell", "executable", "supervisorgeneration",
            "routergeneration", "transactionphase");

    private EnterpriseLabSupervisorProtocol() {
    }

    public enum CommandType {
        HEALTH(CommandClassification.OBSERVATION),
        READINESS(CommandClassification.OBSERVATION),
        READ_INSTALLED_ALLOCATION(CommandClassification.OBSERVATION),
        ESTABLISH_INITIAL_BASELINE(CommandClassification.ALLOCATION_MUTATION),
        APPLY_ALLOCATION(CommandClassification.ALLOCATION_MUTATION),
        RESTORE_BASELINE(CommandClassification.ALLOCATION_MUTATION),
        VERIFY_ALLOCATION(CommandClassification.OBSERVATION),
        ADVANCE_APPLICATION_OWNERSHIP(CommandClassification.OWNERSHIP_HANDOFF),
        READ_SUPERVISOR_GENERATION(CommandClassification.OBSERVATION),
        READ_STATUS(CommandClassification.OBSERVATION),
        CLEAN_SHUTDOWN(CommandClassification.LIFECYCLE);

        private final CommandClassification classification;

        CommandType(CommandClassification classification) {
            this.classification = classification;
        }

        public CommandClassification classification() {
            return classification;
        }

        public boolean mutation() {
            return classification == CommandClassification.ALLOCATION_MUTATION
                    || classification == CommandClassification.OWNERSHIP_HANDOFF
                    || classification == CommandClassification.LIFECYCLE;
        }
    }

    public enum CommandClassification {
        OBSERVATION,
        ALLOCATION_MUTATION,
        OWNERSHIP_HANDOFF,
        LIFECYCLE
    }

    public enum ResponseStatus {
        ACCEPTED,
        REJECTED,
        FAILED
    }

    /**
     * Fully fingerprinted request. Callers should use
     * {@link EnterpriseLabSupervisorProtocolCodec#issue(RequestDraft)} so the
     * schema and request fingerprint cannot be selected independently.
     */
    public record Request(
            String schemaVersion,
            String requestId,
            String requestFingerprint,
            CommandType commandType,
            String applicationInstanceId,
            String applicationOwnershipRecordFingerprint,
            long applicationOwnerGeneration,
            String expectedSupervisorInstanceId,
            long expectedSupervisorGeneration,
            String transactionId,
            Optional<String> experimentId,
            AllocationPurpose allocationPurpose,
            Optional<EnterpriseLabLoopbackAllocationSnapshot> allocation,
            String allocationFingerprint,
            String previousCommittedFingerprint,
            Instant requestedAt,
            Map<String, String> metadata) {

        public Request {
            if (!SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IllegalArgumentException("unsupported supervisor request schemaVersion");
            }
            requestId = requireId(requestId, "requestId");
            requestFingerprint = requireSha(requestFingerprint, "requestFingerprint");
            commandType = Objects.requireNonNull(commandType, "commandType cannot be null");
            applicationInstanceId = requireId(applicationInstanceId, "applicationInstanceId");
            applicationOwnershipRecordFingerprint = requireOptionalSha(
                    applicationOwnershipRecordFingerprint,
                    "applicationOwnershipRecordFingerprint");
            applicationOwnerGeneration = requireGeneration(
                    applicationOwnerGeneration, true, "applicationOwnerGeneration");
            expectedSupervisorInstanceId = requireOptionalId(
                    expectedSupervisorInstanceId, "expectedSupervisorInstanceId");
            expectedSupervisorGeneration = requireGeneration(
                    expectedSupervisorGeneration, true, "expectedSupervisorGeneration");
            requirePairedFence(
                    applicationOwnershipRecordFingerprint,
                    applicationOwnerGeneration,
                    "application ownership");
            requirePairedFence(
                    expectedSupervisorInstanceId,
                    expectedSupervisorGeneration,
                    "expected supervisor");
            transactionId = requireOptionalId(transactionId, "transactionId");
            experimentId = copyOptionalId(experimentId, "experimentId");
            allocationPurpose = Objects.requireNonNull(
                    allocationPurpose, "allocationPurpose cannot be null");
            allocation = Objects.requireNonNull(allocation, "allocation cannot be null");
            allocationFingerprint = requireOptionalSha(
                    allocationFingerprint, "allocationFingerprint");
            previousCommittedFingerprint = requirePriorFingerprint(
                    previousCommittedFingerprint);
            requestedAt = Objects.requireNonNull(requestedAt, "requestedAt cannot be null");
            metadata = safeMetadata(metadata);
            validateRequestCommand(
                    commandType,
                    applicationOwnershipRecordFingerprint,
                    applicationOwnerGeneration,
                    expectedSupervisorInstanceId,
                    expectedSupervisorGeneration,
                    transactionId,
                    experimentId,
                    allocationPurpose,
                    allocation,
                    allocationFingerprint,
                    previousCommittedFingerprint);
        }

        public boolean mutation() {
            return commandType.mutation();
        }
    }

    /** Draft excludes the protocol-controlled schema and request fingerprint. */
    public record RequestDraft(
            String requestId,
            CommandType commandType,
            String applicationInstanceId,
            String applicationOwnershipRecordFingerprint,
            long applicationOwnerGeneration,
            String expectedSupervisorInstanceId,
            long expectedSupervisorGeneration,
            String transactionId,
            Optional<String> experimentId,
            AllocationPurpose allocationPurpose,
            Optional<EnterpriseLabLoopbackAllocationSnapshot> allocation,
            String allocationFingerprint,
            String previousCommittedFingerprint,
            Instant requestedAt,
            Map<String, String> metadata) {

        public RequestDraft {
            experimentId = Objects.requireNonNull(experimentId, "experimentId cannot be null");
            allocation = Objects.requireNonNull(allocation, "allocation cannot be null");
            metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata cannot be null"));
        }
    }

    /**
     * Fully fingerprinted response. The response is correlated to the exact
     * canonical request rather than to caller-controlled transport state.
     */
    public record Response(
            String schemaVersion,
            String requestId,
            String requestFingerprint,
            CommandType commandType,
            String supervisorInstanceId,
            long supervisorGeneration,
            long observedApplicationGeneration,
            CommandClassification commandClassification,
            ResponseStatus status,
            boolean actionPerformed,
            Optional<EnterpriseLabInstalledAllocationSnapshot> installedAllocation,
            String installedFingerprint,
            long routerGeneration,
            long durableStateGeneration,
            VerificationResult verificationResult,
            String reasonCode,
            String reason,
            Instant respondedAt,
            String responseFingerprint) {

        public Response {
            if (!SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IllegalArgumentException("unsupported supervisor response schemaVersion");
            }
            requestId = requireId(requestId, "requestId");
            requestFingerprint = requireSha(requestFingerprint, "requestFingerprint");
            commandType = Objects.requireNonNull(commandType, "commandType cannot be null");
            supervisorInstanceId = requireId(supervisorInstanceId, "supervisorInstanceId");
            supervisorGeneration = requireGeneration(
                    supervisorGeneration, false, "supervisorGeneration");
            observedApplicationGeneration = requireGeneration(
                    observedApplicationGeneration, true, "observedApplicationGeneration");
            commandClassification = Objects.requireNonNull(
                    commandClassification, "commandClassification cannot be null");
            if (commandClassification != commandType.classification()) {
                throw new IllegalArgumentException(
                        "commandClassification must be derived from commandType");
            }
            status = Objects.requireNonNull(status, "status cannot be null");
            installedAllocation = Objects.requireNonNull(
                    installedAllocation, "installedAllocation cannot be null");
            installedFingerprint = requireOptionalSha(
                    installedFingerprint, "installedFingerprint");
            routerGeneration = requireNonNegativeBounded(
                    routerGeneration, "routerGeneration");
            durableStateGeneration = requireNonNegativeBounded(
                    durableStateGeneration, "durableStateGeneration");
            verificationResult = Objects.requireNonNull(
                    verificationResult, "verificationResult cannot be null");
            TransitionReason safeReason = new TransitionReason(reasonCode, reason);
            reasonCode = safeReason.code();
            reason = safeReason.message();
            rejectUnsafeLocation(reason, "reason");
            respondedAt = Objects.requireNonNull(respondedAt, "respondedAt cannot be null");
            responseFingerprint = requireSha(responseFingerprint, "responseFingerprint");
            validateResponse(
                    commandType,
                    status,
                    actionPerformed,
                    installedAllocation,
                    installedFingerprint,
                    routerGeneration,
                    verificationResult);
        }

        public boolean correlates(Request request) {
            Request safe = Objects.requireNonNull(request, "request cannot be null");
            return requestId.equals(safe.requestId())
                    && requestFingerprint.equals(safe.requestFingerprint())
                    && commandType == safe.commandType();
        }

        /**
         * Validates request correlation plus every fence and requested allocation
         * assertion that an accepted response is allowed to claim.
         */
        public boolean validatesAgainst(Request request) {
            Request safe = Objects.requireNonNull(request, "request cannot be null");
            if (!correlates(safe)) {
                return false;
            }
            if (status != ResponseStatus.ACCEPTED) {
                return true;
            }
            if (!NONE.equals(safe.expectedSupervisorInstanceId())
                    && (!supervisorInstanceId.equals(safe.expectedSupervisorInstanceId())
                    || supervisorGeneration != safe.expectedSupervisorGeneration())) {
                return false;
            }
            if ((safe.mutation() || safe.commandType() == CommandType.VERIFY_ALLOCATION)
                    && observedApplicationGeneration != safe.applicationOwnerGeneration()) {
                return false;
            }
            return !requiresExactRequestedAllocation(safe.commandType())
                    || installedFingerprint.equals(safe.allocationFingerprint());
        }
    }

    /** Draft excludes the protocol-controlled schema and response fingerprint. */
    public record ResponseDraft(
            String requestId,
            String requestFingerprint,
            CommandType commandType,
            String supervisorInstanceId,
            long supervisorGeneration,
            long observedApplicationGeneration,
            CommandClassification commandClassification,
            ResponseStatus status,
            boolean actionPerformed,
            Optional<EnterpriseLabInstalledAllocationSnapshot> installedAllocation,
            String installedFingerprint,
            long routerGeneration,
            long durableStateGeneration,
            VerificationResult verificationResult,
            String reasonCode,
            String reason,
            Instant respondedAt) {

        public ResponseDraft {
            installedAllocation = Objects.requireNonNull(
                    installedAllocation, "installedAllocation cannot be null");
        }
    }

    private static void validateRequestCommand(
            CommandType command,
            String ownershipFingerprint,
            long applicationGeneration,
            String supervisorInstanceId,
            long supervisorGeneration,
            String transactionId,
            Optional<String> experimentId,
            AllocationPurpose purpose,
            Optional<EnterpriseLabLoopbackAllocationSnapshot> allocation,
            String allocationFingerprint,
            String previousFingerprint) {
        if (command.mutation() || command == CommandType.VERIFY_ALLOCATION) {
            if (applicationGeneration < EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                    || supervisorGeneration < EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                    || NONE.equals(supervisorInstanceId)
                    || NONE.equals(transactionId)
                    || NONE.equals(ownershipFingerprint)) {
                throw new IllegalArgumentException(
                        "fenced supervisor commands require application ownership, supervisor identity, and transaction evidence");
            }
        }

        boolean carriesAllocation = command == CommandType.ESTABLISH_INITIAL_BASELINE
                || command == CommandType.APPLY_ALLOCATION;
        if (carriesAllocation != allocation.isPresent()) {
            throw new IllegalArgumentException(
                    "allocation payload presence does not match commandType");
        }
        if (allocation.isPresent()) {
            EnterpriseLabLoopbackAllocationSnapshot snapshot = allocation.orElseThrow();
            String expected = EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                    snapshot.scenarioId(), snapshot.allocations());
            if (!expected.equals(allocationFingerprint)) {
                throw new IllegalArgumentException(
                        "allocationFingerprint does not match the canonical allocation payload");
            }
        } else if (command != CommandType.VERIFY_ALLOCATION
                && command != CommandType.RESTORE_BASELINE
                && !NONE.equals(allocationFingerprint)) {
            throw new IllegalArgumentException(
                    "command without allocation verification must use NONE allocationFingerprint");
        }

        switch (command) {
            case HEALTH, READINESS, READ_INSTALLED_ALLOCATION,
                    READ_SUPERVISOR_GENERATION, READ_STATUS -> {
                requireNoExperiment(experimentId, command);
                requirePurpose(purpose, AllocationPurpose.RECONCILIATION_NO_OP, command);
                if (!NONE.equals(transactionId)
                        || !NONE.equals(previousFingerprint)
                        || !NONE.equals(allocationFingerprint)) {
                    throw new IllegalArgumentException(
                            "unfenced observation commands must not carry transaction or allocation mutation evidence");
                }
            }
            case VERIFY_ALLOCATION -> {
                requireNoExperiment(experimentId, command);
                requirePurpose(purpose, AllocationPurpose.RECONCILIATION_NO_OP, command);
                requireSha(allocationFingerprint, "allocationFingerprint");
                requireNoPriorFingerprint(previousFingerprint, command);
            }
            case ADVANCE_APPLICATION_OWNERSHIP, CLEAN_SHUTDOWN -> {
                requireNoExperiment(experimentId, command);
                requirePurpose(purpose, AllocationPurpose.RECONCILIATION_NO_OP, command);
                if (!NONE.equals(allocationFingerprint)
                        || !NONE.equals(previousFingerprint)) {
                    throw new IllegalArgumentException(
                            command + " cannot carry allocation mutation evidence");
                }
            }
            case ESTABLISH_INITIAL_BASELINE -> {
                requireNoExperiment(experimentId, command);
                requirePurpose(purpose, AllocationPurpose.INITIAL_SAFE_BASELINE, command);
                if (allocation.orElseThrow().kind()
                        != EnterpriseLabLoopbackAllocationSnapshot.Kind.BASELINE
                        || !NONE.equals(previousFingerprint)) {
                    throw new IllegalArgumentException(
                            "initial baseline command requires a baseline allocation and no prior committed fingerprint");
                }
            }
            case APPLY_ALLOCATION -> {
                if (experimentId.isEmpty()) {
                    throw new IllegalArgumentException(
                            "apply allocation requires an experimentId");
                }
                if (purpose != AllocationPurpose.EXPERIMENT_CANDIDATE
                        && purpose != AllocationPurpose.EXPERIMENT_HOLD) {
                    throw new IllegalArgumentException(
                            "apply allocation requires an experiment allocation purpose");
                }
                if (allocation.orElseThrow().kind()
                        != EnterpriseLabLoopbackAllocationSnapshot.Kind.CANDIDATE) {
                    throw new IllegalArgumentException(
                            "apply allocation requires a candidate allocation payload");
                }
                requireSha(previousFingerprint, "previousCommittedFingerprint");
            }
            case RESTORE_BASELINE -> {
                if (purpose != AllocationPurpose.ROLLBACK_RESTORATION
                        && purpose != AllocationPurpose.STARTUP_RESTORATION
                        && purpose != AllocationPurpose.TAKEOVER_RESTORATION
                        && purpose != AllocationPurpose.CANCELLATION_RESTORATION
                        && purpose != AllocationPurpose.OPERATOR_REQUESTED_SAFE_RESET) {
                    throw new IllegalArgumentException(
                            "restore baseline requires an explicit restoration purpose");
                }
                requireSha(allocationFingerprint, "allocationFingerprint");
                requireSha(previousFingerprint, "previousCommittedFingerprint");
            }
        }
    }

    private static void validateResponse(
            CommandType command,
            ResponseStatus status,
            boolean actionPerformed,
            Optional<EnterpriseLabInstalledAllocationSnapshot> installed,
            String installedFingerprint,
            long routerGeneration,
            VerificationResult verificationResult) {
        if (status != ResponseStatus.ACCEPTED && actionPerformed) {
            throw new IllegalArgumentException(
                    "rejected or failed responses cannot claim an action");
        }
        if (command.classification() == CommandClassification.OBSERVATION
                && actionPerformed) {
            throw new IllegalArgumentException(
                    "observation responses cannot claim an action");
        }
        if (installed.isPresent()) {
            EnterpriseLabInstalledAllocationSnapshot snapshot = installed.orElseThrow();
            if (!snapshot.allocationFingerprint().equals(installedFingerprint)
                    || snapshot.routerGeneration() != routerGeneration) {
                throw new IllegalArgumentException(
                        "installed response summary must match the installed snapshot exactly");
            }
        } else if (!NONE.equals(installedFingerprint) || routerGeneration != 0L) {
            throw new IllegalArgumentException(
                    "response without installed state must use NONE fingerprint and zero router generation");
        }
        if (status == ResponseStatus.ACCEPTED
                && command == CommandType.READ_INSTALLED_ALLOCATION
                && installed.isEmpty()) {
            throw new IllegalArgumentException(
                    "accepted installed-allocation read requires installed state");
        }
        boolean requiresReadBack = status == ResponseStatus.ACCEPTED
                && (command == CommandType.ESTABLISH_INITIAL_BASELINE
                || command == CommandType.APPLY_ALLOCATION
                || command == CommandType.RESTORE_BASELINE
                || command == CommandType.VERIFY_ALLOCATION);
        if (requiresReadBack && (installed.isEmpty()
                || verificationResult != VerificationResult.MATCHED)) {
            throw new IllegalArgumentException(
                    "accepted allocation response requires exact installed-state verification");
        }
    }

    private static void requireNoExperiment(Optional<String> experimentId, CommandType command) {
        if (experimentId.isPresent()) {
            throw new IllegalArgumentException(command + " cannot carry an experimentId");
        }
    }

    private static void requireNoPriorFingerprint(String value, CommandType command) {
        if (!EnterpriseLabAllocationState.NO_FINGERPRINT.equals(value)) {
            throw new IllegalArgumentException(
                    command + " cannot carry a previous committed fingerprint");
        }
    }

    private static boolean requiresExactRequestedAllocation(CommandType command) {
        return command == CommandType.ESTABLISH_INITIAL_BASELINE
                || command == CommandType.APPLY_ALLOCATION
                || command == CommandType.RESTORE_BASELINE
                || command == CommandType.VERIFY_ALLOCATION;
    }

    private static void requirePurpose(
            AllocationPurpose actual,
            AllocationPurpose expected,
            CommandType command) {
        if (actual != expected) {
            throw new IllegalArgumentException(command + " requires allocation purpose " + expected);
        }
    }

    private static long requireGeneration(long value, boolean allowZero, String field) {
        long minimum = allowZero ? 0L : EnterpriseLabEvidenceOwnership.INITIAL_GENERATION;
        if (value < minimum || value > EnterpriseLabEvidenceOwnership.MAX_GENERATION) {
            throw new IllegalArgumentException(field + " is outside hard bounds");
        }
        return value;
    }

    private static void requirePairedFence(String identity, long generation, String subject) {
        if (NONE.equals(identity) != (generation == 0L)) {
            throw new IllegalArgumentException(
                    subject + " identity and generation must be present or absent together");
        }
    }

    private static long requireNonNegativeBounded(long value, String field) {
        if (value < 0L || value > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION) {
            throw new IllegalArgumentException(field + " is outside hard bounds");
        }
        return value;
    }

    private static String requireId(String value, String field) {
        if (value == null || NONE.equals(value) || !CANONICAL_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be a bounded canonical identifier");
        }
        return value;
    }

    private static String requireOptionalId(String value, String field) {
        if (NONE.equals(value)) {
            return value;
        }
        return requireId(value, field);
    }

    private static Optional<String> copyOptionalId(Optional<String> value, String field) {
        Optional<String> safe = Objects.requireNonNull(value, field + " cannot be null");
        return safe.map(item -> requireId(item, field));
    }

    private static String requireSha(String value, String field) {
        if (value == null || !SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be canonical SHA-256 text");
        }
        return value;
    }

    private static String requireOptionalSha(String value, String field) {
        if (NONE.equals(value)) {
            return value;
        }
        return requireSha(value, field);
    }

    private static String requirePriorFingerprint(String value) {
        if (EnterpriseLabAllocationState.NO_FINGERPRINT.equals(value)) {
            return value;
        }
        return requireSha(value, "previousCommittedFingerprint");
    }

    private static Map<String, String> safeMetadata(Map<String, String> values) {
        Objects.requireNonNull(values, "metadata cannot be null");
        if (values.size() > HARD_MAX_METADATA_ENTRIES) {
            throw new IllegalArgumentException("metadata exceeds the hard entry bound");
        }
        TreeMap<String, String> safe = new TreeMap<>();
        values.forEach((key, value) -> {
            String safeKey = requireText(key, "metadata key", MAX_METADATA_KEY_LENGTH);
            String normalizedKey = normalizedKey(safeKey);
            if (!CANONICAL_ID.matcher(safeKey).matches()
                    || SENSITIVE_KEYS.contains(normalizedKey)
                    || FORBIDDEN_CONTROL_KEYS.contains(normalizedKey)) {
                throw new IllegalArgumentException(
                        "metadata key is not an evidence-safe canonical identifier");
            }
            String safeValue = requireText(value, "metadata value", MAX_METADATA_VALUE_LENGTH);
            if (UNSAFE_EVIDENCE.matcher(safeValue).find()
                    || UNSAFE_LOCATION.matcher(safeValue).find()) {
                throw new IllegalArgumentException(
                        "metadata value contains credential, path, address, or stack-trace shaped content");
            }
            if (safe.putIfAbsent(safeKey, safeValue) != null) {
                throw new IllegalArgumentException("metadata keys must be unique");
            }
        });
        return Map.copyOf(safe);
    }

    private static void rejectUnsafeLocation(String value, String field) {
        if (UNSAFE_LOCATION.matcher(value).find()) {
            throw new IllegalArgumentException(
                    field + " cannot contain path, host, address, port, or URI shaped content");
        }
    }

    private static String requireText(String value, String field, int maximum) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > maximum
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(field + " must be bounded sanitized plain text");
        }
        return value;
    }

    private static String normalizedKey(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
