# OpenAPI Model Generator Gradle Plugin

A comprehensive Gradle plugin for generating Java DTOs from multiple OpenAPI specifications with enterprise-grade
features including template customization, incremental builds, performance optimizations, and automatic version detection.

## Features

### Core Functionality

- **Multi-spec support**: Generate models from multiple OpenAPI specifications in a single project
- **Spec-level configuration**: Individual validation, test generation, and template settings per specification
- **Template customization system**: YAML-based template modifications with comprehensive validation
- **Template precedence hierarchy**: Explicit user templates > User YAML customizations > Plugin YAML customizations >
  OpenAPI generator defaults
- **Template library support**: Share and reuse templates/customizations via dependency JARs
- **Incremental build support**: Only regenerates when inputs actually change
- **Lombok integration**: Full annotation support with constructor conflict resolution
- **Template variable expansion**: Nested variables like `{{copyright}}` containing `{{currentYear}}`
- **OpenAPI Generator mapping support**: Import mappings, type mappings, and additional properties with merging

### Performance & Enterprise Features

- **Multi-level caching system**: Comprehensive session → local → global cache hierarchy with thread safety and
  cross-build persistence
- **Parallel multi-spec processing**: Thread-safe concurrent generation with configurable parallel execution control
- **Cross-build performance optimization**: Global cache persistence with 90% faster no-change builds and
  70% faster incremental builds
- **Automatic version detection**: Works with any OpenAPI Generator version from configuration plugins
- **Clean template discovery**: Uses OpenAPI Generator's official CodegenConfig API without JAR scanning
- **Dynamic generator support**: Works with any generator (spring, java, etc.) from configuration
- **Content-based change detection**: SHA-256 hashing for reliable template change detection
- **Lazy template extraction**: Templates extracted at execution time for better performance
- **Configuration validation**: Comprehensive validation with detailed error reporting
- **Template extraction caching**: High-performance template caching with metadata and cross-build persistence
- **Partial template override**: Only provide templates you want to customize, following OpenAPI Generator best practices

### Developer Experience

- **Command-line options**: Override any configuration via CLI parameters
- **Comprehensive testing**: Complete test coverage with 125+ tests passing (100% success rate, all functionality
  fully verified including caching optimizations)
- **Auto-generated documentation**: Plugin help and comprehensive Javadoc documentation
- **@Internal property optimization**: Precise incremental build invalidation

## Usage

### Important: DSL Syntax

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
templateDir = null                                 // Custom template directory (not set by default)
templateCustomizationsDir = null                  // YAML customizations directory (not set by default)
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
```

### Template Resolution

```gradle
templatePrecedence = [                            // Template resolution priority order
    "user-templates",                             // Project Mustache templates (highest)
    "user-customizations",                        // Project YAML customizations
    "plugin-customizations",                      // Built-in plugin YAML customizations
    "openapi-generator"                          // OpenAPI Generator defaults (lowest)
]
debugTemplateResolution = false                   // Show template source debug info
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
pluginVersion = "1.x.x"                         // Plugin version (from build)
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

**Important**: You must apply the OpenAPI Generator plugin before applying this plugin:

```gradle
plugins {
    id 'org.openapi.generator' version '7.14.0'  // Or your preferred version 7.10.0+
    id 'com.guidedbyte.openapi-modelgen' version '@version@'
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
        outputDir "build/generated/sources/openapi"
        templateDir "src/main/resources/openapi-templates"
        templateCustomizationsDir "src/main/resources/template-customizations"
        modelNameSuffix "Dto"
        validateSpec true
        parallel true                       // Enable thread-safe parallel processing (default: true)
        applyPluginCustomizations true      // Enable built-in YAML customizations
        
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
        
        // OpenAPI Generator mappings
        importMappings([
            'UUID': 'java.util.UUID',
            'LocalDate': 'java.time.LocalDate',
            'LocalDateTime': 'java.time.LocalDateTime'
        ])
        typeMappings([
            'string+uuid': 'UUID',
            'string+date': 'LocalDate',
            'string+date-time': 'LocalDateTime'
        ])
        additionalProperties([
            'library': 'spring-boot',
            'beanValidations': 'true',
            'useSpringBoot3': 'true'
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
            templateCustomizationsDir "src/main/resources/orders-customizations"
            modelNameSuffix "Entity"
            validateSpec false          // Disable validation for this spec
            applyPluginCustomizations false // Disable plugin YAML customizations for this spec
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
            srcDirs += file("build/generated/sources/openapi/src/main/java")
        }
    }
}

// Ensure generation runs before compilation
compileJava.dependsOn generateAllModels

// Optional: Clean generated sources on clean
clean {
    delete file("build/generated/sources/openapi")
}
```

