package cli;

import core.CloudConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadBalancerCLITest {
    private static final String ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    @Test
    void cloudSettingsRejectMissingRequiredValues() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> LoadBalancerCLI.CliRunner.resolveCloudSettings(new Properties(), Map.of()));

        assertTrue(exception.getMessage().contains("aws.accessKeyId/AWS_ACCESS_KEY_ID"));
        assertTrue(exception.getMessage().contains("aws.secretAccessKey/AWS_SECRET_ACCESS_KEY"));
        assertTrue(exception.getMessage().contains("aws.region/AWS_REGION or AWS_DEFAULT_REGION"));
    }

    @Test
    void cloudSettingsDefaultToDryRunWhenLiveModeIsNotExplicit() {
        Properties properties = new Properties();
        properties.setProperty("aws.accessKeyId", ACCESS_KEY);
        properties.setProperty("aws.secretAccessKey", SECRET_KEY);
        properties.setProperty("aws.region", "us-east-1");

        CloudConfig cloudConfig = LoadBalancerCLI.CliRunner
                .resolveCloudSettings(properties, Map.of())
                .toCloudConfig();

        assertFalse(cloudConfig.isLiveMode());
        assertTrue(cloudConfig.isDryRun());
        assertEquals("us-east-1", cloudConfig.getRegion());
    }

    @Test
    void liveCloudSettingsRequireLaunchTemplateAndSubnet() {
        Properties properties = new Properties();
        properties.setProperty("aws.accessKeyId", ACCESS_KEY);
        properties.setProperty("aws.secretAccessKey", SECRET_KEY);
        properties.setProperty("aws.region", "us-east-1");
        properties.setProperty(CloudConfig.LIVE_MODE_PROPERTY, "true");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> LoadBalancerCLI.CliRunner.resolveCloudSettings(properties, Map.of()));

        assertTrue(exception.getMessage().contains("cloud.launchTemplateId/CLOUD_LAUNCH_TEMPLATE_ID"));
        assertTrue(exception.getMessage().contains("cloud.subnetId/CLOUD_SUBNET_ID"));
    }

    @Test
    void cloudSettingsCanBeLoadedFromEnvironmentMap() {
        Map<String, String> environment = Map.of(
                "AWS_ACCESS_KEY_ID", ACCESS_KEY,
                "AWS_SECRET_ACCESS_KEY", SECRET_KEY,
                "AWS_DEFAULT_REGION", "us-west-2",
                "CLOUD_LIVE_MODE", "true",
                "CLOUD_LAUNCH_TEMPLATE_ID", "lt-1234567890abcdef0",
                "CLOUD_SUBNET_ID", "subnet-1234567890abcdef0"
        );

        CloudConfig cloudConfig = LoadBalancerCLI.CliRunner
                .resolveCloudSettings(new Properties(), environment)
                .toCloudConfig();

        assertTrue(cloudConfig.isLiveMode());
        assertEquals("us-west-2", cloudConfig.getRegion());
        assertEquals("lt-1234567890abcdef0", cloudConfig.getLaunchTemplateId());
        assertEquals("subnet-1234567890abcdef0", cloudConfig.getSubnetId());
    }

    @Test
    void cloudGuardrailSettingsCanBeLoadedFromEnvironmentMap() {
        Map<String, String> environment = Map.ofEntries(
                Map.entry("AWS_ACCESS_KEY_ID", ACCESS_KEY),
                Map.entry("AWS_SECRET_ACCESS_KEY", SECRET_KEY),
                Map.entry("AWS_REGION", "us-east-1"),
                Map.entry("CLOUD_MAX_DESIRED_CAPACITY", "8"),
                Map.entry("CLOUD_MAX_SCALE_STEP", "2"),
                Map.entry("CLOUD_ALLOW_LIVE_MUTATION", "true"),
                Map.entry("CLOUD_OPERATOR_INTENT", "LOADBALANCERPRO_LIVE_MUTATION"),
                Map.entry("CLOUD_ALLOW_AUTONOMOUS_SCALE_UP", "true"),
                Map.entry("CLOUD_ENVIRONMENT", "prod"),
                Map.entry("CLOUD_ALLOWED_AWS_ACCOUNT_IDS", "123456789012,210987654321"),
                Map.entry("CLOUD_CURRENT_AWS_ACCOUNT_ID", "123456789012"),
                Map.entry("CLOUD_ALLOWED_REGIONS", "us-east-1,us-west-2")
        );

        CloudConfig cloudConfig = LoadBalancerCLI.CliRunner
                .resolveCloudSettings(new Properties(), environment)
                .toCloudConfig();

        assertFalse(cloudConfig.isLiveMode());
        assertEquals(8, cloudConfig.getMaxDesiredCapacity());
        assertEquals(2, cloudConfig.getMaxScaleStep());
        assertTrue(cloudConfig.isLiveMutationAllowed());
        assertEquals("LOADBALANCERPRO_LIVE_MUTATION", cloudConfig.getOperatorIntent());
        assertTrue(cloudConfig.isAutonomousScaleUpAllowed());
        assertEquals("prod", cloudConfig.getEnvironment());
        assertEquals("123456789012", cloudConfig.getCurrentAwsAccountId());
        assertEquals(java.util.List.of("123456789012", "210987654321"), cloudConfig.getAllowedAwsAccountIds());
        assertEquals(java.util.List.of("us-east-1", "us-west-2"), cloudConfig.getAllowedRegions());
    }

    @Test
    void cloudGuardrailSettingsCanBeLoadedFromSystemProperties() {
        Properties properties = new Properties();
        properties.setProperty("aws.accessKeyId", ACCESS_KEY);
        properties.setProperty("aws.secretAccessKey", SECRET_KEY);
        properties.setProperty("aws.region", "us-west-2");
        properties.setProperty(CloudConfig.MAX_DESIRED_CAPACITY_PROPERTY, "5");
        properties.setProperty(CloudConfig.MAX_SCALE_STEP_PROPERTY, "1");
        properties.setProperty(CloudConfig.ALLOW_LIVE_MUTATION_PROPERTY, "true");
        properties.setProperty(CloudConfig.OPERATOR_INTENT_PROPERTY, "LOADBALANCERPRO_LIVE_MUTATION");
        properties.setProperty(CloudConfig.ALLOW_AUTONOMOUS_SCALE_UP_PROPERTY, "false");
        properties.setProperty(CloudConfig.ENVIRONMENT_PROPERTY, "staging");
        properties.setProperty(CloudConfig.ALLOWED_AWS_ACCOUNT_IDS_PROPERTY, "123456789012");
        properties.setProperty(CloudConfig.CURRENT_AWS_ACCOUNT_ID_PROPERTY, "123456789012");
        properties.setProperty(CloudConfig.ALLOWED_REGIONS_PROPERTY, "us-west-2");

        CloudConfig cloudConfig = LoadBalancerCLI.CliRunner
                .resolveCloudSettings(properties, Map.of())
                .toCloudConfig();

        assertFalse(cloudConfig.isLiveMode());
        assertEquals(5, cloudConfig.getMaxDesiredCapacity());
        assertEquals(1, cloudConfig.getMaxScaleStep());
        assertTrue(cloudConfig.isLiveMutationAllowed());
        assertEquals("LOADBALANCERPRO_LIVE_MUTATION", cloudConfig.getOperatorIntent());
        assertFalse(cloudConfig.isAutonomousScaleUpAllowed());
        assertEquals("staging", cloudConfig.getEnvironment());
        assertEquals(java.util.List.of("123456789012"), cloudConfig.getAllowedAwsAccountIds());
        assertEquals("123456789012", cloudConfig.getCurrentAwsAccountId());
        assertEquals(java.util.List.of("us-west-2"), cloudConfig.getAllowedRegions());
    }

    @Test
    void environmentCredentialsStillDefaultToDryRun() {
        Map<String, String> environment = Map.of(
                "AWS_ACCESS_KEY_ID", ACCESS_KEY,
                "AWS_SECRET_ACCESS_KEY", SECRET_KEY,
                "AWS_REGION", "us-east-2"
        );

        CloudConfig cloudConfig = LoadBalancerCLI.CliRunner
                .resolveCloudSettings(new Properties(), environment)
                .toCloudConfig();

        assertFalse(cloudConfig.isLiveMode());
        assertTrue(cloudConfig.isDryRun());
        assertEquals("us-east-2", cloudConfig.getRegion());
    }

    @Test
    void environmentLiveModeFailsClosedWithoutLaunchTemplateAndSubnet() {
        Map<String, String> environment = Map.of(
                "AWS_ACCESS_KEY_ID", ACCESS_KEY,
                "AWS_SECRET_ACCESS_KEY", SECRET_KEY,
                "AWS_REGION", "us-east-2",
                "CLOUD_LIVE_MODE", "true"
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> LoadBalancerCLI.CliRunner.resolveCloudSettings(new Properties(), environment));

        assertTrue(exception.getMessage().contains("cloud.launchTemplateId/CLOUD_LAUNCH_TEMPLATE_ID"));
        assertTrue(exception.getMessage().contains("cloud.subnetId/CLOUD_SUBNET_ID"));
    }
}
