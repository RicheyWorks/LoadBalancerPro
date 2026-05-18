package com.richmond423.loadbalancerpro.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

public final class RoutingDecisionReplayCapsuleService {
    private static final String SCHEMA_VERSION = "decision-replay-capsule/v1";
    private static final String SOURCE =
            "/api/routing/compare already-built lab evidence: scores, decisionVector, dominantFactorAnalysis, "
                    + "decisionDeltaAnalysis, decisionReplaySnapshot, and decisionReplayReconstructionTrace";
    private static final String FINGERPRINT_ALGORITHM =
            "SHA-256 over stable replay capsule fields; local deterministic fingerprint only";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision Replay Capsule is read-only canonical lab evidence packaging derived only from already-built "
                    + "routing compare response data; it does not persist capsules or audit logs, execute replay, "
                    + "perform what-if mutation, recompute scores, retune weights, change routing behavior, add "
                    + "telemetry, add upload/share/download flows, or add server-side export/PDF/ZIP generation.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "No production certification, live-cloud proof, real-tenant proof, SLA/SLO proof, registry "
                    + "publication proof, container signing proof, governance application proof, production traffic "
                    + "validation, exact production scoring proof, cryptographic production proof, guaranteed replay, "
                    + "or production readiness proof is claimed.";
    private static final Comparator<CandidateDecisionVectorResponse> BY_CANDIDATE_ID = Comparator
            .comparing(candidate -> safeValue(candidate == null ? null : candidate.candidateId()));

