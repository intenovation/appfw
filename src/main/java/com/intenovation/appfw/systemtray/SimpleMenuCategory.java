
// File: SimpleMenuCategory.java
package com.intenovation.appfw.systemtray;

import java.util.List;

/**
 * Simple implementation of the MenuCategory interface
 */
public class SimpleMenuCategory implements MenuCategory {
    private final String label;
    private final List<Action> actions;
    private final List<MenuCategory> subcategories;
    
    /**
     * Create a new simple menu category
     * @param label The category label
     * @param actions The actions in this category
     * @param subcategories The subcategories
     */
    public SimpleMenuCategory(String label, List<Action> actions, List<MenuCategory> subcategories) {
        this.label = label;
        this.actions = actions;
        this.subcategories = subcategories;
    }
    
    @Override
    public String getLabel() {
        return label;
    }
    
    @Override
    public List<Action> getActions() {
        return actions;
    }
    
    @Override
    public List<MenuCategory> getSubcategories() {
        return subcategories;
    }
}
