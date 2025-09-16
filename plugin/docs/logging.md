# OpenAPI Model Generator - Logging Guide

The OpenAPI Model Generator plugin provides a sophisticated logging system that combines intelligent level routing, dual output (console + file), and performance optimizations to give you exactly the information you need during development and troubleshooting.

## Key Features

- **🎯 Intelligent Routing**: Messages automatically route to appropriate levels based on plugin configuration
- **📁 Dual Output**: Console output respects your settings, while comprehensive logs are always written to file
- **⚡ Performance Optimized**: Zero overhead when log levels are disabled, with lazy evaluation support
- **🔧 Template Diagnostics**: Specialized logging for template customization development
- **📊 Build Progress**: Real-time progress tracking with time estimation
- **🐛 Smart Diagnostics**: Automatic troubleshooting suggestions and detailed analysis

## Quick Start

### Basic Configuration

```gradle
openapiModelgen {
    // Set global log level for the plugin
    logLevel = "DEBUG"  // ERROR, WARN, INFO, DEBUG, TRACE

    // Legacy debug flag still supported
    debug = true  // Equivalent to logLevel = "DEBUG"

    defaults {
        // ... other configuration
    }
}
```

### Command Line Override

```bash
# Via project properties
./gradlew generateModels -Popenapi.logLevel=TRACE

# Via environment variable
export OPENAPI_LOG_LEVEL=DEBUG
./gradlew generateModels
```

## Log Levels Explained

### ERROR - Critical Issues Only
**Use for**: CI/CD pipelines, production builds where minimal output is desired

**Shows**:
- Build failures and critical errors
- Configuration validation errors
- Fatal exceptions that stop the build

**Example Output**:
```
[ERROR] Failed to process OpenAPI spec 'pets': File not found
[ERROR] Invalid OpenAPI specification: Missing required 'paths' field
```

### WARN - Errors and Warnings
**Use for**: Production builds where you want to see potential issues

**Shows**: ERROR +
- Configuration warnings
- Deprecated usage notifications
- Optimization suggestions
- Non-fatal issues

**Example Output**:
```
[WARN] Template customization file has syntax errors
[WARN] Using deprecated 'modelNamePrefix' option
[WARN] More operations were skipped (5) than applied (2)
```

### INFO - Standard Development
**Use for**: Regular development, standard builds

**Shows**: WARN +
- Spec processing progress
- Build completion summaries
- Cache performance statistics
- File generation counts
- Template customization summaries

**Example Output**:
```
[INFO] Processing OpenAPI spec 'pets'
[INFO] Generated 23 model files for spec 'pets'
[INFO] Build completed successfully in 5.2s
[INFO] BUILD_PERFORMANCE: cache_hit_rate=78% parallel_efficiency=85%
```

### DEBUG - Template Development
**Use for**: Template customization development, configuration troubleshooting

**Shows**: INFO +
- Template resolution details
- Pattern matching results
- Applied customization operations
- Variable context information
- Cache operation details

**Example Output**:
```
[DEBUG] PATTERN_MATCH_SUCCESS: operation=replacement pattern='class {{classname}}' found_in_template=true
[DEBUG] APPLIED_OPERATIONS: operations=[replacement #0, insertion #1, replacement #2]
[DEBUG] VARIABLE_ANALYSIS: total_variables=15 project_properties=8 environment_variables=7
```

### TRACE - Deep Troubleshooting
**Use for**: Deep debugging, understanding why customizations fail

**Shows**: DEBUG +
- Pattern matching analysis with context
- Template diffs showing exact changes
- Condition evaluation step-by-step
- Line-by-line template context
- Troubleshooting suggestions

**Example Output**:
```
[TRACE] PATTERN_MATCH_1: match_number=1 line=23 matched_text='class PetDto'
[TRACE] TEMPLATE_DIFF: operation='Applied replacement #0' length_change=45 line_change=2
[TRACE] CONDITION_CHECK: type=generatorVersion constraint='>= 7.0.0' context_version='7.2.0'
[TRACE] → Template doesn't contain Mustache variables. Check if this is the correct template.
```

## Programming with PluginLogger

### Basic Usage (Standard SLF4J Interface)

```java
import org.slf4j.Logger;
import com.guidedbyte.openapi.modelgen.util.PluginLoggerFactory;

public class MyService {
    private static final Logger logger = PluginLoggerFactory.getLogger(MyService.class);

    public void processTemplate(String template) {
        // Standard SLF4J logging with intelligent routing
        logger.info("Processing template: {}", template);
        logger.debug("Template size: {} characters", template.length());
        logger.trace("Template content: {}", template);
    }
}
```

