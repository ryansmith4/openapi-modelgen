---
render_with_liquid: false
---

# Template System Architecture

This document provides detailed technical documentation of the OpenAPI Model Generator Plugin's advanced template system, including the multi-source template resolution, library template support, conflict resolution, and working directory organization.

## Template Sources Architecture

The plugin implements a sophisticated **6-source template resolution system** that supports templates and customizations from multiple sources with automatic conflict resolution.

### Template Sources Hierarchy

The plugin processes template sources in the following precedence order (highest to lowest):

1. **User Templates** (`user-templates`)
   - Complete `.mustache` files in `userTemplateDir`
   - **Highest precedence** - overrides everything
   - Example: `src/main/templates/spring/pojo.mustache`

1. **User YAML Customizations** (`user-customizations`)
   - Modifications defined in `userTemplateCustomizationsDir`
   - Applied to base templates from lower-precedence sources
   - Example: `src/main/templateCustomizations/spring/pojo.mustache.yaml`

1. **Library Templates** (`library-templates`)
   - Complete `.mustache` files from JAR dependencies
   - Extracted from `META-INF/openapi-templates/{generatorName}/`
   - Example: Corporate template library with shared templates

1. **Library YAML Customizations** (`library-customizations`)
   - YAML modifications from JAR dependencies
   - Extracted from `META-INF/openapi-customizations/{generatorName}/`
   - Example: Corporate customization library with shared YAML rules

1. **Plugin YAML Customizations** (`plugin-customizations`)
   - Built-in optimizations from plugin resources
   - Example: `plugin/src/main/resources/templateCustomizations/spring/pojo.mustache.yaml`

1. **OpenAPI Generator Defaults** (`openapi-generator`)
   - **Lowest precedence** - standard OpenAPI Generator templates
   - Always available as fallback source

### Template Sources Configuration

Sources are configurable via the `templateSources` property:

```gradle
openapiModelgen {
    defaults {
        templateSources([
            "user-templates",           // Explicit .mustache files (highest)
            "user-customizations",      // Project YAML customizations  
            "library-templates",        // Templates from JAR dependencies
            "library-customizations",   // YAML customizations from JARs
            "plugin-customizations",    // Built-in plugin customizations
            "openapi-generator"         // OpenAPI Generator defaults (lowest)
        ])
    }
}
```

**Auto-Discovery**: The plugin automatically discovers which sources are available and skips missing ones. Non-existent sources are silently ignored.

## Library Template Support

The plugin provides comprehensive support for distributing templates and customizations via JAR dependencies, enabling template libraries for enterprise environments.

### Library Template Structure

Template libraries should follow this structure in their JAR:

```text
my-templates-lib.jar
└── META-INF/
    ├── openapi-library.yaml                 # Library metadata (required)
    ├── openapi-templates/                   # Explicit templates
    │   └── spring/
    │       ├── customPojo.mustache         # Custom template
    │       └── enhancedEnum.mustache       # Enhanced enum template
    └── openapi-customizations/             # YAML customizations  
        └── spring/
            ├── pojo.mustache.yaml          # POJO customizations
            ├── model.mustache.yaml         # Model wrapper customizations
            └── enumClass.mustache.yaml     # Enum customizations
```

### Library Metadata

Each template library must include `META-INF/openapi-library.yaml`:

```yaml
name: "corporate-api-templates"
version: "2.1.0"
description: "Corporate standard templates with audit, validation, and documentation"
author: "Platform Engineering Team"
homepage: "https://github.com/company/api-templates"

supportedGenerators:
  - "spring"
  - "spring-boot"

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

### Using Template Libraries

Add template libraries as dependencies using the `openapiCustomizations` configuration:

```gradle
dependencies {
    // Corporate template library
    openapiCustomizations 'com.company:corporate-api-templates:2.1.0'
    
    // Additional specialized libraries
    openapiCustomizations 'com.company:validation-templates:1.0.0'
    openapiCustomizations 'com.company:audit-templates:1.5.0'
}

