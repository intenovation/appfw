package com.intenovation.appfw.systemtray;

/**
 * Standard implementation of the BackgroundTask interface.
 * This combines functionality from the previous AbstractBackgroundTask and SimpleTask classes.
 */
public class BackgroundTaskImpl implements BackgroundTask {
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
    public BackgroundTaskImpl(String name, int intervalSeconds, boolean availableInMenu, TaskExecutor executor) {
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
    public BackgroundTaskImpl(String name, String description, int intervalSeconds, boolean availableInMenu, TaskExecutor executor) {
        this.name = name;
        this.description = description;
        this.intervalSeconds = intervalSeconds;
        this.availableInMenu = availableInMenu;
        this.executor = executor;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    @Override
    public boolean isAvailableInMenu() {
        return availableInMenu;
    }

    @Override
    public String execute(ProgressStatusCallback callback) throws InterruptedException {
        if (executor != null) {
            return executor.execute(callback);
        }
        return "Task completed (no executor defined)";
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
            return new BackgroundTaskImpl(name, description, intervalSeconds, availableInMenu, executor);
        }
    }
}