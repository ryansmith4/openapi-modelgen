package com.guidedbyte.openapi.modelgen.services;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Detects the OpenAPI Generator version being used in the project.
 * 
 * <p>This service attempts multiple detection strategies in order of reliability:</p>
 * <ol>
 *   <li><strong>Plugin Version Detection:</strong> Extract version from applied OpenAPI Generator plugin</li>
 *   <li><strong>Dependency Analysis:</strong> Find version from resolved project dependencies</li>
 *   <li><strong>Classpath Inspection:</strong> Extract version from OpenAPI Generator classes on classpath</li>
 * </ol>
 * 
 * <p>If all strategies fail, the detector throws a {@link GradleException} with guidance
 * on how to properly configure the OpenAPI Generator plugin. This fail-fast behavior
 * ensures that version-conditional template customizations work reliably.</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class OpenApiGeneratorVersionDetector {
    private static final Logger logger = LoggerFactory.getLogger(OpenApiGeneratorVersionDetector.class);
    
    private static final String OPENAPI_TOOLS_GROUP = "org.openapitools";
    private static final String OPENAPI_GENERATOR_ARTIFACT = "openapi-generator";
    private static final String OPENAPI_GENERATOR_CORE_ARTIFACT = "openapi-generator-core";
    
    /**
     * Detects the OpenAPI Generator version or fails fast if unable to determine.
     * 
     * @param project the Gradle project to analyze
     * @return the detected OpenAPI Generator version
     * @throws GradleException if version cannot be reliably detected
     */
    public String detectVersionOrFail(Project project) {
        logger.debug("Attempting to detect OpenAPI Generator version...");
        
        // Strategy 1: Plugin version detection
        String pluginVersion = detectFromPlugin(project);
        if (pluginVersion != null) {
            logger.info("Detected OpenAPI Generator version from plugin: {}", pluginVersion);
            return pluginVersion;
        }
        
        // Strategy 2: Dependency analysis  
        String depVersion = detectFromDependencies(project);
        if (depVersion != null) {
            logger.info("Detected OpenAPI Generator version from dependencies: {}", depVersion);
            return depVersion;
        }
        
        // Strategy 3: Classpath inspection
        String classpathVersion = detectFromClasspath();
        if (classpathVersion != null) {
            logger.info("Detected OpenAPI Generator version from classpath: {}", classpathVersion);
            return classpathVersion;
        }
        
        // FAIL FAST - no fallback allowed
        throw new GradleException(
            "Unable to detect OpenAPI Generator version. This is required for " +
            "version-conditional template customizations to work correctly.\n\n" +
            "Please ensure the OpenAPI Generator plugin is properly configured:\n" +
            "  plugins {\n" +
            "    id 'org.openapi.generator' version '7.14.0'\n" +
            "  }\n\n" +
            "Or add the dependency explicitly:\n" +
            "  dependencies {\n" +
            "    implementation 'org.openapitools:openapi-generator:7.14.0'\n" +
            "  }"
        );
    }
    
    /**
     * Attempts to detect the OpenAPI Generator version from applied plugins.
     * 
     * @param project the project to examine
     * @return the detected version or null if not found
     */
    private String detectFromPlugin(Project project) {
        logger.debug("Searching for OpenAPI Generator plugin...");
        
        return project.getPlugins().stream()
            .filter(plugin -> {
                String className = plugin.getClass().getName();
                return className.contains("openapi") || 
                       className.contains("OpenApi") ||
                       className.contains("OpenAPI");
            })
            .map(this::extractVersionFromPlugin)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Extracts version information from a plugin instance.
     * 
     * @param plugin the plugin to examine
     * @return the version or null if not extractable
     */
    private String extractVersionFromPlugin(Object plugin) {
        try {
            // Try to get version from plugin implementation
            Package pluginPackage = plugin.getClass().getPackage();
            if (pluginPackage != null) {
                String implementationVersion = pluginPackage.getImplementationVersion();
                if (implementationVersion != null && !implementationVersion.isEmpty()) {
                    logger.debug("Found plugin implementation version: {}", implementationVersion);
                    return implementationVersion;
                }
                
                String specificationVersion = pluginPackage.getSpecificationVersion();
                if (specificationVersion != null && !specificationVersion.isEmpty()) {
                    logger.debug("Found plugin specification version: {}", specificationVersion);
                    return specificationVersion;
                }
            }
            
            logger.debug("No version information found in plugin: {}", plugin.getClass().getName());
            return null;
        } catch (Exception e) {
            logger.debug("Error extracting version from plugin {}: {}", 
                        plugin.getClass().getName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Attempts to detect the OpenAPI Generator version from project dependencies.
     * 
     * @param project the project to examine
     * @return the detected version or null if not found
     */
    private String detectFromDependencies(Project project) {
        logger.debug("Searching project dependencies for OpenAPI Generator...");
        
        return project.getConfigurations().stream()
            .filter(config -> !StringUtils.toRootLowerCase(config.getName()).contains("test"))
            .flatMap(config -> {
                try {
                    return config.getResolvedConfiguration()
                        .getResolvedArtifacts().stream();
                } catch (Exception e) {
                    logger.debug("Unable to resolve configuration {}: {}", config.getName(), e.getMessage());
                    return Stream.empty();
                }
            })
            .filter(this::isOpenApiGeneratorArtifact)
            .map(artifact -> artifact.getModuleVersion().getId().getVersion())
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Checks if an artifact is related to OpenAPI Generator.
     * 
     * @param artifact the artifact to check
     * @return true if this is an OpenAPI Generator artifact
     */
    private boolean isOpenApiGeneratorArtifact(ResolvedArtifact artifact) {
        String group = artifact.getModuleVersion().getId().getGroup();
        String name = artifact.getModuleVersion().getId().getName();
        
        return OPENAPI_TOOLS_GROUP.equals(group) && 
               (OPENAPI_GENERATOR_ARTIFACT.equals(name) || 
                OPENAPI_GENERATOR_CORE_ARTIFACT.equals(name) ||
                name.startsWith("openapi-generator-"));
    }
    
    /**
     * Attempts to detect the OpenAPI Generator version from the classpath.
     * 
     * @return the detected version or null if not found
     */
    private String detectFromClasspath() {
        logger.debug("Searching classpath for OpenAPI Generator classes...");
        
        // Try multiple known OpenAPI Generator classes
        String[] classesToTry = {
            "org.openapitools.codegen.DefaultGenerator",
            "org.openapitools.codegen.CodegenConfig", 
            "org.openapitools.codegen.DefaultCodegen"
        };
        
        for (String className : classesToTry) {
            try {
                Class<?> generatorClass = Class.forName(className);
                String version = extractVersionFromClass(generatorClass);
                if (version != null) {
                    logger.debug("Found version {} from class {}", version, className);
                    return version;
                }
            } catch (ClassNotFoundException e) {
                logger.debug("Class {} not found on classpath", className);
            } catch (Exception e) {
                logger.debug("Error examining class {}: {}", className, e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * Extracts version information from a class.
     * 
     * @param clazz the class to examine
     * @return the version or null if not extractable
     */
    private String extractVersionFromClass(Class<?> clazz) {
        try {
            Package classPackage = clazz.getPackage();
            if (classPackage != null) {
                String implementationVersion = classPackage.getImplementationVersion();
                if (implementationVersion != null && !implementationVersion.isEmpty()) {
                    return implementationVersion;
                }
                
                String specificationVersion = classPackage.getSpecificationVersion();
                if (specificationVersion != null && !specificationVersion.isEmpty()) {
                    return specificationVersion;
                }
            }
            
            // Try to get version from jar manifest if available
            String protectionDomain = clazz.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toString();
            
            if (protectionDomain.contains("openapi-generator")) {
                // Extract version from jar file name if it follows standard naming
                // e.g., openapi-generator-7.14.0.jar
                int start = protectionDomain.indexOf("openapi-generator-") + 18;
                int end = protectionDomain.indexOf(".jar");
                if (start > 17 && end > start) {
                    String version = protectionDomain.substring(start, end);
                    // Validate that it looks like a version
                    if (version.matches("\\d+\\.\\d+.*")) {
                        return version;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.debug("Error extracting version from class {}: {}", clazz.getName(), e.getMessage());
            return null;
        }
    }
}