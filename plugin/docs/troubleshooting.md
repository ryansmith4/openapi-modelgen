---
layout: page
title: Troubleshooting Guide
permalink: /troubleshooting/
---

# Troubleshooting Guide

This guide helps you diagnose and resolve common issues with the OpenAPI Model Generator plugin.

## Quick Diagnostics

### Check Plugin Status

```bash
# Verify plugin is applied correctly
./gradlew generateHelp

# Check available tasks
./gradlew tasks --group="openapi generation"

# Enable debug logging
./gradlew generateAllModels --info --stacktrace
```

### Validate Configuration

```bash
# Test configuration without generating code
./gradlew generateAllModels --dry-run

# Check for configuration cache issues
./gradlew generateAllModels --configuration-cache
```

## Build and Configuration Issues

### Issue: Plugin Not Found or Not Applied

**Symptoms:**
- `Plugin with id 'com.guidedbyte.openapi-modelgen' not found`
- No generation tasks available

**Solution:**
```gradle
plugins {
    // ✅ REQUIRED: Apply OpenAPI Generator plugin first
    id 'org.openapi.generator' version '7.14.0'

    // ✅ Then apply this plugin
    id 'com.guidedbyte.openapi-modelgen' version '1.1.1'
}
```

**Common Mistakes:**
```gradle
❌ Wrong order - this plugin must come after OpenAPI Generator
plugins {
    id 'com.guidedbyte.openapi-modelgen' version '1.1.1'
    id 'org.openapi.generator' version '7.14.0'  // Too late!
}

❌ Missing OpenAPI Generator plugin entirely
plugins {
    id 'com.guidedbyte.openapi-modelgen' version '2.1.0'  // Will fail
}
```

### Issue: Configuration Syntax Errors

**Symptoms:**
- Compilation errors in build.gradle
- "Cannot set property" or "Cannot invoke method" errors

**Problem:** Using assignment syntax instead of method calls

```gradle
❌ Incorrect - Assignment syntax
openapiModelgen {
    defaults {
        outputDir = "build/generated"           // Compilation error
        validateSpec = true                     // Compilation error
    }
}

✅ Correct - Method call syntax
openapiModelgen {
    defaults {
        outputDir "build/generated"             // Method call
        validateSpec true                       // Method call
    }
}
```

### Issue: Configuration Validation Errors

**Symptoms:**
- Detailed validation error messages during configuration phase
- "Configuration validation failed" errors

**Common Validation Issues:**

1. **Missing required properties:**
   ```gradle
   ❌ Problem: Missing required inputSpec or modelPackage
   specs {
       api {
           // Missing inputSpec and modelPackage
       }
   }

   ✅ Solution: Provide required properties
   specs {
       api {
           inputSpec "src/main/resources/openapi/api.yaml"
           modelPackage "com.example.model"
       }
   }
   ```

1. **Invalid package names:**
   ```gradle
   ❌ Problem: Invalid Java package name
   specs {
       api {
           inputSpec "api.yaml"
           modelPackage "invalid-package-name!"  // Hyphens not allowed
       }
   }

   ✅ Solution: Use valid Java package name
   specs {
       api {
           inputSpec "api.yaml"
           modelPackage "com.example.api.model"  // Valid package name
       }
   }
   ```

1. **File not found errors:**
   ```gradle
   ❌ Problem: OpenAPI spec file doesn't exist
   specs {
       api {
           inputSpec "nonexistent.yaml"          // File not found
           modelPackage "com.example.model"
       }
   }

   ✅ Solution: Verify file exists and path is correct
   specs {
       api {
           inputSpec "src/main/resources/openapi/api.yaml"  // Correct path
           modelPackage "com.example.model"
       }
   }
   ```

## Generation Issues

### Issue: No Code Generated

**Symptoms:**
- Tasks run successfully but no files are generated
- Empty output directories

**Diagnostic Steps:**

1. **Check OpenAPI specification:**
   ```bash
   # Validate your OpenAPI spec
   ./gradlew generateApi --validate-spec=true --info
   ```

1. **Verify output directory:**
   ```bash
   # Check if output directory is created
   ls -la build/generated/sources/openapi/src/main/java/
   ```

1. **Check rich debug log:**
   ```bash
   # View detailed generation information
   cat build/logs/openapi-modelgen-debug.log | grep -i "generating"
   ```

**Common Causes:**

1. **No schemas in OpenAPI spec:**
   ```yaml
   ❌ Problem: Empty or missing components section
   openapi: 3.0.3
   info:
     title: API
     version: 1.0.0
   paths: {}
   # Missing components.schemas section

   ✅ Solution: Add schemas to components section
   openapi: 3.0.3
   info:
     title: API
     version: 1.0.0
   paths: {}
   components:
     schemas:
       User:
         type: object
         properties:
           name:
             type: string
   ```

