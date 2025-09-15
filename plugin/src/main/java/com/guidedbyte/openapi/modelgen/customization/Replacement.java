package com.guidedbyte.openapi.modelgen.customization;

/**
 * Represents a find/replace operation in template customization.
 * <p>
 * Replacements modify existing content in templates, supporting:
 * - Simple string replacement
 * - Multi-line replacement
 * - Regex replacement (use carefully)
 * - Conditional replacements based on generator version/features
 * - Fallback replacements when conditions fail
 * 
 * @since 2.0.0
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "YAML configuration object designed for deserialization by SnakeYAML library. " +
                   "Setter methods intentionally store object references directly as this is the standard " +
                   "pattern for YAML/JSON data binding objects. Objects are effectively immutable after deserialization."
)
public class Replacement {
    private String find;
    private String replace;
    private String type = "string"; // Default to string replacement
    private ConditionSet conditions;
    private Replacement fallback;
    
    /**
     * Constructs a new Replacement.
     */
    public Replacement() {}
    
    /**
     * Pattern to find in the template.
     * @return the find pattern
     */
    public String getFind() {
        return find;
    }
    
    /**
     * Sets the find pattern.
     * @param find the find pattern
     */
    public void setFind(String find) {
        this.find = find;
    }
    
    /**
     * Content to replace the found pattern with.
     * @return the replacement content
     */
    public String getReplace() {
        return replace;
    }
    
    /**
     * Sets the replacement content.
     * @param replace the replacement content
     */
    public void setReplace(String replace) {
        this.replace = replace;
    }
    
    /**
     * Type of replacement: "string" (default) or "regex".
     * @return the replacement type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Sets the replacement type.
     * @param type the replacement type
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Conditions that must be met for this replacement to apply.
     * @return the conditions
     */
    public ConditionSet getConditions() {
        return conditions;
    }
    
    /**
     * Sets the conditions.
     * @param conditions the conditions
     */
    public void setConditions(ConditionSet conditions) {
        this.conditions = conditions;
    }
    
    /**
     * Fallback replacement to use when conditions are not met.
     * @return the fallback replacement
     */
    public Replacement getFallback() {
        return fallback;
    }
    
    /**
     * Sets the fallback replacement.
     * @param fallback the fallback replacement
     */
    public void setFallback(Replacement fallback) {
        this.fallback = fallback;
    }
}