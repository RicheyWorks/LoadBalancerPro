package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabK6BrunoToxiproxyBoundaryPlanDocumentationTest {
    private static final Path BOUNDARY_PLAN = Path.of("docs/LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md");
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path READINESS = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");

    @Test
    void boundaryPlanDocExistsAndKeepsToolsFutureOnly() throws Exception {
        String plan = read(BOUNDARY_PLAN);

        for (String expected : List.of(
                "k6, Bruno, and Toxiproxy are future-only tooling",
                "Docker/Docker Compose is future-only unless separately scoped",
                "k6 is a future local-lab tool candidate for controlled traffic scenarios",
                "Bruno is a future local-lab tool candidate for API check collections",
                "Toxiproxy is a future local-lab tool candidate for network degradation simulation",
                "Docker/Docker Compose is a future local service orchestration candidate only after separate boundary review")) {
            assertTrue(plan.contains(expected), "boundary plan should include " + expected);
        }
    }

    @Test
    void boundaryPlanAndCoreDocsStateNoToolFilesOrRuntimeBehaviorAreAdded() throws Exception {
        for (Path doc : List.of(BOUNDARY_PLAN, HANDOFF, NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String text = read(doc);

            assertTrue(text.contains("docs/test-only k6/Bruno/Toxiproxy implementation boundary plan"));
            assertTrue(text.contains("no k6 scripts"));
            assertTrue(text.contains("no Bruno collections"));
            assertTrue(text.contains("no Toxiproxy config"));
            assertTrue(text.contains("no Docker Compose files"));
            assertTrue(text.contains("no scripts"));
            assertTrue(text.contains("no CI jobs"));
            assertTrue(text.contains("no Maven dependencies"));
            assertTrue(text.contains("no production endpoints"));
            assertTrue(text.contains("no production listeners"));
            assertTrue(text.contains("no replay execution"));
            assertTrue(text.contains("no evidence/report generation"));
            assertTrue(text.contains("no storage/export behavior"));
            assertTrue(text.contains("no load/stress/benchmark testing"));
            assertTrue(text.contains("no throughput evidence"));
            assertTrue(text.contains("no p95/p99 evidence"));
        }
    }

    @Test
    void boundaryPlanDocumentsSeparateFutureLanesAndStopConditions() throws Exception {
        String combined = read(BOUNDARY_PLAN) + "\n" + read(HANDOFF) + "\n" + read(NEXT_STEPS);

        for (String expected : List.of(
                "Lane A1: docs-only k6 scenario design",
                "Lane A2: docs-only Bruno collection design",
                "Lane A3: docs-only Toxiproxy fault model design",
                "Lane A4: future actual k6 files only after boundary approval",
                "Lane A5: future actual Bruno collection only after boundary approval",
                "Lane A6: future actual Toxiproxy config only after boundary approval",
                "Lane A7: future Docker Compose only after a separate Docker boundary plan",
                "a PR adds actual k6/Bruno/Toxiproxy files without separate approval",
                "a PR adds Docker Compose before the Docker boundary plan",
                "a PR changes `src/main/java`",
                "a PR adds production endpoint wiring",
                "a PR changes Maven dependencies",
                "a PR changes CI",
                "a PR claims production validation, certification, live-cloud validation, or real-tenant validation",
                "a PR claims load/stress/benchmark/p95/p99/throughput evidence without a separately validated test lane")) {
            assertTrue(combined.contains(expected), "boundary docs should include lane or stop condition " + expected);
        }
    }

    @Test
    void boundaryPlanIncludesReviewerChecklistAndNotProvenBoundaries() throws Exception {
        String plan = read(BOUNDARY_PLAN);

        for (String expected : List.of(
                "Is this docs-only?",
                "Are k6/Bruno/Toxiproxy still future-only?",
                "Are Docker/Compose still future-only?",
                "Are load/stress/benchmark claims absent?",
                "Are throughput/p95/p99 evidence claims absent?",
                "Are production/live-cloud/real-tenant claims absent?",
                "Are storage/export/replay/report claims absent?",
                "Are next implementation steps separately scoped?",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "Docker/k6/Bruno/Toxiproxy implementation",
                "replay execution",
                "evidence/report generation",
                "storage/export behavior",
                "load testing",
                "stress testing",
                "benchmarking",
                "throughput evidence",
                "p95/p99 evidence")) {
            assertTrue(plan.contains(expected), "boundary plan should include reviewer boundary " + expected);
        }
    }

    @Test
    void boundaryPlanDocsAvoidToolingValidationReplayReportStorageExportAndBenchmarkOverclaims()
            throws Exception {
        for (Path doc : List.of(BOUNDARY_PLAN, HANDOFF, NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
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
