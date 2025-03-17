package com.intenovation.appfw.systemtray;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intenovation.appfw.icon.PictureElement;
import com.intenovation.appfw.icon.SmartIcon;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.inversemv.View;

/**
 * Framework-specific tray view implementation that integrates with the
 * intenovation application framework components.
 * 
 * This class combines functionality from the old tray package's
 * AbstractTrayView and RootTrayView classes.
 */
public class FrameworkTrayView implements View {
    private static final Logger log = Logger.getLogger(FrameworkTrayView.class.getName());
    
    protected String appname;
    protected TrayIcon trayIcon;
    protected SystemTray tray;
    protected ParentModel model;
    protected PopupMenu menu;
    protected SmartIcon icon;
    
    /**
     * Creates a new framework tray view.
     * 
     * @param model The parent model
     * @param appName The application name
     */
    public FrameworkTrayView(ParentModel model, String appName) {
        this.model = model;
        this.appname = appName;
        this.menu = new PopupMenu();
        
        if (!SystemTray.isSupported()) {
            log.log(Level.SEVERE, "SystemTray is not supported");
            throw new RuntimeException("SystemTray is not supported");
        }
        
        this.tray = SystemTray.getSystemTray();
    }
    
    /**
     * Set the icon for this tray view.
     * 
     * @param icon The icon to use
     */
    public void setIcon(SmartIcon icon) {
        this.icon = icon;
        trayIcon = new TrayIcon(icon.getIcon());
        trayIcon.setPopupMenu(menu);
        trayIcon.setToolTip(icon.getTooltip());
        
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            log.log(Level.SEVERE, "Failed to add tray icon", e);
            throw new RuntimeException("Failed to add tray icon", e);
        }
    }
    
    /**
     * Add an accent to the tray icon.
     * 
     * @param accent The accent to add
     */
    public void addAccent(PictureElement accent) {
        if (icon != null) {
            icon.addAccent(accent);
            trayIcon.setImage(icon.getIcon());
            trayIcon.setToolTip(icon.getTooltip());
        }
    }
    
    /**
     * Remove an accent from the tray icon.
     * 
     * @param accent The accent to remove
     */
    public void removeAccent(PictureElement accent) {
        if (icon != null) {
            icon.removeAccent(accent);
            trayIcon.setImage(icon.getIcon());
            trayIcon.setToolTip(icon.getTooltip());
        }
    }
    
    /**
     * Get the popup menu for this tray view.
     * 
     * @return The popup menu
     */
    public PopupMenu getMenu() {
        return menu;
    }
    
    /**
     * Display an error message.
     * 
     * @param message The error message
     */
    public void error(String message) {
        log.log(Level.SEVERE, message);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, message, TrayIcon.MessageType.ERROR);
        }
    }
    
    /**
     * Display an error message for an exception.
     * 
     * @param e The exception
     */
    public void error(Throwable e) {
        log.log(Level.SEVERE, e.getMessage(), e);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, e.getMessage(), TrayIcon.MessageType.ERROR);
        }
    }
    
    /**
     * Display an information message.
     * 
     * @param message The information message
     */
    public void info(String message) {
        log.log(Level.INFO, message);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, message, TrayIcon.MessageType.INFO);
        }
    }
    
    /**
     * Display a warning message.
     * 
     * @param message The warning message
     */
    public void warning(String message) {
        log.log(Level.WARNING, message);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, message, TrayIcon.MessageType.WARNING);
        }
    }
    
    /**
     * Display a warning message for an exception.
     * 
     * @param e The exception
     */
    public void warning(Throwable e) {
        log.log(Level.WARNING, e.getMessage(), e);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, e.getMessage(), TrayIcon.MessageType.WARNING);
        }
    }
    
    /**
     * Display a plain message.
     * 
     * @param message The message
     */
    public void none(String message) {
        log.log(Level.FINE, message);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, message, TrayIcon.MessageType.NONE);
        }
    }
    
    @Override
    public void setName(String name) {
        this.appname = name;
    }

    @Override
    public Dimension getIconSize() {
        return null;
    }

    @Override
    public void notifyMyParent() {
        // Not needed in this implementation
    }
}