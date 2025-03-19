// File: SystemTrayApp.java
package com.intenovation.appfw.systemtray;

import com.intenovation.appfw.systemtray.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.AWTException;

/**
 * A unified system tray framework with a backend-friendly API that doesn't expose UI dependencies.
 * This framework allows developers to create system tray applications without UI programming knowledge.
 */
public class SystemTrayApp {
    private static final Logger LOGGER = Logger.getLogger(SystemTrayApp.class.getName());
    private final SystemTrayAppImpl implementation;

    /**
     * Create a system tray application
     * @param config Application configuration
     * @param menuCategories Menu categories
     * @param tasks Background tasks
     * @throws RuntimeException if the system tray is not supported
     */
    public SystemTrayApp(AppConfig config, List<MenuCategory> menuCategories, List<Task> tasks) {
        try {
            implementation = new SystemTrayAppImpl(config, menuCategories, tasks);
            LOGGER.info("System tray application started successfully");
        } catch (AWTException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize system tray", e);
            throw new RuntimeException("Failed to initialize system tray: " + e.getMessage(), e);
        }
    }

    /**
     * Start a task manually
     * @param taskName The name of the task to start
     */
    public void startTask(String taskName) {
        implementation.startTask(taskName);
    }

    /**
     * Cancel a running task
     * @param taskName The name of the task to cancel
     * @return true if successfully cancelled
     */
    public boolean cancelTask(String taskName) {
        return implementation.cancelTask(taskName);
    }

    /**
     * Show a status dialog with all tasks
     */
    public void showTaskStatus() {
        implementation.showTaskStatusDialog();
    }

    /**
     * Show an information message
     * @param title Message title
     * @param message Message content
     */
    public static void showMessage(String title, String message) {
        UIHelper.showMessage(title, message);
    }

    /**
     * Show an error message
     * @param title Message title
     * @param message Error message
     */
    public static void showError(String title, String message) {
        UIHelper.showError(title, message);
    }

    /**
     * Show a warning message
     * @param title Message title
     * @param message Warning message
     */
    public static void showWarning(String title, String message) {
        UIHelper.showWarning(title, message);
    }

    /**
     * Open a directory in the system file explorer
     * @param directory The directory to open
     * @return true if successful
     */
    public static boolean openDirectory(File directory) {
        return UIHelper.openDirectory(directory);
    }

}