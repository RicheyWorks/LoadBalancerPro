package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class EnterpriseLabDecisionReplayEvidenceClosurePacketDocumentationTest {
    private static final Path DOC =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_CLOSURE_PACKET.md");

    @Test
    void closurePacketDocDefinesReadOnlyDerivedBoundaries() throws Exception {
        String doc = Files.readString(DOC, StandardCharsets.UTF_8);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Evidence Closure Packet"));
        assertTrue(doc.contains("`POST /api/routing/compare`"));
        assertTrue(doc.contains("`decisionReplayEvidenceReviewerClosurePacket`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceReviewerClosureSummary`"));
        assertTrue(doc.contains("`decisionReplayEvidenceReviewerClosureRollup`"));
        assertTrue(doc.contains("`decisionReplayEvidenceReviewerClosureChecklist`"));
        assertTrue(doc.contains("packet version `v1`"));
        assertTrue(doc.contains("`closureSummary`"));
        assertTrue(doc.contains("`closureRollup`"));
        assertTrue(doc.contains("`closureChecklist`"));
        assertTrue(doc.contains("`scenarioReplayBoundary`"));
        assertTrue(doc.contains("`notProvenBoundaries`"));
        assertTrue(doc.contains("PASS"));
        assertTrue(doc.contains("WARN"));
        assertTrue(doc.contains("UNKNOWN"));
        assertTrue(doc.contains("COMPLETE"));
        assertTrue(doc.contains("PARTIAL"));
        assertTrue(doc.contains("reviewer-ready boolean"));
        assertTrue(doc.contains("deterministic summary text"));
        assertTrue(doc.contains("reviewer guidance strings"));
        assertTrue(doc.contains("`not replay proof`"));
        assertTrue(doc.contains("`not scoring proof`"));
        assertTrue(doc.contains("`not correctness validation`"));
        assertTrue(doc.contains("`not production readiness`"));
        assertTrue(doc.contains("`not production certification`"));
        assertTrue(doc.contains("`not guaranteed replay`"));
        assertTrue(doc.contains("`not production validation`"));
        assertTrue(doc.contains("does not inspect raw server input"));
        assertTrue(doc.contains("It does not use reflection."));
        assertTrue(doc.contains("is not an export, share, download, PDF, ZIP, or file-generation packet"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist reviewer closure packet data or audit logs server-side"));
        assertTrue(doc.contains("does not export, download, upload, or share reviewer closure packet data"));
        assertTrue(doc.contains("does not change routing behavior"));
        assertTrue(doc.contains("does not change strategy selection"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not generate a new fingerprint, hash, SHA, or UUID"));
        assertTrue(doc.contains("does not add telemetry or storage"));
        assertTrue(doc.contains("does not add external calls, scripts, or CDNs"));
        assertTrue(doc.contains("does not add server-side export/PDF/ZIP/file generation"));
        assertTrue(doc.contains("Scenario replay keeps its stripped routing-result posture"));
        assertTrue(doc.contains("not promoted into scenario replay output as replay proof"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("quality ranking is proven"));
        assertFalse(normalized.contains("approval is granted"));
        assertFalse(normalized.contains("correctness validation is proven"));
        assertFalse(normalized.contains("downloadable proof is generated"));
    }
}
