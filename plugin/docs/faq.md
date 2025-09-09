---
layout: page
title: Frequently Asked Questions
permalink: /faq/
---

# Frequently Asked Questions

## General Questions

### Q: What is the OpenAPI Model Generator plugin?

**A:** This is a comprehensive Gradle plugin that wraps the OpenAPI Generator with enhanced features specifically for generating Java DTOs. It adds multi-spec support, template customization via YAML, performance optimizations, and enterprise-grade features like template libraries and incremental builds.

### Q: How is this different from the standard OpenAPI Generator plugin?

**A:** Key differences include:
- **Multi-spec support**: Generate models from multiple OpenAPI specifications in one project
- **YAML template customization**: Modify templates without complete overrides
- **Template libraries**: Share customizations via dependency JARs
- **Performance optimization**: Multi-level caching, parallel processing, incremental builds
- **Enhanced validation**: Comprehensive configuration validation with detailed error messages
- **Enterprise features**: Corporate environment support, configuration cache compatibility

### Q: Do I still need the OpenAPI Generator plugin?

**A:** Yes, you must apply the OpenAPI Generator plugin first. This plugin enhances and wraps the OpenAPI Generator, it doesn't replace it:

```gradle
plugins {
    id 'org.openapi.generator' version '7.14.0'    // Required first
    id 'com.guidedbyte.openapi-modelgen' version '1.1.1'  // Then this plugin
}
```

## Compatibility Questions

### Q: Which OpenAPI Generator versions are supported?

**A:** The plugin supports OpenAPI Generator versions **7.10.0 and above**. It's tested with:
- 7.11.0 ✅ 
- 7.14.0 ✅ 
- 7.16.0+ ✅ (latest recommended)

The plugin automatically detects your OpenAPI Generator version and adapts accordingly.

### Q: What Java versions are required?

**A:** 
- **Java 17+** (Java 21 recommended for best performance)
- **Gradle 8.0+** (Gradle 8.5+ recommended)

### Q: Is this compatible with Gradle's configuration cache?

**A:** Yes! The plugin is fully compatible with Gradle's configuration cache. Use it for maximum build performance:

```bash
./gradlew generateAllModels --configuration-cache
```

### Q: Does this work with Spring Boot 3?

**A:** Yes, the plugin defaults to Spring Boot 3 configuration:
- Jakarta EE packages (not Java EE)
- Spring Boot 3.x compatibility
- Bean validation annotations
- Modern Java features

### Q: What OpenAPI generators are supported?

**A:** The plugin works with **any OpenAPI Generator**, including:
- **spring** (most common, default examples)
- **java** (plain Java POJOs)
- **kotlin** (Kotlin data classes)
- **typescript-angular**, **typescript-node**
- Any other generator that supports template customization

The plugin adapts to your chosen generator automatically.

## Configuration Questions

### Q: Why do I get compilation errors when using `=` in configuration?

**A:** This plugin uses **method-call syntax** instead of assignment syntax:

```gradle
✅ Correct - Method call syntax
openapiModelgen {
    defaults {
        outputDir "build/generated"     // Method call
        validateSpec true               // Method call
    }
}

❌ Wrong - Assignment syntax (compilation error)
openapiModelgen {
    defaults {
        outputDir = "build/generated"   // Will fail
        validateSpec = true             // Will fail
    }
}
```

This enables type validation, CLI overrides, and better error reporting.

### Q: Can I override configuration from the command line?

**A:** Yes, all configuration options support command-line overrides:

```bash
# Override output directory
./gradlew generateAllModels --output-dir=src/generated

# Enable validation  
./gradlew generateApi --validate-spec=true

# Override model package
./gradlew generateApi --model-package=com.custom.model
```

### Q: How do I configure multiple OpenAPI specifications?

**A:** Use the `specs` block to define multiple specifications:

```gradle
openapiModelgen {
    specs {
        users {
            inputSpec "src/main/resources/openapi/users.yaml"
            modelPackage "com.example.users.model"
        }
        
        orders {
            inputSpec "src/main/resources/openapi/orders.yaml" 
            modelPackage "com.example.orders.model"
        }
    }
}
```

