package com.intenovation.appfw.config;

/**
 * Implementation for password configuration items
 */
public class PasswordConfigItem implements ConfigItem {
    private final String key;
    private final String displayName;
    private final String defaultValue;
    
    public PasswordConfigItem(String key, String displayName, String defaultValue) {
        this.key = key;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
    }
    
    @Override public String getKey() { return key; }
    @Override public String getDisplayName() { return displayName; }
    @Override public ConfigItemType getType() { return ConfigItemType.PASSWORD; }
    @Override public Object getDefaultValue() { return defaultValue; }
}
