package com.guidedbyte.openapi.modelgen.services;

import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Template extractor using OpenAPI Generator's CodegenConfig API.
 * <p>
 * This approach uses the APIs that are available when users include
 * the OpenAPI Generator plugin, without requiring additional dependencies.
 * 
 * @since 1.1.0
 */
public class CodegenConfigTemplateExtractor {
    private static final Logger logger = LoggerFactory.getLogger(CodegenConfigTemplateExtractor.class);
    
    /**
     * Creates a new CodegenConfigTemplateExtractor.
     */
    public CodegenConfigTemplateExtractor() {
    }
    
    /**
     * Extracts a specific template using CodegenConfig API if available,
     * otherwise falls back to classpath resource loading.
     * 
     * @param templateName the name of the template to extract
     * @param generatorName the OpenAPI generator name (e.g., "spring")
     * @return the template content as a string, or null if not found
     */
    public String extractTemplate(String templateName, String generatorName) {
        logger.debug("Extracting template '{}' for generator '{}'", templateName, generatorName);
        
        // Try CodegenConfig API first
        String content = extractViaCodegenConfig(templateName, generatorName);
        if (content != null) {
            logger.debug("Successfully extracted template '{}' via CodegenConfig API", templateName);
            return content;
        }
        
        // Fall back to classpath resource loading
        content = extractViaClasspath(templateName);
        if (content != null) {
            logger.debug("Successfully extracted template '{}' via classpath fallback", templateName);
            return content;
        }
        
        logger.debug("Template '{}' not found", templateName);
        return null;
    }
    
