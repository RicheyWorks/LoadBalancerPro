package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabManualToolingIndexDocumentationTest {
    private static final Path INDEX = Path.of("docs/LOCAL_LAB_MANUAL_TOOLING_INDEX.md");
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
    private static final Path SELF =
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabManualToolingIndexDocumentationTest.java");

    @Test
    void manualToolingIndexExistsAndReferencesAllCurrentSkeletons() throws Exception {
        String index = read(INDEX);

        for (String expected : List.of(
                "docs/test-only",
                "documentation-only index",
                "lab/k6/local-lab-smoke.js",
                "lab/bruno/local-lab-smoke/",
                "lab/toxiproxy/local-lab-toxiproxy.json",
                "LOCAL_LAB_K6_SMOKE_SCRIPT.md",
                "LOCAL_LAB_BRUNO_COLLECTION.md",
                "LOCAL_LAB_TOXIPROXY_CONFIG.md",
                "LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md",
                "LOCAL_LAB_K6_SCENARIO_DESIGN.md",
                "LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md",
                "LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md")) {
            assertTrue(index.contains(expected), "manual tooling index should include " + expected);
        }
    }

    @Test
    void manualToolingIndexStatesSharedExecutionBoundaries() throws Exception {
        String index = read(INDEX);

        for (String expected : List.of(
                "optional",
                "manual-only",
                "local-lab-only",
                "loopback/local defaults",
                "not CI-gated",
                "not Dockerized",
                "not Docker Compose orchestration",
                "not wired into Maven",
                "not wired into production runtime",
                "not production traffic automation",
                "127.0.0.1",
                "localhost",
                "no external endpoint",
                "actually running k6, Bruno, or Toxiproxy is not required")) {
            assertTrue(index.contains(expected), "manual tooling index should preserve boundary " + expected);
        }
    }

    @Test
    void manualToolingIndexStatesReviewerChecklistEvidenceBoundaries() throws Exception {
        String index = read(INDEX);

        for (String expected : List.of(
                "no throughput evidence",
                "no p95/p99 evidence",
                "no load/stress/benchmark evidence",
                "no production readiness/certification conclusion",
                "no live-cloud or real-tenant validation",
                "no runtime enforcement",
                "no replay execution, evidence/report generation, storage, or export behavior")) {
            assertTrue(index.contains(expected), "manual tooling index should include checklist boundary " + expected);
        }
    }

    @Test
    void manualToolingIndexListsRemainingNotProvenBoundaries() throws Exception {
        String index = read(INDEX);

        for (String expected : List.of(
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "Docker/k6/Bruno/Toxiproxy platform implementation",
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
                "facility automation")) {
            assertTrue(index.contains(expected), "manual tooling index should list remaining boundary " + expected);
        }
    }

    @Test
    void existingLocalLabDocsCrossLinkBackToTheManualToolingIndex() throws Exception {
        for (Path doc : List.of(K6_DOC, BRUNO_DOC, TOXIPROXY_DOC, BOUNDARY_PLAN, HANDOFF, NEXT_STEPS,
                READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            assertTrue(read(doc).contains("LOCAL_LAB_MANUAL_TOOLING_INDEX.md"),
                    doc + " should cross-link the manual tooling index");
        }
    }

    @Test
    void manualToolingDocsAvoidProductionAndEvidenceOverclaims() throws Exception {
        for (Path doc : List.of(INDEX, K6_DOC, BRUNO_DOC, TOXIPROXY_DOC, BOUNDARY_PLAN, HANDOFF, NEXT_STEPS,
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

    @Test
    void guardTestOnlyReadsTrackedDocumentationAndDoesNotRunTools() throws Exception {
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
