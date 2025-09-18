#!/bin/bash
# Build script for Android-first Checklist App

echo "Building Android-first Checklist App..."

cd android-native

echo ""
echo "1. Cleaning previous builds..."
./gradlew clean

echo ""
echo "2. Building debug APK..."
./gradlew assembleDebug

echo ""
echo "3. Build completed!"
echo "Debug APK location: app/build/outputs/apk/debug/app-debug.apk"

echo ""
echo "To install on device:"
echo "adb install -r app/build/outputs/apk/debug/app-debug.apk"