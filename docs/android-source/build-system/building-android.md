# Build Android

**Source:** https://source..com/docs/setup/build/building
**Scrape Date:** 2025-10-23

---

Follow the instructions on this page to build Android.


## SET UP YOUR BUILD ENVIRONMENT

From within your working directory, source the `envsetup.sh` script to set up your build environment:

```bash
source build/envsetup.sh
```

This script imports several commands that let you work with the Android source code, including the commands used on this page. To view the source of the script, refer to `platform/build/envsetup.sh`. To view the built-in help, type `hmm`.


## CHOOSE A TARGET

Before building Android, you must identify a target to build. A target reflects the target platform you're building for. To identify your target to build, use the `lunch` command followed by a string representing the target. For example:

```bash
lunch aosp_cf_x86_64_only_phone-aosp_current-userdebug
```

You should see is a synopsis of your target and build environment:

```
============================================
PLATFORM_VERSION_CODENAME=Baklava
PLATFORM_VERSION=Baklava
TARGET_PRODUCT=aosp_cf_x86_64_only_phone
TARGET_BUILD_VARIANT=userdebug
TARGET_ARCH=x86_64
TARGET_ARCH_VARIANT=silvermont
HOST_OS=linux
HOST_OS_EXTRA=Linux-6.10.11-1rodete2-amd64-x86_64-Debian-GNU/Linux-rodete
HOST_CROSS_OS=windows
BUILD_ID=BP1A.250305.020
OUT_DIR=out
============================================
```

**Note:** You must run `source envsetup.sh` once per shell and before the `lunch` command because `lunch` is defined by `envsetup.sh`.

The string representing the target has the following format:

`lunch product_name-release_config-build_variant`

The components of this string are:

*   The `product_name` is the name of the product you want to build, such as `aosp_cf_x86_64_only_phone` or `aosp_husky`. Your specific `product_name` can follow your own format for your device, but the format that Google uses for its devices has these components:
    *   `aosp` refers to the Android Open Source Platform.
    *   (optional) `cf` is included when the target is intended to be run within the Cuttlefish emulator.
    *   Architecture and hardware (codename), such as `x86_64_only_phone` or `husky`, which is the codename for Pixel 8 Pro. For list of codenames for Google devices, see Device codenames.

*   `release_config` is set to a release configuration, such as the development release configuration called `aosp_current`. A release configuration identifies certain features and code that are behind feature launch flags and are either enabled or disabled for a build. For more on release configurations, see Set feature flag launch values.

*   The `build_variant` portion of the string can be one of the three values in the following table:

| build_variant | Description |
| :--- | :--- |
| `user` | This build variant provides limited security access and is suited for production. |
| `userdebug` | This build variant helps the device developers understand the performance and power of in-development releases. When developing with a userdebug build, follow the Guidelines for userdebug. |
| `eng` | This build variant has faster build time and is best suited for day-to-day development if you don't care about performance and power. |

If you run `lunch` without any arguments, a list of common targets is provided. You can also create your own target strings by piecing together the elements of the target string using the information on this page and the codenames that represent specific Google hardware at Device codenames.


## VIEW THE CURRENT TARGET

To see the current lunch settings, run:

```bash
$ echo "$TARGET_PRODUCT-$TARGET_BUILD_VARIANT"
```


## BUILD THE CODE

Run the following command to build your target. Depending on the specification of your workstation, the first build could take less than an hour and up to a few hours. Subsequent builds take significantly less time.

```bash
m
```

The output of your build appears in `$OUT_DIR`. If you build different targets, each target build appears in `$OUT_DIR`.

The `m` command builds from the top of the tree, so you can run `m` from within subdirectories. If you have the `TOP` environment variable set, the `m` command uses it. If `TOP` isn't set, the `m` command looks up the tree from the current directory, trying to find the top of the tree.

The `m` command can handle parallel tasks with a `-jN` argument. If you don't provide a `-j` argument, the build system automatically selects a parallel task count that it thinks is optimal for your system.

You can build specific modules instead of the full device image by listing module names in your `m` command line. In addition, the `m` command provides some pseudo targets, called goals. For example, `m nothing` doesn't build anything, but parses and validates the build structure. For a list of valid goals, type `m help`.


## TROUBLESHOOT BUILD ERRORS (8.0 OR EARLIER)

If you're building AOSP 8 or earlier, `m` might abort when it encounters an issue with your version of Java. For example, you might get this message:

```
************************************************************
You are attempting to build with the incorrect version
of java.

Your version is: WRONG_VERSION.
The correct version is: RIGHT_VERSION.

Please follow the machine setup instructions at
    https://source.android.com/source/initializing.html
************************************************************
```

Here are the likely causes and solutions:

*   You failed to install the correct JDK as specified in the JDK sections of Set Up for AOSP development (2.3 - 8.0) .
*   There's another previously installed JDK appearing in your path. Prepend the correct JDK to the beginning of your path or remove the problematic JDK.
