package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

@Service
public class DecisionExplorerPayloadService {
    private static final String SOURCE =
            "/api/routing/compare results[] already-built Decision Explorer Phase 1 evidence";
    private static final String READ_ONLY_GATE = "boundary-read-only";
    private static final String SIMULATION_ONLY_GATE = "boundary-simulation-only";
    private static final String BOUNDARY_NOTE =
            "Decision Explorer Phase 1 builder is read-only and simulation-only; it only reshapes "
                    + "already-built routing compare evidence and does not change routing behavior, execute replay, "
                    + "perform what-if mutation, persist storage, export evidence, call external systems, or claim "
                    + "production readiness.";
    private static final List<String> NOT_PROVEN_BOUNDARIES = List.of(
            "no production readiness",
            "no production certification",
            "no live-cloud validation",
            "no real-tenant validation",
            "no benchmark/load/stress proof",
            "no throughput/p95/p99 proof",
            "no replay/export proof",
            "no storage proof",
            "no evidence-packet generation",
            "no autonomous production action");
    private static final Comparator<CandidateReadoutV1> BY_SELECTED_THEN_CANDIDATE_ID = Comparator
            .comparing(CandidateReadoutV1::selected)
            .reversed()
            .thenComparing(CandidateReadoutV1::candidateId);
    private static final Comparator<FactorContributionV1> BY_CANDIDATE_THEN_FACTOR = Comparator
            .comparing(FactorContributionV1::candidateId)
            .thenComparing(FactorContributionV1::factorName);
    private static final Comparator<DecisionFactorDrilldownV1> BY_DRILLDOWN_CANDIDATE_THEN_FACTOR = Comparator
            .comparing(DecisionFactorDrilldownV1::candidateId)
            .thenComparing(DecisionFactorDrilldownV1::factorName);
    private static final Comparator<DecisionExplorerCandidateComparisonRowV1> BY_COMPARISON_ORDER = Comparator
            .comparingInt(DecisionExplorerCandidateComparisonRowV1::displayOrder)
            .thenComparing(DecisionExplorerCandidateComparisonRowV1::candidateId);
    private static final Comparator<PolicyGateReadoutV1> BY_GATE_ID =
            Comparator.comparing(PolicyGateReadoutV1::gateId);
    private static final Comparator<EvidencePacketReadoutV1> BY_REFERENCE_ID =
            Comparator.comparing(EvidencePacketReadoutV1::referenceId);

