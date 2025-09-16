package com.guidedbyte.openapi.modelgen.tasks;

import com.guidedbyte.openapi.modelgen.TemplateConfiguration;
import com.guidedbyte.openapi.modelgen.constants.PluginConstants;
import com.guidedbyte.openapi.modelgen.services.CustomizationEngine;
import com.guidedbyte.openapi.modelgen.services.LoggingContext;
import com.guidedbyte.openapi.modelgen.services.TemplateDiscoveryService;
import com.guidedbyte.openapi.modelgen.util.PluginLoggerFactory;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

/**
 * A cacheable task that prepares the template working directory for OpenAPI Generator.
 * This task is properly annotated for Gradle's incremental builds and caching.
 *
 * <p>The task handles:</p>
 * <ul>
 *   <li>Copying user templates from the user template directory</li>
 *   <li>Processing template customizations (YAML-based modifications)</li>
 *   <li>Extracting original templates when requested</li>
 *   <li>Creating necessary marker files for Gradle validation</li>
 * </ul>
 */
@CacheableTask
public abstract class PrepareTemplateDirectoryTask extends DefaultTask {
    private static final Logger logger = PluginLoggerFactory.getLogger(PrepareTemplateDirectoryTask.class);
    
    /**
     * Provides access to Gradle's file system operations for efficient file copying and deletion.
     *
     * @return the file system operations service
     */
    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    /**
     * The template configuration containing all necessary information for template processing.
     * This is serializable and configuration-cache compatible.
     *
     * @return the template configuration property
     */
    @Nested
    public abstract Property<TemplateConfiguration> getTemplateConfiguration();

    /**
     * The output directory where the prepared templates will be placed.
     * Gradle will manage this directory and ensure it exists when needed.
     *
     * @return the output directory property
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /**
     * The main task action that prepares the template directory for OpenAPI Generator.
     * This method handles the complete template preparation workflow including copying user templates,
     * processing customizations, and extracting original templates when requested.
     */
    @TaskAction
    public void prepareTemplates() {
        TemplateConfiguration templateConfig = getTemplateConfiguration().get();
        File outputDir = getOutputDirectory().get().getAsFile();
        
        // Set up MDC context for task-level logging
        LoggingContext.setSpec(templateConfig.getGeneratorName());
        LoggingContext.setComponent("PrepareTemplateDirectoryTask");
        
        try {
            logger.info("Preparing template directory: {}", outputDir.getAbsolutePath());
            // Clean and recreate the output directory using FileSystemOperations
            getFileSystemOperations().delete(deleteSpec -> {
                deleteSpec.delete(outputDir);
            });
            
            if (!outputDir.mkdirs()) {
                throw new RuntimeException("Failed to create template working directory: " + outputDir.getAbsolutePath());
            }
            
            // Create marker file for Gradle input validation
            try {
                File markerFile = new File(outputDir, PluginConstants.TEMPLATE_MARKER_FILE);
                if (!markerFile.exists() && !markerFile.createNewFile()) {
                    logger.warn("Failed to create template directory marker file: {}", markerFile.getAbsolutePath());
                }
            } catch (IOException e) {
                logger.warn("Failed to create template directory marker file: {}", e.getMessage());
            }
            
            // If no customizations, create empty directory and return
            if (!templateConfig.hasAnyCustomizations()) {
                logger.debug(
                    "No template customizations found for generator '{}'. Created empty template directory.",
                    templateConfig.getGeneratorName());
                return;
            }
            
            // Copy user templates if they exist
            if (templateConfig.hasUserTemplates()) {
                copyUserTemplates(outputDir, templateConfig);
            }
            
            // Process template customizations if needed
            if (templateConfig.hasUserCustomizations() || templateConfig.hasPluginCustomizations()) {
                processTemplateCustomizations(outputDir, templateConfig);
            }
            
            // Extract original templates if requested
            if (templateConfig.isSaveOriginalTemplates()) {
                extractOriginalTemplates(outputDir, templateConfig);
            }
            
            logger.info("Successfully prepared template directory: {}", outputDir.getAbsolutePath());
            
        } catch (RuntimeException e) {
            logger.error("Failed to prepare template directory '{}': {}", outputDir.getAbsolutePath(), e.getMessage());
            throw new TaskExecutionException(this, e);
        } finally {
            LoggingContext.clear();
        }
    }
    
    private void copyUserTemplates(File outputDir, TemplateConfiguration templateConfig) {
        if (templateConfig.getUserTemplateDirectory() == null) {
            return;
        }
        
        // Try generator-specific subdirectory first
        File generatorTemplateDir = new File(templateConfig.getUserTemplateDirectory(), templateConfig.getGeneratorName());
        if (generatorTemplateDir.exists() && generatorTemplateDir.isDirectory()) {
            logger.debug(
                "Copying user templates from generator directory: {}", generatorTemplateDir.getAbsolutePath());

            getFileSystemOperations().sync(syncSpec -> {
                syncSpec.from(generatorTemplateDir);
                syncSpec.into(outputDir);
            });
            return;
        }
        
        // Fall back to root template directory
        File rootTemplateDir = new File(templateConfig.getUserTemplateDirectory());
        if (rootTemplateDir.exists() && rootTemplateDir.isDirectory()) {
            // Only copy .mustache files from root to avoid copying unrelated files
            logger.debug(
                "Copying user templates from root directory: {}", rootTemplateDir.getAbsolutePath());

            getFileSystemOperations().sync(syncSpec -> {
                syncSpec.from(rootTemplateDir);
                syncSpec.into(outputDir);
                syncSpec.include("*.mustache");
            });
        }
    }
    
    private void processTemplateCustomizations(File outputDir, TemplateConfiguration templateConfig) {
        logger.debug(
            "Processing template customizations for generator: {}", templateConfig.getGeneratorName());

        CustomizationEngine customizationEngine = new CustomizationEngine();
        customizationEngine.processTemplateCustomizations(templateConfig, outputDir);
    }
    
    private void extractOriginalTemplates(File outputDir, TemplateConfiguration templateConfig) {
        logger.debug(
            "Extracting original templates to orig/ directory");

        File origDir = new File(outputDir, PluginConstants.ORIG_DIR_NAME);
        TemplateDiscoveryService discoveryService = new TemplateDiscoveryService();
        int extractedCount = discoveryService.extractAllTemplates(templateConfig.getGeneratorName(), origDir);
        
        logger.info("Extracted {} original templates to: {}", extractedCount, origDir.getAbsolutePath());
    }
    
}