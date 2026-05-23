package com.richmond423.loadbalancerpro.lab;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

final class LocalLabImplementationReadinessGate {
    private static final String GATE_ID = "local-lab-implementation-readiness-gate";
    private static final String READY_STATUS = "PASSIVE_FOUNDATION_READY_FOR_SEPARATE_IMPLEMENTATION_PR";
    private static final String BLOCKED_STATUS = "PASSIVE_FOUNDATION_BLOCKED_FOR_SEPARATE_IMPLEMENTATION_PR";
    private static final String PLANNED_TOOLING_BOUNDARY =
            "Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling.";
    private static final String FAKE_BACKEND_EXECUTION_BOUNDARY =
            "Fake backend execution remains future tooling only.";
    private static final String SAFETY_BOUNDARY =
            "Readiness gate is test-scope passive in-memory evaluation only; it does not implement a server, "
                    + "start listeners, open ports, call loopback endpoints, generate traffic, run tools, execute "
                    + "replay, write reports, persist storage, export artifacts, or change runtime behavior.";
    private static final String NOT_PROVEN_BOUNDARY =
            "Ready for a separate implementation PR is not production proof, not live-cloud validation, not "
                    + "real-tenant validation, not actual fake backend execution, and not replay execution, "
                    + "report generation, storage, export, or runtime behavior.";
    private static final List<String> VALIDATION_OVERCLAIMS = List.of(
            "production-ready",
            "production certified",
            "production certification is proven",
            "production validation is complete",
            "live-cloud validated",
            "live-cloud validation is complete",
            "real-tenant validated",
            "real-tenant validation is complete");
    private static final List<String> EXECUTION_OVERCLAIMS = List.of(
            "fake backend server is implemented",
            "actual fake backend execution is implemented",
            "actual traffic is generated",
            "http request is executed",
            "replay execution is implemented",
            "evidence report is generated",
            "report generation is implemented",
            "storage is implemented",
            "export behavior is implemented",
            "runtime behavior is changed",
            "routing behavior is changed");

    private LocalLabImplementationReadinessGate() {
    }

