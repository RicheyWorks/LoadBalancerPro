package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentResult;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentScenario;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class EnterpriseLabRunService {
    public static final int DEFAULT_MAX_RETAINED_RUNS = 25;
    public static final int DEFAULT_MAX_SCENARIOS_PER_RUN = 10;
    private static final Clock DEFAULT_CLOCK = Clock.fixed(Instant.parse("2026-05-14T00:00:00Z"), ZoneOffset.UTC);
    private static final List<String> SAFETY_NOTES = List.of(
            "Enterprise Lab runs are deterministic, process-local, and bounded.",
            "Default behavior remains shadow-first; active influence is experiment output only.",
            "No CloudManager, cloud access, external network, release, tag, asset, container, or registry action is used.",
            "Run storage is process-local memory; evidence export writes only under ignored target/ paths.");

    private final EnterpriseLabScenarioCatalogService scenarioCatalogService;
    private final AdaptiveRoutingExperimentService experimentService;
    private final Clock clock;
    private final int maxRetainedRuns;
    private final int maxScenariosPerRun;
    private final AtomicLong runSequence = new AtomicLong();
    private final Map<String, EnterpriseLabRun> retainedRuns = new LinkedHashMap<>();
    private final ArrayDeque<String> retainedRunOrder = new ArrayDeque<>();

    public EnterpriseLabRunService() {
        this(new EnterpriseLabScenarioCatalogService(), new AdaptiveRoutingExperimentService(),
                DEFAULT_CLOCK, DEFAULT_MAX_RETAINED_RUNS, DEFAULT_MAX_SCENARIOS_PER_RUN);
    }

    EnterpriseLabRunService(EnterpriseLabScenarioCatalogService scenarioCatalogService,
                            AdaptiveRoutingExperimentService experimentService,
                            Clock clock,
                            int maxRetainedRuns,
                            int maxScenariosPerRun) {
        this.scenarioCatalogService = scenarioCatalogService;
        this.experimentService = experimentService;
        this.clock = clock;
        this.maxRetainedRuns = Math.max(1, maxRetainedRuns);
        this.maxScenariosPerRun = Math.max(1, maxScenariosPerRun);
    }

    public List<EnterpriseLabScenarioMetadata> listScenarioMetadata() {
        return scenarioCatalogService.listScenarioMetadata();
    }

    public Optional<EnterpriseLabScenarioMetadata> findScenarioMetadata(String scenarioId) {
        return scenarioCatalogService.findScenarioMetadata(scenarioId);
    }

    public synchronized EnterpriseLabRun run(List<String> scenarioIds, String modeValue, String detailLevel) {
        EnterpriseLabMode mode = EnterpriseLabMode.from(modeValue);
        List<AdaptiveRoutingExperimentScenario> selectedScenarios =
                scenarioCatalogService.resolveScenarios(scenarioIds, maxScenariosPerRun);
        boolean activeInfluenceEnabled = mode.activeInfluenceEnabled();
        List<AdaptiveRoutingExperimentResult> results = selectedScenarios.stream()
                .sorted(Comparator.comparing(AdaptiveRoutingExperimentScenario::name))
                .map(scenario -> experimentService.evaluate(scenario, activeInfluenceEnabled))
                .toList();
        EnterpriseLabScorecard scorecard = scorecard(results, mode);
        String runId = "lab-run-%04d".formatted(runSequence.incrementAndGet());
        EnterpriseLabRun run = new EnterpriseLabRun(
                runId,
                clock.instant(),
                mode.wireValue(),
                activeInfluenceEnabled,
                results.stream().map(AdaptiveRoutingExperimentResult::scenarioName).toList(),
                results,
                scorecard,
                SAFETY_NOTES,
                "process-local in-memory bounded store",
                maxRetainedRuns,
                maxScenariosPerRun);
        retain(run);
        return run;
    }

    public synchronized Optional<EnterpriseLabRun> findRun(String runId) {
        return Optional.ofNullable(retainedRuns.get(runId));
    }

    public synchronized List<EnterpriseLabRunSummary> listRunSummaries() {
        List<EnterpriseLabRunSummary> summaries = new ArrayList<>();
        for (String runId : retainedRunOrder) {
            EnterpriseLabRun run = retainedRuns.get(runId);
            if (run != null) {
                summaries.add(EnterpriseLabRunSummary.from(run));
            }
        }
        return List.copyOf(summaries);
    }

    public int maxRetainedRuns() {
        return maxRetainedRuns;
    }

    public int maxScenariosPerRun() {
        return maxScenariosPerRun;
    }

    private void retain(EnterpriseLabRun run) {
        retainedRuns.put(run.runId(), run);
        retainedRunOrder.addLast(run.runId());
        while (retainedRunOrder.size() > maxRetainedRuns) {
            String evicted = retainedRunOrder.removeFirst();
            retainedRuns.remove(evicted);
        }
    }

    private static EnterpriseLabScorecard scorecard(List<AdaptiveRoutingExperimentResult> results,
                                                    EnterpriseLabMode mode) {
        int baselineVsShadow = 0;
        int baselineVsInfluence = 0;
        int guardrailBlocked = 0;
        int allUnhealthyBlocked = 0;
        int staleConflictingBlocked = 0;
        int explanationCoverage = 0;
        for (AdaptiveRoutingExperimentResult result : results) {
            if (result.shadowRecommendedBackend() != null
                    && !result.shadowRecommendedBackend().equals(result.baselineSelectedBackend())) {
                baselineVsShadow++;
            }
            if (result.resultChanged()) {
                baselineVsInfluence++;
            } else if (isInfluenceGuardrail(result)) {
                guardrailBlocked++;
            }
            String pressure = result.expectedPressure().toLowerCase();
            if (pressure.contains("all unhealthy") && !result.resultChanged()) {
                allUnhealthyBlocked++;
            }
            if ((pressure.contains("stale") || pressure.contains("conflicting")) && !result.resultChanged()) {
                staleConflictingBlocked++;
            }
            if (!result.explanation().isBlank() && !result.guardrailReason().isBlank()) {
                explanationCoverage++;
            }
        }
        return new EnterpriseLabScorecard(
                results.size(),
                baselineVsShadow,
                baselineVsInfluence,
                guardrailBlocked,
                allUnhealthyBlocked,
                staleConflictingBlocked,
                explanationCoverage,
                explanationCoverage + "/" + results.size(),
                results.size(),
                mode.wireValue(),
                "lab evidence only / not production activation",
                true);
    }

    private static boolean isInfluenceGuardrail(AdaptiveRoutingExperimentResult result) {
        String guardrail = result.guardrailReason().toLowerCase();
        return guardrail.contains("default shadow")
                || guardrail.contains("stale")
                || guardrail.contains("unavailable")
                || guardrail.contains("no shadow")
                || guardrail.contains("baseline already");
    }
}
