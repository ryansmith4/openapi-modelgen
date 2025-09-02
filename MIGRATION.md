# Migration Guide

This document provides guidance for migrating between different versions of the OpenAPI Model Generator plugin.

## Version Compatibility Matrix

| Plugin Version | Minimum Gradle | Minimum Java | OpenAPI Generator | Notes |
|----------------|----------------|--------------|-------------------|-------|
| 1.0.x          | 8.0           | 11           | 7.14.0+          | Initial stable release |
| 1.1.x          | 8.0           | 11           | 7.14.0+          | Enhanced features (planned) |
| 2.0.x          | 8.5           | 17           | 8.0.0+           | Breaking changes (planned) |

## Breaking Changes by Version

### Version 2.0.0 (Planned)
**Breaking Changes:**
- Minimum Java version increased to 17
- Minimum Gradle version increased to 8.5
- DSL method signatures may change for enhanced type safety
- Generated code structure optimizations
- Template variable format standardization

**Migration Steps:**
1. Update Java version to 17+
2. Update Gradle wrapper to 8.5+
3. Review and update any custom templates
4. Test generation with new structure
5. Update CI/CD pipelines

### Version 1.1.0 (Planned)
**New Features (Backward Compatible):**
- Enhanced CLI options
- Additional template variables
- Performance improvements
- New configuration validation

**Migration Steps:**
- No breaking changes - drop-in replacement
- Review new configuration options
- Update documentation references

## Version Detection

The plugin automatically detects its version from:

1. **Gradle property**: `./gradlew build -Pversion=1.2.3`
2. **gradle.properties**: Uncomment `version=1.2.3` 
3. **Git tags**: Automatic detection from `git describe --tags`
4. **Fallback**: `1.0.0-SNAPSHOT` for development

## Incremental Versioning Strategies

### Strategy 1: Git Tag-Based (Recommended)
```bash
# Create release
git tag -a v1.1.0 -m "Release version 1.1.0"
git push origin v1.1.0

# Build automatically uses tag version
./gradlew build
./gradlew publishPlugins
```

### Strategy 2: Manual gradle.properties
```properties
# gradle.properties
version=1.1.0
```

### Strategy 3: Command Line Override
```bash
# Build specific version
./gradlew build -Pversion=1.1.0-RC1
./gradlew publishPlugins -Pversion=1.1.0
```

## Release Workflow

### Development Cycle
1. **Feature Development**: Work on `1.0.0-SNAPSHOT`
2. **Pre-release Testing**: `1.1.0-RC1`, `1.1.0-RC2`
3. **Release**: Create `v1.1.0` tag
4. **Hotfix**: `1.1.1` for critical fixes

### Release Commands
```bash
# Validate before release
./gradlew validatePlugin

# Create release tag and validate
./gradlew createRelease

# Push tag and publish
git push origin v1.1.0
./gradlew publishPlugins
```

## Configuration Migration

### Checking Compatibility
```bash
# Show current version
./gradlew showVersion

# Validate version format
./gradlew validateVersion

# Get help for configuration options
./gradlew openapiModelgenHelp
```

### DSL Compatibility
The plugin maintains backward compatibility for DSL syntax:

```gradle
// Stable syntax (all versions)
openapiModelgen {
    specs {
        pets {
            inputSpec "src/main/resources/openapi-spec/pets.yaml"
            modelPackage "com.example.model.pets"
        }
    }
}
```

## Troubleshooting Version Issues

### Common Problems

**"Invalid version format" Error:**
- Ensure version follows semantic versioning: `1.2.3` or `1.2.3-SNAPSHOT`
- Check git tag format: should be `v1.2.3` not `1.2.3`

**"Cannot publish development version" Error:**
- Remove `-SNAPSHOT` or `-dirty` suffixes before publishing
- Commit any uncommitted changes before release

**Git versioning not working:**
- Ensure git is available in PATH
- Create at least one git tag: `git tag v1.0.0`
- Check git repository is initialized

### Debug Commands
```bash
# Check current version resolution
./gradlew showVersion

# Check git tag status
git describe --tags --always --dirty

# Manual version override
./gradlew build -Pversion=1.0.0
```

## Future Versioning Considerations

### Planned Enhancements
- **1.1.x**: Enhanced CLI options, additional template variables
- **1.2.x**: Performance optimizations, template caching improvements  
- **2.0.x**: Java 17+ requirement, enhanced type safety, modern Gradle features

### Deprecation Policy
- Features marked deprecated in minor versions
- Removed in next major version
- Minimum 6-month deprecation period
- Clear migration path provided in documentation