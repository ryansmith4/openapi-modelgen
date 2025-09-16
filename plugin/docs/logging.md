# OpenAPI Model Generator - Logging Guide

The OpenAPI Model Generator plugin provides **intuitive logging** that follows standard Gradle conventions while offering comprehensive debugging capabilities through rich file logging.

## 🎯 Key Principles

- **✅ Zero Configuration**: Works out of the box with standard Gradle flags
- **✅ Standard Gradle Behavior**: No plugin-specific logging configuration needed
- **✅ Comprehensive File Logging**: Always captures everything for post-build analysis
- **✅ Per-Spec Analysis**: Filter rich logs by spec using standard Unix tools

## Quick Start

### Standard Gradle Usage (Recommended)

```bash
# Standard Gradle logging - no configuration needed!
./gradlew generatePets                # Normal output
./gradlew generatePets --info         # Verbose output
./gradlew generatePets --debug        # Debug output
./gradlew generatePets --quiet        # Minimal output

# Rich file logging is always available for analysis
grep "[spec:pets]" build/logs/openapi-modelgen-debug.log
```

### No Configuration Required

```gradle
openapiModelgen {
    // No logging configuration needed - just works!
    defaults {
        outputDir "build/generated-sources"
        // ... your normal configuration
    }
    specs {
        pets {
            inputSpec "specs/pets.yaml"
            // ... your normal configuration
        }
    }
}
```

## Log Levels Explained

### Standard Gradle Flags

| Gradle Flag | Log Level | Use Case | Plugin Output |
|-------------|-----------|----------|---------------|
| `--quiet` | ERROR | CI/CD, minimal output | Only critical errors |
| _(default)_ | INFO | Normal development | Standard progress messages |
| `--info` | INFO | Verbose development | Same as default |
| `--debug` | DEBUG | Troubleshooting | Detailed template processing |

### Console Output Examples

**Normal Build (`./gradlew generatePets`):**
```
> Task :generatePets
Generated 15 model classes for spec 'pets'

BUILD SUCCESSFUL in 3s
```

**Debug Build (`./gradlew generatePets --debug`):**
```
> Task :generatePets
Resolved template sources for 'pets': [user-templates, openapi-generator]
Processing template: pojo.mustache
Applied 3 customizations to template: pojo.mustache
Generated 15 model classes for spec 'pets'

BUILD SUCCESSFUL in 3s
```

## Rich File Logging (Always Available)

Regardless of console output level, **comprehensive debug information** is always written to:

```
build/logs/openapi-modelgen-debug.log
```

### File Content Examples

```log
2025-01-15 14:30:45.123 [INFO] [spec:pets|template:pojo.mustache] TemplateResolver - Processing template
2025-01-15 14:30:45.456 [DEBUG] [spec:pets|template:pojo.mustache] CustomizationEngine - Applied replacement: 'class' → 'public class'
2025-01-15 14:30:45.789 [DEBUG] [spec:pets] TaskConfiguration - Generated 15 model classes
2025-01-15 14:30:46.012 [INFO] [spec:orders] TemplateResolver - Processing template
```

### Per-Spec Analysis

Use standard Unix tools to analyze specific specs:

```bash
# Debug only the 'pets' spec
grep "\[spec:pets\]" build/logs/openapi-modelgen-debug.log

# Compare working vs broken spec
grep "\[spec:workingSpec\]" build/logs/openapi-modelgen-debug.log > working.log
grep "\[spec:brokenSpec\]" build/logs/openapi-modelgen-debug.log > broken.log
diff working.log broken.log

# Timeline analysis for a specific spec
grep "\[spec:problemSpec\]" build/logs/openapi-modelgen-debug.log | head -50

# Find template-specific issues
grep "\[spec:pets.*template:pojo\.mustache\]" build/logs/openapi-modelgen-debug.log

# Get error context
grep -B 5 -A 5 "ERROR.*pets" build/logs/openapi-modelgen-debug.log
```

## Programming with PluginLogger

### Basic Usage (Standard SLF4J)

```java
import org.slf4j.Logger;
import com.guidedbyte.openapi.modelgen.util.PluginLoggerFactory;

public class MyService {
    private static final Logger logger = PluginLoggerFactory.getLogger(MyService.class);

    public void processTemplate(String template) {
        // Standard SLF4J - respects Gradle log level for console
        // Always logs to rich file regardless of console level
        logger.info("Processing template: {}", template);
        logger.debug("Template size: {} characters", template.length());
    }
}
```

### Enhanced Features

