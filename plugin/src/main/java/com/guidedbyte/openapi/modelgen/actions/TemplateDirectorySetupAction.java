package com.guidedbyte.openapi.modelgen.actions;

import com.guidedbyte.openapi.modelgen.DefaultConfig;
import com.guidedbyte.openapi.modelgen.ResolvedSpecConfig;
import com.guidedbyte.openapi.modelgen.SpecConfig;
import com.guidedbyte.openapi.modelgen.TemplateConfiguration;
import com.guidedbyte.openapi.modelgen.services.TemplateResolver;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.ProjectLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Configuration-cache compatible action for setting up template directories.
 * This action creates necessary template directories without capturing non-serializable references.
 */
public class TemplateDirectorySetupAction implements Action<Task>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(TemplateDirectorySetupAction.class);
    
    private final Map<String, SpecConfig> specs;
    private final DefaultConfig defaults;
    private final ProjectLayout projectLayout;
    
    public TemplateDirectorySetupAction(Map<String, SpecConfig> specs, DefaultConfig defaults, ProjectLayout projectLayout) {
        this.specs = specs;
        this.defaults = defaults;
        this.projectLayout = projectLayout;
    }
    
    @Override
    public void execute(Task task) {
        // Create all necessary template directories
        specs.forEach((specName, specConfig) -> {
            try {
                ResolvedSpecConfig resolvedConfig = ResolvedSpecConfig.builder(specName, defaults, specConfig).build();
                TemplateResolver templateResolver = new TemplateResolver();
                TemplateConfiguration templateConfig = templateResolver.resolveTemplateConfiguration(
                    projectLayout, resolvedConfig, resolvedConfig.getGeneratorName(), Map.of());
                
                if (templateConfig.isTemplateProcessingEnabled() && templateConfig.getTemplateWorkDirectory() != null) {
                    File templateDir = new File(templateConfig.getTemplateWorkDirectory());
                    if (!templateDir.exists()) {
                        boolean created = templateDir.mkdirs();
                        if (created) {
                            logger.debug("Created template directory: {}", templateDir.getAbsolutePath());
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Error creating template directory for spec '{}': {}", specName, e.getMessage());
            }
        });
    }
}