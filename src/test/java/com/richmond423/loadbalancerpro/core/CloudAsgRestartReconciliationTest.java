package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.CreateAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.TagDescription;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Reservation;

class CloudAsgRestartReconciliationTest {
    private static final String ACCESS_KEY = "UNIT_TEST_ACCESS_KEY_ID";
    private static final String SECRET_KEY = "UNIT_TEST_SECRET_ACCESS_KEY";
    private static final String ACCOUNT_ID = "123456789012";
    private static final String INSTANCE_ID = "i-prior-owned";

    @Test
    void independentlyConstructedConfigsResolveTheSameStableAsgIdentity() {
        CloudConfig first = config(liveGuardrailProperties());
        CloudConfig restarted = config(liveGuardrailProperties());

        assertEquals(first.getAutoScalingGroupName(), restarted.getAutoScalingGroupName());
        assertEquals("lbp-sandbox-LoadBalancerPro-ASG-sandbox", first.getAutoScalingGroupName());
    }

    @Test
    void restartAdoptsThePriorOwnedGroupInsteadOfCreatingADuplicate() throws InterruptedException {
        CloudConfig priorConfig = config(liveGuardrailProperties());
        CloudConfig restartedConfig = config(liveGuardrailProperties());
        AutoScalingClient autoScaling = mock(AutoScalingClient.class);
        Ec2Client ec2 = mock(Ec2Client.class);
        AutoScalingGroup priorGroup = ownedGroup(priorConfig.getAutoScalingGroupName(), INSTANCE_ID);
        when(autoScaling.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(response(priorGroup));
        when(ec2.describeInstances()).thenReturn(DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(software.amazon.awssdk.services.ec2.model.Instance.builder()
                                .instanceId(INSTANCE_ID)
                                .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
                                .build())
                        .build())
                .build());
        LoadBalancer balancer = new LoadBalancer();
        CloudManager restarted =
                new CloudManager(balancer, restartedConfig, ec2, null, autoScaling, null);

        restarted.initializeCloudServers(1, 2);
        restarted.shutdown();

        assertEquals(priorConfig.getAutoScalingGroupName(), restartedConfig.getAutoScalingGroupName());
        assertFalse(balancer.getServerMap().isEmpty(), "The prior owned instance should be adopted after restart.");
        verify(autoScaling).describeAutoScalingGroups(argThat((DescribeAutoScalingGroupsRequest request) ->
                request.autoScalingGroupNames() == null || request.autoScalingGroupNames().isEmpty()));
        verify(autoScaling, never()).createAutoScalingGroup(any(CreateAutoScalingGroupRequest.class));
    }

    @Test
    void emptyInventoryCreatesOneStableOwnedGroup() throws InterruptedException {
        CloudConfig config = config(liveGuardrailProperties());
        AutoScalingClient autoScaling = mock(AutoScalingClient.class);
        Ec2Client ec2 = mock(Ec2Client.class);
        AutoScalingGroup createdGroup = ownedGroup(config.getAutoScalingGroupName(), INSTANCE_ID);
        when(autoScaling.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(response(), response(createdGroup), response(createdGroup));
        when(ec2.describeInstances()).thenReturn(DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(software.amazon.awssdk.services.ec2.model.Instance.builder()
                                .instanceId(INSTANCE_ID)
                                .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
                                .build())
                        .build())
                .build());
        CloudManager manager = new CloudManager(new LoadBalancer(), config, ec2, null, autoScaling, null);

        manager.initializeCloudServers(1, 2);
        manager.shutdown();

        verify(autoScaling).createAutoScalingGroup(argThat((CreateAutoScalingGroupRequest request) ->
                config.getAutoScalingGroupName().equals(request.autoScalingGroupName())
                        && request.tags().stream().anyMatch(tag ->
                                "LoadBalancerPro".equals(tag.key())
                                        && config.getAutoScalingGroupName().equals(tag.value()))));
    }

