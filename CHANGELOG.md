# Changelog

All notable changes to the OpenAPI Model Generator Gradle Plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v2.2.3] - 2026-10-13

### 🐛 Bug Fixes

- **OpenAPI Generator 7.22.0+ compatibility**: Fixed `NoSuchMethodError: GenerateTask.getInputSpec()`
  when using OpenAPI Generator 7.22.0 or later. Upstream PR #23042 changed `inputSpec`, `outputDir`
  and `templateDir` from `Property<String>` to `RegularFileProperty` / `DirectoryProperty`. The plugin
  now adapts to either API at runtime through a small reflective bridge (`GenerateTaskPropertyBridge`).
  Tested range is 7.10.0 through 7.26.0.
- Removed the debug and failure logging hooks from the task configuration service. They were never
  attached to any task but still held execution-time `Task.getProject()` access (configuration cache
  hygiene). The `inputSpec` path is now resolved against the project directory at configuration time,
  so relative paths behave identically on every generator version.
- **Configuration cache entry invalidated after every `clean`**: the rich debug log directory `build/logs`
  was probed and created while the plugin's loggers initialised, which can happen at configuration time.
  Gradle recorded that as a configuration cache input, so the run after the first run following a
  `clean` reported "file system entry 'build/logs' has been created" and re-ran configuration. The
  directory and log file are now created on the first write, at execution time.
- **Absolute `outputDir` values** were nested under the project directory by the generate task while the
  source-set registration and `generateClean` used the path as given, so the three disagreed. All three now
  resolve the value the same way: relative against the project directory, absolute passed through.
- **Generated tests compiled as main code (2.2.2 regression)**: the automatic Java integration registered
  the whole `build/generated/sources/openapi` tree as a main source directory, so anything the generator
  wrote under `{spec}/src/test/java` was compiled by `compileJava` and failed with
  `package org.junit.jupiter.api does not exist`. Only each spec's `{outputDir}/src/main/java` is
  registered now. The registration is still made when the Java plugin is applied (so IntelliJ sync in
  multi-module projects keeps working) but is resolved lazily, so spec-level `outputDir` overrides are
  honoured.
- **`generateModelTests`, `generateApiTests`, `generateModelDocumentation` and `generateApiDocumentation`
  were never applied** to the generate task, so the generator's own defaults won: the plain `java`
  generator wrote model unit tests and a `docs/` tree into the spec output directory. The documented
  default (`false`) now reaches the task on every spec; set a flag to `true` to opt in. Generated tests
  are never added to the main source set.

### 🔄 Changed

- Version check now warns, rather than logs at info, when the generator is newer than the tested maximum
  (7.26.0), and names the tested range.
- CI now compiles the test-app (`compileJava`) after generating it.
- commons-lang3 3.18.0 to 3.21.0 (3.18.0 remains the floor for CVE-2025-48924).
- Dependabot now ignores the minimum-supported `openapi-generator-plugin-compat` ref, JUnit major bumps,
  and the plugin's own id that it misread from a documentation snippet.
- The plugin jar manifest no longer carries `Built-Date`, `Built-By`, `Built-JDK` or `Built-Gradle`. The
  timestamp changed the jar on every build, which changed the task classpath of every generate task in a
  composite build (so they re-ran although nothing had changed) and defeated the build cache across machines.
- test-app and default test classpath moved to OpenAPI Generator 7.26.0. CI matrix now covers
  7.10.0, 7.14.0, 7.21.0 and 7.26.0 on Java 17 and 21 (`-PopenapiGeneratorVersion`).
- On OpenAPI Generator 7.25.0+ the plugin sets `generateJsonIncludeAnnotations=false` and
  `generateJsonSetterNullsAnnotations=false` unless you set them, which silences two warnings per spec
  without changing generated code.

### 📝 Notes for consumers upgrading the generator past 7.14.0

- 7.17.0 defaults `REFACTOR_ALLOF_WITH_PROPERTIES_ONLY` to true.
- 7.21.0 defaults `useSpringBoot3` to true for the spring generator.
- 7.22.0+ runs generation in a Gradle Worker with classloader isolation; expect one extra log line per task.
- 7.23.0+: classes referenced by name in generator options (a custom `NORMALIZER_CLASS`, a custom
  generator class) are no longer visible from the buildscript classpath because generation runs in an
  isolated worker. On 7.25.0+ declare them on the `openApiGeneratorExtra` configuration, which the
  upstream plugin creates when this plugin applies it:

  ```groovy
  dependencies {
      openApiGeneratorExtra 'com.acme:my-normalizer:1.0.0'
  }
  ```

  See docs/configuration.md, "Custom generator classes on OpenAPI Generator 7.23+".
- 7.24.0 emitted field-level `@JsonInclude`/`@JsonSetter` by default; 7.25.0 made that opt-in again.
  Prefer 7.25.0+.
