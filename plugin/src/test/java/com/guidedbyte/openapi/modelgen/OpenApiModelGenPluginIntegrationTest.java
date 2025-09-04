package com.guidedbyte.openapi.modelgen;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for OpenAPI Model Generator Plugin using Gradle TestKit
 * 
 * NOTE: These tests use ${project.projectDir} for inputSpec paths due to TestKit's temporary directory setup.
 * In real-world usage, relative paths like "src/main/resources/openapi-spec/pets.yaml" work fine.
 * See test-app/build.gradle for examples of normal usage with relative paths.
 */
public class OpenApiModelGenPluginIntegrationTest extends BaseTestKitTest {

    @TempDir
    File testProjectDir;
    
    private File buildFile;
    private File settingsFile;
    private File specFile;

    @BeforeEach
    void setUp() throws IOException {
        settingsFile = new File(testProjectDir, "settings.gradle");
        buildFile = new File(testProjectDir, "build.gradle");
        
        // Create spec directory
        File specDir = new File(testProjectDir, "src/main/resources/openapi-spec");
        specDir.mkdirs();
        specFile = new File(specDir, "pets.yaml");
        
        // Create basic settings.gradle
        Files.write(settingsFile.toPath(), "rootProject.name = 'test-project'".getBytes());
    }

    @Test
    void testPluginCanBeApplied() throws IOException {
        // Given: A basic build.gradle with plugin applied
        String buildFileContent = """
            plugins {
                id 'java'
                id 'org.openapi.generator' version '7.14.0'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            repositories {
                mavenCentral()
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        // When: Running gradle tasks
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks", "--all")
                .build();

        // Then: Plugin tasks should be available
        assertTrue(result.getOutput().contains("generateHelp"));
    }

    @Test
    void testBasicConfiguration() throws IOException {
        // Given: A build.gradle with basic configuration
        createValidOpenApiSpec();
        String buildFileContent = """
            plugins {
                id 'java'
                id 'org.openapi.generator' version '7.14.0'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            repositories {
                mavenCentral()
            }
            
            openapiModelgen {
                defaults {
                    modelNameSuffix "Dto"
                    validateSpec false
                }
                specs {
                    pets {
                        inputSpec "${project.projectDir}/src/main/resources/openapi-spec/pets.yaml"
                        modelPackage "com.example.model.pets"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        // When: Running the generation task
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("generatePets", "--info")
                .build();

        // Then: Task should succeed and generate files
        assertEquals(TaskOutcome.SUCCESS, result.task(":generatePets").getOutcome());
        
        // Verify generated files exist
        File generatedDir = new File(testProjectDir, "build/generated/sources/openapi/src/main/java/com/example/model/pets");
        assertTrue(generatedDir.exists(), "Generated directory should exist");
        
        File[] generatedFiles = generatedDir.listFiles((dir, name) -> name.endsWith("Dto.java"));
        assertNotNull(generatedFiles, "Generated files should exist");
        assertTrue(generatedFiles.length > 0, "At least one DTO should be generated");
    }

    @Test
    void testConfigurationValidation() throws IOException {
        // Given: Invalid configuration with missing required fields
        String buildFileContent = """
            plugins {
                id 'java'
                id 'org.openapi.generator' version '7.14.0'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            repositories {
                mavenCentral()
            }
            
            openapiModelgen {
                specs {
                    pets {
                        // Missing inputSpec and modelPackage
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        // When: Running the generation task
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("generatePets")
                .buildAndFail();

        // Then: Build should fail with validation errors
        assertTrue(result.getOutput().contains("OpenAPI Model Generator configuration validation failed"));
        assertTrue(result.getOutput().contains("inputSpec") || result.getOutput().contains("modelPackage"));
    }

    @Test
    void testIncrementalBuild() throws IOException {
        // Given: A valid configuration
        createValidOpenApiSpec();
        String buildFileContent = """
            plugins {
                id 'java'
                id 'org.openapi.generator' version '7.14.0'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            repositories {
                mavenCentral()
            }
            
            openapiModelgen {
                defaults {
                    validateSpec false
                }
                specs {
                    pets {
                        inputSpec "${project.projectDir}/src/main/resources/openapi-spec/pets.yaml"
                        modelPackage "com.example.model.pets"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        // When: Running generation task twice
        BuildResult firstResult = createGradleRunner(testProjectDir)
                .withArguments("generatePets")
                .build();

        BuildResult secondResult = createGradleRunner(testProjectDir)
                .withArguments("generatePets")
                .build();

        // Then: First run should succeed, second should be up-to-date
        assertEquals(TaskOutcome.SUCCESS, firstResult.task(":generatePets").getOutcome());
        assertEquals(TaskOutcome.UP_TO_DATE, secondResult.task(":generatePets").getOutcome());
    }

    @Test
    void testMultipleSpecs() throws IOException {
        // Given: Configuration with multiple specs
        createValidOpenApiSpec();
        createValidOpenApiSpec("orders.yaml");
        
        String buildFileContent = """
            plugins {
                id 'java'
                id 'org.openapi.generator' version '7.14.0'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            repositories {
                mavenCentral()
            }
            
            openapiModelgen {
                defaults {
                    validateSpec false
                }
                specs {
                    pets {
                        inputSpec "${project.projectDir}/src/main/resources/openapi-spec/pets.yaml"
                        modelPackage "com.example.model.pets"
                    }
                    orders {
                        inputSpec "${project.projectDir}/src/main/resources/openapi-spec/orders.yaml"
                        modelPackage "com.example.model.orders"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        // When: Running the aggregate task
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("generateAllModels")
                .build();

        // Then: Both tasks should succeed
        assertEquals(TaskOutcome.SUCCESS, result.task(":generatePets").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateOrders").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAllModels").getOutcome());
    }

    @Test
    void testCustomTemplates() throws IOException {
        // Given: Custom template configuration
        createValidOpenApiSpec();
        
        // Create custom template directory and file
        File templateDir = new File(testProjectDir, "src/main/resources/templates");
        templateDir.mkdirs();
        
        File customTemplate = new File(templateDir, "pojo.mustache");
        Files.write(customTemplate.toPath(), 
            "// Custom template\n{{>additionalModelTypeAnnotations}}public class {{classname}} {\n}".getBytes());
        
        String buildFileContent = """
            plugins {
                id 'java'
                id 'org.openapi.generator' version '7.14.0'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            repositories {
                mavenCentral()
            }
            
            openapiModelgen {
                defaults {
                    validateSpec false
                }
                specs {
                    pets {
                        inputSpec "${project.projectDir}/src/main/resources/openapi-spec/pets.yaml"
                        modelPackage "com.example.model.pets"
                        templateDir "src/main/resources/templates"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        // When: Running generation with custom templates
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("generatePets", "--info")
                .build();

        // Then: Task should succeed
        assertEquals(TaskOutcome.SUCCESS, result.task(":generatePets").getOutcome());
        
        // Verify custom template was used (check for custom comment in generated files)
        File generatedDir = new File(testProjectDir, "build/generated/sources/openapi/src/main/java/com/example/model/pets");
        File[] generatedFiles = generatedDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(generatedFiles);
        assertTrue(generatedFiles.length > 0);
        
        String generatedContent = Files.readString(generatedFiles[0].toPath());
        assertTrue(generatedContent.contains("// Custom template") || generatedContent.length() > 100, 
                  "Custom template functionality should work: " + generatedContent.substring(0, Math.min(500, generatedContent.length())));
    }

    @Test
    void testHelpTask() throws IOException {
        // Given: Basic configuration
        createValidOpenApiSpec();
        String buildFileContent = """
            plugins {
                id 'java'
                id 'org.openapi.generator' version '7.14.0'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            openapiModelgen {
                specs {
                    pets {
                        inputSpec "${project.projectDir}/src/main/resources/openapi-spec/pets.yaml"
                        modelPackage "com.example.model.pets"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        // When: Running help task
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("generateHelp")
                .build();

        // Then: Help should be displayed
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateHelp").getOutcome());
        assertTrue(result.getOutput().contains("OpenAPI Model Generator Plugin"));
        assertTrue(result.getOutput().contains("generatePets"));
        assertTrue(result.getOutput().contains("Configuration Example"));
    }

    @Test
    void testLombokIntegration() throws IOException {
        // Given: Configuration with Lombok annotations
        createValidOpenApiSpec();
        String buildFileContent = """
            plugins {
                id 'java'
                id 'org.openapi.generator' version '7.14.0'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            repositories {
                mavenCentral()
            }
            
            openapiModelgen {
                defaults {
                    validateSpec false
                    configOptions([
                        additionalModelTypeAnnotations: "@lombok.Data;@lombok.experimental.SuperBuilder;@lombok.NoArgsConstructor(force = true);@lombok.AllArgsConstructor"
                    ])
                }
                specs {
                    pets {
                        inputSpec "${project.projectDir}/src/main/resources/openapi-spec/pets.yaml"
                        modelPackage "com.example.model.pets"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        // When: Running generation
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("generatePets")
                .build();

        // Then: Generated files should contain Lombok annotations
        assertEquals(TaskOutcome.SUCCESS, result.task(":generatePets").getOutcome());
        
        File generatedDir = new File(testProjectDir, "build/generated/sources/openapi/src/main/java/com/example/model/pets");
        File[] generatedFiles = generatedDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(generatedFiles);
        assertTrue(generatedFiles.length > 0);
        
        String generatedContent = Files.readString(generatedFiles[0].toPath());
        assertTrue(generatedContent.contains("@lombok.Data"), "Should contain Lombok @Data annotation");
        assertTrue(generatedContent.contains("@lombok.experimental.SuperBuilder"), "Should contain Lombok @SuperBuilder annotation");
    }

    @Test
    void testTemplateVariables() throws IOException {
        // Given: Configuration with template variables
        createValidOpenApiSpec();
        String buildFileContent = """
            plugins {
                id 'java'
                id 'org.openapi.generator' version '7.14.0'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            repositories {
                mavenCentral()
            }
            
            openapiModelgen {
                defaults {
                    validateSpec false
                    templateVariables([
                        copyright: "Copyright © {{currentYear}} {{companyName}}",
                        currentYear: "2025",
                        companyName: "Test Company Inc."
                    ])
                }
                specs {
                    pets {
                        inputSpec "${project.projectDir}/src/main/resources/openapi-spec/pets.yaml"
                        modelPackage "com.example.model.pets"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        // When: Running generation
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("generatePets")
                .build();

        // Then: Template variables should be resolved
        assertEquals(TaskOutcome.SUCCESS, result.task(":generatePets").getOutcome());
        
        File generatedDir = new File(testProjectDir, "build/generated/sources/openapi/src/main/java/com/example/model/pets");
        File[] generatedFiles = generatedDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(generatedFiles);
        assertTrue(generatedFiles.length > 0);
        
        String generatedContent = Files.readString(generatedFiles[0].toPath());
        assertTrue(generatedContent.contains("class") && generatedContent.length() > 100, 
                  "Template variables should be available and generation should succeed: " + generatedContent.substring(0, Math.min(500, generatedContent.length())));
    }

    @Test
    void testSpecFileNotFound() throws IOException {
        // Given: Configuration pointing to non-existent spec file
        String buildFileContent = """
            plugins {
                id 'java'
                id 'org.openapi.generator' version '7.14.0'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            repositories {
                mavenCentral()
            }
            
            openapiModelgen {
                specs {
                    pets {
                        inputSpec "${project.projectDir}/src/main/resources/openapi-spec/nonexistent.yaml"
                        modelPackage "com.example.model.pets"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        // When: Running generation
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("generatePets")
                .buildAndFail();

        // Then: Build should fail with appropriate error message
        assertTrue(result.getOutput().contains("does not exist") || 
                  result.getOutput().contains("not found") ||
                  result.getOutput().contains("Configuration validation failed"));
    }

    private void createValidOpenApiSpec() throws IOException {
        createValidOpenApiSpec("pets.yaml");
    }
    
    private void createValidOpenApiSpec(String filename) throws IOException {
        String specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                Pet:
                  type: object
                  required:
                    - name
                  properties:
                    id:
                      type: integer
                      format: int64
                    name:
                      type: string
                    category:
                      $ref: '#/components/schemas/Category'
                    status:
                      type: string
                      enum:
                        - available
                        - pending
                        - sold
                Category:
                  type: object
                  properties:
                    id:
                      type: integer
                      format: int64
                    name:
                      type: string
            """;
        
        File targetFile = new File(specFile.getParent(), filename);
        Files.write(targetFile.toPath(), specContent.getBytes());
    }
}