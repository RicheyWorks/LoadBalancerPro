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
import java.util.TreeSet;

public final class RoutingDecisionReplayReconstructionTraceService {
    private static final String SCHEMA_VERSION = "decision-replay-reconstruction-trace/v1";
    private static final String SOURCE =
            "/api/routing/compare already-built results[] evidence: scores, decisionVector, "
                    + "dominantFactorAnalysis, decisionDeltaAnalysis, and decisionReplaySnapshot";
    private static final String FINGERPRINT_ALGORITHM =
            "SHA-256 over stable reconstruction trace fields; local deterministic fingerprint only";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision Replay Reconstruction Trace is read-only lab evidence derived only from already-built "
                    + "routing compare response data; it does not persist traces or audit logs, execute replay, "
                    + "perform what-if mutation, recompute scores, retune weights, change routing behavior, "
                    + "add telemetry, add upload/share/download flows, or add server-side export/PDF/ZIP "
                    + "generation.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "No production certification, live-cloud proof, real-tenant proof, SLA/SLO proof, registry "
                    + "publication proof, container signing proof, governance application proof, production traffic "
                    + "validation, exact production scoring proof, cryptographic production proof, guaranteed replay, "
                    + "or production readiness proof is claimed.";
    private static final Comparator<CandidateDecisionVectorResponse> BY_CANDIDATE_ID = Comparator
            .comparing(candidate -> safeValue(candidate == null ? null : candidate.candidateId()));