    public RoutingDecisionReplayCapsuleResponse capsule(
            String strategyId,
            String chosenServerId,
            List<String> candidateServersConsidered,
            Map<String, Double> scores,
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace) {
        List<String> candidateIds = candidateIds(
                candidateServersConsidered,
                scores,
                decisionVector,
                replaySnapshot,
                reconstructionTrace);
        Map<String, Double> finiteScores = finiteScores(scores, reconstructionTrace, candidateIds);
        String selectedCandidateId = selectedCandidateId(
                chosenServerId,
                decisionVector,
                decisionDeltaAnalysis,
                replaySnapshot,
                reconstructionTrace);
        String closestAlternativeId = closestAlternativeId(
                decisionDeltaAnalysis,
                replaySnapshot,
                reconstructionTrace,
                candidateIds);
        Double finalScoreGap = finalScoreGap(decisionDeltaAnalysis, replaySnapshot, reconstructionTrace);
        String largestDeltaFactorName = largestDeltaFactorName(
                decisionDeltaAnalysis,
                replaySnapshot,
                reconstructionTrace);
        String decisionVectorStatus = decisionVectorStatus(decisionVector);
        String factorContributionStatus = factorContributionStatus(decisionVector, reconstructionTrace);
        String dominantStatus = analysisStatus(dominantFactorAnalysis == null
                ? null
                : dominantFactorAnalysis.status());
        String deltaStatus = analysisStatus(decisionDeltaAnalysis == null
                ? null
                : decisionDeltaAnalysis.status());
        String snapshotStatus = analysisStatus(replaySnapshot == null ? null : replaySnapshot.status());
        String traceStatus = analysisStatus(reconstructionTrace == null ? null : reconstructionTrace.status());
        String snapshotFingerprint = fingerprintOrNull(replaySnapshot == null
                ? null
                : replaySnapshot.snapshotFingerprint());
        String traceFingerprint = fingerprintOrNull(reconstructionTrace == null
                ? null
                : reconstructionTrace.traceFingerprint());
        List<String> reconstructionStepIds = reconstructionStepIds(reconstructionTrace);
        List<DecisionReplayCapsuleCandidateEvidenceResponse> candidateEvidence = candidateEvidence(
                candidateIds,
                finiteScores,
                selectedCandidateId,
                decisionVector,
                dominantFactorAnalysis);
        List<DecisionReplayCapsuleFactorEvidenceResponse> factorEvidence = factorEvidence(
                selectedCandidateId,
                closestAlternativeId,
                decisionVector,
                decisionDeltaAnalysis,
                largestDeltaFactorName);
        String status = capsuleStatus(
                candidateIds,
                selectedCandidateId,
                decisionVectorStatus,
                factorContributionStatus,
                snapshotFingerprint,
                traceFingerprint,
                candidateEvidence,
                factorEvidence);
        String fingerprint = fingerprint(
                status,
                strategyId,
                selectedCandidateId,
                candidateIds,
                finiteScores,
                closestAlternativeId,
                finalScoreGap,
                largestDeltaFactorName,
                snapshotFingerprint,
                traceFingerprint,
                decisionVectorStatus,
                factorContributionStatus,
                dominantStatus,
                deltaStatus,
                snapshotStatus,
                traceStatus,
                reconstructionStepIds,
                candidateEvidence,
                factorEvidence);
        String explanation = explanation(
                status,
                strategyId,
                selectedCandidateId,
                candidateIds,
                closestAlternativeId,
                finalScoreGap,
                largestDeltaFactorName,
                snapshotFingerprint,
                traceFingerprint,
                candidateEvidence.size(),
                factorEvidence.size());

        return new RoutingDecisionReplayCapsuleResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                status,
                fingerprint,
                FINGERPRINT_ALGORITHM,
                selectedCandidateId,
                candidateIds,
                candidateIds.size(),
                closestAlternativeId,
                finalScoreGap,
                largestDeltaFactorName,
                snapshotFingerprint,
                traceFingerprint,
                safeValue(strategyId),
                decisionVectorStatus,
                factorContributionStatus,
                dominantStatus,
                deltaStatus,
                snapshotStatus,
                traceStatus,
                reconstructionStepIds,
                candidateEvidence,
                factorEvidence,
                explanation,
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    private static List<String> candidateIds(
            List<String> candidateServersConsidered,
            Map<String, Double> scores,
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace) {
        TreeSet<String> ids = new TreeSet<>();
        addAll(ids, candidateServersConsidered);
        if (scores != null) {
            addAll(ids, scores.keySet().stream().toList());
        }
        if (decisionVector != null && decisionVector.candidateSummaries() != null) {
            decisionVector.candidateSummaries().stream()
                    .filter(candidate -> candidate != null && !isBlank(candidate.candidateId()))
                    .map(candidate -> candidate.candidateId().trim())
                    .forEach(ids::add);
        }
        if (replaySnapshot != null) {
            addAll(ids, replaySnapshot.candidateIdsConsidered());
        }
        if (reconstructionTrace != null) {
            addAll(ids, reconstructionTrace.candidateIdsConsidered());
            if (reconstructionTrace.candidateFinalScores() != null) {
                addAll(ids, reconstructionTrace.candidateFinalScores().keySet().stream().toList());
            }
        }
        return List.copyOf(ids);
    }

    private static void addAll(TreeSet<String> values, List<String> candidates) {
        if (candidates == null) {
            return;
        }
        candidates.stream()
                .filter(value -> !isBlank(value))
                .map(String::trim)
                .forEach(values::add);
    }

    private static Map<String, Double> finiteScores(
            Map<String, Double> scores,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            List<String> candidateIds) {
        LinkedHashMap<String, Double> finiteScores = new LinkedHashMap<>();
        TreeSet<String> ids = new TreeSet<>(candidateIds);
        if (scores != null) {
            scores.keySet().stream()
                    .filter(value -> !isBlank(value))
                    .map(String::trim)
                    .forEach(ids::add);
        }
        if (reconstructionTrace != null && reconstructionTrace.candidateFinalScores() != null) {
            reconstructionTrace.candidateFinalScores().keySet().stream()
                    .filter(value -> !isBlank(value))
                    .map(String::trim)
                    .forEach(ids::add);
        }
        for (String candidateId : ids) {
            Double score = scores == null ? null : scores.get(candidateId);
            if (isFinite(score)) {
                finiteScores.put(candidateId, score);
                continue;
            }
            Double traceScore = reconstructionTrace == null || reconstructionTrace.candidateFinalScores() == null
                    ? null
                    : reconstructionTrace.candidateFinalScores().get(candidateId);
            if (isFinite(traceScore)) {
                finiteScores.put(candidateId, traceScore);
            }
        }
        return Map.copyOf(finiteScores);
    }

    private static String selectedCandidateId(
            String chosenServerId,
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace) {
        if (!isBlank(chosenServerId)) {
            return chosenServerId.trim();
        }
        if (decisionVector != null && decisionVector.selectedCandidateVector() != null
                && !isBlank(decisionVector.selectedCandidateVector().candidateId())) {
            return decisionVector.selectedCandidateVector().candidateId().trim();
        }
        if (decisionVector != null && !isBlank(decisionVector.selectedBackend())) {
            return decisionVector.selectedBackend().trim();
        }
        if (decisionDeltaAnalysis != null && decisionDeltaAnalysis.comparison() != null
                && !isBlank(decisionDeltaAnalysis.comparison().selectedCandidateId())) {
            return decisionDeltaAnalysis.comparison().selectedCandidateId().trim();
        }
        if (replaySnapshot != null && !isBlank(replaySnapshot.selectedCandidateId())) {
            return replaySnapshot.selectedCandidateId().trim();
        }
        if (reconstructionTrace != null && !isBlank(reconstructionTrace.selectedCandidateId())) {
            return reconstructionTrace.selectedCandidateId().trim();
        }
        return null;
    }

    private static String closestAlternativeId(
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            List<String> candidateIds) {
        String candidateId = null;
        if (decisionDeltaAnalysis != null && decisionDeltaAnalysis.comparison() != null
                && !isBlank(decisionDeltaAnalysis.comparison().closestAlternativeCandidateId())) {
            candidateId = decisionDeltaAnalysis.comparison().closestAlternativeCandidateId().trim();
        } else if (replaySnapshot != null && !isBlank(replaySnapshot.closestAlternativeCandidateId())) {
            candidateId = replaySnapshot.closestAlternativeCandidateId().trim();
        } else if (reconstructionTrace != null && !isBlank(reconstructionTrace.closestAlternativeCandidateId())) {
            candidateId = reconstructionTrace.closestAlternativeCandidateId().trim();
        }
        if (isBlank(candidateId)) {
            return null;
        }
        return candidateIds.isEmpty() || candidateIds.contains(candidateId) ? candidateId : null;
    }

    private static Double finalScoreGap(
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace) {
        if (decisionDeltaAnalysis != null && decisionDeltaAnalysis.comparison() != null
                && isFinite(decisionDeltaAnalysis.comparison().finalScoreGap())) {
            return decisionDeltaAnalysis.comparison().finalScoreGap();
        }
        if (replaySnapshot != null && isFinite(replaySnapshot.finalScoreGap())) {
            return replaySnapshot.finalScoreGap();
        }
        if (reconstructionTrace != null && isFinite(reconstructionTrace.finalScoreGap())) {
            return reconstructionTrace.finalScoreGap();
        }
        return null;
    }

    private static String largestDeltaFactorName(
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace) {
        if (decisionDeltaAnalysis != null && decisionDeltaAnalysis.largestAbsoluteFactorDelta() != null
                && !isBlank(decisionDeltaAnalysis.largestAbsoluteFactorDelta().factorName())) {
            return decisionDeltaAnalysis.largestAbsoluteFactorDelta().factorName().trim();
        }
        if (replaySnapshot != null && !isBlank(replaySnapshot.largestDeltaFactorName())) {
            return replaySnapshot.largestDeltaFactorName().trim();
        }
        if (reconstructionTrace != null && !isBlank(reconstructionTrace.largestDeltaFactorName())) {
            return reconstructionTrace.largestDeltaFactorName().trim();
        }
        return null;
    }

    private static String decisionVectorStatus(RoutingDecisionVectorResponse decisionVector) {
        if (decisionVector == null || decisionVector.candidateSummaries() == null
                || decisionVector.candidateSummaries().isEmpty()) {
            return STATUS_UNKNOWN;
        }
        return STATUS_AVAILABLE;
    }

    private static String factorContributionStatus(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace) {
        if (reconstructionTrace != null && !isBlank(reconstructionTrace.factorContributionStatus())) {
            return analysisStatus(reconstructionTrace.factorContributionStatus());
        }
        if (decisionVector == null || decisionVector.candidateSummaries() == null
                || decisionVector.candidateSummaries().isEmpty()) {
            return STATUS_UNKNOWN;
        }
        List<CandidateDecisionVectorResponse> candidates = decisionVector.candidateSummaries().stream()
                .filter(candidate -> candidate != null && !isBlank(candidate.candidateId()))
                .sorted(BY_CANDIDATE_ID)
                .toList();
        if (candidates.isEmpty()) {
            return STATUS_UNKNOWN;
        }
        int withContributions = 0;
        for (CandidateDecisionVectorResponse candidate : candidates) {
            if (candidate.factorContributions() != null
                    && candidate.factorContributions().stream()
                            .anyMatch(RoutingDecisionReplayCapsuleService::hasFiniteNamedContribution)) {
                withContributions++;
            }
        }
        if (withContributions == 0) {
            return STATUS_UNKNOWN;
        }
        return withContributions == candidates.size() ? STATUS_AVAILABLE : STATUS_PARTIAL;
    }

    private static List<DecisionReplayCapsuleCandidateEvidenceResponse> candidateEvidence(
            List<String> candidateIds,
            Map<String, Double> finiteScores,
            String selectedCandidateId,
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis) {
        List<DecisionReplayCapsuleCandidateEvidenceResponse> evidence = new ArrayList<>();
        for (String candidateId : candidateIds) {
            CandidateDecisionVectorResponse candidate = candidateById(decisionVector, candidateId).orElse(null);
            List<String> factorNames = factorNames(candidate);
            List<String> dominantFactorNames = dominantFactorNames(dominantFactorAnalysis, candidateId);
            Double finalScore = finiteScores.get(candidateId);
            String status = candidateEvidenceStatus(candidateId, finalScore, factorNames, dominantFactorNames);
            evidence.add(new DecisionReplayCapsuleCandidateEvidenceResponse(
                    candidateId,
                    !isBlank(selectedCandidateId) && selectedCandidateId.equals(candidateId),
                    finalScore,
                    factorNames,
                    finiteContributionCount(candidate),
                    dominantFactorNames,
                    status));
        }
        return List.copyOf(evidence);
    }

    private static Optional<CandidateDecisionVectorResponse> candidateById(
            RoutingDecisionVectorResponse decisionVector,
            String candidateId) {
        if (decisionVector == null || decisionVector.candidateSummaries() == null || isBlank(candidateId)) {
            return Optional.empty();
        }
        return decisionVector.candidateSummaries().stream()
                .filter(candidate -> candidate != null && candidateId.equals(candidate.candidateId()))
                .findFirst();
    }

    private static List<String> factorNames(CandidateDecisionVectorResponse candidate) {
        if (candidate == null || candidate.factorContributions() == null) {
            return List.of();
        }
        TreeSet<String> names = new TreeSet<>();
        candidate.factorContributions().stream()
                .filter(RoutingDecisionReplayCapsuleService::hasFiniteNamedContribution)
                .map(contribution -> contribution.factorName().trim())
                .forEach(names::add);
        return List.copyOf(names);
    }

    private static int finiteContributionCount(CandidateDecisionVectorResponse candidate) {
        if (candidate == null || candidate.factorContributions() == null) {
            return 0;
        }
        return (int) candidate.factorContributions().stream()
                .filter(RoutingDecisionReplayCapsuleService::hasFiniteNamedContribution)
                .count();
    }

    private static List<String> dominantFactorNames(
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            String candidateId) {
        if (dominantFactorAnalysis == null || dominantFactorAnalysis.candidateAnalyses() == null
                || isBlank(candidateId)) {
            return List.of();
        }
        TreeSet<String> names = new TreeSet<>();
        dominantFactorAnalysis.candidateAnalyses().stream()
                .filter(analysis -> analysis != null && candidateId.equals(analysis.candidateId()))
                .findFirst()
                .ifPresent(analysis -> {
                    addDominantFactorName(names, analysis.largestPositiveContributor());
                    addDominantFactorName(names, analysis.largestPenaltyContributor());
                    addDominantFactorName(names, analysis.largestAbsoluteImpact());
                });
        return List.copyOf(names);
    }

    private static void addDominantFactorName(TreeSet<String> names, DominantFactorResponse factor) {
        if (factor != null && !isBlank(factor.factorName())) {
            names.add(factor.factorName().trim());
        }
    }

    private static String candidateEvidenceStatus(
            String candidateId,
            Double finalScore,
            List<String> factorNames,
            List<String> dominantFactorNames) {
        if (isBlank(candidateId)) {
            return STATUS_UNKNOWN;
        }
        if (isFinite(finalScore) && !factorNames.isEmpty() && !dominantFactorNames.isEmpty()) {
            return STATUS_AVAILABLE;
        }
        if (isFinite(finalScore) || !factorNames.isEmpty() || !dominantFactorNames.isEmpty()) {
            return STATUS_PARTIAL;
        }
        return STATUS_UNKNOWN;
    }

    private static List<DecisionReplayCapsuleFactorEvidenceResponse> factorEvidence(
            String selectedCandidateId,
            String closestAlternativeId,
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            String largestDeltaFactorName) {
        Map<String, ScoreFactorContributionResponse> selectedContributions = finiteContributionsFor(
                decisionVector,
                selectedCandidateId);
        Map<String, ScoreFactorContributionResponse> alternativeContributions = finiteContributionsFor(
                decisionVector,
                closestAlternativeId);
        Map<String, ScoreFactorDeltaResponse> deltas = factorDeltasByName(decisionDeltaAnalysis);
        TreeSet<String> factorNames = new TreeSet<>();
        factorNames.addAll(selectedContributions.keySet());
        factorNames.addAll(alternativeContributions.keySet());
        factorNames.addAll(deltas.keySet());
        if (!isBlank(largestDeltaFactorName)) {
            factorNames.add(largestDeltaFactorName.trim());
        }

        List<DecisionReplayCapsuleFactorEvidenceResponse> evidence = new ArrayList<>();
        for (String factorName : factorNames) {
            ScoreFactorDeltaResponse delta = deltas.get(factorName);
            Double selectedContribution = finiteOrNull(delta == null
                    ? contributionValue(selectedContributions.get(factorName))
                    : delta.selectedCandidateContribution());
            Double alternativeContribution = finiteOrNull(delta == null
                    ? contributionValue(alternativeContributions.get(factorName))
                    : delta.alternativeCandidateContribution());
            Double contributionDelta = finiteOrNull(delta == null ? null : delta.contributionDelta());
            boolean selectedAppeared = selectedContributions.containsKey(factorName) || selectedContribution != null;
            boolean alternativeAppeared = alternativeContributions.containsKey(factorName)
                    || alternativeContribution != null;
            evidence.add(new DecisionReplayCapsuleFactorEvidenceResponse(
                    factorName,
                    selectedAppeared,
                    alternativeAppeared,
                    selectedContribution,
                    alternativeContribution,
                    contributionDelta,
                    factorEvidenceStatus(selectedAppeared, alternativeAppeared, contributionDelta)));
        }
        return List.copyOf(evidence);
    }

    private static Map<String, ScoreFactorContributionResponse> finiteContributionsFor(
            RoutingDecisionVectorResponse decisionVector,
            String candidateId) {
        CandidateDecisionVectorResponse candidate = candidateById(decisionVector, candidateId).orElse(null);
        if (candidate == null || candidate.factorContributions() == null) {
            return Map.of();
        }
        LinkedHashMap<String, ScoreFactorContributionResponse> contributions = new LinkedHashMap<>();
        candidate.factorContributions().stream()
                .filter(RoutingDecisionReplayCapsuleService::hasFiniteNamedContribution)
                .sorted(Comparator.comparing(contribution -> contribution.factorName().trim()))
                .forEach(contribution -> contributions.putIfAbsent(contribution.factorName().trim(), contribution));
        return Map.copyOf(contributions);
    }

    private static Map<String, ScoreFactorDeltaResponse> factorDeltasByName(
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis) {
        if (decisionDeltaAnalysis == null || decisionDeltaAnalysis.factorDeltas() == null) {
            return Map.of();
        }
        LinkedHashMap<String, ScoreFactorDeltaResponse> deltas = new LinkedHashMap<>();
        decisionDeltaAnalysis.factorDeltas().stream()
                .filter(delta -> delta != null && !isBlank(delta.factorName()))
                .filter(delta -> isFinite(delta.selectedCandidateContribution())
                        || isFinite(delta.alternativeCandidateContribution())
                        || isFinite(delta.contributionDelta()))
                .sorted(Comparator.comparing(delta -> delta.factorName().trim()))
                .forEach(delta -> deltas.putIfAbsent(delta.factorName().trim(), delta));
        return Map.copyOf(deltas);
    }

    private static Double contributionValue(ScoreFactorContributionResponse contribution) {
        return contribution == null ? null : contribution.contributionValue();
    }

    private static String factorEvidenceStatus(
            boolean selectedAppeared,
            boolean alternativeAppeared,
            Double contributionDelta) {
        if (selectedAppeared && alternativeAppeared && isFinite(contributionDelta)) {
            return STATUS_AVAILABLE;
        }
        if (selectedAppeared || alternativeAppeared || isFinite(contributionDelta)) {
            return STATUS_PARTIAL;
        }
        return STATUS_UNKNOWN;
    }

    private static String capsuleStatus(
            List<String> candidateIds,
            String selectedCandidateId,
            String decisionVectorStatus,
            String factorContributionStatus,
            String snapshotFingerprint,
            String traceFingerprint,
            List<DecisionReplayCapsuleCandidateEvidenceResponse> candidateEvidence,
            List<DecisionReplayCapsuleFactorEvidenceResponse> factorEvidence) {
        if (candidateIds.isEmpty() && isBlank(selectedCandidateId) && STATUS_UNKNOWN.equals(decisionVectorStatus)) {
            return STATUS_UNKNOWN;
        }
        boolean allCandidatesAvailable = !candidateEvidence.isEmpty()
                && candidateEvidence.stream().allMatch(evidence -> STATUS_AVAILABLE.equals(evidence.status()));
        boolean allFactorsAvailable = !factorEvidence.isEmpty()
                && factorEvidence.stream().allMatch(evidence -> STATUS_AVAILABLE.equals(evidence.status()));
        if (!isBlank(selectedCandidateId)
                && !candidateIds.isEmpty()
                && STATUS_AVAILABLE.equals(decisionVectorStatus)
                && STATUS_AVAILABLE.equals(factorContributionStatus)
                && !isBlank(snapshotFingerprint)
                && !isBlank(traceFingerprint)
                && allCandidatesAvailable
                && allFactorsAvailable) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
    }

    private static List<String> reconstructionStepIds(
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace) {
        if (reconstructionTrace == null || reconstructionTrace.reconstructionSteps() == null) {
            return List.of();
        }
        return reconstructionTrace.reconstructionSteps().stream()
                .filter(Objects::nonNull)
                .map(DecisionReplayReconstructionStepResponse::stepId)
                .filter(value -> !isBlank(value))
                .map(String::trim)
                .toList();
    }

    private static String explanation(
            String status,
            String strategyId,
            String selectedCandidateId,
            List<String> candidateIds,
            String closestAlternativeId,
            Double finalScoreGap,
            String largestDeltaFactorName,
            String snapshotFingerprint,
            String traceFingerprint,
            int candidateEvidenceCount,
            int factorEvidenceCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision Replay Capsule is UNKNOWN because selected candidate, candidate set, and Decision "
                    + "Vector evidence were not returned. No replay execution, what-if mutation, persisted "
                    + "capsule, selected candidate, candidate set, alternative, score gap, largest delta factor, "
                    + "or explanation evidence is invented.";
        }
        String alternative = isBlank(closestAlternativeId) ? "not returned" : closestAlternativeId;
        String scoreGap = isFinite(finalScoreGap) ? format(finalScoreGap) : "not returned as a finite value";
        String largestDelta = isBlank(largestDeltaFactorName) ? "not returned" : largestDeltaFactorName;
        String snapshot = isBlank(snapshotFingerprint) ? "not returned" : snapshotFingerprint;
        String trace = isBlank(traceFingerprint) ? "not returned" : traceFingerprint;
        return "Decision Replay Capsule is " + status + " for strategy " + safeValue(strategyId)
                + ": selected candidate " + safeValue(selectedCandidateId) + ", "
                + candidateIds.size() + " candidate id(s), " + candidateEvidenceCount
                + " candidate evidence item(s), " + factorEvidenceCount + " factor evidence item(s), "
                + "closest alternative " + alternative + ", finalScoreGap=" + scoreGap
                + ", largestDeltaFactor=" + largestDelta + ", linked snapshotFingerprint=" + snapshot
                + ", and linked traceFingerprint=" + trace
                + " were packaged from existing lab compare evidence only.";
    }

