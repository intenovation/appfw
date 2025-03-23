// File: SystemTrayApp.java
package com.intenovation.appfw.systemtray;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A unified system tray framework with a backend-friendly API that doesn't expose UI dependencies.
 * This framework allows developers to create system tray applications without UI programming knowledge.
 */
public class SystemTrayApp {
    private static final Logger LOGGER = Logger.getLogger(SystemTrayApp.class.getName());

    // Configuration and UI components
    private final AppConfig config;
    private final TrayIcon trayIcon;

    // Thread and task management
    private final ScheduledExecutorService scheduledExecutor;
    private final ExecutorService taskExecutor;
    private final Map<String, BackgroundTask> tasksByName = new HashMap<>();
    private final ConcurrentHashMap<String, TaskStatus> taskStatuses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    // UI components
    private final ConcurrentHashMap<String, MenuItem> taskMenuItems = new ConcurrentHashMap<>();
    private JDialog statusDialog;
    private final ConcurrentHashMap<String, JProgressBar> progressBars = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, JLabel> statusLabels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, JButton> startButtons = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, JButton> cancelButtons = new ConcurrentHashMap<>();

    /**
     * Create a system tray application
     * @param config Application configuration
     * @param menuCategories Menu categories
     * @param tasks Background tasks
     * @throws RuntimeException if the system tray is not supported
     */
    public SystemTrayApp(AppConfig config, List<MenuCategory> menuCategories, List<BackgroundTask> tasks) {
        try {
            this.config = config;

            if (!SystemTray.isSupported()) {
                throw new AWTException("System tray not supported on this platform");
            }

            // Store tasks by name
            for (BackgroundTask task : tasks) {
                tasksByName.put(task.getName(), task);
            }

            // Initialize executors
            this.scheduledExecutor = Executors.newScheduledThreadPool(Math.max(1, tasks.size()));
            this.taskExecutor = Executors.newCachedThreadPool();

            // Create popup menu
            PopupMenu popup = new PopupMenu();

            // Add menu categories
            for (MenuCategory category : menuCategories) {
                addMenuCategory(popup, category);
            }

            // Add tasks menu if needed
            boolean hasMenuTasks = tasks.stream().anyMatch(BackgroundTask::isAvailableInMenu);
            if (hasMenuTasks) {
                popup.addSeparator();
                PopupMenu tasksMenu = new PopupMenu("Tasks");

                for (BackgroundTask task : tasks) {
                    if (task.isAvailableInMenu()) {
                        String taskName = task.getName();
                        MenuItem taskItem = new MenuItem(taskName);
                        taskItem.addActionListener(e -> startTask(taskName));
                        tasksMenu.add(taskItem);

                        taskMenuItems.put(taskName, taskItem);
                        taskStatuses.put(taskName, new TaskStatus());
                    }
                }

                popup.add(tasksMenu);
            }

            // Add task status menu item
            MenuItem statusItem = new MenuItem("Task Status");
            statusItem.addActionListener(e -> showTaskStatus());
            popup.add(statusItem);

            // Add exit menu item
            popup.addSeparator();
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> exit());
            popup.add(exitItem);

            // Create tray icon
            Image image = new ImageIcon(getClass().getResource(config.getIconPath())).getImage();
            trayIcon = new TrayIcon(image, config.getAppName(), popup);
            trayIcon.setImageAutoSize(true);

            // Set default action
            trayIcon.addActionListener(e -> config.onIconDoubleClick());

            // Add icon to system tray
            SystemTray.getSystemTray().add(trayIcon);

            // Schedule tasks
            for (BackgroundTask task : tasks) {
                int interval = task.getIntervalSeconds();
                if (interval > 0) {
                    TaskStatus status = taskStatuses.computeIfAbsent(task.getName(), k -> new TaskStatus());

                    ScheduledFuture<?> future = scheduledExecutor.scheduleAtFixedRate(() -> {
                        if (!status.isRunning()) {
                            startTask(task.getName());
                        }
                    }, 0, interval, TimeUnit.SECONDS);

                    scheduledTasks.put(task.getName(), future);
                }
            }

            LOGGER.info("System tray application started successfully");
        } catch (AWTException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize system tray", e);
            throw new RuntimeException("Failed to initialize system tray: " + e.getMessage(), e);
        }
    }

    /**
     * Add a menu category to a popup menu
     * @param parent The parent menu
     * @param category The category to add
     */
    private void addMenuCategory(PopupMenu parent, MenuCategory category) {
        PopupMenu submenu = new PopupMenu(category.getLabel());

        // Add actions
        for (Action action : category.getActions()) {
            MenuItem item = new MenuItem(action.getLabel());
            item.addActionListener(e -> action.execute());
            submenu.add(item);
        }

        // Add subcategories
        for (MenuCategory subcategory : category.getSubcategories()) {
            addMenuCategory(submenu, subcategory);
        }

        parent.add(submenu);
    }

    /**
     * Start a task manually
     * @param taskName The name of the task to start
     */
    public void startTask(String taskName) {
        BackgroundTask task = tasksByName.get(taskName);
        if (task == null) {
            LOGGER.warning("Task not found: " + taskName);
            return;
        }

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
                // Create a progress/status callback that updates the UI
                ProgressStatusCallback callback = new ProgressStatusCallback() {
                    @Override
                    public void update(int percent, String message) {
                        SwingUtilities.invokeLater(() -> {
                            status.setProgress(percent);
                            status.setStatus(message);
                            updateTaskMenuLabel(taskName, percent, true);
                            updateStatusDialogComponents(taskName, percent, message, true);
                        });
                    }
                };

                // Execute the task with our combined callback
                String result = task.execute(callback);

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
                SwingUtilities.invokeLater(() -> {
                    status.setStatus("Cancelled");
                    status.setRunning(false);
                    updateTaskMenuLabel(taskName, 0, false);
                    updateStatusDialogComponents(taskName, 0, "Cancelled", false);
                    trayIcon.displayMessage(taskName, "Task cancelled", TrayIcon.MessageType.WARNING);
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in task: " + taskName, e);

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
     * Update a task menu item's label with progress
     * @param taskName The task name
     * @param progress The progress value (0-100)
     * @param running Whether the task is running
     */
    private void updateTaskMenuLabel(String taskName, int progress, boolean running) {
        MenuItem menuItem = taskMenuItems.get(taskName);
        if (menuItem != null) {
            SwingUtilities.invokeLater(() -> {
                if (running) {
                    menuItem.setLabel(taskName + " (" + progress + "%)");
                } else {
                    menuItem.setLabel(taskName);
                }
            });
        }
    }

    /**
     * Update UI components in the status dialog for a task
     * @param taskName The task name
     * @param progress The progress value (0-100)
     * @param statusText The status text
     * @param running Whether the task is running
     */
    private void updateStatusDialogComponents(String taskName, int progress, String statusText, boolean running) {
        if (statusDialog == null || !statusDialog.isVisible()) {
            return;
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
     * @param taskName The name of the task to cancel
     * @return true if successfully cancelled
     */
    public boolean cancelTask(String taskName) {
        Future<?> future = runningTasks.get(taskName);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                TaskStatus status = taskStatuses.get(taskName);
                if (status != null) {
                    status.setStatus("Cancelling...");
                    updateStatusDialogComponents(taskName, status.getProgress(), "Cancelling...", true);
                }
            }
            return cancelled;
        }
        return false;
    }

    /**
     * Show a dialog with task statuses
     */
    public void showTaskStatus() {
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

            boolean hasAnyTasks = false;

            for (Map.Entry<String, BackgroundTask> entry : tasksByName.entrySet()) {
                String taskName = entry.getKey();
                BackgroundTask task = entry.getValue();
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
                    JButton startButton = new JButton("Start");
                    startButton.setEnabled(!status.isRunning());
                    final String finalTaskName = taskName;
                    startButton.addActionListener(e -> startTask(finalTaskName));
                    buttonPanel.add(startButton);
                    startButtons.put(taskName, startButton);

                    // Add cancel button
                    JButton cancelButton = new JButton("Cancel");
                    cancelButton.setEnabled(status.isRunning());
                    cancelButton.addActionListener(e -> cancelTask(finalTaskName));
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

            // Wrap content in a scroll pane
            JScrollPane scrollPane = new JScrollPane(content);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            statusDialog.add(scrollPane);
            statusDialog.pack();
            statusDialog.setMinimumSize(new Dimension(400, 300));
            // Set preferred size to better handle larger content
            statusDialog.setPreferredSize(new Dimension(500, 400));
            statusDialog.setLocationRelativeTo(null);
            statusDialog.setVisible(true);
        });
    }

    /**
     * Exit the application
     */
    private void exit() {
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