    public RoutingDecisionReplayReconstructionTraceResponse trace(
            String strategyId,
            String chosenServerId,
            List<String> candidateServersConsidered,
            Map<String, Double> scores,
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot) {
        List<String> candidateIds = candidateIds(candidateServersConsidered, scores, decisionVector, replaySnapshot);
        Map<String, Double> finiteScores = finiteScores(scores, candidateIds);
        String selectedCandidateId = selectedCandidateId(chosenServerId, decisionVector, replaySnapshot);
        String decisionVectorStatus = decisionVectorStatus(decisionVector);
        String factorContributionStatus = factorContributionStatus(decisionVector);
        String dominantStatus = analysisStatus(dominantFactorAnalysis == null
                ? null
                : dominantFactorAnalysis.status());
        String deltaStatus = analysisStatus(decisionDeltaAnalysis == null
                ? null
                : decisionDeltaAnalysis.status());
        String snapshotStatus = analysisStatus(replaySnapshot == null ? null : replaySnapshot.status());
        String snapshotFingerprint = fingerprintOrNull(replaySnapshot == null
                ? null
                : replaySnapshot.snapshotFingerprint());
        String closestAlternativeId = closestAlternativeId(decisionDeltaAnalysis, replaySnapshot, candidateIds);
        Double finalScoreGap = finalScoreGap(decisionDeltaAnalysis, replaySnapshot);
        String largestDeltaFactorName = largestDeltaFactorName(decisionDeltaAnalysis, replaySnapshot);
        List<DecisionReplayReconstructionStepResponse> steps = steps(
                candidateIds,
                finiteScores,
                scores,
                selectedCandidateId,
                decisionVectorStatus,
                factorContributionStatus,
                dominantStatus,
                deltaStatus,
                snapshotStatus,
                snapshotFingerprint,
                closestAlternativeId,
                finalScoreGap);
        String status = traceStatus(steps, candidateIds, selectedCandidateId, decisionVectorStatus);
        String traceFingerprint = fingerprint(
                status,
                strategyId,
                selectedCandidateId,
                candidateIds,
                finiteScores,
                decisionVectorStatus,
                factorContributionStatus,
                dominantStatus,
                deltaStatus,
                snapshotStatus,
                snapshotFingerprint,
                closestAlternativeId,
                finalScoreGap,
                largestDeltaFactorName,
                steps);
        String explanation = explanation(
                status,
                strategyId,
                selectedCandidateId,
                candidateIds,
                closestAlternativeId,
                finalScoreGap,
                largestDeltaFactorName,
                snapshotFingerprint,
                steps);

        return new RoutingDecisionReplayReconstructionTraceResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                status,
                traceFingerprint,
                FINGERPRINT_ALGORITHM,
                snapshotFingerprint,
                selectedCandidateId,
                candidateIds,
                candidateIds.size(),
                finiteScores,
                safeValue(strategyId),
                decisionVectorStatus,
                factorContributionStatus,
                dominantStatus,
                deltaStatus,
                snapshotStatus,
                closestAlternativeId,
                finalScoreGap,
                largestDeltaFactorName,
                steps,
                explanation,
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    public RoutingDecisionReplayReconstructionTraceResponse unknownTrace(String strategyId, String explanation) {
        return trace(
                strategyId,
                null,
                List.of(),
                Map.of(),
                null,
                null,
                null,
                new RoutingDecisionReplaySnapshotService().unknownSnapshot(strategyId, explanation));
    }

    private static List<String> candidateIds(
            List<String> candidateServersConsidered,
            Map<String, Double> scores,
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot) {
        TreeSet<String> ids = new TreeSet<>();
        if (candidateServersConsidered != null) {
            candidateServersConsidered.stream()
                    .filter(value -> !isBlank(value))
                    .map(String::trim)
                    .forEach(ids::add);
        }
        if (scores != null) {
            scores.keySet().stream()
                    .filter(value -> !isBlank(value))
                    .map(String::trim)
                    .forEach(ids::add);
        }
        if (decisionVector != null && decisionVector.candidateSummaries() != null) {
            decisionVector.candidateSummaries().stream()
                    .filter(candidate -> candidate != null && !isBlank(candidate.candidateId()))
                    .map(candidate -> candidate.candidateId().trim())
                    .forEach(ids::add);
        }
        if (replaySnapshot != null && replaySnapshot.candidateIdsConsidered() != null) {
            replaySnapshot.candidateIdsConsidered().stream()
                    .filter(value -> !isBlank(value))
                    .map(String::trim)
                    .forEach(ids::add);
        }
        return List.copyOf(ids);
    }

    private static Map<String, Double> finiteScores(Map<String, Double> scores, List<String> candidateIds) {
        LinkedHashMap<String, Double> finiteScores = new LinkedHashMap<>();
        if (scores == null || scores.isEmpty()) {
            return finiteScores;
        }
        TreeSet<String> scoreCandidateIds = new TreeSet<>(candidateIds);
        scores.keySet().stream()
                .filter(value -> !isBlank(value))
                .map(String::trim)
                .forEach(scoreCandidateIds::add);
        for (String candidateId : scoreCandidateIds) {
            Double score = scores.get(candidateId);
            if (score != null && Double.isFinite(score)) {
                finiteScores.put(candidateId, score);
            }
        }
        return finiteScores;
    }

    private static String selectedCandidateId(
            String chosenServerId,
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot) {
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
        if (replaySnapshot != null && !isBlank(replaySnapshot.selectedCandidateId())) {
            return replaySnapshot.selectedCandidateId().trim();
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

    private static String factorContributionStatus(RoutingDecisionVectorResponse decisionVector) {
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
        int candidatesWithFiniteContributions = 0;
        for (CandidateDecisionVectorResponse candidate : candidates) {
            if (candidate.factorContributions() != null && candidate.factorContributions().stream()
                    .anyMatch(RoutingDecisionReplayReconstructionTraceService::hasFiniteNamedContribution)) {
                candidatesWithFiniteContributions++;
            }
        }
        if (candidatesWithFiniteContributions == 0) {
            return STATUS_UNKNOWN;
        }
        if (candidatesWithFiniteContributions == candidates.size()) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
    }

    private static String closestAlternativeId(
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            List<String> candidateIds) {
        String candidateId = null;
        if (decisionDeltaAnalysis != null && decisionDeltaAnalysis.comparison() != null
                && !isBlank(decisionDeltaAnalysis.comparison().closestAlternativeCandidateId())) {
            candidateId = decisionDeltaAnalysis.comparison().closestAlternativeCandidateId().trim();
        } else if (replaySnapshot != null && !isBlank(replaySnapshot.closestAlternativeCandidateId())) {
            candidateId = replaySnapshot.closestAlternativeCandidateId().trim();
        }
        if (isBlank(candidateId)) {
            return null;
        }
        return candidateIds.isEmpty() || candidateIds.contains(candidateId) ? candidateId : null;
    }

    private static Double finalScoreGap(
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot) {
        if (decisionDeltaAnalysis != null && decisionDeltaAnalysis.comparison() != null
                && isFinite(decisionDeltaAnalysis.comparison().finalScoreGap())) {
            return decisionDeltaAnalysis.comparison().finalScoreGap();
        }
        if (replaySnapshot != null && isFinite(replaySnapshot.finalScoreGap())) {
            return replaySnapshot.finalScoreGap();
        }
        return null;
    }

    private static String largestDeltaFactorName(
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot) {
        if (decisionDeltaAnalysis != null && decisionDeltaAnalysis.largestAbsoluteFactorDelta() != null
                && !isBlank(decisionDeltaAnalysis.largestAbsoluteFactorDelta().factorName())) {
            return decisionDeltaAnalysis.largestAbsoluteFactorDelta().factorName().trim();
        }
        if (replaySnapshot != null && !isBlank(replaySnapshot.largestDeltaFactorName())) {
            return replaySnapshot.largestDeltaFactorName().trim();
        }
        return null;
    }

    private static List<DecisionReplayReconstructionStepResponse> steps(
            List<String> candidateIds,
            Map<String, Double> finiteScores,
            Map<String, Double> scores,
            String selectedCandidateId,
            String decisionVectorStatus,
            String factorContributionStatus,
            String dominantStatus,
            String deltaStatus,
            String snapshotStatus,
            String snapshotFingerprint,
            String closestAlternativeId,
            Double finalScoreGap) {
        List<DecisionReplayReconstructionStepResponse> steps = new ArrayList<>();
        steps.add(step(
                "candidate-set-observed",
                candidateIds.isEmpty() ? STATUS_UNKNOWN : STATUS_AVAILABLE,
                "candidateServersConsidered, scores, decisionVector.candidateSummaries, decisionReplaySnapshot",
                candidateIds.isEmpty()
                        ? "No candidate id set was returned in already-built compare evidence."
                        : "Candidate id set was observed from already-built compare evidence in deterministic order.",
                candidateIds.isEmpty() ? "candidate id evidence was absent or empty" : null));
        steps.add(step(
                "selected-candidate-observed",
                selectedCandidateStatus(selectedCandidateId, candidateIds),
                "chosenServerId, decisionVector.selectedCandidateVector, decisionReplaySnapshot.selectedCandidateId",
                isBlank(selectedCandidateId)
                        ? "No selected candidate was returned in already-built compare evidence."
                        : "Selected candidate " + selectedCandidateId
                                + " was observed from already-built compare evidence.",
                isBlank(selectedCandidateId) ? "selected candidate evidence was absent" : null));
        steps.add(step(
                "candidate-final-scores-observed",
                scoreStatus(candidateIds, finiteScores, scores),
                "scores",
                scoreExplanation(candidateIds, finiteScores, scores),
                scoreMissingReason(candidateIds, finiteScores, scores)));
        steps.add(step(
                "decision-vector-observed",
                decisionVectorStatus,
                "decisionVector",
                STATUS_AVAILABLE.equals(decisionVectorStatus)
                        ? "Decision Vector evidence was returned for candidate summaries."
                        : "Decision Vector evidence was not returned with candidate summaries.",
                STATUS_AVAILABLE.equals(decisionVectorStatus) ? null
                        : "decisionVector candidate summary evidence was absent or empty"));
        steps.add(step(
                "candidate-factor-contributions-observed",
                factorContributionStatus,
                "decisionVector.candidateSummaries[].factorContributions",
                factorContributionExplanation(factorContributionStatus),
                STATUS_AVAILABLE.equals(factorContributionStatus) ? null
                        : "finite named factor contribution evidence was incomplete or absent"));
        steps.add(step(
                "dominant-factors-observed",
                dominantStatus,
                "dominantFactorAnalysis",
                analysisExplanation("Dominant Factor Analysis", dominantStatus),
                STATUS_AVAILABLE.equals(dominantStatus) ? null
                        : "dominant factor evidence was incomplete or unavailable"));
        steps.add(step(
                "closest-alternative-observed",
                isBlank(closestAlternativeId) ? STATUS_UNKNOWN : STATUS_AVAILABLE,
                "decisionDeltaAnalysis.comparison.closestAlternativeCandidateId",
                isBlank(closestAlternativeId)
                        ? "No closest alternative candidate id was returned in already-built delta evidence."
                        : "Closest alternative candidate " + closestAlternativeId
                                + " was observed from already-built delta evidence.",
                isBlank(closestAlternativeId) ? "closest alternative evidence was absent" : null));
        steps.add(step(
                "selected-vs-alternative-delta-observed",
                deltaStepStatus(deltaStatus, finalScoreGap),
                "decisionDeltaAnalysis",
                deltaExplanation(deltaStatus, finalScoreGap),
                deltaMissingReason(deltaStatus, finalScoreGap)));
        steps.add(step(
                "replay-snapshot-fingerprint-observed",
                snapshotFingerprintStatus(snapshotStatus, snapshotFingerprint),
                "decisionReplaySnapshot.snapshotFingerprint",
                snapshotExplanation(snapshotStatus, snapshotFingerprint),
                isBlank(snapshotFingerprint) ? "snapshot fingerprint evidence was absent" : null));
        return List.copyOf(steps);
    }

    private static DecisionReplayReconstructionStepResponse step(
            String stepId,
            String status,
            String evidenceSourceFieldPath,
            String explanation,
            String missingEvidenceReason) {
        return new DecisionReplayReconstructionStepResponse(
                stepId,
                analysisStatus(status),
                evidenceSourceFieldPath,
                explanation,
                isBlank(missingEvidenceReason) ? null : missingEvidenceReason);
    }

    private static String selectedCandidateStatus(String selectedCandidateId, List<String> candidateIds) {
        if (isBlank(selectedCandidateId)) {
            return STATUS_UNKNOWN;
        }
        if (!candidateIds.isEmpty() && !candidateIds.contains(selectedCandidateId)) {
            return STATUS_PARTIAL;
        }
        return STATUS_AVAILABLE;
    }

    private static String scoreStatus(List<String> candidateIds, Map<String, Double> finiteScores,
                                      Map<String, Double> scores) {
        if (finiteScores.isEmpty()) {
            return STATUS_UNKNOWN;
        }
        if (candidateIds.isEmpty()) {
            return STATUS_PARTIAL;
        }
        if (finiteScores.keySet().containsAll(candidateIds) && scores != null && scores.keySet().containsAll(candidateIds)) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
    }

    private static String scoreExplanation(List<String> candidateIds, Map<String, Double> finiteScores,
                                           Map<String, Double> scores) {
        if (finiteScores.isEmpty()) {
            return "No finite candidate final scores were returned in already-built compare evidence.";
        }
        if (STATUS_AVAILABLE.equals(scoreStatus(candidateIds, finiteScores, scores))) {
            return "Finite candidate final scores were observed for the deterministic candidate set.";
        }
        return "Some finite candidate final score evidence was observed, but the returned score set was incomplete.";
    }

    private static String scoreMissingReason(List<String> candidateIds, Map<String, Double> finiteScores,
                                             Map<String, Double> scores) {
        if (finiteScores.isEmpty()) {
            return "finite score evidence was absent or non-finite";
        }
        if (STATUS_AVAILABLE.equals(scoreStatus(candidateIds, finiteScores, scores))) {
            return null;
        }
        return "one or more candidate scores were absent or non-finite";
    }

    private static String factorContributionExplanation(String status) {
        if (STATUS_AVAILABLE.equals(status)) {
            return "Finite named factor contribution evidence was returned for all observed candidate vectors.";
        }
        if (STATUS_PARTIAL.equals(status)) {
            return "Some finite named factor contribution evidence was returned, but at least one candidate vector "
                    + "was incomplete.";
        }
        return "No finite named factor contribution evidence was returned.";
    }

    private static String analysisExplanation(String analysisName, String status) {
        if (STATUS_AVAILABLE.equals(status)) {
            return analysisName + " evidence was returned as AVAILABLE.";
        }
        if (STATUS_PARTIAL.equals(status)) {
            return analysisName + " evidence was returned as PARTIAL.";
        }
        return analysisName + " evidence was UNKNOWN or not returned.";
    }

    private static String deltaStepStatus(String deltaStatus, Double finalScoreGap) {
        if (STATUS_UNKNOWN.equals(deltaStatus)) {
            return STATUS_UNKNOWN;
        }
        if (!isFinite(finalScoreGap)) {
            return STATUS_PARTIAL;
        }
        return deltaStatus;
    }

    private static String deltaExplanation(String deltaStatus, Double finalScoreGap) {
        if (STATUS_UNKNOWN.equals(deltaStatus)) {
            return "Selected-vs-alternative decision delta evidence was UNKNOWN or not returned.";
        }
        if (!isFinite(finalScoreGap)) {
            return "Decision delta evidence was returned, but finalScoreGap was not returned as a finite value.";
        }
        return "Selected-vs-alternative decision delta evidence was returned with finite finalScoreGap="
                + format(finalScoreGap) + ".";
    }

    private static String deltaMissingReason(String deltaStatus, Double finalScoreGap) {
        if (STATUS_UNKNOWN.equals(deltaStatus)) {
            return "decision delta evidence was unavailable";
        }
        if (!isFinite(finalScoreGap)) {
            return "finite finalScoreGap evidence was absent";
        }
        return STATUS_AVAILABLE.equals(deltaStatus) ? null : "decision delta evidence was partial";
    }

    private static String snapshotFingerprintStatus(String snapshotStatus, String snapshotFingerprint) {
        if (isBlank(snapshotFingerprint)) {
            return STATUS_UNKNOWN;
        }
        if (STATUS_UNKNOWN.equals(snapshotStatus)) {
            return STATUS_PARTIAL;
        }
        return STATUS_AVAILABLE;
    }

    private static String snapshotExplanation(String snapshotStatus, String snapshotFingerprint) {
        if (isBlank(snapshotFingerprint)) {
            return "No Decision Replay Snapshot fingerprint was returned.";
        }
        if (STATUS_UNKNOWN.equals(snapshotStatus)) {
            return "A deterministic snapshot fingerprint was returned, but the replay snapshot status was UNKNOWN.";
        }
        return "Decision Replay Snapshot fingerprint was observed and linked without executing replay.";
    }

    private static String traceStatus(
            List<DecisionReplayReconstructionStepResponse> steps,
            List<String> candidateIds,
            String selectedCandidateId,
            String decisionVectorStatus) {
        if (candidateIds.isEmpty() && isBlank(selectedCandidateId) && STATUS_UNKNOWN.equals(decisionVectorStatus)) {
            return STATUS_UNKNOWN;
        }
        boolean allAvailable = steps.stream().allMatch(step -> STATUS_AVAILABLE.equals(step.status()));
        if (allAvailable) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
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
            List<DecisionReplayReconstructionStepResponse> steps) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision Replay Reconstruction Trace is UNKNOWN because selected candidate, candidate set, "
                    + "and Decision Vector evidence were not returned. No replay execution, what-if mutation, "
                    + "persisted trace, selected candidate, alternative, score gap, largest delta factor, or "
                    + "explanation evidence is invented.";
        }
        long availableSteps = steps.stream().filter(step -> STATUS_AVAILABLE.equals(step.status())).count();
        String alternative = isBlank(closestAlternativeId) ? "not returned" : closestAlternativeId;
        String scoreGap = isFinite(finalScoreGap) ? format(finalScoreGap) : "not returned as a finite value";
        String largestDelta = isBlank(largestDeltaFactorName) ? "not returned" : largestDeltaFactorName;
        String snapshot = isBlank(snapshotFingerprint) ? "not returned" : snapshotFingerprint;
        return "Decision Replay Reconstruction Trace is " + status + " for strategy " + safeValue(strategyId)
                + ": selected candidate " + safeValue(selectedCandidateId) + ", "
                + candidateIds.size() + " candidate id(s), " + availableSteps + " available reconstruction step(s), "
                + "closest alternative " + alternative + ", finalScoreGap=" + scoreGap
                + ", largestDeltaFactor=" + largestDelta + ", and snapshotFingerprint=" + snapshot
                + " were derived from existing lab compare evidence only.";
    }

    private static String analysisStatus(String status) {
        if (STATUS_AVAILABLE.equals(status) || STATUS_PARTIAL.equals(status) || STATUS_UNKNOWN.equals(status)) {
            return status;
        }
        return STATUS_UNKNOWN;
    }

    private static boolean hasFiniteNamedContribution(ScoreFactorContributionResponse contribution) {
        return contribution != null
                && !isBlank(contribution.factorName())
                && isFinite(contribution.contributionValue());
    }

    private static boolean isFinite(Double value) {
        return value != null && Double.isFinite(value);
    }

    private static String fingerprintOrNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static String fingerprint(
            String status,
            String strategyId,
            String selectedCandidateId,
            List<String> candidateIds,
            Map<String, Double> finiteScores,
            String decisionVectorStatus,
            String factorContributionStatus,
            String dominantStatus,
            String deltaStatus,
            String snapshotStatus,
            String snapshotFingerprint,
            String closestAlternativeId,
            Double finalScoreGap,
            String largestDeltaFactorName,
            List<DecisionReplayReconstructionStepResponse> steps) {
        List<String> fields = new ArrayList<>();
        fields.add("schemaVersion=" + SCHEMA_VERSION);
        fields.add("status=" + safeValue(status));
        fields.add("strategyId=" + safeValue(strategyId));
        fields.add("selectedCandidateId=" + safeValue(selectedCandidateId));
        fields.add("candidateIds=" + String.join(",", candidateIds));
        fields.add("candidateFinalScores=" + formatScores(finiteScores));
        fields.add("decisionVectorStatus=" + safeValue(decisionVectorStatus));
        fields.add("factorContributionStatus=" + safeValue(factorContributionStatus));
        fields.add("dominantFactorAnalysisStatus=" + safeValue(dominantStatus));
        fields.add("decisionDeltaAnalysisStatus=" + safeValue(deltaStatus));
        fields.add("decisionReplaySnapshotStatus=" + safeValue(snapshotStatus));
        fields.add("snapshotFingerprint=" + safeValue(snapshotFingerprint));
        fields.add("closestAlternativeCandidateId=" + safeValue(closestAlternativeId));
        fields.add("finalScoreGap=" + (isFinite(finalScoreGap) ? format(finalScoreGap) : "UNKNOWN"));
        fields.add("largestDeltaFactorName=" + safeValue(largestDeltaFactorName));
        for (DecisionReplayReconstructionStepResponse step : steps) {
            fields.add("step=" + step.stepId()
                    + "|" + safeValue(step.status())
                    + "|" + safeValue(step.evidenceSourceFieldPath())
                    + "|" + safeValue(step.missingEvidenceReason()));
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
