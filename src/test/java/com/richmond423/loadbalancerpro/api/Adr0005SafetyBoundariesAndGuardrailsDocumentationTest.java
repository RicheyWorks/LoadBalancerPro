package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class Adr0005SafetyBoundariesAndGuardrailsDocumentationTest {
    private static final Path ADR = Path.of("docs/adr/ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md");
    private static final Path ADR_0001 = Path.of("docs/adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md");
    private static final Path ADR_0002 = Path.of("docs/adr/ADR-0002_LASE_INTEGRATION_MODEL.md");
    private static final Path ADR_0003 = Path.of("docs/adr/ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md");
    private static final Path ADR_0004 = Path.of("docs/adr/ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PHASE0_INDEX = Path.of("docs/PHASE_0_ARCHITECTURE_ADR_INDEX.md");
    private static final Path ALIGNMENT = Path.of("docs/ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md");
    private static final Path LASE_CONTRACT = Path.of("docs/LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md");
    private static final Path WORKLOAD_CONTRACT = Path.of("docs/WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md");
    private static final Path EXTERNAL_SIGNAL_CONTRACT = Path.of("docs/EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void adr0005DocumentExistsAndStatesPlanningOnlyBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "# ADR-0005 Safety Boundaries And Guardrails",
                "Proposed / planning-only",
                "Decision type: architecture planning",
                "Implementation status: not implemented",
                "This ADR is planning-only.",
                "No runtime enforcement is introduced.",
                "This does not implement runtime LASE enforcement.",
                "This does not add package-boundary enforcement.",
                "This does not add ArchUnit enforcement.",
                "This does not add replay execution.",
                "This does not add evidence/report generation.",
                "This does not add storage or persistence.",
                "This does not add workload generation.",
                "This does not add trace import.",
                "This does not add external signal ingestion.",
                "This does not change routing, scoring, strategy, proxy, API, config, Docker, CI, release, signing, registry, governance, or production behavior.",
                "This does not claim production readiness, production certification, live-cloud validation, or real-tenant validation.")) {
            assertTrue(adr.contains(expected), "ADR-0005 should state planning boundary " + expected);
        }
    }

    @Test
    void adr0005IncludesSafetyModeProgressionAndModeBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Safety Mode Progression",
                "observe-only",
                "recommendation",
                "shadow",
                "active-experiment",
                "manual promotion only",
                "No automatic jump from observation to traffic-changing behavior is allowed by this ADR",
                "Observe-Only Boundaries",
                "must not influence routing",
                "Recommendation Boundaries",
                "must not apply suggested actions automatically",
                "Shadow Boundaries",
                "must not mutate live traffic",
                "Active-Experiment Boundaries",
                "may allow tightly bounded experiments only after explicit policy/operator gates",
                "must not lack rollback/stop conditions",
                "must not lack blast-radius limits",
                "Manual Promotion Only",
                "no hidden background promotion")) {
            assertTrue(adr.contains(expected), "ADR-0005 should include mode item " + expected);
        }
    }

    @Test
    void adr0005IncludesGuardrailCategoriesAndDecisionSafetyEvidence() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Guardrail Categories",
                "policy gates",
                "operator approval / review gates",
                "deterministic evidence requirements",
                "scenario/replay evidence requirements",
                "risk boundaries",
                "rollback/stop conditions",
                "blast-radius limits",
                "confidence thresholds",
                "source-name / signal provenance expectations",
                "privacy and trace-safety constraints",
                "no secret/env leakage",
                "no unsafe external calls",
                "no hidden autonomous mutation",
                "Decision Safety Evidence",
                "selected option",
                "rejected options",
                "signals used",
                "policy checks passed/failed",
                "known uncertainty",
                "what was not proven",
                "safety mode",
                "reason traffic was or was not allowed to change",
                "operator-facing explanation")) {
            assertTrue(adr.contains(expected), "ADR-0005 should include guardrail item " + expected);
        }
    }

    @Test
    void adr0005IncludesEvidenceRollbackBlastRadiusConfidenceAndProvenanceRules() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Deterministic Evidence Requirements",
                "evidence ordering should be stable",
                "deterministic evidence must not be confused with correctness validation",
                "Scenario And Replay Evidence Requirements",
                "replay evidence is not replay proof",
                "comparison output is not correctness validation",
                "clean scenario results are not production safety proof",
                "Risk Boundaries",
                "Rollback And Stop Conditions",
                "Blast-Radius Limits",
                "Confidence Thresholds",
                "Source-Name And Signal Provenance Expectations",
                "future source-name guard output is not enforcement unless separately implemented",
                "Privacy And Trace-Safety Constraints",
                "future trace import requires separate ADR/PR",
                "External Call And Mutation Boundaries",
                "no direct LASE control of GPU, grid, facility, power, carbon, or cloud systems",
                "no hidden autonomous mutation")) {
            assertTrue(adr.contains(expected), "ADR-0005 should include safety rule " + expected);
        }
    }

    @Test
    void adr0005IncludesNorthStarRelationshipsAndNonGoals() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Relationship To The North-Star Vision",
                "future datacenter adaptive traffic control",
                "partial degradation and tail latency under pressure",
                "safer adaptive routing",
                "explainability and auditability",
                "guardrails before autonomy",
                "black-box routing decisions",
                "Relationship To LASE And Adaptive Routing",
                "LASE must not directly mutate live allocation state",
                "LASE must not directly select production routes",
                "LASE must not directly alter proxy behavior",
                "LASE must not bypass policy/operator gates",
                "Relationship To Evidence And Reviewer Metadata",
                "Relationship To Workload And Scenario Modeling",
                "Relationship To External Signals",
                "Safety And Non-Goals",
                "no runtime enforcement",
                "no autonomous production traffic shifting",
                "no carbon-aware routing",
                "no GPU orchestration",
                "no power/grid control",
                "no facility automation",
                "Not-Proven Boundaries",
                "ADR-0005 is proposed/planning-only")) {
            assertTrue(adr.contains(expected), "ADR-0005 should include relationship/non-goal " + expected);
        }
    }

    @Test
    void adr0005LinksRelatedDocsAndReviewerEntryPoints() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "../PHASE_0_ARCHITECTURE_ADR_INDEX.md",
                "ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md",
                "ADR-0002_LASE_INTEGRATION_MODEL.md",
                "ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md",
                "ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md",
                "../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md",
                "../REVIEWER_TRUST_MAP.md",
                "../ENTERPRISE_READINESS_AUDIT.md",
                "../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md",
                "../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md",
                "../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md",
                "../THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md",
                "../SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md",
                "../SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md")) {
            assertTrue(adr.contains(expected), "ADR-0005 should link " + expected);
        }

        for (Path path : List.of(README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001, ADR_0002,
                ADR_0003, ADR_0004, LASE_CONTRACT, WORKLOAD_CONTRACT, EXTERNAL_SIGNAL_CONTRACT)) {
            assertTrue(read(path).contains("ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md"),
                    path + " should link ADR-0005");
        }
    }

    @Test
    void adr0005DocsAvoidImplementationAndProductionOverclaims() throws Exception {
        for (Path path : List.of(ADR, README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001, ADR_0002,
                ADR_0003, ADR_0004, LASE_CONTRACT, WORKLOAD_CONTRACT, EXTERNAL_SIGNAL_CONTRACT)) {
            String normalized = read(path).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "adr-0005 is approved",
                    "adr-0005 is accepted",
                    "adr-0005 implements",
                    "adr-0005 enforces",
                    "runtime enforcement is active",
                    "runtime safety enforcement is active",
                    "active traffic shifting is implemented",
                    "autonomous production traffic shifting is implemented",
                    "replay execution is implemented",
                    "evidence/report generation is implemented",
                    "report generation is active",
                    "storage/persistence is active",
                    "external signal ingestion is implemented",
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
