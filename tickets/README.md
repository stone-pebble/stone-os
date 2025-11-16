# Stone Launcher - Implementation Tickets

This directory contains implementation tickets for building the Stone Launcher standalone application.

---

## Ticket Organization

Tickets are organized into folders:

- **/outstanding/** - Tickets that are "Not Started", "In Progress", or "Blocked"
- **/completed/** - Tickets that have been fully implemented and verified
- **TICKET_TEMPLATE.md** - Template for creating new tickets (stays in root)

---

## IMPORTANT: Native Kotlin Approach

**Implementation Note**: Stone Launcher is being built as a **native Kotlin Android application**, NOT React Native.

**Key Points**:
- **UI**: Native Android Views (Activities, Fragments, XML layouts)
- **Language**: Kotlin
- **Design**: Clone of stone-web-app-proto (grayscale, minimalist)
- **AI Agents**: Run in separate server (agents.js), NOT embedded in app
- **LiveKit**: Android SDK client connects to agents.js server

**Existing tickets may reference React Native or need Kotlin revision** - this is legacy documentation. All new work should use native Kotlin patterns shown in `/docs/LAUNCHER_REQUIREMENTS.md`.

**See**: `/docs/DEVELOPMENT_ROADMAP.md` for the complete development sequence.

---

## Quick Start

1. **Read the architecture documents first**:
   - `/docs/DEVELOPMENT_ROADMAP.md` - **START HERE** - Development sequence
   - `/docs/LAUNCHER_ARCHITECTURE.md` - High-level design and philosophy
   - `/docs/LAUNCHER_REQUIREMENTS.md` - Developer implementation guide (Kotlin patterns)
   - `/docs/TOOLS.md` - Intent API specification

2. **Pick a ticket to work on** (see Priority Order below)

3. **Follow the ticket template** - Each ticket has:
   - Background and requirements
   - Step-by-step implementation plan
   - Testing criteria
   - Acceptance criteria

4. **Update ticket status** as you work:
   - Not Started → In Progress → Completed (or Blocked)

---

## Priority Order (Recommended Sequence)

### Phase 1: Foundation

These must be completed first as they're dependencies for everything else:

1. **TICKET_001**: Intent API Foundation (CRITICAL)
   - Creates the BroadcastReceiver that handles all "headless" control
   - Blocks: All other Intent API features

2. **TICKET_002**: WiFi Controller (HIGH - Reference Implementation)
   - Demonstrates the complete "Head & Headless" pattern
   - Reference for all future features

### Phase 2: Core Apps

High-value apps that can be built independently:

3. **TICKET_003**: TASK App - App Launcher (HIGH)
   - Simple, no special permissions
   - Enables "open X" voice commands

4. **TICKET_004**: SET App - Settings Control (HIGH)
   - WiFi, Bluetooth, brightness, volume
   - Demonstrates permission handling

5. **TICKET_005**: Permission Management System (HIGH)
   - Reusable permission gates
   - Onboarding flow

### Phase 3: Communications & Essential Apps

6. **TICKET_006**: CONNECT App - Phase 1 (CRITICAL)
   - Contacts, Dialer, SMS
   - Most complex app

7. **TICKET_007**: TICK App - Time Management (HIGH)
   - Alarms, timers, world clock

8. **TICKET_008**: PLAN App - Calendar (HIGH)
   - Calendar integration
   - Goal tracking

### Phase 4: Advanced Features

9. **TICKET_009**: GO App - Navigation (MEDIUM)
   - Google Maps integration
   - Location services

10. **TICKET_010**: ASK App - Search & Knowledge (MEDIUM)
    - Perplexity integration
    - Web search

11. **TICKET_011**: LISTEN App - Music Control (MEDIUM)
    - Spotify integration
    - OAuth flow

12. **TICKET_012**: THINK App - Notes (MEDIUM)
    - Local or Notion integration
    - Voice-to-text

### Phase 5: Polish & Advanced

13. **TICKET_013**: REFLECT App - Activity Logging (LOW)
    - Notification listener
    - AI summaries

14. **TICKET_014**: Notification Aggregation (MEDIUM)
    - AI-written summaries
    - Unlock screen

15. **TICKET_015**: FUND App - Wallet Integration (LOW)
    - Android Pay
    - Banking app access

16. **TICKET_016**: LOOK App - Digital Library (LOW)
    - Project Gutenberg
    - Book reader

---

## Ticket Status Overview

| Ticket | Status | Priority | Dependencies |
|--------|--------|----------|--------------|
| #001 Intent API Foundation | Not Started | CRITICAL | None |
| #002 WiFi Controller | Not Started | HIGH | #001 |
| #003 TASK App | Not Started | HIGH | #001 |
| #004 SET App | Not Started | HIGH | #001, #002 |
| #005 Permission System | Not Started | HIGH | None |
| #006 CONNECT App | Not Started | CRITICAL | #001, #005 |
| #007 TICK App | Not Started | HIGH | #001 |
| #008 PLAN App | Not Started | HIGH | #001, #005 |
| #009 GO App | Not Started | MEDIUM | #001, #005 |
| #010 ASK App | Not Started | MEDIUM | #001 |
| #011 LISTEN App | Not Started | MEDIUM | #001 |
| #012 THINK App | Not Started | MEDIUM | #001 |
| #013 REFLECT App | Not Started | LOW | #001, #014 |
| #014 Notification Agg | Not Started | MEDIUM | #001 |
| #015 FUND App | Not Started | LOW | #001 |
| #016 LOOK App | Not Started | LOW | #001 |

---

## How to Use This System

### For Individual Contributors

1. Check dependencies - make sure prerequisite tickets are done
2. Read the ticket carefully
3. Follow the implementation plan
4. Test thoroughly (both touch UI and Intent API)
5. Update ticket status
6. Create a PR or commit with reference to ticket number

### For Project Managers

- Track progress using the status table
- Ensure critical path items (TICKET_001, TICKET_006) are prioritized
- Can parallelize work after foundation is complete:
  - One dev on TASK/SET apps
  - Another on Permission System
  - Another on WiFi/Bluetooth controllers

### For AI Agents

- Pick tickets marked "Not Started" with no dependencies
- Follow implementation plan exactly
- Report any blockers or issues
- Update status when complete

---

## Ticket Template

See `TICKET_TEMPLATE.md` for the structure to use when creating new tickets.

---

## Questions or Issues?

- Check `/docs/LAUNCHER_ARCHITECTURE.md` for design decisions
- Check `/docs/LAUNCHER_REQUIREMENTS.md` for implementation patterns
- Check `/docs/TOOLS.md` for Intent API specs
- Create a new ticket if you discover additional work needed

---

## Success Metrics

### Phase 1 Complete (Foundation)
- [ ] Intent API working
- [ ] WiFi control working (reference implementation)
- [ ] Can test Intent API via adb

### Phase 2 Complete (Core Apps)
- [ ] Can launch any app via touch or voice
- [ ] Can control WiFi, Bluetooth, brightness, volume
- [ ] Permission system handles all dangerous permissions

### Phase 3 Complete (Communications)
- [ ] Can make calls, send SMS via touch or voice
- [ ] Calendar integration working
- [ ] Time management (alarms, timers) working

### Feature Complete (All Phases)
- [ ] All 12 Stone apps implemented
- [ ] All Intent API actions documented and working
- [ ] Comprehensive test coverage
- [ ] Ready for AI agent integration

---

Last Updated: November 12, 2025
