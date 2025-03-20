package com.intenovation.email.downloader;

import com.intenovation.appfw.config.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for email downloading
 */
public class EmailConfiguration implements ConfigurationDefinition {
    private static final Logger LOGGER = Logger.getLogger(EmailConfiguration.class.getName());
    private static final String CONFIG_FILE = System.getProperty("user.home") + File.separator + ".email-downloader.properties";

    // Configuration properties
    private String imapHost = "";
    private String imapPort = "993";
    private String username = "";
    private String password = "";
    private boolean useSSL = true;
    private File emailDirectory = new File(System.getProperty("user.home"), "EmailArchive");
    private int syncIntervalMinutes = 30;
    private int cleanupIntervalHours = 24;

    /**
     * Create a new EmailConfiguration
     */
    public EmailConfiguration() {
        loadConfiguration();
    }

    @Override
    public List<ConfigItem> getConfigItems() {
        List<ConfigItem> items = new ArrayList<>();
        
        // Server settings
        items.add(new TextConfigItem("imapHost", "IMAP Server", imapHost));
        items.add(new TextConfigItem("imapPort", "Port", imapPort));
        items.add(new TextConfigItem("username", "Username", username));
        items.add(new PasswordConfigItem("password", "Password", password));
        items.add(new CheckboxConfigItem("useSSL", "Use SSL/TLS", useSSL));
        
        // Directory settings
        items.add(new DirectoryConfigItem("emailDirectory", "Email Archive Directory", emailDirectory));
        
        // Schedule settings
        List<String> syncIntervals = new ArrayList<>();
        syncIntervals.add("5 minutes");
        syncIntervals.add("15 minutes");
        syncIntervals.add("30 minutes");
        syncIntervals.add("1 hour");
        syncIntervals.add("2 hours");
        syncIntervals.add("6 hours");
        syncIntervals.add("12 hours");
        syncIntervals.add("24 hours");
        
        List<String> cleanupIntervals = new ArrayList<>();
        cleanupIntervals.add("Never");
        cleanupIntervals.add("Daily");
        cleanupIntervals.add("Weekly");
        cleanupIntervals.add("Monthly");
        
        String syncInterval = getSyncIntervalDisplay();
        String cleanupInterval = getCleanupIntervalDisplay();
        
        items.add(new DropdownConfigItem("syncInterval", "Check for new emails every", syncInterval, syncIntervals));
        items.add(new DropdownConfigItem("cleanupInterval", "Clean up email archive", cleanupInterval, cleanupIntervals));
        
        return items;
    }

    @Override
    public void applyConfiguration(Map<String, Object> configValues) {
        this.imapHost = (String) configValues.get("imapHost");
        this.imapPort = (String) configValues.get("imapPort");
        this.username = (String) configValues.get("username");
        this.password = (String) configValues.get("password");
        
        if (configValues.get("useSSL") instanceof Boolean) {
            this.useSSL = (Boolean) configValues.get("useSSL");
        }
        
        if (configValues.get("emailDirectory") instanceof File) {
            this.emailDirectory = (File) configValues.get("emailDirectory");
        } else if (configValues.get("emailDirectory") instanceof String) {
            this.emailDirectory = new File((String) configValues.get("emailDirectory"));
        }
        
        // Parse sync interval
        String syncIntervalStr = (String) configValues.get("syncInterval");
        if (syncIntervalStr != null) {
            this.syncIntervalMinutes = parseSyncInterval(syncIntervalStr);
        }
        
        // Parse cleanup interval
        String cleanupIntervalStr = (String) configValues.get("cleanupInterval");
        if (cleanupIntervalStr != null) {
            this.cleanupIntervalHours = parseCleanupInterval(cleanupIntervalStr);
        }
        
        // Save to file
        saveConfiguration();
        
        // Create directory if it doesn't exist
        if (!emailDirectory.exists()) {
            emailDirectory.mkdirs();
        }
    }

    @Override
    public Map<String, Object> getCurrentValues() {
        Map<String, Object> values = new HashMap<>();
        values.put("imapHost", imapHost);
        values.put("imapPort", imapPort);
        values.put("username", username);
        values.put("password", password);
        values.put("useSSL", useSSL);
        values.put("emailDirectory", emailDirectory);
        values.put("syncInterval", getSyncIntervalDisplay());
        values.put("cleanupInterval", getCleanupIntervalDisplay());
        return values;
    }
    
