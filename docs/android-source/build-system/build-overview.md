# Build Overview

**Source:** https://source.android.com/docs/setup/build
**Scrape Date:** 2025-10-23

---

AOSP uses the Soong build system to build Android. Soong leverages the kati GNU Make clone tool and Ninja build system component to speed up builds of Android.

Soong build files are called blueprint files and are named Android.bp. These files are similar in syntax and sematics to Bazel BUILD files.

For a detailed description of the Android.bp file format, see Android.bp file format.

For information on converting your Make files to Android.bp files, see Make and Soong comparison.


## FEATURE LAUNCH FLAGS AND BUILD FLAGS

Feature launch flags are binary flags used to isolate untested code from tested code. If you have your own mirror of the AOSP external development branch, you can use these flags to keep your mirrored code stable. Additionally, if you intend on contributing code to the public external development branch, you might be asked by your change's reviewer to implement a flag for your code.

Build flags are build-time constants (strings) used to modify your build, such as optionally including a code library.

For an explanation of the different code branches, see Release lifecycle.

Feature launch flags and build flags require changes to build files. For additional information on these flags, including their use in a build, see the Feature launch flags overview and adjacent pages.
