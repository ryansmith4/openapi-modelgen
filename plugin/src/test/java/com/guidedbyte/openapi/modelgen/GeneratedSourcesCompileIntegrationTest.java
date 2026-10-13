package com.guidedbyte.openapi.modelgen;

import com.guidedbyte.openapi.modelgen.testutil.GeneratorVersion;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compiles generated models through the plugin's automatic Java integration.
 *
 * <p>Regression guard for two defects fixed in 2.2.3 that only showed up together at compile time:
 * the {@code generateModelTests} / {@code generateApiTests} / {@code generate*Documentation} DSL
 * flags were never applied to the generate task, so the plain {@code java} generator wrote model
 * unit tests and docs into the spec output directory; and the whole generated-sources base
 * directory was registered as a main source directory, so those JUnit tests were compiled as main
 * code and {@code compileJava} failed with "package org.junit.jupiter.api does not exist".</p>
 */
public class GeneratedSourcesCompileIntegrationTest extends BaseTestKitTest {

    @TempDir
    File testProjectDir;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(new File(testProjectDir, "settings.gradle").toPath(),
            "rootProject.name = 'generated-sources-compile-test'\n");
    }

    @Test
    void javaGeneratorModelsCompileWithoutGeneratedTestsOrDocs() throws IOException {
        writeSpec("src/main/resources/openapi/orders.yaml");
        String buildFileContent = """
            plugins {
                id 'java'
            """ + GeneratorVersion.pluginLine() + """
                id 'com.guidedbyte.openapi-modelgen'
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation 'com.fasterxml.jackson.core:jackson-annotations:2.18.2'
                implementation 'io.swagger.core.v3:swagger-annotations:2.2.19'
                implementation 'jakarta.validation:jakarta.validation-api:3.0.2'
                implementation 'org.hibernate.validator:hibernate-validator:8.0.1.Final'
                implementation 'jakarta.annotation:jakarta.annotation-api:2.1.1'
            }

            openapiModelgen {
                defaults {
                    validateSpec false
                }
                specs {
                    orders {
                        inputSpec "src/main/resources/openapi/orders.yaml"
                        modelPackage "com.example.orders"
                        generator "java"
                        additionalProperties([
                            'library': 'native',
                            'jackson': 'true'
                        ])
                        configOptions([
                            supportUrlQuery: 'false'
                        ])
                    }
                }
            }
            """;
        Files.writeString(new File(testProjectDir, "build.gradle").toPath(), buildFileContent);

        BuildResult result = createGradleRunner(testProjectDir)
            .withArguments("compileJava")
            .build();

        assertEquals(TaskOutcome.SUCCESS, outcome(result, ":generateOrders"));
        assertEquals(TaskOutcome.SUCCESS, outcome(result, ":compileJava"));

        File specOutputDir = new File(testProjectDir, "build/generated/sources/openapi/orders");
        assertTrue(new File(specOutputDir, "src/main/java/com/example/orders/OrderDto.java").isFile(),
            "model should be generated under src/main/java");
        assertFalse(new File(specOutputDir, "src/test").exists(),
            "generateModelTests defaults to false, so no generated tests are expected");
        assertFalse(new File(specOutputDir, "docs").exists(),
            "generateModelDocumentation defaults to false, so no generated docs are expected");
        assertTrue(new File(testProjectDir, "build/classes/java/main/com/example/orders/OrderDto.class").isFile(),
            "generated model should be compiled into the main output");
    }

    @Test
    void optInGeneratedModelTestsStayOutOfTheMainSourceSet() throws IOException {
        writeSpec("src/main/resources/openapi/orders.yaml");
        String buildFileContent = """
            plugins {
                id 'java'
            """ + GeneratorVersion.pluginLine() + """
                id 'com.guidedbyte.openapi-modelgen'
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation 'com.fasterxml.jackson.core:jackson-annotations:2.18.2'
                implementation 'io.swagger.core.v3:swagger-annotations:2.2.19'
                implementation 'jakarta.validation:jakarta.validation-api:3.0.2'
                implementation 'org.hibernate.validator:hibernate-validator:8.0.1.Final'
                implementation 'jakarta.annotation:jakarta.annotation-api:2.1.1'
            }

            openapiModelgen {
                defaults {
                    validateSpec false
                    generateModelTests true
                }
                specs {
                    orders {
                        inputSpec "src/main/resources/openapi/orders.yaml"
                        modelPackage "com.example.orders"
                        generator "java"
                        additionalProperties([
                            'library': 'native',
                            'jackson': 'true'
                        ])
                        configOptions([
                            supportUrlQuery: 'false'
                        ])
                    }
                }
            }
            """;
        Files.writeString(new File(testProjectDir, "build.gradle").toPath(), buildFileContent);

        // No JUnit on the classpath: compileJava can only succeed if the generated tests are not
        // part of the main source set
        BuildResult result = createGradleRunner(testProjectDir)
            .withArguments("compileJava")
            .build();

        assertEquals(TaskOutcome.SUCCESS, outcome(result, ":compileJava"));
        File generatedTestDir = new File(testProjectDir, "build/generated/sources/openapi/orders/src/test/java");
        assertTrue(generatedTestDir.isDirectory(), "generateModelTests true should produce src/test/java");
    }

    private static TaskOutcome outcome(BuildResult result, String taskPath) {
        BuildTask task = result.task(taskPath);
        assertNotNull(task, "Task " + taskPath + " should have run:\n" + result.getOutput());
        return task.getOutcome();
    }

    private void writeSpec(String relativePath) throws IOException {
        File specFile = new File(testProjectDir, relativePath);
        assertTrue(specFile.getParentFile().mkdirs() || specFile.getParentFile().isDirectory());
        String spec = """
            openapi: 3.0.0
            info:
              title: Orders API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                Order:
                  type: object
                  required:
                    - id
                  properties:
                    id:
                      type: integer
                      format: int64
                    total:
                      type: number
                      format: double
                    status:
                      type: string
                      enum: [placed, shipped]
                    items:
                      type: array
                      items:
                        $ref: '#/components/schemas/OrderItem'
                OrderItem:
                  type: object
                  properties:
                    sku:
                      type: string
                    quantity:
                      type: integer
            """;
        Files.writeString(specFile.toPath(), spec);
    }
}
