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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Working integration tests using TestKit
 * 
 * NOTE: These tests use ${project.projectDir} for inputSpec paths due to TestKit's temporary directory setup.
 * In real-world usage, relative paths like "src/main/resources/openapi-spec/pets.yaml" work fine.
 * See test-app/build.gradle for examples of normal usage with relative paths.
 */
public class WorkingIntegrationTest {

    @TempDir
    File testProjectDir;
    
    private File buildFile;
    private File settingsFile;

    @BeforeEach
    void setUp() throws IOException {
        settingsFile = new File(testProjectDir, "settings.gradle");
        buildFile = new File(testProjectDir, "build.gradle");
        
        // Create basic settings.gradle
        Files.write(settingsFile.toPath(), List.of("rootProject.name = 'test-project'"));
    }

    @Test
    void testBasicPluginApplication() throws IOException {
        // Given: A minimal build file that applies the plugin by ID
        String buildFileContent = """
            plugins {
                id 'java'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            repositories {
                mavenCentral()
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running gradle tasks
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--group=help")
                .withPluginClasspath()
                .build();

        // Then: Build should succeed
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks").getOutcome());
        assertFalse(result.getOutput().contains("FAILED"));
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }

    @Test
    void testPluginCreatesHelpTask() throws IOException {
        // Given: A build file with plugin applied
        String buildFileContent = """
            plugins {
                id 'java'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            repositories {
                mavenCentral()
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running help task specifically
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("openapiModelgenHelp")
                .withPluginClasspath()
                .build();

        // Then: Help task should run successfully
        assertEquals(TaskOutcome.SUCCESS, result.task(":openapiModelgenHelp").getOutcome());
        assertTrue(result.getOutput().contains("OpenAPI Model Generator Plugin"));
        assertTrue(result.getOutput().contains("Configuration Example"));
    }

    @Test
    void testValidationWithBadConfig() throws IOException {
        // Given: A build file with invalid configuration
        String buildFileContent = """
            plugins {
                id 'java'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            repositories {
                mavenCentral()
            }
            
            openapiModelgen {
                specs {
                    test {
                        // Missing required properties
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Trying to run a generation task
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateOpenApiDtosForTest")
                .withPluginClasspath()
                .buildAndFail();

        // Then: Should fail with validation error
        assertTrue(result.getOutput().contains("Configuration validation failed") || 
                  result.getOutput().contains("inputSpec") ||
                  result.getOutput().contains("modelPackage"));
    }

    @Test
    void testSuccessfulCodeGeneration() throws IOException {
        // Given: Valid configuration with spec file
        createValidSpecFile();
        
        String buildFileContent = """
            plugins {
                id 'java'
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
                    test {
                        inputSpec "${project.projectDir}/src/main/resources/test-spec.yaml"
                        modelPackage "com.example.test"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running generation
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateOpenApiDtosForTest", "--info")
                .withPluginClasspath()
                .build();

        // Then: Should generate code successfully
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateOpenApiDtosForTest").getOutcome());
        
        // Verify generated files exist in the default location (build/generated)
        File generatedDir = new File(testProjectDir, "build/generated/src/main/java/com/example/test");
        assertTrue(generatedDir.exists(), "Generated directory should exist");
        
        File[] javaFiles = generatedDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(javaFiles, "Java files should be generated");
        assertTrue(javaFiles.length > 0, "At least one Java file should be generated");
    }

    @Test
    void testIncrementalBuildBehavior() throws IOException {
        // Given: Valid configuration
        createValidSpecFile();
        
        String buildFileContent = """
            plugins {
                id 'java'
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
                    test {
                        inputSpec "${project.projectDir}/src/main/resources/test-spec.yaml"
                        modelPackage "com.example.test"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running generation twice
        BuildResult firstResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateOpenApiDtosForTest")
                .withPluginClasspath()
                .build();

        BuildResult secondResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateOpenApiDtosForTest")
                .withPluginClasspath()
                .build();

        // Then: First should succeed, second should be up-to-date
        assertEquals(TaskOutcome.SUCCESS, firstResult.task(":generateOpenApiDtosForTest").getOutcome());
        assertEquals(TaskOutcome.UP_TO_DATE, secondResult.task(":generateOpenApiDtosForTest").getOutcome());
    }

    private void createValidSpecFile() throws IOException {
        // Create resources directory
        File resourcesDir = new File(testProjectDir, "src/main/resources");
        resourcesDir.mkdirs();
        
        String specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                TestModel:
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
                        - active
                        - inactive
            """;
        
        File specFile = new File(resourcesDir, "test-spec.yaml");
        Files.write(specFile.toPath(), List.of(specContent.split("\\n")));
    }
}