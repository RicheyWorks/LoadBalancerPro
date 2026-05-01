package core;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class LaseShadowReplayEngine {
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\r\\n\\t]+");
    private static final Pattern SECRET_SHAPED_WORDS = Pattern.compile(
            "(?i)(api[-_ ]?key|secret|token|password|credential|access[-_ ]?key)[^\\s,;]*");
    private static final int MAX_FAILURE_REASON_LENGTH = 120;

    public LaseShadowReplayMetrics evaluate(Iterable<LaseShadowReplayRecord> records) {
        Objects.requireNonNull(records, "records cannot be null");

        long totalEvents = 0;
        long comparableEvents = 0;
        long agreementCount = 0;
        long failSafeCount = 0;
        Instant firstEventTimestamp = null;
        Instant latestEventTimestamp = null;
        Map<String, Long> recommendationCounts = new LinkedHashMap<>();
        Map<String, Long> failureReasonCounts = new LinkedHashMap<>();
        LaseShadowReplayScoreSummary.ScoreAccumulator decisionScores =
                new LaseShadowReplayScoreSummary.ScoreAccumulator();
        LaseShadowReplayScoreSummary.ScoreAccumulator networkRiskScores =
                new LaseShadowReplayScoreSummary.ScoreAccumulator();
        NetworkAccumulator networkAccumulator = new NetworkAccumulator();

        for (LaseShadowReplayRecord record : records) {
            Objects.requireNonNull(record, "records cannot contain null entries");
            LaseShadowEvent event = record.event();
            totalEvents++;
            if (event.agreedWithRouting() != null) {
                comparableEvents++;
                if (Boolean.TRUE.equals(event.agreedWithRouting())) {
                    agreementCount++;
                }
            }
            if (event.failSafe()) {
                failSafeCount++;
            }
            recommendationCounts.merge(event.recommendedAction(), 1L, Long::sum);
            if (event.failureReason() != null) {
                failureReasonCounts.merge(sanitizeFailureReason(event.failureReason()), 1L, Long::sum);
            }
            decisionScores.record(event.decisionScore());
            networkRiskScores.record(event.networkRiskScore());
            networkAccumulator.record(event.networkAwarenessSignal());
            firstEventTimestamp = firstEventTimestamp == null || event.timestamp().isBefore(firstEventTimestamp)
                    ? event.timestamp()
                    : firstEventTimestamp;
            latestEventTimestamp = latestEventTimestamp == null || event.timestamp().isAfter(latestEventTimestamp)
                    ? event.timestamp()
                    : latestEventTimestamp;
        }

        double agreementRate = comparableEvents == 0 ? 0.0 : agreementCount / (double) comparableEvents;
        double failSafeRate = totalEvents == 0 ? 0.0 : failSafeCount / (double) totalEvents;
        return new LaseShadowReplayMetrics(
                totalEvents,
                comparableEvents,
                agreementCount,
                agreementRate,
                failSafeCount,
                failSafeRate,
                recommendationCounts,
                LaseShadowReplayScoreSummary.from(decisionScores),
                LaseShadowReplayScoreSummary.from(networkRiskScores),
                networkAccumulator.summary(),
                firstEventTimestamp,
                latestEventTimestamp,
                failureReasonCounts);
    }

    private static String sanitizeFailureReason(String reason) {
        String sanitized = CONTROL_CHARACTERS.matcher(reason).replaceAll(" ").trim();
        sanitized = SECRET_SHAPED_WORDS.matcher(sanitized).replaceAll("[redacted]");
        if (sanitized.isBlank()) {
            return "unspecified failure reason";
        }
        if (sanitized.length() > MAX_FAILURE_REASON_LENGTH) {
            return sanitized.substring(0, MAX_FAILURE_REASON_LENGTH) + "...";
        }
        return sanitized;
    }

    private static final class NetworkAccumulator {
        private long count;
        private double timeoutRateTotal;
        private double retryRateTotal;
        private double connectionFailureRateTotal;
        private double maxLatencyJitterMillis;
        private long recentErrorBurstCount;
        private long totalRequestTimeoutCount;

        void record(NetworkAwarenessSignal signal) {
            count++;
            timeoutRateTotal += signal.timeoutRate();
            retryRateTotal += signal.retryRate();
            connectionFailureRateTotal += signal.connectionFailureRate();
            maxLatencyJitterMillis = Math.max(maxLatencyJitterMillis, signal.latencyJitterMillis());
            if (signal.recentErrorBurst()) {
                recentErrorBurstCount++;
            }
            totalRequestTimeoutCount += signal.requestTimeoutCount();
        }

        LaseShadowNetworkSummary summary() {
            if (count == 0) {
                return LaseShadowNetworkSummary.empty();
            }
            return new LaseShadowNetworkSummary(
                    timeoutRateTotal / count,
                    retryRateTotal / count,
                    connectionFailureRateTotal / count,
                    maxLatencyJitterMillis,
                    recentErrorBurstCount,
                    totalRequestTimeoutCount);
        }
    }
}
