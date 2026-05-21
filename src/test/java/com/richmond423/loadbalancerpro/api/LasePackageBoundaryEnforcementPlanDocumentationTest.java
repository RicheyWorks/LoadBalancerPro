package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LasePackageBoundaryEnforcementPlanDocumentationTest {
    private static final Path DOC = Path.of("docs/LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md");
    private static final Path LASE_BOUNDARY = Path.of("docs/LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md");
    private static final Path INVENTORY = Path.of("docs/LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void enforcementPlanDocExistsAndStatesPlanningOnly() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# LASE Package Boundary Enforcement Plan",
                "planning only, not enforcement",
                "docs/test only",
                "No ArchUnit or package-boundary tool is added in this sprint.",
                "No classes are moved in this sprint.",
                "No packages are refactored.",
                "No Maven build files are changed.",
                "no runtime enforcement",
                "no package-boundary enforcement",
                "LASE package boundary is not currently enforced",
                "no runtime package-boundary enforcement is added",
                "not enforcement proof")) {
            assertTrue(doc.contains(expected), "enforcement plan should state " + expected);
        }
    }

    @Test
    void enforcementPlanLinksArchitectureContractAndInventory() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md",
                "LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md",
                "Relationship To LASE Boundary Architecture Contract",
                "Relationship To LASE Boundary Enforcement Inventory",
                "This plan does not replace the contract.",
                "The inventory remains a map, not enforcement. This plan remains a plan, not enforcement.")) {
            assertTrue(doc.contains(expected), "enforcement plan should link adjacent architecture doc " + expected);
        }

        assertTrue(read(LASE_BOUNDARY).contains("LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md"),
                "LASE boundary architecture contract should link the enforcement plan");
        assertTrue(read(INVENTORY).contains("LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md"),
                "LASE boundary inventory should link the enforcement plan");
    }

    @Test
    void enforcementPlanIncludesFuturePackageModelAndPhases() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Proposed Future Package Model",
                "`domain`",
                "`allocation`",
                "`lase`",
                "`infrastructure`",
                "`api`",
                "`config`",
                "Proposed Enforcement Phases",
                "E0",
                "Current inventory and docs guard tests",
                "E1",
                "Naming and docs-only boundary guard tests",
                "E2",
                "Introduce package skeletons only, no class moves",
                "E3",
                "Move pure domain models only, no behavior changes",
                "E4",
                "Introduce read-only observation port only, no behavior changes",
                "E5",
                "Move LASE-only classes only, no behavior changes",
                "E6",
                "Add package-boundary enforcement tool such as ArchUnit only after dependency/build impact review",
                "E7",
                "Enforce no LASE-to-mutation-path dependency",
                "E8",
                "Enforce no LASE-to-cloud/facility/grid/GPU control dependency",
                "E9",
                "Add reviewer-visible boundary evidence only after enforcement is real")) {
            assertTrue(doc.contains(expected), "enforcement plan should include phase/model item " + expected);
        }
    }

    @Test
    void enforcementPlanDocumentsCandidateRulesAndForbiddenLaseAuthority() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Candidate Rules For Future Enforcement",
                "`lase` must not depend on allocation mutation services.",
                "`lase` must not directly mutate live routing state.",
                "`lase` must not become production route selector.",
                "`lase` must not directly call proxy mutation paths.",
                "`lase` must not directly call `CloudManager` or future grid/facility/GPU control paths.",
                "`lase` must not write production configuration.",
                "`allocation` may publish read-only observation snapshots.",
                "`reviewer metadata` may summarize LASE outputs without granting routing authority.",
                "`domain` must remain free of infrastructure dependencies.",
                "`infrastructure` must not be called from LASE without an approved read-only port.",
                "Package-boundary enforcement must not be used to claim production readiness.",
                "Package-boundary enforcement must not be used to claim production certification.")) {
            assertTrue(doc.contains(expected), "enforcement plan should include future rule " + expected);
        }
    }

    @Test
    void enforcementPlanKeepsStagingWarningsApprovalGatesTestingAndRollbackExplicit() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Never combine package moves, enforcement tooling, behavior changes, and production claims in the same sprint.",
                "What Must Not Be Combined In One Sprint",
                "package moves and behavior changes",
                "enforcement tooling and production-readiness claims",
                "Required Approval Gates Before Enforcement",
                "dependency/build impact review for any enforcement tool",
                "tests proving routing behavior is unchanged",
                "tests proving scoring behavior is unchanged",
                "tests proving strategy behavior is unchanged",
                "tests proving proxy behavior is unchanged",
                "Testing Strategy For Future Enforcement",
                "Package-boundary tests should prove only what they enforce.",
                "Rollback Strategy For Future Enforcement",
                "Rollback should restore the previous package layout or enforcement configuration without changing routing behavior.")) {
            assertTrue(doc.contains(expected), "enforcement plan should include staged safeguard " + expected);
        }
    }

    @Test
    void explicitNonGoalsPreventRuntimeToolingAndProductionOverclaims() throws Exception {
        String doc = read(DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no production Java runtime behavior",
                "no records/classes/interfaces under `src/main/java`",
                "no package moves or refactors",
                "no ArchUnit or any new dependency",
                "no Maven build changes",
                "no external API clients",
                "no HTTP calls",
                "no secrets, tokens, environment variables, credentials, config, or properties",
                "no telemetry, storage, or persistence",
                "no replay execution",
                "no what-if mutation",
                "no upload/share/download/export/PDF/ZIP behavior",
                "no Docker, CI, release, signing, registry, or governance changes",
                "no proxy behavior change",
                "no strategy behavior change",
                "no core routing behavior change",
                "no scoring-internals behavior change",
                "no runtime LASE boundary implementation",
                "no runtime package-boundary enforcement",
                "no runtime workload model implementation",
                "no runtime signal ingestion",
                "no production behavior change",
                "no live-cloud validation claim",
                "no real-tenant validation claim",
                "no GPU orchestration claim",
                "no power/grid control claim",
                "no carbon-aware routing implementation claim",
                "no facility automation claim",
                "no production readiness claim",
                "no production certification claim",
                "This plan does not claim LASE package boundary is enforced.",
                "This plan does not claim ArchUnit or package-boundary tooling exists.")) {
            assertTrue(doc.contains(expected), "enforcement plan should keep explicit boundary " + expected);
        }

        for (String forbidden : List.of(
                "this sprint adds archunit",
                "archunit is now enforcing",
                "this plan enforces package boundaries",
                "lase package boundary is now enforced",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "enforcement plan must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkPlanAsPlanningOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md"),
                    path + " should link the enforcement plan");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only staged package-boundary enforcement plan"));
        assertTrue(trustMap.contains("docs/test-only enforcement plan"));
        assertTrue(audit.contains("docs/test-only staged plan for future package-boundary enforcement"));
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
