package com.intenovation.invoice;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.OptionsBuilder;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Uses Ollama to extract invoice information from text content.
 */
public class LLMInvoiceParser {
    private static final Logger LOGGER = Logger.getLogger(LLMInvoiceParser.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second

    private final InvoiceConfiguration config;
    private final OllamaAPI ollamaAPI;
    
    /**
     * Create a new LLM invoice parser with Ollama
     * @param config The invoice configuration
     */
    public LLMInvoiceParser(InvoiceConfiguration config) {
        this.config = config;
        this.ollamaAPI = new OllamaAPI(config.getOllamaHost());
    }

    /**
     * Parse content using Ollama to extract invoice information
     * @param content The content to analyze
     * @param baseInvoice The base invoice with common properties
     * @return An invoice with extracted information or null if parsing failed
     */
    public Invoice parseWithLLM(String content, Invoice baseInvoice) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Check if Ollama server is available
                if (!ollamaAPI.ping()) {
                    LOGGER.log(Level.WARNING, "Ollama server is not available");
                    return null;
                }
                
                // Create prompt for Ollama
                String prompt = createPrompt(content);
                
                // Call Ollama API
                LOGGER.log(Level.INFO, "Calling Ollama with model: " + config.getOllamaModel());
                OllamaResult result = ollamaAPI.generate(
                    config.getOllamaModel(),
                    prompt,
                    false,
                    new OptionsBuilder()
                        .setTemperature(0.1f)  // Low temperature for more deterministic outputs
                        .setNumPredict(config.getOllamaMaxTokens())
                        .build()
                );
                
                if (result != null && result.getResponse() != null && !result.getResponse().isEmpty()) {
                    return parseJsonResponse(result.getResponse(), baseInvoice);
                }
                
                LOGGER.log(Level.WARNING, "Empty Ollama response on attempt " + attempt);
                
                if (attempt < MAX_RETRIES) {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Ollama parsing interrupted", e);
                return null;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in Ollama parsing on attempt " + attempt + ": " + e.getMessage(), e);
                
                if (attempt < MAX_RETRIES) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        
        LOGGER.log(Level.WARNING, "All Ollama parsing attempts failed");
        return null;
    }
    
    /**
     * Create the prompt for Ollama
     * @param content The content to analyze
     * @return The prompt to send to Ollama
     */
    private String createPrompt(String content) {
        return "You are an invoice analysis system. Please analyze the following text which is content from an email or document that might contain invoice information.\n\n" +
            "Extract the following details, formatted as a valid JSON object with no additional text before or after:\n\n" +
            "{\n" +
            "  \"type\": \"ONE OF: [Invoice, Receipt, Statement, Letter, Donation, Reminder, Estimate]\",\n" +
            "  \"amount\": 0.0,\n" +
            "  \"number\": \"\",\n" +
            "  \"account\": \"\",\n" +
            "  \"date\": \"\",\n" +
            "  \"dueDate\": \"\",\n" +
            "  \"city\": \"\",\n" +
            "  \"utility\": \"\",\n" +
            "  \"year\": 0,\n" +
            "  \"month\": 0,\n" +
            "  \"day\": 0\n" +
            "}\n\n" +
            "Rules:\n" +
            "1. For amount, extract the total amount due/paid, not subtotals or line items.\n" +
            "2. For type, infer from context - is this an invoice requesting payment, a receipt of payment, a statement, etc.\n" +
            "3. For number, look for invoice numbers, receipt numbers, order numbers, etc.\n" +
            "4. For account, look for account numbers, customer IDs, membership numbers, etc.\n" +
            "5. For date, extract the invoice date or issue date (not the due date).\n" +
            "6. For dueDate, extract the payment due date if present.\n" +
            "7. Year, month, and day should be extracted from the date if possible.\n" +
            "8. For city and utility, extract any information about location or service provider.\n\n" +
            "Only include fields in the response if you have high confidence. If a field cannot be determined, leave it as the default value shown above.\n" +
            "Return ONLY the JSON with no explanations or other text.\n\n" +
            "Here is the content to analyze:\n\n" +
            content;
    }
    
    /**
     * Parse the JSON response from Ollama into an Invoice object
     * @param jsonResponse The JSON response
     * @param baseInvoice The base invoice with common properties
     * @return An invoice with the extracted information
     */
    private Invoice parseJsonResponse(String jsonResponse, Invoice baseInvoice) {
        try {
            // Extract just the JSON part of the response
            int startBrace = jsonResponse.indexOf('{');
            int endBrace = jsonResponse.lastIndexOf('}');
            
            if (startBrace >= 0 && endBrace >= 0) {
                jsonResponse = jsonResponse.substring(startBrace, endBrace + 1);
            }
            
            LOGGER.log(Level.FINE, "Parsing JSON response: " + jsonResponse);
            JSONObject jsonObject = new JSONObject(jsonResponse);
            
            // Create a new invoice based on the base invoice
            Invoice invoice = new Invoice();
            invoice.setEmailId(baseInvoice.getEmailId());
            invoice.setSubject(baseInvoice.getSubject());
            invoice.setEmail(baseInvoice.getEmail());
            invoice.setFileName(baseInvoice.getFileName());
            invoice.setYear(baseInvoice.getYear());
            invoice.setMonth(baseInvoice.getMonth());
            invoice.setDay(baseInvoice.getDay());
            invoice.setDate(baseInvoice.getDate());
            invoice.setParse("ollama");
            
            // Update with Ollama-extracted information
            if (jsonObject.has("type") && !jsonObject.getString("type").isEmpty()) {
                try {
                    invoice.setType(Type.valueOf(jsonObject.getString("type")));
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.FINE, "Invalid type value: " + jsonObject.getString("type"));
                    invoice.setType(baseInvoice.getType());
                }
            }
            
            if (jsonObject.has("amount") && jsonObject.get("amount") instanceof Number) {
                invoice.setAmount(jsonObject.getDouble("amount"));
            }
            
            if (jsonObject.has("number") && !jsonObject.getString("number").isEmpty()) {
                invoice.setNumber(jsonObject.getString("number"));
            }
            
            if (jsonObject.has("account") && !jsonObject.getString("account").isEmpty()) {
                invoice.setAccount(jsonObject.getString("account"));
            }
            
            if (jsonObject.has("date") && !jsonObject.getString("date").isEmpty()) {
                invoice.setDate(jsonObject.getString("date"));
            }
            
            if (jsonObject.has("dueDate") && !jsonObject.getString("dueDate").isEmpty()) {
                invoice.setDueDate(jsonObject.getString("dueDate"));
            }
            
            if (jsonObject.has("city") && !jsonObject.getString("city").isEmpty()) {
                invoice.setCity(jsonObject.getString("city"));
            }
            
            if (jsonObject.has("utility") && !jsonObject.getString("utility").isEmpty()) {
                invoice.setUtility(jsonObject.getString("utility"));
            }
            
            if (jsonObject.has("year") && jsonObject.get("year") instanceof Number) {
                invoice.setYear(jsonObject.getInt("year"));
            }
            
            if (jsonObject.has("month") && jsonObject.get("month") instanceof Number) {
                invoice.setMonth(jsonObject.getInt("month"));
            }
            
            if (jsonObject.has("day") && jsonObject.get("day") instanceof Number) {
                invoice.setDay(jsonObject.getInt("day"));
            }
            
            return invoice;
        } catch (JSONException e) {
            LOGGER.log(Level.WARNING, "Error parsing Ollama response as JSON: " + e.getMessage() + "\nResponse: " + jsonResponse, e);
            return null;
        }
    }
}