package com.richmond423.loadbalancerpro.api.proxy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Owns the optional proxy access-log sink. Request threads only build a bounded
 * record and offer it to a bounded queue; formatting and file I/O stay on the
 * daemon writer thread.
 */
public final class ReverseProxyAccessLog implements SmartLifecycle {
    static final String THREAD_NAME = "loadbalancerpro-proxy-access-log";
    static final String REDACTED = "-";
    static final int DEFAULT_QUEUE_CAPACITY = 16_384;

    private static final Logger logger = LoggerFactory.getLogger(ReverseProxyAccessLog.class);
    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(5);
    private static final Set<String> METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final String UNMATCHED = "UNMATCHED";
    private static final String NONE = "NONE";
    private static final String OTHER = "OTHER";

    private final Configuration configuration;
    private final Clock clock;
    private final BoundedMpscQueue<RequestLogObservation> queue;
    private final EventWriterFactory writerFactory;
    private final AtomicBoolean accepting = new AtomicBoolean();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicLong sequence = new AtomicLong();
    private final LongAdder dropped = new LongAdder();
    private final LongAdder writeFailures = new LongAdder();
    private final AtomicInteger warnedFailureKinds = new AtomicInteger();
    private volatile Thread writerThread;

    public ReverseProxyAccessLog(ReverseProxyProperties properties) {
        this(properties.getAccessLog(), Clock.systemUTC(), DEFAULT_QUEUE_CAPACITY, FileEventWriter::new);
    }

