# StoneOS AOSP FAQ

This document provides direct, concise answers to the most critical technical questions related to our AOSP development workflow. It is based on the curated documentation in this directory.

---

### **1. Local Manifests**

**Q: What is the official syntax for `.repo/local_manifests/`?**
**A:** Local manifests are XML files. The core structure involves using `<remove-project>` to remove a default AOSP repository and `<project>` to add a new one in its place. Remotes like GitHub should be defined with a `<remote>` tag.
*See: [Local Manifests Documentation](./setup/local-manifests.md)*

**Q: How do we replace AOSP projects with our GitHub forks?**
**A:** Use `<remove-project name="<aosp-project-name>" />` followed by `<project path="<same-path>" name="<github-repo-name>" remote="github" revision="<branch>" />`. This is the foundational mechanism for our `frameworks/base` fork.
*See: [Local Manifests Documentation](./setup/local-manifests.md)*

---

### **2. Soong Build System (`Android.bp`)**

**Q: How do glob patterns (`srcs: ["**/*.java"]`) work?**
**A:** Globs are evaluated at **parse-time**, before any compilation begins. This means that for a new file to be included by a glob, it must physically exist in the source tree *before* the build command (`m`) is run. You cannot add files during the build.
*See: [Soong Build System Overview](./build-system/soong-overview.md)*

**Q: When should we use `android_app` vs `android_library`?**
**A:**
-   `android_app`: Use this for a final, installable application (`.apk`). This is for our system apps like `StoneLauncher` and `StoneSettings`.
-   `android_library`: Use this for a bundle of code that is not an app itself but is meant to be used by other modules.
*See: [Android.bp Syntax Reference](./build-system/android-bp-syntax.md)*

---

### **3. Lunch Targets**

**Q: What is the official format?**
**A:** The format is `<product>-<release>-<variant>`.
-   `product`: The device or build configuration (e.g., `aosp_cf_x86_64_phone`).
-   `release`: The Android version code (e.g., `ap2a` for Android 14 QPR2).
-   `variant`: The build type (e.g., `eng`, `user`, `userdebug`).

**Q: What is the difference between `eng`, `user`, and `userdebug`?**
**A:**
-   `eng` (Engineering): The development configuration. Has maximum debugging tools, root access is enabled by default, and is not performance-optimized. **This is what we use for development.**
-   `userdebug`: Similar to `user` but with root access enabled and more debugging capabilities. Good for testing release candidates.
-   `user`: The official release configuration. Minimal debugging tools, no root access. This is what is shipped to end-users.

---

### **4. Cuttlefish**

**Q: What are the official installation instructions?**
**A:** The correct method is to download and install the official, pre-built Debian host packages (`cuttlefish-common`, `cuttlefish-user`) from Google's CI server. Building the tools from the AOSP source is **not** the recommended approach for setting up the host.
*See: [Cuttlefish: Setup and Installation](./devices/cuttlefish/setup.md)*

**Q: What are the system requirements?**
**A:** The absolute, non-negotiable requirement is **KVM (Kernel-based Virtual Machine)** for hardware acceleration. On a cloud VM, this requires **Nested Virtualization** to be enabled on the instance.
*See: [Cuttlefish: Setup and Installation](./devices/cuttlefish/setup.md)*

**Q: How do we launch our custom build?**
**A:** After installing the host packages, navigate to the AOSP build output directory (`~/aosp/out/target/product/vsoc_x86_64/`) and run the `launch_cvd` command. It automatically finds and uses the images in that directory.
*See: [Cuttlefish: Launching and Usage](./devices/cuttlefish/launching.md)*

---

### **5. Repository Management (`repo`)**

**Q: What are the best practices for `repo sync`?**
**A:**
-   Always use `-j4` to avoid being rate-limited by Google's servers. Using a higher number will result in HTTP 429 errors.
-   Use `-c` (current branch) to download only the branch specified in the manifest, which is significantly faster and saves disk space.
