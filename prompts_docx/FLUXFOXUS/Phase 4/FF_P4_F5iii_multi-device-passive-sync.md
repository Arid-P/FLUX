# FF Phase 4 — Feature 5.iii: Multi-Device Passive Data Sync

**Version:** 1.0  
**Phase:** 4 (Sub-feature iii of F5)  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Passive data sync ensures that non-session, non-usage data (presets, streaks, Focus Score history, YouTube whitelist, app category assignments, AI settings, session history, app limit configuration) automatically propagates from Device A to Device B after any change. Device A dumps to Google Drive and sets a lightweight D1 notification flag. Device B detects the flag, pulls the dump, and applies only changed/new records via selective merge.

This is not real-time — propagation happens within minutes of a change, not seconds. That is acceptable for this data type.

---

## 2. References

| Reference | Location |
|---|---|
| 24hr Drive dump mechanism | FF_P4_F5i_multi-device-sync.md §8 |
| Cloudflare D1 + Worker infrastructure | FF_P4_F5i_multi-device-sync.md §2 |
| FD auto-backup amendment | FD_P2_AMENDMENT_auto-google-drive-backup.md |

---

## 3. Data Covered

All data NOT handled by 5.i or 5.ii:

| Data | Storage | Change frequency |
|---|---|---|
| Presets | SQLite | Occasional |
| Streak records | SQLite | Daily |
| Focus Score history | SQLite | Daily |
| YouTube study channel whitelist | Hive | Occasional |
| App category assignments | Hive | Rare |
| AI settings (difficulty, custom prompt) | Hive | Rare |
| Session history (completed sessions) | SQLite | After each session |
| App limits configuration (settings) | SQLite | Occasional |
| Category limits configuration | SQLite | Occasional |

---

## 4. Sync Flow

### 4.1 Device A — Change Detected + Dump Triggered

```
Qualifying change occurs on Device A
(preset created, streak updated, session completed, etc.)
        ↓
120-second debounce timer starts (resets on further changes)
        ↓
Timer fires → Drive dump executes (full SQLite + Hive serialized)
        ↓
Dump succeeds → Device A writes notification flag to D1:
{
  user_id: "...",
  dump_timestamp: Unix ms,
  dump_version: integer (increments per dump),
  triggering_device: device_id
}
```

### 4.2 Device B — Notification Detection

Device B checks the D1 notification flag:
- **On app open:** always check flag on cold start
- **Periodic background check:** every 15 minutes via WorkManager (lightweight — single D1 read)

```dart
Future<void> checkForNewDump() async {
  final flag = await cloudflareClient.get('/sync/dump-flag');
  final lastKnownVersion = prefs.getInt('last_applied_dump_version') ?? 0;
  
  if (flag['dump_version'] > lastKnownVersion &&
      flag['triggering_device'] != deviceId) {
    // New dump available from another device — trigger merge
    await pullAndMergeDump(flag['dump_timestamp']);
  }
}
```

The `triggering_device` check prevents Device A from re-applying its own dump.

### 4.3 Device B — Pull and Selective Merge

```
New dump detected
        ↓
Device B downloads dump file from Google Drive
        ↓
Selective merge runs (see §5)
        ↓
Device B updates local_applied_dump_version in shared_preferences
        ↓
UI refreshes to reflect any new data
```

---

## 5. Selective Merge

Full replace is avoided — Device B's local data (especially device-specific data like UsageStats) must be preserved. Only records from the dump that are newer or don't exist locally are applied.

### 5.1 Merge Strategy Per Table

| Table | Merge Key | Strategy |
|---|---|---|
| `presets` | `id` | Upsert — dump version wins on conflict (last-write-wins by `updated_at`) |
| `streak_records` | `user_id` | Merge — take highest `current_streak`, highest `longest_streak` |
| `daily_focus_scores` | `date` | Upsert — dump version wins if `is_final = 1`, local wins if dump is not final |
| `focus_sessions` (history) | `id` | Insert-only — never overwrite existing sessions, only add missing ones |
| `app_limits` (config) | `package_name` | Upsert — `updated_at` timestamp determines winner |
| `category_limits` | `category` | Upsert — `updated_at` wins |
| `scheduled_sessions` | `id` | Upsert — `updated_at` wins |
| Hive: YouTube whitelist | channel `id` | Union — add missing channels, never remove |
| Hive: app categories | `package_name` | Upsert — `updated_at` wins |
| Hive: AI settings | key | Upsert — dump wins (settings are global preferences) |

### 5.2 Never Overwritten by Merge

The following are always device-local and never touched by the merge:

- `app_usage_records` (UsageStats data — device-specific)
- `app_usage_sync` D1 records (handled by 5.ii)
- `widget_preferences` (shared_preferences — device-specific)
- Device ID
- Notification channel state
- Permission grant state

### 5.3 Merge Implementation

