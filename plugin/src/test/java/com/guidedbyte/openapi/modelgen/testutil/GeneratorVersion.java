package com.guidedbyte.openapi.modelgen.testutil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single source of truth for the OpenAPI Generator version under test.
 *
 * <p>{@code plugin/build.gradle} resolves the version from the {@code openapiGeneratorVersion}
 * Gradle property (falling back to the version catalog), puts that exact version on the test
 * classpath, and exposes it to tests as the {@code openapi.generator.version} system property.
 * TestKit tests must apply the same version in their generated build scripts so that the
 * {@code plugins {}} block and the injected plugin classpath agree.</p>
 *
 * <p>Run the suite against another generator with
 * {@code gradle plugin:test -PopenapiGeneratorVersion=7.21.0}.</p>
 */
public final class GeneratorVersion {

    private static final String SYSTEM_PROPERTY = "openapi.generator.version";

    private static final Pattern JAR_VERSION_PATTERN =
        Pattern.compile("openapi-generator-gradle-plugin-([0-9]+\\.[0-9]+\\.[0-9]+)");

    private GeneratorVersion() {
    }

    /**
     * Returns the OpenAPI Generator version under test, for example {@code "7.26.0"}.
     *
     * <p>Reads the {@code openapi.generator.version} system property set by the build. When the
     * property is missing (for example when a test is launched from an IDE without the Gradle test
     * task), falls back to the version in the generator plugin JAR name on the classpath.</p>
     *
     * @return the version string
     * @throws IllegalStateException if the version cannot be determined
     */
    public static String current() {
        String fromProperty = System.getProperty(SYSTEM_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        String fromClasspath = detectFromClasspath();
        if (fromClasspath != null) {
            return fromClasspath;
        }
        throw new IllegalStateException("OpenAPI Generator version under test is unknown: system property '"
            + SYSTEM_PROPERTY + "' is not set and the generator plugin JAR was not found on the classpath. "
            + "Run the tests through Gradle (plugin:test).");
    }

    /**
     * Returns the {@code id 'org.openapi.generator' version '...'} line for a Gradle {@code plugins {}}
     * block, indented by four spaces and terminated by a newline, so it can be spliced between two
     * text blocks.
     *
     * @return the plugin application line
     */
    public static String pluginLine() {
        return "    id 'org.openapi.generator' version '" + current() + "'\n";
    }

    private static String detectFromClasspath() {
        try {
            Class<?> generateTask = Class.forName("org.openapitools.generator.gradle.plugin.tasks.GenerateTask");
            java.security.CodeSource source = generateTask.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) {
                Matcher m = JAR_VERSION_PATTERN.matcher(source.getLocation().toString());
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (ClassNotFoundException | RuntimeException e) {
            // fall through
        }
        return null;
    }
}