### Advanced Usage (Enhanced PluginLogger Features)

```java
import com.guidedbyte.openapi.modelgen.util.PluginLogger;
import com.guidedbyte.openapi.modelgen.util.PluginLoggerFactory;
import com.guidedbyte.openapi.modelgen.util.LogLevel;

public class AdvancedService {
    private static final PluginLogger logger = (PluginLogger) PluginLoggerFactory.getLogger(AdvancedService.class);

    public void processTemplate(String template) {
        // Lazy evaluation - expensive operation only runs when DEBUG enabled
        logger.debug("Expensive analysis: {}", () -> performComplexAnalysis(template));
        logger.trace("Full analysis: {}", () -> performFullAnalysis(template));

        // Conditional execution - block only runs when level is enabled
        logger.ifInfo(() -> {
            String summary = generateSummary(template);
            logger.info("Summary: {}", summary);
        });

        logger.ifDebug(() -> {
            String details = generateDetailedReport(template);
            logger.debug("Detailed report: {}", details);
        });

        logger.ifTrace(() -> {
            String trace = performTraceAnalysis(template);
            logger.trace("Trace analysis: {}", trace);
        });

        // Smart diagnostics - automatically routes to appropriate level
        logger.customizationDiagnostic("pattern_match", "Pattern '{}' matched at line {}", pattern, lineNumber);
        logger.performanceMetric("operation_timing", "Template processing took {}ms", duration);

        // Rich file logging utilities
        logger.section("Template Processing Phase");

        // Level checking
        if (logger.isLevelEnabled(LogLevel.DEBUG)) {
            // Expensive debug logic here
        }

        LogLevel currentLevel = logger.getCurrentLogLevel();
        logger.info("Current log level: {}", currentLevel);
    }

    private String performComplexAnalysis(String template) {
        // This expensive operation only runs when DEBUG is enabled
        return analyzeTemplateStructure(template);
    }
}
```

### Smart Diagnostics

The PluginLogger provides specialized diagnostic methods that automatically route messages to appropriate levels:

```java
// Template customization diagnostics
logger.customizationDiagnostic("pattern_match", "Pattern matched: {}", pattern);     // → TRACE
logger.customizationDiagnostic("operation_applied", "Applied replacement", op);      // → DEBUG
logger.customizationDiagnostic("summary", "Applied {} operations", count);          // → INFO

// Performance metrics
logger.performanceMetric("operation_timing", "Operation took {}ms", time);          // → DEBUG
logger.performanceMetric("build_summary", "Build completed in {}s", duration);     // → INFO
logger.performanceMetric("cache_performance", "Cache hit rate: {}%", rate);        // → INFO
```

### Conditional Execution

Use conditional execution to avoid expensive operations when logging is disabled:

```java
// Only runs when INFO level is enabled
logger.ifInfo(() -> {
    String summary = generateSummaryReport();
    logger.info("Summary: {}", summary);
});

// Only runs when DEBUG level is enabled
logger.ifDebug(() -> {
    Map<String, Object> debugInfo = collectDebugInformation();
    logger.debug("Debug info: {}", debugInfo);
});

// Only runs when TRACE level is enabled
logger.ifTrace(() -> {
    String fullAnalysis = performFullAnalysis();
    logger.trace("Full analysis: {}", fullAnalysis);
});
```

### Level Checking and Control

PluginLogger provides methods to check current log levels and control logging behavior:

```java
// Check current log level
LogLevel currentLevel = logger.getCurrentLogLevel();
logger.info("Running at log level: {}", currentLevel);

// Check if specific levels are enabled
if (logger.isLevelEnabled(LogLevel.DEBUG)) {
    // Perform expensive debug operations
    performDebugAnalysis();
}

if (logger.isLevelEnabled(LogLevel.TRACE)) {
    // Perform very expensive trace operations
    performTraceAnalysis();
}
```

### Rich File Logging Utilities

PluginLogger provides additional methods for enhanced file logging:

```java
// Add section headers to rich log files for better organization
logger.section("Template Processing Phase");
logger.info("Starting template processing");
// ... processing logic ...

logger.section("Validation Phase");
logger.info("Starting validation");
// ... validation logic ...

// Check if file logging is available
if (logger.isFileLoggingEnabled()) {
    logger.debug("File logging available at: build/logs/openapi-modelgen-debug.log");
}

// Manually close and flush file logger (usually not needed - automatic via shutdown hook)
logger.close();
```

