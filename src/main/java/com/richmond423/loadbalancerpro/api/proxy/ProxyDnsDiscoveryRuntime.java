package com.richmond423.loadbalancerpro.api.proxy;

import java.net.InetAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bounded asynchronous DNS lookup, timeout, stale-expiry, and publication engine.
 * Request, status, and configuration threads only submit registrations or read immutable snapshots.
 */
final class ProxyDnsDiscoveryRuntime implements AutoCloseable {
    static final int MAX_NAMES = ReverseProxyRoutePlanner.MAX_CONFIGURED_TARGETS;
    static final String SCHEDULER_THREAD_NAME = "loadbalancerpro-dns-scheduler";
    static final String LOOKUP_THREAD_PREFIX = "loadbalancerpro-dns-lookup-";
    static final String PUBLICATION_THREAD_NAME = "loadbalancerpro-dns-publication";

    private final ProxyDnsDiscoverySettings settings;
    private final Resolver resolver;
    private final Scheduler scheduler;
    private final LookupExecutor lookupExecutor;
    private final Publisher publisher;
    private final AtomicReference<Snapshot> latest = new AtomicReference<>(Snapshot.empty());
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Map<String, NameState> names = new LinkedHashMap<>();
    private long currentGeneration;
    private long nextAttemptId;

    ProxyDnsDiscoveryRuntime(ProxyDnsDiscoverySettings settings, Resolver resolver, Listener listener) {
        this(settings, resolver,
                new BoundedScheduler(MAX_NAMES * 3 + ProxyDnsDiscoverySettings.MAX_LOOKUP_THREADS),
                new BoundedLookupExecutor(settings.lookupThreads()),
                new CoalescingPublisher(listener));
    }

    ProxyDnsDiscoveryRuntime(
            ProxyDnsDiscoverySettings settings,
            Resolver resolver,
            Scheduler scheduler,
            LookupExecutor lookupExecutor,
            Publisher publisher) {
        this.settings = Objects.requireNonNull(settings, "settings cannot be null");
        this.resolver = Objects.requireNonNull(resolver, "resolver cannot be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler cannot be null");
        this.lookupExecutor = Objects.requireNonNull(lookupExecutor, "lookupExecutor cannot be null");
        this.publisher = Objects.requireNonNull(publisher, "publisher cannot be null");
    }

    void replace(long generation, Collection<Registration> registrations) {
        if (closed.get()) {
            throw new IllegalStateException("DNS discovery runtime is closed");
        }
        if (generation < 1) {
            throw new IllegalArgumentException("generation must be positive");
        }
        List<Registration> safe = validatedRegistrations(registrations);
        scheduler.schedule(() -> replaceOnScheduler(generation, safe), 0);
    }

    Snapshot snapshot() {
        return latest.get();
    }

    int scheduledTaskCount() {
        return scheduler.queuedTaskCount();
    }

    int queuedLookupCount() {
        return lookupExecutor.queuedTaskCount();
    }

    private static List<Registration> validatedRegistrations(Collection<Registration> registrations) {
        Objects.requireNonNull(registrations, "registrations cannot be null");
        if (registrations.size() > MAX_NAMES) {
            throw new IllegalStateException("DNS discovery must contain at most " + MAX_NAMES + " registrations");
        }
        Map<String, Registration> byLogicalId = new LinkedHashMap<>();
        for (Registration registration : registrations) {
            Objects.requireNonNull(registration, "registration cannot be null");
            if (byLogicalId.putIfAbsent(registration.logicalId(), registration) != null) {
                throw new IllegalStateException("duplicate DNS discovery logical id: " + registration.logicalId());
            }
        }
        long distinctNames = registrations.stream().map(item -> item.spec().name()).distinct().count();
        if (distinctNames > MAX_NAMES) {
            throw new IllegalStateException("DNS discovery must contain at most " + MAX_NAMES + " names");
        }
        return byLogicalId.values().stream()
                .sorted(Comparator.comparing(Registration::logicalId))
                .toList();
    }

