package com.richmond423.loadbalancerpro.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class AuthModeConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AuthModeConfiguration.class);

    @Test
    void defaultApiKeyModeFailsStartupWhenApiKeyIsMissing() {
        contextRunner.run(context -> assertMissingApiKeyFailure(context.getStartupFailure()));
    }

    @Test
    void apiKeyModeFailsStartupWhenApiKeyIsBlank() {
        contextRunner.withPropertyValues(
                        "loadbalancerpro.auth.mode=api-key",
                        "loadbalancerpro.api.key= ")
                .run(context -> assertMissingApiKeyFailure(context.getStartupFailure()));
    }

    @Test
    void apiKeyModeStartsWhenApiKeyIsConfigured() {
        contextRunner.withPropertyValues(
                        "loadbalancerpro.auth.mode=api-key",
                        "loadbalancerpro.api.key=LOCAL_TEST_API_KEY")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void noneModeStartsAndLogsProminentWarning(CapturedOutput output) {
        contextRunner.withPropertyValues("loadbalancerpro.auth.mode=none")
                .run(context -> assertThat(context).hasNotFailed());

        assertThat(output)
                .contains("SECURITY WARNING")
                .contains("loadbalancerpro.auth.mode=none")
                .contains("authentication is disabled");
    }

    @Test
    void oauth2ModeFailsStartupWhenIssuerAndJwkConfigurationAreBlank() {
        contextRunner.withPropertyValues(
                        "loadbalancerpro.auth.mode=oauth2",
                        "loadbalancerpro.auth.oauth2.issuer-uri= ",
                        "loadbalancerpro.auth.oauth2.jwk-set-uri=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable rootCause = rootCause(context.getStartupFailure());
                    assertThat(rootCause)
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("loadbalancerpro.auth.oauth2.issuer-uri")
                            .hasMessageContaining("loadbalancerpro.auth.oauth2.jwk-set-uri");
                });
    }

    private static void assertMissingApiKeyFailure(Throwable startupFailure) {
        assertThat(startupFailure).isNotNull();
        assertThat(rootCause(startupFailure))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("loadbalancerpro.auth.mode=api-key")
                .hasMessageContaining("loadbalancerpro.api.key")
                .hasMessageContaining("refuses to start");
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
