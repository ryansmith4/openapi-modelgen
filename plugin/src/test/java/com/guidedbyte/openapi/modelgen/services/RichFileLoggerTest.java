package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.FileHandleCleanupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the lazy initialisation of {@link RichFileLogger}.
 *
 * <p>Obtaining a logger must not touch the file system. The plugin's loggers are static fields
 * whose initialisers can run at configuration time, and Gradle records a configuration-time
 * {@code File.exists()} probe or directory creation as a configuration cache input. Before 2.2.3
 * the logger probed {@code build/logs} and created it while the plugin was being configured, so
 * the first run after every {@code clean} invalidated the configuration cache entry for the run
 * after it ("file system entry 'build/logs' has been created").</p>
 */
@ExtendWith(FileHandleCleanupExtension.class)
class RichFileLoggerTest {

    @TempDir
    File projectDir;

    @AfterEach
    void closeLoggers() {
        RichFileLogger.closeAll();
    }

    @Test
    void obtainingALoggerDoesNotTouchTheFileSystem() {
        File buildDir = new File(projectDir, "build");

        RichFileLogger.forBuildDir(buildDir);

        assertFalse(new File(buildDir, "logs").exists(),
            "build/logs must not be created before the first write (configuration cache input)");
        assertFalse(buildDir.exists(), "the build directory must not be created either");
    }

    @Test
    void firstWriteCreatesTheLogsDirectoryAndTheLogFile() throws IOException {
        File buildDir = new File(projectDir, "build");
        RichFileLogger logger = RichFileLogger.forBuildDir(buildDir);

        logger.info("first entry for {}", "pets");

        File logFile = new File(buildDir, "logs/openapi-modelgen-debug.log");
        assertTrue(logFile.isFile(), "log file should be created on first write: " + logFile);
        String content = Files.readString(logFile.toPath());
        assertTrue(content.contains("OpenAPI Model Generator Rich Debug Log"), "header expected:\n" + content);
        assertTrue(content.contains("first entry for pets"), "entry expected:\n" + content);
    }

    @Test
    void sectionHeaderAlsoInitialisesLazily() throws IOException {
        File buildDir = new File(projectDir, "build");
        RichFileLogger logger = RichFileLogger.forBuildDir(buildDir);

        logger.section("Template Customization Processing");
        logger.debug("after section");

        File logFile = new File(buildDir, "logs/openapi-modelgen-debug.log");
        String content = Files.readString(logFile.toPath());
        assertTrue(content.contains("=== Template Customization Processing ==="), content);
        assertTrue(content.contains("after section"), content);
    }
}
