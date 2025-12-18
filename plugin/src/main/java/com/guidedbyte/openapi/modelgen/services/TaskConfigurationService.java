package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.DefaultConfig;
import com.guidedbyte.openapi.modelgen.OpenApiModelGenExtension;
import com.guidedbyte.openapi.modelgen.ResolvedSpecConfig;
import com.guidedbyte.openapi.modelgen.SpecConfig;
import com.guidedbyte.openapi.modelgen.TemplateConfiguration;
import com.guidedbyte.openapi.modelgen.actions.ParallelExecutionLoggingAction;
import com.guidedbyte.openapi.modelgen.actions.TemplateDirectorySetupAction;
import com.guidedbyte.openapi.modelgen.constants.PluginConstants;
import com.guidedbyte.openapi.modelgen.util.PluginLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask;
import org.slf4j.Logger;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration-cache compatible service for creating and configuring Gradle tasks.
 * 
 * <p>This service handles the complete lifecycle of Gradle task management for OpenAPI model generation:</p>
 * <ul>
 *   <li><strong>Task Creation:</strong> Creates setup, spec-specific, aggregate, and help tasks</li>
 *   <li><strong>Task Configuration:</strong> Applies OpenAPI Generator settings, Lombok annotations, and validation rules</li>
 *   <li><strong>Mapping Configuration:</strong> Applies import mappings, type mappings, and additional properties</li>
 *   <li><strong>Dependency Management:</strong> Establishes proper task dependencies and execution order</li>
 *   <li><strong>Incremental Builds:</strong> Configures inputs and outputs for efficient change detection</li>
 *   <li><strong>Template Variables:</strong> Expands nested template variables with built-in values</li>
 * </ul>
 * 
 * <p>The service is stateless and configuration-cache compatible, using only serializable state
 * and avoiding Project dependencies in task actions.</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.0.0
 */
