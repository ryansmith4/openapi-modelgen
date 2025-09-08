package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.customization.ConditionSet;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConditionEvaluator.
 */
class ConditionEvaluatorTest {
    
    private Project project;
    private ConditionEvaluator conditionEvaluator;
    
    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().build();
        conditionEvaluator = new ConditionEvaluator();
    }
    
    @ParameterizedTest
    @CsvSource({
        "5.4.0, '>= 5.4.0', true",
        "5.3.9, '>= 5.4.0', false", 
        "6.0.0, '< 6.0.0', false",
        "5.4.2, '~> 5.4', true",
        "5.5.0, '~> 5.4', false",
        "6.0.0, '^5.4.0', false",
        "5.5.0, '^5.4.0', true"
    })
    void testVersionConstraints(String actualVersion, String constraint, boolean expected) {
        boolean result = conditionEvaluator.evaluateVersionConstraint(constraint, actualVersion);
        assertEquals(expected, result, 
            String.format("Version %s should %s match constraint %s", 
                actualVersion, expected ? "" : "not ", constraint));
    }
    
    @Test
    void testNullConditionsReturnTrue() {
        EvaluationContext context = EvaluationContext.builder().build();
        boolean result = conditionEvaluator.evaluate(null, context);
        assertTrue(result);
    }
    
    @Test
    void testVersionCondition() {
        ConditionSet conditions = new ConditionSet();
        conditions.setGeneratorVersion(">= 6.0.0");
        
        EvaluationContext context = EvaluationContext.builder()
            .generatorVersion("6.1.0")
            .build();
        
        boolean result = conditionEvaluator.evaluate(conditions, context);
        assertTrue(result);
    }
    
    @Test
    void testTemplateContainsCondition() {
        ConditionSet conditions = new ConditionSet();
        conditions.setTemplateContains("{{#hasValidation}}");
        
        EvaluationContext context = EvaluationContext.builder()
            .templateContent("{{#model}}{{#hasValidation}}@Valid{{/hasValidation}}{{/model}}")
            .build();
        
        boolean result = conditionEvaluator.evaluate(conditions, context);
        assertTrue(result);
    }
    
    @Test
    void testTemplateNotContainsCondition() {
        ConditionSet conditions = new ConditionSet();
        conditions.setTemplateNotContains("{{#deprecated}}");
        
        EvaluationContext context = EvaluationContext.builder()
            .templateContent("{{#model}}{{name}}{{/model}}")
            .build();
        
        boolean result = conditionEvaluator.evaluate(conditions, context);
        assertTrue(result);
    }
    
    @Test
    void testTemplateContainsAllCondition() {
        ConditionSet conditions = new ConditionSet();
        conditions.setTemplateContainsAll(List.of("{{#model}}", "{{name}}"));
        
        EvaluationContext context = EvaluationContext.builder()
            .templateContent("{{#model}}{{name}}{{/model}}")
            .build();
        
        boolean result = conditionEvaluator.evaluate(conditions, context);
        assertTrue(result);
    }
    
    @Test
    void testTemplateContainsAnyCondition() {
        ConditionSet conditions = new ConditionSet();
        conditions.setTemplateContainsAny(List.of("{{#hasValidation}}", "{{#validation}}"));
        
        EvaluationContext context = EvaluationContext.builder()
            .templateContent("{{#model}}{{#validation}}@Valid{{/validation}}{{/model}}")
            .build();
        
        boolean result = conditionEvaluator.evaluate(conditions, context);
        assertTrue(result);
    }
    
    @Test
    void testHasFeatureCondition() {
        ConditionSet conditions = new ConditionSet();
        conditions.setHasFeature("validation_support");
        
        EvaluationContext context = EvaluationContext.builder()
            .templateContent("{{#hasValidation}}@Valid{{/hasValidation}}")
            .build();
        
        boolean result = conditionEvaluator.evaluate(conditions, context);
        assertTrue(result);
    }
    
    @Test
    void testProjectPropertyCondition() {
        ConditionSet conditions = new ConditionSet();
        conditions.setProjectProperty("enableValidation=true");
        
        EvaluationContext context = EvaluationContext.builder()
            .projectProperties(Map.of("enableValidation", "true"))
            .build();
        
        boolean result = conditionEvaluator.evaluate(conditions, context);
        assertTrue(result);
    }
    
    @Test
    void testEnvironmentVariableCondition() {
        ConditionSet conditions = new ConditionSet();
        conditions.setEnvironmentVariable("NODE_ENV=production");
        
        EvaluationContext context = EvaluationContext.builder()
            .environmentVariables(Map.of("NODE_ENV", "production"))
            .build();
        
        boolean result = conditionEvaluator.evaluate(conditions, context);
        assertTrue(result);
    }
    
    @Test
    void testAllOfCondition() {
        ConditionSet condition1 = new ConditionSet();
        condition1.setGeneratorVersion(">= 6.0.0");
        
        ConditionSet condition2 = new ConditionSet();
        condition2.setTemplateContains("{{#model}}");
        
        ConditionSet allOfCondition = new ConditionSet();
        allOfCondition.setAllOf(List.of(condition1, condition2));
        
        EvaluationContext context = EvaluationContext.builder()
            .generatorVersion("6.1.0")
            .templateContent("{{#model}}content{{/model}}")
            .build();
        
        boolean result = conditionEvaluator.evaluate(allOfCondition, context);
        assertTrue(result);
    }
    
    @Test
    void testAnyOfCondition() {
        ConditionSet condition1 = new ConditionSet();
        condition1.setGeneratorVersion(">= 10.0.0"); // This will fail
        
        ConditionSet condition2 = new ConditionSet();
        condition2.setTemplateContains("{{#model}}"); // This will pass
        
        ConditionSet anyOfCondition = new ConditionSet();
        anyOfCondition.setAnyOf(List.of(condition1, condition2));
        
        EvaluationContext context = EvaluationContext.builder()
            .generatorVersion("6.1.0")
            .templateContent("{{#model}}content{{/model}}")
            .build();
        
        boolean result = conditionEvaluator.evaluate(anyOfCondition, context);
        assertTrue(result);
    }
    
    @Test
    void testNotCondition() {
        ConditionSet innerCondition = new ConditionSet();
        innerCondition.setGeneratorVersion("< 5.0.0");
        
        ConditionSet notCondition = new ConditionSet();
        notCondition.setNot(innerCondition);
        
        EvaluationContext context = EvaluationContext.builder()
            .generatorVersion("6.1.0")
            .build();
        
        boolean result = conditionEvaluator.evaluate(notCondition, context);
        assertTrue(result); // 6.1.0 is NOT < 5.0.0, so NOT condition passes
    }
    
    @Test
    void testComplexNestedConditions() {
        // Create: (version >= 6.0.0 AND template contains model) OR feature validation
        ConditionSet versionCondition = new ConditionSet();
        versionCondition.setGeneratorVersion(">= 6.0.0");
        
        ConditionSet templateCondition = new ConditionSet();
        templateCondition.setTemplateContains("{{#model}}");
        
        ConditionSet featureCondition = new ConditionSet();
        featureCondition.setHasFeature("validation_support");
        
        ConditionSet allOfCondition = new ConditionSet();
        allOfCondition.setAllOf(List.of(versionCondition, templateCondition));
        
        ConditionSet rootCondition = new ConditionSet();
        rootCondition.setAnyOf(List.of(allOfCondition, featureCondition));
        
        EvaluationContext context = EvaluationContext.builder()
            .generatorVersion("5.0.0") // Version condition will fail
            .templateContent("{{#model}}{{#useBeanValidation}}@Valid{{/useBeanValidation}}content{{/model}}") // Template condition passes and has validation support
            .build();
        
        boolean result = conditionEvaluator.evaluate(rootCondition, context);
        assertTrue(result); // Should pass because of validation_support feature
    }
}