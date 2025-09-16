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
 *   <li><strong>generator:</strong> Override OpenAPI generator name for this spec</li>
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
 *     kotlinApi {
 *         inputSpec "src/main/resources/openapi-spec/kotlin-api.yaml"
 *         modelPackage "com.example.kotlin.model"
 *         generator "kotlin"  // Use Kotlin generator for this spec
 *     }
 *     legacyOrders {
 *         inputSpec "src/main/resources/openapi-spec/legacy-orders.yaml"
 *         modelPackage "com.example.legacy.orders"
 *         generator "java"    // Use plain Java generator
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
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Gradle DSL configuration class intentionally exposes mutable Property objects for individual spec configuration. " +
                   "This follows the standard Gradle pattern where configuration properties must be accessible to users for the DSL to work. " +
                   "Each Property object manages its own state and provides thread-safe access for configuration."
)
public class SpecConfig {
    
    private final Property<String> inputSpec;
    private final Property<String> modelPackage;
    private final Property<String> generator;
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
        this.generator = project.getObjects().property(String.class);
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
    /**
     * Gets the input OpenAPI specification file property.
     *
     * <p>This property specifies the path to the OpenAPI specification file (YAML or JSON)
     * that serves as the source for code generation. The path is resolved relative to the
     * project root directory.</p>
     *
     * <p>This is a required property for each specification - generation will fail if not provided.</p>
     *
     * @return the input spec property containing the path to the OpenAPI specification file
     */
    public Property<String> getInputSpec() {
        return inputSpec;
    }
    
    /**
     * Gets the model package property.
     *
     * <p>This property specifies the Java package name where generated model classes will be placed.
     * The package name should follow Java naming conventions and typically reflects the application's
     * package structure.</p>
     *
     * <p>This is a required property for each specification - generation will fail if not provided.</p>
     *
     * <p>Example: {@code "com.example.api.model"} or {@code "com.mycompany.service.dto"}</p>
     *
     * @return the model package property containing the target package name for generated classes
     */
    public Property<String> getModelPackage() {
        return modelPackage;
    }

    /**
     * Gets the OpenAPI generator override property for this specification.
     *
     * <p>This property allows overriding the default generator for this specific specification,
     * enabling mixed-generator projects where different specs use different target languages
     * or frameworks.</p>
     *
     * <p>When not set, inherits the generator from default configuration. When set, overrides
     * the default for this specification only.</p>
     *
     * <p>Example use cases:
     * <ul>
     *   <li>Generate TypeScript models for frontend API while using Java for backend</li>
     *   <li>Use plain Java for legacy services and Spring for modern services</li>
     *   <li>Generate Kotlin data classes for specific microservices</li>
     * </ul>
     * </p>
     *
     * @return the generator override property for this specification
     * @see <a href="https://openapi-generator.tech/docs/generators">Available OpenAPI Generators</a>
     */
    public Property<String> getGenerator() {
        return generator;
    }

    /**
     * Gets the model name prefix override property for this specification.
     *
     * <p>This property allows overriding the default model name prefix for this specific
     * specification, enabling per-spec naming customization within the same project.</p>
     *
     * <p>When not set, inherits the prefix from default configuration. When set, overrides
     * the default for this specification only.</p>
     *
     * <p>Example: Use {@code "Legacy"} prefix for legacy API models while using {@code "Api"}
     * for modern API models.</p>
     *
     * @return the model name prefix override property for this specification
     */
    public Property<String> getModelNamePrefix() {
        return modelNamePrefix;
    }
    
    /**
     * Gets the model name suffix override property for this specification.
     *
     * <p>This property allows overriding the default model name suffix for this specific
     * specification, enabling per-spec naming customization within the same project.</p>
     *
     * <p>When not set, inherits the suffix from default configuration. When set, overrides
     * the default for this specification only.</p>
     *
     * <p>Example: Use {@code "Entity"} suffix for database-oriented models while using
     * {@code "Dto"} for API transfer objects.</p>
     *
     * @return the model name suffix override property for this specification
     */
    public Property<String> getModelNameSuffix() {
        return modelNameSuffix;
    }
    
    /**
     * Gets the output directory override property for this specification.
     *
     * <p>This property allows overriding the default output directory for this specific
     * specification, enabling per-spec output organization within the same project.</p>
     *
     * <p>When not set, inherits the output directory from default configuration. When set,
     * overrides the default for this specification only.</p>
     *
     * <p>Useful for organizing generated code by service, version, or other criteria.</p>
     *
     * @return the output directory override property for this specification
     */
    public Property<String> getOutputDir() {
        return outputDir;
    }
    
