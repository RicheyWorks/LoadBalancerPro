package com.richmond423.loadbalancerpro.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;

public final class RoutingDecisionReplaySnapshotService {
    private static final String SCHEMA_VERSION = "decision-replay-snapshot/v1";
    private static final String SOURCE =
            "/api/routing/compare results[] decisionVector, dominantFactorAnalysis, and decisionDeltaAnalysis";
    private static final String FINGERPRINT_ALGORITHM =
            "SHA-256 over stable replay snapshot fields; local deterministic fingerprint only";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision Replay Snapshot is read-only lab evidence derived only from already-built routing compare "
                    + "response data; it does not persist audit logs, execute replay, perform what-if mutation, "
                    + "recompute scores, retune weights, change routing behavior, or add telemetry.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "No production certification, live-cloud proof, real-tenant proof, SLA/SLO proof, registry "
                    + "publication proof, container signing proof, governance application proof, production traffic "
                    + "validation, exact production scoring proof, or production readiness proof is claimed.";

    public RoutingDecisionReplaySnapshotResponse snapshot(
            String strategyId,
            String chosenServerId,
            List<String> candidateServersConsidered,
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis) {
        List<String> candidateIds = candidateIds(candidateServersConsidered, decisionVector);
        String selectedCandidateId = selectedCandidateId(chosenServerId, decisionVector);
        String decisionVectorStatus = decisionVectorStatus(decisionVector);
        String dominantStatus = analysisStatus(dominantFactorAnalysis == null
                ? null
                : dominantFactorAnalysis.status());
        String deltaStatus = analysisStatus(decisionDeltaAnalysis == null
                ? null
                : decisionDeltaAnalysis.status());
        CandidateDecisionDeltaResponse comparison = decisionDeltaAnalysis == null
                ? null
                : decisionDeltaAnalysis.comparison();
        String closestAlternativeId = closestAlternativeId(comparison, candidateIds);
        Double finalScoreGap = finiteOrNull(comparison == null ? null : comparison.finalScoreGap());
        String largestDeltaFactorName = largestDeltaFactorName(decisionDeltaAnalysis);
        String status = snapshotStatus(
                selectedCandidateId,
                candidateIds,
                decisionVectorStatus,
                dominantStatus,
                deltaStatus,
                comparison,
                finalScoreGap);
        String fingerprint = fingerprint(
                status,
                selectedCandidateId,
                candidateIds,
                strategyId,
                decisionVectorStatus,
                dominantStatus,
                deltaStatus,
                closestAlternativeId,
                finalScoreGap,
                largestDeltaFactorName);
        String explanation = explanation(
                status,
                selectedCandidateId,
                candidateIds,
                strategyId,
                closestAlternativeId,
                finalScoreGap,
                largestDeltaFactorName);

        return new RoutingDecisionReplaySnapshotResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                status,
                fingerprint,
                FINGERPRINT_ALGORITHM,
                selectedCandidateId,
                candidateIds,
                candidateIds.size(),
                safeValue(strategyId),
                decisionVectorStatus,
                dominantStatus,
                deltaStatus,
                closestAlternativeId,
                finalScoreGap,
                largestDeltaFactorName,
                explanation,
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    public RoutingDecisionReplaySnapshotResponse unknownSnapshot(String strategyId, String explanation) {
        String status = STATUS_UNKNOWN;
        String fingerprint = fingerprint(
                status,
                null,
                List.of(),
                strategyId,
                STATUS_UNKNOWN,
                STATUS_UNKNOWN,
                STATUS_UNKNOWN,
                null,
                null,
                null);
        return new RoutingDecisionReplaySnapshotResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                status,
                fingerprint,
                FINGERPRINT_ALGORITHM,
                null,
                List.of(),
                0,
                safeValue(strategyId),
                STATUS_UNKNOWN,
                STATUS_UNKNOWN,
                STATUS_UNKNOWN,
                null,
                null,
                null,
                requireNonBlank(explanation, "explanation"),
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    private static List<String> candidateIds(
            List<String> candidateServersConsidered, RoutingDecisionVectorResponse decisionVector) {
        TreeSet<String> ids = new TreeSet<>();
        if (candidateServersConsidered != null) {
            candidateServersConsidered.stream()
                    .filter(value -> !isBlank(value))
                    .map(String::trim)
                    .forEach(ids::add);
        }
        if (decisionVector != null && decisionVector.candidateSummaries() != null) {
            decisionVector.candidateSummaries().stream()
                    .filter(Objects::nonNull)
                    .map(CandidateDecisionVectorResponse::candidateId)
                    .filter(value -> !isBlank(value))
                    .map(String::trim)
                    .forEach(ids::add);
        }
        return List.copyOf(ids);
    }

    private static String selectedCandidateId(String chosenServerId, RoutingDecisionVectorResponse decisionVector) {
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
        return null;
    }

    private static String decisionVectorStatus(RoutingDecisionVectorResponse decisionVector) {
        if (decisionVector == null || decisionVector.candidateSummaries() == null
                || decisionVector.candidateSummaries().isEmpty()) {
            return STATUS_UNKNOWN;
        }
        return STATUS_AVAILABLE;
    }

    private static String analysisStatus(String status) {
        if (STATUS_AVAILABLE.equals(status) || STATUS_PARTIAL.equals(status) || STATUS_UNKNOWN.equals(status)) {
            return status;
        }
        return STATUS_UNKNOWN;
    }

    private static String closestAlternativeId(CandidateDecisionDeltaResponse comparison, List<String> candidateIds) {
        if (comparison == null || isBlank(comparison.closestAlternativeCandidateId())) {
            return null;
        }
        String candidateId = comparison.closestAlternativeCandidateId().trim();
        return candidateIds.contains(candidateId) ? candidateId : null;
    }

    private static String largestDeltaFactorName(RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis) {
        if (decisionDeltaAnalysis == null || decisionDeltaAnalysis.largestAbsoluteFactorDelta() == null
                || isBlank(decisionDeltaAnalysis.largestAbsoluteFactorDelta().factorName())) {
            return null;
        }
        return decisionDeltaAnalysis.largestAbsoluteFactorDelta().factorName().trim();
    }

    private static String snapshotStatus(
            String selectedCandidateId,
            List<String> candidateIds,
            String decisionVectorStatus,
            String dominantStatus,
            String deltaStatus,
            CandidateDecisionDeltaResponse comparison,
            Double finalScoreGap) {
        if (isBlank(selectedCandidateId) || candidateIds.isEmpty() || STATUS_UNKNOWN.equals(decisionVectorStatus)) {
            return STATUS_UNKNOWN;
        }
        if (STATUS_UNKNOWN.equals(dominantStatus) || STATUS_UNKNOWN.equals(deltaStatus)) {
            return STATUS_PARTIAL;
        }
        if (STATUS_PARTIAL.equals(dominantStatus) || STATUS_PARTIAL.equals(deltaStatus)
                || comparison == null || finalScoreGap == null) {
            return STATUS_PARTIAL;
        }
        return STATUS_AVAILABLE;
    }

    private static String explanation(
            String status,
            String selectedCandidateId,
            List<String> candidateIds,
            String strategyId,
            String closestAlternativeId,
            Double finalScoreGap,
            String largestDeltaFactorName) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision Replay Snapshot is UNKNOWN because selected candidate or Decision Vector evidence was "
                    + "not returned. No replay execution, what-if mutation, persisted audit log, candidate, "
                    + "alternative, score gap, or factor evidence is invented.";
        }
        String alternative = closestAlternativeId == null ? "not returned" : closestAlternativeId;
        String scoreGap = finalScoreGap == null ? "not returned as a finite value" : format(finalScoreGap);
        String largestDelta = largestDeltaFactorName == null ? "not returned" : largestDeltaFactorName;
        return "Decision Replay Snapshot is " + status + " for strategy " + safeValue(strategyId)
                + ": selected candidate " + selectedCandidateId + " was summarized with "
                + candidateIds.size() + " deterministically ordered candidate id(s), closest alternative "
                + alternative + ", finalScoreGap=" + scoreGap + ", and largestDeltaFactor="
                + largestDelta + ". This snapshot is derived from existing lab compare evidence only.";
    }

