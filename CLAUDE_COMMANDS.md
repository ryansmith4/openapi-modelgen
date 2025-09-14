# Claude Commands Reference

## Custom Commands for OpenAPI ModelGen Project

### `/full-build-test` (Simulated)
**Purpose:** Complete clean build, test, and configuration cache validation

**Command:**
```bash
/c/gradle/gradle-8.5/bin/gradle plugin:clean --no-daemon && /c/gradle/gradle-8.5/bin/gradle plugin:build --configuration-cache --no-daemon && cd test-app && /c/gradle/gradle-8.5/bin/gradle clean build --configuration-cache --no-daemon && cd .. && /c/gradle/gradle-8.5/bin/gradle plugin:test --configuration-cache --no-daemon && cd test-app && /c/gradle/gradle-8.5/bin/gradle generateAllModels --configuration-cache --no-daemon && cd .. && echo "✅ FULL BUILD COMPLETE: Clean build, all tests passed, configuration cache validated"
```

**What it does:**
1. Clean plugin build artifacts
2. Build plugin with configuration cache validation
3. Clean build test-app with configuration cache validation  
4. Run all plugin tests (247+ test methods) with configuration cache
5. Test code generation functionality with configuration cache
6. Confirm successful completion

**Usage:** Copy and paste the command when you need a full build cycle.

### Other Useful Commands

#### Quick Plugin Test
```bash
/c/gradle/gradle-8.5/bin/gradle plugin:test --configuration-cache --no-daemon
```

#### SpotBugs Check
```bash
/c/gradle/gradle-8.5/bin/gradle plugin:spotbugsMain --no-daemon
```

#### Generate Models Only
```bash
cd test-app && /c/gradle/gradle-8.5/bin/gradle generateAllModels --configuration-cache --no-daemon && cd ..
```

#### Clean Everything
```bash
/c/gradle/gradle-8.5/bin/gradle plugin:clean --no-daemon && cd test-app && /c/gradle/gradle-8.5/bin/gradle clean --no-daemon && cd ..
```