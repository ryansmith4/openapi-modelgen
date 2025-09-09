---
layout: page
title: Examples Gallery
permalink: /examples/
---

# Examples Gallery

Real-world configuration examples and usage patterns for the OpenAPI Model Generator plugin.

## Basic Examples

### Simple Single-Spec Setup

The minimal configuration for a single OpenAPI specification:

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.1'
    id 'org.openapi.generator' version '7.14.0'
    id 'com.guidedbyte.openapi-modelgen' version '1.1.1'
}

openapiModelgen {
    specs {
        api {
            inputSpec "src/main/resources/openapi/api.yaml"
            modelPackage "com.example.model"
        }
    }
}

// Ensure compilation depends on generation
compileJava.dependsOn generateApi
```

### Multi-Specification Project

Generate models from multiple OpenAPI specifications:

```gradle
openapiModelgen {
    defaults {
        outputDir "build/generated/sources/openapi"
        modelNameSuffix "Dto"
        validateSpec true
    }
    
    specs {
        users {
            inputSpec "src/main/resources/openapi/users-v1.yaml"
            modelPackage "com.example.users.v1.model"
        }
        
        orders {
            inputSpec "src/main/resources/openapi/orders-v2.yaml"
            modelPackage "com.example.orders.v2.model"
            // Override default suffix for this spec
            modelNameSuffix "Entity"
        }
        
        external {
            inputSpec "src/main/resources/openapi/partner-api.yaml"
            modelPackage "com.example.external.partner.model"
            // Different validation for external API
            validateSpec false
        }
    }
}

// Generate all models before compilation
compileJava.dependsOn generateAllModels
```

## Enterprise Configuration

### Corporate Environment Setup

Configuration for enterprise environments with corporate dependency management:

```gradle
plugins {
    id 'com.company.java-standards'        // Corporate plugin manages OpenAPI Generator
    id 'com.guidedbyte.openapi-modelgen'   // Automatically detects corporate version
}

dependencies {
    // Corporate BOM manages all versions
    implementation platform('com.company:platform-bom:2024.1')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
}

openapiModelgen {
    defaults {
        outputDir "build/generated/sources/openapi"
        
        // Corporate template variables
        templateVariables([
            companyName: "Acme Corporation",
            legalNotice: "Confidential and Proprietary",
            supportContact: "api-support@company.com"
        ])
        
        // Corporate validation requirements
        validateSpec true
        generateModelTests false      // Corporate test framework handles this
        generateModelDocumentation true
        
        // Enterprise configuration
        configOptions([
            annotationLibrary: "swagger2",
            useSpringBoot3: "true",
            useBeanValidation: "true",
            hideGenerationTimestamp: "true",
            additionalModelTypeAnnotations: "@com.company.api.Generated;@lombok.Data;@lombok.experimental.SuperBuilder;@lombok.NoArgsConstructor(force = true)"
        ])
    }
    
    specs {
        customerApi {
            inputSpec "src/main/resources/openapi/customer-service-v3.yaml"
            modelPackage "com.company.customer.api.v3.model"
        }
        
        orderApi {
            inputSpec "src/main/resources/openapi/order-service-v2.yaml"  
            modelPackage "com.company.order.api.v2.model"
        }
    }
}
```

### Microservices Architecture

Configuration for microservices with shared schemas:

```gradle
// Service-specific build.gradle
plugins {
    id 'org.springframework.boot' version '3.2.1'
    id 'org.openapi.generator' version '7.14.0'
    id 'com.guidedbyte.openapi-modelgen' version '1.1.1'
}

dependencies {
    // Shared template library across microservices
    openapiCustomizations 'com.company:api-templates:2.1.0'
    
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}

openapiModelgen {
    defaults {
        // Template sources configuration
        templateSources([
            'user-templates',           // Service-specific overrides
            'user-customizations',      // Service-specific customizations
            'library-templates',        // Corporate template library
            'library-customizations',   // Corporate customizations  
            'plugin-customizations',    // Plugin defaults
            'openapi-generator'         // OpenAPI Generator defaults
        ])
        
        // Microservice-specific settings
        modelNameSuffix "Dto"
        outputDir "build/generated/sources/openapi"
        
        // Service mesh and tracing annotations
        configOptions([
            additionalModelTypeAnnotations: "@io.micrometer.tracing.annotation.NewSpan;@lombok.Data;@lombok.experimental.SuperBuilder;@lombok.NoArgsConstructor(force = true)"
        ])
    }
    
    specs {
        // Internal service API
        internal {
            inputSpec "src/main/resources/openapi/internal-api.yaml"
            modelPackage "com.company.userservice.internal.model"
        }
        
        // Public API  
        public {
            inputSpec "src/main/resources/openapi/public-api-v1.yaml"
            modelPackage "com.company.userservice.api.v1.model"
            // Stricter validation for public APIs
            validateSpec true
            generateModelDocumentation true
        }
    }
}

