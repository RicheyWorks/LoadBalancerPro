package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class ThreeTierAdaptiveRoutingStrategyDocumentationTest {
    private static final Path DOC = Path.of("docs/THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");

    @Test
    void threeTierStrategyDocExistsAndFramesCurrentTierOneFocus() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Three-Tier Adaptive Routing Strategy Positioning",
                "Enterprise-Grade Adaptive Routing Experimentation & Evidence Platform",
                "Tier 1 L4-L7 application delivery",
                "Tier 1 remains the live center of gravity",
                "Tier 1 remains the primary product focus",
                "advanced L4-L7 adaptive routing experimentation",
                "LASE/shadow evaluation",
                "explainability",
                "replay/evidence culture",
                "reviewer-safe metadata",
                "strong safety guardrails")) {
            assertTrue(doc.contains(expected), "strategy doc should mention " + expected);
        }
    }

    @Test
    void tierTwoAndTierThreeAreExplicitlyFutureOriented() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "future-oriented architecture hooks",
                "Tier 2 influence",
                "Tier 3 influence",
                "Future-oriented, not currently implemented",
                "read-only signal consumption concept",
                "planned extensions",
                "architecture-ready",
                "Phase 13: Multi-Tier Signal Integration Layer",
                "`ExternalSignalPort`",
                "`ExternalSignalSnapshot`",
                "`SignalSourceMetadata`",
                "`SignalFreshnessStatus`",
                "`SignalTrustLevel`",
                "`CarbonIntensitySignal`",
                "`GpuClusterPressureSignal`",
                "`FacilityThermalPressureSignal`",
                "`PowerCostSignal`",
                "documentation-only pseudocode")) {
            assertTrue(doc.contains(expected), "strategy doc should mention " + expected);
        }

        assertTrue(doc.contains("no mutation methods"));
        assertTrue(doc.contains("no write methods"));
        assertTrue(doc.contains("no control methods"));
    }

    @Test
    void expandedPhaseConceptsStayDocumentationOnly() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Expanded Phase 4: LASE Multi-Objective Evaluation",
                "latency",
                "throughput",
                "error rate",
                "queue pressure",
                "estimated power impact",
                "estimated carbon impact",
                "thermal risk",
                "reviewer evidence completeness",
                "Expanded Phase 5: AI-Era Workload Modeling",
                "`workloadClass`",
                "`burstProfile`",
                "`gpuSensitivity`",
                "`carbonSensitivity`",
                "`estimatedPowerDrawProfile`",
                "`thermalBurstPattern`",
                "`latencyCriticality`",
                "`batchDeferrable`",
                "documentation-only concepts in this sprint",
                "do not change runtime behavior")) {
            assertTrue(doc.contains(expected), "strategy doc should mention " + expected);
        }
    }

    @Test
    void explicitNonGoalsPreventTierTwoTierThreeOverclaims() throws Exception {
        String doc = read(DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no live power/grid control",
                "no facility mutation",
                "no GPU orchestration claim",
                "no production certification claim",
                "no live-cloud validation claim",
                "no real-tenant validation claim",
                "no carbon-aware routing implementation",
                "no production readiness claim",
                "no runtime signal ingestion",
                "no external API clients",
                "no HTTP calls",
                "no secrets, tokens, environment variables, or credentials",
                "no telemetry, storage, or persistence",
                "no replay execution",
                "no what-if mutation",
                "no upload/share/download/export/PDF/ZIP behavior",
                "Carbon-aware routing is not currently implemented",
                "Production readiness is not proven",
                "Live-cloud validation is not provided",
                "Real-tenant validation is not provided",
                "Facility automation is not implemented")) {
            assertTrue(doc.contains(expected), "strategy doc should keep boundary " + expected);
        }

        for (String forbidden : List.of(
                "carbon-aware routing is currently implemented",
                "currently performs carbon-aware routing",
                "gpu orchestration is implemented",
                "currently performs production gpu orchestration",
                "power grid control is implemented",
                "live power/grid control is implemented",
                "facility automation is implemented",
                "facility control is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "strategy doc must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkStrategyDocAsReferenceNotImplementationProof() throws Exception {
        String readme = read(README);
        String trustMap = read(TRUST_MAP);
        String audit = read(AUDIT);

        for (String entryPoint : List.of(readme, trustMap, audit)) {
            assertTrue(entryPoint.contains("THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md"),
                    "reviewer entry point should link the three-tier strategy doc");
        }

        assertTrue(readme.contains("current Tier 1 L4-L7 routing focus"));
        assertTrue(readme.contains("future-oriented Tier 2/Tier 3 signal concepts without claiming implementation"));
        assertTrue(trustMap.contains("strategic architecture reference"));
        assertTrue(trustMap.contains("not proof of GPU orchestration"));
        assertTrue(trustMap.contains("carbon-aware routing implementation"));
        assertTrue(audit.contains("not implementation proof"));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
