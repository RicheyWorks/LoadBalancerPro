package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class StagingCapacityQualificationContractTest {
    private static final Path RUNNER = Path.of("scripts/bench/proxy-staging-capacity-staircase.sh");
    private static final Path PROFILE = Path.of("scripts/bench/staging-capacity-profile.example.json");
    private static final Path PROFILE_VALIDATOR = Path.of("scripts/bench/validate-staging-capacity.py");
    private static final Path SAMPLE_VALIDATOR = Path.of("scripts/bench/validate-staging-capacity-sample.py");
    private static final Path RESULT_EVALUATOR = Path.of("scripts/bench/evaluate-staging-capacity.py");
    private static final Path CONTRACT_TEST = Path.of("scripts/bench/staging-capacity-contract-test.sh");
    private static final Path EVALUATOR_CONTRACT_TEST =
            Path.of("scripts/bench/staging-capacity-evaluator-contract-test.sh");
    private static final Path RUNNER_CONTRACT_TEST =
            Path.of("scripts/bench/staging-capacity-runner-contract-test.sh");
    private static final Path CI = Path.of(".github/workflows/ci.yml");

    @Test
    void runnerBindsCapacityToTheExactCandidateAndRestoresThePriorDigest() throws IOException {
        String runner = Files.readString(RUNNER);
        String evaluator = Files.readString(RESULT_EVALUATOR);
        assertTrue(runner.contains("--execution"));
        assertTrue(runner.contains("stagingProfileSha256"));
        assertTrue(runner.contains("LBP_STAGING_CANDIDATE_IMAGE_REFERENCE"));
        assertTrue(runner.contains("run_transition candidateRollout rollout-candidate candidate"));
        assertTrue(runner.contains("run_transition priorRollback rollback-prior rollback"));
        assertTrue(runner.contains("needs_rollback=true"));
        assertTrue(runner.contains("cleanup()"));
        assertTrue(runner.contains("run_attested_hook verify-deployment candidate"));
        assertTrue(runner.contains("Restart hook did not replace every candidate replica"));
        assertTrue(runner.contains("stableReplicaSet"));
        assertTrue(runner.contains("evaluate-staging-capacity.py"));
        assertTrue(evaluator.contains("firstReproducibleSaturationRate"));
        assertTrue(evaluator.contains("recommendedOperatingEnvelopeRate"));
        assertFalse(runner.contains("LBP_STAGING_CAPACITY_BASE_URL"));
        assertFalse(runner.contains("--insecure"));
    }

    @Test
    void runnerMeasuresTheReviewedFailureMatrixAndEveryReplica() throws IOException {
        String runner = Files.readString(RUNNER);
        for (String scenario : List.of("equal", "slow", "failing", "draining", "recovering")) {
            assertTrue(runner.contains("run_scenario " + scenario), "missing staging capacity case " + scenario);
        }
        for (String signal : List.of(
                "maximumCpuUtilizationRatio",
                "maximumMemoryUtilizationRatio",
                "maximumOpenConnectionsPerReplica",
                "maximumJvmLiveThreadsPerReplica",
                "requestMetricCoverageRatio",
                "retriesDelta",
                "shedsDelta",
                "limitRejectionsDelta",
                "maxInflight",
                "proxyOverheadP99Millis")) {
            assertTrue(runner.contains(signal), "missing staging capacity signal " + signal);
        }
        String sampleValidator = Files.readString(SAMPLE_VALIDATOR);
        assertTrue(sampleValidator.contains("sample must report every reviewed replica exactly once"));
        assertTrue(sampleValidator.contains("sampled replica is not running the reviewed candidate image"));
        assertTrue(sampleValidator.contains("sample upstream request counters must exactly cover reviewed upstream IDs"));
    }

    @Test
    void exampleCannotAuthorizeTrafficAndContractSuitesAreInCi() throws IOException {
        JsonNode profile = new ObjectMapper().readTree(Files.readString(PROFILE));
        assertEquals(1, profile.path("schemaVersion").asInt());
        assertEquals("example", profile.path("review").path("status").asText());
        assertEquals("0".repeat(64), profile.path("stagingBinding").path("stagingProfileSha256").asText());
        assertEquals("0".repeat(64), profile.path("telemetry").path("samplerSha256").asText());
        assertTrue(profile.path("capacity").path("repeatsPerStep").asInt() >= 3);
        assertTrue(profile.path("capacity").path("ratesPerSecond").size() >= 2);

        String profileValidator = Files.readString(PROFILE_VALIDATOR);
        assertTrue(profileValidator.contains("capacity profile is not bound to the exact staging profile bytes"));
        assertTrue(profileValidator.contains("execution requires a reviewed telemetry sampler hash"));
        assertTrue(Files.readString(CONTRACT_TEST).contains("16 profile plus 16 telemetry mutations"));
        assertTrue(Files.readString(EVALUATOR_CONTRACT_TEST).contains("rejected 12 unsafe matrices"));
        assertTrue(Files.readString(RUNNER_CONTRACT_TEST).contains("60 measured cases"));

        String ci = Files.readString(CI);
        assertTrue(ci.contains("bash -n scripts/bench/proxy-staging-capacity-staircase.sh"));
        assertTrue(ci.contains("bash scripts/bench/staging-capacity-contract-test.sh"));
        assertTrue(ci.contains("bash scripts/bench/staging-capacity-evaluator-contract-test.sh"));
        assertTrue(ci.contains("bash scripts/bench/staging-capacity-runner-contract-test.sh"));
        assertTrue(ci.contains("proxy-staging-capacity-staircase.sh --mode validate"));
        assertFalse(ci.contains("proxy-staging-capacity-staircase.sh --mode run --staging-profile"));
    }
}
