package com.intenovation.appfw.systemtray;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ProgressStatusCallback implementation that logs progress updates
 * to a specified logger.
 */
public class LoggingProgressCallback implements ProgressStatusCallback {
    private final Logger logger;
    private final Level logLevel;
    private final String taskName;
    private int lastPercent = -1;
    
    /**
     * Create a new LoggingProgressCallback
     * 
     * @param logger The logger to use
     * @param taskName The name of the task (for log messages)
     */
    public LoggingProgressCallback(Logger logger, String taskName) {
        this(logger, Level.INFO, taskName);
    }
    
    /**
     * Create a new LoggingProgressCallback with custom log level
     * 
     * @param logger The logger to use
     * @param logLevel The log level to use
     * @param taskName The name of the task (for log messages)
     */
    public LoggingProgressCallback(Logger logger, Level logLevel, String taskName) {
        this.logger = logger;
        this.logLevel = logLevel;
        this.taskName = taskName;
    }
    
    @Override
    public void update(int percent, String message) {
        // Only log when percent changes to avoid excessive logging
        if (percent != lastPercent || percent == 0 || percent == 100) {
            logger.log(logLevel, String.format("[%s] %d%% - %s", taskName, percent, message));
            lastPercent = percent;
        }
    }
}