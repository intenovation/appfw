package com.intenovation.email.downloader;

import com.intenovation.appfw.systemtray.*;
import com.intenovation.appfw.ui.UIService;

import javax.mail.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * A functional IMAP email downloader that uses javax.mail to connect to
 * an actual IMAP server and download emails into a structured file system.
 */
public class ImapDownloader {
    private static final Logger LOGGER = Logger.getLogger(ImapDownloader.class.getName());
    private static final String APP_NAME = "IMAP Email Downloader";
    private static final String VERSION = "1.0.0";

    private final EmailConfiguration config;
    private final UIService uiService;
    private SystemTrayApp systemTrayApp;

    static {
        // Configure logger
        try {
            File logDir = new File(System.getProperty("user.home"), ".imap-downloader");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            FileHandler fileHandler = new FileHandler(logDir + File.separator + "imap-downloader.log", 1048576, 5, true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Failed to configure logger: " + e.getMessage());
        }
    }

    /**
     * Create a new ImapDownloader
     * 
     * @param config Email configuration
     * @param uiService UI service
     */
    public ImapDownloader(EmailConfiguration config, UIService uiService) {
        this.config = config;
        this.uiService = uiService;
    }

    /**
     * Initialize the system tray application
     */
    public void initialize() {
        try {
            if (config.getImapHost().isEmpty() || config.getUsername().isEmpty() || config.getPassword().isEmpty()) {
                // If not configured, show configuration dialog first
                if (!showConfigDialog()) {
                    LOGGER.warning("Initial configuration cancelled. Exiting application.");
                    return;
                }
            }

            // Define application configuration
            AppConfig appConfig = createAppConfig();

            // Create menu categories
            List<MenuCategory> menuCategories = createMenuCategories();

            // Create background tasks
            List<BackgroundTask> tasks = createTasks();

            // Initialize the system tray application
            systemTrayApp = new SystemTrayApp(appConfig, menuCategories, tasks);
            LOGGER.info("IMAP Email Downloader started");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            uiService.showError("Error", "Failed to start application: " + e.getMessage());
        }
    }

    /**
     * Create the application configuration
     * @return The app config
     */
    private AppConfig createAppConfig() {
        return new AppConfig() {
            @Override
            public String getAppName() {
                return APP_NAME;
            }

            @Override
            public String getIconPath() {
                return "/intenovation.png";  // Icon in resources folder
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
    private List<MenuCategory> createMenuCategories() {
        List<MenuCategory> categories = new ArrayList<>();

        // Email operations category
        categories.add(new CategoryBuilder("Email Operations")
                .addAction("Sync New Emails Only", this::syncNewEmailsNow)
                .build());

        // Settings category
        categories.add(new CategoryBuilder("Settings")
                .addAction("Configure IMAP Settings", this::showConfigDialog)
                .addAction("Open Email Archive", this::openEmailArchive)
                .build());

        // Help category
        categories.add(new CategoryBuilder("Help")
                .addAction("Check Server Status", this::checkServerStatus)
                .addAction("View Storage Usage", this::showStorageUsage)
                .addAction("View Logs", this::openLogFile)
                .addAction("About", this::showAboutDialog)
                .build());

        return categories;
    }

    /**
     * Create the background tasks for the application
     * @return The tasks
     */
    private List<BackgroundTask> createTasks() {
        List<BackgroundTask> tasks = new ArrayList<>();

        // Full email sync task
        tasks.add(new EmailDownloader());

        // New email check task
        tasks.add(new EmailDownloader(config.getSyncIntervalMinutes()));

        // Email cleanup task
        tasks.add(new EmailCleanup(config.getCleanupIntervalHours()));

        return tasks;
    }

    /**
     * Show the configuration dialog
     * @return true if configuration was saved, false if cancelled
     */
    public boolean showConfigDialog() {
        return uiService.showConfigDialog("IMAP Configuration", config);
    }

    /**
     * Trigger a full email sync
     */
    private void syncAllEmailsNow() {
        if (systemTrayApp != null) {
            systemTrayApp.startTask("Full Email Sync");
        }
    }

    /**
     * Trigger a new emails only sync
     */
    private void syncNewEmailsNow() {
        if (systemTrayApp != null) {
            systemTrayApp.startTask("New Emails Only");
        }
    }

    /**
     * Trigger an email cleanup
     */
    private void cleanupNow() {
        if (systemTrayApp != null) {
            systemTrayApp.startTask("Email Cleanup");
        }
    }

    /**
     * Open the email archive directory
     */
    private void openEmailArchive() {
        File archiveDir = config.getEmailDirectory();
        if (!archiveDir.exists()) {
            archiveDir.mkdirs();
        }

        if (!uiService.openDirectory(archiveDir)) {
            uiService.showError("Error", "Could not open email archive directory: " + archiveDir.getAbsolutePath());
        }
    }

    /**
     * Open the log file
     */
    private void openLogFile() {
        File logFile = new File(System.getProperty("user.home"), ".imap-downloader/imap-downloader.log");
        if (!logFile.exists()) {
            uiService.showInfo("Info", "No log file exists yet");
            return;
        }

        if (!uiService.openDirectory(logFile.getParentFile())) {
            uiService.showError("Error", "Could not open log directory");
        }
    }

    /**
     * Check the IMAP server status
     */
    private void checkServerStatus() {
        uiService.showInfo("Checking Server", "Connecting to server...");

        // Create a background thread to avoid freezing the UI
        new Thread(() -> {
            boolean isAvailable = false;
            String message = "Could not connect to server";

            try {
                // Set up connection properties
                Properties props = new Properties();
                props.put("mail.store.protocol", "imaps");
                props.put("mail.imaps.host", config.getImapHost());
                props.put("mail.imaps.port", config.getImapPort());
                props.put("mail.imaps.ssl.enable", String.valueOf(config.isUseSSL()));
                props.put("mail.imaps.ssl.trust", "*");

                // Create the session
                Session session = Session.getInstance(props);

                // Connect to the server
                Store store = session.getStore("imaps");
                store.connect(config.getImapHost(), config.getUsername(), config.getPassword());

                // Check if we have at least one folder
                Folder defaultFolder = store.getDefaultFolder();
                if (defaultFolder != null) {
                    isAvailable = true;
                    message = "Server is available. Connected successfully.";
                }

                // Close the connection
                store.close();
            } catch (NoSuchProviderException e) {
                message = "IMAP provider error: " + e.getMessage();
                LOGGER.log(Level.WARNING, "IMAP provider error", e);
            } catch (MessagingException e) {
                message = "Connection error: " + e.getMessage();
                LOGGER.log(Level.WARNING, "Connection error", e);
            }

            final String finalMessage = message;
            uiService.showInfo("Server Status", finalMessage);
        }).start();
    }

    /**
     * Show storage usage information
     */
    private void showStorageUsage() {
        File archiveDir = config.getEmailDirectory();
        if (!archiveDir.exists()) {
            uiService.showInfo("Storage Info", "Email archive directory doesn't exist yet");
            return;
        }

        // Calculate storage in a background thread
        new Thread(() -> {
            long size = FileUtils.getFolderSize(archiveDir);
            String formattedSize = FileUtils.formatSize(size);

            int folderCount = FileUtils.countFolders(archiveDir);
            int emailCount = FileUtils.countEmails(archiveDir);

            final String message = "Email Archive Statistics:\n\n" +
                    "Storage Location: " + archiveDir.getAbsolutePath() + "\n" +
                    "Total Size: " + formattedSize + "\n" +
                    "Folders: " + folderCount + "\n" +
                    "Emails: " + emailCount;

            uiService.showInfo("Storage Usage", message);
        }).start();
    }

    /**
     * Show the about dialog
     */
    private void showAboutDialog() {
        uiService.showInfo("About " + APP_NAME,
                APP_NAME + " " + VERSION + "\n\n" +
                        "A utility to download and archive emails from an IMAP server.\n\n" +
                        "© " + Calendar.getInstance().get(Calendar.YEAR));
    }

    /**
     * Show a summary of the email archive status
     */
    private void showStatusSummary() {
        File archiveDir = config.getEmailDirectory();
        int folderCount = 0;
        int emailCount = 0;

        if (archiveDir.exists()) {
            folderCount = FileUtils.countFolders(archiveDir);
            emailCount = FileUtils.countEmails(archiveDir);
        }

        uiService.showInfo(
                APP_NAME + " Status",
                "Email Downloader Status\n\n" +
                        "Archive Location: " + archiveDir.getAbsolutePath() + "\n" +
                        "IMAP Server: " + config.getImapHost() + "\n" +
                        "Account: " + config.getUsername() + "\n" +
                        "Folders: " + folderCount + "\n" +
                        "Emails: " + emailCount + "\n\n" +
                        "Checking for new emails every " + config.getSyncIntervalMinutes() + " minutes"
        );
    }

    // Static accessors required by the existing code
    // These will be used by other classes during migration
    public static String getImapHost() { 
        return ImapDownloaderInstance.getInstance().config.getImapHost(); 
    }
    
    public static String getImapPort() { 
        return ImapDownloaderInstance.getInstance().config.getImapPort(); 
    }
    
    public static String getUsername() { 
        return ImapDownloaderInstance.getInstance().config.getUsername(); 
    }
    
    public static String getPassword() { 
        return ImapDownloaderInstance.getInstance().config.getPassword(); 
    }
    
    public static boolean isUseSSL() { 
        return ImapDownloaderInstance.getInstance().config.isUseSSL(); 
    }
    
    public static String getStoragePath() { 
        return ImapDownloaderInstance.getInstance().config.getStoragePath(); 
    }
    
    /**
     * Singleton holder for migration
     */
    private static class ImapDownloaderInstance {
        private static ImapDownloader INSTANCE;
        
        public static ImapDownloader getInstance() {
            if (INSTANCE == null) {
                // Create a temporary instance with default configuration
                EmailConfiguration config = new EmailConfiguration();
                INSTANCE = new ImapDownloader(config, null);
            }
            return INSTANCE;
        }
        
        public static void setInstance(ImapDownloader instance) {
            INSTANCE = instance;
        }
    }
}