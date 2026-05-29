package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerModularityRegressionTest {
    private static final Path API_SOURCE = Path.of(
            "src", "main", "java", "com", "richmond423", "loadbalancerpro", "api");

    @Test
    void routeTradeoffServiceRemainsThinOrchestratorAfterRefactor() throws IOException {
        String source = source("DecisionExplorerRouteTradeoffService.java");

        assertLineCountAtMost("DecisionExplorerRouteTradeoffService.java", source, 230);
        for (String collaborator : List.of(
                "DecisionExplorerRouteTradeoffRowBuilder",
                "DecisionExplorerCandidateTradeoffScoringBuilder",
                "DecisionExplorerFactorTradeoffDeltaBuilder",
                "DecisionExplorerEvidenceSufficiencyEvaluator",
                "DecisionExplorerReplayReadinessEvaluator",
                "DecisionExplorerRouteTradeoffFingerprintBuilder",
                "DecisionExplorerRouteTradeoffExplanationBuilder")) {
            assertTrue(source.contains(collaborator), "route tradeoff service should delegate to " + collaborator);
        }
        assertDoesNotContainAny(source,
                "private static String diagnosticFingerprint",
                "private static String fingerprintValue",
                "private static List<String> canonicalInputs",
                "private static <T> List<T> copyNonNull");
    }

    @Test
    void shadowDecisionQualityServiceRemainsThinOrchestratorAfterRefactor() throws IOException {
        String source = source("DecisionExplorerShadowDecisionQualityService.java");

        assertLineCountAtMost("DecisionExplorerShadowDecisionQualityService.java", source, 275);
        for (String collaborator : List.of(
                "DecisionExplorerShadowQualityLabelEvaluator",
                "DecisionExplorerShadowCandidateOutcomeBuilder",
                "DecisionExplorerShadowPolicySensitivityEvaluator",
                "DecisionExplorerShadowScenarioInputQualityEvaluator",
                "DecisionExplorerShadowQualityFingerprintBuilder",
                "DecisionExplorerShadowQualityExplanationBuilder")) {
            assertTrue(source.contains(collaborator), "shadow quality service should delegate to " + collaborator);
        }
        assertDoesNotContainAny(source,
                "private static String diagnosticFingerprint",
                "private static String fingerprintValue",
                "private static List<String> canonicalInputs",
                "private static <T> List<T> copyNonNull");
    }

    @Test
    void refactoredRouteTradeoffCollaboratorsUseSharedDiagnosticSupport() throws IOException {
        assertUsesSharedSupport(
                "DecisionExplorerRouteTradeoffFingerprintBuilder.java",
                "DecisionExplorerDiagnosticFingerprintSupport",
                "DecisionExplorerDiagnosticListSupport");
        assertUsesSharedSupport(
                "DecisionExplorerReplayReadinessEvaluator.java",
                "DecisionExplorerDiagnosticFingerprintSupport",
                "DecisionExplorerDiagnosticListSupport");
        assertUsesSharedSupport(
                "DecisionExplorerEvidenceSufficiencyEvaluator.java",
                "DecisionExplorerDiagnosticFingerprintSupport",
                "DecisionExplorerDiagnosticListSupport",
                "distinctSortedNormalizedWhitespace");
        assertUsesSharedSupport(
                "DecisionExplorerShadowQualityFingerprintBuilder.java",
                "DecisionExplorerDiagnosticFingerprintSupport",
                "DecisionExplorerDiagnosticListSupport");
    }

    @Test
    void refactoredDecisionExplorerDiagnosticsDoNotMutateProductionRouting() throws IOException {
        for (String fileName : List.of(
                "DecisionExplorerRouteTradeoffService.java",
                "DecisionExplorerRouteTradeoffRowBuilder.java",
                "DecisionExplorerCandidateTradeoffScoringBuilder.java",
                "DecisionExplorerFactorTradeoffDeltaBuilder.java",
                "DecisionExplorerEvidenceSufficiencyEvaluator.java",
                "DecisionExplorerReplayReadinessEvaluator.java",
                "DecisionExplorerRouteTradeoffFingerprintBuilder.java",
                "DecisionExplorerShadowDecisionQualityService.java",
                "DecisionExplorerShadowQualityFingerprintBuilder.java")) {
            String source = source(fileName);
            assertDoesNotContainAny(source,
                    "com.richmond423.loadbalancerpro.core.LoadBalancer",
                    "com.richmond423.loadbalancerpro.core.Server",
                    "distributeLoad(",
                    "addServer(",
                    "removeServer(",
                    "scaleServers",
                    "proxy",
                    "traffic shift");
        }
    }

    private static void assertUsesSharedSupport(String fileName, String... requiredTokens) throws IOException {
        String source = source(fileName);
        for (String token : requiredTokens) {
            assertTrue(source.contains(token), fileName + " should use " + token);
        }
        assertDoesNotContainAny(source,
                "private static String diagnosticFingerprint",
                "private static String fingerprintValue",
                "private static List<String> canonicalInputs",
                "private static String input(",
                "private static <T> List<T> copyNonNull");
    }

    private static void assertLineCountAtMost(String fileName, String source, int maximumLines) {
        long lineCount = source.lines().count();
        assertTrue(lineCount <= maximumLines,
                fileName + " should stay at or below " + maximumLines + " lines after the modularity refactor but was "
                        + lineCount);
    }

    private static void assertDoesNotContainAny(String source, String... forbiddenTokens) {
        for (String token : forbiddenTokens) {
            assertFalse(source.contains(token), "source must not contain " + token);
        }
    }

    private static String source(String fileName) throws IOException {
        return Files.readString(API_SOURCE.resolve(fileName), StandardCharsets.UTF_8);
    }
}