1. **Wrong global properties:**
   ```gradle
   ❌ Problem: APIs generated instead of models
   globalProperties([
       models: "false",  // Disables model generation
       apis: ""          // Enables API generation
   ])

   ✅ Solution: Generate models only (default)
   globalProperties([
       models: "",       // Enables model generation
       apis: "false"     // Disables API generation (default)
   ])
   ```

### Issue: Compilation Errors in Generated Code

**Symptoms:**
- Generated Java code doesn't compile
- Missing imports or annotations

**Common Causes:**

1. **Missing dependencies:**
   ```gradle
   ✅ Add required dependencies
   dependencies {
       implementation 'org.springframework.boot:spring-boot-starter-web'
       implementation 'org.springframework.boot:spring-boot-starter-validation'
       implementation 'io.swagger.core.v3:swagger-annotations:2.2.19'

       compileOnly 'org.projectlombok:lombok'
       annotationProcessor 'org.projectlombok:lombok'
   }
   ```

1. **Lombok configuration issues:**
   ```gradle
   ✅ Verify Lombok annotations are correctly configured
   openapiModelgen {
       defaults {
           configOptions([
               additionalModelTypeAnnotations: "@lombok.Data;@lombok.experimental.Accessors(fluent = true);@lombok.experimental.SuperBuilder;@lombok.NoArgsConstructor(force = true);@lombok.AllArgsConstructor"
           ])
       }
   }
   ```

1. **Spring Boot version compatibility:**
   ```gradle
   ✅ Ensure Spring Boot 3.x configuration (default)
   openapiModelgen {
       defaults {
           configOptions([
               useSpringBoot3: "true",      // Spring Boot 3.x
               useJakartaEe: "true"         // Jakarta EE (not Java EE)
           ])
       }
   }
   ```

### Issue: Wrong Java Version in Generated Code

**Symptoms:**
- Generated code uses old Java EE instead of Jakarta EE
- Missing modern Java features

**Solution:**
```gradle
openapiModelgen {
    defaults {
        configOptions([
            useSpringBoot3: "true",         // Enable Spring Boot 3.x
            useJakartaEe: "true",          // Use Jakarta EE packages
            dateLibrary: "java8"            // Use Java 8+ date/time
        ])
    }
}
```

## Template Customization Issues

### Issue: Template Customizations Ignored

**Symptoms:**
- YAML customizations don't appear in generated code
- Templates seem to use defaults instead of customizations

**Diagnostic Steps:**

1. **Check for explicit templates:**
   ```bash
   # Look for explicit templates that might override YAML customizations
   find src/main/resources -name "*.mustache" -type f
   ```

1. **Verify directory structure:**
   ```text
   ✅ Correct structure (generator-specific directories)
   src/main/resources/template-customizations/
   └── spring/                              # Generator name directory
       ├── pojo.mustache.yaml              # YAML customization
       └── model.mustache.yaml

   ❌ Incorrect structure (missing generator directory)
   src/main/resources/template-customizations/
   ├── pojo.mustache.yaml                  # Wrong location
   └── model.mustache.yaml
   ```

1. **Check rich debug log:**
   ```bash
   # View template resolution details (always captured)
   cat build/logs/openapi-modelgen-debug.log

   # Filter for template customization entries
   grep -i "customization" build/logs/openapi-modelgen-debug.log
   ```

**Common Causes:**

1. **Explicit template overrides YAML:**
   ```text
   ❌ Problem: Both explicit template and YAML customization exist
   src/main/resources/openapi-templates/spring/pojo.mustache      # Explicit template
   src/main/resources/template-customizations/spring/pojo.mustache.yaml  # YAML (ignored!)

   ✅ Solution: Remove explicit template or YAML customization
   src/main/resources/template-customizations/spring/pojo.mustache.yaml  # YAML used
   ```

1. **Wrong generator directory:**
   ```gradle
   ❌ Problem: Templates in wrong directory for your generator
   # Your configuration uses 'spring' generator
   openapiModelgen {
       defaults {
           generatorName "spring"           # Uses 'spring' directory
       }
   }

   # But templates are in 'java' directory
   src/main/resources/template-customizations/java/pojo.mustache.yaml

   ✅ Solution: Move to correct generator directory
   src/main/resources/template-customizations/spring/pojo.mustache.yaml
   ```

### Issue: YAML Parsing Errors

**Symptoms:**
- Build fails with YAML syntax errors
- "Invalid YAML structure" messages

