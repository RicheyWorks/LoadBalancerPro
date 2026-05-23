package com.richmond423.loadbalancerpro.lab;

import java.util.List;

record LocalLabPassiveReviewerChecklist(
        String checklistId,
        String scenarioId,
        String transcriptId,
        List<LocalLabPassiveReviewerChecklistItem> items) {

    LocalLabPassiveReviewerChecklist {
        requireText("checklistId", checklistId);
        requireText("scenarioId", scenarioId);
        requireText("transcriptId", transcriptId);
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items are required");
        }
        items = List.copyOf(items);
        validateItems(scenarioId, transcriptId, items);
    }

    String deterministicText() {
        return String.join(" ",
                checklistId,
                scenarioId,
                transcriptId,
                String.join(" | ", items.stream()
                        .map(LocalLabPassiveReviewerChecklistItem::deterministicText)
                        .toList()));
    }

    private static void validateItems(
            String scenarioId,
            String transcriptId,
            List<LocalLabPassiveReviewerChecklistItem> items) {
        for (LocalLabPassiveReviewerChecklistItem item : items) {
            if (!scenarioId.equals(item.scenarioId())) {
                throw new IllegalArgumentException("item scenarioId must match checklist");
            }
            if (!transcriptId.equals(item.transcriptId())) {
                throw new IllegalArgumentException("item transcriptId must match checklist");
            }
        }
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
