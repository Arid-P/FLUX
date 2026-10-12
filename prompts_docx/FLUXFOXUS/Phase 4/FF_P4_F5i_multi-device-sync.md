# FF Phase 4 — Feature 5.i: Multi-Device Session Sync

**Version:** 1.0  
**Phase:** 4 (Sub-feature i of F5)  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Multi-device support allows the same FluxFoxus focus session to run simultaneously on two devices. Both devices display the session in real time — breaks, elapsed time, and session state stay in sync. Conflicts (e.g. break tapped on both devices at the same time) are resolved via optimistic locking: the first device to write wins.

Non-session data (presets, streaks, Focus Score history, etc.) syncs via the 24-hour Google Drive full dump — same mechanism as FluxDone P2.

---

## 2. Infrastructure

### 2.1 Cloudflare D1 (Live Session State)
- Cloudflare's serverless SQLite database
- Free tier: 5 GB storage, 5 million reads/day, 100k writes/day
- Used exclusively for live session state during active sessions
- ~200 bytes per user — free tier is never approached in practice

### 2.2 Cloudflare Worker (API Layer)
- Thin serverless JavaScript function (~50–100 lines)
- Acts as the API between Flutter app and D1
- Handles optimistic locking logic server-side
- Deployed on Cloudflare Workers free tier (100k requests/day)

### 2.3 Google Drive (24-Hour Full Dump)
- Same mechanism as FD P2 auto-backup amendment
- Change-triggered (120s debounce) + 24hr periodic fallback
- Full SQLite dump of entire FF database
- Merges session history from both devices on restore

### 2.4 Authentication
- User identity derived from the Google account already linked for Drive backup
- No separate auth system — Google Sign-In (already in FF for Drive) provides the user ID
- Device ID: generated UUID stored in `shared_preferences` on first launch

---

## 3. Data Split

### 3.1 Cloudflare D1 — Live Session State Only

```sql
CREATE TABLE active_sessions (
  user_id TEXT NOT NULL,
  session_id TEXT NOT NULL,
  device_id TEXT NOT NULL,           -- last device to write
  preset_id TEXT NOT NULL,
  session_name TEXT NOT NULL,
  mode TEXT NOT NULL,                -- countdown / stopwatch / open_ended
  planned_duration_seconds INTEGER,
  elapsed_seconds INTEGER NOT NULL DEFAULT 0,
  breaks_total INTEGER NOT NULL,
  breaks_remaining INTEGER NOT NULL,
  break_active INTEGER NOT NULL DEFAULT 0,   -- 1 = break in progress
  break_elapsed_seconds INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'active',     -- active / break / stopped / completed
  version INTEGER NOT NULL DEFAULT 0,        -- increments on every state change
  break_lock INTEGER NOT NULL DEFAULT 0,     -- 1 = break action in flight
  last_updated INTEGER NOT NULL,             -- Unix ms
  PRIMARY KEY (user_id)                      -- one active session per user
);
```

### 3.2 Google Drive — Everything Else
Full SQLite dump includes: sessions history, presets, streaks, Focus Score history, app limits, app categories, YouTube whitelist, AI settings, planner data, all Hive-backed data serialized alongside.

### 3.3 Not Synced in Real-Time
- App limits daily usage counters (device-specific by nature)
- Screen time data (device-specific — UsageStats is per-device)
- Notification state

---

## 4. Live Sync Mechanism

### 4.1 Polling

During an active session, each device polls the Cloudflare Worker every N seconds to check for state updates.

**Adaptive polling interval:**
```dart
int getPollingIntervalSeconds() {
  final remainingQuota = cloudflareQuotaTracker.remainingRequestsToday;
  final remainingSessionSeconds = session.remainingSeconds;
  
  // Estimate requests needed for rest of session at minimum interval
  final minimumRequests = remainingSessionSeconds / 2;  // 2s minimum
  
  if (remainingQuota > minimumRequests * 3) return 2;   // plenty of quota: 2s
  if (remainingQuota > minimumRequests * 2) return 5;   // moderate: 5s
  if (remainingQuota > minimumRequests) return 10;       // low: 10s
  return 30;                                             // critical: 30s
}
```

