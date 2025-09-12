package com.guidedbyte.openapi.modelgen.constants;

/**
 * Central constants for the OpenAPI Model Generator plugin.
 * Provides a single source of truth for commonly used string literals
 * to improve maintainability and reduce typos.
 */
public final class PluginConstants {
    
    // Prevent instantiation
    private PluginConstants() {
        throw new AssertionError("Constants class should not be instantiated");
    }
    
    // Generator defaults
    /** Default generator name used when none is specified. */
    public static final String DEFAULT_GENERATOR_NAME = "spring";
    
    // Task naming
    /** Prefix for generated task names. */
    public static final String TASK_PREFIX = "generate";
    /** Task group name for all plugin tasks. */
    public static final String TASK_GROUP = "openapi modelgen";
    /** Task name for generating all models. */
    public static final String TASK_ALL_MODELS = "generateAllModels";
    /** Task name for cleaning generated files. */
    public static final String TASK_CLEAN = "generateClean";
    /** Task name for showing help information. */
    public static final String TASK_HELP = "generateHelp";
    /** Task name for setting up template directories. */
    public static final String TASK_SETUP_DIRS = "setupTemplateDirectories";
    
    // Directory names
    /** Directory name for template processing work area. */
    public static final String TEMPLATE_WORK_DIR = "template-work";
    /** Directory name for generated sources. */
    public static final String GENERATED_DIR = "generated";
    /** Directory name for sources. */
    public static final String SOURCES_DIR = "sources";
    /** Directory name for OpenAPI specifications. */
    public static final String OPENAPI_DIR = "openapi";
    
    // File extensions
    /** File extension for Mustache template files. */
    public static final String MUSTACHE_EXT = ".mustache";
    /** File extension for YAML files. */
    public static final String YAML_EXT = ".yaml";
    /** Alternative file extension for YAML files. */
    public static final String YML_EXT = ".yml";
    
    // Configuration names
    /** Configuration name for library template dependencies. */
    public static final String LIBRARIES_CONFIG_NAME = "openapiCustomizations";
    
    // Cache related
    /** Cache directory path relative to user home. */
    public static final String CACHE_DIR = ".gradle/caches/openapi-modelgen";
    /** Marker file name for template directories. */
    public static final String TEMPLATE_MARKER_FILE = ".gradle-template-dir";
    
    // Task descriptions
    /** Description for setup template directories task. */
    public static final String DESC_SETUP_DIRS = "Creates required template directories";
    /** Description for generate all models task. */
    public static final String DESC_ALL_MODELS = "Generates models for all OpenAPI specifications";
    /** Description for clean task. */
    public static final String DESC_CLEAN = "Removes all generated OpenAPI models and clears template caches";
    /** Description for help task. */
    public static final String DESC_HELP = "Shows usage information and examples for the OpenAPI Model Generator plugin";
    /** Prefix for dynamic task descriptions. */
    public static final String DESC_GENERATE_PREFIX = "Generate models for ";
    /** Suffix for dynamic task descriptions. */
    public static final String DESC_GENERATE_SUFFIX = " OpenAPI specification";
}