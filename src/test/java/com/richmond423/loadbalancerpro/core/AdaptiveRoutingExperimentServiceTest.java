package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AdaptiveRoutingExperimentServiceTest {

    @Test
    void shadowModeComparesBaselineAndRecommendationsWithoutChangingInfluencedOutput() {
        AdaptiveRoutingExperimentReport report = new AdaptiveRoutingExperimentService().runCatalog(false);

        assertEquals("shadow", report.mode());
        assertFalse(report.activeInfluenceEnabled());
        assertEquals(10, report.results().size());
        assertTrue(report.safetyNotes().stream().anyMatch(note -> note.contains("shadow-only")));
        for (AdaptiveRoutingExperimentResult result : report.results()) {
            assertFalse(result.activeInfluenceEnabled());
            assertFalse(result.resultChanged(), result.scenarioName());
            assertEquals(result.baselineAllocations(), result.influencedAllocations(), result.scenarioName());
            assertEquals(result.baselineUnallocatedLoad(), result.influencedUnallocatedLoad(), 0.0001,
                    result.scenarioName());
            assertTrue(result.laseSignalsConsidered().contains("tail latency"));
        }
    }

    @Test
    void influenceModeCanChangeOnlyExperimentOutputWhenFreshRecommendationDiffers() {
        AdaptiveRoutingExperimentReport report = new AdaptiveRoutingExperimentService().runCatalog(true);

        assertEquals("active-experiment", report.mode());
        assertTrue(report.activeInfluenceEnabled());
        assertTrue(report.results().stream().anyMatch(AdaptiveRoutingExperimentResult::resultChanged),
                "at least one deterministic fixture should demonstrate active-experiment output");
        AdaptiveRoutingExperimentResult changed = report.results().stream()
                .filter(AdaptiveRoutingExperimentResult::resultChanged)
                .findFirst()
                .orElseThrow();
        assertTrue(changed.guardrailReason().contains("policy gates passed"));
        assertTrue(changed.explanation().contains("Opt-in influence preferred LASE-recommended backend"));
        assertTrue(changed.rollbackReason().contains("operator can return policy mode"));
        assertEquals("active-experiment", changed.policyDecision().mode());
    }

    @Test
    void staleAndAllUnhealthyFixturesFailClosedUnderInfluenceMode() {
        AdaptiveRoutingExperimentReport report = new AdaptiveRoutingExperimentService().runCatalog(true);

        AdaptiveRoutingExperimentResult stale = result(report, "stale-signal");
        assertFalse(stale.resultChanged());
        assertTrue(stale.guardrailReason().contains("stale signal"));
        assertTrue(stale.rollbackReason().contains("baseline retained"));

        AdaptiveRoutingExperimentResult allUnhealthy = result(report, "all-unhealthy-degradation");
        assertFalse(allUnhealthy.resultChanged());
        assertNotNull(allUnhealthy.guardrailReason());
        assertTrue(allUnhealthy.guardrailReason().contains("all backends unhealthy"));
        assertTrue(allUnhealthy.baselineAllocations().values().stream().allMatch(value -> value == 0.0));
    }

    @Test
    void offAndRecommendModesKeepBaselineAsFinalDecision() {
        AdaptiveRoutingExperimentService service = new AdaptiveRoutingExperimentService();

        AdaptiveRoutingExperimentReport off = service.runCatalog(AdaptiveRoutingPolicyMode.OFF);
        AdaptiveRoutingExperimentReport recommend = service.runCatalog(AdaptiveRoutingPolicyMode.RECOMMEND);

        assertEquals("off", off.mode());
        assertTrue(off.results().stream().noneMatch(AdaptiveRoutingExperimentResult::resultChanged));
        assertTrue(off.results().stream().allMatch(result ->
                result.policyDecision().guardrailReasons().contains("policy mode off")));

        assertEquals("recommend", recommend.mode());
        assertTrue(recommend.results().stream().noneMatch(AdaptiveRoutingExperimentResult::resultChanged));
        assertTrue(recommend.results().stream().anyMatch(result ->
                result.policyDecision().rollbackReason().contains("recommendation is accepted")));
    }

    @Test
    void formattedExperimentReportIsDeterministicAndAvoidsSecrets() {
        AdaptiveRoutingExperimentService service = new AdaptiveRoutingExperimentService();
        AdaptiveRoutingExperimentReportFormatter formatter = new AdaptiveRoutingExperimentReportFormatter();

        String first = formatter.format(service.runCatalog(true));
        String second = formatter.format(service.runCatalog(true));

        assertEquals(first, second);
        assertTrue(first.contains("# Adaptive Routing Experiment Report"));
        assertTrue(first.contains("normal-balanced-load"));
        assertTrue(first.contains("Active LASE influence enabled: `true`"));
        String lower = first.toLowerCase();
        assertFalse(lower.contains("password"));
        assertFalse(lower.contains("bearer "));
        assertFalse(lower.contains("access key"));
        assertFalse(lower.contains("api key"));
    }

    private AdaptiveRoutingExperimentResult result(AdaptiveRoutingExperimentReport report, String scenarioName) {
        return report.results().stream()
                .filter(result -> scenarioName.equals(result.scenarioName()))
                .findFirst()
                .orElseThrow();
    }
}
