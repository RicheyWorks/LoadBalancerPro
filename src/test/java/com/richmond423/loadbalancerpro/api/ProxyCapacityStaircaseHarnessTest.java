package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProxyCapacityStaircaseHarnessTest {
    private static final Path SCRIPT = Path.of("scripts/bench/proxy-capacity-staircase.sh");
    private static final Path PROFILE = Path.of("scripts/bench/capacity-profile.example.json");
    private static final Path TARGET_RENDERER = Path.of("scripts/bench/render-capacity-targets.jq");
    private static final Path FIXTURE = Path.of("deploy/fixture/FixtureBackend.java");
    private static final Path COMPOSE_SMOKE = Path.of("scripts/smoke/proxy-prod-compose-smoke.sh");
    private static final Path CI = Path.of(".github/workflows/ci.yml");

    @Test
    void runnerUsesFreshProcessesAndCapturesSaturationEvidence() throws IOException {
        String script = Files.readString(SCRIPT);
        for (String scenario : List.of("equal", "slow", "failing", "draining", "recovering")) {
            assertTrue(script.contains(scenario), "missing capacity case " + scenario);
        }
        for (String signal : List.of(
                "achievedThroughput",
                "p50Millis",
                "p95Millis",
                "p99Millis",
                "statusCodes",
                "upstreamDistribution",
                "retriesDelta",
                "shedsDelta",
                "maxInflight",
                "heapUsedBytes",
                "processCpuUsage",
                "processMemoryBytes",
                "containerMemoryBytes",
                "gcPauseSecondsDelta",
                "openConnections",
                "processThreads")) {
            assertTrue(script.contains(signal), "missing capacity signal " + signal);
        }
        assertTrue(script.contains(".capacity.repeatsPerStep | type == \"number\" and . >= 3"));
        assertTrue(script.contains("fresh_proxy_process"));
        assertTrue(script.contains("down --volumes --remove-orphans"));
        assertTrue(script.contains("up --no-build --detach"));
        assertTrue(script.contains("base_url=\"https://127.0.0.1:$proxy_port\""));
        assertFalse(script.contains("LBP_CAPACITY_BASE_URL"));
        assertTrue(script.contains("review.status == \"reviewed\""));
        assertTrue(script.contains(".workload.topology.proxyReplicas == 1"));
        assertTrue(script.contains(".workload.topology.ingress == \"127.0.0.1 only\""));
        assertTrue(script.contains(".workload.upstreams.count == 2"));
        assertTrue(script.contains("maxInFlight: $connection_limit_per_upstream"));
        assertTrue(script.contains("read_process_memory_bytes"));
        assertTrue(script.contains("required_qualification_rate"));
        assertTrue(script.contains("forecast_growth_percent"));
        assertTrue(script.contains("burst_rate"));
        assertFalse(script.contains("LBP_CAPACITY_REUSE_IMAGE"));
        assertTrue(script.contains("Run mode requires a clean checkout"));
        assertTrue(script.contains("client.bin"), "raw Vegeta results must be retained");
        assertFalse(script.contains("rm -f -- \"$results_file\""));
        assertTrue(script.contains("firstReproducibleSaturationRate"));
        assertTrue(script.contains("recommendedOperatingEnvelopeRate"));
        assertTrue(script.contains("forecastPeakPlusHeadroomRate"));
        assertTrue(script.contains("-f \"$target_renderer\""));
        assertTrue(Files.readString(TARGET_RENDERER).contains(".workload.routeMix"));
        assertTrue(Files.readString(TARGET_RENDERER).contains("@base64"));
        assertTrue(Files.readString(FIXTURE).contains("lbpResponseBytes="));
        assertTrue(Files.readString(COMPOSE_SMOKE).contains("lbpResponseBytes=4096"));
    }

    @Test
    void exampleProfileIsCompleteButCannotAuthorizeExecution() throws IOException {
        JsonNode profile = new ObjectMapper().readTree(Files.readString(PROFILE));
        assertEquals(1, profile.path("schemaVersion").asInt());
        assertEquals("example", profile.path("review").path("status").asText());
        assertTrue(profile.path("review").path("approvedBy").asText().isEmpty());
        assertTrue(profile.path("review").path("approvedAt").asText().isEmpty());
        assertTrue(profile.path("capacity").path("repeatsPerStep").asInt() >= 3);
        assertTrue(profile.path("capacity").path("ratesPerSecond").size() >= 2);
        assertEquals(100, profile.path("workload").path("routeMix").get(0).path("percent").asInt());
        for (String section : List.of(
                "requestRate", "concurrency", "routeMix", "payload", "upstreams",
                "objectives", "failureModel", "topology")) {
            assertFalse(profile.path("workload").path(section).isMissingNode(),
                    "missing workload contract section " + section);
        }
    }

    @Test
    void ciSyntaxChecksAndValidatesCapacityContractWithoutMakingAClaim() throws IOException {
        String ci = Files.readString(CI);
        assertTrue(ci.contains("bash -n scripts/bench/proxy-capacity-staircase.sh"));
        assertTrue(ci.contains("proxy-capacity-staircase.sh --mode validate"));
        assertFalse(ci.contains("proxy-capacity-staircase.sh --mode run"));
    }
}
