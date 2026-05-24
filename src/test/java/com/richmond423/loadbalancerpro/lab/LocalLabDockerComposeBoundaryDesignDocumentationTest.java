package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabDockerComposeBoundaryDesignDocumentationTest {
    private static final Path DESIGN = Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md");
    private static final Path INDEX = Path.of("docs/LOCAL_LAB_MANUAL_TOOLING_INDEX.md");
    private static final Path RUNBOOK = Path.of("docs/LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md");
    private static final Path K6_DOC = Path.of("docs/LOCAL_LAB_K6_SMOKE_SCRIPT.md");
    private static final Path BRUNO_DOC = Path.of("docs/LOCAL_LAB_BRUNO_COLLECTION.md");
    private static final Path TOXIPROXY_DOC = Path.of("docs/LOCAL_LAB_TOXIPROXY_CONFIG.md");
    private static final Path BOUNDARY_PLAN = Path.of("docs/LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md");
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path READINESS = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path SELF = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabDockerComposeBoundaryDesignDocumentationTest.java");

    @Test
    void dockerComposeBoundaryDesignExistsAndStatesDocsOnlyFutureBoundary() throws Exception {
        String design = read(DESIGN).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "design-only",
                "future-only",
                "this pr does not add docker compose",
                "this pr does not add dockerfiles",
                "not ci-gated",
                "not wired into maven",
                "not production runtime behavior",
                "local-lab-only",
                "loopback/local defaults",
                "no production/cloud/tenant/external endpoint defaults")) {
            assertTrue(design.contains(expected), "Compose boundary design should include " + expected);
        }
    }

    @Test
    void dockerComposeBoundaryDesignReferencesExistingManualToolsAndReviewerPath() throws Exception {
        String design = read(DESIGN);

        for (String expected : List.of(
                "k6",
                "Bruno",
                "Toxiproxy",
                "LOCAL_LAB_MANUAL_TOOLING_INDEX.md",
                "LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md",
                "LOCAL_LAB_K6_SMOKE_SCRIPT.md",
                "LOCAL_LAB_BRUNO_COLLECTION.md",
                "LOCAL_LAB_TOXIPROXY_CONFIG.md",
                "LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md")) {
            assertTrue(design.contains(expected), "Compose boundary design should reference " + expected);
        }
    }

    @Test
    void dockerComposeBoundaryDesignListsFutureStopConditions() throws Exception {
        String normalized = read(DESIGN).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "future stop conditions",
                "non-loopback or production-looking targets",
                "ci starts executing compose without a separate review",
                "maven starts executing compose without a separate review",
                "benchmark/proof tooling",
                "storage/export/report generation",
                "secrets, credentials, cloud urls, tenant urls, or private network targets")) {
            assertTrue(normalized.contains(expected), "Compose boundary design should include stop condition " + expected);
        }
    }

    @Test
    void dockerComposeBoundaryDesignStatesEvidenceAndValidationBoundaries() throws Exception {
        String normalized = read(DESIGN).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no throughput evidence",
                "no p95/p99 evidence",
                "no load/stress/benchmark evidence",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "replay execution",
                "evidence/report generation",
                "storage/export behavior")) {
            assertTrue(normalized.contains(expected), "Compose boundary design should include boundary " + expected);
        }
    }

    @Test
    void existingLocalLabDocsCrossLinkBackToDockerComposeBoundaryDesign() throws Exception {
        for (Path doc : List.of(INDEX, RUNBOOK, K6_DOC, BRUNO_DOC, TOXIPROXY_DOC, BOUNDARY_PLAN, HANDOFF,
                NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            assertTrue(read(doc).contains("LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md"),
                    doc + " should cross-link the Docker Compose boundary design");
        }
    }

    @Test
    void dockerComposeBoundaryDesignAvoidsImplementationAndProductionOverclaims() throws Exception {
        for (Path doc : List.of(DESIGN, INDEX, RUNBOOK, K6_DOC, BRUNO_DOC, TOXIPROXY_DOC, BOUNDARY_PLAN, HANDOFF,
                NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String normalized = read(doc).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "docker compose is implemented",
                    "dockerfile is added",
                    "compose file is added",
                    "compose profile is added",
                    "production-ready",
                    "production certified",
                    "production certification is proven",
                    "live-cloud validated",
                    "real-tenant validated",
                    "runtime enforcement is implemented",
                    "load testing is complete",
                    "stress testing is complete",
                    "benchmarking is complete",
                    "throughput benchmark",
                    "p95/p99 benchmark",
                    "replay execution is implemented",
                    "evidence report is generated",
                    "storage is implemented",
                    "export behavior is implemented")) {
                assertFalse(normalized.contains(forbidden), doc + " must not overclaim " + forbidden);
            }
        }
    }

    @Test
    void guardTestOnlyReadsTrackedDocumentationAndDoesNotRunTools() throws Exception {
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
