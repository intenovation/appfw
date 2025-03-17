
// File: SimpleTask.java
package com.intenovation.appfw.systemtray;

import java.util.function.Consumer;

/**
 * Simple implementation of the Task interface
 */
public class SimpleTask implements Task {
    private final String name;
    private final String description;
    private final int intervalSeconds;
    private final boolean showInMenu;
    private final TaskBuilder.TaskExecutor executor;
    
    /**
     * Create a new simple task
     * @param name The task name
     * @param description The task description
     * @param intervalSeconds The interval in seconds
     * @param showInMenu Whether to show in menu
     * @param executor The task executor
     */
    public SimpleTask(String name, String description, int intervalSeconds, 
                     boolean showInMenu, TaskBuilder.TaskExecutor executor) {
        this.name = name;
        this.description = description;
        this.intervalSeconds = intervalSeconds;
        this.showInMenu = showInMenu;
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
    public boolean showInMenu() {
        return showInMenu;
    }
    
    @Override
    public String execute(Consumer<Integer> progressUpdater, Consumer<String> statusUpdater) 
            throws InterruptedException {
        if (executor != null) {
            return executor.execute(progressUpdater, statusUpdater);
        }
        return "Task completed (no executor defined)";
    }
}
