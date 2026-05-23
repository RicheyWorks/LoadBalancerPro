package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabTrafficMatrixDocumentationTest {
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path READINESS = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");

    @Test
    void coreDocsDescribeTrafficMatrixSprintAsBoundedTestScopeOnly() throws Exception {
        for (Path doc : List.of(HANDOFF, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String text = read(doc);

            assertTrue(text.contains(
                    "This PR adds deterministic test-scope traffic matrix tests and in-memory summaries only."));
            assertTrue(text.contains(
                    "The matrix uses the existing `src/test/java` multi-backend loopback harness."));
            assertTrue(text.contains(
                    "The matrix calls only `127.0.0.1` harness URLs with ephemeral ports."));
            assertTrue(text.contains(
                    "It is not k6, Bruno, Docker, Toxiproxy, load testing, stress testing, or production traffic."));
            assertTrue(text.contains("It does not add production endpoints."));
            assertTrue(text.contains("It does not change production routing, proxy, scoring, or API behavior."));
            assertTrue(text.contains("It does not execute replay."));
            assertTrue(text.contains("It does not generate evidence reports."));
            assertTrue(text.contains("It does not write files."));
            assertTrue(text.contains("It does not persist storage."));
            assertTrue(text.contains("It does not export/download/upload/PDF/ZIP anything."));
            assertTrue(text.contains("Passing matrix tests is not production proof."));
        }
    }

    @Test
    void handoffDocsMentionTrafficMatrixLayerAndRemainingBoundaries() throws Exception {
        String handoff = read(HANDOFF);

        assertTrue(handoff.contains("deterministic traffic matrix tests"));
        assertTrue(handoff.contains("in-memory traffic matrix summary"));
        assertTrue(handoff.contains("traffic matrix tests"));
        assertTrue(handoff.contains("not load/stress testing"));
        assertTrue(handoff.contains("not production proof"));
        assertTrue(handoff.contains("Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven."));
    }

    @Test
    void trafficMatrixDocsAvoidProductionValidationReplayReportStorageAndExportOverclaims() throws Exception {
        for (Path doc : List.of(HANDOFF, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
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
