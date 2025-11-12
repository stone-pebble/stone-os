# Implement Java SDK Library

**Source:** https://source.android.com/docs/setup/build/java-library
**Scrape Date:** 2025-10-23

---

The Android platform contains a large number of shared Java libraries that can optionally be included in the classpath of apps with the `<uses-library>` tag in the app manifest. Apps link against these libraries, so treat them like the rest of the Android API in terms of compatibility, API review, and tooling support. Note, however, that most libraries don't have these features.

The `java_sdk_library` module type helps manage libraries of this kind. Device manufacturers can use this mechanism for their own shared Java libraries, to maintain backward compatibility for their APIs. If device manufacturers use their own shared Java libraries through the `<uses-library>` tag instead of the bootclass path, `java_sdk_library` can verify that those Java libraries are API-stable.

The `java_sdk_library` implements optional SDK APIs for use by apps. Libraries implemented through `java_sdk_library` in your build file (`Android.bp`) perform the following operations:

*   The stubs libraries are generated to include `stubs`, `stubs.system`, and `stubs.test`. These stubs libraries are created by recognizing `@hide`, `@SystemApi`, and `@TestApi` annotations.
*   The `java_sdk_library` manages API specification files (such as `current.txt`) in an API subdirectory. These files are checked against the latest code to ensure that they’re the most current versions. If they aren’t, you receive an error message that explains how to update them. Manually review all update changes to ensure that they match your expectations.
    
    To update all APIs, use `m update-api`. To verify that an API is up-to-date, use `m checkapi`.
*   The API specification files are checked against the most recently published Android versions to ensure that the API is backward-compatible with earlier releases. The `java_sdk_library` modules provided as part of AOSP place their previously released versions in `prebuilts/sdk/<latest number>`.
*   With respect to the API specification files checks, you can do one of the following three things:
    *   Allow the checks to proceed. (Don’t do anything.)
    *   Disable checks by adding the following to `java_sdk_library`:
        `unsafe_ignore_missing_latest_api: true,`
    *   Provide empty APIs for new `java_sdk_library` modules by creating empty text files named `module_name.txt` in the `version/scope/api` directory.

*   If the implementation library for the runtime is installed, an XML file gets generated and installed.


## HOW JAVA_SDK_LIBRARY WORKS

A `java_sdk_library` called `X` creates the following:

1.  Two copies of the implementation library: one library called `X` and another called `X.impl`. Library `X` is installed on-device. Library `X.impl` is there only if explicit access to the implementation library is needed by other modules, such as for use in testing. Note that explicit access is rarely needed.
2.  Scopes can be enabled and disabled to customize access. (Similar to Java keyword-access modifiers, a public scope provides a wide range of access; a test scope contains APIs only used in testing.) For each enabled scope the library creates the following:
    *   A stubs source module (of `droidstubs` module type) - consumes the implementation source and outputs a set of stub sources along with the API specification file.
    *   A stubs library (of `java_library` module type) - is the compiled version of the stubs. The `libs` used to compile this aren’t the same as those supplied to the `java_sdk_library`, which ensures the implementation details don’t leak into the API stubs.
    *   If you need additional libraries to compile the stubs, use the `stub_only_libs` and `stub_only_static_libs` properties to supply them.

If a `java_sdk_library` is called “X”, and is being compiled against as “X”, always refer to it that way and don’t modify it. The build will select an appropriate library. To ensure that you have the most appropriate library, inspect your stubs to see if the build introduced errors. Make any necessary corrections using this guidance:

*   Verify you have an appropriate library by looking on the command line and inspecting which stubs are listed there to determine your scope:
    *   Scope is too wide: The depending library needs a certain scope of APIs. But you see APIs included in the library that fall outside of that scope, such as system APIs included with the public APIs.
    *   Scope is too narrow: The depending library doesn’t have access to all the requisite libraries. For example, the depending library needs to use the system API but gets the public API instead. This usually results in a compilation error because needed APIs are missing.

*   To fix the library, do only one of the following:
    *   Change the `sdk_version` to select the version you need. OR
    *   Explicitly specify the appropriate library, such as `<X>.stubs` or `<X>.stubs.system`.


## JAVA_SDK_LIBRARY X USAGE

The implementation library `X` gets used when it’s referenced from `apex.java_libs`. However, due to a Soong limitation, when library `X` is referenced from another `java_sdk_library` module within the same APEX library, `X.impl` explicitly must be used, not library `X`.

When the `java_sdk_library` is referenced from elsewhere, a stubs library is used. The stubs library is selected according to the depending module’s `sdk_version` property setting. For example, a module that specifies `sdk_version: "current"` uses the public stubs, whereas a module that specifies `sdk_version: "system_current"` uses the system stubs. If an exact match can’t be found, the closest stub library gets used. A `java_sdk_library` that only provides a public API will supply the public stubs for everyone.

