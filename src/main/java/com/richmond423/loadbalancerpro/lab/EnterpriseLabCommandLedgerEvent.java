package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Immutable evidence for one bounded stage of an application-to-supervisor command.
 * The correlation ID is the existing protocol request ID; fingerprints detect
 * content changes but do not authenticate an author or provide non-repudiation.
 */
public record EnterpriseLabCommandLedgerEvent(
        String schemaVersion,
        LedgerSide ledgerSide,
        long sequence,
        EventType eventType,
        String correlationId,
        String requestFingerprint,
        String transactionId,
        Optional<String> experimentId,
        CommandType commandType,
        String applicationInstanceId,
        long applicationOwnerGeneration,
        String supervisorInstanceId,
        long supervisorGeneration,
        long allocationGeneration,
        String requestedAllocationFingerprint,
        String previousCommittedFingerprint,
        String installedFingerprintBefore,
        String installedFingerprintAfter,
        long routerGenerationBefore,
        long routerGenerationAfter,
        AuthenticationResult authenticationResult,
        ValidationResult validationResult,
        DuplicateClassification duplicateClassification,
        MutationStatus mutationStatus,
        ResponseClassification responseClassification,
        String responseFingerprint,
        String observedSupervisorEventFingerprint,
        ApplicationCommitStatus applicationCommitStatus,
        int retryAttempt,
        String reasonCode,
        Instant occurredAt,
        Map<String, String> metadata,
        String predecessorFingerprint,
        String currentFingerprint) {

    public static final String SCHEMA_VERSION =
            "enterprise-lab-supervisor-command-ledger-event/v1";
    public static final String GENESIS_FINGERPRINT = "GENESIS";
    public static final String NONE = EnterpriseLabSupervisorProtocol.NONE;
    public static final int HARD_MAX_EVENT_BYTES = 32 * 1024;
    public static final int HARD_MAX_METADATA_ENTRIES = 16;
    public static final int HARD_MAX_RETRY_ATTEMPTS = 8;
    public static final long HARD_MAX_SEQUENCE = 1_000_000L;

    private static final int MAX_METADATA_KEY_LENGTH = 64;
    private static final int MAX_METADATA_VALUE_LENGTH = 256;
    private static final Pattern CANONICAL_ID =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private static final Pattern REASON_CODE =
            Pattern.compile("[A-Z0-9][A-Z0-9_.:-]{0,63}");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern UNSAFE_EVIDENCE = Pattern.compile(
            "(?i)(?:\\bbearer\\s+[A-Za-z0-9._~+/-]{12,}={0,2}"
                    + "|-----BEGIN [^-\\r\\n]{1,40}PRIVATE KEY-----"
                    + "|\\b(?:api[_-]?key|password|access[_-]?token|refresh[_-]?token|client[_-]?secret)"
                    + "\\s*[:=]\\s*\\S+|\\bcookie\\s*:\\s*\\S+"
                    + "|(?:^|\\R)\\s*at\\s+[A-Za-z0-9_.$]+\\([^\\r\\n]*\\.java:\\d+\\))");
    private static final Pattern UNSAFE_LOCATION = Pattern.compile(
            "(?i)(?:[a-z][a-z0-9+.-]*://|file:|^[A-Za-z]:[\\\\/]"
                    + "|(?:^|\\s)/|\\\\|(?:^|/)\\.\\.(?:/|$)"
                    + "|(?:^|[\\s(])(?:localhost|(?:\\d{1,3}\\.){3}\\d{1,3}"
                    + "|\\[[0-9a-f:]+\\]|(?:[a-z0-9-]+\\.)+[a-z]{2,})"
                    + "(?::\\d{1,5})?(?:$|[\\s,)]))");
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "authorization", "password", "passwd", "apikey", "accesstoken",
            "refreshtoken", "cookie", "setcookie", "privatekey", "clientsecret",
            "credential", "credentials", "secret", "token", "stacktrace", "throwable");
    private static final Set<String> FORBIDDEN_CONTROL_KEYS = Set.of(
            "path", "filepath", "directory", "url", "uri", "host", "hostname",
            "address", "port", "command", "shell", "executable", "backend",
            "backendaddress", "rawallocation", "rawrequest", "rawresponse");
    private static final Set<EventType> APPLICATION_EVENTS = Set.of(
            EventType.APPLICATION_INTENT_PERSISTED,
            EventType.DISPATCH_ATTEMPTED,
            EventType.APPLICATION_RESPONSE_RECEIVED,
            EventType.APPLICATION_COMMITTED,
            EventType.RESPONSE_LOST,
            EventType.TIMEOUT_OBSERVED,
            EventType.RETRY_ISSUED);
    private static final Set<EventType> SUPERVISOR_EVENTS = Set.of(
            EventType.SUPERVISOR_RECEIPT_PERSISTED,
            EventType.AUTHENTICATION_REJECTED,
            EventType.VALIDATION_REJECTED,
            EventType.DUPLICATE_ACCEPTED,
            EventType.DUPLICATE_REJECTED,
            EventType.MUTATION_STARTED,
            EventType.ALLOCATION_APPLIED,
            EventType.READ_BACK_VERIFIED,
            EventType.SUPERVISOR_COMMITTED,
            EventType.RESPONSE_SENT);

    public EnterpriseLabCommandLedgerEvent {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported command ledger schemaVersion");
        }
        ledgerSide = Objects.requireNonNull(ledgerSide, "ledgerSide cannot be null");
        if (sequence < 1L || sequence > HARD_MAX_SEQUENCE) {
            throw new IllegalArgumentException("sequence is outside hard bounds");
        }
        eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        requireEventSide(ledgerSide, eventType);
        correlationId = requireId(correlationId, "correlationId", false);
        requestFingerprint = requireFingerprint(
                requestFingerprint, "requestFingerprint", false);
        transactionId = requireId(transactionId, "transactionId", true);
        experimentId = copyOptionalId(experimentId, "experimentId");
        commandType = Objects.requireNonNull(commandType, "commandType cannot be null");
        applicationInstanceId = requireId(
                applicationInstanceId, "applicationInstanceId", false);
        applicationOwnerGeneration = requireGeneration(
                applicationOwnerGeneration, true, "applicationOwnerGeneration");
        supervisorInstanceId = requireId(
                supervisorInstanceId, "supervisorInstanceId", true);
        supervisorGeneration = requireGeneration(
                supervisorGeneration, true, "supervisorGeneration");
        requirePairedFence(supervisorInstanceId, supervisorGeneration);
        if (allocationGeneration < 0L
                || allocationGeneration
                > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION) {
            throw new IllegalArgumentException("allocationGeneration is outside hard bounds");
        }
        requestedAllocationFingerprint = requireFingerprint(
                requestedAllocationFingerprint, "requestedAllocationFingerprint", true);
        previousCommittedFingerprint = requireFingerprint(
                previousCommittedFingerprint, "previousCommittedFingerprint", true);
        installedFingerprintBefore = requireFingerprint(
                installedFingerprintBefore, "installedFingerprintBefore", true);
        installedFingerprintAfter = requireFingerprint(
                installedFingerprintAfter, "installedFingerprintAfter", true);
        routerGenerationBefore = requireRouterGeneration(
                routerGenerationBefore, "routerGenerationBefore");
        routerGenerationAfter = requireRouterGeneration(
                routerGenerationAfter, "routerGenerationAfter");
        if (routerGenerationAfter < routerGenerationBefore) {
            throw new IllegalArgumentException("router generation cannot regress in an event");
        }
        authenticationResult = Objects.requireNonNull(
                authenticationResult, "authenticationResult cannot be null");
        validationResult = Objects.requireNonNull(
                validationResult, "validationResult cannot be null");
        duplicateClassification = Objects.requireNonNull(
                duplicateClassification, "duplicateClassification cannot be null");
        mutationStatus = Objects.requireNonNull(
                mutationStatus, "mutationStatus cannot be null");
        responseClassification = Objects.requireNonNull(
                responseClassification, "responseClassification cannot be null");
        responseFingerprint = requireFingerprint(
                responseFingerprint, "responseFingerprint", true);
        observedSupervisorEventFingerprint = requireFingerprint(
                observedSupervisorEventFingerprint,
                "observedSupervisorEventFingerprint",
                true);
        applicationCommitStatus = Objects.requireNonNull(
                applicationCommitStatus, "applicationCommitStatus cannot be null");
        if (retryAttempt < 0 || retryAttempt > HARD_MAX_RETRY_ATTEMPTS) {
            throw new IllegalArgumentException("retryAttempt is outside hard bounds");
        }
        reasonCode = requireReasonCode(reasonCode);
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        metadata = safeMetadata(metadata);
        predecessorFingerprint = requirePredecessor(predecessorFingerprint, sequence);
        currentFingerprint = requireFingerprint(
                currentFingerprint, "currentFingerprint", false);
        requireCommandEvidence(
                ledgerSide,
                commandType,
                transactionId,
                allocationGeneration,
                requestedAllocationFingerprint);
        requireEventOutcome(
                eventType,
                commandType,
                authenticationResult,
                validationResult,
                duplicateClassification,
                mutationStatus,
                responseClassification,
                responseFingerprint,
                observedSupervisorEventFingerprint,
                applicationCommitStatus,
                retryAttempt,
                installedFingerprintAfter);
    }

    /** Matches only the exact canonical protocol command identity and fences. */
    public boolean correlates(Request request) {
        Request safe = Objects.requireNonNull(request, "request cannot be null");
        return correlationId.equals(safe.requestId())
                && requestFingerprint.equals(safe.requestFingerprint())
                && transactionId.equals(safe.transactionId())
                && experimentId.equals(safe.experimentId())
                && commandType == safe.commandType()
                && applicationInstanceId.equals(safe.applicationInstanceId())
                && applicationOwnerGeneration == safe.applicationOwnerGeneration()
                && supervisorInstanceId.equals(safe.expectedSupervisorInstanceId())
                && supervisorGeneration == safe.expectedSupervisorGeneration()
                && requestedAllocationFingerprint.equals(safe.allocationFingerprint())
                && previousCommittedFingerprint.equals(safe.previousCommittedFingerprint())
                && allocationGeneration == requestAllocationGeneration(safe);
    }

    /** Matches the exact response identity learned by either ledger. */
    public boolean observes(Response response) {
        Response safe = Objects.requireNonNull(response, "response cannot be null");
        return correlationId.equals(safe.requestId())
                && requestFingerprint.equals(safe.requestFingerprint())
                && commandType == safe.commandType()
                && responseFingerprint.equals(safe.responseFingerprint())
                && (NONE.equals(installedFingerprintAfter)
                || installedFingerprintAfter.equals(safe.installedFingerprint()))
                && (safe.status() != EnterpriseLabSupervisorProtocol.ResponseStatus.ACCEPTED
                || NONE.equals(supervisorInstanceId)
                || (supervisorInstanceId.equals(safe.supervisorInstanceId())
                && supervisorGeneration == safe.supervisorGeneration()))
                && (safe.status() != EnterpriseLabSupervisorProtocol.ResponseStatus.ACCEPTED
                || (!commandType.mutation()
                && commandType != CommandType.VERIFY_ALLOCATION)
                || applicationOwnerGeneration == safe.observedApplicationGeneration());
    }

    /** Draft excludes the schema and codec-controlled current fingerprint. */
    public record Draft(
            LedgerSide ledgerSide,
            long sequence,
            EventType eventType,
            String correlationId,
            String requestFingerprint,
            String transactionId,
            Optional<String> experimentId,
            CommandType commandType,
            String applicationInstanceId,
            long applicationOwnerGeneration,
            String supervisorInstanceId,
            long supervisorGeneration,
            long allocationGeneration,
            String requestedAllocationFingerprint,
            String previousCommittedFingerprint,
            String installedFingerprintBefore,
            String installedFingerprintAfter,
            long routerGenerationBefore,
            long routerGenerationAfter,
            AuthenticationResult authenticationResult,
            ValidationResult validationResult,
            DuplicateClassification duplicateClassification,
            MutationStatus mutationStatus,
            ResponseClassification responseClassification,
            String responseFingerprint,
            String observedSupervisorEventFingerprint,
            ApplicationCommitStatus applicationCommitStatus,
            int retryAttempt,
            String reasonCode,
            Instant occurredAt,
            Map<String, String> metadata,
            String predecessorFingerprint) {

        public Draft {
            experimentId = Objects.requireNonNull(
                    experimentId, "experimentId cannot be null");
            metadata = Map.copyOf(Objects.requireNonNull(
                    metadata, "metadata cannot be null"));
        }
    }

    public enum LedgerSide {
        APPLICATION,
        SUPERVISOR
    }

    public enum EventType {
        APPLICATION_INTENT_PERSISTED,
        DISPATCH_ATTEMPTED,
        SUPERVISOR_RECEIPT_PERSISTED,
        AUTHENTICATION_REJECTED,
        VALIDATION_REJECTED,
        DUPLICATE_ACCEPTED,
        DUPLICATE_REJECTED,
        MUTATION_STARTED,
        ALLOCATION_APPLIED,
        READ_BACK_VERIFIED,
        SUPERVISOR_COMMITTED,
        RESPONSE_SENT,
        APPLICATION_RESPONSE_RECEIVED,
        APPLICATION_COMMITTED,
        RESPONSE_LOST,
        TIMEOUT_OBSERVED,
        RETRY_ISSUED,
        RECONCILIATION_COMPLETED,
        COMMAND_FAILED,
        COMMAND_QUARANTINED
    }

    public enum AuthenticationResult {
        NOT_ATTEMPTED,
        ACCEPTED,
        REJECTED
    }

    public enum ValidationResult {
        NOT_ATTEMPTED,
        ACCEPTED,
        REJECTED
    }

    public enum DuplicateClassification {
        NOT_EVALUATED,
        FIRST_OBSERVATION,
        IDENTICAL_RETRY,
        CONFLICTING_CORRELATION,
        CONFLICTING_TRANSACTION,
        STALE_APPLICATION_GENERATION
    }

    public enum MutationStatus {
        NOT_ATTEMPTED,
        STARTED,
        APPLIED,
        READ_BACK_VERIFIED,
        COMMITTED,
        FAILED,
        QUARANTINED
    }

    public enum ResponseClassification {
        NOT_ATTEMPTED,
        CONSTRUCTED,
        SENT,
        RECEIVED,
        LOST,
        TIMED_OUT,
        REJECTED
    }

    public enum ApplicationCommitStatus {
        NOT_ATTEMPTED,
        PENDING,
        COMMITTED,
        FAILED
    }

    private static void requireEventSide(LedgerSide side, EventType eventType) {
        if (APPLICATION_EVENTS.contains(eventType) && side != LedgerSide.APPLICATION) {
            throw new IllegalArgumentException("eventType belongs only to the application ledger");
        }
        if (SUPERVISOR_EVENTS.contains(eventType) && side != LedgerSide.SUPERVISOR) {
            throw new IllegalArgumentException("eventType belongs only to the supervisor ledger");
        }
    }

    private static long requestAllocationGeneration(Request request) {
        String value = request.metadata().get("applicationAllocationGeneration");
        if (value == null) {
            return 0L;
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 1L
                    || parsed > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION
                    || !Long.toString(parsed).equals(value)) {
                return -1L;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }

    private static void requireCommandEvidence(
            LedgerSide ledgerSide,
            CommandType commandType,
            String transactionId,
            long allocationGeneration,
            String requestedFingerprint) {
        if (commandType.classification()
                == EnterpriseLabSupervisorProtocol.CommandClassification.ALLOCATION_MUTATION
                && (NONE.equals(transactionId)
                || (ledgerSide == LedgerSide.APPLICATION && allocationGeneration < 1L)
                || NONE.equals(requestedFingerprint))) {
            throw new IllegalArgumentException(
                    "allocation mutation evidence requires transaction, requested fingerprint,"
                            + " and application-side allocation generation");
        }
    }

    private static void requireEventOutcome(
            EventType eventType,
            CommandType commandType,
            AuthenticationResult authentication,
            ValidationResult validation,
            DuplicateClassification duplicate,
            MutationStatus mutation,
            ResponseClassification response,
            String responseFingerprint,
            String observedSupervisorEventFingerprint,
            ApplicationCommitStatus applicationCommit,
            int retryAttempt,
            String installedAfter) {
        switch (eventType) {
            case SUPERVISOR_RECEIPT_PERSISTED -> require(
                    authentication == AuthenticationResult.ACCEPTED,
                    "supervisor receipt requires accepted authentication");
            case AUTHENTICATION_REJECTED -> require(
                    authentication == AuthenticationResult.REJECTED,
                    "authentication rejection requires rejected authentication evidence");
            case VALIDATION_REJECTED -> require(
                    validation == ValidationResult.REJECTED,
                    "validation rejection requires rejected validation evidence");
            case DUPLICATE_ACCEPTED -> require(
                    duplicate == DuplicateClassification.IDENTICAL_RETRY,
                    "accepted duplicate must be an identical retry");
            case DUPLICATE_REJECTED -> require(
                    duplicate == DuplicateClassification.CONFLICTING_CORRELATION
                            || duplicate == DuplicateClassification.CONFLICTING_TRANSACTION
                            || duplicate == DuplicateClassification.STALE_APPLICATION_GENERATION,
                    "rejected duplicate requires a bounded conflict classification");
            case MUTATION_STARTED -> requireMutation(
                    commandType, mutation, MutationStatus.STARTED);
            case ALLOCATION_APPLIED -> requireMutation(
                    commandType, mutation, MutationStatus.APPLIED);
            case READ_BACK_VERIFIED -> {
                requireMutation(commandType, mutation, MutationStatus.READ_BACK_VERIFIED);
                require(!NONE.equals(installedAfter),
                        "verified read-back requires an installed fingerprint");
            }
            case SUPERVISOR_COMMITTED -> {
                requireMutation(commandType, mutation, MutationStatus.COMMITTED);
                require(!NONE.equals(installedAfter),
                        "supervisor commit requires an installed fingerprint");
            }
            case RESPONSE_SENT -> {
                require(response == ResponseClassification.SENT,
                        "response sent event requires sent response evidence");
                require(!NONE.equals(responseFingerprint),
                        "response sent event requires a canonical response fingerprint");
            }
            case APPLICATION_RESPONSE_RECEIVED -> {
                require(response == ResponseClassification.RECEIVED,
                        "application response event requires received response evidence");
                require(!NONE.equals(responseFingerprint)
                                && !NONE.equals(observedSupervisorEventFingerprint),
                        "application response event requires response and supervisor event fingerprints");
            }
            case APPLICATION_COMMITTED -> {
                require(applicationCommit == ApplicationCommitStatus.COMMITTED,
                        "application committed event requires committed application evidence");
                require(!NONE.equals(observedSupervisorEventFingerprint),
                        "application commit requires observed supervisor event evidence");
            }
            case RESPONSE_LOST -> require(
                    response == ResponseClassification.LOST,
                    "response lost event requires lost response evidence");
            case TIMEOUT_OBSERVED -> require(
                    response == ResponseClassification.TIMED_OUT,
                    "timeout event requires timeout response evidence");
            case RETRY_ISSUED -> require(
                    retryAttempt >= 1,
                    "retry issued event requires a positive retry attempt");
            case COMMAND_QUARANTINED -> require(
                    mutation == MutationStatus.QUARANTINED,
                    "quarantined event requires quarantined mutation evidence");
            default -> {
                // Other event types carry their explicit bounded fields without a derived shortcut.
            }
        }
    }

    private static void requireMutation(
            CommandType commandType,
            MutationStatus actual,
            MutationStatus expected) {
        require(commandType.classification()
                        == EnterpriseLabSupervisorProtocol.CommandClassification.ALLOCATION_MUTATION,
                "mutation event requires an allocation mutation command");
        require(actual == expected, "mutation event status does not match eventType");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String requireId(String value, String field, boolean allowNone) {
        if (allowNone && NONE.equals(value)) {
            return value;
        }
        if (value == null || !CANONICAL_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be a bounded canonical identifier");
        }
        return value;
    }

    private static Optional<String> copyOptionalId(Optional<String> value, String field) {
        Optional<String> safe = Objects.requireNonNull(value, field + " cannot be null");
        return safe.map(item -> requireId(item, field, false));
    }

    private static long requireGeneration(long value, boolean allowZero, String field) {
        if ((allowZero && value == 0L)
                || (value >= EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                && value <= EnterpriseLabEvidenceOwnership.MAX_GENERATION)) {
            return value;
        }
        throw new IllegalArgumentException(field + " is outside hard bounds");
    }

    private static long requireRouterGeneration(long value, String field) {
        if (value < 0L || value > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION) {
            throw new IllegalArgumentException(field + " is outside hard bounds");
        }
        return value;
    }

    private static void requirePairedFence(String supervisorInstanceId, long generation) {
        if (NONE.equals(supervisorInstanceId) != (generation == 0L)) {
            throw new IllegalArgumentException(
                    "supervisor identity and generation must both be present or absent");
        }
    }

    private static String requireFingerprint(String value, String field, boolean allowNone) {
        if (allowNone && NONE.equals(value)) {
            return value;
        }
        if (value == null || !SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
        return value;
    }

    private static String requireReasonCode(String value) {
        if (value == null || !REASON_CODE.matcher(value).matches()) {
            throw new IllegalArgumentException("reasonCode must be a bounded canonical code");
        }
        return value;
    }

    private static String requirePredecessor(String value, long sequence) {
        if (sequence == 1L && GENESIS_FINGERPRINT.equals(value)) {
            return value;
        }
        if (sequence > 1L && value != null && SHA_256.matcher(value).matches()) {
            return value;
        }
        throw new IllegalArgumentException(
                "predecessorFingerprint must be GENESIS only for sequence one and SHA-256 afterward");
    }

    private static Map<String, String> safeMetadata(Map<String, String> value) {
        Map<String, String> safe = Objects.requireNonNull(value, "metadata cannot be null");
        if (safe.size() > HARD_MAX_METADATA_ENTRIES) {
            throw new IllegalArgumentException("metadata exceeds its hard entry bound");
        }
        TreeMap<String, String> ordered = new TreeMap<>();
        safe.forEach((key, item) -> {
            if (key == null || key.isBlank() || key.length() > MAX_METADATA_KEY_LENGTH
                    || item == null || item.length() > MAX_METADATA_VALUE_LENGTH) {
                throw new IllegalArgumentException("metadata key or value is outside hard bounds");
            }
            String normalized = key.toLowerCase(java.util.Locale.ROOT)
                    .replace("-", "").replace("_", "").replace(".", "");
            if (SENSITIVE_KEYS.contains(normalized)
                    || FORBIDDEN_CONTROL_KEYS.contains(normalized)
                    || UNSAFE_EVIDENCE.matcher(item).find()
                    || UNSAFE_LOCATION.matcher(item).find()) {
                throw new IllegalArgumentException(
                        "metadata cannot contain credentials, controls, locations, or stack traces");
            }
            ordered.put(key, item);
        });
        return Collections.unmodifiableMap(ordered);
    }
}
