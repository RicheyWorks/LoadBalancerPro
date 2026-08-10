package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProxyDnsDiscoveryTest {
    @Test
    void compilesNormalizedDnsContractAndHttpTemplate() {
        ProxyDnsDiscovery.Spec spec = ProxyDnsDiscovery.compile(
                "dns:Service.Example:8080", "http://service.example:8080/base",
                "address", "upstream.discovery");

        assertEquals("service.example", spec.name());
        assertEquals(8080, spec.port());
        assertEquals("/base", spec.template().getPath());
        assertEquals("service", ProxyDnsDiscovery.compile(
                "dns:service:8080", "http://service:8080", "address", "upstream.discovery").name());
        assertEquals("address", spec.authorityMode());
    }

    @Test
    void rejectsUnsafeOrAmbiguousDiscoveryContracts() {
        assertInvalid("https discovery", "dns:service.example:443", "https://service.example");
        assertInvalid("mismatched host", "dns:service.example:8080", "http://other.example:8080");
        assertInvalid("mismatched port", "dns:service.example:8080", "http://service.example:8081");
        assertInvalid("IP literal", "dns:127.0.0.1:8080", "http://127.0.0.1:8080");
        assertInvalid("trailing dot", "dns:service.example.:8080", "http://service.example.:8080");
        assertInvalid("invalid port", "dns:service.example:0", "http://service.example:0");
        assertThrows(IllegalStateException.class, () -> ProxyDnsDiscovery.compile(
                "dns:service.example:8080", "http://service.example:8080", "", "upstream.discovery"));
        assertThrows(IllegalStateException.class, () -> ProxyDnsDiscovery.compile(
                "dns:service.example:8080", "http://service.example:8080", "logical", "upstream.discovery"));
    }

    @Test
    void canonicalizesDeduplicatesSortsAndBuildsStableMembers() throws Exception {
        ProxyDnsDiscovery.Spec spec = ProxyDnsDiscovery.compile(
                "dns:service.example:8080", "http://service.example:8080/base",
                "address", "upstream.discovery");
        InetAddress second = literal(127, 0, 0, 2);
        InetAddress first = literal(127, 0, 0, 1);

        List<ProxyDnsDiscovery.Member> members = ProxyDnsDiscovery.members(
                spec, "logical-backend", List.of(second, first, first), true);

        assertEquals(List.of("127.0.0.1", "127.0.0.2"),
                members.stream().map(ProxyDnsDiscovery.Member::address).toList());
        assertEquals("http://127.0.0.1:8080/base", members.get(0).endpoint().toString());
        assertEquals(members, ProxyDnsDiscovery.members(spec, "logical-backend", List.of(first, second), true));
        assertTrue(members.stream().allMatch(member -> member.id().length() <= 64));
    }

    @Test
    void filtersPublicAnswersWhenPrivateNetworkValidationIsEnabled() throws Exception {
        ProxyDnsDiscovery.Spec spec = ProxyDnsDiscovery.compile(
                "dns:service.example:8080", "http://service.example:8080",
                "address", "upstream.discovery");
        InetAddress loopback = literal(127, 0, 0, 1);
        InetAddress publicAddress = literal(8, 8, 8, 8);

        assertEquals(List.of("127.0.0.1"), ProxyDnsDiscovery.members(
                spec, "backend", List.of(publicAddress, loopback), true).stream()
                .map(ProxyDnsDiscovery.Member::address)
                .toList());
        assertEquals(2, ProxyDnsDiscovery.members(
                spec, "backend", List.of(publicAddress, loopback), false).size());
    }

    @Test
    void rejectsOverflowingCanonicalMemberSetWithoutPublishingAPartialSet() throws Exception {
        ProxyDnsDiscovery.Spec spec = ProxyDnsDiscovery.compile(
                "dns:service.example:8080", "http://service.example:8080",
                "address", "upstream.discovery");
        List<InetAddress> answers = new ArrayList<>();
        for (int value = 40; value >= 1; value--) {
            answers.add(literal(10, 0, 0, value));
        }

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ProxyDnsDiscovery.members(spec, "backend", answers, true));
        assertTrue(exception.getMessage().contains("more than 32"));
    }

    @Test
    void preservesEscapedBasePathWithoutDoubleEncodingOrDecoding() throws Exception {
        ProxyDnsDiscovery.Spec spec = ProxyDnsDiscovery.compile(
                "dns:service.example:8080",
                "http://service.example:8080/api%2Fv1/%7Euser/%25value",
                "address", "upstream.discovery");

        ProxyDnsDiscovery.Member member = ProxyDnsDiscovery.members(
                spec, "backend", List.of(literal(127, 0, 0, 1)), true).get(0);

        assertEquals("/api%2Fv1/%7Euser/%25value", member.endpoint().getRawPath());
        assertEquals("/api/v1/~user/%value", member.endpoint().getPath());
        assertEquals("http://127.0.0.1:8080/api%2Fv1/%7Euser/%25value", member.endpoint().toString());
    }

    private static void assertInvalid(String label, String discovery, String template) {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ProxyDnsDiscovery.compile(discovery, template, "address", "upstream.discovery"), label);
        assertTrue(exception.getMessage().startsWith("upstream."), label);
    }

    private static InetAddress literal(int... octets) throws Exception {
        byte[] bytes = new byte[octets.length];
        for (int index = 0; index < octets.length; index++) {
            bytes[index] = (byte) octets[index];
        }
        return InetAddress.getByAddress(bytes);
    }
}
