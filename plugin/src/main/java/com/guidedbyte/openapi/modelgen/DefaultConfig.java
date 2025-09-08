package com.guidedbyte.openapi.modelgen;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.options.Option;

import java.util.Arrays;
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
 *   <li><strong>templateDir:</strong> Directory containing user's custom Mustache templates (copied to build/template-work during processing)</li>
 *   <li><strong>templateCustomizationsDir:</strong> Directory containing user's YAML template customization files (applied to build/template-work)</li>
 *   <li><strong>modelNameSuffix:</strong> Suffix appended to model class names (default: "Dto")</li>
 *   <li><strong>validateSpec:</strong> Enable/disable OpenAPI specification validation (default: false)</li>
 *   <li><strong>applyPluginCustomizations:</strong> Enable/disable built-in plugin YAML customizations (default: true)</li>
 *   <li><strong>templateSources:</strong> <em>(Preferred)</em> Ordered list of template sources with auto-discovery (default: all sources)</li>
 *   <li><strong>templatePrecedence:</strong> <em>(Advanced)</em> Configurable template resolution order for fine-grained control</li>
 *   <li><strong>debugTemplateResolution:</strong> Enable debug logging for template source resolution (default: false)</li>
 *   <li><strong>templateVariables:</strong> Variables available in Mustache templates (supports nested expansion)</li>
 *   <li><strong>configOptions:</strong> OpenAPI Generator configuration options (pre-configured for Spring Boot 3 + Jakarta EE + Lombok)</li>
 *   <li><strong>globalProperties:</strong> Global properties passed to the generator</li>
 *   <li><strong>generateModelTests:</strong> Enable/disable model unit test generation (default: false)</li>
 *   <li><strong>generateApiTests:</strong> Enable/disable API unit test generation (default: false)</li>
 *   <li><strong>generateApiDocumentation:</strong> Enable/disable API documentation generation (default: false)</li>
 *   <li><strong>generateModelDocumentation:</strong> Enable/disable model documentation generation (default: false)</li>
 * </ul>
 * 
 * <h2>Migration from Deprecated Properties:</h2>
 * <ul>
 *   <li><strong>templatePrecedence:</strong> <em>Deprecated.</em> Use {@code templateSources} instead - it serves as both source list and precedence order</li>
 *   <li><strong>useLibraryTemplates:</strong> <em>Deprecated.</em> Use {@code templateSources(["user-templates", "library-templates", "plugin-customizations", "openapi-generator"])} instead</li>
 *   <li><strong>useLibraryCustomizations:</strong> <em>Deprecated.</em> Use {@code templateSources(["user-customizations", "library-customizations", "plugin-customizations", "openapi-generator"])} instead</li>
 * </ul>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * defaults {
 *     outputDir "build/generated/sources/openapi"
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
 *     debugTemplateResolution true  // Show template source debug info
 *     
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
    private final Property<String> templateCustomizationsDir;
    private final Property<String> modelNameSuffix;
    private final Property<Boolean> generateModelTests;
    private final Property<Boolean> generateApiTests;
    private final Property<Boolean> generateApiDocumentation;
    private final Property<Boolean> generateModelDocumentation;
    private final Property<Boolean> validateSpec;
    private final Property<Boolean> applyPluginCustomizations;
    private final ListProperty<String> templatePrecedence;
    private final ListProperty<String> templateSources;
    private final Property<Boolean> debugTemplateResolution;
    private final MapProperty<String, String> configOptions;
    private final MapProperty<String, String> globalProperties;
    private final MapProperty<String, String> templateVariables;
    private final Property<Boolean> useLibraryTemplates;
    private final Property<Boolean> useLibraryCustomizations;
    
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
        this.templateCustomizationsDir = project.getObjects().property(String.class);
        this.modelNameSuffix = project.getObjects().property(String.class);
        this.generateModelTests = project.getObjects().property(Boolean.class);
        this.generateApiTests = project.getObjects().property(Boolean.class);
        this.generateApiDocumentation = project.getObjects().property(Boolean.class);
        this.generateModelDocumentation = project.getObjects().property(Boolean.class);
        this.validateSpec = project.getObjects().property(Boolean.class);
        this.applyPluginCustomizations = project.getObjects().property(Boolean.class);
        this.templatePrecedence = project.getObjects().listProperty(String.class);
        this.templateSources = project.getObjects().listProperty(String.class);
        this.debugTemplateResolution = project.getObjects().property(Boolean.class);
        
        // Set smart defaults for templateSources - all possible sources with auto-discovery
        this.templateSources.convention(Arrays.asList(
            "user-templates",           // Explicit .mustache files in templateDir
            "user-customizations",      // YAML customizations in templateCustomizationsDir
            "library-templates",        // Templates from JAR dependencies
            "library-customizations",   // YAML customizations from JAR dependencies
            "plugin-customizations",    // Built-in plugin YAML customizations
            "openapi-generator"         // OpenAPI Generator default templates (always available)
        ));
        this.configOptions = project.getObjects().mapProperty(String.class, String.class);
        this.globalProperties = project.getObjects().mapProperty(String.class, String.class);
        this.templateVariables = project.getObjects().mapProperty(String.class, String.class);
        this.useLibraryTemplates = project.getObjects().property(Boolean.class);
        this.useLibraryCustomizations = project.getObjects().property(Boolean.class);
    }
    
    // Getter methods
    public Property<String> getOutputDir() {
        return outputDir;
    }
    
    public Property<String> getTemplateDir() {
        return templateDir;
    }
    
    public Property<String> getTemplateCustomizationsDir() {
        return templateCustomizationsDir;
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
    
    public Property<Boolean> getApplyPluginCustomizations() {
        return applyPluginCustomizations;
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
    
    /**
     * @deprecated Use {@link #getTemplateSources()} instead. The templateSources list serves as both 
     *             source specification and precedence order, making this property redundant.
     */
    @Deprecated
    public ListProperty<String> getTemplatePrecedence() {
        return templatePrecedence;
    }
    
    /**
     * Gets the template sources list property.
     * 
     * <p>Defines the ordered list of template sources to use for template resolution.
     * Sources are processed in order from highest to lowest priority. Available sources:
     * <ul>
     *   <li><strong>user-templates:</strong> Explicit .mustache files in templateDir</li>
     *   <li><strong>user-customizations:</strong> YAML customizations in templateCustomizationsDir</li>
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
    
    public Property<Boolean> getDebugTemplateResolution() {
        return debugTemplateResolution;
    }
    
    /**
     * @deprecated Use {@link #getTemplateSources()} instead. Check if "library-templates" is in the templateSources list.
     */
    @Deprecated
    public Property<Boolean> getUseLibraryTemplates() {
        return useLibraryTemplates;
    }
    
    /**
     * @deprecated Use {@link #getTemplateSources()} instead. Check if "library-customizations" is in the templateSources list.
     */
    @Deprecated
    public Property<Boolean> getUseLibraryCustomizations() {
        return useLibraryCustomizations;
    }
    
    // Convenience setter methods for Gradle DSL
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
    public void templateDir(String value) {
        this.templateDir.set(value);
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
    public void templateCustomizationsDir(String value) {
        this.templateCustomizationsDir.set(value);
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
    
    @Option(option = "apply-plugin-customizations", description = "Apply built-in plugin YAML customizations to enhance code readability")
    public void applyPluginCustomizations(Boolean value) {
        this.applyPluginCustomizations.set(value);
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
    
    /**
     * Sets the template precedence order for template resolution.
     * 
     * <p>Valid precedence values:</p>
     * <ul>
     *   <li>{@code user-templates} - Project-specific Mustache templates (highest precedence)</li>
     *   <li>{@code user-customizations} - Project-specific YAML customizations</li>
     *   <li>{@code plugin-customizations} - Built-in plugin YAML customizations</li>
     *   <li>{@code openapi-generator} - OpenAPI Generator default templates (lowest precedence)</li>
     * </ul>
     * 
     * <p>Default order: {@code ['user-templates', 'user-customizations', 'plugin-customizations', 'openapi-generator']}</p>
     * 
     * @param precedence ordered list of template sources from highest to lowest precedence
     * @deprecated Use {@link #templateSources(java.util.List)} instead. The templateSources list serves as both 
     *             source specification and precedence order. Simply reorder the templateSources list to change precedence.
     *             Example: {@code templateSources(["user-customizations", "user-templates", "plugin-customizations", "openapi-generator"])}
     */
    @Deprecated
    @Option(option = "template-precedence", description = "Template resolution precedence order (deprecated: use templateSources instead)")
    public void templatePrecedence(java.util.List<String> precedence) {
        this.templatePrecedence.set(precedence);
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
     * Enables debug logging to show which template source was used for each template.
     * 
     * <p>When enabled, logs will show messages like:</p>
     * <pre>
     * DEBUG: Template 'pojo.mustache' resolved from: user-templates
     * DEBUG: Template 'model.mustache' resolved from: plugin-customizations
     * </pre>
     * 
     * @param debug {@code true} to enable debug template resolution logging
     */
    @Option(option = "debug-template-resolution", description = "Enable debug logging for template source resolution")
    public void debugTemplateResolution(Boolean debug) {
        this.debugTemplateResolution.set(debug);
    }
    
    /**
     * Enables/disables the use of template libraries from the openapiCustomizations configuration.
     * 
     * <p>When enabled, templates from library JARs will be extracted and used according to
     * the configured template precedence order.</p>
     * 
     * @param use {@code true} to enable library templates, {@code false} to disable
     * @deprecated Use {@link #templateSources(java.util.List)} instead. Include "library-templates" 
     *             in your templateSources list to enable library templates with better control over precedence.
     *             Example: {@code templateSources(["user-templates", "library-templates", "plugin-customizations", "openapi-generator"])}
     */
    @Deprecated
    @Option(option = "use-library-templates", description = "Enable templates from library dependencies (deprecated: use templateSources instead)")
    public void useLibraryTemplates(Boolean use) {
        this.useLibraryTemplates.set(use);
    }
    
    /**
     * Enables/disables the use of YAML customizations from template libraries.
     * 
     * <p>When enabled, YAML customization files from library JARs will be extracted and
     * applied according to the configured precedence order.</p>
     * 
     * @param use {@code true} to enable library customizations, {@code false} to disable
     * @deprecated Use {@link #templateSources(java.util.List)} instead. Include "library-customizations" 
     *             in your templateSources list to enable library customizations with better control over precedence.
     *             Example: {@code templateSources(["user-customizations", "library-customizations", "plugin-customizations", "openapi-generator"])}
     */
    @Deprecated
    @Option(option = "use-library-customizations", description = "Enable YAML customizations from library dependencies (deprecated: use templateSources instead)")
    public void useLibraryCustomizations(Boolean use) {
        this.useLibraryCustomizations.set(use);
    }
}