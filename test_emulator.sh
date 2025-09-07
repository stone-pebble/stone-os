#!/bin/bash

# StoneOS Emulator Testing Script

echo "🚀 StoneOS Emulator Testing"
echo "=========================="

# Set Android SDK path (will be updated after Android Studio setup)
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$PATH"

# Function to check if emulator is available
check_emulator() {
    if ! command -v emulator &> /dev/null; then
        echo "❌ Emulator not found. Please complete Android Studio setup first."
        echo ""
        echo "To set up:"
        echo "1. Open Android Studio"
        echo "2. Go to Tools > AVD Manager"
        echo "3. Create a Pixel 8a virtual device with API 34+"
        echo "4. Name it: Pixel_8a_API_34"
        exit 1
    fi
}

# Function to list AVDs
list_avds() {
    echo "📱 Available AVDs:"
    emulator -list-avds
}

# Function to start emulator
start_emulator() {
    local AVD_NAME="${1:-Pixel_8a_API_34}"
    
    echo "Starting emulator: $AVD_NAME"
    echo "This will take a few minutes..."
    
    # Start emulator with writable system partition
    emulator -avd "$AVD_NAME" -writable-system -no-snapshot-load &
    
    # Wait for emulator to boot
    echo "Waiting for emulator to boot..."
    adb wait-for-device
    
    # Wait for boot to complete
    while [[ $(adb shell getprop sys.boot_completed 2>/dev/null) != "1" ]]; do
        sleep 2
        echo -n "."
    done
    echo ""
    echo "✅ Emulator ready!"
}

# Function to install SystemUI
install_systemui() {
    local APK_PATH="${1:-builds/manual-20250906/StoneOS_SystemUI.apk}"
    
    if [ ! -f "$APK_PATH" ]; then
        echo "❌ APK not found: $APK_PATH"
        exit 1
    fi
    
    echo "📦 Installing SystemUI..."
    
    # Enable root and remount
    adb root
    sleep 2
    adb remount
    
    # Push SystemUI to system partition
    echo "Pushing SystemUI.apk to system..."
    adb push "$APK_PATH" /system/system_ext/priv-app/SystemUI/SystemUI.apk
    
    # Set correct permissions
    adb shell chmod 644 /system/system_ext/priv-app/SystemUI/SystemUI.apk
    
    # Restart SystemUI
    echo "Restarting SystemUI..."
    adb shell pkill -f com.android.systemui || true
    
    echo "✅ SystemUI installed!"
}

# Function to monitor logs
monitor_logs() {
    echo "📋 Monitoring StoneOS logs..."
    echo "Looking for Stone panel activity..."
    echo "Press Ctrl+C to stop"
    echo ""
    adb logcat -c  # Clear existing logs
    adb logcat | grep -E "StoneOS|StonePanel|StoneIcon|SystemUI" --color=always
}

# Function to test Stone panel
test_stone_panel() {
    echo "🧪 Testing Stone Panel..."
    echo ""
    echo "Instructions:"
    echo "1. Swipe up from the bottom of the screen"
    echo "2. Look for the Stone panel (should take 1/3 of screen)"
    echo "3. Check logs below for any errors"
    echo ""
    
    # Simulate swipe up gesture
    echo "Simulating swipe up..."
    adb shell input swipe 540 2000 540 1000 300
    
    # Monitor logs
    monitor_logs
}

# Main menu
main() {
    check_emulator
    
    echo ""
    echo "What would you like to do?"
    echo "1. List available AVDs"
    echo "2. Start emulator"
    echo "3. Install SystemUI.apk"
    echo "4. Test Stone panel"
    echo "5. Monitor logs"
    echo "6. Full test (start, install, test)"
    echo ""
    read -p "Enter choice (1-6): " choice
    
    case $choice in
        1) list_avds ;;
        2) start_emulator ;;
        3) install_systemui ;;
        4) test_stone_panel ;;
        5) monitor_logs ;;
        6) 
            start_emulator
            sleep 5
            install_systemui
            test_stone_panel
            ;;
        *) echo "Invalid choice" ;;
    esac
}

# Run main function
main "$@"