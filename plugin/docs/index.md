---
layout: page
title: OpenAPI Model Generator Plugin Documentation
---

A comprehensive Gradle plugin for generating Java DTOs from multiple OpenAPI specifications with
enterprise-grade features including template customization, incremental builds, and performance optimizations.

## Quick Start

```gradle
plugins {
    id 'com.guidedbyte.openapi-modelgen' version '1.1.1'
}

openapiModelgen {
    defaults {
        outputDir "build/generated/sources/openapi"
        modelNameSuffix "Dto"
    }
    specs {
        pets {
            inputSpec "src/main/resources/openapi/pets.yaml"
            modelPackage "com.example.pets.model"
        }
    }
}
```

## Key Features

- **Multi-spec Support**: Generate models from multiple OpenAPI specifications
- **Template Customization**: YAML-based template modifications without full overrides
- **Incremental Builds**: Only regenerates when inputs change
- **Lombok Integration**: Full annotation support with conflict resolution
- **Performance Optimized**: Template caching and parallel processing
- **Enterprise Ready**: Comprehensive validation and error reporting

## Documentation

### Core Documentation

- **[Plugin Overview](README.html)** - Basic usage and configuration examples
- **[Template Schema Reference](template-schema.html)** - Complete YAML customization schema

### Advanced Topics

- **Template Precedence System** - Understanding template resolution hierarchy
- **Performance Features** - Incremental builds and optimization strategies
- **Enterprise Integration** - Corporate environment and CI/CD integration

## Template Customization

The plugin's template customization system allows surgical modifications to OpenAPI Generator templates:

```yaml
# model.mustache.yaml
metadata:
  name: "Enhanced Model Template"
  description: "Adds Jackson imports and custom annotations"

insertions:
  - after: "{{#jackson}}"
    content: |
      import com.fasterxml.jackson.annotation.JsonInclude;
    conditions:
      templateNotContains: "import com.fasterxml.jackson.annotation.JsonInclude;"
```

**📘 [Complete Schema Reference](template-schema.html)** - Detailed documentation with examples and validation rules.

## Getting Help

- **GitHub Issues**: [Report bugs and request features](https://github.com/guidedbyte/openapi-modelgen/issues)
- **Documentation**: Comprehensive guides and API reference
- **Community**: Join discussions and get support

## Version Compatibility

The plugin works with OpenAPI Generator versions 7.10.0+ and automatically detects the version in your
environment. No hard dependency on specific OpenAPI Generator versions.

---

**Last updated:** January 2025
