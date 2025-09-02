package com.guidedbyte.openapi.modelgen;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for template precedence logic using ProjectBuilder.
 * 
 * <p>Tests the concept of template resolution hierarchy without full TestKit integration.
 * These tests validate the theoretical aspects of template precedence including:</p>
 * <ul>
 *   <li>Template hierarchy documentation and examples</li>
 *   <li>Template variable inheritance concepts</li>
 *   <li>Configuration validation logic</li>
 *   <li>Template precedence rule definitions</li>
 * </ul>
 * 
 * <p>For actual template extraction and usage testing, see TemplatePrecedenceTest 
 * and LiveTemplatePrecedenceTest which use TestKit for real execution.</p>
 */
public class TemplatePrecedenceUnitTest {

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
    void testTemplatePrecedenceConfiguration() {
        // Given: Plugin applied
        project.getPlugins().apply("com.guidedbyte.openapi-modelgen");
        OpenApiModelGenExtension extension = project.getExtensions()
                .findByType(OpenApiModelGenExtension.class);

        // When: Configuring template directory precedence
        extension.getDefaults().templateDir("src/main/resources/user-templates");

        // Then: User template directory should be configured
        assertTrue(extension.getDefaults().getTemplateDir().isPresent());
        assertEquals("src/main/resources/user-templates", 
                    extension.getDefaults().getTemplateDir().get());
    }

    @Test
    void testSpecSpecificTemplatePrecedence() {
        // Given: Plugin applied and configured
        project.getPlugins().apply("com.guidedbyte.openapi-modelgen");
        OpenApiModelGenExtension extension = project.getExtensions()
                .findByType(OpenApiModelGenExtension.class);

        // When: Configuring both default and spec-specific template directories
        extension.getDefaults().templateDir("src/main/resources/default-templates");
        
        SpecConfig specConfig = new SpecConfig(project);
        specConfig.templateDir("src/main/resources/spec-templates");
        extension.getSpecs().put("test", specConfig);

        // Then: Spec should have its own template directory
        assertEquals("src/main/resources/default-templates", 
                    extension.getDefaults().getTemplateDir().get());
        assertEquals("src/main/resources/spec-templates", 
                    extension.getSpecs().get("test").getTemplateDir().get());
    }

    @Test
    void testTemplateVariableHierarchy() {
        // Given: Plugin applied
        project.getPlugins().apply("com.guidedbyte.openapi-modelgen");
        OpenApiModelGenExtension extension = project.getExtensions()
                .findByType(OpenApiModelGenExtension.class);

        // When: Configuring template variables at different levels
        extension.getDefaults().getTemplateVariables().put("copyright", "Default Copyright");
        extension.getDefaults().getTemplateVariables().put("company", "Default Company");
        
        SpecConfig specConfig = new SpecConfig(project);
        specConfig.getTemplateVariables().put("copyright", "Spec Override Copyright");
        extension.getSpecs().put("test", specConfig);

        // Then: Variables should be configured at appropriate levels
        assertEquals("Default Copyright", 
                    extension.getDefaults().getTemplateVariables().get().get("copyright"));
        assertEquals("Default Company", 
                    extension.getDefaults().getTemplateVariables().get().get("company"));
        assertEquals("Spec Override Copyright", 
                    extension.getSpecs().get("test").getTemplateVariables().get().get("copyright"));
    }

    @Test
    void testTemplateDirectoryFallback() {
        // Given: Plugin applied with spec but no custom templates
        project.getPlugins().apply("com.guidedbyte.openapi-modelgen");
        OpenApiModelGenExtension extension = project.getExtensions()
                .findByType(OpenApiModelGenExtension.class);

        SpecConfig specConfig = new SpecConfig(project);
        specConfig.inputSpec("test.yaml");
        specConfig.modelPackage("com.example.test");
        // Note: No templateDir configured - should fall back to plugin templates
        extension.getSpecs().put("test", specConfig);

        // Then: Template directory should not be present (will use plugin defaults)
        assertFalse(extension.getSpecs().get("test").getTemplateDir().isPresent());
        assertFalse(extension.getDefaults().getTemplateDir().isPresent());
    }

    @Test
    void testMultipleSpecsWithDifferentTemplates() {
        // Given: Plugin with multiple specs using different template strategies
        project.getPlugins().apply("com.guidedbyte.openapi-modelgen");
        OpenApiModelGenExtension extension = project.getExtensions()
                .findByType(OpenApiModelGenExtension.class);

        // When: Configuring multiple specs with different template sources
        extension.getDefaults().templateDir("src/main/resources/common-templates");
        
        SpecConfig petsSpec = new SpecConfig(project);
        petsSpec.inputSpec("pets.yaml");
        petsSpec.modelPackage("com.example.pets");
        petsSpec.templateDir("src/main/resources/pets-templates"); // Spec-specific override
        
        SpecConfig ordersSpec = new SpecConfig(project);
        ordersSpec.inputSpec("orders.yaml");
        ordersSpec.modelPackage("com.example.orders");
        // No templateDir - should inherit from defaults
        
        extension.getSpecs().put("pets", petsSpec);
        extension.getSpecs().put("orders", ordersSpec);

        // Then: Each spec should have appropriate template configuration
        assertEquals("src/main/resources/pets-templates", 
                    extension.getSpecs().get("pets").getTemplateDir().get());
        assertFalse(extension.getSpecs().get("orders").getTemplateDir().isPresent()); // Should inherit from defaults
        assertEquals("src/main/resources/common-templates", 
                    extension.getDefaults().getTemplateDir().get());
    }