    @Test
    void restartFindsTheOwnedGroupOnALaterInventoryPage() throws InterruptedException {
        CloudConfig config = config(liveGuardrailProperties());
        AutoScalingClient autoScaling = mock(AutoScalingClient.class);
        Ec2Client ec2 = mock(Ec2Client.class);
        AutoScalingGroup unrelated = group("unrelated-asg", "unrelated-asg", "i-unrelated");
        AutoScalingGroup priorGroup = ownedGroup(config.getAutoScalingGroupName(), INSTANCE_ID);
        when(autoScaling.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenAnswer(invocation -> {
                    DescribeAutoScalingGroupsRequest request = invocation.getArgument(0);
                    if (request.autoScalingGroupNames() != null
                            && !request.autoScalingGroupNames().isEmpty()) {
                        return response(priorGroup);
                    }
                    if ("owned-page".equals(request.nextToken())) {
                        return response(priorGroup);
                    }
                    return DescribeAutoScalingGroupsResponse.builder()
                            .autoScalingGroups(unrelated)
                            .nextToken("owned-page")
                            .build();
                });
        when(ec2.describeInstances()).thenReturn(DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(software.amazon.awssdk.services.ec2.model.Instance.builder()
                                .instanceId(INSTANCE_ID)
                                .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
                                .build())
                        .build())
                .build());
        CloudManager manager = new CloudManager(new LoadBalancer(), config, ec2, null, autoScaling, null);

        manager.initializeCloudServers(1, 2);
        manager.shutdown();

        verify(autoScaling).describeAutoScalingGroups(argThat((DescribeAutoScalingGroupsRequest request) ->
                "owned-page".equals(request.nextToken())));
        verify(autoScaling, never()).createAutoScalingGroup(any(CreateAutoScalingGroupRequest.class));
    }

