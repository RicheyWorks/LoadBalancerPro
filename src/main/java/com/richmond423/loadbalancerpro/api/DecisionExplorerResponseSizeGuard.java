package com.richmond423.loadbalancerpro.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.api.config.RoutingApiLimitsProperties;
import java.io.IOException;
import java.io.OutputStream;
import org.springframework.stereotype.Component;

@Component
final class DecisionExplorerResponseSizeGuard {
    private final ObjectMapper objectMapper;
    private final RoutingApiLimitsProperties limits;

    DecisionExplorerResponseSizeGuard(ObjectMapper objectMapper, RoutingApiLimitsProperties limits) {
        this.objectMapper = objectMapper;
        this.limits = limits;
    }

    void requireWithinLimit(Object response) {
        long maxBytes = limits.getMaxDecisionExplorerResponseBytes();
        try {
            objectMapper.writeValue(new BoundedCountingOutputStream(maxBytes), response);
        } catch (IOException exception) {
            if (hasSizeLimitCause(exception)) {
                throw new IllegalArgumentException(
                        "decision explorer response exceeds maximum size of " + maxBytes + " bytes");
            }
            throw new IllegalStateException("Unable to verify decision explorer response size", exception);
        }
    }

    private static boolean hasSizeLimitCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ResponseSizeLimitExceededException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static final class BoundedCountingOutputStream extends OutputStream {
        private final long maxBytes;
        private long count;

        private BoundedCountingOutputStream(long maxBytes) {
            this.maxBytes = maxBytes;
        }

        @Override
        public void write(int value) throws IOException {
            requireCapacity(1);
            count++;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            if (bytes == null) {
                throw new NullPointerException("bytes");
            }
            if (offset < 0 || length < 0 || offset > bytes.length - length) {
                throw new IndexOutOfBoundsException();
            }
            requireCapacity(length);
            count += length;
        }

        private void requireCapacity(int additionalBytes) throws ResponseSizeLimitExceededException {
            if (additionalBytes > maxBytes - count) {
                throw new ResponseSizeLimitExceededException();
            }
        }
    }

    private static final class ResponseSizeLimitExceededException extends IOException {
    }
}
