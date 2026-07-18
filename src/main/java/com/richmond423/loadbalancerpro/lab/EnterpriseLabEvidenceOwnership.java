package com.richmond423.loadbalancerpro.lab;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Immutable single-host ownership vocabulary. The operating-system lock is the
 * exclusion authority; these values provide bounded audit and fencing context.
 */
public final class EnterpriseLabEvidenceOwnership {
    public static final String RECORD_SCHEMA_VERSION = "enterprise-lab-evidence-owner-record/v1";
    public static final String GENESIS_FINGERPRINT = "GENESIS";
    public static final String NO_RECORD_FINGERPRINT = "NONE";
    public static final long INITIAL_GENERATION = 1L;
    public static final long MAX_GENERATION = Long.MAX_VALUE - 1L;
    public static final long MAX_TAKEOVER_SEQUENCE = 1_000_000L;

    private static final Pattern CANONICAL_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private static final Pattern CANONICAL_CODE = Pattern.compile("[A-Z0-9][A-Z0-9_.:-]{0,63}");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private EnterpriseLabEvidenceOwnership() {
    }

    public record Policy(
            Duration leaseDuration,
            Duration renewalInterval,
            int acquisitionAttempts,
            int renewalAttempts,
            Duration retryDelay) {
        public static final Duration HARD_MAX_LEASE_DURATION = Duration.ofMinutes(10);
        public static final Duration HARD_MAX_RENEWAL_INTERVAL = Duration.ofMinutes(2);
        public static final Duration HARD_MAX_RETRY_DELAY = Duration.ofSeconds(2);
        public static final int HARD_MAX_ATTEMPTS = 8;

        public Policy {
            leaseDuration = requirePositiveDuration(
                    leaseDuration, "leaseDuration", HARD_MAX_LEASE_DURATION);
            renewalInterval = requirePositiveDuration(
                    renewalInterval, "renewalInterval", HARD_MAX_RENEWAL_INTERVAL);
            retryDelay = requireNonNegativeDuration(retryDelay, "retryDelay", HARD_MAX_RETRY_DELAY);
            if (renewalInterval.multipliedBy(2).compareTo(leaseDuration) > 0) {
                throw new IllegalArgumentException(
                        "leaseDuration must allow at least two bounded renewal intervals");
            }
            if (acquisitionAttempts < 1 || acquisitionAttempts > HARD_MAX_ATTEMPTS
                    || renewalAttempts < 1 || renewalAttempts > HARD_MAX_ATTEMPTS) {
                throw new IllegalArgumentException("ownership attempt counts are outside hard bounds");
            }
        }

        public static Policy safetyFirstDefaults() {
            return new Policy(Duration.ofSeconds(30), Duration.ofSeconds(10), 1, 2,
                    Duration.ofMillis(100));
        }
    }

    public record OwnerIdentity(
            String ownerId,
            String applicationInstanceId,
            long processId,
            String hostDiagnosticFingerprint) {
        public OwnerIdentity {
            ownerId = requireId(ownerId, "ownerId");
            applicationInstanceId = requireId(applicationInstanceId, "applicationInstanceId");
            if (processId < 0) {
                throw new IllegalArgumentException("processId must be zero when unavailable or positive");
            }
            hostDiagnosticFingerprint = requireSha(
                    hostDiagnosticFingerprint, "hostDiagnosticFingerprint");
        }
    }

