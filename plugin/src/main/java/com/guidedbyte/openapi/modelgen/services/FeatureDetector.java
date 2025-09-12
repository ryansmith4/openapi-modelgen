package com.guidedbyte.openapi.modelgen.services;

import java.util.List;
import java.util.Map;

/**
 * Detects features and capabilities in OpenAPI Generator templates.
 * <p>
 * Analyzes template content to determine what features are supported,
 * enabling version-agnostic customizations based on template capabilities.
 * 
 * @since 2.0.0
 */
public class FeatureDetector {
    
    /**
     * Constructs a new FeatureDetector.
     */
    public FeatureDetector() {
        // Default constructor
    }
    
    // Feature detection patterns - maps feature names to patterns that indicate support
    private static final Map<String, List<String>> FEATURE_PATTERNS = Map.of(
        "validation_support", List.of(
            "{{#hasValidation}}", "{{#isValid}}", "{{#validation}}",
            "@Valid", "@NotNull", "@Size", "@Pattern"
        ),
        
        "nullable_support", List.of(
            "{{#isNullable}}", "{{#nullable}}", "{{#allowNull}}",
            "@Nullable", "Optional<"
        ),
        
        "discriminator_support", List.of(
            "{{#hasDiscriminator}}", "{{#discriminator}}", "{{#inheritance}}",
            "@JsonSubTypes", "@JsonTypeInfo"
        ),
        
        "imports_section", List.of(
            "{{#imports}}", "{{#import}}", "import ", "{{>imports}}"
        ),
        
        "package_declaration", List.of(
            "{{#package}}", "{{packageName}}", "package "
        ),
        
        "documentation_support", List.of(
            "{{#description}}", "{{#notes}}", "{{#summary}}",
            "/**", "@ApiModel", "@Schema"
        ),
        
        "serialization_support", List.of(
            "{{#jackson}}", "@JsonProperty", "@JsonInclude", 
            "@JsonIgnore", "@JsonSerialize", "@JsonDeserialize"
        ),
        
        "builder_pattern", List.of(
            "{{#generateBuilders}}", ".builder()", "@Builder",
            "@SuperBuilder", "{{#hasBuilder}}"
        ),
        
        "fluent_setters", List.of(
            "{{#fluent}}", "{{#chainedAccessors}}", 
            "@Accessors(fluent", "return this;"
        )
    );
    
    /**
     * Checks if a template supports a specific feature.
     * 
     * @param templateContent the template content to analyze
     * @param featureName the feature to check for
     * @return true if the feature is detected in the template
     */
    public boolean hasFeature(String templateContent, String featureName) {
        if (templateContent == null || featureName == null) {
            return false;
        }
        
        // Handle custom features (prefixed with "custom_")
        if (featureName.startsWith("custom_")) {
            return hasCustomFeature(templateContent, featureName);
        }
        
        // Check built-in features
        List<String> patterns = FEATURE_PATTERNS.get(featureName);
        if (patterns == null) {
            return false; // Unknown feature
        }
        
        // Feature is detected if any of its patterns are found
        return patterns.stream().anyMatch(templateContent::contains);
    }
    
    /**
     * Checks for custom features based on naming conventions.
     */
    private boolean hasCustomFeature(String templateContent, String featureName) {
        // Custom features are detected by converting the feature name to template patterns
        // e.g., "custom_validation" -> look for "{{#customValidation}}" or "{{customValidation}}"
        String baseFeatureName = featureName.substring("custom_".length());
        
        // Convert to camelCase for Mustache patterns
        String camelCase = toCamelCase(baseFeatureName);
        
        // Look for common Mustache patterns
        String[] patterns = {
            "{{#" + camelCase + "}}",
            "{{" + camelCase + "}}",
            "{{#has" + capitalize(camelCase) + "}}",
            "{{#is" + capitalize(camelCase) + "}}",
            "{{>" + camelCase + "}}",
            "{{>" + baseFeatureName + "}}"
        };
        
        for (String pattern : patterns) {
            if (templateContent.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Detects all features present in a template.
     * 
     * @param templateContent the template content to analyze
     * @return map of feature names to detection status
     */
    public Map<String, Boolean> detectAllFeatures(String templateContent) {
        Map<String, Boolean> results = new java.util.HashMap<>();
        
        for (String featureName : FEATURE_PATTERNS.keySet()) {
            results.put(featureName, hasFeature(templateContent, featureName));
        }
        
        return results;
    }
    
    /**
     * Gets the list of supported built-in features.
     * @return the set of supported feature names
     */
    public java.util.Set<String> getSupportedFeatures() {
        return FEATURE_PATTERNS.keySet();
    }
    
    /**
     * Converts snake_case to camelCase.
     */
    private String toCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return snakeCase;
        }
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Capitalizes the first letter of a string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}