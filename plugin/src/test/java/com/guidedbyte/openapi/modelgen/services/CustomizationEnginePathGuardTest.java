package com.guidedbyte.openapi.modelgen.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a file name taken from a template library JAR can only ever point inside the
 * template work directory. A name containing ".." or an absolute path must be refused.
 */
class CustomizationEnginePathGuardTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsNamesInsideTheWorkDirectory() throws IOException {
        File workDir = Files.createDirectories(tempDir.resolve("work")).toFile();

        File plain = CustomizationEngine.resolveInsideWorkDir(workDir, "pojo.mustache");
        File nested = CustomizationEngine.resolveInsideWorkDir(workDir, "libraries/spring-boot/model.mustache");

        assertEquals(new File(workDir, "pojo.mustache").getCanonicalFile(), plain.getCanonicalFile());
        assertTrue(nested.toPath().startsWith(workDir.toPath().toAbsolutePath().normalize()));
    }

    @Test
    void refusesNamesThatLeaveTheWorkDirectory() throws IOException {
        File workDir = Files.createDirectories(tempDir.resolve("work")).toFile();

        assertThrows(IOException.class,
            () -> CustomizationEngine.resolveInsideWorkDir(workDir, "../escaped.mustache"));
        assertThrows(IOException.class,
            () -> CustomizationEngine.resolveInsideWorkDir(workDir, "sub/../../escaped.mustache"));
        assertThrows(IOException.class,
            () -> CustomizationEngine.resolveInsideWorkDir(workDir, tempDir.resolve("elsewhere.mustache").toString()));
    }
}
