package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.services.GenerateTaskPropertyBridge.FileProperty;
import com.guidedbyte.openapi.modelgen.tasks.PrepareTemplateDirectoryTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link GenerateTaskPropertyBridge}.
 *
 * <p>Runs against whatever OpenAPI Generator version is on the test classpath
 * (see {@code -PopenapiGeneratorVersion}), so under the CI matrix it exercises the
 * {@code FileSystemLocationProperty} branch on 7.22.0+ and the {@code Property<String>} branch
 * on older versions.</p>
 */
class GenerateTaskPropertyBridgeTest {

    private Project project;
    private GenerateTask task;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().build();
        task = project.getTasks().register("gen", GenerateTask.class).get();
    }

    @Test
    void inputSpecRoundTripsThroughSetFileAndAbsolutePath() {
        File spec = new File(project.getProjectDir(), "src/main/resources/pets.yaml").getAbsoluteFile();

        assertFalse(GenerateTaskPropertyBridge.isPresent(task, FileProperty.INPUT_SPEC),
            "inputSpec should be absent before it is set");

        GenerateTaskPropertyBridge.setFile(task, FileProperty.INPUT_SPEC, spec);

        assertTrue(GenerateTaskPropertyBridge.isPresent(task, FileProperty.INPUT_SPEC));
        assertEquals(spec.getAbsolutePath(),
            GenerateTaskPropertyBridge.absolutePath(task, FileProperty.INPUT_SPEC).get());
    }

    @Test
    void outputDirRoundTripsThroughSetFileAndAbsolutePath() {
        File outputDir = new File(project.getProjectDir(), "build/generated/sources/openapi/pets").getAbsoluteFile();

        assertFalse(GenerateTaskPropertyBridge.isPresent(task, FileProperty.OUTPUT_DIR));

        GenerateTaskPropertyBridge.setFile(task, FileProperty.OUTPUT_DIR, outputDir);

        assertTrue(GenerateTaskPropertyBridge.isPresent(task, FileProperty.OUTPUT_DIR));
        assertEquals(outputDir.getAbsolutePath(),
            GenerateTaskPropertyBridge.absolutePath(task, FileProperty.OUTPUT_DIR).get());
    }

    @Test
    void schemaLocationRoundTripsThroughSetFileAndAbsolutePath() {
        File schemas = new File(project.getProjectDir(), "src/main/resources/schemas").getAbsoluteFile();

        assertFalse(GenerateTaskPropertyBridge.isPresent(task, FileProperty.SCHEMA_LOCATION));

        GenerateTaskPropertyBridge.setFile(task, FileProperty.SCHEMA_LOCATION, schemas);

        assertTrue(GenerateTaskPropertyBridge.isPresent(task, FileProperty.SCHEMA_LOCATION));
        assertEquals(schemas.getAbsolutePath(),
            GenerateTaskPropertyBridge.absolutePath(task, FileProperty.SCHEMA_LOCATION).get());
    }

    @Test
    void absolutePathProviderIsAbsentWhenUnset() {
        assertFalse(GenerateTaskPropertyBridge.absolutePath(task, FileProperty.TEMPLATE_DIR).isPresent());
        assertEquals("not set",
            GenerateTaskPropertyBridge.absolutePath(task, FileProperty.TEMPLATE_DIR).getOrElse("not set"));
    }

    @Test
    void setLocationFromPrepareTaskOutputCarriesTaskDependency() {
        TaskProvider<PrepareTemplateDirectoryTask> prepare = project.getTasks()
            .register("prepareTemplates", PrepareTemplateDirectoryTask.class, t ->
                t.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir("template-work/spring-pets")));

        assertFalse(GenerateTaskPropertyBridge.isPresent(task, FileProperty.TEMPLATE_DIR));

        GenerateTaskPropertyBridge.setLocation(task, FileProperty.TEMPLATE_DIR,
            prepare.flatMap(PrepareTemplateDirectoryTask::getOutputDirectory));

        // Do not call get() or isPresent() here: Gradle rejects reading a task-output-derived provider
        // before the producing task has run, in both API generations. The dependency is the contract.
        Set<? extends Task> dependencies = task.getTaskDependencies().getDependencies(task);
        assertTrue(dependencies.contains(prepare.get()),
            "templateDir wired from the prepare task output must add the prepare task as a dependency, got: "
                + dependencies);
    }

    @Test
    void hasPropertyReportsDeclaredAndUndeclaredGetters() {
        assertTrue(GenerateTaskPropertyBridge.hasProperty(task, "getInputSpec"));
        assertTrue(GenerateTaskPropertyBridge.hasProperty(task, "getModelPackage"));
        assertFalse(GenerateTaskPropertyBridge.hasProperty(task, "getNoSuchPropertyOnAnyVersion"));
        // Cached answer must be stable
        assertFalse(GenerateTaskPropertyBridge.hasProperty(task, "getNoSuchPropertyOnAnyVersion"));
    }

    @Test
    void setIfPresentSetsScalarPropertyWhenDeclared() {
        assertTrue(GenerateTaskPropertyBridge.setIfPresent(task, "getModelPackage", "com.example.bridge"));
        assertEquals("com.example.bridge", task.getModelPackage().get());

        assertTrue(GenerateTaskPropertyBridge.setIfPresent(task, "getValidateSpec", Boolean.FALSE));
        assertFalse(task.getValidateSpec().get());
    }

    @Test
    void setIfPresentReturnsFalseWhenGetterMissing() {
        assertFalse(GenerateTaskPropertyBridge.setIfPresent(task, "getNoSuchPropertyOnAnyVersion", "ignored"));
        // Second call exercises the warn-once path and must still return false
        assertFalse(GenerateTaskPropertyBridge.setIfPresent(task, "getNoSuchPropertyOnAnyVersion", "ignored"));
    }

    @Test
    void lookupOfBogusGetterThrowsGradleExceptionNamingTheVersion() {
        GradleException ex = assertThrows(GradleException.class,
            () -> GenerateTaskPropertyBridge.lookup(task, "getBogusProperty"));
        assertTrue(ex.getMessage().contains("getBogusProperty"), ex.getMessage());
        assertTrue(ex.getMessage().contains("Detected generator version: "), ex.getMessage());
    }

    @Test
    void describeGeneratorVersionMatchesVersionUnderTest() {
        String described = GenerateTaskPropertyBridge.describeGeneratorVersion();
        String underTest = System.getProperty("openapi.generator.version");
        if (underTest != null && !underTest.isBlank()) {
            assertEquals(underTest, described);
        } else {
            assertTrue(described.matches("\\d+\\.\\d+\\.\\d+.*") || "unknown".equals(described), described);
        }
    }
}
