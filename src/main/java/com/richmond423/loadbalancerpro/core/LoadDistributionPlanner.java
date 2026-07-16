package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public final class LoadDistributionPlanner {
    private static final double EPSILON = 0.000000001;

    private LoadDistributionPlanner() {
    }

    static Map<String, Double> roundRobin(List<Server> healthyServers, double totalData) {
        Map<String, Double> distribution = new HashMap<>();
        double dataPerServer = totalData / healthyServers.size();
        for (Server server : healthyServers) {
            distribution.put(server.getServerId(), dataPerServer);
        }
        return distribution;
    }

    static Map<String, Double> leastLoaded(List<Server> healthyServers, double totalData) {
        Map<String, Double> distribution = new HashMap<>();
        List<Server> sorted = healthyServers.stream()
                .sorted(Comparator.comparingDouble(Server::getLoadScore))
                .toList();
        double remaining = totalData;
        for (Server server : sorted) {
            double allocation = Math.min(remaining, totalData / sorted.size());
            distribution.put(server.getServerId(), allocation);
            remaining -= allocation;
            if (remaining <= 0) {
                break;
            }
        }
        return distribution;
    }

    static Map<String, Double> weighted(List<Server> healthyServers, double totalData) {
        Map<String, Double> distribution = new HashMap<>();
        double totalWeight = healthyServers.stream().mapToDouble(Server::getWeight).sum();
        if (totalWeight <= 0) {
            return roundRobin(healthyServers, totalData);
        }
        for (Server server : healthyServers) {
            double allocation = (server.getWeight() / totalWeight) * totalData;
            distribution.put(server.getServerId(), allocation);
        }
        return distribution;
    }

    static Map<String, Double> capacityAware(List<Server> healthyServers, double totalData) {
        return capacityAwareResult(healthyServers, totalData).allocations();
    }

    static LoadDistributionResult capacityAwareResult(List<Server> healthyServers, double totalData) {
        Map<String, Double> distribution = new LinkedHashMap<>();
        List<Server> sorted = healthyServers.stream()
                .filter(s -> availableCapacity(s) > 0)
                .sorted(Comparator.comparingDouble((Server s) -> s.getLoadScore() / s.getCapacity())
                        .thenComparing(Server::getServerId))
                .toList();
        double totalCapacity = sorted.stream()
                .mapToDouble(LoadDistributionPlanner::availableCapacity)
                .sum();
        double remaining = totalData;
        for (Server server : sorted) {
            double availableCapacity = availableCapacity(server);
            double proportionalAllocation = (availableCapacity / totalCapacity) * totalData;
            double allocation = Math.min(Math.min(remaining, proportionalAllocation), availableCapacity);
            distribution.put(server.getServerId(), allocation);
            remaining -= allocation;
            if (remaining <= 0) {
                break;
            }
        }
        return new LoadDistributionResult(distribution, remaining);
    }

    static Map<String, Double> predictive(List<Server> healthyServers, double totalData,
                                          Map<String, Double> predictedLoads) {
        return predictiveResult(healthyServers, totalData, predictedLoads).allocations();
    }

    static LoadDistributionResult predictiveResult(List<Server> healthyServers, double totalData,
                                                   Map<String, Double> predictedLoads) {
        Map<String, Double> distribution = new LinkedHashMap<>();
        List<Server> sorted = healthyServers.stream()
                .filter(s -> predictedAvailableCapacity(s, predictedLoads) > 0)
                .sorted(Comparator.comparingDouble((Server s) -> -predictedAvailableCapacity(s, predictedLoads))
                        .thenComparing(Server::getServerId))
                .toList();
        double totalPredictedCapacity = sorted.stream()
                .mapToDouble(s -> predictedAvailableCapacity(s, predictedLoads))
                .sum();
        double remaining = totalData;
        for (Server server : sorted) {
            double availableCapacity = predictedAvailableCapacity(server, predictedLoads);
            double proportionalAllocation = (availableCapacity / totalPredictedCapacity) * totalData;
            double allocation = Math.min(Math.min(remaining, proportionalAllocation), availableCapacity);
            distribution.put(server.getServerId(), allocation);
            remaining -= allocation;
            if (remaining <= 0) {
                break;
            }
        }
        return new LoadDistributionResult(distribution, remaining);
    }

    public static TrafficAllocationRecommendation recommendTrafficShares(
            List<ServerStateVector> candidates,
            Map<String, ServerScoreBreakdown> scores,
            Map<String, Double> previousAllocations,
            TrafficAllocationPolicy policy) {
        Objects.requireNonNull(candidates, "candidates cannot be null");
        Objects.requireNonNull(scores, "scores cannot be null");
        Objects.requireNonNull(previousAllocations, "previousAllocations cannot be null");
        Objects.requireNonNull(policy, "policy cannot be null");
        validatePreviousAllocations(previousAllocations);

        List<String> reasons = new ArrayList<>();
        Map<String, ServerStateVector> uniqueCandidates = uniqueCandidates(candidates);
        Map<String, ServerScoreBreakdown> eligibleScores = new TreeMap<>();
        for (Map.Entry<String, ServerStateVector> entry : uniqueCandidates.entrySet()) {
            String serverId = entry.getKey();
            ServerStateVector candidate = entry.getValue();
            if (!candidate.healthy()) {
                reasons.add("excluded ineligible backend " + serverId);
                continue;
            }
            ServerScoreBreakdown breakdown = scores.get(serverId);
            if (breakdown == null) {
                reasons.add("excluded backend without score " + serverId);
                continue;
            }
            if (!serverId.equals(breakdown.serverId())) {
                throw new IllegalArgumentException("score key must match score serverId");
            }
            if (breakdown.totalScore() < 0.0) {
                throw new IllegalArgumentException("recommendation scores must be non-negative");
            }
            eligibleScores.put(serverId, breakdown);
        }

        if (eligibleScores.isEmpty()) {
            reasons.add("no eligible scored backends; no allocation recommended");
            return TrafficAllocationRecommendation.noAllocation(Map.of(), reasons);
        }

        Map<String, Double> rawAllocations = scoreDerivedAllocations(eligibleScores);
        reasons.add("score-derived utilities normalized across " + eligibleScores.size() + " eligible backends");
        if (hasPositiveIneligiblePreviousShare(previousAllocations, eligibleScores.keySet())) {
            reasons.add("previous allocation contains an ineligible backend; rate-limited redistribution is unsafe");
            return TrafficAllocationRecommendation.noAllocation(rawAllocations, reasons);
        }

        Map<String, Double> lowerBounds = new TreeMap<>();
        Map<String, Double> upperBounds = new TreeMap<>();
        boolean hasPreviousAllocation = !previousAllocations.isEmpty();
        for (String serverId : eligibleScores.keySet()) {
            double lower = policy.minimumBackendShare();
            double upper = policy.maximumBackendShare();
            if (hasPreviousAllocation) {
                double previous = previousAllocations.getOrDefault(serverId, 0.0);
                lower = Math.max(lower, previous - policy.maximumShareChangePerDecision());
                upper = Math.min(upper, previous + policy.maximumShareChangePerDecision());
            }
            lowerBounds.put(serverId, clampUnit(lower));
            upperBounds.put(serverId, clampUnit(upper));
        }

        if (!constraintsFeasible(lowerBounds, upperBounds)) {
            reasons.add("allocation bounds and share-change limits are infeasible; no allocation recommended");
            return TrafficAllocationRecommendation.noAllocation(rawAllocations, reasons);
        }

        Map<String, Double> allocations = project(rawAllocations, lowerBounds, upperBounds);
        boolean rateLimited = hasPreviousAllocation && differs(rawAllocations, allocations);
        reasons.add("per-backend minimum and maximum shares applied");
        if (hasPreviousAllocation) {
            reasons.add(rateLimited
                    ? "per-decision share-change limit constrained the recommendation"
                    : "recommendation remained within the per-decision share-change limit");
        } else {
            reasons.add("initial recommendation has no previous allocation to rate-limit");
        }
        return new TrafficAllocationRecommendation(
                rawAllocations,
                allocations,
                0.0,
                false,
                rateLimited,
                reasons);
    }

    private static double availableCapacity(Server server) {
        return server.getCapacity() - server.getLoadScore();
    }

    private static double predictedAvailableCapacity(Server server, Map<String, Double> predictedLoads) {
        return Math.max(0, server.getCapacity() - predictedLoads.get(server.getServerId()));
    }

    private static Map<String, ServerStateVector> uniqueCandidates(List<ServerStateVector> candidates) {
        Map<String, ServerStateVector> unique = new TreeMap<>();
        for (ServerStateVector candidate : candidates) {
            Objects.requireNonNull(candidate, "candidates cannot contain null values");
            if (unique.putIfAbsent(candidate.serverId(), candidate) != null) {
                throw new IllegalArgumentException("candidate serverIds must be unique");
            }
        }
        return unique;
    }

    private static Map<String, Double> scoreDerivedAllocations(
            Map<String, ServerScoreBreakdown> eligibleScores) {
        Map<String, Double> utilities = new TreeMap<>();
        for (Map.Entry<String, ServerScoreBreakdown> entry : eligibleScores.entrySet()) {
            utilities.put(entry.getKey(), 1.0 / (1.0 + entry.getValue().totalScore()));
        }
        double utilityTotal = utilities.values().stream().mapToDouble(Double::doubleValue).sum();
        if (!Double.isFinite(utilityTotal) || utilityTotal <= 0.0) {
            throw new IllegalArgumentException("score-derived utility total must be finite and positive");
        }
        Map<String, Double> normalized = new TreeMap<>();
        utilities.forEach((serverId, utility) -> normalized.put(serverId, utility / utilityTotal));
        correctResidual(normalized, Map.of(), Map.of());
        return normalized;
    }

    private static Map<String, Double> project(
            Map<String, Double> target,
            Map<String, Double> lowerBounds,
            Map<String, Double> upperBounds) {
        Map<String, Double> projected = new TreeMap<>();
        target.forEach((serverId, share) -> projected.put(
                serverId,
                Math.max(lowerBounds.get(serverId), Math.min(upperBounds.get(serverId), share))));

        double total = projected.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total < 1.0 - EPSILON) {
            double remaining = 1.0 - total;
            double room = projected.entrySet().stream()
                    .mapToDouble(entry -> upperBounds.get(entry.getKey()) - entry.getValue())
                    .sum();
            for (String serverId : projected.keySet()) {
                double available = upperBounds.get(serverId) - projected.get(serverId);
                projected.put(serverId, projected.get(serverId) + (remaining * available / room));
            }
        } else if (total > 1.0 + EPSILON) {
            double excess = total - 1.0;
            double removable = projected.entrySet().stream()
                    .mapToDouble(entry -> entry.getValue() - lowerBounds.get(entry.getKey()))
                    .sum();
            for (String serverId : projected.keySet()) {
                double available = projected.get(serverId) - lowerBounds.get(serverId);
                projected.put(serverId, projected.get(serverId) - (excess * available / removable));
            }
        }
        correctResidual(projected, lowerBounds, upperBounds);
        return projected;
    }

    private static void correctResidual(
            Map<String, Double> shares,
            Map<String, Double> lowerBounds,
            Map<String, Double> upperBounds) {
        double residual = 1.0 - shares.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(residual) <= EPSILON && residual == 0.0) {
            return;
        }
        for (String serverId : shares.keySet()) {
            double lower = lowerBounds.getOrDefault(serverId, 0.0);
            double upper = upperBounds.getOrDefault(serverId, 1.0);
            double adjusted = shares.get(serverId) + residual;
            if (adjusted >= lower - EPSILON && adjusted <= upper + EPSILON) {
                shares.put(serverId, Math.max(lower, Math.min(upper, adjusted)));
                residual = 1.0 - shares.values().stream().mapToDouble(Double::doubleValue).sum();
                break;
            }
        }
        if (Math.abs(residual) > EPSILON) {
            throw new IllegalStateException("unable to normalize allocation within configured bounds");
        }
    }

    private static boolean constraintsFeasible(
            Map<String, Double> lowerBounds,
            Map<String, Double> upperBounds) {
        for (String serverId : lowerBounds.keySet()) {
            if (lowerBounds.get(serverId) > upperBounds.get(serverId) + EPSILON) {
                return false;
            }
        }
        double minimumTotal = lowerBounds.values().stream().mapToDouble(Double::doubleValue).sum();
        double maximumTotal = upperBounds.values().stream().mapToDouble(Double::doubleValue).sum();
        return minimumTotal <= 1.0 + EPSILON && maximumTotal >= 1.0 - EPSILON;
    }

    private static boolean hasPositiveIneligiblePreviousShare(
            Map<String, Double> previousAllocations,
            Set<String> eligibleServerIds) {
        return previousAllocations.entrySet().stream()
                .anyMatch(entry -> !eligibleServerIds.contains(entry.getKey()) && entry.getValue() > EPSILON);
    }

    private static void validatePreviousAllocations(Map<String, Double> previousAllocations) {
        double total = 0.0;
        for (Map.Entry<String, Double> entry : previousAllocations.entrySet()) {
            String serverId = entry.getKey();
            if (serverId == null || serverId.isBlank()) {
                throw new IllegalArgumentException("previous allocation serverId cannot be null or blank");
            }
            if (!serverId.equals(serverId.trim())) {
                throw new IllegalArgumentException("previous allocation serverIds must not have surrounding whitespace");
            }
            Double share = Objects.requireNonNull(entry.getValue(), "previous allocations cannot contain null shares");
            if (!Double.isFinite(share) || share < 0.0 || share > 1.0) {
                throw new IllegalArgumentException("previous allocation shares must be between 0.0 and 1.0");
            }
            total += share;
        }
        if (!previousAllocations.isEmpty() && Math.abs(total - 1.0) > EPSILON) {
            throw new IllegalArgumentException("previous allocations must sum to 1.0");
        }
    }

    private static boolean differs(Map<String, Double> first, Map<String, Double> second) {
        return first.keySet().stream()
                .anyMatch(serverId -> Math.abs(first.get(serverId) - second.get(serverId)) > EPSILON);
    }

    private static double clampUnit(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
