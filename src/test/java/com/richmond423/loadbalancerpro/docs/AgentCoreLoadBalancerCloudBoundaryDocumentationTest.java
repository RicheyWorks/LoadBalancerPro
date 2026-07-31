package com.richmond423.loadbalancerpro.docs;

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

class AgentCoreLoadBalancerCloudBoundaryDocumentationTest {
    private static final Path CONTRACT = Path.of("docs/agent/CORE_LOADBALANCER_FEATURE_CONTRACT.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentCoreLoadBalancerCloudBoundaryDocumentationTest.java");
    private static final Pattern CREDENTIAL_ASSIGNMENT = Pattern.compile(
            "(?i)(access[-_ ]?key|secret[-_ ]?key|token|password)\\s*[:=]\\s*[^\\s`\"']+");

    @Test
    void contractDocumentsCloudShimBoundaryWithoutAddingCloudProof() throws IOException {
        String contract = read(CONTRACT);
        String normalized = contract.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "cloud shim boundary",
                "loadbalancer.initializecloud",
                "guarded api surface",
                "optional/nullable compatibility",
                "getcloudmanageroptional()",
                "hascloudmanager()",
                "documentation/test-only")) {
            assertTrue(normalized.contains(expected), "cloud boundary docs should include " + expected);
        }
    }

    @Test
    void contractPreservesCloudNotProvenBoundaries() throws IOException {
        String normalized = read(CONTRACT).toLowerCase(Locale.ROOT);

        for (String boundary : List.of(
                "no core-lb slot proves live-cloud validation",
                "no core-lb slot proves real-tenant validation",
                "no core-lb slot adds production cloud readiness",
                "no live-cloud validation",
                "no real-tenant validation",
                "no production readiness",
                "no production certification")) {
            assertTrue(normalized.contains(boundary), "missing cloud boundary " + boundary);
        }
    }

    @Test
    void contractDoesNotIntroduceCloudOrCredentialOverclaims() throws IOException {
        String normalized = read(CONTRACT).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "live-cloud validated",
                "real tenant validated",
                "cloud production ready",
                "production cloud ready",
                "certified cloud",
                "benchmark proven")) {
            assertFalse(normalized.contains(forbidden), "contract must not overclaim: " + forbidden);
        }

        assertFalse(CREDENTIAL_ASSIGNMENT.matcher(read(CONTRACT)).find(),
                "cloud boundary docs must not contain credential-looking assignments");
    }

    @Test
    void guardTestOnlyReadsTrackedFiles() throws IOException {
        String source = read(SOURCE);

        for (String forbidden : List.of(
                "Files." + "write",
                "Files." + "create",
                "Files." + "delete",
                "Process" + "Builder",
                "Runtime." + "getRuntime",
                ".ex" + "ec(",
                "Http" + "Client",
                "URL" + "Connection",
                "Socket" + "(")) {
            assertFalse(source.contains(forbidden), "guard test must not use " + forbidden);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
