package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LaseNamingGuardInventoryDocumentationTest {
    private static final Path DOC = Path.of("docs/LASE_NAMING_GUARD_INVENTORY.md");
    private static final Path NAMING_PLAN = Path.of("docs/LASE_BOUNDARY_NAMING_GUARD_PLAN.md");
    private static final Path BOUNDARY_INVENTORY = Path.of("docs/LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md");
    private static final Path PACKAGE_PLAN = Path.of("docs/LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void namingInventoryDocExistsAndStatesInventoryOnly() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# LASE Naming Guard Inventory",
                "naming inventory only, not enforcement",
                "docs/test only",
                "No runtime naming guard is active.",
                "No classes are renamed in this sprint.",
                "No package moves are made in this sprint.",
                "No source-name guard tests are added in this sprint.",
                "No ArchUnit or package-boundary tool is added.",
                "no runtime naming guard is active",
                "no runtime naming enforcement is added",
                "no source-name guard tests are added in this sprint",
                "no classes are renamed in this sprint",
                "no package moves are made in this sprint",
                "Naming inventory does not equal runtime enforcement.",
                "LASE naming inventory is not enforcement.")) {
            assertTrue(doc.contains(expected), "naming inventory should state " + expected);
        }
    }

    @Test
    void namingInventoryLinksAdjacentArchitectureDocuments() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "LASE_BOUNDARY_NAMING_GUARD_PLAN.md",
                "LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md",
                "Relationship To LASE Boundary Naming Guard Plan",
                "Relationship To LASE Boundary Enforcement Inventory",
                "Source-name guard tests are future-only unless separately approved.")) {
            assertTrue(doc.contains(expected), "naming inventory should link adjacent document " + expected);
        }

        assertTrue(read(NAMING_PLAN).contains("LASE_NAMING_GUARD_INVENTORY.md"),
                "naming guard plan should link the naming inventory");
        assertTrue(read(BOUNDARY_INVENTORY).contains("LASE_NAMING_GUARD_INVENTORY.md"),
                "boundary enforcement inventory should link the naming inventory");
        assertTrue(read(PACKAGE_PLAN).contains("LASE_NAMING_GUARD_INVENTORY.md"),
                "package-boundary plan should link the naming inventory");
    }

    @Test
    void namingInventoryIncludesCurrentNamingCategoriesAndObservationSections() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Current Naming Categories Observed",
                "Live allocation naming",
                "LASE shadow/evaluation naming",
                "Replay/evidence naming",
                "Reviewer metadata naming",
                "Domain model naming",
                "Infrastructure/cloud/future integration naming",
                "API/view naming",
                "Configuration naming",
                "Live Allocation Naming Observations",
                "LASE Shadow/Evaluation Naming Observations",
                "Replay/Evidence Naming Observations",
                "Reviewer Metadata Naming Observations",
                "Domain Model Candidate Naming Observations",
                "Infrastructure/Cloud/Future Integration Naming Observations",
                "API/View Naming Observations",
                "Configuration Naming Observations")) {
            assertTrue(doc.contains(expected), "naming inventory should include category/section " + expected);
        }
    }

    @Test
    void namingInventoryClassifiesRequiredCurrentNames() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "`LoadBalancer`",
                "`AllocatorService`",
                "`Server`",
                "`ServerStateVector`",
                "`ServerScoreCalculator`",
                "`AdaptiveRoutingPolicyEngine`",
                "`LaseShadowAdvisor`",
                "`LaseEvaluationEngine`",
                "`LaseShadowReplayEngine`",
                "`AdaptiveRoutingScenario`",
                "`AdaptiveRoutingScenarioEvidencePacket`",
                "`RoutingComparisonResponse`",
                "`RoutingComparisonResultResponse`",
                "`RoutingDecisionReplayEvidence*`",
                "`CloudManager`",
                "`CloudAwsClients`",
                "`CloudConfig`",
                "`AllocatorController`",
                "`RoutingController`",
                "`ScenarioReplayController`",
                "`AdaptiveRoutingPolicyConfiguration`",
                "`routing-demo.html`",
                "`load-balancing-cockpit.html`")) {
            assertTrue(doc.contains(expected), "naming inventory should classify " + expected);
        }
    }

    @Test
    void namingInventoryCapturesRiskLevelNamingReadAndFutureAction() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Risk level",
                "Naming read",
                "Recommended future action",
                "Live allocation naming",
                "Reviewer metadata naming",
                "Evidence/replay intent is visible",
                "runtime authority",
                "reviewer metadata",
                "evidence-only",
                "shadow/evidence",
                "infrastructure/control boundaries",
                "Keep; review before enforcement",
                "Keep; clarify shadow/evaluation scope in docs.",
                "Keep; guard against proof wording.",
                "Keep; no external scripts/CDNs and no unsafe claims.")) {
            assertTrue(doc.contains(expected), "naming inventory should capture risk/future action " + expected);
        }
    }

    @Test
    void namingInventoryDocumentsRisksAndSafeFutureSequence() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Naming Risks Observed",
                "`LoadBalancer` and `AllocatorService` are broad live-runtime names.",
                "`AdaptiveRoutingPolicyEngine` and active-experiment policy vocabulary can blur LASE/shadow influence and live policy authority",
                "`LaseShadowReplayEngine` and replay evidence names must not imply replay proof",
                "`CloudManager` and proxy names imply live-capable infrastructure/control boundaries",
                "These risks are naming observations only. They do not prove unsafe runtime behavior.",
                "Safe Future Naming Guard Sequence",
                "1. Inventory current names.",
                "2. Document risky naming patterns.",
                "3. Add narrow docs-only guard tests.",
                "4. Add source-name guard tests only in a later approved sprint.",
                "5. Keep source-name guard tests narrow and deterministic.",
                "6. Avoid broad false-positive scans.",
                "7. Never combine naming guard enforcement with package moves.",
                "8. Never combine naming guard enforcement with behavior changes.",
                "9. Never use naming guard results to claim production readiness.",
                "Naming guard enforcement must not be combined with package moves or behavior changes.",
                "Naming guards do not equal runtime enforcement.",
                "Naming guards do not equal package-boundary enforcement.")) {
            assertTrue(doc.contains(expected), "naming inventory should document risk/sequence " + expected);
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
                "no ArchUnit or any new dependency",
                "no Maven build changes",
                "no broad source scanning",
                "no source-name guard tests in this sprint",
                "no runtime naming enforcement",
                "no package-boundary enforcement",
                "no runtime LASE boundary implementation",
                "no runtime workload model implementation",
                "no runtime signal ingestion",
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
                "This inventory does not claim a runtime-enforced LASE boundary.",
                "This inventory does not claim package-boundary enforcement is active.",
                "This inventory does not claim naming guard enforcement is active.")) {
            assertTrue(doc.contains(expected), "naming inventory should keep explicit boundary " + expected);
        }

        for (String forbidden : List.of(
                "runtime naming guard is now active",
                "naming guard enforcement is now active",
                "source-name guard tests are now active",
                "this inventory enforces naming",
                "this inventory enforces package boundaries",
                "package-boundary enforcement is now active",
                "archunit is now enforcing",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "naming inventory must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkInventoryAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("LASE_NAMING_GUARD_INVENTORY.md"),
                    path + " should link the naming inventory");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only current naming inventory"));
        assertTrue(trustMap.contains("docs/test-only naming inventory"));
        assertTrue(audit.contains("docs/test-only naming inventory"));
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
