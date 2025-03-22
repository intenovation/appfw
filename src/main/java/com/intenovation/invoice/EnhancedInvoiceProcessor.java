package com.intenovation.invoice;

import com.intenovation.appfw.systemtray.BackgroundTask;
import com.intenovation.appfw.systemtray.ProgressStatusCallback;
import com.intenovation.appfw.ui.UIService;
import com.intenovation.email.reader.LocalMail;

import javax.mail.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced invoice processor that coordinates the parsing of emails to extract invoice information.
 * This is the main entry point that implements BackgroundTask and delegates to specialized parsers.
 */
public class EnhancedInvoiceProcessor extends BackgroundTask {
    private static final Logger LOGGER = Logger.getLogger(EnhancedInvoiceProcessor.class.getName());

    // Track processed message IDs to avoid duplicates
    private final Set<String> processedMessageIds = ConcurrentHashMap.newKeySet();

    // Configuration and services
    private final InvoiceConfiguration config;
    private final UIService uiService;

    // Specialized components
    private final InvoiceParser parser;
    private final InvoiceReportGenerator reportGenerator;
    private final InvoiceStorage storage;

    /**
     * Create a new enhanced invoice processor task with dependencies
     *
     * @param config The invoice configuration
     * @param uiService The UI service
     */
    public EnhancedInvoiceProcessor(InvoiceConfiguration config, UIService uiService) {
        super(
                "Enhanced Invoice Processor",
                "Processes emails to extract invoice information with advanced parsing",
                config.getProcessingIntervalHours() * 60 * 60, // Convert hours to seconds
                true         // Available in menu
        );

        this.config = config;
        this.uiService = uiService;

        // Initialize specialized components
        this.parser = new InvoiceParser();
        this.reportGenerator = new InvoiceReportGenerator();
        // Create output directory if it doesn't exist
        File outputDirectory = new File(config.getOutputDirectory(), "EnhancedInvoiceProcessor");
        outputDirectory.mkdirs();
        this.storage = new InvoiceStorage(outputDirectory);
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
        callback.update(0, "Initializing enhanced invoice processor...");

        Map<String, List<Invoice>> invoicesByFolder = new HashMap<>();
        Store store = null;
        int totalInvoicesFound = 0;

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

            // Step 3: Process each folder
            int processedFolders = 0;

            for (Folder folder : foldersToProcess) {
                // Check for cancellation
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Task cancelled");
                }

                try {
                    folder.open(Folder.READ_ONLY);

                    callback.update(20 + (70 * processedFolders / Math.max(1, foldersWithMessages)),
                            "Processing folder: " + folder.getFullName() + " (" + folder.getMessageCount() + " messages)");

                    // Process messages in this folder
                    Message[] messages = folder.getMessages();
                    List<Invoice> folderInvoices = new ArrayList<>();

                    for (int i = 0; i < messages.length; i++) {
                        // Check for cancellation
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException("Task cancelled");
                        }

                        try {
                            Message message = messages[i];
                            List<Invoice> messageInvoices = processMessage(message, callback);

                            if (!messageInvoices.isEmpty()) {
                                folderInvoices.addAll(messageInvoices);
                                totalInvoicesFound += messageInvoices.size();
                            }

                            // Update progress periodically
                            if (i % 10 == 0 || i == messages.length - 1) {
                                int folderProgress = 20 + (70 * processedFolders / foldersWithMessages)
                                        + (70 * i / (Math.max(1, messages.length) * foldersWithMessages));
                                callback.update(Math.min(90, folderProgress),
                                        "Processed " + (i + 1) + "/" + messages.length + " in folder "
                                                + folder.getFullName() + ", found " + totalInvoicesFound + " invoices");
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error processing message: " + e.getMessage(), e);
                        }
                    }

                    if (!folderInvoices.isEmpty()) {
                        invoicesByFolder.put(folder.getFullName(), folderInvoices);
                    }

                    folder.close(false);
                    processedFolders++;

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing folder " + folder.getFullName(), e);
                }
            }

            // Step 4: Generate reports and save results
            callback.update(90, "Saving invoice data and generating reports...");

            if (totalInvoicesFound == 0) {
                callback.update(100, "No invoices found");
                return "No invoices found in the email archive";
            }

            // Save invoice data to organized folders
            for (Map.Entry<String, List<Invoice>> entry : invoicesByFolder.entrySet()) {
                storage.saveInvoicesToFolders(entry.getValue());
            }

            // Flatten all invoices for the main report
            List<Invoice> allInvoices = new ArrayList<>();
            for (List<Invoice> folderInvoices : invoicesByFolder.values()) {
                allInvoices.addAll(folderInvoices);
            }

