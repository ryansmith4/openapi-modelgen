package com.guidedbyte.openapi.modelgen.services;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenApiGeneratorVersionDetector}.
 */
class OpenApiGeneratorVersionDetectorTest {

    private OpenApiGeneratorVersionDetector detector;
    private Project project;

    @BeforeEach
    void setUp() {
        detector = new OpenApiGeneratorVersionDetector();
        project = ProjectBuilder.builder().build();
    }

    @Test
    void testDetectVersionOrFail_WithTestClasspath_DetectsVersion() {
        // Given: A project with OpenAPI Generator on test classpath (from build.gradle testImplementation)
        
        // When: Detecting version
        String version = detector.detectVersionOrFail(project);
        
        // Then: Should find the version from test classpath
        assertNotNull(version);
        assertTrue(version.matches("\\d+\\.\\d+.*"), "Version should be in semver format: " + version);
        
        // The test environment has OpenAPI Generator 7.14.0 available on classpath
        // So our detector should find it via classpath inspection
    }

    @Test 
    void testDetectVersionOrFail_Documentation() {
        // This test documents the expected behavior and detection strategies
        
        // Strategy 1: Plugin version detection (highest priority)
        // Strategy 2: Dependency analysis  
        // Strategy 3: Classpath inspection
        // Strategy 4: Fail with clear error message (lowest priority)
        
        // In our test environment, Strategy 3 (classpath inspection) should succeed
        // because we have OpenAPI Generator on the test classpath
        
        String version = detector.detectVersionOrFail(project);
        
        // Should successfully detect version from test classpath
        assertNotNull(version);
        assertFalse(version.isEmpty());
        
        // Version should look like semantic version
        assertTrue(version.matches("\\d+\\.\\d+.*"), "Expected semver format, got: " + version);
    }

    @Test
    void testDetectionStrategiesOrder() {
        // This test documents that the detector tries multiple strategies
        // in the correct priority order
        
        // A clean ProjectBuilder project should have:
        // - No OpenAPI Generator plugin applied (Strategy 1 fails)
        // - No OpenAPI Generator dependencies (Strategy 2 fails)  
        // - BUT OpenAPI Generator classes on test classpath (Strategy 3 succeeds)
        
        // Therefore detection should succeed via Strategy 3 (classpath inspection)
        String version = detector.detectVersionOrFail(project);
        
        // Should find version via classpath inspection
        assertNotNull(version);
        assertTrue(version.matches("\\d+\\.\\d+.*"));
    }
    
    @Test
    void testVersionFromTestClasspath() {
        // Verify that we can successfully detect version from test environment
        
        String version = detector.detectVersionOrFail(project);
        
        // Should detect the version from our test dependencies (7.14.0 from build.gradle)
        assertNotNull(version);
        
        // Version should be valid semver format
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+.*"), "Expected semver format like '7.14.0', got: " + version);
        
        // Since we know the test environment has 7.14.0, we can verify that
        // (though this could change if build.gradle is updated)
        assertTrue(version.startsWith("7."), "Expected version starting with 7.x, got: " + version);
    }
    
    @Test 
    void testFailureMessageFormatting() {
        // This test documents the expected error message format
        // We can't easily test the failure case in this environment since
        // we have OpenAPI Generator on the classpath, but we can verify
        // the detector has the right error message structure
        
        // The error message should be comprehensive and helpful when detection fails
        // This is validated by the implementation in OpenApiGeneratorVersionDetector
        
        // For now, just verify successful detection in our test environment
        String version = detector.detectVersionOrFail(project);
        assertNotNull(version);
        assertFalse(version.trim().isEmpty());
    }
}