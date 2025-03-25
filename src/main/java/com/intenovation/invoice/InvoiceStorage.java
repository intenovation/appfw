package com.intenovation.invoice;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the storage of invoices to the file system.
 * Organizes invoices by background process, year, email domain, email address, and message ID
 * in a hierarchical folder structure.
 * Now includes domain-based organization for tax purposes within the same directory structure.
 */
public class InvoiceStorage {
    private static final Logger LOGGER = Logger.getLogger(InvoiceStorage.class.getName());
    private static final double MIN_AMOUNT = 0.0;
    private static final double MAX_AMOUNT = 30000.0;

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
        // Group invoices by year, domain, email, and date+subject
        Map<Integer, Map<String, Map<String, Map<String, List<Invoice>>>>> groupedInvoices = new HashMap<>();

        for (Invoice invoice : invoices) {
            int year = invoice.getYear();
            String email = invoice.getEmail();

            // Format date and subject for directory name
            String dateSubjectDir = formatDateSubjectDirectory(invoice);

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

            Map<String, List<Invoice>> dateSubjectMap = emailMap.get(email);
            if (!dateSubjectMap.containsKey(dateSubjectDir)) {
                dateSubjectMap.put(dateSubjectDir, new ArrayList<>());
            }

            // Add invoice to the list
            dateSubjectMap.get(dateSubjectDir).add(invoice);
        }

