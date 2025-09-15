package com.guidedbyte.openapi.modelgen.logging.pattern;

import com.guidedbyte.openapi.modelgen.constants.PluginConstants;

import java.util.List;

/**
 * A compiled SLF4J log pattern optimized for high-performance formatting.
 * 
 * <p>This class represents a parsed log pattern that has been compiled into
 * fast-executing elements. The pattern is parsed once during construction
 * and then used repeatedly for efficient log message formatting.</p>
 * 
 * <p>Performance optimizations:</p>
 * <ul>
 *   <li>Pattern elements stored as array for fast iteration</li>
 *   <li>StringBuilder pre-allocated based on estimated length</li>
 *   <li>No regex or string parsing during format operations</li>
 *   <li>Direct element appending with minimal object allocation</li>
 * </ul>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public final class CompiledSLF4JPattern {
    private final PatternElement[] elements;
    private final int estimatedLength;
    private final String originalPattern;
    
    /**
     * Compiles the given SLF4J pattern string into fast-executing elements.
     * 
     * @param pattern the SLF4J pattern to compile
     * @throws IllegalArgumentException if the pattern is malformed
     */
    public CompiledSLF4JPattern(String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }
        
        this.originalPattern = pattern;
        
        List<PatternElement> elementList = EnhancedPatternParser.parsePattern(pattern);
        this.elements = elementList.toArray(new PatternElement[0]);
        this.estimatedLength = calculateEstimatedLength();
    }
    
    /**
     * Formats a log message using this compiled pattern.
     * 
     * @param message the log message (can be null)
     * @param spec the current OpenAPI spec name (can be null)
     * @param template the current template name (can be null)
     * @param component the current component name (can be null)
     * @return the formatted log string
     */
    public String format(String message, String spec, String template, String component) {
        // Pre-allocate StringBuilder with estimated capacity to minimize resizing
        StringBuilder sb = new StringBuilder(estimatedLength);
        
        // Fast iteration over pre-compiled elements
        for (PatternElement element : elements) {
            element.append(sb, message, spec, template, component);
        }
        
        return sb.toString();
    }
    
    
    private int calculateEstimatedLength() {
        int total = 0;
        for (PatternElement element : elements) {
            total += element.estimateMaxLength();
        }
        
        // Add some buffer for growth, but cap at reasonable maximum
        int withBuffer = total + PluginConstants.LOG_MESSAGE_BUFFER;
        return Math.min(withBuffer, PluginConstants.LOG_MESSAGE_MAX_LENGTH); // Cap at reasonable length for extremely long patterns
    }
    
    @Override
    public String toString() {
        return "CompiledSLF4JPattern[" + originalPattern + ", " + elements.length + " elements, ~" + estimatedLength + " chars]";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompiledSLF4JPattern)) return false;
        
        CompiledSLF4JPattern that = (CompiledSLF4JPattern) o;
        return originalPattern.equals(that.originalPattern);
    }
    
    @Override
    public int hashCode() {
        return originalPattern.hashCode();
    }
}