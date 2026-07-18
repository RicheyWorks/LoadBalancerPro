package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.SyncPolicy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Draft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Reason;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalVerifier.Classification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalVerifier.Outcome;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalVerifier.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalStorageException.Failure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabExperimentJournalVerifierTest {
    private static final EnterpriseLabExperimentJournalCodec CODEC =
            new EnterpriseLabExperimentJournalCodec();
    private static final String CONFIGURATION_FINGERPRINT = "a".repeat(64);
    private static final String DECISION_FINGERPRINT = "b".repeat(64);
    private static final String BASELINE_FINGERPRINT = "c".repeat(64);
    private static final String CANDIDATE_FINGERPRINT = "d".repeat(64);
    private static final String APPLIED_FINGERPRINT = "e".repeat(64);

    @TempDir
    Path tempDirectory;

    @Test
    void verifiesACompleteCanonicalChainAndPreservesEverySourceByte() throws Exception {
        String experimentId = "verify-complete";
        EnterpriseLabExperimentJournalDirectory directory = directory(experimentId);
        List<EnterpriseLabExperimentJournalEvent> events = completedChain(experimentId);
        try (EnterpriseLabExperimentJournal journal = directory.openJournal(
                experimentId, SyncPolicy.WRITE_TO_OS)) {
            events.forEach(journal::append);
        }
        Path file = journalFile(tempDirectory);
        byte[] before = Files.readAllBytes(file);

        VerificationResult result = directory.verify(experimentId);

        assertEquals(Outcome.VALID, result.outcome());
        assertEquals(Classification.VALID, result.classification());
        assertTrue(result.chainValid());
        assertTrue(result.forensicSourcePreserved());
        assertEquals(events, result.verifiedEvents());
        assertEquals(events.size(), result.lastVerifiedSequence());
        assertEquals(events.get(events.size() - 1).currentEntryFingerprint(),
                result.lastVerifiedFingerprint());
        assertEquals(before.length, result.completeBytes());
        assertEquals(0, result.tailBytes());
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @Test
    void serializesVerificationThroughAnActiveWriterAndRejectsCompetingAccess() {
        String experimentId = "verify-active-writer";
        EnterpriseLabExperimentJournalDirectory directory = directory(experimentId);
        List<EnterpriseLabExperimentJournalEvent> events = completedChain(experimentId);
        try (EnterpriseLabExperimentJournal journal = directory.openJournal(experimentId)) {
            journal.append(events.get(0));
            assertEquals(Outcome.VALID, journal.verify().outcome());
            assertEquals(Outcome.UNAVAILABLE, directory.verify(experimentId).outcome());
            journal.append(events.get(1));
            assertEquals(2, journal.verify().lastVerifiedSequence());
        }
        assertEquals(Outcome.VALID, directory.verify(experimentId).outcome());
    }

    @Test
    void rejectsAnIllegalLifecycleAppendBeforeWritingAndRefusesInvalidReopen() throws Exception {
        String experimentId = "verify-writer-gate";
        EnterpriseLabExperimentJournalDirectory directory = directory(experimentId);
        EnterpriseLabExperimentJournalEvent illegal = event(
                experimentId, 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT,
                EnterpriseLabExperimentJournalEventType.LIFECYCLE_TRANSITION,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.RUNNING, 1);
        EnterpriseLabExperimentJournal writer = directory.openJournal(experimentId);

        EnterpriseLabExperimentJournalStorageException appendFailure = assertThrows(
                EnterpriseLabExperimentJournalStorageException.class,
                () -> writer.append(illegal));
        assertEquals(Failure.VERIFICATION_FAILED, appendFailure.failure());
        assertEquals(0, Files.size(journalFile(tempDirectory)));

        writeEvents(tempDirectory, List.of(illegal));
        assertEquals(Classification.ILLEGAL_TRANSITION,
                directory.verify(experimentId).classification());
        EnterpriseLabExperimentJournalStorageException reopenFailure = assertThrows(
                EnterpriseLabExperimentJournalStorageException.class,
                () -> directory.openJournal(experimentId));
        assertEquals(Failure.VERIFICATION_FAILED, reopenFailure.failure());
        assertTrue(reopenFailure.getMessage().contains("ILLEGAL_TRANSITION"));
    }

    @Test
    void reportsANotFoundJournalWithoutCreatingAFile() throws Exception {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabMutationTestAuthority.ownedDirectory(tempDirectory);

        VerificationResult result = directory.verify("verify-not-found");

        assertEquals(Outcome.NOT_FOUND, result.outcome());
        assertEquals(Classification.JOURNAL_NOT_FOUND, result.classification());
        assertFalse(result.exists());
        assertEquals(0, journalFileCount(tempDirectory));
    }

    @Test
    void acceptsOnlyAPlausiblePartialFinalFrameAsARecoverableTail() throws Exception {
        String experimentId = "verify-partial-tail";
        EnterpriseLabExperimentJournalDirectory directory = directory(experimentId);
        List<EnterpriseLabExperimentJournalEvent> events = completedChain(experimentId);
        byte[] second = CODEC.encode(events.get(1));
        byte[] partial = java.util.Arrays.copyOf(second, second.length / 2);
        writeFrames(tempDirectory, List.of(CODEC.encode(events.get(0))), partial);
        byte[] before = Files.readAllBytes(journalFile(tempDirectory));

        VerificationResult result = directory.verify(experimentId);

        assertEquals(Outcome.VALID_WITH_RECOVERABLE_TRUNCATED_TAIL, result.outcome());
        assertEquals(Classification.RECOVERABLE_TRUNCATED_TAIL, result.classification());
        assertEquals(List.of(events.get(0)), result.verifiedEvents());
        assertEquals(partial.length, result.tailBytes());
        assertArrayEquals(before, Files.readAllBytes(journalFile(tempDirectory)));
    }

    @Test
    void rejectsArbitraryTrailingBytesAndEmptyTrailingFrames() throws Exception {
        String experimentId = "verify-trailing";
        EnterpriseLabExperimentJournalDirectory directory = directory(experimentId);
        EnterpriseLabExperimentJournalEvent first = completedChain(experimentId).get(0);
        writeFrames(tempDirectory, List.of(CODEC.encode(first)), "garbage".getBytes(StandardCharsets.UTF_8));
        assertClassification(directory, experimentId, Classification.TRAILING_UNEXPECTED_DATA);

        writeFrames(tempDirectory, List.of(CODEC.encode(first), new byte[0]), new byte[0]);
        assertClassification(directory, experimentId, Classification.TRAILING_UNEXPECTED_DATA);
    }

    @Test
    void rejectsMalformedMiddleFramesWithoutSkippingToLaterEvidence() throws Exception {
        String experimentId = "verify-malformed-middle";
        EnterpriseLabExperimentJournalDirectory directory = directory(experimentId);
        List<EnterpriseLabExperimentJournalEvent> events = completedChain(experimentId);
        writeFrames(tempDirectory, List.of(
                CODEC.encode(events.get(0)),
                "{]".getBytes(StandardCharsets.UTF_8),
                CODEC.encode(events.get(2))), new byte[0]);
        byte[] before = Files.readAllBytes(journalFile(tempDirectory));

        VerificationResult result = directory.verify(experimentId);

        assertEquals(Classification.MALFORMED_ENTRY, result.classification());
        assertEquals(2, result.findings().get(0).frameNumber());
        assertTrue(result.verifiedEvents().isEmpty());
        assertArrayEquals(before, Files.readAllBytes(journalFile(tempDirectory)));
    }

    @Test
    void distinguishesFingerprintVersionAndCanonicalizationFailures() throws Exception {
        String experimentId = "verify-codec-failures";
        EnterpriseLabExperimentJournalDirectory directory = directory(experimentId);
        byte[] canonical = CODEC.encode(completedChain(experimentId).get(0));

        writeFrames(tempDirectory, List.of(replace(canonical, "event-1", "event-X")), new byte[0]);
        assertClassification(directory, experimentId, Classification.FINGERPRINT_MISMATCH);

        writeFrames(tempDirectory, List.of(replace(canonical, "/v1", "/v2")), new byte[0]);
        assertClassification(directory, experimentId, Classification.UNSUPPORTED_VERSION);

        byte[] nonCanonical = new byte[canonical.length + 1];
        nonCanonical[0] = '{';
        nonCanonical[1] = ' ';
        System.arraycopy(canonical, 1, nonCanonical, 2, canonical.length - 1);
        writeFrames(tempDirectory, List.of(nonCanonical), new byte[0]);
        assertClassification(directory, experimentId, Classification.NON_CANONICAL_ENTRY);
    }

    @Test
    void detectsDeletedReorderedDuplicatedAndInsertedSequenceEvidence() throws Exception {
        String experimentId = "verify-sequence";
        EnterpriseLabExperimentJournalDirectory directory = directory(experimentId);
        List<EnterpriseLabExperimentJournalEvent> events = completedChain(experimentId);

        writeEvents(tempDirectory, List.of(events.get(0), events.get(2)));
        assertClassification(directory, experimentId, Classification.MISSING_ENTRY);

        writeEvents(tempDirectory, List.of(events.get(0), events.get(2), events.get(1)));
        assertClassification(directory, experimentId, Classification.REORDERED_ENTRY);

        writeEvents(tempDirectory, List.of(events.get(0), events.get(1), events.get(1)));
        assertClassification(directory, experimentId, Classification.DUPLICATE_ENTRY);

        EnterpriseLabExperimentJournalEvent inserted = event(
                experimentId, 2, events.get(0).currentEntryFingerprint(),
                EnterpriseLabExperimentJournalEventType.CANDIDATE_ALLOCATION_APPLIED,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.ARMED, 2);
        writeEvents(tempDirectory, List.of(events.get(0), events.get(1), inserted));
        assertClassification(directory, experimentId, Classification.INVALID_SEQUENCE);
    }

    @Test
    void detectsPredecessorModificationAndCrossExperimentSubstitution() throws Exception {
        String experimentId = "verify-predecessor";
        EnterpriseLabExperimentJournalDirectory directory = directory(experimentId);
        List<EnterpriseLabExperimentJournalEvent> events = completedChain(experimentId);
        EnterpriseLabExperimentJournalEvent wrongPredecessor = event(
                experimentId, 2, "f".repeat(64),
                EnterpriseLabExperimentJournalEventType.EXPERIMENT_STARTED,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.RUNNING, 2);
        writeEvents(tempDirectory, List.of(events.get(0), wrongPredecessor));
        assertClassification(directory, experimentId, Classification.PREDECESSOR_MISMATCH);

        EnterpriseLabExperimentJournalEvent substituted = event(
                "another-experiment", 2, events.get(0).currentEntryFingerprint(),
                EnterpriseLabExperimentJournalEventType.EXPERIMENT_STARTED,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.RUNNING, 2);
        writeEvents(tempDirectory, List.of(events.get(0), substituted));
        assertClassification(directory, experimentId, Classification.IDENTITY_MISMATCH);
    }

    @Test
    void enforcesLifecycleContinuityAndTheExistingTransitionGraph() throws Exception {
        String experimentId = "verify-transition";
        EnterpriseLabExperimentJournalDirectory directory = directory(experimentId);
        EnterpriseLabExperimentJournalEvent illegalFirst = event(
                experimentId, 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT,
                EnterpriseLabExperimentJournalEventType.LIFECYCLE_TRANSITION,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.RUNNING, 1);
        writeEvents(tempDirectory, List.of(illegalFirst));
        assertClassification(directory, experimentId, Classification.ILLEGAL_TRANSITION);

        List<EnterpriseLabExperimentJournalEvent> events = completedChain(experimentId);
        EnterpriseLabExperimentJournalEvent discontinuous = event(
                experimentId, 2, events.get(0).currentEntryFingerprint(),
                EnterpriseLabExperimentJournalEventType.OBSERVATION_CHECKPOINT,
                EnterpriseLabExperimentState.HOLDING, EnterpriseLabExperimentState.HOLDING, 2);
        writeEvents(tempDirectory, List.of(events.get(0), discontinuous));
        assertClassification(directory, experimentId, Classification.ILLEGAL_TRANSITION);
    }

    @Test
    void preservesTerminalStateAndAllowsOnlyExplicitPostTerminalEvidence() throws Exception {
        String experimentId = "verify-terminal";
        EnterpriseLabExperimentJournalDirectory directory = directory(experimentId);
        EnterpriseLabExperimentJournalEvent rejected = event(
                experimentId, 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT,
                EnterpriseLabExperimentJournalEventType.EXPERIMENT_REJECTED,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.REJECTED, 1);
        EnterpriseLabExperimentJournalEvent recovery = event(
                experimentId, 2, rejected.currentEntryFingerprint(),
                EnterpriseLabExperimentJournalEventType.RECOVERY_ACTION,
                EnterpriseLabExperimentState.REJECTED, EnterpriseLabExperimentState.REJECTED, 2);
        writeEvents(tempDirectory, List.of(rejected, recovery));
        assertEquals(Outcome.VALID, directory.verify(experimentId).outcome());

        EnterpriseLabExperimentJournalEvent observation = event(
                experimentId, 2, rejected.currentEntryFingerprint(),
                EnterpriseLabExperimentJournalEventType.OBSERVATION_CHECKPOINT,
                EnterpriseLabExperimentState.REJECTED, EnterpriseLabExperimentState.REJECTED, 2);
        writeEvents(tempDirectory, List.of(rejected, observation));
        assertClassification(directory, experimentId, Classification.TERMINAL_STATE_VIOLATION);
    }

    @Test
    void detectsTimestampRegression() throws Exception {
        String experimentId = "verify-time";
        EnterpriseLabExperimentJournalDirectory directory = directory(experimentId);
        EnterpriseLabExperimentJournalEvent first = event(
                experimentId, 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT,
                EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.ARMED, 3);
        EnterpriseLabExperimentJournalEvent second = event(
                experimentId, 2, first.currentEntryFingerprint(),
                EnterpriseLabExperimentJournalEventType.EXPERIMENT_STARTED,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.RUNNING, 2);
        writeEvents(tempDirectory, List.of(first, second));
        assertClassification(directory, experimentId, Classification.TIMESTAMP_REGRESSION);
    }

    @Test
    void enforcesJournalEntryAndFrameBoundsBeforeUnboundedWork() throws Exception {
        String countExperiment = "verify-count-bound";
        EnterpriseLabExperimentJournalDirectory countDirectory =
                EnterpriseLabMutationTestAuthority.ownedDirectory(
                        tempDirectory, EnterpriseLabExperimentJournalDirectory.HARD_MAX_JOURNAL_BYTES, 2);
        try (EnterpriseLabExperimentJournal ignored = countDirectory.openJournal(countExperiment)) {
            // Create only the controlled backing file; raw corruption is supplied below.
        }
        List<EnterpriseLabExperimentJournalEvent> events = completedChain(countExperiment);
        writeEvents(tempDirectory, events.subList(0, 3));
        assertClassification(countDirectory, countExperiment, Classification.ENTRY_COUNT_EXCEEDED);

        Path sizeRoot = Files.createDirectory(tempDirectory.resolve("size"));
        String sizeExperiment = "verify-size-bound";
        EnterpriseLabExperimentJournalDirectory sizeDirectory =
                EnterpriseLabMutationTestAuthority.ownedDirectory(sizeRoot, 1_024, 10);
        try (EnterpriseLabExperimentJournal ignored = sizeDirectory.openJournal(sizeExperiment)) {
            // Create the controlled backing file.
        }
        Files.write(journalFile(sizeRoot), "x".repeat(1_025).getBytes(StandardCharsets.UTF_8));
        assertClassification(sizeDirectory, sizeExperiment, Classification.JOURNAL_SIZE_EXCEEDED);

        Path frameRoot = Files.createDirectory(tempDirectory.resolve("frame"));
        String frameExperiment = "verify-frame-bound";
        EnterpriseLabExperimentJournalDirectory frameDirectory =
                EnterpriseLabMutationTestAuthority.ownedDirectory(frameRoot);
        try (EnterpriseLabExperimentJournal ignored = frameDirectory.openJournal(frameExperiment)) {
            // Create the controlled backing file.
        }
        byte[] oversized = ("{" + "x".repeat(
                EnterpriseLabExperimentJournalCodec.HARD_MAX_ENTRY_BYTES) + "\n")
                .getBytes(StandardCharsets.UTF_8);
        Files.write(journalFile(frameRoot), oversized);
        assertClassification(frameDirectory, frameExperiment, Classification.ENTRY_SIZE_EXCEEDED);
    }

    private EnterpriseLabExperimentJournalDirectory directory(String experimentId) {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabMutationTestAuthority.ownedDirectory(tempDirectory);
        try (EnterpriseLabExperimentJournal ignored = directory.openJournal(experimentId)) {
            return directory;
        }
    }

    private static List<EnterpriseLabExperimentJournalEvent> completedChain(String experimentId) {
        List<EnterpriseLabExperimentJournalEvent> events = new ArrayList<>();
        add(events, experimentId, EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.ARMED);
        add(events, experimentId, EnterpriseLabExperimentJournalEventType.EXPERIMENT_STARTED,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.RUNNING);
        add(events, experimentId, EnterpriseLabExperimentJournalEventType.LIFECYCLE_TRANSITION,
                EnterpriseLabExperimentState.RUNNING, EnterpriseLabExperimentState.HOLDING);
        add(events, experimentId, EnterpriseLabExperimentJournalEventType.LIFECYCLE_TRANSITION,
                EnterpriseLabExperimentState.HOLDING, EnterpriseLabExperimentState.COMPLETING);
        add(events, experimentId, EnterpriseLabExperimentJournalEventType.EXPERIMENT_COMPLETED,
                EnterpriseLabExperimentState.COMPLETING, EnterpriseLabExperimentState.COMPLETED);
        return List.copyOf(events);
    }

    private static void add(
            List<EnterpriseLabExperimentJournalEvent> events,
            String experimentId,
            EnterpriseLabExperimentJournalEventType eventType,
            EnterpriseLabExperimentState before,
            EnterpriseLabExperimentState after) {
        long sequence = events.size() + 1L;
        String previous = events.isEmpty()
                ? EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT
                : events.get(events.size() - 1).currentEntryFingerprint();
        events.add(event(experimentId, sequence, previous, eventType, before, after, sequence));
    }

    private static EnterpriseLabExperimentJournalEvent event(
            String experimentId,
            long sequence,
            String previousFingerprint,
            EnterpriseLabExperimentJournalEventType eventType,
            EnterpriseLabExperimentState before,
            EnterpriseLabExperimentState after,
            long second) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("event", sequence);
        return EnterpriseLabExperimentJournalEvent.create(
                Clock.fixed(Instant.parse("2026-07-17T00:00:00Z").plusSeconds(second), ZoneOffset.UTC),
                new Draft(
                        sequence,
                        experimentId,
                        "stable-steady-state",
                        eventType,
                        before,
                        after,
                        sequence,
                        CONFIGURATION_FINGERPRINT,
                        DECISION_FINGERPRINT,
                        BASELINE_FINGERPRINT,
                        CANDIDATE_FINGERPRINT,
                        APPLIED_FINGERPRINT,
                        new Reason("EVENT_" + sequence, "event-" + sequence),
                        previousFingerprint,
                        Map.of("source", "verifier-test"),
                        payload));
    }

    private static void assertClassification(
            EnterpriseLabExperimentJournalDirectory directory,
            String experimentId,
            Classification classification) {
        VerificationResult result = directory.verify(experimentId);
        assertEquals(Outcome.INVALID, result.outcome());
        assertEquals(classification, result.classification());
        assertEquals(classification, result.findings().get(0).classification());
        assertTrue(result.forensicSourcePreserved());
        assertTrue(result.verifiedEvents().isEmpty());
    }

    private static byte[] replace(byte[] source, String before, String after) {
        String text = new String(source, StandardCharsets.UTF_8);
        String replaced = text.replace(before, after);
        assertFalse(text.equals(replaced));
        return replaced.getBytes(StandardCharsets.UTF_8);
    }

    private static void writeEvents(
            Path root,
            List<EnterpriseLabExperimentJournalEvent> events) throws IOException {
        writeFrames(root, events.stream().map(CODEC::encode).toList(), new byte[0]);
    }

    private static void writeFrames(Path root, List<byte[]> frames, byte[] tail) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (byte[] frame : frames) {
            bytes.write(frame);
            bytes.write('\n');
        }
        bytes.write(tail);
        Files.write(journalFile(root), bytes.toByteArray());
    }

    private static Path journalFile(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private static long journalFileCount(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .count();
        }
    }
}
