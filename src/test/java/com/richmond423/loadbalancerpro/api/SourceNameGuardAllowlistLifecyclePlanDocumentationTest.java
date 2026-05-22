package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class SourceNameGuardAllowlistLifecyclePlanDocumentationTest {
    private static final Path DOC = Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md");
    private static final Path ALLOWLIST_DESIGN_PLAN =
            Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md");
    private static final Path ALLOWLIST_REVIEW_CHECKLIST =
            Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md");
    private static final Path ALLOWLIST_SAMPLE_PLAN =
            Path.of("docs/SOURCE_NAME_GUARD_ALLOWLIST_SAMPLE_PLAN.md");
    private static final Path RULE_REVIEW_CHECKLIST =
            Path.of("docs/SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md");
    private static final Path RULE_CATALOG_PLAN = Path.of("docs/SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void allowlistLifecyclePlanDocExistsAndStatesNoImplementation() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Source-Name Guard Allowlist Lifecycle Plan",
                "lifecycle plan only, no allowlist implementation",
                "lifecycle plan only",
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
                "No state files are added.",
                "No enums, records, config, or source code are added.",
                "source-name guard allowlist lifecycle is not implemented",
                "source-name guard allowlist sample is not generated output",
                "source-name guard allowlist review checklist is not enforcement",
                "source-name guard allowlist is not implemented",
                "no source scanning",
                "no allowlist files",
                "no JSON/YAML/TOML output",
                "no report generation",
                "no runtime naming guard is active")) {
            assertTrue(doc.contains(expected), "allowlist lifecycle plan should state " + expected);
        }
    }

    @Test
    void allowlistLifecyclePlanLinksDesignReviewSampleRuleReviewAndCatalogDocs() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md",
                "SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md",
                "SOURCE_NAME_GUARD_ALLOWLIST_SAMPLE_PLAN.md",
                "SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md",
                "SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md",
                "Relationship To Source-Name Guard Allowlist Design Plan",
                "Relationship To Source-Name Guard Allowlist Review Checklist",
                "Relationship To Source-Name Guard Allowlist Sample Plan")) {
            assertTrue(doc.contains(expected), "allowlist lifecycle plan should link adjacent document "
                    + expected);
        }

        for (Path path : List.of(ALLOWLIST_DESIGN_PLAN, ALLOWLIST_REVIEW_CHECKLIST, ALLOWLIST_SAMPLE_PLAN,
                RULE_REVIEW_CHECKLIST, RULE_CATALOG_PLAN)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md"),
                    path + " should link the allowlist lifecycle plan");
        }
    }

    @Test
    void allowlistLifecyclePlanIncludesLifecycleStatesAndCreationWorkflow() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Future Allowlist Lifecycle States",
                "proposed",
                "under-review",
                "accepted-report-only",
                "re-review-required",
                "retired",
                "rejected",
                "migrated",
                "These are future documentation concepts only.",
                "Do not add actual state files, enums, records, config, or source code.",
                "Future Allowlist Creation Workflow",
                "Source-name guard finding appears in a future report-only output.",
                "Reviewer determines whether rename, documentation clarification, rule adjustment, or allowlist candidate is most appropriate.",
                "Candidate includes rule category.",
                "Candidate includes name or naming pattern.",
                "Candidate includes path/scope boundary.",
                "Candidate includes rationale.",
                "Candidate includes reviewer action.",
                "Candidate includes re-review trigger.",
                "Candidate includes notProofStatement.",
                "Candidate is reviewed before accepted.",
                "Candidate is not treated as production safety certification.")) {
            assertTrue(doc.contains(expected), "allowlist lifecycle plan should include lifecycle content "
                    + expected);
        }
    }

    @Test
    void allowlistLifecyclePlanIncludesReReviewExpirationRetirementAndMigrationWorkflows() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Future Allowlist Re-Review Workflow",
                "re-review when class/name changes",
                "re-review when path/scope changes",
                "re-review when rule category changes",
                "re-review before enforcement mode",
                "re-review before package moves",
                "re-review before production-readiness claims",
                "re-review when false-positive risk changes",
                "re-review when suppression becomes too broad",
                "re-review when documentation meaning changes",
                "Future Allowlist Expiration Workflow",
                "expiration must not depend on unstable timestamps unless separately approved",
                "expiration may be based on stable lifecycle triggers",
                "expiration should move entries into re-review-required, not silently remove them",
                "expiration must not change runtime behavior",
                "expiration must not imply production safety",
                "Future Allowlist Retirement Workflow",
                "retired entries remain auditable in future history if a future allowlist exists",
                "retirement reason should be documented",
                "retirement should not require runtime changes",
                "retirement should not remove evidence of why the entry once existed",
                "retirement should not certify that the underlying name is safe",
                "Future Allowlist Migration Workflow",
                "migrate entries if rule categories are renamed",
                "migrate entries if package boundaries are introduced",
                "migrate entries if source-name guard schema changes",
                "migration requires reviewer approval",
                "migration must not happen automatically in this sprint",
                "migration must not create broad suppressions")) {
            assertTrue(doc.contains(expected), "allowlist lifecycle plan should include workflow " + expected);
        }
    }

    @Test
    void allowlistLifecyclePlanIncludesAuditStaleRiskPrivacyAndImplementationGates() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Future Allowlist Audit Workflow",
                "audit for overbroad entries",
                "audit for stale rationale",
                "audit for entries that imply certification",
                "audit for entries tied to generated/build output",
                "audit for entries containing secrets, env values, tokens, local paths, or private network details",
                "audit before moving from report-only to enforcement",
                "audit before any package-boundary enforcement sprint",
                "Future Stale-Entry Risk Handling",
                "stale entry hides a renamed class",
                "stale entry no longer matches intended rule category",
                "stale entry suppresses a real overclaim",
                "stale entry becomes too broad after refactor",
                "stale entry refers to a path that no longer exists",
                "stale entry is interpreted as production safety proof",
                "stale entry bypasses human review",
                "Future Privacy And Secret-Safety Requirements",
                "no secrets",
                "no tokens",
                "no credentials",
                "no environment variable values",
                "no env values",
                "no private network details",
                "no absolute local machine paths",
                "no real reviewer names",
                "no personal data",
                "no tenant identifiers",
                "no cloud account identifiers",
                "Future Implementation Gates",
                "allowlist design reviewed",
                "allowlist review checklist completed",
                "allowlist sample reviewed",
                "allowlist lifecycle reviewed",
                "rule catalog reviewed",
                "rule review checklist reviewed",
                "false-positive risk reviewed",
                "false-negative risk reviewed",
                "stale-entry risk reviewed",
                "privacy/secret-safety reviewed",
                "deterministic output reviewed",
                "migration and retirement strategy reviewed",
                "rollback/removal reviewed",
                "no allowlist file is added unless separately approved",
                "no JSON/YAML/TOML allowlist output is added unless separately approved",
                "no source scanning logic is added unless separately approved",
                "enforcement remains future-only unless separately approved")) {
            assertTrue(doc.contains(expected), "allowlist lifecycle plan should include audit/privacy/gate "
                    + expected);
        }
    }

    @Test
    void allowlistLifecyclePlanKeepsLifecycleNonProving() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Allowlist lifecycle does not certify production safety.",
                "Allowlist lifecycle does not replace package-boundary enforcement.",
                "Allowlist lifecycle does not replace human review.",
                "Lifecycle states are reviewer metadata only.",
                "Audit findings are review triggers only.",
                "They do not prove unsafe runtime behavior and do not certify clean output as production-safe.",
                "Stale entries must not silently suppress future findings.",
                "Future allowlist lifecycle data, if separately approved later, should preserve privacy and secret safety:")) {
            assertTrue(doc.contains(expected), "allowlist lifecycle plan should keep lifecycle non-proving "
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
                "no dry-run command",
                "no dry-run implementation",
                "no report generation",
                "no JSON output files",
                "no JSON output",
                "no CI workflow changes",
                "no PR comment/report artifact behavior",
                "no state files",
                "no enums",
                "no config",
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
                "This allowlist lifecycle plan does not claim allowlist implementation exists.",
                "This allowlist lifecycle plan does not claim source-name guard rule implementation exists.",
                "This allowlist lifecycle plan does not claim report generation exists.",
                "This allowlist lifecycle plan does not claim source-name guard enforcement is active.",
                "This allowlist lifecycle plan does not claim a runtime-enforced LASE boundary.",
                "This allowlist lifecycle plan does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "allowlist lifecycle plan should keep explicit boundary "
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
                "this allowlist lifecycle plan enforces source names",
                "this allowlist lifecycle plan enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "allowlist lifecycle plan must not overclaim: "
                    + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkAllowlistLifecyclePlanAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md"),
                    path + " should link the allowlist lifecycle plan");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only future allowlist lifecycle rules"));
        assertTrue(trustMap.contains("Docs/test-only allowlist lifecycle plan"));
        assertTrue(audit.contains("docs/test-only allowlist lifecycle plan"));
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
