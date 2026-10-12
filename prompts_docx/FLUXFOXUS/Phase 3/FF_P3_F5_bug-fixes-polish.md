# FF Phase 3 — Feature 5: Bug Fixes + Polish

**Version:** 1.0  
**Phase:** 3  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

This document is a living checklist for accumulated P1 and P2 issues to be resolved in P3. It is intentionally sparse at the time of writing — items are added as P1 and P2 are built and validated. This document is the designated dumping ground for all non-feature work in P3.

---

## 2. How to Use This Document

When a bug, edge case, or UX rough edge is identified during P1 or P2 development that is:
- **Not blocking P1/P2 completion** (blocking bugs must be fixed in their own phase)
- **Not a new feature** (feature requests belong in the phase map)
- **Worth addressing before P4 work begins**

→ Add it to the relevant section below.

**Format for each item:**
```
### [ID] Short title
- **Discovered in:** Phase N, Feature/Screen
- **Description:** What the problem is
- **Expected behaviour:** What should happen
- **Priority:** High / Medium / Low
- **Status:** Open / In Progress / Done
```

---

## 3. Known P1 Items (Pre-Build)

The following are anticipated edge cases documented before P1 build begins based on spec review.

### BUG-001 — WorkManager midnight task drift
- **Discovered in:** P1 spec review
- **Description:** Android WorkManager periodic tasks have a minimum 15-minute flex window. The midnight streak evaluation task may fire up to 15 minutes late, causing a streak miss to be evaluated on the wrong calendar day in edge cases (e.g. user completes a session at 11:58 PM)
- **Expected behaviour:** Streak evaluation uses the session's actual completion timestamp, not the WorkManager fire time. Day boundary is computed from session data, not task execution time
- **Priority:** High
- **Status:** Open

### BUG-002 — Foreground service notification channel conflict
- **Discovered in:** P1 spec review
- **Description:** `flutter_foreground_task` creates its own notification channel. The lock screen session notification (P3 F3) must use a separate channel. Risk of channel ID collision if not explicitly managed
- **Expected behaviour:** Two distinct notification channels: `focus_session_foreground` (flutter_foreground_task) and `focus_lock_screen` (lock screen display). No shared channels
- **Priority:** Medium
- **Status:** Open (relevant from P3 F3)

### BUG-003 — Accessibility Service YouTube detection on YouTube Shorts
- **Discovered in:** P1 spec review
- **Description:** YouTube Shorts uses a different UI hierarchy than standard YouTube videos. Channel name detection via AccessibilityNodeInfo may not work reliably for Shorts
- **Expected behaviour:** Study Mode channel enforcement applies to Shorts as well as standard videos. Detection strategy must account for Shorts UI
- **Priority:** High
- **Status:** Open

### BUG-004 — App limit enforcement when device restarts mid-limit
- **Discovered in:** P1 spec review
- **Description:** If the device restarts while an app limit is active, the `RECEIVE_BOOT_COMPLETED` broadcast restores block schedules (TRD §9.3) but the daily usage counter must be re-read from UsageStats API, not from FF's cached value (which may be stale)
- **Expected behaviour:** On boot, FF re-queries UsageStats for today's usage before re-arming limits
- **Priority:** High
- **Status:** Open

### BUG-005 — FD MethodChannel cold start race condition
- **Discovered in:** P1 spec review
- **Description:** If FF receives a FocusBlockRequest (PRD §3.12.2) from FD before FF has fully initialised (e.g. device boot), the MethodChannel handler may not be registered yet
- **Expected behaviour:** FF queues incoming FocusBlockRequests during startup and processes them once the handler is registered. Max queue size: 50 events. Queue flushed on handler registration
- **Priority:** High
- **Status:** Open

---

## 4. P2 Items (To Be Filled During Build)

*Empty at time of writing. Add items here as P2 is built and tested.*

---

## 5. UX Polish Items (Pre-Build)

Known UX improvements identified during spec phase that are not bugs but deserve attention in P3.

### UX-001 — Flip clock animation smoothness on low-end devices
- **Description:** The mechanical flip animation (PRD §3.2.2) may drop frames on low-end Android devices (API 26, 1GB RAM). The flip should degrade gracefully — static digit update if frame rate drops below 30fps
- **Priority:** Medium
- **Status:** Open

### UX-002 — Intervention overlay (2/5/10/20) dismiss animation
- **Description:** The intervention overlay should have a smooth slide-down dismiss when a time window is selected. Abrupt appearance/disappearance is jarring
- **Priority:** Low
- **Status:** Open

### UX-003 — Preset 60-hour nudge dismissal persistence
- **Description:** The 60-hour confirmation nudge (TRD §5.10) should not reappear if the user has already seen and dismissed it within the same app session. Currently no in-session dismissal flag is specified
- **Priority:** Medium
- **Status:** Open

### UX-004 — Session card "Live" badge pulse animation
- **Description:** The Planner session card sub-label "Live" (PRD §3.10.3) should pulse (subtle opacity animation) to visually distinguish it from "Planned" and "Spent" static labels
- **Priority:** Low
- **Status:** Open

---

## 6. Performance Targets (Carry Forward from P1)

These are not new — they are the P1 NFRs (PRD §4.1) re-stated as P3 acceptance criteria. If any are not met by end of P2, they are resolved in P3 polish.

| Metric | Target | Verify in |
|---|---|---|
| Cold start | < 2 seconds | P1 validation |
| Session timer accuracy | ±1 second | P1 validation |
| Widget refresh delay | ≤ 15 minutes | P1 validation |
| UsageStats query | < 500ms | P1 validation |
| AI API response (break negotiation) | < 10 seconds | P2 validation |
| Focus Score computation | < 200ms | P3 F1 |
| Lock screen notification update | ≤ 1 second lag | P3 F3 |

---

## 7. Deprecation Review

At the start of P3, before any feature work begins, conduct a review of:

- **Device Admin API (P2 F1):** verify Google has not deprecated basic Device Admin registration for the current target SDK. Update if needed
- **Accessibility Service restrictions:** verify that the `canRetrieveWindowContent` flag (TRD §9.4) is still permitted under current Play Store policy for the target SDK
- **flutter_foreground_task:** verify compatibility with latest Flutter stable and target Android SDK
- **WorkManager constraints:** verify that exact alarm scheduling (`android_alarm_manager_plus`) still works without `SCHEDULE_EXACT_ALARM` permission being restricted on the target SDK

Document findings and any required mitigations before P3 feature builds begin.
