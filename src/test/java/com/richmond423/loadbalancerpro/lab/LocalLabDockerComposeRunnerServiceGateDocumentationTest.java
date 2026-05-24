package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabDockerComposeRunnerServiceGateDocumentationTest {
    private static final Path RUNNER_GATE =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md");
    private static final Path COMPOSE = Path.of("lab/docker-compose/local-lab-compose.yml");
    private static final Path APP_SERVICE_RUNBOOK =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md");
    private static final Path HEALTH_READINESS =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md");
    private static final Path MANUAL_SMOKE =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md");
    private static final Path COMPOSE_READINESS =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md");
    private static final Path COMPOSE_MANUAL_RUNBOOK =
            Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md");
    private static final Path K6_DOC = Path.of("docs/LOCAL_LAB_K6_SMOKE_SCRIPT.md");
    private static final Path BRUNO_DOC = Path.of("docs/LOCAL_LAB_BRUNO_COLLECTION.md");
    private static final Path TOOLING_INDEX = Path.of("docs/LOCAL_LAB_MANUAL_TOOLING_INDEX.md");
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path IMPLEMENTATION_READINESS =
            Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path SELF = Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/"
            + "LocalLabDockerComposeRunnerServiceGateDocumentationTest.java");

    @Test
    void runnerServiceGateExistsAndStatesDocsOnlyScope() throws Exception {
        String normalized = read(RUNNER_GATE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "documentation only",
                "future gate for runner services",
                "no k6 runner service is added",
                "no bruno runner service is added",
                "no compose behavior changes",
                "no app behavior changes",
                "no endpoint changes",
                "no ci-gating",
                "no maven wiring",
                "no dockerfile change",
                "no production docker packaging",
                "no production compose change")) {
            assertTrue(normalized.contains(expected), "runner gate should state " + expected);
        }
    }

    @Test
    void runnerServiceGateStatesCurrentReviewerPathAndToolBoundaries() throws Exception {
        String gate = read(RUNNER_GATE);
        String normalized = gate.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md",
                "LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md",
                "LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md",
                "app-service runbook remains the reviewer path",
                "k6 remains manual and separate",
                "Bruno remains manual and separate",
                "app-under-test",
                "Toxiproxy")) {
            assertTrue(gate.contains(expected), "runner gate should reference " + expected);
        }

        for (String expected : List.of(
                "no runner service is added",
                "no new compose services are added",
                "no production runtime behavior change")) {
            assertTrue(normalized.contains(expected), "runner gate should preserve " + expected);
        }
    }

    @Test
    void runnerServiceGateRequiresPreflightProofForFutureRunnerServices() throws Exception {
        String normalized = read(RUNNER_GATE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "separate pr",
                "local-lab-only service name",
                "future runner services must stay local-lab-only and loopback/local-targeted",
                "future runner services must not target production/cloud/tenant/external endpoints",
                "no external endpoints",
                "no secrets",
                "future runner services must not introduce secrets/credentials",
                "no ci/maven wiring unless separately approved",
                "no automated execution unless separately approved",
                "no performance claims",
                "no production validation claims",
                "no automated artifact generation/storage/export",
                "explicit stop conditions")) {
            assertTrue(normalized.contains(expected), "runner gate should require " + expected);
        }
    }

    @Test
    void runnerServiceGateSeparatelyScopesK6AndBrunoRunnerPrs() throws Exception {
        String normalized = read(RUNNER_GATE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "any future k6 runner pr must be separately scoped",
                "any future bruno runner pr must be separately scoped",
                "keep k6 local-lab-only and loopback/local-targeted",
                "keep bruno local-lab-only and loopback/local-targeted",
                "k6 remains manual and separate until that future pr is reviewed",
                "bruno remains manual and separate until that future pr is reviewed")) {
            assertTrue(normalized.contains(expected), "runner gate should scope " + expected);
        }
    }

    @Test
    void runnerServiceGatePreservesUnsupportedClaimBoundaries() throws Exception {
        String normalized = read(RUNNER_GATE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "must not imply load/stress/benchmark evidence",
                "must not claim throughput/p95/p99 evidence",
                "must not claim production readiness/certification",
                "must not claim live-cloud or real-tenant validation",
                "must not claim runtime enforcement",
                "must not claim replay/evidence/report/storage/export behavior",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "replay execution",
                "evidence/report generation",
                "storage/export behavior",
                "load testing",
                "stress testing",
                "benchmarking",
                "throughput evidence",
                "p95 evidence",
                "p99 evidence")) {
            assertTrue(normalized.contains(expected), "runner gate should preserve " + expected);
        }
    }

    @Test
    void composeFileStillHasNoRunnerServicesOrBehaviorChange() throws Exception {
        String compose = read(COMPOSE);
        String normalized = compose.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "app-under-test:",
                "toxiproxy:",
                "127.0.0.1:8080:8080",
                "127.0.0.1:8474:8474",
                "127.0.0.1:18080:18080",
                "127.0.0.1:18081:18081",
                "../../target:/opt/loadbalancerpro:ro",
                "../toxiproxy/local-lab-toxiproxy.json")) {
            assertTrue(normalized.contains(expected), "Compose file should still contain " + expected);
        }

        assertTrue(allPublishedPortsBindToLoopback(compose), "all published ports should bind to 127.0.0.1");

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
            assertFalse(normalized.contains(forbidden), "Compose file should not contain " + forbidden);
        }
    }

    @Test
    void existingDocsCrossLinkBackToRunnerServiceGate() throws Exception {
        for (Path doc : List.of(APP_SERVICE_RUNBOOK, HEALTH_READINESS, MANUAL_SMOKE, COMPOSE_READINESS,
                COMPOSE_MANUAL_RUNBOOK, K6_DOC, BRUNO_DOC, TOOLING_INDEX, HANDOFF, NEXT_STEPS,
                IMPLEMENTATION_READINESS, TRUST_MAP, ADR_0009)) {
            assertTrue(read(doc).contains("LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md"),
                    doc + " should cross-link the runner-service gate");
        }
    }

    @Test
    void relatedDocsAvoidRunnerServiceAndProductionOverclaims() throws Exception {
        for (Path doc : List.of(RUNNER_GATE, APP_SERVICE_RUNBOOK, HEALTH_READINESS, MANUAL_SMOKE,
                COMPOSE_READINESS, COMPOSE_MANUAL_RUNBOOK, K6_DOC, BRUNO_DOC, TOOLING_INDEX, HANDOFF,
                NEXT_STEPS, IMPLEMENTATION_READINESS, TRUST_MAP, ADR_0009)) {
            String normalized = read(doc).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "k6 runner service is operational",
                    "bruno runner service is operational",
                    "runner service is operational",
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
