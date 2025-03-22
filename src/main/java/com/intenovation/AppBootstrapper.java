package com.intenovation;

import com.intenovation.appfw.systemtray.*;
import com.intenovation.appfw.ui.SwingUIService;
import com.intenovation.appfw.ui.UIService;
import com.intenovation.email.downloader.EmailConfiguration;
import com.intenovation.email.downloader.EmailCleanup;
import com.intenovation.email.downloader.EmailDownloader;
import com.intenovation.email.downloader.EmailDownloaderYearFilter;
import com.intenovation.email.downloader.ImapDownloader;
import com.intenovation.email.ui.EmailBrowserIntegration;
import com.intenovation.invoice.InvoiceAnalyzerApp;
import com.intenovation.invoice.InvoiceConfiguration;
import com.intenovation.invoice.InvoiceProcessor;
import com.intenovation.invoice.EnhancedInvoiceProcessor;

import java.time.Year;
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
    private static SystemTrayApp systemTrayApp;

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

            // Create the email browser integration
            EmailBrowserIntegration emailBrowser = new EmailBrowserIntegration(
                    emailConfig.getEmailDirectory(), uiService);

            // Create a single AppConfig
            AppConfig appConfig = createAppConfig(emailDownloader, invoiceAnalyzer, emailBrowser);

            // Create a combined menu
            List<MenuCategory> menuCategories = createCombinedMenu(
                    emailDownloader, invoiceAnalyzer, emailBrowser);

            // Create all background tasks
            List<BackgroundTask> tasks = createCombinedTasks(emailConfig, invoiceConfig, uiService);

            // Create a single SystemTrayApp instance
            systemTrayApp = new SystemTrayApp(appConfig, menuCategories, tasks);

            // Register the system tray app with components so they can access it
            emailDownloader.setSystemTrayApp(systemTrayApp);
            invoiceAnalyzer.setSystemTrayApp(systemTrayApp);
            emailBrowser.setSystemTrayApp(systemTrayApp);

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
    private static AppConfig createAppConfig(ImapDownloader emailDownloader,
                                             InvoiceAnalyzerApp invoiceAnalyzer,
                                             EmailBrowserIntegration emailBrowser) {
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
     * Create a combined menu from all applications
     */
    private static List<MenuCategory> createCombinedMenu(ImapDownloader emailDownloader,
                                                         InvoiceAnalyzerApp invoiceAnalyzer,
                                                         EmailBrowserIntegration emailBrowser) {
        List<MenuCategory> categories = new ArrayList<>();

        // Email menu category with new year-based options
        categories.add(new CategoryBuilder("Email")
                .addAction("Sync New Emails", emailDownloader::syncNewEmailsNow)
                .addAction("Sync Current Year Emails", () -> startYearTask(Year.now().getValue()))
                .addAction("Sync Last Year's Emails", () -> startYearTask(Year.now().getValue() - 1))
                .addAction("Sync Last 5 Years", () -> startYearTask(Year.now().getValue() - 5))
                .addAction("Sync All Emails", () -> startYearTask(0))
                .addAction("Configure IMAP Settings", emailDownloader::showConfigDialog)
                .addAction("Check Server Status", emailDownloader::checkServerStatus)
                .addAction("View Storage Usage", emailDownloader::showStorageUsage)
                .addAction("Open Email Archive", emailDownloader::openEmailArchive)
                .build());

        // Add Email Browser menu category
        categories.add(emailBrowser.createMenuCategory());

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
                .addAction("About Email Browser", emailBrowser.getEmailBrowserApp()::showAboutDialog)
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

        // Email tasks - added year-based email downloaders
        tasks.add(new EmailDownloader()); // Original full sync task
        tasks.add(new EmailDownloader(emailConfig.getSyncIntervalMinutes())); // Incremental sync

        // Add year-specific email downloaders
        int currentYear = Year.now().getValue();
        tasks.add(new EmailDownloaderYearFilter(currentYear)); // Current year only
        tasks.add(new EmailDownloaderYearFilter(currentYear - 1)); // Last year only
        tasks.add(new EmailDownloaderYearFilter(currentYear - 5)); // Last 5 years
        tasks.add(new EmailDownloaderYearFilter(0)); // Full sync with year filter (same as regular)

        // Cleanup task
        tasks.add(new EmailCleanup(emailConfig.getCleanupIntervalHours()));

        // Invoice tasks
        tasks.add(new InvoiceProcessor(invoiceConfig, uiService));

        // Add the Enhanced Invoice Processor
        tasks.add(new EnhancedInvoiceProcessor(invoiceConfig, uiService));

        return tasks;
    }

    /**
     * Start downloading emails from a specific year
     *
     * @param year The year to start from (0 for all years)
     */
    private static void startYearTask(int year) {
        // Find the appropriate task name based on the year
        String taskName;
        if (year == 0) {
            taskName = "Full Email Sync";
        } else if (year == Year.now().getValue()) {
            taskName = "Current Year Email Sync";
        } else if (year == Year.now().getValue() - 1) {
            taskName = "Last Year Email Sync";
        } else if (year == Year.now().getValue() - 5) {
            taskName = "Last 5 Years Email Sync";
        } else {
            taskName = "Year " + year + " Email Sync";
        }

        // Start the task with the system tray app
        if (systemTrayApp != null) {
            LOGGER.info("Starting task: " + taskName);
            systemTrayApp.startTask(taskName);
        } else {
            LOGGER.warning("Cannot start task: " + taskName + " - systemTrayApp is null");
        }
    }
}