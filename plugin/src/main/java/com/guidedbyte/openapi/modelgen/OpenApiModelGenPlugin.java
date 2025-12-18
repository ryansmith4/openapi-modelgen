package com.guidedbyte.openapi.modelgen;

import com.guidedbyte.openapi.modelgen.constants.PluginConstants;
import com.guidedbyte.openapi.modelgen.services.*;
import com.guidedbyte.openapi.modelgen.util.PluginLoggerFactory;
import com.guidedbyte.openapi.modelgen.util.PluginState;
import com.guidedbyte.openapi.modelgen.utils.VersionUtils;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.jetbrains.annotations.NotNull;
import org.openapitools.generator.gradle.plugin.OpenApiGeneratorPlugin;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * OpenAPI Model Generator Plugin - A comprehensive Gradle plugin that wraps the OpenAPI Generator 
 * with enhanced features for generating Java DTOs with Lombok support, custom templates, and 
 * enterprise-grade performance optimizations.
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><strong>Unified Template Sources:</strong> Simplified template configuration with auto-discovery and configurable precedence order</li>
 *   <li><strong>Debug Template Resolution:</strong> Optional debug logging to show which template source was used for each template</li>
 *   <li><strong>YAML Template Customization:</strong> Modify existing templates using structured YAML configuration files</li>
 *   <li><strong>Multi-Level Caching System:</strong> Session → local → global cache hierarchy with 90% faster no-change builds</li>
 *   <li><strong>Parallel Multi-Spec Processing:</strong> Thread-safe concurrent generation with configurable parallel execution</li>
 *   <li><strong>Configuration Cache Compatibility:</strong> Fully compatible with Gradle's configuration cache for optimal performance</li>
 *   <li><strong>Content-Based Change Detection:</strong> SHA-256 hashing for reliable template and customization change detection</li>
 *   <li><strong>Spec-Level Configuration:</strong> Individual validation, template settings, and precedence per specification</li>
 *   <li><strong>Multi-Spec Support:</strong> Generate DTOs from multiple OpenAPI specifications with individual customization</li>
 *   <li><strong>Dynamic Generator Support:</strong> Works with any OpenAPI Generator (spring, java, etc.) from configuration</li>
 *   <li><strong>Incremental Build Support:</strong> Only regenerates when inputs actually change using @Internal property optimization</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * openapiModelgen {
 *     defaults {
 *         validateSpec true
 *         modelNameSuffix "Dto" 
 *         
 *         // Configure template resolution (unified approach)
 *         templateSources([
 *             "user-templates",          // Project Mustache templates (highest)
 *             "user-customizations",     // Project YAML customizations
 *             "library-templates",       // Library Mustache templates  
 *             "library-customizations",  // Library YAML customizations
 *             "plugin-customizations",   // Built-in plugin customizations
 *             "openapi-generator"        // OpenAPI Generator defaults (lowest)
 *         ])
 *         debug true   // Enable comprehensive debug logging
 *         
 *         templateVariables([
 *             copyright: "Copyright © {{currentYear}} {{companyName}}",
 *             currentYear: "2025",
 *             companyName: "MyCompany Inc."
 *         ])
 *     }
 *     specs {
 *         pets {
 *             inputSpec "src/main/resources/openapi-spec/pets.yaml"
 *             modelPackage "com.example.model.pets"
 *         }
 *         orders {
 *             inputSpec "src/main/resources/openapi-spec/orders.yaml" 
 *             modelPackage "com.example.model.orders"
 *             validateSpec false  // Override for legacy spec
 *             generateModelTests false  // Skip tests for this spec
 *             generateApiDocumentation true  // Generate docs despite being legacy
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
public class OpenApiModelGenPlugin implements Plugin<Project> {
    private static final Logger logger = PluginLoggerFactory.getLogger(OpenApiModelGenPlugin.class);
    
    // Configuration-cache compatible services
    private final ConfigurationValidator configurationValidator;
    private final LibraryProcessor libraryProcessor;
    private final TaskConfigurationService taskConfigurationService;

    /**
     * Default constructor that initializes all services.
     */
    public OpenApiModelGenPlugin() {
        this.configurationValidator = new ConfigurationValidator();
        this.libraryProcessor = new LibraryProcessor();
        this.taskConfigurationService = new TaskConfigurationService();
    }
    
    /**
     * Applies the OpenAPI Model Generator plugin to the given project.
     * 
     * <p>This method:</p>
     * <ul>
     *   <li>Ensures the OpenAPI Generator plugin is available</li>
     *   <li>Applies the OpenAPI Generator plugin as a dependency</li>
     *   <li>Creates and configures the 'openapiModelgen' extension</li>
     *   <li>Sets up validation and task creation after project evaluation</li>
     * </ul>
     * 
     * @param project the Gradle project to apply this plugin to
     */
    @Override
    public void apply(@NotNull Project project) {
        // Detect and ensure OpenAPI Generator plugin is available
        ensureOpenApiGeneratorAvailable(project);
        
        // Apply the OpenAPI Generator plugin as a dependency
        project.getPlugins().apply(OpenApiGeneratorPlugin.class);
        
        // Create the template library configuration
        Configuration customizationsConfig = project.getConfigurations().create(PluginConstants.LIBRARIES_CONFIG_NAME, config -> {
            config.setDescription("OpenAPI template and customization libraries");
            config.setCanBeConsumed(false);  // Not exposed to consumers
            config.setCanBeResolved(true);   // Can resolve dependencies
            config.setVisible(true);          // Visible in dependency reports
            config.setTransitive(true);       // Include transitive dependencies
        });
        
        // Create our extension
        OpenApiModelGenExtension extension = project.getExtensions()
            .create("openapiModelgen", OpenApiModelGenExtension.class, project);
        
        // Configure tasks after project evaluation
        project.afterEvaluate(proj -> {
            configurationValidator.validateExtensionConfiguration(proj, proj.getLayout(), extension);

            // Set build directory for rich file logging
            PluginState.getInstance().setBuildDirectory(proj.getLayout().getBuildDirectory().getAsFile().get());
            
            // Check for library dependencies and process if needed
            if (libraryProcessor.shouldProcessLibraries(extension, customizationsConfig)) {
                libraryProcessor.processLibraryDependencies(proj, customizationsConfig, extension);
                
                // Validate library version compatibility after processing
                LibraryTemplateExtractor.LibraryExtractionResult libraryContent = extension.getLibraryContent();
                if (libraryContent != null && !libraryContent.getMetadata().isEmpty()) {
                    List<String> versionErrors = validateAllLibraryVersionCompatibility(proj, libraryContent);
                    if (!versionErrors.isEmpty()) {
                        StringBuilder errorMessage = new StringBuilder("Library version compatibility validation failed:");
                        for (String error : versionErrors) {
                            errorMessage.append("\n  - ").append(error);
                        }
                        throw new InvalidUserDataException(errorMessage.toString());
                    }
                }
            }
            
            taskConfigurationService.createTasksForSpecs(proj, extension);

            // Integrate with Java plugin if present (automatic source set and compile dependency setup)
            configureJavaIntegration(proj, extension);
        });
    }

    /**
     * Configures automatic integration with the Java plugin when present.
     *
     * <p>This method handles:</p>
     * <ul>
     *   <li>Registering generated sources with the main source set (one directory per spec)</li>
     *   <li>Wiring compileJava to depend on generateAllModels</li>
     * </ul>
     *
     * <p>Each spec automatically gets its own output subdirectory to prevent build cache conflicts.
     * For example, with specs "pets" and "orders", the plugin registers:</p>
     * <ul>
     *   <li>{@code build/generated/sources/openapi/pets/src/main/java}</li>
     *   <li>{@code build/generated/sources/openapi/orders/src/main/java}</li>
     * </ul>
     *
     * <p>This eliminates the need for users to manually configure sourceSets or task dependencies.</p>
     *
     * @param project the Gradle project
     * @param extension the plugin extension containing configuration
     */
    private void configureJavaIntegration(Project project, OpenApiModelGenExtension extension) {
        // Only integrate if Java plugin is applied
        if (!project.getPlugins().hasPlugin(JavaPlugin.class)) {
            logger.debug("Java plugin not found - skipping automatic source set integration");
            return;
        }

        // Skip if no specs are configured
        if (extension.getSpecs().isEmpty()) {
            logger.debug("No specs configured - skipping Java integration");
            return;
        }

        try {
            JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            SourceSet mainSourceSet = javaExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

            // Collect all unique output directories from specs
            extension.getSpecs().forEach((specName, specConfig) -> {
                ResolvedSpecConfig resolvedConfig = ResolvedSpecConfig.builder(specName, extension, specConfig).build();
                String outputDir = resolvedConfig.getOutputDir();

                // The OpenAPI Generator outputs to {outputDir}/src/main/java
                File generatedSourcesDir = new File(project.getProjectDir(), outputDir + "/src/main/java");

                // Register with source set if not already present
                if (!mainSourceSet.getJava().getSrcDirs().contains(generatedSourcesDir)) {
                    mainSourceSet.getJava().srcDir(generatedSourcesDir);
                    logger.info("Registered generated sources directory: {}", generatedSourcesDir.getAbsolutePath());
                }
            });

            // Wire compileJava to depend on generateAllModels
            TaskProvider<?> generateAllModelsTask = project.getTasks().named(PluginConstants.TASK_ALL_MODELS);
            project.getTasks().withType(JavaCompile.class).configureEach(compileTask -> {
                // Only configure main compile task (compileJava), not test compile
                if (compileTask.getName().equals("compileJava")) {
                    compileTask.dependsOn(generateAllModelsTask);
                    logger.info("Configured {} to depend on {}", compileTask.getName(), PluginConstants.TASK_ALL_MODELS);
                }
            });

            logger.info("Java plugin integration configured successfully");

        } catch (Exception e) {
            logger.warn("Failed to configure Java plugin integration: {}. " +
                       "Manual configuration may be required.", e.getMessage());
        }
    }

    /**
     * Validates OpenAPI Generator version compatibility and logs warnings for untested versions.
     * This method is called from ensureOpenApiGeneratorAvailable() when a version is detected.
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "REC_CATCH_EXCEPTION", 
        justification = "Defensive programming: version parsing and comparison can fail in various ways (NumberFormatException, " +
                       "StringIndexOutOfBoundsException, etc.) and we want to gracefully handle all parsing failures by " +
                       "treating unknown/malformed versions as compatible rather than failing the build."
    )
    private void validateVersionCompatibility(String version, Project project) {
        if (version == null || version.trim().isEmpty() || Objects.equals(version, PluginConstants.UNKNOWN_VERSION)) {
            return; // Nothing to validate
        }
        
        try {
            // Define known compatible version ranges
            String minTestedVersion = PluginConstants.MIN_OPENAPI_GENERATOR_VERSION;
            String maxTestedVersion = PluginConstants.MAX_OPENAPI_GENERATOR_VERSION;
            
            // Log the detected version
            project.getLogger().debug("Validating OpenAPI Generator version compatibility: {}", version);
            
            // Parse version for comparison (semantic versioning)
            if (VersionUtils.isVersionBelow(version, minTestedVersion)) {
                project.getLogger().warn("OpenAPI Generator version {} is below the minimum tested version {}. " +
                    "Consider upgrading to ensure compatibility with all plugin features.", version, minTestedVersion);
            } else if (VersionUtils.isVersionAbove(version, maxTestedVersion)) {
                project.getLogger().info("OpenAPI Generator version {} is newer than the maximum tested version {}. " +
                    "The plugin should work but some features may behave differently.", version, maxTestedVersion);
            } else {
                project.getLogger().debug("OpenAPI Generator version {} is within the tested compatibility range", version);
            }
            
            // Check for known problematic versions
            if (version.startsWith("6.") || version.startsWith("5.")) {
                project.getLogger().warn("OpenAPI Generator version {} may not be compatible with this plugin. " +
                    "Please upgrade to version {} or later.", version, minTestedVersion);
            }
            
        } catch (Exception e) {
            project.getLogger().debug("Error validating OpenAPI Generator version '{}': {}", version, e.getMessage());
        }
    }
    
    /**
     * Validates a specific library's compatibility with the detected OpenAPI Generator version.
     * This method is separate from general version validation and focuses on library-specific requirements.
     * 
     * @param libraryName the name of the library being validated
     * @param metadata the library's metadata containing version requirements
     * @param detectedGeneratorVersion the detected OpenAPI Generator version
     * @param errors list to collect validation errors
     */
    private void validateLibraryVersionCompatibility(String libraryName, LibraryMetadata metadata, 
                                                   String detectedGeneratorVersion, List<String> errors) {
        if (metadata == null || detectedGeneratorVersion == null || Objects.equals(detectedGeneratorVersion, PluginConstants.UNKNOWN_VERSION)) {
            return; // Cannot validate without proper inputs
        }
        
        try {
            // Validate minimum OpenAPI Generator version requirement
            if (metadata.getMinOpenApiGeneratorVersion() != null) {
                if (VersionUtils.isVersionBelow(detectedGeneratorVersion, metadata.getMinOpenApiGeneratorVersion())) {
                    errors.add(String.format("Library '%s' requires OpenAPI Generator version %s+ but detected version is %s", 
                              libraryName, metadata.getMinOpenApiGeneratorVersion(), detectedGeneratorVersion));
                } else {
                    logger.debug("Library '{}' minimum version requirement satisfied: {} >= {}", 
                               libraryName, detectedGeneratorVersion, metadata.getMinOpenApiGeneratorVersion());
                }
            }
            
            // Validate maximum OpenAPI Generator version requirement
            if (metadata.getMaxOpenApiGeneratorVersion() != null) {
                if (VersionUtils.isVersionAbove(detectedGeneratorVersion, metadata.getMaxOpenApiGeneratorVersion())) {
                    errors.add(String.format("Library '%s' supports OpenAPI Generator up to version %s but detected version is %s", 
                              libraryName, metadata.getMaxOpenApiGeneratorVersion(), detectedGeneratorVersion));
                } else {
                    logger.debug("Library '{}' maximum version requirement satisfied: {} <= {}", 
                               libraryName, detectedGeneratorVersion, metadata.getMaxOpenApiGeneratorVersion());
                }
            }
            
            // Log required features for future enhancement
            if (metadata.getRequiredFeatures() != null && !metadata.getRequiredFeatures().isEmpty()) {
                logger.debug("Library '{}' requires features: {} (feature validation not yet implemented)", 
                           libraryName, metadata.getRequiredFeatures());
            }
            
        } catch (Exception e) {
            logger.warn("Error validating library '{}' version compatibility: {}", libraryName, e.getMessage());
        }
    }
    
    /**
     * Validates all library dependencies against the detected OpenAPI Generator version.
     * This method should be called after library processing to ensure version compatibility.
     * 
     * @param project the Gradle project
     * @param libraryContent the extracted library content with metadata
     * @return list of validation errors, empty if all libraries are compatible
     */
    public List<String> validateAllLibraryVersionCompatibility(Project project, LibraryTemplateExtractor.LibraryExtractionResult libraryContent) {
        List<String> errors = new ArrayList<>();
        
        if (libraryContent == null || libraryContent.getMetadata().isEmpty()) {
            return errors; // No libraries to validate
        }
        
        try {
            // Detect current OpenAPI Generator version
            String detectedVersion = detectOpenApiGeneratorVersion(project);
            
            if (detectedVersion == null || Objects.equals(detectedVersion, PluginConstants.UNKNOWN_VERSION)) {
                logger.debug("Cannot validate library version compatibility - OpenAPI Generator version unknown");
                return errors;
            }
            
            logger.debug("Validating {} libraries against OpenAPI Generator version {}", 
                        libraryContent.getMetadata().size(), detectedVersion);
            
            // Validate each library's version requirements
            for (Map.Entry<String, LibraryMetadata> entry : libraryContent.getMetadata().entrySet()) {
                String libraryName = entry.getKey();
                LibraryMetadata metadata = entry.getValue();
                
                validateLibraryVersionCompatibility(libraryName, metadata, detectedVersion, errors);
            }
            
            if (errors.isEmpty()) {
                logger.debug("All {} libraries are compatible with OpenAPI Generator version {}", 
                            libraryContent.getMetadata().size(), detectedVersion);
            } else {
                logger.warn("Found {} library version compatibility issues", errors.size());
            }
            
        } catch (Exception e) {
            logger.warn("Error during library version compatibility validation: {}", e.getMessage());
            errors.add("Error validating library version compatibility: " + e.getMessage());
        }
        
        return errors;
    }

    /**
     * Detects and logs the OpenAPI Generator version being used.
     * The plugin includes a default version but respects overrides from configuration plugins.
     */
    private void ensureOpenApiGeneratorAvailable(Project project) {
        try {
            String detectedVersion = detectOpenApiGeneratorVersion(project);
            
            if (detectedVersion != null && !Objects.equals(detectedVersion, PluginConstants.UNKNOWN_VERSION)) {
                project.getLogger().info("Using OpenAPI Generator version: {}", detectedVersion);
                validateVersionCompatibility(detectedVersion, project);
            } else {
                project.getLogger().info("Using OpenAPI Generator (version detection unavailable)");
            }
            
        } catch (Exception e) {
            project.getLogger().debug("Could not detect OpenAPI Generator version: {}", e.getMessage());
        }
    }
    
    /**
     * Detects the OpenAPI Generator version from project dependencies.
     * This method is public static to allow sharing with VersionDetectionService.
     * @param project the Gradle project
     * @return the detected OpenAPI Generator version, or null if not found
     */
    public static String detectOpenApiGeneratorVersion(Project project) {
        try {
            // Check if OpenAPI Generator classes are available
            Class.forName("org.openapitools.generator.gradle.plugin.OpenApiGeneratorPlugin");
            
            // Try to find version from dependencies
            return project.getConfigurations().stream()
                .filter(Configuration::isCanBeResolved)
                .flatMap(config -> {
                    try {
                        return config.getAllDependencies().stream();
                    } catch (Exception e) {
                        return Stream.empty();
                    }
                })
                .filter(dep -> "org.openapitools".equals(dep.getGroup()) && 
                             "openapi-generator-gradle-plugin".equals(dep.getName()))
                .map(Dependency::getVersion)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(detectVersionFromClasspath());
                
        } catch (ClassNotFoundException e) {
            return null; // OpenAPI Generator not available
        } catch (Exception e) {
            logger.debug("Could not detect OpenAPI Generator version: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Attempts to detect version from JAR manifest in classpath
     */
    private static String detectVersionFromClasspath() {
        try {
            Class<?> generatorClass = Class.forName("org.openapitools.generator.gradle.plugin.OpenApiGeneratorPlugin");
            String classPath = generatorClass.getProtectionDomain().getCodeSource().getLocation().getPath();
            
            // Extract version from JAR filename if possible
            if (classPath.contains("openapi-generator-gradle-plugin")) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("openapi-generator-gradle-plugin-([0-9]+\\.[0-9]+\\.[0-9]+)");
                java.util.regex.Matcher matcher = pattern.matcher(classPath);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            
            return PluginConstants.UNKNOWN_VERSION; // Present but version unknown
        } catch (Exception e) {
            logger.warn("Failed to detect OpenAPI Generator version from classpath: {}", e.getMessage());
            return null;
        }
    }
}