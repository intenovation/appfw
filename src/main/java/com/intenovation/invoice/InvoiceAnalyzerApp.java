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
    private static final String VERSION = "1.1.0";

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
     * Set the system tray app instance to use
     * @param systemTrayApp The system tray app instance
     */
    public void setSystemTrayApp(SystemTrayApp systemTrayApp) {
        this.systemTrayApp = systemTrayApp;
    }

    /**
     * Initialize the application
     * @deprecated Use setSystemTrayApp instead and let AppBootstrapper manage the system tray
     */
    @Deprecated
    public void initialize() {
        try {
            // Create the directories if they don't exist
            if (!config.getEmailDirectory().exists()) {
                config.getEmailDirectory().mkdirs();
            }
            if (!config.getOutputDirectory().exists()) {
                config.getOutputDirectory().mkdirs();
            }

            // This part is no longer needed as the system tray is managed by AppBootstrapper
            LOGGER.warning("Called deprecated initialize() method. The system tray is now managed by AppBootstrapper.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Application startup failed", e);
            uiService.showError("Error", "Failed to start: " + e.getMessage());
        }
    }

    /**
     * Show the status dialog
     */
    private void showStatusDialog() {
        if (systemTrayApp != null) {
            systemTrayApp.showTaskStatus();
        } else {
            LOGGER.warning("Cannot show status dialog: systemTrayApp is not set");
        }
    }

    /**
     * Run regular invoice analysis
     */
    public void runInvoiceAnalysisNow() {
        if (systemTrayApp != null) {
            systemTrayApp.startTask("Invoice Processor");
        } else {
            LOGGER.warning("Cannot run invoice analysis: systemTrayApp is not set");
        }
    }

    /**
     * Run enhanced invoice analysis
     */
    public void runEnhancedInvoiceAnalysisNow() {
        if (systemTrayApp != null) {
            systemTrayApp.startTask("Enhanced Invoice Processor");
        } else {
            LOGGER.warning("Cannot run enhanced invoice analysis: systemTrayApp is not set");
        }
    }

    /**
     * Show the configuration dialog
     */
    public boolean showConfigDialog() {
        return uiService.showConfigDialog("Invoice Analyzer Configuration", config);
    }

    /**
     * Open the email directory
     */
    public void openEmailDirectory() {
        if (!uiService.openDirectory(config.getEmailDirectory())) {
            uiService.showError("Error", "Could not open email directory: " +
                    config.getEmailDirectory().getAbsolutePath());
        }
    }

    /**
     * Open the reports directory
     */
    public void openReportsDirectory() {
        if (!uiService.openDirectory(config.getOutputDirectory())) {
            uiService.showError("Error", "Could not open reports directory: " +
                    config.getOutputDirectory().getAbsolutePath());
        }
    }

    /**
     * Show the about dialog
     */
    public void showAboutDialog() {
        uiService.showInfo("About " + APP_NAME,
                APP_NAME + " Version " + VERSION + "\n\n" +
                        "Automatic invoice detection and processing from email archives.\n\n" +
                        "Email Directory: " + config.getEmailDirectory() + "\n" +
                        "Output Directory: " + config.getOutputDirectory());
    }

    /**
     * Generate a sample invoice (for testing)
     */
    public void generateSampleInvoice() {
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