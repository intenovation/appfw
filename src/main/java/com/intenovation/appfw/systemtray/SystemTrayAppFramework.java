package com.intenovation.appfw.systemtray;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * A cross-platform framework for creating system tray applications with hierarchical menus
 * that can perform background tasks with progress tracking, status updates, and cancellation.
 * Features menu progress indicators for tasks.
 */
public class SystemTrayAppFramework {
    // Add a logger for debugging
    private static final Logger LOGGER = Logger.getLogger(SystemTrayAppFramework.class.getName());

    /**
     * Interface for a background task that can be executed with progress tracking and cancellation
     */
    public interface BackgroundTask {
        /**
         * Execute the task with progress reporting
         * @param progressCallback Callback to report progress (0-100)
         * @param statusCallback Callback to report status message
         * @return Optional status message that can be displayed on completion
         * @throws InterruptedException if the task is cancelled
         */
        String execute(ProgressCallback progressCallback, StatusCallback statusCallback) throws InterruptedException;

        /**
         * Get the interval in seconds to execute this task if scheduled
         * @return Interval in seconds, or 0 for manual-only execution
         */
        int getIntervalSeconds();

        /**
         * Get a user-friendly name for this task
         * @return Task name
         */
        String getName();

        /**
         * Determine if this task should appear in the menu for manual execution
         * @return true if task should be available in menu
         */
        boolean isAvailableInMenu();

        /**
         * Get an optional description of the task
         * @return Description or null
         */
        default String getDescription() {
            return null;
        }
    }

    /**
     * Callback interface for progress updates
     */
    public interface ProgressCallback {
        /**
         * Update progress percentage
         * @param percent Progress percentage (0-100)
         */
        void updateProgress(int percent);
    }

    /**
     * Callback interface for status updates
     */
    public interface StatusCallback {
        /**
         * Update status message
         * @param message Status message
         */
        void updateStatus(String message);
    }

    /**
     * Interface for a menu item that can be clicked
     */
    public interface MenuAction {
        /**
         * Execute the action when menu item is clicked
         */
        void execute();

        /**
         * Get the label for this menu item
         * @return Menu item label
         */
        String getLabel();
    }

    /**
     * Interface for a submenu that contains menu items
     */
    public interface MenuGroup {
        /**
         * Get the label for this submenu
         * @return Submenu label
         */
        String getLabel();

        /**
         * Get the menu items in this submenu
         * @return List of menu actions
         */
        List<MenuAction> getMenuActions();

        /**
         * Get submenus in this submenu
         * @return List of menu groups (submenus)
         */
        List<MenuGroup> getSubGroups();
    }

    /**
     * Main application interface. Developers should implement this interface.
     */
    public interface SystemTrayApp {
        /**
         * Get the application name
         * @return Application name
         */
        String getAppName();

        /**
         * Get the icon path for the system tray
         * @return Path to icon resource
         */
        String getIconPath();

        /**
         * Get the top-level menu groups
         * @return List of top-level menu groups
         */
        List<MenuGroup> getMenuGroups();

        /**
         * Get background tasks to execute
         * @return List of background tasks
         */
        List<BackgroundTask> getBackgroundTasks();

        /**
         * Action to execute when system tray icon is double-clicked
         * @return Menu action for double-click or null for no action
         */
        MenuAction getDefaultAction();
    }

    private final SystemTrayApp app;
    private final TrayIcon trayIcon;
    private final ScheduledExecutorService scheduledExecutor;
    private final ExecutorService taskExecutor;
    private final ConcurrentHashMap<String, TaskStatus> taskStatuses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MenuItem> taskMenuItems = new ConcurrentHashMap<>();
    private PopupMenu tasksMenu; // Store reference to the tasks menu
    private JDialog statusDialog;
    private final ConcurrentHashMap<String, JProgressBar> progressBars = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, JLabel> statusLabels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, JButton> startButtons = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, JButton> cancelButtons = new ConcurrentHashMap<>();