Minimum: **2 seconds**. Maximum: **30 seconds** (quota-critical fallback).

Quota usage is tracked locally in `shared_preferences` and reset daily at midnight.

### 4.2 State Update on Poll

On each poll, device receives the full D1 `active_sessions` record. Device compares `version` field with its local version:
- Same version → no change, nothing to update
- Higher version → remote state is newer → apply remote state to local UI

Elapsed time is NOT synced via polling — each device increments its own local timer. Only state changes (breaks, stop) trigger version increments.

### 4.3 Cloudflare Worker Endpoints

```
POST /session/start        — Create new active_session record
POST /session/update       — Update state with optimistic lock check
POST /session/end          — Mark session as stopped/completed, delete record
GET  /session/current      — Poll current state
```

All requests include: `user_id`, `device_id`, `version` (for writes).

---

## 5. Conflict Resolution

### 5.1 Optimistic Locking

Every state-changing write includes the client's current `version` number. The Worker checks:

```javascript
// Cloudflare Worker — session/update handler
const current = await db.prepare(
  'SELECT version, break_lock FROM active_sessions WHERE user_id = ?'
).bind(userId).first();

if (current.version !== clientVersion) {
  return Response.json({ success: false, reason: 'version_mismatch', 
                         currentState: current }, { status: 409 });
}

// Accept write — increment version
await db.prepare(
  'UPDATE active_sessions SET ..., version = version + 1 WHERE user_id = ?'
).bind(..., userId).run();

return Response.json({ success: true });
```

### 5.2 Break Conflict — Manual Breaks

**Scenario:** Both devices tap break within the same polling window.

- Device A sends break request with version N → Worker accepts → version becomes N+1, `break_active = 1`, `breaks_remaining` decremented
- Device B sends break request with version N → Worker rejects (version mismatch)
- Device B receives rejection → snackbar: *"Break already taken on another device"*
- Device B polls → gets version N+1 state → UI updates to show break in progress

**Result:** First tap wins. No double-decrement possible.

### 5.3 Break Conflict — AI Breaks

AI break negotiation runs entirely on the initiating device. The other device does not participate in the negotiation.

- Initiating device runs full negotiation flow (FF P2 F3)
- On grant: initiating device writes break state to D1 (same as manual break)
- Other device picks up break state on next poll
- On denial: no D1 write, other device unaffected

### 5.4 Stop Focusing Conflict

- Device A confirms Stop Focusing → writes `status = stopped` to D1 → version increments
- Device B polls → receives `status = stopped` → session end flow triggers on Device B automatically (navigates to home, session logged)
- If Device B also had Stop Focusing modal open: modal closes, session ends

**First to confirm wins.** If Device B's Stop Focusing modal was open when Device A confirmed, Device B's modal closes with a brief message: *"Session ended on another device."*

---

## 6. Session Start on Second Device

When a device opens FF and the user has an active session in D1:

- FF checks D1 on app open (one-time check, not part of polling loop)
- Active session found → home screen shows the session as active immediately
- Timer starts from `elapsed_seconds` in D1
- Polling loop begins
- No confirmation prompt needed — the session is simply shown as running

**If D1 is unreachable on app open:** device falls back to last known local state. If no local session was active, home screen shows normally. No error shown.

---

## 7. Session End + History

On session end (completed or stopped):
- Whichever device ends the session writes final state to D1 (`status = completed/stopped`)
- Both devices log the session to their local SQLite `focus_sessions` table independently
- D1 record is deleted after a 60-second grace period (allows the other device to receive the final state on its next poll)
- Next 24hr Google Drive dump merges both devices' session history into a single backup

