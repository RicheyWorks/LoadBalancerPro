package com.richmond423.loadbalancerpro.api;

import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticListSupport.distinctSorted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

final class DecisionExplorerCounterfactualFactorWeightDeltaEvaluator {
    List<DecisionExplorerCounterfactualFactorWeightDeltaV1> evaluate(
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> policyWeightScenarios,
            String boundaryNote) {
        List<DecisionExplorerFactorTradeoffDeltaV1> factorDeltas = tradeoff == null
                ? List.of()
                : DecisionExplorerDiagnosticListSupport.copyNonNull(tradeoff.factorTradeoffDeltas());
        if (factorDeltas.isEmpty()) {
            return List.of();
        }

        ScenarioContext scenarioContext =
                ScenarioContext.from(DecisionExplorerDiagnosticListSupport.copyNonNull(policyWeightScenarios));
        return factorDeltas.stream()
                .map(delta -> evaluateDelta(delta, scenarioContext, boundaryNote))
                .sorted(Comparator
                        .comparingInt(DecisionExplorerCounterfactualFactorWeightDeltaV1::displayOrder)
                        .thenComparing(DecisionExplorerCounterfactualFactorWeightDeltaV1::factorName)
                        .thenComparing(DecisionExplorerCounterfactualFactorWeightDeltaV1::alternativeCandidateId))
                .toList();
    }

    private static DecisionExplorerCounterfactualFactorWeightDeltaV1 evaluateDelta(
            DecisionExplorerFactorTradeoffDeltaV1 delta,
            ScenarioContext scenarioContext,
            String boundaryNote) {
        String classification = classification(delta, scenarioContext);
        DecisionExplorerCounterfactualPolicyWeightScenarioV1 strongestScenario =
                strongestScenario(classification, scenarioContext);
        boolean selectedSupportStabilizesDecision =
                DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_STABILIZING.equals(classification)
                        && scenarioContext.selectedCandidateRemainsSupported();
        boolean alternativeSupportCanChallengeSelection =
                DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_DESTABILIZING.equals(classification)
                        || scenarioContext.alternativeBecomesCloseOrPreferable();
        List<String> stabilizingSignals = stabilizingSignals(delta, scenarioContext, classification);
        List<String> destabilizingSignals = destabilizingSignals(delta, scenarioContext, classification);
        List<String> limitationSignals = limitationSignals(delta, scenarioContext, classification);
        List<String> reasonCodes = reasonCodes(delta, scenarioContext, classification,
                selectedSupportStabilizesDecision, alternativeSupportCanChallengeSelection);

        return new DecisionExplorerCounterfactualFactorWeightDeltaV1(
                true,
                true,
                true,
                DecisionExplorerCounterfactualFactorWeightDeltaV1.DELTA_OBJECT,
                DecisionExplorerCounterfactualFactorWeightDeltaV1.CONTRACT_VERSION,
                delta.factorName(),
                delta.selectedCandidateId(),
                delta.alternativeCandidateId(),
                delta.displayOrder(),
                classification,
                delta.deltaClassification(),
                delta.selectedContribution(),
                delta.alternativeContribution(),
                delta.selectedFactorStatus(),
                delta.alternativeFactorStatus(),
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_RETURNED_EVIDENCE_WEIGHTS,
                scenarioContext.selectedSupportLabel(),
                scenarioContext.alternativeSupportLabel(),
                strongestScenario == null ? "UNKNOWN" : strongestScenario.scenarioId(),
                strongestScenario == null ? 0 : strongestScenario.localAssumptionWeightShiftPercent(),
                selectedSupportStabilizesDecision,
                alternativeSupportCanChallengeSelection,
                delta.scoreGapCategory(),
                delta.alternativeScoreDeltaFromSelected(),
                summaryText(delta, classification, strongestScenario, selectedSupportStabilizesDecision,
                        alternativeSupportCanChallengeSelection),
                stabilizingSignals,
                destabilizingSignals,
                limitationSignals,
                reasonCodes,
                sourceReferenceIds(delta, scenarioContext),
                boundaryNote);
    }