## Generated Tasks

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

# Get help
./gradlew generateHelp
```

## Template Customization

The plugin provides a comprehensive template customization system with multiple approaches to modify generated code.

📖 **For detailed technical documentation** of the template system architecture, working directory organization, and
selective backup mechanisms, see [template-system.md](plugin/docs/template-system.md).

### Template Directory Architecture

The plugin uses a sophisticated orchestration system to combine templates from multiple sources:

#### User Directories (Source)
- **`templateDir`**: Your custom Mustache templates (e.g., `src/main/templates/{generator}/*.mustache`)
- **`templateCustomizationsDir`**: Your YAML customizations (e.g., `src/main/templateCustomizations/{generator}/*.mustache.yaml`)

#### Build Directory (Orchestration)
- **`build/template-work/{generator}`**: The orchestrated template directory used by OpenAPI Generator
  - Contains user templates (copied from `templateDir`)
  - Contains extracted OpenAPI Generator templates (only those being customized)
  - Has all YAML customizations applied (plugin, user, library)
  - **This is the actual directory passed to OpenAPI Generator**

#### Key Point
The `templateDir` and `templateCustomizationsDir` are **source directories** - they provide inputs to the template orchestration process. The actual code generation uses the fully processed `build/template-work/{generator}` directory, ensuring clean separation between your source files and build outputs.

### Template Precedence Hierarchy

The plugin follows a strict precedence hierarchy when resolving templates:

1. **Explicit User Templates** (highest precedence) - Complete `.mustache` files in `templateDir`
2. **User YAML Customizations** - Modifications defined in `templateCustomizationsDir`
3. **Library Templates** - Templates from template library JARs (if enabled)
4. **Library YAML Customizations** - YAML customizations from template library JARs (if enabled)
5. **Plugin YAML Customizations** - Built-in optimizations (if any configured)
6. **OpenAPI Generator Defaults** (lowest precedence) - Standard OpenAPI Generator templates

### Template Libraries

The plugin supports sharing templates and customizations via library dependencies, enabling teams to standardize code
generation across projects.

#### Library Structure

Template libraries are JAR files with the following structure:

```text
my-api-templates-1.0.0.jar
├── META-INF/
│   ├── openapi-templates/          # Mustache templates
│   │   └── spring/
│   │       ├── pojo.mustache
│   │       └── model.mustache
│   └── openapi-customizations/     # YAML customizations
│       └── spring/
│           ├── pojo.mustache.yaml
│           └── model.mustache.yaml
```

#### Using Template Libraries

1. **Add dependencies** to the `openapiCustomizations` configuration:

```gradle
plugins {
    id 'org.openapi.generator' version '7.14.0'
    id 'com.guidedbyte.openapi-modelgen' version '@version@'
}

dependencies {
    // Regular dependencies
    implementation 'org.openapitools:openapi-generator:7.14.0'
    
    // Template library dependencies
    openapiCustomizations 'com.company:api-templates:1.0.0'
    openapiCustomizations 'com.company:validation-templates:2.1.0'
    openapiCustomizations files('libs/local-templates.jar')
}
```

2. **Configure library usage** in your plugin configuration:

```gradle
openapiModelgen {
    defaults {
        // Enable library support
        useLibraryTemplates true
        useLibraryCustomizations true
        
        // Configure template precedence to include libraries
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
        pets {
            inputSpec 'src/main/resources/openapi/pets.yaml'
            modelPackage 'com.example.pets'
        }
    }
}
```

#### Library Configuration Options

- **`useLibraryTemplates`** - Enable template extraction from library JARs (default: false)
- **`useLibraryCustomizations`** - Enable YAML customization extraction from library JARs (default: false)
- **`templatePrecedence`** - Must include `'library-templates'` and/or `'library-customizations'` when enabled

#### Advanced Library Features

Template libraries can include metadata for version compatibility, generator support, and feature requirements:

##### Library Metadata

Add `META-INF/openapi-library.yaml` to provide library information:

```yaml
# META-INF/openapi-library.yaml
name: "enterprise-api-templates"
version: "2.1.0"
description: "Enterprise-grade OpenAPI templates with validation and documentation"
author: "Platform Team"
homepage: "https://github.com/company/api-templates"

# Generator compatibility
supportedGenerators:
  - "spring"        # Only works with Spring generator
  - "java"          # Also supports Java generator

# Version requirements
minOpenApiGeneratorVersion: "7.11.0"  # Requires OpenAPI Generator 7.11.0+
maxOpenApiGeneratorVersion: "8.0.0"   # Not tested above 8.0.0
minPluginVersion: "1.2.0"             # Requires this plugin version

# Feature dependencies
requiredFeatures:
  - "validation"    # Needs bean validation support
  - "lombok"        # Requires Lombok annotations
  - "jackson"       # Requires Jackson serialization

# Optional features provided by this library  
features:
  customValidation: true
  lombokSupport: true
  springBootIntegration: "3.x"
  apiDocumentation: "openapi-3.0"

# External dependencies this library expects
dependencies:
  - "org.springframework:spring-core:6.0+"
  - "jakarta.validation:jakarta.validation-api:3.0+"
```

##### Generator Compatibility Filtering

When metadata specifies `supportedGenerators`, the plugin automatically filters library content:

```gradle
// This configuration uses the default 'spring' generator
openapiModelgen {
    defaults {
        useLibraryTemplates true
        useLibraryCustomizations true
    }
    specs {
        api {
            inputSpec 'api.yaml'
            modelPackage 'com.example.api'
            // Uses default 'spring' generator
        }
    }
}
```

**Behavior:**
- ✅ Libraries supporting 'spring' generator: All templates and customizations used
- ❌ Libraries supporting only 'java' generator: Build fails with compatibility error
- 📝 Error message: "Library 'my-lib-1.0.0' does not support generator(s): [spring]. Supported generators: [java]"

##### Version Compatibility Validation

The plugin validates library requirements against your environment:

```
INFO: Library 'enterprise-templates-2.1.0' requires OpenAPI Generator version 7.11.0+
WARN: Library 'legacy-templates-1.0.0' supports OpenAPI Generator up to version 7.14.0 (current: 7.16.0)
ERROR: Library 'new-templates-3.0.0' requires plugin version 1.3.0+ (current: 1.2.0)
```

##### Configuration Validation

The plugin validates library setup during configuration:

```gradle
openapiModelgen {
    defaults {
        useLibraryTemplates true
        // ERROR: Missing library-templates in templatePrecedence
        templatePrecedence(['user-templates', 'openapi-generator'])
    }
}
```

**Error:** "useLibraryTemplates is enabled but 'library-templates' is not in templatePrecedence"

##### Library Discovery Logging

Enable debug logging to see library processing:

```gradle
openapiModelgen {
    defaults {
        debugTemplateResolution true
    }
}
```

**Output:**
```
INFO: Processing 2 template library dependencies
DEBUG: Library 'enterprise-templates-2.1.0': Extracted 5 templates, 3 customizations  
DEBUG: Library 'validation-templates-1.0.0': Extracted 2 templates, 1 customizations
DEBUG: Template 'pojo.mustache' resolved from: library-templates (enterprise-templates-2.1.0)
DEBUG: Template 'model.mustache' resolved from: user-customizations  
```

**Important**: While the plugin has no embedded Mustache templates, it does include YAML customizations that enhance
the readability of generated code. The plugin relies on OpenAPI Generator for base templates and uses a recursive
template discovery approach that only extracts templates that have customizations and their dependencies.
When an explicit user template exists for a specific template (e.g., `pojo.mustache`), ALL customizations
(both user and plugin YAML customizations) for that template are ignored.

### Directory Structure

Templates must be organized in generator-specific subdirectories:

```text
src/main/resources/
├── openapi-templates/           # Explicit user templates
│   └── spring/                 # Generator-specific directory  
│       ├── pojo.mustache       # Complete template override
│       └── enumClass.mustache  # Complete template override
└── template-customizations/    # YAML-based customizations
    └── spring/                 # Generator-specific directory
        ├── pojo.mustache.yaml  # Template modifications
        └── model.mustache.yaml # Template modifications
```

### YAML-Based Template Customizations

YAML customizations allow you to modify existing templates without replacing them entirely:

```yaml
# src/main/resources/template-customizations/spring/pojo.mustache.yaml
insertions:
  - before: "{{#description}}"
    content: |
      /**
       * Custom header for all generated classes
       * Generated on {{currentYear}} by {{companyName}}
       */

  - after: "public class {{classname}}"
    content: |
      
      // Custom field added to all POJOs
      private String customField;

