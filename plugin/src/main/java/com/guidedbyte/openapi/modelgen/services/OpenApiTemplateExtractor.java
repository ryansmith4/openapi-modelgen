package com.guidedbyte.openapi.modelgen.services;

import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenConfigLoader;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class to extract OpenAPI Generator templates programmatically
 * without requiring the CLI dependency.
 */
public class OpenApiTemplateExtractor {

    /**
     * Extract templates for a given generator to a specified output directory
     * 
     * @param generatorName The name of the generator (e.g., "java", "spring", "typescript-axios")
     * @param outputDir The directory where templates should be extracted
     * @param library Optional library sub-template (can be null)
     * @throws Exception if extraction fails
     */
    public static void extractTemplates(String generatorName, String outputDir, String library) 
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
        extractTemplatesFromClasspath(embeddedTemplateDir, outputDir, library);
    }
    
    /**
     * Extract templates from the classpath (works with both JAR and file system)
     */
    private static void extractTemplatesFromClasspath(String templateDir, String outputDir, String library) 
            throws Exception {
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        // Try to get the resource URL
        String resourcePath = templateDir;
        URL resourceUrl = classLoader.getResource(resourcePath);
        
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Template directory not found: " + resourcePath);
        }
        
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);
        
        if (resourceUrl.getProtocol().equals("jar")) {
            // Extract from JAR file
            extractFromJar(resourceUrl, resourcePath, outputPath, library);
        } else {
            // Extract from file system (development mode)
            extractFromFileSystem(Paths.get(resourceUrl.toURI()), outputPath, library);
        }
    }
    
    /**
     * Extract templates from a JAR file
     */
    private static void extractFromJar(URL jarUrl, String resourcePath, Path outputPath, String library) 
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
                    
                    // Create parent directories
                    Files.createDirectories(targetPath.getParent());
                    
                    // Copy file
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    
                    System.out.println("Extracted: " + relativePath);
                }
            }
        }
    }
    
    /**
     * Extract templates from file system (development mode)
     */
    private static void extractFromFileSystem(Path sourcePath, Path outputPath, String library) 
            throws Exception {
        
        Files.walk(sourcePath)
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
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourceFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    
                    System.out.println("Extracted: " + relativePath);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to copy file: " + sourceFile, e);
                }
            });
    }
    
    /**
     * Get information about available generators
     */
    public static void listAvailableGenerators() {
        System.out.println("Available generators:");
        for (CodegenConfig generator : CodegenConfigLoader.getAll()) {
            System.out.println("- " + generator.getName() + ": " + generator.getHelp());
        }
    }
    
    /**
     * Get the embedded template directory for a specific generator
     */
    public static String getEmbeddedTemplateDir(String generatorName) throws Exception {
        CodegenConfig config = CodegenConfigLoader.forName(generatorName);
        if (config == null) {
            throw new IllegalArgumentException("Generator '" + generatorName + "' not found");
        }
        return config.embeddedTemplateDir();
    }
    
    /**
     * Example usage and test method
     */
    public static void main(String[] args) {
        try {
            // List available generators
            listAvailableGenerators();
            
            // Extract Java templates
            System.out.println("\nExtracting Java templates...");
            extractTemplates("java", "./extracted-templates/java", null);
            
            // Extract Java templates with specific library
            System.out.println("\nExtracting Java WebClient templates...");
            extractTemplates("java", "./extracted-templates/java-webclient", "webclient");
            
            // Extract Spring templates
            System.out.println("\nExtracting Spring templates...");
            extractTemplates("spring", "./extracted-templates/spring", null);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}