package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class LaseBoundaryArchitectureContractDocumentationTest {
    private static final Path DOC = Path.of("docs/LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md");
    private static final Path EXTERNAL_SIGNAL = Path.of("docs/EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md");
    private static final Path WORKLOAD_PROFILE =
            Path.of("docs/WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md");
    private static final Path THREE_TIER = Path.of("docs/THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path POM = Path.of("pom.xml");
    private static final Path PRODUCTION_SOURCE = Path.of("src/main/java");

    @Test
    void boundaryContractExistsAndStatesNotImplementedStatus() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# LASE Boundary Architecture Contract",
                "boundary contract only",
                "not implemented in this sprint",
                "not an API contract",
                "not a Java runtime interface",
                "not a package refactor",
                "not an ArchUnit rule set",
                "not proof of production readiness",
                "documentation and documentation guard tests only",
                "no Java source files added for this concept",
                "no runtime boundary enforcement",
                "no package moves or refactors",
                "no ArchUnit dependency or rule set",
                "no runtime interface",
                "no ports or adapters",
                "no API fields",
                "no routing behavior change",
                "no scoring behavior change",
                "no strategy behavior change",
                "no proxy behavior change",
                "no production behavior change",
                "does not claim the LASE boundary is already enforced by package refactor or ArchUnit")) {
            assertTrue(doc.contains(expected), "boundary contract should mention " + expected);
        }
    }

    @Test
    void boundaryContractSeparatesLiveAllocationLaseEvidenceAndReviewerMetadataPaths() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Live Allocation Path",
                "LASE Shadow Evaluation Path",
                "Replay/Evidence Path",
                "Reviewer Metadata Path",
                "live allocation chooses through explicit allocation behavior",
                "LASE observes and explains unless a future policy gate explicitly authorizes a bounded mode",
                "live allocation may publish read-only observations in a future design",
                "live allocation must not depend on hidden LASE authority",
                "LASE may consume observations",
                "LASE may run shadow comparisons",
                "LASE may produce recommendations or findings for reviewers",
                "LASE must not directly select production routes",
                "LASE must not directly mutate allocation or proxy state",
                "reviewer metadata must not change route selection",
                "reviewer metadata must not persist hidden routing authority")) {
            assertTrue(doc.contains(expected), "boundary contract should distinguish " + expected);
        }
    }

    @Test
    void boundaryContractDefinesFutureLayeringAndObservationPseudocode() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Future Layered Architecture Concept",
                "Proposed Future Package Boundary Model",
                "`domain`",
                "`allocation`",
                "`lase`",
                "`infrastructure`",
                "`api`",
                "`config`",
                "This is a proposed future package boundary model, not a package move in this sprint.",
                "Proposed Future LaseObservationPort Concept",
                "Documentation-only pseudocode. Not implemented in this sprint.",
                "interface LaseObservationPort",
                "LaseObservationSnapshot currentObservationSnapshot()",
                "LaseObservationSource describeObservationSource()",
                "interface LaseShadowEvaluationBoundary",
                "LaseEvaluationResult evaluateShadowOnly(LaseObservationSnapshot snapshot)",
                "no allocation mutation methods",
                "no routing selection methods",
                "no proxy mutation methods",
                "no `CloudManager` calls",
                "no cloud/facility/grid/GPU control methods",
                "no production activation method")) {
            assertTrue(doc.contains(expected), "boundary contract should include " + expected);
        }
    }

    @Test
    void allowedAndForbiddenResponsibilitiesKeepLaseShadowOnlyUnlessGated() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Allowed LASE Responsibilities",
                "observe snapshots",
                "run shadow comparisons",
                "compare strategies offline or in shadow mode",
                "generate reviewer evidence",
                "generate explanation metadata",
                "support replay evidence in controlled reviewer contexts",
                "report limitations and not-proven boundaries",
                "degrade safely when required inputs are missing or stale",
                "Forbidden LASE Responsibilities",
                "directly mutate live routing state",
                "directly select production routes",
                "directly alter proxy behavior",
                "directly call external control systems",
                "directly call `CloudManager` or future cloud/facility/grid/GPU control paths",
                "write production configuration",
                "persist hidden routing authority",
                "bypass policy gates",
                "claim replay proof, scoring proof, correctness validation, production readiness, or production certification",
                "future approved sprint promotes any LASE output into a controlled decision path")) {
            assertTrue(doc.contains(expected), "boundary contract should state " + expected);
        }
    }

    @Test
    void determinismRelationshipsAndNonGoalsStayReviewerSafe() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Determinism And Evidence Requirements",
                "Observation snapshots must be immutable or treated as immutable by LASE.",
                "Missing data must be represented explicitly as `UNKNOWN`, `UNAVAILABLE`, or equivalent.",
                "Stale data must remain visible to reviewers.",
                "LASE findings must distinguish shadow comparison, reviewer metadata, and gated lab influence.",
                "Any future live influence must be separately gated, tested, documented, and opt-in.",
                "Relationship To ExternalSignalPort",
                "ExternalSignalPort signals must be read-only inputs",
                "must not grant LASE direct control over facility, grid, GPU, cloud, proxy, routing, or production systems",
                "Relationship To WorkloadProfile Signal Metadata",
                "WorkloadProfile metadata may help LASE generate deterministic shadow comparisons and evidence",
                "must not by itself change live routing behavior without explicit future policy gates",
                "Future Implementation Gates",
                "No future implementation should be merged only because this design contract exists.",
                "Reviewer-Facing Value")) {
            assertTrue(doc.contains(expected), "boundary contract should mention " + expected);
        }
    }

    @Test
    void explicitNonGoalsPreventRuntimeAndProductionOverclaims() throws Exception {
        String doc = read(DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no production Java runtime behavior",
                "no Java runtime interface",
                "no ports or adapters",
                "no package moves or refactors",
                "no ArchUnit dependency or build changes",
                "no runtime LASE boundary implementation",
                "no API fields",
                "no routing behavior change",
                "no scoring behavior change",
                "no strategy behavior change",
                "no proxy behavior change",
                "no runtime workload model implementation",
                "no runtime signal ingestion",
                "no external API clients",
                "no HTTP calls",
                "no secrets, tokens, environment variables, credentials, config, or properties",
                "no telemetry, storage, or persistence",
                "no MessageDigest, SHA, hash, UUID, random, time, environment, or system-property behavior",
                "no replay execution",
                "no what-if mutation",
                "no upload/share/download/export/PDF/ZIP behavior",
                "no Docker, CI, release, signing, registry, or governance changes",
                "no live-cloud validation claim",
                "no real-tenant validation claim",
                "no GPU orchestration claim",
                "no live power/grid control",
                "no facility mutation",
                "no carbon-aware routing implementation",
                "no production readiness claim",
                "no production certification claim")) {
            assertTrue(doc.contains(expected), "boundary contract should keep boundary " + expected);
        }

        for (String forbidden : List.of(
                "lase boundary is implemented",
                "runtime lase boundary is implemented",
                "package boundary is enforced",
                "archunit enforces",
                "lase is production routing authority",
                "lase selects production routes",
                "lase mutates live routing state",
                "lase alters proxy behavior",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "boundary contract must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsAndAdjacentContractsLinkLaseBoundaryAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, TRUST_MAP, AUDIT, THREE_TIER, EXTERNAL_SIGNAL, WORKLOAD_PROFILE)) {
            assertTrue(read(path).contains("LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md"),
                    path + " should link the LASE boundary architecture contract");
        }

        String readme = read(README);
        String trustMap = read(TRUST_MAP);
        String threeTier = read(THREE_TIER);
        String externalSignal = read(EXTERNAL_SIGNAL);
        String workloadProfile = read(WORKLOAD_PROFILE);

        assertTrue(readme.contains("without adding runtime enforcement or package refactors"));
        assertTrue(trustMap.contains("docs-only boundary contract for separating live allocation"));
        assertTrue(trustMap.contains("not runtime enforcement, a package refactor, ArchUnit enforcement"));
        assertTrue(threeTier.contains("That contract is documentation-only"));
        assertTrue(threeTier.contains("does not implement runtime boundary enforcement, package refactors"));
        assertTrue(externalSignal.contains("That contract is documentation-only"));
        assertTrue(externalSignal.contains("does not implement runtime boundary enforcement, package refactors"));
        assertTrue(workloadProfile.contains("That contract is documentation-only"));
        assertTrue(workloadProfile.contains("does not implement runtime boundary enforcement, package refactors"));
    }

    @Test
    void sprintDoesNotIntroduceRuntimeLaseBoundaryTypesOrArchUnitDependency() throws Exception {
        assertFalse(read(POM).toLowerCase(Locale.ROOT).contains("archunit"),
                "this sprint must not add an ArchUnit dependency or build change");

        List<String> forbiddenRuntimeTokens = List.of(
                "LaseObservationPort",
                "LaseObservationSnapshot",
                "LaseObservationSource",
                "LaseShadowEvaluationBoundary",
                "LaseEvaluationResult");

        try (Stream<Path> files = Files.walk(PRODUCTION_SOURCE)) {
            List<Path> javaFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path javaFile : javaFiles) {
                String source = read(javaFile);
                for (String token : forbiddenRuntimeTokens) {
                    assertFalse(source.contains(token),
                            "production source must not introduce runtime LASE boundary type " + token
                                    + " in " + javaFile);
                }
            }
        }
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
