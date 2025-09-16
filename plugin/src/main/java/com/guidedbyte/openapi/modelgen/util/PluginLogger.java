package com.guidedbyte.openapi.modelgen.util;

import com.guidedbyte.openapi.modelgen.services.LoggingContext;
import com.guidedbyte.openapi.modelgen.services.RichFileLogger;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.io.File;

/**
 * Unified plugin logger that provides console logging with debug filtering and rich file logging.
 *
 * <p>This logger wraps a standard SLF4J logger and automatically provides:</p>
 * <ul>
 *   <li><strong>Console Output:</strong> Respects plugin debug settings for Gradle console</li>
 *   <li><strong>Rich File Output:</strong> Always logs to detailed debug file with full MDC context</li>
 *   <li><strong>Debug Filtering:</strong> Debug calls automatically respect plugin debug settings</li>
 *   <li><strong>Context Aware:</strong> Automatically includes MDC context in file logs</li>
 *   <li><strong>Performance:</strong> Single call, dual output with minimal overhead</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Standard SLF4J interface - automatically outputs to both console and file
 * Logger logger = PluginLoggerFactory.getLogger(MyClass.class);
 *
 * LoggingContext.setContext("pets", "pojo.mustache");
 * LoggingContext.setComponent("CustomizationEngine");
 *
 * logger.debug("Processing template");
 * // Console: "Processing template" (if plugin debug enabled)
 * // File: "2025-01-15 14:30:45 [DEBUG] [CustomizationEngine] [pets:pojo.mustache] - Processing template"
 *
 * logger.info("Template completed");
 * // Console: "Template completed" (always)
 * // File: "2025-01-15 14:30:45 [INFO] [CustomizationEngine] [pets:pojo.mustache] - Template completed"
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
        return PluginState.getInstance().isDebugEnabled() && delegate.isDebugEnabled();
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return PluginState.getInstance().isDebugEnabled() && delegate.isDebugEnabled(marker);
    }

    @Override
    public void debug(String msg) {
        // Console output (filtered by plugin debug state)
        if (PluginState.getInstance().isDebugEnabled()) {
            delegate.debug(msg);
        }
        // File output (always logged for troubleshooting)
        if (fileLoggingEnabled) {
            richLogger.debug(msg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if (PluginState.getInstance().isDebugEnabled()) {
            delegate.debug(format, arg);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(format, arg);
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (PluginState.getInstance().isDebugEnabled()) {
            delegate.debug(format, arg1, arg2);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(format, arg1, arg2);
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (PluginState.getInstance().isDebugEnabled()) {
            delegate.debug(format, arguments);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(format, arguments);
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (PluginState.getInstance().isDebugEnabled()) {
            delegate.debug(msg, t);
        }
        if (fileLoggingEnabled) {
            richLogger.debug("{} - Exception: {}", msg, t.getMessage());
        }
    }

    @Override
    public void debug(Marker marker, String msg) {
        if (PluginState.getInstance().isDebugEnabled()) {
            delegate.debug(marker, msg);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(msg);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        if (PluginState.getInstance().isDebugEnabled()) {
            delegate.debug(marker, format, arg);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(format, arg);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        if (PluginState.getInstance().isDebugEnabled()) {
            delegate.debug(marker, format, arg1, arg2);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(format, arg1, arg2);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        if (PluginState.getInstance().isDebugEnabled()) {
            delegate.debug(marker, format, arguments);
        }
        if (fileLoggingEnabled) {
            richLogger.debug(format, arguments);
        }
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        if (PluginState.getInstance().isDebugEnabled()) {
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

    // INFO level - pass through (future: could add MDC context here)
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

    // WARN level - pass through (future: could add custom formatting)
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

    // ERROR level - pass through (future: could add structured error data)
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

    // TRACE level - pass through (though rarely used)
    @Override
    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isTraceEnabled(marker);
    }

    @Override
    public void trace(String msg) {
        delegate.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        delegate.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        delegate.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        delegate.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        delegate.trace(msg, t);
    }

    @Override
    public void trace(Marker marker, String msg) {
        delegate.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        delegate.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        delegate.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... arguments) {
        delegate.trace(marker, format, arguments);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        delegate.trace(marker, msg, t);
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