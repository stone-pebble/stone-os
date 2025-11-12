# Build Pixel Kernels

**Source:** https://source.android.com/docs/setup/build/building-pixel-kernels
**Scrape Date:** 2025-10-23

---

This guide provides step-by-step instructions on how to download, compile, and flash a custom Pixel kernel for development. Due to GKI, it's now possible to update the kernel independently of the Android platform build. These steps are only applicable for Pixel 6 and later devices. This is because Pixel 5 and earlier devices require updating the kernel modules on the vendor partition, which is dependent on the Android platform build for those devices. The GKI supported Pixel kernel branches table includes the kernel repository manifest branch for each GKI supported Pixel device. Refer to the Legacy Pixel Kernels section for the Pixel 5 and earlier kernel manifest branches.

**Note:** If you only want to build the core kernel, refer to Building Kernels for the steps to build the GKI kernel. You can use the provided table to determine which GKI kernel version to use. The repository branches are the factory supported Pixel kernel repositories which include both the GKI kernel and Pixel drivers.

## GKI SUPPORTED PIXEL KERNEL BRANCHES

| Device | Repository branches | GKI Kernel |
| :--- | :--- | :--- |
| Pixel 9a (tegu) | android-gs-tegu-6.1-android16 | android14-6.1 |
| Pixel 9 Pro Fold (comet) | android-gs-comet-6.1-android16 | android14-6.1 |
| Pixel 9 (tokay)<br>Pixel 9 Pro (caiman)<br>Pixel 9 Pro XL (komodo) | android-gs-caimito-6.1-android16 | android14-6.1 |
| Pixel 8a (akita) | android-gs-akita-6.1-android16 | android14-6.1 |
| Pixel 8 (shiba)<br>Pixel 8 Pro (husky) | android-gs-shusky-6.1-android16 | android14-6.1 |
| Pixel Fold (felix) | android-gs-felix-6.1-android16 | android14-6.1 |
| Pixel Tablet (tangorpro) | android-gs-tangorpro-6.1-android16 | android14-6.1 |
| Pixel 7a (lynx) | android-gs-lynx-6.1-android16 | android14-6.1 |
| Pixel 7 (panther)<br>Pixel 7 Pro (cheetah) | android-gs-pantah-6.1-android16 | android14-6.1 |
| Pixel 6a (bluejay) | android-gs-bluejay-6.1-android16 | android14-6.1 |
| Pixel 6 (oriole)<br>Pixel 6 Pro (raven) | android-gs-raviole-6.1-android16 | android14-6.1 |

In addition to the factory supported kernels, the Pixel 6 and 6 Pro devices are supported for GKI development purposes only on the Android Common kernel branches included in the Supported Pixel 6/6 Pro Android Platform and Kernel Combinations table. Due to vendor UAPI differences between the Android platform HALs and Pixel kernel drivers, the table provides the supported build combinations.

**Warning:** These development kernels may be unstable and cause your device to malfunction. Proceed with caution and make sure to back up your data before flashing a custom kernel.

## SUPPORTED PIXEL 6/6 PRO ANDROID PLATFORM AND KERNEL COMBINATIONS

| Pixel Kernel Manifest Branch | GKI Branch | Android Platform Build |
| :--- | :--- | :--- |
| gs-android-gs-raviole-mainline | android-mainline | android-latest-release |
| gs-android16-6.12-gs101 | android16-6.12 | android-latest-release |
| gs-android13-gs-raviole-5.15 | android13-5.15 | TQ1A.230205.002 (9471150) |


## PREPARE YOUR PIXEL DEVICE

The following flow chart describes the process for updating the kernel on Pixel 6 and later devices:

