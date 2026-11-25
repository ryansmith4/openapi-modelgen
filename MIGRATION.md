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
1. Update Gradle wrapper to 8.5+
1. Review and update any custom templates
1. Test generation with new structure
1. Update CI/CD pipelines

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

The plugin's own version is the `version` property in `plugin/gradle.properties`. It is not read from
git tags. To build with a different version for one run, pass it on the command line:

```bash
./gradlew plugin:build -Pversion=1.1.0-RC1
```

A release publishes the version named by the pushed tag (see Release Commands below).

## Release Workflow

### Development Cycle
1. **Feature Development**: Work on `1.0.0-SNAPSHOT`
1. **Pre-release Testing**: `1.1.0-RC1`, `1.1.0-RC2`
1. **Release**: Create `v1.1.0` tag
1. **Hotfix**: `1.1.1` for critical fixes

### Release Commands

See "Release Process" in [docs/development.md](docs/development.md). In short: set the version, run
`scripts/update-changelog.sh`, commit, then push a `v<version>` tag. The release workflow publishes; do
not run `publishPlugins` by hand as well.

## Configuration Migration

### Checking Compatibility
```bash
# Show the plugin's current version (run in this repository)
./gradlew plugin:showVersion

# Validate version format (run in this repository)
./gradlew plugin:validateVersion

# Get help for configuration options (run in a project that applies the plugin)
./gradlew generateHelp
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
- A release tag is the version with a `v` in front: `v1.2.3`, not `1.2.3`

**"Cannot publish development version" Error:**
- Remove the `-SNAPSHOT` suffix from the version before publishing

### Debug Commands
```bash
# Check the current version
./gradlew plugin:showVersion

# Build with a different version for one run
./gradlew plugin:build -Pversion=1.0.0
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