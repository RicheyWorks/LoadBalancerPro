package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EnterpriseLabCockpitXssSafetyTest {
    private static final Path ENTERPRISE_LAB =
            Path.of("src/main/resources/static/enterprise-lab.html");
    private static final Path REVIEWER =
            Path.of("src/main/resources/static/enterprise-lab-reviewer.html");
    private static final Path README = Path.of("README.md");

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
    void reviewerRendersEverySummaryPathThroughTextDomNodes() throws Exception {
        String page = Files.readString(REVIEWER);

        assertNoHtmlStringSink(page);
        assertTrue(page.contains("function appendSummaryValue("));
        assertTrue(page.contains("element.replaceChildren("));
        assertTrue(page.contains("artifact.evidenceDirectory"));
        assertTrue(page.contains("evidence.readinessAuditPath"));
        assertTrue(page.contains("evidence.reviewerTrustMapPath"));
        assertTrue(page.contains("evidence.securityPosturePath"));
        assertTrue(page.contains("evidence.governanceHardeningPath"));
        assertTrue(page.contains("document.createTextNode("));
    }

    @Test
    void readmeRecordsTheBoundedSafeRenderingContract() throws Exception {
        String readme = Files.readString(README);

        assertTrue(readme.contains(
                "server-derived scenario, policy, audit, metrics, and reviewer-path"));
        assertTrue(readme.contains("through DOM text nodes"));
        assertTrue(readme.contains("Hostile markup"));
        assertTrue(readme.contains("renders inert"));
    }

    private static void assertNoHtmlStringSink(String page) {
        assertFalse(page.contains("innerHTML"), "server-derived values must not enter innerHTML");
        assertFalse(page.contains("outerHTML"), "server-derived values must not enter outerHTML");
        assertFalse(page.contains("insertAdjacentHTML"), "server-derived values must not enter insertAdjacentHTML");
        assertFalse(page.contains("document.write"), "server-derived values must not enter document.write");
    }
}
