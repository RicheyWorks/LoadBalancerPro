package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentFixtureCatalog;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentScenario;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EnterpriseLabScenarioCatalogService {
    public static final String FIXTURE_VERSION = "adaptive-routing-fixtures-v1";
    private static final List<String> SUPPORTED_MODES = List.of("shadow", "influence", "all");

    private final List<AdaptiveRoutingExperimentScenario> scenarios;
    private final Map<String, AdaptiveRoutingExperimentScenario> scenariosById;
    private final Map<String, EnterpriseLabScenarioMetadata> metadataById;

    public EnterpriseLabScenarioCatalogService() {
        this(new AdaptiveRoutingExperimentFixtureCatalog());
    }

    EnterpriseLabScenarioCatalogService(AdaptiveRoutingExperimentFixtureCatalog fixtureCatalog) {
        this.scenarios = List.copyOf(fixtureCatalog.createAll());
        LinkedHashMap<String, AdaptiveRoutingExperimentScenario> scenarioMap = new LinkedHashMap<>();
        LinkedHashMap<String, EnterpriseLabScenarioMetadata> metadataMap = new LinkedHashMap<>();
        for (AdaptiveRoutingExperimentScenario scenario : scenarios) {
            String scenarioId = scenario.name();
            if (scenarioMap.putIfAbsent(scenarioId, scenario) != null) {
                throw new IllegalStateException("Duplicate enterprise lab scenario id: " + scenarioId);
            }
            metadataMap.put(scenarioId, metadata(scenario));
        }
        this.scenariosById = Map.copyOf(scenarioMap);
        this.metadataById = Map.copyOf(metadataMap);
    }

    public List<EnterpriseLabScenarioMetadata> listScenarioMetadata() {
        return scenarios.stream()
                .map(scenario -> metadataById.get(scenario.name()))
                .toList();
    }

    public Optional<EnterpriseLabScenarioMetadata> findScenarioMetadata(String scenarioId) {
        return Optional.ofNullable(metadataById.get(normalizeId(scenarioId)));
    }

    public Optional<AdaptiveRoutingExperimentScenario> findScenario(String scenarioId) {
        return Optional.ofNullable(scenariosById.get(normalizeId(scenarioId)));
    }

    public List<AdaptiveRoutingExperimentScenario> resolveScenarios(List<String> requestedScenarioIds, int maxScenarios) {
        List<String> selectedIds = requestedScenarioIds == null || requestedScenarioIds.isEmpty()
                ? scenarios.stream().map(AdaptiveRoutingExperimentScenario::name).toList()
                : requestedScenarioIds.stream().map(EnterpriseLabScenarioCatalogService::normalizeId).toList();
        if (selectedIds.size() > maxScenarios) {
            throw new IllegalArgumentException("At most " + maxScenarios + " scenarios can be selected per lab run");
        }
        return selectedIds.stream()
                .map(id -> findScenario(id)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown enterprise lab scenario: " + id)))
                .toList();
    }

    private static EnterpriseLabScenarioMetadata metadata(AdaptiveRoutingExperimentScenario scenario) {
        String category = category(scenario.expectedPressure());
        return new EnterpriseLabScenarioMetadata(
                scenario.name(),
                displayName(scenario.name()),
                category,
                scenario.description(),
                signals(scenario),
                guardrails(scenario),
                SUPPORTED_MODES,
                safeForInfluenceExperiment(scenario),
                FIXTURE_VERSION,
                scenario.strategy(),
                scenario.requestedLoad(),
                scenario.servers().size(),
                scenario.replayEventOrder().size());
    }

    private static List<String> signals(AdaptiveRoutingExperimentScenario scenario) {
        return scenario.replayEventOrder().stream()
                .map(event -> event.replace("-observation", "").replace("-1", ""))
                .distinct()
                .toList();
    }

    private static List<String> guardrails(AdaptiveRoutingExperimentScenario scenario) {
        String pressure = scenario.expectedPressure().toLowerCase(Locale.ROOT);
        if (pressure.contains("stale")) {
            return List.of("stale signals block active influence", "baseline allocation remains available");
        }
        if (pressure.contains("all unhealthy")) {
            return List.of("all-unhealthy scenarios fail closed", "no live cloud mutation is executed");
        }
        if (pressure.contains("conflicting")) {
            return List.of("conflicting signals require explanation", "influence remains experiment-only");
        }
        if (pressure.contains("zero")) {
            return List.of("zero or edge metrics remain deterministic", "no-op allocation is allowed");
        }
        return List.of("influence is opt-in and experiment-only", "default allocation behavior is unchanged");
    }

    private static boolean safeForInfluenceExperiment(AdaptiveRoutingExperimentScenario scenario) {
        String pressure = scenario.expectedPressure().toLowerCase(Locale.ROOT);
        return scenario.signalsFresh() && !pressure.contains("all unhealthy");
    }

    private static String category(String expectedPressure) {
        String normalized = expectedPressure.toLowerCase(Locale.ROOT);
        if (normalized.contains("normal")) {
            return "normal";
        }
        if (normalized.contains("tail")) {
            return "latency";
        }
        if (normalized.contains("overload") || normalized.contains("capacity")) {
            return "capacity-pressure";
        }
        if (normalized.contains("stale") || normalized.contains("conflicting")) {
            return "signal-quality";
        }
        if (normalized.contains("unhealthy") || normalized.contains("recovery")) {
            return "resilience";
        }
        if (normalized.contains("zero")) {
            return "edge-values";
        }
        return "adaptive-routing";
    }

    private static String displayName(String scenarioId) {
        String[] parts = scenarioId.split("-");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private static String normalizeId(String scenarioId) {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId cannot be null or blank");
        }
        return scenarioId.trim().toLowerCase(Locale.ROOT);
    }
}

