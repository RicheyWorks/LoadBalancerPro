package com.richmond423.loadbalancerpro.api;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated by the existing /api/lab/** API-key and OAuth2 operator-role boundary. */
@RestController
@RequestMapping("/api/lab/allocation-supervision")
public final class EnterpriseLabAllocationSupervisionController {
    private final EnterpriseLabExperimentOperatorService operatorService;

    public EnterpriseLabAllocationSupervisionController(
            EnterpriseLabExperimentOperatorService operatorService) {
        this.operatorService = operatorService;
    }

    @GetMapping
    public ResponseEntity<?> status() {
        return operatorService.allocationSupervisionStatus()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(EnterpriseLabAllocationSupervisionController::unavailable);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(HttpServletRequest request) {
        ResponseEntity<?> rejected = rejectBody(request);
        if (rejected != null) {
            return rejected;
        }
        return operatorService.verifyAllocationSupervision()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(EnterpriseLabAllocationSupervisionController::unavailable);
    }

    @PostMapping("/restore-safe-baseline")
    public ResponseEntity<?> restoreSafeBaseline(HttpServletRequest request) {
        ResponseEntity<?> rejected = rejectBody(request);
        if (rejected != null) {
            return rejected;
        }
        return operatorService.restoreSupervisedSafeBaseline()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(EnterpriseLabAllocationSupervisionController::unavailable);
    }

    private static ResponseEntity<UnavailableResponse> unavailable() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new UnavailableResponse(
                false,
                "ALLOCATION_SUPERVISION_NOT_CONFIGURED",
                "allocation supervision requires one fixed repository-controlled loopback scenario and durable ownership"));
    }

    private static ResponseEntity<?> rejectBody(HttpServletRequest request) {
        if (request.getContentLengthLong() > 0
                || request.getHeader("Transfer-Encoding") != null) {
            return ResponseEntity.badRequest().body(new RejectedInputResponse(
                    false,
                    "ALLOCATION_SUPERVISION_INPUT_REJECTED",
                    "verification and safe restoration accept no allocation, generation, phase, path, force, or bypass input"));
        }
        return null;
    }

    public record UnavailableResponse(boolean configured, String reasonCode, String reason) {
    }

    public record RejectedInputResponse(boolean accepted, String reasonCode, String reason) {
    }
}
