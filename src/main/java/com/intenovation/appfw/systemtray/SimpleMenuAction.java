package com.intenovation.appfw.systemtray;

/**
 * Simple implementation of MenuAction
 */
public class SimpleMenuAction implements MenuAction {
    private final String label;
    private final Runnable action;

    public SimpleMenuAction(String label, Runnable action) {
        this.label = label;
        this.action = action;
    }

    @Override
    public void execute() {
        action.run();
    }

    @Override
    public String getLabel() {
        return label;
    }
}
