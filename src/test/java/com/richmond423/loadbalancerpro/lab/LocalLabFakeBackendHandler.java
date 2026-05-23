package com.richmond423.loadbalancerpro.lab;

final class LocalLabFakeBackendHandler {
    private static final String SAFETY_BOUNDARY =
            "Test-scope fake backend handler only; maps simulated request labels to existing response fixtures "
                    + "in memory; does not implement a fake backend server, start listeners, open ports, call "
                    + "loopback endpoints, generate traffic, run Docker, k6, Bruno, Toxiproxy, "
                    + "Prometheus/Grafana, execute replay, write reports, persist storage, export artifacts, "
                    + "or change runtime behavior.";
    private static final String NOT_PROVEN_BOUNDARY =
            "Test-scope handled response only; not production proof; not live-cloud validation; not "
                    + "real-tenant validation; not fake backend server execution; not replay execution; not "
                    + "evidence/report generation; not storage, export, runtime, routing, scoring, strategy, "
                    + "proxy, or API behavior.";

    private LocalLabFakeBackendHandler() {
    }

    static LocalLabFakeBackendHandledResponse handle(LocalLabFakeBackendRequest request) {
        if (request == null) {
            return boundaryResponse("missing-scenario-label", "missing-backend-label");
        }

        return LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                .filter(fixture -> fixture.scenarioId().equals(request.scenarioId()))
                .filter(fixture -> fixture.backendId().equals(request.backendId()))
                .findFirst()
                .map(LocalLabFakeBackendHandler::handledResponse)
                .orElseGet(() -> boundaryResponse(request.scenarioId(), request.backendId()));
    }

    private static LocalLabFakeBackendHandledResponse handledResponse(
            LocalLabFakeBackendResponseFixture fixture) {
        return new LocalLabFakeBackendHandledResponse(
                fixture.scenarioId(),
                fixture.backendId(),
                fixture.responseStatusCode(),
                fixture.responseLatencyLabel(),
                fixture.responseBodySummary(),
                fixture.simulatedErrorLabel(),
                fixture.simulatedLoadLabel(),
                fixture.evidenceNote()
                        + " Handler mapped simulated request labels to the fixture in memory only.",
                SAFETY_BOUNDARY,
                fixture.notProvenBoundary() + " " + NOT_PROVEN_BOUNDARY);
    }

    private static LocalLabFakeBackendHandledResponse boundaryResponse(String scenarioId, String backendId) {
        return new LocalLabFakeBackendHandledResponse(
                stableLabel(scenarioId, "unknown-scenario-label"),
                stableLabel(backendId, "unknown-backend-label"),
                404,
                "boundary response with no fixture latency label",
                "Boundary response for unknown local lab fake backend labels.",
                "unknown scenario or backend label; no fixture matched",
                "load not evaluated because no fixture matched",
                "Evidence should note that no matching test-scope response fixture was found and no traffic was "
                        + "generated.",
                SAFETY_BOUNDARY,
                NOT_PROVEN_BOUNDARY);
    }

    private static String stableLabel(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
