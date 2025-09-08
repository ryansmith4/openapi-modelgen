package com.guidedbyte.openapi.modelgen;

import java.util.Arrays;
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
 *   <li>Template resolution configuration (templateSources, debugTemplateResolution)</li>
 *   <li>Generation control flags (validateSpec, generateTests, generateDocs)</li>
 *   <li>Template customization settings (applyPluginCustomizations)</li>
 *   <li>OpenAPI Generator options and template variables</li>
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
    private final String templateDir;
    private final String templateCustomizationsDir;
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
    private final boolean debugTemplateResolution;
    
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
        this.templateDir = builder.templateDir;
        this.templateCustomizationsDir = builder.templateCustomizationsDir;
        this.modelNamePrefix = builder.modelNamePrefix;
        this.modelNameSuffix = builder.modelNameSuffix;
        this.validateSpec = builder.validateSpec;
        this.generateModelTests = builder.generateModelTests;
        this.generateApiTests = builder.generateApiTests;
        this.generateApiDocumentation = builder.generateApiDocumentation;
        this.generateModelDocumentation = builder.generateModelDocumentation;
        this.templateSources = builder.templateSources;
        this.debugTemplateResolution = builder.debugTemplateResolution;
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
    
    public String getTemplateDir() {
        return templateDir;
    }
    
    public String getTemplateCustomizationsDir() {
        return templateCustomizationsDir;
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
    
    public Map<String, String> getTemplateVariables() {
        return new HashMap<>(templateVariables);
    }
    
    public Map<String, String> getImportMappings() {
        return new HashMap<>(importMappings);
    }
    
    public Map<String, String> getTypeMappings() {
        return new HashMap<>(typeMappings);
    }
    
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
    
    public boolean isDebugTemplateResolution() {
        return debugTemplateResolution;
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
     * Builder for ResolvedSpecConfig that handles the configuration merging logic.
     */
    public static class Builder {
        private final String specName;
        private String inputSpec;
        private String modelPackage;
        private String generatorName = "spring";
        private String outputDir = "build/generated/sources/openapi";
        private String templateDir;
        private String templateCustomizationsDir;
        private String modelNamePrefix; // No default value
        private String modelNameSuffix = "Dto";
        private boolean validateSpec = false;
        private boolean generateModelTests = false;
        private boolean generateApiTests = false;
        private boolean generateApiDocumentation = false;
        private boolean generateModelDocumentation = false;
        private List<String> templateSources = Arrays.asList(
            "user-templates",
            "user-customizations",
            "library-templates",
            "library-customizations",
            "plugin-customizations",
            "openapi-generator"
        );
        private boolean debugTemplateResolution = false;
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
            if (defaults == null) return;
            
            if (defaults.getOutputDir().isPresent()) {
                this.outputDir = defaults.getOutputDir().get();
            }
            if (defaults.getTemplateDir().isPresent()) {
                this.templateDir = defaults.getTemplateDir().get();
            }
            if (defaults.getTemplateCustomizationsDir().isPresent()) {
                this.templateCustomizationsDir = defaults.getTemplateCustomizationsDir().get();
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
            if (defaults.getDebugTemplateResolution().isPresent()) {
                this.debugTemplateResolution = defaults.getDebugTemplateResolution().get();
            }
        }
        
        private void applySpecConfig(SpecConfig spec) {
            if (spec == null) return;
            
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
            if (spec.getTemplateDir().isPresent()) {
                this.templateDir = spec.getTemplateDir().get();
            }
            if (spec.getTemplateCustomizationsDir().isPresent()) {
                this.templateCustomizationsDir = spec.getTemplateCustomizationsDir().get();
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
            if (spec.getTemplateSources().isPresent()) {
                this.templateSources = spec.getTemplateSources().get();
            }
            if (spec.getDebugTemplateResolution().isPresent()) {
                this.debugTemplateResolution = spec.getDebugTemplateResolution().get();
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