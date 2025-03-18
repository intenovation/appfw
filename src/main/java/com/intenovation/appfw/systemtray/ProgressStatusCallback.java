package com.intenovation.appfw.systemtray;

/**
 * Combined callback interface for reporting both progress and status
 * in a single method call.
 */
public interface ProgressStatusCallback {
    /**
     * Update both progress percentage and status message
     *
     * @param percent Progress percentage (0-100)
     * @param message Status message
     */
    void update(int percent, String message);
}