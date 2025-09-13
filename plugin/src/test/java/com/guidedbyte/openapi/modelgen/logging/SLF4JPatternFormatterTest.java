package com.guidedbyte.openapi.modelgen.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SLF4JPatternFormatter class.
 */
class SLF4JPatternFormatterTest {

    @BeforeEach
    void setUp() {
        SLF4JPatternFormatter.clearPatternCache();
        // Note: Don't clear MDC here as tests depend on it for context
    }
    
    @AfterEach
    void tearDown() {
        MDC.clear(); // Clean up MDC after each test
    }

    @Test
    void testPredefinedPatterns() {
        assertEquals("[%X{spec}:%X{template}]", SLF4JPatternFormatter.resolvePattern("default"));
        assertEquals("[%X{spec}]", SLF4JPatternFormatter.resolvePattern("minimal"));
        assertEquals("[%X{component}] [%X{spec}:%X{template}]", SLF4JPatternFormatter.resolvePattern("verbose"));
        assertEquals("[%X{component}|%X{spec}:%X{template}]", SLF4JPatternFormatter.resolvePattern("debug"));
        assertEquals("[%-8X{spec}:%-15X{template}]", SLF4JPatternFormatter.resolvePattern("aligned"));
        assertEquals("%d{HH:mm:ss} [%X{spec}:%X{template}] %msg", SLF4JPatternFormatter.resolvePattern("timestamped"));
        assertEquals("", SLF4JPatternFormatter.resolvePattern("none"));
        assertEquals("", SLF4JPatternFormatter.resolvePattern("disabled"));
    }

    @Test
    void testPatternResolution() {
        // Custom pattern should be returned as-is
        String customPattern = "[%X{custom}] %msg";
        assertEquals(customPattern, SLF4JPatternFormatter.resolvePattern(customPattern));
        
        // Case insensitive predefined patterns
        assertEquals("[%X{spec}]", SLF4JPatternFormatter.resolvePattern("MINIMAL"));
        assertEquals("[%X{spec}]", SLF4JPatternFormatter.resolvePattern("Minimal"));
        
        // Null/empty patterns
        assertEquals("[%X{spec}:%X{template}]", SLF4JPatternFormatter.resolvePattern(null));
        assertEquals("[%X{spec}:%X{template}]", SLF4JPatternFormatter.resolvePattern(""));
        assertEquals("[%X{spec}:%X{template}]", SLF4JPatternFormatter.resolvePattern("  "));
    }

    @Test
    void testBasicFormatting() {
        // Test using explicit context to avoid MDC isolation issues
        String result = SLF4JPatternFormatter.formatWithContext("default", "Processing customization", 
                                                                        "spring", "pojo.mustache", "CustomizationEngine");
        assertEquals("[spring:pojo.mustache]", result);
        
        result = SLF4JPatternFormatter.formatWithContext("minimal", "Processing customization",
                                                                "spring", "pojo.mustache", "CustomizationEngine");
        assertEquals("[spring]", result);
        
        result = SLF4JPatternFormatter.formatWithContext("verbose", "Processing customization",
                                                                "spring", "pojo.mustache", "CustomizationEngine");
        assertEquals("[CustomizationEngine] [spring:pojo.mustache]", result);
        
        result = SLF4JPatternFormatter.formatWithContext("debug", "Processing customization",
                                                                "spring", "pojo.mustache", "CustomizationEngine");
        assertEquals("[CustomizationEngine|spring:pojo.mustache]", result);
    }

    @Test
    void testFormattingWithMissingContext() {
        // Test with no context using explicit method - more reliable than MDC
        String result = SLF4JPatternFormatter.formatWithContext("default", "Processing customization",
                                                                         null, null, null);
        assertEquals("[:]", result);
        
        result = SLF4JPatternFormatter.formatWithContext("minimal", "Processing customization",
                                                                 null, null, null);
        assertEquals("[]", result);
        
        // Partial context
        result = SLF4JPatternFormatter.formatWithContext("default", "Processing customization",
                                                                 "spring", null, null);
        assertEquals("[spring:]", result);
        
        result = SLF4JPatternFormatter.formatWithContext("default", "Processing customization",
                                                                 "spring", "pojo.mustache", null);
        assertEquals("[spring:pojo.mustache]", result);
    }

