package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class SourceNameGuardAllowlistExitCriteriaPlanDocumentationTest {
    private static final Path DOC =
            Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_EXIT_CRITERIA_PLAN.md");
    private static final Path ALLOWLIST_DESIGN_PLAN =
            Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md");
    private static final Path ALLOWLIST_REVIEW_CHECKLIST =
            Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md");
    private static final Path ALLOWLIST_SAMPLE_PLAN =
            Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_SAMPLE_PLAN.md");
    private static final Path ALLOWLIST_LIFECYCLE_PLAN =
            Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md");
    private static final Path RULE_REVIEW_CHECKLIST =
            Path.of("docs/SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md");
    private static final Path RULE_CATALOG_PLAN = Path.of("docs/SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void allowlistExitCriteriaPlanDocExistsAndStatesNoImplementation() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Source-Name Guard Allowlist Exit Criteria Plan",
                "exit criteria only, no allowlist implementation",
                "exit criteria only",
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
                "source-name guard allowlist exit criteria is not enforcement",
                "source-name guard allowlist lifecycle is not implemented",
                "source-name guard allowlist sample is not generated output",
                "source-name guard allowlist review checklist is not enforcement",
                "source-name guard allowlist is not implemented",
                "no source scanning",
                "no allowlist files",
                "no JSON/YAML/TOML output",
                "no report generation",
                "no runtime naming guard is active")) {
            assertTrue(doc.contains(expected), "allowlist exit criteria plan should state " + expected);
        }
    }

    @Test
    void allowlistExitCriteriaPlanLinksAdjacentAllowlistAndRuleDocs() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md",
                "SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md",
                "SOURCE_NAME_GUARD_ALLOWLIST_SAMPLE_PLAN.md",
                "SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md",
                "SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md",
                "SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md",
                "Relationship To Source-Name Guard Allowlist Design Plan",
                "Relationship To Source-Name Guard Allowlist Review Checklist",
                "Relationship To Source-Name Guard Allowlist Sample Plan",
                "Relationship To Source-Name Guard Allowlist Lifecycle Plan")) {
            assertTrue(doc.contains(expected), "allowlist exit criteria plan should link adjacent document "
                    + expected);
        }

        for (Path path : List.of(ALLOWLIST_DESIGN_PLAN, ALLOWLIST_REVIEW_CHECKLIST, ALLOWLIST_SAMPLE_PLAN,
                ALLOWLIST_LIFECYCLE_PLAN, RULE_REVIEW_CHECKLIST, RULE_CATALOG_PLAN)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_ALLOWLIST_EXIT_CRITERIA_PLAN.md"),
                    path + " should link the allowlist exit criteria plan");
        }
    }

    @Test
    void allowlistExitCriteriaPlanIncludesDocumentationCompletenessAndReviewReadinessCriteria()
            throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Required Documentation Completeness Criteria",
                "allowlist design plan exists",
                "allowlist review checklist exists",
                "allowlist sample plan exists",
                "allowlist lifecycle plan exists",
                "rule catalog exists",
                "rule review checklist exists",
                "report schema plan exists",
                "report review checklist exists",
                "report acceptance criteria exists",
                "all docs clearly say planning only",
                "all docs clearly say no enforcement",
                "all docs clearly say no production-readiness claim",
                "Required Review-Readiness Criteria",
                "reviewer can understand why allowlists may be needed",
                "reviewer can evaluate an allowlist candidate",
                "reviewer can evaluate rationale quality",
                "reviewer can evaluate scope/path boundaries",
                "reviewer can identify overbroad suppressions",
                "reviewer can identify stale entries",
                "reviewer can identify invalid entries",
                "reviewer can identify when rename or documentation clarification is preferred",
                "reviewer can confirm an allowlist does not certify production safety")) {
            assertTrue(doc.contains(expected), "allowlist exit criteria plan should include criteria "
                    + expected);
        }
    }

    @Test
    void allowlistExitCriteriaPlanIncludesPrivacyDeterminismAndQualityCriteria() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Required Privacy And Secret-Safety Criteria",
                "future allowlist entries must not include secrets",
                "future allowlist entries must not include tokens",
                "future allowlist entries must not include environment variable values",
                "future allowlist entries must not include private network details",
                "future allowlist entries must not include absolute local machine paths",
                "future allowlist entries must avoid real reviewer names",
                "future allowlist entries must avoid generated unstable IDs unless separately approved",
                "Required Determinism Criteria",
                "future allowlist review should avoid unstable timestamps unless separately approved",
                "future allowlist entries should avoid UUID/random/hash identifiers unless separately approved",
                "future allowlist matching must be deterministic if implemented later",
                "future allowlist report output must be deterministic if implemented later",
                "future allowlist ordering must be stable if implemented later",
                "future allowlist lifecycle states must be reviewable and stable",
                "Required Allowlist-Quality Criteria",
                "each candidate has a rule category",
                "each candidate has a name or naming pattern",
                "each candidate has a path/scope boundary",
                "each candidate has a rationale",
                "each candidate has reviewer action",
                "each candidate has re-review trigger",
                "each candidate has notProofStatement",
                "each candidate avoids production-readiness claims",
                "each candidate avoids certification claims",
                "each candidate avoids replacing human review",
                "each candidate avoids replacing package-boundary enforcement")) {
            assertTrue(doc.contains(expected), "allowlist exit criteria plan should include quality/privacy "
                    + expected);
        }
    }

    @Test
    void allowlistExitCriteriaPlanIncludesMisuseRisksImplementationGatesAndNonExitConditions()
            throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Required Misuse-Risk Criteria",
                "allowlist must not become production safety certification",
                "allowlist must not hide real overclaim risk",
                "allowlist must not be too broad",
                "allowlist must not become stale after rule changes",
                "allowlist must not hide intentionally risky examples incorrectly",
                "allowlist must not target generated/build output unless separately approved",
                "allowlist must not become a substitute for package-boundary enforcement",
                "allowlist must not become a substitute for runtime safety controls",
                "Required Implementation-Readiness Gates",
                "design plan reviewed",
                "review checklist reviewed",
                "sample plan reviewed",
                "lifecycle plan reviewed",
                "exit criteria reviewed",
                "rule catalog reviewed",
                "rule review checklist reviewed",
                "report schema reviewed",
                "report review checklist reviewed",
                "acceptance criteria reviewed",
                "privacy/secret-safety reviewed",
                "deterministic-output strategy reviewed",
                "false-positive risk reviewed",
                "false-negative risk reviewed",
                "rollback/removal plan reviewed",
                "CI impact reviewed",
                "enforcement remains future-only unless separately approved",
                "Explicit Non-Exit Conditions",
                "missing not-proven boundaries",
                "vague allowlist rationale rules",
                "no stale-entry handling",
                "no privacy/secret-safety criteria",
                "no deterministic-output criteria",
                "no rollback/removal path",
                "no false-positive review path",
                "no false-negative review path",
                "allowlist described as production safety certification",
                "allowlist described as package-boundary enforcement",
                "allowlist described as runtime enforcement",
                "source-name guard implementation bundled with unrelated behavior changes")) {
            assertTrue(doc.contains(expected), "allowlist exit criteria plan should include gate/risk "
                    + expected);
        }
    }

    @Test
    void allowlistExitCriteriaPlanKeepsExitCriteriaNonProving() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Allowlist exit criteria do not certify production safety.",
                "Allowlist exit criteria do not replace package-boundary enforcement.",
                "Allowlist exit criteria do not replace human review.",
                "Review readiness is about human judgment.",
                "Misuse-risk criteria are non-proving review criteria.",
                "They do not imply that a clean future allowlist output proves production safety.",
                "Allowlist quality criteria do not certify production safety.",
                "The value is strategic architecture readiness only.")) {
            assertTrue(doc.contains(expected), "allowlist exit criteria plan should keep criteria non-proving "
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
                "no records/classes/interfaces/enums under `src/main/java`",
                "no class renames",
                "no package moves or refactors",
                "no source scanning logic in this sprint",
                "no allowlist files",
                "no JSON/YAML/TOML allowlist output",
                "no JSON/YAML/TOML output",
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
                "This allowlist exit criteria plan does not claim allowlist implementation exists.",
                "This allowlist exit criteria plan does not claim source-name guard rule implementation exists.",
                "This allowlist exit criteria plan does not claim report generation exists.",
                "This allowlist exit criteria plan does not claim source-name guard enforcement is active.",
                "This allowlist exit criteria plan does not claim a runtime-enforced LASE boundary.",
                "This allowlist exit criteria plan does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "allowlist exit criteria plan should keep explicit boundary "
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
                "this allowlist exit criteria plan enforces source names",
                "this allowlist exit criteria plan enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "allowlist exit criteria plan must not overclaim: "
                    + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkAllowlistExitCriteriaPlanAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_ALLOWLIST_EXIT_CRITERIA_PLAN.md"),
                    path + " should link the allowlist exit criteria plan");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only exit criteria"));
        assertTrue(trustMap.contains("Docs/test-only allowlist exit criteria plan"));
        assertTrue(audit.contains("docs/test-only allowlist exit criteria plan"));
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
