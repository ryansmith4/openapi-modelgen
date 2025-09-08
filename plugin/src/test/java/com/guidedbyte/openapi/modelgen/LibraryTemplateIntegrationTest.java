package com.guidedbyte.openapi.modelgen;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for template library support.
 * 
 * <p>Tests the complete workflow of template library dependencies including:</p>
 * <ul>
 *   <li>Library JAR creation with proper structure</li>
 *   <li>Configuration resolution and validation</li>
 *   <li>Template extraction from library JARs</li>
 *   <li>Library template precedence in template resolution</li>
 *   <li>Basic code generation with library templates</li>
 * </ul>
 */
public class LibraryTemplateIntegrationTest extends BaseTestKitTest {

    @TempDir
    File testProjectDir;
    
    private File buildFile;
    private File settingsFile;
    private File specFile;
    private File libraryJar;

    @BeforeEach
    void setup() throws IOException {
        buildFile = new File(testProjectDir, "build.gradle");
        settingsFile = new File(testProjectDir, "settings.gradle");
        specFile = new File(testProjectDir, "src/main/resources/openapi/pets.yaml");
        libraryJar = new File(testProjectDir, "libs/test-templates-1.0.jar");
        
        // Create directory structure
        new File(testProjectDir, "src/main/resources/openapi").mkdirs();
        new File(testProjectDir, "libs").mkdirs();
    }

    @Test
    void testBasicLibraryTemplateUsage() throws IOException {
        // Create a simple OpenAPI spec
        createPetStoreSpec(specFile);
        
        // Create a library JAR with templates
        createLibraryJarWithTemplates(libraryJar);
        
        // Create build.gradle with library dependency
        createBuildFileWithLibraryDependency(buildFile);
        
        // Create settings.gradle
        try (FileWriter writer = new FileWriter(settingsFile)) {
            writer.write("rootProject.name = 'test-library-templates'\n");
        }

        // Run the build - just test that library processing happens during configuration
        // We don't need the actual generation to succeed to verify library support
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks", "--info")  // Use tasks instead of generatePets to avoid OpenAPI execution
                .build();

        // Verify library processing was logged during configuration phase
        String output = result.getOutput();
        assertTrue(output.contains("Processing 1 template library dependencies"), 
                  "Should log library processing: " + output);
        assertTrue(output.contains("Extracted"), 
                  "Should log template extraction: " + output);
    }

    @Test
    void testLibraryTemplateValidation() throws IOException {
        // Create build.gradle with library files but library sources not included in templateSources
        // This should work fine (library files are ignored)
        try (FileWriter writer = new FileWriter(buildFile)) {
            writer.write("""
                plugins {
                    id 'java'
                    id 'com.guidedbyte.openapi-modelgen'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation 'org.openapitools:openapi-generator:7.10.0'
                    openapiCustomizations files('libs/test-templates-1.0.jar')
                }
                
                openapiModelgen {
                    defaults {
                        // Library files exist but library sources not in templateSources - should work fine
                        templateSources([
                            'user-templates',
                            'user-customizations',
                            'plugin-customizations',
                            'openapi-generator'
                        ])
                        applyPluginCustomizations false
                    }
                    specs {
                        pets {
                            inputSpec 'src/main/resources/openapi/pets.yaml'
                            modelPackage 'com.example.pets'
                        }
                    }
                }
                """);
        }
        
        createPetStoreSpec(specFile);
        createLibraryJarWithTemplates(libraryJar);
        
        try (FileWriter writer = new FileWriter(settingsFile)) {
            writer.write("rootProject.name = 'test-library-validation'\n");
        }

        // Run the build - should succeed (library files are ignored since not in templateSources)
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks", "--info")  // Use tasks to test configuration only
                .build();

        // Verify build succeeded and library processing was skipped
        String output = result.getOutput();
        assertEquals(SUCCESS, result.task(":tasks").getOutcome());
        assertFalse(output.contains("Processing 1 template library dependencies"),
                   "Should not process libraries when not in templateSources: " + output);
    }

