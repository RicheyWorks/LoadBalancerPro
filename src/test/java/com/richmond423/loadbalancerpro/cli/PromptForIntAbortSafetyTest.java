package com.richmond423.loadbalancerpro.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.richmond423.loadbalancerpro.core.CloudManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PromptForIntAbortSafetyTest {
    private static final Path CLI_SOURCE =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/cli/LoadBalancerCLI.java");
    private static final Path UNDO_HISTORY = Path.of("undo_history.ser");

    @Test
    void explicitAbortUsesDedicatedSentinelInsteadOfValidNegativeOne() {
        ConsoleUtils console = consoleWithInput("not-an-integer\nn\n");

        int result = console.promptForInt("adjust: ", -10, 10, "adjustment");

        assertEquals(Integer.MIN_VALUE, result);
    }

    @Test
    void maxAttemptsUsesDedicatedSentinelInsteadOfValidNegativeOne() {
        ConsoleUtils console = consoleWithInput(
                "not-an-integer\ny\n"
                        + "still-not-an-integer\ny\n"
                        + "again-not-an-integer\ny\n");

        int result = console.promptForInt("adjust: ", -10, 10, "adjustment");

        assertEquals(Integer.MIN_VALUE, result);
    }

    @Test
    void negativeOneRemainsAValidIntegerWhenItIsInsideTheRequestedRange() {
        ConsoleUtils console = consoleWithInput("-1\n");

        int result = console.promptForInt("adjust: ", -10, 10, "adjustment");

        assertEquals(-1, result);
    }

    @Test
    void everyPromptForIntCallerChecksTheDedicatedAbortSentinel() throws Exception {
        String source = Files.readString(CLI_SOURCE);
        long promptCalls = Pattern.compile("console\\.promptForInt\\(")
                .matcher(source)
                .results()
                .count();
        long dedicatedAbortChecks = Pattern.compile(
                        "(?:==|!=)\\s*ConsoleUtils\\.PROMPT_ABORTED")
                .matcher(source)
                .results()
                .count();

        assertEquals(6, promptCalls, "the caller-wide audit must be updated when promptForInt callers change");
        assertEquals(promptCalls, dedicatedAbortChecks,
                "every caller must distinguish abort from all valid integer values");
    }

    @Test
    void abortingScaleCloudPerformsNoCloudManagerInteraction() throws Exception {
        byte[] originalUndoHistory = Files.exists(UNDO_HISTORY) ? Files.readAllBytes(UNDO_HISTORY) : null;
        LoadBalancerCLI.CliRunner runner = new LoadBalancerCLI.CliRunner(new String[]{"--no-monitor"});
        CloudManager cloudManager = mock(CloudManager.class);
        Field cloudManagerField = LoadBalancerCLI.CliRunner.class.getDeclaredField("cloudManager");
        cloudManagerField.setAccessible(true);
        cloudManagerField.set(runner, cloudManager);
        try {
            Method scaleCloud = LoadBalancerCLI.CliRunner.class
                    .getDeclaredMethod("scaleCloud", ConsoleUtils.class);
            scaleCloud.setAccessible(true);

            scaleCloud.invoke(runner, consoleWithInput("not-an-integer\nn\n"));

            verifyNoInteractions(cloudManager);
        } finally {
            cloudManagerField.set(runner, null);
            Method shutdown = LoadBalancerCLI.CliRunner.class.getDeclaredMethod("shutdown");
            shutdown.setAccessible(true);
            shutdown.invoke(runner);
            if (originalUndoHistory == null) {
                Files.deleteIfExists(UNDO_HISTORY);
            } else {
                Files.write(UNDO_HISTORY, originalUndoHistory);
            }
        }
    }

    private static ConsoleUtils consoleWithInput(String input) {
        return new ConsoleUtils(
                new Scanner(input),
                new CliConfig(new String[0], new Properties()));
    }
}