    /**
     * Gets the user template directory override property for this specification.
     *
     * <p>This property allows overriding the default user template directory for this specific
     * specification, enabling per-spec template customization within the same project.</p>
     *
     * <p>When not set, inherits the template directory from default configuration. When set,
     * allows this specification to use different custom templates than other specs.</p>
     *
     * @return the user template directory override property for this specification
     */
    public Property<String> getUserTemplateDir() {
        return userTemplateDir;
    }
    
    /**
     * Gets the user template customizations directory override property for this specification.
     *
     * <p>This property allows overriding the default template customizations directory for this
     * specific specification, enabling per-spec YAML customizations within the same project.</p>
     *
     * <p>When not set, inherits the customizations directory from default configuration. When set,
     * allows this specification to use different YAML customizations than other specs.</p>
     *
     * @return the user template customizations directory override property for this specification
     */
    public Property<String> getUserTemplateCustomizationsDir() {
        return userTemplateCustomizationsDir;
    }
    
    /**
     * Gets the validate spec override property for this specification.
     *
     * <p>This property allows overriding the default validation setting for this specific
     * specification, enabling per-spec validation control within the same project.</p>
     *
     * <p>When not set, inherits the validation setting from default configuration. Useful for
     * disabling validation for legacy or problematic specs while keeping it enabled for others.</p>
     *
     * @return the validate spec override property for this specification
     */
    public Property<Boolean> getValidateSpec() {
        return validateSpec;
    }
    
    /**
     * Gets the template sources override property for this specification.
     *
     * <p>This property allows overriding the default template source list for this specific
     * specification, enabling per-spec template resolution control within the same project.</p>
     *
     * <p>When not set, inherits the template sources from default configuration. When set,
     * allows fine-grained control over which template sources are used for this specification.</p>
     *
     * @return the template sources override property for this specification
     */
    public ListProperty<String> getTemplateSources() {
        return templateSources;
    }
    
    /**
     * Gets the debug override property for this specification.
     *
     * <p>This property allows overriding the default debug setting for this specific
     * specification, enabling per-spec debug logging control within the same project.</p>
     *
     * <p>When not set, inherits the debug setting from default configuration. Useful for
     * enabling detailed logging for problematic specs while keeping other specs quiet.</p>
     *
     * @return the debug override property for this specification
     */
    public Property<Boolean> getDebug() {
        return debug;
    }
    
    
    /**
     * Gets the generate model tests override property for this specification.
     *
     * <p>This property allows overriding the default model test generation setting for this
     * specific specification, enabling per-spec test generation control within the same project.</p>
     *
     * <p>When not set, inherits the setting from default configuration. Useful for enabling
     * tests for some specs while disabling them for others (e.g., legacy or experimental APIs).</p>
     *
     * @return the generate model tests override property for this specification
     */
    public Property<Boolean> getGenerateModelTests() {
        return generateModelTests;
    }
    
    /**
     * Gets the generate API tests override property for this specification.
     *
     * <p>This property allows overriding the default API test generation setting for this
     * specific specification, enabling per-spec test generation control within the same project.</p>
     *
     * <p>When not set, inherits the setting from default configuration. Useful for enabling
     * tests for production APIs while disabling them for internal or legacy APIs.</p>
     *
     * @return the generate API tests override property for this specification
     */
    public Property<Boolean> getGenerateApiTests() {
        return generateApiTests;
    }
    
    /**
     * Gets the generate API documentation override property for this specification.
     *
     * <p>This property allows overriding the default API documentation generation setting for this
     * specific specification, enabling per-spec documentation control within the same project.</p>
     *
     * <p>When not set, inherits the setting from default configuration. Useful for generating
     * documentation for public APIs while omitting it for internal or prototype APIs.</p>
     *
     * @return the generate API documentation override property for this specification
     */
    public Property<Boolean> getGenerateApiDocumentation() {
        return generateApiDocumentation;
    }
    
    /**
     * Gets the generate model documentation override property for this specification.
     *
     * <p>This property allows overriding the default model documentation generation setting for this
     * specific specification, enabling per-spec documentation control within the same project.</p>
     *
     * <p>When not set, inherits the setting from default configuration. Useful for generating
     * detailed model documentation for public APIs while omitting it for internal schemas.</p>
     *
     * @return the generate model documentation override property for this specification
     */
    public Property<Boolean> getGenerateModelDocumentation() {
        return generateModelDocumentation;
    }
    
