package com.guidedbyte.openapi.modelgen.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the plugin finds its JARs and resource directories when they sit under a directory
 * whose name has a space or another character that a URL encodes. This is the usual case for a
 * Gradle user home under a Windows profile such as {@code C:\Users\Jane Doe}. The plugin used to
 * keep the encoded form ({@code Jane%20Doe}), fail to open the JAR, and skip its built-in
 * customizations with only a warning.
 */
class ResourceUrlsTest {

    /** A directory name with a space, a plus sign and a percent sign: all three are easy to decode wrongly. */
    private static final String AWKWARD_NAME = "Jane Doe+co 100%";

    private static final String CUSTOMIZATIONS = "templateCustomizations/spring";

    @TempDir
    Path tempDir;

    @Test
    void findsTheJarBehindAnEncodedUrl() throws Exception {
        Path jar = writeJar("tpl/pojo.mustache");

        try (URLClassLoader loader = new URLClassLoader(new URL[] {jar.toUri().toURL()}, null)) {
            URL resource = loader.getResource("tpl/pojo.mustache");

            assertTrue(resource.toString().contains("%20"), "expected an encoded URL but got " + resource);
            assertEquals(jar, ResourceUrls.jarFileOf(resource));
        }
    }

    @Test
    void findsTheJarBehindAUrlWrittenWithARawSpace() throws Exception {
        Path jar = writeJar("tpl/pojo.mustache");

        assertEquals(jar, ResourceUrls.jarFileOf(rawUrl("jar:file:" + slashed(jar) + "!/tpl")));
    }

    @Test
    void findsTheDirectoryBehindAFileUrl() throws Exception {
        Path dir = Files.createDirectories(tempDir.resolve(AWKWARD_NAME).resolve("classes"));

        assertEquals(dir, ResourceUrls.fileOf(dir.toUri().toURL()));
        assertEquals(dir, ResourceUrls.fileOf(rawUrl("file:" + slashed(dir))));
    }

    @Test
    void refusesUrlsThatDoNotNameALocalFile() throws Exception {
        URL remoteJar = rawUrl("jar:https://example.invalid/templates.jar!/tpl");
        URL remoteFile = rawUrl("https://example.invalid/templates");

        assertThrows(IOException.class, () -> ResourceUrls.jarFileOf(remoteJar));
        assertThrows(IOException.class, () -> ResourceUrls.jarFileOf(remoteFile));
        assertThrows(IOException.class, () -> ResourceUrls.fileOf(remoteFile));
    }

    @Test
    void extractsTemplatesFromAJarUnderAnAwkwardDirectory() throws Exception {
        Path jar = writeJar("tpl/", "tpl/pojo.mustache");
        Path outputDir = Files.createDirectories(tempDir.resolve("out"));

        try (URLClassLoader loader = new URLClassLoader(new URL[] {jar.toUri().toURL()}, null)) {
            new CodegenConfigTemplateExtractor().extractFromJar(loader.getResource("tpl"), "tpl", outputDir);
        }

        assertEquals("tpl/pojo.mustache", Files.readString(outputDir.resolve("pojo.mustache")));
    }

    @Test
    void discoversBuiltInCustomizationsInAJarUnderAnAwkwardDirectory() throws Exception {
        Path jar = writeJar(CUSTOMIZATIONS + "/", CUSTOMIZATIONS + "/pojo.mustache.yaml",
            CUSTOMIZATIONS + "/model.mustache.yml", CUSTOMIZATIONS + "/notes.txt");

        try (URLClassLoader loader = new URLClassLoader(new URL[] {jar.toUri().toURL()}, null)) {
            Set<String> found = new CustomizationEngine().discoverPluginCustomizations(CUSTOMIZATIONS, loader);

            assertEquals(Set.of("pojo.mustache.yaml", "model.mustache.yml"), found);
        }
    }

    @Test
    void discoversBuiltInCustomizationsInADirectoryWithAnAwkwardName() throws Exception {
        Path classes = Files.createDirectories(tempDir.resolve(AWKWARD_NAME).resolve("classes"));
        Path customizations = Files.createDirectories(classes.resolve(CUSTOMIZATIONS));
        Files.writeString(customizations.resolve("pojo.mustache.yaml"), "replacements: []");

        try (URLClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, null)) {
            Set<String> found = new CustomizationEngine().discoverPluginCustomizations(CUSTOMIZATIONS, loader);

            assertEquals(Set.of("pojo.mustache.yaml"), found);
        }
    }

    /** Writes a JAR under the awkwardly named directory. Each file entry contains its own name as text. */
    private Path writeJar(String... entryNames) throws IOException {
        Path jar = Files.createDirectories(tempDir.resolve(AWKWARD_NAME)).resolve("templates.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            for (String entryName : entryNames) {
                out.putNextEntry(new JarEntry(entryName));
                if (!entryName.endsWith("/")) {
                    out.write(entryName.getBytes(StandardCharsets.UTF_8));
                }
                out.closeEntry();
            }
        }
        return jar;
    }

    /** The path with forward slashes and a leading slash, as it appears after "file:" in a URL. */
    private static String slashed(Path path) {
        String text = path.toString().replace(File.separatorChar, '/');
        return text.startsWith("/") ? text : "/" + text;
    }

    /** Builds a URL from text exactly as given, without encoding anything in it. */
    @SuppressWarnings("deprecation")
    private static URL rawUrl(String spec) throws IOException {
        return new URL(spec);
    }
}