    private void replaceOnScheduler(long generation, List<Registration> registrations) {
        if (closed.get() || generation < currentGeneration) {
            return;
        }
        currentGeneration = generation;
        Map<String, List<Registration>> byName = new TreeMap<>();
        registrations.forEach(registration -> byName
                .computeIfAbsent(registration.spec().name(), ignored -> new ArrayList<>())
                .add(registration));

        for (NameState state : names.values()) {
            state.cancelScheduled();
            state.desired = false;
        }
        for (Map.Entry<String, List<Registration>> desired : byName.entrySet()) {
            NameState state = names.computeIfAbsent(desired.getKey(), NameState::new);
            state.desired = true;
            state.generation = generation;
            Map<String, ConsumerState> previous = state.consumers;
            Map<String, ConsumerState> consumers = new LinkedHashMap<>();
            for (Registration registration : desired.getValue()) {
                ConsumerState prior = previous.get(registration.logicalId());
                consumers.put(registration.logicalId(), prior != null && prior.sameContract(registration)
                        ? prior.withRegistration(registration)
                        : new ConsumerState(registration));
            }
            state.consumers = consumers;
            scheduleStale(state);
            if (state.attempt == null) {
                scheduleRefresh(state, 0);
            }
        }
        names.entrySet().removeIf(entry -> !entry.getValue().desired && entry.getValue().attempt == null);
        publishSnapshot();
    }

    private void scheduleRefresh(NameState state, long delayNanos) {
        state.cancelRefresh();
        if (!state.desired || closed.get()) {
            return;
        }
        long generation = state.generation;
        state.refresh = scheduler.schedule(
                () -> beginLookup(state.name, generation), Math.max(0, delayNanos));
    }

    private void beginLookup(String name, long generation) {
        NameState state = names.get(name);
        if (state == null || !state.desired || state.generation != generation || closed.get()) {
            return;
        }
        state.refresh = Cancellable.NONE;
        if (state.attempt != null) {
            scheduleRefresh(state, settings.ttlFloor().toNanos());
            return;
        }
        long startedAt = scheduler.nanoTime();
        Attempt attempt = new Attempt(++nextAttemptId, generation, startedAt);
        state.attempt = attempt;
        state.lastAttemptStartedNanos = startedAt;
        attempt.timeout = scheduler.schedule(
                () -> timeout(name, attempt.id, generation), settings.resolutionTimeout().toNanos());
        boolean accepted = lookupExecutor.submit(() -> resolve(name, attempt.id, generation));
        if (!accepted) {
            attempt.timeout.cancel();
            state.attempt = null;
            state.consumers.values().forEach(consumer -> consumer.outcome = Outcome.REJECTED);
            expireConsumers(state, scheduler.nanoTime());
            publishSnapshot();
            scheduleNextAttempt(state);
        } else {
            publishSnapshot();
        }
    }

    private void resolve(String name, long attemptId, long generation) {
        LookupResult result = resolveResult(name);
        try {
            scheduler.schedule(() -> complete(name, attemptId, generation, result), 0);
        } catch (RejectedExecutionException ignored) {
            // Shutdown or the fixed scheduler bound won the race; no callback is retained.
        }
    }

    private LookupResult resolveResult(String name) {
        try {
            List<InetAddress> answers = resolver.resolve(name);
            return LookupResult.success(answers == null ? List.of() : List.copyOf(answers));
        } catch (Throwable failure) {
            return LookupResult.failure();
        }
    }

    private void timeout(String name, long attemptId, long generation) {
        NameState state = names.get(name);
        if (!matches(state, attemptId, generation)) {
            return;
        }
        state.attempt.timedOut = true;
        state.consumers.values().forEach(consumer -> consumer.outcome = Outcome.TIMEOUT);
        expireConsumers(state, scheduler.nanoTime());
        publishSnapshot();
        scheduleNextAttempt(state);
    }

