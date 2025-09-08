package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.ResolvedSpecConfig;
import com.guidedbyte.openapi.modelgen.TemplateConfiguration;
import org.gradle.api.file.ProjectLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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
    private static final Logger logger = LoggerFactory.getLogger(TemplateResolver.class);
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
        
        // Get configured template sources (with fallback to defaults)
        List<String> configuredTemplateSources = resolvedConfig.getTemplateSources();
        if (configuredTemplateSources == null || configuredTemplateSources.isEmpty()) {
            configuredTemplateSources = TemplateSourceDiscovery.ALL_TEMPLATE_SOURCES;
            logger.debug("No template sources configured for '{}', using all available sources", generatorName);
        }
        
        // Auto-discover which sources are actually available
        boolean hasLibraryDependencies = libraryContent != null;
        List<String> availableTemplateSources = templateSourceDiscovery.discoverAvailableSources(
            configuredTemplateSources, resolvedConfig, projectLayout, hasLibraryDependencies
        );
        
        // Convert to boolean flags for backward compatibility with TemplateConfiguration
        boolean hasUserTemplates = availableTemplateSources.contains("user-templates");
        boolean hasUserCustomizations = availableTemplateSources.contains("user-customizations");
        boolean hasLibraryTemplates = availableTemplateSources.contains("library-templates") && 
            libraryContent != null && libraryContent.hasTemplates();
        boolean hasLibraryCustomizations = availableTemplateSources.contains("library-customizations") && 
            libraryContent != null && libraryContent.hasCustomizations();
        boolean hasPluginCustomizations = availableTemplateSources.contains("plugin-customizations") && 
            hasPluginCustomizations(generatorName);
        
        // For backward compatibility, also check the old applyPluginCustomizations flag
        boolean applyPluginCustomizations = resolvedConfig.isApplyPluginCustomizations();
        if (!applyPluginCustomizations && hasPluginCustomizations) {
            logger.debug("Plugin customizations available but disabled by applyPluginCustomizations=false for '{}'", generatorName);
            hasPluginCustomizations = false;
        }
        
        // Resolve paths for user templates and customizations
        String resolvedUserTemplateDir = null;
        String resolvedUserCustomizationsDir = null;
        
        if (hasUserTemplates && resolvedConfig.getTemplateDir() != null) {
            resolvedUserTemplateDir = projectLayout.getProjectDirectory()
                .dir(resolvedConfig.getTemplateDir()).getAsFile().getAbsolutePath();
        }
        
        if (hasUserCustomizations && resolvedConfig.getTemplateCustomizationsDir() != null) {
            resolvedUserCustomizationsDir = projectLayout.getProjectDirectory()
                .dir(resolvedConfig.getTemplateCustomizationsDir()).getAsFile().getAbsolutePath();
        }
        
        logger.debug("Template resolution for '{}': configured sources={}, available sources={}", 
            generatorName, configuredTemplateSources, availableTemplateSources);
        
        // Enhanced debug logging for template resolution
        if (resolvedConfig.isDebugTemplateResolution()) {
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
        
        if (templateProcessingEnabled) {
            templateWorkDirectory = projectLayout.getBuildDirectory()
                .dir("template-work/" + generatorName).get().getAsFile().getAbsolutePath();
            logger.debug("Template processing enabled for generator '{}', work directory: {}", generatorName, templateWorkDirectory);
            
            // Create the directory immediately during configuration time to avoid Gradle validation issues
            // We create this aggressively since Gradle will validate it exists during task graph building
            File workDir = new File(templateWorkDirectory);
            
            // Force directory creation even if it exists to ensure it's not deleted by clean tasks
            try {
                boolean created = workDir.mkdirs();
                logger.debug("Ensured template work directory exists: {} (created: {})", templateWorkDirectory, created);
                
                // Also create a marker file to help with Gradle input validation
                File markerFile = new File(workDir, ".gradle-template-dir");
                if (!markerFile.exists()) {
                    markerFile.createNewFile();
                    logger.debug("Created template directory marker file: {}", markerFile.getAbsolutePath());
                }
            } catch (Exception e) {
                logger.warn("Failed to create template work directory '{}': {}", templateWorkDirectory, e.getMessage());
            }
        } else {
            logger.debug("No template customizations found for generator '{}'. Will use OpenAPI Generator defaults.", generatorName);
        }
        
        // Filter library templates for this generator
        Map<String, String> generatorLibraryTemplates = Map.of();
        Map<String, String> generatorLibraryCustomizations = Map.of();
        
        if (libraryContent != null) {
            // Filter library templates to only include those for this generator
            String generatorPrefix = generatorName + "/";
            generatorLibraryTemplates = libraryContent.getTemplates().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(generatorPrefix))
                .collect(java.util.stream.Collectors.toMap(
                    entry -> entry.getKey().substring(generatorPrefix.length()), // Remove generator prefix
                    Map.Entry::getValue));
            
            // Same for customizations
            generatorLibraryCustomizations = libraryContent.getCustomizations().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(generatorPrefix))
                .collect(java.util.stream.Collectors.toMap(
                    entry -> entry.getKey().substring(generatorPrefix.length()),
                    Map.Entry::getValue));
        }
        
        return TemplateConfiguration.builder(generatorName)
            .templateWorkDirectory(templateWorkDirectory)
            .hasUserTemplates(hasUserTemplates)
            .hasUserCustomizations(hasUserCustomizations)
            .hasLibraryTemplates(hasLibraryTemplates)
            .hasLibraryCustomizations(hasLibraryCustomizations)
            .hasPluginCustomizations(hasPluginCustomizations)
            .applyPluginCustomizations(applyPluginCustomizations)
            .userTemplateDirectory(resolvedUserTemplateDir)
            .userCustomizationsDirectory(resolvedUserCustomizationsDir)
            .libraryTemplates(generatorLibraryTemplates)
            .libraryCustomizations(generatorLibraryCustomizations)
            .templateVariables(templateVariables != null ? Map.copyOf(templateVariables) : Map.of())
            .templateProcessingEnabled(templateProcessingEnabled)
            .templatePrecedence(resolvedConfig.getTemplatePrecedence())
            .debugTemplateResolution(resolvedConfig.isDebugTemplateResolution())
            .build();
    }
    
    /**
     * Checks if user templates exist for the specified generator at configuration time.
     */
    private boolean hasUserTemplatesForGenerator(String userTemplateDir, String generatorName) {
        if (userTemplateDir == null) {
            return false;
        }
        
        File templateDir = new File(userTemplateDir, generatorName);
        if (!templateDir.exists() || !templateDir.isDirectory()) {
            return false;
        }
        
        // Check for any .mustache files
        File[] mustacheFiles = templateDir.listFiles((dir, name) -> name.endsWith(".mustache"));
        boolean hasTemplates = mustacheFiles != null && mustacheFiles.length > 0;
        
        if (hasTemplates) {
            logger.debug("Found {} user template(s) in: {}", mustacheFiles.length, templateDir.getAbsolutePath());
        }
        
        return hasTemplates;
    }
    
    /**
     * Checks if user customizations exist for the specified generator at configuration time.
     */
    private boolean hasUserCustomizationsForGenerator(String userCustomizationsDir, String generatorName) {
        if (userCustomizationsDir == null) {
            return false;
        }
        
        File customizationsDir = new File(userCustomizationsDir, generatorName);
        if (!customizationsDir.exists() || !customizationsDir.isDirectory()) {
            return false;
        }
        
        // Check for any .yaml or .yml files
        File[] yamlFiles = customizationsDir.listFiles((dir, name) -> 
            name.endsWith(".yaml") || name.endsWith(".yml"));
        boolean hasCustomizations = yamlFiles != null && yamlFiles.length > 0;
        
        if (hasCustomizations) {
            logger.debug("Found {} user customization(s) in: {}", yamlFiles.length, customizationsDir.getAbsolutePath());
        }
        
        return hasCustomizations;
    }
    
    /**
     * Checks if plugin customizations exist for the specified generator at configuration time.
     */
    private boolean hasPluginCustomizations(String generatorName) {
        try {
            // Check if plugin has any customizations for this generator
            String resourcePath = "/templateCustomizations/" + generatorName + "/";
            
            // Use class loader to check for resources
            var classLoader = TemplateResolver.class.getClassLoader();
            var resource = classLoader.getResource("templateCustomizations/" + generatorName);
            
            boolean hasCustomizations = resource != null;
            
            if (hasCustomizations) {
                logger.debug("Found plugin customizations for generator: {}", generatorName);
            }
            
            return hasCustomizations;
            
        } catch (Exception e) {
            logger.debug("Error checking plugin customizations for generator '{}': {}", generatorName, e.getMessage());
            return false;
        }
    }
}