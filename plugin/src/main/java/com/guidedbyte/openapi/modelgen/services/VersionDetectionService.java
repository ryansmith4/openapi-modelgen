package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.OpenApiModelGenPlugin;
import org.gradle.api.Project;
import org.semver4j.Semver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for detecting OpenAPI Generator version and template features.
 * <p>
 * Provides version detection capabilities and template feature analysis
 * to enable version-aware customizations and compatibility checking.
 * 
 * @since 2.0.0
 */
public class VersionDetectionService {
    private static final Logger logger = LoggerFactory.getLogger(VersionDetectionService.class);
    
    private final Project project;
    private final FeatureDetector featureDetector;
    private final TemplateDiscoveryService templateService;
    
    // Caches for performance
    private final Map<String, Boolean> versionCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Boolean>> templateFeatureCache = new ConcurrentHashMap<>();
    
    // Known version compatibility information
    private static final Map<String, Set<String>> VERSION_FEATURES = Map.of(
        "7.0.0", Set.of("validation_support", "nullable_support", "imports_section"),
        "6.0.0", Set.of("validation_support", "discriminator_support", "imports_section"),
        "5.4.0", Set.of("validation_support", "builder_pattern"),
        "5.0.0", Set.of("imports_section", "package_declaration"),
        "4.0.0", Set.of("imports_section")
    );
    
    public VersionDetectionService(Project project) {
        this.project = project;
        this.featureDetector = new FeatureDetector();
        this.templateService = new TemplateDiscoveryService();
    }
    
    /**
     * Detects the current OpenAPI Generator version.
     * 
     * @return the detected version, or "unknown" if not detectable
     */
    public String detectGeneratorVersion() {
        return OpenApiModelGenPlugin.detectOpenApiGeneratorVersion(project);
    }
    
    /**
     * Detects features available in a specific template.
     * 
     * @param templateName the template to analyze
     * @return map of feature names to availability
     */
    public Map<String, Boolean> detectTemplateFeatures(String templateName) {
        // Check cache first
        Map<String, Boolean> cached = templateFeatureCache.get(templateName);
        if (cached != null) {
            return cached;
        }
        
        // Extract and analyze the template
        String templateContent = templateService.extractBaseTemplate(templateName);
        Map<String, Boolean> features;
        
        if (templateContent != null) {
            features = featureDetector.detectAllFeatures(templateContent);
            logger.debug("Detected {} features in template '{}'", features.size(), templateName);
        } else {
            // Fallback to version-based feature detection
            features = detectFeaturesByVersion(detectGeneratorVersion());
            logger.debug("Using version-based feature detection for template '{}'", templateName);
        }
        
        // Cache the results
        templateFeatureCache.put(templateName, features);
        return features;
    }
    
    /**
     * Checks if the current generator version matches a constraint.
     * 
     * @param versionConstraint the version constraint to check (e.g., ">= 5.4.0")
     * @return true if the version matches the constraint
     */
    public boolean matchesVersionConstraint(String versionConstraint) {
        if (versionConstraint == null || versionConstraint.trim().isEmpty()) {
            return true; // No constraint means always matches
        }
        
        // Check cache
        Boolean cached = versionCache.get(versionConstraint);
        if (cached != null) {
            return cached;
        }
        
        String currentVersion = detectGeneratorVersion();
        boolean matches = evaluateVersionConstraint(currentVersion, versionConstraint);
        
        // Cache the result
        versionCache.put(versionConstraint, matches);
        
        return matches;
    }
    
    /**
     * Checks if a template supports a specific feature.
     * 
     * @param templateName the template to check
     * @param featureName the feature to look for
     * @return true if the feature is supported
     */
    public boolean hasTemplateFeature(String templateName, String featureName) {
        Map<String, Boolean> features = detectTemplateFeatures(templateName);
        return features.getOrDefault(featureName, false);
    }
    
    /**
     * Gets version compatibility information for the current generator.
     * 
     * @return version info including supported features and compatibility notes
     */
    public VersionInfo getVersionInfo() {
        String version = detectGeneratorVersion();
        Set<String> knownFeatures = getKnownFeaturesForVersion(version);
        boolean isSupported = isSupportedVersion(version);
        
        return new VersionInfo(version, knownFeatures, isSupported);
    }
    
