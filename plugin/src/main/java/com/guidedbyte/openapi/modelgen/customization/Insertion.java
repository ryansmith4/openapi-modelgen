package com.guidedbyte.openapi.modelgen.customization;

/**
 * Represents a content insertion operation in template customization.
 * 
 * Insertions add content at specific locations in templates, supporting:
 * - Insert after a pattern
 * - Insert before a pattern  
 * - Insert at start/end of template
 * - Conditional insertions based on generator version/features
 * - Fallback insertions when conditions fail
 * 
 * @since 2.0.0
 */
public class Insertion {
    private String after;
    private String before;
    private String at;
    private String content;
    private ConditionSet conditions;
    private Insertion fallback;
    
    public Insertion() {}
    
    /**
     * Pattern to insert content after
     */
    public String getAfter() {
        return after;
    }
    
    public void setAfter(String after) {
        this.after = after;
    }
    
    /**
     * Pattern to insert content before
     */
    public String getBefore() {
        return before;
    }
    
    public void setBefore(String before) {
        this.before = before;
    }
    
    /**
     * Special insertion points: "start" or "end"
     */
    public String getAt() {
        return at;
    }
    
    public void setAt(String at) {
        this.at = at;
    }
    
    /**
     * Content to insert (can reference partials with {{>partialName}})
     */
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    /**
     * Conditions that must be met for this insertion to apply
     */
    public ConditionSet getConditions() {
        return conditions;
    }
    
    public void setConditions(ConditionSet conditions) {
        this.conditions = conditions;
    }
    
    /**
     * Fallback insertion to use when conditions are not met
     */
    public Insertion getFallback() {
        return fallback;
    }
    
    public void setFallback(Insertion fallback) {
        this.fallback = fallback;
    }
}