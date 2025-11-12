# Add a New Device

**Source:** https://source.android.com/docs/setup/create/new-device
**Scrape Date:** 2025-10-23

---

This page explains how to add a new device type and build Android for it.


## UNDERSTAND ANDROID BUILD CONFIGURATIONS

The Android build system uses build configurations to organize the build process. A build configuration is made up of a device type and a build variant.


### DEVICE TYPES

A device type, or product, is the specific type of Android device you're building. For example, `aosp_cf_x86_64_phone` is a device type. This device type represents a specific hardware configuration, including the device's architecture, bootloader, and other hardware-specific features.


### BUILD VARIANTS

A build variant is the specific flavor of the device type you're building. For example, `userdebug` is a build variant. This build variant represents a specific set of build options, including the level of debugging, the type of signing, and the set of apps that are included.

The combination of a device type and a build variant forms a complete build configuration. For example, `aosp_cf_x86_64_phone-userdebug` is a complete build configuration.


## CREATE DEVICE-SPECIFIC CONFIGURATION FILES

To add a new device type, you need to create a directory for your device in the `device/` directory of the Android source tree. This directory should contain the following files:

*   `BoardConfig.mk`: This file contains the hardware-specific configuration for your device.
*   `device.mk`: This file defines the features and modules that are included in your device.
*   `vendorsetup.sh`: This script is used to add your device to the `lunch` menu.
*   `AndroidProducts.mk`: This file contains a list of the product makefiles for your device.

### CREATE THE `DEVICE.MK` MAKEFILE

The `device.mk` makefile defines the features and modules that are included in your device. For example, to specify that your device has a camera and supports NFC, you would add the following lines to your `device.mk` file:

```make
PRODUCT_PACKAGES += \
    Camera \
    Nfc
```

### CREATE THE `ANDROIDPRODUCTS.MK` MAKEFILE

The `AndroidProducts.mk` makefile contains a list of the product makefiles for your device. For example, if you have a product makefile called `my_device.mk`, you would add the following line to your `AndroidProducts.mk` file:

```make
PRODUCT_MAKEFILES := \
    $(LOCAL_DIR)/my_device.mk
```

### CREATE THE `BOARDCONFIG.MK` MAKEFILE

The `BoardConfig.mk` makefile contains the hardware-specific configuration for your device. For example, to specify that your device has a 64-bit architecture, you would add the following line to your `BoardConfig.mk` file:

```make
TARGET_ARCH := arm64
```

### CREATE THE `VENDORSETUP.SH` SCRIPT

The `vendorsetup.sh` script is used to add your device to the `lunch` menu. For example, to add a `userdebug` build variant for your device, you would add the following line to your `vendorsetup.sh` script:

```bash
add_lunch_combo my_device-userdebug
```

## BUILD FOR YOUR DEVICE

After you have created the device-specific configuration files, you can build Android for your device. To do this, you need to source the `envsetup.sh` script and then use the `lunch` command to select your device and build variant. For example:

```bash
source build/envsetup.sh
lunch my_device-userdebug
m
```