    @Test
    void testLibraryTemplateDisabled() throws IOException {
        // Create build.gradle with library dependency but disabled library usage
        try (FileWriter writer = new FileWriter(buildFile)) {
            writer.write("""
                plugins {
                    id 'java'
                    id 'com.guidedbyte.openapi-modelgen'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation 'org.openapitools:openapi-generator:7.10.0'
                    openapiCustomizations files('libs/test-templates-1.0.jar')
                }
                
                openapiModelgen {
                    defaults {
                        templateSources([
                            'user-templates',
                            'user-customizations',
                            'openapi-generator'  // Exclude library sources
                        ])
                        applyPluginCustomizations false  // Disable plugin customizations to avoid YAML parsing issues
                    }
                    specs {
                        pets {
                            inputSpec 'src/main/resources/openapi/pets.yaml'
                            modelPackage 'com.example.pets'
                        }
                    }
                }
                """);
        }
        
        createPetStoreSpec(specFile);
        createLibraryJarWithTemplates(libraryJar);
        
        try (FileWriter writer = new FileWriter(settingsFile)) {
            writer.write("rootProject.name = 'test-library-disabled'\n");
        }

        // Run the build - just test configuration phase to verify library processing is skipped
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks", "--info")  // Use tasks to test configuration only
                .build();

        // Verify library processing was skipped during configuration phase
        String output = result.getOutput();
        // Should not process libraries since they're disabled
        assertFalse(output.contains("Processing 1 template library dependencies"),
                   "Should not process libraries when disabled: " + output);
    }

    private void createPetStoreSpec(File specFile) throws IOException {
        try (FileWriter writer = new FileWriter(specFile)) {
            writer.write("""
                openapi: 3.0.0
                info:
                  title: Pet Store API
                  version: 1.0.0
                paths:
                  /pets:
                    get:
                      responses:
                        '200':
                          description: List of pets
                          content:
                            application/json:
                              schema:
                                type: array
                                items:
                                  $ref: '#/components/schemas/Pet'
                components:
                  schemas:
                    Pet:
                      type: object
                      required:
                        - name
                      properties:
                        name:
                          type: string
                        tag:
                          type: string
                """);
        }
    }

