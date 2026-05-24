package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabPostAppServiceComposeHandoffDocumentationTest {
    private static final Path COMPOSE = Path.of("lab/docker-compose/local-lab-compose.yml");
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path IMPLEMENTATION_READINESS =
            Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path APP_SERVICE_SKELETON =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md");
    private static final Path COMPOSE_READINESS_GATE =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md");
    private static final Path APP_SERVICE_PREFLIGHT =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md");
    private static final Path COMPOSE_MANUAL_RUNBOOK =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md");
    private static final Path SCENARIO_MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path SELF = Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/"
            + "LocalLabPostAppServiceComposeHandoffDocumentationTest.java");

    private static final List<Path> POST_APP_SERVICE_HANDOFF_DOCS = List.of(
            HANDOFF,
            NEXT_STEPS,
            IMPLEMENTATION_READINESS,
            APP_SERVICE_SKELETON,
            COMPOSE_READINESS_GATE,
            APP_SERVICE_PREFLIGHT,
            COMPOSE_MANUAL_RUNBOOK,
            SCENARIO_MATRIX,
            TRUST_MAP,
            ADR_0009);

    @Test
    void composeFileStillContainsOnlyTheCurrentLocalLabAppAndToxiproxyServices() throws Exception {
        String compose = read(COMPOSE);
        String normalized = compose.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "app-under-test:",
                "toxiproxy:",
                "eclipse-temurin:21-jre",
                "../../target:/opt/loadbalancerpro:ro",
                "127.0.0.1:8080:8080")) {
            assertTrue(normalized.contains(expected), "Compose file should preserve " + expected);
        }

        for (String forbidden : List.of(
                "0.0.0.0",
                "http://",
                "https://",
                "amazonaws",
                "azure",
                "gcp",
                "tenant",
                "prod.",
                "production.",
                "password",
                "secret",
                "credential",
                "api_key",
                "apikey",
                "token",
                "k6",
                "bruno")) {
            assertFalse(normalized.contains(forbidden), "Compose file must not contain " + forbidden);
        }
    }

    @Test
    void handoffDocsStateThePostAppServiceComposeBaseline() throws Exception {
        for (Path doc : POST_APP_SERVICE_HANDOFF_DOCS) {
            String normalized = read(doc).toLowerCase(Locale.ROOT);

            for (String expected : List.of(
                    "post-app-service compose handoff update",
                    "app-under-test` service now exists in local-lab compose",
                    "optional/manual/local-lab-only",
                    "local `target/` mount read-only",
                    "manual package step",
                    "published app port is loopback-bound",
                    "127.0.0.1:8080:8080",
                    "existing toxiproxy service remains present",
                    "k6 remains manual and separate",
                    "bruno remains manual and separate",
                    "no k6 runner service exists",
                    "no bruno runner service exists",
                    "no ci-gating",
                    "no maven wiring",
                    "no dockerfile change",
                    "no production docker packaging",
                    "no production compose change",
                    "no production runtime behavior change")) {
                assertTrue(normalized.contains(expected), doc + " should state " + expected);
            }
        }
    }

    @Test
    void handoffDocsPreserveTheNotProvenBoundaries() throws Exception {
        for (Path doc : POST_APP_SERVICE_HANDOFF_DOCS) {
            String normalized = read(doc).toLowerCase(Locale.ROOT);

            for (String expected : List.of(
                    "no production readiness/certification claim",
                    "no live-cloud or real-tenant validation claim",
                    "no runtime enforcement claim",
                    "no replay/evidence/report/storage/export behavior claim",
                    "no load/stress/benchmark claim",
                    "no throughput/p95/p99 evidence claim")) {
                assertTrue(normalized.contains(expected), doc + " should preserve " + expected);
            }
        }
    }

    @Test
    void handoffDocsDocumentTheNextSafeExpansionLanes() throws Exception {
        for (Path doc : POST_APP_SERVICE_HANDOFF_DOCS) {
            String normalized = read(doc).toLowerCase(Locale.ROOT);

            for (String expected : List.of(
                    "app-service manual smoke checklist docs",
                    "compose manual runbook update for app service",
                    "app-service health/readiness documentation only",
                    "future k6/bruno runner design docs only",
                    "no runner services until separate gates are created")) {
                assertTrue(normalized.contains(expected), doc + " should list next lane " + expected);
            }
        }
    }

    @Test
    void handoffDocsCrossLinkTheComposeGateAndAppServiceSkeletonDocs() throws Exception {
        List<String> composeGuardrailLinks = List.of(
                "LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md",
                "LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md",
                "LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md");

        for (Path doc : POST_APP_SERVICE_HANDOFF_DOCS) {
            String content = read(doc);
            String fileName = doc.getFileName().toString();

            for (String expected : composeGuardrailLinks) {
                if (expected.equals(fileName)) {
                    continue;
                }
                assertTrue(content.contains(expected), doc + " should cross-link " + expected);
            }
        }
    }

    @Test
    void relatedDocsAvoidUnsupportedPostHandoffClaims() throws Exception {
        for (Path doc : POST_APP_SERVICE_HANDOFF_DOCS) {
            String normalized = read(doc).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
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
