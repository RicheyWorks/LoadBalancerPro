package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class PressureClassifierTest {

    @Test
    void oneClassifierAppliesAllSharedPressureBoundaries() {
        PressureClassifier.Thresholds thresholds = new PressureClassifier.Thresholds(
                0.60, 0.85, 20, 200.0, 350.0, 0.10);
        PressureClassifier.Assessment assessment = PressureClassifier.assess(
                new PressureClassifier.Observation(0.50, 0.85, 21, 201.0, 351.0, 0.11),
                thresholds);

        assertEquals(Set.of(
                PressureClassifier.Dimension.HEALTH,
                PressureClassifier.Dimension.UTILIZATION,
                PressureClassifier.Dimension.QUEUE,
                PressureClassifier.Dimension.P95_LATENCY,
                PressureClassifier.Dimension.P99_LATENCY,
                PressureClassifier.Dimension.ERROR_RATE), assessment.activeDimensions());
    }

    @Test
    void equalityTriggersUtilizationButNotStrictAboveOrBelowDimensions() {
        PressureClassifier.Thresholds thresholds = new PressureClassifier.Thresholds(
                0.60, 0.85, 20, 200.0, 350.0, 0.10);
        PressureClassifier.Assessment assessment = PressureClassifier.assess(
                new PressureClassifier.Observation(0.60, 0.85, 20, 200.0, 350.0, 0.10),
                thresholds);

        assertTrue(assessment.has(PressureClassifier.Dimension.UTILIZATION));
        assertFalse(assessment.has(PressureClassifier.Dimension.HEALTH));
        assertFalse(assessment.has(PressureClassifier.Dimension.QUEUE));
        assertFalse(assessment.has(PressureClassifier.Dimension.P95_LATENCY));
        assertFalse(assessment.has(PressureClassifier.Dimension.P99_LATENCY));
        assertFalse(assessment.has(PressureClassifier.Dimension.ERROR_RATE));
    }

    @Test
    void disabledDimensionsCannotCreatePressure() {
        PressureClassifier.Assessment assessment = PressureClassifier.assess(
                new PressureClassifier.Observation(0.0, 10.0, 100, 900.0, 900.0, 1.0),
                PressureClassifier.Thresholds.utilizationOnly(11.0));

        assertFalse(assessment.hasPressure());
        assertTrue(assessment.activeDimensions().isEmpty());
    }
}