    /**
     * Gets the config options override property for this specification.
     *
     * <p>This property allows providing additional or overriding OpenAPI Generator configuration
     * options for this specific specification. These options are merged with default config options,
     * with spec-level options taking precedence.</p>
     *
     * <p>Useful for spec-specific generator customizations such as different annotation styles,
     * validation rules, or language-specific features.</p>
     *
     * @return the config options override property for this specification
     */
    public MapProperty<String, String> getConfigOptions() {
        return configOptions;
    }
    
    /**
     * Gets the global properties override property for this specification.
     *
     * <p>This property allows providing additional or overriding global properties for this
     * specific specification. These properties are merged with default global properties,
     * with spec-level properties taking precedence.</p>
     *
     * <p>Useful for spec-specific generation control such as enabling/disabling certain
     * output types or processing modes.</p>
     *
     * @return the global properties override property for this specification
     */
    public MapProperty<String, String> getGlobalProperties() {
        return globalProperties;
    }
    
    /**
     * Gets the template variables override property for this specification.
     *
     * <p>This property allows providing additional or overriding template variables for this
     * specific specification. These variables are merged with default template variables,
     * with spec-level variables taking precedence.</p>
     *
     * <p>Useful for spec-specific template customizations such as different copyright notices,
     * author information, or custom metadata.</p>
     *
     * @return the template variables override property for this specification
     */
    public MapProperty<String, String> getTemplateVariables() {
        return templateVariables;
    }
    
    /**
     * Gets the import mappings override property for this specification.
     *
     * <p>This property allows providing additional or overriding import mappings for this
     * specific specification. These mappings are merged with default import mappings,
     * with spec-level mappings taking precedence.</p>
     *
     * <p>Useful for spec-specific type imports such as different external library classes
     * or custom type implementations.</p>
     *
     * @return the import mappings override property for this specification
     */
    public MapProperty<String, String> getImportMappings() {
        return importMappings;
    }
    
    /**
     * Gets the type mappings override property for this specification.
     *
     * <p>This property allows providing additional or overriding type mappings for this
     * specific specification. These mappings are merged with default type mappings,
     * with spec-level mappings taking precedence.</p>
     *
     * <p>Useful for spec-specific type transformations such as using different Java types
     * for certain OpenAPI formats or custom type handling.</p>
     *
     * @return the type mappings override property for this specification
     */
    public MapProperty<String, String> getTypeMappings() {
        return typeMappings;
    }
    
    /**
     * Gets the additional properties override property for this specification.
     *
     * <p>This property allows providing additional or overriding additional properties for this
     * specific specification. These properties are merged with default additional properties,
     * with spec-level properties taking precedence.</p>
     *
     * <p>Useful for spec-specific generator configuration such as different library variants,
     * feature flags, or generation options.</p>
     *
     * @return the additional properties override property for this specification
     */
    public MapProperty<String, String> getAdditionalProperties() {
        return additionalProperties;
    }
    
    /**
     * Gets the OpenAPI normalizer override property for this specification.
     *
     * <p>This property allows providing additional or overriding OpenAPI normalizer rules for this
     * specific specification. These rules are merged with default normalizer rules,
     * with spec-level rules taking precedence.</p>
     *
     * <p>Useful for spec-specific schema transformations such as different inheritance handling
     * or schema simplification rules.</p>
     *
     * @return the OpenAPI normalizer override property for this specification
     */
    public MapProperty<String, String> getOpenapiNormalizer() {
        return openapiNormalizer;
    }
    
    /**
     * Gets the save original templates override property for this specification.
     *
     * <p>This property allows overriding the default template preservation setting for this
     * specific specification, enabling per-spec template debugging control within the same project.</p>
     *
     * <p>When not set, inherits the setting from default configuration. Useful for saving
     * original templates only for problematic specs that need debugging.</p>
     *
     * @return the save original templates override property for this specification
     */
    public Property<Boolean> getSaveOriginalTemplates() {
        return saveOriginalTemplates;
    }
    
