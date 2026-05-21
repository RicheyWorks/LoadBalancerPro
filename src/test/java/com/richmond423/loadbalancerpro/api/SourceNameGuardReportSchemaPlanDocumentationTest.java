package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class SourceNameGuardReportSchemaPlanDocumentationTest {
    private static final Path DOC = Path.of("docs/SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md");
    private static final Path DRY_RUN_PLAN = Path.of("docs/SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md");
    private static final Path REVIEW_CHECKLIST = Path.of("docs/SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md");
    private static final Path FEASIBILITY_PLAN = Path.of("docs/LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void reportSchemaPlanDocExistsAndStatesNoGenerationOrSourceScanning() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Source-Name Guard Report Schema Plan",
                "schema plan only, no report generation",
                "docs/test only",
                "No source scanning is added in this sprint.",
                "No JSON output is generated.",
                "No CI workflow change is added.",
                "No PR comment or artifact behavior is added.",
                "No runtime naming guard is active.",
                "No source-name guard is implemented.",
                "No dry-run report generation is added.",
                "No report file is written.",
                "source-name guard report schema not implemented yet",
                "no source scanning",
                "no JSON output",
                "no report generation",
                "no runtime naming guard is active")) {
            assertTrue(doc.contains(expected), "report schema plan should state " + expected);
        }
    }

    @Test
    void reportSchemaPlanLinksDryRunDesignChecklistAndFeasibilityPlan() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md",
                "SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md",
                "LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md",
                "Relationship To Source-Name Guard Dry-Run Design Plan",
                "Relationship To Source-Name Guard Review Checklist")) {
            assertTrue(doc.contains(expected), "report schema plan should link adjacent document " + expected);
        }

        for (Path path : List.of(DRY_RUN_PLAN, REVIEW_CHECKLIST, FEASIBILITY_PLAN)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md"),
                    path + " should link the report schema plan");
        }
    }

    @Test
    void reportSchemaPlanIncludesTopLevelFindingAndSummaryFields() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Proposed Future Report Top-Level Fields",
                "`schemaVersion`",
                "`generatedAtMode`",
                "`repositoryContext`",
                "`scannedScopeSummary`",
                "`sourceNameGuardMode`",
                "`findings`",
                "`summary`",
                "`notProvenBoundaries`",
                "`reviewerGuidance`",
                "Use `generatedAtMode` instead of a real timestamp concept",
                "Proposed Future Finding Fields",
                "`findingIdMode`",
                "`severity`",
                "`category`",
                "`name`",
                "`path`",
                "`reason`",
                "`reviewerAction`",
                "`allowlistStatus`",
                "`falsePositiveNotes`",
                "`notProofStatement`",
                "Use `findingIdMode` instead of generated UUIDs, hashes, random values, or time-derived IDs",
                "Proposed Future Summary Fields",
                "`totalNamesConsidered`",
                "`totalFindings`",
                "`infoCount`",
                "`warnCount`",
                "`blockerCandidateCount`",
                "`allowlistedCount`",
                "`suppressedCount`",
                "`reviewedScopeDescription`",
                "`reportOnlyMode`")) {
            assertTrue(doc.contains(expected), "report schema plan should include field " + expected);
        }
    }

    @Test
    void reportSchemaPlanIncludesSeverityGuidanceNotProvenBoundariesAndDeterminism() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Proposed Future Severity Model",
                "`INFO`",
                "Review suggested, likely safe.",
                "`WARN`",
                "Naming may imply unsafe authority or overclaim.",
                "`BLOCKER_CANDIDATE`",
                "Naming appears to imply production certification/control/proof and requires human review before any blocking enforcement.",
                "`BLOCKER_CANDIDATE` is not an automatic build failure in dry-run mode and is not proof of unsafe runtime behavior.",
                "Proposed Future Reviewer Guidance Fields",
                "`nextReviewStep`",
                "`suggestedOwner`",
                "`documentationReferences`",
                "`suppressionReviewRequired`",
                "`implementationGateRequired`",
                "Proposed Future Not-Proven Boundary Fields",
                "not production-ready",
                "not production-certified",
                "not live-cloud validated",
                "not real-tenant validated",
                "not GPU orchestration",
                "not power/grid control",
                "not carbon-aware routing implementation",
                "not facility automation",
                "Source-name guard report schema not implemented yet",
                "Proposed Future Deterministic Output Rules",
                "output should not depend on timestamps, clocks, time zones, environment variables, system properties, random values, UUIDs, hashes, or network state",
                "repeated runs on the same reviewed tree and same approved scope should produce equivalent report content")) {
            assertTrue(doc.contains(expected), "report schema plan should include severity/guidance/boundary " + expected);
        }
    }

    @Test
    void reportSchemaPlanIncludesPrivacySecretSafetyAndVersioningRules() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Proposed Future Privacy And Secret-Safety Rules",
                "report must not include secrets",
                "report must not include environment variable values",
                "report must not include absolute local machine paths",
                "report must not include tokens, credentials, or private network details",
                "report must not upload artifacts unless separately approved",
                "report must not post PR comments unless separately approved",
                "report must keep all paths repository-relative if future scanning is separately approved",
                "Proposed Future Versioning Strategy",
                "`schemaVersion` such as `source-name-guard-report-schema/v1`",
                "require reviewer approval before adding, removing, or reinterpreting fields",
                "never use schema versioning to claim runtime enforcement, package-boundary enforcement, or production readiness",
                "Future report findings are review triggers, not proof of unsafe runtime behavior.")) {
            assertTrue(doc.contains(expected), "report schema plan should include privacy/versioning content " + expected);
        }
    }

    @Test
    void explicitNonGoalsPreventReportGenerationEnforcementAndProductionOverclaims() throws Exception {
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
                "This report schema plan does not claim report generation exists.",
                "This report schema plan does not claim source-name guard enforcement is active.",
                "This report schema plan does not claim a runtime-enforced LASE boundary.",
                "This report schema plan does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "report schema plan should keep explicit boundary " + expected);
        }

        for (String forbidden : List.of(
                "report generation is now active",
                "json output is generated by this sprint",
                "source-name guard enforcement is now active",
                "runtime naming guard is now active",
                "source scanning is now active",
                "source-name guard is now implemented",
                "this report schema enforces source names",
                "this report schema enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "report schema plan must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkReportSchemaPlanAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md"),
                    path + " should link the report schema plan");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only planning"));
        assertTrue(trustMap.contains("docs/test-only report schema plan"));
        assertTrue(audit.contains("docs/test-only report schema plan"));
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