    private void complete(String name, long attemptId, long generation, LookupResult result) {
        NameState state = names.get(name);
        if (!matches(state, attemptId, generation)) {
            return;
        }
        Attempt attempt = state.attempt;
        attempt.timeout.cancel();
        state.attempt = null;
        if (!state.desired) {
            names.remove(name);
            publishSnapshot();
            return;
        }
        if (state.generation != generation || attempt.timedOut) {
            scheduleRefresh(state, 0);
            return;
        }

        long now = scheduler.nanoTime();
        if (!result.success) {
            state.consumers.values().forEach(consumer -> consumer.outcome = Outcome.FAILURE);
        } else {
            applyAnswer(state, result.answers, now);
        }
        expireConsumers(state, now);
        publishSnapshot();
        scheduleNextAttempt(state);
        scheduleStale(state);
    }

    private void applyAnswer(NameState state, List<InetAddress> answers, long now) {
        try {
            if (!state.consumers.isEmpty()) {
                Registration first = state.consumers.values().iterator().next().registration;
                ProxyDnsDiscovery.members(first.spec(), first.logicalId(), answers, false);
            }
            for (ConsumerState consumer : state.consumers.values()) {
                List<ProxyDnsDiscovery.Member> members = ProxyDnsDiscovery.members(
                        consumer.registration.spec(),
                        consumer.registration.logicalId(),
                        answers,
                        consumer.registration.privateNetworkOnly());
                if (members.isEmpty()) {
                    consumer.outcome = Outcome.EMPTY;
                } else {
                    consumer.members = members;
                    consumer.lastSuccessNanos = now;
                    consumer.outcome = Outcome.SUCCESS;
                }
            }
        } catch (IllegalStateException invalidAnswer) {
            state.consumers.values().forEach(consumer -> consumer.outcome = Outcome.INVALID_ANSWER);
        }
    }

    private void scheduleNextAttempt(NameState state) {
        long elapsed = Math.max(0, scheduler.nanoTime() - state.lastAttemptStartedNanos);
        long delay = Math.max(0, settings.ttlFloor().toNanos() - elapsed);
        scheduleRefresh(state, delay);
    }

    private void scheduleStale(NameState state) {
        state.cancelStale();
        if (!state.desired) {
            return;
        }
        long now = scheduler.nanoTime();
        long earliest = Long.MAX_VALUE;
        for (ConsumerState consumer : state.consumers.values()) {
            if (consumer.lastSuccessNanos != Long.MIN_VALUE && !consumer.members.isEmpty()) {
                long deadline = saturatedAdd(consumer.lastSuccessNanos, settings.staleAfter().toNanos());
                earliest = Math.min(earliest, deadline);
            }
        }
        if (earliest != Long.MAX_VALUE) {
            long generation = state.generation;
            state.stale = scheduler.schedule(
                    () -> expireStale(state.name, generation), Math.max(0, earliest - now));
        }
    }

    private void expireStale(String name, long generation) {
        NameState state = names.get(name);
        if (state == null || !state.desired || state.generation != generation) {
            return;
        }
        state.stale = Cancellable.NONE;
        if (expireConsumers(state, scheduler.nanoTime())) {
            publishSnapshot();
        }
        scheduleStale(state);
    }

    private boolean expireConsumers(NameState state, long now) {
        boolean changed = false;
        long staleNanos = settings.staleAfter().toNanos();
        for (ConsumerState consumer : state.consumers.values()) {
            if (consumer.lastSuccessNanos != Long.MIN_VALUE
                    && now - consumer.lastSuccessNanos >= staleNanos
                    && !consumer.members.isEmpty()) {
                consumer.members = List.of();
                consumer.outcome = Outcome.STALE;
                changed = true;
            }
        }
        return changed;
    }

