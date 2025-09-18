@echo off
REM Build script for Feuerwehr Checklist Android App (Windows)
REM Usage: scripts\build.bat [debug|release]

setlocal enabledelayedexpansion

set BUILD_TYPE=%1
if "%BUILD_TYPE%"=="" set BUILD_TYPE=debug

set PROJECT_DIR=%~dp0..
set APP_NAME=FeuerwehrChecklist

echo 🚒 Building Feuerwehr Checklist Android App
echo Build Type: %BUILD_TYPE%
echo Project Directory: %PROJECT_DIR%

cd /d "%PROJECT_DIR%"

REM Clean previous builds
echo 🧹 Cleaning previous builds...
call gradlew.bat clean
if errorlevel 1 goto :error

REM Build the APK
if "%BUILD_TYPE%"=="release" (
    echo 🔨 Building release APK...
    call gradlew.bat assembleRelease
    if errorlevel 1 goto :error
    
    set APK_PATH=app\build\outputs\apk\release\app-release-unsigned.apk
    if exist "!APK_PATH!" (
        echo ✅ Release APK built successfully!
        echo 📦 APK Location: !APK_PATH!
        
        REM Check if keystore exists for signing
        if exist "checklist-release-key.jks" (
            echo 🔐 Signing APK...
            jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 ^
                -keystore checklist-release-key.jks ^
                "!APK_PATH!" checklist
                
            REM Align APK
            echo 🎯 Aligning APK...
            zipalign -v 4 "!APK_PATH!" "app\build\outputs\apk\release\%APP_NAME%-release.apk"
            echo ✅ Signed and aligned APK ready!
        ) else (
            echo ⚠️ No keystore found. APK is unsigned.
            echo 💡 Create keystore with: keytool -genkey -v -keystore checklist-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias checklist
        )
    ) else (
        echo ❌ Release build failed!
        goto :error
    )
) else (
    echo 🔨 Building debug APK...
    call gradlew.bat assembleDebug
    if errorlevel 1 goto :error
    
    set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
    if exist "!APK_PATH!" (
        echo ✅ Debug APK built successfully!
        echo 📦 APK Location: !APK_PATH!
    ) else (
        echo ❌ Debug build failed!
        goto :error
    )
)

echo.
echo 🚀 Build completed successfully!
echo 💡 To install on device: adb install -r "!APK_PATH!"
goto :end

:error
echo ❌ Build failed with error!
exit /b 1

:end
endlocal