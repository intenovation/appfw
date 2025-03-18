package com.intenovation.invoice;

import com.intenovation.appfw.systemtray.AbstractBackgroundTask;
import com.intenovation.appfw.systemtray.ProgressStatusCallback;
import com.intenovation.email.reader.LocalMail;

import javax.mail.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Invoice processor that implements the BackgroundTask interface.
 * It scans downloaded emails to identify and extract invoice information.
 */
public class InvoiceProcessor extends AbstractBackgroundTask {
    private static final Logger LOGGER = Logger.getLogger(InvoiceProcessor.class.getName());

    // Regular expressions for extracting invoice information
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?i)(total|amount|sum|betrag|summe)[\\s:]*[$€£]?\\s*([\\d,.]+)");
    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile("(?i)(invoice|rechnung|bill)[\\s:#-]*([A-Z0-9]{4,20})");
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("(?i)(account|konto|customer)[\\s:#-]*([A-Z0-9]{4,20})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(?i)(date|datum)[\\s:]*([0-9]{1,2}[\\s./\\-][0-9]{1,2}[\\s./\\-][0-9]{2,4})");
    private static final Pattern DUE_DATE_PATTERN = Pattern.compile("(?i)(due date|fällig|zahlbar bis)[\\s:]*([0-9]{1,2}[\\s./\\-][0-9]{1,2}[\\s./\\-][0-9]{2,4})");

    // Settings
    private final File emailDirectory;
    private final File outputDirectory;

    /**
     * Create a new invoice processor task
     *
     * @param emailDirectory The directory containing downloaded emails
     * @param outputDirectory The directory to save invoice reports
     */
    public InvoiceProcessor(File emailDirectory, File outputDirectory) {
        super("Invoice Processor", "Processes emails to extract invoice information", 2 * 60 * 60, true);
        this.emailDirectory = emailDirectory;
        this.outputDirectory = outputDirectory;

        // Create output directory if it doesn't exist
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
    }

    @Override
    public String execute(ProgressStatusCallback callback) throws InterruptedException {
        callback.update(0, "Initializing invoice processor...");

        List<Invoice> invoices = new ArrayList<>();
        Store store = null;

        try {
            // Step 1: Open the local mail store
            callback.update(5, "Opening local mail store...");

            store = LocalMail.openStore(emailDirectory);
            callback.update(5, "Connected to local email store");

            // Step 2: Get all folders
            Folder rootFolder = store.getDefaultFolder();
            Folder[] folders = rootFolder.list();

            callback.update(10, "Found " + folders.length + " folders");

            // Count folders with messages (for progress calculation)
            int foldersWithMessages = 0;
            for (Folder folder : folders) {
                if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
                    foldersWithMessages++;
                }
            }

            // Step 3: Scan each folder for potential invoices
            int processedFolders = 0;
            List<Message> potentialInvoiceMessages = new ArrayList<>();

            for (Folder folder : folders) {
                // Check for cancellation
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Task cancelled");
                }

                if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0) {
                    continue;
                }

                try {
                    folder.open(Folder.READ_ONLY);

                    callback.update(10 + (10 * processedFolders / Math.max(1, foldersWithMessages)),
                            "Scanning folder: " + folder.getFullName() + " (" + folder.getMessageCount() + " messages)");

                    // Find potential invoice messages
                    Message[] messages = folder.getMessages();
                    for (Message message : messages) {
                        try {
                            if (isPotentialInvoice(message)) {
                                potentialInvoiceMessages.add(message);
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error checking message", e);
                        }
                    }

                    folder.close(false);
                    processedFolders++;

                    int scanProgress = 10 + (30 * processedFolders / foldersWithMessages);
                    callback.update(scanProgress, "Scanned " + processedFolders + " of " + foldersWithMessages + " folders");

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing folder " + folder.getFullName(), e);
                }
            }

            callback.update(40, "Found " + potentialInvoiceMessages.size() + " potential invoice messages");

            // Step 4: Process potential invoice messages
            int processedMessages = 0;
            int invoicesFound = 0;

            for (Message message : potentialInvoiceMessages) {
                // Check for cancellation
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Task cancelled");
                }

                try {
                    Invoice invoice = processMessage(message);
                    if (invoice != null) {
                        invoices.add(invoice);
                        invoicesFound++;
                    }

                    processedMessages++;

                    // Update progress
                    int messageProgress = 40 + (50 * processedMessages / potentialInvoiceMessages.size());
                    callback.update(Math.min(90, messageProgress),
                            "Processed " + processedMessages + " of " + potentialInvoiceMessages.size() +
                                    " messages, found " + invoicesFound + " invoices");

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing message", e);
                }
            }

            // Step 5: Generate reports
            callback.update(90, "Generating invoice reports...");

            if (invoices.isEmpty()) {
                callback.update(100, "No invoices found");
                return "No invoices found in the email archive";
            }

            // Generate invoice reports
            String reportResult = generateReports(invoices);

            callback.update(100, "Completed: " + reportResult);
            return reportResult;

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing invoices", e);
            return "Error processing invoices: " + e.getMessage();
        } finally {
            // Close the store if it was opened
            if (store != null) {
                try {
                    store.close();
                } catch (MessagingException e) {
                    LOGGER.log(Level.WARNING, "Error closing mail store", e);
                }
            }
        }
    }

    /**
     * Check if a message is potentially an invoice
     */
    private boolean isPotentialInvoice(Message message) throws MessagingException {
        // Check subject for invoice-related keywords
        String subject = message.getSubject();
        if (subject == null) {
            return false;
        }

        String subjectLower = subject.toLowerCase();
        String[] keywords = {
                "invoice", "rechnung", "bill", "statement", "payment", "receipt",
                "zahlung", "beleg", "quittung", "betrag", "amount", "total"
        };

        for (String keyword : keywords) {
            if (subjectLower.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    // Rest of the methods remain the same
    // ...

    /**
     * Process a message to extract invoice information
     */
    private Invoice processMessage(Message message) throws MessagingException {
        // Implementation unchanged - omitted for brevity
        return null; // Placeholder - in real code this would extract and return an Invoice object
    }

    /**
     * Extract invoice details from text content
     */
    private void extractInvoiceDetails(Invoice invoice, String content) {
        // Implementation unchanged - omitted for brevity
    }

    /**
     * Generate reports from the extracted invoice data
     */
    private String generateReports(List<Invoice> invoices) throws IOException {
        // Implementation unchanged - omitted for brevity
        return "Generated reports for " + invoices.size() + " invoices"; // Placeholder
    }

    /**
     * Extract domain from email address
     */
    private String extractDomain(String email) {
        // Implementation unchanged - omitted for brevity
        return null; // Placeholder
    }
}