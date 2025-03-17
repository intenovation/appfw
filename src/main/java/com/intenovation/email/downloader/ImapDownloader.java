package com.intenovation.email.downloader;

import com.intenovation.appfw.systemtray.SystemTrayApp;
import com.intenovation.appfw.systemtray.AppConfig;
import com.intenovation.appfw.systemtray.Task;
import com.intenovation.appfw.systemtray.MenuCategory;
import com.intenovation.appfw.systemtray.CategoryBuilder;
import com.intenovation.appfw.systemtray.TaskBuilder;

import javax.mail.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.Preferences;

/**
 * A functional IMAP email downloader that uses javax.mail to connect to
 * an actual IMAP server and download emails into a structured file system.
 */
public class ImapDownloader {
    private static final Logger LOGGER = Logger.getLogger(ImapDownloader.class.getName());
    private static final String APP_NAME = "IMAP Email Downloader";
    private static final String VERSION = "1.0.0";
    
    // Default settings - these will be overridden by user preferences
    private static String imapHost = "";
    private static String imapPort = "993";
    private static String username = "";
    private static String password = "";
    private static boolean useSSL = true;
    private static String storagePath = System.getProperty("user.home") + File.separator + "EmailArchive";
    private static int syncIntervalMinutes = 30;
    private static int cleanupIntervalHours = 24;
    
    private static SystemTrayApp systemTrayApp;
    private static Preferences prefs;
    
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
    
