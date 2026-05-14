package com.richmond423.loadbalancerpro.core;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class AdaptiveRoutingPolicyAuditLog {
    public static final int DEFAULT_MAX_EVENTS = 100;

    private final int maxEvents;
    private final Clock clock;
    private final AtomicLong sequence = new AtomicLong();
    private final ArrayDeque<AdaptiveRoutingPolicyAuditEvent> events = new ArrayDeque<>();

    public AdaptiveRoutingPolicyAuditLog() {
        this(DEFAULT_MAX_EVENTS, Clock.systemUTC());
    }

    public AdaptiveRoutingPolicyAuditLog(int maxEvents, Clock clock) {
        this.maxEvents = Math.max(1, maxEvents);
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public synchronized AdaptiveRoutingPolicyAuditEvent record(AdaptiveRoutingPolicyDecision decision) {
        AdaptiveRoutingPolicyAuditEvent event = AdaptiveRoutingPolicyAuditEvent.from(
                "lase-policy-%04d".formatted(sequence.incrementAndGet()),
                clock.instant(),
                decision);
        events.addLast(event);
        while (events.size() > maxEvents) {
            events.removeFirst();
        }
        return event;
    }

    public synchronized List<AdaptiveRoutingPolicyAuditEvent> snapshot() {
        return List.copyOf(new ArrayList<>(events));
    }

    public synchronized int size() {
        return events.size();
    }

    public int maxEvents() {
        return maxEvents;
    }

    public synchronized Optional<AdaptiveRoutingPolicyAuditEvent> lastEvent() {
        return Optional.ofNullable(events.peekLast());
    }
}
