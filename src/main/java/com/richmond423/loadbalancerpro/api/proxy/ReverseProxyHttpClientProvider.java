package com.richmond423.loadbalancerpro.api.proxy;

import java.net.URI;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;

/** Builds strict per-upstream clients from named Spring SSL bundles. */
final class ReverseProxyHttpClientProvider {
    private final HttpClient defaultClient;
    private final Duration connectTimeout;
    private final SslBundles sslBundles;
    private final ConcurrentMap<ClientKey, HttpClient> clients = new ConcurrentHashMap<>();
    private final Set<String> watchedBundles = ConcurrentHashMap.newKeySet();

    ReverseProxyHttpClientProvider(HttpClient defaultClient, Duration connectTimeout, SslBundles sslBundles) {
        this.defaultClient = Objects.requireNonNull(defaultClient, "defaultClient cannot be null");
        this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout cannot be null");
        this.sslBundles = sslBundles;
    }

    static ReverseProxyHttpClientProvider systemDefault(HttpClient defaultClient, Duration connectTimeout) {
        return new ReverseProxyHttpClientProvider(defaultClient, connectTimeout, null);
    }

    HttpClient clientFor(
            ReverseProxyProperties.BackendTls backendTls,
            ReverseProxyProperties.Upstream upstream) {
        Objects.requireNonNull(backendTls, "backendTls cannot be null");
        Objects.requireNonNull(upstream, "upstream cannot be null");
        ReverseProxyProperties.Tls tls = Objects.requireNonNull(upstream.getTls(), "upstream.tls cannot be null");
        if (!tls.isVerify()) {
            throw new IllegalStateException("loadbalancerpro.proxy.upstreams[].tls.verify=false is not supported");
        }

        String trustBundle = normalizedName(backendTls.getTruststore());
        String clientCertBundle = normalizedName(tls.getClientCert());
        URI upstreamUri = URI.create(Objects.requireNonNull(upstream.getUrl(), "upstream.url cannot be null"));
        if (!"https".equalsIgnoreCase(upstreamUri.getScheme())) {
            if (!trustBundle.isEmpty() || !clientCertBundle.isEmpty()) {
                throw new IllegalStateException("backend TLS bundles require an https upstream URL");
            }
            return defaultClient;
        }
        if (trustBundle.isEmpty() && clientCertBundle.isEmpty()) {
            return defaultClient;
        }
        if (sslBundles == null) {
            throw new IllegalStateException("Spring SSL bundles are unavailable for configured backend TLS");
        }

        watch(trustBundle);
        watch(clientCertBundle);
        ClientKey key = new ClientKey(trustBundle, clientCertBundle);
        return clients.computeIfAbsent(key, this::buildClient);
    }

    private HttpClient buildClient(ClientKey key) {
        SslBundle trustBundle = bundle(key.trustBundle());
        SslBundle clientBundle = bundle(key.clientCertBundle());
        TrustManager[] trustManagers = trustBundle == null
                ? SslBundle.systemDefault().getManagers().getTrustManagers()
                : trustBundle.getManagers().getTrustManagers();
        KeyManager[] keyManagers = clientBundle == null
                ? null
                : clientBundle.getManagers().getKeyManagers();
        String protocol = trustBundle != null
                ? trustBundle.getProtocol()
                : clientBundle.getProtocol();
        try {
            SSLContext context = SSLContext.getInstance(protocol);
            context.init(keyManagers, trustManagers, null);
            SSLParameters parameters = new SSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            SslBundle optionsBundle = trustBundle != null ? trustBundle : clientBundle;
            if (optionsBundle.getOptions().getCiphers() != null) {
                parameters.setCipherSuites(optionsBundle.getOptions().getCiphers());
            }
            if (optionsBundle.getOptions().getEnabledProtocols() != null) {
                parameters.setProtocols(optionsBundle.getOptions().getEnabledProtocols());
            }
            return HttpClient.newBuilder()
                    .connectTimeout(connectTimeout)
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .sslContext(context)
                    .sslParameters(parameters)
                    .build();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("backend TLS context could not be initialized", exception);
        }
    }

    private SslBundle bundle(String name) {
        return name.isEmpty() ? null : sslBundles.getBundle(name);
    }

    private void watch(String name) {
        if (!name.isEmpty() && watchedBundles.add(name)) {
            sslBundles.addBundleUpdateHandler(name, ignored -> clients.clear());
        }
    }

    private static String normalizedName(String name) {
        return name == null ? "" : name.trim();
    }

    private record ClientKey(String trustBundle, String clientCertBundle) {
    }
}
