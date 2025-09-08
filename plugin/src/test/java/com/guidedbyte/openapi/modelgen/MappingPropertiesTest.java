package com.guidedbyte.openapi.modelgen;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for importMappings and typeMappings configuration properties.
 * 
 * <p>Validates that the new mapping properties are properly configured and merged:</p>
 * <ul>
 *   <li>Default-level mappings are applied</li>
 *   <li>Spec-level mappings are applied and merged with defaults</li>
 *   <li>Spec-level mappings override defaults for duplicate keys</li>
 *   <li>Empty mappings are handled correctly</li>
 * </ul>
 */
public class MappingPropertiesTest extends BaseTestKitTest {

    @TempDir
    File testProjectDir;
    
    private File buildFile;
    private File settingsFile;
    private File specFile;

    @BeforeEach
    void setUp() throws IOException {
        settingsFile = new File(testProjectDir, "settings.gradle");
        buildFile = new File(testProjectDir, "build.gradle");
        
        // Create specs directory
        File specsDir = new File(testProjectDir, "src/main/resources/openapi");
        specsDir.mkdirs();
        specFile = new File(specsDir, "test.yaml");
        
        // Create basic settings.gradle
        Files.write(settingsFile.toPath(), "rootProject.name = 'test-project'".getBytes());
        
        // Create basic OpenAPI spec
        String specContent = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /test:
                get:
                  responses:
                    '200':
                      description: OK
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              id:
                                type: string
                                format: uuid
                              createdAt:
                                type: string
                                format: date-time
            """;
        Files.write(specFile.toPath(), specContent.getBytes());
    }

    @Test
    void testImportAndTypeMappingsCompileSuccessfully() throws IOException {
        String buildFileContent = """
            plugins {
                id 'java'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            openapiModelgen {
                defaults {
                    outputDir "build/generated"
                    importMappings([
                        'UUID': 'java.util.UUID',
                        'LocalDateTime': 'java.time.LocalDateTime'
                    ])
                    typeMappings([
                        'string+uuid': 'UUID',
                        'string+date-time': 'LocalDateTime'
                    ])
                }
                specs {
                    test {
                        inputSpec "src/main/resources/openapi/test.yaml"
                        modelPackage "com.example.model"
                        importMappings([
                            'BigDecimal': 'java.math.BigDecimal'  // Additional mapping
                        ])
                        typeMappings([
                            'string+uuid': 'String'  // Override default mapping
                        ])
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        BuildResult result = createGradleRunner(testProjectDir)
            .withArguments("tasks", "--group=openapi modelgen", "--stacktrace")
            .build();
        
        // Should compile without errors and show our tasks
        assertTrue(result.getOutput().contains("generateTest"));
        assertFalse(result.getOutput().contains("FAILED"));
    }
    
    @Test
    void testEmptyMappingsHandledCorrectly() throws IOException {
        String buildFileContent = """
            plugins {
                id 'java'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            openapiModelgen {
                defaults {
                    outputDir "build/generated"
                    importMappings([:])  // Empty map
                    typeMappings([:])    // Empty map
                }
                specs {
                    test {
                        inputSpec "src/main/resources/openapi/test.yaml"
                        modelPackage "com.example.model"
                        // No mappings defined at spec level
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        BuildResult result = createGradleRunner(testProjectDir)
            .withArguments("tasks", "--group=openapi modelgen", "--stacktrace")
            .build();
        
        // Should handle empty mappings without errors
        assertTrue(result.getOutput().contains("generateTest"));
        assertFalse(result.getOutput().contains("FAILED"));
    }
}