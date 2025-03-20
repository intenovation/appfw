package com.intenovation.appfw.config;

import java.util.List;
import java.util.Map;

/**
 * Interface for defining configuration requirements.
 * This allows business logic packages to define what they need to configure
 * without depending on UI frameworks.
 */
public interface ConfigurationDefinition {
    /**
     * Get configuration metadata (what needs to be configured)
     * @return List of configuration items
     */
    List<ConfigItem> getConfigItems();
    
    /**
     * Apply configuration values
     * @param configValues Map of configuration values
     */
    void applyConfiguration(Map<String, Object> configValues);
    
    /**
     * Get current configuration values
     * @return Map of current configuration values
     */
    Map<String, Object> getCurrentValues();
}