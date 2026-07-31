package com.richmond423.loadbalancerpro.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CombinedBuildPlanCampaignStateTest {
    private static final Path MANIFEST =
            Path.of("docs/agent/COMBINED_BUILD_PLAN_CAMPAIGN_SLOTS.json");
    private static final Set<String> FIELDS =
            Set.of("slotId", "status", "pr", "finalHead", "mergeCommit", "blocker");
    private static final Set<String> STATUSES = Set.of("OPEN", "IN_PROGRESS", "MAIN_GREEN");
    private static final Pattern SHA = Pattern.compile("[0-9a-f]{40}");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void manifestContainsOnlyMinimalValidSlotState() throws IOException {
        JsonNode root = MAPPER.readTree(MANIFEST.toFile());
        assertEquals(Set.of("slots"), fieldNames(root));

        JsonNode slots = root.path("slots");
        assertTrue(slots.isArray());
        assertEquals(49, slots.size());

        Set<String> ids = new HashSet<>();
        int active = 0;
        for (JsonNode slot : slots) {
            assertEquals(FIELDS, fieldNames(slot));
            assertTrue(ids.add(requiredText(slot, "slotId")), "duplicate slot ID");

            String status = requiredText(slot, "status");
            assertTrue(STATUSES.contains(status), "unknown status " + status);
            if ("IN_PROGRESS".equals(status)) {
                active++;
            }

            if ("MAIN_GREEN".equals(status)) {
                assertFalse(requiredText(slot, "pr").isBlank());
                assertTrue(SHA.matcher(requiredText(slot, "finalHead")).matches());
                assertTrue(SHA.matcher(requiredText(slot, "mergeCommit")).matches());
                assertTrue(slot.path("blocker").isNull());
            } else if ("OPEN".equals(status)) {
                assertTrue(slot.path("pr").isNull());
                assertTrue(slot.path("finalHead").isNull());
                assertTrue(slot.path("mergeCommit").isNull());
                assertTrue(slot.path("blocker").isNull());
            }
        }
        assertTrue(active <= 1, "only one slot may be active");
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new HashSet<>();
        Iterator<String> fields = node.fieldNames();
        fields.forEachRemaining(names::add);
        return names;
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        assertTrue(value.isTextual(), field + " must be text");
        assertFalse(value.asText().isBlank(), field + " must not be blank");
        return value.asText();
    }
}
