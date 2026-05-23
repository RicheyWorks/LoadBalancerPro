package com.richmond423.loadbalancerpro.lab;

import java.util.List;

record LocalLabTrafficMatrixReviewerChecklist(
        String checklistId,
        String matrixSummaryId,
        List<LocalLabTrafficMatrixReviewerChecklistItem> items) {

    LocalLabTrafficMatrixReviewerChecklist {
        requireText("checklistId", checklistId);
        requireText("matrixSummaryId", matrixSummaryId);
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items are required");
        }
        items = List.copyOf(items);
        for (LocalLabTrafficMatrixReviewerChecklistItem item : items) {
            if (!matrixSummaryId.equals(item.matrixSummaryId())) {
                throw new IllegalArgumentException("item matrixSummaryId must match checklist");
            }
        }
    }

    String deterministicText() {
        return String.join(" ",
                checklistId,
                matrixSummaryId,
                String.join(" | ", items.stream()
                        .map(LocalLabTrafficMatrixReviewerChecklistItem::deterministicText)
                        .toList()));
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