    private void publishSnapshot() {
        long now = scheduler.nanoTime();
        Map<String, List<ProxyDnsDiscovery.Member>> members = new TreeMap<>();
        Map<String, Status> statuses = new TreeMap<>();
        for (NameState state : names.values()) {
            if (!state.desired) {
                continue;
            }
            for (ConsumerState consumer : state.consumers.values()) {
                String logicalId = consumer.registration.logicalId();
                members.put(logicalId, List.copyOf(consumer.members));
                long ageMillis = consumer.lastSuccessNanos == Long.MIN_VALUE
                        ? -1
                        : TimeUnit.NANOSECONDS.toMillis(Math.max(0, now - consumer.lastSuccessNanos));
                long remainingMillis = consumer.lastSuccessNanos == Long.MIN_VALUE || consumer.members.isEmpty()
                        ? 0
                        : TimeUnit.NANOSECONDS.toMillis(Math.max(0,
                                settings.staleAfter().toNanos() - (now - consumer.lastSuccessNanos)));
                statuses.put(logicalId, new Status(
                        state.name,
                        consumer.registration.spec().port(),
                        consumer.registration.spec().authorityMode(),
                        consumer.outcome,
                        state.attempt != null,
                        consumer.members.size(),
                        ageMillis,
                        remainingMillis));
            }
        }
        Snapshot snapshot = new Snapshot(currentGeneration, members, statuses);
        latest.set(snapshot);
        publisher.publish(snapshot);
    }

    private static boolean matches(NameState state, long attemptId, long generation) {
        return state != null && state.attempt != null
                && state.attempt.id == attemptId && state.attempt.generation == generation;
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        scheduler.close();
        lookupExecutor.close();
        publisher.close();
        latest.set(Snapshot.empty());
    }

    @FunctionalInterface
    interface Resolver {
        List<InetAddress> resolve(String normalizedName) throws Exception;

        static Resolver system() {
            return name -> List.of(InetAddress.getAllByName(name));
        }
    }

    @FunctionalInterface
    interface Listener {
        void published(Snapshot snapshot);
    }

    record Registration(
            String logicalId,
            ProxyDnsDiscovery.Spec spec,
            boolean privateNetworkOnly) {
        Registration {
            logicalId = ReverseProxyRoutePlanner.validateUpstreamId(
                    logicalId, "DNS discovery logicalId");
            spec = Objects.requireNonNull(spec, "spec cannot be null");
        }
    }

    enum Outcome {
        NEVER,
        SUCCESS,
        EMPTY,
        FAILURE,
        TIMEOUT,
        REJECTED,
        INVALID_ANSWER,
        STALE
    }

    record Status(
            String name,
            int port,
            String authorityMode,
            Outcome outcome,
            boolean lookupInFlight,
            int memberCount,
            long lastSuccessAgeMillis,
            long staleRemainingMillis) {
    }

    record Snapshot(
            long generation,
            Map<String, List<ProxyDnsDiscovery.Member>> membersByLogicalId,
            Map<String, Status> statusByLogicalId) {
        Snapshot {
            Map<String, List<ProxyDnsDiscovery.Member>> copiedMembers = new TreeMap<>();
            membersByLogicalId.forEach((key, value) -> copiedMembers.put(key, List.copyOf(value)));
            membersByLogicalId = Map.copyOf(copiedMembers);
            statusByLogicalId = Map.copyOf(new TreeMap<>(statusByLogicalId));
        }

        static Snapshot empty() {
            return new Snapshot(0, Map.of(), Map.of());
        }
    }

    interface Scheduler extends AutoCloseable {
        long nanoTime();

        Cancellable schedule(Runnable task, long delayNanos);

        int queuedTaskCount();

        @Override
        void close();
    }

    interface LookupExecutor extends AutoCloseable {
        boolean submit(Runnable task);

        int queuedTaskCount();

        @Override
        void close();
    }

    interface Publisher extends AutoCloseable {
        void publish(Snapshot snapshot);

        @Override
        void close();
    }

    interface Cancellable {
        Cancellable NONE = () -> {
        };

        void cancel();
    }

