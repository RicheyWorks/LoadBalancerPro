package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabProgressHandoffDocumentationTest {
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path READINESS = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");

    @Test
    void handoffDocExistsAndMentionsCurrentLocalLabLayers() throws Exception {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "# Local Lab Progress Handoff",
                "passive scenario catalog",
                "passive response fixtures",
                "passive transcript fixtures",
                "transcript summaries",
                "passive reviewer checklist mapping",
                "implementation readiness gate",
                "in-memory fake backend handler",
                "loopback fake backend server",
                "lifecycle hardening",
                "multi-backend loopback harness",
                "transcript alignment",
                "deterministic loopback traffic smoke client",
                "in-memory smoke summary",
                "smoke reviewer checklist mapping")) {
            assertTrue(handoff.contains(expected), "handoff should mention " + expected);
        }
    }

    @Test
    void handoffDocExplainsWhatLayersProveAndDoNotProve() throws Exception {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "What Each Layer Proves",
                "What Each Layer Does Not Prove",
                "Local loopback smoke is not production proof.",
                "Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped.",
                "small deterministic traffic matrix tests",
                "k6/Bruno planning docs",
                "not production readiness",
                "not production certification",
                "not live-cloud validation",
                "not real-tenant validation",
                "not runtime enforcement",
                "not replay execution",
                "not evidence/report generation",
                "not storage/export")) {
            assertTrue(handoff.contains(expected), "handoff should include boundary " + expected);
        }
    }

    @Test
    void coreDocsLinkTheHandoffAndDescribeThisSprintAsBounded() throws Exception {
        for (Path doc : List.of(READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String text = read(doc);

            assertTrue(text.contains("LOCAL_LAB_PROGRESS_HANDOFF.md"), doc + " should link handoff doc");
            assertTrue(text.contains(
                    "This PR adds a test-scope traffic smoke reviewer checklist mapper and docs-only progress handoff only."));
            assertTrue(text.contains(
                    "The mapper turns existing in-memory loopback traffic smoke summaries into reviewer checklist entries."));
            assertTrue(text.contains("It does not call endpoints."));
            assertTrue(text.contains("It does not execute replay."));
            assertTrue(text.contains("It does not generate evidence reports."));
            assertTrue(text.contains("It does not write files."));
            assertTrue(text.contains("It does not persist storage."));
            assertTrue(text.contains("It does not export/download/upload/PDF/ZIP anything."));
            assertTrue(text.contains("Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped."));
            assertTrue(text.contains("Passing smoke checklist tests is not production proof."));
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
