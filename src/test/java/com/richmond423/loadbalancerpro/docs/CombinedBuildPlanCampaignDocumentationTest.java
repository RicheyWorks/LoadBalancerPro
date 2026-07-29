package com.richmond423.loadbalancerpro.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CombinedBuildPlanCampaignDocumentationTest {
    private static final Path CONTRACT =
            Path.of("docs/agent/COMBINED_BUILD_PLAN_CAMPAIGN_CONTRACT.md");
    private static final Path BOARD =
            Path.of("docs/agent/COMBINED_BUILD_PLAN_CAMPAIGN_BOARD.md");
    private static final Path MANIFEST =
            Path.of("docs/agent/COMBINED_BUILD_PLAN_CAMPAIGN_SLOTS.json");
    private static final Path DEPLOYABLE_PLAN = Path.of("docs/BUILD_PLAN_DEPLOYABLE.md");
    private static final Path LAB_PLAN = Path.of("docs/BUILD_PLAN_LAB_SHADOW.md");
    private static final Path PROXY_AUDIT = Path.of("docs/AUDIT_2026-07-21.md");
    private static final Path LAB_AUDIT = Path.of("docs/AUDIT_LAB_SHADOW_2026-07-21.md");
    private static final Path PLAYGROUND = Path.of("docs/strategy-playground.html");
    private static final Path README = Path.of("README.md");
    private static final Path INDEX = Path.of("docs/agent/CAMPAIGN_SYSTEM_INDEX.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "CombinedBuildPlanCampaignDocumentationTest.java");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void manifestDefinesFiftySourceItemsAsFortyNineTrackedSlots() throws IOException {
        JsonNode root = MAPPER.readTree(MANIFEST.toFile());
        JsonNode slots = root.path("slots");

        assertEquals(50, root.path("sourceItemCount").asInt());
        assertEquals(49, root.path("uniqueSlotCount").asInt());
        assertEquals(49, slots.size());
        assertEquals("CONTRACT-00", root.path("contractPrId").asText());
        assertEquals(
                List.of("P-4.1", "P-4.2", "P-4.3", "P-4.4"),
                MAPPER.convertValue(root.path("syntheticSourceIds"), List.class));

        JsonNode overlap = root.path("overlaps").get(0);
        assertEquals("SEC-DEFAULT-DENY", overlap.path("canonicalId").asText());
        assertEquals(
                List.of("P-0.5", "L-0.1"),
                MAPPER.convertValue(overlap.path("sourceIds"), List.class));

        Set<String> ids = new HashSet<>();
        Set<String> allowedStatuses = new HashSet<>(
                MAPPER.convertValue(root.path("statusValues"), List.class));
        int sourceItemMappings = 0;
        int activeSlots = 0;
        for (int index = 0; index < slots.size(); index++) {
            JsonNode slot = slots.get(index);
            assertEquals(index + 1, slot.path("ordinal").asInt());
            String status = slot.path("status").asText();
            assertTrue(allowedStatuses.contains(status), "unknown slot status");
            if (!Set.of("OPEN", "MAIN_GREEN").contains(status)) {
                activeSlots++;
            }
            assertTrue(ids.add(slot.path("id").asText()), "duplicate slot id");
            assertFalse(slot.path("title").asText().isBlank());
            assertFalse(slot.path("currentState").asText().isBlank());
            sourceItemMappings += slot.path("sourceIds").size();
        }
        assertEquals(50, sourceItemMappings);
        assertTrue(activeSlots <= 1, "only one slot may be active between OPEN and MAIN_GREEN");
    }

    @Test
    void everyDependencyExistsEarlierAndEveryVerificationProfileExists() throws IOException {
        JsonNode root = MAPPER.readTree(MANIFEST.toFile());
        JsonNode slots = root.path("slots");
        JsonNode profiles = root.path("verificationProfiles");
        Map<String, Integer> ordinalById = new HashMap<>();
        Map<String, JsonNode> slotById = new HashMap<>();

        for (JsonNode slot : slots) {
            ordinalById.put(slot.path("id").asText(), slot.path("ordinal").asInt());
            slotById.put(slot.path("id").asText(), slot);
        }

        for (JsonNode slot : slots) {
            String id = slot.path("id").asText();
            int ordinal = slot.path("ordinal").asInt();
            String profile = slot.path("verificationProfile").asText();
            assertTrue(profiles.hasNonNull(profile), id + " has unknown profile " + profile);
            for (JsonNode dependency : slot.path("dependencies")) {
                Integer dependencyOrdinal = ordinalById.get(dependency.asText());
                assertNotNull(dependencyOrdinal, id + " has unknown dependency " + dependency.asText());
                assertTrue(dependencyOrdinal < ordinal, id + " dependency must appear earlier");
            }
        }

        for (String completeM0Dependent : List.of("P-1.1", "P-1.4")) {
            assertTrue(
                    MAPPER.convertValue(
                                    slotById.get(completeM0Dependent).path("dependencies"), List.class)
                            .contains("SEC-DEFAULT-DENY"),
                    completeM0Dependent + " must depend on the shared proxy P-0.5 security slot");
        }
        assertEquals(
                "Add reload-preserved per-upstream runtime statistics",
                slotById.get("P-1.2").path("title").asText());
    }

    @Test
    void boardAndContractCoverEveryCanonicalAndSourceId() throws IOException {
        JsonNode root = MAPPER.readTree(MANIFEST.toFile());
        String board = read(BOARD);
        String contract = read(CONTRACT);

        for (JsonNode slot : root.path("slots")) {
            String id = slot.path("id").asText();
            assertTrue(board.contains(id), "board should contain canonical id " + id);
            for (JsonNode sourceId : slot.path("sourceIds")) {
                assertTrue(
                        board.contains(sourceId.asText()) || contract.contains(sourceId.asText()),
                        "campaign docs should contain source id " + sourceId.asText());
            }
        }

        for (String expected : List.of(
                "one scoped PR at a time",
                "full-diff self-review",
                "all 49 unique slots are `MAIN_GREEN`",
                "Imported",
                "planning inputs",
                "no required-check",
                "liveMode=false",
                "No other implementation slot is open")) {
            assertTrue(contract.contains(expected), "contract should contain " + expected);
        }
    }

    @Test
    void importedLayoutAndNavigationArePresent() throws IOException {
        for (Path path : List.of(
                DEPLOYABLE_PLAN, LAB_PLAN, PROXY_AUDIT, LAB_AUDIT, PLAYGROUND, CONTRACT, BOARD, MANIFEST)) {
            assertTrue(Files.isRegularFile(path), path + " should be a tracked file");
        }

        assertTrue(read(DEPLOYABLE_PLAN).contains("Build Plan: Path to Deployable"));
        assertTrue(read(LAB_PLAN).contains("Build Plan: Lab, Shadow & Analysis Subsystems"));
        assertTrue(read(PLAYGROUND).toLowerCase().contains("<html"));

        for (String navigation : List.of(
                "COMBINED_BUILD_PLAN_CAMPAIGN_CONTRACT.md",
                "COMBINED_BUILD_PLAN_CAMPAIGN_BOARD.md",
                "COMBINED_BUILD_PLAN_CAMPAIGN_SLOTS.json")) {
            assertTrue(read(README).contains(navigation), "README should link " + navigation);
            assertTrue(read(INDEX).contains(navigation), "campaign index should link " + navigation);
        }
    }

    @Test
    void campaignPreservesNotProvenAndExternalTargetBoundaries() throws IOException {
        String combined = (read(CONTRACT) + "\n" + read(BOARD)).toLowerCase();

        for (String expected : List.of(
                "production readiness",
                "production certification",
                "live-cloud",
                "real-tenant",
                "public/external",
                "production throughput/p95/p99",
                "broader automation",
                "no secrets",
                "no live production target")) {
            assertTrue(combined.contains(expected), "campaign should preserve boundary " + expected);
        }
    }

    @Test
    void guardTestOnlyReadsTrackedFiles() throws IOException {
        String source = read(SOURCE);

        for (String forbidden : List.of(
                "Files." + "write",
                "Files." + "create",
                "Files." + "delete",
                "Process" + "Builder",
                "Runtime." + "getRuntime",
                ".ex" + "ec(",
                "Http" + "Client",
                "URL" + "Connection",
                "Socket" + "(")) {
            assertFalse(source.contains(forbidden), "guard test must not use " + forbidden);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
