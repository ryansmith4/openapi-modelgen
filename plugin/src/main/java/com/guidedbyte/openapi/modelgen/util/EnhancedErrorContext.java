package com.guidedbyte.openapi.modelgen.util;

import com.guidedbyte.openapi.modelgen.services.LoggingContext;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced error context and logging utility for the OpenAPI Model Generator plugin.
 *
 * <p>Provides comprehensive error information with context, troubleshooting suggestions,
 * and structured error data for faster problem resolution and better user experience.</p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><strong>Rich Error Context:</strong> Configuration details, file paths, permissions</li>
 *   <li><strong>Automatic Suggestions:</strong> Common solutions based on error patterns</li>
 *   <li><strong>Validation Summaries:</strong> Consolidated error reports with guidance</li>
 *   <li><strong>Template Error Details:</strong> Variable context, template state, processing info</li>
 *   <li><strong>Structured Logging:</strong> Easy parsing for automated analysis</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Configuration error with context
 * Map<String, Object> context = Map.of(
 *     "file", "pets.yaml",
 *     "project_dir", "/app",
 *     "resolved_path", "/app/specs/pets.yaml",
 *     "parent_exists", true,
 *     "permissions", "readable"
 * );
 * EnhancedErrorContext.logConfigurationError("inputSpec", new FileNotFoundException(), context);
 *
 * // Template processing error
 * TemplateContext templateCtx = new TemplateContext("pojo.mustache")
 *     .withVariables(Map.of("currentYear", "2025"))
 *     .withRecursiveDepth(3)
 *     .withTemplateSize(2847);
 * EnhancedErrorContext.logTemplateError("pojo.mustache", "variable_expansion",
 *     new IllegalArgumentException("Invalid variable"), templateCtx);
 *
 * // Validation error summary
 * List<String> errors = List.of("Invalid YAML syntax", "Missing required field", "Invalid path");
 * Map<String, Object> debugInfo = Map.of("check_file_paths", true, "validate_yaml_syntax", true);
 * EnhancedErrorContext.logValidationError("spec_validation", errors, debugInfo);
 * }</pre>
 *
 * <h2>Error Output Format:</h2>
 * <pre>
 * [ERROR] [ConfigurationValidator] [pets] - CONFIG_ERROR: component=inputSpec error=FileNotFoundException context={file=pets.yaml, project_dir=/app, resolved_path=/app/specs/pets.yaml, parent_exists=true, permissions=readable}
 * [ERROR] [TemplateProcessor] [pets:pojo.mustache] - TEMPLATE_ERROR: operation=variable_expansion error=IllegalArgumentException context={variables=5, recursive_depth=3, failed_variable=currentYear, template_size=2847}
 * [ERROR] [ValidationSummary] - VALIDATION_FAILED: type=spec_validation errors=3 suggestions={check_file_paths=true, validate_yaml_syntax=true, verify_permissions=false}
 * </pre>
 *
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public final class EnhancedErrorContext {

    private static final Logger logger = PluginLoggerFactory.getLogger(EnhancedErrorContext.class);

    private EnhancedErrorContext() {
        // Utility class - prevent instantiation
    }

    /**
     * Logs configuration errors with comprehensive context and troubleshooting suggestions.
     *
     * @param component the configuration component that failed
     * @param error the exception that occurred
     * @param context additional context information for debugging
     */
    public static void logConfigurationError(String component, Exception error, Map<String, Object> context) {
        LoggingContext.setComponent("ConfigurationValidator");
        try {
            // Enhance context with automatic debugging information
            Map<String, Object> enrichedContext = new HashMap<>(context != null ? context : new HashMap<>());
            enrichContext(enrichedContext, error);

            // Log structured error
            logger.error("CONFIG_ERROR: component={} error={} context={}",
                component, error.getClass().getSimpleName(), formatContext(enrichedContext));

            // Log the actual exception details to rich file
            logger.error("Exception details for {}: {}", component, error.getMessage(), error);

            // Generate and log troubleshooting suggestions
            List<String> suggestions = generateConfigurationSuggestions(component, error, enrichedContext);
            if (!suggestions.isEmpty()) {
                logger.error("TROUBLESHOOTING_SUGGESTIONS for {}: {}", component, suggestions);
                // Also log user-friendly suggestions to console
                suggestions.forEach(suggestion -> logger.error("  → {}", suggestion));
            }
        } finally {
            // Keep component context for related operations
        }
    }

    /**
     * Logs template processing errors with detailed template context.
     *
     * @param templateName the template being processed
     * @param operation the operation that failed
     * @param error the exception that occurred
     * @param templateContext template-specific context information
     */
    public static void logTemplateError(String templateName, String operation, Exception error, TemplateContext templateContext) {
        LoggingContext.setTemplate(templateName);
        LoggingContext.setComponent("TemplateProcessor");
        try {
            // Build context map from template context
            Map<String, Object> context = new HashMap<>();
            if (templateContext != null) {
                context.put("variables", templateContext.getVariables().size());
                context.put("recursive_depth", templateContext.getRecursiveDepth());
                context.put("template_size", templateContext.getTemplateSize());
                if (templateContext.getFailedVariable() != null) {
                    context.put("failed_variable", templateContext.getFailedVariable());
                }
                if (templateContext.getCurrentPhase() != null) {
                    context.put("current_phase", templateContext.getCurrentPhase());
                }
            }

            // Log structured error
            logger.error("TEMPLATE_ERROR: operation={} error={} context={}",
                operation, error.getClass().getSimpleName(), formatContext(context));

            // Log detailed exception information
            logger.error("Template processing failed for {} during {}: {}", templateName, operation, error.getMessage(), error);

            // Generate template-specific suggestions
            List<String> suggestions = generateTemplateSuggestions(operation, error, templateContext);
            if (!suggestions.isEmpty()) {
                logger.error("TEMPLATE_TROUBLESHOOTING: suggestions={}", suggestions);
                suggestions.forEach(suggestion -> logger.error("  → {}", suggestion));
            }

            // Log variable context if available and relevant
            if (templateContext != null && !templateContext.getVariables().isEmpty() && isVariableRelatedError(error)) {
                logger.error("VARIABLE_CONTEXT: available_variables={}", templateContext.getVariables().keySet());
                templateContext.getVariables().forEach((key, value) -> {
                    String displayValue = value.length() > 100 ? value.substring(0, 100) + "..." : value;
                    logger.error("  {} = {}", key, displayValue);
                });
            }
        } finally {
            // Keep template context for subsequent operations
        }
    }

    /**
     * Logs validation errors with consolidated summary and suggestions.
     *
     * @param validationType the type of validation that failed
     * @param errors list of validation errors
     * @param debugInfo additional debug information for suggestions
     */
    public static void logValidationError(String validationType, List<String> errors, Map<String, Object> debugInfo) {
        LoggingContext.setComponent("ValidationSummary");
        try {
            // Generate suggestions based on error patterns
            Map<String, Object> suggestions = generateValidationSuggestions(validationType, errors, debugInfo);

            // Log validation summary
            logger.error("VALIDATION_FAILED: type={} errors={} suggestions={}",
                validationType, errors.size(), formatContext(suggestions));

            // Log individual errors with context
            logger.error("=== Validation Errors for {} ===", validationType);
            for (int i = 0; i < errors.size(); i++) {
                logger.error("  {}. {}", i + 1, errors.get(i));
            }

            // Log suggestions in user-friendly format
            if (!suggestions.isEmpty()) {
                logger.error("=== Suggested Solutions ===");
                suggestions.forEach((key, value) -> {
                    if (Boolean.TRUE.equals(value)) {
                        logger.error("  → {}", formatSuggestionKey(key));
                    }
                });
            }

            // Performance impact: log validation errors as metrics
            Map<String, Object> validationContext = Map.of(
                "validation_type", validationType,
                "error_count", errors.size(),
                "has_suggestions", !suggestions.isEmpty()
            );
            PerformanceMetrics.logTiming("validation_error_processing",
                java.time.Duration.ofMillis(System.currentTimeMillis() % 1000), validationContext);
        } finally {
            LoggingContext.clear();
        }
    }

    /**
     * Logs file operation errors with path analysis and permission checks.
     *
     * @param operation the file operation that failed
     * @param filePath the file path involved
     * @param error the exception that occurred
     */
    public static void logFileOperationError(String operation, String filePath, Exception error) {
        LoggingContext.setComponent("FileOperations");
        try {
            Map<String, Object> context = analyzeFilePath(filePath);
            context.put("operation", operation);

            logger.error("FILE_ERROR: operation={} error={} path={} context={}",
                operation, error.getClass().getSimpleName(), filePath, formatContext(context));

            // Generate file-specific suggestions
            List<String> suggestions = generateFileOperationSuggestions(operation, filePath, error, context);
            if (!suggestions.isEmpty()) {
                logger.error("FILE_TROUBLESHOOTING: suggestions={}", suggestions);
                suggestions.forEach(suggestion -> logger.error("  → {}", suggestion));
            }
        } finally {
            LoggingContext.clear();
        }
    }

    /**
     * Logs build errors with comprehensive build state context.
     *
     * @param phase the build phase where the error occurred
     * @param error the exception that occurred
     * @param buildContext current build state information
     */
    public static void logBuildError(String phase, Exception error, Map<String, Object> buildContext) {
        LoggingContext.setComponent("BuildError");
        try {
            Map<String, Object> enrichedContext = new HashMap<>(buildContext != null ? buildContext : new HashMap<>());
            enrichedContext.put("phase", phase);
            enrichedContext.put("build_progress", BuildProgressTracker.getBuildProgress());

            logger.error("BUILD_ERROR: phase={} error={} context={}",
                phase, error.getClass().getSimpleName(), formatContext(enrichedContext));

            // Log build state for debugging
            logger.error("Build failed during {} phase: {}", phase, error.getMessage(), error);

            // Generate build-specific suggestions
            List<String> suggestions = generateBuildSuggestions(phase, error, enrichedContext);
            if (!suggestions.isEmpty()) {
                logger.error("BUILD_TROUBLESHOOTING: suggestions={}", suggestions);
                suggestions.forEach(suggestion -> logger.error("  → {}", suggestion));
            }
        } finally {
            LoggingContext.clear();
        }
    }

    // Helper methods for generating context-specific suggestions

    private static List<String> generateConfigurationSuggestions(String component, Exception error, Map<String, Object> context) {
        List<String> suggestions = new ArrayList<>();

        if ("inputSpec".equals(component)) {
            if (error instanceof java.io.FileNotFoundException) {
                suggestions.add("Verify the OpenAPI specification file path exists");
                suggestions.add("Check if the file path is relative to the project root");
                suggestions.add("Ensure the file has proper read permissions");
            }
        }

        // Add suggestions based on error type
        if (error.getMessage() != null) {
            String message = error.getMessage().toLowerCase();
            if (message.contains("permission") || message.contains("access")) {
                suggestions.add("Check file/directory permissions");
                suggestions.add("Verify the build process has necessary access rights");
            }
            if (message.contains("yaml") || message.contains("parse")) {
                suggestions.add("Validate YAML syntax using a YAML validator");
                suggestions.add("Check for proper indentation and special characters");
            }
        }

        return suggestions;
    }

    private static List<String> generateTemplateSuggestions(String operation, Exception error, TemplateContext context) {
        List<String> suggestions = new ArrayList<>();

        if ("variable_expansion".equals(operation)) {
            suggestions.add("Check template variable syntax (use {{variableName}})");
            suggestions.add("Verify all referenced variables are defined in templateVariables");
            if (context != null && context.getRecursiveDepth() > 5) {
                suggestions.add("Review template variables for circular references");
            }
        }

        if ("template_processing".equals(operation)) {
            suggestions.add("Verify template file syntax is valid Mustache");
            suggestions.add("Check for unescaped special characters in template");
        }

        if (error.getMessage() != null && error.getMessage().contains("Stack")) {
            suggestions.add("Reduce template complexity or split into smaller templates");
            suggestions.add("Check for infinite recursion in template variables");
        }

        return suggestions;
    }

    private static Map<String, Object> generateValidationSuggestions(String validationType, List<String> errors, Map<String, Object> debugInfo) {
        Map<String, Object> suggestions = new HashMap<>();

        // Analyze error patterns
        boolean hasFilePathErrors = errors.stream().anyMatch(error -> error.toLowerCase().contains("file") || error.toLowerCase().contains("path"));
        boolean hasYamlErrors = errors.stream().anyMatch(error -> error.toLowerCase().contains("yaml") || error.toLowerCase().contains("syntax"));
        boolean hasPermissionErrors = errors.stream().anyMatch(error -> error.toLowerCase().contains("permission") || error.toLowerCase().contains("access"));

        suggestions.put("check_file_paths", hasFilePathErrors);
        suggestions.put("validate_yaml_syntax", hasYamlErrors);
        suggestions.put("verify_permissions", hasPermissionErrors);
        suggestions.put("review_configuration", errors.size() > 3);
        suggestions.put("check_plugin_version", errors.stream().anyMatch(error -> error.contains("version") || error.contains("compatibility")));

        return suggestions;
    }

    private static List<String> generateFileOperationSuggestions(String operation, String filePath, Exception error, Map<String, Object> context) {
        List<String> suggestions = new ArrayList<>();

        if ("read".equals(operation)) {
            suggestions.add("Verify the file exists and is readable");
            suggestions.add("Check file permissions and ownership");
        } else if ("write".equals(operation)) {
            suggestions.add("Ensure the target directory exists and is writable");
            suggestions.add("Check available disk space");
        }

        // Check if parent directory exists
        if (Boolean.FALSE.equals(context.get("parent_exists"))) {
            suggestions.add("Create the parent directory structure");
        }

        // Check path format
        if (filePath.contains("\\") && !System.getProperty("os.name").toLowerCase().contains("windows")) {
            suggestions.add("Use forward slashes (/) in file paths for cross-platform compatibility");
        }

        return suggestions;
    }

    private static List<String> generateBuildSuggestions(String phase, Exception error, Map<String, Object> context) {
        List<String> suggestions = new ArrayList<>();

        suggestions.add(String.format("Review the %s phase configuration", phase));
        suggestions.add("Check the plugin debug logs for more details");

        if ("template_extraction".equals(phase)) {
            suggestions.add("Verify template directories and permissions");
            suggestions.add("Check if custom templates are properly formatted");
        } else if ("code_generation".equals(phase)) {
            suggestions.add("Ensure output directory is writable");
            suggestions.add("Verify OpenAPI specification is valid");
        }

        return suggestions;
    }

    // Utility methods

    private static void enrichContext(Map<String, Object> context, Exception error) {
        context.put("error_class", error.getClass().getSimpleName());
        context.put("error_message", error.getMessage());
        context.put("thread", Thread.currentThread().getName());
        context.put("timestamp", System.currentTimeMillis());
    }

    private static Map<String, Object> analyzeFilePath(String filePath) {
        Map<String, Object> analysis = new HashMap<>();

        File file = new File(filePath);
        analysis.put("absolute_path", file.getAbsolutePath());
        analysis.put("exists", file.exists());
        analysis.put("is_file", file.isFile());
        analysis.put("is_directory", file.isDirectory());
        analysis.put("can_read", file.canRead());
        analysis.put("can_write", file.canWrite());

        File parent = file.getParentFile();
        analysis.put("parent_exists", parent != null && parent.exists());
        analysis.put("parent_writable", parent != null && parent.canWrite());

        return analysis;
    }

    private static String formatContext(Map<String, Object> context) {
        StringBuilder sb = new StringBuilder("{");
        context.forEach((key, value) -> {
            if (sb.length() > 1) sb.append(", ");
            sb.append(key).append("=").append(value);
        });
        sb.append("}");
        return sb.toString();
    }

    private static String formatSuggestionKey(String key) {
        return key.replace('_', ' ').substring(0, 1).toUpperCase() + key.replace('_', ' ').substring(1);
    }

    private static boolean isVariableRelatedError(Exception error) {
        String message = error.getMessage();
        if (message == null) return false;
        return message.toLowerCase().contains("variable") ||
               message.toLowerCase().contains("expansion") ||
               message.toLowerCase().contains("substitution") ||
               message.toLowerCase().contains("mustache");
    }

    /**
     * Template context container for template error logging.
     */
    public static class TemplateContext {
        private final String templateName;
        private Map<String, String> variables = new HashMap<>();
        private int recursiveDepth = 0;
        private int templateSize = 0;
        private String failedVariable;
        private String currentPhase;

        public TemplateContext(String templateName) {
            this.templateName = templateName;
        }

        public TemplateContext withVariables(Map<String, String> variables) {
            this.variables = new HashMap<>(variables);
            return this;
        }

        public TemplateContext withRecursiveDepth(int depth) {
            this.recursiveDepth = depth;
            return this;
        }

        public TemplateContext withTemplateSize(int size) {
            this.templateSize = size;
            return this;
        }

        public TemplateContext withFailedVariable(String variable) {
            this.failedVariable = variable;
            return this;
        }

        public TemplateContext withCurrentPhase(String phase) {
            this.currentPhase = phase;
            return this;
        }

        // Getters
        public String getTemplateName() { return templateName; }
        public Map<String, String> getVariables() { return variables; }
        public int getRecursiveDepth() { return recursiveDepth; }
        public int getTemplateSize() { return templateSize; }
        public String getFailedVariable() { return failedVariable; }
        public String getCurrentPhase() { return currentPhase; }
    }
}