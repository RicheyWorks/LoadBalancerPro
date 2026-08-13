package example.websocket;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

public final class LoopbackWebSocketBackend implements AutoCloseable {
    private static final AtomicReference<String> TEST_HEADER = new AtomicReference<>();
    private static final AtomicReference<String> FORWARDED_FOR = new AtomicReference<>();
    private static final AtomicReference<String> QUERY = new AtomicReference<>();
    private static final AtomicReference<String> API_KEY = new AtomicReference<>();

    private final ConfigurableApplicationContext application;
    private final int port;

    private LoopbackWebSocketBackend(ConfigurableApplicationContext application, int port) {
        this.application = application;
        this.port = port;
    }

    public static LoopbackWebSocketBackend start() {
        TEST_HEADER.set(null);
        FORWARDED_FOR.set(null);
        QUERY.set(null);
        API_KEY.set(null);
        ConfigurableApplicationContext application = new SpringApplicationBuilder(BackendConfiguration.class)
                .web(WebApplicationType.SERVLET)
                .run(
                        "--server.address=127.0.0.1",
                        "--server.port=0",
                        "--spring.main.banner-mode=off",
                        "--management.endpoints.enabled-by-default=false");
        int port = ((WebServerApplicationContext) application).getWebServer().getPort();
        return new LoopbackWebSocketBackend(application, port);
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    public String testHeader() {
        return TEST_HEADER.get();
    }

    public String forwardedFor() {
        return FORWARDED_FOR.get();
    }

    public String query() {
        return QUERY.get();
    }

    public String apiKey() {
        return API_KEY.get();
    }

    @Override
    public void close() {
        application.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            SecurityAutoConfiguration.class,
            ManagementWebSecurityAutoConfiguration.class
    })
    @EnableWebSocket
    static class BackendConfiguration implements WebSocketConfigurer {
        @Bean
        EchoHandler echoHandler() {
            return new EchoHandler();
        }

        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            registry.addHandler(echoHandler(), "/echo")
                    .addInterceptors(new CaptureHandshakeInterceptor());
        }
    }

    private static final class EchoHandler implements WebSocketHandler, SubProtocolCapable {
        @Override
        public List<String> getSubProtocols() {
            return List.of("lbp-test");
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
            if (message instanceof TextMessage text && "oversize".equals(text.getPayload())) {
                session.sendMessage(new TextMessage("x".repeat(2_048)));
                return;
            }
            session.sendMessage(message);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            session.close(CloseStatus.SERVER_ERROR);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        }

        @Override
        public boolean supportsPartialMessages() {
            return true;
        }
    }

    private static final class CaptureHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes) {
            TEST_HEADER.set(request.getHeaders().getFirst("X-Test-Bridge"));
            FORWARDED_FOR.set(request.getHeaders().getFirst("X-Forwarded-For"));
            QUERY.set(request.getURI().getRawQuery());
            API_KEY.set(request.getHeaders().getFirst("X-API-Key"));
            return true;
        }

        @Override
        public void afterHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Exception exception) {
        }
    }
}
