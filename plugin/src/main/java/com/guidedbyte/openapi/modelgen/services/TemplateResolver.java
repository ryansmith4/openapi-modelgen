package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.ResolvedSpecConfig;
import com.guidedbyte.openapi.modelgen.TemplateConfiguration;
import com.guidedbyte.openapi.modelgen.util.PluginLoggerFactory;
import org.gradle.api.file.ProjectLayout;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Resolves template configuration at configuration time for configuration cache compatibility.
 * This service performs all template discovery and validation during the configuration phase,
 * eliminating the need for runtime project access.
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Discover available template sources (user templates, user customizations, plugin customizations)</li>
 *   <li>Resolve relative paths to absolute paths at configuration time</li>
 *   <li>Configure template precedence order from resolved configuration</li>
 *   <li>Enable debug template resolution logging when requested</li>
 *   <li>Create template work directories for processing</li>
 *   <li>Determine if template processing is needed based on available sources</li>
 * </ul>
 * 
 * <p>This service is stateless and configuration-cache compatible, performing all work
 * using only serializable data structures resolved during the configuration phase.</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
public class TemplateResolver {
    private static final Logger logger = PluginLoggerFactory.getLogger(TemplateResolver.class);
    private final TemplateSourceDiscovery templateSourceDiscovery = new TemplateSourceDiscovery();
    
    /**
     * Resolves template configuration at configuration time without library support.
     * This is a convenience method that delegates to the full method with null library content.
     * 
     * @param projectLayout the Gradle project layout for path resolution
     * @param resolvedConfig the fully resolved specification configuration
     * @param generatorName the OpenAPI Generator name (e.g., "spring")
     * @param templateVariables the template variables for Mustache processing
     * @return a fully configured TemplateConfiguration for this specification
     */
    public TemplateConfiguration resolveTemplateConfiguration(
            ProjectLayout projectLayout, 
            ResolvedSpecConfig resolvedConfig, 
            String generatorName,
            Map<String, String> templateVariables) {
        // Delegate to the enhanced method with null library content for backward compatibility
        return resolveTemplateConfiguration(projectLayout, resolvedConfig, generatorName, 
                                          templateVariables, null);
    }
    
