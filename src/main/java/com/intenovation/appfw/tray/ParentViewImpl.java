package com.intenovation.appfw.tray;

import java.awt.MenuComponent;
import java.awt.PopupMenu;
import java.awt.TrayIcon;

import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.inversemv.ParentView;
import com.intenovation.appfw.inversemv.View;

/**
                                  TrayView instead
 */
@Deprecated
public class ParentViewImpl extends AbstractTrayView implements ParentView {
    protected PopupMenu menu;
    
    public ParentViewImpl(ParentModel model, PopupMenu menu) {
        super(null, model, null, model.getClass().getSimpleName())        s this.menu = menu;
    }
    
    @Override
    public MenuComponent getMenu() {
        return menu;
    }
    
    @Override
    public View addChild(Model child) {
        // Return stub view
        return null;
    }
    
    @O    @O    @O    @O    @O    @O    d(Model child) {
        // Stub implementation
    }
}
