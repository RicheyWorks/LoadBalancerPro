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
}
