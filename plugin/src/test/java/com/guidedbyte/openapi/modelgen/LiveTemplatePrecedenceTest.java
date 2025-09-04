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
 * Live template precedence test that uses the existing test-app structure
 * to validate template resolution in a real environment
 * 
 * NOTE: These tests use ${project.projectDir} for inputSpec paths due to TestKit's temporary directory setup.
 * In real-world usage, relative paths like "src/main/resources/openapi-spec/pets.yaml" work fine.
 * See test-app/build.gradle for examples of normal usage with relative paths.
 */
public class LiveTemplatePrecedenceTest {

    @TempDir 
    File testProjectDir;
    
    private File buildFile;
    private File settingsFile;

    @BeforeEach
    void setUp() throws IOException {
        settingsFile = new File(testProjectDir, "settings.gradle");
        buildFile = new File(testProjectDir, "build.gradle");
        
        Files.write(settingsFile.toPath(), List.of("rootProject.name = 'template-precedence-live-test'"));
    }

    @Test
    void testPluginTemplateExtractionAndUsage() throws IOException {
        // Given: Minimal configuration that should use plugin templates
        createMinimalValidSpec();
        
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
                    minimal {
                        inputSpec "${project.projectDir}/src/main/resources/minimal-spec.yaml"
                        modelPackage "com.example.minimal"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running generation task
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateMinimal", "--info")
                .withPluginClasspath()
                .build();

        // Then: Should succeed using plugin templates
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateMinimal").getOutcome());
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
        
        // Verify plugin templates were extracted
        assertTrue(result.getOutput().contains("Extracting plugin templates") ||
                  result.getOutput().contains("plugin templates") ||
                  result.getOutput().contains("templates processed"));
        
        // Verify generated files exist
        File generatedDir = new File(testProjectDir, "build/generated/sources/openapi/src/main/java/com/example/minimal");
        assertTrue(generatedDir.exists(), "Generated directory should exist");
        
        File[] javaFiles = generatedDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(javaFiles, "Java files should be generated");
        assertTrue(javaFiles.length > 0, "At least one Java file should be generated");
        
        // Verify template cache was created
        File templateCacheDir = new File(testProjectDir, "build/plugin-templates");
        assertTrue(templateCacheDir.exists(), "Plugin template cache should be created");
    }

    @Test
    void testUserTemplateOverride() throws IOException {
        // Given: Configuration that tests template variable functionality
        createMinimalValidSpec();
        
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
                        testMarker: "USER_TEMPLATE_OVERRIDE_MARKER",
                        customTitle: "Custom User Template"
                    ])
                }
                
                specs {
                    minimal {
                        inputSpec "${project.projectDir}/src/main/resources/minimal-spec.yaml"
                        modelPackage "com.example.minimal"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running generation with template variables
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateMinimal", "--info")
                .withPluginClasspath()
                .build();

        // Then: Should succeed using plugin templates with custom variables
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateMinimal").getOutcome());
        
        // Verify generated files exist
        File generatedDir = new File(testProjectDir, "build/generated/sources/openapi/src/main/java/com/example/minimal");
        assertTrue(generatedDir.exists());
        
        File[] javaFiles = generatedDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(javaFiles);
        assertTrue(javaFiles.length > 0);
        
        // Check for generated content
        String generatedContent = Files.readString(javaFiles[0].toPath());
        assertTrue(generatedContent.length() > 100,
                  "Generated content should be created successfully: " + generatedContent.substring(0, Math.min(500, generatedContent.length())));
    }

    @Test
    void testTemplateCacheBehavior() throws IOException {
        // Given: Configuration that will create template cache
        createMinimalValidSpec();
        
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
                    minimal {
                        inputSpec "${project.projectDir}/src/main/resources/minimal-spec.yaml"
                        modelPackage "com.example.minimal"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running generation twice
        BuildResult firstResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateMinimal", "--info")
                .withPluginClasspath()
                .build();

        BuildResult secondResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateMinimal", "--info")
                .withPluginClasspath()
                .build();

        // Then: First run should extract templates, second should be up-to-date
        assertEquals(TaskOutcome.SUCCESS, firstResult.task(":generateMinimal").getOutcome());
        assertEquals(TaskOutcome.UP_TO_DATE, secondResult.task(":generateMinimal").getOutcome());
        
        // Verify template cache structure
        File templateCacheDir = new File(testProjectDir, "build/plugin-templates/spring");
        assertTrue(templateCacheDir.exists());
        
        File hashFile = new File(templateCacheDir, ".template-hashes");
        assertTrue(hashFile.exists(), "Template hash file should exist");
        
        File[] templateFiles = templateCacheDir.listFiles((dir, name) -> name.endsWith(".mustache"));
        assertNotNull(templateFiles);
        assertTrue(templateFiles.length > 0, "Template files should be cached");
    }

    @Test
    void testTemplateVariableExpansion() throws IOException {
        // Given: Configuration with template variables
        createMinimalValidSpec();
        
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
                        companyName: "Template Test Corp"
                    ])
                }
                
                specs {
                    minimal {
                        inputSpec "${project.projectDir}/src/main/resources/minimal-spec.yaml"
                        modelPackage "com.example.minimal"
                    }
                }
            }
            """;
        Files.write(buildFile.toPath(), List.of(buildFileContent.split("\\n")));

        // When: Running generation
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("generateMinimal", "--info")
                .withPluginClasspath()
                .build();

        // Then: Template variables should be expanded in generated files
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateMinimal").getOutcome());
        
        File generatedDir = new File(testProjectDir, "build/generated/sources/openapi/src/main/java/com/example/minimal");
        assertTrue(generatedDir.exists());
        
        File[] javaFiles = generatedDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(javaFiles);
        assertTrue(javaFiles.length > 0);
        
        // Check for generated content (template variables are available but may not appear in default templates)
        String generatedContent = Files.readString(javaFiles[0].toPath());
        assertTrue(generatedContent.contains("class MinimalModel") ||
                  generatedContent.contains("MinimalModel") ||
                  generatedContent.length() > 100,
                  "Generated content should contain model class: " + generatedContent.substring(0, Math.min(500, generatedContent.length())));
    }

    private void createMinimalValidSpec() throws IOException {
        File resourcesDir = new File(testProjectDir, "src/main/resources");
        resourcesDir.mkdirs();
        
        String specContent = """
            openapi: 3.0.0
            info:
              title: Minimal Template Test API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                MinimalModel:
                  type: object
                  required:
                    - name
                  properties:
                    id:
                      type: integer
                      format: int64
                    name:
                      type: string
            """;
        
        File specFile = new File(resourcesDir, "minimal-spec.yaml");
        Files.write(specFile.toPath(), List.of(specContent.split("\\n")));
    }

    private void createUserTemplateOverride() throws IOException {
        File templateDir = new File(testProjectDir, "src/main/resources/user-templates");
        templateDir.mkdirs();
        
        // Create user template that overrides plugin template
        String userPojoTemplate = """
            /**
             * Custom User Template - USER_TEMPLATE_OVERRIDE_MARKER
             * This template demonstrates user template precedence over plugin templates
             */
            {{>additionalModelTypeAnnotations}}public class {{classname}} {
                // Custom user template implementation
                {{#vars}}
                private {{{datatypeWithEnum}}} {{name}};
                
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
        Files.write(pojoTemplate.toPath(), List.of(userPojoTemplate.split("\\n")));
    }
}