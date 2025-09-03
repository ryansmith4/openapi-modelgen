# OpenAPI Model Generator Plugin

## Project Overview
A comprehensive Gradle plugin that wraps the OpenAPI Generator with enhanced features for generating Java DTOs with Lombok support, custom templates, and enterprise-grade performance optimizations.

## Key Features
- **Template Precedence System**: user templates > plugin templates > OpenAPI generator defaults
- **Incremental Build Support**: Only regenerates when inputs actually change
- **Configuration Validation**: Comprehensive validation with detailed error reporting
- **Parallel Template Processing**: Concurrent template extraction for large template sets
- **Content-Based Change Detection**: SHA-256 hashing for reliable template change detection
- **Lazy Template Extraction**: Templates extracted at execution time for better performance
- **@Internal Property Optimization**: Precise incremental build invalidation
- **Nested Template Variables**: Recursive expansion (e.g., `{{copyright}}` containing `{{currentYear}}`)
- **Template Extraction Caching**: High-performance template caching with metadata
- **Custom Mustache Templates**: Enhanced code formatting and structure
- **Lombok Integration**: Full annotation support with constructor conflict resolution
- **Multi-Spec Support**: Generate DTOs from multiple OpenAPI specifications
- **Spec-Level Configuration**: Individual validation and test/doc generation settings per specification
- **Command Line Options**: @Option annotations for CLI parameter overrides
- **Comprehensive Documentation**: Auto-generated help and full Javadoc documentation
- **Integration Tests**: Full TestKit coverage with template precedence testing (61 tests passing)

## Project Structure
```
openapi-modelgen/
├── plugin/                          # Main Gradle plugin implementation
│   ├── src/main/java/com/guidedbyte/openapi/modelgen/
│   │   ├── OpenApiModelGenPlugin.java        # Core plugin with advanced optimizations
│   │   ├── OpenApiModelGenExtension.java     # DSL extension with multi-spec support
│   │   ├── DefaultConfig.java                # Global defaults configuration
│   │   └── SpecConfig.java                   # Individual spec configuration
│   ├── src/main/resources/templates/spring/  # Custom Mustache templates
│   │   ├── additionalModelTypeAnnotations.mustache
│   │   ├── pojo.mustache
│   │   ├── enumClass.mustache
│   │   ├── model.mustache
│   │   ├── generatedAnnotation.mustache
│   │   └── typeInfoAnnotation.mustache
│   └── src/test/java/com/guidedbyte/openapi/modelgen/  # Comprehensive test suite
│       ├── PluginFunctionalTest.java         # Unit tests with ProjectBuilder
│       ├── WorkingIntegrationTest.java       # TestKit integration tests
│       ├── TemplatePrecedenceUnitTest.java   # Template precedence testing
│       ├── ConfigurationValidationTest.java  # Configuration validation tests
│       ├── PerformanceIntegrationTest.java   # Performance and incremental tests
│       └── TestSummary.md                    # Test coverage documentation
└── test-app/                        # Test application using the plugin
    ├── build.gradle                 # Plugin configuration example
    └── src/main/resources/openapi-spec/  # OpenAPI specifications
        ├── pets.yaml                # Pet store API specification
        └── orders.yaml              # Orders API specification
```

## Build Commands
- **Generate models**: `/c/gradle/gradle-8.5/bin/gradle test-app:generatePets`
- **Generate all**: `/c/gradle/gradle-8.5/bin/gradle test-app:generateAllModels`
- **Clean build**: `/c/gradle/gradle-8.5/bin/gradle clean build`
- **Test plugin**: `/c/gradle/gradle-8.5/bin/gradle plugin:test`
- **Generate docs**: `/c/gradle/gradle-8.5/bin/gradle plugin:generatePluginDocs`
- **Plugin help**: `/c/gradle/gradle-8.5/bin/gradle test-app:generateHelp`

## Current Configuration
### Lombok Annotations (test-app/build.gradle)
```gradle
additionalModelTypeAnnotations: "@lombok.Data;@lombok.experimental.Accessors(fluent = true);@lombok.experimental.SuperBuilder;@lombok.NoArgsConstructor(force = true);@lombok.AllArgsConstructor"
```

### Template Variables
- `{{copyright}}`: "Copyright © {{currentYear}} {{companyName}}"
- `{{currentYear}}`: Dynamically generated current year
- `{{companyName}}`: "GuidedByte Technologies Inc."

## Key Technical Concepts
- **Composite build structure**: Plugin in separate directory with `includeBuild`
- **Template resolution**: Precedence-based template loading with JAR resource extraction
- **Template precedence hierarchy**: User templates > plugin templates > OpenAPI defaults
- **Incremental builds**: Input/output tracking with @Internal properties optimization
- **Method-based DSL**: Uses method calls (no `=` equals) for type safety and CLI integration
- **Mustache template engine**: Custom templates with HTML escaping controls (`{{{.}}}`)
- **Recursive variable expansion**: Nested template variables with inheritance
- **SHA-256 content hashing**: Template change detection for efficient caching
- **Parallel processing**: Multi-threaded template extraction for performance
- **Configuration validation**: Comprehensive error reporting with actionable messages
- **Lazy evaluation**: Template extraction deferred until task execution

