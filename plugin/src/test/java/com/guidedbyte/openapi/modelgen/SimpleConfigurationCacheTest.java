package com.guidedbyte.openapi.modelgen;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified configuration cache compatibility tests.
 */
public class SimpleConfigurationCacheTest extends BaseTestKitTest {
    
    @Test
    public void testBasicConfigurationCacheCompatibility(@TempDir Path tempDir) throws IOException {
        File testProjectDir = tempDir.toFile();
        
        // Create basic project structure
        createBuildGradle(testProjectDir);
        createPetSpec(testProjectDir);
        
        // Test that the plugin can be applied with configuration cache enabled
        // This tests the core configuration cache compatibility of our plugin
        BuildResult result = createGradleRunner(testProjectDir)
            .withArguments("--configuration-cache", "help")
            .build();
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":help").getOutcome());
        
        // The key test: configuration cache was successfully stored without serialization errors
        assertTrue(result.getOutput().contains("Configuration cache entry stored") || 
                  result.getOutput().contains("Configuration cache entry reused"), 
                  "Configuration cache should work without serialization errors");
        
        // Second run to verify cache reuse
        BuildResult secondResult = createGradleRunner(testProjectDir)
            .withArguments("--configuration-cache", "help")
            .build();
        
        assertTrue(secondResult.getOutput().contains("Configuration cache entry reused"), 
                  "Configuration cache should be reused on second run");
    }
    
    @Test
    public void testMultiSpecConfigurationCache(@TempDir Path tempDir) throws IOException {
        File testProjectDir = tempDir.toFile();
        
        // Create project with multiple specs
        createMultiSpecBuildGradle(testProjectDir);
        createPetSpec(testProjectDir);
        createOrderSpec(testProjectDir);
        
        // Test configuration cache with complex multi-spec configuration
        // This tests that our parallel processing and complex configuration is cache-compatible
        BuildResult result = createGradleRunner(testProjectDir)
            .withArguments("--configuration-cache", "help")
            .build();
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":help").getOutcome());
        
        // Verify configuration cache works with parallel multi-spec setup
        assertTrue(result.getOutput().contains("Configuration cache entry stored") || 
                  result.getOutput().contains("Configuration cache entry reused"), 
                  "Configuration cache should work with multi-spec parallel configuration");
        
        // Verify cache reuse with complex configuration
        BuildResult secondResult = createGradleRunner(testProjectDir)
            .withArguments("--configuration-cache", "help")
            .build();
        
        assertTrue(secondResult.getOutput().contains("Configuration cache entry reused"), 
                  "Configuration cache should be reused for complex multi-spec configuration");
    }
    
    private void createBuildGradle(File testProjectDir) throws IOException {
        String buildGradle = """
            plugins {
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            openapiModelgen {
                defaults {
                    outputDir "build/generated"
                    validateSpec false
                }
                specs {
                    pets {
                        inputSpec "src/main/resources/pets.yaml"
                        modelPackage "com.example.pets"
                    }
                }
            }
            """;
        
        Files.writeString(testProjectDir.toPath().resolve("build.gradle"), buildGradle);
        Files.writeString(testProjectDir.toPath().resolve("settings.gradle"), "rootProject.name = 'config-cache-test'");
    }
    
    private void createMultiSpecBuildGradle(File testProjectDir) throws IOException {
        String buildGradle = """
            plugins {
                id 'com.guidedbyte.openapi-modelgen'
            }
            
            openapiModelgen {
                defaults {
                    outputDir "build/generated"
                    validateSpec false
                    parallel true
                }
                specs {
                    pets {
                        inputSpec "src/main/resources/pets.yaml"
                        modelPackage "com.example.pets"
                    }
                    orders {
                        inputSpec "src/main/resources/orders.yaml"
                        modelPackage "com.example.orders"
                    }
                }
            }
            """;
        
        Files.writeString(testProjectDir.toPath().resolve("build.gradle"), buildGradle);
        Files.writeString(testProjectDir.toPath().resolve("settings.gradle"), "rootProject.name = 'multi-spec-test'");
    }
    
    private void createPetSpec(File testProjectDir) throws IOException {
        String petSpec = """
            openapi: 3.0.0
            info:
              title: Pet Store API
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
                      enum: [available, pending, sold]
                PetSummary:
                  type: object
                  properties:
                    id:
                      type: integer
                      format: int64
                    name:
                      type: string
            """;
        
        File resourcesDir = new File(testProjectDir, "src/main/resources");
        resourcesDir.mkdirs();
        Files.writeString(new File(resourcesDir, "pets.yaml").toPath(), petSpec);
    }
    
    private void createOrderSpec(File testProjectDir) throws IOException {
        String orderSpec = """
            openapi: 3.0.0
            info:
              title: Order API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                Order:
                  type: object
                  required:
                    - status
                  properties:
                    id:
                      type: integer
                      format: int64
                    status:
                      type: string
                      enum: [pending, confirmed, shipped, delivered]
                    total:
                      type: number
                      format: double
                OrderItem:
                  type: object
                  properties:
                    productId:
                      type: string
                    quantity:
                      type: integer
                    price:
                      type: number
                      format: double
            """;
        
        File resourcesDir = new File(testProjectDir, "src/main/resources");
        resourcesDir.mkdirs();
        Files.writeString(new File(resourcesDir, "orders.yaml").toPath(), orderSpec);
    }
}