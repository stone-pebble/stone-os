# AOSP Architecture Overview

**Source:** [Android System Architecture](https://source.android.com/docs/core/architecture)
**Scrape Date:** 2025-10-23

---

## Overview

The Android system architecture is a software stack composed of several layers, each with a distinct purpose. This layered architecture provides a stable framework for application development while abstracting away the complexity of the underlying hardware.

![Android Software Stack](https://source.android.com/static/docs/core/images/android-stack_2x.png)

### 1. The Linux Kernel

At the base of the Android platform is the Linux Kernel. Android relies on the kernel for core system services such as:
-   **Hardware Abstraction:** The kernel manages all the hardware drivers for the device (display, camera, Wi-Fi, etc.), providing a consistent interface to the upper layers.
-   **Process Management:** Manages application processes and their lifecycles.
-   **Memory Management:** Manages system RAM.
-   **Security:** Enforces application sandboxing and permissions at the kernel level.
-   **Networking:** Manages the network stack.

### 2. Hardware Abstraction Layer (HAL)

The HAL provides a standard interface that exposes device hardware capabilities to the higher-level Java API framework. It consists of multiple library modules, each of which implements an interface for a specific type of hardware component, such as the camera or Bluetooth module.

### 3. Native C/C++ Libraries

This layer contains the core C/C++ libraries that perform many of the fundamental operations of the system. These are exposed to developers through the Java application framework. Key libraries include:
-   **`libc`:** The standard C library.
-   **Graphics Libraries:** `OpenGL ES` for 2D and 3D graphics rendering.
-   **Media Framework:** For playback and recording of audio and video.
-   **SQLite:** A lightweight relational database engine used for application data storage.

### 4. Android Runtime (ART)

ART is the managed runtime used by applications and most system services. Its key features are:
-   **Ahead-of-Time (AOT) Compilation:** When an app is installed, ART compiles its DEX bytecode into native machine code for the target device. This results in faster app startup and better performance.
-   **Garbage Collection (GC):** ART manages memory, automatically reclaiming memory that is no longer in use.

### 5. The Java API Framework

This layer provides the high-level building blocks that applications use. These are the APIs that app developers interact with directly. Key components include:
-   **Activity Manager:** Manages the lifecycle of application components.
-   **Window Manager:** Manages windows and the screen.
-   **Content Providers:** Enables applications to share data with each other.
-   **View System:** Provides the UI toolkit for building application interfaces (Buttons, TextViews, etc.).
-   **Notification Manager:** Manages all system notifications.

### 6. System Apps

Android comes with a set of core applications for essential functionality, such as email, SMS messaging, calendars, internet browsing, and contacts.
<!-- StoneOS Note: Our custom applications, like `StoneLauncher` and `StoneSettings`, are **System Apps**. They are built with the OS and are granted privileged permissions. -->

---

## Key Android Boot Processes

### The Zygote Process

When an Android device boots, the `init` process launches a special process called the **Zygote**.
-   **"The Golden Master":** The Zygote initializes a single instance of the Android Runtime (ART) and preloads all the core Java classes and resources that any application will need.
-   **Efficient App Launching:** When you launch a new app, the Zygote **forks** itself, creating a new child process. This new process inherits the already-warmed-up ART instance and all the preloaded classes, making app startup extremely fast. This "copy-on-write" mechanism is a cornerstone of Android's performance.

### The System Server

The very first process forked from the Zygote is the **System Server**.
-   **The Core of the OS:** The System Server is a single, multi-threaded process that runs almost all of the high-level system services mentioned in the Java API Framework (Activity Manager, Window Manager, etc.).
-   **Service Hub:** Applications running in their own processes communicate with the System Server's services via the **Binder IPC** mechanism to request actions, such as drawing a window on the screen or sending a notification.
