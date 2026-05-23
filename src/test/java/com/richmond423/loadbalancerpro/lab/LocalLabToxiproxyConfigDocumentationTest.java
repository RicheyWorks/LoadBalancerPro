package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class LocalLabToxiproxyConfigDocumentationTest {
    private static final Path TOXIPROXY_CONFIG = Path.of("lab/toxiproxy/local-lab-toxiproxy.json");
    private static final Path TOXIPROXY_DOC = Path.of("docs/LOCAL_LAB_TOXIPROXY_CONFIG.md");
    private static final Path TOXIPROXY_DESIGN = Path.of("docs/LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md");
    private static final Path K6_DOC = Path.of("docs/LOCAL_LAB_K6_SMOKE_SCRIPT.md");
    private static final Path BRUNO_DOC = Path.of("docs/LOCAL_LAB_BRUNO_COLLECTION.md");
    private static final Path BOUNDARY_PLAN = Path.of("docs/LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md");
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path READINESS = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Pattern TOXIPROXY_ENDPOINT =
            Pattern.compile("\"(?:listen|upstream)\"\\s*:\\s*\"([^\"]+)\"");

    @Test
    void toxiproxyConfigExistsAndDefaultsToLoopbackOnly() throws Exception {
        String config = read(TOXIPROXY_CONFIG);

        assertTrue(config.trim().startsWith("["));
        assertTrue(config.contains("\"name\": \"local-lab-app-loopback\""));
        assertTrue(config.contains("\"name\": \"local-lab-backend-loopback\""));
        assertTrue(config.contains("\"listen\": \"127.0.0.1:18080\""));
        assertTrue(config.contains("\"upstream\": \"127.0.0.1:8080\""));
        assertTrue(config.contains("\"listen\": \"127.0.0.1:18081\""));
        assertTrue(config.contains("\"upstream\": \"127.0.0.1:18082\""));
        assertTrue(config.contains("\"toxics\": []"));

        Matcher matcher = TOXIPROXY_ENDPOINT.matcher(config);
        int endpoints = 0;
        while (matcher.find()) {
            endpoints++;
            String endpoint = matcher.group(1);
            assertTrue(isLoopbackEndpoint(endpoint), "Toxiproxy endpoint must be loopback-only: " + endpoint);
        }

        assertTrue(endpoints >= 4, "config should declare explicit loopback listen and upstream endpoints");
    }

    @Test
    void toxiproxyConfigDoesNotContainExternalTargetsOrProductionLookingHosts() throws Exception {
        String normalized = read(TOXIPROXY_CONFIG).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "0.0.0.0",
                "http://",
                "https://",
                ".com",
                ".net",
                ".org",
                ".io",
                "amazonaws",
                "azure",
                "googleapis",
                "cloudfront",
                "prod.",
                "production.",
                "tenant",
                "customer",
                "corp",
                "internal")) {
            assertFalse(normalized.contains(forbidden), "Toxiproxy config must not contain " + forbidden);
        }
    }

    @Test
    void toxiproxyConfigAvoidsValidationBenchmarkAndProductionClaims() throws Exception {
        String normalized = read(TOXIPROXY_CONFIG).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "production-ready",
                "production certified",
                "production certification is proven",
                "production validation is complete",
                "live-cloud validated",
                "real-tenant validated",
                "runtime enforcement",
                "throughput benchmark",
                "p95",
                "p99",
                "stress test",
                "load test",
                "benchmark",
                "autonomous production traffic shifting",
                "carbon-aware routing",
                "gpu orchestration",
                "power/grid control",
                "facility automation")) {
            assertFalse(normalized.contains(forbidden), "Toxiproxy config must not overclaim " + forbidden);
        }
    }

    @Test
    void toxiproxyDocStatesOptionalManualLocalOnlyBoundaries() throws Exception {
        String doc = read(TOXIPROXY_DOC);

        for (String expected : List.of(
                "optional local-lab Toxiproxy config skeleton",
                "local-lab-only tooling",
                "manual-only",
                "It is not CI-gated.",
                "It is not Dockerized.",
                "It is not Docker Compose orchestration.",
                "It is not wired into the application.",
                "It is not wired into Maven.",
                "It is not wired into k6 execution.",
                "It is not wired into Bruno execution.",
                "It does not start Toxiproxy.",
                "It does not start the application.",
                "It does not prove runtime enforcement.",
                "It is not a benchmark.",
                "It is not a stress test.",
                "It is not a load test.",
                "does not prove throughput, p95, p99, production readiness",
                "defaults to loopback-only listen and upstream addresses",
                "must not bind to `0.0.0.0`",
                "local/lab-owned and non-production",
                "Maven and CI")) {
            assertTrue(doc.contains(expected), "Toxiproxy doc should include " + expected);
        }
    }

    @Test
    void toxiproxyDocLinksDesignK6BrunoDocsAndKeepsToolsSeparate() throws Exception {
        String doc = read(TOXIPROXY_DOC);

        for (String expected : List.of(
                "LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md",
                "LOCAL_LAB_K6_SMOKE_SCRIPT.md",
                "LOCAL_LAB_BRUNO_COLLECTION.md",
                "LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md",
                "The Toxiproxy config does not run k6, and the k6 script does not run Toxiproxy.",
                "The Toxiproxy config does not run Bruno, and the Bruno collection does not run Toxiproxy.",
                "does not add Docker",
                "Docker Compose",
                "automatic execution",
                "CI workflow changes",
                "Maven dependency changes",
                "production endpoints",
                "production listeners",
                "production routing/scoring/strategy/proxy/API behavior",
                "replay execution",
                "evidence/report generation",
                "storage",
                "export")) {
            assertTrue(doc.contains(expected), "Toxiproxy doc should preserve boundary " + expected);
        }
    }

    @Test
    void coreDocsCrossLinkTheToxiproxySkeletonAndRelatedToolDocs() throws Exception {
        String design = read(TOXIPROXY_DESIGN);
        assertTrue(design.contains("LOCAL_LAB_TOXIPROXY_CONFIG.md"));
        assertTrue(design.contains("lab/toxiproxy/local-lab-toxiproxy.json"));
        assertTrue(design.contains("optional local-lab Toxiproxy config skeleton"));
        assertTrue(design.contains("manual-only"));
        assertTrue(design.contains("not CI-gated"));
        assertTrue(design.contains("not Dockerized"));
        assertTrue(design.contains("not Docker Compose orchestration"));
        assertTrue(design.contains("does not start Toxiproxy"));
        assertTrue(design.contains("does not start the application"));

        for (Path doc : List.of(BOUNDARY_PLAN, HANDOFF, NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String text = read(doc);

            assertTrue(text.contains("LOCAL_LAB_TOXIPROXY_CONFIG.md"), doc + " should link Toxiproxy config doc");
            assertTrue(text.contains("LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md"),
                    doc + " should link Toxiproxy design");
            assertTrue(text.contains("LOCAL_LAB_K6_SMOKE_SCRIPT.md"), doc + " should link k6 doc");
            assertTrue(text.contains("LOCAL_LAB_BRUNO_COLLECTION.md"), doc + " should link Bruno doc");
            assertTrue(text.contains("lab/toxiproxy/local-lab-toxiproxy.json"),
                    doc + " should name Toxiproxy config path");
            assertTrue(text.contains("optional local-lab Toxiproxy config skeleton"),
                    doc + " should describe the narrow Toxiproxy scope");
            assertTrue(text.contains("manual-only"), doc + " should preserve manual-only boundary");
            assertTrue(text.contains("not CI-gated"), doc + " should preserve no-CI boundary");
            assertTrue(text.contains("not Dockerized"), doc + " should preserve no-Docker boundary");
            assertTrue(text.contains("not Docker Compose orchestration"),
                    doc + " should preserve no-Compose boundary");
            assertTrue(text.contains("not wired into the application"),
                    doc + " should preserve app wiring boundary");
            assertTrue(text.contains("not wired into Maven"), doc + " should preserve Maven boundary");
            assertTrue(text.contains("not wired into k6 execution"), doc + " should separate k6");
            assertTrue(text.contains("not wired into Bruno execution"), doc + " should separate Bruno");
            assertTrue(text.contains("does not start Toxiproxy"), doc + " should preserve manual startup boundary");
            assertTrue(text.contains("does not start the application"),
                    doc + " should preserve app startup boundary");
            assertTrue(text.contains("local/lab-owned loopback endpoints"),
                    doc + " should require local loopback ownership");
        }

        assertTrue(read(K6_DOC).contains("LOCAL_LAB_TOXIPROXY_CONFIG.md"));
        assertTrue(read(BRUNO_DOC).contains("LOCAL_LAB_TOXIPROXY_CONFIG.md"));
    }

    @Test
    void toxiproxyDocsAvoidProductionAndEvidenceOverclaims() throws Exception {
        for (Path doc : List.of(TOXIPROXY_DOC, TOXIPROXY_DESIGN, K6_DOC, BRUNO_DOC, BOUNDARY_PLAN, HANDOFF,
                NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
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
                    "docker compose is implemented",
                    "k6 scenario is implemented",
                    "bruno collection is implemented",
                    "toxiproxy config is implemented",
                    "toxiproxy execution is implemented",
                    "replay execution is implemented",
                    "evidence report is generated",
                    "report generation is implemented",
                    "storage is implemented",
                    "export behavior is implemented",
                    "autonomous production traffic shifting is implemented",
                    "carbon-aware routing is implemented",
                    "gpu orchestration is implemented",
                    "power/grid control is implemented",
                    "facility automation is implemented")) {
                assertFalse(normalized.contains(forbidden), doc + " must not overclaim " + forbidden);
            }
        }
    }

    private static boolean isLoopbackEndpoint(String endpoint) {
        return endpoint.startsWith("127.0.0.1:")
                || endpoint.startsWith("localhost:");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
