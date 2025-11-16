#!/bin/bash
# Build and test Stone Launcher - TICKET_002

set -e

echo "======================================"
echo "Stone Launcher - Build & Test Script"
echo "TICKET_002: Native Kotlin Launcher UI"
echo "======================================"
echo ""

# Change to script directory
cd "$(dirname "$0")"

# Check if Android Studio is available
if [ ! -d "/Applications/Android Studio.app" ]; then
    echo "ERROR: Android Studio not found at /Applications/Android Studio.app"
    echo "Please install Android Studio first."
    exit 1
fi

# Check if gradlew exists
if [ ! -f "gradlew" ]; then
    echo "WARNING: gradlew not found. Opening project in Android Studio..."
    echo "Please use Android Studio to build the project initially."
    open -a "Android Studio" .
    echo ""
    echo "After Gradle sync completes in Android Studio:"
    echo "  1. Click the green Run button (▶)"
    echo "  2. Select an emulator or connected device"
    echo "  3. App will build and install automatically"
    exit 0
fi

# Make gradlew executable
chmod +x gradlew

# Set JAVA_HOME to Android Studio's bundled JDK
if [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    echo "Using Android Studio's JDK: $JAVA_HOME"
elif [ -d "/Applications/Android Studio.app/Contents/jre/Contents/Home" ]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jre/Contents/Home"
    echo "Using Android Studio's JRE: $JAVA_HOME"
else
    echo "ERROR: Could not find Android Studio's JDK/JRE"
    exit 1
fi

echo "Java version:"
"$JAVA_HOME/bin/java" -version
echo ""

echo "Cleaning Gradle caches to avoid JDK compatibility issues..."
echo ""

# Clear Gradle caches
./gradlew clean --no-daemon

# Clear build directories
rm -rf .gradle
rm -rf app/build
rm -rf build

# Clear Gradle cache (helps with JDK compatibility issues)
rm -rf ~/.gradle/caches/

echo "Building Stone Launcher..."
echo ""

# Build debug APK with proper error handling
if ! ./gradlew assembleDebug --no-daemon --stacktrace; then
    echo ""
    echo "======================================"
    echo "BUILD FAILED"
    echo "======================================"
    echo ""
    echo "Common fixes:"
    echo "  1. Check Java version: java -version"
    echo "  2. Ensure JAVA_HOME is set correctly"
    echo "  3. Try opening in Android Studio and syncing Gradle"
    echo "  4. Check build.gradle and gradle-wrapper.properties versions match"
    echo ""
    exit 1
fi

echo ""
echo "======================================"
echo "Build successful!"
echo "======================================"
echo ""
echo "APK location:"
echo "  app/build/outputs/apk/debug/app-debug.apk"
echo ""

# Check if device is connected
if adb devices | grep -q "device$"; then
    echo "Android device detected!"
    echo ""
    read -p "Install on connected device? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Installing..."
        adb install -r app/build/outputs/apk/debug/app-debug.apk
        echo ""
        echo "Starting Stone Launcher..."
        adb shell am start -n com.stonelauncher/.MainActivity
        echo ""
        echo "Monitoring logs (Ctrl+C to stop):"
        adb logcat -s MainActivity:D StoneApiReceiver:D
    fi
else
    echo "No Android device connected."
    echo ""
    echo "To test:"
    echo "  1. Start an emulator in Android Studio"
    echo "  2. Run: adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo "  3. Run: adb shell am start -n com.stonelauncher/.MainActivity"
    echo ""
    echo "Or simply open in Android Studio and click Run ▶"
fi
