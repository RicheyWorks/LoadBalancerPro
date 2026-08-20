package com.richmond423.loadbalancerpro.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthModeConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(AuthModeConfiguration.class);

    @Bean
    ApiKeyVerifier apiKeyVerifier(
            @Value("${loadbalancerpro.api.key:}") String apiKey,
            @Value("${loadbalancerpro.api.rotation-key:}") String rotationKey) {
        return new ApiKeyVerifier(apiKey, rotationKey);
    }

    @Bean
    AuthModeValidator authModeValidator(
            AuthProperties authProperties,
            ApiKeyVerifier apiKeyVerifier) {
        return new AuthModeValidator(authProperties, apiKeyVerifier);
    }

    static final class AuthModeValidator {
        AuthModeValidator(AuthProperties authProperties, ApiKeyVerifier apiKeyVerifier) {
            authProperties.validateApiKeyMode(apiKeyVerifier.isConfigured());
            authProperties.validateOAuth2Mode();
            if (authProperties.isNoneMode()) {
                logger.warn("SECURITY WARNING: loadbalancerpro.auth.mode=none is active; authentication is disabled. "
                        + "Use only for explicitly bounded local development or test execution.");
            }
        }
    }
}
