# OpenAPI Model Generator Gradle Plugin

A comprehensive Gradle plugin for generating Java DTOs from multiple OpenAPI specifications with enterprise-grade features including template customization, incremental builds, performance optimizations, and automatic version detection.

## Features

### Core Functionality
- **Multi-spec support**: Generate models from multiple OpenAPI specifications in a single project
- **Spec-level configuration**: Individual validation, test generation, and template settings per specification  
- **Template precedence system**: User templates > plugin templates > OpenAPI generator defaults
- **Incremental build support**: Only regenerates when inputs actually change
- **Lombok integration**: Full annotation support with constructor conflict resolution
- **Template variable expansion**: Nested variables like `{{copyright}}` containing `{{currentYear}}`

### Performance & Enterprise Features
- **Automatic version detection**: Works with any OpenAPI Generator version from configuration plugins
- **Parallel template processing**: Concurrent template extraction for large template sets
- **Content-based change detection**: SHA-256 hashing for reliable template change detection
- **Lazy template extraction**: Templates extracted at execution time for better performance
- **Configuration validation**: Comprehensive validation with detailed error reporting
- **Template extraction caching**: High-performance template caching with metadata

### Developer Experience
- **Command-line options**: Override any configuration via CLI parameters
- **Comprehensive testing**: Full TestKit coverage with template precedence testing (61/61 tests passing)
- **Auto-generated documentation**: Plugin help and comprehensive Javadoc documentation
- **@Internal property optimization**: Precise incremental build invalidation

## Usage

### Important: DSL Syntax

**This plugin uses method-call syntax (no equals signs) for configuration:**

```gradle
// ✅ CORRECT - Method call syntax
openapiModelgen {
    defaults {
        outputDir "build/generated"           // Calls outputDir(String value)
        validateSpec true                     // Calls validateSpec(Boolean value)
    }
    specs {
        pets {
            inputSpec "specs/pets.yaml"       // Calls inputSpec(String value)
            modelPackage "com.example.pets"   // Calls modelPackage(String value)
        }
    }
}

// ❌ INCORRECT - Assignment syntax (will cause compilation errors)
openapiModelgen {
    defaults {
        outputDir = "build/generated"         // Will fail
        validateSpec = true                   // Will fail
    }
}
```

This method-based approach enables type validation, CLI parameter overrides, and better error reporting.

## Configuration Defaults

The plugin provides sensible defaults for all configuration options. When no explicit configuration is provided, the following defaults are used:

### Built-in Defaults
```gradle
// These defaults are applied automatically - no configuration needed
defaults {
    outputDir "build/generated"              // Generated code output directory
    modelNameSuffix "Dto"                    // Suffix for generated model classes
    validateSpec false                       // OpenAPI spec validation disabled by default
    generateModelTests false                 // Model test generation disabled
    generateApiTests false                   // API test generation disabled  
    generateApiDocumentation false           // API docs generation disabled
    generateModelDocumentation false         // Model docs generation disabled
    
    // Built-in OpenAPI Generator configuration options
    configOptions([
        useSpringBoot3: "true",              // Use Spring Boot 3 annotations
        useJakartaEe: "true",                // Use Jakarta EE instead of javax
        useBeanValidation: "true",           // Add validation annotations
        dateLibrary: "java8",                // Use java.time classes
        serializableModel: "true",           // Implement Serializable
        hideGenerationTimestamp: "true",     // Don't include timestamps in generated code
        performBeanValidation: "true",       // Enable validation
        enumUnknownDefaultCase: "true",      // Handle unknown enum values
        generateBuilders: "true",            // Generate builder methods
        legacyDiscriminatorBehavior: "false", // Use modern discriminator handling
        disallowAdditionalPropertiesIfNotPresent: "false", // Allow additional properties
        useEnumCaseInsensitive: "true",      // Case-insensitive enum handling
        openApiNullable: "false"             // Don't use OpenAPI nullable wrapper
    ])
    
    // Built-in global properties
    globalProperties([
        models: ""                           // Generate only models (no APIs)
    ])
    
    // Built-in template variables
    templateVariables([
        currentYear: "2025",                 // Current year (auto-generated)
        generatedBy: "OpenAPI Model Generator Plugin",
        pluginVersion: "1.0.0"              // Plugin version (auto-generated)
    ])
}
```

### Minimal Configuration Required

You only need to specify the essential properties - everything else uses sensible defaults:

```gradle
openapiModelgen {
    specs {
        mySpec {
            inputSpec "src/main/resources/openapi-spec/api.yaml"    // Required
            modelPackage "com.example.model"                       // Required
            // All other properties use defaults shown above
        }
    }
}
```

### Apply the Plugin

```gradle
plugins {
    id 'com.guidedbyte.openapi-modelgen'
}
```

