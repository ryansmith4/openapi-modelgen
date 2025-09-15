package com.guidedbyte.openapi.modelgen.services;

import org.gradle.api.file.FileCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Service for extracting templates and customizations from library JARs.
 * 
 * <p>This service is designed to be configuration cache compatible with no Project dependencies.
 * It extracts template files and YAML customizations from library JARs at configuration time
 * for use during code generation.</p>
 * 
 * <h2>Library Structure:</h2>
 * <pre>
 * my-api-templates-1.0.0.jar
 * ├── META-INF/
 * │   ├── openapi-templates/
 * │   │   └── spring/
 * │   │       ├── pojo.mustache
 * │   │       └── model.mustache
 * │   └── openapi-customizations/
 * │       └── spring/
 * │           ├── pojo.mustache.yaml
 * │           └── model.mustache.yaml
 * </pre>
 * 
 * @since 1.1.0
 */
public class LibraryTemplateExtractor {
    
    /**
     * Constructs a new LibraryTemplateExtractor.
     */
    public LibraryTemplateExtractor() {
        // Default constructor
    }
    
    private static final Logger logger = LoggerFactory.getLogger(LibraryTemplateExtractor.class);
    
    private static final String TEMPLATES_BASE_PATH = "META-INF/openapi-templates/";
    private static final String CUSTOMIZATIONS_BASE_PATH = "META-INF/openapi-customizations/";
    private static final String METADATA_PATH = "META-INF/openapi-library.yaml";
    
    /**
     * Result container for extracted library content.
     */
    public static class LibraryExtractionResult {
        private final Map<String, String> templates;
        private final Map<String, String> customizations;
        private final Map<String, LibraryMetadata> metadata;
        
        /**
         * Constructs a LibraryExtractionResult with templates and customizations.
         * @param templates the templates map
         * @param customizations the customizations map
         */
        public LibraryExtractionResult(Map<String, String> templates, Map<String, String> customizations) {
            this(templates, customizations, new HashMap<>());
        }
        
        /**
         * Constructs a LibraryExtractionResult with templates, customizations, and metadata.
         * @param templates the templates map
         * @param customizations the customizations map
         * @param metadata the metadata map
         */
        public LibraryExtractionResult(Map<String, String> templates, Map<String, String> customizations, Map<String, LibraryMetadata> metadata) {
            this.templates = templates != null ? templates : new HashMap<>();
            this.customizations = customizations != null ? customizations : new HashMap<>();
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        /**
         * Gets the templates map.
         * @return the templates
         */
        public Map<String, String> getTemplates() {
            return templates;
        }
        
        /**
         * Gets the customizations map.
         * @return the customizations
         */
        public Map<String, String> getCustomizations() {
            return customizations;
        }
        
        /**
         * Gets the metadata map.
         * @return the metadata
         */
        public Map<String, LibraryMetadata> getMetadata() {
            return metadata;
        }
        
        /**
         * Checks if there are any templates.
         * @return true if templates are present
         */
        public boolean hasTemplates() {
            return !templates.isEmpty();
        }
        
        /**
         * Checks if there are any customizations.
         * @return true if customizations are present
         */
        public boolean hasCustomizations() {
            return !customizations.isEmpty();
        }
        
        /**
         * Checks if there is any metadata.
         * @return true if metadata is present
         */
        public boolean hasMetadata() {
            return !metadata.isEmpty();
        }
        
        /**
         * Checks if the extraction result is empty.
         * @return true if no templates or customizations are present
         */
        public boolean isEmpty() {
            return templates.isEmpty() && customizations.isEmpty();
        }
        
        /**
         * Gets metadata for a specific library JAR file.
         * @param libraryFileName the library file name
         * @return the library metadata, or null if not found
         */
        public LibraryMetadata getMetadataForLibrary(String libraryFileName) {
            return metadata.get(libraryFileName);
        }
        
        /**
         * Checks if a library supports the given generator.
         * @param libraryFileName the library file name
         * @param generatorName the generator name
         * @return true if the generator is supported
         */
        public boolean librarySupportsGenerator(String libraryFileName, String generatorName) {
            LibraryMetadata meta = metadata.get(libraryFileName);
            return meta == null || meta.supportsGenerator(generatorName);
        }
    }
    
    /**
     * Extracts templates and customizations from a collection of library JARs.
     * 
     * @param libraries the collection of library JAR files
     * @return extraction result containing all templates and customizations
     */
    public LibraryExtractionResult extractFromLibraries(FileCollection libraries) {
        if (libraries == null || libraries.isEmpty()) {
            logger.debug("No libraries provided for extraction");
            return new LibraryExtractionResult(null, null, null);
        }
        
        Map<String, String> allTemplates = new HashMap<>();
        Map<String, String> allCustomizations = new HashMap<>();
        Map<String, LibraryMetadata> allMetadata = new HashMap<>();
        
        for (File library : libraries) {
            if (!library.exists() || !library.isFile()) {
                logger.warn("Skipping invalid library file: {}", library.getAbsolutePath());
                continue;
            }
            
            logger.debug("Extracting content from library: {}", library.getName());
            extractFromJar(library, allTemplates, allCustomizations, allMetadata);
        }
        
        logger.info("Extracted {} templates, {} customizations, and {} metadata files from {} libraries",
            allTemplates.size(), allCustomizations.size(), allMetadata.size(), libraries.getFiles().size());
        
        return new LibraryExtractionResult(allTemplates, allCustomizations, allMetadata);
    }
    
    
    /**
     * Extracts templates, customizations, and metadata from a single JAR file.
     * 
     * @param jarFile the JAR file to extract from
     * @param templates map to store extracted templates
     * @param customizations map to store extracted customizations
     * @param metadata map to store extracted metadata
     */
    private void extractFromJar(File jarFile, Map<String, String> templates, Map<String, String> customizations, Map<String, LibraryMetadata> metadata) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                // Skip directories
                if (entry.isDirectory()) {
                    continue;
                }
                
