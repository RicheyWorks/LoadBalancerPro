package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabFakeBackendHandlerTranscriptConsistencyTest {
    @Test
    void everyPassiveTranscriptEntryConvertsToAHandledRequestThatMatchesFixtureExpectations() {
        for (LocalLabPassiveTranscriptScenario transcript : LocalLabPassiveTranscriptCatalog.transcripts()) {
            LocalLabFakeBackendResponseFixture fixture =
                    LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId(transcript.scenarioId())
                            .orElseThrow();

            for (LocalLabPassiveTranscriptEntry entry : transcript.entries()) {
                LocalLabFakeBackendRequest request = requestFrom(entry);
                LocalLabFakeBackendHandledResponse response = LocalLabFakeBackendHandler.handle(request);

                assertEquals(transcript.scenarioId(), request.scenarioId());
                assertEquals(entry.backendId(), request.backendId());
                assertEquals(entry.simulatedRequestMethodLabel(), request.requestMethodLabel());
                assertEquals(entry.simulatedRequestPathLabel(), request.requestPathLabel());
                assertEquals(fixture.scenarioId(), response.scenarioId());
                assertEquals(fixture.backendId(), response.backendId());
                assertEquals(fixture.responseStatusCode(), response.statusCode());
                assertEquals(entry.expectedResponseStatusCode(), response.statusCode());
                assertEquals(fixture.responseLatencyLabel(), response.latencyLabel());
                assertEquals(entry.expectedLatencyLabel(), response.latencyLabel());
                assertEquals(fixture.responseBodySummary(), response.bodySummary());
                assertEquals(entry.expectedResponseBodySummary(), response.bodySummary());
                assertEquals(fixture.simulatedErrorLabel(), response.errorLabel());
                assertEquals(entry.expectedErrorLabel(), response.errorLabel());
                assertEquals(fixture.simulatedLoadLabel(), response.loadLabel());
                assertEquals(entry.expectedLoadLabel(), response.loadLabel());
                assertTrue(response.evidenceNote().contains(fixture.evidenceNote()));
                assertTrue(response.evidenceNote().contains("Handler mapped simulated request labels"));
                assertTrue(response.safetyBoundary().contains("Test-scope fake backend handler only"));
                assertTrue(response.notProvenBoundary().contains(fixture.notProvenBoundary()));
            }
        }
    }

    @Test
    void transcriptDrivenHandlerRequestsAndResponsesAreDeterministicAcrossRepeatedCalls() {
        List<LocalLabFakeBackendRequest> firstRequests = transcriptRequests();
        List<LocalLabFakeBackendRequest> secondRequests = transcriptRequests();
        List<LocalLabFakeBackendHandledResponse> firstResponses = firstRequests.stream()
                .map(LocalLabFakeBackendHandler::handle)
                .toList();
        List<LocalLabFakeBackendHandledResponse> secondResponses = secondRequests.stream()
                .map(LocalLabFakeBackendHandler::handle)
                .toList();

        assertEquals(firstRequests, secondRequests);
        assertEquals(firstResponses, secondResponses);
        assertEquals(firstRequests.stream()
                .map(LocalLabFakeBackendRequest::deterministicText)
                .toList(), secondRequests.stream()
                .map(LocalLabFakeBackendRequest::deterministicText)
                .toList());
        assertEquals(firstResponses.stream()
                .map(LocalLabFakeBackendHandledResponse::deterministicText)
                .toList(), secondResponses.stream()
                .map(LocalLabFakeBackendHandledResponse::deterministicText)
                .toList());
    }

    @Test
    void transcriptDrivenHandlerResponsesKeepImplementationPrepBoundaries() {
        for (LocalLabFakeBackendRequest request : transcriptRequests()) {
            String normalized = normalize(LocalLabFakeBackendHandler.handle(request).deterministicText());

            assertTrue(normalized.contains("in memory"));
            assertTrue(normalized.contains("does not implement a fake backend server"));
            assertTrue(normalized.contains("start listeners"));
            assertTrue(normalized.contains("open ports"));
            assertTrue(normalized.contains("call loopback endpoints"));
            assertTrue(normalized.contains("generate traffic"));
            assertTrue(normalized.contains("not production proof"));
            assertTrue(normalized.contains("not live-cloud validation"));
            assertTrue(normalized.contains("not real-tenant validation"));
            assertTrue(normalized.contains("not replay execution"));
            assertTrue(normalized.contains("not storage"));
            assertTrue(normalized.contains("export"));
            assertTrue(normalized.contains("runtime"));

            for (String forbidden : List.of(
                    "production-ready",
                    "production certified",
                    "production certification is proven",
                    "live-cloud validated",
                    "real-tenant validated",
                    "actual traffic is generated",
                    "http request is executed",
                    "replay execution is implemented",
                    "evidence report is generated",
                    "storage is implemented",
                    "export behavior is implemented",
                    "runtime behavior is changed")) {
                assertFalse(normalized.contains(forbidden), request.scenarioId() + " must not overclaim "
                        + forbidden);
            }
        }
    }

    private static List<LocalLabFakeBackendRequest> transcriptRequests() {
        return LocalLabPassiveTranscriptCatalog.transcripts().stream()
                .flatMap(transcript -> transcript.entries().stream())
                .map(LocalLabFakeBackendHandlerTranscriptConsistencyTest::requestFrom)
                .toList();
    }

    private static LocalLabFakeBackendRequest requestFrom(LocalLabPassiveTranscriptEntry entry) {
        return new LocalLabFakeBackendRequest(
                entry.scenarioId(),
                entry.backendId(),
                entry.simulatedRequestMethodLabel(),
                entry.simulatedRequestPathLabel(),
                "test-scope transcript body label for " + entry.scenarioId(),
                "test-scope transcript-to-handler consistency label");
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
