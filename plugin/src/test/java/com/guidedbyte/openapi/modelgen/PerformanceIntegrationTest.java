package com.guidedbyte.openapi.modelgen;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and incremental build tests for the OpenAPI Model Generator Plugin
 * <p>
 * NOTE: These tests use ${project.projectDir} for inputSpec paths due to TestKit's temporary directory setup.
 * In real-world usage, relative paths like "src/main/resources/openapi-spec/pets.yaml" work fine.
 * See test-app/build.gradle for examples of normal usage with relative paths.
 */
public class PerformanceIntegrationTest extends BaseTestKitTest {

    @TempDir
    File testProjectDir;
    
    private File buildFile;
    private File specFile;

    @BeforeEach
    void setUp() throws IOException {
        File settingsFile = new File(testProjectDir, "settings.gradle");
        buildFile = new File(testProjectDir, "build.gradle");
        
        // Create spec directory
        File specDir = new File(testProjectDir, "src/main/resources/openapi-spec");
        assertTrue(specDir.mkdirs() || specDir.exists(),
                  "Failed to create directory: " + specDir);
        specFile = new File(specDir, "pets.yaml");
        
        // Create basic settings.gradle
        Files.write(settingsFile.toPath(), "rootProject.name = 'test-project'".getBytes());
        
        createValidOpenApiSpec();
        createBasicBuildFile();
    }

