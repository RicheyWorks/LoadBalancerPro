package com.richmond423.loadbalancerpro.api.proxy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

final class ProxyRequestHeaders {
    private static final Pattern HEADER_NAME = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]+");
    private static final Pattern TOKEN = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]+");
    private static final Pattern IPV6_LITERAL = Pattern.compile("[0-9A-Fa-f:]+");
    private static final Set<String> SPOOFABLE_HEADERS = Set.of(
            "forwarded", "x-forwarded-for", "x-forwarded-host", "x-forwarded-proto");
    private static final String WEBSOCKET_HANDSHAKE_PREFIX = "sec-websocket-";

    private ProxyRequestHeaders() {
    }

    static ForwardedPolicy compileForwarded(ReverseProxyProperties.Forwarded properties) {
        ReverseProxyProperties.Forwarded safe = properties == null
                ? new ReverseProxyProperties.Forwarded()
                : properties;
        Mode mode = Mode.parse(safe.getMode());
        List<NetworkRange> trustedProxies = new ArrayList<>();
        for (int index = 0; index < safe.getTrustedProxies().size(); index++) {
            trustedProxies.add(NetworkRange.parse(
                    safe.getTrustedProxies().get(index),
                    "loadbalancerpro.proxy.forwarded.trusted-proxies[" + index + "]"));
        }
        return new ForwardedPolicy(mode, List.copyOf(trustedProxies));
    }

    static HeaderRewrites compileRewrites(ReverseProxyProperties.Headers properties, String fieldPrefix) {
        ReverseProxyProperties.Headers safe = properties == null
                ? new ReverseProxyProperties.Headers()
                : properties;
        Map<String, String> set = validatedValues(safe.getSet(), fieldPrefix + ".set");
        Map<String, String> add = validatedValues(safe.getAdd(), fieldPrefix + ".add");
        Set<String> remove = new LinkedHashSet<>();
        Set<String> normalizedNames = new LinkedHashSet<>();
        safe.getRemove().forEach((name, enabled) -> {
            String validated = validateHeaderName(name, fieldPrefix + ".remove");
            String normalized = validated.toLowerCase(Locale.ROOT);
            if (!normalizedNames.add(normalized)) {
                throw new IllegalStateException(fieldPrefix + ".remove contains duplicate header name: " + name);
            }
            if (Boolean.TRUE.equals(enabled)) {
                remove.add(validated);
            }
        });
        return new HeaderRewrites(set, add, Set.copyOf(remove));
    }

    static boolean isSpoofable(String headerName) {
        return headerName != null && SPOOFABLE_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }

    static Map<String, List<String>> webSocketHeaders(
            HttpServletRequest request, ForwardedPolicy forwardedPolicy, HeaderRewrites rewrites) {
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> connectionTokens = connectionHeaderTokens(request);
        Collections.list(request.getHeaderNames()).forEach(headerName -> {
            String normalized = headerName.toLowerCase(Locale.ROOT);
            if (!ReverseProxyService.isHopByHopHeader(headerName)
                    && !normalized.startsWith(WEBSOCKET_HANDSHAKE_PREFIX)
                    && !connectionTokens.contains(normalized)
                    && !isSpoofable(headerName)
                    && !rewrites.removes(headerName)) {
                List<String> values = Collections.list(request.getHeaders(headerName)).stream()
                        .filter(value -> value != null && !containsControlCharacter(value))
                        .toList();
                if (!values.isEmpty()) {
                    headers.put(headerName, new ArrayList<>(values));
                }
            }
        });
        forwardedPolicy.apply(headers, request, rewrites);
        rewrites.apply(headers);
        Map<String, List<String>> immutable = new LinkedHashMap<>();
        headers.forEach((name, values) -> immutable.put(name, List.copyOf(values)));
        return Map.copyOf(immutable);
    }

    private static Set<String> connectionHeaderTokens(HttpServletRequest request) {
        Set<String> tokens = new LinkedHashSet<>();
        Collections.list(request.getHeaders("Connection")).forEach(value -> {
            if (value == null) {
                return;
            }
            for (String token : value.split(",")) {
                String normalized = token.trim().toLowerCase(Locale.ROOT);
                if (TOKEN.matcher(normalized).matches()) {
                    tokens.add(normalized);
                }
            }
        });
        return Set.copyOf(tokens);
    }

    private static Map<String, String> validatedValues(Map<String, String> values, String fieldPrefix) {
        Map<String, String> validated = new LinkedHashMap<>();
        Set<String> normalizedNames = new LinkedHashSet<>();
        values.forEach((name, value) -> {
            String safeName = validateHeaderName(name, fieldPrefix);
            String normalized = safeName.toLowerCase(Locale.ROOT);
            if (!normalizedNames.add(normalized)) {
                throw new IllegalStateException(fieldPrefix + " contains duplicate header name: " + name);
            }
            if (value == null || containsControlCharacter(value)) {
                throw new IllegalStateException(
                        fieldPrefix + "." + name + " must not be null or contain control characters");
            }
            validated.put(safeName, value);
        });
        return Map.copyOf(validated);
    }

    private static String validateHeaderName(String name, String fieldPrefix) {
        if (name == null || !HEADER_NAME.matcher(name).matches()) {
            throw new IllegalStateException(fieldPrefix + " contains an invalid HTTP header name");
        }
        if (ReverseProxyService.isHopByHopHeader(name)) {
            throw new IllegalStateException(fieldPrefix + " cannot rewrite hop-by-hop header: " + name);
        }
        if (name.toLowerCase(Locale.ROOT).startsWith(WEBSOCKET_HANDSHAKE_PREFIX)) {
            throw new IllegalStateException(fieldPrefix + " cannot rewrite WebSocket handshake header: " + name);
        }
        return name;
    }

    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(character -> character < 0x20 || character == 0x7f);
    }

    enum Mode {
        STRIP_AND_SET,
        APPEND,
        OFF;

        static Mode parse(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "strip-and-set" -> STRIP_AND_SET;
                case "append" -> APPEND;
                case "off" -> OFF;
                default -> throw new IllegalStateException(
                        "loadbalancerpro.proxy.forwarded.mode must be strip-and-set, append, or off");
            };
        }
    }

    record ForwardedPolicy(Mode mode, List<NetworkRange> trustedProxies) {
        void apply(HttpRequest.Builder builder, HttpServletRequest request, HeaderRewrites rewrites) {
            if (mode == Mode.OFF) {
                return;
            }
            String remoteAddress = safeRemoteAddress(request.getRemoteAddr());
            String scheme = safeScheme(request.getScheme());
            String host = safeHost(request);
            boolean appendTrusted = mode == Mode.APPEND && isTrusted(request.getRemoteAddr());

            setOrAppendUnlessRemoved(builder, rewrites, "X-Forwarded-For", remoteAddress,
                    appendTrusted ? request.getHeaders("X-Forwarded-For") : null);
            setOrAppendUnlessRemoved(builder, rewrites, "X-Forwarded-Proto", scheme,
                    appendTrusted ? request.getHeaders("X-Forwarded-Proto") : null);
            setOrAppendUnlessRemoved(builder, rewrites, "X-Forwarded-Host", host,
                    appendTrusted ? request.getHeaders("X-Forwarded-Host") : null);
            setOrAppendUnlessRemoved(builder, rewrites, "Forwarded", forwardedElement(remoteAddress, scheme, host),
                    appendTrusted ? request.getHeaders("Forwarded") : null);
        }

        void apply(Map<String, List<String>> headers, HttpServletRequest request, HeaderRewrites rewrites) {
            if (mode == Mode.OFF) {
                return;
            }
            String remoteAddress = safeRemoteAddress(request.getRemoteAddr());
            String scheme = safeScheme(request.getScheme());
            String host = safeHost(request);
            boolean appendTrusted = mode == Mode.APPEND && isTrusted(request.getRemoteAddr());

            setOrAppendUnlessRemoved(headers, rewrites, "X-Forwarded-For", remoteAddress,
                    appendTrusted ? request.getHeaders("X-Forwarded-For") : null);
            setOrAppendUnlessRemoved(headers, rewrites, "X-Forwarded-Proto", scheme,
                    appendTrusted ? request.getHeaders("X-Forwarded-Proto") : null);
            setOrAppendUnlessRemoved(headers, rewrites, "X-Forwarded-Host", host,
                    appendTrusted ? request.getHeaders("X-Forwarded-Host") : null);
            setOrAppendUnlessRemoved(headers, rewrites, "Forwarded", forwardedElement(remoteAddress, scheme, host),
                    appendTrusted ? request.getHeaders("Forwarded") : null);
        }

        private boolean isTrusted(String remoteAddress) {
            byte[] address = literalAddress(remoteAddress, null);
            if (address == null) {
                return false;
            }
            return trustedProxies.stream().anyMatch(range -> range.contains(address));
        }

        private static void setOrAppendUnlessRemoved(HttpRequest.Builder builder,
                                                     HeaderRewrites rewrites,
                                                     String name,
                                                     String currentValue,
                                                     java.util.Enumeration<String> inboundValues) {
            if (rewrites.removes(name)) {
                return;
            }
            List<String> chain = inboundValues == null
                    ? new ArrayList<>()
                    : Collections.list(inboundValues).stream()
                            .filter(value -> value != null && !value.isBlank() && !containsControlCharacter(value))
                            .collect(Collectors.toCollection(ArrayList::new));
            chain.add(currentValue);
            builder.setHeader(name, String.join(", ", chain));
        }

        private static void setOrAppendUnlessRemoved(Map<String, List<String>> headers,
                                                     HeaderRewrites rewrites,
                                                     String name,
                                                     String currentValue,
                                                     java.util.Enumeration<String> inboundValues) {
            if (rewrites.removes(name)) {
                return;
            }
            List<String> chain = inboundValues == null
                    ? new ArrayList<>()
                    : Collections.list(inboundValues).stream()
                            .filter(value -> value != null && !value.isBlank() && !containsControlCharacter(value))
                            .collect(Collectors.toCollection(ArrayList::new));
            chain.add(currentValue);
            headers.put(name, new ArrayList<>(List.of(String.join(", ", chain))));
        }

        private static String safeRemoteAddress(String value) {
            byte[] address = literalAddress(value, null);
            if (address == null) {
                return "unknown";
            }
            try {
                return InetAddress.getByAddress(address).getHostAddress();
            } catch (UnknownHostException exception) {
                return "unknown";
            }
        }

        private static String safeScheme(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            return TOKEN.matcher(normalized).matches() ? normalized : "http";
        }

        private static String safeHost(HttpServletRequest request) {
            String inbound = request.getHeader("Host");
            if (inbound != null && !inbound.isBlank() && !containsControlCharacter(inbound)) {
                return inbound.trim();
            }
            String serverName = request.getServerName();
            String safeName = serverName == null || serverName.isBlank() || containsControlCharacter(serverName)
                    ? "unknown"
                    : serverName.trim();
            int port = request.getServerPort();
            boolean defaultPort = ("http".equalsIgnoreCase(request.getScheme()) && port == 80)
                    || ("https".equalsIgnoreCase(request.getScheme()) && port == 443);
            return port > 0 && !defaultPort ? safeName + ":" + port : safeName;
        }

        private static String forwardedElement(String remoteAddress, String scheme, String host) {
            String node = remoteAddress.contains(":")
                    ? "\"[" + quote(remoteAddress) + "]\""
                    : remoteAddress;
            return "for=" + node + ";proto=" + scheme + ";host=\"" + quote(host) + "\"";
        }

        private static String quote(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    record HeaderRewrites(Map<String, String> set, Map<String, String> add, Set<String> remove) {
        void apply(HttpRequest.Builder builder) {
            set.forEach(builder::setHeader);
            add.forEach(builder::header);
        }

        void apply(Map<String, List<String>> headers) {
            set.forEach((name, value) -> headers.put(name, new ArrayList<>(List.of(value))));
            add.forEach((name, value) -> headers.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value));
        }

        boolean removes(String name) {
            return remove.stream().anyMatch(candidate -> candidate.equalsIgnoreCase(name));
        }
    }

    record NetworkRange(byte[] network, int prefixLength) {
        static NetworkRange parse(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(fieldName + " must be an IPv4 or IPv6 literal CIDR");
            }
            String[] parts = value.trim().split("/", -1);
            if (parts.length > 2) {
                throw new IllegalStateException(fieldName + " must be an IPv4 or IPv6 literal CIDR");
            }
            byte[] address = literalAddress(parts[0], fieldName);
            int maximum = address.length * 8;
            int prefix = maximum;
            if (parts.length == 2) {
                try {
                    prefix = Integer.parseInt(parts[1]);
                } catch (NumberFormatException exception) {
                    throw new IllegalStateException(fieldName + " must have a numeric CIDR prefix", exception);
                }
            }
            if (prefix < 0 || prefix > maximum) {
                throw new IllegalStateException(fieldName + " CIDR prefix must be between 0 and " + maximum);
            }
            return new NetworkRange(address.clone(), prefix);
        }

        boolean contains(byte[] address) {
            if (address.length != network.length) {
                return false;
            }
            int wholeBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int index = 0; index < wholeBytes; index++) {
                if (address[index] != network[index]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xff << (8 - remainingBits);
            return (address[wholeBytes] & mask) == (network[wholeBytes] & mask);
        }
    }

    private static byte[] literalAddress(String value, String fieldName) {
        String input = value == null ? "" : value.trim();
        byte[] parsed = input.contains(":") ? parseIpv6(input) : parseIpv4(input);
        if (parsed == null && fieldName != null) {
            throw new IllegalStateException(fieldName + " must be an IPv4 or IPv6 literal CIDR");
        }
        return parsed;
    }

    private static byte[] parseIpv4(String input) {
        String[] octets = input.split("\\.", -1);
        if (octets.length != 4) {
            return null;
        }
        byte[] address = new byte[4];
        for (int index = 0; index < octets.length; index++) {
            if (octets[index].isEmpty() || octets[index].length() > 3
                    || !octets[index].chars().allMatch(Character::isDigit)) {
                return null;
            }
            int value;
            try {
                value = Integer.parseInt(octets[index]);
            } catch (NumberFormatException exception) {
                return null;
            }
            if (value > 255) {
                return null;
            }
            address[index] = (byte) value;
        }
        return address;
    }

    private static byte[] parseIpv6(String input) {
        if (!IPV6_LITERAL.matcher(input).matches()) {
            return null;
        }
        try {
            byte[] address = InetAddress.getByName(input).getAddress();
            return address.length == 16 ? address : null;
        } catch (UnknownHostException exception) {
            return null;
        }
    }
}
