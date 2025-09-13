package com.guidedbyte.openapi.modelgen.logging.pattern;

/**
 * A pattern element that represents the log message in a log pattern.
 * 
 * <p>Examples: %msg, %m, %-50msg, %.100m</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class MessageElement implements PatternElement {
    private final FormatModifier modifier;
    
    public MessageElement(FormatModifier modifier) {
        this.modifier = modifier != null ? modifier : FormatModifier.NONE;
    }
    
    public MessageElement() {
        this(FormatModifier.NONE);
    }
    
    @Override
    public void append(StringBuilder sb, String message, String spec, String template, String component) {
        String formatted = modifier.apply(message);
        sb.append(formatted);
    }
    
    @Override
    public int estimateMaxLength() {
        // Estimate typical log message length
        int baseEstimate = 80; // Typical message: "Processing customization for template: pojo.mustache"
        
        // Apply format modifier constraints
        if (modifier.getMaxWidth() > 0) {
            baseEstimate = Math.min(baseEstimate, modifier.getMaxWidth());
        }
        if (modifier.getMinWidth() > 0) {
            baseEstimate = Math.max(baseEstimate, modifier.getMinWidth());
        }
        
        return baseEstimate;
    }
    
    public FormatModifier getModifier() {
        return modifier;
    }
    
    @Override
    public String toString() {
        if (modifier.isNone()) {
            return "MessageElement[]";
        } else {
            return "MessageElement[" + modifier + "]";
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageElement)) return false;
        
        MessageElement that = (MessageElement) o;
        return modifier.equals(that.modifier);
    }
    
    @Override
    public int hashCode() {
        return modifier.hashCode();
    }
}