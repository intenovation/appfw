package com.intenovation.email.ui;

import com.intenovation.appfw.systemtray.*;
import com.intenovation.appfw.ui.UIService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Integration class for adding the Email Browser to the Intenovation Suite.
 * This class provides methods for creating menu items and actions that 
 * can be added to the existing system tray application.
 */
public class EmailBrowserIntegration {
    // The email browser application instance
    private final EmailBrowserApp emailBrowserApp;
    
    /**
     * Create a new email browser integration
     *
     * @param emailDirectory The directory containing downloaded emails
     * @param uiService The UI service to use
     */
    public EmailBrowserIntegration(File emailDirectory, UIService uiService) {
        this.emailBrowserApp = new EmailBrowserApp(emailDirectory, uiService);
    }
    
    /**
     * Set the system tray app to use
     *
     * @param systemTrayApp The system tray app
     */
    public void setSystemTrayApp(SystemTrayApp systemTrayApp) {
        emailBrowserApp.setSystemTrayApp(systemTrayApp);
    }
    
    /**
     * Get the email browser app
     *
     * @return The email browser app
     */
    public EmailBrowserApp getEmailBrowserApp() {
        return emailBrowserApp;
    }
    
    /**
     * Create menu categories for the system tray
     *
     * @return List of menu categories
     */
    public List<MenuCategory> createMenuCategories() {
        return emailBrowserApp.createMenuCategories();
    }
    
    /**
     * Create a single menu category for the email browser
     * This can be added to an existing menu structure.
     *
     * @return Menu category for the email browser
     */
    public MenuCategory createMenuCategory() {
        return new CategoryBuilder("Email Browser")
                .addAction("Open Email Browser", emailBrowserApp::showEmailBrowser)
                .addAction("Refresh Emails", emailBrowserApp::refreshEmailBrowser)
                .addAction("Close Browser", emailBrowserApp::closeEmailBrowser)
                .build();
    }
    
    /**
     * Create actions for the email browser
     * These can be added to an existing menu.
     *
     * @return List of actions for the email browser
     */
    public List<Action> createActions() {
        List<Action> actions = new ArrayList<>();
        
        actions.add(new SimpleAction("Open Email Browser", emailBrowserApp::showEmailBrowser));
        actions.add(new SimpleAction("Refresh Emails", emailBrowserApp::refreshEmailBrowser));
        actions.add(new SimpleAction("Close Browser", emailBrowserApp::closeEmailBrowser));
        
        return actions;
    }
}