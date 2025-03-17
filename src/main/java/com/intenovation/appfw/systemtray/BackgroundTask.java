package com.intenovation.appfw.systemtray;

/**
 * Interface for a background task that can be executed with progress tracking and cancellation
 */
public interface BackgroundTask {
    /**
     * Execute the task with progress reporting
     *
     * @param progressCallback Callback to report progress (0-100)
     * @param statusCallback   Callback to report status message
     * @return Optional status message that can be displayed on completion
     * @throws InterruptedException if the task is cancelled
     */
    String execute(ProgressCallback progressCallback, StatusCallback statusCallback) throws InterruptedException;

    /**
     * Get the interval in seconds to execute this task if scheduled
     *
     * @return Interval in seconds, or 0 for manual-only execution
     */
    int getIntervalSeconds();

    /**
     * Get a user-friendly name for this task
     *
     * @return Task name
     */
    String getName();

    /**
     * Determine if this task should appear in the menu for manual execution
     *
     * @return true if task should be available in menu
     */
    boolean isAvailableInMenu();

    /**
     * Get an optional description of the task
     *
     * @return Description or null
     */
    default String getDescription() {
        return null;
    }
}
