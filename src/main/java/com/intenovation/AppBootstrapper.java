package com.intenovation;

import com.intenovation.appfw.systemtray.*;
import com.intenovation.appfw.ui.SwingUIService;
import com.intenovation.appfw.ui.UIService;
import com.intenovation.email.downloader.EmailConfiguration;
import com.intenovation.email.downloader.EmailDownloader;
import com.intenovation.email.downloader.EmailCleanup;
import com.intenovation.email.downloader.ImapDownloader;
import com.intenovation.invoice.InvoiceAnalyzerApp;
import com.intenovation.invoice.InvoiceConfiguration;
import com.intenovation.invoice.InvoiceProcessor;
import com.intenovation.invoice.EnhancedInvoiceProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application bootstrapper that initializes all components
 * and creates a single system tray application
 */
public class AppBootstrapper {
    private static final Logger LOGGER = Logger.getLogger(AppBootstrapper.class.getName());
    private static final String APP_NAME = "Intenovation Suite";

    /**
     * Main entry point
     */
    public static void main(String[] args) {
        try {
            // Create UI service
            UIService uiService = new SwingUIService();

            // Create configurations
            EmailConfiguration emailConfig = new EmailConfiguration();
            InvoiceConfiguration invoiceConfig = new InvoiceConfiguration();

            // Create the app instances but don't initialize them yet
            ImapDownloader emailDownloader = new ImapDownloader(emailConfig, uiService);
            InvoiceAnalyzerApp invoiceAnalyzer = new InvoiceAnalyzerApp(invoiceConfig, uiService);

            // Create a single AppConfig
            AppConfig appConfig = createAppConfig(emailDownloader, invoiceAnalyzer);

            // Create a combined menu
            List<MenuCategory> menuCategories = createCombinedMenu(emailDownloader, invoiceAnalyzer);

            // Create all background tasks
            List<BackgroundTask> tasks = createCombinedTasks(emailConfig, invoiceConfig, uiService);

            // Create a single SystemTrayApp instance
            SystemTrayApp systemTrayApp = new SystemTrayApp(appConfig, menuCategories, tasks);

            // Register the system tray app with components so they can access it
            emailDownloader.setSystemTrayApp(systemTrayApp);
            invoiceAnalyzer.setSystemTrayApp(systemTrayApp);

            // Set the instance in the ImapDownloader's singleton holder
            // This is needed for backward compatibility with existing code
            ImapDownloader.ImapDownloaderInstance.setInstance(emailDownloader);

            LOGGER.info("Intenovation Suite started successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            e.printStackTrace();
        }
    }

    /**
     * Create a single AppConfig for the combined application
     */
    private static AppConfig createAppConfig(ImapDownloader emailDownloader, InvoiceAnalyzerApp invoiceAnalyzer) {
        return new AppConfig() {
            @Override
            public String getAppName() {
                return APP_NAME;
            }

            @Override
            public String getIconPath() {
                return "/intenovation.png";  // Make sure this exists in resources
            }

            @Override
            public void onIconDoubleClick() {
                // Show status dialog when tray icon is double-clicked
                showStatusDialog();
            }

            private void showStatusDialog() {
                // You could add logic here to show a combined status dialog
                // or choose which component's status to show
                emailDownloader.showStatusSummary();
            }
        };
    }

    /**
     * Create a combined menu from both applications
     */
    private static List<MenuCategory> createCombinedMenu(ImapDownloader emailDownloader, InvoiceAnalyzerApp invoiceAnalyzer) {
        List<MenuCategory> categories = new ArrayList<>();

        // Email menu category
        categories.add(new CategoryBuilder("Email")
                .addAction("Sync New Emails", emailDownloader::syncNewEmailsNow)
                .addAction("Configure IMAP Settings", emailDownloader::showConfigDialog)
                .addAction("Check Server Status", emailDownloader::checkServerStatus)
                .addAction("View Storage Usage", emailDownloader::showStorageUsage)
                .addAction("Open Email Archive", emailDownloader::openEmailArchive)
                .build());

        // Invoice menu category
        categories.add(new CategoryBuilder("Invoices")
                .addAction("Run Invoice Analysis", invoiceAnalyzer::runInvoiceAnalysisNow)
                .addAction("Run Enhanced Invoice Analysis", invoiceAnalyzer::runEnhancedInvoiceAnalysisNow)
                .addAction("Configure Invoice Settings", invoiceAnalyzer::showConfigDialog)
                .addAction("Open Reports Directory", invoiceAnalyzer::openReportsDirectory)
                .addAction("Generate Sample Invoice", invoiceAnalyzer::generateSampleInvoice)
                .build());

        // About menu
        categories.add(new CategoryBuilder("About")
                .addAction("About Email Downloader", emailDownloader::showAboutDialog)
                .addAction("About Invoice Analyzer", invoiceAnalyzer::showAboutDialog)
                .build());

        return categories;
    }

    /**
     * Create all background tasks from both applications
     */
    private static List<BackgroundTask> createCombinedTasks(
            EmailConfiguration emailConfig,
            InvoiceConfiguration invoiceConfig,
            UIService uiService) {

        List<BackgroundTask> tasks = new ArrayList<>();

        // Email tasks
        tasks.add(new EmailDownloader()); // Full sync with duplicate detection
        tasks.add(new EmailDownloader(emailConfig.getSyncIntervalMinutes())); // Incremental sync
        tasks.add(new EmailCleanup(emailConfig.getCleanupIntervalHours()));

        // Invoice tasks
        tasks.add(new InvoiceProcessor(invoiceConfig, uiService));
        
        // Add the Enhanced Invoice Processor
        tasks.add(new EnhancedInvoiceProcessor(invoiceConfig, uiService));

        return tasks;
    }
}