    static LocalLabImplementationReadinessAssessment assess() {
        List<LocalLabFakeBackendNodeScenario> scenarios = LocalLabScenarioCatalog.scenarios();
        List<LocalLabFakeBackendResponseFixture> fixtures = LocalLabFakeBackendResponseFixtureCatalog.fixtures();
        List<LocalLabPassiveTranscriptScenario> transcripts = LocalLabPassiveTranscriptCatalog.transcripts();
        List<LocalLabPassiveTranscriptSummary> summaries = LocalLabPassiveTranscriptSummaryRenderer.summaries();
        List<LocalLabPassiveReviewerChecklist> checklists = LocalLabPassiveReviewerChecklistMapper.checklists();
        List<String> passiveArtifactText = passiveArtifactText(scenarios, fixtures, transcripts, summaries,
                checklists);

        List<LocalLabImplementationReadinessCriterion> criteria = List.of(
                criterion(
                        "criterion-scenario-behavior-profiles-present",
                        "Every scenario has a behavior profile.",
                        !scenarios.isEmpty() && scenarios.stream()
                                .allMatch(scenario -> scenario.behaviorProfile() != null
                                        && !scenario.behaviorType().isBlank()),
                        "Scenario catalog carries behavior types for each passive local-lab scenario."),
                criterion(
                        "criterion-response-fixtures-cover-scenarios",
                        "Every scenario has a response fixture.",
                        ids(scenarios, LocalLabFakeBackendNodeScenario::backendId)
                                .equals(ids(fixtures, LocalLabFakeBackendResponseFixture::scenarioId)),
                        "Response fixture catalog covers the scenario catalog in deterministic order."),
                criterion(
                        "criterion-passive-transcripts-cover-scenarios",
                        "Every scenario has a passive transcript.",
                        ids(scenarios, LocalLabFakeBackendNodeScenario::backendId)
                                .equals(ids(transcripts, LocalLabPassiveTranscriptScenario::scenarioId)),
                        "Passive transcript catalog covers the scenario catalog in deterministic order."),
                criterion(
                        "criterion-summaries-cover-transcripts",
                        "Every passive transcript has a summary.",
                        ids(transcripts, LocalLabPassiveTranscriptScenario::transcriptId)
                                .equals(ids(summaries, LocalLabPassiveTranscriptSummary::transcriptId)),
                        "Summary renderer covers every passive transcript in deterministic order."),
                criterion(
                        "criterion-checklists-cover-summaries",
                        "Every summary has a reviewer checklist.",
                        ids(summaries, LocalLabPassiveTranscriptSummary::transcriptId)
                                .equals(ids(checklists, LocalLabPassiveReviewerChecklist::transcriptId)),
                        "Reviewer checklist mapper covers every passive transcript summary."),
                criterion(
                        "criterion-checklists-include-evidence-expectations",
                        "Every checklist includes evidence expectations.",
                        checklistItems(checklists).stream()
                                .allMatch(item -> !item.evidenceExpectation().isBlank()),
                        "Checklist items carry evidence expectation text for reviewer/operator inspection."),
                criterion(
                        "criterion-checklists-include-safety-boundaries",
                        "Every checklist includes safety boundaries.",
                        checklistItems(checklists).stream()
                                .allMatch(item -> !item.safetyBoundary().isBlank()),
                        "Checklist items carry safety boundaries for passive local-lab interpretation."),
                criterion(
                        "criterion-checklists-include-not-proven-boundaries",
                        "Every checklist includes not-proven boundaries.",
                        checklistItems(checklists).stream()
                                .allMatch(item -> !item.notProvenBoundary().isBlank()),
                        "Checklist items carry not-proven boundaries for reviewer/operator interpretation."),
                criterion(
                        "criterion-checklists-include-local-simulation-warning",
                        "Every checklist includes a local-simulation-is-not-production-proof warning.",
                        checklists.stream()
                                .map(LocalLabPassiveReviewerChecklist::deterministicText)
                                .map(LocalLabImplementationReadinessGate::normalize)
                                .allMatch(text -> text.contains("local simulation is not production proof")),
                        "Checklist text preserves the local simulation warning before any implementation work."),
                criterion(
                        "criterion-doc-tooling-boundary-is-future-only",
                        "Docs identify Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana as future tooling only.",
                        plannedToolingBoundaryIsComplete(),
                        PLANNED_TOOLING_BOUNDARY),
                criterion(
                        "criterion-doc-fake-backend-execution-boundary-is-future-only",
                        "Docs identify fake backend execution as future tooling only.",
                        normalize(FAKE_BACKEND_EXECUTION_BOUNDARY).contains("future tooling only"),
                        FAKE_BACKEND_EXECUTION_BOUNDARY),
                criterion(
                        "criterion-passive-artifacts-avoid-validation-overclaims",
                        "Passive artifacts do not claim production, live-cloud, or real-tenant validation.",
                        !containsForbidden(passiveArtifactText, VALIDATION_OVERCLAIMS),
                        "Passive artifacts keep validation claims bounded to local-lab planning/test evidence."),
                criterion(
                        "criterion-passive-artifacts-avoid-execution-storage-export-runtime-overclaims",
                        "Passive artifacts do not claim replay execution, storage, export, or runtime behavior.",
                        !containsForbidden(passiveArtifactText, EXECUTION_OVERCLAIMS),
                        "Passive artifacts avoid execution, storage, export, and runtime behavior claims."));

        int passedCount = (int) criteria.stream()
                .filter(LocalLabImplementationReadinessCriterion::passed)
                .count();
        int blockedCount = criteria.size() - passedCount;
        String status = blockedCount == 0 ? READY_STATUS : BLOCKED_STATUS;

        return new LocalLabImplementationReadinessAssessment(
                GATE_ID,
                status,
                criteria.size(),
                passedCount,
                blockedCount,
                criteria,
                "Passive local-lab planning/test chain is coherent enough to plan the next small implementation "
                        + "step only when separately scoped; this is not production proof.",
                "Future separately scoped fake backend server implementation PR with explicit networking, tool, "
                        + "runtime, and not-proven boundaries.",
                SAFETY_BOUNDARY + " " + PLANNED_TOOLING_BOUNDARY + " " + FAKE_BACKEND_EXECUTION_BOUNDARY,
                NOT_PROVEN_BOUNDARY);
    }

