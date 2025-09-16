package com.guidedbyte.openapi.modelgen;

import com.guidedbyte.openapi.modelgen.constants.OpenApiConfigDefaults;
import com.guidedbyte.openapi.modelgen.constants.PluginConstants;
import com.guidedbyte.openapi.modelgen.constants.TemplateSourceType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the fully resolved configuration for a single OpenAPI specification.
 * This class merges plugin defaults, user defaults, and spec-specific configuration
 * into a single unified configuration object.
 * 
 * <p>Configuration precedence (highest to lowest):</p>
 * <ol>
 *   <li>Spec-level configuration</li>
 *   <li>User default configuration</li>
 *   <li>Plugin default configuration</li>
 * </ol>
 * 
 * <p>This class is configuration-cache compatible and contains all resolved values including:</p>
 * <ul>
 *   <li>Template resolution configuration (templateSources, debug)</li>
 *   <li>Generation control flags (validateSpec, generateTests, generateDocs)</li>
 *   <li>Template customization settings (applyPluginCustomizations)</li>
 *   <li>OpenAPI Generator options and template variables</li>
 *   <li>Type and import mappings (importMappings, typeMappings, additionalProperties, openapiNormalizer)</li>
 * </ul>
 * 
 * <p>This class simplifies method signatures throughout the plugin by providing
 * a single object containing all resolved configuration values.</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 1.2.0
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Configuration data transfer object intentionally stores collection references as final fields for immutability. " +
                   "All getter methods return defensive copies (new HashMap) to prevent external mutation. " +
                   "The internal collections are only modified during construction and are effectively immutable afterward."
)
public class ResolvedSpecConfig {
    
    /**
     * Plugin version loaded lazily when first requested.
     * This avoids static initialization failures in test environments.
     */
    private static volatile String PLUGIN_VERSION = null;
    
    /**
     * Gets the plugin version, loading it on first access.
     * The version should be available from JAR manifest in production environments.
     * 
     * @return the plugin version from build system
     */
    public static String getPluginVersion() {
        if (PLUGIN_VERSION == null) {
            synchronized (ResolvedSpecConfig.class) {
                if (PLUGIN_VERSION == null) {
                    PLUGIN_VERSION = loadVersionFromBuildSystem();
                }
            }
        }
        return PLUGIN_VERSION;
    }
    
    /**
     * Loads the plugin version from available sources.
     * Prioritizes JAR manifest, with fallback to system properties for test environments.
     * 
     * @return the plugin version, or a development version if not available
     */
    private static String loadVersionFromBuildSystem() {
        // First try JAR manifest (production environment)
        Package pkg = ResolvedSpecConfig.class.getPackage();
        if (pkg != null) {
            String manifestVersion = pkg.getImplementationVersion();
            if (manifestVersion != null && !manifestVersion.trim().isEmpty()) {
                return manifestVersion.trim();
            }
        }
        
        // Fallback to system property (test environment or development)
        String version = System.getProperty("plugin.version");
        if (version != null && !version.trim().isEmpty()) {
            return version.trim();
        }
        
        // Final fallback for development/test environments
        // This is acceptable since version is not critical for functionality
        return "1.0.0-DEVELOPMENT";
    }
    
    // Core required fields
    private final String specName;
    private final String inputSpec;
    private final String modelPackage;
    private final String generatorName;
    
    // Template configuration
    private final String outputDir;
    private final String userTemplateDir;
    private final String userTemplateCustomizationsDir;
    private final String modelNamePrefix;
    private final String modelNameSuffix;
    
    // Generation flags
    private final boolean validateSpec;
    private final boolean generateModelTests;
    private final boolean generateApiTests;
    private final boolean generateApiDocumentation;
    private final boolean generateModelDocumentation;
    
    // Template resolution configuration
    private final List<String> templateSources;
    private final boolean saveOriginalTemplates;
    
    // OpenAPI Generator configuration
    private final Map<String, String> configOptions;
    private final Map<String, String> globalProperties;
    private final Map<String, String> templateVariables;
    private final Map<String, String> importMappings;
    private final Map<String, String> typeMappings;
    private final Map<String, String> additionalProperties;
    private final Map<String, String> openapiNormalizer;
    
