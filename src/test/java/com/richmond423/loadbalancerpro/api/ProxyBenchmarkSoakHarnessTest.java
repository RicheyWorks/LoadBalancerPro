package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProxyBenchmarkSoakHarnessTest {
    private static final Path SCRIPT = Path.of("scripts/bench/proxy-benchmark-soak.sh");
    private static final Path OVERRIDE = Path.of("scripts/bench/docker-compose.bench.yml");
    private static final Path GUIDE = Path.of("scripts/bench/README.md");
    private static final Path NIGHTLY = Path.of(".github/workflows/proxy-nightly-soak.yml");
    private static final Path CI = Path.of(".github/workflows/ci.yml");

    @Test
    void harnessContainsAllBoundedLoopbackScenariosAndGates() throws IOException {
        String script = Files.readString(SCRIPT);
        for (String scenario : List.of(
                "steady",
                "spike",
                "slow-backend",
                "backend-kill",
                "reload-under-load",
                "drain-under-load")) {
            assertTrue(script.contains(scenario), "missing scenario " + scenario);
        }
        assertTrue(script.contains("base_url=\"https://127.0.0.1:$proxy_port\""));
        assertFalse(script.contains("LBP_BENCH_BASE_URL"));
        assertTrue(script.contains("lbp_proxy_inflight"));
        assertTrue(script.contains("jvm_memory_used_bytes"));
        assertTrue(script.contains(".latencies[\"99th\"]"));
        assertTrue(script.contains("p99_budget_ms"));
        assertTrue(script.contains("zero_failure_required"));
        assertTrue(script.contains("^5[0-9][0-9]$"));
        assertTrue(script.contains("require_zero_failures"));
        assertTrue(script.contains("Soak mode requires at least 3600 seconds"));
        assertTrue(script.contains("Heap post-GC floor trend exceeded the local growth budget"));
        assertTrue(script.contains("Refusing to remove unexpected temporary path"));
        assertTrue(script.contains("no production SLO, capacity, or certification claim"));
    }

    @Test
    void composeOverrideChangesOnlyHarnessRuntimeTuning() throws IOException {
        String override = Files.readString(OVERRIDE);
        assertTrue(override.contains("services:"));
        assertTrue(override.contains("loadbalancerpro:"));
        assertTrue(override.contains("LBP_HEALTH_CHECK_INTERVAL: 1s"));
        assertTrue(override.contains("LOADBALANCERPRO_PROXY_RETRY_ENABLED: \"true\""));
        assertFalse(override.contains("ports:"));
        assertFalse(override.contains("http://"));
        assertFalse(override.contains("https://"));
    }

    @Test
    void ciRunsSmokeAndNightlyRunsChecksumPinnedOneHourSoak() throws IOException {
        String ci = Files.readString(CI);
        String nightly = Files.readString(NIGHTLY);
        assertTrue(ci.contains("proxy-benchmark-soak.sh --mode smoke"));
        assertTrue(ci.contains("e8759ce45c14e18374bdccd3ba6068197bc3a9f9b7e484db3837f701b9d12e61"));
        assertTrue(nightly.contains("cron: '23 5 * * *'"));
        assertTrue(nightly.contains("LBP_BENCH_SOAK_SECONDS: '3600'"));
        assertTrue(nightly.contains("proxy-benchmark-soak.sh --mode soak"));
        assertTrue(nightly.contains("timeout-minutes: 90"));
        assertTrue(nightly.contains("retention-days: 30"));
        assertTrue(nightly.contains("e8759ce45c14e18374bdccd3ba6068197bc3a9f9b7e484db3837f701b9d12e61"));
        assertFalse(nightly.contains("secrets."));
    }

    @Test
    void guideKeepsEvidenceAndSecretBoundariesExplicit() throws IOException {
        String guide = Files.readString(GUIDE);
        assertTrue(guide.contains("fixed to `https://127.0.0.1:<port>`"));
        assertTrue(guide.contains("never written to evidence"));
        assertTrue(guide.contains("one-hour soak"));
        assertTrue(guide.contains("zero 5xx responses or transport errors during reload and drain"));
        assertTrue(guide.contains("do not establish production SLOs"));
        assertTrue(guide.contains("do not establish production SLOs, production capacity"));
    }
}