## DSL Syntax Requirements

**IMPORTANT**: This plugin uses **method-call syntax** (no equals signs) for configuration:

```gradle
// ✅ CORRECT - Method call syntax
openapiModelgen {
    defaults {
        outputDir "build/generated"           // Calls outputDir(String value)
        validateSpec true                     // Calls validateSpec(Boolean value)
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

## Recently Completed Work
✅ **Core Plugin Development**
  - Fixed duplicate project name build errors
  - Added JSR-305 dependency for nullable annotations
  - Resolved Lombok constructor conflicts
  - Implemented template support with caching
  - Added nested template variable expansion
  - Fixed extra blank lines in generated class annotations
  - Fixed enum spacing issues (removed extra newlines between enum values)

✅ **Performance & Configuration Enhancements**
  - Added comprehensive configuration validation with detailed error reporting
  - Implemented lazy template extraction for improved performance
  - Added @Internal property annotations for incremental build optimization
  - Implemented SHA-256 content hash validation for template change detection
  - Added parallel template extraction using ExecutorService
  - Optimized template fingerprinting for faster builds

✅ **Enhanced Spec-Level Configuration (Latest)**
  - Added spec-level `validateSpec` support for mixed validation in multi-spec setups
  - Added complete spec-level override properties: generateModelTests, generateApiTests, generateApiDocumentation, generateModelDocumentation
  - Enhanced configuration flexibility for individual specifications
  - Maintained strict template directory validation to prevent unintended template usage

✅ **Documentation & Testing Excellence (Latest)**
  - Added comprehensive Javadoc documentation to all main classes with usage examples
  - Enhanced test documentation with TestKit path explanations
  - Fixed all 61 tests - complete test suite now passes
  - Added explanatory comments about `${project.projectDir}` requirement in TestKit tests
  - Documented template precedence and multi-spec configuration patterns

✅ **Documentation & CLI Integration**
  - Added @Option annotations for command-line parameter documentation
  - Implemented automatic plugin documentation generation
  - Created comprehensive help task with usage examples
  - Enhanced plugin description with detailed configuration examples

✅ **Comprehensive Testing Framework**
  - Implemented unit tests using ProjectBuilder for core functionality
  - Added TestKit integration tests for real Gradle environment testing
  - Created comprehensive template precedence test suite (8 test methods)
  - Added live template integration tests with caching validation
  - Implemented template variable expansion testing
  - Added configuration validation error testing
  - Fixed all test path and configuration issues (61/61 tests passing)

## Dependencies
- **OpenAPI Generator 7.14.0**: Core code generation engine
- **Lombok**: Generated model annotations and builders
- **Jackson**: JSON serialization support
- **Spring Boot Validation**: Model validation annotations
- **JSR-305**: Nullable annotation support
- **JUnit 5**: Unit and integration testing framework
- **Gradle TestKit**: Integration testing with real Gradle environment

## Common Issues & Solutions
- **Build errors**: Ensure `include 'plugin'` is removed from settings.gradle (use only `includeBuild`)
- **Repository errors**: Plugin build.gradle needs repositories block for composite builds
- **Template spacing**: Use custom templates to control whitespace in generated code
- **Constructor conflicts**: Use `@NoArgsConstructor(force = true)` with `@AllArgsConstructor`
- **Configuration validation**: Use detailed error messages to identify missing specs, invalid packages, or duplicate names
- **Template precedence**: User templates override plugin templates; check template directory paths
- **Performance issues**: Leverage parallel processing and template caching for large projects
- **Testing failures**: Use ProjectBuilder for unit tests, TestKit for integration scenarios

## File Locations
### Core Plugin Files
- **Main plugin**: `plugin/src/main/java/com/guidedbyte/openapi/modelgen/OpenApiModelGenPlugin.java`
- **Configuration DSL**: `plugin/src/main/java/com/guidedbyte/openapi/modelgen/DefaultConfig.java`
- **Spec configuration**: `plugin/src/main/java/com/guidedbyte/openapi/modelgen/SpecConfig.java`
- **Plugin templates**: `plugin/src/main/resources/templates/spring/`
- **Build configuration**: `plugin/build.gradle`

### Test Suite
- **Unit tests**: `plugin/src/test/java/com/guidedbyte/openapi/modelgen/PluginFunctionalTest.java`
- **Integration tests**: `plugin/src/test/java/com/guidedbyte/openapi/modelgen/WorkingIntegrationTest.java`
- **Template precedence tests**: `plugin/src/test/java/com/guidedbyte/openapi/modelgen/TemplatePrecedenceUnitTest.java`
- **Live template tests**: `plugin/src/test/java/com/guidedbyte/openapi/modelgen/LiveTemplatePrecedenceTest.java`
- **Template precedence integration**: `plugin/src/test/java/com/guidedbyte/openapi/modelgen/TemplatePrecedenceTest.java`
- **Test documentation**: `plugin/src/test/java/com/guidedbyte/openapi/modelgen/TestSummary.md`

### Example Application
- **Generated output**: `test-app/build/generated-sources/openapi/`
- **OpenAPI spec**: `test-app/src/main/resources/openapi-spec/pets.yaml`
- **Test configuration**: `test-app/build.gradle`