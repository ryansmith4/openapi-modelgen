package com.guidedbyte.openapi.modelgen;

import org.gradle.testkit.runner.GradleRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for TestKit integration tests that provides proper plugin classpath configuration
 * for compileOnly OpenAPI Generator plugin dependency.
 * 
 * This class ensures TestKit tests have access to both our plugin and the OpenAPI Generator
 * plugin classes when using compileOnly dependency structure.
 */
public abstract class BaseTestKitTest {
    
    /**
     * Creates a GradleRunner with custom classpath that includes both our plugin
     * and the OpenAPI Generator plugin for TestKit compatibility with compileOnly dependency.
     */
    protected GradleRunner createGradleRunner(File testProjectDir) {
        return GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath(getTestKitClasspath());
    }
    
    /**
     * Gets the classpath for TestKit including our plugin and OpenAPI Generator plugin.
     * 
     * Since we use compileOnly dependency for OpenAPI Generator plugin to avoid bundling it,
     * TestKit needs explicit classpath configuration to access those classes during testing.
     * 
     * This method includes ALL transitive dependencies needed by OpenAPI Generator to run properly.
     */
    private List<File> getTestKitClasspath() {
        List<File> classpath = new ArrayList<>();
        
        // Get the test classpath which includes all our dependencies
        String classpathProperty = System.getProperty("java.class.path");
        String[] classpathEntries = classpathProperty.split(File.pathSeparator);
        
        for (String entry : classpathEntries) {
            File file = new File(entry);
            
            // Include ALL JAR files from test classpath to ensure we have complete dependencies
            // This is necessary because OpenAPI Generator has many transitive dependencies
            if (entry.endsWith(".jar")) {
                classpath.add(file);
            }
            // Also include any class directories (for our compiled plugin classes)
            else if (entry.contains("classes") || entry.contains("resources")) {
                classpath.add(file);
            }
        }
        
        return classpath;
    }
}