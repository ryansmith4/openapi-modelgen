package com.guidedbyte.openapi.modelgen.customization;

/**
 * Optional metadata information for template customizations.
 * 
 * @since 2.0.0
 */
public class CustomizationMetadata {
    private String name;
    private String description;
    private String version;
    private String author;
    
    /**
     * Creates a new CustomizationMetadata instance.
     */
    public CustomizationMetadata() {}
    
    /**
     * Gets the name of this customization.
     * @return the customization name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the name of this customization.
     * @param name the customization name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Gets the description of this customization.
     * @return the customization description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the description of this customization.
     * @param description the customization description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Gets the version of this customization.
     * @return the customization version
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Sets the version of this customization.
     * @param version the customization version
     */
    public void setVersion(String version) {
        this.version = version;
    }
    
    /**
     * Gets the author of this customization.
     * @return the customization author
     */
    public String getAuthor() {
        return author;
    }
    
    /**
     * Sets the author of this customization.
     * @param author the customization author
     */
    public void setAuthor(String author) {
        this.author = author;
    }
}