@echo off
REM Backend Startup Script for Feuerwehr Checklist App
REM This script starts the FastAPI backend on the local network IP for Android debugging

setlocal enabledelayedexpansion

echo 🚒 Starting Feuerwehr Checklist Backend
echo.

REM Get the current directory
set BACKEND_DIR=%~dp0
cd /d "%BACKEND_DIR%"

echo 📁 Backend Directory: %BACKEND_DIR%

REM Get local IP address
echo 🌐 Detecting local IP address...
for /f "tokens=2 delims=:" %%i in ('ipconfig ^| findstr /C:"IPv4 Address"') do (
    set "ip=%%i"
    set "ip=!ip: =!"
    if not "!ip:~0,3!"=="127" if not "!ip:~0,7!"=="169.254" (
        set LOCAL_IP=!ip!
        goto :found_ip
    )
)

:found_ip
if not defined LOCAL_IP (
    echo ❌ Could not detect local IP, using 127.0.0.1
    set LOCAL_IP=127.0.0.1
)

echo 🎯 Local IP detected: %LOCAL_IP%

REM Set environment variables for debugging
set HOST=%LOCAL_IP%
set PORT=8000
set CORS_ORIGINS=http://localhost,http://127.0.0.1,http://%LOCAL_IP%:3000,http://localhost:3000,http://127.0.0.1:3000

echo.
echo 🔧 Configuration:
echo   - Host: %HOST%
echo   - Port: %PORT%
echo   - CORS Origins: %CORS_ORIGINS%
echo.

REM Check if virtual environment exists
if not exist ".venv\" (
    echo ⚠️  Virtual environment not found. Creating one...
    python -m venv .venv
    if errorlevel 1 (
        echo ❌ Failed to create virtual environment
        pause
        exit /b 1
    )
)

REM Activate virtual environment
echo 🔄 Activating virtual environment...
call .venv\Scripts\activate.bat
if errorlevel 1 (
    echo ❌ Failed to activate virtual environment
    pause
    exit /b 1
)

REM Install/upgrade dependencies
echo 📦 Installing/updating dependencies...
pip install -r requirements.txt
if errorlevel 1 (
    echo ❌ Failed to install dependencies
    pause
    exit /b 1
)

echo.
echo 🚀 Starting FastAPI server on %HOST%:%PORT%
echo.
echo 📱 For Android emulator, use this URL in your app:
echo    http://%LOCAL_IP%:8000
echo.
echo 💻 For local browser testing:
echo    http://localhost:8000
echo    http://127.0.0.1:8000
echo.
echo 📊 API Documentation will be available at:
echo    http://%LOCAL_IP%:8000/docs
echo    http://localhost:8000/docs
echo.
echo ⏹️  Press Ctrl+C to stop the server
echo.

REM Start the server
uvicorn app.main:app --host %HOST% --port %PORT% --reload

pause