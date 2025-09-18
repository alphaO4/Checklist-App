#!/bin/bash

# Development helper script for Feuerwehr Checklist Android App
# Provides common development tasks

set -e

PROJECT_DIR="$(dirname "$(dirname "$(readlink -f "$0")")")"
cd "$PROJECT_DIR"

function show_help() {
    echo "🚒 Feuerwehr Checklist Android Development Helper"
    echo ""
    echo "Usage: ./scripts/dev.sh <command>"
    echo ""
    echo "Commands:"
    echo "  build [debug|release]  - Build APK"
    echo "  install [debug|release] - Build and install on device"
    echo "  run                   - Install and run debug version"
    echo "  test                  - Run unit tests"
    echo "  test-ui               - Run UI tests (requires device)"
    echo "  clean                 - Clean build artifacts"
    echo "  lint                  - Run lint checks"
    echo "  deps                  - Show dependency tree"
    echo "  logcat                - Show app logs"
    echo "  devices               - List connected devices"
    echo "  help                  - Show this help"
}

function build_apk() {
    local build_type=${1:-debug}
    echo "🔨 Building $build_type APK..."
    ./scripts/build.sh "$build_type"
}

function install_apk() {
    local build_type=${1:-debug}
    echo "📱 Building and installing $build_type APK..."
    
    # Build first
    build_apk "$build_type"
    
    # Install
    if [ "$build_type" = "release" ]; then
        if [ -f "app/build/outputs/apk/release/FeuerwehrChecklist-release.apk" ]; then
            adb install -r "app/build/outputs/apk/release/FeuerwehrChecklist-release.apk"
        else
            adb install -r "app/build/outputs/apk/release/app-release-unsigned.apk"
        fi
    else
        adb install -r "app/build/outputs/apk/debug/app-debug.apk"
    fi
    
    echo "✅ APK installed successfully!"
}

function run_app() {
    echo "🚀 Installing and running debug app..."
    install_apk debug
    
    echo "🎯 Starting app..."
    adb shell am start -n "com.feuerwehr.checklist/.MainActivity"
    
    echo "📱 App started! Check your device."
}

function run_tests() {
    echo "🧪 Running unit tests..."
    ./gradlew test
    echo "✅ Unit tests completed!"
}

function run_ui_tests() {
    echo "🧪 Running UI tests..."
    echo "📱 Make sure a device is connected..."
    adb devices
    ./gradlew connectedAndroidTest
    echo "✅ UI tests completed!"
}

function clean_build() {
    echo "🧹 Cleaning build artifacts..."
    ./gradlew clean
    rm -rf build/
    rm -rf app/build/
    echo "✅ Clean completed!"
}

function run_lint() {
    echo "🔍 Running lint checks..."
    ./gradlew lint
    echo "📄 Lint report: app/build/reports/lint-results.html"
}

function show_deps() {
    echo "📦 Dependency tree:"
    ./gradlew dependencies
}

function show_logcat() {
    echo "📱 Showing app logs (Ctrl+C to stop)..."
    echo "🔍 Filtering for: ChecklistApp, WebViewClient, OfflineManager"
    adb logcat -s ChecklistApp:D WebViewClient:D OfflineManager:D NetworkUtils:D
}

function list_devices() {
    echo "📱 Connected devices:"
    adb devices -l
}

# Main command handling
case "${1:-help}" in
    "build")
        build_apk "$2"
        ;;
    "install")
        install_apk "$2"
        ;;
    "run")
        run_app
        ;;
    "test")
        run_tests
        ;;
    "test-ui")
        run_ui_tests
        ;;
    "clean")
        clean_build
        ;;
    "lint")
        run_lint
        ;;
    "deps")
        show_deps
        ;;
    "logcat")
        show_logcat
        ;;
    "devices")
        list_devices
        ;;
    "help"|*)
        show_help
        ;;
esac