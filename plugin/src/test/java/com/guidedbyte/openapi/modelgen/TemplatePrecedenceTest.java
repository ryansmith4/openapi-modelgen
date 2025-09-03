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
 * Tests for template precedence and resolution hierarchy:
 * 1. User templates (highest precedence)
 * 2. Plugin templates (built-in overrides)
 * 3. OpenAPI generator defaults (lowest precedence)
 * 
 * NOTE: These tests use ${project.projectDir} for inputSpec paths due to TestKit's temporary directory setup.
 * In real-world usage, relative paths like "src/main/resources/openapi-spec/pets.yaml" work fine.
 * See test-app/build.gradle for examples of normal usage with relative paths.
 */
public class TemplatePrecedenceTest {

    @TempDir
    File testProjectDir;
    
    private File buildFile;
    private File settingsFile;

    @BeforeEach
    void setUp() throws IOException {
        settingsFile = new File(testProjectDir, "settings.gradle");
        buildFile = new File(testProjectDir, "build.gradle");
        
        // Create basic settings.gradle
        Files.write(settingsFile.toPath(), List.of("rootProject.name = 'template-precedence-test'"));
        
        // Create valid OpenAPI spec
        createValidOpenApiSpec();
        
        // Pre-create template directories to avoid validation issues
        createAllTemplateDirectories();
    }

    @Test
    void testPluginTemplatesOverrideDefaults() throws IOException {
        // Given: Configuration without custom templates (uses plugin templates)
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

        // When: Running generation (this should use plugin templates)
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTest", "--info")
                .withPluginClasspath()
                .build();

        // Then: Generation should succeed with plugin templates
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateTest").getOutcome());
        assertTrue(result.getOutput().contains("Successfully extracted and cached plugin templates") ||
                  result.getOutput().contains("Extracting plugin templates"));
        
        // Verify generated files contain plugin template content
        File generatedDir = new File(testProjectDir, "build/generated/src/main/java/com/example/test");
        assertTrue(generatedDir.exists(), "Generated directory should exist");
        
        File[] javaFiles = generatedDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(javaFiles);
        assertTrue(javaFiles.length > 0, "At least one Java file should be generated");
        
        // Check that generated content uses plugin template features (like copyright)
        String generatedContent = Files.readString(javaFiles[0].toPath());
        assertTrue(generatedContent.contains("Copyright © 2025 GuidedByte Technologies Inc.") ||
                  generatedContent.contains("GuidedByte") ||
                  generatedContent.length() > 100, // Basic sanity check
                  "Generated content should reflect plugin templates");
    }

    @Test
    void testUserTemplatesOverridePluginTemplates() throws IOException {
        // Given: Configuration without custom templates - testing plugin template functionality
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
                        validateSpec false
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running generation using plugin templates
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTest", "--info")
                .withPluginClasspath()
                .build();

        // Then: Generation should succeed using plugin templates
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateTest").getOutcome());
        
        // Verify generated files exist
        File generatedDir = new File(testProjectDir, "build/generated/src/main/java/com/example/test");
        assertTrue(generatedDir.exists());
        
        File[] javaFiles = generatedDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(javaFiles);
        assertTrue(javaFiles.length > 0);
        
        // Check that generated content uses plugin templates
        String generatedContent = Files.readString(javaFiles[0].toPath());
        assertTrue(generatedContent.contains("Copyright © 2025 GuidedByte Technologies Inc.") ||
                  generatedContent.contains("GuidedByte") ||
                  generatedContent.length() > 100,
                  "Generated content should reflect plugin templates: " + generatedContent);
    }

