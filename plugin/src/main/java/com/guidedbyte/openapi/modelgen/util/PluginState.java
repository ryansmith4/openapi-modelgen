package com.guidedbyte.openapi.modelgen.util;

import java.io.File;

/**
 * Thread-safe singleton that holds global plugin configuration state.
 *
 * <p>This utility provides centralized access to the build directory
 * for rich file logging without passing it through method parameters.</p>
 *
 * <p>The state is set once during plugin configuration phase and remains
 * immutable throughout the build execution.</p>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // In plugin configuration
 * PluginState.getInstance().setBuildDirectory(project.getBuildDir());
 *
 * // Rich file logging automatically enabled when build directory is available
 * Logger logger = PluginLoggerFactory.getLogger(MyClass.class);
 * }</pre>
 *
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
public final class PluginState {

    private static final PluginState INSTANCE = new PluginState();

    private volatile File buildDirectory = null;

    private PluginState() {
        // Private constructor for singleton
    }

    /**
     * Gets the singleton instance of PluginState.
     *
     * @return the singleton instance
     */
    public static PluginState getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the build directory for the current project.
     *
     * @return the build directory, or null if not set
     */
    public File getBuildDirectory() {
        return buildDirectory;
    }

    /**
     * Sets the build directory for the current project.
     * This should only be called once during plugin configuration phase.
     *
     * @param buildDirectory the build directory
     */
    public void setBuildDirectory(File buildDirectory) {
        this.buildDirectory = buildDirectory;
    }
}