            // Generate main invoice reports
            String reportResult = reportGenerator.generateReports(allInvoices, config.getOutputDirectory());

            callback.update(100, "Completed: " + reportResult);
            return "Found " + totalInvoicesFound + " invoices. " + reportResult;

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
     * Process a message to extract invoice information
     * This is the main entry point for message processing that delegates to the parser
     */
    private List<Invoice> processMessage(Message message, ProgressStatusCallback callback)
            throws MessagingException, IOException {

        // Check for duplicates using Message-ID
        String messageId = MessageUtils.getMessageId(message);
        if (messageId == null || messageId.isEmpty()) {
            // Create a hash-based ID if no Message-ID exists
            messageId = MessageUtils.createMessageHash(message);
        }

        // Skip if already processed
        if (processedMessageIds.contains(messageId)) {
            return Collections.emptyList();
        }

        // Mark as processed
        processedMessageIds.add(messageId);

        // Create a base invoice with common properties
        Invoice baseInvoice;
        try {
            baseInvoice = createBaseInvoice(message, messageId);
        } catch (MessagingException e) {
            LOGGER.log(Level.WARNING, "Error creating base invoice: " + e.getMessage(), e);

            // Fall back to the original method if there's an error
            Date messageDate = message.getSentDate() != null ? message.getSentDate() : message.getReceivedDate();
            if (messageDate == null) {
                messageDate = new Date();
            }

            String from = "unknown";
            if (message.getFrom() != null && message.getFrom().length > 0) {
                from = message.getFrom()[0].toString();
            }

            baseInvoice = new Invoice();
            baseInvoice.setEmailId(messageId);
            baseInvoice.setSubject(message.getSubject() != null ? message.getSubject() : "");
            baseInvoice.setEmail(from);

            Calendar cal = Calendar.getInstance();
            cal.setTime(messageDate);
            baseInvoice.setYear(cal.get(Calendar.YEAR));
            baseInvoice.setMonth(cal.get(Calendar.MONTH) + 1);
            baseInvoice.setDay(cal.get(Calendar.DAY_OF_MONTH));

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            baseInvoice.setDate(sdf.format(messageDate));

            String domain = MessageUtils.extractDomain(from);
            if (domain != null) {
                baseInvoice.setUtility(domain);
            }
        }

        // Delegate to the parser for comprehensive processing
        return parser.parseMessage(message, baseInvoice);
    }

    /**
     * Create a base invoice with common properties that will be shared by all invoices
     * extracted from the same message.
     *
     * @param message The email message
     * @param messageId The message ID
     * @return A base invoice with common properties
     * @throws MessagingException If there is an error accessing message properties
     */
    private Invoice createBaseInvoice(Message message, String messageId) throws MessagingException {
        Invoice invoice = new Invoice();

        // Get the folder name
        Folder folder = message.getFolder();
        String folderName = folder != null ? folder.getFullName() : "unknown";

        // Sanitize the message ID and folder name the same way EmailDownloader does
        String sanitizedId = com.intenovation.email.downloader.FileUtils.sanitizeFileName(messageId);
        String sanitizedFolder = com.intenovation.email.downloader.FileUtils.sanitizeFolderName(folderName);

        // Construct the file:// URL to the message directory
        File emailDirectory = config.getEmailDirectory();
        File messageDir = new File(emailDirectory, sanitizedFolder + File.separator + "messages" + File.separator + sanitizedId);
        String fileUrl = messageDir.toURI().toString();

        // Set the emailId to the file:// URL
        invoice.setEmailId(fileUrl);

        // Set basic properties
        invoice.setSubject(message.getSubject() != null ? message.getSubject() : "");

        // Set email info
        String from = "unknown";
        if (message.getFrom() != null && message.getFrom().length > 0) {
            from = message.getFrom()[0].toString();
        }
        invoice.setEmail(from);

        // Set date-related properties
        Date sentDate = message.getSentDate() != null ? message.getSentDate() : message.getReceivedDate();
        if (sentDate == null) {
            sentDate = new Date();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(sentDate);
        invoice.setYear(cal.get(Calendar.YEAR));
        invoice.setMonth(cal.get(Calendar.MONTH) + 1);
        invoice.setDay(cal.get(Calendar.DAY_OF_MONTH));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        invoice.setDate(sdf.format(sentDate));

        // Extract domain for utility field
        String domain = MessageUtils.extractDomain(from);
        if (domain != null) {
            invoice.setUtility(domain);
        }

        return invoice;
    }
}