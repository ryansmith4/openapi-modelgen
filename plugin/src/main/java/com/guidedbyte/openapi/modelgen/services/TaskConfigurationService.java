package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.DefaultConfig;
import com.guidedbyte.openapi.modelgen.OpenApiModelGenExtension;
import com.guidedbyte.openapi.modelgen.ResolvedSpecConfig;
import com.guidedbyte.openapi.modelgen.SpecConfig;
import com.guidedbyte.openapi.modelgen.TemplateConfiguration;
import com.guidedbyte.openapi.modelgen.actions.ParallelExecutionLoggingAction;
import com.guidedbyte.openapi.modelgen.actions.TemplateDirectorySetupAction;
import com.guidedbyte.openapi.modelgen.actions.TemplatePreparationAction;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(TaskConfigurationService.class);
    
    private final TemplateCacheManager templateCacheManager;
    
    /**
     * Creates a new TaskConfigurationService with dependencies.
     * 
     * @param templateCacheManager the cache manager for template operations and optimization
     */
    public TaskConfigurationService(TemplateCacheManager templateCacheManager) {
        this.templateCacheManager = templateCacheManager;
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
        
        // Create setup task that ensures template directories exist
        TaskProvider<Task> setupTemplatesTask = tasks.register("setupTemplateDirectories", task -> {
            task.setDescription("Creates required template directories");
            task.setGroup("openapi modelgen");
            task.doLast(new TemplateDirectorySetupAction(extension.getSpecs(), extension.getDefaults(), projectLayout));
        });
        
        // Create individual tasks for each spec
        extension.getSpecs().forEach((specName, specConfig) -> {
            String taskName = "generate" + capitalize(specName);
            
            TaskProvider<GenerateTask> specTask = tasks.register(taskName, GenerateTask.class, task -> {
                // Make sure template setup runs first
                task.dependsOn(setupTemplatesTask);
                configureGenerateTask(task, extension, specConfig, specName, project, projectLayout, objectFactory, providerFactory);
                
                // Configure incremental build support
                configureIncrementalBuild(task, projectLayout, extension, specConfig);
            });
        });
        
        // Create aggregate task that runs all spec tasks
        if (!extension.getSpecs().isEmpty()) {
            TaskProvider<Task> aggregateTask = tasks.register("generateAllModels", task -> {
                task.setDescription("Generates models for all OpenAPI specifications");
                task.setGroup("openapi modelgen");
                
                // Configure parallel execution based on user preference
                configureParallelExecution(task, extension, tasks);
                
                // Make this task depend on all spec tasks
                extension.getSpecs().keySet().forEach(specName -> {
                    String specTaskName = "generate" + capitalize(specName);
                    task.dependsOn(specTaskName);
                });
            });
        }
        
        // Create clean task for removing generated code and caches
        createCleanTask(tasks, extension, projectLayout);
        
        // Create help task with usage information
        createHelpTask(tasks, extension);
        
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
        ResolvedSpecConfig resolvedConfig = ResolvedSpecConfig.builder(specName, extension.getDefaults(), specConfig).build();
        
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
                                    ProviderFactory providerFactory) {
        
        // Set basic task properties
        task.setDescription("Generate models for " + specName + " OpenAPI specification");
        task.setGroup("openapi modelgen");
        
        // Apply spec configuration
        applySpecConfig(task, extension, specConfig, specName, project, projectLayout, objectFactory, providerFactory);
        
        // Add template preparation action with resolved template configuration
        TemplateConfiguration templateConfiguration = resolveTemplateConfiguration(extension, specConfig, specName, project, projectLayout);
        task.doFirst(new TemplatePreparationAction(templateConfiguration));
        
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
     */
    public void applySpecConfig(GenerateTask task, OpenApiModelGenExtension extension, SpecConfig specConfig, 
                              String specName, Project project, ProjectLayout projectLayout, 
                              ObjectFactory objectFactory, ProviderFactory providerFactory) {
        
        // Create resolved config to get proper generator name and other settings
        ResolvedSpecConfig resolvedConfig = ResolvedSpecConfig.builder(specName, extension.getDefaults(), specConfig).build();
        
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
        
        /**
         * Template Directory Configuration Strategy:
         * 
         * The plugin uses a sophisticated template orchestration system where:
         * 1. User's templateDir is a SOURCE directory containing their custom templates
         * 2. build/template-work/{generator} is the ORCHESTRATION directory where all template processing happens
         * 3. The template-work directory is what gets passed to OpenAPI Generator
         * 
         * Flow:
         * - User templates from templateDir are copied to template-work
         * - Plugin YAML customizations are applied to template-work
         * - User YAML customizations are applied to template-work
         * - Library templates/customizations are applied to template-work
         * - OpenAPI Generator uses the fully orchestrated template-work directory
         * 
         * This ensures clean separation between user source files and build outputs,
         * while allowing multiple layers of template customization to be combined.
         */
        
        // ALWAYS use the template working directory if it exists (it contains the orchestrated templates)
        File templateWorkDir = new File(projectLayout.getBuildDirectory()
            .dir("template-work/" + resolvedConfig.getGeneratorName()).get().getAsFile().getAbsolutePath());
        
        if (templateWorkDir.exists() && templateWorkDir.isDirectory()) {
            // This is the correct behavior - use the orchestrated template directory
            task.getTemplateDir().set(templateWorkDir.getAbsolutePath());
            logger.debug("Using template working directory (orchestrated templates): {}", templateWorkDir.getAbsolutePath());
        } else if (resolvedConfig.getTemplateDir() != null) {
            // Fallback for cases where no template processing was needed (rare)
            // This would only happen if the user specified a templateDir but no customizations
            // were applied and no templates needed to be extracted
            File templateDir = new File(resolvedConfig.getTemplateDir());
            if (templateDir.exists()) {
                task.getTemplateDir().set(resolvedConfig.getTemplateDir());
                logger.debug("Using user template directory directly (no orchestration needed): {}", templateDir.getAbsolutePath());
            } else {
                logger.debug("Template directory does not exist, using OpenAPI Generator defaults: {}", templateDir.getAbsolutePath());
            }
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
        
        // Configure template variables from resolved configuration
        Map<String, String> templateVariables = resolvedConfig.getTemplateVariables();
        if (!templateVariables.isEmpty()) {
            Map<String, String> expandedVariables = expandTemplateVariables(templateVariables);
            // Add expanded variables to global properties since GenerateTask doesn't have templateProperties
            for (Map.Entry<String, String> entry : expandedVariables.entrySet()) {
                task.getGlobalProperties().put(entry.getKey(), entry.getValue());
            }
            logger.debug("Configured {} template variables for spec: {}", expandedVariables.size(), specName);
        }
    }
    
    /**
     * Configures incremental build support for a GenerateTask.
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
        
        // Template directory - Skip this as GenerateTask already declares templateDir inputs
        // if (task.getTemplateDir().isPresent()) {
        //     File templateDir = new File(task.getTemplateDir().get());
        //     if (templateDir.exists()) {
        //         task.getInputs().dir(templateDir)
        //                 .withPropertyName("templateDir")
        //                 .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE);
        //     }
        // }
        
        // Output directory - Skip this as GenerateTask already declares its outputs
        // task.getOutputs().dir(task.getOutputDir()).withPropertyName("outputDir");
        
        // Configuration properties that affect output
        Map<String, Object> outputAffectingProps = extractOutputAffectingProperties(extension, specConfig);
        configureInternalProperties(task, projectLayout, outputAffectingProps);
        
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
     * Creates the clean task for removing generated code and clearing caches.
     * 
     * @param tasks the task container to register the clean task with
     * @param extension the plugin extension containing configuration
     * @param projectLayout the project layout for path resolution
     */
    private void createCleanTask(TaskContainer tasks, OpenApiModelGenExtension extension, ProjectLayout projectLayout) {
        tasks.register("generateClean", task -> {
            task.setDescription("Removes all generated OpenAPI models and clears template caches");
            task.setGroup("openapi modelgen");
            task.doLast(t -> {
                boolean anythingDeleted = false;
                
                // Clean generated output directories for each spec
                for (Map.Entry<String, SpecConfig> entry : extension.getSpecs().entrySet()) {
                    String specName = entry.getKey();
                    SpecConfig specConfig = entry.getValue();
                    
                    // Resolve the output directory for this spec
                    ResolvedSpecConfig resolvedConfig = ResolvedSpecConfig.builder(
                        specName, extension.getDefaults(), specConfig).build();
                    
                    File outputDir = new File(t.getProject().getProjectDir(), resolvedConfig.getOutputDir());
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
                    .dir("template-work").get().getAsFile();
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
                    System.out.println("\nSuccessfully cleaned all generated models and template caches");
                    System.out.println("Next generation will extract fresh templates and rebuild caches");
                }
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
        tasks.register("generateHelp", task -> {
            task.setDescription("Shows usage information and examples for the OpenAPI Model Generator plugin");
            task.setGroup("openapi modelgen");
            task.doLast(t -> {
                System.out.println("\n=== OpenAPI Model Generator Plugin ===\n");
                System.out.println("This plugin generates Java DTOs from OpenAPI specifications with Lombok support.\n");
                System.out.println("Available tasks:");
                System.out.println("  generateAllModels     - Generate models for all configured specifications");
                
                // Show actual configured spec tasks
                if (extension.getSpecs() != null && !extension.getSpecs().isEmpty()) {
                    for (String specName : extension.getSpecs().keySet()) {
                        String taskName = "generate" + capitalize(specName);
                        System.out.println("  " + taskName + "         - Generate models for " + specName + " specification");
                    }
                } else {
                    System.out.println("  generate<SpecName>    - Generate models for a specific specification");
                }
                
                System.out.println("  generateHelp          - Show this help information");
                System.out.println("\nConfiguration Example:");
                System.out.println("  openapiModelgen {");
                System.out.println("    specs {");
                System.out.println("      pets {");
                System.out.println("        inputSpec \"src/main/resources/openapi-spec/pets.yaml\"");
                System.out.println("        modelPackage \"com.example.pets\"");
                System.out.println("      }");
                System.out.println("    }");
                System.out.println("  }");
                System.out.println("\nFor complete documentation, visit:");
                System.out.println("  https://github.com/guidedbyte/openapi-modelgen\n");
            });
        });
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
        
        // Default Lombok annotations
        task.getConfigOptions().put("additionalModelTypeAnnotations", 
            "@lombok.Data;@lombok.experimental.Accessors(fluent = true);@lombok.experimental.SuperBuilder;@lombok.NoArgsConstructor(force = true);@lombok.AllArgsConstructor");
        
        // Global properties - let OpenAPI Generator use its defaults
    }
    
    /**
     * Extracts properties that affect output generation for incremental builds.
     * 
     * @param extension the plugin extension containing configuration
     * @param specConfig the specification configuration
     * @return map of properties that affect output generation
     */
    private Map<String, Object> extractOutputAffectingProperties(OpenApiModelGenExtension extension, SpecConfig specConfig) {
        Map<String, Object> outputAffecting = new HashMap<>();
        
        // Spec configuration that affects output
        outputAffecting.put("inputSpec", specConfig.getInputSpec().getOrNull());
        outputAffecting.put("modelPackage", specConfig.getModelPackage().getOrNull());
        outputAffecting.put("modelNamePrefix", specConfig.getModelNamePrefix().getOrNull());
        outputAffecting.put("modelNameSuffix", specConfig.getModelNameSuffix().getOrNull());
        
        // Config options that affect output
        if (specConfig.getConfigOptions().isPresent()) {
            for (Map.Entry<String, String> entry : specConfig.getConfigOptions().get().entrySet()) {
                outputAffecting.put("configOptions." + entry.getKey(), entry.getValue());
            }
        }
        
        return outputAffecting;
    }
    
    /**
     * Configures internal properties for incremental builds.
     * 
     * @param task the GenerateTask to configure
     * @param projectLayout the project layout for path resolution
     * @param additionalProps additional properties to include
     */
    private void configureInternalProperties(GenerateTask task, ProjectLayout projectLayout, Map<String, Object> additionalProps) {
        // Mark internal properties that shouldn't trigger rebuilds
        task.getInputs().property("internal.templateCacheDirectory", 
                projectLayout.getBuildDirectory().dir("plugin-templates").get().getAsFile().getAbsolutePath())
                .optional(true);
        
        // Add debug properties
        Map<String, String> debugProps = new HashMap<>();
        for (Map.Entry<String, Object> entry : additionalProps.entrySet()) {
            debugProps.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        
        task.getInputs().property("internal.debugProperties", debugProps).optional(true);
    }
    
    /**
     * Converts template variables map to string map.
     * 
     * @param templateVariables the template variables to convert
     * @return the converted string map
     */
    private Map<String, String> convertToStringMap(Map<String, Object> templateVariables) {
        Map<String, String> stringVariables = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : templateVariables.entrySet()) {
            if (entry.getValue() instanceof String) {
                stringVariables.put(entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() != null) {
                stringVariables.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        
        return stringVariables;
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
     * Capitalizes the first letter of a string.
     * 
     * @param str the string to capitalize
     * @return the capitalized string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}