// Integration with Spring Boot
sourceSets {
    main {
        java {
            srcDirs += file("build/generated/sources/openapi/src/main/java")
        }
    }
}

// Ensure models are generated before tests
test.dependsOn generateAllModels
```

## Advanced Template Customization

### Comprehensive Template Customization

Full-featured template customization with YAML modifications:

```gradle
openapiModelgen {
    defaults {
        templateCustomizationsDir "src/main/resources/template-customizations"
        debugTemplateResolution true  // Show which templates are used
        
        templateVariables([
            companyName: "Tech Innovations Inc.",
            currentYear: "2025",
            copyright: "Copyright © {{currentYear}} {{companyName}}",
            customHeader: "{{copyright}}\\nGenerated with OpenAPI Model Generator",
            auditEnabled: "true",
            validationEnabled: "true"
        ])
    }
    
    specs {
        enhanced {
            inputSpec "src/main/resources/openapi/enhanced-api.yaml"
            modelPackage "com.example.enhanced.model"
        }
    }
}
```

**Template Customization Files:**

```yaml
# src/main/resources/template-customizations/spring/pojo.mustache.yaml
metadata:
  name: "Enhanced POJO Template"
  description: "Adds audit fields, custom validation, and enhanced documentation"
  version: "2.1.0"

insertions:
  # Custom file header
  - at: "start"
    content: |
      /*
       * {{customHeader}}
       * 
       * This file contains generated model classes for {{classname}}.
       * Do not modify this file directly.
       */
      
  # Enhanced imports after Jackson
  - after: "{{#jackson}}"
    content: |
      import com.fasterxml.jackson.annotation.JsonInclude;
      import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
      import com.fasterxml.jackson.annotation.JsonProperty;
    conditions:
      templateNotContains: "import com.fasterxml.jackson.annotation.JsonInclude;"
      
  # Audit imports
  - after: "{{#useBeanValidation}}"
    content: |
      import java.time.Instant;
      import com.company.audit.Auditable;
    conditions:
      templateContains: "{{auditEnabled}}"
      
  # Class-level annotations
  - after: "{{#additionalModelTypeAnnotations}}"
    content: |
      @JsonIgnoreProperties(ignoreUnknown = true)
      @JsonInclude(JsonInclude.Include.NON_NULL)
    conditions:
      templateContains: "{{#jackson}}"
      
  # Audit fields before existing fields
  - before: "{{#vars}}"
    content: |
      {{#auditEnabled}}
      /**
       * Audit fields automatically managed by the system
       */
      @JsonProperty("created_at")
      private Instant createdAt;
      
      @JsonProperty("updated_at") 
      private Instant updatedAt;
      
      @JsonProperty("version")
      private Long version = 1L;
      
      {{/auditEnabled}}
    conditions:
      templateContains: "{{auditEnabled}}"
      
  # Custom validation method
  - after: "{{#equals}}"
    content: |
      
      /**
       * Enhanced validation with business rules
       */
      public boolean isValid() {
          if (!this.validate()) {
              return false;
          }
          
          // Add custom business validation logic here
          return true;
      }
      
      /**
       * Basic field validation
       */
      private boolean validate() {
          {{#vars}}
          {{#required}}
          if ({{name}} == null) {
              return false;
          }
          {{/required}}
          {{/vars}}
          return true;
      }
      
      /**
       * Update audit timestamp
       */
      public void touch() {
          this.updatedAt = Instant.now();
          if (this.createdAt == null) {
              this.createdAt = Instant.now();
          }
          this.version = (this.version == null) ? 1L : this.version + 1L;
      }

replacements:
  # Enhanced toString method
  - pattern: "public String toString() {"
    replacement: |
      /**
       * Enhanced toString with null safety and formatting
       */
      @Override
      public String toString() {
```

```yaml
# src/main/resources/template-customizations/spring/model.mustache.yaml  
metadata:
  name: "Enhanced Model Wrapper"
  description: "Adds package documentation and import optimization"

insertions:
  # Package documentation
  - at: "start"
    content: |
      /**
       * Generated model classes for {{package}}.
       * 
       * {{copyright}}
       * 
       * These classes are generated from OpenAPI specification:
       * - Source: {{inputSpec}}
       * - Generator: {{generatorClass}}
       * - Version: {{appVersion}}
       * 
       * @author OpenAPI Model Generator
       * @version {{appVersion}}
       */
```

### Template Library Example

Creating and using a shared template library:

**Library Project Structure:**
```text
api-templates-lib/
├── build.gradle
└── src/main/resources/
    ├── META-INF/
    │   ├── openapi-library.yaml                    # Library metadata
    │   ├── openapi-templates/                      # Explicit templates
    │   │   └── spring/
    │   │       └── customValidator.mustache
    │   └── openapi-customizations/                 # YAML customizations
    │       └── spring/
    │           ├── pojo.mustache.yaml             # Enhanced POJO
    │           ├── enumClass.mustache.yaml        # Enhanced enums  
    │           └── model.mustache.yaml            # Enhanced model wrapper
```

**Library Metadata:**
```yaml
# src/main/resources/META-INF/openapi-library.yaml
name: "corporate-api-templates"
version: "2.1.0"
description: "Corporate standard templates with audit, validation, and documentation"
author: "Platform Engineering Team"
homepage: "https://github.com/company/api-templates"

supportedGenerators:
  - "spring"

minOpenApiGeneratorVersion: "7.11.0"
minPluginVersion: "1.2.0"

features:
  auditFields: true
  customValidation: true
  enhancedDocumentation: true
  lombokSupport: true
  springBootIntegration: "3.x"
  
dependencies:
  - "org.springframework:spring-core:6.0+"
  - "jakarta.validation:jakarta.validation-api:3.0+"
  - "org.projectlombok:lombok:1.18+"
```

**Library Build Configuration:**
```gradle
// api-templates-lib/build.gradle
plugins {
    id 'java-library'
    id 'maven-publish'
}

group = 'com.company'
version = '2.1.0'

publishing {
    publications {
        maven(MavenPublication) {
            from components.java
            
            pom {
                name = 'Corporate API Templates'
                description = 'Shared OpenAPI templates for corporate standards'
                url = 'https://github.com/company/api-templates'
            }
        }
    }
    
    repositories {
        maven {
            name = 'corporate'
            url = 'https://maven.company.com/releases'
        }
    }
}
```

**Using the Template Library:**
```gradle
// Consumer project build.gradle
plugins {
    id 'org.openapi.generator' version '7.14.0'
    id 'com.guidedbyte.openapi-modelgen' version '1.1.1'
}

dependencies {
    // Corporate template library
    openapiCustomizations 'com.company:corporate-api-templates:2.1.0'
    
    // Additional specialized libraries
    openapiCustomizations 'com.company:validation-templates:1.0.0'
}

openapiModelgen {
    defaults {
        // Template sources configuration
        templateSources([
            'user-templates',           // Project-specific templates (highest)
            'user-customizations',      // Project-specific YAML customizations
            'library-templates',        // Corporate template library explicit templates
            'library-customizations',   // Corporate template library YAML customizations
            'plugin-customizations',    // Plugin built-in customizations
            'openapi-generator'         // OpenAPI Generator defaults (lowest)
        ])
        
        debugTemplateResolution true   // Show which library provides each template
    }
    
    specs {
        api {
            inputSpec "src/main/resources/openapi/api.yaml"
            modelPackage "com.company.service.model"
        }
    }
}
```

## Testing and CI/CD Integration

### Gradle Build Integration

Complete build integration with testing:

```gradle
plugins {
    id 'java'
    id 'jacoco'  // Code coverage
    id 'org.springframework.boot' version '3.2.1'
    id 'org.openapi.generator' version '7.14.0'
    id 'com.guidedbyte.openapi-modelgen' version '1.1.1'
}

openapiModelgen {
    defaults {
        outputDir "build/generated/sources/openapi"
        validateSpec true  // Validate specs during build
    }
    
    specs {
        api {
            inputSpec "src/main/resources/openapi/api.yaml"
            modelPackage "com.example.model"
        }
    }
}

// Source set configuration for IDE integration
sourceSets {
    main {
        java {
            srcDirs += file("build/generated/sources/openapi/src/main/java")
        }
    }
    
    test {
        java {
            // Include generated models in test classpath
            srcDirs += file("build/generated/sources/openapi/src/main/java")
        }
    }
}

// Task dependencies
compileJava.dependsOn generateAllModels
compileTestJava.dependsOn generateAllModels
processResources.dependsOn generateAllModels

// Clean generated sources
clean {
    delete file("build/generated/sources/openapi")
}

// JaCoCo excludes for generated code
jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
    
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                'com/example/model/**',  // Exclude generated models from coverage
            ])
        }))
    }
}
```

### CI/CD Pipeline Configuration

**GitHub Actions Workflow:**
```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Cache Gradle dependencies
      uses: actions/cache@v4
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: gradle-${{ runner.os }}-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        
    - name: Validate OpenAPI specifications
      run: ./gradlew generateAllModels --validate-spec=true
      
    - name: Build with Gradle
      run: ./gradlew build
      
    - name: Run tests
      run: ./gradlew test
      
    - name: Generate coverage report
      run: ./gradlew jacocoTestReport
      
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v4
      with:
        file: ./build/reports/jacoco/test/jacocoTestReport.xml
