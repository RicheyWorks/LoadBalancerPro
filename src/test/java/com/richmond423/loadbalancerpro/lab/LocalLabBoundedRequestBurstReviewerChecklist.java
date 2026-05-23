package com.richmond423.loadbalancerpro.lab;

import java.util.List;

record LocalLabBoundedRequestBurstReviewerChecklist(
        String checklistId,
        String burstSummaryId,
        List<LocalLabBoundedRequestBurstReviewerChecklistItem> items) {

    LocalLabBoundedRequestBurstReviewerChecklist {
        requireText("checklistId", checklistId);
        requireText("burstSummaryId", burstSummaryId);
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items are required");
        }
        items = List.copyOf(items);
        for (LocalLabBoundedRequestBurstReviewerChecklistItem item : items) {
            if (!burstSummaryId.equals(item.burstSummaryId())) {
                throw new IllegalArgumentException("item burstSummaryId must match checklist");
            }
        }
    }

    String deterministicText() {
        return String.join(" ",
                checklistId,
                burstSummaryId,
                String.join(" | ", items.stream()
                        .map(LocalLabBoundedRequestBurstReviewerChecklistItem::deterministicText)
                        .toList()));
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
