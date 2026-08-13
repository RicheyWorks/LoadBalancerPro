package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.apache.coyote.http2.Http2Protocol;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.context.ConfigurableApplicationContext;

class ReverseProxyHttp2IntegrationTest {
    @Test
    void enablesTomcatHttp2AndNegotiatesCleartextUpgrade() throws Exception {
        try (ConfigurableApplicationContext application = new SpringApplicationBuilder(
                LoadBalancerApiApplication.class).run(
                        "--server.address=127.0.0.1",
                        "--server.port=0",
                        "--server.http2.enabled=true",
                        "--spring.main.banner-mode=off",
                        "--loadbalancerpro.auth.mode=none",
                        "--management.endpoints.enabled-by-default=false")) {
            TomcatWebServer server = (TomcatWebServer)
                    ((WebServerApplicationContext) application).getWebServer();
            assertTrue(java.util.Arrays.stream(server.getTomcat().getConnector().findUpgradeProtocols())
                    .anyMatch(Http2Protocol.class::isInstance));

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://127.0.0.1:" + server.getPort() + "/api/health"))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals(HttpClient.Version.HTTP_2, response.version());
        }
    }
}
