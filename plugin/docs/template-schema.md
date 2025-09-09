# Template Customization Schema Reference

Complete reference for OpenAPI Model Generator Plugin YAML template customization files.

## Overview

Template customization files use YAML format to modify existing OpenAPI Generator templates without replacing
them entirely. This allows for surgical modifications while maintaining compatibility with OpenAPI Generator
updates.

## Root Schema

```yaml
metadata:                    # Optional - Customization metadata
insertions:                  # Optional - List of content insertions  
replacements:                # Optional - List of content replacements
```

## Metadata Schema

Optional metadata section for documentation and versioning:

```yaml
metadata:
  name: "string"                    # Customization name
  description: "string"             # Human-readable description  
  version: "string"                 # Version identifier
  author: "string"                  # Author information
  created: "YYYY-MM-DD"            # Creation date
  modified: "YYYY-MM-DD"           # Last modification date
```

### Example

```yaml
metadata:
  name: "Enhanced Model Template"
  description: "Adds Jackson imports and custom annotations"
  version: "2.1.0"
  author: "Development Team"
  created: "2025-01-15"
  modified: "2025-01-20"
```

## Insertions Schema

Insertions add content at specific locations in templates:

```yaml
insertions:
  - # Position specifiers (exactly one required)
    after: "string"                 # Insert after this pattern
    before: "string"                # Insert before this pattern  
    at: "start|end"                 # Insert at template start/end
    
    # Content (required)
    content: "string"               # Content to insert (multi-line supported)
    
    # Optional conditions
    conditions: ConditionSet        # When to apply insertion
    fallback: Insertion            # Alternative if conditions fail
```

### Position Specifiers

Exactly one position specifier is required:

- **`after`**: Insert content immediately after the first occurrence of the pattern
- **`before`**: Insert content immediately before the first occurrence of the pattern  
- **`at`**: Insert content at template start (`"start"`) or end (`"end"`)

### Content Property

The `content` property supports:

- **Multi-line strings**: Use YAML `|` or `>` for multi-line content
- **Template variables**: Reference variables using `{{variableName}}` syntax
- **Partial templates**: Reference other templates using `{{>templateName}}`
- **Conditional content**: Use Mustache conditional syntax

### Insertion Examples

#### Basic Insertion

```yaml
insertions:
  - after: "package {{package}};"
    content: |
      
      // Custom header comment
      // Generated on {{currentDate}}
```

#### Conditional Insertion

```yaml
insertions:
  - before: "public class {{classname}}"
    content: |
      @CustomAnnotation
    conditions:
      templateNotContains: "@CustomAnnotation"
```

#### Fallback Insertion

```yaml
insertions:
  - after: "{{#jackson}}"
    content: |
      import com.fasterxml.jackson.annotation.JsonInclude;
    conditions:
      templateNotContains: "import com.fasterxml.jackson.annotation.JsonInclude;"
    fallback:
      at: "start"  
      content: |
        // Jackson not detected, adding basic import
        import com.fasterxml.jackson.annotation.JsonInclude;
```

## Replacements Schema

Replacements modify existing content in templates:

```yaml
replacements:
  - find: "string"                  # Pattern to find (required)
    replace: "string"               # Replacement content (required)
    type: "string|regex"            # Type of replacement (optional, default: "string")
    conditions: ConditionSet        # Optional conditions
    fallback: Replacement          # Alternative if conditions fail
```

### Pattern Matching

- **String type** (default): Uses exact string matching (not regex)
- **Regex type**: Uses regular expression matching for advanced patterns
- First occurrence is replaced
- Multi-line patterns are supported
- Whitespace and formatting must match exactly for string type

### Replacement Examples

#### Simple Replacement

```yaml
replacements:
  - find: "private {{datatype}} {{name}};"
    replace: "private {{datatype}} {{name}}; // Custom field comment"
```

#### Conditional Replacement  

```yaml
replacements:
  - find: "{{#vendorExtensions.x-extra-annotation}}"
    replace: |
      {{#vendorExtensions.x-extra-annotation}}
      @CustomAnnotation
    conditions:
      generatorVersion: ">=7.11.0"
```

#### Regex Replacement

```yaml
replacements:
  - find: "private\\s+(\\w+)\\s+(\\w+);"
    replace: "private $1 $2; // Auto-generated field"
    type: "regex"
    conditions:
      templateContains: "private"
```

## Conditions Schema

Conditions control when customizations apply using complex logic:

