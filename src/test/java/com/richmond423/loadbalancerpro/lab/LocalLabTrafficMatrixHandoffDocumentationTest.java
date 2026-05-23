package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabTrafficMatrixHandoffDocumentationTest {
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path READINESS = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");

    @Test
    void handoffDocsMentionTrafficMatrixLayerAndReviewerChecklistMapping() throws Exception {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "traffic matrix layer is now present",
                "traffic matrix reviewer checklist mapping is now present",
                "deterministic loopback-only coverage over required local-lab profiles",
                "stable fixture/boundary matching",
                "stable in-memory matrix summaries",
                "reviewer-friendly checklist language",
                "traffic matrix reviewer checklist mapping")) {
            assertTrue(handoff.contains(expected), "handoff should mention " + expected);
        }
    }

    @Test
    void coreDocsDescribeMatrixChecklistSprintAsBoundedTestScopeOnly() throws Exception {
        for (Path doc : List.of(HANDOFF, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String text = read(doc);

            assertTrue(text.contains(
                    "This PR adds a test-scope traffic matrix reviewer checklist mapper and handoff update only."));
            assertTrue(text.contains(
                    "The mapper turns existing in-memory traffic matrix summaries into reviewer checklist entries."));
            assertTrue(text.contains("It does not call endpoints."));
            assertTrue(text.contains("It does not execute replay."));
            assertTrue(text.contains("It does not generate evidence reports."));
            assertTrue(text.contains("It does not write files."));
            assertTrue(text.contains("It does not persist storage."));
            assertTrue(text.contains("It does not export/download/upload/PDF/ZIP anything."));
            assertTrue(text.contains("It is not load testing or stress testing."));
            assertTrue(text.contains("Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped."));
            assertTrue(text.contains("Passing matrix checklist tests is not production proof."));
        }
    }

    @Test
    void handoffDocsKeepToolingReplayReportStorageExportAndValidationBoundariesExplicit() throws Exception {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "not production readiness",
                "not production certification",
                "not live-cloud validation",
                "not real-tenant validation",
                "not runtime enforcement",
                "not Docker/k6/Bruno/Toxiproxy execution",
                "not replay execution",
                "not evidence/report generation",
                "not storage/export",
                "not load/stress testing",
                "docs-only k6/Bruno/Toxiproxy implementation plan",
                "test-scope bounded request burst smoke test")) {
            assertTrue(handoff.contains(expected), "handoff should preserve boundary " + expected);
        }
    }

    @Test
    void handoffDocsAvoidProductionValidationReplayReportStorageAndExportOverclaims() throws Exception {
        for (Path doc : List.of(HANDOFF, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
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
