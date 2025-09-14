package com.guidedbyte.openapi.modelgen.constants;

/**
 * Constants for logging patterns and formatting.
 * These values are based on typical content lengths observed during plugin operation.
 */
public final class LoggingConstants {
    
    // Prevent instantiation
    private LoggingConstants() {
        throw new AssertionError("Constants class should not be instantiated");
    }
    
    // Timestamp format length estimates
    /** Length estimate for ISO timestamp format (2023-12-25T14:30:45.123). */
    public static final int ISO_TIMESTAMP_LENGTH = 23;
    /** Length estimate for time-only format (HH:mm:ss,SSS). */
    public static final int TIME_ONLY_LENGTH = 12;
    /** Length estimate for readable timestamp format (25 Dec 2023 14:30:45,123). */
    public static final int READABLE_TIMESTAMP_LENGTH = 24;
    
    // Message content length estimates (for StringBuilder pre-allocation performance)
    // These are NOT functional limits - content can exceed these values
    /** 
     * Typical log message length estimate. Generous to handle complex error messages.
     * Real example: "Failed to apply YAML customization to template: additionalModelTypeAnnotations.mustache"
     */
    public static final int TYPICAL_MESSAGE_LENGTH = 200;
    /** 
     * Typical spec name length estimate.
     * Real examples: "spring" (6), "user-management-service-api" (28), "payment-processing-microservice" (32)
     */
    public static final int TYPICAL_SPEC_NAME_LENGTH = 40;
    /** 
     * Typical template name length estimate.
     * Real examples: "pojo.mustache" (13), "additionalModelTypeAnnotations.mustache" (39)
     */
    public static final int TYPICAL_TEMPLATE_NAME_LENGTH = 50;
    /** 
     * Typical component name length estimate.
     * Real examples: "CustomizationEngine" (18), "PrepareTemplateDirectoryTask" (28), "LibraryTemplateMetadataValidator" (32)
     */
    public static final int TYPICAL_COMPONENT_NAME_LENGTH = 35;
    /** Default estimate for unknown MDC keys. Generous to avoid underestimation. */
    public static final int DEFAULT_MDC_LENGTH = 25;
    
    // Hash constants for object identity
    /** Prime number used in hashCode calculations. */
    public static final int HASH_PRIME = 31;
}