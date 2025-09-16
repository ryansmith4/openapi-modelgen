---
layout: page
title: Configuration Reference
permalink: /configuration-reference/
---

# Configuration Reference

Complete reference for all configuration options in the OpenAPI Model Generator plugin.

## Configuration Structure

```gradle
openapiModelgen {
    defaults {
        // Default settings for all specifications
    }
    specs {
        specName {
            // Individual specification settings
        }
    }
}
```

**Important**: Use method-call syntax (no equals signs) for all configuration.

## Default Configuration

Settings applied to all specifications unless overridden at the spec level.

### Core Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `outputDir` | String | `"build/generated/sources/openapi"` | Base directory for generated code |
| `userTemplateDir` | String | `null` | Directory containing custom Mustache templates |
| `userTemplateCustomizationsDir` | String | `null` | Directory containing YAML template customizations |
| `modelNamePrefix` | String | `null` | Prefix prepended to generated model class names |
| `modelNameSuffix` | String | `"Dto"` | Suffix added to generated model class names |

```gradle
openapiModelgen {
    defaults {
        outputDir "src/generated/java"
        userTemplateDir "src/main/resources/templates"
        userTemplateCustomizationsDir "src/main/resources/customizations"
        modelNamePrefix "Api"
        modelNameSuffix "Model"
    }
}
```

### Generation Control

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `validateSpec` | Boolean | `false` | Validate OpenAPI spec before generation |
| `generateModelTests` | Boolean | `false` | Generate unit tests for models |
| `generateApiTests` | Boolean | `false` | Generate unit tests for APIs |
| `generateApiDocumentation` | Boolean | `false` | Generate API documentation |
| `generateModelDocumentation` | Boolean | `false` | Generate model documentation |
| `saveOriginalTemplates` | Boolean | `false` | Save original templates to orig/ subdirectory for debugging |

```gradle
openapiModelgen {
    defaults {
        validateSpec true
        generateModelTests true
        generateModelDocumentation true
        saveOriginalTemplates true  // Save originals for debugging
    }
}
```

### Template Resolution

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `templateSources` | List&lt;String&gt; | See below | Template resolution order |
| `logLevel` | String | `"INFO"` | Logging level: ERROR, WARN, INFO, DEBUG, or TRACE |

**Default `templateSources`:** `["user-templates", "user-customizations", "library-templates", "library-customizations", "plugin-customizations", "openapi-generator"]`

```gradle
openapiModelgen {
    defaults {
        templateSources([
            "user-templates",
            "user-customizations",
            "library-templates",
            "library-customizations",
            "plugin-customizations",
            "openapi-generator"
        ])
        logLevel "DEBUG"
    }
}
```


### Configuration Options

Advanced OpenAPI Generator configuration options:

```gradle
openapiModelgen {
    defaults {
        configOptions([
            annotationLibrary: "swagger2",
            useSpringBoot3: "true",
            useJakartaEe: "true",
            useBeanValidation: "true",
            dateLibrary: "java8",
            serializableModel: "true"
        ])
    }
}
```

Default config options (automatically applied):

| Option | Default Value | Description |
|--------|---------------|-------------|
| `annotationLibrary` | `"swagger2"` | Use Swagger 2 annotations |
| `swagger2AnnotationLibrary` | `"true"` | Enable Swagger 2 annotation library |
| `useSpringBoot3` | `"true"` | Spring Boot 3 compatibility |
| `useJakartaEe` | `"true"` | Jakarta EE instead of Java EE |
| `useBeanValidation` | `"true"` | Bean validation annotations |
| `dateLibrary` | `"java8"` | Java 8+ date/time types |
| `serializableModel` | `"true"` | Implement Serializable |
| `hideGenerationTimestamp` | `"true"` | Don't include timestamp in files |
| `performBeanValidation` | `"true"` | Runtime bean validation |
| `enumUnknownDefaultCase` | `"true"` | Handle unknown enum values |
| `generateBuilders` | `"true"` | Generate builder methods |
| `legacyDiscriminatorBehavior` | `"false"` | Use modern discriminator handling |
| `disallowAdditionalPropertiesIfNotPresent` | `"false"` | Allow additional properties |
| `useEnumCaseInsensitive` | `"true"` | Case-insensitive enum parsing |
| `openApiNullable` | `"false"` | Don't use OpenAPI nullable library |
| `skipDefaults` | `"true"` | Skip default constructors (Lombok compatibility) |
| `generateConstructorPropertiesAnnotation` | `"false"` | Don't generate @ConstructorProperties |

