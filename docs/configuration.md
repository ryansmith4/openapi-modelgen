# Configuration Guide

This guide covers all configuration options, defaults, and DSL syntax for the OpenAPI Model Generator plugin.

## Table of Contents

- [DSL Syntax](#dsl-syntax)
- [Configuration Defaults](#configuration-defaults)
- [Configuration Structure](#configuration-structure)
- [Core Settings](#core-settings)
- [Generation Control](#generation-control)
- [Template Configuration](#template-configuration)
- [OpenAPI Generator Options](#openapi-generator-options)
- [Template Variables](#template-variables)
- [OpenAPI Generator Mappings](#openapi-generator-mappings)
- [Spec-Level Overrides](#spec-level-overrides)
- [Command Line Options](#command-line-options)
- [Examples](#examples)

## DSL Syntax

**This plugin uses method-call syntax (no equals signs) for configuration:**

```gradle
// ✅ CORRECT - Method call syntax
openapiModelgen {
    defaults {
        outputDir "build/generated/sources/openapi" // Calls outputDir(String value)
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

The plugin comes pre-configured with sensible defaults for **Spring Boot 3 + Jakarta EE + Lombok**:

### Core Settings

```gradle
generatorName = "spring"                           // OpenAPI Generator name
outputDir = "build/generated/sources/openapi"     // Generated code output directory
userTemplateDir = null                             // Custom template directory (not set by default)
userTemplateCustomizationsDir = null              // YAML customizations directory (not set by default)
modelNameSuffix = "Dto"                           // Suffix for generated model classes
```

### Generation Control

```gradle
validateSpec = false                              // Validate OpenAPI spec before generation
applyPluginCustomizations = true                  // Apply built-in plugin YAML customizations
generateModelTests = false                        // Generate model unit tests
generateApiTests = false                          // Generate API unit tests
generateApiDocumentation = false                  // Generate API documentation
generateModelDocumentation = false                // Generate model documentation
saveOriginalTemplates = false                     // Save original OpenAPI Generator templates to orig/ subdirectory
parallel = true                                   // Enable parallel multi-spec processing
```

### Template Resolution

```gradle
templatePrecedence = [                            // Template resolution priority order
    "user-templates",                             // Project Mustache templates (highest)
    "user-customizations",                        // Project YAML customizations
    "plugin-customizations",                      // Built-in plugin YAML customizations
    "openapi-generator"                          // OpenAPI Generator defaults (lowest)
]
// No logging configuration needed - use standard Gradle flags:
// ./gradlew generatePets --debug    (debug output)
// ./gradlew generatePets --quiet    (minimal output)
```

### OpenAPI Generator Options

The plugin sets these OpenAPI Generator configuration options by default:

```gradle
annotationLibrary = "swagger2"                    // Use Swagger 2 annotations
swagger2AnnotationLibrary = "true"               // Enable Swagger 2 annotation library
useSpringBoot3 = "true"                          // Spring Boot 3 compatibility
useJakartaEe = "true"                            // Jakarta EE instead of Java EE
useBeanValidation = "true"                       // Bean validation annotations
dateLibrary = "java8"                            // Java 8+ date/time types
serializableModel = "true"                       // Implement Serializable
hideGenerationTimestamp = "true"                 // Don't include timestamp in generated files
performBeanValidation = "true"                   // Runtime bean validation
enumUnknownDefaultCase = "true"                  // Handle unknown enum values
generateBuilders = "true"                        // Generate builder methods
legacyDiscriminatorBehavior = "false"            // Use modern discriminator handling
disallowAdditionalPropertiesIfNotPresent = "false" // Allow additional properties
useEnumCaseInsensitive = "true"                  // Case-insensitive enum parsing
openApiNullable = "false"                        // Don't use OpenAPI nullable library
```

### Lombok Compatibility

```gradle
skipDefaults = "true"                            // Skip default constructors (Lombok provides them)
generateConstructorPropertiesAnnotation = "false" // Don't generate @ConstructorProperties
```

### Global Properties

```gradle
models = ""                                      // Generate only models (no APIs)
```

### Template Variables

```gradle
currentYear = 2025                               // Current year (dynamic)
generatedBy = "OpenAPI Model Generator Plugin"   // Generator identification
pluginVersion = "@version@"                        // Plugin version (from build)
```

## Configuration Structure

The plugin configuration follows this structure:

```gradle
openapiModelgen {
    defaults {
        // Global defaults applied to all specs
        // Can be overridden at spec level
    }

    specs {
        specName1 {
            // Required properties
            inputSpec "path/to/spec.yaml"
            modelPackage "com.example.package"

            // Optional spec-specific overrides
        }

        specName2 {
            // Each spec can have its own configuration
        }
    }
}
```

## Core Settings

### Required Properties (Spec Level)

```gradle
specs {
    mySpec {
        inputSpec "src/main/resources/openapi-spec/api.yaml"    // Path to OpenAPI specification
        modelPackage "com.example.model"                       // Java package for generated models
    }
}
```

### Optional Core Properties

```gradle
defaults {
    generatorName "spring"                                    // OpenAPI Generator to use
    outputDir "build/generated/sources/openapi"              // Output directory for generated code
    modelNamePrefix ""                                        // Prefix for model class names
    modelNameSuffix "Dto"                                     // Suffix for model class names
    invokerPackage "com.example.api"                         // Package for generated API classes
    groupId "com.example"                                     // Maven group ID
    artifactId "api-models"                                   // Maven artifact ID
    artifactVersion "1.0.0"                                  // Maven version
    artifactDescription "Generated API models"                // Maven description
}
```

## Generation Control

```gradle
defaults {
    validateSpec true                                         // Validate OpenAPI spec before generation
    generateModelTests true                                   // Generate unit tests for models
    generateApiTests false                                    // Generate unit tests for APIs
    generateApiDocumentation true                             // Generate API documentation
    generateModelDocumentation true                           // Generate model documentation
    generateSupportingFiles true                             // Generate supporting files
    generateApi false                                         // Generate API classes (usually false for model-only)
    generateModel true                                        // Generate model classes
    generateSupporting false                                  // Generate supporting classes
    applyPluginCustomizations true                            // Apply built-in plugin YAML customizations
    saveOriginalTemplates false                               // Save original templates for debugging
    parallel true                                             // Enable parallel processing
}
```

## Template Configuration

### Template Directories

```gradle
defaults {
    userTemplateDir "src/main/resources/openapi-templates"              // Complete template overrides
    userTemplateCustomizationsDir "src/main/resources/customizations"   // YAML-based customizations
}
```

### Template Library Support

```gradle
defaults {
    useLibraryTemplates true                                  // Enable template extraction from JARs
    useLibraryCustomizations true                             // Enable YAML customization extraction from JARs

    templatePrecedence([
        'user-templates',           // Project templates (highest)
        'user-customizations',      // Project YAML customizations
        'library-templates',        // Library templates
        'library-customizations',   // Library YAML customizations
        'plugin-customizations',    // Built-in plugin customizations
        'openapi-generator'         // OpenAPI defaults (lowest)
    ])
}
```

### Adding Template Libraries

```gradle
dependencies {
    openapiCustomizations 'com.company:api-templates:1.0.0'
    openapiCustomizations 'com.company:validation-templates:2.1.0'
    openapiCustomizations files('libs/local-templates.jar')
}
```

## OpenAPI Generator Options

### Configuration Options

Pass any OpenAPI Generator configuration option:

```gradle
defaults {
    configOptions([
        dateLibrary: "java8",
        serializationLibrary: "jackson",
        useBeanValidation: "true",
        hideGenerationTimestamp: "true",
        additionalModelTypeAnnotations: "@lombok.Data;@lombok.experimental.Accessors(fluent = true);@lombok.experimental.SuperBuilder;@lombok.NoArgsConstructor(force = true);@lombok.AllArgsConstructor"
    ])
}
```

### Additional Properties

Equivalent to `--additional-properties` CLI option:

```gradle
defaults {
    additionalProperties([
        'library': 'spring-boot',
        'beanValidations': 'true',
        'useSpringBoot3': 'true',
        'reactive': 'false',
        'serializableModel': 'true'
    ])
}
```

### Global Properties

Equivalent to `--global-property` CLI option:

```gradle
defaults {
    globalProperties([
        skipFormModel: "false",
        generateAliasAsModel: "true",
        models: ""                                           // Generate only models
    ])
}
```

## Template Variables

Template variables support nesting and are available in all Mustache templates:

```gradle
defaults {
    templateVariables([
        // Basic variables
        companyName: "GuidedByte Technologies Inc.",
        currentYear: "2025",

        // Nested variables - will be recursively expanded
        copyright: "Copyright © {{currentYear}} {{companyName}}",
        header: "Generated by {{companyName}} on {{currentYear}}",
        buildInfo: "Build {{buildNumber}} - {{currentYear}}",

        // Custom variables
        apiVersion: "v1",
        serviceType: "REST API",
        customAnnotation: "@CustomValidation"
    ])
}
```

### Built-in Variables

The plugin provides these built-in variables:

- `{{currentYear}}` - Current year (dynamic)
- `{{generatedBy}}` - "OpenAPI Model Generator Plugin"
- `{{pluginVersion}}` - Plugin version from build

## OpenAPI Generator Mappings

### Import Mappings

Map type names to fully qualified import statements:

```gradle
defaults {
    importMappings([
        'UUID': 'java.util.UUID',
        'LocalDate': 'java.time.LocalDate',
        'LocalDateTime': 'java.time.LocalDateTime',
        'BigDecimal': 'java.math.BigDecimal',
        'Instant': 'java.time.Instant'
    ])
}
```

### Type Mappings

Map OpenAPI types to Java types. Supports format-specific mappings with `+` notation:

```gradle
defaults {
    typeMappings([
        'string+uuid': 'UUID',           // string format=uuid -> UUID
        'string+date': 'LocalDate',      // string format=date -> LocalDate
        'string+date-time': 'LocalDateTime',
        'integer+int64': 'Long',
        'number+double': 'Double'
    ])
}
```

### OpenAPI Normalizer Rules

Transform and normalize OpenAPI specifications before code generation:

```gradle
defaults {
    openapiNormalizer([
        'REFACTOR_ALLOF_WITH_PROPERTIES_ONLY': 'true',  // Simplify allOf schemas with only properties
        'SIMPLIFY_ONEOF_ANYOF': 'true',                 // Simplify oneOf/anyOf schemas
        'KEEP_ONLY_FIRST_TAG_IN_OPERATION': 'true',     // Keep only first tag per operation
        'SIMPLIFY_BOOLEAN_ENUM': 'true',                // Convert boolean enums to simple booleans
        'SET_TAGS_FOR_ALL_OPERATIONS': 'default',       // Set default tag for untagged operations
        'NORMALIZE_ENUM_MEMBERS': 'true'                // Normalize enum member names
    ])
}
```

Common normalizer rules:
- **`REFACTOR_ALLOF_WITH_PROPERTIES_ONLY`**: Simplifies `allOf` schemas that only contain properties
- **`SIMPLIFY_ONEOF_ANYOF`**: Simplifies `oneOf`/`anyOf` schemas where possible
- **`KEEP_ONLY_FIRST_TAG_IN_OPERATION`**: Keeps only the first tag for operations with multiple tags
- **`SIMPLIFY_BOOLEAN_ENUM`**: Converts enum with only true/false values to boolean type
- **`SET_TAGS_FOR_ALL_OPERATIONS`**: Adds a default tag to operations without tags
- **`NORMALIZE_ENUM_MEMBERS`**: Normalizes enum member names for consistency

### Mapping Precedence

All mapping properties follow the same merge pattern:
1. **Default-level mappings** are applied first
1. **Spec-level mappings** are merged in, with spec values taking precedence for duplicate keys
1. **Final merged mappings** are passed to OpenAPI Generator

## Spec-Level Overrides

Any default configuration can be overridden at the spec level:

```gradle
openapiModelgen {
    defaults {
        outputDir "build/generated/sources/openapi"
        modelNameSuffix "Dto"
        validateSpec false
        parallel true
    }

    specs {
        pets {
            inputSpec "src/main/resources/openapi-spec/pets.yaml"
            modelPackage "com.example.pets.model"
            // Uses all defaults
        }

        orders {
            inputSpec "src/main/resources/openapi-spec/orders.yaml"
            modelPackage "com.example.orders.model"

            // Spec-specific overrides
            modelNameSuffix "Entity"                        // Override suffix for this spec
            validateSpec true                               // Enable validation for this spec
            outputDir "build/generated/orders"              // Different output directory

            // Spec-specific template configuration
            userTemplateDir "src/main/resources/orders-templates"
            userTemplateCustomizationsDir "src/main/resources/orders-customizations"

            // Spec-specific variables
            templateVariables([
                serviceType: "OrderService",
                apiVersion: "v2"
            ])

            // Spec-specific mappings (merged with defaults)
            importMappings([
                'OrderStatus': 'com.example.orders.OrderStatus'
            ])
        }
    }
}
```

## Command Line Options

All configuration options can be overridden via command line using kebab-case:

```bash
# Override basic options
./gradlew generatePets --model-package=com.custom.model --validate-spec

# Override output and template settings
./gradlew generateAllModels --output-dir=src/generated --model-name-suffix=Entity

# Override template directories
./gradlew generatePets --user-template-dir=custom-templates --user-template-customizations-dir=custom-yaml

# Override OpenAPI Generator options
./gradlew generatePets --generator-name=java --date-library=java8

# Disable features
./gradlew generatePets --no-parallel --no-apply-plugin-customizations

# Enable debugging
./gradlew generatePets --save-original-templates --debug
```

### CLI Option Format

- Configuration properties use kebab-case: `modelPackage` → `--model-package`
- Boolean flags: `--validate-spec` (enable) or `--no-validate-spec` (disable)
- Maps use `key=value` format: `--template-variables=company=Acme,year=2025`

## Examples

### Minimal Configuration

```gradle
openapiModelgen {
    specs {
        api {
            inputSpec "src/main/resources/openapi/api.yaml"
            modelPackage "com.example.model"
            // Everything else uses defaults
        }
    }
}
```

### Multi-Spec Configuration

```gradle
openapiModelgen {
    defaults {
        outputDir "build/generated/sources/openapi"
        modelNameSuffix "Dto"
        validateSpec true

        templateVariables([
            companyName: "Acme Corp",
            currentYear: "2025"
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
        }

        users {
            inputSpec "src/main/resources/openapi-spec/users.yaml"
            modelPackage "com.example.users.model"
        }
    }
}
```

### Advanced Configuration with All Features

```gradle
openapiModelgen {
    defaults {
        // Core settings
        generatorName "spring"
        outputDir "build/generated/sources/openapi"
        modelNameSuffix "Dto"

        // Generation control
        validateSpec true
        parallel true
        applyPluginCustomizations true
        generateModelTests true
        saveOriginalTemplates false

        // Template configuration
        userTemplateDir "src/main/resources/openapi-templates"
        userTemplateCustomizationsDir "src/main/resources/template-customizations"
        useLibraryTemplates true
        useLibraryCustomizations true

        templatePrecedence([
            'user-templates',
            'user-customizations',
            'library-templates',
            'library-customizations',
            'plugin-customizations',
            'openapi-generator'
        ])

        // Template variables with nesting
        templateVariables([
            copyright: "Copyright © {{currentYear}} {{companyName}}",
            header: "Generated by {{companyName}} on {{currentYear}}",
            currentYear: "2025",
            companyName: "GuidedByte Technologies Inc.",
            buildNumber: "1.0.0"
        ])

        // OpenAPI Generator configuration
        configOptions([
            dateLibrary: "java8",
            serializationLibrary: "jackson",
            useBeanValidation: "true",
            hideGenerationTimestamp: "true",
            additionalModelTypeAnnotations: "@lombok.Data;@lombok.NoArgsConstructor;@lombok.AllArgsConstructor"
        ])

        // OpenAPI Generator mappings
        importMappings([
            'UUID': 'java.util.UUID',
            'LocalDate': 'java.time.LocalDate',
            'LocalDateTime': 'java.time.LocalDateTime',
            'BigDecimal': 'java.math.BigDecimal'
        ])

        typeMappings([
            'string+uuid': 'UUID',
            'string+date': 'LocalDate',
            'string+date-time': 'LocalDateTime',
            'integer+int64': 'Long'
        ])

        additionalProperties([
            'library': 'spring-boot',
            'beanValidations': 'true',
            'useSpringBoot3': 'true',
            'reactive': 'false'
        ])

        openapiNormalizer([
            'REFACTOR_ALLOF_WITH_PROPERTIES_ONLY': 'true',
            'SIMPLIFY_ONEOF_ANYOF': 'true',
            'NORMALIZE_ENUM_MEMBERS': 'true'
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
            modelNameSuffix "Entity"
            validateSpec false
            userTemplateDir "src/main/resources/orders-templates"

            // Spec-specific template variables
            templateVariables([
                serviceType: "OrderService",
                apiVersion: "v2"
            ])

            // Additional mappings for this spec
            importMappings([
                'OrderStatus': 'com.example.orders.OrderStatus'
            ])
        }
    }
}
```

### Template Library Configuration

```gradle
plugins {
    id 'org.openapi.generator' version '7.14.0'
    id 'com.guidedbyte.openapi-modelgen' version '@version@'
}

dependencies {
    openapiCustomizations 'com.company:enterprise-api-templates:2.1.0'
    openapiCustomizations 'com.company:validation-templates:1.0.0'
    openapiCustomizations files('libs/local-templates.jar')
}

openapiModelgen {
    defaults {
        // Enable library support
        useLibraryTemplates true
        useLibraryCustomizations true

        // Configure precedence to include libraries
        templatePrecedence([
            'user-templates',           // Project templates (highest)
            'user-customizations',      // Project YAML customizations
            'library-templates',        // Library templates
            'library-customizations',   // Library YAML customizations
            'plugin-customizations',    // Built-in plugin customizations
            'openapi-generator'         // OpenAPI defaults (lowest)
        ])
    }

    specs {
        api {
            inputSpec 'src/main/resources/openapi/api.yaml'
            modelPackage 'com.example.api'
        }
    }
}
```

## Best Practices

1. **Start with minimal configuration** - Only specify what you need to override
2. **Use defaults for common settings** - Set shared configuration in defaults block
3. **Override at spec level** - Use spec-specific settings for unique requirements
4. **Test CLI overrides** - Verify command-line options work for your use cases
5. **Version compatibility** - Test configuration with your target OpenAPI Generator version
6. **Template precedence** - Understand how templates are resolved and merged
7. **Validate configurations** - Use `validateSpec` to catch OpenAPI specification issues early