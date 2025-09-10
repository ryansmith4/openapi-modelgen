package com.guidedbyte.openapi.modelgen;

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
 *   <li>Type and import mappings (importMappings, typeMappings, additionalProperties)</li>
 * </ul>
 * 
 * <p>This class simplifies method signatures throughout the plugin by providing
 * a single object containing all resolved configuration values.</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 1.2.0
 */
public class ResolvedSpecConfig {
    
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
    private final boolean debug;
    private final boolean saveOriginalTemplates;
    
    // OpenAPI Generator configuration
    private final Map<String, String> configOptions;
    private final Map<String, String> globalProperties;
    private final Map<String, String> templateVariables;
    private final Map<String, String> importMappings;
    private final Map<String, String> typeMappings;
    private final Map<String, String> additionalProperties;
    
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
        this.debug = builder.debug;
        this.saveOriginalTemplates = builder.saveOriginalTemplates;
        this.configOptions = new HashMap<>(builder.configOptions);
        this.globalProperties = new HashMap<>(builder.globalProperties);
        this.templateVariables = new HashMap<>(builder.templateVariables);
        this.importMappings = new HashMap<>(builder.importMappings);
        this.typeMappings = new HashMap<>(builder.typeMappings);
        this.additionalProperties = new HashMap<>(builder.additionalProperties);
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
    
    public Map<String, String> getConfigOptions() {
        return new HashMap<>(configOptions);
    }
    
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
     * Gets the resolved template sources list for this specification.
     * 
     * @return the list of template sources in priority order, or null if not configured
     */
    public List<String> getTemplateSources() {
        return templateSources;
    }
    
    public boolean isDebug() {
        return debug;
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
    public static class Builder {
        private final String specName;
        private String inputSpec;
        private String modelPackage;
        private String generatorName = PluginConstants.DEFAULT_GENERATOR_NAME;
        private String outputDir = "build/" + PluginConstants.GENERATED_DIR + "/" + PluginConstants.SOURCES_DIR + "/" + PluginConstants.OPENAPI_DIR;
        private String userTemplateDir;
        private String userTemplateCustomizationsDir;
        private String modelNamePrefix; // No default value
        private String modelNameSuffix = "Dto";
        private boolean validateSpec = false;
        private boolean generateModelTests = false;
        private boolean generateApiTests = false;
        private boolean generateApiDocumentation = false;
        private boolean generateModelDocumentation = false;
        private List<String> templateSources = TemplateSourceType.getAllAsStrings();
        private boolean debug = false;
        private boolean saveOriginalTemplates = false;
        private Map<String, String> configOptions = new HashMap<>();
        private Map<String, String> globalProperties = new HashMap<>();
        private Map<String, String> templateVariables = new HashMap<>();
        private Map<String, String> importMappings = new HashMap<>();
        private Map<String, String> typeMappings = new HashMap<>();
        private Map<String, String> additionalProperties = new HashMap<>();
        
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
            
            // Extension-level debug flag overrides all other debug settings
            if (extension.isDebug()) {
                this.debug = true;
            }
        }
        
        private String getPluginVersion() {
            try {
                java.util.Properties props = new java.util.Properties();
                try (java.io.InputStream is = getClass().getResourceAsStream("/META-INF/gradle-plugins/com.guidedbyte.openapi-modelgen.properties")) {
                    if (is != null) {
                        props.load(is);
                        return props.getProperty("version", "1.0.0");
                    }
                }
            } catch (Exception ignored) {
            }
            return "1.0.0";
        }
        
        private void applyPluginDefaults() {
            // Plugin defaults are already set as field initializers
            
            // Default config options
            configOptions.put("annotationLibrary", "swagger2");
            configOptions.put("swagger2AnnotationLibrary", "true");
            configOptions.put("useSpringBoot3", "true");
            configOptions.put("useJakartaEe", "true");
            configOptions.put("useBeanValidation", "true");
            configOptions.put("dateLibrary", "java8");
            configOptions.put("serializableModel", "true");
            configOptions.put("hideGenerationTimestamp", "true");
            configOptions.put("performBeanValidation", "true");
            configOptions.put("enumUnknownDefaultCase", "true");
            configOptions.put("generateBuilders", "true");
            configOptions.put("legacyDiscriminatorBehavior", "false");
            configOptions.put("disallowAdditionalPropertiesIfNotPresent", "false");
            configOptions.put("useEnumCaseInsensitive", "true");
            configOptions.put("openApiNullable", "false");
            // Lombok compatibility options
            configOptions.put("skipDefaults", "true"); // Skip default constructor generation when using Lombok
            configOptions.put("generateConstructorPropertiesAnnotation", "false"); // Don't generate @ConstructorProperties
            
            // Default global properties
            globalProperties.put("models", "");
            
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
            if (defaults.getTemplateSources().isPresent()) {
                this.templateSources = defaults.getTemplateSources().get();
            }
            if (defaults.getDebug().isPresent()) {
                this.debug = defaults.getDebug().get();
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
            if (spec.getDebug().isPresent()) {
                this.debug = spec.getDebug().get();
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