replacements:
  - pattern: "{{#deprecated}}"
    replacement: "{{#deprecated}}\n  // DEPRECATED - Use newer version"
```

### YAML Customization Types

#### Insertions

Add content before, after, or at specific patterns:

```yaml
insertions:
  - before: "{{#vars}}"            # Insert before this pattern
    content: |                     # Literal block scalar (|) preserves formatting
      // Custom comment before variables
      
  - after: "public {{datatype}} {{getter}}()" # Insert after this pattern
    content: |
      
      // Added logging for getter access
      log.trace("Accessing field: {{name}}");
```

#### Whitespace Control

YAML provides different ways to handle whitespace in multi-line content:

```yaml
insertions:
  # YAML block scalar behavior - strips common indentation from all lines
  - after: "{{#serializableModel}}"
    content: |
        @java.io.Serial  # 8 spaces in YAML, but becomes 0 spaces in output
                         # (YAML strips the 8-space baseline indentation)
      
  # Explicit indentation indicator - preserves exact amount of spaces
  - after: "{{#serializableModel}}" 
    content: |2
        @java.io.Serial  # Preserves exactly 2 spaces of indentation
      
  # Folded block scalar (>) - folds lines but preserves paragraph breaks  
  - before: "{{#description}}"
    content: >
      This long comment will be folded into a single line
      but paragraph breaks are preserved.
      
  # Quoted string for precise control - exactly what you specify
  - after: "{{#serializableModel}}"
    content: "  @java.io.Serial\n"  # Exactly 2 spaces and a newline