    private static final class NameState {
        private final String name;
        private long generation;
        private boolean desired;
        private Map<String, ConsumerState> consumers = new LinkedHashMap<>();
        private Attempt attempt;
        private long lastAttemptStartedNanos;
        private Cancellable refresh = Cancellable.NONE;
        private Cancellable stale = Cancellable.NONE;

        private NameState(String name) {
            this.name = name;
        }

        private void cancelRefresh() {
            refresh.cancel();
            refresh = Cancellable.NONE;
        }

        private void cancelStale() {
            stale.cancel();
            stale = Cancellable.NONE;
        }

        private void cancelScheduled() {
            cancelRefresh();
            cancelStale();
        }
    }

    private static final class ConsumerState {
        private final Registration registration;
        private List<ProxyDnsDiscovery.Member> members;
        private long lastSuccessNanos;
        private Outcome outcome;

        private ConsumerState(Registration registration) {
            this(registration, List.of(), Long.MIN_VALUE, Outcome.NEVER);
        }

        private ConsumerState(
                Registration registration,
                List<ProxyDnsDiscovery.Member> members,
                long lastSuccessNanos,
                Outcome outcome) {
            this.registration = registration;
            this.members = members;
            this.lastSuccessNanos = lastSuccessNanos;
            this.outcome = outcome;
        }

        private boolean sameContract(Registration candidate) {
            return registration.spec().equals(candidate.spec())
                    && registration.privateNetworkOnly() == candidate.privateNetworkOnly();
        }

        private ConsumerState withRegistration(Registration candidate) {
            return new ConsumerState(candidate, members, lastSuccessNanos, outcome);
        }
    }

    private static final class Attempt {
        private final long id;
        private final long generation;
        private final long startedAtNanos;
        private boolean timedOut;
        private Cancellable timeout = Cancellable.NONE;

        private Attempt(long id, long generation, long startedAtNanos) {
            this.id = id;
            this.generation = generation;
            this.startedAtNanos = startedAtNanos;
        }
    }

    private record LookupResult(boolean success, List<InetAddress> answers) {
        private static LookupResult success(List<InetAddress> answers) {
            return new LookupResult(true, answers);
        }

        private static LookupResult failure() {
            return new LookupResult(false, List.of());
        }
    }

    private static final class BoundedLookupExecutor implements LookupExecutor {
        private final ThreadPoolExecutor executor;

        private BoundedLookupExecutor(int threadCount) {
            AtomicLong sequence = new AtomicLong();
            executor = new ThreadPoolExecutor(
                    threadCount,
                    threadCount,
                    0,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(MAX_NAMES),
                    runnable -> {
                        Thread thread = new Thread(runnable, LOOKUP_THREAD_PREFIX + sequence.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    },
                    new ThreadPoolExecutor.AbortPolicy());
        }

        @Override
        public boolean submit(Runnable task) {
            try {
                executor.execute(task);
                return true;
            } catch (RejectedExecutionException rejected) {
                return false;
            }
        }

        @Override
        public int queuedTaskCount() {
            return executor.getQueue().size();
        }

        @Override
        public void close() {
            executor.shutdownNow();
            awaitTermination(executor);
        }
    }

    private static final class BoundedScheduler implements Scheduler {
        private final Object monitor = new Object();
        private final PriorityQueue<ScheduledTask> queue;
        private final int capacity;
        private final AtomicLong sequence = new AtomicLong();
        private final Thread thread;
        private boolean stopped;

        private BoundedScheduler(int capacity) {
            this.capacity = capacity;
            this.queue = new PriorityQueue<>(capacity);
            this.thread = new Thread(this::run, SCHEDULER_THREAD_NAME);
            this.thread.setDaemon(true);
            this.thread.start();
        }

        @Override
        public long nanoTime() {
            return System.nanoTime();
        }

