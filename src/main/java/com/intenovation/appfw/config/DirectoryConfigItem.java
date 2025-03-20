package com.intenovation.appfw.config;

import java.io.File; /**
 * Implementation for directory configuration items
 */
public class DirectoryConfigItem implements ConfigItem {
    private final String key;
    private final String displayName;
    private final File defaultValue;
    
    public DirectoryConfigItem(String key, String displayName, File defaultValue) {
        this.key = key;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
    }
    
    @Override public String getKey() { return key; }
    @Override public String getDisplayName() { return displayName; }
    @Override public ConfigItemType getType() { return ConfigItemType.DIRECTORY; }
    @Override public Object getDefaultValue() { return defaultValue; }
}
