package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabDockerComposeAppServiceSkeletonDocumentationTest {
    private static final Path COMPOSE = Path.of("lab/docker-compose/local-lab-compose.yml");
    private static final Path DOC = Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md");
    private static final Path READINESS_GATE =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md");
    private static final Path PREFLIGHT =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md");
    private static final Path APP_SERVICE_DESIGN =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md");
    private static final Path COMPOSE_MANUAL_RUNBOOK =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md");
    private static final Path COMPOSE_SKELETON =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md");
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path IMPLEMENTATION_READINESS =
            Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path SELF = Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/"
            + "LocalLabDockerComposeAppServiceSkeletonDocumentationTest.java");

    @Test
    void composeFileContainsGatedAppServiceAndExistingToxiproxyService() throws Exception {
        String normalized = read(COMPOSE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "app-under-test:",
                "toxiproxy:",
                "eclipse-temurin:21-jre",
                "java",
                "/opt/loadbalancerpro/loadbalancerpro-2.5.0.jar",
                "../../target:/opt/loadbalancerpro:ro",
                "manual-package-first",
                "local-lab-only",
                "manual-only",
                "not-ci-gated",
                "not-wired")) {
            assertTrue(normalized.contains(expected), "Compose file should include " + expected);
        }
    }

    @Test
    void composeFilePublishesOnlyLoopbackPortsAndAvoidsUnsafeTargets() throws Exception {
        String compose = read(COMPOSE);
        String normalized = compose.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("127.0.0.1:8080:8080"), "app port should bind to loopback");
        assertTrue(allPublishedPortsBindToLoopback(compose), "every published port should bind to 127.0.0.1");

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
                "-----begin",
                "k6",
                "bruno")) {
            assertFalse(normalized.contains(forbidden), "Compose file must not contain " + forbidden);
        }
    }

    @Test
    void appServiceDocumentationStatesManualLocalNonProductionBoundaries() throws Exception {
        String normalized = read(DOC).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "preflight result is pass",
                "optional",
                "manual-only",
                "local-lab-only",
                "not ci-gated",
                "not wired into maven",
                "not production docker packaging",
                "does not change production runtime behavior",
                "does not add a dockerfile",
                "does not build an image",
                "does not publish an image",
                "does not add k6 runner service behavior",
                "does not add bruno runner service behavior",
                "does not add automated execution",
                "user must manually package first",
                "read-only",
                "127.0.0.1:8080:8080")) {
            assertTrue(normalized.contains(expected), "app service doc should include " + expected);
        }
    }

    @Test
    void appServiceDocumentationPreservesEvidenceAndValidationBoundaries() throws Exception {
        String normalized = read(DOC).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not prove production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "replay execution",
                "evidence/report generation",
                "storage",
                "export behavior",
                "not load testing",
                "not stress testing",
                "not benchmarking",
                "no throughput evidence",
                "no p95 evidence",
                "no p99 evidence")) {
            assertTrue(normalized.contains(expected), "app service doc should preserve boundary " + expected);
        }
    }

    @Test
    void requiredDocsCrossLinkBackToAppServiceSkeleton() throws Exception {
        for (Path doc : List.of(READINESS_GATE, PREFLIGHT, APP_SERVICE_DESIGN, COMPOSE_MANUAL_RUNBOOK,
                COMPOSE_SKELETON, HANDOFF, NEXT_STEPS, IMPLEMENTATION_READINESS, TRUST_MAP, ADR_0009)) {
            assertTrue(read(doc).contains("LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md"),
                    doc + " should cross-link the app-service skeleton doc");
        }
    }

    @Test
    void relatedDocsAvoidUnsupportedClaims() throws Exception {
        for (Path doc : List.of(DOC, READINESS_GATE, PREFLIGHT, APP_SERVICE_DESIGN, COMPOSE_MANUAL_RUNBOOK,
                COMPOSE_SKELETON, HANDOFF, NEXT_STEPS, IMPLEMENTATION_READINESS, TRUST_MAP, ADR_0009)) {
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

    private static boolean allPublishedPortsBindToLoopback(String compose) {
        boolean inPorts = false;
        boolean sawPort = false;
        for (String line : compose.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.equals("ports:")) {
                inPorts = true;
                continue;
            }
            if (inPorts && line.startsWith("    ") && !line.startsWith("      ")) {
                inPorts = false;
            }
            if (inPorts && trimmed.startsWith("-")) {
                sawPort = true;
                if (!trimmed.startsWith("- \"127.0.0.1:")) {
                    return false;
                }
            }
        }
        return sawPort;
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
