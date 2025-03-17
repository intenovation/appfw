package com.intenovation.emaildownloader;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility methods for file operations
 */
public class FileUtils {
    
    /**
     * Count all emails in the archive
     * 
     * @param baseDir The base directory
     * @return The total number of emails
     */
    public static int countAllEmails(File baseDir) {
        AtomicInteger count = new AtomicInteger(0);
        
        File[] folders = baseDir.listFiles(File::isDirectory);
        if (folders != null) {
            for (File folder : folders) {
                if (folder.getName().startsWith(".")) continue;
                
                File[] messages = folder.listFiles(File::isDirectory);
                if (messages != null) {
                    count.addAndGet(messages.length);
                }
            }
        }
        
        return count.get();
    }
    
    /**
     * Get the size of a folder recursively
     * 
     * @param folder The folder
     * @return The size in bytes
     */
    public static long getFolderSize(File folder) {
        if (folder == null || !folder.exists()) {
            return 0;
        }
        
        long size = 0;
        
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += getFolderSize(file);
                }
            }
        }
        
        return size;
    }
    
    /**
     * Format a size in bytes to a human-readable string
     * 
     * @param bytes The size in bytes
     * @return A formatted string
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * Count the folders in the archive
     * 
     * @param baseDir The base directory
     * @return The number of folders
     */
    public static int countFolders(File baseDir) {
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return 0;
        }
        
        File[] folders = baseDir.listFiles(File::isDirectory);
        if (folders == null) {
            return 0;
        }
        
        int count = 0;
        for (File folder : folders) {
            if (!folder.getName().startsWith(".")) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Count the emails in the archive
     * 
     * @param baseDir The base directory
     * @return The number of emails
     */
    public static int countEmails(File baseDir) {
        return countAllEmails(baseDir);
    }
    
    /**
     * Sanitize a folder name for use in a file path
     * 
     * @param folderName The folder name
     * @return A safe folder name
     */
    public static String sanitizeFolderName(String folderName) {
        // Replace characters that are invalid in file names
        String safe = folderName.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        // Replace sequences of dots or underscores with a single one
        safe = safe.replaceAll("\\.{2,}", ".");
        safe = safe.replaceAll("_{2,}", "_");
        
        // Trim leading/trailing dots and spaces
        safe = safe.replaceAll("^[\\s\\.]+|[\\s\\.]+$", "");
        
        // If the name became empty, use a default
        if (safe.isEmpty()) {
            safe = "unnamed_folder";
        }
        
        return safe;
    }
    
    /**
     * Sanitize a file name for use in a file path
     * 
     * @param fileName The file name
     * @return A safe file name
     */
    public static String sanitizeFileName(String fileName) {
        // Similar to sanitizeFolderName but with a few extras
        if (fileName == null || fileName.isEmpty()) {
            return "unnamed_file";
        }
        
        // Replace characters that are invalid in file names
        String safe = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        // Replace sequences of dots or underscores with a single one
        safe = safe.replaceAll("\\.{2,}", ".");
        safe = safe.replaceAll("_{2,}", "_");
        
        // Trim leading/trailing dots and spaces
        safe = safe.replaceAll("^[\\s\\.]+|[\\s\\.]+$", "");
        
        // Limit length to avoid file system issues
        if (safe.length() > 200) {
            int extensionPos = safe.lastIndexOf(".");
            if (extensionPos > 0) {
                String name = safe.substring(0, extensionPos);
                String extension = safe.substring(extensionPos);
                safe = name.substring(0, Math.min(name.length(), 195)) + extension;
            } else {
                safe = safe.substring(0, 200);
            }
        }
        
        // If the name became empty, use a default
        if (safe.isEmpty()) {
            safe = "unnamed_file";
        }
        
        return safe;
    }
    
    /**
     * Delete a directory and its contents
     * 
     * @param directory The directory to delete
     * @return true if successful
     */
    public static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }
}