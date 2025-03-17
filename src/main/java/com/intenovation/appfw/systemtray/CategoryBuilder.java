// File: CategoryBuilder.java
package com.intenovation.appfw.systemtray;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating menu categories
 */
public class CategoryBuilder {
    private final String label;
    private final List<Action> actions = new ArrayList<>();
    private final List<MenuCategory> subcategories = new ArrayList<>();
    
    /**
     * Create a new category builder
     * @param label The category label
     */
    public CategoryBuilder(String label) {
        this.label = label;
    }
    
    /**
     * Add an action to this category
     * @param label The action label
     * @param runnable The action to run
     * @return This builder for chaining
     */
    public CategoryBuilder addAction(String label, Runnable runnable) {
        actions.add(new SimpleAction(label, runnable));
        return this;
    }
    
    /**
     * Add a subcategory to this category
     * @param subcategory The subcategory
     * @return This builder for chaining
     */
    public CategoryBuilder addSubcategory(MenuCategory subcategory) {
        subcategories.add(subcategory);
        return this;
    }
    
    /**
     * Build the menu category
     * @return The menu category
     */
    public MenuCategory build() {
        return new SimpleMenuCategory(label, actions, subcategories);
    }
}
