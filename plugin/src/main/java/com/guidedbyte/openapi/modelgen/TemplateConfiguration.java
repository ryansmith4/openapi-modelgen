package com.guidedbyte.openapi.modelgen;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration-cache compatible container for template resolution information.
 * This class is serializable and contains all template-related data resolved at configuration time,
 * eliminating the need for runtime project access.
 * 
 * <p>Contains complete template resolution state including:</p>
 * <ul>
 *   <li>Template source availability flags (user templates, user customizations, plugin customizations)</li>
 *   <li>Directory paths for template work, user templates, and user customizations</li>
 *   <li>Template precedence configuration for resolution order</li>
 *   <li>Debug template resolution flag for troubleshooting</li>
 *   <li>Template variables for Mustache template processing</li>
 * </ul>
 * 
 * <p>This class enables configuration-cache compatibility by pre-resolving all template-related
 * configuration at configuration time rather than task execution time.</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
public class TemplateConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String generatorName;
    private final String templateWorkDirectory;
    private final boolean hasUserTemplates;
    private final boolean hasUserCustomizations;
    private final boolean hasPluginCustomizations;
    private final String userTemplateDirectory;
    private final String userCustomizationsDirectory;
    private final Map<String, String> templateVariables;
    private final boolean templateProcessingEnabled;
    private final List<String> templatePrecedence;
    private final boolean debugTemplateResolution;
    
    // Library template support
    private final boolean hasLibraryTemplates;
    private final boolean hasLibraryCustomizations;
    private final Map<String, String> libraryTemplates;
    private final Map<String, String> libraryCustomizations;
    private final Map<String, com.guidedbyte.openapi.modelgen.services.LibraryMetadata> libraryMetadata;
    
    private TemplateConfiguration(Builder builder) {
        this.generatorName = builder.generatorName;
        this.templateWorkDirectory = builder.templateWorkDirectory;
        this.hasUserTemplates = builder.hasUserTemplates;
        this.hasUserCustomizations = builder.hasUserCustomizations;
        this.hasPluginCustomizations = builder.hasPluginCustomizations;
        this.userTemplateDirectory = builder.userTemplateDirectory;
        this.userCustomizationsDirectory = builder.userCustomizationsDirectory;
        this.templateVariables = builder.templateVariables != null ? 
            Collections.unmodifiableMap(new HashMap<>(builder.templateVariables)) : Collections.emptyMap();
        this.templateProcessingEnabled = builder.templateProcessingEnabled;
        this.templatePrecedence = builder.templatePrecedence != null ?
            Collections.unmodifiableList(builder.templatePrecedence) : Collections.emptyList();
        this.debugTemplateResolution = builder.debugTemplateResolution;
        
        // Library support
        this.hasLibraryTemplates = builder.hasLibraryTemplates;
        this.hasLibraryCustomizations = builder.hasLibraryCustomizations;
        this.libraryTemplates = builder.libraryTemplates != null ?
            Collections.unmodifiableMap(new HashMap<>(builder.libraryTemplates)) : Collections.emptyMap();
        this.libraryCustomizations = builder.libraryCustomizations != null ?
            Collections.unmodifiableMap(new HashMap<>(builder.libraryCustomizations)) : Collections.emptyMap();
        this.libraryMetadata = builder.libraryMetadata != null ?
            Collections.unmodifiableMap(new HashMap<>(builder.libraryMetadata)) : Collections.emptyMap();
    }
    
    public static Builder builder(String generatorName) {
        return new Builder(generatorName);
    }
    
    public String getGeneratorName() { return generatorName; }
    public String getTemplateWorkDirectory() { return templateWorkDirectory; }
    public boolean hasUserTemplates() { return hasUserTemplates; }
    public boolean hasUserCustomizations() { return hasUserCustomizations; }
    public boolean hasPluginCustomizations() { return hasPluginCustomizations; }
    public String getUserTemplateDirectory() { return userTemplateDirectory; }
    public String getUserCustomizationsDirectory() { return userCustomizationsDirectory; }
    public Map<String, String> getTemplateVariables() { return templateVariables; }
    public boolean isTemplateProcessingEnabled() { return templateProcessingEnabled; }
    public List<String> getTemplatePrecedence() { return templatePrecedence; }
    public boolean isDebugTemplateResolution() { return debugTemplateResolution; }
    
    // Library template getters
    public boolean hasLibraryTemplates() { return hasLibraryTemplates; }
    public boolean hasLibraryCustomizations() { return hasLibraryCustomizations; }
    public Map<String, String> getLibraryTemplates() { return libraryTemplates; }
    public Map<String, String> getLibraryCustomizations() { return libraryCustomizations; }
    public Map<String, com.guidedbyte.openapi.modelgen.services.LibraryMetadata> getLibraryMetadata() { return libraryMetadata; }
    
    public boolean hasAnyCustomizations() {
        return hasUserTemplates || hasUserCustomizations || hasLibraryTemplates || hasLibraryCustomizations || hasPluginCustomizations;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateConfiguration that = (TemplateConfiguration) o;
        return hasUserTemplates == that.hasUserTemplates &&
                hasUserCustomizations == that.hasUserCustomizations &&
                hasPluginCustomizations == that.hasPluginCustomizations &&
                templateProcessingEnabled == that.templateProcessingEnabled &&
                debugTemplateResolution == that.debugTemplateResolution &&
                Objects.equals(generatorName, that.generatorName) &&
                Objects.equals(templateWorkDirectory, that.templateWorkDirectory) &&
                Objects.equals(userTemplateDirectory, that.userTemplateDirectory) &&
                Objects.equals(userCustomizationsDirectory, that.userCustomizationsDirectory) &&
                Objects.equals(templateVariables, that.templateVariables) &&
                Objects.equals(templatePrecedence, that.templatePrecedence);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(generatorName, templateWorkDirectory, hasUserTemplates, hasUserCustomizations,
                hasPluginCustomizations, userTemplateDirectory,
                userCustomizationsDirectory, templateVariables, templateProcessingEnabled,
                templatePrecedence, debugTemplateResolution);
    }
    
    @Override
    public String toString() {
        return "TemplateConfiguration{" +
                "generatorName='" + generatorName + '\'' +
                ", templateWorkDirectory='" + templateWorkDirectory + '\'' +
                ", hasUserTemplates=" + hasUserTemplates +
                ", hasUserCustomizations=" + hasUserCustomizations +
                ", hasPluginCustomizations=" + hasPluginCustomizations +
                ", templateProcessingEnabled=" + templateProcessingEnabled +
                '}';
    }
    
    public static class Builder {
        private final String generatorName;
        private String templateWorkDirectory;
        private boolean hasUserTemplates;
        private boolean hasUserCustomizations;
        private boolean hasPluginCustomizations;
        private String userTemplateDirectory;
        private String userCustomizationsDirectory;
        private Map<String, String> templateVariables;
        private boolean templateProcessingEnabled = true;
        private List<String> templatePrecedence;
        private boolean debugTemplateResolution = false;
        
        // Library support
        private boolean hasLibraryTemplates = false;
        private boolean hasLibraryCustomizations = false;
        private Map<String, String> libraryTemplates;
        private Map<String, String> libraryCustomizations;
        private Map<String, com.guidedbyte.openapi.modelgen.services.LibraryMetadata> libraryMetadata;
        
        private Builder(String generatorName) {
            this.generatorName = Objects.requireNonNull(generatorName, "generatorName must not be null");
        }
        
        public Builder templateWorkDirectory(String templateWorkDirectory) {
            this.templateWorkDirectory = templateWorkDirectory;
            return this;
        }
        
        public Builder hasUserTemplates(boolean hasUserTemplates) {
            this.hasUserTemplates = hasUserTemplates;
            return this;
        }
        
        public Builder hasUserCustomizations(boolean hasUserCustomizations) {
            this.hasUserCustomizations = hasUserCustomizations;
            return this;
        }
        
        public Builder hasPluginCustomizations(boolean hasPluginCustomizations) {
            this.hasPluginCustomizations = hasPluginCustomizations;
            return this;
        }
        
        
        public Builder userTemplateDirectory(String userTemplateDirectory) {
            this.userTemplateDirectory = userTemplateDirectory;
            return this;
        }
        
        public Builder userCustomizationsDirectory(String userCustomizationsDirectory) {
            this.userCustomizationsDirectory = userCustomizationsDirectory;
            return this;
        }
        
        public Builder templateVariables(Map<String, String> templateVariables) {
            this.templateVariables = templateVariables;
            return this;
        }
        
        public Builder templateProcessingEnabled(boolean templateProcessingEnabled) {
            this.templateProcessingEnabled = templateProcessingEnabled;
            return this;
        }
        
        public Builder templatePrecedence(List<String> templatePrecedence) {
            this.templatePrecedence = templatePrecedence;
            return this;
        }
        
        public Builder debugTemplateResolution(boolean debugTemplateResolution) {
            this.debugTemplateResolution = debugTemplateResolution;
            return this;
        }
        
        public Builder hasLibraryTemplates(boolean hasLibraryTemplates) {
            this.hasLibraryTemplates = hasLibraryTemplates;
            return this;
        }
        
        public Builder hasLibraryCustomizations(boolean hasLibraryCustomizations) {
            this.hasLibraryCustomizations = hasLibraryCustomizations;
            return this;
        }
        
        public Builder libraryTemplates(Map<String, String> libraryTemplates) {
            this.libraryTemplates = libraryTemplates;
            return this;
        }
        
        public Builder libraryCustomizations(Map<String, String> libraryCustomizations) {
            this.libraryCustomizations = libraryCustomizations;
            return this;
        }
        
        public Builder libraryMetadata(Map<String, com.guidedbyte.openapi.modelgen.services.LibraryMetadata> libraryMetadata) {
            this.libraryMetadata = libraryMetadata;
            return this;
        }
        
        public TemplateConfiguration build() {
            return new TemplateConfiguration(this);
        }
    }
}