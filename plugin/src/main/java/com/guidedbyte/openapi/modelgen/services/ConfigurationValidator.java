package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.DefaultConfig;
import com.guidedbyte.openapi.modelgen.OpenApiModelGenExtension;
import com.guidedbyte.openapi.modelgen.SpecConfig;
import com.guidedbyte.openapi.modelgen.constants.PluginConstants;
import com.guidedbyte.openapi.modelgen.constants.TemplateSourceType;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.file.ProjectLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration-cache compatible service for validating OpenAPI Model Generator plugin configuration.
 * This service validates extension configuration, default settings, spec configurations, and library settings.
 */
public class ConfigurationValidator implements Serializable {
    
    /**
     * Constructs a new ConfigurationValidator.
     */
    public ConfigurationValidator() {
        // Default constructor
    }
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);
    
    // Java reserved words for package name validation
    private static final Set<String> JAVA_RESERVED_WORDS = new HashSet<>(Arrays.asList(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
        "volatile", "while", "true", "false", "null"
    ));
    
    /**
     * Validates the complete extension configuration including defaults, specs, and library settings.
     *
     * @param project the Gradle project
     * @param projectLayout the project layout for path resolution
     * @param extension the extension to validate
     * @throws InvalidUserDataException if validation fails
     */
    public void validateExtensionConfiguration(Project project, ProjectLayout projectLayout, OpenApiModelGenExtension extension) {
        List<String> errors = new ArrayList<>();
        
        // Validate defaults
        validateDefaultConfiguration(extension.getDefaults(), errors);
        
        // Validate individual spec configurations
        Map<String, SpecConfig> specs = extension.getSpecs();
        if (specs != null && !specs.isEmpty()) {
            // Validate spec names
            validateSpecNames(specs, errors);
            
            // Validate each spec configuration
            for (Map.Entry<String, SpecConfig> entry : specs.entrySet()) {
                validateSpecConfiguration(projectLayout, entry.getKey(), entry.getValue(), extension.getDefaults(), errors);
            }
            
            // Validate library configuration if enabled
            validateLibraryConfiguration(project, projectLayout, extension.getDefaults(), specs, errors);
        }
        // Note: Empty specs are allowed - individual tasks will validate spec requirements at execution time
        
        // Use ErrorHandlingUtils for consistent validation error handling
        ErrorHandlingUtils.validateOrThrow(errors, "Configuration", ErrorHandlingUtils.CONFIG_GUIDANCE);
        
        logger.debug("Extension configuration validation completed successfully");
    }
    
    /**
     * Validates default configuration settings.
     * 
     * @param defaults the default configuration to validate
     * @param errors the list to collect validation errors
     */
    public void validateDefaultConfiguration(DefaultConfig defaults, List<String> errors) {
        if (defaults == null) {
            return; // No defaults to validate
        }
        
        // Validate output directory
        if (defaults.getOutputDir().isPresent()) {
            String outputDir = defaults.getOutputDir().get();
            if (outputDir == null || outputDir.trim().isEmpty()) {
                errors.add("defaults.outputDir cannot be empty");
            }
        }
        
        // Validate template directory
        if (defaults.getUserTemplateDir().isPresent()) {
            String templateDir = defaults.getUserTemplateDir().get();
            if (templateDir != null && !templateDir.trim().isEmpty()) {
                File templateDirFile = new File(templateDir);
                if (templateDirFile.exists() && !templateDirFile.isDirectory()) {
                    errors.add("defaults.userTemplateDir is not a directory: " + templateDir);
                }
                // Note: Missing directories are allowed - plugin will fall back to built-in templates
            }
        }
        
        // Note: Template directories are validated above in the original validation block
        
        // Validate config options
        if (defaults.getConfigOptions().isPresent()) {
            validateConfigOptions(defaults.getConfigOptions().get(), "defaults", errors);
        }
        
        // Validate library-related settings
        validateLibrarySettings(defaults, errors);
    }
    
    /**
     * Validates an individual spec configuration.
     * 
     * @param projectLayout the Gradle project layout for path resolution
     * @param specName the name of the specification being validated
     * @param specConfig the specification configuration to validate
     * @param defaults the default configuration for fallback values
     * @param errors the list to collect validation errors
     */
    public void validateSpecConfiguration(ProjectLayout projectLayout, String specName, SpecConfig specConfig, 
                                        DefaultConfig defaults, List<String> errors) {
        String specPrefix = "spec '" + specName + "'";
        
        // Validate spec name format
        if (specName == null || specName.trim().isEmpty()) {
            errors.add("Spec name cannot be empty");
            return; // Can't continue without a valid spec name
        }
        
        if (!specName.matches("[a-zA-Z][a-zA-Z0-9_]*")) {
            errors.add(specPrefix + ": spec name must start with a letter and contain only letters, numbers, and underscores");
        }
        
        // Validate required inputSpec
        if (!specConfig.getInputSpec().isPresent() || specConfig.getInputSpec().get() == null || 
            specConfig.getInputSpec().get().trim().isEmpty()) {
            errors.add(specPrefix + ": inputSpec is required and cannot be empty");
        } else {
            String inputSpecPath = specConfig.getInputSpec().get();
            File inputSpecFile = projectLayout.getProjectDirectory().file(inputSpecPath).getAsFile();
            if (!inputSpecFile.exists()) {
                errors.add(String.format("%s: inputSpec file does not exist: %s", specPrefix, inputSpecPath));
            } else if (!inputSpecFile.isFile()) {
                errors.add(String.format("%s: inputSpec must be a file, not a directory: %s", specPrefix, inputSpecPath));
            } else if (!inputSpecFile.canRead()) {
                errors.add(String.format("%s: inputSpec file is not readable: %s", specPrefix, inputSpecPath));
            } else {
                // Validate file extension (case-insensitive)
                String fileName = inputSpecFile.getName();
                if (!StringUtils.endsWithIgnoreCase(fileName, ".yaml") && 
                    !StringUtils.endsWithIgnoreCase(fileName, ".yml") && 
                    !StringUtils.endsWithIgnoreCase(fileName, ".json")) {
                    errors.add(specPrefix + ": inputSpec should be a YAML (.yaml/.yml) or JSON (.json) file (case-insensitive): " + inputSpecPath);
                }
            }
        }
        
        // Validate required modelPackage - check spec first, then fall back to defaults
        if (!specConfig.getModelPackage().isPresent() || specConfig.getModelPackage().get() == null || 
            specConfig.getModelPackage().get().trim().isEmpty()) {
            // modelPackage is required for each spec - no default fallback available
            errors.add(specPrefix + ": modelPackage is required and cannot be empty");
        } else {
            String packageName = specConfig.getModelPackage().get();
            if (!isValidJavaPackageName(packageName)) {
                errors.add(String.format("%s: modelPackage is not a valid Java package name: %s", specPrefix, packageName));
            }
        }
        
        // Validate optional outputDir
        if (specConfig.getOutputDir().isPresent()) {
            String outputDir = specConfig.getOutputDir().get();
            if (outputDir != null && outputDir.trim().isEmpty()) {
                errors.add(specPrefix + ": outputDir cannot be empty when specified (use null to inherit from defaults)");
            }
        }
        
        // Validate optional userTemplateDir
        if (specConfig.getUserTemplateDir().isPresent()) {
            String templateDir = specConfig.getUserTemplateDir().get();
            if (templateDir != null && !templateDir.trim().isEmpty()) {
                File templateDirFile = new File(templateDir);
                if (templateDirFile.exists() && !templateDirFile.isDirectory()) {
                    errors.add(specPrefix + ": userTemplateDir is not a directory: " + templateDir);
                }
                // Note: Missing directories are allowed - plugin will fall back to built-in templates
            }
        }
        
        // Validate optional modelNameSuffix
        if (specConfig.getModelNameSuffix().isPresent()) {
            String suffix = specConfig.getModelNameSuffix().get();
            if (suffix != null && !suffix.trim().isEmpty() && !suffix.matches("[A-Za-z0-9_]*")) {
                errors.add(specPrefix + ": modelNameSuffix can only contain letters, numbers, and underscores: " + suffix);
            }
        }
        
        // Validate optional modelNamePrefix
        if (specConfig.getModelNamePrefix().isPresent()) {
            String prefix = specConfig.getModelNamePrefix().get();
            if (prefix != null && !prefix.trim().isEmpty() && !prefix.matches("[A-Za-z0-9_]*")) {
                errors.add(specPrefix + ": modelNamePrefix can only contain letters, numbers, and underscores: " + prefix);
            }
        }
        
        // Validate config options
        if (specConfig.getConfigOptions().isPresent()) {
            validateConfigOptions(specConfig.getConfigOptions().get(), specPrefix, errors);
        }
    }
    
    /**
     * Validates OpenAPI Generator config options.
     * @param configOptions the config options to validate
     * @param context the validation context
     * @param errors the list to add validation errors to
     */
    public void validateConfigOptions(Map<String, String> configOptions, String context, List<String> errors) {
        if (configOptions == null) return;
        
        // Check for null or empty values (except for additionalModelTypeAnnotations which can be empty)
        for (Map.Entry<String, String> entry : configOptions.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.trim().isEmpty()) {
                // Allow additionalModelTypeAnnotations to be empty - users may not want any additional annotations
                if (!"additionalModelTypeAnnotations".equals(key)) {
                    errors.add(String.format("%s.configOptions['%s'] cannot be null or empty", context, key));
                }
            }
        }
        
        // Validate Spring Boot 3 and Jakarta EE compatibility (case-insensitive)
        if (StringUtils.equalsIgnoreCase(configOptions.get("useSpringBoot3"), "false") && 
            StringUtils.equalsIgnoreCase(configOptions.get("useJakartaEe"), "true")) {
            errors.add(context + ".configOptions: useJakartaEe=true requires useSpringBoot3=true (case-insensitive)");
        }
        
        // Validate date library
        String dateLibrary = configOptions.get("dateLibrary");
        if (dateLibrary != null && !Arrays.asList("java8", "legacy", "joda", "java8-localdatetime").contains(dateLibrary)) {
            logger.warn("{}. configOptions.dateLibrary='{}' is not a standard value. Expected: java8, legacy, joda, java8-localdatetime", 
                       context, dateLibrary);
        }
    }
    
    /**
     * Validates spec names for uniqueness and valid identifier format.
     * 
     * @param specs the map of specification configurations to validate
     * @param errors the list to collect validation errors
     */
    public void validateSpecNames(Map<String, SpecConfig> specs, List<String> errors) {
        Set<String> seenNames = new HashSet<>();
        
        for (String specName : specs.keySet()) {
            // Check for null or empty names
            if (specName == null || specName.trim().isEmpty()) {
                errors.add("Spec names cannot be null or empty");
                continue;
            }
            
            // Check for duplicates (case-insensitive)
            String lowerName = StringUtils.toRootLowerCase(specName);
            if (seenNames.contains(lowerName)) {
                errors.add(String.format("Duplicate spec name (case-insensitive): %s", specName));
            } else {
                seenNames.add(lowerName);
            }
            
            // Check for valid identifier format (for task names)
            if (!specName.matches("^[a-zA-Z][a-zA-Z0-9]*$")) {
                errors.add(String.format("Spec name '%s' must be a valid identifier (letters and numbers only, starting with a letter)", specName));
            }
        }
    }
    
    /**
     * Validates library configuration when library features are enabled.
     * 
     * @param project the Gradle project
     * @param projectLayout the project layout for path resolution
     * @param defaults the default configuration
     * @param specs the map of specification configurations
     * @param errors the list to collect validation errors
     */
    public void validateLibraryConfiguration(Project project, ProjectLayout projectLayout, DefaultConfig defaults, 
                                           Map<String, SpecConfig> specs, List<String> errors) {
        // Only validate if library sources are included in templateSources
        boolean needsLibraryValidation = false;
        if (defaults != null && defaults.getTemplateSources().isPresent()) {
            java.util.List<String> templateSources = defaults.getTemplateSources().get();
            needsLibraryValidation = templateSources.contains(TemplateSourceType.LIBRARY_TEMPLATES.toString()) || 
                                   templateSources.contains(TemplateSourceType.LIBRARY_CUSTOMIZATIONS.toString());
        }
        
        if (!needsLibraryValidation) {
            return; // No library sources enabled in templateSources
        }
        
        // Validate library settings
        validateLibrarySettings(defaults, errors);
        
        // Get library files and validate metadata
        Set<File> libraryFiles = new HashSet<>();
        try {
            org.gradle.api.artifacts.Configuration customizationsConfig = 
                project.getConfigurations().getByName("openapiCustomizations");
            
            if (customizationsConfig.getFiles().isEmpty()) {
                logger.debug("Library template sources are included in templateSources but no openapiCustomizations dependencies found - library sources will be skipped");
                return; // Skip library validation gracefully
            }
            
            libraryFiles.addAll(customizationsConfig.getFiles());
            validateLibraryMetadata(project, libraryFiles, specs, errors);
            
        } catch (Exception e) {
            // Use ErrorHandlingUtils for consistent validation error handling
            String errorMessage = ErrorHandlingUtils.formatLibraryError("configuration", 
                "validation failed: " + e.getMessage());
            logger.debug("Error validating library configuration: {}", e.getMessage(), e);
            errors.add(errorMessage);
        }
    }
    
    /**
     * Validates library metadata for compatibility.
     */
    /**
     * Validates library metadata for compatibility.
     * 
     * @param project the Gradle project
     * @param libraryFiles the set of library files to validate
     * @param specs the map of specification configurations
     * @param errors the list to collect validation errors
     */
    public void validateLibraryMetadata(Project project, Set<File> libraryFiles, Map<String, SpecConfig> specs, List<String> errors) {
        if (libraryFiles.isEmpty()) return;
        
        try {
            logger.debug("Validating library metadata for {} files", libraryFiles.size());
            
            // Extract library content and metadata using LibraryTemplateExtractor
            LibraryTemplateExtractor extractor = new LibraryTemplateExtractor();
            LibraryTemplateExtractor.LibraryExtractionResult result = extractor.extractFromLibraries(
                project.getObjects().fileCollection().from(libraryFiles));
            
            Map<String, LibraryMetadata> libraryMetadata = result.getMetadata();
            
            if (libraryMetadata.isEmpty()) {
                logger.debug("No library metadata found in {} files", libraryFiles.size());
                return;
            }
            
            // Validate each library's metadata
            for (Map.Entry<String, LibraryMetadata> entry : libraryMetadata.entrySet()) {
                String libraryName = entry.getKey();
                LibraryMetadata metadata = entry.getValue();
                
                logger.debug("Validating metadata for library: {}", libraryName);
                validateSingleLibraryMetadata(libraryName, metadata, specs, errors);
            }
            
            logger.debug("Library metadata validation completed for {} libraries", libraryMetadata.size());
            
        } catch (Exception e) {
            // Use ErrorHandlingUtils for consistent validation error handling
            String errorMessage = ErrorHandlingUtils.formatLibraryError("metadata extraction", 
                "failed: " + e.getMessage());
            logger.debug("Error extracting library metadata: {}", e.getMessage(), e);
            errors.add(errorMessage);
        }
    }
    
    /**
     * Validates a single library's metadata for compatibility.
     * 
     * @param libraryName the name of the library being validated
     * @param metadata the library metadata to validate
     * @param specs the map of specification configurations
     * @param errors the list to collect validation errors
     */
    public void validateSingleLibraryMetadata(String libraryName, LibraryMetadata metadata, 
                                            Map<String, SpecConfig> specs, List<String> errors) {
        if (metadata == null) return;
        
        // Validate generator compatibility
        if (metadata.getSupportedGenerators() != null && !metadata.getSupportedGenerators().isEmpty()) {
            Set<String> usedGenerators = new HashSet<>();
            
            // Collect all generators used in specs (default is "spring")
            // Note: SpecConfig doesn't have generatorName, so we use the default
            for (SpecConfig spec : specs.values()) {
                String generatorName = PluginConstants.DEFAULT_GENERATOR_NAME; // Default generator - specs don't override this currently
                usedGenerators.add(generatorName);
            }
            
            // Check if library supports all used generators
            for (String generator : usedGenerators) {
                if (!metadata.getSupportedGenerators().contains(generator)) {
                    errors.add(String.format("Library '%s' does not support generator '%s'. Supported generators: %s", 
                              libraryName, generator, metadata.getSupportedGenerators()));
                }
            }
        }
        
        // Validate minimum plugin version
        if (metadata.getMinPluginVersion() != null) {
            String currentVersion = getCurrentPluginVersion();
            if (currentVersion != null && !isVersionCompatible(currentVersion, metadata.getMinPluginVersion())) {
                errors.add(String.format("Library '%s' requires plugin version %s+ (current: %s)", 
                          libraryName, metadata.getMinPluginVersion(), currentVersion));
            }
        }
        
        // Validate OpenAPI Generator version requirements
        if (metadata.getMinOpenApiGeneratorVersion() != null) {
            logger.info("Library '{}' requires OpenAPI Generator version {}+", libraryName, metadata.getMinOpenApiGeneratorVersion());
            // Note: We don't validate this here as generator version detection is complex and done elsewhere
        }
        
        logger.debug("Library '{}' metadata validation completed", libraryName);
    }
    
    /**
     * Validates library settings for consistency.
     * 
     * @param defaults the default configuration to validate
     * @param errors the list to collect validation errors
     */
    public void validateLibrarySettings(DefaultConfig defaults, List<String> errors) {
        if (defaults == null) return;
        
        // Library validation is now handled automatically by templateSources inclusion
        // No additional validation needed since library sources are enabled by including them in templateSources
    }
    
    /**
     * Validates Java package name format.
     */
    /**
     * Checks if the given string is a valid Java package name.
     * 
     * @param packageName the package name to validate
     * @return true if the package name is valid, false otherwise
     */
    public boolean isValidJavaPackageName(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = packageName.split("\\.");
        for (String part : parts) {
            if (part.isEmpty() || !part.matches("^[a-zA-Z_$][a-zA-Z0-9_$]*$") || isJavaReservedWord(part)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if a word is a Java reserved word.
     */
    /**
     * Checks if the given string is a Java reserved word.
     * 
     * @param word the word to check
     * @return true if the word is a Java reserved word, false otherwise
     */
    public boolean isJavaReservedWord(String word) {
        return JAVA_RESERVED_WORDS.contains(word);
    }
    
    /**
     * Gets the current plugin version for validation.
     */
    private String getCurrentPluginVersion() {
        // This would need to be implemented to get actual plugin version
        // For now, return null to skip version validation
        return null;
    }
    
    /**
     * Checks if current version meets minimum requirement.
     */
    private boolean isVersionCompatible(String currentVersion, String minVersion) {
        // Simple version comparison - in reality this should use semantic versioning
        return currentVersion.compareTo(minVersion) >= 0;
    }
}