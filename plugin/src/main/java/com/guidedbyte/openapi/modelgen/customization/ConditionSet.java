package com.guidedbyte.openapi.modelgen.customization;

import java.util.List;

/**
 * Represents conditional logic for determining when customizations apply.
 * 
 * Conditions support version constraints, template content checks, feature detection,
 * build environment checks, and complex logical operations.
 * 
 * @since 2.0.0
 */
public class ConditionSet {
    // Version conditions
    private String generatorVersion;
    
    // Template content conditions
    private String templateContains;
    private String templateNotContains;
    private List<String> templateContainsAll;
    private List<String> templateContainsAny;
    
    // Feature detection conditions
    private String hasFeature;
    private List<String> hasAllFeatures;
    private List<String> hasAnyFeatures;
    
    // Build environment conditions
    private String projectProperty;
    private String environmentVariable;
    private String buildType;
    
    // Logical operators
    private List<ConditionSet> allOf;
    private List<ConditionSet> anyOf;
    private ConditionSet not;
    
    /**
     * Creates a new ConditionSet.
     */
    public ConditionSet() {}
    
    /**
     * Semantic versioning constraint (e.g., ">= 5.4.0", "~> 5.4", "^5.4.0")
     * 
     * @return the generator version constraint
     */
    public String getGeneratorVersion() {
        return generatorVersion;
    }
    
    /**
     * Sets the OpenAPI Generator version condition.
     * @param generatorVersion the version constraint (e.g., ">= 7.0.0")
     */
    public void setGeneratorVersion(String generatorVersion) {
        this.generatorVersion = generatorVersion;
    }
    
    /**
     * Pattern that must be present in the template
     * 
     * @return the pattern that must be present
     */
    public String getTemplateContains() {
        return templateContains;
    }
    
    /**
     * Sets the pattern that must be present in the template.
     * @param templateContains the pattern to search for
     */
    public void setTemplateContains(String templateContains) {
        this.templateContains = templateContains;
    }
    
    /**
     * Pattern that must NOT be present in the template
     * 
     * @return the pattern that must not be present
     */
    public String getTemplateNotContains() {
        return templateNotContains;
    }
    
    /**
     * Sets the pattern that must NOT be present in the template.
     * @param templateNotContains the pattern that should not exist
     */
    public void setTemplateNotContains(String templateNotContains) {
        this.templateNotContains = templateNotContains;
    }
    
    /**
     * All patterns must be present in the template
     * 
     * @return the list of patterns that must all be present
     */
    public List<String> getTemplateContainsAll() {
        return templateContainsAll;
    }
    
    /**
     * Sets patterns that must ALL be present in the template.
     * @param templateContainsAll the list of patterns that must all exist
     */
    public void setTemplateContainsAll(List<String> templateContainsAll) {
        this.templateContainsAll = templateContainsAll;
    }
    
    /**
     * At least one pattern must be present in the template
     * 
     * @return the list of patterns where at least one must be present
     */
    public List<String> getTemplateContainsAny() {
        return templateContainsAny;
    }
    
    /**
     * Sets patterns where at least one must be present in the template.
     * @param templateContainsAny the list of patterns where at least one must exist
     */
    public void setTemplateContainsAny(List<String> templateContainsAny) {
        this.templateContainsAny = templateContainsAny;
    }
    
    /**
     * Feature that must be supported by the template
     * 
     * @return the feature that must be supported
     */
    public String getHasFeature() {
        return hasFeature;
    }
    
    /**
     * Sets the feature that must be present.
     * @param hasFeature the feature name to check for
     */
    public void setHasFeature(String hasFeature) {
        this.hasFeature = hasFeature;
    }
    
    /**
     * All features must be supported by the template
     * 
     * @return the list of features that must all be supported
     */
    public List<String> getHasAllFeatures() {
        return hasAllFeatures;
    }
    
    /**
     * Sets features that must ALL be present.
     * @param hasAllFeatures the list of features that must all exist
     */
    public void setHasAllFeatures(List<String> hasAllFeatures) {
        this.hasAllFeatures = hasAllFeatures;
    }
    
    /**
     * At least one feature must be supported by the template
     * 
     * @return the list of features where at least one must be supported
     */
    public List<String> getHasAnyFeatures() {
        return hasAnyFeatures;
    }
    
    /**
     * Sets features where at least one must be present.
     * @param hasAnyFeatures the list of features where at least one must exist
     */
    public void setHasAnyFeatures(List<String> hasAnyFeatures) {
        this.hasAnyFeatures = hasAnyFeatures;
    }
    
    /**
     * Project property that must be set (format: "property=value" or just "property")
     * 
     * @return the project property condition
     */
    public String getProjectProperty() {
        return projectProperty;
    }
    
    /**
     * Sets the project property condition.
     * @param projectProperty the property to check (e.g., "debug=true")
     */
    public void setProjectProperty(String projectProperty) {
        this.projectProperty = projectProperty;
    }
    
    /**
     * Environment variable that must be set (format: "VAR=value" or just "VAR")
     * 
     * @return the environment variable condition
     */
    public String getEnvironmentVariable() {
        return environmentVariable;
    }
    
    /**
     * Sets the environment variable condition.
     * @param environmentVariable the environment variable to check (e.g., "CI=true")
     */
    public void setEnvironmentVariable(String environmentVariable) {
        this.environmentVariable = environmentVariable;
    }
    
    /**
     * Build type (e.g., "debug", "release")
     * 
     * @return the build type condition
     */
    public String getBuildType() {
        return buildType;
    }
    
    /**
     * Sets the build type condition.
     * @param buildType the build type to match (e.g., "debug", "release")
     */
    public void setBuildType(String buildType) {
        this.buildType = buildType;
    }
    
    /**
     * All nested conditions must be true
     * 
     * @return the list of conditions that must all be true
     */
    public List<ConditionSet> getAllOf() {
        return allOf;
    }
    
    /**
     * Sets conditions that must ALL be true.
     * @param allOf the list of conditions that must all be satisfied
     */
    public void setAllOf(List<ConditionSet> allOf) {
        this.allOf = allOf;
    }
    
    /**
     * At least one nested condition must be true
     * 
     * @return the list of conditions where at least one must be true
     */
    public List<ConditionSet> getAnyOf() {
        return anyOf;
    }
    
    /**
     * Sets conditions where at least one must be true.
     * @param anyOf the list of conditions where at least one must be satisfied
     */
    public void setAnyOf(List<ConditionSet> anyOf) {
        this.anyOf = anyOf;
    }
    
    /**
     * Nested condition must be false
     * 
     * @return the condition that must be false
     */
    public ConditionSet getNot() {
        return not;
    }
    
    /**
     * Sets the condition that must be false.
     * @param not the condition that must not be satisfied
     */
    public void setNot(ConditionSet not) {
        this.not = not;
    }
}