package com.guidedbyte.openapi.modelgen.services;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the LoggingContextFormatter for configurable MDC context formatting.
 */
class LoggingContextFormatterTest {

    @Test
    void testDefaultFormat() {
        LoggingContextFormatter formatter = new LoggingContextFormatter();
        
        // Test with spec and template
        String result = formatter.formatContext("spring", "pojo.mustache", "CustomizationEngine");
        assertEquals("[spring:pojo.mustache]", result);
        
        // Test with spec only
        result = formatter.formatContext("spring", null, "CustomizationEngine");
        assertEquals("[spring]", result);
    }
    
    @Test
    void testDebugFormat() {
        LoggingContextFormatter formatter = new LoggingContextFormatter(LoggingContextFormatter.DEBUG_FORMAT);
        
        // Test with all context
        String result = formatter.formatContext("spring", "pojo.mustache", "CustomizationEngine");
        assertEquals("[CustomizationEngine|spring:pojo.mustache]", result);
        
        // Test with spec only
        result = formatter.formatContext("spring", null, "CustomizationEngine");
        assertEquals("[CustomizationEngine|spring]", result);
    }
    
    @Test
    void testMinimalFormat() {
        LoggingContextFormatter formatter = new LoggingContextFormatter(LoggingContextFormatter.MINIMAL_FORMAT);
        
        String result = formatter.formatContext("spring", "pojo.mustache", "CustomizationEngine");
        assertEquals("[spring]", result);
    }
    
    @Test
    void testVerboseFormat() {
        LoggingContextFormatter formatter = new LoggingContextFormatter(LoggingContextFormatter.VERBOSE_FORMAT);
        
        String result = formatter.formatContext("spring", "pojo.mustache", "CustomizationEngine");
        assertEquals("[CustomizationEngine] [spring:pojo.mustache]", result);
    }
    
    @Test
    void testCustomFormat() {
        LoggingContextFormatter formatter = new LoggingContextFormatter("{{spec}}{{#template}} > {{template}}{{/template}} |");
        
        // With template
        String result = formatter.formatContext("spring", "pojo.mustache", null);
        assertEquals("spring > pojo.mustache |", result);
        
        // Without template
        result = formatter.formatContext("spring", null, null);
        assertEquals("spring |", result);
    }
    
    @Test
    void testEmptyFormat() {
        LoggingContextFormatter formatter = new LoggingContextFormatter("");
        
        String result = formatter.formatContext("spring", "pojo.mustache", "CustomizationEngine");
        assertEquals("", result);
    }
    
    @Test
    void testNoSpec() {
        LoggingContextFormatter formatter = new LoggingContextFormatter();
        
        String result = formatter.formatContext(null, "pojo.mustache", "CustomizationEngine");
        assertEquals("", result);
    }
    
    @Test
    void testFromUserConfigPredefined() {
        // Test predefined format names
        LoggingContextFormatter formatter = LoggingContextFormatter.fromUserConfig("debug");
        assertEquals(LoggingContextFormatter.DEBUG_FORMAT, formatter.getFormatTemplate());
        
        formatter = LoggingContextFormatter.fromUserConfig("minimal");
        assertEquals(LoggingContextFormatter.MINIMAL_FORMAT, formatter.getFormatTemplate());
        
        formatter = LoggingContextFormatter.fromUserConfig("none");
        assertEquals("", formatter.getFormatTemplate());
        
        formatter = LoggingContextFormatter.fromUserConfig("disabled");
        assertEquals("", formatter.getFormatTemplate());
    }
    
    @Test
    void testFromUserConfigCustom() {
        String customFormat = "spec={{spec}} template={{template}}";
        LoggingContextFormatter formatter = LoggingContextFormatter.fromUserConfig(customFormat);
        assertEquals(customFormat, formatter.getFormatTemplate());
    }
    
    @Test
    void testFromUserConfigNull() {
        LoggingContextFormatter formatter = LoggingContextFormatter.fromUserConfig(null);
        assertEquals(LoggingContextFormatter.DEFAULT_FORMAT, formatter.getFormatTemplate());
    }
    
    @Test
    void testConditionalSections() {
        // Test nested conditionals don't break
        LoggingContextFormatter formatter = new LoggingContextFormatter("{{spec}}{{#template}}:{{template}}{{/template}}{{#component}}[{{component}}]{{/component}}");
        
        // All values present
        String result = formatter.formatContext("spring", "pojo.mustache", "Engine");
        assertEquals("spring:pojo.mustache[Engine]", result);
        
        // Missing template
        result = formatter.formatContext("spring", null, "Engine");
        assertEquals("spring[Engine]", result);
        
        // Missing component
        result = formatter.formatContext("spring", "pojo.mustache", null);
        assertEquals("spring:pojo.mustache", result);
    }
}