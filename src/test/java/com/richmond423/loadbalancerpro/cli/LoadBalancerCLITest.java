package com.richmond423.loadbalancerpro.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class LoadBalancerCLITest {
    @Test
    void noArgumentsFailClosedWithoutStartingTheRetiredInteractiveMenu() {
        CapturedRun run = run();

        assertEquals(2, run.exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().contains("synthetic interactive LoadBalancerCLI menu has been retired"));
        assertFalse(run.error().contains("AWS_ACCESS_KEY_ID"));
    }

    @Test
    void unsupportedLegacyInteractiveOptionsFailClosed() {
        CapturedRun run = run("--cloud-enabled");

        assertEquals(2, run.exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().contains("has been retired"));
    }

    @Test
    void versionRemainsAvailableForExplicitLegacyLauncherCallers() {
        CapturedRun run = run("--version");

        assertEquals(0, run.exitCode());
        assertEquals("LoadBalancerCLI version 2.5.0" + System.lineSeparator(), run.output());
        assertTrue(run.error().isBlank());
    }

    @Test
    void retainedOfflineCommandsDelegateWithoutStartingInteractiveState() {
        CapturedRun run = run("--list-policy-templates");

        assertEquals(0, run.exitCode());
        assertTrue(run.output().contains("strict-zero-drift"));
        assertTrue(run.error().isBlank());
    }

    @Test
    void interactiveSupportAndUnsafeUndoPersistenceAreAbsent() throws Exception {
        Path cliRoot = Path.of("src/main/java/com/richmond423/loadbalancerpro/cli");
        for (String retired : List.of(
                "CliAction.java",
                "CliConfig.java",
                "ConsoleUtils.java",
                "ProgressAnimation.java",
                "UndoManager.java")) {
            assertFalse(Files.exists(cliRoot.resolve(retired)), retired + " must remain retired");
        }

        String launcher = Files.readString(cliRoot.resolve("LoadBalancerCLI.java"));
        for (String forbidden : List.of(
                "CloudManager",
                "ServerMonitor",
                "System.in",
                "Scanner",
                "ObjectInputStream",
                "undo_history.ser",
                "--cloud-enabled")) {
            assertFalse(launcher.contains(forbidden), "compatibility launcher must not contain " + forbidden);
        }
    }

    private static CapturedRun run(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        int exitCode = LoadBalancerCLI.run(
                args,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(error, true, StandardCharsets.UTF_8));
        return new CapturedRun(
                exitCode,
                output.toString(StandardCharsets.UTF_8),
                error.toString(StandardCharsets.UTF_8));
    }

    private record CapturedRun(int exitCode, String output, String error) {
    }
}
