package com.intenovation.invoice;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the storage of invoices to the file system.
 * Organizes invoices by background process, year, email domain, email address, and message ID
 * in a hierarchical folder structure.
 */
public class InvoiceStorage {
    private static final Logger LOGGER = Logger.getLogger(InvoiceStorage.class.getName());

    private final File baseDirectory;
    private final String processName;

    /**
     * Create a new invoice storage with the specified base directory
     *
     * @param baseDirectory The directory where invoices will be stored
     */
    public InvoiceStorage(File baseDirectory) {
        this(baseDirectory, "InvoiceProcessor");
    }

    /**
     * Create a new invoice storage with the specified base directory and process name
     *
     * @param baseDirectory The directory where invoices will be stored
     * @param processName The name of the background process (used as top-level directory)
     */
    public InvoiceStorage(File baseDirectory, String processName) {
        this.baseDirectory = baseDirectory;
        this.processName = processName;

        // Ensure the base directory exists
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs();
        }
    }

    /**
     * Save invoice data to organized folders
     *
     * @param invoices The list of invoices to save
     */
    public void saveInvoicesToFolders(List<Invoice> invoices) {
        // Group invoices by year, domain, email, and message ID
        Map<Integer, Map<String, Map<String, Map<String, List<Invoice>>>>> groupedInvoices = new HashMap<>();

        for (Invoice invoice : invoices) {
            int year = invoice.getYear();
            String email = invoice.getEmail();
            String messageId = extractMessageId(invoice.getEmailId());

            // Extract domain from email address
            String domain = extractDomain(email);
            if (domain == null || domain.isEmpty()) {
                domain = "unknown";
            }

            // Create nested structure if needed
            if (!groupedInvoices.containsKey(year)) {
                groupedInvoices.put(year, new HashMap<>());
            }

            Map<String, Map<String, Map<String, List<Invoice>>>> domainMap = groupedInvoices.get(year);
            if (!domainMap.containsKey(domain)) {
                domainMap.put(domain, new HashMap<>());
            }

            Map<String, Map<String, List<Invoice>>> emailMap = domainMap.get(domain);
            if (!emailMap.containsKey(email)) {
                emailMap.put(email, new HashMap<>());
            }

            Map<String, List<Invoice>> messageMap = emailMap.get(email);
            if (!messageMap.containsKey(messageId)) {
                messageMap.put(messageId, new ArrayList<>());
            }

            // Add invoice to the list
            messageMap.get(messageId).add(invoice);
        }

        // Save invoices to appropriate folders
        for (Map.Entry<Integer, Map<String, Map<String, Map<String, List<Invoice>>>>> yearEntry : groupedInvoices.entrySet()) {
            int year = yearEntry.getKey();

            for (Map.Entry<String, Map<String, Map<String, List<Invoice>>>> domainEntry : yearEntry.getValue().entrySet()) {
                String domain = domainEntry.getKey();

                for (Map.Entry<String, Map<String, List<Invoice>>> emailEntry : domainEntry.getValue().entrySet()) {
                    String email = emailEntry.getKey();

                    for (Map.Entry<String, List<Invoice>> messageEntry : emailEntry.getValue().entrySet()) {
                        String messageId = messageEntry.getKey();
                        List<Invoice> messageInvoices = messageEntry.getValue();

                        // Create folder structure
                        File folderPath = new File(baseDirectory,
                                processName + File.separator +
                                        year + File.separator +
                                        sanitizeFileName(domain) + File.separator +
                                        sanitizeFileName(email) + File.separator +
                                        sanitizeFileName(messageId));

                        if (!folderPath.exists()) {
                            folderPath.mkdirs();
                        }

                        // Create invoices.tsv file
                        File invoicesFile = new File(folderPath, "invoices.tsv");
                        boolean fileExists = invoicesFile.exists();

                        try (FileWriter writer = new FileWriter(invoicesFile, true)) {
                            // Write header if file is new
                            if (!fileExists) {
                                writer.write(Invoice.header());
                            }

                            // Write invoice data
                            for (Invoice invoice : messageInvoices) {
                                writer.write(invoice.toString());
                            }
                        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, "Error writing to invoices.tsv", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Extract domain from email address
     */
    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }

        // Handle email addresses that might be enclosed in angle brackets
        if (email.contains("<") && email.contains(">")) {
            int start = email.indexOf("<");
            int end = email.indexOf(">");
            if (start < end && email.substring(start, end).contains("@")) {
                email = email.substring(start + 1, end);
            }
        }

        String[] parts = email.split("@");
        if (parts.length == 2) {
            String domain = parts[1];

            // Clean up any trailing characters
            domain = domain.trim();

            // Remove trailing ">" that might be present in some email addresses
            if (domain.endsWith(">")) {
                domain = domain.substring(0, domain.length() - 1);
            }

            // Remove any trailing punctuation or special characters
            while (domain.length() > 0 && !Character.isLetterOrDigit(domain.charAt(domain.length() - 1))) {
                domain = domain.substring(0, domain.length() - 1);
            }

            return domain;
        }

        return null;
    }

    /**
     * Extract message ID from the email ID
     */
    private String extractMessageId(String emailId) {
        if (emailId == null || emailId.isEmpty()) {
            return "unknown";
        }

        // If it's a file URL, get the last path component
        if (emailId.startsWith("file:")) {
            String[] parts = emailId.split("/");
            if (parts.length > 0) {
                return parts[parts.length - 1];
            }
        }

        // If it contains angle brackets, extract the part between them
        if (emailId.contains("<") && emailId.contains(">")) {
            int start = emailId.indexOf("<") + 1;
            int end = emailId.indexOf(">");
            if (start < end) {
                return emailId.substring(start, end);
            }
        }

        return emailId;
    }

    /**
     * Sanitize a file name for use in a file path
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unnamed";
        }

        // Replace invalid file name characters
        String sanitized = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // Replace multiple sequential invalid characters with a single underscore
        sanitized = sanitized.replaceAll("_+", "_");

        // Trim leading/trailing whitespace and dots
        sanitized = sanitized.replaceAll("^[\\s\\.]+|[\\s\\.]+$", "");

        // Remove trailing underscores
        while (sanitized.endsWith("_")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }

        // Limit length to avoid file system issues
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized.isEmpty() ? "unnamed" : sanitized;
    }
}