**Deduplication on restore:** Sessions with the same `session_id` are deduplicated — only one record kept. The record with the longer `actual_focus_seconds` wins (assumes the device that ran longer has more accurate data).

---

## 8. Non-Session Data Sync (24hr Drive Dump)

Follows identical pattern to FD P2 auto-backup amendment:
- Change-triggered: 120s debounce after qualifying writes
- Periodic fallback: every 24 hours
- Manual trigger available in FF Settings → Data
- File: `fluxfoxus_backup.db` — single overwrite file in Google Drive

**On restore (new device setup):**
- User signs in to Google
- FF detects Drive backup exists → offers restore
- Full SQLite + Hive data restored
- Device gets fresh `device_id` UUID — treated as a new device for D1 purposes

---

## 9. Quota Management

### 9.1 Cloudflare D1 Free Tier
- 5 million reads/day, 100k writes/day
- A 2-hour session at 2s polling = 3,600 read requests
- Even with 5 active sessions/day at 2s polling = ~18,000 reads — well within free tier
- Writes are rare (only on state changes) — easily within 100k/day

### 9.2 Cloudflare Worker Free Tier
- 100k requests/day
- Same math as above — well within limits for personal use

### 9.3 Local Quota Tracker

```dart
class CloudflareQuotaTracker {
  static const int dailyReadLimit = 5000000;
  static const int dailyWriteLimit = 100000;
  
  // Stored in shared_preferences, reset daily at midnight
  int get remainingReadsToday => ...;
  int get remainingWritesToday => ...;
  
  void recordRead() => ...;
  void recordWrite() => ...;
}
```

---

## 10. Cloudflare Worker — Full Implementation Spec

The Worker is a single JavaScript file deployed to Cloudflare Workers. It handles all D1 operations. Flutter app never touches D1 directly — only via Worker endpoints.

```javascript
// worker.js — pseudocode structure
export default {
  async fetch(request, env) {
    const { pathname } = new URL(request.url);
    const body = await request.json();
    
    switch(pathname) {
      case '/session/start':   return handleStart(body, env.DB);
      case '/session/update':  return handleUpdate(body, env.DB);
      case '/session/end':     return handleEnd(body, env.DB);
      case '/session/current': return handlePoll(body, env.DB);
      default: return new Response('Not found', { status: 404 });
    }
  }
};
```

Authentication: all requests include a Bearer token derived from the Google Sign-In ID token. Worker validates the token before processing any request.

---

## 11. Settings

**Location:** Settings → Account → Multi-Device Sync

| Control | Type | Default | Description |
|---|---|---|---|
| Multi-Device Sync | Toggle | OFF | Master toggle. OFF = no D1 sync, sessions are device-local |
| Sync status | Status label | — | "Active — 2 devices" / "Not configured" |
| Google account | Display row | — | Shows linked Google account (shared with Drive backup) |
| Daily quota usage | Progress bar | — | Shows today's D1 request usage vs limit |

---

## 12. Module Boundary

**New module:** `multi_device/`

```
features/
└── multi_device/
    ├── data/
    │   ├── cloudflare_api_client.dart      # Worker HTTP calls
    │   ├── quota_tracker.dart              # Daily request tracking
    │   └── multi_device_repository_impl.dart
    ├── domain/
    │   ├── active_session_state.dart       # D1 record model
    │   └── use_cases/
    │       ├── sync_session_state.dart
    │       ├── resolve_break_conflict.dart
    │       └── end_remote_session.dart
    └── presentation/
        └── multi_device_settings_section.dart

cloudflare/
└── worker.js                               # Cloudflare Worker source
```

Modifications to existing modules:
- `focus_timer/presentation/focus_session_screen.dart` — polling loop, remote state apply
- `focus_timer/domain/use_cases/` — break/stop actions check D1 before local write
- `settings/presentation/` — Multi-Device Sync section under Account
- `backup/` — 24hr Drive dump extended to FF (same pattern as FD P2 amendment)
