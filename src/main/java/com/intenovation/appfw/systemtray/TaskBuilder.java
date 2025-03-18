package com.intenovation.appfw.systemtray;

/**
 * Builder for creating tasks
 */
public class TaskBuilder {
    private String name;
    private String description;
    private int intervalSeconds;
    private boolean showInMenu;
    private TaskExecutor executor;

    /**
     * Functional interface for task execution
     */
    @FunctionalInterface
    public interface TaskExecutor {
        String execute(ProgressStatusCallback callback) throws InterruptedException;
    }

    /**
     * Create a new task builder
     * @param name The task name
     */
    public TaskBuilder(String name) {
        this.name = name;
    }

    /**
     * Set the task description
     * @param description The description
     * @return This builder for chaining
     */
    public TaskBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Set the task interval in seconds
     * @param intervalSeconds The interval (0 for manual only)
     * @return This builder for chaining
     */
    public TaskBuilder withIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
        return this;
    }

    /**
     * Set whether the task should show in the menu
     * @param showInMenu true to show in menu
     * @return This builder for chaining
     */
    public TaskBuilder showInMenu(boolean showInMenu) {
        this.showInMenu = showInMenu;
        return this;
    }

    /**
     * Set the task executor
     * @param executor The executor
     * @return This builder for chaining
     */
    public TaskBuilder withExecutor(TaskExecutor executor) {
        this.executor = executor;
        return this;
    }

    /**
     * Build the task
     * @return The task
     */
    public Task build() {
        return new SimpleTask(name, description, intervalSeconds, showInMenu, executor);
    }
}