package com.guidedbyte.openapi.modelgen.logging.pattern;

/**
 * A pattern element that represents literal text in a log pattern.
 * 
 * <p>Examples: "[", "]", " - ", ":", etc.</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class LiteralElement implements PatternElement {
    private final String literal;
    
    public LiteralElement(String literal) {
        this.literal = literal != null ? literal : "";
    }
    
    @Override
    public void append(StringBuilder sb, String message, String spec, String template, String component) {
        sb.append(literal);
    }
    
    @Override
    public int estimateMaxLength() {
        return literal.length();
    }
    
    @Override
    public String toString() {
        return "LiteralElement['" + literal + "']";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LiteralElement)) return false;
        
        LiteralElement that = (LiteralElement) o;
        return literal.equals(that.literal);
    }
    
    @Override
    public int hashCode() {
        return literal.hashCode();
    }
}