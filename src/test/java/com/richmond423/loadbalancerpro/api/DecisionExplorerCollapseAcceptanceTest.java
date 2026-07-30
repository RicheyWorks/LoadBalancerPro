package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DecisionExplorerCollapseAcceptanceTest {
    private static final Path API_SOURCE =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api");
    private static final Set<String> DECISION_EXPLORER_SURVIVORS = Set.of(
            "DecisionExplorerDtoSupport.java",
            "DecisionExplorerResponseSizeGuard.java",
            "DecisionExplorerScenarioCatalogService.java",
            "DecisionExplorerScenarioCatalogV1.java",
            "DecisionExplorerScenarioV1.java");
    private static final List<String> RETIRED_RESPONSE_FIELDS = List.of(
            "decisionReplaySnapshot",
            "decisionReplayReconstructionTrace",
            "decisionReplayCapsule",
            "decisionReplayReadinessChecklist",
            "decisionReplayEvidenceSourceMap");

    @Test
    void productionKeepsOnlyTheScenarioCatalogAndBoundarySurvivors() throws IOException {
        Set<String> decisionExplorerSources;
        Set<String> replaySources;
        try (var paths = Files.walk(API_SOURCE)) {
            List<Path> productionSources = paths
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList();
            decisionExplorerSources = productionSources.stream()
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("DecisionExplorer"))
                    .collect(Collectors.toSet());
            replaySources = productionSources.stream()
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("RoutingDecisionReplay"))
                    .collect(Collectors.toSet());
        }

        assertEquals(DECISION_EXPLORER_SURVIVORS, decisionExplorerSources);
        assertTrue(replaySources.isEmpty(), replaySources.toString());
        assertTrue(Files.exists(API_SOURCE.resolve("explain/RoutingExplanation.java")));
        assertTrue(Files.exists(API_SOURCE.resolve("explain/RoutingExplanationService.java")));
    }

    @Test
    void productionAndCurrentReviewerPagesDoNotReferenceTheRetiredReplayChain() throws IOException {
        String productionSource;
        try (var paths = Files.walk(API_SOURCE)) {
            productionSource = paths
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(DecisionExplorerCollapseAcceptanceTest::read)
                    .collect(Collectors.joining("\n"));
        }

        List<Path> currentPages = List.of(
                Path.of("src/main/resources/static/decision-explorer.html"),
                Path.of("src/main/resources/static/routing-demo.html"),
                Path.of("src/main/resources/static/load-balancing-cockpit.html"));
        RETIRED_RESPONSE_FIELDS.forEach(field -> {
            assertFalse(productionSource.contains(field), field);
            currentPages.forEach(page -> assertFalse(read(page).contains(field), page + ": " + field));
        });
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("could not read " + path, exception);
        }
    }
}
