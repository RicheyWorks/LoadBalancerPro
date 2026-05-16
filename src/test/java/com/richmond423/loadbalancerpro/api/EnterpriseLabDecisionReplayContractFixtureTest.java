package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class EnterpriseLabDecisionReplayContractFixtureTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path SNAPSHOT = Path.of(
            "src/test/resources/enterprise-lab/decision-replay/sample-decision-snapshot.json");
    private static final Path REQUEST = Path.of(
            "src/test/resources/enterprise-lab/decision-replay/sample-what-if-request.json");
    private static final Path RESULT = Path.of(
            "src/test/resources/enterprise-lab/decision-replay/sample-what-if-result.json");
    private static final Path PLAN = Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_WHAT_IF_PLAN.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ROUTING_CONTROLLER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/RoutingController.java");
    private static final Path MAIN_SOURCE = Path.of("src/main/java");
    private static final List<Path> FIXTURES = List.of(SNAPSHOT, REQUEST, RESULT);
    private static final List<String> FORBIDDEN_DECISION_REPLAY_ENDPOINTS = List.of(
            "/api/routing/replay",
            "/api/routing/what-if",
            "/api/routing/decision-replay");
    private static final List<String> UNSAFE_COMPLETION_CLAIMS = List.of(
            "replay execution is complete",
            "what-if execution is complete",
            "production replay proof",
            "production traffic replay proof",
            "live cloud proof complete",
            "real tenant proof complete",
            "sla/slo proof complete",
            "production certification complete",
            "registry publication is complete",
            "container signing is complete",
            "governance-applied proof complete",
            "signed container artifact",
            "registry-published artifact");

    @Test
    void decisionReplayFixturesAreValidJsonObjects() throws Exception {
        for (Path fixture : FIXTURES) {
            JsonNode root = fixture(fixture);
            assertTrue(root.isObject(), fixture + " should be a JSON object");
            assertTrue(root.size() > 0, fixture + " should not be empty");
        }
    }

    @Test
    void decisionSnapshotFixtureDefinesReplayableDecisionEvidenceShape() throws Exception {
        JsonNode snapshot = fixture(SNAPSHOT);

        assertRequired(snapshot, "snapshotVersion");
        assertRequired(snapshot, "fixtureSnapshotId");
        assertRequired(snapshot, "capturedAt");
        assertRequired(snapshot, "strategy");
        assertRequired(snapshot, "selectedBackend");
        assertRequired(snapshot, "candidates");
        assertRequired(snapshot, "decisionVector");
        assertRequired(snapshot, "factorContributions");
        assertRequired(snapshot, "knownSignals");
        assertRequired(snapshot, "unknownSignals");
        assertRequired(snapshot, "exactnessBoundaries");
        assertRequired(snapshot, "notProductionProof");

        assertEquals("enterprise-lab-decision-snapshot-v1", snapshot.path("snapshotVersion").asText());
        assertEquals("decision-replay-fixture-snapshot-001", snapshot.path("fixtureSnapshotId").asText());
        assertEquals("2026-01-01T00:00:00Z", snapshot.path("capturedAt").asText());
        assertEquals("weighted-response-time", snapshot.path("strategy").asText());
        assertEquals("edge-alpha", snapshot.path("selectedBackend").path("backendId").asText());
        assertTrue(snapshot.path("candidates").isArray());
        assertTrue(snapshot.path("candidates").size() >= 3);
        assertTrue(snapshot.path("factorContributions").isArray());
        assertTrue(snapshot.path("decisionVector").path("knownSignals").isArray());
        assertTrue(snapshot.path("decisionVector").path("unknownSignals").isArray());
        assertTrue(snapshot.path("decisionVector").path("replayReadiness").asText()
                .contains("planned/not implemented"));
        assertTrue(snapshot.path("decisionVector").path("whatIfReadiness").asText()
                .contains("planned/not implemented"));
        assertBoundaryBooleans(snapshot.path("notProductionProof"));
    }

    @Test
    void whatIfRequestFixtureMutatesExactlyOneVisibleLabSignal() throws Exception {
        JsonNode request = fixture(REQUEST);
        JsonNode mutation = request.path("mutation");

        assertRequired(request, "requestVersion");
        assertRequired(request, "sourceSnapshotId");
        assertRequired(request, "mutation");
        assertRequired(request, "labOnly");
        assertRequired(request, "boundary");
        assertRequired(request, "notProductionProof");
        assertRequired(mutation, "name");
        assertRequired(mutation, "targetBackend");
        assertRequired(mutation, "targetSignal");
        assertRequired(mutation, "originalValue");
        assertRequired(mutation, "hypotheticalValue");
        assertRequired(mutation, "reason");

        assertEquals("enterprise-lab-what-if-request-v1", request.path("requestVersion").asText());
        assertEquals("decision-replay-fixture-snapshot-001", request.path("sourceSnapshotId").asText());
        assertEquals("lower-edge-beta-p95-latency", mutation.path("name").asText());
        assertEquals("edge-beta", mutation.path("targetBackend").asText());
        assertEquals("p95LatencyMs", mutation.path("targetSignal").asText());
        assertEquals(118, mutation.path("originalValue").asInt());
        assertEquals(38, mutation.path("hypotheticalValue").asInt());
        assertTrue(request.path("labOnly").asBoolean());
        assertTrue(request.path("boundary").asText().contains("Fixture-only contract seed"));
        assertBoundaryBooleans(request.path("notProductionProof"));
    }

    @Test
    void whatIfResultFixtureComparesOriginalAndHypotheticalSelectionWithoutProductionProof() throws Exception {
        JsonNode result = fixture(RESULT);

        assertRequired(result, "resultVersion");
        assertRequired(result, "originalSelectedBackend");
        assertRequired(result, "hypotheticalSelectedBackend");
        assertRequired(result, "changedSelection");
        assertRequired(result, "changedFactors");
        assertRequired(result, "unchangedFactors");
        assertRequired(result, "explanationSummary");
        assertRequired(result, "boundaries");
        assertRequired(result, "notProductionProof");

        assertEquals("enterprise-lab-what-if-result-v1", result.path("resultVersion").asText());
        assertEquals("edge-alpha", result.path("originalSelectedBackend").asText());
        assertEquals("edge-beta", result.path("hypotheticalSelectedBackend").asText());
        assertTrue(result.path("changedSelection").asBoolean());
        assertTrue(result.path("changedFactors").isArray());
        assertTrue(result.path("changedFactors").size() >= 1);
        assertTrue(result.path("unchangedFactors").isArray());
        assertTrue(result.path("unchangedFactors").size() >= 1);
        assertTrue(result.path("explanationSummary").asText().contains("Fixture-only result shape"));
        assertBoundaryBooleans(result.path("notProductionProof"));
    }

    @Test
    void fixtureIdentifiersAndTimestampsAreDeterministic() throws Exception {
        JsonNode snapshot = fixture(SNAPSHOT);
        JsonNode request = fixture(REQUEST);
        JsonNode result = fixture(RESULT);
        String allFixtures = read(SNAPSHOT) + read(REQUEST) + read(RESULT);
        String normalized = allFixtures.toLowerCase(Locale.ROOT);

        assertEquals("decision-replay-fixture-snapshot-001", snapshot.path("fixtureSnapshotId").asText());
        assertEquals("decision-replay-fixture-request-001", request.path("fixtureRequestId").asText());
        assertEquals("decision-replay-fixture-result-001", result.path("fixtureResultId").asText());
        assertEquals("2026-01-01T00:00:00Z", snapshot.path("capturedAt").asText());
        assertFalse(normalized.contains("instant.now"));
        assertFalse(normalized.contains("currenttimestamp"));
        assertFalse(normalized.contains("current-time"));
        assertFalse(normalized.contains("generated-at-runtime"));
    }

    @Test
    void fixturesDoNotAdvertiseLiveDecisionReplayEndpoints() throws Exception {
        String fixtures = read(SNAPSHOT) + read(REQUEST) + read(RESULT);

        for (String forbiddenEndpoint : FORBIDDEN_DECISION_REPLAY_ENDPOINTS) {
            assertFalse(fixtures.contains(forbiddenEndpoint),
                    "fixtures must not advertise a live endpoint: " + forbiddenEndpoint);
        }
    }

    @Test
    void docsSayContractFixtureLaneIsFixtureOnlyAndStillPlannedNotImplemented() throws Exception {
        String plan = read(PLAN);
        String trustMap = read(TRUST_MAP);

        assertTrue(plan.contains("## Contract Fixture Lane"));
        assertTrue(plan.contains("sample-decision-snapshot.json"));
        assertTrue(plan.contains("sample-what-if-request.json"));
        assertTrue(plan.contains("sample-what-if-result.json"));
        assertTrue(plan.contains("EnterpriseLabDecisionReplayContractFixtureTest"));
        assertTrue(plan.contains("fixture-only contract seed"));
        assertTrue(plan.contains("It does not execute replay."));
        assertTrue(plan.contains("It does not execute what-if experiments."));
        assertTrue(plan.contains("No live replay endpoint exists."));
        assertTrue(plan.contains("No routing behavior, scoring behavior, strategy weights, proxy behavior"));
        assertTrue(trustMap.contains("EnterpriseLabDecisionReplayContractFixtureTest"));
        assertTrue(trustMap.contains("fixture-only"));
        assertNoUnsafeClaims(plan);
        assertNoUnsafeClaims(trustMap);
    }

    @Test
    void productionRoutingSourceDoesNotExposeDecisionReplayWhatIfEndpoints() throws Exception {
        String routingController = read(ROUTING_CONTROLLER);
        String mainSource = readAllMainJava();

        assertTrue(routingController.contains("@RequestMapping(\"/api/routing\")"));
        assertTrue(routingController.contains("@PostMapping(\"/compare\")"));
        for (String forbiddenEndpoint : FORBIDDEN_DECISION_REPLAY_ENDPOINTS) {
            assertFalse(mainSource.contains(forbiddenEndpoint),
                    "src/main must not expose " + forbiddenEndpoint);
            assertFalse(routingController.contains(forbiddenEndpoint),
                    "routing controller must not expose " + forbiddenEndpoint);
        }
        assertFalse(mainSource.contains("DecisionReplayController"));
        assertFalse(mainSource.contains("DecisionWhatIfController"));
        assertFalse(mainSource.contains("RoutingWhatIfController"));
        assertFalse(mainSource.contains("RoutingDecisionReplayController"));
    }

    private static JsonNode fixture(Path path) throws IOException {
        return OBJECT_MAPPER.readTree(read(path));
    }

    private static void assertRequired(JsonNode root, String field) {
        assertTrue(root.has(field), "missing required field: " + field);
    }

    private static void assertBoundaryBooleans(JsonNode notProductionProof) {
        assertTrue(notProductionProof.path("labOnly").asBoolean()
                || !notProductionProof.has("labOnly"), "labOnly may be a top-level request flag");
        assertFalse(notProductionProof.path("productionTrafficReplay").asBoolean());
        assertFalse(notProductionProof.path("realBackendMutation").asBoolean());
        assertFalse(notProductionProof.path("externalStorage").asBoolean());
        assertFalse(notProductionProof.path("externalTelemetry").asBoolean());
        assertFalse(notProductionProof.path("liveCloudProof").asBoolean());
        assertFalse(notProductionProof.path("realTenantProof").asBoolean());
        assertFalse(notProductionProof.path("slaSloProof").asBoolean());
        assertFalse(notProductionProof.path("productionCertification").asBoolean());
        assertFalse(notProductionProof.path("registryPublication").asBoolean());
        assertFalse(notProductionProof.path("containerSigning").asBoolean());
    }

    private static void assertNoUnsafeClaims(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String forbidden : UNSAFE_COMPLETION_CLAIMS) {
            assertFalse(normalized.contains(forbidden), "must not overclaim: " + forbidden);
        }
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
}
