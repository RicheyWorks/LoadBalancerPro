package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class SourceNameGuardAllowlistSamplePlanDocumentationTest {
    private static final Path DOC = Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_SAMPLE_PLAN.md");
    private static final Path ALLOWLIST_DESIGN_PLAN =
            Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md");
    private static final Path ALLOWLIST_REVIEW_CHECKLIST =
            Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md");
    private static final Path RULE_REVIEW_CHECKLIST =
            Path.of("docs/SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md");
    private static final Path RULE_CATALOG_PLAN = Path.of("docs/SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void allowlistSamplePlanDocExistsAndStatesStaticExamplesOnly() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Source-Name Guard Allowlist Sample Plan",
                "sample plan only, no allowlist implementation",
                "sample plan only",
                "docs/test only",
                "examples are static documentation examples",
                "examples are not generated output",
                "The examples in this document are static documentation examples, not generated output.",
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
                "source-name guard allowlist sample is not generated output",
                "source-name guard allowlist is not implemented",
                "source-name guard allowlist review checklist is not enforcement",
                "no source scanning",
                "no allowlist files",
                "no JSON/YAML/TOML output",
                "no report generation",
                "no runtime naming guard is active")) {
            assertTrue(doc.contains(expected), "allowlist sample plan should state " + expected);
        }
    }

    @Test
    void allowlistSamplePlanLinksDesignReviewChecklistRuleReviewAndCatalogDocs() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md",
                "SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md",
                "SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md",
                "SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md",
                "Relationship To Source-Name Guard Allowlist Design Plan",
                "Relationship To Source-Name Guard Allowlist Review Checklist")) {
            assertTrue(doc.contains(expected), "allowlist sample plan should link adjacent document "
                    + expected);
        }

        for (Path path : List.of(ALLOWLIST_DESIGN_PLAN, ALLOWLIST_REVIEW_CHECKLIST, RULE_REVIEW_CHECKLIST,
                RULE_CATALOG_PLAN)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_ALLOWLIST_SAMPLE_PLAN.md"),
                    path + " should link the allowlist sample plan");
        }
    }

    @Test
    void allowlistSamplePlanIncludesNarrowDocumentationClarificationAndRenameSamples() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Sample Narrow Allowlist Entry",
                "allowlistEntryMode: documentation-example",
                "ruleCategory: reviewer-metadata-ambiguity",
                "namePattern: ExampleReviewerMetadata",
                "pathScope: docs-example-only",
                "rationale: name is reviewer metadata in this example and does not imply runtime authority",
                "reviewerAction: keep under review",
                "reReviewTrigger: re-review if name, rule category, or package boundary changes",
                "notProofStatement: allowlist entry does not certify production safety",
                "Sample Documentation-Clarification-Preferred Outcome",
                "candidate that should be resolved by documentation clarification rather than allowlist",
                "reason: name is acceptable but reviewer meaning needs clarification",
                "reviewerAction: clarify documentation in a separate scoped PR",
                "notProofStatement: clarification does not prove runtime safety",
                "Sample Rename-Preferred Outcome",
                "candidate that should be resolved by a future rename rather than allowlist",
                "reason: name implies proof/control/certification too strongly",
                "reviewerAction: propose rename in separate scoped PR",
                "notProofStatement: rename recommendation is not proof of unsafe runtime behavior")) {
            assertTrue(doc.contains(expected), "allowlist sample plan should include sample content " + expected);
        }
    }

    @Test
    void allowlistSamplePlanIncludesSuppressionStaleAndInvalidSamples() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Sample Suppression-Review-Required Outcome",
                "candidate that requires explicit suppression review",
                "reason: suppression would hide a high-risk category",
                "reviewerAction: maintainer review required before any future suppression",
                "notProofStatement: suppression does not certify production safety",
                "Sample Stale Allowlist Re-Review Outcome",
                "candidate that must be re-reviewed because a rule category or path scope changed",
                "reviewerAction: re-check rationale before any future enforcement",
                "notProofStatement: stale allowlist is not production safety proof",
                "Sample Invalid Allowlist Entry",
                "missing rationale",
                "overbroad path scope",
                "claims production safety",
                "references generated output or local machine path",
                "reviewerAction: reject candidate",
                "notProofStatement: invalid entry must not suppress future findings")) {
            assertTrue(doc.contains(expected), "allowlist sample plan should include review outcome " + expected);
        }
    }

    @Test
    void allowlistSamplePlanKeepsExamplesNonProving() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Allowlist examples do not certify production safety.",
                "Allowlist examples do not replace package-boundary enforcement.",
                "Allowlist examples do not replace human review.",
                "The sample entries are review triggers and reviewer training aids only.",
                "They do not prove runtime safety.",
                "The example name is a name to avoid, not an implemented class.",
                "These examples are not machine-generated. They are documentation-only mock examples.",
                "invalid entry must not suppress future findings")) {
            assertTrue(doc.contains(expected), "allowlist sample plan should keep examples non-proving "
                    + expected);
        }
    }

    @Test
    void allowlistSamplePlanIncludesPrivacyAndSecretSafetyConstraints() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "The static samples must not include:",
                "real timestamps",
                "UUIDs",
                "hashes",
                "absolute local machine paths",
                "secrets",
                "tokens",
                "environment variable values",
                "private network details",
                "real reviewer names",
                "personal data",
                "should not invent reviewer names, initials, personal data, timestamps, UUIDs, hashes, or absolute local paths")) {
            assertTrue(doc.contains(expected), "allowlist sample plan should include privacy/secret-safety "
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
                "This allowlist sample plan does not claim allowlist implementation exists.",
                "This allowlist sample plan does not claim source-name guard rule implementation exists.",
                "This allowlist sample plan does not claim report generation exists.",
                "This allowlist sample plan does not claim source-name guard enforcement is active.",
                "This allowlist sample plan does not claim a runtime-enforced LASE boundary.",
                "This allowlist sample plan does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "allowlist sample plan should keep explicit boundary "
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
                "this allowlist sample plan enforces source names",
                "this allowlist sample plan enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "allowlist sample plan must not overclaim: "
                    + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkAllowlistSamplePlanAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_ALLOWLIST_SAMPLE_PLAN.md"),
                    path + " should link the allowlist sample plan");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only static examples"));
        assertTrue(trustMap.contains("Docs/test-only allowlist sample plan"));
        assertTrue(audit.contains("docs/test-only allowlist sample plan"));
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
