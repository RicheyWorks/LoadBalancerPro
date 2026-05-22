package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class Adr0002LaseIntegrationModelDocumentationTest {
    private static final Path ADR = Path.of("docs/adr/ADR-0002_LASE_INTEGRATION_MODEL.md");
    private static final Path ADR_0001 = Path.of("docs/adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PHASE0_INDEX = Path.of("docs/PHASE_0_ARCHITECTURE_ADR_INDEX.md");
    private static final Path ALIGNMENT = Path.of("docs/ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md");
    private static final Path LASE_BOUNDARY = Path.of("docs/LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md");
    private static final Path LASE_INVENTORY = Path.of("docs/LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md");
    private static final Path LASE_PACKAGE_PLAN = Path.of("docs/LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md");
    private static final Path EXTERNAL_SIGNAL = Path.of("docs/EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md");
    private static final Path WORKLOAD_PROFILE = Path.of("docs/WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void adr0002DocumentExistsAndStatesPlanningOnlyBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "# ADR-0002 LASE Integration Model",
                "Proposed / planning-only",
                "Decision type: architecture planning",
                "Implementation status: not implemented",
                "This ADR is planning-only.",
                "No runtime LASE enforcement is introduced.",
                "No `LaseObservationPort` interface is added.",
                "No live allocation behavior changes are introduced.",
                "No routing/scoring/strategy/proxy/API behavior changes are introduced.",
                "This does not add replay execution.",
                "This does not add evidence packet implementation.",
                "This does not add ExternalSignalPort implementation.",
                "This does not add WorkloadProfile implementation.",
                "This does not claim production readiness or certification.")) {
            assertTrue(adr.contains(expected), "ADR-0002 should state planning boundary " + expected);
        }
    }

    @Test
    void adr0002IncludesLaseRoleAndRelationships() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Conceptual LASE Role",
                "LASE is the future shadow/evaluation path for adaptive routing experiments",
                "LASE may observe, compare, explain, and produce evidence",
                "LASE must remain separated from live allocation mutation",
                "LASE must not become hidden production routing authority",
                "Integration Surfaces, Future-Only",
                "read-only observation snapshots from allocation",
                "strategy comparison inputs",
                "WorkloadProfile context, if implemented later",
                "ExternalSignalPort context, if implemented later",
                "policy gate decisions",
                "replay/evidence artifacts",
                "reviewer metadata summaries")) {
            assertTrue(adr.contains(expected), "ADR-0002 should include LASE role item " + expected);
        }
    }

    @Test
    void adr0002IncludesLiveShadowReplayEvidenceAndPolicyGateRelationships() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Live Allocation Relationship",
                "LASE must not directly mutate live routing state",
                "LASE must not directly select production routes",
                "LASE must not directly alter proxy behavior",
                "LASE must not bypass policy gates",
                "Shadow Evaluation Relationship",
                "LASE may compare strategies in shadow-only contexts",
                "LASE findings are review signals, not runtime authority",
                "Replay And Comparison Relationship",
                "replay evidence is not replay proof",
                "comparison output is not correctness proof",
                "replay/comparison must not mutate live routing state",
                "Evidence And Reviewer Metadata Relationship",
                "evidence metadata must not claim production certification",
                "evidence metadata must not claim correctness validation",
                "reviewer metadata must remain separate from production routing authority",
                "Policy Gate Relationship",
                "no promotion behavior is implemented in this sprint",
                "future policy gate changes require separate scoped PR")) {
            assertTrue(adr.contains(expected), "ADR-0002 should include relationship item " + expected);
        }
    }

    @Test
    void adr0002IncludesObservationPortExternalSignalAndDeterminismExpectations() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Future Observation Port Relationship",
                "`LaseObservationPort` is a future read-only concept only",
                "no observation port interface is added in this sprint",
                "future observation snapshots must be read-only",
                "future observation snapshots must not expose secrets",
                "future observation snapshots must not include unstable IDs unless separately approved",
                "Future External Signal Relationship",
                "ExternalSignalPort is future-only and read-only",
                "LASE must not directly control GPU/grid/facility/power systems",
                "future signal context must not grant mutation authority",
                "missing/stale external signals must degrade safely if implemented later",
                "Determinism And Replayability Expectations",
                "inputs should be explicit and reviewable",
                "ordering should be stable",
                "unavailable or stale context should be labeled rather than guessed",
                "replayable evidence should separate reconstruction, comparison, and proof claims")) {
            assertTrue(adr.contains(expected), "ADR-0002 should include future expectation " + expected);
        }
    }

    @Test
    void adr0002IncludesSafetyBoundariesAndNotProvenLimits() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Safety And Non-Goals",
                "no production Java runtime behavior",
                "no records/classes/interfaces/enums under `src/main/java`",
                "no `LaseObservationPort`",
                "no package moves or refactors",
                "no ArchUnit or any new dependency",
                "no Maven build changes",
                "no replay execution",
                "no proxy behavior change",
                "no strategy behavior change",
                "no core routing behavior change",
                "no scoring-internals behavior change",
                "no API behavior change",
                "Not-Proven Boundaries",
                "LaseObservationPort not added",
                "LASE boundary not runtime-enforced yet",
                "ADR-0002 is proposed/planning-only")) {
            assertTrue(adr.contains(expected), "ADR-0002 should include safety item " + expected);
        }
    }

    @Test
    void adr0002LinksRelatedDocsAndReviewerEntryPoints() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "../PHASE_0_ARCHITECTURE_ADR_INDEX.md",
                "ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md",
                "../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md",
                "../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md",
                "../LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md",
                "../LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md",
                "../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md",
                "../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md",
                "../REVIEWER_TRUST_MAP.md",
                "../ENTERPRISE_READINESS_AUDIT.md")) {
            assertTrue(adr.contains(expected), "ADR-0002 should link " + expected);
        }

        for (Path path : List.of(README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001,
                LASE_BOUNDARY, LASE_INVENTORY, LASE_PACKAGE_PLAN, EXTERNAL_SIGNAL, WORKLOAD_PROFILE)) {
            assertTrue(read(path).contains("ADR-0002_LASE_INTEGRATION_MODEL.md"),
                    path + " should link ADR-0002");
        }
    }

    @Test
    void adr0002DocsAvoidImplementationAndProductionOverclaims() throws Exception {
        for (Path path : List.of(ADR, README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001,
                LASE_BOUNDARY, LASE_INVENTORY, LASE_PACKAGE_PLAN, EXTERNAL_SIGNAL, WORKLOAD_PROFILE)) {
            String normalized = read(path).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "adr-0002 is approved",
                    "adr-0002 is accepted",
                    "adr-0002 implements",
                    "adr-0002 enforces",
                    "laseobservationport implementation exists",
                    "laseobservationport is implemented",
                    "runtime-enforced lase boundary is now active",
                    "runtime-enforced lase boundary is implemented",
                    "package-boundary enforcement is now active",
                    "external signal port is implemented",
                    "workloadprofile is implemented",
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
