package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabK6BrunoToxiproxyScenarioDesignDocumentationTest {
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

    @Test
    void scenarioDesignDocsExistAndStayDesignOnly() throws Exception {
        for (Path doc : List.of(K6_DESIGN, BRUNO_DESIGN, TOXIPROXY_DESIGN)) {
            String text = read(doc);

            assertTrue(text.contains("docs/test-only"), doc + " should stay docs/test-only");
            assertTrue(text.contains("No Docker Compose files are added in this PR."));
            assertTrue(text.contains("No actual tool execution occurs in this PR."));
            assertTrue(text.contains("Future tool work must be separately scoped."));
            assertTrue(text.contains("Future tool work must target local/lab endpoints first."));
        }
    }

    @Test
    void k6DesignNamesFutureScenarioShapesAndK6Boundaries() throws Exception {
        String text = read(K6_DESIGN);

        for (String expected : List.of(
                "healthy baseline smoke scenario",
                "slow/tail-latency scenario",
                "partial degradation scenario",
                "error-prone backend scenario",
                "queue-pressure/overloaded scenario",
                "all-unhealthy/no-good-choice scenario",
                "recovery scenario",
                "bounded burst scenario",
                "matrix scenario",
                "No k6 scripts are added in this PR.",
                "No load/stress/benchmarking is implemented.",
                "No throughput/p95/p99 evidence is produced.",
                "Future k6 scripts must target local lab endpoints first.",
                "Future k6 scripts must not target production.",
                "Future k6 work must be separately scoped.",
                "Future k6 results must be labeled local/test-scope unless separately validated.")) {
            assertTrue(text.contains(expected), "k6 design should include " + expected);
        }
    }

    @Test
    void brunoDesignNamesFutureCollectionShapesAndBrunoBoundaries() throws Exception {
        String text = read(BRUNO_DESIGN);

        for (String expected : List.of(
                "local health/readiness checks if applicable",
                "local routing compare checks if applicable",
                "local lab harness check patterns",
                "future operator/reviewer API checks",
                "negative/boundary checks",
                "collection naming and folder structure as design only",
                "No Bruno collection files are added in this PR.",
                "No live API or production API checks are added.",
                "No external calls are added.",
                "Future Bruno collection work must target local/lab endpoints first.",
                "Future Bruno work must be separately scoped.")) {
            assertTrue(text.contains(expected), "Bruno design should include " + expected);
        }
    }

    @Test
    void toxiproxyDesignNamesFutureFaultCategoriesAndToxiproxyBoundaries() throws Exception {
        String text = read(TOXIPROXY_DESIGN);

        for (String expected : List.of(
                "latency injection",
                "timeout behavior",
                "intermittent connection failure",
                "bandwidth/throughput constraint as a future concept only",
                "reset/close behavior",
                "recovery after fault removal",
                "partial degradation patterns",
                "No Toxiproxy config is added in this PR.",
                "No Docker Compose is added in this PR.",
                "No network damage is actually executed.",
                "No live network, LAN, or production network is touched.",
                "Future Toxiproxy work must be local/lab-only first.",
                "Future Toxiproxy work must be separately scoped.")) {
            assertTrue(text.contains(expected), "Toxiproxy design should include " + expected);
        }
    }

    @Test
    void scenarioDesignDocsKeepSharedReviewerBoundaries() throws Exception {
        for (Path doc : List.of(K6_DESIGN, BRUNO_DESIGN, TOXIPROXY_DESIGN)) {
            String text = read(doc);

            for (String expected : List.of(
                    "design-only",
                    "no production proof",
                    "no live-cloud proof",
                    "no real-tenant proof",
                    "no runtime enforcement proof",
                    "no replay execution",
                    "no evidence/report generation",
                    "no storage/export behavior",
                    "no production traffic",
                    "no autonomous traffic shifting",
                    "production readiness",
                    "production certification",
                    "live-cloud validation",
                    "real-tenant validation")) {
                assertTrue(text.contains(expected), doc + " should include shared boundary " + expected);
            }
        }
    }

    @Test
    void coreDocsCrossLinkScenarioDesignSpecs() throws Exception {
        for (Path doc : List.of(BOUNDARY_PLAN, HANDOFF, NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String text = read(doc);

            assertTrue(text.contains("LOCAL_LAB_K6_SCENARIO_DESIGN.md"), doc + " should link k6 design");
            assertTrue(text.contains("LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md"), doc + " should link Bruno design");
            assertTrue(text.contains("LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md"),
                    doc + " should link Toxiproxy design");
            assertTrue(text.contains("no k6 scripts"), doc + " should keep k6 future-only");
            assertTrue(text.contains("no Bruno collections"), doc + " should keep Bruno future-only");
            assertTrue(text.contains("no Toxiproxy config"), doc + " should keep Toxiproxy future-only");
            assertTrue(text.contains("no Docker Compose files"), doc + " should keep Compose future-only");
            assertTrue(text.contains("Future tool work must be separately scoped"),
                    doc + " should require separate scope");
            assertTrue(text.contains("must target local/lab endpoints first"),
                    doc + " should require local/lab endpoints first");
        }
    }

    @Test
    void scenarioDesignDocsAvoidImplementationAndValidationOverclaims() throws Exception {
        for (Path doc : List.of(K6_DESIGN, BRUNO_DESIGN, TOXIPROXY_DESIGN, BOUNDARY_PLAN, HANDOFF, NEXT_STEPS,
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

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
