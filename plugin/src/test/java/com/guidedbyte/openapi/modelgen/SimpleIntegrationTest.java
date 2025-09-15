package com.guidedbyte.openapi.modelgen;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration tests that verify basic plugin functionality.
 * 
 * <p>These tests focus on fundamental plugin behavior including:</p>
 * <ul>
 *   <li>Plugin application without errors</li>
 *   <li>Extension registration and configuration</li>
 *   <li>Help task registration and functionality</li>
 *   <li>Basic configuration validation scenarios</li>
 * </ul>
 * 
 * <p>These tests use TestKit for realistic Gradle environment testing.</p>
 */
public class SimpleIntegrationTest extends BaseTestKitTest {

    @TempDir
    File testProjectDir;
    
    private File buildFile;

    @BeforeEach
    void setUp() throws IOException {
        File settingsFile = new File(testProjectDir, "settings.gradle");
        buildFile = new File(testProjectDir, "build.gradle");
        
        // Create basic settings.gradle
        Files.write(settingsFile.toPath(), List.of("rootProject.name = 'test-project'"));
    }

    @Test
    void testPluginCanBeApplied() throws IOException {
        // Given: A basic build.gradle with plugin applied using modern plugin application
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
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running gradle tasks
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks", "--all")
                .build();

        // Then: Plugin should be applied successfully
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(result.task(":tasks")).getOutcome());
        assertFalse(result.getOutput().contains("FAILED"));
    }

    @Test
    void testPluginRegistersExtension() throws IOException {
        // Given: A build file with plugin applied
        String buildFileContent = """
            plugins {
                id 'java'
                id 'org.openapi.generator' version '7.14.0'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            repositories {
                mavenCentral()
            }
            
            task checkExtension {
                doLast {
                    println "Extension class: " + project.extensions.findByName('openapiModelgen')?.class?.name
                }
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running the check task
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("checkExtension")
                .build();

        // Then: Extension should be registered
        assertTrue(result.getOutput().contains("OpenApiModelGenExtension"));
    }

    @Test
    void testHelpTaskIsRegistered() throws IOException {
        // Given: A build file with plugin applied
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
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Listing all tasks
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks", "--all")
                .build();

        // Then: Help task should be available
        assertTrue(result.getOutput().contains("generateHelp"));
    }

    @Test
    void testPluginWithValidConfiguration() throws IOException {
        // Create a valid spec file
        createValidSpec();
        
        // Given: A build file with valid configuration using DSL
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
                        outputDir "build/generated/sources/openapi"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running the generation task
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("generatePets", "--info", "--stacktrace")
                .build();

        // Then: Task should succeed
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(result.task(":generatePets")).getOutcome());
        
        // Verify generated files exist in the default location (build/generated/sources/openapi)
        File generatedPackageDir = new File(testProjectDir, "build/generated/sources/openapi/src/main/java/com/example/model/pets");
        assertTrue(generatedPackageDir.exists(), "Generated directory should exist");
        
        File[] generatedFiles = generatedPackageDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(generatedFiles, "Generated files should exist");
        assertTrue(generatedFiles.length > 0, "At least one Java file should be generated");
    }

    @Test
    void testConfigurationValidationWithMissingSpecs() throws IOException {
        // Given: A build file with no specs configured
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
                    // Empty specs block
                }
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Listing tasks
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks", "--all")
                .build();

        // Then: Should not create generation tasks, only help task
        assertFalse(result.getOutput().contains("generateOpenApiDtosFor"));
        assertTrue(result.getOutput().contains("generateHelp"));
    }

    private void createValidSpec() throws IOException {
        // Create spec directory
        File specDir = new File(testProjectDir, "src/main/resources/openapi-spec");
        assertTrue(specDir.mkdirs() || specDir.exists(),
                  "Failed to create directory: " + specDir);
        
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
        
        File specFile = new File(specDir, "pets.yaml");
        Files.write(specFile.toPath(), List.of(specContent.split("\\n")));
    }
    
    private void printDirectoryTree(File dir, String prefix) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                System.out.println(prefix + file.getName());
                if (file.isDirectory()) {
                    printDirectoryTree(file, prefix + "  ");
                }
            }
        }
    }
}