    private ResolvedSpecConfig(Builder builder) {
        this.specName = builder.specName;
        this.inputSpec = builder.inputSpec;
        this.modelPackage = builder.modelPackage;
        this.generatorName = builder.generatorName;
        this.outputDir = builder.outputDir;
        this.userTemplateDir = builder.userTemplateDir;
        this.userTemplateCustomizationsDir = builder.userTemplateCustomizationsDir;
        this.modelNamePrefix = builder.modelNamePrefix;
        this.modelNameSuffix = builder.modelNameSuffix;
        this.validateSpec = builder.validateSpec;
        this.generateModelTests = builder.generateModelTests;
        this.generateApiTests = builder.generateApiTests;
        this.generateApiDocumentation = builder.generateApiDocumentation;
        this.generateModelDocumentation = builder.generateModelDocumentation;
        this.templateSources = builder.templateSources;
        this.saveOriginalTemplates = builder.saveOriginalTemplates;
        this.configOptions = new HashMap<>(builder.configOptions);
        this.globalProperties = new HashMap<>(builder.globalProperties);
        this.templateVariables = new HashMap<>(builder.templateVariables);
        this.importMappings = new HashMap<>(builder.importMappings);
        this.typeMappings = new HashMap<>(builder.typeMappings);
        this.additionalProperties = new HashMap<>(builder.additionalProperties);
        this.openapiNormalizer = new HashMap<>(builder.openapiNormalizer);
    }
    
    // Getters
    public String getSpecName() {
        return specName;
    }
    
    public String getInputSpec() {
        return inputSpec;
    }
    
    public String getModelPackage() {
        return modelPackage;
    }
    
    /**
     * Gets the OpenAPI generator name for this specification.
     *
     * @return the generator name (e.g., "spring")
     */
    public String getGeneratorName() {
        return generatorName;
    }
    
    public String getOutputDir() {
        return outputDir;
    }
    
    public String getUserTemplateDir() {
        return userTemplateDir;
    }
    
    public String getUserTemplateCustomizationsDir() {
        return userTemplateCustomizationsDir;
    }
    
    public String getModelNamePrefix() {
        return modelNamePrefix;
    }
    
    public String getModelNameSuffix() {
        return modelNameSuffix;
    }
    
    public boolean isValidateSpec() {
        return validateSpec;
    }
    
    
    public boolean isGenerateModelTests() {
        return generateModelTests;
    }
    
    public boolean isGenerateApiTests() {
        return generateApiTests;
    }
    
    public boolean isGenerateApiDocumentation() {
        return generateApiDocumentation;
    }
    
    public boolean isGenerateModelDocumentation() {
        return generateModelDocumentation;
    }
    
    /**
     * Gets the resolved OpenAPI Generator config options for this specification.
     *
     * @return copy of the config options map (defaults + spec overrides)
     */
    public Map<String, String> getConfigOptions() {
        return new HashMap<>(configOptions);
    }
    
    /**
     * Gets the resolved OpenAPI Generator global properties for this specification.
     *
     * @return copy of the global properties map (defaults + spec overrides)
     */
    public Map<String, String> getGlobalProperties() {
        return new HashMap<>(globalProperties);
    }
    
    /**
     * Gets the resolved template variables for this specification.
     * 
     * @return merged map of template variables (defaults + spec overrides)
     */
    public Map<String, String> getTemplateVariables() {
        return new HashMap<>(templateVariables);
    }
    
    /**
     * Gets the resolved import mappings for this specification.
     * 
     * @return merged map of type names to import statements (defaults + spec overrides)
     */
    public Map<String, String> getImportMappings() {
        return new HashMap<>(importMappings);
    }
    
    /**
     * Gets the resolved type mappings for this specification.
     * 
     * @return merged map of OpenAPI types to Java types (defaults + spec overrides)
     */
    public Map<String, String> getTypeMappings() {
        return new HashMap<>(typeMappings);
    }
    
    /**
     * Gets the resolved additional properties for this specification.
     * 
     * @return merged map of additional properties (defaults + spec overrides)
     */
    public Map<String, String> getAdditionalProperties() {
        return new HashMap<>(additionalProperties);
    }
    
