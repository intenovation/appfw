package com.intenovation.email.utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * Utility class for sanitizing and validating email addresses.
 * Helps prevent javax.mail.internet.AddressException due to invalid characters.
 */
public class EmailAddressSanitizer {
    private static final Logger LOGGER = Logger.getLogger(EmailAddressSanitizer.class.getName());
    
    // Basic pattern for email validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
        "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    // Pattern for allowed characters in domain part according to RFC 5322
    private static final Pattern VALID_DOMAIN_CHARS = Pattern.compile("^[a-zA-Z0-9.\\-]+$");
    
    /**
     * Sanitizes an email address by removing illegal characters.
     * 
     * @param emailAddress The potentially invalid email address
     * @return A sanitized version of the email address, or null if it cannot be fixed
     */
    public static String sanitizeEmailAddress(String emailAddress) {
        if (emailAddress == null || emailAddress.isEmpty()) {
            return null;
        }
        
        try {
            // Try to parse as is first - if it's valid, return it unchanged
            new InternetAddress(emailAddress).validate();
            return emailAddress;
        } catch (AddressException ex) {
            LOGGER.log(Level.FINE, "Invalid email address, attempting sanitization: " + emailAddress, ex);
            
            // Split into local and domain parts
            int atPos = emailAddress.lastIndexOf('@');
            if (atPos <= 0 || atPos == emailAddress.length() - 1) {
                LOGGER.log(Level.WARNING, "Email address missing @ symbol or has invalid format: " + emailAddress);
                return createFallbackAddress(emailAddress);
            }
            
            String localPart = emailAddress.substring(0, atPos);
            String domainPart = emailAddress.substring(atPos + 1);
            
            // Clean up domain part - remove any non-alphanumeric, dot, or hyphen
            StringBuilder cleanDomain = new StringBuilder();
            for (char c : domainPart.toCharArray()) {
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                    (c >= '0' && c <= '9') || c == '.' || c == '-') {
                    cleanDomain.append(c);
                }
            }
            
            // Ensure domain has at least one dot
            String sanitizedDomain = cleanDomain.toString();
            if (!sanitizedDomain.contains(".")) {
                LOGGER.log(Level.WARNING, "Sanitized domain lacks a dot: " + sanitizedDomain);
                return createFallbackAddress(emailAddress);
            }
            
            // Recombine parts
            String sanitizedEmail = localPart + "@" + sanitizedDomain;
            
            // Validate the sanitized address
            try {
                new InternetAddress(sanitizedEmail).validate();
                LOGGER.log(Level.INFO, "Successfully sanitized email: " + emailAddress + " -> " + sanitizedEmail);
                return sanitizedEmail;
            } catch (AddressException e) {
                LOGGER.log(Level.WARNING, "Failed to sanitize email: " + emailAddress, e);
                return createFallbackAddress(emailAddress);
            }
        }
    }
    
    /**
     * Creates a fallback email address when sanitization fails.
     * This creates a placeholder that won't cause exceptions but preserves
     * some information about the original address.
     * 
     * @param originalEmail The original problematic email
     * @return A valid placeholder email address
     */
    private static String createFallbackAddress(String originalEmail) {
        // Extract domain if possible, otherwise use a default
        String domain = "example.com";
        int atPos = originalEmail.lastIndexOf('@');
        if (atPos > 0 && atPos < originalEmail.length() - 1) {
            String possibleDomain = originalEmail.substring(atPos + 1);
            // Find the first dot after removing invalid chars
            String cleanedDomain = possibleDomain.replaceAll("[^a-zA-Z0-9.\\-]", "");
            int dotPos = cleanedDomain.indexOf('.');
            if (dotPos > 0) {
                domain = cleanedDomain.substring(0, dotPos) + ".com";
            }
        }
        
        // Generate a hash-based local part
        String hashCode = String.valueOf(Math.abs(originalEmail.hashCode()));
        String localPart = "invalid-email-" + hashCode.substring(0, Math.min(hashCode.length(), 8));
        
        return localPart + "@" + domain;
    }
    
    /**
     * Validates if an email address is properly formatted.
     * 
     * @param emailAddress The email address to validate
     * @return true if the email address is valid, false otherwise
     */
    public static boolean isValidEmailAddress(String emailAddress) {
        if (emailAddress == null || emailAddress.isEmpty()) {
            return false;
        }
        
        try {
            new InternetAddress(emailAddress).validate();
            return true;
        } catch (AddressException e) {
            return false;
        }
    }
}