**Common YAML Issues:**

1. **Indentation problems:**
   ```yaml
   ❌ Incorrect indentation
   insertions:
   - after: "{{#description}}"
   content: |
     Some content

   ✅ Correct indentation
   insertions:
     - after: "{{#description}}"
       content: |
         Some content
   ```

1. **Missing quotes:**
   ```yaml
   ❌ Unquoted Mustache patterns
   conditions:
     templateContains: {{#description}}     # YAML parsing error

   ✅ Properly quoted patterns
   conditions:
     templateContains: "{{#description}}"  # Correct
   ```

1. **Invalid YAML characters:**
   ```yaml
   ❌ Unescaped special characters
   content: |
     String value = "Hello: World";        # Colon in string causes issues

   ✅ Properly escaped or quoted
   content: |
     String value = "Hello\\: World";      # Escaped
   # OR
   content: 'String value = "Hello: World";'  # Single quoted
   ```

### Issue: Template Patterns Not Found

**Symptoms:**
- YAML customizations seem valid but aren't applied
- Rich debug log shows patterns not matching

**Diagnostic Steps:**

1. **Examine base templates:**
   ```bash
   # Check working directory to see actual templates (replace {specName} with your spec name)
   ls -la build/template-work/spring-{specName}/
   cat build/template-work/spring-{specName}/pojo.mustache
   ```

1. **Verify patterns exist:**
   ```bash
   # Search for pattern in base template
   grep -n "{{#description}}" build/template-work/spring-{specName}/pojo.mustache
   ```

**Solutions:**

1. **Check exact pattern matching:**
   ```yaml
   ❌ Pattern doesn't exist in template
   insertions:
     - after: "{{#nonExistentPattern}}"     # Pattern not in base template

   ✅ Use patterns that exist in base template
   insertions:
     - after: "{{#description}}"            # Pattern exists in base template
   ```

1. **Case sensitivity:**
   ```yaml
   ❌ Wrong case
   insertions:
     - after: "{{#Description}}"            # Capital 'D'

   ✅ Correct case
   insertions:
     - after: "{{#description}}"            # Lowercase 'd'
   ```

## Performance Issues

### Issue: Slow Build Times

**Symptoms:**
- Generation tasks take a long time to complete
- Builds seem slower after adding the plugin

**Diagnostic Steps:**

1. **Profile build performance:**
   ```bash
   # Use build scan to identify bottlenecks
   ./gradlew generateAllModels --scan

   # Time specific tasks
   time ./gradlew generateAllModels
   ```

1. **Check cache effectiveness:**
   ```bash
   # Enable configuration cache
   ./gradlew generateAllModels --configuration-cache

   # Run twice to test cache hit
   ./gradlew generateAllModels --configuration-cache  # Should be faster second time
   ```

**Performance Optimizations:**

1. **Enable parallel processing:**
   ```gradle
   openapiModelgen {
       defaults {
           parallel true  # Enable parallel multi-spec processing (default)
       }
   }
   ```

1. **Use configuration cache:**
   ```bash
   # Add to gradle.properties
   org.gradle.configuration-cache=true

   # Or use command line
   ./gradlew generateAllModels --configuration-cache
   ```

1. **Optimize template customizations:**
   ```yaml
   ✅ Use specific patterns for faster processing
   insertions:
     - after: "import {{#jackson}}"         # Specific pattern

   ❌ Avoid broad patterns
   insertions:
     - after: "import"                     # Too broad, slower processing
   ```

### Issue: Excessive Memory Usage

**Symptoms:**
- OutOfMemoryError during generation
- Gradle daemon running out of memory

**Solution:**
```bash
# Increase Gradle daemon memory
export GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"

# Or add to gradle.properties
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
```

## Configuration Cache Issues

### Issue: Configuration Cache Incompatibility

**Symptoms:**
- "Configuration cache problems found" messages
- Serialization errors during configuration cache

**The plugin is designed to be configuration cache compatible. If you encounter issues:**

1. **Update to latest version:**
   ```gradle
   plugins {
       id 'com.guidedbyte.openapi-modelgen' version '1.1.1'  // Latest version
   }
   ```

1. **Check for custom extensions:**
   ```gradle
   ❌ Custom closures may break configuration cache
   openapiModelgen {
       specs {
           api {
               inputSpec "api.yaml"
               modelPackage "com.example.model"
               // Avoid custom closures or non-serializable objects
           }
       }
   }
   ```

1. **Report configuration cache issues:**
   If you encounter configuration cache problems with the latest version, please report them with:
   ```bash
   ./gradlew generateAllModels --configuration-cache --stacktrace > cache-issue.log
   ```

