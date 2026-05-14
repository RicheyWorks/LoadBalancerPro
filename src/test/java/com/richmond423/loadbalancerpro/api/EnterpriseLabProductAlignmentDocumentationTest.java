package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class EnterpriseLabProductAlignmentDocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Path CHARTER = Path.of("docs/ENTERPRISE_LAB_PRODUCT_CHARTER.md");
    private static final Path ROADMAP = Path.of("docs/ENTERPRISE_LAB_ROADMAP.md");
    private static final Path POLICY_GATE = Path.of("docs/CONTROLLED_ACTIVE_LASE_POLICY_GATE.md");
    private static final Path NEXT_GOALS = Path.of("docs/NEXT_GOAL_PROMPTS.md");
    private static final Path PRODUCTION_SUMMARY = Path.of("docs/PRODUCTION_READINESS_SUMMARY.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SRE_HIGHLIGHTS = Path.of("docs/SRE_DEMO_HIGHLIGHTS.md");
    private static final Path DEMO_WALKTHROUGH = Path.of("docs/DEMO_WALKTHROUGH.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path THREAT_MODEL = Path.of("evidence/THREAT_MODEL.md");
    private static final Path RESILIENCE_SCORE = Path.of("evidence/RESILIENCE_SCORE.md");
    private static final Path PERFORMANCE_BASELINE = Path.of("evidence/PERFORMANCE_BASELINE.md");
    private static final Path CONTAINER_ROLLOUT = Path.of("docs/CONTAINER_REGISTRY_SIGNING_ROLLOUT.md");
    private static final Path POST_RELEASE = Path.of("docs/V2_5_0_POST_RELEASE_VERIFICATION.md");

    @Test
    void charterExistsAndDefinesEnterpriseLabIdentity() throws Exception {
        String charter = read(CHARTER);

        for (String expected : List.of(
                "LoadBalancerPro Enterprise Lab",
                "Enterprise Adaptive Routing Lab",
                "Production Gateway Candidate",
                "controlled scenario running",
                "deterministic replay",
                "LASE `off`/`shadow`/`recommend`/`active-experiment` comparison",
                "implemented policy gates",
                "scorecards",
                "evidence export",
                "SRE walkthroughs",
                "v2.5.0",
                "container publication and container signing are deferred",
                "No production deployment certification",
                "No real enterprise IdP tenant proof",
                "Adaptive-routing influence over runtime allocation remains controlled")) {
            assertTrue(charter.contains(expected), "charter should mention " + expected);
        }
    }

    @Test
    void roadmapExistsAndCapturesPrioritizedTracks() throws Exception {
        String roadmap = read(ROADMAP);

        for (String expected : List.of(
                "P0: Truth And Identity Alignment",
                "P1: Adaptive Routing Lab Workflow",
                "/api/lab/scenarios",
                "/api/lab/runs",
                "deterministic scenario summaries",
                "scorecards",
                "browser lab page",
                "memory or ignored-file run storage first",
                "P1: Controlled Active LASE Policy Gate",
                "`off`",
                "`shadow`",
                "`recommend`",
                "`active-experiment`",
                "P1: Observability Packs",
                "Prometheus/Grafana dashboard JSON",
                "SLO templates",
                "P2: Measured Performance Baseline",
                "target/performance-baseline/",
                "P2: Enterprise Auth Proof Lane",
                "mock IdP/JWKS fixture mode",
                "P2: Container Distribution Readiness",
                "cosign/keyless",
                "P3: Disposable Live Sandbox Lab",
                "no default CI live calls")) {
            assertTrue(roadmap.contains(expected), "roadmap should mention " + expected);
        }
    }

    @Test
    void controlledPolicyGateDocExistsAndCapturesSafeModes() throws Exception {
        String policy = read(POLICY_GATE);

        for (String expected : List.of(
                "# Controlled Active LASE Policy Gate",
                "`off`",
                "`shadow`",
                "`recommend`",
                "`active-experiment`",
                "loadbalancerpro.lase.policy.mode=off",
                "loadbalancerpro.lase.policy.active-experiment-enabled=false",
                "GET /api/lab/policy",
                "GET /api/lab/audit-events",
                "target/controlled-adaptive-routing/",
                "not production deployment certification")) {
            assertTrue(policy.contains(expected), "policy gate doc should mention " + expected);
        }
    }

    @Test
    void reviewerEntryPointsLinkCharterRoadmapAndNextGoalPrompts() throws Exception {
        for (Path path : List.of(README, PRODUCTION_SUMMARY, TRUST_MAP, SRE_HIGHLIGHTS, DEMO_WALKTHROUGH, RUNBOOK)) {
            String doc = read(path);
            assertTrue(doc.contains("ENTERPRISE_LAB_PRODUCT_CHARTER.md"),
                    path + " should link the product charter");
            assertTrue(doc.contains("ENTERPRISE_LAB_ROADMAP.md"),
                    path + " should link the roadmap");
        }

        for (Path path : List.of(README, PRODUCTION_SUMMARY, TRUST_MAP, SRE_HIGHLIGHTS, DEMO_WALKTHROUGH, RUNBOOK)) {
            assertTrue(read(path).contains("CONTROLLED_ACTIVE_LASE_POLICY_GATE.md"),
                    path + " should link the controlled policy gate");
        }

        for (Path path : List.of(README, PRODUCTION_SUMMARY, TRUST_MAP, RUNBOOK)) {
            assertTrue(read(path).contains("NEXT_GOAL_PROMPTS.md"),
                    path + " should link next-goal prompts");
        }
    }

    @Test
    void readmeStatesEnterpriseLabAndGatewayCandidateDistinction() throws Exception {
        String readme = read(README);
        String normalized = readme.toLowerCase(Locale.ROOT);

        assertTrue(readme.contains("LoadBalancerPro Enterprise Lab"));
        assertTrue(readme.contains("Enterprise Adaptive Routing Lab"));
        assertTrue(readme.contains("Production Gateway Candidate"));
        assertTrue(readme.contains("not a drop-in production cloud load balancer"));
        assertTrue(readme.contains("or a live enterprise gateway"));
        assertFalse(normalized.contains("production-ready gateway"));
        assertFalse(normalized.contains("production-grade gateway"));
        assertFalse(normalized.contains("production deployment certified"));
    }

    @Test
    void oauthScopeScpRoleDriftIsCorrected() throws Exception {
        String threatModel = read(THREAT_MODEL);
        String apiSecurity = read(Path.of("docs/API_SECURITY.md"));
        String combined = threatModel + "\n" + apiSecurity;

        assertTrue(combined.contains("scope` and `scp` claims do not grant"));
        assertTrue(combined.contains("dedicated role claims"));
        assertFalse(combined.contains("including `role`, `roles`, `authorities`, `scope`, `scp`, and `realm_access.roles`"));
        assertFalse(combined.contains("OAuth2 role mapping normalizes common JWT claim shapes"));
        assertFalse(combined.contains("scope/scp values become application roles"));
    }

    @Test
    void resilienceScoreSummaryMatchesDetailedSupplyChainScore() throws Exception {
        String scorecard = read(RESILIENCE_SCORE);

        int summaryScore = extractInt(scorecard,
                "\\| Supply-chain/dependency posture \\| (\\d+) \\|");
        int detailScore = extractInt(scorecard,
                "### Supply-Chain/Dependency Posture\\R\\RScore: (\\d+)/100\\.");

        assertEquals(summaryScore, detailScore,
                "supply-chain score summary and detail should agree");
        assertTrue(scorecard.contains("Overall score: **79/100**."));
        assertTrue(scorecard.contains("standard `scope` and `scp` claims do not grant application roles"));
    }

    @Test
    void performanceBaselineIsClearlyLocalMeasuredLaneOnly() throws Exception {
        String baseline = read(PERFORMANCE_BASELINE);
        String normalized = baseline.toLowerCase(Locale.ROOT);

        assertTrue(baseline.contains("Status: measured-lane ready."));
        assertTrue(baseline.contains("target/performance-baseline/"));
        assertTrue(baseline.contains("no live/private-network dependency"));
        assertTrue(baseline.contains("scripts\\smoke\\performance-baseline.ps1")
                || baseline.contains("scripts/smoke/performance-baseline.ps1"));
        assertTrue(baseline.contains("performance-dashboard.json"));
        assertTrue(baseline.contains("Do not claim:"));
        assertFalse(normalized.contains("measured production slo"));
        assertFalse(normalized.contains("production capacity proven"));
        assertFalse(normalized.contains("production performance certification is complete"));
    }

    @Test
    void releaseAndContainerTruthRemainHonest() throws Exception {
        String postRelease = read(POST_RELEASE);
        String containerRollout = read(CONTAINER_ROLLOUT);
        String charter = read(CHARTER);

        assertTrue(postRelease.contains("v2.5.0"));
        assertTrue(postRelease.contains("4cc03750be5479d9f8f88f8ef8014e05a8dc587a"));
        assertTrue(postRelease.contains("artifact attestation"));
        assertTrue(postRelease.contains("No container image was published"));
        assertTrue(postRelease.contains("Container registry publication and signing remain deferred"));
        assertTrue(containerRollout.contains("No container publication or container signing was performed"));
        assertTrue(charter.contains("container publication and container signing are deferred"));
        assertFalse((postRelease + containerRollout + charter).contains("published registry image"));
    }

    @Test
    void nextGoalPromptsCoverRequiredProductPushesAndSafetySections() throws Exception {
        String prompts = read(NEXT_GOALS);

        for (String expected : List.of(
                "Adaptive Routing Lab Workflow",
                "Controlled Active LASE Policy Gate",
                "Observability Packs",
                "Measured Performance Baseline",
                "Enterprise Auth Proof Lane",
                "Container Distribution Readiness",
                "Disposable Live Sandbox Lab",
                "Automation mode:",
                "Hard boundaries:",
                "Preflight:",
                "Checkpoints:",
                "Validation:",
                "Final report:")) {
            assertTrue(prompts.contains(expected), "next goal prompts should mention " + expected);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static int extractInt(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        assertTrue(matcher.find(), "expected pattern " + regex);
        return Integer.parseInt(matcher.group(1));
    }
}
