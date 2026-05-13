package com.richmond423.loadbalancerpro.api.proxy;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Bounded executor primitive for future private-network live validation.
 *
 * <p>The executor is deliberately not registered as a Spring component and is not called
 * from startup, Postman, smoke scripts, or proxy routing. It does not resolve DNS,
 * discover targets, scan ports, or create a network client by itself. It requires an allowed
 * {@link PrivateNetworkLiveValidationGate} result before delegating one request to the
 * supplied transport.</p>
 */
public final class PrivateNetworkLiveValidationExecutor {
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2);
    public static final Duration MAX_TIMEOUT = Duration.ofSeconds(2);
    private static final int BODY_SNIPPET_LIMIT = 4_096;
    private static final Map<String, String> ALLOWED_REQUEST_HEADERS = Map.of(
            "x-loadbalancerpro-live-validation", "X-LoadBalancerPro-Live-Validation",
            "x-reviewer-trace", "X-Reviewer-Trace",
            "accept", "Accept");
    private static final Set<String> ALLOWED_RESPONSE_HEADERS = Set.of(
            "content-type",
            "x-private-network-live-proof",
            "x-loadbalancerpro-live-validation-result");

    private final PrivateNetworkLiveValidationGate gate;
    private final Transport transport;
    private final Duration timeout;

    public PrivateNetworkLiveValidationExecutor(PrivateNetworkLiveValidationGate gate,
                                                Transport transport,
                                                Duration timeout) {
        this.gate = Objects.requireNonNull(gate, "gate");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.timeout = boundedTimeout(timeout);
    }

    public Result executeFirstAllowed(ReverseProxyProperties properties, ValidationRequest request) {
        PrivateNetworkLiveValidationGate.Result gateResult = gate.evaluate(properties);
        if (!gateResult.allowed()) {
            return Result.blocked(gateResult.status(), gateResult.reasons(), timeout);
        }

        if (request == null) {
            return Result.invalidRequest(List.of("validation request is required"), timeout);
        }
        ValidationRequest normalizedRequest = request;
        String invalidRequestReason = invalidRequestReason(normalizedRequest.pathAndQuery());
        if (!invalidRequestReason.isBlank()) {
            return Result.invalidRequest(List.of(invalidRequestReason), timeout);
        }

        PrivateNetworkLiveValidationGate.BackendDecision backend =
                gateResult.backendDecisions().stream()
                        .filter(PrivateNetworkLiveValidationGate.BackendDecision::allowed)
                        .findFirst()
                        .orElse(null);
        if (backend == null || backend.normalizedUrl().isBlank()) {
            return Result.blocked(PrivateNetworkLiveValidationGate.Status.BLOCKED,
                    List.of("no classifier-approved backend URL is available"), timeout);
        }

        URI requestUri = validationUri(backend.normalizedUrl(), normalizedRequest.pathAndQuery());
        Attempt attempt = new Attempt(
                backend.label(),
                requestUri,
                normalizedRequest.method(),
                normalizedRequest.headers(),
                normalizedRequest.body(),
                timeout);

        try {
            AttemptResponse response = transport.send(attempt);
            return Result.success(
                    backend.label(),
                    requestUri.toString(),
                    response.statusCode(),
                    response.headers(),
                    bodySnippet(response.body()),
                    timeout);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Result.failed(backend.label(), requestUri.toString(),
                    List.of("live validation request interrupted"), timeout);
        } catch (IOException | RuntimeException exception) {
            return Result.failed(backend.label(), requestUri.toString(),
                    List.of("live validation request failed: " + exception.getClass().getSimpleName()), timeout);
        }
    }

    private static Duration boundedTimeout(Duration candidate) {
        if (candidate == null || candidate.isZero() || candidate.isNegative()) {
            return DEFAULT_TIMEOUT;
        }
        return candidate.compareTo(MAX_TIMEOUT) > 0 ? MAX_TIMEOUT : candidate;
    }

    private static String invalidRequestReason(String pathAndQuery) {
        if (pathAndQuery == null || pathAndQuery.isBlank()) {
            return "validation request path must not be blank";
        }
        if (containsControlCharacter(pathAndQuery)) {
            return "validation request path must not contain control characters";
        }
        if (pathAndQuery.contains("\\")) {
            return "validation request path must not contain backslash characters";
        }
        URI uri;
        try {
            uri = URI.create(pathAndQuery);
        } catch (IllegalArgumentException exception) {
            return "validation request path must be a valid URI path";
        }
        if (uri.isAbsolute() || uri.getRawAuthority() != null) {
            return "validation request path must be relative";
        }
        if (uri.getRawFragment() != null) {
            return "validation request path must not include a fragment";
        }
        if (uri.getRawQuery() != null) {
            return "validation request path must not include a query string";
        }
        String rawPath = uri.getRawPath();
        String decodedPath = uri.getPath();
        if (rawPath == null || !rawPath.startsWith("/")) {
            return "validation request path must start with /";
        }
        if (rawPath.startsWith("//")) {
            return "validation request path must not start with //";
        }
        if (rawPath.contains("\\") || decodedPath.contains("\\")) {
            return "validation request path must not contain backslash characters";
        }
        if (containsControlCharacter(decodedPath)) {
            return "validation request path must not contain encoded control characters";
        }
        if (containsEncodedTraversalToken(rawPath)
                || containsTraversalSegment(rawPath)
                || containsTraversalSegment(decodedPath)) {
            return "validation request path must not contain traversal segments";
        }
        return "";
    }

    private static URI validationUri(String normalizedBackendUrl, String pathAndQuery) {
        URI base = URI.create(normalizedBackendUrl);
        URI request = URI.create(pathAndQuery);
        return base.resolve(request);
    }

    private static String bodySnippet(String body) {
        if (body == null) {
            return "";
        }
        if (body.length() <= BODY_SNIPPET_LIMIT) {
            return body;
        }
        return body.substring(0, BODY_SNIPPET_LIMIT);
    }

    public interface Transport {
        AttemptResponse send(Attempt attempt) throws IOException, InterruptedException;
    }

    public record ValidationRequest(
            String method,
            String pathAndQuery,
            Map<String, String> headers,
            String body) {
        public ValidationRequest {
            method = method == null || method.isBlank()
                    ? "GET"
                    : method.trim().toUpperCase(Locale.ROOT);
            pathAndQuery = pathAndQuery == null ? "" : pathAndQuery;
            headers = copyHeaders(headers);
            body = body == null ? "" : body;
        }

        public static ValidationRequest get(String pathAndQuery) {
            return new ValidationRequest("GET", pathAndQuery, Map.of(), "");
        }
    }

    public record Attempt(
            String backendLabel,
            URI uri,
            String method,
            Map<String, String> headers,
            String body,
            Duration timeout) {
        public Attempt {
            backendLabel = backendLabel == null ? "" : backendLabel;
            uri = Objects.requireNonNull(uri, "uri");
            method = method == null || method.isBlank()
                    ? "GET"
                    : method.trim().toUpperCase(Locale.ROOT);
            headers = copyHeaders(headers);
            body = body == null ? "" : body;
            timeout = boundedTimeout(timeout);
        }
    }

    public record AttemptResponse(
            int statusCode,
            Map<String, List<String>> headers,
            String body) {
        public AttemptResponse {
            headers = copyResponseHeaderLists(headers);
            body = body == null ? "" : body;
        }
    }

    public record Result(
            Status status,
            String backendLabel,
            String requestUri,
            int statusCode,
            Map<String, List<String>> headers,
            String bodySnippet,
            List<String> reasons,
            Duration timeout) {
        public Result {
            backendLabel = backendLabel == null ? "" : backendLabel;
            requestUri = requestUri == null ? "" : requestUri;
            headers = copyResponseHeaderLists(headers);
            bodySnippet = bodySnippet == null ? "" : bodySnippet;
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
            timeout = boundedTimeout(timeout);
        }

        public boolean success() {
            return status == Status.SUCCESS;
        }

        static Result success(String backendLabel,
                              String requestUri,
                              int statusCode,
                              Map<String, List<String>> headers,
                              String bodySnippet,
                              Duration timeout) {
            return new Result(Status.SUCCESS, backendLabel, requestUri, statusCode, headers,
                    bodySnippet, List.of(), timeout);
        }

        static Result blocked(PrivateNetworkLiveValidationGate.Status gateStatus,
                              List<String> reasons,
                              Duration timeout) {
            return new Result(Status.BLOCKED, "", "", 0, Map.of(), "",
                    reasonsWithGateStatus(gateStatus, reasons), timeout);
        }

        static Result invalidRequest(List<String> reasons, Duration timeout) {
            return new Result(Status.INVALID_REQUEST, "", "", 0, Map.of(), "", reasons, timeout);
        }

        static Result failed(String backendLabel, String requestUri, List<String> reasons, Duration timeout) {
            return new Result(Status.FAILED, backendLabel, requestUri, 0, Map.of(), "", reasons, timeout);
        }
    }

    public enum Status {
        SUCCESS,
        BLOCKED,
        INVALID_REQUEST,
        FAILED
    }

    private static Map<String, String> copyHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        headers.forEach((name, value) -> {
            String normalizedName = normalizedHeaderName(name);
            String canonicalName = ALLOWED_REQUEST_HEADERS.get(normalizedName);
            if (canonicalName != null && isSafeHeaderValue(value)) {
                copy.put(canonicalName, value);
            }
        });
        return Map.copyOf(copy);
    }

    private static Map<String, List<String>> copyResponseHeaderLists(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        headers.forEach((name, values) -> {
            String normalizedName = normalizedHeaderName(name);
            if (ALLOWED_RESPONSE_HEADERS.contains(normalizedName) && values != null) {
                List<String> safeValues = values.stream()
                        .filter(PrivateNetworkLiveValidationExecutor::isSafeHeaderValue)
                        .toList();
                if (!safeValues.isEmpty()) {
                    copy.put(normalizedName, List.copyOf(safeValues));
                }
            }
        });
        return Map.copyOf(copy);
    }

    private static List<String> reasonsWithGateStatus(PrivateNetworkLiveValidationGate.Status gateStatus,
                                                      List<String> reasons) {
        List<String> copy = new ArrayList<>();
        copy.add("gate status=" + gateStatus);
        if (reasons != null) {
            copy.addAll(reasons);
        }
        return List.copyOf(copy);
    }

    private static String normalizedHeaderName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isSafeHeaderValue(String value) {
        return value != null && !containsControlCharacter(value);
    }

    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(character -> character < 0x20 || character == 0x7f);
    }

    private static boolean containsEncodedTraversalToken(String rawPath) {
        String normalized = rawPath.toLowerCase(Locale.ROOT);
        return normalized.contains("%2e") || normalized.contains("%2f") || normalized.contains("%5c");
    }

    private static boolean containsTraversalSegment(String path) {
        for (String segment : path.split("/", -1)) {
            if (".".equals(segment) || "..".equals(segment)) {
                return true;
            }
        }
        return false;
    }
}
