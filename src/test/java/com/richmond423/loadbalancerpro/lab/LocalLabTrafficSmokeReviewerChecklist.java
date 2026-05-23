package com.richmond423.loadbalancerpro.lab;

import java.util.List;

record LocalLabTrafficSmokeReviewerChecklist(
        String checklistId,
        String smokeSummaryId,
        List<LocalLabTrafficSmokeReviewerChecklistItem> items) {

    LocalLabTrafficSmokeReviewerChecklist {
        requireText("checklistId", checklistId);
        requireText("smokeSummaryId", smokeSummaryId);
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items are required");
        }
        items = List.copyOf(items);
        for (LocalLabTrafficSmokeReviewerChecklistItem item : items) {
            if (!smokeSummaryId.equals(item.smokeSummaryId())) {
                throw new IllegalArgumentException("item smokeSummaryId must match checklist");
            }
        }
    }

    String deterministicText() {
        return String.join(" ",
                checklistId,
                smokeSummaryId,
                String.join(" | ", items.stream()
                        .map(LocalLabTrafficSmokeReviewerChecklistItem::deterministicText)
                        .toList()));
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
