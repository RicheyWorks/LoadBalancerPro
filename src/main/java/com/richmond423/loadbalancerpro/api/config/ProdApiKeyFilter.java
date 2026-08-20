package com.richmond423.loadbalancerpro.api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import com.richmond423.loadbalancerpro.api.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Conditional(ProdApiKeyFilter.ApiKeyAuthModeCondition.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ProdApiKeyFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(ProdApiKeyFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final ObjectMapper objectMapper;
    private final ApiKeyVerifier apiKeyVerifier;
    private final boolean protectActuator;
    private final AtomicBoolean missingKeyWarningLogged = new AtomicBoolean(false);

    public ProdApiKeyFilter(ObjectMapper objectMapper,
                            ApiKeyVerifier apiKeyVerifier,
                            @Value("${loadbalancerpro.auth.protect-actuator:false}") boolean protectActuator) {
        this.objectMapper = objectMapper;
        this.apiKeyVerifier = apiKeyVerifier;
        this.protectActuator = protectActuator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isProtectedApiRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!apiKeyVerifier.isConfigured()) {
            logMissingKeyWarningOnce();
            writeUnauthorized(request, response);
            return;
        }

        String presentedApiKey = request.getHeader(API_KEY_HEADER);
        if (!apiKeyVerifier.matches(presentedApiKey)) {
            writeUnauthorized(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtectedApiRequest(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod())) {
            return false;
        }
        return isProtectedApiSurface(request)
                || isProtectedProxyRequest(request)
                || isProtectedOpenApiDocs(request)
                || protectActuator && isActuatorRequest(request);
    }

    private static boolean isProtectedApiSurface(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri.startsWith("/api/") && !isPublicApiException(request);
    }

    private static boolean isPublicApiException(HttpServletRequest request) {
        return "GET".equals(request.getMethod()) && "/api/health".equals(request.getRequestURI());
    }

    private static boolean isProtectedProxyRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return "/proxy".equals(requestUri) || requestUri.startsWith("/proxy/");
    }

    private static boolean isProtectedOpenApiDocs(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return "/v3/api-docs".equals(requestUri)
                || requestUri.startsWith("/v3/api-docs/")
                || "/swagger-ui.html".equals(requestUri)
                || requestUri.startsWith("/swagger-ui/");
    }

    private static boolean isActuatorRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return "/actuator".equals(requestUri) || requestUri.startsWith("/actuator/");
    }

    private void logMissingKeyWarningOnce() {
        if (missingKeyWarningLogged.compareAndSet(false, true)) {
            logger.warn("API-key auth mode has no configured key; protected API requests will be rejected.");
        }
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.unauthorized(request.getRequestURI()));
    }

    static final class ApiKeyAuthModeCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String configuredMode = context.getEnvironment().getProperty("loadbalancerpro.auth.mode", "api-key");
            return "api-key".equals(normalize(configuredMode));
        }

        private static String normalize(String mode) {
            return mode == null ? "api-key" : mode.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        }
    }
}
