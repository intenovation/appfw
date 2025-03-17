package com.intenovation.emaildownloader;

import javax.mail.*;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles downloading emails from IMAP server
 */
public class EmailDownloader {
    private static final Logger LOGGER = Logger.getLogger(EmailDownloader.class.getName());
    
    /**
     * Download all emails from the IMAP server
     * 
     * @param progressUpdater Function to report progress
     * @param statusUpdater Function to report status
     * @return Status message
     * @throws InterruptedException if task is cancelled
     */
    public static String downloadAllEmails(Consumer<Integer> progressUpdater, Consumer<String> statusUpdater) 
            throws InterruptedException {
        return downloadEmails(progressUpdater, statusUpdater, false);
    }
    
    /**
     * Download only new emails from the IMAP server
     * 
     * @param progressUpdater Function to report progress
     * @param statusUpdater Function to report status
     * @return Status message
     * @throws InterruptedException if task is cancelled
     */
    public static String downloadNewEmails(Consumer<Integer> progressUpdater, Consumer<String> statusUpdater) 
            throws InterruptedException {
        return downloadEmails(progressUpdater, statusUpdater, true);
    }
    
    /**
     * Download emails from the IMAP server
     * 
     * @param progressUpdater Function to report progress
     * @param statusUpdater Function to report status
     * @param newOnly Whether to download only new emails
     * @return Status message
     * @throws InterruptedException if task is cancelled
     */
    private static String downloadEmails(Consumer<Integer> progressUpdater, Consumer<String> statusUpdater, 
                                        boolean newOnly) throws InterruptedException {
        // Get settings from ImapDownloader
        String imapHost = ImapDownloader.getImapHost();
        String imapPort = ImapDownloader.getImapPort();
        String username = ImapDownloader.getUsername();
        String password = ImapDownloader.getPassword();
        boolean useSSL = ImapDownloader.isUseSSL();
        String storagePath = ImapDownloader.getStoragePath();
        
        statusUpdater.accept("Connecting to IMAP server " + imapHost + "...");
        progressUpdater.accept(0);
        
        // Create the base directory if it doesn't exist
        File baseDir = new File(storagePath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        
        // Create .lastSync file to track last sync time
        File lastSyncFile = new File(baseDir, ".lastSync");
        Date lastSyncDate = null;
        
        if (newOnly && lastSyncFile.exists()) {
            try {
                String dateString = Files.readAllLines(lastSyncFile.toPath()).get(0);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                lastSyncDate = sdf.parse(dateString);
                statusUpdater.accept("Downloading emails since " + dateString);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error reading last sync date", e);
                // Continue without last sync date
                lastSyncDate = null;
                statusUpdater.accept("Last sync date not available, downloading all new emails");
            }
        }
        
        try {
            // Set up connection properties
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", imapHost);
            props.put("mail.imaps.port", imapPort);
            props.put("mail.imaps.ssl.enable", String.valueOf(useSSL));
            props.put("mail.imaps.ssl.trust", "*");
            
            // Create the session
            Session session = Session.getInstance(props);
            
            // Connect to the server
            Store store = session.getStore("imaps");
            store.connect(imapHost, username, password);
            
            // Get the default folder
            Folder defaultFolder = store.getDefaultFolder();
            
            // Check for interruption
            if (Thread.currentThread().isInterrupted()) {
                store.close();
                throw new InterruptedException("Task cancelled");
            }
            
            // Get all folders
            Folder[] folders = defaultFolder.list();
            int totalFolders = folders.length;
            
            statusUpdater.accept("Found " + totalFolders + " folders");
            progressUpdater.accept(5);
            
            int processedFolders = 0;
            int totalEmails = 0;
            int downloadedEmails = 0;
            
            // First pass to count total emails
            if (!newOnly) { // Only count for full sync
                statusUpdater.accept("Counting emails...");
                for (Folder folder : folders) {
                    // Skip non-selectable folders
                    if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0) {
                        continue;
                    }
                    
                    try {
                        folder.open(Folder.READ_ONLY);
                        totalEmails += folder.getMessageCount();
                        folder.close(false);
                    } catch (MessagingException e) {
                        LOGGER.log(Level.WARNING, "Error counting messages in folder: " + folder.getName(), e);
                    }
                    
                    // Check for interruption
                    if (Thread.currentThread().isInterrupted()) {
                        store.close();
                        throw new InterruptedException("Task cancelled");
                    }
                }
            }
            
            // Process each folder
            for (Folder folder : folders) {
                // Skip non-selectable folders
                if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0) {
                    continue;
                }
                
                String folderName = folder.getFullName();
                statusUpdater.accept("Processing folder: " + folderName);
                
                try {
                    // Open the folder
                    folder.open(Folder.READ_ONLY);
                    
                    // Create folder directory
                    String folderPath = storagePath + File.separator + FileUtils.sanitizeFolderName(folderName);
                    File folderDir = new File(folderPath);
                    if (!folderDir.exists()) {
                        folderDir.mkdirs();
                    }
                    
                    // Get messages from this folder
                    Message[] messages;
                    
                    if (newOnly && lastSyncDate != null) {
                        // Get only messages since last sync
                        messages = folder.search(new ReceivedDateTerm(ComparisonTerm.GE, lastSyncDate));
                    } else {
                        // Get all messages
                        messages = folder.getMessages();
                    }
                    
                    if (messages.length > 0) {
                        statusUpdater.accept("Found " + messages.length + " emails in " + folderName);
                    }
                    
                    // Process each message
                    for (int i = 0; i < messages.length; i++) {
                        Message message = messages[i];
                        
                        // Check for interruption
                        if (Thread.currentThread().isInterrupted()) {
                            folder.close(false);
                            store.close();
                            throw new InterruptedException("Task cancelled");
                        }
                        
                        try {
                            // Get message ID or fallback to a unique identifier
                            String messageId = getMessageId(message);
                            if (messageId == null || messageId.isEmpty()) {
                                // Create a unique ID based on folder, date and subject
                                String subject = message.getSubject();
                                Date sentDate = message.getSentDate();
                                if (subject == null) subject = "No Subject";
                                if (sentDate == null) sentDate = new Date();
                                
                                messageId = folderName + "-" + 
                                         new SimpleDateFormat("yyyyMMdd-HHmmss").format(sentDate) + "-" +
                                         Math.abs(subject.hashCode());
                            }
                            
                            // Create message directory
                            File msgDir = new File(folderDir, FileUtils.sanitizeFileName(messageId));
                            
                            // Skip if this message already exists and we're only getting new messages
                            if (msgDir.exists()) {
                                continue;
                            }
                            
                            msgDir.mkdirs();
                            
                            // Save message content
                            saveMessageContent(msgDir, message);
                            
                            // Save message properties
                            saveMessageProperties(msgDir, message);
                            
                            downloadedEmails++;
                            
                            // Update progress for full sync
                            if (!newOnly && totalEmails > 0) {
                                int emailProgress = 5 + (90 * downloadedEmails / totalEmails);
                                progressUpdater.accept(Math.min(95, emailProgress));
                            } else {
                                // For new emails, use a simpler progress calculation
                                progressUpdater.accept(5 + (90 * (i + 1) / Math.max(1, messages.length)));
                            }
                            
                            // Update status message periodically
                            if (downloadedEmails % 10 == 0 || i == messages.length - 1) {
                                statusUpdater.accept("Downloaded " + downloadedEmails + " emails (" + 
                                                  (i + 1) + "/" + messages.length + " from " + folderName + ")");
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error processing message", e);
                            // Continue with next message
                        }
                    }
                    
                    // Close the folder
                    folder.close(false);
                    
                } catch (MessagingException e) {
                    LOGGER.log(Level.WARNING, "Error processing folder: " + folderName, e);
                    // Continue with next folder
                }

                processedFolders++;
                int folderProgress = 5 + (90 * processedFolders / totalFolders);
                if (downloadedEmails == 0) { // Only update if no emails were downloaded
                    progressUpdater.accept(Math.min(95, folderProgress));
                }
            }

            // Close the connection
            store.close();

            // Update last sync time
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Files.write(lastSyncFile.toPath(),
                        Collections.singletonList(sdf.format(new Date())),
                        StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error updating last sync time", e);
            }

            // Finalizing
            statusUpdater.accept("Finalizing download...");
            progressUpdater.accept(100);

            if (downloadedEmails == 0) {
                return "No new emails to download.";
            } else {
                return "Download complete. " + downloadedEmails + " emails downloaded from " +
                        processedFolders + " folders.";
            }

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error downloading emails", e);
            return "Error during email download: " + e.getMessage();
        }
    }

