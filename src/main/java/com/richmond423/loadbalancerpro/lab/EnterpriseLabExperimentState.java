package com.richmond423.loadbalancerpro.lab;

public enum EnterpriseLabExperimentState {
    IDLE(false),
    ARMED(false),
    RUNNING(false),
    HOLDING(false),
    COMPLETING(false),
    ROLLING_BACK(false),
    ROLLED_BACK(true),
    COMPLETED(true),
    REJECTED(true),
    FAILED(true),
    CANCELLED(true);

    private final boolean terminal;

    EnterpriseLabExperimentState(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean terminal() {
        return terminal;
    }
}
