package com.richmond423.loadbalancerpro.api.proxy;

import java.net.IDN;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Pure validation and canonicalization boundary for proxy DNS discovery. */
final class ProxyDnsDiscovery {
    static final int MAX_ADDRESSES_PER_NAME = 32;

    private static final Pattern DNS_LABEL =
            Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?");

    private ProxyDnsDiscovery() {
    }

    static Spec compile(String discovery, String templateUrl, String authorityMode, String fieldName) {
        String value = requireText(discovery, fieldName);
        if (!value.startsWith("dns:")) {
            throw invalid(fieldName, "must use dns:<name>:<port>");
        }
        String authority = value.substring(4);
        int separator = authority.lastIndexOf(':');
        if (separator <= 0 || separator == authority.length() - 1
                || authority.indexOf(':') != separator) {
            throw invalid(fieldName, "must use dns:<name>:<port>");
        }
        String name = normalizedName(authority.substring(0, separator), fieldName);
        int port = parsePort(authority.substring(separator + 1), fieldName);
        URI template = parseTemplate(templateUrl, name, port, fieldName.replace(".discovery", ".url"));
        String normalizedAuthorityMode = requireText(
                authorityMode, fieldName.replace(".discovery", ".discovery-authority"));
        if (!"address".equals(normalizedAuthorityMode)) {
            throw invalid(fieldName.replace(".discovery", ".discovery-authority"),
                    "must be address when DNS discovery is configured");
        }
        return new Spec(name, port, template, normalizedAuthorityMode);
    }

    static List<Member> members(
            Spec spec,
            String logicalUpstreamId,
            Collection<InetAddress> answers,
            boolean privateNetworkOnly) {
        Objects.requireNonNull(spec, "spec cannot be null");
        String logicalId = requireText(logicalUpstreamId, "logicalUpstreamId");
        Objects.requireNonNull(answers, "answers cannot be null");

        Map<String, byte[]> unique = new LinkedHashMap<>();
        for (InetAddress answer : answers) {
            if (answer == null || answer.isAnyLocalAddress() || answer.isMulticastAddress()
                    || answer instanceof Inet6Address ipv6
                    && (ipv6.getScopeId() != 0 || ipv6.getScopedInterface() != null)) {
                continue;
            }
            byte[] bytes = answer.getAddress();
            if (bytes == null || bytes.length != 4 && bytes.length != 16) {
                continue;
            }
            String address = literalAddress(bytes);
            if (privateNetworkOnly && !privateAddressAllowed(address, spec.port())) {
                continue;
            }
            unique.putIfAbsent(hex(bytes), bytes);
        }

        List<byte[]> ordered = new ArrayList<>(unique.values());
        ordered.sort(ProxyDnsDiscovery::compareUnsigned);
        if (ordered.size() > MAX_ADDRESSES_PER_NAME) {
            throw new IllegalStateException("DNS answer contains more than "
                    + MAX_ADDRESSES_PER_NAME + " usable unique addresses");
        }
        return ordered.stream()
                .map(bytes -> member(spec, logicalId, bytes))
                .toList();
    }

    private static Member member(Spec spec, String logicalId, byte[] bytes) {
        String address = literalAddress(bytes);
        String authority = address.contains(":") ? "[" + address + "]" : address;
        String rawPath = Objects.requireNonNullElse(spec.template().getRawPath(), "");
        URI endpoint = URI.create("http://" + authority + ":" + spec.port() + rawPath);
        return new Member(memberId(logicalId, spec, address), address, endpoint);
    }