public class TaskConfigurationService implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger logger = PluginLoggerFactory.getLogger(TaskConfigurationService.class);
    
    /**
     * Creates a new TaskConfigurationService.
     */
    public TaskConfigurationService() {
    }
    
    /**
     * Creates all tasks for the configured OpenAPI specifications.
     *
     * @param project the Gradle project
     * @param extension the plugin extension containing configuration
     */
    public void createTasksForSpecs(Project project, OpenApiModelGenExtension extension) {
        TaskContainer tasks = project.getTasks();
        ProjectLayout projectLayout = project.getLayout();
        ObjectFactory objectFactory = project.getObjects();
        ProviderFactory providerFactory = project.getProviders();

        // Create individual tasks for each spec
        extension.getSpecs().forEach((specName, specConfig) -> {
            String taskName = PluginConstants.TASK_PREFIX + capitalize(specName);
            String prepareTaskName = "prepareTemplateDirectory" + capitalize(specName);
            
            // Create resolved config to determine if we need template preparation
            ResolvedSpecConfig resolvedConfig = ResolvedSpecConfig.builder(specName, extension, specConfig).build();
            TemplateConfiguration templateConfig = resolveTemplateConfiguration(resolvedConfig, projectLayout);
            
            // Create template preparation task if needed
            TaskProvider<com.guidedbyte.openapi.modelgen.tasks.PrepareTemplateDirectoryTask> prepareTask = null;
            if (templateConfig.hasAnyCustomizations()) {
                prepareTask = tasks.register(prepareTaskName, com.guidedbyte.openapi.modelgen.tasks.PrepareTemplateDirectoryTask.class, task -> {
                    task.setDescription("Prepares template directory for " + specName + " generation");
                    task.setGroup(PluginConstants.TASK_GROUP);
                    task.getTemplateConfiguration().set(templateConfig);
                    task.getOutputDirectory().set(projectLayout.getBuildDirectory().dir("template-work/" + templateConfig.getGeneratorName() + "-" + specName));
                });
            }
            
            // Create the main generation task
            final TaskProvider<com.guidedbyte.openapi.modelgen.tasks.PrepareTemplateDirectoryTask> finalPrepareTask = prepareTask;
            tasks.register(taskName, GenerateTask.class, task -> {
                // Configure the task with the prepare task provider
                configureGenerateTask(task, extension, specConfig, specName, project, projectLayout, objectFactory, providerFactory, finalPrepareTask);

                // Configure incremental build support
                configureIncrementalBuild(task, projectLayout, extension, specConfig);
            });
        });
        
        // Create setup template directories task
        tasks.register(PluginConstants.TASK_SETUP_DIRS, task -> {
            task.setDescription(PluginConstants.DESC_SETUP_DIRS);
            task.setGroup(PluginConstants.TASK_GROUP);
            task.doLast(new TemplateDirectorySetupAction(extension.getSpecs(), extension.getDefaults(), projectLayout));
        });
        
        // Create aggregate task that runs all spec tasks
        if (!extension.getSpecs().isEmpty()) {
            tasks.register(PluginConstants.TASK_ALL_MODELS, task -> {
                task.setDescription(PluginConstants.DESC_ALL_MODELS);
                task.setGroup(PluginConstants.TASK_GROUP);
                
                // Configure parallel execution based on user preference
                configureParallelExecution(task, extension, tasks);
                
                // Make this task depend on all spec tasks
                extension.getSpecs().keySet().forEach(specName -> {
                    String specTaskName = PluginConstants.TASK_PREFIX + capitalize(specName);
                    task.dependsOn(specTaskName);
                });
            });
        }
        
        // Create clean task for removing generated code and caches
        createCleanTask(tasks, extension, projectLayout, project);
        
        // Create help task with usage information
        createHelpTask(tasks, extension);

        // Create debug configuration task for troubleshooting
        createDebugConfigTask(tasks, extension, project, projectLayout);

        logger.debug("Created tasks for {} OpenAPI specifications", extension.getSpecs().size());
    }
    
    /**
     * Resolves template configuration for a specification.
     * 
     * @param extension the plugin extension containing configuration
     * @param specConfig the specification configuration  
     * @param specName the name of the specification
     * @param project the Gradle project
     * @param projectLayout the project layout for path resolution
     * @return resolved template configuration
     */
    private TemplateConfiguration resolveTemplateConfiguration(OpenApiModelGenExtension extension, 
                                                              SpecConfig specConfig, String specName, 
                                                              Project project, ProjectLayout projectLayout) {
        // Create resolved config to get generator name and template variables
        ResolvedSpecConfig resolvedConfig = ResolvedSpecConfig.builder(specName, extension, specConfig).build();
        
        // Use TemplateResolver to create proper template configuration
        TemplateResolver templateResolver = new TemplateResolver();
        return templateResolver.resolveTemplateConfiguration(
            projectLayout,
            resolvedConfig,
            resolvedConfig.getGeneratorName(),
            resolvedConfig.getTemplateVariables()
        );
    }

    /**
     * Resolves template configuration using an already-created ResolvedSpecConfig.
     * This avoids duplicate resolution when the ResolvedSpecConfig is already available.
     *
     * @param resolvedConfig the already-resolved spec configuration
     * @param projectLayout the project layout for path resolution
     * @return resolved template configuration
     */
    private TemplateConfiguration resolveTemplateConfiguration(ResolvedSpecConfig resolvedConfig, ProjectLayout projectLayout) {
        // Use TemplateResolver to create proper template configuration
        TemplateResolver templateResolver = new TemplateResolver();
        return templateResolver.resolveTemplateConfiguration(
            projectLayout,
            resolvedConfig,
            resolvedConfig.getGeneratorName(),
            resolvedConfig.getTemplateVariables()
        );
    }

    /**
     * Configures a GenerateTask for a specific OpenAPI specification.
     * 
     * @param task the GenerateTask to configure
     * @param extension the plugin extension containing configuration
     * @param specConfig the specification configuration
     * @param specName the name of the specification
     * @param project the Gradle project
     * @param projectLayout the project layout for path resolution
     * @param objectFactory the object factory for creating Gradle objects
     * @param providerFactory the provider factory for creating lazy properties
     */
    public void configureGenerateTask(GenerateTask task, OpenApiModelGenExtension extension, 
                                    SpecConfig specConfig, String specName, Project project, 
                                    ProjectLayout projectLayout, ObjectFactory objectFactory, 
                                    ProviderFactory providerFactory,
                                    TaskProvider<com.guidedbyte.openapi.modelgen.tasks.PrepareTemplateDirectoryTask> prepareTask) {
        
        // Set basic task properties
        task.setDescription(PluginConstants.DESC_GENERATE_PREFIX + specName + PluginConstants.DESC_GENERATE_SUFFIX);
        task.setGroup(PluginConstants.TASK_GROUP);
        
        // Resolve template configuration first to determine if templateDir is needed
        TemplateConfiguration templateConfiguration = resolveTemplateConfiguration(extension, specConfig, specName, project, projectLayout);
        
        // Apply spec configuration with template configuration context and prepare task
        applySpecConfig(task, extension, specConfig, specName, project, projectLayout, objectFactory, providerFactory, templateConfiguration, prepareTask);
        
        // Template preparation is now handled by the dedicated PrepareTemplateDirectoryTask
        // No need for doFirst action - Gradle handles task dependencies automatically via Provider

        // Debug logging is now handled automatically by PluginLogger based on Gradle log level

        logger.debug("Configured generate task for spec: {}", specName);
    }
    
    /**
     * Applies specification configuration to a GenerateTask.
     * 
     * @param task the GenerateTask to configure
     * @param extension the plugin extension containing configuration
     * @param specConfig the specification configuration
     * @param specName the name of the specification
     * @param project the Gradle project
     * @param projectLayout the project layout for path resolution
     * @param objectFactory the object factory for creating Gradle objects
     * @param providerFactory the provider factory for creating lazy properties
     * @param templateConfiguration the resolved template configuration
     */
    public void applySpecConfig(GenerateTask task, OpenApiModelGenExtension extension, SpecConfig specConfig, 
                              String specName, Project project, ProjectLayout projectLayout, 
                              ObjectFactory objectFactory, ProviderFactory providerFactory, TemplateConfiguration templateConfiguration,
                              TaskProvider<com.guidedbyte.openapi.modelgen.tasks.PrepareTemplateDirectoryTask> prepareTask) {
        
        // Create resolved config to get proper generator name and other settings
        ResolvedSpecConfig resolvedConfig = ResolvedSpecConfig.builder(specName, extension, specConfig).build();
        
        // Set required properties (keep inputSpec as original relative path)
        task.getInputSpec().set(resolvedConfig.getInputSpec());
        task.getModelPackage().set(resolvedConfig.getModelPackage());
        
        // Set generator name from configuration
        task.getGeneratorName().set(resolvedConfig.getGeneratorName());
        
        // Configure output directory from resolved config (resolve relative to project directory)
        File outputDirFile = new File(project.getProjectDir(), resolvedConfig.getOutputDir());
        task.getOutputDir().set(outputDirFile.getAbsolutePath());
        
        // Configure model name prefix and suffix from resolved config
        if (resolvedConfig.getModelNamePrefix() != null) {
            task.getModelNamePrefix().set(resolvedConfig.getModelNamePrefix());
        }
        task.getModelNameSuffix().set(resolvedConfig.getModelNameSuffix());
        
        /*
          Template Directory Configuration Strategy:

          The plugin uses a sophisticated template orchestration system where:
          1. User's templateDir is a SOURCE directory containing their custom templates
          2. build/template-work/{generator} is the ORCHESTRATION directory where all template processing happens
          3. The template-work directory is what gets passed to OpenAPI Generator

          Flow:
          - User templates from templateDir are copied to template-work
          - Plugin YAML customizations are applied to template-work
          - User YAML customizations are applied to template-work
          - Library templates/customizations are applied to template-work
          - OpenAPI Generator uses the fully orchestrated template-work directory

          This ensures clean separation between user source files and build outputs,
          while allowing multiple layers of template customization to be combined.

          For configuration cache compatibility, we set the templateDir to the expected
          working directory path, even if it doesn't exist yet. The TemplatePreparationAction
          will create it before OpenAPI Generator runs.
         */
        
        // Template directory handling - Proper Gradle Solution:
        // Use a dedicated cacheable task that produces the template directory
        if (prepareTask != null && templateConfiguration.hasAnyCustomizations()) {
            // Wire the prepare task output to OpenAPI Generator's templateDir via Provider
            task.getTemplateDir().set(prepareTask.flatMap(prepTask -> 
                prepTask.getOutputDirectory().map(dir -> dir.getAsFile().getAbsolutePath())));
            
            logger.debug("Set templateDir via dedicated PrepareTemplateDirectoryTask output");
        } else {
            logger.debug("No template customizations - templateDir not set (OpenAPI Generator uses defaults)");
        }
        
        // Apply default OpenAPI Generator configuration
        applyDefaultConfiguration(task);
        
        // Configure validation from resolved config
        task.getValidateSpec().set(resolvedConfig.isValidateSpec());
        
        // Apply config options from resolved config
        for (Map.Entry<String, String> entry : resolvedConfig.getConfigOptions().entrySet()) {
            task.getConfigOptions().put(entry.getKey(), entry.getValue());
        }
        
        // Apply global properties from resolved config
        for (Map.Entry<String, String> entry : resolvedConfig.getGlobalProperties().entrySet()) {
            task.getGlobalProperties().put(entry.getKey(), entry.getValue());
        }
        
        
        // Apply import mappings from resolved config
        Map<String, String> importMappings = resolvedConfig.getImportMappings();
        if (!importMappings.isEmpty()) {
            for (Map.Entry<String, String> entry : importMappings.entrySet()) {
                task.getImportMappings().put(entry.getKey(), entry.getValue());
            }
            logger.debug("Configured {} import mappings for spec: {}", importMappings.size(), specName);
        }
        
        // Apply type mappings from resolved config
        Map<String, String> typeMappings = resolvedConfig.getTypeMappings();
        if (!typeMappings.isEmpty()) {
            for (Map.Entry<String, String> entry : typeMappings.entrySet()) {
                task.getTypeMappings().put(entry.getKey(), entry.getValue());
            }
            logger.debug("Configured {} type mappings for spec: {}", typeMappings.size(), specName);
        }

        // Apply schema mappings from resolved config
        Map<String, String> schemaMappings = resolvedConfig.getSchemaMappings();
        if (!schemaMappings.isEmpty()) {
            for (Map.Entry<String, String> entry : schemaMappings.entrySet()) {
                task.getSchemaMappings().put(entry.getKey(), entry.getValue());
            }
            logger.debug("Configured {} schema mappings for spec: {}", schemaMappings.size(), specName);
        }

        // Apply additional properties from resolved config
        Map<String, String> additionalProperties = resolvedConfig.getAdditionalProperties();
        if (!additionalProperties.isEmpty()) {
            for (Map.Entry<String, String> entry : additionalProperties.entrySet()) {
                task.getAdditionalProperties().put(entry.getKey(), entry.getValue());
            }
            logger.debug("Configured {} additional properties for spec: {}", additionalProperties.size(), specName);
        }
        
        // Apply OpenAPI normalizer rules from resolved config
        Map<String, String> openapiNormalizer = resolvedConfig.getOpenapiNormalizer();
        if (!openapiNormalizer.isEmpty()) {
            for (Map.Entry<String, String> entry : openapiNormalizer.entrySet()) {
                task.getOpenapiNormalizer().put(entry.getKey(), entry.getValue());
            }
            logger.debug("Configured {} OpenAPI normalizer rules for spec: {}", openapiNormalizer.size(), specName);
        }
        
        // Configure template variables from resolved configuration
        Map<String, String> templateVariables = resolvedConfig.getTemplateVariables();
        if (!templateVariables.isEmpty()) {
            Map<String, String> expandedVariables = expandTemplateVariables(templateVariables);
            
            // Pass template variables as additional properties - this makes them available to Mustache templates
            for (Map.Entry<String, String> entry : expandedVariables.entrySet()) {
                task.getAdditionalProperties().put(entry.getKey(), entry.getValue());
                logger.debug("Added template variable '{}' = '{}' to additionalProperties", entry.getKey(), entry.getValue());
            }
            logger.debug("Configured {} template variables for spec: {}", expandedVariables.size(), specName);
        }
    }
    
    /**
     * Configures incremental build support for a GenerateTask.
     *
     * <p>This method ensures proper tracking of inputs and outputs for Gradle's
     * incremental build and build cache features.</p>
     *
     * <h3>Key Design Decisions:</h3>
     * <ul>
     *   <li>Each spec automatically gets its own output subdirectory (e.g., build/generated/openapi/pets,
     *       build/generated/openapi/orders) unless explicitly overridden at the spec level.
     *       This prevents build cache conflicts where one spec's cached output could overwrite
     *       another spec's files.</li>
     *   <li>We use upToDateWhen to check if THIS SPECIFIC SPEC's model package
     *       directory exists and contains generated files.</li>
     *   <li>We do NOT add unnecessary internal properties that could cause cache misses.</li>
     * </ul>
     *
     * @param task the GenerateTask to configure
     * @param projectLayout the project layout for path resolution
     * @param extension the plugin extension containing configuration
     * @param specConfig the specification configuration
     */
    public void configureIncrementalBuild(GenerateTask task, ProjectLayout projectLayout,
                                        OpenApiModelGenExtension extension, SpecConfig specConfig) {

        // Input files - resolve inputSpec path relative to project directory
        String inputSpecPath = task.getInputSpec().get();
        File inputSpecFile = projectLayout.getProjectDirectory().file(inputSpecPath).getAsFile();
        task.getInputs().file(inputSpecFile)
                .withPropertyName("openApiSpecFile")
                .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE);

        // VERSION TRACKING FOR BUILD CACHE INVALIDATION
        // Include plugin and OpenAPI Generator versions as inputs so cache is automatically
        // invalidated when either version changes. This prevents stale cache issues after upgrades.
        String pluginVersion = com.guidedbyte.openapi.modelgen.utils.VersionUtils.getCurrentPluginVersion();
        if (pluginVersion != null) {
            task.getInputs().property("openapi.modelgen.pluginVersion", pluginVersion);
            logger.debug("Added plugin version to cache key: {}", pluginVersion);
        }

        // Note: OpenAPI Generator version is already tracked by OpenAPI Generator's GenerateTask
        // as part of its classpath (task implementation version). No need to duplicate.

        // NOTE: Each spec automatically gets its own output subdirectory (via ResolvedSpecConfig),
        // so build cache conflicts are avoided. No need to disable caching.

        // IMPORTANT: We intentionally do NOT declare outputDir as task.getOutputs().dir()
        // because multiple specs may share the same base outputDir. When multiple tasks
        // declare the same directory as output, Gradle's behavior is undefined and can
        // cause tasks to be incorrectly skipped or cached incorrectly.
        //
        // Instead, we rely on:
        // 1. OpenAPI Generator's own output declarations (which are more specific)
        // 2. Our upToDateWhen check below (which verifies spec-specific outputs)

        // Configure up-to-date check to detect missing outputs for THIS SPECIFIC SPEC
        // This ensures tasks re-run after 'clean' even if inputs haven't changed
        task.getOutputs().upToDateWhen(t -> {
            // Check 1: Output directory must be configured
            if (!task.getOutputDir().isPresent()) {
                logger.debug("No output directory configured, task will run");
                return false;
            }

            // Check 2: Model package must be configured
            if (!task.getModelPackage().isPresent()) {
                logger.debug("No model package configured, task will run");
                return false;
            }

            String outputDirPath = task.getOutputDir().get();
            String modelPackage = task.getModelPackage().get();

            // Convert model package to directory path (com.example.api -> com/example/api)
            String packagePath = modelPackage.replace('.', '/');

            // The full path where this spec's models should be generated
            // OpenAPI Generator outputs to: {outputDir}/src/main/java/{packagePath}/
            File modelPackageDir = new File(outputDirPath, "src/main/java/" + packagePath);

            // Check 3: The specific model package directory must exist
            if (!modelPackageDir.exists()) {
                logger.debug("Model package directory doesn't exist, task will run: {}", modelPackageDir);
                return false;
            }

            // Check 4: The directory must contain at least one .java file
            File[] javaFiles = modelPackageDir.listFiles((dir, name) -> name.endsWith(".java"));
            if (javaFiles == null || javaFiles.length == 0) {
                logger.debug("Model package directory has no .java files, task will run: {}", modelPackageDir);
                return false;
            }

            logger.debug("Found {} generated files in {}, deferring to Gradle up-to-date check",
                        javaFiles.length, modelPackageDir);

            // Outputs exist for this specific spec - defer to Gradle's normal up-to-date check
            // based on input file timestamps, configuration properties, etc.
            return true;
        });

        // NOTE: We intentionally do NOT add internal properties as inputs.
        // Previously, internal.templateCacheDirectory and internal.debugProperties
        // were added as input properties, which could cause inconsistent cache behavior:
        // - Absolute paths change between machines/environments
        // - Debug property Maps may serialize inconsistently
        // The OpenAPI Generator's GenerateTask handles its own input/output tracking.

        logger.debug("Configured incremental build for task: {}", task.getName());
    }
    
    /**
     * Configures parallel execution for the aggregate task.
     * 
     * @param aggregateTask the aggregate task to configure
     * @param extension the plugin extension containing configuration
     * @param tasks the task container for finding other tasks
     */
    public void configureParallelExecution(Task aggregateTask, OpenApiModelGenExtension extension, TaskContainer tasks) {
        int specCount = extension.getSpecs().size();
        boolean isParallel = extension.isParallel();
        
        // Add logging actions
        aggregateTask.doFirst(new ParallelExecutionLoggingAction(specCount, isParallel, "start"));
        aggregateTask.doLast(new ParallelExecutionLoggingAction(specCount, isParallel, "end"));
        
        if (isParallel && specCount > 1) {
            logger.debug("Parallel execution enabled for {} specifications", specCount);
            // Note: Actual parallelization is handled by Gradle's --parallel flag
            // No additional task configuration needed here
        } else {
            logger.debug("Sequential execution configured for {} specifications", specCount);
        }
    }
    
    /**
     * Creates the clean task for removing generated code and clearing plugin caches.
     *
     * <p>This task cleans plugin-specific caches (template work directories, global template cache).
     * For Gradle's build cache issues, users should use the built-in {@code gradle cleanBuildCache}.</p>
     *
     * @param tasks the task container to register the clean task with
     * @param extension the plugin extension containing configuration
     * @param projectLayout the project layout for path resolution
     * @param project the Gradle project (unused, kept for API compatibility)
     */
    private void createCleanTask(TaskContainer tasks, OpenApiModelGenExtension extension,
                                  ProjectLayout projectLayout, Project project) {
        Map<String, String> specOutputDirs = new HashMap<>();
        for (Map.Entry<String, SpecConfig> entry : extension.getSpecs().entrySet()) {
            String specName = entry.getKey();
            SpecConfig specConfig = entry.getValue();
            
            // Resolve the output directory for this spec at configuration time
            ResolvedSpecConfig resolvedConfig = ResolvedSpecConfig.builder(
                specName, extension, specConfig).build();
            specOutputDirs.put(specName, resolvedConfig.getOutputDir());
        }
        
        tasks.register(PluginConstants.TASK_CLEAN, task -> {
            task.setDescription(PluginConstants.DESC_CLEAN);
            task.setGroup(PluginConstants.TASK_GROUP);
            task.doLast(t -> {
                boolean anythingDeleted = false;
                
                // Clean generated output directories for each spec
                for (Map.Entry<String, String> entry : specOutputDirs.entrySet()) {
                    String specName = entry.getKey();
                    String outputDirPath = entry.getValue();
                    
                    File outputDir = projectLayout.getProjectDirectory().file(outputDirPath).getAsFile();
                    if (outputDir.exists()) {
                        logger.info("Cleaning generated models for '{}' from: {}", specName, outputDir.getAbsolutePath());
                        if (deleteRecursively(outputDir)) {
                            anythingDeleted = true;
                            System.out.println("✓ Cleaned generated models for '" + specName + "'");
                        }
                    }
                }
                
                // Clean template working directories
                File templateWorkDir = projectLayout.getBuildDirectory()
                    .dir(PluginConstants.TEMPLATE_WORK_DIR).get().getAsFile();
                if (templateWorkDir.exists()) {
                    logger.info("Cleaning template working directory: {}", templateWorkDir.getAbsolutePath());
                    if (deleteRecursively(templateWorkDir)) {
                        anythingDeleted = true;
                        System.out.println("✓ Cleaned template working directories");
                    }
                }
                
                // Clean local working directory caches (.working-dir-cache files)
                // These are stored within the template-work directories, so already cleaned above
                
                // Clear global template cache if it exists
                File globalCacheDir = new File(System.getProperty("user.home"), 
                    ".gradle/caches/openapi-modelgen");
                if (globalCacheDir.exists()) {
                    logger.info("Clearing global template cache: {}", globalCacheDir.getAbsolutePath());
                    if (deleteRecursively(globalCacheDir)) {
                        anythingDeleted = true;
                        System.out.println("✓ Cleared global template cache");
                    }
                }
                
                // Also clear the session cache managed by TemplateCacheManager
                // Since this is in-memory, we can't clear it from here, but the deletion
                // of working directories and global cache will force cache misses

                if (!anythingDeleted) {
                    System.out.println("No generated files or caches to clean");
                } else {
                    System.out.printf("%nSuccessfully cleaned all generated models and template caches%n");
                    System.out.println("Next generation will extract fresh templates and rebuild caches");
                }

                // Show helpful tips for persistent issues
                System.out.println();
                System.out.println("If you experience intermittent generation issues, also try:");
                System.out.println("   gradle cleanBuildCache     (clears Gradle's build cache)");
                System.out.println("   --no-build-cache           (bypasses cache for one build)");
            });
        });
    }
    
    /**
     * Recursively deletes a directory and all its contents.
     * 
     * @param file the file or directory to delete
     * @return true if deletion was successful, false otherwise
     */
    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        return file.delete();
    }
    
    /**
     * Creates the help task with usage information.
     * 
     * @param tasks the task container to register the help task with
     * @param extension the plugin extension containing configuration
     */
    private void createHelpTask(TaskContainer tasks, OpenApiModelGenExtension extension) {
        // Extract spec names at configuration time to avoid capturing extension/project references
        final Map<String, String> specTaskNames = new HashMap<>();
        if (extension.getSpecs() != null && !extension.getSpecs().isEmpty()) {
            for (String specName : extension.getSpecs().keySet()) {
                String taskName = PluginConstants.TASK_PREFIX + capitalize(specName);
                specTaskNames.put(taskName, specName);
            }
        }
        
        tasks.register(PluginConstants.TASK_HELP, task -> {
            task.setDescription(PluginConstants.DESC_HELP);
            task.setGroup(PluginConstants.TASK_GROUP);
            task.doLast(new HelpTaskAction(specTaskNames));
        });
    }
    
    /**
     * Configuration-cache compatible action for the help task.
     */
    private static class HelpTaskAction implements org.gradle.api.Action<org.gradle.api.Task>, Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        
        private final Map<String, String> specTaskNames;
        
        public HelpTaskAction(Map<String, String> specTaskNames) {
            this.specTaskNames = specTaskNames != null ? 
                java.util.Collections.unmodifiableMap(new HashMap<>(specTaskNames)) : java.util.Collections.emptyMap();
        }
        
        @Override
        public void execute(org.gradle.api.Task task) {
            System.out.printf("%n=== OpenAPI Model Generator Plugin ===%n%n");
            System.out.printf("This plugin generates Java DTOs from OpenAPI specifications with Lombok support.%n%n");
            System.out.println("Available tasks:");
            System.out.println("  generateAllModels     - Generate models for all configured specifications");
            
            // Show actual configured spec tasks
            if (!specTaskNames.isEmpty()) {
                for (Map.Entry<String, String> entry : specTaskNames.entrySet()) {
                    String taskName = entry.getKey();
                    String specName = entry.getValue();
                    System.out.println("  " + taskName + "         - Generate models for " + specName + " specification");
                }
            } else {
                System.out.println("  generate<SpecName>    - Generate models for a specific specification");
            }
            
            System.out.println("  generateHelp          - Show this help information");
            System.out.printf("%nConfiguration Example:%n");
            System.out.println("  openapiModelgen {");
            System.out.println("    specs {");
            System.out.println("      pets {");
            System.out.println("        inputSpec \"src/main/resources/openapi-spec/pets.yaml\"");
            System.out.println("        modelPackage \"com.example.pets\"");
            System.out.println("      }");
            System.out.println("    }");
            System.out.println("  }");
            System.out.printf("%nFor complete documentation, visit:%n");
            System.out.printf("  https://github.com/guidedbyte/openapi-modelgen%n%n");
        }
    }

    /**
     * Creates the debug configuration task for troubleshooting plugin configuration.
     *
     * @param tasks the task container to register the debug task with
     * @param extension the plugin extension containing configuration
     * @param project the Gradle project
     * @param projectLayout the project layout for path resolution
     */
    private void createDebugConfigTask(TaskContainer tasks, OpenApiModelGenExtension extension, Project project, ProjectLayout projectLayout) {
        tasks.register("debugOpenApiConfig", task -> {
            task.setDescription("Debug OpenAPI Model Generator plugin configuration");
            task.setGroup(PluginConstants.TASK_GROUP);
            task.doLast(new DebugConfigAction(extension, project, projectLayout));
        });
    }

    /**
     * Configuration-cache compatible action for the debug configuration task.
     */
    private static class DebugConfigAction implements org.gradle.api.Action<org.gradle.api.Task>, Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        // Extract only serializable data at configuration time
        private final boolean parallelMode;
        private final String projectPath;
        private final String buildDirPath;
        private final String gradleVersion;
        private final Map<String, SpecConfigData> specs;
        private final DefaultConfigData defaults;
        private final LibraryData libraryData;

        DebugConfigAction(OpenApiModelGenExtension extension, Project project, ProjectLayout projectLayout) {
            this.parallelMode = extension.isParallel();
            this.projectPath = project.getProjectDir().getAbsolutePath();
            this.buildDirPath = projectLayout.getBuildDirectory().getAsFile().get().getAbsolutePath();
            this.gradleVersion = project.getGradle().getGradleVersion();

            // Extract spec data
            this.specs = new HashMap<>();
            for (Map.Entry<String, SpecConfig> entry : extension.getSpecs().entrySet()) {
                SpecConfig config = entry.getValue();
                this.specs.put(entry.getKey(), new SpecConfigData(
                    config.getInputSpec().getOrNull(),
                    config.getModelPackage().getOrNull(),
                    config.getOutputDir().getOrNull()
                ));
            }

            // Extract defaults data
            DefaultConfig def = extension.getDefaults();
            this.defaults = new DefaultConfigData(
                def.getOutputDir().getOrNull(),
                def.getUserTemplateDir().getOrNull(),
                def.getUserTemplateCustomizationsDir().getOrNull(),
                def.getModelNameSuffix().getOrNull()
            );

            // Extract library data
            LibraryTemplateExtractor.LibraryExtractionResult libContent = extension.getLibraryContent();
            if (libContent != null) {
                this.libraryData = new LibraryData(
                    libContent.getTemplates() != null ? libContent.getTemplates().size() : 0,
                    libContent.getCustomizations() != null ? libContent.getCustomizations().size() : 0,
                    libContent.getMetadata() != null ? libContent.getMetadata().size() : 0
                );
            } else {
                this.libraryData = null;
            }
        }

        private static class SpecConfigData implements Serializable {
            @Serial private static final long serialVersionUID = 1L;
            final String inputSpec;
            final String modelPackage;
            final String outputDir;

            SpecConfigData(String inputSpec, String modelPackage, String outputDir) {
                this.inputSpec = inputSpec;
                this.modelPackage = modelPackage;
                this.outputDir = outputDir;
            }
        }

        private static class DefaultConfigData implements Serializable {
            @Serial private static final long serialVersionUID = 1L;
            final String outputDir;
            final String userTemplateDir;
            final String userCustomizationsDir;
            final String modelNameSuffix;

            DefaultConfigData(String outputDir, String userTemplateDir, String userCustomizationsDir, String modelNameSuffix) {
                this.outputDir = outputDir;
                this.userTemplateDir = userTemplateDir;
                this.userCustomizationsDir = userCustomizationsDir;
                this.modelNameSuffix = modelNameSuffix;
            }
        }

        private static class LibraryData implements Serializable {
            @Serial private static final long serialVersionUID = 1L;
            final int templateCount;
            final int customizationCount;
            final int metadataCount;

            LibraryData(int templateCount, int customizationCount, int metadataCount) {
                this.templateCount = templateCount;
                this.customizationCount = customizationCount;
                this.metadataCount = metadataCount;
            }
        }

        @Override
        public void execute(org.gradle.api.Task task) {
            System.out.printf("%n=== OpenAPI Model Generator Debug Configuration ===%n");

            // Plugin-level configuration
            System.out.printf("%n📋 Plugin Configuration:%n");
            System.out.printf("  Parallel processing: %s%n", parallelMode);
            System.out.printf("  Project directory: %s%n", projectPath);
            System.out.printf("  Build directory: %s%n", buildDirPath);

            // Defaults configuration
            System.out.printf("%n📋 Default Configuration:%n");
            if (defaults.outputDir != null) {
                System.out.printf("  Output directory: %s%n", defaults.outputDir);
            }
            if (defaults.userTemplateDir != null) {
                System.out.printf("  User template directory: %s%n", defaults.userTemplateDir);
            }
            if (defaults.userCustomizationsDir != null) {
                System.out.printf("  User customizations directory: %s%n", defaults.userCustomizationsDir);
            }
            if (defaults.modelNameSuffix != null) {
                System.out.printf("  Model name suffix: %s%n", defaults.modelNameSuffix);
            }

            // Library configuration
            if (libraryData != null) {
                System.out.printf("%n📚 Library Configuration:%n");
                System.out.printf("  Library templates: %d%n", libraryData.templateCount);
                System.out.printf("  Library customizations: %d%n", libraryData.customizationCount);
                System.out.printf("  Library metadata files: %d%n", libraryData.metadataCount);
            }

            // Specifications configuration
            System.out.printf("%n📝 Specifications Configuration:%n");
            if (specs.isEmpty()) {
                System.out.println("  ⚠️  No specifications configured");
                System.out.println("  💡 Add specs using: openapiModelgen { specs { ... } }");
            } else {
                System.out.printf("  Total specifications: %d%n", specs.size());
                for (Map.Entry<String, SpecConfigData> entry : specs.entrySet()) {
                    String specName = entry.getKey();
                    SpecConfigData specConfig = entry.getValue();

                    System.out.printf("%n  📄 Spec: %s%n", specName);
                    if (specConfig.inputSpec != null) {
                        System.out.printf("    Input spec: %s%n", specConfig.inputSpec);

                        // Check if file exists
                        File inputFile = new File(specConfig.inputSpec);
                        if (!inputFile.isAbsolute()) {
                            inputFile = new File(projectPath, specConfig.inputSpec);
                        }
                        System.out.printf("    File exists: %s%n", inputFile.exists() ? "✅ Yes" : "❌ No");
                        if (inputFile.exists()) {
                            System.out.printf("    File size: %d bytes%n", inputFile.length());
                        }
                    }

                    if (specConfig.modelPackage != null) {
                        System.out.printf("    Model package: %s%n", specConfig.modelPackage);
                    }
                    if (specConfig.outputDir != null) {
                        System.out.printf("    Output directory: %s%n", specConfig.outputDir);
                    }
                    // Generator is typically "spring" by default
                    System.out.printf("    Generator: %s (default)%n", "spring");
                }
            }

            // Environment information
            System.out.printf("%n🔧 Environment Information:%n");
            System.out.printf("  Java version: %s%n", System.getProperty("java.version"));
            System.out.printf("  Gradle version: %s%n", gradleVersion);
            System.out.printf("  OS: %s %s%n", System.getProperty("os.name"), System.getProperty("os.version"));

            // Cache information
            System.out.printf("%n💾 Cache Information:%n");
            String globalCacheDir = System.getProperty("user.home") + "/.gradle/caches/openapi-modelgen";
            File globalCache = new File(globalCacheDir);
            System.out.printf("  Global cache directory: %s%n", globalCacheDir);
            System.out.printf("  Global cache exists: %s%n", globalCache.exists() ? "✅ Yes" : "❌ No");
            if (globalCache.exists()) {
                File[] cacheFiles = globalCache.listFiles();
                int fileCount = (cacheFiles != null) ? cacheFiles.length : 0;
                System.out.printf("  Global cache files: %d%n", fileCount);
            }

            String workingCacheDir = buildDirPath + "/template-work";
            File workingCache = new File(workingCacheDir);
            System.out.printf("  Working cache directory: %s%n", workingCacheDir);
            System.out.printf("  Working cache exists: %s%n", workingCache.exists() ? "✅ Yes" : "❌ No");

            // Troubleshooting tips
            System.out.printf("%n💡 Troubleshooting Tips:%n");
            System.out.println("  - Enable debug mode: openapiModelgen { debug true }");
            System.out.println("  - Clear caches: gradle generateClean");
            System.out.println("  - Validate OpenAPI specs: https://editor.swagger.io/");
            System.out.println("  - Check file permissions and paths");
            System.out.println("  - Use --debug flag for detailed logging");

            System.out.printf("%n=== End Debug Configuration ===%n%n");
        }
    }

    /**
     * Applies default OpenAPI Generator configuration optimized for Spring Boot 3 + Lombok.
     * 
     * @param task the GenerateTask to configure
     */
    private void applyDefaultConfiguration(GenerateTask task) {
        // Core Spring Boot 3 + Jakarta EE configuration
        task.getConfigOptions().put("annotationLibrary", "swagger2");
        task.getConfigOptions().put("swagger2AnnotationLibrary", "true");
        task.getConfigOptions().put("useSpringBoot3", "true");
        task.getConfigOptions().put("useJakartaEe", "true");
        task.getConfigOptions().put("useBeanValidation", "true");
        task.getConfigOptions().put("dateLibrary", "java8");
        task.getConfigOptions().put("serializableModel", "true");
        task.getConfigOptions().put("hideGenerationTimestamp", "true");
        task.getConfigOptions().put("performBeanValidation", "true");
        task.getConfigOptions().put("enumUnknownDefaultCase", "true");
        task.getConfigOptions().put("generateBuilders", "true");
        task.getConfigOptions().put("legacyDiscriminatorBehavior", "false");
        task.getConfigOptions().put("disallowAdditionalPropertiesIfNotPresent", "false");
        task.getConfigOptions().put("useEnumCaseInsensitive", "true");
        task.getConfigOptions().put("openApiNullable", "false");
        
        // Lombok compatibility
        task.getConfigOptions().put("skipDefaults", "true");
        task.getConfigOptions().put("generateConstructorPropertiesAnnotation", "false");
        
        
        // Global properties - let OpenAPI Generator use its defaults
    }

    /**
     * Expands template variables recursively (e.g., {{copyright}} containing {{currentYear}}).
     * 
     * @param variables the template variables to expand
     * @return the expanded variables map
     */
    private Map<String, String> expandTemplateVariables(Map<String, String> variables) {
        Map<String, String> expanded = new HashMap<>(variables);
        
        // Add built-in variables
        expanded.putIfAbsent("currentYear", String.valueOf(java.time.Year.now().getValue()));
        expanded.putIfAbsent("generatedBy", "OpenAPI Model Generator Plugin");
        
        // Expand variables recursively (max 10 iterations to prevent infinite loops)
        for (int iteration = 0; iteration < 10; iteration++) {
            boolean anyChanges = false;
            
            for (Map.Entry<String, String> entry : expanded.entrySet()) {
                String value = entry.getValue();
                String expandedValue = expandSingleVariable(value, expanded);
                if (!value.equals(expandedValue)) {
                    expanded.put(entry.getKey(), expandedValue);
                    anyChanges = true;
                }
            }
            
            if (!anyChanges) {
                break; // No more expansions needed
            }
        }
        
        return expanded;
    }
    
    /**
     * Expands variables in a single string value.
     * 
     * @param value the string value to expand
     * @param variables the variables map for expansion
     * @return the expanded string value
     */
    private String expandSingleVariable(String value, Map<String, String> variables) {
        if (value == null) return null;
        
        String result = value;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String variableName = entry.getKey();
            String variableValue = variables.get(variableName);
            if (variableValue != null) {
                result = result.replace("{{" + variableName + "}}", variableValue);
            }
        }
        return result;
    }
    
    /**
     * Adds debug logging hooks to a GenerateTask for OpenAPI Generator execution monitoring.
     *
     * @param task the GenerateTask to instrument with debug logging
     * @param specName the name of the specification being processed
     * @param extension the plugin extension containing debug configuration
     */
    private void addExecutionDebugLogging(GenerateTask task, String specName, OpenApiModelGenExtension extension) {
        task.doFirst(t -> {
            logger.info("🚀 Starting OpenAPI generation for spec: {}", specName);
            try {
                // Log key configuration details that users often need for troubleshooting
                logger.debug("=== OpenAPI Generator Execution Debug ===");
                logger.debug("Spec: {}", specName);
                logger.debug("Generator: {}", task.getGeneratorName().getOrElse("unknown"));
                logger.debug("Input spec: {}", task.getInputSpec().getOrElse("not set"));
                logger.debug("Output directory: {}", task.getOutputDir().getOrElse("not set"));
                logger.debug("Package name: {}", task.getModelPackage().getOrElse("not set"));

                if (task.getTemplateDir().isPresent()) {
                    logger.debug("Template directory: {}", task.getTemplateDir().get());
                } else {
                    logger.debug("Template directory: using OpenAPI Generator defaults");
                }

                // Log file existence checks for common issues
                if (task.getInputSpec().isPresent()) {
                    String inputSpecPath = task.getInputSpec().get();
                    File inputFile = new File(inputSpecPath);
                    if (inputFile.isAbsolute()) {
                        logger.debug("Input spec file check: exists={}, readable={}", inputFile.exists(), inputFile.canRead());
                    } else {
                        File resolvedFile = new File(t.getProject().getProjectDir(), inputSpecPath);
                        logger.debug("Input spec resolved path: {}", resolvedFile.getAbsolutePath());
                        logger.debug("Input spec file check: exists={}, readable={}", resolvedFile.exists(), resolvedFile.canRead());
                    }
                }

                if (task.getOutputDir().isPresent()) {
                    String outputDirPath = task.getOutputDir().get();
                    File outputDir = new File(outputDirPath);
                    logger.debug("Output directory check: exists={}, writable={}", outputDir.exists(), outputDir.canWrite());
                    if (!outputDir.exists()) {
                        logger.debug("Output directory will be created during generation");
                    }
                }

                logger.debug("=== End OpenAPI Generator Debug ===");
            } catch (Exception e) {
                logger.warn("Error during pre-execution debug logging for spec '{}': {}", specName, e.getMessage());
            }
        });

        task.doLast(t -> {
            try {
                logger.info("✅ OpenAPI generation completed for spec: {}", specName);

                // Log post-generation information
                if (task.getOutputDir().isPresent()) {
                    String outputDirPath = task.getOutputDir().get();
                    File outputDir = new File(outputDirPath);
                    if (outputDir.exists()) {
                        File[] generatedFiles = outputDir.listFiles();
                        int fileCount = (generatedFiles != null) ? generatedFiles.length : 0;
                        logger.debug("Generated {} files in output directory: {}", fileCount, outputDir.getAbsolutePath());

                        // Look for common generated files to verify success
                        if (fileCount > 0) {
                            logger.debug("Generation appears successful - output directory contains files");
                        } else {
                            logger.warn("Output directory is empty - generation may have failed silently");
                        }
                    } else {
                        logger.warn("Output directory does not exist after generation: {}", outputDir.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                logger.warn("Error during post-execution debug logging for spec '{}': {}", specName, e.getMessage());
            }
        });

        // Add comprehensive error handling for generation failures
        addGenerationFailureHandling(task, specName, extension);
    }

    /**
     * Adds comprehensive error handling to a GenerateTask to provide actionable error messages.
     *
     * @param task the GenerateTask to instrument with error handling
     * @param specName the name of the specification being processed
     * @param extension the plugin extension containing debug configuration
     */
    private void addGenerationFailureHandling(GenerateTask task, String specName, OpenApiModelGenExtension extension) {
        // Add error handling through task lifecycle hooks without capturing Project reference
        task.doFirst(t -> {
            // Log context setup - no project state needed
        });

        // Add error logging directly in the task's doLast
        task.doLast(t -> {
            // Check if task failed and log detailed failure information
            if (task.getState().getFailure() != null) {
                logGenerationFailure(task, specName, task.getState().getFailure());
            }
        });
    }

    /**
     * Logs detailed information about OpenAPI generation failures with actionable guidance.
     *
     * @param task the failed GenerateTask
     * @param specName the name of the specification
     * @param failure the exception that caused the failure
     */
    private void logGenerationFailure(GenerateTask task, String specName, Throwable failure) {
        logger.error("OpenAPI generation FAILED for spec: {}", specName);
        logger.error("Error: {}", failure.getMessage());

        // Provide comprehensive context about the failure
        logger.debug("=== OpenAPI Generation Failure Analysis ===");
        logger.debug("Spec: {}", specName);

            try {
                // Log task configuration at time of failure
                if (task.getGeneratorName().isPresent()) {
                    logger.debug("Generator: {}", task.getGeneratorName().get());
                }
                if (task.getInputSpec().isPresent()) {
                    String inputSpec = task.getInputSpec().get();
                    logger.debug("Input spec: {}", inputSpec);

                    // Check if input spec file still exists
                    File inputFile = new File(inputSpec);
                    if (!inputFile.isAbsolute()) {
                        inputFile = new File(task.getProject().getProjectDir(), inputSpec);
                    }
                    logger.debug("Input spec file exists: {}", inputFile.exists());
                }

                if (task.getOutputDir().isPresent()) {
                    String outputDirPath = task.getOutputDir().get();
                    File outputDir = new File(outputDirPath);
                    logger.debug("Output directory: {}", outputDir.getAbsolutePath());
                    logger.debug("Output directory exists: {}", outputDir.exists());
                    logger.debug("Output directory writable: {}", outputDir.canWrite());
                }

                if (task.getTemplateDir().isPresent()) {
                    logger.debug("Template directory: {}", task.getTemplateDir().get());
                }

                // Analyze the error and provide specific guidance
                String errorMessage = StringUtils.toRootLowerCase(failure.getMessage());
                logger.debug("=== Troubleshooting Guidance ===");

                if (errorMessage.contains("file not found") || errorMessage.contains("no such file")) {
                    logger.info("💡 SOLUTION: Input spec file not found");
                    logger.info("   - Verify the inputSpec path is correct");
                    logger.info("   - Check if the file exists relative to project directory");
                    logger.info("   - Consider using an absolute path");
                } else if (errorMessage.contains("invalid") || errorMessage.contains("parse") || errorMessage.contains("yaml") || errorMessage.contains("json")) {
                    logger.info("💡 SOLUTION: OpenAPI specification is invalid");
                    logger.info("   - Validate your OpenAPI spec with: https://editor.swagger.io/");
                    logger.info("   - Check YAML/JSON syntax");
                    logger.info("   - Ensure all required OpenAPI fields are present");
                } else if (errorMessage.contains("permission") || errorMessage.contains("access")) {
                    logger.info("💡 SOLUTION: File permission issues");
                    logger.info("   - Check file and directory permissions");
                    logger.info("   - Ensure output directory is writable");
                    logger.info("   - Try running with elevated permissions if necessary");
                } else if (errorMessage.contains("template") || errorMessage.contains("mustache")) {
                    logger.info("💡 SOLUTION: Template processing error");
                    logger.info("   - Check custom template syntax");
                    logger.info("   - Verify template directory structure");
                    logger.info("   - Consider enabling debug mode for template resolution");
                } else if (errorMessage.contains("generator") || errorMessage.contains("language")) {
                    logger.info("💡 SOLUTION: Generator configuration issue");
                    logger.info("   - Verify generator name is supported");
                    logger.info("   - Check OpenAPI Generator version compatibility");
                    logger.info("   - Review generator-specific configuration options");
                } else {
                    logger.info("💡 SOLUTION: General troubleshooting steps");
                    logger.info("   - Enable debug mode: openapiModelgen { debug true }");
                    logger.info("   - Check OpenAPI Generator logs for more details");
                    logger.info("   - Try generating with minimal configuration first");
                    logger.info("   - Verify all required dependencies are present");
                }

                logger.debug("=== End Failure Analysis ===");

            } catch (Exception e) {
                logger.warn("Error during failure analysis for spec '{}': {}", specName, e.getMessage());
            }

        // Always show basic troubleshooting info
        logger.error("Enable debug mode for detailed analysis: openapiModelgen {{ debug true }}");
        logger.error("Documentation: https://github.com/GuidedByte/openapi-modelgen");
    }


    /**
     * Capitalizes the first letter of a string.
     *
     * @param str the string to capitalize
     * @return the capitalized string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return StringUtils.toRootUpperCase(str.substring(0, 1)) + str.substring(1);
    }
}