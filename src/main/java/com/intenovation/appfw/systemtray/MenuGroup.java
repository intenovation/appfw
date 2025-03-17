package com.intenovation.appfw.systemtray;

import java.util.List;

/**
 * Interface for a submenu that contains menu items
 */
public interface MenuGroup {
    /**
     * Get the label for this submenu
     *
     * @return Submenu label
     */
    String getLabel();

    /**
     * Get the menu items in this submenu
     *
     * @return List of menu actions
     */
    List<MenuAction> getMenuActions();

    /**
     * Get submenus in this submenu
     *
     * @return List of menu groups (submenus)
     */
    List<MenuGroup> getSubGroups();
}