### Basic Configuration

```gradle
openapiModelgen {
    specs {
        pets {
            inputSpec "src/main/resources/openapi-spec/pets.yaml"
            modelPackage "com.example.model.pets"
        }
        
        orders {
            inputSpec "src/main/resources/openapi-spec/orders.yaml"
            modelPackage "com.example.model.orders"
        }
    }
}
```

### Advanced Configuration with Template Customization

```gradle
openapiModelgen {
    defaults {
        outputDir "build/generated-sources/openapi"
        templateDir "src/main/resources/openapi-templates"
        modelNameSuffix "Dto"
        validateSpec true
        
        // Lombok integration
        configOptions([
            dateLibrary: "java8",
            serializationLibrary: "jackson", 
            useBeanValidation: "true",
            hideGenerationTimestamp: "true",
            additionalModelTypeAnnotations: "@lombok.Data;@lombok.experimental.Accessors(fluent = true);@lombok.experimental.SuperBuilder;@lombok.NoArgsConstructor(force = true);@lombok.AllArgsConstructor"
        ])
        
        // Template variables with nesting
        templateVariables([
            copyright: "Copyright © {{currentYear}} {{companyName}}",
            currentYear: "2025",
            companyName: "GuidedByte Technologies Inc."
        ])
        
        globalProperties([
            skipFormModel: "false",
            generateAliasAsModel: "true"
        ])
    }
    
    specs {
        pets {
            inputSpec "src/main/resources/openapi-spec/pets.yaml"
            modelPackage "com.example.pets.model"
        }
        
        orders {
            inputSpec "src/main/resources/openapi-spec/orders.yaml"
            modelPackage "com.example.orders.model"
            // Spec-specific overrides
            templateDir "src/main/resources/orders-templates"
            modelNameSuffix "Entity"
            validateSpec false          // Disable validation for this spec
            generateModelTests false    // Skip test generation for this spec
            generateApiDocumentation true  // Generate docs despite other overrides
        }
    }
}
```

### Integration with Build

```gradle
sourceSets {
    main {
        java {
            srcDirs += file("build/generated-sources/openapi/src/main/java")
        }
    }
}

// Ensure generation runs before compilation
compileJava.dependsOn generateOpenApiDtosAll

// Optional: Clean generated sources on clean
clean {
    delete file("build/generated-sources/openapi")
}
```

## Generated Tasks

For each spec named `{specName}`, the plugin creates:
- `generateOpenApiDtosFor{SpecName}` - Generate DTOs for that specific spec
- `generateOpenApiDtosAll` - Generate DTOs for all specs
- `openapiModelgenHelp` - Display comprehensive plugin help and configuration options

### Command Line Options

All configuration options can be overridden via command line:

```bash
# Generate with custom options
./gradlew generateOpenApiDtosForPets --model-package=com.custom.model --validate-spec

# Generate all specs with custom output
./gradlew generateOpenApiDtosAll --output-dir=src/generated

# Override template settings
./gradlew generateOpenApiDtosForPets --template-dir=custom-templates --model-name-suffix=Entity

# Get help
./gradlew openapiModelgenHelp
```

## Template Customization

### Template Resolution Hierarchy

1. **User templates** (highest precedence) - specified in `templateDir`
2. **Plugin templates** - built-in optimized templates with Lombok support
3. **OpenAPI generator defaults** (lowest precedence)

### Custom Template Variables

Template variables support nesting and inheritance:

```gradle
templateVariables = [
    copyright: "Copyright © {{currentYear}} {{companyName}}",  // Nested variables
    currentYear: "2025",
    companyName: "My Company Inc.",
    customHeader: "Generated by {{companyName}} on {{currentYear}}"
]
```

### Supported Template Files

- `pojo.mustache` - Main model class template
- `enumClass.mustache` - Enum class template  
- `additionalModelTypeAnnotations.mustache` - Class-level annotations
- `model.mustache` - Model file wrapper template
- `generatedAnnotation.mustache` - @Generated annotation format

## Dependencies & Version Management

The plugin automatically:
- **Detects OpenAPI Generator** version from your configuration management
- **Works with corporate plugins** that manage dependency versions
- **Falls back to 7.14.0** when no version is provided
- **Validates compatibility** and warns about untested versions
- **Adds required dependencies**: Lombok, Jackson, Spring Boot validation, JSR-305

### Corporate Environment Support

```gradle
plugins {
    id 'com.company.java-standards'        // Provides OpenAPI Generator 8.2.0
    id 'com.guidedbyte.openapi-modelgen'   // Automatically detects and uses 8.2.0
}
// No additional configuration needed - respects corporate version management
```

## Performance Features

