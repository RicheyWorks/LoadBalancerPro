package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.regex.Pattern;

public final class PrivateNetworkEvidenceRedactor {
    private static final String REDACTED = "<REDACTED>";
    private static final List<Pattern> REDACTION_PATTERNS = List.of(
            Pattern.compile("(?i)(X-API-Key\\s*[:=]\\s*)[^\\s,;\\r\\n]+"),
            Pattern.compile("(?i)(Authorization\\s*[:=]\\s*)Bearer\\s+[^\\s,;\\r\\n]+"),
            Pattern.compile("(?i)(Cookie\\s*[:=]\\s*)[^\\r\\n]+"),
            Pattern.compile("(?i)(Set-Cookie\\s*[:=]\\s*)[^\\r\\n]+"),
            Pattern.compile("(?i)((?:api[-_ ]?key|token|secret|password|credential)\\s*[=:]\\s*)[^\\s,;\\r\\n]+"),
            Pattern.compile("(?i)((?:rawBackendUrl|backendUrl|normalizedUrl)\\s*[=:]\\s*)https?://[^\\s,;\\r\\n]+"));

    private PrivateNetworkEvidenceRedactor() {
    }

    public static String redact(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String redacted = value;
        for (Pattern pattern : REDACTION_PATTERNS) {
            redacted = pattern.matcher(redacted).replaceAll("$1" + REDACTED);
        }
        return redacted;
    }

    public static void assertNoSensitiveEvidence(String evidence, String... sensitiveValues) {
        String safeEvidence = evidence == null ? "" : evidence;
        for (String prohibited : List.of(
                "X-API-Key",
                "Authorization",
                "Bearer ",
                "Cookie",
                "Set-Cookie")) {
            assertFalse(safeEvidence.contains(prohibited), "evidence must not contain " + prohibited);
        }
        for (String sensitiveValue : sensitiveValues) {
            if (sensitiveValue != null && !sensitiveValue.isBlank()) {
                assertFalse(safeEvidence.contains(sensitiveValue),
                        "evidence must not contain sensitive value " + sensitiveValue);
            }
        }
    }
}
