package com.guidedbyte.openapi.modelgen;

import com.guidedbyte.openapi.modelgen.constants.TemplateSourceType;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
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
 *   <li><strong>userTemplateDir:</strong> Directory containing user's custom Mustache templates (copied to build/template-work during processing)</li>
 *   <li><strong>userTemplateCustomizationsDir:</strong> Directory containing user's YAML template customization files (applied to build/template-work)</li>
 *   <li><strong>generator:</strong> OpenAPI generator name (default: "spring", options: "java", "kotlin", "typescript-node", etc.)</li>
 *   <li><strong>modelNamePrefix:</strong> Prefix prepended to model class names (no default)</li>
 *   <li><strong>modelNameSuffix:</strong> Suffix appended to model class names (default: "Dto")</li>
 *   <li><strong>validateSpec:</strong> Enable/disable OpenAPI specification validation (default: false)</li>
 *   <li><strong>templateSources:</strong> Ordered list of template sources with auto-discovery (default: all sources)</li>
 *   <li><strong>templateVariables:</strong> Variables available in Mustache templates (supports nested expansion)</li>
 *   <li><strong>configOptions:</strong> OpenAPI Generator configuration options (pre-configured for Spring Boot 3 + Jakarta EE + Lombok)</li>
 *   <li><strong>globalProperties:</strong> Global properties passed to the generator</li>
 *   <li><strong>generateModelTests:</strong> Enable/disable model unit test generation (default: false)</li>
 *   <li><strong>generateApiTests:</strong> Enable/disable API unit test generation (default: false)</li>
 *   <li><strong>generateApiDocumentation:</strong> Enable/disable API documentation generation (default: false)</li>
 *   <li><strong>generateModelDocumentation:</strong> Enable/disable model documentation generation (default: false)</li>
 *   <li><strong>importMappings:</strong> Map type names to fully qualified import statements (merged with spec-level mappings)</li>
 *   <li><strong>typeMappings:</strong> Map OpenAPI types to Java types (merged with spec-level mappings)</li>
 *   <li><strong>additionalProperties:</strong> Additional properties passed to OpenAPI Generator (merged with spec-level properties)</li>
 *   <li><strong>openapiNormalizer:</strong> OpenAPI normalizer rules to transform input specifications (merged with spec-level rules)</li>
 *   <li><strong>saveOriginalTemplates:</strong> Save original OpenAPI Generator templates to orig/ subdirectory (default: false)</li>
 * </ul>
 * 
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * defaults {
 *     outputDir "build/generated/sources/openapi"
 *     generator "java"  // Use plain Java generator instead of Spring
 *     modelNamePrefix "Api"
 *     modelNameSuffix "Dto"
 *     validateSpec true
 *     
 *     // Configure template resolution (new simplified approach)
 *     templateSources([
 *         "user-templates",          // Project .mustache templates (highest priority)
 *         "user-customizations",     // Project YAML customizations
 *         "library-templates",       // Library .mustache templates
 *         "library-customizations",  // Library YAML customizations
 *         "plugin-customizations",   // Built-in plugin customizations
 *         "openapi-generator"        // OpenAPI Generator defaults (fallback)
 *     ])
 *     
 *     templateVariables([
 *         copyright: "Copyright © {{currentYear}} {{companyName}}",
 *         currentYear: "2025",
 *         companyName: "MyCompany Inc."
 *     ])
 *     configOptions([
 *         additionalModelTypeAnnotations: "@lombok.Data;@lombok.experimental.SuperBuilder"
 *     ])
 *     importMappings([
 *         'UUID': 'java.util.UUID',
 *         'LocalDate': 'java.time.LocalDate',
 *         'BigDecimal': 'java.math.BigDecimal'
 *     ])
 *     typeMappings([
 *         'string+uuid': 'UUID',
 *         'string+date': 'LocalDate'
 *     ])
 *     additionalProperties([
 *         'library': 'spring-boot',
 *         'beanValidations': 'true',
 *         'useSpringBoot3': 'true'
 *     ])
 *     openapiNormalizer([
 *         'REFACTOR_ALLOF_WITH_PROPERTIES_ONLY': 'true',
 *         'SIMPLIFY_ONEOF_ANYOF': 'true'
 *     ])
 * }
 * }</pre>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Gradle DSL configuration class intentionally exposes mutable Property objects for user configuration. " +
                   "This is the standard Gradle pattern for configuration objects where properties must be settable by users. " +
                   "The exposed Property objects are part of the public API and their mutability is required for the DSL to function."
)
public class DefaultConfig {
    
