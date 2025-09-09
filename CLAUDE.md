# OpenAPI Model Generator Plugin

## Project Overview

A comprehensive Gradle plugin that wraps the OpenAPI Generator with enhanced features for generating Java DTOs with
Lombok support, custom templates, and enterprise-grade performance optimizations.

## Key Features

- **Template Precedence System**: user templates > plugin YAML customizations > OpenAPI generator defaults
- **Template Customization Engine**: YAML-based template modifications with comprehensive validation
- **Recursive Template Discovery**: Automatic extraction of only necessary templates and their dependencies
- **Partial Template Override**: Only provide templates you want to customize, rely on OpenAPI Generator for the rest
- **Incremental Build Support**: Only regenerates when inputs actually change
- **Configuration Validation**: Comprehensive validation with detailed error reporting
- **Dynamic Template Discovery**: Automatic detection of custom templates for forward compatibility
- **Content-Based Change Detection**: SHA-256 hashing for reliable template change detection
- **Lazy Template Extraction**: Templates extracted at execution time for better performance
- **@Internal Property Optimization**: Precise incremental build invalidation
- **Nested Template Variables**: Recursive expansion (e.g., `{{copyright}}` containing `{{currentYear}}`)
- **Multi-Level Caching System**: Comprehensive caching across session → local → global levels with thread safety
- **Template Extraction Caching**: High-performance template caching with metadata and cross-build persistence
- **Lombok Integration**: Full annotation support with constructor conflict resolution
- **Multi-Spec Support**: Generate DTOs from multiple OpenAPI specifications
- **Spec-Level Configuration**: Individual validation and test/doc generation settings per specification
- **Command Line Options**: @Option annotations for CLI parameter overrides
- **Comprehensive Documentation**: Auto-generated help and full Javadoc documentation
- **Parallel Multi-Spec Processing**: Thread-safe parallel processing with configurable parallel execution control
- **Cross-Build Performance Optimization**: Global cache persistence with 90% faster no-change builds and 70% faster incremental builds
- **Integration Tests**: Complete test coverage with 112/112 tests passing (100% success rate, all functionality fully verified)
- **Clean Task Support**: `generateClean` task removes generated models and invalidates all template caches

## ⚠️ **CRITICAL ARCHITECTURAL REQUIREMENT**

**GRADLE CONFIGURATION CACHE COMPATIBILITY IS MANDATORY**

This plugin is fully compatible with Gradle's configuration cache and **MUST** remain so. All future changes must adhere to the following requirements:

### **Configuration Cache Requirements**

1. **NO `Project` REFERENCES IN ACTIONS OR SERIALIZABLE CLASSES**
   - Task actions that implement `Action<Task>` MUST NOT capture `Project` references
   - All serializable classes MUST NOT contain `Project` or other non-serializable Gradle objects
   - Use `ProjectLayout`, `DirectoryProperty`, or other configuration cache compatible alternatives

2. **STATIC LOGGING ONLY**
   - Use static SLF4J loggers: `private static final Logger logger = LoggerFactory.getLogger(ClassName.class);`
   - **NEVER** use `project.getLogger()` in task actions or serializable classes
   - Project loggers are acceptable ONLY during configuration phase in plugin classes

3. **CONFIGURATION TIME VS EXECUTION TIME SEPARATION**
   - All template resolution, path resolution, and discovery MUST happen at configuration time
   - Task actions should only receive pre-resolved, serializable configuration objects
   - No dynamic discovery or project access during task execution

4. **TESTING REQUIREMENT**
   - All changes MUST be tested with `--configuration-cache` enabled
   - Verify cache hit/miss behavior and ensure proper serialization
   - Example test command: `./gradlew generatePets --configuration-cache`

5. **TEST INTEGRITY REQUIREMENT**
   - **NEVER disable or skip tests** without explicit confirmation from the user
   - Tests MUST NOT be disabled, ignored, or bypassed during development
   - If tests fail, they MUST be fixed rather than disabled
   - Any test modification requires user approval before implementation
   - Test failures indicate real issues that must be resolved

### **Configuration Cache Compatible Services**

The following services are designed to be configuration cache compatible:

- ✅ `TaskConfigurationService` - Stateless task creation and configuration with TemplateCacheManager dependency
- ✅ `TemplateOrchestrator` - Template processing coordination with session-level caching
- ✅ `CustomizationEngine` - No project dependencies, uses static logging
- ✅ `TemplateCacheManager` - Multi-level caching with thread safety
- ✅ `ConfigurationValidator` - Stateless validation with comprehensive error reporting
- ✅ `LibraryProcessor` - Library template processing (Phase 1)
- ✅ `TemplateDiscoveryService` - Project-free template extraction 
- ✅ `TemplateResolver` - Resolves configurations at configuration time
- ✅ `ConditionEvaluator` - Stateless condition evaluation
- ✅ `TemplateProcessor` - Project-free template processing
- ✅ All `Action<Task>` implementations - Use only serializable state

