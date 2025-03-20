package com.intenovation.appfw.config;

/**
 * Implementation for checkbox configuration items
 */
public class CheckboxConfigItem implements ConfigItem {
    private final String key;
    private final String displayName;
    private final boolean defaultValue;
    
    public CheckboxConfigItem(String key, String displayName, boolean defaultValue) {
        this.key = key;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
    }
    
    @Override public String getKey() { return key; }
    @Override public String getDisplayName() { return displayName; }
    @Override public ConfigItemType getType() { return ConfigItemType.CHECKBOX; }
    @Override public Object getDefaultValue() { return defaultValue; }
}
