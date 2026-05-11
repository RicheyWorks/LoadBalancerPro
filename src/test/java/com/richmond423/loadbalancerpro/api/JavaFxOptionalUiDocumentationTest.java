package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class JavaFxOptionalUiDocumentationTest {
    private static final Path JAVAFX_DOC = Path.of("docs/JAVAFX_OPTIONAL_UI.md");
    private static final Path README = Path.of("README.md");
    private static final Path MATRIX = Path.of("docs/OPERATOR_INSTALL_RUN_MATRIX.md");
    private static final Path PACKAGING = Path.of("docs/OPERATOR_PACKAGING.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path API_SECURITY = Path.of("docs/API_SECURITY.md");
    private static final Path TESTING_COVERAGE = Path.of("docs/TESTING_COVERAGE.md");

    @Test
    void javaFxOptionalDocStatesApiProxyAndBrowserIndependence() throws Exception {
        String doc = read(JAVAFX_DOC);

        assertTrue(doc.contains("# JavaFX Optional UI"));
        assertTrue(doc.contains("JavaFX is optional"));
        assertTrue(doc.contains("not required for the Spring API"));
        assertTrue(doc.contains("not required for reverse proxy mode"));
        assertTrue(doc.contains("not required for `/proxy-status.html`"));
        assertTrue(doc.contains("not required for `/load-balancing-cockpit.html`"));
        assertTrue(doc.contains("not required for CI artifact verification"));
        assertTrue(doc.contains("not required for the operator distribution smoke kit"));
        assertTrue(doc.contains("not required for the proxy demo stack"));
    }

    @Test
    void javaFxOptionalDocCapturesPlatformAndHeadlessCaveats() throws Exception {
        String doc = read(JAVAFX_DOC);

        assertTrue(doc.contains("platform-specific JavaFX runtime configuration"));
        assertTrue(doc.contains("JavaFX-capable runtime environment and a desktop display"));
        assertTrue(doc.contains("Headless CI, containers, SSH sessions, and servers without a desktop display may not support the JavaFX UI."));
        assertTrue(doc.contains("desktop display"));
        assertTrue(doc.contains("No JavaFX-specific Maven plugin is bound to the default lifecycle."));
    }

    @Test
    void javaFxOptionalDocUsesRepoBackedLaunchGuidance() throws Exception {
        String doc = read(JAVAFX_DOC);

        assertTrue(doc.contains("com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication"));
        assertTrue(doc.contains("com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher"));
        assertTrue(doc.contains("LoadBalancerGUI"));
        assertTrue(doc.contains("mvn -q exec:java"));
        assertTrue(doc.contains("com.richmond423.loadbalancerpro.cli.LoadBalancerCLI"));
        assertTrue(doc.contains("interactive `Launch GUI` option"));
    }

    @Test
    void existingOperatorDocsLinkToJavaFxOptionalDoc() throws Exception {
        assertTrue(read(README).contains("JAVAFX_OPTIONAL_UI.md"));
        assertTrue(read(MATRIX).contains("JAVAFX_OPTIONAL_UI.md"));
        assertTrue(read(PACKAGING).contains("JAVAFX_OPTIONAL_UI.md"));
        assertTrue(read(RUNBOOK).contains("JAVAFX_OPTIONAL_UI.md"));
        assertTrue(read(API_SECURITY).contains("JAVAFX_OPTIONAL_UI.md"));
        assertTrue(read(TESTING_COVERAGE).contains("JAVAFX_OPTIONAL_UI.md"));
    }

    @Test
    void javaFxDocsAvoidRequiredOperatorPathConfusionAndUnsafeClaims() throws Exception {
        String combined = read(JAVAFX_DOC) + "\n" + read(README) + "\n" + read(MATRIX)
                + "\n" + read(PACKAGING) + "\n" + read(RUNBOOK);
        String normalized = combined.toLowerCase(Locale.ROOT);
        String javaFxDoc = read(JAVAFX_DOC).toLowerCase(Locale.ROOT);

        assertFalse(normalized.contains("javafx is required"), "JavaFX must not be documented as required");
        assertFalse(normalized.contains("requires javafx for proxy"), "Proxy path must not require JavaFX");
        assertFalse(normalized.contains("requires javafx for api"), "API path must not require JavaFX");
        assertFalse(normalized.contains("new cloudmanager"), "docs must not construct CloudManager");
        assertFalse(normalized.contains("cloudmanager("), "docs must not construct CloudManager");
        assertFalse(javaFxDoc.contains("production-grade"), "JavaFX docs must not add production-grade claims");
        assertFalse(javaFxDoc.contains("benchmark proof"), "JavaFX docs must not add benchmark claims");
        assertFalse(javaFxDoc.contains("certification proof"), "JavaFX docs must not add certification claims");
        assertFalse(javaFxDoc.contains("identity proof"), "JavaFX docs must not add identity claims");
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
