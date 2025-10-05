#!/bin/bash
# Android Build Validation Script
echo "🚀 Testing Android Build System..."

echo "📱 Testing debug build..."
./gradlew assembleDebug --no-daemon --quiet

if [ $? -eq 0 ]; then
    echo "✅ Debug build successful"
else
    echo "❌ Debug build failed"
    exit 1
fi

echo "📱 Testing release build..."
./gradlew assembleRelease --no-daemon --quiet

if [ $? -eq 0 ]; then
    echo "✅ Release build successful"
else
    echo "❌ Release build failed"
    exit 1
fi

echo "🔍 Testing lint checks..."
./gradlew lintDebug --no-daemon --quiet

if [ $? -eq 0 ]; then
    echo "✅ Lint checks passed"
else
    echo "❌ Lint checks failed"
    exit 1
fi

echo "✨ All Android build tests passed!"