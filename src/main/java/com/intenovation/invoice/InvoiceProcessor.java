package com.intenovation.invoice;

import com.intenovation.appfw.systemtray.BackgroundTask;
import com.intenovation.appfw.systemtray.ProgressStatusCallback;
import com.intenovation.appfw.ui.UIService;
import com.intenovation.email.reader.LocalMail;

import javax.mail.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Invoice processor that scans downloaded emails to identify and extract invoice information.
 */
public class InvoiceProcessor extends BackgroundTask {
    private static final Logger LOGGER = Logger.getLogger(InvoiceProcessor.class.getName());

    // Regular expressions for extracting invoice information
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?i)(total|amount|sum|betrag|summe)[\\s:]*[$€£]?\\s*([\\d,.]+)");
    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile("(?i)(invoice|rechnung|bill)[\\s:#-]*([A-Z0-9]{4,20})");
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("(?i)(account|konto|customer)[\\s:#-]*([A-Z0-9]{4,20})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(?i)(date|datum)[\\s:]*([0-9]{1,2}[\\s./\\-][0-9]{1,2}[\\s./\\-][0-9]{2,4})");
    private static final Pattern DUE_DATE_PATTERN = Pattern.compile("(?i)(due date|fällig|zahlbar bis)[\\s:]*([0-9]{1,2}[\\s./\\-][0-9]{1,2}[\\s./\\-][0-9]{2,4})");

    // Configuration
    private final InvoiceConfiguration config;
    private final UIService uiService;

    /**
     * Create a new invoice processor task with dependencies
     *
     * @param config The invoice configuration
     * @param uiService The UI service
     */
    public InvoiceProcessor(InvoiceConfiguration config, UIService uiService) {
        super(
                "Invoice Processor",
                "Processes emails to extract invoice information",
                config.getProcessingIntervalHours() * 60 * 60, // Convert hours to seconds
                true         // Available in menu
        );

        this.config = config;
        this.uiService = uiService;

        // Create output directory if it doesn't exist
        if (!config.getOutputDirectory().exists()) {
            config.getOutputDirectory().mkdirs();
        }
    }

    /**
     * Create a new invoice processor task (legacy constructor for backward compatibility)
     *
     * @param emailDirectory The directory containing downloaded emails
     * @param outputDirectory The directory to save invoice reports
     */
    public InvoiceProcessor(File emailDirectory, File outputDirectory) {
        super(
                "Invoice Processor",
                "Processes emails to extract invoice information",
                2 * 60 * 60, // 2 hours interval
                true         // Available in menu
        );

        // Create a configuration with the provided directories
        this.config = new InvoiceConfiguration();

        // Apply the directories to the configuration
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("emailDirectory", emailDirectory);
        configValues.put("outputDirectory", outputDirectory);
        config.applyConfiguration(configValues);

        // No UI service available with this constructor
        this.uiService = null;

        // Create output directory if it doesn't exist
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
    }

    /**
     * Execute the task with progress and status reporting
     *
     * @param callback Callback for reporting progress and status messages
     * @return Status message that will be displayed on completion
     * @throws InterruptedException if the task is cancelled
     */
    @Override
    public String execute(ProgressStatusCallback callback) throws InterruptedException {
        callback.update(0, "Initializing invoice processor...");

        List<Invoice> invoices = new ArrayList<>();
        Store store = null;

        try {
            // Step 1: Open the local mail store
            callback.update(5, "Opening local mail store " + config.getEmailDirectory());

            store = LocalMail.openStore(config.getEmailDirectory());
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
        String messageId = null;
        String[] messageIdHeaders = message.getHeader("Message-ID");
        if (messageIdHeaders != null && messageIdHeaders.length > 0) {
            messageId = messageIdHeaders[0];
        }

        // If no Message-ID, create a hash
        if (messageId == null || messageId.isEmpty()) {
            StringBuilder sb = new StringBuilder();

            if (message.getSubject() != null) {
                sb.append(message.getSubject());
            }

            if (message.getSentDate() != null) {
                sb.append(message.getSentDate().getTime());
            }

            if (message.getFrom() != null && message.getFrom().length > 0) {
                sb.append(message.getFrom()[0].toString());
            }

            messageId = "hash-" + Math.abs(sb.toString().hashCode());
        }

        try {
            // Get the folder name
            Folder folder = message.getFolder();
            String folderName = folder != null ? folder.getFullName() : "unknown";

            // Sanitize the message ID and folder name
            String sanitizedId = com.intenovation.email.downloader.FileUtils.sanitizeFileName(messageId);
            String sanitizedFolder = com.intenovation.email.downloader.FileUtils.sanitizeFolderName(folderName);

            // Construct the file:// URL to the message directory
            File emailDirectory = config.getEmailDirectory();
            File messageDir = new File(emailDirectory, sanitizedFolder + File.separator + "messages" + File.separator + sanitizedId);
            String fileUrl = messageDir.toURI().toString();

            // Set the emailId to the file:// URL
            invoice.setEmailId(fileUrl);
        } catch (Exception e) {
            // Fallback to just using the message ID if there's an error
            LOGGER.log(Level.WARNING, "Error creating file URL: " + e.getMessage(), e);
            invoice.setEmailId(messageId);
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
                String amountStr = amountMatcher.group(2);
                // Smart parsing of amount that handles both German and American formats
                double amount = parseAmount(amountStr);
                invoice.setAmount(amount);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Failed to parse amount: " + amountMatcher.group(2), e);
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
     * Parse an amount string that could be in either German or American format
     * German format: 1.234,56 (. for thousands, , for decimal)
     * American format: 1,234.56 (, for thousands, . for decimal)
     *
     * @param amountStr The amount string to parse
     * @return The parsed amount as a double
     */
    private double parseAmount(String amountStr) {
        // Remove any currency symbols and whitespace
        amountStr = amountStr.replaceAll("[$€£\\s]", "");
        if (amountStr.endsWith(".")){
            LOGGER.warning(amountStr);
            amountStr=amountStr.substring(0,amountStr.length()-1);
            LOGGER.warning(amountStr);
        }
        // If empty after cleaning, return 0
        if (amountStr.isEmpty()) {
            return 0.0;
        }

        // Check if this is likely a German format number (contains comma but no period,
        // or the last separator is a comma)
        boolean isGermanFormat = false;

        // If it has a comma but no period, it's German format
        if (amountStr.contains(",") && !amountStr.contains(".")) {
            isGermanFormat = true;
        }
        // If it has both comma and period, look at the positions
        else if (amountStr.contains(",") && amountStr.contains(".")) {
            int lastCommaPos = amountStr.lastIndexOf(",");
            int lastPeriodPos = amountStr.lastIndexOf(".");

            // If the last separator is a comma, it's likely German format
            // (e.g., 1.234,56)
            isGermanFormat = lastCommaPos > lastPeriodPos;
        }

        // Convert to a parseable format
        if (isGermanFormat) {
            // German format: Remove all periods and replace comma with period
            amountStr = amountStr.replace(".", "").replace(",", ".");
        } else {
            // American format: Remove all commas
            amountStr = amountStr.replace(",", "");
        }
        if (".".equals(amountStr)) return 0.0;
        // Parse the formatted string
        return Double.parseDouble(amountStr);
    }

    /**
     * Generate reports from the extracted invoice data
     */
    private String generateReports(List<Invoice> invoices) throws IOException {
        // Create a timestamp for the report files
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String timestamp = sdf.format(new Date());

        // CSV file for all invoices
        File csvFile = new File(config.getOutputDirectory(), "invoices_" + timestamp + ".tsv");

        try (FileWriter writer = new FileWriter(csvFile)) {
            // Write header
            writer.write(Invoice.header());

            // Write invoice data
            for (Invoice invoice : invoices) {
                writer.write(invoice.toString());
            }
        }

        // Summary report with statistics
        File summaryFile = new File(config.getOutputDirectory(), "summary_" + timestamp + ".txt");

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