    @Test
    void configuredNameWithMismatchedOwnershipTagFailsClosed() throws InterruptedException {
        CloudConfig config = config(liveGuardrailProperties());
        AutoScalingClient autoScaling = mock(AutoScalingClient.class);
        Ec2Client ec2 = mock(Ec2Client.class);
        AutoScalingGroup mismatched =
                group(config.getAutoScalingGroupName(), "another-owner", "i-mismatched-owner");
        when(autoScaling.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(response(mismatched));
        CloudManager manager = new CloudManager(new LoadBalancer(), config, ec2, null, autoScaling, null);

        manager.initializeCloudServers(1, 2);
        manager.shutdown();

        verify(autoScaling, never()).createAutoScalingGroup(any(CreateAutoScalingGroupRequest.class));
        verify(autoScaling, never()).updateAutoScalingGroup(any(UpdateAutoScalingGroupRequest.class));
        verify(autoScaling, never()).deleteAutoScalingGroup(any(DeleteAutoScalingGroupRequest.class));
        verify(ec2, never()).describeInstances();
    }

    @Test
    void ambiguousOwnedGroupInventoryFailsClosed() throws InterruptedException {
        CloudConfig config = config(liveGuardrailProperties());
        AutoScalingClient autoScaling = mock(AutoScalingClient.class);
        AutoScalingGroup first = ownedGroup(config.getAutoScalingGroupName(), INSTANCE_ID);
        AutoScalingGroup duplicate = ownedGroup(config.getAutoScalingGroupName(), "i-duplicate");
        when(autoScaling.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(response(first, duplicate));
        CloudManager manager = new CloudManager(new LoadBalancer(), config, null, null, autoScaling, null);

        manager.initializeCloudServers(1, 2);
        manager.shutdown();

        verify(autoScaling, never()).createAutoScalingGroup(any(CreateAutoScalingGroupRequest.class));
        verify(autoScaling, never()).updateAutoScalingGroup(any(UpdateAutoScalingGroupRequest.class));
        verify(autoScaling, never()).deleteAutoScalingGroup(any(DeleteAutoScalingGroupRequest.class));
    }

    @Test
    void ownershipTagPointingAtTheStableIdentityFromAnotherNameFailsClosed() throws InterruptedException {
        CloudConfig config = config(liveGuardrailProperties());
        AutoScalingClient autoScaling = mock(AutoScalingClient.class);
        AutoScalingGroup collision =
                group("different-asg-name", config.getAutoScalingGroupName(), "i-colliding-owner");
        when(autoScaling.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(response(collision));
        CloudManager manager = new CloudManager(new LoadBalancer(), config, null, null, autoScaling, null);

        manager.initializeCloudServers(1, 2);
        manager.shutdown();

        verify(autoScaling, never()).createAutoScalingGroup(any(CreateAutoScalingGroupRequest.class));
        verify(autoScaling, never()).updateAutoScalingGroup(any(UpdateAutoScalingGroupRequest.class));
        verify(autoScaling, never()).deleteAutoScalingGroup(any(DeleteAutoScalingGroupRequest.class));
    }

    @Test
    void unavailableInventoryFailsClosedBeforeAnyMutation() throws InterruptedException {
        CloudConfig config = config(liveGuardrailProperties());
        AutoScalingClient autoScaling = mock(AutoScalingClient.class);
        when(autoScaling.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenThrow(SdkServiceException.builder().message("inventory unavailable").build());
        CloudManager manager = new CloudManager(new LoadBalancer(), config, null, null, autoScaling, null);

        manager.initializeCloudServers(1, 2);
        manager.shutdown();

        verify(autoScaling, never()).createAutoScalingGroup(any(CreateAutoScalingGroupRequest.class));
        verify(autoScaling, never()).updateAutoScalingGroup(any(UpdateAutoScalingGroupRequest.class));
        verify(autoScaling, never()).deleteAutoScalingGroup(any(DeleteAutoScalingGroupRequest.class));
    }

    @Test
    void dryRunRestartIdentityIsStableWithoutAwsCallsOrMutations() throws InterruptedException {
        Properties props = new Properties();
        props.setProperty(CloudConfig.ENVIRONMENT_PROPERTY, "local");
        CloudConfig first = config(props);
        CloudConfig restartedConfig = config(props);
        AutoScalingClient autoScaling = mock(AutoScalingClient.class);
        CloudManager restarted =
                new CloudManager(new LoadBalancer(), restartedConfig, null, null, autoScaling, null);

        restarted.initializeCloudServers(1, 2);
        restarted.shutdown();

        assertEquals(first.getAutoScalingGroupName(), restartedConfig.getAutoScalingGroupName());
        verify(autoScaling, never()).describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class));
        verify(autoScaling, never()).createAutoScalingGroup(any(CreateAutoScalingGroupRequest.class));
        verify(autoScaling, never()).updateAutoScalingGroup(any(UpdateAutoScalingGroupRequest.class));
        verify(autoScaling, never()).deleteAutoScalingGroup(any(DeleteAutoScalingGroupRequest.class));
    }

    private static CloudConfig config(Properties props) {
        return new CloudConfig(
                ACCESS_KEY,
                SECRET_KEY,
                "us-east-1",
                "lt-test",
                "subnet-test",
                props);
    }

    private static Properties liveGuardrailProperties() {
        Properties props = new Properties();
        props.setProperty(CloudConfig.LIVE_MODE_PROPERTY, "true");
        props.setProperty(CloudConfig.ALLOW_LIVE_MUTATION_PROPERTY, "true");
        props.setProperty(CloudConfig.OPERATOR_INTENT_PROPERTY, "LOADBALANCERPRO_LIVE_MUTATION");
        props.setProperty(CloudConfig.MAX_DESIRED_CAPACITY_PROPERTY, "10");
        props.setProperty(CloudConfig.MAX_SCALE_STEP_PROPERTY, "10");
        props.setProperty(CloudConfig.ENVIRONMENT_PROPERTY, "sandbox");
        props.setProperty(CloudConfig.RESOURCE_NAME_PREFIX_PROPERTY, "lbp-sandbox-");
        props.setProperty(CloudConfig.ALLOWED_AWS_ACCOUNT_IDS_PROPERTY, ACCOUNT_ID);
        props.setProperty(CloudConfig.CURRENT_AWS_ACCOUNT_ID_PROPERTY, ACCOUNT_ID);
        props.setProperty(CloudConfig.ALLOWED_REGIONS_PROPERTY, "us-east-1");
        props.setProperty("retryAttempts", "1");
        return props;
    }

    private static AutoScalingGroup ownedGroup(String name, String instanceId) {
        return group(name, name, instanceId);
    }

    private static AutoScalingGroup group(String name, String ownerValue, String instanceId) {
        return AutoScalingGroup.builder()
                .autoScalingGroupName(name)
                .desiredCapacity(1)
                .minSize(1)
                .maxSize(2)
                .tags(TagDescription.builder().key("LoadBalancerPro").value(ownerValue).build())
                .instances(software.amazon.awssdk.services.autoscaling.model.Instance.builder()
                        .instanceId(instanceId)
                        .lifecycleState("InService")
                        .healthStatus("Healthy")
                        .build())
                .build();
    }

    private static DescribeAutoScalingGroupsResponse response(AutoScalingGroup... groups) {
        return DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(groups).build();
    }
}
