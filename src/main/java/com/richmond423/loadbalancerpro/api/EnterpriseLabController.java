package com.richmond423.loadbalancerpro.api;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabRun;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabRunService;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabRunSummary;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabScenarioMetadata;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
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

@RestController
@RequestMapping("/api/lab")
public class EnterpriseLabController {
    private final EnterpriseLabRunService runService;

    public EnterpriseLabController(EnterpriseLabRunService runService) {
        this.runService = runService;
    }

    @GetMapping("/scenarios")
    public EnterpriseLabScenarioCatalogResponse scenarios() {
        List<EnterpriseLabScenarioMetadata> scenarios = runService.listScenarioMetadata();
        return new EnterpriseLabScenarioCatalogResponse(
                scenarios.size(),
                scenarios,
                "adaptive-routing-fixtures-v1",
                List.of("shadow", "influence", "all"),
                "lab evidence only / not production activation");
    }

    @GetMapping("/scenarios/{scenarioId}")
    public ResponseEntity<?> scenario(@PathVariable("scenarioId") String scenarioId, HttpServletRequest request) {
        return runService.findScenarioMetadata(scenarioId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiErrorResponse.notFound("Unknown enterprise lab scenario: " + scenarioId,
                                request.getRequestURI())));
    }

    @PostMapping("/runs")
    public EnterpriseLabRun createRun(@RequestBody(required = false) EnterpriseLabRunRequest request) {
        EnterpriseLabRunRequest safeRequest = request == null
                ? new EnterpriseLabRunRequest(null, "all", "summary")
                : request;
        return runService.run(safeRequest.scenarioIds(), safeRequest.mode(), safeRequest.detailLevel());
    }

    @GetMapping("/runs")
    public EnterpriseLabRunListResponse runs() {
        List<EnterpriseLabRunSummary> runs = runService.listRunSummaries();
        return new EnterpriseLabRunListResponse(
                runs.size(),
                runs,
                "process-local in-memory bounded store",
                runService.maxRetainedRuns());
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<?> run(@PathVariable("runId") String runId, HttpServletRequest request) {
        return runService.findRun(runId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiErrorResponse.notFound("Unknown enterprise lab run: " + runId,
                                request.getRequestURI())));
    }

    public record EnterpriseLabScenarioCatalogResponse(
            int count,
            List<EnterpriseLabScenarioMetadata> scenarios,
            String deterministicFixtureVersion,
            List<String> supportedModes,
            String finalRecommendation) {
    }

    public record EnterpriseLabRunRequest(
            List<String> scenarioIds,
            String mode,
            String detailLevel) {
    }

    public record EnterpriseLabRunListResponse(
            int count,
            List<EnterpriseLabRunSummary> runs,
            String storageMode,
            int maxRetainedRuns) {
    }

    @Configuration
    static class EnterpriseLabConfiguration {
        @Bean
        EnterpriseLabRunService enterpriseLabRunService() {
            return new EnterpriseLabRunService();
        }
    }
}
