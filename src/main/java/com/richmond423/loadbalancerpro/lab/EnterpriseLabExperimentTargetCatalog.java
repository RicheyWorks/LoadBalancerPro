package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentScenario;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Immutable repository-controlled bindings for Enterprise Lab loopback experiments.
 * Operator API payloads cannot add or replace target addresses.
 */
public final class EnterpriseLabExperimentTargetCatalog {
    public static final int MAX_BOUND_SCENARIOS = 16;

    private final Map<String, List<EnterpriseLabLoopbackTarget>> targetsByScenario;

    public EnterpriseLabExperimentTargetCatalog(Collection<EnterpriseLabLoopbackTarget> targets) {
        Objects.requireNonNull(targets, "targets cannot be null");
        Map<String, List<EnterpriseLabLoopbackTarget>> grouped = new TreeMap<>();
        for (EnterpriseLabLoopbackTarget target : targets) {
            EnterpriseLabLoopbackTarget safeTarget = Objects.requireNonNull(
                    target, "targets cannot contain null");
            grouped.computeIfAbsent(safeTarget.scenarioId(), ignored -> new ArrayList<>()).add(safeTarget);
        }
        if (grouped.size() > MAX_BOUND_SCENARIOS) {
            throw new IllegalArgumentException("target catalog cannot bind more than 16 scenarios");
        }

        EnterpriseLabScenarioCatalogService scenarioCatalog = new EnterpriseLabScenarioCatalogService();
        Map<String, List<EnterpriseLabLoopbackTarget>> validated = new LinkedHashMap<>();
        grouped.forEach((scenarioId, scenarioTargets) -> {
            AdaptiveRoutingExperimentScenario scenario = scenarioCatalog.findScenario(scenarioId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "target catalog scenario is not repository-approved: " + scenarioId));
            validateBackendSet(scenario, scenarioTargets);
            List<EnterpriseLabLoopbackTarget> sorted = scenarioTargets.stream()
                    .sorted(java.util.Comparator.comparing(EnterpriseLabLoopbackTarget::backendId))
                    .toList();
            validated.put(scenarioId, List.copyOf(sorted));
        });
        this.targetsByScenario = Collections.unmodifiableMap(validated);
    }

    public static EnterpriseLabExperimentTargetCatalog empty() {
        return new EnterpriseLabExperimentTargetCatalog(List.of());
    }

    public Optional<List<EnterpriseLabLoopbackTarget>> findTargets(String scenarioId) {
        if (scenarioId == null || scenarioId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(targetsByScenario.get(scenarioId.trim().toLowerCase(java.util.Locale.ROOT)));
    }

    public List<String> boundScenarioIds() {
        return List.copyOf(targetsByScenario.keySet());
    }

    public int size() {
        return targetsByScenario.size();
    }

    private static void validateBackendSet(
            AdaptiveRoutingExperimentScenario scenario,
            List<EnterpriseLabLoopbackTarget> targets) {
        Set<String> expected = new TreeSet<>();
        scenario.servers().forEach(server -> expected.add(server.id()));
        Set<String> actual = new TreeSet<>();
        for (EnterpriseLabLoopbackTarget target : targets) {
            if (!actual.add(target.backendId())) {
                throw new IllegalArgumentException(
                        "target catalog backend identities must be unique per scenario");
            }
        }
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException(
                    "target catalog must bind the exact repository-approved backend set for " + scenario.name());
        }
    }
}
