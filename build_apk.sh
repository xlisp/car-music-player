#!/bin/bash
set -e

cd "$(dirname "$0")"

export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$PATH"

echo "=== Clean ==="
./gradlew clean

echo "=== Build Debug APK ==="
./gradlew assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    echo ""
    echo "=== Build Success ==="
    echo "APK: $(pwd)/$APK_PATH"
    echo "Size: $(du -h "$APK_PATH" | cut -f1)"
else
    echo ""
    echo "=== Build Failed ==="
    exit 1
fi
