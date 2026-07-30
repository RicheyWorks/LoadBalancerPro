package com.richmond423.loadbalancerpro.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RoutingApiLimitsProperties.class)
public class RoutingApiLimitsConfiguration {
}
