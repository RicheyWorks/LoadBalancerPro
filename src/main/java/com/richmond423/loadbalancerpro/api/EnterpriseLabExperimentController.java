package com.richmond423.loadbalancerpro.api;

import com.richmond423.loadbalancerpro.api.config.AdaptiveRoutingPolicyProperties;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.ArmRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.OperatorReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.RequestBatchReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.RequestBatchRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentTargetCatalog;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated by the existing /api/lab/** API-key and OAuth2 operator-role boundary.
 */
@RestController
@RequestMapping("/api/lab/experiments")
public class EnterpriseLabExperimentController {
    private static final int DEFAULT_MAXIMUM_REQUEST_COUNT = 20;
    private static final long DEFAULT_MAXIMUM_DURATION_SECONDS = 60;
    private static final int DEFAULT_MINIMUM_EVIDENCE_COUNT = 5;
    private static final int DEFAULT_HOLD_DOWN_CYCLES = 2;
    private static final long DEFAULT_EXPIRATION_SECONDS = 120;
    private static final int DEFAULT_REQUEST_BATCH_COUNT = 1;
    private static final long DEFAULT_REQUEST_TIMEOUT_MILLIS = 1_000;

    private final EnterpriseLabExperimentOperatorService operatorService;
    private final AdaptiveRoutingPolicyProperties policyProperties;

    public EnterpriseLabExperimentController(
            EnterpriseLabExperimentOperatorService operatorService,
            AdaptiveRoutingPolicyProperties policyProperties) {
        this.operatorService = operatorService;
        this.policyProperties = policyProperties;
    }

    @PostMapping("/arm")
    public OperatorReceipt arm(@RequestBody(required = false) ArmExperimentRequest request) {
        ArmExperimentRequest safeRequest = requireBody(request);
        long maximumDurationSeconds = valueOrDefault(
                safeRequest.maximumDurationSeconds(), DEFAULT_MAXIMUM_DURATION_SECONDS);
        long expirationSeconds = valueOrDefault(
                safeRequest.expirationSeconds(), Math.max(DEFAULT_EXPIRATION_SECONDS, maximumDurationSeconds));
        ArmRequest command = new ArmRequest(
                safeRequest.operatorRequestId(),
                safeRequest.experimentId(),
                safeRequest.scenarioId(),
                valueOrDefault(safeRequest.maximumRequestCount(), DEFAULT_MAXIMUM_REQUEST_COUNT),
                Duration.ofSeconds(maximumDurationSeconds),
                valueOrDefault(safeRequest.minimumEvidenceCount(), DEFAULT_MINIMUM_EVIDENCE_COUNT),
                valueOrDefault(safeRequest.holdDownCycles(), DEFAULT_HOLD_DOWN_CYCLES),
                Duration.ofSeconds(expirationSeconds));
        return operatorService.arm(command, activeExperimentEnabled());
    }

    @PostMapping("/{experimentId}/start")
    public OperatorReceipt start(
            @PathVariable("experimentId") String experimentId,
            @RequestBody(required = false) OperatorCommandRequest request) {
        return operatorService.start(
                experimentId,
                requireBody(request).operatorRequestId(),
                activeExperimentEnabled());
    }

    @PostMapping("/{experimentId}/requests")
    public RequestBatchReceipt executeRequests(
            @PathVariable("experimentId") String experimentId,
            @RequestBody(required = false) RequestBatchApiRequest request) {
        RequestBatchApiRequest safeRequest = requireBody(request);
        RequestBatchRequest command = new RequestBatchRequest(
                safeRequest.operatorRequestId(),
                valueOrDefault(safeRequest.count(), DEFAULT_REQUEST_BATCH_COUNT),
                Duration.ofMillis(valueOrDefault(
                        safeRequest.timeoutMillis(), DEFAULT_REQUEST_TIMEOUT_MILLIS)));
        return operatorService.executeRequests(experimentId, command, activeExperimentEnabled());
    }

    @PostMapping("/{experimentId}/evaluations")
    public OperatorReceipt evaluate(
            @PathVariable("experimentId") String experimentId,
            @RequestBody(required = false) OperatorCommandRequest request) {
        return operatorService.evaluate(
                experimentId,
                requireBody(request).operatorRequestId(),
                activeExperimentEnabled());
    }