```

**Important YAML Block Scalar Behavior**:

- YAML block scalars (`|`) determine indentation from the **first non-empty line** after the indicator
- This baseline indentation is then **stripped from all lines** in the block
- To preserve leading spaces, use:
  - **Explicit indentation indicators** (`|2` preserves 2 spaces)
  - **Quoted strings** for exact control (`"  text\n"`)
- The template processor does NOT normalize or adjust whitespace - it uses exactly what YAML provides

#### Replacements

Replace existing content with new content:

```yaml
replacements:
  - pattern: "{{#vendorExtensions.x-extra-annotation}}"
    replacement: "{{#vendorExtensions.x-extra-annotation}}\n  @CustomAnnotation"
    
  - pattern: "private {{datatype}} {{name}};"
    replacement: "private {{datatype}} {{name}}; // Custom field comment"
```

#### Conditional Processing

Apply customizations only when certain conditions are met:

```yaml
insertions:
  - before: "{{#description}}"
    content: |
      /**
       * Enterprise class header - {{description}}
       */
    conditions:
      generatorVersion: ">=7.11.0"
      templateContains: "@Valid"
```

### Custom Template Variables

Template variables support nesting and inheritance:

```gradle
templateVariables = [
    copyright: "Copyright © {{currentYear}} {{companyName}}",  // Nested variables
    currentYear: "2025",
    companyName: "My Company Inc.",
    customHeader: "Generated by {{companyName}} on {{currentYear}}",
    buildInfo: "Build {{buildNumber}} - {{currentYear}}"
]
```

Variables are processed recursively, so `{{copyright}}` will expand to include the resolved `{{currentYear}}` and
`{{companyName}}` values.

### YAML Customization Schema

Template customization files follow a structured schema with comprehensive validation:

```yaml
# Complete YAML customization schema
metadata:                                    # Optional metadata section
  name: "string"                            # Customization name
  description: "string"                     # Description
  version: "string"                         # Version identifier