    /**
     * Get the Message-ID header from an email message
     *
     * @param message The email message
     * @return The Message-ID or null if not found
     */
    private static String getMessageId(Message message) {
        try {
            String[] headers = message.getHeader("Message-ID");
            if (headers != null && headers.length > 0) {
                return headers[0];
            }
        } catch (MessagingException e) {
            LOGGER.log(Level.WARNING, "Error getting Message-ID", e);
        }
        return null;
    }

    /**
     * Save the content of an email message to files
     *
     * @param msgDir The message directory
     * @param message The email message
     * @throws Exception If an error occurs
     */
    private static void saveMessageContent(File msgDir, Message message) throws Exception {
        Object content = message.getContent();

        // Create content.txt for the main message content
        File contentFile = new File(msgDir, "content.txt");
        FileWriter writer = new FileWriter(contentFile);

        // Handle different content types
        if (content instanceof String) {
            // Simple text message
            writer.write((String) content);
        } else if (content instanceof Multipart) {
            // Multipart message (with possible attachments)
            Multipart multipart = (Multipart) content;
            processMultipart(multipart, writer, msgDir);
        } else if (content instanceof InputStream) {
            // Input stream content
            InputStream is = (InputStream) content;
            byte[] buffer = new byte[4096];
            int bytesRead;
            StringBuilder sb = new StringBuilder();
            while ((bytesRead = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }
            writer.write(sb.toString());
            is.close();
        } else {
            // Unknown content type
            writer.write("Content type not supported: " + message.getContentType());
        }

        writer.close();
    }

    /**
     * Process a multipart message
     *
     * @param multipart The multipart content
     * @param writer The writer for the main content
     * @param msgDir The message directory
     */
    private static void processMultipart(Multipart multipart, FileWriter writer, File msgDir) throws Exception {
        int count = multipart.getCount();

        // Create attachments directory if needed
        File attachmentsDir = new File(msgDir, "attachments");
        boolean hasAttachments = false;

        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            // Check if this is an attachment
            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                    bodyPart.getFileName() != null) {
                // This is an attachment
                if (!hasAttachments) {
                    attachmentsDir.mkdirs();
                    hasAttachments = true;
                }

                // Save the attachment
                String fileName = bodyPart.getFileName();
                if (fileName == null) {
                    fileName = "attachment-" + (i + 1);
                }

                fileName = FileUtils.sanitizeFileName(fileName);
                File attachmentFile = new File(attachmentsDir, fileName);

                try (InputStream is = bodyPart.getInputStream();
                     FileOutputStream fos = new FileOutputStream(attachmentFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }

                // Add a note about the attachment to the content file
                writer.write("\n[ATTACHMENT: " + fileName + "]\n");

            } else {
                // This is the message body or an inline part
                Object content = bodyPart.getContent();

                if (content instanceof String) {
                    // Text content
                    writer.write((String) content);
                    writer.write("\n");
                } else if (content instanceof Multipart) {
                    // Nested multipart
                    processMultipart((Multipart) content, writer, msgDir);
                } else if (bodyPart.isMimeType("text/html")) {
                    // HTML content - create a separate HTML file
                    File htmlFile = new File(msgDir, "content.html");
                    try (FileWriter htmlWriter = new FileWriter(htmlFile)) {
                        htmlWriter.write(bodyPart.getContent().toString());
                    }

                    // Add a note about the HTML content
                    writer.write("\n[HTML CONTENT AVAILABLE IN content.html]\n");
                }
            }
        }
    }