    ReverseProxyAccessLog(
            ReverseProxyProperties.AccessLog properties,
            Clock clock,
            int queueCapacity,
            EventWriterFactory writerFactory) {
        this.configuration = Configuration.from(properties);
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity must be greater than zero");
        }
        this.queue = new BoundedMpscQueue<>(queueCapacity);
        this.writerFactory = Objects.requireNonNull(writerFactory, "writerFactory cannot be null");
    }

    static ReverseProxyAccessLog disabled() {
        return new ReverseProxyAccessLog(
                new ReverseProxyProperties.AccessLog(),
                Clock.systemUTC(),
                1,
                ignored -> NoOpEventWriter.INSTANCE);
    }

    static void validateConfiguration(ReverseProxyProperties.AccessLog properties) {
        Configuration.from(properties);
    }

    static boolean sameConfiguration(
            ReverseProxyProperties.AccessLog left,
            ReverseProxyProperties.AccessLog right) {
        return Configuration.from(left).equals(Configuration.from(right));
    }

    RequestLogObservation begin(String method) {
        try {
            if (!accepting.get() || !selected()) {
                return null;
            }
            return new RequestLogObservation(
                    clock.millis(), System.nanoTime(), normalizedMethod(method));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @Override
    public synchronized void start() {
        if (!configuration.enabled() || running.get()) {
            return;
        }
        running.set(true);
        accepting.set(true);
        Thread thread = new Thread(this::writeLoop, THREAD_NAME);
        thread.setDaemon(true);
        writerThread = thread;
        thread.start();
    }

    @Override
    public synchronized void stop() {
        accepting.set(false);
        running.set(false);
        LockSupport.unpark(writerThread);
        Thread thread = writerThread;
        if (thread == null || thread == Thread.currentThread()) {
            return;
        }
        try {
            thread.join(STOP_TIMEOUT.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        if (thread.isAlive()) {
            thread.interrupt();
            int abandoned = discardQueued();
            dropped.add(abandoned);
        }
        writerThread = null;
    }

    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        // Start before the proxy and stop after it so terminal records can drain.
        return Integer.MIN_VALUE + 100;
    }

    long acceptedCount() {
        return queue.offeredCount();
    }

    long droppedCount() {
        return dropped.sum();
    }

    long writeFailureCount() {
        return writeFailures.sum();
    }

    private void writeLoop() {
        try {
            EventWriter openedWriter;
            try {
                openedWriter = writerFactory.open(configuration.path());
            } catch (IOException | RuntimeException exception) {
                writeFailures.increment();
                accepting.set(false);
                int abandoned = discardQueued();
                dropped.add(abandoned);
                warnFixedFailure(FailureKind.OPEN);
                return;
            }
            try (EventWriter writer = openedWriter) {
                while (running.get() || !queue.isEmpty()) {
                    RequestLogObservation event = queue.poll();
                    if (event == null) {
                        if (queue.isEmpty()) {
                            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
                        } else {
                            Thread.onSpinWait();
                        }
                        continue;
                    }
                    try {
                        writer.append(format(event));
                        if (queue.isEmpty()) {
                            writer.flush();
                        }
                    } catch (IOException | RuntimeException exception) {
                        writeFailures.increment();
                        warnFixedFailure(FailureKind.WRITE);
                    }
                }
                try {
                    writer.flush();
                } catch (IOException | RuntimeException exception) {
                    writeFailures.increment();
                    warnFixedFailure(FailureKind.FLUSH);
                }
            } catch (IOException | RuntimeException exception) {
                writeFailures.increment();
                warnFixedFailure(FailureKind.CLOSE);
            }
        } finally {
            accepting.set(false);
            running.set(false);
        }
    }

    private void enqueue(RequestLogObservation event) {
        try {
            if (!accepting.get() || !queue.offer(event)) {
                dropped.increment();
                return;
            }
            if (queue.size() == 1) {
                LockSupport.unpark(writerThread);
            }
        } catch (RuntimeException exception) {
            dropped.increment();
        }
    }

    private int discardQueued() {
        int discarded = 0;
        while (queue.poll() != null) {
            discarded++;
        }
        return discarded;
    }

    private String format(RequestLogObservation event) {
        String timestamp = Instant.ofEpochMilli(event.timestampMillis()).toString();
        if (configuration.format() == Format.COMBINED) {
            return "event=proxy.access"
                    + " timestamp=" + timestamp
                    + " client=" + REDACTED
                    + " method=" + event.method()
                    + " path=" + REDACTED
                    + " route=" + event.route()
                    + " upstream=" + event.upstream()
                    + " status=" + event.status()
                    + " bytes_in=" + event.bytesIn()
                    + " bytes_out=" + event.bytesOut()
                    + " duration_micros=" + event.durationMicros()
                    + " retries=" + event.retries()
                    + " shed=" + isShed(event.outcome())
                    + " cooldown=" + event.cooldown()
                    + " outcome=" + event.outcome().name();
        }
        return "{\"event\":\"proxy.access\""
                + ",\"timestamp\":\"" + timestamp + '"'
                + ",\"client\":\"-\""
                + ",\"method\":\"" + event.method() + '"'
                + ",\"path\":\"-\""
                + ",\"route\":\"" + event.route() + '"'
                + ",\"upstream\":\"" + event.upstream() + '"'
                + ",\"status\":" + event.status()
                + ",\"bytes_in\":" + event.bytesIn()
                + ",\"bytes_out\":" + event.bytesOut()
                + ",\"duration_micros\":" + event.durationMicros()
                + ",\"retries\":" + event.retries()
                + ",\"shed\":" + isShed(event.outcome())
                + ",\"cooldown\":" + event.cooldown()
                + ",\"outcome\":\"" + event.outcome().name() + "\"}";
    }

    private void warnFixedFailure(FailureKind kind) {
        int bit = 1 << kind.ordinal();
        int previous = warnedFailureKinds.getAndUpdate(current -> current | bit);
        if ((previous & bit) != 0) {
            return;
        }
        try {
            logger.warn("proxy.access_log.failure reason={}", kind.externalName);
        } catch (RuntimeException exception) {
            // An ordinary logging failure cannot escape the access-log boundary.
        }
    }

    private boolean selected() {
        double sampleRate = configuration.sampleRate();
        if (sampleRate <= 0.0) {
            return false;
        }
        if (sampleRate >= 1.0) {
            return true;
        }
        long value = sequence.getAndIncrement();
        long mixed = mix64(value);
        double unit = (mixed >>> 11) * 0x1.0p-53;
        return unit < sampleRate;
    }

    private static long mix64(long value) {
        long mixed = value + 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    private static String normalizedMethod(String value) {
        if (value == null) {
            return OTHER;
        }
        if (METHODS.contains(value)) {
            return value;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return METHODS.contains(normalized) ? normalized : OTHER;
    }

    private static String normalizedIdentifier(String value, String fallback) {
        if (!isLogicalIdentifier(value)) {
            return fallback;
        }
        return value;
    }

    private static boolean isLogicalIdentifier(String value) {
        if (value == null || value.isEmpty() || value.length() > 64 || !isAsciiLetterOrDigit(value.charAt(0))) {
            return false;
        }
        for (int index = 1; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!isAsciiLetterOrDigit(character)
                    && character != '.' && character != '_' && character != '-') {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiLetterOrDigit(char value) {
        return (value >= 'A' && value <= 'Z')
                || (value >= 'a' && value <= 'z')
                || (value >= '0' && value <= '9');
    }

    private static int normalizedStatus(int value) {
        return value >= 100 && value <= 599 ? value : 0;
    }

    private static long nonNegative(long value) {
        return Math.max(0L, value);
    }

    final class RequestLogObservation {
        private final long timestampMillis;
        private final long startedAtNanos;
        private final String method;
        private boolean completed;
        private boolean terminalAssigned;
        private long responseBytes;
        private int retries;
        private boolean cooldown;
        private String route = UNMATCHED;
        private String upstream = NONE;
        private int status;
        private long bytesIn;
        private long durationMicros;
        private ReverseProxyMetrics.TerminalOutcome outcome =
                ReverseProxyMetrics.TerminalOutcome.INTERNAL_ERROR;

        private RequestLogObservation(long timestampMillis, long startedAtNanos, String method) {
            this.timestampMillis = timestampMillis;
            this.startedAtNanos = startedAtNanos;
            this.method = method;
        }

        void bindRoute(String value) {
            if (!completed) {
                route = normalizedIdentifier(value, OTHER);
            }
        }

        void bindUpstream(String routeValue, String upstreamValue) {
            if (!completed) {
                route = normalizedIdentifier(routeValue, OTHER);
                upstream = normalizedIdentifier(upstreamValue, OTHER);
            }
        }

        void recordDispatch(boolean retry) {
            if (retry && !completed && retries < Integer.MAX_VALUE) {
                retries++;
            }
        }

        void addResponseBytes(long bytes) {
            if (bytes > 0 && !completed) {
                responseBytes = Long.MAX_VALUE - responseBytes < bytes
                        ? Long.MAX_VALUE
                        : responseBytes + bytes;
            }
        }

        void cooldownActivated() {
            if (!completed) {
                cooldown = true;
            }
        }

        void terminal(int statusCode, ReverseProxyMetrics.TerminalOutcome terminalOutcome) {
            if (completed) {
                return;
            }
            status = normalizedStatus(statusCode);
            outcome = terminalOutcome == null ? ReverseProxyMetrics.TerminalOutcome.OTHER : terminalOutcome;
            terminalAssigned = true;
        }

        void terminalIfUnset(int statusCode, ReverseProxyMetrics.TerminalOutcome terminalOutcome) {
            if (completed || terminalAssigned) {
                return;
            }
            terminalAssigned = true;
            status = normalizedStatus(statusCode);
            outcome = terminalOutcome == null ? ReverseProxyMetrics.TerminalOutcome.OTHER : terminalOutcome;
        }

        void complete(long requestBytes) {
            if (completed) {
                return;
            }
            completed = true;
            long elapsedNanos = Math.max(0L, System.nanoTime() - startedAtNanos);
            bytesIn = nonNegative(requestBytes);
            durationMicros = TimeUnit.NANOSECONDS.toMicros(elapsedNanos);
            enqueue(this);
        }

        private long timestampMillis() {
            return timestampMillis;
        }

        private String method() {
            return method;
        }

        private String route() {
            return route;
        }

        private String upstream() {
            return upstream;
        }

        private int status() {
            return status;
        }

        private long bytesIn() {
            return bytesIn;
        }

        private long bytesOut() {
            return responseBytes;
        }

        private long durationMicros() {
            return durationMicros;
        }

        private int retries() {
            return retries;
        }

        private boolean cooldown() {
            return cooldown;
        }

        private ReverseProxyMetrics.TerminalOutcome outcome() {
            return outcome;
        }
    }

    private static boolean isShed(ReverseProxyMetrics.TerminalOutcome outcome) {
        return outcome == ReverseProxyMetrics.TerminalOutcome.LOAD_SHED
                || outcome == ReverseProxyMetrics.TerminalOutcome.GLOBAL_CONCURRENCY_LIMIT
                || outcome == ReverseProxyMetrics.TerminalOutcome.UPSTREAM_CONCURRENCY_LIMIT;
    }

    private enum Format {
        JSON,
        COMBINED
    }

    private enum FailureKind {
        OPEN("open"),
        WRITE("write"),
        FLUSH("flush"),
        CLOSE("close");

        private final String externalName;

        FailureKind(String externalName) {
            this.externalName = externalName;
        }
    }

    private record Configuration(boolean enabled, Format format, Path path, double sampleRate) {
        private static Configuration from(ReverseProxyProperties.AccessLog properties) {
            ReverseProxyProperties.AccessLog safe = properties == null
                    ? new ReverseProxyProperties.AccessLog()
                    : properties;
            String rawFormat = safe.getFormat();
            Format format;
            try {
                format = Format.valueOf(rawFormat == null ? "" : rawFormat.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException(
                        "loadbalancerpro.proxy.access-log.format must be JSON or COMBINED");
            }
            String rawPath = safe.getPath();
            if (rawPath == null || rawPath.isBlank() || containsControlCharacter(rawPath)) {
                throw new IllegalStateException(
                        "loadbalancerpro.proxy.access-log.path must be a non-blank path without control characters");
            }
            Path path;
            try {
                path = Path.of(rawPath).toAbsolutePath().normalize();
            } catch (InvalidPathException exception) {
                throw new IllegalStateException("loadbalancerpro.proxy.access-log.path must be a valid local path");
            }
            double sampleRate = safe.getSampleRate();
            if (!Double.isFinite(sampleRate) || sampleRate < 0.0 || sampleRate > 1.0) {
                throw new IllegalStateException(
                        "loadbalancerpro.proxy.access-log.sample-rate must be between 0.0 and 1.0");
            }
            return new Configuration(safe.isEnabled(), format, path, sampleRate);
        }
    }

    private static boolean containsControlCharacter(String value) {
        return value.codePoints().anyMatch(Character::isISOControl);
    }

    /** Single-consumer bounded ring; producers never wait for capacity. */
    private static final class BoundedMpscQueue<E> {
        private final AtomicReferenceArray<E> entries;
        private final int mask;
        private final AtomicLong producerIndex = new AtomicLong();
        private final AtomicLong consumerIndex = new AtomicLong();

        private BoundedMpscQueue(int capacity) {
            if (capacity <= 0 || Integer.bitCount(capacity) != 1) {
                throw new IllegalArgumentException("queueCapacity must be a positive power of two");
            }
            this.entries = new AtomicReferenceArray<>(capacity);
            this.mask = capacity - 1;
        }

        private boolean offer(E entry) {
            Objects.requireNonNull(entry, "entry cannot be null");
            while (true) {
                long producer = producerIndex.get();
                long consumer = consumerIndex.get();
                if (producer - consumer >= entries.length()) {
                    return false;
                }
                if (producerIndex.compareAndSet(producer, producer + 1)) {
                    entries.lazySet((int) producer & mask, entry);
                    return true;
                }
                Thread.onSpinWait();
            }
        }

        private E poll() {
            long consumer = consumerIndex.get();
            if (consumer >= producerIndex.get()) {
                return null;
            }
            int slot = (int) consumer & mask;
            E entry = entries.get(slot);
            if (entry == null) {
                return null;
            }
            entries.set(slot, null);
            consumerIndex.lazySet(consumer + 1);
            return entry;
        }

        private boolean isEmpty() {
            return consumerIndex.get() >= producerIndex.get();
        }

        private int size() {
            return (int) Math.min(Integer.MAX_VALUE,
                    Math.max(0L, producerIndex.get() - consumerIndex.get()));
        }

        private long offeredCount() {
            return producerIndex.get();
        }
    }

    @FunctionalInterface
    interface EventWriterFactory {
        EventWriter open(Path path) throws IOException;
    }

    interface EventWriter extends AutoCloseable {
        void append(String line) throws IOException;

        void flush() throws IOException;

        @Override
        void close() throws IOException;
    }

    private static final class FileEventWriter implements EventWriter {
        private final BufferedWriter writer;

        private FileEventWriter(Path path) throws IOException {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            this.writer = Files.newBufferedWriter(
                    path,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND);
        }

        @Override
        public void append(String line) throws IOException {
            writer.write(line);
            writer.newLine();
        }

        @Override
        public void flush() throws IOException {
            writer.flush();
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }

    private enum NoOpEventWriter implements EventWriter {
        INSTANCE;

        @Override
        public void append(String line) {
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
