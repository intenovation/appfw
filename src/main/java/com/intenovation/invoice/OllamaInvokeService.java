package com.intenovation.invoice;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.OptionsBuilder;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service that encapsulates the Ollama4j API calls.
 * This isolates the direct dependency on Ollama4j to a single class.
 */
public class OllamaInvokeService {
    private static final Logger LOGGER = Logger.getLogger(OllamaInvokeService.class.getName());
    
    private final String host;
    private final String model;
    private final int maxTokens;
    private final int timeoutSeconds;
    
    /**
     * Create a new OllamaInvokeService
     * 
     * @param host The Ollama host URL
     * @param model The Ollama model to use
     * @param maxTokens Maximum number of tokens to generate
     * @param timeoutSeconds Timeout in seconds for API calls
     */
    public OllamaInvokeService(String host, String model, int maxTokens, int timeoutSeconds) {
        this.host = host;
        this.model = model;
        this.maxTokens = maxTokens;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    /**
     * Invoke the Ollama API with a prompt
     * 
     * @param prompt The prompt to send to Ollama
     * @return The response from Ollama or null if an error occurs
     */
    public String invokeOllama(String prompt) {
        try {
            // Create Ollama API client
            OllamaAPI ollamaAPI = new OllamaAPI(host);
            ollamaAPI.setRequestTimeoutSeconds(timeoutSeconds);
            
            // Check if Ollama server is available
            if (!ollamaAPI.ping()) {
                LOGGER.log(Level.WARNING, "Ollama server is not available at " + host);
                return null;
            }
            
            // Create options for the API call
            OptionsBuilder options = new OptionsBuilder()
                    .setTemperature(0.0f)  // Low temperature for more deterministic outputs
                    .setNumPredict(maxTokens);
            
            // Call Ollama API
            OllamaResult result = ollamaAPI.generate(
                    model,
                    prompt,
                    false,  // Don't stream the response
                    options.build()
            );
            
            // Return the response if available
            if (result != null) {
                return result.getResponse();
            }
            
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error invoking Ollama API", e);
            return null;
        }
    }
}