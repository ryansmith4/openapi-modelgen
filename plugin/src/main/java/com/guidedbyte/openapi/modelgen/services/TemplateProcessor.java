package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.customization.*;
import com.guidedbyte.openapi.modelgen.services.CustomizationEngine.CustomizationException;
import org.slf4j.Logger;
import com.guidedbyte.openapi.modelgen.util.PluginLoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Processes templates by applying customizations through string-based operations.
 * <p>
 * Handles all types of template modifications:
 * - Basic insertions (after/before/at patterns)
 * - Find/replace operations (string and regex)
 * - Smart replacements (version-agnostic patterns)
 * - Smart insertions (semantic insertion points)
 * - Partial expansion and content resolution
 * 
 * @since 2.0.0
 */
public class TemplateProcessor {
    private static final Logger logger = PluginLoggerFactory.getLogger(TemplateProcessor.class);
    
    private final ConditionEvaluator conditionEvaluator;
    private final PatternMatcher patternMatcher;
    private final SemanticProcessor semanticProcessor;
    
    public TemplateProcessor(ConditionEvaluator conditionEvaluator) {
        this.conditionEvaluator = conditionEvaluator;
        this.patternMatcher = new PatternMatcher();
        this.semanticProcessor = new SemanticProcessor();
    }
    
    /**
     * Applies an insertion to the template content.
     */
    public String applyInsertion(String template, Insertion insertion, EvaluationContext context, 
                               Map<String, String> partials) throws CustomizationException {
        
        if (shouldSkipCustomization(insertion.getConditions(), context)) {
            // Try fallback if conditions not met
            if (insertion.getFallback() != null) {
                logger.debug("Primary insertion conditions not met, trying fallback");
                return applyInsertion(template, insertion.getFallback(), context, partials);
            } else {
                logger.debug("Insertion conditions not met and no fallback provided");
                return template;
            }
        }
        
        String content = expandPartials(insertion.getContent(), partials);
        
        // Apply insertion based on type
        if (insertion.getAfter() != null) {
            return patternMatcher.insertAfterPattern(template, insertion.getAfter(), content);
        } else if (insertion.getBefore() != null) {
            return patternMatcher.insertBeforePattern(template, insertion.getBefore(), content);
        } else if (insertion.getAt() != null) {
            return applyAtInsertion(template, insertion.getAt(), content);
        } else {
            // Use ErrorHandlingUtils for consistent error messaging
            String errorMessage = ErrorHandlingUtils.formatTemplateError("insertion", 
                "must specify after, before, or at");
            throw new CustomizationException(errorMessage);
        }
    }
    
    /**
     * Applies a replacement to the template content.
     */
    public String applyReplacement(String template, Replacement replacement, EvaluationContext context,
                                 Map<String, String> partials) throws CustomizationException {
        
        logger.debug("=== REPLACEMENT DEBUG ===");
        logger.debug("Template length: {}", template.length());
        logger.debug("Find pattern: '{}'", replacement.getFind());
        logger.debug("Replace content: '{}'", replacement.getReplace());
        logger.debug("Replacement type: '{}'", replacement.getType());
        logger.debug("Has conditions: {}", replacement.getConditions() != null);
        
        if (shouldSkipCustomization(replacement.getConditions(), context)) {
            logger.debug("CONDITIONS NOT MET - skipping replacement");
            // Try fallback if conditions not met
            if (replacement.getFallback() != null) {
                logger.debug("Primary replacement conditions not met, trying fallback");
                return applyReplacement(template, replacement.getFallback(), context, partials);
            } else {
                logger.debug("Replacement conditions not met and no fallback provided");
                return template;
            }
        }
        
        logger.debug("CONDITIONS MET - applying replacement");
        String replaceContent = expandPartials(replacement.getReplace(), partials);
        logger.debug("Expanded replace content: '{}'", replaceContent);
        
        // Check if pattern exists in template
        boolean patternFound = template.contains(replacement.getFind());
        logger.debug("Pattern '{}' found in template: {}", replacement.getFind(), patternFound);
        
        if (patternFound) {
            logger.debug("Template excerpt around first match: '{}'", 
                getTemplateExcerpt(template, replacement.getFind(), 50));
        }
        
        String result;
        if ("regex".equals(replacement.getType())) {
            logger.debug("Applying REGEX replacement");
            result = patternMatcher.replaceWithRegex(template, replacement.getFind(), replaceContent);
        } else {
            logger.debug("Applying STRING replacement");
            result = patternMatcher.replaceString(template, replacement.getFind(), replaceContent);
        }
        
        boolean wasModified = !template.equals(result);
        logger.debug("Template was modified: {}", wasModified);
        
        if (wasModified) {
            logger.debug("Template length changed from {} to {}", template.length(), result.length());
        }
        
        logger.debug("=== END REPLACEMENT DEBUG ===");
        return result;
    }
    
