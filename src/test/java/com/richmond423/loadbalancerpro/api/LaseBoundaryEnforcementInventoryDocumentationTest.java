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

class LaseBoundaryEnforcementInventoryDocumentationTest {
    private static final Path DOC = Path.of("docs/LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md");
    private static final Path LASE_BOUNDARY = Path.of("docs/LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ENTERPRISE_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path THREE_TIER = Path.of("docs/THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md");
    private static final Path README = Path.of("README.md");
    private static final Path POM = Path.of("pom.xml");
    private static final Path PRODUCTION_SOURCE = Path.of("src/main/java");

    @Test
    void inventoryDocExistsAndStatesPreparationOnly() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# LASE Boundary Enforcement Inventory",
                "inventory only",
                "docs/test only",
                "no runtime enforcement",
                "no package move",
                "no Java runtime interface",
                "no ArchUnit rule set",
                "no proof of production readiness",
                "No classes were moved.",
                "No packages were refactored.",
                "No runtime interfaces were added.",
                "No ArchUnit dependency or enforcement rule was added.",
                "This inventory does not claim the LASE boundary is runtime-enforced.")) {
            assertTrue(doc.contains(expected), "inventory doc should mention " + expected);
        }
    }

    @Test
    void inventoryDocCoversRequiredBoundaryCategories() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "live allocation / routing path",
                "LASE shadow/evaluation path",
                "replay / evidence path",
                "reviewer metadata path",
                "domain model candidates",
                "infrastructure / cloud / future integration boundary",
                "API/view layer",
                "configuration layer",
                "Live Allocation Path Candidates",
                "LASE Shadow/Evaluation Path Candidates",
                "Replay/Evidence Path Candidates",
                "Reviewer Metadata Path Candidates",
                "Domain Model Candidates",
                "Infrastructure / Cloud / External Integration Candidates",
                "API And Static UI Surface Candidates",
                "Configuration Layer Candidates")) {
            assertTrue(doc.contains(expected), "inventory doc should cover category " + expected);
        }
    }

    @Test
    void inventoryDocClassifiesCurrentClassesAndPackages() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "com.richmond423.loadbalancerpro.core",
                "com.richmond423.loadbalancerpro.api",
                "com.richmond423.loadbalancerpro.api.config",
                "There is no standalone `com.richmond423.loadbalancerpro.config` package",
                "`LoadBalancer`",
                "`AllocatorService`",
                "`Server`",
                "`ServerStateVector`",
                "`ServerScoreCalculator`",
                "`AdaptiveRoutingPolicyEngine`",
                "`LaseShadowAdvisor`",
                "`LaseEvaluationEngine`",
                "`LaseShadowReplayEngine`",
                "`AdaptiveRoutingScenario`",
                "`AdaptiveRoutingScenarioEvidencePacket`",
                "`RoutingComparisonResponse`",
                "`RoutingComparisonResultResponse`",
                "`RoutingDecisionReplayEvidence*`",
                "`CloudManager`",
                "`CloudAwsClients`",
                "`CloudConfig`",
                "`routing-demo.html`",
                "`load-balancing-cockpit.html`")) {
            assertTrue(doc.contains(expected), "inventory doc should classify " + expected);
        }
    }

    @Test
    void inventoryDocRecordsRiskAndStatusForCategories() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Likely future package/category",
                "Migration risk",
                "Runtime/evidence/API status",
                "Safety notes",
                "Runtime behavior.",
                "API/view-only metadata.",
                "Static API/view-only surface.",
                "Runtime infrastructure behavior.",
                "Runtime config.",
                "Evidence packets must stay reviewer metadata unless future gates approve more.",
                "LASE must not directly call `CloudManager` or future cloud/facility/grid/GPU control paths.",
                "LASE must not directly alter proxy behavior.")) {
            assertTrue(doc.contains(expected), "inventory doc should include risk/status detail " + expected);
        }
    }

    @Test
    void inventoryDocIncludesSafeFutureMigrationSequenceAndFutureOnlyEnforcement() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Safe Future Migration Sequence",
                "1. Inventory current classes and responsibilities.",
                "2. Add package-boundary documentation.",
                "3. Add non-invasive tests for docs and naming.",
                "4. Introduce domain package only when separately approved.",
                "5. Introduce LaseObservationPort only when separately approved.",
                "6. Move LASE classes only when separately approved.",
                "7. Add ArchUnit or equivalent package-boundary enforcement only when separately approved and dependency/build impact is reviewed.",
                "8. Never combine package moves with behavior changes.",
                "9. Never combine boundary enforcement with production claims.",
                "Package moves must not be combined with behavior changes.",
                "ArchUnit or package enforcement is future-only unless separately approved.",
                "Future Enforcement Options",
                "None of these are implemented by this inventory.")) {
            assertTrue(doc.contains(expected), "inventory doc should include migration sequence detail " + expected);
        }
    }

    @Test
    void inventoryDocKeepsNonGoalsAndNoProductionOverclaimsExplicit() throws Exception {
        String doc = read(DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no production Java runtime behavior",
                "no records/classes/interfaces under `src/main/java`",
                "no package moves or refactors",
                "no ArchUnit or new dependency",
                "no Docker, CI, release, signing, registry, or governance changes",
                "no proxy behavior change",
                "no strategy behavior change",
                "no core routing behavior change",
                "no scoring-internals behavior change",
                "no runtime LASE boundary implementation",
                "no runtime workload model implementation",
                "no runtime signal ingestion",
                "no external API clients",
                "no HTTP calls",
                "no secrets, tokens, environment variables, credentials, config, or properties",
                "no telemetry, storage, or persistence",
                "no replay execution",
                "no what-if mutation",
                "no upload/share/download/export/PDF/ZIP behavior",
                "no live-cloud validation claim",
                "no real-tenant validation claim",
                "no GPU orchestration claim",
                "no power/grid control claim",
                "no carbon-aware routing implementation claim",
                "no facility automation claim",
                "no production readiness claim",
                "no production certification claim",
                "This inventory does not claim LASE package boundary is enforced.")) {
            assertTrue(doc.contains(expected), "inventory doc should keep explicit boundary " + expected);
        }

        for (String forbidden : List.of(
                "this inventory enforces the lase boundary",
                "lase package boundary is enforced by this inventory",
                "this inventory adds archunit enforcement",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "inventory doc must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkInventoryAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, REVIEWER_TRUST_MAP, ENTERPRISE_AUDIT, THREE_TIER, LASE_BOUNDARY)) {
            assertTrue(read(path).contains("LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md"),
                    path + " should link the inventory doc");
        }

        String readme = read(README);
        String trustMap = read(REVIEWER_TRUST_MAP);
        String laseBoundary = read(LASE_BOUNDARY);

        assertTrue(readme.contains("docs/test-only migration-readiness inventory"));
        assertTrue(trustMap.contains("docs/test-only current class mapping and migration-readiness inventory"));
        assertTrue(laseBoundary.contains("maps current classes into future boundary buckets without moving classes"));
    }

    @Test
    void sprintDoesNotIntroduceRuntimeInventoryTypesOrArchUnitDependency() throws Exception {
        assertFalse(read(POM).toLowerCase(Locale.ROOT).contains("archunit"),
                "this sprint must not add an ArchUnit dependency or build change");

        List<String> forbiddenRuntimeTokens = List.of(
                "LaseBoundaryEnforcementInventory",
                "LasePackageBoundaryRule",
                "LaseBoundaryArchUnit",
                "LaseObservationPort");

        try (Stream<Path> files = Files.walk(PRODUCTION_SOURCE)) {
            List<Path> javaFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path javaFile : javaFiles) {
                String source = read(javaFile);
                for (String token : forbiddenRuntimeTokens) {
                    assertFalse(source.contains(token),
                            "production source must not introduce runtime inventory/enforcement type "
                                    + token + " in " + javaFile);
                }
            }
        }
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
