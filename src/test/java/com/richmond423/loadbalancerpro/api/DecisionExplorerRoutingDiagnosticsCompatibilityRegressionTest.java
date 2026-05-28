package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DecisionExplorerRoutingDiagnosticsCompatibilityRegressionTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final DecisionExplorerConfidenceSummaryService summaryService =
            new DecisionExplorerConfidenceSummaryService();
    private final DecisionExplorerRoutingDiagnosticsService diagnosticsService =
            new DecisionExplorerRoutingDiagnosticsService();

    @Test
    void routingDiagnosticsJsonKeepsAdditiveFieldOrder() {
        JsonNode json = OBJECT_MAPPER.valueToTree(diagnostics().get(0));

        assertEquals(List.of(
                "readOnly",
                "simulationOnly",
                "diagnosticsObject",
                "contractVersion",
                "overallStatus",
                "evidenceQuality",
                "selectedCandidateId",
                "diagnosticCount",
                "presentEvidenceCount",
                "partialEvidenceCount",
                "missingEvidenceCount",
                "degradedEvidenceCount",
                "unknownEvidenceCount",
                "evidenceDiagnostics",
                "selectedCandidateDiagnostic",
                "alternativeCandidateDiagnostics",
                "candidateDiagnostics",
                "factorDiagnostics",
                "degradationReasons",
                "partialEvidenceReasons",
                "unknownEvidenceReasons",
                "explanationText",
                "diagnosticReasons",
                "warnings",
                "unknowns",
                "sourceReferenceIds",
                "boundaryNote"), fieldNames(json));
        assertEquals("DecisionExplorerRoutingDiagnosticsV1", json.path("diagnosticsObject").asText());
        assertEquals("Routing diagnostics mark selected candidate edge-a as STRONG because "
                        + "selected candidate diagnostics are STRONG/LOW, evidence quality is COMPLETE, "
                        + "1 alternative candidate(s) are available for comparison, and 2 factor diagnostic row(s) "
                        + "were computed.",
                json.path("explanationText").asText());
    }

    @Test
    void fixtureDiagnosticsSerializeDeterministicallyAcrossBuilds() {
        List<String> firstFingerprints = compatibilityFingerprints();
        List<String> secondFingerprints = compatibilityFingerprints();

        assertEquals(firstFingerprints, secondFingerprints);
        assertEquals(List.of(
                "strong-confirmed-selection|STRONG|COMPLETE|edge-a|9:8/0/0/0/1",
                "partial-candidate-and-factor-evidence|PARTIAL|PARTIAL|edge-a|9:1/7/0/0/1",
                "unknown-no-routing-evidence|UNKNOWN|UNKNOWN|UNKNOWN|9:2/0/6/0/1",
                "degraded-selected-health-evidence|DEGRADED|DEGRADED|edge-a|9:4/0/0/5/0"),
                firstFingerprints.stream()
                        .map(fingerprint -> fingerprint.substring(0, fingerprint.indexOf("|candidates=")))
                        .toList());
        assertTrue(firstFingerprints.get(0).contains("candidates=1:edge-a:STRONG:LOW,2:edge-b:STRONG:LOW"));
        assertTrue(firstFingerprints.get(1).contains("factors=1:edge-b:latency:WARNING:PARTIAL"));
        assertTrue(firstFingerprints.get(2).contains("evidence=CANDIDATE:candidate-comparisons:MISSING"));
        assertTrue(firstFingerprints.get(3).contains("factors=1:edge-a:healthState:DEGRADED:DEGRADED"));
    }

    @Test
    void unknownFallbackKeepsDiagnosticsArraysAndExplanationPresent() {
        JsonNode json = OBJECT_MAPPER.valueToTree(DecisionExplorerRoutingDiagnosticsV1.unknown(null));

        assertEquals("UNKNOWN", json.path("overallStatus").asText());
        assertEquals("UNKNOWN", json.path("evidenceQuality").asText());
        assertEquals(1, json.path("unknownEvidenceCount").asInt());
        assertTrue(json.path("evidenceDiagnostics").isArray());
        assertEquals(1, json.path("evidenceDiagnostics").size());
        assertTrue(json.path("alternativeCandidateDiagnostics").isArray());
        assertEquals(0, json.path("alternativeCandidateDiagnostics").size());
        assertTrue(json.path("candidateDiagnostics").isArray());
        assertEquals(0, json.path("candidateDiagnostics").size());
        assertTrue(json.path("factorDiagnostics").isArray());
        assertEquals(0, json.path("factorDiagnostics").size());
        assertTrue(json.path("explanationText").asText().contains("NO_CONFIDENCE_SUMMARY_RETURNED"));
        assertEquals("UNKNOWN", json.path("boundaryNote").asText());
    }

    @Test
    void routingDiagnosticsCompatibilityPayloadDoesNotOverclaim() {
        String normalized = diagnostics().toString().toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("read-only"));
        assertTrue(normalized.contains("no production routing behavior changes"));
        for (String forbidden : List.of(
                "production readiness proven",
                "certification complete",
                "live-cloud validation complete",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "replay export is implemented",
                "autonomous production action enabled")) {
            assertFalse(normalized.contains(forbidden), "routing diagnostics must not overclaim " + forbidden);
        }
    }

    private List<DecisionExplorerRoutingDiagnosticsV1> diagnostics() {
        return DecisionExplorerConfidenceSummaryFixtureCatalog.fixtures().stream()
                .map(fixture -> fixture.buildDiagnostics(summaryService, diagnosticsService))
                .toList();
    }

    private List<String> compatibilityFingerprints() {
        List<DecisionExplorerConfidenceSummaryFixtureCatalog.StatusFixture> fixtures =
                DecisionExplorerConfidenceSummaryFixtureCatalog.fixtures();
        List<DecisionExplorerRoutingDiagnosticsV1> diagnostics = diagnostics();
        return java.util.stream.IntStream.range(0, fixtures.size())
                .mapToObj(index -> compatibilityFingerprint(fixtures.get(index).fixtureId(), diagnostics.get(index)))
                .toList();
    }

    private static String compatibilityFingerprint(String fixtureId, DecisionExplorerRoutingDiagnosticsV1 diagnostics) {
        return fixtureId + "|"
                + diagnostics.overallStatus() + "|"
                + diagnostics.evidenceQuality() + "|"
                + diagnostics.selectedCandidateId() + "|"
                + diagnostics.diagnosticCount() + ":"
                + diagnostics.presentEvidenceCount() + "/"
                + diagnostics.partialEvidenceCount() + "/"
                + diagnostics.missingEvidenceCount() + "/"
                + diagnostics.degradedEvidenceCount() + "/"
                + diagnostics.unknownEvidenceCount()
                + "|candidates=" + diagnostics.candidateDiagnostics().stream()
                        .map(candidate -> candidate.displayOrder() + ":" + candidate.candidateId() + ":"
                                + candidate.diagnosticStatus() + ":" + candidate.riskLevel())
                        .collect(Collectors.joining(","))
                + "|factors=" + diagnostics.factorDiagnostics().stream()
                        .map(factor -> factor.displayOrder() + ":" + factor.candidateId() + ":"
                                + factor.factorName() + ":" + factor.contribution() + ":"
                                + factor.factorStatus())
                        .collect(Collectors.joining(","))
                + "|evidence=" + diagnostics.evidenceDiagnostics().stream()
                        .map(evidence -> evidence.category() + ":" + evidence.diagnosticId() + ":"
                                + evidence.status())
                        .collect(Collectors.joining(","));
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
