# Troubleshooting Guide

This guide covers common issues, solutions, debug options, and known limitations for the OpenAPI Model Generator plugin.

## Table of Contents

- [Common Issues](#common-issues)
- [Build Errors](#build-errors)
- [Configuration Issues](#configuration-issues)
- [Template Issues](#template-issues)
- [Performance Issues](#performance-issues)
- [Debug Options](#debug-options)
- [Known Limitations](#known-limitations)
- [Error Messages](#error-messages)
- [Getting Help](#getting-help)

## Common Issues

### Plugin Not Found

**Symptoms**:
```
Plugin [id: 'com.guidedbyte.openapi-modelgen'] was not found
```

**Solution**:
1. Verify plugin is published to Gradle Plugin Portal
2. Check version number is correct
3. Ensure OpenAPI Generator plugin is applied first:

```gradle
plugins {
    id 'org.openapi.generator' version '7.14.0'  // Must be first
    id 'com.guidedbyte.openapi-modelgen' version '@version@'
}
```

### Generation Tasks Not Created

**Symptoms**:
- No `generatePets` tasks available
- `generateAllModels` task not found

**Solution**:
1. Check plugin is applied correctly
2. Verify specs are configured:
```gradle
openapiModelgen {
    specs {
        pets {  // This creates generatePets task
            inputSpec "specs/pets.yaml"
            modelPackage "com.example.pets"
        }
    }
}
```

### Generated Code Not Compiled

**Symptoms**:
- Models generated but compilation fails
- Classes not found at runtime

**Solution**:
1. Add generated sources to sourceSets:
```gradle
sourceSets {
    main {
        java {
            srcDirs += file("build/generated/sources/openapi/src/main/java")
        }
    }
}
```

2. Ensure generation runs before compilation:
```gradle
compileJava.dependsOn generateAllModels
```

## Build Errors

### Syntax Errors in Configuration

**Symptoms**:
```
Could not set unknown property 'outputDir' for extension 'openapiModelgen'
```

**Cause**: Using assignment syntax instead of method calls

**Solution**: Use method-call syntax:
```gradle
// ❌ INCORRECT
openapiModelgen {
    defaults {
        outputDir = "build/generated"  // Assignment syntax fails
    }
}

// ✅ CORRECT
openapiModelgen {
    defaults {
        outputDir "build/generated"    // Method call syntax
    }
}
```

### OpenAPI Generator Version Issues

**Symptoms**:
```
OpenAPI Generator version 7.9.0 is not supported. Minimum version: 7.10.0
```

**Solution**:
1. Update OpenAPI Generator plugin:
```gradle
plugins {
    id 'org.openapi.generator' version '7.14.0'  // Use 7.10.0+
}
```

2. For corporate environments, ensure your dependency management provides 7.10.0+

### Composite Build Issues

**Symptoms**:
```
Project with path ':plugin' could not be found
```

**Solution**:
1. Use `includeBuild`, not `include`:
```gradle
// settings.gradle
includeBuild 'plugin'  // ✅ CORRECT for composite build

// ❌ INCORRECT
include 'plugin'  // Don't use for composite builds
```

## Configuration Issues

### Missing Required Properties

**Symptoms**:
```
inputSpec is required for spec 'pets'
modelPackage is required for spec 'pets'
```

**Solution**:
Ensure all specs have required properties:
```gradle
specs {
    pets {
        inputSpec "src/main/resources/openapi-spec/pets.yaml"  // Required
        modelPackage "com.example.pets.model"                 // Required
    }
}
```

### Invalid Package Names

**Symptoms**:
```
Invalid package name: 'com.example.123invalid'
```

**Solution**:
Use valid Java package naming:
```gradle
specs {
    pets {
        modelPackage "com.example.pets"     // ✅ Valid
        // modelPackage "com.123invalid"    // ❌ Invalid - starts with number
        // modelPackage "com.example.class" // ❌ Invalid - reserved keyword
    }
}
```

### Template Directory Issues

**Symptoms**:
```
Template directory does not exist: src/main/resources/templates
Generator subdirectory not found: src/main/resources/templates/spring
```

**Solution**:
1. Ensure template directories exist:
```bash
mkdir -p src/main/resources/openapi-templates/spring
mkdir -p src/main/resources/template-customizations/spring
```

2. Use correct generator subdirectories:
```text
src/main/resources/
├── openapi-templates/
│   └── spring/              # Must match generatorName
│       └── pojo.mustache
└── template-customizations/
    └── spring/              # Must match generatorName
        └── pojo.mustache.yaml
```

## Template Issues

### Customizations Ignored

**Symptoms**:
- YAML customizations don't appear in generated code
- Expected modifications missing

**Troubleshooting**:
1. Check if explicit user template exists:
```bash
# If this exists, YAML customizations are ignored
ls src/main/resources/openapi-templates/spring/pojo.mustache
```

2. Verify generator subdirectory:
```bash
# Should be in generator-specific directory
ls src/main/resources/template-customizations/spring/pojo.mustache.yaml
```

3. Enable debug logging:
```bash
./gradlew generatePets --debug | grep -i template
```

### YAML Parsing Errors

**Symptoms**:
```
Error parsing YAML customization: pojo.mustache.yaml
Invalid YAML syntax at line 5
```

**Solution**:
1. Validate YAML syntax:
```bash
# Test YAML syntax
python -c "import yaml; yaml.safe_load(open('src/main/resources/template-customizations/spring/pojo.mustache.yaml'))"
```

2. Check common YAML issues:
```yaml
# ❌ INCORRECT - Indentation issue
insertions:
- before: "{{#description}}"
  content: |
    // Comment
      // Inconsistent indentation

# ✅ CORRECT - Consistent indentation
insertions:
  - before: "{{#description}}"
    content: |
      // Comment
      // Consistent indentation
```

### Pattern Matching Issues

**Symptoms**:
- YAML insertions/replacements don't apply
- Patterns not found in templates

**Solution**:
1. Save original templates for comparison:
```gradle
openapiModelgen {
    defaults {
        saveOriginalTemplates true
    }
}
```

2. Compare patterns with actual template content:
```bash
# Generate with original templates saved
./gradlew generatePets

# View original template
cat build/template-work/spring-pets/orig/pojo.mustache

# Check your pattern matches exactly
grep -n "{{#description}}" build/template-work/spring-pets/orig/pojo.mustache
```

3. Use exact pattern matching:
```yaml
# Make sure pattern exists exactly in template
insertions:
  - before: "{{#description}}"  # Must match exactly, including whitespace
    content: |
      // Custom comment
```

### Template Spacing Issues

**Symptoms**:
- Generated code has incorrect indentation
- Extra or missing whitespace

**Solution**:
1. Use explicit indentation indicators in YAML:
```yaml
insertions:
  - after: "public class {{classname}}"
    content: |2  # Preserve exactly 2 spaces

      // Custom field with exact indentation
      private String customField;
```

2. Use quoted strings for precise control:
```yaml
insertions:
  - after: "{{#serializableModel}}"
    content: "  @java.io.Serial\n"  # Exactly 2 spaces and newline
```

## Performance Issues

### Slow Builds

**Symptoms**:
- Generation takes longer than expected
- Memory usage is high

**Troubleshooting**:
1. Check if parallel processing is enabled:
```gradle
openapiModelgen {
    defaults {
        parallel true  // Should be enabled
    }
}
```

2. Profile the build:
```bash
./gradlew generateAllModels --profile
# Check build/reports/profile/ for timing details
```

3. Clear cache if corrupted:
```bash
rm -rf ~/.gradle/caches/openapi-modelgen/
./gradlew clean generateAllModels
```

**Solution**:
See [Performance Guide](performance.md) for detailed optimization.

### Memory Issues

**Symptoms**:
```
OutOfMemoryError: Java heap space
```

**Solution**:
1. Increase Gradle memory:
```bash
export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"
./gradlew generateAllModels
```

2. Disable parallel processing temporarily:
```bash
./gradlew generateAllModels --no-parallel
```

3. Generate specs individually:
```bash
./gradlew generatePets
./gradlew generateOrders
# Instead of generateAllModels
```

## Debug Options

### Enable Debug Logging

**Full debug output**:
```bash
./gradlew generatePets --debug
```

**Info level logging**:
```bash
./gradlew generatePets --info
```

**Quiet output for CI**:
```bash
./gradlew generatePets --quiet
```

### Template Debugging

**Save original templates**:
```gradle
openapiModelgen {
    defaults {
        saveOriginalTemplates true
    }
}
```

**View template processing**:
```bash
./gradlew generatePets --debug | grep -E "(template|customization)"
```

**Compare templates**:
```bash
# After generation with saveOriginalTemplates=true
diff build/template-work/spring-pets/orig/pojo.mustache \
     build/template-work/spring-pets/pojo.mustache
```

### Configuration Debugging

**Show effective configuration**:
```bash
./gradlew generateHelp
```

**Test CLI overrides**:
```bash
./gradlew generatePets --model-package=com.test --debug
```

**Validate configuration**:
```bash
./gradlew generatePets --dry-run
```

### Cache Debugging

**Inspect cache contents**:
```bash
# View global cache
find ~/.gradle/caches/openapi-modelgen/ -type f | head -20

# View working directory cache
find build/template-work/ -type f | head -20
```

**Clear specific caches**:
```bash
# Clear global cache
rm -rf ~/.gradle/caches/openapi-modelgen/

# Clear working directory cache
rm -rf build/template-work/

# Clear everything
./gradlew clean
```

## Known Limitations

### OpenAPI Generator Limitations

The plugin itself works correctly, but there are known bugs in the underlying OpenAPI Generator that affect certain configuration combinations:

#### Type/Import Mappings with Model Name Prefixes/Suffixes

**Affected Versions**: OpenAPI Generator 5.4.0+ (including 7.x series)

**Problem**: When using `modelNamePrefix` or `modelNameSuffix` together with `typeMappings` and `importMappings`, the prefix/suffix gets incorrectly applied to the type mapping targets, causing the import mappings to fail.

**Symptoms**:
- Instead of `java.time.LocalDate`, you get `ApiLocalDate` or `LocalDateDto` wrapper models
- Import statements show package-local models instead of configured external types

**Upstream Issues**:
- [OpenAPITools/openapi-generator#19043](https://github.com/OpenAPITools/openapi-generator/issues/19043)
- [OpenAPITools/openapi-generator#11478](https://github.com/OpenAPITools/openapi-generator/issues/11478)

**Workarounds**:
1. **Choose one or the other**: Use either prefixes/suffixes OR type mappings, but not both
2. **Use schemaMappings**: Replace `importMappings` with `schemaMappings` where possible
3. **Redundant configuration**: Define mappings for both original and prefixed types

**Example affected configuration**:
```gradle
openapiModelgen {
    defaults {
        modelNamePrefix "Api"                    // This causes the issue
        typeMappings([
            'string+date': 'LocalDate'           // Gets incorrectly prefixed to ApiLocalDate
        ])
        importMappings([
            'LocalDate': 'java.time.LocalDate'   // Fails to match ApiLocalDate
        ])
    }
}
```

### Gradle Limitations

#### Configuration Cache Compatibility

**Important**: The plugin MUST remain configuration cache compatible.

**Requirements**:
- No `Project` references in task actions
- Use `ProjectLayout`, `DirectoryProperty` instead
- Static logging only: `LoggerFactory.getLogger(Class)`
- Serializable configuration objects only

**Testing**:
```bash
./gradlew generatePets --configuration-cache
```

#### Incremental Build Limitations

**File watching limitations**:
- Gradle doesn't watch files outside project directory
- External template libraries won't trigger rebuilds automatically
- Manual clean required after library updates

**Workaround**:
```bash
./gradlew clean generateAllModels  # After library updates
```

## Error Messages

### Common Error Patterns

**"Plugin not found"**:
```
Plugin [id: 'com.guidedbyte.openapi-modelgen'] was not found
```
- Check plugin version exists
- Verify Gradle Plugin Portal access
- Ensure OpenAPI Generator plugin is applied first

**"Method not found"**:
```
Could not find method outputDir() for arguments [build/generated]
```
- Check plugin is applied correctly
- Verify using method-call syntax, not assignment
- Ensure configuration is inside `openapiModelgen` block

**"Configuration not found"**:
```
Could not get unknown property 'openapiCustomizations'
```
- Plugin not applied or applied incorrectly
- Check plugin block order

**"Template not found"**:
```
Template 'pojo.mustache' not found in generator
```
- Verify generator name is correct
- Check OpenAPI Generator version compatibility
- Ensure generator supports the template

**"YAML parsing error"**:
```
Error parsing YAML customization: Invalid YAML syntax
```
- Check YAML syntax and indentation
- Verify file encoding (must be UTF-8)
- Check for tabs vs spaces consistency

### Error Recovery

**General recovery steps**:
1. **Clean build**:
   ```bash
   ./gradlew clean
   ```

2. **Clear caches**:
   ```bash
   rm -rf ~/.gradle/caches/openapi-modelgen/
   rm -rf build/template-work/
   ```

3. **Regenerate with debug**:
   ```bash
   ./gradlew generatePets --debug --stacktrace
   ```

4. **Test minimal configuration**:
   ```gradle
   openapiModelgen {
       specs {
           test {
               inputSpec "spec.yaml"
               modelPackage "com.test"
           }
       }
   }
   ```

## Getting Help

### Before Reporting Issues

1. **Search existing issues**: [GitHub Issues](https://github.com/guidedbyte/openapi-modelgen/issues)

2. **Test with minimal configuration**:
   ```gradle
   openapiModelgen {
       specs {
           test {
               inputSpec "path/to/spec.yaml"
               modelPackage "com.example.test"
           }
       }
   }
   ```

3. **Collect debug information**:
   ```bash
   ./gradlew generatePets --debug --stacktrace > debug.log 2>&1
   ```

4. **Check versions**:
   ```bash
   ./gradlew --version
   java -version
   ```

### Information to Include

When reporting issues, include:

1. **Plugin version**
2. **OpenAPI Generator version**
3. **Gradle version**
4. **Java version**
5. **Minimal reproduction case**
6. **Complete error message with stack trace**
7. **Relevant configuration**

### Community Resources

- **Documentation**: [GitHub Pages](https://guidedbyte.github.io/openapi-modelgen/)
- **Template Schema**: [Schema Documentation](https://guidedbyte.github.io/openapi-modelgen/template-schema.html)
- **Issues**: [GitHub Issues](https://github.com/guidedbyte/openapi-modelgen/issues)
- **Discussions**: [GitHub Discussions](https://github.com/guidedbyte/openapi-modelgen/discussions)

### Commercial Support

For enterprise support and custom development:
- Contact: [GuidedByte Technologies](mailto:support@guidedbyte.com)
- Response time: 1-2 business days
- Includes: Configuration assistance, custom template development, performance optimization