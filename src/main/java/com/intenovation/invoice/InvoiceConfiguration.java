package com.intenovation.invoice;

import com.intenovation.appfw.config.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for invoice processing
 */
public class InvoiceConfiguration implements ConfigurationDefinition {
    private static final Logger LOGGER = Logger.getLogger(InvoiceConfiguration.class.getName());
    private static final String CONFIG_FILE = System.getProperty("user.home") + File.separator + ".invoice-analyzer.properties";

    // Configuration properties
    private File emailDirectory = new File(System.getProperty("user.home"), "EmailArchive");
    private File outputDirectory = new File(System.getProperty("user.home"), "InvoiceReports");
    private int processingIntervalHours = 2;
    private boolean automaticProcessing = true;

    // Ollama configuration
    private boolean useOllamaFallback = true;
    private String ollamaHost = "http://localhost:11434";
    private String ollamaModel = "llama3.2";
    private int ollamaMaxTokens = 4096;

    /**
     * Create a new InvoiceConfiguration
     */
    public InvoiceConfiguration() {
        loadConfiguration();
    }

    /**
     * Check if the configuration is complete and valid
     * @return true if configuration is complete and directories exist
     */
    public boolean isConfigurationComplete() {
        return emailDirectory != null && emailDirectory.exists() &&
                outputDirectory != null && outputDirectory.exists() &&
                !emailDirectory.getAbsolutePath().isEmpty() &&
                !outputDirectory.getAbsolutePath().isEmpty();
    }

    @Override
    public List<ConfigItem> getConfigItems() {
        List<ConfigItem> items = new ArrayList<>();

        // Directory settings
        items.add(new DirectoryConfigItem("emailDirectory", "Email Archive Directory", emailDirectory));
        items.add(new DirectoryConfigItem("outputDirectory", "Reports Output Directory", outputDirectory));

        // Processing settings
        items.add(new CheckboxConfigItem("automaticProcessing", "Automatically process invoices", automaticProcessing));

        List<String> intervals = new ArrayList<>();
        intervals.add("1 hour");
        intervals.add("2 hours");
        intervals.add("4 hours");
        intervals.add("8 hours");
        intervals.add("12 hours");
        intervals.add("24 hours");

        String interval = getProcessingIntervalDisplay();

        items.add(new DropdownConfigItem("processingInterval", "Process invoices every", interval, intervals));

        // Add a section for Ollama settings
        items.add(new CheckboxConfigItem("useOllamaFallback", "Use Ollama when rule-based parsing fails", useOllamaFallback));
        items.add(new TextConfigItem("ollamaHost", "Ollama Host URL", ollamaHost));
        items.add(new TextConfigItem("ollamaModel", "Ollama Model Name", ollamaModel));
        items.add(new NumberConfigItem("ollamaMaxTokens", "Max Response Tokens", ollamaMaxTokens));

        return items;
    }

    @Override
    public void applyConfiguration(Map<String, Object> configValues) {
        if (configValues.get("emailDirectory") instanceof File) {
            this.emailDirectory = (File) configValues.get("emailDirectory");
        } else if (configValues.get("emailDirectory") instanceof String) {
            this.emailDirectory = new File((String) configValues.get("emailDirectory"));
        }

        if (configValues.get("outputDirectory") instanceof File) {
            this.outputDirectory = (File) configValues.get("outputDirectory");
        } else if (configValues.get("outputDirectory") instanceof String) {
            this.outputDirectory = new File((String) configValues.get("outputDirectory"));
        }

        if (configValues.get("automaticProcessing") instanceof Boolean) {
            this.automaticProcessing = (Boolean) configValues.get("automaticProcessing");
        }

        // Parse processing interval
        String processingIntervalStr = (String) configValues.get("processingInterval");
        if (processingIntervalStr != null) {
            this.processingIntervalHours = parseProcessingInterval(processingIntervalStr);
        }

        // Apply Ollama settings
        if (configValues.get("useOllamaFallback") instanceof Boolean) {
            this.useOllamaFallback = (Boolean) configValues.get("useOllamaFallback");
        }

        if (configValues.get("ollamaHost") instanceof String) {
            this.ollamaHost = (String) configValues.get("ollamaHost");
        }

        if (configValues.get("ollamaModel") instanceof String) {
            this.ollamaModel = (String) configValues.get("ollamaModel");
        }

        if (configValues.get("ollamaMaxTokens") instanceof Number) {
            this.ollamaMaxTokens = ((Number) configValues.get("ollamaMaxTokens")).intValue();
        }

        // Save to file
        saveConfiguration();

        // Create directories if they don't exist
        if (!emailDirectory.exists()) {
            emailDirectory.mkdirs();
        }

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
    }

