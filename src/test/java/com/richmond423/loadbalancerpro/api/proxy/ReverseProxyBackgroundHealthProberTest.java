package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class ReverseProxyBackgroundHealthProberTest {

    @Test
    void probeIoUsesDaemonWorkersAndRequestStatusReadsDoNotProbeOrRecountFailures() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<Void> probeResponse = mock(HttpResponse.class);
        when(probeResponse.statusCode()).thenReturn(503);
        HttpResponse<byte[]> forwardResponse = mock(HttpResponse.class);
        when(forwardResponse.statusCode()).thenReturn(200);
        when(forwardResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
        when(forwardResponse.body()).thenReturn(new byte[0]);
        AtomicInteger probeCalls = new AtomicInteger();
        AtomicInteger forwardCalls = new AtomicInteger();
        ConcurrentLinkedQueue<String> probeThreads = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Boolean> probeThreadDaemonFlags = new ConcurrentLinkedQueue<>();
        when(httpClient.send(any(), any())).thenAnswer(invocation -> {
            HttpRequest outbound = invocation.getArgument(0);
            if ("/health".equals(outbound.uri().getPath())) {
                probeCalls.incrementAndGet();
                probeThreads.add(Thread.currentThread().getName());
                probeThreadDaemonFlags.add(Thread.currentThread().isDaemon());
                return probeResponse;
            }
            forwardCalls.incrementAndGet();
            return forwardResponse;
        });
        ReverseProxyService service = new ReverseProxyService(
                healthCheckedProperties(),
                httpClient,
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.systemUTC());

        await(() -> service.statusSnapshot().upstreams().get(0).consecutiveFailures() > 0,
                Duration.ofSeconds(2));
        service.closeHealthProber();
        int completedProbeCalls = probeCalls.get();
        int recordedFailures = service.statusSnapshot().upstreams().get(0).consecutiveFailures();

        for (int read = 0; read < 10; read++) {
            assertEquals(recordedFailures,
                    service.statusSnapshot().upstreams().get(0).consecutiveFailures());
        }
        service.forward(request(), new byte[0]);

        assertEquals(completedProbeCalls, probeCalls.get(),
                "request and status paths must not perform health-probe I/O");
        assertEquals(1, forwardCalls.get());
        assertTrue(probeThreads.stream()
                        .allMatch(name -> name.startsWith(UpstreamHealthProber.THREAD_NAME_PREFIX)),
                () -> "probe I/O escaped dedicated workers: " + probeThreads);
        assertTrue(probeThreadDaemonFlags.stream().allMatch(Boolean::booleanValue),
                () -> "probe I/O used a non-daemon worker: " + probeThreadDaemonFlags);
    }

    private static ReverseProxyProperties healthCheckedProperties() {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        ReverseProxyProperties.HealthCheck healthCheck = new ReverseProxyProperties.HealthCheck();
        healthCheck.setEnabled(true);
        healthCheck.setPath("/health");
        healthCheck.setInterval(Duration.ofMillis(20));
        healthCheck.setTimeout(Duration.ofSeconds(1));
        healthCheck.setHealthyThreshold(2);
        healthCheck.setUnhealthyThreshold(3);
        properties.setHealthCheck(healthCheck);
        ReverseProxyProperties.Cooldown cooldown = new ReverseProxyProperties.Cooldown();
        cooldown.setEnabled(true);
        cooldown.setConsecutiveFailureThreshold(100);
        properties.setCooldown(cooldown);
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId("backend");
        upstream.setUrl("http://127.0.0.1:18081");
        properties.setUpstreams(List.of(upstream));
        return properties;
    }

    private static HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/proxy/live");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getQueryString()).thenReturn(null);
        return request;
    }

    private static void await(BooleanSupplier condition, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertTrue(condition.getAsBoolean(), "condition did not converge within " + timeout);
    }
}
