# Research Ticket #27: Scrape and Organize Android Source Documentation

**Status**: Active
**Assigned to**: Gemini (Research Agent)
**Priority**: High (foundational knowledge base)
**Created**: 2025-10-23

---

## RESEARCH QUESTION

**Primary objective**: Scrape, organize, and create a comprehensive local knowledge base from the official Android Open Source Project (AOSP) documentation at https://source.android.com/docs/setup

**Why this is critical**:
- We're doing **fork-based AOSP development** - modifying frameworks/base directly
- Official Google documentation is the authoritative source for:
  - Build system (Soong, Android.bp)
  - Repository management (repo tool, manifest files)
  - SystemUI architecture
  - Testing and deployment
  - Cuttlefish virtual device
- Having this documentation locally indexed will:
  - Speed up research for future tickets
  - Provide authoritative answers to technical questions
  - Reduce reliance on potentially outdated Stack Overflow answers
  - Enable offline reference

**Context**:
- Our development flow is built around the official Google methodology
- We use `repo` tool for AOSP source management
- We use Soong build system (not custom build scripts)
- We follow AOSP conventions for SystemUI integration
- Cuttlefish is Google's official virtual device

---

## SCOPE

### Pages to Scrape

Start with the following critical sections from https://source.android.com/docs/:

1. **Setup Section** (`/docs/setup`)
   - Requirements
   - Download and build
   - Running builds
   - Building kernels
   - Known issues

2. **Core Topics** (`/docs/core`)
   - Architecture
   - Permissions
   - Graphics
   - Media

3. **Devices Section** (`/docs/devices`)
   - Cuttlefish Virtual Device (CRITICAL)
   - Testing
   - Debugging

4. **Source Code Section** (`/docs/setup/about`)
   - Code lines, branches, and builds
   - Codenames, tags, and build numbers
   - Life of a patch
   - Submitting patches
   - Local manifests (we use this!)

5. **Build System** (`/docs/setup/build`)
   - Building Android
   - Soong build system (CRITICAL - we use Android.bp)
   - Make files vs Blueprints
   - Build variants (eng, user, userdebug)

### Deliverables

1. **Organized Documentation Archive**
   - Create `docs/android-source/` directory
   - Subdirectories by topic (setup/, core/, devices/, build-system/)
   - Markdown files for each major page
   - Preserve code examples exactly as shown
   - Include links to original source

2. **Quick Reference Guide**
   - `docs/android-source/INDEX.md` - Master index with all topics
   - Key sections highlighted for StoneOS development:
     - Forked repo workflow
     - Soong/Android.bp
     - Cuttlefish setup and usage
     - SystemUI development
     - Local manifests

3. **StoneOS-Specific Annotations**
   - Where relevant, add notes like:
     ```
     <!-- StoneOS Note: We use this pattern for vendor/stone integration -->
     ```
   - Highlight sections particularly relevant to our fork-based approach
   - Note any differences between docs and our implementation

---

## SCRAPING METHODOLOGY

### Approach

1. **Manual curation preferred over automated scraping**
   - Focus on quality and relevance
   - Extract key information, not entire HTML
   - Convert to clean Markdown
   - Preserve code examples

2. **Prioritize these topics first** (for immediate use):
   - Local manifests (we use `.repo/local_manifests/stoneos.xml`)
   - Soong build system and Android.bp syntax
   - Cuttlefish virtual device documentation
   - Building for specific targets (lunch command)
   - SystemUI architecture (if documented)

3. **Tools you can use**:
   - WebFetch tool to retrieve pages
   - Manual markdown conversion for clean output
   - Organize by topic, not by URL structure

### Quality Standards

- **Accurate**: Preserve technical accuracy from source
- **Relevant**: Focus on sections we actually use
- **Organized**: Logical directory structure
- **Searchable**: Good filenames, clear headers
- **Maintained**: Note scrape date, link to source

---

## DESIRED OUTPUT STRUCTURE

