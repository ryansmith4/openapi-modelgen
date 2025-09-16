package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.customization.ConditionSet;
import com.guidedbyte.openapi.modelgen.utils.VersionUtils;
import org.semver4j.Semver;
import org.slf4j.Logger;
import com.guidedbyte.openapi.modelgen.util.PluginLoggerFactory;

import java.util.List;

/**
 * Configuration-cache compatible evaluator for complex conditional logic in template customizations.
 * 
 * <p>This service handles sophisticated condition evaluation including:</p>
 * <ul>
 *   <li><strong>Version Constraints:</strong> Semantic versioning with comparison operators (&gt;=, &lt;=, etc.)</li>
 *   <li><strong>Template Content Checks:</strong> Pattern matching within template content</li>
 *   <li><strong>Feature Detection:</strong> OpenAPI Generator feature availability checks</li>
 *   <li><strong>Environment Conditions:</strong> Project properties and environment variables</li>
 *   <li><strong>Logical Operations:</strong> Complex boolean logic with allOf, anyOf, not operators</li>
 * </ul>
 * 
 * <p>The evaluator is stateless and thread-safe, using only immutable evaluation contexts
 * to ensure compatibility with Gradle's configuration cache and parallel processing.</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.0.0
 */
public class ConditionEvaluator {
    private static final Logger logger = PluginLoggerFactory.getLogger(ConditionEvaluator.class);
    
    /**
     * Creates a new ConditionEvaluator.
     */
    public ConditionEvaluator() {
    }
    
    /**
     * Evaluates a condition set against the given context.
     * 
     * @param conditions the conditions to evaluate
     * @param context the evaluation context
     * @return true if all conditions are met
     */
    public boolean evaluate(ConditionSet conditions, EvaluationContext context) {
        if (conditions == null) {
            return true; // No conditions = always true
        }
        
        // Handle logical operators first
        if (conditions.getAllOf() != null) {
            return evaluateAllOf(conditions.getAllOf(), context);
        }
        
        if (conditions.getAnyOf() != null) {
            return evaluateAnyOf(conditions.getAnyOf(), context);
        }
        
        if (conditions.getNot() != null) {
            return !evaluate(conditions.getNot(), context);
        }
        
        // Evaluate simple conditions - all must be true
        return evaluateSimpleConditions(conditions, context);
    }
    
    /**
     * Evaluates all conditions in a list (AND operation).
     */
    private boolean evaluateAllOf(List<ConditionSet> conditionsList, EvaluationContext context) {
        return conditionsList.stream().allMatch(c -> evaluate(c, context));
    }
    
    /**
     * Evaluates any condition in a list (OR operation).
     */
    private boolean evaluateAnyOf(List<ConditionSet> conditionsList, EvaluationContext context) {
        return conditionsList.stream().anyMatch(c -> evaluate(c, context));
    }
    
    /**
     * Evaluates simple (non-logical) conditions.
     */
    private boolean evaluateSimpleConditions(ConditionSet conditions, EvaluationContext context) {
        // Version constraints
        if (conditions.getGeneratorVersion() != null) {
            if (!evaluateVersionConstraint(conditions.getGeneratorVersion(), context.getGeneratorVersion())) {
                logger.debug("Version constraint not met: {} vs {}", 
                    conditions.getGeneratorVersion(), context.getGeneratorVersion());
                return false;
            }
        }
        
        // Template content conditions
        if (conditions.getTemplateContains() != null) {
            if (!context.templateContains(conditions.getTemplateContains())) {
                logger.debug("Template does not contain required pattern: {}", conditions.getTemplateContains());
                return false;
            }
        }
        
        if (conditions.getTemplateNotContains() != null) {
            if (context.templateContains(conditions.getTemplateNotContains())) {
                logger.debug("Template contains forbidden pattern: {}", conditions.getTemplateNotContains());
                return false;
            }
        }
        
        if (conditions.getTemplateContainsAll() != null) {
            for (String pattern : conditions.getTemplateContainsAll()) {
                if (!context.templateContains(pattern)) {
                    logger.debug("Template does not contain required pattern from 'all' list: {}", pattern);
                    return false;
                }
            }
        }
        
        if (conditions.getTemplateContainsAny() != null) {
            boolean anyFound = conditions.getTemplateContainsAny().stream()
                .anyMatch(context::templateContains);
            if (!anyFound) {
                logger.debug("Template does not contain any pattern from 'any' list: {}", conditions.getTemplateContainsAny());
                return false;
            }
        }
        
        // Feature conditions
        if (conditions.getHasFeature() != null) {
            if (!context.hasFeature(conditions.getHasFeature())) {
                logger.debug("Template does not have required feature: {}", conditions.getHasFeature());
                return false;
            }
        }
        
        if (conditions.getHasAllFeatures() != null) {
            for (String feature : conditions.getHasAllFeatures()) {
                if (!context.hasFeature(feature)) {
                    logger.debug("Template does not have required feature from 'all' list: {}", feature);
                    return false;
                }
            }
        }
        
        if (conditions.getHasAnyFeatures() != null) {
            boolean anyFound = conditions.getHasAnyFeatures().stream()
                .anyMatch(context::hasFeature);
            if (!anyFound) {
                logger.debug("Template does not have any feature from 'any' list: {}", conditions.getHasAnyFeatures());
                return false;
            }
        }
        
        // Build environment conditions
        if (conditions.getProjectProperty() != null) {
            if (!context.hasProjectProperty(conditions.getProjectProperty())) {
                logger.debug("Project property condition not met: {}", conditions.getProjectProperty());
                return false;
            }
        }
        
        if (conditions.getEnvironmentVariable() != null) {
            if (!context.hasEnvironmentVariable(conditions.getEnvironmentVariable())) {
                logger.debug("Environment variable condition not met: {}", conditions.getEnvironmentVariable());
                return false;
            }
        }
        
        if (conditions.getBuildType() != null) {
            // Build type is typically determined from project properties or environment
            String buildType = context.getProjectProperty("buildType");
            if (buildType == null) {
                buildType = context.getEnvironmentVariable("BUILD_TYPE");
            }
            if (!conditions.getBuildType().equals(buildType)) {
                logger.debug("Build type condition not met: expected {}, actual {}", 
                    conditions.getBuildType(), buildType);
                return false;
            }
        }
        
        return true; // All conditions passed
    }
    
