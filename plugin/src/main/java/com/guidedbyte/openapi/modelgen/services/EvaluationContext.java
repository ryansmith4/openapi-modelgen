package com.guidedbyte.openapi.modelgen.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context for evaluating customization conditions.
 * 
 * Contains all the information needed to evaluate whether a customization
 * should be applied, including generator version, template content, project
 * properties, and cached feature detection results.
 * 
 * @since 2.0.0
 */
public class EvaluationContext {
    private final String generatorVersion;
    private final String templateContent;
    private final Map<String, String> projectProperties;
    private final Map<String, String> environmentVariables;
    private final FeatureDetector featureDetector;
    
    // Lazy-loaded caches
    private final Map<String, Boolean> featureCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> templateContentCache = new ConcurrentHashMap<>();
    
    public EvaluationContext(String generatorVersion, String templateContent, 
                           Map<String, String> projectProperties, 
                           Map<String, String> environmentVariables) {
        this.generatorVersion = generatorVersion;
        this.templateContent = templateContent;
        this.projectProperties = projectProperties != null ? projectProperties : Map.of();
        this.environmentVariables = environmentVariables != null ? environmentVariables : Map.of();
        this.featureDetector = new FeatureDetector();
    }
    
    /**
     * Gets the OpenAPI Generator version being used
     */
    public String getGeneratorVersion() {
        return generatorVersion;
    }
    
    /**
     * Gets the template content being processed
     */
    public String getTemplateContent() {
        return templateContent;
    }
    
    /**
     * Gets project properties (from gradle.properties, command line, etc.)
     */
    public Map<String, String> getProjectProperties() {
        return projectProperties;
    }
    
    /**
     * Gets environment variables
     */
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }
    
    /**
     * Checks if the template supports a specific feature (with caching)
     */
    public boolean hasFeature(String featureName) {
        return featureCache.computeIfAbsent(featureName, name -> 
            featureDetector.hasFeature(templateContent, name)
        );
    }
    
    /**
     * Checks if the template contains a specific pattern (with caching)
     */
    public boolean templateContains(String pattern) {
        return templateContentCache.computeIfAbsent(pattern, p -> 
            templateContent != null && templateContent.contains(p)
        );
    }
    
    /**
     * Checks if the template does NOT contain a specific pattern
     */
    public boolean templateNotContains(String pattern) {
        return !templateContains(pattern);
    }
    
    /**
     * Checks if a project property is set with an optional value
     */
    public boolean hasProjectProperty(String propertySpec) {
        if (propertySpec == null) {
            return false;
        }
        
        if (propertySpec.contains("=")) {
            // Property with value: "enableFeature=true"
            String[] parts = propertySpec.split("=", 2);
            String key = parts[0].trim();
            String expectedValue = parts[1].trim();
            String actualValue = projectProperties.get(key);
            return expectedValue.equals(actualValue);
        } else {
            // Property existence: "enableFeature"
            return projectProperties.containsKey(propertySpec.trim()) && 
                   !"false".equals(projectProperties.get(propertySpec.trim()));
        }
    }
    
    /**
     * Checks if an environment variable is set with an optional value
     */
    public boolean hasEnvironmentVariable(String variableSpec) {
        if (variableSpec == null) {
            return false;
        }
        
        if (variableSpec.contains("=")) {
            // Variable with value: "NODE_ENV=production"
            String[] parts = variableSpec.split("=", 2);
            String key = parts[0].trim();
            String expectedValue = parts[1].trim();
            String actualValue = environmentVariables.get(key);
            return expectedValue.equals(actualValue);
        } else {
            // Variable existence: "NODE_ENV"
            String value = environmentVariables.get(variableSpec.trim());
            return value != null && !value.isEmpty() && !"false".equals(value);
        }
    }
    
    /**
     * Gets a project property value
     */
    public String getProjectProperty(String key) {
        return projectProperties.get(key);
    }
    
    /**
     * Gets an environment variable value
     */
    public String getEnvironmentVariable(String key) {
        return environmentVariables.get(key);
    }
    
    /**
     * Builder for creating evaluation contexts
     */
    public static class Builder {
        private String generatorVersion;
        private String templateContent;
        private Map<String, String> projectProperties;
        private Map<String, String> environmentVariables;
        
        public Builder generatorVersion(String version) {
            this.generatorVersion = version;
            return this;
        }
        
        public Builder templateContent(String content) {
            this.templateContent = content;
            return this;
        }
        
        public Builder projectProperties(Map<String, String> properties) {
            this.projectProperties = properties;
            return this;
        }
        
        public Builder environmentVariables(Map<String, String> variables) {
            this.environmentVariables = variables;
            return this;
        }
        
        public EvaluationContext build() {
            return new EvaluationContext(generatorVersion, templateContent, 
                                       projectProperties, environmentVariables);
        }
    }
    
    /**
     * Creates a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}