    public static void main(String[] args) {
        try {
            // Load user preferences
            prefs = Preferences.userNodeForPackage(ImapDownloader.class);
            loadPreferences();
            
            if (imapHost.isEmpty() || username.isEmpty() || password.isEmpty()) {
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
     * Load user preferences
     */
    private static void loadPreferences() {
        imapHost = prefs.get("imapHost", imapHost);
        imapPort = prefs.get("imapPort", imapPort);
        username = prefs.get("username", username);
        password = prefs.get("password", password); // In a real app, use a more secure method
        useSSL = prefs.getBoolean("useSSL", useSSL);
        storagePath = prefs.get("storagePath", storagePath);
        syncIntervalMinutes = prefs.getInt("syncIntervalMinutes", syncIntervalMinutes);
        cleanupIntervalHours = prefs.getInt("cleanupIntervalHours", cleanupIntervalHours);
    }
    
    /**
     * Save user preferences
     */
    private static void savePreferences() {
        prefs.put("imapHost", imapHost);
        prefs.put("imapPort", imapPort);
        prefs.put("username", username);
        prefs.put("password", password); // In a real app, use a more secure method
        prefs.putBoolean("useSSL", useSSL);
        prefs.put("storagePath", storagePath);
        prefs.putInt("syncIntervalMinutes", syncIntervalMinutes);
        prefs.putInt("cleanupIntervalHours", cleanupIntervalHours);
    }
    
    /**
     * Create the application configuration
     * @return The app config
     */
    private static AppConfig createAppConfig() {
        return new AppConfig() {
            @Override
            public String getAppName() {
                return APP_NAME;
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
        
        // Email operations category
        categories.add(new CategoryBuilder("Email Operations")
            //.addAction("Sync All Emails Now", ImapDownloader::syncAllEmailsNow)
            .addAction("Sync New Emails Only", ImapDownloader::syncNewEmailsNow)
           // .addAction("Clean Up Now", ImapDownloader::cleanupNow)
            .build());
        
        // Settings category
        categories.add(new CategoryBuilder("Settings")
            .addAction("Configure IMAP Settings", ImapDownloader::showConfigDialog)
            .addAction("Configure Sync Schedule", ImapDownloader::showScheduleDialog)
            .addAction("Open Email Archive", ImapDownloader::openEmailArchive)
            .build());
        
        // Help category
        categories.add(new CategoryBuilder("Help")
            .addAction("Check Server Status", ImapDownloader::checkServerStatus)
            .addAction("View Storage Usage", ImapDownloader::showStorageUsage)
            .addAction("View Logs", ImapDownloader::openLogFile)
            .addAction("About", ImapDownloader::showAboutDialog)
            .build());
        
        return categories;
    }
    
    /**
     * Create the background tasks for the application
     * @return The tasks
     */
    private static List<Task> createTasks() {
        List<Task> tasks = new ArrayList<>();
        
        // Full email sync task
        tasks.add(new TaskBuilder("Full Email Sync")
            .withDescription("Downloads all emails from the IMAP server")
            .withIntervalSeconds(3600 * 12) // Every 12 hours by default
            .showInMenu(true)
            .withExecutor(EmailDownloader::downloadAllEmails)
            .build());
        
        // New email check task
        tasks.add(new TaskBuilder("New Emails Only")
            .withDescription("Downloads only new emails since last check")
            .withIntervalSeconds(syncIntervalMinutes * 60) // Using configured interval
            .showInMenu(true)
            .withExecutor(EmailDownloader::downloadNewEmails)
            .build());
        
        // Email cleanup task
        tasks.add(new TaskBuilder("Email Cleanup")
            .withDescription("Cleans up and organizes the email archive")
            .withIntervalSeconds(cleanupIntervalHours * 3600) // Using configured interval
            .showInMenu(true)
            .withExecutor(EmailCleanup::cleanupEmailArchive)
            .build());
        
        return tasks;
    }
    
    /**
     * Show the configuration dialog
     * @return true if configuration was saved, false if cancelled
     */
    private static boolean showConfigDialog() {
        JTextField hostField = new JTextField(imapHost, 30);
        JTextField portField = new JTextField(imapPort, 10);
        JTextField userField = new JTextField(username, 30);
        JPasswordField passField = new JPasswordField(password, 30);
        JCheckBox sslBox = new JCheckBox("Use SSL/TLS", useSSL);
        JTextField pathField = new JTextField(storagePath, 30);
        JButton browseButton = new JButton("Browse...");
        
        JPanel pathPanel = new JPanel();
        pathPanel.setLayout(new BoxLayout(pathPanel, BoxLayout.X_AXIS));
        pathPanel.add(pathField);
        pathPanel.add(browseButton);
        
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(new File(pathField.getText()));
            
            if (chooser.showDialog(null, "Select") == JFileChooser.APPROVE_OPTION) {
                pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        
        Object[] message = {
            "IMAP Server:", hostField,
            "Port:", portField,
            "Username:", userField,
            "Password:", passField,
            sslBox,
            "Storage Path:", pathPanel
        };
        
        int option = JOptionPane.showConfirmDialog(null, message, "IMAP Configuration", 
                                                  JOptionPane.OK_CANCEL_OPTION);
        
        if (option == JOptionPane.OK_OPTION) {
            // Validate input
            if (hostField.getText().trim().isEmpty() || userField.getText().trim().isEmpty() 
                || passField.getPassword().length == 0 || pathField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "All fields are required", "Validation Error", 
                                             JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            // Save configuration
            imapHost = hostField.getText().trim();
            imapPort = portField.getText().trim();
            username = userField.getText().trim();
            password = new String(passField.getPassword());
            useSSL = sslBox.isSelected();
            storagePath = pathField.getText().trim();
            
            savePreferences();
            
            // Create storage directory if it doesn't exist
            File storageDir = new File(storagePath);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Show the sync schedule configuration dialog
     */
    private static void showScheduleDialog() {
        String[] intervalOptions = {"5 minutes", "15 minutes", "30 minutes", "1 hour", "2 hours", "6 hours", "12 hours", "24 hours"};
        String[] cleanupOptions = {"Never", "Daily", "Weekly", "Monthly"};
        
        JComboBox<String> syncBox = new JComboBox<>(intervalOptions);
        JComboBox<String> cleanupBox = new JComboBox<>(cleanupOptions);
        
        // Set current values
        int syncIndex = 2; // Default to 30 minutes
        if (syncIntervalMinutes == 5) syncIndex = 0;
        else if (syncIntervalMinutes == 15) syncIndex = 1;
        else if (syncIntervalMinutes == 30) syncIndex = 2;
        else if (syncIntervalMinutes == 60) syncIndex = 3;
        else if (syncIntervalMinutes == 120) syncIndex = 4;
        else if (syncIntervalMinutes == 360) syncIndex = 5;
        else if (syncIntervalMinutes == 720) syncIndex = 6;
        else if (syncIntervalMinutes == 1440) syncIndex = 7;
        syncBox.setSelectedIndex(syncIndex);
        
        int cleanupIndex = 1; // Default to daily
        if (cleanupIntervalHours == 0) cleanupIndex = 0;
        else if (cleanupIntervalHours == 24) cleanupIndex = 1;
        else if (cleanupIntervalHours == 168) cleanupIndex = 2;
        else if (cleanupIntervalHours == 720) cleanupIndex = 3;
        cleanupBox.setSelectedIndex(cleanupIndex);
        
        Object[] message = {
            "Check for new emails every:", syncBox,
            "Clean up email archive:", cleanupBox
        };
        
        int option = JOptionPane.showConfirmDialog(null, message, "Schedule Configuration", 
                                                  JOptionPane.OK_CANCEL_OPTION);
        
        if (option == JOptionPane.OK_OPTION) {
            // Convert selection to minutes/hours
            int selectedSync = syncBox.getSelectedIndex();
            switch (selectedSync) {
                case 0: syncIntervalMinutes = 5; break;
                case 1: syncIntervalMinutes = 15; break;
                case 2: syncIntervalMinutes = 30; break;
                case 3: syncIntervalMinutes = 60; break;
                case 4: syncIntervalMinutes = 120; break;
                case 5: syncIntervalMinutes = 360; break;
                case 6: syncIntervalMinutes = 720; break;
                case 7: syncIntervalMinutes = 1440; break;
            }
            
            int selectedCleanup = cleanupBox.getSelectedIndex();
            switch (selectedCleanup) {
                case 0: cleanupIntervalHours = 0; break; // Never
                case 1: cleanupIntervalHours = 24; break; // Daily
                case 2: cleanupIntervalHours = 168; break; // Weekly
                case 3: cleanupIntervalHours = 720; break; // Monthly
            }
            
            savePreferences();
            SystemTrayApp.showMessage("Schedule Updated", 
                                    "Sync schedule has been updated. Changes will take effect after restart.");
        }
    }
    
    /**
     * Trigger a full email sync
     */
    private static void syncAllEmailsNow() {
        systemTrayApp.startTask("Full Email Sync");
    }
    
    /**
     * Trigger a new emails only sync
     */
    private static void syncNewEmailsNow() {
        systemTrayApp.startTask("New Emails Only");
    }
    
    /**
     * Trigger an email cleanup
     */
    private static void cleanupNow() {
        systemTrayApp.startTask("Email Cleanup");
    }
    
    /**
     * Open the email archive directory
     */
    private static void openEmailArchive() {
        File archiveDir = new File(storagePath);
        if (!archiveDir.exists()) {
            archiveDir.mkdirs();
        }
        
        if (!SystemTrayApp.openDirectory(archiveDir)) {
            SystemTrayApp.showError(
                "Error", 
                "Could not open email archive directory: " + storagePath
            );
        }
    }
    
    /**
     * Open the log file
     */
    private static void openLogFile() {
        File logFile = new File(System.getProperty("user.home"), ".imap-downloader/imap-downloader.log");
        if (!logFile.exists()) {
            SystemTrayApp.showMessage("Info", "No log file exists yet");
            return;
        }
        
        if (!SystemTrayApp.openDirectory(logFile.getParentFile())) {
            SystemTrayApp.showError("Error", "Could not open log directory");
        }
    }
    
    /**
     * Check the IMAP server status
     */
    private static void checkServerStatus() {
        SystemTrayApp.showMessage("Checking Server", "Connecting to server...");
        
        // Create a background thread to avoid freezing the UI
        new Thread(() -> {
            boolean isAvailable = false;
            String message = "Could not connect to server";
            
            try {
                // Set up connection properties
                Properties props = new Properties();
                props.put("mail.store.protocol", "imaps");
                props.put("mail.imaps.host", imapHost);
                props.put("mail.imaps.port", imapPort);
                props.put("mail.imaps.ssl.enable", String.valueOf(useSSL));
                props.put("mail.imaps.ssl.trust", "*");
                
                // Create the session
                Session session = Session.getInstance(props);
                
                // Connect to the server
                Store store = session.getStore("imaps");
                store.connect(imapHost, username, password);
                
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
            SwingUtilities.invokeLater(() -> {

               SystemTrayApp.showMessage("Server Status", finalMessage);

            });
        }).start();
    }
    
    /**
     * Show storage usage information
     */
    private static void showStorageUsage() {
        File archiveDir = new File(storagePath);
        if (!archiveDir.exists()) {
            SystemTrayApp.showMessage("Storage Info", "Email archive directory doesn't exist yet");
            return;
        }
        
        // Calculate storage in a background thread
        new Thread(() -> {
            long size = FileUtils.getFolderSize(archiveDir);
            String formattedSize = FileUtils.formatSize(size);
            
            int folderCount = FileUtils.countFolders(archiveDir);
            int emailCount = FileUtils.countEmails(archiveDir);
            
            final String message = "Email Archive Statistics:\n\n" +
                               "Storage Location: " + storagePath + "\n" +
                               "Total Size: " + formattedSize + "\n" +
                               "Folders: " + folderCount + "\n" +
                               "Emails: " + emailCount;
            
            SwingUtilities.invokeLater(() -> {
                SystemTrayApp.showMessage("Storage Usage", message);
            });
        }).start();
    }
    
    /**
     * Show the about dialog
     */
    private static void showAboutDialog() {
        SystemTrayApp.showMessage("About " + APP_NAME,
                              APP_NAME + " " + VERSION + "\n\n" +
                              "A utility to download and archive emails from an IMAP server.\n\n" +
                              "© " + Calendar.getInstance().get(Calendar.YEAR));
    }
    
    /**
     * Show a summary of the email archive status
     */
    private static void showStatusSummary() {
        File archiveDir = new File(storagePath);
        int folderCount = 0;
        int emailCount = 0;
        
        if (archiveDir.exists()) {
            folderCount = FileUtils.countFolders(archiveDir);
            emailCount = FileUtils.countEmails(archiveDir);
        }
        
        SystemTrayApp.showMessage(
            APP_NAME + " Status",
            "Email Downloader Status\n\n" +
            "Archive Location: " + storagePath + "\n" +
            "IMAP Server: " + imapHost + "\n" +
            "Account: " + username + "\n" +
            "Folders: " + folderCount + "\n" +
            "Emails: " + emailCount + "\n\n" +
            "Checking for new emails every " + syncIntervalMinutes + " minutes"
        );
    }
    
    // Getter methods for other classes to access settings
    public static String getImapHost() { return imapHost; }
    public static String getImapPort() { return imapPort; }
    public static String getUsername() { return username; }
    public static String getPassword() { return password; }
    public static boolean isUseSSL() { return useSSL; }
    public static String getStoragePath() { return storagePath; }
}