package com.guidedbyte.openapi.modelgen.services;

import org.slf4j.MDC;

/**
 * Manages SLF4J MDC (Mapped Diagnostic Context) for consistent, context-aware logging
 * throughout the OpenAPI Model Generator plugin.
 * 
 * <p>This utility provides a simple way to set contextual information that will
 * automatically appear in all log messages from the current thread. The context
 * includes:</p>
 * 
 * <ul>
 *   <li><strong>Spec Name:</strong> Which OpenAPI specification is being processed</li>
 *   <li><strong>Template Name:</strong> Which template file is being customized</li>
 *   <li><strong>Component:</strong> Which service/component is performing the operation</li>
 * </ul>
 * 
 * <p><strong>Usage Pattern:</strong></p>
 * <pre>
 * LoggingContext.setContext("pets", "pojo.mustache");
 * LoggingContext.setComponent("CustomizationEngine");
 * try {
 *     // All logging from this thread will include the context
 *     logger.debug("Processing customizations...");
 * } finally {
 *     LoggingContext.clear(); // Always clean up
 * }
 * </pre>
 * 
 * <p><strong>Logback Pattern:</strong> Include MDC context in your logback.xml:</p>
 * <pre>
 * &lt;pattern&gt;%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%X{component}] [%X{spec}:%X{template}] - %msg%n&lt;/pattern&gt;
 * </pre>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public final class LoggingContext {
    
    /** MDC key for the OpenAPI specification being processed */
    public static final String SPEC_KEY = "spec";
    
    /** MDC key for the template file being processed */
    public static final String TEMPLATE_KEY = "template";
    
    /** MDC key for the component/service performing the operation */
    public static final String COMPONENT_KEY = "component";
    
    // Private constructor - utility class
    private LoggingContext() {
    }
    
    /**
     * Sets the OpenAPI specification and template context for logging.
     * 
     * @param specName the name of the OpenAPI specification being processed
     * @param templateName the name of the template being processed (can be null)
     */
    public static void setContext(String specName, String templateName) {
        if (specName != null) {
            MDC.put(SPEC_KEY, specName);
        }
        if (templateName != null) {
            MDC.put(TEMPLATE_KEY, templateName);
        }
    }
    
    /**
     * Sets only the OpenAPI specification context for logging.
     * 
     * @param specName the name of the OpenAPI specification being processed
     */
    public static void setSpec(String specName) {
        if (specName != null) {
            MDC.put(SPEC_KEY, specName);
        }
    }
    
    /**
     * Sets only the template context for logging.
     * 
     * @param templateName the name of the template being processed
     */
    public static void setTemplate(String templateName) {
        if (templateName != null) {
            MDC.put(TEMPLATE_KEY, templateName);
        }
    }
    
    /**
     * Sets the component/service context for logging.
     * 
     * @param componentName the name of the component performing the operation
     */
    public static void setComponent(String componentName) {
        if (componentName != null) {
            MDC.put(COMPONENT_KEY, componentName);
        }
    }
    
    /**
     * Clears all logging context from the current thread.
     * 
     * <p><strong>Important:</strong> Always call this in a finally block to ensure
     * context doesn't leak to other operations on the same thread.</p>
     */
    public static void clear() {
        MDC.clear();
    }
    
    /**
     * Clears only the template context, leaving spec and component context intact.
     * 
     * <p>Useful when processing multiple templates for the same spec.</p>
     */
    public static void clearTemplate() {
        MDC.remove(TEMPLATE_KEY);
    }
    
    /**
     * Gets the current spec name from MDC context.
     * 
     * @return the current spec name or null if not set
     */
    public static String getCurrentSpec() {
        return MDC.get(SPEC_KEY);
    }
    
    /**
     * Gets the current template name from MDC context.
     * 
     * @return the current template name or null if not set
     */
    public static String getCurrentTemplate() {
        return MDC.get(TEMPLATE_KEY);
    }
    
    /**
     * Gets the current component name from MDC context.
     * 
     * @return the current component name or null if not set
     */
    public static String getCurrentComponent() {
        return MDC.get(COMPONENT_KEY);
    }
    
    /**
     * Executes code with specific logging context, automatically cleaning up afterwards.
     * 
     * <p>This is the safest way to use logging context as it guarantees cleanup
     * even if exceptions occur.</p>
     * 
     * @param specName the spec name for context
     * @param templateName the template name for context (can be null)
     * @param componentName the component name for context
     * @param operation the operation to execute with context
     */
    public static void withContext(String specName, String templateName, String componentName, Runnable operation) {
        // Save existing context
        String oldSpec = getCurrentSpec();
        String oldTemplate = getCurrentTemplate();
        String oldComponent = getCurrentComponent();
        
        try {
            setContext(specName, templateName);
            setComponent(componentName);
            operation.run();
        } finally {
            // Restore previous context
            clear();
            if (oldSpec != null) setSpec(oldSpec);
            if (oldTemplate != null) setTemplate(oldTemplate);
            if (oldComponent != null) setComponent(oldComponent);
        }
    }
    
    /**
     * Executes code with specific spec context, automatically cleaning up afterwards.
     * 
     * @param specName the spec name for context
     * @param componentName the component name for context
     * @param operation the operation to execute with context
     */
    public static void withSpecContext(String specName, String componentName, Runnable operation) {
        withContext(specName, null, componentName, operation);
    }
}