package com.intenovation.appfw.systemtrayexample.email;

import com.intenovation.appfw.systemtray.SystemTrayApp;
import com.intenovation.appfw.systemtray.AppConfig;
import com.intenovation.appfw.systemtray.Task;
import com.intenovation.appfw.systemtray.MenuCategory;
import com.intenovation.appfw.systemtray.CategoryBuilder;
import com.intenovation.appfw.systemtray.TaskBuilder;
import com.intenovation.email.downloader.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An IMAP email downloader application that uses the restructured SystemTrayApp framework.
 * This application downloads emails from an IMAP server and organizes them in a local file system.
 */
public class ImapEmailDownloader {
    private static final Logger LOGGER = Logger.getLogger(ImapEmailDownloader.class.getName());

    // Configuration parameters - in a real app, these would be loaded from a config file
    private static final String EMAIL_HOST = "imap.example.com";
    private static final String EMAIL_USERNAME = "username@example.com";
    private static final String EMAIL_PASSWORD = "password";  // In a real app, use secure password storage
    private static final String BASE_STORAGE_PATH = System.getProperty("user.home") + File.separator + "EmailArchive";

    // The system tray app instance
    private static SystemTrayApp systemTrayApp;

    public static void main(String[] args) {
        try {
            // Define application configuration
            AppConfig appConfig = createAppConfig();

            // Create menu categories
            List<MenuCategory> menuCategories = createMenuCategories();

            // Create background tasks
            List<Task> tasks = createTasks();

            // Initialize the system tray application
            systemTrayApp = new SystemTrayApp(appConfig, menuCategories, tasks);
            LOGGER.info("IMAP Email Downloader started");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            SystemTrayApp.showError("Error", "Failed to start application: " + e.getMessage());
        }
    }

    /**
     * Create the application configuration
     * @return The app config
     */
    private static AppConfig createAppConfig() {
        return new AppConfig() {
            @Override
            public String getAppName() {
                return "IMAP Email Downloader";
            }

            @Override
            public String getIconPath() {
                return "/intenovation.png";  // Place this icon in your resources folder
            }

            @Override
            public void onIconDoubleClick() {
                showStatusSummary();
            }
        };
    }

    /**
     * Create the menu categories for the application
     * @return The menu categories
     */
    private static List<MenuCategory> createMenuCategories() {
        List<MenuCategory> categories = new ArrayList<>();

        // Email Operations category
        categories.add(new CategoryBuilder("Email Operations")
                .addAction("Configure IMAP Settings", ImapEmailDownloader::showConfigDialog)
                .addAction("Open Email Archive", ImapEmailDownloader::openEmailArchive)
                .build());

        // Tools category
        categories.add(new CategoryBuilder("Tools")
                .addAction("Check Server Status", ImapEmailDownloader::checkServerStatus)
                .addAction("View Storage Usage", ImapEmailDownloader::showStorageUsage)
                .build());

        return categories;
    }

    /**
     * Create the background tasks for the application
     * @return The tasks
     */
    private static List<Task> createTasks() {
        List<Task> tasks = new ArrayList<>();

        // Full mailbox sync task
        tasks.add(new TaskBuilder("Full Email Sync")
                .withDescription("Downloads all emails from the IMAP server")
                .withIntervalSeconds(3600 * 4) // Every 4 hours
                .showInMenu(true)
                .withExecutor(ImapEmailDownloader::syncEmailsFromImap)
                .build());

        // New mail check task
        tasks.add(new TaskBuilder("New Emails Only")
                .withDescription("Downloads only new emails since last check")
                .withIntervalSeconds(600) // Every 10 minutes
                .showInMenu(true)
                .withExecutor((progress, status) -> syncEmailsFromImap(progress, status, true))
                .build());

        // Email cleanup task
        tasks.add(new TaskBuilder("Email Cleanup")
                .withDescription("Cleans up and organizes the email archive")
                .withIntervalSeconds(3600 * 24) // Once per day
                .showInMenu(true)
                .withExecutor(ImapEmailDownloader::cleanupEmailArchive)
                .build());

        return tasks;
    }