- **Incremental builds**: Only regenerates when OpenAPI specs or templates change
- **Template caching**: Efficient template extraction and validation with SHA-256 hashing
- **Parallel processing**: Concurrent template processing for large template sets
- **Lazy evaluation**: Template extraction deferred until task execution
- **Configuration validation**: Comprehensive validation with detailed error messages

## Building and Testing

### Build Commands

```bash
# Build the plugin
./gradlew plugin:build

# Run comprehensive test suite
./gradlew plugin:test

# Generate plugin documentation
./gradlew plugin:generatePluginDocs

# Test with sample application
./gradlew test-app:generateOpenApiDtosForPets
./gradlew test-app:generateOpenApiDtosAll
./gradlew test-app:build
```

### Test Coverage

Comprehensive test suite includes:
- **Unit tests**: Core functionality with ProjectBuilder
- **Integration tests**: Real Gradle environment with TestKit
- **Template precedence tests**: Comprehensive template resolution testing
- **Performance tests**: Incremental builds and caching validation
- **Configuration validation tests**: Error handling and validation

## Project Structure

```
openapi-modelgen/
├── plugin/                          # Main Gradle plugin implementation
│   ├── src/main/java/com/guidedbyte/openapi/modelgen/
│   │   ├── OpenApiModelGenPlugin.java        # Core plugin with advanced optimizations
│   │   ├── OpenApiModelGenExtension.java     # DSL extension with multi-spec support
│   │   ├── DefaultConfig.java                # Global defaults configuration
│   │   └── SpecConfig.java                   # Individual spec configuration
│   ├── src/main/resources/
│   │   ├── templates/spring/                 # Custom Mustache templates
│   │   └── plugin-description.md             # External plugin documentation
│   └── src/test/java/com/guidedbyte/openapi/modelgen/  # Comprehensive test suite
│       ├── PluginFunctionalTest.java         # Unit tests with ProjectBuilder
│       ├── WorkingIntegrationTest.java       # TestKit integration tests
│       ├── TemplatePrecedenceUnitTest.java   # Template precedence testing
│       ├── TemplatePrecedenceTest.java       # Template precedence integration tests
│       ├── LiveTemplatePrecedenceTest.java   # Live template testing
│       └── TestSummary.md                    # Test coverage documentation
└── test-app/                        # Test application using the plugin
    ├── build.gradle                 # Plugin configuration example
    └── src/main/resources/openapi-spec/  # OpenAPI specifications
        ├── pets.yaml                # Pet store API specification
        └── orders.yaml              # Orders API specification (if exists)
```

## Technical Details

### Requirements
- **Java 21+**
- **Gradle 8.0+** 
- **OpenAPI Generator 7.14.0+** (automatically managed)

### Key Technical Features
- **Composite build structure**: Plugin in separate directory with `includeBuild`
- **Template resolution**: Precedence-based template loading with JAR resource extraction
- **SHA-256 content hashing**: Template change detection for efficient caching
- **Parallel processing**: Multi-threaded template extraction for performance
- **Configuration validation**: Comprehensive error reporting with actionable messages
- **Lazy evaluation**: Template extraction deferred until task execution
- **Mustache template engine**: Custom templates with HTML escaping controls

### Troubleshooting

#### Common Issues
- **Build errors**: Ensure composite build is configured properly
- **Syntax errors**: Use method-call syntax (no `=` equals) - see DSL Syntax section above
- **Template spacing**: Use custom templates to control whitespace
- **Constructor conflicts**: Plugin handles Lombok conflicts automatically
- **Configuration validation**: Check error messages for missing specs or invalid packages

#### Debug Options
```bash
# Enable debug logging
./gradlew generateOpenApiDtosForPets --info

# Get comprehensive help
./gradlew openapiModelgenHelp

# Generate plugin documentation
./gradlew plugin:generatePluginDocs
```

## Versioning and Releases

### Version Management
The plugin uses semantic versioning with automated git-based version detection:

```bash
# Check current version
./gradlew showVersion

# Validate version format
./gradlew validateVersion

# Create release (validates, tests, and tags)
./gradlew createRelease
```

### Release Process
1. **Development**: Work on `SNAPSHOT` versions
2. **Testing**: Validate with `./gradlew validatePlugin`
3. **Release**: Create tag with `./gradlew createRelease`
4. **Publish**: Push tag and run `./gradlew publishPlugins`

### Version Override Options
```bash
# Command line override
./gradlew build -Pversion=1.1.0

# gradle.properties override
# Uncomment version=1.1.0 in gradle.properties

# Git tag-based (automatic)
git tag v1.1.0 && ./gradlew build
```

See `MIGRATION.md` for detailed migration guidance between versions.

## Contributing

See `CLAUDE.md` for detailed development guidelines and technical architecture documentation.