package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ReverseProxyRequestStreamingMemoryTest {
    @Test
    @Timeout(90)
    void oneGigabyteVirtualChunkedBodyFailsAtTheCapUnder128MegabyteHeap() throws Exception {
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
                ReverseProxyRequestStreamingMemoryProbe.class.getName())
                .redirectErrorStream(true)
                .start();

        boolean completed = process.waitFor(60, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(completed, "bounded request streaming probe timed out: " + output);
        assertEquals(0, process.exitValue(), "bounded request streaming probe failed: " + output);
        assertTrue(output.contains(ReverseProxyRequestStreamingMemoryProbe.SUCCESS_MARKER),
                "bounded request streaming probe did not report success: " + output);
    }
}
