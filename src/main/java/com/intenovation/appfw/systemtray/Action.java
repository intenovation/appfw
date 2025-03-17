
// File: Action.java
package com.intenovation.appfw.systemtray;

/**
 * Menu action interface
 */
public interface Action {
    /**
     * Get the label for this menu item
     * @return Menu item label
     */
    String getLabel();
    
    /**
     * Execute the action
     */
    void execute();
}
