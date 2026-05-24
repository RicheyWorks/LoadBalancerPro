package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabDockerComposeAppServiceBoundaryDesignDocumentationTest {
    private static final Path APP_SERVICE_DESIGN =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md");
    private static final Path COMPOSE = Path.of("lab/docker-compose/local-lab-compose.yml");
    private static final Path APP_SERVICE_SKELETON =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md");
    private static final Path COMPOSE_MANUAL_RUNBOOK =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md");
    private static final Path COMPOSE_SKELETON = Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md");
    private static final Path COMPOSE_BOUNDARY = Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md");
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
    private static final Path SELF = Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/"
            + "LocalLabDockerComposeAppServiceBoundaryDesignDocumentationTest.java");

    @Test
    void appServiceBoundaryDesignExistsAndStatesGatedScope() throws Exception {
        String normalized = read(APP_SERVICE_DESIGN).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "boundary design",
                "app-under-test",
                "first gated app-service skeleton follows this design",
                "local-lab docker compose lane",
                "does not add docker packaging",
                "makes no docker packaging changes",
                "not ci-gated",
                "not maven-wired",
                "does not wire compose into ci or maven",
                "does not change production runtime behavior",
                "does not change production runtime behavior")) {
            assertTrue(normalized.contains(expected), "app service design should state " + expected);
        }
    }

    @Test
    void appServiceBoundaryDesignStatesLocalExposureAndSecretBoundaries() throws Exception {
        String normalized = read(APP_SERVICE_DESIGN).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "local-lab-only",
                "loopback/local-only",
                "no 0.0.0.0 default exposure",
                "no production/cloud/tenant/external endpoints",
                "no cloud/private network/tenant targets",
                "no secrets or credentials",
                "no secrets/credentials",
                "no production profile",
                "no docker image publishing",
                "registry login",
                "registry push")) {
            assertTrue(normalized.contains(expected), "app service design should preserve " + expected);
        }
    }

    @Test
    void appServiceBoundaryDesignKeepsCurrentToolingSeparate() throws Exception {
        String design = read(APP_SERVICE_DESIGN);

        for (String expected : List.of(
                "current Compose skeleton contains Toxiproxy plus the gated app-under-test service",
                "LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md",
                "k6 smoke script remains manual and separate",
                "Bruno collection remains manual and separate",
                "Toxiproxy remains manual/local-only",
                "manual tooling index and runbook remain the current reviewer entry points",
                "Future app-service expansion must be separately scoped",
                "LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md",
                "LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md",
                "LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md",
                "LOCAL_LAB_MANUAL_TOOLING_INDEX.md",
                "LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md")) {
            assertTrue(design.contains(expected), "app service design should include " + expected);
        }
    }

    @Test
    void appServiceBoundaryDesignDocumentsFuturePrerequisitesAndStopConditions() throws Exception {
        String design = read(APP_SERVICE_DESIGN);

        for (String expected : List.of(
                "explicit local-lab-only service naming",
                "documented startup command and health/readiness expectation",
                "documented dependency on an existing packaged artifact only if safe and reviewed",
                "documented stop conditions",
                "it changes `src/main/java`",
                "it changes production routing/scoring/proxy/API behavior",
                "it changes production Docker packaging unexpectedly",
                "it exposes ports on `0.0.0.0` by default",
                "it points to production/cloud/tenant/private-network targets",
                "it introduces secrets/credentials",
                "it wires Compose into CI without separate approval",
                "it wires Compose into Maven without separate approval")) {
            assertTrue(design.contains(expected), "app service design should include stop condition " + expected);
        }
    }

    @Test
    void appServiceBoundaryDesignStatesEvidenceAndValidationBoundaries() throws Exception {
        String normalized = read(APP_SERVICE_DESIGN).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no throughput evidence",
                "no p95/p99 evidence",
                "no load/stress/benchmark evidence",
                "no production readiness/certification",
                "no live-cloud or real-tenant validation",
                "no runtime enforcement",
                "no replay execution, evidence/report generation, storage, or export behavior")) {
            assertTrue(normalized.contains(expected), "app service design should state " + expected);
        }
    }

    @Test
    void appServiceBoundaryDesignListsRemainingNotProvenBoundaries() throws Exception {
        String design = read(APP_SERVICE_DESIGN);

        for (String expected : List.of(
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "Docker/k6/Bruno/Toxiproxy platform implementation beyond optional local-lab skeleton",
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
            assertTrue(design.contains(expected), "app service design should list " + expected);
        }
    }

    @Test
    void existingLocalLabDocsCrossLinkBackToAppServiceBoundaryDesign() throws Exception {
        for (Path doc : List.of(COMPOSE_MANUAL_RUNBOOK, COMPOSE_SKELETON, COMPOSE_BOUNDARY, INDEX, TOOLING_RUNBOOK,
                TOXIPROXY_DOC, K6_DOC, BRUNO_DOC, BOUNDARY_PLAN, HANDOFF, READINESS, NEXT_STEPS, MATRIX, TRUST_MAP,
                ADR_0009)) {
            assertTrue(read(doc).contains("LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md"),
                    doc + " should cross-link app service boundary design");
        }
    }

    @Test
    void composeFileContainsGatedAppServiceWithoutRunnerServices() throws Exception {
        String compose = read(COMPOSE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "app-under-test",
                "local-lab-only",
                "manual-only",
                "127.0.0.1:8080:8080",
                "127.0.0.1:8474:8474",
                "127.0.0.1:18080:18080",
                "127.0.0.1:18081:18081",
                "../toxiproxy/local-lab-toxiproxy.json")) {
            assertTrue(compose.contains(expected), "Compose skeleton should still contain " + expected);
        }

        for (String forbidden : List.of(
                "0.0.0.0",
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
    void appServiceBoundaryDocsAvoidProductionAndEvidenceOverclaims() throws Exception {
        for (Path doc : List.of(APP_SERVICE_DESIGN, APP_SERVICE_SKELETON, COMPOSE_MANUAL_RUNBOOK, COMPOSE_SKELETON,
                COMPOSE_BOUNDARY, INDEX, TOOLING_RUNBOOK, TOXIPROXY_DOC, K6_DOC, BRUNO_DOC, BOUNDARY_PLAN, HANDOFF,
                READINESS, NEXT_STEPS, MATRIX, TRUST_MAP, ADR_0009)) {
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
                    "app-under-test service is operational",
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
