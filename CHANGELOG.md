# Changelog

All notable changes to the OpenAPI Model Generator Gradle Plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - Development towards v2.1.0

### 🔒 Security

- **CRITICAL**: Resolved CVE-2022-1471 by upgrading SnakeYAML from 1.33 to 2.3
- **MEDIUM**: Resolved CVE-2025-48924 by upgrading Commons Lang3 from 3.14.0 to 3.18.0
- **MEDIUM**: Resolved CVE-2025-48734 by upgrading Checkstyle to 11.0.1
- Added comprehensive OWASP dependency scanning with automated security validation
- Achieved zero security vulnerabilities in dependency scans
- Added SpotBugs security analysis with 70+ issues resolved

### ✨ Features

- **Enhanced Logging System**: Implemented unified logging architecture with automatic rich file logging
  - Rich debug logs automatically saved to `build/logs/openapi-modelgen-debug.log`
  - No configuration required - follows standard Gradle logging conventions
  - Performance metrics and build progress tracking included
  - Full MDC context with spec, template, and component information
- **Version Detection**: Added automatic OpenAPI Generator version detection with multi-strategy fallback
- **Performance Improvements**: Added comprehensive caching with performance metrics tracking
- **Template Processing**: Enhanced template customization with detailed diagnostics
- **Build Automation**: Added custom slash commands for automated build validation

### 💥 Breaking Changes

- **Logging Configuration**: Removed `debug` configuration parameters at all levels
  - Console output now controlled purely by Gradle flags (`--debug`, `--info`, `--quiet`)
  - Rich file logging always captures everything for post-build analysis
  - Per-spec debugging available via grep filtering
- **Deprecated API Removal**: Removed all deprecated template extraction methods
  - `OpenApiTemplateExtractor.extractTemplates(String, String, String)` method removed
  - Legacy template extraction methods eliminated from public/private API

### 🔧 Improvements

- **Code Quality**:
  - Replaced StringUtils dependencies with standard Java methods and Apache Commons Lang3
  - Fixed all JavaDoc warnings and compilation errors (100+ → 0)
  - Implemented Checkstyle for consistent code style
  - Removed 150+ lines of deprecated/legacy code
- **Documentation**:
  - Restructured documentation into focused topic-specific guides
  - Added comprehensive JavaDoc for all public API classes
  - Created detailed configuration, performance, and troubleshooting guides
- **Build System**:
  - Migrated to Gradle version catalogs for centralized dependency management
  - Added dependency bundles for common use cases
  - Improved Windows compatibility with better file handle management
- **Error Handling**: Standardized error handling across all plugin services with actionable guidance

### 🐛 Bug Fixes

- **Windows Compatibility**: Fixed file handle cleanup issues that caused test failures (7 failing → 0)
- **Configuration Cache**: Maintained 100% Gradle configuration cache compatibility
- **Template Processing**: Fixed YAML syntax errors in template customization files
- **Resource Management**: Added try-with-resources blocks to prevent resource leaks
- **Test Reliability**: All 247+ test methods now pass consistently

### 📚 Documentation

- Restructured README.md into streamlined overview with quick start guide
- Created comprehensive guides in `docs/` directory:
  - `docs/configuration.md` - Complete configuration reference
  - `docs/performance.md` - Caching and optimization details
  - `docs/troubleshooting.md` - Common issues and solutions
  - `docs/development.md` - Building and contributing guide
- Added detailed JavaDoc documentation for all public APIs
- Documented Windows file handling design decisions and performance trade-offs

### 🔧 Technical Details

- **Dependencies Updated**:
  - SnakeYAML: 1.33 → 2.3 (CVE-2022-1471)
  - Commons Lang3: 3.14.0 → 3.18.0 (CVE-2025-48924)
  - Checkstyle: → 11.0.1 (CVE-2025-48734)
  - Added SpotBugs for security analysis
- **Build Tools**:
  - Added OWASP Dependency Check v12.1.3
  - Implemented CodeQL analysis for Java
  - Added automated GitHub Actions workflows
- **Code Organization**:
  - Reorganized logging package structure
  - Extracted hard-coded values into well-documented constants
  - Standardized error handling patterns across services

---

## [v2.0.2] - 2024-XX-XX

Previous release - see git history for details.

---

## Links

- [Repository](https://github.com/guidedbyte-technologies/openapi-modelgen)
- [Documentation](docs/)
- [Issue Tracker](https://github.com/guidedbyte-technologies/openapi-modelgen/issues)