openapiModelgen {
    defaults {
        templateSources([
            "user-templates",           // Project overrides (highest)
            "user-customizations",      // Project customizations
            "library-templates",        // Corporate template libraries
            "library-customizations",   // Corporate YAML customizations  
            "plugin-customizations",    // Plugin defaults
            "openapi-generator"         // Generator defaults (lowest)
        ])
        
        // No logging configuration needed - rich debug log always captured
    }
}
```

## Conflict Resolution System

The plugin implements sophisticated conflict resolution for handling multiple template sources and overlapping customizations.

### Template Resolution Rules

1. **Explicit Templates Override Everything**
   - User templates (`templateDir`) override all other sources
   - Library templates override customizations and generator defaults
   - First explicit template found wins (by source precedence)

1. **YAML Customizations Are Cumulative**
   - Multiple YAML customizations can apply to the same template
   - Higher-precedence customizations override lower ones
   - Insertions and replacements are merged intelligently

1. **Source Precedence Always Respected**
   - Earlier sources in `templateSources` list have higher precedence
   - Sources are processed in order, with conflicts resolved immediately

### Conflict Resolution Examples

#### Example 1: Multiple Library Templates

```text
Library A: META-INF/openapi-templates/spring/pojo.mustache
Library B: META-INF/openapi-templates/spring/pojo.mustache
User:      src/main/templates/spring/pojo.mustache

Resolution: User template wins (highest precedence)
```

#### Example 2: Overlapping YAML Customizations

```text
Library A: META-INF/openapi-customizations/spring/pojo.mustache.yaml
  - Adds audit fields
Library B: META-INF/openapi-customizations/spring/pojo.mustache.yaml  
  - Adds validation annotations
User:      src/main/templateCustomizations/spring/pojo.mustache.yaml
  - Adds custom header

Resolution: All applied in precedence order (User → Library A → Library B)
Base template from generator defaults with all customizations merged
```

#### Example 3: Template vs Customization Conflict

```text
Library A: META-INF/openapi-templates/spring/pojo.mustache (explicit)
Library B: META-INF/openapi-customizations/spring/pojo.mustache.yaml (YAML)

