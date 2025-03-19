package com.intenovation.appfw.systemtray;

/**
 * Simple implementation of the BackgroundTask interface
 */
public class SimpleTask implements BackgroundTask {
    private final String name;
    private final String description;
    private final int intervalSeconds;
    private final boolean availableInMenu;
    private final TaskBuilder.TaskExecutor executor;

    /**
     * Create a new simple task
     * @param name The task name
     * @param description The task description
     * @param intervalSeconds The interval in seconds
     * @param availableInMenu Whether to show in menu
     * @param executor The task executor
     */
    public SimpleTask(String name, String description, int intervalSeconds,
                      boolean availableInMenu, TaskBuilder.TaskExecutor executor) {
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
}