package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class EvidenceRedactionService {
    static final String DEFAULT_LABEL = "[REDACTED]";

    private static final String SUMMARY_VERSION = "1";
    private static final ObjectMapper REDACTION_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    RedactionPlan createPlan(List<String> literalValues, String label) {
        String replacement = label == null || label.isBlank() ? DEFAULT_LABEL : label;
        Set<String> distinctValues = new LinkedHashSet<>();
        if (literalValues != null) {
            for (String value : literalValues) {
                if (value != null && !value.isBlank()) {
                    distinctValues.add(value);
                }
            }
        }
        List<RedactionToken> tokens = distinctValues.stream()
                .sorted(Comparator.comparingInt(String::length).reversed().thenComparing(String::compareTo))
                .map(value -> new RedactionToken(
                        ReportChecksumManifestService.sha256(value.getBytes(StandardCharsets.UTF_8)),
                        value))
                .toList();
        return new RedactionPlan(tokens, replacement);
    }

    List<String> readRedactionFile(Path path) throws IOException {
        Objects.requireNonNull(path, "redaction file path cannot be null");
        String content = Files.readString(path, StandardCharsets.UTF_8);
        String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        if (trimmed.startsWith("[")) {
            JsonNode root = REDACTION_MAPPER.readTree(trimmed);
            if (!root.isArray()) {
                throw new IllegalArgumentException("redaction JSON file must contain a string array");
            }
            List<String> values = new ArrayList<>();
            for (JsonNode node : root) {
                if (!node.isTextual()) {
                    throw new IllegalArgumentException("redaction JSON file must contain only strings");
                }
                values.add(node.asText());
            }
            return values;
        }
        return content.lines()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    String toJson(RedactionSummary summary) throws IOException {
        return REDACTION_MAPPER.writeValueAsString(summary) + System.lineSeparator();
    }

    record RedactionPlan(List<RedactionToken> tokens, String label) {

        RedactionPlan {
            tokens = tokens == null ? List.of() : List.copyOf(tokens);
            label = label == null || label.isBlank() ? DEFAULT_LABEL : label;
        }

        boolean enabled() {
            return !tokens.isEmpty();
        }

        RedactionSession newSession() {
            return new RedactionSession(this);
        }

        String redactWithoutCounting(String value) {
            return redact(value, null, false).text();
        }

        private RedactedText redact(String value, Map<String, Integer> tokenCounts, boolean count) {
            if (value == null || !enabled()) {
                return new RedactedText(value == null ? "" : value, 0);
            }
            String redacted = value;
            int total = 0;
            for (RedactionToken token : tokens) {
                int occurrences = countOccurrences(redacted, token.value());
                if (occurrences == 0) {
                    continue;
                }
                redacted = redacted.replace(token.value(), label);
                total += occurrences;
                if (count && tokenCounts != null) {
                    tokenCounts.merge(token.sha256(), occurrences, Integer::sum);
                }
            }
            return new RedactedText(redacted, total);
        }

        private static int countOccurrences(String value, String token) {
            int count = 0;
            int index = 0;
            while ((index = value.indexOf(token, index)) >= 0) {
                count++;
                index += token.length();
            }
            return count;
        }
    }

    static final class RedactionSession {
        private final RedactionPlan plan;
        private final Map<String, Integer> tokenCounts = new LinkedHashMap<>();
        private final Map<String, Integer> fileCounts = new LinkedHashMap<>();

        private RedactionSession(RedactionPlan plan) {
            this.plan = Objects.requireNonNull(plan, "redaction plan cannot be null");
            for (RedactionToken token : plan.tokens()) {
                tokenCounts.put(token.sha256(), 0);
            }
        }

        String redact(String fileName, String value) {
            RedactedText redacted = plan.redact(value, tokenCounts, true);
            fileCounts.merge(fileName, redacted.replacementCount(), Integer::sum);
            return redacted.text();
        }

        RedactionSummary summary() {
            List<RedactedTokenSummary> tokens = plan.tokens().stream()
                    .map(token -> new RedactedTokenSummary(token.sha256(), tokenCounts.getOrDefault(token.sha256(), 0)))
                    .toList();
            List<RedactedFileSummary> files = fileCounts.entrySet().stream()
                    .map(entry -> new RedactedFileSummary(entry.getKey(), entry.getValue()))
                    .toList();
            int total = tokenCounts.values().stream().mapToInt(Integer::intValue).sum();
            return new RedactionSummary(
                    SUMMARY_VERSION,
                    plan.enabled(),
                    plan.label(),
                    plan.tokens().size(),
                    total,
                    tokens,
                    files,
                    List.of(
                            "Summary intentionally records token SHA-256 digests and counts, not original values.",
                            "Redaction is deterministic literal string replacement, not legal anonymization."));
        }
    }

    record RedactionToken(String sha256, String value) {
    }

    record RedactedText(String text, int replacementCount) {
    }

    record RedactionSummary(
            String summaryVersion,
            boolean redactionApplied,
            String label,
            int configuredTokenCount,
            int totalReplacementCount,
            List<RedactedTokenSummary> tokens,
            List<RedactedFileSummary> files,
            List<String> warnings) {

        RedactionSummary {
            tokens = tokens == null ? List.of() : List.copyOf(tokens);
            files = files == null ? List.of() : List.copyOf(files);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    record RedactedTokenSummary(String tokenSha256, int replacementCount) {
    }

    record RedactedFileSummary(String fileName, int replacementCount) {
    }
}