    @Override
    public Map<String, Object> getCurrentValues() {
        Map<String, Object> values = new HashMap<>();
        values.put("emailDirectory", emailDirectory);
        values.put("outputDirectory", outputDirectory);
        values.put("automaticProcessing", automaticProcessing);
        values.put("processingInterval", getProcessingIntervalDisplay());

        // Ollama settings
        values.put("useOllamaFallback", useOllamaFallback);
        values.put("ollamaHost", ollamaHost);
        values.put("ollamaModel", ollamaModel);
        values.put("ollamaMaxTokens", ollamaMaxTokens);

        return values;
    }

    // Helper method to load configuration from file
    private void loadConfiguration() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);

                // Load properties
                String emailDirStr = props.getProperty("email.directory");
                if (emailDirStr != null && !emailDirStr.isEmpty()) {
                    emailDirectory = new File(emailDirStr);
                }

                String outputDirStr = props.getProperty("output.directory");
                if (outputDirStr != null && !outputDirStr.isEmpty()) {
                    outputDirectory = new File(outputDirStr);
                }

                String automaticProcessingStr = props.getProperty("automatic.processing");
                if (automaticProcessingStr != null) {
                    automaticProcessing = Boolean.parseBoolean(automaticProcessingStr);
                }

                String processingIntervalStr = props.getProperty("processing.interval.hours");
                if (processingIntervalStr != null && !processingIntervalStr.isEmpty()) {
                    try {
                        processingIntervalHours = Integer.parseInt(processingIntervalStr);
                    } catch (NumberFormatException e) {
                        LOGGER.log(Level.WARNING, "Invalid processing interval in config: " + processingIntervalStr, e);
                    }
                }

                // Load Ollama settings
                String useOllamaFallbackStr = props.getProperty("use.ollama.fallback");
                if (useOllamaFallbackStr != null) {
                    useOllamaFallback = Boolean.parseBoolean(useOllamaFallbackStr);
                }

                String ollamaHostStr = props.getProperty("ollama.host");
                if (ollamaHostStr != null && !ollamaHostStr.isEmpty()) {
                    ollamaHost = ollamaHostStr;
                }

                String ollamaModelStr = props.getProperty("ollama.model");
                if (ollamaModelStr != null && !ollamaModelStr.isEmpty()) {
                    ollamaModel = ollamaModelStr;
                }

                String ollamaMaxTokensStr = props.getProperty("ollama.max.tokens");
                if (ollamaMaxTokensStr != null && !ollamaMaxTokensStr.isEmpty()) {
                    try {
                        ollamaMaxTokens = Integer.parseInt(ollamaMaxTokensStr);
                    } catch (NumberFormatException e) {
                        LOGGER.log(Level.WARNING, "Invalid Ollama max tokens in config: " + ollamaMaxTokensStr, e);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error loading configuration", e);
            }
        }
    }

    // Helper method to save configuration to file
    private void saveConfiguration() {
        Properties props = new Properties();

        props.setProperty("email.directory", emailDirectory.getAbsolutePath());
        props.setProperty("output.directory", outputDirectory.getAbsolutePath());
        props.setProperty("automatic.processing", String.valueOf(automaticProcessing));
        props.setProperty("processing.interval.hours", String.valueOf(processingIntervalHours));

        // Save Ollama settings
        props.setProperty("use.ollama.fallback", String.valueOf(useOllamaFallback));
        props.setProperty("ollama.host", ollamaHost);
        props.setProperty("ollama.model", ollamaModel);
        props.setProperty("ollama.max.tokens", String.valueOf(ollamaMaxTokens));

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Invoice Analyzer Configuration");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving configuration", e);
        }
    }

    // Helper method to parse processing interval from display string
    private int parseProcessingInterval(String intervalStr) {
        switch (intervalStr) {
            case "1 hour": return 1;
            case "2 hours": return 2;
            case "4 hours": return 4;
            case "8 hours": return 8;
            case "12 hours": return 12;
            case "24 hours": return 24;
            default: return 2;
        }
    }

    // Helper method to get processing interval display string
    private String getProcessingIntervalDisplay() {
        if (processingIntervalHours == 1) return "1 hour";
        if (processingIntervalHours == 2) return "2 hours";
        if (processingIntervalHours == 4) return "4 hours";
        if (processingIntervalHours == 8) return "8 hours";
        if (processingIntervalHours == 12) return "12 hours";
        if (processingIntervalHours == 24) return "24 hours";
        return "2 hours";
    }

    // Getters for business logic
    public File getEmailDirectory() { return emailDirectory; }
    public File getOutputDirectory() { return outputDirectory; }
    public int getProcessingIntervalHours() { return processingIntervalHours; }
    public boolean isAutomaticProcessing() { return automaticProcessing; }

    // Getters for Ollama configuration
    public boolean isUseOllamaFallback() { return useOllamaFallback; }
    public String getOllamaHost() { return ollamaHost; }
    public String getOllamaModel() { return ollamaModel; }
    public int getOllamaMaxTokens() { return ollamaMaxTokens; }
}