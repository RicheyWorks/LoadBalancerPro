package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerScenarioV1(
        String scenarioObject,
        String scenarioId,
        String scenarioLabel,
        String scenarioCategory,
        String evidenceStatus,
        int displayOrder,
        String description,
        String requestPresetId,
        List<String> sourceReferenceIds,
        List<String> expectedReviewerQuestions,
        List<String> tags,
        List<String> warnings,
        List<String> unknowns,
        List<String> notProvenBoundaries,
        String boundaryNote) {
    public static final String SCENARIO_OBJECT = "DecisionExplorerScenarioV1";

    public DecisionExplorerScenarioV1 {
        scenarioObject = DecisionExplorerDtoSupport.valueOrDefault(scenarioObject, SCENARIO_OBJECT);
        scenarioId = DecisionExplorerDtoSupport.valueOrUnknown(scenarioId);
        scenarioLabel = DecisionExplorerDtoSupport.valueOrUnknown(scenarioLabel);
        scenarioCategory = DecisionExplorerDtoSupport.valueOrUnknown(scenarioCategory);
        evidenceStatus = DecisionExplorerDtoSupport.valueOrUnknown(evidenceStatus);
        displayOrder = Math.max(0, displayOrder);
        description = DecisionExplorerDtoSupport.valueOrUnknown(description);
        requestPresetId = DecisionExplorerDtoSupport.valueOrUnknown(requestPresetId);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        expectedReviewerQuestions = DecisionExplorerDtoSupport.copyOrEmpty(expectedReviewerQuestions);
        tags = DecisionExplorerDtoSupport.copyOrEmpty(tags);
        warnings = DecisionExplorerDtoSupport.copyOrEmpty(warnings);
        unknowns = DecisionExplorerDtoSupport.copyOrEmpty(unknowns);
        notProvenBoundaries = DecisionExplorerDtoSupport.copyOrEmpty(notProvenBoundaries);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }
}
