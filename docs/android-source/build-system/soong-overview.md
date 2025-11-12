# AOSP Build System: Soong

**Source:** [Soong Build System Overview](https://source.android.com/docs/setup/build/soong)
**Scrape Date:** 2025-10-23

---

## Overview

Soong is the modern build system used by AOSP, introduced in Android 7.0 to replace the legacy Make-based system. It was designed to address the slowness, error-proneness, and lack of scalability of Make.

<!-- StoneOS Note: Understanding Soong is critical. All of our custom components, from framework modifications in `SystemUI` to our standalone system apps like `StoneLauncher`, are defined using Soong's `Android.bp` files. -->

## Key Concepts

### `Android.bp` Files

-   **Declarative Syntax:** Instead of the procedural `Android.mk` files, Soong uses `Android.bp` files. These files are simple, declarative, JSON-like configuration files that describe *what* to build, not *how* to build it.
-   **No Conditionals:** `Android.bp` files do not support conditionals (`if/else`) or control flow. Any complex logic is handled in the build system's Go code, keeping the build definitions simple and clean.

### The Build Process

Soong does not execute the build itself. Instead, it parses all `Android.bp` files across the entire AOSP source tree and uses that information to generate a high-performance `build.ninja` file. The actual compilation and linking is then handled by the **Ninja** build system, which excels at executing command graphs with high parallelism.

**Workflow:**
1.  `m` command is invoked.
2.  **Soong** (a Go program) scans the source tree for all `Android.bp` files.
3.  Soong generates a `build.ninja` file describing the full dependency graph of the build.
4.  **Ninja** reads the `build.ninja` file and executes the build commands (e.g., `clang++`, `javac`) in the most efficient, parallel manner possible.

### Modules

The fundamental unit in an `Android.bp` file is a **module**.

-   A module starts with a module type (e.g., `android_app`, `cc_binary`).
-   It is followed by a set of properties in `name: "value"` format.
-   Every module **must** have a `name` property, which must be unique across all `Android.bp` files.

**Example:**
```json
cc_library {
    name: "libMyFoo",
    srcs: ["foo.c", "bar.c"],
    shared_libs: ["libbase"],
    cflags: ["-Wall"],
}
```

### Transition from Make

AOSP is still in a transition period from Make to Soong. While most modern components use `Android.bp`, some legacy `Android.mk` files still exist. The build system uses a tool called **Kati** to parse these `Android.mk` files and convert them into Ninja format as well, allowing both systems to coexist.

<!-- StoneOS Note: For all new custom components, we **must** use `Android.bp`. We should never create new `Android.mk` files. -->
