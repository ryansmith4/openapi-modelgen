package com.guidedbyte.openapi.modelgen.logging.pattern;

/**
 * Represents a single element in a compiled SLF4J log pattern.
 * 
 * <p>Pattern elements are created once during pattern compilation and then
 * used repeatedly for high-performance log formatting. Each element knows
 * how to append its formatted content to a StringBuilder.</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public interface PatternElement {
    
    /**
     * Appends this element's formatted content to the provided StringBuilder.
     * 
     * @param sb the StringBuilder to append to
     * @param message the log message
     * @param spec the current OpenAPI spec name (can be null)
     * @param template the current template name (can be null)
     * @param component the current component name (can be null)
     */
    void append(StringBuilder sb, String message, String spec, String template, String component);
    
    /**
     * Estimates the maximum number of characters this element might contribute
     * to the final formatted string. Used for StringBuilder pre-allocation.
     * 
     * @return estimated maximum character count
     */
    int estimateMaxLength();
}