```java
import com.guidedbyte.openapi.modelgen.util.PluginLogger;
import com.guidedbyte.openapi.modelgen.services.LoggingContext;

public class AdvancedService {
    private static final PluginLogger logger = (PluginLogger) PluginLoggerFactory.getLogger(AdvancedService.class);

    public void processSpec(String specName) {
        // Set MDC context for rich file filtering
        LoggingContext.setContext(specName, "pojo.mustache");
        LoggingContext.setComponent("TemplateProcessor");

        // Standard logging
        logger.info("Processing spec: {}", specName);

        // Lazy evaluation for expensive operations
        logger.debug("Analysis: {}", () -> performExpensiveAnalysis());

        // Conditional execution
        logger.ifDebug(() -> {
            String report = generateDetailedReport();
            logger.debug("Report: {}", report);
        });

        // Smart diagnostics
        logger.customizationDiagnostic("pattern_match", "Pattern '{}' matched", pattern);
        logger.performanceMetric("timing", "Processing took {}ms", duration);

        // Clean up context
        LoggingContext.clear();
    }
}
```

## Troubleshooting Workflows

### Problem: Template Customizations Not Working

**Step 1: Enable debug output**
```bash
./gradlew generatePets --debug
```

**Step 2: Check rich file for details**
```bash
grep "\[spec:pets.*template:" build/logs/openapi-modelgen-debug.log
```

**Step 3: Look for specific issues**
```bash
# Check template resolution
grep "Template.*resolved" build/logs/openapi-modelgen-debug.log

# Check customization application
grep "Applied.*customization" build/logs/openapi-modelgen-debug.log

# Check for errors
grep "ERROR\|WARN" build/logs/openapi-modelgen-debug.log
```

### Problem: Slow Build Performance

**Step 1: Run with timing**
```bash
./gradlew generateAllModels --debug --profile
```

**Step 2: Analyze performance metrics**
```bash
grep "BUILD_PERFORMANCE\|cache_hit_rate\|parallel_efficiency" build/logs/openapi-modelgen-debug.log
```

### Problem: Missing Generated Files

**Step 1: Check console for obvious errors**
```bash
./gradlew generatePets
```

**Step 2: Check rich file for detailed diagnosis**
```bash
grep -B 5 -A 5 "Generated.*files\|ERROR\|Failed" build/logs/openapi-modelgen-debug.log
```

## Rich File Analysis Examples

### Template Processing Analysis
```bash
# See full template processing flow
grep "\[spec:pets\].*template" build/logs/openapi-modelgen-debug.log | head -20

# Check which templates were resolved
grep "resolved.*template" build/logs/openapi-modelgen-debug.log

# See customization details
grep "customization\|replacement\|insertion" build/logs/openapi-modelgen-debug.log
```

### Performance Analysis
```bash
# Cache performance
grep "cache.*hit\|cache.*miss" build/logs/openapi-modelgen-debug.log

# Timing analysis
grep "took.*ms\|completed.*in" build/logs/openapi-modelgen-debug.log

# Parallel processing efficiency
grep "parallel\|concurrent" build/logs/openapi-modelgen-debug.log
```

### Cross-Spec Comparison
```bash
# Compare specs side by side
for spec in pets orders inventory; do
    echo "=== $spec ==="
    grep "\[spec:$spec\]" build/logs/openapi-modelgen-debug.log | wc -l
done

# Find common patterns across specs
grep "\[spec:" build/logs/openapi-modelgen-debug.log | cut -d']' -f3- | sort | uniq -c | sort -nr
```

## Legacy Migration

If you had complex logging configuration before, **remove it** - the plugin now works with standard Gradle conventions:

**Before (Complex):**
```gradle
openapiModelgen {
    debug true  // Remove this
    defaults {
        debug false  // Remove this
    }
    specs {
        pets { debug true }  // Remove this
    }
}
```

**After (Simple):**
```gradle
openapiModelgen {
    // No logging config needed!
    defaults {
        outputDir "build/generated-sources"
    }
    specs {
        pets {
            inputSpec "specs/pets.yaml"
        }
    }
}
```

## Performance Impact

- **Console Logging**: Follows standard Gradle overhead (minimal)
- **Rich File Logging**: Low overhead (~1-2% build time)
- **Debug Analysis**: Only when using `--debug` flag

## FAQ

**Q: How do I debug only one spec?**
A: Use rich file filtering: `grep "[spec:pets]" build/logs/openapi-modelgen-debug.log`

**Q: Can I disable file logging?**
A: File logging is always enabled and has minimal overhead. Use the file for debugging!

**Q: Console output is too verbose with --debug**
A: Use normal mode for console (`./gradlew generatePets`) and analyze the rich file afterward.

**Q: Where is the debug configuration?**
A: There isn't one! Use standard Gradle flags. The plugin follows Gradle conventions.

**Q: How do I get detailed template customization info?**
A: Run with `--debug` flag and/or check rich file for full details.

## Related Documentation

- [Plugin Configuration](./configuration-reference.md)
- [Template Customization](./template-customization.md)
- [Troubleshooting Guide](./troubleshooting.md)