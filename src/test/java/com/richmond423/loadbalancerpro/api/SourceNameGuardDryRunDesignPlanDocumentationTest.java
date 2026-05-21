package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class SourceNameGuardDryRunDesignPlanDocumentationTest {
    private static final Path DOC = Path.of("docs/SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md");
    private static final Path REVIEW_CHECKLIST = Path.of("docs/SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md");
    private static final Path FEASIBILITY_PLAN = Path.of("docs/LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md");
    private static final Path NAMING_INVENTORY = Path.of("docs/LASE_NAMING_GUARD_INVENTORY.md");
    private static final Path NAMING_PLAN = Path.of("docs/LASE_BOUNDARY_NAMING_GUARD_PLAN.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void dryRunDesignDocExistsAndStatesNoImplementation() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Source-Name Guard Dry-Run Design Plan",
                "dry-run design only, no implementation",
                "docs/test only",
                "No source scanning is added in this sprint.",
                "No CI workflow change is added.",
                "No report generation is added.",
                "No PR comment bot or artifact upload is added.",
                "No runtime naming guard is active.",
                "No source-name guard is implemented.",
                "No dry-run command is added.",
                "source-name guard not implemented yet",
                "source-name guard dry run not implemented yet",
                "no source scanning",
                "no CI workflow change is added",
                "no report generation is added",
                "no PR comment bot or artifact upload is added",
                "no runtime naming guard is active",
                "no source-name guard is implemented")) {
            assertTrue(doc.contains(expected), "dry-run design doc should state " + expected);
        }
    }

    @Test
    void dryRunDesignDocLinksChecklistAndFeasibilityPlan() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md",
                "LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md",
                "Relationship To Source-Name Guard Review Checklist",
                "Relationship To Source-Name Guard Feasibility Plan")) {
            assertTrue(doc.contains(expected), "dry-run design doc should link adjacent document " + expected);
        }

        for (Path path : List.of(REVIEW_CHECKLIST, FEASIBILITY_PLAN, NAMING_INVENTORY, NAMING_PLAN)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md"),
                    path + " should link the dry-run design plan");
        }
    }

    @Test
    void dryRunDesignDocIncludesFutureModesAndReportContents() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Proposed Future Dry-Run Modes",
                "Local docs-only dry run",
                "Local source-name dry run",
                "CI report-only dry run",
                "PR comment/report artifact dry run",
                "Enforcement mode",
                "Future-only and must require separate approval.",
                "These are future concepts only.",
                "This sprint does not add a dry-run command, CI workflow, artifact upload, report file generation, PR comment bot, or source scanning logic.",
                "Proposed Future Report Contents",
                "scanned scope summary",
                "files/classes considered",
                "allowlisted names",
                "flagged names",
                "severity",
                "reason",
                "recommended reviewer action",
                "false-positive notes",
                "suppression review notes",
                "not-proven boundaries",
                "statement that findings are review triggers, not runtime safety proof")) {
            assertTrue(doc.contains(expected), "dry-run design doc should include mode/report content " + expected);
        }
    }

    @Test
    void dryRunDesignDocIncludesReviewWorkflowSeverityAndCiGates() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Proposed Future Review Workflow",
                "Confirm dry-run/report-only output is non-blocking.",
                "Future dry-run findings are review triggers, not runtime safety proof.",
                "Proposed Future Severity Levels",
                "`INFO`",
                "Naming should be reviewed but likely safe.",
                "`WARN`",
                "Name may imply unsafe authority or overclaim.",
                "`BLOCKER-CANDIDATE`",
                "Name appears to imply production certification/control/proof, but still requires human review before blocking.",
                "A future dry-run must not automatically prove unsafe behavior. It only identifies review candidates.",
                "Future CI Integration Gates",
                "no CI integration in this sprint",
                "future CI report-only mode must be separate from required enforcement",
                "future CI report-only output must not fail builds unless separately approved",
                "future enforcement mode must be a separate sprint",
                "future PR comments or artifacts must not include secrets",
                "future output must avoid noisy broad scans",
                "future output must be deterministic")) {
            assertTrue(doc.contains(expected), "dry-run design doc should include workflow/severity/CI gate " + expected);
        }
    }

    @Test
    void dryRunDesignDocIncludesAllowlistSuppressionFalsePositiveFalseNegativeAndRollback() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Proposed Future Allowlist/Suppression Workflow",
                "require a reason for each allowlisted name",
                "require a reason for each suppression",
                "False-Positive Handling",
                "no runtime behavior change just to silence a naming false positive",
                "False positives should improve guard design, not create churn.",
                "False-Negative Handling",
                "safe names can still hide unsafe behavior",
                "source-name dry runs do not prove package boundaries",
                "source-name dry runs do not prove dependency direction",
                "source-name dry runs do not prove routing, scoring, strategy, proxy, or production behavior safety",
                "Rollback And Disable Strategy",
                "disable report-only output without changing runtime behavior",
                "remove report generation without changing routing, scoring, strategy, proxy, API, config, Docker, release, registry, governance, or production behavior",
                "This sprint does not implement disable flags, environment variables, system properties, config properties, CI switches, or report files.")) {
            assertTrue(doc.contains(expected), "dry-run design doc should include handling/rollback content " + expected);
        }
    }

    @Test
    void explicitNonGoalsPreventDryRunImplementationAndProductionOverclaims() throws Exception {
        String doc = read(DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no production Java runtime behavior",
                "no records/classes/interfaces under `src/main/java`",
                "no class renames",
                "no package moves or refactors",
                "no source scanning logic in this sprint",
                "no dry-run command",
                "no report generation",
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
                "This dry-run design plan does not claim dry-run implementation is active.",
                "This dry-run design plan does not claim source-name guard enforcement is active.",
                "This dry-run design plan does not claim a runtime-enforced LASE boundary.",
                "This dry-run design plan does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "dry-run design doc should keep explicit boundary " + expected);
        }

        for (String forbidden : List.of(
                "dry-run implementation is now active",
                "the dry-run implementation is active",
                "source-name guard enforcement is now active",
                "runtime naming guard is now active",
                "source scanning is now active",
                "source-name guard is now implemented",
                "this dry-run design enforces source names",
                "this dry-run design enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "dry-run design doc must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkDryRunDesignAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md"),
                    path + " should link the dry-run design plan");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only planning"));
        assertTrue(trustMap.contains("Docs/test-only dry-run design plan"));
        assertTrue(audit.contains("docs/test-only dry-run design plan"));
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
