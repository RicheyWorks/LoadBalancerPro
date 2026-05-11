package com.richmond423.loadbalancerpro.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ProxyDemoFixtureLauncherTest {
    private static final Path LAUNCHER_SOURCE =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/demo/ProxyDemoFixtureLauncher.java");
    private static final Pattern NON_LOOPBACK_URL =
            Pattern.compile("https?://(?!127\\.0\\.0\\.1(?::|/|$)|localhost(?::|/|$))[^\\s\"'`]+");

    @Test
    void helpTextAndHelpRunExposeModesAndSafeRequirements() {
        String help = ProxyDemoFixtureLauncher.helpText();
        assertTrue(help.contains("round-robin"));
        assertTrue(help.contains("weighted-round-robin"));
        assertTrue(help.contains("failover"));
        assertTrue(help.contains("--backend-a-port"));
        assertTrue(help.contains("--backend-b-port"));
        assertTrue(help.contains("does not require Python"));
        assertTrue(help.contains("does not persist demo state"));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int exitCode = ProxyDemoFixtureLauncher.run(
                new String[] {"--help"},
                new PrintStream(output, true, StandardCharsets.UTF_8),
                System.err);

        assertEquals(0, exitCode);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("ProxyDemoFixtureLauncher"));
    }

    @Test
    void modeParsingAcceptsSupportedModesAndRejectsInvalidMode() {
        assertEquals(ProxyDemoFixtureLauncher.DemoMode.ROUND_ROBIN,
                ProxyDemoFixtureLauncher.parse(new String[] {"--mode", "round-robin"}).mode());
        assertEquals(ProxyDemoFixtureLauncher.DemoMode.WEIGHTED_ROUND_ROBIN,
                ProxyDemoFixtureLauncher.parse(new String[] {"--mode=weighted-round-robin"}).mode());
        assertEquals(ProxyDemoFixtureLauncher.DemoMode.FAILOVER,
                ProxyDemoFixtureLauncher.parse(new String[] {"--mode", "failover"}).mode());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ProxyDemoFixtureLauncher.parse(new String[] {"--mode", "random"}));
        assertTrue(exception.getMessage().contains("Invalid mode"));

        ByteArrayOutputStream error = new ByteArrayOutputStream();
        int exitCode = ProxyDemoFixtureLauncher.run(
                new String[] {"--mode", "random"},
                System.out,
                new PrintStream(error, true, StandardCharsets.UTF_8));
        assertEquals(2, exitCode);
        assertTrue(error.toString(StandardCharsets.UTF_8).contains("Valid values"));
    }

    @Test
    void fixtureBackendReturnsBackendIdAndMethodPathQueryBodyEvidence() throws Exception {
        int backendAPort = freePort();
        int backendBPort = freePort();
        ProxyDemoFixtureLauncher.Options options = new ProxyDemoFixtureLauncher.Options(
                ProxyDemoFixtureLauncher.DemoMode.ROUND_ROBIN,
                "127.0.0.1",
                backendAPort,
                backendBPort,
                false);

        try (ProxyDemoFixtureLauncher.DemoFixtureServers servers =
                     ProxyDemoFixtureLauncher.startServers(options)) {
            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> getResponse = client.send(
                    HttpRequest.newBuilder(URI.create(servers.backendAUrl() + "/demo/path?step=1"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertEquals(200, getResponse.statusCode());
            assertEquals("backend-a", getResponse.headers()
                    .firstValue("X-Fixture-Upstream")
                    .orElseThrow());
            assertTrue(getResponse.body().contains("backend-a handled GET /demo/path?step=1"));
            assertTrue(getResponse.body().contains("bodyLength=0"));

            HttpResponse<String> postResponse = client.send(
                    HttpRequest.newBuilder(URI.create(servers.backendBUrl() + "/echo?item=one"))
                            .POST(HttpRequest.BodyPublishers.ofString("demo-body"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertEquals(200, postResponse.statusCode());
            assertEquals("backend-b", postResponse.headers()
                    .firstValue("X-Fixture-Upstream")
                    .orElseThrow());
            assertTrue(postResponse.body().contains("backend-b handled POST /echo?item=one"));
            assertTrue(postResponse.body().contains("bodyLength=9"));
        }
    }

    @Test
    void healthEndpointAndFailoverModeExposeDeterministicUnhealthyFixture() throws Exception {
        int backendAPort = freePort();
        int backendBPort = freePort();
        ProxyDemoFixtureLauncher.Options options = new ProxyDemoFixtureLauncher.Options(
                ProxyDemoFixtureLauncher.DemoMode.FAILOVER,
                "127.0.0.1",
                backendAPort,
                backendBPort,
                false);

        try (ProxyDemoFixtureLauncher.DemoFixtureServers servers =
                     ProxyDemoFixtureLauncher.startServers(options)) {
            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> backendAHealth = get(client, servers.backendAUrl() + "/health");
            HttpResponse<String> backendBHealth = get(client, servers.backendBUrl() + "/health");

            assertEquals(200, backendAHealth.statusCode());
            assertTrue(backendAHealth.body().contains("backend-a health=true"));
            assertEquals(503, backendBHealth.statusCode());
            assertTrue(backendBHealth.body().contains("backend-b health=false"));

            assertEquals(200, get(client, servers.backendBUrl() + "/fixture/health/ok").statusCode());
            HttpResponse<String> recovered = get(client, servers.backendBUrl() + "/health");
            assertEquals(200, recovered.statusCode());
            assertTrue(recovered.body().contains("backend-b health=true"));

            assertEquals(200, get(client, servers.backendBUrl() + "/fixture/health/fail").statusCode());
            HttpResponse<String> failedAgain = get(client, servers.backendBUrl() + "/health");
            assertEquals(503, failedAgain.statusCode());
            assertTrue(failedAgain.body().contains("backend-b health=false"));
        }
    }

    @Test
    void printedInstructionsIncludeProfilesCurlStatusHeadersAndCleanup() throws Exception {
        int backendAPort = freePort();
        int backendBPort = freePort();
        ProxyDemoFixtureLauncher.Options options = new ProxyDemoFixtureLauncher.Options(
                ProxyDemoFixtureLauncher.DemoMode.WEIGHTED_ROUND_ROBIN,
                "127.0.0.1",
                backendAPort,
                backendBPort,
                false);

        try (ProxyDemoFixtureLauncher.DemoFixtureServers servers =
                     ProxyDemoFixtureLauncher.startServers(options)) {
            String instructions = ProxyDemoFixtureLauncher.buildInstructions(options, servers);

            assertTrue(instructions.contains("proxy-demo-round-robin"));
            assertTrue(instructions.contains("proxy-demo-weighted-round-robin"));
            assertTrue(instructions.contains("proxy-demo-failover"));
            assertTrue(instructions.contains("/proxy/**"));
            assertTrue(instructions.contains("/proxy/weighted?step=1"));
            assertTrue(instructions.contains("/proxy-status.html"));
            assertTrue(instructions.contains("/api/proxy/status"));
            assertTrue(instructions.contains("X-LoadBalancerPro-Upstream"));
            assertTrue(instructions.contains("X-LoadBalancerPro-Strategy"));
            assertTrue(instructions.contains("Press Ctrl+C"));
            assertTrue(instructions.toLowerCase(Locale.ROOT).contains("no cloud"));
            assertTrue(instructions.toLowerCase(Locale.ROOT).contains("no production gateway"));
            assertTrue(instructions.toLowerCase(Locale.ROOT).contains("benchmark claim"));
        }
    }

    @Test
    void launcherSourceStaysNoCloudNoExternalUrlAndNoCloudManager() throws Exception {
        String source = Files.readString(LAUNCHER_SOURCE, StandardCharsets.UTF_8);
        String normalized = source.toLowerCase(Locale.ROOT);

        assertFalse(NON_LOOPBACK_URL.matcher(source).find());
        assertFalse(normalized.contains("new cloudmanager"));
        assertFalse(normalized.contains("cloudmanager("));
        assertFalse(normalized.contains("amazonaws"));
        assertFalse(normalized.contains("software.amazon.awssdk"));
        assertFalse(normalized.contains("localstorage"));
        assertFalse(normalized.contains("sessionstorage"));
        assertFalse(normalized.contains("python3"));
        assertFalse(normalized.contains("node "));
        assertTrue(source.contains("does not require Python, Node, Docker"));
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
