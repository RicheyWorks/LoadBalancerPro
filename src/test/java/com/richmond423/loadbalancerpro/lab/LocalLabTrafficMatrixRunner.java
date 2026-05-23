package com.richmond423.loadbalancerpro.lab;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class LocalLabTrafficMatrixRunner {
    static final String TRAFFIC_MATRIX_BOUNDARY =
            "Test-scope deterministic traffic matrix only; uses the existing src/test/java multi-backend "
                    + "loopback harness and smoke client; calls only 127.0.0.1 harness URLs with "
                    + "harness-assigned ephemeral ports; not k6; not Bruno; not Docker; not Toxiproxy; "
                    + "not load testing; not stress testing; not production traffic; not production proof; "
                    + "not live-cloud validation; not real-tenant validation; not production certification; "
                    + "no fixed ports; no non-loopback target calls; no replay execution, evidence/report "
                    + "generation, file writing, environment reads, process execution, storage, export, "
                    + "runtime, routing, scoring, strategy, proxy, or API behavior changes.";

    private static final List<Integer> COMMON_FIXED_PORTS = List.of(0, 80, 443, 8080, 8081, 9090);

    private LocalLabTrafficMatrixRunner() {
    }

    static List<LocalLabTrafficMatrixObservation> run(
            LocalLabMultiBackendLoopbackHarness harness) throws IOException {
        if (harness == null) {
            throw new IllegalArgumentException("harness is required");
        }
        return run(harness.descriptors(), LocalLabTrafficMatrixCatalog.cases());
    }

    static List<LocalLabTrafficMatrixObservation> run(
            List<LocalLabMultiBackendLoopbackHarness.Descriptor> descriptors,
            List<LocalLabTrafficMatrixCase> cases) throws IOException {
        List<LocalLabLoopbackTrafficSmokeRequest> requests = requestsFor(descriptors, cases);
        List<LocalLabLoopbackTrafficSmokeObservation> smokeObservations =
                LocalLabLoopbackTrafficSmokeClient.smoke(requests);
        List<LocalLabTrafficMatrixObservation> matrixObservations = new ArrayList<>();

        for (int i = 0; i < cases.size(); i++) {
            LocalLabTrafficMatrixCase matrixCase = cases.get(i);
            LocalLabLoopbackTrafficSmokeObservation smokeObservation = smokeObservations.get(i);
            matrixObservations.add(new LocalLabTrafficMatrixObservation(
                    matrixCase.matrixCaseId(),
                    smokeObservation.scenarioId(),
                    smokeObservation.backendId(),
                    smokeObservation.host(),
                    smokeObservation.assignedPort(),
                    smokeObservation.responseStatusCode(),
                    smokeObservation.matchedExpectedFixture(),
                    smokeObservation.evidenceLabelFound(),
                    smokeObservation.safetyBoundaryLabelFound(),
                    smokeObservation.notProvenBoundaryLabelFound(),
                    smokeObservation.boundaryResponse(),
                    TRAFFIC_MATRIX_BOUNDARY));
        }
        return List.copyOf(matrixObservations);
    }

    static List<LocalLabLoopbackTrafficSmokeRequest> requestsFor(
            List<LocalLabMultiBackendLoopbackHarness.Descriptor> descriptors,
            List<LocalLabTrafficMatrixCase> cases) {
        if (descriptors == null || descriptors.isEmpty()) {
            throw new IllegalArgumentException("descriptors are required");
        }
        if (cases == null || cases.isEmpty()) {
            throw new IllegalArgumentException("matrix cases are required");
        }

        List<LocalLabLoopbackTrafficSmokeRequest> requests = new ArrayList<>();
        for (LocalLabTrafficMatrixCase matrixCase : cases) {
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor = descriptorFor(descriptors, matrixCase);
            requests.add(requestFor(descriptor, matrixCase));
        }
        return List.copyOf(requests);
    }

    private static LocalLabMultiBackendLoopbackHarness.Descriptor descriptorFor(
            List<LocalLabMultiBackendLoopbackHarness.Descriptor> descriptors,
            LocalLabTrafficMatrixCase matrixCase) {
        if (matrixCase.boundaryCase()) {
            return validated(descriptors.get(0));
        }
        return descriptors.stream()
                .filter(descriptor -> descriptor.scenarioId().equals(matrixCase.scenarioId()))
                .filter(descriptor -> descriptor.backendId().equals(matrixCase.backendId()))
                .findFirst()
                .map(LocalLabTrafficMatrixRunner::validated)
                .orElseThrow(() -> new IllegalArgumentException("No descriptor for matrix case "
                        + matrixCase.matrixCaseId()));
    }

    private static LocalLabMultiBackendLoopbackHarness.Descriptor validated(
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor) {
        URI uri = URI.create(descriptor.localUrl());
        if (!"127.0.0.1".equals(descriptor.host()) || !"127.0.0.1".equals(uri.getHost())) {
            throw new IllegalArgumentException("matrix runner accepts 127.0.0.1 loopback descriptors only");
        }
        if (descriptor.assignedPort() <= 0 || COMMON_FIXED_PORTS.contains(descriptor.assignedPort())) {
            throw new IllegalArgumentException("matrix runner requires positive non-common ephemeral ports");
        }
        if (uri.getPort() != descriptor.assignedPort()) {
            throw new IllegalArgumentException("descriptor port must match localUrl port");
        }
        if (!LocalLabLoopbackFakeBackendServer.ENDPOINT_PATH.equals(uri.getPath())) {
            throw new IllegalArgumentException("descriptor must target the test-scope fake backend endpoint");
        }
        return descriptor;
    }

    private static LocalLabLoopbackTrafficSmokeRequest requestFor(
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor,
            LocalLabTrafficMatrixCase matrixCase) {
        RequestLabels labels = labelsFor(matrixCase);
        return new LocalLabLoopbackTrafficSmokeRequest(
                matrixCase.scenarioId(),
                matrixCase.backendId(),
                requestUri(descriptor, matrixCase, labels),
                labels.methodLabel(),
                labels.pathLabel(),
                matrixCase.expectedStatusCode(),
                matrixCase.expectedLatencyLabel(),
                matrixCase.expectedErrorLoadLabel(),
                matrixCase.expectedEvidenceLabel(),
                matrixCase.expectedSafetyBoundaryLabel(),
                matrixCase.expectedNotProvenBoundaryLabel());
    }

    private static RequestLabels labelsFor(LocalLabTrafficMatrixCase matrixCase) {
        if (matrixCase.boundaryCase()) {
            return new RequestLabels(
                    "GET label",
                    "/local-lab/traffic-matrix/unknown path label",
                    "deterministic traffic matrix unknown request body label");
        }
        LocalLabPassiveTranscriptEntry entry =
                LocalLabPassiveTranscriptCatalog.findByScenarioId(matrixCase.scenarioId())
                        .orElseThrow()
                        .entries()
                        .get(0);
        return new RequestLabels(
                entry.simulatedRequestMethodLabel(),
                entry.simulatedRequestPathLabel(),
                "deterministic traffic matrix request body label for " + matrixCase.scenarioId());
    }

    private static URI requestUri(
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor,
            LocalLabTrafficMatrixCase matrixCase,
            RequestLabels labels) {
        String query = String.join("&",
                parameter("scenarioId", matrixCase.scenarioId()),
                parameter("backendId", matrixCase.backendId()),
                parameter("requestMethodLabel", labels.methodLabel()),
                parameter("requestPathLabel", labels.pathLabel()),
                parameter("requestBodySummary", labels.bodySummary()),
                parameter("requestPurposeLabel", "test-scope deterministic traffic matrix request label"));
        return URI.create(descriptor.localUrl() + "?" + query);
    }

    private static String parameter(String key, String value) {
        return encode(key) + "=" + encode(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record RequestLabels(String methodLabel, String pathLabel, String bodySummary) {
    }
}
