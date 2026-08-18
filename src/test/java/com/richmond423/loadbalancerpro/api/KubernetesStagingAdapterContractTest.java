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

class KubernetesStagingAdapterContractTest {
    private static final Path PROFILE =
            Path.of("scripts/bench/kubernetes-staging-adapter-profile.example.json");
    private static final Path COMPILER =
            Path.of("scripts/bench/prepare-kubernetes-staging-adapters.py");
    private static final Path RUNTIME =
            Path.of("scripts/bench/kubernetes-staging-adapter-runtime.py");
    private static final Path COMPILER_CONTRACT =
            Path.of("scripts/bench/kubernetes-staging-adapter-contract-test.sh");
    private static final Path RUNTIME_CONTRACT =
            Path.of("scripts/bench/kubernetes-staging-runtime-contract-test.sh");
    private static final Path CI = Path.of(".github/workflows/ci.yml");

    @Test
    void reviewedProfileBindsClusterMutationsAndTelemetry() throws IOException {
        JsonNode profile = new ObjectMapper().readTree(read(PROFILE));
        assertEquals("example", profile.path("review").path("status").asText());
        assertEquals("0".repeat(64), profile.path("kubectl").path("executableSha256").asText());
        for (String action : List.of("reload", "drain", "reset")) {
            assertEquals("0".repeat(64),
                    profile.path("faults").path(action).path("payloadSha256").asText());
        }
        assertEquals(2, profile.path("capacity").path("upstreamIds").size());

        String compiler = read(COMPILER);
        assertTrue(compiler.contains("generated adapters must remain outside the repository"));
        assertTrue(compiler.contains("cluster context and namespace must not look production-like"));
        assertTrue(compiler.contains("build mode requires a reviewed faults.{action}.payloadSha256"));
    }

    @Test
    void runtimeRechecksExactClusterAndDeploymentIdentity() throws IOException {
        String runtime = read(RUNTIME);
        for (String invariant : List.of(
                "current kubectl context is not the reviewed staging context",
                "kubectl context API server differs from the reviewed staging server",
                "staging namespace UID differs from the reviewed namespace",
                "staging artifact identity differs from the compiled adapter binding",
                "live Kubernetes maxUnavailable differs from the reviewed rollout limit",
                "live Kubernetes configuration fingerprint differs from the reviewed staging profile",
                "live Kubernetes ingress fingerprint differs from the reviewed staging profile",
                "configuration object is not immutable",
                "reviewed {action} payload hash changed",
                "ready pod runtime image digest differs from the reviewed digest")) {
            assertTrue(runtime.contains(invariant), "missing Kubernetes adapter invariant: " + invariant);
        }
        assertFalse(runtime.contains("shell=True"));
        assertFalse(runtime.contains("--insecure"));
    }

    @Test
    void ciExecutesCompilerAndHermeticRuntimeContracts() throws IOException {
        assertTrue(read(COMPILER_CONTRACT).contains("rejected %s unsafe profiles without cluster access"));
        assertTrue(read(RUNTIME_CONTRACT).contains("9 fail-closed drift boundaries"));
        String ci = read(CI);
        assertTrue(ci.contains("bash scripts/bench/kubernetes-staging-adapter-contract-test.sh"));
        assertTrue(ci.contains("bash scripts/bench/kubernetes-staging-runtime-contract-test.sh"));
        assertTrue(ci.contains("python3 -m py_compile scripts/bench/kubernetes-staging-adapter-runtime.py"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
