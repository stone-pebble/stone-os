# AOSP SystemUI Architecture

**Source:** [SystemUI CoreStartable Documentation](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/packages/SystemUI/docs/corestartable.md)
**Scrape Date:** 2025-10-23

---

## Overview

`SystemUI` is the Android system process that manages all UI elements that are not part of a standard application. This includes critical components like:
-   Status Bar
-   Navigation Bar
-   Notification Shade
-   Lock Screen (Keyguard)
-   Volume UI
-   Recent Apps Screen

It is a privileged, persistent process that is started by the `SystemServer` during the device boot sequence.

<!-- StoneOS Note: Our core UI components (`StoneManager`, `StoneIcon`, `StonePanel`) are implemented as a `SystemUI` modification. This is why our development is centered around a fork of `frameworks/base`. -->

## The `CoreStartable` Interface

The modern entry point for adding new, distinct features to `SystemUI` is the `CoreStartable` interface. Classes that implement this interface act as "mini-services" that are initialized when `SystemUI` starts.

**Key Characteristics:**
-   It is an **interface**, not a base class. You must `implement` it.
-   It has a single lifecycle method: `start()`. This method is called by `SystemUIApplication` on boot.
-   `CoreStartable`s do not have their own `Context` like an Activity or Service. The `Context` must be provided via dependency injection.

<!-- StoneOS Note: Our `StoneManager` is a `CoreStartable`. This is the correct, modern AOSP pattern for integrating our custom UI. -->

## Dependency Injection with Dagger

`SystemUI` uses the **Dagger** framework for dependency injection. This is how `CoreStartable`s and other components receive their dependencies (like `Context`, `WindowManager`, etc.).

The standard pattern for a `CoreStartable` is:

1.  **Annotate the Class:** The class itself is annotated with `@SysUISingleton` to indicate that only one instance should be created for the entire `SystemUI` process.

2.  **Use Constructor Injection:** The constructor is annotated with `@Inject`. The parameters of the constructor are the dependencies that Dagger will provide.

    ```java
    @SysUISingleton
    public class MyComponent implements CoreStartable {
        private final Context mContext;
        private final WindowManager mWindowManager;

        @Inject
        public MyComponent(Context context, WindowManager windowManager) {
            mContext = context;
            mWindowManager = windowManager;
        }

        @Override
        public void start() {
            // ... initialization logic using mContext and mWindowManager ...
        }
    }
    ```

3.  **Bind the `CoreStartable`:** To make `SystemUI` aware of the new component, it must be "bound" into Dagger's object graph. This is done in a Dagger Module, typically `SystemUICoreStartableModule.kt`.

    ```kotlin
    // In SystemUICoreStartableModule.kt
    @Binds
    @IntoMap
    @ClassKey(MyComponent::class)
    abstract fun bindMyComponent(sysui: MyComponent): CoreStartable
    ```
    This tells Dagger that `MyComponent` is a `CoreStartable` and that it should be instantiated and started along with all the others.

<!-- StoneOS Note: This exact pattern is how our `StoneManager` is integrated, making it a first-class citizen within the `SystemUI` process. -->
