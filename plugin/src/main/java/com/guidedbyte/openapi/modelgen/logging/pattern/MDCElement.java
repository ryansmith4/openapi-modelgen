package com.guidedbyte.openapi.modelgen.logging.pattern;

import com.guidedbyte.openapi.modelgen.constants.LoggingConstants;

/**
 * A pattern element that represents an MDC variable in a log pattern.
 * 
 * <p>Examples: %X{spec}, %X{template}, %X{component}, %-10X{spec}, %.15X{template}</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class MDCElement implements PatternElement {
    private final String key;
    private final FormatModifier modifier;
    
    public MDCElement(String key, FormatModifier modifier) {
        this.key = key != null ? key : "";
        this.modifier = modifier != null ? modifier : FormatModifier.NONE;
    }
    
    public MDCElement(String key) {
        this(key, FormatModifier.NONE);
    }
    
    @Override
    public void append(StringBuilder sb, String message, String spec, String template, String component) {
        String value = getMDCValue(key, spec, template, component);
        String formatted = modifier.apply(value);
        sb.append(formatted);
    }
    
    @Override
    public int estimateMaxLength() {
        // Estimate based on typical values and format modifiers
        int baseEstimate;
        switch (key) {
            case "spec":
                baseEstimate = LoggingConstants.TYPICAL_SPEC_NAME_LENGTH; // Typical spec names: "spring", "orders", "users"
                break;
            case "template":
                baseEstimate = LoggingConstants.TYPICAL_TEMPLATE_NAME_LENGTH; // Typical template names: "pojo.mustache", "enumClass.mustache"
                break;
            case "component":
                baseEstimate = LoggingConstants.TYPICAL_COMPONENT_NAME_LENGTH; // Component names: "CustomizationEngine", "PrepareTemplateDirectoryTask"
                break;
            default:
                baseEstimate = LoggingConstants.DEFAULT_MDC_LENGTH; // Unknown MDC keys
        }
        
        // Apply format modifier constraints
        if (modifier.getMaxWidth() > 0) {
            baseEstimate = Math.min(baseEstimate, modifier.getMaxWidth());
        }
        if (modifier.getMinWidth() > 0) {
            baseEstimate = Math.max(baseEstimate, modifier.getMinWidth());
        }
        
        return baseEstimate;
    }
    
    private String getMDCValue(String key, String spec, String template, String component) {
        switch (key) {
            case "spec":
                return spec != null ? spec : "";
            case "template":
                return template != null ? template : "";
            case "component":
                return component != null ? component : "";
            default:
                // Unknown MDC key - return empty string (consistent with MDC.get() for missing keys)
                return "";
        }
    }
    
    public String getKey() {
        return key;
    }
    
    public FormatModifier getModifier() {
        return modifier;
    }
    
    @Override
    public String toString() {
        if (modifier.isNone()) {
            return "MDCElement[" + key + "]";
        } else {
            return "MDCElement[" + key + ", " + modifier + "]";
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MDCElement)) return false;
        
        MDCElement that = (MDCElement) o;
        return key.equals(that.key) && modifier.equals(that.modifier);
    }
    
    @Override
    public int hashCode() {
        return LoggingConstants.HASH_PRIME * key.hashCode() + modifier.hashCode();
    }
}