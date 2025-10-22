#
# StoneOS Product Configuration
#
# This makefile adds StoneOS-specific packages and removes AOSP defaults
#

# Add StoneOS apps to the build
PRODUCT_PACKAGES += \
    StoneLauncher \
    StoneSettings \
    Tick \

# Remove default AOSP launcher
# Note: This uses the PRODUCT_PACKAGES_REMOVE mechanism
# to exclude Launcher3QuickStep from the build
PRODUCT_PACKAGES_REMOVE += \
    Launcher3QuickStep \

# Add Stone SystemUI components (already in frameworks/base)
# StoneManager, StoneIcon, and StonePanel are part of SystemUI
# and will be included automatically

# Product branding for StoneOS
PRODUCT_NAME := stoneos_x86_64
PRODUCT_BRAND := StoneOS
PRODUCT_MODEL := StoneOS Emulator
