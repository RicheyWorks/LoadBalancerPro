package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public final class AdaptiveRoutingPolicyEngine {

    public AdaptiveRoutingPolicyDecision decide(AdaptiveRoutingPolicyInput input) {
        if (input.mode() == AdaptiveRoutingPolicyMode.OFF) {
            return AdaptiveRoutingPolicyDecision.disabled(input.contextId(), input.baselineDecision());
        }

        if (input.mode() == AdaptiveRoutingPolicyMode.OBSERVE) {
            return blocked(input, "observe mode records inputs only", "observe mode cannot alter allocation",
                    "LASE recorded the decision inputs, but observe mode keeps the baseline final.");
        }

        if (input.failureReason() != null) {
            return blocked(input, "LASE evaluation failed safely", "fail-closed to baseline: "
                    + input.failureReason(), "LASE policy degraded to baseline because evaluation failed.");
        }

        if (input.mode() == AdaptiveRoutingPolicyMode.SHADOW) {
            return blocked(input, "shadow mode observes only", "shadow mode cannot alter allocation",
                    "LASE observed the decision path, but shadow mode keeps baseline final.");
        }

        if (input.mode() == AdaptiveRoutingPolicyMode.RECOMMEND) {
            return blocked(input, "recommend mode requires explicit operator acceptance",
                    "baseline retained until recommendation is accepted in a bounded experiment",
                    "LASE produced a recommendation for review; final allocation remains baseline.");
        }

        return activeExperiment(input);
    }

    public TrafficAllocationGuardrailDecision evaluateAllocation(
            TrafficAllocationGuardrailInput input,
            TrafficAllocationGuardrailPolicy policy) {
        Objects.requireNonNull(input, "input cannot be null");
        Objects.requireNonNull(policy, "policy cannot be null");
        Map<String, Double> requested = input.recommendation().allocations();
        List<String> reasons = new ArrayList<>();

        if (input.mode() == AdaptiveRoutingPolicyMode.OFF) {
            reasons.add("policy mode off");
        } else if (input.mode() == AdaptiveRoutingPolicyMode.OBSERVE) {
            reasons.add("observe mode records allocation inputs only");
        }
        if (input.recommendation().fallbackApplied() || requested.isEmpty()) {
            reasons.add("allocation recommendation is in safe fallback");
        }
        if (!input.signalsFresh()) {
            reasons.add("stale allocation signals");
        }
        if (!input.evidenceSufficient()) {
            reasons.add("insufficient allocation evidence");
        }
        if (input.conflictingSignals()) {
            reasons.add("conflicting allocation signals");
        }
        if (input.cooldownActive()) {
            reasons.add("allocation cooldown is active");
        }
        if (input.operatorStopRequested()) {
            reasons.add("operator stop requested");
        }
        if (input.mode() == AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT
                && !input.explicitExperimentContext()) {
            reasons.add("active-experiment requires explicit bounded experiment context");
        }
        if (!requested.isEmpty() && TrafficAllocationMaps.hasPositiveShareMissingFrom(
                input.baselineAllocations(), requested.keySet())) {
            reasons.add("candidate omits a backend with positive baseline allocation");
        }
        if (!reasons.isEmpty()) {
            return deniedAllocation(input, reasons);
        }

        Optional<Map<String, Double>> capped = capBackendShares(requested, policy.maximumBackendShare());
        if (capped.isEmpty()) {
            return deniedAllocation(input, List.of(
                    "maximum backend share is infeasible for the candidate backend count"));
        }

        Map<String, Double> approved = capped.get();
        boolean clamped = !TrafficAllocationMaps.same(requested, approved);
        if (clamped) {
            reasons.add("candidate clamped to maximum backend share");
        }

        double movement = TrafficAllocationMaps.totalVariation(input.baselineAllocations(), approved);
        if (movement > policy.maximumTotalShareMovement() + TrafficAllocationMaps.EPSILON) {
            double factor = policy.maximumTotalShareMovement() == 0.0
                    ? 0.0
                    : policy.maximumTotalShareMovement() / movement;
            approved = TrafficAllocationMaps.interpolate(input.baselineAllocations(), approved, factor);
            clamped = true;
            reasons.add("candidate clamped to maximum total share movement");
        }
        if (approved.values().stream()
                .anyMatch(share -> share > policy.maximumBackendShare() + TrafficAllocationMaps.EPSILON)) {
            return deniedAllocation(input, List.of(
                    "share-movement limit cannot bring allocation within maximum backend share"));
        }

        TrafficAllocationGuardrailAction action = clamped
                ? TrafficAllocationGuardrailAction.CLAMP
                : TrafficAllocationGuardrailAction.ALLOW;
        if (!clamped) {
            reasons.add("allocation passed all configured guardrails");
        }
        boolean influenceAllowed = input.mode() == AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT;
        if (input.mode() == AdaptiveRoutingPolicyMode.SHADOW) {
            reasons.add("shadow mode retains baseline while recording the approved candidate");
        } else if (input.mode() == AdaptiveRoutingPolicyMode.RECOMMEND) {
            reasons.add("recommend mode retains baseline until explicit bounded experiment review");
        } else if (influenceAllowed) {
            reasons.add("active-experiment allocation decision passed; no traffic action is performed here");
        }
        Map<String, Double> effective = influenceAllowed ? approved : input.baselineAllocations();
        boolean changed = !TrafficAllocationMaps.same(input.baselineAllocations(), effective);
        String rollbackReason = influenceAllowed
                ? "baseline allocation remains the rollback target for the bounded experiment"
                : "baseline allocation retained because the selected mode does not permit influence";
        return new TrafficAllocationGuardrailDecision(
                input.contextId(),
                input.mode(),
                action,
                influenceAllowed,
                input.baselineAllocations(),
                requested,
                approved,
                effective,
                changed,
                reasons,
                rollbackReason);
    }

    private AdaptiveRoutingPolicyDecision activeExperiment(AdaptiveRoutingPolicyInput input) {
        List<String> guardrails = new ArrayList<>();
        if (!input.explicitExperimentContext()) {
            guardrails.add("active-experiment requires explicit bounded experiment context");
        }
        if (!input.recommendationRecorded() || input.recommendedDecision() == null) {
            guardrails.add("no LASE backend recommendation");
        }
        if (input.servers().stream().noneMatch(Server::isHealthy)) {
            guardrails.add("all backends unhealthy");
        }
        if (!input.signalsFresh()) {
            guardrails.add("stale signal");
        }
        if (input.conflictingSignals()) {
            guardrails.add("conflicting signal");
        }
        Optional<Server> recommendedServer = recommendedServer(input);
        if (recommendedServer.isEmpty() && input.recommendedDecision() != null) {
            guardrails.add("recommended backend not eligible");
        } else if (recommendedServer.isPresent() && !recommendedServer.get().isHealthy()) {
            guardrails.add("recommended backend unhealthy");
        } else if (recommendedServer.isPresent() && recommendedServer.get().getCapacity() <= 0.0) {
            guardrails.add("recommended backend has no available capacity");
        }
        if (input.requestedLoad() > totalHealthyCapacity(input.servers())) {
            guardrails.add("capacity constraints failed");
        }

        if (!guardrails.isEmpty()) {
            return blocked(input, guardrails, "baseline retained by active-experiment guardrail",
                    "Active experiment refused influence and kept the baseline allocation.");
        }

        if (input.recommendedDecision().equals(input.baselineDecision())) {
            return new AdaptiveRoutingPolicyDecision(
                    input.contextId(),
                    input.mode().wireValue(),
                    true,
                    true,
                    input.baselineDecision(),
                    input.recommendedDecision(),
                    input.recommendedAction(),
                    input.baselineDecision(),
                    false,
                    List.of("baseline already matches LASE recommendation"),
                    "no rollback required because final decision matches baseline",
                    "Active experiment passed policy gates, but LASE agreed with baseline.",
                    auditFields(input, false, input.baselineDecision(), "baseline already matches recommendation"));
        }

        return new AdaptiveRoutingPolicyDecision(
                input.contextId(),
                input.mode().wireValue(),
                true,
                true,
                input.baselineDecision(),
                input.recommendedDecision(),
                input.recommendedAction(),
                input.recommendedDecision(),
                true,
                List.of("all active-experiment policy gates passed"),
                "operator can return policy mode to off, shadow, or recommend to restore baseline behavior",
                "Active experiment passed health, eligibility, capacity, freshness, conflict, and context gates.",
                auditFields(input, true, input.recommendedDecision(), "policy gates passed"));
    }

    private AdaptiveRoutingPolicyDecision blocked(
            AdaptiveRoutingPolicyInput input,
            String guardrail,
            String rollbackReason,
            String explanationSummary) {
        return blocked(input, List.of(guardrail), rollbackReason, explanationSummary);
    }

    private AdaptiveRoutingPolicyDecision blocked(
            AdaptiveRoutingPolicyInput input,
            List<String> guardrails,
            String rollbackReason,
            String explanationSummary) {
        return new AdaptiveRoutingPolicyDecision(
                input.contextId(),
                input.mode().wireValue(),
                false,
                input.recommendationRecorded() && input.recommendedDecision() != null,
                input.baselineDecision(),
                input.recommendedDecision(),
                input.recommendedAction(),
                input.baselineDecision(),
                false,
                guardrails,
                rollbackReason,
                explanationSummary,
                auditFields(input, false, input.baselineDecision(), String.join("; ", guardrails)));
    }

    private Optional<Server> recommendedServer(AdaptiveRoutingPolicyInput input) {
        if (input.recommendedDecision() == null) {
            return Optional.empty();
        }
        return input.servers().stream()
                .filter(server -> input.recommendedDecision().equals(server.getServerId()))
                .findFirst();
    }

    private static TrafficAllocationGuardrailDecision deniedAllocation(
            TrafficAllocationGuardrailInput input,
            List<String> reasons) {
        return new TrafficAllocationGuardrailDecision(
                input.contextId(),
                input.mode(),
                TrafficAllocationGuardrailAction.DENY,
                false,
                input.baselineAllocations(),
                input.recommendation().allocations(),
                Map.of(),
                input.baselineAllocations(),
                false,
                reasons,
                "baseline allocation retained by allocation guardrail");
    }

    private static Optional<Map<String, Double>> capBackendShares(
            Map<String, Double> requested,
            double maximumBackendShare) {
        if (requested.size() * maximumBackendShare < 1.0 - TrafficAllocationMaps.EPSILON) {
            return Optional.empty();
        }
        Map<String, Double> capped = new TreeMap<>();
        double excess = 0.0;
        for (Map.Entry<String, Double> entry : requested.entrySet()) {
            double share = Math.min(entry.getValue(), maximumBackendShare);
            capped.put(entry.getKey(), share);
            excess += entry.getValue() - share;
        }
        if (excess > TrafficAllocationMaps.EPSILON) {
            double room = capped.values().stream()
                    .mapToDouble(share -> maximumBackendShare - share)
                    .sum();
            if (room < excess - TrafficAllocationMaps.EPSILON) {
                return Optional.empty();
            }
            for (String serverId : capped.keySet()) {
                double available = maximumBackendShare - capped.get(serverId);
                capped.put(serverId, capped.get(serverId) + (excess * available / room));
            }
        }
        return Optional.of(TrafficAllocationMaps.immutableNormalized(capped, "cappedAllocations", false));
    }

    private static double totalHealthyCapacity(List<Server> servers) {
        return servers.stream()
                .filter(Server::isHealthy)
                .mapToDouble(server -> Math.max(0.0, server.getCapacity()))
                .sum();
    }

    private static Map<String, String> auditFields(
            AdaptiveRoutingPolicyInput input,
            boolean changed,
            String finalDecision,
            String guardrail) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("policyMode", input.mode().wireValue());
        fields.put("contextId", input.contextId());
        fields.put("baselineDecision", display(input.baselineDecision()));
        fields.put("recommendedDecision", display(input.recommendedDecision()));
        fields.put("finalDecision", display(finalDecision));
        fields.put("changed", Boolean.toString(changed));
        fields.put("guardrail", guardrail);
        return fields;
    }

    private static String display(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }
}
