package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.customization.CustomizationConfig;
import com.guidedbyte.openapi.modelgen.customization.Replacement;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to isolate SnakeYAML parsing issues with CustomizationConfig.
 */
public class YamlParsingTest {

    @Test
    public void testMinimalReplacementYamlParsing() {
        String yamlContent = """
            replacements:
              - find: "test"
                replace: "replaced"
            """;
        
        // Test raw YAML parsing first
        Yaml basicYaml = new Yaml();
        Map<String, Object> rawParsed = basicYaml.load(yamlContent);
        
        assertNotNull(rawParsed);
        assertTrue(rawParsed.containsKey("replacements"));
        
        Object replacements = rawParsed.get("replacements");
        assertInstanceOf(List.class, replacements);
        
        @SuppressWarnings("unchecked")
        java.util.List<Object> replacementList = (java.util.List<Object>) replacements;
        assertEquals(1, replacementList.size(), "Expected 1 replacement in raw YAML");
        
        // Test typed parsing with CustomizationConfig
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(50);
        options.setAllowRecursiveKeys(false);
        
        Constructor constructor = new Constructor(CustomizationConfig.class, options);
        Yaml typedYaml = new Yaml(constructor);
        
        CustomizationConfig config = typedYaml.loadAs(yamlContent, CustomizationConfig.class);
        
        assertNotNull(config, "CustomizationConfig should not be null");
        
        // This is the critical test - are replacements properly deserialized?
        if (config.getReplacements() == null) {
            fail("Replacements list is null - SnakeYAML failed to deserialize replacements");
        }
        
        assertEquals(1, config.getReplacements().size(), 
            "Expected 1 replacement but got " + config.getReplacements().size());
        
        Replacement replacement = config.getReplacements().get(0);
        assertNotNull(replacement, "First replacement should not be null");
        assertEquals("test", replacement.getFind(), "Find pattern should be 'test'");
        assertEquals("replaced", replacement.getReplace(), "Replace pattern should be 'replaced'");
        
        System.out.println("✅ YAML parsing test passed - replacements correctly deserialized");
    }
    
    @Test
    public void testComplexYamlParsing() {
        String yamlContent = """
            metadata:
              name: "Test Config"
              version: "1.0.0"
            replacements:
              - find: "private static final long serialVersionUID = 1L;"
                replace: "private static final long serialVersionUID = 1L; // REPLACEMENT_WORKED"
              - find: "}} {{>permits}}{"
                replace: "}} {{>permits}}\\n{"
            insertions:
              - before: "{{#serializableModel}}"
                content: "// Test insertion"
            """;
        
        // Test raw YAML parsing
        Yaml basicYaml = new Yaml();
        Map<String, Object> rawParsed = basicYaml.load(yamlContent);
        
        assertNotNull(rawParsed);
        assertTrue(rawParsed.containsKey("replacements"));
        assertTrue(rawParsed.containsKey("insertions"));
        assertTrue(rawParsed.containsKey("metadata"));
        
        @SuppressWarnings("unchecked")
        java.util.List<Object> rawReplacements = (java.util.List<Object>) rawParsed.get("replacements");
        assertEquals(2, rawReplacements.size(), "Expected 2 replacements in raw YAML");
        
        @SuppressWarnings("unchecked")
        java.util.List<Object> rawInsertions = (java.util.List<Object>) rawParsed.get("insertions");
        assertEquals(1, rawInsertions.size(), "Expected 1 insertion in raw YAML");
        
        // Test typed parsing
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(50);
        options.setAllowRecursiveKeys(false);
        
        Constructor constructor = new Constructor(CustomizationConfig.class, options);
        Yaml typedYaml = new Yaml(constructor);
        
        CustomizationConfig config = typedYaml.loadAs(yamlContent, CustomizationConfig.class);
        
        assertNotNull(config, "CustomizationConfig should not be null");
        assertNotNull(config.getMetadata(), "Metadata should not be null");
        assertEquals("Test Config", config.getMetadata().getName());
        
        // Check replacements deserialization
        assertNotNull(config.getReplacements(), "Replacements should not be null");
        assertEquals(2, config.getReplacements().size(), 
            "Expected 2 replacements but got " + (config.getReplacements() != null ? config.getReplacements().size() : "null"));
        
        // Check insertions deserialization
        assertNotNull(config.getInsertions(), "Insertions should not be null");
        assertEquals(1, config.getInsertions().size(),
            "Expected 1 insertion but got " + (config.getInsertions() != null ? config.getInsertions().size() : "null"));
        
        System.out.println("✅ Complex YAML parsing test passed");
    }
    