Resolution: Library A explicit template wins, Library B YAML ignored
```

### Debug Template Resolution

View conflict resolution in the rich debug log (always captured):

```gradle
openapiModelgen {
    defaults {
        // No logging configuration needed - view build/logs/openapi-modelgen-debug.log
    }
}
```

Debug output shows:
```
DEBUG: Template 'pojo.mustache' resolved from: user-templates
DEBUG: Template 'model.mustache' resolved from: library-templates (corporate-api-templates-2.1.0) 
DEBUG: Template 'enumClass.mustache' resolved from: plugin-customizations
DEBUG: YAML customization 'pojo.mustache.yaml' applied from: user-customizations
DEBUG: YAML customization 'pojo.mustache.yaml' applied from: library-customizations (validation-templates-1.0.0)
```

## Template Working Directory

The plugin creates spec-specific template working directories at `build/template-work/{generatorName}-{specName}/` where all template resolution and customization occurs. Each spec gets its own isolated directory to prevent cross-contamination.

### Directory Structure

```
build/template-work/spring-{specName}/  # Each spec has its own directory
├── .working-dir-cache              # Cache metadata for incremental builds
├── .source-resolution-cache        # Template source resolution cache
├── orig/spring/                    # Original template backups (selective)
│   ├── pojo.orig                   # Backs up base template before customizations
│   └── model.orig                  # Backs up library template before user customizations
├── pojo.mustache                   # Final resolved template (user + library + plugin YAML applied)
├── model.mustache                  # Library template with user customizations applied  
├── customEnum.mustache             # Explicit library template (no customizations)
├── beanValidation.mustache         # Dependency template (no customization)
├── jackson_annotations.mustache    # Dependency template (no customization)
└── ... other dependency templates
```

### Template File Categories

#### 1. Resolved Templates with Customizations
- Templates with YAML customizations from any source applied
- May originate from user, library, or generator sources
- Example: `pojo.mustache` (base from generator + library YAML + user YAML)
- **Have corresponding `.orig` backup files**

#### 2. Explicit Templates from Libraries
- Complete `.mustache` files from template libraries
- Used as-is without further customization
- Example: `customEnum.mustache` from corporate library
- **No `.orig` backup files** (no modifications made)

#### 3. Original Backup Files (`.orig`)
- Located in `orig/{generatorName}/` subdirectory  
- Created **only** for templates receiving customizations
- Preserve the base template (from highest precedence source) before YAML modifications
- Example: `orig/spring/pojo.orig` backs up generator default before customizations
- Example: `orig/spring/model.orig` backs up library template before user customizations

#### 4. Dependency Templates
- Templates extracted due to `{{>templateName}}` references
- Remain unmodified from their source (generator, library, etc.)
- Examples: `beanValidation.mustache`, `jackson_annotations.mustache`  
- **No `.orig` backup files** (no modifications made)

## Template Resolution Process

The plugin follows a sophisticated multi-phase resolution process that handles all 6 template sources with proper conflict resolution.

### Phase 1: Working Directory Setup
1. Check working directory cache validity using content hashing
1. Clean directory if cache invalid or sources changed
1. Create directory structure including `orig/{generatorName}/`
1. Initialize source resolution cache

### Phase 2: Library Discovery and Extraction
1. **Scan classpath** for template libraries with `META-INF/openapi-library.yaml`
1. **Extract library templates** from `META-INF/openapi-templates/{generatorName}/`
1. **Extract library customizations** from `META-INF/openapi-customizations/{generatorName}/`
1. **Validate library metadata** and check version compatibility
1. **Cache library contents** for performance across builds

### Phase 3: Template Source Resolution (by precedence)
1. **User Templates** (`user-templates`)
   - Copy explicit `.mustache` files from `templateDir` to working directory
   - Mark as highest precedence source for each template
   
1. **User YAML Customizations** (`user-customizations`)
   - Identify YAML customization files in `templateCustomizationsDir`
   - Queue for application to base templates
   
1. **Library Templates** (`library-templates`)
   - Copy explicit library `.mustache` files not already resolved
   - Apply in library dependency order
   
1. **Library YAML Customizations** (`library-customizations`)
   - Identify library YAML customizations not yet applied
   - Queue for application in precedence order
   
1. **Plugin YAML Customizations** (`plugin-customizations`)
   - Process built-in plugin YAML customizations
   - Skip templates already resolved or customized
   
1. **OpenAPI Generator Defaults** (`openapi-generator`)
   - Extract generator defaults for remaining templates
   - Always available as fallback source

### Phase 4: YAML Customization Application
1. **Identify base templates** requiring YAML customizations
1. **Create `.orig` backups** for templates receiving customizations
1. **Apply customizations in precedence order**:
   - User customizations (highest priority)
   - Library customizations (by library precedence)  
   - Plugin customizations (lowest priority)
1. **Handle conflicts** by allowing higher precedence to override lower
1. **Validate results** and report any application errors

### Phase 5: Dependency Discovery and Extraction
1. **Parse all resolved templates** for `{{>templateName}}` references
1. **Recursively discover dependencies** following template inclusion chains
1. **Extract missing dependencies** from appropriate sources (libraries, generator)
1. **Place dependency templates** directly in working directory
1. **No `.orig` files created** for dependencies (no modifications made)

### Phase 6: Cache Update and Validation
1. **Generate working directory cache key** from all input sources
1. **Update local cache metadata** with source resolution information
1. **Persist global cache** for cross-build and cross-project performance
1. **Record library usage** for debugging and audit purposes

## Selective Backup Strategy

The plugin implements a **selective backup strategy** for `.orig` files:

### When `.orig` Files Are Created
- Template has user YAML customizations applied
- Template has library YAML customizations applied
- Template has plugin YAML customizations applied
- Template is being modified in any way from its base source

### When `.orig` Files Are NOT Created
- Template is extracted only as a dependency (`{{>templateName}}`)
- Template remains unmodified from its source (user, library, or generator)
- No customizations of any kind exist for the template
- Explicit library templates used as-is without further modification

### Benefits
1. **Efficiency**: Only backs up what's actually being modified
1. **Clarity**: Easy to identify which templates have customizations applied
1. **Multi-source awareness**: Preserves the correct base template from highest precedence source
1. **Storage**: Minimal disk usage for dependency templates and library templates
1. **Performance**: Faster template processing and cache validation
1. **Debugging**: Clear audit trail of what was customized and from which source

## Recursive Template Discovery

The plugin uses intelligent template dependency resolution:

### Discovery Process
1. **Parse all resolved templates** for `{{>templateName}}` references
1. **Search template sources in precedence order**:
   - User templates (`templateDir`)
   - Library templates (`META-INF/openapi-templates/`)
   - Plugin resources (`plugin/src/main/resources/templates/`)
   - OpenAPI Generator JAR (via reflection and resource scanning)
1. **Recursively parse dependencies** for additional references
1. **Cache extraction results** to avoid duplicate work across builds
1. **Track dependency sources** for debugging and conflict resolution

### Example Discovery Chain
```
pojo.mustache (user customized, base from generator)
├── {{>beanValidation}} → found in corporate-lib → extracts beanValidation.mustache
├── {{>jackson_annotations}} → found in generator → extracts jackson_annotations.mustache
└── {{>xmlAnnotation}} → found in generator → extracts xmlAnnotation.mustache
    └── {{>additionalModelTypeAnnotations}} → found in validation-lib → extracts additionalModelTypeAnnotations.mustache
        └── {{>customValidation}} → found in corporate-lib → extracts customValidation.mustache
