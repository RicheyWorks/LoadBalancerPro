package com.richmond423.loadbalancerpro.api.config;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyAuditLog;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingObservabilityMetrics;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdaptiveRoutingPolicyProperties.class)
public class AdaptiveRoutingPolicyConfiguration {

    @Bean
    AdaptiveRoutingObservabilityMetrics adaptiveRoutingObservabilityMetrics() {
        return new AdaptiveRoutingObservabilityMetrics();
    }

    @Bean
    AdaptiveRoutingPolicyAuditLog adaptiveRoutingPolicyAuditLog(
            AdaptiveRoutingPolicyProperties properties,
            AdaptiveRoutingObservabilityMetrics observabilityMetrics) {
        return new AdaptiveRoutingPolicyAuditLog(
                properties.getMaxAuditEvents(), java.time.Clock.systemUTC(), observabilityMetrics);
    }
}
