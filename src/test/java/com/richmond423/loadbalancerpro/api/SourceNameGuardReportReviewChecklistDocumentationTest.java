package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class SourceNameGuardReportReviewChecklistDocumentationTest {
    private static final Path DOC = Path.of("docs/SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md");
    private static final Path REPORT_SCHEMA_PLAN = Path.of("docs/SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md");
    private static final Path DRY_RUN_PLAN = Path.of("docs/SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md");
    private static final Path SOURCE_GUARD_REVIEW_CHECKLIST = Path.of("docs/SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void reportReviewChecklistDocExistsAndStatesNoReportGenerationOrSourceScanning() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Source-Name Guard Report Review Checklist",
                "review checklist only, no report generation",
                "docs/test only",
                "No source scanning is added in this sprint.",
                "No JSON output is generated.",
                "No CI workflow change is added.",
                "No PR comment or artifact behavior is added.",
                "No runtime naming guard is active.",
                "No source-name guard is implemented.",
                "No dry-run report generation is added.",
                "No report file is written.",
                "No dry-run command is added.",
                "source-name guard report review checklist is not enforcement",
                "no source scanning",
                "no JSON output",
                "no report generation",
                "no runtime naming guard is active")) {
            assertTrue(doc.contains(expected), "report review checklist should state " + expected);
        }
    }

    @Test
    void reportReviewChecklistLinksSchemaPlanDryRunDesignAndAdjacentChecklist() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md",
                "SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md",
                "SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md",
                "Relationship To Source-Name Guard Report Schema Plan",
                "Relationship To Source-Name Guard Dry-Run Design Plan")) {
            assertTrue(doc.contains(expected), "report review checklist should link adjacent document " + expected);
        }

        for (Path path : List.of(REPORT_SCHEMA_PLAN, DRY_RUN_PLAN, SOURCE_GUARD_REVIEW_CHECKLIST)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md"),
                    path + " should link the report review checklist");
        }
    }

    @Test
    void reportReviewChecklistIncludesReportCompletenessAndFindingReviewSections() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Report Completeness Checklist",
                "`schemaVersion` is present",
                "`sourceNameGuardMode` is report-only or dry-run if implemented later",
                "`scannedScopeSummary` is clear",
                "Findings are grouped consistently",
                "Summary counts match findings",
                "`notProvenBoundaries` are present",
                "`reviewerGuidance` is present",
                "A privacy/secret-safety statement is present",
                "Finding Review Checklist",
                "Each finding has a clear name, path, and category.",
                "Each finding has a specific reason.",
                "Each finding has a reviewer action.",
                "Each finding includes a `notProofStatement`.",
                "Each finding does not expose secrets.",
                "Each finding is not based on generated/build output unless separately approved.",
                "Each finding is not based only on intentionally documented risky examples.")) {
            assertTrue(doc.contains(expected), "report review checklist should include report/finding content " + expected);
        }
    }

    @Test
    void reportReviewChecklistIncludesSeverityAllowlistAndFalsePositiveReviews() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Severity Review Checklist",
                "`INFO` means review suggested, likely safe.",
                "`WARN` means naming may imply unsafe authority or overclaim.",
                "`BLOCKER_CANDIDATE` means human review required before any blocking enforcement.",
                "`BLOCKER_CANDIDATE` is not proof of unsafe runtime behavior.",
                "`BLOCKER_CANDIDATE` is not an automatic build failure in report-only mode.",
                "Allowlist/Suppression Review Checklist",
                "Known safe names have reviewed allowlist rationale.",
                "Suppressions require reviewed documentation.",
                "Suppressions do not encourage inline ignore spam.",
                "False-Positive Review Checklist",
                "intentionally documented risky examples that must not be treated as implemented classes",
                "negative boundary text that must not be treated as an implementation claim",
                "False positives are treated as guard-design feedback, not evidence of unsafe runtime behavior.")) {
            assertTrue(doc.contains(expected), "report review checklist should include severity/allowlist/false-positive content " + expected);
        }
    }

    @Test
    void reportReviewChecklistIncludesFalseNegativePrivacyAndDeterminismReviews() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "False-Negative Review Checklist",
                "safe names can still hide unsafe behavior",
                "naming checks do not prove package boundaries",
                "naming checks do not prove dependency direction",
                "naming checks do not prove routing, scoring, strategy, proxy, or production behavior safety",
                "Privacy And Secret-Safety Review Checklist",
                "The report does not include secrets.",
                "The report does not include environment variable values.",
                "The report does not include absolute local machine paths.",
                "The report does not include tokens, credentials, or private network details.",
                "The report does not upload artifacts unless separately approved.",
                "The report does not post PR comments unless separately approved.",
                "Determinism Review Checklist",
                "Field ordering is stable.",
                "Finding ordering is stable and reviewer-explainable.",
                "Output does not depend on timestamps, clocks, time zones, environment variables, system properties, random values, UUIDs, hashes, or network state.",
                "`generatedAtMode` describes timing posture",
                "`findingIdMode` describes deterministic identity posture")) {
            assertTrue(doc.contains(expected), "report review checklist should include safety/determinism content " + expected);
        }
    }

    @Test
    void reportReviewChecklistIncludesDecisionOutcomesAndApprovalGatesBeforeEnforcement() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Reviewer Decision Outcomes",
                "accept as safe",
                "request rename in a later scoped PR",
                "request allowlist entry with rationale",
                "request suppression review",
                "request guard rule change before implementation",
                "reject guard proposal as too broad/noisy",
                "defer until package-boundary enforcement exists",
                "Approval Gates Before Any Enforcement",
                "Report reviewed by maintainer.",
                "False-positive rate reviewed.",
                "False-negative risk reviewed.",
                "Allowlist reviewed.",
                "Suppression process reviewed.",
                "Rollback plan reviewed.",
                "CI impact reviewed.",
                "No combination with package moves.",
                "No combination with runtime behavior changes.",
                "No production-readiness or certification claim.")) {
            assertTrue(doc.contains(expected), "report review checklist should include decision/gate content " + expected);
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
                "This report review checklist does not claim report generation exists.",
                "This report review checklist does not claim source-name guard enforcement is active.",
                "This report review checklist does not claim a runtime-enforced LASE boundary.",
                "This report review checklist does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "report review checklist should keep explicit boundary " + expected);
        }

        for (String forbidden : List.of(
                "report generation is now active",
                "json output is generated by this sprint",
                "source-name guard enforcement is now active",
                "runtime naming guard is now active",
                "source scanning is now active",
                "source-name guard is now implemented",
                "this report review checklist enforces source names",
                "this report review checklist enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "report review checklist must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkReportReviewChecklistAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md"),
                    path + " should link the report review checklist");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only checklist"));
        assertTrue(trustMap.contains("docs/test-only report review checklist"));
        assertTrue(audit.contains("docs/test-only report review checklist"));
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
