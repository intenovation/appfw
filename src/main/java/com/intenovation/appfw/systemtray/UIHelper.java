// File: UIHelper.java
package com.intenovation.appfw.systemtray;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Helper class for UI operations to hide UI dependencies from the main API
 */
public class UIHelper {
    private static final Logger LOGGER = Logger.getLogger(UIHelper.class.getName());
    
    /**
     * Show an information message
     * @param title Message title
     * @param message Message content
     */
    public static void showMessage(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
        });
    }
    
    /**
     * Show an error message
     * @param title Message title
     * @param message Error message
     */
    public static void showError(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
        });
    }
    
    /**
     * Show a warning message
     * @param title Message title
     * @param message Warning message
     */
    public static void showWarning(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
        });
    }
    
    /**
     * Open a directory in the system file explorer
     * @param directory The directory to open
     * @return true if successful
     */
    public static boolean openDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return false;
        }
        
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(directory);
                return true;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error opening directory with Desktop API", e);
        }
        
        // Fallback to command line for specific operating systems
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("windows")) {
                Runtime.getRuntime().exec("explorer.exe \"" + directory.getAbsolutePath() + "\"");
                return true;
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", directory.getAbsolutePath()});
                return true;
            } else if (os.contains("linux")) {
                Runtime.getRuntime().exec(new String[]{"xdg-open", directory.getAbsolutePath()});
                return true;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error opening directory with command line", e);
        }
        
        return false;
    }
}