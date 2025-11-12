# Feature Launch Flag Overview

**Source:** https://source.android.com/docs/setup/build/feature-flagging
**Scrape Date:** 2025-10-23

---

When adding code into AOSP, use feature launch flags to isolate untested code from tested code. Enable feature launch flags to execute and test your code. Conversely, disable feature launch flags to ensure untested code doesn't execute.

Feature launch flags are used primarily in these two ways:

*   If you're contributing to AOSP, you might be asked by your change's reviewer to implement a feature launch flag so that the feature is tested properly. For further information on branches, see Release lifecycle.
*   Google uses feature launch flags to ensure the Android latest release branch (android16-release) is stable for everyone. If your company keeps a mirror of AOSP and works from that mirror, use feature launch flagging to keep your mirror of AOSP code stable for your development team.

**Note:** Feature launch flagging is part of a new development process called Trunk Stable whereby all official AOSP releases are snapped from a single internal main development branch. To achieve this goal, the main development branch must remain stable at all time. Trunk Stable requires all updates and new features to be flagged so they can, on a case-by-case basis, be included or excluded from the internal main branch before snapping a release. For more on the AOSP release process, see Release lifecycle.

The high-level steps for implementing feature launch flagging are:

1.  For a given code change, determine if you need a flag and, if so, determine the flag type.
2.  Declare the flag.
3.  Wrap your code change in the flag.
4.  Set the flag's value.
5.  Build and test your code.
6.  Change flag values at runtime.
7.  Test code that uses feature release flags

The pages in this section teach you how to perform each of these steps.
