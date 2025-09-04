package com.guidedbyte.openapi.modelgen;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.openapitools.generator.gradle.plugin.OpenApiGeneratorPlugin;
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * OpenAPI Model Generator Plugin - A comprehensive Gradle plugin that wraps the OpenAPI Generator 
 * with enhanced features for generating Java DTOs with Lombok support, custom templates, and 
 * enterprise-grade performance optimizations.
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><strong>Template Precedence System:</strong> user templates &gt; plugin templates &gt; OpenAPI generator defaults</li>
 *   <li><strong>Incremental Build Support:</strong> Only regenerates when inputs actually change</li>
 *   <li><strong>Configuration Validation:</strong> Comprehensive validation with detailed error reporting</li>
 *   <li><strong>Parallel Template Processing:</strong> Concurrent template extraction for large template sets</li>
 *   <li><strong>Content-Based Change Detection:</strong> SHA-256 hashing for reliable template change detection</li>
 *   <li><strong>Spec-Level Configuration:</strong> Individual validation and template settings per specification</li>
 *   <li><strong>Multi-Spec Support:</strong> Generate DTOs from multiple OpenAPI specifications</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * openapiModelgen {
 *     defaults {
 *         validateSpec true
 *         modelNameSuffix "Dto" 
 *         templateVariables([
 *             copyright: "Copyright © {{currentYear}} {{companyName}}",
 *             currentYear: "2025",
 *             companyName: "MyCompany Inc."
 *         ])
 *     }
 *     specs {
 *         pets {
 *             inputSpec "src/main/resources/openapi-spec/pets.yaml"
 *             modelPackage "com.example.model.pets"
 *         }
 *         orders {
 *             inputSpec "src/main/resources/openapi-spec/orders.yaml" 
 *             modelPackage "com.example.model.orders"
 *             validateSpec false  // Override for legacy spec
 *             generateModelTests false  // Skip tests for this spec
 *             generateApiDocumentation true  // Generate docs despite being legacy
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
public class OpenApiModelGenPlugin implements Plugin<Project> {
    
    /**
     * Applies the OpenAPI Model Generator plugin to the given project.
     * 
     * <p>This method:</p>
     * <ul>
     *   <li>Ensures the OpenAPI Generator plugin is available</li>
     *   <li>Applies the OpenAPI Generator plugin as a dependency</li>
     *   <li>Creates and configures the 'openapiModelgen' extension</li>
     *   <li>Sets up validation and task creation after project evaluation</li>
     * </ul>
     * 
     * @param project the Gradle project to apply this plugin to
     */
    @Override
    public void apply(Project project) {
        // Detect and ensure OpenAPI Generator plugin is available
        ensureOpenApiGeneratorAvailable(project);
        
        // Apply the OpenAPI Generator plugin as a dependency
        project.getPlugins().apply(OpenApiGeneratorPlugin.class);
        
        // Create our extension
        OpenApiModelGenExtension extension = project.getExtensions()
            .create("openapiModelgen", OpenApiModelGenExtension.class, project);
        
        // Configure tasks after project evaluation
        project.afterEvaluate(proj -> {
            validateExtensionConfiguration(proj, extension);
            createTasksForSpecs(proj, extension);
        });
    }
    
    /**
     * Validates the entire extension configuration and all spec configurations.
     * 
     * <p>Performs comprehensive validation including:</p>
     * <ul>
     *   <li>Default configuration validation (template directories, output directories)</li>
     *   <li>Individual spec configuration validation (input specs, packages, model names)</li>
     *   <li>Spec name uniqueness and Java identifier compliance</li>
     *   <li>Configuration option validation</li>
     * </ul>
     * 
     * @param project the Gradle project
     * @param extension the plugin extension containing configuration
     * @throws InvalidUserDataException if validation fails with detailed error messages
     */
    private void validateExtensionConfiguration(Project project, OpenApiModelGenExtension extension) {
        List<String> allErrors = new ArrayList<>();
        
        // Validate default configuration
        validateDefaultConfiguration(extension.getDefaults(), allErrors);
        
        // Validate each spec configuration
        extension.getSpecs().forEach((specName, specConfig) -> {
            validateSpecConfiguration(project, specName, specConfig, extension.getDefaults(), allErrors);
        });
        
        // Check for duplicate spec names (case-insensitive)
        validateSpecNames(extension.getSpecs(), allErrors);
        
        // If we have any errors, throw an exception with all of them
        if (!allErrors.isEmpty()) {
            throw new InvalidUserDataException("OpenAPI Model Generator configuration validation failed:\n" + 
                String.join("\n", allErrors));
        }
    }
    
    /**
     * Validates default configuration settings
     */
    private void validateDefaultConfiguration(DefaultConfig defaults, List<String> errors) {
        // Validate output directory if specified
        if (defaults.getOutputDir().isPresent()) {
            String outputDir = defaults.getOutputDir().get();
            if (outputDir == null || outputDir.trim().isEmpty()) {
                errors.add("defaults.outputDir cannot be empty");
            }
        }
        
        // Validate template directory if specified
        if (defaults.getTemplateDir().isPresent()) {
            String templateDir = defaults.getTemplateDir().get();
            if (templateDir != null && !templateDir.trim().isEmpty()) {
                File templateDirFile = new File(templateDir);
                if (!templateDirFile.exists()) {
                    errors.add("defaults.templateDir does not exist: " + templateDir);
                } else if (!templateDirFile.isDirectory()) {
                    errors.add("defaults.templateDir is not a directory: " + templateDir);
                }
            }
        }
        
        // Validate config options for common mistakes
        if (defaults.getConfigOptions().isPresent()) {
            validateConfigOptions(defaults.getConfigOptions().get(), "defaults", errors);
        }
    }
    
    /**
     * Validates individual spec configuration
     */
    private void validateSpecConfiguration(Project project, String specName, SpecConfig specConfig, 
                                         DefaultConfig defaults, List<String> errors) {
        String specPrefix = "spec '" + specName + "'";
        
        // Validate spec name
        if (specName == null || specName.trim().isEmpty()) {
            errors.add("Spec name cannot be empty");
            return; // Can't continue without a valid spec name
        }
        
        if (!specName.matches("[a-zA-Z][a-zA-Z0-9_]*")) {
            errors.add(specPrefix + ": spec name must start with a letter and contain only letters, numbers, and underscores");
        }
        
        // Validate required fields
        if (!specConfig.getInputSpec().isPresent() || specConfig.getInputSpec().get() == null || 
            specConfig.getInputSpec().get().trim().isEmpty()) {
            errors.add(specPrefix + ": inputSpec is required and cannot be empty");
        } else {
            // Validate that input spec file exists
            String inputSpecPath = specConfig.getInputSpec().get();
            File inputSpecFile = project.file(inputSpecPath);
            if (!inputSpecFile.exists()) {
                errors.add(specPrefix + ": inputSpec file does not exist: " + inputSpecPath);
            } else if (!inputSpecFile.isFile()) {
                errors.add(specPrefix + ": inputSpec is not a file: " + inputSpecPath);
            } else {
                // Validate file extension
                String fileName = inputSpecFile.getName().toLowerCase();
                if (!fileName.endsWith(".yaml") && !fileName.endsWith(".yml") && !fileName.endsWith(".json")) {
                    errors.add(specPrefix + ": inputSpec should be a YAML (.yaml/.yml) or JSON (.json) file: " + inputSpecPath);
                }
            }
        }
        
        if (!specConfig.getModelPackage().isPresent() || specConfig.getModelPackage().get() == null || 
            specConfig.getModelPackage().get().trim().isEmpty()) {
            errors.add(specPrefix + ": modelPackage is required and cannot be empty");
        } else {
            // Validate package name format
            String packageName = specConfig.getModelPackage().get();
            if (!isValidJavaPackageName(packageName)) {
                errors.add(specPrefix + ": modelPackage is not a valid Java package name: " + packageName);
            }
        }
        
        // Validate optional fields
        if (specConfig.getOutputDir().isPresent()) {
            String outputDir = specConfig.getOutputDir().get();
            if (outputDir == null || outputDir.trim().isEmpty()) {
                errors.add(specPrefix + ": outputDir cannot be empty when specified");
            }
        }
        
        if (specConfig.getTemplateDir().isPresent()) {
            String templateDir = specConfig.getTemplateDir().get();
            if (templateDir != null && !templateDir.trim().isEmpty()) {
                File templateDirFile = new File(templateDir);
                if (!templateDirFile.exists()) {
                    errors.add(specPrefix + ": templateDir does not exist: " + templateDir);
                } else if (!templateDirFile.isDirectory()) {
                    errors.add(specPrefix + ": templateDir is not a directory: " + templateDir);
                }
            }
        }
        
        // Validate model name suffix
        if (specConfig.getModelNameSuffix().isPresent()) {
            String suffix = specConfig.getModelNameSuffix().get();
            if (suffix != null && !suffix.matches("[A-Za-z0-9_]*")) {
                errors.add(specPrefix + ": modelNameSuffix can only contain letters, numbers, and underscores: " + suffix);
            }
        }
        
        // Validate spec-specific config options
        if (specConfig.getConfigOptions().isPresent()) {
            validateConfigOptions(specConfig.getConfigOptions().get(), specPrefix, errors);
        }
    }
    
    /**
     * Validates config options for common mistakes and conflicts
     */
    private void validateConfigOptions(Map<String, String> configOptions, String context, List<String> errors) {
        // Check for common boolean value mistakes
        Set<String> booleanOptions = Set.of(
            "useBeanValidation", "performBeanValidation", "useSpringBoot3", "useJakartaEe", 
            "serializableModel", "hideGenerationTimestamp", "enumUnknownDefaultCase", 
            "generateBuilders", "legacyDiscriminatorBehavior", "disallowAdditionalPropertiesIfNotPresent",
            "useEnumCaseInsensitive", "openApiNullable", "generateConstructorWithAllArgs",
            "generatedConstructorWithRequiredArgs", "generateDefaultConstructor"
        );
        
        for (String option : booleanOptions) {
            if (configOptions.containsKey(option)) {
                String value = configOptions.get(option);
                if (value != null && !value.equals("true") && !value.equals("false")) {
                    errors.add(context + ": configOption '" + option + "' should be 'true' or 'false', got: " + value);
                }
            }
        }
        
        // Check for conflicting options
        if ("false".equals(configOptions.get("useSpringBoot3")) && "true".equals(configOptions.get("useJakartaEe"))) {
            errors.add(context + ": useJakartaEe=true requires useSpringBoot3=true (Spring Boot 3 uses Jakarta EE)");
        }
        
        // Validate date library options
        String dateLibrary = configOptions.get("dateLibrary");
        if (dateLibrary != null) {
            Set<String> validDateLibraries = Set.of("java8", "java8-localdatetime", "legacy", "joda");
            if (!validDateLibraries.contains(dateLibrary)) {
                errors.add(context + ": dateLibrary must be one of " + validDateLibraries + ", got: " + dateLibrary);
            }
        }
    }
    
    /**
     * Validates that spec names are unique (case-insensitive)
     */
    private void validateSpecNames(Map<String, SpecConfig> specs, List<String> errors) {
        Set<String> lowerCaseNames = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        
        for (String specName : specs.keySet()) {
            String lowerName = specName.toLowerCase();
            if (!lowerCaseNames.add(lowerName)) {
                duplicates.add(lowerName);
            }
        }
        
        if (!duplicates.isEmpty()) {
            errors.add("Duplicate spec names found (case-insensitive): " + duplicates);
        }
    }
    
    /**
     * Validates Java package name format
     */
    private boolean isValidJavaPackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        
        // Check for valid package name pattern
        String[] parts = packageName.split("\\.");
        for (String part : parts) {
            if (part.isEmpty() || !part.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                return false;
            }
            
            // Check for Java reserved words
            if (isJavaReservedWord(part)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if a word is a Java reserved word
     */
    private boolean isJavaReservedWord(String word) {
        Set<String> reservedWords = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null"
        );
        
        return reservedWords.contains(word.toLowerCase());
    }
    
    /**
     * Gets a fingerprint of plugin templates for the given generator to track changes
     * without extracting templates during configuration
     */
    private String getPluginTemplateFingerprint(String generatorName) {
        try {
            // Use a stable fingerprint based on plugin version and known template structure
            // This avoids expensive discovery during configuration phase and potential inconsistencies
            String pluginVersion = getPluginVersion();
            
            // Include generator name and plugin version for basic fingerprinting
            // The actual template discovery and content validation happens during execution
            String resourceFingerprint = "selective-templates:" + generatorName + ":" + pluginVersion;
            
            // Add a hash to ensure uniqueness and detect plugin changes
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(resourceFingerprint.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple version-based fingerprint
            return "selective-templates:" + generatorName + ":" + getPluginVersion();
        }
    }
    
    /**
     * Creates generation tasks for each configured OpenAPI specification.
     * 
     * <p>For each spec in the extension configuration:</p>
     * <ul>
     *   <li>Creates a spec-specific generation task (e.g., pets)</li>
     *   <li>Configures incremental build support with input/output tracking</li>
     *   <li>Sets up template resolution hierarchy (user > plugin > defaults)</li>
     *   <li>Applies configuration inheritance (defaults + spec overrides)</li>
     * </ul>
     * 
     * <p>Also creates an aggregate task 'generateOpenApiDtosAll' that depends on all spec tasks.</p>
     * 
     * @param project the Gradle project
     * @param extension the plugin extension containing spec configurations
     */
    private void createTasksForSpecs(Project project, OpenApiModelGenExtension extension) {
        TaskContainer tasks = project.getTasks();
        
        // Create individual tasks for each spec
        extension.getSpecs().forEach((specName, specConfig) -> {
            String taskName = "generate" + capitalize(specName);
            
            TaskProvider<GenerateTask> specTask = tasks.register(taskName, GenerateTask.class, task -> {
                configureGenerateTask(task, extension, specConfig, specName);
                configureIncrementalBuild(task, project, specConfig);
            });
        });
        
        // Create aggregate task that runs all spec tasks
        if (!extension.getSpecs().isEmpty()) {
            TaskProvider<Task> aggregateTask = tasks.register("generateAllModels", task -> {
                task.setDescription("Generates models for all OpenAPI specifications");
                task.setGroup("openapi modelgen");
                
                // Make this task depend on all spec tasks
                extension.getSpecs().keySet().forEach(specName -> {
                    String specTaskName = "generate" + capitalize(specName);
                    task.dependsOn(specTaskName);
                });
            });
        }
        
        // Create help task with usage information
        tasks.register("generateHelp", task -> {
            task.setDescription("Shows usage information and examples for the OpenAPI Model Generator plugin");
            task.setGroup("openapi modelgen");
            task.doLast(t -> {
                System.out.println("\n=== OpenAPI Model Generator Plugin ===\n");
                System.out.println("This plugin generates Java DTOs from OpenAPI specifications with Lombok support.\n");
                
                System.out.println("Available Tasks:");
                extension.getSpecs().forEach((specName, config) -> {
                    String taskName = "generate" + capitalize(specName);
                    System.out.println("  " + taskName + " - Generate DTOs for " + specName + " specification");
                });
                if (!extension.getSpecs().isEmpty()) {
                    System.out.println("  generateAllModels - Generate DTOs for all specifications");
                }
                System.out.println("  generateHelp - Show this help information\n");
                
                System.out.println("Configuration Example:");
                System.out.println("openapiModelgen {");
                System.out.println("    defaults {");
                System.out.println("        outputDir = \"build/generated-sources/openapi\"");
                System.out.println("        modelNameSuffix = \"Dto\"");
                System.out.println("        validateSpec = true");
                System.out.println("    }");
                System.out.println("    specs {");
                System.out.println("        pets {");
                System.out.println("            inputSpec = \"src/main/resources/openapi-spec/pets.yaml\"");
                System.out.println("            modelPackage = \"com.example.model.pets\"");
                System.out.println("        }");
                System.out.println("    }");
                System.out.println("}\n");
                
                System.out.println("Command Line Options:");
                System.out.println("  --model-package=<package>     Override model package");
                System.out.println("  --output-dir=<directory>      Override output directory");
                System.out.println("  --model-name-suffix=<suffix>  Override model name suffix");
                System.out.println("  --validate-spec               Enable specification validation");
                System.out.println("  --template-dir=<directory>    Override template directory\n");
                
                System.out.println("For detailed documentation, run: ./gradlew generatePluginDocs");
            });
        });
    }
    
    private void configureGenerateTask(GenerateTask task, OpenApiModelGenExtension extension, 
                                     SpecConfig specConfig, String specName) {
        task.setDescription("Generate models for " + specName + " OpenAPI specification");
        task.setGroup("openapi modelgen");
        
        // Apply plugin defaults
        applyPluginDefaults(task);
        
        // Apply user defaults
        applyUserDefaults(task, extension.getDefaults());
        
        // Apply spec-specific configuration
        applySpecConfig(task, extension, specConfig, specName);
    }
    
    private void applyPluginDefaults(GenerateTask task) {
        // Built-in sensible defaults for model-only generation
        task.getGeneratorName().set("spring");
        task.getOutputDir().set(task.getProject().getLayout().getBuildDirectory().dir("generated").get().getAsFile().getAbsolutePath());
        task.getModelNameSuffix().set("Dto");
        task.getGenerateModelTests().set(false);
        task.getGenerateApiTests().set(false);
        task.getGenerateApiDocumentation().set(false);
        task.getGenerateModelDocumentation().set(false);
        task.getValidateSpec().set(false);
        
        // Only generate models
        Map<String, String> globalProps = new HashMap<>();
        globalProps.put("models", "");
        task.getGlobalProperties().set(globalProps);
        
        // Spring/Jakarta defaults
        Map<String, String> configOpts = new HashMap<>();
        configOpts.put("useSpringBoot3", "true");
        configOpts.put("useJakartaEe", "true");
        configOpts.put("useBeanValidation", "true");
        configOpts.put("dateLibrary", "java8");
        configOpts.put("serializableModel", "true");
        configOpts.put("hideGenerationTimestamp", "true");
        configOpts.put("performBeanValidation", "true");
        configOpts.put("enumUnknownDefaultCase", "true");
        configOpts.put("generateBuilders", "true");
        configOpts.put("legacyDiscriminatorBehavior", "false");
        configOpts.put("disallowAdditionalPropertiesIfNotPresent", "false");
        configOpts.put("useEnumCaseInsensitive", "true");
        configOpts.put("openApiNullable", "false");
        task.getConfigOptions().set(configOpts);
        
        // Add template variables accessible in Mustache templates
        Map<String, Object> additionalProps = new HashMap<>();
        additionalProps.put("currentYear", String.valueOf(java.time.Year.now().getValue()));
        additionalProps.put("generatedBy", "OpenAPI Model Generator Plugin");
        additionalProps.put("pluginVersion", getPluginVersion());
        task.getAdditionalProperties().set(additionalProps);
    }
    
    private void applyUserDefaults(GenerateTask task, DefaultConfig defaults) {
        // Apply user-defined defaults that override plugin defaults
        if (defaults.getOutputDir().isPresent()) {
            task.getOutputDir().set(defaults.getOutputDir().get());
        }
        if (defaults.getModelNameSuffix().isPresent()) {
            task.getModelNameSuffix().set(defaults.getModelNameSuffix().get());
        }
        if (defaults.getGenerateModelTests().isPresent()) {
            task.getGenerateModelTests().set(defaults.getGenerateModelTests().get());
        }
        if (defaults.getGenerateApiTests().isPresent()) {
            task.getGenerateApiTests().set(defaults.getGenerateApiTests().get());
        }
        if (defaults.getGenerateApiDocumentation().isPresent()) {
            task.getGenerateApiDocumentation().set(defaults.getGenerateApiDocumentation().get());
        }
        if (defaults.getGenerateModelDocumentation().isPresent()) {
            task.getGenerateModelDocumentation().set(defaults.getGenerateModelDocumentation().get());
        }
        if (defaults.getValidateSpec().isPresent()) {
            task.getValidateSpec().set(defaults.getValidateSpec().get());
        }
        
        // Merge config options
        if (defaults.getConfigOptions().isPresent()) {
            Map<String, String> mergedOptions = new HashMap<>(task.getConfigOptions().get());
            mergedOptions.putAll(defaults.getConfigOptions().get());
            task.getConfigOptions().set(mergedOptions);
        }
        
        // Merge global properties  
        if (defaults.getGlobalProperties().isPresent()) {
            Map<String, String> mergedGlobal = new HashMap<>(task.getGlobalProperties().get());
            mergedGlobal.putAll(defaults.getGlobalProperties().get());
            task.getGlobalProperties().set(mergedGlobal);
        }
        
        // Merge additional properties and template variables
        Map<String, Object> mergedAdditionalProps = new HashMap<>(task.getAdditionalProperties().get());
        
        // Apply user-defined template variables (can override plugin defaults)
        if (defaults.getTemplateVariables().isPresent()) {
            mergedAdditionalProps.putAll(defaults.getTemplateVariables().get());
        }
        
        task.getAdditionalProperties().set(mergedAdditionalProps);
    }
    
    private void applySpecConfig(GenerateTask task, OpenApiModelGenExtension extension, SpecConfig specConfig, String specName) {
        // Required spec configuration (validation already done in validateExtensionConfiguration)
        task.getInputSpec().set(specConfig.getInputSpec().get());
        task.getModelPackage().set(specConfig.getModelPackage().get());
        
        // Optional overrides
        if (specConfig.getModelNameSuffix().isPresent()) {
            task.getModelNameSuffix().set(specConfig.getModelNameSuffix().get());
        }
        if (specConfig.getOutputDir().isPresent()) {
            task.getOutputDir().set(specConfig.getOutputDir().get());
        }
        if (specConfig.getValidateSpec().isPresent()) {
            task.getValidateSpec().set(specConfig.getValidateSpec().get());
        }
        if (specConfig.getGenerateModelTests().isPresent()) {
            task.getGenerateModelTests().set(specConfig.getGenerateModelTests().get());
        }
        if (specConfig.getGenerateApiTests().isPresent()) {
            task.getGenerateApiTests().set(specConfig.getGenerateApiTests().get());
        }
        if (specConfig.getGenerateApiDocumentation().isPresent()) {
            task.getGenerateApiDocumentation().set(specConfig.getGenerateApiDocumentation().get());
        }
        if (specConfig.getGenerateModelDocumentation().isPresent()) {
            task.getGenerateModelDocumentation().set(specConfig.getGenerateModelDocumentation().get());
        }
        
        // Apply template directory resolution lazily at execution time
        String userTemplateDir = specConfig.getTemplateDir().isPresent() ? 
            specConfig.getTemplateDir().get() : 
            (extension.getDefaults().getTemplateDir().isPresent() ? extension.getDefaults().getTemplateDir().get() : null);
        
        // Resolve template directory during configuration with immediate extraction if needed
        String resolvedTemplateDir = resolveTemplateDir(task.getProject(), userTemplateDir, task.getGeneratorName().get());
        if (resolvedTemplateDir != null) {
            task.getTemplateDir().set(resolvedTemplateDir);
        }
        // If resolvedTemplateDir is null, templateDir remains unset, which lets OpenAPI generator use defaults
        
        // Merge spec-specific config options
        if (specConfig.getConfigOptions().isPresent()) {
            Map<String, String> mergedOptions = new HashMap<>(task.getConfigOptions().get());
            mergedOptions.putAll(specConfig.getConfigOptions().get());
            task.getConfigOptions().set(mergedOptions);
        }
        
        // Merge spec-specific global properties
        if (specConfig.getGlobalProperties().isPresent()) {
            Map<String, String> mergedGlobal = new HashMap<>(task.getGlobalProperties().get());
            mergedGlobal.putAll(specConfig.getGlobalProperties().get());
            task.getGlobalProperties().set(mergedGlobal);
        }
        
        // Apply spec-specific template variables (highest precedence)
        Map<String, Object> finalAdditionalProps = new HashMap<>(task.getAdditionalProperties().get());
        
        // Spec-specific template variables override all others
        if (specConfig.getTemplateVariables().isPresent()) {
            finalAdditionalProps.putAll(specConfig.getTemplateVariables().get());
        }
        
        // Expand nested template variables recursively (only for String values)
        finalAdditionalProps = expandTemplateVariables(finalAdditionalProps);
        
        task.getAdditionalProperties().set(finalAdditionalProps);
    }
    
    /**
     * Expands template variables recursively to support nested variables like:
     * copyright = "Copyright (c) {{currentYear}} MyCompany Inc."
     * currentYear = "2025"
     * Result: copyright = "Copyright (c) 2025 MyCompany Inc."
     */
    private Map<String, Object> expandTemplateVariables(Map<String, Object> variables) {
        Map<String, Object> expanded = new HashMap<>();
        int maxIterations = 10; // Prevent infinite loops
        
        // Copy all variables first
        expanded.putAll(variables);
        
        // Create a string-only view for template expansion
        Map<String, String> stringVariables = new HashMap<>();
        for (Map.Entry<String, Object> entry : expanded.entrySet()) {
            if (entry.getValue() instanceof String) {
                stringVariables.put(entry.getKey(), (String) entry.getValue());
            }
        }
        
        // Perform multiple passes to handle nested expansions
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            boolean hasExpansions = false;
            Map<String, String> currentPass = new HashMap<>(stringVariables);
            
            for (Map.Entry<String, String> entry : currentPass.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String expandedValue = expandSingleVariable(value, stringVariables);
                
                if (!value.equals(expandedValue)) {
                    stringVariables.put(key, expandedValue);
                    expanded.put(key, expandedValue);
                    hasExpansions = true;
                }
            }
            
            // If no expansions happened this pass, we're done
            if (!hasExpansions) {
                break;
            }
        }
        
        return expanded;
    }
    
    /**
     * Expands a single variable value by replacing {{variable}} patterns
     */
    private String expandSingleVariable(String value, Map<String, String> variables) {
        if (value == null) {
            return null;
        }
        
        String result = value;
        
        // Find all {{variableName}} patterns and replace them
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(value);
        
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            String variableValue = variables.get(variableName);
            
            if (variableValue != null) {
                // Replace this occurrence
                String placeholder = "{{" + matcher.group(1) + "}}";
                result = result.replace(placeholder, variableValue);
            }
        }
        
        return result;
    }
    
    /**
     * Detects and logs the OpenAPI Generator version being used.
     * The plugin includes a default version but respects overrides from configuration plugins.
     */
    private void ensureOpenApiGeneratorAvailable(Project project) {
        try {
            String detectedVersion = detectOpenApiGeneratorVersion(project);
            
            if (detectedVersion != null && !"unknown".equals(detectedVersion)) {
                project.getLogger().info("Using OpenAPI Generator version: {}", detectedVersion);
                validateVersionCompatibility(detectedVersion, project);
            } else {
                project.getLogger().info("Using OpenAPI Generator (version detection unavailable)");
            }
            
        } catch (Exception e) {
            project.getLogger().debug("Could not detect OpenAPI Generator version: {}", e.getMessage());
        }
    }
    
    /**
     * Detects the OpenAPI Generator version from project dependencies
     */
    private String detectOpenApiGeneratorVersion(Project project) {
        try {
            // Check if OpenAPI Generator classes are available
            Class.forName("org.openapitools.generator.gradle.plugin.OpenApiGeneratorPlugin");
            
            // Try to find version from dependencies
            return project.getConfigurations().stream()
                .filter(config -> config.isCanBeResolved())
                .flatMap(config -> {
                    try {
                        return config.getAllDependencies().stream();
                    } catch (Exception e) {
                        return Stream.empty();
                    }
                })
                .filter(dep -> "org.openapitools".equals(dep.getGroup()) && 
                             "openapi-generator-gradle-plugin".equals(dep.getName()))
                .map(dep -> dep.getVersion())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(detectVersionFromClasspath());
                
        } catch (ClassNotFoundException e) {
            return null; // OpenAPI Generator not available
        } catch (Exception e) {
            project.getLogger().debug("Could not detect OpenAPI Generator version: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Attempts to detect version from JAR manifest in classpath
     */
    private String detectVersionFromClasspath() {
        try {
            Class<?> generatorClass = Class.forName("org.openapitools.generator.gradle.plugin.OpenApiGeneratorPlugin");
            String classPath = generatorClass.getProtectionDomain().getCodeSource().getLocation().getPath();
            
            // Extract version from JAR filename if possible
            if (classPath.contains("openapi-generator-gradle-plugin")) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("openapi-generator-gradle-plugin-([0-9]+\\.[0-9]+\\.[0-9]+)");
                java.util.regex.Matcher matcher = pattern.matcher(classPath);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            
            return "unknown"; // Present but version unknown
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Validates version compatibility and logs warnings for untested versions
     */
    private void validateVersionCompatibility(String version, Project project) {
        if ("unknown".equals(version)) {
            project.getLogger().info("OpenAPI Generator version unknown - plugin tested with 7.14.0+");
            return;
        }
        
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            
            // Check minimum supported version (7.10.0)
            if (major < 7 || (major == 7 && minor < 10)) {
                project.getLogger().warn("OpenAPI Generator {} may not be compatible. Plugin requires 7.10.0+", version);
            } else if (major > 7) {
                project.getLogger().info("OpenAPI Generator {} detected. Plugin tested with 7.14.0 - newer versions should work", version);
            } else {
                project.getLogger().debug("OpenAPI Generator {} is compatible", version);
            }
            
        } catch (Exception e) {
            project.getLogger().info("Could not parse version {}, assuming compatible", version);
        }
    }
    
    
    /**
     * Configures incremental build support for OpenAPI spec file changes
     */
    private void configureIncrementalBuild(GenerateTask task, Project project, SpecConfig specConfig) {
        // Configure inputs: OpenAPI spec file
        if (specConfig.getInputSpec().isPresent()) {
            String inputSpecPath = specConfig.getInputSpec().get();
            File inputSpecFile = project.file(inputSpecPath);
            task.getInputs().file(inputSpecFile)
                .withPropertyName("openApiSpecFile")
                .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE);
        }
        
        // Configure inputs: Template directory tracking without early resolution
        // Track user template directory if specified
        String userTemplateDir = specConfig.getTemplateDir().isPresent() ? 
            specConfig.getTemplateDir().get() : 
            (project.getExtensions().getByType(OpenApiModelGenExtension.class).getDefaults().getTemplateDir().isPresent() ? 
                project.getExtensions().getByType(OpenApiModelGenExtension.class).getDefaults().getTemplateDir().get() : null);
        
        if (userTemplateDir != null) {
            File userTemplateDirFile = new File(userTemplateDir);
            if (userTemplateDirFile.exists() && userTemplateDirFile.isDirectory()) {
                task.getInputs().dir(userTemplateDirFile)
                    .withPropertyName("userTemplateDir")
                    .withPathSensitivity(PathSensitivity.RELATIVE);
            }
        }
        
        // === @Input PROPERTIES (affect output - trigger rebuild when changed) ===
        
        // Core generation settings that affect output
        String generatorName = task.getGeneratorName().getOrElse("spring");
        task.getInputs().property("generatorName", generatorName);
        task.getInputs().property("modelNameSuffix", task.getModelNameSuffix().getOrElse(""));
        task.getInputs().property("validateSpec", task.getValidateSpec().getOrElse(false));  // Keep as @Input for future functionality
        
        // Plugin template files
        // Register plugin template fingerprint for incremental build detection
        task.getInputs().property("generatorTemplatesFingerprint", getPluginTemplateFingerprint(generatorName));
        
        // Configuration that affects generated code
        task.getInputs().property("configOptions", task.getConfigOptions().getOrElse(java.util.Collections.emptyMap()));
        task.getInputs().property("globalProperties", task.getGlobalProperties().getOrElse(java.util.Collections.emptyMap()));
        
        // Template variables that are included in generated code
        Map<String, Object> additionalProps = task.getAdditionalProperties().getOrElse(java.util.Collections.emptyMap());
        Map<String, Object> outputAffectingProps = extractOutputAffectingProperties(additionalProps);
        task.getInputs().property("templateVariables", outputAffectingProps);
        
        // === CONFIGURE @Internal PROPERTIES (for logging/debugging - don't affect up-to-date) ===
        configureInternalProperties(task, project, additionalProps);
        
        // Configure caching (GenerateTask already declares outputDir as output)
        task.getOutputs().cacheIf("OpenAPI specs are deterministic", t -> true);
        
        // Add description for task
        task.setDescription("Generates DTOs from OpenAPI specification with incremental build support");
    }
    
    
    /**
     * Extracts properties that actually affect the generated output from template variables
     */
    private Map<String, Object> extractOutputAffectingProperties(Map<String, Object> allProps) {
        Map<String, Object> outputAffecting = new HashMap<>();
        
        // Properties that appear in generated code and affect output
        Set<String> outputAffectingKeys = Set.of(
            "companyName",           // Used in copyright headers
            "shortCopyright",        // Used in generated files  
            "fullCopyright",         // Used in generated files
            "header",                // Used in generated files
            "currentYear"            // Used in copyright (but auto-generated, so changes are expected)
        );
        
        for (Map.Entry<String, Object> entry : allProps.entrySet()) {
            String key = entry.getKey();
            // Only include properties that affect output - exclude debug/internal properties
            if (outputAffectingKeys.contains(key) && !isInternalProperty(key)) {
                outputAffecting.put(key, entry.getValue());
            }
        }
        
        return outputAffecting;
    }
    
    /**
     * Determines if a property is internal (doesn't affect output) based on naming patterns
     */
    private boolean isInternalProperty(String propertyName) {
        return propertyName.startsWith("debug") || 
               propertyName.startsWith("internal") ||
               propertyName.equals("generatedBy") ||
               propertyName.equals("pluginVersion") ||
               propertyName.equals("buildTimestamp") ||
               propertyName.contains("BuildTime") ||
               propertyName.contains("Timestamp");
    }
    
    /**
     * Configure @Internal properties that are used for execution but don't affect output.
     * These properties won't cause the task to be considered out-of-date when they change.
     */
    private void configureInternalProperties(GenerateTask task, Project project, Map<String, Object> additionalProps) {
        // Store internal properties in task extensions for access during execution
        task.getExtensions().create("internal", InternalTaskProperties.class);
        InternalTaskProperties internal = task.getExtensions().getByType(InternalTaskProperties.class);
        
        // Plugin metadata (doesn't affect generated code)
        internal.pluginVersion = getPluginVersion();
        internal.buildTimestamp = String.valueOf(System.currentTimeMillis());
        internal.templateExtractionEnabled = true;
        
        // Debug/performance info (doesn't affect output)
        internal.debugLogging = project.getLogger().isDebugEnabled();
        internal.projectName = project.getName();
        internal.gradleVersion = project.getGradle().getGradleVersion();
        
        // Template extraction metadata (location doesn't affect output, only content does)
        internal.templateCacheDirectory = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "plugin-templates").getAbsolutePath();
        
        // Non-output-affecting template variables for debugging
        Map<String, Object> debugProps = new HashMap<>();
        for (Map.Entry<String, Object> entry : additionalProps.entrySet()) {
            String key = entry.getKey();
            // Store debug/internal variables that don't affect generated code
            if (key.startsWith("debug") || key.equals("generatedBy") || key.equals("pluginVersion")) {
                debugProps.put(key, entry.getValue());
            }
        }
        internal.debugProperties = debugProps;
        
        // Add action to log internal properties if debug is enabled
        task.doFirst("logInternalProperties", t -> {
            if (internal.debugLogging) {
                project.getLogger().debug("Plugin version: {}", internal.pluginVersion);
                project.getLogger().debug("Template cache: {}", internal.templateCacheDirectory);
                project.getLogger().debug("Debug properties: {}", internal.debugProperties);
            }
        });
    }
    
    /**
     * Container for @Internal properties that don't affect task up-to-date checks
     */
    public static class InternalTaskProperties {
        public String pluginVersion;
        public String buildTimestamp;
        public boolean templateExtractionEnabled;
        public boolean debugLogging;
        public String projectName;
        public String gradleVersion;
        public String templateCacheDirectory;
        public Map<String, Object> debugProperties;
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Determines if plugin templates should be used without extracting them during provider evaluation.
     * This method checks for available templates without side effects to maintain incremental build compatibility.
     */
    private boolean shouldUsePluginTemplates(Project project, String generatorName, File pluginTemplateDir) {
        // Only return true if the directory already exists (templates have been extracted)
        // This avoids the "directory doesn't exist" validation error
        if (!pluginTemplateDir.exists()) {
            return false;
        }
        
        // Check if templates are already cached and valid
        File cacheFile = new File(pluginTemplateDir, ".template-cache");
        String currentPluginVersion = getPluginVersion();
        String expectedCacheKey = computeSelectiveTemplateCacheKey(generatorName, currentPluginVersion);
        
        return isTemplateCacheValidWithHashes(cacheFile, expectedCacheKey, pluginTemplateDir);
    }
    
    /**
     * Resolves template directory with the following precedence:
     * 1. User-provided template directory (external)
     * 2. Plugin-provided templates (plugin/src/main/resources/templates/<generator_name>)
     * 3. OpenAPI generator default templates (no templateDir set)
     * 
     * This method is called lazily at task execution time to avoid expensive 
     * template extraction during configuration phase.
     */
    private String resolveTemplateDir(Project project, String userTemplateDir, String generatorName) {
        // 1. User-provided template directory has highest precedence
        if (userTemplateDir != null && !userTemplateDir.trim().isEmpty()) {
            File userTemplateFile = new File(userTemplateDir);
            if (userTemplateFile.exists() && userTemplateFile.isDirectory()) {
                return userTemplateDir;
            }
            // If user specified a template directory but it doesn't exist, warn but continue with fallback
            project.getLogger().warn("User-specified template directory does not exist: {}. Falling back to plugin templates.", userTemplateDir);
        }
        
        // 2. Check for plugin-provided templates (selective extraction)
        File pluginTemplateDir = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "plugin-templates/" + generatorName);
        if (extractSelectivePluginTemplates(project, generatorName, pluginTemplateDir)) {
            project.getLogger().debug("Selective template extraction completed: {}", pluginTemplateDir.getAbsolutePath());
            return pluginTemplateDir.getAbsolutePath();
        }
        
        // 3. Fall back to OpenAPI generator default templates (return null to let generator use defaults)
        return null;
    }
    
    /**
     * Extracts only the plugin templates we actually have, allowing generator to use its own defaults for missing templates
     */
    private boolean extractSelectivePluginTemplates(Project project, String generatorName, File targetDir) {
        String resourcePath = "/templates/" + generatorName;
        
        // Dynamically discover what templates our plugin actually provides
        List<String> ourTemplates = discoverAvailablePluginTemplates(resourcePath);
        boolean anyTemplateExtracted = false;
        
        // Ensure target directory exists
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            project.getLogger().warn("Failed to create template directory: {}", targetDir.getAbsolutePath());
            return false;
        }
        
        // Check cache first
        File cacheFile = new File(targetDir, ".template-cache");
        String currentPluginVersion = getPluginVersion();
        String expectedCacheKey = computeSelectiveTemplateCacheKey(generatorName, currentPluginVersion);
        
        if (isTemplateCacheValidWithHashes(cacheFile, expectedCacheKey, targetDir)) {
            project.getLogger().debug("Using cached selective plugin templates from {}", targetDir.getAbsolutePath());
            // Cache is valid - templates are available for use
            return true;
        }
        
        // Clean existing templates before extraction
        try {
            cleanTemplateDirectory(targetDir, cacheFile);
        } catch (IOException e) {
            project.getLogger().warn("Failed to clean template directory: {}", e.getMessage());
        }
        
        // If no templates discovered, return early
        if (ourTemplates.isEmpty()) {
            project.getLogger().debug("No plugin templates discovered for generator '{}' - will use generator defaults", generatorName);
            return false;
        }
        
        project.getLogger().debug("Discovered {} plugin templates for generator '{}'", ourTemplates.size(), generatorName);
        
        // Extract only our templates that exist
        for (String templateName : ourTemplates) {
            String templateResourcePath = resourcePath + "/" + templateName;
            
            // Try multiple classloader strategies for this specific template
            InputStream templateStream = null;
            String accessMethod = null;
            
            // Strategy 1: Plugin classloader
            templateStream = getClass().getResourceAsStream(templateResourcePath);
            if (templateStream != null) {
                accessMethod = "plugin classloader";
            }
            
            // Strategy 2: Context classloader
            if (templateStream == null) {
                templateStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(templateResourcePath.substring(1));
                if (templateStream != null) {
                    accessMethod = "context classloader";
                }
            }
            
            // Strategy 3: System classloader  
            if (templateStream == null) {
                templateStream = ClassLoader.getSystemClassLoader().getResourceAsStream(templateResourcePath.substring(1));
                if (templateStream != null) {
                    accessMethod = "system classloader";
                }
            }
            
            if (templateStream != null) {
                try {
                    File targetFile = new File(targetDir, templateName);
                    Files.copy(templateStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    project.getLogger().debug("Extracted template {} using {}", templateName, accessMethod);
                    anyTemplateExtracted = true;
                } catch (IOException e) {
                    project.getLogger().warn("Failed to extract template {}: {}", templateName, e.getMessage());
                } finally {
                    try {
                        templateStream.close();
                    } catch (IOException ignored) {
                        // Ignore close errors
                    }
                }
            } else {
                // This is normal - we might not have all templates, and that's OK
                project.getLogger().debug("Template {} not found in plugin (will use generator default)", templateName);
            }
        }
        
        if (anyTemplateExtracted) {
            // Update cache with selective template extraction key
            try {
                updateTemplateCache(cacheFile, expectedCacheKey);
                project.getLogger().debug("Updated template cache with key: {}", expectedCacheKey);
                
                // Save template content hashes for change detection
                Map<String, String> templateHashes = new HashMap<>();
                try (Stream<Path> paths = Files.walk(targetDir.toPath())) {
                    for (Path templatePath : paths.filter(Files::isRegularFile)
                                                   .filter(path -> {
                                                       String fileName = path.getFileName().toString();
                                                       return fileName.endsWith(".mustache") || 
                                                              fileName.endsWith(".hbs") || 
                                                              fileName.endsWith(".handlebars");
                                                   })
                                                   .toArray(Path[]::new)) {
                        String relativePath = targetDir.toPath().relativize(templatePath).toString().replace(File.separatorChar, '/');
                        String hash = calculateFileContentHash(templatePath);
                        templateHashes.put(relativePath, hash);
                    }
                }
                storeTemplateHashes(targetDir, templateHashes);
                project.getLogger().debug("Saved template hashes for {} templates", templateHashes.size());
            } catch (IOException e) {
                project.getLogger().warn("Failed to update template cache: {}", e.getMessage());
            }
            long extractedCount = ourTemplates.stream()
                .mapToInt(name -> new File(targetDir, name).exists() ? 1 : 0)
                .sum();
            project.getLogger().info("Selectively extracted {} plugin templates to {}", 
                extractedCount, targetDir.getAbsolutePath());
            return true;
        } else {
            project.getLogger().debug("No plugin templates found for generator '{}' - will use generator defaults", generatorName);
            return false;
        }
    }
    
    /**
     * Dynamically discovers what templates are available in our plugin resources
     */
    private List<String> discoverAvailablePluginTemplates(String resourcePath) {
        List<String> templates = new ArrayList<>();
        
        // Try to get the resource URL using multiple strategies
        URL resourceUrl = null;
        
        // Strategy 1: Plugin classloader
        resourceUrl = getClass().getResource(resourcePath);
        
        // Strategy 2: Context classloader  
        if (resourceUrl == null) {
            resourceUrl = Thread.currentThread().getContextClassLoader().getResource(resourcePath.substring(1));
        }
        
        // Strategy 3: System classloader
        if (resourceUrl == null) {
            resourceUrl = ClassLoader.getSystemClassLoader().getResource(resourcePath.substring(1));
        }
        
        if (resourceUrl == null) {
            return templates; // Empty list if no templates found
        }
        
        try {
            URI resourceUri = resourceUrl.toURI();
            Path templateSourcePath;
            FileSystem fileSystem = null;
            boolean needToCloseFileSystem = false;
            
            try {
                if (resourceUri.getScheme().equals("jar")) {
                    try {
                        fileSystem = FileSystems.getFileSystem(resourceUri);
                    } catch (FileSystemNotFoundException e) {
                        fileSystem = FileSystems.newFileSystem(resourceUri, Collections.emptyMap());
                        needToCloseFileSystem = true;
                    }
                    templateSourcePath = fileSystem.getPath(resourcePath);
                } else {
                    templateSourcePath = Paths.get(resourceUri);
                }
                
                if (Files.exists(templateSourcePath) && Files.isDirectory(templateSourcePath)) {
                    try (Stream<Path> paths = Files.list(templateSourcePath)) {
                        templates = paths.filter(Files::isRegularFile)
                                        .filter(path -> path.toString().endsWith(".mustache") || 
                                                       path.toString().endsWith(".hbs") ||
                                                       path.toString().endsWith(".handlebars"))
                                        .map(path -> path.getFileName().toString())
                                        .collect(Collectors.toList());
                    }
                }
                
            } finally {
                if (needToCloseFileSystem && fileSystem != null) {
                    try {
                        fileSystem.close();
                    } catch (IOException ignored) {
                        // Ignore close errors
                    }
                }
            }
            
        } catch (URISyntaxException | IOException e) {
            // Return empty list if discovery fails - this is not an error condition
        }
        
        return templates;
    }
    
    /**
     * Extracts plugin templates from resources to the specified directory with caching optimization
     * @deprecated Use extractSelectivePluginTemplates for better forward compatibility
     */
    private boolean extractPluginTemplates(Project project, String resourcePath, File targetDir) {
        try {
            // Ensure target directory exists
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                project.getLogger().warn("Failed to create template directory: {}", targetDir.getAbsolutePath());
                return false;
            }

            // Check if templates are already extracted and up-to-date
            File cacheFile = new File(targetDir, ".template-cache");
            String currentPluginVersion = getPluginVersion();
            String expectedCacheKey = computeTemplateCacheKey(resourcePath, currentPluginVersion);
            
            if (isTemplateCacheValidWithHashes(cacheFile, expectedCacheKey, targetDir)) {
                project.getLogger().debug("Using cached plugin templates (content verified) from {}", targetDir.getAbsolutePath());
                return true;
            }

            // Get the resource URL with comprehensive module system compatibility
            URL resourceUrl = null;
            String accessMethod = null;
            
            // Strategy 1: Plugin classloader (standard approach)
            resourceUrl = getClass().getResource(resourcePath);
            if (resourceUrl != null) {
                accessMethod = "plugin classloader";
            }
            
            // Strategy 2: Thread context classloader (Spring Boot compatibility)
            if (resourceUrl == null) {
                resourceUrl = Thread.currentThread().getContextClassLoader().getResource(resourcePath.substring(1));
                if (resourceUrl != null) {
                    accessMethod = "context classloader";
                }
            }
            
            // Strategy 3: System classloader (module system fallback)
            if (resourceUrl == null) {
                resourceUrl = ClassLoader.getSystemClassLoader().getResource(resourcePath.substring(1));
                if (resourceUrl != null) {
                    accessMethod = "system classloader";
                }
            }
            
            // Strategy 4: Try without leading slash (alternative path format)
            if (resourceUrl == null && resourcePath.startsWith("/")) {
                String alternatePath = resourcePath.substring(1);
                resourceUrl = getClass().getClassLoader().getResource(alternatePath);
                if (resourceUrl != null) {
                    accessMethod = "alternate path format";
                }
            }
            
            if (resourceUrl == null) {
                project.getLogger().warn("Plugin templates not found at resource path: {}. This indicates a Java module system compatibility issue.", resourcePath);
                project.getLogger().warn("Common causes:");
                project.getLogger().warn("  - Spring Boot 3.x fat JAR nested classloader isolation");
                project.getLogger().warn("  - Java 9+ module system access restrictions");
                project.getLogger().warn("  - Plugin JAR not properly included in classpath");
                project.getLogger().warn("WORKAROUND: Specify templateDir in your configuration to use external templates:");
                project.getLogger().warn("  openapiModelgen {{ defaults {{ templateDir 'path/to/custom/templates' }} }}");
                return false;
            } else {
                project.getLogger().debug("Found plugin templates using: {} (path: {})", accessMethod, resourceUrl);
            }

            // Clean existing templates before extraction
            cleanTemplateDirectory(targetDir, cacheFile);

            URI resourceUri = resourceUrl.toURI();
            Path templateSourcePath;
            FileSystem fileSystem = null;
            boolean needToCloseFileSystem = false;

            try {
                if (resourceUri.getScheme().equals("jar")) {
                    // Running from JAR - need to create filesystem to access JAR contents
                    try {
                        fileSystem = FileSystems.getFileSystem(resourceUri);
                    } catch (FileSystemNotFoundException e) {
                        fileSystem = FileSystems.newFileSystem(resourceUri, Collections.emptyMap());
                        needToCloseFileSystem = true;
                    }
                    templateSourcePath = fileSystem.getPath(resourcePath);
                } else {
                    // Running from file system (e.g., during development)
                    templateSourcePath = Paths.get(resourceUri);
                }

                if (!Files.exists(templateSourcePath)) {
                    project.getLogger().debug("Template source path does not exist: {}", templateSourcePath);
                    return false;
                }

                // Extract templates with metadata tracking
                boolean extractionSuccessful = extractTemplatesWithMetadata(project, templateSourcePath, targetDir);
                
                if (extractionSuccessful) {
                    // Update cache metadata
                    updateTemplateCache(cacheFile, expectedCacheKey);
                    project.getLogger().info("Successfully extracted and cached plugin templates from {} to {}", resourcePath, targetDir.getAbsolutePath());
                    return true;
                }

                return false;

            } finally {
                if (needToCloseFileSystem && fileSystem != null) {
                    try {
                        fileSystem.close();
                    } catch (IOException e) {
                        project.getLogger().debug("Failed to close filesystem: {}", e.getMessage());
                    }
                }
            }

        } catch (URISyntaxException | IOException e) {
            project.getLogger().warn("Failed to extract plugin templates from {}: {}", resourcePath, e.getMessage());
            return false;
        }
    }

    /**
     * Computes a cache key based on plugin version and resource path
     */
    private String computeTemplateCacheKey(String resourcePath, String pluginVersion) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String input = resourcePath + ":" + pluginVersion;
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple string concatenation
            return resourcePath.replace("/", "_") + "_" + pluginVersion;
        }
    }

    /**
     * Computes a cache key for selective template extraction based on generator name and plugin version
     */
    private String computeSelectiveTemplateCacheKey(String generatorName, String pluginVersion) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String input = "selective-templates:" + generatorName + ":" + pluginVersion;
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple string concatenation
            return "selective_" + generatorName + "_" + pluginVersion;
        }
    }

    /**
     * Gets the plugin version for cache invalidation
     */
    private String getPluginVersion() {
        try {
            Properties props = new Properties();
            InputStream versionStream = getClass().getResourceAsStream("/plugin.properties");
            if (versionStream != null) {
                props.load(versionStream);
                return props.getProperty("version", "unknown");
            }
        } catch (IOException e) {
            // Ignore and use fallback
        }
        
        // Fallback: use package implementation version
        Package pkg = getClass().getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }
        
        // Last resort: use timestamp of the class file
        try {
            URL classUrl = getClass().getResource(getClass().getSimpleName() + ".class");
            if (classUrl != null) {
                return String.valueOf(classUrl.openConnection().getLastModified());
            }
        } catch (IOException e) {
            // Ignore
        }
        
        return "development";
    }

    /**
     * Checks if the template cache is valid
     */
    private boolean isTemplateCacheValid(File cacheFile, String expectedCacheKey, File targetDir) {
        if (!cacheFile.exists()) {
            return false;
        }

        try {
            Properties cacheProps = new Properties();
            try (FileInputStream fis = new FileInputStream(cacheFile)) {
                cacheProps.load(fis);
            }

            String cachedKey = cacheProps.getProperty("cache.key");
            String cachedTimestamp = cacheProps.getProperty("cache.timestamp");
            
            // Check if cache key matches
            if (!expectedCacheKey.equals(cachedKey)) {
                return false;
            }

            // Check if template files still exist
            String templateCount = cacheProps.getProperty("template.count", "0");
            try {
                int expectedCount = Integer.parseInt(templateCount);
                if (expectedCount == 0) {
                    return false;
                }
                
                // Quick check: count actual template files
                try (Stream<Path> paths = Files.walk(targetDir.toPath())) {
                    long actualCount = paths.filter(Files::isRegularFile)
                                          .filter(path -> {
                                              String fileName = path.getFileName().toString();
                                              return fileName.endsWith(".mustache") || 
                                                     fileName.endsWith(".hbs") || 
                                                     fileName.endsWith(".handlebars");
                                          })
                                          .count();
                    
                    return actualCount == expectedCount;
                }
                
            } catch (NumberFormatException e) {
                return false;
            }

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Cleans the template directory but preserves the cache file
     */
    private void cleanTemplateDirectory(File targetDir, File cacheFile) throws IOException {
        if (!targetDir.exists()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(targetDir.toPath())) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> !path.equals(cacheFile.toPath()))
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                     } catch (IOException e) {
                         // Log but don't fail the whole operation
                     }
                 });
        }
    }

    /**
     * Extracts templates and tracks metadata for caching with parallel processing and content hash validation
     */
    private boolean extractTemplatesWithMetadata(Project project, Path templateSourcePath, File targetDir) throws IOException {
        // Collect all template files first
        List<Path> templateFiles;
        try (Stream<Path> paths = Files.walk(templateSourcePath)) {
            templateFiles = paths.filter(Files::isRegularFile)
                                 .filter(path -> path.toString().endsWith(".mustache") || 
                                                path.toString().endsWith(".hbs") ||
                                                path.toString().endsWith(".handlebars"))
                                 .collect(java.util.stream.Collectors.toList());
        }
        
        if (templateFiles.isEmpty()) {
            return false;
        }
        
        project.getLogger().debug("Starting parallel extraction of {} template files", templateFiles.size());
        
        // Determine optimal thread count based on template count and available processors
        int optimalThreadCount = determineOptimalThreadCount(templateFiles.size());
        
        // Thread-safe collections for results
        Map<String, String> templateHashes = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger changeCount = new AtomicInteger(0);
        
        // Create thread pool for parallel processing
        ExecutorService executorService = Executors.newFixedThreadPool(optimalThreadCount);
        
        try {
            // Pre-create all necessary parent directories to avoid race conditions
            preCreateDirectories(templateFiles, templateSourcePath, targetDir);
            
            // Submit all template extraction tasks
            List<Future<Void>> futures = new ArrayList<>();
            
            for (Path sourcePath : templateFiles) {
                Future<Void> future = executorService.submit(() -> {
                    try {
                        extractSingleTemplate(project, templateSourcePath, targetDir, sourcePath, 
                                             templateHashes, successCount, changeCount);
                    } catch (Exception e) {
                        project.getLogger().warn("Failed to extract template file {}: {}", sourcePath, e.getMessage());
                    }
                    return null;
                });
                futures.add(future);
            }
            
            // Wait for all tasks to complete
            for (Future<Void> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS); // Timeout per template
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    project.getLogger().warn("Template extraction task failed or timed out: {}", e.getMessage());
                }
            }
            
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        int totalSuccess = successCount.get();
        int totalChanges = changeCount.get();
        
        project.getLogger().info("Parallel template extraction completed: {} templates processed, {} changed, {} threads used", 
                                totalSuccess, totalChanges, optimalThreadCount);
        
        // Store template hashes in cache for future validation
        storeTemplateHashes(targetDir, templateHashes);
        
        return totalSuccess > 0;
    }
    
    /**
     * Determines optimal thread count for template extraction based on file count and system resources
     */
    private int determineOptimalThreadCount(int templateFileCount) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        
        // For small template sets, don't over-parallelize
        if (templateFileCount <= 3) {
            return 1;
        } else if (templateFileCount <= 8) {
            return Math.min(2, availableProcessors);
        } else {
            // For larger sets, use more threads but cap at reasonable limits
            int optimalThreads = Math.min(templateFileCount / 2, availableProcessors);
            return Math.max(2, Math.min(optimalThreads, 8)); // Cap at 8 threads max
        }
    }
    
    /**
     * Pre-creates all necessary parent directories to avoid race conditions during parallel extraction
     */
    private void preCreateDirectories(List<Path> templateFiles, Path templateSourcePath, File targetDir) throws IOException {
        Set<File> dirsToCreate = new HashSet<>();
        
        for (Path sourcePath : templateFiles) {
            Path relativePath = templateSourcePath.relativize(sourcePath);
            File targetFile = new File(targetDir, relativePath.toString().replace('/', File.separatorChar));
            File parentDir = targetFile.getParentFile();
            
            if (parentDir != null) {
                dirsToCreate.add(parentDir);
            }
        }
        
        // Create all directories in batch
        for (File dir : dirsToCreate) {
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }
    
    /**
     * Extracts a single template file with thread-safe hash tracking
     */
    private void extractSingleTemplate(Project project, Path templateSourcePath, File targetDir, 
                                     Path sourcePath, Map<String, String> templateHashes, 
                                     AtomicInteger successCount, AtomicInteger changeCount) throws IOException {
        
        // Calculate relative path from template source
        Path relativePath = templateSourcePath.relativize(sourcePath);
        File targetFile = new File(targetDir, relativePath.toString().replace('/', File.separatorChar));
        String relativePathStr = relativePath.toString().replace(File.separatorChar, '/');
        
        // Calculate source content hash for tracking
        String sourceHash = calculateFileContentHash(sourcePath);
        templateHashes.put(relativePathStr, sourceHash); // ConcurrentHashMap is thread-safe
        
        // Copy the template file only if content has changed
        boolean contentChanged = shouldCopyTemplate(sourcePath, targetFile);
        if (contentChanged) {
            copyTemplateFile(sourcePath, targetFile);
            changeCount.incrementAndGet();
            project.getLogger().debug("Extracted template (content changed): {} -> {}", sourcePath, targetFile.getAbsolutePath());
        } else {
            project.getLogger().debug("Template content unchanged, skipping: {} (hash: {})", targetFile.getName(), sourceHash.substring(0, 8) + "...");
        }
        
        successCount.incrementAndGet();
    }
    
    /**
     * Thread-safe template file copying with proper resource management
     */
    private void copyTemplateFile(Path sourcePath, File targetFile) throws IOException {
        try (InputStream inputStream = Files.newInputStream(sourcePath);
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * Determines if a template should be copied based on content hash comparison
     */
    private boolean shouldCopyTemplate(Path sourcePath, File targetFile) throws IOException {
        if (!targetFile.exists()) {
            return true;
        }
        
        // Compare content hashes instead of size/bytes - more reliable for all file sizes
        try {
            String sourceHash = calculateFileContentHash(sourcePath);
            String targetHash = calculateFileContentHash(targetFile.toPath());
            return !sourceHash.equals(targetHash);
        } catch (IOException e) {
            // If we can't calculate hashes, assume we should copy
            return true;
        }
    }
    
    /**
     * Calculates SHA-256 content hash for a file
     */
    private String calculateFileContentHash(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Use streaming approach for large files to avoid memory issues
            try (InputStream is = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            // Convert hash to hex string
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (NoSuchAlgorithmException e) {
            // Fallback to MD5 if SHA-256 is not available (shouldn't happen)
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] fileBytes = Files.readAllBytes(filePath);
                byte[] hashBytes = md5.digest(fileBytes);
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) {
                    sb.append(String.format("%02x", b));
                }
                return "md5:" + sb.toString();
            } catch (NoSuchAlgorithmException ex) {
                // Last resort: use file size + timestamp as "hash"
                BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                return "fallback:" + attrs.size() + ":" + attrs.lastModifiedTime().toMillis();
            }
        }
    }

    /**
     * Stores template content hashes in a separate cache file for validation
     */
    private void storeTemplateHashes(File targetDir, Map<String, String> templateHashes) throws IOException {
        File hashCacheFile = new File(targetDir, ".template-hashes");
        Properties hashProps = new Properties();
        
        // Store each template's content hash
        for (Map.Entry<String, String> entry : templateHashes.entrySet()) {
            hashProps.setProperty("hash." + entry.getKey(), entry.getValue());
        }
        
        hashProps.setProperty("hash.timestamp", String.valueOf(System.currentTimeMillis()));
        hashProps.setProperty("hash.count", String.valueOf(templateHashes.size()));
        
        try (FileOutputStream fos = new FileOutputStream(hashCacheFile)) {
            hashProps.store(fos, "Template content hashes for change detection");
        }
    }
    
    /**
     * Validates template cache using content hashes instead of just counts
     */
    private boolean isTemplateCacheValidWithHashes(File cacheFile, String expectedCacheKey, File targetDir) {
        // First check basic cache validity
        if (!isTemplateCacheValid(cacheFile, expectedCacheKey, targetDir)) {
            return false;
        }
        
        // Then validate content hashes
        File hashCacheFile = new File(targetDir, ".template-hashes");
        if (!hashCacheFile.exists()) {
            return false; // No hash cache means we need to rebuild
        }
        
        try {
            Properties hashProps = new Properties();
            try (FileInputStream fis = new FileInputStream(hashCacheFile)) {
                hashProps.load(fis);
            }
            
            // Validate that all expected templates have correct hashes
            try (Stream<Path> paths = Files.walk(targetDir.toPath())) {
                for (Path templatePath : paths.filter(Files::isRegularFile)
                                               .filter(path -> {
                                                   String fileName = path.getFileName().toString();
                                                   return fileName.endsWith(".mustache") || 
                                                          fileName.endsWith(".hbs") || 
                                                          fileName.endsWith(".handlebars");
                                               })
                                               .toArray(Path[]::new)) {
                    
                    // Get relative path for hash lookup
                    String relativePath = targetDir.toPath().relativize(templatePath).toString().replace(File.separatorChar, '/');
                    String cachedHash = hashProps.getProperty("hash." + relativePath);
                    
                    if (cachedHash == null) {
                        return false; // Hash missing for this template
                    }
                    
                    // Calculate current hash and compare
                    String currentHash = calculateFileContentHash(templatePath);
                    if (!cachedHash.equals(currentHash)) {
                        return false; // Content has changed
                    }
                }
            }
            
            return true; // All hashes match
            
        } catch (IOException e) {
            return false; // Error reading hashes means we should rebuild
        }
    }

    /**
     * Updates the template cache metadata
     */
    private void updateTemplateCache(File cacheFile, String cacheKey) throws IOException {
        Properties cacheProps = new Properties();
        cacheProps.setProperty("cache.key", cacheKey);
        cacheProps.setProperty("cache.timestamp", String.valueOf(System.currentTimeMillis()));
        
        // Count template files
        File parentDir = cacheFile.getParentFile();
        if (parentDir != null && parentDir.exists()) {
            try (Stream<Path> paths = Files.walk(parentDir.toPath())) {
                long templateCount = paths.filter(Files::isRegularFile)
                                        .filter(path -> {
                                            String fileName = path.getFileName().toString();
                                            return fileName.endsWith(".mustache") || 
                                                   fileName.endsWith(".hbs") || 
                                                   fileName.endsWith(".handlebars");
                                        })
                                        .count();
                cacheProps.setProperty("template.count", String.valueOf(templateCount));
            }
        }
        
        try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
            cacheProps.store(fos, "Template extraction cache with content hash validation");
        }
    }
}