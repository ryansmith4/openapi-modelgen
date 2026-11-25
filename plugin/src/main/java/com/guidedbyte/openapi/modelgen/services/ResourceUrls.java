package com.guidedbyte.openapi.modelgen.services;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Turns the URL a class loader gives for a resource into the file it points at.
 *
 * <p>A class loader writes such a URL with characters like a space encoded ({@code %20}). Cutting the
 * path out of the URL text keeps the {@code %20}, so the file cannot be found whenever it sits under a
 * directory with a space in its name, such as a Windows user profile. These methods decode the URL.
 */
final class ResourceUrls {

    private static final String FILE_PREFIX = "file:";
    private static final String JAR_ENTRY_SEPARATOR = "!/";

    private ResourceUrls() {
    }

    /**
     * Returns the JAR file that a {@code jar:file:...!/entry} URL points into.
     *
     * @param jarUrl a URL with the {@code jar} protocol
     * @return the JAR file on disk
     * @throws IOException if the URL does not name a JAR on the local file system
     */
    static Path jarFileOf(URL jarUrl) throws IOException {
        String jarAndEntry = jarUrl.getPath();
        int separator = jarAndEntry.indexOf(JAR_ENTRY_SEPARATOR);
        if (!"jar".equals(jarUrl.getProtocol()) || separator < 0) {
            throw new IOException("Not a URL into a JAR file: " + jarUrl);
        }
        return localPath(jarAndEntry.substring(0, separator));
    }

    /**
     * Returns the file or directory that a {@code file:} URL names.
     *
     * @param fileUrl a URL with the {@code file} protocol
     * @return the file or directory on disk
     * @throws IOException if the URL does not name something on the local file system
     */
    static Path fileOf(URL fileUrl) throws IOException {
        return localPath(fileUrl.toExternalForm());
    }

    private static Path localPath(String fileUrl) throws IOException {
        if (!fileUrl.startsWith(FILE_PREFIX)) {
            throw new IOException("Not a file on the local file system: " + fileUrl);
        }
        try {
            return Paths.get(new URI(fileUrl));
        } catch (URISyntaxException | IllegalArgumentException | FileSystemNotFoundException e) {
            // Not a well-formed URI, for example because it holds a raw space. Nothing in it is
            // encoded then, so the text after "file:" is the path as written.
            return pathAsWritten(fileUrl);
        }
    }

    private static Path pathAsWritten(String fileUrl) throws IOException {
        try {
            return new File(fileUrl.substring(FILE_PREFIX.length())).toPath();
        } catch (InvalidPathException e) {
            throw new IOException("Cannot turn '" + fileUrl + "' into a file path: " + e.getMessage(), e);
        }
    }
}
