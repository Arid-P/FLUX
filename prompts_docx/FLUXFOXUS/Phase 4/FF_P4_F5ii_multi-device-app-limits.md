# FF Phase 4 — Feature 5.ii: Multi-Device App Limits Sync

**Version:** 1.0  
**Phase:** 4 (Sub-feature ii of F5)  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

App Limits enforcement becomes cross-device aware. Both devices share a single daily time budget per app — usage on Device A counts against the same limit as usage on Device B. Configuration (limit settings) syncs via the 24hr Google Drive dump. Live usage tracking syncs via Cloudflare D1, updated every 1–5 minutes while a limited app is in use.

---

## 2. References

| Reference | Location |
|---|---|
| App Limits — P1 spec | FF_PRD_v1_0.md §3.6 |
| Category Limits | FF_P4_F3_category-limits.md |
| Multi-Device Session Sync (D1 infrastructure) | FF_P4_F5i_multi-device-sync.md |
| Cloudflare D1 + Worker | FF_P4_F5i_multi-device-sync.md §2 |
| Google Drive 24hr dump | FF_P4_F5i_multi-device-sync.md §8 |

---

## 3. Data Split

### 3.1 Google Drive (24hr Dump) — Configuration
App limit settings travel between devices via the standard 24hr Drive dump:
- Daily time budget per app
- Extra time session count + duration
- Category limit settings
- App category assignments
- Per-app friction settings (P4 F2)

Configuration changes on Device A are picked up by Device B within 24 hours. No real-time sync needed for settings.

### 3.2 Cloudflare D1 — Live Usage

```sql
CREATE TABLE app_usage_sync (
  user_id TEXT NOT NULL,
  package_name TEXT NOT NULL,
  date TEXT NOT NULL,                    -- YYYY-MM-DD (local date)
  device_a_seconds INTEGER NOT NULL DEFAULT 0,
  device_b_seconds INTEGER NOT NULL DEFAULT 0,
  combined_seconds INTEGER NOT NULL DEFAULT 0,  -- maintained by Worker
  extra_sessions_remaining INTEGER NOT NULL,     -- shared pool
  last_updated INTEGER NOT NULL,
  version INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (user_id, package_name, date)
);
```

One row per app per day per user. `combined_seconds` = `device_a_seconds` + `device_b_seconds`, maintained server-side by the Worker on every write.

---

## 4. Usage Sync Mechanism

### 4.1 Sync Interval

User-configurable in Settings → Account → Multi-Device Sync → App Limits Sync Interval:

- Range: **1 minute to 5 minutes** (60–300 seconds)
- Default: **2 minutes**
- Presented as a slider with minute labels: 1m / 2m / 3m / 4m / 5m
- Adaptive fallback: same quota-aware logic as session polling (FF_P4_F5i §4.1) — backs off toward 5 minutes as daily D1 quota is consumed

### 4.2 Push on Interval

While a limited app is in foreground, FF pushes a usage delta to D1 every N seconds:

```dart
void pushUsageDelta(String packageName, int deltaSeconds) async {
  final payload = {
    'user_id': userId,
    'device_id': deviceId,
    'package_name': packageName,
    'date': today,
    'delta_seconds': deltaSeconds,
  };
  
  await cloudflareClient.post('/limits/usage/push', payload);
  quotaTracker.recordWrite();
}
```

The Worker adds `delta_seconds` to the appropriate device column and recomputes `combined_seconds`.

### 4.3 Pull on App Open

When a limited app is opened, FF immediately pulls the current combined usage from D1 before deciding whether to enforce:

```dart
Future<int> getCombinedUsageToday(String packageName) async {
  try {
    final result = await cloudflareClient.get(
      '/limits/usage/current',
      params: {'package_name': packageName, 'date': today},
    );
    quotaTracker.recordRead();
    return result['combined_seconds'];
  } catch (_) {
    // Offline — fall back to device-local usage
    return localUsageRepository.getTodayUsage(packageName);
  }
}
```

---

## 5. Enforcement Flow

### 5.1 Normal (Online) Flow

```
User opens limited app
        ↓
FF pulls combined_seconds from D1
        ↓
combined_seconds >= daily_limit?
  YES → 2/5/10/20 intervention window (PRD §3.7.7)
  NO  → App opens, usage sync loop starts (push every N seconds)
        ↓
App closed / moved to background
        ↓
Final delta pushed to D1
```

### 5.2 Offline Flow

```
User opens limited app (device offline)
        ↓
D1 pull fails → FF falls back to device-local usage only
        ↓
device_local_seconds >= daily_limit?
  YES → 2/5/10/20 intervention (enforcement continues offline)
  NO  → App opens, local usage tracked
        ↓
Device comes back online
        ↓
Reconciliation runs (see §6)
```

---

## 6. Offline Reconciliation

