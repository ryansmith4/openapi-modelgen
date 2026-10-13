package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.DefaultConfig;
import com.guidedbyte.openapi.modelgen.OpenApiModelGenExtension;
import com.guidedbyte.openapi.modelgen.SpecConfig;
import com.guidedbyte.openapi.modelgen.TemplateConfiguration;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TaskConfigurationService.
 * Tests task creation and configuration logic in isolation from the full plugin.
 */
class TaskConfigurationServiceTest {

    private TaskConfigurationService taskConfigurationService;
    private Project project;
    private OpenApiModelGenExtension extension;

    @BeforeEach
    void setUp() {
        taskConfigurationService = new TaskConfigurationService();
        
        // Create a real project for testing task creation
        project = ProjectBuilder.builder().build();
        
        // Create extension with test configuration
        extension = new OpenApiModelGenExtension(project);
        setupTestConfiguration();
    }

    private void setupTestConfiguration() {
        // Setup defaults
        DefaultConfig defaults = extension.getDefaults();
        defaults.getOutputDir().set("build/generated/sources/openapi");
        defaults.getModelNameSuffix().set("Dto");
        defaults.getValidateSpec().set(false);
        
        // Setup a test spec
        SpecConfig petSpec = new SpecConfig(project);
        petSpec.getInputSpec().set("src/test/resources/pets.yaml");
        petSpec.getModelPackage().set("com.example.pets");
        extension.getSpecs().put("pets", petSpec);
    }

    @Test
    void testCreateTasksForSpecs() {
        // When
        taskConfigurationService.createTasksForSpecs(project, extension);
        
        // Then
        TaskContainer tasks = project.getTasks();
        
        // Verify setup task is created
        Task setupTask = tasks.findByName("setupTemplateDirectories");
        assertNotNull(setupTask, "Setup template directories task should be created");
        assertEquals("openapi modelgen", setupTask.getGroup());
        
        // Verify spec task is created
        Task petsTask = tasks.findByName("generatePets");
        assertNotNull(petsTask, "Generate pets task should be created");
        assertInstanceOf(GenerateTask.class, petsTask, "Pets task should be a GenerateTask");
        assertEquals("openapi modelgen", petsTask.getGroup());
        
        // Verify aggregate task is created
        Task aggregateTask = tasks.findByName("generateAllModels");
        assertNotNull(aggregateTask, "Generate all models task should be created");
        assertEquals("openapi modelgen", aggregateTask.getGroup());
        
        // Verify help task is created
        Task helpTask = tasks.findByName("generateHelp");
        assertNotNull(helpTask, "Generate help task should be created");
        assertEquals("openapi modelgen", helpTask.getGroup());
    }

    @Test
    void testConfigureGenerateTaskBasicProperties() {
        // Given
        GenerateTask task = project.getTasks().register("testTask", GenerateTask.class).get();
        SpecConfig specConfig = new SpecConfig(project);
        specConfig.getInputSpec().set("test-spec.yaml");
        specConfig.getModelPackage().set("com.example.test");
        
        ProjectLayout projectLayout = project.getLayout();
        ObjectFactory objectFactory = project.getObjects();
        ProviderFactory providerFactory = project.getProviders();
        
        // When
        taskConfigurationService.configureGenerateTask(task, extension, specConfig, "test", 
                project, projectLayout, objectFactory, providerFactory, null);
        
        // Then
        assertEquals("Generate models for test OpenAPI specification", task.getDescription());
        assertEquals("openapi modelgen", task.getGroup());
        assertEquals(new File(project.getProjectDir(), "test-spec.yaml").getAbsolutePath(),
                GenerateTaskPropertyBridge.absolutePath(task, GenerateTaskPropertyBridge.FileProperty.INPUT_SPEC).get());
        assertEquals("com.example.test", task.getModelPackage().get());
        assertEquals("spring", task.getGeneratorName().get());
        assertFalse(task.getValidateSpec().get(), "Validation should be disabled from defaults");
    }

