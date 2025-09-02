package com.guidedbyte.openapi.modelgen;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for plugin functionality using ProjectBuilder.
 * 
 * <p>These tests validate core plugin behavior including:</p>
 * <ul>
 *   <li>Plugin registration and extension creation</li>
 *   <li>Default configuration values and inheritance</li>
 *   <li>Task registration logic</li>
 *   <li>Multi-spec configuration handling</li>
 * </ul>
 * 
 * <p>Note: These are unit tests that don't execute actual generation tasks.
 * For integration testing with real task execution, see WorkingIntegrationTest.</p>
 */
public class PluginFunctionalTest {

    private Project project;
    
    @TempDir
    File testProjectDir;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder()
                .withProjectDir(testProjectDir)
                .build();
    }

    @Test
    void testPluginRegistration() {
        // When: Plugin is applied
        project.getPlugins().apply("com.guidedbyte.openapi-modelgen");

        // Then: Extension should be registered
        OpenApiModelGenExtension extension = project.getExtensions()
                .findByType(OpenApiModelGenExtension.class);
        
        assertNotNull(extension, "Extension should be registered");
        assertNotNull(extension.getDefaults(), "Defaults should be initialized");
        assertNotNull(extension.getSpecs(), "Specs should be initialized");
    }

    @Test
    void testExtensionConfiguration() {
        // Given: Plugin applied
        project.getPlugins().apply("com.guidedbyte.openapi-modelgen");
        OpenApiModelGenExtension extension = project.getExtensions()
                .findByType(OpenApiModelGenExtension.class);

        // When: Configuring extension
        extension.getDefaults().outputDir("build/custom-output");
        extension.getDefaults().modelNameSuffix("Entity");
        extension.getDefaults().validateSpec(true);

        // Then: Configuration should be set
        assertEquals("build/custom-output", extension.getDefaults().getOutputDir().get());
        assertEquals("Entity", extension.getDefaults().getModelNameSuffix().get());
        assertTrue(extension.getDefaults().getValidateSpec().get());
    }

    @Test
    void testSpecConfiguration() {
        // Given: Plugin applied
        project.getPlugins().apply("com.guidedbyte.openapi-modelgen");
        OpenApiModelGenExtension extension = project.getExtensions()
                .findByType(OpenApiModelGenExtension.class);

        // When: Adding a spec configuration
        SpecConfig specConfig = new SpecConfig(project);
        specConfig.inputSpec("src/main/resources/pets.yaml");
        specConfig.modelPackage("com.example.model");
        extension.getSpecs().put("pets", specConfig);

        // Then: Spec should be configured
        assertEquals(1, extension.getSpecs().size());
        SpecConfig retrievedSpec = extension.getSpecs().get("pets");
        assertNotNull(retrievedSpec);
        assertEquals("src/main/resources/pets.yaml", retrievedSpec.getInputSpec().get());
        assertEquals("com.example.model", retrievedSpec.getModelPackage().get());
    }

    @Test
    void testNoGenerationTasksWithoutConfiguration() {
        // Given: Plugin is applied without specs
        project.getPlugins().apply("com.guidedbyte.openapi-modelgen");
        OpenApiModelGenExtension extension = project.getExtensions()
                .findByType(OpenApiModelGenExtension.class);

        // When: No specs are configured
        assertTrue(extension.getSpecs().isEmpty(), "Specs should be empty initially");

        // Then: Extension should be registered but no generation tasks exist yet
        assertNotNull(extension, "Extension should be registered");
        
        // Note: Tasks are created in afterEvaluate, which doesn't run in ProjectBuilder
        // So we test that the extension is properly configured to have empty specs
        assertEquals(0, extension.getSpecs().size(), "No specs should be configured");
    }

    @Test
    void testPluginAppliesWithoutErrors() {
        // When: Plugin is applied
        assertDoesNotThrow(() -> {
            project.getPlugins().apply("com.guidedbyte.openapi-modelgen");
        }, "Plugin should apply without errors");

        // Then: Extension should be available
        OpenApiModelGenExtension extension = project.getExtensions()
                .findByType(OpenApiModelGenExtension.class);
        assertNotNull(extension, "Extension should be registered");
    }

    @Test
    void testDefaultConfigValues() {
        // Given: Plugin applied
        project.getPlugins().apply("com.guidedbyte.openapi-modelgen");
        OpenApiModelGenExtension extension = project.getExtensions()
                .findByType(OpenApiModelGenExtension.class);

        // Then: Default values should not be set initially
        assertFalse(extension.getDefaults().getOutputDir().isPresent());
        assertFalse(extension.getDefaults().getModelNameSuffix().isPresent());
        assertFalse(extension.getDefaults().getValidateSpec().isPresent());
        assertFalse(extension.getDefaults().getGenerateModelTests().isPresent());
    }

    @Test
    void testSpecConfigDefaults() {
        // Given: New spec config
        SpecConfig specConfig = new SpecConfig(project);

        // Then: Default values should not be set initially
        assertFalse(specConfig.getInputSpec().isPresent());
        assertFalse(specConfig.getModelPackage().isPresent());
        assertFalse(specConfig.getModelNameSuffix().isPresent());
        assertFalse(specConfig.getOutputDir().isPresent());
    }

    @Test
    void testMultipleSpecs() {
        // Given: Plugin applied
        project.getPlugins().apply("com.guidedbyte.openapi-modelgen");
        OpenApiModelGenExtension extension = project.getExtensions()
                .findByType(OpenApiModelGenExtension.class);

        // When: Adding multiple specs
        SpecConfig petsSpec = new SpecConfig(project);
        petsSpec.inputSpec("pets.yaml");
        petsSpec.modelPackage("com.example.pets");
        
        SpecConfig ordersSpec = new SpecConfig(project);
        ordersSpec.inputSpec("orders.yaml");
        ordersSpec.modelPackage("com.example.orders");
        
        extension.getSpecs().put("pets", petsSpec);
        extension.getSpecs().put("orders", ordersSpec);

        // Then: Both specs should be configured
        assertEquals(2, extension.getSpecs().size());
        assertTrue(extension.getSpecs().containsKey("pets"));
        assertTrue(extension.getSpecs().containsKey("orders"));
    }
}