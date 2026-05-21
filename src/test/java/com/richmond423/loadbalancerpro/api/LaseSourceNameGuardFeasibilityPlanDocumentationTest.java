package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LaseSourceNameGuardFeasibilityPlanDocumentationTest {
    private static final Path DOC = Path.of("docs/LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md");
    private static final Path NAMING_PLAN = Path.of("docs/LASE_BOUNDARY_NAMING_GUARD_PLAN.md");
    private static final Path NAMING_INVENTORY = Path.of("docs/LASE_NAMING_GUARD_INVENTORY.md");
    private static final Path PACKAGE_PLAN = Path.of("docs/LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void feasibilityDocExistsAndStatesNoSourceScanning() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# LASE Source-Name Guard Feasibility Plan",
                "feasibility plan only, not source scanning",
                "docs/test only",
                "No source scanning is added in this sprint.",
                "No runtime naming guard is active.",
                "No runtime naming enforcement is added.",
                "No classes are renamed in this sprint.",
                "No package moves are made in this sprint.",
                "No source-name guard enforcement is added.",
                "source-name guard not implemented yet",
                "no source scanning",
                "no runtime naming guard is active",
                "no runtime naming enforcement is added",
                "no source-name guard enforcement is active",
                "no classes are renamed in this sprint",
                "no package moves are made in this sprint")) {
            assertTrue(doc.contains(expected), "feasibility doc should state " + expected);
        }
    }

    @Test
    void feasibilityDocLinksNamingPlanAndInventory() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "LASE_BOUNDARY_NAMING_GUARD_PLAN.md",
                "LASE_NAMING_GUARD_INVENTORY.md",
                "Relationship To LASE Boundary Naming Guard Plan",
                "Relationship To LASE Naming Guard Inventory",
                "The inventory remains inventory only. This feasibility plan remains feasibility only.")) {
            assertTrue(doc.contains(expected), "feasibility doc should link adjacent document " + expected);
        }

        assertTrue(read(NAMING_PLAN).contains("LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md"),
                "naming guard plan should link the source-name feasibility plan");
        assertTrue(read(NAMING_INVENTORY).contains("LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md"),
                "naming inventory should link the source-name feasibility plan");
        assertTrue(read(PACKAGE_PLAN).contains("LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md"),
                "package-boundary plan should link the source-name feasibility plan");
    }

    @Test
    void feasibilityDocExplainsNarrowScopeAndOutOfScopePatterns() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Why Source-Name Guards Must Be Narrow",
                "Candidate Future Source-Name Guard Scope",
                "only scan stable production source file names",
                "do not scan generated output, ignored artifacts, `target/`, release bundles, or dependency directories",
                "start with very narrow denylist terms that imply unsafe production claims",
                "consider docs/test-only guard coverage before any source guard",
                "prefer explicit allowlists for known safe class names and safe naming families",
                "require clear failure messages",
                "require a documented review process before adding new denylist terms",
                "require easy suppression only through reviewed documentation, not inline ignore spam",
                "treat guard failures as review triggers, not proof of unsafe runtime behavior",
                "Out-Of-Scope Patterns",
                "broad source-content scanning",
                "scanning documentation examples as if they were implemented classes",
                "scanning negative boundary text as unsafe claims",
                "claiming runtime enforcement from naming checks")) {
            assertTrue(doc.contains(expected), "feasibility doc should cover scope/out-of-scope " + expected);
        }
    }

    @Test
    void feasibilityDocDocumentsFalsePositiveAndFalseNegativeRisks() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "False-Positive Risks",
                "Risky examples in docs",
                "Negative boundary language",
                "Existing live allocation names",
                "LASE evidence names",
                "False positives should be treated as guard design failures, not as evidence that runtime behavior is unsafe.",
                "False-Negative Risks",
                "Unsafe behavior with safe names",
                "Split wording",
                "API/view ambiguity",
                "Infrastructure coupling",
                "naming guards must never replace architecture review, tests, dependency-direction checks, or runtime audits")) {
            assertTrue(doc.contains(expected), "feasibility doc should document risk " + expected);
        }
    }

    @Test
    void feasibilityDocIncludesAllowlistDenylistReviewWorkflowAndGates() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Allowlist And Denylist Strategy",
                "keep the denylist very small and focused on unsafe implication themes",
                "include allowlists for known safe existing names and safe naming families",
                "Potential future denylist themes",
                "production certification implication",
                "replay proof implication",
                "scoring proof implication",
                "live grid control implication",
                "facility automation implication",
                "GPU orchestration implication",
                "carbon-aware production routing implication",
                "hidden production routing authority implication",
                "Future Review Workflow",
                "Guard failures are review triggers, not proof of unsafe runtime behavior.",
                "Future Implementation Gates",
                "explicit sprint approval for source-name scanning",
                "approved source path scope",
                "approved denylist themes",
                "approved allowlist strategy",
                "clear failure messages")) {
            assertTrue(doc.contains(expected), "feasibility doc should include strategy/workflow/gate " + expected);
        }
    }

    @Test
    void feasibilityDocIncludesRiskyExamplesAsNamesToAvoidAndSafeFamilies() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Example Risky Future Source-Name Patterns",
                "Risky examples are names to avoid, not implemented classes",
                "CertifiedRouter",
                "ProductionCertifiedBalancer",
                "ReplayProofValidator",
                "ScoringProofEngine",
                "LiveGridController",
                "FacilityAutomationManager",
                "GpuOrchestrator",
                "CarbonAwareProductionRouter",
                "AutonomousProductionRouter",
                "These examples must not be read as existing source classes",
                "Example Safe Existing/Future Naming Families",
                "LaseShadow",
                "LaseEvaluation",
                "LaseEvidence",
                "ReviewerMetadata",
                "ReviewerSummary",
                "ExternalSignalSnapshot",
                "WorkloadProfileSignalMetadata",
                "BoundaryInventory",
                "BoundaryPlan",
                "Safe naming families do not prove safe runtime behavior.")) {
            assertTrue(doc.contains(expected), "feasibility doc should include example/family " + expected);
        }
    }

    @Test
    void explicitNonGoalsPreventRuntimeNamingEnforcementAndProductionOverclaims() throws Exception {
        String doc = read(DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no production Java runtime behavior",
                "no records/classes/interfaces under `src/main/java`",
                "no class renames",
                "no package moves or refactors",
                "no source scanning logic in this sprint",
                "no runtime naming enforcement",
                "no source-name guard enforcement",
                "no package-boundary enforcement",
                "no runtime LASE boundary implementation",
                "no runtime workload model implementation",
                "no runtime signal ingestion",
                "no ArchUnit or any new dependency",
                "no Maven build changes",
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
                "no live-cloud validation claim",
                "no real-tenant validation claim",
                "no GPU orchestration claim",
                "no power/grid control claim",
                "no carbon-aware routing implementation claim",
                "no facility automation claim",
                "no production readiness claim",
                "no production certification claim",
                "Source-name guard enforcement must not be combined with package moves or behavior changes.",
                "This feasibility plan does not claim source-name guard enforcement is active.",
                "This feasibility plan does not claim a runtime-enforced LASE boundary.",
                "This feasibility plan does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "feasibility doc should keep explicit boundary " + expected);
        }

        for (String forbidden : List.of(
                "source-name guard enforcement is now active",
                "runtime naming guard is now active",
                "source scanning is now active",
                "this feasibility plan enforces source names",
                "this plan enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "feasibility plan must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkFeasibilityPlanAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md"),
                    path + " should link the source-name feasibility plan");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only feasibility"));
        assertTrue(trustMap.contains("Docs/test-only feasibility plan"));
        assertTrue(audit.contains("docs/test-only feasibility plan"));
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
