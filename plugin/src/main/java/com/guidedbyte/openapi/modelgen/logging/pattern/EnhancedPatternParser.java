package com.guidedbyte.openapi.modelgen.logging.pattern;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import com.guidedbyte.openapi.modelgen.util.PluginLoggerFactory;

/**
 * High-performance parser for SLF4J log patterns that converts them into compiled elements.
 * 
 * <p>Supports SLF4J patterns including format modifiers:</p>
 * <ul>
 *   <li><code>%X{key}</code> - MDC variables</li>
 *   <li><code>%msg</code> or <code>%m</code> - Log message</li>
 *   <li><code>%d{pattern}</code> - Timestamps</li>
 *   <li><code>%-20X{spec}</code> - Left-align with minimum width</li>
 *   <li><code>%.15X{template}</code> - Truncate to maximum width</li>
 *   <li><code>%-10.30X{component}</code> - Left-align with min/max width</li>
 * </ul>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class EnhancedPatternParser {

    private static final Logger logger = PluginLoggerFactory.getLogger(EnhancedPatternParser.class);
    
    /**
     * Parses an SLF4J pattern string into a list of pattern elements for high-performance formatting.
     * 
     * @param pattern the SLF4J pattern string
     * @return list of compiled pattern elements
     * @throws IllegalArgumentException if the pattern is malformed
     */
    public static List<PatternElement> parsePattern(String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }
        
        List<PatternElement> elements = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            
            if (c == '%' && i + 1 < pattern.length()) {
                // Check if this looks like a valid pattern
                char nextChar = pattern.charAt(i + 1);
                if (isValidPatternStart(nextChar) || Character.isDigit(nextChar) || nextChar == '-' || nextChar == '.') {
                    // Flush any accumulated literal text
                    if (!literal.isEmpty()) {
                        elements.add(new LiteralElement(literal.toString()));
                        literal.setLength(0);
                    }
                    
                    // Parse the % pattern
                    i = parsePercentPattern(pattern, i, elements) - 1; // -1 because loop will ++
                } else {
                    // Not a valid pattern, treat % as literal
                    literal.append(c);
                }
            } else {
                literal.append(c);
            }
        }
        
        // Flush remaining literal
        if (!literal.isEmpty()) {
            elements.add(new LiteralElement(literal.toString()));
        }
        
        return elements;
    }
    
    /**
     * Checks if a character can start a valid SLF4J pattern.
     */
    private static boolean isValidPatternStart(char c) {
        return c == 'X' || c == 'd' || c == 'm';
    }
    
    private static int parsePercentPattern(String pattern, int start, List<PatternElement> elements) {
        int i = start + 1; // Skip the %
        
        if (i >= pattern.length()) {
            // Lone % at end of pattern - treat as literal
            elements.add(new LiteralElement("%"));
            return i;
        }
        
        // Parse format modifier: %[-][\d*][.\d*]
        FormatModifierResult modifierResult = parseFormatModifier(pattern, i);
        FormatModifier modifier = modifierResult.modifier;
        i = modifierResult.endPosition;
        
        if (i >= pattern.length()) {
            // Pattern ends after modifier - treat as literal
            elements.add(new LiteralElement(pattern.substring(start, i)));
            return i;
        }
        
        char patternChar = pattern.charAt(i);
        switch (patternChar) {
            case 'X':
                // Parse %X{key}
                return parseMDCPattern(pattern, i, elements, modifier);
                
            case 'd':
                // Parse %d{format}
                return parseTimestampPattern(pattern, i, elements, modifier);
                
            case 'm':
                // Parse %msg or %m
                if (i + 2 < pattern.length() && pattern.startsWith("msg", i)) {
                    elements.add(new MessageElement(modifier));
                    return i + 3;
                } else {
                    elements.add(new MessageElement(modifier));
                    return i + 1;
                }
                
            default:
                // Unknown pattern - treat % as literal and backtrack
                elements.add(new LiteralElement("%"));
                return start + 1; // Return to just after the %, let main loop handle the rest
        }
    }
    
    private static int parseMDCPattern(String pattern, int start, List<PatternElement> elements, FormatModifier modifier) {
        int i = start + 1; // Skip 'X'
        
        if (i >= pattern.length() || pattern.charAt(i) != '{') {
            // Not %X{key} format - treat as literal
            elements.add(new LiteralElement(pattern.substring(start - (modifier.isNone() ? 1 : 2), i)));
            return i;
        }
        
        i++; // Skip '{'
        int closeIndex = pattern.indexOf('}', i);
        if (closeIndex == -1) {
            // No closing brace - treat as literal
            elements.add(new LiteralElement(pattern.substring(start - (modifier.isNone() ? 1 : 2))));
            return pattern.length();
        }
        
        String key = pattern.substring(i, closeIndex);
        if (key.trim().isEmpty()) {
            // Empty key - treat as literal
            elements.add(new LiteralElement(pattern.substring(start - (modifier.isNone() ? 1 : 2), closeIndex + 1)));
            return closeIndex + 1;
        }
        
        elements.add(new MDCElement(key, modifier));
        return closeIndex + 1;
    }
    
    private static int parseTimestampPattern(String pattern, int start, List<PatternElement> elements, FormatModifier modifier) {
        int i = start + 1; // Skip 'd'
        
        if (i >= pattern.length() || pattern.charAt(i) != '{') {
            // Just %d without format - use default
            elements.add(new TimestampElement(null, modifier));
            return i;
        }
        
        i++; // Skip '{'
        int closeIndex = pattern.indexOf('}', i);
        if (closeIndex == -1) {
            // No closing brace - use default format
            elements.add(new TimestampElement(null, modifier));
            return i;
        }
        
        String datePattern = pattern.substring(i, closeIndex);
        elements.add(new TimestampElement(datePattern, modifier));
        return closeIndex + 1;
    }
    
    private static FormatModifierResult parseFormatModifier(String pattern, int start) {
        boolean leftAlign = false;
        int minWidth = 0;
        int maxWidth = 0;
        int i = start;
        
        // Check for left alignment: %-
        if (i < pattern.length() && pattern.charAt(i) == '-') {
            leftAlign = true;
            i++;
        }
        
        // Parse minimum width: %20 or %-20
        StringBuilder widthStr = new StringBuilder();
        while (i < pattern.length() && Character.isDigit(pattern.charAt(i))) {
            widthStr.append(pattern.charAt(i));
            i++;
        }
        if (!widthStr.isEmpty()) {
            try {
                minWidth = Integer.parseInt(widthStr.toString());
            } catch (NumberFormatException e) {
                logger.warn("Invalid minimum width in pattern: '{}'. Ignoring width modifier.", widthStr);
            }
        }
        
        // Parse maximum width: %.30
        if (i < pattern.length() && pattern.charAt(i) == '.') {
            i++; // Skip the dot
            StringBuilder maxWidthStr = new StringBuilder();
            while (i < pattern.length() && Character.isDigit(pattern.charAt(i))) {
                maxWidthStr.append(pattern.charAt(i));
                i++;
            }
            if (!maxWidthStr.isEmpty()) {
                try {
                    maxWidth = Integer.parseInt(maxWidthStr.toString());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid maximum width in pattern: '{}'. Ignoring width modifier.", maxWidthStr);
                }
            }
        }
        
        FormatModifier modifier = (leftAlign || minWidth > 0 || maxWidth > 0) 
            ? new FormatModifier(leftAlign, minWidth, maxWidth)
            : FormatModifier.NONE;
            
        return new FormatModifierResult(modifier, i);
    }
    
    /**
     * Result of parsing a format modifier.
     */
    private static class FormatModifierResult {
        final FormatModifier modifier;
        final int endPosition;
        
        FormatModifierResult(FormatModifier modifier, int endPosition) {
            this.modifier = modifier;
            this.endPosition = endPosition;
        }
    }
}