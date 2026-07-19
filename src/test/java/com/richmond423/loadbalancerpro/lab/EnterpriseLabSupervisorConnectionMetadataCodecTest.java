package com.richmond423.loadbalancerpro.lab;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabSupervisorConnectionMetadataCodecTest {
    private static final Instant NOW = Instant.parse("2026-07-19T10:00:00Z");
    private static final String FINGERPRINT = "a".repeat(64);

    private final EnterpriseLabSupervisorConnectionMetadataCodec codec =
            new EnterpriseLabSupervisorConnectionMetadataCodec();

    @Test
    void canonicalRoundTripUsesOneFixedLiteralLoopbackDocument() {
        EnterpriseLabSupervisorConnectionMetadata metadata = metadata();
        byte[] encoded = codec.encode(metadata);

        assertEquals((byte) '\n', encoded[encoded.length - 1]);
        assertArrayEquals(encoded, codec.encode(codec.decode(encoded)));
        assertEquals(metadata, codec.decode(encoded));
        String text = new String(encoded, StandardCharsets.UTF_8);
        assertTrue(text.startsWith(
                "{\"schemaVersion\":\"enterprise-lab-supervisor-readiness/v1\","
                        + "\"address\":\"127.0.0.1\",\"port\":18081,"));
    }

    @Test
    void rejectsUnknownDuplicateMissingAndNonCanonicalContent() {
        String canonical = new String(codec.encode(metadata()), StandardCharsets.UTF_8);
        assertEquals(
                EnterpriseLabSupervisorConnectionMetadataCodec.Failure.UNKNOWN_FIELD,
                failure(canonical.replaceFirst("\\{", "{\"unknown\":true,")));
        assertEquals(
                EnterpriseLabSupervisorConnectionMetadataCodec.Failure.MALFORMED_METADATA,
                failure(canonical.replaceFirst(
                        "\"address\":\"127.0.0.1\",",
                        "\"address\":\"127.0.0.1\",\"address\":\"127.0.0.1\",")));
        assertEquals(
                EnterpriseLabSupervisorConnectionMetadataCodec.Failure.MISSING_FIELD,
                failure(canonical.replace("\"address\":\"127.0.0.1\",", "")));
        assertEquals(
                EnterpriseLabSupervisorConnectionMetadataCodec.Failure.NON_CANONICAL_METADATA,
                failure(canonical.replace("{", "{ ")));
        assertEquals(
                EnterpriseLabSupervisorConnectionMetadataCodec.Failure.NON_CANONICAL_METADATA,
                failure(canonical.replace("\n", "\r\n")));
    }

    @Test
    void recordRejectsEveryAddressPortGenerationAndFingerprintEscape() {
        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabSupervisorConnectionMetadata(
                        EnterpriseLabSupervisorConnectionMetadata.SCHEMA_VERSION,
                        "localhost", 18081, "supervisor-a", 1L, 1L,
                        FINGERPRINT, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabSupervisorConnectionMetadata(
                        EnterpriseLabSupervisorConnectionMetadata.SCHEMA_VERSION,
                        EnterpriseLabSupervisorConnectionMetadata.LITERAL_ADDRESS,
                        80, "supervisor-a", 1L, 1L, FINGERPRINT, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabSupervisorConnectionMetadata(
                        EnterpriseLabSupervisorConnectionMetadata.SCHEMA_VERSION,
                        EnterpriseLabSupervisorConnectionMetadata.LITERAL_ADDRESS,
                        18081, "supervisor-a", 0L, 1L, FINGERPRINT, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabSupervisorConnectionMetadata(
                        EnterpriseLabSupervisorConnectionMetadata.SCHEMA_VERSION,
                        EnterpriseLabSupervisorConnectionMetadata.LITERAL_ADDRESS,
                        18081, "supervisor-a", 1L, 0L, FINGERPRINT, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabSupervisorConnectionMetadata(
                        EnterpriseLabSupervisorConnectionMetadata.SCHEMA_VERSION,
                        EnterpriseLabSupervisorConnectionMetadata.LITERAL_ADDRESS,
                        18081, "supervisor-a", 1L, 1L, "not-a-fingerprint", NOW));
    }

    private EnterpriseLabSupervisorConnectionMetadataCodec.Failure failure(String text) {
        return assertThrows(
                EnterpriseLabSupervisorConnectionMetadataCodec.CodecException.class,
                () -> codec.decode(text.getBytes(StandardCharsets.UTF_8)))
                .failure();
    }

    private static EnterpriseLabSupervisorConnectionMetadata metadata() {
        return new EnterpriseLabSupervisorConnectionMetadata(
                EnterpriseLabSupervisorConnectionMetadata.SCHEMA_VERSION,
                EnterpriseLabSupervisorConnectionMetadata.LITERAL_ADDRESS,
                18081,
                "supervisor-a",
                1L,
                7L,
                FINGERPRINT,
                NOW);
    }
}
