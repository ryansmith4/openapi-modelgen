package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.constants.PluginConstants;
import com.guidedbyte.openapi.modelgen.logging.ContextAwareLogger;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Standardized error handling utilities for consistent error patterns across the plugin.
 * 
 * <p>This utility provides:</p>
 * <ul>
 *   <li><strong>Consistent Exception Wrapping:</strong> Standard patterns for wrapping and rethrowing exceptions</li>
 *   <li><strong>Actionable Error Messages:</strong> Context-rich messages that help users resolve issues</li>
 *   <li><strong>Standardized Logging:</strong> Consistent logging patterns for different error scenarios</li>
 *   <li><strong>Error Recovery:</strong> Graceful degradation strategies where appropriate</li>
 * </ul>
 * 
 * <p>Usage Examples:</p>
 * <pre>{@code
 * // File operation with context
 * ErrorHandlingUtils.handleFileOperation(
 *     () -> Files.readString(configFile),
 *     "Failed to read configuration file: " + configFile.getFileName(),
 *     "Verify the file exists and is readable. Check file permissions.",
 *     logger
 * );
 * 
 * // Validation with actionable message
 * ErrorHandlingUtils.validateNotNull(templateContent, 
 *     "Template content cannot be null",
 *     "Ensure the template file exists and contains valid content",
 *     logger);
 * }</pre>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class ErrorHandlingUtils {
    
    // Error context constants for consistency
    public static final String FILE_NOT_FOUND_GUIDANCE = "Verify the file exists and the path is correct.";
    public static final String PERMISSION_GUIDANCE = "Check file permissions and ensure the process has read/write access.";
    public static final String YAML_SYNTAX_GUIDANCE = "Validate YAML syntax using an online YAML validator or IDE.";
    public static final String CONFIG_GUIDANCE = "Review the plugin configuration documentation for correct syntax.";
    public static final String TEMPLATE_GUIDANCE = "Ensure template files exist in the expected directory structure.";
    public static final String VERSION_GUIDANCE = "Verify OpenAPI Generator is properly installed and accessible.";
    public static final String LIBRARY_GUIDANCE = "Verify library configuration and dependencies. Check library metadata files.";
    public static final String GRADLE_GUIDANCE = "Check Gradle configuration and ensure all required plugins are applied.";
    
    /**
     * Handles file operations with standardized error handling and context.
     * 
     * @param operation the file operation to perform
     * @param errorMessage the primary error message
     * @param actionableGuidance what the user should do to fix the issue
     * @param logger the logger to use for error reporting
     * @param <T> the return type of the operation
     * @return the result of the operation
     * @throws RuntimeException if the operation fails
     */
    public static <T> T handleFileOperation(
            FileOperation<T> operation, 
            String errorMessage, 
            String actionableGuidance, 
            Logger logger) {
        try {
            return operation.execute();
        } catch (IOException e) {
            String fullMessage = errorMessage + ". " + actionableGuidance;
            ContextAwareLogger.error(logger, fullMessage + " Cause: {}", e.getMessage());
            throw new RuntimeException(fullMessage, e);
        } catch (SecurityException e) {
            String fullMessage = errorMessage + ". " + PERMISSION_GUIDANCE;
            ContextAwareLogger.error(logger, fullMessage + " Cause: {}", e.getMessage());
            throw new RuntimeException(fullMessage, e);
        }
    }
    
    /**
     * Handles YAML parsing operations with specific guidance for YAML issues.
     * 
     * @param operation the YAML parsing operation
     * @param fileName the name of the YAML file being parsed
     * @param logger the logger to use
     * @param <T> the return type
     * @return the parsed result
     * @throws RuntimeException if parsing fails
     */
    public static <T> T handleYamlOperation(
            Supplier<T> operation,
            String fileName,
            Logger logger) {
        try {
            return operation.get();
        } catch (org.yaml.snakeyaml.constructor.ConstructorException e) {
            String message = "Invalid YAML structure in " + fileName + ". " + YAML_SYNTAX_GUIDANCE;
            ContextAwareLogger.error(logger, message + " Details: {}", e.getMessage());
            throw new RuntimeException(message, e);
        } catch (org.yaml.snakeyaml.parser.ParserException e) {
            String message = "YAML syntax error in " + fileName + ". " + YAML_SYNTAX_GUIDANCE;
            ContextAwareLogger.error(logger, message + " Details: {}", e.getMessage());
            throw new RuntimeException(message, e);
        } catch (Exception e) {
            String message = "Failed to parse YAML file " + fileName + ". " + YAML_SYNTAX_GUIDANCE;
            ContextAwareLogger.error(logger, message + " Cause: {}", e.getMessage());
            throw new RuntimeException(message, e);
        }
    }
    
    /**
     * Validates that a value is not null with actionable error context.
     * 
     * @param value the value to check
     * @param errorMessage the error message if null
     * @param actionableGuidance what the user should do
     * @param logger the logger to use
     * @throws IllegalArgumentException if value is null
     */
    public static void validateNotNull(Object value, String errorMessage, String actionableGuidance, Logger logger) {
        if (value == null) {
            String fullMessage = errorMessage + ". " + actionableGuidance;
            ContextAwareLogger.error(logger, fullMessage);
            throw new IllegalArgumentException(fullMessage);
        }
    }
    
    /**
     * Validates that a string is not null or empty with context.
     * 
     * @param value the string to check
     * @param fieldName the name of the field being validated
     * @param actionableGuidance what the user should do
     * @param logger the logger to use
     * @throws IllegalArgumentException if value is null or empty
     */
    public static void validateNotEmpty(String value, String fieldName, String actionableGuidance, Logger logger) {
        if (value == null || value.trim().isEmpty()) {
            String fullMessage = fieldName + " cannot be null or empty. " + actionableGuidance;
            ContextAwareLogger.error(logger, fullMessage);
            throw new IllegalArgumentException(fullMessage);
        }
    }
    
    /**
     * Validates that a file exists with actionable guidance.
     * 
     * @param filePath the path to validate
     * @param fileDescription description of what the file is for
     * @param logger the logger to use
     * @throws IllegalArgumentException if file doesn't exist
     */
    public static void validateFileExists(Path filePath, String fileDescription, Logger logger) {
        if (filePath == null || !filePath.toFile().exists()) {
            String message = fileDescription + " does not exist: " + (filePath != null ? filePath : "null") + ". " + FILE_NOT_FOUND_GUIDANCE;
            ContextAwareLogger.error(logger, message);
            throw new IllegalArgumentException(message);
        }
        
        if (!filePath.toFile().isFile()) {
            String message = fileDescription + " path is not a file: " + filePath + ". " + FILE_NOT_FOUND_GUIDANCE;
            ContextAwareLogger.error(logger, message);
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * Handles operations with graceful degradation and fallback.
     * 
     * @param primaryOperation the main operation to try
     * @param fallbackValue the value to return if operation fails
     * @param operationDescription description of what's being attempted
     * @param logger the logger to use
     * @param <T> the return type
     * @return result of primary operation or fallback value
     */
    public static <T> T withFallback(
            Supplier<T> primaryOperation,
            T fallbackValue,
            String operationDescription,
            Logger logger) {
        try {
            return primaryOperation.get();
        } catch (Exception e) {
            ContextAwareLogger.warn(logger, "Failed to {}, using fallback. Cause: {}", 
                operationDescription, e.getMessage());
            return fallbackValue;
        }
    }
    
    /**
     * Handles operations with retry logic for transient failures.
     * 
     * @param operation the operation to retry
     * @param maxAttempts maximum number of attempts
     * @param operationDescription description for logging
     * @param logger the logger to use
     * @param <T> the return type
     * @return the result of the operation
     * @throws RuntimeException if all attempts fail
     */
    public static <T> T withRetry(
            Supplier<T> operation,
            int maxAttempts,
            String operationDescription,
            Logger logger) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    ContextAwareLogger.debug(logger, true, "Retrying {}, attempt {} of {}", 
                        operationDescription, attempt, maxAttempts);
                }
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    ContextAwareLogger.warn(logger, "Attempt {} failed for {}: {}. Retrying...", 
                        attempt, operationDescription, e.getMessage());
                    // Brief pause between retries
                    try {
                        Thread.sleep(PluginConstants.BASE_RETRY_DELAY_MS * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        String message = "Failed to " + operationDescription + " after " + maxAttempts + " attempts";
        String lastErrorMessage = lastException != null ? lastException.getMessage() : "Unknown error (possibly interrupted)";
        ContextAwareLogger.error(logger, message + ". Last error: {}", lastErrorMessage);
        throw new RuntimeException(message, lastException);
    }
    
    /**
     * Wraps exceptions with additional context while preserving the original cause.
     * 
     * @param operation the operation to wrap
     * @param contextMessage additional context for the error
     * @param logger the logger to use
     * @param <T> the return type
     * @return the result of the operation
     * @throws RuntimeException with enhanced context
     */
    public static <T> T wrapWithContext(
            Supplier<T> operation,
            String contextMessage,
            Logger logger) {
        try {
            return operation.get();
        } catch (RuntimeException e) {
            // Don't double-wrap RuntimeExceptions, just add context via logging
            ContextAwareLogger.error(logger, "{}. Cause: {}", contextMessage, e.getMessage());
            throw e;
        } catch (Exception e) {
            String message = contextMessage + ". " + e.getMessage();
            ContextAwareLogger.error(logger, message);
            throw new RuntimeException(message, e);
        }
    }
    
    /**
     * Functional interface for file operations that can throw IOException.
     * 
     * @param <T> the return type
     */
    @FunctionalInterface
    public interface FileOperation<T> {
        T execute() throws IOException;
    }
    
    /**
     * Creates a standardized error message for configuration issues.
     * 
     * @param configProperty the configuration property that's invalid
     * @param actualValue the actual (invalid) value
     * @param expectedFormat description of the expected format
     * @return formatted error message with guidance
     */
    public static String formatConfigError(String configProperty, Object actualValue, String expectedFormat) {
        return String.format("Invalid configuration for '%s': got '%s', expected %s. %s", 
            configProperty, actualValue, expectedFormat, CONFIG_GUIDANCE);
    }
    
    /**
     * Creates a standardized error message for template-related issues.
     * 
     * @param templateName the name of the template
     * @param issue description of the issue
     * @return formatted error message with guidance
     */
    public static String formatTemplateError(String templateName, String issue) {
        return String.format("Template '%s': %s. %s", templateName, issue, TEMPLATE_GUIDANCE);
    }
    
    /**
     * Creates a standardized error message for version-related issues.
     * 
     * @param detectedVersion the version that was detected (may be null)
     * @param requiredVersion the version that was required
     * @param operation the operation that failed
     * @return formatted error message with guidance
     */
    public static String formatVersionError(String detectedVersion, String requiredVersion, String operation) {
        if (detectedVersion == null) {
            return String.format("Cannot %s: OpenAPI Generator version could not be detected, required version: %s. %s", 
                operation, requiredVersion, VERSION_GUIDANCE);
        } else {
            return String.format("Cannot %s: detected OpenAPI Generator version %s, required: %s. %s", 
                operation, detectedVersion, requiredVersion, VERSION_GUIDANCE);
        }
    }
    
    /**
     * Creates a standardized InvalidUserDataException with actionable guidance.
     * 
     * @param issue the configuration issue description
     * @param guidance specific guidance for resolving the issue
     * @return formatted InvalidUserDataException
     */
    public static InvalidUserDataException createConfigurationError(String issue, String guidance) {
        return new InvalidUserDataException(issue + ". " + guidance);
    }
    
    /**
     * Creates a standardized InvalidUserDataException with default configuration guidance.
     * 
     * @param issue the configuration issue description
     * @return formatted InvalidUserDataException
     */
    public static InvalidUserDataException createConfigurationError(String issue) {
        return createConfigurationError(issue, CONFIG_GUIDANCE);
    }
    
    /**
     * Creates a standardized GradleException with enhanced context.
     * 
     * @param operation the operation that failed
     * @param cause the underlying cause
     * @param guidance specific guidance for resolution
     * @return formatted GradleException
     */
    public static GradleException createGradleError(String operation, String cause, String guidance) {
        String message = String.format("Failed to %s: %s. %s", operation, cause, guidance);
        return new GradleException(message);
    }
    
    /**
     * Creates a standardized GradleException with default Gradle guidance.
     * 
     * @param operation the operation that failed
     * @param cause the underlying cause
     * @return formatted GradleException
     */
    public static GradleException createGradleError(String operation, String cause) {
        return createGradleError(operation, cause, GRADLE_GUIDANCE);
    }
    
    /**
     * Creates a standardized error message for library processing issues.
     * 
     * @param libraryName the name of the library that failed
     * @param issue description of the issue
     * @return formatted error message with guidance
     */
    public static String formatLibraryError(String libraryName, String issue) {
        return String.format("Library '%s': %s. %s", libraryName, issue, LIBRARY_GUIDANCE);
    }
    
    /**
     * Handles validation with consistent error reporting and context collection.
     * 
     * @param validationName name of the validation being performed
     * @param validationOperation the validation operation to perform
     * @param errorCollector list to collect validation errors
     * @param logger the logger to use
     */
    public static void handleValidation(String validationName, Runnable validationOperation, 
                                      java.util.List<String> errorCollector, Logger logger) {
        try {
            validationOperation.run();
        } catch (Exception e) {
            String errorMessage = validationName + ": " + e.getMessage();
            ContextAwareLogger.warn(logger, "Validation failed: {}", errorMessage);
            errorCollector.add(errorMessage);
        }
    }
    
    /**
     * Validates configuration with error accumulation and final exception throwing.
     * 
     * @param validationResults list of validation error messages
     * @param contextDescription description of what is being validated
     * @param guidance specific guidance for fixing issues
     * @throws InvalidUserDataException if any validation errors exist
     */
    public static void validateOrThrow(java.util.List<String> validationResults, 
                                     String contextDescription, String guidance) {
        if (!validationResults.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append(contextDescription).append(" validation failed:\n");
            
            for (int i = 0; i < validationResults.size(); i++) {
                errorMessage.append(String.format("  %d. %s", i + 1, validationResults.get(i)))
                           .append(System.lineSeparator());
            }
            
            if (guidance != null && !guidance.trim().isEmpty()) {
                errorMessage.append("\n").append(guidance);
            }
            
            throw new InvalidUserDataException(errorMessage.toString());
        }
    }
}