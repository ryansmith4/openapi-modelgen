package com.guidedbyte.openapi.modelgen.actions;

import com.guidedbyte.openapi.modelgen.TemplateConfiguration;
import com.guidedbyte.openapi.modelgen.constants.PluginConstants;
import com.guidedbyte.openapi.modelgen.services.CustomizationEngine;
import com.guidedbyte.openapi.modelgen.services.LoggingContext;
import com.guidedbyte.openapi.modelgen.util.BuildProgressTracker;
import com.guidedbyte.openapi.modelgen.util.EnhancedErrorContext;
import com.guidedbyte.openapi.modelgen.util.PerformanceMetrics;
import com.guidedbyte.openapi.modelgen.util.PluginLoggerFactory;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * Configuration-cache compatible task action that prepares template working directory
 * using pre-resolved template configuration. This action operates without any project references,
 * making it fully compatible with Gradle's configuration cache.
 * <p>
 * This action also sets the OpenAPI Generator's templateDir property at execution time
 * after creating the working directory, ensuring configuration cache compatibility.
 */
public class TemplatePreparationAction implements Action<Task> {
    private static final Logger logger = PluginLoggerFactory.getLogger(TemplatePreparationAction.class);
    
    private final TemplateConfiguration templateConfig;
    
    public TemplatePreparationAction(TemplateConfiguration templateConfig) {
        this.templateConfig = templateConfig;
    }
    
