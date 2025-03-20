package com.intenovation.appfw.config;

/**
 * Implementation for number configuration items
 */
public class NumberConfigItem implements ConfigItem {
    private final String key;
    private final String displayName;
    private final Number defaultValue;
    
    public NumberConfigItem(String key, String displayName, Number defaultValue) {
        this.key = key;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
    }
    
    @Override public String getKey() { return key; }
    @Override public String getDisplayName() { return displayName; }
    @Override public ConfigItemType getType() { return ConfigItemType.NUMBER; }
    @Override public Object getDefaultValue() { return defaultValue; }
}
