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

class PrivateNetworkProxyProfilePlanDocumentationTest {
    private static final Path PLAN = Path.of("docs/PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path LIVE_PROXY_CONTAINMENT = Path.of("docs/LIVE_PROXY_CONTAINMENT.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");

    private static final Pattern PUBLIC_EXTERNAL_URL =
            Pattern.compile("https?://(?!127\\.0\\.0\\.1(?::|/|$)|localhost(?::|/|$))[^\\s\"'`]+");

    @Test
    void privateNetworkProxyProfilePlanExistsAndIsDesignOnly() throws Exception {
        String plan = read(PLAN);
        String normalized = plan.toLowerCase(Locale.ROOT);

        assertTrue(plan.contains("# Private-Network Proxy Profile Plan"));
        assertTrue(normalized.contains("design-only plan"));
        assertTrue(normalized.contains("does not implement runtime behavior"));
        assertTrue(normalized.contains("does not add private-network live execution"));
        assertTrue(normalized.contains("does not change the current local-only evidence path"));
        assertFalse(PUBLIC_EXTERNAL_URL.matcher(plan).find(), "plan must not introduce public URL targets");
    }

    @Test
    void planDefinesUseCaseNonGoalsAndAllowedHostsModel() throws Exception {
        String plan = read(PLAN);
        String normalized = plan.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("explicit local or private http backends"));
        assertTrue(normalized.contains("operator-provided explicit backend urls only"));
        assertTrue(normalized.contains("loopback"));
        assertTrue(plan.contains("127.0.0.0/8"));
        assertTrue(plan.contains("10.0.0.0/8"));
        assertTrue(plan.contains("172.16.0.0/12"));
        assertTrue(plan.contains("192.168.0.0/16"));
        assertTrue(plan.contains("fc00::/7"));
        assertTrue(normalized.contains("no public internet validation"));
        assertTrue(normalized.contains("no host discovery, dns enumeration, subnet scanning, or port scanning"));
        assertTrue(normalized.contains("must not expand hostnames, cidr ranges, ip ranges, or service names"));
        assertTrue(normalized.contains("unsupported schemes, public addresses, wildcard domains"));
        assertTrue(normalized.contains("fail closed"));
    }

    @Test
    void planPreservesAntivirusSafeContainmentAndReleaseBoundaries() throws Exception {
        String plan = read(PLAN);
        String normalized = plan.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no service installation",
                "scheduled tasks",
                "persistence mechanisms",
                "hidden agents",
                "credential storage",
                "no native executables",
                "installers",
                "wrappers",
                "packers",
                "native-image",
                "launch4j",
                "jpackage",
                "self-extracting archives",
                "downloaded servers",
                "vendored binaries",
                "release-downloads/",
                "ignored `target/` output")) {
            assertTrue(normalized.contains(expected), "plan should preserve boundary: " + expected);
        }

        assertTrue(normalized.contains("no api key, oauth2 token, credential, or secret may be persisted"));
        assertTrue(normalized.contains("browser `localstorage`"));
        assertTrue(normalized.contains("browser `sessionstorage`"));
        assertTrue(normalized.contains("generated evidence"));
        assertFalse(normalized.contains("gh release create"), "plan must not create releases");
        assertFalse(normalized.contains("gh release upload"), "plan must not upload release assets");
        assertFalse(normalized.contains("git tag -"), "plan must not create tags");
    }

    @Test
    void planPreservesSecurityAndFailureBehavior() throws Exception {
        String plan = read(PLAN);
        String normalized = plan.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("prod and cloud-sandbox api-key modes must continue to require `x-api-key`"));
        assertTrue(normalized.contains("oauth2 mode must continue to require the configured allocation role"));
        assertTrue(normalized.contains("tls termination, ingress exposure, rate limits, and identity provider controls"));
        assertTrue(normalized.contains("prod api-key `401`/`200` boundary"));
        assertTrue(normalized.contains("oauth2 `401`/`403`/authorized boundary"));
        assertTrue(normalized.contains("invalid configuration should fail before traffic"));
        assertTrue(normalized.contains("no eligible backend returns controlled `503` behavior"));
        assertTrue(normalized.contains("unreachable selected backends return controlled `502` behavior"));
        assertTrue(normalized.contains("must not fall back to public urls, generated targets, or discovered hosts"));
    }

    @Test
    void planDefinesSafeTestAndRolloutStrategyWithoutPrivateNetworkExecution() throws Exception {
        String plan = read(PLAN);
        String normalized = plan.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("keep ci on loopback-only junit/jdk `httpserver` evidence"));
        assertTrue(normalized.contains("pure java unit tests"));
        assertTrue(normalized.contains("startup/reload validation tests"));
        assertTrue(normalized.contains("dry-run-only private-network profile recipe"));
        assertTrue(normalized.contains("opt-in private-network live smoke only after a separate reviewed task"));
        assertTrue(normalized.contains("no test should scan ports, discover hosts, require public dns"));
        assertTrue(normalized.contains("download servers"));
        assertTrue(normalized.contains("write secrets"));
    }

    @Test
    void reviewerDocsLinkPlanAndDescribeBoundaries() throws Exception {
        String trustMap = read(TRUST_MAP);
        String liveProxyContainment = read(LIVE_PROXY_CONTAINMENT);
        String runbook = read(RUNBOOK);
        String combined = trustMap + "\n" + liveProxyContainment + "\n" + runbook;
        String normalized = combined.toLowerCase(Locale.ROOT);

        assertTrue(trustMap.contains("PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md"));
        assertTrue(liveProxyContainment.contains("PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md"));
        assertTrue(runbook.contains("PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md"));
        assertTrue(normalized.contains("explicit operator-provided backend urls only"));
        assertTrue(normalized.contains("local/private-network allowlisting"));
        assertTrue(normalized.contains("no discovery or scanning"));
        assertTrue(normalized.contains("no secret persistence"));
        assertTrue(normalized.contains("no private-network live execution until separately approved"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
