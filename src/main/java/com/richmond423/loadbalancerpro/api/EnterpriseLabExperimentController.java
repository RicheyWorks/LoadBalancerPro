package com.richmond423.loadbalancerpro.api;

import com.richmond423.loadbalancerpro.api.config.AdaptiveRoutingPolicyProperties;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalDirectory;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentDurableEvidenceRepository;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayEngine;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalStorageException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalVerifier;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentTerminalManifest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.ArmRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.OperatorReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.RequestBatchReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.RequestBatchRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentRecoveryGate;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentTargetCatalog;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabProcessLocalAllocationRecovery;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipLease;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
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

    @GetMapping("/durable")
    public ResponseEntity<?> durableJournals() {
        return durableEvidence().<ResponseEntity<?>>map(repository -> {
            List<DurableJournalSummary> journals = repository.discover().stream()
                    .map(DurableJournalSummary::from)
                    .toList();
            return ResponseEntity.ok(new DurableJournalListResponse(
                    journals.size(), journals,
                    EnterpriseLabExperimentJournalDirectory.HARD_MAX_DISCOVERED_JOURNALS,
                    "sanitized summaries only; no filesystem paths or raw journal bytes"));
        }).orElseGet(this::durableUnavailable);
    }

    @GetMapping("/durable/recovery")
    public ResponseEntity<?> durableRecoveryStatus() {
        return durableEvidence().<ResponseEntity<?>>map(repository ->
                ResponseEntity.ok(repository.recoveryStatus()))
                .orElseGet(this::durableUnavailable);
    }

    @GetMapping("/durable/{experimentId}/verification")
    public ResponseEntity<?> durableVerification(
            @PathVariable("experimentId") String experimentId) {
        return durableEvidence().<ResponseEntity<?>>map(repository ->
                ResponseEntity.ok(VerificationSummary.from(repository.verify(experimentId))))
                .orElseGet(this::durableUnavailable);
    }

    @PostMapping("/durable/{experimentId}/verify")
    public ResponseEntity<?> requestDurableVerification(
            @PathVariable("experimentId") String experimentId) {
        return durableVerification(experimentId);
    }

    @GetMapping("/durable/{experimentId}/export")
    public ResponseEntity<?> exportDurableEvidence(
            @PathVariable("experimentId") String experimentId) {
        return durableEvidence().<ResponseEntity<?>>map(repository -> {
            EnterpriseLabExperimentJournalVerifier.VerificationResult verification =
                    repository.verify(experimentId);
            EnterpriseLabExperimentJournalReplayEngine.ReplayResult replay =
                    repository.replay(experimentId);
            return ResponseEntity.ok(new DurableEvidenceExport(
                    VerificationSummary.from(verification),
                    replay.outcome(), replay.classification(), replay.reconstructedState(),
                    "bounded reconstructed evidence; raw source bytes and backing paths are not exported"));
        }).orElseGet(this::durableUnavailable);
    }

    @PostMapping("/durable/{experimentId}/compact")
    public ResponseEntity<?> compactTerminalEvidence(
            @PathVariable("experimentId") String experimentId) {
        return durableEvidence().<ResponseEntity<?>>map(repository -> {
            try {
                return ResponseEntity.ok(repository.compactTerminal(experimentId));
            } catch (EnterpriseLabExperimentJournalStorageException exception) {
                return durableActionRejected(exception);
            }
        })
                .orElseGet(this::durableUnavailable);
    }

    @GetMapping("/durable/compacted")
    public ResponseEntity<?> compactedEvidence() {
        return durableEvidence().<ResponseEntity<?>>map(repository -> {
            List<EnterpriseLabExperimentTerminalManifest> manifests = repository.compactedManifests();
            return ResponseEntity.ok(new CompactedEvidenceResponse(
                    manifests.size(), manifests,
                    EnterpriseLabExperimentJournalDirectory.HARD_MAX_DISCOVERED_JOURNALS));
        }).orElseGet(this::durableUnavailable);
    }

    @GetMapping("/durable/quarantine")
    public ResponseEntity<?> quarantineEvidence() {
        return durableEvidence().<ResponseEntity<?>>map(repository -> {
            List<EnterpriseLabExperimentJournalDirectory.QuarantineMetadata> entries =
                    repository.quarantineMetadata();
            return ResponseEntity.ok(new QuarantineEvidenceResponse(
                    entries.size(), entries,
                    "forensic bytes retained; no raw content or backing path is exposed"));
        }).orElseGet(this::durableUnavailable);
    }

    @PostMapping("/durable/retention")
    public ResponseEntity<?> enforceDurableRetention(
            @RequestBody(required = false) RetentionRequest request) {
        RetentionRequest safe = requireBody(request);
        return durableEvidence().<ResponseEntity<?>>map(repository -> {
            try {
                return ResponseEntity.ok(
                        repository.enforceRetention(safe.maximumTerminalJournals(), safe.dryRun()));
            } catch (EnterpriseLabExperimentJournalStorageException exception) {
                return durableActionRejected(exception);
            }
        })
                .orElseGet(this::durableUnavailable);
    }

    private Optional<EnterpriseLabExperimentDurableEvidenceRepository> durableEvidence() {
        return operatorService.durableEvidence();
    }

    private ResponseEntity<?> durableUnavailable() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new DurableUnavailableResponse(
                false,
                "DURABLE_EVIDENCE_NOT_CONFIGURED",
                "set the explicit local experiment journal data directory before using durable evidence APIs"));
    }

    private ResponseEntity<?> durableActionRejected(
            EnterpriseLabExperimentJournalStorageException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new DurableActionResponse(
                false,
                exception.failure().name(),
                "durable evidence action was rejected without changing unresolved or active history"));
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

    public record DurableUnavailableResponse(boolean configured, String reasonCode, String reason) {
    }

    public record DurableActionResponse(boolean applied, String reasonCode, String reason) {
    }

    public record DurableJournalListResponse(
            int count,
            List<DurableJournalSummary> journals,
            int maximumCount,
            String evidenceBoundary) {
    }

    public record DurableJournalSummary(
            String journalId,
            Optional<String> experimentId,
            EnterpriseLabExperimentJournalDirectory.DiscoveryOutcome outcome,
            String classification,
            Optional<EnterpriseLabExperimentJournalVerifier.Outcome> verificationOutcome,
            int verifiedEventCount,
            long totalBytes) {
        private static DurableJournalSummary from(
                EnterpriseLabExperimentJournalDirectory.JournalDiscovery discovery) {
            return new DurableJournalSummary(
                    discovery.journalId(), discovery.experimentId(), discovery.outcome(),
                    discovery.classification(),
                    discovery.verification().map(
                            EnterpriseLabExperimentJournalVerifier.VerificationResult::outcome),
                    discovery.verification().map(value -> value.verifiedEvents().size()).orElse(0),
                    discovery.verification().map(
                            EnterpriseLabExperimentJournalVerifier.VerificationResult::totalBytes).orElse(0L));
        }
    }

    public record VerificationSummary(
            String journalId,
            EnterpriseLabExperimentJournalVerifier.Outcome outcome,
            EnterpriseLabExperimentJournalVerifier.Classification classification,
            int verifiedEventCount,
            long completeBytes,
            long tailBytes,
            long totalBytes,
            List<EnterpriseLabExperimentJournalVerifier.Finding> findings) {
        private static VerificationSummary from(
                EnterpriseLabExperimentJournalVerifier.VerificationResult value) {
            return new VerificationSummary(
                    value.journalId(), value.outcome(), value.classification(),
                    value.verifiedEvents().size(), value.completeBytes(), value.tailBytes(),
                    value.totalBytes(), value.findings());
        }
    }

    public record DurableEvidenceExport(
            VerificationSummary verification,
            EnterpriseLabExperimentJournalReplayEngine.Outcome replayOutcome,
            EnterpriseLabExperimentJournalReplayEngine.Classification replayClassification,
            Optional<EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState>
                    reconstructedState,
            String evidenceBoundary) {
    }

    public record CompactedEvidenceResponse(
            int count,
            List<EnterpriseLabExperimentTerminalManifest> manifests,
            int maximumCount) {
    }

    public record QuarantineEvidenceResponse(
            int count,
            List<EnterpriseLabExperimentJournalDirectory.QuarantineMetadata> entries,
            String evidenceBoundary) {
    }

    public record RetentionRequest(int maximumTerminalJournals, boolean dryRun) {
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
                EnterpriseLabExperimentTargetCatalog targetCatalog,
                @Value("${loadbalancer.enterprise-lab.experiment-journal-data-directory:}")
                String journalDataDirectory) {
            if (journalDataDirectory == null || journalDataDirectory.isBlank()) {
                return new EnterpriseLabExperimentOperatorService(targetCatalog);
            }
            if (!journalDataDirectory.equals(journalDataDirectory.trim())) {
                throw new IllegalArgumentException(
                        "experiment journal data directory must not contain surrounding whitespace");
            }
            Path trustedRoot = Path.of(journalDataDirectory).normalize();
            if (!trustedRoot.isAbsolute()) {
                throw new IllegalArgumentException(
                        "experiment journal data directory must be an explicit absolute path");
            }
            EnterpriseLabExperimentRecoveryGate recoveryGate =
                    EnterpriseLabExperimentRecoveryGate.pending();
            Clock clock = Clock.systemUTC();
            EnterpriseLabEvidenceOwnership.Policy ownershipPolicy =
                    EnterpriseLabEvidenceOwnership.Policy.safetyFirstDefaults();
            EnterpriseLabProcessLocalAllocationRecovery allocationRecovery =
                    new EnterpriseLabProcessLocalAllocationRecovery(targetCatalog);

            EnterpriseLabEvidenceOwnershipLease ownership;
            var acquisition = EnterpriseLabEvidenceOwnershipManager.acquire(
                    trustedRoot, ownershipPolicy, clock);
            if (acquisition.result().status()
                    == EnterpriseLabEvidenceOwnership.OperationStatus.SUCCEEDED) {
                ownership = acquisition.ownership().orElseThrow();
            } else if (acquisition.result().failure()
                    == EnterpriseLabEvidenceOwnership.FailureClassification.TAKEOVER_NOT_PERMITTED) {
                EnterpriseLabExperimentJournalDirectory inspectionDirectory =
                        EnterpriseLabExperimentJournalDirectory.create(trustedRoot);
                var takeover = EnterpriseLabEvidenceOwnershipManager.takeover(
                        trustedRoot,
                        ownershipPolicy,
                        clock,
                        new EnterpriseLabExperimentStartupReconciler(
                                inspectionDirectory,
                                allocationRecovery,
                                recoveryGate,
                                clock));
                if (takeover.result().status()
                        != EnterpriseLabEvidenceOwnership.OperationStatus.SUCCEEDED) {
                    throw new IllegalStateException(
                            "Enterprise Lab ownership takeover failed closed: "
                                    + takeover.result().failure().name()
                                    + "/" + takeover.result().reasonCode());
                }
                ownership = takeover.ownership().orElseThrow();
            } else {
                throw new IllegalStateException(
                        "Enterprise Lab ownership acquisition failed closed: "
                                + acquisition.result().failure().name()
                                + "/" + acquisition.result().reasonCode());
            }

            try {
                EnterpriseLabExperimentJournalDirectory directory =
                        EnterpriseLabExperimentJournalDirectory.create(
                                trustedRoot, ownership.ownershipGate());
                new EnterpriseLabExperimentStartupReconciler(
                        directory,
                        allocationRecovery,
                        recoveryGate,
                        clock).initialize();
                EnterpriseLabExperimentDurableEvidenceRepository durableEvidence =
                        new EnterpriseLabExperimentDurableEvidenceRepository(
                                directory, recoveryGate, clock);
                return new EnterpriseLabExperimentOperatorService(
                        targetCatalog, recoveryGate, durableEvidence, ownership);
            } catch (RuntimeException exception) {
                try {
                    ownership.close();
                } catch (RuntimeException releaseFailure) {
                    exception.addSuppressed(releaseFailure);
                }
                throw exception;
            }
        }
    }
}
