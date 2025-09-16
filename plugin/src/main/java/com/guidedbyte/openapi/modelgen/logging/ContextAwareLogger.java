package com.guidedbyte.openapi.modelgen.logging;

import org.slf4j.Logger;

/**
 * A context-aware logger that uses SLF4J-compatible patterns for formatting.
 * Works around Gradle's logging limitations by providing enhanced console output.
 * 
 * <p>This logger supports all log levels (debug, info, warn, error) and automatically 
 * formats context using SLF4J patterns with compiled pattern caching.</p>
 * 
 * <p>Pattern Examples:</p>
 * <ul>
 *   <li>Default: <code>"[%X{spec}:%X{template}]"</code> → <code>[spring:pojo.mustache] message</code></li>
 *   <li>Minimal: <code>"[%X{spec}]"</code> → <code>[spring] message</code></li>
 *   <li>Debug: <code>"[%X{component}|%X{spec}:%X{template}]"</code> → <code>[Engine|spring:pojo.mustache] message</code></li>
 *   <li>Timestamped: <code>"%d{HH:mm:ss} [%X{spec}:%X{template}] %msg"</code> → <code>14:30:45 [spring:pojo.mustache] message</code></li>
 * </ul>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class ContextAwareLogger {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ContextAwareLogger() {
        // Utility class - prevent instantiation
    }

    /**
     * Logs a debug message with automatic SLF4J pattern formatting when debug is enabled.
     * Uses the default pattern.
     * 
     * @param logger the SLF4J logger to use
     * @param debugEnabled whether debug mode is enabled (from TemplateConfiguration)
     * @param message the message template
     * @param args the message arguments
     */
    public static void debug(Logger logger, boolean debugEnabled, String message, Object... args) {
        debug(logger, debugEnabled, "plugin", "default", message, args);
    }
    
    /**
     * Logs a debug message with configurable SLF4J pattern formatting.
     * 
     * @param logger the SLF4J logger to use
     * @param debugEnabled whether debug mode is enabled
     * @param patternSrc the pattern source ("plugin" or "native")
     * @param pattern the SLF4J pattern string or predefined name
     * @param message the message template
     * @param args the message arguments
     */
    public static void debug(Logger logger, boolean debugEnabled, String patternSrc, String pattern, String message, Object... args) {
        SLF4JPatternFormatter.log(patternSrc, pattern, logger, debugEnabled, message, args);
    }
    
    /**
     * Logs an info message with automatic SLF4J pattern formatting when debug is enabled.
     * Uses the default pattern.
     * 
     * @param logger the SLF4J logger to use  
     * @param debugEnabled whether debug mode is enabled
     * @param message the message template
     * @param args the message arguments
     */
    public static void info(Logger logger, boolean debugEnabled, String message, Object... args) {
        info(logger, debugEnabled, "plugin", "default", message, args);
    }
    
    /**
     * Logs an info message with configurable SLF4J pattern formatting.
     * 
     * @param logger the SLF4J logger to use
     * @param debugEnabled whether debug mode is enabled
     * @param patternSrc the pattern source ("plugin" or "native")
     * @param pattern the SLF4J pattern string or predefined name
     * @param message the message template
     * @param args the message arguments
     */
    public static void info(Logger logger, boolean debugEnabled, String patternSrc, String pattern, String message, Object... args) {
        if (debugEnabled) {
            // Use plugin's SLF4J formatter for enhanced output
            String formattedMessage = SLF4JPatternFormatter.format(pattern, message, args);
            logger.info(formattedMessage);
        } else {
            // Normal logging without context enhancement
            logger.info(message, args);
        }
    }
    
    /**
     * Logs a warning with SLF4J pattern formatting always included.
     * Uses the default pattern.
     * 
     * @param logger the SLF4J logger to use
     * @param message the message template  
     * @param args the message arguments
     */
    public static void warn(Logger logger, String message, Object... args) {
        warn(logger, "plugin", "default", message, args);
    }
    
    /**
     * Logs a warning with configurable SLF4J pattern formatting.
     * 
     * @param logger the SLF4J logger to use
     * @param patternSrc the pattern source ("plugin" or "native")
     * @param pattern the SLF4J pattern string or predefined name
     * @param message the message template
     * @param args the message arguments
     */
    public static void warn(Logger logger, String patternSrc, String pattern, String message, Object... args) {
        String formattedMessage = SLF4JPatternFormatter.format(pattern, message, args);
        logger.warn(formattedMessage);
    }
    
    /**
     * Logs an error with SLF4J pattern formatting always included.
     * Uses the default pattern.
     * 
     * @param logger the SLF4J logger to use
     * @param message the message template
     * @param args the message arguments
     */
    public static void error(Logger logger, String message, Object... args) {
        error(logger, "plugin", "default", message, args);
    }
    
    /**
     * Logs an error with configurable SLF4J pattern formatting.
     * 
     * @param logger the SLF4J logger to use
     * @param patternSrc the pattern source ("plugin" or "native")
     * @param pattern the SLF4J pattern string or predefined name
     * @param message the message template
     * @param args the message arguments
     */
    public static void error(Logger logger, String patternSrc, String pattern, String message, Object... args) {
        String formattedMessage = SLF4JPatternFormatter.format(pattern, message, args);
        logger.error(formattedMessage);
    }
}