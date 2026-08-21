package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class EnterpriseLabDecisionReplayContractReaderTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path SNAPSHOT = Path.of(
            "src/test/resources/enterprise-lab/decision-replay/sample-decision-snapshot.json");
    private static final Path REQUEST = Path.of(
            "src/test/resources/enterprise-lab/decision-replay/sample-what-if-request.json");
    private static final Path RESULT = Path.of(
            "src/test/resources/enterprise-lab/decision-replay/sample-what-if-result.json");
    private static final Path MAIN_SOURCE = Path.of("src/main/java");
    private static final Path READER_TEST_SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/api/"
                    + "EnterpriseLabDecisionReplayContractReaderTest.java");
    private static final List<String> FORBIDDEN_DECISION_REPLAY_ENDPOINTS = List.of(
            "/api/routing/replay",
            "/api/routing/what-if",
            "/api/routing/decision-replay");

    @Test
    void typedReaderParsesAllFixturesIntoStableRecords() throws Exception {
        DecisionSnapshot snapshot = readFixture(SNAPSHOT, DecisionSnapshot.class);
        WhatIfRequest request = readFixture(REQUEST, WhatIfRequest.class);
        WhatIfResult result = readFixture(RESULT, WhatIfResult.class);

        assertAll(
                () -> assertEquals("enterprise-lab-decision-snapshot-v1", snapshot.snapshotVersion()),
                () -> assertEquals("enterprise-lab-what-if-request-v1", request.requestVersion()),
                () -> assertEquals("enterprise-lab-what-if-result-v1", result.resultVersion()),
                () -> assertEquals("decision-replay-fixture-snapshot-001", snapshot.fixtureSnapshotId()),
                () -> assertEquals("decision-replay-fixture-request-001", request.fixtureRequestId()),
                () -> assertEquals("decision-replay-fixture-result-001", result.fixtureResultId()),
                () -> assertEquals("2026-01-01T00:00:00Z", snapshot.capturedAt()),
                () -> assertEquals(Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse(snapshot.capturedAt())));
    }

    @Test
    void fixtureLinkageIsConsistentAcrossTypedContracts() throws Exception {
        DecisionSnapshot snapshot = readFixture(SNAPSHOT, DecisionSnapshot.class);
        WhatIfRequest request = readFixture(REQUEST, WhatIfRequest.class);
        WhatIfResult result = readFixture(RESULT, WhatIfResult.class);

        assertAll(
                () -> assertEquals(snapshot.fixtureSnapshotId(), request.sourceSnapshotId()),
                () -> assertEquals(snapshot.fixtureSnapshotId(), result.sourceSnapshotId()),
                () -> assertEquals(request.fixtureRequestId(), result.sourceRequestId()),
                () -> assertEquals(snapshot.selectedBackend().backendId(), result.originalSelectedBackend()),
                () -> assertEquals(request.mutation().targetBackend(), result.hypotheticalSelectedBackend()));
    }

    @Test
    void typedRecordsPreserveSelectedBackendAndCandidateConsistency() throws Exception {
        DecisionSnapshot snapshot = readFixture(SNAPSHOT, DecisionSnapshot.class);
        WhatIfResult result = readFixture(RESULT, WhatIfResult.class);

        List<Candidate> selectedCandidates = snapshot.candidates().stream()
                .filter(Candidate::selected)
                .toList();

        assertAll(
                () -> assertEquals("weighted-response-time", snapshot.strategy()),
                () -> assertEquals("weighted-response-time", snapshot.decisionVector().selectedStrategy()),
                () -> assertEquals("edge-alpha", snapshot.selectedBackend().backendId()),
                () -> assertEquals("edge-alpha", snapshot.decisionVector().selectedBackendId()),
                () -> assertEquals(1, selectedCandidates.size()),
                () -> assertEquals(snapshot.selectedBackend().backendId(),
                        selectedCandidates.get(0).backendId()),
                () -> assertTrue(snapshot.decisionVector().candidateBackendIds()
                        .containsAll(snapshot.candidates().stream()
                                .map(Candidate::backendId)
                                .toList())),
                () -> assertTrue(result.changedSelection()),
                () -> assertFalse(result.changedFactors().isEmpty()),
                () -> assertFalse(result.unchangedFactors().isEmpty()));
    }

    @Test
    void typedContractsKeepLabOnlyNotProductionProofBoundariesExplicit() throws Exception {
        DecisionSnapshot snapshot = readFixture(SNAPSHOT, DecisionSnapshot.class);
        WhatIfRequest request = readFixture(REQUEST, WhatIfRequest.class);
        WhatIfResult result = readFixture(RESULT, WhatIfResult.class);

        assertAll(
                () -> assertTrue(snapshot.notProductionProof().labOnly()),
                () -> assertTrue(request.labOnly()),
                () -> assertTrue(result.notProductionProof().labOnly()),
                () -> assertNotProductionProof(snapshot.notProductionProof()),
                () -> assertNotProductionProof(request.notProductionProof()),
                () -> assertNotProductionProof(result.notProductionProof()),
                () -> assertFalse(snapshot.exactnessBoundaries().isEmpty()),
                () -> assertTrue(snapshot.exactnessBoundaries().stream()
                        .anyMatch(boundary -> boundary.contains("Exact only for visible fixture fields"))),
                () -> assertTrue(snapshot.exactnessBoundaries().stream()
                        .anyMatch(boundary -> boundary.contains("Not exact for production traffic behavior"))),
                () -> assertTrue(snapshot.unknownSignals().contains("productionTraffic")),
                () -> assertTrue(snapshot.decisionVector().unknownSignals().contains("productionTelemetry")),
                () -> assertTrue(result.boundaries().stream()
                        .anyMatch(boundary -> boundary.contains("Unknown and unexposed signals"))));
    }

    @Test
    void typedReaderDoesNotGenerateCurrentTimestampsDuringParsing() throws Exception {
        DecisionSnapshot firstRead = readFixture(SNAPSHOT, DecisionSnapshot.class);
        DecisionSnapshot secondRead = readFixture(SNAPSHOT, DecisionSnapshot.class);
        String allFixtures = read(SNAPSHOT) + read(REQUEST) + read(RESULT);
        String normalized = allFixtures.toLowerCase(Locale.ROOT);

        assertAll(
                () -> assertEquals(firstRead.capturedAt(), secondRead.capturedAt()),
                () -> assertEquals("2026-01-01T00:00:00Z", firstRead.capturedAt()),
                () -> assertFalse(firstRead.capturedAt().contains(String.valueOf(Instant.now().getEpochSecond()))),
                () -> assertFalse(normalized.contains("instant.now")),
                () -> assertFalse(normalized.contains("currenttimestamp")),
                () -> assertFalse(normalized.contains("current-time")),
                () -> assertFalse(normalized.contains("generated-at-runtime")));
    }

    @Test
    void fixturesAndMainSourceDoNotPresentDecisionReplayEndpointsAsImplemented() throws Exception {
        String fixtures = read(SNAPSHOT) + read(REQUEST) + read(RESULT);
        String mainSource = readAllMainJava();

        for (String forbiddenEndpoint : FORBIDDEN_DECISION_REPLAY_ENDPOINTS) {
            assertFalse(fixtures.contains(forbiddenEndpoint),
                    "fixtures must not advertise endpoint " + forbiddenEndpoint);
            assertFalse(mainSource.contains(forbiddenEndpoint),
                    "src/main must not expose endpoint " + forbiddenEndpoint);
        }
        assertFalse(mainSource.contains("DecisionReplayController"));
        assertFalse(mainSource.contains("DecisionWhatIfController"));
        assertFalse(mainSource.contains("RoutingWhatIfController"));
        assertFalse(mainSource.contains("RoutingDecisionReplayController"));
        assertFalse(mainSource.contains("DecisionReplayService"));
        assertFalse(mainSource.contains("DecisionWhatIfService"));
    }

    @Test
    void readerIsTestOnlyAndDoesNotImportRoutingOrScoringRuntimePaths() throws Exception {
        String readerSource = read(READER_TEST_SOURCE);
        List<String> imports = readerSource.lines()
                .filter(line -> line.startsWith("import "))
                .toList();

        assertAll(
                () -> assertTrue(Files.exists(READER_TEST_SOURCE)),
                () -> assertFalse(Files.exists(Path.of(
                        "src/main/java/com/richmond423/loadbalancerpro/api/"
                                + "EnterpriseLabDecisionReplayContractReader.java"))),
                () -> assertTrue(imports.stream().noneMatch(line -> line.contains("loadbalancerpro.core"))),
                () -> assertTrue(imports.stream().noneMatch(line -> line.contains("loadbalancerpro.strategy"))),
                () -> assertTrue(imports.stream().noneMatch(line -> line.contains("ServerScore"))),
                () -> assertTrue(imports.stream().noneMatch(line -> line.contains("LoadBalancer"))));
    }

    private static <T> T readFixture(Path path, Class<T> type) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return OBJECT_MAPPER.readValue(read(path), type);
    }

    private static void assertNotProductionProof(NotProductionProof proof) {
        assertNotNull(proof);
        assertFalse(proof.productionTrafficReplay());
        assertFalse(proof.realBackendMutation());
        assertFalse(proof.externalStorage());
        assertFalse(proof.externalTelemetry());
        assertFalse(proof.liveCloudProof());
        assertFalse(proof.realTenantProof());
        assertFalse(proof.slaSloProof());
        assertFalse(proof.productionCertification());
        assertFalse(proof.registryPublication());
        assertFalse(proof.containerSigning());
    }

    private static String readAllMainJava() throws IOException {
        StringBuilder builder = new StringBuilder();
        try (Stream<Path> paths = Files.walk(MAIN_SOURCE)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList()) {
                builder.append(read(path)).append('\n');
            }
        }
        return builder.toString();
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private record DecisionSnapshot(
            String snapshotVersion,
            String fixtureSnapshotId,
            String capturedAt,
            String strategy,
            SelectedBackend selectedBackend,
            List<Candidate> candidates,
            DecisionVector decisionVector,
            List<FactorContribution> factorContributions,
            List<String> knownSignals,
            List<String> unknownSignals,
            List<String> exactnessBoundaries,
            NotProductionProof notProductionProof) {
    }

    private record SelectedBackend(String backendId, String displayName, boolean healthy) {
    }

    private record Candidate(String backendId, boolean selected, boolean healthy, Signals signals) {
    }

    private record Signals(
            int p95LatencyMs,
            double currentLoad,
            double recentErrorRate,
            int configuredCapacity) {
    }

    private record DecisionVector(
            String schemaVersion,
            String selectedStrategy,
            String selectedBackendId,
            List<String> candidateBackendIds,
            List<String> knownSignals,
            List<String> unknownSignals,
            String replayReadiness,
            String whatIfReadiness) {
    }

    private record FactorContribution(
            String backendId,
            String factor,
            String direction,
            String summary) {
    }

    private record WhatIfRequest(
            String requestVersion,
            String fixtureRequestId,
            String sourceSnapshotId,
            Mutation mutation,
            boolean labOnly,
            String boundary,
            NotProductionProof notProductionProof) {
    }

    private record Mutation(
            String name,
            String targetBackend,
            String targetSignal,
            int originalValue,
            int hypotheticalValue,
            String validationBoundary,
            String reason) {
    }

    private record WhatIfResult(
            String resultVersion,
            String fixtureResultId,
            String sourceSnapshotId,
            String sourceRequestId,
            String originalSelectedBackend,
            String hypotheticalSelectedBackend,
            boolean changedSelection,
            List<ChangedFactor> changedFactors,
            List<UnchangedFactor> unchangedFactors,
            String explanationSummary,
            List<String> boundaries,
            NotProductionProof notProductionProof) {
    }

    private record ChangedFactor(
            String factor,
            String backendId,
            int originalValue,
            int hypotheticalValue,
            String summary) {
    }

    private record UnchangedFactor(String factor, String summary) {
    }

    private record NotProductionProof(
            Boolean labOnly,
            boolean productionTrafficReplay,
            boolean realBackendMutation,
            boolean externalStorage,
            boolean externalTelemetry,
            boolean liveCloudProof,
            boolean realTenantProof,
            boolean slaSloProof,
            boolean productionCertification,
            boolean registryPublication,
            boolean containerSigning) {
    }
}
