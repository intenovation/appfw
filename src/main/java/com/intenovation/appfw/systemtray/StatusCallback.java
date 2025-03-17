package com.intenovation.appfw.systemtray;

/**
 * Callback interface for status updates
 */
public interface StatusCallback {
    /**
     * Update status message
     *
     * @param message Status message
     */
    void updateStatus(String message);
}
