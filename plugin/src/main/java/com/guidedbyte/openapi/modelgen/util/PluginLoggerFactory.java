package com.guidedbyte.openapi.modelgen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating unified plugin loggers with console and rich file logging.
 *
 * <p>This factory creates loggers that wrap standard SLF4J loggers with plugin-specific
 * enhancements including debug filtering, rich file logging with MDC context, and
 * automatic dual output (console + file).</p>
 *
 * <p>This provides a drop-in replacement for {@code LoggerFactory.getLogger()} that:</p>
 * <ul>
 *   <li>Eliminates the need for {@code DebugLogger} utility calls</li>
 *   <li>Automatically provides rich file logging with full context</li>
 *   <li>Filters console debug output based on plugin settings</li>
 *   <li>Always logs to file for comprehensive troubleshooting</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Simple console + file logging
 * Logger logger = PluginLoggerFactory.getLogger(MyClass.class);
 *
 * LoggingContext.setContext("pets", "pojo.mustache");
 * LoggingContext.setComponent("CustomizationEngine");
 *
 * logger.debug("Processing template");
 * // Console: "Processing template" (if debug enabled)
 * // File: "2025-01-15 14:30:45 [DEBUG] [CustomizationEngine] [pets:pojo.mustache] - Processing template"
 * }</pre>
 *
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
public final class PluginLoggerFactory {

    private PluginLoggerFactory() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a plugin logger with console output and automatic rich file logging.
     *
     * <p>Rich file logging is automatically enabled if a build directory has been
     * set in PluginState. This provides the optimal logging experience without
     * requiring manual build directory management.</p>
     *
     * @param clazz the class to create a logger for
     * @return a plugin logger with debug filtering and automatic rich file logging
     */
    public static Logger getLogger(Class<?> clazz) {
        Logger delegate = LoggerFactory.getLogger(clazz);

        // Automatically enable rich file logging if build directory is available
        java.io.File buildDir = PluginState.getInstance().getBuildDirectory();
        return new PluginLogger(delegate, buildDir);
    }

    /**
     * Creates a plugin logger with console output and automatic rich file logging.
     *
     * <p>Rich file logging is automatically enabled if a build directory has been
     * set in PluginState. This provides the optimal logging experience without
     * requiring manual build directory management.</p>
     *
     * @param name the logger name
     * @return a plugin logger with debug filtering and automatic rich file logging
     */
    public static Logger getLogger(String name) {
        Logger delegate = LoggerFactory.getLogger(name);

        // Automatically enable rich file logging if build directory is available
        java.io.File buildDir = PluginState.getInstance().getBuildDirectory();
        return new PluginLogger(delegate, buildDir);
    }

    /**
     * Creates a plugin logger with both console and rich file output.
     *
     * <p><strong>Note:</strong> This method is provided for legacy compatibility
     * and testing purposes. The {@link #getLogger(Class)} method automatically
     * enables rich file logging when a build directory is available in PluginState.</p>
     *
     * @param clazz the class to create a logger for
     * @param buildDir the build directory for rich file logging
     * @return a plugin logger with dual output
     */
    public static Logger getLogger(Class<?> clazz, java.io.File buildDir) {
        Logger delegate = LoggerFactory.getLogger(clazz);
        return new PluginLogger(delegate, buildDir);
    }

    /**
     * Creates a plugin logger with both console and rich file output.
     *
     * <p><strong>Note:</strong> This method is provided for legacy compatibility
     * and testing purposes. The {@link #getLogger(String)} method automatically
     * enables rich file logging when a build directory is available in PluginState.</p>
     *
     * @param name the logger name
     * @param buildDir the build directory for rich file logging
     * @return a plugin logger with dual output
     */
    public static Logger getLogger(String name, java.io.File buildDir) {
        Logger delegate = LoggerFactory.getLogger(name);
        return new PluginLogger(delegate, buildDir);
    }

    /**
     * Creates a standard SLF4J logger (bypass plugin enhancements).
     *
     * <p>Use this when you want normal SLF4J behavior without plugin-specific features.
     * This is useful for framework-level logging or when you need debug output
     * regardless of plugin settings.</p>
     *
     * @param clazz the class to create a logger for
     * @return a standard SLF4J logger without plugin enhancements
     */
    public static Logger getStandardLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * Creates a standard SLF4J logger (bypass plugin enhancements).
     *
     * @param name the logger name
     * @return a standard SLF4J logger without plugin enhancements
     */
    public static Logger getStandardLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

}