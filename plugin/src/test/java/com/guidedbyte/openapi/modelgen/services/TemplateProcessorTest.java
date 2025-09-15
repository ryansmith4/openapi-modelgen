package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.customization.*;
import com.guidedbyte.openapi.modelgen.services.CustomizationEngine.CustomizationException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TemplateProcessor.
 */
class TemplateProcessorTest {

    private TemplateProcessor templateProcessor;
    private EvaluationContext context;
    
    @BeforeEach
    void setUp() {
        Project project = ProjectBuilder.builder().build();
        ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
        templateProcessor = new TemplateProcessor(conditionEvaluator);
        
        context = EvaluationContext.builder()
            .generatorVersion("6.0.0")
            .templateContent("{{#model}}test{{/model}}")
            .build();
    }
    
    @Test
    void testApplyInsertionAfter() throws CustomizationException {
        String template = "{{#model}}\\npublic class Test {\\n{{/model}}";
        
        Insertion insertion = new Insertion();
        insertion.setAfter("{{#model}}");
        insertion.setContent("\\n@Valid");
        
        String result = templateProcessor.applyInsertion(template, insertion, context, Map.of());
        
        assertTrue(result.contains("{{#model}}\\n@Valid"));
    }
    
    @Test
    void testApplyInsertionBefore() throws CustomizationException {
        String template = "{{#model}}\\npublic class Test {\\n{{/model}}";
        
        Insertion insertion = new Insertion();
        insertion.setBefore("{{/model}}");
        insertion.setContent("\\n}");
        
        String result = templateProcessor.applyInsertion(template, insertion, context, Map.of());
        
        // The result should contain the inserted content before the target pattern
        assertTrue(result.contains("\\n}{{/model}}"), 
            "Result should contain the inserted content before {{/model}}. Got: " + result);
    }
    
    @Test
    void testApplyInsertionAtStart() throws CustomizationException {
        String template = "{{#model}}content{{/model}}";
        
        Insertion insertion = new Insertion();
        insertion.setAt("start");
        insertion.setContent("// Header\\n");
        
        String result = templateProcessor.applyInsertion(template, insertion, context, Map.of());
        
        assertTrue(result.startsWith("// Header\\n"));
    }
    
    @Test
    void testApplyInsertionAtEnd() throws CustomizationException {
        String template = "{{#model}}content{{/model}}";
        
        Insertion insertion = new Insertion();
        insertion.setAt("end");
        insertion.setContent("\\n// Footer");
        
        String result = templateProcessor.applyInsertion(template, insertion, context, Map.of());
        
        assertTrue(result.endsWith("\\n// Footer"));
    }
    
    @Test
    void testApplyReplacementString() throws CustomizationException {
        String template = "{{#vars}}{{name}}{{/vars}}";
        
        Replacement replacement = new Replacement();
        replacement.setFind("{{name}}");
        replacement.setReplace("{{>customName}}");
        replacement.setType("string");
        
        String result = templateProcessor.applyReplacement(template, replacement, context, Map.of());
        
        assertEquals("{{#vars}}{{>customName}}{{/vars}}", result);
    }
    
    @Test
    void testApplyReplacementRegex() throws CustomizationException {
        String template = "public class TestClass extends BaseClass";
        
        Replacement replacement = new Replacement();
        replacement.setFind("class (\\w+) extends");
        replacement.setReplace("class $1 implements Serializable extends");
        replacement.setType("regex");
        
        String result = templateProcessor.applyReplacement(template, replacement, context, Map.of());
        
        assertTrue(result.contains("class TestClass implements Serializable extends"));
    }
    
    @Test
    void testApplySmartReplacementFindAny() throws CustomizationException {
        String template = "{{#hasValidation}}@Valid{{/hasValidation}}";
        
        SmartReplacement smartReplacement = new SmartReplacement();
        smartReplacement.setFindAny(List.of(
            "{{#validation}}", 
            "{{#hasValidation}}", 
            "{{#isValid}}"
        ));
        smartReplacement.setReplace("{{>customValidation}}");
        
        String result = templateProcessor.applySmartReplacement(template, smartReplacement, context, Map.of());
        
        assertTrue(result.contains("{{>customValidation}}"));
    }
    
