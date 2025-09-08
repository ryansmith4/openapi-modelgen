package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.DefaultConfig;
import com.guidedbyte.openapi.modelgen.OpenApiModelGenExtension;
import com.guidedbyte.openapi.modelgen.OpenApiModelGenPlugin;
import com.guidedbyte.openapi.modelgen.SpecConfig;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Configuration-cache compatible service for processing OpenAPI template libraries.
 * Handles library dependency processing, metadata validation, and compatibility checking.
 */
public class LibraryProcessor implements Serializable {
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
        
        // Check if any library features are enabled at default level
        DefaultConfig defaults = extension.getDefaults();
        if (defaults != null) {
            boolean useLibraryTemplates = defaults.getUseLibraryTemplates().isPresent() && defaults.getUseLibraryTemplates().get();
            boolean useLibraryCustomizations = defaults.getUseLibraryCustomizations().isPresent() && defaults.getUseLibraryCustomizations().get();
            
            if (useLibraryTemplates || useLibraryCustomizations) {
                logger.debug("Library features enabled at defaults level");
                return true;
            }
        }
        
        // Note: SpecConfig doesn't have library settings, they're only in DefaultConfig
        // So we only check defaults level for library configuration
        
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
                StringBuilder errorMessage = new StringBuilder("Library compatibility validation failed:");
                for (String issue : validationResult.getIssues()) {
                    errorMessage.append("\n  - ").append(issue);
                }
                throw new org.gradle.api.InvalidUserDataException(errorMessage.toString());
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
            throw new org.gradle.api.InvalidUserDataException("Failed to process template libraries: " + e.getMessage(), e);
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
                throw new RuntimeException("Library content extraction failed", e);
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
        for (SpecConfig spec : specs.values()) {
            String generatorName = "spring"; // Default generator - specs don't override this currently
            usedGenerators.add(generatorName);
        }
        
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
            String currentVersion = getCurrentPluginVersion();
            if (currentVersion != null && !isVersionCompatible(currentVersion, metadata.getMinPluginVersion())) {
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
                if (isVersionBelow(detectedVersion, metadata.getMinOpenApiGeneratorVersion())) {
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
                if (isVersionAbove(detectedVersion, metadata.getMaxOpenApiGeneratorVersion())) {
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
     * Gets current plugin version for validation.
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
    
    /**
     * Checks if a version is below the minimum version using simple semantic versioning.
     */
    private boolean isVersionBelow(String version, String minVersion) {
        try {
            return compareVersions(version, minVersion) < 0;
        } catch (Exception e) {
            logger.debug("Error comparing versions '{}' and '{}': {}", version, minVersion, e.getMessage());
            return false; // If we can't parse, assume it's compatible
        }
    }
    
    /**
     * Checks if a version is above the maximum version using simple semantic versioning.
     */
    private boolean isVersionAbove(String version, String maxVersion) {
        try {
            return compareVersions(version, maxVersion) > 0;
        } catch (Exception e) {
            logger.debug("Error comparing versions '{}' and '{}': {}", version, maxVersion, e.getMessage());
            return false; // If we can't parse, assume it's compatible
        }
    }
    
    /**
     * Compares two version strings using simple semantic versioning.
     * Returns: negative if version1 < version2, zero if equal, positive if version1 > version2
     */
    private int compareVersions(String version1, String version2) {
        if (version1.equals(version2)) {
            return 0;
        }
        
        String[] parts1 = version1.split("[.-]");
        String[] parts2 = version2.split("[.-]");
        
        int maxLength = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLength; i++) {
            String part1 = i < parts1.length ? parts1[i] : "0";
            String part2 = i < parts2.length ? parts2[i] : "0";
            
            // Try to parse as integer first
            try {
                int num1 = Integer.parseInt(part1);
                int num2 = Integer.parseInt(part2);
                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            } catch (NumberFormatException e) {
                // If not numeric, compare as strings
                int stringComparison = part1.compareTo(part2);
                if (stringComparison != 0) {
                    return stringComparison;
                }
            }
        }
        
        return 0; // All parts are equal
    }
    
    /**
     * Container for library content extracted from dependencies.
     */
    public static class LibraryContent implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Map<String, String> templates = new java.util.HashMap<>();
        private Map<String, String> customizations = new java.util.HashMap<>();
        private Map<String, LibraryMetadata> metadata = new java.util.HashMap<>();
        
        public Map<String, String> getTemplates() { return templates; }
        public void setTemplates(Map<String, String> templates) { this.templates = templates; }
        
        public Map<String, String> getCustomizations() { return customizations; }
        public void setCustomizations(Map<String, String> customizations) { this.customizations = customizations; }
        
        public Map<String, LibraryMetadata> getMetadata() { return metadata; }
        public void setMetadata(Map<String, LibraryMetadata> metadata) { this.metadata = metadata; }
        
        public boolean isEmpty() {
            return templates.isEmpty() && customizations.isEmpty() && metadata.isEmpty();
        }
    }
    
    /**
     * Result of library compatibility validation.
     */
    public static class LibraryValidationResult implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final java.util.List<String> issues = new java.util.ArrayList<>();
        
        public void addIssue(String issue) {
            issues.add(issue);
        }
        
        public java.util.List<String> getIssues() {
            return new java.util.ArrayList<>(issues);
        }
        
        public boolean hasIssues() {
            return !issues.isEmpty();
        }
        
        public boolean isValid() {
            return issues.isEmpty();
        }
    }
}