package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class EnterpriseProxyFoundationSafetyTest {
    private static final Path ROUTE_EXAMPLE =
            Path.of("docs/examples/proxy/application-proxy-operator-routes-example.properties");
    private static final List<Path> SPRINT_DOCS = List.of(
            Path.of("docs/API_CONTRACTS.md"),
            Path.of("docs/REVERSE_PROXY_MODE.md"),
            Path.of("docs/REAL_BACKEND_PROXY_EXAMPLES.md"),
            Path.of("docs/REVIEWER_TRUST_MAP.md"),
            Path.of("docs/OPERATIONS_RUNBOOK.md"),
            ROUTE_EXAMPLE);
    private static final List<Path> SPRINT_CODE = List.of(
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyProperties.java"),
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyRoutePlanner.java"),
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyService.java"),
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyStatusResponse.java"),
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyStatusController.java"));
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+CloudManager\\s*\\(|CloudManager\\s*\\(");
    private static final Pattern RELEASE_COMMAND =
            Pattern.compile("(?im)^\\s*(gh\\s+release|git\\s+tag)\\b");

    @Test
    void operatorRouteExampleDocumentsExplicitEnableAndLoopbackTargets() throws Exception {
        String example = read(ROUTE_EXAMPLE);

        assertTrue(example.contains("loadbalancerpro.proxy.enabled=true"));
        assertTrue(example.contains("loadbalancerpro.proxy.routes.api.path-prefix=/api"));
        assertTrue(example.contains("loadbalancerpro.proxy.routes.api.strategy=ROUND_ROBIN"));
        assertTrue(example.contains("loadbalancerpro.proxy.routes.api.targets[0].url=http://localhost:9001"));
        assertTrue(example.contains("loadbalancerpro.proxy.routes.api.targets[1].url=http://localhost:9002"));
    }

    @Test
    void sprintDocsAvoidUnsafeClaimsAndReleaseCommands() throws Exception {
        for (Path doc : SPRINT_DOCS) {
            String content = read(doc);
            String normalized = content.toLowerCase(Locale.ROOT);

            assertFalse(normalized.contains("production-grade"), doc + " should not add production-grade wording");
            assertFalse(normalized.contains("benchmark result"), doc + " should not add benchmark claims");
            assertFalse(normalized.contains("certification proof"), doc + " should not add certification claims");
            assertFalse(normalized.contains("certified gateway"), doc + " should not add certification claims");
            assertFalse(RELEASE_COMMAND.matcher(content).find(), doc + " should not instruct release/tag creation");
        }
    }

    @Test
    void sprintCodeAndDocsDoNotConstructCloudManager() throws Exception {
        for (Path path : concat(SPRINT_CODE, SPRINT_DOCS)) {
            assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(read(path)).find(),
                    path + " must not construct CloudManager");
        }
    }

    private static List<Path> concat(List<Path> first, List<Path> second) {
        return java.util.stream.Stream.concat(first.stream(), second.stream()).toList();
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
