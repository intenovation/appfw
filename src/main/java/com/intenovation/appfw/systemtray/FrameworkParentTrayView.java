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
public class FrameworkParentTrayView extends FrameworkTrayView implements ParentView {
    private static final Logger log = Logger.getLogger(FrameworkParentTrayView.class.getName());

    private SystemTray tray;
    private PopupMenu menu;

    /**
     * Create a new parent tray view.
     *
     * @param model The parent model
     */
    public FrameworkParentTrayView(ParentModel model) {
        super(null, model, null, model.getClass().getSimpleName());
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
                ((Model)model).stop();
                System.exit(0);
            }
        });
        getMenu().add(exitItem);

        getMenu().addSeparator();
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
        super.setIcon(icon);
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

    @Override
    public PopupMenu getMenu() {
        return menu;
    }

    @Override
    public View addChild(Model child) {
        // Determine the appropriate view type based on the model type
        FrameworkTrayView childView;

        if (child instanceof com.intenovation.appfw.inversemv.CheckboxModel) {
            childView = new FrameworkCheckboxTrayView((ParentModel) this.model, child, trayIcon, appname);
        } else if (child instanceof com.intenovation.appfw.inversemv.ActionModel) {
            childView = new FrameworkActionTrayView((ParentModel)this.model, child, trayIcon, appname);
        } else if (child instanceof ParentModel) {
            childView = new FrameworkParentTrayView((ParentModel)child);
            childView.trayIcon = this.trayIcon;
            childView.appname = this.appname;
        } else {
            childView = new FrameworkTrayView((ParentModel)this.model, child, trayIcon, appname);
        }

        // Add the child's menu item to this menu if applicable
        if (childView.getMenu() != null) {
                        MenuComponent childMenu = childView.getMenu();
                        if (childMenu instanceof MenuItem)
                                menu.add((MenuItem)childMenu);
        }


        return childView;
    }

    @Override
    public void removeChild(Model child) {
        // In a real implementation, this would remove the child's menu items
        // This is a simplified version
        log.info("Removing child: " + child);
    }
}