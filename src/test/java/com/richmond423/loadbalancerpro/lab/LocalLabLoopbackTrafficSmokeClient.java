package com.richmond423.loadbalancerpro.lab;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class LocalLabLoopbackTrafficSmokeClient {
    static final String SMOKE_CLIENT_BOUNDARY =
            "Test-scope deterministic loopback traffic smoke client only; calls only src/test/java "
                    + "multi-backend loopback harness URLs on 127.0.0.1 with harness-assigned ephemeral "
                    + "ports; not k6; not Bruno; not Docker; not Toxiproxy; not production traffic; "
                    + "not production proof; not live-cloud validation; not real-tenant validation; "
                    + "not production certification; no replay execution, evidence/report generation, "
                    + "file writing, storage, export, runtime, routing, scoring, strategy, proxy, or API "
                    + "behavior changes.";

    private static final String HANDLER_SAFETY_LABEL = "Test-scope fake backend handler only";
    private static final String NOT_PROVEN_LABEL = "not production proof";
    private static final String UNKNOWN_SCENARIO_ID = "unknown-loopback-traffic-smoke-scenario";
    private static final String UNKNOWN_BACKEND_ID = "unknown-loopback-traffic-smoke-backend";

    private LocalLabLoopbackTrafficSmokeClient() {
    }

    static List<LocalLabLoopbackTrafficSmokeObservation> smoke(
            LocalLabMultiBackendLoopbackHarness harness) throws IOException {
        if (harness == null) {
            throw new IllegalArgumentException("harness is required");
        }
        return smoke(requestsFor(harness.descriptors()));
    }

    static List<LocalLabLoopbackTrafficSmokeObservation> smoke(
            List<LocalLabLoopbackTrafficSmokeRequest> requests) throws IOException {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("requests are required");
        }

        List<LocalLabLoopbackTrafficSmokeObservation> observations = new ArrayList<>();
        for (LocalLabLoopbackTrafficSmokeRequest request : requests) {
            observations.add(observe(request));
        }
        return List.copyOf(observations);
    }

    static List<LocalLabLoopbackTrafficSmokeRequest> requestsFor(
            List<LocalLabMultiBackendLoopbackHarness.Descriptor> descriptors) {
        if (descriptors == null || descriptors.isEmpty()) {
            throw new IllegalArgumentException("descriptors are required");
        }

        List<LocalLabLoopbackTrafficSmokeRequest> requests = new ArrayList<>();
        for (LocalLabMultiBackendLoopbackHarness.Descriptor descriptor : descriptors) {
            LocalLabFakeBackendResponseFixture fixture =
                    LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId(descriptor.scenarioId())
                            .orElseThrow();
            LocalLabPassiveTranscriptEntry entry =
                    LocalLabPassiveTranscriptCatalog.findByScenarioId(fixture.scenarioId())
                            .orElseThrow()
                            .entries()
                            .get(0);
            requests.add(requestFor(descriptor, fixture, entry));
        }

        LocalLabMultiBackendLoopbackHarness.Descriptor boundaryDescriptor = descriptors.get(0);
        requests.add(boundaryRequest(boundaryDescriptor));
        return List.copyOf(requests);
    }

    private static LocalLabLoopbackTrafficSmokeObservation observe(
            LocalLabLoopbackTrafficSmokeRequest request) throws IOException {
        Response response = get(request.loopbackUri());
        String body = response.body();
        String normalizedBody = body.toLowerCase(Locale.ROOT);
        boolean boundary = response.statusCode() == 404;

        boolean matchedExpected = !boundary
                && response.statusCode() == request.expectedStatusCode()
                && body.contains("scenarioId=" + request.scenarioId())
                && body.contains("backendId=" + request.backendId())
                && body.contains("latencyLabel=" + request.expectedLatencyLabel())
                && containsErrorLoadLabels(body, request.expectedErrorLoadLabel());

        return new LocalLabLoopbackTrafficSmokeObservation(
                request.scenarioId(),
                request.backendId(),
                boundary ? "UNKNOWN_BOUNDARY" : behaviorTypeFor(request.scenarioId()),
                request.loopbackUri().getHost(),
                request.loopbackUri().getPort(),
                request.expectedStatusCode(),
                response.statusCode(),
                clean(body),
                matchedExpected,
                body.contains(request.expectedEvidenceLabel()),
                body.contains(request.safetyBoundaryLabel()),
                normalizedBody.contains(request.notProvenBoundaryLabel().toLowerCase(Locale.ROOT)),
                boundary,
                SMOKE_CLIENT_BOUNDARY);
    }

    private static LocalLabLoopbackTrafficSmokeRequest requestFor(
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor,
            LocalLabFakeBackendResponseFixture fixture,
            LocalLabPassiveTranscriptEntry entry) {
        return new LocalLabLoopbackTrafficSmokeRequest(
                fixture.scenarioId(),
                fixture.backendId(),
                requestUri(
                        descriptor,
                        fixture.scenarioId(),
                        fixture.backendId(),
                        entry.simulatedRequestMethodLabel(),
                        entry.simulatedRequestPathLabel(),
                        "deterministic loopback traffic smoke request body label for " + fixture.scenarioId()),
                entry.simulatedRequestMethodLabel(),
                entry.simulatedRequestPathLabel(),
                fixture.responseStatusCode(),
                fixture.responseLatencyLabel(),
                errorLoadLabel(fixture.simulatedErrorLabel(), fixture.simulatedLoadLabel()),
                fixture.evidenceNote(),
                HANDLER_SAFETY_LABEL,
                NOT_PROVEN_LABEL);
    }

    private static LocalLabLoopbackTrafficSmokeRequest boundaryRequest(
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor) {
        return new LocalLabLoopbackTrafficSmokeRequest(
                UNKNOWN_SCENARIO_ID,
                UNKNOWN_BACKEND_ID,
                requestUri(
                        descriptor,
                        UNKNOWN_SCENARIO_ID,
                        UNKNOWN_BACKEND_ID,
                        "GET label",
                        "/local-lab/traffic-smoke/unknown path label",
                        "deterministic loopback traffic smoke unknown request body label"),
                "GET label",
                "/local-lab/traffic-smoke/unknown path label",
                404,
                "boundary response with no fixture latency label",
                errorLoadLabel(
                        "unknown scenario or backend label",
                        "load not evaluated because no fixture matched"),
                "no matching test-scope response fixture",
                HANDLER_SAFETY_LABEL,
                NOT_PROVEN_LABEL);
    }

    private static URI requestUri(
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor,
            String scenarioId,
            String backendId,
            String requestMethodLabel,
            String requestPathLabel,
            String requestBodySummary) {
        String query = String.join("&",
                parameter("scenarioId", scenarioId),
                parameter("backendId", backendId),
                parameter("requestMethodLabel", requestMethodLabel),
                parameter("requestPathLabel", requestPathLabel),
                parameter("requestBodySummary", requestBodySummary),
                parameter("requestPurposeLabel", "test-scope deterministic loopback traffic smoke request label"));
        return URI.create(descriptor.localUrl() + "?" + query);
    }

    private static Response get(URI uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");
        try {
            int statusCode = connection.getResponseCode();
            String body = new String(
                    (statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream())
                            .readAllBytes(),
                    StandardCharsets.UTF_8);
            return new Response(statusCode, body);
        } finally {
            connection.disconnect();
        }
    }

    private static String parameter(String key, String value) {
        return encode(key) + "=" + encode(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean containsErrorLoadLabels(String body, String expectedErrorLoadLabel) {
        String[] labels = expectedErrorLoadLabel.split(" \\| ", -1);
        return body.contains("errorLabel=" + labels[0])
                && labels.length == 2
                && body.contains("loadLabel=" + labels[1]);
    }

    private static String errorLoadLabel(String errorLabel, String loadLabel) {
        return errorLabel + " | " + loadLabel;
    }

    private static String behaviorTypeFor(String scenarioId) {
        return LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId(scenarioId)
                .orElseThrow()
                .behaviorType();
    }

    private static String clean(String value) {
        return value.replace('\r', ' ').replace('\n', ' ');
    }

    private record Response(int statusCode, String body) {
    }
}
