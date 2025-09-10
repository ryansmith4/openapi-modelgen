package com.guidedbyte.openapi.modelgen;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.options.Option;

import java.util.Map;

/**
 * Configuration for an individual OpenAPI specification.
 * 
 * <p>This class defines the configuration for a single OpenAPI specification within 
 * a multi-spec setup. Each spec can override default settings and define its own 
 * input file, package structure, and generation options.</p>
 * 
 * <h2>Required Configuration:</h2>
 * <ul>
 *   <li><strong>inputSpec:</strong> Path to the OpenAPI specification file (YAML/JSON)</li>
 *   <li><strong>modelPackage:</strong> Java package name for generated model classes</li>
 * </ul>
 * 
 * <h2>Optional Configuration (overrides defaults):</h2>
 * <ul>
 *   <li><strong>outputDir:</strong> Override output directory for this spec</li>
 *   <li><strong>userTemplateDir:</strong> Override template directory for this spec</li>
 *   <li><strong>userTemplateCustomizationsDir:</strong> Override template customizations directory for this spec</li>
 *   <li><strong>modelNameSuffix:</strong> Override model name suffix for this spec</li>
 *   <li><strong>validateSpec:</strong> Override validation setting for this spec</li>
 *   <li><strong>applyPluginCustomizations:</strong> Override plugin YAML customizations setting for this spec</li>
 *   <li><strong>generateModelTests:</strong> Override model test generation for this spec</li>
 *   <li><strong>generateApiTests:</strong> Override API test generation for this spec</li>
 *   <li><strong>generateApiDocumentation:</strong> Override API documentation generation for this spec</li>
 *   <li><strong>generateModelDocumentation:</strong> Override model documentation generation for this spec</li>
 *   <li><strong>templateVariables:</strong> Additional or override template variables</li>
 *   <li><strong>configOptions:</strong> Spec-specific OpenAPI Generator options</li>
 *   <li><strong>importMappings:</strong> Additional or override import mappings (merged with defaults)</li>
 *   <li><strong>typeMappings:</strong> Additional or override type mappings (merged with defaults)</li>
 *   <li><strong>additionalProperties:</strong> Additional or override OpenAPI Generator properties (merged with defaults)</li>
 *   <li><strong>openapiNormalizer:</strong> Additional or override OpenAPI normalizer rules (merged with defaults)</li>
 *   <li><strong>saveOriginalTemplates:</strong> Override default setting for saving original templates to orig/ subdirectory</li>
 * </ul>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * specs {
 *     pets {
 *         inputSpec "src/main/resources/openapi-spec/pets.yaml"
 *         modelPackage "com.example.model.pets"
 *         modelNameSuffix "Dto"
 *     }
 *     legacyOrders {
 *         inputSpec "src/main/resources/openapi-spec/legacy-orders.yaml"
 *         modelPackage "com.example.legacy.orders"
 *         validateSpec false  // Disable validation for legacy spec
 *         userTemplateDir "src/main/resources/legacy-templates"
 *         generateModelTests false  // Skip tests for legacy spec
 *         generateApiDocumentation true  // Generate docs despite being legacy
 *         importMappings([
 *             'OrderStatus': 'com.example.legacy.OrderStatus'  // Legacy-specific mapping
 *         ])
 *         typeMappings([
 *             'string+legacy-id': 'LegacyId'  // Custom type for legacy spec
 *         ])
 *         additionalProperties([
 *             'library': 'spring-cloud',  // Override default library
 *             'reactive': 'true'          // Legacy-specific additional property
 *         ])
 *         openapiNormalizer([
 *             'KEEP_ONLY_FIRST_TAG_IN_OPERATION': 'true'  // Legacy-specific normalizer rule
 *         ])
 *     }
 * }
 * }</pre>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
public class SpecConfig {
    
    private final Property<String> inputSpec;
    private final Property<String> modelPackage;
    private final Property<String> modelNamePrefix;
    private final Property<String> modelNameSuffix;
    private final Property<String> outputDir;
    private final Property<String> userTemplateDir;
    private final Property<String> userTemplateCustomizationsDir;
    private final Property<Boolean> validateSpec;
    private final ListProperty<String> templateSources;
    private final Property<Boolean> debug;
    private final Property<Boolean> generateModelTests;
    private final Property<Boolean> generateApiTests;
    private final Property<Boolean> generateApiDocumentation;
    private final Property<Boolean> generateModelDocumentation;
    private final MapProperty<String, String> configOptions;
    private final MapProperty<String, String> globalProperties;
    private final MapProperty<String, String> templateVariables;
    private final MapProperty<String, String> importMappings;
    private final MapProperty<String, String> typeMappings;
    private final MapProperty<String, String> additionalProperties;
    private final MapProperty<String, String> openapiNormalizer;
    private final Property<Boolean> saveOriginalTemplates;
    
    /**
     * Creates a new specification configuration for the given project.
     * 
     * <p>Initializes all configuration properties using Gradle's property system.
     * Properties not explicitly set will inherit from default configuration.</p>
     * 
     * @param project the Gradle project this spec configuration belongs to
     */
    public SpecConfig(Project project) {
        this.inputSpec = project.getObjects().property(String.class);
        this.modelPackage = project.getObjects().property(String.class);
        this.modelNamePrefix = project.getObjects().property(String.class);
        this.modelNameSuffix = project.getObjects().property(String.class);
        this.outputDir = project.getObjects().property(String.class);
        this.userTemplateDir = project.getObjects().property(String.class);
        this.userTemplateCustomizationsDir = project.getObjects().property(String.class);
        this.validateSpec = project.getObjects().property(Boolean.class);
        this.templateSources = project.getObjects().listProperty(String.class);
        this.debug = project.getObjects().property(Boolean.class);
        
        // No default convention for templateSources - let specs inherit from defaults
        // or explicitly override as needed
        
        this.generateModelTests = project.getObjects().property(Boolean.class);
        this.generateApiTests = project.getObjects().property(Boolean.class);
        this.generateApiDocumentation = project.getObjects().property(Boolean.class);
        this.generateModelDocumentation = project.getObjects().property(Boolean.class);
        this.configOptions = project.getObjects().mapProperty(String.class, String.class);
        this.globalProperties = project.getObjects().mapProperty(String.class, String.class);
        this.templateVariables = project.getObjects().mapProperty(String.class, String.class);
        this.importMappings = project.getObjects().mapProperty(String.class, String.class);
        this.typeMappings = project.getObjects().mapProperty(String.class, String.class);
        this.additionalProperties = project.getObjects().mapProperty(String.class, String.class);
        this.openapiNormalizer = project.getObjects().mapProperty(String.class, String.class);
        this.saveOriginalTemplates = project.getObjects().property(Boolean.class);
        // No default convention - inherits from DefaultConfig
    }
    
    // Getter methods
    public Property<String> getInputSpec() {
        return inputSpec;
    }
    
    public Property<String> getModelPackage() {
        return modelPackage;
    }
    
    public Property<String> getModelNamePrefix() {
        return modelNamePrefix;
    }
    
    public Property<String> getModelNameSuffix() {
        return modelNameSuffix;
    }
    
    public Property<String> getOutputDir() {
        return outputDir;
    }
    
    public Property<String> getUserTemplateDir() {
        return userTemplateDir;
    }
    
    public Property<String> getUserTemplateCustomizationsDir() {
        return userTemplateCustomizationsDir;
    }
    
    public Property<Boolean> getValidateSpec() {
        return validateSpec;
    }
    
    public ListProperty<String> getTemplateSources() {
        return templateSources;
    }
    
    public Property<Boolean> getDebug() {
        return debug;
    }
    
    
    public Property<Boolean> getGenerateModelTests() {
        return generateModelTests;
    }
    
    public Property<Boolean> getGenerateApiTests() {
        return generateApiTests;
    }
    
    public Property<Boolean> getGenerateApiDocumentation() {
        return generateApiDocumentation;
    }
    
    public Property<Boolean> getGenerateModelDocumentation() {
        return generateModelDocumentation;
    }
    
    public MapProperty<String, String> getConfigOptions() {
        return configOptions;
    }
    
    public MapProperty<String, String> getGlobalProperties() {
        return globalProperties;
    }
    
    public MapProperty<String, String> getTemplateVariables() {
        return templateVariables;
    }
    
    public MapProperty<String, String> getImportMappings() {
        return importMappings;
    }
    
    public MapProperty<String, String> getTypeMappings() {
        return typeMappings;
    }
    
    public MapProperty<String, String> getAdditionalProperties() {
        return additionalProperties;
    }
    
    public MapProperty<String, String> getOpenapiNormalizer() {
        return openapiNormalizer;
    }
    
    public Property<Boolean> getSaveOriginalTemplates() {
        return saveOriginalTemplates;
    }
    
    // Convenience setter methods for Gradle DSL
    @Option(option = "input-spec", description = "Path to the OpenAPI specification file (YAML or JSON)")
    public void inputSpec(String value) {
        this.inputSpec.set(value);
    }
    
    @Option(option = "model-package", description = "Java package name for generated model classes (e.g., 'com.example.model')")
    public void modelPackage(String value) {
        this.modelPackage.set(value);
    }
    
    @Option(option = "model-name-prefix", description = "Prefix to prepend to generated model class names for this spec")
    public void modelNamePrefix(String value) {
        this.modelNamePrefix.set(value);
    }
    
    @Option(option = "model-name-suffix", description = "Suffix to append to generated model class names for this spec")
    public void modelNameSuffix(String value) {
        this.modelNameSuffix.set(value);
    }
    
    @Option(option = "output-dir", description = "Output directory for this spec's generated code")
    public void outputDir(String value) {
        this.outputDir.set(value);
    }
    
    @Option(option = "template-dir", description = "Custom template directory for this spec")
    public void userTemplateDir(String value) {
        this.userTemplateDir.set(value);
    }
    
    @Option(option = "template-customizations-dir", description = "Directory containing YAML template customization files for this spec")
    public void userTemplateCustomizationsDir(String value) {
        this.userTemplateCustomizationsDir.set(value);
    }
    
    @Option(option = "validate-spec", description = "Enable/disable OpenAPI specification validation for this spec")
    public void validateSpec(boolean value) {
        this.validateSpec.set(value);
    }
    
    @Option(option = "template-sources", description = "Ordered list of template sources for this spec")
    public void templateSources(java.util.List<String> sources) {
        this.templateSources.set(sources);
    }
    
    @Option(option = "debug", description = "Enable comprehensive debug logging for this spec")
    public void debug(boolean value) {
        this.debug.set(value);
    }
    
    
    @Option(option = "generate-model-tests", description = "Generate unit tests for model classes for this spec")
    public void generateModelTests(boolean value) {
        this.generateModelTests.set(value);
    }
    
    @Option(option = "generate-api-tests", description = "Generate unit tests for API classes for this spec")
    public void generateApiTests(boolean value) {
        this.generateApiTests.set(value);
    }
    
    @Option(option = "generate-api-docs", description = "Generate API documentation for this spec")
    public void generateApiDocumentation(boolean value) {
        this.generateApiDocumentation.set(value);
    }
    
    @Option(option = "generate-model-docs", description = "Generate model documentation for this spec")
    public void generateModelDocumentation(boolean value) {
        this.generateModelDocumentation.set(value);
    }
    
    public void configOptions(Map<String, String> options) {
        this.configOptions.set(options);
    }
    
    /**
     * Sets global properties for this specification.
     * These are merged with default global properties, with spec taking precedence.
     * 
     * @param properties map of global properties specific to this spec
     */
    public void globalProperties(Map<String, String> properties) {
        this.globalProperties.set(properties);
    }
    
    /**
     * Sets template variables for this specification.
     * These are merged with default template variables, with spec taking precedence.
     * 
     * @param variables map of template variables specific to this spec
     */
    public void templateVariables(Map<String, String> variables) {
        this.templateVariables.set(variables);
    }
    
    /**
     * Sets import mappings for this specification.
     * These are merged with default import mappings, with spec taking precedence.
     * 
     * @param mappings map of type names to fully qualified import statements
     */
    public void importMappings(Map<String, String> mappings) {
        this.importMappings.set(mappings);
    }
    
    /**
     * Sets type mappings for this specification.
     * These are merged with default type mappings, with spec taking precedence.
     * 
     * @param mappings map of OpenAPI types to Java types
     */
    public void typeMappings(Map<String, String> mappings) {
        this.typeMappings.set(mappings);
    }
    
    /**
     * Sets additional properties for this specification.
     * These are merged with default additional properties, with spec taking precedence.
     * 
     * @param properties map of additional properties for generator-specific configuration
     */
    public void additionalProperties(Map<String, String> properties) {
        this.additionalProperties.set(properties);
    }
    
    /**
     * Sets OpenAPI normalizer rules for this specification.
     * These are merged with default normalizer rules, with spec taking precedence.
     * 
     * @param rules map of normalizer rules and their values for this spec
     */
    public void openapiNormalizer(Map<String, String> rules) {
        this.openapiNormalizer.set(rules);
    }
    
    /**
     * Saves original OpenAPI Generator templates to a subdirectory for reference.
     * 
     * <p>When enabled, extracts and preserves the original OpenAPI Generator templates
     * to {@code build/template-work/{generator}-{specName}/orig/} before applying any
     * customizations. This overrides the default setting for this specific spec.</p>
     * 
     * @param saveOriginalTemplates {@code true} to save original templates before customization
     */
    @Option(option = "save-original-templates", description = "Save original OpenAPI Generator templates to orig/ subdirectory for this spec")
    public void saveOriginalTemplates(Boolean saveOriginalTemplates) {
        this.saveOriginalTemplates.set(saveOriginalTemplates);
    }
}