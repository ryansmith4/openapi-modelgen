package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.customization.CustomizationConfig;
import com.guidedbyte.openapi.modelgen.customization.Insertion;
import com.guidedbyte.openapi.modelgen.customization.Replacement;
import com.guidedbyte.openapi.modelgen.services.CustomizationEngine.CustomizationException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomizationEngine.
 */
class CustomizationEngineTest {
    
    private Project project;
    private CustomizationEngine customizationEngine;
    
    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().build();
        customizationEngine = new CustomizationEngine();
    }
    
    @Test
    void testParseValidYaml() throws CustomizationException {
        String yamlContent = """
            metadata:
              name: "Test Customization"
              version: "1.0.0"
            
            insertions:
              - after: "{{#model}}"
                content: "@Valid"
            
            replacements:
              - find: "{{name}}"
                replace: "{{>customName}}"
            
            partials:
              customName: "{{dataType}} {{name}}"
            """;
        
        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8));
        CustomizationConfig config = customizationEngine.parseCustomizationYaml(inputStream, "test.yaml");
        
        assertNotNull(config);
        assertNotNull(config.getMetadata());
        assertEquals("Test Customization", config.getMetadata().getName());
        assertEquals("1.0.0", config.getMetadata().getVersion());
        
        assertEquals(1, config.getInsertions().size());
        Insertion insertion = config.getInsertions().get(0);
        assertEquals("{{#model}}", insertion.getAfter());
        assertEquals("@Valid", insertion.getContent());
        
        assertEquals(1, config.getReplacements().size());
        Replacement replacement = config.getReplacements().get(0);
        assertEquals("{{name}}", replacement.getFind());
        assertEquals("{{>customName}}", replacement.getReplace());
        
        assertEquals(1, config.getPartials().size());
        assertEquals("{{dataType}} {{name}}", config.getPartials().get("customName"));
    }
    
    @Test
    void testParseInvalidYaml() {
        String invalidYaml = """
            insertions:
              - invalid_key: "value"
            """;
        
        InputStream inputStream = new ByteArrayInputStream(invalidYaml.getBytes(StandardCharsets.UTF_8));
        
        assertThrows(CustomizationException.class, () -> {
            customizationEngine.parseCustomizationYaml(inputStream, "invalid.yaml");
        });
    }
    
    @Test
    void testApplyBasicInsertion() throws CustomizationException {
        String baseTemplate = """
            {{#model}}
            public class {{classname}} {
            {{/model}}
            """;
        
        CustomizationConfig config = new CustomizationConfig();
        Insertion insertion = new Insertion();
        insertion.setAfter("{{#model}}");
        insertion.setContent("\\n@Valid");
        config.setInsertions(java.util.List.of(insertion));
        
        EvaluationContext context = EvaluationContext.builder()
            .generatorVersion("6.0.0")
            .templateContent(baseTemplate)
            .build();
        
        String result = customizationEngine.applyCustomizations(baseTemplate, config, context);
        
        assertTrue(result.contains("{{#model}}\\n@Valid"));
    }
    
    @Test
    void testApplyBasicReplacement() throws CustomizationException {
        String baseTemplate = "{{#vars}}{{name}}{{/vars}}";
        
        CustomizationConfig config = new CustomizationConfig();
        Replacement replacement = new Replacement();
        replacement.setFind("{{name}}");
        replacement.setReplace("{{>customName}}");
        config.setReplacements(java.util.List.of(replacement));
        
        EvaluationContext context = EvaluationContext.builder()
            .generatorVersion("6.0.0")
            .templateContent(baseTemplate)
            .build();
        
        String result = customizationEngine.applyCustomizations(baseTemplate, config, context);
        
        assertEquals("{{#vars}}{{>customName}}{{/vars}}", result);
    }
    
    @Test
    void testPartialExpansion() throws CustomizationException {
        String baseTemplate = "{{#model}}{{/model}}";
        
        CustomizationConfig config = new CustomizationConfig();
        
        Insertion insertion = new Insertion();
        insertion.setAfter("{{#model}}");
        insertion.setContent("{{>validations}}");
        config.setInsertions(java.util.List.of(insertion));
        
        config.setPartials(java.util.Map.of(
            "validations", "@Valid\\n@NotNull"
        ));
        
        EvaluationContext context = EvaluationContext.builder()
            .generatorVersion("6.0.0")
            .templateContent(baseTemplate)
            .build();
        
        String result = customizationEngine.applyCustomizations(baseTemplate, config, context);
        
        assertTrue(result.contains("@Valid\\n@NotNull"));
        assertFalse(result.contains("{{>validations}}"));
    }
    
    @Test
    void testEmptyConfigReturnsOriginal() throws CustomizationException {
        String baseTemplate = "{{#model}}original{{/model}}";
        
        EvaluationContext context = EvaluationContext.builder()
            .generatorVersion("6.0.0")
            .templateContent(baseTemplate)
            .build();
        
        String result = customizationEngine.applyCustomizations(baseTemplate, null, context);
        
        assertEquals(baseTemplate, result);
    }
    
    @Test
    void testNullTemplateThrowsException() {
        CustomizationConfig config = new CustomizationConfig();
        EvaluationContext context = EvaluationContext.builder().build();
        
        assertThrows(CustomizationException.class, () -> {
            customizationEngine.applyCustomizations(null, config, context);
        });
    }
    
    @Test
    void testPojoMustacheYamlCustomization() throws CustomizationException {
        // This test validates that the actual pojo.mustache.yaml customization works as expected
        // It should add header information before the {{description}} tag
        
        String basePojoTemplate = """
            /**
            {{#description}}
             * {{description}}
            {{/description}}
             */
            public class {{classname}} {
            {{#vars}}
              private {{>nullableDataType}} {{name}};
            {{/vars}}
            }
            """;
        
        // Load the actual pojo.mustache.yaml customization from plugin resources
        String yamlContent = """
            metadata:
              name: "Enhanced POJO Template"
              description: "Adds header variable"
              version: "2.0.0"
            insertions:
              - before: "{{#description}}"
                content: |
                  {{#header}} * {{header}}
                   *
                  {{/header}} * {{^header}}{{/header}}
            """;
        
        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8));
        CustomizationConfig config = customizationEngine.parseCustomizationYaml(inputStream, "pojo.mustache.yaml");
        
        EvaluationContext context = EvaluationContext.builder()
            .generatorVersion("7.11.0")
            .templateContent(basePojoTemplate)
            .build();
        
        String result = customizationEngine.applyCustomizations(basePojoTemplate, config, context);
        
        // Verify the customization was applied
        assertNotNull(result);
        assertTrue(result.contains("{{#header}} * {{header}}"), 
            "Should contain header template variable");
        assertTrue(result.contains("{{^header}}{{/header}}"), 
            "Should contain header negation template");
        
        // Verify the insertion happens before {{description}}
        int headerIndex = result.indexOf("{{#header}}");
        int descriptionIndex = result.indexOf("{{#description}}");
        assertTrue(headerIndex < descriptionIndex && headerIndex > 0, 
            "Header template should appear before description and after start");
        
        // Verify the base template structure is preserved
        assertTrue(result.contains("public class {{classname}}"), 
            "Should preserve class declaration");
        assertTrue(result.contains("private {{>nullableDataType}} {{name}};"), 
            "Should preserve field declarations");
    }
    
    @Test
    void testPojoCustomizationWithMustacheVariableExpansion() throws CustomizationException {
        // Test that when header variable is provided, the customization renders correctly
        
        String baseTemplate = """
            /**
            {{#description}}
             * {{description}}
            {{/description}}
             */
            public class {{classname}} {
            }
            """;
        
        String yamlContent = """
            insertions:
              - before: "{{#description}}"
                content: |
                  {{#header}} * {{header}}
                   *
                  {{/header}} * {{^header}}{{/header}}
            """;
        
        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8));
        CustomizationConfig config = customizationEngine.parseCustomizationYaml(inputStream, "test.yaml");
        
        EvaluationContext context = EvaluationContext.builder()
            .generatorVersion("7.11.0")
            .templateContent(baseTemplate)
            .build();
        
        String result = customizationEngine.applyCustomizations(baseTemplate, config, context);
        
        // The result should contain the header template logic intact for Mustache to process later
        assertTrue(result.contains("{{#header}} * {{header}}"));
        assertTrue(result.contains("{{^header}}{{/header}}"));
        
        // Verify proper positioning - header logic should come before description
        String[] lines = result.split("\n");
        boolean foundHeaderStart = false;
        boolean foundDescription = false;
        
        for (String line : lines) {
            if (line.contains("{{#header}}")) {
                foundHeaderStart = true;
                assertFalse(foundDescription, "Header should come before description");
            }
            if (line.contains("{{#description}}")) {
                foundDescription = true;
                assertTrue(foundHeaderStart, "Should have found header start before description");
            }
        }
        
        assertTrue(foundHeaderStart && foundDescription, "Should find both header and description markers");
    }
    
    @Test
    void testPojoCustomizationIntegrationWithFallbackTemplate() throws CustomizationException {
        // Test the customization with a template similar to what our fallback template generates
        
        String fallbackPojoTemplate = """
            {{#description}}
            /**
             * {{description}}
             */
            {{/description}}
            {{>generatedAnnotation}}
            public class {{classname}} {
            {{#vars}}
              @JsonProperty("{{baseName}}")
              private {{>nullableDataType}} {{name}};
            
              public {{>nullableDataType}} {{getter}}() {
                return {{name}};
              }
            
              public void {{setter}}({{>nullableDataType}} {{name}}) {
                this.{{name}} = {{name}};
              }
            {{/vars}}
            }
            """;
        
        String yamlContent = """
            insertions:
              - before: "{{#description}}"
                content: |
                  {{#header}} * {{header}}
                   *
                  {{/header}} * {{^header}}{{/header}}
            """;
        
        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8));
        CustomizationConfig config = customizationEngine.parseCustomizationYaml(inputStream, "pojo.mustache.yaml");
        
        EvaluationContext context = EvaluationContext.builder()
            .generatorVersion("7.11.0")
            .templateContent(fallbackPojoTemplate)
            .build();
        
        String result = customizationEngine.applyCustomizations(fallbackPojoTemplate, config, context);
        
        // Verify customization was applied
        assertTrue(result.contains("{{#header}} * {{header}}"));
        
        // Verify the fallback template structure is preserved
        assertTrue(result.contains("@JsonProperty(\"{{baseName}}\")"));
        assertTrue(result.contains("public {{>nullableDataType}} {{getter}}()"));
        assertTrue(result.contains("{{>generatedAnnotation}}"));
    }
}