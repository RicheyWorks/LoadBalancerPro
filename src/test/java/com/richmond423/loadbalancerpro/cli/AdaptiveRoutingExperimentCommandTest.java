package com.richmond423.loadbalancerpro.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class AdaptiveRoutingExperimentCommandTest {

    @Test
    void defaultModeRunsShadowOnlyExperimentWithoutStartingApi() {
        CapturedRun run = runExperiment("--adaptive-routing-experiment");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.output().contains("LoadBalancerPro Adaptive Routing Experiment"));
        assertTrue(run.output().contains("Mode: `shadow`"));
        assertTrue(run.output().contains("Active LASE influence enabled: `false`"));
        assertTrue(run.output().contains("normal-balanced-load"));
        assertTrue(run.output().contains("shadow-only"));
        assertTrue(run.output().contains("No CloudManager"));
        assertFalse(run.output().contains("Started LoadBalancerApiApplication"));
        assertTrue(run.error().isBlank());
    }

    @Test
    void influenceModeIsExplicitOptIn() {
        CapturedRun run = runExperiment("--adaptive-routing-experiment=influence");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.output().contains("Mode: `active-experiment`"));
        assertTrue(run.output().contains("Active LASE influence enabled: `true`"));
        assertTrue(run.output().contains("policy gates passed"));
    }

    @Test
    void allModePrintsShadowAndInfluenceReports() {
        CapturedRun run = runExperiment("--adaptive-routing-experiment=all");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.output().contains("Mode: `off`"));
        assertTrue(run.output().contains("Mode: `shadow`"));
        assertTrue(run.output().contains("Mode: `recommend`"));
        assertTrue(run.output().contains("Mode: `active-experiment`"));
    }

    @Test
    void invalidModeFailsSafelyWithUsage() {
        CapturedRun run = runExperiment("--adaptive-routing-experiment=live");

        assertEquals(2, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().contains("Invalid adaptive routing experiment mode"));
        assertTrue(run.error().contains("off, shadow, recommend, active-experiment, all"));
    }

    @Test
    void requestDetectionOnlyMatchesExperimentFlag() {
        assertTrue(AdaptiveRoutingExperimentCommand.isRequested(new String[]{"--adaptive-routing-experiment"}));
        assertTrue(AdaptiveRoutingExperimentCommand.isRequested(new String[]{"--adaptive-routing-experiment=all"}));
        assertFalse(AdaptiveRoutingExperimentCommand.isRequested(new String[]{"--lase-demo"}));
        assertFalse(AdaptiveRoutingExperimentCommand.isRequested(new String[]{"--server.port=18080"}));
    }

    private CapturedRun runExperiment(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        AdaptiveRoutingExperimentCommand.Result result = AdaptiveRoutingExperimentCommand.run(args,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(error, true, StandardCharsets.UTF_8));
        return new CapturedRun(result, output.toString(StandardCharsets.UTF_8),
                error.toString(StandardCharsets.UTF_8));
    }

    private record CapturedRun(AdaptiveRoutingExperimentCommand.Result result, String output, String error) {
    }
}