    /**
     * Evaluates a version constraint against a specific version.
     */
    private boolean evaluateVersionConstraint(String actualVersion, String constraint) {
        if ("unknown".equals(actualVersion)) {
            logger.debug("Cannot evaluate version constraint '{}' - actual version is unknown", constraint);
            return false; // Conservative approach for unknown versions
        }
        
        try {
            ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
            return conditionEvaluator.evaluateVersionConstraint(constraint, actualVersion);
        } catch (Exception e) {
            logger.warn("Failed to evaluate version constraint '{}' against '{}': {}", 
                constraint, actualVersion, e.getMessage());
            return false;
        }
    }
    
    /**
     * Detects features based on known version capabilities.
     */
    private Map<String, Boolean> detectFeaturesByVersion(String version) {
        Map<String, Boolean> features = new ConcurrentHashMap<>();
        
        // Initialize all features as false
        Set<String> allFeatures = Set.of(
            "validation_support", "nullable_support", "discriminator_support",
            "imports_section", "package_declaration", "documentation_support",
            "serialization_support", "builder_pattern", "fluent_setters"
        );
        
        allFeatures.forEach(feature -> features.put(feature, false));
        
        // Set features based on version
        if (!"unknown".equals(version)) {
            try {
                Semver semver = Semver.parse(normalizeVersion(version));
                
                // Add features based on version milestones
                for (Map.Entry<String, Set<String>> entry : VERSION_FEATURES.entrySet()) {
                    Semver milestoneVersion = Semver.parse(entry.getKey());
                    if (semver.isGreaterThanOrEqualTo(milestoneVersion)) {
                        entry.getValue().forEach(feature -> features.put(feature, true));
                    }
                }
                
            } catch (Exception e) {
                logger.debug("Could not parse version '{}' for feature detection: {}", version, e.getMessage());
            }
        }
        
        return features;
    }
    
    /**
     * Gets known features for a specific version.
     */
    private Set<String> getKnownFeaturesForVersion(String version) {
        if ("unknown".equals(version)) {
            return Set.of();
        }
        
        try {
            Semver semver = Semver.parse(normalizeVersion(version));
            
            // Find the highest milestone version that this version supports
            return VERSION_FEATURES.entrySet().stream()
                .filter(entry -> {
                    try {
                        Semver milestone = Semver.parse(entry.getKey());
                        return semver.isGreaterThanOrEqualTo(milestone);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(Map.Entry::getValue)
                .reduce(Set.of(), (set1, set2) -> {
                    Set<String> combined = new java.util.HashSet<>(set1);
                    combined.addAll(set2);
                    return combined;
                });
                
        } catch (Exception e) {
            return Set.of();
        }
    }
    
    /**
     * Checks if a version is supported by the plugin.
     */
    private boolean isSupportedVersion(String version) {
        if ("unknown".equals(version)) {
            return false;
        }
        
        try {
            Semver semver = Semver.parse(normalizeVersion(version));
            Semver minSupported = Semver.parse("5.0.0");
            
            return semver.isGreaterThanOrEqualTo(minSupported);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Normalizes a version string for semver parsing.
     */
    private String normalizeVersion(String version) {
        if (version == null || "unknown".equals(version)) {
            return "0.0.0";
        }
        
        // Remove 'v' prefix if present
        String normalized = version.startsWith("v") ? version.substring(1) : version;
        
        // Ensure at least three parts
        String[] parts = normalized.split("\\.");
        if (parts.length == 1) {
            return parts[0] + ".0.0";
        } else if (parts.length == 2) {
            return parts[0] + "." + parts[1] + ".0";
        }
        
        return normalized;
    }
    
    /**
     * Version information container.
     */
    public static class VersionInfo {
        private final String version;
        private final Set<String> supportedFeatures;
        private final boolean isSupported;
        
        public VersionInfo(String version, Set<String> supportedFeatures, boolean isSupported) {
            this.version = version;
            this.supportedFeatures = supportedFeatures;
            this.isSupported = isSupported;
        }
        
        public String getVersion() {
            return version;
        }
        
        public Set<String> getSupportedFeatures() {
            return supportedFeatures;
        }
        
        public boolean isSupported() {
            return isSupported;
        }
        
        public boolean hasFeature(String featureName) {
            return supportedFeatures.contains(featureName);
        }
    }
}