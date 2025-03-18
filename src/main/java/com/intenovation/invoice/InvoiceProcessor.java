package com.intenovation.invoice;

import com.intenovation.appfw.systemtray.AbstractBackgroundTask;
import com.intenovation.appfw.systemtray.ProgressStatusCallback;
import com.intenovation.email.reader.LocalMail;

import javax.mail.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
            callback.update(5, "Opening local mail store " + emailDirectory);

            store = LocalMail.openStore(emailDirectory);
            callback.update(10, "Connected to local email store");

            // Step 2: Get the root folder and check if it holds messages
            Folder rootFolder = store.getDefaultFolder();

            // Create a list to hold all folders we need to process
            List<Folder> foldersToProcess = new ArrayList<>();

            // Check if root folder holds messages
            boolean rootHoldsMessages = (rootFolder.getType() & Folder.HOLDS_MESSAGES) != 0;
            if (rootHoldsMessages) {
                foldersToProcess.add(rootFolder);
                LOGGER.info("Root folder holds messages, adding to processing list");
            }

            // Get sub-folders
            Folder[] subFolders = rootFolder.list();
            callback.update(15, "Found " + subFolders.length + " sub-folders" +
                    (rootHoldsMessages ? " (plus root folder)" : ""));

            // Add sub-folders that hold messages
            for (Folder folder : subFolders) {
                if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
                    foldersToProcess.add(folder);
                }
            }

            int foldersWithMessages = foldersToProcess.size();
            callback.update(20, "Found " + foldersWithMessages + " folders with messages");

            // Step 3: Scan each folder for potential invoices
            int processedFolders = 0;
            List<Message> potentialInvoiceMessages = new ArrayList<>();

            for (Folder folder : foldersToProcess) {
                // Check for cancellation
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Task cancelled");
                }

                try {
                    folder.open(Folder.READ_ONLY);

                    callback.update(20 + (20 * processedFolders / Math.max(1, foldersWithMessages)),
                            "Scanning folder: " + folder.getFullName() + " (" + folder.getMessageCount() + " messages)");

                    // Find potential invoice messages
                    Message[] messages = folder.getMessages();
                    for (Message message : messages) {
                        try {
                            if (isPotentialInvoice(message)) {
                                potentialInvoiceMessages.add(message);
                            }
                            else {
                                LOGGER.log(Level.FINE, "Rejecting message", message);
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error checking message", e);
                        }
                    }

                    folder.close(false);
                    processedFolders++;

                    int scanProgress = 20 + (30 * processedFolders / foldersWithMessages);
                    callback.update(scanProgress, "Scanned " + processedFolders + " of " + foldersWithMessages + " folders");

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing folder " + folder.getFullName(), e);
                }
            }

            callback.update(50, "Found " + potentialInvoiceMessages.size() + " potential invoice messages");

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
                    int messageProgress = 50 + (40 * processedMessages / potentialInvoiceMessages.size());
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

    /**
     * Process a message to extract invoice information
     */
    private Invoice processMessage(Message message) throws MessagingException, IOException {
        String subject = message.getSubject();
        if (subject == null) {
            return null;
        }

        // Create a new invoice object
        Invoice invoice = new Invoice();
        invoice.setSubject(subject);

        // Try to extract message ID if available
        String[] messageIdHeaders = message.getHeader("Message-ID");
        if (messageIdHeaders != null && messageIdHeaders.length > 0) {
            invoice.setEmailId(messageIdHeaders[0]);
        }

        // Get sender email
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses != null && fromAddresses.length > 0) {
            String fromEmail = fromAddresses[0].toString();
            invoice.setEmail(fromEmail);

            // Extract domain for utility field
            String domain = extractDomain(fromEmail);
            if (domain != null) {
                invoice.setUtility(domain);
            }
        }

        // Set dates
        Date sentDate = message.getSentDate();
        if (sentDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(sentDate);
            invoice.setYear(cal.get(Calendar.YEAR));
            invoice.setMonth(cal.get(Calendar.MONTH) + 1); // Calendar months are 0-based
            invoice.setDay(cal.get(Calendar.DAY_OF_MONTH));

            // Format date for display
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            invoice.setDate(sdf.format(sentDate));
        }

        // Get content
        Object content = message.getContent();
        String textContent = "";

        if (content instanceof String) {
            textContent = (String) content;
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            textContent = extractTextFromMultipart(multipart);
        } else if (content instanceof InputStream) {
            // Handle input stream
            InputStream is = (InputStream) content;
            StringBuilder sb = new StringBuilder();
            try (Scanner scanner = new Scanner(is, "UTF-8")) {
                while (scanner.hasNextLine()) {
                    sb.append(scanner.nextLine()).append("\n");
                }
            }
            textContent = sb.toString();
        }

        // Detect document type
        invoice.setType(Type.detectType(textContent));

        // Extract invoice details from content
        extractInvoiceDetails(invoice, textContent);

        // Only return if we found some useful information
        if (invoice.getAmount() > 0 || invoice.getNumber() != null && !invoice.getNumber().isEmpty()) {
            return invoice;
        }

        return null;
    }

    /**
     * Extract text content from multipart message
     */
    private String extractTextFromMultipart(Multipart multipart) throws IOException, MessagingException {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String contentType = bodyPart.getContentType().toLowerCase();

            if (contentType.contains("text/plain")) {
                result.append(bodyPart.getContent().toString());
            } else if (contentType.contains("text/html")) {
                // For HTML content, we could use a simple HTML parser
                // For now, just append it as is - in production you'd use JSoup or similar
                result.append(bodyPart.getContent().toString());
            } else if (contentType.contains("multipart")) {
                // Recursive call for nested multiparts
                result.append(extractTextFromMultipart((Multipart) bodyPart.getContent()));
            }
        }

        return result.toString();
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
                invoice.setAmount(Double.parseDouble(amountStr));
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Failed to parse amount", e);
            }
        }

        // Extract invoice number
        Matcher invoiceNumberMatcher = INVOICE_NUMBER_PATTERN.matcher(content);
        if (invoiceNumberMatcher.find()) {
            invoice.setNumber(invoiceNumberMatcher.group(2));
        }

        // Extract account number
        Matcher accountNumberMatcher = ACCOUNT_NUMBER_PATTERN.matcher(content);
        if (accountNumberMatcher.find()) {
            invoice.setAccount(accountNumberMatcher.group(2));
        }

        // Extract date
        Matcher dateMatcher = DATE_PATTERN.matcher(content);
        if (dateMatcher.find()) {
            invoice.setDate(dateMatcher.group(2));
        }

        // Extract due date
        Matcher dueDateMatcher = DUE_DATE_PATTERN.matcher(content);
        if (dueDateMatcher.find()) {
            invoice.setDueDate(dueDateMatcher.group(2));
        }
    }

    /**
     * Generate reports from the extracted invoice data
     */
    private String generateReports(List<Invoice> invoices) throws IOException {
        // Create a timestamp for the report files
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String timestamp = sdf.format(new Date());

        // CSV file for all invoices
        File csvFile = new File(outputDirectory, "invoices_" + timestamp + ".csv");

        try (FileWriter writer = new FileWriter(csvFile)) {
            // Write header
            writer.write(Invoice.header());

            // Write invoice data
            for (Invoice invoice : invoices) {
                writer.write(invoice.toString());
            }
        }

        // Summary report with statistics
        File summaryFile = new File(outputDirectory, "summary_" + timestamp + ".txt");

        try (FileWriter writer = new FileWriter(summaryFile)) {
            writer.write("Invoice Analysis Summary\n");
            writer.write("=======================\n\n");
            writer.write("Generated: " + new Date() + "\n\n");

            writer.write("Total invoices found: " + invoices.size() + "\n\n");

            // Calculate total amount
            double totalAmount = 0;
            for (Invoice invoice : invoices) {
                totalAmount += invoice.getAmount();
            }
            writer.write("Total amount: $" + String.format("%.2f", totalAmount) + "\n\n");

            // Count by type
            Map<Type, Integer> typeCount = new HashMap<>();
            for (Invoice invoice : invoices) {
                Type type = invoice.getType();
                typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
            }

            writer.write("Document types:\n");
            for (Map.Entry<Type, Integer> entry : typeCount.entrySet()) {
                writer.write("  " + entry.getKey() + ": " + entry.getValue() + "\n");
            }
            writer.write("\n");

            // Top senders
            Map<String, Integer> senderCount = new HashMap<>();
            for (Invoice invoice : invoices) {
                String sender = invoice.getEmail();
                if (sender != null && !sender.isEmpty()) {
                    senderCount.put(sender, senderCount.getOrDefault(sender, 0) + 1);
                }
            }

            writer.write("Top senders:\n");
            senderCount.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(10)
                    .forEach(entry -> {
                        try {
                            writer.write("  " + entry.getKey() + ": " + entry.getValue() + "\n");
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Error writing to summary file", e);
                        }
                    });
        }

        return "Generated reports with " + invoices.size() + " invoices.\n" +
                "CSV report: " + csvFile.getName() + "\n" +
                "Summary report: " + summaryFile.getName();
    }

    /**
     * Extract domain from email address
     */
    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }

        String[] parts = email.split("@");
        if (parts.length == 2) {
            return parts[1];
        }

        return null;
    }
}