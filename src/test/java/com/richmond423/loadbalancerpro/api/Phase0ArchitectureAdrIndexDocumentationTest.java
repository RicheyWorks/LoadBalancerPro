package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class Phase0ArchitectureAdrIndexDocumentationTest {
    private static final Path DOC = Path.of("docs/PHASE_0_ARCHITECTURE_ADR_INDEX.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path ALIGNMENT = Path.of("docs/ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md");
    private static final Path LASE_BOUNDARY = Path.of("docs/LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md");
    private static final Path EXTERNAL_SIGNAL = Path.of("docs/EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md");
    private static final Path WORKLOAD_PROFILE =
            Path.of("docs/WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void phase0AdrIndexDocExistsAndStatesNoImplementation() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Phase 0 Architecture ADR Index",
                "ADR index only, not implementation",
                "This is docs/test only.",
                "Proposed ADRs are planning only until separately written/approved",
                "No package moves are introduced.",
                "No runtime LASE enforcement is introduced.",
                "No EvidencePacket implementation is introduced.",
                "No WorkloadProfile implementation is introduced.",
                "No ScenarioGenerator implementation is introduced.",
                "No ExternalSignalPort implementation is introduced.",
                "No production readiness claim is made.",
                "No production certification claim is made.",
                "no runtime architecture changes",
                "no source scanning logic",
                "no ArchUnit or package-boundary tooling",
                "no Maven build changes",
                "no API behavior changes")) {
            assertTrue(doc.contains(expected), "ADR index should state boundary " + expected);
        }
    }

    @Test
    void phase0AdrIndexLinksAlignmentIndexAndIncludesProposedAdrSet() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md",
                "Proposed ADR Set",
                "ADR-0001 layered architecture boundary",
                "ADR-0002 LASE integration model",
                "ADR-0003 evidence as first-class artifact",
                "ADR-0004 workload realism and scenario modeling",
                "ADR-0005 safety boundaries and guardrails",
                "ADR-0006 live allocation vs shadow evaluation separation",
                "ADR-0007 reviewer evidence and trust model",
                "ADR-0008 future external signal context boundaries")) {
            assertTrue(doc.contains(expected), "ADR index should include proposed ADR item " + expected);
        }
    }

    @Test
    void eachAdrCarriesDecisionMetadataAndBoundaries() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Decision area:",
                "Why it matters:",
                "Existing related docs:",
                "Current status:",
                "Implementation boundary:",
                "Safe next documentation slice:",
                "Explicit non-claims:",
                "must not move packages in this sprint",
                "must not claim package-boundary enforcement",
                "must not implement runtime LASE enforcement",
                "must not implement EvidencePacket",
                "must not implement WorkloadProfile",
                "must not implement ScenarioGenerator",
                "must not mutate cloud resources",
                "must not change routing behavior, scoring behavior, strategy behavior, proxy behavior",
                "must not implement external clients, HTTP calls, signal ingestion")) {
            assertTrue(doc.contains(expected), "ADR index should include metadata/boundary item " + expected);
        }
    }

    @Test
    void phase0AdrIndexIncludesReadinessChecklistAndSequencingGuidance() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "ADR Readiness Checklist",
                "ADR Sequencing Guidance",
                "decision area is clearly named",
                "related existing docs are linked",
                "current status is labeled as current behavior, documented only, planning only, or future-only",
                "implementation boundary is explicit",
                "safe next documentation slice is identified",
                "explicit non-claims are present",
                "future enforcement remains separate unless explicitly approved later",
                "The sequence is advisory. It does not authorize implementation.")) {
            assertTrue(doc.contains(expected), "ADR index should include readiness/sequencing item " + expected);
        }
    }

    @Test
    void phase0AdrIndexLinksRelatedArchitectureDocs() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md",
                "LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md",
                "LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md",
                "LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md",
                "LASE_BOUNDARY_NAMING_GUARD_PLAN.md",
                "LASE_NAMING_GUARD_INVENTORY.md",
                "REVIEWER_TRUST_MAP.md",
                "ENTERPRISE_READINESS_AUDIT.md",
                "SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md",
                "SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md",
                "SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md",
                "WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md",
                "THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md",
                "EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md")) {
            assertTrue(doc.contains(expected), "ADR index should link " + expected);
        }

        for (Path path : List.of(ALIGNMENT, LASE_BOUNDARY, EXTERNAL_SIGNAL, WORKLOAD_PROFILE)) {
            assertTrue(read(path).contains("PHASE_0_ARCHITECTURE_ADR_INDEX.md"),
                    path + " should link the Phase 0 ADR index");
        }
    }

    @Test
    void reviewerEntryPointsLinkPhase0AdrIndexAsPlanningOnlyReference() throws Exception {
        for (Path path : List.of(README, TRUST_MAP, AUDIT)) {
            assertTrue(read(path).contains("PHASE_0_ARCHITECTURE_ADR_INDEX.md"),
                    path + " should link the Phase 0 ADR index");
        }

        assertTrue(read(README).contains("initial planning-only ADR set"));
        assertTrue(read(TRUST_MAP).contains("docs/test-only Phase 0 ADR index"));
        assertTrue(read(AUDIT).contains("docs/test-only Phase 0 architecture ADR index"));
    }

    @Test
    void phase0AdrIndexKeepsFutureImplementationBoundariesExplicit() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Explicit Future-Only Implementation Boundaries",
                "proposed ADRs are planning only until separately written/approved",
                "future architecture implementation would require separate scoped PR review",
                "no package-boundary enforcement is active",
                "no ExternalSignalPort implementation exists",
                "no WorkloadProfile implementation exists",
                "no EvidencePacket implementation exists",
                "no ScenarioGenerator implementation exists",
                "no source-name guard implementation exists",
                "no allowlist implementation exists",
                "no report generation is added",
                "no telemetry/storage/persistence implementation is added",
                "no production readiness claim, production certification claim, live-cloud validation claim, or real-tenant validation claim is made")) {
            assertTrue(doc.contains(expected), "ADR index should include future-only boundary " + expected);
        }
    }

    @Test
    void phase0AdrIndexDocsAvoidImplementationAndProductionOverclaims() throws Exception {
        for (Path path : List.of(DOC, README, TRUST_MAP, AUDIT, ALIGNMENT, LASE_BOUNDARY, EXTERNAL_SIGNAL,
                WORKLOAD_PROFILE)) {
            String normalized = read(path).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "runtime-enforced lase boundary is now active",
                    "runtime-enforced lase boundary is implemented",
                    "package-boundary enforcement is now active",
                    "externalsignalport implementation now exists",
                    "externalsignalport implementation is active",
                    "workloadprofile implementation now exists",
                    "workloadprofile implementation is active",
                    "evidencepacket implementation now exists",
                    "scenariogenerator implementation now exists",
                    "phase 0 adr index implements",
                    "phase 0 adr index enforces",
                    "architecture adr index implements",
                    "architecture adr index enforces",
                    "production readiness is proven",
                    "production certification is proven",
                    "live-cloud validation is complete",
                    "real-tenant validation is complete")) {
                assertFalse(normalized.contains(forbidden), path + " must not overclaim " + forbidden);
            }
        }
    }

    @Test
    void sprintDoesNotIntroduceArchUnitDependency() throws Exception {
        assertFalse(read(POM).toLowerCase(Locale.ROOT).contains("archunit"),
                "this sprint must not add an ArchUnit dependency or build change");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
