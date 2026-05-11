package com.richmond423.loadbalancerpro.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.richmond423.loadbalancerpro.api.proxy.ReverseProxyConfiguration;
import com.richmond423.loadbalancerpro.api.proxy.ReverseProxyMetrics;
import com.richmond423.loadbalancerpro.api.proxy.ReverseProxyProperties;
import com.richmond423.loadbalancerpro.api.proxy.ReverseProxyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class EnterpriseProxyConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ReverseProxyConfiguration.class,
                    ReverseProxyMetrics.class,
                    ReverseProxyService.class);

    @Test
    void proxyRemainsDisabledByDefaultWithoutRoutesOrTargets() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ReverseProxyService.class);
            assertThat(context.getBean(ReverseProxyProperties.class).isEnabled()).isFalse();
        });
    }

    @Test
    void validOperatorRouteConfigBindsAndStartsProxyService() {
        contextRunner.withPropertyValues(validOperatorRouteProperties())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ReverseProxyService.class);
                    ReverseProxyProperties properties = context.getBean(ReverseProxyProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getRoutes()).containsKey("api");
                    assertThat(properties.getRoutes().get("api").getPathPrefix()).isEqualTo("/api");
                    assertThat(properties.getRoutes().get("api").getTargets()).hasSize(2);
                });
    }

    @Test
    void enabledProxyWithNoRoutesOrLegacyUpstreamsFailsClearly() {
        contextRunner.withPropertyValues("loadbalancerpro.proxy.enabled=true")
                .run(context -> assertStartupFailureContains(context.getStartupFailure(),
                        "requires at least one configured route or upstream target"));
    }

    @Test
    void routeWithNoTargetsFailsClearly() {
        contextRunner.withPropertyValues(
                        "loadbalancerpro.proxy.enabled=true",
                        "loadbalancerpro.proxy.routes.api.path-prefix=/api",
                        "loadbalancerpro.proxy.routes.api.strategy=ROUND_ROBIN")
                .run(context -> assertStartupFailureContains(context.getStartupFailure(),
                        "loadbalancerpro.proxy.routes.api.targets must contain at least one target"));
    }

    @Test
    void blankTargetIdFailsClearly() {
        contextRunner.withPropertyValues(
                        "loadbalancerpro.proxy.enabled=true",
                        "loadbalancerpro.proxy.routes.api.path-prefix=/api",
                        "loadbalancerpro.proxy.routes.api.strategy=ROUND_ROBIN",
                        "loadbalancerpro.proxy.routes.api.targets[0].id= ",
                        "loadbalancerpro.proxy.routes.api.targets[0].url=http://127.0.0.1:18081",
                        "loadbalancerpro.proxy.routes.api.targets[0].weight=1")
                .run(context -> assertStartupFailureContains(context.getStartupFailure(),
                        "loadbalancerpro.proxy.routes.api.targets[0].id must not be blank"));
    }

    @Test
    void malformedTargetUriFailsClearly() {
        contextRunner.withPropertyValues(
                        "loadbalancerpro.proxy.enabled=true",
                        "loadbalancerpro.proxy.routes.api.path-prefix=/api",
                        "loadbalancerpro.proxy.routes.api.targets[0].id=local-a",
                        "loadbalancerpro.proxy.routes.api.targets[0].url=http://[bad",
                        "loadbalancerpro.proxy.routes.api.targets[0].weight=1")
                .run(context -> assertStartupFailureContains(context.getStartupFailure(),
                        "loadbalancerpro.proxy.routes.api.targets[0].url must be a valid http/https URI"));
    }

    @Test
    void unsupportedTargetSchemeFailsClearly() {
        contextRunner.withPropertyValues(
                        "loadbalancerpro.proxy.enabled=true",
                        "loadbalancerpro.proxy.routes.api.path-prefix=/api",
                        "loadbalancerpro.proxy.routes.api.targets[0].id=local-a",
                        "loadbalancerpro.proxy.routes.api.targets[0].url=ftp://127.0.0.1:18081",
                        "loadbalancerpro.proxy.routes.api.targets[0].weight=1")
                .run(context -> assertStartupFailureContains(context.getStartupFailure(),
                        "loadbalancerpro.proxy.routes.api.targets[0].url must use http or https"));
    }

    @Test
    void invalidWeightFailsClearly() {
        contextRunner.withPropertyValues(
                        "loadbalancerpro.proxy.enabled=true",
                        "loadbalancerpro.proxy.routes.api.path-prefix=/api",
                        "loadbalancerpro.proxy.routes.api.targets[0].id=local-a",
                        "loadbalancerpro.proxy.routes.api.targets[0].url=http://127.0.0.1:18081",
                        "loadbalancerpro.proxy.routes.api.targets[0].weight=0")
                .run(context -> assertStartupFailureContains(context.getStartupFailure(),
                        "loadbalancerpro.proxy.routes.api.targets[0].weight must be finite and greater than 0"));
    }

    @Test
    void invalidRouteNameFailsClearly() {
        contextRunner.withPropertyValues(
                        "loadbalancerpro.proxy.enabled=true",
                        "loadbalancerpro.proxy.routes._bad.path-prefix=/api",
                        "loadbalancerpro.proxy.routes._bad.targets[0].id=local-a",
                        "loadbalancerpro.proxy.routes._bad.targets[0].url=http://127.0.0.1:18081",
                        "loadbalancerpro.proxy.routes._bad.targets[0].weight=1")
                .run(context -> assertStartupFailureContains(context.getStartupFailure(),
                        "loadbalancerpro.proxy.routes route names must match"));
    }

    private static String[] validOperatorRouteProperties() {
        return new String[] {
                "loadbalancerpro.proxy.enabled=true",
                "loadbalancerpro.proxy.routes.api.path-prefix=/api",
                "loadbalancerpro.proxy.routes.api.strategy=ROUND_ROBIN",
                "loadbalancerpro.proxy.routes.api.targets[0].id=local-a",
                "loadbalancerpro.proxy.routes.api.targets[0].url=http://127.0.0.1:18081",
                "loadbalancerpro.proxy.routes.api.targets[0].weight=1",
                "loadbalancerpro.proxy.routes.api.targets[1].id=local-b",
                "loadbalancerpro.proxy.routes.api.targets[1].url=http://127.0.0.1:18082",
                "loadbalancerpro.proxy.routes.api.targets[1].weight=1"
        };
    }

    private static void assertStartupFailureContains(Throwable startupFailure, String expectedMessage) {
        assertThat(startupFailure).isNotNull();
        boolean foundIllegalStateException = false;
        boolean foundExpectedMessage = false;
        Throwable cause = startupFailure;
        while (cause != null) {
            foundIllegalStateException = foundIllegalStateException || cause instanceof IllegalStateException;
            foundExpectedMessage = foundExpectedMessage
                    || (cause.getMessage() != null && cause.getMessage().contains(expectedMessage));
            cause = cause.getCause();
        }
        assertThat(foundIllegalStateException).as("startup failure should include IllegalStateException").isTrue();
        assertThat(foundExpectedMessage)
                .as("startup failure should mention " + expectedMessage)
                .isTrue();
    }
}
