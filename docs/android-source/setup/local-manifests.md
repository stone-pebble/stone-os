# AOSP Local Manifests

**Source:** [Repo Manifest Format Documentation](https://gerrit.googlesource.com/git-repo/+/master/docs/manifest-format.md)
**Scrape Date:** 2025-10-23

---

## Overview

Local manifests are XML files stored in `.repo/local_manifests/` that allow you to customize the AOSP source checkout without altering the main `default.xml` manifest. This is the primary mechanism for adding new projects, replacing existing AOSP projects with your own forks, and making other structural changes to the source tree.

<!-- StoneOS Note: This is a critical component of our development workflow, allowing us to substitute our forked frameworks/base. -->

## Usage

Local manifest files are loaded automatically by `repo sync` in alphabetical order. They can contain any valid manifest elements, but are most commonly used for adding or modifying projects.

**Location:** `.repo/local_manifests/`

You can have multiple files in this directory (e.g., `01-removals.xml`, `02-additions.xml`) and they will be loaded in order.

## Key Elements for StoneOS

### Replacing an AOSP Project with a Fork

This is the most important pattern for StoneOS. To replace a stock AOSP project (like `platform/frameworks/base`) with your own GitHub fork, you use two tags: `<remove-project>` and `<project>`.

**Example (`.repo/local_manifests/stoneos.xml`):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<manifest>
  <!-- First, remove the default AOSP project -->
  <remove-project name="platform/frameworks/base" />

  <!-- Then, add our own fork in its place -->
  <project path="frameworks/base"
           name="stone-pebble/stoneos-frameworks"
           remote="github"
           revision="android-14.0.0_r61" />

  <!-- It's good practice to define the remote if it's not standard -->
  <remote name="github"
          fetch="https://github.com/" />
</manifest>
```

-   `<remove-project name="..." />`: This tag tells `repo` to ignore the project with the specified name from the main manifest.
-   `<project ... />`: This tag adds a new project.
    -   `path`: The destination directory in the AOSP source tree. This **must** match the path of the project you removed.
    -   `name`: The repository name on the remote (e.g., your GitHub repository).
    -   `remote`: The name of the remote server, as defined by a `<remote>` tag.
    -   `revision`: The specific branch or tag to check out.

### Adding a New Project

To add a new project that doesn't exist in the main manifest (for example, our custom applications in `vendor/stone`), you simply use the `<project>` tag.

```xml
<manifest>
  <project path="vendor/stone"
           name="stone-pebble/vendor-stone"
           remote="github"
           revision="main" />
</manifest>
```
<!-- StoneOS Note: This is how we integrate our custom applications and makefiles into the AOSP build. The `vendor/stone` repository is synced into the build tree, and the Soong build system then discovers the `Android.bp` files within it. -->

### The `<copyfile>` Element

The `<copyfile>` element is a useful utility for copying a file from a project into a specific location in the source tree during `repo sync`.

-   `src`: The path to the file within the project.
-   `dest`: The destination path, relative to the top of the AOSP source tree.

**Example:**
```xml
<manifest>
  <project path="vendor/scripts" name="my/scripts" remote="github" revision="main">
    <copyfile src="setup.sh" dest="setup-dev-env.sh" />
  </project>
</manifest>
```
This would copy `vendor/scripts/setup.sh` to `~/aosp/setup-dev-env.sh`.

---

## Best Practices

1.  **Use Multiple Files:** For complex projects, split your manifests by function (e.g., `removals.xml`, `device-specific.xml`, `custom-apps.xml`).
2.  **Be Specific with Revisions:** Pin projects to specific branches or commit hashes (`revision="..."`) to ensure a reproducible build.
3.  **Define Remotes:** Always include a `<remote>` tag for any non-AOSP remotes like GitHub or GitLab.