Each spec gets its own generation task: `generateUsers`, `generateOrders`, plus `generateAllModels`. **Each spec also gets its own isolated template directory** to prevent cross-contamination between different specifications.

## Template Customization Questions

### Q: What's the difference between explicit templates and YAML customizations?

**A:** 
- **Explicit templates**: Complete `.mustache` files that replace the entire template
- **YAML customizations**: Surgical modifications (insertions, replacements) to existing templates

Use YAML customizations for most cases - they're easier to maintain and survive OpenAPI Generator updates.

### Q: Why aren't my YAML customizations working?

**A:** Common causes:

1. **Explicit template exists**: If you have both an explicit template AND YAML customization for the same template, the explicit template takes precedence and YAML is ignored.

2. **Wrong directory structure**: Templates must be in generator-specific subdirectories:
   ```text
   ✅ Correct
   src/main/resources/template-customizations/spring/pojo.mustache.yaml
   
   ❌ Wrong  
   src/main/resources/template-customizations/pojo.mustache.yaml
   ```

3. **Pattern not found**: Insertion patterns must exist in the base template. Check the working directory: `build/template-work/spring-{specName}/`

### Q: Do multiple specs with the same generator interfere with each other?

**A:** No! Each spec gets its own isolated template working directory:

- **Spec A**: `build/template-work/spring-users/`
- **Spec B**: `build/template-work/spring-orders/`

This prevents template cross-contamination and allows each spec to have different template customizations even when using the same generator (e.g., `spring`).

### Q: Can I share templates across projects?

**A:** Yes! Use template libraries:

1. **Create a library JAR** with templates in `META-INF/openapi-templates/` and customizations in `META-INF/openapi-customizations/`

2. **Add as dependency:**
   ```gradle
   dependencies {
       openapiCustomizations 'com.company:api-templates:1.0.0'
   }
   ```

3. **Enable library support:**
   ```gradle
   openapiModelgen {
       defaults {
           useLibraryTemplates true
           useLibraryCustomizations true
           templatePrecedence([
               'user-templates',
               'library-templates', 
               'user-customizations',
               'library-customizations',
               'plugin-customizations',
               'openapi-generator'
           ])
       }
   }
   ```

### Q: How do I debug template issues?

**A:** Enable debug logging:

```gradle
openapiModelgen {
    defaults {
        debug true
    }
}
```

```bash
# Generate with debug info
./gradlew generateApi --info

# Check template working directory (replace {specName} with your spec name)
ls -la build/template-work/spring-{specName}/
```

## Performance Questions

### Q: How can I speed up builds?

**A:** Several optimization strategies:

1. **Enable configuration cache:**
   ```bash
   ./gradlew generateAllModels --configuration-cache
   ```

2. **Use parallel processing** (enabled by default):
   ```gradle
   openapiModelgen {
       defaults {
           parallel true  // Default: true
       }
   }
   ```

3. **Skip unnecessary generation:**
   ```gradle
   openapiModelgen {
       defaults {
           generateModelTests false        // Skip if not needed
           generateApiDocumentation false  // Skip if not needed
           validateSpec false              // Skip in development (enable in CI)
       }
   }
   ```

4. **Optimize gradle.properties:**
   ```properties
   org.gradle.daemon=true
   org.gradle.parallel=true
   org.gradle.caching=true
   org.gradle.configuration-cache=true
   ```

### Q: Why is my first build slow but subsequent builds faster?

**A:** This is normal! The plugin uses multi-level caching:

1. **First build**: Extracts templates, processes customizations, builds cache
2. **Subsequent builds**: Uses cached templates and processing results
3. **No-change builds**: 90% faster due to global cache persistence

### Q: How does caching work?

**A:** The plugin uses a three-level cache hierarchy:

1. **Session cache**: Eliminates redundant work within single build
2. **Working directory cache**: Local project cache with SHA-256 validation
3. **Global cache**: Cross-project cache in `~/.gradle/caches/openapi-modelgen/`

