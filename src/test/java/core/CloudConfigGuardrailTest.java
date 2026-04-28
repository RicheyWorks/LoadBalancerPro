package core;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudConfigGuardrailTest {
    private static final String ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    @Test
    void guardrailDefaultsAreFailClosed() {
        CloudConfig config = new CloudConfig(ACCESS_KEY, SECRET_KEY, "us-east-1", "lt-test", "subnet-test");

        assertEquals(CloudConfig.DEFAULT_MAX_DESIRED_CAPACITY, config.getMaxDesiredCapacity());
        assertEquals(CloudConfig.DEFAULT_MAX_SCALE_STEP, config.getMaxScaleStep());
        assertFalse(config.isLiveMutationAllowed());
        assertEquals(CloudConfig.DEFAULT_OPERATOR_INTENT, config.getOperatorIntent());
        assertFalse(config.isAutonomousScaleUpAllowed());
    }

    @Test
    void validGuardrailPropertiesAreParsed() {
        Properties props = new Properties();
        props.setProperty(CloudConfig.MAX_DESIRED_CAPACITY_PROPERTY, "8");
        props.setProperty(CloudConfig.MAX_SCALE_STEP_PROPERTY, "2");
        props.setProperty(CloudConfig.ALLOW_LIVE_MUTATION_PROPERTY, "true");
        props.setProperty(CloudConfig.OPERATOR_INTENT_PROPERTY, "LOADBALANCERPRO_LIVE_MUTATION");
        props.setProperty(CloudConfig.ALLOW_AUTONOMOUS_SCALE_UP_PROPERTY, "true");

        CloudConfig config = new CloudConfig(ACCESS_KEY, SECRET_KEY, "us-east-1", "lt-test", "subnet-test", props);

        assertEquals(8, config.getMaxDesiredCapacity());
        assertEquals(2, config.getMaxScaleStep());
        assertTrue(config.isLiveMutationAllowed());
        assertEquals("LOADBALANCERPRO_LIVE_MUTATION", config.getOperatorIntent());
        assertTrue(config.isAutonomousScaleUpAllowed());
    }

    @Test
    void invalidAndBlankGuardrailValuesFailSafely() {
        Properties props = new Properties();
        props.setProperty(CloudConfig.MAX_DESIRED_CAPACITY_PROPERTY, "-1");
        props.setProperty(CloudConfig.MAX_SCALE_STEP_PROPERTY, "not-a-number");
        props.setProperty(CloudConfig.ALLOW_LIVE_MUTATION_PROPERTY, "not-true");
        props.setProperty(CloudConfig.OPERATOR_INTENT_PROPERTY, "   ");
        props.setProperty(CloudConfig.ALLOW_AUTONOMOUS_SCALE_UP_PROPERTY, "not-true");

        CloudConfig config = new CloudConfig(ACCESS_KEY, SECRET_KEY, "us-east-1", "lt-test", "subnet-test", props);

        assertEquals(CloudConfig.DEFAULT_MAX_DESIRED_CAPACITY, config.getMaxDesiredCapacity());
        assertEquals(CloudConfig.DEFAULT_MAX_SCALE_STEP, config.getMaxScaleStep());
        assertFalse(config.isLiveMutationAllowed());
        assertEquals(CloudConfig.DEFAULT_OPERATOR_INTENT, config.getOperatorIntent());
        assertFalse(config.isAutonomousScaleUpAllowed());
    }
}