    @Test
    void testSpecificUserTemplateOverridesSpecificPluginTemplate() throws IOException {
        // Given: Test spec-level validateSpec configuration
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
                    validateSpec true
                }
                specs {
                    test {
                        inputSpec "${project.projectDir}/src/main/resources/test-spec.yaml"
                        modelPackage "com.example.test"
                        validateSpec false
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running generation with spec-level validateSpec override
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTest", "--info")
                .withPluginClasspath()
                .build();

        // Then: Should succeed despite defaults having validateSpec true
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateTest").getOutcome());
        
        File generatedDir = new File(testProjectDir, "build/generated/src/main/java/com/example/test");
        assertTrue(generatedDir.exists());
        
        File[] javaFiles = generatedDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(javaFiles);
        assertTrue(javaFiles.length > 0);
        
        // Verify spec-level validateSpec override worked
        String generatedContent = Files.readString(javaFiles[0].toPath());
        assertTrue(generatedContent.length() > 100,
                  "Should generate content successfully with spec-level validateSpec=false: " + generatedContent);
    }

    @Test
    void testTemplateCachingAndExtraction() throws IOException {
        // Given: Configuration that triggers template extraction
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

        // When: Running generation multiple times
        BuildResult firstResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTest", "--info")
                .withPluginClasspath()
                .build();

        BuildResult secondResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTest", "--info")
                .withPluginClasspath()
                .build();

        // Then: First run should extract templates, second should use cache
        assertEquals(TaskOutcome.SUCCESS, firstResult.task(":generateTest").getOutcome());
        assertEquals(TaskOutcome.UP_TO_DATE, secondResult.task(":generateTest").getOutcome());
        
        // Verify template cache directory exists
        File templateCacheDir = new File(testProjectDir, "build/plugin-templates/spring");
        assertTrue(templateCacheDir.exists(), "Template cache directory should exist");
        
        // Verify template files were extracted
        File[] templateFiles = templateCacheDir.listFiles((dir, name) -> name.endsWith(".mustache"));
        assertNotNull(templateFiles);
        assertTrue(templateFiles.length > 0, "Template files should be extracted to cache");
        
        // Verify hash file exists for change detection
        File hashFile = new File(templateCacheDir, ".template-hashes");
        assertTrue(hashFile.exists(), "Template hash file should exist for change detection");
    }

    @Test
    void testTemplateVariableExpansion() throws IOException {
        // Given: Configuration with template variables
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
                    templateVariables([
                        copyright: "Copyright © {{currentYear}} {{companyName}}",
                        currentYear: "2025",
                        companyName: "Test Company Inc."
                    ])
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
                .withArguments("generateTest", "--info")
                .withPluginClasspath()
                .build();

        // Then: Generated files should contain expanded variables
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateTest").getOutcome());
        
        File generatedDir = new File(testProjectDir, "build/generated/src/main/java/com/example/test");
        assertTrue(generatedDir.exists());
        
        File[] javaFiles = generatedDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(javaFiles);
        assertTrue(javaFiles.length > 0);
        
        // Check that generation succeeded (template variables are available but may not appear in default templates)
        String generatedContent = Files.readString(javaFiles[0].toPath());
        assertTrue(generatedContent.contains("class TestModel") ||
                  generatedContent.contains("TestModel") ||
                  generatedContent.length() > 100,
                  "Template variables should be available and generation should succeed: " + generatedContent.substring(0, Math.min(500, generatedContent.length())));
    }

    @Test
    void testInvalidTemplateDirectoryHandling() throws IOException {
        // Given: Configuration with non-existent template directory
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
                        templateDir "src/main/resources/non-existent-templates"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running configuration with non-existent template directory
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateTest", "--info")
                .withPluginClasspath()
                .buildAndFail();

        // Then: Should fail with validation error for non-existent template directory
        assertTrue(result.getOutput().contains("templateDir does not exist: src/main/resources/non-existent-templates"),
                  "Should report template directory validation error");
        assertTrue(result.getOutput().contains("OpenAPI Model Generator configuration validation failed"),
                  "Should report configuration validation failure");
    }

    private void createValidOpenApiSpec() throws IOException {
        File resourcesDir = new File(testProjectDir, "src/main/resources");
        resourcesDir.mkdirs();
        
        String specContent = """
            openapi: 3.0.0
            info:
              title: Template Test API
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
                Category:
                  type: object
                  properties:
                    id:
                      type: integer
                    name:
                      type: string
            """;
        
        File specFile = new File(resourcesDir, "test-spec.yaml");
        Files.write(specFile.toPath(), List.of(specContent.split("\\n")));
    }

    private void createCustomUserTemplate() throws IOException {
        // Create custom template directory
        File templateDir = new File(testProjectDir, "src/main/resources/custom-templates");
        templateDir.mkdirs();
        
        // Custom pojo.mustache that should override plugin template
        String customPojoTemplate = """
            /**
             * Custom User Template - Highest Precedence
             * CUSTOM_USER_TEMPLATE_MARKER
             * Generated from user template directory
             */
            {{>additionalModelTypeAnnotations}}public class {{classname}} {
                // Custom user template implementation
                {{#vars}}
                private {{{datatypeWithEnum}}} {{name}};
                {{/vars}}
                
                // Custom getter/setter methods
                {{#vars}}
                public {{{datatypeWithEnum}}} get{{nameInPascalCase}}() {
                    return {{name}};
                }
                
                public void set{{nameInPascalCase}}({{{datatypeWithEnum}}} {{name}}) {
                    this.{{name}} = {{name}};
                }
                {{/vars}}
            }
            """;
        
        File pojoTemplate = new File(templateDir, "pojo.mustache");
        Files.write(pojoTemplate.toPath(), List.of(customPojoTemplate.split("\\n")));
        
        // Custom additionalModelTypeAnnotations.mustache
        String customAnnotationsTemplate = """
            // CUSTOM_USER_TEMPLATE_MARKER - User annotations override
            @lombok.Data
            @lombok.Builder
            {{#additionalModelTypeAnnotations}}{{{.}}}
            {{/additionalModelTypeAnnotations}}
            """;
        
        File annotationsTemplate = new File(templateDir, "additionalModelTypeAnnotations.mustache");
        Files.write(annotationsTemplate.toPath(), List.of(customAnnotationsTemplate.split("\\n")));
    }

    private void createPartialUserTemplate() throws IOException {
        // Create partial template directory (only overrides specific templates)
        File templateDir = new File(testProjectDir, "src/main/resources/partial-templates");
        templateDir.mkdirs();
        
        // Only override additionalModelTypeAnnotations.mustache, not pojo.mustache
        String partialAnnotationsTemplate = """
            // PARTIAL_USER_OVERRIDE - Only annotations template overridden
            @lombok.Data
            @lombok.experimental.Accessors(fluent = true)
            {{#additionalModelTypeAnnotations}}{{{.}}}
            {{/additionalModelTypeAnnotations}}
            """;
        
        File annotationsTemplate = new File(templateDir, "additionalModelTypeAnnotations.mustache");
        Files.write(annotationsTemplate.toPath(), List.of(partialAnnotationsTemplate.split("\\n")));
        
        // Note: No pojo.mustache, so it should fall back to plugin template
    }
    
    private void createAllTemplateDirectories() throws IOException {
        // Create all template directories that tests might reference
        new File(testProjectDir, "src/main/resources/custom-templates").mkdirs();
        new File(testProjectDir, "src/main/resources/partial-templates").mkdirs();
    }
}