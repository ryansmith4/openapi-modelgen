package com.guidedbyte.openapi.modelgen;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.options.Option;

import java.util.Arrays;
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
 *   <li><strong>templateDir:</strong> Override template directory for this spec</li>
 *   <li><strong>templateCustomizationsDir:</strong> Override template customizations directory for this spec</li>
 *   <li><strong>modelNameSuffix:</strong> Override model name suffix for this spec</li>
 *   <li><strong>validateSpec:</strong> Override validation setting for this spec</li>
 *   <li><strong>applyPluginCustomizations:</strong> Override plugin YAML customizations setting for this spec</li>
 *   <li><strong>generateModelTests:</strong> Override model test generation for this spec</li>
 *   <li><strong>generateApiTests:</strong> Override API test generation for this spec</li>
 *   <li><strong>generateApiDocumentation:</strong> Override API documentation generation for this spec</li>
 *   <li><strong>generateModelDocumentation:</strong> Override model documentation generation for this spec</li>
 *   <li><strong>templateVariables:</strong> Additional or override template variables</li>
 *   <li><strong>configOptions:</strong> Spec-specific OpenAPI Generator options</li>
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
 *         templateDir "src/main/resources/legacy-templates"
 *         generateModelTests false  // Skip tests for legacy spec
 *         generateApiDocumentation true  // Generate docs despite being legacy
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
    private final Property<String> modelNameSuffix;
    private final Property<String> outputDir;
    private final Property<String> templateDir;
    private final Property<String> templateCustomizationsDir;
    private final Property<Boolean> validateSpec;
    private final ListProperty<String> templateSources;
    private final Property<Boolean> debugTemplateResolution;
    private final Property<Boolean> generateModelTests;
    private final Property<Boolean> generateApiTests;
    private final Property<Boolean> generateApiDocumentation;
    private final Property<Boolean> generateModelDocumentation;
    private final MapProperty<String, String> configOptions;
    private final MapProperty<String, String> globalProperties;
    private final MapProperty<String, String> templateVariables;
    
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
        this.modelNameSuffix = project.getObjects().property(String.class);
        this.outputDir = project.getObjects().property(String.class);
        this.templateDir = project.getObjects().property(String.class);
        this.templateCustomizationsDir = project.getObjects().property(String.class);
        this.validateSpec = project.getObjects().property(Boolean.class);
        this.templateSources = project.getObjects().listProperty(String.class);
        this.debugTemplateResolution = project.getObjects().property(Boolean.class);
        
        // Set smart defaults for templateSources - same as DefaultConfig
        this.templateSources.convention(Arrays.asList(
            "user-templates",
            "user-customizations",
            "library-templates",
            "library-customizations",
            "plugin-customizations",
            "openapi-generator"
        ));
        
        this.generateModelTests = project.getObjects().property(Boolean.class);
        this.generateApiTests = project.getObjects().property(Boolean.class);
        this.generateApiDocumentation = project.getObjects().property(Boolean.class);
        this.generateModelDocumentation = project.getObjects().property(Boolean.class);
        this.configOptions = project.getObjects().mapProperty(String.class, String.class);
        this.globalProperties = project.getObjects().mapProperty(String.class, String.class);
        this.templateVariables = project.getObjects().mapProperty(String.class, String.class);
    }
    
    // Getter methods
    public Property<String> getInputSpec() {
        return inputSpec;
    }
    
    public Property<String> getModelPackage() {
        return modelPackage;
    }
    
    public Property<String> getModelNameSuffix() {
        return modelNameSuffix;
    }
    
    public Property<String> getOutputDir() {
        return outputDir;
    }
    
    public Property<String> getTemplateDir() {
        return templateDir;
    }
    
    public Property<String> getTemplateCustomizationsDir() {
        return templateCustomizationsDir;
    }
    
    public Property<Boolean> getValidateSpec() {
        return validateSpec;
    }
    
    public ListProperty<String> getTemplateSources() {
        return templateSources;
    }
    
    public Property<Boolean> getDebugTemplateResolution() {
        return debugTemplateResolution;
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
    
    // Convenience setter methods for Gradle DSL
    @Option(option = "input-spec", description = "Path to the OpenAPI specification file (YAML or JSON)")
    public void inputSpec(String value) {
        this.inputSpec.set(value);
    }
    
    @Option(option = "model-package", description = "Java package name for generated model classes (e.g., 'com.example.model')")
    public void modelPackage(String value) {
        this.modelPackage.set(value);
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
    public void templateDir(String value) {
        this.templateDir.set(value);
    }
    
    @Option(option = "template-customizations-dir", description = "Directory containing YAML template customization files for this spec")
    public void templateCustomizationsDir(String value) {
        this.templateCustomizationsDir.set(value);
    }
    
    @Option(option = "validate-spec", description = "Enable/disable OpenAPI specification validation for this spec")
    public void validateSpec(boolean value) {
        this.validateSpec.set(value);
    }
    
    @Option(option = "template-sources", description = "Ordered list of template sources for this spec")
    public void templateSources(java.util.List<String> sources) {
        this.templateSources.set(sources);
    }
    
    @Option(option = "debug-template-resolution", description = "Enable debug logging for template resolution for this spec")
    public void debugTemplateResolution(boolean value) {
        this.debugTemplateResolution.set(value);
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
    
    public void globalProperties(Map<String, String> properties) {
        this.globalProperties.set(properties);
    }
    
    public void templateVariables(Map<String, String> variables) {
        this.templateVariables.set(variables);
    }
}