- Generated DTOs differ from 7.14.0 output. Observed in the test-app on 7.26.0: spring models that use
  Lombok via `additionalModelTypeAnnotations` now carry `@NotNull`, `@Size`, `@Valid`, `@Schema` and
  `@JsonProperty` on their fields (7.14.0 emitted bare fields, so bean validation did not apply);
  the plain `java` generator emits `@JsonProperty(value = ..., required = true|false)` on accessors
  instead of `@JsonProperty(NAME)`; `toIndentedString` is a one-liner; the `@Generated` comment names
  the new version. No runtime behaviour of this plugin changed.

---

## [v2.2.2] - 2025-12-18

### 🐛 Bug Fixes

- **Fixed IntelliJ Multi-Module Source Recognition**: Resolved issue where IntelliJ couldn't find generated DTOs in multi-module projects
  - Root cause: Source set registration happened in `afterEvaluate`, which was too late for IntelliJ sync in cross-module dependency scenarios
  - Solution: Register generated sources base directory early via `plugins.withType(JavaPlugin.class)` callback
  - Uses convention-based path (`build/generated/sources/openapi`) that doesn't require extension evaluation
  - Configuration cache compatible using `ProjectLayout.getBuildDirectory()`

### 🔧 Technical Details

- Split `configureJavaIntegration()` into two methods for proper timing:
  - `configureJavaSourceSetsEarly()` - Registers source directories during plugin application (before `afterEvaluate`)
  - `configureCompileJavaDependency()` - Wires task dependencies in `afterEvaluate`
- This ensures IntelliJ captures source sets correctly during Gradle sync, enabling proper import resolution across modules

---

## [v2.2.1] - 2025-12-18

### 🐛 Bug Fixes

- **Fixed CI Workflow**: Updated CI workflow to use correct spec-specific output directory path
  - Changed verification path from `/openapi/src/main/java/` to `/openapi/pets/src/main/java/`

---

## [v2.2.0] - 2025-12-18

### ✨ Features

- **Automatic Java Integration**: Plugin now automatically integrates with Java plugin
  - Automatically registers generated source directories with main sourceSet
  - Automatically wires `compileJava` to depend on `generateAllModels`
  - **No manual `sourceSets` or `dependsOn` configuration required**
  - Each spec's generated sources registered individually for correct IDE support

- **Spec-Specific Output Directories**: Each spec now generates to its own subdirectory
  - Default outputDir structure: `build/generated/sources/openapi/{specName}/src/main/java/`
  - Example: `pets` spec → `build/generated/sources/openapi/pets/src/main/java/`
  - Example: `orders` spec → `build/generated/sources/openapi/orders/src/main/java/`
  - **Prevents Gradle build cache conflicts** that caused intermittent missing files
  - Explicit `outputDir` at spec level still respected (no spec name appended)

- **Schema Mappings**: Added `schemaMappings` configuration option for OpenAPI Generator
  - Map OpenAPI schema names to custom Java class names at global and spec levels
  - Supports renaming generated classes and resolving naming conflicts
  - Follows same merge pattern as `typeMappings` with spec-level overrides
  - Configuration cache compatible
  - Example: `schemaMappings(['Pet': 'Animal', 'User': 'Person'])`
  - See [configuration.md](docs/configuration.md#schema-mappings) for details

### 🐛 Bug Fixes

- **Fixed Intermittent Code Generation Failures**: Resolved issue where generated models were sometimes missing after `clean build`
  - Root cause: OpenAPI Generator's `@OutputDirectory` annotation cached the entire shared outputDir
  - When Gradle restored cached outputs for one spec, it could overwrite files from other specs
  - Solution: Each spec now has its own output subdirectory, ensuring isolated cache entries
  - Issue reported in [OPENAPI_MODELGEN_BUG_REPORT.md](OPENAPI_MODELGEN_BUG_REPORT.md)

- **Fixed Missing Task Dependencies**: `compileJava` now automatically depends on `generateAllModels`
  - Previously required manual `compileJava.dependsOn generateAllModels` configuration
  - Now handled automatically when Java plugin is present

### 🔧 Improvements

- **Improved Incremental Build Tracking**: Template customization directories are now properly tracked
  - YAML customization file changes now correctly invalidate task outputs
  - Uses `@InputDirectory` annotation for content-based change detection

- **Enhanced Task Output Validation**: PrepareTemplateDirectoryTask now validates outputs exist
  - Added `upToDateWhen` check to detect missing or incomplete outputs
  - Tasks automatically re-run if output directory or marker file is missing

### 💥 Breaking Changes

- **Output Directory Structure Changed**: Generated files now in spec-specific subdirectories
  - **Before**: `build/generated/sources/openapi/src/main/java/{package}/`
  - **After**: `build/generated/sources/openapi/{specName}/src/main/java/{package}/`
  - **Migration**: Remove manual `sourceSets` configuration - plugin handles it automatically
  - **Opt-out**: Set explicit `outputDir` at spec level to use custom path without spec name

### 📚 Documentation

- Added comprehensive schema mappings documentation with use cases and examples
- Updated configuration.md with schema mappings section and examples
- Updated README Quick Start to remove manual sourceSet/dependsOn configuration
- Added migration notes for users upgrading from v2.1.x

---

## [v2.1.0] - 2025-01-XX

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
