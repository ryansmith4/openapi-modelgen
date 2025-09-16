package com.guidedbyte.openapi.modelgen.logging;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;
import java.util.concurrent.ConcurrentHashMap;
import com.guidedbyte.openapi.modelgen.logging.pattern.CompiledSLF4JPattern;

/**
 * SLF4J-compatible log formatter with pattern caching and global configuration.
 * 
 * <p>This formatter provides SLF4J pattern compatibility while working around Gradle's
 * logging limitations. It supports format modifiers and caches compiled patterns for
 * optimal performance.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li><strong>Pattern Caching:</strong> Patterns compiled once and reused</li>
 *   <li><strong>Format Modifiers:</strong> Supports %-20X{spec}, %.15X{template}, etc.</li>
 *   <li><strong>Global Configuration:</strong> Single format applies to all specs</li>
 *   <li><strong>High Performance:</strong> No regex at runtime, pre-allocated StringBuilders</li>
 *   <li><strong>SLF4J Compatible:</strong> Standard patterns work identically to logback</li>
 * </ul>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class SLF4JPatternFormatter {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SLF4JPatternFormatter() {
        // Utility class - prevent instantiation
    }

    private static final ConcurrentHashMap<String, CompiledSLF4JPattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    // Default patterns for easy use

    /** Standard pattern showing spec and template context. */
    public static final String DEFAULT_PATTERN = "[%X{spec}:%X{template}]";

    /** Minimal pattern showing only spec context. */
    public static final String MINIMAL_PATTERN = "[%X{spec}]";

    /** Verbose pattern with component, spec, and template context. */
    public static final String VERBOSE_PATTERN = "[%X{component}] [%X{spec}:%X{template}]";

    /** Debug pattern with pipe-separated component, spec, and template. */
    public static final String DEBUG_PATTERN = "[%X{component}|%X{spec}:%X{template}]";

    /** Aligned pattern with fixed-width spec and template columns. */
    public static final String ALIGNED_PATTERN = "[%-8X{spec}:%-15X{template}]";

    /** Timestamped pattern including time, spec, template, and message. */
    public static final String TIMESTAMPED_PATTERN = "%d{HH:mm:ss} [%X{spec}:%X{template}] %msg";
    
    /**
     * Formats a log message using the specified pattern and context.
     * 
     * @param logPatternSrc the pattern source ("plugin" or "native")
     * @param pattern the SLF4J pattern string or predefined name
     * @param logger the SLF4J logger to use for output
     * @param debugEnabled whether debug mode is enabled
     * @param message the log message
     * @param args optional message arguments
     */
    public static void log(String logPatternSrc, String pattern, Logger logger, boolean debugEnabled, 
                          String message, Object... args) {
        if (!debugEnabled) {
            // Normal debug logging when debug is disabled
            logger.debug(message, args);
            return;
        }
        
        if ("native".equals(logPatternSrc)) {
            // User expects Gradle/logback to handle formatting
            logger.debug(message, args);
            return;
        }
        
        // Use plugin's high-performance formatter
        String formattedMessage = format(pattern, message, args);
        
        // Use INFO level so it appears in Gradle console output
        logger.info(formattedMessage);
    }
    
    /**
     * Formats a message using the specified pattern.
     * 
     * @param pattern the SLF4J pattern string or predefined name
     * @param message the log message
     * @param args optional message arguments
     * @return the formatted string
     */
    public static String format(String pattern, String message, Object... args) {
        // Format the message with arguments first
        String formattedMessage = formatMessage(message, args);
        
        // Get context from MDC
        String spec = MDC.get("spec");
        String template = MDC.get("template");
        String component = MDC.get("component");
        
        return formatWithContext(pattern, formattedMessage, spec, template, component);
    }
    
    /**
     * Formats a message using the specified pattern with explicit context values.
     * 
     * @param pattern the SLF4J pattern string or predefined name
     * @param message the formatted log message
     * @param spec the spec name
     * @param template the template name
     * @param component the component name
     * @return the formatted string
     */
    public static String formatWithContext(String pattern, String message, String spec, String template, String component) {
        // Resolve pattern (handle predefined names)
        String resolvedPattern = resolvePattern(pattern);
        
        // Get or create compiled pattern (cached for performance)
        CompiledSLF4JPattern compiledPattern = PATTERN_CACHE.computeIfAbsent(resolvedPattern, 
                                                                             CompiledSLF4JPattern::new);
        
        // Format using compiled pattern
        return compiledPattern.format(message, spec, template, component);
    }
    
    /**
     * Resolves pattern names to actual SLF4J patterns.
     * 
     * @param pattern the pattern name or SLF4J pattern string
     * @return the resolved SLF4J pattern string
     */
    public static String resolvePattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return DEFAULT_PATTERN;
        }
        
        // Check for predefined pattern names
        switch (StringUtils.toRootLowerCase(pattern).trim()) {
            case "default":
                return DEFAULT_PATTERN;
            case "minimal":
                return MINIMAL_PATTERN;
            case "verbose":
                return VERBOSE_PATTERN;
            case "debug":
                return DEBUG_PATTERN;
            case "aligned":
                return ALIGNED_PATTERN;
            case "timestamped":
                return TIMESTAMPED_PATTERN;
            case "none":
            case "disabled":
                return ""; // No formatting
            default:
                // Treat as literal SLF4J pattern
                return pattern;
        }
    }
    
    /**
     * Formats a message with arguments, similar to SLF4J's MessageFormatter.
     * 
     * @param message the message template
     * @param args the arguments
     * @return the formatted message
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "REC_CATCH_EXCEPTION",
        justification = "Defensive programming in logging utility: must catch all exceptions to ensure logging system " +
                       "reliability. Any exception during message formatting should not break the logging system, " +
                       "so we catch Exception and provide a fallback formatted message."
    )
    private static String formatMessage(String message, Object... args) {
        if (message == null) {
            return "";
        }
        
        if (args == null || args.length == 0) {
            return message;
        }
        
        try {
            // Simple {} placeholder replacement (like SLF4J)
            String result = message;
            for (Object arg : args) {
                int index = result.indexOf("{}");
                if (index != -1) {
                    String argStr = arg != null ? arg.toString() : "null";
                    result = result.substring(0, index) + argStr + result.substring(index + 2);
                } else {
                    break;
                }
            }
            return result;
        } catch (Exception e) { // Defensive: catch any unexpected formatting errors
            // Fallback if formatting fails
            return message + " " + java.util.Arrays.toString(args);
        }
    }
    
    /**
     * Clears the pattern cache. Useful for testing or memory management.
     */
    public static void clearPatternCache() {
        PATTERN_CACHE.clear();
    }
    
    /**
     * Returns the current size of the pattern cache.
     * 
     * @return number of cached patterns
     */
    public static int getPatternCacheSize() {
        return PATTERN_CACHE.size();
    }
    
    /**
     * Returns true if the specified pattern is cached.
     * 
     * @param pattern the pattern to check
     * @return true if cached
     */
    public static boolean isPatternCached(String pattern) {
        String resolved = resolvePattern(pattern);
        return PATTERN_CACHE.containsKey(resolved);
    }
}