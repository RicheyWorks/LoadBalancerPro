package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class EnterpriseLabRunServiceTest {

    @Test
    void defaultRunExecutesAllScenariosWithDeterministicScorecard() {
        EnterpriseLabRunService service = new EnterpriseLabRunService();

        EnterpriseLabRun run = service.run(null, "all", "summary");

        assertEquals("lab-run-0001", run.runId());
        assertEquals("2026-05-14T00:00:00Z", run.createdAt().toString());
        assertEquals("all", run.mode());
        assertTrue(run.activeInfluenceEnabled());
        assertEquals(10, run.results().size());
        assertEquals(10, run.scorecard().totalScenarios());
        assertEquals("10/10", run.scorecard().explanationCoverage());
        assertEquals("lab evidence only / not production activation", run.scorecard().finalRecommendation());
        assertTrue(run.scorecard().labEvidenceOnly());
        assertTrue(run.safetyNotes().stream().anyMatch(note -> note.contains("No CloudManager")));
    }

    @Test
    void selectedScenarioRunIsBoundedAndDeterministic() {
        EnterpriseLabRunService service = new EnterpriseLabRunService();

        EnterpriseLabRun run = service.run(List.of("tail-latency-pressure", "normal-balanced-load"),
                "shadow", "summary");

        assertEquals(List.of("normal-balanced-load", "tail-latency-pressure"), run.selectedScenarioIds());
        assertFalse(run.activeInfluenceEnabled());
        assertEquals(2, run.scorecard().deterministicFixtureCount());
        assertEquals(2, service.listRunSummaries().get(0).scorecard().totalScenarios());
    }

    @Test
    void invalidModeUnknownScenarioAndTooManyScenariosFailClosed() {
        EnterpriseLabRunService service = new EnterpriseLabRunService();

        assertThrows(IllegalArgumentException.class, () -> service.run(null, "live", "summary"));
        assertThrows(IllegalArgumentException.class,
                () -> service.run(List.of("missing-scenario"), "all", "summary"));
        assertThrows(IllegalArgumentException.class,
                () -> service.run(List.of(
                        "normal-balanced-load",
                        "tail-latency-pressure",
                        "overload-pressure",
                        "stale-signal",
                        "conflicting-signal",
                        "all-unhealthy-degradation",
                        "recovery-transition",
                        "capacity-skew",
                        "repeated-replay-event-order",
                        "zero-edge-metric-values",
                        "extra"), "all", "summary"));
    }

    @Test
    void retainedRunsAreBoundedInMemory() {
        EnterpriseLabRunService service = new EnterpriseLabRunService();

        for (int i = 0; i < EnterpriseLabRunService.DEFAULT_MAX_RETAINED_RUNS + 2; i++) {
            service.run(List.of("normal-balanced-load"), "shadow", "summary");
        }

        assertEquals(EnterpriseLabRunService.DEFAULT_MAX_RETAINED_RUNS, service.listRunSummaries().size());
        assertTrue(service.findRun("lab-run-0001").isEmpty());
        assertTrue(service.findRun("lab-run-0027").isPresent());
    }
}