```yaml
conditions:
  # Version conditions
  generatorVersion: "string"              # Semantic version constraint
  
  # Template content conditions
  templateContains: "string"              # Must contain exact string
  templateNotContains: "string"           # Must NOT contain exact string
  templateContainsAll: ["str1", "str2"]   # Must contain all strings
  templateContainsAny: ["str1", "str2"]   # Must contain at least one string
  
  # Feature detection
  hasFeature: "string"                    # Must support named feature
  hasAllFeatures: ["feat1", "feat2"]     # Must support all features
  hasAnyFeatures: ["feat1", "feat2"]     # Must support any feature
  
  # Environment conditions
  projectProperty: "string"               # Project property condition
  environmentVariable: "string"           # Environment variable condition  
  buildType: "string"                     # Build configuration type
  
  # Logical operators
  allOf: [ConditionSet, ...]             # All conditions must be true
  anyOf: [ConditionSet, ...]             # Any condition must be true
  not: ConditionSet                      # Condition must be false
```

### Version Conditions

Semantic versioning constraints following [SemVer](https://semver.org/) specification:

```yaml
conditions:
  generatorVersion: ">=7.11.0"      # Greater than or equal
  # generatorVersion: ">7.10.0"     # Greater than
  # generatorVersion: "<=7.15.0"    # Less than or equal
  # generatorVersion: "<8.0.0"      # Less than
  # generatorVersion: "7.14.0"      # Exact version
  # generatorVersion: "~>7.14.0"    # Compatible version (~7.14.0)
  # generatorVersion: "^7.14.0"     # Compatible version (^7.14.0)
```

### Template Content Conditions

Check for patterns in the base template:

```yaml
conditions:
  templateContains: "import com.fasterxml.jackson"
  templateNotContains: "@Deprecated"
  templateContainsAll: 
    - "{{#jackson}}"
    - "{{#validation}}"
  templateContainsAny:
    - "{{#swagger1AnnotationLibrary}}"
    - "{{#swagger2AnnotationLibrary}}"
```

### Feature Detection Conditions

Detect OpenAPI Generator features and capabilities:

```yaml
conditions:
  hasFeature: "jackson"                    # Jackson serialization support
  hasAllFeatures: ["validation", "lombok"] # Bean validation AND Lombok
  hasAnyFeatures: ["xml", "jackson"]       # XML OR Jackson support
```

#### Supported Features

- `jackson` - Jackson JSON serialization
- `validation` - Bean validation annotations
- `lombok` - Lombok annotation support  
- `xml` - XML binding annotations
- `swagger1` - Swagger 1.x annotations
- `swagger2` - Swagger 2.x annotations
- `openapi3` - OpenAPI 3.x annotations

### Environment Conditions

Check build environment and configuration:

```yaml
conditions:
  projectProperty: "customization.enabled=true"    # Gradle project property
  environmentVariable: "ENABLE_CUSTOM_TEMPLATES"   # Environment variable
  buildType: "debug"                               # Build configuration
```

#### Property Formats

- **With value**: `"property.name=expectedValue"`
- **Existence only**: `"property.name"` (checks if property exists)

#### Build Types

- `debug` - Debug/development builds
- `release` - Release/production builds  
- `test` - Test builds

### Logical Operators

Combine conditions using boolean logic:

```yaml
conditions:
  allOf:                           # All conditions must be true (AND)
    - generatorVersion: ">=7.11.0"
    - templateContains: "{{#jackson}}"
  anyOf:                          # Any condition must be true (OR) 
    - hasFeature: "jackson"
    - hasFeature: "xml"
  not:                            # Condition must be false (NOT)
    templateContains: "@Deprecated"
```

#### Nested Logical Operators

Complex logic with nested operators:

```yaml
conditions:
  allOf:
    - generatorVersion: ">=7.11.0"
    - anyOf:
        - hasFeature: "jackson" 
        - hasFeature: "xml"
    - not:
        templateContains: "@Deprecated"
```

## Complete Examples

### Adding Jackson Imports

```yaml
metadata:
  name: "Jackson Import Enhancement"
  description: "Adds missing Jackson imports to model templates"
  version: "1.0.0"

insertions:
  - after: "{{#jackson}}"
    content: |
      import com.fasterxml.jackson.annotation.JsonInclude;
      import com.fasterxml.jackson.annotation.JsonPropertyOrder;
    conditions:
      allOf:
        - templateContains: "{{#jackson}}"
        - templateNotContains: "import com.fasterxml.jackson.annotation.JsonInclude;"
    fallback:
      after: "package {{package}};"
      content: |
        
        // Jackson imports (fallback)
        import com.fasterxml.jackson.annotation.JsonInclude;
        import com.fasterxml.jackson.annotation.JsonPropertyOrder;
```

### Version-Aware Customization

```yaml
metadata:
  name: "Version-Aware Annotations"
  description: "Adds annotations based on OpenAPI Generator version"
  version: "2.0.0"

insertions:
  - before: "public class {{classname}}"
    content: |
      @Generated(value = "{{generatorClass}}", date = "{{generatedDate}}")
    conditions:
      generatorVersion: ">=7.11.0"
    fallback:
      content: |
        @Generated("{{generatorClass}}")

replacements:
  - find: "{{#isDeprecated}}\n@Deprecated{{/isDeprecated}}"
    replace: |
      {{#isDeprecated}}
      @Deprecated(since = "{{deprecatedSince}}", forRemoval = {{forRemoval}})
      {{/isDeprecated}}
    conditions:
      generatorVersion: ">=7.14.0"
```

### Enterprise Template Enhancement

```yaml
metadata:
  name: "Enterprise Template Enhancements" 
  description: "Corporate standards and compliance additions"
  version: "3.0.0"
  author: "Enterprise Architecture Team"

insertions:
  # Add enterprise header
  - at: "start"
    content: |
      /*
       * {{companyName}} - Confidential and Proprietary
       * {{copyright}}
       * 
       * This file contains confidential and proprietary information.
       * Unauthorized reproduction or distribution is prohibited.
       */
      
  # Add audit logging
  - after: "public class {{classname}}"
    content: |
      
      @AuditLogged
      @EntityMetrics
    conditions:
      allOf:
        - projectProperty: "enterprise.audit.enabled=true"
        - not:
            templateContains: "@AuditLogged"
            
  # Add validation enhancements
  - before: "{{#vars}}"
    content: |
      
      // Enterprise validation markers
      {{#hasValidation}}
      @ValidatedEntity
      {{/hasValidation}}
    conditions:
      hasFeature: "validation"

replacements:
  # Enhanced error messages
  - find: "@NotNull"
    replace: "@NotNull(message = \"Field {{name}} is required for {{classname}}\")"
    conditions:
      anyOf:
        - buildType: "debug"
        - projectProperty: "validation.verbose=true"
```

## Validation Rules

The plugin performs comprehensive validation on customization files:

### File Structure Validation

- Valid YAML syntax
- Required schema compliance  
- Proper nesting structure

### Content Validation

- Position specifiers are mutually exclusive
- `content` property is required for insertions
- `find` and `replace` are required for replacements
- `type` property must be "string" or "regex" if specified

### Condition Validation

- Version constraints follow semantic versioning
- Feature names are recognized  
- Property formats are valid
- Logical operators have valid operands

### Runtime Validation

- Pattern matching finds target content
- Template variables resolve correctly
- Conditional logic evaluates properly

## Best Practices

### Organization

1. **Use metadata** - Document your customizations with metadata
2. **Version your schemas** - Track customization versions
3. **Group related changes** - Keep related customizations together

### Conditions

1. **Prefer specific conditions** - Use precise patterns and version constraints
2. **Provide fallbacks** - Handle edge cases with fallback insertions/replacements
3. **Test thoroughly** - Validate customizations across OpenAPI Generator versions

### Content

1. **Preserve formatting** - Match existing template indentation and style
2. **Use template variables** - Leverage existing variables when possible  
3. **Document patterns** - Comment complex patterns and logic

### Performance

1. **Minimize conditions** - Simple conditions evaluate faster
2. **Cache-friendly patterns** - Use consistent patterns across files
3. **Avoid redundant customizations** - Don't duplicate existing functionality

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Pattern not found | Exact pattern doesn't exist in template | Verify pattern exists in base template |
| YAML parsing error | Invalid YAML syntax | Validate YAML structure |
| Condition always false | Incorrect condition logic | Test conditions with debug logging |
| Template variables not resolved | Variable name typos | Check variable names and availability |
| Customization ignored | Explicit user template exists | Remove explicit template or rename |

### Debug Mode

Enable debug logging to troubleshoot customizations:

```bash
./gradlew generateYourTask --debug
```

Debug output includes:

- Template extraction details
- Condition evaluation results  
- Pattern matching status
- Variable resolution information

## Migration Guide

### From Version 1.x

Version 2.0 introduced breaking changes:

- `pattern` property renamed to `after` in insertions
- `pattern` and `replacement` properties renamed to `find` and `replace` in replacements
- `position` property removed (use `after`/`before`/`at`)
- Condition property names changed (`template_contains` → `templateContains`)
- Added `type` property for replacements (defaults to "string", supports "regex")

### From Explicit Templates

Converting from `.mustache` files to YAML customizations:

1. Identify differences from base template
2. Extract additions as insertions
3. Extract modifications as replacements  
4. Add appropriate conditions
5. Test thoroughly

## Schema Changelog

### Version 2.1.0 (Latest)

- Added `fallback` support for insertions and replacements
- Enhanced condition system with logical operators
- Added environment and build type conditions
- Improved validation and error reporting

### Version 2.0.0

- Breaking: Renamed insertion properties for clarity
- Added comprehensive condition system
- Enhanced template variable support
- Added metadata section

### Version 1.0.0

- Initial YAML customization support
- Basic insertions and replacements
- Simple condition system
