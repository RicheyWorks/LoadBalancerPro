package com.richmond423.loadbalancerpro.api.proxy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProxyBackendUrlClassifier {
    private ProxyBackendUrlClassifier() {
    }

    public enum Status {
        LOOPBACK_ALLOWED,
        PRIVATE_NETWORK_ALLOWED,
        PUBLIC_NETWORK_REJECTED,
        INVALID_REJECTED,
        UNSUPPORTED_SCHEME_REJECTED,
        USERINFO_REJECTED,
        AMBIGUOUS_HOST_REJECTED
    }

    public record Classification(Status status, String host, String normalizedUrl, String reason) {
        public boolean allowed() {
            return status == Status.LOOPBACK_ALLOWED || status == Status.PRIVATE_NETWORK_ALLOWED;
        }
    }

    public static Classification classify(String value) {
        if (value == null || value.isBlank()) {
            return rejected(Status.INVALID_REJECTED, "", "URL must not be blank");
        }
        String candidate = value.trim();
        if (containsControlCharacter(candidate)) {
            return rejected(Status.INVALID_REJECTED, "", "URL must not contain control characters");
        }

        URI uri;
        try {
            uri = new URI(candidate);
        } catch (URISyntaxException exception) {
            return rejected(Status.INVALID_REJECTED, "", "URL must be a valid URI");
        }

        String scheme = uri.getScheme();
        if (scheme == null || scheme.isBlank()) {
            return rejected(Status.INVALID_REJECTED, "", "URL must include an http or https scheme");
        }
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return rejected(Status.UNSUPPORTED_SCHEME_REJECTED, "", "URL scheme must be http or https");
        }
        if (uri.getRawUserInfo() != null) {
            return rejected(Status.USERINFO_REJECTED, "", "URL must not include user info");
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            return rejected(Status.INVALID_REJECTED, "", "Backend URL must not include query strings or fragments");
        }

        String rawAuthority = uri.getRawAuthority();
        if (rawAuthority == null || rawAuthority.isBlank()) {
            return rejected(Status.INVALID_REJECTED, "", "URL must include a host");
        }
        if (rawAuthority.contains("*")) {
            return rejected(Status.AMBIGUOUS_HOST_REJECTED, rawAuthority, "Wildcard hosts are not allowed");
        }

        String host = normalizedHost(uri.getHost());
        if (host.isBlank()) {
            return rejected(Status.INVALID_REJECTED, "", "URL must include a valid host");
        }
        if (isBroadHostPattern(host)) {
            return rejected(Status.AMBIGUOUS_HOST_REJECTED, host, "Broad host patterns are not allowed");
        }
        if ("localhost".equals(host)) {
            return allowed(Status.LOOPBACK_ALLOWED, uri, host, "localhost is loopback");
        }

        if (looksLikeIpv4Candidate(host)) {
            if (!host.contains(".")) {
                return rejected(Status.AMBIGUOUS_HOST_REJECTED, host,
                        "Numeric host shorthand is not allowed");
            }
            if (hasAmbiguousIpv4Formatting(host)) {
                return rejected(Status.AMBIGUOUS_HOST_REJECTED, host, "IPv4 host uses ambiguous formatting");
            }
            int[] octets = parseIpv4(host);
            if (octets == null) {
                return rejected(Status.INVALID_REJECTED, host, "IPv4 host is invalid");
            }
            if (isBroadIpv4(octets)) {
                return rejected(Status.AMBIGUOUS_HOST_REJECTED, host, "Broad IPv4 host is not allowed");
            }
            if (isLoopbackIpv4(octets)) {
                return allowed(Status.LOOPBACK_ALLOWED, uri, host, "IPv4 host is loopback");
            }
            if (isPrivateIpv4(octets)) {
                return allowed(Status.PRIVATE_NETWORK_ALLOWED, uri, host, "IPv4 host is RFC1918 private");
            }
            return rejected(Status.PUBLIC_NETWORK_REJECTED, host, "IPv4 host is not loopback or RFC1918 private");
        }

        if (host.contains(":")) {
            byte[] bytes = parseIpv6(host);
            if (bytes == null) {
                return rejected(Status.INVALID_REJECTED, host, "IPv6 host is invalid");
            }
            if (isUnspecifiedIpv6(bytes)) {
                return rejected(Status.AMBIGUOUS_HOST_REJECTED, host, "Broad IPv6 host is not allowed");
            }
            if (isLoopbackIpv6(bytes)) {
                return allowed(Status.LOOPBACK_ALLOWED, uri, host, "IPv6 host is loopback");
            }
            if (isUniqueLocalIpv6(bytes)) {
                return allowed(Status.PRIVATE_NETWORK_ALLOWED, uri, host, "IPv6 host is unique-local private");
            }
            return rejected(Status.PUBLIC_NETWORK_REJECTED, host, "IPv6 host is not loopback or unique-local private");
        }

        return rejected(Status.AMBIGUOUS_HOST_REJECTED, host,
                "Hostnames require a separate reviewed resolver policy");
    }

    private static Classification allowed(Status status, URI uri, String host, String reason) {
        return new Classification(status, host, safeNormalizedUrl(uri, host), reason);
    }

    private static Classification rejected(Status status, String host, String reason) {
        return new Classification(status, host == null ? "" : host, "", reason);
    }

    private static String safeNormalizedUrl(URI uri, String host) {
        try {
            return new URI(
                    uri.getScheme().toLowerCase(Locale.ROOT),
                    null,
                    host,
                    uri.getPort(),
                    uri.getRawPath() == null || uri.getRawPath().isBlank() ? null : uri.getRawPath(),
                    null,
                    null).toString();
        } catch (URISyntaxException exception) {
            return "";
        }
    }

    private static String normalizedHost(String host) {
        if (host == null) {
            return "";
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            return normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(character -> character < 0x20 || character == 0x7f);
    }

    private static boolean isBroadHostPattern(String host) {
        return host.contains("*") || host.equals("0.0.0.0") || host.equals("::") || host.endsWith(".");
    }

    private static boolean looksLikeIpv4Candidate(String host) {
        return host.chars().allMatch(character -> Character.isDigit(character) || character == '.');
    }

    private static boolean hasAmbiguousIpv4Formatting(String host) {
        String[] parts = host.split("\\.", -1);
        for (String part : parts) {
            if (part.length() > 1 && part.startsWith("0")) {
                return true;
            }
        }
        return false;
    }

    private static int[] parseIpv4(String host) {
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        int[] octets = new int[4];
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            if (part.isBlank() || !part.chars().allMatch(Character::isDigit)) {
                return null;
            }
            try {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return null;
                }
                octets[index] = value;
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return octets;
    }

    private static boolean isBroadIpv4(int[] octets) {
        return octets[0] == 0 || octets[0] >= 224
                || (octets[0] == 255 && octets[1] == 255 && octets[2] == 255 && octets[3] == 255);
    }

    private static boolean isLoopbackIpv4(int[] octets) {
        return octets[0] == 127;
    }

    private static boolean isPrivateIpv4(int[] octets) {
        return octets[0] == 10
                || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31)
                || (octets[0] == 192 && octets[1] == 168);
    }

    private static byte[] parseIpv6(String host) {
        if (!host.contains(":") || host.contains("%") || host.contains(".")) {
            return null;
        }
        if (!host.chars().allMatch(ProxyBackendUrlClassifier::isIpv6Character)) {
            return null;
        }

        int compressionIndex = host.indexOf("::");
        if (compressionIndex != host.lastIndexOf("::")) {
            return null;
        }
        List<Integer> hextets;
        if (compressionIndex >= 0) {
            String left = host.substring(0, compressionIndex);
            String right = host.substring(compressionIndex + 2);
            List<Integer> leftHextets = parseIpv6Hextets(left);
            List<Integer> rightHextets = parseIpv6Hextets(right);
            if (leftHextets == null || rightHextets == null) {
                return null;
            }
            int zerosToInsert = 8 - leftHextets.size() - rightHextets.size();
            if (zerosToInsert < 1) {
                return null;
            }
            hextets = new ArrayList<>(8);
            hextets.addAll(leftHextets);
            for (int index = 0; index < zerosToInsert; index++) {
                hextets.add(0);
            }
            hextets.addAll(rightHextets);
        } else {
            hextets = parseIpv6Hextets(host);
            if (hextets == null || hextets.size() != 8) {
                return null;
            }
        }

        if (hextets.size() != 8) {
            return null;
        }
        byte[] bytes = new byte[16];
        for (int index = 0; index < hextets.size(); index++) {
            int value = hextets.get(index);
            bytes[index * 2] = (byte) ((value >>> 8) & 0xff);
            bytes[index * 2 + 1] = (byte) (value & 0xff);
        }
        return bytes;
    }

    private static boolean isIpv6Character(int character) {
        return character == ':' || (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f') || (character >= 'A' && character <= 'F');
    }

    private static List<Integer> parseIpv6Hextets(String value) {
        List<Integer> hextets = new ArrayList<>();
        if (value.isEmpty()) {
            return hextets;
        }
        String[] parts = value.split(":", -1);
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 4) {
                return null;
            }
            try {
                hextets.add(Integer.parseInt(part, 16));
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return hextets;
    }

    private static boolean isUnspecifiedIpv6(byte[] bytes) {
        for (byte value : bytes) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLoopbackIpv6(byte[] bytes) {
        for (int index = 0; index < bytes.length - 1; index++) {
            if (bytes[index] != 0) {
                return false;
            }
        }
        return bytes[15] == 1;
    }

    private static boolean isUniqueLocalIpv6(byte[] bytes) {
        return (bytes[0] & 0xfe) == 0xfc;
    }
}