![Flow chart for updating kernel on Pixel 6 and later devices](https://source.android.com/static/docs/setup/images/build/gki-pixel-kernel-update-flow.png)


## FLASH THE DEVICE USING FLASH.ANDROID.COM

**Note:** The bootloader needs to be unlocked before you can flash your device. Refer to the section Locking and Unlocking the Bootloader.
 1. Navigate to flash.android.com
 2. Pick the Android build based on the supported Android Platform and Kernel combinations.
    * For android-latest-release, select either "Back to Public", Canary, or Beta.
 3. Select the following options:
    * Wipe Device
    * Force Flash all Partitions
    * Disable Verification
 4. Press the Install build button to flash the device.


## DOWNLOAD AND COMPILE THE KERNEL


### SYNC THE KERNEL REPOSITORY

Run the following commands to download the kernel source code. Refer to the Supported Pixel 6/6 Pro Android Platform and Kernel Combinations table for the Pixel KERNEL_MANIFEST_BRANCH.

```bash
repo init -u https://android.googlesource.com/kernel/manifest -b KERNEL_MANIFEST_BRANCH
repo sync -c --no-tags
```


### UPDATE THE VENDOR RAMDISK

**Important:** Skip this section for Pixel 7 and later devices. Only Pixel 6/6 Pro/6a devices need to update the vendor ramdisk. Pixel 7 and later devices split the vendor_boot partition into two partitions -- vendor_boot and vendor_kernel_boot -- where the kernel artifacts are fully contained by the vendor_kernel_boot image.
**Important:** The Pixel 6/6 Pro/6a kernel repos include a prebuilt Android vendor ramdisk which may not match the Android build on your device. This mismatch usually results in an SELinux failure that prevents the device from booting.

Update the file `vendor_ramdisk-DEVICE.img` in the kernel repository to match the Android platform build that is flashed on the device. There are a couple of options to update the `vendor_ramdisk-DEVICE.img` file. Use option (1) if you are using Android 15-QPR2 (BP11.241025.006) or later. Otherwise, use option (2).

*   Option 1) Update only the DTB and DLKM ramdisk bits of the vendor_boot partition
    
    Starting with fastboot version 35.0.2-12583183, you can directly flash the DTB and DLKM ramdisk on the vendor_boot partition. Download and extract `sdk-repo-HOST_OS-platform-tools-12583183.zip` from the v35.0.2-12583183 artifacts to your host machine's environment for use.
    
    Follow the instructions to flash DTB and vendor_boot:dlkm in the Flash the kernel images.

*   Option 2) Extract the vendor ramdisk image from the Pixel factory image.
    
    1.  Download the supported factory image for your device from https://developers.google.com/android/images.
        
        **Note:** For Pixel 6/6 Pro/6a devices, pick the build that is flashed on your device so that the vendor ramdisk matches the platform build. You can use flash.android.com if you want to change the Android build on your device.
    
    2.  Extract the `vendor_boot.img`:
        
        The following commands use the Pixel 6 Pro AP1A.240505.004 as an example. Replace the zipfile name with the filename of the factory image you downloaded.
        
        ```bash
        unzip raven-ap1a.240505.004-factory-9d783215.zip
        cd raven-ap1a.240505.004
        unzip image-raven-ap1a.240505.004.zip vendor_boot.img
        ```
    
    3.  Unpack the `vendor_boot.img` to obtain the vendor ramdisk.
        
        ```bash
        KERNEL_REPO_ROOT/tools/mkbootimg/unpack_bootimg.py --boot_img vendor_boot.img \
            --out vendor_boot_out
        ```
    
    4.  Copy the extracted `vendor-ramdisk-by-name/ramdisk_` file to the Pixel kernel repository.
        
| Device | DEVICE_RAMDISK_PATH |
| :--- | :--- |
| Pixel 6 (oriole)<br>Pixel 6 Pro (raven) | `prebuilts/boot-artifacts/ramdisks/vendor_ramdisk-oriole.img` |
| Pixel 6a (bluejay) | `private/devices/google/bluejay/vendor_ramdisk-bluejay.img` |
        
        ```bash
        cp vendor_boot_out/vendor-ramdisk-by-name/ramdisk_ \
            KERNEL_REPO_ROOT/DEVICE_RAMDISK_PATH
        ```


### COMPILE THE KERNEL (KLEAF)

In Android 13, the `build.sh` script was replaced with a new kernel build system called Kleaf. For devices using `android13-5.15` and later, the kernel should be built using Kleaf.

For convenience, you can run the `build_DEVICE.sh` script found at the `KERNEL_REPO_ROOT`. In most cases, `DEVICE` needs to be the code name, which can be the code name of one device, such as "akita" (Pixel 8a), or a code name that represents a group of related devices that share a kernel, such as "caimito" which means Pixel 9 (tokay), Pixel 9 Pro (caiman), and Pixel 9 Pro XL (komodo). For `android14` and earlier releases, use `build_slider.sh` for Pixel 6 and Pixel 6 Pro, and `build_cloudripper.sh` for Pixel 7 and Pixel 7 Pro.

For example, to build the kernel for Pixel 6 on the branch `android-gs-raviole-5.10-android14`, you would run the command:

```bash
build_slider.sh
```

By default on the production kernel branches, the `build_DEVICE.sh` scripts use the prebuilt GKI kernel to speed up the build process. If you want to modify the core kernel, then set the environment variable `BUILD_AOSP_KERNEL=1` to build the kernel from the local sources instead. The development kernel branches directly build the kernel source by default.

