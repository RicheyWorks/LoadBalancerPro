package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LocalLabImplementationReadinessGateTest {
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path READINESS_DOC = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final List<Path> READINESS_SOURCES = List.of(
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/"
                    + "LocalLabImplementationReadinessCriterion.java"),
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/"
                    + "LocalLabImplementationReadinessGate.java"),
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/"
                    + "LocalLabImplementationReadinessAssessment.java"));
    private static final List<String> REQUIRED_CRITERIA = List.of(
            "criterion-scenario-behavior-profiles-present",
            "criterion-response-fixtures-cover-scenarios",
            "criterion-passive-transcripts-cover-scenarios",
            "criterion-summaries-cover-transcripts",
            "criterion-checklists-cover-summaries",
            "criterion-checklists-include-evidence-expectations",
            "criterion-checklists-include-safety-boundaries",
            "criterion-checklists-include-not-proven-boundaries",
            "criterion-checklists-include-local-simulation-warning",
            "criterion-doc-tooling-boundary-is-future-only",
            "criterion-doc-fake-backend-execution-boundary-is-future-only",
            "criterion-passive-artifacts-avoid-validation-overclaims",
            "criterion-passive-artifacts-avoid-execution-storage-export-runtime-overclaims");

    @Test
    void readinessGateEvaluatesAllRequiredCriteriaAndCurrentPassiveCriteriaPass() {
        LocalLabImplementationReadinessAssessment assessment = LocalLabImplementationReadinessGate.assess();

        assertEquals(REQUIRED_CRITERIA, assessment.criteria().stream()
                .map(LocalLabImplementationReadinessCriterion::criterionId)
                .toList());
        assertEquals(REQUIRED_CRITERIA.size(), assessment.criteriaCount());
        assertEquals(REQUIRED_CRITERIA.size(), assessment.passedCriteriaCount());
        assertEquals(0, assessment.blockedCriteriaCount());
        assertTrue(assessment.criteria().stream().allMatch(LocalLabImplementationReadinessCriterion::passed));
        assertEquals(REQUIRED_CRITERIA.size(), Set.copyOf(REQUIRED_CRITERIA).size());
    }

    @Test
    void readinessAssessmentIsDeterministicStableAndImmutable() {
        LocalLabImplementationReadinessAssessment first = LocalLabImplementationReadinessGate.assess();
        LocalLabImplementationReadinessAssessment second = LocalLabImplementationReadinessGate.assess();

        assertEquals(first, second);
        assertEquals(first.deterministicText(), second.deterministicText());
        assertEquals("local-lab-implementation-readiness-gate", first.gateId());
        assertEquals("PASSIVE_FOUNDATION_READY_FOR_SEPARATE_IMPLEMENTATION_PR", first.statusLabel());
        assertThrows(UnsupportedOperationException.class, () -> first.criteria().add(first.criteria().get(0)));
    }

    @Test
    void assessmentNamesTheNextSafeImplementationCandidateAndSeparateScopeBoundary() {
        LocalLabImplementationReadinessAssessment assessment = LocalLabImplementationReadinessGate.assess();
        String normalized = normalize(assessment.deterministicText());

        assertTrue(assessment.nextSafeImplementationCandidate().contains("separately scoped"));
        assertTrue(assessment.nextSafeImplementationCandidate().contains("fake backend server implementation PR"));
        assertTrue(assessment.readinessSummary().contains("separately scoped"));
        assertTrue(normalized.contains("ready for a separate implementation pr"));
        assertTrue(normalized.contains("not production proof"));
    }

    @Test
    void assessmentCarriesSafetyAndNotProvenBoundariesWithoutValidationExecutionOrRuntimeClaims() {
        LocalLabImplementationReadinessAssessment assessment = LocalLabImplementationReadinessGate.assess();
        String normalized = normalize(assessment.deterministicText());

        assertTrue(normalized.contains("test-scope passive in-memory evaluation only"));
        assertTrue(normalized.contains("does not implement a server"));
        assertTrue(normalized.contains("start listeners"));
        assertTrue(normalized.contains("open ports"));
        assertTrue(normalized.contains("call loopback endpoints"));
        assertTrue(normalized.contains("generate traffic"));
        assertTrue(normalized.contains("run tools"));
        assertTrue(normalized.contains("not live-cloud validation"));
        assertTrue(normalized.contains("not real-tenant validation"));
        assertTrue(normalized.contains("not actual fake backend execution"));
        assertTrue(normalized.contains("not replay execution"));
        assertTrue(normalized.contains("report generation"));
        assertTrue(normalized.contains("storage"));
        assertTrue(normalized.contains("export"));
        assertTrue(normalized.contains("runtime behavior"));

        for (String forbidden : List.of(
                "production-ready",
                "production certified",
                "production certification is proven",
                "production validation is complete",
                "live-cloud validated",
                "live-cloud validation is complete",
                "real-tenant validated",
                "real-tenant validation is complete",
                "fake backend server is implemented",
                "actual fake backend execution is implemented",
                "actual traffic is generated",
                "http request is executed",
                "docker compose is implemented",
                "k6 scenario is implemented",
                "bruno collection is implemented",
                "toxiproxy config is implemented",
                "prometheus/grafana dashboard is implemented",
                "replay execution is implemented",
                "evidence report is generated",
                "report generation is implemented",
                "storage is implemented",
                "export behavior is implemented",
                "runtime behavior is changed",
                "routing behavior is changed")) {
            assertFalse(normalized.contains(forbidden), "assessment must not overclaim " + forbidden);
        }
    }

    @Test
    void readinessGateDoesNotMutateExistingCatalogData() {
        List<LocalLabFakeBackendNodeScenario> scenarios = LocalLabScenarioCatalog.scenarios();
        List<LocalLabFakeBackendResponseFixture> fixtures = LocalLabFakeBackendResponseFixtureCatalog.fixtures();
        List<LocalLabPassiveTranscriptScenario> transcripts = LocalLabPassiveTranscriptCatalog.transcripts();
        List<LocalLabPassiveTranscriptSummary> summaries = LocalLabPassiveTranscriptSummaryRenderer.summaries();
        List<LocalLabPassiveReviewerChecklist> checklists = LocalLabPassiveReviewerChecklistMapper.checklists();

        LocalLabImplementationReadinessGate.assess();

        assertEquals(scenarios, LocalLabScenarioCatalog.scenarios());
        assertEquals(fixtures, LocalLabFakeBackendResponseFixtureCatalog.fixtures());
        assertEquals(transcripts, LocalLabPassiveTranscriptCatalog.transcripts());
        assertEquals(summaries, LocalLabPassiveTranscriptSummaryRenderer.summaries());
        assertEquals(checklists, LocalLabPassiveReviewerChecklistMapper.checklists());
    }

    @Test
    void readinessGateSourcesStayPassiveAndAvoidSideEffectApis() throws Exception {
        for (Path source : READINESS_SOURCES) {
            String text = Files.readString(source, StandardCharsets.UTF_8);
            String normalized = normalize(text);

            for (String forbidden : List.of(
                    "thread.sleep",
                    "java.time",
                    "new random",
                    "secureRandom",
                    "uuid",
                    "system.getenv",
                    "system.getproperty",
                    "currenttimemillis",
                    "nanotime",
                    "processbuilder",
                    "runtime.getruntime",
                    "files.",
                    "path.",
                    "java.io",
                    "socket",
                    "serversocket",
                    "httpclient",
                    "urlconnection",
                    "localhost",
                    "127.0.0.1",
                    "http://",
                    "https://",
                    "new thread",
                    "executor")) {
                assertFalse(normalized.contains(normalize(forbidden)), source + " must not use " + forbidden);
            }
        }
    }

    @Test
    void docsDescribeImplementationReadinessGateAsPassiveTestScopeOnly() throws Exception {
        for (Path path : List.of(ADR_0009, MATRIX, TRUST_MAP, READINESS_DOC)) {
            String doc = read(path);

            assertTrue(doc.contains("This PR adds a test-scope implementation readiness gate only."));
            assertTrue(doc.contains("The readiness gate evaluates passive planning/test artifacts in memory."));
            assertTrue(doc.contains("It does not implement fake backend servers."));
            assertTrue(doc.contains("It does not execute replay."));
            assertTrue(doc.contains("It does not generate evidence reports."));
            assertTrue(doc.contains("It does not write files."));
            assertTrue(doc.contains("It does not persist storage."));
            assertTrue(doc.contains("It does not export/download/upload/PDF/ZIP anything."));
            assertTrue(doc.contains(
                    "It does not start listeners, open ports, call localhost, generate traffic, or run tools."));
            assertTrue(doc.contains("Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling."));
            assertTrue(doc.contains("Fake backend execution remains future tooling only."));
            assertTrue(doc.contains(
                    "Passing the readiness gate only means ready for a separately scoped implementation PR, "
                            + "not production proof."));
        }
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
