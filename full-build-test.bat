@echo off
echo ========================================
echo OpenAPI ModelGen - Full Build and Test
echo ========================================

echo.
echo [1/5] Cleaning all projects...
call /c/gradle/gradle-8.5/bin/gradle clean plugin:clean --no-daemon
if %ERRORLEVEL% neq 0 (
    echo ERROR: Clean failed
    exit /b 1
)

echo.
echo [2/5] Building plugin with configuration cache...
call /c/gradle/gradle-8.5/bin/gradle plugin:build --configuration-cache --no-daemon
if %ERRORLEVEL% neq 0 (
    echo ERROR: Plugin build failed
    exit /b 1
)

echo.
echo [3/5] Building test-app with configuration cache...
cd test-app
call /c/gradle/gradle-8.5/bin/gradle clean build --configuration-cache --no-daemon
if %ERRORLEVEL% neq 0 (
    echo ERROR: Test-app build failed
    exit /b 1
)

echo.
echo [4/5] Running plugin tests with configuration cache...
cd ..
call /c/gradle/gradle-8.5/bin/gradle plugin:test --configuration-cache --no-daemon
if %ERRORLEVEL% neq 0 (
    echo ERROR: Plugin tests failed
    exit /b 1
)

echo.
echo [5/5] Testing code generation with configuration cache...
cd test-app
call /c/gradle/gradle-8.5/bin/gradle generateAllModels --configuration-cache --no-daemon
if %ERRORLEVEL% neq 0 (
    echo ERROR: Code generation failed
    exit /b 1
)

cd ..
echo.
echo ========================================
echo ✅ ALL BUILDS AND TESTS PASSED!
echo ========================================
echo.
echo Configuration cache validation completed successfully.
echo Both plugin and test-app are compatible with Gradle's configuration cache.