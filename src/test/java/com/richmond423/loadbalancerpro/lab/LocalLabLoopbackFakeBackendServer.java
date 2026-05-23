package com.richmond423.loadbalancerpro.lab;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

final class LocalLabLoopbackFakeBackendServer implements AutoCloseable {
    static final String ENDPOINT_PATH = "/local-lab/fake-backend";
    static final String SERVER_BOUNDARY =
            "Test-scope loopback fake backend server harness only; binds to 127.0.0.1 with an "
                    + "OS-assigned ephemeral port; exposes the in-memory handler for focused tests; "
                    + "does not add production endpoints, production API behavior, routing, scoring, "
                    + "strategy, proxy, Docker, k6, Bruno, Toxiproxy, Prometheus/Grafana, replay, "
                    + "evidence/report generation, file writing, storage, export, or runtime behavior.";

    private static final int EPHEMERAL_PORT_REQUEST = 0;
    private static final String LOOPBACK_HOST = "127.0.0.1";
    private final HttpServer server;
    private boolean stopped;

    private LocalLabLoopbackFakeBackendServer(HttpServer server) {
        this.server = server;
    }

    static LocalLabLoopbackFakeBackendServer start() throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName(LOOPBACK_HOST), EPHEMERAL_PORT_REQUEST),
                0);
        server.createContext(ENDPOINT_PATH, LocalLabLoopbackFakeBackendServer::handleExchange);
        server.setExecutor(null);
        server.start();
        return new LocalLabLoopbackFakeBackendServer(server);
    }

    String host() {
        return server.getAddress().getAddress().getHostAddress();
    }

    int requestedPort() {
        return EPHEMERAL_PORT_REQUEST;
    }

    int port() {
        return server.getAddress().getPort();
    }

    URI endpointUri() {
        return URI.create("http://" + host() + ":" + port() + ENDPOINT_PATH);
    }

    boolean stopped() {
        return stopped;
    }

    @Override
    public void close() {
        if (!stopped) {
            stopped = true;
            server.stop(0);
        }
    }

    private static void handleExchange(HttpExchange exchange) throws IOException {
        LocalLabFakeBackendHandledResponse response = responseFor(exchange);
        byte[] body = deterministicBody(response).getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(response.statusCode(), body.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
        }
    }

    private static LocalLabFakeBackendHandledResponse responseFor(HttpExchange exchange) {
        if (!ENDPOINT_PATH.equals(exchange.getRequestURI().getPath())) {
            return LocalLabFakeBackendHandler.handle(null);
        }

        Map<String, String> parameters = queryParameters(exchange.getRequestURI());
        LocalLabFakeBackendRequest request = new LocalLabFakeBackendRequest(
                label(parameters, "scenarioId", "missing-scenario-label"),
                label(parameters, "backendId", "missing-backend-label"),
                label(parameters, "requestMethodLabel", exchange.getRequestMethod() + " label"),
                label(parameters, "requestPathLabel", ENDPOINT_PATH + " path label"),
                label(parameters, "requestBodySummary", "loopback test request body label"),
                label(parameters, "requestPurposeLabel", "loopback test fake backend handler request label"));
        return LocalLabFakeBackendHandler.handle(request);
    }

    private static Map<String, String> queryParameters(URI uri) {
        Map<String, String> parameters = new LinkedHashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return parameters;
        }

        for (String pair : query.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int separator = pair.indexOf('=');
            String key = decode(separator >= 0 ? pair.substring(0, separator) : pair);
            String value = decode(separator >= 0 ? pair.substring(separator + 1) : "");
            if (!key.isBlank()) {
                parameters.put(key, value);
            }
        }
        return parameters;
    }

    private static String label(Map<String, String> parameters, String key, String fallback) {
        String value = parameters.get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String deterministicBody(LocalLabFakeBackendHandledResponse response) {
        return String.join("\n",
                "scenarioId=" + clean(response.scenarioId()),
                "backendId=" + clean(response.backendId()),
                "statusCode=" + response.statusCode(),
                "latencyLabel=" + clean(response.latencyLabel()),
                "bodySummary=" + clean(response.bodySummary()),
                "errorLabel=" + clean(response.errorLabel()),
                "loadLabel=" + clean(response.loadLabel()),
                "evidenceNote=" + clean(response.evidenceNote()),
                "safetyBoundary=" + clean(response.safetyBoundary()),
                "notProvenBoundary=" + clean(response.notProvenBoundary()),
                "serverBoundary=" + SERVER_BOUNDARY);
    }

    private static String clean(String value) {
        return value.replace('\r', ' ').replace('\n', ' ');
    }
}