```
docs/android-source/
├── INDEX.md                          # Master index
├── setup/
│   ├── requirements.md
│   ├── downloading-source.md         # repo init, repo sync
│   ├── building-android.md           # lunch, m commands
│   └── local-manifests.md            # LOCAL MANIFEST USAGE (critical!)
├── build-system/
│   ├── soong-overview.md             # CRITICAL
│   ├── android-bp-syntax.md          # CRITICAL
│   ├── build-variants.md             # eng vs user vs userdebug
│   └── building-modules.md           # m SystemUI, m StoneSettings
├── devices/
│   ├── cuttlefish/
│   │   ├── setup.md                  # CRITICAL
│   │   ├── launching.md              # launch_cvd
│   │   └── debugging.md
│   └── testing.md
├── core/
│   ├── architecture-overview.md
│   ├── systemui/                     # If docs exist
│   └── permissions.md
└── reference/
    ├── repo-command.md               # repo tool reference
    ├── lunch-targets.md              # product-release-variant format
    └── common-commands.md            # Quick reference
```

---

## SPECIFIC QUESTIONS TO ANSWER

While scraping, please explicitly document answers to these questions (create a separate `docs/android-source/STONEOS_FAQ.md`):

1. **Local Manifests**:
   - Official syntax for `.repo/local_manifests/`
   - How to remove/replace AOSP projects
   - How to add GitHub forks
   - Best practices for manifest organization

2. **Soong Build System**:
   - Android.bp glob pattern behavior
   - How srcs arrays are evaluated (parse-time vs compile-time)
   - Static vs shared libraries in Android.bp
   - When to use android_app vs android_library

3. **Lunch Targets**:
   - Official format: `<product>-<release>-<variant>`
   - How to find available lunch combos
   - What `ap2a` release code means
   - Difference between `eng`, `user`, `userdebug`

4. **Cuttlefish**:
   - Official installation instructions
   - System requirements (KVM, etc.)
   - launch_cvd command options
   - Web UI access
   - Debugging and logging

5. **Repository Management**:
   - `repo sync` best practices
   - Dealing with local modifications
   - Forked repository workflow
   - Branch and tag management

---

## SUCCESS CRITERIA

- [ ] `docs/android-source/` directory created with organized content
- [ ] INDEX.md master index with all topics
- [ ] STONEOS_FAQ.md answers the 5 specific question areas
- [ ] Local manifests documentation complete (we use this!)
- [ ] Soong/Android.bp documentation complete (we use this!)
- [ ] Cuttlefish documentation complete (we use this!)
- [ ] All code examples preserved exactly
- [ ] All content cites original source URL
- [ ] StoneOS-specific annotations added where relevant

---

## RESEARCH FINDINGS

**Gemini (Research Agent)**: Fill this section when research is complete.

### Summary
[Overview of documentation scraped and organized]

### Key Discoveries
[Any surprising or particularly useful information found]

### Documentation Structure Created
[List of files and directories created]

### Answers to Specific Questions
[Provide answers to the 5 question areas, or link to STONEOS_FAQ.md]

### Gaps in Official Documentation
[Topics we need but aren't well documented by Google]

### Recommendations for CLAUDE.md/GEMINI.md Updates
[Based on official docs, suggest improvements to our guides]

### Next Steps
[What additional documentation should be gathered]

---

## TIMELINE

**Phase 1** (Immediate): Priority topics
- Local manifests
- Soong/Android.bp
- Cuttlefish
- Build commands

**Phase 2** (Follow-up): Supporting topics
- Architecture
- Testing
- Debugging
- Advanced build topics

Start with Phase 1 - it's sufficient for current development needs.

---

## NOTES

- This is a **one-time research effort** with ongoing maintenance
- Future tickets can reference this local documentation
- Keep scrape date noted for each file (Android docs do update)
- Focus on **Android 14** specific information where available
- If docs are version-agnostic, note that too

**The goal**: Create an authoritative, local, organized reference that supports our fork-based AOSP development workflow.
