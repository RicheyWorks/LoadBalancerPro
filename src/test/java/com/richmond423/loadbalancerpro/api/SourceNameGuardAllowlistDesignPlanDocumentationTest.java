package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class SourceNameGuardAllowlistDesignPlanDocumentationTest {
    private static final Path DOC = Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md");
    private static final Path RULE_REVIEW_CHECKLIST =
            Path.of("docs/SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md");
    private static final Path RULE_CATALOG_PLAN = Path.of("docs/SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md");
    private static final Path ACCEPTANCE_CRITERIA_PLAN =
            Path.of("docs/SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md");
    private static final Path REPORT_REVIEW_CHECKLIST =
            Path.of("docs/SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void allowlistDesignPlanDocExistsAndStatesNoImplementation() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Source-Name Guard Allowlist Design Plan",
                "allowlist design only, no implementation",
                "allowlist design only",
                "docs/test only",
                "No source scanning is added in this sprint.",
                "No allowlist file is added.",
                "No allowlist files are added.",
                "No JSON/YAML/TOML output is added.",
                "No JSON/YAML/TOML allowlist output is added.",
                "No report generation is added.",
                "No CI workflow change is added.",
                "No PR comment or artifact behavior is added.",
                "No runtime naming guard is active.",
                "No source-name guard is implemented.",
                "No source-name guard rule implementation exists.",
                "No dry-run report generation is added.",
                "No report file is written.",
                "No dry-run command is added.",
                "source-name guard allowlist is not implemented",
                "no source scanning",
                "no allowlist files",
                "no JSON/YAML/TOML output",
                "no report generation",
                "no runtime naming guard is active")) {
            assertTrue(doc.contains(expected), "allowlist design plan should state " + expected);
        }
    }

    @Test
    void allowlistDesignPlanLinksRuleReviewCatalogAcceptanceAndReportReviewDocs() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md",
                "SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md",
                "SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md",
                "SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md",
                "Relationship To Source-Name Guard Rule Review Checklist",
                "Relationship To Source-Name Guard Rule Catalog Plan")) {
            assertTrue(doc.contains(expected), "allowlist design plan should link adjacent document " + expected);
        }

        for (Path path : List.of(RULE_REVIEW_CHECKLIST, RULE_CATALOG_PLAN, ACCEPTANCE_CRITERIA_PLAN,
                REPORT_REVIEW_CHECKLIST)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md"),
                    path + " should link the allowlist design plan");
        }
    }

    @Test
    void allowlistDesignPlanIncludesPurposeEntryFieldsAndWorkflow() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Future Allowlist Purpose",
                "document known-safe exceptions for future source-name guard findings",
                "preserve reviewer context for why a name is acceptable",
                "reduce repeated false positives",
                "keep source-name guard output reviewable and low-noise",
                "never certify production safety",
                "never replace package-boundary enforcement",
                "never replace human review",
                "Allowlist entries do not certify production safety.",
                "Allowlist does not replace package-boundary enforcement.",
                "Allowlist does not replace human review.",
                "Future Allowlist Entry Fields",
                "`allowlistEntryMode`",
                "`ruleCategory`",
                "`namePattern`",
                "`pathScope`",
                "`rationale`",
                "`reviewerAction`",
                "`reReviewTrigger`",
                "`expirationMode`",
                "`notProofStatement`",
                "`linkedDocumentation`",
                "Use allowlistEntryMode instead of generated IDs.",
                "Do not propose UUIDs, hashes, random IDs, or timestamp-generated IDs.",
                "Do not add an actual allowlist file in this sprint.",
                "Future Allowlist Review Workflow",
                "Finding appears in future report-only output.",
                "Reviewer determines whether the finding is expected.",
                "Reviewer chooses rename, documentation clarification, rule adjustment, or allowlist candidate.",
                "Allowlist candidate must include rationale.",
                "Allowlist candidate must identify rule category.",
                "Allowlist candidate must include path/scope.",
                "Allowlist candidate must include re-review trigger.",
                "Allowlist candidate must not be treated as production safety certification.",
                "Allowlist candidate must be reviewed before any future enforcement mode.")) {
            assertTrue(doc.contains(expected), "allowlist design plan should include purpose/fields/workflow "
                    + expected);
        }
    }

    @Test
    void allowlistDesignPlanIncludesExpirationSuppressionPrivacyDeterminismAndMisuseRisk() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Future Allowlist Expiration And Re-Review",
                "related class names change",
                "rule categories change",
                "before moving from report-only to enforcement",
                "before package moves",
                "before any production-readiness claim",
                "Expiration must not depend on unstable timestamps unless separately approved.",
                "Future Suppression Strategy",
                "suppressions should be reviewed and documented",
                "suppressions should not become inline ignore spam",
                "suppressions should not hide broad categories",
                "suppressions should not suppress production-certification/control claims without explicit maintainer review",
                "suppressions should remain auditable",
                "suppressions should be removable without runtime behavior changes",
                "Future Allowlist Privacy And Secret-Safety Rules",
                "no secrets",
                "no tokens",
                "no environment variable values",
                "no absolute local machine paths",
                "repository-relative paths only if future scanning is separately approved",
                "Future Allowlist Deterministic-Output Rules",
                "stable ordering by reviewed rule category, path scope, and name pattern",
                "no UUID values",
                "no random values",
                "no hashes or SHA values",
                "no generated timestamp IDs",
                "`allowlistEntryMode` is not a generated ID",
                "`expirationMode` is not a timestamp unless separately approved",
                "Future Allowlist Misuse Risks",
                "allowlist treated as safety certification",
                "allowlist hides real overclaim risk",
                "allowlist becomes too broad",
                "allowlist suppresses intentional risky examples incorrectly",
                "allowlist tied to unstable paths or generated output",
                "allowlist includes secrets or local machine paths",
                "allowlist becomes replacement for package-boundary enforcement")) {
            assertTrue(doc.contains(expected), "allowlist design plan should include review controls " + expected);
        }
    }

    @Test
    void allowlistDesignPlanIncludesImplementationGates() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Future Implementation Gates",
                "rule review checklist reviewed",
                "rule catalog reviewed",
                "allowlist design reviewed",
                "report schema reviewed",
                "report acceptance criteria reviewed",
                "report review checklist reviewed",
                "false-positive risk reviewed",
                "false-negative risk reviewed",
                "privacy/secret-safety reviewed",
                "deterministic output reviewed",
                "suppression process reviewed",
                "allowlist entry fields reviewed",
                "expiration and re-review posture reviewed",
                "rollback/removal path reviewed",
                "no allowlist file or output until separately approved",
                "no JSON/YAML/TOML output until separately approved",
                "no source scanning until separately approved",
                "no report generation until separately approved",
                "no CI workflow change until separately approved",
                "no PR comment/report artifact behavior until separately approved",
                "enforcement remains future-only unless separately approved")) {
            assertTrue(doc.contains(expected), "allowlist design plan should include implementation gate " + expected);
        }
    }

    @Test
    void explicitNonGoalsPreventAllowlistFilesScanningGenerationEnforcementAndProductionOverclaims()
            throws Exception {
        String doc = read(DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no production Java runtime behavior",
                "no records/classes/interfaces under `src/main/java`",
                "no class renames",
                "no package moves or refactors",
                "no source scanning logic in this sprint",
                "no allowlist files",
                "no JSON/YAML/TOML allowlist output",
                "no dry-run command",
                "no dry-run implementation",
                "no report generation",
                "no JSON output files",
                "no JSON output",
                "no CI workflow changes",
                "no PR comment/report artifact behavior",
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
                "no production readiness claim",
                "no production certification claim",
                "no live-cloud validation claim",
                "no real-tenant validation claim",
                "no GPU orchestration claim",
                "no power/grid control claim",
                "no carbon-aware routing implementation claim",
                "no facility automation claim",
                "This allowlist design plan does not claim allowlist implementation exists.",
                "This allowlist design plan does not claim source-name guard rule implementation exists.",
                "This allowlist design plan does not claim report generation exists.",
                "This allowlist design plan does not claim source-name guard enforcement is active.",
                "This allowlist design plan does not claim a runtime-enforced LASE boundary.",
                "This allowlist design plan does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "allowlist design plan should keep explicit boundary " + expected);
        }

        for (String forbidden : List.of(
                "allowlist implementation now exists",
                "allowlist file is now active",
                "allowlist files are implemented",
                "json/yaml/toml output is generated by this sprint",
                "source-name guard rule implementation now exists",
                "source-name guard rules are implemented",
                "report generation is now active",
                "source-name guard enforcement is now active",
                "runtime naming guard is now active",
                "source scanning is now active",
                "source-name guard is now implemented",
                "this allowlist design plan enforces source names",
                "this allowlist design plan enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "allowlist design plan must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkAllowlistDesignAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md"),
                    path + " should link the allowlist design plan");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only future allowlist semantics"));
        assertTrue(trustMap.contains("Docs/test-only allowlist design plan"));
        assertTrue(audit.contains("docs/test-only allowlist design plan"));
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
