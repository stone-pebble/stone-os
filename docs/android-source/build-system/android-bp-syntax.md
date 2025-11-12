# Android.bp File Syntax Reference

**Source:** [Android.bp File Format](https://source.android.com/docs/setup/build/bp-format)
**Scrape Date:** 2025-10-23

---

## Overview

`Android.bp` files are JSON-like, declarative configuration files used by the Soong build system. They are designed to be simple, without complex logic, to describe the modules that need to be built.

<!-- StoneOS Note: This is the file format we use to define all our custom applications (`StoneLauncher`, `StoneSettings`) and to integrate our framework modifications. -->

## File Format

### Comments

`Android.bp` files support C-style block comments and C++ style line comments.

```json
/*
 * This is a multi-line
 * block comment.
 */
cc_library {
    name: "libFoo", // This is a single-line comment
    srcs: ["foo.c"],
}
```

### Data Types

Properties in `Android.bp` are strongly typed. The supported types are:

-   **Booleans:** `true` or `false`
-   **Strings:** `"a string"`
-   **Lists of Strings:** `["string1", "string2"]`
-   **Maps:** `{ name: "value", key: "another_value" }`
-   **Integers:** `5` (Used for properties like `min_sdk_version`)

### Modules

Every `Android.bp` file consists of one or more modules. A module begins with the module type, followed by a block of properties in `{}`.

```json
module_type {
    name: "unique_module_name",
    property1: "value1",
    property2: ["value2a", "value2b"],
    ...
}
```

Every module **must** have a `name` property, and its value must be unique across the entire AOSP source tree.

### Common Module Types for StoneOS

-   `android_app`: Defines a new Android application (`.apk`).
    <!-- StoneOS Note: We use this for `StoneLauncher`, `StoneSettings`, etc. -->
-   `android_library`: Defines a Java/Kotlin library for use by other Android modules.
-   `cc_library`, `cc_binary`: Defines a C/C++ library or executable.
    <!-- StoneOS Note: We will use this for modifying native components like `SurfaceFlinger`. -->
-   `filegroup`: Defines a named group of source files.

### Variables

An `Android.bp` file can contain top-level variable assignments. These variables are scoped to the remainder of the file they are declared in.

```json
my_c_flags = ["-Wall", "-Werror"]

cc_library {
    name: "libBar",
    srcs: ["bar.c"],
    cflags: my_c_flags,
}
```

### Globs

The `srcs` property and other file-related properties can use glob patterns to match multiple files.

```json
android_library {
    name: "my-java-lib",
    srcs: [
        "src/com/example/**/*.java", // Recursively include all Java files
        "src/com/example/protos/*.proto",
    ],
}
```
<!-- StoneOS Note: This is a critical feature. Our `SystemUI` modifications are picked up by an existing `src/**/*.java` glob in the main `SystemUI` `Android.bp` file. Understanding glob behavior is essential. -->

### The `defaults` Property

A `defaults` module can be used to share the same set of properties across multiple modules.

```json
cc_defaults {
    name: "my_c_defaults",
    cflags: ["-O3", "-g"],
}

cc_library {
    name: "libFoo",
    defaults: ["my_c_defaults"],
    srcs: ["foo.c"],
}

cc_library {
    name: "libBar",
    defaults: ["my_c_defaults"],
    srcs: ["bar.c"],
}
```

### Formatting

AOSP provides a canonical formatter for `Android.bp` files called `bpfmt`. It should be used to ensure all files follow the standard style.

```bash
# To reformat the current directory's Android.bp file
bpfmt -w Android.bp
```
