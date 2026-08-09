package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.richmond423.loadbalancerpro.api.LaseShadowRuntime;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * A bounded local saturation guard for request-thread access-log overhead. It
 * uses a literal-loopback upstream and no-op async sink, so it is regression
 * evidence, not a production throughput or latency benchmark.
 */
class ReverseProxyAccessLogBenchmark {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int SAMPLE_COUNT = 9;
    private static final int WARMUP_REQUESTS_PER_THREAD = 600;
    /*
     * Keep each measured saturation window long enough that a scheduler quantum
     * is small relative to the observation. Shorter 300-request windows were
     * about 120 ms on the Linux runner and produced materially different medians
     * for identical trees. This strengthens the workload without changing the
     * sample count, zero-drop assertion, or evidence boundary.
     */
    private static final int REQUESTS_PER_THREAD = 900;

    @TempDir
    Path temporaryDirectory;

    @Test
    void measureBoundedConcurrentSaturationOverhead() throws Exception {
        int threads = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));
        CountingWriter writer = new CountingWriter();
        ExecutorService upstreamExecutor = Executors.newFixedThreadPool(threads);
        HttpServer upstream = loopbackUpstream(upstreamExecutor);
        Fixture disabled = fixture(false, writer, upstream.getAddress().getPort());
        Fixture enabled = fixture(true, writer, upstream.getAddress().getPort());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (int warmup = 0; warmup < 3; warmup++) {
                runConcurrent(executor, disabled.service, threads, WARMUP_REQUESTS_PER_THREAD);
                runConcurrent(executor, enabled.service, threads, WARMUP_REQUESTS_PER_THREAD);
                runConcurrent(executor, enabled.service, threads, WARMUP_REQUESTS_PER_THREAD);
                runConcurrent(executor, disabled.service, threads, WARMUP_REQUESTS_PER_THREAD);
            }
            awaitWriter(enabled.accessLog, writer);

            List<Long> disabledSamples = new ArrayList<>();
            List<Long> enabledSamples = new ArrayList<>();
            List<Long> disabledFirstSamples = new ArrayList<>();
            List<Long> disabledSecondSamples = new ArrayList<>();
            List<Long> enabledFirstSamples = new ArrayList<>();
            List<Long> enabledSecondSamples = new ArrayList<>();
            List<Double> overheadSamples = new ArrayList<>();
            for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
                long disabledFirst;
                long disabledSecond;
                long enabledFirst;
                long enabledSecond;
                if ((sample & 1) == 0) {
                    disabledFirst = runConcurrent(
                            executor, disabled.service, threads, REQUESTS_PER_THREAD);
                    enabledFirst = runConcurrent(
                            executor, enabled.service, threads, REQUESTS_PER_THREAD);
                    enabledSecond = runConcurrent(
                            executor, enabled.service, threads, REQUESTS_PER_THREAD);
                    disabledSecond = runConcurrent(
                            executor, disabled.service, threads, REQUESTS_PER_THREAD);
                } else {
                    enabledFirst = runConcurrent(
                            executor, enabled.service, threads, REQUESTS_PER_THREAD);
                    disabledFirst = runConcurrent(
                            executor, disabled.service, threads, REQUESTS_PER_THREAD);
                    disabledSecond = runConcurrent(
                            executor, disabled.service, threads, REQUESTS_PER_THREAD);
                    enabledSecond = runConcurrent(
                            executor, enabled.service, threads, REQUESTS_PER_THREAD);
                }
                awaitWriter(enabled.accessLog, writer);
                disabledFirstSamples.add(disabledFirst);
                disabledSecondSamples.add(disabledSecond);
                enabledFirstSamples.add(enabledFirst);
                enabledSecondSamples.add(enabledSecond);
                long disabledAverage = average(disabledFirst, disabledSecond);
                long enabledAverage = average(enabledFirst, enabledSecond);
                disabledSamples.add(disabledAverage);
                enabledSamples.add(enabledAverage);
                overheadSamples.add(((double) enabledAverage / disabledAverage - 1.0) * 100.0);
            }

            double overheadPercent = medianDouble(overheadSamples);
            System.out.printf(Locale.ROOT,
                    "Access-log benchmark median overhead: %.3f%% (non-production evidence)%n", overheadPercent);
            writeRawResult(Map.ofEntries(
                    Map.entry("formatVersion", 1),
                    Map.entry("evidenceBoundary", "local-or-hosted-runner measurement; not production proof"),
                    Map.entry("availableProcessors", Runtime.getRuntime().availableProcessors()),
                    Map.entry("threads", threads),
                    Map.entry("warmupRequestsPerThread", WARMUP_REQUESTS_PER_THREAD),
                    Map.entry("requestsPerThread", REQUESTS_PER_THREAD),
                    Map.entry("disabledFirstNanos", disabledFirstSamples),
                    Map.entry("disabledSecondNanos", disabledSecondSamples),
                    Map.entry("enabledFirstNanos", enabledFirstSamples),
                    Map.entry("enabledSecondNanos", enabledSecondSamples),
                    Map.entry("disabledAverageNanos", disabledSamples),
                    Map.entry("enabledAverageNanos", enabledSamples),
                    Map.entry("pairedOverheadPercent", overheadSamples),
                    Map.entry("medianOverheadPercent", overheadPercent),
                    Map.entry("acceptedRecords", enabled.accessLog.acceptedCount()),
                    Map.entry("writtenRecords", writer.count.get()),
                    Map.entry("droppedRecords", enabled.accessLog.droppedCount())));
            assertEquals(0, enabled.accessLog.droppedCount());
        } finally {
            executor.shutdownNow();
            disabled.close();
            enabled.close();
            upstream.stop(0);
            upstreamExecutor.shutdownNow();
        }
    }

    private static void writeRawResult(Map<String, Object> result) throws Exception {
        String configuredPath = System.getProperty("loadbalancerpro.access-log-benchmark.output");
        if (configuredPath == null || configuredPath.isBlank()) {
            return;
        }
        Path output = Path.of(configuredPath).toAbsolutePath().normalize();
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), result);
    }

    private Fixture fixture(boolean accessLogEnabled, CountingWriter writer, int upstreamPort) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setRequestTimeout(Duration.ofSeconds(30));
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId("benchmark-backend");
        upstream.setUrl("http://127.0.0.1:" + upstreamPort);
        properties.setUpstreams(List.of(upstream));
        properties.getAccessLog().setEnabled(accessLogEnabled);
        properties.getAccessLog().setPath(temporaryDirectory.resolve("access.log").toString());
        ReverseProxyAccessLog accessLog = accessLogEnabled
                ? new ReverseProxyAccessLog(
                        properties.getAccessLog(), Clock.systemUTC(),
                        ReverseProxyAccessLog.DEFAULT_QUEUE_CAPACITY, ignored -> writer)
                : ReverseProxyAccessLog.disabled();
        accessLog.start();
        ReverseProxyService service = new ReverseProxyService(
                properties,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.systemUTC(),
                LaseShadowRuntime.disabled(),
                accessLog);
        return new Fixture(service, accessLog);
    }

    private static HttpServer loopbackUpstream(ExecutorService executor) throws Exception {
        HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/", exchange -> {
            try {
                exchange.sendResponseHeaders(204, -1);
            } finally {
                exchange.close();
            }
        });
        server.setExecutor(executor);
        server.start();
        return server;
    }

    private static long runConcurrent(
            ExecutorService executor,
            ReverseProxyService service,
            int threads,
            int requestsPerThread) throws Exception {
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Future<?>> futures = new ArrayList<>();
        for (int thread = 0; thread < threads; thread++) {
            futures.add(executor.submit(() -> {
                MockHttpServletRequest request = new MockHttpServletRequest("GET", "/proxy/benchmark");
                request.setRequestURI("/proxy/benchmark");
                ready.countDown();
                try {
                    start.await();
                    for (int index = 0; index < requestsPerThread; index++) {
                        if (service.forward(request, new byte[0]).statusCode() != 204) {
                            throw new IllegalStateException("unexpected proxy response");
                        }
                    }
                } finally {
                    done.countDown();
                }
                return null;
            }));
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        long startedAtNanos = System.nanoTime();
        start.countDown();
        assertTrue(done.await(90, TimeUnit.SECONDS));
        long elapsed = System.nanoTime() - startedAtNanos;
        for (Future<?> future : futures) {
            future.get(1, TimeUnit.SECONDS);
        }
        return elapsed;
    }

    private static void awaitWriter(ReverseProxyAccessLog accessLog, CountingWriter writer)
            throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (writer.count.get() < accessLog.acceptedCount() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertEquals(accessLog.acceptedCount(), writer.count.get());
    }

    private static double medianDouble(List<Double> samples) {
        List<Double> sorted = new ArrayList<>(samples);
        sorted.sort(Comparator.naturalOrder());
        return sorted.get(sorted.size() / 2);
    }

    private static long average(long first, long second) {
        return (first + second) / 2;
    }

    private record Fixture(ReverseProxyService service, ReverseProxyAccessLog accessLog) {
        private void close() {
            service.stop();
            accessLog.stop();
        }
    }

    private static final class CountingWriter implements ReverseProxyAccessLog.EventWriter {
        private final AtomicLong count = new AtomicLong();

        @Override
        public void append(String line) {
            count.incrementAndGet();
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
