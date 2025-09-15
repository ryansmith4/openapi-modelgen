package com.guidedbyte.openapi.modelgen.customization;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Root configuration class for template customizations loaded from YAML.
 * <p>
 * Represents the complete structure of a .yaml customization file that defines
 * surgical modifications to OpenAPI Generator templates.
 * 
 * @since 2.0.0
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "YAML configuration object designed for deserialization by SnakeYAML library. " +
                   "Setter methods intentionally store collection references directly as this is the standard " +
                   "pattern for YAML/JSON data binding objects. Collections are effectively immutable after " +
                   "deserialization and getter methods return defensive copies via Collections.unmodifiableList/Map()."
)
public class CustomizationConfig {
    private CustomizationMetadata metadata;
    private List<Insertion> insertions;
    private List<Replacement> replacements;
    private List<SmartReplacement> smartReplacements;
    private List<SmartInsertion> smartInsertions;
    private Map<String, String> partials;
    private ConditionSet conditions;
    
    /**
     * Creates a new CustomizationConfig instance.
     */
    public CustomizationConfig() {}
    
    /**
     * Gets the optional metadata for this customization file.
     * @return the customization metadata
     */
    public CustomizationMetadata getMetadata() {
        return metadata;
    }
    
    /**
     * Sets the metadata for this customization file.
     * @param metadata the customization metadata
     */
    public void setMetadata(CustomizationMetadata metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Gets the list of content insertions to apply to the template.
     * @return the list of insertions
     */
    public List<Insertion> getInsertions() {
        return insertions != null ? Collections.unmodifiableList(insertions) : null;
    }
    
    /**
     * Sets the list of content insertions to apply to the template.
     * @param insertions the list of insertions
     */
    public void setInsertions(List<Insertion> insertions) {
        this.insertions = insertions;
    }
    
    /**
     * Gets the list of find/replace operations to apply to the template.
     * @return the list of replacements
     */
    public List<Replacement> getReplacements() {
        return replacements != null ? Collections.unmodifiableList(replacements) : null;
    }
    
    /**
     * Sets the list of find/replace operations to apply to the template.
     * @param replacements the list of replacements
     */
    public void setReplacements(List<Replacement> replacements) {
        this.replacements = replacements;
    }
    
    /**
     * Gets the list of version-agnostic smart replacements.
     * @return the list of smart replacements
     */
    public List<SmartReplacement> getSmartReplacements() {
        return smartReplacements != null ? Collections.unmodifiableList(smartReplacements) : null;
    }
    
    /**
     * Sets the list of version-agnostic smart replacements.
     * @param smartReplacements the list of smart replacements
     */
    public void setSmartReplacements(List<SmartReplacement> smartReplacements) {
        this.smartReplacements = smartReplacements;
    }
    
    /**
     * Gets the list of semantic insertion points.
     * @return the list of smart insertions
     */
    public List<SmartInsertion> getSmartInsertions() {
        return smartInsertions != null ? Collections.unmodifiableList(smartInsertions) : null;
    }
    
    /**
     * Sets the list of semantic insertion points.
     * @param smartInsertions the list of smart insertions
     */
    public void setSmartInsertions(List<SmartInsertion> smartInsertions) {
        this.smartInsertions = smartInsertions;
    }
    
    /**
     * Gets the reusable template fragments referenced by insertions and replacements.
     * @return the map of partial templates
     */
    public Map<String, String> getPartials() {
        return partials != null ? Collections.unmodifiableMap(partials) : null;
    }
    
    /**
     * Sets the reusable template fragments referenced by insertions and replacements.
     * @param partials the map of partial templates
     */
    public void setPartials(Map<String, String> partials) {
        this.partials = partials;
    }
    
    /**
     * Gets the global conditions that determine if this entire customization applies.
     * @return the condition set
     */
    public ConditionSet getConditions() {
        return conditions;
    }
    
    /**
     * Sets the global conditions that determine if this entire customization applies.
     * @param conditions the condition set
     */
    public void setConditions(ConditionSet conditions) {
        this.conditions = conditions;
    }
}