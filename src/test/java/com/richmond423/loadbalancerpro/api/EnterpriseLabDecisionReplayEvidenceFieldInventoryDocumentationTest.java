package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class EnterpriseLabDecisionReplayEvidenceFieldInventoryDocumentationTest {
    private static final Path DOC =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY.md");

    @Test
    void fieldInventoryDocIsLabOnlyReadOnlyAndNotProductionProof() throws Exception {
        String doc = read(DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Evidence Field Inventory"));
        assertTrue(doc.contains("LoadBalancerPro is an Enterprise Lab Cockpit"));
        assertTrue(doc.contains("read-only lab field-inventory lane"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceFieldInventory`"));
        assertTrue(doc.contains("`decision-replay-evidence-field-inventory/v1`"));
        assertTrue(doc.contains("derived only from already-built compare"));
        assertTrue(doc.contains("decisionReplayEvidenceBoundarySummary"));
        assertTrue(doc.contains("does not use reflection"));
        assertTrue(doc.contains("does not generate a new fingerprint"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist field inventories or audit logs server-side"));
        assertTrue(doc.contains("does not export, download, or share field inventories"));
        assertTrue(doc.contains("does not change routing behavior"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(normalized.contains("not production certification"));
        assertTrue(normalized.contains("not live-cloud proof"));
        assertTrue(normalized.contains("not real-tenant proof"));
        assertTrue(normalized.contains("not sla/slo proof"));
        assertTrue(normalized.contains("not registry publication proof"));
        assertTrue(normalized.contains("not signing proof"));
        assertTrue(normalized.contains("not governance application proof"));
        assertTrue(normalized.contains("not exact production scoring proof"));
        assertTrue(normalized.contains("not cryptographic production proof"));
        assertTrue(normalized.contains("not guaranteed replay"));
        assertTrue(normalized.contains("not production traffic validation"));
    }

    @Test
    void fieldInventoryDocAvoidsUnsafeBehaviorAndOverclaims() throws Exception {
        String normalized = read(DOC).toLowerCase(Locale.ROOT);

        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("cryptographic production proof is proven"));
        assertFalse(normalized.contains("exact production scoring proof is proven"));
        assertFalse(normalized.contains("live-cloud proof is proven"));
        assertFalse(normalized.contains("real-tenant proof is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("download endpoint"));
        assertFalse(normalized.contains("share endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
        assertFalse(normalized.contains("field-inventory persistence is implemented"));
        assertFalse(normalized.contains("replay execution is implemented"));
        assertFalse(normalized.contains("what-if mutation is implemented"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
