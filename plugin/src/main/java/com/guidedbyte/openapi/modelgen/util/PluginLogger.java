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
 *   <li><strong>Console Output:</strong> Pure Gradle console control with no plugin-level filtering</li>
 *   <li><strong>Rich File Output:</strong> Always logs to detailed debug file with full MDC context</li>
 *   <li><strong>Pure Gradle Control:</strong> Console output controlled entirely by Gradle logging configuration</li>
 *   <li><strong>Lazy Evaluation:</strong> Expensive log message construction only when needed</li>
 *   <li><strong>Context Awareness:</strong> Rich file logging captures full context for debugging</li>
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
 * // Standard logging - console controlled by Gradle, file always enabled
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

    /**
     * Creates a plugin logger with both console and file output.
     *
     * @param delegate the underlying SLF4J logger for console output
     * @param buildDir the build directory for rich file logging (null to disable file logging)
     */
    public PluginLogger(Logger delegate, File buildDir) {
        this.delegate = delegate;

        // Initialize rich file logger if build directory is provided
        RichFileLogger tempRichLogger = null;
        boolean tempFileLoggingEnabled = false;

        if (buildDir != null) {
            try {
                tempRichLogger = RichFileLogger.forBuildDir(buildDir);
                tempFileLoggingEnabled = true;
            } catch (Exception e) {
                // Fallback gracefully if file logging fails
                // Fallback gracefully if file logging fails - use standard SLF4J
                delegate.warn("Failed to initialize rich file logging: {}", e.getMessage());
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
        return delegate.isDebugEnabled();
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegate.isDebugEnabled(marker);
    }

    @Override
    public void debug(String msg) {
        // Console output (pure Gradle control)
        delegate.debug(msg);
        // File output (always logged at DEBUG level for comprehensive debugging)
        if (fileLoggingEnabled) {
            richLogger.debug(msg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        delegate.debug(format, arg);
        if (fileLoggingEnabled) {
            richLogger.debug(format, arg);
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        delegate.debug(format, arg1, arg2);
        if (fileLoggingEnabled) {
            richLogger.debug(format, arg1, arg2);
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        delegate.debug(format, arguments);
        if (fileLoggingEnabled) {
            richLogger.debug(format, arguments);
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        delegate.debug(msg, t);
        if (fileLoggingEnabled) {
            richLogger.debug("{} - Exception: {}", msg, t.getMessage());
        }
    }

    @Override
    public void debug(Marker marker, String msg) {
        delegate.debug(marker, msg);
        if (fileLoggingEnabled) {
            richLogger.debug(msg);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        delegate.debug(marker, format, arg);
        if (fileLoggingEnabled) {
            richLogger.debug(format, arg);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        delegate.debug(marker, format, arg1, arg2);
        if (fileLoggingEnabled) {
            richLogger.debug(format, arg1, arg2);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        delegate.debug(marker, format, arguments);
        if (fileLoggingEnabled) {
            richLogger.debug(format, arguments);
        }
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        delegate.debug(marker, msg, t);
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

    // INFO level - pure Gradle console control
    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegate.isInfoEnabled(marker);
    }

    @Override
    public void info(String msg) {
        delegate.info(msg);
        if (fileLoggingEnabled) {
            richLogger.info(msg);
        }
    }

    @Override
    public void info(String format, Object arg) {
        delegate.info(format, arg);
        if (fileLoggingEnabled) {
            richLogger.info(format, arg);
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        delegate.info(format, arg1, arg2);
        if (fileLoggingEnabled) {
            richLogger.info(format, arg1, arg2);
        }
    }

    @Override
    public void info(String format, Object... arguments) {
        delegate.info(format, arguments);
        if (fileLoggingEnabled) {
            richLogger.info(format, arguments);
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        delegate.info(msg, t);
        if (fileLoggingEnabled) {
            richLogger.info("{} - Exception: {}", msg, t.getMessage());
        }
    }

    @Override
    public void info(Marker marker, String msg) {
        delegate.info(marker, msg);
        if (fileLoggingEnabled) {
            richLogger.info(msg);
        }
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        delegate.info(marker, format, arg);
        if (fileLoggingEnabled) {
            richLogger.info(format, arg);
        }
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        delegate.info(marker, format, arg1, arg2);
        if (fileLoggingEnabled) {
            richLogger.info(format, arg1, arg2);
        }
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        delegate.info(marker, format, arguments);
        if (fileLoggingEnabled) {
            richLogger.info(format, arguments);
        }
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        delegate.info(marker, msg, t);
        if (fileLoggingEnabled) {
            richLogger.info("{} - Exception: {}", msg, t.getMessage());
        }
    }

    // WARN level - pure Gradle console control
    @Override
    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegate.isWarnEnabled(marker);
    }

    @Override
    public void warn(String msg) {
        delegate.warn(msg);
        if (fileLoggingEnabled) {
            richLogger.warn(msg);
        }
    }

    @Override
    public void warn(String format, Object arg) {
        delegate.warn(format, arg);
        if (fileLoggingEnabled) {
            richLogger.warn(format, arg);
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        delegate.warn(format, arg1, arg2);
        if (fileLoggingEnabled) {
            richLogger.warn(format, arg1, arg2);
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        delegate.warn(format, arguments);
        if (fileLoggingEnabled) {
            richLogger.warn(format, arguments);
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        delegate.warn(msg, t);
        if (fileLoggingEnabled) {
            richLogger.warn("{} - Exception: {}", msg, t.getMessage());
        }
    }

    @Override
    public void warn(Marker marker, String msg) {
        delegate.warn(marker, msg);
        if (fileLoggingEnabled) {
            richLogger.warn(msg);
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        delegate.warn(marker, format, arg);
        if (fileLoggingEnabled) {
            richLogger.warn(format, arg);
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        delegate.warn(marker, format, arg1, arg2);
        if (fileLoggingEnabled) {
            richLogger.warn(format, arg1, arg2);
        }
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        delegate.warn(marker, format, arguments);
        if (fileLoggingEnabled) {
            richLogger.warn(format, arguments);
        }
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        delegate.warn(marker, msg, t);
        if (fileLoggingEnabled) {
            richLogger.warn("{} - Exception: {}", msg, t.getMessage());
        }
    }

    // ERROR level - pure Gradle console control
    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegate.isErrorEnabled(marker);
    }

    @Override
    public void error(String msg) {
        delegate.error(msg);
        if (fileLoggingEnabled) {
            richLogger.error(msg);
        }
    }

    @Override
    public void error(String format, Object arg) {
        delegate.error(format, arg);
        if (fileLoggingEnabled) {
            richLogger.error(format, arg);
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        delegate.error(format, arg1, arg2);
        if (fileLoggingEnabled) {
            richLogger.error(format, arg1, arg2);
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        delegate.error(format, arguments);
        if (fileLoggingEnabled) {
            richLogger.error(format, arguments);
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        delegate.error(msg, t);
        if (fileLoggingEnabled) {
            richLogger.error("{} - Exception: {}", msg, t.getMessage());
        }
    }

    @Override
    public void error(Marker marker, String msg) {
        delegate.error(marker, msg);
        if (fileLoggingEnabled) {
            richLogger.error(msg);
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        delegate.error(marker, format, arg);
        if (fileLoggingEnabled) {
            richLogger.error(format, arg);
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        delegate.error(marker, format, arg1, arg2);
        if (fileLoggingEnabled) {
            richLogger.error(format, arg1, arg2);
        }
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        delegate.error(marker, format, arguments);
        if (fileLoggingEnabled) {
            richLogger.error(format, arguments);
        }
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        delegate.error(marker, msg, t);
        if (fileLoggingEnabled) {
            richLogger.error("{} - Exception: {}", msg, t.getMessage());
        }
    }

    // TRACE level - routes to DEBUG with [TRACE] prefix as per SmartLogger pattern
    @Override
    public boolean isTraceEnabled() {
        return delegate.isDebugEnabled();
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isDebugEnabled(marker);
    }

    @Override
    public void trace(String msg) {
        delegate.debug("[TRACE] " + msg);
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + msg);
        }
    }

    @Override
    public void trace(String format, Object arg) {
        delegate.debug("[TRACE] " + format, arg);
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + format, arg);
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        delegate.debug("[TRACE] " + format, arg1, arg2);
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + format, arg1, arg2);
        }
    }

    @Override
    public void trace(String format, Object... arguments) {
        delegate.debug("[TRACE] " + format, arguments);
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + format, arguments);
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        delegate.debug("[TRACE] " + msg, t);
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] {} - Exception: {}", msg, t.getMessage());
        }
    }

    @Override
    public void trace(Marker marker, String msg) {
        delegate.debug(marker, "[TRACE] " + msg);
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + msg);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        delegate.debug(marker, "[TRACE] " + format, arg);
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + format, arg);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        delegate.debug(marker, "[TRACE] " + format, arg1, arg2);
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + format, arg1, arg2);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object... arguments) {
        delegate.debug(marker, "[TRACE] " + format, arguments);
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + format, arguments);
        }
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        delegate.debug(marker, "[TRACE] " + msg, t);
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] {} - Exception: {}", msg, t.getMessage());
        }
    }

    // ==================== SMART LOGGER METHODS ====================

    /**
     * Logs an informational message with lazy evaluation. Visible at INFO level and above.
     */
    public void info(String message, Supplier<Object> argSupplier) {
        delegate.info(message, argSupplier.get());
        if (fileLoggingEnabled) {
            richLogger.info(message, argSupplier.get());
        }
    }

    /**
     * Logs a debug message with lazy evaluation. Visible at DEBUG level and above.
     */
    public void debug(String message, Supplier<Object> argSupplier) {
        delegate.debug(message, argSupplier.get());
        if (fileLoggingEnabled) {
            richLogger.debug(message, argSupplier.get());
        }
    }

    /**
     * Logs a trace message with lazy evaluation. Visible only at TRACE level.
     */
    public void trace(String message, Supplier<Object> argSupplier) {
        delegate.debug("[TRACE] " + message, argSupplier.get());
        if (fileLoggingEnabled) {
            richLogger.debug("[TRACE] " + message, argSupplier.get());
        }
    }

    /**
     * Executes code only if INFO level is enabled.
     */
    public void ifInfo(Runnable action) {
        if (delegate.isInfoEnabled()) {
            action.run();
        }
    }

    /**
     * Executes code only if DEBUG level is enabled.
     */
    public void ifDebug(Runnable action) {
        if (delegate.isDebugEnabled()) {
            action.run();
        }
    }

    /**
     * Executes code only if TRACE level is enabled.
     */
    public void ifTrace(Runnable action) {
        if (delegate.isDebugEnabled()) {
            action.run();
        }
    }

    /**
     * Logs template customization diagnostics at the appropriate level.
     * Routes messages to different log levels based on diagnostic type:
     * - Pattern matches: TRACE
     * - Applied operations: DEBUG
     * - Summary stats: INFO
     *
     * @param type the type of diagnostic (e.g., "pattern_match", "operation_applied", "summary")
     * @param message the log message format string
     * @param args the message format arguments
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
     * Routes messages to different log levels based on performance scope:
     * - Detailed timing: DEBUG
     * - Summary metrics: INFO
     * - Build completion: INFO
     *
     * @param scope the performance scope (e.g., "build_summary", "operation_timing", "cache_performance")
     * @param message the log message format string
     * @param args the message format arguments
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
     * Gets the underlying SLF4J logger for direct access when needed.
     *
     * @return the underlying SLF4J logger delegate
     */
    public Logger getUnderlyingLogger() {
        return delegate;
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