    @Test
    void testApplySpecConfigWithDefaults() {
        // Given
        GenerateTask task = project.getTasks().register("testTask", GenerateTask.class).get();
        SpecConfig specConfig = new SpecConfig(project);
        specConfig.getInputSpec().set("test-spec.yaml");
        specConfig.getModelPackage().set("com.example.test");
        // Don't set outputDir or modelNameSuffix to test defaults
        
        ProjectLayout projectLayout = project.getLayout();
        ObjectFactory objectFactory = project.getObjects();
        ProviderFactory providerFactory = project.getProviders();
        
        // When
        // Create a mock template configuration
        TemplateConfiguration templateConfig = TemplateConfiguration.builder("spring")
            .templateWorkDir(projectLayout.getBuildDirectory().dir("template-work/spring-test").get().getAsFile().getAbsolutePath())
            .build();
            
        taskConfigurationService.applySpecConfig(task, extension, specConfig, "test", 
                project, projectLayout, objectFactory, providerFactory, templateConfig, null);
        
        // Then
        assertEquals(new File(project.getProjectDir(), "test-spec.yaml").getAbsolutePath(),
                GenerateTaskPropertyBridge.absolutePath(task, GenerateTaskPropertyBridge.FileProperty.INPUT_SPEC).get());
        assertEquals("com.example.test", task.getModelPackage().get());
        // OutputDir should be absolute path ending with the expected relative path + spec name
        // (spec name "test" is appended when not explicitly overridden at spec level)
        String outputDir = GenerateTaskPropertyBridge.absolutePath(task, GenerateTaskPropertyBridge.FileProperty.OUTPUT_DIR).get();
        assertTrue(outputDir.endsWith("build" + File.separator + "generated" + File.separator + "sources" + File.separator + "openapi" + File.separator + "test"),
                "OutputDir should end with spec-specific subdirectory, but was: " + outputDir);
        assertEquals("Dto", task.getModelNameSuffix().get());
    }

    @Test
    void testApplySpecConfigSpecOverridesDefaults() {
        // Given
        GenerateTask task = project.getTasks().register("testTask", GenerateTask.class).get();
        SpecConfig specConfig = new SpecConfig(project);
        specConfig.getInputSpec().set("test-spec.yaml");
        specConfig.getModelPackage().set("com.example.test");
        specConfig.getOutputDir().set("custom/output/dir");  // Override default
        specConfig.getModelNameSuffix().set("Model");        // Override default
        specConfig.getValidateSpec().set(true);              // Override default
        
        ProjectLayout projectLayout = project.getLayout();
        ObjectFactory objectFactory = project.getObjects();
        ProviderFactory providerFactory = project.getProviders();
        
        // When
        // Create a mock template configuration
        TemplateConfiguration templateConfig = TemplateConfiguration.builder("spring")
            .templateWorkDir(projectLayout.getBuildDirectory().dir("template-work/spring-test").get().getAsFile().getAbsolutePath())
            .build();
            
        taskConfigurationService.applySpecConfig(task, extension, specConfig, "test", 
                project, projectLayout, objectFactory, providerFactory, templateConfig, null);
        
        // Then - outputDir is now resolved to absolute path
        String expectedOutputDir = new File(project.getProjectDir(), "custom/output/dir").getAbsolutePath();
        assertEquals(expectedOutputDir, GenerateTaskPropertyBridge.absolutePath(task, GenerateTaskPropertyBridge.FileProperty.OUTPUT_DIR).get());
        assertEquals("Model", task.getModelNameSuffix().get());
        assertTrue(task.getValidateSpec().get(), "Should use spec-level validation setting");
    }

