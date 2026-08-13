package com.richmond423.loadbalancerpro.api.proxy;

import java.io.IOException;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

@Component
@ConditionalOnProperty(
        prefix = "loadbalancerpro.proxy",
        name = {"enabled", "websocket.enabled"},
        havingValue = "true")
final class ReverseProxyWebSocketHandler
        implements WebSocketHandler, SubProtocolCapable, SmartLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(ReverseProxyWebSocketHandler.class);
    private static final int MAX_CLOSE_REASON_CHARS = 120;

    private final ReverseProxyProperties.WebSocket properties;
    private final ConcurrentMap<String, Bridge> bridges = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean();

    ReverseProxyWebSocketHandler(ReverseProxyProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null").getWebsocket();
    }

    @Override
    public List<String> getSubProtocols() {
        return properties.getSubprotocols().stream().map(String::trim).toList();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object planAttribute = session.getAttributes().remove(
                ReverseProxyWebSocketHandshakeInterceptor.PLAN_ATTRIBUTE);
        if (!(planAttribute instanceof ReverseProxyWebSocketPlan plan)) {
            closeQuietly(session, CloseStatus.SERVICE_RESTARTED);
            return;
        }
        if (!plan.claim() || !running.get()) {
            plan.close();
            closeQuietly(session, CloseStatus.SERVER_ERROR);
            return;
        }

        ConcurrentWebSocketSessionDecorator downstream;
        try {
            session.setTextMessageSizeLimit(properties.getMaxTextMessageBytes());
            session.setBinaryMessageSizeLimit(properties.getMaxBinaryMessageBytes());
            downstream = new ConcurrentWebSocketSessionDecorator(
                    session,
                    durationMillisAsInt(properties.getSendTimeout()),
                    plan.sendBufferBytes(),
                    ConcurrentWebSocketSessionDecorator.OverflowStrategy.TERMINATE);
        } catch (RuntimeException exception) {
            plan.close();
            closeQuietly(session, CloseStatus.SERVER_ERROR);
            return;
        }
        Bridge bridge = new Bridge(plan, downstream);
        if (bridges.putIfAbsent(session.getId(), bridge) != null) {
            plan.close();
            closeQuietly(session, CloseStatus.SERVER_ERROR);
            return;
        }
        try {
            bridge.connect(session.getAcceptedProtocol());
            logger.info("proxy.websocket.connected route={} upstreamId={} strategy={}",
                    plan.routeName(), plan.upstreamId(), plan.strategyName());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            bridge.fail("upstream_connect_interrupted", exception);
        } catch (ExecutionException | TimeoutException | RuntimeException exception) {
            bridge.fail("upstream_connect_failed", exception);
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        Bridge bridge = bridges.get(session.getId());
        if (bridge == null) {
            closeQuietly(session, CloseStatus.SERVER_ERROR);
            return;
        }
        try {
            bridge.sendUpstream(message);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            bridge.fail("upstream_send_interrupted", exception);
        } catch (ExecutionException | TimeoutException | RuntimeException exception) {
            bridge.fail("upstream_send_failed", exception);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Bridge bridge = bridges.get(session.getId());
        if (bridge != null) {
            bridge.fail("downstream_transport_error", exception);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        Bridge bridge = bridges.remove(session.getId());
        if (bridge != null) {
            bridge.downstreamClosed(closeStatus);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return true;
    }

    @Override
    public void start() {
        running.set(true);
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            bridges.values().forEach(Bridge::serviceStopping);
            bridges.clear();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    int activeConnectionCount() {
        return bridges.size();
    }

    private final class Bridge implements WebSocket.Listener {
        private final ReverseProxyWebSocketPlan plan;
        private final ConcurrentWebSocketSessionDecorator downstream;
        private final AtomicReference<WebSocket> upstream = new AtomicReference<>();
        private final AtomicBoolean terminal = new AtomicBoolean();
        private int inboundTextBytes;
        private int inboundBinaryBytes;

        private Bridge(
                ReverseProxyWebSocketPlan plan,
                ConcurrentWebSocketSessionDecorator downstream) {
            this.plan = plan;
            this.downstream = downstream;
        }

        private void connect(String acceptedProtocol)
                throws InterruptedException, ExecutionException, TimeoutException {
            WebSocket.Builder builder = plan.httpClient().newWebSocketBuilder()
                    .connectTimeout(plan.connectTimeout());
            plan.headers().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
            if (acceptedProtocol != null && !acceptedProtocol.isBlank()) {
                builder.subprotocols(acceptedProtocol);
            }
            WebSocket connected = builder.buildAsync(plan.targetUri(), this)
                    .get(plan.connectTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (acceptedProtocol != null && !acceptedProtocol.isBlank()
                    && !acceptedProtocol.equals(connected.getSubprotocol())) {
                connected.abort();
                throw new IllegalStateException("Upstream did not accept the downstream WebSocket subprotocol");
            }
            upstream.set(connected);
            plan.markConnected();
            if (terminal.get()) {
                connected.abort();
                throw new IllegalStateException("Upstream WebSocket closed during connection establishment");
            }
            connected.request(1);
        }

        private void sendUpstream(WebSocketMessage<?> message)
                throws InterruptedException, ExecutionException, TimeoutException {
            WebSocket target = upstream.get();
            if (target == null || terminal.get()) {
                throw new IllegalStateException("WebSocket upstream is unavailable");
            }
            CompletableFuture<WebSocket> send;
            if (message instanceof TextMessage text) {
                send = target.sendText(text.getPayload(), text.isLast());
            } else if (message instanceof BinaryMessage binary) {
                send = target.sendBinary(binary.getPayload().asReadOnlyBuffer(), binary.isLast());
            } else if (message instanceof PingMessage ping) {
                send = target.sendPing(ping.getPayload().asReadOnlyBuffer());
            } else if (message instanceof PongMessage pong) {
                send = target.sendPong(pong.getPayload().asReadOnlyBuffer());
            } else {
                throw new IllegalArgumentException("Unsupported WebSocket message type");
            }
            send.get(plan.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            // Demand starts only after subprotocol validation in connect().
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            int fragmentBytes = data.toString().getBytes(StandardCharsets.UTF_8).length;
            if (exceedsInboundLimit(fragmentBytes, properties.getMaxTextMessageBytes(), true, last)) {
                tooLarge(webSocket);
                return CompletableFuture.completedFuture(null);
            }
            return forwardDownstream(webSocket, new TextMessage(data, last));
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            if (exceedsInboundLimit(data.remaining(), properties.getMaxBinaryMessageBytes(), false, last)) {
                tooLarge(webSocket);
                return CompletableFuture.completedFuture(null);
            }
            return forwardDownstream(webSocket, new BinaryMessage(data.asReadOnlyBuffer(), last));
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            return forwardDownstream(webSocket, new PingMessage(message.asReadOnlyBuffer()));
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            return forwardDownstream(webSocket, new PongMessage(message.asReadOnlyBuffer()));
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (terminal.compareAndSet(false, true)) {
                bridges.remove(downstream.getId(), this);
                closeQuietly(downstream, closeStatus(statusCode, reason));
                boolean upstreamFailure = upstreamFailureClose(statusCode);
                plan.complete(
                        !upstreamFailure,
                        upstreamFailure,
                        upstreamFailure
                                ? ReverseProxyMetrics.TerminalOutcome.UPSTREAM_TRANSPORT_FAILURE
                                : ReverseProxyMetrics.TerminalOutcome.SUCCESS);
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            fail("upstream_transport_error", error);
        }

        private CompletionStage<?> forwardDownstream(WebSocket webSocket, WebSocketMessage<?> message) {
            if (terminal.get()) {
                return CompletableFuture.completedFuture(null);
            }
            try {
                downstream.sendMessage(message);
                webSocket.request(1);
                return CompletableFuture.completedFuture(null);
            } catch (IOException | RuntimeException exception) {
                fail("downstream_send_failed", exception);
                return CompletableFuture.failedFuture(exception);
            }
        }

        private void downstreamClosed(CloseStatus status) {
            if (!terminal.compareAndSet(false, true)) {
                return;
            }
            WebSocket target = upstream.get();
            if (target != null && !target.isOutputClosed()) {
                try {
                    target.sendClose(status.getCode(), safeCloseReason(status.getReason()))
                            .orTimeout(plan.sendTimeout().toMillis(), TimeUnit.MILLISECONDS)
                            .exceptionally(exception -> {
                                target.abort();
                                return null;
                            });
                } catch (RuntimeException exception) {
                    target.abort();
                }
            }
            boolean normalClose = status.getCode() == CloseStatus.NORMAL.getCode()
                    || status.getCode() == CloseStatus.GOING_AWAY.getCode();
            plan.complete(
                    normalClose,
                    false,
                    normalClose
                            ? ReverseProxyMetrics.TerminalOutcome.SUCCESS
                            : ReverseProxyMetrics.TerminalOutcome.DOWNSTREAM_DISCONNECT);
        }

        private boolean exceedsInboundLimit(int fragmentBytes, int limit, boolean text, boolean last) {
            int current = text ? inboundTextBytes : inboundBinaryBytes;
            boolean exceeded = fragmentBytes < 0 || current > limit - fragmentBytes;
            int next = exceeded ? limit : current + fragmentBytes;
            if (text) {
                inboundTextBytes = last ? 0 : next;
            } else {
                inboundBinaryBytes = last ? 0 : next;
            }
            return exceeded;
        }

        private void tooLarge(WebSocket webSocket) {
            if (!terminal.compareAndSet(false, true)) {
                return;
            }
            bridges.remove(downstream.getId(), this);
            try {
                webSocket.sendClose(
                        CloseStatus.TOO_BIG_TO_PROCESS.getCode(),
                        "proxy_message_too_large");
            } catch (RuntimeException exception) {
                // Abort below closes the transport even when the close frame cannot be queued.
            }
            webSocket.abort();
            closeQuietly(
                    downstream,
                    CloseStatus.TOO_BIG_TO_PROCESS.withReason("proxy_message_too_large"));
            plan.complete(
                    false,
                    true,
                    ReverseProxyMetrics.TerminalOutcome.UPSTREAM_TRANSPORT_FAILURE);
        }

        private void serviceStopping() {
            if (!terminal.compareAndSet(false, true)) {
                return;
            }
            WebSocket target = upstream.get();
            if (target != null) {
                target.abort();
            }
            closeQuietly(downstream, CloseStatus.SERVICE_RESTARTED);
            plan.complete(
                    false,
                    false,
                    ReverseProxyMetrics.TerminalOutcome.INTERRUPTED);
        }

        private void fail(String reason, Throwable exception) {
            if (!terminal.compareAndSet(false, true)) {
                return;
            }
            bridges.remove(downstream.getId(), this);
            WebSocket target = upstream.get();
            if (target != null) {
                target.abort();
            }
            logger.warn("proxy.websocket.failure route={} upstreamId={} reason={} exceptionType={}",
                    plan.routeName(), plan.upstreamId(), reason, exception.getClass().getSimpleName());
            closeQuietly(downstream, CloseStatus.SERVER_ERROR);
            boolean upstreamFailure = reason.startsWith("upstream_");
            plan.complete(
                    false,
                    upstreamFailure,
                    upstreamFailure
                            ? ReverseProxyMetrics.TerminalOutcome.UPSTREAM_TRANSPORT_FAILURE
                            : ReverseProxyMetrics.TerminalOutcome.DOWNSTREAM_DISCONNECT);
        }
    }

    private static CloseStatus closeStatus(int statusCode, String reason) {
        if (statusCode < 1000 || statusCode > 4999) {
            return CloseStatus.SERVER_ERROR;
        }
        return new CloseStatus(statusCode, safeCloseReason(reason));
    }

    private static boolean upstreamFailureClose(int statusCode) {
        return statusCode == CloseStatus.NO_CLOSE_FRAME.getCode()
                || (statusCode >= CloseStatus.SERVER_ERROR.getCode() && statusCode <= 1014);
    }

    private static String safeCloseReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        StringBuilder safe = new StringBuilder();
        int utf8Bytes = 0;
        for (int offset = 0; offset < reason.length() && safe.length() < MAX_CLOSE_REASON_CHARS;) {
            int codePoint = reason.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (codePoint < 0x20 || codePoint == 0x7f) {
                continue;
            }
            String encoded = new String(Character.toChars(codePoint));
            int encodedBytes = encoded.getBytes(StandardCharsets.UTF_8).length;
            if (utf8Bytes + encodedBytes > MAX_CLOSE_REASON_CHARS) {
                break;
            }
            safe.append(encoded);
            utf8Bytes += encodedBytes;
        }
        return safe.toString();
    }

    private static int durationMillisAsInt(Duration duration) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, duration.toMillis()));
    }

    private static void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException | RuntimeException exception) {
            // Connection teardown is best-effort and must not escape a container callback.
        }
    }
}
