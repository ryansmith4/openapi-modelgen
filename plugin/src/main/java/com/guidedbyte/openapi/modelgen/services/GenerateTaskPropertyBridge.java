package com.guidedbyte.openapi.modelgen.services;

import com.guidedbyte.openapi.modelgen.constants.PluginConstants;
import com.guidedbyte.openapi.modelgen.util.PluginLoggerFactory;
import com.guidedbyte.openapi.modelgen.utils.VersionUtils;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileSystemLocationProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask;
import org.slf4j.Logger;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Version-neutral access to the file-shaped properties of OpenAPI Generator's {@link GenerateTask}.
 *
 * <p>OpenAPI Generator Gradle plugin 7.22.0 (upstream PR #23042) changed {@code inputSpec},
 * {@code outputDir}, {@code templateDir} and {@code schemaLocation} from {@code Property<String>}
 * to {@code RegularFileProperty} / {@code DirectoryProperty}. The JVM links methods by their full
 * descriptor, including the return type, so code compiled against the 7.10.0 API fails with
 * {@code NoSuchMethodError} on 7.22.0 and later. This bridge looks the getter up reflectively on
 * the public {@link GenerateTask} class and then branches on the runtime type of the returned
 * property, so one plugin artifact works on every generator from 7.10.0 onwards.</p>
 *
 * <p>In both API generations the getter returns a Gradle {@link Property}. On 7.22.0+ it
 * additionally implements {@link FileSystemLocationProperty}. Both are stable Gradle interfaces,
 * so the branch is an {@code instanceof} check rather than version sniffing.</p>
 *
 * <p>Reflection happens at configuration time and inside task actions. Neither path touches
 * {@code Project}, so the bridge is configuration-cache safe.</p>
 *
 * <p>The scalar helpers {@link #hasProperty(GenerateTask, String)} and
 * {@link #setIfPresent(GenerateTask, String, Object)} exist so that optional upstream properties
 * added after 7.10.0 (for example {@code workerIsolation} from 7.23.0) can be set when the running
 * generator declares them and skipped, with a single warning, when it does not.</p>
 *
 * @since 2.2.3
 */
final class GenerateTaskPropertyBridge {

    private static final Logger logger = PluginLoggerFactory.getLogger(GenerateTaskPropertyBridge.class);

    /** Per-getter cache of whether {@link GenerateTask} declares a public no-arg method with that name. */
    private static final Map<String, Boolean> DECLARED_GETTERS = new ConcurrentHashMap<>();

    /** Getter names for which the "property not supported on this generator" warning was already logged. */
    private static final Set<String> WARNED_MISSING = ConcurrentHashMap.newKeySet();

    private GenerateTaskPropertyBridge() {
    }

    /**
     * The file-shaped {@link GenerateTask} properties whose Java type differs between generator versions.
     *
     * <p>Each constant carries the getter name so that the string lives in exactly one place and
     * callers cannot pass arbitrary names.</p>
     */
    enum FileProperty {
        /** {@code inputSpec}: {@code Property<String>} before 7.22.0, {@code RegularFileProperty} after. */
        INPUT_SPEC("getInputSpec"),
        /** {@code outputDir}: {@code Property<String>} before 7.22.0, {@code DirectoryProperty} after. */
        OUTPUT_DIR("getOutputDir"),
        /** {@code templateDir}: {@code Property<String>} before 7.22.0, {@code DirectoryProperty} after. */
        TEMPLATE_DIR("getTemplateDir"),
        /**
         * {@code schemaLocation}: {@code Property<String>} before 7.22.0, {@code DirectoryProperty} after.
         * Declared for completeness; not used by any 2.2.3 call site.
         */
        SCHEMA_LOCATION("getSchemaLocation");

        private final String getterName;

        FileProperty(String getterName) {
            this.getterName = getterName;
        }

        /** Returns the public getter name on {@link GenerateTask}. */
        String getterName() {
            return getterName;
        }
    }

    /**
     * Sets a file-shaped property from a File. Works for {@code RegularFileProperty},
     * {@code DirectoryProperty} and {@code Property<String>}.
     *
     * <p>Callers should pass an absolute file so both API generations behave identically.
     * The legacy branch stores {@link File#getAbsolutePath()}.</p>
     *
     * @param task the generate task
     * @param property the property to set
     * @param file the file or directory
     */
    static void setFile(GenerateTask task, FileProperty property, File file) {
        Object p = lookup(task, property.getterName());
        if (p instanceof FileSystemLocationProperty<?> fsp) {
            fsp.fileValue(file);
        } else if (p instanceof Property<?> prop) {
            @SuppressWarnings("unchecked")
            Property<String> stringProp = (Property<String>) prop;
            stringProp.set(file.getAbsolutePath());
        } else {
            throw unsupported(property.getterName(), p);
        }
    }

    /**
     * Sets a file-shaped property from a lazy location, preserving task-dependency wiring.
     *
     * <p>When the provider derives from another task's output (for example a prepare task's
     * {@code @OutputDirectory}), the producer dependency is carried through on both API generations.</p>
     *
     * @param task the generate task
     * @param property the property to set
     * @param location the lazy file or directory location
     */
    static void setLocation(GenerateTask task, FileProperty property,
                            Provider<? extends FileSystemLocation> location) {
        Object p = lookup(task, property.getterName());
        if (p instanceof FileSystemLocationProperty<?> fsp) {
            fsp.fileProvider(location.map(FileSystemLocation::getAsFile));
        } else if (p instanceof Property<?> prop) {
            @SuppressWarnings("unchecked")
            Property<String> stringProp = (Property<String>) prop;
            stringProp.set(location.map(l -> l.getAsFile().getAbsolutePath()));
        } else {
            throw unsupported(property.getterName(), p);
        }
    }

    /**
     * Reads a file-shaped property as an absolute-path provider. The provider is absent when the
     * property is unset.
     *
     * <p>The returned provider carries no task-producer information, so it can be read inside the
     * task's own {@code upToDateWhen} spec and actions even for {@code outputDir}, which the
     * generate task declares as its own output. (A plain {@code map()} over such a property is
     * rejected by Gradle with "Querying the mapped value ... before task has completed".)</p>
     *
     * <p>Do not read the returned provider at configuration time when the property was wired from
     * another task's output (the template directory); Gradle rejects both {@code get()} and
     * {@code isPresent()} in that situation, in both API generations.</p>
     *
     * @param task the generate task
     * @param property the property to read
     * @return a provider of the absolute path
     */
    static Provider<String> absolutePath(GenerateTask task, FileProperty property) {
        Object p = lookup(task, property.getterName());
        if (p instanceof FileSystemLocationProperty<?> fsp) {
            // getLocationOnly() has no task dependencies or producer, so mapping it is safe to read
            // during the owning task's execution.
            return fsp.getLocationOnly().map(location -> location.getAsFile().getAbsolutePath());
        } else if (p instanceof Provider<?> prov) {
            // Pre-7.22 the getter is Property<String>; return it unchanged rather than mapping it,
            // because a mapped view of the task's own outputDir cannot be read until the task completes.
            @SuppressWarnings("unchecked")
            Provider<String> stringProvider = (Provider<String>) prov;
            return stringProvider;
        }
        throw unsupported(property.getterName(), p);
    }

    /**
     * Returns true if the file-shaped property has a value.
     *
     * @param task the generate task
     * @param property the property to check
     * @return whether a value is present
     */
    static boolean isPresent(GenerateTask task, FileProperty property) {
        Object p = lookup(task, property.getterName());
        if (p instanceof Provider<?> prov) {
            return prov.isPresent();
        }
        throw unsupported(property.getterName(), p);
    }

    /**
     * Returns true if {@link GenerateTask} declares a public no-arg method with this name.
     * The answer is cached per getter name.
     *
     * @param task the generate task (its runtime type is not consulted; the lookup is against the public upstream class)
     * @param getterName the getter name, for example {@code "getWorkerIsolation"}
     * @return whether the getter exists on the running generator version
     */
    static boolean hasProperty(GenerateTask task, String getterName) {
        return DECLARED_GETTERS.computeIfAbsent(getterName, name -> {
            try {
                GenerateTask.class.getMethod(name);
                return Boolean.TRUE;
            } catch (NoSuchMethodException e) {
                return Boolean.FALSE;
            }
        });
    }

    /**
     * Sets a scalar {@code Property<T>} if the running generator declares the getter.
     *
     * <p>Returns false, and logs once at warn level, when the getter does not exist. This is the
     * primitive for exposing upstream knobs that were added after the minimum supported generator.</p>
     *
     * @param task the generate task
     * @param getterName the getter name, for example {@code "getWorkerIsolation"}
     * @param value the value to set
     * @param <T> the property type
     * @return true if the property was set, false if the generator does not declare it
     */
    static <T> boolean setIfPresent(GenerateTask task, String getterName, T value) {
        if (!hasProperty(task, getterName)) {
            if (WARNED_MISSING.add(getterName)) {
                logger.warn("OpenAPI Generator {} does not support GenerateTask.{}(); the setting is ignored. "
                    + "Upgrade the generator to use it.", describeGeneratorVersion(), getterName);
            }
            return false;
        }
        Object p = lookup(task, getterName);
        if (p instanceof Property<?> prop) {
            @SuppressWarnings("unchecked")
            Property<T> typed = (Property<T>) prop;
            typed.set(value);
            return true;
        }
        throw unsupported(getterName, p);
    }

    /**
     * Invokes the named public no-arg getter on the task and returns the property object.
     *
     * <p>The lookup is against {@link GenerateTask} itself rather than {@code task.getClass()} so it
     * resolves the public upstream method and not Gradle's generated {@code _Decorated} subclass.</p>
     *
     * @param task the generate task
     * @param getterName the getter name
     * @return the property object returned by the getter
     * @throws GradleException if the getter cannot be found or invoked
     */
    static Object lookup(GenerateTask task, String getterName) {
        try {
            Method getter = GenerateTask.class.getMethod(getterName);
            return getter.invoke(task);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new GradleException("Unsupported OpenAPI Generator GenerateTask API: cannot access "
                + getterName + "(). Detected generator version: " + describeGeneratorVersion()
                + ". This plugin supports OpenAPI Generator 7.10.0 and later.", e);
        }
    }

    private static GradleException unsupported(String getterName, Object property) {
        return new GradleException("Unsupported OpenAPI Generator GenerateTask API: " + getterName
            + "() returned " + (property == null ? "null" : property.getClass().getName())
            + ", expected a Gradle Property. Detected generator version: " + describeGeneratorVersion());
    }

    /**
     * Best-effort description of the generator plugin version on the classpath, for error messages.
     * Delegates to {@link VersionUtils#detectOpenApiGeneratorVersionFromClasspath()} so the plugin's
     * apply-time version check and these messages can never disagree.
     */
    static String describeGeneratorVersion() {
        String version = VersionUtils.detectOpenApiGeneratorVersionFromClasspath();
        return version != null ? version : PluginConstants.UNKNOWN_VERSION;
    }
}
