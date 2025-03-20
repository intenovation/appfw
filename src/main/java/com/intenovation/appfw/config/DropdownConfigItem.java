package com.intenovation.appfw.config;

import java.util.List; /**
 * Implementation for dropdown configuration items
 */
public class DropdownConfigItem implements ConfigItem {
    private final String key;
    private final String displayName;
    private final String defaultValue;
    private final List<String> options;
    
    public DropdownConfigItem(String key, String displayName, String defaultValue, List<String> options) {
        this.key = key;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
        this.options = options;
    }
    
    @Override public String getKey() { return key; }
    @Override public String getDisplayName() { return displayName; }
    @Override public ConfigItemType getType() { return ConfigItemType.DROPDOWN; }
    @Override public Object getDefaultValue() { return defaultValue; }
    
    public List<String> getOptions() { return options; }
}
