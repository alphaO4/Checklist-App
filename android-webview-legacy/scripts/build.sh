#!/bin/bash

# Build script for Feuerwehr Checklist Android App
# Usage: ./scripts/build.sh [debug|release]

set -e

BUILD_TYPE=${1:-debug}
PROJECT_DIR="$(dirname "$(dirname "$(readlink -f "$0")")")"
APP_NAME="FeuerwehrChecklist"

echo "🚒 Building Feuerwehr Checklist Android App"
echo "Build Type: $BUILD_TYPE"
echo "Project Directory: $PROJECT_DIR"

cd "$PROJECT_DIR"

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Build the APK
if [ "$BUILD_TYPE" = "release" ]; then
    echo "🔨 Building release APK..."
    ./gradlew assembleRelease
    
    APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
    if [ -f "$APK_PATH" ]; then
        echo "✅ Release APK built successfully!"
        echo "📦 APK Location: $APK_PATH"
        
        # Check if keystore exists for signing
        if [ -f "checklist-release-key.jks" ]; then
            echo "🔐 Signing APK..."
            jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
                -keystore checklist-release-key.jks \
                "$APK_PATH" checklist
            
            # Align APK
            echo "🎯 Aligning APK..."
            zipalign -v 4 "$APK_PATH" "app/build/outputs/apk/release/${APP_NAME}-release.apk"
            echo "✅ Signed and aligned APK ready!"
        else
            echo "⚠️ No keystore found. APK is unsigned."
            echo "💡 Create keystore with: keytool -genkey -v -keystore checklist-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias checklist"
        fi
    else
        echo "❌ Release build failed!"
        exit 1
    fi
else
    echo "🔨 Building debug APK..."
    ./gradlew assembleDebug
    
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        echo "✅ Debug APK built successfully!"
        echo "📦 APK Location: $APK_PATH"
    else
        echo "❌ Debug build failed!"
        exit 1
    fi
fi

# Show APK info
echo ""
echo "📊 APK Information:"
aapt dump badging "$APK_PATH" | grep -E "package|application-label|sdkVersion|targetSdkVersion"

echo ""
echo "🚀 Build completed successfully!"
echo "💡 To install on device: adb install -r \"$APK_PATH\""