```

### Template Extraction Sources (by precedence)
1. **User templates**: Direct file system copies from `templateDir`
1. **Library templates**: Extracted from `META-INF/openapi-templates/{generatorName}/` in JAR dependencies
1. **Plugin resources**: Built-in templates in `plugin/src/main/resources/templates/{generatorName}/`
1. **OpenAPI Generator JAR**: Default templates via reflection and resource scanning

## Caching Architecture

### Working Directory Cache
- **Key**: Combination of spec hash, all source content hashes, template variables, generator version, library versions
- **Validation**: SHA-256 content verification of all templates and sources
- **Scope**: Per working directory, persistent across builds
- **Multi-source aware**: Tracks changes in user, library, and plugin sources

### Template Extraction Cache  
- **Key**: Template name + generator + source identifier + version
- **Storage**: Global cache in `~/.gradle/caches/openapi-modelgen/`
- **Lifetime**: Persistent across projects and builds
- **Library support**: Caches templates from all sources including libraries

### Library Template Cache
- **Key**: Library JAR hash + template path + extraction timestamp
- **Storage**: Shared global cache for all projects using same libraries
- **Validation**: Automatic invalidation when library versions change
- **Performance**: Eliminates repeated JAR scanning across projects

### Source Resolution Cache
- **Key**: Template name + generator + available sources fingerprint
- **Purpose**: Caches which source provides each template to avoid re-scanning
- **Scope**: Per-build session, not persisted
- **Conflict resolution**: Tracks precedence decisions for debugging

### Cache Benefits
- **90% faster** no-change builds with library templates
- **70% faster** incremental builds with mixed sources
- **Thread-safe** parallel processing across multiple template sources
- **Cross-project** template sharing including library templates
- **Library optimization**: Shared cache for corporate template libraries
- **Intelligent invalidation**: Precise cache invalidation based on actual source changes

## Error Handling

### Template Resolution Errors
- **Missing templates**: Fallback through source hierarchy (user → library → plugin → generator)
- **Library extraction failures**: Detailed error messages with library name and template path
- **Customization conflicts**: Clear reporting when multiple libraries modify same template
- **YAML parsing errors**: Validation with line numbers and source file identification
- **Version compatibility**: Warnings when library requires newer plugin or generator versions

### Cache Corruption Recovery
- **Automatic detection**: SHA-256 hash validation across all source types
- **Self-healing**: Invalid cache automatically cleared and rebuilt for affected sources
- **Graceful fallback**: Falls back to fresh extraction from correct source on cache failures
- **Library cache isolation**: Corruption in one library cache doesn't affect others

### Library-Specific Errors
- **Missing library metadata**: Clear errors when `META-INF/openapi-library.yaml` is invalid or missing
- **Incompatible versions**: Detailed version mismatch reporting with suggested resolutions
- **Conflicting libraries**: Warnings when multiple libraries provide same templates with clear precedence resolution
- **Invalid template paths**: Specific errors for malformed library template structures

## Best Practices

### Template Organization
1. **Use generator subdirectories**: Always organize by generator (e.g., `spring/`)
1. **Prefer YAML customizations**: More maintainable than full template replacements
1. **Test with dependencies**: Verify that template references work correctly
1. **Library structure**: Follow META-INF conventions for distributable template libraries
1. **Version template libraries**: Use semantic versioning for library compatibility

### Library Template Best Practices
1. **Provide comprehensive metadata**: Always include complete `openapi-library.yaml`
1. **Version compatibility ranges**: Specify minimum plugin and generator versions
1. **Namespace templates carefully**: Use unique names to avoid conflicts with other libraries
1. **Document template library features**: Clear documentation of what customizations are provided
1. **Test across plugin versions**: Ensure library works with supported plugin versions

### Performance Optimization
1. **Enable caching**: Don't disable working directory cache without good reason
1. **Minimize customizations**: Only customize what you actually need to change
1. **Use parallel processing**: Enable parallel generation for multi-spec projects
1. **Optimize template sources**: Order `templateSources` with most frequently used sources first
1. **Library consolidation**: Use fewer, well-organized libraries rather than many small ones

### Debugging Multi-Source Issues
1. **Check rich debug log**: View `build/logs/openapi-modelgen-debug.log` for detailed source resolution information
1. **Check working directory**: Inspect `build/template-work/{generator}-{specName}/` for final template resolution
1. **Verify .orig files**: Ensure original templates match expectations from correct sources
1. **Library inspection**: Check `~/.gradle/caches/openapi-modelgen/` for library template cache
1. **Source precedence verification**: Confirm template sources are ordered correctly
1. **Conflict analysis**: Review logs for template conflicts between sources

## Migration Considerations

### From Full Templates to YAML Customizations
1. **Identify changes**: Compare your full template to current source template (user/library/generator)
1. **Extract modifications**: Convert changes to YAML insertion/replacement rules
1. **Test thoroughly**: Verify that YAML customizations produce identical results
1. **Remove full templates**: Delete `.mustache` files to enable YAML processing
1. **Consider library extraction**: Move common customizations to shared template libraries

### Migrating to Template Libraries
1. **Audit existing customizations**: Identify common patterns across projects
1. **Create library structure**: Set up proper META-INF directory structure
1. **Extract shared templates**: Move commonly used explicit templates to library
1. **Convert to YAML customizations**: Transform project-specific changes to library YAML files
1. **Version and test**: Create versioned library and test across consuming projects
1. **Update project configurations**: Replace local templates/customizations with library dependencies

### Template Library Version Updates
1. **Check compatibility**: Review library changelog and version compatibility matrix
1. **Test incrementally**: Update one library at a time in development environment
1. **Validate precedence**: Ensure library precedence order is correct for conflict resolution
1. **Clear library cache**: Force fresh library extraction with `./gradlew generateClean`
1. **Monitor conflicts**: Watch for template conflicts when multiple libraries are updated

### OpenAPI Generator Version Updates
1. **Test compatibility**: YAML customizations and libraries may need adjustment for new versions
1. **Update library requirements**: Check if template libraries support new generator version
1. **Clear all caches**: Force fresh template extraction with `./gradlew generateClean`
1. **Validate output**: Compare generated code before and after version change
1. **Update conditions**: Use `generatorVersion` conditions for version-specific customizations
1. **Library compatibility**: Verify all template libraries work with new generator version