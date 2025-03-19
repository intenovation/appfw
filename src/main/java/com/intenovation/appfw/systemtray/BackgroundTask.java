package com.intenovation.appfw.systemtray;

/**
 * Abstract background task that can be executed with progress tracking and cancellation.
 * Task implementations should extend this class and override the execute method.
 */
public abstract class BackgroundTask {
    private final String name;
    private final String description;
    private final int intervalSeconds;
    private final boolean availableInMenu;

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
     */
    public BackgroundTask(String name, int intervalSeconds, boolean availableInMenu) {
        this(name, null, intervalSeconds, availableInMenu);
    }

    /**
     * Create a new task with description
     * @param name The task name
     * @param description The task description
     * @param intervalSeconds The interval in seconds (0 for manual only)
     * @param availableInMenu Whether the task should be available in the menu
     */
    public BackgroundTask(String name, String description, int intervalSeconds, boolean availableInMenu) {
        this.name = name;
        this.description = description;
        this.intervalSeconds = intervalSeconds;
        this.availableInMenu = availableInMenu;
    }

    /**
     * Execute the task with progress and status reporting
     *
     * @param callback Callback for reporting progress and status messages
     * @return Optional status message that can be displayed on completion
     * @throws InterruptedException if the task is cancelled
     */
    public abstract String execute(ProgressStatusCallback callback) throws InterruptedException;

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
}