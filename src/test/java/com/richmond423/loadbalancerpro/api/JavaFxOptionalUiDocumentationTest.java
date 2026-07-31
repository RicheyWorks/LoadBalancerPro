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
    void retirementDocStatesDesktopSourceAndDependencyAreGone() throws Exception {
        String doc = read(JAVAFX_DOC);

        assertTrue(doc.contains("# JavaFX Desktop UI Retirement"));
        assertTrue(doc.contains("no longer includes or distributes the JavaFX desktop simulator"));
        assertTrue(doc.contains("`org.openjfx:javafx-controls` Maven dependency were removed"));
        assertTrue(doc.contains("no `javafx.version`, `org.openjfx`, or `javafx-controls` declaration"));
        assertTrue(doc.contains("no JavaFX imports or JavaFX `Application` entry point"));
        assertTrue(doc.contains("There is no supported JavaFX launch command"));
    }

    @Test
    void retirementDocNamesMaintainedOperatorAlternativesAndCompatibilityBoundary() throws Exception {
        String doc = read(JAVAFX_DOC);

        assertTrue(doc.contains("com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication"));
        assertTrue(doc.contains("com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher"));
        assertTrue(doc.contains("/proxy-status.html"));
        assertTrue(doc.contains("/load-balancing-cockpit.html"));
        assertTrue(doc.contains("`com.richmond423.loadbalancerpro.gui.Command`"));
        assertTrue(doc.contains("JavaFX-free compatibility contract used by"));
        assertTrue(doc.contains("`CloudManager`"));
        assertTrue(doc.contains("CI continues to reject OpenJFX libraries"));
    }

    @Test
    void currentOperatorDocsLinkToRetirementDoc() throws Exception {
        assertTrue(read(README).contains("JAVAFX_OPTIONAL_UI.md"));
        assertTrue(read(MATRIX).contains("JAVAFX_OPTIONAL_UI.md"));
        assertTrue(read(PACKAGING).contains("JAVAFX_OPTIONAL_UI.md"));
        assertTrue(read(RUNBOOK).contains("JAVAFX_OPTIONAL_UI.md"));
        assertTrue(read(API_SECURITY).contains("JAVAFX_OPTIONAL_UI.md"));
        assertTrue(read(TESTING_COVERAGE).contains("JAVAFX_OPTIONAL_UI.md"));
    }

    @Test
    void currentDocsAvoidRestoringTheDesktopPathOrInflatingClaims() throws Exception {
        String combined = read(JAVAFX_DOC) + "\n" + read(README) + "\n" + read(MATRIX)
                + "\n" + read(PACKAGING) + "\n" + read(RUNBOOK)
                + "\n" + read(API_SECURITY) + "\n" + read(TESTING_COVERAGE);
        String normalized = combined.toLowerCase(Locale.ROOT);
        String javaFxDoc = read(JAVAFX_DOC).toLowerCase(Locale.ROOT);

        assertFalse(normalized.contains("javafx is optional"), "current docs must describe retirement, not optionality");
        assertFalse(normalized.contains("optional javafx"), "current docs must not advertise an optional launch path");
        assertFalse(normalized.contains("launch javafx"), "current docs must not advertise a JavaFX launcher");
        assertFalse(normalized.contains("new cloudmanager"), "docs must not construct CloudManager");
        assertFalse(normalized.contains("cloudmanager("), "docs must not construct CloudManager");
        assertFalse(javaFxDoc.contains("production-grade"), "retirement docs must not add production-grade claims");
        assertFalse(javaFxDoc.contains("benchmark proof"), "retirement docs must not add benchmark claims");
        assertFalse(javaFxDoc.contains("certification proof"), "retirement docs must not add certification claims");
        assertFalse(javaFxDoc.contains("identity proof"), "retirement docs must not add identity claims");
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