    private static Double finiteOrNull(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return null;
        }
        return value;
    }

    private static String fingerprint(
            String status,
            String selectedCandidateId,
            List<String> candidateIds,
            String strategyId,
            String decisionVectorStatus,
            String dominantStatus,
            String deltaStatus,
            String closestAlternativeId,
            Double finalScoreGap,
            String largestDeltaFactorName) {
        List<String> fields = new ArrayList<>();
        fields.add("schemaVersion=" + SCHEMA_VERSION);
        fields.add("status=" + safeValue(status));
        fields.add("selectedCandidateId=" + safeValue(selectedCandidateId));
        fields.add("candidateIds=" + String.join(",", candidateIds));
        fields.add("strategyId=" + safeValue(strategyId));
        fields.add("decisionVectorStatus=" + safeValue(decisionVectorStatus));
        fields.add("dominantFactorAnalysisStatus=" + safeValue(dominantStatus));
        fields.add("decisionDeltaAnalysisStatus=" + safeValue(deltaStatus));
        fields.add("closestAlternativeCandidateId=" + safeValue(closestAlternativeId));
        fields.add("finalScoreGap=" + (finalScoreGap == null ? "UNKNOWN" : format(finalScoreGap)));
        fields.add("largestDeltaFactorName=" + safeValue(largestDeltaFactorName));
        return sha256Hex(String.join("\n", fields));
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
        if (value == null) {
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

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
