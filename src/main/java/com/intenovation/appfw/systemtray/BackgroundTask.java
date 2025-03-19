package com.intenovation.appfw.systemtray;

/**
 * Background task that can be executed with progress tracking and cancellation.
 * This unified class combines the former BackgroundTask interface, BackgroundTaskImpl class,
 * and Tasks factory methods into a single comprehensive implementation.
 */
public class BackgroundTask {
    private final String name;
    private final String description;
    private final int intervalSeconds;
    private final boolean availableInMenu;
    private final TaskExecutor executor;

    /**
     * Functional interface for task execution
     */
    @FunctionalInterface
    public interface TaskExecutor {
        String execute(ProgressStatusCallback callback) throws InterruptedException;
    }

    /**
     * Create a new task
     * @param name The task name
     * @param intervalSeconds The interval in seconds (0 for manual only)
     * @param availableInMenu Whether the task should be available in the menu
     * @param executor The task executor
     */
    public BackgroundTask(String name, int intervalSeconds, boolean availableInMenu, TaskExecutor executor) {
        this(name, null, intervalSeconds, availableInMenu, executor);
    }

    /**
     * Create a new task with description
     * @param name The task name
     * @param description The task description
     * @param intervalSeconds The interval in seconds (0 for manual only)
     * @param availableInMenu Whether the task should be available in the menu
     * @param executor The task executor
     */
    public BackgroundTask(String name, String description, int intervalSeconds, boolean availableInMenu, TaskExecutor executor) {
        this.name = name;
        this.description = description;
        this.intervalSeconds = intervalSeconds;
        this.availableInMenu = availableInMenu;
        this.executor = executor;
    }

    /**
     * Execute the task with progress and status reporting
     *
     * @param callback Callback for reporting progress and status messages
     * @return Optional status message that can be displayed on completion
     * @throws InterruptedException if the task is cancelled
     */
    public String execute(ProgressStatusCallback callback) throws InterruptedException {
        if (executor != null) {
            return executor.execute(callback);
        }
        return "Task completed (no executor defined)";
    }

    /**
     * Get the interval in seconds to execute this task if scheduled
     *
     * @return Interval in seconds, or 0 for manual-only execution
     */
    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    /**
     * Get a user-friendly name for this task
     *
     * @return Task name
     */
    public String getName() {
        return name;
    }

    /**
     * Determine if this task should appear in the menu for manual execution
     *
     * @return true if task should be available in menu
     */
    public boolean isAvailableInMenu() {
        return availableInMenu;
    }

    /**
     * Get an optional description of the task
     *
     * @return Description or null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Builder for creating tasks
     */
    public static class Builder {
        private String name;
        private String description;
        private int intervalSeconds;
        private boolean availableInMenu;
        private TaskExecutor executor;

        /**
         * Create a new task builder
         * @param name The task name
         */
        public Builder(String name) {
            this.name = name;
        }

        /**
         * Set the task description
         * @param description The description
         * @return This builder for chaining
         */
        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the task interval in seconds
         * @param intervalSeconds The interval (0 for manual only)
         * @return This builder for chaining
         */
        public Builder withIntervalSeconds(int intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
            return this;
        }

        /**
         * Set whether the task should be available in the menu
         * @param availableInMenu true to show in menu
         * @return This builder for chaining
         */
        public Builder availableInMenu(boolean availableInMenu) {
            this.availableInMenu = availableInMenu;
            return this;
        }

        /**
         * Set the task executor
         * @param executor The executor
         * @return This builder for chaining
         */
        public Builder withExecutor(TaskExecutor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Build the task
         * @return The task
         */
        public BackgroundTask build() {
            return new BackgroundTask(name, description, intervalSeconds, availableInMenu, executor);
        }
    }

    // Factory methods (formerly in Tasks class)

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
            TaskExecutor executor) {
        return new BackgroundTask(name, description, intervalSeconds, availableInMenu, executor);
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
            TaskExecutor executor) {
        return new BackgroundTask(name, intervalSeconds, availableInMenu, executor);
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
            TaskExecutor executor) {
        return new BackgroundTask(name, description, 0, true, executor);
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
            TaskExecutor executor) {
        return new BackgroundTask(name, description, intervalSeconds, false, executor);
    }

    /**
     * Start building a task with a fluent builder
     *
     * @param name Task name
     * @return A builder for creating a task
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }
}