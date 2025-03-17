package com.intenovation.appfw.systemtray;

import java.awt.MenuItem;
import java.awt.MenuComponent;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import com.intenovation.appfw.inversemv.ActionModel;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.thread.IntenovationThreadPool;

/**
 * An action view implementation for system tray menu items.
 */
public class FrameworkActionTrayView extends FrameworkTrayView {
    private static final Logger log = Logger.getLogger(FrameworkActionTrayView.class.getName());
    
    private MenuItem menuItem;
    
    /**
     * Create a new action tray view.
     * 
     * @param parent The parent model
     * @param model The model for this view
     * @param trayIcon The tray icon to use
     * @param appName The application name
     */
    public FrameworkActionTrayView(ParentModel parent, Model model, TrayIcon trayIcon, String appName) {
        super(parent, model, trayIcon, appName);
        
        // Create the menu item
        menuItem = new MenuItem(appName);
        
        // Add action listener to execute the model's action
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ActionModel actionModel = (ActionModel)model;
                IntenovationThreadPool.invokeLater("Action " + appName, new Runnable() {
                    @Override
                    public void run() {
                        actionModel.action();
                    }
                });
            }
        });
    }
    
    @Override
    public void setName(String name) {
        super.setName(name);
        if (menuItem != null) {
            menuItem.setLabel(name);
        }
    }
    
    @Override
    public MenuComponent getMenu() {
        return menuItem;
    }
}