package com.richmond423.loadbalancerpro.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts two loopback-only HTTP fixture backends for local reverse proxy demos.
 */
public final class ProxyDemoFixtureLauncher {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_BACKEND_A_PORT = 18081;
    private static final int DEFAULT_BACKEND_B_PORT = 18082;

    private ProxyDemoFixtureLauncher() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        Objects.requireNonNull(out, "out cannot be null");
        Objects.requireNonNull(err, "err cannot be null");

        Options options;
        try {
            options = parse(args);
        } catch (IllegalArgumentException exception) {
            err.println(exception.getMessage());
            err.println();
            err.println(helpText());
            return 2;
        }

        if (options.help()) {
            out.println(helpText());
            return 0;
        }

        CountDownLatch stopSignal = new CountDownLatch(1);
        try (DemoFixtureServers servers = startServers(options)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                servers.close();
                stopSignal.countDown();
            }, "proxy-demo-fixture-shutdown"));
            out.println(buildInstructions(options, servers));
            stopSignal.await();
            return 0;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return 130;
        } catch (IOException | RuntimeException exception) {
            err.println("Proxy demo fixture launcher failed safely: " + safeMessage(exception));
            return 1;
        }
    }

    static Options parse(String[] args) {
        DemoMode mode = DemoMode.ROUND_ROBIN;
        String host = DEFAULT_HOST;
        int backendAPort = DEFAULT_BACKEND_A_PORT;
        int backendBPort = DEFAULT_BACKEND_B_PORT;
        boolean help = false;

        List<String> values = args == null ? List.of() : List.of(args);
        for (int index = 0; index < values.size(); index++) {
            String arg = values.get(index);
            if ("--help".equals(arg) || "-h".equals(arg)) {
                help = true;
            } else if (arg.startsWith("--mode=")) {
                mode = DemoMode.from(arg.substring("--mode=".length()));
            } else if ("--mode".equals(arg)) {
                mode = DemoMode.from(requireValue(values, ++index, "--mode"));
            } else if (arg.startsWith("--backend-a-port=")) {
                backendAPort = parsePort(arg.substring("--backend-a-port=".length()), "--backend-a-port");
            } else if ("--backend-a-port".equals(arg)) {
                backendAPort = parsePort(requireValue(values, ++index, "--backend-a-port"), "--backend-a-port");
            } else if (arg.startsWith("--backend-b-port=")) {
                backendBPort = parsePort(arg.substring("--backend-b-port=".length()), "--backend-b-port");
            } else if ("--backend-b-port".equals(arg)) {
                backendBPort = parsePort(requireValue(values, ++index, "--backend-b-port"), "--backend-b-port");
            } else if (arg.startsWith("--host=")) {
                host = normalizeHost(arg.substring("--host=".length()));
            } else if ("--host".equals(arg)) {
                host = normalizeHost(requireValue(values, ++index, "--host"));
            } else {
                throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }

        if (backendAPort == backendBPort) {
            throw new IllegalArgumentException("backend-a and backend-b ports must be different");
        }

        return new Options(mode, host, backendAPort, backendBPort, help);
    }

    static DemoFixtureServers startServers(Options options) throws IOException {
        Objects.requireNonNull(options, "options cannot be null");
        FixtureBackend backendA = FixtureBackend.start(
                "backend-a", options.host(), options.backendAPort(), true);
        try {
            boolean backendBHealthy = options.mode() != DemoMode.FAILOVER;
            FixtureBackend backendB = FixtureBackend.start(
                    "backend-b", options.host(), options.backendBPort(), backendBHealthy);
            return new DemoFixtureServers(backendA, backendB);
        } catch (IOException | RuntimeException exception) {
            backendA.close();
            throw exception;
        }
    }

    static String helpText() {
        return String.join(System.lineSeparator(),
                "LoadBalancerPro proxy demo fixture launcher",
                "",
                "Usage:",
                "  java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode round-robin",
                "  java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode weighted-round-robin",
                "  java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode failover",
                "",
                "Options:",
                "  --mode MODE              round-robin, weighted-round-robin, or failover",
                "  --backend-a-port PORT    backend-a loopback port, default 18081",
                "  --backend-b-port PORT    backend-b loopback port, default 18082",
                "  --host HOST              127.0.0.1 or localhost, default 127.0.0.1",
                "  --help                   print this help",
                "",
                "The launcher starts loopback fixture servers only. It does not contact cloud services,",
                "does not require Python, Node, Docker, or public internet, and does not persist demo state.");
    }

    static String buildInstructions(Options options, DemoFixtureServers servers) {
        Objects.requireNonNull(options, "options cannot be null");
        Objects.requireNonNull(servers, "servers cannot be null");

        String proxyArguments = proxyArguments(options, servers);
        List<String> lines = new ArrayList<>();
        lines.add("LoadBalancerPro proxy demo fixture launcher");
        lines.add("Mode: " + options.mode().cliName());
        lines.add("Safety: loopback-only, no cloud, no external network, no persistence.");
        lines.add("");
        lines.add("Started fixture backends:");
        lines.add("  backend-a " + servers.backendAUrl());
        lines.add("  backend-b " + servers.backendBUrl());
        lines.add("Health endpoints:");
        lines.add("  " + servers.backendAUrl() + "/health");
        lines.add("  " + servers.backendBUrl() + "/health");
        if (options.mode() == DemoMode.FAILOVER) {
            lines.add("Failover mode starts backend-b health as failing; use /fixture/health/ok to restore it.");
        }
        lines.add("");
        lines.add("Checked-in Spring profile: " + options.mode().profileName());
        lines.add("Available demo profiles: proxy-demo-round-robin, proxy-demo-weighted-round-robin, proxy-demo-failover");
        lines.add("Start LoadBalancerPro in a second terminal:");
        lines.add("  mvn spring-boot:run \"-Dspring-boot.run.arguments=" + proxyArguments + "\"");
        lines.add("");
        lines.add("Proxy status:");
        lines.add("  Browser status page: http://localhost:8080/proxy-status.html");
        lines.add("  curl -s http://127.0.0.1:8080/api/proxy/status");
        lines.add("");
        lines.add("Expected proxy evidence headers:");
        lines.add("  X-LoadBalancerPro-Upstream");
        lines.add("  X-LoadBalancerPro-Strategy");
        lines.add("");
        lines.addAll(curlRecipes(options, servers));
        lines.add("");
        lines.add("Fixture controls:");
        lines.add("  curl " + servers.backendBUrl() + "/fixture/health/fail");
        lines.add("  curl " + servers.backendBUrl() + "/fixture/health/ok");
        lines.add("");
        lines.add("Cleanup:");
        lines.add("  Press Ctrl+C to stop fixture backends.");
        lines.add("");
        lines.add("Limitations: local loopback demo only; no production gateway or benchmark claim.");
        return String.join(System.lineSeparator(), lines);
    }

    private static List<String> curlRecipes(Options options, DemoFixtureServers servers) {
        List<String> lines = new ArrayList<>();
        lines.add("Proxy curl recipes:");
        if (options.mode() == DemoMode.WEIGHTED_ROUND_ROBIN) {
            lines.add("  # Expected first four selected upstreams with weights 3:1: backend-a, backend-a, backend-b, backend-a");
            lines.add("  curl -i http://127.0.0.1:8080/proxy/weighted?step=1");
            lines.add("  curl -i http://127.0.0.1:8080/proxy/weighted?step=2");
            lines.add("  curl -i http://127.0.0.1:8080/proxy/weighted?step=3");
            lines.add("  curl -i http://127.0.0.1:8080/proxy/weighted?step=4");
        } else if (options.mode() == DemoMode.FAILOVER) {
            lines.add("  curl -i http://127.0.0.1:8080/proxy/failover?step=1");
            lines.add("  curl -s http://127.0.0.1:8080/api/proxy/status");
            lines.add("  curl " + servers.backendBUrl() + "/fixture/health/ok");
            lines.add("  curl -i http://127.0.0.1:8080/proxy/failover?step=2");
        } else {
            lines.add("  # Expected first four selected upstreams: backend-a, backend-b, backend-a, backend-b");
            lines.add("  curl -i http://127.0.0.1:8080/proxy/demo?step=1");
            lines.add("  curl -i http://127.0.0.1:8080/proxy/demo?step=2");
            lines.add("  curl -i http://127.0.0.1:8080/proxy/demo?step=3");
            lines.add("  curl -i http://127.0.0.1:8080/proxy/demo?step=4");
        }
        lines.add("  curl -X POST -d \"demo-body\" http://127.0.0.1:8080/proxy/body-demo?step=post");
        lines.add("  curl -s http://127.0.0.1:8080/api/proxy/status");
        lines.add("  # The proxy strips /proxy/** before forwarding to the fixture.");
        return lines;
    }

    private static String proxyArguments(Options options, DemoFixtureServers servers) {
        List<String> arguments = new ArrayList<>();
        arguments.add("--spring.profiles.active=" + options.mode().profileName());
        if (!servers.backendAUrl().equals("http://127.0.0.1:" + DEFAULT_BACKEND_A_PORT)) {
            arguments.add("--loadbalancerpro.proxy.upstreams[0].url=" + servers.backendAUrl());
        }
        if (!servers.backendBUrl().equals("http://127.0.0.1:" + DEFAULT_BACKEND_B_PORT)) {
            arguments.add("--loadbalancerpro.proxy.upstreams[1].url=" + servers.backendBUrl());
        }
        return String.join(" ", arguments);
    }

    private static String requireValue(List<String> args, int index, String option) {
        if (index >= args.size() || args.get(index).startsWith("--")) {
            throw new IllegalArgumentException(option + " requires a value");
        }
        return args.get(index);
    }

    private static int parsePort(String value, String option) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException(option + " must be between 1 and 65535");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(option + " must be a numeric port", exception);
        }
    }

    private static String normalizeHost(String host) {
        String normalized = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
        if (!"127.0.0.1".equals(normalized) && !"localhost".equals(normalized)) {
            throw new IllegalArgumentException("--host must be 127.0.0.1 or localhost");
        }
        return normalized;
    }

    private static String safeMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    public enum DemoMode {
        ROUND_ROBIN("round-robin", "proxy-demo-round-robin"),
        WEIGHTED_ROUND_ROBIN("weighted-round-robin", "proxy-demo-weighted-round-robin"),
        FAILOVER("failover", "proxy-demo-failover");

        private final String cliName;
        private final String profileName;

        DemoMode(String cliName, String profileName) {
            this.cliName = cliName;
            this.profileName = profileName;
        }

        public String cliName() {
            return cliName;
        }

        public String profileName() {
            return profileName;
        }

        static DemoMode from(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            for (DemoMode mode : values()) {
                if (mode.cliName.equals(normalized)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Invalid mode: " + value
                    + ". Valid values: round-robin, weighted-round-robin, failover");
        }
    }

    public record Options(DemoMode mode, String host, int backendAPort, int backendBPort, boolean help) {
        public Options {
            Objects.requireNonNull(mode, "mode cannot be null");
            host = normalizeHost(host);
            parsePort(Integer.toString(backendAPort), "--backend-a-port");
            parsePort(Integer.toString(backendBPort), "--backend-b-port");
            if (backendAPort == backendBPort) {
                throw new IllegalArgumentException("backend-a and backend-b ports must be different");
            }
        }
    }

    public static final class DemoFixtureServers implements AutoCloseable {
        private final FixtureBackend backendA;
        private final FixtureBackend backendB;

        private DemoFixtureServers(FixtureBackend backendA, FixtureBackend backendB) {
            this.backendA = backendA;
            this.backendB = backendB;
        }

        public String backendAUrl() {
            return backendA.baseUrl();
        }

        public String backendBUrl() {
            return backendB.baseUrl();
        }

        @Override
        public void close() {
            backendA.close();
            backendB.close();
        }
    }

    private static final class FixtureBackend implements AutoCloseable {
        private final String id;
        private final String host;
        private final HttpServer server;
        private final ExecutorService executor;
        private final AtomicBoolean healthy;

        private FixtureBackend(String id, String host, HttpServer server,
                               ExecutorService executor, boolean initiallyHealthy) {
            this.id = id;
            this.host = host;
            this.server = server;
            this.executor = executor;
            this.healthy = new AtomicBoolean(initiallyHealthy);
        }

        private static FixtureBackend start(String id, String host, int port, boolean initiallyHealthy)
                throws IOException {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);
            HttpServer server = HttpServer.create(address, 0);
            ExecutorService executor = Executors.newCachedThreadPool();
            FixtureBackend backend = new FixtureBackend(id, host, server, executor, initiallyHealthy);
            server.createContext("/", backend::handle);
            server.setExecutor(executor);
            server.start();
            return backend;
        }

        private String baseUrl() {
            return "http://" + host + ":" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }

        private void handle(HttpExchange exchange) throws IOException {
            try {
                URI uri = exchange.getRequestURI();
                String path = uri.getPath();
                if ("/fixture/health/fail".equals(path)) {
                    healthy.set(false);
                    respond(exchange, 200, id + " fixture health set to failing");
                } else if ("/fixture/health/ok".equals(path)) {
                    healthy.set(true);
                    respond(exchange, 200, id + " fixture health set to healthy");
                } else if ("/health".equals(path)) {
                    boolean currentHealth = healthy.get();
                    respond(exchange, currentHealth ? 200 : 503, id + " health=" + currentHealth);
                } else {
                    byte[] requestBody = exchange.getRequestBody().readAllBytes();
                    String body = id
                            + " handled " + exchange.getRequestMethod()
                            + " " + uri
                            + " bodyLength=" + requestBody.length;
                    respond(exchange, 200, body);
                }
            } finally {
                exchange.close();
            }
        }

        private void respond(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.getResponseHeaders().set("X-Fixture-Upstream", id);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }
}
