package com.guidedbyte.openapi.modelgen.actions;

import com.guidedbyte.openapi.modelgen.TemplateConfiguration;
import com.guidedbyte.openapi.modelgen.services.CustomizationEngine;
import com.guidedbyte.openapi.modelgen.util.DebugLogger;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Configuration-cache compatible task action that prepares template working directory
 * using pre-resolved template configuration. This action operates without any project references,
 * making it fully compatible with Gradle's configuration cache.
 * 
 * This action also sets the OpenAPI Generator's templateDir property at execution time
 * after creating the working directory, ensuring configuration cache compatibility.
 */
public class TemplatePreparationAction implements Action<Task> {
    private static final Logger logger = LoggerFactory.getLogger(TemplatePreparationAction.class);
    
    private final TemplateConfiguration templateConfig;
    
    public TemplatePreparationAction(TemplateConfiguration templateConfig) {
        this.templateConfig = templateConfig;
    }
    
    @Override
    public void execute(Task task) {
        if (!templateConfig.isTemplateProcessingEnabled()) {
            DebugLogger.debug(logger, templateConfig.isDebug(), 
                "Template processing disabled for generator '{}'", templateConfig.getGeneratorName());
            return;
        }
        
        try {
            prepareTemplateWorkingDirectory();
        } catch (Exception e) {
            logger.error("Failed to prepare template working directory for generator '{}': {}", 
                templateConfig.getGeneratorName(), e.getMessage());
            DebugLogger.debug(logger, templateConfig.isDebug(), 
                "Template preparation error details: {}", e.getMessage());
            throw new RuntimeException("Template preparation failed", e);
        }
    }
    
    private void prepareTemplateWorkingDirectory() throws IOException {
        String workDir = templateConfig.getTemplateWorkDir();
        if (workDir == null) {
            return;
        }
        
        File templateWorkDir = new File(workDir);
        
        // Always ensure the directory exists for Gradle validation, even if we have no customizations
        if (!templateWorkDir.exists()) {
            if (!templateWorkDir.mkdirs()) {
                throw new IOException("Failed to create template working directory: " + templateWorkDir.getAbsolutePath());
            }
        }
        
        // If no customizations, just create empty directory and return
        if (!templateConfig.hasAnyCustomizations()) {
            DebugLogger.debug(logger, templateConfig.isDebug(), 
                "No template customizations found for generator '{}'. Created empty template directory for Gradle validation.", 
                templateConfig.getGeneratorName());
            return;
        }
        
        // Check if working directory cache is valid
        if (isWorkingDirectoryCacheValid(templateWorkDir)) {
            logger.info("Using cached template working directory: {}", templateWorkDir.getAbsolutePath());
            return;
        }
        
        // Clean and recreate working directory for customization processing
        DebugLogger.debug(logger, templateConfig.isDebug(), 
            "Working directory cache invalid, cleaning: {}", templateWorkDir.getAbsolutePath());
        deleteDirectory(templateWorkDir);
        
        if (!templateWorkDir.mkdirs()) {
            throw new IOException("Failed to create template working directory: " + templateWorkDir.getAbsolutePath());
        }
        
        // Copy user templates if they exist
        if (templateConfig.hasUserTemplates()) {
            copyUserTemplates(templateWorkDir);
        }
        
        // Process template customizations if needed
        if (templateConfig.hasUserCustomizations() || templateConfig.hasPluginCustomizations()) {
            processTemplateCustomizations(templateWorkDir);
        }
        
        // Update cache marker
        updateWorkingDirectoryCache(templateWorkDir);
        
        logger.info("Prepared template working directory: {}", templateWorkDir.getAbsolutePath());
    }
    
    private boolean isWorkingDirectoryCacheValid(File templateWorkDir) {
        if (!templateWorkDir.exists()) {
            return false;
        }
        
        File cacheFile = new File(templateWorkDir, ".working-dir-cache");
        if (!cacheFile.exists()) {
            return false;
        }
        
        try {
            String cachedHash = Files.readString(cacheFile.toPath()).trim();
            String currentHash = computeTemplateConfigurationHash();
            boolean valid = cachedHash.equals(currentHash);
            
            if (!valid) {
                DebugLogger.debug(logger, templateConfig.isDebug(), 
                    "Working directory cache invalid - hash mismatch (cached: {}, current: {})", 
                    cachedHash.substring(0, Math.min(8, cachedHash.length())), 
                    currentHash.substring(0, Math.min(8, currentHash.length())));
            }
            
            return valid;
        } catch (IOException e) {
            DebugLogger.debug(logger, templateConfig.isDebug(), 
                "Failed to read working directory cache: {}", e.getMessage());
            return false;
        }
    }
    
    private void updateWorkingDirectoryCache(File templateWorkDir) {
        try {
            File cacheFile = new File(templateWorkDir, ".working-dir-cache");
            String hash = computeTemplateConfigurationHash();
            Files.writeString(cacheFile.toPath(), hash);
            DebugLogger.debug(logger, templateConfig.isDebug(), 
                "Updated working directory cache with hash: {}", hash.substring(0, Math.min(16, hash.length())));
        } catch (IOException e) {
            DebugLogger.debug(logger, templateConfig.isDebug(), 
                "Failed to update working directory cache: {}", e.getMessage());
        }
    }
    
    private String computeTemplateConfigurationHash() {
        // Create a hash based on template configuration that captures all relevant state
        return String.valueOf(templateConfig.hashCode());
    }
    
    private void copyUserTemplates(File templateWorkDir) throws IOException {
        if (templateConfig.getUserTemplateDirectory() == null) {
            return;
        }
        
        File userTemplateDir = new File(templateConfig.getUserTemplateDirectory(), templateConfig.getGeneratorName());
        if (!userTemplateDir.exists()) {
            return;
        }
        
        DebugLogger.debug(logger, templateConfig.isDebug(), 
            "Copying user templates from: {}", userTemplateDir.getAbsolutePath());
        copyDirectory(userTemplateDir, templateWorkDir);
    }
    
    private void processTemplateCustomizations(File templateWorkDir) {
        DebugLogger.debug(logger, templateConfig.isDebug(), 
            "Processing template customizations for generator: {}", templateConfig.getGeneratorName());
        
        CustomizationEngine customizationEngine = new CustomizationEngine();
        customizationEngine.processTemplateCustomizations(templateConfig, templateWorkDir);
    }
    
    private void copyDirectory(File sourceDir, File targetDir) throws IOException {
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            return;
        }
        
        try (var pathStream = Files.walk(sourceDir.toPath())) {
            pathStream.forEach(sourcePath -> {
                try {
                    var targetPath = targetDir.toPath().resolve(sourceDir.toPath().relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    logger.warn("Failed to copy template file {}: {}", sourcePath, e.getMessage());
                }
            });
        }
    }
    
    private void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        
        try (var pathStream = Files.walk(directory.toPath())) {
            pathStream
                .map(java.nio.file.Path::toFile)
                .sorted((f1, f2) -> f2.getPath().compareTo(f1.getPath())) // Delete files before directories
                .forEach(file -> {
                    if (!file.delete()) {
                        logger.warn("Failed to delete: {}", file.getAbsolutePath());
                    }
                });
        }
    }
}