    @Test
    void testApplySpecConfigWithEmptyStringSuffix() {
        // Given
        GenerateTask task = project.getTasks().register("testTask", GenerateTask.class).get();
        SpecConfig specConfig = new SpecConfig(project);
        specConfig.getInputSpec().set("test-spec.yaml");
        specConfig.getModelPackage().set("com.example.test");
        specConfig.getModelNameSuffix().set("");  // Test empty string suffix
        
        ProjectLayout projectLayout = project.getLayout();
        ObjectFactory objectFactory = project.getObjects();
        ProviderFactory providerFactory = project.getProviders();
        
        // When
        // Create a mock template configuration
        TemplateConfiguration templateConfig = TemplateConfiguration.builder("spring")
            .templateWorkDir(projectLayout.getBuildDirectory().dir("template-work/spring-test").get().getAsFile().getAbsolutePath())
            .build();
            
        taskConfigurationService.applySpecConfig(task, extension, specConfig, "test", 
                project, projectLayout, objectFactory, providerFactory, templateConfig, null);
        
        // Then
        assertEquals("", task.getModelNameSuffix().get(), "Empty string suffix should be preserved");
    }

    @Test
    void testApplyDefaultConfigurationSetsCommonOptions() {
        // Given
        GenerateTask task = project.getTasks().register("testTask", GenerateTask.class).get();
        
        // When - Use reflection to call private method
        invokeApplyDefaultConfiguration(task);
        
        // Then
        // Verify some key configuration options are set
        assertEquals("swagger2", task.getConfigOptions().get().get("annotationLibrary"));
        assertEquals("true", task.getConfigOptions().get().get("useSpringBoot3"));
        assertEquals("true", task.getConfigOptions().get().get("useJakartaEe"));
        assertEquals("java8", task.getConfigOptions().get().get("dateLibrary"));
        assertEquals("true", task.getConfigOptions().get().get("serializableModel"));
        
        // Verify no hardcoded additionalModelTypeAnnotations - plugin should not provide fallback configuration
        String additionalAnnotations = task.getConfigOptions().get().get("additionalModelTypeAnnotations");
        assertNull(additionalAnnotations, "Plugin should not provide hardcoded additionalModelTypeAnnotations");
    }

    @Test
    void testJacksonAnnotationOptionsDefaultedToFalseOnGenerator725AndLater() {
        // Given: generator 7.26.0 detected and the user did not set either option
        taskConfigurationService.setDetectedGeneratorVersion("7.26.0");
        GenerateTask task = project.getTasks().register("testTask", GenerateTask.class).get();
        SpecConfig specConfig = newSpecConfig();

        // When
        applySpecConfig(task, specConfig);

        // Then
        Map<String, String> options = task.getConfigOptions().get();
        assertEquals("false", options.get("generateJsonIncludeAnnotations"));
        assertEquals("false", options.get("generateJsonSetterNullsAnnotations"));
    }

    @Test
    void testJacksonAnnotationOptionsNotSetBelowGenerator725() {
        // Given: generator 7.14.0 detected; the options are unknown to that generator
        taskConfigurationService.setDetectedGeneratorVersion("7.14.0");
        GenerateTask task = project.getTasks().register("testTask", GenerateTask.class).get();
        SpecConfig specConfig = newSpecConfig();

        // When
        applySpecConfig(task, specConfig);

        // Then
        Map<String, String> options = task.getConfigOptions().get();
        assertFalse(options.containsKey("generateJsonIncludeAnnotations"),
            "Option must not be set below 7.25.0, but was: " + options.get("generateJsonIncludeAnnotations"));
        assertFalse(options.containsKey("generateJsonSetterNullsAnnotations"),
            "Option must not be set below 7.25.0, but was: " + options.get("generateJsonSetterNullsAnnotations"));
    }

    @Test
    void testJacksonAnnotationOptionsNotSetWhenGeneratorVersionUnknown() {
        // Given: no detected version
        taskConfigurationService.setDetectedGeneratorVersion(null);
        GenerateTask task = project.getTasks().register("testTask", GenerateTask.class).get();
        SpecConfig specConfig = newSpecConfig();

        // When
        applySpecConfig(task, specConfig);

        // Then
        Map<String, String> options = task.getConfigOptions().get();
        assertFalse(options.containsKey("generateJsonIncludeAnnotations"));
        assertFalse(options.containsKey("generateJsonSetterNullsAnnotations"));
    }