    // Helper method to show template context around a pattern
    private String getTemplateExcerpt(String template, String pattern, int contextLength) {
        int index = template.indexOf(pattern);
        if (index == -1) return "PATTERN NOT FOUND";
        
        int start = Math.max(0, index - contextLength);
        int end = Math.min(template.length(), index + pattern.length() + contextLength);
        
        String excerpt = template.substring(start, end);
        return excerpt.replace("\n", "\\n").replace("\r", "\\r");
    }
    
    /**
     * Applies a smart replacement to the template content.
     */
    public String applySmartReplacement(String template, SmartReplacement replacement, EvaluationContext context,
                                      Map<String, String> partials) throws CustomizationException {
        
        if (shouldSkipCustomization(replacement.getConditions(), context)) {
            logger.debug("Smart replacement conditions not met");
            return template;
        }
        
        String replaceContent = expandPartials(replacement.getReplace(), partials);
        
        if (replacement.getFindAny() != null) {
            return applyFindAnyReplacement(template, replacement.getFindAny(), replaceContent);
        } else if (replacement.getSemantic() != null) {
            return semanticProcessor.applySemanticReplacement(template, replacement.getSemantic(), replaceContent);
        } else if (replacement.getFindPattern() != null) {
            return applyFindPatternReplacement(template, replacement.getFindPattern(), replaceContent);
        } else {
            // Use ErrorHandlingUtils for consistent error messaging
            String errorMessage = ErrorHandlingUtils.formatTemplateError("smart replacement", 
                "must specify findAny, semantic, or findPattern");
            throw new CustomizationException(errorMessage);
        }
    }
    
    /**
     * Applies a smart insertion to the template content.
     */
    public String applySmartInsertion(String template, SmartInsertion insertion, EvaluationContext context,
                                    Map<String, String> partials) throws CustomizationException {
        
        if (shouldSkipCustomization(insertion.getConditions(), context)) {
            // Try fallback if conditions not met
            if (insertion.getFallback() != null) {
                logger.debug("Smart insertion conditions not met, trying fallback");
                return applyInsertion(template, insertion.getFallback(), context, partials);
            } else {
                logger.debug("Smart insertion conditions not met and no fallback provided");
                return template;
            }
        }
        
        String content = expandPartials(insertion.getContent(), partials);
        
        if (insertion.getSemantic() != null) {
            return semanticProcessor.applySemanticInsertion(template, insertion.getSemantic(), content);
        } else if (insertion.getFindInsertionPoint() != null) {
            return applyFindInsertionPoint(template, insertion.getFindInsertionPoint(), content);
        } else {
            // Use ErrorHandlingUtils for consistent error messaging
            String errorMessage = ErrorHandlingUtils.formatTemplateError("smart insertion", 
                "must specify semantic or findInsertionPoint");
            throw new CustomizationException(errorMessage);
        }
    }
    
    /**
     * Applies insertion at special locations (start/end).
     */
    private String applyAtInsertion(String template, String at, String content) {
        if ("start".equalsIgnoreCase(at)) {
            return content + template;
        } else if ("end".equalsIgnoreCase(at)) {
            return template + content;
        } else {
            logger.warn("Unknown insertion location: {}", at);
            return template;
        }
    }
    
    /**
     * Applies findAny replacement - tries patterns in order, uses first match.
     */
    private String applyFindAnyReplacement(String template, List<String> patterns, String replaceContent) {
        for (String pattern : patterns) {
            if (template.contains(pattern)) {
                logger.debug("Found pattern '{}' for smart replacement", pattern);
                return patternMatcher.replaceString(template, pattern, replaceContent);
            }
        }
        
        logger.debug("No patterns matched for smart replacement: {}", patterns);
        return template; // No patterns matched
    }
    
    /**
     * Applies findPattern replacement with complex pattern matching.
     */
    private String applyFindPatternReplacement(String template, SmartReplacement.FindPattern findPattern, String replaceContent) {
        if (findPattern.getVariants() == null || findPattern.getVariants().isEmpty()) {
            logger.warn("FindPattern has no variants");
            return template;
        }
        
        // For now, treat variants like findAny patterns
        // In the future, this could be enhanced with pattern type-specific logic
        for (String variant : findPattern.getVariants()) {
            if (template.contains(variant)) {
                logger.debug("Found pattern variant '{}' for findPattern replacement", variant);
                return patternMatcher.replaceString(template, variant, replaceContent);
            }
        }
        
        logger.debug("No pattern variants matched for findPattern replacement");
        return template;
    }
    
