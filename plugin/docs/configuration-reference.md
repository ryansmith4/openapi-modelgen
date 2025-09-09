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
| `templateDir` | String | `null` | Directory containing custom Mustache templates |
| `templateCustomizationsDir` | String | `null` | Directory containing YAML template customizations |
| `modelNamePrefix` | String | `null` | Prefix prepended to generated model class names |
| `modelNameSuffix` | String | `"Dto"` | Suffix added to generated model class names |

```gradle
openapiModelgen {
    defaults {
        outputDir "src/generated/java"
        templateDir "src/main/resources/templates"
        templateCustomizationsDir "src/main/resources/customizations"
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

```gradle
openapiModelgen {
    defaults {
        validateSpec true
        generateModelTests true
        generateModelDocumentation true
    }
}
```

### Template Resolution

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `templateSources` | List\<String\> | `["user-templates", "user-customizations", "library-templates", "library-customizations", "plugin-customizations", "openapi-generator"]` | Template resolution order |
| `debug` | Boolean | `false` | Enable comprehensive debug logging throughout the plugin |

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
        debug true
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
            templateDir "src/main/resources/users-templates"
            
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
./gradlew generateAllModels -Pdebug=true

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
2. **Package names** must be valid Java package names
3. **Directory paths** must be valid and accessible
4. **Boolean values** must be true/false
5. **Template sources** must include valid source types
6. **Template sources** must be ordered from highest to lowest priority

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

## Debug Logging Configuration

The plugin provides comprehensive debug logging to help troubleshoot template resolution, customization processing, and configuration issues.

### Enable Debug Logging

**Global Debug (applies to all specs):**
```gradle
openapiModelgen {
    debug true  // Enable comprehensive debug logging
    
    defaults {
        // Your default settings...
    }
    
    specs {
        // Your spec configurations...
    }
}
```

**Per-Spec Debug:**
```gradle
openapiModelgen {
    defaults {
        debug false  // Debug disabled by default
    }
    
    specs {
        problemSpec {
            inputSpec "problem-spec.yaml"
            modelPackage "com.example.problem"
            debug true  // Enable debug only for this spec
        }
        
        workingSpec {
            inputSpec "working-spec.yaml"
            modelPackage "com.example.working"
            // Uses default debug setting (false)
        }
    }
}
```

**Command Line Override:**
```bash
# Enable debug for single build
./gradlew generateAllModels -Pdebug=true --info

# Enable debug for specific task
./gradlew generateProblemSpec -Pdebug=true --info
```

### Debug Output Information

When debug logging is enabled, you'll see detailed information about:

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
        templateDir "src/main/resources/templates"
        templateCustomizationsDir "src/main/resources/customizations"
        templateSources([
            "user-templates",
            "user-customizations",
            "library-templates",
            "library-customizations",
            "plugin-customizations",
            "openapi-generator"
        ])
        debug false
        
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
            templateCustomizationsDir "src/main/resources/external-customizations"
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