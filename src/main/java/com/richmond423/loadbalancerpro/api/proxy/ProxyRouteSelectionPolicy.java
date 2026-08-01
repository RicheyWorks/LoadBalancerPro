package com.richmond423.loadbalancerpro.api.proxy;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.richmond423.loadbalancerpro.core.ServerStateVector;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

final class ProxyRouteSelectionPolicy {
    private static final Pattern HTTP_TOKEN = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]+");
    private static final Set<String> SENSITIVE_HASH_HEADERS = Set.of(
            "authorization", "proxy-authorization", "cookie", "set-cookie", "x-api-key");
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();
    private static final int MINIMUM_HMAC_KEY_BYTES = 32;
    private static final int MAXIMUM_HMAC_KEY_BYTES = 4096;
    private static final int MAXIMUM_COOKIE_VALUE_LENGTH = 4096;

    private final HashKeySource hashKeySource;
    private final Affinity affinity;

    private ProxyRouteSelectionPolicy(HashKeySource hashKeySource, Affinity affinity) {
        this.hashKeySource = Objects.requireNonNull(hashKeySource, "hashKeySource cannot be null");
        this.affinity = Objects.requireNonNull(affinity, "affinity cannot be null");
    }

    static ProxyRouteSelectionPolicy compile(
            String routeName, ReverseProxyProperties.Route route, String fieldPrefix) {
        Objects.requireNonNull(route, "route cannot be null");
        return new ProxyRouteSelectionPolicy(
                HashKeySource.parse(route.getHashOn(), fieldPrefix + ".hash-on"),
                Affinity.compile(routeName, route.getAffinity(), fieldPrefix + ".affinity"));
    }

    static ProxyRouteSelectionPolicy legacy(String routeName) {
        return new ProxyRouteSelectionPolicy(HashKeySource.clientIp(), Affinity.disabled(routeName));
    }

    String routingKey(HttpServletRequest request) {
        return hashKeySource.value(request);
    }

    Optional<String> affinityTarget(HttpServletRequest request, List<ServerStateVector> candidates) {
        Optional<String> targetId = affinity.targetFrom(request);
        if (targetId.isEmpty()) {
            return Optional.empty();
        }
        return candidates.stream()
                .filter(Objects::nonNull)
                .filter(ServerStateVector::healthy)
                .filter(candidate -> candidate.weight() > 0.0)
                .map(ServerStateVector::serverId)
                .filter(targetId.get()::equals)
                .findFirst();
    }

    ReverseProxyResponse applyAffinityResponse(
            ReverseProxyResponse response,
            HttpServletRequest request,
            String upstreamId,
            boolean issueCookie) {
        if (!affinity.enabled()) {
            return response;
        }
        HttpHeaders headers = new HttpHeaders();
        response.headers().forEach((name, values) -> headers.put(name, new ArrayList<>(values)));
        List<String> preservedCookies = headers.getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> !affinity.isOwnSetCookie(value))
                .toList();
        headers.remove(HttpHeaders.SET_COOKIE);
        preservedCookies.forEach(value -> headers.add(HttpHeaders.SET_COOKIE, value));
        if (issueCookie) {
            ResponseCookie cookie = ResponseCookie.from(affinity.cookieName(), affinity.signedValue(upstreamId))
                    .httpOnly(true)
                    .secure(request.isSecure())
                    .sameSite("Lax")
                    .path("/proxy")
                    .build();
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return new ReverseProxyResponse(response.statusCode(), headers, response.body());
    }

    String hashOnDescription() {
        return hashKeySource.description();
    }

    boolean affinityEnabled() {
        return affinity.enabled();
    }

    private record HashKeySource(String headerName) {
        static HashKeySource parse(String configured, String fieldName) {
            String value = configured == null || configured.isBlank() ? "client-ip" : configured.trim();
            if (value.equalsIgnoreCase("client-ip")) {
                return clientIp();
            }
            if (!value.regionMatches(true, 0, "header:", 0, "header:".length())) {
                throw new IllegalStateException(fieldName + " must be client-ip or header:<name>");
            }
            String headerName = value.substring("header:".length()).trim();
            if (!HTTP_TOKEN.matcher(headerName).matches()) {
                throw new IllegalStateException(fieldName + " contains an invalid HTTP header name");
            }
            String normalized = headerName.toLowerCase(Locale.ROOT);
            if (SENSITIVE_HASH_HEADERS.contains(normalized)
                    || ProxyRequestHeaders.isSpoofable(headerName)
                    || ReverseProxyService.isHopByHopHeader(headerName)) {
                throw new IllegalStateException(fieldName + " cannot use a sensitive, forwarding, or hop-by-hop header");
            }
            return new HashKeySource(headerName);
        }

        static HashKeySource clientIp() {
            return new HashKeySource(null);
        }

        String value(HttpServletRequest request) {
            Objects.requireNonNull(request, "request cannot be null");
            if (headerName != null) {
                String headerValue = request.getHeader(headerName);
                if (headerValue != null && !headerValue.isBlank()) {
                    return headerValue.trim();
                }
            }
            String remoteAddress = request.getRemoteAddr();
            return remoteAddress == null || remoteAddress.isBlank()
                    ? "unknown-immediate-client"
                    : remoteAddress.trim();
        }

        String description() {
            return headerName == null ? "client-ip" : "header:" + headerName;
        }
    }

    private record Affinity(String routeName, String cookieName, byte[] hmacKey) {
        private Affinity {
            hmacKey = hmacKey.clone();
        }

        static Affinity compile(
                String routeName, ReverseProxyProperties.Affinity configured, String fieldPrefix) {
            ReverseProxyProperties.Affinity safe = configured == null
                    ? new ReverseProxyProperties.Affinity()
                    : configured;
            String cookieName = safe.getCookieName() == null ? "" : safe.getCookieName().trim();
            String configuredKey = safe.getHmacKey() == null ? "" : safe.getHmacKey();
            if (cookieName.isEmpty()) {
                if (!configuredKey.isBlank()) {
                    throw new IllegalStateException(fieldPrefix + ".hmac-key requires cookie-name");
                }
                return disabled(routeName);
            }
            if (cookieName.length() > 128 || cookieName.startsWith("$")
                    || !HTTP_TOKEN.matcher(cookieName).matches()) {
                throw new IllegalStateException(fieldPrefix + ".cookie-name must be a valid cookie name");
            }
            byte[] key = configuredKey.getBytes(StandardCharsets.UTF_8);
            if (key.length < MINIMUM_HMAC_KEY_BYTES || key.length > MAXIMUM_HMAC_KEY_BYTES) {
                throw new IllegalStateException(fieldPrefix + ".hmac-key must contain between "
                        + MINIMUM_HMAC_KEY_BYTES + " and " + MAXIMUM_HMAC_KEY_BYTES + " UTF-8 bytes");
            }
            return new Affinity(routeName, cookieName, key);
        }

        static Affinity disabled(String routeName) {
            return new Affinity(routeName, "", new byte[0]);
        }

        boolean enabled() {
            return !cookieName.isEmpty();
        }

        Optional<String> targetFrom(HttpServletRequest request) {
            if (!enabled()) {
                return Optional.empty();
            }
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return Optional.empty();
            }
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return verifiedTarget(cookie.getValue());
                }
            }
            return Optional.empty();
        }

        String signedValue(String upstreamId) {
            String encodedId = BASE64_ENCODER.encodeToString(upstreamId.getBytes(StandardCharsets.UTF_8));
            String signature = BASE64_ENCODER.encodeToString(sign(upstreamId));
            return "v1." + encodedId + "." + signature;
        }

        boolean isOwnSetCookie(String value) {
            return value != null && value.stripLeading().startsWith(cookieName + "=");
        }

        private Optional<String> verifiedTarget(String value) {
            if (value == null || value.length() > MAXIMUM_COOKIE_VALUE_LENGTH) {
                return Optional.empty();
            }
            String[] parts = value.split("\\.", -1);
            if (parts.length != 3 || !"v1".equals(parts[0])) {
                return Optional.empty();
            }
            try {
                String targetId = new String(BASE64_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
                byte[] suppliedSignature = BASE64_DECODER.decode(parts[2]);
                return MessageDigest.isEqual(sign(targetId), suppliedSignature)
                        ? Optional.of(targetId)
                        : Optional.empty();
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }

        private byte[] sign(String upstreamId) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
                return mac.doFinal((routeName + "\n" + upstreamId).getBytes(StandardCharsets.UTF_8));
            } catch (GeneralSecurityException exception) {
                throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
            }
        }
    }
}
