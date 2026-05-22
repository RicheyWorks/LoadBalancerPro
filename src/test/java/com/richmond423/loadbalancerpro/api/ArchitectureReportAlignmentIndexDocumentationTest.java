package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class ArchitectureReportAlignmentIndexDocumentationTest {
    private static final Path DOC = Path.of("docs/ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path THREE_TIER = Path.of("docs/THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md");
    private static final Path LASE_BOUNDARY = Path.of("docs/LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md");
    private static final Path EXTERNAL_SIGNAL = Path.of("docs/EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md");
    private static final Path WORKLOAD_PROFILE =
            Path.of("docs/WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void architectureAlignmentIndexDocExistsAndStatesNoImplementation() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# Architecture Report Alignment Index",
                "alignment index only, not implementation",
                "This is docs/test only.",
                "Future phases are not implemented merely because they are documented.",
                "Future implementation would require separate scoped PR review.",
                "No production readiness claim is made.",
                "No production certification claim is made.",
                "No live-cloud validation claim is made.",
                "No real-tenant validation claim is made.",
                "No runtime-enforced LASE boundary is active.",
                "No package-boundary enforcement is active.",
                "No ExternalSignalPort implementation exists.",
                "No WorkloadProfile implementation exists.",
                "no runtime architecture changes",
                "no package moves or refactors",
                "no source scanning logic",
                "no ArchUnit or package-boundary tooling",
                "no Maven build changes",
                "no API behavior changes")) {
            assertTrue(doc.contains(expected), "alignment index should state boundary " + expected);
        }
    }

    @Test
    void architectureAlignmentIndexReferencesReportAsGuidanceAndIncludesPhaseMap() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Adaptive Routing Experimentation & Evidence Platform",
                "Engineering Architecture Report, Iterated v4",
                "architecture guidance",
                "Architecture Report Phase Map",
                "Phase 0 Alignment: Discovery And North-Star Definition",
                "Phase 1 Alignment: Domain Model And Core Abstractions",
                "Phase 2 Alignment: Strategy Engine And Adaptive Framework",
                "Phase 3 Alignment: Health, Resilience, And Adaptive Signals",
                "Phase 4 Alignment: LASE Shadow Evaluation And Policy Gates",
                "Phase 5 Alignment: Workload Modeling And Scenario Generation",
                "Phase 6 Alignment: Evidence, Audit Trail, And Explainability",
                "Phase 7 Alignment: Observability And Experimentation Cockpit",
                "Phase 8 Alignment: Configuration And Safe Control Plane",
                "Phase 9 Alignment: Security And Guardrail Hardening",
                "Phase 10 Alignment: Testing, Chaos, Validation, Feedback",
                "Phase 11 Alignment: Documentation, Golden Paths, Adoption",
                "Phase 12 Alignment: Future Research And Evolution",
                "Current implementation status",
                "recommended next safe sprint type",
                "explicit not-proven boundary")) {
            assertTrue(doc.contains(expected), "alignment index should include report/phase item " + expected);
        }
    }

    @Test
    void architectureAlignmentIndexCoversLaseWorkloadEvidenceAndGuardrailAlignment() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "LASE alignment",
                "WorkloadProfile alignment",
                "Evidence alignment",
                "safety and guardrails alignment",
                "LASE boundary not runtime-enforced",
                "LASE package boundary not enforced",
                "WorkloadProfile implementation and ScenarioGenerator implementation are not added",
                "EvidencePacket implementation not added",
                "source-name guard not implemented",
                "source-name guard allowlist implementation exists",
                "no source-name guard implementation",
                "no allowlist files or source scanning",
                "no runtime naming enforcement",
                "no package-boundary enforcement")) {
            assertTrue(doc.contains(expected), "alignment index should include cross-cutting item " + expected);
        }
    }

    @Test
    void architectureAlignmentIndexLinksPrimaryReviewerAndArchitectureDocs() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "../README.md",
                "REVIEWER_TRUST_MAP.md",
                "ENTERPRISE_READINESS_AUDIT.md",
                "THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md",
                "LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md",
                "EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md",
                "WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md",
                "LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md",
                "LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md",
                "LASE_BOUNDARY_NAMING_GUARD_PLAN.md",
                "LASE_NAMING_GUARD_INVENTORY.md",
                "LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md",
                "SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md",
                "SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md",
                "SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md",
                "SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md")) {
            assertTrue(doc.contains(expected), "alignment index should link " + expected);
        }

        for (Path path : List.of(README, TRUST_MAP, AUDIT, THREE_TIER, LASE_BOUNDARY, EXTERNAL_SIGNAL,
                WORKLOAD_PROFILE)) {
            assertTrue(Files.exists(path), path + " should exist");
        }
    }

    @Test
    void architectureAlignmentIndexIncludesCoverageTableBoundariesAndRecommendedSlices() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Current Documentation Coverage Table",
                "Architecture report area",
                "Current repo document",
                "Coverage type",
                "Implementation status",
                "Safe next action",
                "Explicit boundary",
                "already documented",
                "partially documented",
                "planned only",
                "future implementation candidate",
                "not in scope",
                "current repo behavior",
                "documentation only",
                "planning only",
                "future-only",
                "not implemented",
                "Future-Only Implementation Boundaries",
                "Recommended Next Architecture Slices",
                "Phase 0 architecture ADR index",
                "Phase 1 domain model package plan",
                "Phase 4 LASE observation port readiness plan",
                "Phase 5 workload profile implementation readiness plan",
                "Phase 6 evidence packet schema readiness plan",
                "Phase 10 deterministic validation roadmap",
                "Phase 11 golden path documentation plan",
                "These are recommendations only. Do not implement these slices in this sprint.")) {
            assertTrue(doc.contains(expected), "alignment index should include coverage/slice item " + expected);
        }
    }

    @Test
    void architectureAlignmentIndexKeepsFuturePhasesNonProving() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "aligned with the architecture report does not mean implemented",
                "documents a future target does not mean production-ready",
                "planning-only does not mean source scanning, package-boundary enforcement, or runtime enforcement",
                "not currently implemented means future implementation would require a separate scoped PR",
                "Architecture report alignment index is not implementation.",
                "Production readiness, production certification, live-cloud validation, real-tenant validation",
                "ExternalSignalPort implementation, WorkloadProfile implementation, EvidencePacket implementation",
                "remain not proven")) {
            assertTrue(doc.contains(expected), "alignment index should keep non-proving wording " + expected);
        }
    }

    @Test
    void reviewerEntryPointsLinkAlignmentIndexAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, TRUST_MAP, AUDIT)) {
            assertTrue(read(path).contains("ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md"),
                    path + " should link the architecture report alignment index");
        }

        assertTrue(read(README).contains("docs/test-only reviewer guidance"));
        assertTrue(read(TRUST_MAP).contains("docs/test-only architecture alignment index"));
        assertTrue(read(AUDIT).contains("docs/test-only architecture report alignment index"));
    }

    @Test
    void architectureAlignmentDocsAvoidImplementationAndProductionOverclaims() throws Exception {
        for (Path path : List.of(DOC, README, TRUST_MAP, AUDIT)) {
            String normalized = read(path).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "runtime-enforced lase boundary is now active",
                    "runtime-enforced lase boundary is implemented",
                    "package-boundary enforcement is now active",
                    "externalsignalport implementation now exists",
                    "externalsignalport implementation is active",
                    "workloadprofile implementation now exists",
                    "workloadprofile implementation is active",
                    "evidencepacket implementation now exists",
                    "scenariogenerator implementation now exists",
                    "architecture report alignment index implements",
                    "architecture report alignment index enforces",
                    "production readiness is proven",
                    "production certification is proven",
                    "live-cloud validation is complete",
                    "real-tenant validation is complete")) {
                assertFalse(normalized.contains(forbidden), path + " must not overclaim " + forbidden);
            }
        }
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
