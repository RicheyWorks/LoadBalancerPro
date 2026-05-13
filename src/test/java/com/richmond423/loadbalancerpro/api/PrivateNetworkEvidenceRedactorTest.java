package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PrivateNetworkEvidenceRedactorTest {
    @Test
    void redactsSensitiveEvidenceValuesAndRawBackendUrls() {
        String sensitive = String.join("\n",
                "X-API-Key: TEST_PRIVATE_NETWORK_EVIDENCE_API_KEY",
                "Authorization: Bearer TEST_PRIVATE_NETWORK_EVIDENCE_BEARER_TOKEN",
                "Cookie: SESSION=TEST_PRIVATE_NETWORK_EVIDENCE_COOKIE",
                "Set-Cookie: SESSION=TEST_PRIVATE_NETWORK_EVIDENCE_SET_COOKIE",
                "token=TEST_PRIVATE_NETWORK_EVIDENCE_TOKEN",
                "apiKey=TEST_PRIVATE_NETWORK_EVIDENCE_API_KEY_FIELD",
                "secret=TEST_PRIVATE_NETWORK_EVIDENCE_SECRET",
                "password=TEST_PRIVATE_NETWORK_EVIDENCE_PASSWORD",
                "credential=TEST_PRIVATE_NETWORK_EVIDENCE_CREDENTIAL",
                "backendUrl=http://127.0.0.1:18081",
                "rawBackendUrl=http://10.1.2.3:18082",
                "normalizedUrl=http://192.168.1.10:18083");

        String redacted = PrivateNetworkEvidenceRedactor.redact(sensitive);

        for (String prohibited : new String[] {
                "TEST_PRIVATE_NETWORK_EVIDENCE_API_KEY",
                "TEST_PRIVATE_NETWORK_EVIDENCE_BEARER_TOKEN",
                "TEST_PRIVATE_NETWORK_EVIDENCE_COOKIE",
                "TEST_PRIVATE_NETWORK_EVIDENCE_SET_COOKIE",
                "TEST_PRIVATE_NETWORK_EVIDENCE_TOKEN",
                "TEST_PRIVATE_NETWORK_EVIDENCE_API_KEY_FIELD",
                "TEST_PRIVATE_NETWORK_EVIDENCE_SECRET",
                "TEST_PRIVATE_NETWORK_EVIDENCE_PASSWORD",
                "TEST_PRIVATE_NETWORK_EVIDENCE_CREDENTIAL",
                "http://127.0.0.1:18081",
                "http://10.1.2.3:18082",
                "http://192.168.1.10:18083"
        }) {
            assertFalse(redacted.contains(prohibited), "redacted evidence should remove " + prohibited);
        }
        assertTrue(redacted.contains("X-API-Key: <REDACTED>"));
        assertTrue(redacted.contains("Authorization: <REDACTED>"));
        assertTrue(redacted.contains("Cookie: <REDACTED>"));
        assertTrue(redacted.contains("Set-Cookie: <REDACTED>"));
        assertTrue(redacted.contains("backendUrl=<REDACTED>"));
        assertTrue(redacted.contains("rawBackendUrl=<REDACTED>"));
        assertTrue(redacted.contains("normalizedUrl=<REDACTED>"));
    }
}
