package com.intenovation.appfw.ui;

import com.intenovation.appfw.config.ConfigurationDefinition;
import java.io.File;

/**
 * Interface for UI services
 * This allows business logic to request UI operations without 
 * directly depending on UI frameworks.
 */
public interface UIService {
    /**
     * Show configuration dialog
     * @param title Title of the dialog
     * @param config Configuration definition
     * @return true if configuration was saved, false if cancelled
     */
    boolean showConfigDialog(String title, ConfigurationDefinition config);
    
    /**
     * Show information message
     * @param title Message title
     * @param message Message content
     */
    void showInfo(String title, String message);
    
    /**
     * Show error message
     * @param title Message title
     * @param message Error message
     */
    void showError(String title, String message);
    
    /**
     * Show warning message
     * @param title Message title
     * @param message Warning message
     */
    void showWarning(String title, String message);
    
    /**
     * Open a directory in the system file explorer
     * @param directory The directory to open
     * @return true if successful
     */
    boolean openDirectory(File directory);
}