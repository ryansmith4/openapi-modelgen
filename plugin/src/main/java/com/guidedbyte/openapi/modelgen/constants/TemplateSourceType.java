package com.guidedbyte.openapi.modelgen.constants;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enum representing the different template source types with explicit precedence ordering.
 * These values are used in the templateSources configuration to control
 * which template sources are active and their resolution order.
 * 
 * <p>Each source has an explicit precedence value (lower number = higher priority):</p>
 * <ol>
 *   <li>USER_TEMPLATES (1) - Explicit .mustache files in templateDir</li>
 *   <li>USER_CUSTOMIZATIONS (2) - YAML customizations in templateCustomizationsDir</li>
 *   <li>LIBRARY_TEMPLATES (3) - Templates from JAR dependencies</li>
 *   <li>LIBRARY_CUSTOMIZATIONS (4) - YAML customizations from JAR dependencies</li>
 *   <li>PLUGIN_CUSTOMIZATIONS (5) - Built-in plugin YAML customizations</li>
 *   <li>OPENAPI_GENERATOR (6) - OpenAPI Generator default templates</li>
 * </ol>
 * 
 * <p>The precedence values ensure consistent ordering regardless of enum declaration order,
 * making the code more maintainable and refactoring-safe.</p>
 */
public enum TemplateSourceType {
    
    USER_TEMPLATES(1, "user-templates", "Explicit .mustache files in templateDir"),
    USER_CUSTOMIZATIONS(2, "user-customizations", "YAML customizations in templateCustomizationsDir"),
    LIBRARY_TEMPLATES(3, "library-templates", "Templates from JAR dependencies"),
    LIBRARY_CUSTOMIZATIONS(4, "library-customizations", "YAML customizations from JAR dependencies"),
    PLUGIN_CUSTOMIZATIONS(5, "plugin-customizations", "Built-in plugin YAML customizations"),
    OPENAPI_GENERATOR(6, "openapi-generator", "OpenAPI Generator default templates (always available)");
    
    private final int precedence;
    private final String value;
    private final String description;
    
    TemplateSourceType(int precedence, String value, String description) {
        this.precedence = precedence;
        this.value = value;
        this.description = description;
    }
    
    /**
     * Gets the precedence order (lower number = higher priority).
     * @return the precedence value
     */
    public int getPrecedence() {
        return precedence;
    }
    
    /**
     * Gets the string value used in configuration files.
     * @return the configuration value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Gets the human-readable description of this template source.
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Converts a string value to the corresponding enum constant.
     * @param value the string value to convert
     * @return the matching enum constant
     * @throws IllegalArgumentException if the value doesn't match any constant
     */
    public static TemplateSourceType fromValue(String value) {
        for (TemplateSourceType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown template source type: " + value);
    }
    
    /**
     * Checks if a string value is a valid template source type.
     * @param value the string value to check
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String value) {
        for (TemplateSourceType type : values()) {
            if (type.value.equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the default list of all template sources in precedence order.
     * @return unmodifiable list of all template source values in precedence order
     */
    public static List<String> getDefaultSourcesList() {
        return getAllAsStrings();
    }
    
    /**
     * Converts a list of TemplateSourceType enums to their string values,
     * maintaining precedence order.
     * @param types the enum types to convert
     * @return list of string values sorted by precedence
     */
    public static List<String> toStringList(List<TemplateSourceType> types) {
        return types.stream()
            .sorted(Comparator.comparingInt(TemplateSourceType::getPrecedence))
            .map(TemplateSourceType::toString)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all enum values as their string representations in precedence order.
     * @return list of all template source strings sorted by precedence
     */
    public static List<String> getAllAsStrings() {
        return Arrays.stream(values())
            .sorted(Comparator.comparingInt(TemplateSourceType::getPrecedence))
            .map(TemplateSourceType::toString)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all enum values sorted by precedence order.
     * @return list of all template source types sorted by precedence
     */
    public static List<TemplateSourceType> getAllInPrecedenceOrder() {
        return Arrays.stream(values())
            .sorted(Comparator.comparingInt(TemplateSourceType::getPrecedence))
            .collect(Collectors.toList());
    }
    
    /**
     * Checks if this source has higher precedence than another.
     * @param other the other template source to compare
     * @return true if this source has higher precedence (lower number)
     */
    public boolean hasHigherPrecedenceThan(TemplateSourceType other) {
        return this.precedence < other.precedence;
    }
    
    @Override
    public String toString() {
        return value;
    }
}