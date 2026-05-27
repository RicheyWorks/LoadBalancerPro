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

class AgentDecisionExplorerPhase1FinalHandoffDocumentationTest {
    private static final Path HANDOFF =
            Path.of("docs/agent/DECISION_EXPLORER_PHASE1_FINAL_HANDOFF.md");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_PHASE1_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerPhase1FinalHandoffDocumentationTest.java");

    @Test
    void finalHandoffExistsAndListsPhase1MergeEvidence() throws IOException {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "# Decision Explorer Phase 1 Final Handoff",
                "Status: active / phase1-final-handoff.",
                "Classification: WARN / decision-explorer-phase1-handoff.",
                "Campaign slot: DX-P1-G09.",
                "Candidate closeout status: `WARN / pending DX-P1-G09 merge-health gate`",
                "final operator response records the actual DX-P1-G09 merge SHA",
                "Current main before DX-P1-G09 local edits: `968a38eb1b2f8d8c4acaff58ab6cde4f99d71740`")) {
            assertTrue(handoff.contains(expected), "handoff should include " + expected);
        }

        for (String expected : List.of(
                "#360",
                "codex/decision-explorer-phase1-architecture",
                "0fe9331a757973d93820bbae46b05ae53f8ba64a",
                "#361",
                "codex/decision-explorer-phase1-dto-skeleton",
                "fca765b897937cd20ee9955bfb7f9ba7a665a9be",
                "#362",
                "codex/decision-explorer-phase1-builder",
                "af351b043fbc3ff0ffff50d9c0f17a667f84b7af",
                "#363",
                "codex/decision-explorer-phase1-api",
                "20b9080d5c24ef3807e15a3ef8367a8ef1ae4915",
                "#364",
                "codex/decision-explorer-phase1-ui-first-pass",
                "818540b424dc92df0ec59de68e456d0ce080adbf",
                "#365",
                "codex/decision-explorer-phase1-ui-navigation",
                "66242b7911c123b1f20f2820249b7173a3ef575a",
                "#366",
                "codex/decision-explorer-phase1-docs-examples",
                "3d85730efc979373c2838e414c78c16df43656a9",
                "#367",
                "codex/decision-explorer-phase1-hardening",
                "968a38eb1b2f8d8c4acaff58ab6cde4f99d71740",
                "DX-P1-G09",
                "codex/decision-explorer-phase1-final-handoff")) {
            assertTrue(handoff.contains(expected), "handoff should list slot evidence " + expected);
        }
    }

    @Test
    void finalHandoffSummarizesImplementedBehaviorAndFilesChangedByArea() throws IOException {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "DecisionExplorerPayloadV1",
                "DecisionReadoutV1",
                "CandidateReadoutV1",
                "FactorContributionV1",
                "PolicyGateReadoutV1",
                "DecisionDiffReadoutV1",
                "EvidencePacketReadoutV1",
                "AgentStructuredOutputV1",
                "DecisionExplorerPayloadService",
                "POST /api/routing/decision-explorer",
                "/decision-explorer.html",
                "src/main/resources/static/decision-explorer.html",
                "DecisionExplorerPayloadServiceTest",
                "DecisionExplorerStaticPageTest",
                "RoutingOpenApiContractTest",
                "AgentDecisionExplorerPhase1FinalHandoffDocumentationTest")) {
            assertTrue(handoff.contains(expected), "handoff should summarize implemented artifact " + expected);
        }

        for (String expected : List.of(
                "accepts caller-provided routing comparison input",
                "preserves deterministic selected-first candidate ordering",
                "serves a static same-origin page",
                "keeps optional API keys in page memory only",
                "read-only",
                "simulation-only")) {
            assertTrue(handoff.contains(expected), "handoff should summarize behavior " + expected);
        }
    }

    @Test
    void finalHandoffRecordsVerificationAndMergeGate() throws IOException {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "Focused tests for the active slice",
                "Relevant Decision Explorer selector bundle",
                "mvn -q test",
                "mvn -q \"-DskipTests\" package",
                "mvn -B package",
                "git diff --check",
                "git diff --cached --check",
                "git diff --check origin/main...HEAD",
                ".\\scripts\\smoke\\enterprise-lab-workflow.ps1 -Package",
                "current-head PR Build/Test/Package/Smoke",
                "Analyze Java",
                "CodeQL",
                "Dependency Review",
                "post-merge main CI and CodeQL",
                "https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26501430347",
                "https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26501430345",
                "https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26501780145",
                "https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26501780148")) {
            assertTrue(handoff.contains(expected), "handoff should preserve verification item " + expected);
        }
    }

    @Test
    void finalHandoffIsLinkedFromReviewerSurfacesAndCampaignState() throws IOException {
        String readme = read(README);
        String trustMap = read(TRUST_MAP);
        String board = read(BOARD);
        String session = read(SESSION);

        for (String expected : List.of(
                "DECISION_EXPLORER_PHASE1_FINAL_HANDOFF.md",
                "Decision Explorer Phase 1 final handoff",
                "Decision Explorer Implementation Phase 2")) {
            assertTrue(readme.contains(expected), "README should link final handoff item " + expected);
            assertTrue(trustMap.contains(expected), "Reviewer Trust Map should link final handoff item " + expected);
        }

        for (String expected : List.of(
                "Status: active / phase1-final-handoff.",
                "Current PR slot: DX-P1-G09.",
                "Completed Phase 1 PRs: 8 / 9 planned.",
                "merged-main-green as PR #367",
                "968a38eb1b2f8d8c4acaff58ab6cde4f99d71740",
                "codex/decision-explorer-phase1-final-handoff",
                "active-local")) {
            assertTrue(board.contains(expected), "board should track final handoff item " + expected);
        }

        for (String expected : List.of(
                "Current PR slot: DX-P1-G09",
                "PR #367 merged as",
                "codex/decision-explorer-phase1-final-handoff",
                "DECISION_EXPLORER_PHASE1_FINAL_HANDOFF.md",
                "AgentDecisionExplorerPhase1FinalHandoffDocumentationTest")) {
            assertTrue(session.contains(expected), "session should track final handoff item " + expected);
        }
    }

    @Test
    void finalHandoffPreservesSafetyAndNotProvenBoundaries() throws IOException {
        String combined = (read(HANDOFF) + "\n" + read(README) + "\n" + read(TRUST_MAP)).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "additive",
                "read-only",
                "simulation-only",
                "does not shift traffic",
                "does not change production routing",
                "no production readiness",
                "no live-cloud validation",
                "no real-tenant validation",
                "no benchmark/load/stress",
                "throughput/p95/p99",
                "replay/export/storage",
                "evidence-packet",
                "autonomous production action",
                "broader automation",
                "no hidden side effects")) {
            assertTrue(combined.contains(expected), "handoff should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "production readiness is proven",
                "certified production",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "autonomous production action is enabled",
                "traffic shifting enabled",
                "replay export is implemented",
                "evidence packet generated")) {
            assertFalse(combined.contains(forbidden), "handoff must not overclaim " + forbidden);
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
