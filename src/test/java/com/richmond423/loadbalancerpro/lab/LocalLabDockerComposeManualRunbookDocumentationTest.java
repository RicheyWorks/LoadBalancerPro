package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabDockerComposeManualRunbookDocumentationTest {
    private static final Path RUNBOOK = Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md");
    private static final Path COMPOSE = Path.of("lab/docker-compose/local-lab-compose.yml");
    private static final Path SKELETON_DOC = Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md");
    private static final Path DESIGN_DOC = Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md");
    private static final Path INDEX = Path.of("docs/LOCAL_LAB_MANUAL_TOOLING_INDEX.md");
    private static final Path TOOLING_RUNBOOK = Path.of("docs/LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md");
    private static final Path TOXIPROXY_DOC = Path.of("docs/LOCAL_LAB_TOXIPROXY_CONFIG.md");
    private static final Path K6_DOC = Path.of("docs/LOCAL_LAB_K6_SMOKE_SCRIPT.md");
    private static final Path BRUNO_DOC = Path.of("docs/LOCAL_LAB_BRUNO_COLLECTION.md");
    private static final Path BOUNDARY_PLAN = Path.of("docs/LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md");
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path READINESS = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path SELF = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabDockerComposeManualRunbookDocumentationTest.java");

    @Test
    void manualComposeRunbookExistsAndReferencesCurrentDocs() throws Exception {
        String runbook = read(RUNBOOK);

        for (String expected : List.of(
                "docs/test-only",
                "lab/docker-compose/local-lab-compose.yml",
                "docs/LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md",
                "LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md",
                "LOCAL_LAB_MANUAL_TOOLING_INDEX.md",
                "LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md")) {
            assertTrue(runbook.contains(expected), "Compose manual runbook should reference " + expected);
        }
    }

    @Test
    void manualComposeRunbookStatesInspectionAndOptionalManualLocalPaths() throws Exception {
        String normalized = read(RUNBOOK).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "inspection-only path",
                "optional manual local-only path",
                "docker compose -f lab/docker-compose/local-lab-compose.yml config",
                "docker compose -f lab/docker-compose/local-lab-compose.yml up toxiproxy",
                "docker compose -f lab/docker-compose/local-lab-compose.yml down",
                "not ci-gated",
                "not wired into maven",
                "not production docker packaging",
                "does not add new compose behavior",
                "does not add new services")) {
            assertTrue(normalized.contains(expected), "Compose manual runbook should include " + expected);
        }
    }

    @Test
    void manualComposeRunbookStatesServiceAndTargetStopBoundaries() throws Exception {
        String normalized = read(RUNBOOK).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "127.0.0.1",
                "loopback only",
                "no 0.0.0.0",
                "no external/cloud/tenant/production endpoint",
                "no secrets/credentials",
                "no app service",
                "no k6 runner service",
                "no bruno runner service",
                "not production docker packaging",
                "not ci-gated",
                "not wired into maven")) {
            assertTrue(normalized.contains(expected), "Compose manual runbook should preserve boundary " + expected);
        }
    }

    @Test
    void manualComposeRunbookStatesEvidenceAndValidationBoundaries() throws Exception {
        String normalized = read(RUNBOOK).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no throughput evidence",
                "no p95/p99 evidence",
                "no load/stress/benchmark evidence",
                "no production readiness/certification",
                "no live-cloud or real-tenant validation",
                "no runtime enforcement",
                "no replay execution, evidence/report generation, storage, or export behavior")) {
            assertTrue(normalized.contains(expected), "Compose manual runbook should include " + expected);
        }
    }

    @Test
    void manualComposeRunbookListsRemainingNotProvenBoundaries() throws Exception {
        String runbook = read(RUNBOOK);

        for (String expected : List.of(
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "Docker/k6/Bruno/Toxiproxy platform implementation beyond optional local-lab Compose skeleton",
                "replay execution",
                "evidence/report generation",
                "storage/export behavior",
                "load testing",
                "stress testing",
                "benchmarking",
                "throughput evidence",
                "p95/p99 evidence",
                "autonomous production traffic shifting",
                "carbon-aware routing",
                "GPU orchestration",
                "power/grid control",
                "facility automation")) {
            assertTrue(runbook.contains(expected), "Compose manual runbook should list " + expected);
        }
    }

    @Test
    void existingLocalLabDocsCrossLinkBackToComposeManualRunbook() throws Exception {
        for (Path doc : List.of(SKELETON_DOC, DESIGN_DOC, INDEX, TOOLING_RUNBOOK, TOXIPROXY_DOC, K6_DOC, BRUNO_DOC,
                BOUNDARY_PLAN, HANDOFF, READINESS, NEXT_STEPS, MATRIX, TRUST_MAP, ADR_0009)) {
            assertTrue(read(doc).contains("LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md"),
                    doc + " should cross-link the Compose manual runbook");
        }
    }

    @Test
    void composeRunbookSprintDoesNotChangeComposeBehaviorOrRunnerServices() throws Exception {
        String compose = read(COMPOSE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "local-lab-only",
                "manual-only",
                "127.0.0.1:8474:8474",
                "127.0.0.1:18080:18080",
                "127.0.0.1:18081:18081",
                "../toxiproxy/local-lab-toxiproxy.json")) {
            assertTrue(compose.contains(expected), "Compose skeleton should still contain " + expected);
        }

        for (String forbidden : List.of(
                "0.0.0.0",
                "app-under-test",
                "k6",
                "bruno",
                "password",
                "secret",
                "credential",
                "token",
                "http://",
                "https://")) {
            assertFalse(compose.contains(forbidden), "Compose skeleton should not contain " + forbidden);
        }
    }

    @Test
    void manualComposeRunbookAvoidsProductionAndEvidenceOverclaims() throws Exception {
        for (Path doc : List.of(RUNBOOK, SKELETON_DOC, DESIGN_DOC, INDEX, TOOLING_RUNBOOK, TOXIPROXY_DOC, K6_DOC,
                BRUNO_DOC, BOUNDARY_PLAN, HANDOFF, READINESS, NEXT_STEPS, MATRIX, TRUST_MAP, ADR_0009)) {
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
                    "app service is operational",
                    "k6 runner service is operational",
                    "bruno runner service is operational",
                    "compose is wired into ci",
                    "compose is wired into maven",
                    "replay execution is implemented",
                    "evidence report is generated",
                    "report generation is implemented",
                    "storage is implemented",
                    "export behavior is implemented")) {
                assertFalse(normalized.contains(forbidden), doc + " must not overclaim " + forbidden);
            }
        }
    }

    @Test
    void guardTestOnlyReadsTrackedFilesAndDoesNotRunTools() throws Exception {
        String source = read(SELF);

        for (String forbidden : List.of(
                "Files." + "write",
                "write" + "String",
                "Process" + "Builder",
                "Runtime." + "getRuntime",
                ".ex" + "ec(",
                "Http" + "Client",
                "URL" + "Connection",
                "new " + "Socket",
                "System." + "getenv",
                "System." + "getProperty")) {
            assertFalse(source.contains(forbidden), "guard test must not use " + forbidden);
        }
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
