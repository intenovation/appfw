package com.intenovation.email.downloader;

import com.intenovation.appfw.systemtray.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles email archive cleanup and organization
 */
public class EmailCleanup extends BackgroundTask {
    private static final Logger LOGGER = Logger.getLogger(EmailCleanup.class.getName());

    /**
     * Create a new Email Cleanup task
     *
     * @param cleanupIntervalHours The cleanup interval in hours
     */
    public EmailCleanup(int cleanupIntervalHours) {
        super(
                "Email Cleanup",
                "Cleans up and organizes the email archive",
                cleanupIntervalHours * 3600, // Convert hours to seconds
                true                        // Available in menu
        );
    }

    /**
     * Execute the task with progress and status reporting
     *
     * @param callback Callback for reporting progress and status messages
     * @return Status message that will be displayed on completion
     * @throws InterruptedException if the task is cancelled
     */
    @Override
    public String execute(ProgressStatusCallback callback) throws InterruptedException {
        LOGGER.info("Starting Email Cleanup");

        // Create a logging wrapper around the callback
        ProgressStatusCallback loggingCallback = new ProgressStatusCallback() {
            @Override
            public void update(int percent, String message) {
                // Update the original callback
                callback.update(percent, message);

                // Log the progress
                LOGGER.info(String.format("[Email Cleanup] %d%% - %s", percent, message));
            }
        };

        try {
            // Execute the cleanup with our logging callback
            String result = cleanupEmailArchive(loggingCallback);
            LOGGER.info("Email Cleanup completed: " + result);
            return result;
        } catch (InterruptedException e) {
            LOGGER.warning("Email Cleanup was interrupted");
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Email Cleanup error", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Clean up and organize the email archive
     *
     * @param callback Callback for reporting progress and status
     * @return Status message
     * @throws InterruptedException if task is cancelled
     */
    public static String cleanupEmailArchive(ProgressStatusCallback callback)
            throws InterruptedException {
        callback.update(0, "Starting email archive cleanup...");

        String storagePath = ImapDownloader.getStoragePath();
        File baseDir = new File(storagePath);
        if (!baseDir.exists()) {
            return "Email archive directory does not exist";
        }

        try {
            // Get all folders
            File[] folders = baseDir.listFiles(File::isDirectory);
            if (folders == null || folders.length == 0) {
                return "No folders found in email archive";
            }

            int totalFolders = folders.length;
            int processedFolders = 0;
            int totalEmails = FileUtils.countAllEmails(baseDir);
            int processedEmails = 0;
            int duplicatesRemoved = 0;
            int emptyDirectoriesRemoved = 0;

            callback.update(5, "Found " + totalFolders + " folders with approximately " +
                    totalEmails + " emails");

            // Track message IDs to find duplicates
            Map<String, File> messageIds = new HashMap<>();

            // Process each folder
            for (File folder : folders) {
                // Skip hidden folders and special files
                if (folder.getName().startsWith(".") || !folder.isDirectory()) {
                    continue;
                }

                callback.update(10, "Cleaning folder: " + folder.getName());

                // Check for interruption
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Task cancelled");
                }

                // Ensure the "messages" directory exists in this folder
                File messagesDir = new File(folder, "messages");
                if (!messagesDir.exists()) {
                    // This folder might be using the old structure
                    // Check if there are message directories directly in the folder
                    boolean hasMessageDirectories = false;
                    File[] folderContents = folder.listFiles();
                    if (folderContents != null) {
                        for (File content : folderContents) {
                            if (content.isDirectory() && !content.getName().equals("messages") &&
                                    new File(content, "message.properties").exists()) {
                                hasMessageDirectories = true;
                                break;
                            }
                        }
                    }

                    if (hasMessageDirectories) {
                        // Migrate message directories to the new structure
                        messagesDir.mkdirs();

                        for (File content : folderContents) {
                            if (content.isDirectory() && !content.getName().equals("messages") &&
                                    new File(content, "message.properties").exists()) {
                                // This is a message directory in the old structure
                                File newLocation = new File(messagesDir, content.getName());
                                boolean moved = content.renameTo(newLocation);
                                if (!moved) {
                                    LOGGER.warning("Failed to move message directory: " + content.getPath());
                                }
                            }
                        }
                    } else {
                        // This is likely a normal folder without message directories
                        messagesDir.mkdirs();
                    }
                }

                // Process emails in this folder
                File[] messageDirs = messagesDir.listFiles(File::isDirectory);
                if (messageDirs != null) {
                    for (File messageDir : messageDirs) {
                        // Check for interruption
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException("Task cancelled");
                        }

                        processedEmails++;

                        // Check if this message has valid properties
                        File propsFile = new File(messageDir, "message.properties");
                        if (!propsFile.exists()) {
                            continue;
                        }

                        try {
                            // Load properties
                            Properties props = new Properties();
                            props.load(new FileInputStream(propsFile));

                            // Get message ID (prefer the folder version if available)
                            String messageId = props.getProperty("message.id.folder");
                            if (messageId == null || messageId.isEmpty()) {
                                // Fall back to original message ID and sanitize it
                                messageId = props.getProperty("message.id");
                                if (messageId != null && !messageId.isEmpty()) {
                                    messageId = FileUtils.sanitizeFileName(messageId);
                                }
                            }

                            if (messageId != null && !messageId.isEmpty()) {
                                // Check if this is a duplicate
                                if (messageIds.containsKey(messageId)) {
                                    File existingDir = messageIds.get(messageId);

                                    // Only remove if different directories
                                    if (!existingDir.equals(messageDir)) {

                                        // Keep the newer message
                                        long existingTime = existingDir.lastModified();
                                        long currentTime = messageDir.lastModified();

                                        if (currentTime > existingTime) {
                                            // Current is newer, remove existing
                                            FileUtils.deleteDirectory(existingDir);
                                            messageIds.put(messageId, messageDir);
                                        } else {
                                            // Existing is newer, remove current
                                            FileUtils.deleteDirectory(messageDir);
                                        }

                                        duplicatesRemoved++;
                                    }
                                } else {
                                    messageIds.put(messageId, messageDir);
                                }
                            }

                            // Update progress
                            if (totalEmails > 0) {
                                int progress = 5 + (90 * processedEmails / totalEmails);
                                callback.update(Math.min(95, progress)," processed Emails");
                            }

                            // Update status periodically
                            if (processedEmails % 50 == 0) {
                                callback.update(5 + (90 * processedEmails / Math.max(1, totalEmails)),
                                        "Processed " + processedEmails + " of ~" +
                                                totalEmails + " emails. Removed " +
                                                duplicatesRemoved + " duplicates.");
                            }

                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Error reading properties for " + messageDir.getName(), e);
                            // Continue with next message
                        }
                    }
                }

                // Check for and remove empty message directories
                if (messagesDir.exists() && messagesDir.list().length == 0) {
                    messagesDir.delete();
                }

                // Check for and remove empty folders
                if (folder.listFiles().length == 0) {
                    folder.delete();
                    emptyDirectoriesRemoved++;
                }

                processedFolders++;
                if (totalEmails == 0) { // Only update based on folders if no emails
                    int progress = 5 + (90 * processedFolders / totalFolders);
                    callback.update(Math.min(95, progress),"Downloading");
                }
            }

            // Finalizing
            callback.update(95, "Finalizing cleanup...");

            // Remove empty directories one more time
            folders = baseDir.listFiles(File::isDirectory);
            if (folders != null) {
                for (File folder : folders) {
                    if (folder.getName().startsWith(".")) continue;

                    // Check if messages directory is empty
                    File messagesDir = new File(folder, "messages");
                    if (messagesDir.exists() && messagesDir.listFiles().length == 0) {
                        messagesDir.delete();
                    }

                    // Check if folder is now empty
                    if (folder.isDirectory() && folder.listFiles().length == 0) {
                        folder.delete();
                        emptyDirectoriesRemoved++;
                    }
                }
            }

            // Compact the database if needed
            File[] allFiles = baseDir.listFiles();
            if (allFiles != null) {
                int totalFiles = allFiles.length;
                if (totalFiles > 10000) {
                    callback.update(98, "Database is large (" + totalFiles + " files). " +
                            "Consider archiving older emails.");
                }
            }

            callback.update(100, "Cleanup complete");

            return "Cleanup complete. Processed " + processedEmails + " emails in " +
                    processedFolders + " folders. Removed " + duplicatesRemoved +
                    " duplicates and " + emptyDirectoriesRemoved + " empty directories.";

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during cleanup", e);
            return "Error during cleanup: " + e.getMessage();
        }
    }
}