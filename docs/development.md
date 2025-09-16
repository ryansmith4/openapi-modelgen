# Development Guide

This guide covers building, testing, and contributing to the OpenAPI Model Generator plugin.

## Table of Contents

- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Building](#building)
- [Testing](#testing)
- [Development Workflow](#development-workflow)
- [Contributing](#contributing)
- [Versioning and Releases](#versioning-and-releases)
- [Technical Architecture](#technical-architecture)
- [Code Style and Standards](#code-style-and-standards)

## Project Structure

```text
openapi-modelgen/
├── gradle/
│   └── libs.versions.toml           # Version catalog (shared)
├── plugin/                          # Main plugin implementation
│   ├── gradle/
│   │   └── libs.versions.toml       # Version catalog (plugin copy)
│   ├── src/main/java/com/guidedbyte/openapi/modelgen/
│   │   ├── OpenApiModelGenPlugin.java        # Core plugin with advanced optimizations
│   │   ├── OpenApiModelGenExtension.java     # DSL extension with multi-spec support
│   │   ├── DefaultConfig.java                # Global defaults configuration
│   │   ├── SpecConfig.java                   # Individual spec configuration
│   │   ├── actions/                          # Task action implementations
│   │   │   ├── GenerateModelsAction.java     # Core generation logic
│   │   │   └── TemplatePreparationAction.java # Template orchestration
│   │   └── services/                         # Core services
│   │       ├── ConfigurationValidator.java   # Configuration validation
│   │       ├── LibraryTemplateExtractor.java # Template library support
│   │       ├── TaskConfigurationService.java # Task configuration
│   │       └── TemplateCacheManager.java     # Performance optimization
│   ├── src/main/resources/
│   │   ├── templateCustomizations/           # Built-in YAML customizations
│   │   │   └── spring/                      # Generator-specific customizations
│   │   └── plugin-description.md            # External plugin documentation
│   └── src/test/java/com/guidedbyte/openapi/modelgen/  # Comprehensive test suite
│       ├── PluginFunctionalTest.java         # Unit tests with ProjectBuilder
│       ├── WorkingIntegrationTest.java       # TestKit integration tests
│       ├── TemplatePrecedenceUnitTest.java   # Template precedence testing
│       ├── TemplatePrecedenceTest.java       # Template precedence integration tests
│       ├── LiveTemplatePrecedenceTest.java   # Live template testing
│       ├── services/                         # Service-specific tests
│       └── TestSummary.md                    # Test coverage documentation
└── test-app/                        # Test application using the plugin
    ├── build.gradle                 # Plugin configuration example
    └── src/main/resources/openapi-spec/  # OpenAPI specifications
        ├── pets.yaml                # Pet store API specification
        └── orders.yaml              # Orders API specification
```

## Requirements

### Development Environment

- **Java 17+** (for development and runtime)
- **Gradle 8.0+** (wrapper included)
- **Git** (for version control)

### Runtime Dependencies

- **OpenAPI Generator 7.10.0+** (provided by consumer projects)
- **Gradle 8.0+** (for plugin execution)

## Building

### Build Commands

**Build the plugin**:
```bash
./gradlew plugin:build
```

**Clean build**:
```bash
./gradlew clean build
```

**Build with configuration cache**:
```bash
./gradlew plugin:build --configuration-cache
```

### Composite Build Structure

The project uses a composite build with the plugin in a separate directory:

**settings.gradle**:
```gradle
includeBuild 'plugin'  // Composite build inclusion
```

**Important**: Use `includeBuild`, not `include 'plugin'` for composite builds.

### Version Catalogs

The project uses Gradle version catalogs for centralized dependency management:

**gradle/libs.versions.toml** (shared across composite build):
```toml
[versions]
# Security fixes - DO NOT downgrade
snakeyaml = "2.3"                    # Fixes CVE-2022-1471 (CRITICAL)
commons-lang3 = "3.18.0"             # Fixes CVE-2025-48924 (MEDIUM)

openapi-generator = "7.14.0"
gradle-plugin-publish = "1.3.0"
lombok = "1.18.36"

[libraries]
# Plugin core dependencies
plugin-core = { group = "org.apache.commons", name = "commons-lang3", version.ref = "commons-lang3" }
snakeyaml = { group = "org.yaml", name = "snakeyaml", version.ref = "snakeyaml" }

[bundles]
plugin-core = ["plugin-core", "snakeyaml"]
plugin-test = ["junit-jupiter", "assertj-core", "mockito-core"]
```

### Security-Critical Dependencies

**Version Catalogs** track security-critical dependencies:
```toml
[versions]
# Security fixes - DO NOT downgrade
snakeyaml = "2.3"                    # Fixes CVE-2022-1471 (CRITICAL)
commons-lang3 = "3.18.0"             # Fixes CVE-2025-48924 (MEDIUM)
```

## Testing

### Test Suite Overview

The plugin includes comprehensive testing with **247+ test methods across 27 test classes**:

- **Unit tests**: Core functionality with ProjectBuilder
- **Integration tests**: Real Gradle environment with TestKit
- **Template precedence tests**: Comprehensive template resolution testing
- **Performance tests**: Incremental builds and caching validation
- **Configuration validation tests**: Error handling and validation

### Running Tests

**Run all tests**:
```bash
./gradlew plugin:test
```

**Run with configuration cache**:
```bash
./gradlew plugin:test --configuration-cache
```

**Run specific test class**:
```bash
./gradlew plugin:test --tests "PluginFunctionalTest"
```

**Run with debug output**:
```bash
./gradlew plugin:test --info
```

### Test Categories

#### Unit Tests

**ProjectBuilder-based tests** for fast feedback:
```java
@Test
void testPluginAppliesCorrectly() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply(OpenApiModelGenPlugin.class);

    assertThat(project.getExtensions().findByType(OpenApiModelGenExtension.class))
        .isNotNull();
}
```

#### Integration Tests

**TestKit-based tests** for real Gradle environment:
```java
@Test
void testGenerateTaskCreation() {
    BuildResult result = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments("tasks", "--all")
        .withPluginClasspath()
        .build();

    assertThat(result.getOutput()).contains("generatePets");
}
```

#### Template Precedence Tests

**Comprehensive template resolution testing**:
```java
@Test
void testUserTemplateOverridesCustomizations() {
    // Setup user template and YAML customization
    // Verify user template takes precedence
}
```

### Test Environment

**Test application** (`test-app/`) provides real-world testing:

```bash
# Test with sample application
cd test-app

# Generate models
./gradlew generatePets

# Generate all models
./gradlew generateAllModels

# Build with generated code
./gradlew build
```

### Test Configuration

**Plugin test configuration**:
```gradle
// plugin/build.gradle
test {
    useJUnitPlatform()

    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }

    systemProperty 'test.gradle.version', gradle.gradleVersion
}
```

## Development Workflow

### Local Development

1. **Make changes** to plugin source code
2. **Test changes**:
   ```bash
   ./gradlew plugin:test
   ```
3. **Test with sample app**:
   ```bash
   cd test-app
   ./gradlew generatePets
   ```
4. **Verify integration**:
   ```bash
   cd test-app
   ./gradlew build
   ```

### Configuration Cache Compatibility

⚠️ **CRITICAL**: Plugin MUST remain configuration cache compatible.

**Requirements**:
1. **No `Project` in Actions**: Use `ProjectLayout`, `DirectoryProperty` instead
2. **Static Logging Only**: `LoggerFactory.getLogger(Class)`, never `project.getLogger()`
3. **Config vs Execution Separation**: All discovery at config time, serializable objects only
4. **Test with `--configuration-cache`**: All changes must pass cache tests

**Testing**:
```bash
./gradlew plugin:test --configuration-cache
./gradlew plugin:build --configuration-cache
cd test-app && ./gradlew generatePets --configuration-cache
```

### Debugging

**Debug plugin during test**:
```bash
./gradlew plugin:test --debug-jvm
```

**Debug test application**:
```bash
cd test-app
./gradlew generatePets --debug
```

**View template processing**:
```bash
cd test-app
./gradlew generatePets --info | grep -i template
```

## Contributing

### Before Contributing

1. **Read this development guide**
2. **Check existing issues**: [GitHub Issues](https://github.com/guidedbyte/openapi-modelgen/issues)
3. **Run full test suite**:
   ```bash
   ./gradlew plugin:test
   cd test-app && ./gradlew build
   ```

### Pull Request Process

1. **Create feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make changes** with tests
3. **Verify tests pass**:
   ```bash
   ./gradlew plugin:test --configuration-cache
   ```

4. **Test with sample app**:
   ```bash
   cd test-app
   ./gradlew clean generateAllModels build
   ```

5. **Update documentation** if needed
6. **Create pull request** with:
   - Clear description
   - Test coverage
   - Documentation updates
   - Breaking change notes

### Code Review Requirements

- **All tests must pass**
- **Configuration cache compatibility maintained**
- **No security vulnerabilities introduced**
- **Documentation updated for new features**
- **Backward compatibility preserved** (or migration path provided)

## Versioning and Releases

### Version Management

The plugin uses **semantic versioning** with automated git-based version detection:

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

### Publishing

**Gradle Plugin Portal**:
```bash
./gradlew publishPlugins
```

**Local testing**:
```bash
./gradlew publishToMavenLocal
```

## Technical Architecture

### Core Components

#### Plugin Class (`OpenApiModelGenPlugin`)

**Responsibilities**:
- Plugin registration and lifecycle
- Task creation and configuration
- Extension registration
- Dependency management integration

#### Extension Class (`OpenApiModelGenExtension`)

**Responsibilities**:
- DSL configuration interface
- Defaults and spec configuration
- Configuration validation
- CLI parameter support

#### Action Classes

**`GenerateModelsAction`**:
- Core generation logic
- OpenAPI Generator integration
- Output management

**`TemplatePreparationAction`**:
- Template orchestration
- Multi-level caching
- Template precedence resolution

#### Service Classes

**`ConfigurationValidator`**:
- Configuration validation
- Error reporting
- Best practice enforcement

**`LibraryTemplateExtractor`**:
- Template library support
- JAR resource extraction
- Metadata processing

**`TaskConfigurationService`**:
- Task configuration management
- CLI parameter integration
- Configuration merging

**`TemplateCacheManager`**:
- Multi-level caching system
- Performance optimization
- Cache invalidation

### Key Technical Features

#### Configuration Cache Compatibility

**Serializable Configuration**:
```java
public class SpecConfig implements Serializable {
    private String inputSpec;
    private String modelPackage;
    // All fields must be serializable
}
```

**No Project References in Actions**:
```java
public abstract class GenerateModelsAction implements WorkAction<GenerateModelsParameters> {
    // Use parameters, not direct Project references
    @Override
    public void execute() {
        String outputDir = getParameters().getOutputDir().get();
        // Implementation using parameters only
    }
}
```

#### Template Resolution System

**Precedence Hierarchy**:
1. **Explicit User Templates** (highest precedence)
2. **User YAML Customizations**
3. **Library Templates**
4. **Library YAML Customizations**
5. **Plugin YAML Customizations**
6. **OpenAPI Generator Defaults** (lowest precedence)

**Template Discovery**:
```java
// Recursive template dependency discovery
Pattern includePattern = Pattern.compile("\\{\\{>\\s*(\\w+)\\s*\\}\\}");
Matcher matcher = includePattern.matcher(templateContent);
while (matcher.find()) {
    String dependentTemplate = matcher.group(1);
    extractTemplateDependency(dependentTemplate);
}
```

#### Multi-Level Caching

**Session Cache**: ConcurrentHashMap for build session
**Working Directory Cache**: SHA-256 content validation
**Global Cache**: Cross-build persistence in `~/.gradle/caches/`

### Performance Optimizations

- **Lazy evaluation**: Template extraction deferred until execution
- **Selective processing**: Only processes templates requiring customization
- **Parallel processing**: Thread-safe multi-spec generation
- **Content-based invalidation**: SHA-256 hashing for change detection

## Code Style and Standards

### Java Code Style

**Follow standard Java conventions**:
- Package naming: `com.guidedbyte.openapi.modelgen`
- Class naming: PascalCase
- Method naming: camelCase
- Constants: UPPER_SNAKE_CASE

**Example**:
```java
public class TemplatePreparationAction implements WorkAction<TemplatePreparationParameters> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplatePreparationAction.class);

    @Override
    public void execute() {
        // Implementation
    }
}
```

### Gradle Script Conventions

**Method-call DSL**:
```gradle
// Use method calls, not assignment
openapiModelgen {
    defaults {
        outputDir "build/generated"  // Method call
        // outputDir = "build/generated"  // Don't use assignment
    }
}
```

### Testing Standards

**Test naming**:
```java
@Test
void testConfigurationValidationWithMissingInputSpec() {
    // Test implementation
}
```

**Assertions with AssertJ**:
```java
assertThat(result.getOutput())
    .contains("generatePets")
    .doesNotContain("error");
```

### Documentation Standards

**Javadoc for public APIs**:
```java
/**
 * Validates the plugin configuration and reports errors.
 *
 * @param extension the plugin extension containing configuration
 * @throws InvalidUserDataException if configuration is invalid
 */
public void validateConfiguration(OpenApiModelGenExtension extension) {
    // Implementation
}
```

**README and docs**:
- Use clear examples
- Include troubleshooting sections
- Provide migration guides for breaking changes

### Security Considerations

**Dependency Security**:
- Track security-critical dependencies in version catalogs
- Document CVE fixes and version requirements
- Never downgrade security-critical versions

**Template Security**:
- Validate template content
- Sanitize user inputs
- Prevent path traversal attacks

**Build Security**:
- No secrets in configuration
- Validate all external inputs
- Use secure defaults

By following these development guidelines, you can contribute effectively to the OpenAPI Model Generator plugin while maintaining high code quality and security standards.