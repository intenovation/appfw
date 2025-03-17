package com.intenovation.appfw.systemtray;

/**
 * Interface for a menu item that can be clicked
 */
public interface MenuAction {
    /**
     * Execute the action when menu item is clicked
     */
    void execute();

    /**
     * Get the label for this menu item
     *
     * @return Menu item label
     */
    String getLabel();
}
