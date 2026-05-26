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

class AgentCoreLoadBalancerEvidenceConsolidationDocumentationTest {
    private static final Path CONSOLIDATION = Path.of("docs/agent/CORE_LOADBALANCER_EVIDENCE_CONSOLIDATION.md");
    private static final Path CONTRACT = Path.of("docs/agent/CORE_LOADBALANCER_FEATURE_CONTRACT.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentCoreLoadBalancerEvidenceConsolidationDocumentationTest.java");

    @Test
    void consolidationNamesCoreLbGoalsAndEvidenceRecords() throws IOException {
        String consolidation = read(CONSOLIDATION);

        for (String goal : List.of(
                "Core-LB-G01",
                "Core-LB-G02",
                "Core-LB-G03",
                "Core-LB-G04",
                "Core-LB-G05",
                "Core-LB-G06",
                "Core-LB-G07",
                "Core-LB-G08",
                "Core-LB-G09",
                "Core-LB-G10")) {
            assertTrue(consolidation.contains(goal), "consolidation should name " + goal);
        }

        for (String pr : List.of("#333", "#335", "#336", "#337", "#338", "#339", "#340", "#341", "#342")) {
            assertTrue(consolidation.contains(pr), "consolidation should cite merged PR " + pr);
        }

        for (String branch : List.of(
                "codex/core-loadbalancer-feature-contract",
                "codex/core-lb-g02-edge-invariants",
                "codex/core-lb-g03-capacity-predictive-overload",
                "codex/core-lb-g04-deterministic-ordering-ties",
                "codex/core-lb-g05-least-loaded-semantics",
                "codex/core-lb-g06-routing-registry-comparison-contract",
                "codex/core-lb-g07-server-lifecycle-invariants",
                "codex/core-lb-g08-overload-recovery-scenarios",
                "codex/core-lb-g09-reviewer-evidence-map",
                "codex/core-lb-g10-evidence-consolidation")) {
            assertTrue(consolidation.contains(branch), "consolidation should cite branch " + branch);
        }
    }

    @Test
    void consolidationSummarizesTestedGuaranteesAndVerificationPattern() throws IOException {
        String normalized = read(CONSOLIDATION).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "empty server sets",
                "all-unhealthy server sets",
                "negative total load",
                "capacity-aware and predictive",
                "deterministic ordering",
                "least-loaded facade behavior",
                "routing strategy registry order",
                "server lifecycle",
                "overload, degradation, recovery",
                "reviewer-facing evidence navigation")) {
            assertTrue(normalized.contains(expected), "consolidation should summarize " + expected);
        }

        for (String command : List.of(
                "mvn -q test",
                "mvn -q \"-DskipTests\" package",
                "mvn -B package",
                "git diff --check",
                "git diff --cached --check",
                ".\\scripts\\smoke\\enterprise-lab-workflow.ps1 -Package")) {
            assertTrue(read(CONSOLIDATION).contains(command), "consolidation should include verification command " + command);
        }
    }

    @Test
    void consolidationIsLinkedFromContractAndReviewerPathRemainsLinked() throws IOException {
        assertTrue(read(CONTRACT).contains("CORE_LOADBALANCER_EVIDENCE_CONSOLIDATION.md"),
                "core feature contract should link to consolidation");
        assertTrue(read(TRUST_MAP).contains("agent/CORE_LOADBALANCER_FEATURE_CONTRACT.md"),
                "reviewer trust map should retain core contract link");
        assertTrue(read(CONSOLIDATION).contains("../REVIEWER_TRUST_MAP.md"),
                "consolidation should name the reviewer trust map path");
    }

    @Test
    void consolidationPreservesNotProvenBoundariesAndAvoidsOverclaims() throws IOException {
        String normalized = (read(CONSOLIDATION) + "\n" + read(CONTRACT)).toLowerCase(Locale.ROOT);

        for (String boundary : List.of(
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
            assertTrue(normalized.contains(boundary), "missing not-proven boundary " + boundary);
        }

        for (String forbidden : List.of(
                "production ready",
                "is certified",
                "certified for production",
                "guaranteed p99",
                "real tenant validated",
                "live-cloud validated",
                "benchmark proven")) {
            assertFalse(normalized.contains(forbidden), "consolidation must not overclaim: " + forbidden);
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
