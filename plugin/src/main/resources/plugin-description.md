A comprehensive Gradle plugin for generating Java DTOs from OpenAPI specifications with Lombok support, incremental builds, and template customization.

Key Features:
• Multi-spec support with individual task generation per specification
• Full Lombok integration with automatic annotation support
• Template precedence system (user > plugin > generator defaults)
• Incremental build support for optimal performance
• Dynamic template discovery for forward/backward compatibility
• Configuration validation with detailed error reporting
• Command-line parameter overrides for all options

Requirements:
• Java 17+
• Gradle 8.0+  
• OpenAPI Generator 7.10.0+ (automatically managed)

Basic Usage:
openapiModelgen {
    specs {
        myApi {
            inputSpec "src/main/resources/api.yaml"
            modelPackage "com.example.model"
        }
    }
}

Generated Tasks:
• generateMyApi - Generate models for specific spec
• generateAllModels - Generate models for all specs
• generateHelp - Show plugin help and configuration options

The plugin automatically detects OpenAPI Generator versions and adds required dependencies. Supports template customization and works with corporate dependency management.

For detailed documentation and examples visit: https://github.com/ryansmith4/openapi-modelgen