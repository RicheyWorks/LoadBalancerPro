package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class TopologyRolloutPackagingContractTest {
    private static final Path COMPOSE = Path.of("deploy/topology/docker-compose.active-active.yml");
    private static final Path CANDIDATE = Path.of("deploy/topology/RolloutCandidate.Dockerfile");
    private static final Path REJECTED = Path.of("deploy/topology/RolloutRejected.Dockerfile");
    private static final Path INGRESS = Path.of("deploy/topology/TopologyIngress.java");
    private static final Path RUNNER = Path.of("scripts/bench/proxy-active-active-topology.sh");
    private static final Path CONTRACT = Path.of("scripts/bench/topology-validator-contract-test.sh");
    private static final Path CI = Path.of(".github/workflows/ci.yml");

    @Test
    void composeAllowsOnlyExplicitPerReplicaImageReplacement() throws Exception {
        String compose = read(COMPOSE);

        for (String expected : List.of(
                "LBP_TOPOLOGY_PROXY_A_IMAGE",
                "LBP_TOPOLOGY_PROXY_B_IMAGE",
                "read_only: true",
                "no-new-privileges:true",
                "stop_grace_period: 35s")) {
            assertTrue(compose.contains(expected), "missing topology replacement boundary: " + expected);
        }
        assertFalse(compose.contains("image: ${LBP_TOPOLOGY_PROXY_IMAGE"));
    }

    @Test
    void rolloutImagesPreserveLayersAndMakeRejectionUnstartable() throws Exception {
        String candidate = read(CANDIDATE);
        String rejected = read(REJECTED);

        for (String expected : List.of("ARG BASE_IMAGE", "FROM ${BASE_IMAGE}", "rollout.release-id")) {
            assertTrue(candidate.contains(expected));
            assertTrue(rejected.contains(expected));
        }
        assertFalse(candidate.contains("RUN "));
        assertFalse(candidate.contains("COPY "));
        assertFalse(candidate.contains("ADD "));
        assertFalse(candidate.contains("ENTRYPOINT"));
        assertTrue(rejected.contains("ENTRYPOINT [\"/bin/false\"]"));
    }

    @Test
    void runnerPinsExactImagesAndFailsClosedThroughAbortAndRollback() throws Exception {
        String runner = read(RUNNER);
        String contract = read(CONTRACT);
        String ingress = read(INGRESS);
        String ci = read(CI);

        for (String expected : List.of(
                "^sha256:[0-9a-f]{64}$",
                "assert_replica_image",
                "candidate_rejection_action",
                "immutable_rollout_action",
                "immutable_rollback_action",
                "--no-deps --no-build --force-recreate",
                "applicationLayersIdentical:true",
                "registry manifest digest")) {
            assertTrue(runner.contains(expected), "missing immutable rollout invariant: " + expected);
        }
        assertTrue(contract.contains("assert_rejected"));
        assertTrue(ingress.contains("Executors.newFixedThreadPool(32)"));
        assertTrue(ingress.contains("read.get(upstreamTimeout.toMillis(), TimeUnit.MILLISECONDS)"));
        assertTrue(ci.contains("Scan immutable rollout candidate image"));
        assertFalse(runner.contains("--insecure"));
        assertFalse(runner.contains("tls.verify=false"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
