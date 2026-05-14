package com.richmond423.loadbalancerpro.lab;

public record EnterpriseLabScorecard(
        int totalScenarios,
        int baselineVsShadowDifferences,
        int baselineVsInfluenceDifferences,
        int guardrailBlockedInfluenceCount,
        int unsafeAllUnhealthyBlockedCount,
        int staleConflictingSignalBlockedCount,
        int explanationCoverageCount,
        String explanationCoverage,
        int deterministicFixtureCount,
        String modeUsed,
        String finalRecommendation,
        boolean labEvidenceOnly) {
}

