package com.intenovation.appfw.systemtray;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intenovation.appfw.icon.PictureElement;
import com.intenovation.appfw.icon.SmartIcon;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.inversemv.ParentView;
import com.intenovation.appfw.inversemv.View;

/**
 * A parent tray view that integrates with the system tray and provides
 * parent-child view relationships for the framework.
 */
public class FrameworkParentTrayView implements ParentView {
    private static final Logger log = Logger.getLogger(FrameworkParentTrayView.class.getName());
    
    private SystemTray tray;
    private TrayIcon trayIcon;
    private String appname;
    private PopupMenu menu;
    private SmartIcon icon;
    private ParentModel model;
    
    /**
     * Create a new parent tray view.
     * 
     * @param model The parent model
     */
    public FrameworkParentTrayView(ParentModel model) {
        this.model = model;
        this.menu = new PopupMenu();
        
        if (!SystemTray.isSupported()) {
            log.log(Level.SEVERE, "SystemTray is not supported");
            throw new RuntimeException("SystemTray is not supported");
        }
        
        this.tray = SystemTray.getSystemTray();
    }
    
    /**
     * Add exit and window options to the menu.
     */
    private void makeExit() {
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tray.remove(trayIcon);
                getModel().stop();
                System.exit(0);
            }
        });
        getMenu().add(exitItem);
        
        getMenu().addSeparator();
    }
    
    /**
     * Get the model for this view.
     */
    public Model getModel() {
        return model;
    }
    
    @Override
    public void setName(String name) {
        this.appname = name;
    }
    
    @Override
    public Dimension getIconSize() {
        if (tray == null) {
            log.warning("tray==null");
            tray = SystemTray.getSystemTray();
        }
        return tray.getTrayIconSize();
    }
    
    @Override
    public void setIcon(SmartIcon icon) {
        this.icon = icon;
        trayIcon = new TrayIcon(icon.getIcon());
        trayIcon.setPopupMenu(menu);
        trayIcon.setToolTip(icon.getTooltip());
        
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            log.log(Level.SEVERE, "Failed to add tray icon", e);
        }
        
        makeExit();
    }
    
    @Override
    public void addAccent(PictureElement accent) {
        if (icon != null) {
            icon.addAccent(accent);
            trayIcon.setImage(icon.getIcon());
            trayIcon.setToolTip(icon.getTooltip());
        }
    }
    
    @Override
    public void removeAccent(PictureElement accent) {
        if (icon != null) {
            icon.removeAccent(accent);
            trayIcon.setImage(icon.getIcon());
            trayIcon.setToolTip(icon.getTooltip());
        }
    }
    
    /**
     * Get the menu for this view.
     */
    public PopupMenu getMenu() {
        return menu;
    }
    
    @Override
    public View addChild(Model child) {
        // Create a child view for the given model
        FrameworkTrayView childView = new FrameworkTrayView((ParentModel)model, child, trayIcon, appname);
        
        // Add the child view's menu to this view's menu if applicable
        if (childView instanceof FrameworkTrayView) {
            MenuComponent childMenu = ((FrameworkTrayView) childView).getMenu();
            if (childMenu != null) {
                menu.add(childMenu);
            }
        }
        
        return childView;
    }
    
    @Override
    public void removeChild(Model child) {
        // In a real implementation, this would remove the child's menu items
        // This is a simplified version
        log.info("Removing child: " + child);
    }
    
    @Override
    public void error(String message) {
        log.log(Level.SEVERE, message);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, message, TrayIcon.MessageType.ERROR);
        }
    }
    
    @Override
    public void error(Throwable e) {
        log.log(Level.SEVERE, e.getMessage(), e);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, e.getMessage(), TrayIcon.MessageType.ERROR);
        }
    }
    
    @Override
    public void info(String message) {
        log.log(Level.INFO, message);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, message, TrayIcon.MessageType.INFO);
        }
    }
    
    @Override
    public void warning(String message) {
        log.log(Level.WARNING, message);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, message, TrayIcon.MessageType.WARNING);
        }
    }
    
    @Override
    public void warning(Throwable e) {
        log.log(Level.WARNING, e.getMessage(), e);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, e.getMessage(), TrayIcon.MessageType.WARNING);
        }
    }
    
    @Override
    public void none(String message) {
        log.log(Level.FINE, message);
        if (trayIcon != null) {
            trayIcon.displayMessage(appname, message, TrayIcon.MessageType.NONE);
        }
    }
    
    @Override
    public void notifyMyParent() {
        // As the root, there's no parent to notify
    }
}