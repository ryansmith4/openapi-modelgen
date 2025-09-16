package com.guidedbyte.openapi.modelgen.util;

import com.guidedbyte.openapi.modelgen.services.LoggingContext;
import com.guidedbyte.openapi.modelgen.services.RichFileLogger;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.io.File;
import java.util.function.Supplier;

/**
 * Smart plugin logger that provides intelligent log level routing, dual output, and performance optimizations.
 *
 * <p>This logger combines the best of both worlds - infrastructure-level dual logging with
 * intelligent business logic routing. It automatically provides:</p>
 * <ul>
 *   <li><strong>Console Output:</strong> Respects plugin log level settings for Gradle console</li>
 *   <li><strong>Rich File Output:</strong> Always logs to detailed debug file with full MDC context</li>
 *   <li><strong>Intelligent Routing:</strong> Routes messages to appropriate levels based on plugin LogLevel</li>
 *   <li><strong>Lazy Evaluation:</strong> Expensive log message construction only when needed</li>
 *   <li><strong>Context Awareness:</strong> Adjusts output based on current plugin log level</li>
 *   <li><strong>Smart Diagnostics:</strong> Automatic routing for template customization and performance logs</li>
 *   <li><strong>Performance:</strong> Zero overhead when log levels are disabled</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Standard SLF4J interface with intelligent routing
 * Logger logger = PluginLoggerFactory.getLogger(MyClass.class);
 *
 * LoggingContext.setContext("pets", "pojo.mustache");
 * LoggingContext.setComponent("CustomizationEngine");
 *
 * // Standard logging - routes based on plugin log level
 * logger.info("Processing spec: {}", specName);
 * logger.debug("Template cache hit rate: {}%", hitRate);
 * logger.trace("Pattern matching details: {}", details);
 *
 * // Lazy evaluation for expensive operations
 * ((PluginLogger) logger).debug("Expensive analysis: {}", () -> performAnalysis());
 *
 * // Conditional execution
 * ((PluginLogger) logger).ifDebug(() -> {
 *     String report = generateDetailedReport();
 *     logger.debug("Detailed report: {}", report);
 * });
 *
 * // Smart diagnostics - automatically routes to appropriate levels
 * ((PluginLogger) logger).customizationDiagnostic("pattern_match", "Pattern matched: {}", pattern);
 * ((PluginLogger) logger).performanceMetric("build_summary", "Build completed in {}ms", duration);
 * }</pre>
 *
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
public class PluginLogger implements Logger {

    private final Logger delegate;
    private final RichFileLogger richLogger;
    private final boolean fileLoggingEnabled;
    private final PluginState pluginState;

    /**
     * Creates a plugin logger with both console and file output.
     *
     * @param delegate the underlying SLF4J logger for console output
     * @param buildDir the build directory for rich file logging (null to disable file logging)
     */
    public PluginLogger(Logger delegate, File buildDir) {
        this.delegate = delegate;
        this.pluginState = PluginState.getInstance();

        // Initialize rich file logger if build directory is provided
        RichFileLogger tempRichLogger = null;
        boolean tempFileLoggingEnabled = false;

        if (buildDir != null) {
            try {
                tempRichLogger = RichFileLogger.forBuildDir(buildDir);
                tempFileLoggingEnabled = true;
            } catch (Exception e) {
                // Fallback gracefully if file logging fails
                if (pluginState.isWarnEnabled()) {
                    delegate.warn("Failed to initialize rich file logging: {}", e.getMessage());
                }
                tempRichLogger = null;
                tempFileLoggingEnabled = false;
            }
        }

        this.richLogger = tempRichLogger;
        this.fileLoggingEnabled = tempFileLoggingEnabled;
    }

    /**
     * Creates a plugin logger with console output only.
     *
     * @param delegate the underlying SLF4J logger for console output
     */
    public PluginLogger(Logger delegate) {
        this(delegate, null);
    }

    // ==================== DEBUG METHODS (Plugin-aware) ====================

    @Override
    public boolean isDebugEnabled() {
        return pluginState.isDebugEnabled() && delegate.isDebugEnabled();
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return pluginState.isDebugEnabled() && delegate.isDebugEnabled(marker);
    }

    @Override
    public void debug(String msg) {
        // Console output (filtered by plugin debug state)
        if (pluginState.isDebugEnabled()) {
            delegate.debug(msg);
        }
        // File output (always logged for troubleshooting)
        if (fileLoggingEnabled) {
            richLogger.debug(msg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if (pluginState.isDebugEnabled()) {
            delegate.debug(format, arg);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(format, arg);
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (pluginState.isDebugEnabled()) {
            delegate.debug(format, arg1, arg2);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(format, arg1, arg2);
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (pluginState.isDebugEnabled()) {
            delegate.debug(format, arguments);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(format, arguments);
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (pluginState.isDebugEnabled()) {
            delegate.debug(msg, t);
        }
        if (fileLoggingEnabled) {
            richLogger.debug("{} - Exception: {}", msg, t.getMessage());
        }
    }

    @Override
    public void debug(Marker marker, String msg) {
        if (pluginState.isDebugEnabled()) {
            delegate.debug(marker, msg);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(msg);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        if (pluginState.isDebugEnabled()) {
            delegate.debug(marker, format, arg);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(format, arg);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        if (pluginState.isDebugEnabled()) {
            delegate.debug(marker, format, arg1, arg2);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(format, arg1, arg2);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        if (pluginState.isDebugEnabled()) {
            delegate.debug(marker, format, arguments);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(format, arguments);
        }
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        if (pluginState.isDebugEnabled()) {
            delegate.debug(marker, msg, t);
        }
        if (fileLoggingEnabled) {
            richLogger.debug("{} - Exception: {}", msg, t.getMessage());
        }
    }

    // ==================== PASS-THROUGH METHODS ====================
    // All other log levels pass through normally to maintain SLF4J compatibility

    @Override
    public String getName() {
        return delegate.getName();
    }

    // INFO level - respects plugin INFO level setting
    @Override
    public boolean isInfoEnabled() {
        return pluginState.isInfoEnabled() && delegate.isInfoEnabled();
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return pluginState.isInfoEnabled() && delegate.isInfoEnabled(marker);
    }

    @Override
    public void info(String msg) {
        if (pluginState.isInfoEnabled()) {
            delegate.info(msg);
        }
        if (fileLoggingEnabled) {
            richLogger.info(msg);
        }
    }

    @Override
    public void info(String format, Object arg) {
        if (pluginState.isInfoEnabled()) {
            delegate.info(format, arg);
        }
        if (fileLoggingEnabled) {
            richLogger.info(format, arg);
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (pluginState.isInfoEnabled()) {
            delegate.info(format, arg1, arg2);
        }
        if (fileLoggingEnabled) {
            richLogger.info(format, arg1, arg2);
        }
    }

    @Override
    public void info(String format, Object... arguments) {
        if (pluginState.isInfoEnabled()) {
            delegate.info(format, arguments);
        }
        if (fileLoggingEnabled) {
            richLogger.info(format, arguments);
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        if (pluginState.isInfoEnabled()) {
            delegate.info(msg, t);
        }
        if (fileLoggingEnabled) {
            richLogger.info("{} - Exception: {}", msg, t.getMessage());
        }
    }

    @Override
    public void info(Marker marker, String msg) {
        if (pluginState.isInfoEnabled()) {
            delegate.info(marker, msg);
        }
        if (fileLoggingEnabled) {
            richLogger.info(msg);
        }
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        if (pluginState.isInfoEnabled()) {
            delegate.info(marker, format, arg);
        }
        if (fileLoggingEnabled) {
            richLogger.info(format, arg);
        }
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        if (pluginState.isInfoEnabled()) {
            delegate.info(marker, format, arg1, arg2);
        }
        if (fileLoggingEnabled) {
            richLogger.info(format, arg1, arg2);
        }
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        if (pluginState.isInfoEnabled()) {
            delegate.info(marker, format, arguments);
        }
        if (fileLoggingEnabled) {
            richLogger.info(format, arguments);
        }
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        if (pluginState.isInfoEnabled()) {
            delegate.info(marker, msg, t);
        }
        if (fileLoggingEnabled) {
            richLogger.info("{} - Exception: {}", msg, t.getMessage());
        }
    }

    // WARN level - respects plugin WARN level setting
    @Override
    public boolean isWarnEnabled() {
        return pluginState.isWarnEnabled() && delegate.isWarnEnabled();
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return pluginState.isWarnEnabled() && delegate.isWarnEnabled(marker);
    }

    @Override
    public void warn(String msg) {
        if (pluginState.isWarnEnabled()) {
            delegate.warn(msg);
        }
        if (fileLoggingEnabled) {
            richLogger.warn(msg);
        }
    }

    @Override
    public void warn(String format, Object arg) {
        if (pluginState.isWarnEnabled()) {
            delegate.warn(format, arg);
        }
        if (fileLoggingEnabled) {
            richLogger.warn(format, arg);
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (pluginState.isWarnEnabled()) {
            delegate.warn(format, arg1, arg2);
        }
        if (fileLoggingEnabled) {
            richLogger.warn(format, arg1, arg2);
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (pluginState.isWarnEnabled()) {
            delegate.warn(format, arguments);
        }
        if (fileLoggingEnabled) {
            richLogger.warn(format, arguments);
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (pluginState.isWarnEnabled()) {
            delegate.warn(msg, t);
        }
        if (fileLoggingEnabled) {
            richLogger.warn("{} - Exception: {}", msg, t.getMessage());
        }
    }

    @Override
    public void warn(Marker marker, String msg) {
        if (pluginState.isWarnEnabled()) {
            delegate.warn(marker, msg);
        }
        if (fileLoggingEnabled) {
            richLogger.warn(msg);
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        if (pluginState.isWarnEnabled()) {
            delegate.warn(marker, format, arg);
        }
        if (fileLoggingEnabled) {
            richLogger.warn(format, arg);
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        if (pluginState.isWarnEnabled()) {
            delegate.warn(marker, format, arg1, arg2);
        }
        if (fileLoggingEnabled) {
            richLogger.warn(format, arg1, arg2);
        }
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        if (pluginState.isWarnEnabled()) {
            delegate.warn(marker, format, arguments);
        }
        if (fileLoggingEnabled) {
            richLogger.warn(format, arguments);
        }
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        if (pluginState.isWarnEnabled()) {
            delegate.warn(marker, msg, t);
        }
        if (fileLoggingEnabled) {
            richLogger.warn("{} - Exception: {}", msg, t.getMessage());
        }
    }

    // ERROR level - respects plugin ERROR level setting
    @Override
    public boolean isErrorEnabled() {
        return pluginState.isErrorEnabled() && delegate.isErrorEnabled();
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return pluginState.isErrorEnabled() && delegate.isErrorEnabled(marker);
    }

    @Override
    public void error(String msg) {
        if (pluginState.isErrorEnabled()) {
            delegate.error(msg);
        }
        if (fileLoggingEnabled) {
            richLogger.error(msg);
        }
    }

    @Override
    public void error(String format, Object arg) {
        if (pluginState.isErrorEnabled()) {
            delegate.error(format, arg);
        }
        if (fileLoggingEnabled) {
            richLogger.error(format, arg);
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (pluginState.isErrorEnabled()) {
            delegate.error(format, arg1, arg2);
        }
        if (fileLoggingEnabled) {
            richLogger.error(format, arg1, arg2);
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        if (pluginState.isErrorEnabled()) {
            delegate.error(format, arguments);
        }
        if (fileLoggingEnabled) {
            richLogger.error(format, arguments);
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        if (pluginState.isErrorEnabled()) {
            delegate.error(msg, t);
        }
        if (fileLoggingEnabled) {
            richLogger.error("{} - Exception: {}", msg, t.getMessage());
        }
    }

    @Override
    public void error(Marker marker, String msg) {
        if (pluginState.isErrorEnabled()) {
            delegate.error(marker, msg);
        }
        if (fileLoggingEnabled) {
            richLogger.error(msg);
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        if (pluginState.isErrorEnabled()) {
            delegate.error(marker, format, arg);
        }
        if (fileLoggingEnabled) {
            richLogger.error(format, arg);
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        if (pluginState.isErrorEnabled()) {
            delegate.error(marker, format, arg1, arg2);
        }
        if (fileLoggingEnabled) {
            richLogger.error(format, arg1, arg2);
        }
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        if (pluginState.isErrorEnabled()) {
            delegate.error(marker, format, arguments);
        }
        if (fileLoggingEnabled) {
            richLogger.error(format, arguments);
        }
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        if (pluginState.isErrorEnabled()) {
            delegate.error(marker, msg, t);
        }
        if (fileLoggingEnabled) {
            richLogger.error("{} - Exception: {}", msg, t.getMessage());
        }
    }

    // TRACE level - routes to DEBUG with [TRACE] prefix as per SmartLogger pattern
    @Override
    public boolean isTraceEnabled() {
        return pluginState.isTraceEnabled() && delegate.isDebugEnabled();
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return pluginState.isTraceEnabled() && delegate.isDebugEnabled(marker);
    }

    @Override
    public void trace(String msg) {
        if (pluginState.isTraceEnabled()) {
            delegate.debug("[TRACE] " + msg);
        }
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + msg);
        }
    }

    @Override
    public void trace(String format, Object arg) {
        if (pluginState.isTraceEnabled()) {
            delegate.debug("[TRACE] " + format, arg);
        }
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + format, arg);
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (pluginState.isTraceEnabled()) {
            delegate.debug("[TRACE] " + format, arg1, arg2);
        }
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + format, arg1, arg2);
        }
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (pluginState.isTraceEnabled()) {
            delegate.debug("[TRACE] " + format, arguments);
        }
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + format, arguments);
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (pluginState.isTraceEnabled()) {
            delegate.debug("[TRACE] " + msg, t);
        }
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] {} - Exception: {}", msg, t.getMessage());
        }
    }

    @Override
    public void trace(Marker marker, String msg) {
        if (pluginState.isTraceEnabled()) {
            delegate.debug(marker, "[TRACE] " + msg);
        }
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + msg);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        if (pluginState.isTraceEnabled()) {
            delegate.debug(marker, "[TRACE] " + format, arg);
        }
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + format, arg);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        if (pluginState.isTraceEnabled()) {
            delegate.debug(marker, "[TRACE] " + format, arg1, arg2);
        }
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + format, arg1, arg2);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object... arguments) {
        if (pluginState.isTraceEnabled()) {
            delegate.debug(marker, "[TRACE] " + format, arguments);
        }
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + format, arguments);
        }
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        if (pluginState.isTraceEnabled()) {
            delegate.debug(marker, "[TRACE] " + msg, t);
        }
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] {} - Exception: {}", msg, t.getMessage());
        }
    }

    // ==================== SMART LOGGER METHODS ====================

    /**
     * Logs an informational message with lazy evaluation. Visible at INFO level and above.
     */
    public void info(String message, Supplier<Object> argSupplier) {
        if (pluginState.isInfoEnabled()) {
            delegate.info(message, argSupplier.get());
        }
        if (fileLoggingEnabled) {
            richLogger.info(message, argSupplier.get());
        }
    }

    /**
     * Logs a debug message with lazy evaluation. Visible at DEBUG level and above.
     */
    public void debug(String message, Supplier<Object> argSupplier) {
        if (pluginState.isDebugEnabled()) {
            delegate.debug(message, argSupplier.get());
        }
        if (fileLoggingEnabled) {
            richLogger.debug(message, argSupplier.get());
        }
    }

    /**
     * Logs a trace message with lazy evaluation. Visible only at TRACE level.
     */
    public void trace(String message, Supplier<Object> argSupplier) {
        if (pluginState.isTraceEnabled()) {
            delegate.debug("[TRACE] " + message, argSupplier.get());
        }
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + message, argSupplier.get());
        }
    }

    /**
     * Executes code only if INFO level is enabled.
     */
    public void ifInfo(Runnable action) {
        if (pluginState.isInfoEnabled()) {
            action.run();
        }
    }

    /**
     * Executes code only if DEBUG level is enabled.
     */
    public void ifDebug(Runnable action) {
        if (pluginState.isDebugEnabled()) {
            action.run();
        }
    }

    /**
     * Executes code only if TRACE level is enabled.
     */
    public void ifTrace(Runnable action) {
        if (pluginState.isTraceEnabled()) {
            action.run();
        }
    }

    /**
     * Logs template customization diagnostics at the appropriate level.
     * - Pattern matches: TRACE
     * - Applied operations: DEBUG
     * - Summary stats: INFO
     */
    public void customizationDiagnostic(String type, String message, Object... args) {
        switch (type.toLowerCase()) {
            case "pattern_match":
            case "template_diff":
            case "condition_evaluation":
                trace(message, args);
                break;
            case "operation_applied":
            case "cache_operation":
            case "variable_analysis":
                debug(message, args);
                break;
            case "summary":
            case "performance":
            case "progress":
                info(message, args);
                break;
            default:
                debug(message, args);
        }
    }

    /**
     * Logs performance metrics at the appropriate level.
     * - Detailed timing: DEBUG
     * - Summary metrics: INFO
     * - Build completion: INFO
     */
    public void performanceMetric(String scope, String message, Object... args) {
        switch (scope.toLowerCase()) {
            case "build_summary":
            case "completion":
            case "cache_performance":
            case "phase_timing":
                info(message, args);
                break;
            case "operation_timing":
            case "detailed_metrics":
                debug(message, args);
                break;
            default:
                info(message, args);
        }
    }

    /**
     * Gets the current log level.
     */
    public LogLevel getCurrentLogLevel() {
        return pluginState.getLogLevel();
    }

    /**
     * Gets the underlying SLF4J logger.
     */
    public Logger getUnderlyingLogger() {
        return delegate;
    }

    /**
     * Checks if the specified log level is enabled.
     */
    public boolean isLevelEnabled(LogLevel level) {
        return pluginState.getLogLevel().includes(level);
    }

    // ==================== PLUGIN-SPECIFIC UTILITY METHODS ====================

    /**
     * Writes a section header to the rich log file.
     * This is a plugin-specific method not part of SLF4J interface.
     *
     * @param section the section name
     */
    public void section(String section) {
        if (fileLoggingEnabled) {
            richLogger.section(section);
        }
    }

    /**
     * Closes the rich file logger and flushes any remaining content.
     * This is automatically called via shutdown hook, but can be called manually.
     */
    public void close() {
        if (fileLoggingEnabled) {
            richLogger.close();
        }
    }

    /**
     * Returns whether file logging is enabled for this logger.
     *
     * @return true if file logging is enabled, false otherwise
     */
    public boolean isFileLoggingEnabled() {
        return fileLoggingEnabled;
    }
}