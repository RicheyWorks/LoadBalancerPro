package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class OperatorPackagingPolishTest {
    private static final Path PACKAGE_NAMING_DOC = Path.of("docs/PACKAGE_NAMING.md");
    private static final Path OPERATOR_PACKAGING_DOC = Path.of("docs/OPERATOR_PACKAGING.md");
    private static final Path LAUNCHER_DOC = Path.of("docs/PROXY_DEMO_FIXTURE_LAUNCHER.md");
    private static final Path DEMO_STACK_DOC = Path.of("docs/PROXY_DEMO_STACK.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");
    private static final Path DEFAULT_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final List<Path> REAL_BACKEND_EXAMPLES = List.of(
            Path.of("docs/examples/proxy/application-proxy-real-backend-example.properties"),
            Path.of("docs/examples/proxy/application-proxy-real-backend-weighted-example.properties"),
            Path.of("docs/examples/proxy/application-proxy-real-backend-failover-example.properties"));

    private static final Pattern PUBLIC_EXTERNAL_URL =
            Pattern.compile("https?://(?!127\\.0\\.0\\.1(?::|/|$)|localhost(?::|/|$))[^\\s\"'`]+");
    private static final Pattern SECRET_ASSIGNMENT =
            Pattern.compile("(?im)^\\s*[^#\\r\\n]*(password|secret|token|api[-_]?key|x-api-key)\\s*=");

    @Test
    void packageNamingDecisionDocumentsStableLegacyNamespace() throws Exception {
        String doc = read(PACKAGE_NAMING_DOC);

        assertTrue(doc.contains("RicheyWorks/LoadBalancerPro"));
        assertTrue(doc.contains("com.richmond423.loadbalancerpro"));
        assertTrue(doc.toLowerCase(Locale.ROOT).contains("stable legacy namespace"));
        assertTrue(doc.toLowerCase(Locale.ROOT).contains("rename is deferred"));
        assertTrue(doc.toLowerCase(Locale.ROOT).contains("does not change runtime behavior"));
        assertTrue(doc.toLowerCase(Locale.ROOT).contains("not a functional defect"));
        assertNoUnsafePolishContent(doc, PACKAGE_NAMING_DOC);
    }

    @Test
    void launcherDocsExposeMavenExecAndClasspathFallbackWithoutChangingDefaultMain() throws Exception {
        String pom = read(POM);
        String operatorPackaging = read(OPERATOR_PACKAGING_DOC);
        String launcherDoc = read(LAUNCHER_DOC);
        String demoStack = read(DEMO_STACK_DOC);

        assertTrue(pom.contains("<artifactId>exec-maven-plugin</artifactId>"));
        assertTrue(pom.contains("<artifactId>spring-boot-maven-plugin</artifactId>"));
        assertTrue(pom.contains("<mainClass>com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication</mainClass>"));
        assertTrue(operatorPackaging.contains("compile exec:java"));
        assertTrue(operatorPackaging.contains("-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher"));
        assertTrue(operatorPackaging.contains("-Dexec.args=--mode round-robin"));
        assertTrue(operatorPackaging.contains("java -cp target/classes"));
        assertTrue(operatorPackaging.contains("target/LoadBalancerPro-2.5.0.jar"));
        assertTrue(launcherDoc.contains("compile exec:java"));
        assertTrue(launcherDoc.contains("ProxyDemoFixtureLauncher"));
        assertTrue(demoStack.contains("compile exec:java"));
        assertTrue(demoStack.contains("java -cp target/classes"));
        assertNoUnsafePolishContent(operatorPackaging, OPERATOR_PACKAGING_DOC);
        assertNoUnsafePolishContent(launcherDoc, LAUNCHER_DOC);
    }

    @Test
    void realBackendExamplesAreLocalPlaceholdersOnly() throws Exception {
        for (Path example : REAL_BACKEND_EXAMPLES) {
            String content = read(example);
            assertTrue(content.contains("loadbalancerpro.proxy.enabled=true"),
                    example + " should be an explicit opt-in example");
            assertTrue(content.contains("http://localhost:9001"),
                    example + " should use a local backend-a placeholder");
            assertTrue(content.contains("http://localhost:9002"),
                    example + " should use a local backend-b placeholder");
            assertTrue(content.contains("loadbalancerpro.proxy.health-check.enabled=true"),
                    example + " should document health-check behavior");
            assertTrue(content.contains("loadbalancerpro.proxy.retry.enabled=false"),
                    example + " should keep retries off unless intentionally changed");
            assertTrue(content.contains("loadbalancerpro.proxy.cooldown.enabled=false"),
                    example + " should keep cooldown off unless intentionally changed");
            assertNoUnsafePolishContent(content, example);
        }

        assertTrue(read(REAL_BACKEND_EXAMPLES.get(0)).contains("loadbalancerpro.proxy.strategy=ROUND_ROBIN"));
        String weighted = read(REAL_BACKEND_EXAMPLES.get(1));
        assertTrue(weighted.contains("loadbalancerpro.proxy.strategy=WEIGHTED_ROUND_ROBIN"));
        assertTrue(weighted.contains("loadbalancerpro.proxy.upstreams[0].weight=3.0"));
        assertTrue(weighted.contains("loadbalancerpro.proxy.upstreams[1].weight=1.0"));
        String failover = read(REAL_BACKEND_EXAMPLES.get(2));
        assertTrue(failover.contains("loadbalancerpro.proxy.health-check.interval=5s"));
    }

    @Test
    void defaultProxyStaysDisabledAndJavaFxIsOptionalForOperatorPath() throws Exception {
        String defaults = read(DEFAULT_PROPERTIES);
        String operatorPackaging = read(OPERATOR_PACKAGING_DOC);
        String readme = read(README);

        assertTrue(defaults.contains("loadbalancerpro.proxy.enabled=false"));
        assertFalse(defaults.contains("loadbalancerpro.proxy.enabled=true"));
        assertTrue(operatorPackaging.contains("JavaFX UI support exists"));
        assertTrue(operatorPackaging.contains("do not require JavaFX"));
        assertTrue(readme.contains("JavaFX UI support is optional"));
        assertTrue(readme.contains("API, proxy, CLI, Java fixture launcher, and static browser workflows do not require JavaFX"));
    }

    @Test
    void packagingDocsPointToRealBackendExamplesAndAvoidUnsafeClaims() throws Exception {
        String operatorPackaging = read(OPERATOR_PACKAGING_DOC);
        String demoStack = read(DEMO_STACK_DOC);
        String readme = read(README);

        assertTrue(operatorPackaging.contains("docs/examples/proxy/application-proxy-real-backend-example.properties"));
        assertTrue(operatorPackaging.contains("docs/examples/proxy/application-proxy-real-backend-weighted-example.properties"));
        assertTrue(operatorPackaging.contains("docs/examples/proxy/application-proxy-real-backend-failover-example.properties"));
        assertTrue(demoStack.contains("Real-Backend Example Profiles"));
        assertTrue(readme.contains("OPERATOR_PACKAGING.md"));
        assertTrue(readme.contains("PACKAGE_NAMING.md"));
        assertNoUnsafePolishContent(demoStack, DEMO_STACK_DOC);
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void assertNoUnsafePolishContent(String content, Path source) {
        String normalized = content.toLowerCase(Locale.ROOT);
        assertFalse(PUBLIC_EXTERNAL_URL.matcher(content).find(), source + " should not contain public URLs");
        assertFalse(SECRET_ASSIGNMENT.matcher(content).find(), source + " should not contain secret assignments");
        assertFalse(normalized.contains("new cloudmanager"), source + " should not construct CloudManager");
        assertFalse(normalized.contains("cloudmanager("), source + " should not construct CloudManager");
        assertFalse(normalized.contains("amazonaws"), source + " should not target AWS endpoints");
        assertFalse(normalized.contains("localstorage"), source + " should not use browser storage");
        assertFalse(normalized.contains("sessionstorage"), source + " should not use browser storage");
        assertFalse(normalized.contains("production-grade"), source + " should not add production-grade claims");
        assertFalse(normalized.contains("certification proof"), source + " should not add certification claims");
        assertFalse(normalized.contains("identity proof"), source + " should not add identity claims");
    }
}