    /**
     * Gets the resolved OpenAPI normalizer rules for this specification.
     * 
     * @return merged map of normalizer rules (defaults + spec overrides)
     */
    public Map<String, String> getOpenapiNormalizer() {
        return new HashMap<>(openapiNormalizer);
    }
    
    
    /**
     * Gets the resolved template sources list for this specification.
     * 
     * @return the list of template sources in priority order, or null if not configured
     */
    public List<String> getTemplateSources() {
        return templateSources;
    }
    
    
    public boolean isSaveOriginalTemplates() {
        return saveOriginalTemplates;
    }
    
    
    /**
     * Creates a new builder for ResolvedSpecConfig.
     * 
     * @param specName the name of the specification
     * @param userDefaults the user's default configuration  
     * @param specConfig the spec-specific configuration
     * @return a new Builder instance
     */
    public static Builder builder(String specName, DefaultConfig userDefaults, SpecConfig specConfig) {
        return new Builder(specName, userDefaults, specConfig);
    }
    
    /**
     * Creates a new builder for ResolvedSpecConfig with extension-level debug flag support.
     * The extension debug flag will override any debug settings from defaults or spec configs.
     * 
     * @param specName the name of the specification
     * @param extension the plugin extension containing global settings
     * @param specConfig the spec-specific configuration
     * @return a new Builder instance
     */
    public static Builder builder(String specName, OpenApiModelGenExtension extension, SpecConfig specConfig) {
        return new Builder(specName, extension, specConfig);
    }
    
