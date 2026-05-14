package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AdaptiveRoutingPolicyEngine {

    public AdaptiveRoutingPolicyDecision decide(AdaptiveRoutingPolicyInput input) {
        if (input.mode() == AdaptiveRoutingPolicyMode.OFF) {
            return AdaptiveRoutingPolicyDecision.disabled(input.contextId(), input.baselineDecision());
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
