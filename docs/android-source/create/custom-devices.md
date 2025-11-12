# Custom Device Development

**Source:** https://source.android.com/docs/setup/create/custom-devices
**Scrape Date:** 2025-10-23

---

This page explains several tasks you might need to perform if you're setting up a build for your own device.


## CREATE A CUSTOM FLASH CONFIGURATION

Fastboot instructions are defined in an artifact called `fastboot-info.txt`. If you build multiple targets, you'll have multiple `fastboot-info.txt` files in the `$OUT_DIR`. And, `$ANDROID_PRODUCT_OUT` points to the most current target you built. This page list the tasks for fastboot to execute and can be regenerated using `m fastboot_info`. You can introduce custom flashing logic by modifying the `fastboot-info.txt` file.

The `fastboot-info.txt` file supports these commands:

*   `flash %s`: Flashes a given partition. Optional arguments include `--slot-other`,`filename_path`, and`--apply-vbmeta`.
*   `update-super`: Updates the super partition.
*   `if-wipe`: Conditionally runs some other component if a wipe is specified.
*   `erase %s`: Erases a given partition (can only be used in conjunction with `if-wipe` -> eg. `if-wipe erase cache`).


## DETERMINE FLASH LOCK STATE

If you're building a custom flashboot daemon (`flashbootd`) for a device, you need to be able to obtain bootloader and bootloader lock state. The `getFlashLockState()` `@SystemApi` transmits the bootloader state and the `PersistentDataBlockManager.getFlashLockState()` system API returns the bootloader's lock status on compliant devices.

| Return value | Conditions |
| :--- | :--- |
| `FLASH_LOCK_UNKNOWN` | Returned only by devices upgrading to Android 7.x or higher that didn't previously support the bootloader changes required to get the flash lock status if they supported flashing lock/unlock capability.<br><br>*   New devices running Android 7.x or higher must be in either a `FLASH_LOCK_LOCKED` or `FLASH_LOCK_UNLOCKED` state.<br>*   Devices upgrading to Android 7.x or higher that don't support flashing unlock/lock capability should return a `FLASH_LOCK_LOCKED` state. |
| `FLASH_LOCK_LOCKED` | Returned by any device that doesn't support flashing lock/unlock (that is, the device is always locked), or any device that supports flashing lock/unlock and is in the locked state. |
| `FLASH_LOCK_UNLOCKED` | Returned by any device that supports flashing lock/unlock and is in the unlocked state. |

Manufacturers should test the values returned by devices with locked and unlocked bootloaders. For example, AOSP contains a reference implementation that returns a value based on the `ro.boot.flash.locked` boot property. Example code is located in the following directories:

*   `frameworks/base/services/core/java/com/android/server/PersistentDataBlockService.java`
*   `frameworks/base/core/java/android/service/persistentdata/PersistentDataBlockManager.java`