### Global Properties

```gradle
openapiModelgen {
    defaults {
        globalProperties([
            models: "",
            // Add other global properties as needed
        ])
    }
}
```

Default global properties:

| Property | Default Value | Description |
|----------|---------------|-------------|
| `models` | `""` | Generate only models (no APIs) |

### Template Variables

```gradle
openapiModelgen {
    defaults {
        templateVariables([
            companyName: "Your Company",
            currentYear: "2025",
            customHeader: "Generated by Your System"
        ])
    }
}
```

Default template variables (automatically available):

| Variable | Value | Description |
|----------|--------|-------------|
| `currentYear` | Current year | Dynamically generated current year |
| `generatedBy` | `"OpenAPI Model Generator Plugin"` | Generator identification |
| `pluginVersion` | Plugin version | Current plugin version |

### OpenAPI Generator Mappings

Configure type mappings, import mappings, additional properties, and normalizer rules.

#### Import Mappings

Map type names to fully qualified import statements:

```gradle
openapiModelgen {
    defaults {
        importMappings([
            'UUID': 'java.util.UUID',
            'LocalDate': 'java.time.LocalDate',
            'BigDecimal': 'java.math.BigDecimal'
        ])
    }
}
```

#### Type Mappings

Map OpenAPI types to Java types (supports format-specific mappings):

```gradle
openapiModelgen {
    defaults {
        typeMappings([
            'string+uuid': 'UUID',           // string format=uuid -> UUID
            'string+date': 'LocalDate',      // string format=date -> LocalDate
            'string+date-time': 'LocalDateTime',
            'integer+int64': 'Long'
        ])
    }
}
```

#### Additional Properties

Pass generator-specific configuration options:

```gradle
openapiModelgen {
    defaults {
        additionalProperties([
            'library': 'spring-boot',        // Use Spring Boot library
            'beanValidations': 'true',       // Enable bean validation
            'useSpringBoot3': 'true',        // Use Spring Boot 3 features
            'reactive': 'false',             // Disable reactive features
            'serializableModel': 'true'      // Make models serializable
        ])
    }
}
```

#### OpenAPI Normalizer Rules

Transform and normalize OpenAPI specifications before generation:

```gradle
openapiModelgen {
    defaults {
        openapiNormalizer([
            'REFACTOR_ALLOF_WITH_PROPERTIES_ONLY': 'true',  // Simplify allOf schemas
            'SIMPLIFY_ONEOF_ANYOF': 'true',                 // Simplify oneOf/anyOf schemas
            'KEEP_ONLY_FIRST_TAG_IN_OPERATION': 'true',     // Keep only first tag per operation
            'SIMPLIFY_BOOLEAN_ENUM': 'true',                // Convert boolean enums to booleans
            'SET_TAGS_FOR_ALL_OPERATIONS': 'default',       // Set default tag for untagged operations
            'NORMALIZE_ENUM_MEMBERS': 'true'                // Normalize enum member names
        ])
    }
}
```

**Common Normalizer Rules:**
- `REFACTOR_ALLOF_WITH_PROPERTIES_ONLY`: Simplifies `allOf` schemas that only contain properties
- `SIMPLIFY_ONEOF_ANYOF`: Simplifies `oneOf`/`anyOf` schemas where possible
- `KEEP_ONLY_FIRST_TAG_IN_OPERATION`: Keeps only the first tag for operations with multiple tags
- `SIMPLIFY_BOOLEAN_ENUM`: Converts enum with only true/false values to boolean type
- `SET_TAGS_FOR_ALL_OPERATIONS`: Adds a default tag to operations without tags
- `NORMALIZE_ENUM_MEMBERS`: Normalizes enum member names for consistency

#### Mapping Precedence

