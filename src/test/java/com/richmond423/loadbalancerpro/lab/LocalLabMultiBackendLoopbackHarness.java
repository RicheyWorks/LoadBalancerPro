package com.richmond423.loadbalancerpro.lab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class LocalLabMultiBackendLoopbackHarness implements AutoCloseable {
    static final String MULTI_BACKEND_BOUNDARY =
            "Test-scope multi-backend loopback harness only; starts loopback fake backend servers "
                    + "under src/test/java; each server binds to 127.0.0.1 with an OS-assigned ephemeral "
                    + "port; does not add production endpoints, production API behavior, routing, scoring, "
                    + "strategy, proxy, Docker, k6, Bruno, Toxiproxy, Prometheus/Grafana, replay, "
                    + "evidence/report generation, file writing, storage, export, or runtime behavior; "
                    + "not production proof; not live-cloud validation; not real-tenant validation; "
                    + "not production certification.";

    private final List<LocalLabLoopbackFakeBackendServer> servers;
    private final List<Descriptor> descriptors;
    private boolean stopped;

    private LocalLabMultiBackendLoopbackHarness(
            List<LocalLabLoopbackFakeBackendServer> servers,
            List<Descriptor> descriptors) {
        this.servers = servers;
        this.descriptors = descriptors;
    }

    static LocalLabMultiBackendLoopbackHarness start() throws IOException {
        List<LocalLabLoopbackFakeBackendServer> startedServers = new ArrayList<>();
        List<Descriptor> startedDescriptors = new ArrayList<>();

        try {
            for (LocalLabFakeBackendResponseFixture fixture : LocalLabFakeBackendResponseFixtureCatalog.fixtures()) {
                LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start();
                startedServers.add(server);
                startedDescriptors.add(descriptorFor(fixture, server));
            }
            return new LocalLabMultiBackendLoopbackHarness(
                    List.copyOf(startedServers),
                    List.copyOf(startedDescriptors));
        } catch (IOException | RuntimeException ex) {
            for (LocalLabLoopbackFakeBackendServer server : startedServers) {
                server.close();
            }
            throw ex;
        }
    }

    List<Descriptor> descriptors() {
        return descriptors;
    }

    boolean stopped() {
        return stopped;
    }

    @Override
    public void close() {
        if (!stopped) {
            stopped = true;
            for (LocalLabLoopbackFakeBackendServer server : servers) {
                server.close();
            }
        }
    }

    private static Descriptor descriptorFor(
            LocalLabFakeBackendResponseFixture fixture,
            LocalLabLoopbackFakeBackendServer server) {
        return new Descriptor(
                fixture.scenarioId(),
                fixture.backendId(),
                server.host(),
                server.port(),
                fixture.behaviorType(),
                fixture.behaviorProfile().behaviorLabel(),
                server.endpointUri().toString(),
                MULTI_BACKEND_BOUNDARY);
    }

    record Descriptor(
            String scenarioId,
            String backendId,
            String host,
            int assignedPort,
            String behaviorType,
            String behaviorLabel,
            String localUrl,
            String notProductionProofBoundary) {

        Descriptor {
            requireText("scenarioId", scenarioId);
            requireText("backendId", backendId);
            requireText("host", host);
            if (assignedPort <= 0) {
                throw new IllegalArgumentException("assignedPort must be a positive OS-assigned port");
            }
            requireText("behaviorType", behaviorType);
            requireText("behaviorLabel", behaviorLabel);
            requireText("localUrl", localUrl);
            requireText("notProductionProofBoundary", notProductionProofBoundary);
        }

        private static void requireText(String field, String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
        }
    }
}