insertions:                                  # List of content insertions
  - after: "string"                         # Insert content after this pattern
    before: "string"                        # OR insert content before this pattern  
    at: "start|end"                         # OR insert at template start/end
    content: "string"                       # Content to insert (supports {{variables}})
    conditions:                             # Optional conditions (see Condition Schema)
      templateNotContains: "string"         # Only if template doesn't contain pattern
      templateContains: "string"            # Only if template contains pattern
      generatorVersion: ">=7.11.0"          # Version constraint
    fallback:                               # Optional fallback insertion
      content: "string"                     # Alternative content if conditions fail

replacements:                               # List of content replacements  
  - pattern: "string"                       # Pattern to find and replace
    replacement: "string"                   # Replacement content
    conditions:                             # Optional conditions (same as insertions)
      templateNotContains: "string"
```

#### Condition Schema

Conditions support complex logic for version-aware and context-sensitive customizations:

```yaml
conditions:
  # Version conditions
  generatorVersion: ">=7.11.0"              # Semantic versioning constraint
  
  # Template content conditions  
  templateContains: "string"                # Must contain pattern
  templateNotContains: "string"             # Must NOT contain pattern
  templateContainsAll: ["str1", "str2"]     # Must contain all patterns
  templateContainsAny: ["str1", "str2"]     # Must contain at least one
  
  # Feature detection
  hasFeature: "jackson"                     # Must support feature
  hasAllFeatures: ["jackson", "validation"] # Must support all features
  hasAnyFeatures: ["jackson", "xml"]        # Must support any feature
  
  # Environment conditions
  projectProperty: "property=value"          # Project property check
  environmentVariable: "VAR=value"           # Environment variable check
  buildType: "debug|release"                # Build type check
  
  # Logical operators
  allOf: [condition1, condition2]           # All conditions must be true
  anyOf: [condition1, condition2]           # Any condition must be true  
  not: condition                            # Condition must be false
```

**📘 For complete schema documentation with examples and validation rules, see our [GitHub Pages Documentation](https://guidedbyte.github.io/openapi-modelgen/template-schema.html).**

### Supported Template Files

The plugin supports customization of all OpenAPI Generator template files:

#### Model Templates

- `pojo.mustache` - Main model class template
- `enumClass.mustache` - Enum class template
- `model.mustache` - Model file wrapper template

#### Annotation Templates

- `additionalModelTypeAnnotations.mustache` - Class-level annotations
- `generatedAnnotation.mustache` - @Generated annotation format
- `typeInfoAnnotation.mustache` - Type information annotations
- `beanValidation.mustache` - Bean validation annotations

#### Utility Templates

- `xmlAnnotation.mustache` - XML binding annotations
- `nullableAnnotation.mustache` - Nullable type annotations
- `jackson_annotations.mustache` - Jackson JSON annotations

### Configuration Options

#### Global Template Settings

```gradle
openapiModelgen {
    defaults {
        templateDir "src/main/resources/openapi-templates"              // Explicit templates
        templateCustomizationsDir "src/main/resources/customizations"   // YAML customizations
        
        templateVariables([
            companyName: "Acme Corp",
            currentYear: "2025"
        ])
    }
}
```

#### Spec-Level Template Overrides

```gradle
specs {
    orders {
        inputSpec "specs/orders.yaml"
        modelPackage "com.example.orders"
        
        // Override templates for this spec only
        templateDir "src/main/resources/orders-templates"
        templateCustomizationsDir "src/main/resources/orders-customizations"
        
        // Spec-specific variables
        templateVariables([
            serviceType: "OrderService",
            apiVersion: "v2"
        ])
    }
}
```

### OpenAPI Generator Mappings

The plugin provides comprehensive support for OpenAPI Generator's mapping configuration options:

#### Import Mappings

Map type names to fully qualified import statements. These are merged between default and spec levels, with spec-level taking precedence:

```gradle
openapiModelgen {
    defaults {
        importMappings([
            'UUID': 'java.util.UUID',
            'LocalDate': 'java.time.LocalDate',
            'BigDecimal': 'java.math.BigDecimal'
        ])
    }
    specs {
        mySpec {
            importMappings([
                'Instant': 'java.time.Instant',  // Additional import for this spec
                'UUID': 'java.lang.String'      // Overrides default UUID mapping
            ])
        }
    }
}
```

#### Type Mappings  

Map OpenAPI types to Java types. Supports format-specific mappings with `+` notation:

```gradle
typeMappings([
    'string+uuid': 'UUID',           // string format=uuid -> UUID
    'string+date': 'LocalDate',      // string format=date -> LocalDate
    'string+date-time': 'LocalDateTime',
    'integer+int64': 'Long'
])
```

#### Additional Properties

Pass generator-specific configuration options equivalent to `--additional-properties` CLI option:

```gradle
additionalProperties([
    'library': 'spring-boot',        // Use Spring Boot library
    'beanValidations': 'true',       // Enable bean validation
    'useSpringBoot3': 'true',        // Use Spring Boot 3 features
    'reactive': 'false',             // Disable reactive features
    'serializableModel': 'true'      // Make models serializable
])
```

#### Mapping Precedence

All mapping properties follow the same merge pattern:
1. **Default-level mappings** are applied first
2. **Spec-level mappings** are merged in, with spec values taking precedence for duplicate keys
3. **Final merged mappings** are passed to OpenAPI Generator

### Best Practices

1. **Use YAML customizations** for small modifications and additions
2. **Use explicit templates** only when you need complete control over the template structure
3. **Organize by generator** - always use generator-specific subdirectories (e.g., `spring/`)
4. **Test precedence** - remember that explicit templates override all customizations
5. **Version compatibility** - test customizations with your target OpenAPI Generator version

### Troubleshooting Template Customization

#### Common Issues

- **Customizations ignored**: Check if an explicit user template exists for the same template name
- **Pattern not matching**: Verify the exact pattern exists in the base template
- **YAML parsing errors**: Validate YAML syntax and structure
- **Generator directory missing**: Ensure templates are in generator-specific subdirectories

#### Debug Template Processing

```bash
# Enable debug logging to see template resolution
./gradlew generatePets --info

