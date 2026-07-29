package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ServerMonitorShutdownHookStressTest {
    private static final String SUCCESS_MARKER =
            "P-0.1 bounded-heap allocation probe passed: 10000";

    @Test
    @Timeout(value = 90)
    void tenThousandRequestScopedAllocationsCompleteUnderSixtyFourMegabyteHeap() throws Exception {
        String javaExecutable = Path.of(
                System.getProperty("java.home"),
                "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java")
                .toString();
        String testClasspath = System.getProperty(
                "surefire.test.class.path",
                System.getProperty("java.class.path"));
        String loggingConfiguration = Path.of(
                "target",
                "test-classes",
                "logback-stress.xml")
                .toAbsolutePath()
                .toString();
        Process process = new ProcessBuilder(
                javaExecutable,
                "-Xmx64m",
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "-Dlogback.configurationFile=" + loggingConfiguration,
                "-cp",
                testClasspath,
                ServerMonitorShutdownHookStressProbe.class.getName())
                .redirectErrorStream(true)
                .start();

        boolean completed = process.waitFor(60, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(completed, "Bounded-heap allocation probe timed out. Output:\n" + output);
        assertEquals(0, process.exitValue(), "Bounded-heap allocation probe failed. Output:\n" + output);
        assertTrue(output.contains(SUCCESS_MARKER), "Bounded-heap probe did not report completion. Output:\n" + output);
    }
}
