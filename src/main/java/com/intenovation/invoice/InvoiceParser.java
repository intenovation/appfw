package com.intenovation.invoice;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specialized class for parsing email messages to extract invoice information.
 * Handles different content types (text, HTML, PDF) and performs pattern matching.
 */
public class InvoiceParser {
    private static final Logger LOGGER = Logger.getLogger(InvoiceParser.class.getName());

    // Regular expressions for extracting invoice information
    private static final Pattern[] AMOUNT_PATTERNS = {
            // German style
            Pattern.compile("(?i)(Gesamtbetrag\\s*\\(brutto\\)|Gesamtsumme|Rechnungsbetrag in Höhe von|Rechnungsbetrag:|Rechnungsbetrag von|Zu zahlender Betrag:|Endbetrag EUR|Brutto)\\s*[€]?\\s*([\\d.,]+)"),
            // English style
            Pattern.compile("(?i)(Total Charges:|Total Amount Due:|Total Balance Due:|a payment of|Total due now:|Total Due|Statement Amount:|Amount due:|Amount Due:|Amount:|The amount of|Invoice amount|invoice total is|Total\\$|Original Charge|Total Charged|Payment amount:|BALANCE DUE|Current charges:|Total \\(USD\\):|Total \\$|TOTAL \\$|Amount Received|Amount due on this invoice:|Total|Statement balance:|Statement balance|Amount Charged \\$|PAY THIS AMOUNT:|outstanding balance of|TOTAL|Amount paid|DETAILS)\\s*[\\$£]?\\s*([\\d.,]+)"),
            // General pattern
            Pattern.compile("(?i)(total|amount|sum|betrag|summe)[\\s:]*[$€£]?\\s*([\\d,.]+)")
    };

    private static final Pattern[] INVOICE_NUMBER_PATTERNS = {
            Pattern.compile("(?i)(Your invoice|Invoice #:|Order Number #|Rechnungsnummer:|Billing Period:|Plan period|aktuelle Rechnung|Invoice no.|Invoice Number:|INVOICE|Bill Period\\s*:|Receipt #|Ihre Rechnung|Order Number|Order #:)\\s*([A-Za-z0-9-]{3,20})"),
            Pattern.compile("(?i)(invoice|rechnung|bill)[\\s:#-]*([A-Z0-9]{4,20})")
    };

    private static final Pattern[] ACCOUNT_NUMBER_PATTERNS = {
            Pattern.compile("(?i)(Kundennummer:|Kundennummer|Kunden Nr|Account Number: Ending in|account number:|Account Number:|Account:|Service Address:|Account number:|Your order from|ordered from|Beleg für|CF Number:|for account number \\*{6}|Receipt for Your Payment to|for account :|Customer #:|account ending in \\*{6}|Policy Number)\\s*([A-Za-z0-9-]{3,30})"),
            Pattern.compile("(?i)(account|konto|customer)[\\s:#-]*([A-Z0-9]{4,20})")
    };

    private static final Pattern[] DATE_PATTERNS = {
            Pattern.compile("(?i)(Billing Date:|Date:|Rechnungsdatum:|Payment date:|was placed on|Statement date:)\\s*([0-9]{1,2}[\\s./\\-][0-9]{1,2}[\\s./\\-][0-9]{2,4})"),
            Pattern.compile("(?i)(date|datum)[\\s:]*([0-9]{1,2}[\\s./\\-][0-9]{1,2}[\\s./\\-][0-9]{2,4})")
    };

    private static final Pattern[] DUE_DATE_PATTERNS = {
            Pattern.compile("(?i)(Due Date:|DUE DATE|is due on|Total Current Charges Due On|Payment Due Date|Payment due date:|TRANSACTION DATE|Auto Pay Date:|Received:|delivered on|DUE|AUTO DRAFT DATE:|APS amount to be applied on|will be charged on)\\s*([0-9]{1,2}[\\s./\\-][0-9]{1,2}[\\s./\\-][0-9]{2,4})"),
            Pattern.compile("(?i)(due date|fällig|zahlbar bis)[\\s:]*([0-9]{1,2}[\\s./\\-][0-9]{1,2}[\\s./\\-][0-9]{2,4})")
    };

