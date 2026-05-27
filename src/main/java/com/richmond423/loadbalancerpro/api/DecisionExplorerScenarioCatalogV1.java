package com.richmond423.loadbalancerpro.api;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record DecisionExplorerScenarioCatalogV1(
        boolean readOnly,
        boolean simulationOnly,
        String payloadObject,
        String contractVersion,
        String source,
        List<DecisionExplorerScenarioV1> scenarios,
        List<String> warnings,
        List<String> unknowns,
        List<String> notProvenBoundaries,
        String boundaryNote) {
    public static final String PAYLOAD_OBJECT = "DecisionExplorerScenarioCatalogV1";
    public static final String CONTRACT_VERSION = "v1";

    private static final Comparator<DecisionExplorerScenarioV1> BY_ORDER_THEN_ID = Comparator
            .comparingInt(DecisionExplorerScenarioV1::displayOrder)
            .thenComparing(DecisionExplorerScenarioV1::scenarioId);

    public DecisionExplorerScenarioCatalogV1 {
        readOnly = true;
        simulationOnly = true;
        payloadObject = DecisionExplorerDtoSupport.valueOrDefault(payloadObject, PAYLOAD_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        source = DecisionExplorerDtoSupport.valueOrUnknown(source);
        scenarios = DecisionExplorerDtoSupport.copyOrEmpty(scenarios).stream()
                .filter(Objects::nonNull)
                .sorted(BY_ORDER_THEN_ID)
                .toList();
        warnings = DecisionExplorerDtoSupport.copyOrEmpty(warnings);
        unknowns = DecisionExplorerDtoSupport.copyOrEmpty(unknowns);
        notProvenBoundaries = DecisionExplorerDtoSupport.copyOrEmpty(notProvenBoundaries);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }
}
