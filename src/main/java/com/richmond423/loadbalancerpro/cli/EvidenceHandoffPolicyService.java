package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

final class EvidenceHandoffPolicyService {
    private static final List<String> LIMITATIONS = List.of(
            "Local checksum policy evaluation only; no identity proof or cryptographic signature is provided.",
            "This policy report is not a legal chain-of-custody system.",
            "The policy can only evaluate drift already recorded in the supplied evidence catalog diff.");
    private static final ObjectMapper POLICY_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    HandoffPolicy readPolicy(Path policyPath) throws IOException {
        Objects.requireNonNull(policyPath, "policy path cannot be null");
        try {
            return readPolicyNode(POLICY_MAPPER.readTree(policyPath.toFile()));
        } catch (IOException | IllegalArgumentException e) {
            throw new IOException("failed to read evidence handoff policy " + policyPath + ": " + safeMessage(e), e);
        }
    }

    HandoffPolicy readPolicyJson(String sourceName, String policyJson) throws IOException {
        try {
            return readPolicyNode(POLICY_MAPPER.readTree(policyJson));
        } catch (IOException | IllegalArgumentException e) {
            throw new IOException("failed to read evidence handoff policy " + sourceName + ": " + safeMessage(e), e);
        }
    }

    PolicyEvaluation evaluate(EvidenceCatalogDiffService.EvidenceCatalogDiff diff, HandoffPolicy policy) {
        Objects.requireNonNull(diff, "diff cannot be null");
        Objects.requireNonNull(policy, "policy cannot be null");
        List<EvaluatedPolicyChange> evaluatedChanges = new ArrayList<>();
        int failCount = 0;
        int warnCount = 0;
        int infoCount = 0;
        int ignoredCount = 0;
        int unclassifiedCount = 0;

        for (EvidenceCatalogDiffService.EvidenceCatalogChange change : diff.changes()) {
            if (!change.drifted()) {
                continue;
            }
            HandoffPolicyRule matched = matchingRule(change, policy.rules());
            boolean unclassified = matched == null;
            Severity severity = matched == null ? defaultSeverity(policy) : matched.severity();
            switch (severity) {
                case FAIL -> failCount++;
                case WARN -> warnCount++;
                case INFO -> infoCount++;
                case IGNORE -> ignoredCount++;
            }
            if (unclassified) {
                unclassifiedCount++;
            }
            evaluatedChanges.add(new EvaluatedPolicyChange(
                    change.path(),
                    change.changeType(),
                    severity,
                    matched == null ? null : matched.ruleLabel(),
                    matched == null ? "unmatched change uses policy default severity" : matched.reason(),
                    change.beforeType(),
                    change.afterType(),
                    change.beforeSha256(),
                    change.afterSha256(),
                    change.beforeVerificationStatus(),
                    change.afterVerificationStatus(),
                    change.beforeLatestAuditEntryHash(),
                    change.afterLatestAuditEntryHash(),
                    change.beforeAuditEntryCount(),
                    change.afterAuditEntryCount(),
                    unclassified));
        }

        PolicyDecision decision = failCount > 0
                ? PolicyDecision.FAIL
                : warnCount > 0 ? PolicyDecision.WARN : PolicyDecision.PASS;
        PolicyEvaluationSummary summary = new PolicyEvaluationSummary(
                failCount,
                warnCount,
                infoCount,
                ignoredCount,
                unclassifiedCount);
        return new PolicyEvaluation(
                policy.policyVersion(),
                policy.mode(),
                policy.defaultSeverity(),
                decision,
                diff.beforeCatalog(),
                diff.afterCatalog(),
                summary,
                List.copyOf(evaluatedChanges),
                LIMITATIONS);
    }

    String renderJson(PolicyEvaluation evaluation) throws IOException {
        return POLICY_MAPPER.writeValueAsString(evaluation) + System.lineSeparator();
    }

