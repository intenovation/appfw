package com.intenovation.appfw.systemtray;

/**
 * Task interface for implementing background tasks
 */
public interface Task {
    /**
     * Get the task name
     * @return Task name
     */
    String getName();

    /**
     * Get the task description
     * @return Task description or null
     */
    String getDescription();

    /**
     * Get the interval in seconds for scheduled execution
     * @return Interval in seconds (0 for manual execution only)
     */
    int getIntervalSeconds();

    /**
     * Whether this task should appear in the menu
     * @return true if available in menu
     */
    boolean showInMenu();

    /**
     * Execute the task with progress and status updates
     * @param callback Callback for progress and status updates
     * @return Result message
     * @throws InterruptedException if the task is cancelled
     */
    String execute(ProgressStatusCallback callback) throws InterruptedException;
}