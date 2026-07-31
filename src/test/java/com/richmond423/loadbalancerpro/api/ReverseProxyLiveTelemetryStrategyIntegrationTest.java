package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "loadbalancerpro.auth.mode=none")
@AutoConfigureMockMvc
@DirtiesContext
class ReverseProxyLiveTelemetryStrategyIntegrationTest {
    private static final int CONCURRENT_REQUESTS = 120;
    private static final int COMPARISON_REQUESTS = 40;
    private static final LatencyUpstream SLOW = LatencyUpstream.start("a-slow", Duration.ofMillis(150));
    private static final LatencyUpstream FAST = LatencyUpstream.start("b-fast", Duration.ofMillis(5));

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.request-timeout", () -> "2s");
        addRoute(registry, "least", "WEIGHTED_LEAST_CONNECTIONS");
        addRoute(registry, "round", "ROUND_ROBIN");
        addRoute(registry, "tail", "TAIL_LATENCY_POWER_OF_TWO");
    }

    @AfterAll
    static void stopUpstreams() {
        SLOW.close();
        FAST.close();
    }

    @Test
    void liveSignalsReduceSlowShareAndTailLatencyAgainstRoundRobin() throws Exception {
        List<Observation> leastConnections = concurrentObservations("/proxy/least/load");
        long leastSlowCount = slowCount(leastConnections);
        assertTrue(leastSlowCount > 0, "the slow fixture must receive traffic before live load shifts selection");
        assertTrue(leastSlowCount < CONCURRENT_REQUESTS * 0.30,
                () -> "slow upstream share did not fall below 30%: "
                        + leastSlowCount + "/" + CONCURRENT_REQUESTS);

        List<Observation> roundRobin = sequentialObservations("/proxy/round/baseline", COMPARISON_REQUESTS);
        assertEquals(COMPARISON_REQUESTS / 2, slowCount(roundRobin),
                "round-robin baseline must exercise the slow fixture evenly");

        List<Observation> tailAware = sequentialObservations("/proxy/tail/comparison", COMPARISON_REQUESTS);
        long roundRobinP95 = percentileMillis(roundRobin, 0.95);
        long tailAwareP95 = percentileMillis(tailAware, 0.95);
        assertTrue(slowCount(tailAware) < COMPARISON_REQUESTS * 0.30,
                "tail-aware routing must materially reduce slow-upstream selections");
        assertTrue(tailAwareP95 < roundRobinP95 * 0.50,
                () -> "tail-aware p95 must be less than half the round-robin baseline: tail="
                        + tailAwareP95 + "ms, roundRobin=" + roundRobinP95 + "ms");
    }

    private List<Observation> concurrentObservations(String path) throws Exception {
        ExecutorService clients = Executors.newFixedThreadPool(24);
        List<Future<Observation>> requests = new ArrayList<>();
        try {
            for (int request = 0; request < CONCURRENT_REQUESTS; request++) {
                int requestNumber = request;
                requests.add(clients.submit(() -> observe(path + "?request=" + requestNumber)));
                Thread.sleep(4);
            }
            List<Observation> observations = new ArrayList<>();
            for (Future<Observation> request : requests) {
                observations.add(request.get(10, TimeUnit.SECONDS));
            }
            return observations;
        } finally {
            clients.shutdownNow();
        }
    }

    private List<Observation> sequentialObservations(String path, int count) throws Exception {
        List<Observation> observations = new ArrayList<>();
        for (int request = 0; request < count; request++) {
            observations.add(observe(path + "?request=" + request));
        }
        return observations;
    }

    private Observation observe(String path) throws Exception {
        long startedAtNanos = System.nanoTime();
        MvcResult result = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn();
        long latencyMillis = Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
        return new Observation(
                result.getResponse().getHeader("X-LoadBalancerPro-Upstream"),
                latencyMillis);
    }

    private static long slowCount(List<Observation> observations) {
        return observations.stream()
                .filter(observation -> SLOW.id().equals(observation.upstreamId()))
                .count();
    }

    private static long percentileMillis(List<Observation> observations, double percentile) {
        List<Long> sorted = observations.stream()
                .map(Observation::latencyMillis)
                .sorted(Comparator.naturalOrder())
                .toList();
        int index = Math.max(0, (int) Math.ceil(percentile * sorted.size()) - 1);
        return sorted.get(index);
    }

    private static void addRoute(DynamicPropertyRegistry registry, String route, String strategy) {
        String prefix = "loadbalancerpro.proxy.routes." + route;
        registry.add(prefix + ".path-prefix", () -> "/" + route);
        registry.add(prefix + ".strategy", () -> strategy);
        registry.add(prefix + ".targets[0].id", SLOW::id);
        registry.add(prefix + ".targets[0].url", SLOW::baseUrl);
        registry.add(prefix + ".targets[0].healthy", () -> "true");
        registry.add(prefix + ".targets[1].id", FAST::id);
        registry.add(prefix + ".targets[1].url", FAST::baseUrl);
        registry.add(prefix + ".targets[1].healthy", () -> "true");
    }

    private record Observation(String upstreamId, long latencyMillis) {
    }

    private static final class LatencyUpstream implements AutoCloseable {
        private final String id;
        private final Duration latency;
        private final HttpServer server;
        private final ExecutorService executor;

        private LatencyUpstream(String id, Duration latency, HttpServer server, ExecutorService executor) {
            this.id = id;
            this.latency = latency;
            this.server = server;
            this.executor = executor;
        }

        private static LatencyUpstream start(String id, Duration latency) {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newCachedThreadPool();
                LatencyUpstream upstream = new LatencyUpstream(id, latency, server, executor);
                server.createContext("/", upstream::handle);
                server.setExecutor(executor);
                server.start();
                return upstream;
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        private String id() {
            return id;
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private void handle(HttpExchange exchange) throws IOException {
            try {
                Thread.sleep(latency.toMillis());
                byte[] body = id.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("latency fixture interrupted", exception);
            } finally {
                exchange.close();
            }
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