    String renderMarkdown(PolicyEvaluation evaluation) {
        StringBuilder builder = new StringBuilder();
        builder.append("# LoadBalancerPro Evidence Handoff Policy Report")
                .append(System.lineSeparator())
                .append(System.lineSeparator());
        builder.append("- Policy version: ").append(evaluation.policyVersion()).append(System.lineSeparator());
        builder.append("- Mode: ").append(evaluation.mode()).append(System.lineSeparator());
        builder.append("- Default severity: ").append(evaluation.defaultSeverity()).append(System.lineSeparator());
        builder.append("- Decision: ").append(evaluation.decision()).append(System.lineSeparator());
        builder.append("- Before catalog: ").append(evaluation.beforeCatalog().path()).append(System.lineSeparator());
        builder.append("- After catalog: ").append(evaluation.afterCatalog().path()).append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("## Summary").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- Fail: ").append(evaluation.summary().failCount()).append(System.lineSeparator());
        builder.append("- Warn: ").append(evaluation.summary().warnCount()).append(System.lineSeparator());
        builder.append("- Info: ").append(evaluation.summary().infoCount()).append(System.lineSeparator());
        builder.append("- Ignored: ").append(evaluation.summary().ignoredCount()).append(System.lineSeparator());
        builder.append("- Unclassified: ").append(evaluation.summary().unclassifiedCount())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("## Evaluated Changes").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("| Path | Change | Severity | Rule | Reason |")
                .append(System.lineSeparator());
        builder.append("| --- | --- | --- | --- | --- |").append(System.lineSeparator());
        for (EvaluatedPolicyChange change : evaluation.evaluatedChanges()) {
            builder.append("| ")
                    .append(escapeMarkdown(change.path()))
                    .append(" | ")
                    .append(change.changeType())
                    .append(" | ")
                    .append(change.severity())
                    .append(" | ")
                    .append(escapeMarkdown(change.matchedRule()))
                    .append(" | ")
                    .append(escapeMarkdown(change.reason()))
                    .append(" |")
                    .append(System.lineSeparator());
        }

        builder.append(System.lineSeparator()).append("## Limitations").append(System.lineSeparator())
                .append(System.lineSeparator());
        for (String limitation : evaluation.limitations()) {
            builder.append("- ").append(limitation).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private static Severity defaultSeverity(HandoffPolicy policy) {
        if (policy.mode() == PolicyMode.STRICT) {
            return Severity.FAIL;
        }
        return policy.defaultSeverity();
    }

    private static HandoffPolicyRule matchingRule(
            EvidenceCatalogDiffService.EvidenceCatalogChange change,
            List<HandoffPolicyRule> rules) {
        for (HandoffPolicyRule rule : rules) {
            if (rule.changeType() == change.changeType() && rule.matches(change.path())) {
                return rule;
            }
        }
        return null;
    }

    private static HandoffPolicy readPolicyNode(JsonNode root) {
        List<HandoffPolicyRule> rules = new ArrayList<>();
        JsonNode ruleNodes = root.path("rules");
        if (!ruleNodes.isMissingNode() && !ruleNodes.isArray()) {
            throw new IllegalArgumentException("policy rules must be an array");
        }
        if (ruleNodes.isArray()) {
            for (int index = 0; index < ruleNodes.size(); index++) {
                JsonNode rule = ruleNodes.get(index);
                EvidenceCatalogDiffService.ChangeType changeType =
                        EvidenceCatalogDiffService.ChangeType.parse(requiredText(rule, "changeType"));
                if (changeType == EvidenceCatalogDiffService.ChangeType.UNCHANGED) {
                    throw new IllegalArgumentException("policy rule changeType cannot be UNCHANGED");
                }
                rules.add(new HandoffPolicyRule(
                        changeType,
                        requiredText(rule, "pathPattern"),
                        Severity.parse(requiredText(rule, "severity")),
                        optionalText(rule, "reason"),
                        index + 1));
            }
        }
        return new HandoffPolicy(
                requiredText(root, "policyVersion"),
                PolicyMode.parse(requiredText(root, "mode")),
                Severity.parse(optionalText(root, "defaultSeverity", "FAIL")),
                List.copyOf(rules));
    }

    private static String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null) {
            throw new IllegalArgumentException("policy " + field + " is required");
        }
        return value;
    }

    private static String optionalText(JsonNode node, String field) {
        return optionalText(node, field, null);
    }

    private static String optionalText(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : fallback;
    }

