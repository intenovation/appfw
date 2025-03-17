package com.intenovation.appfw.systemtray;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple implementation of MenuGroup to simplify creating menus
 */
public class SimpleMenuGroup implements MenuGroup {
    private final String label;
    private final List<MenuAction> actions = new ArrayList<>();
    private final List<MenuGroup> subGroups = new ArrayList<>();

    public SimpleMenuGroup(String label) {
        this.label = label;
    }

    public SimpleMenuGroup addAction(MenuAction action) {
        actions.add(action);
        return this;
    }

    public SimpleMenuGroup addSubGroup(MenuGroup group) {
        subGroups.add(group);
        return this;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public List<MenuAction> getMenuActions() {
        return actions;
    }

    @Override
    public List<MenuGroup> getSubGroups() {
        return subGroups;
    }
}
