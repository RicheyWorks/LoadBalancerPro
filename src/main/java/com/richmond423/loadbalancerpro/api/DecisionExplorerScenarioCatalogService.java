package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentFixtureCatalog;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentScenario;
import org.springframework.stereotype.Service;

@Service
public class DecisionExplorerScenarioCatalogService {
    private static final String SOURCE =
            "AdaptiveRoutingExperimentFixtureCatalog local synthetic fixtures for Decision Explorer Phase 2";
    private static final String BOUNDARY_NOTE =
            "Scenario catalog API is read-only and simulation-only; it returns deterministic local synthetic "
                    + "reviewer metadata and does not run routing, shift traffic, persist storage, execute replay, "
                    + "export evidence, call external systems, or claim production readiness.";
    private static final List<String> NOT_PROVEN_BOUNDARIES = List.of(
            "no production readiness",
            "no production certification",
            "no live-cloud validation",
            "no real-tenant validation",
            "no benchmark/load/stress proof",
            "no throughput/p95/p99 proof",
            "no replay/export proof",
            "no storage proof",
            "no evidence-packet generation",
            "no autonomous production action");

    private final AdaptiveRoutingExperimentFixtureCatalog fixtureCatalog;

    public DecisionExplorerScenarioCatalogService() {
        this(new AdaptiveRoutingExperimentFixtureCatalog());
    }

    DecisionExplorerScenarioCatalogService(AdaptiveRoutingExperimentFixtureCatalog fixtureCatalog) {
        this.fixtureCatalog = fixtureCatalog;
    }

    public DecisionExplorerScenarioCatalogV1 buildCatalog() {
        List<AdaptiveRoutingExperimentScenario> fixtures = fixtureCatalog.createAll();
        List<DecisionExplorerScenarioV1> scenarios = new ArrayList<>();
        for (int index = 0; index < fixtures.size(); index++) {
            scenarios.add(toScenario(fixtures.get(index), (index + 1) * 10));
        }
        return new DecisionExplorerScenarioCatalogV1(
                true,
                true,
                DecisionExplorerScenarioCatalogV1.PAYLOAD_OBJECT,
                DecisionExplorerScenarioCatalogV1.CONTRACT_VERSION,
                SOURCE,
                scenarios,
                List.of("Scenario catalog entries are deterministic local synthetic review presets only."),
                List.of("hidden routing internals", "live-cloud behavior", "real-tenant behavior"),
                NOT_PROVEN_BOUNDARIES,
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerScenarioV1 toScenario(
            AdaptiveRoutingExperimentScenario fixture,
            int displayOrder) {
        ScenarioClassification classification = classify(fixture);
        return new DecisionExplorerScenarioV1(
                DecisionExplorerScenarioV1.SCENARIO_OBJECT,
                fixture.name(),
                title(fixture.name()),
                classification.category(),
                classification.evidenceStatus(),
                displayOrder,
                fixture.description(),
                fixture.name(),
                sourceReferences(fixture),
                reviewerQuestions(classification),
                tags(fixture, classification),
                warnings(fixture, classification),
                unknowns(fixture, classification),
                NOT_PROVEN_BOUNDARIES,
                BOUNDARY_NOTE);
    }

    private static ScenarioClassification classify(AdaptiveRoutingExperimentScenario fixture) {
        return switch (fixture.name()) {
            case "normal-balanced-load" -> new ScenarioClassification("HEALTHY_BASELINE", "AVAILABLE");
            case "stale-signal" -> new ScenarioClassification("PARTIAL_EVIDENCE", "PARTIAL");
            case "all-unhealthy-degradation" -> new ScenarioClassification("NO_HEALTHY_SERVER", "UNKNOWN");
            default -> new ScenarioClassification("LOCAL_SYNTHETIC", "AVAILABLE");
        };
    }

    private static List<String> sourceReferences(AdaptiveRoutingExperimentScenario fixture) {
        return List.of(
                "AdaptiveRoutingExperimentFixtureCatalog:" + fixture.name(),
                "DecisionExplorerPayloadV1",
                "POST /api/routing/decision-explorer");
    }

    private static List<String> reviewerQuestions(ScenarioClassification classification) {
        return switch (classification.category()) {
            case "HEALTHY_BASELINE" -> List.of(
                    "Which route would the reviewer inspect first?",
                    "Which deterministic evidence explains the selected route?");
            case "PARTIAL_EVIDENCE" -> List.of(
                    "Which evidence is stale or incomplete?",
                    "Which warnings should remain visible before a reviewer trusts the summary?");
            case "NO_HEALTHY_SERVER" -> List.of(
                    "What remains unknown when no healthy route is available?",
                    "Which fallback or no-selection boundary should the reviewer preserve?");
            default -> List.of(
                    "Which local synthetic evidence is visible?",
                    "Which boundaries remain not proven?");
        };
    }

    private static List<String> tags(
            AdaptiveRoutingExperimentScenario fixture,
            ScenarioClassification classification) {
        return List.of(
                normalizedTag(classification.category()),
                normalizedTag(classification.evidenceStatus()),
                normalizedTag(fixture.expectedPressure()));
    }

    private static List<String> warnings(
            AdaptiveRoutingExperimentScenario fixture,
            ScenarioClassification classification) {
        List<String> warnings = new ArrayList<>();
        if ("PARTIAL".equals(classification.evidenceStatus())) {
            warnings.add("stale or partial evidence must stay visible");
        }
        if ("UNKNOWN".equals(classification.evidenceStatus())) {
            warnings.add("no healthy selected route is invented");
        }
        if (fixture.requestedLoad() > healthyCapacity(fixture)) {
            warnings.add("requested load exceeds visible healthy capacity");
        }
        return List.copyOf(warnings);
    }

    private static List<String> unknowns(
            AdaptiveRoutingExperimentScenario fixture,
            ScenarioClassification classification) {
        List<String> unknowns = new ArrayList<>();
        unknowns.add("hidden routing internals");
        if ("PARTIAL".equals(classification.evidenceStatus())) {
            unknowns.add("freshness of hidden routing internals");
        }
        if ("UNKNOWN".equals(classification.evidenceStatus())) {
            unknowns.add("selected route is unavailable when no healthy server is returned");
        }
        return List.copyOf(unknowns);
    }

    private static double healthyCapacity(AdaptiveRoutingExperimentScenario fixture) {
        return fixture.servers().stream()
                .filter(server -> server.healthy())
                .mapToDouble(server -> server.capacity())
                .sum();
    }

    private static String title(String value) {
        String[] words = value.split("-");
        List<String> titled = new ArrayList<>();
        for (String word : words) {
            if (!word.isBlank()) {
                titled.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
            }
        }
        return String.join(" ", titled);
    }

    private static String normalizedTag(String value) {
        return value.toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
    }

    private record ScenarioClassification(String category, String evidenceStatus) {
    }
}
