package com.richmond423.loadbalancerpro.core;

public enum ServerObservationOutcome {
    SUCCESS,
    FAILURE,
    TIMEOUT,
    CONNECTION_FAILURE;

    public boolean successful() {
        return this == SUCCESS;
    }

    public boolean failed() {
        return !successful();
    }
}
