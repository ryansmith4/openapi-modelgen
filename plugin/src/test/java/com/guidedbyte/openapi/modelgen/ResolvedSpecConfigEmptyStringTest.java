package com.guidedbyte.openapi.modelgen;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResolvedSpecConfigEmptyStringTest {

    private Project project;
    private OpenApiModelGenExtension extension;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().build();
        extension = new OpenApiModelGenExtension(project);
    }

    @Test
    void testEmptyStringModelNameSuffixIsPreserved() {
        // Given
        extension.getDefaults().modelNameSuffix("");  // Set empty string as default
        
        SpecConfig specConfig = new SpecConfig(project);
        specConfig.getInputSpec().set("test.yaml");
        specConfig.getModelPackage().set("com.test");
        
        // When
        ResolvedSpecConfig resolved = ResolvedSpecConfig.builder("test", extension, specConfig).build();
        
        // Then
        assertEquals("", resolved.getModelNameSuffix(), "Empty string suffix should be preserved from defaults");
    }

    @Test
    void testSpecConfigEmptyStringOverridesDefault() {
        // Given - defaults have "Dto" suffix
        // Note: defaults already have "Dto" as hardcoded default
        
        SpecConfig specConfig = new SpecConfig(project);
        specConfig.getInputSpec().set("test.yaml");
        specConfig.getModelPackage().set("com.test");
        specConfig.getModelNameSuffix().set("");  // Override with empty string
        
        // When
        ResolvedSpecConfig resolved = ResolvedSpecConfig.builder("test", extension, specConfig).build();
        
        // Then
        assertEquals("", resolved.getModelNameSuffix(), "Empty string suffix should override default 'Dto'");
    }
}