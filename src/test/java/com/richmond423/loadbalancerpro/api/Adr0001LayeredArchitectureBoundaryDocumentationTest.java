package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class Adr0001LayeredArchitectureBoundaryDocumentationTest {
    private static final Path ADR = Path.of("docs/adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PHASE0_INDEX = Path.of("docs/PHASE_0_ARCHITECTURE_ADR_INDEX.md");
    private static final Path ALIGNMENT = Path.of("docs/ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md");
    private static final Path LASE_BOUNDARY = Path.of("docs/LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md");
    private static final Path LASE_INVENTORY = Path.of("docs/LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md");
    private static final Path LASE_PACKAGE_PLAN = Path.of("docs/LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void adr0001DocumentExistsAndStatesPlanningOnlyBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "# ADR-0001 Layered Architecture Boundary",
                "Proposed / planning-only",
                "Decision type: architecture planning",
                "Implementation status: not implemented",
                "This ADR draft documents the intended future layer model",
                "No package moves are introduced.",
                "No ArchUnit or package-boundary enforcement is introduced.",
                "No runtime behavior changes are introduced.",
                "This does not change routing/scoring/strategy/proxy behavior.",
                "This does not implement domain/application/infrastructure packages.",
                "This does not claim production readiness or certification.")) {
            assertTrue(adr.contains(expected), "ADR-0001 should state planning boundary " + expected);
        }
    }

    @Test
    void adr0001IncludesIntendedFutureLayerModel() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Intended Future Layer Model",
                "`domain`",
                "Pure models, value objects, domain events",
                "`allocation`",
                "allocation/live routing layer",
                "live routing/allocation decision path",
                "`lase`",
                "LASE shadow/evaluation layer",
                "shadow evaluation, comparison, replay support",
                "`evidence`",
                "evidence/reviewer layer",
                "`infrastructure`",
                "infrastructure/cloud/future integration layer",
                "`api`",
                "api layer",
                "`config`",
                "config layer",
                "`docs/tests`")) {
            assertTrue(adr.contains(expected), "ADR-0001 should include layer model item " + expected);
        }
    }

    @Test
    void adr0001IncludesCurrentPackageAndClassObservations() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Current Package/Class Observations",
                "com.richmond423.loadbalancerpro.core",
                "com.richmond423.loadbalancerpro.api",
                "com.richmond423.loadbalancerpro.api.config",
                "LoadBalancer",
                "AllocatorService",
                "Server",
                "ServerStateVector",
                "ServerScoreCalculator",
                "AdaptiveRoutingPolicyEngine",
                "LaseShadowAdvisor",
                "LaseEvaluationEngine",
                "LaseShadowReplayEngine",
                "AdaptiveRoutingScenario",
                "CloudManager",
                "static UI pages",
                "reviewer dashboards")) {
            assertTrue(adr.contains(expected), "ADR-0001 should include current observation " + expected);
        }
    }

    @Test
    void adr0001IncludesFutureOnlyBoundaryRulesAndDependencies() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Boundary Rules, Future-Only",
                "domain must not depend on infrastructure",
                "LASE must not directly mutate live allocation state",
                "LASE must not directly call proxy mutation paths",
                "LASE must not directly call CloudManager or future grid/facility/GPU control paths",
                "evidence/reviewer metadata must not become production routing authority",
                "api may expose reviewer metadata but must not bypass policy gates",
                "package-boundary enforcement is future-only and separately approved",
                "Allowed Dependencies, Future-Only",
                "api may depend on application services and response DTOs",
                "allocation may depend on domain models and policy gates",
                "LASE may consume read-only observation snapshots",
                "evidence may summarize domain/allocation/LASE outputs",
                "infrastructure may implement guarded adapters",
                "docs/tests may reference all areas for documentation and validation",
                "Forbidden Dependencies, Future-Only",
                "domain to infrastructure",
                "LASE to allocation mutation APIs",
                "LASE to proxy mutation APIs",
                "LASE to CloudManager or future grid/facility/GPU control APIs",
                "evidence to hidden production route selection",
                "api to bypass safety policy gates",
                "docs/tests to claim runtime enforcement that does not exist")) {
            assertTrue(adr.contains(expected), "ADR-0001 should include future-only rule " + expected);
        }
    }

    @Test
    void adr0001IncludesFutureOnlyMigrationStrategyAndSafetyBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Migration Strategy, Future-Only",
                "Keep this ADR proposed until separately reviewed.",
                "Keep current packages stable.",
                "Draft ADR-0002 through ADR-0008 separately.",
                "Add package skeletons only in a separately approved sprint.",
                "Move pure domain models only in a separately approved sprint.",
                "Introduce observation ports only in a separately approved sprint.",
                "Add ArchUnit or equivalent enforcement only after dependency/build impact review.",
                "Never combine package moves with behavior changes.",
                "Never combine enforcement with production-readiness claims.",
                "Safety Boundaries And Non-Goals",
                "no records/classes/interfaces/enums under `src/main/java`",
                "no package moves or refactors",
                "no ArchUnit or any new dependency",
                "no Maven build changes",
                "no proxy behavior change",
                "no strategy behavior change",
                "no core routing behavior change",
                "no scoring-internals behavior change")) {
            assertTrue(adr.contains(expected), "ADR-0001 should include migration/safety item " + expected);
        }
    }

    @Test
    void adr0001LinksRelatedDocsAndReviewerEntryPoints() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "../PHASE_0_ARCHITECTURE_ADR_INDEX.md",
                "../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md",
                "../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md",
                "../LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md",
                "../LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md",
                "../REVIEWER_TRUST_MAP.md",
                "../ENTERPRISE_READINESS_AUDIT.md")) {
            assertTrue(adr.contains(expected), "ADR-0001 should link " + expected);
        }

        for (Path path : List.of(README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, LASE_BOUNDARY,
                LASE_INVENTORY, LASE_PACKAGE_PLAN)) {
            assertTrue(read(path).contains("ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md"),
                    path + " should link ADR-0001");
        }
    }

    @Test
    void adr0001DocsAvoidImplementationAndProductionOverclaims() throws Exception {
        for (Path path : List.of(ADR, README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, LASE_BOUNDARY,
                LASE_INVENTORY, LASE_PACKAGE_PLAN)) {
            String normalized = read(path).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "adr-0001 is approved",
                    "adr-0001 is accepted",
                    "adr-0001 implements",
                    "adr-0001 enforces",
                    "runtime-enforced lase boundary is now active",
                    "runtime-enforced lase boundary is implemented",
                    "package-boundary enforcement is now active",
                    "package-boundary enforcement is active because of adr-0001",
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
