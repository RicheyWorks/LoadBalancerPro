package com.richmond423.loadbalancerpro.api.proxy;

import java.util.List;

import org.springframework.http.HttpStatus;

public record ReverseProxyAdminMutationResponse(
        boolean success,
        String status,
        String action,
        String upstreamId,
        long generation,
        List<String> errors,
        ReverseProxyAdminConfigResponse config) {

    static ReverseProxyAdminMutationResponse success(
            String action,
            String upstreamId,
            ReverseProxyAdminConfigResponse config) {
        return new ReverseProxyAdminMutationResponse(
                true, "success", action, upstreamId, config.generation(), List.of(), config);
    }

    static ReverseProxyAdminMutationResponse rejected(
            String status,
            String action,
            String upstreamId,
            ReverseProxyAdminConfigResponse config,
            List<String> errors) {
        return new ReverseProxyAdminMutationResponse(
                false, status, action, upstreamId, config.generation(), List.copyOf(errors), config);
    }

    HttpStatus httpStatus() {
        return switch (status) {
            case "generation_conflict", "unavailable" -> HttpStatus.CONFLICT;
            case "not_found" -> HttpStatus.NOT_FOUND;
            default -> success ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        };
    }
}
