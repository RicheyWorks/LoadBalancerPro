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
    AuthModeValidator authModeValidator(
            AuthProperties authProperties,
            @Value("${loadbalancerpro.api.key:}") String apiKey) {
        return new AuthModeValidator(authProperties, apiKey);
    }

    static final class AuthModeValidator {
        AuthModeValidator(AuthProperties authProperties, String apiKey) {
            authProperties.validateApiKeyMode(apiKey);
            authProperties.validateOAuth2Mode();
            if (authProperties.isNoneMode()) {
                logger.warn("SECURITY WARNING: loadbalancerpro.auth.mode=none is active; authentication is disabled. "
                        + "Use only for explicitly bounded local development or test execution.");
            }
        }
    }
}