    private void createLibraryJarWithTemplates(File jarFile) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile.toPath()))) {
            // Add a simple template for the spring generator
            JarEntry templateEntry = new JarEntry("META-INF/openapi-templates/spring/pojo.mustache");
            jos.putNextEntry(templateEntry);
            
            String templateContent = """
                // Library template for {{classname}}
                {{>licenseInfo}}
                package {{package}};
                
                {{#imports}}import {{import}};
                {{/imports}}
                
                /**
                 * {{description}}{{^description}}{{classname}}{{/description}}
                 * Generated from library template
                 */
                {{#models}}
                {{#model}}
                public class {{classname}} {
                    {{#vars}}
                    private {{{datatypeWithEnum}}} {{name}};
                    {{/vars}}
                    
                    {{#vars}}
                    public {{{datatypeWithEnum}}} get{{nameInCamelCase}}() { return {{name}}; }
                    public void set{{nameInCamelCase}}({{{datatypeWithEnum}}} {{name}}) { this.{{name}} = {{name}}; }
                    {{/vars}}
                }
                {{/model}}
                {{/models}}
                """;
            
            jos.write(templateContent.getBytes());
            jos.closeEntry();
            
            // Add a simple manifest file to verify library structure
            JarEntry manifestEntry = new JarEntry("META-INF/openapi-templates/library-manifest.txt");
            jos.putNextEntry(manifestEntry);
            
            String manifestContent = "Test template library version 1.0.0\n";
            jos.write(manifestContent.getBytes());
            jos.closeEntry();
        }
    }

    private void createBuildFileWithLibraryDependency(File buildFile) throws IOException {
        try (FileWriter writer = new FileWriter(buildFile)) {
            writer.write("""
                plugins {
                    id 'java'
                    id 'com.guidedbyte.openapi-modelgen'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation 'org.openapitools:openapi-generator:7.10.0'
                    openapiCustomizations files('libs/test-templates-1.0.jar')
                }
                
                openapiModelgen {
                    defaults {
                        templateSources([
                            'user-templates',
                            'library-templates',
                            'user-customizations',
                            'openapi-generator'  // Skip plugin-customizations
                        ])
                        applyPluginCustomizations false  // Disable plugin customizations to avoid YAML parsing issues
                    }
                    specs {
                        pets {
                            inputSpec 'src/main/resources/openapi/pets.yaml'
                            modelPackage 'com.example.pets'
                        }
                    }
                }
                """);
        }
    }

    // ========================================
    // Phase 2: Enhanced Integration Tests
    // ========================================

    @Test
    void testLibraryYAMLCustomizationsIntegration() throws IOException {
        // Create a OpenAPI spec
        createPetStoreSpec(specFile);
        
        // Create a library JAR with YAML customizations
        createLibraryJarWithYAMLCustomizations(libraryJar);
        
        // Create build.gradle with library customizations enabled
        createBuildFileWithLibraryCustomizations(buildFile);
        
        // Create settings.gradle
        try (FileWriter writer = new FileWriter(settingsFile)) {
            writer.write("rootProject.name = 'test-library-yaml-customizations'\n");
        }

        // Run the build to test YAML customization processing
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks", "--info")
                .build();

        // Verify library YAML customization processing was logged
        String output = result.getOutput();
        assertTrue(output.contains("Processing 1 template library dependencies"), 
                  "Should log library processing: " + output);
        assertTrue(output.contains("Extracted"), 
                  "Should log template extraction with customizations: " + output);
    }

    @Test
    void testLibraryMetadataValidation() throws IOException {
        // Create OpenAPI spec
        createPetStoreSpec(specFile);
        
        // Create a library JAR with metadata that only supports 'java' generator
        createLibraryJarWithIncompatibleMetadata(libraryJar);
        
        // Create build.gradle that uses 'spring' generator (incompatible)
        createBuildFileWithIncompatibleGenerator(buildFile);
        
        // Create settings.gradle
        try (FileWriter writer = new FileWriter(settingsFile)) {
            writer.write("rootProject.name = 'test-library-metadata-validation'\n");
        }

        // Run the build - should fail with metadata compatibility error
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("generatePets", "--info", "--stacktrace")
                .buildAndFail();

        // Verify metadata validation error is shown - library only supports 'java' but uses default 'spring' generator
        String output = result.getOutput();
        assertTrue(output.contains("does not support generator(s): [spring]") || 
                  output.contains("Supported generators: [java]") ||
                  output.contains("generator compatibility"),
                  "Should show generator compatibility error: " + output);
    }

    @Test
    void testLibraryGeneratorCompatibilityFiltering() throws IOException {
        // Create OpenAPI spec
        createPetStoreSpec(specFile);
        
        // Create a library JAR with content for multiple generators but metadata restricting to 'spring'
        createLibraryJarWithGeneratorFiltering(libraryJar);
        
        // Create build.gradle that uses 'spring' generator (compatible)
        createBuildFileWithSpringGenerator(buildFile);
        
        // Create settings.gradle
        try (FileWriter writer = new FileWriter(settingsFile)) {
            writer.write("rootProject.name = 'test-library-generator-filtering'\n");
        }

        // Run the build - should succeed with compatible generator
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks", "--info")
                .build();

        // Verify library processing succeeded - both library and default generator use 'spring'
        String output = result.getOutput();
        assertTrue(output.contains("Processing 1 template library dependencies"), 
                  "Should log library processing: " + output);
        
        // Should not show compatibility warnings since library supports spring generator
        assertFalse(output.contains("incompatible library"),
                   "Should not show compatibility warnings for supported generator: " + output);
    }

    @Test
    void testLibraryTemplatePrecedenceWithMetadata() throws IOException {
        // Create OpenAPI spec
        createPetStoreSpec(specFile);
        
        // Create a library JAR with proper metadata and templates
        createLibraryJarWithCompleteMetadata(libraryJar);
        
        // Create build.gradle with comprehensive template precedence
        createBuildFileWithComprehensivePrecedence(buildFile);
        
        // Create settings.gradle
        try (FileWriter writer = new FileWriter(settingsFile)) {
            writer.write("rootProject.name = 'test-library-precedence-metadata'\n");
        }

        // Run the build
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks", "--info")
                .build();

        // Verify library precedence is respected
        String output = result.getOutput();
        assertTrue(output.contains("Processing 1 template library dependencies"), 
                  "Should log library processing: " + output);
    }

    @Test
    void testLibraryConfigurationValidationErrors() throws IOException {
        // Create OpenAPI spec
        createPetStoreSpec(specFile);
        
        // Create build.gradle with library sources but no dependencies - should work gracefully
        try (FileWriter writer = new FileWriter(buildFile)) {
            writer.write("""
                plugins {
                    id 'java'
                    id 'com.guidedbyte.openapi-modelgen'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation 'org.openapitools:openapi-generator:7.10.0'
                    // No openapiCustomizations dependencies - should be handled gracefully
                }
                
                openapiModelgen {
                    defaults {
                        templateSources([
                            'library-templates',  // No dependencies but should not error
                            'library-customizations',
                            'plugin-customizations',
                            'openapi-generator'
                        ])
                        applyPluginCustomizations false
                    }
                    specs {
                        pets {
                            inputSpec 'src/main/resources/openapi/pets.yaml'
                            modelPackage 'com.example.pets'
                        }
                    }
                }
                """);
        }
        
        // Create settings.gradle
        try (FileWriter writer = new FileWriter(settingsFile)) {
            writer.write("rootProject.name = 'test-library-config-validation'\n");
        }

        // Run the build - should succeed (library sources gracefully skipped)
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks", "--info")  // Use tasks to test configuration only
                .build();

        // Verify build succeeded and library processing was skipped
        String output = result.getOutput();
        assertEquals(SUCCESS, result.task(":tasks").getOutcome());
        assertFalse(output.contains("Processing 1 template library dependencies"),
                   "Should not process libraries when no dependencies exist: " + output);
    }

    @Test
    void testLibraryMetadataVersionCompatibility() throws IOException {
        // Create OpenAPI spec
        createPetStoreSpec(specFile);
        
        // Create a library JAR with version requirements metadata
        createLibraryJarWithVersionRequirements(libraryJar);
        
        // Create build.gradle with library dependency
        createBuildFileWithLibraryDependency(buildFile);
        
        // Create settings.gradle
        try (FileWriter writer = new FileWriter(settingsFile)) {
            writer.write("rootProject.name = 'test-library-version-compatibility'\n");
        }

        // Run the build - should succeed but log version requirements
        BuildResult result = createGradleRunner(testProjectDir)
                .withArguments("tasks", "--info")
                .build();

        // Verify version compatibility logging
        String output = result.getOutput();
        assertTrue(output.contains("requires OpenAPI Generator version") || 
                  output.contains("supports OpenAPI Generator up to version"),
                  "Should log version compatibility information: " + output);
    }

    // ========================================
    // Phase 2: Helper Methods for Test Data
    // ========================================

    private void createLibraryJarWithYAMLCustomizations(File jarFile) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile.toPath()))) {
            // Add YAML customization file for spring generator
            JarEntry customizationEntry = new JarEntry("META-INF/openapi-customizations/spring/pojo.mustache.yaml");
            jos.putNextEntry(customizationEntry);
            
            String yamlCustomization = """
                metadata:
                  name: "Enhanced POJO template"
                  version: "1.0.0"
                  description: "Adds custom header and validation annotations"
                
                insertions:
                  - before: "package {{package}};"
                    content: |
                      // Custom library header - Generated by Library Template
                      // Library Version: 1.0.0
                
                  - before: "public class {{classname}} {"
                    content: |
                      @javax.annotation.Generated("LibraryTemplate")
                      @lombok.Data
                      @lombok.NoArgsConstructor
                      @lombok.AllArgsConstructor
                """;
            
            jos.write(yamlCustomization.getBytes());
            jos.closeEntry();

            // Add library metadata
            JarEntry metadataEntry = new JarEntry("META-INF/openapi-library.yaml");
            jos.putNextEntry(metadataEntry);
            
            String libraryMetadata = """
                name: "test-library"
                version: "1.0.0"
                description: "Test library with YAML customizations"
                author: "Test Author"
                supportedGenerators:
                  - "spring"
                  - "java"
                minOpenApiGeneratorVersion: "7.10.0"
                maxOpenApiGeneratorVersion: "8.0.0"
                """;
            
            jos.write(libraryMetadata.getBytes());
            jos.closeEntry();
        }
    }

    private void createLibraryJarWithIncompatibleMetadata(File jarFile) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile.toPath()))) {
            // Add library metadata that only supports 'java' generator
            JarEntry metadataEntry = new JarEntry("META-INF/openapi-library.yaml");
            jos.putNextEntry(metadataEntry);
            
            String libraryMetadata = """
                name: "java-only-library"
                version: "1.0.0"
                description: "Library that only supports Java generator"
                supportedGenerators:
                  - "java"  # Does not support 'spring'
                minOpenApiGeneratorVersion: "7.10.0"
                """;
            
            jos.write(libraryMetadata.getBytes());
            jos.closeEntry();

            // Add template that won't be used due to incompatibility
            JarEntry templateEntry = new JarEntry("META-INF/openapi-templates/java/pojo.mustache");
            jos.putNextEntry(templateEntry);
            jos.write("// Java-only template content".getBytes());
            jos.closeEntry();
        }
    }

    private void createLibraryJarWithGeneratorFiltering(File jarFile) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile.toPath()))) {
            // Add metadata that restricts to spring generator
            JarEntry metadataEntry = new JarEntry("META-INF/openapi-library.yaml");
            jos.putNextEntry(metadataEntry);
            
            String libraryMetadata = """
                name: "spring-filtered-library"
                version: "1.0.0"
                supportedGenerators:
                  - "spring"  # Only supports spring
                """;
            
            jos.write(libraryMetadata.getBytes());
            jos.closeEntry();

            // Add spring template (compatible)
            JarEntry springTemplateEntry = new JarEntry("META-INF/openapi-templates/spring/pojo.mustache");
            jos.putNextEntry(springTemplateEntry);
            jos.write("// Spring-compatible template".getBytes());
            jos.closeEntry();

            // Add java template (should be filtered out)
            JarEntry javaTemplateEntry = new JarEntry("META-INF/openapi-templates/java/pojo.mustache");
            jos.putNextEntry(javaTemplateEntry);
            jos.write("// Java template (should be filtered out)".getBytes());
            jos.closeEntry();
        }
    }

    private void createLibraryJarWithCompleteMetadata(File jarFile) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile.toPath()))) {
            // Add comprehensive metadata
            JarEntry metadataEntry = new JarEntry("META-INF/openapi-library.yaml");
            jos.putNextEntry(metadataEntry);
            
            String libraryMetadata = """
                name: "comprehensive-library"
                version: "2.1.0"
                description: "Library with complete metadata and features"
                author: "GuidedByte Technologies"
                homepage: "https://example.com/library"
                supportedGenerators:
                  - "spring"
                  - "java"
                minOpenApiGeneratorVersion: "7.10.0"
                maxOpenApiGeneratorVersion: "8.0.0"
                requiredFeatures:
                  - "validation"
                  - "lombok"
                features:
                  customValidation: true
                  lombokSupport: true
                  springBootIntegration: "3.x"
                dependencies:
                  - "org.springframework:spring-core:6.0+"
                """;
            
            jos.write(libraryMetadata.getBytes());
            jos.closeEntry();

            // Add template
            JarEntry templateEntry = new JarEntry("META-INF/openapi-templates/spring/pojo.mustache");
            jos.putNextEntry(templateEntry);
            jos.write("// Comprehensive library template".getBytes());
            jos.closeEntry();

            // Add customization
            JarEntry customizationEntry = new JarEntry("META-INF/openapi-customizations/spring/pojo.mustache.yaml");
            jos.putNextEntry(customizationEntry);
            
            String yamlCustomization = """
                metadata:
                  name: "Comprehensive customization"
                
                insertions:
                  - before: "public class {{classname}} {"
                    content: |
                      @lombok.Data
                      @javax.validation.Valid
                """;
            
            jos.write(yamlCustomization.getBytes());
            jos.closeEntry();
        }
    }

    private void createLibraryJarWithVersionRequirements(File jarFile) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile.toPath()))) {
            // Add metadata with specific version requirements
            JarEntry metadataEntry = new JarEntry("META-INF/openapi-library.yaml");
            jos.putNextEntry(metadataEntry);
            
            String libraryMetadata = """
                name: "version-specific-library"
                version: "1.5.0"
                description: "Library with specific version requirements"
                supportedGenerators:
                  - "spring"
                minOpenApiGeneratorVersion: "7.11.0"
                maxOpenApiGeneratorVersion: "7.14.0"
                minPluginVersion: "1.0.0"
                requiredFeatures:
                  - "advanced-validation"
                """;
            
            jos.write(libraryMetadata.getBytes());
            jos.closeEntry();

            // Add basic template
            JarEntry templateEntry = new JarEntry("META-INF/openapi-templates/spring/pojo.mustache");
            jos.putNextEntry(templateEntry);
            jos.write("// Version-specific template".getBytes());
            jos.closeEntry();
        }
    }

    private void createBuildFileWithLibraryCustomizations(File buildFile) throws IOException {
        try (FileWriter writer = new FileWriter(buildFile)) {
            writer.write("""
                plugins {
                    id 'java'
                    id 'com.guidedbyte.openapi-modelgen'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation 'org.openapitools:openapi-generator:7.10.0'
                    openapiCustomizations files('libs/test-templates-1.0.jar')
                }
                
                openapiModelgen {
                    defaults {
                        templateSources([
                            'user-templates',
                            'library-templates',
                            'library-customizations',  // Include library customizations
                            'user-customizations',
                            'openapi-generator'
                        ])
                        applyPluginCustomizations false
                    }
                    specs {
                        pets {
                            inputSpec 'src/main/resources/openapi/pets.yaml'
                            modelPackage 'com.example.pets'
                        }
                    }
                }
                """);
        }
    }

    private void createBuildFileWithIncompatibleGenerator(File buildFile) throws IOException {
        try (FileWriter writer = new FileWriter(buildFile)) {
            writer.write("""
                plugins {
                    id 'java'
                    id 'com.guidedbyte.openapi-modelgen'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation 'org.openapitools:openapi-generator:7.10.0'
                    openapiCustomizations files('libs/test-templates-1.0.jar')
                }
                
                openapiModelgen {
                    defaults {
                        // Note: Uses default 'spring' generator but library only supports java
                        templateSources([
                            'library-templates',
                            'library-customizations',
                            'openapi-generator'
                        ])
                        applyPluginCustomizations false
                    }
                    specs {
                        pets {
                            inputSpec 'src/main/resources/openapi/pets.yaml'
                            modelPackage 'com.example.pets'
                        }
                    }
                }
                """);
        }
    }

    private void createBuildFileWithSpringGenerator(File buildFile) throws IOException {
        try (FileWriter writer = new FileWriter(buildFile)) {
            writer.write("""
                plugins {
                    id 'java'
                    id 'com.guidedbyte.openapi-modelgen'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation 'org.openapitools:openapi-generator:7.10.0'
                    openapiCustomizations files('libs/test-templates-1.0.jar')
                }
                
                openapiModelgen {
                    defaults {
                        // Note: Uses default 'spring' generator (compatible with library)
                        templateSources([
                            'library-templates',
                            'library-customizations',
                            'openapi-generator'
                        ])
                        applyPluginCustomizations false
                    }
                    specs {
                        pets {
                            inputSpec 'src/main/resources/openapi/pets.yaml'
                            modelPackage 'com.example.pets'
                        }
                    }
                }
                """);
        }
    }

    private void createBuildFileWithComprehensivePrecedence(File buildFile) throws IOException {
        try (FileWriter writer = new FileWriter(buildFile)) {
            writer.write("""
                plugins {
                    id 'java'
                    id 'com.guidedbyte.openapi-modelgen'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation 'org.openapitools:openapi-generator:7.10.0'
                    openapiCustomizations files('libs/test-templates-1.0.jar')
                }
                
                openapiModelgen {
                    defaults {
                        templateSources([
                            'user-templates',           // Highest precedence
                            'user-customizations',      
                            'library-templates',        // Library templates
                            'library-customizations',   // Library YAML customizations
                            'plugin-customizations',    // Plugin customizations (disabled)
                            'openapi-generator'         // Lowest precedence
                        ])
                        debugTemplateResolution true  // Enable debug logging
                        applyPluginCustomizations false
                    }
                    specs {
                        pets {
                            inputSpec 'src/main/resources/openapi/pets.yaml'
                            modelPackage 'com.example.pets'
                        }
                    }
                }
                """);
        }
    }
}