    /**
     * Builder for ResolvedSpecConfig that handles the configuration merging logic.
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Builder pattern intentionally accepts collection parameters and stores references after defensive copying via putAll(). " +
                       "The original collections passed to methods like configOptions(Map) are not stored directly - " +
                       "instead their contents are copied into the builder's internal collections using putAll(), providing proper encapsulation."
    )
    public static class Builder {
        private final String specName;
        private String inputSpec;
        private String modelPackage;
        private String generatorName = PluginConstants.DEFAULT_GENERATOR_NAME;
        private String outputDir = "build/" + PluginConstants.GENERATED_DIR + "/" + PluginConstants.SOURCES_DIR + "/" + PluginConstants.OPENAPI_DIR;
        private String userTemplateDir;
        private String userTemplateCustomizationsDir;
        private String modelNamePrefix; // No default value
        private String modelNameSuffix = OpenApiConfigDefaults.DEFAULT_MODEL_NAME_SUFFIX;
        private boolean validateSpec = false;
        private boolean generateModelTests = false;
        private boolean generateApiTests = false;
        private boolean generateApiDocumentation = false;
        private boolean generateModelDocumentation = false;
        private List<String> templateSources = TemplateSourceType.getAllAsStrings();
        private boolean saveOriginalTemplates = false;
        private Map<String, String> configOptions = new HashMap<>();
        private Map<String, String> globalProperties = new HashMap<>();
        private Map<String, String> templateVariables = new HashMap<>();
        private Map<String, String> importMappings = new HashMap<>();
        private Map<String, String> typeMappings = new HashMap<>();
        private Map<String, String> additionalProperties = new HashMap<>();
        private Map<String, String> openapiNormalizer = new HashMap<>();
        
        private Builder(String specName, DefaultConfig userDefaults, SpecConfig specConfig) {
            this.specName = specName;
            
            // Apply configuration in order of precedence (lowest to highest)
            applyPluginDefaults();
            applyUserDefaults(userDefaults);
            applySpecConfig(specConfig);
        }
        
        private Builder(String specName, OpenApiModelGenExtension extension, SpecConfig specConfig) {
            this.specName = specName;
            
            // Apply configuration in order of precedence (lowest to highest)
            applyPluginDefaults();
            applyUserDefaults(extension.getDefaults());
            applySpecConfig(specConfig);
            
        }
        
        // Note: getPluginVersion() method is now defined at the class level above
        
        private void applyPluginDefaults() {
            // Plugin defaults are already set as field initializers
            
            // Default config options
            configOptions.put("annotationLibrary", OpenApiConfigDefaults.ANNOTATION_LIBRARY);
            configOptions.put("swagger2AnnotationLibrary", OpenApiConfigDefaults.SWAGGER2_ANNOTATION_LIBRARY);
            configOptions.put("useSpringBoot3", OpenApiConfigDefaults.USE_SPRING_BOOT_3);
            configOptions.put("useJakartaEe", OpenApiConfigDefaults.USE_JAKARTA_EE);
            configOptions.put("useBeanValidation", OpenApiConfigDefaults.USE_BEAN_VALIDATION);
            configOptions.put("dateLibrary", OpenApiConfigDefaults.DATE_LIBRARY);
            configOptions.put("serializableModel", OpenApiConfigDefaults.SERIALIZABLE_MODEL);
            configOptions.put("hideGenerationTimestamp", OpenApiConfigDefaults.HIDE_GENERATION_TIMESTAMP);
            configOptions.put("performBeanValidation", OpenApiConfigDefaults.PERFORM_BEAN_VALIDATION);
            configOptions.put("enumUnknownDefaultCase", OpenApiConfigDefaults.ENUM_UNKNOWN_DEFAULT_CASE);
            configOptions.put("generateBuilders", OpenApiConfigDefaults.GENERATE_BUILDERS);
            configOptions.put("legacyDiscriminatorBehavior", OpenApiConfigDefaults.LEGACY_DISCRIMINATOR_BEHAVIOR);
            configOptions.put("disallowAdditionalPropertiesIfNotPresent", OpenApiConfigDefaults.DISALLOW_ADDITIONAL_PROPERTIES_IF_NOT_PRESENT);
            configOptions.put("useEnumCaseInsensitive", OpenApiConfigDefaults.USE_ENUM_CASE_INSENSITIVE);
            configOptions.put("openApiNullable", OpenApiConfigDefaults.OPENAPI_NULLABLE);
            // Lombok compatibility options
            configOptions.put("skipDefaults", OpenApiConfigDefaults.SKIP_DEFAULTS); // Skip default constructor generation when using Lombok
            configOptions.put("generateConstructorPropertiesAnnotation", OpenApiConfigDefaults.GENERATE_CONSTRUCTOR_PROPERTIES_ANNOTATION); // Don't generate @ConstructorProperties
            
            // Default global properties
            globalProperties.put("models", OpenApiConfigDefaults.MODELS_ONLY);
            
            // Default template variables
            templateVariables.put("currentYear", String.valueOf(java.time.Year.now().getValue()));
            templateVariables.put("generatedBy", "OpenAPI Model Generator Plugin");
            templateVariables.put("pluginVersion", getPluginVersion());
            
        }
        
        private void applyUserDefaults(DefaultConfig defaults) {
            if (defaults == null) {
                return;
            }
            
            if (defaults.getOutputDir().isPresent()) {
                this.outputDir = defaults.getOutputDir().get();
            }
            if (defaults.getUserTemplateDir().isPresent()) {
                this.userTemplateDir = defaults.getUserTemplateDir().get();
            }
            if (defaults.getUserTemplateCustomizationsDir().isPresent()) {
                this.userTemplateCustomizationsDir = defaults.getUserTemplateCustomizationsDir().get();
            }
            if (defaults.getGenerator().isPresent()) {
                this.generatorName = defaults.getGenerator().get();
            }
            if (defaults.getModelNamePrefix().isPresent()) {
                this.modelNamePrefix = defaults.getModelNamePrefix().get();
            }
            if (defaults.getModelNameSuffix().isPresent()) {
                this.modelNameSuffix = defaults.getModelNameSuffix().get();
            }
            if (defaults.getValidateSpec().isPresent()) {
                this.validateSpec = defaults.getValidateSpec().get();
            }
            if (defaults.getGenerateModelTests().isPresent()) {
                this.generateModelTests = defaults.getGenerateModelTests().get();
            }
            if (defaults.getGenerateApiTests().isPresent()) {
                this.generateApiTests = defaults.getGenerateApiTests().get();
            }
            if (defaults.getGenerateApiDocumentation().isPresent()) {
                this.generateApiDocumentation = defaults.getGenerateApiDocumentation().get();
            }
            if (defaults.getGenerateModelDocumentation().isPresent()) {
                this.generateModelDocumentation = defaults.getGenerateModelDocumentation().get();
            }
            if (defaults.getConfigOptions().isPresent()) {
                this.configOptions.putAll(defaults.getConfigOptions().get());
            }
            if (defaults.getGlobalProperties().isPresent()) {
                this.globalProperties.putAll(defaults.getGlobalProperties().get());
            }
            if (defaults.getTemplateVariables().isPresent()) {
                this.templateVariables.putAll(defaults.getTemplateVariables().get());
            }
            if (defaults.getImportMappings().isPresent()) {
                this.importMappings.putAll(defaults.getImportMappings().get());
            }
            if (defaults.getTypeMappings().isPresent()) {
                this.typeMappings.putAll(defaults.getTypeMappings().get());
            }
            if (defaults.getAdditionalProperties().isPresent()) {
                this.additionalProperties.putAll(defaults.getAdditionalProperties().get());
            }
            if (defaults.getOpenapiNormalizer().isPresent()) {
                this.openapiNormalizer.putAll(defaults.getOpenapiNormalizer().get());
            }
            if (defaults.getTemplateSources().isPresent()) {
                this.templateSources = defaults.getTemplateSources().get();
            }
            if (defaults.getSaveOriginalTemplates().isPresent()) {
                this.saveOriginalTemplates = defaults.getSaveOriginalTemplates().get();
            }
        }
        
        private void applySpecConfig(SpecConfig spec) {
            if (spec == null) {
                return;
            }
            
            // Required fields
            if (spec.getInputSpec().isPresent()) {
                this.inputSpec = spec.getInputSpec().get();
            }
            if (spec.getModelPackage().isPresent()) {
                this.modelPackage = spec.getModelPackage().get();
            }
            
            // Optional overrides
            if (spec.getOutputDir().isPresent()) {
                this.outputDir = spec.getOutputDir().get();
            }
            if (spec.getUserTemplateDir().isPresent()) {
                this.userTemplateDir = spec.getUserTemplateDir().get();
            }
            if (spec.getUserTemplateCustomizationsDir().isPresent()) {
                this.userTemplateCustomizationsDir = spec.getUserTemplateCustomizationsDir().get();
            }
            if (spec.getGenerator().isPresent()) {
                this.generatorName = spec.getGenerator().get();
            }
            if (spec.getModelNamePrefix().isPresent()) {
                this.modelNamePrefix = spec.getModelNamePrefix().get();
            }
            if (spec.getModelNameSuffix().isPresent()) {
                this.modelNameSuffix = spec.getModelNameSuffix().get();
            }
            if (spec.getValidateSpec().isPresent()) {
                this.validateSpec = spec.getValidateSpec().get();
            }
            if (spec.getGenerateModelTests().isPresent()) {
                this.generateModelTests = spec.getGenerateModelTests().get();
            }
            if (spec.getGenerateApiTests().isPresent()) {
                this.generateApiTests = spec.getGenerateApiTests().get();
            }
            if (spec.getGenerateApiDocumentation().isPresent()) {
                this.generateApiDocumentation = spec.getGenerateApiDocumentation().get();
            }
            if (spec.getGenerateModelDocumentation().isPresent()) {
                this.generateModelDocumentation = spec.getGenerateModelDocumentation().get();
            }
            if (spec.getTemplateSources().isPresent() && !spec.getTemplateSources().get().isEmpty()) {
                this.templateSources = spec.getTemplateSources().get();
            }
            if (spec.getSaveOriginalTemplates().isPresent()) {
                this.saveOriginalTemplates = spec.getSaveOriginalTemplates().get();
            }
            if (spec.getConfigOptions().isPresent()) {
                this.configOptions.putAll(spec.getConfigOptions().get());
            }
            if (spec.getGlobalProperties().isPresent()) {
                this.globalProperties.putAll(spec.getGlobalProperties().get());
            }
            if (spec.getTemplateVariables().isPresent()) {
                this.templateVariables.putAll(spec.getTemplateVariables().get());
            }
            if (spec.getImportMappings().isPresent()) {
                this.importMappings.putAll(spec.getImportMappings().get());
            }
            if (spec.getTypeMappings().isPresent()) {
                this.typeMappings.putAll(spec.getTypeMappings().get());
            }
            if (spec.getAdditionalProperties().isPresent()) {
                this.additionalProperties.putAll(spec.getAdditionalProperties().get());
            }
            if (spec.getOpenapiNormalizer().isPresent()) {
                this.openapiNormalizer.putAll(spec.getOpenapiNormalizer().get());
            }
        }
        
        public ResolvedSpecConfig build() {
            // Validate required fields
            if (inputSpec == null || inputSpec.trim().isEmpty()) {
                throw new IllegalArgumentException("inputSpec is required for spec: " + specName);
            }
            if (modelPackage == null || modelPackage.trim().isEmpty()) {
                throw new IllegalArgumentException("modelPackage is required for spec: " + specName);
            }
            
            return new ResolvedSpecConfig(this);
        }
    }
}