package com.guidedbyte.openapi.modelgen.services;

import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration-cache compatible service for managing template caching operations.
 * Handles working directory caches, global cache persistence, and template hash validation.
 */
public class TemplateCacheManager implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(TemplateCacheManager.class);
    
    // Thread-safe cache for extracted templates
    private final Map<String, String> extractedTemplatesCache = new ConcurrentHashMap<>();
    
    /**
     * Computes cache key for selective template extraction.
     *
     * @param generatorName the OpenAPI generator name
     * @param pluginVersion the plugin version
     * @return computed cache key
     */
    public String computeSelectiveTemplateCacheKey(String generatorName, String pluginVersion) {
        return String.format("%s-%s-selective", generatorName, pluginVersion);
    }
    
    /**
     * Checks if template cache is valid by comparing cache key.
     *
     * @param cacheFile the cache metadata file
     * @param expectedCacheKey the expected cache key
     * @param targetDir the target directory (unused but kept for compatibility)
     * @return true if cache is valid
     */
    public boolean isTemplateCacheValid(File cacheFile, String expectedCacheKey, File targetDir) {
        if (!cacheFile.exists()) {
            logger.debug("Template cache file does not exist: {}", cacheFile.getAbsolutePath());
            return false;
        }
        
        try {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(cacheFile)) {
                props.load(fis);
            }
            
            String actualCacheKey = props.getProperty("cacheKey");
            boolean isValid = expectedCacheKey.equals(actualCacheKey);
            
            if (isValid) {
                logger.debug("Template cache is valid: {}", cacheFile.getAbsolutePath());
            } else {
                logger.debug("Template cache key mismatch. Expected: {}, Actual: {}", expectedCacheKey, actualCacheKey);
            }
            
            return isValid;
            
        } catch (IOException e) {
            logger.debug("Error reading template cache file: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Calculates SHA-256 hash of a file's content.
     *
     * @param filePath path to the file
     * @return SHA-256 hash string
     * @throws IOException if file cannot be read
     */
    public String calculateFileContentHash(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (NoSuchAlgorithmException e) {
            // Use ErrorHandlingUtils for consistent error handling
            String message = "SHA-256 algorithm not available. This indicates a serious JVM configuration issue.";
            String guidance = "Verify Java installation and runtime environment. Contact system administrator if issue persists.";
            throw new RuntimeException(message + " " + guidance, e);
        }
    }
    
    /**
     * Stores template hashes for cache validation.
     *
     * @param targetDir directory containing templates
     * @param templateHashes map of template names to hashes
     * @throws IOException if hash file cannot be written
     */
    public void storeTemplateHashes(File targetDir, Map<String, String> templateHashes) throws IOException {
        // Use ErrorHandlingUtils for validation
        try {
            ErrorHandlingUtils.validateNotNull(targetDir, "Target directory", 
                ErrorHandlingUtils.FILE_NOT_FOUND_GUIDANCE, logger);
            ErrorHandlingUtils.validateNotNull(templateHashes, "Template hashes map", 
                "Ensure template extraction completed successfully", logger);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
        
        File hashFile = new File(targetDir, "template-hashes.properties");
        Properties hashProps = new Properties();
        
        for (Map.Entry<String, String> entry : templateHashes.entrySet()) {
            hashProps.setProperty("hash." + entry.getKey(), entry.getValue());
        }
        
        // Use ErrorHandlingUtils for file operations
        ErrorHandlingUtils.handleFileOperation(
            () -> {
                try (FileOutputStream fos = new FileOutputStream(hashFile)) {
                    hashProps.store(fos, "Template content hashes for cache validation");
                    return null;
                }
            },
            "Failed to store template hashes in: " + hashFile.getAbsolutePath(),
            ErrorHandlingUtils.PERMISSION_GUIDANCE,
            logger
        );
        
        logger.debug("Stored {} template hashes in {}", templateHashes.size(), hashFile.getAbsolutePath());
    }
    
    /**
     * Validates template cache using stored content hashes.
     *
     * @param cacheFile the cache metadata file
     * @param expectedCacheKey the expected cache key
     * @param targetDir the target directory containing templates and hashes
     * @return true if cache is valid
     */
    public boolean isTemplateCacheValidWithHashes(File cacheFile, String expectedCacheKey, File targetDir) {
        // First check basic cache key validity
        if (!isTemplateCacheValid(cacheFile, expectedCacheKey, targetDir)) {
            return false;
        }
        
        // Then validate template content hashes
        File hashFile = new File(targetDir, "template-hashes.properties");
        if (!hashFile.exists()) {
            logger.debug("Template hash file does not exist: {}", hashFile.getAbsolutePath());
            return false;
        }
        
        try {
            Properties hashProps = new Properties();
            try (FileInputStream fis = new FileInputStream(hashFile)) {
                hashProps.load(fis);
            }
            
            // Verify each stored template hash
            for (String propertyName : hashProps.stringPropertyNames()) {
                if (propertyName.startsWith("hash.")) {
                    String templateName = propertyName.substring(5); // Remove "hash." prefix
                    String expectedHash = hashProps.getProperty(propertyName);
                    
                    File templateFile = new File(targetDir, templateName);
                    if (!templateFile.exists()) {
                        logger.debug("Template file missing: {}", templateFile.getAbsolutePath());
                        return false;
                    }
                    
                    String actualHash = calculateFileContentHash(templateFile.toPath());
                    if (!expectedHash.equals(actualHash)) {
                        logger.debug("Template hash mismatch for {}: expected {}, actual {}", 
                                   templateName, expectedHash, actualHash);
                        return false;
                    }
                }
            }
            
            logger.debug("Template cache validation with hashes successful");
            return true;
            
        } catch (IOException e) {
            logger.debug("Error validating template hashes: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Updates template cache metadata.
     *
     * @param cacheFile the cache file to update
     * @param cacheKey the cache key to store
     * @throws IOException if cache file cannot be written
     */
    public void updateTemplateCache(File cacheFile, String cacheKey) throws IOException {
        // Use ErrorHandlingUtils for validation
        try {
            ErrorHandlingUtils.validateNotNull(cacheFile, "Cache file", 
                "Provide a valid cache file path", logger);
            ErrorHandlingUtils.validateNotEmpty(cacheKey, "Cache key", 
                "Ensure cache key is properly computed", logger);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
        
        Properties props = new Properties();
        props.setProperty("cacheKey", cacheKey);
        props.setProperty("timestamp", String.valueOf(System.currentTimeMillis()));
        
        // Ensure parent directory exists with improved error handling
        File parentDir = cacheFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            ErrorHandlingUtils.handleFileOperation(
                () -> {
                    if (!parentDir.mkdirs()) {
                        throw new IOException("Directory creation failed");
                    }
                    return null;
                },
                "Failed to create cache directory: " + parentDir.getAbsolutePath(),
                ErrorHandlingUtils.PERMISSION_GUIDANCE,
                logger
            );
        }
        
        // Use ErrorHandlingUtils for file operations
        ErrorHandlingUtils.handleFileOperation(
            () -> {
                try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                    props.store(fos, "Template cache metadata");
                    return null;
                }
            },
            "Failed to update template cache: " + cacheFile.getAbsolutePath(),
            ErrorHandlingUtils.PERMISSION_GUIDANCE,
            logger
        );
        
        logger.debug("Updated template cache: {}", cacheFile.getAbsolutePath());
    }
    
    /**
     * Gets the global cache directory for cross-build persistence.
     *
     * @param project the Gradle project
     * @return global cache directory
     */
    public File getGlobalCacheDir(Project project) {
        return new File(project.getGradle().getGradleUserHomeDir(), "caches/openapi-modelgen");
    }
    
    /**
     * Computes SHA-256 hash of a string.
     *
     * @param content the string content to hash
     * @return SHA-256 hash string
     */
    public String computeStringHash(String content) {
        if (content == null) {
            return "null";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes("UTF-8"));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (Exception e) {
            logger.debug("Error computing string hash: {}", e.getMessage());
            return "error-" + System.currentTimeMillis();
        }
    }
    
    /**
     * Computes cache key for working directory.
     *
     * @param project the Gradle project
     * @param generatorName the generator name
     * @param pluginVersion the plugin version
     * @param pluginCustomizationHash hash of plugin customizations
     * @param userTemplateHash hash of user templates
     * @param userCustomizationHash hash of user customizations
     * @return computed working directory cache key
     */
    public String computeWorkingDirCacheKey(Project project,
                                          String generatorName, 
                                          String pluginVersion,
                                          String pluginCustomizationHash,
                                          String userTemplateHash, 
                                          String userCustomizationHash) {
        
        // Detect OpenAPI Generator version for cache key
        String openapiVersion = detectOpenApiGeneratorVersion(project);
        
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("generator:").append(generatorName).append(";");
        keyBuilder.append("plugin:").append(pluginVersion).append(";");
        keyBuilder.append("openapi:").append(openapiVersion).append(";");
        keyBuilder.append("pluginCustomizations:").append(pluginCustomizationHash).append(";");
        keyBuilder.append("userTemplates:").append(userTemplateHash).append(";");
        keyBuilder.append("userCustomizations:").append(userCustomizationHash);
        
        String fullKey = keyBuilder.toString();
        return computeStringHash(fullKey);
    }
    
    /**
     * Checks if working directory cache is valid.
     *
     * @param workingDir the working directory
     * @param expectedCacheKey the expected cache key
     * @return true if cache is valid
     */
    public boolean isWorkingDirectoryCacheValid(File workingDir, String expectedCacheKey) {
        if (!workingDir.exists() || !workingDir.isDirectory()) {
            logger.debug("Working directory does not exist: {}", workingDir.getAbsolutePath());
            return false;
        }
        
        File cacheFile = new File(workingDir, ".working-dir-cache");
        if (!cacheFile.exists()) {
            logger.debug("Working directory cache file does not exist: {}", cacheFile.getAbsolutePath());
            return false;
        }
        
        try {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(cacheFile)) {
                props.load(fis);
            }
            
            String actualCacheKey = props.getProperty("cacheKey");
            boolean isValid = expectedCacheKey.equals(actualCacheKey);
            
            if (isValid) {
                logger.debug("Working directory cache is valid: {}", workingDir.getAbsolutePath());
            } else {
                logger.debug("Working directory cache key mismatch. Expected: {}, Actual: {}", expectedCacheKey, actualCacheKey);
            }
            
            return isValid;
            
        } catch (IOException e) {
            logger.debug("Error reading working directory cache: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Updates working directory cache.
     *
     * @param workingDir the working directory
     * @param cacheKey the cache key to store
     */
    public void updateWorkingDirectoryCache(File workingDir, String cacheKey) {
        File cacheFile = new File(workingDir, ".working-dir-cache");
        
        try {
            Properties props = new Properties();
            props.setProperty("cacheKey", cacheKey);
            props.setProperty("timestamp", String.valueOf(System.currentTimeMillis()));
            
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                props.store(fos, "Working directory cache metadata");
            }
            
            logger.debug("Updated working directory cache: {}", workingDir.getAbsolutePath());
            
        } catch (IOException e) {
            logger.debug("Error updating working directory cache: {}", e.getMessage());
        }
    }
    
    /**
     * Computes hash of a directory's contents.
     *
     * @param directory the directory to hash
     * @return SHA-256 hash of directory contents
     */
    public String hashDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return "empty-directory";
        }
        
        try {
            List<String> fileHashes = new ArrayList<>();
            hashDirectoryRecursive(directory.toPath(), fileHashes);
            
            // Sort for consistent ordering
            fileHashes.sort(String::compareTo);
            
            // Combine all file hashes
            StringBuilder combined = new StringBuilder();
            for (String hash : fileHashes) {
                combined.append(hash);
            }
            
            return computeStringHash(combined.toString());
            
        } catch (Exception e) {
            logger.debug("Error hashing directory {}: {}", directory.getAbsolutePath(), e.getMessage());
            return "error-" + System.currentTimeMillis();
        }
    }
    
    /**
     * Recursively hashes directory contents.
     */
    private void hashDirectoryRecursive(Path directory, List<String> fileHashes) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    hashDirectoryRecursive(entry, fileHashes);
                } else if (Files.isRegularFile(entry)) {
                    String relativePathHash = computeStringHash(entry.toString());
                    String contentHash = calculateFileContentHash(entry);
                    fileHashes.add(relativePathHash + ":" + contentHash);
                }
            }
        }
    }
    
    /**
     * Loads global template hash cache for cross-build persistence.
     *
     * @param project the Gradle project
     * @return map of cache keys to hashes
     */
    public Map<String, String> loadGlobalTemplateHashCache(Project project) {
        Map<String, String> globalHashes = new ConcurrentHashMap<>();
        
        try {
            File globalCacheDir = getGlobalCacheDir(project);
            File hashCacheFile = new File(globalCacheDir, "template-hashes.properties");
            
            if (hashCacheFile.exists()) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(hashCacheFile)) {
                    props.load(fis);
                }
                
                for (String key : props.stringPropertyNames()) {
                    globalHashes.put(key, props.getProperty(key));
                }
                
                logger.debug("Loaded {} global template hashes from cache", globalHashes.size());
            }
            
        } catch (IOException e) {
            logger.debug("Error loading global template hash cache: {}", e.getMessage());
        }
        
        return globalHashes;
    }
    
    /**
     * Saves global template hash cache for cross-build persistence.
     *
     * @param project the Gradle project
     * @param globalHashes map of cache keys to hashes to save
     */
    public void saveGlobalTemplateHashCache(Project project, Map<String, String> globalHashes) {
        try {
            File globalCacheDir = getGlobalCacheDir(project);
            if (!globalCacheDir.exists() && !globalCacheDir.mkdirs()) {
                logger.debug("Failed to create global cache directory: {}", globalCacheDir.getAbsolutePath());
                return;
            }
            
            File hashCacheFile = new File(globalCacheDir, "template-hashes.properties");
            Properties props = new Properties();
            
            for (Map.Entry<String, String> entry : globalHashes.entrySet()) {
                props.setProperty(entry.getKey(), entry.getValue());
            }
            
            try (FileOutputStream fos = new FileOutputStream(hashCacheFile)) {
                props.store(fos, "Global template hash cache for cross-build persistence");
            }
            
            logger.debug("Saved {} global template hashes to cache", globalHashes.size());
            
        } catch (IOException e) {
            logger.debug("Error saving global template hash cache: {}", e.getMessage());
        }
    }
    
    /**
     * Checks working directory cache validity with global hash persistence.
     *
     * @param project the Gradle project
     * @param workingDir the working directory
     * @param expectedCacheKey the expected cache key
     * @return true if cache is valid
     */
    public boolean isWorkingDirectoryCacheValidWithGlobalHashes(Project project, File workingDir, String expectedCacheKey) {
        // First check local working directory cache
        if (!isWorkingDirectoryCacheValid(workingDir, expectedCacheKey)) {
            return false;
        }
        
        // Then check global hash cache
        Map<String, String> globalHashes = loadGlobalTemplateHashCache(project);
        String cachedGlobalHash = globalHashes.get(expectedCacheKey);
        
        if (cachedGlobalHash != null) {
            // Verify current directory matches cached global hash
            String currentHash = hashDirectory(workingDir);
            boolean isValid = cachedGlobalHash.equals(currentHash);
            
            if (isValid) {
                logger.debug("Working directory cache validated with global hashes");
            } else {
                logger.debug("Working directory hash mismatch with global cache");
            }
            
            return isValid;
        }
        
        logger.debug("No global hash found for cache key, assuming valid");
        return true;
    }
    
    /**
     * Updates working directory cache with global persistence.
     *
     * @param project the Gradle project
     * @param workingDir the working directory
     * @param cacheKey the cache key
     */
    public void updateWorkingDirectoryCacheWithGlobalPersistence(Project project, File workingDir, String cacheKey) {
        // Update local working directory cache
        updateWorkingDirectoryCache(workingDir, cacheKey);
        
        // Update global hash cache
        Map<String, String> globalHashes = loadGlobalTemplateHashCache(project);
        String directoryHash = hashDirectory(workingDir);
        globalHashes.put(cacheKey, directoryHash);
        saveGlobalTemplateHashCache(project, globalHashes);
        
        logger.debug("Updated working directory cache with global persistence");
    }
    
    /**
     * Gets the extracted templates cache for session-level caching.
     *
     * @return the extracted templates cache map
     */
    public Map<String, String> getExtractedTemplatesCache() {
        return extractedTemplatesCache;
    }
    
    /**
     * Detects OpenAPI Generator version from project using comprehensive detection strategies.
     */
    private String detectOpenApiGeneratorVersion(Project project) {
        try {
            OpenApiGeneratorVersionDetector detector = new OpenApiGeneratorVersionDetector();
            return detector.detectVersionOrFail(project);
        } catch (Exception e) {
            logger.warn("Failed to detect OpenAPI Generator version: {}", e.getMessage());
            throw e; // Re-throw to maintain fail-fast behavior
        }
    }
}