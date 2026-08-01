package com.richmond423.loadbalancerpro.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RoutingApiLimitsConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RoutingApiLimitsConfiguration.class);

    @Test
    void defaultLimitsBindInsideTheHardCeilings() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            RoutingApiLimitsProperties limits = context.getBean(RoutingApiLimitsProperties.class);
            assertThat(limits.getMaxCandidates()).isEqualTo(32);
            assertThat(limits.getMaxStrategies()).isEqualTo(6);
            assertThat(limits.getMaxDecisionExplorerResponseBytes()).isEqualTo(16L * 1024L * 1024L);
        });
    }

    @Test
    void configuredLowerLimitsBind() {
        contextRunner
                .withPropertyValues(
                        "loadbalancerpro.api.max-candidates=2",
                        "loadbalancerpro.api.max-strategies=2",
                        "loadbalancerpro.api.max-decision-explorer-response-bytes=1024")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    RoutingApiLimitsProperties limits = context.getBean(RoutingApiLimitsProperties.class);
                    assertThat(limits.getMaxCandidates()).isEqualTo(2);
                    assertThat(limits.getMaxStrategies()).isEqualTo(2);
                    assertThat(limits.getMaxDecisionExplorerResponseBytes()).isEqualTo(1024);
                });
    }

    @Test
    void candidateLimitAboveHardCeilingFailsStartup() {
        contextRunner
                .withPropertyValues("loadbalancerpro.api.max-candidates=33")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertHardCeilingFailure(context.getStartupFailure(), "maxCandidates", "32");
                });
    }

    @Test
    void strategyLimitAboveHardCeilingFailsStartup() {
        contextRunner
                .withPropertyValues("loadbalancerpro.api.max-strategies=7")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertHardCeilingFailure(context.getStartupFailure(), "maxStrategies", "6");
                });
    }

    @Test
    void responseLimitAboveHardCeilingFailsStartup() {
        contextRunner
                .withPropertyValues("loadbalancerpro.api.max-decision-explorer-response-bytes=67108865")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertHardCeilingFailure(
                            context.getStartupFailure(),
                            "maxDecisionExplorerResponseBytes",
                            "67108864");
                });
    }

    private static void assertHardCeilingFailure(Throwable startupFailure, String field, String ceiling) {
        Throwable deepestCause = startupFailure;
        while (deepestCause.getCause() != null) {
            deepestCause = deepestCause.getCause();
        }
        assertThat(deepestCause)
                .hasMessageContaining(field)
                .hasMessageContaining(ceiling);
    }
}