### **Violation Detection**

If configuration cache compatibility is broken:
- Gradle will show: `Configuration cache problems found` 
- Look for: `cannot serialize object of type` errors
- Common culprits: `Project`, `Logger` from project, non-serializable closures

**Breaking configuration cache compatibility is considered a critical architectural regression and must be fixed immediately.**

## Project Structure

```text
openapi-modelgen/
├── plugin/                          # Main Gradle plugin implementation
│   ├── src/main/java/com/guidedbyte/openapi/modelgen/
│   │   ├── OpenApiModelGenPlugin.java        # Core plugin with advanced optimizations
│   │   ├── OpenApiModelGenExtension.java     # DSL extension with multi-spec support
│   │   ├── DefaultConfig.java                # Global defaults configuration
│   │   └── SpecConfig.java                   # Individual spec configuration
│   ├── src/main/resources/templates/spring/  # (Empty - no embedded Mustache templates)
│   │   └── pojo2.mustache              # Renamed from pojo.mustache for testing
│   ├── src/main/resources/templateCustomizations/spring/  # YAML template customizations
│   │   └── pojo.mustache.yaml          # Template customization configuration
│   └── src/test/java/com/guidedbyte/openapi/modelgen/  # Comprehensive test suite
│       ├── PluginFunctionalTest.java         # Unit tests with ProjectBuilder
│       ├── WorkingIntegrationTest.java       # TestKit integration tests
│       ├── TemplatePrecedenceUnitTest.java   # Template precedence testing
│       ├── ConfigurationValidationTest.java  # Configuration validation tests
│       ├── PerformanceIntegrationTest.java   # Performance and incremental tests
│       ├── EndToEndCustomizationTest.java    # Template customization integration tests
│       ├── services/CustomizationEngineTest.java  # YAML customization engine tests
│       └── TestSummary.md                    # Test coverage documentation
└── test-app/                        # Test application using the plugin
    ├── build.gradle                 # Plugin configuration example
    └── src/main/resources/openapi-spec/  # OpenAPI specifications
        ├── pets.yaml                # Pet store API specification
        └── orders.yaml              # Orders API specification
```

## Build Commands

**⚠️ CRITICAL: test-app is a STANDALONE project - ALWAYS use `cd test-app &&` prefix**

- **Generate models**: `cd test-app && /c/gradle/gradle-8.5/bin/gradle generatePets`
- **Generate all**: `cd test-app && /c/gradle/gradle-8.5/bin/gradle generateAllModels`
- **Clean generated**: `cd test-app && /c/gradle/gradle-8.5/bin/gradle generateClean`
- **Clean build**: `/c/gradle/gradle-8.5/bin/gradle clean build`
- **Test plugin**: `/c/gradle/gradle-8.5/bin/gradle plugin:test`
- **Generate docs**: `/c/gradle/gradle-8.5/bin/gradle plugin:generatePluginDocs`
- **Plugin help**: `cd test-app && /c/gradle/gradle-8.5/bin/gradle generateHelp`

### Project Structure Rules