### Lazy Evaluation

PluginLogger supports lazy evaluation for expensive log message construction:

```java
// PluginLogger lazy evaluation (requires cast for access to enhanced methods)
PluginLogger pluginLogger = (PluginLogger) logger;

// Lazy evaluation methods - expensive operations only run when level is enabled
pluginLogger.info("Summary: {}", () -> generateExpensiveSummary());
pluginLogger.debug("Debug analysis: {}", () -> performDebugAnalysis());
pluginLogger.trace("Trace analysis: {}", () -> performTraceAnalysis());

// Standard SLF4J approaches also work
logger.debug("Result: {}", () -> expensiveOperation()); // SLF4J 2.0+ feature
logger.debug("Result: {}", expensiveOperation());        // Always evaluated
```

## Integration with Gradle

The plugin automatically detects and respects Gradle's log level settings:

| Gradle Flag | Plugin Log Level | Description |
|-------------|------------------|-------------|
| `gradle --quiet` | ERROR | Only critical errors |
| `gradle --warn` | WARN | Errors and warnings |
| `gradle` (default) | INFO | Standard development output |
| `gradle --info` | INFO | Same as default |
| `gradle --debug` | DEBUG | Template development details |

### Manual Override

You can override the automatic detection:

```bash
# Project properties (gradle.properties)
openapi.logLevel=TRACE

# Command line
./gradlew generateModels -Popenapi.logLevel=DEBUG

# Environment variable
export OPENAPI_LOG_LEVEL=TRACE
./gradlew generateModels
```

## Rich File Logging

Regardless of your console log level, comprehensive debug information is **always** written to:

```
build/logs/openapi-modelgen-debug.log
```

This file contains:
- **Full context**: All log messages with complete MDC context
- **Structured data**: Machine-readable log entries for analysis
- **Performance metrics**: Detailed timing and cache statistics
- **Template diagnostics**: Complete customization workflow traces
- **Build progression**: Phase-by-phase build analysis

### Log File Format

```
2025-01-15 14:30:45.123 [INFO] [BuildProgress] [pets:pojo.mustache] - Processing template
2025-01-15 14:30:45.456 [DEBUG] [CustomizationEngine] [pets:pojo.mustache] - PATTERN_MATCH_SUCCESS: pattern='class {{classname}}'
2025-01-15 14:30:45.789 [TRACE] [TemplateProcessor] [pets:pojo.mustache] - Applied replacement: 'public class' → 'public final class'
```

## Performance Impact

| Log Level | Performance Impact | When to Use |
|-----------|-------------------|-------------|
| **ERROR/WARN** | Minimal (<1%) | CI/CD pipelines, production |
| **INFO** | Minimal (<1%) | Standard development |
| **DEBUG** | Low (2-5%) | Template customization development |
| **TRACE** | Moderate (5-10%) | Deep troubleshooting only |

### Performance Optimization Features

- **Zero overhead**: Disabled log levels have no performance cost
- **Lazy evaluation**: Expensive message construction only when needed
- **Conditional execution**: Code blocks only run when logging is enabled
- **Efficient routing**: Smart diagnostics avoid unnecessary string operations

## Legacy Compatibility

The plugin maintains backward compatibility with existing code:

```java
// Legacy debug flag checking (still works)
if (PluginState.getInstance().isDebugEnabled()) {
    // Debug logic
}

// New granular level checking
if (PluginState.getInstance().isInfoEnabled()) {
    // Info logic
}

if (PluginState.getInstance().isTraceEnabled()) {
    // Trace logic
}
```

## Troubleshooting

### Common Issues

**Q: I set `logLevel = "DEBUG"` but don't see debug messages**
- Check that Gradle's log level isn't overriding: try `./gradlew --info generateModels`
- Verify the spelling: `"DEBUG"` not `"debug"`

**Q: Too much output in console**
- Lower the console level: `logLevel = "INFO"`
- Use `gradle --quiet` for minimal output
- Remember: file logging always captures everything

**Q: Missing log file**
- Ensure `buildDir` is accessible and writable
- Check `build/logs/` directory exists
- File logging requires successful plugin initialization

**Q: Performance issues**
- Avoid `TRACE` level in production builds
- Use lazy evaluation: `logger.debug("Result: {}", () -> expensiveOperation())`
- Use conditional execution for expensive blocks

### Debug Plugin Logging Itself

```bash
# Enable plugin internal debugging
./gradlew generateModels -Popenapi.logLevel=TRACE --debug
```

This shows how the plugin's own logging system is working internally.

## Migration Guide

