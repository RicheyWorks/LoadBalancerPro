package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class Adr0003EvidenceAsFirstClassArtifactDocumentationTest {
    private static final Path ADR = Path.of("docs/adr/ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md");
    private static final Path ADR_0001 = Path.of("docs/adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md");
    private static final Path ADR_0002 = Path.of("docs/adr/ADR-0002_LASE_INTEGRATION_MODEL.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PHASE0_INDEX = Path.of("docs/PHASE_0_ARCHITECTURE_ADR_INDEX.md");
    private static final Path ALIGNMENT = Path.of("docs/ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void adr0003DocumentExistsAndStatesPlanningOnlyBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "# ADR-0003 Evidence As First-Class Artifact",
                "Proposed / planning-only",
                "Decision type: architecture planning",
                "Implementation status: not implemented",
                "This ADR is planning-only.",
                "No EvidencePacket implementation is introduced.",
                "No EvidenceAssembler implementation is introduced.",
                "No report generation is introduced.",
                "No JSON output is introduced.",
                "No storage/persistence/telemetry/audit log implementation is introduced.",
                "No replay execution is introduced.",
                "No routing/scoring/strategy/proxy/API behavior changes are introduced.",
                "This does not claim correctness validation, replay proof, production readiness, or production certification.")) {
            assertTrue(adr.contains(expected), "ADR-0003 should state planning boundary " + expected);
        }
    }

    @Test
    void adr0003IncludesEvidenceRoleAndArtifactTypes() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Conceptual Evidence Role",
                "Evidence is a future first-class architecture concern",
                "what decision or comparison was made",
                "what inputs were considered",
                "Evidence should preserve not-proven boundaries",
                "Evidence should never become hidden production routing authority",
                "Evidence should not be treated as correctness proof by default",
                "Future Evidence Artifact Types",
                "reviewer summary",
                "decision explanation",
                "strategy comparison summary",
                "LASE shadow evaluation summary",
                "replay/comparison summary",
                "workload/scenario context summary",
                "policy gate summary",
                "not-proven boundary summary",
                "future EvidencePacket concept")) {
            assertTrue(adr.contains(expected), "ADR-0003 should include evidence role item " + expected);
        }
    }

    @Test
    void adr0003IncludesEvidencePacketReviewerTrustAndExplainability() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Future Evidence Packet Boundaries",
                "`EvidencePacket` is future-only and not implemented in this sprint",
                "Future EvidencePacket must not include secrets",
                "Future EvidencePacket must not include env var values",
                "Future EvidencePacket must not include tokens, credentials, private network details, or absolute local machine paths",
                "Future EvidencePacket IDs/timestamps/hashes must be deterministic or separately approved",
                "Future EvidencePacket must include `notProvenBoundaries`",
                "Future EvidencePacket must distinguish report-only/shadow/replay/lab outputs from production proof",
                "Future EvidencePacket must not claim production certification",
                "Reviewer Trust Relationship",
                "evidence must not replace human review",
                "evidence must not certify production safety",
                "Auditability And Explainability Relationship",
                "future evidence should be auditable",
                "future evidence should distinguish observed facts from derived summaries",
                "future evidence should avoid unstable generated IDs unless separately approved")) {
            assertTrue(adr.contains(expected), "ADR-0003 should include packet/trust item " + expected);
        }
    }

    @Test
    void adr0003IncludesLaseReplayDeterminismPrivacyAndStorageBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "LASE And Shadow Evaluation Relationship",
                "LASE evidence remains shadow/evaluation metadata",
                "LASE evidence must not directly mutate live allocation",
                "LASE evidence must not become production route authority",
                "Replay And Comparison Relationship",
                "replay evidence is not replay proof",
                "comparison evidence is not correctness validation",
                "replay/comparison output must not mutate live routing state",
                "Determinism Expectations",
                "evidence should avoid unstable timestamps unless separately approved",
                "evidence should avoid UUID/random/hash identifiers unless separately approved",
                "evidence ordering should be stable if implemented later",
                "Integrity And Privacy Expectations",
                "Future Storage And Retention Boundaries",
                "No storage or persistence is implemented in this sprint",
                "future retention requires separate ADR/PR",
                "future audit log implementation must be separately approved",
                "future export/download/share behavior must be separately approved")) {
            assertTrue(adr.contains(expected), "ADR-0003 should include future expectation " + expected);
        }
    }

    @Test
    void adr0003IncludesSafetyBoundariesAndNotProvenLimits() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Safety And Non-Goals",
                "no production Java runtime behavior",
                "no records/classes/interfaces/enums under `src/main/java`",
                "no `EvidencePacket`",
                "no `EvidenceAssembler`",
                "no report generation",
                "no JSON output",
                "no storage, persistence, telemetry, or audit log implementation",
                "no replay execution",
                "no proxy behavior change",
                "no strategy behavior change",
                "no core routing behavior change",
                "no scoring-internals behavior change",
                "no API behavior change",
                "Not-Proven Boundaries",
                "EvidencePacket implementation not added",
                "EvidenceAssembler implementation not added",
                "replay execution not added",
                "evidence/report generation not added",
                "storage/persistence not added",
                "ADR-0003 is proposed/planning-only")) {
            assertTrue(adr.contains(expected), "ADR-0003 should include safety item " + expected);
        }
    }

    @Test
    void adr0003LinksRelatedDocsAndReviewerEntryPoints() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "../PHASE_0_ARCHITECTURE_ADR_INDEX.md",
                "ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md",
                "ADR-0002_LASE_INTEGRATION_MODEL.md",
                "../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md",
                "../REVIEWER_TRUST_MAP.md",
                "../ENTERPRISE_READINESS_AUDIT.md",
                "../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md",
                "../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_GUIDANCE.md",
                "../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_HANDOFF_SUMMARY.md",
                "../SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md")) {
            assertTrue(adr.contains(expected), "ADR-0003 should link " + expected);
        }

        for (Path path : List.of(README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001, ADR_0002)) {
            assertTrue(read(path).contains("ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md"),
                    path + " should link ADR-0003");
        }
    }

    @Test
    void adr0003DocsAvoidImplementationAndProductionOverclaims() throws Exception {
        for (Path path : List.of(ADR, README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001, ADR_0002)) {
            String normalized = read(path).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "adr-0003 is approved",
                    "adr-0003 is accepted",
                    "adr-0003 implements",
                    "adr-0003 enforces",
                    "evidencepacket is implemented",
                    "evidencepacket has been implemented",
                    "evidenceassembler is implemented",
                    "evidenceassembler has been implemented",
                    "report generation is active",
                    "json output is generated by adr-0003",
                    "storage/persistence is active",
                    "audit log is implemented by adr-0003",
                    "replay execution is implemented",
                    "replay proof is proven",
                    "correctness validation is proven",
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