    private static LocalLabImplementationReadinessCriterion criterion(
            String criterionId,
            String criterionDescription,
            boolean passed,
            String evidenceSummary) {
        return new LocalLabImplementationReadinessCriterion(
                criterionId,
                criterionDescription,
                passed,
                evidenceSummary,
                SAFETY_BOUNDARY,
                NOT_PROVEN_BOUNDARY);
    }

    private static List<LocalLabPassiveReviewerChecklistItem> checklistItems(
            List<LocalLabPassiveReviewerChecklist> checklists) {
        return checklists.stream()
                .flatMap(checklist -> checklist.items().stream())
                .toList();
    }

    private static <T> List<String> ids(List<T> values, Function<T, String> mapper) {
        return values.stream()
                .map(mapper)
                .toList();
    }

    private static boolean plannedToolingBoundaryIsComplete() {
        String normalized = normalize(PLANNED_TOOLING_BOUNDARY);
        return List.of("docker", "k6", "bruno", "toxiproxy", "prometheus", "grafana", "future tooling")
                .stream()
                .allMatch(normalized::contains);
    }

    private static boolean containsForbidden(List<String> values, List<String> forbiddenPhrases) {
        List<String> normalizedValues = values.stream()
                .map(LocalLabImplementationReadinessGate::normalize)
                .toList();
        return forbiddenPhrases.stream()
                .map(LocalLabImplementationReadinessGate::normalize)
                .anyMatch(forbidden -> normalizedValues.stream().anyMatch(value -> value.contains(forbidden)));
    }

    private static List<String> passiveArtifactText(
            List<LocalLabFakeBackendNodeScenario> scenarios,
            List<LocalLabFakeBackendResponseFixture> fixtures,
            List<LocalLabPassiveTranscriptScenario> transcripts,
            List<LocalLabPassiveTranscriptSummary> summaries,
            List<LocalLabPassiveReviewerChecklist> checklists) {
        return Stream.of(
                        scenarios.stream().map(LocalLabImplementationReadinessGate::scenarioText),
                        fixtures.stream().map(LocalLabImplementationReadinessGate::fixtureText),
                        transcripts.stream().map(LocalLabImplementationReadinessGate::transcriptText),
                        summaries.stream().map(LocalLabPassiveTranscriptSummary::deterministicText),
                        checklists.stream().map(LocalLabPassiveReviewerChecklist::deterministicText))
                .flatMap(Function.identity())
                .toList();
    }

    private static String scenarioText(LocalLabFakeBackendNodeScenario scenario) {
        return String.join(" ",
                scenario.backendId(),
                scenario.backendName(),
                scenario.behaviorType(),
                scenario.evidenceExpectationSummary(),
                scenario.notProvenBoundary());
    }

    private static String fixtureText(LocalLabFakeBackendResponseFixture fixture) {
        return String.join(" ",
                fixture.fixtureId(),
                fixture.scenarioId(),
                fixture.backendId(),
                fixture.behaviorType(),
                Integer.toString(fixture.responseStatusCode()),
                fixture.responseLatencyLabel(),
                fixture.responseBodySummary(),
                fixture.simulatedErrorLabel(),
                fixture.simulatedLoadLabel(),
                fixture.evidenceNote(),
                fixture.notProvenBoundary());
    }

    private static String transcriptText(LocalLabPassiveTranscriptScenario transcript) {
        return String.join(" ",
                transcript.transcriptId(),
                transcript.scenarioId(),
                transcript.fixtureId(),
                transcript.behaviorType(),
                String.join(" | ", transcript.entries().stream()
                        .map(LocalLabImplementationReadinessGate::transcriptEntryText)
                        .toList()),
                transcript.notProvenBoundary());
    }

    private static String transcriptEntryText(LocalLabPassiveTranscriptEntry entry) {
        return String.join(" ",
                entry.transcriptId(),
                entry.scenarioId(),
                entry.fixtureId(),
                Integer.toString(entry.stepNumber()),
                entry.simulatedRequestMethodLabel(),
                entry.simulatedRequestPathLabel(),
                entry.backendId(),
                Integer.toString(entry.expectedResponseStatusCode()),
                entry.expectedLatencyLabel(),
                entry.expectedResponseBodySummary(),
                entry.expectedErrorLabel(),
                entry.expectedLoadLabel(),
                entry.routingEvidenceObservationNote(),
                entry.safetyBoundaryNote(),
                entry.notProvenBoundary());
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