All mapping properties follow the same merge pattern:
1. **Default-level mappings** are applied first
1. **Spec-level mappings** are merged in, with spec values taking precedence for duplicate keys
1. **Final merged mappings** are passed to OpenAPI Generator

## Specification Configuration

Settings for individual OpenAPI specifications.

### Required Settings

| Property | Type | Description |
|----------|------|-------------|
| `inputSpec` | String | **Required** - Path to OpenAPI specification file |
| `modelPackage` | String | **Required** - Java package for generated models |

```gradle
openapiModelgen {
    specs {
        myApi {
            inputSpec "src/main/resources/openapi/api.yaml"
            modelPackage "com.example.api.model"
        }
    }
}
```

### Optional Spec Settings

All default configuration options can be overridden at the spec level:

```gradle
openapiModelgen {
    specs {
        usersApi {
            inputSpec "src/main/resources/openapi/users.yaml"
            modelPackage "com.example.users.model"
            
            // Override defaults for this spec only
            modelNamePrefix "Legacy"
            modelNameSuffix "Entity"
            validateSpec true
            userTemplateDir "src/main/resources/users-templates"
            
            configOptions([
                useJakartaEe: "false",  // Use Java EE for this spec
                dateLibrary: "legacy"   // Use legacy date handling
            ])
        }
    }
}
```

## Command-Line Overrides

All configuration options support command-line overrides using `@Option` annotations:

```bash
# Override output directory
./gradlew generateAllModels -PoutputDir=src/generated

# Enable validation
./gradlew generateMyApi -PvalidateSpec=true

# Debug template resolution
./gradlew generateAllModels -PlogLevel=DEBUG

# Override model package
./gradlew generateUsersApi -PmodelPackage=com.custom.model
```

## Environment Variables

Configuration can also be provided via environment variables:

```bash
export OPENAPI_MODELGEN_OUTPUT_DIR="src/generated/java"
export OPENAPI_MODELGEN_VALIDATE_SPEC="true"
./gradlew generateAllModels
```

## Configuration Validation

The plugin performs comprehensive validation of configuration:

### Validation Rules

1. **Required properties** must be specified
1. **Package names** must be valid Java package names
1. **Directory paths** must be valid and accessible
1. **Boolean values** must be true/false
1. **Template sources** must include valid source types
1. **Template sources** must be ordered from highest to lowest priority

### Validation Examples

**Valid configuration:**
```gradle
openapiModelgen {
    specs {
        valid {
            inputSpec "existing-file.yaml"
            modelPackage "com.valid.package"
        }
    }
}
```

**Invalid configurations:**
```gradle
openapiModelgen {
    specs {
        invalid1 {
            // Missing required inputSpec
            modelPackage "com.example"
        }
        invalid2 {
            inputSpec "nonexistent.yaml"
            modelPackage "invalid-package-name!"  // Invalid characters
        }
        invalid3 {
            inputSpec "spec.yaml"
            modelPackage "com.example"
            templateSources(["invalid-source"])  // Unknown source type
        }
    }
}
```

## Logging Configuration

The plugin provides comprehensive logging to help troubleshoot template resolution, customization processing, and configuration issues.

### Configure Log Level

**Global Log Level (applies to all specs):**
```gradle
openapiModelgen {
    defaults {
        logLevel "DEBUG"  // Enable debug logging for all specs
        // Available levels: ERROR, WARN, INFO, DEBUG, TRACE
    }

    specs {
        // Your spec configurations...
    }
}
```

**Per-Spec Log Level:**
```gradle
openapiModelgen {
    defaults {
        logLevel "INFO"  // Default logging level
    }

    specs {
        problemSpec {
            inputSpec "problem-spec.yaml"
            modelPackage "com.example.problem"
            logLevel "DEBUG"  // Enable debug logging only for this spec
        }

        workingSpec {
            inputSpec "working-spec.yaml"
            modelPackage "com.example.working"
            // Uses default logLevel setting (INFO)
        }
    }
}
```

**Command Line Override:**
```bash
# Enable debug logging for single build
./gradlew generateAllModels -PlogLevel=DEBUG --info

# Enable debug logging for specific task
./gradlew generateProblemSpec -PlogLevel=DEBUG --info

# Enable trace logging for maximum detail
./gradlew generateAllModels -PlogLevel=TRACE --info
```

