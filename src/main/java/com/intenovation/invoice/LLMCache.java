package com.intenovation.invoice;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A cache for LLM responses to avoid redundant API calls.
 * Stores responses in the file system using a hash of the prompt as the filename.
 */
public class LLMCache {
    private static final Logger LOGGER = Logger.getLogger(LLMCache.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds between retries
    
    private final Path cacheDirectory;
    private final String ollamaHost;
    private final String ollamaModel;
    private final int ollamaMaxTokens;
    private final int ollamaTimeoutSeconds;
    
    /**
     * Create a new LLMCache with the provided configuration
     * 
     * @param config The invoice configuration containing Ollama settings
     */
    public LLMCache(InvoiceConfiguration config) {
        this.ollamaHost = config.getOllamaHost();
        this.ollamaModel = config.getOllamaModel();
        this.ollamaMaxTokens = config.getOllamaMaxTokens();
        this.ollamaTimeoutSeconds = config.getOllamaTimeoutSeconds();
        
        // Create cache directory in the output directory
        this.cacheDirectory = Paths.get(config.getOutputDirectory().getAbsolutePath(), "llm-cache");
        
        try {
            Files.createDirectories(cacheDirectory);
            LOGGER.info("LLM cache directory created at: " + cacheDirectory);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create cache directory", e);
        }
    }
    
    /**
     * Get a response from the LLM, using the cache if available
     * 
     * @param prompt The prompt to send to the LLM
     * @return The LLM response or null if an error occurs
     */
    public String getResponse(String prompt) {
        // Calculate hash of the prompt to use as filename
        String hash = calculateSHA256(prompt);
        if (hash == null) {
            LOGGER.severe("Failed to calculate hash for prompt");
            return null;
        }
        
        // Create file path for the cached response
        Path responsePath = cacheDirectory.resolve(hash + ".txt");
        
        // Check if response is already cached
        if (Files.exists(responsePath)) {
            LOGGER.info("Cache hit for prompt hash: " + hash);
            try {
                // Read response from cache
                String cachedResponse = Files.readString(responsePath, StandardCharsets.UTF_8);
                return cachedResponse;
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read cached response", e);
                // Fall through to getting a fresh response
            }
        }
        
        // Cache miss, get fresh response from Ollama
        LOGGER.info("Cache miss for prompt hash: " + hash + ", calling Ollama API");
        String response = callOllamaAPI(prompt);
        
        // Cache the response if we got one
        if (response != null) {
            try {
                Files.writeString(responsePath, response, StandardCharsets.UTF_8);
                LOGGER.info("Cached response for prompt hash: " + hash);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to cache response", e);
                // Continue even if caching fails
            }
        }
        
        return response;
    }
    
    /**
     * Call the Ollama API without directly depending on the Ollama4j library.
     * This method delegates to an instance of OllamaInvokeService.
     * 
     * @param prompt The prompt to send to Ollama
     * @return The response from Ollama or null if an error occurs
     */
    private String callOllamaAPI(String prompt) {
        // Create an invoker that will handle the actual Ollama API call
        OllamaInvokeService invoker = new OllamaInvokeService(
                ollamaHost, 
                ollamaModel, 
                ollamaMaxTokens, 
                ollamaTimeoutSeconds
        );
        
        // Try multiple times in case of temporary failures
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                LOGGER.info("Calling Ollama API (attempt " + attempt + " of " + MAX_RETRIES + ")");
                
                String response = invoker.invokeOllama(prompt);
                if (response != null && !response.isEmpty()) {
                    return response;
                }
                
                LOGGER.warning("Empty response from Ollama on attempt " + attempt);
                
                if (attempt < MAX_RETRIES) {
                    LOGGER.info("Waiting " + RETRY_DELAY_MS + "ms before retry");
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Interrupted while waiting to retry", e);
                return null;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error calling Ollama on attempt " + attempt, e);
                
                if (attempt < MAX_RETRIES) {
                    try {
                        LOGGER.info("Waiting " + RETRY_DELAY_MS + "ms before retry");
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        
        LOGGER.severe("All Ollama API call attempts failed");
        return null;
    }
    
    /**
     * Calculate SHA-256 hash of a string
     * 
     * @param input The input string
     * @return Hex string representation of the hash, or null if hash calculation fails
     */
    private String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "SHA-256 algorithm not available", e);
            return null;
        }
    }
    
    /**
     * Clear the cache by deleting all cached responses
     * 
     * @return true if the cache was successfully cleared, false otherwise
     */
    public boolean clearCache() {
        try {
            File[] cacheFiles = cacheDirectory.toFile().listFiles((dir, name) -> name.endsWith(".txt"));
            if (cacheFiles != null) {
                for (File file : cacheFiles) {
                    if (!file.delete()) {
                        LOGGER.warning("Failed to delete cache file: " + file);
                    }
                }
            }
            LOGGER.info("LLM cache cleared");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to clear cache", e);
            return false;
        }
    }
    
    /**
     * Get the size of the cache in bytes
     * 
     * @return The total size of all cached responses in bytes
     */
    public long getCacheSize() {
        try {
            File[] cacheFiles = cacheDirectory.toFile().listFiles((dir, name) -> name.endsWith(".txt"));
            if (cacheFiles != null) {
                long totalSize = 0;
                for (File file : cacheFiles) {
                    totalSize += file.length();
                }
                return totalSize;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to calculate cache size", e);
        }
        return 0;
    }
    
    /**
     * Get the number of cached responses
     * 
     * @return The number of cached responses
     */
    public int getCacheCount() {
        try {
            File[] cacheFiles = cacheDirectory.toFile().listFiles((dir, name) -> name.endsWith(".txt"));
            return cacheFiles != null ? cacheFiles.length : 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to count cache entries", e);
            return 0;
        }
    }
}