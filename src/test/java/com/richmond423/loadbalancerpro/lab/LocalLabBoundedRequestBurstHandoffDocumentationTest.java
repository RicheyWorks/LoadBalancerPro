package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabBoundedRequestBurstHandoffDocumentationTest {
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path READINESS = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");

    @Test
    void handoffDocsMentionBoundedBurstLayerAndReviewerChecklistMapping() throws Exception {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "bounded burst layer is now present",
                "bounded burst reviewer checklist mapping is now present",
                "fixed small-count deterministic loopback-only request coverage",
                "stable fixture/boundary matching under repeated calls",
                "stable in-memory burst summaries",
                "reviewer-friendly checklist language",
                "bounded burst reviewer checklist mapping")) {
            assertTrue(handoff.contains(expected), "handoff should mention " + expected);
        }
    }

    @Test
    void coreDocsDescribeBoundedBurstChecklistSprintAsBoundedTestScopeOnly() throws Exception {
        for (Path doc : List.of(HANDOFF, NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String text = read(doc);

            assertTrue(text.contains(
                    "This PR adds a test-scope bounded burst reviewer checklist mapper and handoff update only."));
            assertTrue(text.contains(
                    "The mapper turns existing in-memory bounded request burst summaries into reviewer checklist entries."));
            assertTrue(text.contains("It does not call endpoints."));
            assertTrue(text.contains("It does not execute replay."));
            assertTrue(text.contains("It does not generate evidence reports."));
            assertTrue(text.contains("It does not write files."));
            assertTrue(text.contains("It does not persist storage."));
            assertTrue(text.contains("It does not export/download/upload/PDF/ZIP anything."));
            assertTrue(text.contains(
                    "It is not load testing, stress testing, benchmarking, throughput evidence, or p95/p99 evidence."));
            assertTrue(text.contains("Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped."));
            assertTrue(text.contains("Passing bounded burst checklist tests is not production proof."));
        }
    }

    @Test
    void handoffDocsKeepBurstLoadBenchmarkToolingReplayStorageExportAndValidationBoundariesExplicit()
            throws Exception {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "bounded burst tests are not production proof",
                "bounded burst tests are not load/stress/benchmark tests",
                "bounded burst tests do not provide throughput or p95/p99 evidence",
                "not production readiness",
                "not production certification",
                "not live-cloud validation",
                "not real-tenant validation",
                "not runtime enforcement",
                "not Docker/k6/Bruno/Toxiproxy execution",
                "not replay execution",
                "not evidence/report generation",
                "not storage/export",
                "docs-only k6/Bruno/Toxiproxy implementation plan",
                "test-scope fault-style fixture expansion",
                "docs-only Docker Compose design boundary")) {
            assertTrue(handoff.contains(expected), "handoff should preserve boundary " + expected);
        }
    }

    @Test
    void handoffDocsAvoidProductionValidationReplayReportStorageExportAndBenchmarkOverclaims()
            throws Exception {
        for (Path doc : List.of(HANDOFF, NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
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
                    "prometheus/grafana dashboard is implemented",
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
