package com.intenovation.email.ui;

import com.intenovation.appfw.systemtray.*;
import com.intenovation.appfw.ui.UIService;
import com.intenovation.email.reader.LocalMail;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application class for the Email Browser.
 * This integrates with the SystemTrayApp framework and manages the email browser window.
 */
public class EmailBrowserApp {
    private static final Logger LOGGER = Logger.getLogger(EmailBrowserApp.class.getName());
    private static final String APP_NAME = "Email Browser";
    private static final String VERSION = "1.0.0";

    // Dependencies
    private final File emailDirectory;
    private final UIService uiService;

    // UI components
    private EmailBrowserFrame browserFrame;

    // SystemTrayApp instance
    private SystemTrayApp systemTrayApp;

    /**
     * Create a new EmailBrowserApp
     *
     * @param emailDirectory The directory containing downloaded emails
     * @param uiService UI service
     */
    public EmailBrowserApp(File emailDirectory, UIService uiService) {
        this.emailDirectory = emailDirectory;
        this.uiService = uiService;
    }

    /**
     * Set the system tray app instance to use
     * @param systemTrayApp The system tray app instance
     */
    public void setSystemTrayApp(SystemTrayApp systemTrayApp) {
        this.systemTrayApp = systemTrayApp;
    }

    /**
     * Show the email browser window
     */
    public void showEmailBrowser() {
        if (browserFrame == null) {
            try {
                // Open the email store
                Store store = LocalMail.openStore(emailDirectory);
                
                // Create the browser frame
                browserFrame = new EmailBrowserFrame(store, emailDirectory);
                browserFrame.setVisible(true);
            } catch (MessagingException e) {
                LOGGER.log(Level.SEVERE, "Error opening email store", e);
                uiService.showError("Error", "Failed to open email store: " + e.getMessage());
            }
        } else {
            // If already open, bring to front
            browserFrame.setVisible(true);
            browserFrame.toFront();
        }
    }

    /**
     * Close the email browser window
     */
    public void closeEmailBrowser() {
        if (browserFrame != null) {
            browserFrame.dispose();
            browserFrame = null;
        }
    }

    /**
     * Refresh the email browser (reload folders and messages)
     */
    public void refreshEmailBrowser() {
        if (browserFrame != null) {
            browserFrame.refreshFolders();
        } else {
            showEmailBrowser();
        }
    }

    /**
     * Show the about dialog
     */
    public void showAboutDialog() {
        uiService.showInfo("About " + APP_NAME,
                APP_NAME + " Version " + VERSION + "\n\n" +
                        "A tool for browsing emails that have been downloaded to your local machine.\n\n" +
                        "Email Directory: " + emailDirectory.getAbsolutePath());
    }

    /**
     * Create menu categories for the system tray
     * @return List of menu categories
     */
    public List<MenuCategory> createMenuCategories() {
        List<MenuCategory> categories = new ArrayList<>();

        // Email Browser menu
        categories.add(new CategoryBuilder("Email Browser")
                .addAction("Open Email Browser", this::showEmailBrowser)
                .addAction("Refresh Emails", this::refreshEmailBrowser)
                .addAction("Close Browser", this::closeEmailBrowser)
                .build());

        // Help menu
        categories.add(new CategoryBuilder("Help")
                .addAction("About Email Browser", this::showAboutDialog)
                .build());

        return categories;
    }
}