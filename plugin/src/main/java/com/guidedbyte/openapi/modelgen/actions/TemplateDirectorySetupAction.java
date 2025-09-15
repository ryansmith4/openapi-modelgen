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
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = {"EI_EXPOSE_REP2", "REC_CATCH_EXCEPTION"},
    justification = "EI_EXPOSE_REP2: Gradle action class stores configuration objects passed by Gradle framework for execution phase. " +
                   "The configuration objects are immutable at execution time and are part of Gradle's configuration cache mechanism. " +
                   "REC_CATCH_EXCEPTION: Defensive programming in template setup - catches all exceptions to prevent one spec " +
                   "from failing template directory setup for all specs. Non-critical setup failures are logged and ignored."
)
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
                
                if (templateConfig.isTemplateProcessingEnabled() && templateConfig.getTemplateWorkDir() != null) {
                    File templateDir = new File(templateConfig.getTemplateWorkDir());
                    if (!templateDir.exists()) {
                        boolean created = templateDir.mkdirs();
                        if (created) {
                            logger.debug("Created template directory: {}", templateDir.getAbsolutePath());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error creating template directory for spec '{}': {}", specName, e.getMessage());
            }
        });
    }
}