    /**
     * Evaluates a semantic version constraint against an actual version.
     * <p>
     * Supports operators: &gt;=, &gt;, &lt;=, &lt;, ~&gt;, ^
     * 
     * @param constraint the version constraint to evaluate
     * @param actualVersion the actual version to check
     * @return true if the version satisfies the constraint
     */
    public boolean evaluateVersionConstraint(String constraint, String actualVersion) {
        if (constraint == null || actualVersion == null) {
            return false;
        }
        
        try {
            // Parse the constraint
            String operator;
            String constraintVersion;
            
            if (constraint.startsWith(">=")) {
                operator = ">=";
                constraintVersion = constraint.substring(2).trim();
            } else if (constraint.startsWith("<=")) {
                operator = "<=";
                constraintVersion = constraint.substring(2).trim();
            } else if (constraint.startsWith("~>")) {
                operator = "~>";
                constraintVersion = constraint.substring(2).trim();
            } else if (constraint.startsWith(">")) {
                operator = ">";
                constraintVersion = constraint.substring(1).trim();
            } else if (constraint.startsWith("<")) {
                operator = "<";
                constraintVersion = constraint.substring(1).trim();
            } else if (constraint.startsWith("^")) {
                operator = "^";
                constraintVersion = constraint.substring(1).trim();
            } else {
                // Default to exact match
                operator = "=";
                constraintVersion = constraint.trim();
            }
            
            return evaluateVersionOperation(actualVersion, operator, constraintVersion);
            
        } catch (Exception e) {
            logger.warn("Invalid version constraint '{}' or version '{}': {}", 
                constraint, actualVersion, e.getMessage());
            return false;
        }
    }
    
    /**
     * Evaluates a specific version operation using semver4j.
     */
    private boolean evaluateVersionOperation(String actualVersion, String operator, String constraintVersion) {
        try {
            Semver actual = Semver.parse(VersionUtils.normalizeVersion(actualVersion));
            Semver constraint = Semver.parse(VersionUtils.normalizeVersion(constraintVersion));
            
            switch (operator) {
                case ">=":
                    return actual.isGreaterThanOrEqualTo(constraint);
                case ">":
                    return actual.isGreaterThan(constraint);
                case "<=":
                    return actual.isLowerThanOrEqualTo(constraint);
                case "<":
                    return actual.isLowerThan(constraint);
                case "=":
                    return actual.isEqualTo(constraint);
                case "~>":
                    // Compatible within same minor version
                    return actual.getMajor() == constraint.getMajor() && 
                           actual.getMinor() == constraint.getMinor() &&
                           actual.isGreaterThanOrEqualTo(constraint);
                case "^":
                    // Compatible within same major version
                    return actual.getMajor() == constraint.getMajor() &&
                           actual.isGreaterThanOrEqualTo(constraint);
                default:
                    logger.warn("Unknown version operator: {}", operator);
                    return false;
            }
        } catch (Exception e) {
            // Fallback to string comparison for non-semver versions
            logger.debug("Using string comparison for version constraint (not valid semver): {} {} {}", 
                actualVersion, operator, constraintVersion);
            return evaluateVersionStringComparison(actualVersion, operator, constraintVersion);
        }
    }
    
    
    /**
     * Fallback string comparison for non-semver versions.
     */
    private boolean evaluateVersionStringComparison(String actual, String operator, String constraint) {
        int comparison = actual.compareToIgnoreCase(constraint);
        
        switch (operator) {
            case ">=":
                return comparison >= 0;
            case ">":
                return comparison > 0;
            case "<=":
                return comparison <= 0;
            case "<":
                return comparison < 0;
            case "=":
                return comparison == 0;
            default:
                return false;
        }
    }
}