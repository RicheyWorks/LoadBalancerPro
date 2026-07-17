package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalCodec.CodecException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Bounded, read-only verifier for canonical local experiment journals.
 * The controlled directory supplies the backing path without exposing it to callers.
 */
public final class EnterpriseLabExperimentJournalVerifier {
    private static final int READ_BUFFER_BYTES = 8_192;
    private static final int MAX_CONSECUTIVE_ZERO_READS = 3;

    private final EnterpriseLabExperimentJournalCodec codec;
    private final long maxJournalBytes;
    private final int maxJournalEntries;

    EnterpriseLabExperimentJournalVerifier(
            EnterpriseLabExperimentJournalCodec codec,
            long maxJournalBytes,
            int maxJournalEntries) {
        this.codec = Objects.requireNonNull(codec, "codec cannot be null");
        this.maxJournalBytes = maxJournalBytes;
        this.maxJournalEntries = maxJournalEntries;
    }

    VerificationResult verify(Path journalPath, String journalId, String experimentId) {
        Objects.requireNonNull(journalPath, "journalPath cannot be null");
        Objects.requireNonNull(journalId, "journalId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        if (!Files.exists(journalPath, LinkOption.NOFOLLOW_LINKS)) {
            return VerificationResult.notFound(journalId);
        }
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    journalPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile() || attributes.isSymbolicLink()) {
                return invalid(journalId, Classification.UNSAFE_STORAGE, 0, 0,
                        "UNSAFE_STORAGE", "journal source is not a controlled regular file",
                        0, 0, 0, 0);
            }
        } catch (IOException exception) {
            return invalid(journalId, Classification.IO_FAILURE, 0, 0,
                    "SOURCE_INSPECTION_FAILED", "journal source could not be inspected",
                    0, 0, 0, 0);
        }

        List<Frame> frames = new ArrayList<>();
        ByteArrayOutputStream current = new ByteArrayOutputStream();
        long declaredBytes;
        long observedBytes = 0;
        long acceptedCompleteBytes = 0;
        long frameStartOffset = 0;
        try (FileChannel channel = FileChannel.open(
                journalPath, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            declaredBytes = channel.size();
            if (declaredBytes > maxJournalBytes) {
                return invalid(journalId, Classification.JOURNAL_SIZE_EXCEEDED, 0, 0,
                        "JOURNAL_SIZE_EXCEEDED", "journal exceeds the configured byte limit",
                        0, 0, 0, declaredBytes);
            }
            ByteBuffer buffer = ByteBuffer.allocate(READ_BUFFER_BYTES);
            int zeroReads = 0;
            while (true) {
                int read = channel.read(buffer);
                if (read < 0) {
                    break;
                }
                if (read == 0) {
                    zeroReads++;
                    if (zeroReads >= MAX_CONSECUTIVE_ZERO_READS) {
                        return invalid(journalId, Classification.IO_FAILURE, frames.size() + 1L,
                                frameStartOffset, "READ_NO_PROGRESS",
                                "bounded journal verification read made no progress",
                                acceptedCompleteBytes, current.size(), observedBytes, declaredBytes);
                    }
                    continue;
                }
                zeroReads = 0;
                buffer.flip();
                while (buffer.hasRemaining()) {
                    byte value = buffer.get();
                    observedBytes++;
                    if (value == '\n') {
                        long frameNumber = frames.size() + 1L;
                        if (current.size() == 0) {
                            Classification classification = frames.isEmpty()
                                    ? Classification.INVALID_FRAMING
                                    : Classification.TRAILING_UNEXPECTED_DATA;
                            return invalid(journalId, classification, frameNumber, frameStartOffset,
                                    classification.name(), "journal contains an empty complete frame",
                                    acceptedCompleteBytes, 0, observedBytes, declaredBytes);
                        }
                        if (frames.size() >= maxJournalEntries) {
                            return invalid(journalId, Classification.ENTRY_COUNT_EXCEEDED,
                                    frameNumber, frameStartOffset, "ENTRY_COUNT_EXCEEDED",
                                    "journal exceeds the configured entry-count limit",
                                    acceptedCompleteBytes, 0, observedBytes, declaredBytes);
                        }
                        byte[] encoded = current.toByteArray();
                        EnterpriseLabExperimentJournalEvent event;
                        try {
                            event = decodeCanonical(encoded);
                        } catch (VerificationFailure failure) {
                            return invalid(journalId, failure.classification(), frameNumber,
                                    frameStartOffset, failure.code(), failure.getMessage(),
                                    acceptedCompleteBytes, 0, observedBytes, declaredBytes);
                        }
                        if (!experimentId.equals(event.experimentId())) {
                            return invalid(journalId, Classification.IDENTITY_MISMATCH,
                                    frameNumber, frameStartOffset, "EXPERIMENT_IDENTITY_MISMATCH",
                                    "journal frame belongs to a different experiment",
                                    acceptedCompleteBytes, 0, observedBytes, declaredBytes);
                        }
                        frames.add(new Frame(event, frameNumber, frameStartOffset));
                        current.reset();
                        acceptedCompleteBytes = observedBytes;
                        frameStartOffset = observedBytes;
                    } else {
                        if (current.size() >= EnterpriseLabExperimentJournalCodec.HARD_MAX_ENTRY_BYTES) {
                            return invalid(journalId, Classification.ENTRY_SIZE_EXCEEDED,
                                    frames.size() + 1L, frameStartOffset, "ENTRY_SIZE_EXCEEDED",
                                    "journal frame exceeds the canonical entry-size limit",
                                    acceptedCompleteBytes, current.size() + 1L, observedBytes, declaredBytes);
                        }
                        current.write(value);
                    }
                }
                buffer.clear();
            }
        } catch (IOException exception) {
            return invalid(journalId, Classification.IO_FAILURE, frames.size() + 1L,
                    frameStartOffset, "READ_FAILED", "journal could not be read for verification",
                    acceptedCompleteBytes, current.size(), observedBytes, observedBytes);
        }

        if (observedBytes != declaredBytes) {
            return invalid(journalId, Classification.IO_FAILURE, frames.size() + 1L,
                    frameStartOffset, "SOURCE_CHANGED", "journal changed during bounded verification",
                    acceptedCompleteBytes, current.size(), observedBytes, declaredBytes);
        }

        VerificationFailure chainFailure = verifyChain(frames);
        if (chainFailure != null) {
            Frame frame = frames.get((int) chainFailure.frameNumber() - 1);
            return invalid(journalId, chainFailure.classification(), chainFailure.frameNumber(),
                    frame.byteOffset(), chainFailure.code(), chainFailure.getMessage(),
                    acceptedCompleteBytes, current.size(), observedBytes, declaredBytes);
        }

        List<EnterpriseLabExperimentJournalEvent> events = frames.stream().map(Frame::event).toList();
        if (current.size() > 0) {
            if (current.toByteArray()[0] != '{') {
                return invalid(journalId, Classification.TRAILING_UNEXPECTED_DATA,
                        frames.size() + 1L, frameStartOffset, "TRAILING_UNEXPECTED_DATA",
                        "journal has trailing bytes that cannot begin a canonical frame",
                        acceptedCompleteBytes, current.size(), observedBytes, declaredBytes);
            }
            return VerificationResult.recoverableTail(
                    journalId, events, acceptedCompleteBytes, current.size(), observedBytes);
        }
        return VerificationResult.valid(journalId, events, acceptedCompleteBytes);
    }

    private EnterpriseLabExperimentJournalEvent decodeCanonical(byte[] encoded) {
        try {
            EnterpriseLabExperimentJournalEvent event = codec.decode(encoded);
            if (!Arrays.equals(encoded, codec.encode(event))) {
                throw new VerificationFailure(
                        Classification.NON_CANONICAL_ENTRY,
                        "NON_CANONICAL_ENTRY",
                        "journal frame is valid JSON but not canonical",
                        0);
            }
            return event;
        } catch (VerificationFailure failure) {
            throw failure;
        } catch (CodecException exception) {
            Classification classification = switch (exception.failure()) {
                case UNSUPPORTED_VERSION -> Classification.UNSUPPORTED_VERSION;
                case FINGERPRINT_MISMATCH -> Classification.FINGERPRINT_MISMATCH;
                case EXCEEDED_BOUNDS -> Classification.ENTRY_SIZE_EXCEEDED;
                case MALFORMED_ENTRY, UNKNOWN_FIELD, SENSITIVE_CONTENT -> Classification.MALFORMED_ENTRY;
            };
            throw new VerificationFailure(
                    classification,
                    classification.name(),
                    switch (classification) {
                        case UNSUPPORTED_VERSION -> "journal frame uses an unsupported schema version";
                        case FINGERPRINT_MISMATCH -> "journal frame fingerprint does not match canonical content";
                        case ENTRY_SIZE_EXCEEDED -> "journal frame content exceeds canonical bounds";
                        default -> "journal frame is malformed or violates the data-only schema";
                    },
                    0);
        } catch (RuntimeException exception) {
            throw new VerificationFailure(
                    Classification.MALFORMED_ENTRY,
                    "MALFORMED_ENTRY",
                    "journal frame could not be decoded safely",
                    0);
        }
    }

    private static VerificationFailure verifyChain(List<Frame> frames) {
        Set<Long> allSequences = new HashSet<>();
        Map<Long, Integer> sequenceCounts = new HashMap<>();
        for (Frame frame : frames) {
            allSequences.add(frame.event().sequence());
            sequenceCounts.merge(frame.event().sequence(), 1, Integer::sum);
        }

        Set<String> seenFingerprints = new HashSet<>();
        EnterpriseLabExperimentJournalEvent previousEvent = null;
        String expectedPrevious = EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT;
        for (int index = 0; index < frames.size(); index++) {
            Frame frame = frames.get(index);
            EnterpriseLabExperimentJournalEvent event = frame.event();
            long expectedSequence = index + 1L;
            if (event.sequence() != expectedSequence) {
                Classification classification;
                if (seenFingerprints.contains(event.currentEntryFingerprint())) {
                    classification = Classification.DUPLICATE_ENTRY;
                } else if (sequenceCounts.getOrDefault(event.sequence(), 0) > 1) {
                    classification = Classification.INVALID_SEQUENCE;
                } else if (event.sequence() > expectedSequence && !allSequences.contains(expectedSequence)) {
                    classification = Classification.MISSING_ENTRY;
                } else {
                    classification = Classification.REORDERED_ENTRY;
                }
                return failure(classification, frame.frameNumber(),
                        "journal entry sequence is not in one contiguous canonical order");
            }
            if (!expectedPrevious.equals(event.previousEntryFingerprint())) {
                return failure(Classification.PREDECESSOR_MISMATCH, frame.frameNumber(),
                        "journal entry predecessor fingerprint does not match the prior entry");
            }
            Classification transitionFailure = nextEventFailure(previousEvent, event);
            if (transitionFailure != null) {
                return failure(transitionFailure, frame.frameNumber(),
                        transitionFailure == Classification.TERMINAL_STATE_VIOLATION
                                ? "terminal journal state cannot transition or accept this event"
                                : "journal event is incompatible with the lifecycle transition graph");
            }
            seenFingerprints.add(event.currentEntryFingerprint());
            expectedPrevious = event.currentEntryFingerprint();
            previousEvent = event;
        }
        return null;
    }

    static Classification nextEventFailure(
            EnterpriseLabExperimentJournalEvent previous,
            EnterpriseLabExperimentJournalEvent event) {
        Objects.requireNonNull(event, "event cannot be null");
        if (previous != null) {
            if (event.stateBefore() != previous.stateAfter()) {
                return previous.stateAfter().terminal()
                        ? Classification.TERMINAL_STATE_VIOLATION
                        : Classification.ILLEGAL_TRANSITION;
            }
            if (event.occurredAt().isBefore(previous.occurredAt())) {
                return Classification.TIMESTAMP_REGRESSION;
            }
        }
        return transitionFailure(event);
    }

    private static Classification transitionFailure(EnterpriseLabExperimentJournalEvent event) {
        EnterpriseLabExperimentState before = event.stateBefore();
        EnterpriseLabExperimentState after = event.stateAfter();
        if (before.terminal()) {
            boolean permittedTerminalEvidence = before == after
                    && (event.eventType() == EnterpriseLabExperimentJournalEventType.RECOVERY_ACTION
                    || event.eventType() == EnterpriseLabExperimentJournalEventType.QUARANTINE_FINDING);
            return permittedTerminalEvidence ? null : Classification.TERMINAL_STATE_VIOLATION;
        }
        if (before == after) {
            return requiresStateChange(event.eventType()) ? Classification.ILLEGAL_TRANSITION : null;
        }
        if (!allowedStateChange(before, after) || !eventTypeMatchesStateChange(event.eventType(), after)) {
            return Classification.ILLEGAL_TRANSITION;
        }
        return null;
    }

    private static boolean requiresStateChange(EnterpriseLabExperimentJournalEventType eventType) {
        return switch (eventType) {
            case EXPERIMENT_ARMED, EXPERIMENT_STARTED, ROLLBACK_REQUESTED,
                    EXPERIMENT_CANCELLED, EXPERIMENT_COMPLETED, EXPERIMENT_ROLLED_BACK,
                    EXPERIMENT_REJECTED, EXPERIMENT_FAILED, LIFECYCLE_TRANSITION -> true;
            default -> false;
        };
    }

    private static boolean eventTypeMatchesStateChange(
            EnterpriseLabExperimentJournalEventType eventType,
            EnterpriseLabExperimentState after) {
        return switch (eventType) {
            case EXPERIMENT_ARMED -> after == EnterpriseLabExperimentState.ARMED;
            case EXPERIMENT_STARTED -> after == EnterpriseLabExperimentState.RUNNING;
            case ROLLBACK_REQUESTED -> after == EnterpriseLabExperimentState.ROLLING_BACK;
            case EXPERIMENT_CANCELLED -> after == EnterpriseLabExperimentState.CANCELLED;
            case EXPERIMENT_COMPLETED -> after == EnterpriseLabExperimentState.COMPLETED;
            case EXPERIMENT_ROLLED_BACK -> after == EnterpriseLabExperimentState.ROLLED_BACK;
            case EXPERIMENT_REJECTED -> after == EnterpriseLabExperimentState.REJECTED;
            case EXPERIMENT_FAILED -> after == EnterpriseLabExperimentState.FAILED;
            case LIFECYCLE_TRANSITION -> true;
            default -> false;
        };
    }

    private static boolean allowedStateChange(
            EnterpriseLabExperimentState before,
            EnterpriseLabExperimentState after) {
        return switch (before) {
            case IDLE -> after == EnterpriseLabExperimentState.ARMED
                    || after == EnterpriseLabExperimentState.REJECTED;
            case ARMED -> after == EnterpriseLabExperimentState.RUNNING
                    || after == EnterpriseLabExperimentState.CANCELLED
                    || after == EnterpriseLabExperimentState.FAILED;
            case RUNNING -> after == EnterpriseLabExperimentState.HOLDING
                    || after == EnterpriseLabExperimentState.ROLLING_BACK;
            case HOLDING -> after == EnterpriseLabExperimentState.COMPLETING
                    || after == EnterpriseLabExperimentState.ROLLING_BACK;
            case COMPLETING -> after == EnterpriseLabExperimentState.COMPLETED
                    || after == EnterpriseLabExperimentState.ROLLING_BACK;
            case ROLLING_BACK -> after == EnterpriseLabExperimentState.ROLLED_BACK;
            case ROLLED_BACK, COMPLETED, REJECTED, FAILED, CANCELLED -> false;
        };
    }

    private static VerificationFailure failure(
            Classification classification,
            long frameNumber,
            String message) {
        return new VerificationFailure(classification, classification.name(), message, frameNumber);
    }

    private static VerificationResult invalid(
            String journalId,
            Classification classification,
            long frameNumber,
            long byteOffset,
            String code,
            String message,
            long completeBytes,
            long tailBytes,
            long observedBytes,
            long totalBytes) {
        return VerificationResult.invalid(
                journalId,
                new Finding(classification, frameNumber, byteOffset, code, message),
                completeBytes,
                tailBytes,
                Math.max(observedBytes, totalBytes));
    }

    public enum Outcome {
        VALID,
        VALID_WITH_RECOVERABLE_TRUNCATED_TAIL,
        INVALID,
        NOT_FOUND,
        UNAVAILABLE
    }

    public enum Classification {
        VALID,
        RECOVERABLE_TRUNCATED_TAIL,
        JOURNAL_NOT_FOUND,
        INVALID_FRAMING,
        ENTRY_SIZE_EXCEEDED,
        JOURNAL_SIZE_EXCEEDED,
        ENTRY_COUNT_EXCEEDED,
        UNSUPPORTED_VERSION,
        MALFORMED_ENTRY,
        NON_CANONICAL_ENTRY,
        FINGERPRINT_MISMATCH,
        IDENTITY_MISMATCH,
        INVALID_SEQUENCE,
        MISSING_ENTRY,
        REORDERED_ENTRY,
        DUPLICATE_ENTRY,
        PREDECESSOR_MISMATCH,
        ILLEGAL_TRANSITION,
        TERMINAL_STATE_VIOLATION,
        TIMESTAMP_REGRESSION,
        TRAILING_UNEXPECTED_DATA,
        UNSAFE_STORAGE,
        IO_FAILURE,
        VERIFICATION_UNAVAILABLE
    }

    public record Finding(
            Classification classification,
            long frameNumber,
            long byteOffset,
            String code,
            String message) {
        public Finding {
            classification = Objects.requireNonNull(classification, "classification cannot be null");
            if (frameNumber < 0 || byteOffset < 0) {
                throw new IllegalArgumentException("finding positions cannot be negative");
            }
            code = requireBoundedText(code, "code", 64);
            message = requireBoundedText(message, "message", 256);
        }
    }

    public record VerificationResult(
            String journalId,
            boolean exists,
            Outcome outcome,
            Classification classification,
            List<Finding> findings,
            List<EnterpriseLabExperimentJournalEvent> verifiedEvents,
            long completeBytes,
            long tailBytes,
            long totalBytes,
            long lastVerifiedSequence,
            String lastVerifiedFingerprint,
            boolean forensicSourcePreserved) {
        public VerificationResult {
            journalId = requireBoundedText(journalId, "journalId", 96);
            outcome = Objects.requireNonNull(outcome, "outcome cannot be null");
            classification = Objects.requireNonNull(classification, "classification cannot be null");
            findings = List.copyOf(Objects.requireNonNull(findings, "findings cannot be null"));
            verifiedEvents = List.copyOf(Objects.requireNonNull(
                    verifiedEvents, "verifiedEvents cannot be null"));
            if (completeBytes < 0 || tailBytes < 0 || totalBytes < 0
                    || completeBytes + tailBytes > totalBytes || lastVerifiedSequence < 0) {
                throw new IllegalArgumentException("verification byte and sequence counts are inconsistent");
            }
            lastVerifiedFingerprint = requireBoundedText(
                    lastVerifiedFingerprint, "lastVerifiedFingerprint", 64);
            boolean chainValid = outcome == Outcome.VALID
                    || outcome == Outcome.VALID_WITH_RECOVERABLE_TRUNCATED_TAIL;
            if (chainValid != findings.isEmpty()
                    || (!chainValid && !verifiedEvents.isEmpty())
                    || lastVerifiedSequence != verifiedEvents.size()) {
                throw new IllegalArgumentException("verification outcome and evidence are inconsistent");
            }
        }

        public boolean chainValid() {
            return outcome == Outcome.VALID
                    || outcome == Outcome.VALID_WITH_RECOVERABLE_TRUNCATED_TAIL;
        }

        static VerificationResult valid(
                String journalId,
                List<EnterpriseLabExperimentJournalEvent> events,
                long totalBytes) {
            return success(journalId, Outcome.VALID, Classification.VALID, events,
                    totalBytes, 0, totalBytes);
        }

        static VerificationResult recoverableTail(
                String journalId,
                List<EnterpriseLabExperimentJournalEvent> events,
                long completeBytes,
                long tailBytes,
                long totalBytes) {
            return success(journalId, Outcome.VALID_WITH_RECOVERABLE_TRUNCATED_TAIL,
                    Classification.RECOVERABLE_TRUNCATED_TAIL, events,
                    completeBytes, tailBytes, totalBytes);
        }

        static VerificationResult notFound(String journalId) {
            return new VerificationResult(
                    journalId, false, Outcome.NOT_FOUND, Classification.JOURNAL_NOT_FOUND,
                    List.of(new Finding(Classification.JOURNAL_NOT_FOUND, 0, 0,
                            "JOURNAL_NOT_FOUND", "journal does not exist")),
                    List.of(), 0, 0, 0, 0,
                    EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT, false);
        }

        static VerificationResult unavailable(String journalId) {
            return new VerificationResult(
                    journalId, true, Outcome.UNAVAILABLE, Classification.VERIFICATION_UNAVAILABLE,
                    List.of(new Finding(Classification.VERIFICATION_UNAVAILABLE, 0, 0,
                            "VERIFICATION_UNAVAILABLE",
                            "journal is owned by another process-local operation")),
                    List.of(), 0, 0, 0, 0,
                    EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT, true);
        }

        static VerificationResult invalid(
                String journalId,
                Finding finding,
                long completeBytes,
                long tailBytes,
                long totalBytes) {
            return new VerificationResult(
                    journalId, true, Outcome.INVALID, finding.classification(), List.of(finding),
                    List.of(), completeBytes, tailBytes, totalBytes, 0,
                    EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT, true);
        }

        private static VerificationResult success(
                String journalId,
                Outcome outcome,
                Classification classification,
                List<EnterpriseLabExperimentJournalEvent> events,
                long completeBytes,
                long tailBytes,
                long totalBytes) {
            List<EnterpriseLabExperimentJournalEvent> safeEvents = List.copyOf(events);
            String lastFingerprint = safeEvents.isEmpty()
                    ? EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT
                    : safeEvents.get(safeEvents.size() - 1).currentEntryFingerprint();
            return new VerificationResult(
                    journalId, true, outcome, classification, List.of(), safeEvents,
                    completeBytes, tailBytes, totalBytes, safeEvents.size(), lastFingerprint, true);
        }
    }

    private record Frame(
            EnterpriseLabExperimentJournalEvent event,
            long frameNumber,
            long byteOffset) {
    }

    private static final class VerificationFailure extends RuntimeException {
        private final Classification classification;
        private final String code;
        private final long frameNumber;

        private VerificationFailure(
                Classification classification,
                String code,
                String message,
                long frameNumber) {
            super(message, null, false, false);
            this.classification = classification;
            this.code = code;
            this.frameNumber = frameNumber;
        }

        private Classification classification() {
            return classification;
        }

        private String code() {
            return code;
        }

        private long frameNumber() {
            return frameNumber;
        }
    }

    private static String requireBoundedText(String value, String fieldName, int maximumLength) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " must be non-blank, trimmed, and bounded");
        }
        return value;
    }
}
