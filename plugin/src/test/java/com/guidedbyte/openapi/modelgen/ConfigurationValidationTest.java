package com.guidedbyte.openapi.modelgen;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for configuration validation functionality.
 * 
 * <p>Validates that the plugin properly detects and reports configuration errors including:</p>
 * <ul>
 *   <li>Missing required properties (inputSpec, modelPackage)</li>
 *   <li>Invalid package names and reserved words</li>
 *   <li>Invalid output directories</li>
 *   <li>Duplicate specification names</li>
 *   <li>Empty or malformed configurations</li>
 * </ul>
 * 
 * <p>These tests ensure users get clear, actionable error messages for configuration issues.</p>
 */
public class ConfigurationValidationTest extends BaseTestKitTest {

    @TempDir
    File testProjectDir;
    
    private File buildFile;

    @BeforeEach
    void setUp() throws IOException {
        File settingsFile = new File(testProjectDir, "settings.gradle");
        buildFile = new File(testProjectDir, "build.gradle");
        
        // Create basic settings.gradle
        Files.write(settingsFile.toPath(), "rootProject.name = 'test-project'".getBytes());
    }

    @Test
    void testMissingInputSpec() throws IOException {
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
                        modelPackage "com.example.model.pets"
                        // inputSpec is missing
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks")
                .buildAndFail();

        assertTrue(result.getOutput().contains("Configuration validation failed"));
        assertTrue(result.getOutput().contains("inputSpec"));
    }

    @Test
    void testMissingModelPackage() throws IOException {
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
                        inputSpec "src/main/resources/openapi-spec/pets.yaml"
                        // modelPackage is missing
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks")
                .buildAndFail();

        assertTrue(result.getOutput().contains("Configuration validation failed"));
        assertTrue(result.getOutput().contains("modelPackage"));
    }

    @Test
    void testInvalidPackageName() throws IOException {
        // Create a valid spec file
        createValidSpec();
        
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
                        inputSpec "src/main/resources/openapi-spec/pets.yaml"
                        modelPackage "123.invalid.package"  // Invalid package name
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks")
                .buildAndFail();

        assertTrue(result.getOutput().contains("Configuration validation failed"));
        assertTrue(result.getOutput().contains("package") || result.getOutput().contains("invalid"));
    }

    @Test
    void testReservedWordInPackageName() throws IOException {
        // Create a valid spec file
        createValidSpec();
        
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
                        inputSpec "src/main/resources/openapi-spec/pets.yaml"
                        modelPackage "com.class.interface"  // Contains Java reserved words
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks")
                .buildAndFail();

        assertTrue(result.getOutput().contains("Configuration validation failed"));
        assertTrue(result.getOutput().contains("reserved") || result.getOutput().contains("package"));
    }

    @Test
    void testEmptySpecs() throws IOException {
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
                    // No specs defined
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks", "--all")
                .build();

        // Should succeed - empty specs are allowed (tasks will validate at execution time)
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
        // Should not create any generation tasks
        assertFalse(result.getOutput().contains("generateOpenApiDtosFor"));
        assertTrue(result.getOutput().contains("generateHelp"));
    }

    @Test
    void testDuplicateSpecNames() throws IOException {
        // Create valid spec files
        createValidSpec();
        createValidSpec("orders.yaml");
        
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
                        inputSpec "src/main/resources/openapi-spec/pets.yaml"
                        modelPackage "com.example.model.pets"
                    }
                    PETS {  // Same name but different case
                        inputSpec "src/main/resources/openapi-spec/orders.yaml"
                        modelPackage "com.example.model.orders"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks")
                .buildAndFail();

        assertTrue(result.getOutput().contains("Configuration validation failed"));
        assertTrue(result.getOutput().contains("Duplicate") || result.getOutput().contains("duplicate"));
    }

    @Test
    void testValidConfiguration() throws IOException {
        // Create a valid spec file
        createValidSpec();
        
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
                    outputDir "build/generated-sources/openapi"
                    modelNameSuffix "Dto"
                    validateSpec false
                    configOptions([
                        dateLibrary: "java8",
                        serializationLibrary: "jackson"
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

        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("generatePets", "--info")
                .build();

        assertFalse(result.getOutput().contains("Configuration validation failed"));
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }

    @Test
    void testInvalidOutputDirectory() throws IOException {
        // Create a valid spec file
        createValidSpec();
        
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
                    outputDir ""  // Empty output directory
                }
                specs {
                    pets {
                        inputSpec "src/main/resources/openapi-spec/pets.yaml"
                        modelPackage "com.example.model.pets"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks")
                .buildAndFail();

        assertTrue(result.getOutput().contains("Configuration validation failed"));
        assertTrue(result.getOutput().contains("outputDir") || result.getOutput().contains("output"));
    }

    private void createValidSpec() throws IOException {
        createValidSpec("pets.yaml");
    }
    
    private void createValidSpec(String filename) throws IOException {
        // Create spec directory
        File specDir = new File(testProjectDir, "src/main/resources/openapi-spec");
        assertTrue(specDir.mkdirs() || specDir.exists());
        
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
                    status:
                      type: string
                      enum:
                        - available
                        - pending
                        - sold
            """;
        
        File specFile = new File(specDir, filename);
        Files.write(specFile.toPath(), specContent.getBytes());
    }
}