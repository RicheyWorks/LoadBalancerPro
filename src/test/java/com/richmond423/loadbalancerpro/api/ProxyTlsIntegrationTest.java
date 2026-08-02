package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

class ProxyTlsIntegrationTest {
    @Test
    void configuresTomcatHttpsAndSniThenReloadsCertificateWithoutRestart() throws Exception {
        Path directory = TlsTestMaterial.directory("proxy-inbound-tls");
        Path authority = TlsTestMaterial.certificateAuthority(directory, "inbound-ca");
        Path defaultKeyStore = TlsTestMaterial.signedKeyStore(
                directory, "default", "default.localhost",
                "dns:default.localhost,dns:localhost,ip:127.0.0.1", authority);
        Path sniKeyStore = TlsTestMaterial.signedKeyStore(
                directory, "sni", "sni.localhost",
                "dns:sni.localhost,dns:localhost,ip:127.0.0.1", authority);
        Path replacementKeyStore = TlsTestMaterial.signedKeyStore(
                directory, "replacement", "reloaded.localhost",
                "dns:default.localhost,dns:localhost,ip:127.0.0.1", authority);
        Path clientTrust = TlsTestMaterial.trustStore(directory, "inbound-client-trust", authority);
        SSLContext clientContext = TlsTestMaterial.sslContext(null, clientTrust);

        Map<String, Object> properties = baseApplicationProperties();
        properties.put("server.ssl.enabled", "true");
        properties.put("server.ssl.bundle", "default-server");
        properties.put("server.ssl.server-name-bundles[0].server-name", "sni.localhost");
        properties.put("server.ssl.server-name-bundles[0].bundle", "sni-server");
        jksKeyStoreBundle(properties, "default-server", defaultKeyStore);
        jksKeyStoreBundle(properties, "sni-server", sniKeyStore);

        try (ConfigurableApplicationContext application = startApplication(properties)) {
            int originalPort = port(application);
            AbstractHttp11Protocol<?> protocol = httpsProtocol(application);
            assertTrue(protocol.isSSLEnabled());
            assertTrue(serverIdentity(clientContext, hostConfig(protocol, "_default_"), "default.localhost")
                    .contains("CN=default.localhost"));
            assertTrue(serverIdentity(clientContext, hostConfig(protocol, "sni.localhost"), "sni.localhost")
                    .contains("CN=sni.localhost"));

            Files.copy(replacementKeyStore, defaultKeyStore, StandardCopyOption.REPLACE_EXISTING);
            long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
            String subject = "";
            while (System.nanoTime() < deadline) {
                subject = serverIdentity(
                        clientContext, hostConfig(protocol, "_default_"), "default.localhost");
                if (subject.contains("CN=reloaded.localhost")) {
                    break;
                }
                Thread.sleep(200);
            }
            assertTrue(subject.contains("CN=reloaded.localhost"),
                    "certificate reload did not converge: " + subject);
            assertEquals(originalPort, port(application), "certificate reload must not restart Tomcat");
        }
    }

    private static String serverIdentity(
            SSLContext clientContext, SSLHostConfig hostConfig, String peerHost) throws Exception {
        SSLHostConfigCertificate certificate = hostConfig.getCertificates().iterator().next();
        SSLEngine client = clientContext.createSSLEngine(peerHost, 443);
        SSLParameters parameters = client.getSSLParameters();
        parameters.setEndpointIdentificationAlgorithm("HTTPS");
        client.setSSLParameters(parameters);
        SSLEngine server = certificate.getSslContext().createSSLEngine();
        return TlsTestMaterial.handshake(client, server).serverIdentity().getName();
    }

    private static SSLHostConfig hostConfig(AbstractHttp11Protocol<?> protocol, String hostName) {
        return Arrays.stream(protocol.findSslHostConfigs())
                .filter(config -> hostName.equals(config.getHostName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing TLS host configuration " + hostName));
    }

    private static AbstractHttp11Protocol<?> httpsProtocol(ConfigurableApplicationContext application) {
        WebServerApplicationContext web = (WebServerApplicationContext) application;
        org.springframework.boot.web.embedded.tomcat.TomcatWebServer server =
                (org.springframework.boot.web.embedded.tomcat.TomcatWebServer) web.getWebServer();
        return (AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler();
    }

    private static ConfigurableApplicationContext startApplication(Map<String, Object> properties) {
        String[] arguments = properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
        return new SpringApplicationBuilder(LoadBalancerApiApplication.class).run(arguments);
    }

    private static Map<String, Object> baseApplicationProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("server.address", "127.0.0.1");
        properties.put("server.port", "0");
        properties.put("spring.main.banner-mode", "off");
        properties.put("loadbalancerpro.auth.mode", "none");
        properties.put("management.endpoints.enabled-by-default", "false");
        return properties;
    }

    private static void jksKeyStoreBundle(Map<String, Object> properties, String name, Path keyStore) {
        String prefix = "spring.ssl.bundle.jks." + name;
        properties.put(prefix + ".keystore.location", keyStore.toUri().toString());
        properties.put(prefix + ".keystore.password", TlsTestMaterial.PASSWORD);
        properties.put(prefix + ".keystore.type", "PKCS12");
        properties.put(prefix + ".reload-on-update", "true");
    }

    private static int port(ConfigurableApplicationContext application) {
        return ((WebServerApplicationContext) application).getWebServer().getPort();
    }
}