    private final Property<String> outputDir;
    private final Property<String> userTemplateDir;
    private final Property<String> userTemplateCustomizationsDir;
    private final Property<String> generator;
    private final Property<String> modelNamePrefix;
    private final Property<String> modelNameSuffix;
    private final Property<Boolean> generateModelTests;
    private final Property<Boolean> generateApiTests;
    private final Property<Boolean> generateApiDocumentation;
    private final Property<Boolean> generateModelDocumentation;
    private final Property<Boolean> validateSpec;
    private final ListProperty<String> templateSources;
    private final MapProperty<String, String> configOptions;
    private final MapProperty<String, String> globalProperties;
    private final MapProperty<String, String> templateVariables;
    private final MapProperty<String, String> importMappings;
    private final MapProperty<String, String> typeMappings;
    private final MapProperty<String, String> additionalProperties;
    private final MapProperty<String, String> openapiNormalizer;
    private final Property<Boolean> saveOriginalTemplates;
    
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
        this.userTemplateDir = project.getObjects().property(String.class);
        this.userTemplateCustomizationsDir = project.getObjects().property(String.class);
        this.generator = project.getObjects().property(String.class);
        this.modelNamePrefix = project.getObjects().property(String.class);
        this.modelNameSuffix = project.getObjects().property(String.class);
        this.generateModelTests = project.getObjects().property(Boolean.class);
        this.generateApiTests = project.getObjects().property(Boolean.class);
        this.generateApiDocumentation = project.getObjects().property(Boolean.class);
        this.generateModelDocumentation = project.getObjects().property(Boolean.class);
        this.validateSpec = project.getObjects().property(Boolean.class);
        this.templateSources = project.getObjects().listProperty(String.class);
        