    /**
     * Applies findInsertionPoint - tries insertion points in order, uses first match.
     */
    private String applyFindInsertionPoint(String template, SmartInsertion.InsertionPoint insertionPoint, String content) {
        if (insertionPoint.getPatterns() == null || insertionPoint.getPatterns().isEmpty()) {
            logger.warn("InsertionPoint has no patterns");
            return template;
        }
        
        for (SmartInsertion.PatternLocation location : insertionPoint.getPatterns()) {
            if (location.getAfter() != null && template.contains(location.getAfter())) {
                logger.debug("Found insertion point after '{}'", location.getAfter());
                return patternMatcher.insertAfterPattern(template, location.getAfter(), content);
            } else if (location.getBefore() != null && template.contains(location.getBefore())) {
                logger.debug("Found insertion point before '{}'", location.getBefore());
                return patternMatcher.insertBeforePattern(template, location.getBefore(), content);
            }
        }
        
        logger.debug("No insertion points matched");
        return template;
    }
    
    /**
     * Expands partial references in content.
     */
    private String expandPartials(String content, Map<String, String> partials) {
        if (content == null || partials == null || partials.isEmpty()) {
            return content;
        }
        
        String result = content;
        
        // Pattern to match {{>partialName}}
        Pattern partialPattern = Pattern.compile("\\{\\{>([^}]+)\\}\\}");
        Matcher matcher = partialPattern.matcher(content);
        
        while (matcher.find()) {
            String partialName = matcher.group(1).trim();
            String partialContent = partials.get(partialName);
            
            if (partialContent != null) {
                String partialRef = "{{>" + matcher.group(1) + "}}";
                result = result.replace(partialRef, partialContent);
                logger.debug("Expanded partial '{}' in content", partialName);
            } else {
                logger.warn("Partial '{}' referenced but not defined", partialName);
            }
        }
        
        return result;
    }
    
    /**
     * Checks if a customization should be skipped based on its conditions.
     * Returns true if conditions are not met and customization should be skipped.
     */
    private boolean shouldSkipCustomization(ConditionSet conditions, EvaluationContext context) {
        return !conditionEvaluator.evaluate(conditions, context);
    }
    
    /**
     * Helper class for pattern matching operations.
     */
    private static class PatternMatcher {
        
        public PatternMatcher() {
        }
        
        /**
         * Inserts content after the first occurrence of a pattern.
         */
        public String insertAfterPattern(String template, String pattern, String content) {
            int index = template.indexOf(pattern);
            if (index >= 0) {
                int insertPoint = index + pattern.length();
                String result = template.substring(0, insertPoint) + content + template.substring(insertPoint);
                logger.debug("Inserted content after pattern '{}'", pattern);
                return result;
            } else {
                logger.debug("Pattern '{}' not found for insertion", pattern);
                return template;
            }
        }
        
        /**
         * Inserts content before the first occurrence of a pattern.
         */
        public String insertBeforePattern(String template, String pattern, String content) {
            int index = template.indexOf(pattern);
            if (index >= 0) {
                String result = template.substring(0, index) + content + template.substring(index);
                logger.debug("Inserted content before pattern '{}'", pattern);
                return result;
            } else {
                logger.debug("Pattern '{}' not found for insertion", pattern);
                return template;
            }
        }
        
        /**
         * Replaces all occurrences of a string pattern.
         */
        public String replaceString(String template, String find, String replace) {
            // Enhanced debug logging for pattern matching troubleshooting
            logger.debug("=== STRING REPLACEMENT DEBUG ===");
            logger.debug("Find pattern length: {}", find.length());
            logger.debug("Find pattern (escaped): '{}'", find.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));
            logger.debug("Replace pattern (escaped): '{}'", replace.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));
            logger.debug("Template contains pattern: {}", template.contains(find));
            
            if (!template.contains(find)) {
                // Additional debugging for near-matches
                String[] lines = template.split("\n");
                logger.debug("Template has {} lines", lines.length);
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if (line.contains("name") && line.contains("value")) {
                        logger.debug("Potential match at line {}: '{}'", i + 1, line.replace("\t", "\\t"));
                    }
                }
                
