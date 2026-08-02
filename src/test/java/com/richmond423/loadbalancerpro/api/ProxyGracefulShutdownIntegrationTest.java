package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

class ProxyGracefulShutdownIntegrationTest {
    private static final int IN_FLIGHT_REQUESTS = 4;

    @Test
    void contextShutdownLetsInFlightTomcatProxyRequestsCompleteWithinTheGraceWindow() throws Exception {
        CountDownLatch backendStarted = new CountDownLatch(IN_FLIGHT_REQUESTS);
        CountDownLatch releaseBackend = new CountDownLatch(1);
        try (BlockingBackend backend = BlockingBackend.start(backendStarted, releaseBackend)) {
            ConfigurableApplicationContext application = startApplication(backend.baseUrl());
            ExecutorService shutdownExecutor = Executors.newSingleThreadExecutor();
            try {
                assertEquals("graceful", application.getEnvironment().getProperty("server.shutdown"));
                assertEquals("30s", application.getEnvironment()
                        .getProperty("spring.lifecycle.timeout-per-shutdown-phase"));

                int port = ((WebServerApplicationContext) application).getWebServer().getPort();
                HttpClient client = HttpClient.newHttpClient();
                List<java.util.concurrent.CompletableFuture<HttpResponse<String>>> responses = new ArrayList<>();
                for (int index = 0; index < IN_FLIGHT_REQUESTS; index++) {
                    HttpRequest request = HttpRequest.newBuilder(
                                    URI.create("http://127.0.0.1:" + port + "/proxy/api/slow/" + index))
                            .timeout(Duration.ofSeconds(10))
                            .GET()
                            .build();
                    responses.add(client.sendAsync(request, HttpResponse.BodyHandlers.ofString()));
                }
                assertTrue(backendStarted.await(5, TimeUnit.SECONDS),
                        "all proxy requests must reach the loopback backend before shutdown");

                Future<?> shutdown = shutdownExecutor.submit(application::close);
                Thread.sleep(100);
                assertFalse(shutdown.isDone(), "graceful shutdown must wait for in-flight Tomcat requests");
                assertTrue(responses.stream().noneMatch(java.util.concurrent.CompletableFuture::isDone));

                releaseBackend.countDown();
                for (java.util.concurrent.CompletableFuture<HttpResponse<String>> response : responses) {
                    HttpResponse<String> completed = response.get(5, TimeUnit.SECONDS);
                    assertEquals(200, completed.statusCode());
                    assertEquals("completed", completed.body());
                }
                shutdown.get(5, TimeUnit.SECONDS);
                assertFalse(application.isActive());
            } finally {
                releaseBackend.countDown();
                application.close();
                shutdownExecutor.shutdownNow();
            }
        }
    }

    private static ConfigurableApplicationContext startApplication(String backendUrl) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("server.address", "127.0.0.1");
        properties.put("server.port", "0");
        properties.put("spring.main.banner-mode", "off");
        properties.put("loadbalancerpro.auth.mode", "none");
        properties.put("management.endpoints.enabled-by-default", "false");
        properties.put("loadbalancerpro.proxy.enabled", "true");
        properties.put("loadbalancerpro.proxy.routes.api.path-prefix", "/api");
        properties.put("loadbalancerpro.proxy.routes.api.targets[0].id", "slow-backend");
        properties.put("loadbalancerpro.proxy.routes.api.targets[0].url", backendUrl);
        properties.put("loadbalancerpro.proxy.routes.api.targets[0].weight", "1");
        String[] arguments = properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
        return new SpringApplicationBuilder(LoadBalancerApiApplication.class).run(arguments);
    }

    private static final class BlockingBackend implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;

        private BlockingBackend(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
        }

        private static BlockingBackend start(
                CountDownLatch started, CountDownLatch release) {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newFixedThreadPool(IN_FLIGHT_REQUESTS);
                BlockingBackend backend = new BlockingBackend(server, executor);
                server.createContext("/", exchange -> handle(exchange, started, release));
                server.setExecutor(executor);
                server.start();
                return backend;
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }

        private static void handle(
                HttpExchange exchange, CountDownLatch started, CountDownLatch release) throws IOException {
            started.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new IOException("test backend release timed out");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("test backend interrupted", exception);
            }
            byte[] body = "completed".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        }
    }
}
