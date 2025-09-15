package com.guidedbyte.openapi.modelgen.customization;

import java.util.List;

/**
 * Represents a semantic insertion point that adapts to template structure.
 * <p>
 * Smart insertions find logical insertion points in templates that may vary
 * across generator versions, using semantic understanding of template structure.
 * 
 * @since 2.0.0
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "YAML configuration object designed for deserialization by SnakeYAML library. " +
                   "Setter methods intentionally store object and collection references directly as this is the standard " +
                   "pattern for YAML/JSON data binding objects. Objects are effectively immutable after deserialization."
)
public class SmartInsertion {
    private InsertionPoint findInsertionPoint;
    private String semantic;
    private String content;
    private ConditionSet conditions;
    private Insertion fallback;
    
    public SmartInsertion() {}
    
    /**
     * Configuration for finding logical insertion points
     */
    public InsertionPoint getFindInsertionPoint() {
        return findInsertionPoint;
    }
    
    public void setFindInsertionPoint(InsertionPoint findInsertionPoint) {
        this.findInsertionPoint = findInsertionPoint;
    }
    
    /**
     * Semantic insertion point name (e.g., "after_model_declaration")
     */
    public String getSemantic() {
        return semantic;
    }
    
    public void setSemantic(String semantic) {
        this.semantic = semantic;
    }
    
    /**
     * Content to insert at the found insertion point
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
     * Fallback insertion when smart insertion fails
     */
    public Insertion getFallback() {
        return fallback;
    }
    
    public void setFallback(Insertion fallback) {
        this.fallback = fallback;
    }
    
    /**
     * Configuration for finding insertion points with multiple fallback patterns
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "YAML configuration nested class designed for deserialization by SnakeYAML library. " +
                       "Setter methods intentionally store collection references directly as this is the standard " +
                       "pattern for YAML/JSON data binding objects."
    )
    public static class InsertionPoint {
        private List<PatternLocation> patterns;
        
        public InsertionPoint() {}
        
        public List<PatternLocation> getPatterns() {
            return patterns;
        }
        
        public void setPatterns(List<PatternLocation> patterns) {
            this.patterns = patterns;
        }
    }
    
    /**
     * A pattern location specifying where to insert relative to a pattern
     */
    public static class PatternLocation {
        private String after;
        private String before;
        
        public PatternLocation() {}
        
        public String getAfter() {
            return after;
        }
        
        public void setAfter(String after) {
            this.after = after;
        }
        
        public String getBefore() {
            return before;
        }
        
        public void setBefore(String before) {
            this.before = before;
        }
    }
}