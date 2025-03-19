// File: MenuCategory.java
package com.intenovation.appfw.systemtray;

import java.util.List;

/**
 * Menu category interface representing a menu or submenu with actions and subcategories
 */
public interface MenuCategory {
    /**
     * Get the label for this menu
     * @return Menu label
     */
    String getLabel();

    /**
     * Get menu items
     * @return List of actions
     */
    List<Action> getActions();

    /**
     * Get subcategories
     * @return List of subcategories
     */
    List<MenuCategory> getSubcategories();
}