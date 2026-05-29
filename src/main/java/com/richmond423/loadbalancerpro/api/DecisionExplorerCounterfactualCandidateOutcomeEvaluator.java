package com.richmond423.loadbalancerpro.api;

import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticListSupport.distinctSorted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class DecisionExplorerCounterfactualCandidateOutcomeEvaluator {
    List<DecisionExplorerCounterfactualCandidateOutcomeV1> evaluate(
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> policyWeightScenarios,
            String boundaryNote) {
        List<DecisionExplorerRouteTradeoffRowV1> rows =
                DecisionExplorerDiagnosticListSupport.copyNonNull(tradeoff.candidateTradeoffs());
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<String, DecisionExplorerShadowCandidateOutcomeV1> shadowOutcomesByCandidate =
                DecisionExplorerDtoSupport.copyOrEmpty(quality.candidateOutcomeComparisons()).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(
                                DecisionExplorerShadowCandidateOutcomeV1::candidateId,
                                row -> row,
                                (left, right) -> left,
                                java.util.TreeMap::new));
        List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios =
                DecisionExplorerDiagnosticListSupport.copyNonNull(policyWeightScenarios);
        return rows.stream()
                .sorted(DecisionExplorerRouteTradeoffRowBuilder.BY_SELECTED_THEN_ORDER)
                .map(row -> outcome(row, shadowOutcomesByCandidate.get(row.candidateId()), scenarios, boundaryNote))
                .sorted(Comparator
                        .comparingInt((DecisionExplorerCounterfactualCandidateOutcomeV1 row) -> row.selected()
                                ? 0
                                : 1)
                        .thenComparingInt(DecisionExplorerCounterfactualCandidateOutcomeV1::displayOrder)
                        .thenComparing(DecisionExplorerCounterfactualCandidateOutcomeV1::candidateId))
                .toList();
    }

    private static DecisionExplorerCounterfactualCandidateOutcomeV1 outcome(
            DecisionExplorerRouteTradeoffRowV1 row,
            DecisionExplorerShadowCandidateOutcomeV1 shadowOutcome,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> policyWeightScenarios,
            String boundaryNote) {
        String counterfactualOutcome = counterfactualOutcome(row, shadowOutcome, policyWeightScenarios);
        String influence = policyScenarioInfluence(counterfactualOutcome);
        List<String> supportingScenarios = supportingScenarioIds(row, policyWeightScenarios);
        List<String> challengingScenarios = challengingScenarioIds(row, policyWeightScenarios, counterfactualOutcome);
        String strongestScenarioId = strongestScenarioId(counterfactualOutcome, supportingScenarios,
                challengingScenarios);
        List<String> benefitSignals = benefitSignals(row, shadowOutcome, supportingScenarios);
        List<String> riskSignals = riskSignals(row, shadowOutcome, challengingScenarios, counterfactualOutcome);
        List<String> unknownSignals = unknownSignals(row, shadowOutcome, policyWeightScenarios);
        List<String> degradedSignals = degradedSignals(row, shadowOutcome);
        List<String> reasonCodes = reasonCodes(row, shadowOutcome, counterfactualOutcome, influence,
                strongestScenarioId);
        return new DecisionExplorerCounterfactualCandidateOutcomeV1(
                true,
                true,
                true,
                DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_OBJECT,
                DecisionExplorerCounterfactualCandidateOutcomeV1.CONTRACT_VERSION,
                row.candidateId(),
                row.candidateLabel(),
                row.selected(),
                row.displayOrder(),
                shadowOutcome == null ? "UNKNOWN" : shadowOutcome.outcomeLabel(),
                counterfactualOutcome,
                influence,
                strongestScenarioId,
                row.tradeoffCategory(),
                row.riskBenefitClassification(),
                row.scoreGapCategory(),
                row.finalScore(),
                row.scoreDeltaFromSelected(),
                supportingScenarios,
                challengingScenarios,
                summaryText(row, counterfactualOutcome, strongestScenarioId),
                benefitSignals,
                riskSignals,
                unknownSignals,
                degradedSignals,
                reasonCodes,
                sourceReferenceIds(row, shadowOutcome),
                boundaryNote);
    }

    private static String counterfactualOutcome(
            DecisionExplorerRouteTradeoffRowV1 row,
            DecisionExplorerShadowCandidateOutcomeV1 shadowOutcome,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> policyWeightScenarios) {
        if (hasInsufficientScenario(policyWeightScenarios)) {
            return DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_INSUFFICIENT_EVIDENCE;
        }
        if (row.selected()) {
            if (isSelectedDegraded(row, shadowOutcome) || hasDegradedScenario(policyWeightScenarios)) {
                return DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_SELECTED_DEGRADED;
            }
            if (hasSensitiveScenario(policyWeightScenarios)) {
                return DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_SELECTED_SENSITIVE;
            }
            return DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_SELECTED_STABLE;
        }
        if (DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_BEATS_SELECTED.equals(row.tradeoffCategory())
                || shadowOutcome != null
                        && DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SAFER_ALTERNATIVE.equals(
                                shadowOutcome.outcomeLabel())) {
            return DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_ALTERNATIVE_CHALLENGES_SELECTED;
        }
        if (DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_CLOSE.equals(row.tradeoffCategory())
                || hasAlternativeCloseScenario(policyWeightScenarios)) {
            return DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_ALTERNATIVE_CLOSE_CALL;
        }
        if (DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_UNKNOWN.equals(row.tradeoffCategory())
                || shadowOutcome != null
                        && DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_UNKNOWN_ALTERNATIVE.equals(
                                shadowOutcome.outcomeLabel())) {
            return DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_ALTERNATIVE_UNKNOWN;
        }
        if (policyWeightScenarios.isEmpty()) {
            return DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_UNKNOWN;
        }
        return DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_ALTERNATIVE_TRAILING;
    }

    private static String policyScenarioInfluence(String counterfactualOutcome) {
        return switch (counterfactualOutcome) {
            case DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_SELECTED_STABLE,
                    DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_ALTERNATIVE_TRAILING ->
                    DecisionExplorerCounterfactualCandidateOutcomeV1.INFLUENCE_STABILIZING;
            case DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_SELECTED_SENSITIVE,
                    DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_ALTERNATIVE_CLOSE_CALL ->
                    DecisionExplorerCounterfactualCandidateOutcomeV1.INFLUENCE_SENSITIVE;
            case DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_ALTERNATIVE_CHALLENGES_SELECTED ->
                    DecisionExplorerCounterfactualCandidateOutcomeV1.INFLUENCE_CHALLENGING;
            case DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_SELECTED_DEGRADED ->
                    DecisionExplorerCounterfactualCandidateOutcomeV1.INFLUENCE_DEGRADED;
            case DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_INSUFFICIENT_EVIDENCE ->
                    DecisionExplorerCounterfactualCandidateOutcomeV1.INFLUENCE_INSUFFICIENT;
            default -> DecisionExplorerCounterfactualCandidateOutcomeV1.INFLUENCE_UNKNOWN;
        };
    }

    private static List<String> supportingScenarioIds(
            DecisionExplorerRouteTradeoffRowV1 row,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios) {
        return distinctSorted(scenarios.stream()
                .filter(scenario -> DecisionExplorerCounterfactualAnalysisV1.LABEL_STABLE.equals(
                        scenario.sensitivityLabel())
                        || row.selected()
                                && DecisionExplorerCounterfactualPolicyWeightScenarioV1
                                        .SCENARIO_SELECTED_SUPPORT_PLUS_10.equals(scenario.scenarioId()))
                .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::scenarioId)
                .toList());
    }

    private static List<String> challengingScenarioIds(
            DecisionExplorerRouteTradeoffRowV1 row,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios,
            String counterfactualOutcome) {
        List<String> values = new ArrayList<>();
        scenarios.stream()
                .filter(scenario -> DecisionExplorerCounterfactualAnalysisV1.LABEL_CLOSE_CALL.equals(
                        scenario.sensitivityLabel())
                        || DecisionExplorerCounterfactualAnalysisV1.LABEL_SENSITIVE.equals(
                                scenario.sensitivityLabel())
                        || DecisionExplorerCounterfactualAnalysisV1.LABEL_DEGRADED.equals(
                                scenario.sensitivityLabel())
                        || DecisionExplorerCounterfactualAnalysisV1.LABEL_INSUFFICIENT_EVIDENCE.equals(
                                scenario.sensitivityLabel()))
                .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::scenarioId)
                .forEach(values::add);
        if (!row.selected() && DecisionExplorerCounterfactualCandidateOutcomeV1
                .OUTCOME_ALTERNATIVE_CHALLENGES_SELECTED.equals(counterfactualOutcome)) {
            values.add(DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_ALTERNATIVE_SUPPORT_PLUS_10);
        }
        return distinctSorted(values);
    }

    private static String strongestScenarioId(
            String counterfactualOutcome,
            List<String> supportingScenarioIds,
            List<String> challengingScenarioIds) {
        if (DecisionExplorerCounterfactualCandidateOutcomeV1.INFLUENCE_CHALLENGING.equals(
                policyScenarioInfluence(counterfactualOutcome))
                || DecisionExplorerCounterfactualCandidateOutcomeV1.INFLUENCE_SENSITIVE.equals(
                        policyScenarioInfluence(counterfactualOutcome))) {
            return challengingScenarioIds.stream().findFirst().orElse("UNKNOWN");
        }
        return supportingScenarioIds.stream().findFirst().orElse("UNKNOWN");
    }

    private static List<String> benefitSignals(
            DecisionExplorerRouteTradeoffRowV1 row,
            DecisionExplorerShadowCandidateOutcomeV1 shadowOutcome,
            List<String> supportingScenarioIds) {
        List<String> signals = new ArrayList<>(row.benefitSignals());
        signals.addAll(supportingScenarioIds.stream()
                .map(id -> "policy scenario " + id + " supports current candidate interpretation")
                .toList());
        if (shadowOutcome != null
                && DecisionExplorerShadowCandidateOutcomeV1.IMPACT_SUPPORTS_DECISION.equals(
                        shadowOutcome.qualityImpact())) {
            signals.add("shadow candidate outcome supports the returned decision");
        }
        return distinctSorted(signals);
    }

    private static List<String> riskSignals(
            DecisionExplorerRouteTradeoffRowV1 row,
            DecisionExplorerShadowCandidateOutcomeV1 shadowOutcome,
            List<String> challengingScenarioIds,
            String counterfactualOutcome) {
        List<String> signals = new ArrayList<>(row.riskSignals());
        signals.addAll(challengingScenarioIds.stream()
                .map(id -> "policy scenario " + id + " challenges current candidate interpretation")
                .toList());
        if (shadowOutcome != null
                && !DecisionExplorerShadowCandidateOutcomeV1.IMPACT_SUPPORTS_DECISION.equals(
                        shadowOutcome.qualityImpact())) {
            signals.add("shadow candidate outcome is " + shadowOutcome.qualityImpact());
        }
        if (DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_ALTERNATIVE_CHALLENGES_SELECTED.equals(
                counterfactualOutcome)) {
            signals.add("alternative can challenge selected candidate under returned score evidence");
        }
        return distinctSorted(signals);
    }

    private static List<String> unknownSignals(
            DecisionExplorerRouteTradeoffRowV1 row,
            DecisionExplorerShadowCandidateOutcomeV1 shadowOutcome,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios) {
        List<String> signals = new ArrayList<>(row.unknownSignals());
        if (shadowOutcome != null) {
            signals.addAll(shadowOutcome.unknownSignals());
        }
        if (scenarios.isEmpty()) {
            signals.add("policy-weight scenarios were unavailable for candidate counterfactual outcome");
        }
        return distinctSorted(signals);
    }

    private static List<String> degradedSignals(
            DecisionExplorerRouteTradeoffRowV1 row,
            DecisionExplorerShadowCandidateOutcomeV1 shadowOutcome) {
        return distinctSorted(Stream.of(row.degradedSignals(),
                        shadowOutcome == null ? List.<String>of() : shadowOutcome.degradedSignals())
                .flatMap(Collection::stream)
                .toList());
    }

    private static List<String> reasonCodes(
            DecisionExplorerRouteTradeoffRowV1 row,
            DecisionExplorerShadowCandidateOutcomeV1 shadowOutcome,
            String counterfactualOutcome,
            String influence,
            String strongestScenarioId) {
        List<String> reasons = new ArrayList<>();
        reasons.add("COUNTERFACTUAL_CANDIDATE_OUTCOME_" + counterfactualOutcome);
        reasons.add("COUNTERFACTUAL_CANDIDATE_INFLUENCE_" + influence);
        reasons.add("COUNTERFACTUAL_CANDIDATE_SCENARIO_" + strongestScenarioId);
        reasons.add("ROUTE_TRADEOFF_CATEGORY_" + row.tradeoffCategory());
        reasons.addAll(row.reasonCodes());
        if (shadowOutcome != null) {
            reasons.addAll(shadowOutcome.reasonCodes());
        }
        return distinctSorted(reasons);
    }

    private static List<String> sourceReferenceIds(
            DecisionExplorerRouteTradeoffRowV1 row,
            DecisionExplorerShadowCandidateOutcomeV1 shadowOutcome) {
        return distinctSorted(Stream.of(row.sourceReferenceIds(),
                        shadowOutcome == null ? List.<String>of() : shadowOutcome.sourceReferenceIds())
                .flatMap(Collection::stream)
                .toList());
    }

    private static boolean hasSensitiveScenario(
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios) {
        return scenarios.stream().anyMatch(scenario ->
                DecisionExplorerCounterfactualAnalysisV1.LABEL_SENSITIVE.equals(scenario.sensitivityLabel())
                        || DecisionExplorerCounterfactualAnalysisV1.LABEL_CLOSE_CALL.equals(
                                scenario.sensitivityLabel()));
    }

    private static boolean hasAlternativeCloseScenario(
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios) {
        return scenarios.stream().anyMatch(scenario ->
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_ALTERNATIVE_SUPPORT_PLUS_10.equals(
                        scenario.scenarioId())
                        && DecisionExplorerCounterfactualAnalysisV1.LABEL_CLOSE_CALL.equals(
                                scenario.sensitivityLabel()));
    }

    private static boolean hasInsufficientScenario(
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios) {
        return scenarios.stream().anyMatch(scenario ->
                DecisionExplorerCounterfactualAnalysisV1.LABEL_INSUFFICIENT_EVIDENCE.equals(
                        scenario.sensitivityLabel()));
    }

    private static boolean hasDegradedScenario(
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios) {
        return scenarios.stream().anyMatch(scenario ->
                DecisionExplorerCounterfactualAnalysisV1.LABEL_DEGRADED.equals(scenario.sensitivityLabel()));
    }

    private static boolean isSelectedDegraded(
            DecisionExplorerRouteTradeoffRowV1 row,
            DecisionExplorerShadowCandidateOutcomeV1 shadowOutcome) {
        return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(row.diagnosticStatus())
                || !row.degradedSignals().isEmpty()
                || shadowOutcome != null
                        && DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED.equals(
                                shadowOutcome.outcomeLabel());
    }

    private static String summaryText(
            DecisionExplorerRouteTradeoffRowV1 row,
            String counterfactualOutcome,
            String strongestScenarioId) {
        return "Candidate " + row.candidateId() + " has counterfactual outcome "
                + counterfactualOutcome + " under local policy scenario " + strongestScenarioId
                + " from returned tradeoff " + row.tradeoffCategory()
                + "; no production routing, scoring, proxying, or traffic shifting is performed.";
    }
}
