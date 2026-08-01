package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ReverseProxyResponseStreamingMemoryTest {
    @Test
    @Timeout(120)
    void twoGigabyteVirtualResponseStreamsUnder128MegabyteHeap() throws Exception {
        String executable = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "java.exe"
                : "java";
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", executable).toString();
        String testClasspath = System.getProperty(
                "surefire.test.class.path",
                System.getProperty("java.class.path"));
        Process process = new ProcessBuilder(
                javaExecutable,
                "-Xmx128m",
                "-cp",
                testClasspath,
                ReverseProxyResponseStreamingMemoryProbe.class.getName())
                .redirectErrorStream(true)
                .start();

        boolean completed = process.waitFor(90, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(completed, "bounded response streaming probe timed out: " + output);
        assertEquals(0, process.exitValue(), "bounded response streaming probe failed: " + output);
        assertTrue(output.contains(ReverseProxyResponseStreamingMemoryProbe.SUCCESS_MARKER),
                "bounded response streaming probe did not report success: " + output);
    }
}
