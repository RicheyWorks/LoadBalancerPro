package com.richmond423.loadbalancerpro.api.proxy;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReverseProxyProperties.class)
public class ReverseProxyConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "loadbalancerpro.proxy", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    HttpClient reverseProxyHttpClient(ReverseProxyProperties properties) {
        Duration connectTimeout = properties.getConnectTimeout();
        if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.connect-timeout must be greater than zero");
        }
        return HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "loadbalancerpro.proxy", name = "enabled", havingValue = "true")
    ReverseProxyHttpClientProvider reverseProxyHttpClientProvider(
            HttpClient reverseProxyHttpClient,
            ReverseProxyProperties properties,
            ObjectProvider<SslBundles> sslBundlesProvider) {
        return new ReverseProxyHttpClientProvider(
                reverseProxyHttpClient,
                properties.getConnectTimeout(),
                sslBundlesProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnProperty(prefix = "loadbalancerpro.proxy", name = "enabled", havingValue = "true")
    ReverseProxyAccessLog reverseProxyAccessLog(ReverseProxyProperties properties) {
        return new ReverseProxyAccessLog(properties);
    }
}
