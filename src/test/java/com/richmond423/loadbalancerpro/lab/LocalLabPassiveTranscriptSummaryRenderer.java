package com.richmond423.loadbalancerpro.lab;

import java.util.List;

final class LocalLabPassiveTranscriptSummaryRenderer {
    private static final String SUMMARY_NOT_PROVEN_BOUNDARY =
            "Test-scope passive transcript summary renderer only; not production proof; not live-cloud validation; "
                    + "not real-tenant validation; no Docker, k6, Bruno, or Toxiproxy execution is required; "
                    + "no fake backend server, listener, port, loopback HTTP call, generated traffic, replay "
                    + "execution, evidence report generation, file writing, storage, export, runtime behavior, "
                    + "routing, scoring, strategy, proxy, API, or production validation behavior is added.";

    private LocalLabPassiveTranscriptSummaryRenderer() {
    }

    static List<LocalLabPassiveTranscriptSummary> summaries() {
        return render(LocalLabPassiveTranscriptCatalog.transcripts());
    }

    static List<LocalLabPassiveTranscriptSummary> render(List<LocalLabPassiveTranscriptScenario> transcripts) {
        if (transcripts == null || transcripts.isEmpty()) {
            throw new IllegalArgumentException("transcripts are required");
        }
        return transcripts.stream()
                .map(LocalLabPassiveTranscriptSummaryRenderer::summary)
                .toList();
    }

    private static LocalLabPassiveTranscriptSummary summary(LocalLabPassiveTranscriptScenario transcript) {
        return new LocalLabPassiveTranscriptSummary(
                "summary-" + transcript.transcriptId(),
                transcript.scenarioId(),
                transcript.behaviorType(),
                transcript.transcriptId(),
                transcript.entries().size(),
                transcript.entries().stream()
                        .map(LocalLabPassiveTranscriptEntry::backendId)
                        .toList(),
                transcript.entries().stream()
                        .map(LocalLabPassiveTranscriptSummaryRenderer::requestLabel)
                        .toList(),
                transcript.entries().stream()
                        .map(entry -> entry.expectedResponseStatusCode() + " status label")
                        .toList(),
                transcript.entries().stream()
                        .map(LocalLabPassiveTranscriptEntry::expectedLatencyLabel)
                        .toList(),
                transcript.entries().stream()
                        .map(LocalLabPassiveTranscriptSummaryRenderer::errorLoadLabel)
                        .toList(),
                transcript.entries().stream()
                        .map(LocalLabPassiveTranscriptEntry::routingEvidenceObservationNote)
                        .collect(java.util.stream.Collectors.joining(" | ")),
                transcript.entries().stream()
                        .map(LocalLabPassiveTranscriptEntry::safetyBoundaryNote)
                        .collect(java.util.stream.Collectors.joining(" | ")),
                transcript.notProvenBoundary() + " Summary boundary: " + SUMMARY_NOT_PROVEN_BOUNDARY);
    }

    private static String requestLabel(LocalLabPassiveTranscriptEntry entry) {
        return entry.simulatedRequestMethodLabel() + " " + entry.simulatedRequestPathLabel();
    }

    private static String errorLoadLabel(LocalLabPassiveTranscriptEntry entry) {
        return entry.expectedErrorLabel() + " | " + entry.expectedLoadLabel();
    }
}
