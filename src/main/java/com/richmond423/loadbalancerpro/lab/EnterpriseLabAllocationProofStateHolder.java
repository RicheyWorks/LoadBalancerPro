package com.richmond423.loadbalancerpro.lab;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/** Separate-JVM, literal-loopback installed-state holder used only by packaged proof. */
public final class EnterpriseLabAllocationProofStateHolder {
    public static final int MAX_REQUESTS = 128;
    public static final Duration START_TIMEOUT = Duration.ofSeconds(8);
    public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private static final String AUTH_HEADER = "X-Allocation-Proof-Key";
    private static final String INITIAL_FILE = "holder-initial.json";
    private static final String READY_FILE = "holder-ready.txt";
    private static final Pattern RUN_TOKEN = Pattern.compile("[0-9a-f]{64}");
    private static final int MAX_EXCHANGE_BYTES =
            EnterpriseLabInstalledAllocationSnapshotCodec.HARD_MAX_SNAPSHOT_BYTES * 2 + 4;

    private EnterpriseLabAllocationProofStateHolder() {
    }

    public static int runChild(
            Path output,
            String runToken,
            PrintStream out,
            PrintStream err) {
        try {
            Path runRoot = runRoot(output, runToken);
            Path initialFile = controlled(runRoot, INITIAL_FILE);
            Path readyFile = controlled(runRoot, READY_FILE);
            EnterpriseLabInstalledAllocationSnapshotCodec codec =
                    new EnterpriseLabInstalledAllocationSnapshotCodec(fixedTargets());
            EnterpriseLabInstalledAllocationSnapshot initial = codec.decode(
                    Files.readAllBytes(initialFile));
            AtomicReference<EnterpriseLabInstalledAllocationSnapshot> current =
                    new AtomicReference<>(initial);
            AtomicInteger requests = new AtomicInteger();
            CountDownLatch stop = new CountDownLatch(1);
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "allocation-proof-state-holder");
                thread.setDaemon(false);
                return thread;
            });
            server.setExecutor(executor);
            server.createContext("/state", exchange -> handleState(
                    exchange, runToken, codec, current, requests));
            server.createContext("/stop", exchange -> handleStop(
                    exchange, runToken, requests, stop));
            server.start();
            try {
                Files.writeString(
                        readyFile,
                        Integer.toString(server.getAddress().getPort()) + System.lineSeparator(),
                        StandardCharsets.UTF_8);
                out.println("allocation-proof-holder-ready");
                if (!stop.await(Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS)) {
                    err.println("allocation proof holder reached its bounded lifetime");
                    return 2;
                }
                return 0;
            } finally {
                server.stop(0);
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            err.println("allocation proof holder was interrupted");
            return 2;
        } catch (IOException | RuntimeException exception) {
            err.println("allocation proof holder failed safely: " + safeMessage(exception));
            return 2;
        }
    }

    static HolderProcess start(
            Path output,
            String runToken,
            EnterpriseLabInstalledAllocationSnapshot initial,
            EnterpriseLabExperimentTargetCatalog targetCatalog) throws IOException {
        Path runRoot = runRoot(output, runToken);
        Files.createDirectories(runRoot);
        EnterpriseLabInstalledAllocationSnapshotCodec codec =
                new EnterpriseLabInstalledAllocationSnapshotCodec(targetCatalog);
        Files.write(controlled(runRoot, INITIAL_FILE), codec.encode(initial));
        Files.deleteIfExists(controlled(runRoot, READY_FILE));

        List<String> command = javaCommand();
        command.add("--enterprise-lab-allocation-proof-holder");
        command.add("--enterprise-lab-allocation-proof-output="
                + output.toAbsolutePath().normalize());
        command.add("--enterprise-lab-allocation-proof-run=" + runToken);
        Process process = new ProcessBuilder(command)
                .directory(Path.of("").toAbsolutePath().normalize().toFile())
                .redirectErrorStream(true)
                .start();
        Path ready = controlled(runRoot, READY_FILE);
        Instant deadline = Instant.now().plus(START_TIMEOUT);
        try {
            while (Instant.now().isBefore(deadline)) {
                if (Files.isRegularFile(ready)) {
                    int port = Integer.parseInt(Files.readString(
                            ready, StandardCharsets.UTF_8).trim());
                    if (port < 1 || port > 65_535) {
                        throw new IllegalStateException(
                                "proof holder published an invalid loopback port");
                    }
                    return new HolderProcess(
                            process,
                            new ExternalStore(port, runToken, codec));
                }
                if (!process.isAlive()) {
                    throw new IllegalStateException(
                            "proof holder exited before readiness: "
                                    + boundedProcessOutput(process));
                }
                Thread.sleep(20L);
            }
            throw new IllegalStateException("proof holder readiness timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "proof holder startup was interrupted", exception);
        } catch (RuntimeException exception) {
            process.destroyForcibly();
            throw exception;
        }
    }

    static EnterpriseLabExperimentTargetCatalog fixedTargets() {
        return new EnterpriseLabExperimentTargetCatalog(List.of(
                new EnterpriseLabLoopbackTarget(
                        "tail-latency-pressure", "blue",
                        URI.create("http://127.0.0.1:1/allocation-proof")),
                new EnterpriseLabLoopbackTarget(
                        "tail-latency-pressure", "green",
                        URI.create("http://127.0.0.1:1/allocation-proof")),
                new EnterpriseLabLoopbackTarget(
                        "tail-latency-pressure", "orange",
                        URI.create("http://127.0.0.1:1/allocation-proof"))));
    }

    private static void handleState(
            HttpExchange exchange,
            String runToken,
            EnterpriseLabInstalledAllocationSnapshotCodec codec,
            AtomicReference<EnterpriseLabInstalledAllocationSnapshot> current,
            AtomicInteger requests) throws IOException {
        try (exchange) {
            if (!authorized(exchange, runToken) || !withinBound(requests)) {
                send(exchange, 403, new byte[0]);
                return;
            }
            if ("GET".equals(exchange.getRequestMethod())) {
                send(exchange, 200, codec.encode(current.get()));
                return;
            }
            if (!"PUT".equals(exchange.getRequestMethod())) {
                send(exchange, 405, new byte[0]);
                return;
            }
            byte[] body = readBounded(exchange.getRequestBody(), MAX_EXCHANGE_BYTES);
            if (body.length < 5) {
                send(exchange, 400, new byte[0]);
                return;
            }
            int expectedLength = ByteBuffer.wrap(body, 0, 4).getInt();
            if (expectedLength < 1
                    || expectedLength > EnterpriseLabInstalledAllocationSnapshotCodec.HARD_MAX_SNAPSHOT_BYTES
                    || body.length - 4 - expectedLength < 1) {
                send(exchange, 400, new byte[0]);
                return;
            }
            byte[] expectedBytes = java.util.Arrays.copyOfRange(
                    body, 4, 4 + expectedLength);
            byte[] updateBytes = java.util.Arrays.copyOfRange(
                    body, 4 + expectedLength, body.length);
            EnterpriseLabInstalledAllocationSnapshot expected = codec.decode(expectedBytes);
            EnterpriseLabInstalledAllocationSnapshot update = codec.decode(updateBytes);
            boolean replaced = current.get().equals(expected);
            if (replaced) {
                current.set(update);
            }
            send(exchange, replaced ? 204 : 409, new byte[0]);
        } catch (RuntimeException exception) {
            sendSafely(exchange, 400);
        }
    }

    private static void handleStop(
            HttpExchange exchange,
            String runToken,
            AtomicInteger requests,
            CountDownLatch stop) throws IOException {
        try (exchange) {
            if (!authorized(exchange, runToken) || !withinBound(requests)) {
                send(exchange, 403, new byte[0]);
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, new byte[0]);
                return;
            }
            send(exchange, 204, new byte[0]);
            stop.countDown();
        }
    }

    private static boolean authorized(HttpExchange exchange, String runToken) {
        return runToken.equals(exchange.getRequestHeaders().getFirst(AUTH_HEADER))
                && exchange.getRemoteAddress().getAddress().isLoopbackAddress()
                && exchange.getLocalAddress().getAddress().isLoopbackAddress();
    }

    private static boolean withinBound(AtomicInteger requests) {
        return requests.incrementAndGet() <= MAX_REQUESTS;
    }

    private static byte[] readBounded(InputStream input, int maximum) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4_096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + read > maximum) {
                throw new IOException("proof holder request exceeded its hard byte bound");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static void send(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            exchange.getResponseBody().write(body);
        }
    }

    private static void sendSafely(HttpExchange exchange, int status) {
        try {
            send(exchange, status, new byte[0]);
        } catch (IOException ignored) {
            // The bounded client may already have disconnected.
        }
    }

    private static Path runRoot(Path output, String runToken) {
        Path safeOutput = Objects.requireNonNull(output, "output cannot be null")
                .toAbsolutePath().normalize();
        if (runToken == null || !RUN_TOKEN.matcher(runToken).matches()) {
            throw new IllegalArgumentException(
                    "allocation proof run token must be canonical SHA-256 text");
        }
        Path root = safeOutput.resolve("allocation-proof-runs")
                .resolve(runToken).toAbsolutePath().normalize();
        if (!root.startsWith(safeOutput)) {
            throw new IllegalArgumentException(
                    "allocation proof run root escaped the output boundary");
        }
        return root;
    }

    private static Path controlled(Path root, String name) {
        Path path = root.resolve(name).toAbsolutePath().normalize();
        if (!path.startsWith(root.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException(
                    "allocation proof holder path escaped its run root");
        }
        return path;
    }

    private static List<String> javaCommand() {
        List<String> command = new ArrayList<>();
        Path java = Path.of(System.getProperty("java.home"), "bin",
                isWindows() ? "java.exe" : "java");
        command.add(java.toString());
        String classPath = System.getProperty("java.class.path");
        boolean singleEntry = !classPath.contains(System.getProperty("path.separator"));
        Path possibleJar = singleEntry ? Path.of(classPath) : null;
        if (singleEntry && classPath.endsWith(".jar")
                && Files.isRegularFile(Objects.requireNonNull(possibleJar))) {
            command.add("-jar");
            command.add(possibleJar.toString());
        } else {
            command.add("-cp");
            command.add(classPath);
            command.add("com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication");
        }
        return command;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT)
                .contains("win");
    }

    private static String boundedProcessOutput(Process process) {
        try {
            String value = new String(
                    process.getInputStream().readNBytes(2_048), StandardCharsets.UTF_8)
                    .replace('\r', ' ').replace('\n', ' ').trim();
            return value.length() <= 512 ? value : value.substring(0, 512);
        } catch (IOException exception) {
            return "output unavailable";
        }
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message.replace('\r', ' ').replace('\n', ' ');
    }

    static final class ExternalStore
            implements EnterpriseLabLoopbackAllocationRouter.InstalledStateStore {
        private final URI stateUri;
        private final URI stopUri;
        private final String runToken;
        private final EnterpriseLabInstalledAllocationSnapshotCodec codec;
        private final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();

        private ExternalStore(
                int port,
                String runToken,
                EnterpriseLabInstalledAllocationSnapshotCodec codec) {
            this.stateUri = URI.create("http://127.0.0.1:" + port + "/state");
            this.stopUri = URI.create("http://127.0.0.1:" + port + "/stop");
            this.runToken = runToken;
            this.codec = codec;
        }

        @Override
        public EnterpriseLabInstalledAllocationSnapshot read() {
            HttpRequest request = request(stateUri).GET().build();
            HttpResponse<byte[]> response = send(request);
            if (response.statusCode() != 200
                    || response.body().length
                    > EnterpriseLabInstalledAllocationSnapshotCodec.HARD_MAX_SNAPSHOT_BYTES) {
                throw new IllegalStateException(
                        "proof holder installed-state read failed closed");
            }
            return codec.decode(response.body());
        }

        @Override
        public boolean compareAndSet(
                EnterpriseLabInstalledAllocationSnapshot expected,
                EnterpriseLabInstalledAllocationSnapshot update) {
            byte[] expectedBytes = codec.encode(expected);
            byte[] updateBytes = codec.encode(update);
            ByteBuffer payload = ByteBuffer.allocate(
                    4 + expectedBytes.length + updateBytes.length);
            payload.putInt(expectedBytes.length).put(expectedBytes).put(updateBytes);
            HttpRequest request = request(stateUri)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(payload.array()))
                    .build();
            int status = send(request).statusCode();
            if (status == 204) {
                return true;
            }
            if (status == 409) {
                return false;
            }
            throw new IllegalStateException(
                    "proof holder installed-state replacement failed closed");
        }

        private void stop() {
            HttpRequest request = request(stopUri)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            send(request);
        }

        private HttpRequest.Builder request(URI uri) {
            return HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header(AUTH_HEADER, runToken);
        }

        private HttpResponse<byte[]> send(HttpRequest request) {
            try {
                return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "proof holder loopback request failed", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "proof holder loopback request was interrupted", exception);
            }
        }
    }

    static final class HolderProcess implements AutoCloseable {
        private final Process process;
        private final ExternalStore store;

        private HolderProcess(Process process, ExternalStore store) {
            this.process = process;
            this.store = store;
        }

        ExternalStore store() {
            return store;
        }

        boolean alive() {
            return process.isAlive();
        }

        @Override
        public void close() {
            if (!process.isAlive()) {
                return;
            }
            try {
                store.stop();
                if (!process.waitFor(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                }
            } catch (RuntimeException exception) {
                process.destroyForcibly();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }
}
