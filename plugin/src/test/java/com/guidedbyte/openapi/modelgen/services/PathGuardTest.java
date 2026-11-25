package com.guidedbyte.openapi.modelgen.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a file name taken from a JAR or template library can only ever point inside the
 * directory the plugin is writing to. A name that leaves it must fail with
 * {@link PathEscapeException}, which is unchecked so that it fails the build.
 */
class PathGuardTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsNamesInsideTheDirectory() throws IOException {
        Path root = Files.createDirectories(tempDir.resolve("work"));

        assertEquals(root.resolve("pojo.mustache"), PathGuard.resolveInside(root, "pojo.mustache", "test"));
        assertEquals(root.resolve("libraries").resolve("spring-boot").resolve("model.mustache"),
            PathGuard.resolveInside(root, "libraries/spring-boot/model.mustache", "test"));
        // ".." that stays inside the directory is harmless
        assertEquals(root.resolve("b.mustache"), PathGuard.resolveInside(root, "a/../b.mustache", "test"));
    }

    @Test
    void refusesNamesThatLeaveTheDirectory() throws IOException {
        Path root = Files.createDirectories(tempDir.resolve("work"));
        // A sibling whose name starts with the root's name must not count as inside it.
        Files.createDirectories(tempDir.resolve("work-other"));

        List<String> escaping = List.of(
            "../escaped.mustache",
            "sub/../../escaped.mustache",
            "../work-other/escaped.mustache",
            tempDir.resolve("elsewhere.mustache").toString(),
            // absolute on every operating system
            "/escaped.mustache",
            "//server/share/escaped.mustache");

        for (String name : escaping) {
            PathEscapeException error = assertThrows(PathEscapeException.class,
                () -> PathGuard.resolveInside(root, name, "the test archive"), name);
            assertTrue(error.getMessage().contains(name), error.getMessage());
            assertTrue(error.getMessage().contains("the test archive"), error.getMessage());
        }
    }

    /** A name that resolves to the directory itself is not a file name, so there is nothing to write. */
    @Test
    void refusesNamesThatAreTheDirectoryItself() throws IOException {
        Path root = Files.createDirectories(tempDir.resolve("work"));

        for (String name : List.of("", ".", "a/..")) {
            IOException error = assertThrows(IOException.class,
                () -> PathGuard.resolveInside(root, name, "the test archive"), "'" + name + "'");
            assertTrue(error.getMessage().contains("the test archive"), error.getMessage());
        }
    }

    /** On Windows a backslash separates directories and a drive letter makes a name absolute. */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void refusesWindowsFormsOfLeavingTheDirectory() throws IOException {
        Path root = Files.createDirectories(tempDir.resolve("work"));

        for (String name : List.of("..\\escaped.mustache", "sub\\..\\..\\escaped.mustache",
                "\\escaped.mustache", "\\\\server\\share\\escaped.mustache")) {
            assertThrows(PathEscapeException.class, () -> PathGuard.resolveInside(root, name, "test"), name);
        }
        // A character Windows does not allow in a file name is reported, not passed on.
        assertThrows(IOException.class, () -> PathGuard.resolveInside(root, "escaped?.mustache", "test"));
    }

    /**
     * Names whose meaning depends on the operating system: a backslash separates directories on
     * Windows and is an ordinary character elsewhere, and a drive or UNC prefix only exists on
     * Windows. Whatever the platform makes of them, the result must never be outside the directory.
     */
    @Test
    void neverReturnsAPathOutsideTheDirectory() {
        Path root = tempDir.resolve("work").toAbsolutePath().normalize();

        List<String> suspicious = List.of(
            "..\\escaped.mustache",
            "sub\\..\\..\\escaped.mustache",
            "\\escaped.mustache",
            "/escaped.mustache",
            "//server/share/escaped.mustache",
            "Z:/escaped.mustache",
            "Z:escaped.mustache",
            "escaped?.mustache");

        for (String name : suspicious) {
            try {
                Path resolved = PathGuard.resolveInside(root, name, "test");
                assertTrue(resolved.startsWith(root), name + " resolved to " + resolved);
            } catch (PathEscapeException | IOException expected) {
                // refused, which is also fine
            }
        }
    }
}
