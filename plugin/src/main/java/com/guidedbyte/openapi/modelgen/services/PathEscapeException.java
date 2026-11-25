package com.guidedbyte.openapi.modelgen.services;

import org.gradle.api.GradleException;

/**
 * Thrown when a file name taken from an archive or library would be written outside the directory
 * the plugin is filling, for example a JAR entry named {@code ../../somewhere/file}.
 * <p>
 * It is unchecked on purpose. The code that extracts templates catches {@code IOException} and
 * carries on with a warning, which is right for a file that cannot be read but wrong for an archive
 * that tries to choose where its files go. An unchecked exception passes through those handlers and
 * fails the build.
 */
public class PathEscapeException extends GradleException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message what was refused and why
     */
    public PathEscapeException(String message) {
        super(message);
    }
}
