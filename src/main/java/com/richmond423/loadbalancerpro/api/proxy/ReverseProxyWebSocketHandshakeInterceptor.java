package com.richmond423.loadbalancerpro.api.proxy;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

final class ReverseProxyWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ReverseProxyWebSocketHandshakeInterceptor.class);
    static final String PLAN_ATTRIBUTE =
            ReverseProxyWebSocketHandshakeInterceptor.class.getName() + ".plan";

    private final ReverseProxyService proxyService;

    ReverseProxyWebSocketHandshakeInterceptor(ReverseProxyService proxyService) {
        this.proxyService = Objects.requireNonNull(proxyService, "proxyService cannot be null");
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
        try {
            ReverseProxyWebSocketPlan plan = proxyService.planWebSocket(servletRequest.getServletRequest());
            attributes.put(PLAN_ATTRIBUTE, plan);
            servletRequest.getServletRequest().setAttribute(PLAN_ATTRIBUTE, plan);
            return true;
        } catch (ReverseProxyWebSocketPlanningException exception) {
            logger.warn("proxy.websocket.rejected status={} reason={}",
                    exception.status().value(), exception.errorCode());
            response.setStatusCode(exception.status());
            if (exception.retryAfterSeconds() != null) {
                response.getHeaders().set(
                        HttpHeaders.RETRY_AFTER, Integer.toString(exception.retryAfterSeconds()));
            }
            return false;
        } catch (RuntimeException exception) {
            logger.warn("proxy.websocket.rejected status=500 reason=planning_failure exceptionType={}",
                    exception.getClass().getSimpleName());
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            Object plan = servletRequest.getServletRequest().getAttribute(PLAN_ATTRIBUTE);
            servletRequest.getServletRequest().removeAttribute(PLAN_ATTRIBUTE);
            if (exception != null && plan instanceof ReverseProxyWebSocketPlan webSocketPlan) {
                webSocketPlan.close();
            }
        }
    }
}
