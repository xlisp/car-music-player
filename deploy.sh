#!/bin/bash
set -e

DEVICE="u0_a84@192.168.201.58"
PORT=8022
PACKAGE="com.carlauncher.musicplayer"
REMOTE_PATH="/sdcard/music-player.apk"

cd "$(dirname "$0")"

# 1. Build debug APK
echo "=== Building debug APK ==="
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    echo "Build failed!"
    exit 1
fi
echo "APK size: $(du -h "$APK_PATH" | cut -f1)"

# 2. Push APK to device
echo "=== Pushing APK to device ==="
scp -P $PORT "$APK_PATH" "$DEVICE":~/app-debug.apk
ssh -p $PORT "$DEVICE" "cp ~/app-debug.apk $REMOTE_PATH"

# 3. Uninstall old version (ignore if not installed)
echo "=== Uninstalling old version ==="
ssh -p $PORT "$DEVICE" "pm uninstall --user 0 $PACKAGE 2>/dev/null || true"

# 4. Launch system installer
echo "=== Installing ==="
ssh -p $PORT "$DEVICE" "am start -a android.intent.action.VIEW -d 'file://$REMOTE_PATH' -t application/vnd.android.package-archive"

echo ""
echo "=== Done! Check device screen to confirm installation ==="
