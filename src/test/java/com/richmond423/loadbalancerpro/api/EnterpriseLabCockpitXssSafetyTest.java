package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EnterpriseLabCockpitXssSafetyTest {
    private static final Path ENTERPRISE_LAB =
            Path.of("src/main/resources/static/enterprise-lab.html");
    private static final Path EVIDENCE_VIEWER_LIBRARY =
            Path.of("src/main/resources/static/evidence-viewer-lib.js");

    @Test
    void enterpriseLabRendersEveryServerFieldThroughTextDomNodes() throws Exception {
        String page = Files.readString(ENTERPRISE_LAB);

        assertNoHtmlStringSink(page);
        assertTrue(page.contains("function appendTextCell("));
        assertTrue(page.contains("function createPills("));
        assertTrue(page.contains("function renderKeyValueTable("));
        assertTrue(page.contains("appendTextCell(row, \"th\", label);"));
        assertTrue(page.contains("value instanceof Node"));
        assertFalse(page.contains("typeof value.nodeType"));
        assertTrue(page.contains("scenario.displayName"));
        assertTrue(page.contains("scenario.expectedGuardrails"));
        assertTrue(page.contains("policy.lastGuardrailReason"));
        assertTrue(page.contains("policy.warning"));
        assertTrue(page.contains("event.eventId"));
        assertTrue(page.contains("event.guardrailReasons"));
        assertTrue(page.contains("event.rollbackReason"));
        assertTrue(page.contains("metrics.warning"));
        assertTrue(page.contains("document.createTextNode("));
    }

    @Test
    void evidenceViewerRendersApiValuesThroughTextDomNodes() throws Exception {
        String page = Files.readString(EVIDENCE_VIEWER_LIBRARY);

        assertNoHtmlStringSink(page);
        assertTrue(page.contains("function createElement("));
        assertTrue(page.contains("element.textContent = text"));
        assertTrue(page.contains("container.textContent = scalarText(value)"));
        assertTrue(page.contains("document.getElementById(\"raw-output\").textContent"));
        assertTrue(page.contains("Object.entries(payload)"));
    }


    private static void assertNoHtmlStringSink(String page) {
        assertFalse(page.contains("innerHTML"), "server-derived values must not enter innerHTML");
        assertFalse(page.contains("outerHTML"), "server-derived values must not enter outerHTML");
        assertFalse(page.contains("insertAdjacentHTML"), "server-derived values must not enter insertAdjacentHTML");
        assertFalse(page.contains("document.write"), "server-derived values must not enter document.write");
    }
}
