package com.guidedbyte.openapi.modelgen.services;

import org.slf4j.Logger;

/**
 * A context-aware logger that includes user-configurable MDC context in log messages
 * to work around Gradle's logging limitations.
 * 
 * <p>This logger supports all log levels (debug, info, warn, error) and automatically 
 * includes context information in log messages using a configurable format.
 * Users can customize the format in their build configuration.</p>
 * 
 * <p>Examples:</p>
 * <ul>
 *   <li>Default: <code>[spring:pojo.mustache] message</code></li>
 *   <li>Minimal: <code>[spring] message</code></li>
 *   <li>Debug: <code>[CustomizationEngine|spring:pojo.mustache] message</code></li>
 *   <li>Custom: <code>spec=spring template=pojo.mustache message</code></li>
 * </ul>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class ContextAwareLogger {
    
    /**
     * Logs a debug message with automatic MDC context inclusion when debug is enabled.
     * Uses the default context formatter.
     * 
     * @param logger the SLF4J logger to use
     * @param debugEnabled whether debug mode is enabled (from TemplateConfiguration)
     * @param message the message template
     * @param args the message arguments
     */
    public static void debug(Logger logger, boolean debugEnabled, String message, Object... args) {
        debug(logger, debugEnabled, null, message, args);
    }
    
    /**
     * Logs a debug message with configurable MDC context format.
     * 
     * @param logger the SLF4J logger to use
     * @param debugEnabled whether debug mode is enabled
     * @param contextFormat the context format to use (null for default)
     * @param message the message template
     * @param args the message arguments
     */
    public static void debug(Logger logger, boolean debugEnabled, String contextFormat, String message, Object... args) {
        if (debugEnabled) {
            String contextualMessage = enrichMessageWithContext(message, contextFormat);
            logger.info(contextualMessage, args); // Use INFO so it shows in Gradle output
        } else {
            logger.debug(message, args); // Normal debug logging
        }
    }
    
    /**
     * Logs an info message with automatic MDC context inclusion when debug is enabled.
     * 
     * @param logger the SLF4J logger to use  
     * @param debugEnabled whether debug mode is enabled
     * @param message the message template
     * @param args the message arguments
     */
    public static void info(Logger logger, boolean debugEnabled, String message, Object... args) {
        info(logger, debugEnabled, null, message, args);
    }
    
    /**
     * Logs an info message with configurable MDC context format.
     * 
     * @param logger the SLF4J logger to use
     * @param debugEnabled whether debug mode is enabled
     * @param contextFormat the context format to use (null for default)
     * @param message the message template
     * @param args the message arguments
     */
    public static void info(Logger logger, boolean debugEnabled, String contextFormat, String message, Object... args) {
        String contextualMessage = debugEnabled ? enrichMessageWithContext(message, contextFormat) : message;
        logger.info(contextualMessage, args);
    }
    
    /**
     * Logs a warning with context information always included.
     * 
     * @param logger the SLF4J logger to use
     * @param message the message template  
     * @param args the message arguments
     */
    public static void warn(Logger logger, String message, Object... args) {
        warn(logger, null, message, args);
    }
    
    /**
     * Logs a warning with configurable MDC context format.
     * 
     * @param logger the SLF4J logger to use
     * @param contextFormat the context format to use (null for default)
     * @param message the message template
     * @param args the message arguments
     */
    public static void warn(Logger logger, String contextFormat, String message, Object... args) {
        String contextualMessage = enrichMessageWithContext(message, contextFormat);
        logger.warn(contextualMessage, args);
    }
    
    /**
     * Logs an error with context information always included.
     * 
     * @param logger the SLF4J logger to use
     * @param message the message template
     * @param args the message arguments
     */
    public static void error(Logger logger, String message, Object... args) {
        error(logger, null, message, args);
    }
    
    /**
     * Logs an error with configurable MDC context format.
     * 
     * @param logger the SLF4J logger to use
     * @param contextFormat the context format to use (null for default)
     * @param message the message template
     * @param args the message arguments
     */
    public static void error(Logger logger, String contextFormat, String message, Object... args) {
        String contextualMessage = enrichMessageWithContext(message, contextFormat);
        logger.error(contextualMessage, args);
    }
    
    /**
     * Enriches a log message with configurable MDC context information.
     * 
     * @param message the original message
     * @param contextFormat the context format to use (null for default)
     * @return the message with context prefix
     */
    private static String enrichMessageWithContext(String message, String contextFormat) {
        LoggingContextFormatter formatter = LoggingContextFormatter.fromUserConfig(contextFormat);
        String context = formatter.formatCurrentContext();
        
        if (context.isEmpty()) {
            return message;
        }
        
        return context + " " + message;
    }
    
    /**
     * Enriches a log message with default MDC context information.
     * 
     * @param message the original message
     * @return the message with context prefix
     */
    private static String enrichMessageWithContext(String message) {
        return enrichMessageWithContext(message, null);
    }
}