package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProxyOperatorStatusPageTest {
    private static final Path PROXY_STATUS_PAGE =
            Path.of("src/main/resources/static/proxy-status.html");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void proxyStatusPageExistsAndIsServed() throws Exception {
        assertTrue(Files.exists(PROXY_STATUS_PAGE), "proxy status page should be source-controlled");

        mockMvc.perform(get("/proxy-status.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("LoadBalancerPro Proxy Operator Status")))
                .andExpect(content().string(containsString("/api/proxy/status")));
    }

    @Test
    void proxyStatusPageContainsExpectedControlsSectionsAndLabels() throws Exception {
        String page = Files.readString(PROXY_STATUS_PAGE, StandardCharsets.UTF_8);

        assertTrue(page.contains("LoadBalancerPro Proxy Operator Status"));
        assertTrue(page.contains("Refresh status"));
        assertTrue(page.contains("Start live refresh"));
        assertTrue(page.contains("Stop live refresh"));
        assertTrue(page.contains("Copy status summary"));
        assertTrue(page.contains("Copy demo curl commands"));
        assertTrue(page.contains("Raw JSON display"));
        assertTrue(page.contains("upstream-table"));
        assertTrue(page.contains("Counters table"));
        assertTrue(page.contains("Retry / cooldown section"));
        assertTrue(page.contains("Safety / limitations"));
        assertTrue(page.contains("/api/proxy/status"));
        assertTrue(page.contains("Proxy enabled"));
        assertTrue(page.contains("Strategy"));
        assertTrue(page.contains("Health check enabled"));
        assertTrue(page.contains("Retry enabled"));
        assertTrue(page.contains("Cooldown enabled"));
        assertTrue(page.contains("Total forwarded"));
        assertTrue(page.contains("Total failure"));
        assertTrue(page.contains("Total retry"));
        assertTrue(page.contains("Total cooldown"));
        assertTrue(page.contains("2xx"));
        assertTrue(page.contains("3xx"));
        assertTrue(page.contains("4xx"));
        assertTrue(page.contains("5xx"));
        assertTrue(page.contains("Other"));
        assertTrue(page.contains("Last selected upstream"));
        assertTrue(page.contains("Cooldown active"));
        assertTrue(page.contains("Consecutive failures"));
        assertTrue(page.contains("cooldownRemainingMillis"));
        assertTrue(page.contains("consecutiveFailures"));
        assertTrue(page.contains("totalRetryAttempts"));
        assertTrue(page.contains("totalCooldownActivations"));
        assertTrue(page.contains("lastSelectedUpstream"));
        assertTrue(page.contains("PROXY_DEMO_STACK.md"));
        assertTrue(page.contains("PROXY_STRATEGY_DEMO_LAB.md"));
        assertTrue(page.contains("ProxyDemoFixtureLauncher"));
        assertTrue(page.contains("java -cp target/classes"));
        assertTrue(page.contains("-Mode round-robin"));
        assertTrue(page.contains("-Mode weighted-round-robin"));
        assertTrue(page.contains("-Mode failover"));
        assertTrue(page.contains("scripts/proxy-demo.sh --mode round-robin"));
        assertTrue(page.contains("scripts/proxy-demo.sh --mode weighted-round-robin"));
        assertTrue(page.contains("scripts/proxy-demo.sh --mode failover"));
        assertTrue(page.contains("/proxy/weighted?step=1"));
        assertTrue(page.contains("/proxy/failover?step=1"));
    }

    @Test
    void proxyStatusPageUsesOnlyReadOnlySameOriginStatusEndpoint() throws Exception {
        String page = Files.readString(PROXY_STATUS_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("fetch(STATUS_ENDPOINT"));
        assertTrue(page.contains("method: \"GET\""));
        assertTrue(page.contains("same-origin <code>GET /api/proxy/status</code>"));
        assertTrue(page.contains("No backend writes"));
        assertTrue(page.contains("No browser storage"));
        assertTrue(page.contains("No cloud mutation"));
        assertTrue(page.contains("Live refresh state is in-memory only"));
        assertTrue(page.contains("Status endpoint unavailable."));
        assertTrue(page.contains("Error state: "));
        assertFalse(normalized.contains("method: \"post\""));
        assertFalse(normalized.contains("method: 'post'"));
        assertFalse(normalized.contains("method=\"post\""));
        assertFalse(normalized.contains("data-action=\"reset"));
        assertFalse(normalized.contains("id=\"reset"));
        assertFalse(normalized.contains("reset metrics"));
        assertFalse(normalized.contains("reset cooldown"));
        assertFalse(normalized.contains("delete-branch"));
    }

    @Test
    void proxyStatusPageHasNoExternalFrontendStorageCloudOrInflatedClaims() throws Exception {
        String page = Files.readString(PROXY_STATUS_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertFalse(normalized.contains("<script src="));
        assertFalse(normalized.contains("<link rel=\"stylesheet\""));
        assertFalse(normalized.contains("<img"));
        assertFalse(normalized.contains("background-image"));
        assertFalse(normalized.contains("cdn."));
        assertFalse(normalized.contains("fonts.googleapis"));
        assertFalse(normalized.contains("fonts.gstatic"));
        assertFalse(normalized.contains("localstorage"));
        assertFalse(normalized.contains("sessionstorage"));
        assertFalse(normalized.contains("x-api-key"));
        assertFalse(normalized.contains("bearer"));
        assertFalse(normalized.contains("authorization"));
        assertFalse(normalized.contains("type=\"password\""));
        assertFalse(normalized.contains("password"));
        assertFalse(normalized.contains("access key"));
        assertFalse(normalized.contains("secret key"));
        assertFalse(normalized.contains("cloudmanager"));
        assertFalse(normalized.contains("production-grade"));
        assertFalse(normalized.contains("benchmark"));
        assertFalse(normalized.contains("certification"));
        assertFalse(normalized.contains("legal compliance"));
        assertFalse(normalized.contains("identity proof"));
    }
}