    private static URI parseTemplate(String value, String name, int port, String fieldName) {
        String candidate = requireText(value, fieldName);
        URI uri;
        try {
            uri = URI.create(candidate);
        } catch (IllegalArgumentException exception) {
            throw invalid(fieldName, "must be a valid HTTP URI");
        }
        if (!"http".equalsIgnoreCase(uri.getScheme())) {
            throw invalid(fieldName, "must use http when DNS discovery is configured");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw invalid(fieldName, "must include only an HTTP authority and optional base path");
        }
        String templateName = normalizedName(uri.getHost(), fieldName);
        int templatePort = uri.getPort() < 0 ? 80 : uri.getPort();
        if (!name.equals(templateName) || port != templatePort) {
            throw invalid(fieldName, "host and effective port must match discovery");
        }
        return uri;
    }

    private static String normalizedName(String value, String fieldName) {
        String candidate = requireText(value, fieldName);
        if (candidate.endsWith(".") || candidate.contains("*") || candidate.contains(":")) {
            throw invalid(fieldName, "must contain an unambiguous DNS name");
        }
        String ascii;
        try {
            ascii = IDN.toASCII(candidate, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            throw invalid(fieldName, "must contain a valid DNS name");
        }
        if (ascii.length() > 253 || ascii.chars().allMatch(character -> character == '.' || Character.isDigit(character))) {
            throw invalid(fieldName, "must contain a DNS name rather than an IP literal");
        }
        String[] labels = ascii.split("\\.", -1);
        for (String label : labels) {
            if (!DNS_LABEL.matcher(label).matches()) {
                throw invalid(fieldName, "must contain valid DNS labels");
            }
        }
        return ascii;
    }

    private static int parsePort(String value, String fieldName) {
        if (!value.chars().allMatch(Character::isDigit)) {
            throw invalid(fieldName, "port must be an integer from 1 through 65535");
        }
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65_535) {
                throw invalid(fieldName, "port must be an integer from 1 through 65535");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw invalid(fieldName, "port must be an integer from 1 through 65535");
        }
    }

    private static boolean privateAddressAllowed(String address, int port) {
        String host = address.contains(":") ? "[" + address + "]" : address;
        return ProxyBackendUrlClassifier.classify("http://" + host + ":" + port).allowed();
    }

    private static String memberId(String logicalId, Spec spec, String address) {
        String prefix = logicalId.length() <= 40 ? logicalId : logicalId.substring(0, 40);
        byte[] input = (logicalId + '\n' + spec.name() + '\n' + spec.port() + '\n' + address)
                .getBytes(StandardCharsets.UTF_8);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
            return prefix + "-dns-" + hex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String literalAddress(byte[] bytes) {
        try {
            return InetAddress.getByAddress(bytes).getHostAddress().toLowerCase(Locale.ROOT);
        } catch (java.net.UnknownHostException exception) {
            throw new IllegalArgumentException("address must contain 4 or 16 bytes", exception);
        }
    }

    private static int compareUnsigned(byte[] left, byte[] right) {
        int length = Integer.compare(left.length, right.length);
        if (length != 0) {
            return length;
        }
        for (int index = 0; index < left.length; index++) {
            int compared = Integer.compare(Byte.toUnsignedInt(left[index]), Byte.toUnsignedInt(right[index]));
            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) {
            value.append(Character.forDigit((item >>> 4) & 0xf, 16));
            value.append(Character.forDigit(item & 0xf, 16));
        }
        return value.toString();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank() || value.chars().anyMatch(Character::isISOControl)) {
            throw invalid(fieldName, "must not be blank or contain control characters");
        }
        return value.trim();
    }

    private static IllegalStateException invalid(String fieldName, String reason) {
        return new IllegalStateException(fieldName + " " + reason);
    }

    record Spec(String name, int port, URI template, String authorityMode) {
        Spec {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(template, "template cannot be null");
            Objects.requireNonNull(authorityMode, "authorityMode cannot be null");
        }
    }

    record Member(String id, String address, URI endpoint) {
        Member {
            Objects.requireNonNull(id, "id cannot be null");
            Objects.requireNonNull(address, "address cannot be null");
            Objects.requireNonNull(endpoint, "endpoint cannot be null");
        }
    }
}