    private static String classification(
            DecisionExplorerFactorTradeoffDeltaV1 delta,
            ScenarioContext scenarioContext) {
        if (delta == null) {
            return DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_UNKNOWN;
        }
        if (DecisionExplorerFactorTradeoffDeltaV1.DELTA_DEGRADED.equals(delta.deltaClassification())
                || scenarioContext.hasDegradedScenario()) {
            return DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_DEGRADED;
        }
        if (DecisionExplorerFactorTradeoffDeltaV1.DELTA_UNKNOWN.equals(delta.deltaClassification())
                || scenarioContext.hasNoComputedScenarioBasis()) {
            return DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_UNKNOWN;
        }
        if (DecisionExplorerFactorTradeoffDeltaV1.DELTA_ADVANTAGE.equals(delta.deltaClassification())) {
            return DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_STABILIZING;
        }
        if (DecisionExplorerFactorTradeoffDeltaV1.DELTA_DISADVANTAGE.equals(delta.deltaClassification())) {
            return DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_DESTABILIZING;
        }
        return DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_NEUTRAL;
    }

    private static DecisionExplorerCounterfactualPolicyWeightScenarioV1 strongestScenario(
            String classification,
            ScenarioContext scenarioContext) {
        return switch (classification) {
            case DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_STABILIZING ->
                    scenarioContext.selectedSupportScenario();
            case DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_DESTABILIZING ->
                    scenarioContext.alternativeSupportScenario();
            case DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_DEGRADED ->
                    scenarioContext.degradedScenario();
            default -> scenarioContext.baselineScenario();
        };
    }

    private static List<String> stabilizingSignals(
            DecisionExplorerFactorTradeoffDeltaV1 delta,
            ScenarioContext scenarioContext,
            String classification) {
        List<String> signals = new ArrayList<>();
        if (DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_STABILIZING.equals(classification)) {
            signals.add("selected factor has returned-evidence advantage");
        }
        if (scenarioContext.selectedCandidateRemainsSupported()) {
            signals.add("selected-support scenario keeps selected candidate supported");
        }
        if (DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_SUPPORTING.equals(delta.selectedContribution())) {
            signals.add("selected contribution=SUPPORTING");
        }
        signals.addAll(delta.selectedSignals());
        return distinctSorted(signals);
    }

    private static List<String> destabilizingSignals(
            DecisionExplorerFactorTradeoffDeltaV1 delta,
            ScenarioContext scenarioContext,
            String classification) {
        List<String> signals = new ArrayList<>();
        if (DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_DESTABILIZING.equals(classification)) {
            signals.add("alternative factor has returned-evidence advantage");
        }
        if (scenarioContext.alternativeBecomesCloseOrPreferable()) {
            signals.add("alternative-support scenario can make the alternative close or preferable");
        }
        if (DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_SUPPORTING.equals(delta.alternativeContribution())) {
            signals.add("alternative contribution=SUPPORTING");
        }
        signals.addAll(delta.alternativeSignals());
        return distinctSorted(signals);
    }

    private static List<String> limitationSignals(
            DecisionExplorerFactorTradeoffDeltaV1 delta,
            ScenarioContext scenarioContext,
            String classification) {
        List<String> limitations = new ArrayList<>();
        if (scenarioContext.scenarios().isEmpty()) {
            limitations.add("policy-weight scenarios were unavailable for factor sensitivity");
        }
        if (scenarioContext.hasInsufficientEvidenceScenario()) {
            limitations.add("policy-weight scenario evidence is INSUFFICIENT_EVIDENCE");
        }
        if (DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_UNKNOWN.equals(classification)) {
            limitations.add("factor-weight delta is UNKNOWN from returned evidence");
        }
        if (DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_DEGRADED.equals(classification)) {
            limitations.add("factor-weight delta includes degraded evidence");
        }
        limitations.addAll(delta.limitationSignals());
        limitations.addAll(scenarioContext.limitationSignals());
        return distinctSorted(limitations);
    }

    private static List<String> reasonCodes(
            DecisionExplorerFactorTradeoffDeltaV1 delta,
            ScenarioContext scenarioContext,
            String classification,
            boolean selectedSupportStabilizesDecision,
            boolean alternativeSupportCanChallengeSelection) {
        List<String> reasons = new ArrayList<>();
        reasons.add("COUNTERFACTUAL_FACTOR_WEIGHT_DELTA_" + classification);
        reasons.add("BASELINE_FACTOR_DELTA_" + delta.deltaClassification());
        reasons.add("SELECTED_SUPPORT_SCENARIO_" + scenarioContext.selectedSupportLabel());
        reasons.add("ALTERNATIVE_SUPPORT_SCENARIO_" + scenarioContext.alternativeSupportLabel());
        if (selectedSupportStabilizesDecision) {
            reasons.add("SELECTED_SUPPORT_STABILIZES_DECISION");
        }
        if (alternativeSupportCanChallengeSelection) {
            reasons.add("ALTERNATIVE_SUPPORT_CAN_CHALLENGE_SELECTION");
        }
        reasons.addAll(delta.reasonCodes());
        return distinctSorted(reasons);
    }

