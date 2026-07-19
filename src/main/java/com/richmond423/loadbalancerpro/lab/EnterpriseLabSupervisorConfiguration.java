package com.richmond423.loadbalancerpro.lab;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Repository-owned constants for the bounded single-host supervisor. */
public final class EnterpriseLabSupervisorConfiguration {
    public static final String SCENARIO_ID = "tail-latency-pressure";
    public static final int MAX_CONCURRENT_CONNECTIONS = 4;
    public static final int MAX_QUEUED_CONNECTIONS = 8;
    public static final int MAX_REQUESTS_PER_PROCESS = 4_096;
    public static final int MAX_TRANSACTION_HISTORY = 1;
    public static final Duration CONNECTION_IDLE_TIMEOUT = Duration.ofSeconds(3);
    public static final Duration MAX_CONNECTION_LIFETIME = Duration.ofSeconds(10);
    public static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration MAX_REQUEST_AGE = Duration.ofSeconds(30);
    public static final Duration MAX_REQUEST_CLOCK_SKEW = Duration.ofSeconds(5);
    public static final int MIN_CONFIGURED_PORT = 1_024;
    public static final int MAX_CONFIGURED_PORT = 65_535;

    private EnterpriseLabSupervisorConfiguration() {
    }

    public static EnterpriseLabExperimentTargetCatalog approvedTargets() {
        return new EnterpriseLabExperimentTargetCatalog(List.of(
                target("blue"),
                target("green"),
                target("orange")));
    }

    public static Map<String, Double> safeBaselineAllocation() {
        return Map.of(
                "blue", 0.34d,
                "green", 0.33d,
                "orange", 0.33d);
    }

    /** Creates 127.0.0.1 without hostname lookup. */
    public static InetAddress literalLoopbackAddress() {
        try {
            return InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        } catch (UnknownHostException impossible) {
            throw new IllegalStateException("literal IPv4 loopback is unavailable", impossible);
        }
    }

    public static int requireConfiguredPort(int value) {
        if (value == 0 || (value >= MIN_CONFIGURED_PORT && value <= MAX_CONFIGURED_PORT)) {
            return value;
        }
        throw new IllegalArgumentException(
                "supervisor port must be zero for ephemeral binding or between 1024 and 65535");
    }

    private static EnterpriseLabLoopbackTarget target(String backendId) {
        return new EnterpriseLabLoopbackTarget(
                SCENARIO_ID,
                backendId,
                URI.create("http://127.0.0.1:1/supervisor-approved-target"));
    }
}