    /**
     * Creates and initializes the system tray application
     * @param app The application implementation
     * @throws AWTException If the system tray is not supported
     */
    public SystemTrayAppFramework(SystemTrayApp app) throws AWTException {
        if (!SystemTray.isSupported()) {
            throw new AWTException("System tray not supported on this platform");
        }

        this.app = app;
        this.scheduledExecutor = Executors.newScheduledThreadPool(Math.max(1, app.getBackgroundTasks().size()));
        this.taskExecutor = Executors.newCachedThreadPool();

        LOGGER.info("Initializing System Tray Framework");

        // Create popup menu
        PopupMenu popup = new PopupMenu();

        // Add menu groups
        for (MenuGroup group : app.getMenuGroups()) {
            addMenuGroup(popup, group);
        }

        // Add tasks menu if there are tasks available in menu
        boolean hasMenuTasks = app.getBackgroundTasks().stream()
                .anyMatch(BackgroundTask::isAvailableInMenu);

        if (hasMenuTasks) {
            popup.addSeparator();
            tasksMenu = new PopupMenu("Tasks");

            LOGGER.info("Adding task menu items:");
            for (BackgroundTask task : app.getBackgroundTasks()) {
                if (task.isAvailableInMenu()) {
                    String taskName = task.getName();
                    LOGGER.info("Adding task menu item: " + taskName);

                    MenuItem taskItem = new MenuItem(taskName);
                    taskItem.addActionListener(e -> startTask(task));
                    tasksMenu.add(taskItem);

                    // Store the menu item for later updates
                    taskMenuItems.put(taskName, taskItem);
                    LOGGER.info("Stored menu item for task: " + taskName);

                    // Initialize task status
                    taskStatuses.put(taskName, new TaskStatus());
                }
            }

            popup.add(tasksMenu);
        }

        // Add task status menu item
        MenuItem statusItem = new MenuItem("Task Status");
        statusItem.addActionListener(e -> showTaskStatusDialog());
        popup.add(statusItem);

        // Add a separator and exit menu item
        popup.addSeparator();
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> exit());
        popup.add(exitItem);

        // Setup tray icon
        Image image = new ImageIcon(getClass().getResource(app.getIconPath())).getImage();
        trayIcon = new TrayIcon(image, app.getAppName(), popup);
        trayIcon.setImageAutoSize(true);

        // Set default action if provided
        MenuAction defaultAction = app.getDefaultAction();
        if (defaultAction != null) {
            trayIcon.addActionListener(e -> defaultAction.execute());
        }

        // Add to system tray
        SystemTray.getSystemTray().add(trayIcon);

        // Schedule background tasks
        scheduleBackgroundTasks();

