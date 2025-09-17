package com.guidedbyte.openapi.modelgen.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Logging levels for the OpenAPI Model Generator plugin, aligned with SLF4J standard levels.
 *
 * <p>Provides granular control over plugin logging output using the familiar SLF4J hierarchy.
 * This allows seamless integration with existing logging configurations and developer expectations.</p>
 *
 * <h2>SLF4J-Aligned Log Level Hierarchy:</h2>
 * <ul>
 *   <li><strong>ERROR:</strong> Build failures and critical errors only</li>
 *   <li><strong>WARN:</strong> + warnings and important notices</li>
 *   <li><strong>INFO:</strong> + spec processing, build progress, and completion summaries</li>
 *   <li><strong>DEBUG:</strong> + template processing, cache operations, and diagnostic details</li>
 *   <li><strong>TRACE:</strong> + comprehensive diagnostics, pattern matching, and deep troubleshooting</li>
 * </ul>
 *
 * <h2>Use Case Examples:</h2>
 * <pre>{@code
 * // Production CI/CD builds
 * logLevel=ERROR
 *
 * // Development builds
 * logLevel=INFO
 *
 * // Template customization development
 * logLevel=DEBUG
 *
 * // Deep troubleshooting
 * logLevel=TRACE
 * }</pre>
 *
 * <h2>What Each Level Shows:</h2>
 * <ul>
 *   <li><strong>ERROR:</strong> Build failures, critical configuration errors</li>
 *   <li><strong>WARN:</strong> + configuration warnings, deprecated usage, optimization suggestions</li>
 *   <li><strong>INFO:</strong> + spec processing, cache performance, build completion, progress indicators</li>
 *   <li><strong>DEBUG:</strong> + template resolution, customization application, variable context, cache details</li>
 *   <li><strong>TRACE:</strong> + pattern matching analysis, template diffs, condition evaluation, line-by-line context</li>
 * </ul>
 *
 * <h2>Integration with SLF4J:</h2>
 * <p>These levels map directly to SLF4J levels, ensuring consistent behavior with logging frameworks
 * like Logback, Log4j, and java.util.logging. Gradle's logging levels also align naturally:</p>
 * <ul>
 *   <li><strong>gradle --quiet</strong> → ERROR level</li>
 *   <li><strong>gradle (default)</strong> → WARN level</li>
 *   <li><strong>gradle --info</strong> → INFO level</li>
 *   <li><strong>gradle --debug</strong> → DEBUG level</li>
 * </ul>
 *
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public enum LogLevel {
    /**
     * Only errors and critical failures.
     * Shows: Build failures, critical configuration errors.
     * Maps to SLF4J ERROR level.
     */
    ERROR(0, "ERROR"),

    /**
     * Errors and warnings.
     * Shows: + Configuration warnings, deprecated usage, optimization suggestions.
     * Maps to SLF4J WARN level.
     */
    WARN(1, "WARN"),

    /**
     * Standard informational output.
     * Shows: + Spec processing, cache performance, build completion, progress indicators.
     * Maps to SLF4J INFO level.
     */
    INFO(2, "INFO"),

    /**
     * Diagnostic details for development and debugging.
     * Shows: + Template resolution, customization application, variable analysis, cache details.
     * Maps to SLF4J DEBUG level.
     */
    DEBUG(3, "DEBUG"),

    /**
     * Comprehensive diagnostics for deep troubleshooting.
     * Shows: + Pattern matching analysis, template diffs, condition evaluation, line-by-line context.
     * Maps to SLF4J TRACE level.
     */
    TRACE(4, "TRACE");

    private final int level;
    private final String displayName;

    LogLevel(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    /**
     * Gets the numeric level for comparison.
     *
     * @return the numeric level (0-4)
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets the display name for the log level.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this log level includes the specified level.
     * Higher levels include all lower levels.
     *
     * @param otherLevel the level to check
     * @return true if this level includes the other level
     */
    public boolean includes(LogLevel otherLevel) {
        return this.level >= otherLevel.level;
    }

    /**
     * Checks if ERROR logging is enabled.
     *
     * @return true if ERROR level or higher
     */
    public boolean isErrorEnabled() {
        return this.level >= ERROR.level;
    }

    /**
     * Checks if WARN logging is enabled.
     *
     * @return true if WARN level or higher
     */
    public boolean isWarnEnabled() {
        return this.level >= WARN.level;
    }

    /**
     * Checks if INFO logging is enabled.
     *
     * @return true if INFO level or higher
     */
    public boolean isInfoEnabled() {
        return this.level >= INFO.level;
    }

    /**
     * Checks if DEBUG logging is enabled.
     *
     * @return true if DEBUG level or higher
     */
    public boolean isDebugEnabled() {
        return this.level >= DEBUG.level;
    }

    /**
     * Checks if TRACE logging is enabled.
     *
     * @return true if TRACE level
     */
    public boolean isTraceEnabled() {
        return this == TRACE;
    }

    /**
     * Parses a log level from string, with fallback to INFO.
     *
     * @param logLevelString the log level string (case-insensitive)
     * @return the parsed log level, or INFO if invalid
     */
    public static LogLevel fromString(String logLevelString) {
        if (logLevelString == null || logLevelString.trim().isEmpty()) {
            return INFO;
        }

        String normalized = StringUtils.toRootUpperCase(logLevelString.trim());

        try {
            return LogLevel.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Handle common variations and legacy values
            switch (normalized) {
                case "OFF":
                case "ERRORS":
                case "QUIET":
                    return ERROR;
                case "WARNING":
                case "WARNINGS":
                    return WARN;
                case "STANDARD":
                case "NORMAL":
                case "VERBOSE":
                case "LIFECYCLE":
                    return INFO;
                case "ALL":
                case "FULL":
                    return TRACE;
                default:
                    return INFO;
            }
        }
    }

    /**
     * Gets the appropriate log level based on legacy debug flag and Gradle log level.
     *
     * @param debugEnabled legacy debug flag
     * @param gradleLogLevel Gradle's log level (if available)
     * @return the appropriate plugin log level
     */
    public static LogLevel fromLegacyAndGradle(boolean debugEnabled, String gradleLogLevel) {
        // Handle Gradle log levels - map directly to SLF4J equivalents
        if (gradleLogLevel != null) {
            String normalized = StringUtils.toRootUpperCase(gradleLogLevel);
            switch (normalized) {
                case "QUIET":
                    return ERROR;  // Only errors in quiet mode
                case "LIFECYCLE":
                    return WARN;   // Gradle's default level shows warnings
                case "INFO":
                    return INFO;   // Standard informational output
                case "DEBUG":
                    return DEBUG;  // Development debugging
                case "TRACE":
                    return TRACE;  // Deep troubleshooting
            }
        }

        // Legacy debug flag mapping
        return debugEnabled ? DEBUG : INFO;
    }

    @Override
    public String toString() {
        return displayName;
    }
}