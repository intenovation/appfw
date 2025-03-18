package com.intenovation.appfw.systemtray;

/**
 * Abstract base class for background tasks to simplify implementation
 */
public abstract class AbstractBackgroundTask implements BackgroundTask {
    private final String name;
    private final String description;
    private final int intervalSeconds;
    private final boolean availableInMenu;

    public AbstractBackgroundTask(String name, int intervalSeconds, boolean availableInMenu) {
        this(name, null, intervalSeconds, availableInMenu);
    }

    public AbstractBackgroundTask(String name, String description, int intervalSeconds, boolean availableInMenu) {
        this.name = name;
        this.description = description;
        this.intervalSeconds = intervalSeconds;
        this.availableInMenu = availableInMenu;
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
}