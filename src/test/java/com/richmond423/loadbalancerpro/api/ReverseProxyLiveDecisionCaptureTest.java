package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

@SpringBootTest(properties = {
        "loadbalancerpro.auth.mode=none",
        "loadbalancerpro.lase.shadow.enabled=true"
})
@AutoConfigureMockMvc
@DirtiesContext
class ReverseProxyLiveDecisionCaptureTest {
    private static final LoopbackUpstream UPSTREAM = LoopbackUpstream.start();
    private static final LoopbackUpstream DEGRADED_UPSTREAM = LoopbackUpstream.start();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LaseShadowRuntime laseShadowRuntime;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.strategy", () -> "WEIGHTED_LEAST_LOAD");
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "decision-upstream");
        registry.add("loadbalancerpro.proxy.upstreams[0].url", UPSTREAM::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[0].healthy", () -> "true");
        registry.add("loadbalancerpro.proxy.upstreams[0].in-flight-request-count", () -> "4");
        registry.add("loadbalancerpro.proxy.upstreams[0].average-latency-millis", () -> "11");
        registry.add("loadbalancerpro.proxy.upstreams[0].p95-latency-millis", () -> "22");
        registry.add("loadbalancerpro.proxy.upstreams[0].p99-latency-millis", () -> "33");
        registry.add("loadbalancerpro.proxy.upstreams[0].recent-error-rate", () -> "0.25");
        registry.add("loadbalancerpro.proxy.upstreams[1].id", () -> "degraded-upstream");
        registry.add("loadbalancerpro.proxy.upstreams[1].url", DEGRADED_UPSTREAM::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[1].healthy", () -> "true");
        registry.add("loadbalancerpro.proxy.upstreams[1].in-flight-request-count", () -> "40");
        registry.add("loadbalancerpro.proxy.upstreams[1].average-latency-millis", () -> "80");
        registry.add("loadbalancerpro.proxy.upstreams[1].p95-latency-millis", () -> "160");
        registry.add("loadbalancerpro.proxy.upstreams[1].p99-latency-millis", () -> "240");
        registry.add("loadbalancerpro.proxy.upstreams[1].recent-error-rate", () -> "0.5");
    }

    @AfterAll
    static void stopUpstream() {
        UPSTREAM.close();
        DEGRADED_UPSTREAM.close();
    }

    @Test
    void recentEndpointReturnsRealAttemptWithoutRequestOrTargetData() throws Exception {
        mockMvc.perform(get("/proxy/orders?access_token=DO_NOT_RETAIN")
                        .header("Cookie", "session=DO_NOT_RETAIN_COOKIE"))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "decision-upstream"));

        mockMvc.perform(get("/api/proxy/decisions/recent"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("DO_NOT_RETAIN"))))
                .andExpect(content().string(not(containsString(UPSTREAM.baseUrl()))))
                .andExpect(content().string(not(containsString(DEGRADED_UPSTREAM.baseUrl()))))
                .andExpect(jsonPath("$.proxyEnabled", is(true)))
                .andExpect(jsonPath("$.retentionScope", is("process-local")))
                .andExpect(jsonPath("$.maxRetained", is(100)))
                .andExpect(jsonPath("$.retainedCount", is(1)))
                .andExpect(jsonPath("$.totalCaptured", is(1)))
                .andExpect(jsonPath("$.totalDropped", is(0)))
                .andExpect(jsonPath("$.decisions[0].decisionId", is("proxy-decision-00000001")))
                .andExpect(jsonPath("$.decisions[0].configurationGeneration", is(1)))
                .andExpect(jsonPath("$.decisions[0].routeName", is("legacy-upstreams")))
                .andExpect(jsonPath("$.decisions[0].strategy", is("WEIGHTED_LEAST_LOAD")))
                .andExpect(jsonPath("$.decisions[0].attempt", is(1)))
                .andExpect(jsonPath("$.decisions[0].selectionSource", is("strategy")))
                .andExpect(jsonPath("$.decisions[0].chosenUpstreamId", is("decision-upstream")))
                .andExpect(jsonPath("$.decisions[0].responseStatus", is(201)))
                .andExpect(jsonPath("$.decisions[0].latencyMillis", greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.decisions[0].retriable", is(false)))
                .andExpect(jsonPath("$.decisions[0].outcome", is("upstream_response")))
                .andExpect(jsonPath("$.decisions[0].candidates[0].upstreamId", is("decision-upstream")))
                .andExpect(jsonPath("$.decisions[0].candidates[0].healthy", is(true)))
                .andExpect(jsonPath("$.decisions[0].candidates[0].inFlightRequestCount", is(4)))
                .andExpect(jsonPath("$.decisions[0].candidates[0].averageLatencyMillis", is(11.0)))
                .andExpect(jsonPath("$.decisions[0].candidates[0].p95LatencyMillis", is(22.0)))
                .andExpect(jsonPath("$.decisions[0].candidates[0].p99LatencyMillis", is(33.0)))
                .andExpect(jsonPath("$.decisions[0].candidates[0].recentErrorRate", is(0.25)))
                .andExpect(jsonPath("$.decisions[0].candidates[0].observedAt").isString())
                .andExpect(jsonPath("$.decisions[0].candidates[1].upstreamId", is("degraded-upstream")))
                .andExpect(jsonPath("$.decisions[0].selectionEvidence").doesNotExist());

        mockMvc.perform(get("/api/proxy/decisions/proxy-decision-00000001/explain"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("DO_NOT_RETAIN"))))
                .andExpect(content().string(not(containsString(UPSTREAM.baseUrl()))))
                .andExpect(content().string(not(containsString(DEGRADED_UPSTREAM.baseUrl()))))
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.retainedLiveProxyAttempt", is(true)))
                .andExpect(jsonPath("$.retentionScope", is("process-local")))
                .andExpect(jsonPath("$.decisionId", is("proxy-decision-00000001")))
                .andExpect(jsonPath("$.routeName", is("legacy-upstreams")))
                .andExpect(jsonPath("$.strategyId", is("WEIGHTED_LEAST_LOAD")))
                .andExpect(jsonPath("$.selectionSource", is("strategy")))
                .andExpect(jsonPath("$.selectedCandidateId", is("decision-upstream")))
                .andExpect(jsonPath("$.exactCandidateIds[0]", is("decision-upstream")))
                .andExpect(jsonPath("$.exactCandidateIds[1]", is("degraded-upstream")))
                .andExpect(jsonPath("$.candidateObservations[0].inFlightRequestCount", is(4)))
                .andExpect(jsonPath("$.candidateObservations[1].p95LatencyMillis", is(160.0)))
                .andExpect(jsonPath("$.candidateObservations[1].recentErrorRate", is(0.5)))
                .andExpect(jsonPath("$.selectionEvidenceStatus", is("CAPTURED")))
                .andExpect(jsonPath("$.scorePreference", is("LOWER_WINS")))
                .andExpect(jsonPath("$.analysis.simulationOnly", is(false)))
                .andExpect(jsonPath("$.analysis.selectedCandidateId", is("decision-upstream")))
                .andExpect(jsonPath("$.analysis.dominantFactors.status", is("AVAILABLE")))
                .andExpect(jsonPath("$.analysis.decisionDelta.status", is("AVAILABLE")))
                .andExpect(jsonPath("$.analysis.decisionDelta.closestAlternativeCandidateId",
                        is("degraded-upstream")))
                .andExpect(jsonPath("$.analysis.counterfactualWeightScenarios").isNotEmpty())
                .andExpect(jsonPath("$.decisionChangeThresholds").isNotEmpty());

        mockMvc.perform(get("/api/proxy/decisions/proxy-decision-99999999/explain"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("not_found")))
                .andExpect(jsonPath("$.path",
                        is("/api/proxy/decisions/proxy-decision-99999999/explain")));

        awaitLiveShadowEvaluation();
        mockMvc.perform(get("/api/lase/shadow"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("DO_NOT_RETAIN"))))
                .andExpect(content().string(not(containsString(UPSTREAM.baseUrl()))))
                .andExpect(jsonPath("$.liveProxyDispatch.enabled", is(true)))
                .andExpect(jsonPath("$.liveProxyDispatch.queueCapacity", is(100)))
                .andExpect(jsonPath("$.liveProxyDispatch.totalAccepted", is(1)))
                .andExpect(jsonPath("$.liveProxyDispatch.totalCompleted", is(1)))
                .andExpect(jsonPath("$.liveProxyDispatch.totalDropped", is(0)))
                .andExpect(jsonPath("$.summary.totalEvaluations", is(1)))
                .andExpect(jsonPath("$.summary.comparableEvaluations", is(1)))
                .andExpect(jsonPath("$.recentEvents[0].evaluationId",
                        is("lase-shadow-proxy-decision-00000001")))
                .andExpect(jsonPath("$.recentEvents[0].strategy", is("WEIGHTED_LEAST_LOAD")))
                .andExpect(jsonPath("$.recentEvents[0].selectionSource", is("strategy")))
                .andExpect(jsonPath("$.recentEvents[0].candidateServerIds[0]", is("decision-upstream")))
                .andExpect(jsonPath("$.recentEvents[0].actualSelectedServerId", is("decision-upstream")));
    }

    private void awaitLiveShadowEvaluation() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (laseShadowRuntime.snapshot().liveProxyDispatch().totalCompleted() < 1
                && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }

    private static final class LoopbackUpstream implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;

        private LoopbackUpstream(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
        }

        private static LoopbackUpstream start() {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                LoopbackUpstream upstream = new LoopbackUpstream(server, executor);
                server.createContext("/", upstream::handle);
                server.setExecutor(executor);
                server.start();
                return upstream;
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private void handle(HttpExchange exchange) throws IOException {
            byte[] body = "created".getBytes(StandardCharsets.UTF_8);
            try {
                exchange.sendResponseHeaders(201, body.length);
                exchange.getResponseBody().write(body);
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