    public record OwnershipRecord(
            String schemaVersion,
            String directoryIdentity,
            String lockFileIdentity,
            OwnerIdentity owner,
            long generation,
            OwnershipState state,
            Instant acquiredAt,
            Instant lastRenewedAt,
            Instant leaseExpiresAt,
            String previousOwnerFingerprint,
            String takeoverReasonCode,
            long takeoverSequence,
            ReconciliationStatus reconciliationStatus,
            ReleaseStatus releaseStatus,
            String recordFingerprint) {
        public OwnershipRecord {
            if (!RECORD_SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IllegalArgumentException("unsupported ownership record schemaVersion");
            }
            directoryIdentity = requireSha(directoryIdentity, "directoryIdentity");
            lockFileIdentity = requireSha(lockFileIdentity, "lockFileIdentity");
            owner = Objects.requireNonNull(owner, "owner cannot be null");
            generation = requireGeneration(generation);
            state = Objects.requireNonNull(state, "state cannot be null");
            acquiredAt = Objects.requireNonNull(acquiredAt, "acquiredAt cannot be null");
            lastRenewedAt = Objects.requireNonNull(lastRenewedAt, "lastRenewedAt cannot be null");
            leaseExpiresAt = Objects.requireNonNull(leaseExpiresAt, "leaseExpiresAt cannot be null");
            previousOwnerFingerprint = requirePriorFingerprint(previousOwnerFingerprint);
            takeoverReasonCode = requireCode(takeoverReasonCode, "takeoverReasonCode");
            reconciliationStatus = Objects.requireNonNull(
                    reconciliationStatus, "reconciliationStatus cannot be null");
            releaseStatus = Objects.requireNonNull(releaseStatus, "releaseStatus cannot be null");
            recordFingerprint = requireSha(recordFingerprint, "recordFingerprint");
            if (lastRenewedAt.isBefore(acquiredAt) || !leaseExpiresAt.isAfter(lastRenewedAt)) {
                throw new IllegalArgumentException("ownership record timestamps are inconsistently ordered");
            }
            if (takeoverSequence < 0 || takeoverSequence > MAX_TAKEOVER_SEQUENCE) {
                throw new IllegalArgumentException("takeoverSequence is outside hard bounds");
            }
            if (generation == INITIAL_GENERATION) {
                if (!GENESIS_FINGERPRINT.equals(previousOwnerFingerprint)
                        || takeoverSequence != 0
                        || !"INITIAL_ACQUISITION".equals(takeoverReasonCode)) {
                    throw new IllegalArgumentException(
                            "initial ownership must use genesis evidence without a takeover");
                }
            } else if (GENESIS_FINGERPRINT.equals(previousOwnerFingerprint)
                    || takeoverSequence < 1) {
                throw new IllegalArgumentException(
                        "later ownership generations require prior evidence and a takeover sequence");
            }
            if ((state == OwnershipState.RELEASED) != (releaseStatus == ReleaseStatus.RELEASED)) {
                throw new IllegalArgumentException(
                        "released ownership state and release evidence must agree");
            }
            String expected = EnterpriseLabEvidenceOwnershipCodec.canonicalFingerprint(
                    schemaVersion, directoryIdentity, lockFileIdentity, owner, generation, state,
                    acquiredAt, lastRenewedAt, leaseExpiresAt, previousOwnerFingerprint,
                    takeoverReasonCode, takeoverSequence, reconciliationStatus, releaseStatus);
            if (!MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    recordFingerprint.getBytes(StandardCharsets.US_ASCII))) {
                throw new IllegalArgumentException(
                        "recordFingerprint does not match canonical ownership content");
            }
        }

        public static OwnershipRecord initial(
                Clock clock,
                Policy policy,
                OwnerIdentity owner,
                String directoryIdentity,
                String lockFileIdentity) {
            Instant acquiredAt = Objects.requireNonNull(clock, "clock cannot be null").instant();
            Policy safePolicy = Objects.requireNonNull(policy, "policy cannot be null");
            return create(
                    directoryIdentity,
                    lockFileIdentity,
                    owner,
                    INITIAL_GENERATION,
                    OwnershipState.OWNED,
                    acquiredAt,
                    acquiredAt,
                    acquiredAt.plus(safePolicy.leaseDuration()),
                    GENESIS_FINGERPRINT,
                    "INITIAL_ACQUISITION",
                    0,
                    ReconciliationStatus.NOT_STARTED,
                    ReleaseStatus.NOT_REQUESTED);
        }