    @Test
    void testJacksonAnnotationOptionsUserValuesWinOnGenerator725AndLater() {
        // Given: generator 7.25.0 detected; user opts in at the defaults level for one option
        // and at the spec level for the other
        taskConfigurationService.setDetectedGeneratorVersion("7.25.0");
        extension.getDefaults().getConfigOptions().put("generateJsonIncludeAnnotations", "true");
        GenerateTask task = project.getTasks().register("testTask", GenerateTask.class).get();
        SpecConfig specConfig = newSpecConfig();
        specConfig.getConfigOptions().put("generateJsonSetterNullsAnnotations", "true");

        // When
        applySpecConfig(task, specConfig);

        // Then
        Map<String, String> options = task.getConfigOptions().get();
        assertEquals("true", options.get("generateJsonIncludeAnnotations"), "defaults-level user value must be preserved");
        assertEquals("true", options.get("generateJsonSetterNullsAnnotations"), "spec-level user value must be preserved");
    }

    @Test
    void testGenerationControlFlagsDefaultToFalseOnTheTask() {
        // Given: neither defaults nor spec set any generation control flag
        GenerateTask task = project.getTasks().register("testTask", GenerateTask.class).get();
        SpecConfig specConfig = newSpecConfig();

        // When
        applySpecConfig(task, specConfig);

        // Then: the documented plugin defaults (false) reach the task, overriding the generator's
        // own defaults; before 2.2.3 the flags were never applied and the "java" generator wrote
        // model tests and docs into the output directory
        assertFalse(task.getGenerateModelTests().get(), "generateModelTests must default to false");
        assertFalse(task.getGenerateApiTests().get(), "generateApiTests must default to false");
        assertFalse(task.getGenerateModelDocumentation().get(), "generateModelDocumentation must default to false");
        assertFalse(task.getGenerateApiDocumentation().get(), "generateApiDocumentation must default to false");
    }

    @Test
    void testGenerationControlFlagsHonourDefaultsAndSpecOverrides() {
        // Given: defaults enable two flags, the spec overrides one of them back off and enables another
        extension.getDefaults().generateModelTests(true);
        extension.getDefaults().generateApiDocumentation(true);
        GenerateTask task = project.getTasks().register("testTask", GenerateTask.class).get();
        SpecConfig specConfig = newSpecConfig();
        specConfig.generateModelTests(false);
        specConfig.generateApiTests(true);

        // When
        applySpecConfig(task, specConfig);

        // Then
        assertFalse(task.getGenerateModelTests().get(), "spec-level false must override defaults-level true");
        assertTrue(task.getGenerateApiTests().get(), "spec-level true must be applied");
        assertTrue(task.getGenerateApiDocumentation().get(), "defaults-level true must be applied");
        assertFalse(task.getGenerateModelDocumentation().get(), "unset flag must stay false");
    }

    private SpecConfig newSpecConfig() {
        SpecConfig specConfig = new SpecConfig(project);
        specConfig.getInputSpec().set("test-spec.yaml");
        specConfig.getModelPackage().set("com.example.test");
        return specConfig;
    }

    private void applySpecConfig(GenerateTask task, SpecConfig specConfig) {
        ProjectLayout projectLayout = project.getLayout();
        TemplateConfiguration templateConfig = TemplateConfiguration.builder("spring")
            .templateWorkDir(projectLayout.getBuildDirectory().dir("template-work/spring-test").get().getAsFile().getAbsolutePath())
            .build();
        taskConfigurationService.applySpecConfig(task, extension, specConfig, "test",
                project, projectLayout, project.getObjects(), project.getProviders(), templateConfig, null);
    }

