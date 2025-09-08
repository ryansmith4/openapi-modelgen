package com.guidedbyte.openapi.modelgen.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Template extractor using OpenAPI Generator's CodegenConfig API.
 * 
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
            logger.debug("Failed to load template resource '{}': {}", templatePath, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Saves a template to a file in the working directory.
     * 
     * @param templateName the name of the template file
     * @param content the template content to save
     * @param workingDirectory the working directory to save the template in
     * @throws IOException if file writing fails
     */
    public void saveTemplate(String templateName, String content, File workingDirectory) throws IOException {
        File templateFile = new File(workingDirectory, templateName);
        Files.createDirectories(templateFile.getParentFile().toPath());
        Files.writeString(templateFile.toPath(), content);
        logger.debug("Saved template to: {}", templateFile.getAbsolutePath());
    }
}