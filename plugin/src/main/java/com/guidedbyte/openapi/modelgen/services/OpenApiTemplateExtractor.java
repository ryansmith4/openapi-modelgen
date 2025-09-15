package com.guidedbyte.openapi.modelgen.services;

import org.gradle.api.file.FileSystemOperations;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class to extract OpenAPI Generator templates programmatically
 * without requiring the CLI dependency.
 */
public class OpenApiTemplateExtractor {
    private static final Logger logger = LoggerFactory.getLogger(OpenApiTemplateExtractor.class);
    
    /**
     * Constructs a new OpenApiTemplateExtractor.
     */
    public OpenApiTemplateExtractor() {
        // Default constructor
    }

    /**
     * Extract templates for a given generator to a specified output directory
     * 
     * @param generatorName The name of the generator (e.g., "java", "spring", "typescript-axios")
     * @param outputDir The directory where templates should be extracted
     * @param library Optional library sub-template (can be null)
     * @param fileSystemOperations Gradle FileSystemOperations for file operations
     * @throws Exception if extraction fails
     */
    public static void extractTemplates(String generatorName, String outputDir, String library, FileSystemOperations fileSystemOperations) 
            throws Exception {
        
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
        
        // Set library if specified
        if (library != null && !library.isEmpty()) {
            config.setLibrary(library);
        }
        
        // Extract templates from classpath
        extractTemplatesFromClasspath(embeddedTemplateDir, outputDir, library, fileSystemOperations);
    }

    
    /**
     * Extract templates from the classpath (works with both JAR and file system)
     */
    private static void extractTemplatesFromClasspath(String templateDir, String outputDir, String library, FileSystemOperations fileSystemOperations) 
            throws Exception {
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        // Try to get the resource URL
        URL resourceUrl = classLoader.getResource(templateDir);
        
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Template directory not found: " + templateDir);
        }
        
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);
        
        if (resourceUrl.getProtocol().equals("jar")) {
            // Extract from JAR file
            extractFromJar(resourceUrl, templateDir, outputPath, library, fileSystemOperations);
        } else {
            // Extract from file system (development mode)
            extractFromFileSystem(Paths.get(resourceUrl.toURI()), outputPath, library, fileSystemOperations);
        }
    }
    
    /**
     * Extract templates from a JAR file
     */
    private static void extractFromJar(URL jarUrl, String resourcePath, Path outputPath, String library, FileSystemOperations fileSystemOperations) 
            throws Exception {
        
        String jarPath = jarUrl.getPath().substring(5, jarUrl.getPath().indexOf("!"));
        
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                // Check if entry is in the template directory
                if (entryName.startsWith(resourcePath + "/") && !entry.isDirectory()) {
                    
                    // Handle library-specific templates
                    if (library != null && !library.isEmpty()) {
                        // Include library-specific templates and base templates
                        if (!entryName.contains("/libraries/" + library + "/") && 
                            entryName.contains("/libraries/")) {
                            continue; // Skip other library templates
                        }
                    }
                    
                    // Calculate relative path
                    String relativePath = entryName.substring(resourcePath.length() + 1);
                    Path targetPath = outputPath.resolve(relativePath);
                    
                    // Copy file from JAR entry
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        var parentPath = targetPath.getParent();
                        if (parentPath == null) {
                            throw new IllegalStateException(
                                String.format("Failed to resolve parent directory for target path '%s' when extracting '%s' from JAR '%s'", 
                                targetPath, entryName, jarPath));
                        }
                        Files.createDirectories(parentPath);
                        Files.copy(inputStream, targetPath);
                    }
                    
                    logger.debug("Extracted: {}", relativePath);
                }
            }
        }
    }
    
    /**
     * Extract templates from file system (development mode)
     */
    private static void extractFromFileSystem(Path sourcePath, Path outputPath, String library, FileSystemOperations fileSystemOperations)
            throws Exception {

        try (var pathStream = Files.walk(sourcePath)) {
            pathStream
                .filter(Files::isRegularFile)
                .forEach(sourceFile -> {
                    try {
                        Path relativePath = sourcePath.relativize(sourceFile);
                        String pathStr = relativePath.toString();

                        // Handle library-specific templates
                        if (library != null && !library.isEmpty()) {
                            if (!pathStr.contains("/libraries/" + library + "/") &&
                                pathStr.contains("/libraries/")) {
                                return; // Skip other library templates
                            }
                        }

                        Path targetPath = outputPath.resolve(relativePath);
                        var parentPath = targetPath.getParent();
                        if (parentPath == null) {
                            throw new IllegalStateException(
                                String.format("Failed to resolve parent directory for target path '%s' when extracting '%s' from filesystem '%s'",
                                targetPath, relativePath, sourceFile));
                        }
                        Files.createDirectories(parentPath);
                        fileSystemOperations.copy(copySpec -> {
                            copySpec.from(sourceFile);
                            copySpec.into(parentPath.toFile());
                        });

                        logger.debug("Extracted: {}", relativePath);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to copy file: " + sourceFile, e);
                    }
                });
        }
    }

    
    /**
     * Get information about available generators
     */
    public static void listAvailableGenerators() {
        logger.info("Available generators:");
        for (CodegenConfig generator : CodegenConfigLoader.getAll()) {
            logger.info("- {}: {}", generator.getName(), generator.getHelp());
        }
    }
    
    /**
     * Get the embedded template directory for a specific generator.
     * @param generatorName the generator name
     * @return the embedded template directory path
     * @throws Exception if the generator is not found or template directory cannot be retrieved
     */
    public static String getEmbeddedTemplateDir(String generatorName) throws Exception {
        CodegenConfig config = CodegenConfigLoader.forName(generatorName);
        if (config == null) {
            throw new IllegalArgumentException("Generator '" + generatorName + "' not found");
        }
        return config.embeddedTemplateDir();
    }
}