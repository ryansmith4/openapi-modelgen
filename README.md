# OpenAPI Model Generator Gradle Plugin

A comprehensive Gradle plugin for generating Java DTOs from multiple OpenAPI specifications with enterprise-grade
features including template customization, incremental builds, performance optimizations, and automatic version
detection.

## 🚀 Quick Start

### 1. Apply the Plugin

**Important**: You must apply the OpenAPI Generator plugin before applying this plugin:

```gradle
plugins {
    id 'org.openapi.generator' version '7.14.0'  // Or your preferred version 7.10.0+
    id 'com.guidedbyte.openapi-modelgen' version '@version@'
}
```

### 2. Basic Configuration

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

### 3. Generate Models

```bash
# Generate models for specific spec
./gradlew generatePets

# Generate models for all specs
./gradlew generateAllModels

# Get help
./gradlew generateHelp
```

### 4. Build Integration (Automatic)

**No manual configuration required!** The plugin automatically:
- Registers generated source directories with the main sourceSet
- Wires `compileJava` to depend on `generateAllModels`

Each spec generates to its own subdirectory for build cache isolation:
- `pets` → `build/generated/sources/openapi/pets/src/main/java/`
- `orders` → `build/generated/sources/openapi/orders/src/main/java/`

Just run `./gradlew build` and everything works.

## ✨ Key Features

### Core Functionality

- **Multi-spec support**: Generate models from multiple OpenAPI specifications in a single project
- **Template customization system**: YAML-based template modifications with comprehensive validation
- **Template precedence hierarchy**: User templates > User YAML customizations > Plugin YAML > OpenAPI defaults
- **Template library support**: Share and reuse templates/customizations via dependency JARs
- **Incremental build support**: Only regenerates when inputs actually change
- **Lombok integration**: Full annotation support with constructor conflict resolution

### Performance & Enterprise Features

- **Multi-level caching system**: 90% faster no-change builds, 70% faster incremental builds
- **Parallel multi-spec processing**: Thread-safe concurrent generation
- **Cross-build performance optimization**: Global cache persistence
- **Automatic version detection**: Works with any OpenAPI Generator version from configuration plugins
- **Content-based change detection**: SHA-256 hashing for reliable template change detection

### Developer Experience

- **Command-line options**: Override any configuration via CLI parameters
- **Comprehensive testing**: 247+ test methods across 27 test classes (100% pass rate)
- **Method-call DSL**: Type-safe configuration with validation and CLI overrides

## 📖 Documentation

### Configuration

- **[Configuration Guide](docs/configuration.md)** - Complete configuration options, defaults, and DSL syntax
- **[Template System](docs/template-system.md)** - Template customization, YAML modifications, and libraries

### Advanced Topics

- **[Performance](docs/performance.md)** - Caching system, parallel processing, and optimization
- **[Troubleshooting](docs/troubleshooting.md)** - Common issues, debug options, and known limitations
- **[Development](docs/development.md)** - Building, testing, and contributing

### Release Information

- **[CHANGELOG](CHANGELOG.md)** - Release notes and version history

## 💡 Important: DSL Syntax

**This plugin uses method-call syntax (no equals signs) for configuration:**

```gradle
// ✅ CORRECT - Method call syntax
openapiModelgen {
    defaults {
        outputDir "build/generated/sources/openapi"  // Calls outputDir(String value)
        validateSpec true                            // Calls validateSpec(Boolean value)
    }
    specs {
        pets {
            inputSpec "specs/pets.yaml"              // Calls inputSpec(String value)
            modelPackage "com.example.pets"          // Calls modelPackage(String value)
        }
    }
}

// ❌ INCORRECT - Assignment syntax (will cause compilation errors)
openapiModelgen {
    defaults {
        outputDir = "build/generated"                // Will fail
        validateSpec = true                          // Will fail
    }
}
```

This method-based approach enables type validation, CLI parameter overrides, and better error reporting.

## 🔧 Configuration Defaults

The plugin comes pre-configured with sensible defaults for **Spring Boot 3 + Jakarta EE + Lombok**:

```gradle
// These are the built-in defaults - you only need to override what you want to change
openapiModelgen {
    defaults {
        generatorName "spring"                               // OpenAPI Generator name
        outputDir "build/generated/sources/openapi"         // Generated code output
        modelNameSuffix "Dto"                               // Suffix for model classes
        validateSpec false                                   // Validate specs before generation
        applyPluginCustomizations true                       // Use built-in optimizations
        parallel true                                        // Enable parallel processing

        // Template variables available in all templates
        templateVariables([
            currentYear: "2025",                            // Dynamic
            generatedBy: "OpenAPI Model Generator Plugin",  // Plugin identification
            pluginVersion: "@version@"                      // Plugin version
        ])
    }
}
```

