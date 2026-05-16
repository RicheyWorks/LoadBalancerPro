package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class EnterpriseLabDecisionVectorDocumentationTest {
    private static final Path DECISION_VECTOR = Path.of("docs/ENTERPRISE_LAB_DECISION_VECTOR.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path FRAMING = Path.of("docs/ENTERPRISE_LAB_COCKPIT_FRAMING.md");
    private static final Path ROUTING_COCKPIT = Path.of("src/main/resources/static/routing-demo.html");

    @Test
    void decisionVectorDocDefinesControlledLabContract() throws Exception {
        String doc = read(DECISION_VECTOR);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Vector Contract"));
        assertTrue(doc.contains(
                "LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo."));
        assertTrue(doc.contains(
                "The Enterprise Lab Decision Vector is the structured explanation object for one controlled lab routing decision."));
        assertTrue(doc.contains("## Decision Vector Fields"));
        assertTrue(doc.contains("`selectedStrategy`"));
        assertTrue(doc.contains("`selectedBackend`"));
        assertTrue(doc.contains("`candidateBackends`"));
        assertTrue(doc.contains("`selectedCandidateVector`"));
        assertTrue(doc.contains("`nonSelectedCandidateVectors`"));
        assertTrue(doc.contains("`visibleCandidateSignals`"));
        assertTrue(doc.contains("`knownSignals`"));
        assertTrue(doc.contains("`unknownSignals`"));
        assertTrue(doc.contains("`exactScoringAvailability`"));
        assertTrue(doc.contains("`factorContributionAvailability`"));
        assertTrue(doc.contains("`replayReadiness`"));
        assertTrue(normalized.contains("how it answers why this backend"));
        assertTrue(normalized.contains("selected-vs-alternative"));
        assertTrue(normalized.contains("known visible signals from unknown or unexposed signals"));
    }

    @Test
    void candidateDecisionVectorAndFactorPlaceholderAreBounded() throws Exception {
        String doc = read(DECISION_VECTOR);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("## Candidate Decision Vector"));
        assertTrue(doc.contains("`candidateId` or `candidateName`"));
        assertTrue(doc.contains("`selected`"));
        assertTrue(doc.contains("`healthState`"));
        assertTrue(doc.contains("`latencySignal`"));
        assertTrue(doc.contains("`loadOrConnectionPressureSignal`"));
        assertTrue(doc.contains("`capacityOrWeightSignal`"));
        assertTrue(doc.contains("`visibleSupportSignals`"));
        assertTrue(doc.contains("`visibleCautionSignals`"));
        assertTrue(doc.contains("`selectionExplanation`"));
        assertTrue(doc.contains("`fallbackExplanation`"));
        assertTrue(doc.contains("Candidate reason is unknown from visible data"));
        assertTrue(doc.contains("## Factor Contribution Placeholder Contract"));
        assertTrue(doc.contains("`factorName`"));
        assertTrue(doc.contains("`rawValue`"));
        assertTrue(doc.contains("`normalizedValue`"));
        assertTrue(doc.contains("`direction`"));
        assertTrue(doc.contains("`contributionValue`"));
        assertTrue(doc.contains("`weight`"));
        assertTrue(normalized.contains("future extension unless the api explicitly exposes contribution data"));
        assertTrue(normalized.contains("contributionvalue"));
        assertTrue(normalized.contains("future/not implemented"));
        assertTrue(normalized.contains("exact production scoring is not claimed"));
        assertTrue(normalized.contains("hidden scoring must not be inferred"));
        assertTrue(doc.contains("## From Decision Vector to Factor Contributions"));
        assertTrue(doc.contains("ServerScoreCalculator"));
        assertTrue(doc.contains("It explains current calculator components; it does not retune weights."));
        assertTrue(doc.contains("It preserves existing score values and routing selection behavior."));
        assertTrue(doc.contains("## Candidate Factor Contribution Integration"));
        assertTrue(doc.contains("CandidateFactorContributionSummary"));
        assertTrue(doc.contains("selected and non-selected candidates carry the same contribution summary shape"));
        assertTrue(doc.contains("This sprint does not implement a runtime Decision Vector API field"));
        assertTrue(doc.contains("\"factorContributionSummary\""));
        assertTrue(doc.contains("It does not implement decision replay, what-if experiments, or structured decision logging."));
        assertTrue(doc.contains("\"factorContributions\""));
        assertTrue(doc.contains("\"EXACT_FROM_CALCULATOR\""));
        assertTrue(doc.contains("\"NOT_EXPOSED\""));
        assertTrue(doc.contains("not production scoring proof"));
    }

    @Test
    void decisionVectorRoadmapIsFutureAndNotImplemented() throws Exception {
        String doc = read(DECISION_VECTOR);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("## Replay, What-If, Logging, and Plugin Roadmap"));
        for (String futureItem : List.of(
                "Factor contribution analysis: future/not implemented.",
                "Decision replay: future/not implemented.",
                "What-if experiments: future/not implemented.",
                "Structured decision logging: future/not implemented.",
                "Strategy plugin explainability: future/not implemented.",
                "Rack, zone, and topology modeling: future/not implemented.",
                "Correlated failure modeling: future/not implemented.",
                "Live interrogation mode: future/not implemented.")) {
            assertTrue(doc.contains(futureItem), "roadmap item should stay explicitly future: " + futureItem);
        }

        assertTrue(doc.contains("## Static Example Decision Vector Payload"));
        assertTrue(doc.contains("\"factorContributionAvailability\": \"futureNotImplementedUnlessExposedByApi\""));
        assertTrue(doc.contains("\"replayReadiness\": \"plannedFutureContract; replay execution is not implemented\""));
        assertTrue(doc.contains("\"whatIfReadiness\": \"plannedFutureContract; what-if execution is not implemented\""));
        assertTrue(doc.contains("\"structuredDecisionLoggingReadiness\": \"plannedFutureContract; structured logging is not implemented\""));
        assertFalse(normalized.contains("factor contribution analysis is implemented"));
        assertFalse(normalized.contains("decision replay is implemented"));
        assertFalse(normalized.contains("what-if experiments are implemented"));
        assertFalse(normalized.contains("strategy plugin explainability is implemented"));
    }

    @Test
    void decisionVectorDocIsLinkedFromReviewerDocs() throws Exception {
        String readme = read(README);
        String trustMap = read(TRUST_MAP);
        String framing = read(FRAMING);

        for (String doc : List.of(readme, trustMap, framing)) {
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_VECTOR.md"));
            assertTrue(doc.contains("Decision Vector"));
        }

        assertTrue(readme.contains("Decision Vector contract: [`docs/ENTERPRISE_LAB_DECISION_VECTOR.md`](docs/ENTERPRISE_LAB_DECISION_VECTOR.md)."));
        assertTrue(trustMap.contains("### Decision Vector Contract"));
        assertTrue(framing.contains("## Decision Vector Contract"));
    }

    @Test
    void routingCockpitIncludesDecisionVectorFoundation() throws Exception {
        String page = read(ROUTING_COCKPIT);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"decision-vector-foundation-panel\""));
        assertTrue(page.contains("Decision Vector Foundation"));
        assertTrue(page.contains("Structured Decision Evidence"));
        assertTrue(page.contains("data-copy-target=\"decision-vector-summary-output\""));
        assertTrue(page.contains("A structured explanation object for one controlled lab routing decision"));
        assertTrue(page.contains("Candidate Decision Vector"));
        assertTrue(page.contains("Known vs unknown separation"));
        assertTrue(page.contains("ServerScoreCalculator factor contribution contract extraction has begun"));
        assertTrue(page.contains("candidate summaries can attach those explanations to selected and non-selected candidate vectors"));
        assertTrue(page.contains("no score weights are retuned"));
        assertTrue(page.contains("Decision replay, what-if experiments, and structured decision logging should build on this contract later"));
        assertTrue(page.contains("id=\"decision-vector-selected-strategy\""));
        assertTrue(page.contains("id=\"decision-vector-selected-backend\""));
        assertTrue(page.contains("id=\"decision-vector-candidate-count\""));
        assertTrue(page.contains("id=\"decision-vector-known-signals\""));
        assertTrue(page.contains("id=\"decision-vector-unknown-signals\""));
        assertTrue(page.contains("id=\"decision-vector-scoring-availability\""));
        assertTrue(page.contains("id=\"decision-vector-factor-contribution\""));
        assertTrue(page.contains("id=\"decision-vector-replay-readiness\""));
        assertTrue(page.contains("# Decision Vector Foundation"));
        assertTrue(page.contains("decisionIdOrLabRunId: "));
        assertTrue(page.contains("candidateVectors: "));
        assertTrue(page.contains("exactScoringAvailability: "));
        assertTrue(page.contains("factorContributionAvailability: "));
        assertTrue(page.contains("internal calculator contribution and candidate summary contracts started"));
        assertTrue(page.contains("replayReadiness: "));
        assertTrue(page.contains("whatIfReadiness: planned future contract; what-if execution is not implemented"));
        assertTrue(page.contains("structuredDecisionLoggingReadiness: planned future contract; structured decision logging is not implemented"));
        assertTrue(page.contains("copyBoundary: browser-local copy action only; no upload/share endpoint; no server-side export/PDF/ZIP generation; no external calls; no telemetry"));
        assertTrue(normalized.contains("hidden scoring is not invented"));
        assertTrue(normalized.contains("exact production scoring is not claimed unless exposed by the api"));
    }

    @Test
    void decisionVectorDocsAndUiAvoidUnsafeClaims() throws Exception {
        for (Path path : List.of(DECISION_VECTOR, README, TRUST_MAP, FRAMING, ROUTING_COCKPIT)) {
            String content = read(path);
            String normalized = content.toLowerCase(Locale.ROOT);

            assertTrue(content.contains("Enterprise Lab Cockpit"), path + " should preserve Enterprise Lab framing");
            assertTrue(normalized.contains("not a demo"), path + " should preserve not-a-demo framing");
            assertFalse(normalized.contains("hidden scoring is available"), path + " must not expose hidden scoring");
            assertFalse(normalized.contains("hidden scoring is inferred"), path + " must not infer hidden scoring");
            assertFalse(normalized.contains("exact production scoring is claimed"), path + " must not claim exact scoring");
            assertFalse(normalized.contains("completed factor contribution analysis is implemented"),
                    path + " must not claim completed factor contributions");
            assertFalse(normalized.contains("completed replay is implemented"), path + " must not claim completed replay");
            assertFalse(normalized.contains("completed what-if experiments are implemented"),
                    path + " must not claim completed what-if work");
            assertFalse(normalized.contains("production telemetry is available"), path + " must not claim production telemetry");
            assertFalse(normalized.contains("production monitoring is available"), path + " must not claim production monitoring");
            assertFalse(normalized.contains("upload endpoint"), path + " must not add upload endpoints");
            assertFalse(normalized.contains("server-side decision vector export"), path + " must not add server-side export behavior");
            assertFalse(normalized.contains("fetch(\"https://"), path + " must not add external calls");
            assertFalse(normalized.contains("fetch('https://"), path + " must not add external calls");
            assertFalse(normalized.contains("sendbeacon"), path + " must not add telemetry");
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
