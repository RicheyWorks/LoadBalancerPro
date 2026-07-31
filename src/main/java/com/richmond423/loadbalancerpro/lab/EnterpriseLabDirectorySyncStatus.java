package com.richmond423.loadbalancerpro.lab;

/**
 * Evidence about the parent-directory synchronization associated with a
 * durable storage mutation.
 */
public enum EnterpriseLabDirectorySyncStatus {
    /** No directory entry was created, renamed, or deleted by the operation. */
    NOT_REQUIRED_EXISTING_ENTRY,

    /** Every changed parent directory accepted a metadata force operation. */
    SYNCHRONIZED,

    /**
     * The local Java filesystem provider does not expose directory forcing.
     * File data may still have been forced, but directory-entry crash
     * persistence is not claimed.
     */
    UNSUPPORTED_ON_LOCAL_FILESYSTEM;

    static EnterpriseLabDirectorySyncStatus combine(
            EnterpriseLabDirectorySyncStatus first,
            EnterpriseLabDirectorySyncStatus second) {
        if (first == UNSUPPORTED_ON_LOCAL_FILESYSTEM
                || second == UNSUPPORTED_ON_LOCAL_FILESYSTEM) {
            return UNSUPPORTED_ON_LOCAL_FILESYSTEM;
        }
        if (first == SYNCHRONIZED || second == SYNCHRONIZED) {
            return SYNCHRONIZED;
        }
        return NOT_REQUIRED_EXISTING_ENTRY;
    }
}
