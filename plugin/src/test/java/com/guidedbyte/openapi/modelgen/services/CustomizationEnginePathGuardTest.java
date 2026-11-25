package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.TemplateConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the customization engine fails the build when a template library names a file outside
 * the template work directory. A template library is a JAR the build reads as data, so its entry
 * names are the one input here that the build author did not write.
 *
 * <p>The engine only looks at library entries whose name starts with the generator name, here
 * {@code spring/}.
 */
class CustomizationEnginePathGuardTest {

    private static final List<String> SOURCES = List.of("user-templates", "user-customizations",
        "library-templates", "library-customizations", "plugin-customizations", "openapi-generator");

    private final CustomizationEngine engine = new CustomizationEngine();

    @TempDir
    Path tempDir;

    @Test
    void writesALibraryTemplateThatStaysInsideTheWorkDirectory() throws IOException {
        Path workDir = workDir();
        TemplateConfiguration config = config(workDir).hasLibraryTemplates(true)
            .libraryTemplates(Map.of("spring/pojo.mustache", "library pojo"))
            .build();

        engine.processTemplateCustomizations(config, workDir.toFile());

        assertEquals("library pojo", Files.readString(workDir.resolve("pojo.mustache")));
    }

    @Test
    void failsOnALibraryTemplateThatLeavesTheWorkDirectory() throws IOException {
        Path workDir = workDir();
        File workDirFile = workDir.toFile();
        TemplateConfiguration config = config(workDir).hasLibraryTemplates(true)
            .libraryTemplates(Map.of("spring/../../escaped.mustache", "escaped"))
            .build();

        // Only the call under test is inside the lambda, so nothing else can supply the exception.
        RuntimeException error = assertThrows(RuntimeException.class,
            () -> engine.processTemplateCustomizations(config, workDirFile));

        assertInstanceOf(PathEscapeException.class, rootCause(error));
        assertNothingWrittenOutside(workDir);
    }

    @Test
    void failsOnALibraryCustomizationThatLeavesTheWorkDirectory() throws IOException {
        Path workDir = workDir();
        File workDirFile = workDir.toFile();
        TemplateConfiguration config = config(workDir).hasLibraryCustomizations(true)
            .libraryCustomizations(Map.of("spring/../../escaped.mustache.yaml", "replacements: []"))
            .build();

        RuntimeException error = assertThrows(RuntimeException.class,
            () -> engine.processTemplateCustomizations(config, workDirFile));

        assertInstanceOf(PathEscapeException.class, rootCause(error));
        assertNothingWrittenOutside(workDir);
    }

    /** The work directory sits two levels down, so "../../x" would still land inside the temp directory. */
    private Path workDir() throws IOException {
        return Files.createDirectories(tempDir.resolve("build").resolve("template-work"));
    }

    private static TemplateConfiguration.Builder config(Path workDir) {
        return TemplateConfiguration.builder("spring")
            .templateWorkDir(workDir.toString())
            .templateSources(SOURCES)
            .templateVariables(Map.of());
    }

    private static Throwable rootCause(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private void assertNothingWrittenOutside(Path workDir) throws IOException {
        try (Stream<Path> files = Files.walk(tempDir)) {
            List<Path> outside = files.filter(Files::isRegularFile)
                .filter(file -> !file.startsWith(workDir))
                .toList();
            assertTrue(outside.isEmpty(), "files written outside the work directory: " + outside);
        }
    }
}
