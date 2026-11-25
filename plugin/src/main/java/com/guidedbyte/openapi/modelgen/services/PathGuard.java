package com.guidedbyte.openapi.modelgen.services;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Keeps file names that come from outside the build (JAR entries, template library contents)
 * inside the directory the plugin is writing to.
 */
final class PathGuard {

    private PathGuard() {
        // static helper
    }

    /**
     * Resolves {@code name} against {@code root} and returns the result only if it is inside
     * {@code root}. A name such as {@code ../x}, an absolute path, or a path on another drive is
     * refused, so the archive a name came from cannot choose where a file is written.
     * <p>
     * The comparison is made on whole path segments, so a sibling directory whose name merely
     * starts with the root's name ({@code out-other} next to {@code out}) is not treated as inside.
     *
     * @param root the directory files may be written to
     * @param name the file name to resolve, relative to {@code root}
     * @param source where the name came from, used in the error message
     * @return the absolute, normalized path inside {@code root}
     * @throws PathEscapeException if the name resolves outside {@code root}
     * @throws IOException if the name cannot be used as a path on this file system at all
     */
    static Path resolveInside(Path root, String name, String source) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved;
        try {
            resolved = normalizedRoot.resolve(name).normalize();
        } catch (InvalidPathException e) {
            throw new IOException("'" + name + "' from " + source + " is not a usable file name: "
                + e.getMessage(), e);
        }
        if (!resolved.startsWith(normalizedRoot)) {
            throw new PathEscapeException("Refusing to write '" + name + "' from " + source
                + ": it resolves outside the directory '" + normalizedRoot + "'");
        }
        return resolved;
    }
}
