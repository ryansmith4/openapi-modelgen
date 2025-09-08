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
        
        // Generic method for any spec name
        public Object methodMissing(String name, Object args) {
            if (args instanceof Object[]) {
                Object[] argsArray = (Object[]) args;
                if (argsArray.length == 1) {
                    Object arg = argsArray[0];
                    if (arg instanceof Closure) {
                        createSpec(name, (Closure<?>) arg);
                        return null;
                    } else if (arg instanceof Action) {
                        @SuppressWarnings("unchecked")
                        Action<? super SpecConfig> action = (Action<? super SpecConfig>) arg;
                        createSpec(name, action);
                        return null;
                    }
                }
            }
            throw new IllegalArgumentException("Unknown method: " + name);
        }
        
        private void createSpec(String name, Closure<?> closure) {
            SpecConfig specConfig = new SpecConfig(project);
            ConfigureUtil.configure(closure, specConfig);
            specs.put(name, specConfig);
        }
        
        private void createSpec(String name, Action<? super SpecConfig> action) {
            SpecConfig specConfig = new SpecConfig(project);
            action.execute(specConfig);
            specs.put(name, specConfig);
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