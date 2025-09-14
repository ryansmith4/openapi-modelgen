package com.guidedbyte.openapi.modelgen;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

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
 *   <li>Debug flag for comprehensive plugin troubleshooting</li>
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
    @Serial
    private static final long serialVersionUID = 1L;
    
    private final String generatorName;
    private final String templateWorkDir;
    private final boolean hasUserTemplates;
    private final boolean hasUserCustomizations;
    private final boolean hasPluginCustomizations;
    private final String userTemplateDirectory;
    private final String userCustomizationsDirectory;
    private final Map<String, String> templateVariables;
    private final boolean templateProcessingEnabled;
    private final List<String> templateSources;
    private final boolean debug;
    private final boolean saveOriginalTemplates;
    
    // Library template support
    private final boolean hasLibraryTemplates;
    private final boolean hasLibraryCustomizations;
    private final Map<String, String> libraryTemplates;
    private final Map<String, String> libraryCustomizations;
    private final Map<String, com.guidedbyte.openapi.modelgen.services.LibraryMetadata> libraryMetadata;
    
    private TemplateConfiguration(Builder builder) {
        this.generatorName = builder.generatorName;
        this.templateWorkDir = builder.templateWorkDir;
        this.hasUserTemplates = builder.hasUserTemplates;
        this.hasUserCustomizations = builder.hasUserCustomizations;
        this.hasPluginCustomizations = builder.hasPluginCustomizations;
        this.userTemplateDirectory = builder.userTemplateDirectory;
        this.userCustomizationsDirectory = builder.userCustomizationsDirectory;
        this.templateVariables = builder.templateVariables != null ? 
            Collections.unmodifiableMap(new HashMap<>(builder.templateVariables)) : Collections.emptyMap();
        this.templateProcessingEnabled = builder.templateProcessingEnabled;
        this.templateSources = builder.templateSources != null ?
            Collections.unmodifiableList(builder.templateSources) : Collections.emptyList();
        this.debug = builder.debug;
        this.saveOriginalTemplates = builder.saveOriginalTemplates;
        
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
    
    @Input
    public String getGeneratorName() { return generatorName; }
    @Internal
    public String getTemplateWorkDir() { return templateWorkDir; }
    @Input
    public boolean getHasUserTemplates() { return hasUserTemplates; }
    @Input
    public boolean getHasUserCustomizations() { return hasUserCustomizations; }
    @Input
    public boolean getHasPluginCustomizations() { return hasPluginCustomizations; }
    
    // Keep original method names for compatibility
    public boolean hasUserTemplates() { return hasUserTemplates; }
    public boolean hasUserCustomizations() { return hasUserCustomizations; }
    public boolean hasPluginCustomizations() { return hasPluginCustomizations; }
    @Input
    @Optional
    public String getUserTemplateDirectory() { return userTemplateDirectory; }
    
    /**
     * Gets the user template directory as a file input for Gradle's incremental build tracking.
     * This ensures that changes to files in the user template directory properly invalidate the task.
     * We track the entire user template directory to catch both direct templates and generator-specific subdirectories.
     */
    @InputDirectory
    @Optional
    @org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    public java.io.File getUserTemplateDirectoryAsFile() { 
        if (userTemplateDirectory == null) {
            return null;
        }
        java.io.File dir = new java.io.File(userTemplateDirectory);
        return dir.exists() && dir.isDirectory() ? dir : null;
    }
    @Input
    @Optional
    public String getUserCustomizationsDirectory() { return userCustomizationsDirectory; }
    @Input
    public Map<String, String> getTemplateVariables() { return templateVariables != null ? Collections.unmodifiableMap(templateVariables) : null; }
    @Input
    public boolean isTemplateProcessingEnabled() { return templateProcessingEnabled; }
    @Input
    public List<String> getTemplateSources() { return templateSources != null ? Collections.unmodifiableList(templateSources) : null; }
    @Input
    public boolean isDebug() { return debug; }
    @Input
    public boolean isSaveOriginalTemplates() { return saveOriginalTemplates; }
    
    // Library template getters
    @Input
    public boolean getHasLibraryTemplates() { return hasLibraryTemplates; }
    @Input
    public boolean getHasLibraryCustomizations() { return hasLibraryCustomizations; }
    
    // Keep original method names for compatibility
    public boolean hasLibraryTemplates() { return hasLibraryTemplates; }
    public boolean hasLibraryCustomizations() { return hasLibraryCustomizations; }
    @Input
    public Map<String, String> getLibraryTemplates() { return libraryTemplates != null ? Collections.unmodifiableMap(libraryTemplates) : null; }
    @Input
    public Map<String, String> getLibraryCustomizations() { return libraryCustomizations != null ? Collections.unmodifiableMap(libraryCustomizations) : null; }
    @Internal  // Internal metadata used for processing, not part of task inputs
    public Map<String, com.guidedbyte.openapi.modelgen.services.LibraryMetadata> getLibraryMetadata() { return libraryMetadata != null ? Collections.unmodifiableMap(libraryMetadata) : null; }
    
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
                debug == that.debug &&
                saveOriginalTemplates == that.saveOriginalTemplates &&
                Objects.equals(generatorName, that.generatorName) &&
                Objects.equals(templateWorkDir, that.templateWorkDir) &&
                Objects.equals(userTemplateDirectory, that.userTemplateDirectory) &&
                Objects.equals(userCustomizationsDirectory, that.userCustomizationsDirectory) &&
                Objects.equals(templateVariables, that.templateVariables) &&
                Objects.equals(templateSources, that.templateSources);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(generatorName, templateWorkDir, hasUserTemplates, hasUserCustomizations,
                hasPluginCustomizations, userTemplateDirectory,
                userCustomizationsDirectory, templateVariables, templateProcessingEnabled,
                templateSources, debug, saveOriginalTemplates);
    }
    
    @Override
    public String toString() {
        return "TemplateConfiguration{" +
                "generatorName='" + generatorName + '\'' +
                ", templateWorkDir='" + templateWorkDir + '\'' +
                ", hasUserTemplates=" + hasUserTemplates +
                ", hasUserCustomizations=" + hasUserCustomizations +
                ", hasPluginCustomizations=" + hasPluginCustomizations +
                ", templateProcessingEnabled=" + templateProcessingEnabled +
                '}';
    }
    
    public static class Builder {
        private final String generatorName;
        private String templateWorkDir;
        private boolean hasUserTemplates;
        private boolean hasUserCustomizations;
        private boolean hasPluginCustomizations;
        private String userTemplateDirectory;
        private String userCustomizationsDirectory;
        private Map<String, String> templateVariables;
        private boolean templateProcessingEnabled = true;
        private List<String> templateSources;
        private boolean debug = false;
        private boolean saveOriginalTemplates = false;
        
        // Library support
        private boolean hasLibraryTemplates = false;
        private boolean hasLibraryCustomizations = false;
        private Map<String, String> libraryTemplates;
        private Map<String, String> libraryCustomizations;
        private Map<String, com.guidedbyte.openapi.modelgen.services.LibraryMetadata> libraryMetadata;
        
        private Builder(String generatorName) {
            this.generatorName = Objects.requireNonNull(generatorName, "generatorName must not be null");
        }
        
        public Builder templateWorkDir(String templateWorkDir) {
            this.templateWorkDir = templateWorkDir;
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
            this.templateVariables = templateVariables != null ? new HashMap<>(templateVariables) : null;
            return this;
        }
        
        public Builder templateProcessingEnabled(boolean templateProcessingEnabled) {
            this.templateProcessingEnabled = templateProcessingEnabled;
            return this;
        }
        
        public Builder templateSources(List<String> templateSources) {
            this.templateSources = templateSources != null ? new ArrayList<>(templateSources) : null;
            return this;
        }
        
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }
        
        public Builder saveOriginalTemplates(boolean saveOriginalTemplates) {
            this.saveOriginalTemplates = saveOriginalTemplates;
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
            this.libraryTemplates = libraryTemplates != null ? new HashMap<>(libraryTemplates) : null;
            return this;
        }
        
        public Builder libraryCustomizations(Map<String, String> libraryCustomizations) {
            this.libraryCustomizations = libraryCustomizations != null ? new HashMap<>(libraryCustomizations) : null;
            return this;
        }
        
        public Builder libraryMetadata(Map<String, com.guidedbyte.openapi.modelgen.services.LibraryMetadata> libraryMetadata) {
            this.libraryMetadata = libraryMetadata != null ? new HashMap<>(libraryMetadata) : null;
            return this;
        }
        
        public TemplateConfiguration build() {
            return new TemplateConfiguration(this);
        }
    }
}