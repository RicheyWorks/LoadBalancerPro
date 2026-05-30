package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class DecisionExplorerCounterfactualFixtureCatalogTest {

    @Test
    void fixturesCoverMajorCounterfactualStatesInDeterministicOrder() {
        List<DecisionExplorerCounterfactualFixtureCatalog.CounterfactualFixture> fixtures =
                DecisionExplorerCounterfactualFixtureCatalog.fixtures();

        assertEquals(List.of(
                        "counterfactual-stable-selected-advantage",
                        "counterfactual-sensitive-partial-evidence",
                        "counterfactual-close-alternative",
                        "counterfactual-degraded-selected",
                        "counterfactual-insufficient-evidence",
                        "counterfactual-unknown-empty-evidence"),
                fixtures.stream()
                        .map(DecisionExplorerCounterfactualFixtureCatalog.CounterfactualFixture::fixtureId)
                        .toList());
        assertEquals(List.of("STABLE", "SENSITIVE", "CLOSE_CALL", "DEGRADED", "INSUFFICIENT_EVIDENCE",
                        "UNKNOWN"),
                fixtures.stream()
                        .map(DecisionExplorerCounterfactualFixtureCatalog.CounterfactualFixture::expectedLabel)
                        .toList());
    }

    @Test
    void fixturesBuildExpectedLocalOnlyCounterfactualOutputs() {
        for (DecisionExplorerCounterfactualFixtureCatalog.CounterfactualFixture fixture
                : DecisionExplorerCounterfactualFixtureCatalog.fixtures()) {
            DecisionExplorerCounterfactualAnalysisV1 analysis = fixture.analysis();

            assertTrue(analysis.readOnly(), fixture.fixtureId());
            assertTrue(analysis.simulationOnly(), fixture.fixtureId());
            assertTrue(analysis.localOnly(), fixture.fixtureId());
            assertEquals(fixture.expectedLabel(), analysis.counterfactualLabel(), fixture.fixtureId());
            assertEquals(fixture.expectedBand(), analysis.sensitivityBand(), fixture.fixtureId());
            assertEquals(fixture.expectedPolicyWeightScenarioCount(), analysis.policyWeightScenarioCount(),
                    fixture.fixtureId());
            assertEquals(fixture.expectedCandidateOutcomeCount(), analysis.counterfactualCandidateOutcomeCount(),
                    fixture.fixtureId());
            assertEquals(fixture.expectedFactorWeightDeltaCount(), analysis.factorWeightDeltaCount(),
                    fixture.fixtureId());
            assertEquals(DecisionExplorerCounterfactualFixtureCatalog.BOUNDARY_NOTE, analysis.boundaryNote(),
                    fixture.fixtureId());
            assertTrue(analysis.diagnosticFingerprint().startsWith("counterfactual-analysis|v1|"),
                    fixture.fixtureId());
            assertTrue(analysis.reproducibilityKey().startsWith("counterfactual:v1:"),
                    fixture.fixtureId());
            assertTrue(analysis.summaryText().toLowerCase(Locale.ROOT).contains("counterfactual"),
                    fixture.fixtureId());
        }
    }

    @Test
    void fixtureOutputsAreDeterministicAcrossCatalogBuilds() {
        List<DecisionExplorerCounterfactualFixtureCatalog.CounterfactualFixture> first =
                DecisionExplorerCounterfactualFixtureCatalog.fixtures();
        List<DecisionExplorerCounterfactualFixtureCatalog.CounterfactualFixture> second =
                DecisionExplorerCounterfactualFixtureCatalog.fixtures();

        assertEquals(
                first.stream().map(fixture -> fixture.analysis().diagnosticFingerprint()).toList(),
                second.stream().map(fixture -> fixture.analysis().diagnosticFingerprint()).toList());
        assertEquals(
                first.stream().map(fixture -> fixture.analysis().reproducibilityKey()).toList(),
                second.stream().map(fixture -> fixture.analysis().reproducibilityKey()).toList());
        assertEquals(
                first.stream().map(fixture -> fixture.analysis().fingerprintInputs()).toList(),
                second.stream().map(fixture -> fixture.analysis().fingerprintInputs()).toList());
    }

    @Test
    void fixturesStayLocalOnlyAndDoNotAdvertiseRuntimeExecution() throws Exception {
        String source = Files.readString(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/"
                + "DecisionExplorerCounterfactualFixtureCatalog.java"), StandardCharsets.UTF_8);
        String normalized = source.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("local-only counterfactual diagnostics"));
        for (String forbidden : List.of(
                "httpclient",
                "urlconnection",
                "socket",
                "files.write",
                "distributeload",
                "addserver",
                "removeserver",
                "proxyclient",
                "trafficshifter",
                "replay execution",
                "evidence packet",
                "live-cloud",
                "real-tenant")) {
            assertFalse(normalized.contains(forbidden), "fixture catalog must not contain " + forbidden);
        }
    }
}
