package com.richmond423.loadbalancerpro.api.proxy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;

public final class ReverseProxyResponseStreamingMemoryProbe {
    static final String SUCCESS_MARKER = "P-2.2 two-gigabyte response streaming probe passed";
    private static final long TWO_GIB = 2L * 1024 * 1024 * 1024;

    private ReverseProxyResponseStreamingMemoryProbe() {
    }

    public static void main(String[] args) throws Exception {
        GeneratedInputStream upstreamBody = new GeneratedInputStream(TWO_GIB);
        HttpResponse<InputStream> upstreamResponse = mock(HttpResponse.class);
        when(upstreamResponse.statusCode()).thenReturn(200);
        when(upstreamResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Length", List.of(Long.toString(TWO_GIB))),
                (name, value) -> true));
        when(upstreamResponse.body()).thenReturn(upstreamBody);

        HttpClient client = mock(HttpClient.class);
        when(client.send(
                any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(upstreamResponse);

        CountingServletOutputStream output = new CountingServletOutputStream();
        HttpServletResponse downstream = mock(HttpServletResponse.class);
        when(downstream.getOutputStream()).thenReturn(output);

        ReverseProxyProperties properties = properties();
        ReverseProxyService service = new ReverseProxyService(
                properties,
                client,
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.systemUTC());
        try {
            ReverseProxyResponse response = service.forward(request(), downstream);
            if (response.statusCode() != 200) {
                throw new AssertionError("unexpected status: " + response.statusCode());
            }
            if (output.writtenBytes != TWO_GIB) {
                throw new AssertionError(
                        "streamed byte count was " + output.writtenBytes + ", expected " + TWO_GIB);
            }
            if (!upstreamBody.closed) {
                throw new AssertionError("upstream body was not closed");
            }
        } finally {
            service.closeHealthProber();
        }
        System.out.println(SUCCESS_MARKER);
    }

    private static ReverseProxyProperties properties() {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setMaxResponseBytes(0);
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId("heap-probe");
        upstream.setUrl("http://127.0.0.1:19080");
        upstream.setHealthy(true);
        properties.setUpstreams(List.of(upstream));
        return properties;
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/proxy/heap-probe");
        request.setContextPath("");
        request.setServletPath("");
        request.setRemoteAddr("127.0.0.1");
        request.setRemotePort(39000);
        request.setLocalPort(8080);
        request.setScheme("http");
        request.addHeader("Host", "127.0.0.1:8080");
        return request;
    }

    private static final class GeneratedInputStream extends InputStream {
        private long remaining;
        private boolean closed;

        private GeneratedInputStream(long length) {
            this.remaining = length;
        }

        @Override
        public int read() {
            if (remaining == 0) {
                return -1;
            }
            remaining--;
            return 0;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            if (remaining == 0) {
                return -1;
            }
            int count = (int) Math.min(length, remaining);
            remaining -= count;
            return count;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class CountingServletOutputStream extends ServletOutputStream {
        private long writtenBytes;

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }

        @Override
        public void write(int value) {
            writtenBytes++;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            if (length < 0) {
                throw new IOException("negative fixture length");
            }
            writtenBytes += length;
        }
    }
}
