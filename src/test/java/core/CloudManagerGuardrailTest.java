package core;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CloudManagerGuardrailTest {
    private static final String ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    @Test
    void dryRunWithInjectedClientsDoesNotMutateAws() throws Exception {
        AmazonEC2 ec2 = mock(AmazonEC2.class);
        AmazonCloudWatch cloudWatch = mock(AmazonCloudWatch.class);
        AmazonAutoScaling autoScaling = mock(AmazonAutoScaling.class);
        CloudConfig config = new CloudConfig(ACCESS_KEY, SECRET_KEY, "us-east-1", "lt-test", "subnet-test");
        CloudManager manager = new CloudManager(new LoadBalancer(), config, ec2, cloudWatch, autoScaling, null);

        manager.initializeCloudServers(1, 2);
        manager.scaleServers(2);
        manager.updateServerMetricsFromCloud();
        manager.startBackgroundJobs();
        manager.shutdown();

        verify(autoScaling, never()).createAutoScalingGroup(any());
        verify(autoScaling, never()).updateAutoScalingGroup(any(UpdateAutoScalingGroupRequest.class));
        verify(autoScaling, never()).deleteAutoScalingGroup(any(DeleteAutoScalingGroupRequest.class));
        verify(cloudWatch, never()).getMetricStatistics(any(GetMetricStatisticsRequest.class));
        verify(ec2, never()).createTags(any(CreateTagsRequest.class));
    }

    @Test
    void dryRunScalingCallbackDoesNotRequireAwsMutation() {
        AmazonAutoScaling autoScaling = mock(AmazonAutoScaling.class);
        CloudConfig config = new CloudConfig(ACCESS_KEY, SECRET_KEY, "us-east-1", "lt-test", "subnet-test");
        CloudManager manager = new CloudManager(new LoadBalancer(), config, null, null, autoScaling, null);
        AtomicBoolean callbackResult = new AtomicBoolean(false);

        manager.scaleServersAsync(3, callbackResult::set);
        manager.shutdown();

        assertTrue(callbackResult.get(), "Dry-run scaling should complete locally.");
        verify(autoScaling, never()).updateAutoScalingGroup(any(UpdateAutoScalingGroupRequest.class));
    }

    @Test
    void deletionRequiresLiveModeOwnershipAndDeletionApproval() {
        assertDeletionSkipped(configWithDeletionFlags(false, true, true));
        assertDeletionSkipped(configWithDeletionFlags(true, false, true));
        assertDeletionSkipped(configWithDeletionFlags(true, true, false));
    }

    @Test
    void deletionRunsOnlyWhenAllDeletionGatesAreExplicitlyEnabled() {
        AmazonAutoScaling autoScaling = mock(AmazonAutoScaling.class);
        CloudManager manager = new CloudManager(
                new LoadBalancer(),
                configWithDeletionFlags(true, true, true),
                null,
                null,
                autoScaling,
                null);

        manager.shutdown();

        verify(autoScaling).deleteAutoScalingGroup(any(DeleteAutoScalingGroupRequest.class));
    }

    private static void assertDeletionSkipped(CloudConfig config) {
        AmazonAutoScaling autoScaling = mock(AmazonAutoScaling.class);
        CloudManager manager = new CloudManager(new LoadBalancer(), config, null, null, autoScaling, null);

        manager.shutdown();

        verify(autoScaling, never()).deleteAutoScalingGroup(any(DeleteAutoScalingGroupRequest.class));
    }

    private static CloudConfig configWithDeletionFlags(boolean liveMode, boolean ownershipConfirmed,
                                                      boolean deletionAllowed) {
        Properties props = new Properties();
        props.setProperty(CloudConfig.LIVE_MODE_PROPERTY, Boolean.toString(liveMode));
        props.setProperty(CloudConfig.CONFIRM_RESOURCE_OWNERSHIP_PROPERTY, Boolean.toString(ownershipConfirmed));
        props.setProperty(CloudConfig.ALLOW_RESOURCE_DELETION_PROPERTY, Boolean.toString(deletionAllowed));
        props.setProperty("retryAttempts", "1");
        return new CloudConfig(ACCESS_KEY, SECRET_KEY, "us-east-1", "lt-test", "subnet-test", props);
    }
}
