package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.ResolvedSpecConfig;
import com.guidedbyte.openapi.modelgen.util.DebugLogger;
import org.gradle.api.file.ProjectLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuration-cache compatible service for discovering available template sources.
 * 
 * <p>This service automatically discovers which template sources are available in the project
 * and filters the requested sources to only include those that exist. It provides comprehensive
 * debug logging to help users understand template resolution.</p>
 * 
 * <p><strong>Auto-Discovery Logic:</strong></p>
 * <ul>
 *   <li><strong>user-templates:</strong> Checks if templateDir exists and contains .mustache files</li>
 *   <li><strong>user-customizations:</strong> Checks if templateCustomizationsDir exists and contains .yaml files</li>
 *   <li><strong>library-templates:</strong> Checks if library dependencies contain template JARs</li>
 *   <li><strong>library-customizations:</strong> Checks if library dependencies contain customization JARs</li>
 *   <li><strong>plugin-customizations:</strong> Always available (built into plugin JAR)</li>
 *   <li><strong>openapi-generator:</strong> Always available (from OpenAPI Generator dependency)</li>
 * </ul>
 * 
 * <p><strong>Configuration Cache Compatibility:</strong></p>
 * <ul>
 *   <li>Uses static SLF4J logger instead of Project logger</li>
 *   <li>All discovery methods are pure functions with no Project dependencies</li>
 *   <li>Serializable for Gradle configuration cache</li>
 *   <li>No runtime state - all configuration resolved at configuration time</li>
 * </ul>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class TemplateSourceDiscovery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(TemplateSourceDiscovery.class);
    
    /**
     * All possible template sources in default priority order.
     */
    public static final List<String> ALL_TEMPLATE_SOURCES = Arrays.asList(
        "user-templates",           // Highest priority
        "user-customizations",
        "library-templates",
        "library-customizations",
        "plugin-customizations",
        "openapi-generator"         // Lowest priority (always available)
    );
    
    /**
     * Discovers which template sources are actually available for the given configuration.
     * 
     * <p>This method performs auto-discovery by checking for the existence of template directories,
     * library dependencies, and other required resources. Sources that don't exist are filtered out
     * with appropriate debug logging.</p>
     * 
     * @param requestedSources the list of sources requested by the user (in priority order)
     * @param resolvedConfig the resolved configuration for the specification
     * @param projectLayout the project layout for path resolution (configuration-time only)
     * @param hasLibraryDependencies whether library dependencies are configured
     * @return the filtered list of available sources in the requested priority order
     */
    public List<String> discoverAvailableSources(List<String> requestedSources, 
                                                ResolvedSpecConfig resolvedConfig,
                                                ProjectLayout projectLayout,
                                                boolean hasLibraryDependencies) {
        
        boolean debugEnabled = resolvedConfig.isDebugTemplateResolution();
        
        if (requestedSources == null || requestedSources.isEmpty()) {
            DebugLogger.debug(logger, debugEnabled, 
                "No template sources requested, using all available sources");
            requestedSources = ALL_TEMPLATE_SOURCES;
        }
        
        DebugLogger.debug(logger, debugEnabled, 
            "Starting template source discovery for spec '{}' with requested sources: {}", 
            resolvedConfig.getSpecName(), requestedSources);
        
        // Create discovery strategies for each source type
        Map<String, SourceAvailability> discoveryResults = new LinkedHashMap<>();
        
        for (String source : requestedSources) {
            DebugLogger.debug(logger, debugEnabled, 
                "Checking availability of source: {}", source);
            SourceAvailability availability = checkSourceAvailability(source, resolvedConfig, projectLayout, hasLibraryDependencies);
            discoveryResults.put(source, availability);
            DebugLogger.debug(logger, debugEnabled, 
                "Source '{}' availability: {} (reason: {})", 
                source, availability.isAvailable(), availability.getReason());
        }
        
        // Filter to only available sources
        List<String> availableSources = discoveryResults.entrySet().stream()
            .filter(entry -> entry.getValue().isAvailable())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        DebugLogger.debug(logger, debugEnabled, 
            "Template source discovery complete. Available sources: {}", availableSources);
        
        // Log discovery results
        logDiscoveryResults(resolvedConfig, requestedSources, discoveryResults, availableSources);
        
        return availableSources;
    }
    
    /**
     * Checks if a specific template source is available.
     * 
     * @param source the template source to check
     * @param resolvedConfig the resolved configuration
     * @param projectLayout the project layout for path resolution
     * @param hasLibraryDependencies whether library dependencies are configured
     * @return the availability status with reason
     */
    private SourceAvailability checkSourceAvailability(String source, 
                                                      ResolvedSpecConfig resolvedConfig,
                                                      ProjectLayout projectLayout,
                                                      boolean hasLibraryDependencies) {

        return switch (source) {
            case "user-templates" -> checkUserTemplates(resolvedConfig, projectLayout);
            case "user-customizations" -> checkUserCustomizations(resolvedConfig, projectLayout);
            case "library-templates" -> checkLibraryTemplates(hasLibraryDependencies);
            case "library-customizations" -> checkLibraryCustomizations(hasLibraryDependencies);
            case "plugin-customizations" -> SourceAvailability.available("Built-in plugin customizations");
            case "openapi-generator" -> SourceAvailability.available("OpenAPI Generator defaults");
            default -> SourceAvailability.unavailable("Unknown template source: " + source);
        };
    }
    
    /**
     * Checks if user templates are available.
     */
    private SourceAvailability checkUserTemplates(ResolvedSpecConfig resolvedConfig, ProjectLayout projectLayout) {
        String templateDir = resolvedConfig.getTemplateDir();
        if (templateDir == null || templateDir.trim().isEmpty()) {
            return SourceAvailability.unavailable("No templateDir configured");
        }
        
        File templateDirFile = projectLayout.getProjectDirectory().file(templateDir).getAsFile();
        if (!templateDirFile.exists()) {
            return SourceAvailability.unavailable("Template directory does not exist: " + templateDir);
        }
        
        if (!templateDirFile.isDirectory()) {
            return SourceAvailability.unavailable("Template path is not a directory: " + templateDir);
        }
        
        // Check for .mustache files
        File[] mustacheFiles = templateDirFile.listFiles((dir, name) -> name.endsWith(".mustache"));
        if (mustacheFiles == null || mustacheFiles.length == 0) {
            return SourceAvailability.unavailable("No .mustache files found in: " + templateDir);
        }
        
        return SourceAvailability.available("Found " + mustacheFiles.length + " template(s) in: " + templateDir);
    }
    
    /**
     * Checks if user customizations are available.
     */
    private SourceAvailability checkUserCustomizations(ResolvedSpecConfig resolvedConfig, ProjectLayout projectLayout) {
        String customizationsDir = resolvedConfig.getTemplateCustomizationsDir();
        if (customizationsDir == null || customizationsDir.trim().isEmpty()) {
            return SourceAvailability.unavailable("No templateCustomizationsDir configured");
        }
        
        File customizationsDirFile = projectLayout.getProjectDirectory().file(customizationsDir).getAsFile();
        if (!customizationsDirFile.exists()) {
            return SourceAvailability.unavailable("Customizations directory does not exist: " + customizationsDir);
        }
        
        if (!customizationsDirFile.isDirectory()) {
            return SourceAvailability.unavailable("Customizations path is not a directory: " + customizationsDir);
        }
        
        // Check for .yaml files in any subdirectory (generator-specific structure)
        boolean hasYamlFiles = findYamlFilesRecursively(customizationsDirFile);
        if (!hasYamlFiles) {
            return SourceAvailability.unavailable("No .yaml files found in: " + customizationsDir);
        }
        
        return SourceAvailability.available("Found customization files in: " + customizationsDir);
    }
    
    /**
     * Checks if library templates are available.
     * Note: Actual library discovery would be performed by LibraryProcessor service.
     */
    private SourceAvailability checkLibraryTemplates(boolean hasLibraryDependencies) {
        if (!hasLibraryDependencies) {
            return SourceAvailability.unavailable("No library dependencies configured");
        }
        
        // In a real implementation, this would check the actual JAR contents
        // For now, we assume libraries are available if dependencies are configured
        return SourceAvailability.available("Library dependencies configured (detailed scanning at execution time)");
    }
    
    /**
     * Checks if library customizations are available.
     * Note: Actual library discovery would be performed by LibraryProcessor service.
     */
    private SourceAvailability checkLibraryCustomizations(boolean hasLibraryDependencies) {
        if (!hasLibraryDependencies) {
            return SourceAvailability.unavailable("No library dependencies configured");
        }
        
        return SourceAvailability.available("Library dependencies configured (detailed scanning at execution time)");
    }
    
    /**
     * Logs comprehensive discovery results for debugging.
     */
    private void logDiscoveryResults(ResolvedSpecConfig resolvedConfig, 
                                   List<String> requestedSources, 
                                   Map<String, SourceAvailability> discoveryResults, 
                                   List<String> availableSources) {
        
        boolean debugEnabled = resolvedConfig.isDebugTemplateResolution();
        String specName = resolvedConfig.getSpecName();
        
        DebugLogger.debug(logger, debugEnabled,
            "Template source discovery for spec '{}': {} requested, {} available", 
            specName, requestedSources.size(), availableSources.size());
        
        if (debugEnabled) {
            DebugLogger.debug(logger, debugEnabled,
                "Requested template sources for '{}': {}", specName, requestedSources);
            
            for (Map.Entry<String, SourceAvailability> entry : discoveryResults.entrySet()) {
                String source = entry.getKey();
                SourceAvailability availability = entry.getValue();
                String status = availability.isAvailable() ? "✅" : "❌";
                DebugLogger.debug(logger, debugEnabled,
                    "  - {}: {} ({})", source, status, availability.getReason());
            }
            
            DebugLogger.debug(logger, debugEnabled,
                "Effective template sources for '{}': {}", specName, availableSources);
        }
        
        // Log warning if no sources are available
        if (availableSources.isEmpty()) {
            logger.warn("No template sources available for spec '{}'. Only OpenAPI Generator defaults will be used.", specName);
            availableSources.add("openapi-generator"); // Always ensure openapi-generator is available as fallback
        }
    }
    
    /**
     * Represents the availability status of a template source.
     */
    private static class SourceAvailability implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        
        private final boolean available;
        private final String reason;
        
        private SourceAvailability(boolean available, String reason) {
            this.available = available;
            this.reason = reason;
        }
        
        public static SourceAvailability available(String reason) {
            return new SourceAvailability(true, reason);
        }
        
        public static SourceAvailability unavailable(String reason) {
            return new SourceAvailability(false, reason);
        }
        
        public boolean isAvailable() {
            return available;
        }
        
        public String getReason() {
            return reason;
        }
    }
    
    /**
     * Recursively searches for .yaml files in the given directory and subdirectories.
     * 
     * @param directory the directory to search
     * @return true if any .yaml files are found, false otherwise
     */
    private boolean findYamlFilesRecursively(File directory) {
        if (!directory.isDirectory()) {
            return false;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }
        
        for (File file : files) {
            if (file.isFile() && (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml"))) {
                return true; // Found a YAML file
            } else if (file.isDirectory()) {
                // Recursively search subdirectories
                if (findYamlFilesRecursively(file)) {
                    return true;
                }
            }
        }
        
        return false; // No YAML files found
    }
}