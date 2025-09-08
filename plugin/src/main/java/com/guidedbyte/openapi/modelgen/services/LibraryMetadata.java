package com.guidedbyte.openapi.modelgen.services;

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
    
    // Default constructor for YAML parsing
    public LibraryMetadata() {}
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }
    
    public String getMinOpenApiGeneratorVersion() { return minOpenApiGeneratorVersion; }
    public void setMinOpenApiGeneratorVersion(String minOpenApiGeneratorVersion) { 
        this.minOpenApiGeneratorVersion = minOpenApiGeneratorVersion; 
    }
    
    public String getMaxOpenApiGeneratorVersion() { return maxOpenApiGeneratorVersion; }
    public void setMaxOpenApiGeneratorVersion(String maxOpenApiGeneratorVersion) { 
        this.maxOpenApiGeneratorVersion = maxOpenApiGeneratorVersion; 
    }
    
    public String getMinPluginVersion() { return minPluginVersion; }
    public void setMinPluginVersion(String minPluginVersion) { this.minPluginVersion = minPluginVersion; }
    
    public String getMaxPluginVersion() { return maxPluginVersion; }
    public void setMaxPluginVersion(String maxPluginVersion) { this.maxPluginVersion = maxPluginVersion; }
    
    public List<String> getSupportedGenerators() { return supportedGenerators; }
    public void setSupportedGenerators(List<String> supportedGenerators) { 
        this.supportedGenerators = supportedGenerators; 
    }
    
    public List<String> getRequiredFeatures() { return requiredFeatures; }
    public void setRequiredFeatures(List<String> requiredFeatures) { 
        this.requiredFeatures = requiredFeatures; 
    }
    
    public Map<String, Object> getFeatures() { return features; }
    public void setFeatures(Map<String, Object> features) { this.features = features; }
    
    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
    
    /**
     * Checks if this library supports the given generator.
     */
    public boolean supportsGenerator(String generatorName) {
        return supportedGenerators == null || supportedGenerators.isEmpty() || 
               supportedGenerators.contains(generatorName);
    }
    
    /**
     * Checks if this library has the specified feature.
     */
    public boolean hasFeature(String featureName) {
        return features != null && features.containsKey(featureName);
    }
    
    /**
     * Gets a feature value as a string.
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