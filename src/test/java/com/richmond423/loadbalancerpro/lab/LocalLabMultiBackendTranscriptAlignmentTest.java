package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class LocalLabMultiBackendTranscriptAlignmentTest {
    private static final List<Integer> COMMON_FIXED_PORTS = List.of(0, 80, 443, 8080, 8081, 9090);
    private static final List<LocalLabFakeBackendBehaviorProfile> REQUIRED_PROFILES = List.of(
            LocalLabFakeBackendBehaviorProfile.HEALTHY_FAST,
            LocalLabFakeBackendBehaviorProfile.SLOW_TAIL_LATENCY,
            LocalLabFakeBackendBehaviorProfile.PARTIAL_DEGRADATION,
            LocalLabFakeBackendBehaviorProfile.ERROR_PRONE,
            LocalLabFakeBackendBehaviorProfile.OVERLOADED_QUEUE_PRESSURE,
            LocalLabFakeBackendBehaviorProfile.ALL_UNHEALTHY_OR_NO_GOOD_CHOICE,
            LocalLabFakeBackendBehaviorProfile.RECOVERY);

    @Test
    void everyPassiveTranscriptScenarioMapsToRunningHarnessDescriptor() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            Map<String, LocalLabMultiBackendLoopbackHarness.Descriptor> descriptorsByScenario =
                    descriptorsByScenario(harness);

            assertEquals(
                    LocalLabPassiveTranscriptCatalog.transcripts().stream()
                            .map(LocalLabPassiveTranscriptScenario::scenarioId)
                            .toList(),
                    harness.descriptors().stream()
                            .map(LocalLabMultiBackendLoopbackHarness.Descriptor::scenarioId)
                            .toList());

            for (LocalLabPassiveTranscriptScenario transcript : LocalLabPassiveTranscriptCatalog.transcripts()) {
                LocalLabMultiBackendLoopbackHarness.Descriptor descriptor =
                        descriptorsByScenario.get(transcript.scenarioId());
                assertEquals(transcript.scenarioId(), descriptor.scenarioId());
                assertEquals(transcript.behaviorType(), descriptor.behaviorType());
                assertEquals("127.0.0.1", descriptor.host());
                assertTrue(descriptor.assignedPort() > 0);
                assertFalse(COMMON_FIXED_PORTS.contains(descriptor.assignedPort()));
                assertTrue(descriptor.localUrl().startsWith("http://127.0.0.1:"));
                assertFalse(descriptor.localUrl().contains("0.0.0.0"));
                assertFalse(descriptor.localUrl().contains("localhost"));
            }

            assertFalse(harness.stopped());
        }
    }

    @Test
    void passiveTranscriptLabelsReturnExpectedFixtureResponsesThroughHarness() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            Map<String, LocalLabMultiBackendLoopbackHarness.Descriptor> descriptorsByScenario =
                    descriptorsByScenario(harness);

            for (LocalLabPassiveTranscriptScenario transcript : LocalLabPassiveTranscriptCatalog.transcripts()) {
                LocalLabMultiBackendLoopbackHarness.Descriptor descriptor =
                        descriptorsByScenario.get(transcript.scenarioId());
                LocalLabFakeBackendResponseFixture fixture = fixtureFor(transcript.scenarioId());

                for (LocalLabPassiveTranscriptEntry entry : transcript.entries()) {
                    LoopbackResponse response = get(requestUri(descriptor, entry));
                    String normalizedBody = normalize(response.body());

                    assertEquals(entry.expectedResponseStatusCode(), response.statusCode());
                    assertEquals(fixture.responseStatusCode(), response.statusCode());
                    assertTrue(response.body().contains("scenarioId=" + entry.scenarioId()));
                    assertTrue(response.body().contains("backendId=" + entry.backendId()));
                    assertTrue(response.body().contains("statusCode=" + entry.expectedResponseStatusCode()));
                    assertTrue(response.body().contains("latencyLabel=" + entry.expectedLatencyLabel()));
                    assertTrue(response.body().contains("bodySummary=" + entry.expectedResponseBodySummary()));
                    assertTrue(response.body().contains("errorLabel=" + entry.expectedErrorLabel()));
                    assertTrue(response.body().contains("loadLabel=" + entry.expectedLoadLabel()));
                    assertTrue(response.body().contains(fixture.evidenceNote()));
                    assertTrue(normalizedBody.contains("test-scope loopback fake backend server harness only"));
                    assertTrue(normalizedBody.contains("os-assigned ephemeral port"));
                    assertTrue(normalizedBody.contains("not production proof"));
                    assertTrue(normalizedBody.contains("not live-cloud validation"));
                    assertTrue(normalizedBody.contains("not real-tenant validation"));
                }
            }
        }
    }

    @Test
    void everyRequiredScenarioProfileRemainsCoveredByTranscriptFixtureAndHarnessAlignment() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            assertEquals(
                    REQUIRED_PROFILES.stream().map(Enum::name).toList(),
                    harness.descriptors().stream()
                            .map(LocalLabMultiBackendLoopbackHarness.Descriptor::behaviorType)
                            .toList());
            assertEquals(
                    REQUIRED_PROFILES.stream().map(Enum::name).toList(),
                    LocalLabPassiveTranscriptCatalog.transcripts().stream()
                            .map(LocalLabPassiveTranscriptScenario::behaviorType)
                            .toList());
            assertEquals(
                    REQUIRED_PROFILES.stream().map(Enum::name).toList(),
                    LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                            .map(LocalLabFakeBackendResponseFixture::behaviorType)
                            .toList());
        }
    }

    @Test
    void reviewerBoundaryExpectationsAlignWithHarnessResponses() throws Exception {
        Map<String, LocalLabPassiveReviewerChecklist> checklistsByScenario =
                LocalLabPassiveReviewerChecklistMapper.checklists().stream()
                        .collect(Collectors.toMap(
                                LocalLabPassiveReviewerChecklist::scenarioId,
                                Function.identity()));

        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            Map<String, LocalLabMultiBackendLoopbackHarness.Descriptor> descriptorsByScenario =
                    descriptorsByScenario(harness);

            for (LocalLabPassiveTranscriptScenario transcript : LocalLabPassiveTranscriptCatalog.transcripts()) {
                LocalLabMultiBackendLoopbackHarness.Descriptor descriptor =
                        descriptorsByScenario.get(transcript.scenarioId());
                LoopbackResponse response = get(requestUri(descriptor, transcript.entries().get(0)));
                LocalLabPassiveReviewerChecklist checklist = checklistsByScenario.get(transcript.scenarioId());
                String combined = normalize(response.body() + " " + checklist.deterministicText());

                assertTrue(combined.contains("what evidence was present"));
                assertTrue(combined.contains("what safety boundary was stated"));
                assertTrue(combined.contains("what was not proven"));
                assertTrue(combined.contains("local simulation is not production proof"));
                assertTrue(combined.contains("not production proof"));
                assertTrue(combined.contains("not live-cloud validation"));
                assertTrue(combined.contains("not real-tenant validation"));
            }
        }
    }

    @Test
    void unknownLabelBoundaryResponseRemainsStableThroughMultiBackendHarness() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor = harness.descriptors().get(0);
            URI uri = requestUri(
                    descriptor,
                    "unknown-transcript-alignment-scenario",
                    "unknown-transcript-alignment-backend",
                    "GET label",
                    "/local-lab/multi-backend/transcript-alignment/unknown path label");

            LoopbackResponse first = get(uri);
            LoopbackResponse second = get(uri);
            String normalizedBody = normalize(first.body());

            assertEquals(first, second);
            assertEquals(404, first.statusCode());
            assertTrue(first.body().contains("scenarioId=unknown-transcript-alignment-scenario"));
            assertTrue(first.body().contains("backendId=unknown-transcript-alignment-backend"));
            assertTrue(normalizedBody.contains("unknown scenario or backend label"));
            assertTrue(normalizedBody.contains("no matching test-scope response fixture"));
            assertTrue(normalizedBody.contains("not production proof"));
        }
    }

    @Test
    void repeatedAlignmentRunsAreDeterministicAndStopCleanly() throws Exception {
        List<String> firstAlignment = alignmentSnapshot();
        List<String> secondAlignment = alignmentSnapshot();

        assertEquals(firstAlignment, secondAlignment);

        LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start();
        List<LocalLabMultiBackendLoopbackHarness.Descriptor> descriptorsBeforeStop = harness.descriptors();

        assertFalse(harness.stopped());
        harness.close();

        assertTrue(harness.stopped());
        assertEquals(descriptorsBeforeStop, harness.descriptors());
    }

    @Test
    void alignmentBoundaryAvoidsToolRuntimeAndValidationOverclaims() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            Set<Integer> assignedPorts = new HashSet<>();

            for (LocalLabMultiBackendLoopbackHarness.Descriptor descriptor : harness.descriptors()) {
                String normalizedBoundary = normalize(descriptor.notProductionProofBoundary());

                assertEquals("127.0.0.1", descriptor.host());
                assertTrue(descriptor.assignedPort() > 0);
                assertFalse(COMMON_FIXED_PORTS.contains(descriptor.assignedPort()));
                assertTrue(assignedPorts.add(descriptor.assignedPort()));
                assertFalse(descriptor.localUrl().contains("0.0.0.0"));
                assertFalse(descriptor.localUrl().contains("localhost"));
                assertTrue(normalizedBoundary.contains("test-scope multi-backend loopback harness only"));
                assertTrue(normalizedBoundary.contains("docker"));
                assertTrue(normalizedBoundary.contains("k6"));
                assertTrue(normalizedBoundary.contains("bruno"));
                assertTrue(normalizedBoundary.contains("toxiproxy"));
                assertTrue(normalizedBoundary.contains("prometheus/grafana"));
                assertTrue(normalizedBoundary.contains("does not add production endpoints"));
                assertTrue(normalizedBoundary.contains("runtime behavior"));
                assertTrue(normalizedBoundary.contains("not production proof"));
                assertTrue(normalizedBoundary.contains("not live-cloud validation"));
                assertTrue(normalizedBoundary.contains("not real-tenant validation"));
                assertTrue(normalizedBoundary.contains("not production certification"));

                for (String forbidden : List.of(
                        "production-ready",
                        "production certified",
                        "production certification is proven",
                        "production validation is complete",
                        "live-cloud validated",
                        "live-cloud validation is complete",
                        "real-tenant validated",
                        "real-tenant validation is complete",
                        "docker compose is implemented",
                        "k6 scenario is implemented",
                        "bruno collection is implemented",
                        "toxiproxy config is implemented",
                        "prometheus/grafana dashboard is implemented",
                        "replay execution is implemented",
                        "evidence report is generated",
                        "report generation is implemented",
                        "storage is implemented",
                        "export behavior is implemented",
                        "runtime behavior is changed",
                        "routing behavior is changed",
                        "production api behavior is changed")) {
                    assertFalse(normalizedBoundary.contains(forbidden), descriptor.backendId()
                            + " must not overclaim " + forbidden);
                }
            }
        }
    }

    private static Map<String, LocalLabMultiBackendLoopbackHarness.Descriptor> descriptorsByScenario(
            LocalLabMultiBackendLoopbackHarness harness) {
        return harness.descriptors().stream()
                .collect(Collectors.toMap(
                        LocalLabMultiBackendLoopbackHarness.Descriptor::scenarioId,
                        Function.identity()));
    }

    private static LocalLabFakeBackendResponseFixture fixtureFor(String scenarioId) {
        return LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId(scenarioId).orElseThrow();
    }

    private static URI requestUri(
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor,
            LocalLabPassiveTranscriptEntry entry) {
        return requestUri(
                descriptor,
                entry.scenarioId(),
                entry.backendId(),
                entry.simulatedRequestMethodLabel(),
                entry.simulatedRequestPathLabel());
    }

    private static URI requestUri(
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor,
            String scenarioId,
            String backendId,
            String requestMethodLabel,
            String requestPathLabel) {
        String query = String.join("&",
                parameter("scenarioId", scenarioId),
                parameter("backendId", backendId),
                parameter("requestMethodLabel", requestMethodLabel),
                parameter("requestPathLabel", requestPathLabel),
                parameter("requestBodySummary", "transcript alignment test request body label for " + scenarioId),
                parameter("requestPurposeLabel", "test-scope multi-backend transcript alignment request label"));
        return URI.create(descriptor.localUrl() + "?" + query);
    }

    private static String parameter(String key, String value) {
        return encode(key) + "=" + encode(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static LoopbackResponse get(URI uri) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");
        try {
            int statusCode = connection.getResponseCode();
            String body = new String(
                    (statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream())
                            .readAllBytes(),
                    StandardCharsets.UTF_8);
            return new LoopbackResponse(statusCode, body);
        } finally {
            connection.disconnect();
        }
    }

    private static List<String> alignmentSnapshot() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            Map<String, LocalLabMultiBackendLoopbackHarness.Descriptor> descriptorsByScenario =
                    descriptorsByScenario(harness);
            List<String> snapshot = LocalLabPassiveTranscriptCatalog.transcripts().stream()
                    .map(transcript -> alignmentLine(descriptorsByScenario, transcript))
                    .toList();
            assertFalse(harness.stopped());
            return snapshot;
        }
    }

    private static String alignmentLine(
            Map<String, LocalLabMultiBackendLoopbackHarness.Descriptor> descriptorsByScenario,
            LocalLabPassiveTranscriptScenario transcript) {
        LocalLabMultiBackendLoopbackHarness.Descriptor descriptor =
                descriptorsByScenario.get(transcript.scenarioId());
        LocalLabPassiveTranscriptEntry entry = transcript.entries().get(0);
        return String.join("|",
                transcript.scenarioId(),
                transcript.behaviorType(),
                descriptor.backendId(),
                descriptor.host(),
                descriptor.behaviorLabel(),
                entry.simulatedRequestMethodLabel(),
                entry.simulatedRequestPathLabel(),
                Integer.toString(entry.expectedResponseStatusCode()),
                entry.expectedLatencyLabel(),
                entry.expectedErrorLabel(),
                entry.expectedLoadLabel());
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private record LoopbackResponse(int statusCode, String body) {
    }
}
