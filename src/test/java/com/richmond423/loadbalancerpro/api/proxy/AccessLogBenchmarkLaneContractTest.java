package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AccessLogBenchmarkLaneContractTest {
    @Test
    void benchmarkIsOptInFreshForkedAndPreservesRawNonProductionEvidence() throws Exception {
        Path benchmark = Path.of(
                "src/test/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyAccessLogBenchmark.java");
        Path oldGate = Path.of(
                "src/test/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyAccessLogOverheadTest.java");
        String source = read(benchmark);
        String script = read(Path.of("scripts/bench/access-log-overhead.sh"));
        String workflow = read(Path.of(".github/workflows/access-log-benchmark.yml"));

        assertFalse(Files.exists(oldGate), "hosted-runner threshold gate must not remain in the default suite");
        assertTrue(source.contains("class ReverseProxyAccessLogBenchmark"));
        assertFalse(source.contains("overheadPercent < 5.0"));
        assertTrue(source.contains("disabledFirstNanos"));
        assertTrue(source.contains("enabledFirstNanos"));
        assertTrue(source.contains("pairedOverheadPercent"));
        assertTrue(source.contains("not production proof"));

        assertTrue(script.contains("LBP_ACCESS_LOG_BENCHMARK_FORKS"));
        assertTrue(script.contains("-DforkCount=1"));
        assertTrue(script.contains("-DreuseForks=false"));
        assertTrue(script.contains("fork-${fork}.json"));
        assertFalse(script.contains("< 5"));

        assertTrue(workflow.contains("workflow_dispatch:"));
        assertTrue(workflow.contains("schedule:"));
        assertTrue(workflow.contains("if: always()"));
        assertTrue(workflow.contains("target/access-log-benchmark"));
        assertTrue(workflow.contains("Non-Production Evidence"));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " must exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
