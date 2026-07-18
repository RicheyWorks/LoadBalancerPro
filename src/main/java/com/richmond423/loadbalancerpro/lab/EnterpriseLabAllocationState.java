package com.richmond423.loadbalancerpro.lab;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Immutable, bounded state for one Enterprise Lab loopback allocation transaction.
 * Fingerprints detect content changes; they do not authenticate an author.
 */
public final class EnterpriseLabAllocationState {
    public static final String SCHEMA_VERSION = "enterprise-lab-allocation-state/v1";
    public static final String GENESIS_FINGERPRINT = "GENESIS";
    public static final String NO_FINGERPRINT = "NONE";
    public static final long HARD_MAX_ALLOCATION_GENERATION = 1_000_000;
    public static final int HARD_MAX_METADATA_ENTRIES = 16;

    private static final int MAX_ID_LENGTH = 128;
    private static final int MAX_REASON_CODE_LENGTH = 64;
    private static final int MAX_REASON_MESSAGE_LENGTH = 512;
    private static final int MAX_METADATA_KEY_LENGTH = 64;
    private static final int MAX_METADATA_VALUE_LENGTH = 256;
    private static final Pattern CANONICAL_ID = Pattern.compile("[A-Za-z0-9._:-]+");
    private static final Pattern REASON_CODE = Pattern.compile("[A-Z0-9][A-Z0-9_.:-]*");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern CREDENTIAL_VALUE = Pattern.compile(
            "(?i)(?:\\bbearer\\s+[A-Za-z0-9._~+/-]{12,}={0,2}"
                    + "|-----BEGIN [^-\\r\\n]{1,40}PRIVATE KEY-----"
                    + "|\\b(?:api[_-]?key|password|access[_-]?token|refresh[_-]?token|client[_-]?secret)"
                    + "\\s*[:=]\\s*\\S+|\\bcookie\\s*:\\s*\\S+)");
    private static final Pattern STACK_TRACE = Pattern.compile(
            "(?m)(?:^|\\R)\\s*at\\s+[A-Za-z0-9_.$]+\\([^\\r\\n]*\\.java:\\d+\\)");
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "authorization", "password", "passwd", "apikey", "accesstoken", "refreshtoken",
            "cookie", "setcookie", "privatekey", "clientsecret", "credential", "credentials",
            "stacktrace", "throwable");

    private final String schemaVersion;
    private final String allocationTransactionId;
    private final Optional<String> experimentId;
    private final String scenarioId;
    private final long ownerGeneration;
    private final long allocationGeneration;
    private final AllocationPurpose allocationPurpose;
    private final Map<String, Double> baselineAllocation;
    private final Map<String, Double> requestedAllocation;
    private final Map<String, Double> guardrailApprovedAllocation;
    private final Map<String, Double> installedAllocation;
    private final String normalizedAllocationFingerprint;
    private final String routerReadBackFingerprint;
    private final String previousCommittedAllocationFingerprint;
    private final TransactionPhase transactionPhase;
    private final TransitionReason transitionReason;
    private final boolean actionPerformed;
    private final Instant createdAt;
    private final Optional<Instant> lastVerifiedAt;
    private final VerificationResult verificationResult;
    private final RecoveryClassification recoveryClassification;
    private final String predecessorRecordFingerprint;
    private final String currentRecordFingerprint;
    private final Map<String, String> metadata;

    private EnterpriseLabAllocationState(
            String schemaVersion,
            String allocationTransactionId,
            Optional<String> experimentId,
            String scenarioId,
            long ownerGeneration,
            long allocationGeneration,
            AllocationPurpose allocationPurpose,
            Map<String, Double> baselineAllocation,
            Map<String, Double> requestedAllocation,
            Map<String, Double> guardrailApprovedAllocation,
            Map<String, Double> installedAllocation,
            String routerReadBackFingerprint,
            String previousCommittedAllocationFingerprint,
            TransactionPhase transactionPhase,
            TransitionReason transitionReason,
            boolean actionPerformed,
            Instant createdAt,
            Optional<Instant> lastVerifiedAt,
            VerificationResult verificationResult,
            RecoveryClassification recoveryClassification,
            String predecessorRecordFingerprint,
            Map<String, String> metadata,
            EnterpriseLabExperimentTargetCatalog targetCatalog) {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported allocation state schemaVersion");
        }
        this.schemaVersion = schemaVersion;
        this.allocationTransactionId = requireCanonicalId(
                allocationTransactionId, "allocationTransactionId");
        this.experimentId = requireOptionalId(experimentId, "experimentId");
        this.scenarioId = requireCanonicalId(scenarioId, "scenarioId");
        if (ownerGeneration < EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                || ownerGeneration > EnterpriseLabEvidenceOwnership.MAX_GENERATION) {
            throw new IllegalArgumentException("ownerGeneration is outside hard bounds");
        }
        this.ownerGeneration = ownerGeneration;
        if (allocationGeneration < 1 || allocationGeneration > HARD_MAX_ALLOCATION_GENERATION) {
            throw new IllegalArgumentException("allocationGeneration must be between 1 and 1000000");
        }
        this.allocationGeneration = allocationGeneration;
        this.allocationPurpose = Objects.requireNonNull(
                allocationPurpose, "allocationPurpose cannot be null");

        Set<String> approvedBackendIds = approvedBackendIds(targetCatalog, this.scenarioId);
        this.baselineAllocation = normalize(
                approvedBackendIds, baselineAllocation, "baselineAllocation");
        this.requestedAllocation = normalize(
                approvedBackendIds, requestedAllocation, "requestedAllocation");
        this.guardrailApprovedAllocation = normalize(
                approvedBackendIds, guardrailApprovedAllocation, "guardrailApprovedAllocation");
        this.installedAllocation = normalize(
                approvedBackendIds, installedAllocation, "installedAllocation");
        this.normalizedAllocationFingerprint =
                EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                        this.scenarioId, this.guardrailApprovedAllocation);
        this.routerReadBackFingerprint = requireOptionalFingerprint(
                routerReadBackFingerprint, "routerReadBackFingerprint");
        if (!NO_FINGERPRINT.equals(this.routerReadBackFingerprint)) {
            String installedFingerprint = EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                    this.scenarioId, this.installedAllocation);
            if (!installedFingerprint.equals(this.routerReadBackFingerprint)) {
                throw new IllegalArgumentException(
                        "routerReadBackFingerprint must match the normalized installedAllocation");
            }
        }
        this.previousCommittedAllocationFingerprint = requireOptionalFingerprint(
                previousCommittedAllocationFingerprint, "previousCommittedAllocationFingerprint");
        this.transactionPhase = Objects.requireNonNull(
                transactionPhase, "transactionPhase cannot be null");
        this.transitionReason = Objects.requireNonNull(
                transitionReason, "transitionReason cannot be null");
        this.actionPerformed = actionPerformed;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.lastVerifiedAt = Objects.requireNonNull(
                lastVerifiedAt, "lastVerifiedAt cannot be null");
        this.lastVerifiedAt.ifPresent(value -> {
            if (value.isAfter(this.createdAt)) {
                throw new IllegalArgumentException("lastVerifiedAt cannot be after createdAt");
            }
        });
        this.verificationResult = Objects.requireNonNull(
                verificationResult, "verificationResult cannot be null");
        this.recoveryClassification = Objects.requireNonNull(
                recoveryClassification, "recoveryClassification cannot be null");
        validateVerificationState();
        this.predecessorRecordFingerprint = requirePredecessorFingerprint(
                predecessorRecordFingerprint);
        this.metadata = immutableMetadata(metadata);
        this.currentRecordFingerprint = EnterpriseLabAllocationStateCodec.canonicalRecordFingerprint(this);
    }

    /**
     * Creates a record from the authoritative live ownership resource. The
     * owner generation is deliberately absent from {@link Draft}.
     */
    static EnterpriseLabAllocationState create(
            Clock clock,
            EnterpriseLabEvidenceMutationAuthority mutationAuthority,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            Draft draft) {
        Clock safeClock = Objects.requireNonNull(clock, "clock cannot be null");
        EnterpriseLabEvidenceMutationAuthority safeAuthority = Objects.requireNonNull(
                mutationAuthority, "mutationAuthority cannot be null");
        Draft safeDraft = Objects.requireNonNull(draft, "draft cannot be null");
        var authorization = safeAuthority.requireMutationAuthorization();
        EnterpriseLabAllocationState state = new EnterpriseLabAllocationState(
                SCHEMA_VERSION,
                safeDraft.allocationTransactionId(),
                safeDraft.experimentId(),
                safeDraft.scenarioId(),
                authorization.generation(),
                safeDraft.allocationGeneration(),
                safeDraft.allocationPurpose(),
                safeDraft.baselineAllocation(),
                safeDraft.requestedAllocation(),
                safeDraft.guardrailApprovedAllocation(),
                safeDraft.installedAllocation(),
                safeDraft.routerReadBackFingerprint(),
                safeDraft.previousCommittedAllocationFingerprint(),
                safeDraft.transactionPhase(),
                safeDraft.transitionReason(),
                safeDraft.actionPerformed(),
                safeClock.instant(),
                safeDraft.lastVerifiedAt(),
                safeDraft.verificationResult(),
                safeDraft.recoveryClassification(),
                safeDraft.predecessorRecordFingerprint(),
                safeDraft.metadata(),
                targetCatalog);
        authorization.requireSameEpoch(safeAuthority.requireMutationAuthorization());
        return state;
    }

    static EnterpriseLabAllocationState reconstitute(
            String schemaVersion,
            String allocationTransactionId,
            Optional<String> experimentId,
            String scenarioId,
            long ownerGeneration,
            long allocationGeneration,
            AllocationPurpose allocationPurpose,
            Map<String, Double> baselineAllocation,
            Map<String, Double> requestedAllocation,
            Map<String, Double> guardrailApprovedAllocation,
            Map<String, Double> installedAllocation,
            String routerReadBackFingerprint,
            String previousCommittedAllocationFingerprint,
            TransactionPhase transactionPhase,
            TransitionReason transitionReason,
            boolean actionPerformed,
            Instant createdAt,
            Optional<Instant> lastVerifiedAt,
            VerificationResult verificationResult,
            RecoveryClassification recoveryClassification,
            String predecessorRecordFingerprint,
            Map<String, String> metadata,
            EnterpriseLabExperimentTargetCatalog targetCatalog) {
        return new EnterpriseLabAllocationState(
                schemaVersion, allocationTransactionId, experimentId, scenarioId,
                ownerGeneration, allocationGeneration, allocationPurpose,
                baselineAllocation, requestedAllocation, guardrailApprovedAllocation,
                installedAllocation, routerReadBackFingerprint,
                previousCommittedAllocationFingerprint, transactionPhase, transitionReason,
                actionPerformed, createdAt, lastVerifiedAt, verificationResult,
                recoveryClassification, predecessorRecordFingerprint, metadata, targetCatalog);
    }

    void validateAgainst(EnterpriseLabExperimentTargetCatalog targetCatalog) {
        Set<String> approved = approvedBackendIds(targetCatalog, scenarioId);
        normalize(approved, baselineAllocation, "baselineAllocation");
        normalize(approved, requestedAllocation, "requestedAllocation");
        normalize(approved, guardrailApprovedAllocation, "guardrailApprovedAllocation");
        normalize(approved, installedAllocation, "installedAllocation");
    }

    private void validateVerificationState() {
        boolean readBackAvailable = !NO_FINGERPRINT.equals(routerReadBackFingerprint);
        if (verificationResult == VerificationResult.NOT_ATTEMPTED && readBackAvailable) {
            throw new IllegalArgumentException("unattempted verification cannot claim router read-back");
        }
        if (verificationResult == VerificationResult.READ_BACK_FAILED && readBackAvailable) {
            throw new IllegalArgumentException("failed read-back cannot claim a router fingerprint");
        }
        if ((verificationResult == VerificationResult.MATCHED
                || verificationResult == VerificationResult.MISMATCHED)
                && (lastVerifiedAt.isEmpty() || !readBackAvailable)) {
            throw new IllegalArgumentException(
                    "completed verification requires a timestamp and router read-back fingerprint");
        }
        if (verificationResult == VerificationResult.MATCHED
                && !normalizedAllocationFingerprint.equals(routerReadBackFingerprint)) {
            throw new IllegalArgumentException("matched verification requires equal intended and router fingerprints");
        }
        if (verificationResult == VerificationResult.MISMATCHED
                && normalizedAllocationFingerprint.equals(routerReadBackFingerprint)) {
            throw new IllegalArgumentException("mismatched verification requires different fingerprints");
        }
    }

    private static Set<String> approvedBackendIds(
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            String scenarioId) {
        List<EnterpriseLabLoopbackTarget> targets = Objects.requireNonNull(
                        targetCatalog, "targetCatalog cannot be null")
                .findTargets(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "scenario has no repository-controlled literal-loopback target binding"));
        Set<String> backendIds = new TreeSet<>();
        for (EnterpriseLabLoopbackTarget target : targets) {
            if (!scenarioId.equals(target.scenarioId()) || !backendIds.add(target.backendId())) {
                throw new IllegalArgumentException("target binding is not a unique approved scenario allocation");
            }
        }
        return Set.copyOf(backendIds);
    }

    private static Map<String, Double> normalize(
            Set<String> approvedBackendIds,
            Map<String, Double> values,
            String fieldName) {
        try {
            return EnterpriseLabLoopbackAllocationSnapshot.exactNormalizedAllocations(
                    approvedBackendIds,
                    Objects.requireNonNull(values, fieldName + " cannot be null"));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " is invalid: " + exception.getMessage(), exception);
        }
    }

    private static Optional<String> requireOptionalId(Optional<String> value, String fieldName) {
        Optional<String> safe = Objects.requireNonNull(value, fieldName + " cannot be null");
        return safe.map(item -> requireCanonicalId(item, fieldName));
    }

    private static String requireCanonicalId(String value, String fieldName) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > MAX_ID_LENGTH || !CANONICAL_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a bounded canonical identifier");
        }
        return value;
    }

    private static String requireOptionalFingerprint(String value, String fieldName) {
        if (NO_FINGERPRINT.equals(value) || (value != null && SHA_256.matcher(value).matches())) {
            return value;
        }
        throw new IllegalArgumentException(fieldName + " must be NONE or lowercase SHA-256");
    }

    private static String requirePredecessorFingerprint(String value) {
        if (GENESIS_FINGERPRINT.equals(value) || (value != null && SHA_256.matcher(value).matches())) {
            return value;
        }
        throw new IllegalArgumentException(
                "predecessorRecordFingerprint must be GENESIS or lowercase SHA-256");
    }

    private static Map<String, String> immutableMetadata(Map<String, String> values) {
        Objects.requireNonNull(values, "metadata cannot be null");
        if (values.size() > HARD_MAX_METADATA_ENTRIES) {
            throw new IllegalArgumentException("metadata cannot exceed 16 entries");
        }
        Map<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = requireBoundedPlainText(entry.getKey(), "metadata key", MAX_METADATA_KEY_LENGTH);
            String value = requireBoundedPlainText(
                    entry.getValue(), "metadata value", MAX_METADATA_VALUE_LENGTH);
            rejectSensitiveKey(key);
            rejectUnsafeEvidenceText(value);
            if (sorted.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("metadata keys must be unique");
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    private static String requireBoundedPlainText(String value, String fieldName, int maximumLength) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " must be non-blank, trimmed, and bounded");
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                throw new IllegalArgumentException(fieldName + " cannot contain control characters");
            }
        }
        return value;
    }

    private static void rejectSensitiveKey(String key) {
        String normalized = key.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (SENSITIVE_KEYS.contains(normalized)) {
            throw new EnterpriseLabAllocationStateCodec.CodecException(
                    EnterpriseLabAllocationStateCodec.Failure.SENSITIVE_CONTENT,
                    "allocation state cannot contain credential or stack-trace fields");
        }
    }

    private static void rejectUnsafeEvidenceText(String value) {
        if (CREDENTIAL_VALUE.matcher(value).find() || STACK_TRACE.matcher(value).find()) {
            throw new EnterpriseLabAllocationStateCodec.CodecException(
                    EnterpriseLabAllocationStateCodec.Failure.SENSITIVE_CONTENT,
                    "allocation state cannot contain credentials or stack traces");
        }
    }

    public String schemaVersion() {
        return schemaVersion;
    }

    public String allocationTransactionId() {
        return allocationTransactionId;
    }

    public Optional<String> experimentId() {
        return experimentId;
    }

    public String scenarioId() {
        return scenarioId;
    }

    public long ownerGeneration() {
        return ownerGeneration;
    }

    public long allocationGeneration() {
        return allocationGeneration;
    }

    public AllocationPurpose allocationPurpose() {
        return allocationPurpose;
    }

    public Map<String, Double> baselineAllocation() {
        return baselineAllocation;
    }

    public Map<String, Double> requestedAllocation() {
        return requestedAllocation;
    }

    public Map<String, Double> guardrailApprovedAllocation() {
        return guardrailApprovedAllocation;
    }

    public Map<String, Double> installedAllocation() {
        return installedAllocation;
    }

    public String normalizedAllocationFingerprint() {
        return normalizedAllocationFingerprint;
    }

    public String routerReadBackFingerprint() {
        return routerReadBackFingerprint;
    }

    public String previousCommittedAllocationFingerprint() {
        return previousCommittedAllocationFingerprint;
    }

    public TransactionPhase transactionPhase() {
        return transactionPhase;
    }

    public TransitionReason transitionReason() {
        return transitionReason;
    }

    public boolean actionPerformed() {
        return actionPerformed;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> lastVerifiedAt() {
        return lastVerifiedAt;
    }

    public VerificationResult verificationResult() {
        return verificationResult;
    }

    public RecoveryClassification recoveryClassification() {
        return recoveryClassification;
    }

    public String predecessorRecordFingerprint() {
        return predecessorRecordFingerprint;
    }

    public String currentRecordFingerprint() {
        return currentRecordFingerprint;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EnterpriseLabAllocationState that)) {
            return false;
        }
        return ownerGeneration == that.ownerGeneration
                && allocationGeneration == that.allocationGeneration
                && actionPerformed == that.actionPerformed
                && schemaVersion.equals(that.schemaVersion)
                && allocationTransactionId.equals(that.allocationTransactionId)
                && experimentId.equals(that.experimentId)
                && scenarioId.equals(that.scenarioId)
                && allocationPurpose == that.allocationPurpose
                && baselineAllocation.equals(that.baselineAllocation)
                && requestedAllocation.equals(that.requestedAllocation)
                && guardrailApprovedAllocation.equals(that.guardrailApprovedAllocation)
                && installedAllocation.equals(that.installedAllocation)
                && normalizedAllocationFingerprint.equals(that.normalizedAllocationFingerprint)
                && routerReadBackFingerprint.equals(that.routerReadBackFingerprint)
                && previousCommittedAllocationFingerprint.equals(that.previousCommittedAllocationFingerprint)
                && transactionPhase == that.transactionPhase
                && transitionReason.equals(that.transitionReason)
                && createdAt.equals(that.createdAt)
                && lastVerifiedAt.equals(that.lastVerifiedAt)
                && verificationResult == that.verificationResult
                && recoveryClassification == that.recoveryClassification
                && predecessorRecordFingerprint.equals(that.predecessorRecordFingerprint)
                && currentRecordFingerprint.equals(that.currentRecordFingerprint)
                && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                schemaVersion, allocationTransactionId, experimentId, scenarioId, ownerGeneration,
                allocationGeneration, allocationPurpose, baselineAllocation, requestedAllocation,
                guardrailApprovedAllocation, installedAllocation, normalizedAllocationFingerprint,
                routerReadBackFingerprint, previousCommittedAllocationFingerprint, transactionPhase,
                transitionReason, actionPerformed, createdAt, lastVerifiedAt, verificationResult,
                recoveryClassification, predecessorRecordFingerprint, currentRecordFingerprint, metadata);
    }

    public enum AllocationPurpose {
        INITIAL_SAFE_BASELINE,
        EXPERIMENT_CANDIDATE,
        EXPERIMENT_HOLD,
        ROLLBACK_RESTORATION,
        STARTUP_RESTORATION,
        TAKEOVER_RESTORATION,
        CANCELLATION_RESTORATION,
        OPERATOR_REQUESTED_SAFE_RESET,
        RECONCILIATION_NO_OP
    }

    public enum TransactionPhase {
        PREPARED,
        INTENT_PERSISTED,
        APPLYING,
        APPLIED,
        VERIFYING,
        COMMITTED,
        RESTORE_REQUIRED,
        RESTORING,
        RESTORED,
        REJECTED,
        FAILED,
        QUARANTINED
    }

    public enum VerificationResult {
        NOT_ATTEMPTED,
        MATCHED,
        MISMATCHED,
        READ_BACK_FAILED
    }

    public enum RecoveryClassification {
        NOT_REQUIRED,
        INCOMPLETE_TRANSACTION,
        DRIFT_DETECTED,
        BASELINE_RESTORATION_REQUIRED,
        BASELINE_RESTORED,
        REJECTED,
        FAILED,
        QUARANTINED
    }

    public record TransitionReason(String code, String message) {
        public TransitionReason {
            code = requireBoundedPlainText(code, "transition reason code", MAX_REASON_CODE_LENGTH);
            if (!REASON_CODE.matcher(code).matches()) {
                throw new IllegalArgumentException(
                        "transition reason code must use canonical uppercase characters");
            }
            message = requireBoundedPlainText(
                    message, "transition reason message", MAX_REASON_MESSAGE_LENGTH);
            rejectUnsafeEvidenceText(message);
        }
    }

    /** Draft input deliberately excludes schema, owner generation, timestamps, and current fingerprint. */
    public record Draft(
            String allocationTransactionId,
            Optional<String> experimentId,
            String scenarioId,
            long allocationGeneration,
            AllocationPurpose allocationPurpose,
            Map<String, Double> baselineAllocation,
            Map<String, Double> requestedAllocation,
            Map<String, Double> guardrailApprovedAllocation,
            Map<String, Double> installedAllocation,
            String routerReadBackFingerprint,
            String previousCommittedAllocationFingerprint,
            TransactionPhase transactionPhase,
            TransitionReason transitionReason,
            boolean actionPerformed,
            Optional<Instant> lastVerifiedAt,
            VerificationResult verificationResult,
            RecoveryClassification recoveryClassification,
            String predecessorRecordFingerprint,
            Map<String, String> metadata) {
    }
}
