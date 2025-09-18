@echo off
REM Complete build and deployment script for Android-first Checklist App

echo ========================================
echo Android-First Checklist App Deployment
echo ========================================

REM Check prerequisites
echo Checking prerequisites...

where adb >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: ADB not found. Please install Android SDK Platform-Tools.
    pause
    exit /b 1
)

where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java not found. Please install JDK 11 or newer.
    pause
    exit /b 1
)

echo Prerequisites OK!
echo.

REM Start backend if not running
echo 1. Starting backend server...
cd backend
start "Backend Server" cmd /k "python -m uvicorn app.main:app --host 10.20.1.108 --port 8000 --reload"
echo Backend starting at http://10.20.1.108:8000
echo.

REM Wait for backend to start
echo Waiting 5 seconds for backend to initialize...
timeout /t 5 /nobreak >nul

REM Build Android app
echo 2. Building Android application...
cd ..\android-native
call gradlew clean assembleDebug

if %ERRORLEVEL% neq 0 (
    echo ERROR: Android build failed!
    pause
    exit /b 1
)

echo Android build successful!
echo.

REM Check for connected devices
echo 3. Checking for connected Android devices...
adb devices

echo.
echo Available commands:
echo - Install on device: adb install -r app\build\outputs\apk\debug\app-debug.apk
echo - View logs: adb logcat -s "ChecklistApp"
echo - Uninstall: adb uninstall com.feuerwehr.checklist.debug

echo.
echo Build completed successfully!
echo APK location: android-native\app\build\outputs\apk\debug\app-debug.apk
echo Backend running at: http://10.20.1.108:8000

pause