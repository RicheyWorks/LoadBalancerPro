package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class SourceNameGuardReportSamplePlanDocumentationTest {
    private static final Path DOC = Path.of("docs/SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md");
    private static final Path REPORT_SCHEMA_PLAN = Path.of("docs/SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md");
    private static final Path REPORT_REVIEW_CHECKLIST = Path.of("docs/SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md");
    private static final Path DRY_RUN_PLAN = Path.of("docs/SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void reportSamplePlanDocExistsAndStatesStaticExamplesOnly() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Source-Name Guard Report Sample Plan",
                "sample plan only, no report generation",
                "docs/test only",
                "examples are static documentation examples",
                "examples are not generated output",
                "The examples in this document are static documentation examples, not generated output.",
                "No source scanning is added in this sprint.",
                "No JSON output files are generated.",
                "No CI workflow change is added.",
                "No PR comment or artifact behavior is added.",
                "No runtime naming guard is active.",
                "No source-name guard is implemented.",
                "No dry-run report generation is added.",
                "No report file is written.",
                "No dry-run command is added.",
                "source-name guard report sample is not generated output",
                "no source scanning",
                "no JSON output files",
                "no report generation",
                "no runtime naming guard is active")) {
            assertTrue(doc.contains(expected), "report sample plan should state " + expected);
        }
    }

    @Test
    void reportSamplePlanLinksSchemaReviewChecklistAndDryRunPlan() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md",
                "SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md",
                "SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md",
                "Relationship To Source-Name Guard Report Schema Plan",
                "Relationship To Source-Name Guard Report Review Checklist")) {
            assertTrue(doc.contains(expected), "report sample plan should link adjacent document " + expected);
        }

        for (Path path : List.of(REPORT_SCHEMA_PLAN, REPORT_REVIEW_CHECKLIST, DRY_RUN_PLAN)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md"),
                    path + " should link the report sample plan");
        }
    }

    @Test
    void reportSamplePlanIncludesCleanInfoWarnAndBlockerCandidateExamples() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Sample Clean Report",
                "schemaVersion: example-v1",
                "sourceNameGuardMode: report-only example",
                "scannedScopeSummary: example scope only",
                "findings: none",
                "totalFindings: 0",
                "no action required except normal review",
                "clean sample is not proof of production safety",
                "Sample INFO Finding Report",
                "severity: INFO",
                "category: naming-clarity",
                "name: ExampleReviewerSummary",
                "review suggested but likely safe",
                "confirm naming remains reviewer metadata only",
                "finding is a review trigger only",
                "Sample WARN Finding Report",
                "severity: WARN",
                "category: possible-overclaim",
                "name: ExampleReplayValidator",
                "name could imply stronger proof than intended",
                "consider rename or documentation clarification",
                "not proof of unsafe runtime behavior",
                "Sample BLOCKER_CANDIDATE Finding Report",
                "severity: BLOCKER_CANDIDATE",
                "category: production-authority-overclaim",
                "name: ExampleProductionCertifiedRouter",
                "name appears to imply production certification or production routing authority",
                "human review required before any future enforcement",
                "still not automatic build failure in report-only mode and not proof of unsafe runtime behavior")) {
            assertTrue(doc.contains(expected), "report sample plan should include sample content " + expected);
        }
    }

    @Test
    void reportSamplePlanIncludesAllowlistedFindingFalsePositiveNoteAndReviewerOutcomes() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Sample Allowlisted Finding Report",
                "allowlistStatus: reviewed-example-only",
                "suppressionReviewRequired: true",
                "confirm allowlist rationale remains valid",
                "allowlist does not certify production safety",
                "Sample False-Positive Review Note",
                "falsePositiveNotes",
                "documented risky example used to explain names to avoid",
                "downgrade or dismiss as documentation-only example",
                "keep example clearly marked as not an implemented class",
                "false-positive disposition is not proof of runtime safety",
                "Sample Reviewer Decision Outcomes",
                "accept as safe for report-only context",
                "request rename in a later scoped PR",
                "request allowlist entry with rationale",
                "request suppression review",
                "request guard rule change before implementation",
                "reject guard proposal as too broad or noisy",
                "defer until package-boundary enforcement exists")) {
            assertTrue(doc.contains(expected), "report sample plan should include review example content " + expected);
        }
    }

    @Test
    void reportSamplePlanKeepsSeverityAndCleanSamplesNonProving() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "`BLOCKER_CANDIDATE` is not automatic build failure in report-only mode",
                "`BLOCKER_CANDIDATE` is not proof of unsafe runtime behavior",
                "the example name is a name to avoid, not an implemented class",
                "a clean report is not proof of production safety",
                "clean report is not production readiness, production certification, source-name guard enforcement, package-boundary enforcement, or runtime LASE boundary enforcement",
                "Sample findings are review triggers, not proof of unsafe runtime behavior.",
                "These examples are not machine-generated. They are documentation-only mock examples.")) {
            assertTrue(doc.contains(expected), "report sample plan should keep sample non-proving " + expected);
        }
    }

    @Test
    void reportSamplePlanIncludesPrivacyAndSecretSafetyConstraints() throws Exception {
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
                "do not invent reviewer names, initials, personal data, timestamps, UUIDs, hashes, or absolute local paths")) {
            assertTrue(doc.contains(expected), "report sample plan should include privacy/secret-safety constraint " + expected);
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
                "This sample plan does not claim report generation exists.",
                "This sample plan does not claim JSON output exists.",
                "This sample plan does not claim source-name guard enforcement is active.",
                "This sample plan does not claim a runtime-enforced LASE boundary.",
                "This sample plan does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "report sample plan should keep explicit boundary " + expected);
        }

        for (String forbidden : List.of(
                "report generation is now active",
                "json output is generated by this sprint",
                "source-name guard enforcement is now active",
                "runtime naming guard is now active",
                "source scanning is now active",
                "source-name guard is now implemented",
                "this report sample enforces source names",
                "this report sample enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "report sample plan must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkReportSamplePlanAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md"),
                    path + " should link the report sample plan");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only static examples"));
        assertTrue(trustMap.contains("docs/test-only report sample plan"));
        assertTrue(audit.contains("docs/test-only report sample plan"));
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
