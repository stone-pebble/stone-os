# AOSP `repo` Command Reference

**Source:** [Repo Command Reference](https://gerrit.googlesource.com/git-repo/+/master/docs/manifest-format.md)
**Scrape Date:** 2025-10-23

---

## Overview

The `repo` tool is a Python script that initializes and orchestrates multiple Git repositories (projects) within a single AOSP checkout. It is essential for managing the vast and distributed codebase of Android.

<!-- StoneOS Note: A solid understanding of `repo` is mandatory for our workflow, especially `init` (for using local manifests) and `sync` (for pulling updates). -->

## Key Commands

### `repo init`

**Usage:** `repo init -u URL [OPTIONS]`

**Description:**
Installs Repo in the current directory. This command creates a `.repo/` directory which contains the Git repositories for Repo's own source code and the manifest files that define the AOSP checkout.

**Key Options:**
-   `-u URL`: **(Required)** Specifies the URL of the manifest repository. For AOSP, this is `https://android.googlesource.com/platform/manifest`.
-   `-b REVISION`: Specifies a particular branch or tag in the manifest repository to use (e.g., `android-14.0.0_r61`).
-   `-m FILE`: Selects a specific XML file within the manifest repository. If omitted, it defaults to `default.xml`.

<!-- StoneOS Note: Our setup relies on `repo init` to fetch the main AOSP manifest, after which our local manifest in `.repo/local_manifests/` is used to modify it. -->

---

### `repo sync`

**Usage:** `repo sync [PROJECT_LIST]`

**Description:**
Downloads new changes from the remote repositories and updates the local working files. It essentially performs a `git fetch` across all projects defined in the manifest.

-   On a new project, `repo sync` is equivalent to `git clone`.
-   On an existing project, it is equivalent to `git remote update` followed by `git rebase origin/<branch>`.

**Key Options:**
-   `-j <THREADS>`: Splits the sync across multiple threads for faster completion.
    <!-- StoneOS Note: We **must** use `-j4`. Using a higher number will cause Google's servers to rate-limit us with HTTP 429 errors. -->
-   `-c`: Fetches only the current branch specified in the manifest, significantly reducing download size and time.
-   `-d`: Detaches projects back to the manifest revision. Useful for temporarily leaving a topic branch to return to the "pristine" source state.
-   `-f`: Proceeds with syncing other projects even if one project fails.

---

### `repo start`

**Usage:** `repo start <BRANCH_NAME> [PROJECT_LIST]`

**Description:**
Starts a new topic branch for development, beginning from the revision specified in the manifest. This is the standard way to begin work on a new feature or bug fix.

-   `<BRANCH_NAME>`: A descriptive name for your new branch (e.g., `feature/grayscale-filter`).
-   `[PROJECT_LIST]`: The specific project(s) you want to work on. Use `.` to specify the project in the current directory.

**Example:**
```bash
# Start a new branch named 'my-feature' in the current project
repo start my-feature .
```

---

### `repo upload`

**Usage:** `repo upload [PROJECT_LIST]`

**Description:**
Uploads local commits from your topic branch to the Gerrit code review server. It compares your local branches to the remote and prompts you to select which branches to upload.

---

### `repo forall`

**Usage:** `repo forall [PROJECT_LIST] -c <COMMAND>`

**Description:**
Executes a given shell command in every project specified. This is a powerful tool for running commands across the entire AOSP source tree.

**Example:**
```bash
# See the git status for every project
repo forall -c "git status"
```

---

### `repo status`

**Usage:** `repo status [PROJECT_LIST]`

**Description:**
Compares the working tree to the Git staging area (index) and the most recent commit on the branch (HEAD) for each project. It provides a concise summary of all modified, staged, and untracked files.
