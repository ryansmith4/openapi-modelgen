package com.guidedbyte.openapi.modelgen;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard for OpenAPI Generator 7.22+ compatibility.
 *
 * <p>Main code must never link directly to the file-shaped {@code GenerateTask} getters whose
 * return type changed in OpenAPI Generator 7.22.0 ({@code getInputSpec}, {@code getOutputDir},
 * {@code getTemplateDir}, {@code getSchemaLocation}). A direct call compiles against the 7.10.0
 * descriptor and throws {@code NoSuchMethodError} on 7.22+. All access has to go through
 * {@code GenerateTaskPropertyBridge}, which looks the getter up reflectively and therefore leaves
 * no method reference in the constant pool.</p>
 *
 * <p>A source grep cannot do this job because the plugin's own DSL classes ({@code SpecConfig},
 * {@code DefaultConfig}) legitimately declare getters with the same names. This test parses the
 * constant pool of every compiled main class and fails on any {@code Methodref} or
 * {@code InterfaceMethodref} whose owner is {@code GenerateTask} and whose name is guarded.</p>
 */
class NoDirectGenerateTaskFilePropertyAccessTest {

    private static final String GENERATE_TASK_INTERNAL_NAME =
        "org/openapitools/generator/gradle/plugin/tasks/GenerateTask";

    private static final Set<String> GUARDED_GETTERS = Set.of(
        "getInputSpec", "getOutputDir", "getTemplateDir", "getSchemaLocation");

    @Test
    void mainClassesDoNotLinkToGenerateTaskFileProperties() throws IOException, URISyntaxException {
        Path mainClasses = Paths.get(OpenApiModelGenPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        List<String> violations = new ArrayList<>();
        int scanned;
        if (Files.isDirectory(mainClasses)) {
            scanned = scanDirectory(mainClasses, violations);
        } else {
            scanned = scanJar(mainClasses, violations);
        }

        assertTrue(scanned > 20, "Expected to scan the plugin's main classes under " + mainClasses + ", scanned " + scanned);
        assertTrue(violations.isEmpty(),
            "Main code must access these GenerateTask properties through GenerateTaskPropertyBridge only "
                + "(direct calls break on OpenAPI Generator 7.22+):\n  " + String.join("\n  ", violations));
    }

    private static int scanDirectory(Path root, List<String> violations) throws IOException {
        int scanned = 0;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".class"))::iterator) {
                try (InputStream in = Files.newInputStream(file)) {
                    collectViolations(root.relativize(file).toString(), in, violations);
                }
                scanned++;
            }
        }
        return scanned;
    }

    private static int scanJar(Path jar, List<String> violations) throws IOException {
        int scanned = 0;
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream in = jarFile.getInputStream(entry)) {
                        collectViolations(entry.getName(), in, violations);
                    }
                    scanned++;
                }
            }
        }
        return scanned;
    }

    /**
     * Parses the class-file constant pool (JVMS section 4.4) and records every method reference
     * to a guarded {@code GenerateTask} getter.
     */
    private static void collectViolations(String className, InputStream raw, List<String> violations) throws IOException {
        DataInputStream in = new DataInputStream(raw);
        int magic = in.readInt();
        if (magic != 0xCAFEBABE) {
            throw new IOException("Not a class file: " + className);
        }
        in.readUnsignedShort(); // minor
        in.readUnsignedShort(); // major
        int count = in.readUnsignedShort();

        String[] utf8 = new String[count];
        int[] classNameIndex = new int[count];      // CONSTANT_Class -> name_index
        int[] refClassIndex = new int[count];       // Methodref/InterfaceMethodref -> class_index
        int[] refNameAndTypeIndex = new int[count]; // Methodref/InterfaceMethodref -> name_and_type_index
        int[] natNameIndex = new int[count];        // NameAndType -> name_index
        boolean[] isMethodRef = new boolean[count];

        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1 -> utf8[i] = in.readUTF();
                case 3, 4 -> in.readInt();
                case 5, 6 -> {
                    in.readLong();
                    i++; // long and double take two constant pool slots
                }
                case 7 -> classNameIndex[i] = in.readUnsignedShort();
                case 8, 16, 19, 20 -> in.readUnsignedShort();
                case 9 -> {
                    in.readUnsignedShort();
                    in.readUnsignedShort();
                }
                case 10, 11 -> {
                    refClassIndex[i] = in.readUnsignedShort();
                    refNameAndTypeIndex[i] = in.readUnsignedShort();
                    isMethodRef[i] = true;
                }
                case 12 -> {
                    natNameIndex[i] = in.readUnsignedShort();
                    in.readUnsignedShort(); // descriptor_index
                }
                case 15 -> {
                    in.readUnsignedByte();
                    in.readUnsignedShort();
                }
                case 17, 18 -> {
                    in.readUnsignedShort();
                    in.readUnsignedShort();
                }
                default -> throw new IOException("Unknown constant pool tag " + tag + " in " + className);
            }
        }

        for (int i = 1; i < count; i++) {
            if (!isMethodRef[i]) {
                continue;
            }
            String owner = utf8[classNameIndex[refClassIndex[i]]];
            String name = utf8[natNameIndex[refNameAndTypeIndex[i]]];
            if (GENERATE_TASK_INTERNAL_NAME.equals(owner) && GUARDED_GETTERS.contains(name)) {
                violations.add(className + " calls GenerateTask." + name + "()");
            }
        }
    }
}
