package com.guidedbyte.openapi.modelgen;

import com.guidedbyte.openapi.modelgen.services.LibraryTemplateExtractor;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.util.internal.ConfigureUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Gradle DSL extension for configuring the OpenAPI Model Generator plugin.
 * 
 * <p>This extension provides the 'openapiModelgen' configuration block that allows users to:</p>
 * <ul>
 *   <li>Configure default settings that apply to all specifications</li>
 *   <li>Define multiple OpenAPI specifications with individual settings</li>
 *   <li>Override defaults on a per-specification basis</li>
 *   <li>Set up template variables and custom template directories</li>
 * </ul>
 * 
 * <h2>Configuration Structure:</h2>
 * <pre>{@code
 * openapiModelgen {
 *     // Global plugin settings
 *     parallel true  // Enable parallel spec processing (default: true)
 *     debug true     // Enable debug logging for template resolution (default: false)
 *     
 *     defaults {
 *         // Global defaults applied to all specs
 *         validateSpec true
 *         modelNameSuffix "Dto"
 *         outputDir "build/generated/sources/openapi"
 *         templateVariables([...])
 *     }
 *     specs {
 *         specName {
 *             // Required settings
 *             inputSpec "path/to/spec.yaml"
 *             modelPackage "com.example.model"
 *             
 *             // Optional overrides
 *             validateSpec false  // Override default
 *             templateDir "custom/templates"
 *             modelNameSuffix "Model"
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Gradle plugin extension class intentionally exposes mutable objects including Project and nested configuration containers. " +
                   "This is the standard pattern for Gradle extensions where the extension acts as a configuration DSL entry point. " +
                   "The Project reference and configuration containers must be mutable for the plugin's configuration phase to function properly."
)
public class OpenApiModelGenExtension {
    
    private final DefaultConfig defaults;
    private final Map<String, SpecConfig> specs = new HashMap<>();
    private final Project project;
    
    /**
     * Whether to enable parallel processing of multiple specifications.
     * Default is true for better performance on multi-core systems.
     */
    private boolean parallel = true;

    
    
    /**
     * Library content extracted from template library dependencies.
     * Set during plugin configuration phase for use by tasks.
     */
    private LibraryTemplateExtractor.LibraryExtractionResult libraryContent;
    
    /**
     * Creates a new OpenAPI Model Generator extension for the given project.
     * 
     * @param project the Gradle project this extension belongs to
     */
    public OpenApiModelGenExtension(Project project) {
        this.project = project;
        this.defaults = new DefaultConfig(project);
    }
    
    /**
     * Configures the default settings that apply to all specifications.
     * 
     * @param action the configuration action to apply to defaults
     */
    public void defaults(Action<? super DefaultConfig> action) {
        action.execute(defaults);
    }
    
    /**
     * Configures the default settings using Groovy closure syntax.
     * 
     * @param closure the Groovy closure to configure defaults
     */
    @SuppressWarnings("unused")
    public void defaults(@DelegatesTo(DefaultConfig.class) Closure<?> closure) {
        ConfigureUtil.configure(closure, defaults);
    }
    
    /**
     * Configures the OpenAPI specifications to generate DTOs from.
     * 
     * @param action the configuration action to apply to the specs container
     */
    public void specs(Action<? super SpecsContainer> action) {
        SpecsContainer container = new SpecsContainer();
        action.execute(container);
    }
    
    /**
     * Configures the OpenAPI specifications using Groovy closure syntax.
     * 
     * @param closure the Groovy closure to configure specifications
     */
    @SuppressWarnings("unused")
    public void specs(@DelegatesTo(SpecsContainer.class) Closure<?> closure) {
        SpecsContainer container = new SpecsContainer();
        ConfigureUtil.configure(closure, container);
    }
    
    /**
     * Gets the default configuration settings.
     * 
     * @return the default configuration that applies to all specs
     */
    public DefaultConfig getDefaults() {
        return defaults;
    }
    
    /**
     * Gets the map of configured OpenAPI specifications.
     * 
     * @return a map of spec name to spec configuration
     */
    public Map<String, SpecConfig> getSpecs() {
        return specs;
    }
    
    /**
     * Container for OpenAPI specification configurations.
     * 
     * <p>This class provides the DSL for defining multiple OpenAPI specifications.
     * Users can define specs using dynamic method names via Groovy's methodMissing,
     * allowing any specification name to be used.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * specs {
     *     pets {
     *         inputSpec "specs/pets.yaml"
     *         modelPackage "com.example.pets"
     *     }
     *     orders {
     *         inputSpec "specs/orders.yaml"
     *         modelPackage "com.example.orders"
     *     }
     *     myCustomApi {
     *         inputSpec "specs/custom.yaml"
     *         modelPackage "com.example.custom"
     *     }
     * }
     * }</pre>
     */
    public class SpecsContainer {

        /**
         * Groovy dynamic method dispatch for OpenAPI specification configuration.
         *
         * <p>This method enables users to define OpenAPI specifications using any valid identifier
         * as the specification name through Groovy's {@code methodMissing} feature. When a user
         * calls a method that doesn't exist on this class, Groovy automatically invokes this
         * method, allowing for flexible, declarative specification naming.</p>
         *
         * <p><strong>How it works:</strong></p>
         * <ol>
         *   <li>User writes: {@code pets { inputSpec "pets.yaml" }}</li>
         *   <li>Groovy looks for a {@code pets()} method on SpecsContainer</li>
         *   <li>Method not found, so Groovy calls {@code methodMissing("pets", [closure])}</li>
         *   <li>This method creates a new {@link SpecConfig} named "pets"</li>
         *   <li>The closure is applied to configure the specification</li>
         *   <li>The configured spec is stored in the parent's specs map</li>
         * </ol>
         *
         * <p><strong>Supported syntax patterns:</strong></p>
         * <pre>{@code
         * specs {
         *     // Groovy closure syntax (most common)
         *     pets {
         *         inputSpec "specs/pets.yaml"
         *         modelPackage "com.example.pets"
         *     }
         *
         *     // Kotlin/Java Action syntax
         *     orders(Action<SpecConfig> { spec ->
         *         spec.inputSpec("specs/orders.yaml")
         *         spec.modelPackage("com.example.orders")
         *     })
         * }
         * }</pre>
         *
         * <p><strong>Naming flexibility:</strong></p>
         * <p>Any valid Java/Groovy identifier can be used as a specification name:</p>
         * <ul>
         *   <li>{@code userApi}, {@code productCatalog}, {@code authService}</li>
         *   <li>{@code api_v1}, {@code legacy_system}, {@code new_features}</li>
         *   <li>{@code microserviceA}, {@code clientSDK}, {@code internalTools}</li>
         * </ul>
         *
         * @param name the specification name (method name that was called)
         * @param args the arguments passed to the method call (expected to be a single closure or action)
         * @return always {@code null} - this method performs configuration as a side effect and doesn't return meaningful values
         * @throws IllegalArgumentException if the method call doesn't match expected patterns
         *
         * @see <a href="https://groovy-lang.org/metaprogramming.html#_methodmissing">Groovy methodMissing Documentation</a>
         */
        public Void methodMissing(String name, Object args) {
            // Groovy passes method arguments as an Object array
            if (args instanceof Object[] argsArray) {
                // We expect exactly one argument: either a Closure or an Action
                if (argsArray.length == 1) {
                    Object arg = argsArray[0];

                    // Handle Groovy closure syntax: specName { ... }
                    if (arg instanceof Closure) {
                        createSpec(name, (Closure<?>) arg);
                        return null; // methodMissing must return a value, null indicates success
                    }
                    // Handle Kotlin/Java Action syntax: specName(Action<SpecConfig> { ... })
                    else if (arg instanceof Action) {
                        @SuppressWarnings("unchecked")
                        Action<? super SpecConfig> action = (Action<? super SpecConfig>) arg;
                        createSpec(name, action);
                        return null; // methodMissing must return a value, null indicates success
                    }
                    // Unknown argument type - this shouldn't happen in normal usage
                    else {
                        throw new IllegalArgumentException(
                            String.format("Unsupported argument type for spec '%s': %s. Expected Closure or Action.",
                                name, arg.getClass().getSimpleName()));
                    }
                }
                // Wrong number of arguments - user likely made a syntax error
                else {
                    throw new IllegalArgumentException(
                        String.format("Invalid method call for spec '%s': expected 1 argument (closure or action), got %d",
                            name, argsArray.length));
                }
            }
            // This shouldn't happen - Groovy always passes args as Object[]
            else {
                throw new IllegalArgumentException(
                    String.format("Unexpected argument structure for method '%s': %s",
                        name, args != null ? args.getClass().getSimpleName() : "null"));
            }
        }
        
        /**
         * Creates and configures a new OpenAPI specification using Groovy closure syntax.
         *
         * <p>This method is called by {@link #methodMissing(String, Object)} when a user
         * defines a specification using Groovy closure syntax like:</p>
         * <pre>{@code
         * specName {
         *     inputSpec "path/to/spec.yaml"
         *     modelPackage "com.example.model"
         *     // ... other configuration
         * }
         * }</pre>
         *
         * @param name the name of the specification (becomes task name and identifier)
         * @param closure the Groovy closure containing the specification configuration
         */
        private void createSpec(String name, Closure<?> closure) {
            SpecConfig specConfig = new SpecConfig(project);
            ConfigureUtil.configure(closure, specConfig); // Apply Groovy closure to SpecConfig
            specs.put(name, specConfig); // Store in parent extension's specs map
        }

        /**
         * Creates and configures a new OpenAPI specification using Gradle Action syntax.
         *
         * <p>This method is called by {@link #methodMissing(String, Object)} when a user
         * defines a specification using Kotlin/Java Action syntax like:</p>
         * <pre>{@code
         * specName(Action<SpecConfig> { spec ->
         *     spec.inputSpec("path/to/spec.yaml")
         *     spec.modelPackage("com.example.model")
         *     // ... other configuration
         * })
         * }</pre>
         *
         * @param name the name of the specification (becomes task name and identifier)
         * @param action the Gradle Action containing the specification configuration
         */
        private void createSpec(String name, Action<? super SpecConfig> action) {
            SpecConfig specConfig = new SpecConfig(project);
            action.execute(specConfig); // Apply Action to SpecConfig
            specs.put(name, specConfig); // Store in parent extension's specs map
        }
    }
    
    /**
     * Returns whether parallel processing of multiple specifications is enabled.
     * 
     * @return true if parallel processing is enabled, false otherwise
     */
    public boolean isParallel() {
        return parallel;
    }
    
    /**
     * Sets whether to enable parallel processing of multiple specifications.
     * When enabled, multiple OpenAPI specifications will be processed concurrently
     * for better performance on multi-core systems.
     * 
     * <p><strong>Thread Safety:</strong> The plugin ensures thread safety when parallel processing 
     * is enabled by using proper synchronization mechanisms.</p>
     * 
     * @param parallel true to enable parallel processing, false to process sequentially
     */
    public void parallel(boolean parallel) {
        this.parallel = parallel;
    }
    
    /**
     * Enables parallel processing of multiple specifications.
     * Equivalent to {@code parallel(true)}.
     */
    public void parallel() {
        this.parallel = true;
    }
    
    
    

    /**
     * Gets the extracted library content from template library dependencies.
     * 
     * @return the library content, or null if no libraries were processed
     */
    public LibraryTemplateExtractor.LibraryExtractionResult getLibraryContent() {
        return libraryContent;
    }
    
    /**
     * Sets the extracted library content from template library dependencies.
     * This is called internally by the plugin during configuration phase.
     * 
     * @param libraryContent the extracted templates and customizations from libraries
     */
    public void setLibraryContent(LibraryTemplateExtractor.LibraryExtractionResult libraryContent) {
        this.libraryContent = libraryContent;
    }
}