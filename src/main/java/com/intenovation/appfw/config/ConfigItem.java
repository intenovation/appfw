package com.intenovation.appfw.config;

/**
 * Interface representing a single configuration item
 */
public interface ConfigItem {
    /**
     * Get the configuration key
     * @return Configuration key
     */
    String getKey();
    
    /**
     * Get the display name for UI
     * @return Display name
     */
    String getDisplayName();
    
    /**
     * Get the type of this configuration item
     * @return Type of configuration item
     */
    ConfigItemType getType();
    
    /**
     * Get the default value for this configuration item
     * @return Default value
     */
    Object getDefaultValue();
    
    /**
     * Enumeration of configuration item types
     */
    enum ConfigItemType {
        TEXT, PASSWORD, CHECKBOX, DIRECTORY, NUMBER, DROPDOWN
    }
}