package com.intenovation.invoice;

/**
 * Utility methods for parsing and formatting numbers in invoices.
 */
public class NumberUtils {
    /**
     * Parse an amount string that could be in either German or American format
     * German format: 1.234,56 (. for thousands, , for decimal)
     * American format: 1,234.56 (, for thousands, . for decimal)
     *
     * @param amountStr The amount string to parse
     * @return The parsed amount as a double
     * @throws NumberFormatException If the string cannot be parsed as a number
     */
    public static double parseAmount(String amountStr) {
        // Remove any currency symbols and whitespace
        amountStr = amountStr.replaceAll("[$€£\\s]", "");
        
        // Handle trailing decimal point
        if (amountStr.endsWith(".")) {
            amountStr = amountStr.substring(0, amountStr.length() - 1);
        }
        
        // If empty after cleaning, return 0
        if (amountStr.isEmpty()) {
            return 0.0;
        }

        // Check if this is likely a German format number
        boolean isGermanFormat = false;

        // If it has a comma but no period, it's German format
        if (amountStr.contains(",") && !amountStr.contains(".")) {
            isGermanFormat = true;
        }
        // If it has both comma and period, look at the positions
        else if (amountStr.contains(",") && amountStr.contains(".")) {
            int lastCommaPos = amountStr.lastIndexOf(",");
            int lastPeriodPos = amountStr.lastIndexOf(".");

            // If the last separator is a comma, it's likely German format
            isGermanFormat = lastCommaPos > lastPeriodPos;
        }

        // Convert to a parseable format
        if (isGermanFormat) {
            // German format: Remove all periods and replace comma with period
            amountStr = amountStr.replace(".", "").replace(",", ".");
        } else {
            // American format: Remove all commas
            amountStr = amountStr.replace(",", "");
        }

        // Parse the formatted string
        return Double.parseDouble(amountStr);
    }
    
    /**
     * Format a monetary amount with 2 decimal places
     *
     * @param amount The amount to format
     * @return A formatted string representation of the amount
     */
    public static String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }
}