    @Override
    public void execute(Task task) {
        PerformanceMetrics.Timer timer = PerformanceMetrics.startTimer("template_preparation");
        String generatorName = templateConfig.getGeneratorName();

        // Set logging context for this template preparation
        LoggingContext.setSpec(generatorName);
        LoggingContext.setComponent("TemplatePreparation");

        try {
            if (!templateConfig.isTemplateProcessingEnabled()) {
                logger.debug("Template processing disabled for generator '{}'", generatorName);
                timer.stopAndLog(Map.of("generator", generatorName, "processing_enabled", false));
                return;
            }

            // Report phase transition for build progress tracking
            BuildProgressTracker.ProgressInfo progressInfo = new BuildProgressTracker.ProgressInfo()
                .withElapsed(timer.getElapsed());
            BuildProgressTracker.logPhaseTransition("template_preparation", generatorName, progressInfo);

            prepareTemplateWorkingDirectory();
            setTemplateDirectoryOnTask(task);

            // Log completion with performance metrics
            Map<String, Object> context = Map.of(
                "generator", generatorName,
                "has_customizations", templateConfig.hasAnyCustomizations(),
                "has_user_templates", templateConfig.hasUserTemplates()
            );
            timer.stopAndLog(context);

            logger.info("Template preparation completed successfully for generator '{}'", generatorName);

        } catch (Exception e) {
            // Enhanced error logging with context
            Map<String, Object> errorContext = Map.of(
                "generator", generatorName,
                "template_work_dir", templateConfig.getTemplateWorkDir() != null ? templateConfig.getTemplateWorkDir() : "null",
                "has_customizations", templateConfig.hasAnyCustomizations(),
                "processing_enabled", templateConfig.isTemplateProcessingEnabled()
            );

            EnhancedErrorContext.logBuildError("template_preparation", e, errorContext);

            timer.stopAndLog(Map.of("generator", generatorName, "result", "error"));
            throw new RuntimeException("Template preparation failed for generator '" + generatorName + "'", e);
        } finally {
            LoggingContext.clear();
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
            logger.debug(
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
        logger.debug(
            "Working directory cache invalid, cleaning: {}", templateWorkDir.getAbsolutePath());
        deleteDirectory(templateWorkDir);
        
        if (!templateWorkDir.mkdirs()) {
            throw new IOException("Failed to create template working directory: " + templateWorkDir.getAbsolutePath());
        }
        
        // Add progress indicators for long-running operations
        logger.debug(
            "🔄 Template preparation started (1/4): Analyzing template sources...");

        int totalSteps = 0;
        if (templateConfig.hasUserTemplates()) totalSteps++;
        if (templateConfig.hasUserCustomizations() || templateConfig.hasPluginCustomizations()) totalSteps++;
        totalSteps++; // Cache update step

        int currentStep = 0;

        // Copy user templates if they exist
        if (templateConfig.hasUserTemplates()) {
            currentStep++;
            logger.info("🔄 Template preparation ({}/{}): Copying user templates...", currentStep, totalSteps);
            copyUserTemplates(templateWorkDir);
            logger.debug(
                "✅ User templates copied successfully");
        }

        // Process template customizations if needed
        if (templateConfig.hasUserCustomizations() || templateConfig.hasPluginCustomizations()) {
            currentStep++;
            logger.info("🔄 Template preparation ({}/{}): Processing template customizations...", currentStep, totalSteps);
            processTemplateCustomizations(templateWorkDir);
            logger.debug(
                "✅ Template customizations processed successfully");
        }

        // Update cache marker
        currentStep++;
        logger.info("🔄 Template preparation ({}/{}): Updating cache...", currentStep, totalSteps);
        updateWorkingDirectoryCache(templateWorkDir);
        logger.debug(
            "✅ Template cache updated successfully");
        
        logger.info("Prepared template working directory: {}", templateWorkDir.getAbsolutePath());
    }
    
    private boolean isWorkingDirectoryCacheValid(File templateWorkDir) {
        PerformanceMetrics.Timer timer = PerformanceMetrics.startTimer("working_dir_cache_validation");

        if (!templateWorkDir.exists()) {
            timer.stopAndLog(Map.of("result", "miss", "reason", "dir_not_exists"));
            PerformanceMetrics.logCachePerformance("working-dir-cache", 0, 1, timer.getElapsed());
            return false;
        }

        File cacheFile = new File(templateWorkDir, ".working-dir-cache");
        if (!cacheFile.exists()) {
            timer.stopAndLog(Map.of("result", "miss", "reason", "cache_file_not_exists"));
            PerformanceMetrics.logCachePerformance("working-dir-cache", 0, 1, timer.getElapsed());
            return false;
        }

        try {
            String cachedHash = Files.readString(cacheFile.toPath()).trim();
            String currentHash = computeTemplateConfigurationHash();
            boolean valid = cachedHash.equals(currentHash);

            if (valid) {
                logger.debug("Working directory cache is valid");
                timer.stopAndLog(Map.of("result", "hit"));
                PerformanceMetrics.logCachePerformance("working-dir-cache", 1, 0, timer.getElapsed());
            } else {
                logger.debug(
                    "Working directory cache invalid - hash mismatch (cached: {}, current: {})",
                    cachedHash.substring(0, Math.min(8, cachedHash.length())),
                    currentHash.substring(0, Math.min(8, currentHash.length())));
                timer.stopAndLog(Map.of("result", "miss", "reason", "hash_mismatch"));
                PerformanceMetrics.logCachePerformance("working-dir-cache", 0, 1, timer.getElapsed());
            }

            return valid;
        } catch (IOException e) {
            logger.debug("Failed to read working directory cache: {}", e.getMessage());
            timer.stopAndLog(Map.of("result", "error", "error", e.getClass().getSimpleName()));
            PerformanceMetrics.logCachePerformance("working-dir-cache", 0, 1, timer.getElapsed());
            return false;
        }
    }
    
    private void updateWorkingDirectoryCache(File templateWorkDir) {
        try {
            File cacheFile = new File(templateWorkDir, ".working-dir-cache");
            String hash = computeTemplateConfigurationHash();
            Files.writeString(cacheFile.toPath(), hash);
            logger.debug(
                "Updated working directory cache with hash: {}", hash.substring(0, Math.min(PluginConstants.HASH_DISPLAY_LENGTH, hash.length())));
        } catch (IOException e) {
            logger.debug(
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
        
        logger.debug(
            "Copying user templates from: {}", userTemplateDir.getAbsolutePath());
        copyDirectory(userTemplateDir, templateWorkDir);
    }
    
    private void processTemplateCustomizations(File templateWorkDir) {
        logger.debug(
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
                        var parentPath = targetPath.getParent();
                        if (parentPath == null) {
                            throw new IllegalStateException(
                                String.format("Failed to resolve parent directory for target path '%s' when copying '%s'. " +
                                "Source directory: '%s', Target directory: '%s'", 
                                targetPath, sourcePath, sourceDir, targetDir));
                        }
                        Files.createDirectories(parentPath);
                        Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    logger.warn("Failed to copy template file {}: {}", sourcePath, e.getMessage());
                }
            });
        }
    }
    
    /**
     * Validates that the template directory is ready for OpenAPI Generator.
     * The templateDir property is set at configuration time by TaskConfigurationService.
     */
    private void setTemplateDirectoryOnTask(Task task) {
        String workDir = templateConfig.getTemplateWorkDir();
        if (workDir != null && templateConfig.hasAnyCustomizations()) {
            File templateWorkDir = new File(workDir);
            
            if (templateWorkDir.exists() && templateWorkDir.isDirectory()) {
                logger.info("Template working directory is ready for OpenAPI Generator: {}", templateWorkDir.getAbsolutePath());
                logger.debug(
                    "Template working directory validated: {}", templateWorkDir.getAbsolutePath());
            } else {
                logger.warn("Template working directory does not exist - this may cause generation issues: {}", 
                    templateWorkDir.getAbsolutePath());
            }
        } else {
            logger.debug(
                "No template customizations - OpenAPI Generator will use default templates");
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