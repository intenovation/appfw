package com.intenovation.email.utils;

import javax.mail.internet.InternetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test class demonstrating how to use the EmailAddressSanitizer
 * to prevent javax.mail.internet.AddressException errors.
 */
public class EmailAddressSanitizerTest {
    private static final Logger LOGGER = Logger.getLogger(EmailAddressSanitizerTest.class.getName());
    
    public static void main(String[] args) {
        // Example problematic email addresses
        String[] testEmails = {
            "normal.user@example.com",
            "servicejeeise@spp7011536.myworkday.com☃",
            "user@domain.com..",
            "user@domain#invalid.com",
            "no-at-symbol-example.com",
            "user@.com",
            null,
            ""
        };
        
        // Test sanitization
        for (String email : testEmails) {
            testSanitizeEmail(email);
        }
    }
    
    private static void testSanitizeEmail(String email) {
        System.out.println("-----------------------------------");
        System.out.println("Original: " + email);
        
        // First try direct handling with InternetAddress
        System.out.println("Direct parsing result:");
        try {
            new InternetAddress(email).validate();
            System.out.println("  Valid");
        } catch (Exception e) {
            System.out.println("  Error: " + e.getMessage());
        }
        
        // Now try with sanitizer
        System.out.println("With sanitizer:");
        String sanitized = EmailAddressSanitizer.sanitizeEmailAddress(email);
        System.out.println("  Sanitized: " + sanitized);
        
        // Confirm sanitized address is valid
        if (sanitized != null) {
            try {
                new InternetAddress(sanitized).validate();
                System.out.println("  Valid after sanitization");
            } catch (Exception e) {
                System.out.println("  ERROR: Still invalid after sanitization! " + e.getMessage());
            }
        } else {
            System.out.println("  Sanitizer returned null");
        }
    }
}