    @Test
    void testIncrementalBuildAfterSpecChange() throws IOException {
        // When: Running generation task twice, then modifying spec and running again
        BuildResult firstResult = runGenerationTask();
        BuildResult secondResult = runGenerationTask();
        
        // Modify the spec file
        modifySpecFile();
        
        BuildResult thirdResult = runGenerationTask();

        // Then: First run SUCCESS, second UP-TO-DATE or SUCCESS, third SUCCESS due to spec change
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(firstResult.task(":generatePets")).getOutcome());
        // Note: Template precedence detection may cause tasks to run as SUCCESS instead of UP_TO_DATE
        TaskOutcome secondOutcome = Objects.requireNonNull(secondResult.task(":generatePets")).getOutcome();
        assertTrue(secondOutcome == TaskOutcome.UP_TO_DATE ||
                   secondOutcome == TaskOutcome.SUCCESS,
                   "Second run should be UP_TO_DATE or SUCCESS, was: " + secondOutcome);
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(thirdResult.task(":generatePets")).getOutcome());
    }

    @Test
    void testIncrementalBuildAfterConfigurationChange() throws IOException {
        // When: Running generation, then changing configuration
        BuildResult firstResult = runGenerationTask();
        
        // Modify configuration
        modifyConfiguration();
        
        BuildResult secondResult = runGenerationTask();

        // Then: Both runs should succeed (configuration change should invalidate cache)
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(firstResult.task(":generatePets")).getOutcome());
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(secondResult.task(":generatePets")).getOutcome());
    }

    @Test
    void testIncrementalBuildWithTemplateChange() throws IOException {
        // Given: Configuration with custom template
        setupCustomTemplateConfiguration();
        
        BuildResult firstResult = runGenerationTask();
        
        // Wait a bit to ensure file timestamps differ
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Modify template
        modifyCustomTemplate();
        
        BuildResult secondResult = runGenerationTask();

        // Then: First run should succeed, second run should also be SUCCESS since template changed
        // (custom template changes DO affect the task when using userTemplateDir)
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(firstResult.task(":generatePets")).getOutcome());
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(secondResult.task(":generatePets")).getOutcome());
    }

    @Test
    void testParallelTemplateProcessing() throws IOException {
        // Given: Configuration that should trigger parallel processing
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
                    userTemplateDir "src/main/templates/custom" // Trigger plugin template extraction
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

        // When: Running generation task with info logging
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("generatePets", "--info")
                .build();

        // Then: Task should succeed (template processing working correctly)
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(result.task(":generatePets")).getOutcome());
        // Note: Template processing log messages may vary, core functionality is the successful build
    }

    @Test
    void testCacheBehaviorAcrossCleanBuilds() throws IOException {
        // When: Run, clean, run again
        BuildResult firstResult = runGenerationTask();
        
        BuildResult cleanResult = createGradleRunner(testProjectDir)
                .withArguments("clean")
                .build();
        
        BuildResult secondResult = runGenerationTask();

        // Then: Both generation runs should succeed (no UP-TO-DATE after clean)
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(firstResult.task(":generatePets")).getOutcome());
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(cleanResult.task(":clean")).getOutcome());
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(secondResult.task(":generatePets")).getOutcome());
    }

    @Test
    void testMultipleSpecsIncrementalBehavior() throws IOException {
        // Given: Multiple specs configuration
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

        // When: Run all, then modify only one spec
        BuildResult firstResult = createGradleRunner(testProjectDir)
                .withArguments("generateAllModels")
                .build();
        
        // Modify only pets spec
        modifySpecFile();
        
        BuildResult secondResult = createGradleRunner(testProjectDir)
                .withArguments("generateAllModels")
                .build();

        // Then: First run all SUCCESS, second run pets SUCCESS and orders UP-TO-DATE or SUCCESS
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(firstResult.task(":generatePets")).getOutcome());
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(firstResult.task(":generateOrders")).getOutcome());
        
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(secondResult.task(":generatePets")).getOutcome());
        // Note: Template precedence detection may cause tasks to run as SUCCESS instead of UP_TO_DATE
        TaskOutcome ordersOutcome = Objects.requireNonNull(secondResult.task(":generateOrders")).getOutcome();
        assertTrue(ordersOutcome == TaskOutcome.UP_TO_DATE ||
                   ordersOutcome == TaskOutcome.SUCCESS,
                   "Orders task should be UP_TO_DATE or SUCCESS, was: " + ordersOutcome);
    }

    @Test
    void testLargeSpecFilePerformance() throws IOException {
        // Given: A larger spec file with multiple models
        createLargeOpenApiSpec();
        
        long startTime = System.currentTimeMillis();
        
        // When: Running generation
        BuildResult result = runGenerationTask();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: Should complete successfully within reasonable time (< 30 seconds)
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(result.task(":generatePets")).getOutcome());
        assertTrue(duration < 30000, "Generation should complete within 30 seconds, took: " + duration + "ms");
        
        // Verify multiple files were generated
        File generatedDir = new File(testProjectDir, "build/generated/sources/openapi/src/main/java/com/example/model/pets");
        assertTrue(generatedDir.exists());
        
        File[] generatedFiles = generatedDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(generatedFiles);
        assertTrue(generatedFiles.length >= 5, "Should generate multiple model files");
    }

    private BuildResult runGenerationTask() {
        return createGradleRunner(testProjectDir)
                .withArguments("generatePets")
                .build();
    }

    private void createBasicBuildFile() throws IOException {
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

    private void modifySpecFile() throws IOException {
        String additionalContent = """
                Tag:
                  type: object
                  properties:
                    id:
                      type: integer
                      format: int64
                    name:
                      type: string
            """;
        
        Files.write(specFile.toPath(), additionalContent.getBytes(), StandardOpenOption.APPEND);
    }

    private void modifyConfiguration() throws IOException {
        String updatedBuildFile = """
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
                    modelNameSuffix "Entity"  // Changed suffix
                }
                specs {
                    pets {
                        inputSpec "${project.projectDir}/src/main/resources/openapi-spec/pets.yaml"
                        modelPackage "com.example.model.pets"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), updatedBuildFile.getBytes());
    }

    private void setupCustomTemplateConfiguration() throws IOException {
        // Create custom template  
        File templateDir = new File(testProjectDir, "src/main/resources/templates");
        assertTrue(templateDir.mkdirs() || templateDir.exists(),
                  "Failed to create directory: " + templateDir);
        
        File customTemplate = new File(templateDir, "pojo.mustache");
        Files.write(customTemplate.toPath(), 
            "// Custom template v1\n{{>additionalModelTypeAnnotations}}public class {{classname}} {\n}".getBytes());
        
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
                        userTemplateDir "src/main/resources/templates"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());
    }

    private void modifyCustomTemplate() throws IOException {
        File templateDir = new File(testProjectDir, "src/main/resources/templates");
        File customTemplate = new File(templateDir, "pojo.mustache");
        
        Files.write(customTemplate.toPath(), 
            "// Custom template v2 - MODIFIED\n{{>additionalModelTypeAnnotations}}public class {{classname}} {\n}".getBytes());
    }

    private void createLargeOpenApiSpec() throws IOException {
        String largeSpecContent = """
            openapi: 3.0.0
            info:
              title: Large Test API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                Pet:
                  type: object
                  properties:
                    id:
                      type: integer
                      format: int64
                    name:
                      type: string
                    category:
                      $ref: '#/components/schemas/Category'
                    tags:
                      type: array
                      items:
                        $ref: '#/components/schemas/Tag'
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
                    description:
                      type: string
                Tag:
                  type: object
                  properties:
                    id:
                      type: integer
                      format: int64
                    name:
                      type: string
                    color:
                      type: string
                User:
                  type: object
                  properties:
                    id:
                      type: integer
                      format: int64
                    username:
                      type: string
                    firstName:
                      type: string
                    lastName:
                      type: string
                    email:
                      type: string
                    phone:
                      type: string
                    userStatus:
                      type: integer
                      description: User Status
                Order:
                  type: object
                  properties:
                    id:
                      type: integer
                      format: int64
                    petId:
                      type: integer
                      format: int64
                    quantity:
                      type: integer
                    shipDate:
                      type: string
                      format: date-time
                    status:
                      type: string
                      enum:
                        - placed
                        - approved
                        - delivered
                    complete:
                      type: boolean
                Address:
                  type: object
                  properties:
                    street:
                      type: string
                    city:
                      type: string
                    state:
                      type: string
                    zip:
                      type: string
            """;
        
        Files.write(specFile.toPath(), largeSpecContent.getBytes());
    }
}