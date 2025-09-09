package com.guidedbyte.openapi.modelgen.constants;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TemplateSourceType enum to verify precedence ordering.
 */
class TemplateSourceTypeTest {

    @Test
    void testPrecedenceOrdering() {
        // Given: All template source types
        List<TemplateSourceType> allTypes = TemplateSourceType.getAllInPrecedenceOrder();
        
        // Then: Should be in correct precedence order
        assertEquals(6, allTypes.size());
        assertEquals(TemplateSourceType.USER_TEMPLATES, allTypes.get(0));
        assertEquals(TemplateSourceType.USER_CUSTOMIZATIONS, allTypes.get(1));
        assertEquals(TemplateSourceType.LIBRARY_TEMPLATES, allTypes.get(2));
        assertEquals(TemplateSourceType.LIBRARY_CUSTOMIZATIONS, allTypes.get(3));
        assertEquals(TemplateSourceType.PLUGIN_CUSTOMIZATIONS, allTypes.get(4));
        assertEquals(TemplateSourceType.OPENAPI_GENERATOR, allTypes.get(5));
    }

    @Test
    void testGetAllAsStringsPreservesOrder() {
        // Given: All template sources as strings
        List<String> allStrings = TemplateSourceType.getAllAsStrings();
        
        // Then: Should be in precedence order
        assertEquals(6, allStrings.size());
        assertEquals("user-templates", allStrings.get(0));
        assertEquals("user-customizations", allStrings.get(1));
        assertEquals("library-templates", allStrings.get(2));
        assertEquals("library-customizations", allStrings.get(3));
        assertEquals("plugin-customizations", allStrings.get(4));
        assertEquals("openapi-generator", allStrings.get(5));
    }

    @Test
    void testToStringListSortsByPrecedence() {
        // Given: Unsorted list of template source types
        List<TemplateSourceType> unsorted = Arrays.asList(
            TemplateSourceType.OPENAPI_GENERATOR,
            TemplateSourceType.USER_TEMPLATES,
            TemplateSourceType.PLUGIN_CUSTOMIZATIONS,
            TemplateSourceType.LIBRARY_TEMPLATES
        );
        
        // When: Converting to string list
        List<String> sorted = TemplateSourceType.toStringList(unsorted);
        
        // Then: Should be sorted by precedence
        assertEquals(4, sorted.size());
        assertEquals("user-templates", sorted.get(0));
        assertEquals("library-templates", sorted.get(1));
        assertEquals("plugin-customizations", sorted.get(2));
        assertEquals("openapi-generator", sorted.get(3));
    }

    @Test
    void testPrecedenceComparison() {
        // Given: Different template source types
        TemplateSourceType userTemplates = TemplateSourceType.USER_TEMPLATES;
        TemplateSourceType pluginCustomizations = TemplateSourceType.PLUGIN_CUSTOMIZATIONS;
        TemplateSourceType openApiGenerator = TemplateSourceType.OPENAPI_GENERATOR;
        
        // Then: Higher priority sources should have higher precedence
        assertTrue(userTemplates.hasHigherPrecedenceThan(pluginCustomizations));
        assertTrue(pluginCustomizations.hasHigherPrecedenceThan(openApiGenerator));
        assertFalse(openApiGenerator.hasHigherPrecedenceThan(userTemplates));
    }

    @Test
    void testPrecedenceValues() {
        // Given: All template source types
        // Then: Should have correct precedence values
        assertEquals(1, TemplateSourceType.USER_TEMPLATES.getPrecedence());
        assertEquals(2, TemplateSourceType.USER_CUSTOMIZATIONS.getPrecedence());
        assertEquals(3, TemplateSourceType.LIBRARY_TEMPLATES.getPrecedence());
        assertEquals(4, TemplateSourceType.LIBRARY_CUSTOMIZATIONS.getPrecedence());
        assertEquals(5, TemplateSourceType.PLUGIN_CUSTOMIZATIONS.getPrecedence());
        assertEquals(6, TemplateSourceType.OPENAPI_GENERATOR.getPrecedence());
    }

    @Test
    void testToStringReturnsValue() {
        // Given: Template source types
        // Then: toString should return the configuration value
        assertEquals("user-templates", TemplateSourceType.USER_TEMPLATES.toString());
        assertEquals("plugin-customizations", TemplateSourceType.PLUGIN_CUSTOMIZATIONS.toString());
        assertEquals("openapi-generator", TemplateSourceType.OPENAPI_GENERATOR.toString());
    }

    @Test
    void testFromValueConversion() {
        // Given: String values
        // Then: Should convert correctly
        assertEquals(TemplateSourceType.USER_TEMPLATES, TemplateSourceType.fromValue("user-templates"));
        assertEquals(TemplateSourceType.OPENAPI_GENERATOR, TemplateSourceType.fromValue("openapi-generator"));
        
        // And: Should throw exception for invalid values
        assertThrows(IllegalArgumentException.class, () -> 
            TemplateSourceType.fromValue("invalid-source"));
    }

    @Test
    void testIsValidMethod() {
        // Given: Valid and invalid template source strings
        // Then: Should correctly identify valid values
        assertTrue(TemplateSourceType.isValid("user-templates"));
        assertTrue(TemplateSourceType.isValid("openapi-generator"));
        assertFalse(TemplateSourceType.isValid("invalid-source"));
        assertFalse(TemplateSourceType.isValid(null));
    }
}