        public static OwnershipRecord create(
                String directoryIdentity,
                String lockFileIdentity,
                OwnerIdentity owner,
                long generation,
                OwnershipState state,
                Instant acquiredAt,
                Instant lastRenewedAt,
                Instant leaseExpiresAt,
                String previousOwnerFingerprint,
                String takeoverReasonCode,
                long takeoverSequence,
                ReconciliationStatus reconciliationStatus,
                ReleaseStatus releaseStatus) {
            String fingerprint = EnterpriseLabEvidenceOwnershipCodec.canonicalFingerprint(
                    RECORD_SCHEMA_VERSION, directoryIdentity, lockFileIdentity, owner, generation,
                    state, acquiredAt, lastRenewedAt, leaseExpiresAt, previousOwnerFingerprint,
                    takeoverReasonCode, takeoverSequence, reconciliationStatus, releaseStatus);
            return new OwnershipRecord(
                    RECORD_SCHEMA_VERSION, directoryIdentity, lockFileIdentity, owner, generation,
                    state, acquiredAt, lastRenewedAt, leaseExpiresAt, previousOwnerFingerprint,
                    takeoverReasonCode, takeoverSequence, reconciliationStatus, releaseStatus,
                    fingerprint);
        }
    }

    public record AcquisitionResult(
            OperationStatus status,
            FailureClassification failure,
            Optional<OwnershipRecord> record,
            String reasonCode) {
        public AcquisitionResult {
            status = Objects.requireNonNull(status, "status cannot be null");
            failure = Objects.requireNonNull(failure, "failure cannot be null");
            record = Objects.requireNonNull(record, "record cannot be null");
            reasonCode = requireCode(reasonCode, "reasonCode");
            requireResultConsistency(status, failure, record.isPresent());
            requireSuccessfulRecordState(status, record, OwnershipState.OWNED,
                    "successful acquisition requires an owned record");
        }
    }

    public record RenewalResult(
            OperationStatus status,
            FailureClassification failure,
            Optional<OwnershipRecord> record,
            String reasonCode) {
        public RenewalResult {
            status = Objects.requireNonNull(status, "status cannot be null");
            failure = Objects.requireNonNull(failure, "failure cannot be null");
            record = Objects.requireNonNull(record, "record cannot be null");
            reasonCode = requireCode(reasonCode, "reasonCode");
            requireResultConsistency(status, failure, record.isPresent());
            requireSuccessfulActiveRecordState(status, record,
                    "successful renewal requires an owned record");
        }
    }

    public record VerificationResult(
            OperationStatus status,
            FailureClassification failure,
            Optional<OwnershipRecord> record,
            boolean operatingSystemLockValid,
            String reasonCode) {
        public VerificationResult {
            status = Objects.requireNonNull(status, "status cannot be null");
            failure = Objects.requireNonNull(failure, "failure cannot be null");
            record = Objects.requireNonNull(record, "record cannot be null");
            reasonCode = requireCode(reasonCode, "reasonCode");
            requireResultConsistency(status, failure, record.isPresent());
            if (status == OperationStatus.SUCCEEDED && !operatingSystemLockValid) {
                throw new IllegalArgumentException("successful ownership verification requires a valid OS lock");
            }
            requireSuccessfulActiveRecordState(status, record,
                    "successful verification requires an owned record");
        }
    }

    public record ReleaseResult(
            OperationStatus status,
            FailureClassification failure,
            Optional<OwnershipRecord> record,
            boolean operatingSystemLockReleased,
            String reasonCode) {
        public ReleaseResult {
            status = Objects.requireNonNull(status, "status cannot be null");
            failure = Objects.requireNonNull(failure, "failure cannot be null");
            record = Objects.requireNonNull(record, "record cannot be null");
            reasonCode = requireCode(reasonCode, "reasonCode");
            requireResultConsistency(status, failure, record.isPresent());
            if (status == OperationStatus.SUCCEEDED && !operatingSystemLockReleased) {
                throw new IllegalArgumentException("successful release must release the OS lock");
            }
            requireSuccessfulRecordState(status, record, OwnershipState.RELEASED,
                    "successful release requires a released record");
        }
    }

    public record StaleOwnerFinding(
            StaleClassification classification,
            Optional<OwnershipRecord> previousRecord,
            boolean exclusiveLockAcquired,
            String reasonCode) {
        public StaleOwnerFinding {
            classification = Objects.requireNonNull(classification, "classification cannot be null");
            previousRecord = Objects.requireNonNull(previousRecord, "previousRecord cannot be null");
            reasonCode = requireCode(reasonCode, "reasonCode");
            if (classification == StaleClassification.LIVE_COMPETING_OWNER && exclusiveLockAcquired) {
                throw new IllegalArgumentException("a live competing owner cannot coexist with an acquired lock");
            }
            if (classification == StaleClassification.STALE_CANDIDATE && !exclusiveLockAcquired) {
                throw new IllegalArgumentException("stale candidacy requires exclusive lock acquisition");
            }
            boolean recordRequired = switch (classification) {
                case CLEANLY_RELEASED, STALE_CANDIDATE, ACTIVE_LOOKING_WITHOUT_LOCK,
                        DIRECTORY_IDENTITY_MISMATCH, LOCK_IDENTITY_MISMATCH,
                        GENERATION_INVALID, TIMESTAMP_INVALID, TAKEOVER_INCOMPLETE -> true;
                default -> false;
            };
            if ((classification == StaleClassification.NO_PREVIOUS_OWNER
                    && previousRecord.isPresent())
                    || (recordRequired && previousRecord.isEmpty())) {
                throw new IllegalArgumentException(
                        "stale-owner classification and previous-record evidence must agree");
            }
        }
    }

    public record TakeoverRequest(
            String expectedPreviousRecordFingerprint,
            String reasonCode) {
        public TakeoverRequest {
            expectedPreviousRecordFingerprint = requireSha(
                    expectedPreviousRecordFingerprint, "expectedPreviousRecordFingerprint");
            reasonCode = requireCode(reasonCode, "reasonCode");
        }
    }

    public record TakeoverResult(
            OperationStatus status,
            FailureClassification failure,
            Optional<OwnershipRecord> record,
            ReconciliationStatus reconciliationStatus,
            String reasonCode) {
        public TakeoverResult {
            status = Objects.requireNonNull(status, "status cannot be null");
            failure = Objects.requireNonNull(failure, "failure cannot be null");
            record = Objects.requireNonNull(record, "record cannot be null");
            reconciliationStatus = Objects.requireNonNull(
                    reconciliationStatus, "reconciliationStatus cannot be null");
            reasonCode = requireCode(reasonCode, "reasonCode");
            requireResultConsistency(status, failure, record.isPresent());
            if (status == OperationStatus.SUCCEEDED
                    && reconciliationStatus != ReconciliationStatus.SUCCEEDED) {
                throw new IllegalArgumentException(
                        "successful takeover requires successful reconciliation");
            }
            requireSuccessfulRecordState(status, record, OwnershipState.TAKEOVER_COMPLETE,
                    "successful takeover requires a takeover-complete record");
        }
    }

    public enum OwnershipState {
        ACQUIRING,
        OWNED,
        RENEWING,
        RELEASE_PENDING,
        RELEASED,
        COMPETING_OWNER,
        STALE_CANDIDATE,
        TAKEOVER_PENDING,
        TAKEOVER_COMPLETE,
        FAILED
    }

    public enum ReconciliationStatus {
        NOT_STARTED,
        IN_PROGRESS,
        SUCCEEDED,
        FAILED
    }

    public enum ReleaseStatus {
        NOT_REQUESTED,
        PENDING,
        RELEASED,
        FAILED
    }

    public enum OperationStatus {
        SUCCEEDED,
        REFUSED,
        FAILED
    }

    public enum StaleClassification {
        NO_PREVIOUS_OWNER,
        CLEANLY_RELEASED,
        LIVE_COMPETING_OWNER,
        STALE_CANDIDATE,
        ACTIVE_LOOKING_WITHOUT_LOCK,
        MALFORMED_RECORD,
        UNSUPPORTED_RECORD,
        FINGERPRINT_MISMATCH,
        DIRECTORY_IDENTITY_MISMATCH,
        LOCK_IDENTITY_MISMATCH,
        GENERATION_INVALID,
        TIMESTAMP_INVALID,
        TAKEOVER_INCOMPLETE
    }

    public enum FailureClassification {
        NONE,
        UNSAFE_PATH,
        STORAGE_UNAVAILABLE,
        PERMISSION_DENIED,
        LOCK_UNSUPPORTED,
        LIVE_COMPETING_OWNER,
        DUPLICATE_ACQUISITION,
        RECORD_MALFORMED,
        UNSUPPORTED_RECORD_VERSION,
        RECORD_FINGERPRINT_MISMATCH,
        DIRECTORY_IDENTITY_MISMATCH,
        LOCK_IDENTITY_MISMATCH,
        GENERATION_REGRESSION,
        GENERATION_EXHAUSTED,
        CLOCK_REGRESSION,
        RENEWAL_DEADLINE_EXCEEDED,
        LOCK_LOST,
        RECORD_REPLACED,
        RELEASE_FAILED,
        RECONCILIATION_FAILED,
        TAKEOVER_NOT_PERMITTED,
        IO_FAILURE
    }

    public static long nextGeneration(long previousGeneration) {
        requireGeneration(previousGeneration);
        if (previousGeneration == MAX_GENERATION) {
            throw new IllegalStateException("ownership generation is exhausted");
        }
        return previousGeneration + 1L;
    }

    static String requireId(String value, String field) {
        if (value == null || !CANONICAL_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be a bounded canonical identifier");
        }
        return value;
    }

    static String requireCode(String value, String field) {
        if (value == null || !CANONICAL_CODE.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be a bounded canonical code");
        }
        return value;
    }

    static String requireSha(String value, String field) {
        if (value == null || !SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
        return value;
    }

    private static String requirePriorFingerprint(String value) {
        if (GENESIS_FINGERPRINT.equals(value)) {
            return value;
        }
        return requireSha(value, "previousOwnerFingerprint");
    }

    private static long requireGeneration(long value) {
        if (value < INITIAL_GENERATION || value > MAX_GENERATION) {
            throw new IllegalArgumentException("ownership generation is outside hard bounds");
        }
        return value;
    }

    private static Duration requirePositiveDuration(Duration value, String field, Duration maximum) {
        Duration safe = Objects.requireNonNull(value, field + " cannot be null");
        if (safe.isZero() || safe.isNegative() || safe.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(field + " is outside bounded positive duration limits");
        }
        return safe;
    }

    private static Duration requireNonNegativeDuration(Duration value, String field, Duration maximum) {
        Duration safe = Objects.requireNonNull(value, field + " cannot be null");
        if (safe.isNegative() || safe.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(field + " is outside bounded duration limits");
        }
        return safe;
    }

    private static void requireResultConsistency(
            OperationStatus status,
            FailureClassification failure,
            boolean recordPresent) {
        if (status == OperationStatus.SUCCEEDED) {
            if (failure != FailureClassification.NONE || !recordPresent) {
                throw new IllegalArgumentException(
                        "successful ownership result requires a record and no failure");
            }
        } else if (failure == FailureClassification.NONE) {
            throw new IllegalArgumentException("unsuccessful ownership result requires a failure classification");
        }
    }

    private static void requireSuccessfulRecordState(
            OperationStatus status,
            Optional<OwnershipRecord> record,
            OwnershipState expectedState,
            String message) {
        if (status == OperationStatus.SUCCEEDED
                && record.orElseThrow().state() != expectedState) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireSuccessfulActiveRecordState(
            OperationStatus status,
            Optional<OwnershipRecord> record,
            String message) {
        if (status == OperationStatus.SUCCEEDED) {
            OwnershipState state = record.orElseThrow().state();
            if (state != OwnershipState.OWNED && state != OwnershipState.TAKEOVER_COMPLETE) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}
