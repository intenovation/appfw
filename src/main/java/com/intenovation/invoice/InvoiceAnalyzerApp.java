package com.intenovation.invoice;

import com.intenovation.appfw.systemtray.*;
import com.intenovation.appfw.ui.UIService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    // Dependencies
    private final InvoiceConfiguration config;
    private final UIService uiService;

    // SystemTrayApp instance
    private SystemTrayApp systemTrayApp;

    /**
     * Create a new InvoiceAnalyzerApp
     *
     * @param config Invoice configuration
     * @param uiService UI service
     */
    public InvoiceAnalyzerApp(InvoiceConfiguration config, UIService uiService) {
        this.config = config;
        this.uiService = uiService;
    }

    /**
     * Initialize the application
     */
    public void initialize() {
        try {
            // Create the directories if they don't exist
            if (!config.getEmailDirectory().exists()) {
                config.getEmailDirectory().mkdirs();
            }
            if (!config.getOutputDirectory().exists()) {
                config.getOutputDirectory().mkdirs();
            }

            // Create app configuration
            AppConfig appConfig = createAppConfig();

            // Create menu categories
            List<MenuCategory> menuCategories = createMenuCategories();

            // Create tasks
            List<BackgroundTask> tasks = createTasks();

            // Create the system tray app
            systemTrayApp = new SystemTrayApp(appConfig, menuCategories, tasks);
            LOGGER.info("Invoice Analyzer started");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Application startup failed", e);
            uiService.showError("Error", "Failed to start: " + e.getMessage());
        }
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
        settingsBuilder.addAction("Open Email Directory", () -> openEmailDirectory());
        settingsBuilder.addAction("Open Reports Directory", () -> openReportsDirectory());
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
    private List<BackgroundTask> createTasks() {
        List<BackgroundTask> tasks = new ArrayList<>();

        // Create the invoice processor that extends BackgroundTask
        tasks.add(new InvoiceProcessor(config, uiService));

        return tasks;
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
    private boolean showConfigDialog() {
        return uiService.showConfigDialog("Invoice Analyzer Configuration", config);
    }

    /**
     * Open the email directory
     */
    private void openEmailDirectory() {
        if (!uiService.openDirectory(config.getEmailDirectory())) {
            uiService.showError("Error", "Could not open email directory: " +
                    config.getEmailDirectory().getAbsolutePath());
        }
    }

    /**
     * Open the reports directory
     */
    private void openReportsDirectory() {
        if (!uiService.openDirectory(config.getOutputDirectory())) {
            uiService.showError("Error", "Could not open reports directory: " +
                    config.getOutputDirectory().getAbsolutePath());
        }
    }

    /**
     * Show the about dialog
     */
    private void showAboutDialog() {
        uiService.showInfo("About " + APP_NAME,
                APP_NAME + " Version " + VERSION + "\n\n" +
                        "Automatic invoice detection and processing from email archives.\n\n" +
                        "Email Directory: " + config.getEmailDirectory() + "\n" +
                        "Output Directory: " + config.getOutputDirectory());
    }

    /**
     * Generate a sample invoice (for testing)
     */
    private void generateSampleInvoice() {
        // Create a sample email with invoice content
        try {
            // Create a directory for the sample
            File sampleDir = new File(config.getEmailDirectory(), "INBOX");
            if (!sampleDir.exists()) {
                sampleDir.mkdirs();
            }

            // Create a messages directory (for the new structure)
            File messagesDir = new File(sampleDir, "messages");
            if (!messagesDir.exists()) {
                messagesDir.mkdirs();
            }

            // Create a directory for the message
            String messageId = "sample-invoice-" + System.currentTimeMillis();
            File messageDir = new File(messagesDir, messageId);
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
            Files.write(contentFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

            uiService.showInfo("Sample Created",
                    "A sample invoice email has been created in your email archive.");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error creating sample invoice", e);
            uiService.showError("Error", "Failed to create sample invoice: " + e.getMessage());
        }
    }
}