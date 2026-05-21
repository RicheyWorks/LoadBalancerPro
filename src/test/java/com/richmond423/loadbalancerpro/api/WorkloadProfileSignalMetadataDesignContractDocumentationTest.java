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

class WorkloadProfileSignalMetadataDesignContractDocumentationTest {
    private static final Path DOC = Path.of("docs/WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md");
    private static final Path EXTERNAL_SIGNAL = Path.of("docs/EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md");
    private static final Path THREE_TIER = Path.of("docs/THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PRODUCTION_SOURCE = Path.of("src/main/java");

    @Test
    void designContractExistsAndStatesNotImplementedStatus() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "# WorkloadProfile Signal Metadata Design Contract",
                "design contract only",
                "not implemented in this sprint",
                "not an API contract",
                "not a Java runtime record/class",
                "not a workload model feature",
                "not a signal ingestion feature",
                "not proof of production readiness",
                "Current status:",
                "no Java source files added for this concept",
                "no runtime record or class",
                "no API field",
                "no workload model code",
                "no scoring influence",
                "no strategy influence",
                "no routing influence",
                "no proxy behavior change",
                "no signal ingestion",
                "no persistence",
                "no telemetry",
                "no secrets, tokens, environment variables, or credentials",
                "no production behavior change")) {
            assertTrue(doc.contains(expected), "design contract should mention " + expected);
        }
    }

    @Test
    void designContractDefinesVocabularyFieldsAndPseudocode() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Future WorkloadProfile Vocabulary",
                "`WorkloadProfile`",
                "`BurstProfile`",
                "`TailLatencyProfile`",
                "`ThinkTimeProfile`",
                "`WorkloadSegment`",
                "`WorkloadClass`",
                "`WorkloadSignalMetadata`",
                "`GpuSensitivity`",
                "`CarbonSensitivity`",
                "`ThermalBurstPattern`",
                "`EstimatedPowerDrawProfile`",
                "`BatchDeferrable`",
                "`LatencyCriticality`",
                "Future Optional Metadata Fields",
                "`workloadClass`",
                "`burstProfile`",
                "`tailLatencyProfile`",
                "`thinkTimeProfile`",
                "`gpuSensitivity`",
                "`carbonSensitivity`",
                "`estimatedPowerDrawProfile`",
                "`thermalBurstPattern`",
                "`latencyCriticality`",
                "`batchDeferrable`",
                "`deterministicSeed`",
                "`metadataVersion`",
                "`evidenceNotes`",
                "Documentation-only pseudocode. Not implemented in this sprint.",
                "record WorkloadProfile(",
                "Optional<BurstProfile> burstProfile",
                "Optional<TailLatencyProfile> tailLatencyProfile",
                "Optional<ThinkTimeProfile> thinkTimeProfile",
                "long deterministicSeed",
                "List<String> evidenceNotes")) {
            assertTrue(doc.contains(expected), "design contract should mention " + expected);
        }

        assertTrue(doc.contains("no routing methods"));
        assertTrue(doc.contains("no scoring methods"));
        assertTrue(doc.contains("no strategy-selection methods"));
        assertTrue(doc.contains("no proxy methods"));
        assertTrue(doc.contains("no external signal calls"));
        assertTrue(doc.contains("no HTTP client contract"));
        assertTrue(doc.contains("no persistence contract"));
        assertTrue(doc.contains("no telemetry contract"));
        assertTrue(doc.contains("no credential contract"));
    }

    @Test
    void relationshipsDeterminismAndSafeDegradationStayReviewerSafe() throws Exception {
        String doc = read(DOC);

        for (String expected : List.of(
                "Relationship To LASE / Shadow Evaluation",
                "begin as metadata shown to reviewers, not hidden scoring behavior",
                "Any future LASE influence must require a separate approved sprint",
                "Relationship To ExternalSignalPort",
                "`WorkloadProfile` is separate from the future `ExternalSignalPort` concept",
                "Missing, stale, or unavailable external signals must not silently mutate WorkloadProfile metadata.",
                "Determinism And Evidence Requirements",
                "Metadata must carry an explicit metadata version.",
                "Missing data must be represented explicitly as `UNKNOWN`, `UNAVAILABLE`, or equivalent.",
                "Synthetic fixture data must be labeled as synthetic.",
                "Estimated data must be labeled as estimated.",
                "Reviewer-supplied notes must be visibly separated from measured or fixture-derived fields.",
                "Safe Degradation Rules",
                "Missing workload profile returns `UNKNOWN` status.",
                "Invalid metadata fails closed to reviewer warnings, not silent scoring influence.",
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
                "no Java runtime record or class",
                "no API behavior change",
                "no API field",
                "no workload model code",
                "no routing behavior change",
                "no scoring behavior change",
                "no strategy behavior change",
                "no proxy behavior change",
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
                "workloadprofile is implemented",
                "runtime workloadprofile",
                "workload model is implemented",
                "workloadprofile currently changes strategy selection",
                "workloadprofile currently changes routing behavior",
                "api field is implemented",
                "scoring behavior is changed",
                "routing behavior is changed",
                "strategy behavior is changed",
                "proxy behavior is changed",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete")) {
            assertFalse(normalized.contains(forbidden), "design contract must not overclaim: " + forbidden);
        }
    }

    @Test
    void reviewerEntryPointsLinkWorkloadProfileContractAsDocsOnlyReference() throws Exception {
        for (Path path : List.of(README, TRUST_MAP, AUDIT, THREE_TIER, EXTERNAL_SIGNAL)) {
            assertTrue(read(path).contains("WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md"),
                    path + " should link the workload profile metadata design contract");
        }

        String readme = read(README);
        String trustMap = read(TRUST_MAP);
        String threeTier = read(THREE_TIER);
        String externalSignal = read(EXTERNAL_SIGNAL);

        assertTrue(readme.contains("without adding runtime records/classes or behavior"));
        assertTrue(trustMap.contains("docs-only design contract for future AI-era workload metadata"));
        assertTrue(trustMap.contains("not a runtime record/class, API field, workload model, scoring input"));
        assertTrue(threeTier.contains("That contract is documentation-only"));
        assertTrue(threeTier.contains("does not implement Java records/classes, API fields, workload model code"));
        assertTrue(externalSignal.contains("That contract is documentation-only"));
        assertTrue(externalSignal.contains("does not implement Java records/classes, API fields, workload model code"));
    }

    @Test
    void sprintDoesNotIntroduceRuntimeWorkloadProfileTypes() throws Exception {
        List<String> forbiddenRuntimeTokens = List.of(
                "WorkloadProfile",
                "BurstProfile",
                "TailLatencyProfile",
                "ThinkTimeProfile",
                "WorkloadSegment",
                "WorkloadClass",
                "WorkloadSignalMetadata",
                "GpuSensitivity",
                "CarbonSensitivity",
                "ThermalBurstPattern",
                "EstimatedPowerDrawProfile",
                "BatchDeferrable",
                "LatencyCriticality");

        try (Stream<Path> files = Files.walk(PRODUCTION_SOURCE)) {
            List<Path> javaFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path javaFile : javaFiles) {
                String source = read(javaFile);
                for (String token : forbiddenRuntimeTokens) {
                    assertFalse(source.contains(token),
                            "production source must not introduce runtime workload profile type " + token
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