For more details about the kernel build system and how to customize the build, refer to the Kleaf - Building Android Kernels with Bazel.


### FLASH THE KERNEL IMAGES

> **Note:** If you haven't disabled verification, you need to do it before flashing the custom kernel. Here is the command to do so:
> 
> ```bash
> fastboot oem disable-verification
> ```

> **WARNING:** If you are flashing a custom kernel on top of a platform build, then you may need to wipe your device if there is a security patch level (SPL) downgrade associated with the new kernel. This process erases all of your personal data. Be sure to back up your data before wiping.
> 
> ```bash
> fastboot -w
> ```

To flash the kernel images, run the `fastboot flash` command for each kernel partition listed for your device. For dynamic partitions, you need to reboot into `fastbootd` mode before flashing.

| Device | Kernel Partitions |
| :--- | :--- |
| Pixel 6 (oriole)<br>Pixel 6 Pro (raven)<br>Pixel 6a (bluejay) | `boot`<br>`dtbo`<br>`vendor_boot` or `vendor_boot:dlkm`<br>`vendor_dlkm` (dynamic partition) |
| Pixel 9 (tegu)<br>Pixel 9 Pro Fold (comet)<br>Pixel 9 (tokay)<br>Pixel 9 Pro (caiman)<br>Pixel 9 Pro XL (komodo)<br>Pixel 8 (shiba)<br>Pixel 8 Pro (husky)<br>Pixel Fold (felix)<br>Pixel Tablet (tangorpro)<br>Pixel 7a (lynx)<br>Pixel 7 (panther)<br>Pixel 7 Pro (cheetah) | `boot`<br>`dtbo`<br>`vendor_kernel_boot`<br>`vendor_dlkm` (dynamic partition)<br>`system_dlkm` (dynamic partition) |

Here are the flashing commands for Pixel 6 on `android-mainline`:

```bash
fastboot flash boot        out/slider/dist/boot.img
fastboot flash dtbo        out/slider/dist/dtbo.img
fastboot flash  --dtb out/slider/dist/dtb.img vendor_boot:dlkm out/slider/dist/initramfs.img
fastboot reboot fastboot
fastboot flash vendor_dlkm out/slider/dist/vendor_dlkm.img
```

For Pixel 6/6 Pro/6a, if you updated the `vendor_ramdisk` in the Update the vendor ramdisk section, then instead use the following command to update the `vendor_boot` partition:

```bash
fastboot flash vendor_boot out/slider/dist/vendor_boot.img
```

The kernel images can be found in the `DIST_DIR`.

| Kernel branch | DIST_DIR |
| :--- | :--- |
| v5.10 | `out/mixed/dist` |
| v5.15 and later | `out/DEVICE/dist` |

> **Note:** If you have a serial dongle and want to enable serial logs, the command is:
> 
> ```bash
> fastboot oem uart enable
> fastboot oem uart config 3000000
> ```
> 
> Example command to connect from the host:
> 
> ```bash
> screen -fn /dev/ttyUSB* 3000000
> ```


## RESTORE THE FACTORY IMAGES

To restore your device back to the factory images, you can use flash.android.com.


## LEGACY PIXEL KERNELS

As a reference, the legacy Pixel kernel branches table provides the kernel repository branches for Pixel 5 and earlier devices. These are non-GKI supported devices.

## LEGACY PIXEL KERNEL BRANCHES

| Device | Binary path in AOSP tree | Repository branches |
| :--- | :--- | :--- |
| Pixel 5a (barbet)<br>Pixel 4a (5G) (bramble)<br>Pixel 5 (redfin) | `device/google/redbull-kernel` | `android-msm-redbull-4.19-android14-qpr3` |
| Pixel 4a (sunfish) | `device/google/sunfish-kernel` | `android-msm-sunfish-4.14-android13-qpr3` |
| Pixel 4 (flame)<br>Pixel 4 XL (coral) | `device/google/coral-kernel` | `android-msm-coral-4.14-android13` |
| Pixel 3a (sargo)<br>Pixel 3a XL (bonito) | `device/google/bonito-kernel` | `android-msm-bonito-4.9-android12L` |
| Pixel 3 (blueline)<br>Pixel 3 XL (crosshatch) | `device/google/crosshatch-kernel` | `android-msm-crosshatch-4.9-android12` |
| Pixel 2 (walleye)<br>Pixel 2 XL (taimen) | `device/google/wahoo-kernel` | `android-msm-wahoo-4.4-android10-qpr3` |
| Pixel (sailfish)<br>Pixel XL (marlin) | `device/google/marlin-kernel` | `android-msm-marlin-3.18-pie-qpr2` |
