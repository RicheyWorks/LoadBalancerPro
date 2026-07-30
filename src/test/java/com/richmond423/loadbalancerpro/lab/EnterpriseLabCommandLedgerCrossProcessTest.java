package com.richmond423.loadbalancerpro.lab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabCommandLedgerCrossProcessTest {
    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(20);
    private static final long CONTINUOUS_WRITE_MILLIS = 1_500L;

    @TempDir
    Path root;

    @Test
    void readerNeverClassifiesAFramePausedInsideAnotherJvmWriteAsTruncated()
            throws Exception {
        Child child = startChild("supervisor-paused", 0L);
        try {
            awaitReady(child);
            Path ledgerFile = root
                    .resolve(EnterpriseLabSupervisorOwnership.DIRECTORY_NAME)
                    .resolve(EnterpriseLabSupervisorCommandLedger.DIRECTORY_NAME)
                    .resolve(EnterpriseLabSupervisorCommandLedger.FILE_NAME);
            try (FileChannel channel = FileChannel.open(
                    ledgerFile, StandardOpenOption.READ);
                 FileLock unexpected = channel.tryLock(
                         0L, Long.MAX_VALUE, true)) {
                assertNull(unexpected,
                        "writer did not hold its exclusive frame lock");
            }

            publishRelease(child.release());
            EnterpriseLabSupervisorCommandLedger.ReadResult replay =
                    EnterpriseLabSupervisorCommandLedger.inspect(root).replay();

            assertEquals(1, replay.events().size());
            awaitSuccessfulExit(child);
        } finally {
            stop(child);
        }
    }

    @Test
    void supervisorWriterAndReaderJvmsRunConcurrentlyWithoutFalseFailures()
            throws Exception {
        assertContinuousReplayHasNoTransientFailure(
                "supervisor-continuous",
                () -> EnterpriseLabSupervisorCommandLedger.inspect(root).replay().events().size());
    }

    @Test
    void applicationWriterAndReaderJvmsRunConcurrentlyWithoutFalseFailures()
            throws Exception {
        assertContinuousReplayHasNoTransientFailure(
                "application-continuous",
                () -> EnterpriseLabApplicationCommandLedger.inspect(root).replay().events().size());
    }

    private void assertContinuousReplayHasNoTransientFailure(
            String mode, Replay replay) throws Exception {
        Child child = startChild(mode, CONTINUOUS_WRITE_MILLIS);
        List<String> failures = new ArrayList<>();
        int attempts = 0;
        int largestReplay = 0;
        try {
            awaitReady(child);
            while (child.process().isAlive()) {
                attempts++;
                try {
                    largestReplay = Math.max(largestReplay, replay.eventCount());
                } catch (EnterpriseLabApplicationCommandLedger.StoreException exception) {
                    failures.add("application:" + exception.failure());
                } catch (EnterpriseLabSupervisorCommandLedger.StoreException exception) {
                    failures.add("supervisor:" + exception.failure());
                }
            }
            awaitSuccessfulExit(child);
            largestReplay = Math.max(largestReplay, replay.eventCount());
        } finally {
            stop(child);
        }
        assertTrue(attempts > 0, "reader did not overlap the writer process");
        assertTrue(largestReplay > 0, "reader did not reconstruct any durable event");
        assertEquals(List.of(), failures,
                "healthy cross-process append/replay produced false ledger failures");
    }

    private Child startChild(String mode, long durationMillis) throws IOException {
        Path ready = root.resolve(mode + ".ready");
        Path release = root.resolve(mode + ".release");
        Path output = root.resolve(mode + ".log");
        List<String> command = new ArrayList<>();
        command.add(javaExecutable().toString());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(EnterpriseLabCommandLedgerProcessProbe.class.getName());
        command.add(mode);
        command.add(root.toAbsolutePath().normalize().toString());
        command.add(ready.toAbsolutePath().normalize().toString());
        command.add(release.toAbsolutePath().normalize().toString());
        command.add(Long.toString(durationMillis));
        Process process = new ProcessBuilder(command)
                .directory(Path.of("").toAbsolutePath().normalize().toFile())
                .redirectErrorStream(true)
                .redirectOutput(output.toFile())
                .start();
        return new Child(process, ready, release, output);
    }

    private void awaitReady(Child child) throws Exception {
        long deadline = System.nanoTime() + PROCESS_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (Files.isRegularFile(child.ready())) {
                return;
            }
            if (!child.process().isAlive()) {
                throw new AssertionError(
                        "writer process exited before readiness: " + output(child));
            }
            Thread.sleep(10L);
        }
        throw new AssertionError("writer process readiness timed out: " + output(child));
    }

    private void awaitSuccessfulExit(Child child) throws Exception {
        assertTrue(child.process().waitFor(
                        PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS),
                "writer process did not exit within bounds");
        assertEquals(0, child.process().exitValue(), output(child));
    }

    private static void publishRelease(Path release) throws IOException {
        Files.writeString(
                release,
                "release",
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private static void stop(Child child) throws Exception {
        if (!Files.exists(child.release())) {
            publishRelease(child.release());
        }
        if (!child.process().isAlive()) {
            return;
        }
        child.process().destroyForcibly();
        if (!child.process().waitFor(
                PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new AssertionError("writer process could not be stopped within bounds");
        }
    }

    private static String output(Child child) {
        try {
            if (!Files.exists(child.output())) {
                return "no child output";
            }
            String value = Files.readString(child.output(), StandardCharsets.UTF_8)
                    .replace('\r', ' ')
                    .replace('\n', ' ')
                    .trim();
            return value.length() <= 1_024 ? value : value.substring(0, 1_024);
        } catch (IOException exception) {
            return "child output unavailable: " + exception.getMessage();
        }
    }

    private static Path javaExecutable() {
        String executable = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }

    @FunctionalInterface
    private interface Replay {
        int eventCount();
    }

    private record Child(Process process, Path ready, Path release, Path output) {
    }
}
