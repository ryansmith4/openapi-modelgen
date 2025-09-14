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
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "YAML configuration object designed for deserialization by SnakeYAML library. " +
                   "Setter methods intentionally store object references directly as this is the standard " +
                   "pattern for YAML/JSON data binding objects. Objects are effectively immutable after deserialization."
)
public class Insertion {
    private String after;
    private String before;
    private String at;
    private String content;
    private ConditionSet conditions;
    private Insertion fallback;
    
    /**
     * Constructs a new Insertion.
     */
    public Insertion() {}
    
    /**
     * Pattern to insert content after.
     * @return the after pattern
     */
    public String getAfter() {
        return after;
    }
    
    /**
     * Sets the after pattern.
     * @param after the after pattern
     */
    public void setAfter(String after) {
        this.after = after;
    }
    
    /**
     * Pattern to insert content before.
     * @return the before pattern
     */
    public String getBefore() {
        return before;
    }
    
    /**
     * Sets the before pattern.
     * @param before the before pattern
     */
    public void setBefore(String before) {
        this.before = before;
    }
    
    /**
     * Special insertion points: "start" or "end".
     * @return the at position
     */
    public String getAt() {
        return at;
    }
    
    /**
     * Sets the at position.
     * @param at the at position
     */
    public void setAt(String at) {
        this.at = at;
    }
    
    /**
     * Content to insert (can reference partials with {{>partialName}}).
     * @return the content
     */
    public String getContent() {
        return content;
    }
    
    /**
     * Sets the content.
     * @param content the content
     */
    public void setContent(String content) {
        this.content = content;
    }
    
    /**
     * Conditions that must be met for this insertion to apply.
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
     * Fallback insertion to use when conditions are not met.
     * @return the fallback insertion
     */
    public Insertion getFallback() {
        return fallback;
    }
    
    /**
     * Sets the fallback insertion.
     * @param fallback the fallback insertion
     */
    public void setFallback(Insertion fallback) {
        this.fallback = fallback;
    }
}