# See which templates are being used
./gradlew generatePets --debug | grep "template"
```

## Dependencies & Version Management

The plugin automatically:

- **Detects OpenAPI Generator** version from your project configuration
- **Works with corporate plugins** that manage dependency versions
- **Validates compatibility** and warns about unsupported versions (requires 7.10.0+)
- **Adds required dependencies**: Lombok, Jackson, Spring Boot validation, JSR-305

**Note**: As of version 1.1.0+, you must provide the OpenAPI Generator plugin yourself. This allows you to choose any
version 7.10.0+ and ensures compatibility with corporate dependency management.

### Corporate Environment Support

```gradle
plugins {
    id 'com.company.java-standards'        // Provides OpenAPI Generator 8.2.0
    id 'com.guidedbyte.openapi-modelgen'   // Automatically detects and uses 8.2.0
}
// No additional configuration needed - respects corporate version management
```

## Performance Features

### Multi-Level Caching System

- **Session-scoped extraction cache**: Eliminates redundant template extractions within single build execution
- **Working directory cache**: SHA-256 content validation for reliable template change detection
- **Global cache persistence**: Cross-build performance stored in `~/.gradle/caches/openapi-modelgen/` with 90% faster
  no-change builds and 70% faster incremental builds
- **Thread-safe caching**: ConcurrentHashMap-based caches supporting parallel multi-spec generation
- **Cache invalidation**: Comprehensive invalidation based on OpenAPI Generator version, specs, templates,
  customizations, and plugin version

### Parallel Processing & Optimization

- **Thread-safe parallel processing**: Configurable parallel multi-spec generation with `parallel` configuration
  option
- **Incremental builds**: Only regenerates when OpenAPI specs or templates change
- **Template caching**: Efficient template extraction and validation with SHA-256 hashing
- **Comprehensive template pre-extraction**: Automatically extracts base templates for ANY customization file
  (plugin or user)
- **Recursive template discovery**: Intelligently discovers and extracts template dependencies
- **Selective template processing**: Only processes templates that require customization
- **Lazy evaluation**: Template extraction deferred until task execution
- **Template precedence optimization**: Early detection of explicit user templates to skip unnecessary processing
- **Configuration validation**: Comprehensive validation with detailed error messages
- **Smart customization skipping**: Automatically skips plugin customizations when user customizations exist
- **Customization result caching**: Thread-safe caching of YAML customization processing with SHA-256 key generation

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
cd test-app
./gradlew generatePets
./gradlew generateAllModels
./gradlew build
```

