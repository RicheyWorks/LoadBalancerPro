package com.richmond423.loadbalancerpro.api;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentTargetCatalog;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackTarget;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "loadbalancerpro.lase.policy.mode=active-experiment",
        "loadbalancerpro.lase.policy.active-experiment-enabled=true"
})
@AutoConfigureMockMvc
@Import(EnterpriseLabExperimentControllerLoopbackIntegrationTest.LoopbackConfiguration.class)
class EnterpriseLabExperimentControllerLoopbackIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoopbackHarness harness;

    @Test
    void operatorApiRoutesActualLoopbackRequestThenCancelsAndReturnsFinalRecord() throws Exception {
        String arm = """
                {
                  "operatorRequestId":"http-arm-1",
                  "experimentId":"http-experiment-1",
                  "scenarioId":"tail-latency-pressure",
                  "maximumRequestCount":10,
                  "maximumDurationSeconds":60,
                  "minimumEvidenceCount":2,
                  "holdDownCycles":2,
                  "expirationSeconds":120
                }
                """;
        mockMvc.perform(post("/api/lab/experiments/arm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(arm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPLIED")))
                .andExpect(jsonPath("$.experimentRecord.lifecycle.state", is("ARMED")))
                .andExpect(jsonPath("$.trafficActionPerformed", is(false)));

        String baselineRequest = """
                {"operatorRequestId":"http-baseline-1","count":3,"timeoutMillis":1000}
                """;
        mockMvc.perform(post("/api/lab/experiments/http-experiment-1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(baselineRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentCount", is(3)))
                .andExpect(jsonPath("$.observationsRecorded", is(3)))
                .andExpect(jsonPath("$.candidateRequestsRecorded", is(0)))
                .andExpect(jsonPath("$.trafficActionPerformed", is(false)));

        mockMvc.perform(post("/api/lab/experiments/http-experiment-1/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operatorRequestId\":\"http-start-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPLIED")))
                .andExpect(jsonPath("$.experimentRecord.lifecycle.state", is("RUNNING")))
                .andExpect(jsonPath("$.trafficActionPerformed", is(true)));

        String candidateRequest = """
                {"operatorRequestId":"http-candidate-1","count":1,"timeoutMillis":1000}
                """;
        mockMvc.perform(post("/api/lab/experiments/http-experiment-1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentCount", is(1)))
                .andExpect(jsonPath("$.observationsRecorded", is(1)))
                .andExpect(jsonPath("$.candidateRequestsRecorded", is(1)))
                .andExpect(jsonPath("$.trafficActionPerformed", is(true)))
                .andExpect(jsonPath("$.outcomes[0].outcome", is("SUCCESS")))
                .andExpect(jsonPath("$.outcomes[0].targetScope", is("approved Enterprise Lab loopback target")));

        mockMvc.perform(get("/api/lab/experiments/http-experiment-1/record"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.finalRecordAvailable", is(false)))
                .andExpect(jsonPath("$.reasonCode", is("EXPERIMENT_NOT_TERMINAL")));

        mockMvc.perform(post("/api/lab/experiments/http-experiment-1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"operatorRequestId":"http-cancel-1","reason":"operator integration cancellation"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RECORDED")))
                .andExpect(jsonPath("$.experimentRecord.lifecycle.state", is("ROLLED_BACK")))
                .andExpect(jsonPath("$.experimentRecord.currentAllocation.kind", is("RESTORED_BASELINE")))
                .andExpect(jsonPath("$.trafficActionPerformed", is(true)));

        mockMvc.perform(get("/api/lab/experiments/http-experiment-1/record"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalRecordAvailable", is(true)))
                .andExpect(jsonPath("$.reasonCode", is("FINAL_RECORD_AVAILABLE")))
                .andExpect(jsonPath("$.experimentRecord.lifecycle.terminal", is(true)))
                .andExpect(jsonPath("$.experimentRecord.contentFingerprint").isString());

        assertEquals(4, harness.requestCount(),
                "only the three explicit baseline requests and one explicit candidate request may be sent");
    }

    private static void assertEquals(int expected, int actual, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LoopbackConfiguration {
        @Bean(destroyMethod = "close")
        LoopbackHarness loopbackHarness() throws IOException {
            return LoopbackHarness.start();
        }

        @Bean
        @Primary
        EnterpriseLabExperimentTargetCatalog testEnterpriseLabExperimentTargetCatalog(LoopbackHarness harness) {
            return new EnterpriseLabExperimentTargetCatalog(harness.targets());
        }
    }

    static final class LoopbackHarness implements AutoCloseable {
        private static final String SCENARIO = "tail-latency-pressure";
        private final List<Backend> backends;

        private LoopbackHarness(List<Backend> backends) {
            this.backends = backends;
        }

        private static LoopbackHarness start() throws IOException {
            List<Backend> started = new ArrayList<>();
            try {
                started.add(Backend.start("blue"));
                started.add(Backend.start("green"));
                started.add(Backend.start("orange"));
                return new LoopbackHarness(List.copyOf(started));
            } catch (IOException exception) {
                started.forEach(Backend::close);
                throw exception;
            }
        }

        private List<EnterpriseLabLoopbackTarget> targets() {
            return backends.stream()
                    .map(backend -> new EnterpriseLabLoopbackTarget(
                            SCENARIO, backend.backendId, backend.uri()))
                    .toList();
        }

        private int requestCount() {
            return backends.stream().mapToInt(value -> value.requestCount.get()).sum();
        }

        @Override
        public void close() {
            backends.forEach(Backend::close);
        }
    }

    private static final class Backend implements AutoCloseable {
        private final String backendId;
        private final HttpServer server;
        private final AtomicInteger requestCount = new AtomicInteger();

        private Backend(String backendId, HttpServer server) {
            this.backendId = backendId;
            this.server = server;
        }

        private static Backend start(String backendId) throws IOException {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            Backend backend = new Backend(backendId, server);
            server.createContext("/enterprise-lab/operator-api", backend::handle);
            server.start();
            return backend;
        }

        private void handle(HttpExchange exchange) throws IOException {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        }

        private URI uri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                    + "/enterprise-lab/operator-api");
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
