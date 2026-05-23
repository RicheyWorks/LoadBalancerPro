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
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class LocalLabBrunoCollectionDocumentationTest {
    private static final Path BRUNO_COLLECTION = Path.of("lab/bruno/local-lab-smoke");
    private static final Path BRUNO_JSON = BRUNO_COLLECTION.resolve("bruno.json");
    private static final Path BRUNO_ENV = BRUNO_COLLECTION.resolve("environments/local.bru");
    private static final Path API_HEALTH_REQUEST = BRUNO_COLLECTION.resolve("requests/api-health.bru");
    private static final Path ACTUATOR_HEALTH_REQUEST = BRUNO_COLLECTION.resolve("requests/actuator-health.bru");
    private static final Path LAB_SCENARIOS_REQUEST = BRUNO_COLLECTION.resolve("requests/local-lab-scenarios.bru");
    private static final Path BRUNO_DOC = Path.of("docs/LOCAL_LAB_BRUNO_COLLECTION.md");
    private static final Path BRUNO_DESIGN = Path.of("docs/LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md");
    private static final Path K6_DOC = Path.of("docs/LOCAL_LAB_K6_SMOKE_SCRIPT.md");
    private static final Path K6_DESIGN = Path.of("docs/LOCAL_LAB_K6_SCENARIO_DESIGN.md");
    private static final Path TOXIPROXY_DESIGN = Path.of("docs/LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md");
    private static final Path BOUNDARY_PLAN = Path.of("docs/LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md");
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path READINESS = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\"')]+");

    @Test
    void brunoCollectionExistsAndDefaultsBaseUrlToLoopback() throws Exception {
        assertTrue(Files.isDirectory(BRUNO_COLLECTION), "Bruno collection folder should exist");
        assertTrue(read(BRUNO_JSON).contains("\"name\": \"Local Lab Smoke\""));
        assertTrue(read(BRUNO_ENV).contains("baseUrl: http://127.0.0.1:8080"));

        for (Path request : List.of(API_HEALTH_REQUEST, ACTUATOR_HEALTH_REQUEST, LAB_SCENARIOS_REQUEST)) {
            String text = read(request);

            assertTrue(text.contains("type: http"), request + " should be an HTTP request");
            assertTrue(text.contains("url: {{baseUrl}}/"), request + " should use baseUrl");
            assertTrue(text.contains("body: none"), request + " should stay tiny and read-only");
            assertTrue(text.contains("auth: none"), request + " should avoid credential defaults");
        }

        assertTrue(read(API_HEALTH_REQUEST).contains("/api/health"));
        assertTrue(read(ACTUATOR_HEALTH_REQUEST).contains("/actuator/health"));
        assertTrue(read(LAB_SCENARIOS_REQUEST).contains("/api/lab/scenarios"));
    }

    @Test
    void brunoCollectionFilesDoNotContainExternalUrlDefaults() throws Exception {
        int urls = 0;

        for (Path file : collectionFiles()) {
            Matcher matcher = URL_PATTERN.matcher(read(file));
            while (matcher.find()) {
                String url = matcher.group();
                urls++;
                assertTrue(isLoopback(url), file + " URL must be loopback-only: " + url);
            }
        }

        assertTrue(urls > 0, "Bruno collection should declare an explicit loopback default URL");
    }

    @Test
    void brunoCollectionFilesAvoidBenchmarkAndProductionClaims() throws Exception {
        for (Path file : collectionFiles()) {
            String normalized = read(file).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "production-ready",
                    "production certified",
                    "production certification is proven",
                    "production validation is complete",
                    "live-cloud validated",
                    "real-tenant validated",
                    "throughput benchmark",
                    "p95",
                    "p99",
                    "stress test",
                    "load test",
                    "autonomous production traffic shifting",
                    "carbon-aware routing",
                    "gpu orchestration",
                    "power/grid control",
                    "facility automation")) {
                assertFalse(normalized.contains(forbidden), file + " must not overclaim " + forbidden);
            }
        }
    }

    @Test
    void brunoDocStatesOptionalLocalOnlyManualBoundaries() throws Exception {
        String doc = read(BRUNO_DOC);

        for (String expected : List.of(
                "optional local-lab Bruno collection skeleton",
                "targets an already-running local app or local-lab-owned endpoint only",
                "optional and manual",
                "It is not CI-gated.",
                "It is not Dockerized.",
                "It is not Toxiproxy integration.",
                "It is not k6 execution.",
                "It is not a benchmark.",
                "It is not a stress test.",
                "It is not load testing.",
                "does not prove throughput, p95, p99, production readiness",
                "default `baseUrl` is `http://127.0.0.1:8080`",
                "must not target external hosts by default",
                "only for local/lab-owned loopback endpoints",
                "Maven tests do not run Bruno",
                "CI does not run Bruno")) {
            assertTrue(doc.contains(expected), "Bruno doc should include " + expected);
        }
    }

    @Test
    void brunoDocLinksDesignDocsK6DocAndPreservesToxiproxyBoundaries() throws Exception {
        String doc = read(BRUNO_DOC);

        for (String expected : List.of(
                "LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md",
                "LOCAL_LAB_K6_SMOKE_SCRIPT.md",
                "The Bruno collection does not run k6, and the k6 script does not run Bruno.",
                "LOCAL_LAB_K6_SCENARIO_DESIGN.md",
                "LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md",
                "no Toxiproxy config is added here",
                "does not add Docker",
                "Docker Compose",
                "CI workflow changes",
                "Maven dependency changes",
                "production endpoints",
                "production listeners",
                "production routing/scoring/strategy/proxy/API behavior",
                "replay execution",
                "evidence/report generation",
                "storage",
                "export")) {
            assertTrue(doc.contains(expected), "Bruno doc should preserve boundary " + expected);
        }
    }

    @Test
    void coreDocsCrossLinkTheBrunoSkeletonK6DocAndDesignDocs() throws Exception {
        String brunoDesign = read(BRUNO_DESIGN);
        assertTrue(brunoDesign.contains("LOCAL_LAB_BRUNO_COLLECTION.md"));
        assertTrue(brunoDesign.contains("LOCAL_LAB_K6_SMOKE_SCRIPT.md"));
        assertTrue(brunoDesign.contains("lab/bruno/local-lab-smoke"));
        assertTrue(brunoDesign.contains("optional local-lab Bruno collection skeleton"));
        assertTrue(brunoDesign.contains("not CI-gated"));
        assertTrue(brunoDesign.contains("not Dockerized"));
        assertTrue(brunoDesign.contains("not Toxiproxy integration"));
        assertTrue(brunoDesign.contains("not k6 execution"));
        assertTrue(brunoDesign.contains("local/lab-owned loopback endpoints"));

        for (Path doc : List.of(BOUNDARY_PLAN, HANDOFF, NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String text = read(doc);

            assertTrue(text.contains("LOCAL_LAB_BRUNO_COLLECTION.md"), doc + " should link Bruno skeleton doc");
            assertTrue(text.contains("LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md"), doc + " should link Bruno design");
            assertTrue(text.contains("LOCAL_LAB_K6_SMOKE_SCRIPT.md"), doc + " should link k6 smoke doc");
            assertTrue(text.contains("LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md"),
                    doc + " should link Toxiproxy design");
            assertTrue(text.contains("lab/bruno/local-lab-smoke"), doc + " should name Bruno collection path");
            assertTrue(text.contains("optional local-lab Bruno collection skeleton"),
                    doc + " should describe the narrow Bruno scope");
            assertTrue(text.contains("not CI-gated"), doc + " should preserve manual-only boundary");
            assertTrue(text.contains("not Dockerized"), doc + " should preserve no-Docker boundary");
            assertTrue(text.contains("not Toxiproxy integration"), doc + " should preserve no-Toxiproxy boundary");
            assertTrue(text.contains("not k6 execution"), doc + " should separate Bruno from k6");
            assertTrue(text.contains("local/lab-owned loopback endpoints"),
                    doc + " should require local loopback ownership");
        }

        assertTrue(read(K6_DOC).contains("LOCAL_LAB_BRUNO_COLLECTION.md"));
        assertTrue(read(K6_DESIGN).contains("LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md"));
        assertTrue(read(TOXIPROXY_DESIGN).contains("No Toxiproxy config is added in this PR."));
    }

    @Test
    void brunoDocsAvoidProductionAndEvidenceOverclaims() throws Exception {
        for (Path doc : List.of(BRUNO_DOC, BRUNO_DESIGN, K6_DOC, BOUNDARY_PLAN, HANDOFF, NEXT_STEPS,
                READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
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
                    "bruno collection is implemented",
                    "toxiproxy config is implemented",
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

    private static List<Path> collectionFiles() throws Exception {
        try (Stream<Path> files = Files.walk(BRUNO_COLLECTION)) {
            return files.filter(Files::isRegularFile).sorted().toList();
        }
    }

    private static boolean isLoopback(String url) {
        return url.startsWith("http://127.0.0.1")
                || url.startsWith("http://localhost")
                || url.startsWith("http://[::1]");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
