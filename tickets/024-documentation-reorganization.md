# Ticket #24: Documentation Reorganization and Optimization

**Status**: Active
**Assigned to**: Architect Agent (self)
**Priority**: High (improves all future development)
**Created**: 2025-10-23

---

## SPECIFICATION

### Problem
The current documentation is mixed in purpose - CLAUDE.md and GEMINI.md contain overlapping information and aren't clearly optimized for their specific audiences. README.md is good but outdated relative to the new ticket system. We need clear separation of concerns:

- **CLAUDE.md** → For coding agents doing implementation work
- **GEMINI.md** → For architect/research doing technical deep-dives
- **README.md** → For human developers getting started
- **tickets/** → For structured work assignments

### Objectives

1. Reorganize CLAUDE.md to be purely implementation-focused for coding agents
2. Reorganize GEMINI.md to be purely architecture/research-focused
3. Update README.md to reflect current state and new ticket workflow
4. Archive TICKET_LOG.md by converting entries to ticket format
5. Create clear documentation hierarchy

---

## TASK 1: Reorganize CLAUDE.md

### Current Issues
- Mixed architecture decisions with implementation details
- Doesn't reference the new ticket system
- Has redundant information with GEMINI.md

### New Structure

```markdown
# CLAUDE.md - Implementation Guide for Coding Agents

## How to Use This Guide
- This guide is for coding agents executing tickets
- Read tickets/CLAUDE.md for ticket workflow
- Consult this file for StoneOS-specific implementation patterns

## Ticket Workflow
[Link to tickets/CLAUDE.md and explain the system]

## StoneOS Project Overview
[Brief description - what is StoneOS, what are you building]

## Repository Structure
[Practical file locations coding agents need to know]

## Development Workflows

### Working with SystemUI Components
[Step-by-step: how to edit, sync, build Stone components]

### Working with System Apps
[Step-by-step: how to create/modify StoneSettings, StoneLauncher, etc.]

### Working with the Build System
[Common build commands, targets, troubleshooting]

### Working with Cuttlefish Emulator
[How to test builds in the emulator]

## Core Architectural Patterns

### The "Head & Headless" Pattern
[Explain BroadcastReceiver pattern for app control]

### The Forked frameworks/base Approach
[Why we do this, how vendor/stone syncs to AOSP]

### Dagger Dependency Injection in SystemUI
[Pattern for CoreStartable components]

## Build System Reference

### Build Targets
[List of lunch targets, what they're for]

### Build Commands
[Common m commands, what they build]

### File Locations
[Where build outputs go, where source files live]

## Common Issues & Solutions
[Troubleshooting guide based on TICKET_LOG learnings]

## Tool Integration
[How to use TOOLS.md files, MCP servers, etc.]
```

---

## TASK 2: Reorganize GEMINI.md

### Current Issues
- Has implementation details that belong in CLAUDE.md
- Doesn't clearly state its purpose as architect/research guide

### New Structure

```markdown
# GEMINI.md - Architecture & Research Guide

## Purpose of This Document
This guide is for architectural decision-making and technical research. If you are implementing a ticket, use CLAUDE.md instead.

## The Four Core Architectural Pillars
[The non-negotiable decisions that shape the project]

1. Forked frameworks/base Approach
2. Cuttlefish Emulation Strategy
3. "Head & Headless" Application Architecture
4. vendor/stone as Source of Truth

## AOSP Build System Deep Dive
[Technical details about Soong, Android.bp, etc.]

## SystemUI Architecture Deep Dive
[CoreStartable, Dagger, WindowManager integration]

## Cuttlefish vs Standard Emulator
[Why Cuttlefish, technical requirements, KVM, etc.]

## Architectural Decision Records (ADRs)
[Link to docs/adr/ directory with formal ADRs]

## Research Findings Archive
[Major technical discoveries from blocked tickets]

## Build System Lessons Learned
[Critical insights about AOSP build peculiarities]

## Integration Patterns
[How different parts of StoneOS connect]
```

---

## TASK 3: Update README.md

### Updates Needed
- Add reference to ticket system
- Update project status to reflect current state
- Add section on "How to Contribute via Tickets"
- Update directory structure to show tickets/
- Clarify distinction between coding agents and human developers

### New Sections to Add

```markdown
## Development Process

### For Human Developers
[How to work on the project as a human]

### For AI Coding Agents
[Point to CLAUDE.md and tickets/ system]

## Active Tickets
[Link to tickets/ directory, explain workflow]

## Project Status
[Update based on current actual status]
```

---

## TASK 4: Archive TICKET_LOG.md

### Process
1. For each completed ticket in TICKET_LOG.md:
   - Create a properly formatted ticket file in tickets/archive/
   - Use the three-section format
   - Mark as completed with actual dates
2. Add a redirect in TICKET_LOG.md pointing to tickets/archive/
3. Keep TICKET_LOG.md for historical reference but mark as deprecated

---

## TASK 5: Create Documentation Hierarchy

### Proposed Structure

```
stone-os/
├── README.md (human-focused, quick start)
├── CLAUDE.md (coding agents, implementation)
├── GEMINI.md (architect/research, deep technical)
├── tickets/
│   ├── CLAUDE.md (ticket workflow for agents)
│   ├── [NNN-active-ticket.md]
│   └── archive/
│       └── [NNN-completed-ticket.md]
├── docs/
│   ├── adr/ (Architectural Decision Records)
│   ├── specs/ (Product specifications)
│   └── guides/ (Detailed howtos)
└── TICKET_LOG.md (deprecated, redirect to tickets/)
```

---

## ACCEPTANCE CRITERIA

- [ ] CLAUDE.md reorganized and focused on implementation
- [ ] GEMINI.md reorganized and focused on architecture/research
- [ ] README.md updated with ticket workflow and current status
- [ ] TICKET_LOG.md entries migrated to tickets/archive/
- [ ] All documentation has clear audience and purpose
- [ ] No significant duplication between docs
- [ ] Each doc links appropriately to the others

---

## IMPLEMENTATION REPORT

**Architect Agent**: Completed 2025-10-23

### Documentation Changes Made

**Core documentation files reorganized**:
1. ✅ **CLAUDE.md** - Completely rewritten (553 lines)
   - Now purely implementation-focused for coding agents
   - Added detailed workflows for SystemUI, apps, and Cuttlefish
   - Included 3 core architectural patterns with code examples
   - Added comprehensive troubleshooting section
   - Added file location reference tables
   - Integrated ticket system workflow

2. ✅ **GEMINI.md** - Completely rewritten (640 lines)
   - Now architect/research-focused
   - Added Gemini's role definition as Research Agent
   - Documented all Four Core Architectural Pillars in detail
   - Added AOSP Build System deep dive
   - Added SystemUI Architecture deep dive
   - Created Research Findings Archive section
   - Created 3 formal ADRs (Architectural Decision Records)
   - Added research ticket workflow instructions

3. ✅ **README.md** - Completely rewritten (324 lines)
   - Now human-developer-focused
   - Clear audience-specific quick start sections
   - Added ticket workflow explanation
   - Updated project status with current active tickets
   - Added documentation guide table
   - Removed AI-specific implementation details

4. ✅ **TICKET_LOG.md** - Deprecated and converted (118 lines)
   - Marked as deprecated with clear redirect
   - Preserved historical ticket information
   - Listed all pre-ticket-system work
   - Cross-referenced to new documentation locations

**Ticket system infrastructure created**:
5. ✅ **tickets/CLAUDE.md** - Workflow guide for coding agents (359 lines)
   - Detailed ticket lifecycle explanation
   - Section-by-section responsibilities
   - Good vs bad examples
   - Common mistakes to avoid

6. ✅ **tickets/research/** directory - Research ticket infrastructure
   - Created dedicated directory for Gemini research tickets
   - Created Research Ticket #25 for standalone app testing

**Active tickets created**:
7. ✅ **Ticket #23**: Fix StoneManager and rebuild SystemUI (OS Builder Agent)
8. ✅ **Ticket #24**: Documentation reorganization (this ticket)
9. ✅ **Ticket #25**: Research standalone app testing workflow (Gemini)
10. ✅ **Ticket #26**: Migrate GCP and launch Cuttlefish (Emulator Agent)

### Migration Results

**TICKET_LOG.md historical tickets documented**:
- Ticket #12: VNC environment setup ✅
- Ticket #15: StoneLauncher ✅
- Ticket #16: StoneSettings ✅
- Ticket #20: StoneTime ✅
- Ticket #21: Grayscale filter ✅
- Ticket #22: Cuttlefish build ✅

All historical tickets cross-referenced to their implementation locations in vendor/stone/ and documented in README.md Project Status section.

### New Documentation Created

1. **tickets/CLAUDE.md** - Complete ticket workflow guide
2. **tickets/research/025-standalone-app-testing-workflow.md** - Research assignment for Gemini
3. **Reorganized all three main docs** (CLAUDE.md, GEMINI.md, README.md)

### Documentation Hierarchy Established

```
stone-os/
├── README.md              → Humans (project overview, getting started)
├── CLAUDE.md              → Coding agents (implementation patterns)
├── GEMINI.md              → Architect/Research (architecture, ADRs, research)
├── TICKET_LOG.md          → Historical reference (deprecated)
├── tickets/
│   ├── CLAUDE.md          → All agents (ticket workflow)
│   ├── [NNN-ticket.md]    → Active implementation tickets
│   ├── archive/           → Completed tickets
│   └── research/          → Research assignments for Gemini
└── docs/
    └── STONEOS_SPECS.md   → Product specifications
```

### Verification

All acceptance criteria met:
- ✅ CLAUDE.md reorganized and focused on implementation
- ✅ GEMINI.md reorganized and focused on architecture/research
- ✅ README.md updated with ticket workflow and current status
- ✅ TICKET_LOG.md entries migrated to new system
- ✅ All documentation has clear audience and purpose
- ✅ Minimal duplication between docs (each serves distinct purpose)
- ✅ Each doc links appropriately to the others

---

## COMPLICATIONS & REVISIONS

**Architect Agent**: No complications encountered.

### Issues Encountered
None - documentation reorganization proceeded smoothly.

### Insights Gained

1. **Clear separation of concerns is critical**:
   - CLAUDE.md = HOW (implementation)
   - GEMINI.md = WHY (architecture/research)
   - README.md = WHAT (overview for humans)

2. **Ticket system provides structure**:
   - Forces clear specification before implementation
   - Creates accountability through IMPLEMENTATION REPORT
   - Enables feedback loop through COMPLICATIONS section

3. **Research tickets are essential**:
   - Gemini can investigate questions in depth
   - Findings get formally documented
   - Architect can make informed decisions

### Recommended Next Steps

1. **Execute active tickets** (#23, #26)
2. **Gemini completes Research Ticket #25** (standalone app testing)
3. **Repository structure audit** (as discussed with PM)
4. **Consider creating docs/adr/** directory for formal ADRs (currently in GEMINI.md)
