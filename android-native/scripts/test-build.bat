@echo off
REM Android Build Validation Script for Windows
echo 🚀 Testing Android Build System...

echo 📱 Testing debug build...
call gradlew assembleDebug --no-daemon --quiet
if %ERRORLEVEL% neq 0 (
    echo ❌ Debug build failed
    exit /b 1
)
echo ✅ Debug build successful

echo 📱 Testing release build...
call gradlew assembleRelease --no-daemon --quiet
if %ERRORLEVEL% neq 0 (
    echo ❌ Release build failed
    exit /b 1
)
echo ✅ Release build successful

echo 🔍 Testing lint checks...
call gradlew lintDebug --no-daemon --quiet
if %ERRORLEVEL% neq 0 (
    echo ❌ Lint checks failed
    exit /b 1
)
echo ✅ Lint checks passed

echo ✨ All Android build tests passed!