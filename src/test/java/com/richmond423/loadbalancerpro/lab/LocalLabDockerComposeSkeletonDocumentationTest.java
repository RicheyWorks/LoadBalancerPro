package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabDockerComposeSkeletonDocumentationTest {
    private static final Path COMPOSE = Path.of("lab/docker-compose/local-lab-compose.yml");
    private static final Path DOC = Path.of("docs/LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md");
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
            "src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabDockerComposeSkeletonDocumentationTest.java");

    @Test
    void composeSkeletonAndDocumentationExist() throws Exception {
        assertTrue(Files.exists(COMPOSE), "local-lab Compose skeleton should exist");
        assertTrue(Files.exists(DOC), "local-lab Compose skeleton doc should exist");
        assertTrue(read(DOC).contains("lab/docker-compose/local-lab-compose.yml"));
    }

    @Test
    void composeSkeletonIsLocalLabOnlyAndLoopbackBound() throws Exception {
        String compose = read(COMPOSE);
        String normalized = compose.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "local-lab-only",
                "manual-only",
                "not-ci-gated",
                "not-wired",
                "127.0.0.1:8474:8474",
                "127.0.0.1:18080:18080",
                "127.0.0.1:18081:18081",
                "../toxiproxy/local-lab-toxiproxy.json",
                ":ro")) {
            assertTrue(normalized.contains(expected), "Compose skeleton should include " + expected);
        }

        assertFalse(compose.contains("0.0.0.0"), "Compose skeleton must not bind to all interfaces");
        assertTrue(allPublishedPortsBindToLoopback(compose), "every published port should bind to 127.0.0.1");
    }

    @Test
    void composeSkeletonAvoidsExternalTargetsCredentialsAndRunnerServices() throws Exception {
        String normalized = read(COMPOSE).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "http://",
                "https://",
                "0.0.0.0",
                "amazonaws",
                "azure",
                "gcp",
                "cloud",
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
                "bruno",
                "app-under-test")) {
            assertFalse(normalized.contains(forbidden), "Compose skeleton must not contain " + forbidden);
        }
    }

    @Test
    void documentationStatesManualLocalNonProductionBoundaries() throws Exception {
        String normalized = read(DOC).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "optional",
                "manual-only",
                "local-lab-only",
                "not ci-gated",
                "not wired into maven",
                "not production runtime behavior",
                "not production docker packaging",
                "not wired into automated execution",
                "inspect the compose file without running docker",
                "loopback/local-only",
                "k6 smoke script remains a separate optional manual tool",
                "bruno collection remains a separate optional manual tool",
                "toxiproxy config remains a separate optional manual config")) {
            assertTrue(normalized.contains(expected), "Compose skeleton doc should include " + expected);
        }
    }

    @Test
    void documentationStatesEvidenceAndValidationBoundaries() throws Exception {
        String normalized = read(DOC).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no throughput",
                "p95",
                "p99",
                "not a benchmark/load/stress setup",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "replay execution",
                "evidence/report generation",
                "storage",
                "export behavior")) {
            assertTrue(normalized.contains(expected), "Compose skeleton doc should include boundary " + expected);
        }
    }

    @Test
    void existingLocalLabDocsCrossLinkBackToComposeSkeleton() throws Exception {
        for (Path doc : List.of(DESIGN, INDEX, RUNBOOK, K6_DOC, BRUNO_DOC, TOXIPROXY_DOC, BOUNDARY_PLAN, HANDOFF,
                NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            assertTrue(read(doc).contains("LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md"),
                    doc + " should cross-link the Compose skeleton doc");
        }
    }

    @Test
    void composeSkeletonDocsAvoidAutomationProductionAndEvidenceOverclaims() throws Exception {
        for (Path doc : List.of(DOC, DESIGN, INDEX, RUNBOOK, K6_DOC, BRUNO_DOC, TOXIPROXY_DOC, BOUNDARY_PLAN,
                HANDOFF, NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
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
                    "k6 scenario is implemented",
                    "bruno collection is implemented",
                    "toxiproxy execution is implemented",
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