### Available Log Levels

| Level | Description |
|-------|-------------|
| `ERROR` | Only error messages |
| `WARN` | Warnings and errors |
| `INFO` | General information, warnings, and errors (default) |
| `DEBUG` | Detailed debugging information |
| `TRACE` | Maximum detail for troubleshooting |

### Debug Output Information

When debug or trace logging is enabled, you'll see detailed information about:

- **Template Resolution**: Which template sources are available and used
- **Template Extraction**: Base template discovery and content processing
- **Customization Processing**: YAML customization application steps
- **Configuration Validation**: Detailed validation results and error diagnosis
- **Cache Operations**: Cache hits, misses, and invalidation events
- **Performance Metrics**: Template processing timing and statistics

### Debug Output Examples

**Template Resolution:**
```
=== Template Resolution Debug for 'spring' ===
Configured template sources: [user-templates, user-customizations, plugin-customizations, openapi-generator]
Available template sources: [user-customizations, plugin-customizations, openapi-generator]
✅ user-customizations: C:\project\src\main\templateCustomizations
✅ plugin-customizations: built-in YAML customizations
✅ openapi-generator: OpenAPI Generator default templates (fallback)
=== End Template Resolution Debug ===
```

**Template Processing:**
```
=== CUSTOMIZATION ENGINE ENTRY ===
Template length: 2547
Config name: pojo.mustache
Config has replacements: 2
Template starts with: '{{>licenseInfo}}{{#models}}{{#model}}'
=== CUSTOMIZATION DEBUG: Finished template customization ===
```

## Complete Configuration Example

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.1'
    id 'org.openapi.generator' version '7.14.0'
    id 'com.guidedbyte.openapi-modelgen'
}

openapiModelgen {
    defaults {
        // Core settings
        outputDir "build/generated/sources/openapi"
        modelNameSuffix "Dto"
        
        // Generation control
        validateSpec false
        generateModelTests false
        
        // Template settings
        userTemplateDir "src/main/resources/templates"
        userTemplateCustomizationsDir "src/main/resources/customizations"
        templateSources([
            "user-templates",
            "user-customizations",
            "library-templates",
            "library-customizations",
            "plugin-customizations",
            "openapi-generator"
        ])
        logLevel "INFO"
        
        // Template variables
        templateVariables([
            companyName: "Acme Corporation",
            projectName: "My Project"
        ])
        
        // Advanced configuration
        configOptions([
            annotationLibrary: "swagger2",
            useSpringBoot3: "true",
            useBeanValidation: "true"
        ])
        
        // Global properties
        globalProperties([
            models: "",
            supportingFiles: "false"
        ])
    }
    
    specs {
        users {
            inputSpec "src/main/resources/openapi/users.yaml"
            modelPackage "com.example.users.model"
        }
        
        orders {
            inputSpec "src/main/resources/openapi/orders.yaml"
            modelPackage "com.example.orders.model"
            
            // Spec-specific overrides
            modelNameSuffix "Entity"
            validateSpec true
        }
        
        external {
            inputSpec "src/main/resources/openapi/external-api.yaml"
            modelPackage "com.example.external.model"
            
            // Different template customizations for external API
            userTemplateCustomizationsDir "src/main/resources/external-customizations"
        }
    }
}

// Make compilation depend on model generation
compileJava.dependsOn 'generateAllModels'
```

## Performance Configuration

For optimal performance in large projects:

```gradle
openapiModelgen {
    defaults {
        // Enable all performance features (default: true)
        parallel true  // Parallel multi-spec processing
        
        // Template resolution optimization
        templateSources([
            "user-templates",      // Check user templates first
            "user-customizations", // Then user customizations
            "library-templates",   // Library templates
            "library-customizations", // Library customizations
            "plugin-customizations", // Then plugin customizations  
            "openapi-generator"    // OpenAPI Generator defaults last
        ])
    }
}
```

Use with Gradle configuration cache for maximum performance:

```bash
./gradlew generateAllModels --configuration-cache
```

This reference covers all available configuration options. For practical examples, see the [Examples Gallery](examples.md).