    @PostMapping("/{experimentId}/cancel")
    public OperatorReceipt cancel(
            @PathVariable("experimentId") String experimentId,
            @RequestBody(required = false) CancelExperimentRequest request) {
        CancelExperimentRequest safeRequest = requireBody(request);
        return operatorService.cancel(experimentId, safeRequest.operatorRequestId(), safeRequest.reason());
    }

    @GetMapping("/{experimentId}")
    public ResponseEntity<?> status(
            @PathVariable("experimentId") String experimentId,
            HttpServletRequest request) {
        return operatorService.findRecord(experimentId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiErrorResponse.notFound(
                                "Unknown Enterprise Lab experiment: " + experimentId,
                                request.getRequestURI())));
    }

    @GetMapping("/{experimentId}/record")
    public ResponseEntity<?> finalRecord(
            @PathVariable("experimentId") String experimentId,
            HttpServletRequest request) {
        Optional<EnterpriseLabExperimentOperatorRecord> current = operatorService.findRecord(experimentId);
        if (current.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.notFound(
                            "Unknown Enterprise Lab experiment: " + experimentId,
                            request.getRequestURI()));
        }
        Optional<EnterpriseLabExperimentOperatorRecord> finalRecord = operatorService.findFinalRecord(experimentId);
        if (finalRecord.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new FinalRecordResponse(
                            false,
                            current,
                            "EXPERIMENT_NOT_TERMINAL",
                            "final evidence is available only after completion, rollback, rejection, failure, or cancellation"));
        }
        return ResponseEntity.ok(new FinalRecordResponse(
                true,
                finalRecord,
                "FINAL_RECORD_AVAILABLE",
                "immutable bounded final experiment evidence is available"));
    }

    @GetMapping
    public ExperimentListResponse experiments() {
        List<EnterpriseLabExperimentOperatorRecord> records = operatorService.records();
        return new ExperimentListResponse(
                records.size(),
                records,
                operatorService.maxRetainedExperiments(),
                operatorService.boundScenarioIds(),
                activeExperimentEnabled(),
                "request bodies cannot supply or reveal backend target addresses");
    }

    private boolean activeExperimentEnabled() {
        return policyProperties.isActiveExperimentEnabled()
                && policyProperties.configuredMode() == AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT;
    }

    private static int valueOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static long valueOrDefault(Long value, long defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static <T> T requireBody(T body) {
        if (body == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        return body;
    }

    public record ArmExperimentRequest(
            String operatorRequestId,
            String experimentId,
            String scenarioId,
            Integer maximumRequestCount,
            Long maximumDurationSeconds,
            Integer minimumEvidenceCount,
            Integer holdDownCycles,
            Long expirationSeconds) {
    }

    public record OperatorCommandRequest(String operatorRequestId) {
    }

    public record RequestBatchApiRequest(String operatorRequestId, Integer count, Long timeoutMillis) {
    }

    public record CancelExperimentRequest(String operatorRequestId, String reason) {
    }

    public record FinalRecordResponse(
            boolean finalRecordAvailable,
            Optional<EnterpriseLabExperimentOperatorRecord> experimentRecord,
            String reasonCode,
            String reason) {
    }

    public record ExperimentListResponse(
            int count,
            List<EnterpriseLabExperimentOperatorRecord> experiments,
            int maxRetainedExperiments,
            List<String> boundScenarioIds,
            boolean activeExperimentEnabled,
            String targetBoundary) {
    }

    @Configuration
    static class EnterpriseLabExperimentConfiguration {
        @Bean
        @ConditionalOnMissingBean(EnterpriseLabExperimentTargetCatalog.class)
        EnterpriseLabExperimentTargetCatalog enterpriseLabExperimentTargetCatalog() {
            return EnterpriseLabExperimentTargetCatalog.empty();
        }

        @Bean(destroyMethod = "close")
        @ConditionalOnMissingBean(EnterpriseLabExperimentOperatorService.class)
        EnterpriseLabExperimentOperatorService enterpriseLabExperimentOperatorService(
                EnterpriseLabExperimentTargetCatalog targetCatalog) {
            return new EnterpriseLabExperimentOperatorService(targetCatalog);
        }
    }
}
