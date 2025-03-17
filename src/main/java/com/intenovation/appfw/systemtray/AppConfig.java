// File: AppConfig.java
package com.intenovation.appfw.systemtray;

/**
 * Application configuration interface
 */
public interface AppConfig {
    /**
     * Get the application name
     * @return Application name
     */
    String getAppName();
    
    /**
     * Get the icon path in resources
     * @return Path to icon
     */
    String getIconPath();
    
    /**
     * Action to perform when the tray icon is double-clicked
     */
    void onIconDoubleClick();
}


