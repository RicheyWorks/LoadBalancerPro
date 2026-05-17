package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AdaptiveRoutingScenarioEvidencePacketBuilder {
    public static final String PACKET_NAME = "Adaptive Routing Scenario Evidence Packet";
    public static final String PACKET_VERSION = "adaptive-routing-scenario-evidence-packet/v1";
    public static final String API_PATH = "/api/enterprise-lab/adaptive-routing-scenario-evidence-packet";
    public static final String LOCAL_PACKET_PATH =
            "target/adaptive-routing-scenarios/adaptive-routing-scenario-evidence-packet.json";

    private final AdaptiveRoutingScenarioRunner runner;

    public AdaptiveRoutingScenarioEvidencePacketBuilder() {
        this(new AdaptiveRoutingScenarioRunner());
    }

    public AdaptiveRoutingScenarioEvidencePacketBuilder(AdaptiveRoutingScenarioRunner runner) {
        this.runner = Objects.requireNonNull(runner, "runner cannot be null");
    }

    public AdaptiveRoutingScenarioEvidencePacket build() {
        AdaptiveRoutingScenarioSummary summary = runner.runSummary();
        AdaptiveRoutingScenarioDrilldown drilldown = runner.runDrilldown();

        return new AdaptiveRoutingScenarioEvidencePacket(
                PACKET_NAME,
                PACKET_VERSION,
                summary.mode(),
                summary.deterministic() && drilldown.deterministic(),
                summary.generatedAt(),
                summary.generatedAtPolicy(),
                summary.dashboardPath(),
                API_PATH,
                List.of(
                        summary.apiPath(),
                        drilldown.detailApiPath(),
                        API_PATH),
                summary,
                drilldown,
                summary.strategiesCompared(),
                summary.totalDecisions(),
                selectedServerDistributions(summary),
                explanationNotes(drilldown),
                warnings(summary, drilldown),
                readinessForCiGate(summary),
                localEvidencePaths(summary),
                summary.notProvenBoundaries(),
                summary.safetyBoundaries(),
                reviewerChecklist(),
                recommendedNextSteps(),
                evidenceSections(summary, drilldown));
    }

    private Map<String, Map<String, Map<String, Integer>>> selectedServerDistributions(
            AdaptiveRoutingScenarioSummary summary) {
        Map<String, Map<String, Map<String, Integer>>> distributions = new LinkedHashMap<>();
        for (AdaptiveRoutingScenarioResult scenario : summary.scenarios()) {
            distributions.put(scenario.scenarioName(), scenario.selectedServerCounts());
        }
        return distributions;
    }

    private List<String> explanationNotes(AdaptiveRoutingScenarioDrilldown drilldown) {
        List<String> notes = new ArrayList<>();
        for (AdaptiveRoutingScenarioDetail scenario : drilldown.scenarios()) {
            for (AdaptiveRoutingStrategyExplanation explanation : scenario.strategyExplanations()) {
                explanation.explanationNotes().stream()
                        .limit(2)
                        .map(note -> scenario.scenarioName() + " / " + explanation.strategyName() + ": " + note)
                        .forEach(notes::add);
            }
        }
        notes.add("Packet explanations are local synthetic review notes based on visible inputs, exposed decision "
                + "reasons, score snapshots when available, and observed distribution.");
        notes.add("The packet does not claim hidden strategy causality, production performance, or live traffic proof.");
        return notes;
    }

    private List<String> warnings(AdaptiveRoutingScenarioSummary summary, AdaptiveRoutingScenarioDrilldown drilldown) {
        Set<String> warnings = new LinkedHashSet<>();
        warnings.addAll(summary.warnings());
        warnings.addAll(drilldown.warnings());
        for (AdaptiveRoutingScenarioResult scenario : summary.scenarios()) {
            warnings.addAll(scenario.warnings());
        }
        warnings.add("Evidence packet is in-memory endpoint output only; it does not write a generated artifact file.");
        return List.copyOf(warnings);
    }

    private List<String> localEvidencePaths(AdaptiveRoutingScenarioSummary summary) {
        Set<String> paths = new LinkedHashSet<>(summary.localEvidencePaths());
        paths.add(LOCAL_PACKET_PATH);
        return List.copyOf(paths);
    }

    private String readinessForCiGate(AdaptiveRoutingScenarioSummary summary) {
        boolean allReady = summary.scenarios().stream()
                .allMatch(scenario -> "READY_FOR_LOCAL_CI_GATE_REVIEW".equals(scenario.readinessForCiGate()));
        return allReady ? "READY_FOR_LOCAL_CI_GATE_REVIEW" : "NEEDS_MANUAL_REVIEW";
    }

    private List<String> reviewerChecklist() {
        return List.of(
                "Confirm packetVersion is " + PACKET_VERSION + ".",
                "Confirm mode is local-synthetic and deterministic is true.",
                "Confirm sourceEndpoints are same-origin local Enterprise Lab APIs.",
                "Confirm scenarioSummary contains 3 scenarios, 5 strategies, and 100 selected decisions.",
                "Confirm scenarioDrilldowns include strategy explanations and selected-server distributions.",
                "Confirm localEvidencePaths stay under ignored target/ paths.",
                "Confirm not-proven boundaries remain explicit before citing this packet.",
                "Confirm no production benchmark, live traffic, tenant, IdP, cloud, signing, registry, or governance "
                        + "proof is inferred.");
    }

    private List<String> recommendedNextSteps() {
        return List.of(
                "Use this packet as a local reviewer handoff shape for adaptive-routing scenario evidence.",
                "Compare packet source endpoints with the CI evidence gate artifact contract before any future parser.",
                "Keep any future generated packet output under ignored target/adaptive-routing-scenarios/ only.",
                "Approve schema and failure policy separately before treating the packet as a required CI gate input.");
    }

    private List<AdaptiveRoutingScenarioEvidenceSection> evidenceSections(
            AdaptiveRoutingScenarioSummary summary,
            AdaptiveRoutingScenarioDrilldown drilldown) {
        return List.of(
                new AdaptiveRoutingScenarioEvidenceSection(
                        "Packet Overview",
                        "Deterministic in-memory packet for local adaptive-routing scenario review.",
                        List.of(
                                "Mode: " + summary.mode(),
                                "Deterministic: " + summary.deterministic(),
                                "Total decisions: " + summary.totalDecisions())),
                new AdaptiveRoutingScenarioEvidenceSection(
                        "Scenario Summary",
                        "Selected-server distribution evidence across existing routing strategies.",
                        summary.scenarios().stream()
                                .map(scenario -> scenario.scenarioName() + ": " + scenario.totalDecisions()
                                        + " selected decisions")
                                .toList()),
                new AdaptiveRoutingScenarioEvidenceSection(
                        "Scenario Drilldowns",
                        "Strategy explanation packets based on visible synthetic inputs and observed distribution.",
                        drilldown.scenarios().stream()
                                .map(scenario -> scenario.scenarioName() + ": "
                                        + scenario.strategyExplanations().size() + " strategy explanations")
                                .toList()),
                new AdaptiveRoutingScenarioEvidenceSection(
                        "Safety Boundaries",
                        "The packet is local review evidence, not production or governance proof.",
                        summary.notProvenBoundaries()));
    }
}