    private static List<String> sourceReferenceIds(
            DecisionExplorerFactorTradeoffDeltaV1 delta,
            ScenarioContext scenarioContext) {
        return distinctSorted(Stream.concat(
                        delta.sourceReferenceIds().stream(),
                        scenarioContext.scenarios().stream()
                                .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::sourceReferenceIds)
                                .filter(Objects::nonNull)
                                .flatMap(Collection::stream))
                .toList());
    }

    private static String summaryText(
            DecisionExplorerFactorTradeoffDeltaV1 delta,
            String classification,
            DecisionExplorerCounterfactualPolicyWeightScenarioV1 strongestScenario,
            boolean selectedSupportStabilizesDecision,
            boolean alternativeSupportCanChallengeSelection) {
        String scenarioId = strongestScenario == null ? "UNKNOWN" : strongestScenario.scenarioId();
        return "Local factor-weight delta classifies factor " + delta.factorName() + " as " + classification
                + " from baseline factor tradeoff " + delta.deltaClassification()
                + " under strongest bounded scenario " + scenarioId
                + "; selectedSupportStabilizesDecision=" + selectedSupportStabilizesDecision
                + ", alternativeSupportCanChallengeSelection=" + alternativeSupportCanChallengeSelection
                + ", and no production routing, scoring, proxying, replay execution, storage, export, or traffic "
                + "shifting is performed.";
    }

    private record ScenarioContext(
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios,
            DecisionExplorerCounterfactualPolicyWeightScenarioV1 baselineScenario,
            DecisionExplorerCounterfactualPolicyWeightScenarioV1 selectedSupportScenario,
            DecisionExplorerCounterfactualPolicyWeightScenarioV1 alternativeSupportScenario,
            DecisionExplorerCounterfactualPolicyWeightScenarioV1 degradedScenario) {
        private static ScenarioContext from(List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios) {
            return new ScenarioContext(
                    scenarios,
                    findScenario(scenarios,
                            DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_BASELINE_RETURNED_EVIDENCE),
                    findScenario(scenarios,
                            DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_SELECTED_SUPPORT_PLUS_10),
                    findScenario(scenarios,
                            DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_ALTERNATIVE_SUPPORT_PLUS_10),
                    scenarios.stream()
                            .filter(scenario -> DecisionExplorerCounterfactualAnalysisV1.LABEL_DEGRADED.equals(
                                    scenario.sensitivityLabel()))
                            .findFirst()
                            .orElse(null));
        }

        private String selectedSupportLabel() {
            return selectedSupportScenario == null
                    ? DecisionExplorerCounterfactualAnalysisV1.LABEL_UNKNOWN
                    : selectedSupportScenario.sensitivityLabel();
        }

        private String alternativeSupportLabel() {
            return alternativeSupportScenario == null
                    ? DecisionExplorerCounterfactualAnalysisV1.LABEL_UNKNOWN
                    : alternativeSupportScenario.sensitivityLabel();
        }

        private boolean selectedCandidateRemainsSupported() {
            return selectedSupportScenario != null && selectedSupportScenario.selectedCandidateRemainsSupported();
        }

        private boolean alternativeBecomesCloseOrPreferable() {
            return alternativeSupportScenario != null
                    && alternativeSupportScenario.alternativeBecomesCloseOrPreferable();
        }

        private boolean hasNoComputedScenarioBasis() {
            return scenarios.isEmpty();
        }

        private boolean hasDegradedScenario() {
            return degradedScenario != null;
        }

        private boolean hasInsufficientEvidenceScenario() {
            return scenarios.stream()
                    .anyMatch(scenario -> DecisionExplorerCounterfactualAnalysisV1.LABEL_INSUFFICIENT_EVIDENCE.equals(
                            scenario.sensitivityLabel()));
        }

        private List<String> limitationSignals() {
            return distinctSorted(scenarios.stream()
                    .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::limitationSignals)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .toList());
        }

        private static DecisionExplorerCounterfactualPolicyWeightScenarioV1 findScenario(
                List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios,
                String scenarioId) {
            return scenarios.stream()
                    .filter(scenario -> scenarioId.equals(scenario.scenarioId()))
                    .findFirst()
                    .orElse(null);
        }
    }
}
