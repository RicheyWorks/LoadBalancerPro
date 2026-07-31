package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ExecutableJarSelectionContractTest {
    private static final Pattern MTIME_JAR_SELECTION = Pattern.compile(
            "(?s)(LoadBalancerPro-\\*\\.jar|Filter\\s+\"\\*\\.jar\").{0,500}Sort-Object\\s+LastWriteTime");
    private static final Path BASH_RESOLVER = Path.of("scripts/resolve-executable-jar.sh");
    private static final Path POWERSHELL_RESOLVER = Path.of("scripts/resolve-executable-jar.ps1");
    private static final List<Path> CONSUMERS = List.of(
            Path.of("scripts/operator-distribution-smoke.sh"),
            Path.of("scripts/operator-distribution-smoke.ps1"),
            Path.of("scripts/local-artifact-verify.sh"),
            Path.of("scripts/local-artifact-verify.ps1"),
            Path.of("scripts/smoke/adaptive-routing-experiment.ps1"),
            Path.of("scripts/smoke/controlled-adaptive-routing-policy.ps1"),
            Path.of("scripts/smoke/enterprise-auth-proof.ps1"),
            Path.of("scripts/smoke/enterprise-lab-observability-pack.ps1"),
            Path.of("scripts/smoke/enterprise-lab-workflow.ps1"),
            Path.of("scripts/smoke/operator-run-profiles-smoke.ps1"),
            Path.of("scripts/smoke/performance-baseline.ps1"),
            Path.of("scripts/smoke/postman-enterprise-lab-safe-smoke.ps1"),
            Path.of("scripts/smoke/release-candidate-dry-run-packet.ps1"),
            Path.of("scripts/smoke/release-intent-review.ps1"),
            Path.of("Dockerfile"),
            Path.of(".github/workflows/ci.yml"),
            Path.of("README.md"));

    @Test
    void crossPlatformResolversUseTheEffectiveMavenFinalNameAndRequireTheExactJar() throws IOException {
        String bash = read(BASH_RESOLVER);
        String powershell = read(POWERSHELL_RESOLVER);
        String combined = bash + "\n" + powershell;

        assertTrue(bash.contains("-Dexpression=project.build.finalName"));
        assertTrue(powershell.contains("'-Dexpression=project.build.finalName'"));
        assertTrue(combined.contains("target/"));
        assertTrue(combined.contains(".jar"));
        assertTrue(combined.contains("Expected executable jar not found"));
        assertTrue(bash.contains("--expected-only"));
        assertTrue(powershell.contains("$ExpectedOnly"));
        assertFalse(combined.contains("LoadBalancerPro-2.5.0"));
        assertFalse(combined.contains("LastWriteTime"));
        assertFalse(combined.contains("ls -t"));
    }

    @Test
    void everyAuthorizedConsumerUsesTheSharedResolverWithoutOrderOrVersionSelectionDrift() throws IOException {
        for (Path consumer : CONSUMERS) {
            String content = read(consumer);
            assertTrue(content.contains("resolve-executable-jar"),
                    consumer + " should use the shared executable-JAR contract");
            assertFalse(content.contains("LoadBalancerPro-2.5.0.jar"),
                    consumer + " should not hardcode the current project version");
            assertFalse(content.contains("ls -t target/LoadBalancerPro"),
                    consumer + " should not select by modification time");
            assertFalse(MTIME_JAR_SELECTION.matcher(content).find(),
                    consumer + " should not select by modification time");
            assertFalse(content.contains("| sort | tail -n 1"),
                    consumer + " should not select by lexical ordering");
        }
    }

    @Test
    void localArtifactHelpersRetainExplicitOperatorOverrides() throws IOException {
        String bash = read(Path.of("scripts/local-artifact-verify.sh"));
        String powershell = read(Path.of("scripts/local-artifact-verify.ps1"));

        assertTrue(bash.contains("--jar"));
        assertTrue(bash.contains("JAR_PATH=\"${2:-}\""));
        assertTrue(powershell.contains("[string]$JarPath"));
        assertTrue(powershell.contains("[string]::IsNullOrWhiteSpace($JarPath)"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
