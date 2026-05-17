package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class AdaptiveRoutingScenarioGateEvaluator {
    public static final String EVALUATION_NAME = "Adaptive Routing Scenario Gate Evaluation";
    public static final String EVALUATION_VERSION = "adaptive-routing-scenario-gate-evaluation/v1";
    public static final String API_PATH = "/api/enterprise-lab/adaptive-routing-scenario-gate-evaluation";
    private static final String ENFORCEMENT_STATUS = "NOT_ENFORCED";
    private static final int EXPECTED_SCENARIO_COUNT = 3;
    private static final int EXPECTED_STRATEGY_COUNT = 5;
    private static final int EXPECTED_TOTAL_DECISIONS = 100;

    private final AdaptiveRoutingScenarioEvidencePacketBuilder packetBuilder;

    public AdaptiveRoutingScenarioGateEvaluator() {
        this(new AdaptiveRoutingScenarioEvidencePacketBuilder());
    }

    public AdaptiveRoutingScenarioGateEvaluator(AdaptiveRoutingScenarioEvidencePacketBuilder packetBuilder) {
        this.packetBuilder = Objects.requireNonNull(packetBuilder, "packetBuilder cannot be null");
    }

    public AdaptiveRoutingScenarioGateEvaluation evaluate() {
        AdaptiveRoutingScenarioEvidencePacket packet = packetBuilder.build();
        List<AdaptiveRoutingScenarioGateFinding> findings = findings(packet);
        int passed = count(findings, "PASS");
        int warnings = count(findings, "WARN");
        int failed = count(findings, "FAIL");

        return new AdaptiveRoutingScenarioGateEvaluation(
                EVALUATION_NAME,
                EVALUATION_VERSION,
                packet.mode(),
                packet.deterministic(),
                ENFORCEMENT_STATUS,
                decision(warnings, failed),
                packet.dashboardPath(),
                API_PATH,
                packet.apiPath(),
                packet.packetVersion(),
                sourceEndpoints(packet),
                packet.scenarioSummary().scenarioCount(),
                packet.strategiesCompared().size(),
                packet.totalDecisions(),
                findings.size(),
                passed,
                warnings,
                failed,
                findings,
                reviewerSummary(warnings, failed),
                reviewerActions(findings),
                recommendedNextSteps(),
                packet.notProvenBoundaries(),
                packet.safetyBoundaries());
    }

    private List<AdaptiveRoutingScenarioGateFinding> findings(AdaptiveRoutingScenarioEvidencePacket packet) {
        List<AdaptiveRoutingScenarioGateFinding> findings = new ArrayList<>();
        findings.add(passIf(
                "packet-present",
                "Evidence packet is present",
                packet.packetName().equals(AdaptiveRoutingScenarioEvidencePacketBuilder.PACKET_NAME)
                        && packet.packetVersion().equals(AdaptiveRoutingScenarioEvidencePacketBuilder.PACKET_VERSION),
                packet.apiPath(),
                "Packet name and version match the local adaptive-routing evidence packet contract.",
                "Review packet version before comparing evidence across revisions."));
        findings.add(passIf(
                "local-synthetic-mode",
                "Local synthetic mode is explicit",
                "local-synthetic".equals(packet.mode()) && packet.deterministic(),
                packet.apiPath(),
                "Packet mode is local-synthetic and deterministic is true.",
                "Do not cite this output as live traffic or production benchmark evidence."));
        findings.add(new AdaptiveRoutingScenarioGateFinding(
                "not-enforced-boundary",
                "Gate evaluation is not enforced",
                "WARN",
                "warning",
                API_PATH,
                "This evaluator produces local reviewer findings only; enforcementStatus remains NOT_ENFORCED.",
                "Keep branch protection, required checks, and GitHub settings unchanged unless a later manual governance sprint approves them."));
        findings.add(passIf(
                "scenario-count",
                "Expected scenario count is present",
                packet.scenarioSummary().scenarioCount() == EXPECTED_SCENARIO_COUNT
                        && packet.scenarioDrilldowns().scenarioCount() == EXPECTED_SCENARIO_COUNT,
                packet.scenarioSummary().apiPath() + " and " + packet.scenarioDrilldowns().detailApiPath(),
                "Summary and drilldown both expose 3 deterministic scenarios.",
                "Review missing scenarios before treating this packet as ready for local review."));
        findings.add(passIf(
                "strategy-count",
                "Expected strategy count is present",
                packet.strategiesCompared().size() == EXPECTED_STRATEGY_COUNT,
                packet.apiPath(),
                "Packet compares 5 existing routing strategies.",
                "Review strategy coverage before changing adaptive-routing behavior."));
        findings.add(passIf(
                "stable-decision-count",
                "Selected decision count is stable",
                packet.totalDecisions() == EXPECTED_TOTAL_DECISIONS && selectedServerCount(packet) == packet.totalDecisions(),
                packet.apiPath(),
                "Packet reports 100 selected decisions and selected-server distributions sum to that count.",
                "Investigate any mismatch before using packet output for review."));
        findings.add(passIf(
                "selected-server-distributions",
                "Selected-server distributions exist",
                !packet.selectedServerDistributions().isEmpty()
                        && packet.selectedServerDistributions().values().stream()
                                .allMatch(strategyCounts -> !strategyCounts.isEmpty()),
                packet.apiPath(),
                "Each scenario carries strategy-level selected-server distribution data.",
                "Inspect distribution changes when reviewing strategy behavior."));
        findings.add(passIf(
                "explanation-notes",
                "Explanation notes exist",
                !packet.explanationNotes().isEmpty()
                        && packet.scenarioDrilldowns().scenarios().stream()
                                .flatMap(scenario -> scenario.strategyExplanations().stream())
                                .allMatch(explanation -> !explanation.explanationNotes().isEmpty()),
                packet.scenarioDrilldowns().detailApiPath(),
                "Packet includes scenario drilldown explanation notes based on local synthetic inputs and observed output distribution.",
                "Read explanation notes as review guidance, not guaranteed hidden internal causality."));
        findings.add(passIf(
                "reviewer-checklist",
                "Reviewer checklist exists",
                !packet.reviewerChecklist().isEmpty(),
                packet.apiPath(),
                "Packet includes reviewer checklist items for local handoff.",
                "Complete checklist items before citing the packet in reviewer notes."));
        findings.add(passIf(
                "not-proven-boundaries",
                "Not-proven boundaries exist",
                packet.notProvenBoundaries().contains("No production certification")
                        && packet.notProvenBoundaries().contains("No live traffic validation")
                        && packet.notProvenBoundaries().contains("No GitHub governance-applied proof"),
                packet.apiPath(),
                "Packet keeps production, live traffic, and governance-applied boundaries explicit.",
                "Keep those boundaries attached to any reviewer handoff."));
        findings.add(new AdaptiveRoutingScenarioGateFinding(
                "intentional-limitations",
                "Intentional limitations remain visible",
                packet.warnings().isEmpty() ? "FAIL" : "WARN",
                packet.warnings().isEmpty() ? "blocking-for-review-only" : "warning",
                packet.apiPath(),
                packet.warnings().isEmpty()
                        ? "Packet warnings are missing, so local limitations are not visible."
                        : "Packet warnings are present and should be reviewed as local-only limitations.",
                "Confirm warnings do not become production proof or active CI enforcement claims."));
        findings.add(passIf(
                "no-production-proof-claim",
                "Production proof is not claimed",
                packet.notProvenBoundaries().stream().anyMatch(boundary -> boundary.contains("No production"))
                        && packet.safetyBoundaries().contains("no external network calls"),
                packet.apiPath(),
                "Packet carries explicit no-production-proof boundaries and stays inside local safety constraints.",
                "Reject any reviewer note that turns this local evaluation into production certification."));
        return List.copyOf(findings);
    }

    private AdaptiveRoutingScenarioGateFinding passIf(String id,
                                                      String title,
                                                      boolean condition,
                                                      String evidenceSource,
                                                      String passExplanation,
                                                      String reviewerAction) {
        return new AdaptiveRoutingScenarioGateFinding(
                id,
                title,
                condition ? "PASS" : "FAIL",
                condition ? "info" : "blocking-for-review-only",
                evidenceSource,
                condition ? passExplanation : "Required local packet condition is missing or inconsistent.",
                reviewerAction);
    }

    private List<String> sourceEndpoints(AdaptiveRoutingScenarioEvidencePacket packet) {
        Set<String> endpoints = new LinkedHashSet<>(packet.sourceEndpoints());
        endpoints.add(API_PATH);
        return List.copyOf(endpoints);
    }

    private int selectedServerCount(AdaptiveRoutingScenarioEvidencePacket packet) {
        return packet.selectedServerDistributions().values().stream()
                .flatMap(strategyCounts -> strategyCounts.values().stream())
                .flatMap(serverCounts -> serverCounts.values().stream())
                .mapToInt(Integer::intValue)
                .sum();
    }

    private int count(List<AdaptiveRoutingScenarioGateFinding> findings, String status) {
        return (int) findings.stream()
                .filter(finding -> status.equals(finding.status()))
                .count();
    }

    private String decision(int warnings, int failed) {
        if (failed > 0) {
            return "BLOCKED_FOR_LOCAL_REVIEW";
        }
        if (warnings > 0) {
            return "REVIEW_WARNINGS_PRESENT";
        }
        return "READY_FOR_LOCAL_REVIEW";
    }

    private String reviewerSummary(int warnings, int failed) {
        if (failed > 0) {
            return "Local adaptive-routing scenario evidence needs reviewer attention before it is cited.";
        }
        if (warnings > 0) {
            return "Local adaptive-routing scenario evidence is complete enough for review, with warnings that keep enforcement and production-proof limits explicit.";
        }
        return "Local adaptive-routing scenario evidence is ready for review.";
    }

    private List<String> reviewerActions(List<AdaptiveRoutingScenarioGateFinding> findings) {
        return findings.stream()
                .map(finding -> finding.title() + ": " + finding.reviewerAction())
                .toList();
    }

    private List<String> recommendedNextSteps() {
        return List.of(
                "Use this evaluation as a local reviewer checklist for adaptive-routing scenario evidence.",
                "Keep REVIEW_WARNINGS_PRESENT as a local review signal, not a merge-blocking CI result.",
                "Compare findings with the CI evidence gate artifact contract before building any parser.",
                "Approve schema, failure policy, and runtime cost before any future CI enforcement work.");
    }
}