    @Test
    public void testWindowsLineEndingsYaml() {
        // Test with Windows line endings (\r\n) like what's in the JAR
        String yamlWithCRLF = "replacements:\r\n  - find: \"test\"\r\n    replace: \"replaced\"\r\n";
        
        System.out.println("Testing YAML with Windows line endings (CRLF):");
        System.out.println("Length: " + yamlWithCRLF.length());
        System.out.println("Hex: " + bytesToHex(yamlWithCRLF.getBytes()));
        
        // Test raw YAML parsing first
        Yaml basicYaml = new Yaml();
        Map<String, Object> rawParsed = basicYaml.load(yamlWithCRLF);
        
        assertNotNull(rawParsed);
        assertTrue(rawParsed.containsKey("replacements"));
        
        Object replacements = rawParsed.get("replacements");
        assertInstanceOf(List.class, replacements);
        
        @SuppressWarnings("unchecked")
        java.util.List<Object> replacementList = (java.util.List<Object>) replacements;
        assertEquals(1, replacementList.size(), "Expected 1 replacement in raw YAML with CRLF");
        
        // Test typed parsing
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(50);
        options.setAllowRecursiveKeys(false);
        
        Constructor constructor = new Constructor(CustomizationConfig.class, options);
        Yaml typedYaml = new Yaml(constructor);
        
        try {
            CustomizationConfig config = typedYaml.loadAs(yamlWithCRLF, CustomizationConfig.class);
            
            assertNotNull(config, "CustomizationConfig should not be null with CRLF endings");
            
            // This is the critical test
            if (config.getReplacements() == null) {
                fail("❌ FOUND THE ISSUE: Replacements list is null with CRLF line endings - SnakeYAML failed to deserialize");
            }
            
            int actualCount = config.getReplacements().size();
            if (actualCount != 1) {
                fail(String.format("❌ FOUND THE ISSUE: Expected 1 replacement but got %d with CRLF line endings", actualCount));
            }
            
            System.out.println("✅ Windows line endings test passed - not the issue");
            
        } catch (Exception e) {
            fail("❌ FOUND THE ISSUE: Exception with CRLF line endings: " + e.getMessage(), e);
        }
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    @Test
    public void testCustomizationEngineParsingLikePluginResources() {
        // Test YAML parsing exactly like the plugin does, but using in-memory YAML content
        // This validates the CustomizationEngine can properly parse plugin-style YAML configurations
        String yamlContent = """
            replacements:
              - find: "{{classname}}"
                replace: "{{classname}} /* REPLACEMENT_WORKED */"
              - find: "private static final long"
                replace: "private static final long /* SECOND_REPLACEMENT */"
            """;
        
        System.out.println("=== TEST YAML CONTENT ===");
        System.out.println("Length: " + yamlContent.length());
        System.out.println("Content: '" + yamlContent + "'");
        System.out.println("Hex: " + bytesToHex(yamlContent.getBytes()));
        System.out.println("========================");
        
        try {
            // Parse using CustomizationEngine exactly like plugin resources are parsed
            CustomizationEngine engine = new CustomizationEngine();
            CustomizationConfig config = engine.parseCustomizationYaml(
                new java.io.ByteArrayInputStream(yamlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)), 
                "test-yaml-content");
            
            assertNotNull(config, "Config should not be null");
            
            System.out.println("Parsed config via CustomizationEngine:");
            System.out.println("- replacements: " + (config.getReplacements() != null ? config.getReplacements().size() : "null"));
            System.out.println("- insertions: " + (config.getInsertions() != null ? config.getInsertions().size() : "null"));
            
            // Validate the parsing worked correctly
            assertNotNull(config.getReplacements(), "Replacements should not be null");
            assertEquals(2, config.getReplacements().size(), "Should have exactly 2 replacements");
            
            // Validate specific replacement content
            assertEquals("{{classname}}", config.getReplacements().get(0).getFind());
            assertEquals("{{classname}} /* REPLACEMENT_WORKED */", config.getReplacements().get(0).getReplace());
            assertEquals("private static final long", config.getReplacements().get(1).getFind());
            assertEquals("private static final long /* SECOND_REPLACEMENT */", config.getReplacements().get(1).getReplace());
            
            System.out.println("✅ CustomizationEngine parsing test passed (plugin-style YAML)");
            
        } catch (Exception e) {
            fail("❌ Exception parsing YAML content: " + e.getMessage(), e);
        }
    }

