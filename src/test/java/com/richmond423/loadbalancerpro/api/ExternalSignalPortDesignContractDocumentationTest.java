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

class ExternalSignalPortDesignContractDocumentationTest {
    private static final Path DOC = Path.of("docs/EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md");
    private static final Path THREE_TIER = Path.of("docs/THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PRODUCTION_SOURCE = Path.of("src/main/java");

    @Test
    void designContractExistsAndStatesNotImplementedStatus() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# External Signal Port Design Contract",
                "design contract only",
                "not implemented in this sprint",
                "not an API contract",
                "not a Java runtime interface",
                "not a signal ingestion feature",
                "not proof of production readiness",
                "Current status:",
                "no Java source files added for this concept",
                "no runtime interface",
                "no adapters",
                "no HTTP clients",
                "no signal ingestion",
                "no persistence",
                "no telemetry",
                "no secrets, tokens, environment variables, or credentials",
                "no production behavior change")) {
            assertTrue(doc.contains(expected), "design contract should mention " + expected);
        }
    }

    @Test
    void designContractDefinesReadOnlySignalsVocabularyAndPseudocode() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Read-Only Signal Boundary",
                "Future Signal Categories",
                "Workload / compute pressure",
                "GPU cluster pressure",
                "Power cost",
                "Carbon intensity",
                "Thermal/facility pressure",
                "Signal freshness / confidence",
                "`ExternalSignalPort`",
                "`ExternalSignalSnapshot`",
                "`SignalSourceMetadata`",
                "`SignalFreshnessStatus`",
                "`SignalTrustLevel`",
                "`CarbonIntensitySignal`",
                "`GpuClusterPressureSignal`",
                "`FacilityThermalPressureSignal`",
                "`PowerCostSignal`",
                "Documentation-only pseudocode. Not implemented in this sprint.",
                "ExternalSignalSnapshot getCurrentSignalSnapshot()",
                "SignalSourceMetadata describeSignalSource()")) {
            assertTrue(doc.contains(expected), "design contract should mention " + expected);
        }

        assertTrue(doc.contains("no mutation methods"));
        assertTrue(doc.contains("no write methods"));
        assertTrue(doc.contains("no control methods"));
        assertTrue(doc.contains("no HTTP client contract"));
        assertTrue(doc.contains("no persistence contract"));
        assertTrue(doc.contains("no telemetry contract"));
        assertTrue(doc.contains("no credential contract"));
    }

    @Test
    void determinismEvidenceAndRelationshipSectionsStayReviewerSafe() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Determinism And Evidence Requirements",
                "Signal snapshots must carry source metadata.",
                "Signal snapshots must carry freshness status.",
                "Signal snapshots must carry trust level.",
                "Missing data must be represented explicitly as `UNKNOWN`, `UNAVAILABLE`, or equivalent.",
                "Synthetic fixture data must be labeled as synthetic.",
                "Estimated data must be labeled as estimated.",
                "Relationship To LASE / Shadow Evaluation",
                "begin as reviewer metadata, not hidden scoring behavior",
                "Any future scoring influence must require a separate approved sprint",
                "Relationship To WorkloadProfile",
                "`ExternalSignalPort` is separate from a future `WorkloadProfile`.",
                "Reviewer-Facing Value",
                "Future Implementation Gates",
                "No future implementation should be merged only because this design contract exists.")) {
            assertTrue(doc.contains(expected), "design contract should mention " + expected);
        }
    }

    @Test
    void explicitNonGoalsPreventRuntimeAndProductionOverclaims() throws Exception {
        String doc = read(DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no Java runtime interface",
                "no adapters",
                "no signal ingestion",
                "no external API clients",
                "no HTTP calls",
                "no secrets, tokens, environment variables, or credentials",
                "no telemetry, storage, or persistence",
                "no MessageDigest, SHA, hash, UUID, random, time, environment, or system-property behavior",
                "no replay execution",
                "no what-if mutation",
                "no upload/share/download/export/PDF/ZIP behavior",
                "no Docker, CI, release, signing, registry, or governance changes",
                "no proxy behavior changes",
                "no strategy behavior changes",
                "no core routing behavior changes",
                "no scoring-internals changes",
                "no GPU orchestration claim",
                "no live power/grid control",
                "no facility mutation",
                "no carbon-aware routing implementation",
                "no production readiness claim",
                "no production certification claim",
                "no live-cloud validation claim",
                "no real-tenant validation claim")) {
            assertTrue(doc.contains(expected), "design contract should keep boundary " + expected);
        }

        for (String forbidden : List.of(
                "externalsignalport is implemented",
                "runtime externalsignalport",
                "signal ingestion is implemented",
                "http client is implemented",
                "carbon-aware routing is currently implemented",
                "currently performs carbon-aware routing",
                "gpu orchestration is implemented",
                "power grid control is implemented",
                "facility automation is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "design contract must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkExternalSignalContractAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, TRUST_MAP, AUDIT, THREE_TIER)) {
            assertTrue(read(path).contains("EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md"),
                    path + " should link the external signal port design contract");
        }

        String readme = read(README);
        String trustMap = read(TRUST_MAP);
        String threeTier = read(THREE_TIER);

        assertTrue(readme.contains("without adding runtime implementation"));
        assertTrue(trustMap.contains("docs-only design contract"));
        assertTrue(trustMap.contains("not a runtime interface, adapter, HTTP client, signal ingestion path"));
        assertTrue(threeTier.contains("That contract is documentation-only"));
        assertTrue(threeTier.contains("does not implement Java interfaces, adapters, clients, signal ingestion"));
    }

    @Test
    void sprintDoesNotIntroduceRuntimeExternalSignalTypes() throws Exception {
        List<String> forbiddenRuntimeTokens = List.of(
                "ExternalSignalPort",
                "ExternalSignalSnapshot",
                "SignalSourceMetadata",
                "SignalFreshnessStatus",
                "SignalTrustLevel",
                "CarbonIntensitySignal",
                "GpuClusterPressureSignal",
                "FacilityThermalPressureSignal",
                "PowerCostSignal");

        try (Stream<Path> files = Files.walk(PRODUCTION_SOURCE)) {
            List<Path> javaFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path javaFile : javaFiles) {
                String source = read(javaFile);
                for (String token : forbiddenRuntimeTokens) {
                    assertFalse(source.contains(token),
                            "production source must not introduce runtime external signal type " + token
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
