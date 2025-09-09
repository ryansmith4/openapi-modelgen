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
    public static final String DEFAULT_GENERATOR_NAME = "spring";
    
    // Task naming
    public static final String TASK_PREFIX = "generate";
    public static final String TASK_GROUP = "openapi modelgen";
    public static final String TASK_ALL_MODELS = "generateAllModels";
    public static final String TASK_CLEAN = "generateClean";
    public static final String TASK_HELP = "generateHelp";
    public static final String TASK_SETUP_DIRS = "setupTemplateDirectories";
    
    // Directory names
    public static final String TEMPLATE_WORK_DIR = "template-work";
    public static final String GENERATED_DIR = "generated";
    public static final String SOURCES_DIR = "sources";
    public static final String OPENAPI_DIR = "openapi";
    
    // File extensions
    public static final String MUSTACHE_EXT = ".mustache";
    public static final String YAML_EXT = ".yaml";
    public static final String YML_EXT = ".yml";
    
    // Configuration names
    public static final String LIBRARIES_CONFIG_NAME = "openapiCustomizations";
    
    // Cache related
    public static final String CACHE_DIR = ".gradle/caches/openapi-modelgen";
    public static final String TEMPLATE_MARKER_FILE = ".gradle-template-dir";
    
    // Task descriptions
    public static final String DESC_SETUP_DIRS = "Creates required template directories";
    public static final String DESC_ALL_MODELS = "Generates models for all OpenAPI specifications";
    public static final String DESC_CLEAN = "Removes all generated OpenAPI models and clears template caches";
    public static final String DESC_HELP = "Shows usage information and examples for the OpenAPI Model Generator plugin";
    public static final String DESC_GENERATE_PREFIX = "Generate models for ";
    public static final String DESC_GENERATE_SUFFIX = " OpenAPI specification";
}