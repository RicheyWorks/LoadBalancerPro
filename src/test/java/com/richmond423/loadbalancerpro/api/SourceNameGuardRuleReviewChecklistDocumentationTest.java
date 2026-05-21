package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class SourceNameGuardRuleReviewChecklistDocumentationTest {
    private static final Path DOC = Path.of("docs/SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md");
    private static final Path RULE_CATALOG_PLAN = Path.of("docs/SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md");
    private static final Path ACCEPTANCE_CRITERIA_PLAN =
            Path.of("docs/SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md");
    private static final Path REPORT_SCHEMA_PLAN = Path.of("docs/SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md");
    private static final Path REPORT_REVIEW_CHECKLIST =
            Path.of("docs/SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void ruleReviewChecklistDocExistsAndStatesNoImplementation() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Source-Name Guard Rule Review Checklist",
                "rule review checklist only, no implementation",
                "rule review checklist only",
                "docs/test only",
                "No source scanning is added in this sprint.",
                "No report generation is added.",
                "No JSON output is generated.",
                "No JSON output files are generated.",
                "No CI workflow change is added.",
                "No PR comment or artifact behavior is added.",
                "No runtime naming guard is active.",
                "No source-name guard is implemented.",
                "No source-name guard rule implementation exists.",
                "No dry-run report generation is added.",
                "No report file is written.",
                "No dry-run command is added.",
                "source-name guard rule review checklist is not enforcement",
                "no source scanning",
                "no JSON output",
                "no report generation",
                "no runtime naming guard is active")) {
            assertTrue(doc.contains(expected), "rule review checklist should state " + expected);
        }
    }

    @Test
    void ruleReviewChecklistLinksCatalogAcceptanceSchemaAndReportReviewDocs() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md",
                "SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md",
                "SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md",
                "SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md",
                "Relationship To Source-Name Guard Rule Catalog Plan",
                "Relationship To Source-Name Guard Report Acceptance Criteria Plan")) {
            assertTrue(doc.contains(expected), "rule review checklist should link adjacent document " + expected);
        }

        for (Path path : List.of(RULE_CATALOG_PLAN, ACCEPTANCE_CRITERIA_PLAN, REPORT_SCHEMA_PLAN,
                REPORT_REVIEW_CHECKLIST)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md"),
                    path + " should link the rule review checklist");
        }
    }

    @Test
    void ruleReviewChecklistIncludesPerRuleIntentScopeAndPatternChecklists() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Per-Rule Review Checklist",
                "Rule category is clearly named.",
                "Rule intent is clear.",
                "Reviewed risky name examples are documented.",
                "Reviewed safe name examples are documented.",
                "Severity guidance is documented.",
                "False-positive risk is documented.",
                "False-negative risk is documented.",
                "Allowlist strategy is documented.",
                "Suppression strategy is documented.",
                "Reviewer action is documented.",
                "Report output expectation is documented.",
                "Rollback/removal path is documented.",
                "Rule does not claim runtime safety proof.",
                "Rule does not claim production readiness or certification.",
                "Rule Intent Checklist",
                "Rule detects possible naming overclaim only.",
                "Rule does not infer runtime behavior from name alone.",
                "Rule does not certify clean output as safe.",
                "Rule does not claim package-boundary enforcement.",
                "Rule does not claim LASE runtime enforcement.",
                "Rule Scope Checklist",
                "Rule scope is narrow.",
                "Rule avoids generated files.",
                "Rule avoids build output.",
                "Rule avoids release assets.",
                "Rule avoids examples unless separately approved.",
                "Rule avoids broad ambiguous terms.",
                "Rule is deterministic.",
                "Rule can be reviewed by humans.",
                "Pattern Specificity Checklist",
                "Pattern is specific enough to avoid normal engineering language.",
                "Pattern avoids flagging safe reviewer/evidence terms.",
                "Pattern avoids flagging intentionally documented risky examples.",
                "Pattern has clear allowlist examples.",
                "Pattern has clear failure message expectations.")) {
            assertTrue(doc.contains(expected), "rule review checklist should include rule review content " + expected);
        }
    }

    @Test
    void ruleReviewChecklistIncludesSeverityFalsePositiveAndFalseNegativeReview() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Severity Assignment Checklist",
                "`INFO` is for clarity review only",
                "`WARN` is for possible unsafe authority or overclaim",
                "`BLOCKER_CANDIDATE` is for possible production certification/control/proof implications",
                "`BLOCKER_CANDIDATE` is not an automatic build failure in report-only mode",
                "Severity does not prove unsafe runtime behavior.",
                "False-Positive Review Checklist",
                "False-positive risk is documented for the rule category.",
                "Intentionally documented risky examples are excluded or reviewed separately.",
                "Negative boundary text is not treated as an implementation claim.",
                "Safe naming families are documented.",
                "Normal engineering language is not swept into the pattern.",
                "The rule does not require runtime behavior changes just to silence a false positive.",
                "False-Negative Review Checklist",
                "False-negative risk is documented for the rule category.",
                "Reviewers acknowledge that safe names can still hide unsafe behavior.",
                "Reviewers acknowledge that naming checks do not prove package boundaries.",
                "Reviewers acknowledge that naming checks do not prove dependency direction.",
                "Reviewers acknowledge that naming checks do not prove routing, scoring, strategy, proxy, or production behavior safety.",
                "Clean output is not production safety proof.")) {
            assertTrue(doc.contains(expected), "rule review checklist should include severity/risk content " + expected);
        }
    }

    @Test
    void ruleReviewChecklistIncludesAllowlistSuppressionReportOutputRollbackAndGates() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Allowlist Review Checklist",
                "Allowlist strategy is documented.",
                "Allowlist entries require a rationale.",
                "Allowlist entries identify the rule category.",
                "Allowlist entries are reviewed by humans.",
                "Allowlist entries do not certify production safety.",
                "Suppression Review Checklist",
                "Suppression strategy is documented.",
                "Suppressions require reviewed documentation.",
                "Suppressions avoid inline ignore spam.",
                "Suppressions can be removed without changing runtime behavior.",
                "Report-Output Review Checklist",
                "Report output expectation is documented before implementation.",
                "Findings include a clear name, category, reason, severity, reviewer action, and not-proof statement.",
                "Findings are review triggers, not proof of unsafe runtime behavior.",
                "Clean output is not production safety proof.",
                "`BLOCKER_CANDIDATE` is not automatic build failure in report-only mode.",
                "Rollback Review Checklist",
                "Rollback/removal path is documented.",
                "Rule can be removed without changing runtime behavior.",
                "Rule can be disabled without package moves.",
                "Approval Gates Before Implementation",
                "Rule catalog reviewed.",
                "Rule review checklist completed.",
                "Acceptance criteria reviewed.",
                "Report schema reviewed.",
                "Sample report reviewed.",
                "False-positive risk reviewed.",
                "False-negative risk reviewed.",
                "Allowlist strategy reviewed.",
                "Deterministic output reviewed.",
                "Privacy/secret-safety reviewed.",
                "Rollback strategy reviewed.",
                "Enforcement remains future-only unless separately approved.")) {
            assertTrue(doc.contains(expected), "rule review checklist should include controls/gates " + expected);
        }
    }

    @Test
    void explicitNonGoalsPreventScanningGenerationEnforcementAndProductionOverclaims() throws Exception {
        String doc = read(DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no production Java runtime behavior",
                "no records/classes/interfaces under `src/main/java`",
                "no class renames",
                "no package moves or refactors",
                "no source scanning logic in this sprint",
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
                "This rule review checklist does not claim source-name guard rule implementation exists.",
                "This rule review checklist does not claim report generation exists.",
                "This rule review checklist does not claim source-name guard enforcement is active.",
                "This rule review checklist does not claim a runtime-enforced LASE boundary.",
                "This rule review checklist does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "rule review checklist should keep explicit boundary " + expected);
        }

        for (String forbidden : List.of(
                "source-name guard rule implementation now exists",
                "source-name guard rules are implemented",
                "report generation is now active",
                "json output is generated by this sprint",
                "source-name guard enforcement is now active",
                "runtime naming guard is now active",
                "source scanning is now active",
                "source-name guard is now implemented",
                "this rule review checklist enforces source names",
                "this rule review checklist enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "rule review checklist must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkRuleReviewChecklistAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md"),
                    path + " should link the rule review checklist");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only per-rule review questions"));
        assertTrue(trustMap.contains("Docs/test-only rule review checklist"));
        assertTrue(audit.contains("docs/test-only rule review checklist"));
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
