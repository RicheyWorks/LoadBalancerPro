package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerPayloadV1(
        boolean readOnly,
        boolean simulationOnly,
        String payloadObject,
        String contractVersion,
        String source,
        String decisionId,
        DecisionReadoutV1 decisionReadout,
        CandidateReadoutV1 selectedCandidate,
        List<CandidateReadoutV1> candidateSet,
        List<FactorContributionV1> factorContributions,
        List<DecisionFactorDrilldownV1> factorDrilldowns,
        List<PolicyGateReadoutV1> policyGateReadouts,
        List<DecisionDiffReadoutV1> decisionDiffReadouts,
        List<EvidencePacketReadoutV1> evidencePacketReadouts,
        AgentStructuredOutputV1 agentStructuredOutput,
        List<String> warnings,
        List<String> unknowns,
        List<String> notProvenBoundaries,
        String boundaryNote) {
    public static final String PAYLOAD_OBJECT = "DecisionExplorerPayloadV1";
    public static final String CONTRACT_VERSION = "v1";
    public static final String READ_ONLY_BOUNDARY = "read-only";
    public static final String SIMULATION_ONLY_BOUNDARY = "simulation-only";

    public DecisionExplorerPayloadV1 {
        readOnly = true;
        simulationOnly = true;
        payloadObject = DecisionExplorerDtoSupport.valueOrDefault(payloadObject, PAYLOAD_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        source = DecisionExplorerDtoSupport.valueOrUnknown(source);
        decisionId = DecisionExplorerDtoSupport.valueOrUnknown(decisionId);
        candidateSet = DecisionExplorerDtoSupport.copyOrEmpty(candidateSet);
        factorContributions = DecisionExplorerDtoSupport.copyOrEmpty(factorContributions);
        factorDrilldowns = DecisionExplorerDtoSupport.copyOrEmpty(factorDrilldowns);
        policyGateReadouts = DecisionExplorerDtoSupport.copyOrEmpty(policyGateReadouts);
        decisionDiffReadouts = DecisionExplorerDtoSupport.copyOrEmpty(decisionDiffReadouts);
        evidencePacketReadouts = DecisionExplorerDtoSupport.copyOrEmpty(evidencePacketReadouts);
        warnings = DecisionExplorerDtoSupport.copyOrEmpty(warnings);
        unknowns = DecisionExplorerDtoSupport.copyOrEmpty(unknowns);
        notProvenBoundaries = DecisionExplorerDtoSupport.copyOrEmpty(notProvenBoundaries);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    public DecisionExplorerPayloadV1(
            boolean readOnly,
            boolean simulationOnly,
            String payloadObject,
            String contractVersion,
            String source,
            String decisionId,
            DecisionReadoutV1 decisionReadout,
            CandidateReadoutV1 selectedCandidate,
            List<CandidateReadoutV1> candidateSet,
            List<FactorContributionV1> factorContributions,
            List<PolicyGateReadoutV1> policyGateReadouts,
            List<DecisionDiffReadoutV1> decisionDiffReadouts,
            List<EvidencePacketReadoutV1> evidencePacketReadouts,
            AgentStructuredOutputV1 agentStructuredOutput,
            List<String> warnings,
            List<String> unknowns,
            List<String> notProvenBoundaries,
            String boundaryNote) {
        this(readOnly,
                simulationOnly,
                payloadObject,
                contractVersion,
                source,
                decisionId,
                decisionReadout,
                selectedCandidate,
                candidateSet,
                factorContributions,
                List.of(),
                policyGateReadouts,
                decisionDiffReadouts,
                evidencePacketReadouts,
                agentStructuredOutput,
                warnings,
                unknowns,
                notProvenBoundaries,
                boundaryNote);
    }
}
