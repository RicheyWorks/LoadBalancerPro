package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class SourceNameGuardAllowlistReviewChecklistDocumentationTest {
    private static final Path DOC = Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md");
    private static final Path ALLOWLIST_DESIGN_PLAN =
            Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md");
    private static final Path RULE_REVIEW_CHECKLIST =
            Path.of("docs/SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md");
    private static final Path RULE_CATALOG_PLAN = Path.of("docs/SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md");
    private static final Path REPORT_REVIEW_CHECKLIST =
            Path.of("docs/SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void allowlistReviewChecklistDocExistsAndStatesNoImplementation() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Source-Name Guard Allowlist Review Checklist",
                "allowlist review checklist only, no implementation",
                "allowlist review checklist only",
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
                "source-name guard allowlist review checklist is not enforcement",
                "source-name guard allowlist is not implemented",
                "no source scanning",
                "no allowlist files",
                "no JSON/YAML/TOML output",
                "no report generation",
                "no runtime naming guard is active")) {
            assertTrue(doc.contains(expected), "allowlist review checklist should state " + expected);
        }
    }

    @Test
    void allowlistReviewChecklistLinksDesignRuleReviewCatalogAndReportReviewDocs() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md",
                "SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md",
                "SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md",
                "SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md",
                "Relationship To Source-Name Guard Allowlist Design Plan",
                "Relationship To Source-Name Guard Rule Review Checklist")) {
            assertTrue(doc.contains(expected), "allowlist review checklist should link adjacent document "
                    + expected);
        }

        for (Path path : List.of(ALLOWLIST_DESIGN_PLAN, RULE_REVIEW_CHECKLIST, RULE_CATALOG_PLAN,
                REPORT_REVIEW_CHECKLIST)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md"),
                    path + " should link the allowlist review checklist");
        }
    }

    @Test
    void allowlistReviewChecklistIncludesCandidateRationaleScopeAndRuleCategoryReview() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Allowlist Candidate Review Checklist",
                "Candidate has a clear rule category.",
                "Candidate has a clear name or naming pattern.",
                "Candidate has a clear path/scope boundary.",
                "Candidate has a clear rationale.",
                "Candidate identifies why rename or documentation clarification is not preferred.",
                "Candidate identifies reviewer action.",
                "Candidate includes notProofStatement.",
                "Candidate has a re-review trigger.",
                "Candidate does not certify production safety.",
                "Candidate does not replace package-boundary enforcement.",
                "Candidate does not replace human review.",
                "Rationale Review Checklist",
                "Rationale is specific.",
                "Rationale references the rule category.",
                "Rationale explains why the name is acceptable.",
                "Rationale avoids production-readiness claims.",
                "Rationale avoids certification claims.",
                "Rationale avoids live-cloud or real-tenant validation claims.",
                "Rationale avoids GPU/grid/facility/carbon-control claims.",
                "Scope/Path Review Checklist",
                "Scope is narrow.",
                "Scope avoids generated files.",
                "Scope avoids build output.",
                "Scope avoids release assets.",
                "Scope avoids broad repository-wide suppression unless separately approved.",
                "Scope avoids absolute local machine paths.",
                "Scope avoids secrets or private network details.",
                "Rule-Category Review Checklist",
                "Candidate maps to a reviewed source-name guard rule category.",
                "Candidate does not invent an ad hoc category only to hide a finding.")) {
            assertTrue(doc.contains(expected), "allowlist review checklist should include candidate review "
                    + expected);
        }
    }

    @Test
    void allowlistReviewChecklistIncludesReReviewSuppressionPrivacyAndMisuseReview() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Re-Review Trigger Checklist",
                "Re-review on class/name changes.",
                "Re-review on rule category changes.",
                "Re-review before enforcement mode.",
                "Re-review before package moves.",
                "Re-review before production-readiness claims.",
                "Re-review if false-positive rate changes.",
                "Re-review if suppression becomes too broad.",
                "Suppression Review Checklist",
                "Suppression is reviewed.",
                "Suppression is documented.",
                "Suppression is not inline ignore spam.",
                "Suppression does not hide whole categories without explicit review.",
                "Suppression does not suppress production-certification/control/proof terms without maintainer review.",
                "Suppression remains auditable.",
                "Suppression can be removed without runtime behavior changes.",
                "Privacy And Secret-Safety Checklist",
                "no secrets",
                "no environment variable values",
                "no absolute local machine paths",
                "Misuse-Risk Checklist",
                "allowlist treated as certification",
                "allowlist used to hide unsafe overclaims",
                "allowlist too broad",
                "allowlist stale after rule/category changes",
                "allowlist hides intentionally risky examples incorrectly",
                "allowlist tied to generated paths",
                "allowlist includes secrets, env values, tokens, private network details, or local machine paths",
                "allowlist used instead of package-boundary enforcement")) {
            assertTrue(doc.contains(expected), "allowlist review checklist should include review controls "
                    + expected);
        }
    }

    @Test
    void allowlistReviewChecklistIncludesApprovalGates() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Approval Gates Before Allowlist Implementation",
                "Allowlist design reviewed.",
                "Allowlist review checklist completed.",
                "Rule catalog reviewed.",
                "Rule review checklist reviewed.",
                "Candidate rationale reviewed.",
                "Scope/path reviewed.",
                "Rule category reviewed.",
                "Re-review trigger reviewed.",
                "Suppression review completed.",
                "Privacy/secret-safety reviewed.",
                "Misuse risks reviewed.",
                "False-positive risk reviewed.",
                "False-negative risk reviewed.",
                "Deterministic output reviewed.",
                "No allowlist file is added unless separately approved.",
                "No JSON/YAML/TOML allowlist output is added unless separately approved.",
                "No source scanning logic is added unless separately approved.",
                "Enforcement remains future-only unless separately approved.")) {
            assertTrue(doc.contains(expected), "allowlist review checklist should include approval gate "
                    + expected);
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
                "This allowlist review checklist does not claim allowlist implementation exists.",
                "This allowlist review checklist does not claim source-name guard rule implementation exists.",
                "This allowlist review checklist does not claim report generation exists.",
                "This allowlist review checklist does not claim source-name guard enforcement is active.",
                "This allowlist review checklist does not claim a runtime-enforced LASE boundary.",
                "This allowlist review checklist does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "allowlist review checklist should keep explicit boundary "
                    + expected);
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
                "this allowlist review checklist enforces source names",
                "this allowlist review checklist enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "allowlist review checklist must not overclaim: "
                    + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkAllowlistReviewAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md"),
                    path + " should link the allowlist review checklist");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only allowlist candidate review criteria"));
        assertTrue(trustMap.contains("Docs/test-only allowlist review checklist"));
        assertTrue(audit.contains("docs/test-only allowlist review checklist"));
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
