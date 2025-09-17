package com.guidedbyte.openapi.modelgen.util;

import com.guidedbyte.openapi.modelgen.customization.CustomizationConfig;
import com.guidedbyte.openapi.modelgen.customization.Insertion;
import com.guidedbyte.openapi.modelgen.customization.Replacement;
import com.guidedbyte.openapi.modelgen.customization.ConditionSet;
import com.guidedbyte.openapi.modelgen.services.EvaluationContext;
import com.guidedbyte.openapi.modelgen.services.LoggingContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comprehensive diagnostic utilities for template customization development and troubleshooting.
 *
 * <p>Provides detailed logging and analysis capabilities to help users debug their template
 * customizations, understand why patterns don't match, diagnose condition failures, and
 * trace the complete customization workflow.</p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><strong>Pattern Matching Analysis:</strong> Shows why regex patterns succeed or fail</li>
 *   <li><strong>Condition Evaluation Tracing:</strong> Detailed condition evaluation logging</li>
 *   <li><strong>Template Diff Generation:</strong> Before/after comparisons with highlighted changes</li>
 *   <li><strong>Variable Context Analysis:</strong> Available variables and their values</li>
 *   <li><strong>Line-by-Line Context:</strong> Shows exact template locations being modified</li>
 *   <li><strong>Troubleshooting Suggestions:</strong> Automated suggestions for common issues</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Pattern matching analysis
 * TemplateCustomizationDiagnostics.analyzePatternMatch(
 *     "class {{classname}}", template, "replacement", true);
 *
 * // Template diff for before/after comparison
 * TemplateCustomizationDiagnostics.logTemplateDiff(
 *     "pojo.mustache", originalTemplate, modifiedTemplate, "Applied replacement #3");
 *
 * // Condition evaluation tracing
 * TemplateCustomizationDiagnostics.traceConditionEvaluation(
 *     conditions, context, "Global conditions");
 *
 * // Complete customization summary
 * TemplateCustomizationDiagnostics.logCustomizationSummary(
 *     config, originalTemplate, finalTemplate, appliedOperations);
 * }</pre>
 *
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public final class TemplateCustomizationDiagnostics {

    private static final PluginLogger logger = (PluginLogger) PluginLoggerFactory.getLogger(TemplateCustomizationDiagnostics.class);

    private static final int MAX_CONTEXT_LINES = 3;
    private static final int MAX_PATTERN_PREVIEW = 80;
    private static final int MAX_DIFF_CONTEXT = 5;

    private TemplateCustomizationDiagnostics() {
        // Utility class - prevent instantiation
    }

    /**
     * Analyzes pattern matching for replacements and insertions, providing detailed
     * feedback on why patterns succeed or fail to match.
     *
     * @param pattern the regex or string pattern being matched
     * @param template the template content being searched
     * @param operationType the type of operation (replacement, insertion, etc.)
     * @param succeeded whether the pattern match succeeded
     */
    public static void analyzePatternMatch(String pattern, String template, String operationType, boolean succeeded) {
        LoggingContext.setComponent("TemplateCustomizationDiagnostics");
        try {
            if (succeeded) {
                // Success at DEBUG level - shows what matched
                logger.debug("PATTERN_MATCH_SUCCESS: operation={} pattern='{}' found_in_template=true",
                    operationType, truncatePattern(pattern));

                // Detailed match context at TRACE level
                logger.ifTrace(() -> showPatternMatches(pattern, template, operationType));
            } else {
                // Failures at DEBUG level - important for troubleshooting
                logger.debug("PATTERN_MATCH_FAILURE: operation={} pattern='{}' found_in_template=false",
                    operationType, truncatePattern(pattern));

                // Troubleshooting suggestions at TRACE level
                logger.ifTrace(() -> providePatterMatchingSuggestions(pattern, template, operationType));
            }
        } finally {
            // Keep component context for related operations
        }
    }

    /**
     * Logs a detailed diff between original and modified template content.
     *
     * @param templateName the name of the template being modified
     * @param originalTemplate the original template content
     * @param modifiedTemplate the modified template content
     * @param operationDescription description of the operation that caused the change
     */
    public static void logTemplateDiff(String templateName, String originalTemplate,
                                     String modifiedTemplate, String operationDescription) {
        LoggingContext.setTemplate(templateName);
        LoggingContext.setComponent("TemplateDiff");
        try {
            if (originalTemplate.equals(modifiedTemplate)) {
                logger.debug("TEMPLATE_DIFF: operation='{}' changes=none template_unchanged=true",
                    operationDescription);
                return;
            }

            // Calculate diff statistics
            int lengthDiff = modifiedTemplate.length() - originalTemplate.length();
            String[] originalLines = originalTemplate.split("\n");
            String[] modifiedLines = modifiedTemplate.split("\n");
            int lineDiff = modifiedLines.length - originalLines.length;

            logger.debug("TEMPLATE_DIFF: operation='{}' length_change={} line_change={} template_changed=true",
                operationDescription, lengthDiff, lineDiff);

            // Show detailed diff for significant changes
            if (Math.abs(lengthDiff) > 20 || Math.abs(lineDiff) > 2) {
                generateDetailedDiff(originalLines, modifiedLines, operationDescription);
            }

            // Show first and last few lines for context
            logTemplateContextSample(modifiedTemplate, operationDescription);

        } finally {
            // Keep template context for subsequent operations
        }
    }

    /**
     * Traces condition evaluation for troubleshooting conditional customizations.
     *
     * @param conditions the condition set being evaluated
     * @param context the evaluation context
     * @param conditionDescription description of what these conditions control
     */
    public static void traceConditionEvaluation(ConditionSet conditions, EvaluationContext context,
                                              String conditionDescription) {
        LoggingContext.setComponent("ConditionEvaluator");
        try {
            if (conditions == null) {
                logger.debug("CONDITION_EVALUATION: description='{}' conditions=none result=true",
                    conditionDescription);
                return;
            }

            logger.debug("CONDITION_EVALUATION_START: description='{}' condition_type=ConditionSet",
                conditionDescription);

            // Trace key condition properties
            if (conditions.getGeneratorVersion() != null) {
                logger.debug("CONDITION_CHECK: type=generatorVersion constraint='{}' context_version='{}'",
                    conditions.getGeneratorVersion(), context != null ? context.getGeneratorVersion() : "unknown");
            }

            if (conditions.getTemplateContains() != null) {
                boolean templateMatches = context != null && context.getTemplateContent() != null &&
                    context.getTemplateContent().contains(conditions.getTemplateContains());
                logger.debug("CONDITION_CHECK: type=templateContains pattern='{}' result={}",
                    conditions.getTemplateContains(), templateMatches);
            }

            if (conditions.getProjectProperty() != null) {
                Object actualValue = getContextValue(context, conditions.getProjectProperty());
                logger.debug("CONDITION_CHECK: type=projectProperty key='{}' actual='{}'",
                    conditions.getProjectProperty(), actualValue);
            }

        } finally {
            // Keep component context for related operations
        }
    }

    /**
     * Logs comprehensive customization summary with performance and change metrics.
     *
     * @param config the customization configuration applied
     * @param originalTemplate the original template content
     * @param finalTemplate the final template content after all customizations
     * @param appliedOperations list of operations that were actually applied
     */
    public static void logCustomizationSummary(CustomizationConfig config, String originalTemplate,
                                             String finalTemplate, List<String> appliedOperations) {
        LoggingContext.setComponent("CustomizationSummary");
        try {
            int totalOperations = 0;
            if (config.getReplacements() != null) totalOperations += config.getReplacements().size();
            if (config.getInsertions() != null) totalOperations += config.getInsertions().size();
            if (config.getSmartReplacements() != null) totalOperations += config.getSmartReplacements().size();
            if (config.getSmartInsertions() != null) totalOperations += config.getSmartInsertions().size();

            int appliedCount = appliedOperations != null ? appliedOperations.size() : 0;
            int skippedCount = totalOperations - appliedCount;

            int lengthChange = finalTemplate.length() - originalTemplate.length();
            String[] originalLines = originalTemplate.split("\n");
            String[] finalLines = finalTemplate.split("\n");
            int lineChange = finalLines.length - originalLines.length;

            // Summary at INFO level - shows overall results
            logger.info("CUSTOMIZATION_SUMMARY: total_operations={} applied={} skipped={} " +
                       "length_change={} line_change={} template_modified={}",
                totalOperations, appliedCount, skippedCount, lengthChange, lineChange,
                !originalTemplate.equals(finalTemplate));

            // Applied operations details at DEBUG level
            if (appliedOperations != null && !appliedOperations.isEmpty()) {
                logger.debug("APPLIED_OPERATIONS: operations={}", appliedOperations);
            }

            // Warnings always visible (unless QUIET)
            if (skippedCount > appliedCount && totalOperations > 2) {
                logger.warn("CUSTOMIZATION_WARNING: More operations were skipped ({}) than applied ({}). " +
                           "Consider reviewing conditions and patterns in your customization file.",
                    skippedCount, appliedCount);

                // Optimization suggestions at TRACE level
                logger.ifTrace(() -> provideCustomizationOptimizationSuggestions(config, appliedCount, skippedCount));
            }

        } finally {
            LoggingContext.clear();
        }
    }

    /**
     * Analyzes and logs available template variables for customization development.
     *
     * @param context the evaluation context containing variables
     * @param templateName the name of the template being customized
     */
    public static void analyzeTemplateVariables(EvaluationContext context, String templateName) {
        LoggingContext.setTemplate(templateName);
        LoggingContext.setComponent("VariableAnalysis");
        try {
            if (context == null) {
                logger.debug("VARIABLE_ANALYSIS: context=null available_variables=0");
                return;
            }

            Map<String, String> projectProps = context.getProjectProperties();
            Map<String, String> envVars = context.getEnvironmentVariables();

            int projectPropCount = projectProps != null ? projectProps.size() : 0;
            int envVarCount = envVars != null ? envVars.size() : 0;
            int totalVars = projectPropCount + envVarCount;

            logger.debug("VARIABLE_ANALYSIS: total_variables={} project_properties={} environment_variables={}",
                totalVars, projectPropCount, envVarCount);

            // Log commonly used variables for reference
            if (projectProps != null && !projectProps.isEmpty()) {
                logger.debug("PROJECT_PROPERTIES: {}",
                    limitMapSize(projectProps, 10, "Additional project properties available"));
            }

            // Log environment variables (with truncated values for large content)
            if (envVars != null && !envVars.isEmpty()) {
                logger.debug("ENVIRONMENT_VARIABLES: {}",
                    limitMapSize(envVars, 10, "Additional environment variables available"));
            }

            // Suggest common variables if none are available
            if (totalVars == 0) {
                logger.debug("VARIABLE_SUGGESTION: No variables available. Common variables include: " +
                           "generator, packageName, classname, description, currentYear");
            }

        } finally {
            // Keep template context for subsequent operations
        }
    }

    /**
     * Provides detailed line-by-line analysis of where template modifications occurred.
     *
     * @param templateName the name of the template
     * @param beforeTemplate template content before modification
     * @param afterTemplate template content after modification
     * @param lineNumber approximate line number where change occurred (0-based)
     * @param operationDescription description of the operation
     */
    public static void logLineContext(String templateName, String beforeTemplate, String afterTemplate,
                                    int lineNumber, String operationDescription) {
        LoggingContext.setTemplate(templateName);
        LoggingContext.setComponent("LineContext");
        try {
            String[] beforeLines = beforeTemplate.split("\n");
            String[] afterLines = afterTemplate.split("\n");

            // Ensure line number is valid
            int contextLine = Math.max(0, Math.min(lineNumber, beforeLines.length - 1));

            logger.debug("LINE_CONTEXT: operation='{}' line_number={} total_lines_before={} total_lines_after={}",
                operationDescription, contextLine + 1, beforeLines.length, afterLines.length);

            // Show context around the change
            int startLine = Math.max(0, contextLine - MAX_CONTEXT_LINES);
            int endLine = Math.min(beforeLines.length - 1, contextLine + MAX_CONTEXT_LINES);

            logger.debug("TEMPLATE_CONTEXT: Showing lines {} to {} around modification:",
                startLine + 1, endLine + 1);

            for (int i = startLine; i <= endLine; i++) {
                String marker = (i == contextLine) ? " >> " : "    ";
                String line = i < beforeLines.length ? beforeLines[i] : "";
                logger.debug("{}Line {}: {}", marker, i + 1, truncateLine(line, 120));
            }

        } finally {
            // Keep template context for subsequent operations
        }
    }

    // Helper methods

    private static void showPatternMatches(String pattern, String template, String operationType) {
        try {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(template);
            int matchCount = 0;

            while (matcher.find() && matchCount < 5) { // Limit to first 5 matches
                matchCount++;
                int lineNumber = getLineNumber(template, matcher.start());
                String matchedText = matcher.group();

                logger.trace("PATTERN_MATCH_{}: match_number={} line={} matched_text='{}'",
                    matchCount, matchCount, lineNumber, truncatePattern(matchedText));
            }

            if (matchCount > 0) {
                logger.debug("PATTERN_MATCHES_TOTAL: pattern='{}' total_matches={}",
                    truncatePattern(pattern), matchCount);
            }
        } catch (Exception e) {
            logger.debug("PATTERN_ANALYSIS_ERROR: pattern='{}' error='{}'",
                truncatePattern(pattern), e.getMessage());
        }
    }

    private static void providePatterMatchingSuggestions(String pattern, String template, String operationType) {
        logger.trace("TROUBLESHOOTING_SUGGESTIONS for pattern '{}' in {}:",
            truncatePattern(pattern), operationType);

        // Check for common issues
        if (pattern.contains("{{") && !template.contains("{{")) {
            logger.trace("  → Template doesn't contain Mustache variables. Check if this is the correct template.");
        }

        if (pattern.contains("\\") && !pattern.contains("\\\\")) {
            logger.trace("  → Pattern contains backslashes. In YAML, use double backslashes (\\\\\\\\) for literal backslashes.");
        }

        if (pattern.contains("class ") && !template.contains("class ")) {
            logger.trace("  → Pattern looks for 'class ' but template doesn't contain class declarations. Check template type.");
        }

        // Show similar content for fuzzy matching suggestions
        String[] templateLines = template.split("\n");
        String firstPatternWord = pattern.split("\\s+")[0].replaceAll("[^a-zA-Z]", "");

        if (firstPatternWord.length() > 3) {
            for (int i = 0; i < Math.min(templateLines.length, 20); i++) {
                if (StringUtils.toRootLowerCase(templateLines[i]).contains(StringUtils.toRootLowerCase(firstPatternWord))) {
                    logger.trace("  → Similar content found on line {}: '{}'",
                        i + 1, truncateLine(templateLines[i].trim(), 60));
                    break;
                }
            }
        }
    }


    private static void provideCustomizationOptimizationSuggestions(CustomizationConfig config, int applied, int skipped) {
        logger.debug("OPTIMIZATION_SUGGESTIONS: applied={} skipped={}", applied, skipped);
        logger.debug("  → Review condition expressions - many operations were skipped due to unmet conditions");
        logger.debug("  → Verify regex patterns are matching expected template content");
        logger.debug("  → Consider enabling debug logging to see detailed pattern matching results");
        logger.debug("  → Check that templateVariables include all required values for conditions");
    }

    private static void generateDetailedDiff(String[] originalLines, String[] modifiedLines, String operation) {
        logger.debug("DETAILED_DIFF for operation '{}': original_lines={} modified_lines={}",
            operation, originalLines.length, modifiedLines.length);

        // Simple diff algorithm - show changes
        int maxLines = Math.max(originalLines.length, modifiedLines.length);
        int changesShown = 0;

        for (int i = 0; i < maxLines && changesShown < MAX_DIFF_CONTEXT; i++) {
            String origLine = i < originalLines.length ? originalLines[i] : "";
            String modLine = i < modifiedLines.length ? modifiedLines[i] : "";

            if (!origLine.equals(modLine)) {
                changesShown++;
                logger.trace("DIFF_LINE_{}: original='{}' modified='{}'",
                    i + 1, truncateLine(origLine, 60), truncateLine(modLine, 60));
            }
        }
    }

    private static void logTemplateContextSample(String template, String operation) {
        String[] lines = template.split("\n");
        int totalLines = lines.length;

        logger.trace("TEMPLATE_SAMPLE after '{}': total_lines={}", operation, totalLines);

        // Show first few lines
        for (int i = 0; i < Math.min(3, totalLines); i++) {
            logger.trace("  Line {}: {}", i + 1, truncateLine(lines[i], 80));
        }

        // Show last few lines if template is long
        if (totalLines > 6) {
            logger.trace("  ...");
            for (int i = Math.max(totalLines - 3, 3); i < totalLines; i++) {
                logger.trace("  Line {}: {}", i + 1, truncateLine(lines[i], 80));
            }
        }
    }

    private static Object getContextValue(EvaluationContext context, String key) {
        if (context == null) return null;

        // Check project properties first
        if (context.getProjectProperties() != null && context.getProjectProperties().containsKey(key)) {
            return context.getProjectProperties().get(key);
        }

        // Check environment variables
        if (context.getEnvironmentVariables() != null && context.getEnvironmentVariables().containsKey(key)) {
            return context.getEnvironmentVariables().get(key);
        }

        return null;
    }

    private static int getLineNumber(String text, int position) {
        return (int) text.substring(0, position).chars().filter(ch -> ch == '\n').count() + 1;
    }

    private static String truncatePattern(String pattern) {
        if (pattern == null) return "null";
        return pattern.length() > MAX_PATTERN_PREVIEW ?
            pattern.substring(0, MAX_PATTERN_PREVIEW) + "..." : pattern;
    }

    private static String truncateLine(String line, int maxLength) {
        if (line == null) return "";
        return line.length() > maxLength ? line.substring(0, maxLength) + "..." : line;
    }

    private static String limitMapSize(Map<?, ?> map, int maxEntries, String moreMessage) {
        if (map.size() <= maxEntries) {
            return map.toString();
        }

        StringBuilder sb = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count >= maxEntries) break;
            if (count > 0) sb.append(", ");

            sb.append(entry.getKey()).append("=");
            Object value = entry.getValue();
            String valueStr = value != null ? value.toString() : "null";
            sb.append(valueStr.length() > 50 ? valueStr.substring(0, 50) + "..." : valueStr);
            count++;
        }

        if (map.size() > maxEntries) {
            sb.append(", ... (").append(map.size() - maxEntries).append(" more entries)");
        }
        sb.append("}");

        return sb.toString();
    }
}