package com.guidedbyte.openapi.modelgen.util;

import org.slf4j.Logger;

/**
 * Utility class for conditional debug logging based on plugin debug flag.
 * 
 * <p>This utility provides methods to log debug messages only when the debug flag 
 * is enabled, helping to reduce log noise in production while preserving detailed 
 * template resolution information for troubleshooting.</p>
 * 
 * <p>The debug flag is controlled at the plugin extension level and passed to 
 * various services and actions that need conditional logging.</p>
 * 
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Basic debug logging
 * DebugLogger.debug(logger, debugEnabled, "Template '{}' extracted for generator '{}'", 
 *                   templateName, generatorName);
 * 
 * // Conditional debug with expensive operations
 * DebugLogger.debugIf(debugEnabled, () -> {
 *     String details = computeExpensiveDetails();
 *     logger.debug("Expensive operation result: {}", details);
 * });
 * }</pre>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
public final class DebugLogger {
    
    private DebugLogger() {
        // Utility class
    }
    
    /**
     * Logs a debug message only if debug logging is enabled.
     * 
     * @param logger the SLF4J logger to use
     * @param debugEnabled whether debug logging is enabled
     * @param message the debug message
     */
    public static void debug(Logger logger, boolean debugEnabled, String message) {
        if (debugEnabled && logger.isDebugEnabled()) {
            logger.debug(message);
        }
    }
    
    /**
     * Logs a debug message with one argument only if debug logging is enabled.
     * 
     * @param logger the SLF4J logger to use
     * @param debugEnabled whether debug logging is enabled
     * @param format the debug message format
     * @param arg the argument to substitute
     */
    public static void debug(Logger logger, boolean debugEnabled, String format, Object arg) {
        if (debugEnabled && logger.isDebugEnabled()) {
            logger.debug(format, arg);
        }
    }
    
    /**
     * Logs a debug message with two arguments only if debug logging is enabled.
     * 
     * @param logger the SLF4J logger to use
     * @param debugEnabled whether debug logging is enabled
     * @param format the debug message format
     * @param arg1 the first argument to substitute
     * @param arg2 the second argument to substitute
     */
    public static void debug(Logger logger, boolean debugEnabled, String format, Object arg1, Object arg2) {
        if (debugEnabled && logger.isDebugEnabled()) {
            logger.debug(format, arg1, arg2);
        }
    }
    
    /**
     * Logs a debug message with multiple arguments only if debug logging is enabled.
     * 
     * @param logger the SLF4J logger to use
     * @param debugEnabled whether debug logging is enabled
     * @param format the debug message format
     * @param arguments the arguments to substitute
     */
    public static void debug(Logger logger, boolean debugEnabled, String format, Object... arguments) {
        if (debugEnabled && logger.isDebugEnabled()) {
            logger.debug(format, arguments);
        }
    }
    
    /**
     * Executes a debug logging block only if debug logging is enabled.
     * This is useful for expensive logging operations that should only be 
     * performed when debug logging is actually needed.
     * 
     * @param debugEnabled whether debug logging is enabled
     * @param debugAction the action to execute for debug logging
     */
    public static void debugIf(boolean debugEnabled, Runnable debugAction) {
        if (debugEnabled) {
            debugAction.run();
        }
    }
}