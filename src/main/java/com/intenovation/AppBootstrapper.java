package com.intenovation;

import com.intenovation.appfw.ui.SwingUIService;
import com.intenovation.appfw.ui.UIService;
import com.intenovation.email.downloader.EmailConfiguration;
import com.intenovation.email.downloader.ImapDownloader;
import com.intenovation.invoice.InvoiceAnalyzerApp;
import com.intenovation.invoice.InvoiceConfiguration;

/**
 * Main application bootstrapper that initializes all components
 */
public class AppBootstrapper {
    
    /**
     * Main entry point
     */
    public static void main(String[] args) {
        // Create UI service
        UIService uiService = new SwingUIService();
        
        // Create configurations
        EmailConfiguration emailConfig = new EmailConfiguration();
        InvoiceConfiguration invoiceConfig = new InvoiceConfiguration();
        
        // Create and initialize the email downloader
        ImapDownloader emailDownloader = new ImapDownloader(emailConfig, uiService);
        emailDownloader.initialize();
        
        // Create and initialize the invoice analyzer
        InvoiceAnalyzerApp invoiceAnalyzer = new InvoiceAnalyzerApp(invoiceConfig, uiService);
        invoiceAnalyzer.initialize();
    }
}