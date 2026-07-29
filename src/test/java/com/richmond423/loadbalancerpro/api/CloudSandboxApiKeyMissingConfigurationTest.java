package com.richmond423.loadbalancerpro.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.richmond423.loadbalancerpro.api.config.AuthModeConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CloudSandboxApiKeyMissingConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AuthModeConfiguration.class);

    @Test
    void cloudSandboxProfileRefusesStartupWhenApiKeyIsMissing() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=cloud-sandbox",
                        "loadbalancerpro.auth.mode=api-key")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("loadbalancerpro.auth.mode=api-key")
                            .hasMessageContaining("loadbalancerpro.api.key")
                            .hasMessageContaining("refuses to start");
                });
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
