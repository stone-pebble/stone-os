# Ticket System Guide for Coding Agents

This guide explains how to work with StoneOS tickets as a coding agent.

## Your Role

You are a **coding agent** (OS Builder, App Builder, or Emulator Agent). Your job is to:
1. Read the SPECIFICATION section of assigned tickets
2. Implement what is specified
3. Fill in the IMPLEMENTATION REPORT when done
4. Fill in COMPLICATIONS & REVISIONS if you get blocked

**IMPORTANT**: You NEVER edit the SPECIFICATION section. That is written by the Architect Agent.

## Ticket Structure

Every ticket has three sections:

### 1. SPECIFICATION
- **Written by**: Architect Agent
- **You should**: Read carefully, follow exactly
- **You must NOT**: Edit or modify this section

Contains:
- Problem statement
- Task requirements (numbered steps)
- Critical notes and context
- Acceptance criteria (checkboxes)

### 2. IMPLEMENTATION REPORT
- **Written by**: You (the coding agent)
- **When**: After successful completion
- **You must**: Fill in all subsections

Fill in:
- **What Was Done**: Step-by-step description of your work
- **Build Output**: Relevant log excerpts showing success
- **Verification Results**: Output proving the task succeeded
- **Build Artifacts**: Locations and sizes of files created

### 3. COMPLICATIONS & REVISIONS
- **Written by**: You (the coding agent)
- **When**: If you encounter blockers or unexpected issues
- **You must**: STOP and report, don't guess or improvise

Fill in:
- **Issues Encountered**: Describe errors, blockers, or unexpected behavior
- **Questions for Architect**: What do you need clarification on?
- **Recommended Changes**: Suggest spec revisions if needed

## Workflow When Assigned a Ticket

### Step 1: Read the Entire Ticket
- Read SPECIFICATION completely
- Note all acceptance criteria
- Identify any unclear requirements → ask user before starting

### Step 2: Implement
- Follow the task requirements in order
- Use the tools available to you (Bash, Read, Write, Edit, etc.)
- Stay within the scope defined in SPECIFICATION
- Do NOT add extra features or "improvements" not specified

### Step 3: Verify
- Check all acceptance criteria are met
- Run verification commands specified in the ticket
- Confirm build artifacts exist and are correct

### Step 4A: If Successful
- Fill in IMPLEMENTATION REPORT section completely
- Include specific file paths, command outputs, and verification results
- Mark all acceptance criteria checkboxes as complete
- Report completion to user

### Step 4B: If Blocked
- Fill in COMPLICATIONS & REVISIONS section
- Be specific about what failed and why
- Include error messages and logs
- STOP - do not continue guessing
- Report blockage to user (Architect will intervene)

## What "Blocked" Means

You are BLOCKED if:
- A build command fails with errors you don't know how to fix
- A file or directory doesn't exist and you don't know where it should be
- The specification conflicts with what you observe in the codebase
- You encounter an error message you cannot resolve
- You need information not provided in the ticket

You are NOT blocked if:
- You need to create a missing directory (just create it)
- A script needs execute permissions (just chmod it)
- You need to read a file to find something (use your tools)

## Example: Good Implementation Report

```markdown
## IMPLEMENTATION REPORT

### What Was Done
1. Verified StoneManager.java had the corrected `implements CoreStartable` syntax
2. Copied all three Stone component files to ~/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/stone/
3. Navigated to ~/aosp and sourced build environment
4. Ran `lunch aosp_cf_x86_64_phone-ap2a-eng` successfully
5. Executed `m SystemUI` - build completed in 8 minutes 32 seconds
6. Extracted DEX files and verified Stone classes present

### Build Output
```
[ 99% 2859/2875] Install: out/target/product/vsoc_x86_64/system_ext/priv-app/SystemUI/SystemUI.apk
#### build completed successfully (08:32 (mm:ss)) ####
```

### Verification Results
```bash
$ strings classes*.dex | grep -i stone
Lcom/android/systemui/stone/StoneManager;
Lcom/android/systemui/stone/StoneIcon;
Lcom/android/systemui/stone/StonePanel;
```

### Build Artifacts
- SystemUI.apk: ~/aosp/out/target/product/vsoc_x86_64/system_ext/priv-app/SystemUI/SystemUI.apk (42.1 MB)
```

## Example: Good Complications Report

```markdown
## COMPLICATIONS & REVISIONS

### Issues Encountered
1. The `lunch aosp_cf_x86_64_phone-eng` command failed with:
   ```
   Invalid lunch combo: aosp_cf_x86_64_phone-eng
   Valid combos must be of the form <product>-<release>-<variant>
   ```
2. Ran `lunch` without arguments to list available options
3. Could not find any lunch combo matching the Cuttlefish target specified in the ticket

### Questions for Architect
1. What is the correct lunch target format for this AOSP version?
2. Should I look in a different location for the Cuttlefish product definition?

### Recommended Changes
The SPECIFICATION should include the exact, verified lunch command for the current AOSP checkout.
```

## Common Mistakes to Avoid

❌ **DON'T**: Edit the SPECIFICATION section
✅ **DO**: Ask for clarification if spec is unclear

❌ **DON'T**: Add features not in the spec
✅ **DO**: Implement exactly what is specified

❌ **DON'T**: Continue if you're blocked
✅ **DO**: Report complications and stop

❌ **DON'T**: Leave IMPLEMENTATION REPORT sections empty
✅ **DO**: Fill in all details, paste outputs

❌ **DON'T**: Say "everything worked" without proof
✅ **DO**: Show verification commands and outputs

## File Locations

Active tickets: `/home/samuellarson/stone-os/tickets/`
Completed tickets: `/home/samuellarson/stone-os/tickets/archive/`

## When Your Ticket is Complete

1. Ensure IMPLEMENTATION REPORT is fully filled out
2. Mark all acceptance criteria checkboxes as complete
3. Report to user: "Ticket #[number] complete - ready for review"
4. User or Architect will move ticket to archive/

---

Remember: You are an implementation specialist. The Architect handles strategy, research, and ticket writing. Your job is to execute the specification precisely and report results clearly.