                logger.debug("Pattern '{}' not found for replacement", find.replace("\n", "\\n").replace("\r", "\\r"));
                return template;
            }
            
            String result = template.replace(find, replace);
            int occurrences = template.length() - result.length() + replace.length();
            logger.debug("Replaced {} occurrence(s) of pattern", occurrences);
            logger.debug("Template length before: {}, after: {}", template.length(), result.length());
            return result;
        }
        
        /**
         * Replaces using regex pattern.
         */
        public String replaceWithRegex(String template, String pattern, String replacement) throws CustomizationException {
            try {
                Pattern regex = Pattern.compile(pattern);
                Matcher matcher = regex.matcher(template);
                
                if (matcher.find()) {
                    String result = matcher.replaceAll(replacement);
                    logger.debug("Applied regex replacement for pattern '{}'", pattern);
                    return result;
                } else {
                    logger.debug("Regex pattern '{}' not found for replacement", pattern);
                    return template;
                }
            } catch (PatternSyntaxException e) {
                throw new CustomizationException("Invalid regex pattern '" + pattern + "': " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Helper class for semantic processing operations.
     */
    private static class SemanticProcessor {
        
        // Semantic insertion point patterns
        private static final Map<String, String[]> SEMANTIC_PATTERNS;
        
        static {
            Map<String, String[]> patterns = new HashMap<>();
            patterns.put("start_of_file", new String[]{});  // Handled specially
            patterns.put("end_of_file", new String[]{});    // Handled specially
            patterns.put("after_license", new String[]{"{{>licenseInfo}}", "*/", "# License", "// License"});
            patterns.put("after_package", new String[]{"package ", "{{package}}", "{{#package}}"});
            patterns.put("end_of_imports", new String[]{"{{/imports}}", "{{/import}}", "import ", "; // imports end"});
            patterns.put("after_class_declaration", new String[]{"public class ", "class ", "{{classname}}", "public interface "});
            patterns.put("after_model_declaration", new String[]{"{{#model}}", "{{#models}}", "// Model declaration"});
            patterns.put("before_class_end", new String[]{"}\n}", "} // class end", "{{/model}}"});
            patterns.put("after_constructor", new String[]{"// constructor", "{{#generateConstructors}}", "public {{classname}}("});
            patterns.put("after_fields", new String[]{"{{/vars}}", "// fields end", "{{#hasVars}}{{/hasVars}}"});
            patterns.put("after_getters_setters", new String[]{"// getters/setters", "{{/operations}}", "{{/vars}}"});
            SEMANTIC_PATTERNS = patterns;
        }
        
        public SemanticProcessor() {
        }
        
        /**
         * Applies semantic insertion at logical points in the template.
         */
        public String applySemanticInsertion(String template, String semantic, String content) {
            return switch (semantic) {
                case "start_of_file" -> content + template;
                case "end_of_file" -> template + content;
                default -> applyPatternBasedInsertion(template, semantic, content);
            };
        }
        
        /**
         * Applies semantic replacement based on meaning rather than exact patterns.
         */
        public String applySemanticReplacement(String template, String semantic, String replacement) {
            // For now, semantic replacements use pattern-based logic
            // This could be enhanced with more sophisticated semantic analysis
            logger.debug("Applying semantic replacement: {}", semantic);
            
            // Try common patterns for the semantic concept
            String[] patterns = getSemanticPatterns(semantic);
            for (String pattern : patterns) {
                if (template.contains(pattern)) {
                    logger.debug("Found semantic pattern '{}' for '{}'", pattern, semantic);
                    return template.replace(pattern, replacement);
                }
            }
            
            logger.debug("No patterns found for semantic replacement: {}", semantic);
            return template;
        }
        
        private String applyPatternBasedInsertion(String template, String semantic, String content) {
            String[] patterns = SEMANTIC_PATTERNS.get(semantic);
            if (patterns == null) {
                logger.warn("Unknown semantic insertion point: {}", semantic);
                return template;
            }
            
            // Try patterns in order
            for (String pattern : patterns) {
                if (template.contains(pattern)) {
                    int index = template.indexOf(pattern);
                    int insertPoint = index + pattern.length();
                    logger.debug("Found semantic insertion point '{}' using pattern '{}'", semantic, pattern);
                    return template.substring(0, insertPoint) + content + template.substring(insertPoint);
                }
            }
            
            logger.debug("No patterns found for semantic insertion: {}", semantic);
            return template;
        }
        
        private String[] getSemanticPatterns(String semantic) {
            // Return patterns that might represent this semantic concept
            return SEMANTIC_PATTERNS.getOrDefault(semantic, new String[]{});
        }
    }
}