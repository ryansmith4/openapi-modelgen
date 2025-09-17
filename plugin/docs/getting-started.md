---
layout: page
title: Getting Started
permalink: /getting-started/
---

# Getting Started with OpenAPI Model Generator

This guide will help you set up and use the OpenAPI Model Generator plugin in your project.

## Prerequisites

- **Java 17+** (Java 21 recommended for best performance)
- **Gradle 8.0+** (Gradle 8.5+ recommended)
- An **OpenAPI 3.x specification** file (YAML or JSON)

## Step 1: Apply the Plugin

Add both the OpenAPI Generator plugin and this plugin to your `build.gradle`:

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.1'
    id 'org.openapi.generator' version '7.14.0'    // Must be applied first
    id 'com.guidedbyte.openapi-modelgen' version '@version@'  // Apply second
}
```

**Important**: The OpenAPI Generator plugin must be applied **before** this plugin.

## Step 2: Add Required Dependencies

```gradle
dependencies {
    // Spring Boot dependencies
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // OpenAPI/Swagger annotations
    implementation 'io.swagger.core.v3:swagger-annotations:2.2.19'
    
    // Lombok (optional but recommended)
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

## Step 3: Create Your OpenAPI Specification

Create a directory structure and add your OpenAPI specification:

```text
src/
└── main/
    └── resources/
        └── openapi/
            └── api.yaml
```

Example minimal specification (`src/main/resources/openapi/api.yaml`):

```yaml
openapi: 3.0.3
info:
  title: My API
  version: 1.0.0
paths: {}  # Paths not needed for model generation
components:
  schemas:
    User:
      type: object
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
        email:
          type: string
          format: email
      required:
        - name
        - email
    
    CreateUserRequest:
      type: object
      properties:
        name:
          type: string
        email:
          type: string
          format: email
      required:
        - name
        - email
```

## Step 4: Configure the Plugin

Add the minimal configuration to your `build.gradle`:

```gradle
openapiModelgen {
    specs {
        myApi {
            inputSpec "src/main/resources/openapi/api.yaml"
            modelPackage "com.example.model"
        }
    }
}
```

**Note**: Use method call syntax (no equals signs) for all configuration.

## Step 5: Generate Models

Run the generation task:

```bash
./gradlew generateMyApi
```

Or generate all configured specifications:

```bash
./gradlew generateAllModels
```

## Step 6: View Generated Code

The generated models will be in:

```text
build/
└── generated/
    └── sources/
        └── openapi/
            └── src/
                └── main/
                    └── java/
                        └── com/
                            └── example/
                                └── model/
                                    ├── UserDto.java
                                    └── CreateUserRequestDto.java
```

Example generated code:

```java
package com.example.model;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.*;

@Data
@Accessors(fluent = true)
@SuperBuilder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class UserDto implements Serializable {
    
    private Long id;
    
    @NotNull
    private String name;
    
    @NotNull
    @Email
    private String email;
}
```

## Step 7: Use Generated Models

Add the generated source directory to your IDE and use the models:

```java
@RestController
public class UserController {
    
    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        UserDto user = UserDto.builder()
            .name(request.name())
            .email(request.email())
            .build();
            
        // Save user logic here
        
        return ResponseEntity.ok(user);
    }
}
```

## Common Configuration Options

### Multiple Specifications

```gradle
openapiModelgen {
    specs {
        users {
            inputSpec "src/main/resources/openapi/users.yaml"
            modelPackage "com.example.users.model"
        }
        orders {
            inputSpec "src/main/resources/openapi/orders.yaml"
            modelPackage "com.example.orders.model"
        }
    }
}
```

### Custom Output Directory

```gradle
openapiModelgen {
    defaults {
        outputDir "src/generated/java"
    }
    specs {
        myApi {
            inputSpec "src/main/resources/openapi/api.yaml"
            modelPackage "com.example.model"
        }
    }
}
```

### Enable Validation

```gradle
openapiModelgen {
    defaults {
        validateSpec true  // Validate OpenAPI spec before generation
    }
    specs {
        myApi {
            inputSpec "src/main/resources/openapi/api.yaml"
            modelPackage "com.example.model"
        }
    }
}
```

## Performance Features

The plugin includes several performance optimizations enabled by default:

- **Multi-level caching**: Speeds up repeat builds by 90%
- **Incremental builds**: Only regenerates when specifications change
- **Parallel processing**: Generates multiple specs concurrently

For configuration cache compatibility (faster builds):

```bash
./gradlew generateAllModels --configuration-cache
```

## Next Steps

- **Template Customization**: See [Template Customization Guide](template-customization-guide.md)
- **Full Configuration**: See [Configuration Reference](configuration-reference.md)
- **Advanced Examples**: See [Examples Gallery](examples.md)
- **Issues?**: See [Troubleshooting Guide](troubleshooting.md)

## IDE Integration

### IntelliJ IDEA

1. **Mark generated directory as source**:
   - Right-click `build/generated/sources/openapi/src/main/java`
   - Select "Mark Directory as" → "Generated Sources Root"

1. **Auto-import**: IntelliJ will automatically detect the generated classes

### VS Code

1. **Add to Java source paths** in `.vscode/settings.json`:

```json
{
    "java.compile.sourcePaths": [
        "src/main/java",
        "build/generated/sources/openapi/src/main/java"
    ]
}
```

## Gradle Integration

### Build Dependencies

Make compilation depend on model generation:

```gradle
compileJava.dependsOn 'generateAllModels'
```

### Clean Task

Clean generated sources:

```gradle
clean {
    delete 'build/generated'
}
```

This completes the basic setup! Your project now generates type-safe Java DTOs from OpenAPI specifications with Lombok support and enterprise-grade performance optimizations.