    private final LLMInvoiceParser llmParser;
    private final InvoiceConfiguration config;

    /**
     * Create a new invoice parser
     * @param config The invoice configuration
     */
    public InvoiceParser(InvoiceConfiguration config) {
        this.config = config;
        this.llmParser = new LLMInvoiceParser(config);
    }

    /**
     * Parse a message to extract all possible invoice information
     * This handles different content types and attachment formats.
     */
    public List<Invoice> parseMessage(Message message, Invoice baseInvoice) {
        List<Invoice> results = new ArrayList<>();

        try {
            // Process message content
            Object content = message.getContent();

            if (content instanceof String) {
                // Process plain text content
                Invoice textInvoice = cloneInvoice(baseInvoice);
                textInvoice.setParse("text");
                if (processTextContent(textInvoice, (String) content)) {
                    results.add(textInvoice);
                }
            } else if (content instanceof Multipart) {
                // Process multipart content
                Multipart multipart = (Multipart) content;
                results.addAll(processMultipartContent(baseInvoice, multipart));
            } else if (content instanceof InputStream) {
                // Process input stream content
                Invoice streamInvoice = cloneInvoice(baseInvoice);
                streamInvoice.setParse("stream");
                InputStream is = (InputStream) content;
                String streamContent = streamToString(is);
                if (processTextContent(streamInvoice, streamContent)) {
                    results.add(streamInvoice);
                }
            }

            // Post-process the results for better type detection and property mapping
            for (Invoice invoice : results) {
                // If type is still Letter, try to determine a better type
                if (invoice.getType() == Type.Letter) {
                    invoice.setType(Type.detectType(invoice.getSubject()));
                }

                // If still Letter and we have an amount, it's likely an Invoice
                if (invoice.getType() == Type.Letter && invoice.getAmount() > 0) {
                    invoice.setType(Type.Invoice);
                }

                // Detect city and property based on content
                detectCityAndProperty(invoice);

                // Detect utility type
                detectUtilityType(invoice);
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing message: " + e.getMessage(), e);
        }

        return results;
    }

    /**
     * Process text content to extract invoice details
     */
    private boolean processTextContent(Invoice invoice, String content) {
        // Check if content is HTML and extract text if needed
        String textContent = content;
        if (isHtmlContent(textContent)) {
            textContent = Jsoup.parse(textContent).text();
            LOGGER.info("Extracted text from HTML content for processing");
            invoice.setParse(invoice.getParse() + "+html_extracted");
        }

        // Detect document type
        invoice.setType(Type.detectType(textContent));

        // Try rule-based extraction first
        boolean success = extractInvoiceDetails(invoice, textContent);

        // If rule-based extraction failed and Ollama fallback is enabled, try Ollama parsing
        if (!success && config.isUseOllamaFallback()) {
            LOGGER.info("Rule-based parsing failed, attempting Ollama parsing");
            Invoice llmInvoice = llmParser.parseWithLLM(textContent, invoice);

            if (llmInvoice != null) {
                // Copy relevant fields from Ollama-parsed invoice
                copyInvoiceFields(llmInvoice, invoice);
                success = true;
                LOGGER.info("Ollama parsing successful");
            } else {
                LOGGER.warning("Ollama parsing failed");
            }
        }

        return success;
    }

    /**
     * Check if the content is likely HTML
     * @param content The content to check
     * @return True if the content appears to be HTML
     */
    private boolean isHtmlContent(String content) {
        if (content == null) return false;

        String trimmedContent = content.trim().toLowerCase();
        return trimmedContent.startsWith("<!doctype html") ||
                trimmedContent.startsWith("<html") ||
                (trimmedContent.contains("<body") && trimmedContent.contains("</body>")) ||
                (trimmedContent.contains("<div") && trimmedContent.contains("</div>"));
    }

    /**
     * Copy relevant fields from source invoice to destination invoice
     */
    private void copyInvoiceFields(Invoice source, Invoice destination) {
        if (source.getType() != null && source.getType() != Type.Letter) {
            destination.setType(source.getType());
        }

        if (source.getAmount() > 0) {
            destination.setAmount(source.getAmount());
        }

        if (source.getNumber() != null && !source.getNumber().isEmpty()) {
            destination.setNumber(source.getNumber());
        }

        if (source.getAccount() != null && !source.getAccount().isEmpty()) {
            destination.setAccount(source.getAccount());
        }

        if (source.getDate() != null && !source.getDate().isEmpty()) {
            destination.setDate(source.getDate());
        }

        if (source.getDueDate() != null && !source.getDueDate().isEmpty()) {
            destination.setDueDate(source.getDueDate());
        }

        if (source.getCity() != null && !source.getCity().isEmpty()) {
            destination.setCity(source.getCity());
        }

        if (source.getUtility() != null && !source.getUtility().isEmpty()) {
            destination.setUtility(source.getUtility());
        }

        if (source.getYear() > 0) {
            destination.setYear(source.getYear());
        }

        if (source.getMonth() > 0) {
            destination.setMonth(source.getMonth());
        }

        if (source.getDay() > 0) {
            destination.setDay(source.getDay());
        }

        // Append parse method
        destination.setParse(destination.getParse() + "+ollama");
    }

    /**
     * Process multipart content to extract invoice details from each part
     */
    private List<Invoice> processMultipartContent(Invoice baseInvoice, Multipart multipart)
            throws MessagingException, IOException {
        List<Invoice> results = new ArrayList<>();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String contentType = bodyPart.getContentType().toLowerCase();

            if (contentType.contains("text/plain")) {
                // Process plain text part
                Invoice textInvoice = cloneInvoice(baseInvoice);
                textInvoice.setParse("text_part");
                String textContent = bodyPart.getContent().toString();
                if (processTextContent(textInvoice, textContent)) {
                    results.add(textInvoice);
                }
            } else if (contentType.contains("text/html")) {
                // Process HTML part - extract text from HTML for better processing
                Invoice htmlInvoice = cloneInvoice(baseInvoice);
                htmlInvoice.setParse("html_part");
                String htmlContent = bodyPart.getContent().toString();
                // Extract text from HTML
                String textFromHtml = Jsoup.parse(htmlContent).text();
                if (processTextContent(htmlInvoice, textFromHtml)) {
                    results.add(htmlInvoice);
                }
            } else if (contentType.contains("multipart")) {
                // Recursive call for nested multiparts
                Multipart nestedMultipart = (Multipart) bodyPart.getContent();
                results.addAll(processMultipartContent(baseInvoice, nestedMultipart));
            } else if (contentType.contains("application/pdf") ||
                    (bodyPart.getDisposition() != null &&
                            bodyPart.getDisposition().equalsIgnoreCase(Part.ATTACHMENT) &&
                            bodyPart.getFileName() != null &&
                            bodyPart.getFileName().toLowerCase().endsWith(".pdf"))) {
                // Process PDF attachment
                Invoice pdfInvoice = cloneInvoice(baseInvoice);
                pdfInvoice.setParse("pdf_attachment");
                pdfInvoice.setFileName(bodyPart.getFileName());

                // Extract text from PDF
                try (InputStream is = bodyPart.getInputStream()) {
                    String pdfText = extractTextFromPdf(is);
                    if (pdfText != null && !pdfText.isEmpty() && processTextContent(pdfInvoice, pdfText)) {
                        results.add(pdfInvoice);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error extracting text from PDF: " + e.getMessage(), e);
                }
            }
        }

        return results;
    }

    /**
     * Clone an invoice object
     */
    private Invoice cloneInvoice(Invoice original) {
        Invoice copy = new Invoice();
        copy.setEmailId(original.getEmailId());
        copy.setCity(original.getCity());
        copy.setUtility(original.getUtility());
        copy.setType(original.getType());
        copy.setEmail(original.getEmail());
        copy.setAmount(original.getAmount());
        copy.setFileName(original.getFileName());
        copy.setSubject(original.getSubject());
        copy.setYear(original.getYear());
        copy.setMonth(original.getMonth());
        copy.setDay(original.getDay());
        copy.setDate(original.getDate());
        copy.setDueDate(original.getDueDate());
        copy.setNumber(original.getNumber());
        copy.setAccount(original.getAccount());
        copy.setParse(original.getParse());
        return copy;
    }

    /**
     * Detect city and property based on content
     */
    private void detectCityAndProperty(Invoice invoice) {
        // Try to detect city from subject, domain, and account
        String contentToCheck = (invoice.getSubject() + " " +
                invoice.getUtility() + " " +
                invoice.getAccount() + " " +
                invoice.getNumber()).toLowerCase();

        // Check for city identification
        City detectedCity = City.detectCity(contentToCheck);
        if (detectedCity != City.Unknown) {
            invoice.setCity(detectedCity.name());
        }

        // Try to detect property (for potential future use)
        Property.detectProperty(contentToCheck);
    }

    /**
     * Detect utility type based on content
     */
    private void detectUtilityType(Invoice invoice) {
        // Only detect if the utility is currently just a domain or empty
        if (invoice.getUtility() == null || invoice.getUtility().contains(".")) {
            String contentToCheck = (invoice.getSubject() + " " +
                    invoice.getEmail() + " " +
                    invoice.getAccount() + " " +
                    invoice.getNumber()).toLowerCase();

            Utility detectedUtility = Utility.detectUtility(contentToCheck);
            if (detectedUtility != Utility.Unknown) {
                invoice.setUtility(detectedUtility.name());
            }
        }
    }

    /**
     * Extract text from a PDF file
     */
    private String extractTextFromPdf(InputStream pdfStream) {
        try (PDDocument document = PDDocument.load(pdfStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error extracting text from PDF: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Convert an input stream to a string
     */
    private String streamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Extract invoice details from text content using multiple patterns
     */
    private boolean extractInvoiceDetails(Invoice invoice, String content) {
        boolean foundSomething = false;

        // Extract amount using multiple patterns
        for (Pattern pattern : AMOUNT_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                try {
                    String amountStr = matcher.group(2);
                    double amount = NumberUtils.parseAmount(amountStr);
                    if (amount > 0 && amount < 100000) { // Sanity check
                        invoice.setAmount(amount);
                        foundSomething = true;
                        break;
                    }
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.FINE, "Failed to parse amount: " + matcher.group(2), e);
                }
            }
        }

        // Extract invoice number using multiple patterns
        for (Pattern pattern : INVOICE_NUMBER_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String number = matcher.group(2);
                if (number != null && !number.isEmpty()) {
                    invoice.setNumber(cleanInvoiceNumber(number));
                    foundSomething = true;
                    break;
                }
            }
        }

        // Extract account number using multiple patterns
        for (Pattern pattern : ACCOUNT_NUMBER_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String account = matcher.group(2);
                if (account != null && !account.isEmpty()) {
                    invoice.setAccount(account);
                    foundSomething = true;
                    break;
                }
            }
        }

        // Extract date using multiple patterns
        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String date = matcher.group(2);
                if (date != null && !date.isEmpty()) {
                    invoice.setDate(date);
                    foundSomething = true;
                    break;
                }
            }
        }

        // Extract due date using multiple patterns
        for (Pattern pattern : DUE_DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String dueDate = matcher.group(2);
                if (dueDate != null && !dueDate.isEmpty()) {
                    invoice.setDueDate(dueDate);
                    foundSomething = true;
                    break;
                }
            }
        }

        return foundSomething;
    }

    /**
     * Clean up invoice number by removing common non-alphanumeric characters
     */
    private String cleanInvoiceNumber(String number) {
        return number.replaceAll("[\\s:,;]", "").trim();
    }
}