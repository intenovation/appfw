package com.intenovation.appfw.systemtray;

import java.awt.MenuComponent;
import java.awt.TrayIcon;
import java.awt.Dimension;
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
 */
public class FrameworkTrayView implements View {
    private static final Logger log = Logger.getLogger(FrameworkTrayView.class.getName());

    protected String appname;
    protected TrayIcon trayIcon;
    protected ParentModel parent;
    protected Model model;
    protected SmartIcon icon;
    protected Dimension iconSize;

    /**
     * Creates a new framework tray view.
     *
     * @param parent The parent model
     * @param model The model for this view
     * @param trayIcon The tray icon to use
     * @param appName The application name
     */
    public FrameworkTrayView(ParentModel parent, Model model, TrayIcon trayIcon, String appName) {
        this.parent = parent;
        this.model = model;
        this.trayIcon = trayIcon;
        this.appname = appName;
    }

    @Override
    public void setName(String name) {
        this.appname = name;
    }

    /**
     * Get the icon size for this view.
     */
    public Dimension getIconSize() {
        return iconSize;
    }

    /**
     * Set the icon for this view.
     */
    public void setIcon(SmartIcon icon) {
        this.icon = icon;
    }

    /**
     * Add an accent to the icon.
     */
    public void addAccent(PictureElement accent) {
        // Default implementation does nothing
    }

    /**
     * Remove an accent from the icon.
     */
    public void removeAccent(PictureElement accent) {
        // Default implementation does nothing
    }

    /**
     * Get the menu component for this view.
     */
    public MenuComponent getMenu() {
        return null; // Subclasses should override
    }

    /**
     * Display an error message.
     */
    public void error(String message) {
        log.log(Level.SEVERE, message);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, message, TrayIcon.MessageType.ERROR);
        }
    }

    /**
     * Display an error message for an exception.
     */
    public void error(Throwable e) {
        log.log(Level.SEVERE, e.getMessage(), e);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, e.getMessage(), TrayIcon.MessageType.ERROR);
        }
    }

    /**
     * Display an information message.
     */
    public void info(String message) {
        log.log(Level.INFO, message);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, message, TrayIcon.MessageType.INFO);
        }
    }

    /**
     * Display a warning message.
     */
    public void warning(String message) {
        log.log(Level.WARNING, message);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, message, TrayIcon.MessageType.WARNING);
        }
    }

    /**
     * Display a warning message for an exception.
     */
    public void warning(Throwable e) {
        log.log(Level.WARNING, e.getMessage(), e);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, e.getMessage(), TrayIcon.MessageType.WARNING);
        }
    }

    /**
     * Display a plain message.
     */
    public void none(String message) {
        log.log(Level.FINE, message);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, message, TrayIcon.MessageType.NONE);
        }
    }

    @Override
    public void notifyMyParent() {
        if (parent != null) {
            parent.childHasChanged(model);
        }
    }
}