```

**Jenkins Pipeline:**
```groovy
// Jenkinsfile
pipeline {
    agent any
    
    tools {
        jdk 'JDK17'
    }
    
    environment {
        GRADLE_OPTS = '-Xmx2g -Dfile.encoding=UTF-8'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Validate OpenAPI Specs') {
            steps {
                sh './gradlew generateAllModels --validate-spec=true'
            }
        }
        
        stage('Build') {
            steps {
                sh './gradlew clean build --configuration-cache'
            }
        }
        
        stage('Test') {
            steps {
                sh './gradlew test jacocoTestReport'
            }
            
            post {
                always {
                    publishTestResults testResultsPattern: 'build/test-results/**/*.xml'
                    publishCoverage adapters: [jacocoAdapter('build/reports/jacoco/test/jacocoTestReport.xml')], sourceFileResolver: sourceFiles('STORE_LAST_BUILD')
                }
            }
        }
        
        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}
```

## Performance Optimization Examples

### High-Performance Configuration

Optimized configuration for large projects with many specifications:

```gradle
openapiModelgen {
    defaults {
        // Enable all performance features
        parallel true                    // Parallel multi-spec processing
        
        // Optimized template resolution order
        templateSources([
            'user-templates',            // Check user overrides first
            'user-customizations',       // Then user customizations
            'library-templates',         // Library templates
            'library-customizations',    // Library customizations
            'plugin-customizations',     // Plugin optimizations
            'openapi-generator'          // Generator defaults last
        ])
        
        // Minimal validation for performance (enable in CI)
        validateSpec false
        
        // Skip unnecessary generation
        generateModelTests false
        generateApiTests false
        generateApiDocumentation false
        generateModelDocumentation false
        
        // Performance-optimized config options
        configOptions([
            hideGenerationTimestamp: "true",    // Consistent output for caching
            skipDefaultInterface: "true",       // Reduce generated code size
            skipFormModel: "true"               // Skip form models if not needed
        ])
    }
    
    specs {
        // Define all specs for parallel processing
        users { 
            inputSpec "specs/users.yaml"
            modelPackage "com.example.users.model" 
        }
        orders { 
            inputSpec "specs/orders.yaml"
            modelPackage "com.example.orders.model" 
        }
        products { 
            inputSpec "specs/products.yaml"
            modelPackage "com.example.products.model" 
        }
        payments { 
            inputSpec "specs/payments.yaml"
            modelPackage "com.example.payments.model" 
        }
    }
}

