package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentResult;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentScenario;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentService;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingObservabilityMetrics;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingObservabilitySnapshot;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyAuditEvent;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyAuditLog;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyStatus;

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
            "Default behavior remains off/shadow-first; active-experiment influence is explicit lab output only.",
            "No CloudManager, cloud access, external network, release, tag, asset, container, or registry action is used.",
            "Run storage is process-local memory; evidence export writes only under ignored target/ paths.");

    private final EnterpriseLabScenarioCatalogService scenarioCatalogService;
    private final AdaptiveRoutingExperimentService experimentService;
    private final Clock clock;
    private final int maxRetainedRuns;
    private final int maxScenariosPerRun;
    private final AdaptiveRoutingPolicyAuditLog policyAuditLog;
    private final AdaptiveRoutingObservabilityMetrics observabilityMetrics;
    private final AtomicLong runSequence = new AtomicLong();
    private final Map<String, EnterpriseLabRun> retainedRuns = new LinkedHashMap<>();
    private final ArrayDeque<String> retainedRunOrder = new ArrayDeque<>();

    public EnterpriseLabRunService() {
        this(new AdaptiveRoutingObservabilityMetrics());
    }

    private EnterpriseLabRunService(AdaptiveRoutingObservabilityMetrics observabilityMetrics) {
        this(new EnterpriseLabScenarioCatalogService(), new AdaptiveRoutingExperimentService(),
                DEFAULT_CLOCK, DEFAULT_MAX_RETAINED_RUNS, DEFAULT_MAX_SCENARIOS_PER_RUN,
                new AdaptiveRoutingPolicyAuditLog(AdaptiveRoutingPolicyAuditLog.DEFAULT_MAX_EVENTS, DEFAULT_CLOCK,
                        observabilityMetrics),
                observabilityMetrics);
    }

    public EnterpriseLabRunService(EnterpriseLabScenarioCatalogService scenarioCatalogService,
                                   AdaptiveRoutingExperimentService experimentService,
                                   Clock clock,
                                   int maxRetainedRuns,
                                   int maxScenariosPerRun,
                                   AdaptiveRoutingPolicyAuditLog policyAuditLog) {
        this(scenarioCatalogService, experimentService, clock, maxRetainedRuns, maxScenariosPerRun,
                policyAuditLog, new AdaptiveRoutingObservabilityMetrics());
    }

    public EnterpriseLabRunService(EnterpriseLabScenarioCatalogService scenarioCatalogService,
                                   AdaptiveRoutingExperimentService experimentService,
                                   Clock clock,
                                   int maxRetainedRuns,
                                   int maxScenariosPerRun,
                                   AdaptiveRoutingPolicyAuditLog policyAuditLog,
                                   AdaptiveRoutingObservabilityMetrics observabilityMetrics) {
        this.scenarioCatalogService = scenarioCatalogService;
        this.experimentService = experimentService;
        this.clock = clock;
        this.maxRetainedRuns = Math.max(1, maxRetainedRuns);
        this.maxScenariosPerRun = Math.max(1, maxScenariosPerRun);
        this.observabilityMetrics = observabilityMetrics == null
                ? new AdaptiveRoutingObservabilityMetrics()
                : observabilityMetrics;
        this.policyAuditLog = policyAuditLog == null
                ? new AdaptiveRoutingPolicyAuditLog(
                        AdaptiveRoutingPolicyAuditLog.DEFAULT_MAX_EVENTS, clock, this.observabilityMetrics)
                : policyAuditLog;
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
        boolean activeInfluenceEnabled = mode.policyMode() == AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT;
        List<AdaptiveRoutingExperimentResult> results = selectedScenarios.stream()
                .sorted(Comparator.comparing(AdaptiveRoutingExperimentScenario::name))
                .map(scenario -> experimentService.evaluate(scenario, mode.policyMode()))
                .toList();
        List<AdaptiveRoutingPolicyAuditEvent> policyAuditEvents = results.stream()
                .map(result -> policyAuditLog.record(result.policyDecision()))
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
                policyAuditEvents,
                scorecard,
                SAFETY_NOTES,
                "process-local in-memory bounded store",
                maxRetainedRuns,
                maxScenariosPerRun);
        observabilityMetrics.recordLabRun(mode.wireValue(), results.size(), scorecard.explanationCoverageCount());
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

    public AdaptiveRoutingPolicyStatus policyStatus(String configuredMode, boolean activeExperimentEnabled) {
        AdaptiveRoutingPolicyMode configured = AdaptiveRoutingPolicyMode.fromOrOff(configuredMode);
        AdaptiveRoutingPolicyMode current = configured == AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT
                && !activeExperimentEnabled ? AdaptiveRoutingPolicyMode.OFF : configured;
        String lastGuardrail = policyAuditLog.lastEvent()
                .map(event -> String.join("; ", event.guardrailReasons()))
                .orElse("no policy decisions recorded yet");
        return new AdaptiveRoutingPolicyStatus(
                configured.wireValue(),
                current.wireValue(),
                activeExperimentEnabled,
                AdaptiveRoutingPolicyMode.wireValues(),
                policyAuditLog.size(),
                policyAuditLog.maxEvents(),
                lastGuardrail,
                "active-experiment is explicit, bounded, auditable lab evidence; it is not production certification");
    }

    public List<AdaptiveRoutingPolicyAuditEvent> policyAuditEvents() {
        return policyAuditLog.snapshot();
    }

    public AdaptiveRoutingObservabilitySnapshot observabilitySnapshot() {
        return observabilityMetrics.snapshot();
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
                mode.policyMode() == AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT
                        ? "controlled active-experiment evidence only / not production activation"
                        : "lab evidence only / not production activation",
                true);
    }

    private static boolean isInfluenceGuardrail(AdaptiveRoutingExperimentResult result) {
        String guardrail = result.guardrailReason().toLowerCase();
        return guardrail.contains("default shadow")
                || guardrail.contains("policy mode off")
                || guardrail.contains("shadow mode")
                || guardrail.contains("recommend mode")
                || guardrail.contains("stale")
                || guardrail.contains("conflicting")
                || guardrail.contains("capacity constraints")
                || guardrail.contains("all backends")
                || guardrail.contains("unavailable")
                || guardrail.contains("no lase")
                || guardrail.contains("no shadow")
                || guardrail.contains("baseline already");
    }
}
