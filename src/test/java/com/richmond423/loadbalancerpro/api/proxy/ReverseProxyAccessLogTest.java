package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReverseProxyAccessLogTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-02T12:34:56Z"), ZoneOffset.UTC);
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path temporaryDirectory;

    @Test
    void jsonRecordIsExactOnceBoundedAndPrivacySafe() throws Exception {
        MemoryWriter writer = new MemoryWriter();
        ReverseProxyAccessLog accessLog = accessLog("JSON", 1.0, 8, ignored -> writer);
        accessLog.start();

        ReverseProxyAccessLog.RequestLogObservation observation = accessLog.begin("GET");
        observation.bindRoute("api");
        observation.bindUpstream("api", "backend-a");
        observation.recordDispatch(false);
        observation.recordDispatch(true);
        observation.addResponseBytes(11);
        observation.cooldownActivated();
        observation.terminal(503, ReverseProxyMetrics.TerminalOutcome.LOAD_SHED);
        observation.complete(7);
        observation.complete(999);
        accessLog.stop();

        assertEquals(1, writer.lines.size());
        JsonNode event = JSON.readTree(writer.lines.get(0));
        assertEquals("proxy.access", event.get("event").asText());
        assertEquals("2026-08-02T12:34:56Z", event.get("timestamp").asText());
        assertEquals("-", event.get("client").asText());
        assertEquals("GET", event.get("method").asText());
        assertEquals("-", event.get("path").asText());
        assertEquals("api", event.get("route").asText());
        assertEquals("backend-a", event.get("upstream").asText());
        assertEquals(503, event.get("status").asInt());
        assertEquals(7, event.get("bytes_in").asLong());
        assertEquals(11, event.get("bytes_out").asLong());
        assertTrue(event.get("duration_micros").asLong() >= 0);
        assertEquals(1, event.get("retries").asInt());
        assertTrue(event.get("shed").asBoolean());
        assertTrue(event.get("cooldown").asBoolean());
        assertEquals("LOAD_SHED", event.get("outcome").asText());
        assertEquals(1, accessLog.acceptedCount());
        assertEquals(0, accessLog.droppedCount());
    }

    @Test
    void combinedRecordCollapsesUntrustedValuesAndCannotForgeLines() {
        MemoryWriter writer = new MemoryWriter();
        ReverseProxyAccessLog accessLog = accessLog("combined", 1.0, 8, ignored -> writer);
        accessLog.start();

        ReverseProxyAccessLog.RequestLogObservation observation =
                accessLog.begin("GET\r\nforged=true secret-api-key");
        observation.bindRoute("/private/path?token=secret");
        observation.bindUpstream("api", "https://10.0.0.1:8443/private");
        observation.terminal(999, null);
        observation.complete(-1);
        accessLog.stop();

        assertEquals(1, writer.lines.size());
        String line = writer.lines.get(0);
        assertFalse(line.contains("\r"));
        assertFalse(line.contains("\n"));
        assertFalse(line.contains("secret"));
        assertFalse(line.contains("10.0.0.1"));
        assertFalse(line.contains("private/path"));
        assertTrue(line.contains("client=- method=OTHER path=- route=api upstream=OTHER status=0"));
        assertTrue(line.endsWith("outcome=OTHER"));
    }

    @Test
    void deterministicSamplingAndZeroSamplingAvoidRequestRecords() {
        MemoryWriter firstWriter = new MemoryWriter();
        MemoryWriter secondWriter = new MemoryWriter();
        ReverseProxyAccessLog first = accessLog("JSON", 0.25, 1_024, ignored -> firstWriter);
        ReverseProxyAccessLog second = accessLog("JSON", 0.25, 1_024, ignored -> secondWriter);
        first.start();
        second.start();

        for (int index = 0; index < 1_000; index++) {
            completeIfSelected(first.begin("GET"));
            completeIfSelected(second.begin("GET"));
        }
        first.stop();
        second.stop();

        assertEquals(firstWriter.lines.size(), secondWriter.lines.size());
        assertTrue(firstWriter.lines.size() > 150 && firstWriter.lines.size() < 350);

        ReverseProxyAccessLog zero = accessLog("JSON", 0.0, 1, ignored -> new MemoryWriter());
        zero.start();
        assertNull(zero.begin("GET"));
        zero.stop();
        assertEquals(0, zero.acceptedCount());
    }

    @Test
    void boundedQueueDropsWithoutBlockingAndDrainsAcceptedRecords() throws Exception {
        BlockingWriter writer = new BlockingWriter();
        ReverseProxyAccessLog accessLog = accessLog("JSON", 1.0, 1, ignored -> writer);
        accessLog.start();

        completeIfSelected(accessLog.begin("GET"));
        assertTrue(writer.entered.await(2, TimeUnit.SECONDS));
        completeIfSelected(accessLog.begin("GET"));
        long started = System.nanoTime();
        completeIfSelected(accessLog.begin("GET"));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertTrue(elapsedMillis < 250, "queue saturation blocked the request thread");
        assertEquals(1, accessLog.droppedCount());
        writer.release.countDown();
        accessLog.stop();
        assertEquals(2, writer.lines.size());
    }

    @Test
    void writerFailureIsContainedAndDoesNotEscapeCompletion() {
        ReverseProxyAccessLog accessLog = accessLog("JSON", 1.0, 8, ignored -> new FailingWriter());
        accessLog.start();

        ReverseProxyAccessLog.RequestLogObservation observation = accessLog.begin("POST");
        assertDoesNotThrow(() -> {
            observation.terminal(200, ReverseProxyMetrics.TerminalOutcome.SUCCESS);
            observation.complete(3);
        });
        accessLog.stop();

        assertEquals(1, accessLog.acceptedCount());
        assertEquals(1, accessLog.writeFailureCount());
    }

    @Test
    void realFileWriterAppendsAndGracefullyDrains() throws Exception {
        Path path = temporaryDirectory.resolve("nested/proxy-access.log");
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.getAccessLog().setEnabled(true);
        properties.getAccessLog().setPath(path.toString());
        ReverseProxyAccessLog accessLog = new ReverseProxyAccessLog(properties);
        accessLog.start();
        ReverseProxyAccessLog.RequestLogObservation observation = accessLog.begin("GET");
        observation.terminal(204, ReverseProxyMetrics.TerminalOutcome.SUCCESS);
        observation.complete(0);
        accessLog.stop();

        List<String> lines = Files.readAllLines(path);
        assertEquals(1, lines.size());
        assertEquals("proxy.access", JSON.readTree(lines.get(0)).get("event").asText());
    }

    @Test
    void malformedConfigurationFailsClosedWithBoundedMessages() {
        ReverseProxyProperties.AccessLog invalidFormat = enabledProperties("XML", 1.0);
        assertThrows(IllegalStateException.class,
                () -> ReverseProxyAccessLog.validateConfiguration(invalidFormat));

        ReverseProxyProperties.AccessLog notANumber = enabledProperties("JSON", Double.NaN);
        assertThrows(IllegalStateException.class,
                () -> ReverseProxyAccessLog.validateConfiguration(notANumber));

        ReverseProxyProperties.AccessLog aboveOne = enabledProperties("JSON", 1.1);
        assertThrows(IllegalStateException.class,
                () -> ReverseProxyAccessLog.validateConfiguration(aboveOne));

        ReverseProxyProperties.AccessLog invalidPath = enabledProperties("JSON", 1.0);
        invalidPath.setPath("bad\nforged");
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ReverseProxyAccessLog.validateConfiguration(invalidPath));
        assertFalse(failure.getMessage().contains("forged"));
    }

    private ReverseProxyAccessLog accessLog(
            String format,
            double sampleRate,
            int capacity,
            ReverseProxyAccessLog.EventWriterFactory writerFactory) {
        return new ReverseProxyAccessLog(
                enabledProperties(format, sampleRate), FIXED_CLOCK, capacity, writerFactory);
    }

    private ReverseProxyProperties.AccessLog enabledProperties(String format, double sampleRate) {
        ReverseProxyProperties.AccessLog properties = new ReverseProxyProperties.AccessLog();
        properties.setEnabled(true);
        properties.setFormat(format);
        properties.setPath(temporaryDirectory.resolve("proxy-access.log").toString());
        properties.setSampleRate(sampleRate);
        return properties;
    }

    private static void completeIfSelected(ReverseProxyAccessLog.RequestLogObservation observation) {
        if (observation != null) {
            observation.terminal(200, ReverseProxyMetrics.TerminalOutcome.SUCCESS);
            observation.complete(0);
        }
    }

    private static class MemoryWriter implements ReverseProxyAccessLog.EventWriter {
        protected final List<String> lines = new CopyOnWriteArrayList<>();

        @Override
        public void append(String line) throws IOException {
            lines.add(line);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    private static final class BlockingWriter extends MemoryWriter {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public void append(String line) throws IOException {
            entered.countDown();
            try {
                if (!release.await(2, TimeUnit.SECONDS)) {
                    throw new IOException("test writer timed out");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("test writer interrupted");
            }
            lines.add(line);
        }
    }

    private static final class FailingWriter implements ReverseProxyAccessLog.EventWriter {
        @Override
        public void append(String line) throws IOException {
            throw new IOException("synthetic secret writer failure");
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
