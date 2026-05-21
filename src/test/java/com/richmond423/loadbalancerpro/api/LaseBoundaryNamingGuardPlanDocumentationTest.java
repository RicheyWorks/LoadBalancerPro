package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LaseBoundaryNamingGuardPlanDocumentationTest {
    private static final Path DOC = Path.of("docs/LASE_BOUNDARY_NAMING_GUARD_PLAN.md");
    private static final Path LASE_BOUNDARY = Path.of("docs/LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md");
    private static final Path INVENTORY = Path.of("docs/LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md");
    private static final Path ENFORCEMENT_PLAN = Path.of("docs/LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void namingGuardPlanDocExistsAndStatesPlanningOnly() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# LASE Boundary Naming Guard Plan",
                "naming plan only, not enforcement",
                "docs/test only",
                "No ArchUnit or package-boundary tool is added in this sprint.",
                "No classes are moved in this sprint.",
                "No packages are refactored.",
                "No Maven build files are changed.",
                "No runtime interface",
                "no runtime enforcement",
                "no package-boundary enforcement",
                "LASE naming guard is not runtime-enforced",
                "no runtime interface or behavior is added",
                "no runtime naming enforcement beyond documentation guard tests is added",
                "not runtime enforcement proof")) {
            assertTrue(doc.contains(expected), "naming guard plan should state " + expected);
        }
    }

    @Test
    void namingGuardPlanLinksAdjacentLaseArchitectureDocuments() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md",
                "LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md",
                "LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md",
                "Relationship To LASE Boundary Architecture Contract",
                "Relationship To LASE Boundary Enforcement Inventory",
                "Relationship To LASE Package Boundary Enforcement Plan",
                "Naming guards can support that contract by making names less ambiguous.",
                "This naming plan fits the E1-style preparation lane")) {
            assertTrue(doc.contains(expected), "naming guard plan should link adjacent architecture doc " + expected);
        }

        assertTrue(read(LASE_BOUNDARY).contains("LASE_BOUNDARY_NAMING_GUARD_PLAN.md"),
                "LASE boundary architecture contract should link the naming guard plan");
        assertTrue(read(INVENTORY).contains("LASE_BOUNDARY_NAMING_GUARD_PLAN.md"),
                "LASE boundary inventory should link the naming guard plan");
        assertTrue(read(ENFORCEMENT_PLAN).contains("LASE_BOUNDARY_NAMING_GUARD_PLAN.md"),
                "LASE package-boundary enforcement plan should link the naming guard plan");
    }

    @Test
    void namingGuardPlanIncludesFutureNamingCategories() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Future Naming Categories",
                "Live allocation naming",
                "LASE shadow/evaluation naming",
                "Replay/evidence naming",
                "Reviewer metadata naming",
                "Domain model naming",
                "Infrastructure/cloud/future integration naming",
                "API/view naming",
                "Configuration naming")) {
            assertTrue(doc.contains(expected), "naming guard plan should include category " + expected);
        }
    }

    @Test
    void namingGuardPlanDocumentsCandidateNamingRules() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Candidate Naming Rules",
                "LASE-only classes should use clear `Lase`, `Shadow`, `Evaluation`, `Replay`, `Evidence`, or `ReviewerEvidence` naming where appropriate.",
                "Live allocation classes should avoid names that imply shadow-only behavior unless they are explicitly observation-only.",
                "Reviewer metadata classes should clearly signal metadata/view/reporting responsibilities.",
                "Evidence and replay classes should not imply production route authority.",
                "Future ports should include read-only wording when they are not mutation/control paths.",
                "Names must not imply production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, or facility automation.",
                "Naming guard tests must not claim runtime enforcement.",
                "Naming guard tests must not replace package-boundary enforcement.",
                "Naming guard tests must be treated as a weak early signal only.")) {
            assertTrue(doc.contains(expected), "naming guard plan should include candidate rule " + expected);
        }
    }

    @Test
    void namingGuardPlanIncludesAllowedAndRiskyExamples() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Allowed Naming Examples",
                "LaseShadowEvaluation",
                "LaseObservationSnapshot",
                "LaseEvidenceSummary",
                "RoutingDecisionReviewerMetadata",
                "ExternalSignalSnapshot",
                "WorkloadProfileSignalMetadata",
                "Risky Naming Examples",
                "Risky examples are examples of names to avoid, not implemented classes",
                "ProductionLaseRouter",
                "CertifiedAdaptiveRouter",
                "LiveGridControlSignal",
                "GpuOrchestrator",
                "CarbonAwareProductionRouter",
                "AutoFacilityController",
                "ReplayProofValidator",
                "ScoringProofEngine",
                "These risky examples must not be read as existing source classes")) {
            assertTrue(doc.contains(expected), "naming guard plan should document example " + expected);
        }
    }

    @Test
    void namingGuardPlanKeepsFutureGuardStrategyNarrowAndNonEnforcing() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Future Docs/Test-Only Guard Strategy",
                "verify that docs describe naming guards as weak early signals",
                "verify that docs do not claim naming guards equal runtime enforcement",
                "verify that docs do not claim naming guards equal package-boundary enforcement",
                "This sprint adds only documentation guard tests for this plan itself.",
                "It does not add broad source scanning that could create flaky or broad false-positive behavior.",
                "It does not add runtime naming enforcement.",
                "It does not add ArchUnit or package-boundary tooling.",
                "Future Implementation Gates",
                "an agreed list of unsafe implication patterns",
                "proof that tests do not scan generated output or ignored artifacts",
                "proof that tests do not block honest negative boundary text",
                "separate approval before any package move, ArchUnit dependency, runtime interface, or enforcement tool is introduced")) {
            assertTrue(doc.contains(expected), "naming guard plan should keep future guard narrow " + expected);
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
                "no source scanning beyond documentation guard tests for this plan",
                "no external API clients",
                "no HTTP calls",
                "no secrets, tokens, environment variables, credentials, config, or properties",
                "no telemetry, storage, or persistence",
                "no MessageDigest, SHA, hash, UUID, random, time, environment, or system-property behavior",
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
                "no runtime naming enforcement beyond documentation guard tests",
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
                "This plan does not claim the LASE boundary is runtime-enforced.",
                "This plan does not claim the LASE package boundary is enforced.",
                "This plan does not claim ArchUnit or package-boundary tooling exists.",
                "This plan does not claim a runtime LASE naming guard exists.")) {
            assertTrue(doc.contains(expected), "naming guard plan should keep explicit boundary " + expected);
        }

        for (String forbidden : List.of(
                "this sprint adds archunit",
                "archunit is now enforcing",
                "this plan enforces package boundaries",
                "lase package boundary is now enforced",
                "runtime lase boundary is implemented",
                "runtime naming guard is implemented",
                "naming guards enforce runtime behavior",
                "package-boundary enforcement is now active",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "naming guard plan must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkPlanAsPlanningOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("LASE_BOUNDARY_NAMING_GUARD_PLAN.md"),
                    path + " should link the naming guard plan");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("future docs/test-only naming guard vocabulary")
                || readme.contains("docs/test-only naming guard plan"));
        assertTrue(trustMap.contains("docs/test-only naming plan"));
        assertTrue(audit.contains("docs/test-only naming guard plan"));
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
