package com.richmond423.loadbalancerpro.api;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentTargetCatalog;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackTarget;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=prod",
        "loadbalancerpro.api.key=ALLOCATION_SUPERVISION_TEST_KEY"
})
@AutoConfigureMockMvc
@Import(EnterpriseLabAllocationSupervisionSpringIntegrationTest.FixedTargets.class)
class EnterpriseLabAllocationSupervisionSpringIntegrationTest {
    private static final Path DATA_ROOT = createDataRoot();

    @DynamicPropertySource
    static void durableRoot(DynamicPropertyRegistry registry) {
        registry.add(
                "loadbalancer.enterprise-lab.experiment-journal-data-directory",
                () -> DATA_ROOT.toString());
    }

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @Test
    void springStartupPublishesAuthenticatedBaselineReadyStatusAndBoundedActions()
            throws Exception {
        mockMvc.perform(get("/api/lab/allocation-supervision")
                        .header("X-API-Key", "ALLOCATION_SUPERVISION_TEST_KEY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured", is(true)))
                .andExpect(jsonPath("$.ready", is(true)))
                .andExpect(jsonPath("$.readinessState", is("READY")))
                .andExpect(jsonPath("$.installed.kind", is("BASELINE")))
                .andExpect(jsonPath("$.fingerprints.baselineMatchesInstalled", is(true)))
                .andExpect(jsonPath("$.independentSupervisor.configuredMode", is("in-process")))
                .andExpect(jsonPath("$.independentSupervisor.externalSupervisorRequired",
                        is(false)))
                .andExpect(jsonPath("$.independentSupervisor.supervisorInstanceId").doesNotExist())
                .andExpect(content().string(not(containsString(DATA_ROOT.toString()))))
                .andExpect(content().string(not(containsString("127.0.0.1"))));

        mockMvc.perform(post("/api/lab/allocation-supervision/verify")
                        .header("X-API-Key", "ALLOCATION_SUPERVISION_TEST_KEY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready", is(true)));

        mockMvc.perform(post("/api/lab/allocation-supervision/restore-safe-baseline")
                        .header("X-API-Key", "ALLOCATION_SUPERVISION_TEST_KEY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restorationStatus", is("NO_CHANGE")))
                .andExpect(jsonPath("$.status.ready", is(true)));
    }

    private static Path createDataRoot() {
        try {
            return Files.createTempDirectory("loadbalancerpro-allocation-supervision-")
                    .toAbsolutePath().normalize();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedTargets {
        @Bean
        @Primary
        EnterpriseLabExperimentTargetCatalog allocationProofTargets() {
            return new EnterpriseLabExperimentTargetCatalog(List.of(
                    new EnterpriseLabLoopbackTarget(
                            "tail-latency-pressure", "blue",
                            URI.create("http://127.0.0.1:1/supervision-test")),
                    new EnterpriseLabLoopbackTarget(
                            "tail-latency-pressure", "green",
                            URI.create("http://127.0.0.1:1/supervision-test")),
                    new EnterpriseLabLoopbackTarget(
                            "tail-latency-pressure", "orange",
                            URI.create("http://127.0.0.1:1/supervision-test"))));
        }
    }
}