    @Test
    void testFormattingWithAlignedPattern() {
        String result = SLF4JPatternFormatter.formatWithContext("aligned", "test",
                                                                        "orders", "enum.mustache", "Component");
        // Should be left-aligned with proper padding
        assertTrue(result.startsWith("[orders "));
        assertTrue(result.contains("enum.mustache"));
        
        // Test with longer names
        result = SLF4JPatternFormatter.formatWithContext("aligned", "test",
                                                                 "verylongspecname", "verylongtemplatename.mustache", "Component");
        // Should still format correctly even when truncation occurs
        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    void testMessageWithArguments() {
        // Test with no context first
        String result = SLF4JPatternFormatter.formatWithContext("[%X{spec}] %msg", "Processing template for spec", 
                                                                          null, null, null);
        assertEquals("[] Processing template for spec", result);
        
        // Test with explicit context - more reliable than MDC in test environment
        result = SLF4JPatternFormatter.formatWithContext("[%X{spec}] %msg", "Processing template for spec",
                                                                  "spring", null, null);
        assertEquals("[spring] Processing template for spec", result);
    }

    @Test
    void testTimestampPattern() {
        String result = SLF4JPatternFormatter.format("timestamped", "test message");
        
        // Should contain timestamp, context, and message
        assertNotNull(result);
        assertTrue(result.contains("test message"));
        assertTrue(result.matches("\\d{2}:\\d{2}:\\d{2} \\[.*\\] test message"));
    }

    @Test
    void testCustomPattern() {
        // Test using explicit context
        String result = SLF4JPatternFormatter.formatWithContext("%X{component} working on %X{spec}: %msg", "custom message",
                                                                         "spring", "template", "Engine");
        assertEquals("Engine working on spring: custom message", result);
    }

    @Test
    void testPatternCaching() {
        assertEquals(0, SLF4JPatternFormatter.getPatternCacheSize());
        assertFalse(SLF4JPatternFormatter.isPatternCached("default"));
        
        // First use should compile and cache
        SLF4JPatternFormatter.format("default", "test");
        assertEquals(1, SLF4JPatternFormatter.getPatternCacheSize());
        assertTrue(SLF4JPatternFormatter.isPatternCached("default"));
        
        // Second use should use cache
        SLF4JPatternFormatter.format("default", "test2");
        assertEquals(1, SLF4JPatternFormatter.getPatternCacheSize());
        
        // Different pattern should add to cache
        SLF4JPatternFormatter.format("minimal", "test3");
        assertEquals(2, SLF4JPatternFormatter.getPatternCacheSize());
        assertTrue(SLF4JPatternFormatter.isPatternCached("minimal"));
        
        // Clear cache
        SLF4JPatternFormatter.clearPatternCache();
        assertEquals(0, SLF4JPatternFormatter.getPatternCacheSize());
        assertFalse(SLF4JPatternFormatter.isPatternCached("default"));
    }

    @Test
    void testMessageFormattingEdgeCases() {
        // Null message
        String result = SLF4JPatternFormatter.format("default", null);
        assertEquals("[:]", result);
        
        // Empty message
        result = SLF4JPatternFormatter.format("default", "");
        assertEquals("[:]", result);
        
        // Message with no placeholders but with args
        result = SLF4JPatternFormatter.format("[%X{spec}] %msg", "no placeholders", "unused", "args");
        assertEquals("[] no placeholders", result);
        
        // More placeholders than args
        result = SLF4JPatternFormatter.format("[%X{spec}] %msg", "Has {} and {} placeholders", "one");
        assertEquals("[] Has one and {} placeholders", result);
        
        // More args than placeholders
        result = SLF4JPatternFormatter.format("[%X{spec}] %msg", "Has {} placeholder", "one", "two", "three");
        assertEquals("[] Has one placeholder", result);
        
        // Null args
        result = SLF4JPatternFormatter.format("[%X{spec}] %msg", "Null arg: {}", (Object) null);
        assertEquals("[] Null arg: null", result);
    }

    @Test
    void testComplexRealWorldPattern() {
        String pattern = "%d{HH:mm:ss.SSS} [%-15X{component}] [%-8X{spec}:%-20X{template}] - %msg";
        String result = SLF4JPatternFormatter.formatWithContext(pattern, "Applied 3 customizations to template",
                                                                        "petstore", "apiController.mustache", "CustomizationEngine");
        
        // Verify structure
        assertTrue(result.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[CustomizationEngine\\s*\\] \\[petstore\\s*:apiController\\.mustache\\s*\\] - Applied 3 customizations to template"));
    }

    @Test
    void testFormatModifierIntegration() {
        // Test truncation using explicit context
        String result = SLF4JPatternFormatter.formatWithContext("[%X{spec}:%.10X{template}]", "test",
                                                                        "a", "verylongtemplatename.mustache", "Component");
        assertEquals("[a:verylongte]", result);
        
        // Test padding
        result = SLF4JPatternFormatter.formatWithContext("[%-5X{spec}:%-10X{template}]", "test",
                                                                 "a", "verylongtemplatename.mustache", "Component");
        assertTrue(result.startsWith("[a    :"));
        assertTrue(result.contains("verylongtemplatename.mustache"));
        assertTrue(result.endsWith("]"));
    }

    @Test
    void testNoFormatPattern() {
        String result = SLF4JPatternFormatter.format("none", "test message");
        assertEquals("", result);
        
        result = SLF4JPatternFormatter.format("disabled", "test message");
        assertEquals("", result);
    }

    @Test
    void testPerformanceCharacteristics() {
        // This test verifies that patterns are cached and reused efficiently
        String pattern = "%d{HH:mm:ss} [%-8X{spec}:%-15X{template}] %msg";
        
        // Warm up cache using explicit context for reliability
        SLF4JPatternFormatter.formatWithContext(pattern, "warmup", "performance", "test.mustache", "Component");
        assertEquals(1, SLF4JPatternFormatter.getPatternCacheSize());
        
        // Multiple calls should reuse cached pattern
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            SLF4JPatternFormatter.formatWithContext(pattern, "Message " + i, "performance", "test.mustache", "Component");
        }
        long elapsed = System.nanoTime() - start;
        
        // Should complete quickly (this is not a strict performance test, just a sanity check)
        assertTrue(elapsed < 100_000_000); // 100ms for 100 calls should be easily achievable
        assertEquals(1, SLF4JPatternFormatter.getPatternCacheSize()); // Still just one cached pattern
    }
}