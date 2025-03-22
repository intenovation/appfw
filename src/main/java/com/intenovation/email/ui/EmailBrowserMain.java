package com.intenovation.email.ui;

import com.intenovation.appfw.systemtray.*;
import com.intenovation.appfw.ui.SwingUIService;
import com.intenovation.appfw.ui.UIService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class for launching the Email Browser application.
 * This can be run standalone or integrated with the existing Intenovation Suite.
 */
public class EmailBrowserMain {
    private static final Logger LOGGER = Logger.getLogger(EmailBrowserMain.class.getName());
    private static final String APP_NAME = "Email Browser";

    /**
     * Main entry point for standalone mode
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command line args
            File emailDirectory = getEmailDirectoryFromArgs(args);
            
            // Create UI service
            UIService uiService = new SwingUIService();
            
            // Create the email browser app
            EmailBrowserApp emailBrowserApp = new EmailBrowserApp(emailDirectory, uiService);
            
            // Create system tray configuration
            AppConfig appConfig = createAppConfig(emailBrowserApp);
            
            // Create menu categories
            List<MenuCategory> menuCategories = emailBrowserApp.createMenuCategories();
            
            // Create tasks (empty list for this app)
            List<BackgroundTask> tasks = new ArrayList<>();
            
            // Create the system tray app
            SystemTrayApp systemTrayApp = new SystemTrayApp(appConfig, menuCategories, tasks);
            
            // Set the system tray app in the email browser app
            emailBrowserApp.setSystemTrayApp(systemTrayApp);
            
            // Show the browser window after startup
            emailBrowserApp.showEmailBrowser();
            
            LOGGER.info("Email Browser application started successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start Email Browser application", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Get the email directory from command line arguments
     *
     * @param args Command line arguments
     * @return The email directory
     */
    private static File getEmailDirectoryFromArgs(String[] args) {
        if (args.length > 0) {
            return new File(args[0]);
        } else {
            // Default location if not specified
            return new File(System.getProperty("user.home"), "EmailArchive");
        }
    }
    
    /**
     * Create the application configuration
     *
     * @param emailBrowserApp The email browser application
     * @return The app config
     */
    private static AppConfig createAppConfig(EmailBrowserApp emailBrowserApp) {
        return new AppConfig() {
            @Override
            public String getAppName() {
                return APP_NAME;
            }
            
            @Override
            public String getIconPath() {
                return "/intenovation.png";  // Make sure this exists in resources
            }
            
            @Override
            public void onIconDoubleClick() {
                // Show the email browser when the tray icon is double-clicked
                emailBrowserApp.showEmailBrowser();
            }
        };
    }
    
    /**
     * Create the EmailBrowserApp as part of the Intenovation Suite
     *
     * @param emailDirectory The email directory
     * @param uiService The UI service
     * @return The email browser app
     */
    public static EmailBrowserApp createForIntegration(File emailDirectory, UIService uiService) {
        return new EmailBrowserApp(emailDirectory, uiService);
    }
}