        // Save invoices to appropriate folders
        for (Map.Entry<Integer, Map<String, Map<String, Map<String, List<Invoice>>>>> yearEntry : groupedInvoices.entrySet()) {
            int year = yearEntry.getKey();

            for (Map.Entry<String, Map<String, Map<String, List<Invoice>>>> domainEntry : yearEntry.getValue().entrySet()) {
                String domain = domainEntry.getKey();

                for (Map.Entry<String, Map<String, List<Invoice>>> emailEntry : domainEntry.getValue().entrySet()) {
                    String email = emailEntry.getKey();

                    for (Map.Entry<String, List<Invoice>> dateSubjectEntry : emailEntry.getValue().entrySet()) {
                        String dateSubjectDir = dateSubjectEntry.getKey();
                        List<Invoice> dirInvoices = dateSubjectEntry.getValue();

                        // Create folder structure
                        File folderPath = new File(baseDirectory,
                                processName + File.separator +
                                        year + File.separator +
                                        sanitizeFileName(domain) + File.separator +
                                        sanitizeFileName(email) + File.separator +
                                        sanitizeFileName(dateSubjectDir));

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
                            for (Invoice invoice : dirInvoices) {
                                writer.write(invoice.toString());
                            }
                        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, "Error writing to invoices.tsv", e);
                        }
                    }
                }
            }
        }

        // Also save domain-based reports for tax purposes
        saveToDomainFolders(invoices);
    }

    /**
     * Save invoices organized by domain, taking only the first with a reasonable amount
     * for tax reporting purposes. Uses the same directory structure as the main storage.
     *
     * @param invoices The list of invoices to save
     */
    private void saveToDomainFolders(List<Invoice> invoices) {
        // Track unique email IDs to avoid duplicates
        Map<String, Invoice> bestInvoices = new HashMap<>();

        // Group invoices by message/email ID
        Map<String, List<Invoice>> emailGroups = new HashMap<>();

        // First, group all invoices by their email ID
        for (Invoice invoice : invoices) {
            String emailId = invoice.getEmailId();
            if (!emailGroups.containsKey(emailId)) {
                emailGroups.put(emailId, new ArrayList<>());
            }
            emailGroups.get(emailId).add(invoice);
        }

        // For each group (same email), select the best invoice (first with reasonable amount)
        for (Map.Entry<String, List<Invoice>> entry : emailGroups.entrySet()) {
            String emailId = entry.getKey();
            List<Invoice> groupInvoices = entry.getValue();

            // Sort by amount to be consistent
            groupInvoices.sort(Comparator.comparingDouble(Invoice::getAmount));

            // Find first invoice with reasonable amount
            Invoice selected = null;
            for (Invoice inv : groupInvoices) {
                double amount = inv.getAmount();
                if (amount > MIN_AMOUNT && amount < MAX_AMOUNT) {
                    selected = inv;
                    break;
                }
            }

            // If no reasonable amount, use first invoice
            if (selected == null && !groupInvoices.isEmpty()) {
                selected = groupInvoices.get(0);
                LOGGER.info("No invoice with reasonable amount found for email ID: " +
                        emailId + ", using first available invoice with amount: " +
                        selected.getAmount());
            }

            if (selected != null) {
                bestInvoices.put(emailId, selected);
            }
        }

        // Now we have one invoice per email ID - group them by year and domain
        Map<Integer, Map<String, List<Invoice>>> yearDomainGroups = new HashMap<>();

        for (Invoice invoice : bestInvoices.values()) {
            int year = invoice.getYear();

            // Extract domain from email
            String email = invoice.getEmail();
            String domain = extractDomain(email);
            if (domain == null || domain.isEmpty()) {
                domain = "unknown";
            }

            // Create structure if needed
            if (!yearDomainGroups.containsKey(year)) {
                yearDomainGroups.put(year, new HashMap<>());
            }

            Map<String, List<Invoice>> domainMap = yearDomainGroups.get(year);
            if (!domainMap.containsKey(domain)) {
                domainMap.put(domain, new ArrayList<>());
            }

            // Add to domain's invoice list
            domainMap.get(domain).add(invoice);
        }

        // Save domain reports
        for (Map.Entry<Integer, Map<String, List<Invoice>>> yearEntry : yearDomainGroups.entrySet()) {
            int year = yearEntry.getKey();

            for (Map.Entry<String, List<Invoice>> domainEntry : yearEntry.getValue().entrySet()) {
                String domain = domainEntry.getKey();
                List<Invoice> domainInvoices = domainEntry.getValue();

                // Sort by date for readability
                domainInvoices.sort(Comparator
                        .comparing(Invoice::getYear)
                        .thenComparing(Invoice::getMonth)
                        .thenComparing(Invoice::getDay));

                // Create domain summary folder
                File folderPath = new File(baseDirectory,
                        processName + File.separator +
                                year + File.separator +
                                sanitizeFileName(domain) + File.separator +
                                "_DomainSummary");

                if (!folderPath.exists()) {
                    folderPath.mkdirs();
                }

                // Create invoices.tsv file
                File invoicesFile = new File(folderPath, "invoices.tsv");

                try (FileWriter writer = new FileWriter(invoicesFile, false)) { // Overwrite existing file
                    // Write header
                    writer.write(Invoice.header());

                    // Write invoice data (one per email)
                    for (Invoice invoice : domainInvoices) {
                        writer.write(invoice.toString());
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error writing to domain invoices.tsv", e);
                }

                // Create summary file with total amount
                createDomainSummary(folderPath, domain, year, domainInvoices);
            }
        }
    }

    /**
     * Create a summary file for a domain with total amount for the year
     */
    private void createDomainSummary(File folderPath, String domain, int year, List<Invoice> invoices) {
        File summaryFile = new File(folderPath, "summary.txt");

        try (FileWriter writer = new FileWriter(summaryFile)) {
            writer.write("Tax Summary for " + domain + " - " + year + "\n");
            writer.write("==================================================\n\n");

            // Calculate total amount
            double totalAmount = 0.0;
            for (Invoice invoice : invoices) {
                totalAmount += invoice.getAmount();
            }

            writer.write("Total invoices: " + invoices.size() + "\n");
            writer.write("Total amount: $" + String.format("%.2f", totalAmount) + "\n\n");

            // List individual invoices
            writer.write("Invoice details:\n");
            writer.write("-----------------\n");

            for (Invoice invoice : invoices) {
                writer.write(String.format("Date: %s, Amount: $%.2f, Subject: %s\n",
                        invoice.getDate(),
                        invoice.getAmount(),
                        invoice.getSubject()));
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error writing domain summary file", e);
        }
    }

    /**
     * Format date and subject into a directory name
     * Uses format: YYYY-MM-DD_subject-line
     */
    private String formatDateSubjectDirectory(Invoice invoice) {
        // Format the date part (year-month-day)
        String datePart = String.format("%04d-%02d-%02d",
                invoice.getYear(),
                invoice.getMonth(),
                invoice.getDay());

        // Get a shortened subject (first 50 chars max)
        String subject = invoice.getSubject();
        if (subject == null || subject.isEmpty()) {
            subject = "no-subject";
        } else if (subject.length() > 50) {
            subject = subject.substring(0, 50);
        }

        // Combine date and subject
        return datePart + "_" + subject;
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