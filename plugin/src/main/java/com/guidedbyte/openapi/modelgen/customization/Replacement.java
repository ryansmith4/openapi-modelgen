package com.guidedbyte.openapi.modelgen.customization;

/**
 * Represents a find/replace operation in template customization.
 * 
 * Replacements modify existing content in templates, supporting:
 * - Simple string replacement
 * - Multi-line replacement
 * - Regex replacement (use carefully)
 * - Conditional replacements based on generator version/features
 * - Fallback replacements when conditions fail
 * 
 * @since 2.0.0
 */
public class Replacement {
    private String find;
    private String replace;
    private String type = "string"; // Default to string replacement
    private ConditionSet conditions;
    private Replacement fallback;
    
    public Replacement() {}
    
    /**
     * Pattern to find in the template
     */
    public String getFind() {
        return find;
    }
    
    public void setFind(String find) {
        this.find = find;
    }
    
    /**
     * Content to replace the found pattern with
     */
    public String getReplace() {
        return replace;
    }
    
    public void setReplace(String replace) {
        this.replace = replace;
    }
    
    /**
     * Type of replacement: "string" (default) or "regex"
     */
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Conditions that must be met for this replacement to apply
     */
    public ConditionSet getConditions() {
        return conditions;
    }
    
    public void setConditions(ConditionSet conditions) {
        this.conditions = conditions;
    }
    
    /**
     * Fallback replacement to use when conditions are not met
     */
    public Replacement getFallback() {
        return fallback;
    }
    
    public void setFallback(Replacement fallback) {
        this.fallback = fallback;
    }
}