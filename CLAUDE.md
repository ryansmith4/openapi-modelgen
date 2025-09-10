# OpenAPI Model Generator Plugin

## Project Overview

Gradle plugin wrapping OpenAPI Generator with Java DTOs, Lombok support, custom templates, and performance optimizations.

## Key Features

- **Template Precedence**: user > plugin YAML > OpenAPI defaults
- **YAML Customizations**: Modify templates without full overrides
- **Multi-Level Caching**: 90% faster no-change builds, 70% faster incremental
- **Multi-Spec Support**: Parallel processing of multiple OpenAPI specs
- **Incremental Builds**: SHA-256 change detection, lazy extraction
- **Configuration Cache Compatible**: Full Gradle compatibility
- **Template Variables**: Recursive expansion ({{copyright}} → {{currentYear}})
- **Lombok Integration**: Full annotation support
- **Comprehensive Testing**: 112/112 tests passing

## ⚠️ CRITICAL: Configuration Cache Compatibility

**MANDATORY**: Plugin MUST remain configuration cache compatible.

### Requirements
1. **No `Project` in Actions**: Use `ProjectLayout`, `DirectoryProperty` instead
2. **Static Logging Only**: `LoggerFactory.getLogger(Class)`, never `project.getLogger()`
3. **Config vs Execution Separation**: All discovery at config time, serializable objects only
4. **Test with `--configuration-cache`**: All changes must pass cache tests
5. **Never Disable Tests**: Fix failures, don't bypass them

Breaking compatibility = critical regression requiring immediate fix.

## Project Structure

```text
openapi-modelgen/
├── plugin/                          # Main plugin
│   ├── src/main/java/.../           # Core classes
│   ├── src/main/resources/templateCustomizations/  # YAML customizations
│   └── src/test/java/.../           # Test suite
└── test-app/                        # STANDALONE test project
    ├── build.gradle                 # Configuration example
    └── src/main/resources/openapi-spec/  # API specs
```

## Build Commands

⚠️ **test-app is STANDALONE - ALWAYS use `cd test-app &&`**

**test-app tasks:**
- Generate: `cd test-app && /c/gradle/gradle-8.5/bin/gradle generatePets`
- Generate all: `cd test-app && /c/gradle/gradle-8.5/bin/gradle generateAllModels`
- Clean: `cd test-app && /c/gradle/gradle-8.5/bin/gradle generateClean`

**Plugin tasks:**
- Test: `/c/gradle/gradle-8.5/bin/gradle plugin:test`
- Build: `/c/gradle/gradle-8.5/bin/gradle clean build`

## Configuration

### DSL Syntax (method calls, not assignment)
```gradle
openapiModelgen {
    defaults {
        outputDir "build/generated"
        parallel true                  // Enable parallel processing
        applyPluginCustomizations true // Use built-in YAML customizations
        templateVariables([
            copyright: "© {{currentYear}} {{companyName}}"
        ])
        importMappings(['UUID': 'java.util.UUID'])
        typeMappings(['string+uuid': 'UUID'])
    }
    specs {
        pets { inputSpec "specs/pets.yaml" }
    }
}
```

### Template Variables
- **Built-in**: `{{currentYear}}`, `{{generatedBy}}`, `{{pluginVersion}}`
- **User**: Configurable with recursive expansion
- **Usage**: Available in all Mustache templates

## Template Architecture

**Template Precedence**: User templates > User YAML customizations > Plugin YAML > OpenAPI defaults

Templates processed in spec-specific working directories (`build/template-work/{generator}-{specName}/`) with multi-level caching (session → local → global).

## Key Technical Details

- **Composite build**: Plugin separate from test-app
- **Caching**: SHA-256 hashing, global persistence in `~/.gradle/caches/openapi-modelgen/`
- **Parallel processing**: Thread-safe multi-spec generation
- **Incremental builds**: Only regenerates on actual changes
- **Method-call DSL**: No `=` assignment syntax

## File Locations

- **Plugin**: `plugin/src/main/java/.../OpenApiModelGenPlugin.java`
- **Services**: `plugin/src/main/java/.../services/`
- **Tests**: `plugin/src/test/java/.../`
- **YAML customizations**: `plugin/src/main/resources/templateCustomizations/`
- **Generated output**: `test-app/build/generated-sources/openapi/`
- **Global cache**: `~/.gradle/caches/openapi-modelgen/`

## Common Issues

- **Build errors**: Use `includeBuild`, not `include 'plugin'`
- **Template syntax**: Use `{{#description}}` not `{{description}}`
- **Constructor conflicts**: `@NoArgsConstructor(force = true)` with `@AllArgsConstructor`

## Known Limitations

**OpenAPI Generator Bug**: `modelNamePrefix`/`modelNameSuffix` breaks `typeMappings`. 
**Workaround**: Don't use both together.

## ⚠️ CRITICAL COMMAND PATTERNS

❌ **WRONG**: `/c/gradle/gradle-8.5/bin/gradle test-app:generatePets`
✅ **CORRECT**: `cd test-app && /c/gradle/gradle-8.5/bin/gradle generatePets`

**test-app is standalone, not a subproject!**
