package com.intenovation.email.downloader;

import com.intenovation.appfw.systemtray.*;

import javax.mail.*;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles downloading emails from IMAP server
 */
public class EmailDownloader extends BackgroundTask {
    private static final Logger LOGGER = Logger.getLogger(EmailDownloader.class.getName());
    private final boolean newOnly;

    /**
     * Create a new Email Downloader task for full sync
     */
    public EmailDownloader() {
        this(false, "Full Email Sync", "Downloads all emails from the IMAP server that aren't already downloaded", 3600 * 12);
    }

    /**
     * Create a new Email Downloader task for new emails only
     *
     * @param syncIntervalMinutes Sync interval in minutes
     */
    public EmailDownloader(int syncIntervalMinutes) {
        this(true, "New Emails Only", "Downloads only new emails since last check", syncIntervalMinutes * 60);
    }

    /**
     * Private constructor with common initialization
     *
     * @param newOnly Whether to download only new emails
     * @param name Task name
     * @param description Task description
     * @param intervalSeconds Interval in seconds
     */
    private EmailDownloader(boolean newOnly, String name, String description, int intervalSeconds) {
        super(name, description, intervalSeconds, true);
        this.newOnly = newOnly;
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
        LOGGER.info("Starting " + (newOnly ? "New Emails Sync" : "Full Email Sync"));

        // Create a logging wrapper around the callback
        ProgressStatusCallback loggingCallback = new ProgressStatusCallback() {
            @Override
            public void update(int percent, String message) {
                // Update the original callback
                callback.update(percent, message);

                // Log the progress
                LOGGER.info(String.format("[%s] %d%% - %s",
                        (newOnly ? "New Emails Sync" : "Full Email Sync"),
                        percent, message));
            }
        };

        try {
            // Execute the download with our logging callback
            String result;
            if (newOnly) {
                result = downloadNewEmails(loggingCallback);
            } else {
                result = downloadAllEmails(loggingCallback);
            }

            LOGGER.info((newOnly ? "New Emails Sync" : "Full Email Sync") + " completed: " + result);
            return result;
        } catch (InterruptedException e) {
            LOGGER.warning((newOnly ? "New Emails Sync" : "Full Email Sync") + " was interrupted");
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, (newOnly ? "New Emails Sync" : "Full Email Sync") + " error", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Download all emails from the IMAP server
     *
     * @param progressUpdater Function to report progress
     * @return Status message
     * @throws InterruptedException if task is cancelled
     */
    public static String downloadAllEmails(ProgressStatusCallback progressUpdater)
            throws InterruptedException {
        return downloadEmails(progressUpdater, false);
    }

    /**
     * Download only new emails from the IMAP server
     *
     * @param progressUpdater Function to report progress
     * @return Status message
     * @throws InterruptedException if task is cancelled
     */
    public static String downloadNewEmails(ProgressStatusCallback progressUpdater)
            throws InterruptedException {
        return downloadEmails(progressUpdater, true);
    }

    /**
     * Download emails from the IMAP server
     *
     * @param progressUpdater Function to report progress
     * @param newOnly Whether to download only new emails
     * @return Status message
     * @throws InterruptedException if task is cancelled
     */
    private static String downloadEmails(ProgressStatusCallback progressUpdater,
                                         boolean newOnly) throws InterruptedException {
        // Get settings from ImapDownloader
        String imapHost = ImapDownloader.getImapHost();
        String imapPort = ImapDownloader.getImapPort();
        String username = ImapDownloader.getUsername();
        String password = ImapDownloader.getPassword();
        boolean useSSL = ImapDownloader.isUseSSL();
        String storagePath = ImapDownloader.getStoragePath();

        progressUpdater.update(0, "Connecting to IMAP server " + imapHost + "...");

        // Create the base directory if it doesn't exist
        File baseDir = new File(storagePath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        // Index existing message IDs to avoid duplicate downloads
        Set<String> existingMessageIds = ConcurrentHashMap.newKeySet();
        if (!newOnly) {
            progressUpdater.update(5, "Indexing existing messages to avoid duplicates...");
            indexExistingMessages(baseDir, existingMessageIds);
            progressUpdater.update(10, "Found " + existingMessageIds.size() + " existing messages");
        }

        // Create .lastSync file to track last sync time
        File lastSyncFile = new File(baseDir, ".lastSync");
        Date lastSyncDate = null;

        if (newOnly && lastSyncFile.exists()) {
            try {
                String dateString = Files.readAllLines(lastSyncFile.toPath()).get(0);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                lastSyncDate = sdf.parse(dateString);
                progressUpdater.update(5, "Downloading emails since " + dateString);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error reading last sync date", e);
                // Continue without last sync date
                lastSyncDate = null;
                progressUpdater.update(5, "Last sync date not available, downloading all new emails");
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

            progressUpdater.update(15, "Found " + totalFolders + " folders");

            int processedFolders = 0;
            int totalEmails = 0;
            int downloadedEmails = 0;
            int skippedEmails = 0;

            // First pass to count total emails
            if (!newOnly) { // Only count for full sync
                progressUpdater.update(15, "Counting emails...");
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
                progressUpdater.update(20, "Processing folder: " + folderName);

                try {
                    // Open the folder
                    folder.open(Folder.READ_ONLY);

                    // Create folder directory
                    String folderPath = storagePath + File.separator + FileUtils.sanitizeFolderName(folderName);
                    File folderDir = new File(folderPath);
                    if (!folderDir.exists()) {
                        folderDir.mkdirs();
                    }

                    // Create messages directory within folder
                    File messagesDir = new File(folderDir, "messages");
                    if (!messagesDir.exists()) {
                        messagesDir.mkdirs();
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
                        progressUpdater.update(30, "Found " + messages.length + " emails in " + folderName);
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

                            // Skip if this message already exists and we're only getting new messages
                            String sanitizedId = FileUtils.sanitizeFileName(messageId);
                            File msgDir = new File(messagesDir, sanitizedId);
                            
                            // Skip if this message already exists in our index or on disk
                            if (existingMessageIds.contains(messageId) || msgDir.exists()) {
                                skippedEmails++;
                                continue;
                            }

                            // Message doesn't exist, download it
                            msgDir.mkdirs();

                            // Save message content
                            saveMessageContent(msgDir, message);

                            // Save message properties
                            saveMessageProperties(msgDir, message);

                            // Add to the set of existing messages to avoid duplicates in same run
                            existingMessageIds.add(messageId);
                            
                            downloadedEmails++;

                            // Update progress for full sync
                            if (!newOnly && totalEmails > 0) {
                                int emailProgress = 20 + (75 * (downloadedEmails + skippedEmails) / totalEmails);
                                progressUpdater.update(Math.min(95, emailProgress), "Downloaded " + 
                                    downloadedEmails + " new emails, skipped " + skippedEmails + " existing emails");
                            } else {
                                // For new emails, use a simpler progress calculation
                                progressUpdater.update(20 + (75 * (i + 1) / Math.max(1, messages.length)),
                                    "Downloaded " + downloadedEmails + " new emails, skipped " + skippedEmails + " existing emails");
                            }

                            // Update status message periodically
                            if (downloadedEmails % 10 == 0 || i == messages.length - 1) {
                                progressUpdater.update(20 + (75 * (i + 1) / Math.max(1, messages.length)),
                                        "Downloaded " + downloadedEmails + " emails (" +
                                                (i + 1) + "/" + messages.length + " from " + folderName + 
                                                "), skipped " + skippedEmails + " emails");
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
                    progressUpdater.update(Math.min(95, folderProgress), "Skipped " + skippedEmails + " existing emails");
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
            progressUpdater.update(95, "Finalizing download...");

            String resultMessage;
            if (downloadedEmails == 0) {
                resultMessage = "No new emails to download. " + skippedEmails + " emails already exist locally.";
            } else {
                resultMessage = "Download complete. " + downloadedEmails + " emails downloaded from " +
                        processedFolders + " folders. " + skippedEmails + " emails skipped.";
            }
            
            progressUpdater.update(100, resultMessage);
            return resultMessage;

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error downloading emails", e);
            return "Error during email download: " + e.getMessage();
        }
    }

    /**
     * Index existing message IDs to avoid re-downloading
     *
     * @param baseDir The base directory containing email folders
     * @param existingIds Set to populate with existing message IDs
     */
    private static void indexExistingMessages(File baseDir, Set<String> existingIds) {
        // Get all folders
        File[] folders = baseDir.listFiles(file ->
                file.isDirectory() && !file.getName().startsWith("."));

        if (folders == null) {
            return;
        }

        for (File folder : folders) {
            // Check for messages in "messages" directory (new structure)
            File messagesDir = new File(folder, "messages");
            if (messagesDir.exists() && messagesDir.isDirectory()) {
                File[] messageDirs = messagesDir.listFiles(File::isDirectory);
                if (messageDirs != null) {
                    for (File messageDir : messageDirs) {
                        // Check if this is a valid message directory
                        File propertiesFile = new File(messageDir, "message.properties");
                        if (propertiesFile.exists()) {
                            try (FileInputStream fis = new FileInputStream(propertiesFile)) {
                                // Load properties to get message ID
                                Properties props = new Properties();
                                props.load(fis);

                                // Try to get the message ID
                                String messageId = props.getProperty("message.id");
                                if (messageId != null && !messageId.isEmpty()) {
                                    existingIds.add(messageId);
                                }

                                // Also add the folder name version
                                String folderMessageId = props.getProperty("message.id.folder");
                                if (folderMessageId != null && !folderMessageId.isEmpty()) {
                                    existingIds.add(folderMessageId);
                                }

                                // If no message ID, use the folder name
                                if ((messageId == null || messageId.isEmpty()) &&
                                        (folderMessageId == null || folderMessageId.isEmpty())) {
                                    existingIds.add(messageDir.getName());
                                }
                            } catch (IOException e) {
                                LOGGER.log(Level.WARNING, "Error reading properties file: " + propertiesFile, e);
                            }
                        }
                    }
                }
            }

            // Check for messages in the old structure as well
            File[] oldMessageDirs = folder.listFiles(file ->
                    file.isDirectory() &&
                            !file.getName().equals("messages") &&
                            !file.getName().startsWith(".") &&
                            new File(file, "message.properties").exists());

            if (oldMessageDirs != null) {
                for (File messageDir : oldMessageDirs) {
                    // Check if this is a valid message directory
                    File propertiesFile = new File(messageDir, "message.properties");
                    if (propertiesFile.exists()) {
                        try (FileInputStream fis = new FileInputStream(propertiesFile)) {
                            // Load properties to get message ID
                            Properties props = new Properties();
                            props.load(fis);

                            // Try to get the message ID
                            String messageId = props.getProperty("message.id");
                            if (messageId != null && !messageId.isEmpty()) {
                                existingIds.add(messageId);
                            }

                            // If no message ID, use the folder name
                            if (messageId == null || messageId.isEmpty()) {
                                existingIds.add(messageDir.getName());
                            }
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Error reading properties file: " + propertiesFile, e);
                        }
                    }
                }
            }
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
    static void saveMessageContent(File msgDir, Message message) throws Exception {
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
    static void saveMessageProperties(File msgDir, Message message)
            throws MessagingException, IOException {
        Properties props = new Properties();

        // Message ID
        String messageId = getMessageId(message);
        if (messageId != null) {
            props.setProperty("message.id", messageId);
            // Also store the sanitized version that was used for the directory name
            props.setProperty("message.id.folder", FileUtils.sanitizeFileName(messageId));
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

        // Reply-To (Add this new section)
        Address[] replyToAddresses = message.getReplyTo();
        if (replyToAddresses != null && replyToAddresses.length > 0) {
            StringBuilder replyTo = new StringBuilder();
            for (Address address : replyToAddresses) {
                if (replyTo.length() > 0) {
                    replyTo.append(", ");
                }
                replyTo.append(address.toString());
            }
            props.setProperty("reply.to", replyTo.toString());
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