## Version Compatibility Issues

### Issue: OpenAPI Generator Version Conflicts

**Symptoms:**
- "Unsupported OpenAPI Generator version" warnings
- Generation failures with newer OpenAPI Generator versions

**Solutions:**

1. **Check supported versions:**
   ```text
   Supported: OpenAPI Generator 7.10.0+
   Tested with: 7.11.0, 7.14.0, 7.16.0+
   ```

1. **Update OpenAPI Generator:**
   ```gradle
   plugins {
       id 'org.openapi.generator' version '7.14.0'  // Recommended version
       id 'com.guidedbyte.openapi-modelgen' version '1.1.1'
   }
   ```

1. **Override dependency if needed:**
   ```gradle
   dependencies {
       implementation 'org.openapitools:openapi-generator:7.14.0'
   }
   ```

### Issue: Java Version Incompatibility

**Symptoms:**
- Compilation errors related to Java version
- Unsupported class file version errors

**Requirements:**
- **Java 17+** (Java 21 recommended)
- **Gradle 8.0+** (Gradle 8.5+ recommended)

**Solution:**
```gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)  // Or 21
    }
}
```

## Getting Help

### Access Comprehensive Debug Information

The plugin automatically captures detailed debug logging to help troubleshoot template resolution, customization processing, and configuration issues.

#### Rich Debug Log File

No configuration needed - debug information is always captured:

```bash
# View complete debug log (always available)
cat build/logs/openapi-modelgen-debug.log

# Filter by specific spec name
grep "[spec:api]" build/logs/openapi-modelgen-debug.log

# Search for template-related entries
grep -i "template" build/logs/openapi-modelgen-debug.log

# View customization processing
grep -i "customization" build/logs/openapi-modelgen-debug.log
```

#### Console Output Control

Use standard Gradle flags to control console verbosity:

```bash
# Detailed console output
./gradlew generateAllModels --info

# Full debug console output
./gradlew generateAllModels --debug

# Minimal console output
./gradlew generateAllModels --quiet
```

#### Debug Output Examples

**Template Resolution Debug:**
```
=== Template Resolution Debug for 'spring' ===
Configured template sources: [user-templates, user-customizations, openapi-generator]
Available template sources: [user-customizations, openapi-generator]
✅ user-customizations: C:\project\src\main\templateCustomizations
✅ openapi-generator: OpenAPI Generator default templates (fallback)
=== End Template Resolution Debug ===
```

**Template Processing Debug:**
```
=== CUSTOMIZATION ENGINE ENTRY ===
Template length: 2547
Config name: pojo.mustache
Config has replacements: 2
Template starts with: '{{>licenseInfo}}{{#models}}{{#model}}'
```

#### Common Debug Commands

```bash
# View rich debug log with all details
cat build/logs/openapi-modelgen-debug.log

# Template-specific debugging
grep -i "template" build/logs/openapi-modelgen-debug.log

# Configuration validation details
./gradlew generateAllModels --dry-run --info

# Watch template customization processing
grep -i "customization" build/logs/openapi-modelgen-debug.log

# Filter by specific spec
grep "[spec:myApi]" build/logs/openapi-modelgen-debug.log
```

### Useful Debug Information

When reporting issues, include:

1. **Plugin and dependency versions:**
   ```bash
   ./gradlew buildEnvironment | grep openapi
   ./gradlew dependencyInsight --dependency org.openapitools
   ```

1. **Configuration details:**
   ```bash
   ./gradlew generateHelp > config-help.txt
   ```

1. **Template working directory:**
   ```bash
   find build/template-work -name "*.mustache" -exec echo "=== {} ===" \; -exec cat {} \;
   ```

### Common Command Reference

```bash
# Generate with verbose output
./gradlew generateAllModels --info --stacktrace

# Clean and regenerate everything
./gradlew clean generateAllModels

# Test configuration cache compatibility
./gradlew generateAllModels --configuration-cache

# Get plugin help and configuration options
./gradlew generateHelp

# Profile build performance
./gradlew generateAllModels --scan --profile

# Validate configuration without generating
./gradlew generateAllModels --dry-run
```

### Community Support

- **GitHub Issues**: [Report bugs and request features](https://github.com/guidedbyte/openapi-modelgen/issues)
- **Discussions**: Join community discussions for usage questions
- **Documentation**: Complete guides at [GitHub Pages](https://guidedbyte.github.io/openapi-modelgen/)

When reporting issues, please include:
- Plugin version
- OpenAPI Generator version
- Java and Gradle versions
- Minimal reproduction case
- Complete error messages and stack traces
- Rich debug log content (if applicable)