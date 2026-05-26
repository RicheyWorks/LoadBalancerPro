package com.richmond423.loadbalancerpro.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class AgentCoreLoadBalancerFeatureContractDocumentationTest {
    private static final Path CONTRACT = Path.of("docs/agent/CORE_LOADBALANCER_FEATURE_CONTRACT.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentCoreLoadBalancerFeatureContractDocumentationTest.java");

    @Test
    void coreLoadBalancerContractExistsAndDefinesCampaignScope() throws IOException {
        String normalized = read(CONTRACT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "core loadbalancer reliability contract campaign",
                "warn / core-feature-audit",
                "core-lb-g01",
                "documentation/test-only",
                "does not add production code",
                "loadbalancer",
                "loaddistributionplanner",
                "routingstrategyregistry")) {
            assertTrue(normalized.contains(expected), "contract should define " + expected);
        }
    }

    @Test
    void contractNamesRequiredStrategyInvariants() throws IOException {
        String normalized = read(CONTRACT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "round robin allocation facade",
                "least-loaded allocation facade",
                "weighted distribution facade",
                "consistent hashing facade",
                "capacity-aware allocation",
                "predictive allocation",
                "routing strategy registry",
                "request-level routing strategies")) {
            assertTrue(normalized.contains(expected), "contract should name strategy invariant " + expected);
        }
    }

    @Test
    void contractCoversRequiredAuditTopics() throws IOException {
        String normalized = read(CONTRACT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "empty server set behavior",
                "all-unhealthy behavior",
                "unhealthy server exclusion",
                "recovered server re-entry",
                "duplicate server replacement",
                "server removal and accumulated-load reconciliation",
                "zero load and negative load handling",
                "zero weight and all-zero weight behavior",
                "capacity exhaustion and unallocated load",
                "predictive overload behavior",
                "deterministic ordering and tie handling",
                "explanation boundaries")) {
            assertTrue(normalized.contains(expected), "contract should cover audit topic " + expected);
        }
    }

    @Test
    void contractListsCurrentEvidenceAndFollowUpGoals() throws IOException {
        String content = read(CONTRACT);
        String normalized = content.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "LoadBalancerTest",
                "CoreRoutingDecisionIntegrationTest",
                "ServerTelemetryRoutingTest",
                "RoutingComparisonEngineTest",
                "TailLatencyPowerOfTwoHysteresisTest",
                "WeightedLeastLoadStrategyTest",
                "WeightedLeastConnectionsRoutingStrategyTest",
                "WeightedRoundRobinRoutingStrategyTest")) {
            assertTrue(content.contains(expected), "contract should cite evidence " + expected);
        }

        Matcher matcher = Pattern.compile("Core-LB-G\\d{2}").matcher(content);
        Set<String> goalIds = new java.util.TreeSet<>();
        while (matcher.find()) {
            goalIds.add(matcher.group());
        }
        assertTrue(goalIds.size() >= 10, "contract should define at least ten Core-LB follow-up goals");

        for (String expected : List.of(
                "cross-strategy edge invariant tests",
                "capacity-aware and predictive overload hardening",
                "deterministic ordering and tie behavior",
                "least-loaded semantics decision",
                "routing registry and comparison contract",
                "server lifecycle invariants",
                "overload and recovery scenario tests",
                "reviewer evidence map update",
                "core load-balancer evidence consolidation")) {
            assertTrue(normalized.contains(expected), "contract should include follow-up " + expected);
        }
    }

    @Test
    void contractPreservesNotProvenBoundariesAndAvoidsOverclaims() throws IOException {
        String normalized = read(CONTRACT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no production readiness",
                "no production certification",
                "no live-cloud validation",
                "no real-tenant validation",
                "no runtime enforcement",
                "no load/stress/benchmarking evidence",
                "no throughput/p95/p99 production evidence",
                "no real-world latency improvement",
                "no replay/evidence/report/storage/export proof",
                "no broader automation")) {
            assertTrue(normalized.contains(expected), "contract should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "production ready",
                "certified",
                "guaranteed p99",
                "real tenant validated",
                "live-cloud validated",
                "benchmark proven")) {
            assertFalse(normalized.contains(forbidden), "contract must not overclaim: " + forbidden);
        }
    }

    @Test
    void readmeAndReviewerTrustMapLinkToCoreLoadBalancerContract() throws IOException {
        assertTrue(read(README).contains("docs/agent/CORE_LOADBALANCER_FEATURE_CONTRACT.md"),
                "README should link to the core load-balancer feature contract");
        assertTrue(read(TRUST_MAP).contains("agent/CORE_LOADBALANCER_FEATURE_CONTRACT.md"),
                "Reviewer Trust Map should link to the core load-balancer feature contract");
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
