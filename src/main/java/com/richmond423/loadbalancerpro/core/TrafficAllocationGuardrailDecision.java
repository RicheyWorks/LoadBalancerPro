package com.richmond423.loadbalancerpro.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record TrafficAllocationGuardrailDecision(
        String contextId,
        AdaptiveRoutingPolicyMode mode,
        TrafficAllocationGuardrailAction action,
        boolean influenceAllowed,
        Map<String, Double> baselineAllocations,
        Map<String, Double> requestedAllocations,
        Map<String, Double> approvedAllocations,
        Map<String, Double> effectiveAllocations,
        boolean changed,
        List<String> reasons,
        String rollbackReason) {

    public TrafficAllocationGuardrailDecision {
        contextId = requireNonBlank(contextId, "contextId");
        mode = Objects.requireNonNull(mode, "mode cannot be null");
        action = Objects.requireNonNull(action, "action cannot be null");
        baselineAllocations = TrafficAllocationMaps.immutableNormalized(
                baselineAllocations, "baselineAllocations", false);
        requestedAllocations = TrafficAllocationMaps.immutableNormalized(
                requestedAllocations, "requestedAllocations", true);
        approvedAllocations = TrafficAllocationMaps.immutableNormalized(
                approvedAllocations, "approvedAllocations", action == TrafficAllocationGuardrailAction.DENY);
        effectiveAllocations = TrafficAllocationMaps.immutableNormalized(
                effectiveAllocations, "effectiveAllocations", false);
        reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons cannot be null"));
        if (reasons.isEmpty()) {
            throw new IllegalArgumentException("reasons cannot be empty");
        }
        rollbackReason = requireNonBlank(rollbackReason, "rollbackReason");

        if (action == TrafficAllocationGuardrailAction.DENY && !approvedAllocations.isEmpty()) {
            throw new IllegalArgumentException("denied allocation cannot contain approved allocations");
        }
        if ((mode == AdaptiveRoutingPolicyMode.OFF || mode == AdaptiveRoutingPolicyMode.OBSERVE)
                && action != TrafficAllocationGuardrailAction.DENY) {
            throw new IllegalArgumentException("off and observe modes must deny allocation approval");
        }
        if (action != TrafficAllocationGuardrailAction.DENY && approvedAllocations.isEmpty()) {
            throw new IllegalArgumentException("allowed or clamped allocation requires approved allocations");
        }
        if (action != TrafficAllocationGuardrailAction.DENY && requestedAllocations.isEmpty()) {
            throw new IllegalArgumentException("allowed or clamped allocation requires requested allocations");
        }
        if (influenceAllowed && (mode != AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT
                || action == TrafficAllocationGuardrailAction.DENY)) {
            throw new IllegalArgumentException("only an allowed active-experiment decision can permit influence");
        }
        Map<String, Double> expectedEffective = influenceAllowed ? approvedAllocations : baselineAllocations;
        if (!TrafficAllocationMaps.same(expectedEffective, effectiveAllocations)) {
            throw new IllegalArgumentException("effective allocation must match the authorized allocation");
        }
        if (changed != !TrafficAllocationMaps.same(baselineAllocations, effectiveAllocations)) {
            throw new IllegalArgumentException("changed must reflect baseline versus effective allocation");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
