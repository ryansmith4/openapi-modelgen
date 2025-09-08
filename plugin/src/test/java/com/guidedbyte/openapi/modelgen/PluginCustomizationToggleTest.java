package com.guidedbyte.openapi.modelgen;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the applyPluginCustomizations feature flag
 */
public class PluginCustomizationToggleTest {

    @TempDir
    File testProjectDir;
    
    private Project project;
    private OpenApiModelGenExtension extension;
    
    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder()
                .withProjectDir(testProjectDir)
                .build();
        
        // Apply the plugin
        project.getPluginManager().apply(OpenApiModelGenPlugin.class);
        extension = project.getExtensions().getByType(OpenApiModelGenExtension.class);
    }
    
    @Test
    void testDefaultValueIsNotSet() {
        // The default value should not be present initially (allowing the plugin to use its own default)
        assertFalse(extension.getDefaults().getApplyPluginCustomizations().isPresent(),
            "applyPluginCustomizations should not be set by default");
    }
    
    @Test
    void testCanSetDefaultValue() {
        // Test setting the value at the default level
        extension.getDefaults().applyPluginCustomizations(false);
        
        assertTrue(extension.getDefaults().getApplyPluginCustomizations().isPresent());
        assertFalse(extension.getDefaults().getApplyPluginCustomizations().get());
    }
    
    @Test
    void testCanSetSpecLevelValue() {
        // Create a spec configuration directly
        SpecConfig specConfig = new SpecConfig(project);
        extension.getSpecs().put("testSpec", specConfig);
        
        // Test setting the value at the spec level
        specConfig.applyPluginCustomizations(false);
        
        assertTrue(specConfig.getApplyPluginCustomizations().isPresent());
        assertFalse(specConfig.getApplyPluginCustomizations().get());
    }
    
    @Test
    void testSpecLevelOverridesDefault() throws IOException {
        // Set default to true
        extension.getDefaults().applyPluginCustomizations(true);
        
        // Create test spec with override directly
        SpecConfig specConfig = new SpecConfig(project);
        extension.getSpecs().put("testSpec", specConfig);
        specConfig.inputSpec("test.yaml");
        specConfig.modelPackage("com.test");
        specConfig.applyPluginCustomizations(false);
        
        // Create a dummy spec file
        File specFile = new File(testProjectDir, "test.yaml");
        Files.writeString(specFile.toPath(), "openapi: 3.0.0\ninfo:\n  title: Test\n  version: 1.0.0\npaths: {}");
        
        // Verify the spec level value overrides the default
        assertTrue(extension.getDefaults().getApplyPluginCustomizations().get());
        assertFalse(specConfig.getApplyPluginCustomizations().get());
    }
    
    @Test
    void testCanEnablePluginCustomizationsExplicitly() {
        // Test explicitly enabling the flag
        extension.getDefaults().applyPluginCustomizations(true);
        
        assertTrue(extension.getDefaults().getApplyPluginCustomizations().isPresent());
        assertTrue(extension.getDefaults().getApplyPluginCustomizations().get());
    }
}