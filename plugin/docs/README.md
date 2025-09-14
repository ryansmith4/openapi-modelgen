---
layout: page
title: Plugin Overview
---

# OpenAPI Model Generator Plugin

A comprehensive Gradle plugin for generating Java DTOs from multiple OpenAPI specifications with enhanced features:

Features:
• Multi-spec support with individual task generation
• Lombok annotation integration (@Data, @Builder, @SuperBuilder, etc.)
• Custom Mustache template support with precedence resolution
• Template variable expansion (nested variables like {{currentYear}} in {{copyright}})
• Incremental build support for optimal performance
• Configuration validation with detailed error reporting
• Parallel template processing for large template sets
• Content-based template change detection using SHA-256 hashing

Usage Examples:

Basic Configuration:

```groovy
plugins {
    id 'com.guidedbyte.openapi-modelgen'
}

openapiModelgen {
    defaults {
        outputDir "build/generated-sources/openapi"
        modelNameSuffix "Dto"
        generateModelTests false
        validateSpec true
    }
    
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

Advanced Configuration:

```groovy
openapiModelgen {
    defaults {
        outputDir "build/generated-sources/openapi"
        userTemplateDir "src/main/resources/openapi-templates"
        configOptions([
            dateLibrary: "java8",
            serializationLibrary: "jackson",
            useBeanValidation: "true",
            hideGenerationTimestamp: "true"
        ])
        templateVariables([
            copyright: "Copyright © {{currentYear}} {{companyName}}",
            currentYear: "2025", 
            companyName: "My Company Inc."
        ])
        globalProperties([
            skipFormModel: "false",
            generateAliasAsModel: "true"
        ])
    }
    
    specs {
        pets {
            inputSpec "specs/pets-v1.yaml"
            modelPackage "com.example.pets.v1.model"
            configOptions([
                additionalModelTypeAnnotations: "@lombok.Data;@lombok.experimental.SuperBuilder"
            ])
        }
    }
}
```

Command Line Options:
All configuration options can be overridden via command line:
• --model-package=com.example.model
• --output-dir=build/custom-output
• --template-dir=custom-templates
• --model-name-suffix=Entity
• --validate-spec
• --generate-model-tests
• --generate-api-docs

Task Generation:
• generateOpenApiDtosForPets - Generate DTOs for pets specification
• generateOpenApiDtosForOrders - Generate DTOs for orders specification  
• generateOpenApiDtosAll - Generate DTOs for all specifications

Dependencies:
The plugin automatically detects and works with any OpenAPI Generator version provided by your configuration
management (corporate plugins, etc.). If no version is found, it falls back to the tested default version 7.14.0.
The plugin also automatically adds required dependencies including Lombok, Jackson, Spring Boot validation,
and JSR-305 annotations.

Template Customization:
Place custom .mustache templates in your template directory to override plugin defaults. Template resolution
follows precedence: user templates > plugin templates > OpenAPI generator defaults.

For detailed documentation visit: [GitHub Repository](https://github.com/ryansmith4/openapi-modelgen)

## Installation

Add to your build.gradle:

```groovy
plugins {
    id 'com.guidedbyte.openapi-modelgen' version '1.0.0'
}
```