- **plugin/** - Main plugin project (use `/c/gradle/gradle-8.5/bin/gradle plugin:taskName`)
- **test-app/** - **STANDALONE** project (NEVER use `test-app:taskName` - ALWAYS use `cd test-app &&`)
- **Root project** - Composite build that includes plugin/ only (test-app is independent)

## Current Configuration

### Lombok Annotations (test-app/build.gradle)

```gradle
additionalModelTypeAnnotations: "@lombok.Data;@lombok.experimental.Accessors(fluent = true);@lombok.experimental.SuperBuilder;@lombok.NoArgsConstructor(force = true);@lombok.AllArgsConstructor"
```

### OpenAPI Generator Mappings

The plugin supports comprehensive mapping configuration:

- **importMappings**: Map type names to fully qualified import statements (merged between defaults and specs)
- **typeMappings**: Map OpenAPI types to Java types with format support (e.g., 'string+uuid': 'UUID')  
- **additionalProperties**: Pass generator-specific options equivalent to --additional-properties CLI option

Example configuration:
```gradle
defaults {
    importMappings([
        'UUID': 'java.util.UUID',
        'LocalDateTime': 'java.time.LocalDateTime'
    ])
    typeMappings([
        'string+uuid': 'UUID',
        'string+date-time': 'LocalDateTime'
    ])
    additionalProperties([
        'library': 'spring-boot',
        'useSpringBoot3': 'true'
    ])
}
```

### Plugin Customization Control

- `applyPluginCustomizations`: Boolean flag to enable/disable built-in YAML customizations (default: true)
- Can be set at both default and spec levels
- Spec-level setting overrides default setting

### Parallel Processing Configuration

- `parallel`: Boolean flag to enable/disable parallel multi-spec processing (default: true)
- Enables thread-safe concurrent generation of multiple OpenAPI specifications
- Can be disabled for environments with resource constraints or debugging

### Template Variables

- `{{copyright}}`: "Copyright © {{currentYear}} {{companyName}}"
- `{{currentYear}}`: Dynamically generated current year
- `{{companyName}}`: "GuidedByte Technologies Inc."

## Key Technical Concepts

- **Composite build structure**: Plugin in separate directory with `includeBuild`
- **Template resolution**: Clean CodegenConfig API-based template access without JAR scanning
- **Template precedence hierarchy**: User templates > plugin YAML customizations > OpenAPI defaults
- **Recursive template discovery**: Parses templates for `{{>templateName}}` references and extracts dependencies
- **Partial template override**: Only provide/extract templates you want to customize
- **Template customization flow**: Working directory with base template extraction + YAML customizations
- **YAML-based customizations**: Template modifications using structured YAML configuration files
- **Incremental builds**: Input/output tracking with @Internal properties optimization
- **Method-based DSL**: Uses method calls (no `=` equals) for type safety and CLI integration
- **Mustache template engine**: Custom templates with HTML escaping controls (`{{{.}}}`)
- **Recursive variable expansion**: Nested template variables with inheritance
- **Multi-level caching strategy**: Session-scoped extraction cache, working directory cache, and global hash persistence  
- **Thread-safe parallel processing**: ConcurrentHashMap-based caches supporting parallel multi-spec generation
- **Cross-build cache persistence**: Global cache stored in ~/.gradle/caches/openapi-modelgen/ for performance across builds
- **Cache invalidation logic**: Comprehensive invalidation based on OpenAPI Generator version, specs, templates, customizations, and plugin version
- **Working directory cache validation**: SHA-256 content hashing for reliable template change detection and cache key generation
- **Session template extraction summary**: Eliminates redundant template extractions within single build execution
- **Customization result caching**: Thread-safe caching of YAML customization processing with SHA-256 key generation
- **Selective extraction**: Only extracts templates with customizations and their dependencies
- **Configuration validation**: Comprehensive error reporting with actionable messages
- **Lazy evaluation**: Template extraction deferred until task execution
- **Dynamic generator support**: Uses actual generator names from configuration instead of hard-coding
- **CodegenConfig API integration**: Leverages OpenAPI Generator's official template discovery mechanisms
- **Configuration cache compatibility**: All services designed for Gradle configuration cache with no Project dependencies
- **Static logging architecture**: SLF4J static loggers throughout for serialization compatibility

## Template Directory Architecture

### **Template Processing Architecture**

The plugin orchestrates templates from multiple sources with clear precedence hierarchy:

1. **User templates** (`templateDir`) - Complete template overrides, highest priority
2. **User YAML customizations** (`templateCustomizationsDir`) - Surgical template modifications  
3. **Plugin YAML customizations** - Built-in customizations (can be disabled)
4. **OpenAPI Generator defaults** - Base templates extracted as needed

All processing happens in **spec-specific template working directories** (`build/template-work/{generator}-{specName}/`) that serve as the single source of truth for OpenAPI Generator execution. Each spec gets its own isolated template directory to prevent cross-contamination. Templates are cached and only rebuilt when inputs change.

## DSL Syntax Requirements

**IMPORTANT**: This plugin uses **method-call syntax** (no equals signs) for configuration:

```gradle
// ✅ CORRECT - Method call syntax
openapiModelgen {
    defaults {
        outputDir "build/generated"           // Calls outputDir(String value)
        validateSpec true                     // Calls validateSpec(Boolean value)
        parallel true                         // Calls parallel(Boolean value) - enables thread-safe multi-spec processing
        templateVariables([                   // Calls templateVariables(Map)
            copyright: "© 2025"
        ])
    }
    specs {
        pets {
            inputSpec "specs/pets.yaml"       // Calls inputSpec(String value)
            modelPackage "com.example.pets"   // Calls modelPackage(String value)
        }
    }
}

// ❌ INCORRECT - Assignment syntax (will fail)
openapiModelgen {
    defaults {
        outputDir = "build/generated"         // Compilation error
        validateSpec = true                   // Compilation error
    }
}
```

This design enables:

- **Type validation** and comprehensive error reporting
- **@Option annotations** for command-line parameter overrides
- **Gradle Property API** integration for incremental builds
- **Consistent validation** at configuration time

## Current Status

**All core features are fully functional with 112/112 tests passing (100% success rate):**

- ✅ **YAML-based template customization** with comprehensive validation and processing
- ✅ **Multi-level caching system** (session → local → global) with 90% faster no-change builds
- ✅ **Configuration cache compatibility** across all services with static logging
- ✅ **Parallel multi-spec processing** with thread-safe concurrent execution  
- ✅ **Service-oriented architecture** with clean separation of concerns
- ✅ **Template library support** with advanced metadata processing and validation
- ✅ **Configurable template precedence** with debug logging capabilities
- ✅ **Dynamic template discovery** using OpenAPI Generator's CodegenConfig API
- ✅ **Comprehensive documentation** with GitHub Pages deployment

## Dependencies

- **OpenAPI Generator 7.10.0+**: Core code generation engine
- **Lombok**: Generated model annotations and builders
- **Jackson**: JSON serialization support
- **Spring Boot Validation**: Model validation annotations
- **JSR-305**: Nullable annotation support
- **SnakeYAML**: YAML parsing for template customizations
- **JUnit 5**: Unit and integration testing framework
- **Gradle TestKit**: Integration testing with real Gradle environment

## Common Issues & Solutions

- **Build errors**: Remove `include 'plugin'` from settings.gradle (use only `includeBuild`)
- **Template customization**: Use correct Mustache syntax (`{{#description}}` not `{{description}}`)
- **Constructor conflicts**: Use `@NoArgsConstructor(force = true)` with `@AllArgsConstructor`
- **Cache issues**: Global cache at `~/.gradle/caches/openapi-modelgen/` auto-invalidates on changes
- **Performance**: Leverage multi-level caching and parallel processing for large projects
- **Testing**: Use ProjectBuilder for unit tests, TestKit for integration scenarios

## Known OpenAPI Generator Limitations

The plugin works correctly, but there are known bugs in the underlying OpenAPI Generator:

**Type/Import Mappings with Model Prefixes/Suffixes**
- **Affected Versions**: OpenAPI Generator 5.4.0+ (including 7.x series)
- **Issue**: `modelNamePrefix`/`modelNameSuffix` gets incorrectly applied to `typeMappings` targets, causing `importMappings` to fail
- **Symptoms**: Generated wrapper models like `ApiLocalDate` instead of `java.time.LocalDate`
- **Upstream Issues**: 
  - [OpenAPITools/openapi-generator#19043](https://github.com/OpenAPITools/openapi-generator/issues/19043)
  - [OpenAPITools/openapi-generator#11478](https://github.com/OpenAPITools/openapi-generator/issues/11478)
- **Workarounds**: Use either prefixes/suffixes OR type mappings, not both

## Essential File Locations

- **Main plugin**: `plugin/src/main/java/com/guidedbyte/openapi/modelgen/OpenApiModelGenPlugin.java`
- **Configuration classes**: `DefaultConfig.java`, `SpecConfig.java`
- **Core services**: `plugin/src/main/java/com/guidedbyte/openapi/modelgen/services/`
- **Plugin YAML customizations**: `plugin/src/main/resources/templateCustomizations/spring/`
- **Test suite**: `plugin/src/test/java/com/guidedbyte/openapi/modelgen/`
- **Example app**: `test-app/build.gradle` (configuration), `test-app/src/main/resources/openapi/` (specs)
- **Generated output**: `test-app/build/generated-sources/openapi/`
- **Template working dir**: `test-app/build/template-work/{generator}-{specName}/` (e.g., `spring-pets`, `spring-orders`)
- **Global cache**: `~/.gradle/caches/openapi-modelgen/`

## Documentation Standards

When updating project features, ensure consistency across:

1. **README.md** - Main documentation with usage examples
2. **CLAUDE.md** - This technical reference file  
3. **plugin-description.md** - Gradle Plugin Portal description (plain text, concise)
4. **Javadoc comments** - In-code documentation
5. **Test documentation** - TestSummary.md and test comments

### Markdown Quality Requirements

Use `markdownlint-cli` for validation:
- Maximum 120 character lines
- Specify code block languages (`gradle`, `bash`, `yaml`, `text`)
- Unique headings per document
- End files with single newline

```bash
# Validate markdown files
npx markdownlint README.md CLAUDE.md
```

## ⚠️ CRITICAL COMMAND PATTERNS

**NEVER run test-app tasks from root project - they will ALWAYS fail!**

- ❌ WRONG: `/c/gradle/gradle-8.5/bin/gradle test-app:generatePets` 
- ❌ WRONG: `/c/gradle/gradle-8.5/bin/gradle generatePets`
- ✅ CORRECT: `cd test-app && /c/gradle/gradle-8.5/bin/gradle generatePets`

**test-app is a standalone project, not a subproject!**
