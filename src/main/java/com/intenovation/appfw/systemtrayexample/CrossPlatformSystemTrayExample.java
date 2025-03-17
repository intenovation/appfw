package com.intenovation.appfw.systemtrayexample;

import com.intenovation.appfw.systemtray.SystemTrayAppFramework;
import com.intenovation.appfw.systemtray.SystemTrayAppFramework.*;
import java.awt.AWTException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * Cross-platform example application that demonstrates the enhanced system tray framework
 * with progress tracking, status updates, and task cancellation.
 * Works on Windows and macOS.
 */
public class CrossPlatformSystemTrayExample implements SystemTrayApp {

    public static void main(String[] args) {
        try {
            new SystemTrayAppFramework(new CrossPlatformSystemTrayExample());
            System.out.println("Cross-platform system tray app started");
        } catch (AWTException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Could not initialize system tray: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public String getAppName() {
        return "Cross-Platform System Tray Example";
    }

    @Override
    public String getIconPath() {
        return "/intenovation.png";  // Place an icon.png in your resources folder
    }

    @Override
    public List<MenuGroup> getMenuGroups() {
        // Create a File operations menu group
        SimpleMenuGroup fileOperations = new SimpleMenuGroup("File Operations");
        fileOperations.addAction(new SimpleMenuAction("Open Home Directory", () -> {
            File homeDir = new File(System.getProperty("user.home"));
            if (!SystemTrayAppFramework.openDirectory(homeDir)) {
                SystemTrayAppFramework.showMessage(
                        "Error",
                        "Could not open home directory",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }));

        // Create a cross-platform Tools menu group
        SimpleMenuGroup tools = new SimpleMenuGroup("Tools");
        tools.addAction(new SimpleMenuAction("System Info", () -> {
            StringBuilder info = new StringBuilder();
            info.append("OS: ").append(System.getProperty("os.name")).append("\n");
            info.append("OS Version: ").append(System.getProperty("os.version")).append("\n");
            info.append("OS Arch: ").append(System.getProperty("os.arch")).append("\n");
            info.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
            info.append("Java Home: ").append(System.getProperty("java.home")).append("\n");
            info.append("User Home: ").append(System.getProperty("user.home")).append("\n");
            info.append("Memory Usage: ").append(
                            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024)
                    .append(" MB");

            JOptionPane.showMessageDialog(null, info.toString(), "System Info", JOptionPane.INFORMATION_MESSAGE);
        }));

        // Return the list of top-level menu groups
        return Arrays.asList(fileOperations, tools);
    }

    @Override
    public List<BackgroundTask> getBackgroundTasks() {
        return Arrays.asList(
                // File indexer task (cross-platform implementation)
                new AbstractBackgroundTask("File Indexer",
                        "Indexes files in the user home directory",
                        0,
                        true) {
                    @Override
                    public String execute(ProgressCallback progressCallback, StatusCallback statusCallback)
                            throws InterruptedException {
                        File homeDir = new File(System.getProperty("user.home"));

                        if (!homeDir.exists() || !homeDir.isDirectory()) {
                            return "Home directory not found or not accessible";
                        }

                        try {
                            statusCallback.updateStatus("Counting files...");
                            progressCallback.updateProgress(0);

                            // Count files first to get total
                            int[] totalFiles = {0};
                            Files.walk(homeDir.toPath())
                                    .filter(Files::isRegularFile)
                                    .forEach(p -> totalFiles[0]++);

                            if (totalFiles[0] == 0) {
                                return "No files found";
                            }

                            statusCallback.updateStatus("Starting indexing of " + totalFiles[0] + " files");
                            progressCallback.updateProgress(0);

                            // Index file counter
                            final int[] processedFiles = {0};

                            // Process files
                            Files.walk(homeDir.toPath())
                                    .filter(Files::isRegularFile)
                                    .forEach(path -> {
                                        // Check for interruption
                                        if (Thread.currentThread().isInterrupted()) {
                                            throw new RuntimeException(new InterruptedException("Task cancelled"));
                                        }

                                        // Update progress
                                        processedFiles[0]++;
                                        int progress = (int)((processedFiles[0] / (double)totalFiles[0]) * 100);
                                        progressCallback.updateProgress(progress);

                                        // Update status periodically
                                        if (processedFiles[0] % 100 == 0 || progress >= 100) {
                                            statusCallback.updateStatus("Indexed " + processedFiles[0] +
                                                    " of " + totalFiles[0] + " files");
                                        }

                                        // Simulate processing time
                                        try {
                                            Thread.sleep(5);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                            throw new RuntimeException(e);
                                        }
                                    });

                            return "Successfully indexed " + processedFiles[0] + " files";
                        } catch (RuntimeException e) {
                            if (e.getCause() instanceof InterruptedException) {
                                throw (InterruptedException) e.getCause();
                            }
                            return "Error during indexing: " + e.getMessage();
                        } catch (IOException e) {
                            return "Error accessing files: " + e.getMessage();
                        }
                    }
                },

                // Cross-platform disk cleanup task
                new AbstractBackgroundTask("Disk Cleanup",
                        "Cleans temporary files",
                        3600,
                        true) {
                    @Override
                    public String execute(ProgressCallback progressCallback, StatusCallback statusCallback)
                            throws InterruptedException {
                        // Get temp directory (cross-platform)
                        File tempDir = new File(System.getProperty("java.io.tmpdir"));

                        statusCallback.updateStatus("Analyzing temp directory: " + tempDir.getAbsolutePath());
                        progressCallback.updateProgress(10);

                        // Check for interruption
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException("Task cancelled");
                        }

                        // Simulate analysis
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            throw e;
                        }

                        // Count files
                        statusCallback.updateStatus("Counting temporary files...");
                        progressCallback.updateProgress(20);

                        File[] tempFiles = tempDir.listFiles();
                        if (tempFiles == null || tempFiles.length == 0) {
                            return "No temporary files found to clean";
                        }

                        // Only process regular files, not directories
                        List<File> filesToDelete = Arrays.stream(tempFiles)
                                .filter(File::isFile)
                                .filter(f -> !f.getName().startsWith(".")) // Skip hidden files
                                .toList();

                        int totalSize = filesToDelete.size();
                        if (totalSize == 0) {
                            return "No temporary files found to clean";
                        }

                        statusCallback.updateStatus("Found " + totalSize + " files to clean");
                        progressCallback.updateProgress(30);

                        // Simulate cleanup (we won't actually delete files for safety)
                        int deletedFiles = 0;
                        long deletedBytes = 0;

                        for (int i = 0; i < totalSize; i++) {
                            // Check for interruption
                            if (Thread.currentThread().isInterrupted()) {
                                throw new InterruptedException("Task cancelled");
                            }

                            File file = filesToDelete.get(i);

                            // Simulate file deletion
                            try {
                                // In a real application, you would delete the file here
                                // file.delete();

                                // For simulation, just record the details
                                deletedFiles++;
                                deletedBytes += file.length();

                                // Update progress (from 30% to 90%)
                                int progress = 30 + (60 * (i + 1) / totalSize);
                                progressCallback.updateProgress(progress);

                                // Update status periodically
                                if ((i + 1) % 10 == 0 || (i + 1) == totalSize) {
                                    statusCallback.updateStatus("Cleaned " + (i + 1) + " of " + totalSize + " files");
                                }

                                // Simulate processing time
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                throw e;
                            }
                        }

                        // Finalize
                        statusCallback.updateStatus("Finalizing cleanup...");
                        progressCallback.updateProgress(95);

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw e;
                        }

                        progressCallback.updateProgress(100);

                        // Format the deleted bytes in a human-readable format
                        String freedSpace;
                        if (deletedBytes > 1024 * 1024 * 1024) {
                            freedSpace = String.format("%.2f GB", deletedBytes / (1024.0 * 1024.0 * 1024.0));
                        } else if (deletedBytes > 1024 * 1024) {
                            freedSpace = String.format("%.2f MB", deletedBytes / (1024.0 * 1024.0));
                        } else if (deletedBytes > 1024) {
                            freedSpace = String.format("%.2f KB", deletedBytes / 1024.0);
                        } else {
                            freedSpace = deletedBytes + " bytes";
                        }

                        return "Simulated cleanup completed. Would have freed " + freedSpace +
                                " by removing " + deletedFiles + " files.";
                    }
                },

                // Cross-platform log rotation task
                new AbstractBackgroundTask("Log Rotation",
                        "Rotates and compresses log files",
                        7200, // Every 2 hours
                        true) {
                    @Override
                    public String execute(ProgressCallback progressCallback, StatusCallback statusCallback)
                            throws InterruptedException {
                        // Create a platform-independent path to logs
                        Path logsDir = Paths.get(System.getProperty("user.home"), "logs");

                        // Create the logs directory if it doesn't exist
                        try {
                            Files.createDirectories(logsDir);
                        } catch (IOException e) {
                            return "Could not create logs directory: " + e.getMessage();
                        }

                        statusCallback.updateStatus("Starting log rotation in: " + logsDir);
                        progressCallback.updateProgress(0);

                        // Simulate log rotation process
                        try {
                            // Create a sample log file
                            Path sampleLog = logsDir.resolve("application.log");
                            String logContent = "=== Sample Log File ===\n" +
                                    "Generated on: " + new Date() + "\n" +
                                    "System: " + System.getProperty("os.name") + "\n" +
                                    "User: " + System.getProperty("user.name") + "\n\n" +
                                    "This is a sample log entry.\n";

                            // Write the log file
                            Files.writeString(sampleLog, logContent);

                            statusCallback.updateStatus("Analyzing log files...");
                            progressCallback.updateProgress(20);
                            Thread.sleep(1000);

                            statusCallback.updateStatus("Rotating logs...");
                            progressCallback.updateProgress(40);
                            Thread.sleep(1500);

                            // Create a rotated log with timestamp
                            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                            Path rotatedLog = logsDir.resolve("application-" + timestamp + ".log");
                            Files.copy(sampleLog, rotatedLog);

                            statusCallback.updateStatus("Archiving old logs...");
                            progressCallback.updateProgress(60);
                            Thread.sleep(1000);

                            statusCallback.updateStatus("Cleaning up...");
                            progressCallback.updateProgress(80);
                            Thread.sleep(1000);

                            progressCallback.updateProgress(100);

                            return "Log rotation completed successfully. Logs stored in: " + logsDir;
                        } catch (IOException e) {
                            return "Error during log rotation: " + e.getMessage();
                        } catch (InterruptedException e) {
                            throw e;
                        }
                    }
                }
        );
    }

    @Override
    public MenuAction getDefaultAction() {
        return new SimpleMenuAction("Default Action", () -> {
            JOptionPane.showMessageDialog(null,
                    "Cross-Platform System Tray Example\n\n" +
                            "Operating System: " + System.getProperty("os.name") + "\n" +
                            "Java Version: " + System.getProperty("java.version"),
                    "Application Info", JOptionPane.INFORMATION_MESSAGE);
        });
    }
}