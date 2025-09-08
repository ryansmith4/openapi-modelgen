package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.customization.*;
import com.guidedbyte.openapi.modelgen.services.CustomizationEngine.CustomizationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates YAML customization configurations against the schema.
 * 
 * Performs comprehensive validation including:
 * - Required field validation
 * - Schema structure validation
 * - Content validation (patterns, references, etc.)
 * - Security validation (dangerous operations)
 * 
 * @since 2.0.0
 */
public class YamlValidator {
    
    // Valid semantic insertion points
    private static final Set<String> VALID_SEMANTIC_INSERTIONS = Set.of(
        "start_of_file", "end_of_file", "after_license", "after_package", 
        "end_of_imports", "after_class_declaration", "after_model_declaration", 
        "before_class_end", "after_constructor", "after_fields", "after_getters_setters"
    );
    
    // Valid built-in features
    private static final Set<String> VALID_FEATURES = Set.of(
        "validation_support", "nullable_support", "discriminator_support",
        "imports_section", "package_declaration", "documentation_support",
        "serialization_support", "builder_pattern", "fluent_setters"
    );
    
    // Version constraint pattern (semantic versioning)
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "^(>=|>|<=|<|~>|\\^)\\s*\\d+\\.\\d+(\\.\\d+)?(-[\\w.-]+)?(\\+[\\w.-]+)?$"
    );
    
    /**
     * Validates a complete customization configuration.
     * 
     * @param config the configuration to validate
     * @param sourceName name of the source for error reporting
     * @throws CustomizationException if validation fails
     */
    public void validateCustomizationConfig(CustomizationConfig config, String sourceName) throws CustomizationException {
        List<String> errors = new ArrayList<>();
        
        // Validate metadata (optional but if present, should be valid)
        if (config.getMetadata() != null) {
            validateMetadata(config.getMetadata(), errors);
        }
        
        // Validate global conditions
        if (config.getConditions() != null) {
            validateConditionSet(config.getConditions(), "global", errors);
        }
        
        // Validate insertions
        if (config.getInsertions() != null) {
            for (int i = 0; i < config.getInsertions().size(); i++) {
                validateInsertion(config.getInsertions().get(i), "insertions[" + i + "]", errors);
            }
        }
        
        // Validate replacements
        if (config.getReplacements() != null) {
            for (int i = 0; i < config.getReplacements().size(); i++) {
                validateReplacement(config.getReplacements().get(i), "replacements[" + i + "]", errors);
            }
        }
        
        // Validate smart replacements
        if (config.getSmartReplacements() != null) {
            for (int i = 0; i < config.getSmartReplacements().size(); i++) {
                validateSmartReplacement(config.getSmartReplacements().get(i), "smartReplacements[" + i + "]", errors);
            }
        }
        
        // Validate smart insertions
        if (config.getSmartInsertions() != null) {
            for (int i = 0; i < config.getSmartInsertions().size(); i++) {
                validateSmartInsertion(config.getSmartInsertions().get(i), "smartInsertions[" + i + "]", errors);
            }
        }
        
        // Validate partials
        if (config.getPartials() != null) {
            validatePartials(config.getPartials(), errors);
        }
        
        // Validate that we have at least some customizations
        boolean hasCustomizations = 
            (config.getInsertions() != null && !config.getInsertions().isEmpty()) ||
            (config.getReplacements() != null && !config.getReplacements().isEmpty()) ||
            (config.getSmartReplacements() != null && !config.getSmartReplacements().isEmpty()) ||
            (config.getSmartInsertions() != null && !config.getSmartInsertions().isEmpty());
        
        if (!hasCustomizations) {
            errors.add("Configuration must contain at least one customization (insertions, replacements, smartReplacements, or smartInsertions)");
        }
        
        if (!errors.isEmpty()) {
            throw new CustomizationException("Validation failed for " + sourceName + ":\\n" + String.join("\\n", errors));
        }
    }
    
    private void validateMetadata(CustomizationMetadata metadata, List<String> errors) {
        if (metadata.getName() != null && metadata.getName().trim().isEmpty()) {
            errors.add("metadata.name cannot be empty");
        }
        if (metadata.getVersion() != null && metadata.getVersion().trim().isEmpty()) {
            errors.add("metadata.version cannot be empty");
        }
    }
    
    private void validateInsertion(Insertion insertion, String path, List<String> errors) {
        if (insertion == null) {
            errors.add(path + ": insertion cannot be null");
            return;
        }
        
        // Must specify exactly one insertion point
        int insertionPointCount = 0;
        if (insertion.getAfter() != null && !insertion.getAfter().trim().isEmpty()) insertionPointCount++;
        if (insertion.getBefore() != null && !insertion.getBefore().trim().isEmpty()) insertionPointCount++;
        if (insertion.getAt() != null && !insertion.getAt().trim().isEmpty()) insertionPointCount++;
        
        if (insertionPointCount == 0) {
            errors.add(path + ": must specify one insertion point (after, before, or at)");
        } else if (insertionPointCount > 1) {
            errors.add(path + ": can only specify one insertion point (after, before, or at)");
        }
        
        // Validate 'at' values
        if (insertion.getAt() != null) {
            String at = insertion.getAt().trim().toLowerCase();
            if (!at.equals("start") && !at.equals("end")) {
                errors.add(path + ".at: must be 'start' or 'end', got: " + insertion.getAt());
            }
        }
        
        // Content is required
        if (insertion.getContent() == null || insertion.getContent().trim().isEmpty()) {
            errors.add(path + ".content: content is required and cannot be empty");
        }
        
        // Validate conditions
        if (insertion.getConditions() != null) {
            validateConditionSet(insertion.getConditions(), path + ".conditions", errors);
        }
        
        // Validate fallback
        if (insertion.getFallback() != null) {
            validateInsertion(insertion.getFallback(), path + ".fallback", errors);
        }
        
        // Check for dangerous content
        validateSafeContent(insertion.getContent(), path + ".content", errors);
    }
    
    private void validateReplacement(Replacement replacement, String path, List<String> errors) {
        if (replacement == null) {
            errors.add(path + ": replacement cannot be null");
            return;
        }
        
        // Find pattern is required
        if (replacement.getFind() == null || replacement.getFind().trim().isEmpty()) {
            errors.add(path + ".find: find pattern is required and cannot be empty");
        }
        
        // Replace content is required
        if (replacement.getReplace() == null) {
            errors.add(path + ".replace: replace content is required (can be empty string)");
        }
        
        // Validate replacement type
        if (replacement.getType() != null) {
            String type = replacement.getType().toLowerCase();
            if (!type.equals("string") && !type.equals("regex")) {
                errors.add(path + ".type: must be 'string' or 'regex', got: " + replacement.getType());
            }
            
            // Validate regex if specified
            if (type.equals("regex")) {
                try {
                    Pattern.compile(replacement.getFind());
                } catch (Exception e) {
                    errors.add(path + ".find: invalid regex pattern: " + e.getMessage());
                }
            }
        }
        
        // Validate conditions
        if (replacement.getConditions() != null) {
            validateConditionSet(replacement.getConditions(), path + ".conditions", errors);
        }
        
        // Validate fallback
        if (replacement.getFallback() != null) {
            validateReplacement(replacement.getFallback(), path + ".fallback", errors);
        }
        
        // Check for dangerous content
        validateSafeContent(replacement.getFind(), path + ".find", errors);
        validateSafeContent(replacement.getReplace(), path + ".replace", errors);
    }
    
    private void validateSmartReplacement(SmartReplacement replacement, String path, List<String> errors) {
        if (replacement == null) {
            errors.add(path + ": smart replacement cannot be null");
            return;
        }
        
        // Must specify exactly one find method
        int findMethodCount = 0;
        if (replacement.getFindAny() != null && !replacement.getFindAny().isEmpty()) findMethodCount++;
        if (replacement.getSemantic() != null && !replacement.getSemantic().trim().isEmpty()) findMethodCount++;
        if (replacement.getFindPattern() != null) findMethodCount++;
        
        if (findMethodCount == 0) {
            errors.add(path + ": must specify one find method (findAny, semantic, or findPattern)");
        } else if (findMethodCount > 1) {
            errors.add(path + ": can only specify one find method (findAny, semantic, or findPattern)");
        }
        
        // Validate findAny
        if (replacement.getFindAny() != null) {
            if (replacement.getFindAny().isEmpty()) {
                errors.add(path + ".findAny: cannot be empty list");
            } else {
                for (int i = 0; i < replacement.getFindAny().size(); i++) {
                    String pattern = replacement.getFindAny().get(i);
                    if (pattern == null || pattern.trim().isEmpty()) {
                        errors.add(path + ".findAny[" + i + "]: pattern cannot be null or empty");
                    }
                }
            }
        }
        
        // Validate semantic
        if (replacement.getSemantic() != null && replacement.getSemantic().trim().isEmpty()) {
            errors.add(path + ".semantic: cannot be empty");
        }
        
        // Validate findPattern
        if (replacement.getFindPattern() != null) {
            validateFindPattern(replacement.getFindPattern(), path + ".findPattern", errors);
        }
        
        // Replace content is required
        if (replacement.getReplace() == null) {
            errors.add(path + ".replace: replace content is required (can be empty string)");
        }
        
        // Validate conditions
        if (replacement.getConditions() != null) {
            validateConditionSet(replacement.getConditions(), path + ".conditions", errors);
        }
        
        // Check for dangerous content
        validateSafeContent(replacement.getReplace(), path + ".replace", errors);
    }
    
    private void validateSmartInsertion(SmartInsertion insertion, String path, List<String> errors) {
        if (insertion == null) {
            errors.add(path + ": smart insertion cannot be null");
            return;
        }
        
        // Must specify exactly one insertion method
        int insertionMethodCount = 0;
        if (insertion.getFindInsertionPoint() != null) insertionMethodCount++;
        if (insertion.getSemantic() != null && !insertion.getSemantic().trim().isEmpty()) insertionMethodCount++;
        
        if (insertionMethodCount == 0) {
            errors.add(path + ": must specify one insertion method (findInsertionPoint or semantic)");
        } else if (insertionMethodCount > 1) {
            errors.add(path + ": can only specify one insertion method (findInsertionPoint or semantic)");
        }
        
        // Validate semantic insertion point
        if (insertion.getSemantic() != null) {
            String semantic = insertion.getSemantic().trim();
            if (!VALID_SEMANTIC_INSERTIONS.contains(semantic)) {
                errors.add(path + ".semantic: unknown semantic insertion point '" + semantic + "'. Valid values: " + VALID_SEMANTIC_INSERTIONS);
            }
        }
        
        // Validate findInsertionPoint
        if (insertion.getFindInsertionPoint() != null) {
            validateInsertionPoint(insertion.getFindInsertionPoint(), path + ".findInsertionPoint", errors);
        }
        
        // Content is required
        if (insertion.getContent() == null || insertion.getContent().trim().isEmpty()) {
            errors.add(path + ".content: content is required and cannot be empty");
        }
        
        // Validate conditions
        if (insertion.getConditions() != null) {
            validateConditionSet(insertion.getConditions(), path + ".conditions", errors);
        }
        
        // Validate fallback
        if (insertion.getFallback() != null) {
            validateInsertion(insertion.getFallback(), path + ".fallback", errors);
        }
        
        // Check for dangerous content
        validateSafeContent(insertion.getContent(), path + ".content", errors);
    }
    
    private void validateFindPattern(SmartReplacement.FindPattern pattern, String path, List<String> errors) {
        if (pattern.getType() == null || pattern.getType().trim().isEmpty()) {
            errors.add(path + ".type: type is required");
        }
        
        if (pattern.getVariants() == null || pattern.getVariants().isEmpty()) {
            errors.add(path + ".variants: at least one variant is required");
        } else {
            for (int i = 0; i < pattern.getVariants().size(); i++) {
                String variant = pattern.getVariants().get(i);
                if (variant == null || variant.trim().isEmpty()) {
                    errors.add(path + ".variants[" + i + "]: variant cannot be null or empty");
                }
            }
        }
    }
    
    private void validateInsertionPoint(SmartInsertion.InsertionPoint point, String path, List<String> errors) {
        if (point.getPatterns() == null || point.getPatterns().isEmpty()) {
            errors.add(path + ".patterns: at least one pattern is required");
        } else {
            for (int i = 0; i < point.getPatterns().size(); i++) {
                SmartInsertion.PatternLocation location = point.getPatterns().get(i);
                validatePatternLocation(location, path + ".patterns[" + i + "]", errors);
            }
        }
    }
    
    private void validatePatternLocation(SmartInsertion.PatternLocation location, String path, List<String> errors) {
        if (location == null) {
            errors.add(path + ": pattern location cannot be null");
            return;
        }
        
        // Must specify exactly one location type
        int locationCount = 0;
        if (location.getAfter() != null && !location.getAfter().trim().isEmpty()) locationCount++;
        if (location.getBefore() != null && !location.getBefore().trim().isEmpty()) locationCount++;
        
        if (locationCount == 0) {
            errors.add(path + ": must specify either 'after' or 'before'");
        } else if (locationCount > 1) {
            errors.add(path + ": can only specify either 'after' or 'before', not both");
        }
    }
    
    private void validateConditionSet(ConditionSet conditions, String path, List<String> errors) {
        if (conditions == null) {
            return;
        }
        
        // Validate version constraints
        if (conditions.getGeneratorVersion() != null) {
            if (!VERSION_PATTERN.matcher(conditions.getGeneratorVersion()).matches()) {
                errors.add(path + ".generatorVersion: invalid version constraint format. Use: >=1.2.3, >1.2.3, <=1.2.3, <1.2.3, ~>1.2.3, ^1.2.3");
            }
        }
        
        // Validate template content conditions
        validateStringCondition(conditions.getTemplateContains(), path + ".templateContains", errors);
        validateStringCondition(conditions.getTemplateNotContains(), path + ".templateNotContains", errors);
        validateStringListCondition(conditions.getTemplateContainsAll(), path + ".templateContainsAll", errors);
        validateStringListCondition(conditions.getTemplateContainsAny(), path + ".templateContainsAny", errors);
        
        // Validate feature conditions
        if (conditions.getHasFeature() != null) {
            String feature = conditions.getHasFeature().trim();
            if (!VALID_FEATURES.contains(feature) && !feature.startsWith("custom_")) {
                errors.add(path + ".hasFeature: unknown feature '" + feature + "'. Valid built-in features: " + VALID_FEATURES + ", or use 'custom_' prefix for custom features");
            }
        }
        
        validateFeatureListCondition(conditions.getHasAllFeatures(), path + ".hasAllFeatures", errors);
        validateFeatureListCondition(conditions.getHasAnyFeatures(), path + ".hasAnyFeatures", errors);
        
        // Validate build environment conditions
        validateStringCondition(conditions.getProjectProperty(), path + ".projectProperty", errors);
        validateStringCondition(conditions.getEnvironmentVariable(), path + ".environmentVariable", errors);
        validateStringCondition(conditions.getBuildType(), path + ".buildType", errors);
        
        // Validate logical operators
        if (conditions.getAllOf() != null) {
            if (conditions.getAllOf().isEmpty()) {
                errors.add(path + ".allOf: cannot be empty list");
            } else {
                for (int i = 0; i < conditions.getAllOf().size(); i++) {
                    validateConditionSet(conditions.getAllOf().get(i), path + ".allOf[" + i + "]", errors);
                }
            }
        }
        
        if (conditions.getAnyOf() != null) {
            if (conditions.getAnyOf().isEmpty()) {
                errors.add(path + ".anyOf: cannot be empty list");
            } else {
                for (int i = 0; i < conditions.getAnyOf().size(); i++) {
                    validateConditionSet(conditions.getAnyOf().get(i), path + ".anyOf[" + i + "]", errors);
                }
            }
        }
        
        if (conditions.getNot() != null) {
            validateConditionSet(conditions.getNot(), path + ".not", errors);
        }
    }
    
    private void validateStringCondition(String value, String path, List<String> errors) {
        if (value != null && value.trim().isEmpty()) {
            errors.add(path + ": cannot be empty string");
        }
    }
    
    private void validateStringListCondition(List<String> values, String path, List<String> errors) {
        if (values != null) {
            if (values.isEmpty()) {
                errors.add(path + ": cannot be empty list");
            } else {
                for (int i = 0; i < values.size(); i++) {
                    if (values.get(i) == null || values.get(i).trim().isEmpty()) {
                        errors.add(path + "[" + i + "]: cannot be null or empty");
                    }
                }
            }
        }
    }
    
    private void validateFeatureListCondition(List<String> features, String path, List<String> errors) {
        if (features != null) {
            if (features.isEmpty()) {
                errors.add(path + ": cannot be empty list");
            } else {
                for (int i = 0; i < features.size(); i++) {
                    String feature = features.get(i);
                    if (feature == null || feature.trim().isEmpty()) {
                        errors.add(path + "[" + i + "]: cannot be null or empty");
                    } else if (!VALID_FEATURES.contains(feature.trim()) && !feature.trim().startsWith("custom_")) {
                        errors.add(path + "[" + i + "]: unknown feature '" + feature + "'. Valid built-in features: " + VALID_FEATURES + ", or use 'custom_' prefix");
                    }
                }
            }
        }
    }
    
    private void validatePartials(java.util.Map<String, String> partials, List<String> errors) {
        for (java.util.Map.Entry<String, String> entry : partials.entrySet()) {
            String name = entry.getKey();
            String content = entry.getValue();
            
            if (name == null || name.trim().isEmpty()) {
                errors.add("partials: partial name cannot be null or empty");
                continue;
            }
            
            if (!name.matches("[a-zA-Z][a-zA-Z0-9_]*")) {
                errors.add("partials." + name + ": partial name must start with a letter and contain only letters, numbers, and underscores");
            }
            
            if (content == null) {
                errors.add("partials." + name + ": partial content cannot be null");
            }
            
            // Check for dangerous content
            validateSafeContent(content, "partials." + name, errors);
        }
    }
    
    private void validateSafeContent(String content, String path, List<String> errors) {
        if (content == null) {
            return;
        }
        
        // Check for potentially dangerous patterns
        String[] dangerousPatterns = {
            "<%", "%>",           // JSP/ASP tags
            "${java:",            // Java expression injection
            "Runtime.getRuntime", // Direct runtime access
            "ProcessBuilder",     // Process execution
            "System.exit",        // System shutdown
            "<script",           // JavaScript injection
            "javascript:",       // JavaScript protocol
            "file://",           // File protocol
            "exec(",             // Command execution
        };
        
        String lowerContent = content.toLowerCase();
        for (String pattern : dangerousPatterns) {
            if (lowerContent.contains(pattern.toLowerCase())) {
                errors.add(path + ": potentially dangerous content detected: " + pattern);
            }
        }
    }
}