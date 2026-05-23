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
                "Expanded Toxiproxy fault execution remains future-only tooling",
                "Docker/Docker Compose is future-only unless separately scoped",
                "k6 has one optional local-lab smoke script skeleton",
                "Bruno has one optional local-lab Bruno collection skeleton",
                "Toxiproxy has one optional local-lab Toxiproxy config skeleton",
                "LOCAL_LAB_BRUNO_COLLECTION.md",
                "lab/bruno/local-lab-smoke",
                "LOCAL_LAB_TOXIPROXY_CONFIG.md",
                "lab/toxiproxy/local-lab-toxiproxy.json",
                "not Toxiproxy integration",
                "not k6 execution",
                "not wired into k6 execution",
                "not wired into Bruno execution",
                "does not start Toxiproxy",
                "does not start the application",
                "Toxiproxy has one optional local-lab Toxiproxy config skeleton and remains a future local-lab tool candidate for expanded network degradation simulation",
                "Docker/Docker Compose is a future local service orchestration candidate only after separate boundary review")) {
            assertTrue(plan.contains(expected), "boundary plan should include " + expected);
        }
    }

    @Test
    void boundaryPlanAndCoreDocsStateNoToolFilesOrRuntimeBehaviorAreAdded() throws Exception {
        for (Path doc : List.of(BOUNDARY_PLAN, HANDOFF, NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String text = read(doc);

            assertTrue(text.contains("docs/test-only k6/Bruno/Toxiproxy implementation boundary plan"));
            assertTrue(text.contains("optional local-lab k6 smoke script skeleton"));
            assertTrue(text.contains("optional local-lab Bruno collection skeleton"));
            assertTrue(text.contains("optional local-lab Toxiproxy config skeleton"));
            assertTrue(text.contains("expanded Toxiproxy fault execution"));
            assertTrue(text.contains("no Docker Compose files"));
            assertTrue(text.contains("not Toxiproxy integration"));
            assertTrue(text.contains("not k6 execution"));
            assertTrue(text.contains("not wired into k6 execution"));
            assertTrue(text.contains("not wired into Bruno execution"));
            assertTrue(text.contains("automatic"));
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
                "Lane A4a: first optional local-lab k6 smoke script skeleton",
                "Lane A4b: future expanded k6 scenario files only after separate review",
                "Lane A5a: first optional local-lab Bruno collection skeleton",
                "Lane A5b: future expanded Bruno collection files only after separate review",
                "Lane A6a: first optional local-lab Toxiproxy config skeleton",
                "Lane A6b: future expanded Toxiproxy fault execution only after separate review",
                "Lane A7: future Docker Compose only after a separate Docker boundary plan",
                "a PR adds expanded k6, expanded Bruno, or expanded Toxiproxy fault files without separate approval",
                "the optional k6 smoke skeleton stops defaulting to loopback/local targets",
                "the optional k6 smoke skeleton becomes CI-gated or Dockerized",
                "the optional Bruno collection skeleton stops defaulting to loopback/local targets",
                "the optional Bruno collection skeleton becomes CI-gated, Dockerized, Toxiproxy integration, or k6 execution",
                "the optional Toxiproxy config skeleton stops defaulting to loopback/local targets",
                "the optional Toxiproxy config skeleton binds to `0.0.0.0`",
                "the optional Toxiproxy config skeleton becomes CI-gated, Dockerized, Docker Compose orchestration, wired into the application, wired into Maven, wired into k6 execution, or wired into Bruno execution",
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
                "Is this limited to the one optional k6 smoke skeleton, one optional Bruno collection skeleton, one optional Toxiproxy config skeleton, plus docs/tests?",
                "Is expanded Toxiproxy fault execution still separately scoped?",
                "Is expanded k6 work still separately scoped?",
                "Is expanded Bruno work still separately scoped?",
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
                "Docker/Toxiproxy platform implementation",
                "expanded k6 scenario implementation",
                "expanded Bruno collection implementation",
                "expanded Toxiproxy fault execution",
                "automatic k6 execution",
                "automatic Bruno execution",
                "automatic Toxiproxy execution",
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