        @Override
        public Cancellable schedule(Runnable task, long delayNanos) {
            Objects.requireNonNull(task, "task cannot be null");
            ScheduledTask scheduled = new ScheduledTask(
                    saturatedAdd(nanoTime(), Math.max(0, delayNanos)), sequence.incrementAndGet(), task);
            synchronized (monitor) {
                if (stopped) {
                    throw new RejectedExecutionException("DNS scheduler is stopped");
                }
                if (queue.size() >= capacity) {
                    throw new RejectedExecutionException("DNS scheduler queue is full");
                }
                queue.add(scheduled);
                monitor.notifyAll();
            }
            return () -> cancel(scheduled);
        }

        @Override
        public int queuedTaskCount() {
            synchronized (monitor) {
                return queue.size();
            }
        }

        private void cancel(ScheduledTask task) {
            synchronized (monitor) {
                task.cancelled = true;
                queue.remove(task);
                monitor.notifyAll();
            }
        }

        private void run() {
            while (true) {
                ScheduledTask task;
                synchronized (monitor) {
                    while (true) {
                        if (stopped) {
                            return;
                        }
                        task = queue.peek();
                        if (task == null) {
                            waitOnMonitor(0, 0);
                            continue;
                        }
                        long remaining = task.deadlineNanos - nanoTime();
                        if (remaining > 0) {
                            long millis = remaining / 1_000_000;
                            int nanos = (int) (remaining % 1_000_000);
                            waitOnMonitor(millis, nanos);
                            continue;
                        }
                        queue.poll();
                        break;
                    }
                }
                if (!task.cancelled) {
                    try {
                        task.command.run();
                    } catch (RuntimeException ignored) {
                        // A task cannot terminate the bounded scheduler thread.
                    }
                }
            }
        }

        private void waitOnMonitor(long millis, int nanos) {
            try {
                monitor.wait(millis, nanos);
            } catch (InterruptedException interrupted) {
                if (stopped) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        public void close() {
            synchronized (monitor) {
                stopped = true;
                queue.clear();
                monitor.notifyAll();
            }
            thread.interrupt();
            join(thread);
        }
    }

    private static final class ScheduledTask implements Comparable<ScheduledTask> {
        private final long deadlineNanos;
        private final long sequence;
        private final Runnable command;
        private boolean cancelled;

        private ScheduledTask(long deadlineNanos, long sequence, Runnable command) {
            this.deadlineNanos = deadlineNanos;
            this.sequence = sequence;
            this.command = command;
        }

        @Override
        public int compareTo(ScheduledTask other) {
            int deadline = Long.compare(deadlineNanos, other.deadlineNanos);
            return deadline != 0 ? deadline : Long.compare(sequence, other.sequence);
        }
    }

    private static final class CoalescingPublisher implements Publisher {
        private final Listener listener;
        private final Object monitor = new Object();
        private final AtomicReference<Snapshot> pending = new AtomicReference<>();
        private final Thread thread;
        private boolean stopped;

        private CoalescingPublisher(Listener listener) {
            this.listener = Objects.requireNonNull(listener, "listener cannot be null");
            this.thread = new Thread(this::run, PUBLICATION_THREAD_NAME);
            this.thread.setDaemon(true);
            this.thread.start();
        }

        @Override
        public void publish(Snapshot snapshot) {
            pending.set(snapshot);
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }

        private void run() {
            while (true) {
                Snapshot snapshot = null;
                synchronized (monitor) {
                    while (!stopped && (snapshot = pending.getAndSet(null)) == null) {
                        try {
                            monitor.wait();
                        } catch (InterruptedException interrupted) {
                            if (stopped) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                    if (stopped) {
                        return;
                    }
                }
                try {
                    listener.published(snapshot);
                } catch (RuntimeException ignored) {
                    // A publication callback cannot terminate the bounded publisher thread.
                }
            }
        }

        @Override
        public void close() {
            synchronized (monitor) {
                stopped = true;
                pending.set(null);
                monitor.notifyAll();
            }
            thread.interrupt();
            join(thread);
        }
    }

    private static void awaitTermination(ThreadPoolExecutor executor) {
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void join(Thread thread) {
        try {
            thread.join(Duration.ofSeconds(5).toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