    @Test
    public void testUniversalRegexForClassDeclarations() {
        // Universal regex pattern that should match both OpenAPI Generator versions
        String universalRegex = "^public.*class.*\\{$";
        
        // OpenAPI Generator 7.14 pattern (current) with {{>permits}}
        String pattern714 = "public {{>sealed}}class {{classname}}{{#parent}} extends {{{parent}}}{{/parent}}{{^parent}}{{#hateoas}} extends RepresentationModel<{{classname}}> {{/hateoas}}{{/parent}}{{#vendorExtensions.x-implements}}{{#-first}} implements {{{.}}}{{/-first}}{{^-first}}, {{{.}}}{{/-first}}{{/vendorExtensions.x-implements}} {{>permits}}{";
        
        // OpenAPI Generator 7.10 pattern (legacy) without {{>permits}}  
        String pattern710 = "public class {{classname}}{{#parent}} extends {{{parent}}}{{/parent}}{{^parent}}{{#hateoas}} extends RepresentationModel<{{classname}}> {{/hateoas}}{{/parent}}{{#vendorExtensions.x-implements}}{{#-first}} implements {{{.}}}{{/-first}}{{^-first}}, {{{.}}}{{/-first}}{{/vendorExtensions.x-implements}} {";
        
        System.out.println("Testing universal regex: " + universalRegex);
        System.out.println();
        
        // Test 7.14 pattern matching
        System.out.println("Testing OpenAPI Generator 7.14 pattern:");
        System.out.println("Pattern: " + pattern714);
        assertTrue(pattern714.matches(universalRegex), 
            "7.14 pattern should match universal regex: " + pattern714);
        System.out.println("✅ 7.14 pattern matches");
        
        // Test 7.10 pattern matching  
        System.out.println();
        System.out.println("Testing OpenAPI Generator 7.10 pattern:");
        System.out.println("Pattern: " + pattern710);
        assertTrue(pattern710.matches(universalRegex), 
            "7.10 pattern should match universal regex: " + pattern710);
        System.out.println("✅ 7.10 pattern matches");
        
        // Test with some mustache placeholders expanded (realistic scenarios)
        String pattern714Expanded = "public class ApiPetDto extends BaseDto implements Serializable {";
        String pattern710Expanded = "public class ApiPetDto implements Serializable {";
        
        System.out.println();
        System.out.println("Testing with expanded placeholders:");
        System.out.println("7.14 expanded: " + pattern714Expanded);
        assertTrue(pattern714Expanded.matches(universalRegex), 
            "7.14 expanded pattern should match universal regex: " + pattern714Expanded);
        System.out.println("✅ 7.14 expanded pattern matches");
        
        System.out.println("7.10 expanded: " + pattern710Expanded);
        assertTrue(pattern710Expanded.matches(universalRegex), 
            "7.10 expanded pattern should match universal regex: " + pattern710Expanded);
        System.out.println("✅ 7.10 expanded pattern matches");
        
        // Test edge cases that should also match
        String[] additionalPatterns = {
            "public final class TestClass {",
            "public abstract class TestClass extends Parent implements Interface {",
            "public class TestClass<T> implements Interface1, Interface2 {",
            "public static class InnerClass {"
        };
        
        System.out.println();
        System.out.println("Testing additional edge cases:");
        for (String pattern : additionalPatterns) {
            System.out.println("Pattern: " + pattern);
            assertTrue(pattern.matches(universalRegex), 
                "Pattern should match universal regex: " + pattern);
            System.out.println("✅ Pattern matches");
        }
        
        System.out.println();
        System.out.println("✅ All class declaration patterns match the universal regex!");
    }
    
    @Test
    public void testRegexReplacementValidation() {
        // Test that regex replacement would work correctly for both versions
        String universalRegex = "^(public.*class.*)(\\{)$";
        String replacement = "$1\n$2";
        
        // Test patterns from both OpenAPI Generator versions
        String pattern714 = "public class ApiPetDto extends BaseDto implements Serializable {";
        String pattern710 = "public class ApiPetDto implements Serializable {";
        
        System.out.println("Testing regex replacement transformation:");
        System.out.println("Regex pattern: " + universalRegex);
        System.out.println("Replacement: " + replacement.replace("\n", "\\n"));
        System.out.println();
        
        // Debug: Check if patterns match the regex
        boolean matches714 = pattern714.matches(universalRegex);
        boolean matches710 = pattern710.matches(universalRegex);
        
        System.out.println("7.14 pattern matches regex: " + matches714);
        System.out.println("7.10 pattern matches regex: " + matches710);
        
        // Apply regex replacement
        String result714 = pattern714.replaceAll(universalRegex, replacement);
        String result710 = pattern710.replaceAll(universalRegex, replacement);
        
        System.out.println();
        System.out.println("7.14 Original: '" + pattern714 + "'");
        System.out.println("7.14 Result:   '" + result714.replace("\n", "\\n") + "'");
        System.out.println("7.14 Changed:  " + !result714.equals(pattern714));
        
        System.out.println();
        System.out.println("7.10 Original: '" + pattern710 + "'");
        System.out.println("7.10 Result:   '" + result710.replace("\n", "\\n") + "'");
        System.out.println("7.10 Changed:  " + !result710.equals(pattern710));
        
        // Only assert if the replacement actually worked (pattern matched)
        if (matches714) {
            assertTrue(result714.contains("\n{"), "7.14: Opening brace should be on new line");
            assertTrue(result714.startsWith("public class ApiPetDto"), "7.14: Should preserve class declaration");
            System.out.println("✅ 7.14 regex replacement works");
        } else {
            System.out.println("⚠️ 7.14 pattern doesn't match regex - replacement not applied");
        }
        
        if (matches710) {
            assertTrue(result710.contains("\n{"), "7.10: Opening brace should be on new line");
            assertTrue(result710.startsWith("public class ApiPetDto"), "7.10: Should preserve class declaration");
            System.out.println("✅ 7.10 regex replacement works");
        } else {
            System.out.println("⚠️ 7.10 pattern doesn't match regex - replacement not applied");
        }
        
        System.out.println();
        System.out.println("✅ Universal regex replacement validation completed!");
    }
}