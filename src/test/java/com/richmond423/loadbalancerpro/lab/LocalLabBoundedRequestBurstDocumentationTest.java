package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabBoundedRequestBurstDocumentationTest {
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path READINESS = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");

    @Test
    void coreDocsDescribeBoundedBurstSprintAsBoundedTestScopeOnly() throws Exception {
        for (Path doc : List.of(HANDOFF, NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String text = read(doc);

            assertTrue(text.contains(
                    "This PR adds deterministic test-scope bounded request burst smoke tests and in-memory summaries only."));
            assertTrue(text.contains(
                    "The burst uses the existing `src/test/java` multi-backend loopback harness."));
            assertTrue(text.contains(
                    "The burst calls only `127.0.0.1` harness URLs with ephemeral ports."));
            assertTrue(text.contains("The burst uses fixed small request counts only."));
            assertTrue(text.contains(
                    "It is not k6, Bruno, Docker, Toxiproxy, load testing, stress testing, benchmarking, or production traffic."));
            assertTrue(text.contains("It does not add production endpoints."));
            assertTrue(text.contains("It does not change production routing, proxy, scoring, or API behavior."));
            assertTrue(text.contains("It does not execute replay."));
            assertTrue(text.contains("It does not generate evidence reports."));
            assertTrue(text.contains("It does not write files."));
            assertTrue(text.contains("It does not persist storage."));
            assertTrue(text.contains("It does not export/download/upload/PDF/ZIP anything."));
            assertTrue(text.contains("Passing bounded burst tests is not production proof."));
            assertTrue(text.contains(
                    "Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven."));
        }
    }

    @Test
    void handoffDocsMentionBoundedBurstLayerAndRemainingBoundaries() throws Exception {
        String combined = read(HANDOFF) + "\n" + read(NEXT_STEPS);

        for (String expected : List.of(
                "Bounded request burst smoke tests",
                "In-memory bounded request burst summary renderer",
                "fixed small request counts",
                "not load/stress/benchmark testing",
                "not throughput evidence",
                "not p95/p99 evidence",
                "not production proof",
                "not Docker/k6/Bruno/Toxiproxy execution",
                "not replay execution",
                "not evidence/report generation",
                "not storage/export")) {
            assertTrue(combined.contains(expected), "handoff should mention " + expected);
        }
    }

    @Test
    void boundedBurstDocsAvoidProductionValidationReplayReportStorageExportBenchmarkAndAutonomyOverclaims()
            throws Exception {
        for (Path doc : List.of(HANDOFF, NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String normalized = read(doc).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "production-ready",
                    "production certified",
                    "production certification is proven",
                    "production validation is complete",
                    "live-cloud validated",
                    "live-cloud validation is complete",
                    "real-tenant validated",
                    "real-tenant validation is complete",
                    "runtime enforcement is implemented",
                    "load testing is complete",
                    "stress testing is complete",
                    "benchmarking is complete",
                    "throughput benchmark",
                    "p95/p99 benchmark",
                    "docker compose is implemented",
                    "k6 scenario is implemented",
                    "bruno collection is implemented",
                    "toxiproxy config is implemented",
                    "prometheus/grafana dashboard is implemented",
                    "replay execution is implemented",
                    "evidence report is generated",
                    "report generation is implemented",
                    "storage is implemented",
                    "export behavior is implemented",
                    "autonomous production traffic shifting is implemented",
                    "carbon-aware routing is implemented",
                    "gpu orchestration is implemented",
                    "power/grid control is implemented",
                    "facility automation is implemented")) {
                assertFalse(normalized.contains(forbidden), doc + " must not overclaim " + forbidden);
            }
        }
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
