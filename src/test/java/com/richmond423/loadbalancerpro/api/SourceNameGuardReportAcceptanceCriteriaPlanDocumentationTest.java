package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class SourceNameGuardReportAcceptanceCriteriaPlanDocumentationTest {
    private static final Path DOC = Path.of("docs/SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md");
    private static final Path REPORT_SAMPLE_PLAN = Path.of("docs/SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md");
    private static final Path REPORT_REVIEW_CHECKLIST = Path.of("docs/SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md");
    private static final Path REPORT_SCHEMA_PLAN = Path.of("docs/SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md");
    private static final Path DRY_RUN_PLAN = Path.of("docs/SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void acceptanceCriteriaPlanDocExistsAndStatesNoImplementation() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Source-Name Guard Report Acceptance Criteria Plan",
                "acceptance criteria plan only, no implementation",
                "acceptance criteria only",
                "docs/test only",
                "No source scanning is added in this sprint.",
                "No report generation is added.",
                "No JSON output is generated.",
                "No JSON output files are generated.",
                "No CI workflow change is added.",
                "No PR comment or artifact behavior is added.",
                "No runtime naming guard is active.",
                "No source-name guard is implemented.",
                "No dry-run report generation is added.",
                "No report file is written.",
                "No dry-run command is added.",
                "source-name guard report acceptance criteria is not enforcement",
                "no source scanning",
                "no JSON output",
                "no report generation",
                "no runtime naming guard is active")) {
            assertTrue(doc.contains(expected), "acceptance criteria plan should state " + expected);
        }
    }

    @Test
    void acceptanceCriteriaPlanLinksSampleReviewChecklistSchemaAndDryRunPlan() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md",
                "SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md",
                "SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md",
                "SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md",
                "Relationship To Source-Name Guard Report Sample Plan",
                "Relationship To Source-Name Guard Report Review Checklist",
                "Relationship To Source-Name Guard Report Schema Plan")) {
            assertTrue(doc.contains(expected), "acceptance criteria plan should link adjacent document " + expected);
        }

        for (Path path : List.of(REPORT_SAMPLE_PLAN, REPORT_REVIEW_CHECKLIST, REPORT_SCHEMA_PLAN, DRY_RUN_PLAN)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md"),
                    path + " should link the report acceptance criteria plan");
        }
    }

    @Test
    void acceptanceCriteriaPlanIncludesRequiredReportFindingAndSeverityCriteria() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Required Report Quality Criteria",
                "report is clearly marked report-only or dry-run if implemented later",
                "report includes `schemaVersion` or equivalent future schema marker",
                "report includes `scannedScopeSummary`",
                "report includes a findings list, even when empty",
                "report includes summary counts that match findings",
                "report includes `reviewerGuidance`",
                "report includes `notProvenBoundaries`",
                "report includes a statement that findings are review triggers, not runtime safety proof",
                "clean reports are explicitly not production safety proof",
                "Required Finding Quality Criteria",
                "each finding has a clear name",
                "each finding has a clear path or scope reference",
                "each finding has a category",
                "each finding has severity",
                "each finding has a specific reason",
                "each finding has `reviewerAction`",
                "each finding has `notProofStatement`",
                "each finding avoids secrets, credentials, tokens, environment values, private network details, and absolute local paths",
                "each finding avoids generated/build output unless separately approved",
                "Required Severity Quality Criteria",
                "`INFO` means review suggested and likely safe",
                "`WARN` means naming may imply unsafe authority or overclaim",
                "`BLOCKER_CANDIDATE` means human review required before any future blocking enforcement",
                "`BLOCKER_CANDIDATE` is not automatic build failure in report-only mode",
                "`BLOCKER_CANDIDATE` is not proof of unsafe runtime behavior")) {
            assertTrue(doc.contains(expected), "acceptance criteria plan should include required criteria " + expected);
        }
    }

    @Test
    void acceptanceCriteriaPlanIncludesPrivacyDeterminismAndReviewerActionCriteria() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Required Privacy And Secret-Safety Criteria",
                "report contains no secrets",
                "report contains no credentials",
                "report contains no tokens",
                "report contains no environment variable values",
                "report contains no private network details",
                "report contains no absolute local machine paths",
                "report keeps paths repository-relative if future scanning is separately approved",
                "report does not upload artifacts unless separately approved",
                "report does not post PR comments unless separately approved",
                "Required Determinism Criteria",
                "field ordering is stable",
                "finding ordering is stable and reviewer-explainable",
                "counts are derived only from the approved report scope",
                "output does not depend on filesystem traversal order",
                "output does not depend on timestamps, clocks, time zones, environment variables, system properties, random values, UUIDs, hashes, network state, or local machine state",
                "`generatedAtMode` describes timing posture",
                "`findingIdMode` describes deterministic identity posture",
                "Required Reviewer-Action Criteria",
                "every finding has a clear reviewer action",
                "reviewer actions distinguish accept, rename later, allowlist, suppression review, guard rule change, reject guard proposal, or defer until package-boundary enforcement exists",
                "reviewer actions do not require package moves in the same sprint as source-name guard work",
                "reviewer actions do not require runtime behavior changes in the same sprint as source-name guard work")) {
            assertTrue(doc.contains(expected), "acceptance criteria plan should include privacy/determinism/action " + expected);
        }
    }

    @Test
    void acceptanceCriteriaPlanIncludesRejectionCriteriaAndImplementationGates() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Rejection Criteria For Future Reports",
                "report contains secrets or environment values",
                "report contains absolute local machine paths",
                "report claims production readiness, certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, or facility automation",
                "report treats findings as proof of unsafe runtime behavior",
                "report treats clean output as proof of production safety",
                "report generates unstable timestamps, UUIDs, hashes, or random IDs without separate approval",
                "report scans broad/generated/build output without review",
                "report lacks clear reviewer actions",
                "report lacks rollback/removal guidance",
                "report is too noisy to be useful",
                "Approval Gates Before Future Implementation",
                "Schema reviewed.",
                "Sample reviewed.",
                "Review checklist reviewed.",
                "Acceptance criteria reviewed.",
                "False-positive risk reviewed.",
                "False-negative risk reviewed.",
                "Privacy/secret-safety reviewed.",
                "Deterministic-output strategy reviewed.",
                "Rollback strategy reviewed.",
                "CI impact reviewed.",
                "Enforcement remains future-only unless separately approved.")) {
            assertTrue(doc.contains(expected), "acceptance criteria plan should include rejection/gate content " + expected);
        }
    }

    @Test
    void explicitNonGoalsPreventGenerationEnforcementAndProductionOverclaims() throws Exception {
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
                "This acceptance criteria plan does not claim report generation exists.",
                "This acceptance criteria plan does not claim source-name guard enforcement is active.",
                "This acceptance criteria plan does not claim a runtime-enforced LASE boundary.",
                "This acceptance criteria plan does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "acceptance criteria plan should keep explicit boundary " + expected);
        }

        for (String forbidden : List.of(
                "report generation is now active",
                "json output is generated by this sprint",
                "source-name guard enforcement is now active",
                "runtime naming guard is now active",
                "source scanning is now active",
                "source-name guard is now implemented",
                "this acceptance criteria plan enforces source names",
                "this acceptance criteria plan enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "acceptance criteria plan must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkAcceptanceCriteriaPlanAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md"),
                    path + " should link the report acceptance criteria plan");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only acceptance criteria"));
        assertTrue(trustMap.contains("docs/test-only report acceptance criteria plan"));
        assertTrue(audit.contains("docs/test-only report acceptance criteria plan"));
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
