package com.guidedbyte.openapi.modelgen;

import org.gradle.api.Project;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.options.Option;

import java.util.Map;

/**
 * Default configuration settings that apply to all OpenAPI specifications.
 * 
 * <p>This class defines the default values and global settings that are inherited 
 * by all individual specification configurations unless explicitly overridden.</p>
 * 
 * <h2>Supported Configuration Options:</h2>
 * <ul>
 *   <li><strong>outputDir:</strong> Base directory for generated code (default: "build/generated/sources/openapi")</li>
 *   <li><strong>templateDir:</strong> Custom template directory path</li>
 *   <li><strong>modelNameSuffix:</strong> Suffix appended to model class names (e.g., "Dto")</li>
 *   <li><strong>validateSpec:</strong> Enable/disable OpenAPI specification validation</li>
 *   <li><strong>templateVariables:</strong> Variables available in Mustache templates</li>
 *   <li><strong>configOptions:</strong> OpenAPI Generator configuration options</li>
 *   <li><strong>globalProperties:</strong> Global properties passed to the generator</li>
 * </ul>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * defaults {
 *     outputDir "build/generated/sources/openapi"
 *     modelNameSuffix "Dto" 
 *     validateSpec true
 *     templateVariables([
 *         copyright: "Copyright © {{currentYear}} {{companyName}}",
 *         currentYear: "2025",
 *         companyName: "MyCompany Inc."
 *     ])
 *     configOptions([
 *         additionalModelTypeAnnotations: "@lombok.Data;@lombok.experimental.SuperBuilder"
 *     ])
 * }
 * }</pre>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
public class DefaultConfig {
    
    private final Property<String> outputDir;
    private final Property<String> templateDir;
    private final Property<String> modelNameSuffix;
    private final Property<Boolean> generateModelTests;
    private final Property<Boolean> generateApiTests;
    private final Property<Boolean> generateApiDocumentation;
    private final Property<Boolean> generateModelDocumentation;
    private final Property<Boolean> validateSpec;
    private final MapProperty<String, String> configOptions;
    private final MapProperty<String, String> globalProperties;
    private final MapProperty<String, String> templateVariables;
    
    /**
     * Creates a new default configuration for the given project.
     * 
     * <p>Initializes all configuration properties using Gradle's property system
     * for proper lazy evaluation and incremental build support.</p>
     * 
     * @param project the Gradle project this configuration belongs to
     */
    public DefaultConfig(Project project) {
        this.outputDir = project.getObjects().property(String.class);
        this.templateDir = project.getObjects().property(String.class);
        this.modelNameSuffix = project.getObjects().property(String.class);
        this.generateModelTests = project.getObjects().property(Boolean.class);
        this.generateApiTests = project.getObjects().property(Boolean.class);
        this.generateApiDocumentation = project.getObjects().property(Boolean.class);
        this.generateModelDocumentation = project.getObjects().property(Boolean.class);
        this.validateSpec = project.getObjects().property(Boolean.class);
        this.configOptions = project.getObjects().mapProperty(String.class, String.class);
        this.globalProperties = project.getObjects().mapProperty(String.class, String.class);
        this.templateVariables = project.getObjects().mapProperty(String.class, String.class);
    }
    
    // Getter methods
    public Property<String> getOutputDir() {
        return outputDir;
    }
    
    public Property<String> getTemplateDir() {
        return templateDir;
    }
    
    public Property<String> getModelNameSuffix() {
        return modelNameSuffix;
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
    
    public Property<Boolean> getValidateSpec() {
        return validateSpec;
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
    @Option(option = "output-dir", description = "Directory where generated code will be written (relative to project root)")
    public void outputDir(String value) {
        this.outputDir.set(value);
    }
    
    @Option(option = "template-dir", description = "Directory containing custom Mustache templates to override defaults")
    public void templateDir(String value) {
        this.templateDir.set(value);
    }
    
    @Option(option = "model-name-suffix", description = "Suffix to append to all generated model class names (e.g., 'Dto', 'Model')")
    public void modelNameSuffix(String value) {
        this.modelNameSuffix.set(value);
    }
    
    @Option(option = "generate-model-tests", description = "Generate unit tests for model classes")
    public void generateModelTests(Boolean value) {
        this.generateModelTests.set(value);
    }
    
    @Option(option = "generate-api-tests", description = "Generate unit tests for API classes")
    public void generateApiTests(Boolean value) {
        this.generateApiTests.set(value);
    }
    
    @Option(option = "generate-api-docs", description = "Generate API documentation from OpenAPI specification")
    public void generateApiDocumentation(Boolean value) {
        this.generateApiDocumentation.set(value);
    }
    
    @Option(option = "generate-model-docs", description = "Generate model documentation from OpenAPI specification")
    public void generateModelDocumentation(Boolean value) {
        this.generateModelDocumentation.set(value);
    }
    
    @Option(option = "validate-spec", description = "Validate OpenAPI specification before code generation")
    public void validateSpec(Boolean value) {
        this.validateSpec.set(value);
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