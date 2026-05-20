package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class EnterpriseLabDecisionReplayEvidenceReviewerClosureSummaryDocumentationTest {
    private static final Path DOC =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_CLOSURE_SUMMARY.md");

    @Test
    void reviewerClosureSummaryDocDefinesReadOnlyDerivedBoundaries() throws Exception {
        String doc = Files.readString(DOC, StandardCharsets.UTF_8);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Evidence Reviewer Closure Summary"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceReviewerClosureSummary`"));
        assertTrue(doc.contains("decision-replay-evidence-reviewer-closure-summary/v1"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceReviewerHandoffSummary`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceReviewerGuidance`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceReviewerSnapshot`"));
        assertTrue(doc.contains("It does not inspect raw server input or raw request payload data."));
        assertTrue(doc.contains("It does not use reflection."));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist reviewer closure summary data or audit logs server-side"));
        assertTrue(doc.contains("does not export, download, upload, or share reviewer closure summary data"));
        assertTrue(doc.contains("does not change routing behavior"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(doc.contains("does not infer hidden scoring"));
        assertTrue(doc.contains("does not generate a new fingerprint"));
        assertTrue(doc.contains("not production validation"));
        assertTrue(doc.contains("not production certification"));
        assertTrue(doc.contains("not live-cloud proof"));
        assertTrue(doc.contains("not real-tenant proof"));
        assertTrue(doc.contains("not SLA/SLO proof"));
        assertTrue(doc.contains("not registry"));
        assertTrue(doc.contains("not signing proof"));
        assertTrue(doc.contains("not governance application proof"));
        assertTrue(doc.contains("not exact production scoring proof"));
        assertTrue(doc.contains("not guaranteed replay"));
        assertTrue(doc.contains("not cryptographic production proof"));
        assertTrue(doc.contains("not production traffic validation"));
        assertTrue(doc.contains("It is not an approval, remediation, enforcement decision"));
        assertTrue(doc.contains("not invent a selected candidate"));
        assertTrue(doc.contains("candidate set"));
        assertTrue(doc.contains("closest"));
        assertTrue(doc.contains("score gap"));
        assertTrue(doc.contains("largest delta factor"));
        assertTrue(doc.contains("fingerprint"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("quality ranking is proven"));
        assertFalse(normalized.contains("approval is granted"));
        assertFalse(normalized.contains("correctness validation is proven"));
    }
}
