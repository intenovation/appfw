package com.intenovation.email.downloader;

import com.intenovation.appfw.systemtray.ProgressStatusCallback;

import java.io.File;
import java.io.IOException;
import java.time.Year;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.*;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Helper class for downloading emails by year.
 * This is a simpler approach that doesn't require modifying the main application structure.
 */
public class YearDownloader {
    private static final Logger LOGGER = Logger.getLogger(YearDownloader.class.getName());
    
    /**
     * Download emails starting from a specific year
     * 
     * @param year The year to start from (0 for all years)
     * @param showProgressDialog Whether to show a progress dialog
     * @return Result message
     */
    public static String downloadFromYear(int year, boolean showProgressDialog) {
        try {
            // Create a callback that can update a progress dialog if requested
            ProgressDialog dialog = null;
            
            if (showProgressDialog) {
                dialog = new ProgressDialog("Downloading Emails", 
                    "Downloading emails" + (year > 0 ? " from year " + year : ""));
                dialog.setVisible(true);
            }
            
            final ProgressDialog finalDialog = dialog;
            
            ProgressStatusCallback callback = new ProgressStatusCallback() {
                @Override
                public void update(int percent, String message) {
                    LOGGER.info(percent + "% - " + message);
                    
                    if (finalDialog != null) {
                        SwingUtilities.invokeLater(() -> {
                            finalDialog.updateProgress(percent, message);
                        });
                    }
                }
            };
            
            String result = downloadFromYear(callback, year);
            
            if (finalDialog != null) {
                SwingUtilities.invokeLater(() -> {
                    finalDialog.dispose();
                });
            }
            
            return result;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error downloading emails", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Download emails starting from a specific year with a progress callback
     * 
     * @param callback The progress callback
     * @param year The year to start from (0 for all years)
     * @return Result message
     * @throws Exception If an error occurs
     */
    public static String downloadFromYear(ProgressStatusCallback callback, int year) throws Exception {
        // Get settings from ImapDownloader
        String imapHost = ImapDownloader.getImapHost();
        String imapPort = ImapDownloader.getImapPort();
        String username = ImapDownloader.getUsername();
        String password = ImapDownloader.getPassword();
        boolean useSSL = ImapDownloader.isUseSSL();
        String storagePath = ImapDownloader.getStoragePath();

        callback.update(0, "Connecting to IMAP server " + imapHost + "...");

        // Create the base directory if it doesn't exist
        File baseDir = new File(storagePath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        // Set up connection properties
        java.util.Properties props = new java.util.Properties();
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

        try {
            // Get the default folder
            Folder defaultFolder = store.getDefaultFolder();
            
            // Get all folders
            Folder[] folders = defaultFolder.list();
            
            callback.update(10, "Found " + folders.length + " folders");
            
            // Create date for year filtering if needed
            Calendar cal = null;
            if (year > 0) {
                cal = Calendar.getInstance();
                cal.set(year, Calendar.JANUARY, 1, 0, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);
                callback.update(15, "Filtering emails from year " + year + " onwards");
            }
            
            int totalFolders = folders.length;
            int processedFolders = 0;
            int downloadedEmails = 0;
            int skippedEmails = 0;
            
            // Process each folder
            for (Folder folder : folders) {
                // Skip non-selectable folders
                if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0) {
                    continue;
                }
                
                String folderName = folder.getFullName();
                callback.update(20 + (75 * processedFolders / totalFolders), 
                    "Processing folder: " + folderName);
                
                try {
                    // Open the folder
                    folder.open(Folder.READ_ONLY);
                    
                    // Get messages from this folder with year filtering if needed
                    Message[] messages;
                    if (year > 0 && cal != null) {
                        // Get only messages from the specified year onwards
                        SearchTerm dateTerm = new ReceivedDateTerm(
                            ComparisonTerm.GE, cal.getTime());
                        messages = folder.search(dateTerm);
                    } else {
                        // Get all messages
                        messages = folder.getMessages();
                    }
                    
                    callback.update(20 + (75 * processedFolders / totalFolders),
                        "Found " + messages.length + " emails in " + folderName);
                    
                    // Process each message - use the EmailDownloader's methods
                    for (int i = 0; i < messages.length; i++) {
                        Message message = messages[i];
                        
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
                                        new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(sentDate) + "-" +
                                        Math.abs(subject.hashCode());
                            }

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

                            // Check if this message already exists
                            String sanitizedId = FileUtils.sanitizeFileName(messageId);
                            File msgDir = new File(messagesDir, sanitizedId);
                            
                            if (msgDir.exists()) {
                                skippedEmails++;
                            } else {
                                // Message doesn't exist, download it
                                msgDir.mkdirs();

                                // Save message content - reuse EmailDownloader's methods
                                saveMessageContent(msgDir, message);
                                
                                // Save message properties - reuse EmailDownloader's methods
                                saveMessageProperties(msgDir, message, messageId);
                                
                                downloadedEmails++;
                            }
                            
                            // Update progress periodically
                            if (i % 10 == 0 || i == messages.length - 1) {
                                callback.update(20 + (75 * (processedFolders + (i / messages.length)) / totalFolders),
                                    "Processed " + (i + 1) + "/" + messages.length + " in folder " + folderName + 
                                    ", Downloaded: " + downloadedEmails + ", Skipped: " + skippedEmails);
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error processing message", e);
                        }
                    }
                    
                    // Close the folder
                    folder.close(false);
                    
                } catch (MessagingException e) {
                    LOGGER.log(Level.WARNING, "Error processing folder: " + folderName, e);
                }
                
                processedFolders++;
            }
            
            // Close the store
            store.close();
            
            callback.update(95, "Finalizing...");
            
            String resultMessage;
            if (downloadedEmails == 0) {
                resultMessage = "No new emails downloaded. " + skippedEmails + " emails skipped.";
            } else {
                resultMessage = "Download complete. " + downloadedEmails + " emails downloaded, " + 
                    skippedEmails + " emails skipped.";
            }
            
            callback.update(100, resultMessage);
            return resultMessage;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error downloading emails", e);
            store.close();
            throw e;
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
     * This is a copy of EmailDownloader's method to avoid dependencies
     * 
     * @param msgDir The message directory
     * @param message The message to save
     * @throws Exception If there is an error
     */
    private static void saveMessageContent(File msgDir, Message message) throws Exception {
        // Delegate to EmailDownloader instead of copying the code
        // This version just provides a simplified stub that can be expanded if needed
        EmailDownloader.saveMessageContent(msgDir, message);
    }
    
    /**
     * Save the properties of an email message
     * This is a copy of EmailDownloader's method to avoid dependencies
     * 
     * @param msgDir The message directory
     * @param message The message
     * @param messageId The message ID
     * @throws MessagingException If there is a messaging error

     */
    private static void saveMessageProperties(File msgDir, Message message, String messageId)
            throws MessagingException, IOException {
        // Delegate to EmailDownloader instead of copying the code
        // This version just provides a simplified stub that can be expanded if needed
        EmailDownloader.saveMessageProperties(msgDir, message);
    }
    
    /**
     * Simple dialog for showing download progress
     */
    static class ProgressDialog extends JDialog {
        private JProgressBar progressBar;
        private JLabel messageLabel;
        private boolean userCancelled = false;
        
        public ProgressDialog(String title, String initialMessage) {
            super((Frame)null, title, false);
            
            // Set up the UI
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // Handle user cancellation
                    userCancelled = true;
                    dispose();
                }
            });
            
            // Create components
            progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            messageLabel = new JLabel(initialMessage);
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> {
                userCancelled = true;
                dispose();
            });
            
            // Layout
            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            contentPanel.add(messageLabel, BorderLayout.NORTH);
            contentPanel.add(progressBar, BorderLayout.CENTER);
            contentPanel.add(cancelButton, BorderLayout.SOUTH);
            
            setContentPane(contentPanel);
            setSize(400, 150);
            setLocationRelativeTo(null);
        }
        
        public void updateProgress(int percent, String message) {
            progressBar.setValue(percent);
            messageLabel.setText(message);
        }
        
        public boolean wasUserCancelled() {
            return userCancelled;
        }
    }
    
    /**
     * Command-line interface to download emails
     */
    public static void main(String[] args) {
        try {
            int yearToStart = 0;
            
            if (args.length > 0) {
                try {
                    yearToStart = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid year parameter: " + args[0]);
                    System.out.println("Usage: java YearDownloader [year]");
                    System.out.println("  where [year] is the starting year (0 for all years)");
                    return;
                }
            } else {
                // If no arguments provided, default to current year
                yearToStart = Year.now().getValue();
                System.out.println("No year specified, defaulting to current year: " + yearToStart);
            }
            
            System.out.println("Starting email download from year: " + 
                (yearToStart > 0 ? yearToStart : "all years"));
                
            String result = downloadFromYear(yearToStart, true);
            System.out.println("Result: " + result);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}