### From SmartLogger (Removed in v2.1)

SmartLogger functionality has been merged into PluginLogger:

```java
// OLD: SmartLogger (removed)
SmartLogger smartLogger = SmartLogger.forClass(MyClass.class);
smartLogger.normal("Processing...");
smartLogger.verbose("Details...");

// NEW: PluginLogger (enhanced)
PluginLogger logger = (PluginLogger) PluginLoggerFactory.getLogger(MyClass.class);
logger.info("Processing...");  // replaces normal()
logger.debug("Details...");    // replaces verbose()
```

### From Basic SLF4J

No changes needed! PluginLoggerFactory returns standard SLF4J Logger interface:

```java
// Works exactly the same
Logger logger = PluginLoggerFactory.getLogger(MyClass.class);
logger.info("This works unchanged");
```

Cast to PluginLogger only when you need enhanced features:

```java
// Enhanced features
((PluginLogger) logger).ifDebug(() -> expensiveOperation());
```

## Complete PluginLogger API Reference

### Standard SLF4J Methods
PluginLogger implements the complete SLF4J Logger interface with intelligent level routing:

```java
// Level checking (enhanced with plugin state awareness)
boolean isErrorEnabled()
boolean isWarnEnabled()
boolean isInfoEnabled()
boolean isDebugEnabled()
boolean isTraceEnabled()

// Logging methods (all levels: error, warn, info, debug, trace)
void error(String msg)
void error(String format, Object arg)
void error(String format, Object arg1, Object arg2)
void error(String format, Object... arguments)
void error(String msg, Throwable t)
// ... same patterns for warn, info, debug, trace

// Marker support for all levels
void error(Marker marker, String msg)
// ... etc for all levels

// Utility methods
String getName()
```

### Enhanced PluginLogger Methods

```java
// Lazy evaluation (expensive operations only when level enabled)
void info(String message, Supplier<Object> argSupplier)
void debug(String message, Supplier<Object> argSupplier)
void trace(String message, Supplier<Object> argSupplier)

// Conditional execution (code blocks only run when level enabled)
void ifInfo(Runnable action)
void ifDebug(Runnable action)
void ifTrace(Runnable action)

// Smart diagnostics (automatic level routing)
void customizationDiagnostic(String type, String message, Object... args)
void performanceMetric(String scope, String message, Object... args)

// Level management
LogLevel getCurrentLogLevel()
boolean isLevelEnabled(LogLevel level)

// Rich file logging utilities
void section(String section)           // Add section headers to file logs
boolean isFileLoggingEnabled()        // Check if file logging available
void close()                          // Manual close/flush (auto via shutdown hook)

// Access underlying logger
Logger getUnderlyingLogger()          // Get wrapped SLF4J logger
```

### Smart Diagnostic Types

For `customizationDiagnostic(String type, ...)`:

| Type | Routes To | Use Case |
|------|-----------|----------|
| `"pattern_match"` | TRACE | Pattern matching analysis |
| `"template_diff"` | TRACE | Template before/after differences |
| `"condition_evaluation"` | TRACE | Condition evaluation details |
| `"operation_applied"` | DEBUG | Applied customization operations |
| `"cache_operation"` | DEBUG | Cache hit/miss details |
| `"variable_analysis"` | DEBUG | Variable context analysis |
| `"summary"` | INFO | Customization summaries |
| `"performance"` | INFO | Performance metrics |
| `"progress"` | INFO | Progress indicators |
| _default_ | DEBUG | Fallback for unknown types |

### Performance Metric Scopes

For `performanceMetric(String scope, ...)`:

| Scope | Routes To | Use Case |
|-------|-----------|----------|
| `"build_summary"` | INFO | Overall build completion stats |
| `"completion"` | INFO | Phase completion notifications |
| `"cache_performance"` | INFO | Cache hit rates and efficiency |
| `"phase_timing"` | INFO | Major phase timing summaries |
| `"operation_timing"` | DEBUG | Individual operation timings |
| `"detailed_metrics"` | DEBUG | Detailed performance breakdowns |
| _default_ | INFO | Fallback for unknown scopes |

---

## Examples Repository

For complete working examples, see:
- [Template Customization Examples](../examples/template-customization/)
- [Performance Logging Examples](../examples/performance-logging/)
- [Build Progress Examples](../examples/build-progress/)

## Related Documentation

- [Plugin Configuration](./configuration.md)
- [Template Customization](./template-customization.md)
- [Performance Optimization](./performance.md)
- [Troubleshooting Guide](./troubleshooting.md)