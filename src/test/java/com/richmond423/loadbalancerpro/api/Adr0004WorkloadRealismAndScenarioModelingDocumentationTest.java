package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class Adr0004WorkloadRealismAndScenarioModelingDocumentationTest {
    private static final Path ADR = Path.of("docs/adr/ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md");
    private static final Path ADR_0001 = Path.of("docs/adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md");
    private static final Path ADR_0002 = Path.of("docs/adr/ADR-0002_LASE_INTEGRATION_MODEL.md");
    private static final Path ADR_0003 = Path.of("docs/adr/ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PHASE0_INDEX = Path.of("docs/PHASE_0_ARCHITECTURE_ADR_INDEX.md");
    private static final Path ALIGNMENT = Path.of("docs/ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md");
    private static final Path WORKLOAD_CONTRACT = Path.of("docs/WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md");
    private static final Path EXTERNAL_SIGNAL_CONTRACT = Path.of("docs/EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md");
    private static final Path LASE_CONTRACT = Path.of("docs/LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void adr0004DocumentExistsAndStatesPlanningOnlyBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "# ADR-0004 Workload Realism And Scenario Modeling",
                "Proposed / planning-only",
                "Decision type: architecture planning",
                "Implementation status: not implemented",
                "This ADR is planning-only.",
                "No WorkloadProfile implementation is introduced.",
                "No ScenarioGenerator implementation is introduced.",
                "No workload generator implementation is introduced.",
                "No trace import is introduced.",
                "No replay execution is introduced.",
                "No EvidencePacket/report generation implementation is introduced.",
                "No JSON output is introduced.",
                "No telemetry/storage/persistence implementation is introduced.",
                "No routing/scoring/strategy/proxy/API behavior changes are introduced.",
                "This does not claim production readiness or production certification.")) {
            assertTrue(adr.contains(expected), "ADR-0004 should state planning boundary " + expected);
        }
    }

    @Test
    void adr0004IncludesWorkloadRoleProfilesGeneratorsAndCategories() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Conceptual Workload Realism Role",
                "Workload realism is a future architecture concern",
                "steady, bursty, degraded, tail-heavy, recovery, and inference-like conditions",
                "Workload modeling must not become production traffic control by itself",
                "Scenario modeling does not prove live-cloud validation",
                "Scenario modeling does not prove real-tenant validation",
                "Future WorkloadProfile Boundaries",
                "`WorkloadProfile` is future-only and not implemented in this sprint",
                "Future WorkloadProfile should describe workload shape/context, not mutate routing state",
                "Future ScenarioGenerator Boundaries",
                "`ScenarioGenerator` is future-only and not implemented in this sprint",
                "Future ScenarioGenerator must not generate live traffic",
                "Future Workload/Scenario Categories",
                "steady baseline",
                "bursty load",
                "tail-heavy latency",
                "overload pressure",
                "all-unhealthy degradation",
                "recovery",
                "mixed steady-plus-bursty",
                "inference-like request spikes",
                "external signal context, future-only",
                "reviewer evidence scenario, future-only")) {
            assertTrue(adr.contains(expected), "ADR-0004 should include workload item " + expected);
        }
    }

    @Test
    void adr0004IncludesLaseStrategyEvidenceAndReplayRelationships() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Relationship To LASE And Shadow Evaluation",
                "Future workload profiles may provide context for LASE shadow evaluation",
                "Workload context must not grant LASE live routing authority",
                "Relationship To Strategy Comparison",
                "comparison output is not correctness validation",
                "workload-driven comparisons must not mutate live allocation",
                "Relationship To Evidence And Reviewer Metadata",
                "evidence must state workload/scenario limitations",
                "clean scenario results are not production safety proof",
                "Relationship To Replay And Deterministic Validation",
                "replay evidence is not replay proof",
                "deterministic validation is future architecture guidance, not current implementation",
                "replay/comparison output must not mutate live routing state")) {
            assertTrue(adr.contains(expected), "ADR-0004 should include relationship " + expected);
        }
    }

    @Test
    void adr0004IncludesDeterminismPrivacyTraceSafetyAndImportBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Determinism Expectations",
                "Future scenario generation should be deterministic where practical",
                "future scenario ordering should be stable",
                "avoid UUID/random/time/env/system-property behavior unless separately approved",
                "deterministic output must not be confused with correctness proof",
                "Privacy And Trace-Safety Expectations",
                "Future trace import requires separate ADR/PR",
                "future trace import must avoid real secrets",
                "future traces must be anonymized or synthetic unless separately approved",
                "trace import must not imply real-tenant validation unless separately proven",
                "Future Import/Replay Boundaries",
                "No import tooling is added in this sprint",
                "No replay execution is added in this sprint",
                "future import/replay must be lab-safe",
                "future import/replay must not certify production safety")) {
            assertTrue(adr.contains(expected), "ADR-0004 should include deterministic/privacy item " + expected);
        }
    }

    @Test
    void adr0004IncludesSafetyBoundariesAndNotProvenLimits() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Safety And Non-Goals",
                "no production Java runtime behavior",
                "no records/classes/interfaces/enums under `src/main/java`",
                "no WorkloadProfile",
                "no ScenarioGenerator",
                "no workload generators",
                "no trace import",
                "no replay execution",
                "no EvidencePacket",
                "no EvidenceAssembler",
                "no report generation",
                "no JSON output",
                "no storage, persistence, telemetry, or audit log implementation",
                "no proxy behavior change",
                "no strategy behavior change",
                "no core routing behavior change",
                "no scoring-internals behavior change",
                "no API behavior change",
                "Not-Proven Boundaries",
                "WorkloadProfile implementation not added",
                "ScenarioGenerator implementation not added",
                "workload generator implementation not added",
                "trace import not added",
                "ADR-0004 is proposed/planning-only")) {
            assertTrue(adr.contains(expected), "ADR-0004 should include safety item " + expected);
        }
    }

    @Test
    void adr0004LinksRelatedDocsAndReviewerEntryPoints() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "../PHASE_0_ARCHITECTURE_ADR_INDEX.md",
                "ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md",
                "ADR-0002_LASE_INTEGRATION_MODEL.md",
                "ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md",
                "../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md",
                "../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md",
                "../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md",
                "../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md",
                "../REVIEWER_TRUST_MAP.md",
                "../ENTERPRISE_READINESS_AUDIT.md",
                "../THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md",
                "../ENTERPRISE_LAB_DECISION_REPLAY_READINESS_CHECKLIST.md")) {
            assertTrue(adr.contains(expected), "ADR-0004 should link " + expected);
        }

        for (Path path : List.of(README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001, ADR_0002,
                ADR_0003, WORKLOAD_CONTRACT, EXTERNAL_SIGNAL_CONTRACT, LASE_CONTRACT)) {
            assertTrue(read(path).contains("ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md"),
                    path + " should link ADR-0004");
        }
    }

    @Test
    void adr0004DocsAvoidImplementationAndProductionOverclaims() throws Exception {
        for (Path path : List.of(ADR, README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001, ADR_0002,
                ADR_0003, WORKLOAD_CONTRACT, EXTERNAL_SIGNAL_CONTRACT, LASE_CONTRACT)) {
            String normalized = read(path).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "adr-0004 is approved",
                    "adr-0004 is accepted",
                    "adr-0004 implements",
                    "adr-0004 enforces",
                    "workloadprofile is implemented",
                    "workloadprofile has been implemented",
                    "scenariogenerator is implemented",
                    "scenariogenerator has been implemented",
                    "workload generator is implemented",
                    "trace import is active",
                    "trace import is implemented",
                    "replay execution is implemented",
                    "evidencepacket is implemented by adr-0004",
                    "report generation is active",
                    "json output is generated by adr-0004",
                    "storage/persistence is active",
                    "telemetry is implemented by adr-0004",
                    "live-cloud validation is complete",
                    "real-tenant validation is complete",
                    "production readiness is proven",
                    "production certification is proven")) {
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