![Build flow with Java SDK library](https://source.android.com/static/docs/setup/images/build/java-sdk-library-build-flow.png)
*Figure 1. Build flow with Java SDK library*


## EXAMPLES AND SOURCES

The `srcs` and `api_packages` properties must be present in the `java_sdk_library`.

```json
java_sdk_library {
        name: "com.android.future.usb.accessory",
        srcs: ["src/**/*.java"],
        api_packages: ["com.android.future.usb"],
    }
```

AOSP recommends (but doesn’t require) that new `java_sdk_library` instances explicitly enable the API scopes they want to use. You can also (optionally) migrate existing `java_sdk_library` instances to explicitly enable the API scopes they’ll use:

```json
java_sdk_library {
         name: "lib",
         public: {
           enabled: true,
         },
         system: {
           enabled: true,
         },
         …
    }
```

To configure the `impl` library used for runtime, use all the normal `java_library` properties, such as `hostdex`, `compile_dex`, and `errorprone`.

```json
java_sdk_library {
        name: "android.test.base",

        srcs: ["src/**/*.java"],

        errorprone: {
          javacflags: ["-Xep:DepAnn:ERROR"],
        },

        hostdex: true,

        api_packages: [
            "android.test",
            "android.test.suitebuilder.annotation",
            "com.android.internal.util",
            "junit.framework",
        ],

        compile_dex: true,
    }
```

To configure stubs libraries, use the following properties:

*   `merge_annotations_dirs` and `merge_inclusion_annotations_dirs`.
*   `api_srcs`: The list of optional source files that are part of the API but not part of the runtime library.
*   `stubs_only_libs`: The list of Java libraries that are in the classpath when building stubs.
*   `hidden_api_packages`: The list of package names that must be hidden from the API.
*   `droiddoc_options`: Additional argument for metalava.
*   `droiddoc_option_files`: Lists the files that can be referenced from within `droiddoc_options` using `$(location <label>)`, where `<file>` is an entry in the list.
*   `annotations_enabled`.

The `java_sdk_library` is a `java_library`, but it isn’t a `droidstubs` module and so doesn't support all of the `droidstubs` properties. The following example was taken from the `android.test.mock` library build file.

```json
java_sdk_library {
        name: "android.test.mock",

        srcs: [":android-test-mock-sources"],
        api_srcs: [
            // Note: The following aren’t APIs of this library. Only APIs under the
            // android.test.mock package are taken. These do provide private APIs
            // to which android.test.mock APIs reference. These classes are present
            // in source code form to access necessary comments that disappear when
            // the classes are compiled into a Jar library.
            ":framework-core-sources-for-test-mock",
            ":framework_native_aidl",
        ],

        libs: [
            "framework",
            "framework-annotations-lib",
            "app-compat-annotations",
            "Unsupportedappusage",
        ],

        api_packages: [
            "android.test.mock",
        ],
        permitted_packages: [
            "android.test.mock",
        ],
        compile_dex: true,
        default_to_stubs: true,
    }
```


## MAINTAIN BACKWARD COMPATIBILITY

The build system checks whether the APIs have maintained backward compatibility by comparing the latest API files with the generated API files at build time. The `java_sdk_library` performs the compatibility check using the information provided by `prebuilt_apis`. All libraries built with `java_sdk_library` must have API files in the latest version of `api_dirs` in `prebuilt_apis`. When you release the version, the API lists files and stubs libraries can be obtained with `dist` build with `PRODUCT=sdk_phone_armv7-sdk`.

The `api_dirs` property is list of API version directories in `prebuilt_apis`. The API-version directories must be located at the `Android.bp` directory level.

```json
prebuilt_apis {
       name: "foo",
       api_dirs: [
           "1",
           "2",
             ....
           "30",
           "current",
       ],
    }
```

Configure the directories with the `version/scope/api/` structure under the `prebuilts` directory. `version` corresponds to the API level and `scope` defines whether the directory is public, system, or test.

*   `version/scope` contains Java libraries.
*   `version/scope/api` contains API `.txt` files. Create empty text files named `module_name.txt` and `module_name-removed.txt` here.
    
```
    ├── 30
    │   ├── public
    │   │   ├── api
    │   │   │   ├── android.test.mock-removed.txt
    │   │   │   └── android.test.mock.txt
    │   │   └── android.test.mock.jar
    │   ├── system
    │   │   ├── api
    │   │   │   ├── android.test.mock-removed.txt
    │   │   │   └── android.test.mock.txt
    │   │   └── android.test.mock.jar
    │   └── test
    │       ├── api
    │       │   ├── android.test.mock-removed.txt
    │       │   └── android.test.mock.txt
    │       └── android.test.mock.jar
    └── Android.bp
```
