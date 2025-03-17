package com.intenovation.email.downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles email archive cleanup and organization
 */
public class EmailCleanup {
    private static final Logger LOGGER = Logger.getLogger(EmailCleanup.class.getName());
    
    /**
     * Clean up and organize the email archive
     * 
     * @param progressUpdater Function to report progress
     * @param statusUpdater Function to report status
     * @return Status message
     * @throws InterruptedException if task is cancelled
     */
    public static String cleanupEmailArchive(Consumer<Integer> progressUpdater, Consumer<String> statusUpdater) 
            throws InterruptedException {
        statusUpdater.accept("Starting email archive cleanup...");
        progressUpdater.accept(0);
        
        String storagePath = ImapDownloader.getStoragePath();
        File baseDir = new File(storagePath);
        if (!baseDir.exists()) {
            return "Email archive directory does not exist";
        }
        
        try {
            // Get all folders
            File[] folders = baseDir.listFiles(File::isDirectory);
            if (folders == null || folders.length == 0) {
                return "No folders found in email archive";
            }
            
            int totalFolders = folders.length;
            int processedFolders = 0;
            int totalEmails = FileUtils.countAllEmails(baseDir);
            int processedEmails = 0;
            int duplicatesRemoved = 0;
            int emptyDirectoriesRemoved = 0;
            
            statusUpdater.accept("Found " + totalFolders + " folders with approximately " + 
                              totalEmails + " emails");
            progressUpdater.accept(5);
            
            // Track message IDs to find duplicates
            Map<String, File> messageIds = new HashMap<>();
            
            // Process each folder
            for (File folder : folders) {
                // Skip hidden folders and special files
                if (folder.getName().startsWith(".") || !folder.isDirectory()) {
                    continue;
                }
                
                statusUpdater.accept("Cleaning folder: " + folder.getName());
                
                // Check for interruption
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Task cancelled");
                }
                
                // Process emails in this folder
                File[] messageDirs = folder.listFiles(File::isDirectory);
                if (messageDirs != null) {
                    for (File messageDir : messageDirs) {
                        // Check for interruption
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException("Task cancelled");
                        }
                        
                        processedEmails++;
                        
                        // Check if this message has valid properties
                        File propsFile = new File(messageDir, "message.properties");
                        if (!propsFile.exists()) {
                            continue;
                        }
                        
                        try {
                            // Load properties
                            Properties props = new Properties();
                            props.load(new FileInputStream(propsFile));

                            // Get message ID (prefer the folder version if available)
                            String messageId = props.getProperty("message.id.folder");
                            if (messageId == null || messageId.isEmpty()) {
                                // Fall back to original message ID and sanitize it
                                messageId = props.getProperty("message.id");
                                if (messageId != null && !messageId.isEmpty()) {
                                    messageId = FileUtils.sanitizeFileName(messageId);
                                }
                            }

                            if (messageId != null && !messageId.isEmpty()) {
                                // Check if this is a duplicate
                                if (messageIds.containsKey(messageId)) {
                                    File existingDir = messageIds.get(messageId);

                                    // Only remove if different directories
                                    if (!existingDir.equals(messageDir)) {

                                        // Keep the newer message
                                        long existingTime = existingDir.lastModified();
                                        long currentTime = messageDir.lastModified();
                                        
                                        if (currentTime > existingTime) {
                                            // Current is newer, remove existing
                                            FileUtils.deleteDirectory(existingDir);
                                            messageIds.put(messageId, messageDir);
                                        } else {
                                            // Existing is newer, remove current
                                            FileUtils.deleteDirectory(messageDir);
                                        }
                                        
                                        duplicatesRemoved++;
                                    }
                                } else {
                                    messageIds.put(messageId, messageDir);
                                }
                            }
                            
                            // Update progress
                            if (totalEmails > 0) {
                                int progress = 5 + (90 * processedEmails / totalEmails);
                                progressUpdater.accept(Math.min(95, progress));
                            }
                            
                            // Update status periodically
                            if (processedEmails % 50 == 0) {
                                statusUpdater.accept("Processed " + processedEmails + " of ~" + 
                                                  totalEmails + " emails. Removed " + 
                                                  duplicatesRemoved + " duplicates.");
                            }
                            
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Error reading properties for " + messageDir.getName(), e);
                            // Continue with next message
                        }
                    }
                }
                
                // Check for and remove empty directories
                if (folder.listFiles().length == 0) {
                    folder.delete();
                    emptyDirectoriesRemoved++;
                }
                
                processedFolders++;
                if (totalEmails == 0) { // Only update based on folders if no emails
                    int progress = 5 + (90 * processedFolders / totalFolders);
                    progressUpdater.accept(Math.min(95, progress));
                }
            }
            
            // Finalizing
            statusUpdater.accept("Finalizing cleanup...");
            progressUpdater.accept(95);
            
            // Remove empty directories one more time
            folders = baseDir.listFiles(File::isDirectory);
            if (folders != null) {
                for (File folder : folders) {
                    if (folder.getName().startsWith(".")) continue;
                    
                    if (folder.isDirectory() && folder.listFiles().length == 0) {
                        folder.delete();
                        emptyDirectoriesRemoved++;
                    }
                }
            }
            
            // Compact the database if needed
            File[] allFiles = baseDir.listFiles();
            if (allFiles != null) {
                int totalFiles = allFiles.length;
                if (totalFiles > 10000) {
                    statusUpdater.accept("Database is large (" + totalFiles + " files). " +
                                      "Consider archiving older emails.");
                }
            }
            
            progressUpdater.accept(100);
            
            return "Cleanup complete. Processed " + processedEmails + " emails in " + 
                  processedFolders + " folders. Removed " + duplicatesRemoved + 
                  " duplicates and " + emptyDirectoriesRemoved + " empty directories.";
            
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during cleanup", e);
            return "Error during cleanup: " + e.getMessage();
        }
    }
}