// File: SimpleAction.java
package com.intenovation.appfw.systemtray;

/**
 * Simple implementation of the Action interface
 */
public class SimpleAction implements Action {
    private final String label;
    private final Runnable action;
    
    /**
     * Create a new simple action
     * @param label The action label
     * @param action The action to execute
     */
    public SimpleAction(String label, Runnable action) {
        this.label = label;
        this.action = action;
    }
    
    @Override
    public String getLabel() {
        return label;
    }
    
    @Override
    public void execute() {
        action.run();
    }
}
