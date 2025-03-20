package com.intenovation.appfw.config;

import java.io.File;
import java.util.List;

/**
 * Implementation for text configuration items
 */
public class TextConfigItem implements ConfigItem {
    private final String key;
    private final String displayName;
    private final String defaultValue;
    
    public TextConfigItem(String key, String displayName, String defaultValue) {
        this.key = key;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
    }
    
    @Override public String getKey() { return key; }
    @Override public String getDisplayName() { return displayName; }
    @Override public ConfigItemType getType() { return ConfigItemType.TEXT; }
    @Override public Object getDefaultValue() { return defaultValue; }
}

