package com.guidedbyte.openapi.modelgen.logging.pattern;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for EnhancedPatternParser class.
 */
class EnhancedPatternParserTest {

    @Test
    void testNullPattern() {
        assertThrows(IllegalArgumentException.class, () -> EnhancedPatternParser.parsePattern(null));
    }

    @Test
    void testEmptyPattern() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("");
        assertTrue(elements.isEmpty());
    }

    @Test
    void testLiteralOnly() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("literal text");
        
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof LiteralElement);
        
        // Test formatting
        StringBuilder sb = new StringBuilder();
        elements.get(0).append(sb, "msg", "spec", "template", "component");
        assertEquals("literal text", sb.toString());
    }

    @Test
    void testBasicMDCVariable() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%X{spec}");
        
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof MDCElement);
        
        MDCElement mdcElement = (MDCElement) elements.get(0);
        assertEquals("spec", mdcElement.getKey());
        assertTrue(mdcElement.getModifier().isNone());
    }

    @Test
    void testMDCVariableWithFormatModifier() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%-10X{spec}");
        
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof MDCElement);
        
        MDCElement mdcElement = (MDCElement) elements.get(0);
        assertEquals("spec", mdcElement.getKey());
        FormatModifier modifier = mdcElement.getModifier();
        assertFalse(modifier.isNone());
        assertTrue(modifier.isLeftAlign());
        assertEquals(10, modifier.getMinWidth());
        assertEquals(0, modifier.getMaxWidth());
    }

    @Test
    void testMDCVariableWithMaxWidth() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%.15X{template}");
        
        assertEquals(1, elements.size());
        MDCElement mdcElement = (MDCElement) elements.get(0);
        assertEquals("template", mdcElement.getKey());
        FormatModifier modifier = mdcElement.getModifier();
        assertEquals(0, modifier.getMinWidth());
        assertEquals(15, modifier.getMaxWidth());
        assertFalse(modifier.isLeftAlign());
    }

    @Test
    void testMDCVariableWithMinAndMaxWidth() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%-10.20X{component}");
        
        assertEquals(1, elements.size());
        MDCElement mdcElement = (MDCElement) elements.get(0);
        assertEquals("component", mdcElement.getKey());
        FormatModifier modifier = mdcElement.getModifier();
        assertTrue(modifier.isLeftAlign());
        assertEquals(10, modifier.getMinWidth());
        assertEquals(20, modifier.getMaxWidth());
    }

    @Test
    void testBasicMessage() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%msg");
        
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof MessageElement);
        
        MessageElement msgElement = (MessageElement) elements.get(0);
        assertTrue(msgElement.getModifier().isNone());
    }

    @Test
    void testShortMessage() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%m");
        
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof MessageElement);
    }

    @Test
    void testMessageWithModifier() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%-50.100msg");
        
        assertEquals(1, elements.size());
        MessageElement msgElement = (MessageElement) elements.get(0);
        FormatModifier modifier = msgElement.getModifier();
        assertTrue(modifier.isLeftAlign());
        assertEquals(50, modifier.getMinWidth());
        assertEquals(100, modifier.getMaxWidth());
    }

    @Test
    void testBasicTimestamp() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%d");
        
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof TimestampElement);
    }

    @Test
    void testTimestampWithFormat() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%d{HH:mm:ss}");
        
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof TimestampElement);
        
        // Test that it formats without throwing exception
        StringBuilder sb = new StringBuilder();
        elements.get(0).append(sb, "msg", "spec", "template", "component");
        assertTrue(sb.length() > 0);
        assertTrue(sb.toString().matches("\\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testTimestampWithModifier() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%-15d{ISO8601}");
        
        assertEquals(1, elements.size());
        TimestampElement tsElement = (TimestampElement) elements.get(0);
        FormatModifier modifier = tsElement.getModifier();
        assertTrue(modifier.isLeftAlign());
        assertEquals(15, modifier.getMinWidth());
    }

    @Test
    void testComplexPattern() {
        String pattern = "%d{HH:mm:ss} [%-8X{spec}:%-15X{template}] [%X{component}] - %msg";
        List<PatternElement> elements = EnhancedPatternParser.parsePattern(pattern);
        
        // Should have: timestamp, literal, MDC, literal, MDC, literal, MDC, literal, message
        assertEquals(9, elements.size());
        
        assertTrue(elements.get(0) instanceof TimestampElement);
        assertTrue(elements.get(1) instanceof LiteralElement);
        assertTrue(elements.get(2) instanceof MDCElement);
        assertTrue(elements.get(3) instanceof LiteralElement);
        assertTrue(elements.get(4) instanceof MDCElement);
        assertTrue(elements.get(5) instanceof LiteralElement);
        assertTrue(elements.get(6) instanceof MDCElement);
        assertTrue(elements.get(7) instanceof LiteralElement);
        assertTrue(elements.get(8) instanceof MessageElement);
        
        // Test MDC elements have correct modifiers
        MDCElement specElement = (MDCElement) elements.get(2);
        assertEquals("spec", specElement.getKey());
        assertTrue(specElement.getModifier().isLeftAlign());
        assertEquals(8, specElement.getModifier().getMinWidth());
        
        MDCElement templateElement = (MDCElement) elements.get(4);
        assertEquals("template", templateElement.getKey());
        assertTrue(templateElement.getModifier().isLeftAlign());
        assertEquals(15, templateElement.getModifier().getMinWidth());
        
        MDCElement componentElement = (MDCElement) elements.get(6);
        assertEquals("component", componentElement.getKey());
        assertTrue(componentElement.getModifier().isNone());
    }

    @Test
    void testMalformedMDC() {
        // Missing closing brace
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%X{spec missing brace");
        
        // Should be treated as literal
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof LiteralElement);
    }

    @Test
    void testMalformedTimestamp() {
        // Missing closing brace
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%d{HH:mm:ss missing brace");
        
        // Should create timestamp with partial format and literal remainder
        assertEquals(2, elements.size());
        assertTrue(elements.get(0) instanceof TimestampElement);
        assertTrue(elements.get(1) instanceof LiteralElement);
    }

    @Test
    void testEmptyMDCKey() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%X{}");
        
        // Should be treated as literal
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof LiteralElement);
    }

    @Test
    void testUnknownPattern() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%unknown");
        
        // Should be treated as literal
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof LiteralElement);
    }

    @Test
    void testLonePercent() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("test % end");
        
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof LiteralElement);
        
        StringBuilder sb = new StringBuilder();
        elements.get(0).append(sb, "msg", "spec", "template", "component");
        assertEquals("test % end", sb.toString());
    }

    @Test
    void testPercentAtEnd() {
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("test%");
        
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof LiteralElement);
        
        StringBuilder sb = new StringBuilder();
        elements.get(0).append(sb, "msg", "spec", "template", "component");
        assertEquals("test%", sb.toString());
    }

    @Test
    void testInvalidModifierNumbers() {
        // Test that invalid numbers are handled gracefully
        List<PatternElement> elements = EnhancedPatternParser.parsePattern("%999999999999999999999X{spec}");
        
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof MDCElement);
        
        // Should still work even if number parsing fails
        MDCElement element = (MDCElement) elements.get(0);
        assertEquals("spec", element.getKey());
    }

    @Test
    void testMixedPatterns() {
        String pattern = "Start [%X{spec}] middle %-10X{template} %msg end";
        List<PatternElement> elements = EnhancedPatternParser.parsePattern(pattern);
        
        assertEquals(7, elements.size());
        
        // Verify element types
        assertTrue(elements.get(0) instanceof LiteralElement);  // "Start ["
        assertTrue(elements.get(1) instanceof MDCElement);     // spec
        assertTrue(elements.get(2) instanceof LiteralElement); // "] middle "
        assertTrue(elements.get(3) instanceof MDCElement);     // template with modifier
        assertTrue(elements.get(4) instanceof LiteralElement); // " "
        assertTrue(elements.get(5) instanceof MessageElement); // msg
        assertTrue(elements.get(6) instanceof LiteralElement); // " end"
        
        // Test that the complete pattern formats correctly
        StringBuilder sb = new StringBuilder();
        for (PatternElement element : elements) {
            element.append(sb, "test message", "testspec", "test.mustache", "TestComponent");
        }
        
        String result = sb.toString();
        assertTrue(result.contains("Start [testspec]"));
        assertTrue(result.contains("test.mustache"));
        assertTrue(result.contains("test message"));
    }
}