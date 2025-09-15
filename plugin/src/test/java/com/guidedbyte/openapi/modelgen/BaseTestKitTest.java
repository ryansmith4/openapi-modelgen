package com.guidedbyte.openapi.modelgen;

import org.gradle.testkit.runner.GradleRunner;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Base class for TestKit integration tests that provides proper plugin classpath configuration
 * for compileOnly OpenAPI Generator plugin dependency.
 * <p>
 * This class ensures TestKit tests have access to both our plugin and the OpenAPI Generator
 * plugin classes when using compileOnly dependency structure.
 */
public abstract class BaseTestKitTest {
    
    /**
     * Creates a GradleRunner with stable plugin classpath for configuration cache compatibility.
     */
    protected GradleRunner createGradleRunner(File testProjectDir) {
        return GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath(getStableTestKitClasspath());
    }
    
    /**
     * Gets a stable classpath for TestKit that's configuration cache compatible.
     * <p>
     * The key insight is that we cache the classpath result to avoid recomputation
     * which can vary between test runs and break configuration cache.
     */
    private static volatile List<File> cachedClasspath = null;
    
    private List<File> getStableTestKitClasspath() {
        if (cachedClasspath == null) {
            synchronized (BaseTestKitTest.class) {
                if (cachedClasspath == null) {
                    cachedClasspath = computeStableClasspath();
                }
            }
        }
        return cachedClasspath;
    }
    
    /**
     * Computes a stable classpath once and caches it for configuration cache compatibility.
     */
    private List<File> computeStableClasspath() {
        List<File> classpath = new ArrayList<>();
        
        // Get the test classpath which includes all our dependencies
        String classpathProperty = System.getProperty("java.class.path");
        String[] classpathEntries = classpathProperty.split(File.pathSeparator);
        
        // Use a TreeSet to ensure consistent ordering for configuration cache
        Set<String> stableEntries = extractStableEntries(classpathEntries);

        // Convert to File objects in stable order
        for (String entry : stableEntries) {
            classpath.add(new File(entry));
        }
        
        return classpath;
    }

    private static @NotNull Set<String> extractStableEntries(String[] classpathEntries) {
        Set<String> stableEntries = new java.util.TreeSet<>();

        for (String entry : classpathEntries) {
            // Include ALL JAR files from test classpath to ensure we have complete dependencies
            // This is necessary because OpenAPI Generator has many transitive dependencies
            if (entry.endsWith(".jar")) {
                stableEntries.add(entry);
            }
            // Also include any class directories (for our compiled plugin classes)
            else if (entry.contains("classes") || entry.contains("resources")) {
                stableEntries.add(entry);
            }
        }
        return stableEntries;
    }
}