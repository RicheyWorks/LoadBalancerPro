package com.richmond423.loadbalancerpro.api.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;

import javax.net.ssl.SSLEngine;

import com.richmond423.loadbalancerpro.api.TlsTestMaterial;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;

class ReverseProxyBackendTlsTest {
    @Test
    void composesCustomTrustAndClientIdentityForMutualTlsHandshake() throws Exception {
        Path directory = TlsTestMaterial.directory("proxy-backend-tls");
        Path authority = TlsTestMaterial.certificateAuthority(directory, "backend-ca");
        Path backendKeyStore = TlsTestMaterial.signedKeyStore(
                directory, "backend", "loopback-backend", "ip:127.0.0.1", authority);
        Path clientKeyStore = TlsTestMaterial.signedKeyStore(
                directory, "proxy-client", "proxy-client", "dns:proxy-client", authority);
        Path proxyTrust = TlsTestMaterial.trustStore(directory, "proxy-trust", authority);
        Path backendTrust = TlsTestMaterial.trustStore(directory, "backend-trust", authority);

        DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry();
        SslBundle trustBundle = TlsTestMaterial.sslBundle(null, proxyTrust);
        registry.registerBundle("backend-trust", trustBundle);
        registry.registerBundle("proxy-client", TlsTestMaterial.sslBundle(clientKeyStore, null));
        HttpClient defaultClient = HttpClient.newHttpClient();
        ReverseProxyHttpClientProvider provider =
                new ReverseProxyHttpClientProvider(defaultClient, Duration.ofSeconds(1), registry);
        ReverseProxyProperties.BackendTls backendTls = new ReverseProxyProperties.BackendTls();
        backendTls.setTruststore("backend-trust");
        ReverseProxyProperties.Upstream upstream = upstream("https://127.0.0.1:8443");
        upstream.getTls().setClientCert("proxy-client");

        HttpClient configuredClient = provider.clientFor(backendTls, upstream);
        assertThat(configuredClient).isNotSameAs(defaultClient);
        assertThat(configuredClient.sslParameters().getEndpointIdentificationAlgorithm()).isEqualTo("HTTPS");

        SSLEngine client = configuredClient.sslContext().createSSLEngine("127.0.0.1", 8443);
        client.setSSLParameters(configuredClient.sslParameters());
        SSLEngine server = TlsTestMaterial.sslContext(backendKeyStore, backendTrust).createSSLEngine();
        server.setNeedClientAuth(true);
        TlsTestMaterial.HandshakeResult result = TlsTestMaterial.handshake(client, server);

        assertThat(result.serverIdentity().getName()).contains("CN=loopback-backend");
        assertThat(result.clientIdentity().getName()).contains("CN=proxy-client");

        registry.updateBundle("backend-trust", trustBundle);
        assertThat(provider.clientFor(backendTls, upstream)).isNotSameAs(configuredClient);
    }

    @Test
    void defaultsToSystemTrustAndFailsClosedForUnsafeOrIncoherentTlsSettings() {
        HttpClient defaultClient = HttpClient.newHttpClient();
        ReverseProxyHttpClientProvider provider =
                ReverseProxyHttpClientProvider.systemDefault(defaultClient, Duration.ofSeconds(1));
        ReverseProxyProperties.BackendTls backendTls = new ReverseProxyProperties.BackendTls();
        ReverseProxyProperties.Upstream https = upstream("https://127.0.0.1");

        assertThat(provider.clientFor(backendTls, https)).isSameAs(defaultClient);

        https.getTls().setVerify(false);
        assertThatThrownBy(() -> provider.clientFor(backendTls, https))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tls.verify=false is not supported");

        ReverseProxyProperties.Upstream http = upstream("http://127.0.0.1:8080");
        http.getTls().setClientCert("proxy-client");
        assertThatThrownBy(() -> provider.clientFor(backendTls, http))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("require an https upstream URL");

        backendTls.setTruststore("missing");
        assertThatThrownBy(() -> provider.clientFor(backendTls, upstream("https://127.0.0.1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SSL bundles are unavailable");
    }

    private static ReverseProxyProperties.Upstream upstream(String url) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId("backend");
        upstream.setUrl(url);
        return upstream;
    }
}
