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
 * Organizes invoices by year, domain, and email in a hierarchical folder structure.
 */
public class InvoiceStorage {
    private static final Logger LOGGER = Logger.getLogger(InvoiceStorage.class.getName());
    
    private final File baseDirectory;
    
    /**
     * Create a new invoice storage with the specified base directory
     * 
     * @param baseDirectory The directory where invoices will be stored
     */
    public InvoiceStorage(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        
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
        // Group invoices by year, domain, and email
        Map<Integer, Map<String, Map<String, List<Invoice>>>> groupedInvoices = new HashMap<>();
        
        for (Invoice invoice : invoices) {
            int year = invoice.getYear();
            String domain = invoice.getUtility();
            String email = invoice.getEmail();
            
            if (domain == null || domain.isEmpty()) {
                domain = "unknown";
            }
            
            // Create nested structure if needed
            if (!groupedInvoices.containsKey(year)) {
                groupedInvoices.put(year, new HashMap<>());
            }
            
            Map<String, Map<String, List<Invoice>>> domainMap = groupedInvoices.get(year);
            if (!domainMap.containsKey(domain)) {
                domainMap.put(domain, new HashMap<>());
            }
            
            Map<String, List<Invoice>> emailMap = domainMap.get(domain);
            if (!emailMap.containsKey(email)) {
                emailMap.put(email, new ArrayList<>());
            }
            
            // Add invoice to the list
            emailMap.get(email).add(invoice);
        }
        
        // Save invoices to appropriate folders
        for (Map.Entry<Integer, Map<String, Map<String, List<Invoice>>>> yearEntry : groupedInvoices.entrySet()) {
            int year = yearEntry.getKey();
            
            for (Map.Entry<String, Map<String, List<Invoice>>> domainEntry : yearEntry.getValue().entrySet()) {
                String domain = domainEntry.getKey();
                
                for (Map.Entry<String, List<Invoice>> emailEntry : domainEntry.getValue().entrySet()) {
                    String email = emailEntry.getKey();
                    List<Invoice> emailInvoices = emailEntry.getValue();
                    
                    // Create folder structure
                    File folderPath = new File(baseDirectory, 
                            year + File.separator + domain + File.separator + sanitizeFileName(email));
                    
                    if (!folderPath.exists()) {
                        folderPath.mkdirs();
                    }
                    
                    // Create a messages subfolder
                    File messagesFolder = new File(folderPath, "messages");
                    if (!messagesFolder.exists()) {
                        messagesFolder.mkdirs();
                    }
                    
                    // Create or append to invoices.tsv file
                    File invoicesFile = new File(messagesFolder, "invoices.tsv");
                    boolean fileExists = invoicesFile.exists();
                    
                    try (FileWriter writer = new FileWriter(invoicesFile, true)) {
                        // Write header if file is new
                        if (!fileExists) {
                            writer.write(Invoice.header());
                        }
                        
                        // Write invoice data
                        for (Invoice invoice : emailInvoices) {
                            writer.write(invoice.toString());
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Error writing to invoices.tsv", e);
                    }
                }
            }
        }
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
        
        // Limit length to avoid file system issues
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        
        return sanitized.isEmpty() ? "unnamed" : sanitized;
    }
}