Cache automatically invalidates when specs, templates, or plugin version changes.

## Troubleshooting Questions

### Q: Why do I get "Plugin not found" errors?

**A:** Ensure you apply the OpenAPI Generator plugin first:

```gradle
plugins {
    id 'org.openapi.generator' version '7.14.0'    // Must be first
    id 'com.guidedbyte.openapi-modelgen' version '1.1.1'
}
```

### Q: Why is no code generated?

**A:** Common causes:

1. **Empty OpenAPI spec**: Ensure your spec has `components.schemas`
2. **Wrong global properties**: Default generates models only (not APIs)
3. **Invalid spec**: Enable validation to check: `validateSpec true`

### Q: Why do generated models have compilation errors?

**A:** Usually missing dependencies:

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'io.swagger.core.v3:swagger-annotations:2.2.19'
    
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

### Q: How do I fix YAML parsing errors?

**A:** Common YAML issues:

```yaml
❌ Wrong indentation
insertions:
- after: "pattern"
content: "text"

✅ Correct indentation
insertions:
  - after: "pattern"
    content: "text"

❌ Unquoted Mustache patterns  
conditions:
  templateContains: {{#description}}

✅ Properly quoted
conditions:
  templateContains: "{{#description}}"
```

## Advanced Questions

### Q: Can I use this in a corporate environment?

**A:** Yes! The plugin is designed for enterprise use:

- **Corporate dependency management**: Works with corporate plugins that manage OpenAPI Generator versions
- **Template libraries**: Share corporate standards via dependency JARs  
- **Configuration validation**: Comprehensive validation with detailed error messages
- **Performance optimization**: Suitable for large projects with many specifications
- **CI/CD integration**: Full support for automated builds and validation

### Q: How do I migrate from the standard OpenAPI Generator plugin?

**A:** Migration steps:

1. **Keep existing OpenAPI Generator plugin** - don't remove it
2. **Add this plugin** after the OpenAPI Generator plugin
3. **Convert configuration** to method-call syntax (no `=` signs)
4. **Update task names** if needed (e.g., `openApiGenerate` → `generateApiName`)
5. **Test thoroughly** and leverage new features like multi-spec support

### Q: Can I contribute to this plugin?

**A:** Yes! Check the GitHub repository for contribution guidelines:
- **Bug reports**: Use GitHub Issues with minimal reproduction cases
- **Feature requests**: Propose enhancements through GitHub Discussions
- **Code contributions**: Follow the development guidelines in `CLAUDE.md`

### Q: Where can I get help?

**A:** Multiple support channels:

1. **Documentation**: [GitHub Pages Documentation](https://guidedbyte.github.io/openapi-modelgen/)
2. **Issues**: [GitHub Issues](https://github.com/guidedbyte/openapi-modelgen/issues) for bugs
3. **Discussions**: GitHub Discussions for usage questions
4. **Plugin help**: `./gradlew generateHelp` for configuration reference

## Version and Migration Questions

### Q: How do I check what version I'm using?

**A:** Several ways:

```bash
# Check plugin version
./gradlew buildEnvironment | grep openapi-modelgen

# Get comprehensive help (includes version info)  
./gradlew generateHelp

# Check dependencies
./gradlew dependencyInsight --dependency com.guidedbyte.openapi-modelgen
```

### Q: How do I upgrade to a newer version?

**A:** Update the version in your build.gradle:

```gradle
plugins {
    id 'com.guidedbyte.openapi-modelgen' version '1.1.1'  // Update version
}
```

Check the release notes for any breaking changes or new features.

### Q: Are there any breaking changes between versions?

**A:** The plugin follows semantic versioning:
- **Patch versions** (1.1.x): Bug fixes, no breaking changes
- **Minor versions** (1.x.0): New features, backward compatible  
- **Major versions** (x.0.0): May include breaking changes

Always check the release notes and migration guide when upgrading.

---

**Still have questions?** Check the [complete documentation](https://guidedbyte.github.io/openapi-modelgen/) or ask on [GitHub Discussions](https://github.com/guidedbyte/openapi-modelgen/discussions).