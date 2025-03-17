package com.intenovation.appfw.tray;

import java.awt.MenuComponent;
import java.awt.TrayIcon;

import com.intenovation.appfw.icon.PictureElement;
import com.intenovation.appfw.inimport com.intenovation.appftenovation.appfw.inversemv.ParentModel;

/**
 * @deprecated Use FrameworkActionTrayView instead
 */
@Deprecated
public class ActionViewImpl extends AbstractTrayView {
    
    public ActionViewImpl(ParentModel parent, Model model, TrayIcon trayIcon, String appname) {
        super(parent, model, trayIcon, appname);
    }
    
    @Override
    public MenuComponent getMenu() {
        return null;
    }
}
