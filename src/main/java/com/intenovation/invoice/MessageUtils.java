package com.intenovation.invoice;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for working with email messages.
 */
public class MessageUtils {
    private static final Logger LOGGER = Logger.getLogger(MessageUtils.class.getName());
    
    /**
     * Get the Message-ID header from an email message
     *
     * @param message The email message
     * @return The Message-ID or null if not found
     */
    public static String getMessageId(Message message) {
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
     * Create a hash-based message ID when the Message-ID header is not available
     *
     * @param message The email message
     * @return A hash-based message ID
     * @throws MessagingException If there is an error accessing message properties
     */
    public static String createMessageHash(Message message) throws MessagingException {
        StringBuilder sb = new StringBuilder();
        
        if (message.getSubject() != null) {
            sb.append(message.getSubject());
        }
        
        if (message.getSentDate() != null) {
            sb.append(message.getSentDate().getTime());
        }
        
        if (message.getFrom() != null && message.getFrom().length > 0) {
            sb.append(message.getFrom()[0].toString());
        }
        
        return "hash-" + Math.abs(sb.toString().hashCode());
    }
    
    /**
     * Extract domain from email address
     *
     * @param email The email address
     * @return The domain part of the email address, or null if not found
     */
    public static String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }

        String[] parts = email.split("@");
        if (parts.length == 2) {
            return parts[1];
        }

        return null;
    }
}