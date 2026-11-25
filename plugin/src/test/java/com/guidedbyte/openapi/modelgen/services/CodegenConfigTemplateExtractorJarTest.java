package com.guidedbyte.openapi.modelgen.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openapitools.codegen.CodegenConfigLoader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that copying templates out of a JAR only ever writes inside the output directory.
 * A JAR entry whose name contains ".." must be refused rather than written somewhere else on disk,
 * and the refusal must fail the build rather than be logged and skipped.
 */
class CodegenConfigTemplateExtractorJarTest {

    @TempDir
    Path tempDir;

    private final CodegenConfigTemplateExtractor extractor = new CodegenConfigTemplateExtractor();

    @Test
    void extractsEntriesUnderTheTemplateDirectory() throws Exception {
        Path jar = writeJar("tpl/pojo.mustache", "tpl/libraries/spring-boot/model.mustache", "other/skipped.mustache");
        Path outputDir = Files.createDirectories(tempDir.resolve("work").resolve("out"));

        extractor.extractFromJar(jarUrl(jar, "tpl"), "tpl", outputDir);

        assertEquals("tpl/pojo.mustache", Files.readString(outputDir.resolve("pojo.mustache")));
        assertTrue(Files.exists(outputDir.resolve("libraries").resolve("spring-boot").resolve("model.mustache")));
        assertFalse(Files.exists(outputDir.resolve("skipped.mustache")));
    }

    @Test
    void refusesEntryThatResolvesOutsideTheOutputDirectory() throws Exception {
        Path jar = writeJar("tpl/../../escaped.mustache");
        Path outputDir = Files.createDirectories(tempDir.resolve("work").resolve("out"));
        URL hostileUrl = jarUrl(jar, "tpl");

        PathEscapeException error = assertThrows(PathEscapeException.class,
            () -> extractor.extractFromJar(hostileUrl, "tpl", outputDir));

        assertTrue(error.getMessage().contains("tpl/../../escaped.mustache"), error.getMessage());
        // "../../escaped.mustache" relative to work/out is tempDir/escaped.mustache
        assertFalse(Files.exists(tempDir.resolve("escaped.mustache")));
    }

    /**
     * The public entry points catch ordinary extraction failures, log a warning and return 0.
     * An entry that tries to leave the output directory must not be treated that way: it has to
     * reach the caller so the build fails. The test swaps in a class loader that answers the
     * generator's template directory with a JAR holding such an entry.
     */
    @Test
    void anEscapingEntryFailsExtractionInsteadOfBeingLoggedAndSkipped() throws Exception {
        String templateDir = CodegenConfigLoader.forName("spring").embeddedTemplateDir();
        Path jar = writeJar(templateDir + "/../../escaped.mustache");
        URL hostileUrl = jarUrl(jar, templateDir);
        File outputDir = tempDir.resolve("work").resolve("orig").toFile();
        TemplateDiscoveryService discovery = new TemplateDiscoveryService();

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new SingleResourceClassLoader(original, templateDir, hostileUrl));
        try {
            assertThrows(PathEscapeException.class, () -> extractor.extractAllTemplates("spring", outputDir));
            assertThrows(PathEscapeException.class, () -> discovery.extractAllTemplates("spring", outputDir));
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }

        assertFalse(Files.exists(tempDir.resolve("escaped.mustache")));
    }

    /** Answers one resource name with a fixed URL and passes everything else to its parent. */
    private static final class SingleResourceClassLoader extends ClassLoader {
        private final String resourceName;
        private final URL resourceUrl;

        SingleResourceClassLoader(ClassLoader parent, String resourceName, URL resourceUrl) {
            super(parent);
            this.resourceName = resourceName;
            this.resourceUrl = resourceUrl;
        }

        @Override
        public URL getResource(String name) {
            return resourceName.equals(name) ? resourceUrl : super.getResource(name);
        }
    }

    /** Writes a JAR whose entries each contain their own name as text. */
    private Path writeJar(String... entryNames) throws IOException {
        Path jar = tempDir.resolve("templates.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            for (String entryName : entryNames) {
                out.putNextEntry(new JarEntry(entryName));
                out.write(entryName.getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return jar;
    }

    private static URL jarUrl(Path jar, String resourcePath) throws Exception {
        return URI.create("jar:" + jar.toUri() + "!/" + resourcePath).toURL();
    }
}