    /**
     * Show the configuration dialog
     */
    private static void showConfigDialog() {
        SystemTrayApp.showMessage(
                "IMAP Configuration",
                "This would open a configuration dialog in a real application.\n" +
                        "Currently using host: " + EMAIL_HOST + "\n" +
                        "Username: " + EMAIL_USERNAME
        );
    }

    /**
     * Open the email archive directory
     */
    private static void openEmailArchive() {
        File archiveDir = new File(BASE_STORAGE_PATH);
        if (!archiveDir.exists()) {
            archiveDir.mkdirs();
        }

        if (!SystemTrayApp.openDirectory(archiveDir)) {
            SystemTrayApp.showError(
                    "Error",
                    "Could not open email archive directory: " + BASE_STORAGE_PATH
            );
        }
    }

    /**
     * Check the IMAP server status
     */
    private static void checkServerStatus() {
        // This would actually check the server connection in a real app
        boolean isAvailable = new Random().nextInt(10) < 9;  // 90% chance of being available

        if (isAvailable) {
            SystemTrayApp.showMessage(
                    "Server Status",
                    "IMAP Server is available and responding."
            );
        } else {
            SystemTrayApp.showWarning(
                    "Server Status",
                    "IMAP Server is not responding. Please check your connection."
            );
        }
    }

    /**
     * Show storage usage information
     */
    private static void showStorageUsage() {
        File archiveDir = new File(BASE_STORAGE_PATH);
        long size = getFolderSize(archiveDir);
        String formattedSize = formatSize(size);

        SystemTrayApp.showMessage(
                "Storage Usage",
                "Email Archive Size: " + formattedSize + "\n" +
                        "Location: " + BASE_STORAGE_PATH
        );
    }

    /**
     * Show a summary of the email archive status
     */
    private static void showStatusSummary() {
        File archiveDir = new File(BASE_STORAGE_PATH);
        int folderCount = 0;
        int emailCount = 0;

        if (archiveDir.exists()) {
            folderCount = countFolders(archiveDir);
            emailCount = countEmails(archiveDir);
        }

        SystemTrayApp.showMessage(
                "Email Downloader Status",
                "IMAP Email Downloader Status\n\n" +
                        "Archive Location: " + BASE_STORAGE_PATH + "\n" +
                        "Folders: " + folderCount + "\n" +
                        "Emails: " + emailCount + "\n" +
                        "Server: " + EMAIL_HOST + "\n" +
                        "Account: " + EMAIL_USERNAME
        );
    }

    /**
     * Sync emails from IMAP server and save them to local storage
     *
     * @param progressUpdater Function to report progress
     * @param statusUpdater Function to report status
     * @return Status message
     * @throws InterruptedException if task is cancelled
     */
    private static String syncEmailsFromImap(Consumer<Integer> progressUpdater, Consumer<String> statusUpdater)
            throws InterruptedException {
        // Call the overloaded method with newOnly = false
        return syncEmailsFromImap(progressUpdater, statusUpdater, false);
    }

    /**
     * Sync emails from IMAP server and save them to local storage
     *
     * @param progressUpdater Function to report progress
     * @param statusUpdater Function to report status
     * @param newOnly Whether to download only new emails
     * @return Status message
     * @throws InterruptedException if task is cancelled
     */
    private static String syncEmailsFromImap(Consumer<Integer> progressUpdater, Consumer<String> statusUpdater,
                                             boolean newOnly) throws InterruptedException {
        statusUpdater.accept("Connecting to IMAP server...");
        progressUpdater.accept(0);

        // Create the base directory if it doesn't exist
        File baseDir = new File(BASE_STORAGE_PATH);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        // In a simulation, sleep to represent connection time
        Thread.sleep(1500);

        // Check for interruption
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Task cancelled");
        }

        // Set up JavaMail session and store (in real app)
        // Properties props = new Properties();
        // props.setProperty("mail.store.protocol", "imaps");
        // Session session = Session.getInstance(props, null);

