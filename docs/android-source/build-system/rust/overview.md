# Android Rust Introduction

**Source:** https://source.android.com/docs/setup/build/rust/building-rust-modules/overview
**Scrape Date:** 2025-10-23

---

The Android platform provides support for developing native OS components in Rust, a modern systems-programming language that provides memory safety guarantees with performance equivalent to C/C++. Rust uses a combination of compile-time checks that enforce object lifetime and ownership, and runtime checks that ensure valid memory accesses, thereby eliminating the need for a garbage collector.

Rust provides a range of modern language features which allow developers to be more productive and confident in their code:

*   **Safe concurrent programming** - The ease with which this allows users to write efficient, thread-safe code has given rise to Rust’s Fearless Concurrency slogan.
*   **Expressive type system** - Rust helps prevent logical programming bugs by allowing for highly expressive types (such as Newtype wrappers, and enum variants with contents).
*   **Stronger Compile-time Checks** - More bugs caught at compile-time increases developer confidence that when code compiles successfully, it works as intended.
*   **Built-in Testing Framework** - Rust provides a built-in testing framework where unit tests can be placed alongside the implementation they test, making unit testing easier to include.
*   **Error handling enforcement** - Functions with recoverable failures can return a Result type, which will be either a success variant or an error variant. The compiler requires callers to check for and handle the error variant of a Result enum returned from a function call. This reduces the potential for bugs resulting from unhandled failures.
*   **Initialization** - Rust requires every variable to be initialized to a legal member of its type before use, preventing an unintentional initialization to an unsafe value.
*   **Safer integer handling** - All integer-type conversions are explicit casts. Developers can’t accidentally cast during a function call when assigning to a variable, or when attempting to do arithmetic with other types. Overflow checking is on by default in Android for Rust, which requires overflow operations to be explicit.

For more information, see the series of blog posts on Android Rust support:

*   [Rust in the Android Platform](https://security.googleblog.com/2021/04/rust-in-android-platform.html)
    Provides an overview on why the Android team introduced Rust as a new platform language.
*   [Integrating Rust into the Android Open Source Project](https://security.googleblog.com/2021/06/integrating-rust-into-android-open.html)
    Discusses how Rust support has been introduced to the build system, and why certain design decisions were made.
*   [Rust/C++ interop in the Android Platform](https://security.googleblog.com/2022/09/rustc-interop-in-android-platform.html)
    Discusses the approach to Rust/C++ interoperability within Android.