### Test Coverage

Comprehensive test suite includes:

- **Unit tests**: Core functionality with ProjectBuilder
- **Integration tests**: Real Gradle environment with TestKit
- **Template precedence tests**: Comprehensive template resolution testing
- **Performance tests**: Incremental builds and caching validation
- **Configuration validation tests**: Error handling and validation

## Project Structure

```text
openapi-modelgen/
├── plugin/                          # Main Gradle plugin implementation
│   ├── src/main/java/com/guidedbyte/openapi/modelgen/
│   │   ├── OpenApiModelGenPlugin.java        # Core plugin with advanced optimizations
│   │   ├── OpenApiModelGenExtension.java     # DSL extension with multi-spec support
│   │   ├── DefaultConfig.java                # Global defaults configuration
│   │   └── SpecConfig.java                   # Individual spec configuration
│   ├── src/main/resources/
│   │   ├── templates/spring/                 # (Empty - plugin has no embedded templates)
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
        └── orders.yaml              # Orders API specification
```

## Technical Details

### Requirements

- **Java 17+**
- **Gradle 8.0+**
- **OpenAPI Generator 7.10.0+** (must be provided by consumer project)

**Note**: This plugin uses a `compileOnly` dependency approach, allowing you to choose your preferred OpenAPI Generator
version. This design prevents dependency conflicts and enables compatibility with corporate dependency management systems.

### Key Technical Features

- **Composite build structure**: Plugin in separate directory with `includeBuild`
- **Template resolution**: Precedence-based template loading with JAR resource extraction
- **Recursive template discovery**: Parses templates for `{{>templateName}}` references and extracts dependencies
- **Partial template override**: Only extracts/provides templates that need customization
- **SHA-256 content hashing**: Template change detection for efficient caching
- **Configuration validation**: Comprehensive error reporting with actionable messages
- **Lazy evaluation**: Template extraction deferred until task execution
- **Mustache template engine**: Custom templates with HTML escaping controls

### Troubleshooting

#### Build Issues

- **Build errors**: Ensure composite build is configured properly
- **Syntax errors**: Use method-call syntax (no `=` equals) - see DSL Syntax section above
- **Template customizations ignored**: Check if explicit user templates exist for the same template name
- **YAML parsing errors**: Validate YAML structure and ensure proper generator subdirectory organization
- **Template spacing**: Use custom templates to control whitespace
- **Constructor conflicts**: Plugin handles Lombok conflicts automatically
- **Configuration validation**: Check error messages for missing specs or invalid packages
- **Template precedence issues**: Remember that explicit templates override ALL customizations

#### Known OpenAPI Generator Limitations

The plugin itself works correctly, but there are known bugs in the underlying OpenAPI Generator that affect certain configuration combinations:

**Issue: Type/Import Mappings with Model Name Prefixes/Suffixes**

- **Affected Versions**: OpenAPI Generator 5.4.0+ (including 7.x series)
- **Problem**: When using `modelNamePrefix` or `modelNameSuffix` together with `typeMappings` and `importMappings`, the prefix/suffix gets incorrectly applied to the type mapping targets, causing the import mappings to fail
- **Symptoms**: 
  - Instead of `java.time.LocalDate`, you get `ApiLocalDate` or `LocalDateDto` wrapper models
  - Import statements show package-local models instead of configured external types
- **Upstream Issues**: 
  - [OpenAPITools/openapi-generator#19043](https://github.com/OpenAPITools/openapi-generator/issues/19043)
  - [OpenAPITools/openapi-generator#11478](https://github.com/OpenAPITools/openapi-generator/issues/11478)
- **Workarounds**:
  1. **Choose one or the other**: Use either prefixes/suffixes OR type mappings, but not both
  2. **Use schemaMappings**: Replace `importMappings` with `schemaMappings` where possible
  3. **Redundant configuration**: Define mappings for both original and prefixed types

Example affected configuration:
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

This limitation will be resolved when the upstream OpenAPI Generator bugs are fixed.

#### Debug Options

```bash
# Enable debug logging
./gradlew generatePets --info

# Get comprehensive help
./gradlew generateHelp

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
