package com.richmond423.loadbalancerpro.api;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.richmond423.loadbalancerpro.api.proxy.LiveRoutingObservationSink;
import com.richmond423.loadbalancerpro.core.LiveRoutingShadowObservation;

/** Lab-side adapter for the production proxy observation boundary. */
@Component
public final class LaseProxyObservationSink implements LiveRoutingObservationSink {
    private final LaseShadowRuntime runtime;

    public LaseProxyObservationSink(LaseShadowRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime cannot be null");
    }

    @Override
    public boolean isEnabled() {
        return runtime.isLiveProxyEnabled();
    }

    @Override
    public boolean submit(Observation observation) {
        Objects.requireNonNull(observation, "observation cannot be null");
        return runtime.submitLiveRouting(new LiveRoutingShadowObservation(
                observation.decisionId(),
                observation.observedAt(),
                observation.routeName(),
                observation.strategy(),
                observation.selectionSource(),
                observation.actualSelectedServerId(),
                observation.candidates(),
                observation.initialConcurrencyLimit(),
                observation.telemetrySampleSize()));
    }
}
