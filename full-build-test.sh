#!/bin/bash
set -e  # Exit on any error

echo "========================================"
echo "OpenAPI ModelGen - Full Build and Test"
echo "========================================"

echo
echo "[1/5] Cleaning all projects..."
/c/gradle/gradle-8.5/bin/gradle clean plugin:clean --no-daemon

echo
echo "[2/5] Building plugin with configuration cache..."
/c/gradle/gradle-8.5/bin/gradle plugin:build --configuration-cache --no-daemon

echo
echo "[3/5] Building test-app with configuration cache..."
cd test-app
/c/gradle/gradle-8.5/bin/gradle clean build --configuration-cache --no-daemon

echo
echo "[4/5] Running plugin tests with configuration cache..."
cd ..
/c/gradle/gradle-8.5/bin/gradle plugin:test --configuration-cache --no-daemon

echo
echo "[5/5] Testing code generation with configuration cache..."
cd test-app
/c/gradle/gradle-8.5/bin/gradle generateAllModels --configuration-cache --no-daemon

cd ..
echo
echo "========================================"
echo "✅ ALL BUILDS AND TESTS PASSED!"
echo "========================================"
echo
echo "Configuration cache validation completed successfully."
echo "Both plugin and test-app are compatible with Gradle's configuration cache."