package com.guidedbyte.openapi.modelgen.services;

import java.util.regex.Pattern;

/**
 * Formats logging context using user-configurable patterns.
 * 
 * <p>Supports template variables that can be customized by users:</p>
 * <ul>
 *   <li><code>{{spec}}</code> - The OpenAPI specification name</li>
 *   <li><code>{{template}}</code> - The template file being processed</li>
 *   <li><code>{{component}}</code> - The internal component (for debugging)</li>
 * </ul>
 * 
 * <p>Default format: <code>[{{spec}}{{#template}}:{{template}}{{/template}}]</code></p>
 * <p>Results in: <code>[spring:pojo.mustache]</code> or <code>[spring]</code></p>
 * 
 * <p>Users can customize the format in their build configuration:</p>
 * <pre>
 * openapiModelgen {
 *     defaults {
 *         loggingContextFormat "[{{spec}}] {{component}} ->"
 *         // Results in: [spring] CustomizationEngine ->
 *     }
 * }
 * </pre>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class LoggingContextFormatter {
    
    /**
     * Default context format that shows spec and template in a clean format.
     */
    public static final String DEFAULT_FORMAT = "[{{spec}}{{#template}}:{{template}}{{/template}}]";
    
    /**
     * Alternative format that includes component information for debugging.
     */
    public static final String DEBUG_FORMAT = "[{{component}}|{{spec}}{{#template}}:{{template}}{{/template}}]";
    
    /**
     * Minimal format that only shows the spec name.
     */
    public static final String MINIMAL_FORMAT = "[{{spec}}]";
    
    /**
     * Verbose format that shows all available context.
     */
    public static final String VERBOSE_FORMAT = "[{{component}}] [{{spec}}{{#template}}:{{template}}{{/template}}]";
    
    private final String formatTemplate;
    
    /**
     * Creates a formatter with the default format.
     */
    public LoggingContextFormatter() {
        this(DEFAULT_FORMAT);
    }
    
    /**
     * Creates a formatter with a custom format.
     * 
     * @param formatTemplate the format template to use
     */
    public LoggingContextFormatter(String formatTemplate) {
        this.formatTemplate = formatTemplate != null ? formatTemplate : DEFAULT_FORMAT;
    }
    
    /**
     * Formats the current MDC context using the configured template.
     * 
     * @return the formatted context string, or empty string if no context
     */
    public String formatCurrentContext() {
        String spec = LoggingContext.getCurrentSpec();
        String template = LoggingContext.getCurrentTemplate();
        String component = LoggingContext.getCurrentComponent();
        
        return formatContext(spec, template, component);
    }
    
    /**
     * Formats the provided context values using the configured template.
     * 
     * @param spec the spec name
     * @param template the template name (can be null)
     * @param component the component name (can be null)
     * @return the formatted context string
     */
    public String formatContext(String spec, String template, String component) {
        if (spec == null) {
            return ""; // No context to format
        }
        
        String result = formatTemplate;
        
        // Replace simple variables
        result = result.replace("{{spec}}", spec != null ? spec : "");
        result = result.replace("{{template}}", template != null ? template : "");
        result = result.replace("{{component}}", component != null ? component : "");
        
        // Handle conditional template section: {{#template}}:{{template}}{{/template}}
        result = handleConditionalSection(result, "template", template);
        result = handleConditionalSection(result, "component", component);
        result = handleConditionalSection(result, "spec", spec);
        
        // Clean up any remaining empty conditional sections
        result = cleanupEmptyConditionals(result);
        
        return result.trim();
    }
    
    /**
     * Handles conditional sections like {{#template}}content{{/template}}.
     * If the value is null/empty, the entire section is removed.
     * If the value exists, the section content is kept and variables are replaced.
     */
    private String handleConditionalSection(String input, String variable, String value) {
        String startTag = "{{#" + variable + "}}";
        String endTag = "{{/" + variable + "}}";
        
        int startIndex = input.indexOf(startTag);
        if (startIndex == -1) {
            return input; // No conditional section found
        }
        
        int endIndex = input.indexOf(endTag, startIndex);
        if (endIndex == -1) {
            return input; // Malformed template
        }
        
        String beforeSection = input.substring(0, startIndex);
        String sectionContent = input.substring(startIndex + startTag.length(), endIndex);
        String afterSection = input.substring(endIndex + endTag.length());
        
        if (value != null && !value.trim().isEmpty()) {
            // Keep the section and replace variables within it
            String processedContent = sectionContent.replace("{{" + variable + "}}", value);
            return beforeSection + processedContent + afterSection;
        } else {
            // Remove the entire section
            return beforeSection + afterSection;
        }
    }
    
    /**
     * Cleans up any remaining empty conditional sections that might cause formatting issues.
     */
    private String cleanupEmptyConditionals(String input) {
        // Remove any remaining {{#variable}}{{/variable}} pairs
        return input.replaceAll("\\{\\{#\\w+\\}\\}\\{\\{/\\w+\\}\\}", "");
    }
    
    /**
     * Creates a formatter from a user configuration string.
     * Supports predefined format names and custom templates.
     * 
     * @param userFormat the user-specified format (can be predefined name or custom template)
     * @return the configured formatter
     */
    public static LoggingContextFormatter fromUserConfig(String userFormat) {
        if (userFormat == null || userFormat.trim().isEmpty()) {
            return new LoggingContextFormatter();
        }
        
        // Check for predefined format names
        switch (userFormat.toLowerCase().trim()) {
            case "default":
                return new LoggingContextFormatter(DEFAULT_FORMAT);
            case "debug":
                return new LoggingContextFormatter(DEBUG_FORMAT);
            case "minimal":
                return new LoggingContextFormatter(MINIMAL_FORMAT);
            case "verbose":
                return new LoggingContextFormatter(VERBOSE_FORMAT);
            case "none":
            case "disabled":
                return new LoggingContextFormatter(""); // No context formatting
            default:
                return new LoggingContextFormatter(userFormat); // Custom template
        }
    }
    
    /**
     * Gets the format template being used by this formatter.
     * 
     * @return the format template
     */
    public String getFormatTemplate() {
        return formatTemplate;
    }
}