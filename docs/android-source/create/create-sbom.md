# Create a Software Bill of Materials (SBOM)

**Source:** https://source.android.com/docs/setup/create/create-sbom
**Scrape Date:** 2025-10-23

---

In February 2022, the National Institute of Standards and Technology (NIST) published version 1.1 of the Secure Software Development Framework (SSDF), a set of comprehensive guidelines on secure software development practices in response to the 2021 Cybersecurity Executive Order (EO) 14028.

As part of these requirements, the US government might request a software bill of materials (SBOM), which lists components of a software release.

**Note:** SBOMs generated using the steps on this page include the Minimal Elements for a Software Bill of Materials (SBOM) as published by the National Telecommunications and Information Administration (NTIA). Additionally, Android-based SBOM tooling and product SBOMs are compatible with the Software Package Data Exchange (SPDX) 2.3 format for communicating the component and metadata information associated with software packages.

SBOMs are automatically generated for Android Continuous Integration (Android CI) builds. If you use one of the CI builds, use the following steps to obtain an SBOM for a build. Otherwise, follow the steps to generate a custom SBOM.


## OBTAIN A PREGENERATED SBOM

To obtain a pregenerated SBOM:

1.  In your browser, navigate to ci.android.com.

2.  In the **Enter a branch name** field, type `aosp-android-latest-release`.

3.  For any of the builds with green status, click the **View artifacts** down arrow. The Build artifacts screen appears.

4.  In the Build artifacts screen, use a find command to locate the SBOM JSON folder (CTRL+F or CMD+F).


## GENERATE A CUSTOM SBOM

For any additions to the platform, including any binary or build and release tool chains, you must provide a SBOM representation of your product that meets the Minimal Elements for a Software Bill of Materials (SBOM). To generate a custom SBOM:

1.  Run the following commands to set up your environment and build the SBOM:
    
    ```bash
    $ source build/envsetup.sh
    $ lunch TARGET
    $ m sbom # Generates an SBOM
    ```
    
    The `TARGET` refers to the same build target that you are using to build Android, such as `aosp_arm64-userdebug`.

2.  To ensure the SBOM built correctly, execute:
    
    ```bash
    $ ls out/dist/sbom*
    ```
