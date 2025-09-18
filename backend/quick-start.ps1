#!/usr/bin/env pwsh
# Quick Backend Start Script
# Starts the backend with your current local IP for Android debugging

Write-Host "🚒 Quick Backend Start" -ForegroundColor Red
Write-Host ""

# Navigate to backend directory
$BackendPath = "E:\Github\Checklist-App\backend"
Set-Location $BackendPath

# Start the backend using the full script
& ".\start-backend.ps1"