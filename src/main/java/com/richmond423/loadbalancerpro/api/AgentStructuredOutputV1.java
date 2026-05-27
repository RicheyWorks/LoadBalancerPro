package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record AgentStructuredOutputV1(
        String schemaName,
        String schemaVersion,
        List<String> stableFieldNames,
        List<String> parseabilityRules,
        List<String> supportedQuestions,
        List<String> unsupportedActions,
        List<String> notProvenBoundaries,
        String boundaryNote) {
    public static final String SCHEMA_NAME = "AgentStructuredOutputV1";
    public static final String SCHEMA_VERSION = "v1";

    public AgentStructuredOutputV1 {
        schemaName = DecisionExplorerDtoSupport.valueOrDefault(schemaName, SCHEMA_NAME);
        schemaVersion = DecisionExplorerDtoSupport.valueOrDefault(schemaVersion, SCHEMA_VERSION);
        stableFieldNames = DecisionExplorerDtoSupport.copyOrEmpty(stableFieldNames);
        parseabilityRules = DecisionExplorerDtoSupport.copyOrEmpty(parseabilityRules);
        supportedQuestions = DecisionExplorerDtoSupport.copyOrEmpty(supportedQuestions);
        unsupportedActions = DecisionExplorerDtoSupport.copyOrEmpty(unsupportedActions);
        notProvenBoundaries = DecisionExplorerDtoSupport.copyOrEmpty(notProvenBoundaries);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }
}