### Minimal Configuration Required

You only need to specify the essential properties:

```gradle
openapiModelgen {
    specs {
        mySpec {
            inputSpec "src/main/resources/openapi-spec/api.yaml"    // Required
            modelPackage "com.example.model"                       // Required
            // All other properties use sensible defaults
        }
    }
}
```

## 🎨 Template Customization Overview

The plugin provides multiple ways to customize generated code:

### 1. YAML-Based Customizations (Recommended)

Modify existing templates without replacing them entirely:

```yaml
# src/main/resources/template-customizations/spring/pojo.mustache.yaml
insertions:
  - before: "{{#description}}"
    content: |
      /**
       * Generated by {{companyName}} on {{currentYear}}
       */

replacements:
  - pattern: "{{#deprecated}}"
    replacement: "{{#deprecated}}\n  // DEPRECATED - Use newer version"
```

### 2. Complete Template Override

Replace entire templates with your own:

```text
src/main/resources/openapi-templates/
└── spring/
    ├── pojo.mustache       # Complete template override
    └── enumClass.mustache  # Complete template override
```

### 3. Template Libraries

Share templates across projects via JAR dependencies:

```gradle
dependencies {
    openapiCustomizations 'com.company:api-templates:1.0.0'
}

openapiModelgen {
    defaults {
        useLibraryTemplates true
        useLibraryCustomizations true
    }
}
```

**📖 For complete template documentation, see [Template System Guide](docs/template-system.md)**

## 🏗️ Advanced Configuration Example

```gradle
openapiModelgen {
    defaults {
        outputDir "build/generated/sources/openapi"
        userTemplateDir "src/main/resources/openapi-templates"
        userTemplateCustomizationsDir "src/main/resources/template-customizations"
        modelNameSuffix "Dto"
        validateSpec true
        parallel true

        // Template variables with nesting
        templateVariables([
            copyright: "Copyright © {{currentYear}} {{companyName}}",
            currentYear: "2025",
            companyName: "GuidedByte Technologies Inc."
        ])

        // OpenAPI Generator mappings
        importMappings([
            'UUID': 'java.util.UUID',
            'LocalDate': 'java.time.LocalDate'
        ])
        typeMappings([
            'string+uuid': 'UUID',
            'string+date': 'LocalDate'
        ])
        openapiNormalizer([
            'REFACTOR_ALLOF_WITH_PROPERTIES_ONLY': 'true',
            'SIMPLIFY_ONEOF_ANYOF': 'true'
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
            modelNameSuffix "Entity"                     // Spec-specific override
            validateSpec false                           // Disable validation for this spec
        }
    }
}
```

**📖 For complete configuration documentation, see [Configuration Guide](docs/configuration.md)**

## 🚀 Generated Tasks

For each spec named `{specName}`, the plugin creates:

- `generate{SpecName}` - Generate models for that specific spec (e.g., `generatePets`, `generateOrders`)
- `generateAllModels` - Generate models for all specs
- `generateHelp` - Display comprehensive plugin help and configuration options

### Command Line Options

All configuration options can be overridden via command line:

```bash
# Generate with custom options
./gradlew generatePets --model-package=com.custom.model --validate-spec

# Generate all specs with custom output
./gradlew generateAllModels --output-dir=src/generated

# Override template settings
./gradlew generatePets --template-dir=custom-templates --model-name-suffix=Entity
```

## 🏢 Corporate Environment Support

The plugin automatically detects OpenAPI Generator versions from corporate plugins:

```gradle
plugins {
    id 'com.company.java-standards'        // Provides OpenAPI Generator 8.2.0
    id 'com.guidedbyte.openapi-modelgen'   // Automatically detects and uses 8.2.0
}
// No additional configuration needed - respects corporate version management
```

## 📋 Requirements

- **Java 17+**
- **Gradle 8.0+**
- **OpenAPI Generator 7.10.0+** (must be provided by consumer project)

## 🤝 Contributing

See [Development Guide](docs/development.md) for detailed development guidelines and technical architecture documentation.

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Issues**: [GitHub Issues](https://github.com/guidedbyte/openapi-modelgen/issues)
- **Documentation**: [GitHub Pages](https://guidedbyte.github.io/openapi-modelgen/)
- **Template Schema**: [Schema Documentation](https://guidedbyte.github.io/openapi-modelgen/template-schema.html)

---

**📖 For detailed technical documentation, see the [docs/](docs/) directory.**
