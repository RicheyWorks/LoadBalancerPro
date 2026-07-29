package com.richmond423.loadbalancerpro.core;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.richmond423.loadbalancerpro.api.AllocatorController;
import com.richmond423.loadbalancerpro.api.AllocatorService;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public final class ServerMonitorShutdownHookStressProbe {
    private static final int ALLOCATION_COUNT = 10_000;
    private static final String REQUEST_JSON = """
            {
              "requestedLoad": 25.0,
              "servers": [
                {
                  "id": "bounded-heap-server",
                  "cpuUsage": 10.0,
                  "memoryUsage": 20.0,
                  "diskUsage": 30.0,
                  "capacity": 100.0,
                  "weight": 1.0,
                  "healthy": true
                }
              ]
            }
            """;

    private ServerMonitorShutdownHookStressProbe() {
    }

    public static void main(String[] args) throws Exception {
        AllocatorService allocatorService = new AllocatorService(new MockEnvironment()
                .withProperty("loadbalancerpro.lase.shadow.enabled", "false"));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AllocatorController(allocatorService, "shutdown-hook-stress"))
                .build();

        postCapacityAware(mockMvc);
        Set<Thread> hooksBefore = snapshotShutdownHooks();
        Set<Thread> addedHooks = Set.of();
        try {
            for (int allocation = 0; allocation < ALLOCATION_COUNT; allocation++) {
                postCapacityAware(mockMvc);
            }
            Set<Thread> hooksAfter = snapshotShutdownHooks();
            addedHooks = new HashSet<>(hooksAfter);
            addedHooks.removeAll(hooksBefore);
        } finally {
            for (Thread addedHook : addedHooks) {
                Runtime.getRuntime().removeShutdownHook(addedHook);
            }
        }

        if (!addedHooks.isEmpty()) {
            throw new AssertionError("P-0.1 leaked shutdown hooks: " + addedHooks.size());
        }

        System.out.println("P-0.1 bounded-heap allocation probe passed: " + ALLOCATION_COUNT);
    }

    private static void postCapacityAware(MockMvc mockMvc) throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/allocate/capacity-aware")
                        .contentType("application/json")
                        .content(REQUEST_JSON))
                .andReturn()
                .getResponse()
                .getStatus();
        if (status != 200) {
            throw new AssertionError("P-0.1 allocation POST returned HTTP " + status);
        }
    }

    private static Set<Thread> snapshotShutdownHooks() throws Exception {
        Class<?> applicationShutdownHooks = Class.forName("java.lang.ApplicationShutdownHooks");
        Field hooksField = applicationShutdownHooks.getDeclaredField("hooks");
        hooksField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Thread, Thread> hooks = (Map<Thread, Thread>) hooksField.get(null);
        return hooks == null ? Set.of() : new HashSet<>(hooks.keySet());
    }
}
