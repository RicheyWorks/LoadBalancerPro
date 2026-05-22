package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class Adr0006EvidencePacketAndReplayBoundaryModelDocumentationTest {
    private static final Path ADR = Path.of("docs/adr/ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md");
    private static final Path ADR_0001 = Path.of("docs/adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md");
    private static final Path ADR_0002 = Path.of("docs/adr/ADR-0002_LASE_INTEGRATION_MODEL.md");
    private static final Path ADR_0003 = Path.of("docs/adr/ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md");
    private static final Path ADR_0004 = Path.of("docs/adr/ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md");
    private static final Path ADR_0005 = Path.of("docs/adr/ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md");
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
    void adr0006DocumentExistsAndStatesPlanningOnlyBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "# ADR-0006 Evidence Packet And Replay Boundary Model",
                "Proposed / planning-only.",
                "Decision type: architecture planning.",
                "Implementation status: not implemented.",
                "This ADR is planning-only.",
                "This does not implement `EvidencePacket`.",
                "This does not implement `EvidenceAssembler`.",
                "This does not add replay execution.",
                "This does not add evidence generation or report generation.",
                "This does not add storage, persistence, telemetry, or audit log implementation.",
                "This does not add filesystem-writing behavior.",
                "This does not add export, upload, download, PDF, ZIP, PR comment, or report artifact behavior.",
                "This does not change routing, scoring, strategy, proxy, API, config, Docker, CI, release, signing, registry, governance, or production behavior.",
                "This does not claim production readiness, production certification, live-cloud validation, or real-tenant validation.")) {
            assertTrue(adr.contains(expected), "ADR-0006 should state planning boundary " + expected);
        }
    }

    @Test
    void adr0006IncludesEvidencePacketPurposeAndCategories() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "EvidencePacket Purpose",
                "capture decision context",
                "capture signals used",
                "capture selected option",
                "capture rejected options",
                "capture policy checks",
                "capture safety mode",
                "capture uncertainty and not-proven boundaries",
                "support reviewer/operator understanding",
                "avoid black-box adaptive routing",
                "Evidence Categories",
                "routing decision evidence",
                "policy gate evidence",
                "signal provenance evidence",
                "workload/scenario evidence",
                "safety boundary evidence",
                "rejected-option evidence",
                "operator-review evidence",
                "not-proven boundary evidence")) {
            assertTrue(adr.contains(expected), "ADR-0006 should include evidence item " + expected);
        }
    }

    @Test
    void adr0006IncludesEvidenceAssemblerAndReplayBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "EvidenceAssembler Boundary",
                "`EvidenceAssembler` is a future component boundary only.",
                "deterministic assembly expectations",
                "no secret/env leakage",
                "no unsafe external calls",
                "no hidden mutation",
                "no filesystem writes unless separately designed later",
                "no production traffic mutation",
                "Replay Boundary Model",
                "replay should be deterministic and bounded",
                "replay evidence should not be treated as production proof",
                "replay should not mutate live systems",
                "replay should not import unsafe traces without privacy review",
                "replay should distinguish observed facts from inferred/hypothetical outcomes",
                "Replay evidence is not replay proof.",
                "Comparison evidence is not correctness validation.")) {
            assertTrue(adr.contains(expected), "ADR-0006 should include assembler/replay item " + expected);
        }
    }

    @Test
    void adr0006IncludesDeterminismPrivacyFilesystemAndArtifactRules() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Determinism And Ordering Expectations",
                "evidence ordering should be stable",
                "generated IDs, UUIDs, random values, unstable timestamps, hashes, MessageDigest, SHA, environment, and system-property behavior should be avoided unless separately approved",
                "deterministic output must not be confused with correctness proof",
                "Privacy And Trace-Safety Expectations",
                "no secrets",
                "no tokens",
                "no credentials",
                "no env var values",
                "no private network details",
                "no absolute local machine paths",
                "future trace import requires separate ADR/PR",
                "Filesystem And Artifact Boundaries",
                "no filesystem writes unless separately designed later",
                "export/upload/download/PDF/ZIP behavior requires separate approval",
                "storage and retention require separate approval")) {
            assertTrue(adr.contains(expected), "ADR-0006 should include safety rule " + expected);
        }
    }

    @Test
    void adr0006DocumentsPriorAdrRelationshipsAndNotProvenBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Relationship To Prior ADRs",
                "ADR-0001 architecture boundary",
                "ADR-0002 LASE boundary",
                "ADR-0003 evidence and external signal/source boundary",
                "ADR-0004 workload realism/scenario modeling",
                "ADR-0005 safety boundaries and guardrails",
                "Safety And Non-Goals",
                "no `EvidencePacket`",
                "no `EvidenceAssembler`",
                "no evidence generation",
                "no report generation",
                "no replay execution",
                "no storage or persistence",
                "no filesystem-writing implementation",
                "no export, upload, download, PDF, or ZIP behavior",
                "no autonomous production traffic shifting",
                "no carbon-aware routing",
                "no GPU orchestration",
                "no power/grid control",
                "no facility automation",
                "Not-Proven Boundaries",
                "ADR-0006 is proposed/planning-only")) {
            assertTrue(adr.contains(expected), "ADR-0006 should include relationship/non-goal " + expected);
        }
    }

    @Test
    void adr0006LinksRelatedDocsAndReviewerEntryPoints() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "../PHASE_0_ARCHITECTURE_ADR_INDEX.md",
                "ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md",
                "ADR-0002_LASE_INTEGRATION_MODEL.md",
                "ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md",
                "ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md",
                "ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md",
                "../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md",
                "../REVIEWER_TRUST_MAP.md",
                "../ENTERPRISE_READINESS_AUDIT.md",
                "../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md",
                "../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md",
                "../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md")) {
            assertTrue(adr.contains(expected), "ADR-0006 should link " + expected);
        }

        for (Path path : List.of(README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001, ADR_0002,
                ADR_0003, ADR_0004, ADR_0005, LASE_CONTRACT, WORKLOAD_CONTRACT, EXTERNAL_SIGNAL_CONTRACT)) {
            assertTrue(read(path).contains("ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md"),
                    path + " should link ADR-0006");
        }
    }

    @Test
    void adr0006DocsAvoidImplementationAndProductionOverclaims() throws Exception {
        for (Path path : List.of(ADR, README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001, ADR_0002,
                ADR_0003, ADR_0004, ADR_0005, LASE_CONTRACT, WORKLOAD_CONTRACT, EXTERNAL_SIGNAL_CONTRACT)) {
            String normalized = read(path).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "adr-0006 is approved",
                    "adr-0006 is accepted",
                    "adr-0006 implements",
                    "adr-0006 enforces",
                    "evidencepacket is implemented",
                    "evidenceassembler is implemented",
                    "replay execution is implemented",
                    "evidence/report generation is implemented",
                    "report generation is active",
                    "storage/persistence is active",
                    "filesystem-writing implementation is active",
                    "export/upload/download/pdf/zip implementation is active",
                    "autonomous production traffic shifting is implemented",
                    "production-ready now",
                    "production certified",
                    "production certification is proven",
                    "live-cloud validation is complete",
                    "real-tenant validation is complete",
                    "carbon-aware routing is implemented",
                    "gpu orchestration is implemented",
                    "power/grid control is implemented",
                    "facility automation is implemented")) {
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