    @Test
    void testTemplateVariableInheritanceConcept() throws IOException {
        // Given: Plugin configured with hierarchical template variables
        project.getPlugins().apply("com.guidedbyte.openapi-modelgen");
        OpenApiModelGenExtension extension = project.getExtensions()
                .findByType(OpenApiModelGenExtension.class);

        // When: Setting up template variable hierarchy
        // Default level
        extension.getDefaults().getTemplateVariables().put("copyright", "Copyright © {{currentYear}} {{companyName}}");
        extension.getDefaults().getTemplateVariables().put("currentYear", "2025");
        extension.getDefaults().getTemplateVariables().put("companyName", "Default Corp");
        
        // Spec level override
        SpecConfig specConfig = new SpecConfig(project);
        specConfig.getTemplateVariables().put("companyName", "Custom Spec Corp"); // Override company name
        specConfig.getTemplateVariables().put("customVariable", "Spec-specific value");
        extension.getSpecs().put("test", specConfig);

        // Then: Variables should be configured correctly for inheritance
        // Default variables
        assertEquals("Copyright © {{currentYear}} {{companyName}}", 
                    extension.getDefaults().getTemplateVariables().get().get("copyright"));
        assertEquals("2025", 
                    extension.getDefaults().getTemplateVariables().get().get("currentYear"));
        assertEquals("Default Corp", 
                    extension.getDefaults().getTemplateVariables().get().get("companyName"));
        
        // Spec overrides and additions
        assertEquals("Custom Spec Corp", 
                    extension.getSpecs().get("test").getTemplateVariables().get().get("companyName"));
        assertEquals("Spec-specific value", 
                    extension.getSpecs().get("test").getTemplateVariables().get().get("customVariable"));
        
        // Spec should not duplicate default variables (inheritance concept)
        assertFalse(extension.getSpecs().get("test").getTemplateVariables().get().containsKey("currentYear"));
        assertFalse(extension.getSpecs().get("test").getTemplateVariables().get().containsKey("copyright"));
    }

    @Test
    void testTemplatePrecedenceDocumentation() {
        // This test documents the expected template precedence behavior
        // Given: Understanding of template resolution hierarchy
        
        String expectedPrecedence = """
            Template Resolution Hierarchy (Highest to Lowest Precedence):
            1. User Templates (spec-specific templateDir)
            2. User Templates (default templateDir) 
            3. Plugin Templates (built-in JAR resources)
            4. OpenAPI Generator Defaults
            
            Template Variable Resolution:
            1. Spec-specific templateVariables
            2. Default templateVariables
            3. Plugin built-in variables
            
            Cache Behavior:
            - Plugin templates are extracted to build/plugin-templates/
            - User templates are used directly from their directories
            - SHA-256 content hashing for change detection
            - Parallel extraction for performance
            """;
        
        // Then: This documents our expected behavior
        assertNotNull(expectedPrecedence);
        assertTrue(expectedPrecedence.contains("User Templates"));
        assertTrue(expectedPrecedence.contains("Plugin Templates"));
        assertTrue(expectedPrecedence.contains("OpenAPI Generator Defaults"));
        
        // The actual implementation should follow this hierarchy
        // This test serves as living documentation of the template precedence system
    }

    @Test
    void testCreateUserTemplateFiles() throws IOException {
        // This test demonstrates how user templates would be structured
        
        // Given: User template directory structure
        File userTemplateDir = new File(testProjectDir, "src/main/resources/user-templates");
        userTemplateDir.mkdirs();
        
        // When: Creating user template files that override plugin templates
        createUserTemplate(userTemplateDir, "pojo.mustache", 
            "// User override of pojo template\npublic class {{classname}} { /* custom implementation */ }");
        createUserTemplate(userTemplateDir, "additionalModelTypeAnnotations.mustache", 
            "@CustomUserAnnotation\n{{#additionalModelTypeAnnotations}}{{{.}}}{{/additionalModelTypeAnnotations}}");
        
        // Then: User template files should exist and override plugin defaults
        assertTrue(new File(userTemplateDir, "pojo.mustache").exists());
        assertTrue(new File(userTemplateDir, "additionalModelTypeAnnotations.mustache").exists());
        
        String pojoContent = Files.readString(new File(userTemplateDir, "pojo.mustache").toPath());
        assertTrue(pojoContent.contains("User override"));
        assertTrue(pojoContent.contains("custom implementation"));
        
        String annotationsContent = Files.readString(new File(userTemplateDir, "additionalModelTypeAnnotations.mustache").toPath());
        assertTrue(annotationsContent.contains("@CustomUserAnnotation"));
    }

    private void createUserTemplate(File templateDir, String templateName, String content) throws IOException {
        File templateFile = new File(templateDir, templateName);
        Files.write(templateFile.toPath(), List.of(content.split("\\n")));
    }
}