        // Set smart defaults for templateSources - all possible sources with auto-discovery
        this.templateSources.convention(TemplateSourceType.getAllAsStrings());
        this.configOptions = project.getObjects().mapProperty(String.class, String.class);
        this.globalProperties = project.getObjects().mapProperty(String.class, String.class);
        this.templateVariables = project.getObjects().mapProperty(String.class, String.class);
        this.importMappings = project.getObjects().mapProperty(String.class, String.class);
        this.typeMappings = project.getObjects().mapProperty(String.class, String.class);
        this.additionalProperties = project.getObjects().mapProperty(String.class, String.class);
        this.openapiNormalizer = project.getObjects().mapProperty(String.class, String.class);
        this.saveOriginalTemplates = project.getObjects().property(Boolean.class);
        this.saveOriginalTemplates.convention(false);  // Default to false to avoid clutter
    }
    
    // Getter methods
    /**
     * Gets the output directory property for generated code.
     *
     * <p>This property defines the base directory where all generated OpenAPI model classes
     * will be written. The path is resolved relative to the project root directory.</p>
     *
     * <p>Default value: {@code "build/generated/sources/openapi"}</p>
     *
     * @return the output directory property containing the path where generated code will be placed
     */
    public Property<String> getOutputDir() {
        return outputDir;
    }
    
    /**
     * Gets the user template directory property.
     *
     * <p>This property specifies the source directory containing user-provided custom Mustache templates.
     * These templates are copied to the build/template-work directory during processing and take
     * precedence over plugin defaults and OpenAPI Generator templates.</p>
     *
     * <p>Templates should be organized by generator name (e.g., {@code templates/spring/pojo.mustache}).</p>
     *
     * <p>Default value: No default - templates are optional</p>
     *
     * @return the user template directory property containing the path to custom template source files
     */
    public Property<String> getUserTemplateDir() {
        return userTemplateDir;
    }
    
    /**
     * Gets the user template customizations directory property.
     *
     * <p>This property specifies the source directory containing user-provided YAML template
     * customization files. These YAML files define surgical modifications (insertions, replacements)
     * that are applied to templates in the build/template-work directory during processing.</p>
     *
     * <p>Customization files should be organized by generator name (e.g., {@code customizations/spring/pojo.mustache.yaml})
     * and allow template modifications without maintaining full template copies.</p>
     *
     * <p>Default value: No default - customizations are optional</p>
     *
     * @return the user template customizations directory property containing the path to YAML customization source files
     */
    public Property<String> getUserTemplateCustomizationsDir() {
        return userTemplateCustomizationsDir;
    }
    
    /**
     * Gets the OpenAPI generator name property.
     *
     * <p>This property specifies which OpenAPI Generator to use for code generation. The generator
     * determines the output language, framework, and structure of the generated model classes.</p>
     *
     * <p>Common generator options include:
     * <ul>
     *   <li><strong>spring:</strong> Spring Boot with Jakarta EE and Jackson annotations</li>
     *   <li><strong>java:</strong> Plain Java classes with configurable libraries</li>
     *   <li><strong>kotlin:</strong> Kotlin data classes</li>
     *   <li><strong>typescript-node:</strong> TypeScript interfaces for Node.js</li>
     * </ul>
     * </p>
     *
     * <p>Default value: {@code "spring"}</p>
     *
     * @return the generator property containing the OpenAPI generator name to use
     * @see <a href="https://openapi-generator.tech/docs/generators">OpenAPI Generator Documentation</a>
     */
    public Property<String> getGenerator() {
        return generator;
    }

    /**
     * Gets the model name prefix property.
     *
     * <p>This property specifies a string to prepend to all generated model class names.
     * Useful for avoiding naming conflicts and establishing naming conventions.</p>
     *
     * <p>Example: With prefix {@code "Api"}, a model named {@code Pet} becomes {@code ApiPet}</p>
     *
     * <p>Default value: No prefix</p>
     *
     * @return the model name prefix property containing the string to prepend to model class names
     */
    public Property<String> getModelNamePrefix() {
        return modelNamePrefix;
    }
    
    /**
     * Gets the model name suffix property.
     *
     * <p>This property specifies a string to append to all generated model class names.
     * Commonly used to distinguish generated classes (e.g., DTOs) from domain objects.</p>
     *
     * <p>Example: With suffix {@code "Dto"}, a model named {@code Pet} becomes {@code PetDto}</p>
     *
     * <p>Default value: {@code "Dto"}</p>
     *
     * @return the model name suffix property containing the string to append to model class names
     */
    public Property<String> getModelNameSuffix() {
        return modelNameSuffix;
    }
    
    /**
     * Gets the generate model tests property.
     *
     * <p>This property controls whether unit tests are generated for model classes.
     * When enabled, creates test classes that validate model construction, serialization,
     * and validation logic.</p>
     *
     * <p>Default value: {@code false}</p>
     *
     * @return the generate model tests property controlling unit test generation for models
     */
    public Property<Boolean> getGenerateModelTests() {
        return generateModelTests;
    }
    
    /**
     * Gets the generate API tests property.
     *
     * <p>This property controls whether unit tests are generated for API classes.
     * When enabled, creates test classes for API endpoints and controller logic.</p>
     *
     * <p>Default value: {@code false}</p>
     *
     * @return the generate API tests property controlling unit test generation for APIs
     */
    public Property<Boolean> getGenerateApiTests() {
        return generateApiTests;
    }
    
    /**
     * Gets the generate API documentation property.
     *
     * <p>This property controls whether API documentation is generated from the OpenAPI specification.
     * When enabled, creates documentation files in various formats (HTML, Markdown, etc.)
     * describing the API endpoints, request/response schemas, and usage examples.</p>
     *
     * <p>Default value: {@code false}</p>
     *
     * @return the generate API documentation property controlling API documentation generation
     */
    public Property<Boolean> getGenerateApiDocumentation() {
        return generateApiDocumentation;
    }
    
    /**
     * Gets the generate model documentation property.
     *
     * <p>This property controls whether model documentation is generated from the OpenAPI specification.
     * When enabled, creates documentation files describing the data models, their properties,
     * validation rules, and relationships.</p>
     *
     * <p>Default value: {@code false}</p>
     *
     * @return the generate model documentation property controlling model documentation generation
     */
    public Property<Boolean> getGenerateModelDocumentation() {
        return generateModelDocumentation;
    }
    
    /**
     * Gets the validate spec property.
     *
     * <p>This property controls whether the OpenAPI specification is validated before code generation.
     * When enabled, checks for structural errors, missing required fields, and spec compliance
     * with OpenAPI standards. Validation failures will cause the build to fail.</p>
     *
     * <p>Default value: {@code false}</p>
     *
     * @return the validate spec property controlling OpenAPI specification validation
     */
    public Property<Boolean> getValidateSpec() {
        return validateSpec;
    }
    
    /**
     * Gets the config options property.
     *
     * <p>This property contains OpenAPI Generator configuration options that control
     * code generation behavior. Options are generator-specific and include settings
     * for annotations, validation, serialization, and language-specific features.</p>
     *
     * <p>Common options include:
     * <ul>
     *   <li>{@code useSpringBoot3}: Enable Spring Boot 3 compatibility</li>
     *   <li>{@code useBeanValidation}: Add Jakarta validation annotations</li>
     *   <li>{@code dateLibrary}: Choose date/time library (java8, legacy, etc.)</li>
     * </ul>
     * </p>
     *
     * @return the config options property containing generator-specific configuration settings
     */
    public MapProperty<String, String> getConfigOptions() {
        return configOptions;
    }
    
    /**
     * Gets the global properties.
     *
     * <p>This property contains global properties passed to the OpenAPI Generator.
     * These properties affect the overall generation process and are typically
     * used to control high-level behavior like output selection and processing modes.</p>
     *
     * <p>Common global properties include:
     * <ul>
     *   <li>{@code models}: Generate only model classes (empty string enables)</li>
     *   <li>{@code apis}: Generate only API classes</li>
     *   <li>{@code supportingFiles}: Generate supporting files</li>
     * </ul>
     * </p>
     *
     * @return the global properties controlling overall OpenAPI Generator behavior
     */
    public MapProperty<String, String> getGlobalProperties() {
        return globalProperties;
    }
    
    /**
     * Gets the template variables property.
     *
     * <p>This property contains custom variables that are available in Mustache templates
     * during code generation. Variables support nested expansion, allowing one variable
     * to reference another (e.g., {@code copyright: "© {{currentYear}} MyCompany"}).</p>
     *
     * <p>Built-in variables include:
     * <ul>
     *   <li>{@code currentYear}: Current year (e.g., "2025")</li>
     *   <li>{@code generatedBy}: Plugin identification string</li>
     *   <li>{@code pluginVersion}: Plugin version number</li>
     * </ul>
     * </p>
     *
     * @return the template variables property containing custom variables for Mustache templates
     */
    public MapProperty<String, String> getTemplateVariables() {
        return templateVariables;
    }
    
    /**
     * Gets the import mappings property.
     *
     * <p>This property maps type names to fully qualified import statements in generated code.
     * Used to ensure proper imports for custom types, external libraries, and framework classes.</p>
     *
     * <p>Example mappings:
     * <ul>
     *   <li>{@code UUID -> java.util.UUID}</li>
     *   <li>{@code LocalDate -> java.time.LocalDate}</li>
     *   <li>{@code BigDecimal -> java.math.BigDecimal}</li>
     * </ul>
     * </p>
     *
     * <p>Mappings are merged with spec-level mappings, with spec-level taking precedence.</p>
     *
     * @return the import mappings property mapping type names to import statements
     */
    public MapProperty<String, String> getImportMappings() {
        return importMappings;
    }
    
    /**
     * Gets the type mappings property.
     *
     * <p>This property maps OpenAPI schema types to target language types in generated code.
     * Allows customization of how OpenAPI types and formats are translated to Java/Kotlin/etc. types.</p>
     *
     * <p>Example mappings:
     * <ul>
     *   <li>{@code string+uuid -> UUID}</li>
     *   <li>{@code string+date -> LocalDate}</li>
     *   <li>{@code string+date-time -> LocalDateTime}</li>
     * </ul>
     * </p>
     *
     * <p>Mappings are merged with spec-level mappings, with spec-level taking precedence.</p>
     *
     * @return the type mappings property mapping OpenAPI types to target language types
     */
    public MapProperty<String, String> getTypeMappings() {
        return typeMappings;
    }
    
    /**
     * Gets the additional properties.
     *
     * <p>This property contains additional properties passed to OpenAPI Generator.
     * These properties provide fine-grained control over generator-specific features
     * and are equivalent to the {@code --additional-properties} CLI option.</p>
     *
     * <p>Common additional properties include:
     * <ul>
     *   <li>{@code library}: Library variant for the generator (e.g., 'spring-boot', 'jackson')</li>
     *   <li>{@code beanValidations}: Enable Jakarta Bean Validation annotations</li>
     *   <li>{@code reactive}: Enable reactive programming support</li>
     * </ul>
     * </p>
     *
     * <p>Properties are merged with spec-level properties, with spec-level taking precedence.</p>
     *
     * @return the additional properties for generator-specific configuration
     */
    public MapProperty<String, String> getAdditionalProperties() {
        return additionalProperties;
    }
    
    /**
     * Gets the OpenAPI normalizer property.
     *
     * <p>This property contains OpenAPI normalizer rules that transform input specifications
     * before code generation. Normalizer rules can simplify complex schemas, refactor
     * inheritance patterns, and optimize the specification structure.</p>
     *
     * <p>Common normalizer rules include:
     * <ul>
     *   <li>{@code REFACTOR_ALLOF_WITH_PROPERTIES_ONLY -> true}: Simplify allOf schemas</li>
     *   <li>{@code SIMPLIFY_ONEOF_ANYOF -> true}: Simplify oneOf/anyOf schemas</li>
     *   <li>{@code KEEP_ONLY_FIRST_TAG_IN_OPERATION -> true}: Use only first tag per operation</li>
     * </ul>
     * </p>
     *
     * <p>Rules are merged with spec-level rules, with spec-level taking precedence.</p>
     *
     * @return the OpenAPI normalizer property containing transformation rules
     */
    public MapProperty<String, String> getOpenapiNormalizer() {
        return openapiNormalizer;
    }
    
    /**
     * Gets the save original templates property.
     *
     * <p>This property controls whether original OpenAPI Generator templates are preserved
     * in the {@code build/template-work/{generator}-{specName}/orig/} directory before
     * applying customizations. Useful for debugging template customizations and creating
     * new custom templates based on the originals.</p>
     *
     * <p>When enabled, templates are extracted and saved during the template preparation phase,
     * allowing comparison between original and customized versions.</p>
     *
     * <p>Default value: {@code false} to avoid cluttering the build directory</p>
     *
     * @return the save original templates property controlling template preservation
     */
    public Property<Boolean> getSaveOriginalTemplates() {
        return saveOriginalTemplates;
    }
    
    
    /**
     * Gets the template sources list property.
     * 
     * <p>Defines the ordered list of template sources to use for template resolution.
     * Sources are processed in order from highest to lowest priority. Available sources:
     * <ul>
     *   <li><strong>user-templates:</strong> Explicit .mustache files in userTemplateDir</li>
     *   <li><strong>user-customizations:</strong> YAML customizations in userTemplateCustomizationsDir</li>
     *   <li><strong>library-templates:</strong> Templates from JAR dependencies</li>
     *   <li><strong>library-customizations:</strong> YAML customizations from JAR dependencies</li>
     *   <li><strong>plugin-customizations:</strong> Built-in plugin YAML customizations</li>
     *   <li><strong>openapi-generator:</strong> OpenAPI Generator default templates (always available)</li>
     * </ul>
     * 
     * <p>The plugin automatically discovers which sources are available and only processes
     * those that exist. Non-existent sources are silently skipped.</p>
     * 
     * @return the template sources list property
     */
    public ListProperty<String> getTemplateSources() {
        return templateSources;
    }
    
    /**
    
    
    // Convenience setter methods for Gradle DSL
    /**
     * Sets the output directory for generated code.
     *
     * <p>Specifies the base directory where all generated OpenAPI model classes will be written.
     * The path is resolved relative to the project root directory and should typically point
     * to a location within the build directory to avoid source control conflicts.</p>
     *
     * <p>The plugin will create the necessary subdirectory structure within this directory
     * to organize generated code by package and generator type.</p>
     *
     * @param value the output directory path relative to project root (e.g., "build/generated/sources/openapi")
     */
    @Option(option = "output-dir", description = "Directory where generated code will be written (relative to project root)")
    public void outputDir(String value) {
        this.outputDir.set(value);
    }
    
    /**
     * Sets the directory containing user's custom Mustache templates.
     * These templates are copied to build/template-work/{generator} during processing,
     * where they override OpenAPI Generator defaults. The actual generation uses the
     * orchestrated template-work directory, not this source directory directly.
     * 
     * @param value Path to the directory containing custom templates organized by generator
     */
    @Option(option = "template-dir", description = "Source directory for user's custom Mustache templates (copied to build/template-work)")
    public void userTemplateDir(String value) {
        this.userTemplateDir.set(value);
    }
    
    /**
     * Sets the directory containing user's YAML template customization files.
     * These YAML files define surgical modifications to templates (insertions, replacements)
     * that are applied to the templates in build/template-work/{generator} during processing.
     * This allows template customization without maintaining full template copies.
     * 
     * @param value Path to the directory containing YAML customizations organized by generator
     */
    @Option(option = "template-customizations-dir", description = "Source directory for user's YAML template customizations (applied to build/template-work)")
    public void userTemplateCustomizationsDir(String value) {
        this.userTemplateCustomizationsDir.set(value);
    }
    
    /**
     * Sets the OpenAPI generator name.
     *
     * <p>Specifies which OpenAPI Generator to use for code generation. The generator determines
     * the target language, framework, annotations, and overall structure of the generated code.</p>
     *
     * <p>Popular generator options include:
     * <ul>
     *   <li><strong>spring:</strong> Spring Boot with Jakarta EE annotations and Jackson support</li>
     *   <li><strong>java:</strong> Plain Java with configurable library support (Jackson, native, etc.)</li>
     *   <li><strong>kotlin:</strong> Kotlin data classes with null safety and modern syntax</li>
     *   <li><strong>typescript-node:</strong> TypeScript interfaces for Node.js applications</li>
     * </ul>
     * </p>
     *
     * <p>Each generator supports different libraries and configuration options via additionalProperties.</p>
     *
     * @param value the generator name (e.g., 'spring', 'java', 'kotlin', 'typescript-node')
     * @see <a href="https://openapi-generator.tech/docs/generators">Complete list of available generators</a>
     */
    @Option(option = "generator", description = "OpenAPI generator name (e.g., 'spring', 'java', 'kotlin', 'typescript-node')")
    public void generator(String value) {
        this.generator.set(value);
    }

    /**
     * Sets the model name prefix for all generated classes.
     *
     * <p>Specifies a string to prepend to all generated model class names. This is useful for
     * avoiding naming conflicts with existing classes and establishing consistent naming conventions
     * across generated code.</p>
     *
     * <p>Example: Setting prefix to {@code "Api"} transforms:
     * <ul>
     *   <li>{@code Pet} → {@code ApiPet}</li>
     *   <li>{@code Category} → {@code ApiCategory}</li>
     *   <li>{@code Order} → {@code ApiOrder}</li>
     * </ul>
     * </p>
     *
     * @param value the prefix to prepend to all generated model class names (e.g., "Api", "Generated")
     */
    @Option(option = "model-name-prefix", description = "Prefix to prepend to all generated model class names (e.g., 'Api', 'Generated')")
    public void modelNamePrefix(String value) {
        this.modelNamePrefix.set(value);
    }
    
    /**
     * Sets the model name suffix for all generated classes.
     *
     * <p>Specifies a string to append to all generated model class names. Commonly used to
     * distinguish generated classes from domain objects and establish clear naming patterns.</p>
     *
     * <p>Example: Setting suffix to {@code "Dto"} transforms:
     * <ul>
     *   <li>{@code Pet} → {@code PetDto}</li>
     *   <li>{@code Category} → {@code CategoryDto}</li>
     *   <li>{@code Order} → {@code OrderDto}</li>
     * </ul>
     * </p>
     *
     * @param value the suffix to append to all generated model class names (e.g., "Dto", "Model")
     */
    @Option(option = "model-name-suffix", description = "Suffix to append to all generated model class names (e.g., 'Dto', 'Model')")
    public void modelNameSuffix(String value) {
        this.modelNameSuffix.set(value);
    }
    
    /**
     * Sets whether to generate unit tests for model classes.
     *
     * <p>When enabled, generates comprehensive unit tests for each model class that validate:
     * <ul>
     *   <li>Object construction and initialization</li>
     *   <li>Getter and setter functionality</li>
     *   <li>Serialization and deserialization (JSON/XML)</li>
     *   <li>Bean validation constraints</li>
     *   <li>Equals and hashCode methods</li>
     * </ul>
     * </p>
     *
     * <p>Test generation respects the target testing framework and follows established patterns
     * for the chosen generator and library combination.</p>
     *
     * @param value {@code true} to generate unit tests for model classes, {@code false} to skip test generation
     */
    @Option(option = "generate-model-tests", description = "Generate unit tests for model classes")
    public void generateModelTests(Boolean value) {
        this.generateModelTests.set(value);
    }
    
    /**
     * Sets whether to generate unit tests for API classes.
     *
     * <p>When enabled, generates unit tests for API endpoint classes that validate:
     * <ul>
     *   <li>HTTP method mappings and routing</li>
     *   <li>Request parameter binding and validation</li>
     *   <li>Response serialization and status codes</li>
     *   <li>Error handling and exception mapping</li>
     *   <li>Security and authentication integration</li>
     * </ul>
     * </p>
     *
     * <p>Test generation includes mock setups and follows best practices for the target framework
     * (e.g., Spring Boot Test, MockMvc for Spring generators).</p>
     *
     * @param value {@code true} to generate unit tests for API classes, {@code false} to skip test generation
     */
    @Option(option = "generate-api-tests", description = "Generate unit tests for API classes")
    public void generateApiTests(Boolean value) {
        this.generateApiTests.set(value);
    }
    
    /**
     * Sets whether to generate API documentation.
     * @param value true to generate API documentation from OpenAPI specification
     */
    @Option(option = "generate-api-docs", description = "Generate API documentation from OpenAPI specification")
    public void generateApiDocumentation(Boolean value) {
        this.generateApiDocumentation.set(value);
    }
    
    /**
     * Sets whether to generate model documentation.
     * @param value true to generate model documentation from OpenAPI specification
     */
    @Option(option = "generate-model-docs", description = "Generate model documentation from OpenAPI specification")
    public void generateModelDocumentation(Boolean value) {
        this.generateModelDocumentation.set(value);
    }
    
    /**
     * Sets whether to validate the OpenAPI specification.
     * @param value true to validate OpenAPI specification before code generation
     */
    @Option(option = "validate-spec", description = "Validate OpenAPI specification before code generation")
    public void validateSpec(Boolean value) {
        this.validateSpec.set(value);
    }
    
    /**
     * Sets OpenAPI Generator configuration options.
     * 
     * @param options map of configuration options passed to the generator
     */
    public void configOptions(Map<String, String> options) {
        this.configOptions.set(options);
    }
    
    /**
     * Sets global properties passed to the OpenAPI Generator.
     * 
     * @param properties map of global properties
     */
    public void globalProperties(Map<String, String> properties) {
        this.globalProperties.set(properties);
    }
    
    /**
     * Sets template variables available in Mustache templates.
     * Supports nested variable expansion (e.g., {{copyright}} containing {{currentYear}}).
     * 
     * @param variables map of template variables and their values
     */
    public void templateVariables(Map<String, String> variables) {
        this.templateVariables.set(variables);
    }
    
    /**
     * Sets type name to import statement mappings.
     * These mappings are merged with spec-level mappings, with spec taking precedence.
     * 
     * @param mappings map of type names to fully qualified import statements
     */
    @Option(option = "import-mappings", description = "Map type names to import statements")
    public void importMappings(Map<String, String> mappings) {
        this.importMappings.set(mappings);
    }
    
    /**
     * Sets OpenAPI type to Java type mappings.
     * These mappings are merged with spec-level mappings, with spec taking precedence.
     * 
     * @param mappings map of OpenAPI types (e.g., 'string+uuid') to Java types (e.g., 'UUID')
     */
    @Option(option = "type-mappings", description = "Map OpenAPI types to Java types")
    public void typeMappings(Map<String, String> mappings) {
        this.typeMappings.set(mappings);
    }
    
    /**
     * Sets additional properties passed to the OpenAPI Generator.
     * These properties are merged with spec-level properties, with spec taking precedence.
     * Equivalent to the --additional-properties CLI option.
     * 
     * @param properties map of additional properties for generator-specific configuration
     */
    @Option(option = "additional-properties", description = "Additional properties for OpenAPI Generator")
    public void additionalProperties(Map<String, String> properties) {
        this.additionalProperties.set(properties);
    }
    
    /**
     * Sets OpenAPI normalizer rules to transform input specifications.
     * These rules are merged with spec-level rules, with spec taking precedence.
     * Equivalent to the --openapi-normalizer CLI option.
     * 
     * @param rules map of normalizer rules and their values (e.g., 'REFACTOR_ALLOF_WITH_PROPERTIES_ONLY': 'true')
     */
    @Option(option = "openapi-normalizer", description = "OpenAPI normalizer rules to transform input specifications")
    public void openapiNormalizer(Map<String, String> rules) {
        this.openapiNormalizer.set(rules);
    }
    
    
    /**
     * Sets the template sources list for template resolution.
     * 
     * <p>Defines the ordered list of template sources to use, from highest to lowest priority.
     * The plugin will automatically discover which sources are available and skip missing ones.
     * 
     * <p>Available sources:</p>
     * <ul>
     *   <li><strong>user-templates:</strong> Explicit .mustache files</li>
     *   <li><strong>user-customizations:</strong> YAML customization files</li>
     *   <li><strong>library-templates:</strong> Templates from JAR dependencies</li>
     *   <li><strong>library-customizations:</strong> YAML customizations from JARs</li>
     *   <li><strong>plugin-customizations:</strong> Built-in plugin customizations</li>
     *   <li><strong>openapi-generator:</strong> OpenAPI Generator defaults (always available)</li>
     * </ul>
     * 
     * @param sources the ordered list of template sources to use
     */
    @Option(option = "template-sources", description = "Ordered list of template sources to use for template resolution")
    public void templateSources(java.util.List<String> sources) {
        this.templateSources.set(sources);
    }
    
    /**
    
    /**
     * Saves original OpenAPI Generator templates to a subdirectory for reference.
     * 
     * <p>When enabled, extracts and preserves the original OpenAPI Generator templates
     * to {@code build/template-work/{generator}-{specName}/orig/} before applying any
     * customizations. This is useful for:</p>
     * <ul>
     *   <li>Debugging template customizations and understanding what's being modified</li>
     *   <li>Creating new custom templates based on the originals</li>
     *   <li>Comparing customized templates with their original versions</li>
     * </ul>
     * 
     * <p>Default is {@code false} to avoid cluttering the work directory.</p>
     * 
     * @param saveOriginalTemplates {@code true} to save original templates before customization
     */
    @Option(option = "save-original-templates", description = "Save original OpenAPI Generator templates to orig/ subdirectory for reference")
    public void saveOriginalTemplates(Boolean saveOriginalTemplates) {
        this.saveOriginalTemplates.set(saveOriginalTemplates);
    }
    
}