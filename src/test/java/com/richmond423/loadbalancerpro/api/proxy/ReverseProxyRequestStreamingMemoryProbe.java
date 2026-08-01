package com.richmond423.loadbalancerpro.api.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ReverseProxyRequestStreamingMemoryProbe {
    static final String SUCCESS_MARKER = "P-2.1 bounded request streaming probe passed";

    private ReverseProxyRequestStreamingMemoryProbe() {
    }

    public static void main(String[] args) throws Exception {
        long virtualRequestBytes = 1_073_741_824L;
        long maxRequestBytes = 65_536L;
        AtomicBoolean exceeded = new AtomicBoolean();
        InputStream generated = new GeneratedInputStream(virtualRequestBytes);
        try (InputStream bounded = new ProxyRequestBody.BoundedInputStream(
                generated, maxRequestBytes, exceeded)) {
            byte[] buffer = new byte[8_192];
            while (bounded.read(buffer) != -1) {
                // Consume the virtual body without retaining it.
            }
            throw new AssertionError("virtual oversized body was not rejected");
        } catch (ProxyRequestBody.RequestBodyLimitExceededException expected) {
            if (!exceeded.get()) {
                throw new AssertionError("bounded stream did not record the limit breach");
            }
        }
        System.out.println(SUCCESS_MARKER);
    }

    private static final class GeneratedInputStream extends InputStream {
        private long remaining;

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
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (remaining == 0) {
                return -1;
            }
            int count = (int) Math.min(length, remaining);
            remaining -= count;
            return count;
        }
    }
}