                // Check if entry is a template
                if (entryName.startsWith(TEMPLATES_BASE_PATH)) {
                    String relativePath = entryName.substring(TEMPLATES_BASE_PATH.length());
                    String content = readEntryContent(jar, entry);
                    if (content != null) {
                        templates.put(relativePath, content);
                        logger.debug("Extracted template: {} from {}", relativePath, jarFile.getName());
                    }
                }
                // Check if entry is a customization
                else if (entryName.startsWith(CUSTOMIZATIONS_BASE_PATH)) {
                    String relativePath = entryName.substring(CUSTOMIZATIONS_BASE_PATH.length());
                    String content = readEntryContent(jar, entry);
                    if (content != null) {
                        customizations.put(relativePath, content);
                        logger.debug("Extracted customization: {} from {}", relativePath, jarFile.getName());
                    }
                }
                // Check if entry is metadata
                else if (entryName.equals(METADATA_PATH)) {
                    String content = readEntryContent(jar, entry);
                    if (content != null) {
                        LibraryMetadata libraryMetadata = parseMetadata(content, jarFile.getName());
                        if (libraryMetadata != null) {
                            metadata.put(jarFile.getName(), libraryMetadata);
                            logger.debug("Extracted metadata from {}: {}", jarFile.getName(), libraryMetadata.getName());
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to extract from JAR: {}", jarFile.getName(), e);
        }
    }
    
    /**
     * Reads the content of a JAR entry as a string.
     * 
     * @param jar the JAR file
     * @param entry the entry to read
     * @return the content as a string, or null if reading fails
     */
    private String readEntryContent(JarFile jar, JarEntry entry) {
        try (InputStream is = jar.getInputStream(entry)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read JAR entry: {}", entry.getName(), e);
            return null;
        }
    }
    
    /**
     * Parses metadata YAML content into a LibraryMetadata object.
     * 
     * @param yamlContent the YAML content to parse
     * @param libraryName the library name for error reporting
     * @return parsed metadata, or null if parsing fails
     */
    private LibraryMetadata parseMetadata(String yamlContent, String libraryName) {
        try {
            // Use the same YAML parser approach as the CustomizationEngine
            org.yaml.snakeyaml.LoaderOptions options = new org.yaml.snakeyaml.LoaderOptions();
            options.setMaxAliasesForCollections(50);
            options.setNestingDepthLimit(50);
            options.setAllowDuplicateKeys(false);
            
            org.yaml.snakeyaml.constructor.Constructor constructor = new org.yaml.snakeyaml.constructor.Constructor(LibraryMetadata.class, options);
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(constructor);
            
            LibraryMetadata metadata = yaml.loadAs(yamlContent, LibraryMetadata.class);
            if (metadata == null) {
                logger.warn("Empty or invalid metadata YAML in library: {}", libraryName);
                return null;
            }
            
            logger.debug("Successfully parsed library metadata: {}", metadata);
            return metadata;
            
        } catch (Exception e) {
            logger.warn("Failed to parse metadata YAML from library {}: {}", libraryName, e.getMessage());
            logger.debug("Metadata parsing error details", e);
            return null;
        }
    }
    
    /**
     * Validates if a JAR file contains valid template library structure.
     * 
     * @param jarFile the JAR file to validate
     * @return true if the JAR contains valid template library structure
     */
    public boolean isValidTemplateLibrary(File jarFile) {
        if (!jarFile.exists() || !jarFile.isFile()) {
            return false;
        }
        
        try (JarFile jar = new JarFile(jarFile)) {
            // Check for expected directory structure
            boolean hasTemplates = jar.getEntry(TEMPLATES_BASE_PATH) != null;
            boolean hasCustomizations = jar.getEntry(CUSTOMIZATIONS_BASE_PATH) != null;
            boolean hasMetadata = jar.getEntry(METADATA_PATH) != null;
            
            // At least one should be present
            return hasTemplates || hasCustomizations || hasMetadata;
        } catch (IOException e) {
            logger.warn("Failed to validate JAR as template library: {}", jarFile.getName(), e);
            return false;
        }
    }
    
    /**
     * Extracts templates only (no customizations) from a collection of libraries.
     * This is useful for Phase 1 implementation focusing on templates only.
     * 
     * @param libraries the collection of library JAR files
     * @return map of template paths to template content
     */
    public Map<String, String> extractTemplatesOnly(FileCollection libraries) {
        LibraryExtractionResult result = extractFromLibraries(libraries);
        return result.getTemplates();
    }
}