#!/usr/bin/env pwsh
# Backend Startup Script for Feuerwehr Checklist App
# This script starts the FastAPI backend on the local network IP for Android debugging

param(
    [string]$Host = "",
    [int]$Port = 8000
)

Write-Host "🚒 Starting Feuerwehr Checklist Backend" -ForegroundColor Red
Write-Host ""

# Change to backend directory
$BackendDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $BackendDir
Write-Host "📁 Backend Directory: $BackendDir"

# Get local IP address if not provided
if ([string]::IsNullOrEmpty($Host)) {
    Write-Host "🌐 Detecting local IP address..." -ForegroundColor Yellow
    
    $LocalIP = Get-NetIPAddress -AddressFamily IPv4 | 
        Where-Object {$_.InterfaceAlias -notlike "*Loopback*" -and $_.IPAddress -notlike "169.254.*"} |
        Select-Object -ExpandProperty IPAddress -First 1
    
    if ([string]::IsNullOrEmpty($LocalIP)) {
        Write-Host "❌ Could not detect local IP, using 127.0.0.1" -ForegroundColor Red
        $LocalIP = "127.0.0.1"
    }
    
    $Host = $LocalIP
}

Write-Host "🎯 Local IP detected: $Host" -ForegroundColor Green

# Set environment variables
$env:HOST = $Host
$env:PORT = $Port.ToString()
$env:CORS_ORIGINS = "http://localhost,http://127.0.0.1,http://${Host}:3000,http://localhost:3000,http://127.0.0.1:3000"

Write-Host ""
Write-Host "🔧 Configuration:" -ForegroundColor Cyan
Write-Host "   - Host: $Host"
Write-Host "   - Port: $Port"
Write-Host "   - CORS Origins: $($env:CORS_ORIGINS)"
Write-Host ""

# Check if virtual environment exists
if (-not (Test-Path ".venv")) {
    Write-Host "⚠️  Virtual environment not found. Creating one..." -ForegroundColor Yellow
    python -m venv .venv
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Failed to create virtual environment" -ForegroundColor Red
        exit 1
    }
}

# Activate virtual environment
Write-Host "🔄 Activating virtual environment..." -ForegroundColor Yellow

if ($IsWindows -or ($env:OS -eq "Windows_NT")) {
    & ".venv\Scripts\Activate.ps1"
} else {
    & ".venv/bin/Activate.ps1"
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to activate virtual environment" -ForegroundColor Red
    exit 1
}

# Install/upgrade dependencies
Write-Host "📦 Installing/updating dependencies..." -ForegroundColor Yellow
pip install -r requirements.txt
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to install dependencies" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🚀 Starting FastAPI server on ${Host}:${Port}" -ForegroundColor Green
Write-Host ""
Write-Host "📱 For Android emulator, use this URL in your app:" -ForegroundColor Cyan
Write-Host "   http://${Host}:${Port}"
Write-Host ""
Write-Host "💻 For local browser testing:" -ForegroundColor Cyan
Write-Host "   http://localhost:${Port}"
Write-Host "   http://127.0.0.1:${Port}"
Write-Host ""
Write-Host "📊 API Documentation will be available at:" -ForegroundColor Cyan
Write-Host "   http://${Host}:${Port}/docs"
Write-Host "   http://localhost:${Port}/docs"
Write-Host ""
Write-Host "⏹️  Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host ""

# Start the server
uvicorn app.main:app --host $Host --port $Port --reload