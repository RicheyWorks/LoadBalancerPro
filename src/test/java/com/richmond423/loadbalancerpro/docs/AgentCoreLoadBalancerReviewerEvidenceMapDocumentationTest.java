package com.richmond423.loadbalancerpro.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class AgentCoreLoadBalancerReviewerEvidenceMapDocumentationTest {
    private static final Path CONTRACT = Path.of("docs/agent/CORE_LOADBALANCER_FEATURE_CONTRACT.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentCoreLoadBalancerReviewerEvidenceMapDocumentationTest.java");

    @Test
    void coreContractContainsReviewerEvidenceMapForCompletedCoreLbEvidence() throws IOException {
        String contract = read(CONTRACT);
        String normalized = contract.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("## reviewer evidence map"),
                "core contract should expose a reviewer evidence map");
        for (String goal : List.of(
                "Core-LB-G01",
                "Core-LB-G02",
                "Core-LB-G03",
                "Core-LB-G04",
                "Core-LB-G05",
                "Core-LB-G06",
                "Core-LB-G07",
                "Core-LB-G08")) {
            assertTrue(contract.contains(goal), "reviewer evidence map should name " + goal);
        }

        for (String evidence : List.of(
                "CoreLoadBalancerCrossStrategyEdgeInvariantTest",
                "CoreLoadBalancerCapacityPredictiveOverloadTest",
                "CoreLoadBalancerDeterministicOrderingTieTest",
                "CoreLoadBalancerLeastLoadedSemanticsTest",
                "CoreRoutingRegistryComparisonContractTest",
                "CoreLoadBalancerServerLifecycleInvariantTest",
                "CoreLoadBalancerOverloadRecoveryScenarioTest")) {
            assertTrue(contract.contains(evidence), "reviewer evidence map should cite " + evidence);
        }
    }

    @Test
    void reviewerTrustMapPointsReviewersToCoreEvidenceMapAndBoundaries() throws IOException {
        String trustMap = read(TRUST_MAP);
        String normalized = trustMap.toLowerCase(Locale.ROOT);

        assertTrue(trustMap.contains("agent/CORE_LOADBALANCER_FEATURE_CONTRACT.md"),
                "trust map should link to the core feature contract");
        assertTrue(trustMap.contains("Core-LB-G01 through Core-LB-G08"),
                "trust map should summarize completed core evidence coverage");
        for (String expected : List.of(
                "edge invariants",
                "capacity/predictive overload behavior",
                "deterministic ordering",
                "least-loaded semantics",
                "registry/comparison shape",
                "lifecycle behavior",
                "overload/recovery scenarios")) {
            assertTrue(normalized.contains(expected), "trust map should name reviewer path " + expected);
        }

        for (String boundary : List.of(
                "does not change production behavior",
                "production readiness",
                "certification",
                "live-cloud validation",
                "real-tenant validation",
                "benchmark/load/stress evidence",
                "throughput/p95/p99 evidence",
                "replay/export proof",
                "broader automation")) {
            assertTrue(normalized.contains(boundary), "trust map should preserve boundary " + boundary);
        }
    }

    @Test
    void evidenceMapAvoidsUnsupportedOverclaims() throws IOException {
        String combined = (read(CONTRACT) + "\n" + read(TRUST_MAP)).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "production ready",
                "is certified",
                "certified for production",
                "guaranteed p99",
                "real tenant validated",
                "live-cloud validated",
                "benchmark proven")) {
            assertFalse(combined.contains(forbidden), "reviewer evidence docs must not overclaim: " + forbidden);
        }
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
