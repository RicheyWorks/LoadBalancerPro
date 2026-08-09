package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReverseProxyAccessLogArchitectureTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyAccessLog.java");

    @TempDir
    Path temporaryDirectory;

    @Test
    void requestPublicationPathContainsNoBlockingLockOrIoPrimitive() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        String begin = slice(source,
                "RequestLogObservation begin(String method, long startedAtNanos)",
                "public synchronized void start()");
        String enqueue = slice(source,
                "private void enqueue(RequestLogObservation event)",
                "private int discardQueued()");
        String completion = slice(source,
                "void completeAt(long requestBytes, long completedAtNanos)",
                "private long timestampMillis()");
        String requestPath = begin + enqueue + completion;

        for (String forbidden : new String[] {
                "synchronized", ".lock(", ".await(", ".park", "Thread.sleep", ".join(",
                "Files.", "writer.append", "writer.flush"}) {
            assertFalse(requestPath.contains(forbidden),
                    "request publication path must not contain " + forbidden);
        }
        for (String unbounded : new String[] {
                "LinkedBlockingQueue", "ConcurrentLinkedQueue", "newCachedThreadPool", "newWorkStealingPool"}) {
            assertFalse(source.contains(unbounded), "access log must not use unbounded primitive " + unbounded);
        }
        assertTrue(source.contains("BoundedMpscQueue"));
        assertTrue(source.contains("AtomicReferenceArray"));
        assertTrue(source.contains("DEFAULT_QUEUE_CAPACITY = 16_384"));
    }

    @Test
    void writerIoRunsOnlyOnDaemonWriterAndStopLeavesNoWriterThread() throws Exception {
        CountDownLatch appended = new CountDownLatch(1);
        AtomicReference<Thread> appendThread = new AtomicReference<>();
        ReverseProxyProperties.AccessLog properties = new ReverseProxyProperties.AccessLog();
        properties.setEnabled(true);
        properties.setPath(temporaryDirectory.resolve("access.log").toString());
        ReverseProxyAccessLog accessLog = new ReverseProxyAccessLog(
                properties, Clock.systemUTC(), 8, ignored -> new ReverseProxyAccessLog.EventWriter() {
                    @Override
                    public void append(String line) {
                        appendThread.set(Thread.currentThread());
                        appended.countDown();
                    }

                    @Override
                    public void flush() {
                    }

                    @Override
                    public void close() {
                    }
                });
        Thread requestThread = Thread.currentThread();

        accessLog.start();
        ReverseProxyAccessLog.RequestLogObservation observation = accessLog.begin("GET");
        observation.terminal(204, ReverseProxyMetrics.TerminalOutcome.SUCCESS);
        observation.complete(0);
        assertTrue(appended.await(2, TimeUnit.SECONDS));
        accessLog.stop();

        Thread writerThread = appendThread.get();
        assertNotEquals(requestThread, writerThread);
        assertTrue(writerThread.isDaemon());
        assertTrue(writerThread.getName().startsWith(ReverseProxyAccessLog.THREAD_NAME));
        assertFalse(writerThread.isAlive(), "stop must join the access-log writer thread");
    }

    private static String slice(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);
        assertTrue(start >= 0, "missing source marker: " + startMarker);
        assertTrue(end > start, "missing source marker: " + endMarker);
        return source.substring(start, end);
    }
}