    /**
     * Resolves template configuration at configuration time with optional library support.
     * All template discovery, validation, and path resolution happens here.
     * 
     * <p>This method:</p>
     * <ul>
     *   <li>Discovers available template sources (user templates, user customizations, library templates, plugin customizations)</li>
     *   <li>Resolves relative paths to absolute paths using the project layout</li>
     *   <li>Determines if template processing is needed based on available sources</li>
     *   <li>Creates template work directories for processing when needed</li>
     *   <li>Configures template precedence order and debug logging from resolved config</li>
     *   <li>Integrates library templates and customizations when provided</li>
     * </ul>
     * 
     * @param projectLayout the Gradle project layout for path resolution
     * @param resolvedConfig the fully resolved specification configuration
     * @param generatorName the OpenAPI Generator name (e.g., "spring")
     * @param templateVariables the template variables for Mustache processing
     * @param libraryContent extracted library templates and customizations (may be null)
     * @return a fully configured TemplateConfiguration for this specification
     */
    public TemplateConfiguration resolveTemplateConfiguration(
            ProjectLayout projectLayout, 
            ResolvedSpecConfig resolvedConfig, 
            String generatorName,
            Map<String, String> templateVariables,
            LibraryTemplateExtractor.LibraryExtractionResult libraryContent) {
        
        boolean debugEnabled = resolvedConfig.isDebug();
        
        logger.debug(
            "=== TEMPLATE RESOLUTION START ===");
        logger.debug(
            "Generator: {}, Spec: {}", generatorName, resolvedConfig.getSpecName());
        logger.debug(
            "Template dir: {}, Customizations dir: {}",
            resolvedConfig.getUserTemplateDir(), resolvedConfig.getUserTemplateCustomizationsDir());
        
        // Get configured template sources (with fallback to defaults)
        List<String> configuredTemplateSources = resolvedConfig.getTemplateSources();
        logger.debug(
            "Starting template resolution for generator '{}' with sources: {}",
            generatorName, configuredTemplateSources);
        
        if (configuredTemplateSources == null || configuredTemplateSources.isEmpty()) {
            configuredTemplateSources = TemplateSourceDiscovery.ALL_TEMPLATE_SOURCES;
            logger.debug(
                "No template sources configured for '{}', using default sources: {}",
                generatorName, configuredTemplateSources);
        } else {
            logger.debug(
                "Using configured template sources for '{}': {}",
                generatorName, configuredTemplateSources);
        }
        
        // Auto-discover which sources are actually available
        boolean hasLibraryDependencies = libraryContent != null;
        logger.debug(
            "Checking for library dependencies: {}", hasLibraryDependencies);
        
        List<String> availableTemplateSources = templateSourceDiscovery.discoverAvailableSources(
            configuredTemplateSources, resolvedConfig, projectLayout, hasLibraryDependencies
        );
        
        logger.debug(
            "Available template sources after discovery: {}", availableTemplateSources);
        
        // Convert to boolean flags for backward compatibility with TemplateConfiguration
        boolean hasUserTemplates = availableTemplateSources.contains("user-templates");
        boolean hasUserCustomizations = availableTemplateSources.contains("user-customizations");
        boolean hasLibraryTemplates = availableTemplateSources.contains("library-templates") && 
            libraryContent != null && libraryContent.hasTemplates();
        boolean hasLibraryCustomizations = availableTemplateSources.contains("library-customizations") && 
            libraryContent != null && libraryContent.hasCustomizations();
        boolean hasPluginCustomizations = availableTemplateSources.contains("plugin-customizations") && 
            hasPluginCustomizations(generatorName);
        
        logger.debug(
            "Template source flags - userTemplates: {}, userCustomizations: {}, libraryTemplates: {}, " +
            "libraryCustomizations: {}, pluginCustomizations: {}",
            hasUserTemplates, hasUserCustomizations, hasLibraryTemplates,
            hasLibraryCustomizations, hasPluginCustomizations);
        
        // Resolve paths for user templates and customizations
        String resolvedUserTemplateDir = null;
        String resolvedUserCustomizationsDir = null;
        
        if (hasUserTemplates && resolvedConfig.getUserTemplateDir() != null) {
            resolvedUserTemplateDir = projectLayout.getProjectDirectory()
                .dir(resolvedConfig.getUserTemplateDir()).getAsFile().getAbsolutePath();
            logger.debug(
                "Resolved user template directory: {}", resolvedUserTemplateDir);
        }
        
        if (hasUserCustomizations && resolvedConfig.getUserTemplateCustomizationsDir() != null) {
            resolvedUserCustomizationsDir = projectLayout.getProjectDirectory()
                .dir(resolvedConfig.getUserTemplateCustomizationsDir()).getAsFile().getAbsolutePath();
            logger.debug(
                "Resolved user customizations directory: {}", resolvedUserCustomizationsDir);
        }
        
        logger.debug(
            "Template resolution summary for '{}': configured={}, available={}",
            generatorName, configuredTemplateSources, availableTemplateSources);
        
        // Enhanced debug logging for template resolution
        if (resolvedConfig.isDebug()) {
            logger.info("=== Template Resolution Debug for '{}' ===", generatorName);
            logger.info("Configured template sources: {}", configuredTemplateSources);
            logger.info("Available template sources: {}", availableTemplateSources);
            
            if (hasUserTemplates) {
                logger.info("✅ user-templates: {}", resolvedUserTemplateDir);
            }
            if (hasUserCustomizations) {
                logger.info("✅ user-customizations: {}", resolvedUserCustomizationsDir);
            }
            if (hasLibraryTemplates) {
                logger.info("✅ library-templates: {} templates from libraries", libraryContent.getTemplates().size());
            }
            if (hasLibraryCustomizations) {
                logger.info("✅ library-customizations: {} customizations from libraries", libraryContent.getCustomizations().size());
            }
            if (hasPluginCustomizations) {
                logger.info("✅ plugin-customizations: built-in YAML customizations");
            }
            if (availableTemplateSources.contains("openapi-generator") || availableTemplateSources.isEmpty()) {
                logger.info("✅ openapi-generator: OpenAPI Generator default templates (fallback)");
            }
            logger.info("=== End Template Resolution Debug ===");
        }
        
        // Determine template work directory path at configuration time
        String templateWorkDirectory = null;
        boolean templateProcessingEnabled = hasUserTemplates || hasUserCustomizations || hasLibraryTemplates || hasLibraryCustomizations || hasPluginCustomizations;
        
        logger.debug(
            "Template processing enabled: {} (checking: userTemplates={}, userCustomizations={}, " +
            "libraryTemplates={}, libraryCustomizations={}, pluginCustomizations={})",
            templateProcessingEnabled, hasUserTemplates, hasUserCustomizations,
            hasLibraryTemplates, hasLibraryCustomizations, hasPluginCustomizations);
        
        if (templateProcessingEnabled) {
            String specName = resolvedConfig.getSpecName();
            templateWorkDirectory = projectLayout.getBuildDirectory()
                .dir("template-work/" + generatorName + "-" + specName).get().getAsFile().getAbsolutePath();
            logger.debug(
                "Template processing enabled for generator '{}', spec '{}', work directory: {}",
                generatorName, specName, templateWorkDirectory);
            
            // Create the work directory but not the marker file during configuration phase
            // to maintain proper input tracking while avoiding configuration cache issues
            File workDir = new File(templateWorkDirectory);
            try {
                boolean created = workDir.mkdirs();
                logger.debug(
                    "Ensured template work directory exists: {} (created: {})",
                    templateWorkDirectory, created);
                
                // DO NOT create marker file during configuration phase - this breaks configuration cache
                // Marker files will be created during task execution phase by PrepareTemplateDirectoryTask
            } catch (Exception e) {
                logger.warn("Failed to create template work directory '{}': {}", templateWorkDirectory, e.getMessage());
            }
        } else {
            logger.debug(
                "No template customizations found for generator '{}'. Will use OpenAPI Generator defaults.",
                generatorName);
        }
        
        // Filter library templates for this generator
        Map<String, String> generatorLibraryTemplates = Map.of();
        Map<String, String> generatorLibraryCustomizations = Map.of();
        
        if (libraryContent != null) {
            logger.debug(
                "Processing library content: {} templates, {} customizations",
                libraryContent.getTemplates().size(), libraryContent.getCustomizations().size());
            
            // Filter library templates to only include those for this generator
            String generatorPrefix = generatorName + "/";
            generatorLibraryTemplates = libraryContent.getTemplates().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(generatorPrefix))
                .collect(java.util.stream.Collectors.toMap(
                    entry -> entry.getKey().substring(generatorPrefix.length()), // Remove generator prefix
                    Map.Entry::getValue));
            
            logger.debug(
                "Filtered {} library templates for generator '{}'",
                generatorLibraryTemplates.size(), generatorName);
            
            // Same for customizations
            generatorLibraryCustomizations = libraryContent.getCustomizations().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(generatorPrefix))
                .collect(java.util.stream.Collectors.toMap(
                    entry -> entry.getKey().substring(generatorPrefix.length()),
                    Map.Entry::getValue));
            
