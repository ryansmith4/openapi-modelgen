package com.guidedbyte.openapi.modelgen.util;

import java.io.File;

/**
 * Thread-safe singleton that holds global plugin configuration state.
 *
 * <p>This utility provides centralized access to plugin-wide configuration
 * values that need to be accessible from various services and utilities
 * without passing them through method parameters.</p>
 *
 * <p>The state is set once during plugin configuration phase and remains
 * immutable throughout the build execution.</p>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // In plugin configuration (extension)
 * PluginState.getInstance().setDebugEnabled(extension.isDebug());
 * PluginState.getInstance().setBuildDirectory(project.getBuildDir());
 *
 * // In services and utilities
 * if (PluginState.getInstance().isDebugEnabled()) {
 *     logger.debug("Debug logging is enabled");
 * }
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

    private volatile boolean debugEnabled = false;
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
     * Returns whether debug logging is enabled for the plugin.
     *
     * @return true if debug logging is enabled, false otherwise
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Sets whether debug logging is enabled for the plugin.
     * This should only be called once during plugin configuration phase.
     *
     * @param debugEnabled true to enable debug logging, false to disable
     */
    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
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
     * @param buildDirectory the project's build directory
     */
    public void setBuildDirectory(File buildDirectory) {
        this.buildDirectory = buildDirectory;
    }

    /**
     * Returns whether rich file logging is available.
     * Rich file logging is available when a build directory has been set.
     *
     * @return true if rich file logging is available, false otherwise
     */
    public boolean isRichFileLoggingAvailable() {
        return buildDirectory != null;
    }

    /**
     * Resets the plugin state to default values.
     * This method is primarily intended for testing purposes.
     */
    public void reset() {
        this.debugEnabled = false;
        this.buildDirectory = null;
    }
}