    /**
     * Attempts to extract template using CodegenConfig API.
     */
    private String extractViaCodegenConfig(String templateName, String generatorName) {
        try {
            // Try to use CodegenConfigLoader if available
            Class<?> loaderClass = Class.forName("org.openapitools.codegen.CodegenConfigLoader");
            Class<?> configClass = Class.forName("org.openapitools.codegen.CodegenConfig");
            
            // Get the config for the generator
            Object config = loaderClass.getMethod("forName", String.class).invoke(null, generatorName);
            
            // Get embedded template directory path
            String templateDir = (String) configClass.getMethod("embeddedTemplateDir").invoke(config);
            
            if (templateDir != null) {
                // Try to load the template from the embedded template directory
                String templatePath = templateDir.endsWith("/") ? templateDir + templateName : templateDir + "/" + templateName;
                return loadTemplateResource(templatePath);
            }
            
        } catch (ClassNotFoundException e) {
            logger.debug("CodegenConfig classes not available, falling back to classpath search");
        } catch (Exception e) {
            logger.debug("Failed to use CodegenConfig API: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Fallback method using simple classpath resource loading.
     */
    private String extractViaClasspath(String templateName) {
        // Common template paths in OpenAPI Generator
        String[] templatePaths = {
            "JavaSpring/" + templateName,  // Primary path for Spring generator
            "Java/" + templateName         // Fallback for shared Java templates
        };
        
        for (String templatePath : templatePaths) {
            String content = loadTemplateResource(templatePath);
            if (content != null) {
                return content;
            }
        }
        
        return null;
    }
    
    /**
     * Loads a template resource from the classpath.
     */
    private String loadTemplateResource(String templatePath) {
        try {
            // Try loading from current thread's context classloader first
            try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(templatePath)) {
                if (stream != null) {
                    return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            
            // Fallback to this class's classloader
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(templatePath)) {
                if (stream != null) {
                    return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            
        } catch (IOException e) {
            logger.warn("Failed to load template resource '{}': {}", templatePath, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extracts all available templates for a generator to the specified directory.
     * Uses OpenAPI Generator's template extraction mechanism.
     * 
     * @param generatorName the OpenAPI generator name (e.g., "spring")
     * @param targetDirectory the directory to save all templates to
     * @return the number of templates successfully extracted
     */
    public int extractAllTemplates(String generatorName, File targetDirectory) {
        logger.debug("Extracting all templates for generator '{}' to: {}", generatorName, targetDirectory.getAbsolutePath());
        
        try {
            // Create target directory if it doesn't exist
            Files.createDirectories(targetDirectory.toPath());
            
            // Extract templates using direct approach (avoiding deprecated OpenApiTemplateExtractor method)
            extractTemplatesDirectly(generatorName, targetDirectory);
            
            // Count the extracted files
            File[] extractedFiles = targetDirectory.listFiles((dir, name) -> name.endsWith(".mustache"));
            int extracted = extractedFiles != null ? extractedFiles.length : 0;
            
            logger.info("Successfully extracted {} templates using OpenApiTemplateExtractor", extracted);
            return extracted;
            
        } catch (Exception e) {
            logger.warn("Failed to extract all templates using OpenApiTemplateExtractor: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Extracts templates directly without using deprecated OpenApiTemplateExtractor methods.
     * This replicates the core functionality needed for template extraction.
     * 
     * @param generatorName the OpenAPI generator name
     * @param targetDirectory the directory to extract templates to
     * @throws Exception if extraction fails
     */
    private void extractTemplatesDirectly(String generatorName, File targetDirectory) throws Exception {
        // Load the generator configuration
        CodegenConfig config = CodegenConfigLoader.forName(generatorName);
        
        if (config == null) {
            throw new IllegalArgumentException("Generator '" + generatorName + "' not found");
        }
        
        // Get the embedded template directory
        String embeddedTemplateDir = config.embeddedTemplateDir();
        if (embeddedTemplateDir == null || embeddedTemplateDir.isEmpty()) {
            embeddedTemplateDir = generatorName; // fallback to generator name
        }
        
        // Use basic file operations for template extraction
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String resourcePath = embeddedTemplateDir;
        URL resourceUrl = classLoader.getResource(resourcePath);
        
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Template directory not found: " + resourcePath);
        }
        
        Path outputPath = targetDirectory.toPath();
        Files.createDirectories(outputPath);
        
        // Extract templates based on resource location (JAR vs filesystem)
        if (resourceUrl.getProtocol().equals("jar")) {
            extractFromJar(resourceUrl, resourcePath, outputPath);
        } else {
            extractFromFileSystem(Paths.get(resourceUrl.toURI()), outputPath);
        }
    }
    
    /**
     * Extract templates from a JAR file.
     */
    private void extractFromJar(URL jarUrl, String resourcePath, Path outputPath) throws Exception {
        String jarPath = jarUrl.getPath().substring(5, jarUrl.getPath().indexOf("!"));
        
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                if (entryName.startsWith(resourcePath + "/") && !entry.isDirectory()) {
                    String relativePath = entryName.substring((resourcePath + "/").length());
                    Path targetPath = outputPath.resolve(relativePath);
                    
                    Path parentPath = targetPath.getParent();
                    if (parentPath != null) {
                        Files.createDirectories(parentPath);
                    }
                    
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        Files.copy(inputStream, targetPath);
                        logger.debug("Extracted from JAR: {}", relativePath);
                    }
                }
            }
        }
    }
    
    /**
     * Extract templates from the file system.
     */
    private void extractFromFileSystem(Path sourcePath, Path outputPath) throws Exception {
        try (var pathStream = Files.walk(sourcePath)) {
            pathStream
                .filter(Files::isRegularFile)
                .forEach(sourceFile -> {
                    try {
                        Path relativePath = sourcePath.relativize(sourceFile);
                        Path targetPath = outputPath.resolve(relativePath);

                        Path parentPath = targetPath.getParent();
                        if (parentPath != null) {
                            Files.createDirectories(parentPath);
                        }
                        Files.copy(sourceFile, targetPath);

                        logger.debug("Extracted from filesystem: {}", relativePath);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to copy file: " + sourceFile, e);
                    }
                });
        }
    }
    
}