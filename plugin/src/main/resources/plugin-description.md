# OpenAPI Model Generator Plugin

A comprehensive Gradle plugin for generating Java DTOs from OpenAPI specifications with Lombok support,
YAML-based template customization, multi-level caching system, and enterprise-grade performance optimizations.

Key Features:
• Multi-spec support with individual task generation per specification
• Thread-safe parallel processing with configurable parallel execution control
• Multi-level caching system: session → local → global with 90% faster no-change builds and 70% faster incremental builds
• Cross-build performance optimization with global cache persistence
• Full Lombok integration with automatic annotation support  
• YAML-based template customization engine with insertions, replacements, and conditions
• Clean template discovery using OpenAPI Generator's official CodegenConfig API
• Dynamic generator support - works with any generator from configuration (spring, java, etc.)
• Template precedence hierarchy: User templates > User YAML > Plugin YAML > Generator defaults
• Partial template override approach - customize only what you need
• Incremental build support with selective template processing
• Generator directory organization with enforced subdirectory structure
• Configuration validation with detailed error reporting
• Command-line parameter overrides for all options

Requirements:
• Java 17+
• Gradle 8.0+  
• OpenAPI Generator 7.10.0+ (must be applied by consumer project)

Basic Usage:
plugins {
    id 'org.openapi.generator' version '7.11.0'  // Or your preferred version 7.10.0+
    id 'com.guidedbyte.openapi-modelgen'
}

openapiModelgen {
    defaults {
        parallel true  // Enable thread-safe parallel processing (default: true)
    }
    specs {
        myApi {
            inputSpec "src/main/resources/api.yaml"
            modelPackage "com.example.model"
            // Optional: explicit templates and YAML customizations
            templateDir "src/main/resources/templates"
            templateCustomizationsDir "src/main/resources/customizations"
        }
    }
}

Generated Tasks:
• generateMyApi - Generate models for specific spec
• generateAllModels - Generate models for all specs
• generateHelp - Show plugin help and configuration options

The plugin includes comprehensive caching with global persistence across builds and thread-safe parallel processing.
It provides YAML customizations to enhance code readability while using OpenAPI Generator's official APIs for clean
template access. It automatically detects and works with whatever OpenAPI Generator version you provide (7.10.0+)
and integrates seamlessly with corporate dependency management.

For detailed documentation and examples visit: <https://github.com/guidedbyte/openapi-modelgen>

For comprehensive template system documentation see: <https://github.com/guidedbyte/openapi-modelgen/blob/main/plugin/docs/template-system.md>