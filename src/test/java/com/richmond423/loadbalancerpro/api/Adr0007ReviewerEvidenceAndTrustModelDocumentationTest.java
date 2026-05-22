package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class Adr0007ReviewerEvidenceAndTrustModelDocumentationTest {
    private static final Path ADR = Path.of("docs/adr/ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md");
    private static final Path ADR_0001 = Path.of("docs/adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md");
    private static final Path ADR_0002 = Path.of("docs/adr/ADR-0002_LASE_INTEGRATION_MODEL.md");
    private static final Path ADR_0003 = Path.of("docs/adr/ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md");
    private static final Path ADR_0004 = Path.of("docs/adr/ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md");
    private static final Path ADR_0005 = Path.of("docs/adr/ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md");
    private static final Path ADR_0006 =
            Path.of("docs/adr/ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PHASE0_INDEX = Path.of("docs/PHASE_0_ARCHITECTURE_ADR_INDEX.md");
    private static final Path ALIGNMENT = Path.of("docs/ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md");
    private static final Path LASE_CONTRACT = Path.of("docs/LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md");
    private static final Path WORKLOAD_CONTRACT =
            Path.of("docs/WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md");
    private static final Path EXTERNAL_SIGNAL_CONTRACT = Path.of("docs/EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void adr0007DocumentExistsAndStatesPlanningOnlyBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "# ADR-0007 Reviewer Evidence And Trust Model",
                "Proposed / planning-only.",
                "Decision type: architecture planning.",
                "Implementation status: not implemented.",
                "This ADR is planning-only.",
                "This does not implement reviewer portals.",
                "This does not implement reviewer dashboards.",
                "This does not implement reviewer APIs.",
                "This does not add evidence generation or report generation.",
                "This does not add replay execution.",
                "This does not add storage, persistence, telemetry, or audit log implementation.",
                "This does not add filesystem-writing behavior.",
                "This does not add export, upload, download, PDF, ZIP, PR comment, or report artifact behavior.",
                "This does not add runtime safety enforcement or runtime LASE enforcement.",
                "This does not change routing, scoring, strategy, proxy, API, config, Docker, CI, release, signing, registry, governance, or production behavior.",
                "This does not claim production readiness, production certification, live-cloud validation, or real-tenant validation.")) {
            assertTrue(adr.contains(expected), "ADR-0007 should state planning boundary " + expected);
        }
    }

    @Test
    void adr0007IncludesReviewerEvidencePurposeAndTrustModel() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Reviewer Evidence Purpose",
                "help reviewers understand adaptive-routing decisions",
                "avoid black-box trust",
                "separate observed facts from inferred/hypothetical claims",
                "show what was proven and what was not proven",
                "support operator review before autonomy",
                "support future enterprise evaluation without overclaiming production readiness",
                "Reviewer Trust Model",
                "decision evidence",
                "policy gate evidence",
                "signal provenance evidence",
                "rejected-option evidence",
                "safety-mode evidence",
                "replay/scenario evidence",
                "uncertainty and not-proven boundaries",
                "manual/operator approval requirements",
                "deterministic evidence expectations")) {
            assertTrue(adr.contains(expected), "ADR-0007 should include reviewer trust item " + expected);
        }
    }

    @Test
    void adr0007IncludesEvidenceExplanationExpectations() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Evidence Explanation Expectations",
                "what decision was considered",
                "which option was selected",
                "which options were rejected",
                "which signals were used",
                "signal source/provenance",
                "which policy checks passed/failed",
                "current safety mode",
                "why traffic was or was not allowed to change",
                "known uncertainty",
                "explicit not-proven boundaries",
                "operator-facing explanation",
                "manual/operator approval requirements where applicable")) {
            assertTrue(adr.contains(expected), "ADR-0007 should include explanation item " + expected);
        }
    }

    @Test
    void adr0007IncludesTrustBoundariesAndNonGoals() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Trust Boundaries",
                "is not production certification",
                "is not live-cloud validation",
                "is not real-tenant validation",
                "is not proof of correctness",
                "is not proof of replay accuracy unless replay is separately implemented and validated",
                "is not proof of safe autonomous production traffic shifting",
                "must not hide unsafe assumptions",
                "must not imply runtime enforcement where none exists",
                "Safety And Non-Goals",
                "no reviewer portal implementation",
                "no reviewer dashboard implementation",
                "no reviewer API implementation",
                "no EvidencePacket implementation",
                "no EvidenceAssembler implementation",
                "no replay execution",
                "no storage or persistence",
                "no filesystem-writing implementation",
                "no export, upload, download, PDF, or ZIP behavior",
                "no autonomous production traffic shifting",
                "Not-Proven Boundaries",
                "ADR-0007 is proposed/planning-only")) {
            assertTrue(adr.contains(expected), "ADR-0007 should include trust boundary/non-goal " + expected);
        }
    }

    @Test
    void adr0007DocumentsPriorAdrRelationshipsAndNorthStarVision() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Relationship To Prior ADRs",
                "ADR-0001 architecture boundary",
                "ADR-0002 LASE boundary",
                "ADR-0003 external signal/source boundary",
                "ADR-0004 workload realism/scenario modeling",
                "ADR-0005 safety boundaries and guardrails",
                "ADR-0006 evidence packet and replay boundary model",
                "Relationship To North-Star Vision",
                "future datacenter adaptive traffic control",
                "partial degradation and recovery behavior",
                "tail latency under pressure",
                "safer adaptive routing requires guardrails before autonomy",
                "explainability and auditability should come before hidden traffic-changing authority",
                "trusted adaptive routing should avoid black-box routing decisions")) {
            assertTrue(adr.contains(expected), "ADR-0007 should include relationship/north-star item " + expected);
        }
    }

    @Test
    void adr0007LinksRelatedDocsAndReviewerEntryPoints() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "../PHASE_0_ARCHITECTURE_ADR_INDEX.md",
                "ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md",
                "ADR-0002_LASE_INTEGRATION_MODEL.md",
                "ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md",
                "ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md",
                "ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md",
                "ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md",
                "../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md",
                "../REVIEWER_TRUST_MAP.md",
                "../ENTERPRISE_READINESS_AUDIT.md",
                "../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md",
                "../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md",
                "../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md")) {
            assertTrue(adr.contains(expected), "ADR-0007 should link " + expected);
        }

        for (Path path : List.of(README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001, ADR_0002,
                ADR_0003, ADR_0004, ADR_0005, ADR_0006, LASE_CONTRACT, WORKLOAD_CONTRACT,
                EXTERNAL_SIGNAL_CONTRACT)) {
            assertTrue(read(path).contains("ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md"),
                    path + " should link ADR-0007");
        }
    }

    @Test
    void adr0007DocsAvoidImplementationAndProductionOverclaims() throws Exception {
        for (Path path : List.of(ADR, README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001, ADR_0002,
                ADR_0003, ADR_0004, ADR_0005, ADR_0006, LASE_CONTRACT, WORKLOAD_CONTRACT,
                EXTERNAL_SIGNAL_CONTRACT)) {
            String normalized = read(path).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "adr-0007 is approved",
                    "adr-0007 is accepted",
                    "adr-0007 implements",
                    "adr-0007 enforces",
                    "reviewer portal is implemented",
                    "reviewer dashboard is implemented",
                    "reviewer api is implemented",
                    "reviewer portal/dashboard/api implementation is active",
                    "evidence generation is implemented",
                    "evidence/report generation is implemented",
                    "report generation is active",
                    "replay execution is implemented",
                    "storage/persistence is active",
                    "filesystem-writing implementation is active",
                    "export/upload/download/pdf/zip implementation is active",
                    "runtime enforcement is implemented by adr-0007",
                    "autonomous production traffic shifting is implemented",
                    "reviewer evidence certifies production",
                    "reviewer evidence proves live-cloud validation",
                    "reviewer evidence proves real-tenant validation",
                    "production-ready now",
                    "production certified",
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
