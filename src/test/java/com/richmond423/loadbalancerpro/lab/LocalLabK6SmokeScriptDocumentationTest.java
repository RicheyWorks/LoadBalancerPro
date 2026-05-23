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

class LocalLabK6SmokeScriptDocumentationTest {
    private static final Path K6_SCRIPT = Path.of("lab/k6/local-lab-smoke.js");
    private static final Path K6_DOC = Path.of("docs/LOCAL_LAB_K6_SMOKE_SCRIPT.md");
    private static final Path BRUNO_DOC = Path.of("docs/LOCAL_LAB_BRUNO_COLLECTION.md");
    private static final Path K6_DESIGN = Path.of("docs/LOCAL_LAB_K6_SCENARIO_DESIGN.md");
    private static final Path BRUNO_DESIGN = Path.of("docs/LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md");
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
    void k6SmokeScriptExistsAndDefaultsToLoopback() throws Exception {
        String script = read(K6_SCRIPT);

        assertTrue(script.contains("DEFAULT_BASE_URL = 'http://127.0.0.1:8080'"));
        assertTrue(script.contains("__ENV.LOCAL_LAB_BASE_URL"));
        assertTrue(script.contains("vus: 1"));
        assertTrue(script.contains("iterations: 3"));
        assertTrue(script.contains("/actuator/health"));
        assertTrue(script.contains("status === 200"));
        assertTrue(script.contains("Boolean(r.body)"));
        assertTrue(script.contains("Local-lab smoke walkthrough only"));
        assertTrue(script.contains("not load, stress, or benchmark evidence"));
    }

    @Test
    void k6SmokeScriptDoesNotContainExternalUrlDefaults() throws Exception {
        String script = read(K6_SCRIPT);
        Matcher matcher = URL_PATTERN.matcher(script);
        int urls = 0;

        while (matcher.find()) {
            String url = matcher.group();
            urls++;
            assertTrue(isLoopback(url), "k6 script URL must be loopback-only: " + url);
        }

        assertTrue(urls > 0, "k6 script should declare an explicit loopback default URL");
    }

    @Test
    void k6SmokeScriptAvoidsValidationBenchmarkAndProductionClaims() throws Exception {
        String normalized = read(K6_SCRIPT).toLowerCase(Locale.ROOT);

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
                "autonomous production traffic shifting",
                "carbon-aware routing",
                "gpu orchestration",
                "power/grid control",
                "facility automation")) {
            assertFalse(normalized.contains(forbidden), "k6 script must not overclaim " + forbidden);
        }
    }

    @Test
    void k6SmokeDocStatesOptionalLocalOnlyManualBoundaries() throws Exception {
        String doc = read(K6_DOC);

        for (String expected : List.of(
                "optional local-lab k6 smoke script skeleton",
                "targets an already-running local app or local-lab-owned endpoint only",
                "It is not CI-gated.",
                "It is not Dockerized.",
                "It is not a benchmark.",
                "It is not a stress test.",
                "It is not load testing.",
                "does not prove throughput, p95, p99, production readiness",
                "default base URL is `http://127.0.0.1:8080`",
                "must not target external hosts by default",
                "LOCAL_LAB_BASE_URL",
                "only for local/lab-owned loopback endpoints",
                "Maven tests do not run this script",
                "CI does not run this script")) {
            assertTrue(doc.contains(expected), "k6 smoke doc should include " + expected);
        }
    }

    @Test
    void k6SmokeDocLinksDesignDocsAndPreservesBrunoToxiproxyBoundaries() throws Exception {
        String doc = read(K6_DOC);

        for (String expected : List.of(
                "LOCAL_LAB_K6_SCENARIO_DESIGN.md",
                "LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md",
                "LOCAL_LAB_BRUNO_COLLECTION.md",
                "LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md",
                "The k6 script does not run Bruno, and the Bruno collection does not run k6.",
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
            assertTrue(doc.contains(expected), "k6 smoke doc should preserve boundary " + expected);
        }
    }

    @Test
    void coreDocsCrossLinkTheK6SmokeScriptAndDesignDocs() throws Exception {
        String k6Design = read(K6_DESIGN);
        assertTrue(k6Design.contains("LOCAL_LAB_K6_SMOKE_SCRIPT.md"));
        assertTrue(k6Design.contains("LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md"));
        assertTrue(k6Design.contains("LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md"));
        assertTrue(k6Design.contains("lab/k6/local-lab-smoke.js"));
        assertTrue(k6Design.contains("optional local-lab k6 smoke script skeleton"));
        assertTrue(k6Design.contains("not CI-gated"));
        assertTrue(k6Design.contains("not Dockerized"));
        assertTrue(k6Design.contains("local/lab-owned loopback endpoints"));

        for (Path doc : List.of(BOUNDARY_PLAN, HANDOFF, NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String text = read(doc);

            assertTrue(text.contains("LOCAL_LAB_K6_SMOKE_SCRIPT.md"), doc + " should link k6 smoke doc");
            assertTrue(text.contains("LOCAL_LAB_BRUNO_COLLECTION.md"), doc + " should link Bruno skeleton doc");
            assertTrue(text.contains("LOCAL_LAB_K6_SCENARIO_DESIGN.md"), doc + " should link k6 design");
            assertTrue(text.contains("LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md"), doc + " should link Bruno design");
            assertTrue(text.contains("LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md"),
                    doc + " should link Toxiproxy design");
            assertTrue(text.contains("lab/k6/local-lab-smoke.js"), doc + " should name k6 smoke script path");
            assertTrue(text.contains("optional local-lab k6 smoke script skeleton"),
                    doc + " should describe the narrow k6 script scope");
            assertTrue(text.contains("not CI-gated"), doc + " should preserve manual-only boundary");
            assertTrue(text.contains("not Dockerized"), doc + " should preserve no-Docker boundary");
            assertTrue(text.contains("local/lab-owned loopback endpoints"),
                    doc + " should require local loopback ownership");
        }

        assertTrue(read(BRUNO_DOC).contains("optional local-lab Bruno collection skeleton"));
        assertTrue(read(BRUNO_DESIGN).contains("one optional local-lab Bruno collection skeleton"));
        assertTrue(read(TOXIPROXY_DESIGN).contains("No Toxiproxy config is added in this PR."));
    }

    @Test
    void k6SmokeDocsAvoidProductionAndEvidenceOverclaims() throws Exception {
        for (Path doc : List.of(K6_DOC, K6_DESIGN, BOUNDARY_PLAN, HANDOFF, NEXT_STEPS, READINESS, MATRIX,
                TRUST_MAP, ADR_0009)) {
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
