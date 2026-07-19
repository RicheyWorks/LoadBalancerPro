package com.richmond423.loadbalancerpro.lab;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabAllocationProofRunnerTest {
    @Test
    void packagedProofExercisesCrashWindowsExternalStateDriftAndFencing() throws Exception {
        Path output = Path.of("target", "allocation-proof-runner-test-" + UUID.randomUUID());
        EnterpriseLabAllocationProofReport report =
                new EnterpriseLabAllocationProofRunner().run(output);

        assertTrue(report.allPassed(), report.failedChecks().toString());
        assertTrue(report.externalHolderSeparateProcess());
        assertEquals(EnterpriseLabAllocationProofReport.REQUIRED_DRIFT_CASES,
                report.driftClassifications().size());
        assertTrue(report.driftClassifications().values().stream()
                .noneMatch("SAFE_BASELINE_INSTALLED"::equals));
        assertEquals(report.baselineFingerprint(), report.finalInstalledFingerprint());
    }
}
