package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabDockerComposeAppServicePreflightChecklistDocumentationTest {
    private static final Path PREFLIGHT =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md");
    private static final Path READINESS_GATE =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md");
    private static final Path APP_SERVICE_DESIGN =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md");
    private static final Path COMPOSE_MANUAL_RUNBOOK =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md");
    private static final Path COMPOSE_SKELETON =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md");
    private static final Path COMPOSE_BOUNDARY =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md");
    private static final Path INDEX = Path.of("docs/LOCAL_LAB_MANUAL_TOOLING_INDEX.md");
    private static final Path TOOLING_RUNBOOK =
            Path.of("docs/LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md");
    private static final Path TOXIPROXY_DOC = Path.of("docs/LOCAL_LAB_TOXIPROXY_CONFIG.md");
    private static final Path K6_DOC = Path.of("docs/LOCAL_LAB_K6_SMOKE_SCRIPT.md");
    private static final Path BRUNO_DOC = Path.of("docs/LOCAL_LAB_BRUNO_COLLECTION.md");
    private static final Path BOUNDARY_PLAN =
            Path.of("docs/LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md");
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path IMPLEMENTATION_READINESS =
            Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path SELF = Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/"
            + "LocalLabDockerComposeAppServicePreflightChecklistDocumentationTest.java");

    @Test
    void preflightChecklistExistsAndStatesThisSprintScope() throws Exception {
        String normalized = read(PREFLIGHT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "preflight checklist",
                "future app-service pr",
                "no app service is added",
                "no compose behavior changes",
                "no docker packaging changes",
                "no ci-gating",
                "no maven wiring",
                "does not change production runtime behavior")) {
            assertTrue(normalized.contains(expected), "preflight checklist should state " + expected);
        }
    }

    @Test
    void preflightChecklistDefinesRequiredAppServiceProof() throws Exception {
        String normalized = read(PREFLIGHT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "future app-service pr must be separately scoped",
                "local-lab-only service name",
                "local-only build/run story",
                "no production dockerfile mutation unless separately reviewed",
                "no production compose profile",
                "no registry push/pull requirement beyond public base images already accepted by repo policy",
                "documented startup command",
                "documented health/readiness expectation",
                "documented shutdown path",
                "documented relationship to the current toxiproxy-only compose skeleton",
                "documented stop conditions")) {
            assertTrue(normalized.contains(expected), "preflight checklist should require " + expected);
        }
    }

    @Test
    void preflightChecklistStatesLocalNetworkAndSecretBoundaries() throws Exception {
        String normalized = read(PREFLIGHT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "local-lab-only",
                "loopback-only ports",
                "no `0.0.0.0` default exposure",
                "no 0.0.0.0 default exposure",
                "no production/cloud/tenant/private-network/external targets",
                "no secrets/credentials",
                "ci/maven untouched",
                "production runtime untouched")) {
            assertTrue(normalized.contains(expected), "preflight checklist should preserve " + expected);
        }
    }

    @Test
    void preflightChecklistStatesCurrentToolingStatus() throws Exception {
        String checklist = read(PREFLIGHT);

        for (String expected : List.of(
                "Current Compose skeleton remains Toxiproxy-only",
                "No app service exists yet",
                "k6 remains manual and separate",
                "Bruno remains manual and separate",
                "Toxiproxy remains manual/local-only",
                "LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md",
                "LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md",
                "LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md",
                "LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md",
                "LOCAL_LAB_MANUAL_TOOLING_INDEX.md",
                "LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md")) {
            assertTrue(checklist.contains(expected), "preflight checklist should include " + expected);
        }
    }

    @Test
    void preflightChecklistStatesEvidenceAndValidationBoundaries() throws Exception {
        String normalized = read(PREFLIGHT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no throughput evidence",
                "p95/p99 evidence",
                "load/stress/benchmark evidence",
                "production readiness/certification claims avoided",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement claims avoided",
                "replay/evidence/report/storage/export claims avoided",
                "no replay execution, evidence/report generation, storage, or export behavior")) {
            assertTrue(normalized.contains(expected), "preflight checklist should state " + expected);
        }
    }

    @Test
    void preflightChecklistListsRemainingNotProvenBoundaries() throws Exception {
        String checklist = read(PREFLIGHT);

        for (String expected : List.of(
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "Docker/k6/Bruno/Toxiproxy platform implementation beyond optional local-lab skeletons",
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
                "facility automation",
                "broader automation")) {
            assertTrue(checklist.contains(expected), "preflight checklist should list " + expected);
        }
    }

    @Test
    void existingLocalLabDocsCrossLinkBackToPreflightChecklist() throws Exception {
        for (Path doc : List.of(READINESS_GATE, APP_SERVICE_DESIGN, COMPOSE_MANUAL_RUNBOOK, COMPOSE_SKELETON,
                COMPOSE_BOUNDARY, INDEX, TOOLING_RUNBOOK, TOXIPROXY_DOC, K6_DOC, BRUNO_DOC, BOUNDARY_PLAN,
                HANDOFF, IMPLEMENTATION_READINESS, NEXT_STEPS, MATRIX, TRUST_MAP, ADR_0009)) {
            assertTrue(read(doc).contains("LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md"),
                    doc + " should cross-link the app-service preflight checklist");
        }
    }

    @Test
    void preflightDocsAvoidProductionAndEvidenceOverclaims() throws Exception {
        for (Path doc : List.of(PREFLIGHT, READINESS_GATE, APP_SERVICE_DESIGN, COMPOSE_MANUAL_RUNBOOK,
                COMPOSE_SKELETON, COMPOSE_BOUNDARY, INDEX, TOOLING_RUNBOOK, TOXIPROXY_DOC, K6_DOC, BRUNO_DOC,
                BOUNDARY_PLAN, HANDOFF, IMPLEMENTATION_READINESS, NEXT_STEPS, MATRIX, TRUST_MAP, ADR_0009)) {
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