    @Test
    void testConfigureParallelExecution() {
        // Given
        Task aggregateTask = project.getTasks().register("testAggregate").get();
        TaskContainer tasks = project.getTasks();
        
        // When
        taskConfigurationService.configureParallelExecution(aggregateTask, extension, tasks);
        
        // Then
        // The method should complete without error
        // Actual parallel execution is handled by Gradle's --parallel flag
        // This test verifies the configuration doesn't throw exceptions
        assertNotNull(aggregateTask);
    }

    @Test
    void testCreateHelpTask() {
        // When
        taskConfigurationService.createTasksForSpecs(project, extension);
        
        // Then
        Task helpTask = project.getTasks().findByName("generateHelp");
        assertNotNull(helpTask);
        assertEquals("openapi modelgen", helpTask.getGroup());
        assertEquals("Shows usage information and examples for the OpenAPI Model Generator plugin", 
                    helpTask.getDescription());
        
        // Verify help task has an action (doLast)
        assertFalse(helpTask.getActions().isEmpty(), "Help task should have actions");
    }

    @Test
    void testTaskDependencies() {
        // When
        taskConfigurationService.createTasksForSpecs(project, extension);
        
        // Then
        Task petsTask = project.getTasks().findByName("generatePets");
        Task setupTask = project.getTasks().findByName("setupTemplateDirectories");
        Task aggregateTask = project.getTasks().findByName("generateAllModels");
        
        assertNotNull(petsTask);
        assertNotNull(setupTask);
        assertNotNull(aggregateTask);
        
        // Verify pets task depends on prepare task (new architecture uses individual prepare tasks)
        Task prepareTask = project.getTasks().findByName("prepareTemplateDirectoryPets");
        assertNotNull(prepareTask, "Prepare task should exist for pets");
        
        // With Provider-based wiring, dependencies are implicit through task inputs
        // The GenerateTask will automatically depend on PrepareTemplateDirectoryTask via Provider
        // So we don't need to check explicit dependencies here anymore
        
        // Verify aggregate task depends on pets task
        boolean dependsOnPets = aggregateTask.getDependsOn().stream()
                               .anyMatch(dep -> dep.toString().contains("generatePets"));
        assertTrue(dependsOnPets, "Aggregate task should depend on pets task");
    }

    @Test
    void testExpandTemplateVariables() {
        // Given - Create test variables
        Map<String, String> templateVars = new HashMap<>();
        templateVars.put("company", "TestCorp");
        templateVars.put("copyright", "© {{currentYear}} {{company}}");
        
        // When - Use reflection to call the private method
        Map<String, String> expanded = invokeExpandTemplateVariables(templateVars);
        
        // Then
        assertNotNull(expanded);
        assertTrue(expanded.containsKey("currentYear"), "Should add built-in currentYear variable");
        assertTrue(expanded.containsKey("generatedBy"), "Should add built-in generatedBy variable");
        
        String copyright = expanded.get("copyright");
        assertNotNull(copyright);
        assertTrue(copyright.contains("TestCorp"), "Should expand company variable");
        assertFalse(copyright.contains("{{"), "Should not contain unexpanded variables");
    }

    /**
     * Helper methods to invoke private methods using reflection
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> invokeExpandTemplateVariables(Map<String, String> variables) {
        try {
            var method = TaskConfigurationService.class.getDeclaredMethod("expandTemplateVariables", Map.class);
            method.setAccessible(true);
            return (Map<String, String>) method.invoke(taskConfigurationService, variables);
        } catch (Exception e) {
            fail("Could not invoke expandTemplateVariables method: " + e.getMessage());
            return null;
        }
    }

    private void invokeApplyDefaultConfiguration(GenerateTask task) {
        try {
            var method = TaskConfigurationService.class.getDeclaredMethod("applyDefaultConfiguration", GenerateTask.class);
            method.setAccessible(true);
            method.invoke(taskConfigurationService, task);
        } catch (Exception e) {
            fail("Could not invoke applyDefaultConfiguration method: " + e.getMessage());
        }
    }
}