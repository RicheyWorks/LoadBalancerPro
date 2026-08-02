package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ReverseProxyMicrometerMetricsTest {

    @Test
    void requestLifecycleRecordsExactOnceTerminalLatencyInflightAttemptsAndBytes() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ReverseProxyMetrics metrics = configuredMetrics(registry, "alpha");

        ReverseProxyMetrics.RequestObservation observation = metrics.beginRequest();
        assertEquals(1.0, totalGauge(registry, ReverseProxyMetrics.INFLIGHT));
        observation.bindRoute(ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME);
        observation.bindUpstream(ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME, "alpha");
        assertEquals(1.0, totalGauge(registry, ReverseProxyMetrics.INFLIGHT));
        observation.recordDispatch(false, ReverseProxyMetrics.RetryReason.INITIAL);
        observation.addResponseBytes(13);
        observation.terminal(200, ReverseProxyMetrics.TerminalOutcome.SUCCESS);
        observation.complete(7);
        observation.complete(999);

        assertEquals(0.0, totalGauge(registry, ReverseProxyMetrics.INFLIGHT));
        assertEquals(1.0, counter(registry, ReverseProxyMetrics.REQUESTS,
                "route", ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME,
                "upstream", "alpha",
                "status_class", "2xx",
                "outcome", "SUCCESS").count());
        assertEquals(1L, registry.get(ReverseProxyMetrics.LATENCY)
                .tags("route", ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME, "upstream", "alpha")
                .timer().count());
        assertEquals(1.0, counter(registry, ReverseProxyMetrics.ATTEMPTS,
                "route", ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME,
                "upstream", "alpha",
                "kind", "PRIMARY",
                "reason", "INITIAL").count());
        assertEquals(0.0, counter(registry, ReverseProxyMetrics.RETRIES,
                "route", ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME,
                "upstream", "alpha",
                "reason", "TRANSPORT_FAILURE").count());
        assertEquals(7.0, registry.get(ReverseProxyMetrics.REQUEST_BYTES)
                .tags("route", ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME, "upstream", "alpha")
                .summary().totalAmount());
        assertEquals(13.0, registry.get(ReverseProxyMetrics.RESPONSE_BYTES)
                .tags("route", ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME, "upstream", "alpha")
                .summary().totalAmount());
    }

    @Test
    void actualRetryAndLimitAndShedReasonsUseOnlyClosedTagValues() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ReverseProxyMetrics metrics = configuredMetrics(registry, "alpha");
        ReverseProxyMetrics.RequestObservation retry = metrics.beginRequest();
        retry.bindUpstream(ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME, "alpha");
        retry.recordDispatch(false, ReverseProxyMetrics.RetryReason.INITIAL);
        retry.recordDispatch(true, ReverseProxyMetrics.RetryReason.TRANSPORT_FAILURE);
        retry.terminal(200, ReverseProxyMetrics.TerminalOutcome.SUCCESS);
        retry.complete(0);

        assertEquals(1.0, counter(registry, ReverseProxyMetrics.RETRIES,
                "route", ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME,
                "upstream", "alpha",
                "reason", "TRANSPORT_FAILURE").count());

        ReverseProxyMetrics.RequestObservation requestLimit = metrics.beginRequest();
        requestLimit.terminal(413, ReverseProxyMetrics.TerminalOutcome.REQUEST_SIZE_LIMIT);
        requestLimit.complete(65_537);
        assertEquals(1.0, counter(registry, ReverseProxyMetrics.LIMIT_REJECTIONS,
                "route", ReverseProxyMetrics.UNMATCHED,
                "upstream", ReverseProxyMetrics.NONE,
                "direction", "REQUEST",
                "phase", "PRECOMMIT").count());

        ReverseProxyMetrics.RequestObservation shed = metrics.beginRequest();
        shed.bindRoute(ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME);
        shed.terminal(503, ReverseProxyMetrics.TerminalOutcome.LOAD_SHED);
        shed.complete(0);
        assertEquals(1.0, counter(registry, ReverseProxyMetrics.SHEDS,
                "route", ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME,
                "upstream", ReverseProxyMetrics.NONE,
                "reason", "LOAD_SHED").count());

        assertTrue(tagValues(registry, ReverseProxyMetrics.RETRIES, "reason")
                .stream().allMatch(value -> Set.of(
                        "INITIAL", "RETRYABLE_STATUS", "TRANSPORT_FAILURE",
                        "PRECOMMIT_UPSTREAM_FAILURE", "OTHER").contains(value)));
    }

    @Test
    void variedRequestDerivedValuesCollapseWithoutCreatingUnboundedSeriesOrSensitiveTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ReverseProxyMetrics metrics = configuredMetrics(registry, "alpha");
        int before = registry.getMeters().size();

        for (int index = 0; index < 200; index++) {
            ReverseProxyMetrics.RequestObservation observation = metrics.beginRequest();
            observation.bindRoute("/private/path?request=" + index);
            observation.bindUpstream("https://secret-" + index + ".example.test:8443", "api-key-" + index);
            observation.terminal(0, ReverseProxyMetrics.TerminalOutcome.OTHER);
            observation.complete(index);
        }

        int growth = registry.getMeters().size() - before;
        assertTrue(growth <= 7, "varied request data created unexpected series growth: " + growth);
        Set<String> allTagValues = registry.getMeters().stream()
                .flatMap(meter -> meter.getId().getTags().stream())
                .map(tag -> tag.getValue())
                .collect(Collectors.toSet());
        assertFalse(allTagValues.stream().anyMatch(value -> value.contains("secret")
                || value.contains("api-key") || value.contains("private/path")));
        assertTrue(allTagValues.contains(ReverseProxyMetrics.OTHER));
        assertTrue(allTagValues.contains(ReverseProxyMetrics.UNMATCHED));
    }

    @Test
    void retiredConfigurationMetersRemainOnlyForInflightObservationThenAreRemoved() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ReverseProxyMetrics metrics = configuredMetrics(registry, "alpha");
        ReverseProxyMetrics.RequestObservation oldGeneration = metrics.beginRequest();
        oldGeneration.bindUpstream(ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME, "alpha");

        metrics.activateConfiguration(routes("beta"));
        assertTrue(hasTagValue(registry, "upstream", "alpha"));
        oldGeneration.terminal(200, ReverseProxyMetrics.TerminalOutcome.SUCCESS);
        oldGeneration.complete(0);
        assertFalse(hasTagValue(registry, "upstream", "alpha"));

        int stableCount = registry.getMeters().size();
        for (int generation = 0; generation < 50; generation++) {
            metrics.activateConfiguration(routes("backend-" + generation));
            assertTrue(registry.getMeters().size() <= stableCount,
                    "meter count grew after generation " + generation);
        }
        assertFalse(hasTagValue(registry, "upstream", "beta"));
        assertTrue(hasTagValue(registry, "upstream", "backend-49"));
    }

    @Test
    void meterRegistryFailureCannotEscapeInstrumentationBoundary() {
        SimpleMeterRegistry failing = new SimpleMeterRegistry();
        failing.config().onMeterAdded(meter -> {
            throw new IllegalStateException("synthetic registry failure containing secret-value");
        });

        assertDoesNotThrow(() -> {
            ReverseProxyMetrics metrics = configuredMetrics(failing, "alpha");
            ReverseProxyMetrics.RequestObservation observation = metrics.beginRequest();
            observation.bindUpstream(ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME, "alpha");
            observation.recordDispatch(false, ReverseProxyMetrics.RetryReason.INITIAL);
            observation.addResponseBytes(5);
            observation.terminal(200, ReverseProxyMetrics.TerminalOutcome.SUCCESS);
            observation.complete(3);
            metrics.recordHealth("alpha", false);
            metrics.recordCooldownActivation("alpha");
        });
    }

    @Test
    void logicalTagValidationAndConfigurationCountCapsAreFailClosed() {
        ReverseProxyProperties invalidId = properties("backend with spaces");
        assertThrows(IllegalStateException.class, () -> routes(invalidId));

        ReverseProxyProperties reservedId = properties(ReverseProxyMetrics.OTHER);
        assertThrows(IllegalStateException.class, () -> routes(reservedId));

        ReverseProxyProperties tooMany = new ReverseProxyProperties();
        tooMany.setEnabled(true);
        tooMany.setUpstreams(IntStream.rangeClosed(0, ReverseProxyRoutePlanner.MAX_TARGETS_PER_ROUTE)
                .mapToObj(index -> upstream("backend-" + index))
                .toList());
        assertThrows(IllegalStateException.class, () -> routes(tooMany));

        ReverseProxyProperties tooManyRoutes = new ReverseProxyProperties();
        tooManyRoutes.setEnabled(true);
        for (int index = 0; index <= ReverseProxyRoutePlanner.MAX_CONFIGURED_ROUTES; index++) {
            ReverseProxyProperties.Route route = new ReverseProxyProperties.Route();
            route.setPathPrefix("/route-" + index);
            route.setTargets(List.of(upstream("backend-" + index)));
            tooManyRoutes.getRoutes().put("route-" + index, route);
        }
        assertThrows(IllegalStateException.class, () -> routes(tooManyRoutes));
    }

    private static ReverseProxyMetrics configuredMetrics(SimpleMeterRegistry registry, String upstreamId) {
        ReverseProxyMetrics metrics = new ReverseProxyMetrics(registry);
        metrics.activateConfiguration(routes(upstreamId));
        return metrics;
    }

    private static List<ReverseProxyRoutePlanner.ConfiguredRoute> routes(String upstreamId) {
        return routes(properties(upstreamId));
    }

    private static List<ReverseProxyRoutePlanner.ConfiguredRoute> routes(ReverseProxyProperties properties) {
        return ReverseProxyRoutePlanner.buildEnabledRoutes(
                properties, RoutingStrategyRegistry.defaultRegistry());
    }

    private static ReverseProxyProperties properties(String upstreamId) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setUpstreams(List.of(upstream(upstreamId)));
        return properties;
    }

    private static ReverseProxyProperties.Upstream upstream(String id) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId(id);
        upstream.setUrl("http://127.0.0.1:18080");
        return upstream;
    }

    private static Counter counter(SimpleMeterRegistry registry, String name, String... tags) {
        return registry.get(name).tags(tags).counter();
    }

    private static double totalGauge(SimpleMeterRegistry registry, String name) {
        return registry.find(name).gauges().stream().mapToDouble(Gauge::value).sum();
    }

    private static Set<String> tagValues(SimpleMeterRegistry registry, String name, String key) {
        return registry.find(name).meters().stream()
                .map(Meter::getId)
                .map(id -> id.getTag(key))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static boolean hasTagValue(SimpleMeterRegistry registry, String key, String value) {
        return registry.getMeters().stream()
                .map(Meter::getId)
                .anyMatch(id -> value.equals(id.getTag(key)));
    }
}
