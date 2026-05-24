package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabDockerComposeAppServiceManualSmokeChecklistDocumentationTest {
    private static final Path CHECKLIST =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md");
    private static final Path COMPOSE = Path.of("lab/docker-compose/local-lab-compose.yml");
    private static final Path APP_SERVICE_SKELETON =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md");
    private static final Path COMPOSE_MANUAL_RUNBOOK =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md");
    private static final Path APP_SERVICE_PREFLIGHT =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md");
    private static final Path COMPOSE_READINESS_GATE =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md");
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path IMPLEMENTATION_READINESS =
            Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path SELF = Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/"
            + "LocalLabDockerComposeAppServiceManualSmokeChecklistDocumentationTest.java");

    @Test
    void checklistExistsAndReferencesCurrentComposeAppServiceDocs() throws Exception {
        String checklist = read(CHECKLIST);

        for (String expected : List.of(
                "manual smoke checklist only",
                "lab/docker-compose/local-lab-compose.yml",
                "LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md",
                "LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md",
                "LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md",
                "LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md")) {
            assertTrue(checklist.contains(expected), "checklist should reference " + expected);
        }
    }

    @Test
    void checklistStatesManualLocalOnlyAppServiceBoundaries() throws Exception {
        String normalized = read(CHECKLIST).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "optional",
                "manual-only",
                "local-lab-only",
                "manual maven package step",
                "app-under-test service already exists in local-lab compose",
                "target/` directory read-only",
                "127.0.0.1:8080:8080",
                "toxiproxy remains present",
                "k6 remains manual and separate",
                "bruno remains manual and separate",
                "no k6 runner service",
                "no bruno runner service",
                "no ci-gating",
                "no maven wiring",
                "no dockerfile change",
                "no production docker packaging",
                "no production compose change",
                "no production runtime behavior change")) {
            assertTrue(normalized.contains(expected), "checklist should state " + expected);
        }
    }

    @Test
    void checklistIncludesInspectionOnlyAndOptionalManualCommands() throws Exception {
        String normalized = read(CHECKLIST).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "inspection-only path",
                "inspect [`../lab/docker-compose/local-lab-compose.yml`",
                "confirm `app-under-test` is loopback-bound",
                "confirm no `0.0.0.0`",
                "confirm no secrets",
                "confirm no external, cloud, tenant, or production target",
                "confirm no ci/maven wiring",
                "optional manual smoke path",
                "mvn -q \"-dskiptests\" package",
                "docker compose -f lab/docker-compose/local-lab-compose.yml config",
                "docker compose -f lab/docker-compose/local-lab-compose.yml up",
                "docker compose -f lab/docker-compose/local-lab-compose.yml down",
                "not part of ci",
                "not part of maven lifecycle wiring")) {
            assertTrue(normalized.contains(expected), "checklist should include " + expected);
        }
    }

    @Test
    void checklistPreservesUnsupportedClaimBoundaries() throws Exception {
        String normalized = read(CHECKLIST).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no production readiness/certification claim",
                "no live-cloud or real-tenant validation claim",
                "no runtime enforcement claim",
                "no replay/evidence/report/storage/export behavior claim",
                "no load/stress/benchmark claim",
                "no throughput/p95/p99 evidence claim",
                "do not prove production readiness",
                "do not prove production certification",
                "do not prove live-cloud validation",
                "do not prove real-tenant validation",
                "do not prove production readiness",
                "load testing",
                "stress testing",
                "benchmarking",
                "throughput evidence",
                "p95 evidence",
                "p99 evidence")) {
            assertTrue(normalized.contains(expected), "checklist should preserve " + expected);
        }
    }

    @Test
    void composeFileStillContainsOnlyTheLocalLabAppAndToxiproxyServices() throws Exception {
        String compose = read(COMPOSE);
        String normalized = compose.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "app-under-test:",
                "toxiproxy:",
                "eclipse-temurin:21-jre",
                "../../target:/opt/loadbalancerpro:ro",
                "127.0.0.1:8080:8080",
                "127.0.0.1:8474:8474",
                "127.0.0.1:18080:18080",
                "127.0.0.1:18081:18081")) {
            assertTrue(normalized.contains(expected), "Compose file should preserve " + expected);
        }

        assertTrue(allPublishedPortsBindToLoopback(compose), "all published ports should bind to 127.0.0.1");

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
    void existingDocsCrossLinkBackToManualSmokeChecklist() throws Exception {
        for (Path doc : List.of(APP_SERVICE_SKELETON, COMPOSE_MANUAL_RUNBOOK, APP_SERVICE_PREFLIGHT,
                COMPOSE_READINESS_GATE, HANDOFF, NEXT_STEPS, IMPLEMENTATION_READINESS, TRUST_MAP, ADR_0009)) {
            assertTrue(read(doc).contains("LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md"),
                    doc + " should cross-link the manual smoke checklist");
        }
    }

    @Test
    void relatedDocsAvoidUnsupportedManualSmokeClaims() throws Exception {
        for (Path doc : List.of(CHECKLIST, APP_SERVICE_SKELETON, COMPOSE_MANUAL_RUNBOOK, APP_SERVICE_PREFLIGHT,
                COMPOSE_READINESS_GATE, HANDOFF, NEXT_STEPS, IMPLEMENTATION_READINESS, TRUST_MAP, ADR_0009)) {
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