    @Test
    void testApplySmartInsertionWithFindInsertionPoint() throws CustomizationException {
        String template = "{{#apiDocumentationUrl}}docs{{/apiDocumentationUrl}}\\n{{#baseName}}test{{/baseName}}";
        
        SmartInsertion.PatternLocation location1 = new SmartInsertion.PatternLocation();
        location1.setAfter("{{#apiDocumentationUrl}}docs{{/apiDocumentationUrl}}");
        
        SmartInsertion.PatternLocation location2 = new SmartInsertion.PatternLocation();
        location2.setAfter("{{#baseName}}test{{/baseName}}");
        
        SmartInsertion.InsertionPoint insertionPoint = new SmartInsertion.InsertionPoint();
        insertionPoint.setPatterns(List.of(location1, location2));
        
        SmartInsertion smartInsertion = new SmartInsertion();
        smartInsertion.setFindInsertionPoint(insertionPoint);
        smartInsertion.setContent("\\n{{>customMetadata}}");
        
        String result = templateProcessor.applySmartInsertion(template, smartInsertion, context, Map.of());
        
        // Should use the first matching pattern
        assertTrue(result.contains("{{#apiDocumentationUrl}}docs{{/apiDocumentationUrl}}\\n{{>customMetadata}}"));
    }
    
    @Test
    void testPartialExpansionInInsertion() throws CustomizationException {
        String template = "{{#model}}{{/model}}";
        
        Insertion insertion = new Insertion();
        insertion.setAfter("{{#model}}");
        insertion.setContent("{{>validations}}");
        
        Map<String, String> partials = Map.of(
            "validations", "@Valid\\n@NotNull"
        );
        
        String result = templateProcessor.applyInsertion(template, insertion, context, partials);
        
        assertTrue(result.contains("@Valid\\n@NotNull"));
        assertFalse(result.contains("{{>validations}}"));
    }
    
    @Test
    void testPartialExpansionInReplacement() throws CustomizationException {
        String template = "{{name}}";
        
        Replacement replacement = new Replacement();
        replacement.setFind("{{name}}");
        replacement.setReplace("{{>enhancedName}}");
        
        Map<String, String> partials = Map.of(
            "enhancedName", "{{dataType}} {{name}}"
        );
        
        String result = templateProcessor.applyReplacement(template, replacement, context, partials);
        
        assertEquals("{{dataType}} {{name}}", result);
    }
    
    @Test
    void testInsertionWithConditions() throws CustomizationException {
        String template = "{{#model}}{{/model}}";
        
        ConditionSet conditions = new ConditionSet();
        conditions.setGeneratorVersion(">= 6.0.0");
        
        Insertion insertion = new Insertion();
        insertion.setAfter("{{#model}}");
        insertion.setContent("@Valid");
        insertion.setConditions(conditions);
        
        String result = templateProcessor.applyInsertion(template, insertion, context, Map.of());
        
        assertTrue(result.contains("@Valid"));
    }
    
    @Test
    void testInsertionWithFailingConditions() throws CustomizationException {
        String template = "{{#model}}{{/model}}";
        
        ConditionSet conditions = new ConditionSet();
        conditions.setGeneratorVersion(">= 10.0.0"); // This will fail
        
        Insertion insertion = new Insertion();
        insertion.setAfter("{{#model}}");
        insertion.setContent("@Valid");
        insertion.setConditions(conditions);
        
        String result = templateProcessor.applyInsertion(template, insertion, context, Map.of());
        
        // Should return original template since condition failed
        assertEquals(template, result);
    }
    
    @Test
    void testInsertionWithFallback() throws CustomizationException {
        String template = "{{#model}}{{/model}}";
        
        ConditionSet conditions = new ConditionSet();
        conditions.setGeneratorVersion(">= 10.0.0"); // This will fail
        
        Insertion fallback = new Insertion();
        fallback.setAfter("{{#model}}");
        fallback.setContent("@Fallback");
        
        Insertion insertion = new Insertion();
        insertion.setAfter("{{#model}}");
        insertion.setContent("@Valid");
        insertion.setConditions(conditions);
        insertion.setFallback(fallback);
        
        String result = templateProcessor.applyInsertion(template, insertion, context, Map.of());
        
        assertTrue(result.contains("@Fallback"));
        assertFalse(result.contains("@Valid"));
    }
    
    @Test
    void testInvalidInsertionThrowsException() {
        String template = "{{#model}}{{/model}}";
        
        Insertion insertion = new Insertion();
        // No insertion point specified - should throw exception
        insertion.setContent("@Valid");
        
        assertThrows(CustomizationException.class, () -> {
            templateProcessor.applyInsertion(template, insertion, context, Map.of());
        });
    }
}