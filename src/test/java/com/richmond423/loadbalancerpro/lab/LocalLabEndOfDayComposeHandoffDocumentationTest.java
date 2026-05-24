package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabEndOfDayComposeHandoffDocumentationTest {
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path IMPLEMENTATION_READINESS =
            Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path COMPOSE_READINESS =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md");
    private static final Path APP_SERVICE_PREFLIGHT =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md");
    private static final Path COMPOSE_MANUAL_RUNBOOK =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path SELF = Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/"
            + "LocalLabEndOfDayComposeHandoffDocumentationTest.java");

    @Test
    void handoffDocsSummarizeTodayMergedLocalLabSequence() throws Exception {
        String combined = read(HANDOFF) + "\n" + read(NEXT_STEPS);

        for (String expected : List.of(
                "today's merged local-lab",
                "PR #270",
                "PR #272",
                "PR #273",
                "PR #274",
                "PR #275",
                "PR #276",
                "PR #277",
                "PR #278",
                "PR #279",
                "PR #280",
                "PR #281",
                "PR #282",
                "Docker Compose boundary design",
                "Docker Compose manual runbook",
                "Compose app-service boundary design",
                "Compose readiness gate",
                "Compose app-service preflight checklist")) {
            assertTrue(combined.contains(expected), "handoff should mention " + expected);
        }
    }

    @Test
    void handoffDocsStateCurrentComposeToolingStatus() throws Exception {
        String combined = read(HANDOFF) + "\n" + read(NEXT_STEPS);

        for (String expected : List.of(
                "the k6 skeleton exists and remains optional/manual/local-only",
                "the Bruno collection exists and remains optional/manual/local-only",
                "the Toxiproxy config exists and remains optional/manual/local-only",
                "the Docker Compose skeleton exists and remains optional/manual/local-only",
                "Current Compose skeleton remains Toxiproxy-only",
                "No app service exists yet",
                "k6 remains manual and separate",
                "Bruno remains manual and separate",
                "Toxiproxy remains manual/local-only",
                "Compose is not CI-gated",
                "Compose is not Maven-wired",
                "no production Docker packaging has been added")) {
            assertTrue(combined.contains(expected), "handoff should preserve current status " + expected);
        }
    }

    @Test
    void handoffDocsPointNextSprintToReadinessGateAndPreflightChecklist() throws Exception {
        String combined = read(HANDOFF) + "\n" + read(NEXT_STEPS);

        for (String expected : List.of(
                "LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md",
                "LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md",
                "A future app-service PR must be separately scoped",
                "not current proof")) {
            assertTrue(combined.contains(expected), "handoff should point next sprint to " + expected);
        }
    }

    @Test
    void handoffDocsListRequiredNextSprintStopConditions() throws Exception {
        String combined = read(HANDOFF) + "\n" + read(NEXT_STEPS);

        for (String expected : List.of(
                "stop if `src/main/java` changes appear",
                "stop if production API/routing behavior changes appear",
                "stop if Dockerfile or production Compose changes appear unexpectedly",
                "stop if CI/Maven wiring appears",
                "stop if external/cloud/tenant/private-network targets appear",
                "stop if secrets/credentials appear",
                "stop if `0.0.0.0` default exposure appears",
                "stop if performance/readiness/certification/runtime-enforcement claims appear",
                "stop if replay/evidence/report/storage/export behavior appears")) {
            assertTrue(combined.contains(expected), "handoff should include stop condition " + expected);
        }
    }

    @Test
    void handoffDocsPreserveRemainingNotProvenBoundaries() throws Exception {
        String combined = read(HANDOFF) + "\n" + read(NEXT_STEPS);

        for (String expected : List.of(
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "broader Docker/k6/Bruno/Toxiproxy platform implementation beyond optional local-lab skeletons",
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
            assertTrue(combined.contains(expected), "handoff should preserve not-proven boundary " + expected);
        }
    }

    @Test
    void relatedDocsCrossLinkTheEndOfDayComposeHandoff() throws Exception {
        for (Path doc : List.of(IMPLEMENTATION_READINESS, MATRIX, TRUST_MAP, COMPOSE_READINESS,
                APP_SERVICE_PREFLIGHT, COMPOSE_MANUAL_RUNBOOK, ADR_0009)) {
            String text = read(doc);

            assertTrue(text.contains("LOCAL_LAB_PROGRESS_HANDOFF.md"), doc + " should link progress handoff");
            assertTrue(text.contains("LOCAL_LAB_NEXT_STEPS_BOUNDARY.md"), doc + " should link next steps");
            assertTrue(text.toLowerCase(Locale.ROOT).contains("end-of-day compose handoff"),
                    doc + " should name the end-of-day Compose handoff");
        }
    }

    @Test
    void handoffDocsAvoidProductionAndEvidenceOverclaims() throws Exception {
        for (Path doc : List.of(HANDOFF, NEXT_STEPS, IMPLEMENTATION_READINESS, MATRIX, TRUST_MAP,
                COMPOSE_READINESS, APP_SERVICE_PREFLIGHT, COMPOSE_MANUAL_RUNBOOK, ADR_0009)) {
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
