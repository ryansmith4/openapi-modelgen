package com.guidedbyte.openapi.modelgen.util;

import com.guidedbyte.openapi.modelgen.services.LoggingContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Map;

/**
 * Specialized logger for template processing operations in the OpenAPI Model Generator plugin.
 *
 * <p>Provides detailed visibility into template resolution, customization application,
 * and variable expansion with structured logging for debugging and optimization.</p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><strong>Template Resolution Tracking:</strong> Logs source, priority, and fallback information</li>
 *   <li><strong>Customization Application:</strong> Tracks modifications and processing time</li>
 *   <li><strong>Variable Expansion Details:</strong> Monitors variable substitution and recursion</li>
 *   <li><strong>Context-Aware:</strong> Automatically includes spec and template context</li>
 *   <li><strong>Performance Metrics:</strong> Integrates with PerformanceMetrics for timing</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Template resolution logging
 * TemplateProcessingLogger.logTemplateResolution(
 *     "pojo.mustache", TemplateSource.USER_CUSTOMIZATIONS, 2, true);
 *
 * // Customization application tracking
 * TemplateProcessingLogger.logCustomizationApplication(
 *     "pojo.mustache", "replacement", 3, Duration.ofMillis(15));
 *
 * // Variable expansion details
 * Map<String, String> variables = Map.of("copyright", "©2025 GuidedByte", "currentYear", "2025");
 * TemplateProcessingLogger.logVariableExpansion(
 *     "pojo.mustache", variables, 7, true);
 * }</pre>
 *
 * <h2>Log Output Format:</h2>
 * <p>All logs follow a structured format for easy parsing and analysis:</p>
 * <pre>
 * [DEBUG] [TemplateResolver] [pets:pojo.mustache] - TEMPLATE_RESOLUTION: source=user-customizations priority=2 customizations=3 fallback=plugin-customizations
 * [DEBUG] [CustomizationEngine] [pets:pojo.mustache] - CUSTOMIZATION_APPLIED: type=replacement pattern="class {{classname}}" modifications=1 duration=15ms
 * [DEBUG] [VariableExpansion] [pets:pojo.mustache] - VARIABLE_EXPANSION: variables={copyright=©2025 GuidedByte, currentYear=2025} expansions=7 recursive=true
 * </pre>
 *
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public final class TemplateProcessingLogger {

    private static final Logger logger = PluginLoggerFactory.getLogger(TemplateProcessingLogger.class);

    private TemplateProcessingLogger() {
        // Utility class - prevent instantiation
    }

    /**
     * Logs detailed template resolution information.
     *
     * @param templateName the name of the template being resolved
     * @param source the source from which the template was resolved
     * @param priority the resolution priority (higher = more precedence)
     * @param customized whether customizations were applied
     */
    public static void logTemplateResolution(String templateName, TemplateSource source,
                                           int priority, boolean customized) {
        LoggingContext.setTemplate(templateName);
        LoggingContext.setComponent("TemplateResolver");
        try {
            logger.debug("TEMPLATE_RESOLUTION: source={} priority={} customized={} fallback={}",
                StringUtils.toRootLowerCase(source.name()).replace('_', '-'),
                priority,
                customized,
                source.getFallbackSource() != null ?
                    StringUtils.toRootLowerCase(source.getFallbackSource().name()).replace('_', '-') : "none");
        } finally {
            // Keep template context for subsequent operations
        }
    }

    /**
     * Logs customization application details.
     *
     * @param templateName the name of the template being customized
     * @param customizationType the type of customization applied
     * @param modificationsApplied number of modifications made
     * @param processingTime time taken to apply customizations
     */
    public static void logCustomizationApplication(String templateName, String customizationType,
                                                 int modificationsApplied, Duration processingTime) {
        LoggingContext.setTemplate(templateName);
        LoggingContext.setComponent("CustomizationEngine");
        try {
            logger.debug("CUSTOMIZATION_APPLIED: type={} modifications={} duration={}ms",
                customizationType,
                modificationsApplied,
                processingTime.toMillis());

            // Also log as performance metric
            Map<String, Object> context = Map.of(
                "template", templateName,
                "type", customizationType,
                "modifications", modificationsApplied
            );
            PerformanceMetrics.logTiming("template_customization", processingTime, context);
        } finally {
            // Keep template context for subsequent operations
        }
    }

    /**
     * Logs variable expansion details.
     *
     * @param templateName the name of the template being processed
     * @param variables the variables being expanded
     * @param expansionCount total number of variable expansions performed
     * @param hasRecursion whether recursive expansion occurred
     */
    public static void logVariableExpansion(String templateName, Map<String, String> variables,
                                          int expansionCount, boolean hasRecursion) {
        LoggingContext.setTemplate(templateName);
        LoggingContext.setComponent("VariableExpansion");
        try {
            StringBuilder variableInfo = new StringBuilder();
            variableInfo.append("{");
            variables.forEach((key, value) -> {
                if (variableInfo.length() > 1) {
                    variableInfo.append(", ");
                }
                variableInfo.append(key).append("=").append(value);
            });
            variableInfo.append("}");

            logger.debug("VARIABLE_EXPANSION: variables={} expansions={} recursive={}",
                variableInfo.toString(),
                expansionCount,
                hasRecursion);

            // Log detailed variable information if debug is enabled and we have many variables
            if (variables.size() > 5) {
                logger.debug("VARIABLE_DETAILS: Found {} variables for template expansion", variables.size());
                variables.forEach((key, value) ->
                    logger.debug("  {} -> {}", key, value.length() > 50 ? value.substring(0, 50) + "..." : value));
            }
        } finally {
            // Keep template context for subsequent operations
        }
    }

    /**
     * Logs template processing start with context setup.
     *
     * @param templateName the template being processed
     * @param templateSize size of the template in characters
     * @param expectedVariables number of variables expected to be expanded
     */
    public static void logTemplateProcessingStart(String templateName, int templateSize, int expectedVariables) {
        LoggingContext.setTemplate(templateName);
        LoggingContext.setComponent("TemplateProcessor");
        try {
            logger.debug("TEMPLATE_PROCESSING_START: size={}chars expected_variables={}",
                templateSize, expectedVariables);
        } finally {
            // Keep template context for subsequent operations
        }
    }

    /**
     * Logs template processing completion with performance summary.
     *
     * @param templateName the template that was processed
     * @param processingTime total processing time
     * @param outputSize size of the generated output
     * @param variablesExpanded number of variables that were expanded
     */
    public static void logTemplateProcessingComplete(String templateName, Duration processingTime,
                                                   int outputSize, int variablesExpanded) {
        LoggingContext.setTemplate(templateName);
        LoggingContext.setComponent("TemplateProcessor");
        try {
            logger.debug("TEMPLATE_PROCESSING_COMPLETE: duration={}ms output_size={}chars variables_expanded={}",
                processingTime.toMillis(), outputSize, variablesExpanded);

            // Log as performance metric
            Map<String, Object> context = Map.of(
                "template", templateName,
                "output_size", outputSize,
                "variables_expanded", variablesExpanded
            );
            PerformanceMetrics.logTiming("template_processing", processingTime, context);
        } finally {
            LoggingContext.clearTemplate();
        }
    }

    /**
     * Logs template cache operations.
     *
     * @param templateName the template being cached/retrieved
     * @param operation the cache operation (hit, miss, store, evict)
     * @param cacheKey the cache key used
     * @param accessTime time taken for the cache operation
     */
    public static void logTemplateCacheOperation(String templateName, CacheOperation operation,
                                               String cacheKey, Duration accessTime) {
        LoggingContext.setTemplate(templateName);
        LoggingContext.setComponent("TemplateCache");
        try {
            logger.debug("TEMPLATE_CACHE_{}: key={} access_time={}ms",
                operation.name(), cacheKey, accessTime.toMillis());

            // Update cache performance metrics
            if (operation == CacheOperation.HIT || operation == CacheOperation.MISS) {
                int hits = operation == CacheOperation.HIT ? 1 : 0;
                int misses = operation == CacheOperation.MISS ? 1 : 0;
                PerformanceMetrics.logCachePerformance("template-cache", hits, misses, accessTime);
            }
        } finally {
            // Keep template context for subsequent operations
        }
    }

    /**
     * Logs template validation results.
     *
     * @param templateName the template that was validated
     * @param validationTime time taken for validation
     * @param issues number of validation issues found
     * @param warnings number of warnings generated
     */
    public static void logTemplateValidation(String templateName, Duration validationTime,
                                           int issues, int warnings) {
        LoggingContext.setTemplate(templateName);
        LoggingContext.setComponent("TemplateValidator");
        try {
            String level = issues > 0 ? "ERROR" : warnings > 0 ? "WARN" : "SUCCESS";
            logger.debug("TEMPLATE_VALIDATION_{}: duration={}ms issues={} warnings={}",
                level, validationTime.toMillis(), issues, warnings);

            // Log performance metric
            Map<String, Object> context = Map.of(
                "template", templateName,
                "issues", issues,
                "warnings", warnings
            );
            PerformanceMetrics.logTiming("template_validation", validationTime, context);
        } finally {
            // Keep template context for subsequent operations
        }
    }

    /**
     * Template source enumeration for tracking resolution paths.
     */
    public enum TemplateSource {
        USER_TEMPLATES("User Templates", null),
        USER_CUSTOMIZATIONS("User YAML Customizations", USER_TEMPLATES),
        PLUGIN_CUSTOMIZATIONS("Plugin YAML Customizations", USER_CUSTOMIZATIONS),
        OPENAPI_DEFAULTS("OpenAPI Generator Defaults", PLUGIN_CUSTOMIZATIONS);

        private final String displayName;
        private final TemplateSource fallbackSource;

        TemplateSource(String displayName, TemplateSource fallbackSource) {
            this.displayName = displayName;
            this.fallbackSource = fallbackSource;
        }

        public String getDisplayName() {
            return displayName;
        }

        public TemplateSource getFallbackSource() {
            return fallbackSource;
        }
    }

    /**
     * Cache operation enumeration for tracking cache behavior.
     */
    public enum CacheOperation {
        HIT, MISS, STORE, EVICT, CLEAR
    }
}