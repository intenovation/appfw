// File: Task.java
package com.intenovation.appfw.systemtray;

import java.util.function.Consumer;

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
     * @param progressUpdater Function to update progress (0-100)
     * @param statusUpdater Function to update status text
     * @return Result message
     * @throws InterruptedException if the task is cancelled
     */
    String execute(Consumer<Integer> progressUpdater, Consumer<String> statusUpdater) 
            throws InterruptedException;
}