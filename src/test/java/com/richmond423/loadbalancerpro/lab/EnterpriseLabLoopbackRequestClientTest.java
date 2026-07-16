package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.ServerObservationOutcome;
import com.richmond423.loadbalancerpro.core.ServerObservationSource;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackObservationIngress.ReceiptStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabLoopbackRequestClientTest {

    @Test
    void actualLoopbackResponseIsMeasuredAndRecorded() throws Exception {
        try (LoopbackServer server = LoopbackServer.start()) {
            EnterpriseLabLoopbackObservationIngress ingress =
                    new EnterpriseLabLoopbackObservationIngress(Set.of("backend-a"));
            EnterpriseLabLoopbackRequestClient client = client(server.uri("/ok"), ingress);

            var execution = client.get("request-1", "backend-a", Duration.ofSeconds(1));

            assertTrue(execution.requestSent());
            assertEquals(ServerObservationOutcome.SUCCESS, execution.outcome().orElseThrow());
            assertEquals(204, execution.responseStatusCode().orElseThrow());
            assertEquals(ReceiptStatus.RECORDED, execution.observationReceipt().status());
            assertEquals(ServerObservationSource.ENTERPRISE_LAB_LOOPBACK,
                    execution.observationReceipt().observation().orElseThrow().source());
            assertTrue(execution.observationReceipt().observation().orElseThrow().latencyMillis().isPresent());
            assertEquals(1, server.okRequests.get());
        }
    }

    @Test
    void redirectIsClassifiedAsHttpFailureAndNeverFollowed() throws Exception {
        try (LoopbackServer server = LoopbackServer.start()) {
            EnterpriseLabLoopbackObservationIngress ingress =
                    new EnterpriseLabLoopbackObservationIngress(Set.of("backend-a"));
            EnterpriseLabLoopbackRequestClient client = client(server.uri("/redirect"), ingress);

            var execution = client.get("request-redirect", "backend-a", Duration.ofSeconds(1));

            assertEquals(ServerObservationOutcome.FAILURE, execution.outcome().orElseThrow());
            assertEquals(302, execution.responseStatusCode().orElseThrow());
            assertEquals(0, server.redirectTargets.get(), "the client must never follow a redirect");
        }
    }

    @Test
    void actualHttpFailureRetainsMeasuredLatency() throws Exception {
        try (LoopbackServer server = LoopbackServer.start()) {
            EnterpriseLabLoopbackObservationIngress ingress =
                    new EnterpriseLabLoopbackObservationIngress(Set.of("backend-a"));
            EnterpriseLabLoopbackRequestClient client = client(server.uri("/failure"), ingress);

            var execution = client.get("request-failure", "backend-a", Duration.ofSeconds(1));

            assertEquals(ServerObservationOutcome.FAILURE, execution.outcome().orElseThrow());
            assertEquals(503, execution.responseStatusCode().orElseThrow());
            assertTrue(execution.observationReceipt().observation().orElseThrow().latencyMillis().isPresent());
        }
    }

    @Test
    void timeoutAndConnectionFailuresAreClassifiedWithoutThrowing() {
        EnterpriseLabLoopbackObservationIngress timeoutIngress =
                new EnterpriseLabLoopbackObservationIngress(Set.of("backend-a"));
        EnterpriseLabLoopbackRequestClient timeoutClient = client(
                URI.create("http://127.0.0.1:49151/local-lab/backend"),
                timeoutIngress,
                (uri, timeout) -> {
                    throw new HttpTimeoutException("bounded timeout");
                });
        var timeout = timeoutClient.get("request-timeout", "backend-a", Duration.ofMillis(20));
        assertEquals(ServerObservationOutcome.TIMEOUT, timeout.outcome().orElseThrow());
        assertEquals(ReceiptStatus.RECORDED, timeout.observationReceipt().status());

        EnterpriseLabLoopbackObservationIngress connectionIngress =
                new EnterpriseLabLoopbackObservationIngress(Set.of("backend-a"));
        EnterpriseLabLoopbackRequestClient connectionClient = client(
                URI.create("http://127.0.0.1:49152/local-lab/backend"),
                connectionIngress,
                (uri, requestTimeout) -> {
                    throw new ConnectException("connection refused");
                });
        var connection = connectionClient.get("request-connection", "backend-a", Duration.ofMillis(20));
        assertEquals(ServerObservationOutcome.CONNECTION_FAILURE, connection.outcome().orElseThrow());
        assertEquals(ReceiptStatus.RECORDED, connection.observationReceipt().status());
    }

    @Test
    void unknownBackendAndInvalidTimeoutNeverInvokeTransport() {
        AtomicInteger calls = new AtomicInteger();
        EnterpriseLabLoopbackObservationIngress ingress =
                new EnterpriseLabLoopbackObservationIngress(Set.of("backend-a"));
        EnterpriseLabLoopbackRequestClient client = client(
                URI.create("http://127.0.0.1:49153/local-lab/backend"),
                ingress,
                (uri, timeout) -> {
                    calls.incrementAndGet();
                    return 200;
                });

        assertFalse(client.get("request-unknown", "backend-b", Duration.ofSeconds(1)).requestSent());
        assertFalse(client.get("request-timeout", "backend-a", Duration.ofSeconds(6)).requestSent());
        assertEquals(0, calls.get());
        assertTrue(ingress.observations("backend-a").isEmpty());
    }

    @Test
    void targetRequiresLiteralLoopbackHttpWithoutAmbientUrlParts() {
        assertThrows(IllegalArgumentException.class, () -> target("http://localhost:49154/local-lab/backend"));
        assertThrows(IllegalArgumentException.class, () -> target("https://127.0.0.1:49154/local-lab/backend"));
        assertThrows(IllegalArgumentException.class, () -> target("http://127.0.0.1/local-lab/backend"));
        assertThrows(IllegalArgumentException.class,
                () -> target("http://127.0.0.1:49154/local-lab/backend?target=other"));
        String nonLoopback = "http://" + "192.0.2." + "10:49154/local-lab/backend";
        assertThrows(IllegalArgumentException.class, () -> target(nonLoopback));
    }

    private static EnterpriseLabLoopbackRequestClient client(
            URI uri,
            EnterpriseLabLoopbackObservationIngress ingress) {
        return new EnterpriseLabLoopbackRequestClient(List.of(target(uri.toString())), ingress);
    }

    private static EnterpriseLabLoopbackRequestClient client(
            URI uri,
            EnterpriseLabLoopbackObservationIngress ingress,
            EnterpriseLabLoopbackRequestClient.Transport transport) {
        return new EnterpriseLabLoopbackRequestClient(
                List.of(target(uri.toString())), ingress, transport, Duration.ofSeconds(5));
    }

    private static EnterpriseLabLoopbackTarget target(String uri) {
        return new EnterpriseLabLoopbackTarget("normal-balanced-load", "backend-a", URI.create(uri));
    }

    private static final class LoopbackServer implements AutoCloseable {
        private final HttpServer server;
        private final AtomicInteger okRequests = new AtomicInteger();
        private final AtomicInteger redirectTargets = new AtomicInteger();

        private LoopbackServer(HttpServer server) {
            this.server = server;
        }

        static LoopbackServer start() throws IOException {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0),
                    0);
            LoopbackServer wrapper = new LoopbackServer(server);
            server.createContext("/ok", exchange -> {
                wrapper.okRequests.incrementAndGet();
                respond(exchange, 204);
            });
            server.createContext("/failure", exchange -> respond(exchange, 503));
            server.createContext("/redirect", exchange -> {
                exchange.getResponseHeaders().set("Location", wrapper.uri("/redirect-target").toString());
                respond(exchange, 302);
            });
            server.createContext("/redirect-target", exchange -> {
                wrapper.redirectTargets.incrementAndGet();
                respond(exchange, 200);
            });
            server.setExecutor(null);
            server.start();
            return wrapper;
        }

        URI uri(String path) {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
        }

        @Override
        public void close() {
            server.stop(0);
        }

        private static void respond(HttpExchange exchange, int statusCode) throws IOException {
            exchange.sendResponseHeaders(statusCode, -1);
            exchange.close();
        }
    }
}
