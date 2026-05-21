package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class SourceNameGuardReviewChecklistDocumentationTest {
    private static final Path DOC = Path.of("docs/SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md");
    private static final Path FEASIBILITY_PLAN = Path.of("docs/LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md");
    private static final Path NAMING_INVENTORY = Path.of("docs/LASE_NAMING_GUARD_INVENTORY.md");
    private static final Path NAMING_PLAN = Path.of("docs/LASE_BOUNDARY_NAMING_GUARD_PLAN.md");
    private static final Path PACKAGE_PLAN = Path.of("docs/LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void checklistDocExistsAndStatesNoSourceScanningOrRuntimeGuard() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Source-Name Guard Review Checklist",
                "checklist only, no source scanning",
                "docs/test only",
                "No source scanning is added in this sprint.",
                "No runtime naming guard is active.",
                "No source-name guard is implemented.",
                "No runtime naming enforcement is added.",
                "No source-name guard enforcement is active.",
                "source-name guard not implemented yet",
                "no source scanning",
                "no runtime naming guard is active",
                "no source-name guard is implemented",
                "no runtime naming enforcement is added",
                "no source-name guard enforcement is active",
                "no classes are renamed in this sprint",
                "no package moves are made in this sprint",
                "Source-name guard checklist is not enforcement.")) {
            assertTrue(doc.contains(expected), "checklist doc should state " + expected);
        }
    }

    @Test
    void checklistDocLinksFeasibilityPlanAndNamingInventory() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md",
                "LASE_NAMING_GUARD_INVENTORY.md",
                "Relationship To LASE Source-Name Guard Feasibility Plan",
                "Relationship To LASE Naming Guard Inventory")) {
            assertTrue(doc.contains(expected), "checklist doc should link adjacent document " + expected);
        }

        for (Path path : List.of(FEASIBILITY_PLAN, NAMING_INVENTORY, NAMING_PLAN, PACKAGE_PLAN)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md"),
                    path + " should link the source-name guard review checklist");
        }
    }

    @Test
    void checklistDocIncludesRequiredReviewQuestions() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "When This Checklist Should Be Used",
                "Required Review Questions",
                "Is the proposed guard narrow and deterministic?",
                "Does it avoid broad source scanning?",
                "Does it avoid scanning generated files, target directories, build output, release assets, or documentation examples unless separately approved?",
                "Does it avoid flagging intentionally documented risky examples?",
                "Are denylist terms specific enough to avoid normal engineering language?",
                "Is there a reviewed allowlist for known safe existing names?",
                "Is there a documented suppression process that does not encourage inline ignore spam?",
                "Are failure messages clear and actionable?",
                "Does the guard treat failures as review triggers, not proof of unsafe runtime behavior?",
                "Is the guard separate from package moves?",
                "Is the guard separate from behavior changes?",
                "Is the guard separate from production-readiness claims?",
                "Is rollback simple?",
                "Can the guard be removed without changing runtime behavior?",
                "Has the false-positive risk been reviewed?",
                "Has the false-negative risk been reviewed?")) {
            assertTrue(doc.contains(expected), "checklist doc should include review question " + expected);
        }
    }

    @Test
    void checklistDocIncludesScopeDenylistAllowlistAndSuppressionSections() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Proposed Guard Scope Checklist",
                "The guard scans only paths approved in a separate implementation sprint.",
                "The guard avoids broad source scanning.",
                "The guard does not scan generated files, `target/`, build output, release assets, dependency directories, or ignored evidence output.",
                "The guard does not flag intentionally documented risky examples as implemented classes.",
                "Denylist Term Checklist",
                "Each denylist term has a plain-language unsafe implication.",
                "Denylist terms are specific enough to avoid normal engineering language.",
                "production certification implication",
                "replay proof implication",
                "scoring proof implication",
                "live grid control implication",
                "facility automation implication",
                "GPU orchestration implication",
                "carbon-aware production routing implication",
                "hidden production routing authority implication",
                "Allowlist And Suppression Checklist",
                "Known safe existing names have a reviewed allowlist entry.",
                "Suppressions do not encourage inline ignore spam.",
                "The allowlist is small enough that reviewers can understand it.")) {
            assertTrue(doc.contains(expected), "checklist doc should include scope/denylist/allowlist content " + expected);
        }
    }

    @Test
    void checklistDocIncludesFalsePositiveFalseNegativeFailureAndRollbackSections() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "False-Positive Review Checklist",
                "False positives should be treated as guard design failures, not as evidence that runtime behavior is unsafe.",
                "False-Negative Review Checklist",
                "safe names can still hide unsafe behavior",
                "naming checks do not prove package boundaries",
                "naming checks do not prove dependency direction",
                "False negatives are why naming guards must remain weak early signals only.",
                "Failure-Message Checklist",
                "Failure messages say failures are review triggers, not proof of unsafe runtime behavior.",
                "Failure messages do not claim source-name guard enforcement proves runtime safety.",
                "Rollback And Removal Checklist",
                "Rollback is simple.",
                "The guard can be removed without changing runtime behavior.",
                "The guard can be removed without moving packages or renaming classes.",
                "The guard can be removed without changing Maven dependencies or build plugins.")) {
            assertTrue(doc.contains(expected), "checklist doc should include risk/failure/rollback content " + expected);
        }
    }

    @Test
    void checklistDocRequiresApprovalGatesAndSeparationFromMovesBehaviorAndClaims() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Approval Gates Before Implementation",
                "Separate sprint approval for source-name scanning.",
                "Complete this checklist.",
                "Source-name guard implementation must not be combined with package moves.",
                "Source-name guard implementation must not be combined with behavior changes.",
                "Source-name guard implementation must not be combined with production claims.",
                "Guard failures are review triggers, not proof of unsafe runtime behavior.")) {
            assertTrue(doc.contains(expected), "checklist doc should include implementation gate " + expected);
        }
    }

    @Test
    void explicitNonGoalsPreventSourceScanningRuntimeEnforcementAndProductionOverclaims() throws Exception {
        String doc = read(DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no source scanning in this sprint",
                "no runtime naming enforcement",
                "no source-name guard implementation",
                "no source-name guard enforcement",
                "no package-boundary enforcement",
                "no ArchUnit/tooling implementation",
                "no production Java runtime behavior",
                "no records/classes/interfaces under `src/main/java`",
                "no class renames",
                "no package moves or refactors",
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
                "no production readiness claim",
                "no production certification claim",
                "no live-cloud validation claim",
                "no real-tenant validation claim",
                "no GPU orchestration claim",
                "no power/grid control claim",
                "no carbon-aware routing implementation claim",
                "no facility automation claim",
                "This checklist does not claim source-name guard enforcement is active.",
                "This checklist does not claim a runtime-enforced LASE boundary.",
                "This checklist does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "checklist doc should keep explicit boundary " + expected);
        }

        for (String forbidden : List.of(
                "source-name guard enforcement is now active",
                "runtime naming guard is now active",
                "source scanning is now active",
                "source-name guard is now implemented",
                "this checklist enforces source names",
                "this checklist enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "checklist doc must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkChecklistAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md"),
                    path + " should link the source-name guard review checklist");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only checklist"));
        assertTrue(trustMap.contains("Docs/test-only checklist"));
        assertTrue(audit.contains("docs/test-only reviewer checklist"));
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