        LOGGER.info("System Tray Framework initialized successfully");
    }

    /**
     * Class to track task status
     */
    private static class TaskStatus {
        private volatile int progress = 0;
        private volatile String status = "Not started";
        private volatile boolean running = false;

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = Math.max(0, Math.min(100, progress));
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }
    }

    private void addMenuGroup(PopupMenu parent, MenuGroup group) {
        // Create submenu if it has subitems
        PopupMenu submenu = new PopupMenu(group.getLabel());

        // Add menu actions
        for (MenuAction action : group.getMenuActions()) {
            MenuItem item = new MenuItem(action.getLabel());
            item.addActionListener(e -> action.execute());
            submenu.add(item);
        }

        // Add subgroups recursively
        if (group.getSubGroups() != null) {
            for (MenuGroup subGroup : group.getSubGroups()) {
                addMenuGroup(submenu, subGroup);
            }
        }

        // Add the submenu to parent
        parent.add(submenu);
    }

    private void scheduleBackgroundTasks() {
        List<BackgroundTask> tasks = app.getBackgroundTasks();
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (BackgroundTask task : tasks) {
            int interval = task.getIntervalSeconds();
            if (interval > 0) {
                TaskStatus status = taskStatuses.computeIfAbsent(task.getName(), k -> new TaskStatus());

                ScheduledFuture<?> future = scheduledExecutor.scheduleAtFixedRate(() -> {
                    if (!status.isRunning()) {
                        startTask(task);
                    }
                }, 0, interval, TimeUnit.SECONDS);

                scheduledTasks.put(task.getName(), future);
            }
        }
    }

    /**
     * Update the menu item label with progress
     * @param taskName Name of the task
     * @param progress Progress percentage (0-100)
     * @param running Whether the task is running
     */
    private void updateTaskMenuLabel(final String taskName, final int progress, final boolean running) {
        MenuItem menuItem = taskMenuItems.get(taskName);
        if (menuItem != null) {
            SwingUtilities.invokeLater(() -> {
                String baseLabel = taskName;
                if (running) {
                    String newLabel = baseLabel + " (" + progress + "%)";
                    LOGGER.info("Updating menu label for task '" + taskName + "' to: " + newLabel);
                    menuItem.setLabel(newLabel);
                } else {
                    LOGGER.info("Resetting menu label for task '" + taskName + "' to: " + baseLabel);
                    menuItem.setLabel(baseLabel);
                }
            });
        } else {
            LOGGER.warning("Menu item not found for task: " + taskName);
        }
    }

    /**
     * Start a background task with progress tracking
     * @param task The task to start
     */
    public void startTask(BackgroundTask task) {
        final String taskName = task.getName();
        TaskStatus status = taskStatuses.computeIfAbsent(taskName, k -> new TaskStatus());

        // Don't start if already running
        if (status.isRunning()) {
            trayIcon.displayMessage(taskName, "Task already running", TrayIcon.MessageType.INFO);
            return;
        }

        LOGGER.info("Starting task: " + taskName);

        // Update status
        status.setProgress(0);
        status.setStatus("Starting...");
        status.setRunning(true);

        // Update menu item
        updateTaskMenuLabel(taskName, 0, true);

        // Update UI components in status dialog if it's open
        updateStatusDialogComponents(taskName, 0, "Starting...", true);

        // Start task in executor
        Future<?> future = taskExecutor.submit(() -> {
            try {
                String result = task.execute(
                        // Progress callback
                        percent -> {
                            LOGGER.info("Progress update for task '" + taskName + "': " + percent + "%");
                            SwingUtilities.invokeLater(() -> {
                                status.setProgress(percent);
                                updateTaskMenuLabel(taskName, percent, true);
                                updateStatusDialogComponents(taskName, percent, status.getStatus(), true);
                            });
                        },
                        // Status callback
                        message -> {
                            LOGGER.info("Status update for task '" + taskName + "': " + message);
                            SwingUtilities.invokeLater(() -> {
                                status.setStatus(message);
                                updateStatusDialogComponents(taskName, status.getProgress(), message, true);
                            });
                        }
                );

                LOGGER.info("Task completed: " + taskName + " with result: " + result);

                SwingUtilities.invokeLater(() -> {
                    status.setProgress(100);
                    status.setStatus("Completed");
                    status.setRunning(false);
                    updateTaskMenuLabel(taskName, 100, false);
                    updateStatusDialogComponents(taskName, 100, "Completed", false);

                    if (result != null && !result.isEmpty()) {
                        trayIcon.displayMessage(taskName, result, TrayIcon.MessageType.INFO);
                    }
                });
            } catch (InterruptedException e) {
                LOGGER.info("Task cancelled: " + taskName);

                SwingUtilities.invokeLater(() -> {
                    status.setStatus("Cancelled");
                    status.setRunning(false);
                    updateTaskMenuLabel(taskName, 0, false);
                    updateStatusDialogComponents(taskName, 0, "Cancelled", false);
                    trayIcon.displayMessage(taskName, "Task cancelled", TrayIcon.MessageType.WARNING);
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in task '" + taskName + "': " + e.getMessage(), e);

                SwingUtilities.invokeLater(() -> {
                    status.setStatus("Error: " + e.getMessage());
                    status.setRunning(false);
                    updateTaskMenuLabel(taskName, 0, false);
                    updateStatusDialogComponents(taskName, 0, "Error: " + e.getMessage(), false);
                    trayIcon.displayMessage(taskName,
                            "Error: " + e.getMessage(),
                            TrayIcon.MessageType.ERROR);
                });
            }
        });

        // Store future for cancellation
        runningTasks.put(taskName, future);
    }

    /**
     * Update UI components in the status dialog for a specific task
     * @param taskName The task name
     * @param progress The progress value (0-100)
     * @param statusText The status text
     * @param running Whether the task is running
     */
    private void updateStatusDialogComponents(String taskName, int progress, String statusText, boolean running) {
        if (statusDialog == null || !statusDialog.isVisible()) {
            return; // No need to update if dialog isn't visible
        }

        SwingUtilities.invokeLater(() -> {
            JProgressBar progressBar = progressBars.get(taskName);
            JLabel statusLabel = statusLabels.get(taskName);
            JButton startButton = startButtons.get(taskName);
            JButton cancelButton = cancelButtons.get(taskName);

            if (progressBar != null) {
                progressBar.setValue(progress);
            }

            if (statusLabel != null) {
                statusLabel.setText(statusText);
            }

            if (startButton != null) {
                startButton.setEnabled(!running);
            }

            if (cancelButton != null) {
                cancelButton.setEnabled(running);
            }
        });
    }

    /**
     * Cancel a running task
     * @param taskName Name of the task to cancel
     * @return true if task was running and cancelled
     */
    public boolean cancelTask(String taskName) {
        LOGGER.info("Attempting to cancel task: " + taskName);

        Future<?> future = runningTasks.get(taskName);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                LOGGER.info("Task cancelled successfully: " + taskName);

                TaskStatus status = taskStatuses.get(taskName);
                if (status != null) {
                    status.setStatus("Cancelling...");
                    updateStatusDialogComponents(taskName, status.getProgress(), "Cancelling...", true);
                }
            } else {
                LOGGER.warning("Failed to cancel task: " + taskName);
            }
            return cancelled;
        }
        return false;
    }

    /**
     * Show a dialog with all task statuses
     */
    private void showTaskStatusDialog() {
        // If dialog already exists and is visible, just bring it to front
        if (statusDialog != null && statusDialog.isVisible()) {
            statusDialog.toFront();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            statusDialog = new JDialog((Frame) null, "Task Status", false);
            statusDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(new EmptyBorder(10, 10, 10, 10));

            // Clear previous component references
            progressBars.clear();
            statusLabels.clear();
            startButtons.clear();
            cancelButtons.clear();

            List<BackgroundTask> tasks = app.getBackgroundTasks();
            boolean hasAnyTasks = false;

            for (BackgroundTask task : tasks) {
                String taskName = task.getName();
                TaskStatus status = taskStatuses.get(taskName);
                if (status != null) {
                    hasAnyTasks = true;

                    JPanel taskPanel = new JPanel();
                    taskPanel.setLayout(new BorderLayout(5, 5));
                    taskPanel.setBorder(new EmptyBorder(5, 0, 10, 0));

                    // Task name and description
                    JPanel headerPanel = new JPanel(new BorderLayout());
                    JLabel nameLabel = new JLabel(taskName);
                    nameLabel.setFont(nameLabel.getFont().deriveFont(java.awt.Font.BOLD));
                    headerPanel.add(nameLabel, BorderLayout.NORTH);

                    if (task.getDescription() != null) {
                        JLabel descLabel = new JLabel(task.getDescription());
                        descLabel.setFont(descLabel.getFont().deriveFont(java.awt.Font.ITALIC));
                        headerPanel.add(descLabel, BorderLayout.CENTER);
                    }

                    taskPanel.add(headerPanel, BorderLayout.NORTH);

                    // Progress bar
                    JProgressBar progressBar = new JProgressBar(0, 100);
                    progressBar.setValue(status.getProgress());
                    progressBar.setStringPainted(true);
                    taskPanel.add(progressBar, BorderLayout.CENTER);
                    progressBars.put(taskName, progressBar);

                    // Status label and control buttons
                    JPanel controlPanel = new JPanel(new BorderLayout(5, 0));
                    JLabel statusLabel = new JLabel(status.getStatus());
                    controlPanel.add(statusLabel, BorderLayout.CENTER);
                    statusLabels.put(taskName, statusLabel);

                    JPanel buttonPanel = new JPanel();

                    // Add start button
                    BackgroundTask finalTask = task; // Need final reference for lambda
                    JButton startButton = new JButton("Start");
                    startButton.setEnabled(!status.isRunning());
                    startButton.addActionListener(e -> {
                        startTask(finalTask);
                    });
                    buttonPanel.add(startButton);
                    startButtons.put(taskName, startButton);

                    // Add cancel button
                    JButton cancelButton = new JButton("Cancel");
                    cancelButton.setEnabled(status.isRunning());
                    cancelButton.addActionListener(e -> {
                        cancelTask(taskName);
                    });
                    buttonPanel.add(cancelButton);
                    cancelButtons.put(taskName, cancelButton);

                    controlPanel.add(buttonPanel, BorderLayout.EAST);
                    taskPanel.add(controlPanel, BorderLayout.SOUTH);

                    content.add(taskPanel);
                }
            }

            if (!hasAnyTasks) {
                content.add(new JLabel("No tasks available"));
            }

            // Close button at bottom
            JPanel bottomPanel = new JPanel();
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> statusDialog.dispose());
            bottomPanel.add(closeButton);
            content.add(bottomPanel);

            statusDialog.add(content);
            statusDialog.pack();
            statusDialog.setMinimumSize(new Dimension(400, 300));
            statusDialog.setLocationRelativeTo(null);
            statusDialog.setVisible(true);
        });
    }

    /**
     * Open a directory in the system file explorer
     * Works cross-platform on Windows, macOS, and Linux
     *
     * @param directory The directory to open
     * @return true if successful, false otherwise
     */
    public static boolean openDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return false;
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(directory);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Fallback to command line for specific operating systems
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("windows")) {
                Runtime.getRuntime().exec("explorer.exe \"" + directory.getAbsolutePath() + "\"");
                return true;
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", directory.getAbsolutePath()});
                return true;
            } else if (os.contains("linux")) {
                Runtime.getRuntime().exec(new String[]{"xdg-open", directory.getAbsolutePath()});
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Exits the application and cleans up resources
     */
    public void exit() {
        LOGGER.info("Exiting application");

        // Cancel all scheduled tasks
        for (ScheduledFuture<?> future : scheduledTasks.values()) {
            future.cancel(false);
        }

        // Cancel all running tasks
        for (Future<?> future : runningTasks.values()) {
            future.cancel(true);
        }

        // Shutdown executors
        scheduledExecutor.shutdown();
        taskExecutor.shutdown();

        // Close status dialog if open
        if (statusDialog != null && statusDialog.isVisible()) {
            statusDialog.dispose();
        }

        // Remove tray icon
        SystemTray.getSystemTray().remove(trayIcon);

        // Exit application
        System.exit(0);
    }

    /**
     * Helper method to display a message dialog
     * @param title Dialog title
     * @param message Message to display
     * @param messageType Message type (JOptionPane constants)
     */
    public static void showMessage(String title, String message, int messageType) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, title, messageType);
        });
    }

    /**
     * Simple implementation of MenuGroup to simplify creating menus
     */
    public static class SimpleMenuGroup implements MenuGroup {
        private final String label;
        private final List<MenuAction> actions = new ArrayList<>();
        private final List<MenuGroup> subGroups = new ArrayList<>();

        public SimpleMenuGroup(String label) {
            this.label = label;
        }

        public SimpleMenuGroup addAction(MenuAction action) {
            actions.add(action);
            return this;
        }

        public SimpleMenuGroup addSubGroup(MenuGroup group) {
            subGroups.add(group);
            return this;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public List<MenuAction> getMenuActions() {
            return actions;
        }

        @Override
        public List<MenuGroup> getSubGroups() {
            return subGroups;
        }
    }

    /**
     * Simple implementation of MenuAction
     */
    public static class SimpleMenuAction implements MenuAction {
        private final String label;
        private final Runnable action;

        public SimpleMenuAction(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        @Override
        public void execute() {
            action.run();
        }

        @Override
        public String getLabel() {
            return label;
        }
    }

    /**
     * Abstract base class for background tasks to simplify implementation
     */
    public static abstract class AbstractBackgroundTask implements BackgroundTask {
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
}