package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class SourceNameGuardRuleCatalogPlanDocumentationTest {
    private static final Path DOC = Path.of("docs/SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md");
    private static final Path ACCEPTANCE_CRITERIA_PLAN =
            Path.of("docs/SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md");
    private static final Path REPORT_SCHEMA_PLAN = Path.of("docs/SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md");
    private static final Path REPORT_REVIEW_CHECKLIST =
            Path.of("docs/SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md");
    private static final Path DRY_RUN_PLAN = Path.of("docs/SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void ruleCatalogPlanDocExistsAndStatesNoImplementation() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Source-Name Guard Rule Catalog Plan",
                "rule catalog only, no implementation",
                "rule catalog only",
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
                "source-name guard rule catalog is not implementation",
                "no source scanning",
                "no JSON output",
                "no report generation",
                "no runtime naming guard is active")) {
            assertTrue(doc.contains(expected), "rule catalog plan should state " + expected);
        }
    }

    @Test
    void ruleCatalogPlanLinksAcceptanceCriteriaSchemaReviewAndDryRunPlans() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md",
                "SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md",
                "SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md",
                "SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md",
                "Relationship To Source-Name Guard Report Acceptance Criteria Plan",
                "Relationship To Source-Name Guard Report Schema Plan")) {
            assertTrue(doc.contains(expected), "rule catalog plan should link adjacent document " + expected);
        }

        for (Path path : List.of(ACCEPTANCE_CRITERIA_PLAN, REPORT_SCHEMA_PLAN, REPORT_REVIEW_CHECKLIST, DRY_RUN_PLAN)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md"),
                    path + " should link the rule catalog plan");
        }
    }

    @Test
    void ruleCatalogPlanIncludesCandidateCategoriesAndExamples() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Candidate Future Rule Categories",
                "`production-certification-overclaim`",
                "`replay-proof-overclaim`",
                "`scoring-proof-overclaim`",
                "`live-grid-control-claim`",
                "`facility-automation-claim`",
                "`gpu-orchestration-claim`",
                "`carbon-aware-production-routing-claim`",
                "`hidden-production-routing-authority`",
                "`reviewer-metadata-ambiguity`",
                "`shadow-vs-live-allocation-ambiguity`",
                "Production Certification Overclaim Rule Category",
                "Replay/Scoring Proof Overclaim Rule Category",
                "Live-Control Authority Rule Category",
                "GPU/Grid/Facility/Control Rule Category",
                "Carbon-Aware Production Routing Rule Category",
                "Reviewer Metadata Ambiguity Rule Category",
                "CertifiedRouter",
                "ProductionCertifiedBalancer",
                "ReplayProofValidator",
                "ScoringProofEngine",
                "LiveGridController",
                "FacilityAutomationManager",
                "GpuOrchestrator",
                "CarbonAwareProductionRouter",
                "AutonomousProductionRouter",
                "HiddenProductionRouter",
                "ReviewerProofEngine",
                "LaseShadow",
                "LaseEvaluation",
                "LaseEvidence",
                "ReviewerMetadata",
                "ReviewerSummary",
                "BoundaryInventory",
                "BoundaryPlan",
                "ExternalSignalSnapshot",
                "WorkloadProfileSignalMetadata",
                "ReportOnlyFinding")) {
            assertTrue(doc.contains(expected), "rule catalog plan should include category/example " + expected);
        }
    }

    @Test
    void ruleCatalogPlanIncludesAllowlistFalsePositiveSeverityAndReviewerActions() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Future Allowlist Strategy By Rule Category",
                "allowlist entries must have a rationale",
                "allowlist entries must identify the rule category",
                "allowlist entries must be reviewed",
                "allowlist entries must not certify production safety",
                "allowlist entries must be easy to audit",
                "suppressions should not be inline ignore spam",
                "allowlist does not replace package-boundary enforcement",
                "Future False-Positive Risk By Rule Category",
                "False positives should improve future guard design.",
                "Future Severity Guidance By Rule Category",
                "`INFO`: name should be reviewed for clarity but is likely safe",
                "`WARN`: name may imply unsafe authority, runtime control, proof, or overclaim",
                "`BLOCKER_CANDIDATE`: name appears to imply production certification, production routing authority, live control, proof, or facility/grid/GPU control and requires human review before any future blocking enforcement",
                "`BLOCKER_CANDIDATE` is not automatic build failure in report-only mode",
                "`BLOCKER_CANDIDATE` is not proof of unsafe runtime behavior",
                "Future Reviewer Actions By Rule Category",
                "request allowlist entry with rationale and rule category",
                "reject guard proposal as too noisy",
                "defer until package-boundary enforcement exists")) {
            assertTrue(doc.contains(expected), "rule catalog plan should include review strategy " + expected);
        }
    }

    @Test
    void ruleCatalogPlanIncludesImplementationGates() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Future Implementation Gates",
                "Rule catalog reviewed.",
                "Report schema reviewed.",
                "Sample report reviewed.",
                "Report review checklist reviewed.",
                "Acceptance criteria reviewed.",
                "False-positive risk reviewed.",
                "False-negative risk reviewed.",
                "Allowlist strategy reviewed.",
                "Deterministic output reviewed.",
                "Privacy/secret-safety reviewed.",
                "Source scanning scope reviewed separately.",
                "JSON output reviewed separately.",
                "CI report-only behavior reviewed separately.",
                "PR comment/report artifact behavior reviewed separately.",
                "Enforcement remains future-only unless separately approved.")) {
            assertTrue(doc.contains(expected), "rule catalog plan should include implementation gate " + expected);
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
                "This rule catalog plan does not claim source-name guard rule implementation exists.",
                "This rule catalog plan does not claim report generation exists.",
                "This rule catalog plan does not claim source-name guard enforcement is active.",
                "This rule catalog plan does not claim a runtime-enforced LASE boundary.",
                "This rule catalog plan does not claim package-boundary enforcement is active.")) {
            assertTrue(doc.contains(expected), "rule catalog plan should keep explicit boundary " + expected);
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
                "this rule catalog plan enforces source names",
                "this rule catalog plan enforces package boundaries",
                "package-boundary enforcement is now active",
                "runtime lase boundary is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "rule catalog plan must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkRuleCatalogPlanAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT)) {
            assertTrue(read(path).contains("SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md"),
                    path + " should link the rule catalog plan");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String audit = read(ENTERPRISE_AUDIT);

        assertTrue(readme.contains("docs/test-only candidate future rule categories"));
        assertTrue(trustMap.contains("Docs/test-only rule catalog plan"));
        assertTrue(audit.contains("docs/test-only rule catalog plan"));
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
