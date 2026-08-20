package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class KubernetesLiveTopologyContractTest {
    private static final Path DOCKERFILE = Path.of("Dockerfile");
    private static final Path CLUSTER = Path.of("deploy/kubernetes/kind-cluster.yaml");
    private static final Path WORKLOAD = Path.of("deploy/kubernetes/qualification.yaml");
    private static final Path CANDIDATE = Path.of("deploy/topology/RolloutCandidate.Dockerfile");
    private static final Path PROFILE = Path.of("scripts/bench/kubernetes-topology-profile.example.json");
    private static final Path RUNNER = Path.of("scripts/bench/proxy-kubernetes-topology.sh");
    private static final Path CONTRACT = Path.of("scripts/bench/kubernetes-topology-contract-test.sh");
    private static final Path CI = Path.of(".github/workflows/ci.yml");

    @Test
    void imageAndWorkloadUseAKubernetesVerifiableRestrictedIdentity() throws IOException {
        assertTrue(read(DOCKERFILE).contains("USER 10001:10001"));
        String workload = read(WORKLOAD);
        for (String invariant : List.of(
                "pod-security.kubernetes.io/enforce: restricted",
                "automountServiceAccountToken: false",
                "maxUnavailable: 0",
                "maxSurge: 1",
                "minDomains: 2",
                "runAsNonRoot: true",
                "runAsUser: 10001",
                "runAsGroup: 10001",
                "allowPrivilegeEscalation: false",
                "readOnlyRootFilesystem: true",
                "drop: [\"ALL\"]",
                "kind: PodDisruptionBudget",
                "kind: NetworkPolicy",
                "secretName: loadbalancerpro-api-key-a",
                "path: loadbalancerpro.api.rotation-key",
                "https://${LBP_TLS_HOSTNAME}:8080/proxy/kubernetes/topology",
                "LBP_HEALTH_CHECK_ENABLED: \"false\"",
                "LBP_COOLDOWN_ENABLED: \"false\"",
                "LBP_RETRY_ENABLED: \"true\"",
                "LBP_RETRY_MAX_ATTEMPTS: \"2\"",
                "LBP_RETRY_BUDGET_PERCENT: \"100\"",
                "LBP_RETRY_NON_IDEMPOTENT: \"false\"",
                "secretName: loadbalancerpro-server-tls-a")) {
            assertTrue(workload.contains(invariant), "missing restricted workload invariant: " + invariant);
        }
        assertFalse(workload.contains("BEGIN PRIVATE KEY"));
        assertFalse(workload.contains("stringData:"));
    }

    @Test
    void clusterAndProfilePinTheDisposableTwoZoneBoundary() throws IOException {
        String cluster = read(CLUSTER);
        assertEquals(3, count(cluster, "  - role:"));
        assertEquals(2, count(cluster, "  - role: worker"));
        assertTrue(cluster.contains("listenAddress: 127.0.0.1"));
        assertTrue(cluster.contains("hostPort: 18460"));
        assertTrue(cluster.contains("kind: KubeProxyConfiguration"));
        assertTrue(cluster.contains("mode: iptables"));
        assertTrue(cluster.contains("minSyncPeriod: 0s"));
        assertTrue(cluster.contains("syncPeriod: 1s"));
        assertFalse(cluster.contains("nodePort"));
        assertEquals(3, count(cluster,
                "kindest/node:v1.34.3@sha256:08497ee19eace7b4b5348db5c6a1591d7752b164530a36f855cb0f2bdcbadd48"));

        JsonNode profile = new ObjectMapper().readTree(read(PROFILE));
        assertEquals(6, profile.path("schemaVersion").asInt());
        assertEquals("example", profile.path("review").path("status").asText());
        assertEquals("v1.34.3", profile.path("cluster").path("kubectlVersion").asText());
        assertEquals(2, profile.path("cluster").path("workers").asInt());
        assertEquals(2, profile.path("cluster").path("zones").asInt());
        assertEquals("lbp-kubernetes-smoke", profile.path("cluster").path("namespace").asText());
        assertEquals(30443, profile.path("cluster").path("nodePort").asInt());
        assertEquals("close-per-request", profile.path("workload").path("connectionMode").asText());
        assertTrue(profile.path("workload").path("transitionSeconds").asInt() >= 40);
        assertTrue(profile.path("workload").path("rolloutSeconds").asInt()
                >= profile.path("objectives").path("maximumRolloutSeconds").asInt() + 5);
        assertTrue(profile.path("objectives").path("minimumRolloutSuccessRatio").asDouble() >= 0.95);
        assertTrue(profile.path("objectives").path("minimumPostRolloutSuccessRatio").asDouble() >= 0.95);
        assertTrue(profile.path("workload").path("rollbackSeconds").asInt()
                >= profile.path("objectives").path("maximumRollbackSeconds").asInt() + 5);
        assertTrue(profile.path("objectives").path("minimumRollbackSuccessRatio").asDouble() >= 0.95);
        assertTrue(profile.path("objectives").path("minimumPostRollbackSuccessRatio").asDouble() >= 0.95);
        assertTrue(profile.path("workload").path("certificateRotationSeconds").asInt()
                >= profile.path("objectives").path("maximumCertificateRotationSeconds").asInt() + 5);
        assertTrue(profile.path("workload").path("certificateRollbackSeconds").asInt()
                >= profile.path("objectives").path("maximumCertificateRollbackSeconds").asInt() + 5);
        assertTrue(profile.path("objectives").path("minimumCertificateRotationSuccessRatio").asDouble() >= 0.95);
        assertTrue(profile.path("objectives").path("minimumPostCertificateRotationSuccessRatio").asDouble() >= 0.95);
        assertTrue(profile.path("objectives").path("minimumCertificateRollbackSuccessRatio").asDouble() >= 0.95);
        assertTrue(profile.path("objectives").path("minimumPostCertificateRollbackSuccessRatio").asDouble() >= 0.95);
        assertTrue(profile.path("workload").path("apiKeyTransitionSeconds").asInt()
                >= profile.path("objectives").path("maximumApiKeyTransitionSeconds").asInt() + 5);
        assertTrue(profile.path("objectives").path("minimumApiKeyTransitionSuccessRatio").asDouble() >= 0.95);
        assertTrue(profile.path("objectives").path("minimumPostApiKeyTransitionSuccessRatio").asDouble() >= 0.95);
        assertEquals("lbp-kubernetes.local", profile.path("tlsRotation").path("hostname").asText());
        assertEquals("loadbalancerpro-server-tls-a",
                profile.path("tlsRotation").path("baselineSecret").asText());
        assertEquals("loadbalancerpro-server-tls-b",
                profile.path("tlsRotation").path("candidateSecret").asText());
        assertEquals("loadbalancerpro-api-key-a",
                profile.path("apiKeyRotation").path("baselineSecret").asText());
        assertEquals("loadbalancerpro-api-key-a-b",
                profile.path("apiKeyRotation").path("overlapSecret").asText());
        assertEquals("loadbalancerpro-api-key-b",
                profile.path("apiKeyRotation").path("candidateSecret").asText());
        assertTrue(profile.path("workload").path("abruptTransitionSeconds").asInt()
                >= profile.path("objectives").path("maximumAbruptEndpointWithdrawalSeconds").asInt() + 5);
        assertTrue(profile.path("objectives").path("minimumAbruptTransitionSuccessRatio").asDouble() >= 0.90);
        assertTrue(profile.path("objectives").path("minimumAbruptDegradedSuccessRatio").asDouble() >= 0.95);
        assertTrue(profile.path("objectives").path("minimumAbruptRecoveredSuccessRatio").asDouble() >= 0.95);
        assertTrue(profile.path("objectives").path("maximumAbruptTransitionP99Millis").asInt() <= 6000);
    }

    @Test
    void runnerExecutesLiveImageTransitionRollbackPlannedAndAbruptWorkerLossChecks() throws IOException {
        String runner = read(RUNNER);
        assertTrue(read(CANDIDATE).contains("metadata-only-local-candidate"));
        for (String behavior : List.of(
                "kind create cluster",
                "kind load docker-image",
                "Kubernetes rollout candidate is not content-distinct from the baseline image",
                "Kubernetes rollout candidate changed application layers",
                "Refusing to reuse or delete an existing kind cluster",
                "Live kube-proxy config is missing",
                "kube-proxy-config.yaml",
                "Proxy replicas were not placed in distinct zones",
                "minDomains: 2",
                "-keepalive=false",
                "${phase}-${pod}-metrics.txt",
                "loadbalancerpro.io/qualification-rollout",
                "loadbalancerpro.io/qualification-release",
                "--arg image \"$candidate_image\"",
                "kubectl rollout status deployment/loadbalancerpro",
                "rollout-continuity.csv",
                "rollback-continuity.csv",
                ".metadata.deletionTimestamp == null",
                ".conditions.terminating != true",
                "Rolling replacement retained an initial proxy pod UID",
                "Candidate rollout did not change the immutable runtime image ID",
                "post-rollout-distribution-delta.json",
                "bothReplacementProxyReplicasServed: true",
                "Baseline rollback retained a prior proxy pod UID",
                "Baseline rollback did not restore the initial immutable runtime image ID",
                "post-rollback-distribution-delta.json",
                "bothRestoredBaselineProxyReplicasServed: true",
                "generate_server_identity",
                "Generated Kubernetes TLS identities did not use independent trust roots",
                "-verify_hostname \"$tls_hostname\"",
                ".immutable = true",
                ".type = \"kubernetes.io/tls\"",
                "tls-secret-metadata.json",
                "Candidate-only CA before rotation",
                "Baseline TLS identity",
                "loadbalancerpro.io/qualification-tls-rotation",
                "certificate-rotation-continuity.csv",
                "Certificate rotation retained a baseline-certificate pod UID",
                "Certificate rotation changed the immutable runtime image ID",
                "Baseline-only CA after rotation",
                "Rotated TLS identity",
                "bothRotatedCertificateProxyReplicasServed",
                "loadbalancerpro.io/qualification-tls-rollback",
                "certificate-rollback-continuity.csv",
                "Certificate rollback retained a prior TLS pod UID",
                "Certificate rollback changed the immutable runtime image ID",
                "Candidate-only CA after certificate rollback",
                "Restored baseline TLS identity",
                "bothRestoredCertificateProxyReplicasServed",
                "continuousTrafficBundleContainsBothAuthorities: true",
                "candidateOnlyRejectedBeforeRotation: true",
                "baselineOnlyRejectedAfterRotation: true",
                "candidateOnlyRejectedAfterRollback: true",
                "not an ingress-controller",
                "create_immutable_api_key_secret",
                "api-key-secret-metadata.json",
                "loadbalancerpro.io/qualification-api-key-overlap",
                "loadbalancerpro.io/qualification-api-key-commit",
                "loadbalancerpro.io/qualification-api-key-rollback-overlap",
                "loadbalancerpro.io/qualification-api-key-rollback-commit",
                "${phase}-continuity.csv",
                "Candidate API key before overlap",
                "Retired baseline API key after commit",
                "Retired candidate API key after rollback",
                "bothApiKeysAcceptedDuringOverlap",
                "candidateApiKeyRetiredAfterRollback",
                "required primary plus at most one operator-bounded rotation key",
                "Kubernetes API-key value leaked into evidence",
                "not dynamic Secret reload",
                "priorPodUids: $priorPodUids",
                "candidatePodUids: $candidatePodUids",
                "restoredPodUids: $restoredPodUids",
                "contentDistinctRuntimeImageId: true",
                "restoredInitialRuntimeImageId: true",
                "kubectl cordon",
                "kubectl drain",
                "--pod-selector='loadbalancerpro.io/backend=backend-a'",
                "wait_for_backend_endpoint_count backend-a 1 30",
                "--pod-selector='loadbalancerpro.io/backend=backend-b'",
                "wait_for_backend_endpoint_count backend-b 1 30",
                "--pod-selector='app.kubernetes.io/name=loadbalancerpro'",
                "ready Service endpoints during planned drain",
                "docker stop",
                "ready Service endpoints while one worker is stopped",
                "docker start",
                "kubectl uncordon",
                "recovered-distribution-delta.json",
                "Both recovered proxies and both backends must serve traffic after planned loss",
                "docker kill \"$abrupt_node\"",
                "node.kubernetes.io/out-of-service=qualification-abrupt-worker-loss:NoExecute",
                "--ignore-not-found --force --grace-period=0 --wait=false",
                "abrupt-loss source pods remaining in the API",
                "apiForcedPodNames: $abruptForcedPodNames",
                "docker inspect --format '{{.State.Running}}'",
                "Abrupt-loss recovery retained the failed worker pod UID",
                "abrupt-recovered-distribution-delta.json",
                "bothRecoveredProxyReplicasServed: true",
                "stoppedWithoutDrain: $abruptWorker",
                "retainedFailedProxyPodUids: 0",
                "bothProxyReplicasServed: true")) {
            assertTrue(runner.contains(behavior), "missing live Kubernetes proof behavior: " + behavior);
        }
        assertTrue(read(CONTRACT).contains("rejected 64 unsafe profiles without creating a cluster"));
        assertFalse(runner.contains("--insecure"));
        assertFalse(runner.contains("--validate=false"));
    }

    @Test
    void ciPinsKindAndRunsAndUploadsTheLiveProof() throws IOException {
        String ci = read(CI);
        assertTrue(ci.contains("timeout-minutes: 45"));
        assertTrue(ci.contains("eb244cbafcc157dff60cf68693c14c9a75c4e6e6fedaf9cd71c58117cb93e3fa"));
        assertTrue(ci.contains("ab60ca5f0fd60c1eb81b52909e67060e3ba0bd27e55a8ac147cbc2172ff14212"));
        assertTrue(ci.contains("bash scripts/bench/kubernetes-topology-contract-test.sh"));
        assertTrue(ci.contains("bash scripts/bench/proxy-kubernetes-topology.sh --mode smoke"));
        assertTrue(ci.contains("LBP_KUBERNETES_CANDIDATE_SOURCE_IMAGE: loadbalancerpro:immutable-candidate-ci"));
        assertTrue(ci.contains("name: kubernetes-live-topology-smoke-"));
    }

    private static int count(String value, String needle) {
        return value.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
