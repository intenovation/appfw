package com.intenovation.appfw.tray;

import java.awt.MenuComponent;
import java.awt.TrayIcon;

import com.intenovation.appfw.icon.PictureElement;
import com.intenovation.appfw.inversemv.CheckboxView;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;

/**
 * @deprecated Use FrameworkCheckboxTrayView instead
 */
@Deprecated
public class CheckboxViewImpl extends AbstractTrayView implements CheckboxView {
    
    public CheckboxViewImpl(ParentModel parent, Model model, TrayIcon trayIcon, String appname) {
        super(parent, model, trayIcon, appname);
    }
    
    @Override
    public void setChecked(boolean checked) {
        // Stub implementation
    }
    
    @Override
    public MenuComponent getMenu() {
        return null;
    }
}
