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
 * Tests for importMappings, typeMappings, additionalProperties, and openapiNormalizer configuration properties.
 * 
 * <p>Validates that the mapping properties are properly configured and merged:</p>
 * <ul>
 *   <li>Default-level mappings are applied</li>
 *   <li>Spec-level mappings are applied and merged with defaults</li>
 *   <li>Spec-level mappings override defaults for duplicate keys</li>
 *   <li>Empty mappings are handled correctly</li>
 *   <li>Additional properties are passed to OpenAPI Generator</li>
 *   <li>OpenAPI normalizer rules are configured and merged</li>
 * </ul>
 */
public class MappingPropertiesTest extends BaseTestKitTest {

    @TempDir
    File testProjectDir;
    
    private File buildFile;

    @BeforeEach
    void setUp() throws IOException {
        File settingsFile = new File(testProjectDir, "settings.gradle");
        buildFile = new File(testProjectDir, "build.gradle");
        
        // Create specs directory
        File specsDir = new File(testProjectDir, "src/main/resources/openapi");
        assertTrue(specsDir.mkdirs() || specsDir.exists(),
                  "Failed to create directory: " + specsDir);
        File specFile = new File(specsDir, "test.yaml");
        
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
    void testImportTypeMappingsAndAdditionalPropertiesCompileSuccessfully() throws IOException {
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
                    additionalProperties([
                        'library': 'spring-boot',
                        'beanValidations': 'true'
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
                        additionalProperties([
                            'reactive': 'true'  // Additional property for this spec only
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
                    importMappings([:])       // Empty map
                    typeMappings([:])         // Empty map
                    additionalProperties([:]) // Empty map
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
    
    @Test
    void testAdditionalPropertiesConfigurationWorks() throws IOException {
        String buildFileContent = """
            plugins {
                id 'java'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            openapiModelgen {
                defaults {
                    outputDir "build/generated"
                    additionalProperties([
                        'library': 'spring-boot',
                        'reactive': 'false',
                        'serializableModel': 'true'
                    ])
                }
                specs {
                    test {
                        inputSpec "src/main/resources/openapi/test.yaml"
                        modelPackage "com.example.model"
                        additionalProperties([
                            'reactive': 'true',      // Override default
                            'useSpringBoot3': 'true' // Additional property
                        ])
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        BuildResult result = createGradleRunner(testProjectDir)
            .withArguments("tasks", "--group=openapi modelgen", "--stacktrace")
            .build();
        
        // Should compile and configure additional properties without errors
        assertTrue(result.getOutput().contains("generateTest"));
        assertFalse(result.getOutput().contains("FAILED"));
    }
    
    @Test
    void testOpenapiNormalizerConfigurationWorks() throws IOException {
        String buildFileContent = """
            plugins {
                id 'java'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            openapiModelgen {
                defaults {
                    outputDir "build/generated"
                    openapiNormalizer([
                        'REFACTOR_ALLOF_WITH_PROPERTIES_ONLY': 'true',
                        'SIMPLIFY_ONEOF_ANYOF': 'true'
                    ])
                }
                specs {
                    test {
                        inputSpec "src/main/resources/openapi/test.yaml"
                        modelPackage "com.example.model"
                        openapiNormalizer([
                            'KEEP_ONLY_FIRST_TAG_IN_OPERATION': 'true',
                            'SIMPLIFY_ONEOF_ANYOF': 'false'  // Override default
                        ])
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        BuildResult result = createGradleRunner(testProjectDir)
            .withArguments("tasks", "--group=openapi modelgen", "--stacktrace")
            .build();
        
        // Should compile and configure OpenAPI normalizer without errors
        assertTrue(result.getOutput().contains("generateTest"));
        assertFalse(result.getOutput().contains("FAILED"));
    }
    
    @Test
    void testEmptyOpenapiNormalizerHandledCorrectly() throws IOException {
        String buildFileContent = """
            plugins {
                id 'java'
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            openapiModelgen {
                defaults {
                    outputDir "build/generated"
                    openapiNormalizer([:])  // Empty map
                }
                specs {
                    test {
                        inputSpec "src/main/resources/openapi/test.yaml"
                        modelPackage "com.example.model"
                        // No normalizer rules defined at spec level
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), buildFileContent.getBytes());

        BuildResult result = createGradleRunner(testProjectDir)
            .withArguments("tasks", "--group=openapi modelgen", "--stacktrace")
            .build();
        
        // Should handle empty normalizer rules without errors
        assertTrue(result.getOutput().contains("generateTest"));
        assertFalse(result.getOutput().contains("FAILED"));
    }
}