    private static String escapeMarkdown(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace(System.lineSeparator(), " ");
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    enum PolicyMode {
        STRICT,
        ALLOWLIST;

        static PolicyMode parse(String value) {
            String normalized = Objects.requireNonNull(value, "policy mode is required")
                    .trim()
                    .toUpperCase(Locale.ROOT);
            return switch (normalized) {
                case "STRICT" -> STRICT;
                case "ALLOWLIST" -> ALLOWLIST;
                default -> throw new IllegalArgumentException("policy mode must be STRICT or ALLOWLIST");
            };
        }
    }

    enum Severity {
        FAIL,
        WARN,
        INFO,
        IGNORE;

        static Severity parse(String value) {
            String normalized = Objects.requireNonNull(value, "severity is required")
                    .trim()
                    .toUpperCase(Locale.ROOT);
            return switch (normalized) {
                case "FAIL" -> FAIL;
                case "WARN" -> WARN;
                case "INFO" -> INFO;
                case "IGNORE" -> IGNORE;
                default -> throw new IllegalArgumentException("severity must be FAIL, WARN, INFO, or IGNORE");
            };
        }
    }

    enum PolicyDecision {
        PASS,
        WARN,
        FAIL
    }

    enum PolicyReportFormat {
        MARKDOWN,
        JSON;

        static PolicyReportFormat parse(String value) {
            String normalized = Objects.requireNonNull(value, "policy report format is required")
                    .trim()
                    .toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "markdown", "md" -> MARKDOWN;
                case "json" -> JSON;
                default -> throw new IllegalArgumentException("policy report format must be markdown or json");
            };
        }
    }

    record HandoffPolicy(
            String policyVersion,
            PolicyMode mode,
            Severity defaultSeverity,
            List<HandoffPolicyRule> rules) {

        HandoffPolicy {
            rules = rules == null ? List.of() : List.copyOf(rules);
        }
    }

    record HandoffPolicyRule(
            EvidenceCatalogDiffService.ChangeType changeType,
            String pathPattern,
            Severity severity,
            String reason,
            int index) {

        HandoffPolicyRule {
            Objects.requireNonNull(changeType, "rule change type cannot be null");
            if (pathPattern == null || pathPattern.isBlank()) {
                throw new IllegalArgumentException("rule pathPattern is required");
            }
            severity = severity == null ? Severity.FAIL : severity;
            reason = reason == null || reason.isBlank() ? "policy rule matched" : reason.trim();
        }

        boolean matches(String path) {
            String normalizedPattern = normalize(pathPattern);
            String normalizedPath = normalize(path);
            if (normalizedPattern.equals(normalizedPath)) {
                return true;
            }
            if (normalizedPattern.endsWith("/**")) {
                String prefix = normalizedPattern.substring(0, normalizedPattern.length() - 3);
                return normalizedPath.equals(prefix) || normalizedPath.startsWith(prefix + "/");
            }
            if (!normalizedPattern.contains("*")) {
                return false;
            }
            return Pattern.compile(globRegex(normalizedPattern)).matcher(normalizedPath).matches();
        }

        String ruleLabel() {
            return "#" + index + " " + changeType + " " + pathPattern;
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim().replace('\\', '/');
        }

        private static String globRegex(String pattern) {
            StringBuilder regex = new StringBuilder("^");
            for (int index = 0; index < pattern.length(); index++) {
                char current = pattern.charAt(index);
                if (current == '*') {
                    boolean doubleStar = index + 1 < pattern.length() && pattern.charAt(index + 1) == '*';
                    if (doubleStar) {
                        regex.append(".*");
                        index++;
                    } else {
                        regex.append("[^/]*");
                    }
                } else {
                    regex.append(Pattern.quote(String.valueOf(current)));
                }
            }
            regex.append("$");
            return regex.toString();
        }
    }

    record PolicyEvaluation(
            String policyVersion,
            PolicyMode mode,
            Severity defaultSeverity,
            PolicyDecision decision,
            EvidenceCatalogDiffService.EvidenceCatalogRef beforeCatalog,
            EvidenceCatalogDiffService.EvidenceCatalogRef afterCatalog,
            PolicyEvaluationSummary summary,
            List<EvaluatedPolicyChange> evaluatedChanges,
            List<String> limitations) {

        PolicyEvaluation {
            evaluatedChanges = evaluatedChanges == null ? List.of() : List.copyOf(evaluatedChanges);
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
        }
    }

    record PolicyEvaluationSummary(
            int failCount,
            int warnCount,
            int infoCount,
            int ignoredCount,
            int unclassifiedCount) {
    }

    record EvaluatedPolicyChange(
            String path,
            EvidenceCatalogDiffService.ChangeType changeType,
            Severity severity,
            String matchedRule,
            String reason,
            String beforeType,
            String afterType,
            String beforeSha256,
            String afterSha256,
            String beforeVerificationStatus,
            String afterVerificationStatus,
            String beforeLatestAuditEntryHash,
            String afterLatestAuditEntryHash,
            Integer beforeAuditEntryCount,
            Integer afterAuditEntryCount,
            boolean unclassified) {
    }
}
