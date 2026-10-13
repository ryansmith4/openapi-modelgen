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
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional coverage that matters across the supported OpenAPI Generator range (7.10.0 through 7.26.0).
 *
 * <p>Runs against the generator version selected by {@code -PopenapiGeneratorVersion}
 * (see {@link GeneratorVersion}). On 7.22.0+ these tests exercise the lazy task API
 * ({@code RegularFileProperty}/{@code DirectoryProperty}) and Worker-based execution with
 * classloader isolation; on older versions they exercise the {@code Property<String>} API.</p>
 */
public class GeneratorCompatibilityFunctionalTest extends BaseTestKitTest {

    @TempDir
    File testProjectDir;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(new File(testProjectDir, "settings.gradle").toPath(),
            "rootProject.name = 'generator-compat-test'\n");
    }

    /**
     * Regression guard for relative {@code inputSpec} paths: the plugin resolves them against the
     * project directory at configuration time, so generation works with the configuration cache
     * on both generator API generations. Before 2.2.3 the TestKit tests had to pass absolute paths.
     */
    @Test
    void relativeInputSpecGeneratesWithConfigurationCache() throws IOException {
        writeSpec("src/main/resources/openapi/pets.yaml", "Pet", "PetSummary");
        String buildFileContent = """
            plugins {
                id 'java'
            """ + GeneratorVersion.pluginLine() + """
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
                        inputSpec "src/main/resources/openapi/pets.yaml"
                        modelPackage "com.example.pets"
                    }
                }
            }
            """;
        Files.writeString(new File(testProjectDir, "build.gradle").toPath(), buildFileContent);

        BuildResult first = createGradleRunner(testProjectDir)
            .withArguments("generatePets", "--configuration-cache")
            .build();

        assertEquals(TaskOutcome.SUCCESS, outcome(first, ":generatePets"));
        assertTrue(first.getOutput().contains("Configuration cache entry stored"),
            "First run should store a configuration cache entry:\n" + first.getOutput());
        assertGeneratedModels("pets", "com/example/pets");

        BuildResult second = createGradleRunner(testProjectDir)
            .withArguments("generatePets", "--configuration-cache")
            .build();

        assertTrue(second.getOutput().contains("Configuration cache entry reused"),
            "Second run should reuse the configuration cache entry:\n" + second.getOutput());
        TaskOutcome secondOutcome = outcome(second, ":generatePets");
        assertTrue(secondOutcome == TaskOutcome.UP_TO_DATE || secondOutcome == TaskOutcome.SUCCESS,
            "Second run should be UP_TO_DATE or SUCCESS, was: " + secondOutcome);
        assertFalse(second.getOutput().contains("configuration cache problems"),
            "No configuration cache problems expected:\n" + second.getOutput());
    }

    /**
     * Two specs with {@code parallel true} under {@code --parallel}. On 7.22.0+ this exercises
     * classloader-isolated workers sharing one build; on older versions the pre-worker path.
     */
    @Test
    void twoSpecsWithParallelTrueGenerateAllModelsInParallel() throws IOException {
        writeSpec("src/main/resources/openapi/pets.yaml", "Pet", "PetSummary");
        writeSpec("src/main/resources/openapi/orders.yaml", "Order", "OrderItem");
        String buildFileContent = """
            plugins {
                id 'java'
            """ + GeneratorVersion.pluginLine() + """
                id 'com.guidedbyte.openapi-modelgen'
            }

            repositories {
                mavenCentral()
            }

            openapiModelgen {
                defaults {
                    validateSpec false
                    parallel true
                }
                specs {
                    pets {
                        inputSpec "src/main/resources/openapi/pets.yaml"
                        modelPackage "com.example.pets"
                    }
                    orders {
                        inputSpec "src/main/resources/openapi/orders.yaml"
                        modelPackage "com.example.orders"
                    }
                }
            }
            """;
        Files.writeString(new File(testProjectDir, "build.gradle").toPath(), buildFileContent);

        BuildResult result = createGradleRunner(testProjectDir)
            .withArguments("generateAllModels", "--parallel")
            .build();

        assertEquals(TaskOutcome.SUCCESS, outcome(result, ":generatePets"));
        assertEquals(TaskOutcome.SUCCESS, outcome(result, ":generateOrders"));
        assertEquals(TaskOutcome.SUCCESS, outcome(result, ":generateAllModels"));
        assertGeneratedModels("pets", "com/example/pets");
        assertGeneratedModels("orders", "com/example/orders");

        BuildResult second = createGradleRunner(testProjectDir)
            .withArguments("generateAllModels", "--parallel")
            .build();

        for (String task : List.of(":generatePets", ":generateOrders")) {
            TaskOutcome secondOutcome = outcome(second, task);
            assertTrue(secondOutcome == TaskOutcome.UP_TO_DATE || secondOutcome == TaskOutcome.SUCCESS,
                task + " second run should be UP_TO_DATE or SUCCESS, was: " + secondOutcome);
        }
    }

    private static TaskOutcome outcome(BuildResult result, String taskPath) {
        BuildTask task = result.task(taskPath);
        assertNotNull(task, "Task " + taskPath + " should have run:\n" + result.getOutput());
        return Objects.requireNonNull(task).getOutcome();
    }

    private void assertGeneratedModels(String specName, String packagePath) {
        File modelDir = new File(testProjectDir,
            "build/generated/sources/openapi/" + specName + "/src/main/java/" + packagePath);
        assertTrue(modelDir.isDirectory(), "Generated model package directory should exist: " + modelDir);
        File[] javaFiles = modelDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(javaFiles);
        assertTrue(javaFiles.length > 0, "At least one model should be generated in " + modelDir);
    }

    private void writeSpec(String relativePath, String firstSchema, String secondSchema) throws IOException {
        File specFile = new File(testProjectDir, relativePath);
        assertTrue(specFile.getParentFile().mkdirs() || specFile.getParentFile().isDirectory());
        String spec = """
            openapi: 3.0.0
            info:
              title: %s API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                %s:
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
                      enum: [active, inactive]
                %s:
                  type: object
                  properties:
                    id:
                      type: integer
                      format: int64
                    name:
                      type: string
            """.formatted(firstSchema, firstSchema, secondSchema);
        Files.writeString(specFile.toPath(), spec);
    }
}
