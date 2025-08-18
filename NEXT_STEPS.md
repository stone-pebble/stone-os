# Next Steps for StoneOS

## Current Status
- ✅ Bootloader unlocked on Pixel 8a
- ✅ SystemUI extracted and analyzed
- ⏳ Root access pending (Magisk needs Direct Install)
- ✅ Repository structured for Option C approach

## Immediate Next Session Tasks

### 1. Get Root Working (15 minutes)
```bash
# On device: Open Magisk app
# Select: Direct Install
# Reboot when complete
# Verify: adb shell su -c 'whoami'  # Should output: root
```

### 2. Test SystemUI Replacement (30 minutes)
```bash
# Once rooted, run:
./systemui-mod.sh
./install-systemui.sh

# Check logs:
adb logcat | grep StoneOS
```

### 3. Install Stone Launcher (30 minutes)
```bash
cd stone-launcher
npm install
npm run build:android
adb install -r android/app/build/outputs/apk/release/app-release.apk
adb shell cmd package set-home-activity com.stonelauncher/.MainActivity
```

## Key Files for Next Session

- `STONEOS_SPECS.md` - The source of truth
- `CLAUDE.md` - Implementation approach (Option C)
- `systemui-mod.sh` - Ready to modify SystemUI
- `install-systemui.sh` - Ready to install modified SystemUI

## What Success Looks Like

1. **Proof of Root**: Can modify system files
2. **Proof of SystemUI Control**: Our code runs in SystemUI
3. **Proof of Launcher**: Stone launcher replaces home
4. **Proof of Chat**: Overlay appears on swipe-up

## Don't Get Distracted By

- AOSP building (we're NOT doing that)
- Perfect UI (just prove concept)
- All 12 apps (just get basics working)
- Voice integration (text chat first)

## Remember the Vision

We're making a MINIMALIST OS where:
- Apps are grayscale and full-screen
- Stone icon at bottom enables AI chat
- Real Android apps, not web views
- User chooses touch OR AI control

Keep it simple. Under 100 lines per component.