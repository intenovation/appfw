package com.intenovation.invoice;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates summary reports and exports for invoice data.
 * Creates CSV files and summary reports with statistics.
 */
public class InvoiceReportGenerator {
    private static final Logger LOGGER = Logger.getLogger(InvoiceReportGenerator.class.getName());
    
    /**
     * Generate reports from the extracted invoice data
     * 
     * @param invoices The list of invoices to generate reports for
     * @param outputDirectory The directory to save reports to
     * @return A status message describing the generated reports
     * @throws IOException If there is an error writing the reports
     */
    public String generateReports(List<Invoice> invoices, File outputDirectory) throws IOException {
        // Create a timestamp for the report files
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String timestamp = sdf.format(new Date());

        // CSV file for all invoices
        File csvFile = new File(outputDirectory, "invoices_" + timestamp + ".tsv");

        try (FileWriter writer = new FileWriter(csvFile)) {
            // Write header
            writer.write(Invoice.header());

            // Write invoice data
            for (Invoice invoice : invoices) {
                writer.write(invoice.toString());
            }
        }

        // Summary report with statistics
        File summaryFile = new File(outputDirectory, "summary_" + timestamp + ".txt");

        try (FileWriter writer = new FileWriter(summaryFile)) {
            writer.write("Invoice Analysis Summary\n");
            writer.write("=======================\n\n");
            writer.write("Generated: " + new Date() + "\n\n");

            writer.write("Total invoices found: " + invoices.size() + "\n\n");

            // Calculate total amount
            double totalAmount = 0;
            for (Invoice invoice : invoices) {
                totalAmount += invoice.getAmount();
            }
            writer.write("Total amount: $" + String.format("%.2f", totalAmount) + "\n\n");

            // Count by type
            Map<Type, Integer> typeCount = new HashMap<>();
            for (Invoice invoice : invoices) {
                Type type = invoice.getType();
                typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
            }

            writer.write("Document types:\n");
            for (Map.Entry<Type, Integer> entry : typeCount.entrySet()) {
                writer.write("  " + entry.getKey() + ": " + entry.getValue() + "\n");
            }
            writer.write("\n");
            
            // Count by city
            Map<String, Integer> cityCount = new HashMap<>();
            for (Invoice invoice : invoices) {
                String city = invoice.getCity();
                if (city != null && !city.isEmpty()) {
                    cityCount.put(city, cityCount.getOrDefault(city, 0) + 1);
                }
            }

            writer.write("Cities:\n");
            for (Map.Entry<String, Integer> entry : cityCount.entrySet()) {
                writer.write("  " + entry.getKey() + ": " + entry.getValue() + "\n");
            }
            writer.write("\n");
            
            // Count by utility
            Map<String, Integer> utilityCount = new HashMap<>();
            Map<String, Double> utilityAmounts = new HashMap<>();
            for (Invoice invoice : invoices) {
                String utility = invoice.getUtility();
                if (utility != null && !utility.isEmpty()) {
                    utilityCount.put(utility, utilityCount.getOrDefault(utility, 0) + 1);
                    utilityAmounts.put(utility, utilityAmounts.getOrDefault(utility, 0.0) + invoice.getAmount());
                }
            }

            writer.write("Utilities:\n");
            for (Map.Entry<String, Integer> entry : utilityCount.entrySet()) {
                String utility = entry.getKey();
                writer.write("  " + utility + ": " + entry.getValue() + " invoices, $" + 
                    String.format("%.2f", utilityAmounts.get(utility)) + "\n");
            }
            writer.write("\n");

            // Top senders
            Map<String, Integer> senderCount = new HashMap<>();
            for (Invoice invoice : invoices) {
                String sender = invoice.getEmail();
                if (sender != null && !sender.isEmpty()) {
                    senderCount.put(sender, senderCount.getOrDefault(sender, 0) + 1);
                }
            }

            writer.write("Top senders:\n");
            senderCount.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(10)
                    .forEach(entry -> {
                        try {
                            writer.write("  " + entry.getKey() + ": " + entry.getValue() + "\n");
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Error writing to summary file", e);
                        }
                    });
            
            // By year statistics
            Map<Integer, Double> yearlyAmount = new HashMap<>();
            for (Invoice invoice : invoices) {
                int year = invoice.getYear();
                if (year > 0) {
                    yearlyAmount.put(year, yearlyAmount.getOrDefault(year, 0.0) + invoice.getAmount());
                }
            }
            
            writer.write("\nYearly totals:\n");
            yearlyAmount.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    try {
                        writer.write("  " + entry.getKey() + ": $" + 
                            String.format("%.2f", entry.getValue()) + "\n");
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Error writing to summary file", e);
                    }
                });
        }

        return "Generated reports with " + invoices.size() + " invoices.\n" +
                "CSV report: " + csvFile.getName() + "\n" +
                "Summary report: " + summaryFile.getName();
    }
}