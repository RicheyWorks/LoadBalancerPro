package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class Adr0008RuntimeEnforcementAndPackageBoundaryPlanDocumentationTest {
    private static final Path ADR = Path.of("docs/adr/ADR-0008_RUNTIME_ENFORCEMENT_AND_PACKAGE_BOUNDARY_PLAN.md");
    private static final Path ADR_0001 = Path.of("docs/adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md");
    private static final Path ADR_0002 = Path.of("docs/adr/ADR-0002_LASE_INTEGRATION_MODEL.md");
    private static final Path ADR_0003 = Path.of("docs/adr/ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md");
    private static final Path ADR_0004 = Path.of("docs/adr/ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md");
    private static final Path ADR_0005 = Path.of("docs/adr/ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md");
    private static final Path ADR_0006 =
            Path.of("docs/adr/ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md");
    private static final Path ADR_0007 = Path.of("docs/adr/ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PHASE0_INDEX = Path.of("docs/PHASE_0_ARCHITECTURE_ADR_INDEX.md");
    private static final Path ALIGNMENT = Path.of("docs/ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md");
    private static final Path LASE_CONTRACT = Path.of("docs/LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md");
    private static final Path LASE_PACKAGE_PLAN = Path.of("docs/LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md");
    private static final Path WORKLOAD_CONTRACT =
            Path.of("docs/WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md");
    private static final Path EXTERNAL_SIGNAL_CONTRACT = Path.of("docs/EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void adr0008DocumentExistsAndStatesPlanningOnlyBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "# ADR-0008 Runtime Enforcement And Package Boundary Plan",
                "Proposed / planning-only.",
                "Decision type: architecture planning.",
                "Implementation status: not implemented.",
                "This ADR is planning-only.",
                "This does not implement runtime enforcement.",
                "This does not implement package-boundary enforcement.",
                "This does not add ArchUnit rules.",
                "This does not add an ArchUnit dependency.",
                "This does not move packages.",
                "This does not rename packages or classes.",
                "This does not change API behavior.",
                "This does not change routing behavior.",
                "This does not add production traffic controls.",
                "This does not claim production readiness, production certification, live-cloud validation, or real-tenant validation.")) {
            assertTrue(adr.contains(expected), "ADR-0008 should state planning boundary " + expected);
        }
    }

    @Test
    void adr0008IncludesRuntimePurposeAndPackageBoundaryModel() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Runtime Enforcement Purpose",
                "prevent adaptive logic from silently mutating production traffic",
                "keep observe-only, recommendation, shadow, and active-experiment authority explicit",
                "separate decision explanation from decision authority",
                "make future enforcement reviewable and testable",
                "prevent black-box adaptive routing behavior",
                "Planned Package Boundary Model",
                "LASE/adaptive decision logic",
                "Routing strategy/core load-balancer behavior",
                "External signal ingestion",
                "Workload/scenario modeling",
                "Evidence packet assembly",
                "Reviewer/operator explanation",
                "Safety policy gates",
                "Replay/scenario evaluation",
                "Production traffic mutation authority")) {
            assertTrue(adr.contains(expected), "ADR-0008 should include runtime/package item " + expected);
        }
    }

    @Test
    void adr0008IncludesDependencyDirectionAndFutureMechanisms() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Dependency Direction Expectations",
                "routing core should not depend directly on reviewer evidence UI/docs",
                "external signals should enter through explicit ports/contracts",
                "evidence assembly should observe decisions, not mutate routing",
                "replay/scenario evaluation should not mutate live systems",
                "reviewer evidence should not become runtime authority",
                "active-experiment behavior must pass explicit policy/operator gates",
                "Future Enforcement Mechanisms",
                "package-boundary tests",
                "ArchUnit-style rules",
                "source-name guard checks",
                "explicit port interfaces",
                "adapter boundaries",
                "mutation-authority checks",
                "safety-mode tests",
                "forbidden dependency checks",
                "These mechanisms are documented only.")) {
            assertTrue(adr.contains(expected), "ADR-0008 should include dependency/mechanism item " + expected);
        }
    }

    @Test
    void adr0008DocumentsPriorAdrRelationshipsAndNorthStarVision() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Relationship To Prior ADRs",
                "ADR-0001 architecture boundary",
                "ADR-0002 LASE boundary",
                "ADR-0003 external signal/source boundary",
                "ADR-0004 workload realism/scenario modeling",
                "ADR-0005 safety boundaries and guardrails",
                "ADR-0006 evidence packet and replay boundary model",
                "ADR-0007 reviewer evidence and trust model",
                "Relationship To North-Star Vision",
                "future datacenter adaptive traffic control",
                "partial degradation and recovery behavior",
                "tail latency under pressure",
                "safer adaptive routing requires guardrails before autonomy",
                "explainability and auditability need package and runtime boundaries",
                "trusted adaptive routing should avoid black-box routing decisions")) {
            assertTrue(adr.contains(expected), "ADR-0008 should include relationship/north-star item " + expected);
        }
    }

    @Test
    void adr0008IncludesSafetyNonGoalsAndNotProvenBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Safety And Non-Goals",
                "no ArchUnit dependency",
                "no ArchUnit enforcement",
                "no package-boundary enforcement",
                "no source-name guard implementation",
                "no runtime LASE enforcement",
                "no routing behavior change",
                "no scoring behavior change",
                "no strategy behavior change",
                "no proxy behavior change",
                "no API behavior change",
                "no reviewer portal implementation",
                "no reviewer dashboard implementation",
                "no reviewer API implementation",
                "no EvidencePacket implementation",
                "no EvidenceAssembler implementation",
                "no replay execution",
                "no storage or persistence",
                "no filesystem-writing implementation",
                "no export, upload, download, PDF, or ZIP behavior",
                "Not-Proven Boundaries",
                "ADR-0008 is proposed/planning-only")) {
            assertTrue(adr.contains(expected), "ADR-0008 should include safety boundary " + expected);
        }
    }

    @Test
    void adr0008LinksRelatedDocsAndReviewerEntryPoints() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "../PHASE_0_ARCHITECTURE_ADR_INDEX.md",
                "ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md",
                "ADR-0002_LASE_INTEGRATION_MODEL.md",
                "ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md",
                "ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md",
                "ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md",
                "ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md",
                "ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md",
                "../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md",
                "../REVIEWER_TRUST_MAP.md",
                "../ENTERPRISE_READINESS_AUDIT.md",
                "../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md",
                "../LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md",
                "../LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md",
                "../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md",
                "../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md")) {
            assertTrue(adr.contains(expected), "ADR-0008 should link " + expected);
        }

        for (Path path : List.of(README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001, ADR_0002,
                ADR_0003, ADR_0004, ADR_0005, ADR_0006, ADR_0007, LASE_CONTRACT, LASE_PACKAGE_PLAN,
                WORKLOAD_CONTRACT, EXTERNAL_SIGNAL_CONTRACT)) {
            assertTrue(read(path).contains("ADR-0008_RUNTIME_ENFORCEMENT_AND_PACKAGE_BOUNDARY_PLAN.md"),
                    path + " should link ADR-0008");
        }
    }

    @Test
    void adr0008DocsAvoidImplementationAndProductionOverclaims() throws Exception {
        for (Path path : List.of(ADR, README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001, ADR_0002,
                ADR_0003, ADR_0004, ADR_0005, ADR_0006, ADR_0007, LASE_CONTRACT, LASE_PACKAGE_PLAN,
                WORKLOAD_CONTRACT, EXTERNAL_SIGNAL_CONTRACT)) {
            String normalized = read(path).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "adr-0008 is approved",
                    "adr-0008 is accepted",
                    "adr-0008 implements",
                    "adr-0008 enforces",
                    "adr-0008 moves packages",
                    "adr-0008 changes routing",
                    "adr-0008 changes api behavior",
                    "runtime enforcement is implemented by adr-0008",
                    "package-boundary enforcement is implemented by adr-0008",
                    "archunit enforcement is implemented",
                    "source-name guard is implemented by adr-0008",
                    "reviewer portal is implemented by adr-0008",
                    "evidencepacket is implemented by adr-0008",
                    "evidenceassembler is implemented by adr-0008",
                    "replay execution is implemented by adr-0008",
                    "storage/persistence is implemented by adr-0008",
                    "export/upload/download/pdf/zip is implemented by adr-0008",
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
