package com.richmond423.loadbalancerpro.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class AgentDecisionExplorerBootstrapCloseoutDocumentationTest {
    private static final Path CLOSEOUT = Path.of("docs/agent/DECISION_EXPLORER_BOOTSTRAP_CLOSEOUT.md");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md");
    private static final Path IMPLEMENTATION_PLAN =
            Path.of("docs/agent/DECISION_EXPLORER_IMPLEMENTATION_PLAN.md");
    private static final Path REVIEWER_WALKTHROUGH =
            Path.of("docs/agent/DECISION_EXPLORER_REVIEWER_WALKTHROUGH.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerBootstrapCloseoutDocumentationTest.java");

    @Test
    void bootstrapCloseoutExistsAndStatesCandidateScope() throws IOException {
        String closeout = read(CLOSEOUT);

        for (String expected : List.of(
                "# Decision Explorer Bootstrap Closeout",
                "Status: planned / docs-test-only.",
                "Classification: WARN / decision-explorer-bootstrap.",
                "Campaign slot: DX-G10.",
                "DECISION_EXPLORER_CAMPAIGN_BOARD.md",
                "ADR-0010-Interactive-Decision-Explorer-Architecture.md",
                "DECISION_EXPLORER_DATA_CONTRACT.md",
                "DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md",
                "DECISION_EXPLORER_EVIDENCE_LANE.md",
                "DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md",
                "DECISION_EXPLORER_IMPLEMENTATION_PLAN.md",
                "DECISION_EXPLORER_REVIEWER_WALKTHROUGH.md",
                "planned",
                "read-only",
                "simulation-only",
                "docs-test-only",
                "WARN / pending DX-G10 merge-health gate")) {
            assertTrue(closeout.contains(expected), "closeout should state scope item " + expected);
        }
    }

    @Test
    void bootstrapCloseoutRecordsPriorSlotEvidenceAndRepairContext() throws IOException {
        String closeout = read(CLOSEOUT);

        for (String expected : List.of(
                "#348",
                "#350",
                "#351",
                "#352",
                "#353",
                "#354",
                "#355",
                "#356",
                "#358",
                "#357",
                "3d3be5eaca13a49381ebdd28dcf4b9fa6b3eb056",
                "9548f1e5e4759836d19b1478a4a4b972cafb3d1d",
                "19b52952e6074ee043f1d651de5ed68b6bb4ac17",
                "ee8ca72b97fead1f7d03e3914089edbcc1804ff7",
                "2c36931ca6b2ab3d4845272e236eaf07551ee917",
                "cef4a980a56b83aa5e9d022a84925a7933b357d6",
                "d8dfa0194cfc8bfd2c0a05656f86494c64d3da05",
                "5a9175b3cfb1442848d1c772a94f467ffcf098f9",
                "1e2ac88266e16af4359234e39e685a06c7689455",
                "7bc69fc005261a91e7d4c2b198dd8b71879e4fc2",
                "e7204f9071de292c0902d1d8670c0ec14c9236ef",
                "2fba68f8bb5430046c303442dd154489f1d31506",
                "c449ebf9b7dc0062672d1eccf5f5ddfa90e9d725",
                "8dc32bfae658fd08042e1c9286a23275edc549f1",
                "dff8324d5aa979f4c27671feee006ed37b08b402",
                "695c952b626e8945b3a68580471fe75e11e0b5f6",
                "01e8148aea45dacf76f760e2b9df622cc8e4d3a7",
                "9f67687d9d8ed991f51c3ab6e83c3d8c55c4fccf",
                "340769ac90709c8aabe05bb48ba4cc9eda0db07a",
                "ae4a25ace932a6d5eadb7fa95d6a9c1d62d9eb70",
                "merged-main-green",
                "active-local / candidate closeout")) {
            assertTrue(closeout.contains(expected), "closeout should record evidence item " + expected);
        }
    }

    @Test
    void bootstrapCloseoutNamesWhatExistsNowAndWhatRemainsPlanned() throws IOException {
        String closeout = read(CLOSEOUT);

        for (String expected : List.of(
                "What Exists Now",
                "What Remains Planned",
                "DecisionExplorerPayloadV1",
                "AgentStructuredOutputV1",
                "stable identifiers",
                "JSON field naming rules",
                "enum stability",
                "null and unknown handling",
                "parseability rules",
                "source-card template",
                "research intake rules",
                "stale-information retirement policy",
                "repo bloat prevention",
                "compacting policy",
                "no raw research dumps",
                "evidence packet future path",
                "human reviewer understanding",
                "AI-agent structured understanding")) {
            assertTrue(closeout.contains(expected), "closeout should summarize artifact item " + expected);
        }
    }

    @Test
    void bootstrapCloseoutDefinesFinalGateAndFutureImplementationHandoff() throws IOException {
        String closeout = read(CLOSEOUT);

        for (String expected : List.of(
                "DX-G10 Final Merge-Health Gate",
                "workspace is clean",
                "current-head PR CI, CodeQL, and Dependency Review are green",
                "post-merge main CI and CodeQL are green",
                "Do not claim final bootstrap completion while DX-G10 PR checks or post-merge main checks are pending",
                "failed, cancelled, stale, skipped-only, or duplicate-only required checks",
                "Final Handoff To Implementation Phase 1",
                "Decision Explorer Implementation Phase 1",
                "fresh scoped contract",
                "implementation is future work",
                "separately scoped")) {
            assertTrue(closeout.contains(expected), "closeout should define final gate item " + expected);
        }
    }

    @Test
    void bootstrapCloseoutAlignsBoardAndImplementationPlan() throws IOException {
        String closeout = read(CLOSEOUT);
        String board = read(BOARD);
        String implementationPlan = read(IMPLEMENTATION_PLAN);
        String reviewerWalkthrough = read(REVIEWER_WALKTHROUGH);

        for (String expected : List.of(
                "DX-G10 | Bootstrap closeout",
                "codex/dx-g10-bootstrap-closeout",
                "AgentDecisionExplorerBootstrapCloseoutDocumentationTest",
                "Close bootstrap and prepare implementation campaign",
                "DECISION_EXPLORER_BOOTSTRAP_CLOSEOUT.md")) {
            assertTrue(board.contains(expected), "campaign board should align with closeout item " + expected);
        }

        for (String expected : List.of(
                "DX-G10 should close the bootstrap and preserve not-proven boundaries",
                "DECISION_EXPLORER_BOOTSTRAP_CLOSEOUT.md",
                "Decision Explorer Implementation Phase 1")) {
            assertTrue(implementationPlan.contains(expected),
                    "implementation plan should align with closeout item " + expected);
            assertTrue(reviewerWalkthrough.contains("DX-G10 should close the bootstrap"),
                    "reviewer walkthrough should still point to DX-G10 closeout");
            assertTrue(closeout.contains(expected), "closeout should include implementation handoff item " + expected);
        }
    }

    @Test
    void bootstrapCloseoutPreservesScopeSafetyAndNotProvenBoundaries() throws IOException {
        String normalized = read(CLOSEOUT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not add java classes",
                "does not prove",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "benchmark/load/stress evidence",
                "throughput/p95/p99 evidence",
                "replay/export behavior",
                "storage behavior",
                "runtime endpoint/ui/storage/evidence-packet implementation",
                "evidence packet implementation",
                "autonomous production action",
                "broader automation",
                "no runtime endpoint/ui/storage/export/replay behavior",
                "no hidden side effects",
                "maven configuration",
                "ci/workflow files",
                "dockerfile",
                "compose behavior",
                "scripts",
                "secrets",
                "external targets",
                "rulesets")) {
            assertTrue(normalized.contains(expected), "closeout should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "decision explorer is implemented",
                "decision explorer endpoint is implemented",
                "decision explorer ui is implemented",
                "decision explorer storage is implemented",
                "evidence packet is implemented",
                "replay export is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "autonomous production action is enabled")) {
            assertFalse(normalized.contains(forbidden), "closeout must not overclaim " + forbidden);
        }
    }

    @Test
    void guardTestOnlyReadsTrackedFiles() throws IOException {
        String source = read(SOURCE);

        for (String forbidden : List.of(
                "Files." + "write",
                "Files." + "create",
                "Files." + "delete",
                "Process" + "Builder",
                "Runtime." + "getRuntime",
                ".ex" + "ec(",
                "Http" + "Client",
                "URL" + "Connection",
                "Socket" + "(")) {
            assertFalse(source.contains(forbidden), "guard test must not use " + forbidden);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
