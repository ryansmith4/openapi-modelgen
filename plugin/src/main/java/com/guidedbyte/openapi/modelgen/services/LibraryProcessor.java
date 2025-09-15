package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.DefaultConfig;
import com.guidedbyte.openapi.modelgen.OpenApiModelGenExtension;
import com.guidedbyte.openapi.modelgen.OpenApiModelGenPlugin;
import com.guidedbyte.openapi.modelgen.SpecConfig;
import com.guidedbyte.openapi.modelgen.constants.PluginConstants;
import com.guidedbyte.openapi.modelgen.constants.TemplateSourceType;
import com.guidedbyte.openapi.modelgen.utils.VersionUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Configuration-cache compatible service for processing OpenAPI template libraries.
 * Handles library dependency processing, metadata validation, and compatibility checking.
 */
public class LibraryProcessor implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(LibraryProcessor.class);
    
    private final transient LibraryTemplateExtractor libraryExtractor;
    
    /**
     * Creates a new LibraryProcessor with default dependencies.
     */
    public LibraryProcessor() {
        this.libraryExtractor = new LibraryTemplateExtractor();
    }
    
    /**
     * Creates a new LibraryProcessor with custom library extractor.
     *
     * @param libraryExtractor the library template extractor to use
     */
    public LibraryProcessor(LibraryTemplateExtractor libraryExtractor) {
        this.libraryExtractor = libraryExtractor;
    }
    
    /**
     * Determines if library processing should be performed based on configuration.
     *
     * @param extension the plugin extension
     * @param customizationsConfig the openapiCustomizations configuration
     * @return true if libraries should be processed
     */
    public boolean shouldProcessLibraries(OpenApiModelGenExtension extension, Configuration customizationsConfig) {
        // Check if customizations configuration exists and has dependencies
        if (customizationsConfig == null || customizationsConfig.getDependencies().isEmpty()) {
            logger.debug("No openapiCustomizations dependencies found, skipping library processing");
            return false;
        }
        
        // Check if any library sources are enabled in templateSources
        DefaultConfig defaults = extension.getDefaults();
        if (defaults != null && defaults.getTemplateSources().isPresent()) {
            java.util.List<String> templateSources = defaults.getTemplateSources().get();
            boolean hasLibrarySources = templateSources.contains(TemplateSourceType.LIBRARY_TEMPLATES.toString()) || 
                                       templateSources.contains(TemplateSourceType.LIBRARY_CUSTOMIZATIONS.toString());
            
            if (hasLibrarySources) {
                logger.debug("Library sources enabled in templateSources: {}", templateSources);
                return true;
            }
        }
        
        logger.debug("No library features enabled, skipping library processing");
        return false;
    }
    
    /**
     * Processes library dependencies and extracts metadata for validation.
     *
     * @param project the Gradle project
     * @param customizationsConfig the openapiCustomizations configuration
     * @param extension the plugin extension
     */
    public void processLibraryDependencies(Project project, Configuration customizationsConfig, OpenApiModelGenExtension extension) {
        if (!shouldProcessLibraries(extension, customizationsConfig)) {
            return;
        }
        
        try {
            // Resolve dependencies at configuration time
            Set<File> libraryFiles = customizationsConfig.resolve();
            
            if (libraryFiles.isEmpty()) {
                logger.debug("No template libraries found in openapiCustomizations configuration");
                return;
            }
            
            logger.info("Processing {} template library dependencies", libraryFiles.size());
            
            // Create the extractor service
            LibraryTemplateExtractor extractor = new LibraryTemplateExtractor();
            
            // Convert Set<File> to FileCollection
            org.gradle.api.file.FileCollection libraries = project.files(libraryFiles);
            
            // Extract templates and customizations from all libraries
            LibraryTemplateExtractor.LibraryExtractionResult libraryContent = extractor.extractFromLibraries(libraries);
            
            if (libraryContent.isEmpty()) {
                logger.warn("No templates or customizations found in library dependencies");
                return;
            }
            
            logger.info("Extracted {} templates and {} customizations from libraries",
                libraryContent.getTemplates().size(), 
                libraryContent.getCustomizations().size());
            
            // Validate library compatibility with current configuration
            LibraryValidationResult validationResult = validateLibraryCompatibility(project, customizationsConfig, extension.getSpecs());
            if (validationResult.hasIssues()) {
                // Use ErrorHandlingUtils for consistent validation error handling
                ErrorHandlingUtils.validateOrThrow(validationResult.getIssues(), 
                    "Library compatibility", ErrorHandlingUtils.LIBRARY_GUIDANCE);
            }
            
            // Additional validation: Check library version compatibility with detected OpenAPI Generator version
            // Note: This requires the main plugin's validation method due to version detection complexity
            logger.debug("Library metadata extracted successfully - version validation will be performed by main plugin");
            
            // Store extracted content in extension for later use by tasks
            extension.setLibraryContent(libraryContent);
            
            // Log details if debug is enabled
            if (logger.isDebugEnabled()) {
                libraryContent.getTemplates().forEach((path, content) -> 
                    logger.debug("Library template: {}", path));
                libraryContent.getCustomizations().forEach((path, content) -> 
                    logger.debug("Library customization: {}", path));
            }
            
            logger.info("Library dependency processing completed successfully");
            
        } catch (Exception e) {
            logger.error("Failed to process template libraries", e);
            // Use ErrorHandlingUtils for consistent error creation
            throw ErrorHandlingUtils.createConfigurationError(
                "Failed to process template libraries: " + e.getMessage(), 
                ErrorHandlingUtils.LIBRARY_GUIDANCE);
        }
    }
    
    /**
     * Extracts library content (templates and customizations) for use during generation.
     *
     * @param project the Gradle project
     * @param customizationsConfig the openapiCustomizations configuration
     * @param generatorName the OpenAPI generator name
     * @return library content organized by type
     */
    public LibraryContent extractLibraryContent(Project project, Configuration customizationsConfig, String generatorName) {
        LibraryContent content = new LibraryContent();
        
        if (!customizationsConfig.getDependencies().isEmpty()) {
            try {
                Set<File> libraryFiles = customizationsConfig.getFiles();
                
                // Extract library content using LibraryTemplateExtractor
                LibraryTemplateExtractor.LibraryExtractionResult result = libraryExtractor.extractFromLibraries(customizationsConfig);
                logger.info("Extracted {} templates and {} customizations from {} files for generator '{}'", 
                           result.getTemplates().size(), result.getCustomizations().size(), libraryFiles.size(), generatorName);
                
                // Convert to LibraryContent format
                content.setTemplates(result.getTemplates());
                content.setCustomizations(result.getCustomizations());
                content.setMetadata(result.getMetadata());
                
            } catch (Exception e) {
                logger.warn("Error extracting library content: {}", e.getMessage(), e);
                // Use ErrorHandlingUtils for consistent error handling
                String errorMessage = ErrorHandlingUtils.formatLibraryError("content extraction", 
                    "failed: " + e.getMessage());
                throw new RuntimeException(errorMessage, e);
            }
        }
        
        return content;
    }
    
    /**
     * Validates library compatibility with current configuration.
     *
     * @param project the Gradle project
     * @param customizationsConfig the openapiCustomizations configuration
     * @param specs the spec configurations to validate against
     * @return validation result with any compatibility issues
     */
    public LibraryValidationResult validateLibraryCompatibility(Project project, Configuration customizationsConfig, 
                                                               Map<String, SpecConfig> specs) {
        LibraryValidationResult result = new LibraryValidationResult();
        
        if (customizationsConfig.getDependencies().isEmpty()) {
            return result; // No libraries to validate
        }
        
        try {
            Set<File> libraryFiles = customizationsConfig.getFiles();
            
            // Extract library metadata and validate compatibility
            LibraryTemplateExtractor.LibraryExtractionResult extractionResult = libraryExtractor.extractFromLibraries(customizationsConfig);
            logger.debug("Validating library compatibility for {} files with {} metadata entries", 
                        libraryFiles.size(), extractionResult.getMetadata().size());
            
            // Validate each library's metadata
            for (Map.Entry<String, LibraryMetadata> entry : extractionResult.getMetadata().entrySet()) {
                String libraryName = entry.getKey();
                LibraryMetadata metadata = entry.getValue();
                
                logger.debug("Validating library: {} with metadata: {}", libraryName, metadata);
                logger.debug("Library '{}' supported generators: {}", libraryName, metadata.getSupportedGenerators());
                
                // Validate generator compatibility
                validateGeneratorCompatibility(libraryName, metadata, specs, result);
                
                // Validate version requirements  
                validateVersionRequirements(project, libraryName, metadata, result);
                
                // Validate feature compatibility
                validateFeatureCompatibility(libraryName, metadata, result);
                
                logger.debug("Library '{}' validation result: {} issues", libraryName, result.getIssues().size());
            }
            
            logger.debug("Library compatibility validation completed with {} issues", result.getIssues().size());
            
        } catch (Exception e) {
            logger.warn("Error validating library compatibility: {}", e.getMessage(), e);
            result.addIssue("Error validating library compatibility: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validates generator compatibility for a library.
     */
    private void validateGeneratorCompatibility(String libraryName, LibraryMetadata metadata, 
                                              Map<String, SpecConfig> specs, LibraryValidationResult result) {
        if (metadata.getSupportedGenerators() == null || metadata.getSupportedGenerators().isEmpty()) {
            return; // No generator restrictions
        }
        
        // Collect all generators used in specs
        Set<String> usedGenerators = new java.util.HashSet<>();
        // Since all specs currently use the default generator, add it directly
        usedGenerators.add(PluginConstants.DEFAULT_GENERATOR_NAME);
        
        logger.debug("Library '{}' validation: supportedGenerators={}, usedGenerators={}", 
                   libraryName, metadata.getSupportedGenerators(), usedGenerators);
        
        // Check compatibility
        for (String generator : usedGenerators) {
            if (!metadata.getSupportedGenerators().contains(generator)) {
                String issue = String.format("Library '%s' does not support generator(s): [%s]. Supported generators: %s",
                               libraryName, generator, metadata.getSupportedGenerators());
                logger.debug("Adding validation issue: {}", issue);
                result.addIssue(issue);
            }
        }
    }
    
    /**
     * Validates version requirements for a library.
     * 
     * @param project the Gradle project for version detection
     * @param libraryName name of the library being validated
     * @param metadata library metadata containing version requirements
     * @param result validation result to collect issues
     */
    private void validateVersionRequirements(Project project, String libraryName, LibraryMetadata metadata, LibraryValidationResult result) {
        // Validate minimum plugin version
        if (metadata.getMinPluginVersion() != null) {
            String currentVersion = VersionUtils.getCurrentPluginVersion();
            if (currentVersion != null && VersionUtils.isVersionIncompatible(currentVersion, metadata.getMinPluginVersion())) {
                result.addIssue(String.format("Library '%s' requires plugin version %s+ (current: %s)",
                               libraryName, metadata.getMinPluginVersion(), currentVersion));
            }
        }
        
        // Validate OpenAPI Generator version requirements
        String detectedVersion = null;
        try {
            detectedVersion = OpenApiModelGenPlugin.detectOpenApiGeneratorVersion(project);
        } catch (Exception e) {
            logger.debug("Could not detect OpenAPI Generator version for library validation: {}", e.getMessage());
        }
        
        if (metadata.getMinOpenApiGeneratorVersion() != null) {
            logger.info("Library '{}' requires OpenAPI Generator version {}+", 
                       libraryName, metadata.getMinOpenApiGeneratorVersion());
            
            if (detectedVersion != null && !"unknown".equals(detectedVersion)) {
                if (VersionUtils.isVersionBelow(detectedVersion, metadata.getMinOpenApiGeneratorVersion())) {
                    result.addIssue(String.format("Library '%s' requires OpenAPI Generator version %s+ but detected version is %s", 
                                  libraryName, metadata.getMinOpenApiGeneratorVersion(), detectedVersion));
                } else {
                    logger.debug("Library '{}' minimum OpenAPI Generator version requirement satisfied: {} >= {}", 
                               libraryName, detectedVersion, metadata.getMinOpenApiGeneratorVersion());
                }
            } else {
                logger.warn("Library '{}' requires OpenAPI Generator version {}+ but version detection unavailable - validation skipped", 
                           libraryName, metadata.getMinOpenApiGeneratorVersion());
            }
        }
        
        if (metadata.getMaxOpenApiGeneratorVersion() != null) {
            logger.info("Library '{}' supports OpenAPI Generator up to version {}",
                       libraryName, metadata.getMaxOpenApiGeneratorVersion());
            
            if (detectedVersion != null && !"unknown".equals(detectedVersion)) {
                if (VersionUtils.isVersionAbove(detectedVersion, metadata.getMaxOpenApiGeneratorVersion())) {
                    result.addIssue(String.format("Library '%s' supports OpenAPI Generator up to version %s but detected version is %s", 
                                  libraryName, metadata.getMaxOpenApiGeneratorVersion(), detectedVersion));
                } else {
                    logger.debug("Library '{}' maximum OpenAPI Generator version requirement satisfied: {} <= {}", 
                               libraryName, detectedVersion, metadata.getMaxOpenApiGeneratorVersion());
                }
            } else {
                logger.warn("Library '{}' supports OpenAPI Generator up to version {} but version detection unavailable - validation skipped",
                           libraryName, metadata.getMaxOpenApiGeneratorVersion());
            }
        }
    }
    
    /**
     * Validates feature compatibility for a library.
     */
    private void validateFeatureCompatibility(String libraryName, LibraryMetadata metadata, LibraryValidationResult result) {
        // Currently just logs feature information
        if (metadata.getFeatures() != null && !metadata.getFeatures().isEmpty()) {
            logger.debug("Library '{}' provides features: {}", libraryName, metadata.getFeatures().keySet());
        }
        
        // Future enhancement: validate required dependencies are available
        if (metadata.getDependencies() != null && !metadata.getDependencies().isEmpty()) {
            logger.debug("Library '{}' expects dependencies: {}", libraryName, metadata.getDependencies());
        }
    }
    
    /**
     * Container for library content extracted from dependencies.
     */
    public static class LibraryContent implements Serializable {
        
        /**
         * Constructs a new LibraryContent.
         */
        public LibraryContent() {
            // Default constructor
        }
        @Serial
        private static final long serialVersionUID = 1L;
        
        private Map<String, String> templates = new java.util.HashMap<>();
        private Map<String, String> customizations = new java.util.HashMap<>();
        private Map<String, LibraryMetadata> metadata = new java.util.HashMap<>();
        
        /**
         * Gets the templates map.
         * @return the templates
         */
        public Map<String, String> getTemplates() { return templates; }
        
        /**
         * Sets the templates map.
         * @param templates the templates
         */
        public void setTemplates(Map<String, String> templates) { this.templates = templates; }
        
        /**
         * Gets the customizations map.
         * @return the customizations
         */
        public Map<String, String> getCustomizations() { return customizations; }
        
        /**
         * Sets the customizations map.
         * @param customizations the customizations
         */
        public void setCustomizations(Map<String, String> customizations) { this.customizations = customizations; }
        
        /**
         * Gets the metadata map.
         * @return the metadata
         */
        public Map<String, LibraryMetadata> getMetadata() { return metadata; }
        
        /**
         * Sets the metadata map.
         * @param metadata the metadata
         */
        public void setMetadata(Map<String, LibraryMetadata> metadata) { this.metadata = metadata; }
        
        /**
         * Checks if this library content is empty.
         * @return true if all maps are empty
         */
        public boolean isEmpty() {
            return templates.isEmpty() && customizations.isEmpty() && metadata.isEmpty();
        }
    }
    
    /**
     * Result of library compatibility validation.
     */
    public static class LibraryValidationResult implements Serializable {
        
        /**
         * Constructs a new LibraryValidationResult.
         */
        public LibraryValidationResult() {
            // Default constructor
        }
        @Serial
        private static final long serialVersionUID = 1L;
        
        private final java.util.List<String> issues = new java.util.ArrayList<>();
        
        /**
         * Adds an issue to the validation result.
         * @param issue the issue to add
         */
        public void addIssue(String issue) {
            issues.add(issue);
        }
        
        /**
         * Gets the list of validation issues.
         * @return a copy of the issues list
         */
        public java.util.List<String> getIssues() {
            return new java.util.ArrayList<>(issues);
        }
        
        /**
         * Checks if there are any validation issues.
         * @return true if there are issues
         */
        public boolean hasIssues() {
            return !issues.isEmpty();
        }
        
        /**
         * Checks if the validation passed.
         * @return true if there are no issues
         */
        public boolean isValid() {
            return issues.isEmpty();
        }
    }
}