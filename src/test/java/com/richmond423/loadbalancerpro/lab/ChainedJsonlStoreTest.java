package com.richmond423.loadbalancerpro.lab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChainedJsonlStoreTest {
    @TempDir
    Path root;

    @Test
    void exactFrameAppendAndBoundedReplayShareOneEngine() throws Exception {
        Path file = Files.createFile(root.resolve("chain.jsonl"));
        ChainedJsonlStore store = new ChainedJsonlStore(file, 1_024L);

        store.appendFrame(
                "{\"sequence\":1}".getBytes(StandardCharsets.UTF_8),
                0L,
                ChainedJsonlStore.ForceMode.DATA_AND_METADATA,
                () -> { },
                bytes -> { },
                bytes -> { },
                bytes -> { });

        assertArrayEquals(
                "{\"sequence\":1}\n".getBytes(StandardCharsets.UTF_8),
                store.readBoundedBytes());
    }

    @Test
    void pinnedFileKeyAndCreationTimeRejectPathReplacement() throws Exception {
        Path file = Files.createFile(root.resolve("identity.jsonl"));
        Files.writeString(file, "{\"sequence\":1}\n", StandardCharsets.UTF_8);
        ChainedJsonlStore store = new ChainedJsonlStore(file, 1_024L);
        store.readBoundedBytes();
        ChainedJsonlStore.FileIdentity original =
                ChainedJsonlStore.identityOfControlledRegularFile(file);

        Files.move(file, root.resolve("identity.replaced"));
        Files.writeString(file, "{\"sequence\":2}\n", StandardCharsets.UTF_8);
        ChainedJsonlStore.FileIdentity replacement =
                ChainedJsonlStore.identityOfControlledRegularFile(file);
        if (original.equals(replacement)) {
            BasicFileAttributeView attributes = Files.getFileAttributeView(
                    file,
                    BasicFileAttributeView.class);
            FileTime distinctCreationTime = FileTime.from(
                    original.creationTime().toInstant().plus(1L, ChronoUnit.SECONDS));
            attributes.setTimes(null, null, distinctCreationTime);
            replacement = ChainedJsonlStore.identityOfControlledRegularFile(file);
        }
        assertNotEquals(
                original,
                replacement);

        ChainedJsonlStore.StoreIOException exception = assertThrows(
                ChainedJsonlStore.StoreIOException.class,
                store::readBoundedBytes);
        assertEquals(
                ChainedJsonlStore.Failure.FILE_IDENTITY_CHANGED,
                exception.failure());
    }

    @Test
    void sameJvmReaderWaitsUntilExactFrameAndForceFinish() throws Exception {
        Path file = Files.createFile(root.resolve("locked.jsonl"));
        ChainedJsonlStore writer = new ChainedJsonlStore(file, 1_024L);
        ChainedJsonlStore reader = new ChainedJsonlStore(file, 1_024L);
        CountDownLatch frameWritten = new CountDownLatch(1);
        CountDownLatch releaseForce = new CountDownLatch(1);
        CountDownLatch readerStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> append = executor.submit(() -> {
                writer.appendFrame(
                        "{\"sequence\":1}".getBytes(StandardCharsets.UTF_8),
                        0L,
                        ChainedJsonlStore.ForceMode.DATA_AND_METADATA,
                        () -> { },
                        bytes -> { },
                        bytes -> {
                            frameWritten.countDown();
                            await(releaseForce);
                        },
                        bytes -> { });
                return null;
            });
            assertTrue(frameWritten.await(2, TimeUnit.SECONDS));

            Future<byte[]> replay = executor.submit(() -> {
                readerStarted.countDown();
                return reader.readBoundedBytes();
            });
            assertTrue(readerStarted.await(2, TimeUnit.SECONDS));
            assertFalse(replay.isDone());
            releaseForce.countDown();

            append.get(2, TimeUnit.SECONDS);
            assertArrayEquals(
                    "{\"sequence\":1}\n".getBytes(StandardCharsets.UTF_8),
                    replay.get(2, TimeUnit.SECONDS));
        } finally {
            releaseForce.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void archiveAndTruncateKeepsOneCanonicalChainAcrossSegments()
            throws Exception {
        Path file = Files.createFile(root.resolve("rotating.jsonl"));
        ChainedJsonlStore store = new ChainedJsonlStore(file, 1_024L);
        store.prepareRotationDirectory();
        append(store, "1", 0L);

        ChainedJsonlStore.RotationReceipt rotation =
                store.rotateCurrentSegment(() -> { });
        assertTrue(rotation.rotated());
        assertEquals(1, rotation.archiveIndex());
        assertEquals(2L, rotation.archivedBytes());
        assertEquals(0L, Files.size(file));

        append(store, "2", 0L);
        ChainedJsonlStore.SegmentedChainReplay<String> replay =
                replay(store);

        assertEquals(List.of("1", "2"), replay.entries());
        assertEquals(1, replay.archiveCount());
        assertEquals(1, replay.currentEntries());
        assertEquals(2L, replay.currentBytes());
        assertEquals(4L, replay.totalBytes());
    }

    @Test
    void startupRecoveryRemovesInstallingFileAndCompletesPendingTruncate()
            throws Exception {
        Path file = Files.createFile(root.resolve("recovering.jsonl"));
        ChainedJsonlStore store = new ChainedJsonlStore(file, 1_024L);
        store.prepareRotationDirectory();
        append(store, "1", 0L);
        store.rotateCurrentSegment(() -> { });
        Path archive = store.segmentsDirectory()
                .resolve("segment-v1-00000001.jsonl");
        Files.write(file, Files.readAllBytes(archive));
        Path installing = store.segmentsDirectory()
                .resolve("segment-v1-00000002.jsonl.installing");
        Files.writeString(installing, "orphan", StandardCharsets.UTF_8);
        Path repairInstalling = store.repairQuarantineDirectory()
                .resolve("repair-v1-" + "a".repeat(64) + ".jsonl.installing");
        Files.createDirectory(store.repairQuarantineDirectory());
        Files.writeString(
                repairInstalling, "orphan", StandardCharsets.UTF_8);

        ChainedJsonlStore recovered =
                new ChainedJsonlStore(file, 1_024L);
        ChainedJsonlStore.RotationRecovery result =
                recovered.recoverRotationArtifacts(() -> { });

        assertEquals(1, result.removedInstallingFiles());
        assertTrue(result.completedPendingTruncate());
        assertFalse(Files.exists(installing));
        assertFalse(Files.exists(repairInstalling));
        assertEquals(0L, Files.size(file));
        assertEquals(List.of("1"), replay(recovered).entries());
    }

    @Test
    void segmentedReplayRejectsNonContiguousArchives() throws Exception {
        Path file = Files.createFile(root.resolve("non-contiguous.jsonl"));
        ChainedJsonlStore store = new ChainedJsonlStore(file, 1_024L);
        store.prepareRotationDirectory();
        Files.writeString(
                store.segmentsDirectory()
                        .resolve("segment-v1-00000002.jsonl"),
                "1\n",
                StandardCharsets.UTF_8);

        ChainedJsonlStore.StoreIOException exception = assertThrows(
                ChainedJsonlStore.StoreIOException.class,
                () -> replay(store));

        assertEquals(
                ChainedJsonlStore.Failure.CONCURRENT_CHANGE,
                exception.failure());
    }

    @Test
    void repairQuarantineRejectsFingerprintMismatch() throws Exception {
        Path file = Files.createFile(root.resolve("repair-integrity.jsonl"));
        ChainedJsonlStore store = new ChainedJsonlStore(file, 1_024L);
        Files.createDirectory(store.repairQuarantineDirectory());
        Files.writeString(
                store.repairQuarantineDirectory().resolve(
                        "repair-v1-" + "0".repeat(64) + ".jsonl"),
                "retained-source-bytes",
                StandardCharsets.UTF_8);

        ChainedJsonlStore.StoreIOException exception = assertThrows(
                ChainedJsonlStore.StoreIOException.class,
                store::validateRepairQuarantine);

        assertEquals(
                ChainedJsonlStore.Failure.CONCURRENT_CHANGE,
                exception.failure());
    }

    private static void append(
            ChainedJsonlStore store,
            String value,
            long expectedSize) throws Exception {
        store.appendFrame(
                value.getBytes(StandardCharsets.UTF_8),
                expectedSize,
                ChainedJsonlStore.ForceMode.DATA_AND_METADATA,
                () -> { },
                bytes -> { },
                bytes -> { },
                bytes -> { });
    }

    private static ChainedJsonlStore.SegmentedChainReplay<String> replay(
            ChainedJsonlStore store) throws Exception {
        return store.replaySegmentedChain(
                new ChainedJsonlStore.FrameCodec<>() {
                    @Override
                    public String decode(byte[] encoded) {
                        return new String(encoded, StandardCharsets.UTF_8);
                    }

                    @Override
                    public byte[] encode(String value) {
                        return value.getBytes(StandardCharsets.UTF_8);
                    }
                },
                (prior, next) -> assertEquals(
                        Integer.toString(prior.size() + 1),
                        next),
                1,
                16,
                ChainedJsonlStore.TailPolicy.REJECT);
    }

    private static void await(CountDownLatch latch) throws IOException {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new IOException("test latch timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("test latch was interrupted", exception);
        }
    }
}
