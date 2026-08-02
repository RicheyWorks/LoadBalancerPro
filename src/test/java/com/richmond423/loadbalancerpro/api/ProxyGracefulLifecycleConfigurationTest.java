package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;

import com.richmond423.loadbalancerpro.api.proxy.ReverseProxyService;
import org.junit.jupiter.api.Test;
import org.springframework.context.SmartLifecycle;

class ProxyGracefulLifecycleConfigurationTest {
    private static final Path RESOURCES = Path.of("src/main/resources");

    @Test
    void gracefulShutdownAndBoundedReloadDrainDefaultsApplyToEveryProfile() throws Exception {
        Properties defaults = properties(RESOURCES.resolve("application.properties"));
        assertEquals("graceful", defaults.getProperty("server.shutdown"));
        assertEquals("30s", defaults.getProperty("spring.lifecycle.timeout-per-shutdown-phase"));
        assertEquals("30s", defaults.getProperty("loadbalancerpro.proxy.reload.drain-timeout"));

        try (Stream<Path> profiles = Files.list(RESOURCES)) {
            profiles.filter(path -> path.getFileName().toString().matches("application-.+\\.properties"))
                    .forEach(path -> {
                        Properties profile = propertiesUnchecked(path);
                        assertFalse(profile.containsKey("server.shutdown"),
                                path + " must inherit graceful shutdown");
                        assertFalse(profile.containsKey("spring.lifecycle.timeout-per-shutdown-phase"),
                                path + " must inherit the bounded lifecycle timeout");
                    });
        }
    }

    @Test
    void proxyAndShadowWorkersParticipateInSpringLifecyclePhases() throws Exception {
        assertTrue(SmartLifecycle.class.isAssignableFrom(ReverseProxyService.class));
        assertTrue(SmartLifecycle.class.isAssignableFrom(LaseShadowRuntime.class));
        assertTrue(SmartLifecycle.class.isAssignableFrom(Class.forName(
                "com.richmond423.loadbalancerpro.api.proxy.UpstreamHealthProber")));
    }

    private static Properties properties(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        return properties;
    }

    private static Properties propertiesUnchecked(Path path) {
        try {
            return properties(path);
        } catch (IOException exception) {
            throw new AssertionError("could not read " + path, exception);
        }
    }
}