    private static String fingerprint(
            String status,
            String strategyId,
            String selectedCandidateId,
            List<String> candidateIds,
            Map<String, Double> finiteScores,
            String closestAlternativeId,
            Double finalScoreGap,
            String largestDeltaFactorName,
            String snapshotFingerprint,
            String traceFingerprint,
            String decisionVectorStatus,
            String factorContributionStatus,
            String dominantStatus,
            String deltaStatus,
            String snapshotStatus,
            String traceStatus,
            List<String> reconstructionStepIds,
            List<DecisionReplayCapsuleCandidateEvidenceResponse> candidateEvidence,
            List<DecisionReplayCapsuleFactorEvidenceResponse> factorEvidence) {
        List<String> fields = new ArrayList<>();
        fields.add("schemaVersion=" + SCHEMA_VERSION);
        fields.add("status=" + safeValue(status));
        fields.add("strategyId=" + safeValue(strategyId));
        fields.add("selectedCandidateId=" + safeValue(selectedCandidateId));
        fields.add("candidateIds=" + String.join(",", candidateIds));
        fields.add("candidateFinalScores=" + formatScores(finiteScores));
        fields.add("closestAlternativeCandidateId=" + safeValue(closestAlternativeId));
        fields.add("finalScoreGap=" + (isFinite(finalScoreGap) ? format(finalScoreGap) : "UNKNOWN"));
        fields.add("largestDeltaFactorName=" + safeValue(largestDeltaFactorName));
        fields.add("linkedReplaySnapshotFingerprint=" + safeValue(snapshotFingerprint));
        fields.add("linkedReconstructionTraceFingerprint=" + safeValue(traceFingerprint));
        fields.add("decisionVectorStatus=" + safeValue(decisionVectorStatus));
        fields.add("factorContributionStatus=" + safeValue(factorContributionStatus));
        fields.add("dominantFactorAnalysisStatus=" + safeValue(dominantStatus));
        fields.add("decisionDeltaAnalysisStatus=" + safeValue(deltaStatus));
        fields.add("decisionReplaySnapshotStatus=" + safeValue(snapshotStatus));
        fields.add("decisionReplayReconstructionTraceStatus=" + safeValue(traceStatus));
        fields.add("reconstructionStepIds=" + String.join(",", reconstructionStepIds));
        for (DecisionReplayCapsuleCandidateEvidenceResponse evidence : candidateEvidence) {
            fields.add("candidateEvidence=" + safeValue(evidence.candidateId())
                    + "|" + evidence.selected()
                    + "|" + (isFinite(evidence.finalScore()) ? format(evidence.finalScore()) : "UNKNOWN")
                    + "|" + String.join(",", evidence.factorNames())
                    + "|" + evidence.contributionCount()
                    + "|" + String.join(",", evidence.dominantFactorNames())
                    + "|" + safeValue(evidence.status()));
        }
        for (DecisionReplayCapsuleFactorEvidenceResponse evidence : factorEvidence) {
            fields.add("factorEvidence=" + safeValue(evidence.factorName())
                    + "|" + evidence.appearedInSelectedCandidate()
                    + "|" + evidence.appearedInClosestAlternative()
                    + "|" + (isFinite(evidence.selectedCandidateContribution())
                            ? format(evidence.selectedCandidateContribution()) : "UNKNOWN")
                    + "|" + (isFinite(evidence.closestAlternativeContribution())
                            ? format(evidence.closestAlternativeContribution()) : "UNKNOWN")
                    + "|" + (isFinite(evidence.contributionDelta()) ? format(evidence.contributionDelta()) : "UNKNOWN")
                    + "|" + safeValue(evidence.status()));
        }
        return sha256Hex(String.join("\n", fields));
    }

    private static String formatScores(Map<String, Double> finiteScores) {
        List<String> scoreParts = new ArrayList<>();
        finiteScores.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> scoreParts.add(entry.getKey() + "=" + format(entry.getValue())));
        return String.join(",", scoreParts);
    }

    private static boolean hasFiniteNamedContribution(ScoreFactorContributionResponse contribution) {
        return contribution != null
                && !isBlank(contribution.factorName())
                && isFinite(contribution.contributionValue());
    }

    private static String analysisStatus(String status) {
        if (STATUS_AVAILABLE.equals(status) || STATUS_PARTIAL.equals(status) || STATUS_UNKNOWN.equals(status)) {
            return status;
        }
        return STATUS_UNKNOWN;
    }

    private static Double finiteOrNull(Double value) {
        return isFinite(value) ? value : null;
    }

    private static boolean isFinite(Double value) {
        return value != null && Double.isFinite(value);
    }

    private static String fingerprintOrNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", ex);
        }
    }

    private static String format(Double value) {
        if (!isFinite(value)) {
            return "UNKNOWN";
        }
        return String.format(Locale.ROOT, "%.12f", value);
    }

    private static String safeValue(String value) {
        return isBlank(value) ? "UNKNOWN" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
