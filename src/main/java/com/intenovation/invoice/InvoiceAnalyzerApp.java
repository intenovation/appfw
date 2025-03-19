package com.intenovation.invoice;

import com.intenovation.appfw.systemtray.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application class for the Invoice Analyzer.
 * This integrates the InvoiceProcessor with the SystemTrayApp framework.
 */
public class InvoiceAnalyzerApp {
    private static final Logger LOGGER = Logger.getLogger(InvoiceAnalyzerApp.class.getName());
    private static final String APP_NAME = "Invoice Analyzer";
    private static final String VERSION = "1.0.0";

    // Configuration
    private File emailDirectory;
    private File outputDirectory;
    private Properties config;

    // SystemTrayApp instance
    private SystemTrayApp systemTrayApp;

    /**
     * Main entry point
     */
    public static void main(String[] args) {
        try {
            InvoiceAnalyzerApp app = new InvoiceAnalyzerApp();
            app.initialize();
            app.start();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Application startup failed", e);
            SystemTrayApp.showError("Error", "Failed to start: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Initialize the application
     */
    public void initialize() throws IOException {
        // Load configuration
        loadConfiguration();

        // Create the directories if they don't exist
        if (!emailDirectory.exists()) {
            emailDirectory.mkdirs();
        }
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
    }

    /**
     * Start the application
     */
    public void start() {
        // Create app configuration
        AppConfig appConfig = createAppConfig();

        // Create menu categories
        List<MenuCategory> menuCategories = createMenuCategories();

        // Create tasks
        List<BackgroundTask> tasks = createTasks();

        // Create the system tray app
        systemTrayApp = new SystemTrayApp(appConfig, menuCategories, tasks);
        LOGGER.info("Invoice Analyzer started");
    }

    /**
     * Create application configuration
     */
    private AppConfig createAppConfig() {
        return new AppConfig() {
            @Override
            public String getAppName() {
                return APP_NAME;
            }

            @Override
            public String getIconPath() {
                return "/intenovation.png";  // Make sure this exists in your resources
            }

            @Override
            public void onIconDoubleClick() {
                showStatusDialog();
            }
        };
    }

    /**
     * Create menu categories
     */
    private List<MenuCategory> createMenuCategories() {
        List<MenuCategory> categories = new ArrayList<>();

        // Settings category
        CategoryBuilder settingsBuilder = new CategoryBuilder("Settings");
        settingsBuilder.addAction("Configure Directories", this::showConfigDialog);
        settingsBuilder.addAction("Open Email Directory", () -> openDirectory(emailDirectory));
        settingsBuilder.addAction("Open Reports Directory", () -> openDirectory(outputDirectory));
        categories.add(settingsBuilder.build());

        // Actions category
        CategoryBuilder actionsBuilder = new CategoryBuilder("Actions");
        actionsBuilder.addAction("Run Invoice Analysis Now", () -> systemTrayApp.startTask("Invoice Processor"));
        actionsBuilder.addAction("Generate Sample Invoice", this::generateSampleInvoice);
        categories.add(actionsBuilder.build());

        // Help category
        CategoryBuilder helpBuilder = new CategoryBuilder("Help");
        helpBuilder.addAction("About", this::showAboutDialog);
        categories.add(helpBuilder.build());

        return categories;
    }

    /**
     * Create tasks
     */
    /**
     * Create tasks
     */
    private List<BackgroundTask> createTasks() {
        List<BackgroundTask> tasks = new ArrayList<>();

        // Create the invoice processor task with logging
        final InvoiceProcessor processor = new InvoiceProcessor(emailDirectory, outputDirectory);

        // Create a task that adapts to the new ProgressStatusCallback interface
        BackgroundTask processorTask = new SimpleTask(
                "Invoice Processor",
                "Analyzes emails to extract invoice information",
                processor.getIntervalSeconds(),
                true,
                // Task executor with logging
                (callback) -> {
                    // Log all progress and status updates
                    LOGGER.info("Starting Invoice Processor task");

                    // Create a wrapped callback that logs as well as updates
                    ProgressStatusCallback loggingCallback = new ProgressStatusCallback() {
                        @Override
                        public void update(int percent, String message) {
                            // First update the original callback
                            callback.update(percent, message);

                            // Then log the progress
                            LOGGER.info(String.format("[Invoice Processor] %d%% - %s", percent, message));
                        }
                    };

                    // Run the processor with our logging callback
                    try {
                        String result = processor.execute(loggingCallback);
                        LOGGER.info("Invoice Processor completed: " + result);
                        return result;
                    } catch (InterruptedException e) {
                        LOGGER.warning("Invoice Processor was interrupted");
                        throw e;
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Invoice Processor error", e);
                        return "Error: " + e.getMessage();
                    }
                }
        );

        tasks.add(processorTask);
        return tasks;
    }

    /**
     * Load configuration from config file
     */
    private void loadConfiguration() throws IOException {
        config = new Properties();

        // Set defaults
        String userHome = System.getProperty("user.home");
        emailDirectory = new File(userHome, "EmailArchive");
        outputDirectory = new File(userHome, "InvoiceReports");

        // Try to load from config file
        File configFile = new File(userHome, ".invoice-analyzer.properties");
        if (configFile.exists()) {
            config.load(Files.newInputStream(configFile.toPath()));

            // Override defaults if found in config
            String emailDirPath = config.getProperty("email.directory");
            if (emailDirPath != null && !emailDirPath.isEmpty()) {
                emailDirectory = new File(emailDirPath);
            }

            String outputDirPath = config.getProperty("output.directory");
            if (outputDirPath != null && !outputDirPath.isEmpty()) {
                outputDirectory = new File(outputDirPath);
            }
        }

        // Save the configuration (in case it didn't exist)
        saveConfiguration();
    }

    /**
     * Save configuration to config file
     */
    private void saveConfiguration() throws IOException {
        // Update properties
        config.setProperty("email.directory", emailDirectory.getAbsolutePath());
        config.setProperty("output.directory", outputDirectory.getAbsolutePath());

        // Save to file
        File configFile = new File(System.getProperty("user.home"), ".invoice-analyzer.properties");
        config.store(Files.newOutputStream(configFile.toPath()), "Invoice Analyzer Configuration");
    }

    /**
     * Show the status dialog
     */
    private void showStatusDialog() {
        systemTrayApp.showTaskStatus();
    }

    /**
     * Show the configuration dialog
     */
    private void showConfigDialog() {
        JTextField emailField = new JTextField(emailDirectory.getAbsolutePath(), 30);
        JTextField outputField = new JTextField(outputDirectory.getAbsolutePath(), 30);

        JButton emailBrowseButton = new JButton("Browse...");
        JButton outputBrowseButton = new JButton("Browse...");

        JPanel emailPanel = new JPanel();
        emailPanel.setLayout(new BoxLayout(emailPanel, BoxLayout.X_AXIS));
        emailPanel.add(emailField);
        emailPanel.add(emailBrowseButton);

        JPanel outputPanel = new JPanel();
        outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.X_AXIS));
        outputPanel.add(outputField);
        outputPanel.add(outputBrowseButton);

        emailBrowseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(new File(emailField.getText()));

            if (chooser.showDialog(null, "Select") == JFileChooser.APPROVE_OPTION) {
                emailField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        outputBrowseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(new File(outputField.getText()));

            if (chooser.showDialog(null, "Select") == JFileChooser.APPROVE_OPTION) {
                outputField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        Object[] message = {
                "Email Directory:", emailPanel,
                "Output Directory:", outputPanel
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Directory Configuration",
                JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            // Update directories
            emailDirectory = new File(emailField.getText());
            outputDirectory = new File(outputField.getText());

            // Create directories if they don't exist
            if (!emailDirectory.exists()) {
                emailDirectory.mkdirs();
            }
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            try {
                saveConfiguration();
                SystemTrayApp.showMessage("Configuration Updated",
                        "Directory settings have been updated. The changes will take effect immediately.");

                // Restart the application to apply changes
                restartApp();

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error saving configuration", e);
                SystemTrayApp.showError("Error", "Failed to save configuration: " + e.getMessage());
            }
        }
    }

    /**
     * Restart the application to apply configuration changes
     */
    private void restartApp() {
        // For now, just show a message
        // In a real application, you would implement proper restart functionality
        SystemTrayApp.showMessage("Restart Required",
                "Please restart the application for the changes to take full effect.");
    }

    /**
     * Show the about dialog
     */
    private void showAboutDialog() {
        SystemTrayApp.showMessage("About " + APP_NAME,
                APP_NAME + " Version " + VERSION + "\n\n" +
                        "Automatic invoice detection and processing from email archives.\n\n" +
                        "Email Directory: " + emailDirectory + "\n" +
                        "Output Directory: " + outputDirectory);
    }

    /**
     * Generate a sample invoice (for testing)
     */
    private void generateSampleInvoice() {
        // Create a sample email with invoice content
        try {
            // Create a directory for the sample
            File sampleDir = new File(emailDirectory, "INBOX");
            if (!sampleDir.exists()) {
                sampleDir.mkdirs();
            }

            // Create a directory for the message
            String messageId = "sample-invoice-" + System.currentTimeMillis();
            File messageDir = new File(sampleDir, messageId);
            messageDir.mkdirs();

            // Create message.properties
            Properties props = new Properties();
            props.setProperty("message.id", "<" + messageId + "@example.com>");
            props.setProperty("message.id.folder", messageId);
            props.setProperty("subject", "Your Invoice #INV-12345");
            props.setProperty("from", "billing@example.com");
            props.setProperty("to", "customer@example.com");
            props.setProperty("sent.date", "2023-04-15 10:30:00");
            props.setProperty("received.date", "2023-04-15 10:31:22");

            File propsFile = new File(messageDir, "message.properties");
            props.store(Files.newOutputStream(propsFile.toPath()), "Sample Invoice Email");

            // Create content.txt
            String content = "Dear Customer,\n\n" +
                    "Thank you for your business. Please find your invoice details below:\n\n" +
                    "Invoice Number: INV-12345\n" +
                    "Date: April 15, 2023\n" +
                    "Due Date: May 15, 2023\n\n" +
                    "Amount: $250.75\n\n" +
                    "Payment can be made to account number ACCT-987654.\n\n" +
                    "Best regards,\n" +
                    "Billing Department\n" +
                    "Example Company";

            File contentFile = new File(messageDir, "content.txt");
            Files.write(contentFile.toPath(), content.getBytes());

            SystemTrayApp.showMessage("Sample Created",
                    "A sample invoice email has been created in your email archive.");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error creating sample invoice", e);
            SystemTrayApp.showError("Error", "Failed to create sample invoice: " + e.getMessage());
        }
    }

    /**
     * Open a directory in the file explorer
     */
    private void openDirectory(File directory) {
        try {
            if (!directory.exists()) {
                directory.mkdirs();
            }

            if (!SystemTrayApp.openDirectory(directory)) {
                // Fall back to manual method if the helper method fails
                String os = System.getProperty("os.name").toLowerCase();

                if (os.contains("win")) {
                    Runtime.getRuntime().exec("explorer.exe \"" + directory.getAbsolutePath() + "\"");
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec(new String[]{"open", directory.getAbsolutePath()});
                } else if (os.contains("nix") || os.contains("nux")) {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", directory.getAbsolutePath()});
                } else {
                    throw new IOException("Unsupported operating system");
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening directory", e);
            SystemTrayApp.showError("Error", "Failed to open directory: " + e.getMessage());
        }
    }
}