    // Convenience setter methods for Gradle DSL
    /**
     * Sets the input OpenAPI specification file path.
     *
     * <p>This is a required property that specifies the path to the OpenAPI specification file
     * (YAML or JSON format) that serves as the source for code generation. The path is resolved
     * relative to the project root directory.</p>
     *
     * <p>Example paths:
     * <ul>
     *   <li>{@code "src/main/resources/api/pets.yaml"}</li>
     *   <li>{@code "specs/user-service.json"}</li>
     *   <li>{@code "$projectDir/api-definitions/orders.yaml"}</li>
     * </ul>
     * </p>
     *
     * @param value the path to the OpenAPI specification file
     */
    @Option(option = "input-spec", description = "Path to the OpenAPI specification file (YAML or JSON)")
    public void inputSpec(String value) {
        this.inputSpec.set(value);
    }
    
    /**
     * Sets the Java package name for generated model classes.
     *
     * <p>This is a required property that specifies the Java package where all generated
     * model classes for this specification will be placed. The package name should follow
     * Java naming conventions and reflect your application's package structure.</p>
     *
     * <p>Example package names:
     * <ul>
     *   <li>{@code "com.example.api.model.pets"}</li>
     *   <li>{@code "org.mycompany.service.dto"}</li>
     *   <li>{@code "com.acme.backend.generated.orders"}</li>
     * </ul>
     * </p>
     *
     * @param value the Java package name for generated model classes
     */
    @Option(option = "model-package", description = "Java package name for generated model classes (e.g., 'com.example.model')")
    public void modelPackage(String value) {
        this.modelPackage.set(value);
    }

    /**
     * Sets the OpenAPI generator name for this specification.
     *
     * <p>This property allows overriding the default generator for this specific specification,
     * enabling mixed-generator projects where different specs target different languages or frameworks.</p>
     *
     * <p>Common generator choices:
     * <ul>
     *   <li><strong>spring:</strong> Spring Boot with Jakarta EE and Jackson annotations</li>
     *   <li><strong>java:</strong> Plain Java with configurable library support</li>
     *   <li><strong>kotlin:</strong> Kotlin data classes with null safety</li>
     *   <li><strong>typescript-node:</strong> TypeScript interfaces for Node.js</li>
     * </ul>
     * </p>
     *
     * <p>When not set, inherits the generator from default configuration.</p>
     *
     * @param value the OpenAPI generator name (e.g., 'spring', 'java', 'kotlin')
     * @see <a href="https://openapi-generator.tech/docs/generators">Complete list of available generators</a>
     */
    @Option(option = "generator", description = "OpenAPI generator name for this spec (e.g., 'spring', 'java', 'kotlin')")
    public void generator(String value) {
        this.generator.set(value);
    }

    /**
     * Sets the model name prefix override for this specification.
     *
     * <p>This property allows overriding the default model name prefix for this specific
     * specification, enabling per-spec naming customization within the same project.</p>
     *
     * <p>Example: Use {@code "Legacy"} for legacy API models while using {@code "Api"}
     * for modern API models, allowing clear distinction in the same codebase.</p>
     *
     * @param value the prefix to prepend to generated model class names for this spec
     */
    @Option(option = "model-name-prefix", description = "Prefix to prepend to generated model class names for this spec")
    public void modelNamePrefix(String value) {
        this.modelNamePrefix.set(value);
    }
    
    /**
     * Sets the model name suffix override for this specification.
     *
     * <p>This property allows overriding the default model name suffix for this specific
     * specification, enabling per-spec naming customization within the same project.</p>
     *
     * <p>Example: Use {@code "Entity"} for database-oriented models while using {@code "Dto"}
     * for API transfer objects, clearly distinguishing their purpose.</p>
     *
     * @param value the suffix to append to generated model class names for this spec
     */
    @Option(option = "model-name-suffix", description = "Suffix to append to generated model class names for this spec")
    public void modelNameSuffix(String value) {
        this.modelNameSuffix.set(value);
    }
    
    /**
     * Sets the output directory override for this specification.
     *
     * <p>This property allows overriding the default output directory for this specific
     * specification, enabling per-spec output organization within the same project.</p>
     *
     * <p>Useful for organizing generated code by service, version, or other criteria
     * (e.g., {@code "build/generated/v1"} vs {@code "build/generated/v2"}).</p>
     *
     * @param value the output directory path for this spec's generated code
     */
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
    
    /**
     * Sets the validation override for this specification.
     *
     * <p>This property allows overriding the default validation setting for this specific
     * specification, enabling per-spec validation control within the same project.</p>
     *
     * <p>Useful for disabling validation for legacy or problematic specs while keeping
     * it enabled for well-formed specifications.</p>
     *
     * @param value {@code true} to enable validation, {@code false} to disable for this spec
     */
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