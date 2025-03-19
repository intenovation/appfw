package com.intenovation.appfw.systemtray;

/**
 * Factory methods for creating background tasks.
 * This provides a more elegant alternative to the builder pattern.
 */
public class Tasks {

    /**
     * Create a simple background task
     *
     * @param name Task name
     * @param description Task description
     * @param intervalSeconds Interval in seconds (0 for manual only)
     * @param availableInMenu Whether task is available in menu
     * @param executor Task execution function
     * @return A new background task
     */
    public static BackgroundTask create(
            String name,
            String description,
            int intervalSeconds,
            boolean availableInMenu,
            BackgroundTaskImpl.TaskExecutor executor) {
        return new BackgroundTaskImpl(name, description, intervalSeconds, availableInMenu, executor);
    }

    /**
     * Create a simple background task without description
     *
     * @param name Task name
     * @param intervalSeconds Interval in seconds (0 for manual only)
     * @param availableInMenu Whether task is available in menu
     * @param executor Task execution function
     * @return A new background task
     */
    public static BackgroundTask create(
            String name,
            int intervalSeconds,
            boolean availableInMenu,
            BackgroundTaskImpl.TaskExecutor executor) {
        return new BackgroundTaskImpl(name, intervalSeconds, availableInMenu, executor);
    }

    /**
     * Create a manual-only task
     *
     * @param name Task name
     * @param description Task description
     * @param executor Task execution function
     * @return A new background task that runs only when manually triggered
     */
    public static BackgroundTask manual(
            String name,
            String description,
            BackgroundTaskImpl.TaskExecutor executor) {
        return new BackgroundTaskImpl(name, description, 0, true, executor);
    }

    /**
     * Create a scheduled-only task
     *
     * @param name Task name
     * @param description Task description
     * @param intervalSeconds Interval in seconds
     * @param executor Task execution function
     * @return A new background task that runs on schedule but is not in menu
     */
    public static BackgroundTask scheduled(
            String name,
            String description,
            int intervalSeconds,
            BackgroundTaskImpl.TaskExecutor executor) {
        return new BackgroundTaskImpl(name, description, intervalSeconds, false, executor);
    }

    /**
     * Start building a task with a fluent builder
     *
     * @param name Task name
     * @return A builder for creating a task
     */
    public static BackgroundTaskImpl.Builder builder(String name) {
        return new BackgroundTaskImpl.Builder(name);
    }
}