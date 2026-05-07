package com.richmond423.loadbalancerpro.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LoadBalancerCloudMetricsTest {
    private static final int CURRENT_CLOUD_METRICS_RETRY_LIMIT = 3;

    private LoadBalancer balancer;

    @BeforeEach
    void setUp() {
        balancer = new LoadBalancer();
    }

    @AfterEach
    void tearDown() {
        Thread.interrupted();
        if (balancer != null) {
            balancer.shutdown();
        }
    }

    @Test
    void updateCloudMetricsIfAvailableNoOpsWhenCloudManagerIsMissing() {
        assertDoesNotThrow(() -> balancer.updateCloudMetricsIfAvailable());
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedUpdateMetricsFromCloudNoOpsWhenCloudManagerIsMissing() {
        assertDoesNotThrow(() -> balancer.updateMetricsFromCloud());
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedUpdateMetricsFromCloudDelegatesToCurrentCloudMetricsPath() throws Exception {
        CloudManager cloudManager = attachMockCloudManager();

        balancer.updateMetricsFromCloud();

        verify(cloudManager).updateServerMetricsFromCloud();
    }

    @Test
    void updateCloudMetricsIfAvailableReturnsAfterSuccessfulCloudMetricUpdate() throws Exception {
        CloudManager cloudManager = attachMockCloudManager();

        balancer.updateCloudMetricsIfAvailable();

        verify(cloudManager).updateServerMetricsFromCloud();
    }

    @Test
    void updateCloudMetricsIfAvailableRetriesUntilCloudMetricUpdateSucceeds() throws Exception {
        CloudManager cloudManager = attachMockCloudManager();
        doAnswer(throwIoException("first cloud metric failure"))
                .doAnswer(throwIoException("second cloud metric failure"))
                .doNothing()
                .when(cloudManager).updateServerMetricsFromCloud();

        balancer.updateCloudMetricsIfAvailable();

        verify(cloudManager, times(CURRENT_CLOUD_METRICS_RETRY_LIMIT)).updateServerMetricsFromCloud();
    }

    @Test
    void updateCloudMetricsIfAvailableThrowsIOExceptionAfterRetryExhaustion() throws Exception {
        CloudManager cloudManager = attachMockCloudManager();
        doAnswer(throwIoException("cloud metrics unavailable"))
                .when(cloudManager).updateServerMetricsFromCloud();

        IOException exception = assertThrows(IOException.class, () -> balancer.updateCloudMetricsIfAvailable());

        assertEquals("Cloud metric update failed after retries", exception.getMessage());
        assertInstanceOf(IOException.class, exception.getCause());
        verify(cloudManager, times(CURRENT_CLOUD_METRICS_RETRY_LIMIT)).updateServerMetricsFromCloud();
    }

    @Test
    void updateCloudMetricsIfAvailablePreservesInterruptStatusWhenRetryDelayIsInterrupted() throws Exception {
        CloudManager cloudManager = attachMockCloudManager();
        doAnswer(throwIoException("cloud metric failure before retry delay"))
                .when(cloudManager).updateServerMetricsFromCloud();

        Thread.currentThread().interrupt();
        try {
            IOException exception = assertThrows(IOException.class, () -> balancer.updateCloudMetricsIfAvailable());

            assertEquals("Interrupted during retry delay", exception.getMessage());
            assertInstanceOf(InterruptedException.class, exception.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(cloudManager).updateServerMetricsFromCloud();
        } finally {
            Thread.interrupted();
        }
    }

    private CloudManager attachMockCloudManager() throws ReflectiveOperationException {
        CloudManager cloudManager = mock(CloudManager.class);
        Field cloudManagerField = LoadBalancer.class.getDeclaredField("cloudManager");
        cloudManagerField.setAccessible(true);
        cloudManagerField.set(balancer, cloudManager);
        assertSame(cloudManager, balancer.getCloudManagerOptional().orElseThrow());
        return cloudManager;
    }

    private static Answer<Void> throwIoException(String message) {
        return invocation -> {
            throw new IOException(message);
        };
    }
}
