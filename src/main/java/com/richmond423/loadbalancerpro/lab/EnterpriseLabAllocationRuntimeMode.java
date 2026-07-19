package com.richmond423.loadbalancerpro.lab;

import java.util.Locale;

/** Explicit installed-allocation authority selected at application startup. */
public enum EnterpriseLabAllocationRuntimeMode {
    IN_PROCESS("in-process"),
    EXTERNAL_SUPERVISOR_REQUIRED("external-supervisor-required"),
    DISABLED("disabled");

    private final String wireValue;

    EnterpriseLabAllocationRuntimeMode(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static EnterpriseLabAllocationRuntimeMode parse(String value) {
        if (value == null || value.isEmpty()) {
            return IN_PROCESS;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "allocation supervisor mode must not be whitespace");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals(value)) {
            throw new IllegalArgumentException(
                    "allocation supervisor mode must use an exact lowercase wire value");
        }
        for (EnterpriseLabAllocationRuntimeMode mode : values()) {
            if (mode.wireValue.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException(
                "allocation supervisor mode must be in-process, external-supervisor-required, or disabled");
    }
}
