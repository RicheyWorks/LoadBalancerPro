package com.richmond423.loadbalancerpro.api.proxy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;

@Configuration(proxyBeanMethods = false)
@EnableWebSocket
@ConditionalOnProperty(
        prefix = "loadbalancerpro.proxy",
        name = {"enabled", "websocket.enabled"},
        havingValue = "true")
class ReverseProxyWebSocketConfiguration implements WebSocketConfigurer {
    private final ReverseProxyWebSocketHandler handler;
    private final ReverseProxyService proxyService;
    private final ReverseProxyProperties properties;

    ReverseProxyWebSocketConfiguration(
            ReverseProxyWebSocketHandler handler,
            ReverseProxyService proxyService,
            ReverseProxyProperties properties) {
        this.handler = handler;
        this.proxyService = proxyService;
        this.properties = properties;
    }

    @Bean
    ServletServerContainerFactoryBean createWebSocketContainer() {
        ReverseProxyProperties.WebSocket websocket = properties.getWebsocket();
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setAsyncSendTimeout(websocket.getSendTimeout().toMillis());
        container.setMaxSessionIdleTimeout(websocket.getIdleTimeout().toMillis());
        container.setMaxTextMessageBufferSize(websocket.getMaxTextMessageBytes());
        container.setMaxBinaryMessageBufferSize(websocket.getMaxBinaryMessageBytes());
        return container;
    }

    @Bean
    static BeanPostProcessor reverseProxyWebSocketHandlerMappingOrder() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof WebSocketHandlerMapping mapping
                        && "webSocketHandlerMapping".equals(beanName)) {
                    mapping.setOrder(-1);
                }
                return bean;
            }
        };
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        WebSocketHandlerRegistration registration = registry
                .addHandler(handler, "/proxy", "/proxy/", "/proxy/**")
                .addInterceptors(new ReverseProxyWebSocketHandshakeInterceptor(proxyService));
        if (!properties.getWebsocket().getAllowedOrigins().isEmpty()) {
            registration.setAllowedOrigins(
                    properties.getWebsocket().getAllowedOrigins().toArray(String[]::new));
        }
    }
}
