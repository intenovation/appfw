package com.intenovation.appfw.systemtray;

import java.awt.CheckboxMenuItem;
import java.awt.MenuComponent;
import java.awt.TrayIcon;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.logging.Logger;

import com.intenovation.appfw.inversemv.CheckboxModel;
import com.intenovation.appfw.inversemv.CheckboxView;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;

/**
 * A checkbox view implementation for system tray menu items.
 */
public class FrameworkCheckboxTrayView extends FrameworkTrayView implements CheckboxView {
    private static final Logger log = Logger.getLogger(FrameworkCheckboxTrayView.class.getName());
    
    private CheckboxMenuItem checkboxItem;
    
    /**
     * Create a new checkbox tray view.
     * 
     * @param parent The parent model
     * @param model The model for this view
     * @param trayIcon The tray icon to use
     * @param appName The application name
     */
    public FrameworkCheckboxTrayView(ParentModel parent, Model model, TrayIcon trayIcon, String appName) {
        super(parent, model, trayIcon, appName);
        
        // Create the checkbox menu item
        checkboxItem = new CheckboxMenuItem(appName);
        
        // Add item listener to update the model when the checkbox state changes
        checkboxItem.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
                ((CheckboxModel)model).setChecked(selected);
            }
        });
    }
    
    @Override
    public void setChecked(boolean checked) {
        if (checkboxItem != null) {
            checkboxItem.setState(checked);
        }
    }
    
    @Override
    public void setName(String name) {
        super.setName(name);
        if (checkboxItem != null) {
            checkboxItem.setLabel(name);
        }
    }
    
    @Override
    public MenuComponent getMenu() {
        return checkboxItem;
    }
}