    public List<DecisionExplorerPayloadV1> buildPayloads(RoutingComparisonResponse comparison) {
        if (comparison == null || comparison.results() == null || comparison.results().isEmpty()) {
            return List.of(unknownPayload("Routing comparison response did not include result evidence."));
        }
        List<DecisionExplorerPayloadV1> payloads = comparison.results().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((RoutingComparisonResultResponse result) -> safeValue(result.strategyId()))
                        .thenComparing(result -> safeValue(selectedCandidateId(result))))
                .map(this::buildPayload)
                .toList();
        if (payloads.isEmpty()) {
            return List.of(unknownPayload("Routing comparison response contained only null result evidence."));
        }
        return payloads;
    }

    public DecisionExplorerPayloadV1 buildPayload(RoutingComparisonResultResponse result) {
        if (result == null) {
            return unknownPayload("Routing comparison result evidence was not provided.");
        }
        String selectedCandidateId = selectedCandidateId(result);
        List<CandidateReadoutV1> candidates = candidateReadouts(result, selectedCandidateId);
        CandidateReadoutV1 selectedCandidate = selectedCandidate(candidates, selectedCandidateId);
        List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons =
                candidateComparisons(candidates, selectedCandidate);
        List<FactorContributionV1> factorContributions = factorContributions(result);
        List<DecisionFactorDrilldownV1> factorDrilldowns = factorDrilldowns(result);
        List<PolicyGateReadoutV1> policyGateReadouts = policyGateReadouts(result);
        List<DecisionDiffReadoutV1> decisionDiffReadouts = decisionDiffReadouts(result, selectedCandidateId);
        List<EvidencePacketReadoutV1> evidencePacketReadouts = evidencePacketReadouts(result);
        List<String> warnings = warnings(result, candidates, factorContributions, decisionDiffReadouts);

        return new DecisionExplorerPayloadV1(
                true,
                true,
                DecisionExplorerPayloadV1.PAYLOAD_OBJECT,
                DecisionExplorerPayloadV1.CONTRACT_VERSION,
                SOURCE,
                decisionId(result.strategyId(), selectedCandidateId),
                decisionReadout(result, selectedCandidateId),
                selectedCandidate,
                candidates,
                candidateComparisons,
                factorContributions,
                factorDrilldowns,
                policyGateReadouts,
                decisionDiffReadouts,
                evidencePacketReadouts,
                agentStructuredOutput(),
                warnings,
                unknowns(result),
                NOT_PROVEN_BOUNDARIES,
                BOUNDARY_NOTE);
    }

    private DecisionExplorerPayloadV1 unknownPayload(String explanation) {
        CandidateReadoutV1 unknownCandidate = new CandidateReadoutV1(
                "UNKNOWN",
                "UNKNOWN",
                false,
                "UNKNOWN",
                null,
                List.of(),
                List.of("routing comparison evidence was not returned"),
                List.of("DECISION_EXPLORER_SOURCE_UNKNOWN"),
                List.of(READ_ONLY_GATE, SIMULATION_ONLY_GATE),
                List.of(),
                BOUNDARY_NOTE);
        return new DecisionExplorerPayloadV1(
                true,
                true,
                DecisionExplorerPayloadV1.PAYLOAD_OBJECT,
                DecisionExplorerPayloadV1.CONTRACT_VERSION,
                SOURCE,
                "routing-compare/unknown/unknown",
                new DecisionReadoutV1(
                        "routing-compare/unknown/unknown",
                        "UNKNOWN",
                        "UNKNOWN",
                        "UNKNOWN",
                        explanation,
                        List.of("DECISION_EXPLORER_SOURCE_UNKNOWN"),
                        List.of(),
                        BOUNDARY_NOTE),
                unknownCandidate,
                List.of(),
                List.of(),
                List.of(),
                policyGateReadouts(null),
                List.of(),
                evidencePacketReadouts(null),
                agentStructuredOutput(),
                List.of(explanation),
                List.of("routing comparison result evidence was unavailable"),
                NOT_PROVEN_BOUNDARIES,
                BOUNDARY_NOTE);
    }

    private static DecisionReadoutV1 decisionReadout(RoutingComparisonResultResponse result,
                                                     String selectedCandidateId) {
        String strategyId = safeValue(result.strategyId());
        String decisionId = decisionId(strategyId, selectedCandidateId);
        return new DecisionReadoutV1(
                decisionId,
                safeValue(result.status()),
                safeValue(selectedCandidateId),
                strategyId,
                decisionSummary(result, selectedCandidateId),
                decisionReasonCodes(result),
                sourceReferenceIds(result),
                BOUNDARY_NOTE);
    }

    private static String decisionSummary(RoutingComparisonResultResponse result, String selectedCandidateId) {
        if (isBlank(selectedCandidateId)) {
            return "Decision Explorer summarized available compare evidence without a selected candidate; "
                    + "no selected route is invented.";
        }
        return "Decision Explorer summarized strategy " + safeValue(result.strategyId())
                + " with selected candidate " + safeValue(selectedCandidateId)
                + " from already-built routing compare evidence only.";
    }

    private static List<String> decisionReasonCodes(RoutingComparisonResultResponse result) {
        List<String> reasons = new ArrayList<>();
        addIfPresent(reasons, "STATUS_" + safeValue(result.status()));
        if (!isBlank(selectedCandidateId(result))) {
            reasons.add("SELECTED_CANDIDATE_RETURNED");
        } else {
            reasons.add("SELECTED_CANDIDATE_UNKNOWN");
        }
        reasons.add(result.decisionVector() == null ? "DECISION_VECTOR_UNKNOWN" : "DECISION_VECTOR_AVAILABLE");
        reasons.add(result.scores() == null || result.scores().isEmpty()
                ? "FINAL_SCORES_UNKNOWN"
                : "FINAL_SCORES_RETURNED");
        return distinctSorted(reasons);
    }

    private static List<CandidateReadoutV1> candidateReadouts(
            RoutingComparisonResultResponse result,
            String selectedCandidateId) {
        Map<String, Double> scores = result.scores() == null ? Map.of() : result.scores();
        List<CandidateDecisionVectorResponse> vectorCandidates = vectorCandidates(result);
        Map<String, CandidateDecisionVectorResponse> vectorsByCandidateId = vectorCandidates.stream()
                .filter(candidate -> !isBlank(candidate.candidateId()))
                .collect(java.util.stream.Collectors.toMap(
                        candidate -> candidate.candidateId().trim(),
                        candidate -> candidate,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new));
        TreeSet<String> candidateIds = new TreeSet<>();
        candidateIds.addAll(vectorsByCandidateId.keySet());
        candidateIds.addAll(nonBlankValues(result.candidateServersConsidered()));
        candidateIds.addAll(nonBlankValues(scores.keySet()));
        if (!isBlank(selectedCandidateId)) {
            candidateIds.add(selectedCandidateId.trim());
        }

        return candidateIds.stream()
                .map(candidateId -> candidateReadout(
                        candidateId,
                        vectorsByCandidateId.get(candidateId),
                        scores.get(candidateId),
                        selectedCandidateId,
                        result.decisionVector() == null))
                .sorted(BY_SELECTED_THEN_CANDIDATE_ID)
                .toList();
    }

    private static CandidateReadoutV1 candidateReadout(
            String candidateId,
            CandidateDecisionVectorResponse vector,
            Double score,
            String selectedCandidateId,
            boolean decisionVectorMissing) {
        boolean selected = !isBlank(selectedCandidateId) && selectedCandidateId.trim().equals(candidateId)
                || vector != null && vector.selected();
        List<String> visibleSignals = vector == null
                ? List.of()
                : distinctSorted(vector.knownVisibleSignals());
        List<String> unknownSignals = vector == null
                ? List.of("candidate Decision Vector signals were not returned")
                : distinctSorted(vector.unknownOrUnexposedSignals());
        List<String> reasonCodes = candidateReasonCodes(selected, score, vector, decisionVectorMissing);
        return new CandidateReadoutV1(
                candidateId,
                candidateId,
                selected,
                selected ? "SELECTED" : "NOT_SELECTED",
                finiteOrNull(score),
                visibleSignals,
                unknownSignals,
                reasonCodes,
                List.of(READ_ONLY_GATE, SIMULATION_ONLY_GATE),
                candidateEvidenceReferences(candidateId, vector, score),
                vector == null ? BOUNDARY_NOTE : safeValue(vector.productionNotProvenBoundary()));
    }

    private static List<String> candidateReasonCodes(
            boolean selected,
            Double score,
            CandidateDecisionVectorResponse vector,
            boolean decisionVectorMissing) {
        List<String> reasonCodes = new ArrayList<>();
        reasonCodes.add(selected ? "SELECTED_CANDIDATE" : "NON_SELECTED_CANDIDATE");
        reasonCodes.add(decisionVectorMissing ? "DECISION_VECTOR_UNKNOWN" : "DECISION_VECTOR_AVAILABLE");
        if (finiteOrNull(score) != null) {
            reasonCodes.add("FINAL_SCORE_RETURNED");
        }
        if (vector != null && !vector.factorContributions().isEmpty()) {
            reasonCodes.add("FACTOR_CONTRIBUTIONS_RETURNED");
        }
        return distinctSorted(reasonCodes);
    }

    private static List<String> candidateEvidenceReferences(
            String candidateId,
            CandidateDecisionVectorResponse vector,
            Double score) {
        List<String> references = new ArrayList<>();
        if (vector != null) {
            references.add("decision-vector:" + candidateId);
        }
        if (finiteOrNull(score) != null) {
            references.add("scores:" + candidateId);
        }
        return distinctSorted(references);
    }

    private static CandidateReadoutV1 selectedCandidate(
            List<CandidateReadoutV1> candidates,
            String selectedCandidateId) {
        return candidates.stream()
                .filter(CandidateReadoutV1::selected)
                .findFirst()
                .orElseGet(() -> new CandidateReadoutV1(
                        safeValue(selectedCandidateId),
                        safeValue(selectedCandidateId),
                        false,
                        "UNKNOWN",
                        null,
                        List.of(),
                        List.of("selected candidate was not returned in visible evidence"),
                        List.of("SELECTED_CANDIDATE_UNKNOWN"),
                        List.of(READ_ONLY_GATE, SIMULATION_ONLY_GATE),
                        List.of(),
                        BOUNDARY_NOTE));
    }

    private static List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons(
            List<CandidateReadoutV1> candidates,
            CandidateReadoutV1 selectedCandidate) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Double selectedScore = selectedCandidate == null ? null : finiteOrNull(selectedCandidate.finalScore());
        List<CandidateReadoutV1> sortedCandidates = candidates.stream()
                .filter(Objects::nonNull)
                .sorted(BY_SELECTED_THEN_CANDIDATE_ID)
                .toList();
        List<DecisionExplorerCandidateComparisonRowV1> rows = new ArrayList<>();
        for (int index = 0; index < sortedCandidates.size(); index++) {
            CandidateReadoutV1 candidate = sortedCandidates.get(index);
            rows.add(candidateComparison(candidate, index + 1, selectedScore));
        }
        return rows.stream().sorted(BY_COMPARISON_ORDER).toList();
    }

    private static DecisionExplorerCandidateComparisonRowV1 candidateComparison(
            CandidateReadoutV1 candidate,
            int displayOrder,
            Double selectedScore) {
        Double finalScore = finiteOrNull(candidate.finalScore());
        Double scoreDelta = finalScore == null || selectedScore == null ? null : finalScore - selectedScore;
        List<String> warnings = candidateComparisonWarnings(candidate, finalScore, selectedScore);
        List<String> unknowns = candidateComparisonUnknowns(candidate, scoreDelta);
        return new DecisionExplorerCandidateComparisonRowV1(
                candidate.candidateId(),
                candidate.candidateLabel(),
                candidate.selected(),
                displayOrder,
                candidateComparisonStatus(candidate, finalScore, selectedScore),
                finalScore,
                scoreDelta,
                candidate.visibleSignals(),
                candidate.unknownSignals(),
                candidate.reasonCodes(),
                candidate.policyGateIds(),
                candidate.evidenceReferenceIds(),
                warnings,
                unknowns,
                candidate.boundaryNote());
    }

    private static String candidateComparisonStatus(
            CandidateReadoutV1 candidate,
            Double finalScore,
            Double selectedScore) {
        if (candidate.selected()) {
            return finalScore == null ? "SELECTED_SCORE_UNKNOWN" : "SELECTED";
        }
        if (finalScore == null || selectedScore == null) {
            return "PARTIAL_EVIDENCE";
        }
        return "COMPARED_TO_SELECTED";
    }

    private static List<String> candidateComparisonWarnings(
            CandidateReadoutV1 candidate,
            Double finalScore,
            Double selectedScore) {
        List<String> warnings = new ArrayList<>();
        if (finalScore == null) {
            warnings.add("candidate final score was not returned");
        }
        if (!candidate.selected() && selectedScore == null) {
            warnings.add("selected candidate final score was not returned");
        }
        if (candidate.evidenceReferenceIds().isEmpty()) {
            warnings.add("candidate evidence references were not returned");
        }
        return distinctSorted(warnings);
    }

    private static List<String> candidateComparisonUnknowns(
            CandidateReadoutV1 candidate,
            Double scoreDelta) {
        List<String> unknowns = new ArrayList<>(candidate.unknownSignals());
        if (!candidate.selected() && scoreDelta == null) {
            unknowns.add("score delta from selected candidate");
        }
        unknowns.add("hidden routing internals");
        return distinctSorted(unknowns);
    }

    private static List<FactorContributionV1> factorContributions(RoutingComparisonResultResponse result) {
        return vectorCandidates(result).stream()
                .flatMap(candidate -> candidate.factorContributions().stream()
                        .filter(Objects::nonNull)
                        .map(contribution -> new FactorContributionV1(
                                contribution.factorName(),
                                candidate.candidateId(),
                                contribution.direction(),
                                finiteOrNull(contribution.contributionValue()),
                                contribution.exactness(),
                                contribution.explanationText(),
                                List.of("decision-vector:" + safeValue(candidate.candidateId())),
                                contribution.boundaryNote())))
                .sorted(BY_CANDIDATE_THEN_FACTOR)
                .toList();
    }

    private static List<DecisionFactorDrilldownV1> factorDrilldowns(RoutingComparisonResultResponse result) {
        return vectorCandidates(result).stream()
                .flatMap(candidate -> candidate.factorContributions().stream()
                        .filter(Objects::nonNull)
                        .map(contribution -> factorDrilldown(candidate, contribution)))
                .sorted(BY_DRILLDOWN_CANDIDATE_THEN_FACTOR)
                .toList();
    }

    private static DecisionFactorDrilldownV1 factorDrilldown(
            CandidateDecisionVectorResponse candidate,
            ScoreFactorContributionResponse contribution) {
        String candidateId = safeValue(candidate.candidateId());
        String factorName = safeValue(contribution.factorName());
        String observedValue = !isBlank(contribution.rawValueDescription())
                ? contribution.rawValueDescription().trim()
                : safeValue(contribution.contributionDescription());
        String evidenceStatus = factorEvidenceStatus(contribution);
        List<String> warnings = factorWarnings(contribution, evidenceStatus);
        List<String> unknowns = factorUnknowns(contribution);
        return new DecisionFactorDrilldownV1(
                factorName,
                candidateId,
                observedValue,
                influenceCategory(contribution),
                evidenceStatus,
                safeValue(contribution.explanationText()),
                warnings,
                unknowns,
                List.of(
                        "decision-vector:" + candidateId,
                        "factor-contribution:" + candidateId + ":" + normalizedId(factorName)),
                safeValue(contribution.boundaryNote()));
    }

    private static String factorEvidenceStatus(ScoreFactorContributionResponse contribution) {
        boolean hasObservedValue = !isBlank(contribution.rawValueDescription());
        boolean hasExplanation = !isBlank(contribution.explanationText());
        boolean hasFiniteContribution = finiteOrNull(contribution.contributionValue()) != null;
        if (hasObservedValue && hasExplanation && hasFiniteContribution) {
            return "AVAILABLE";
        }
        if (hasObservedValue || hasExplanation || hasFiniteContribution) {
            return "PARTIAL";
        }
        return "UNKNOWN";
    }

    private static String influenceCategory(ScoreFactorContributionResponse contribution) {
        String direction = safeValue(contribution.direction());
        if ("SUPPORTS_SELECTION".equals(direction) || "WEAKENS_SELECTION".equals(direction)) {
            return direction;
        }
        Double value = finiteOrNull(contribution.contributionValue());
        if (value == null) {
            return "UNKNOWN_INFLUENCE";
        }
        if (value < 0.0) {
            return "SUPPORTS_SELECTION";
        }
        if (value > 0.0) {
            return "WEAKENS_SELECTION";
        }
        return "NEUTRAL";
    }

    private static List<String> factorWarnings(
            ScoreFactorContributionResponse contribution,
            String evidenceStatus) {
        List<String> warnings = new ArrayList<>();
        if (!"AVAILABLE".equals(evidenceStatus)) {
            warnings.add("factor evidence is " + evidenceStatus.toLowerCase(Locale.ROOT));
        }
        if (finiteOrNull(contribution.contributionValue()) == null) {
            warnings.add("finite contribution value was not returned");
        }
        if (!"EXACT_FROM_RETURNED_EVIDENCE".equals(safeValue(contribution.exactness()))) {
            warnings.add("factor exactness is " + safeValue(contribution.exactness()));
        }
        return distinctSorted(warnings);
    }

    private static List<String> factorUnknowns(ScoreFactorContributionResponse contribution) {
        List<String> unknowns = new ArrayList<>();
        if (isBlank(contribution.rawValueDescription())) {
            unknowns.add("observed value or status");
        }
        if (finiteOrNull(contribution.contributionValue()) == null) {
            unknowns.add("numeric contribution value");
        }
        unknowns.add("hidden routing internals");
        return distinctSorted(unknowns);
    }

    private static List<PolicyGateReadoutV1> policyGateReadouts(RoutingComparisonResultResponse result) {
        List<PolicyGateReadoutV1> gates = new ArrayList<>();
        gates.add(new PolicyGateReadoutV1(
                READ_ONLY_GATE,
                "Read-only boundary",
                "AVAILABLE",
                "PASS",
                "Decision Explorer Phase 1 builder only reshapes already-built compare evidence.",
                List.of("docs/agent/DECISION_EXPLORER_PHASE1_ARCHITECTURE_SCOPE.md"),
                BOUNDARY_NOTE));
        gates.add(new PolicyGateReadoutV1(
                SIMULATION_ONLY_GATE,
                "Simulation-only boundary",
                "AVAILABLE",
                "PASS",
                "Decision Explorer Phase 1 builder does not shift traffic, mutate routing, or call external systems.",
                List.of("docs/agent/DECISION_EXPLORER_PHASE1_ARCHITECTURE_SCOPE.md"),
                BOUNDARY_NOTE));
        if (result != null && result.decisionReplayReadinessChecklist() != null) {
            result.decisionReplayReadinessChecklist().checklistItems().stream()
                    .filter(Objects::nonNull)
                    .map(DecisionExplorerPayloadService::policyGateFromChecklistItem)
                    .forEach(gates::add);
        }
        return gates.stream().sorted(BY_GATE_ID).toList();
    }

    private static PolicyGateReadoutV1 policyGateFromChecklistItem(
            DecisionReplayReadinessChecklistItemResponse item) {
        String status = safeValue(item.status());
        String explanation = !isBlank(item.explanation())
                ? item.explanation().trim()
                : safeValue(item.missingEvidenceReason());
        return new PolicyGateReadoutV1(
                "readiness-" + normalizedId(item.itemId()),
                safeValue(item.label()),
                status,
                outcomeFor(status),
                explanation,
                List.of(safeValue(item.evidenceSourceFieldPath())),
                BOUNDARY_NOTE);
    }

    private static List<DecisionDiffReadoutV1> decisionDiffReadouts(
            RoutingComparisonResultResponse result,
            String selectedCandidateId) {
        RoutingDecisionDeltaAnalysisResponse delta = result.decisionDeltaAnalysis();
        if (delta == null) {
            return List.of();
        }
        CandidateDecisionDeltaResponse comparison = delta.comparison();
        if (comparison == null) {
            return List.of(new DecisionDiffReadoutV1(
                    selectedCandidateId,
                    null,
                    delta.status(),
                    null,
                    List.of(),
                    List.of(),
                    List.of("decision-delta-analysis"),
                    delta.explanation(),
                    delta.boundaryNote()));
        }
        return List.of(new DecisionDiffReadoutV1(
                comparison.selectedCandidateId(),
                comparison.closestAlternativeCandidateId(),
                delta.status(),
                finiteOrNull(comparison.finalScoreGap()),
                distinctSorted(comparison.comparedFactorNames()),
                distinctSorted(comparison.omittedFactorNames()),
                List.of("decision-delta-analysis"),
                comparison.explanation(),
                delta.boundaryNote()));
    }

    private static List<EvidencePacketReadoutV1> evidencePacketReadouts(RoutingComparisonResultResponse result) {
        if (result != null && result.decisionReplayEvidenceSourceMap() != null
                && !result.decisionReplayEvidenceSourceMap().sourceMapEntries().isEmpty()) {
            return result.decisionReplayEvidenceSourceMap().sourceMapEntries().stream()
                    .filter(Objects::nonNull)
                    .map(DecisionExplorerPayloadService::evidenceReadoutFromSourceMap)
                    .sorted(BY_REFERENCE_ID)
                    .toList();
        }
        return List.of(new EvidencePacketReadoutV1(
                "future-evidence-packet",
                "NOT_IMPLEMENTED",
                "docs/agent/DECISION_EXPLORER_EVIDENCE_LANE.md",
                "PLANNED",
                List.of("Decision Explorer Phase 1 builder does not generate, persist, export, or replay "
                        + "evidence packets."),
                BOUNDARY_NOTE));
    }

    private static EvidencePacketReadoutV1 evidenceReadoutFromSourceMap(
            DecisionReplayEvidenceSourceMapEntryResponse entry) {
        String status = safeValue(entry.status());
        List<String> unavailableReasons = "AVAILABLE".equals(status)
                ? List.of()
                : List.of("source map entry status is " + status);
        return new EvidencePacketReadoutV1(
                entry.sourceId(),
                status,
                entry.sourceFieldPath(),
                "SOURCE_VISIBLE",
                unavailableReasons,
                "Reference-only source map readout; no evidence packet generation, storage, export, or replay "
                        + "execution is implemented.");
    }

    private static AgentStructuredOutputV1 agentStructuredOutput() {
        return new AgentStructuredOutputV1(
                AgentStructuredOutputV1.SCHEMA_NAME,
                AgentStructuredOutputV1.SCHEMA_VERSION,
                List.of(
                        "payloadObject",
                        "contractVersion",
                        "decisionReadout",
                        "selectedCandidate",
                        "candidateSet",
                        "candidateComparisons",
                        "factorContributions",
                        "factorDrilldowns",
                        "policyGateReadouts",
                        "decisionDiffReadouts",
                        "evidencePacketReadouts",
                        "warnings",
                        "unknowns",
                        "notProvenBoundaries"),
                List.of(
                        "use stable field names",
                        "treat null scores and empty lists as unknown or unavailable evidence",
                        "do not infer hidden routing internals from missing values",
                        "sort repeated readouts deterministically"),
                List.of(
                        "Which candidate was selected?",
                        "Which visible factors contributed to the selected decision?",
                        "Which boundaries remain not proven?"),
                List.of(
                        "no autonomous production action",
                        "no live mutation",
                        "no hidden side effects",
                        "no replay/export execution"),
                NOT_PROVEN_BOUNDARIES,
                "Agent structured output is for reviewer understanding only.");
    }

    private static List<String> warnings(
            RoutingComparisonResultResponse result,
            List<CandidateReadoutV1> candidates,
            List<FactorContributionV1> factorContributions,
            List<DecisionDiffReadoutV1> decisionDiffReadouts) {
        List<String> warnings = new ArrayList<>();
        if (isBlank(selectedCandidateId(result))) {
            warnings.add("Selected candidate was not returned.");
        }
        if (candidates.isEmpty()) {
            warnings.add("Candidate set was not returned.");
        }
        if (result.decisionVector() == null) {
            warnings.add("Decision Vector evidence was not returned; candidate and factor details are partial.");
        }
        if (factorContributions.isEmpty()) {
            warnings.add("Factor contribution evidence was not returned.");
        }
        if (decisionDiffReadouts.isEmpty()) {
            warnings.add("Decision diff evidence was not returned.");
        }
        warnings.add("Decision Explorer payload is read-only and simulation-only; it does not change routing behavior.");
        return distinctSorted(warnings);
    }

    private static List<String> unknowns(RoutingComparisonResultResponse result) {
        List<String> unknowns = new ArrayList<>();
        if (result.decisionVector() == null) {
            unknowns.add("candidate-level Decision Vector evidence");
        } else {
            unknowns.addAll(result.decisionVector().unknownOrUnexposedSignals());
        }
        unknowns.add("hidden routing internals");
        unknowns.add("exact production scoring");
        unknowns.add("live-cloud behavior");
        unknowns.add("real-tenant behavior");
        unknowns.add("benchmark/load/stress behavior");
        unknowns.add("replay/export/storage behavior");
        return distinctSorted(unknowns);
    }

    private static List<String> sourceReferenceIds(RoutingComparisonResultResponse result) {
        List<String> references = new ArrayList<>();
        references.add("routing-comparison-result");
        if (result.decisionVector() != null) {
            references.add("decision-vector");
        }
        if (result.dominantFactorAnalysis() != null) {
            references.add("dominant-factor-analysis");
        }
        if (result.decisionDeltaAnalysis() != null) {
            references.add("decision-delta-analysis");
        }
        if (result.decisionReplayEvidenceSourceMap() != null) {
            references.add("evidence-source-map");
        }
        return distinctSorted(references);
    }

    private static List<CandidateDecisionVectorResponse> vectorCandidates(RoutingComparisonResultResponse result) {
        if (result == null || result.decisionVector() == null
                || result.decisionVector().candidateSummaries() == null) {
            return List.of();
        }
        return result.decisionVector().candidateSummaries().stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private static String selectedCandidateId(RoutingComparisonResultResponse result) {
        if (result == null) {
            return null;
        }
        if (!isBlank(result.chosenServerId())) {
            return result.chosenServerId().trim();
        }
        RoutingDecisionVectorResponse decisionVector = result.decisionVector();
        if (decisionVector != null && decisionVector.selectedCandidateVector() != null
                && !isBlank(decisionVector.selectedCandidateVector().candidateId())) {
            return decisionVector.selectedCandidateVector().candidateId().trim();
        }
        if (decisionVector != null && !isBlank(decisionVector.selectedBackend())) {
            return decisionVector.selectedBackend().trim();
        }
        return null;
    }

    private static String decisionId(String strategyId, String selectedCandidateId) {
        return "routing-compare/" + normalizedId(strategyId) + "/" + normalizedId(selectedCandidateId);
    }

    private static String outcomeFor(String status) {
        return switch (status) {
            case "AVAILABLE" -> "PASS";
            case "PARTIAL" -> "REVIEW";
            case "UNKNOWN" -> "UNKNOWN";
            default -> status;
        };
    }

    private static List<String> nonBlankValues(Collection<String> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .filter(value -> !isBlank(value))
                        .map(String::trim)
                        .toList();
    }

    private static List<String> distinctSorted(Collection<String> values) {
        if (values == null) {
            return List.of();
        }
        Set<String> distinct = new LinkedHashSet<>();
        values.stream()
                .filter(value -> !isBlank(value))
                .map(String::trim)
                .forEach(distinct::add);
        return distinct.stream().sorted().toList();
    }

    private static Double finiteOrNull(Double value) {
        return value == null || !Double.isFinite(value) ? null : value;
    }

    private static void addIfPresent(List<String> values, String value) {
        if (!isBlank(value)) {
            values.add(value.trim());
        }
    }

    private static String safeValue(String value) {
        return isBlank(value) ? "UNKNOWN" : value.trim();
    }

    private static String normalizedId(String value) {
        if (isBlank(value)) {
            return "unknown";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