When a device reconnects after offline usage:

1. FF pushes accumulated offline usage delta to D1
2. Worker recomputes `combined_seconds`
3. FF pulls updated `combined_seconds`
4. If `combined_seconds` now exceeds `daily_limit` (overage occurred):
   - If the limited app is currently open: 2/5/10/20 intervention fires immediately
   - If the limited app is not open: overage is logged silently. Next time the app is opened, enforcement will trigger normally
5. No retroactive penalty for the overage period — usage already happened, it's recorded and future enforcement is tightened

---

## 7. Extra Time Sessions — Shared Pool

Extra time sessions (PRD §3.6.3) are shared across both devices. The `extra_sessions_remaining` field in D1 is the source of truth.

### 7.1 Consuming an Extra Time Session

```
User hits limit on Device A → 2/5/10/20 window appears → user requests extra time
        ↓
FF sends extra time request to Worker:
  { user_id, package_name, date, device_id, version }
        ↓
Worker checks extra_sessions_remaining > 0 AND version matches
  YES → decrement extra_sessions_remaining, increment version, return new session duration
  NO (version mismatch) → reject: "Extra time already used on another device"
  NO (none remaining) → reject: "No extra time sessions remaining"
```

### 7.2 Conflict Resolution

Same optimistic locking as session breaks (FF_P4_F5i §5.1). First device to request wins. Second device gets a rejection with the current state.

### 7.3 Offline Extra Time

If device is offline when requesting extra time:
- FF uses local `extra_sessions_remaining` count
- On reconnect: reconcile with D1
- If overage (both devices consumed from the same session offline): log the discrepancy, set `extra_sessions_remaining = 0` in D1, no retroactive enforcement

---

## 8. Category Limits

Category limits (FF_P4_F3) follow the same shared budget model:

```sql
-- Additional D1 table
CREATE TABLE category_usage_sync (
  user_id TEXT NOT NULL,
  category TEXT NOT NULL,              -- 'distracting' | 'semi_productive' | etc.
  date TEXT NOT NULL,
  device_a_seconds INTEGER NOT NULL DEFAULT 0,
  device_b_seconds INTEGER NOT NULL DEFAULT 0,
  combined_seconds INTEGER NOT NULL DEFAULT 0,
  extra_sessions_remaining INTEGER NOT NULL,
  version INTEGER NOT NULL DEFAULT 0,
  last_updated INTEGER NOT NULL,
  PRIMARY KEY (user_id, category, date)
);
```

Same push/pull/reconcile logic as per-app limits. Apps tracked under a category limit push to `category_usage_sync` instead of `app_usage_sync`.

---

## 9. Quota Impact

### 9.1 D1 Request Volume Estimate

Assume 5 limited apps, both devices active 8 hours/day, 2-minute sync interval:

- Reads on app open: 5 apps × 10 opens/day × 2 devices = 100 reads/day
- Writes on interval: (8 hours × 60 min/hr / 2 min interval) × 5 apps × 2 devices = 2,400 writes/day
- **Total: ~2,500 reads + 2,400 writes/day** — well within free tier (5M reads, 100k writes)

### 9.2 Combined with Session Sync

Adding session sync (FF_P4_F5i) at 2s polling for 3 hours/day:
- Session reads: 5,400/day
- **Grand total: ~7,900 reads + ~2,500 writes/day** — still well within free tier

---

## 10. Cloudflare Worker — New Endpoints

Added to existing Worker from FF_P4_F5i:

```
POST /limits/usage/push      — Push usage delta for an app or category
GET  /limits/usage/current   — Pull current combined usage for an app
POST /limits/extratime/use   — Consume an extra time session (with optimistic lock)
```

---

## 11. Settings

Multi-Device Sync settings screen (FF_P4_F5i §11) gains a new row:

| Control | Type | Default | Description |
|---|---|---|---|
| App Limits Sync Interval | Slider (1–5 min) | 2 min | How often usage is pushed to D1 while a limited app is open |

---

## 12. Module Boundary

Extensions to existing modules — no new module needed (shares `multi_device/` from FF_P4_F5i):

```
features/
└── multi_device/
    ├── data/
    │   ├── cloudflare_api_client.dart     ← EXTENDED (new endpoints)
    │   └── multi_device_repository_impl.dart ← EXTENDED (usage push/pull)
    └── domain/
        └── use_cases/
            ├── sync_app_usage.dart        ← NEW
            ├── get_combined_usage.dart    ← NEW
            └── consume_shared_extra_time.dart ← NEW

cloudflare/
└── worker.js                              ← EXTENDED (3 new endpoints)
```

Modifications to existing modules:
- `app_limits/domain/use_cases/enforce_app_limit.dart` — pull combined usage from D1 on app open
- `app_limits/data/` — usage push loop during active app usage
- `app_limits/presentation/` — extra time session conflict message