        try {
            // In a real application, we would connect to the actual IMAP server:
            // Store store = session.getStore("imaps");
            // store.connect(EMAIL_HOST, EMAIL_USERNAME, EMAIL_PASSWORD);

            // Instead, we'll simulate the IMAP operations
            List<FolderInfo> folders = simulateGetFolders();

            statusUpdater.accept("Found " + folders.size() + " folders");
            progressUpdater.accept(10);

            int totalFolders = folders.size();
            int processedFolders = 0;
            int totalEmails = 0;
            int downloadedEmails = 0;

            // Process each folder
            for (FolderInfo folder : folders) {
                // Check for interruption
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Task cancelled");
                }

                statusUpdater.accept("Processing folder: " + folder.name);

                // Create folder directory
                String folderPath = BASE_STORAGE_PATH + File.separator +
                        sanitizeFolderName(folder.name);
                File folderDir = new File(folderPath);
                if (!folderDir.exists()) {
                    folderDir.mkdirs();
                }

                // Get messages in this folder
                List<MessageInfo> messages = simulateGetMessages(folder.name, newOnly);
                totalEmails += messages.size();

                // Process each message
                for (MessageInfo msg : messages) {
                    // Check for interruption
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Task cancelled");
                    }

                    // Create message directory using message ID
                    String msgDirName = sanitizeFileName(msg.messageId);
                    File msgDir = new File(folderDir, msgDirName);

                    // Skip if this message already exists and we're only getting new messages
                    if (newOnly && msgDir.exists()) {
                        continue;
                    }

                    if (!msgDir.exists()) {
                        msgDir.mkdirs();
                    }

                    // Save message content
                    saveMessageContent(msgDir, msg);

                    // Save message properties
                    saveMessageProperties(msgDir, msg);

                    // Save attachments
                    for (AttachmentInfo attachment : msg.attachments) {
                        saveAttachment(msgDir, attachment);
                    }

                    downloadedEmails++;

                    // Update progress based on processed emails
                    int emailProgress = 10 + (80 * downloadedEmails / Math.max(1, totalEmails));
                    progressUpdater.accept(Math.min(90, emailProgress));

                    // Update status message periodically
                    if (downloadedEmails % 5 == 0 || downloadedEmails == totalEmails) {
                        statusUpdater.accept("Downloaded " + downloadedEmails +
                                " emails from folder " + folder.name);
                    }

                    // Simulate some network delay
                    Thread.sleep(200);
                }

