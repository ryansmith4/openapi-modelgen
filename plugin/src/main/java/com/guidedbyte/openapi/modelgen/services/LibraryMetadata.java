package com.guidedbyte.openapi.modelgen.services;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Represents metadata for a template library.
 * 
 * <p>This class encapsulates information about a template library including
 * version compatibility, supported generators, features, and other metadata
 * that can be used for validation and filtering.</p>
 * 
 * @since 1.2.0
 */
public class LibraryMetadata implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private String name;
    private String version;
    private String description;
    private String author;
    private String homepage;
    
    // Version compatibility
    private String minOpenApiGeneratorVersion;
    private String maxOpenApiGeneratorVersion;
    private String minPluginVersion;
    private String maxPluginVersion;
    
    // Generator support
    private List<String> supportedGenerators;
    private List<String> requiredFeatures;
    private Map<String, Object> features;
    
    // Dependencies
    private List<String> dependencies;
    
    /**
     * Default constructor for YAML parsing.
     */
    public LibraryMetadata() {}
    
    /**
     * Gets the library name.
     * @return the name
     */
    public String getName() { return name; }
    
    /**
     * Sets the library name.
     * @param name the name
     */
    public void setName(String name) { this.name = name; }
    
    /**
     * Gets the library version.
     * @return the version
     */
    public String getVersion() { return version; }
    
    /**
     * Sets the library version.
     * @param version the version
     */
    public void setVersion(String version) { this.version = version; }
    
    /**
     * Gets the library description.
     * @return the description
     */
    public String getDescription() { return description; }
    
    /**
     * Sets the library description.
     * @param description the description
     */
    public void setDescription(String description) { this.description = description; }
    
    /**
     * Gets the library author.
     * @return the author
     */
    public String getAuthor() { return author; }
    
    /**
     * Sets the library author.
     * @param author the author
     */
    public void setAuthor(String author) { this.author = author; }
    
    /**
     * Gets the homepage URL.
     * @return the homepage
     */
    public String getHomepage() { return homepage; }
    
    /**
     * Sets the homepage URL.
     * @param homepage the homepage
     */
    public void setHomepage(String homepage) { this.homepage = homepage; }
    
    /**
     * Gets the minimum OpenAPI Generator version.
     * @return the minimum OpenAPI Generator version
     */
    public String getMinOpenApiGeneratorVersion() { return minOpenApiGeneratorVersion; }
    
    /**
     * Sets the minimum OpenAPI Generator version.
     * @param minOpenApiGeneratorVersion the minimum OpenAPI Generator version
     */
    public void setMinOpenApiGeneratorVersion(String minOpenApiGeneratorVersion) { 
        this.minOpenApiGeneratorVersion = minOpenApiGeneratorVersion; 
    }
    
    /**
     * Gets the maximum OpenAPI Generator version.
     * @return the maximum OpenAPI Generator version
     */
    public String getMaxOpenApiGeneratorVersion() { return maxOpenApiGeneratorVersion; }
    
    /**
     * Sets the maximum OpenAPI Generator version.
     * @param maxOpenApiGeneratorVersion the maximum OpenAPI Generator version
     */
    public void setMaxOpenApiGeneratorVersion(String maxOpenApiGeneratorVersion) { 
        this.maxOpenApiGeneratorVersion = maxOpenApiGeneratorVersion; 
    }
    
    /**
     * Gets the minimum plugin version.
     * @return the minimum plugin version
     */
    public String getMinPluginVersion() { return minPluginVersion; }
    
    /**
     * Sets the minimum plugin version.
     * @param minPluginVersion the minimum plugin version
     */
    public void setMinPluginVersion(String minPluginVersion) { this.minPluginVersion = minPluginVersion; }
    
    /**
     * Gets the maximum plugin version.
     * @return the maximum plugin version
     */
    public String getMaxPluginVersion() { return maxPluginVersion; }
    
    /**
     * Sets the maximum plugin version.
     * @param maxPluginVersion the maximum plugin version
     */
    public void setMaxPluginVersion(String maxPluginVersion) { this.maxPluginVersion = maxPluginVersion; }
    
    /**
     * Gets the supported generators.
     * @return the supported generators
     */
    public List<String> getSupportedGenerators() { return supportedGenerators; }
    
    /**
     * Sets the supported generators.
     * @param supportedGenerators the supported generators
     */
    public void setSupportedGenerators(List<String> supportedGenerators) { 
        this.supportedGenerators = supportedGenerators; 
    }
    
    /**
     * Gets the required features.
     * @return the required features
     */
    public List<String> getRequiredFeatures() { return requiredFeatures; }
    
    /**
     * Sets the required features.
     * @param requiredFeatures the required features
     */
    public void setRequiredFeatures(List<String> requiredFeatures) { 
        this.requiredFeatures = requiredFeatures; 
    }
    
    /**
     * Gets the features map.
     * @return the features
     */
    public Map<String, Object> getFeatures() { return features; }
    
    /**
     * Sets the features map.
     * @param features the features
     */
    public void setFeatures(Map<String, Object> features) { this.features = features; }
    
    /**
     * Gets the dependencies.
     * @return the dependencies
     */
    public List<String> getDependencies() { return dependencies; }
    
    /**
     * Sets the dependencies.
     * @param dependencies the dependencies
     */
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
    
    /**
     * Checks if this library supports the given generator.
     * @param generatorName the generator name to check
     * @return true if the generator is supported
     */
    public boolean supportsGenerator(String generatorName) {
        return supportedGenerators == null || supportedGenerators.isEmpty() || 
               supportedGenerators.contains(generatorName);
    }
    
    /**
     * Checks if this library has the specified feature.
     * @param featureName the feature name to check
     * @return true if the feature is available
     */
    public boolean hasFeature(String featureName) {
        return features != null && features.containsKey(featureName);
    }
    
    /**
     * Gets a feature value as a string.
     * @param featureName the feature name
     * @return the feature value as a string
     */
    public String getFeatureValue(String featureName) {
        if (features == null) {
            return null;
        }
        Object value = features.get(featureName);
        return value != null ? value.toString() : null;
    }
    
    @Override
    public String toString() {
        return "LibraryMetadata{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", supportedGenerators=" + supportedGenerators +
                '}';
    }
}