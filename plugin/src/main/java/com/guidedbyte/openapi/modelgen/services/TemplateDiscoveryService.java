package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.constants.PluginConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Configuration-cache compatible template discovery service using OpenAPI Generator's CodegenConfig API.
 * 
 * <p>This service provides clean template extraction without complex JAR scanning:</p>
 * <ul>
 *   <li><strong>Official API Usage:</strong> Uses OpenAPI Generator's CodegenConfig API instead of reverse-engineering</li>
 *   <li><strong>Selective Extraction:</strong> Only extracts templates that are needed for customization</li>
 *   <li><strong>Generator Support:</strong> Works with any OpenAPI Generator (spring, java, etc.)</li>
 *   <li><strong>Fallback Handling:</strong> Graceful handling when templates aren't available</li>
 *   <li><strong>Error Recovery:</strong> Robust error handling with informative logging</li>
 * </ul>
 * 
 * <p>The service is stateless and thread-safe, making it compatible with configuration caching
 * and parallel processing. It relies on {@link CodegenConfigTemplateExtractor} for the actual
 * template extraction using OpenAPI Generator's official APIs.</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
public class TemplateDiscoveryService {
    private static final Logger logger = LoggerFactory.getLogger(TemplateDiscoveryService.class);
    private final CodegenConfigTemplateExtractor extractor = new CodegenConfigTemplateExtractor();
    
    /**
     * Extracts a base template using the default Spring generator.
     * This is a convenience method for tests and backward compatibility.
     */
    public String extractBaseTemplate(String templateName) {
        return extractBaseTemplate(templateName, PluginConstants.DEFAULT_GENERATOR_NAME);
    }
    
    /**
     * Extracts a base template for a specific generator.
     */
    public String extractBaseTemplate(String templateName, String generatorName) {
        if (templateName == null || templateName.trim().isEmpty()) {
            logger.warn("Template name cannot be null or empty");
            return null;
        }
        
        logger.debug("=== TEMPLATE EXTRACTION START ===");
        logger.debug("Template: '{}', Generator: '{}'", templateName, generatorName);
        
        try {
            String templateContent = extractor.extractTemplate(templateName, generatorName);
            
            if (templateContent != null) {
                logger.debug("Successfully extracted template '{}' ({} characters)", 
                    templateName, templateContent.length());
                logger.debug("Template first 100 chars: {}", 
                    templateContent.length() > 100 ? templateContent.substring(0, 100).replace("\n", "\\n") + "..." : templateContent.replace("\n", "\\n"));
            } else {
                logger.debug("Template '{}' not found or empty for generator '{}'", templateName, generatorName);
            }
            
            logger.debug("=== TEMPLATE EXTRACTION COMPLETE ===");
            return templateContent;
            
        } catch (Exception e) {
            logger.warn("Failed to extract base template '{}': {}", templateName, e.getMessage());
            logger.debug("Template extraction error details for '{}'", templateName, e);
            logger.debug("=== TEMPLATE EXTRACTION FAILED ===");
            return null;
        }
    }
    
}