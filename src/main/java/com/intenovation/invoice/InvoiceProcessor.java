package com.intenovation.invoice;

import com.intenovation.appfw.systemtray.AbstractBackgroundTask;
import com.intenovation.appfw.systemtray.ProgressCallback;
import com.intenovation.appfw.systemtray.StatusCallback;
import com.intenovation.email.reader.LocalMail;
import com.intenovation.invoice.Invoice;
import com.intenovation.invoice.Type;

import javax.mail.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    public String execute(ProgressCallback progressCallback, StatusCallback statusCallback)
            throws InterruptedException {
        statusCallback.updateStatus("Initializing invoice processor...");
        progressCallback.updateProgress(0);

        List<Invoice> invoices = new ArrayList<>();
        Store store = null;

        try {
            // Step 1: Open the local mail store
            statusCallback.updateStatus("Opening local mail store...");
            progressCallback.updateProgress(5);

            store = LocalMail.openStore(emailDirectory);
            statusCallback.updateStatus("Connected to local email store");

            // Step 2: Get all folders
            Folder rootFolder = store.getDefaultFolder();
            Folder[] folders = rootFolder.list();

            statusCallback.updateStatus("Found " + folders.length + " folders");
            progressCallback.updateProgress(10);

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

                    statusCallback.updateStatus("Scanning folder: " + folder.getFullName() +
                            " (" + folder.getMessageCount() + " messages)");

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
                    progressCallback.updateProgress(scanProgress);

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing folder " + folder.getFullName(), e);
                }
            }

            statusCallback.updateStatus("Found " + potentialInvoiceMessages.size() + " potential invoice messages");
            progressCallback.updateProgress(40);

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
                    progressCallback.updateProgress(Math.min(90, messageProgress));

                    // Update status periodically
                    if (processedMessages % 10 == 0 || processedMessages == potentialInvoiceMessages.size()) {
                        statusCallback.updateStatus("Processed " + processedMessages + " of " +
                                potentialInvoiceMessages.size() + " messages, found " +
                                invoicesFound + " invoices");
                    }

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing message", e);
                }
            }

            // Step 5: Generate reports
            statusCallback.updateStatus("Generating invoice reports...");
            progressCallback.updateProgress(90);

            if (invoices.isEmpty()) {
                statusCallback.updateStatus("No invoices found");
                progressCallback.updateProgress(100);
                return "No invoices found in the email archive";
            }

            // Generate invoice reports
            String reportResult = generateReports(invoices);

            progressCallback.updateProgress(100);
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

    /**
     * Process a message to extract invoice information
     */
    private Invoice processMessage(Message message) throws MessagingException {
        try {
            // Get the subject
            String subject = message.getSubject();
            if (subject == null) {
                return null;
            }

            // Check content to determine if this is an invoice
            Object content = message.getContent();
            String textContent = "";

            if (content instanceof String) {
                textContent = (String) content;
            } else {
                // Skip complex content for now
                return null;
            }

            // Determine document type
            Type documentType = Type.detectType(textContent + " " + subject);

            // Only process invoices, receipts, and statements
            if (documentType != Type.Invoice &&
                    documentType != Type.Receipt &&
                    documentType != Type.Statement) {
                return null;
            }

            // Create a new invoice object
            Invoice invoice = new Invoice();

            // Set basic properties
            invoice.subject = subject;
            invoice.type = documentType;

            // Generate a unique email ID
            invoice.emailId = UUID.randomUUID().toString().substring(0, 8);

            // Get message folder and ID
            Folder folder = message.getFolder();
            if (folder != null) {
                invoice.fileName = folder.getFullName() + "/" + message.getMessageNumber();
            }

            // Set email information
            Address[] fromAddresses = message.getFrom();
            if (fromAddresses != null && fromAddresses.length > 0) {
                invoice.email = fromAddresses[0].toString();

                // Try to determine city and utility from email domain
                String domain = extractDomain(invoice.email);
                if (domain != null) {
                    invoice.utility = domain.split("\\.")[0];
                    invoice.city = "unknown";
                }
            }

            // Extract dates
            Date sentDate = message.getSentDate();
            if (sentDate != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(sentDate);
                invoice.year = cal.get(Calendar.YEAR);
                invoice.month = cal.get(Calendar.MONTH) + 1; // Calendar months are 0-based
                invoice.day = cal.get(Calendar.DAY_OF_MONTH);
                invoice.date = new SimpleDateFormat("yyyy-MM-dd").format(sentDate);
            }

            // Extract invoice information from content
            extractInvoiceDetails(invoice, textContent);

            return invoice;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing message: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract invoice details from text content
     */
    private void extractInvoiceDetails(Invoice invoice, String content) {
        // Extract amount
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(content);
        if (amountMatcher.find()) {
            try {
                String amountStr = amountMatcher.group(2).replace(",", ".");
                invoice.amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }

        // Extract invoice number
        Matcher invoiceNumberMatcher = INVOICE_NUMBER_PATTERN.matcher(content);
        if (invoiceNumberMatcher.find()) {
            invoice.number = invoiceNumberMatcher.group(2);
        }

        // Extract account number
        Matcher accountNumberMatcher = ACCOUNT_NUMBER_PATTERN.matcher(content);
        if (accountNumberMatcher.find()) {
            invoice.account = accountNumberMatcher.group(2);
        }

        // Extract due date
        Matcher dueDateMatcher = DUE_DATE_PATTERN.matcher(content);
        if (dueDateMatcher.find()) {
            invoice.dueDate = dueDateMatcher.group(2);
        } else {
            // If no due date found, use regular date matcher as fallback
            Matcher dateMatcher = DATE_PATTERN.matcher(content);
            if (dateMatcher.find()) {
                invoice.dueDate = dateMatcher.group(2);
            }
        }

        // Add the first 100 characters of content as parse snippet
        invoice.parse = content.length() > 100 ? content.substring(0, 100) : content;
    }

    /**
     * Generate reports from the extracted invoice data
     */
    private String generateReports(List<Invoice> invoices) throws IOException {
        // Create the output files
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File csvFile = new File(outputDirectory, "invoices-" + timestamp + ".csv");
        File summaryFile = new File(outputDirectory, "invoice-summary-" + timestamp + ".txt");

        // Write CSV file
        try (FileWriter writer = new FileWriter(csvFile)) {
            // Write header
            writer.write(Invoice.header());

            // Write invoice data
            for (Invoice invoice : invoices) {
                writer.write(invoice.toString());
            }
        }

        // Write summary file
        try (FileWriter writer = new FileWriter(summaryFile)) {
            writer.write("Invoice Summary - " + new Date() + "\n");
            writer.write("Total invoices found: " + invoices.size() + "\n\n");

            // Summary by type
            Map<Type, Integer> typeCount = new HashMap<>();
            for (Invoice invoice : invoices) {
                typeCount.put(invoice.type, typeCount.getOrDefault(invoice.type, 0) + 1);
            }

            writer.write("By Document Type:\n");
            for (Map.Entry<Type, Integer> entry : typeCount.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
            }
            writer.write("\n");

            // Summary by sender
            Map<String, Integer> senderCount = new HashMap<>();
            for (Invoice invoice : invoices) {
                String domain = extractDomain(invoice.email);
                if (domain != null) {
                    senderCount.put(domain, senderCount.getOrDefault(domain, 0) + 1);
                }
            }

            writer.write("By Sender Domain:\n");
            List<Map.Entry<String, Integer>> sortedSenders = new ArrayList<>(senderCount.entrySet());
            sortedSenders.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

            for (Map.Entry<String, Integer> entry : sortedSenders) {
                writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
            }
            writer.write("\n");

            // Total amount
            double totalAmount = 0;
            for (Invoice invoice : invoices) {
                totalAmount += invoice.amount;
            }

            writer.write("Total Amount: $" + String.format("%.2f", totalAmount) + "\n");
        }

        return "Found " + invoices.size() + " invoices\n" +
                "CSV saved to: " + csvFile.getAbsolutePath() + "\n" +
                "Summary saved to: " + summaryFile.getAbsolutePath();
    }

    /**
     * Extract domain from email address
     */
    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return null;
        }

        return parts[1];
    }
}