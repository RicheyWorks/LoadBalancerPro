package api;

import java.util.List;

public record ApiErrorResponse(String error, String message, List<String> details) {
    public static ApiErrorResponse badRequest(String message) {
        return new ApiErrorResponse("bad_request", message, List.of());
    }

    public static ApiErrorResponse validation(List<String> details) {
        return new ApiErrorResponse("validation_failed", "Request validation failed", details);
    }
}