    // Helper method to load configuration from file
    private void loadConfiguration() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);
        
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                
                // Load properties
                imapHost = props.getProperty("imapHost", imapHost);
                imapPort = props.getProperty("imapPort", imapPort);
                username = props.getProperty("username", username);
                password = props.getProperty("password", password);
                useSSL = Boolean.parseBoolean(props.getProperty("useSSL", String.valueOf(useSSL)));
                
                String emailDirStr = props.getProperty("emailDirectory");
                if (emailDirStr != null && !emailDirStr.isEmpty()) {
                    emailDirectory = new File(emailDirStr);
                }
                
                String syncIntervalStr = props.getProperty("syncIntervalMinutes");
                if (syncIntervalStr != null && !syncIntervalStr.isEmpty()) {
                    try {
                        syncIntervalMinutes = Integer.parseInt(syncIntervalStr);
                    } catch (NumberFormatException e) {
                        LOGGER.log(Level.WARNING, "Invalid sync interval in config: " + syncIntervalStr, e);
                    }
                }
                
                String cleanupIntervalStr = props.getProperty("cleanupIntervalHours");
                if (cleanupIntervalStr != null && !cleanupIntervalStr.isEmpty()) {
                    try {
                        cleanupIntervalHours = Integer.parseInt(cleanupIntervalStr);
                    } catch (NumberFormatException e) {
                        LOGGER.log(Level.WARNING, "Invalid cleanup interval in config: " + cleanupIntervalStr, e);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error loading configuration", e);
            }
        }
    }
    
    // Helper method to save configuration to file
    private void saveConfiguration() {
        Properties props = new Properties();
        
        props.setProperty("imapHost", imapHost);
        props.setProperty("imapPort", imapPort);
        props.setProperty("username", username);
        props.setProperty("password", password);
        props.setProperty("useSSL", String.valueOf(useSSL));
        props.setProperty("emailDirectory", emailDirectory.getAbsolutePath());
        props.setProperty("syncIntervalMinutes", String.valueOf(syncIntervalMinutes));
        props.setProperty("cleanupIntervalHours", String.valueOf(cleanupIntervalHours));
        
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Email Downloader Configuration");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving configuration", e);
        }
    }
    
    // Helper method to parse sync interval from display string
    private int parseSyncInterval(String syncIntervalStr) {
        switch (syncIntervalStr) {
            case "5 minutes": return 5;
            case "15 minutes": return 15;
            case "30 minutes": return 30;
            case "1 hour": return 60;
            case "2 hours": return 120;
            case "6 hours": return 360;
            case "12 hours": return 720;
            case "24 hours": return 1440;
            default: return 30;
        }
    }
    
    // Helper method to parse cleanup interval from display string
    private int parseCleanupInterval(String cleanupIntervalStr) {
        switch (cleanupIntervalStr) {
            case "Never": return 0;
            case "Daily": return 24;
            case "Weekly": return 168;
            case "Monthly": return 720;
            default: return 24;
        }
    }
    
    // Helper method to get sync interval display string
    private String getSyncIntervalDisplay() {
        if (syncIntervalMinutes == 5) return "5 minutes";
        if (syncIntervalMinutes == 15) return "15 minutes";
        if (syncIntervalMinutes == 30) return "30 minutes";
        if (syncIntervalMinutes == 60) return "1 hour";
        if (syncIntervalMinutes == 120) return "2 hours";
        if (syncIntervalMinutes == 360) return "6 hours";
        if (syncIntervalMinutes == 720) return "12 hours";
        if (syncIntervalMinutes == 1440) return "24 hours";
        return "30 minutes";
    }
    
    // Helper method to get cleanup interval display string
    private String getCleanupIntervalDisplay() {
        if (cleanupIntervalHours == 0) return "Never";
        if (cleanupIntervalHours == 24) return "Daily";
        if (cleanupIntervalHours == 168) return "Weekly";
        if (cleanupIntervalHours == 720) return "Monthly";
        return "Daily";
    }
    
    // Getters for business logic
    public String getImapHost() { return imapHost; }
    public String getImapPort() { return imapPort; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean isUseSSL() { return useSSL; }
    public File getEmailDirectory() { return emailDirectory; }
    public String getStoragePath() { return emailDirectory.getAbsolutePath(); }
    public int getSyncIntervalMinutes() { return syncIntervalMinutes; }
    public int getCleanupIntervalHours() { return cleanupIntervalHours; }
}