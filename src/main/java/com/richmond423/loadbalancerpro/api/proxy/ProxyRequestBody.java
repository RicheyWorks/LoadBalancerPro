package com.richmond423.loadbalancerpro.api.proxy;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.servlet.http.HttpServletRequest;

final class ProxyRequestBody {
    private final HttpServletRequest servletRequest;
    private final byte[] repeatableBytes;
    private final long declaredLength;
    private final boolean bodyPresent;
    private final AtomicBoolean opened = new AtomicBoolean();
    private final AtomicBoolean limitExceeded = new AtomicBoolean();
    private final AtomicLong consumedBytes = new AtomicLong();

    private ProxyRequestBody(HttpServletRequest servletRequest,
                             byte[] repeatableBytes,
                             long declaredLength,
                             boolean bodyPresent) {
        this.servletRequest = servletRequest;
        this.repeatableBytes = repeatableBytes;
        this.declaredLength = declaredLength;
        this.bodyPresent = bodyPresent;
        if (repeatableBytes != null) {
            consumedBytes.set(repeatableBytes.length);
        }
    }

    static ProxyRequestBody streaming(HttpServletRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        long declaredLength = request.getContentLengthLong();
        boolean bodyPresent = declaredLength > 0
                || request.getHeader("Transfer-Encoding") != null;
        return new ProxyRequestBody(request, null, declaredLength, bodyPresent);
    }

    static ProxyRequestBody repeatable(byte[] bytes) {
        byte[] safeBytes = bytes == null ? new byte[0] : bytes;
        return new ProxyRequestBody(null, safeBytes, safeBytes.length, safeBytes.length > 0);
    }

    long declaredLength() {
        return declaredLength;
    }

    boolean repeatable() {
        return servletRequest == null || !bodyPresent;
    }

    boolean limitExceeded() {
        return limitExceeded.get();
    }

    long consumedBytes() {
        return consumedBytes.get();
    }

    HttpRequest.BodyPublisher publisher(long maxRequestBytes) throws IOException {
        if (!bodyPresent) {
            return HttpRequest.BodyPublishers.noBody();
        }
        if (repeatableBytes != null) {
            return HttpRequest.BodyPublishers.ofByteArray(repeatableBytes);
        }
        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.ofInputStream(() -> {
            if (!opened.compareAndSet(false, true)) {
                throw new IllegalStateException("Streaming proxy request body cannot be subscribed twice.");
            }
            try {
                return new BoundedInputStream(
                        servletRequest.getInputStream(), maxRequestBytes, limitExceeded, consumedBytes);
            } catch (IOException exception) {
                throw new UncheckedIOException("Proxy request body could not be opened.", exception);
            }
        });
        return declaredLength > 0
                ? new FixedLengthBodyPublisher(publisher, declaredLength)
                : publisher;
    }

    private static final class FixedLengthBodyPublisher implements HttpRequest.BodyPublisher {
        private final HttpRequest.BodyPublisher delegate;
        private final long contentLength;

        private FixedLengthBodyPublisher(HttpRequest.BodyPublisher delegate, long contentLength) {
            this.delegate = delegate;
            this.contentLength = contentLength;
        }

        @Override
        public long contentLength() {
            return contentLength;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            delegate.subscribe(subscriber);
        }
    }

    static final class BoundedInputStream extends FilterInputStream {
        private final long maxBytes;
        private final AtomicBoolean limitExceeded;
        private final AtomicLong consumedBytes;
        private long bytesRead;

        BoundedInputStream(InputStream input, long maxBytes, AtomicBoolean limitExceeded) {
            this(input, maxBytes, limitExceeded, new AtomicLong());
        }

        BoundedInputStream(
                InputStream input, long maxBytes, AtomicBoolean limitExceeded, AtomicLong consumedBytes) {
            super(Objects.requireNonNull(input, "input cannot be null"));
            if (maxBytes <= 0) {
                throw new IllegalArgumentException("maxBytes must be greater than 0");
            }
            this.maxBytes = maxBytes;
            this.limitExceeded = Objects.requireNonNull(limitExceeded, "limitExceeded cannot be null");
            this.consumedBytes = Objects.requireNonNull(consumedBytes, "consumedBytes cannot be null");
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value == -1) {
                return -1;
            }
            consumedBytes.incrementAndGet();
            if (bytesRead >= maxBytes) {
                failLimit();
            }
            bytesRead++;
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            if (length == 0) {
                return 0;
            }
            long remaining = maxBytes - bytesRead;
            if (remaining < 0) {
                failLimit();
            }
            int boundedLength = remaining >= Integer.MAX_VALUE
                    ? length
                    : (int) Math.min(length, remaining + 1);
            int count = super.read(bytes, offset, boundedLength);
            if (count == -1) {
                return -1;
            }
            consumedBytes.addAndGet(count);
            if (count > remaining) {
                failLimit();
            }
            bytesRead += count;
            return count;
        }

        @Override
        public long skip(long count) throws IOException {
            if (count <= 0) {
                return 0;
            }
            long remaining = maxBytes - bytesRead;
            if (remaining < 0) {
                failLimit();
            }
            long boundedCount = remaining == Long.MAX_VALUE
                    ? count
                    : Math.min(count, remaining + 1);
            long skipped = super.skip(boundedCount);
            consumedBytes.addAndGet(skipped);
            if (skipped > remaining) {
                failLimit();
            }
            bytesRead += skipped;
            return skipped;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public synchronized void mark(int readLimit) {
            // Deliberately unsupported so callers cannot reset and bypass the cumulative cap.
        }

        @Override
        public synchronized void reset() throws IOException {
            throw new IOException("mark/reset is not supported for bounded proxy request bodies.");
        }

        private void failLimit() throws RequestBodyLimitExceededException {
            limitExceeded.set(true);
            throw new RequestBodyLimitExceededException(maxBytes);
        }
    }

    static final class RequestBodyLimitExceededException extends IOException {
        private RequestBodyLimitExceededException(long maxBytes) {
            super("Proxy request body exceeds maximum size of " + maxBytes + " bytes.");
        }
    }
}
