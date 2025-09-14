package com.guidedbyte.openapi.modelgen.constants;

/**
 * Default configuration values for OpenAPI Generator options.
 * These constants represent the plugin's default configuration that optimizes
 * for Spring Boot 3 + Jakarta EE + Lombok compatibility.
 */
public final class OpenApiConfigDefaults {
    
    // Prevent instantiation
    private OpenApiConfigDefaults() {
        throw new AssertionError("Constants class should not be instantiated");
    }
    
    // Model naming defaults
    /** Default model name suffix. */
    public static final String DEFAULT_MODEL_NAME_SUFFIX = "Dto";
    
    // Annotation and library configuration
    /** Default annotation library for OpenAPI Generator. */
    public static final String ANNOTATION_LIBRARY = "swagger2";
    /** Enable Swagger 2 annotation library. */
    public static final String SWAGGER2_ANNOTATION_LIBRARY = "true";
    /** Enable Spring Boot 3 compatibility. */
    public static final String USE_SPRING_BOOT_3 = "true";
    /** Use Jakarta EE instead of Java EE. */
    public static final String USE_JAKARTA_EE = "true";
    /** Enable bean validation annotations. */
    public static final String USE_BEAN_VALIDATION = "true";
    /** Date library to use for date/time types. */
    public static final String DATE_LIBRARY = "java8";
    /** Make models implement Serializable. */
    public static final String SERIALIZABLE_MODEL = "true";
    /** Hide generation timestamp in generated files. */
    public static final String HIDE_GENERATION_TIMESTAMP = "true";
    /** Enable runtime bean validation. */
    public static final String PERFORM_BEAN_VALIDATION = "true";
    /** Handle unknown enum values gracefully. */
    public static final String ENUM_UNKNOWN_DEFAULT_CASE = "true";
    /** Generate builder methods for models. */
    public static final String GENERATE_BUILDERS = "true";
    /** Use modern discriminator behavior. */
    public static final String LEGACY_DISCRIMINATOR_BEHAVIOR = "false";
    /** Allow additional properties when not explicitly forbidden. */
    public static final String DISALLOW_ADDITIONAL_PROPERTIES_IF_NOT_PRESENT = "false";
    /** Enable case-insensitive enum parsing. */
    public static final String USE_ENUM_CASE_INSENSITIVE = "true";
    /** Disable OpenAPI nullable library (conflicts with Lombok). */
    public static final String OPENAPI_NULLABLE = "false";
    
    // Lombok compatibility options
    /** Skip default constructor generation (Lombok provides them). */
    public static final String SKIP_DEFAULTS = "true";
    /** Don't generate @ConstructorProperties annotation. */
    public static final String GENERATE_CONSTRUCTOR_PROPERTIES_ANNOTATION = "false";
    
    // Global properties
    /** Generate only models, not APIs. */
    public static final String MODELS_ONLY = "";
}