    /**
     * Save message properties to a file
     *
     * @param msgDir The message directory
     * @param message The message
     * @throws MessagingException If a messaging error occurs
     * @throws IOException If an I/O error occurs
     */
    private static void saveMessageProperties(File msgDir, Message message)
            throws MessagingException, IOException {
        Properties props = new Properties();

        // Message ID
        String messageId = getMessageId(message);
        if (messageId != null) {
            props.setProperty("message.id", messageId);
        }

        // Subject
        String subject = message.getSubject();
        if (subject != null) {
            props.setProperty("subject", subject);
        } else {
            props.setProperty("subject", "(No Subject)");
        }

        // From
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses != null && fromAddresses.length > 0) {
            StringBuilder from = new StringBuilder();
            for (Address address : fromAddresses) {
                if (from.length() > 0) {
                    from.append(", ");
                }
                from.append(address.toString());
            }
            props.setProperty("from", from.toString());
        }

        // To
        Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
        if (toAddresses != null) {
            StringBuilder to = new StringBuilder();
            for (Address address : toAddresses) {
                if (to.length() > 0) {
                    to.append(", ");
                }
                to.append(address.toString());
            }
            props.setProperty("to", to.toString());
        }

        // CC
        Address[] ccAddresses = message.getRecipients(Message.RecipientType.CC);
        if (ccAddresses != null) {
            StringBuilder cc = new StringBuilder();
            for (Address address : ccAddresses) {
                if (cc.length() > 0) {
                    cc.append(", ");
                }
                cc.append(address.toString());
            }
            props.setProperty("cc", cc.toString());
        }

        // Date
        Date sentDate = message.getSentDate();
        if (sentDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            props.setProperty("sent.date", sdf.format(sentDate));
        }

        Date receivedDate = message.getReceivedDate();
        if (receivedDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            props.setProperty("received.date", sdf.format(receivedDate));
        }

        // Size
        props.setProperty("size.bytes", String.valueOf(message.getSize()));

        // Save properties to file
        File propsFile = new File(msgDir, "message.properties");
        try (FileOutputStream out = new FileOutputStream(propsFile)) {
            props.store(out, "Email Message Properties");
        }
    }
}