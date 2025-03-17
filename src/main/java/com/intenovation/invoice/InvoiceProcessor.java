package com.intenovation.invoice;

import com.intenovation.email.reader.LocalMail;
import com.intenovation.email.reader.LocalStore;
import com.intenovation.invoice.Invoice;
import com.intenovation.invoice.Type;

import javax.mail.*;
import javax.mail.search.SubjectTerm;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility for processing invoices from downloaded emails.
 * This class uses the LocalMail API to efficiently scan through
 * downloaded emails and extract invoice information.
 */
public class InvoiceProcessor {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    // Regex patterns for extracting information
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?i)(total|amount|sum|betrag|summe)[\\s:]*[$€£]?\\s*([\\d,.]+)");
    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile("(?i)(invoice|rechnung|bill)[\\s:#-]*([A-Z0-9]{4,20})");
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("(?i)(account|konto|customer)[\\s:#-]*([A-Z0-9]{4,20})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(?i)(date|datum)[\\s:]*([0-9]{1,2}[\\s./\\-][0-9]{1,2}[\\s./\\-][0-9]{2,4})");
    private static final Pattern DUE_DATE_PATTERN = Pattern.compile("(?i)(due date|fällig|zahlbar bis)[\\s:]*([0-9]{1,2}[\\s./\\-][0-9]{1,2}[\\s./\\-][0-9]{2,4})");
    
    public static void main(String[] args) {
        // Specify the directory where emails are stored
        File emailDirectory = new File(System.getProperty("user.home") + File.separator + "EmailArchive");
        
        try {
            // Process invoices and generate a CSV file
            List<Invoice> invoices = processInvoices(emailDirectory);
            
            System.out.println("Found " + invoices.size() + " invoices");
            
            // Export to CSV
            exportToCSV(invoices, new File("invoices.csv"));
            
            System.out.println("Exported invoices to invoices.csv");
            
        } catch (Exception e) {
            System.err.println("Error processing invoices: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Process all emails in the archive to extract invoice information
     * 
     * @param emailDirectory The directory containing downloaded emails
     * @return A list of Invoice objects
     * @throws MessagingException If there is an error accessing the emails
     */
    public static List<Invoice> processInvoices(File emailDirectory) throws MessagingException {
        List<Invoice> invoices = new ArrayList<>();
        
        // Open the local store
        Store store = LocalMail.openStore(emailDirectory);
        
        try {
            // Get all folders
            Folder rootFolder = store.getDefaultFolder();
            Folder[] folders = rootFolder.list();
            
            // First, search for emails with invoice-related subjects
            String[] invoiceKeywords = {
                "invoice", "rechnung", "bill", "statement", "payment", "receipt",
                "zahlung", "beleg", "quittung"
            };
            
            for (Folder folder : folders) {
                if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0) {
                    continue;
                }
                
                try {
                    folder.open(Folder.READ_ONLY);
                    
                    // Process all messages
                    Message[] messages = folder.getMessages();
                    System.out.println("Processing " + messages.length + " messages in " + folder.getFullName());
                    
                    for (Message message : messages) {
                        Invoice invoice = processMessage(message);
                        if (invoice != null) {
                            invoices.add(invoice);
                        }
                    }
                    
                    folder.close(false);
                } catch (Exception e) {
                    System.err.println("Error processing folder " + folder.getFullName() + ": " + e.getMessage());
                }
            }
            
            return invoices;
        } finally {
            store.close();
        }
    }
    
    /**
     * Process a message to extract invoice information
     * 
     * @param message The email message
     * @return An Invoice object or null if no invoice information is found
     */
    private static Invoice processMessage(Message message) {
        try {
            // Get the subject and check if it contains invoice-related keywords
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
            invoice.fileName = message.getFileName();
            
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
                invoice.date = DATE_FORMAT.format(sentDate);
            }
            
            // Extract invoice information from content
            extractInvoiceDetails(invoice, textContent);
            
            return invoice;
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract invoice details from text content
     * 
     * @param invoice The invoice object to populate
     * @param content The text content to process
     */
    private static void extractInvoiceDetails(Invoice invoice, String content) {
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
     * Extract domain from email address
     * 
     * @param email The email address
     * @return The domain part or null if not found
     */
    private static String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return null;
        }
        
        return parts[1];
    }
    
    /**
     * Export invoices to a CSV file
     * 
     * @param invoices The list of invoices to export
     * @param outputFile The output CSV file
     * @throws IOException If there is an error writing to the file
     */
    private static void exportToCSV(List<Invoice> invoices, File outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            // Write header
            writer.write(Invoice.header());
            
            // Write invoice data
            for (Invoice invoice : invoices) {
                writer.write(invoice.toString());
            }
        }
    }
}