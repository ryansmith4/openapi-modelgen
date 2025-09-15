package com.guidedbyte.openapi.modelgen.customization;

import java.util.List;

/**
 * Represents a version-agnostic smart replacement that tries multiple patterns.
 * <p>
 * Smart replacements adapt to different template structures across generator versions
 * by trying multiple find patterns and using the first one that matches.
 * 
 * @since 2.0.0
 */
public class SmartReplacement {
    private List<String> findAny;
    private String semantic;
    private FindPattern findPattern;
    private String replace;
    private ConditionSet conditions;
    
    public SmartReplacement() {}
    
    /**
     * List of patterns to try in order - uses first match
     */
    public List<String> getFindAny() {
        return findAny;
    }
    
    public void setFindAny(List<String> findAny) {
        this.findAny = findAny;
    }
    
    /**
     * Semantic replacement type (e.g., "validation_check")
     */
    public String getSemantic() {
        return semantic;
    }
    
    public void setSemantic(String semantic) {
        this.semantic = semantic;
    }
    
    /**
     * Complex pattern matching configuration
     */
    public FindPattern getFindPattern() {
        return findPattern;
    }
    
    public void setFindPattern(FindPattern findPattern) {
        this.findPattern = findPattern;
    }
    
    /**
     * Content to replace matched pattern with
     */
    public String getReplace() {
        return replace;
    }
    
    public void setReplace(String replace) {
        this.replace = replace;
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
     * Complex pattern matching configuration for smart replacements
     */
    public static class FindPattern {
        private String type;
        private List<String> variants;
        
        public FindPattern() {}
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public List<String> getVariants() {
            return variants;
        }
        
        public void setVariants(List<String> variants) {
            this.variants = variants;
        }
    }
}