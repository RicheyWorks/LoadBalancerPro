package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class DecisionExplorerPayloadV1Test {

    @Test
    void payloadForcesReadOnlyAndSimulationOnlyBoundaries() {
        DecisionExplorerPayloadV1 payload = new DecisionExplorerPayloadV1(
                false,
                false,
                null,
                null,
                "/api/routing/compare",
                "decision-1",
                decisionReadout(),
                selectedCandidate(),
                List.of(selectedCandidate()),
                List.of(factorContribution()),
                List.of(policyGate()),
                List.of(decisionDiff()),
                List.of(evidencePacketReadout()),
                agentStructuredOutput(),
                List.of("partial evidence is visible"),
                List.of("non-selected candidate internals are not exposed"),
                notProvenBoundaries(),
                "read-only simulation-only DTO skeleton");

        assertTrue(payload.readOnly());
        assertTrue(payload.simulationOnly());
        assertEquals("DecisionExplorerPayloadV1", payload.payloadObject());
        assertEquals("v1", payload.contractVersion());
    }

    @Test
    void dtoNamesMatchThePhaseOneDataContract() {
        DecisionExplorerPayloadV1 payload = samplePayload();

        assertEquals("DecisionExplorerPayloadV1", payload.payloadObject());
        assertEquals("AgentStructuredOutputV1", payload.agentStructuredOutput().schemaName());
        assertEquals("selected-candidate", payload.decisionReadout().selectedCandidateId());
        assertEquals("candidate-a", payload.selectedCandidate().candidateId());
        assertEquals("candidate-a", payload.candidateComparisons().get(0).candidateId());
        assertEquals("DecisionExplorerConfidenceSummaryV1", payload.confidenceSummary().summaryObject());
        assertEquals("STRONG", payload.confidenceSummary().status());
        assertEquals("candidate-a", payload.confidenceSummary().candidateConfidenceDetails().get(0).candidateId());
        assertEquals("latency", payload.confidenceSummary().factorStatusDetails().get(0).factorName());
        assertEquals("DecisionExplorerStatusExplanationV1",
                payload.confidenceSummary().statusExplanation().explanationObject());
        assertEquals("STRONG", payload.confidenceSummary().statusExplanation().status());
        assertEquals("DecisionExplorerRoutingDiagnosticsV1", payload.routingDiagnostics().diagnosticsObject());
        assertEquals("latency", payload.factorContributions().get(0).factorName());
        assertEquals("policy-health", payload.policyGateReadouts().get(0).gateId());
        assertEquals("candidate-a", payload.decisionDiffReadouts().get(0).baselineCandidateId());
        assertEquals("future-evidence-packet", payload.evidencePacketReadouts().get(0).referenceId());
    }

    @Test
    void collectionsAreCopiedAndRemainUnmodifiable() {
        List<String> warnings = new ArrayList<>();
        warnings.add("first warning");
        List<CandidateReadoutV1> candidates = new ArrayList<>();
        candidates.add(selectedCandidate());

        DecisionExplorerPayloadV1 payload = new DecisionExplorerPayloadV1(
                true,
                true,
                "DecisionExplorerPayloadV1",
                "v1",
                "test",
                "decision-1",
                decisionReadout(),
                selectedCandidate(),
                candidates,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                agentStructuredOutput(),
                warnings,
                List.of(),
                notProvenBoundaries(),
                "read-only boundary");

        warnings.add("late warning");
        candidates.add(nonSelectedCandidate());

        assertEquals(List.of("first warning"), payload.warnings());
        assertEquals(1, payload.candidateSet().size());
        assertThrows(UnsupportedOperationException.class, () -> payload.warnings().add("mutated"));
        assertThrows(UnsupportedOperationException.class, () -> payload.candidateSet().add(nonSelectedCandidate()));
    }

    @Test
    void nullCollectionsNormalizeToEmptyForPartialEvidence() {
        DecisionExplorerPayloadV1 payload = new DecisionExplorerPayloadV1(
                true,
                true,
                null,
                null,
                null,
                null,
                new DecisionReadoutV1(null, null, null, null, null, null, null, null),
                new CandidateReadoutV1(null, null, false, null, null, null, null, null, null, null, null),
                null,
                null,
                null,
                null,
                null,
                new AgentStructuredOutputV1(null, null, null, null, null, null, null, null),
                null,
                null,
                null,
                null);

        assertEquals("UNKNOWN", payload.source());
        assertEquals("UNKNOWN", payload.decisionId());
        assertTrue(payload.candidateSet().isEmpty());
        assertTrue(payload.candidateComparisons().isEmpty());
        assertTrue(payload.factorContributions().isEmpty());
        assertTrue(payload.policyGateReadouts().isEmpty());
        assertTrue(payload.decisionDiffReadouts().isEmpty());
        assertTrue(payload.evidencePacketReadouts().isEmpty());
        assertTrue(payload.warnings().isEmpty());
        assertTrue(payload.unknowns().isEmpty());
        assertTrue(payload.notProvenBoundaries().isEmpty());
        assertEquals("UNKNOWN", payload.confidenceSummary().status());
        assertTrue(payload.confidenceSummary().factorStatusDetails().isEmpty());
        assertEquals("UNKNOWN", payload.confidenceSummary().statusExplanation().status());
        assertEquals("UNKNOWN", payload.routingDiagnostics().overallStatus());
        assertTrue(payload.routingDiagnostics().candidateDiagnostics().isEmpty());
        assertEquals("UNKNOWN", payload.decisionReadout().summary());
        assertTrue(payload.selectedCandidate().visibleSignals().isEmpty());
        assertTrue(payload.agentStructuredOutput().stableFieldNames().isEmpty());
    }

    @Test
    void boundaryLanguageDoesNotOverclaimImplementedEvidence() {
        String normalized = samplePayload().toString().toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("read-only"));
        assertTrue(normalized.contains("simulation-only"));
        assertTrue(normalized.contains("no production readiness"));
        assertTrue(normalized.contains("no live-cloud validation"));
        assertTrue(normalized.contains("no replay/export proof"));
        assertTrue(normalized.contains("no runtime endpoint/ui/storage/evidence-packet implementation"));
        assertTrue(normalized.contains("no autonomous production action"));
        assertFalse(normalized.contains("production readiness proven"));
        assertFalse(normalized.contains("certification complete"));
        assertFalse(normalized.contains("live-cloud validation complete"));
        assertFalse(normalized.contains("throughput proof complete"));
        assertFalse(normalized.contains("autonomous production action enabled"));
    }

    private static DecisionExplorerPayloadV1 samplePayload() {
        return new DecisionExplorerPayloadV1(
                true,
                true,
                "DecisionExplorerPayloadV1",
                "v1",
                "/api/routing/compare",
                "decision-1",
                decisionReadout(),
                selectedCandidate(),
                List.of(selectedCandidate(), nonSelectedCandidate()),
                List.of(candidateComparisonRow()),
                confidenceSummary(),
                List.of(factorContribution()),
                List.of(),
                List.of(policyGate()),
                List.of(decisionDiff()),
                List.of(evidencePacketReadout()),
                agentStructuredOutput(),
                List.of("partial evidence is visible"),
                List.of("non-selected candidate internals are not exposed"),
                notProvenBoundaries(),
                "read-only simulation-only DTO skeleton");
    }

    private static DecisionExplorerConfidenceSummaryV1 confidenceSummary() {
        return new DecisionExplorerConfidenceSummaryV1(
                true,
                true,
                "DecisionExplorerConfidenceSummaryV1",
                "v1",
                "STRONG",
                "COMPLETE",
                "candidate-a",
                2,
                1,
                1,
                0,
                0,
                0,
                0,
                3,
                List.of(candidateConfidence()),
                List.of(factorStatus()),
                statusExplanation(),
                List.of("candidateCount=2", "decisionStatus=AVAILABLE"),
                List.of("CANDIDATE_COMPARISONS_AVAILABLE", "FACTOR_EVIDENCE_AVAILABLE"),
                List.of(),
                List.of(),
                List.of("decision-vector", "decision-vector:candidate-a", "phase1-scope"),
                "confidence summary is read-only and simulation-only");
    }

    private static DecisionExplorerCandidateConfidenceV1 candidateConfidence() {
        return new DecisionExplorerCandidateConfidenceV1(
                "candidate-a",
                "candidate-a",
                true,
                1,
                "STRONG",
                "HEALTHY",
                "SELECTED",
                10.0,
                0.0,
                2,
                1,
                1,
                0,
                0,
                List.of("CANDIDATE_COMPARISON_AVAILABLE", "HEALTH_SIGNAL_HEALTHY"),
                List.of(),
                List.of(),
                List.of("decision-vector:candidate-a", "phase1-scope"),
                "candidate confidence is read-only and simulation-only");
    }

    private static DecisionExplorerFactorStatusV1 factorStatus() {
        return new DecisionExplorerFactorStatusV1(
                "candidate-a",
                "latency",
                1,
                "STRONG",
                "AVAILABLE",
                "12ms",
                "SUPPORTS_SELECTION",
                "Factor latency has available evidence for candidate candidate-a.",
                List.of("FACTOR_EVIDENCE_AVAILABLE", "FACTOR_SOURCE_REFERENCES_AVAILABLE"),
                List.of(),
                List.of(),
                List.of("decision-vector:candidate-a", "factor-contribution:candidate-a:latency"),
                "factor status is read-only and simulation-only");
    }

    private static DecisionExplorerStatusExplanationV1 statusExplanation() {
        return new DecisionExplorerStatusExplanationV1(
                "DecisionExplorerStatusExplanationV1",
                "v1",
                "STRONG",
                "COMPLETE",
                "candidate-a",
                "STRONG",
                "HEALTHY",
                "STRONG",
                "Decision Explorer marks selected candidate candidate-a as STRONG because candidate confidence is "
                        + "STRONG and factor status rollup is STRONG.",
                List.of("CANDIDATE_COMPARISONS_AVAILABLE", "FACTOR_EVIDENCE_AVAILABLE"),
                List.of("candidateConfidenceDetailCount=1", "factorStatusDetailCount=1"),
                List.of("no status warnings or unknowns surfaced"),
                List.of("decision-vector:candidate-a", "factor-contribution:candidate-a:latency"),
                "status explanation is read-only and simulation-only");
    }

    private static DecisionReadoutV1 decisionReadout() {
        return new DecisionReadoutV1(
                "decision-1",
                "AVAILABLE",
                "selected-candidate",
                "TAIL_LATENCY_POWER_OF_TWO",
                "Decision Explorer summarizes visible routing evidence only.",
                List.of("VISIBLE_SIGNAL_MATCH"),
                List.of("adr-0010", "phase1-scope"),
                "read-only simulation-only decision readout");
    }

    private static CandidateReadoutV1 selectedCandidate() {
        return new CandidateReadoutV1(
                "candidate-a",
                "candidate-a",
                true,
                "SELECTED",
                10.0,
                List.of("healthState=healthy", "latency=12ms"),
                List.of("hidden routing internals not exposed"),
                List.of("VISIBLE_SIGNAL_MATCH"),
                List.of("policy-health"),
                List.of("phase1-scope"),
                "candidate readout does not mutate routing behavior");
    }

    private static CandidateReadoutV1 nonSelectedCandidate() {
        return new CandidateReadoutV1(
                "candidate-b",
                "candidate-b",
                false,
                "REJECTED",
                12.0,
                List.of("healthState=healthy", "latency=30ms"),
                List.of("hidden routing internals not exposed"),
                List.of("VISIBLE_SIGNAL_GAP"),
                List.of("policy-health"),
                List.of("phase1-scope"),
                "candidate readout does not prove production routing behavior");
    }

    private static FactorContributionV1 factorContribution() {
        return new FactorContributionV1(
                "latency",
                "candidate-a",
                "SUPPORTS_SELECTION",
                2.0,
                "EXACT_FROM_RETURNED_EVIDENCE",
                "Latency contribution is copied from visible returned evidence when available.",
                List.of("decision-vector"),
                "factor readout is not benchmark/load/stress evidence");
    }

    private static DecisionExplorerCandidateComparisonRowV1 candidateComparisonRow() {
        return new DecisionExplorerCandidateComparisonRowV1(
                "candidate-a",
                "candidate-a",
                true,
                1,
                "SELECTED",
                10.0,
                0.0,
                List.of("healthState=healthy", "latency=12ms"),
                List.of("hidden routing internals not exposed"),
                List.of("VISIBLE_SIGNAL_MATCH"),
                List.of("policy-health"),
                List.of("decision-vector:candidate-a"),
                List.of(),
                List.of("hidden routing internals"),
                "candidate comparison row is read-only and simulation-only");
    }

    private static PolicyGateReadoutV1 policyGate() {
        return new PolicyGateReadoutV1(
                "policy-health",
                "Health visibility",
                "AVAILABLE",
                "PASS",
                "Visible health state is available for this candidate.",
                List.of("phase1-scope"),
                "policy gate visualization is read-only");
    }

    private static DecisionDiffReadoutV1 decisionDiff() {
        return new DecisionDiffReadoutV1(
                "candidate-a",
                "candidate-b",
                "AVAILABLE",
                2.0,
                List.of("latency"),
                List.of("hidden-routing-internals"),
                List.of("decision-delta-analysis"),
                "Selected-vs-alternative diff is derived from visible returned evidence.",
                "diff readout does not execute replay");
    }

    private static EvidencePacketReadoutV1 evidencePacketReadout() {
        return new EvidencePacketReadoutV1(
                "future-evidence-packet",
                "NOT_IMPLEMENTED",
                "evidence-lane",
                "PLANNED",
                List.of("no evidence packet generation in this DTO skeleton"),
                "evidence packet readout is a reference only");
    }

    private static AgentStructuredOutputV1 agentStructuredOutput() {
        return new AgentStructuredOutputV1(
                "AgentStructuredOutputV1",
                "v1",
                List.of("payloadObject", "decisionReadout", "candidateSet", "notProvenBoundaries"),
                List.of("use stable field names", "do not infer hidden internals from null values"),
                List.of("Which candidate was selected?", "Which boundaries remain not proven?"),
                List.of("no autonomous production action", "no live mutation", "no hidden side effects"),
                notProvenBoundaries(),
                "agent output is for structured understanding only");
    }

    private static List<String> notProvenBoundaries() {
        return List.of(
                "no production readiness",
                "no production certification",
                "no live-cloud validation",
                "no real-tenant validation",
                "no benchmark/load/stress proof",
                "no throughput/p95/p99 proof",
                "no replay/export proof",
                "no runtime endpoint/UI/storage/evidence-packet implementation");
    }
}
