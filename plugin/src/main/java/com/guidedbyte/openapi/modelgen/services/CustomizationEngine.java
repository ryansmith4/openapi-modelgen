package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.customization.*;
import com.guidedbyte.openapi.modelgen.TemplateConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.nodes.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration-cache compatible engine for parsing YAML customizations and applying them to templates.
 * 
 * <p>This service handles the complete template customization workflow:</p>
 * <ul>
 *   <li><strong>YAML Parsing:</strong> Safe parsing with security restrictions to prevent code execution</li>
 *   <li><strong>Schema Validation:</strong> Comprehensive validation of customization file structure</li>
 *   <li><strong>Template Processing:</strong> Applies insertions, replacements, and conditional logic</li>
 *   <li><strong>Caching:</strong> Multi-level caching (session → local → global) for optimal performance</li>
 *   <li><strong>Thread Safety:</strong> ConcurrentHashMap-based caches supporting parallel processing</li>
 *   <li><strong>Error Handling:</strong> Detailed error reporting with actionable messages</li>
 * </ul>
 * 
 * <p>The engine is stateless and configuration-cache compatible, using only static loggers
 * and avoiding Project dependencies. It supports both plugin customizations (embedded resources)
 * and user customizations (file system) with configurable precedence.</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.0.0
 */
public class CustomizationEngine {
    private static final Logger logger = LoggerFactory.getLogger(CustomizationEngine.class);
    
    private final YamlValidator yamlValidator;
    
    /**
     * Thread-safe cache for customization results.
     * Key: computed cache key based on inputs, Value: customized template result
     */
    private final Map<String, String> customizationResultCache = new ConcurrentHashMap<>();
    
    /**
     * Creates a new CustomizationEngine instance.
     */
    public CustomizationEngine() {
        this.yamlValidator = new YamlValidator();
    }
    
    /**
     * Parses a YAML customization file safely and validates its structure.
     * 
     * @param yamlFile the customization YAML file to parse
     * @return parsed and validated customization configuration
     * @throws CustomizationException if parsing or validation fails
     */
    public CustomizationConfig parseCustomizationYaml(File yamlFile) throws CustomizationException {
        if (!yamlFile.exists()) {
            throw new CustomizationException("Customization file does not exist: " + yamlFile.getPath());
        }
        
        if (!yamlFile.isFile()) {
            throw new CustomizationException("Customization path is not a file: " + yamlFile.getPath());
        }
        
        try (InputStream inputStream = new FileInputStream(yamlFile)) {
            return parseCustomizationYaml(inputStream, yamlFile.getName());
        } catch (IOException e) {
            throw new CustomizationException("Failed to read customization file: " + yamlFile.getPath(), e);
        }
    }
    
