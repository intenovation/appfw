package com.intenovation.appfw.systemtray;

/**
 * Callback interface for progress updates
 */
public interface ProgressCallback {
    /**
     * Update progress percentage
     *
     * @param percent Progress percentage (0-100)
     */
    void updateProgress(int percent);
}
