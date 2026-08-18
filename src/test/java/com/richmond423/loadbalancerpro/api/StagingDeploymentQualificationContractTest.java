package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class StagingDeploymentQualificationContractTest {
    private static final Path PROFILE = Path.of("scripts/bench/staging-profile.example.json");
    private static final Path TARGET_VALIDATOR = Path.of("scripts/bench/validate-staging-target.py");
    private static final Path DEPLOYMENT_VALIDATOR = Path.of("scripts/bench/validate-staging-deployment.py");
    private static final Path RUNNER = Path.of("scripts/bench/proxy-staging-qualification.sh");
    private static final Path CONTRACT = Path.of("scripts/bench/staging-deployment-contract-test.sh");
    private static final Path MANIFEST = Path.of("deploy/kubernetes-proxy-prod.yaml");
    private static final Path CI = Path.of(".github/workflows/ci.yml");

    @Test
    void profileBindsDistinctRegistryArtifactsAndDeploymentLimits() throws Exception {
        String profile = read(PROFILE);
        String targetValidator = read(TARGET_VALIDATOR);

        for (String expected : List.of(
                "registryRepository",
                "prior",
                "candidate",
                "minimumZones",
                "maximumZoneSkew",
                "cpuRequestMillis",
                "requiredSignals",
                "maximumUnavailable",
                "maximumRolloutSeconds",
                "candidateRollout",
                "priorRollback",
                "rollout-candidate",
                "rollback-prior")) {
            assertTrue(profile.contains(expected), "missing staging deployment input: " + expected);
            assertTrue(targetValidator.contains(expected), "staging validator does not bind: " + expected);
        }
        assertFalse(profile.contains("apiKey"));
    }

    @Test
    void deploymentSnapshotIsStrictAndTransitionsFailClosed() throws Exception {
        String validator = read(DEPLOYMENT_VALIDATOR);
        String runner = read(RUNNER);

        for (String expected : List.of(
                "snapshot image reference is not the reviewed registry repository and digest",
                "all ready replicas must run the reviewed phase digest",
                "snapshot does not prove the reviewed minimum zone count",
                "snapshot resource",
                "metrics must cover every ready replica",
                "transition exceeded the reviewed unavailable-replica limit",
                "transition did not prove accepted-work drain")) {
            assertTrue(validator.contains(expected), "missing deployment snapshot invariant: " + expected);
        }
        for (String expected : List.of(
                "run_attested_hook",
                "LBP_STAGING_PRIOR_IMAGE_REFERENCE",
                "LBP_STAGING_CANDIDATE_IMAGE_REFERENCE",
                "needs_rollback=true",
                "run_scenario candidateRollout rollout-candidate candidate",
                "run_scenario priorRollback rollback-prior rollback",
                "deploymentSnapshotValidated:true")) {
            assertTrue(runner.contains(expected), "missing staging transition behavior: " + expected);
        }
        assertFalse(runner.contains("--insecure"));
    }

    @Test
    void ciRunsTheSnapshotContractAndManifestRequiresZoneSeparation() throws Exception {
        String contract = read(CONTRACT);
        String manifest = read(MANIFEST);
        String ci = read(CI);

        assertTrue(contract.contains("rejected 16 unsafe snapshots"));
        assertTrue(ci.contains("bash scripts/bench/staging-deployment-contract-test.sh"));
        assertTrue(manifest.contains("topologySpreadConstraints:"));
        assertTrue(manifest.contains("minDomains: 2"));
        assertTrue(manifest.contains("whenUnsatisfiable: DoNotSchedule"));
        assertTrue(manifest.contains("topologyKey: topology.kubernetes.io/zone"));
        assertTrue(manifest.contains("maxUnavailable: 0"));
        assertTrue(manifest.contains("maxSurge: 1"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