// Gradle performance optimizations
tasks.withType(JavaCompile) {
    options.incremental = true
    options.fork = true
    options.forkOptions.jvmArgs << '-Xmx2g'
}

// Enable configuration cache for fastest builds
tasks.register('generateAllModelsWithCache') {
    dependsOn 'generateAllModels'
    doFirst {
        println "Using configuration cache for optimal performance"
    }
}
```

**gradle.properties for Performance:**
```properties
# Gradle performance settings
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.configureondemand=true
org.gradle.caching=true
org.gradle.configuration-cache=true

# JVM performance settings  
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseG1GC

# Plugin-specific performance
openapi.modelgen.cache.enabled=true
openapi.modelgen.parallel.threads=4
```

### Build Script Optimization

Optimized build scripts for different project types:

```gradle
// Multi-module project optimization
subprojects {
    apply plugin: 'java'
    apply plugin: 'org.openapi.generator'
    apply plugin: 'com.guidedbyte.openapi-modelgen'
    
    openapiModelgen {
        defaults {
            // Shared configuration across all modules
            outputDir "build/generated/sources/openapi"
            modelNameSuffix "Dto"
            parallel true
            validateSpec project.hasProperty('ci')  // Only validate in CI
        }
    }
    
    // Module-specific generation
    if (project.name == 'user-service') {
        openapiModelgen {
            specs {
                userApi {
                    inputSpec "src/main/resources/openapi/user-api.yaml"
                    modelPackage "com.company.user.model"
                }
            }
        }
    }
    
    // Common dependency optimization
    compileJava.dependsOn generateAllModels
}
```

This examples gallery demonstrates real-world usage patterns from simple single-spec setups to complex enterprise configurations with template libraries, CI/CD integration, and performance optimizations.