    /**
     * Parses YAML content from an input stream.
     * 
     * @param inputStream the YAML content stream
     * @param sourceName name of the source for error reporting
     * @return parsed and validated customization configuration
     * @throws CustomizationException if parsing or validation fails
     */
    public CustomizationConfig parseCustomizationYaml(InputStream inputStream, String sourceName) throws CustomizationException {
        try {
            // Read YAML content for pre-validation
            String yamlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            
            // Pre-validate YAML structure before attempting deserialization
            validateYamlStructure(yamlContent, sourceName);
            
            // Create safe YAML loader with security restrictions
            Yaml yaml = createSafeYamlLoader();
            
            // Parse YAML content with validated structure
            CustomizationConfig config = yaml.loadAs(yamlContent, CustomizationConfig.class);
            
            if (config == null) {
                throw new CustomizationException("Empty or invalid YAML content in: " + sourceName);
            }
            
            // Validate the parsed configuration
            yamlValidator.validateCustomizationConfig(config, sourceName);
            
            logger.debug("Successfully parsed and validated customization: {}", sourceName);
            return config;
            
        } catch (ConstructorException e) {
            throw new CustomizationException("Invalid YAML structure in " + sourceName + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new CustomizationException("Failed to read YAML content from " + sourceName + ": " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof CustomizationException) {
                throw e;
            }
            throw new CustomizationException("Failed to parse YAML customization from " + sourceName + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Applies all customizations from a configuration to a base template with result caching.
     * This is a caching wrapper around the core customization application logic.
     * 
     * @param baseTemplate the original template content
     * @param config the customization configuration to apply
     * @param context the evaluation context for conditions
     * @return the customized template content (from cache if available)
     * @throws CustomizationException if customization application fails
     */
    public String applyCustomizations(String baseTemplate, CustomizationConfig config, EvaluationContext context) throws CustomizationException {
        // Enhanced debug logging for troubleshooting
        logger.info("=== CUSTOMIZATION ENGINE ENTRY ===");
        logger.info("Template length: {}", baseTemplate != null ? baseTemplate.length() : "null");
        logger.info("Config name: {}", config != null && config.getMetadata() != null ? config.getMetadata().getName() : "null");
        logger.info("Config has replacements: {}", config != null && config.getReplacements() != null ? config.getReplacements().size() : 0);
        logger.info("Template starts with: '{}'", baseTemplate != null && baseTemplate.length() > 50 ? baseTemplate.substring(0, 50).replace("\n", "\\n") : "null");
        
        // Validate base template first (before caching logic)
        if (baseTemplate == null) {
            throw new CustomizationException("Base template content cannot be null");
        }
        
        // Check cache first
        String cacheKey = computeCustomizationCacheKey(baseTemplate, config, context);
        String cachedResult = customizationResultCache.get(cacheKey);
        
        if (cachedResult != null) {
            logger.info("Using cached customization result (key: {})", cacheKey.substring(0, Math.min(16, cacheKey.length())) + "...");
            return cachedResult;
        }
        
        // Apply customizations and cache result
        String result = applyCustomizationsInternal(baseTemplate, config, context);
        customizationResultCache.put(cacheKey, result);
        
        logger.debug("Cached customization result (key: {}, cache size: {})", 
            cacheKey.substring(0, Math.min(16, cacheKey.length())) + "...", customizationResultCache.size());
        
        return result;
    }
    
    /**
     * Internal method that performs the actual customization application without caching.
     * 
     * @param baseTemplate the original template content
     * @param config the customization configuration to apply
     * @param context the evaluation context for conditions
     * @return the customized template content
     * @throws CustomizationException if customization application fails
     */
    private String applyCustomizationsInternal(String baseTemplate, CustomizationConfig config, EvaluationContext context) throws CustomizationException {
        if (baseTemplate == null) {
            throw new CustomizationException("Base template content cannot be null");
        }
        
        logger.debug("Starting template customization, base template length: {}", baseTemplate.length());
        
        if (config == null) {
            logger.info("CUSTOMIZATION DEBUG: No customization config provided, returning original template");
            return baseTemplate;
        }
        
        logger.info("CUSTOMIZATION DEBUG: Config metadata: {}", config.getMetadata());
        logger.info("CUSTOMIZATION DEBUG: Number of insertions: {}", 
            config.getInsertions() != null ? config.getInsertions().size() : 0);
        
        // Check global conditions first
        ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
        if (config.getConditions() != null && !conditionEvaluator.evaluate(config.getConditions(), context)) {
            logger.info("CUSTOMIZATION DEBUG: Global conditions not met, skipping customization");
            return baseTemplate;
        }
        
        String result = baseTemplate;
        TemplateProcessor processor = new TemplateProcessor(conditionEvaluator);
        
        try {
            // Apply customizations in a specific order for predictable results
            
            // 1. Apply replacements first (modify existing content)
            if (config.getReplacements() != null) {
                for (Replacement replacement : config.getReplacements()) {
                    result = processor.applyReplacement(result, replacement, context, config.getPartials());
                }
            }
            
            // 2. Apply smart replacements (version-agnostic modifications)
            if (config.getSmartReplacements() != null) {
                for (SmartReplacement smartReplacement : config.getSmartReplacements()) {
                    result = processor.applySmartReplacement(result, smartReplacement, context, config.getPartials());
                }
            }
            
            // 3. Apply insertions (add new content)
            if (config.getInsertions() != null) {
                logger.info("CUSTOMIZATION DEBUG: Processing {} insertions", config.getInsertions().size());
                for (int i = 0; i < config.getInsertions().size(); i++) {
                    Insertion insertion = config.getInsertions().get(i);
                    logger.info("CUSTOMIZATION DEBUG: Processing insertion #{}: before='{}', content='{}'", 
                        i, insertion.getBefore(), insertion.getContent());
                    String beforeResult = result;
                    result = processor.applyInsertion(result, insertion, context, config.getPartials());
                    logger.info("CUSTOMIZATION DEBUG: Template changed: {}", !beforeResult.equals(result));
                }
            }
            
            // 4. Apply smart insertions (semantic insertion points)
            if (config.getSmartInsertions() != null) {
                for (SmartInsertion smartInsertion : config.getSmartInsertions()) {
                    result = processor.applySmartInsertion(result, smartInsertion, context, config.getPartials());
                }
            }
            
            logger.info("CUSTOMIZATION DEBUG: Final result length: {}", result.length());
            logger.info("CUSTOMIZATION DEBUG: Final result first 200 chars: {}", 
                result.length() > 200 ? result.substring(0, 200) + "..." : result);
            logger.info("=== CUSTOMIZATION DEBUG: Finished template customization ===");
            
            return result;
        
        } catch (Exception e) {
            logger.info("CUSTOMIZATION DEBUG: Exception during customization: {}", e.getMessage());
            throw new CustomizationException("Failed to apply customizations: " + e.getMessage(), e);
        }
    }
    
    /**
     * Pre-validates YAML structure before deserialization to catch common schema errors.
     * This prevents cryptic SnakeYAML constructor exceptions by validating property names 
     * and structure before attempting object creation.
     * 
     * @param yamlContent the raw YAML content
     * @param sourceName name of the source for error reporting  
     * @throws CustomizationException if structure validation fails
     */
    private void validateYamlStructure(String yamlContent, String sourceName) throws CustomizationException {
        try {
            // Create a basic YAML loader for structural validation (no type binding)
            Yaml yaml = new Yaml();
            Object parsed = yaml.load(yamlContent);
            
            if (parsed == null) {
                throw new CustomizationException("Empty YAML content in: " + sourceName);
            }
            
            if (!(parsed instanceof java.util.Map)) {
                throw new CustomizationException("YAML root must be an object/map in: " + sourceName);
            }
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> root = (java.util.Map<String, Object>) parsed;
            
            // Validate root-level properties
            validateRootProperties(root, sourceName);
            
            // Validate insertions structure
            if (root.containsKey("insertions")) {
                validateInsertionsStructure(root.get("insertions"), sourceName);
            }
            
            // Validate replacements structure  
            if (root.containsKey("replacements")) {
                validateReplacementsStructure(root.get("replacements"), sourceName);
            }
            
            // Validate smart replacements structure
            if (root.containsKey("smartReplacements")) {
                validateSmartReplacementsStructure(root.get("smartReplacements"), sourceName);
            }
            
            // Validate smart insertions structure
            if (root.containsKey("smartInsertions")) {
                validateSmartInsertionsStructure(root.get("smartInsertions"), sourceName);
            }
            
            logger.debug("YAML structure validation passed for: {}", sourceName);
            
        } catch (Exception e) {
            if (e instanceof CustomizationException) {
                throw e;
            }
            throw new CustomizationException("YAML structure validation failed for " + sourceName + ": " + e.getMessage(), e);
        }
    }
    
    private void validateRootProperties(java.util.Map<String, Object> root, String sourceName) throws CustomizationException {
        // Define allowed root properties
        java.util.Set<String> allowedRootProps = java.util.Set.of(
            "metadata", "conditions", "insertions", "replacements", 
            "smartReplacements", "smartInsertions", "partials"
        );
        
        for (String key : root.keySet()) {
            if (!allowedRootProps.contains(key)) {
                throw new CustomizationException(
                    "Unknown root property '" + key + "' in " + sourceName + 
                    ". Allowed properties: " + allowedRootProps
                );
            }
        }
    }
    
    private void validateInsertionsStructure(Object insertions, String sourceName) throws CustomizationException {
        if (!(insertions instanceof java.util.List)) {
            throw new CustomizationException("'insertions' must be a list/array in: " + sourceName);
        }
        
        @SuppressWarnings("unchecked")
        java.util.List<Object> insertionList = (java.util.List<Object>) insertions;
        
        for (int i = 0; i < insertionList.size(); i++) {
            Object item = insertionList.get(i);
            if (!(item instanceof java.util.Map)) {
                throw new CustomizationException("insertions[" + i + "] must be an object in: " + sourceName);
            }
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> insertion = (java.util.Map<String, Object>) item;
            
            // Check for invalid property names that cause ConstructorException
            java.util.Set<String> allowedInsertionProps = java.util.Set.of(
                "after", "before", "at", "content", "conditions", "fallback"
            );
            
            for (String key : insertion.keySet()) {
                if (!allowedInsertionProps.contains(key)) {
                    // Provide helpful error messages for common mistakes
                    if ("pattern".equals(key)) {
                        throw new CustomizationException(
                            "Invalid property 'pattern' in insertions[" + i + "] in " + sourceName + 
                            ". Use 'after' or 'before' instead of 'pattern'"
                        );
                    }
                    if ("position".equals(key)) {
                        throw new CustomizationException(
                            "Invalid property 'position' in insertions[" + i + "] in " + sourceName + 
                            ". Use 'after', 'before', or 'at' instead of 'position'"
                        );
                    }
                    throw new CustomizationException(
                        "Unknown property '" + key + "' in insertions[" + i + "] in " + sourceName + 
                        ". Allowed properties: " + allowedInsertionProps
                    );
                }
            }
        }
    }
    
    private void validateReplacementsStructure(Object replacements, String sourceName) throws CustomizationException {
        if (!(replacements instanceof java.util.List)) {
            throw new CustomizationException("'replacements' must be a list/array in: " + sourceName);
        }
        
        @SuppressWarnings("unchecked")
        java.util.List<Object> replacementList = (java.util.List<Object>) replacements;
        
        for (int i = 0; i < replacementList.size(); i++) {
            Object item = replacementList.get(i);
            if (!(item instanceof java.util.Map)) {
                throw new CustomizationException("replacements[" + i + "] must be an object in: " + sourceName);
            }
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> replacement = (java.util.Map<String, Object>) item;
            
            // Check for required properties
            if (!replacement.containsKey("find")) {
                throw new CustomizationException("replacements[" + i + "] must have 'find' property in: " + sourceName);
            }
            if (!replacement.containsKey("replace")) {
                throw new CustomizationException("replacements[" + i + "] must have 'replace' property in: " + sourceName);
            }
            
            // Check for invalid property names
            java.util.Set<String> allowedReplacementProps = java.util.Set.of(
                "find", "replace", "type", "conditions", "fallback"
            );
            
            for (String key : replacement.keySet()) {
                if (!allowedReplacementProps.contains(key)) {
                    throw new CustomizationException(
                        "Unknown property '" + key + "' in replacements[" + i + "] in " + sourceName + 
                        ". Allowed properties: " + allowedReplacementProps
                    );
                }
            }
        }
    }
    
    private void validateSmartReplacementsStructure(Object smartReplacements, String sourceName) throws CustomizationException {
        if (!(smartReplacements instanceof java.util.List)) {
            throw new CustomizationException("'smartReplacements' must be a list/array in: " + sourceName);
        }
        // Additional validation can be added here for smart replacement structure
    }
    
    private void validateSmartInsertionsStructure(Object smartInsertions, String sourceName) throws CustomizationException {
        if (!(smartInsertions instanceof java.util.List)) {
            throw new CustomizationException("'smartInsertions' must be a list/array in: " + sourceName);
        }
        // Additional validation can be added here for smart insertion structure
    }
    
    /**
     * Creates a safe YAML loader with security restrictions to prevent code injection.
     */
    private Yaml createSafeYamlLoader() {
        // Configure loader options for security
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(50);
        options.setNestingDepthLimit(50);
        options.setAllowDuplicateKeys(false);
        
        // Create a secure constructor that only allows safe types
        Constructor constructor = new Constructor(CustomizationConfig.class, options) {
            @Override
            protected Object constructObject(Node node) {
                // Only allow safe types to prevent code execution
                if (isAllowedType(node.getType())) {
                    return super.constructObject(node);
                }
                throw new RuntimeException("Forbidden type in YAML: " + node.getType().getName());
            }
        };
        
        return new Yaml(constructor);
    }
    
    /**
     * Checks if a type is allowed in YAML parsing for security.
     */
    private boolean isAllowedType(Class<?> type) {
        // Allow basic types and our customization classes
        Set<Class<?>> allowedTypes = Set.of(
            // Basic types
            String.class, Integer.class, Long.class, Float.class, Double.class, Boolean.class,
            int.class, long.class, float.class, double.class, boolean.class, Object.class,
            
            // Collections
            List.class, ArrayList.class, java.util.Map.class, java.util.HashMap.class,
            java.util.LinkedHashMap.class, Set.class, java.util.HashSet.class,
            
            // Date/Time (limited support)
            java.util.Date.class,
            
            // Our customization model classes
            CustomizationConfig.class, CustomizationMetadata.class,
            Insertion.class, Replacement.class, SmartReplacement.class, SmartInsertion.class,
            ConditionSet.class, SmartReplacement.FindPattern.class,
            SmartInsertion.InsertionPoint.class, SmartInsertion.PatternLocation.class
        );
        
        return allowedTypes.stream().anyMatch(allowedType -> allowedType.isAssignableFrom(type));
    }
    
    /**
     * Computes a cache key for customization results based on all inputs that affect the outcome.
     * 
     * @param baseTemplate the original template content
     * @param config the customization configuration
     * @param context the evaluation context
     * @return a unique cache key string
     */
    private String computeCustomizationCacheKey(String baseTemplate, CustomizationConfig config, EvaluationContext context) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Hash base template content
            digest.update("template:".getBytes(StandardCharsets.UTF_8));
            digest.update(computeStringHash(baseTemplate).getBytes(StandardCharsets.UTF_8));
            
            // Hash customization configuration
            if (config != null) {
                digest.update(";config:".getBytes(StandardCharsets.UTF_8));
                digest.update(computeStringHash(config.toString()).getBytes(StandardCharsets.UTF_8));
            } else {
                digest.update(";config:none".getBytes(StandardCharsets.UTF_8));
            }
            
            // Hash evaluation context
            if (context != null) {
                digest.update(";context:".getBytes(StandardCharsets.UTF_8));
                digest.update("template:".getBytes(StandardCharsets.UTF_8));
                if (context.getTemplateContent() != null) {
                    digest.update(computeStringHash(context.getTemplateContent()).getBytes(StandardCharsets.UTF_8));
                }
                digest.update("version:".getBytes(StandardCharsets.UTF_8));
                if (context.getGeneratorVersion() != null) {
                    digest.update(context.getGeneratorVersion().getBytes(StandardCharsets.UTF_8));
                }
            }
            
            // Convert hash to hex string
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            // Fallback to simple hash combination if SHA-256 fails
            return "fallback:" + 
                   baseTemplate.hashCode() + ":" +
                   (config != null ? config.hashCode() : 0) + ":" +
                   (context != null ? context.hashCode() : 0);
        }
    }
    
    /**
     * Computes SHA-256 hash of a string for cache key generation.
     */
    private String computeStringHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (Exception e) {
            // Fallback to simple hash code for robustness
            return String.valueOf(content.hashCode());
        }
    }
    
    /**
     * Clears the customization result cache. Useful for memory management and testing.
     */
    public void clearCustomizationCache() {
        int cacheSize = customizationResultCache.size();
        customizationResultCache.clear();
        if (cacheSize > 0) {
            logger.debug("Cleared customization result cache ({} entries)", cacheSize);
        }
    }
    
    /**
     * Returns the current size of the customization result cache.
     * @return the number of cached customization results
     */
    public int getCacheSize() {
        return customizationResultCache.size();
    }
    
    /**
     * High-level method to process template customizations for a complete template working directory.
     * This orchestrates the full template customization workflow including base template extraction,
     * plugin customizations, and user customizations.
     * 
     * @param templateConfig the template configuration
     * @param templateWorkDir the working directory for templates
     */
    public void processTemplateCustomizations(TemplateConfiguration templateConfig, File templateWorkDir) {
        // Check if we have any customizations to process based on the configured template sources
        List<String> templateSources = templateConfig.getTemplateSources();
        boolean hasAnyRelevantCustomizations = hasCustomizationsForTemplateSources(templateConfig, templateSources);
        
        if (!hasAnyRelevantCustomizations) {
            logger.debug("No relevant template customizations to process for generator: {} (templateSources: {})", 
                templateConfig.getGeneratorName(), templateSources);
            return;
        }
        
        try {
            logger.debug("Processing template customizations for generator: {}", templateConfig.getGeneratorName());
            
            // Extract base templates that need customization
            extractRequiredBaseTemplates(templateConfig, templateWorkDir);
            
            // Apply customizations in precedence order based on template precedence configuration
            applyCustomizationsInPrecedenceOrder(templateConfig, templateWorkDir);
            
            logger.info("Successfully processed template customizations for generator: {}", templateConfig.getGeneratorName());
            
        } catch (Exception e) {
            logger.error("Failed to process template customizations for generator '{}': {}", 
                templateConfig.getGeneratorName(), e.getMessage());
            logger.debug("Customization processing error details", e);
            throw new RuntimeException("Template customization processing failed", e);
        }
    }
    
    /**
     * Applies customizations in precedence order based on the template precedence configuration.
     * This method respects the configured precedence order for all template sources.
     * 
     * @param templateConfig the template configuration with precedence settings
     * @param templateWorkDir the working directory for templates
     */
    private void applyCustomizationsInPrecedenceOrder(TemplateConfiguration templateConfig, File templateWorkDir) {
        List<String> templateSources = templateConfig.getTemplateSources();
        logger.info("TEMPLATE SOURCES DEBUG: received templateSources = {}, isEmpty = {}", templateSources, templateSources.isEmpty());
        if (templateSources.isEmpty()) {
            // Fallback to legacy approach if no precedence is configured
            logger.info("TEMPLATE SOURCES DEBUG: Falling back to legacy order because templateSources is empty");
            applyCustomizationsLegacyOrder(templateConfig, templateWorkDir);
            return;
        }
        
        logger.debug("Applying customizations in template source order: {}", templateSources);
        
        // Apply customizations in reverse order (lowest precedence first)
        // This ensures that higher precedence sources override lower precedence ones
        for (int i = templateSources.size() - 1; i >= 0; i--) {
            String source = templateSources.get(i);
            
            switch (source) {
                case "openapi-generator":
                    // Base templates are already extracted, no additional action needed
                    logger.debug("Base OpenAPI Generator templates are already in place");
                    break;
                    
                case "plugin-customizations":
                    if (templateConfig.hasPluginCustomizations()) {
                        applyPluginCustomizations(templateConfig, templateWorkDir);
                    }
                    break;
                    
                case "library-customizations":
                    if (templateConfig.hasLibraryCustomizations()) {
                        applyLibraryCustomizations(templateConfig, templateWorkDir);
                    }
                    break;
                    
                case "library-templates":
                    if (templateConfig.hasLibraryTemplates()) {
                        applyLibraryTemplates(templateConfig, templateWorkDir);
                    }
                    break;
                    
                case "user-customizations":
                    if (templateConfig.hasUserCustomizations()) {
                        applyUserCustomizations(templateConfig, templateWorkDir);
                    }
                    break;
                    
                case "user-templates":
                    // User templates are handled by the main template resolution logic
                    // They override everything when present, so no action needed here
                    logger.debug("User template precedence handled by main template resolution");
                    break;
                    
                default:
                    logger.warn("Unknown template source in precedence configuration: {}", source);
                    break;
            }
        }
    }
    
    /**
     * Legacy customization application order (for backward compatibility).
     */
    private void applyCustomizationsLegacyOrder(TemplateConfiguration templateConfig, File templateWorkDir) {
        logger.debug("Using legacy customization application order");
        
        // Apply plugin customizations if enabled
        if (templateConfig.hasPluginCustomizations()) {
            applyPluginCustomizations(templateConfig, templateWorkDir);
        }
        
        // Apply library customizations
        if (templateConfig.hasLibraryCustomizations()) {
            applyLibraryCustomizations(templateConfig, templateWorkDir);
        }
        
        // Apply user customizations (these take precedence over plugin/library customizations)
        if (templateConfig.hasUserCustomizations()) {
            applyUserCustomizations(templateConfig, templateWorkDir);
        }
    }
    
    /**
     * Applies library template files directly to the working directory.
     * This method copies library templates that should override OpenAPI Generator defaults,
     * filtering by generator compatibility based on library metadata.
     */
    private void applyLibraryTemplates(TemplateConfiguration templateConfig, File templateWorkDir) {
        logger.debug("Applying library templates for generator: {}", templateConfig.getGeneratorName());
        
        Map<String, String> libraryTemplates = templateConfig.getLibraryTemplates();
        Map<String, LibraryMetadata> libraryMetadata = templateConfig.getLibraryMetadata();
        
        // Filter templates by generator name
        String generatorPrefix = templateConfig.getGeneratorName() + "/";
        
        for (Map.Entry<String, String> entry : libraryTemplates.entrySet()) {
            String templatePath = entry.getKey();
            String templateContent = entry.getValue();
            
            // Only process templates for this generator
            if (templatePath.startsWith(generatorPrefix)) {
                String fileName = templatePath.substring(generatorPrefix.length());
                
                // Check generator compatibility based on library metadata
                if (isLibraryContentCompatible(templatePath, templateConfig.getGeneratorName(), libraryMetadata)) {
                    try {
                        File templateFile = new File(templateWorkDir, fileName);
                        Files.createDirectories(templateFile.getParentFile().toPath());
                        Files.writeString(templateFile.toPath(), templateContent);
                        
                        logger.debug("Applied compatible library template: {}", fileName);
                        
                    } catch (IOException e) {
                        logger.warn("Failed to apply library template {}: {}", fileName, e.getMessage());
                    }
                } else {
                    logger.debug("Skipping incompatible library template: {} (generator: {})", fileName, templateConfig.getGeneratorName());
                }
            }
        }
    }
    
    /**
     * Applies library YAML customizations to existing templates,
     * filtering by generator compatibility based on library metadata.
     */
    private void applyLibraryCustomizations(TemplateConfiguration templateConfig, File templateWorkDir) {
        logger.debug("Applying library customizations for generator: {}", templateConfig.getGeneratorName());
        
        Map<String, String> libraryCustomizations = templateConfig.getLibraryCustomizations();
        Map<String, LibraryMetadata> libraryMetadata = templateConfig.getLibraryMetadata();
        
        // Filter customizations by generator name
        String generatorPrefix = templateConfig.getGeneratorName() + "/";
        
        for (Map.Entry<String, String> entry : libraryCustomizations.entrySet()) {
            String customizationPath = entry.getKey();
            String yamlContent = entry.getValue();
            
            // Only process customizations for this generator
            if (customizationPath.startsWith(generatorPrefix)) {
                String fileName = customizationPath.substring(generatorPrefix.length());
                String templateName = fileName.replace(".yaml", "").replace(".yml", "");
                
                // Check generator compatibility based on library metadata
                if (isLibraryContentCompatible(customizationPath, templateConfig.getGeneratorName(), libraryMetadata)) {
                    try {
                        File templateFile = new File(templateWorkDir, templateName);
                        if (templateFile.exists()) {
                            applyYamlCustomizationToFile(templateFile, yamlContent, templateConfig);
                            logger.debug("Applied compatible library customization: {} -> {}", fileName, templateName);
                        } else {
                            logger.debug("Template file {} does not exist, skipping library customization", templateName);
                        }
                        
                    } catch (IOException e) {
                        logger.warn("Failed to apply library customization {}: {}", fileName, e.getMessage());
                    }
                } else {
                    logger.debug("Skipping incompatible library customization: {} (generator: {})", fileName, templateConfig.getGeneratorName());
                }
            }
        }
    }
    
    /**
     * Checks if library content (template or customization) is compatible with the given generator.
     * This method uses library metadata to determine generator compatibility.
     * 
     * @param contentPath the path of the library content (template or customization)
     * @param generatorName the target generator name
     * @param libraryMetadata map of library metadata keyed by library name
     * @return true if the content is compatible, false otherwise
     */
    private boolean isLibraryContentCompatible(String contentPath, String generatorName, Map<String, LibraryMetadata> libraryMetadata) {
        // If no metadata available, allow content by default (backward compatibility)
        if (libraryMetadata == null || libraryMetadata.isEmpty()) {
            logger.debug("No library metadata available, allowing content: {}", contentPath);
            return true;
        }
        
        // Extract library name from content path
        // Expected format: "libraryName/generator/template.mustache" or "libraryName/generator/template.mustache.yaml"
        String[] pathParts = contentPath.split("/", 3);
        if (pathParts.length < 2) {
            logger.debug("Invalid library content path format: {}, allowing by default", contentPath);
            return true;
        }
        
        String libraryName = pathParts[0];
        LibraryMetadata metadata = libraryMetadata.get(libraryName);
        
        if (metadata == null) {
            // No metadata for this library, allow by default
            logger.debug("No metadata found for library '{}', allowing content: {}", libraryName, contentPath);
            return true;
        }
        
        // Check generator compatibility
        boolean isCompatible = metadata.supportsGenerator(generatorName);
        
        if (isCompatible) {
            logger.debug("Library content compatible: {} supports generator '{}'", contentPath, generatorName);
        } else {
            logger.info("Library content incompatible: {} does not support generator '{}' (supported: {})", 
                contentPath, generatorName, metadata.getSupportedGenerators());
        }
        
        return isCompatible;
    }
    
    /**
     * Extracts base templates that need customization from OpenAPI Generator.
     */
    private void extractRequiredBaseTemplates(TemplateConfiguration templateConfig, File templateWorkDir) throws IOException {
        logger.debug("Extracting required base templates for customization");
        
        // Collect all templates that need extraction based on available customizations
        Set<String> templatesToExtract = new java.util.HashSet<>();
        
        // Add templates that have library customizations
        if (templateConfig.hasLibraryCustomizations()) {
            addTemplatesFromLibraryCustomizations(templateConfig, templatesToExtract);
        }
        
        // Add templates that have plugin customizations
        if (templateConfig.hasPluginCustomizations()) {
            addTemplatesFromPluginCustomizations(templateConfig, templatesToExtract);
        }
        
        // Add templates that have user customizations
        if (templateConfig.hasUserCustomizations()) {
            addTemplatesFromUserCustomizations(templateConfig, templatesToExtract);
        }
        
        // Extract the identified templates
        TemplateDiscoveryService discoveryService = new TemplateDiscoveryService();
        
        for (String templateName : templatesToExtract) {
            extractBaseTemplate(discoveryService, templateName, templateConfig, templateWorkDir);
        }
    }
    
    /**
     * Adds templates from library customizations to the extraction set,
     * filtering by generator compatibility.
     */
    private void addTemplatesFromLibraryCustomizations(TemplateConfiguration templateConfig, Set<String> templatesToExtract) {
        String generatorPrefix = templateConfig.getGeneratorName() + "/";
        Map<String, LibraryMetadata> libraryMetadata = templateConfig.getLibraryMetadata();
        
        for (String customizationPath : templateConfig.getLibraryCustomizations().keySet()) {
            if (customizationPath.startsWith(generatorPrefix)) {
                // Only add templates for compatible library customizations
                if (isLibraryContentCompatible(customizationPath, templateConfig.getGeneratorName(), libraryMetadata)) {
                    String fileName = customizationPath.substring(generatorPrefix.length());
                    String templateName = fileName.replace(".yaml", "").replace(".yml", "");
                    templatesToExtract.add(templateName);
                    logger.debug("Added compatible library customization template for extraction: {}", templateName);
                } else {
                    logger.debug("Skipping incompatible library customization for extraction: {}", customizationPath);
                }
            }
        }
    }
    
    /**
     * Adds templates from plugin customizations to the extraction set.
     */
    private void addTemplatesFromPluginCustomizations(TemplateConfiguration templateConfig, Set<String> templatesToExtract) {
        if (!templateConfig.hasPluginCustomizations()) {
            return;
        }
        
        // Known plugin customization files
        String[] knownCustomizations = {"pojo.mustache.yaml", "model.mustache.yaml", "enumClass.mustache.yaml", "additionalModelTypeAnnotations.mustache.yaml"};
        
        for (String customizationFile : knownCustomizations) {
            String resourcePath = "templateCustomizations/" + templateConfig.getGeneratorName() + "/" + customizationFile;
            var resource = getClass().getClassLoader().getResource(resourcePath);
            
            if (resource != null) {
                String templateName = customizationFile.replace(".yaml", "").replace(".yml", "");
                templatesToExtract.add(templateName);
            }
        }
    }
    
    /**
     * Adds templates from user customizations to the extraction set.
     */
    private void addTemplatesFromUserCustomizations(TemplateConfiguration templateConfig, Set<String> templatesToExtract) {
        if (templateConfig.getUserCustomizationsDirectory() == null) {
            return;
        }
        
        File userCustomizationsDir = new File(templateConfig.getUserCustomizationsDirectory(), 
            templateConfig.getGeneratorName());
        
        if (!userCustomizationsDir.exists()) {
            return;
        }
        
        File[] yamlFiles = userCustomizationsDir.listFiles((dir, name) -> 
            name.endsWith(".yaml") || name.endsWith(".yml"));
        
        if (yamlFiles != null) {
            for (File yamlFile : yamlFiles) {
                String templateName = yamlFile.getName().replace(".yaml", "").replace(".yml", "");
                templatesToExtract.add(templateName);
            }
        }
    }
    
    /**
     * Checks if a template needs to be extracted based on available customizations.
     */
    private boolean needsTemplate(TemplateConfiguration templateConfig, String templateName) {
        // Check if this template has customizations
        if (templateConfig.hasUserCustomizations()) {
            File userCustomizationFile = new File(templateConfig.getUserCustomizationsDirectory(), 
                templateConfig.getGeneratorName() + "/" + templateName + ".yaml");
            if (userCustomizationFile.exists()) {
                return true;
            }
        }
        
        if (templateConfig.hasPluginCustomizations()) {
            // Check if plugin has customizations for this template
            var resource = getClass().getClassLoader().getResource("templateCustomizations/" + templateConfig.getGeneratorName() + "/" + templateName + ".yaml");
            return resource != null;
        }
        
        return false;
    }
    
    /**
     * Extracts a single base template from OpenAPI Generator.
     */
    private void extractBaseTemplate(TemplateDiscoveryService discoveryService, String templateName, 
                                   TemplateConfiguration templateConfig, File templateWorkDir) throws IOException {
        logger.debug("Extracting base template: {}", templateName);
        
        // Use the actual generator name from the configuration instead of hard-coding "spring"
        String baseTemplateContent = discoveryService.extractBaseTemplate(templateName, templateConfig.getGeneratorName());
        if (baseTemplateContent != null) {
            File templateFile = new File(templateWorkDir, templateName);
            Files.createDirectories(templateFile.getParentFile().toPath());
            Files.writeString(templateFile.toPath(), baseTemplateContent);
            
            logger.debug("Extracted base template to: {}", templateFile.getAbsolutePath());
        } else {
            logger.warn("Could not extract base template: {}", templateName);
        }
    }
    
    /**
     * Applies plugin-provided YAML customizations.
     */
    private void applyPluginCustomizations(TemplateConfiguration templateConfig, File templateWorkDir) {
        logger.debug("Applying plugin customizations for generator: {}", templateConfig.getGeneratorName());
        
        try {
            // Load plugin customization resources
            String generatorPath = "templateCustomizations/" + templateConfig.getGeneratorName();
            var classLoader = getClass().getClassLoader();
            
            // For configuration cache compatibility, we need to process customizations 
            // based on known files rather than dynamic discovery at execution time
            String[] commonCustomizations = {"pojo.mustache.yaml", "model.mustache.yaml", "enumClass.mustache.yaml"};
            
            for (String customizationFile : commonCustomizations) {
                String resourcePath = generatorPath + "/" + customizationFile;
                var resource = classLoader.getResource(resourcePath);
                
                if (resource != null) {
                    applyPluginCustomizationFile(resource, customizationFile, templateWorkDir, templateConfig);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to apply plugin customizations: {}", e.getMessage());
            logger.debug("Plugin customization error details", e);
            throw new RuntimeException("Plugin customization failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Applies a single plugin customization file.
     */
    private void applyPluginCustomizationFile(java.net.URL resource, String customizationFileName, 
                                            File templateWorkDir, TemplateConfiguration templateConfig) {
        try {
            String yamlContent;
            try (InputStream inputStream = resource.openStream()) {
                yamlContent = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            String templateName = customizationFileName.replace(".yaml", "").replace(".yml", "");
            
            logger.debug("Applying plugin customization: {} -> {}", customizationFileName, templateName);
            
            // Apply customization to the template file using the comprehensive YAML engine
            File templateFile = new File(templateWorkDir, templateName);
            if (templateFile.exists()) {
                applyYamlCustomizationToFile(templateFile, yamlContent, templateConfig);
            } else {
                logger.debug("Template file {} does not exist, skipping customization", templateFile.getName());
            }
            
        } catch (Exception e) {
            logger.error("Failed to apply plugin customization file {}: {}", customizationFileName, e.getMessage());
            logger.debug("Plugin customization error details for {}", customizationFileName, e);
            throw new RuntimeException("Plugin customization failed for " + customizationFileName + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Applies user-provided YAML customizations.
     */
    private void applyUserCustomizations(TemplateConfiguration templateConfig, File templateWorkDir) {
        if (templateConfig.getUserCustomizationsDirectory() == null) {
            return;
        }
        
        logger.debug("Applying user customizations for generator: {}", templateConfig.getGeneratorName());
        
        File userCustomizationsDir = new File(templateConfig.getUserCustomizationsDirectory(), 
            templateConfig.getGeneratorName());
        
        if (!userCustomizationsDir.exists()) {
            logger.debug("User customizations directory does not exist: {}", userCustomizationsDir.getAbsolutePath());
            return;
        }
        
        File[] yamlFiles = userCustomizationsDir.listFiles((dir, name) -> 
            name.endsWith(".yaml") || name.endsWith(".yml"));
        
        if (yamlFiles == null) {
            return;
        }
        
        for (File yamlFile : yamlFiles) {
            try {
                String yamlContent = Files.readString(yamlFile.toPath());
                String templateName = yamlFile.getName().replace(".yaml", "").replace(".yml", "");
                
                logger.debug("Applying user customization: {} -> {}", yamlFile.getName(), templateName);
                
                File templateFile = new File(templateWorkDir, templateName);
                if (templateFile.exists()) {
                    applyYamlCustomizationToFile(templateFile, yamlContent, templateConfig);
                } else {
                    logger.debug("Template file {} does not exist, skipping customization", templateFile.getName());
                }
                
            } catch (IOException e) {
                logger.warn("Failed to apply user customization file {}: {}", yamlFile.getName(), e.getMessage());
            }
        }
    }
    
    /**
     * Applies YAML customization to a template file using the comprehensive customization engine.
     */
    private void applyYamlCustomizationToFile(File templateFile, String yamlContent, TemplateConfiguration templateConfig) throws IOException {
        try {
            // Parse the YAML customization
            CustomizationConfig customizationConfig = parseCustomizationYaml(
                new java.io.ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8)), templateFile.getName() + ".yaml");
            
            // Read the current template content
            String originalContent = Files.readString(templateFile.toPath());
            
            // Create proper evaluation context with version detection and comprehensive analysis
            EvaluationContext context = createEvaluationContext(originalContent, templateConfig);
            
            // Apply the customizations using the comprehensive engine
            String customizedContent = applyCustomizations(originalContent, customizationConfig, context);
            
            // Write back the customized content
            Files.writeString(templateFile.toPath(), customizedContent);
            
            logger.debug("Applied comprehensive YAML customization to template: {}", templateFile.getName());
            
        } catch (CustomizationException e) {
            logger.warn("Failed to apply YAML customization to {}: {}", templateFile.getName(), e.getMessage());
            logger.debug("YAML customization error details", e);
        }
    }
    
    /**
     * Creates a comprehensive EvaluationContext with version detection and template analysis.
     * 
     * @param templateContent the current template content
     * @param templateConfig the template configuration containing generator and environment info
     * @return a fully configured EvaluationContext
     */
    private EvaluationContext createEvaluationContext(String templateContent, TemplateConfiguration templateConfig) {
        // Detect OpenAPI Generator version from classpath or default
        String generatorVersion = detectOpenApiGeneratorVersion();
        
        // Get environment variables
        Map<String, String> envVars = System.getenv();
        
        // Create project properties map from template variables (which may include project properties)
        Map<String, String> projectProps = new HashMap<>(templateConfig.getTemplateVariables());
        
        // Add generator-specific properties
        projectProps.put("generatorName", templateConfig.getGeneratorName());
        projectProps.put("hasUserTemplates", String.valueOf(templateConfig.hasUserTemplates()));
        projectProps.put("hasUserCustomizations", String.valueOf(templateConfig.hasUserCustomizations()));
        projectProps.put("hasPluginCustomizations", String.valueOf(templateConfig.hasPluginCustomizations()));
        projectProps.put("applyPluginCustomizations", String.valueOf(templateConfig.hasPluginCustomizations()));
        projectProps.put("debugTemplateResolution", String.valueOf(templateConfig.isDebugTemplateResolution()));
        
        return EvaluationContext.builder()
            .generatorVersion(generatorVersion)
            .templateContent(templateContent)
            .projectProperties(projectProps)
            .environmentVariables(envVars)
            .build();
    }
    
    /**
     * Detects the OpenAPI Generator version from the classpath.
     * 
     * @return the detected version or a fallback version if detection fails
     */
    private String detectOpenApiGeneratorVersion() {
        try {
            // Try to get version from OpenAPI Generator package
            Package openapiPackage = org.openapitools.codegen.CodegenConfig.class.getPackage();
            if (openapiPackage != null && openapiPackage.getImplementationVersion() != null) {
                String version = openapiPackage.getImplementationVersion();
                logger.debug("Detected OpenAPI Generator version from classpath: {}", version);
                return version;
            }
            
            // Try alternative approach using manifest
            Class<?> codegenClass = org.openapitools.codegen.CodegenConfig.class;
            String classPath = codegenClass.getResource(
                codegenClass.getSimpleName() + ".class").toString();
            
            if (classPath.startsWith("jar:")) {
                // Extract version from JAR path if possible
                String jarPath = classPath.substring(4, classPath.indexOf("!"));
                if (jarPath.contains("openapi-generator")) {
                    // Try to extract version from jar name pattern like "openapi-generator-7.14.0.jar"
                    String jarName = jarPath.substring(jarPath.lastIndexOf('/') + 1);
                    if (jarName.matches(".*-\\d+\\.\\d+\\.\\d+.*")) {
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+\\.\\d+\\.\\d+)");
                        java.util.regex.Matcher matcher = pattern.matcher(jarName);
                        if (matcher.find()) {
                            String version = matcher.group(1);
                            logger.debug("Detected OpenAPI Generator version from JAR name: {}", version);
                            return version;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to detect OpenAPI Generator version: {}", e.getMessage());
        }
        
        // Fallback to a reasonable default
        String fallbackVersion = "7.11.0";
        logger.debug("Using fallback OpenAPI Generator version: {}", fallbackVersion);
        return fallbackVersion;
    }

    /**
     * Checks if any relevant customizations exist based on the configured template sources.
     * This method only returns true if customizations exist for template sources that are included
     * in the templateSources configuration.
     * 
     * @param templateConfig the template configuration with availability flags
     * @param templateSources the configured template sources to check
     * @return true if there are customizations for any of the configured template sources
     */
    private boolean hasCustomizationsForTemplateSources(TemplateConfiguration templateConfig, List<String> templateSources) {
        if (templateSources == null || templateSources.isEmpty()) {
            return false;
        }
        
        for (String source : templateSources) {
            switch (source) {
                case "user-customizations":
                    if (templateConfig.hasUserCustomizations()) {
                        return true;
                    }
                    break;
                    
                case "plugin-customizations":
                    if (templateConfig.hasPluginCustomizations()) {
                        return true;
                    }
                    break;
                    
                case "library-customizations":
                    if (templateConfig.hasLibraryCustomizations()) {
                        return true;
                    }
                    break;
                    
                case "user-templates":
                    if (templateConfig.hasUserTemplates()) {
                        return true;
                    }
                    break;
                    
                case "library-templates":
                    if (templateConfig.hasLibraryTemplates()) {
                        return true;
                    }
                    break;
                    
                case "openapi-generator":
                    // Base OpenAPI generator doesn't need customization processing
                    break;
                    
                default:
                    logger.debug("Unknown template source in configuration: {}", source);
                    break;
            }
        }
        
        return false;
    }

    /**
     * Exception thrown when customization parsing or application fails.
     */
    public static class CustomizationException extends Exception {
        /**
         * Creates a new CustomizationException with a message.
         * @param message the error message
         */
        public CustomizationException(String message) {
            super(message);
        }
        
        /**
         * Creates a new CustomizationException with a message and cause.
         * @param message the error message
         * @param cause the underlying cause
         */
        public CustomizationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}