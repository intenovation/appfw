package com.intenovation.email.reader.example;

import com.intenovation.email.reader.LocalMail;
import javax.mail.*;
import javax.mail.search.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Example class demonstrating how to use the LocalMail API to work with downloaded emails
 */
public class LocalMailExample {

    public static void main(String[] args) {
        // Specify the directory where emails are stored
        File emailDirectory = new File(System.getProperty("user.home") + File.separator + "EmailArchive");
        
        try {
            // Open the local store
            Store store = LocalMail.openStore(emailDirectory);
            System.out.println("Connected to local email store: " + emailDirectory);
            
            // Get the default folder (root)
            Folder rootFolder = store.getDefaultFolder();
            System.out.println("Root folder: " + rootFolder.getFullName());
            
            // List all folders
            Folder[] folders = rootFolder.list();
            System.out.println("Found " + folders.length + " folders:");
            for (Folder folder : folders) {
                System.out.println("  - " + folder.getFullName() + " (" + folder.getMessageCount() + " messages)");
            }
            
            // Example 1: List all emails in the INBOX
            processFolder(store, "INBOX");
            
            // Example 2: Search for specific emails in all folders
            searchAllFolders(store, "invoice", "billing@example.com");
            
            // Example 3: Process emails received in the last month
            processRecentEmails(store, 30);
            
            // Close the store
            store.close();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Process all messages in a specific folder
     */
    private static void processFolder(Store store, String folderName) throws Exception {
        Folder folder = store.getFolder(folderName);
        if (!folder.exists()) {
            System.out.println("Folder does not exist: " + folderName);
            return;
        }
        
        // Open the folder
        folder.open(Folder.READ_ONLY);
        System.out.println("\nProcessing folder: " + folder.getFullName());
        System.out.println("Total messages: " + folder.getMessageCount());
        
        // Get all messages
        Message[] messages = folder.getMessages();
        System.out.println("Listing messages:");
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < messages.length; i++) {
            Message message = messages[i];
            Date sentDate = message.getSentDate();
            String dateStr = sentDate != null ? sdf.format(sentDate) : "Unknown";
            
            System.out.printf("  %3d: [%s] %-40s (From: %s)%n", 
                i + 1, 
                dateStr, 
                truncate(message.getSubject(), 40), 
                getFromAddress(message));
        }
        
        // Close the folder
        folder.close(false);
    }
    
    /**
     * Search for emails matching subject and from criteria across all folders
     */
    private static void searchAllFolders(Store store, String subjectKeyword, String fromEmail) throws Exception {
        System.out.println("\nSearching for emails containing '" + subjectKeyword + 
                           "' from '" + fromEmail + "'");
        
        // Create search terms
        SearchTerm subjectSearch = new SubjectTerm(subjectKeyword);
        SearchTerm fromSearch = new FromStringTerm(fromEmail);
        SearchTerm andTerm = new AndTerm(subjectSearch, fromSearch);
        
        // Get all folders
        Folder rootFolder = store.getDefaultFolder();
        Folder[] folders = rootFolder.list();
        
        int totalFound = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Search each folder
        for (Folder folder : folders) {
            if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0) {
                continue;
            }
            
            folder.open(Folder.READ_ONLY);
            
            Message[] found = folder.search(andTerm);
            if (found.length > 0) {
                System.out.println("Found " + found.length + " matching messages in " + folder.getFullName() + ":");
                
                for (Message message : found) {
                    Date sentDate = message.getSentDate();
                    String dateStr = sentDate != null ? sdf.format(sentDate) : "Unknown";
                    
                    System.out.printf("  - [%s] %s%n", dateStr, message.getSubject());
                    totalFound++;
                }
            }
            
            folder.close(false);
        }
        
        System.out.println("Total matching messages found: " + totalFound);
    }
    
    /**
     * Process emails received in the last N days
     */
    private static void processRecentEmails(Store store, int days) throws Exception {
        System.out.println("\nProcessing emails received in the last " + days + " days:");
        
        // Calculate date N days ago
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -days);
        Date startDate = cal.getTime();
        
        // Create search term for received date
        SearchTerm dateTerm = new ReceivedDateTerm(ReceivedDateTerm.GE, startDate);
        
        // Get all folders
        Folder rootFolder = store.getDefaultFolder();
        Folder[] folders = rootFolder.list();
        
        int totalProcessed = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Process each folder
        for (Folder folder : folders) {
            if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0) {
                continue;
            }
            
            folder.open(Folder.READ_ONLY);
            
            Message[] found = folder.search(dateTerm);
            if (found.length > 0) {
                System.out.println("Found " + found.length + " recent messages in " + folder.getFullName() + ":");
                
                for (Message message : found) {
                    Date sentDate = message.getSentDate();
                    Date receivedDate = message.getReceivedDate();
                    String sentStr = sentDate != null ? sdf.format(sentDate) : "Unknown";
                    String receivedStr = receivedDate != null ? sdf.format(receivedDate) : "Unknown";
                    
                    System.out.printf("  - Sent: [%s] Received: [%s] %s%n", 
                                    sentStr, receivedStr, message.getSubject());
                    
                    // Process message content (example)
                    Object content = message.getContent();
                    if (content instanceof String) {
                        String text = (String) content;
                        System.out.println("    Content length: " + text.length() + " characters");
                        // Here you could extract key information from the content
                    } else {
                        System.out.println("    Complex content type: " + message.getContentType());
                    }
                    
                    totalProcessed++;
                }
            }
            
            folder.close(false);
        }
        
        System.out.println("Total recent messages processed: " + totalProcessed);
    }
    
    /**
     * Get the from address as a string
     */
    private static String getFromAddress(Message message) throws MessagingException {
        Address[] addresses = message.getFrom();
        if (addresses == null || addresses.length == 0) {
            return "Unknown";
        }
        return addresses[0].toString();
    }
    
    /**
     * Truncate a string to a maximum length
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        return str.length() <= maxLength ? str : str.substring(0, maxLength - 3) + "...";
    }
}