```dart
Future<void> applySelectiveMerge(FFBackup dump) async {
  await db.transaction(() async {
    // Presets — upsert by updated_at
    for (final preset in dump.presets) {
      final local = await presetDao.getById(preset.id);
      if (local == null || preset.updatedAt.isAfter(local.updatedAt)) {
        await presetDao.upsert(preset);
      }
    }
    
    // Streak — take best values
    final localStreak = await streakDao.get();
    final dumpStreak = dump.streakRecord;
    await streakDao.save(StreakRecord(
      currentStreak: max(localStreak.currentStreak, dumpStreak.currentStreak),
      longestStreak: max(localStreak.longestStreak, dumpStreak.longestStreak),
      // ... other fields from whichever has later updatedAt
    ));
    
    // Session history — insert missing only
    for (final session in dump.focusSessions) {
      final exists = await sessionDao.exists(session.id);
      if (!exists) await sessionDao.insert(session);
    }
    
    // ... same pattern for all other tables
  });
  
  // Hive merge (outside SQLite transaction)
  await mergeYoutubeWhitelist(dump.youtubeChannels);
  await mergeAppCategories(dump.appCategories);
  await mergeAiSettings(dump.aiSettings);
}
```

---

## 6. D1 Notification Flag Table

Added to existing D1 database (FF_P4_F5i):

```sql
CREATE TABLE sync_flags (
  user_id TEXT PRIMARY KEY,
  dump_timestamp INTEGER NOT NULL,
  dump_version INTEGER NOT NULL DEFAULT 0,
  triggering_device TEXT NOT NULL,
  last_updated INTEGER NOT NULL
);
```

Single row per user. Always overwritten on new dump. ~50 bytes.

---

## 7. Cloudflare Worker — New Endpoints

```
POST /sync/notify-dump    — Device A writes dump flag after successful Drive upload
GET  /sync/dump-flag      — Device B checks for new dump availability
```

---

## 8. Conflict Edge Cases

### 8.1 Both Devices Change Same Preset Simultaneously
- Both dump within minutes of each other
- Each dump has a different `dump_version`
- Whichever dump has the later `updated_at` on the preset wins
- The other device's change is overwritten — acceptable for rare settings changes

### 8.2 Streak Conflict
- Device A completes a session → streak increments to 47 → dumps
- Device B also completed a session while offline → streak incremented to 47 locally
- Merge takes `max(47, 47) = 47` — correct

- Device A's streak resets to 0 (quit session) → dumps
- Device B hasn't synced yet → still shows 47
- Merge: streak record `updatedAt` from Device A (reset) is later → Device A wins → streak resets on Device B too
- This is correct behaviour — a confirmed quit should propagate

### 8.3 New Device Setup
On first launch of FF on a new device:
- User signs in to Google
- FF checks D1 for `sync_flags` record
- If found → full restore from Drive dump (not selective merge — no local data exists yet)
- If not found → fresh install, no sync

---

## 9. Storage + Quota Impact

### 9.1 D1
- `sync_flags` table: 1 row per user, ~50 bytes — negligible
- New endpoints: 2 reads + 1 write per dump cycle
- Background check: 1 read per device per 15 minutes = ~96 reads/device/day
- **Total new D1 load: ~200 reads/day** — negligible against 5M free tier

### 9.2 Google Drive
- Dump file size: FF SQLite is typically 2–10 MB depending on session history
- Drive free storage: 15 GB — years of dumps before approaching limit
- Each dump overwrites the same file — storage doesn't grow

---

## 10. Settings

No new settings controls needed. The sync is automatic and transparent. The existing Multi-Device Sync screen (FF_P4_F5i §11) shows:

- Last sync timestamp (updated after each successful merge)
- "Sync now" button — triggers immediate dump + flag update on current device

---

## 11. Module Boundary

Extensions to existing `multi_device/` module:

```
features/
└── multi_device/
    ├── data/
    │   ├── cloudflare_api_client.dart        ← EXTENDED (2 new endpoints)
    │   ├── drive_dump_client.dart            ← NEW (Drive download for merge)
    │   └── multi_device_repository_impl.dart ← EXTENDED
    ├── domain/
    │   ├── ff_backup.dart                    ← NEW (dump deserialization model)
    │   └── use_cases/
    │       ├── notify_dump_available.dart    ← NEW (called after Drive upload)
    │       ├── check_for_new_dump.dart       ← NEW (periodic + on-open check)
    │       └── apply_selective_merge.dart    ← NEW
    └── presentation/
        └── multi_device_settings_section.dart ← MODIFIED (last sync timestamp, sync now)

cloudflare/
└── worker.js                                  ← EXTENDED (2 new endpoints)
```

Modifications to existing modules:
- `backup/` — after successful Drive upload, call `notify_dump_available`
- `core/background/` — WorkManager task for 15-minute dump check