                processedFolders++;
                int folderProgress = 10 + (80 * processedFolders / totalFolders);
                progressUpdater.accept(Math.min(90, folderProgress));
            }

            // Finalizing
            statusUpdater.accept("Finalizing download...");
            progressUpdater.accept(95);
            Thread.sleep(500);

            progressUpdater.accept(100);

            return "Download complete. Processed " + totalFolders + " folders, downloaded " +
                    downloadedEmails + " emails.";

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error syncing emails", e);
            return "Error during email sync: " + e.getMessage();
        }
    }

    /**
     * Clean up and organize the email archive
     *
     * @param progressUpdater Function to report progress
     * @param statusUpdater Function to report status
     * @return Status message
     * @throws InterruptedException if task is cancelled
     */
    private static String cleanupEmailArchive(Consumer<Integer> progressUpdater, Consumer<String> statusUpdater)
            throws InterruptedException {
        statusUpdater.accept("Starting email archive cleanup...");
        progressUpdater.accept(0);

        File baseDir = new File(BASE_STORAGE_PATH);
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
            int reorganizedEmails = 0;
            int removedDuplicates = 0;

            statusUpdater.accept("Found " + totalFolders + " folders to process");
            progressUpdater.accept(5);

            // Process each folder
            for (File folder : folders) {
                // Check for interruption
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Task cancelled");
                }

                statusUpdater.accept("Cleaning folder: " + folder.getName());

                // Get all message directories
                File[] messageDirs = folder.listFiles(File::isDirectory);
                if (messageDirs != null) {
                    // Find and remove duplicate messages (in a real app, this would be more sophisticated)
                    Set<String> messageHashes = new HashSet<>();

                    for (File messageDir : messageDirs) {
                        // Check for interruption
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException("Task cancelled");
                        }

                        // Compute a simple "hash" for the message (this is simplified)
                        // In a real app, we would use a proper hash of the content
                        String simpleHash = messageDir.getName();

                        // Check if this is a duplicate
                        if (messageHashes.contains(simpleHash)) {
                            // In a real app, you'd want to be certain before deleting anything
                            // Here we'll just simulate removal
                            removedDuplicates++;
                            // deleteDirectory(messageDir);  // Comment out for safety in this example
                        } else {
                            messageHashes.add(simpleHash);
                            reorganizedEmails++;
                        }

                        // Simulate some processing time
                        Thread.sleep(50);
                    }
                }

                processedFolders++;
                int progress = 5 + (90 * processedFolders / totalFolders);
                progressUpdater.accept(progress);

                // Update status periodically
                statusUpdater.accept("Processed " + processedFolders + " of " +
                        totalFolders + " folders. Found " +
                        removedDuplicates + " duplicates.");
            }

            // Finalizing
            statusUpdater.accept("Finalizing cleanup...");
            progressUpdater.accept(95);
            Thread.sleep(1000);

            progressUpdater.accept(100);

            return "Cleanup complete. Processed " + reorganizedEmails +
                    " emails. Found " + removedDuplicates + " potential duplicates.";

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during cleanup", e);
            return "Error during cleanup: " + e.getMessage();
        }
    }

    /**
     * Simulate getting folders from IMAP server
     * In a real application, this would connect to the actual IMAP server
     *
     * @return List of folder information
     */
    private static List<FolderInfo> simulateGetFolders() {
        List<FolderInfo> folders = new ArrayList<>();

        // Add some common folders
        folders.add(new FolderInfo("INBOX", 25));
        folders.add(new FolderInfo("Sent", 15));
        folders.add(new FolderInfo("Drafts", 5));
        folders.add(new FolderInfo("Spam", 10));
        folders.add(new FolderInfo("Trash", 8));
        folders.add(new FolderInfo("Work", 20));
        folders.add(new FolderInfo("Personal", 12));

        return folders;
    }

    /**
     * Simulate getting messages from a folder
     * In a real application, this would fetch actual messages from the IMAP folder
     *
     * @param folderName The folder name
     * @param newOnly Whether to get only new messages
     * @return List of message information
     */
    private static List<MessageInfo> simulateGetMessages(String folderName, boolean newOnly) {
        List<MessageInfo> messages = new ArrayList<>();

        // Generate a random number of messages for this folder
        Random rand = new Random();
        int messageCount = rand.nextInt(10) + 1;  // 1-10 messages

        // If we're only getting new messages, simulate fewer messages
        if (newOnly) {
            messageCount = Math.max(1, messageCount / 3);
        }

        // Generate some sample messages
        for (int i = 0; i < messageCount; i++) {
            String messageId = UUID.randomUUID().toString();

            MessageInfo msg = new MessageInfo();
            msg.messageId = messageId;
            msg.subject = "Sample " + folderName + " message " + (i + 1);
            msg.sender = "sender" + rand.nextInt(5) + "@example.com";
            msg.recipients = Arrays.asList("recipient@example.com");
            msg.sentDate = new Date();
            msg.content = "This is the content of message " + messageId + " in folder " + folderName + ".\n\n" +
                    "This is a sample message generated for demonstration purposes.";

            // Some messages have attachments
            if (rand.nextBoolean()) {
                int attachmentCount = rand.nextInt(3) + 1;  // 1-3 attachments
                for (int j = 0; j < attachmentCount; j++) {
                    AttachmentInfo attachment = new AttachmentInfo();
                    attachment.filename = "attachment" + (j + 1) + ".txt";
                    attachment.content = "This is the content of attachment " + (j + 1) +
                            " for message " + messageId + ".";
                    msg.attachments.add(attachment);
                }
            }

            messages.add(msg);
        }

        return messages;
    }

    /**
     * Save message content to a file
     *
     * @param msgDir The message directory
     * @param msg The message information
     * @throws IOException If an error occurs
     */
    private static void saveMessageContent(File msgDir, MessageInfo msg) throws IOException {
        File contentFile = new File(msgDir, "content.txt");
        Files.write(contentFile.toPath(), msg.content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Save message properties to a file
     *
     * @param msgDir The message directory
     * @param msg The message information
     * @throws IOException If an error occurs
     */
    private static void saveMessageProperties(File msgDir, MessageInfo msg) throws IOException {
        File propsFile = new File(msgDir, "message.properties");

        Properties props = new Properties();

        // Message ID
        String messageId =  msg.messageId;
        if (messageId != null) {
            props.setProperty("message.id", messageId);
            // Also store the sanitized version that was used for the directory name
            props.setProperty("message.id.folder", FileUtils.sanitizeFileName(messageId));
        }
        props.setProperty("subject", msg.subject);
        props.setProperty("sender", msg.sender);
        props.setProperty("recipients", String.join(", ", msg.recipients));
        props.setProperty("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(msg.sentDate));
        props.setProperty("has.attachments", String.valueOf(!msg.attachments.isEmpty()));
        props.setProperty("attachment.count", String.valueOf(msg.attachments.size()));

        try (FileOutputStream out = new FileOutputStream(propsFile)) {
            props.store(out, "Email Message Properties");
        }
    }

    /**
     * Save an attachment to a file
     *
     * @param msgDir The message directory
     * @param attachment The attachment information
     * @throws IOException If an error occurs
     */
    private static void saveAttachment(File msgDir, AttachmentInfo attachment) throws IOException {
        File attachmentFile = new File(msgDir, sanitizeFileName(attachment.filename));
        Files.write(attachmentFile.toPath(), attachment.content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get the size of a folder and its contents
     *
     * @param folder The folder to check
     * @return The size in bytes
     */
    private static long getFolderSize(File folder) {
        if (folder == null || !folder.exists()) {
            return 0;
        }

        long size = 0;

        // Add size of all files in the folder
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += getFolderSize(file);
                }
            }
        }

        return size;
    }

    /**
     * Format a size in bytes to a human-readable string
     *
     * @param bytes The size in bytes
     * @return A formatted string (e.g., "1.23 MB")
     */
    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Count the number of folders in the email archive
     *
     * @param baseDir The base directory
     * @return The number of folders
     */
    private static int countFolders(File baseDir) {
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return 0;
        }

        return (int) Arrays.stream(baseDir.listFiles())
                .filter(File::isDirectory)
                .count();
    }

    /**
     * Count the number of emails in the email archive
     *
     * @param baseDir The base directory
     * @return The number of emails
     */
    private static int countEmails(File baseDir) {
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return 0;
        }

        AtomicInteger count = new AtomicInteger(0);

        File[] folders = baseDir.listFiles(File::isDirectory);
        if (folders != null) {
            for (File folder : folders) {
                File[] messages = folder.listFiles(File::isDirectory);
                if (messages != null) {
                    count.addAndGet(messages.length);
                }
            }
        }

        return count.get();
    }

    /**
     * Sanitize a folder name for use in a file path
     *
     * @param folderName The folder name
     * @return A sanitized folder name
     */
    private static String sanitizeFolderName(String folderName) {
        // Replace invalid path characters
        return folderName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * Sanitize a filename for use in a file path
     *
     * @param filename The filename
     * @return A sanitized filename
     */
    private static String sanitizeFileName(String filename) {
        // Replace invalid path characters
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * Recursively delete a directory and its contents
     *
     * @param directory The directory to delete
     * @return true if successful
     */
    private static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }

    // Model classes for simulating IMAP data

    /**
     * Information about an IMAP folder
     */
    private static class FolderInfo {
        String name;
        int messageCount;

        FolderInfo(String name, int messageCount) {
            this.name = name;
            this.messageCount = messageCount;
        }
    }

    /**
     * Information about an email message
     */
    private static class MessageInfo {
        String messageId;
        String subject;
        String sender;
        List<String> recipients = new ArrayList<>();
        Date sentDate;
        String content;
        List<AttachmentInfo> attachments = new ArrayList<>();
    }

    /**
     * Information about an email attachment
     */
    private static class AttachmentInfo {
        String filename;
        String content;
    }
}