            logger.debug(
                "Filtered {} library customizations for generator '{}'",
                generatorLibraryCustomizations.size(), generatorName);
        }
        
        logger.debug(
            "Building TemplateConfiguration for generator '{}' with workDir: {}, processingEnabled: {}",
            generatorName, templateWorkDirectory, templateProcessingEnabled);
        
        TemplateConfiguration templateConfig = TemplateConfiguration.builder(generatorName)
            .templateWorkDir(templateWorkDirectory)
            .hasUserTemplates(hasUserTemplates)
            .hasUserCustomizations(hasUserCustomizations)
            .hasLibraryTemplates(hasLibraryTemplates)
            .hasLibraryCustomizations(hasLibraryCustomizations)
            .hasPluginCustomizations(hasPluginCustomizations)
            .userTemplateDirectory(resolvedUserTemplateDir)
            .userCustomizationsDirectory(resolvedUserCustomizationsDir)
            .libraryTemplates(generatorLibraryTemplates)
            .libraryCustomizations(generatorLibraryCustomizations)
            .templateVariables(templateVariables != null ? Map.copyOf(templateVariables) : Map.of())
            .templateProcessingEnabled(templateProcessingEnabled)
            .templateSources(resolvedConfig.getTemplateSources())
            .debug(resolvedConfig.isDebug())
            .saveOriginalTemplates(resolvedConfig.isSaveOriginalTemplates())
            .build();
        
        logger.debug(
            "TemplateConfiguration created successfully for generator '{}'", generatorName);
        
        if (debugEnabled) {
            logger.info("=== TEMPLATE RESOLUTION COMPLETE ===");
            logger.info("Final configuration for generator '{}', spec '{}'", generatorName, resolvedConfig.getSpecName());
            logger.info("Template processing enabled: {}", templateProcessingEnabled);
            logger.info("Template work directory: {}", templateWorkDirectory);
            if (hasUserTemplates) {
                logger.info("✅ User templates: {}", resolvedUserTemplateDir);
            }
            if (hasUserCustomizations) {
                logger.info("✅ User customizations: {}", resolvedUserCustomizationsDir);
            }
            if (hasLibraryTemplates) {
                logger.info("✅ Library templates: {} templates", generatorLibraryTemplates.size());
            }
            if (hasLibraryCustomizations) {
                logger.info("✅ Library customizations: {} files", generatorLibraryCustomizations.size());
            }
            if (hasPluginCustomizations) {
                logger.info("✅ Plugin customizations: enabled");
            }
            logger.info("Template variables: {}", templateVariables != null ? templateVariables.keySet() : "none");
            logger.info("=== END TEMPLATE RESOLUTION ===");
        }
        
        return templateConfig;
    }
    
    
    
    /**
     * Checks if plugin customizations exist for the specified generator at configuration time.
     */
    private boolean hasPluginCustomizations(String generatorName) {
        try {
            // Check if plugin has any customizations for this generator
            // Use class loader to check for resources
            var classLoader = TemplateResolver.class.getClassLoader();
            var resource = classLoader.getResource("templateCustomizations/" + generatorName);
            
            boolean hasCustomizations = resource != null;
            
            if (hasCustomizations) {
                logger.debug("Found plugin customizations for generator: {}", generatorName);
            }
            
            return hasCustomizations;
            
        } catch (Exception e) {
            logger.warn("Error checking plugin customizations for generator '{}': {}", generatorName, e.getMessage());
            return false;
        }
    }
}