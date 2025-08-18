# SystemUI Modification Status

## What We've Done
✅ Extracted SystemUI.apk from Pixel 8a (35MB)
✅ Decompiled with apktool successfully
✅ Located key SystemUI components
✅ Created modification scripts

## Current Issue
The APK rebuild has resource errors (normal for SystemUI):
- Private resource references
- These are warnings, not fatal

## Next Steps for Minimal POC

### Option 1: Simpler Test (5 minutes)
Instead of modifying SystemUI, first prove we can:
1. Disable stock launcher
2. Install our launcher as system app
3. Add overlay service for chat

### Option 2: Continue SystemUI (1-2 hours)
1. Fix resource compilation issues
2. Get platform signing keys
3. Test on device with recovery ready

## The Real Question
Do we need to modify SystemUI for POC, or can we prove concept with:
- Root + Custom Launcher (replaces home)
- Accessibility Service (controls apps)
- Overlay Service (chat interface)
- Xposed/LSPosed (system modifications)

## File Structure (Cleaned)
```
stone-os/
├── SystemUI_original.apk    # From device
├── SystemUI_decompiled/      # Decompiled
├── systemui-mod.sh          # Modification script
├── install-systemui.sh      # Installation script
├── stone.keystore           # Signing key
├── STONEOS_SPECS.md        # Original specs
├── CLAUDE.md               # Updated for Option C
└── stone-launcher/         # React Native launcher
```

## Recommendation
Start with simpler approach:
1. Get Magisk working (for root)
2. Install custom launcher
3. Add chat overlay
4. THEN tackle SystemUI if needed

This proves concept faster with less risk.