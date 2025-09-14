package com.guidedbyte.openapi.modelgen.services;

import org.apache.commons.lang3.StringUtils;

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
    
    /**
     * Constructs an EvaluationContext.
     * @param generatorVersion the OpenAPI Generator version
     * @param templateContent the template content being processed
     * @param projectProperties the project properties
     * @param environmentVariables the environment variables
     */
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
     * Gets the OpenAPI Generator version being used.
     * @return the generator version
     */
    public String getGeneratorVersion() {
        return generatorVersion;
    }
    
    /**
     * Gets the template content being processed.
     * @return the template content
     */
    public String getTemplateContent() {
        return templateContent;
    }
    
    /**
     * Gets project properties (from gradle.properties, command line, etc.).
     * @return the project properties
     */
    public Map<String, String> getProjectProperties() {
        return projectProperties;
    }
    
    /**
     * Gets environment variables.
     * @return the environment variables
     */
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }
    
    /**
     * Checks if the template supports a specific feature (with caching).
     * @param featureName the feature name to check
     * @return true if the feature is supported
     */
    public boolean hasFeature(String featureName) {
        return featureCache.computeIfAbsent(featureName, name -> 
            featureDetector.hasFeature(templateContent, name)
        );
    }
    
    /**
     * Checks if the template contains a specific pattern (with caching).
     * @param pattern the pattern to check
     * @return true if the template contains the pattern
     */
    public boolean templateContains(String pattern) {
        return templateContentCache.computeIfAbsent(pattern, p -> 
            templateContent != null && templateContent.contains(p)
        );
    }
    
    /**
     * Checks if the template does NOT contain a specific pattern.
     * @param pattern the pattern to check
     * @return true if the template does not contain the pattern
     */
    public boolean templateNotContains(String pattern) {
        return !templateContains(pattern);
    }
    
    /**
     * Checks if a project property is set with an optional value.
     * @param propertySpec the property specification to check
     * @return true if the project property is set
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
            // Property existence: "enableFeature" (case-insensitive false check)
            return projectProperties.containsKey(propertySpec.trim()) && 
                   !StringUtils.equalsIgnoreCase(projectProperties.get(propertySpec.trim()), "false");
        }
    }
    
    /**
     * Checks if an environment variable is set with an optional value.
     * @param variableSpec the variable specification to check
     * @return true if the environment variable is set
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
            // Variable existence: "NODE_ENV" (case-insensitive false check)
            String value = environmentVariables.get(variableSpec.trim());
            return value != null && !value.isEmpty() && !StringUtils.equalsIgnoreCase(value, "false");
        }
    }
    
    /**
     * Gets a project property value.
     * @param key the property key
     * @return the property value
     */
    public String getProjectProperty(String key) {
        return projectProperties.get(key);
    }
    
    /**
     * Gets an environment variable value.
     * @param key the variable key
     * @return the variable value
     */
    public String getEnvironmentVariable(String key) {
        return environmentVariables.get(key);
    }
    
    /**
     * Builder for creating evaluation contexts.
     */
    public static class Builder {
        /**
         * Constructs a new Builder.
         */
        public Builder() {
            // Default constructor
        }
        private String generatorVersion;
        private String templateContent;
        private Map<String, String> projectProperties;
        private Map<String, String> environmentVariables;
        
        /**
         * Sets the generator version.
         * @param version the generator version
         * @return this builder
         */
        public Builder generatorVersion(String version) {
            this.generatorVersion = version;
            return this;
        }
        
        /**
         * Sets the template content.
         * @param content the template content
         * @return this builder
         */
        public Builder templateContent(String content) {
            this.templateContent = content;
            return this;
        }
        
        /**
         * Sets the project properties.
         * @param properties the project properties
         * @return this builder
         */
        public Builder projectProperties(Map<String, String> properties) {
            this.projectProperties = properties;
            return this;
        }
        
        /**
         * Sets the environment variables.
         * @param variables the environment variables
         * @return this builder
         */
        public Builder environmentVariables(Map<String, String> variables) {
            this.environmentVariables = variables;
            return this;
        }
        
        /**
         * Builds the EvaluationContext.
         * @return the constructed EvaluationContext
         */
        public EvaluationContext build() {
            return new EvaluationContext(generatorVersion, templateContent, 
                                       projectProperties, environmentVariables);
        }
    }
    
    /**
     * Creates a new builder instance.
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}